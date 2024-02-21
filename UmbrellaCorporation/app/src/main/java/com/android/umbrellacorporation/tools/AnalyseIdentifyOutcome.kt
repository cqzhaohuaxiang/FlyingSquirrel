package com.android.umbrellacorporation.tools

import android.os.SystemClock
import android.util.Log
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 分析识别结果
 *
 * 主要是提取按键状态 与 指针位置
 *
 * 目前手机端返回的关键点数据 还无法判断关键点是不是识别出来的，还是猜出来的
 * （有可能以后 visibility() presence() 这两个参数能用之后就可以判断了）
 *
 * **/
class AnalyseIdentifyOutcome {
    companion object {

        private var leftButton = false
        private var rightButton = false
        private var pointingMove = false
        private var wheelMove = false
        private val mouseTools = MouseMovementHandling()
        private var mouseReportableData = ByteArray(5)                 //鼠标报告符数据
        private var startX = 0f
        private var startY = 0f
        private var endX = 0f
        private var endY = 0f
        private var standardization = 1f    // 手 脸 的大小与 图片的大小之间的反比

        private const val EAR = 0.28 //这个参数比较重要 眼睛纵横比 0.28
        private const val frequency = 6
        private var eyeLeftTime:Long  = 0
        private var eyeLeftBlinkNumber  = 0  //左眼眨眼计数
        private var eyeRightTime:Long  = 0
        private var eyeRightBlinkNumber  = 0

        private var faceResults: FaceLandmarkerResult? = null

        private var height = 1
        private var width = 1
        /**
         * 手部数据处理
         *主要是距离的比较
         * 科学的处理方式可看这个老头的视频
         * https://toptechboy.com/category/artificial-intelligence/
         *
         * */
        fun handDataProcessing(handResults: GestureRecognizerResult, image: MPImage,timeStamp: Long){
            wheelMove = false
            pointingMove = false
            for(i in 1..4) mouseReportableData[i] = 0
            if(handResults.gestures().isNotEmpty() && handResults.landmarks().isNotEmpty() ){
                val handData = handResults.landmarks()[0]
                val viewSize = min(image.width,image.height).toFloat()
                val handSize = sqrt(abs(((handData[0].x()*image.width  - handData[5].x()*image.width ).pow(2)) + abs((handData[0].y()*image.height - handData[5].y()*image.height).pow(2))))

                standardization = (1-(handSize/viewSize)) + 1

                /** 有多少只手的坐标位置**/
                val handNumber= handResults.landmarks().count()
                /**手势结果为最先识别的那只手*/
                when(handResults.gestures()[0] [0].categoryName()){
                    "Open_Palm" -> {
                        /** 这个状态为鼠标按下   如果是一只手为左 二只为右     **/
                        when(handNumber){
                            1 -> leftButton = true//鼠标按下 左
                            2 -> rightButton = true//鼠标按下 左
                        }
                    }
                    "Closed_Fist" -> {
                        /** 这个状态为鼠标抬起   如果是一只手为左 二只为右     **/
                        when(handNumber){
                            1 -> leftButton = false//鼠标抬起 左
                            2 -> rightButton = false
                        }
                    }
                    "Victory" -> {
                        wheelMove = true //滚轮移动
                        /**位置数据要做归一化*/
                        startX = handData[8].x() * image.width
                        startY = handData[8].y() * image.height
                        endX = handData[12].x() * image.width
                        endY = handData[12].y() * image.height
                    }
                    "Pointing_Up" -> {
                        pointingMove = true //指针移动
                        startX = handData[8].x() * image.width
                        startY = handData[8].y() * image.height
                    }

                    else ->{
                        mouseTools.allMoveLeave()
                    }


                }

            }



        }

        /**人脸混合形状faceBlendshapes() 开启好像没有什么卵用 （应该是不知道怎么用）
         * faceLandmarks关键数据点为468个 加上双眼的 10 个 全部为478个
         *
         *  归一化地标表示 3D 空间中具有 x、y、z 坐标的点。x 和 y 是 分别按图像宽度和高度归一化为 [0.0， 1.0]。
         *  z 代表地标 深度，值越小，地标离相机越近。z 的大小 使用与 x 大致相同的比例。
         *
         * */
        fun faceDataProcessing(image: MPImage){
            if (faceResults != null){
                val faceOne = faceResults!!.faceLandmarks()[0]
                /**计算眨眼的论文出处我忘了记录
                 *
                 * 选择的左眼关键点   33 133        160 144     158 153
                 * 选择的右眼关键点   362 263       385 380     387 373
                 *
                val al = sqrt(abs(((faceOne[33].x()   - faceOne[133].x()).pow(2)) + abs((faceOne[33].y()    - faceOne[133].y() ).pow(2))))
                val bl = sqrt(abs(((faceOne[160].x()  - faceOne[144].x()).pow(2)) + abs((faceOne[160].y()  - faceOne[144].y() ).pow(2))))
                val cl = sqrt(abs(((faceOne[158].x() - faceOne[153].x()).pow(2)) + abs((faceOne[158].y()  - faceOne[153].y() ).pow(2))))
                val eyeLeftEAR = (bl + cl ) / (2 * al)
                Log.d("out","LeftEAR=$eyeLeftEAR")
                if(eyeLeftEAR < EAR) eyeLeftBlinkNumber++  else eyeLeftBlinkNumber--
                eyeLeftTime += timeStamp
                if(eyeLeftTime > 500  && abs(eyeLeftBlinkNumber) > frequency){
                    if(eyeLeftBlinkNumber > 0 ) {
                        pointingMove = false
                    } else if(eyeLeftBlinkNumber < 0 ){
                        pointingMove = true
                    }
                    eyeLeftBlinkNumber = 0

                }

                 * **/

            /**嘴巴的开合状态
             *
             * 关键点选取  78 308        82 87        312 317
             * */


                val crisscrossA = sqrt(abs(((faceOne[78].x() - faceOne[308].x()).pow(2)) + abs((faceOne[78].y()  - faceOne[308].y()).pow(2))))
                val crisscrossB = sqrt(abs(((faceOne[82].x() - faceOne[87].x()).pow(2)) + abs((faceOne[82].y() - faceOne[87].y()).pow(2))))
                val crisscrossC = sqrt(abs(((faceOne[312].x() - faceOne[317].x()).pow(2)) + abs((faceOne[312].y() - faceOne[317].y()).pow(2))))
                val mouthCrisscross = (crisscrossB + crisscrossC ) / (2 * crisscrossA)

                leftButton = mouthCrisscross > EAR  //鼠标左键

                /**脸的*/
                pointingMove = true
                startX = faceOne[2].x() * image.width
                startY = faceOne[2].y() * image.height

            }
        }

        fun getLeftButton():Boolean{
            return leftButton
        }
        fun getRightButton():Boolean{
            return rightButton
        }

        fun getPointingMove():Boolean{
            return pointingMove
        }
        fun getWheelMove():Boolean{
            return wheelMove
        }

        fun getMouseData(sensitivity:Int,velocity:Int,wheel:Int):ByteArray{
            for(i in 1..4) mouseReportableData[i] = 0

            if(leftButton)  mouseReportableData[0] = mouseReportableData[0].or(1)
                else  mouseReportableData[0] = mouseReportableData[0] and 1.inv().toByte()

            if(rightButton) mouseReportableData[0] = mouseReportableData[0].or((1.shl(1)).toByte())
                else  mouseReportableData[0] = mouseReportableData[0] and (1.shl(1).toUByte()).inv().toByte()

            if (pointingMove){
                val move =  mouseTools.mousePointerMove(startX,startY, SystemClock.uptimeMillis(),sensitivity,velocity,standardization)
                if(move[0] != 0.toByte() || move[1] != 0.toByte()){
                    mouseReportableData[1] = move[0]
                    mouseReportableData[2] = move[1]
                }
            }
            if (wheelMove){
                val move =  mouseTools.mouseWheelMove(startX,startY, endX,endY,wheel)
                if(move[0] != 0.toByte() || move[1] != 0.toByte()){
                    mouseReportableData[4] = move[0]
                    mouseReportableData[3] = move[1]

                }
            }

            return mouseReportableData
        }


        /**获取脸相对于屏幕的比例大小
         *
         * 返回 结果 0 不用调整
         *          1 脸小了 调大焦距
         *         -1 脸大了 缩小焦距
         * */
        fun getFaceSize(faceResults: FaceLandmarkerResult, image: MPImage):Int{
            var outcome = 0
            this.faceResults = faceResults

            if(faceResults.faceLandmarks().isNotEmpty()) {
                val faceOne = faceResults.faceLandmarks()[0]
                val faceSize =sqrt(abs(((faceOne[10].x()*image.width  - faceOne[152].x()*image.width ).pow(2))
                        + abs((faceOne[10].y()*image.height - faceOne[152].y()*image.height).pow(2))))
                val viewSize = min(image.width,image.height).toFloat()
//
                standardization = (1 - (faceSize/viewSize )) + 1

                outcome = when(faceSize/viewSize){
                    in 0f..0.3f -> 1
                    in 0.6f..1f -> -1
                    else-> 0
                }
            }else this.faceResults = null

            return outcome
        }
    }

}