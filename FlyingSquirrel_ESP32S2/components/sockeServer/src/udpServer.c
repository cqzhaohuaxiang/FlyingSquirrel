/**
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 * 作用：用来向同网端下的设备广播自己的地址
 * 目的：便于客户端进行TCP连接
 * 
 多播程序设计的框架
    （1）建立一个socket。
    （2）然后设置多播的参数，例如超时时间TTL、本地回环许可LOOP等。
    （3）加入多播组。
    （4）发送和接收数据。
    （5）从多播组离开。
*/
#include "sockeServer.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/event_groups.h"
#include "esp_log.h"
#include "lwip/err.h"
#include "lwip/sockets.h"
#include "lwip/sys.h"
#include <lwip/netdb.h>
static const char *TAG = "udpServer";
static const char message[] = "I am a keyboard and mouse service";
static int multicast_socket = -1;
/**多播组设置*/
static int multicastGroup(int sock, bool assign_source_if)
{
    struct ip_mreq imreq = { 0 };
    struct in_addr iaddr = { 0 };
    int err = 0;
    // 配置源接口
#if LISTEN_ALL_IF
    imreq.imr_interface.s_addr = IPADDR_ANY;
#else
    esp_netif_ip_info_t ip_info = { 0 };
    err = esp_netif_get_ip_info(get_example_netif(), &ip_info);
    if (err != ESP_OK) {
        ESP_LOGE(TAG, "获取IP地址信息失败. Error 0x%x", err);
        goto Error;
    }
    inet_addr_from_ip4addr(&iaddr, &ip_info.ip);
#endif 
    /**配置监听的组播地址*/ 
    err = inet_aton(UDP_IPV4_ADDR, &imreq.imr_multiaddr.s_addr);
    if (err != 1) {
        ESP_LOGE(TAG, "多播地址 %s 无效",UDP_IPV4_ADDR);
        err = -1;
        goto Error;
    }
    ESP_LOGE(TAG, "配置的 IPV4 组播地址 %s", inet_ntoa(imreq.imr_multiaddr.s_addr));
    if (!IP_MULTICAST(ntohl(imreq.imr_multiaddr.s_addr))) {
        ESP_LOGE(TAG, "配置的 IPV4 多播地址 %s  不是有效的多播地址。 这可能行不通", UDP_IPV4_ADDR);
    }

    if (assign_source_if) {
        /**通过其 IP 分配 IPv4 组播源接口（仅当此套接字仅是 IPV4 时才需要）**/
        err = setsockopt(sock, IPPROTO_IP, IP_MULTICAST_IF, &iaddr,
                         sizeof(struct in_addr));
        if (err < 0) {
            ESP_LOGE(TAG, "无法设置 IP_MULTICAST_IF. Error %d", errno);
            goto Error;
        }
    }

    err = setsockopt(sock, IPPROTO_IP, IP_ADD_MEMBERSHIP,
                         &imreq, sizeof(struct ip_mreq));
    if (err < 0) {
        ESP_LOGE(TAG, "无法设置 IP_ADD_MEMBERSHIP. Error %d", errno);
        goto Error;
    }

 Error:
    return err;
}

/**配置ipv4多播套接字*/
static int configurationSocket(void)
{

    /**
     * socket 函数功能：创建套接字
     * 参数说明：
     * 1) 为地址族（Address Family），也就是 IP 地址类型，常用的有 AF_INET 和 AF_INET6。
     * 2) type 为数据传输方式/套接字类型，常用的有 SOCK_STREAM（流格式套接字/面向连接的套接字） 
     * SOCK_STREAM 是一种可靠的、双向的通信数据流，数据可以准确无误地到达另一台计算机，如果损坏或丢失，可以重新发送。
     * 
     * SOCK_DGRAM（数据报套接字/无连接的套接字）
     * 计算机只管传输数据，不作数据校验，如果数据在传输中损坏，或者没有到达另一台计算机，是没有办法补救的。
     * 也就是说，数据错了就错了，无法重传。
     * 因为数据报套接字所做的校验工作少，所以在传输效率方面比流格式套接字要高。
     * 
     * 3) protocol 表示传输协议，常用的有 IPPROTO_TCP 和 IPPTOTO_UDP，分别表示 TCP 传输协议和 UDP 传输协议。
     * 有了地址类型和数据传输方式，还不足以决定采用哪种协议吗？为什么还需要第三个参数呢？
     * 正如大家所想，一般情况下有了 af 和 type 两个参数就可以创建套接字了，
     * 操作系统会自动推演出协议类型，除非遇到这样的情况：有两种不同的协议支持同一种地址类型和数据传输类型。
     * 如果我们不指明使用哪种协议，操作系统是没办法自动推演的。
     * 可以将 protocol 的值设为 0，系统会自动推演出应该使用什么协议，如下所示：
     * int tcp_socket = socket(AF_INET, SOCK_STREAM, 0);  //创建TCP套接字
     * int udp_socket = socket(AF_INET, SOCK_DGRAM, 0);  //创建UDP套接字
     */
    
    int err = 0;
    multicast_socket = socket(PF_INET, SOCK_DGRAM, IPPROTO_IP);
    if (multicast_socket < 0) {
        ESP_LOGE(TAG, "创建套接字失败.Error %d", errno);
        return -1;
    }

    /**
     * bind函数功能：将监听套接字绑定到本地地址和端口上。
     * 在完成第一步创建套接字，分配了一个Socket描述符后，服务端的第二步就是使用在这个描述符用Bind绑定
     * Bind()系统调用的主要用处：
        1.服务器向系统注册它的众所周知的地址。面向连接和无连接的服务器在接受客户的请求之前都必须做这一步。 
        2.客户可为自己注册一个特定的地址，以便服务器可以用这个有效的地址送回响应。

     * 返回值:
        成功返回非负值，失败返回-1，最常见的错误一般是端口被占用。
        需要注意的是，在Linux系统中，1024以下的端口都需要root权限的程序才可以绑定

     * 参数说明：
        第一个参数sockfd为上一步创建socket时的返回值。
        第二个参数 saddr 为 sockaddr 结构体变量的指针。
        第三个参数addrlen为 saddr 变量的大小，可由 sizeof() 计算得出。                
     */ 
    struct sockaddr_in saddr = { 0 }; //创建结构体变量
    saddr.sin_family = PF_INET;//协议族，与前面Socket函数中提到的一样
    saddr.sin_port = htons(PORT); //端口号
    saddr.sin_addr.s_addr = htonl(INADDR_ANY);//iP地址 
    err = bind(multicast_socket, (struct sockaddr *)&saddr, sizeof(struct sockaddr_in));
    if (err < 0) {
        ESP_LOGE(TAG, "绑定套接字失败. Error %d", errno);
        goto err;
    }

    /**
     * setsockopt 函数设置套接字选项
     * setsockopt(int s,int level,int optname,const void *opval,socklen_t optlen)
     * 参数 s 标识套接字的描述符  (要设置那个套接字)
     * 参数 level 定义选项的级别
     * 参数 optname 要为其设置值的套接字选项， optname 参数必须是在指定级别内定义的套接字选项，否则行为未定义。
        IP_MULTICAST_TTL 允许设置超时TTL，范围为0～255之间的任何值
        IP_MULTICAST_IF 用于设置组播的默认默认网络接口，会从给定的网络接口发送，另一个网络接口会忽略此数据
        IP_MULTICAST_LOOP 用于控制数据是否回送到本地的回环接口
        IP_ADD_MEMBERSHIP 用于加入某个广播组，之后就可以向这个广播组发送数据或者从广播组接收数据。
        IP_DROP_MEMBERSHIP 用于从一个广播组中退出
        struct ip_mreq mreq;
        setsockopt(s,IPPROTP_IP,IP_DROP_MEMBERSHIP,&mreq,sizeof(sreq))
     * 
     * 参数 optval 指向指定所请求选项值的缓冲区的指针。
     * 参数 optlen optval 参数指向的缓冲区的大小（以字节为单位）。
     * 
     * */ 
    uint8_t ttl = MULTICAST_TTL; //数据报生存时间 （也就是经过多少次路由）
    setsockopt(multicast_socket, IPPROTO_IP, IP_MULTICAST_TTL, &ttl, sizeof(uint8_t));
    if (err < 0) {
        ESP_LOGE(TAG, "设置超时TTL失败. Error %d", errno);
        goto err;
    }




    /**
     * 选择该设备是否也应接收组播流量 （如果未调用 setsockopt()，则默认为 "否）
     * 
     * 启用选项，含义从设备发送的数据包也被接收本身设备。
     * */ 
    // uint8_t loopback_val = 1;
    // err = setsockopt(sock, IPPROTO_IP, IP_MULTICAST_LOOP,& , sizeof(uint8_t));
    //  if (err < 0) {
    //     ESP_LOGE(TAG, "设置接收组播流量失败. Error %d", errno);
    //     goto err;
    // }

    /**添加到多播组...*/  
    err = multicastGroup(multicast_socket, true);
    if (err < 0) {
        ESP_LOGE(TAG, "添加到多播监听组失败. Error %d", errno);
        goto err;
    }

 

    /**所有设置完成*/ 
    return multicast_socket;

err:
    close(multicast_socket);
    return -1;

}

/**发送多播数据*/
static int  mulitcastDataSendingdf(int sock){

    char sendbuf[64];
    char addrbuf[64] = { 0 };
    /**
     * snprintf() 是一个 C 语言标准库函数，用于格式化输出字符串，并将结果写入到指定的缓冲区，
     * 与 sprintf() 不同的是，snprintf() 会限制输出的字符数，避免缓冲区溢出。
     * str -- 目标字符串，用于存储格式化后的字符串的字符数组的指针。
     * size -- 字符数组的大小。
     * format -- 格式化字符串。
     * ... -- 可变参数，可变数量的参数根据 format 中的格式化指令进行格式化。
     */
    int len = snprintf(sendbuf, sizeof(sendbuf), message);
    if (len > sizeof(sendbuf)) {
        ESP_LOGE(TAG, "多播 sendfmt 缓冲区溢出！！");
        return -1;
    }

    struct addrinfo hints = {
        .ai_flags = AI_PASSIVE,
        .ai_socktype = SOCK_DGRAM,
    };
    struct addrinfo *res;
    hints.ai_family = AF_INET; // 对于 IPv4 套接字

//仅官方 TCPIP_LWIP 堆栈 (esp-lwip) 支持解析 IPv4 映射的 IPv6 地址
// 对于具有 V4 映射地址的 IPv4 套接字
// #ifdef CONFIG_ESP_NETIF_TCPIP_LWIP  
//                 hints.ai_family = AF_INET6; 
//                 hints.ai_flags |= AI_V4MAPPED;
// #endif
    int err = getaddrinfo(UDP_IPV4_ADDR, NULL,&hints,&res);
    if (err < 0) {
        ESP_LOGE(TAG, "getaddrinfo() 对于 IPV4 目标地址失败. error: %d", err);
        return -1;
    }
    if (res == 0) {
        ESP_LOGE(TAG, "getaddrinfo() 没有返回任何地址");
        return -1;
    }

    ((struct sockaddr_in *)res->ai_addr)->sin_port = htons(PORT);
    inet_ntoa_r(((struct sockaddr_in *)res->ai_addr)->sin_addr, addrbuf, sizeof(addrbuf)-1);
        // ESP_LOGI(TAG, "发送到 IPV4 多播地址 %s:%d...",  addrbuf, PORT);


    err = sendto(sock, sendbuf, len, 0, res->ai_addr, res->ai_addrlen);
    freeaddrinfo(res);
    if (err < 0) {
        ESP_LOGE(TAG, "IPV4 发送失败. errno: %d", errno);
        return -1;
    }

    return 0;
}



void udpServerTask(void *pvParameters)
{
    while (1)
    {
        multicast_socket = configurationSocket(); /**配置ipv4多播套接字*/
        if (multicast_socket < 0) {   
            vTaskDelay(100 / portTICK_PERIOD_MS);
            continue;
        }

         /**地址转换为二进制*/ 
        struct sockaddr_in sdestv4 = {
            .sin_family = PF_INET,
            .sin_port = htons(PORT),
        };
        inet_aton(UDP_IPV4_ADDR, &sdestv4.sin_addr.s_addr); 
        while (1)
        {
            mulitcastDataSendingdf(multicast_socket);
            vTaskDelay(300 / portTICK_PERIOD_MS);
            
        }

        ESP_LOGE(TAG, "关闭套接字并重新启动...");
        shutdown(multicast_socket, 0);
        close(multicast_socket);
    }
    
}