package com.android.umbrellacorporation.activity

/**
 * 鼠标报告符数据内容
 *  byte 0 记录按键状态
 *      bit0: 左键    bit1: 右键   bit2: 中键    bit3: 后退键      bit4: 前进键
 *      bit5: 无  bit6: 无 bit7: 无
 *  byte 1  (左 右 指针)
 *  byte 2  ( 上 下 指针)
 *  byte 3  (上 下 滚动)
 *  byte 4  (左 右 滚动)
 **/

import android.annotation.SuppressLint
import android.app.Service
import android.content.ComponentName

import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.MotionEventCompat.getPointerId
import com.android.umbrellacorporation.R
import com.android.umbrellacorporation.service.BackgroundServices
import com.android.umbrellacorporation.tools.MouseMovementHandling
import com.android.umbrellacorporation.tools.MsgPrompts
import com.android.umbrellacorporation.tools.ParameterShared
import com.android.umbrellacorporation.tools.Tools
import com.android.umbrellacorporation.tools.WifiSwitch
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.math.*

class MouseActivity : AppCompatActivity()
                    , View.OnClickListener
                    , View.OnTouchListener
{

    private val tools = Tools(this)
    private var mService: BackgroundServices? = null    //后台服务句柄
    private var bootingServer : Intent? = null          //启动服务意图

    private lateinit var touchZone : View                      //手指的触摸区
    private lateinit var touchZoneImage : ImageView             //触摸区的图标
    private lateinit var seekBarWheel : SeekBar                //滚轮 滑动条
    private lateinit var seekBarPointer : SeekBar              //指针
    private lateinit var seekBarSensitivity : SeekBar          //灵敏度
    private lateinit var parameter : ParameterShared   //键值参数数据


    private var wheel = 5                       //鼠标滚轮速度
    private var velocity  = 10                    //鼠标指针速度
    private var sensitivity : Int = 1                            //灵敏度
    private val mouseTools = MouseMovementHandling()
    private var mouseReportableData = ByteArray(5)                 //鼠标报告符数据

    @SuppressLint("ClickableViewAccessibility", "UseCompatLoadingForDrawables",
        "UnspecifiedRegisterReceiverFlag"
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mouse)

        touchZoneImage = findViewById(R.id.mouse_touch_icon)
        seekBarWheel = findViewById(R.id.mouse_wheel)
        seekBarPointer =findViewById(R.id.mouse_pointer)
        seekBarSensitivity =findViewById(R.id.mouse_sensitivity)

        tools.registeredBroadcast() //注册广播

        /**取出灵敏度数据*/
        parameter = ParameterShared(this)
        wheel= parameter.getParameter("mouseWheelSensitivity",5) as Int
        velocity = parameter.getParameter("mousePointerSensitivity",10) as Int
        sensitivity = parameter.getParameter("mouseSensitivity",1) as Int

        /**设置显示为取处的灵敏度数据*/
        seekBarWheel.progress = wheel
        seekBarPointer.progress = velocity
        seekBarSensitivity.progress = sensitivity

        /**为滑动条注册 事件*/
        seekBarWheel.setOnSeekBarChangeListener( object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                /***进度数据已更改 什么者不做 */
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                /***进度已获取焦点 什么者不做 */
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                /*** 进度失取焦点 记录数据    */
                wheel = seekBarWheel.progress
                parameter.setParameter("mouseWheelSensitivity", wheel)
            }
        })
        seekBarPointer.setOnSeekBarChangeListener( object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                velocity = seekBarPointer.progress
                parameter.setParameter("mousePointerSensitivity", velocity)
            }
        })
        seekBarSensitivity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            //停止跟踪触摸
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                sensitivity = seekBarSensitivity.progress
                parameter.setParameter("mouseSensitivity", sensitivity)
            }
        })

        /**界面所有按键事件注册*/
        for (i in 1..6) {
            @SuppressLint("DiscouragedApi") val resId = resources.getIdentifier(
                "mouse_button$i", "id",
                packageName
            )
            if (resId != 0) {
                findViewById<Button>(resId).setOnClickListener(this)//点击监听
                findViewById<Button>(resId).setOnTouchListener(this) //触摸监听

            }
        }

        /**触摸区事件注册
         * 实现鼠标移动功能在这儿
         * */
        touchZone = findViewById(R.id.touch_zone)
        touchZone.setOnTouchListener { _, event ->
            when(event.actionMasked){
                //第一根手指按下时
                MotionEvent.ACTION_DOWN -> {
                    touchZoneImage.background = ContextCompat.getDrawable(this, R.drawable.image_mouse_pointer)

                }
                //双指按下时
                MotionEvent.ACTION_POINTER_DOWN -> {
                    touchZoneImage.background = ContextCompat.getDrawable(this, R.drawable.image_mouse_wheel)
                }
                //最后一根手指抬起时
                MotionEvent.ACTION_UP -> {
                    touchZoneImage.background = ContextCompat.getDrawable(this, R.drawable.image_mouse_not_touch)
                    mouseTools.allMoveLeave()

                }

                //手指移动时
                MotionEvent.ACTION_MOVE -> {
                    /**单根手指为*/
                    if (event.pointerCount == 1) {
                        touchZoneImage.background =  ContextCompat.getDrawable(this, R.drawable.image_mouse_pointer)

                        val move =  mouseTools.mousePointerMove(event.x,event.y, SystemClock.uptimeMillis(),sensitivity,velocity,1f)
                        if(move[0] != 0.toByte() || move[1] != 0.toByte()){
                            mouseReportableData[1] = move[0]
                            mouseReportableData[2] = move[1]
                            mService?.reporterData(mouseReportableData)
                            for(i in 1..4) mouseReportableData[i] = 0
                        }

                    }
                    /***当有双指移动时 滚动的阀值为双指按下时的距离*/
                    if (event.pointerCount == 2  ) {

                        touchZoneImage.background =  ContextCompat.getDrawable(this, R.drawable.image_mouse_wheel)
                        val move =  mouseTools.mouseWheelMove(event.x,event.y, event.getX(1),event.getY(1),wheel)
                        if(move[0] != 0.toByte() || move[1] != 0.toByte()){
                            mouseReportableData[4] = move[0]
                            mouseReportableData[3] = move[1]
                            mService?.reporterData(mouseReportableData)
                            for(i in 1..4) mouseReportableData[i] = 0
                        }

                    }
                }
            }
            true
        }





    }


    override fun onResume() {

        tools.setHideVirtualKeys(window)//隐藏虚拟按键
        /**启动服务*/
        bootingServer = Intent(this, BackgroundServices::class.java)
        startService(bootingServer)
        bindService(bootingServer!!, connection, Service.BIND_AUTO_CREATE)//绑定服务
        super.onResume()
    }


    override fun onDestroy() {
        tools.cancellationBroadcast()//    注销广播
        unbindService(connection)// 解绑服务
        stopService(bootingServer) // 停止服务
        super.onDestroy()
    }


    override fun onClick(v: View?) {
        when (v?.tag.toString()) {
            "F4" -> startActivity(Intent(this@MouseActivity, KeyboardActivity::class.java))
            "F7" -> startActivity(Intent(this@MouseActivity, CameraActivity::class.java))
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        when(event?.action){
            MotionEvent.ACTION_DOWN -> {
                when(v?.tag.toString()){
                    "F8" -> {
                        mouseReportableData[0] = mouseReportableData[0].or(1)
                        mService?.reporterData(mouseReportableData)

                    }

                    "F9" -> {
                        mouseReportableData[0] = mouseReportableData[0].or((1.shl(1)).toByte())
                        mService?.reporterData(mouseReportableData)
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                when(v?.tag.toString()){
                    "F8" -> {
                        mouseReportableData[0] = mouseReportableData[0] and 1.inv().toByte()
                        mService?.reporterData(mouseReportableData)
                    }
                    "F9" -> {
                        mouseReportableData[0] = mouseReportableData[0] and (1.shl(1).toUByte()).inv().toByte()
                        mService?.reporterData(mouseReportableData)
                    }

                }

            }
        }
        return false
    }

    /**实体按键
     *
     * onKeyUp 与 onKeyDown 事件是对全部按键的
     */
    @Suppress("INTEGER_OPERATOR_RESOLVE_WILL_CHANGE")
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        when(keyCode){
            KeyEvent.KEYCODE_VOLUME_UP -> {
                //bit1 为 0
                mouseReportableData[0] = mouseReportableData[0] and (1.shl(1).toUByte()).inv().toByte()
                mService?.reporterData(mouseReportableData)

            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                //bit0 为 0
                mouseReportableData[0] = mouseReportableData[0] and 1.inv()
                mService?.reporterData(mouseReportableData)

            }

        }

        return true
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when(keyCode){
            KeyEvent.KEYCODE_VOLUME_UP -> {
                //bit1 为 1
                mouseReportableData[0] = mouseReportableData[0].or((1.shl(1)).toByte())
                mService?.reporterData(mouseReportableData)

            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                //bit0 为 1
                mouseReportableData[0] = mouseReportableData[0].or(1)
                mService?.reporterData(mouseReportableData)

            }
        }
        return true
    }



    /** 获取后台服务句柄 */
    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder  = service as BackgroundServices.LocalBinder
            mService = binder.getService()

        }
        override fun onServiceDisconnected(arg0: ComponentName) {
            mService = null
        }
    }



}






