package com.android.umbrellacorporation.tools

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.provider.Settings
import android.util.Log
import java.util.Objects

/**用途：接收wifi广播，当wifi关闭时 打开wifi
 * 如果wifi是工作在热点状态，发送打开wifi意图
 * */

/**wifi的几种状态
 * WifiManager.WIFI_STATE_DISABLING 正在关闭
 * WifiManager.WIFI_STATE_DISABLED  已经关闭
 * WifiManager.WIFI_STATE_ENABLING  正在开启
 * WifiManager.WIFI_STATE_ENABLED   已经开启
 */
class WifiSwitch : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        intent.action?.let { Log.d("wifi", it) }
        when(intent.action) {
            /** 便携式热点的状态为：10---正在关闭；11---已关闭；12---正在开启；13---已开启 **/
            "android.net.wifi.WIFI_AP_STATE_CHANGED" -> {
                /**wifi工作为热点的状态*/
                if (intent.getIntExtra("wifi_state", 0) == 13) {

                    openWifi(context)
                }
            }
            else -> {
                /**wifi 以启用 但没有连接到任何网络*/
                if (Objects.equals(intent.action, WifiManager.WIFI_STATE_CHANGED_ACTION)){
                    if(intent.getIntExtra( WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_DISABLED)
                        == WifiManager.WIFI_STATE_DISABLED){

                        openWifi(context)
                    }
                }

            }
        }

    }

    private fun openWifi(context: Context){

//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            /**Android10及以上版本不能由程序自动打开wifi 只能通过Intent 用户自己动手 */

//        }
//        else {
            /**Wifi没有打开的情况 由程序自己则打开wifi
             * 好像不太好，有些流氓
             * */
//            val openWifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
//            openWifi.isWifiEnabled = true
//        }



        val openWifi = Intent(Settings.ACTION_WIFI_SETTINGS)
        if (openWifi.resolveActivity(context.packageManager) !=null){
            context.startActivity(openWifi)
        }


    }

}