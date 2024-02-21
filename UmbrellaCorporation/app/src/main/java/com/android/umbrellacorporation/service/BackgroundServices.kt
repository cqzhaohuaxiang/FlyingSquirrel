package com.android.umbrellacorporation.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.android.umbrellacorporation.tools.Tools
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import java.io.IOException
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.Socket
import java.util.concurrent.LinkedBlockingQueue


/** 后台任务
 * 报告符数据的发送
 * 数据库相关的操作
 * 推理结果返回
 * **/
class BackgroundServices : Service() {


    private var reporterSymbolsQueue: LinkedBlockingQueue<ByteArray>? = null  //报告符队列
    private var binder = LocalBinder(this)

    private var multicastSocket : MulticastSocket? = null
    private var multicast = true
    private var serviceEquipment = false
    private var packet: DatagramPacket? = null //从这儿取出键盘鼠标服务的设备ip地址
    private var socket: Socket? = null
    private val port : Int = 36870
    private lateinit var tools : Tools
    private var longKey = false
    class LocalBinder(private val myService: BackgroundServices) : Binder(){
        fun getService() : BackgroundServices {
            return myService
        }
    }
    /**
     * 首次创建服务时，系统将调用此方法来执行一次性设置程序（在调用 onStartCommand() 或 onBind() 之前）。
     * 如果服务已在运行，则不会调用此方法。该方法只被调用一次
     */
    override fun onCreate() {
        super.onCreate()
        Log.d("Service","onCreate 首次创建服务时")
        /**新建一个待发送的报告符数据队列 */
        reporterSymbolsQueue = LinkedBlockingQueue()
        tools =Tools(this)
        getServiceEquipment()

    }

    /** 每次通过 startService()方法启动Service时都会被回调。 */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        Log.d("Service","onStartCommand 通过 startService()方法启动 ")

        //其他初始化操作...

        return super.onStartCommand(intent, flags, startId)
    }




    /** 绑定服务时才会调用  必须要实现的方法 */
    override fun onBind(intent: Intent?): IBinder? {


        Log.d("Service","绑定服务时才会调用")
        return binder
    }



    /**解除绑定时调用 */
    override fun onUnbind(intent: Intent?): Boolean {
//        Log.d("Service","解除绑定时调用")

        return super.onUnbind(intent)
    }
    /**服务销毁时的回调 */
    override fun onDestroy() {
        reporterSymbolsQueue = null
        socket?.close()
        super.onDestroy()
//        Log.d("Service","服务销毁时的回调")
    }



    /** 报告符数据扔到队列中去 */
    fun reporterData(date: ByteArray)  {
        val queueData = ByteArray(date.size )
        System.arraycopy(date, 0, queueData, 0, date.size)//将报告符数据放入queueData数组
        reporterSymbolsQueue?.put(queueData) //提交数据到队列

        if(!serviceEquipment){
            /**发送意图*/
            val intent = Intent()
            intent.action = "NoDevices"
            sendBroadcast(intent)
        }

    }

    /** 报告符数据扔到队列中去 */
    fun reporterDataKeyboardLong(date: ByteArray,state : Boolean) {
        longKey = state

        Thread {
            while (longKey){
                val queueData = ByteArray(date.size )
                System.arraycopy(date, 0, queueData, 0, date.size)//将报告符数据放入queueData数组
                reporterSymbolsQueue?.put(queueData) //提交数据到队列

                if(!serviceEquipment){
                    /**发送意图*/
                    val intent = Intent()
                    intent.action = "NoDevices"
                    sendBroadcast(intent)
                }
                Thread.sleep(50) //显示字符快慢 (不要太快了)

            }
        }.start()

    }



    /**返回脸部推理结果
     * faceLandmarks 坐标点
     * faceBlendshapes  混合形状五官
     * facialTransformationMatrixes 面部转换矩阵
     * */
    fun cameraResultsFace(faceResult: FaceLandmarkerResult){
        if(faceResult.faceLandmarks().isNotEmpty()){
        /**faceLandmarks全部数据点为478个*/
//            Log.d("Service","第27   ${faceResults.faceBlendshapes().get()[0][27].categoryName()}有几分${faceResults.faceBlendshapes().get()[0][27].score()}")
        }


    }


    private fun getServiceEquipment() {

        Thread{
            while (multicast){

                if(tools.getNetWorkInfo() == 1){
                    try {
                        Log.d("网络","创建组播中")
                        val group = InetAddress.getByName("232.10.11.12")
                        multicastSocket = MulticastSocket(port)
                        multicastSocket?.loopbackMode = true //禁用组播环回
                        multicastSocket?.joinGroup(group) //加入到组播组
                        val buffer = ByteArray(64)
                        while (multicast){
                            Log.d("网络","接收组播数据中")
                            packet = DatagramPacket(buffer, buffer.size)
                            multicastSocket?.receive(packet) //接收组播数据报(此处为阻塞)
                            val msg =  String(packet!!.data, 0, packet!!.length)
                            if(msg == "I am a keyboard and mouse service"){
                                serviceEquipment = true
                                multicast = false
                                sendDataSocket()
                                multicastSocket?.close()
                            }

                        }

                    }catch(e: IOException) {
                        Log.d("网络","组播错误： $e")
                        multicastSocket?.close()
                        packet = null
                        serviceEquipment = false
                        multicast = true
                        getServiceEquipment()
                    }finally{
                        Log.d("网络","组播 一定会到时这儿的")
                    }


                }
                Thread.sleep(100)
            }




        }.start()
    }

    private fun sendDataSocket() {
        Thread{

            try {
                socket = Socket(packet?.address, port)
                socket?.soTimeout = 1000
                val output = socket?.getOutputStream() //二进制方式
//                val output = OutputStreamWriter(socket?.getOutputStream())//通过 \n 防粘包 按行读取 不太好
                val input = socket?.getInputStream()
                val returnInformation = ByteArray(128)

                if(socket!!.isConnected){
                    /**取队列中的数据*/
                    while (serviceEquipment){
                        val buffer = reporterSymbolsQueue?.take() //阻塞 取全部的数据
                        /**通过socket发送数据
                         * 在要发送的数据前面加上数据长度
                         * 防接收粘包
                         * */
                        val size : Int? = buffer?.size
                        if (size != null) {
                            val bufferReporter = ByteArray(size + 1)
                            bufferReporter[0] = size.toByte()
                            System.arraycopy(buffer, 0, bufferReporter, 1, size)
                            output?.write(bufferReporter)
                            output?.flush()// 刷新缓冲区

                            //接收数据 应为 Message OK
                            val len = input?.read(returnInformation);
                            val msg = len?.let { String(returnInformation, 0, it) }

                            if (msg != "Message OK"){
                                val intent = Intent()
                                intent.action = "NetworkError"
                                sendBroadcast(intent)
                            }
                        }
//                        Log.d("socket", buffer.toString() + "\n")
                    }

                }

            }catch (e: IOException){
                Log.d("网络"," socket error  $e")
                val intent = Intent()
                intent.action = "NetworkError"
                sendBroadcast(intent)
                reporterSymbolsQueue?.clear()//从此队列中删除所有元素
                socket?.close()
                packet = null
                serviceEquipment = false
                multicast = true
                getServiceEquipment()

            }

        }.start()
    }




}

