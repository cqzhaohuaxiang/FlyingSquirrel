package com.android.umbrellacorporation.activity
/**     键盘报告符数据内容
 *  byte 0 记录特殊的功能键
 *      bit0: 左 ctrl 键    bit1: 左 shift 键   bit2: 左 Alt 键 bit3: 左 Window 键
 *      bit4: 右 ctrl 键    bit5: 右 shift 键   bit6: 右 Alt 键 bit7: 右 Window 键
 *  byte 1 为保留功能 （应保持为 0）
 *  byte 2 ---- byte 7 每个字节存 除了特殊的功能键 以外的所有按键 最多一次有 6 个按键
 *  */
import android.annotation.SuppressLint
import android.app.Service
import android.content.ComponentName

import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.android.umbrellacorporation.R
import com.android.umbrellacorporation.service.BackgroundServices
import com.android.umbrellacorporation.tools.ParameterShared
import com.android.umbrellacorporation.tools.SoundVibratorPrompts
import com.android.umbrellacorporation.tools.Tools
import java.util.Arrays
import kotlin.experimental.or

class   KeyboardActivity : AppCompatActivity()
                            ,View.OnClickListener
                            ,View.OnTouchListener
                            ,View.OnLongClickListener
{
    private val tools = Tools(this)
    private lateinit var prompts : SoundVibratorPrompts  //提示类一定要在onCreate中实例化 因为有振动服务的获取
    private var mService: BackgroundServices? = null    //后台服务句柄
    private var bootingServer : Intent? = null          //启动服务意图

    private lateinit var parameter : ParameterShared   //键值参数数据
    private var soundSwitch = false                       //声音反馈状态
    private var vibratorSwitch= false                    //反馈状态
    private var capitalLookSwitch = false                //大小写提示

    private lateinit var buttonSound: Button
    private lateinit var buttonVibrator: Button
    private lateinit var buttonCapitalLook: Button

    private var buttonsPressCount = 2       //按钮按下计数
    private var keyboardReportableData = ByteArray(8)    //键盘报告符数据
    private var longKeyboardReportableData = ByteArray(8) //按钮长按


    @SuppressLint("ClickableViewAccessibility", "UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_keyboard)
        /**这三个按键单独获取索引 懒得用绑定方式了**/
        buttonSound = findViewById(R.id.button68)
        buttonVibrator = findViewById(R.id.button69)
        buttonCapitalLook = findViewById(R.id.button42)

        prompts  = SoundVibratorPrompts(this)
        parameter = ParameterShared(this)   //键值参数数据
        tools.registeredBroadcast() //注册广播

        /**取出键盘共享数据*/
        soundSwitch = parameter.getParameter("soundSwitch",false) as Boolean
        vibratorSwitch = parameter.getParameter("vibratorSwitch",false) as Boolean

        changeButtonStatus() //更新按键状态

        /**按键们事件注册 */
        for (i in 0..77) {
            @SuppressLint("DiscouragedApi") val resId = resources.getIdentifier(
                "button$i", "id",
                packageName
            )
            if (resId != 0) {
                findViewById<Button>(resId).setOnClickListener(this)//点击监听
                findViewById<Button>(resId).setOnTouchListener(this) //触摸监听
                findViewById<Button>(resId).setOnLongClickListener(this) //长按监听
            }
        }
    }


    override fun onResume() {
        tools.setHideVirtualKeys(window)//隐藏虚拟按键
        /**启动服务*/
        bootingServer = Intent(this@KeyboardActivity, BackgroundServices::class.java)
        startService(bootingServer)
        bindService(bootingServer!!, connection, Service.BIND_AUTO_CREATE)//绑定服务


        super.onResume()
    }

    override fun onDestroy() {
        tools.cancellationBroadcast()//    注销广播
        prompts.release() //释放提示的资源
        unbindService(connection)// 解绑服务
        stopService(bootingServer) // 停止服务
        super.onDestroy()
    }



    override fun onClick(v: View?) {
        when (v?.tag.toString()) {
            "F1" -> startActivity(Intent(this@KeyboardActivity, MouseActivity::class.java))

            "F2" -> {
                soundSwitch =! soundSwitch
                /**将声音状态保存起来 */
                parameter.setParameter("soundSwitch", soundSwitch)
                changeButtonStatus()
            }
            "F3" -> {
                /**将振动的状态保存起来 */
                vibratorSwitch = !vibratorSwitch
                parameter.setParameter("vibratorSwitch", vibratorSwitch)
                changeButtonStatus()
            }
            "39" -> {
                /**大小写转换*/
                capitalLookSwitch = !capitalLookSwitch
                changeButtonStatus()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        when(event?.action){
            MotionEvent.ACTION_DOWN -> {
                prompts.play(soundSwitch,vibratorSwitch)//播放提示
                /** 取处按键附带的数据 */
                val keyPad = java.lang.Byte.toUnsignedInt(v?.tag.toString().toInt(16).toByte())

                when(keyPad){
                    in 0x04 ..0xa4 -> {
                        buttonsPressCount++
                        if (buttonsPressCount < 8){
                            keyboardReportableData[buttonsPressCount] = keyPad.toByte()
                        }

                    }

                    0xe1 -> keyboardReportableData[0] = keyboardReportableData[0].or((1.shl(1)).toByte())
                    0xe3 -> keyboardReportableData[0] = keyboardReportableData[0].or((1.shl(3)).toByte())
                    0xe4 -> keyboardReportableData[0] = keyboardReportableData[0].or((1.shl(4)).toByte())
                    0xe5 -> keyboardReportableData[0] = keyboardReportableData[0].or((1.shl(5)).toByte())
                    0xe6 -> keyboardReportableData[0] = keyboardReportableData[0].or((1.shl(6)).toByte())


                }
            }
            MotionEvent.ACTION_UP -> {
                prompts.stop()//停用提示
                mService?.reporterDataKeyboardLong(longKeyboardReportableData,false)  //停止长按发送
                buttonsPressCount = 2
                mService?.reporterData(keyboardReportableData)  //提交数据
                Arrays.fill(keyboardReportableData, 0.toByte()) //数据全部置为0

            }
        }
        return false
    }

    override fun onLongClick(v: View?): Boolean {
        longKeyboardReportableData[2] = java.lang.Byte.toUnsignedInt(v?.tag.toString().toInt(16).toByte()).toByte()
        mService?.reporterDataKeyboardLong(longKeyboardReportableData,true)
        return false
    }


    /**更新按键状态*/
    @SuppressLint("UseCompatLoadingForDrawables", "UseCompatLoadingForColorStateLists", "SetTextI18n")
    private fun changeButtonStatus() {
        when(soundSwitch){
            true -> buttonSound.background = ContextCompat.getDrawable(this, R.drawable.styles_keyboard_button_sound_down)

            false -> buttonSound.background = ContextCompat.getDrawable(this, R.drawable.styles_keyboard_button_sound_up)
        }
        when(vibratorSwitch){
            true -> buttonVibrator.background = ContextCompat.getDrawable(this, R.drawable.styles_keyboard_button_vibrate_down)
            false -> buttonVibrator.background = ContextCompat.getDrawable(this, R.drawable.styles_keyboard_button_vibrate_up)
        }
        when(capitalLookSwitch){
            true ->{
                //文字颜色变成红色，来提示一下
                buttonCapitalLook.text = "CAPS\nLOCK"
                buttonCapitalLook.setTextColor(Color.parseColor("#FF0000"))
            }
            false -> {
                buttonCapitalLook.text = "caps\nlock"
                buttonCapitalLook.setTextColor(Color.parseColor("#74747c"))

            }
        }

    }


    /** 获取后台服务句柄 */
    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as BackgroundServices.LocalBinder
            mService = binder.getService()
        }
        override fun onServiceDisconnected(arg0: ComponentName) {
            mService = null
        }
    }


}

