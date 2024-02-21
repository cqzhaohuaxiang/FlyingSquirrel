package com.android.umbrellacorporation.tools


import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity



/**乱七八糟的一些功能*/

@SuppressLint("UnspecifiedRegisterReceiverFlag")
class Tools (context: Context){
    private var context : Context

    init {
        this.context = context

    }

    /**注册广播接收器 */
    fun registeredBroadcast(){
        val msg = IntentFilter()
        msg.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION) //网络状态变化
        msg.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION) //wifi开关变化
        msg.addAction("android.net.conn.CONNECTIVITY_CHANGE")
        msg.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED") //监听热点的状态
        context.registerReceiver(WifiSwitch(), msg) //注册广播接收
        msg.addAction("NoDevices") //用来提示设备状态
        msg.addAction("NetworkError") //用来服务返回结果
        context.registerReceiver(MsgPrompts(), msg)
    }
//    注销广播
    fun cancellationBroadcast(){
        context.unregisterReceiver(WifiSwitch())
        context.unregisterReceiver(MsgPrompts())
    }
    /**隐藏虚拟按键*/
    fun setHideVirtualKeys(window : Window) {
        val view  = window.decorView
        when(Build.VERSION.SDK_INT){
            in 12..18-> {
                view.systemUiVisibility = View.GONE
            }
            else ->{
                val uiOptions = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN)
                view.systemUiVisibility = uiOptions
            }
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) //不自动锁屏
    }

    /***  获取网络状态
     * 返回类型
     * TYPE_MOBILE = 0  移动数据连接
     * TYPE_WIFI  = 1   WIFI数据连接
     * TYPE_MOBILE_MMS  = 2 特定于彩信的移动数据连接。
     * TYPE_MOBILE_SUPL = 3     特定于 SUPL 的移动数据连接。
     * TYPE_MOBILE_DUN  = 4     特定于 DUN 的移动数据连接
     * TYPE_MOBILE_HIPRI = 5    高优先级移动数据连接。
     * TYPE_WIMAX       = 6     WiMAX 数据连接。
     * TYPE_BLUETOOTH   = 7     蓝牙数据连接。
     * TYPE_DUMMY       = 8     虚假数据连接。 这不应在运输设备上使用。
     * TYPE_ETHERNET    = 9     以太网数据连接。
     * TYPE_MOBILE_FOTA = 10    无线管理。
     * TYPE_MOBILE_IMS  = 11    IP 多媒体子系统。
     * TYPE_MOBILE_CBS  = 12    运营商品牌服务。
     * TYPE_WIFI_P2P    = 13    Wi-Fi p2p 连接。只有请求进程才能访问已连接的对等方。
     * TYPE_MOBILE_IA = 14      用于初始连接到网络的网络
     * TYPE_MOBILE_EMERGENCY = 15  用于紧急服务的紧急 PDN 连接。 这可能包括 IMS 和 MMS。
     * TYPE_PROXY = 16          使用代理实现连接的网络。
     * TYPE_VPN = 17            使用一个或多个本机承载的虚拟网络。它可能提供也可能不提供安全服务。
     * TYPE_TEST = 18           专门用于测试的网络
     * */
    fun getNetWorkInfo(): Int {
        var networkType = -1
        val connectivityManager =
            context.getSystemService(AppCompatActivity.CONNECTIVITY_SERVICE) as ConnectivityManager
        //通过网络管理类的实例得到联网日志的状态，返回联网日志的实例
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
            ?: return networkType //状态为空当前网络异常
        networkType = activeNetworkInfo.type
        ConnectivityManager.TYPE_MOBILE
//                ConnectivityManager.TYPE_WIFI
        return networkType
    }


}