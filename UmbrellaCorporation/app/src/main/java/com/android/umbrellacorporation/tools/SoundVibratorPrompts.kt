package com.android.umbrellacorporation.tools

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Vibrator
import com.android.umbrellacorporation.R


/**按键反馈*/
@SuppressLint("WrongConstant")
class SoundVibratorPrompts (context: Context?): Activity() {

    private var vibrator: Vibrator? = null
    private var soundPool: SoundPool? = null
    private var soundId = -1

    init {
        vibrator = context?.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator? //从系统服务中获取振动管理器
        /*支持的声音数量  声音类型  声音质量*/
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(16)
            .setAudioAttributes(audioAttributes)
            .build()

        /*加载声音资源文件（R.raw.message：音频文件）*/
        soundId = soundPool!!.load(context, R.raw.sound, 1)

    }

    fun play(soundMode: Boolean, vibratorMode: Boolean) {
        if ((vibrator != null) or (soundPool != null)) {
            /*检查硬件是否带有振动器*/
            if (vibrator != null && !vibratorMode and vibrator!!.hasVibrator()) {
                val time = longArrayOf(1, 500, 1, 500) // 关的时间毫秒，开的时间毫秒 （依次来）
                vibrator!!.vibrate(time, 0) //  0 循环 -1 为一次

                //当版本大于 O 时才可用
//                val effect = VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE)
//                vibrator?.vibrate(effect)

            }
            if (!soundMode) {
                /*播放 声音id 左声道 右声道 优先级  0表示不循环，-1表示循环播放 播放比率，0.5~2，一般为1*/
                soundPool!!.play(soundId, 1f, 1f, 0, 0, 1f)
            }
        }
    }

    fun stop() {
        if ((vibrator != null) or (soundPool != null)) {
            if (soundPool != null) {
                soundPool!!.stop(soundId)
            }
            vibrator!!.cancel()
        }
    }

    fun release() {
        if (soundPool != null) {
            soundPool!!.release() //回收SoundPool资源
            soundPool = null
            vibrator = null
        }
    }
}