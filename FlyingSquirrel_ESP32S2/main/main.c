/*SPDX-License-Identifier: Unlicense OR CC0-1.0*/
#include "sockeServer.h"      
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/semphr.h" 
#include "esp_log.h"


static const char *TAG = "main";




void app_main(void)
{
  wifiInit();
  xTaskCreate(&usbHidTask, "usbHid", 2048, NULL, 5, NULL);
  xTaskCreate(&udpServerTask, "udpServer", 2048, NULL, 5, NULL);
  xTaskCreate(&tcpServerTask, "tcpServer", 2048, NULL, 5, NULL);

}

