package com.android.umbrellacorporation.tools



import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.android.umbrellacorporation.R

/**错误信息提示*/

class MsgPrompts : BroadcastReceiver()  {

    private var timeStamp  = SystemClock.uptimeMillis()


    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.action.let {
            when(it){
                /** 无服务设备的时候 提示一下 6秒内不重复*/
                "NoDevices" -> {
                    if((SystemClock.uptimeMillis() - timeStamp) > 6000 ){
                        val toast = Toast.makeText(context, null, Toast.LENGTH_SHORT)
                        val layout = toast.view as LinearLayout?
                        layout!!.setBackgroundResource(R.color.transparent)  //设置背景
                        val text = layout.getChildAt(0) as TextView
                        text.textSize = 20f  //设置字体大小
                        text.setTextColor(Color.YELLOW)//设置字体颜色
                        toast.setGravity(Gravity.CENTER, 0, 0)//显示的位置
                        toast.setText(R.string.NoDevices)  //显示内容
                        toast.show()
                        timeStamp  = SystemClock.uptimeMillis()
                    }

                }
                /** 收到信息不正确时提示一下*/
                "NetworkError" -> {
                    if((SystemClock.uptimeMillis() - timeStamp) > 6000 ){
                        val toast = Toast.makeText(context, null, Toast.LENGTH_SHORT)
                        val layout = toast.view as LinearLayout?
                        layout!!.setBackgroundResource(R.color.transparent)  //设置背景
                        val text = layout.getChildAt(0) as TextView
                        text.textSize = 20f  //设置字体大小
                        text.setTextColor(Color.YELLOW)//设置字体颜色
                        toast.setGravity(Gravity.CENTER, 0, 0)//显示的位置
                        toast.setText(R.string.NetworkError)  //显示内容
                        toast.show()
                        timeStamp  = SystemClock.uptimeMillis()
                    }
                }
                /** 其他 */
            }

        }


    }
}