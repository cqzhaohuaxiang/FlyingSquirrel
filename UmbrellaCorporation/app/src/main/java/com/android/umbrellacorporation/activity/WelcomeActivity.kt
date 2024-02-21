package com.android.umbrellacorporation.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.appcompat.app.AppCompatActivity
import com.android.umbrellacorporation.R
import com.android.umbrellacorporation.tools.Tools


class WelcomeActivity : AppCompatActivity() {


    private var runnable: Runnable? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)
        val tool = Tools(this)
        tool.setHideVirtualKeys(window)


        val handler: Handler = object : Handler(Looper.getMainLooper()) {
            @SuppressLint("HandlerLeak")
            override fun handleMessage(msg: Message) {
                //正常操作
            }
        }
        handler.postDelayed(Runnable {
            val intent = Intent(this, MouseActivity::class.java)
            startActivity(intent)
            finish()//销毁欢迎页面
        }.also { runnable = it }, 1000) //延迟发送handler信息



    }
}