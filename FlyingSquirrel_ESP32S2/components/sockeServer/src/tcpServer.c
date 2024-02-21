#include "sockeServer.h"
#include <string.h>
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "sys/socket.h"
#include "netdb.h"
#include "errno.h"
#include "esp_system.h"
#include "esp_event.h"
#include "esp_log.h"

static const char *TAG = "tcpServer";
static const char *tx_buffer = "Message OK";
char rx_buffer[512];////接收客户端的数据
static char addr_str[128];//客户端地址
char mouseReporter[5];
char keyboardReporter[8];


/**hid设备队列数据*/
// typedef struct {
//   uint8_t modifier;   /**键盘修饰符*/
//   uint8_t reserved;   /**< 保留 始终设置为 0。 */
//   uint8_t keycode[6]; /**< 当前按下按键的键码 */

//   uint8_t buttons;      /**< 当前按下的鼠标按钮 */
//   int8_t  x;            /**< 鼠标 X 坐标变化量 */
//   int8_t  y;            /**< 鼠标 Y 坐标变化量 */
//   int8_t  vertical;     /**< 垂直滚动 */
//   int8_t  horizontal;   /**< 水平滚动 */
// } hid_msg_t;

static int socketReceive(const char *tag, const int sock, char * data, size_t max_len)
{
    int len = recv(sock, data, max_len, 0);
    if (len < 0) {
        if (errno == EINPROGRESS || errno == EAGAIN || errno == EWOULDBLOCK) {
            return 0;   //没有错误
        }
        if (errno == ENOTCONN) {
            ESP_LOGW(TAG, "[Socket %d]:已断开连接", sock);
            return -2;  // Socket 已断开连接
        }
      
        return -1;
    }

    return len;
}


void tcpServerTask(void *pvParameters){


    /**
        一个基本的socket建立顺序是
        Server端： socket(),  bind(), listen(),  accept(),  recv(),  recvfrom(),  recvmsg()
        Client端： socket(), connect(),  send(),  sendto(),  sendmsg()
        参考网址：https://www.nongnu.org/lwip/2_0_x/group__socket.html
        socket()用于创建一个socket描述符
        bind()函数把一个地址族中的特定地址赋给socket
        调用listen()来监听这个socket，如果客户端这时调用connect()发出连接请求，服务器端就会接收到这个请求。
        TCP服务器端依次调用socket()、bind()、listen()之后，就会监听指定的socket地址了。
        TCP客户端依次调用socket()、connect()之后就想TCP服务器发送了一个连接请求。
        TCP服务器监听到这个请求之后，就会调用 accept() 函数取接收请求，这样连接就建立好了。
        之后就可以开始网络I/O操作了，即类同于普通文件的读写I/O操作。
    */ 
    /* USB键鼠接收数据队列句柄*/
    extern QueueHandle_t usbHidQueue;
    usbHidQueue = xQueueCreate(HID_QUEUE_LENGTH, sizeof(hid_msg_t));
    if (usbHidQueue == NULL) {
        ESP_LOGE(TAG, "创建usbHid键鼠队列失败");
        
    }
    struct sockaddr_storage dest_addr;
    struct sockaddr_in *dest_addr_ip4 = (struct sockaddr_in *)&dest_addr;
        dest_addr_ip4->sin_addr.s_addr = htonl(INADDR_ANY);
        dest_addr_ip4->sin_family = AF_INET;
        dest_addr_ip4->sin_port = htons(PORT);

     

   /**
    int socket(int domain, int type, int protocol);
    服务器根据地址类型（ipv4,ipv6）、socket类型、协议创建socket。
    domain:协议族，常用的有 AF_INET 、AF_INET6、AF_LOCAL、AF_ROUTE其中AF_INET代表使用ipv4地址
    type:socket类型，常用的socket类型有，SOCK_STREAM、SOCK_DGRAM、SOCK_RAW、SOCK_PACKET、SOCK_SEQPACKET等
    protocol:协议。常用的协议有，IPPROTO_TCP、IPPTOTO_UDP、IPPROTO_SCTP、IPPROTO_TIPC等
    */
    int listen_sock = socket(AF_INET, SOCK_STREAM, IPPROTO_IP); 
    if (listen_sock < 0) {
        ESP_LOGE(TAG, "无法创建套接字: errno %d", errno);
        vTaskDelete(NULL);  
    }
    // int opt = 1;
    // setsockopt(listen_sock, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));   //设置套接口的选项

    ESP_LOGI(TAG, "套接字创建成功");


    // 将套接字标记为非阻塞
    int flags = fcntl(listen_sock, F_GETFL);
    if (fcntl(listen_sock, F_SETFL, flags | O_NONBLOCK) == -1) {
        ESP_LOGI(TAG, "无法将套接字设置为非阻塞");
    }
   
    /**
    int bind(int sockfd, const struct sockaddr *addr, socklen_t addrlen);
    把一个地址族中的特定地址赋给socket
    sockfd:socket描述字，也就是socket引用
    addr:要绑定给sockfd的协议地址
    addrlen:地址的长度
        通常服务器在启动的时候都会绑定一个地址（如ip地址+端口号），用于提供服务。
    有些端口号是约定俗成的不能乱用，如80用作http，502用作modbus。
    */
    int err = bind(listen_sock, (struct sockaddr *)&dest_addr, sizeof(dest_addr));
    if (err != 0) {
        ESP_LOGE(TAG, "套接字无法绑定: errno %d ", errno);    
    }
    ESP_LOGI(TAG, "套接字已绑定");

    /**
    int listen(int sockfd, int backlog);
    监听socket
    sockfd:要监听的socket描述字
    backlog:相应socket可以排队的最大连接个数 
    将待处理连接的队列（积压）设置为 1 个（可以更多）
    */
    err = listen(listen_sock, 1);
    if (err != 0) {
        ESP_LOGE(TAG, "监听时发生错误: errno %d", errno);    
    }

    struct sockaddr_storage source_addr; // 客户方地址
    socklen_t addr_len = sizeof(source_addr);

    /**设置所有套接字为空闲*/
    const size_t max_socks = CONFIG_LWIP_MAX_SOCKETS - 1;
    static int sock[CONFIG_LWIP_MAX_SOCKETS - 1];
    int socks_index = 0;
    for (socks_index=0; socks_index<max_socks; socks_index++) {
        sock[socks_index] = INVALID_SOCK;
    }

    hid_msg_t msg;
    
    while (1) {

        /**找到一个空闲的插座*/       
        for (socks_index=0; socks_index<max_socks; socks_index++) {
            if (sock[socks_index] == INVALID_SOCK) {
                break;
            }
        }

        /**仅当我们有空闲套接字时我们才接受新连接*/
        if (socks_index < max_socks) {
            sock[socks_index] = accept(listen_sock, (struct sockaddr *)&source_addr, &addr_len);
            if (sock[socks_index] < 0) {
                if (errno == EWOULDBLOCK) { 
                    // 侦听器不接受任何连接
                    ESP_LOGV(TAG, "没有待处理的连接...");
                }
            } else {
                /**
                 * 当有新的客户端连接时 
                 * 将客户端ip地址转换为字符串
                 */ 
                if (source_addr.ss_family == PF_INET) {
                    inet_ntoa_r(((struct sockaddr_in *)&source_addr)->sin_addr, addr_str, sizeof(addr_str) - 1);
                }
                ESP_LOGI(TAG, "套接字 %d 接受的 IP 地址： %s", sock[socks_index], addr_str);

                 /**将客户端的套接字设置为非阻塞*/
                flags = fcntl(sock[socks_index], F_GETFL);
                if (fcntl(sock[socks_index], F_SETFL, flags | O_NONBLOCK) == -1) {
                    ESP_LOGI(TAG, "无法将套接字%d设置为非阻塞",sock[socks_index]);
                }
                ESP_LOGI(TAG, "套接字%d标记为非阻塞", sock[socks_index]);
            }
        }

                   
        /**在此循环中为所有连接的客户端提供服务 轮训接收非阻塞*/
        for (int i=0; i<max_socks; ++i) {
            if (sock[i] != INVALID_SOCK) {

                int len = socketReceive(TAG, sock[i], rx_buffer, sizeof(rx_buffer));
                if (len < 0) {
                    // 该客户端的套接字出现错误 -> 关闭并标记无效
                    ESP_LOGI(TAG, "[Sock=%d]: 读取时错误 %d ", sock[i], len);
                    close(sock[i]);
                    sock[i] = INVALID_SOCK;
                } else if (len > 0) {
                    /**数据加入到队列*/
                    int size = 0;
                    
                    while (len > 0)
                    {
                        msg.length = rx_buffer[size];
                        if (msg.length == 8 )
                        {
                            // ESP_LOGI(TAG, "键盘报告");
                            //从接收缓冲区copy数据
                            memcpy(keyboardReporter,rx_buffer + size + 1,msg.length);
                            msg.modifier  = keyboardReporter[0];//1               
                            msg.packet[0] = keyboardReporter[2];//3
                            msg.packet[1] = keyboardReporter[3];//4
                            msg.packet[2] = keyboardReporter[4];//5
                            msg.packet[3] = keyboardReporter[5];//6
                            msg.packet[4] = keyboardReporter[6];//7
                            msg.packet[5] = keyboardReporter[7];//8
                            size = size + msg.length + 1;
                            len = len - msg.length - 1;
                            if (xQueueSend(usbHidQueue, &msg, pdMS_TO_TICKS(10)) != pdTRUE) {
                                 ESP_LOGE(TAG, "发送HID_USB消息失败或超时");
                            }

                        }else if (msg.length == 5)
                        {
                            // ESP_LOGI(TAG, "鼠标报告");
                            memcpy(mouseReporter,rx_buffer + size + 1,msg.length);//
                            msg.buttons = mouseReporter[0];
                            msg.x = mouseReporter[1];
                            msg.y = mouseReporter[2];
                            msg.vertical = mouseReporter[3];
                            msg.horizontal = mouseReporter[4];
                            size = size + msg.length + 1;
                            len = len - msg.length -1;
                            if (xQueueSend(usbHidQueue, &msg, pdMS_TO_TICKS(10)) != pdTRUE) {
                                 ESP_LOGE(TAG, "发送HID_USB消息失败或超时");
                            }
                            
                           
                        }
                       
                         /**向客户端发送收到了数据  */
                    if ((send(sock[i], tx_buffer, strlen(tx_buffer), 0)) < 0) {
                        ESP_LOGW(TAG, "[Socket%d]: 发送过程中发生错误", sock[i]);
                        break;
                    }
                    }
                       
                  
                }

            } 
        } 

        vTaskDelay(pdMS_TO_TICKS(20));
       
    }



    vTaskDelete(NULL);
}