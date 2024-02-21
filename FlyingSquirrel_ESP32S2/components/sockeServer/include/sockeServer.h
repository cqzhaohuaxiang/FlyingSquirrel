#pragma once

#include "esp_err.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/queue.h"
#include "freertos/semphr.h" 

#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
    int8_t length; 
    uint8_t modifier;           //键盘组合键 
    int8_t packet[6];           //键盘按下的键
    uint8_t buttons;            //鼠标按键
    int8_t  x;                  //鼠标X方向
    int8_t  y;                  //鼠标Y方向
    int8_t  vertical;           //鼠标垂直滚动
    int8_t  horizontal;         //鼠标水平滚动  
} hid_msg_t;

/**开启AP的名称与密码 **/
#define WIFI_SSID      "FlyingSquirrel"
#define WIFI_PASS      "12345678"

/**服务端口号**/
#define PORT                    36870   
#define LISTEN_ALL_IF   1 //所有接口（仅限 IPV4）
#define MULTICAST_TTL 1 //设置组播数据包的 TTL 字段。与单播和广播 TTL 不同。 数据报生存时间
#define UDP_IPV4_ADDR    "232.10.11.12"   //多播组地址设置

#define INVALID_SOCK (-1)   //套接字空闲标记

#define HID_QUEUE_LENGTH (30) //报告符队列大小

void tcpServerTask(void *pvParameters);
void udpServerTask(void *pvParameters);
void usbHidTask(void *pvParameters);
esp_err_t wifiInit(void);

#ifdef __cplusplus
}
#endif