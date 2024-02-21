package com.android.umbrellacorporation.activity


import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.android.umbrellacorporation.R
import com.android.umbrellacorporation.tools.AnalyseIdentifyOutcome
import com.android.umbrellacorporation.tools.PathGeneration
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import kotlin.math.min



class ReasoningResultsView (context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {
    private val divisor = 8f//绘图缩小因子
    private var faceResults: FaceLandmarkerResult? = null    //脸部结果
    private var handResults: GestureRecognizerResult? = null
    private var path = PathGeneration()
//    private var outcome = AnalyseIdentifyOutcome()
    private val svgPaint = Paint()
    private val textPaint = Paint()
    private val redPointPaint = Paint()
    private val linePaint = Paint()
    private val whitePointPaint = Paint()

    private var imageWidth: Int = 1
    private var imageHeight: Int = 1


    private var handTimeStamp: Long = 0
    private var faceTimeStamp: Long = 0
    private var handHardware : String = ""                    //硬件类型
    private var faceHardware : String = ""                    //硬件类型

    private var clearCanvas = false


    private var screenOrientation = 0

    /**画图路径数据*/
    private val body = "m 597.3,960 c -35.53333,0 -71.06667,0 -106.6,0 -2.17715,-42.14809 -7.17592,-100.75655 38.2445,-122.23553 41.32892,-13.61834 95.20201,-13.34884 123.1368,25.82233 C 679.53226,903.11956 644.63343,962.38812 597.3,960 Z m -64,-42.7 c 27.59102,10.65702 113.42224,-3.40079 65.18193,-40.58137 C 570.9167,864.87049 518.81036,877.72429 533.3,917.3 Z m 0,42.7 c -50.28498,-6.46212 -109.85912,16.69943 -152.283,-20.32012 -41.06121,-36.53777 -6.76459,-105.3547 44.31694,-108.99667 41.20671,-5.41814 94.78891,3.54856 106.3165,51.07623 C 538.58925,907.21863 530.66461,934.02867 533.3,960 Z M 426.7,874.7 c -58.67661,32.94954 66.91377,76.25027 62.58859,22.46341 C 480.83423,874.31591 447.73703,864.12378 426.7,874.7 Z m 473.88327,-27.50478 c -29.42088,-44.5139 18.17524,-91.5901 21.70336,-137.48067 C 927.81612,648.68066 920.28211,587.1592 921.1,525.9 c 34.12046,18.42504 23.7827,72.77261 35.43149,107.03249 6.80549,50.45976 19.99825,103.7395 6.38457,153.70928 -14.64972,25.35407 -36.45897,46.78139 -62.33279,60.55345 z M 374.8,920.2 C 272.1523,870.47098 220.19426,746.75143 237.34985,636.87772 c 44.02875,-43.33953 45.98186,54.80535 42.82616,82.98949 9.46961,68.55944 54.19615,128.83744 111.57851,165.82998 10.66055,12.58283 -0.005,35.90505 -16.95452,34.50281 z m 275.3,-0.6 c -44.86944,-48.22515 51.55605,-70.13825 62.0942,-113.32926 31.57002,-50.04925 36.65123,-111.23232 35.2856,-168.98319 41.82415,-44.27112 57.56506,48.49838 40.14203,76.68728 C 776.61279,798.1926 729.12092,882.49327 650.1,919.6 Z M 421.5,561 c -85.17762,-12.90373 -170.18705,-26.88234 -255.3,-40.2 -45.49638,-44.52517 50.485,-52.21096 76.95637,-31.82703 63.39511,10.3119 127.1605,18.51278 190.13338,31.29318 C 450.59834,530.28218 442.82038,563.3576 421.5,561 Z m -258.2,50.6 c -51.64619,-39.97638 46.00114,-58.01772 75.99067,-44.94806 59.53644,-1.05065 119.07289,-2.1013 178.60933,-3.15194 52.49645,37.73343 -43.14248,60.02436 -73.00044,44.98946 C 284.36713,609.56884 223.83601,610.73399 163.3,611.6 Z M 613.2,561.2 C 563.91139,518.32248 662.57807,500.5546 694.60928,504.3612 750.27285,495.5408 805.93642,486.72039 861.6,477.9 918.74815,508.48144 826.45629,543.09236 794.82955,532.62583 734.30082,542.24162 673.80018,552.04071 613.2,561.2 Z m 248.5,50.6 c -85.13358,-1.48589 -170.26663,-3.00235 -255.4,-4.5 -51.30108,-37.83456 42.79498,-58.82664 71.88842,-42.26847 64.36356,1.43415 128.81385,0.47314 193.08922,4.71293 19.42789,8.3967 13.06053,44.59705 -9.57764,42.05554 z m -25.8,276.8 c -47.44648,-5.30863 -128.48795,27.64357 -152.75604,-20.46933 9.59715,-53.35141 73.17202,-36.58734 111.08384,-33.56769 54.07495,-0.70801 102.30934,-31.20279 137.80722,-69.87592 51.0912,-17.31493 26.6857,65.00521 -0.0202,80.24041 C 908.1302,872.18328 872.32799,889.21218 835.9,888.6 Z M 549.2,591.5 C 589.06191,534.36939 461.22817,500.77158 468.58965,571.58594 468.55871,610.8349 532.08971,629.45385 549.2,591.5 Z m 70.2,-185 c -70.22578,-8.84576 28.92579,-99.72354 27.33978,-24.52448 C 644.97553,395.09013 633.41107,407.74551 619.4,406.5 Z m 0,-42.6 c -48.60216,13.43642 37.84946,43.57162 7.53499,5.59931 z m -21.3,14.9 c 15.80642,63.2796 72.11127,-50.18304 6.30567,-13.34779 l -3.91823,6.3243 z m -192,27.7 c -70.22578,-8.84576 28.92579,-99.72354 27.33978,-24.52448 C 431.67553,395.09013 420.11107,407.74551 406.1,406.5 Z m 0,-42.6 c -48.58861,13.40503 37.81846,43.59586 7.52941,5.6064 z m -21.3,14.9 c 15.80642,63.2796 72.11127,-50.18304 6.30567,-13.34779 l -3.91822,6.3243 z M 514.9,711.4 C 455.50661,705.28405 398.27503,686.37575 339.10238,678.86056 281.41134,665.68409 214.49223,641.04737 191.68406,581.15358 190.48901,533.10295 252.70824,499.24271 219.12203,448.19606 194.04867,288.54815 325.10224,124.70773 486.65467,114.71522 625.84589,101.15599 769.64494,195.55894 803.06533,333.28039 818.80197,389.91659 817.55985,452.23655 794.1,506.6 c 56.0504,27.03157 43.23981,118.32415 -13.82673,135.97031 -70.73344,45.04892 -158.11272,41.83243 -236.02792,66.27167 -9.69674,1.59766 -19.51452,2.51 -29.34535,2.55802 z m 0,-554.7 c -137.51507,-4.52038 -263.17737,123.27005 -256.09969,260.72733 -8.22232,43.38598 56.67959,102.6503 -7.92891,125.86771 -37.56218,49.68076 40.78299,82.65988 80.58575,89.87212 58.07157,14.82563 118.43981,19.99808 176.35439,34.62682 58.77026,-1.04468 113.63944,-26.24086 171.80214,-31.87791 38.86259,-8.29614 126.066,-31.5385 96.25718,-84.68741 C 744.16438,527.1441 737.47093,492.71976 763.77131,463.80841 801.08066,322.52341 685.75094,166.19996 540.20266,157.90846 531.79551,157.08571 523.34736,156.68005 514.9,156.7 Z m -293,251.1 C 106.06724,390.17466 43.400614,233.52332 116.45029,141.30039 c 53.24489,-75.944618 172.2263,-95.095476 245.00168,-36.44863 41.05786,5.53706 40.07871,91.68032 -4.29227,56.72052 -53.66193,-75.110019 -188.36474,-58.96576 -219.76656,29.15753 -28.72537,67.70478 11.42587,156.62139 84.80077,173.24601 14.34799,11.17651 0.87837,29.9712 -0.29391,43.82418 z m 581.6,-0.2 C 774.48626,351.96986 865.37617,351.27841 877.71381,307.04268 918.42243,242.46303 888.34446,143.83417 813.13899,120.97749 766.18203,103.48667 705.32122,113.37279 675.4914,156.46801 624.61685,186.74987 630.97327,94.428852 675.63224,97.600008 768.9876,29.800602 920.93233,94.564648 936.49809,209.06575 953.14676,295.88587 892.27647,392.6336 803.5,407.6 Z"
    private val rightEyes = "m 809.87295,408.2363 c 36.50379,-11.90445 72.40946,-29.79224 95.26909,-61.83948 19.79549,-28.20065 34.81437,-60.44273 35.77787,-95.4846 4.42351,-38.65537 -9.81632,-77.49731 -28.25266,-110.57683 C 888.68682,112.43981 856.91437,91.609574 822.87007,78.35593 788.20967,63.231154 752.01078,67.851427 716.04033,74.511541 685.1832,85.235058 650.60938,96.55218 634.36959,127.39291 c -12.27185,24.33445 40.93611,32.69418 55.83894,50.74022 39.11037,32.44153 76.18995,70.12099 93.96967,118.77422 13.74356,35.79913 19.38237,73.6964 25.69475,111.32895 z"
    private val leftEyes = "m 216,407.33333 c -35.03069,-7.3684 -65.05814,-29.97807 -89.35543,-55.3251 -21.33333,-26.84461 -32.601272,-58.64122 -39.975305,-91.70728 -6.209027,-33.42932 -0.486603,-70.00939 16.255125,-99.53317 28.34936,-43.08394 71.17088,-78.983616 123.48909,-86.434589 43.81707,-5.995659 91.06071,-1.494463 127.65612,25.491295 24.35393,4.206254 58.07275,42.270084 17.40327,50.722274 -41.63715,24.29254 -74.06905,61.10822 -103.05245,98.92782 -24.99101,42.65414 -42.23292,89.60109 -48.64801,138.7272 -1.25747,6.37719 -2.51494,12.75437 -3.77241,19.13155 z"
    private val nose = "M 549.2,591.5 C 561,571.1 554,545 533.6,533.2 c -6.5,-3.7 -13.8,-5.7 -21.3,-5.7 -14.03371,0 -26.47619,6.7539 -34.25747,17.19172 -5.30422,7.1151 -8.44253,15.94199 -8.44253,25.50828 0,7.5 2,14.8 5.7,21.3 11.8,20.4 37.9,27.4 58.3,15.6 6.5,-3.7 11.9,-9.1 15.6,-15.6 z"



    init {
        initPaints()
    }
    /**画笔设置*/
    private fun initPaints() {
        linePaint.color = ContextCompat.getColor(context!!, R.color.white)
        linePaint.strokeWidth = 1f
        linePaint.style = Paint.Style.STROKE

        redPointPaint.color = ContextCompat.getColor(context!!, R.color.red)
        redPointPaint.strokeWidth = 8f
        redPointPaint.style = Paint.Style.FILL

        whitePointPaint.color = ContextCompat.getColor(context!!, R.color.white)
        whitePointPaint.strokeWidth = 8f
        whitePointPaint.style = Paint.Style.FILL

        textPaint.color= ContextCompat.getColor(context!!, R.color.yellow)
        textPaint.textSize= 30f
        textPaint.style = Paint.Style.FILL


    }
    private fun clear() {
        faceResults = null
        handResults = null
        svgPaint.reset()
        textPaint.reset()
        linePaint.reset()
        redPointPaint.reset()
        whitePointPaint.reset()
        invalidate()
        initPaints()
    }


    /**绘图实现**/
    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        /**设定画布方向*/
        when(screenOrientation){
            0 -> {}
            90 -> {
                canvas.translate(0f, height.toFloat())
                canvas.rotate(-90f)
                canvas.translate((height-(width*4/3)).toFloat(),0f)
            }
            180 -> {
                canvas.translate(width.toFloat(), height.toFloat())
                canvas.rotate(180f)
                canvas.translate(0f,(height-(width*4/3)).toFloat())
            }
            270 -> {
                canvas.translate(width.toFloat(), 0f)
                canvas.rotate(90f)
            }
        }
        /**清屏*/
        if(clearCanvas){
            clearCanvas = false
            clear()
            return
        }

        /**画脸*/
        faceResults?.let { faceResult ->

            if(faceResult.faceLandmarks().isNotEmpty()){
                /**画出全部关键点*/
//                for(landmark in faceResult.faceLandmarks()) {
//                    for(normalizedLandmark in landmark) {
//                        canvas.drawPoint(normalizedLandmark.x() * imageWidth , normalizedLandmark.y() * imageHeight , pointPaint)
//
//                    }
//                }

//            faceResult.FACE_LANDMARKS_LIPS 嘴部轮廓
//            faceResult.FACE_LANDMARKS_LEFT_EYE//眼部轮廓
//            faceResult.FACE_LANDMARKS_LEFT_EYE_BROW //眉毛轮廓
//            faceResult.FACE_LANDMARKS_LEFT_IRIS//眼球轮廓
//            faceResult.FACE_LANDMARKS_FACE_OVAL//脸轮廓
//            faceResult.FACE_LANDMARKS_CONNECTORS//脸 眉毛 嘴 眼轮廓
//            faceResult.FACE_LANDMARKS_TESSELATION //全部 轮廓
                FaceLandmarker.FACE_LANDMARKS_TESSELATION.forEach {
                    canvas.drawLine(
                        faceResult.faceLandmarks()[0][it.start()].x() * imageWidth ,
                        faceResult.faceLandmarks()[0][it.start()].y() * imageHeight ,
                        faceResult.faceLandmarks()[0][it.end()].x() * imageWidth ,
                        faceResult.faceLandmarks()[0][it.end()].y() * imageHeight ,
                        linePaint)
                }
                canvas.drawText("${context.getString(R.string.FaceIdentify)}$faceHardware${context.getString(R.string.ReasoningTime)} : $faceTimeStamp ms",150f,100f,textPaint)
            } else {
                canvas.drawText(context.getString(R.string.NoFace),150f,100f,textPaint)
            }

        }

        /**画爪子*/
        handResults?.let {handResults ->
            if(handResults.landmarks().isNotEmpty()){
                /**画全部的骨格*/
                for(landmark in handResults.landmarks()) {
                    val x = handResults.landmarks()[0] [8].x() * imageWidth
                    val y = handResults.landmarks()[0] [8].y() * imageHeight
                    /**画第一只手的关键点*/
                    for(i in 0..<handResults.landmarks()[0].size){
                        canvas.drawPoint(
                            handResults.landmarks()[0][i].x() * imageWidth ,
                            handResults.landmarks()[0][i].y() * imageHeight ,
                            redPointPaint
                        )
                    }

                    /**画第一只手关键点之间连线*/
                    HandLandmarker.HAND_CONNECTIONS.forEach {
                        canvas.drawLine(
                            handResults.landmarks()[0][it.start()].x() * imageWidth ,
                            handResults.landmarks()[0][it.start()].y() * imageHeight ,
                            handResults.landmarks()[0][it.end()].x() * imageWidth ,
                            handResults.landmarks()[0][it.end()].y() * imageHeight ,
                            linePaint)
                    }
                    /**如果有两个手 则画第二只*/
                    if(handResults.landmarks().count() == 2){
                        HandLandmarker.HAND_CONNECTIONS.forEach {
                            canvas.drawLine(
                                handResults.landmarks()[1][it.start()].x() * imageWidth ,
                                handResults.landmarks()[1][it.start()].y() * imageHeight ,
                                handResults.landmarks()[1][it.end()].x() * imageWidth ,
                                handResults.landmarks()[1][it.end()].y() * imageHeight ,
                                linePaint)
                        }
                        /**画第二只手的关键点*/
                        for(i in 0..<handResults.landmarks()[1].size){
                            canvas.drawPoint(
                                handResults.landmarks()[1][i].x() * imageWidth ,
                                handResults.landmarks()[1][i].y() * imageHeight ,
                                whitePointPaint
                            )
                        }
                    }
                    if (AnalyseIdentifyOutcome.getPointingMove()) canvas.drawBitmap(BitmapFactory.decodeResource(resources, R.drawable.image_mouse_pointer),x,y,null)
                    if (AnalyseIdentifyOutcome.getWheelMove()) canvas.drawBitmap(BitmapFactory.decodeResource(resources, R.drawable.image_mouse_wheel),x,y,null)
                    canvas.drawText("${context.getString(R.string.HandIdentify)}$handHardware${context.getString(R.string.ReasoningTime)} : $handTimeStamp ms",150f,50f,textPaint)
                }
            }else {
                canvas.drawText(context.getString(R.string.NoHand),150f,50f,textPaint)
            }

        }

        svgDrawing(canvas)

    }

    fun handResults(
        handResults: GestureRecognizerResult,
        timeStamp: Long,
        image: MPImage,
        handHardware :Boolean,
        screenOrientation : Int) {
        this.handResults = handResults
        this.handTimeStamp = timeStamp
        this.imageHeight = (image.height * (min(width.toFloat() , height.toFloat()) / min(image.width,image.height))).toInt()
        this.imageWidth = (image.width * (min(width.toFloat() , height.toFloat()) / min(image.width,image.height))).toInt()
        this.handHardware = if(handHardware)  " GPU " else " CPU "
        this.screenOrientation = screenOrientation
//        outcome.handDataProcessing(handResults,image,timeStamp)
        invalidate()

    }

    fun faceResults(
        faceResults: FaceLandmarkerResult,
        timeStamp: Long,
        image: MPImage,
        faceHardware :Boolean,
        screenOrientation : Int) {
        this.faceResults = faceResults
        this.faceTimeStamp = timeStamp
        this.imageHeight =  (image.height * (min(width.toFloat() , height.toFloat()) / min(image.width,image.height))).toInt()
        this.imageWidth = (image.width * (min(width.toFloat() , height.toFloat()) / min(image.width,image.height))).toInt()
        this.faceHardware = if(faceHardware)  " GPU " else " CPU "
        this.screenOrientation = screenOrientation
//        outcome.faceDataProcessing(faceResults,image,timeStamp)
        invalidate()
    }



    fun clearCanvas () {
        clearCanvas = true
        invalidate()
    }


    /**绘图 */
    private  fun svgDrawing(canvas: Canvas){
        /**画笔设置*/
        svgPaint.color = ContextCompat.getColor(context!!, R.color.green)
        svgPaint.strokeWidth = 1f
        svgPaint.style = Paint.Style.FILL
        svgPaint.isAntiAlias = true  //平滑

        canvas.drawPath(path.getpath(body,divisor) ,svgPaint)//画出小老鼠
        if(AnalyseIdentifyOutcome.getRightButton()){
            svgPaint.color = ContextCompat.getColor(context!!, R.color.red)
            canvas.drawPath(path.getpath(rightEyes,divisor) ,svgPaint)
        }

        if(AnalyseIdentifyOutcome.getLeftButton()){
            svgPaint.color = ContextCompat.getColor(context!!, R.color.red)
            canvas.drawPath(path.getpath(leftEyes,divisor) ,svgPaint)
        }

        if(AnalyseIdentifyOutcome.getPointingMove()){

            svgPaint.color = ContextCompat.getColor(context!!, R.color.red)
            canvas.drawPath(path.getpath(nose,divisor) ,svgPaint)

        }


    }





}


