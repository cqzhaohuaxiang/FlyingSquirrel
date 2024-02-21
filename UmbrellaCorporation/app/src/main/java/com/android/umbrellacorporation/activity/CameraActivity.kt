package com.android.umbrellacorporation.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.OrientationEventListener
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.android.umbrellacorporation.R
import com.android.umbrellacorporation.databinding.ActivityCameraBinding
import com.android.umbrellacorporation.service.BackgroundServices
import com.android.umbrellacorporation.tools.AnalyseIdentifyOutcome
import com.android.umbrellacorporation.tools.ParameterShared
import com.android.umbrellacorporation.tools.Tools
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random


class CameraActivity : AppCompatActivity()
{
    private lateinit var activityCameraBinding: ActivityCameraBinding //绑定 activity_camera.xml文件
    private val tools = Tools(this)
    private var mService: BackgroundServices? = null    //后台服务句柄
    private var screenOrientation = 0 //屏幕方向
    private var rotateOrientation = 270 //视图要转换的方向
    private val permissionsRequestCode = Random.nextInt(0, 10000)//伪随机数 （权限请求时比较）
    private val permissions = listOf(Manifest.permission.CAMERA) //动态申请的权限内容

    private lateinit var cameraProvider: ProcessCameraProvider
    private var preview: Preview? = null
    private var camera: Camera? = null
    private lateinit var imageAnalyzer : ImageAnalysis  //图像分析
    private lateinit var cameraSelector : CameraSelector  //镜头
    private var cameraControl: CameraControl? = null               //提供相机各种异步操作，如变焦、对焦和测光
    private var lensFacing: Int = CameraSelector.LENS_FACING_FRONT //镜头 选择

    private var cameraCurrentZoom  = 0f //相机当前缩放
    private val movementThreshold = 20  //阈值 缩放双指间的移动
    private var towDown = 0f
    private var bootingServer : Intent? = null          //启动服务意图

    private lateinit var reasoningTask: ExecutorService          // 推理任务句柄

    private var handRecognizer: GestureRecognizer? = null        //手势识别器
    private var faceRecognizer: FaceLandmarker? = null           //人脸识别器

    private var faceButton = false
    private var handButton = false
    private var reasoningButton = false
    private var flipButton = false

    private var faceButtonOperateLocked = true  //脸部按键 （不运行完上次内容 这次操作不运行）
    private var handButtonOperateLocked = true
    private var flipButtonOperateLocked = true

    private var handHardware= false       //手的推理方式
    private var faceHardware = false

    private lateinit var parameter : ParameterShared   //键值参数数据
    private var mousePointerVelocity  = 10                    //鼠标指针速度
    private var sensitivity : Int = 1                            //灵敏度


    private lateinit var cameraButtonFace : Button    //推理按键 状态
    private lateinit var cameraButtonHand : Button

//    private var outcome = AnalyseIdentifyOutcome()

    /**启动*/
    @SuppressLint("ClickableViewAccessibility", "UseCompatLoadingForDrawables",
        "UnspecifiedRegisterReceiverFlag"
    )
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        activityCameraBinding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(activityCameraBinding.root)

        orientationEventListener.enable() //开启屏幕方向监听

        /**取出数据*/
        parameter = ParameterShared(this)


        parameter.getParameter("cameraPointerSensitivity",10) as Int


        mousePointerVelocity =  parameter.getParameter("cameraPointerSensitivity",10) as Int
        sensitivity = parameter.getParameter("cameraSensitivity",1) as Int
        faceButton =  parameter.getParameter("cameraFaceButton",false) as Boolean
        handButton =  parameter.getParameter("cameraHandButton",false) as Boolean
        faceHardware = parameter.getParameter("cameraFaceHardware",false) as Boolean
        handHardware  = parameter.getParameter("cameraHandHardware",false) as Boolean
        lensFacing =  parameter.getParameter("cameraLensFacing",0) as Int


        tools.registeredBroadcast() //注册广播

        reasoningTask = Executors.newSingleThreadExecutor()


        /**滑动条*/
        val seekBarPointer =findViewById<SeekBar>(R.id.cameraPointer)
        val seekBarSensitivity =findViewById<SeekBar>(R.id.cameraSensitivity)
        /**滑动条 显示上次位置*/
        seekBarPointer.progress = mousePointerVelocity
        seekBarSensitivity.progress = sensitivity

        /**为滑动条注册 事件*/
        seekBarPointer.setOnSeekBarChangeListener( object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                /***进度数据已更改 什么者不做 */
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                /***进度已获取焦点 什么者不做 */
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                /*** 进度失取焦点 记录数据    */
                mousePointerVelocity = seekBarPointer.progress
                parameter.setParameter("cameraPointerSensitivity", mousePointerVelocity)
            }
        })
        seekBarSensitivity.setOnSeekBarChangeListener( object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                sensitivity = seekBarSensitivity.progress
                parameter.setParameter("cameraSensitivity", sensitivity)
            }
        })


        /**** 按键 索引**/
        val cameraContainer = findViewById<View>(R.id.cameraContainer)
            cameraButtonFace = findViewById(R.id.cameraButtonFace)
            cameraButtonHand = findViewById(R.id.cameraButtonHand)
        val cameraButtonHardware = findViewById<Button>(R.id.cameraButtonHardware)
        val cameraButtonFlip = findViewById<Button>(R.id.cameraButtonFlip)
        /**显示按键上次状态*/
        when(faceButton){
            true -> {
                cameraButtonFace.background =
                    ContextCompat.getDrawable(this,R.drawable.styles_mouse_buttons_stop)
            }
            false -> {
                cameraButtonFace.background =
                    ContextCompat.getDrawable(this,R.drawable.styles_mouse_buttons_up)
            }
        }
        when(handButton){
            true -> {
                cameraButtonHand.background =
                    ContextCompat.getDrawable(this,R.drawable.styles_mouse_buttons_stop)
            }
            false -> {
                cameraButtonHand.background =
                    ContextCompat.getDrawable(this,R.drawable.styles_mouse_buttons_up)
            }
        }
        /**镜头缩放*/
        cameraContainer.setOnTouchListener { _, event ->
            when(event.actionMasked){
                MotionEvent.ACTION_POINTER_DOWN -> {
                    if (event.pointerCount == 2) {
                        towDown = sqrt(abs(((event.getX(1)-event.getX(0)).pow(2)) +
                                ((event.getY(1)-event.getY(0)).pow(2))))
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if(event.pointerCount == 2){
                       val twoMove = sqrt(abs(((event.getX(1)-event.getX(0)).pow(2)) +
                               ((event.getY(1)-event.getY(0)).pow(2))))

                        if ((twoMove - towDown) > movementThreshold ){
                            cameraCurrentZoom += 0.05f
                            if(cameraCurrentZoom  > 1) cameraCurrentZoom = 1f
                            cameraControl?.setLinearZoom(cameraCurrentZoom)
                            towDown = twoMove
                        }else if ((twoMove - towDown) < -1 * movementThreshold ) {
                            cameraCurrentZoom -= 0.05f
                            if(cameraCurrentZoom < 0) cameraCurrentZoom = 0f
                            cameraControl?.setLinearZoom(cameraCurrentZoom)
                            towDown = twoMove
                        }
                    }
                }
            }
            true
        }

        cameraButtonFace.setOnTouchListener { _, event ->
            when(event.actionMasked){
                MotionEvent.ACTION_DOWN -> {
                    cameraButtonFace.background =
                            ContextCompat.getDrawable(this,R.drawable.styles_mouse_buttons_stop)

                }

                MotionEvent.ACTION_UP -> {

                    if(faceButtonOperateLocked){
                        faceButtonOperateLocked = false
                        faceButton = !faceButton
                        /**异步提交数据*/
                        parameter.setParameter("cameraFaceButton", faceButton)
                        when (faceButton) {
                            true -> {
                                reasoningTask.execute {

                                    faceRecognizer?.close()
                                    faceRecognizer = null
                                    faceHardware = when (reasoningButton) {
                                        true -> true
                                        false -> false
                                    }
                                    faceReasoningInit(this)

                                    faceButtonOperateLocked = true
                                }
                            }
                            false -> {

                                reasoningTask.execute {

                                    faceRecognizer?.close()
                                    faceRecognizer = null
                                    cameraButtonFace.background =
                                        ContextCompat.getDrawable(this,R.drawable.styles_mouse_buttons_up)

                                    activityCameraBinding.reasoningOverlay.clearCanvas()
                                    activityCameraBinding.reasoningOverlay.invalidate() // 强制重绘
                                    faceButtonOperateLocked = true
                                }

                            }
                        }

                    }
                }

            }
            true
        }

        cameraButtonHand.setOnTouchListener { _, event ->
            when(event.actionMasked){
                MotionEvent.ACTION_DOWN -> {
                    cameraButtonHand.background =
                        ContextCompat.getDrawable(this,R.drawable.styles_mouse_buttons_stop)
                }

                MotionEvent.ACTION_UP -> {

                    if(handButtonOperateLocked){
                        handButtonOperateLocked = false
                        handButton = !handButton
                        parameter.setParameter("cameraHandButton", handButton)
                        when (handButton) {
                            true -> {
                                reasoningTask.execute {

                                    handRecognizer?.close()
                                    handRecognizer = null
                                    handHardware = when (reasoningButton) {
                                        true -> true
                                        false -> false
                                    }
                                    handReasoningInit(this)

                                    handButtonOperateLocked = true
                                }
                            }
                            false -> {

                                reasoningTask.execute {

                                    handRecognizer?.close()
                                    handRecognizer = null

                                    cameraButtonHand.background =
                                        ContextCompat.getDrawable(this,R.drawable.styles_mouse_buttons_up)

                                    activityCameraBinding.reasoningOverlay.clearCanvas()
                                    activityCameraBinding.reasoningOverlay.invalidate() // 强制重绘
                                    handButtonOperateLocked = true
                                }

                            }
                        }
                    }
                }

            }
            true
        }

        cameraButtonHardware.setOnTouchListener { _, event ->

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    cameraButtonHardware.background =
                        ContextCompat.getDrawable(this, R.drawable.styles_mouse_buttons_down)
                }

                MotionEvent.ACTION_UP -> {

                    cameraButtonHardware.background =
                        ContextCompat.getDrawable(this, R.drawable.styles_mouse_buttons_up)
                    reasoningButton = !reasoningButton
                    when (reasoningButton) {
                        true -> {

                            activityCameraBinding.cameraButtonHardware.text = resources.getText(R.string.GPUReasoning)
                            activityCameraBinding.cameraButtonHardware.setCompoundDrawablesWithIntrinsicBounds(
                                null,
                                resources.getDrawable(R.drawable.image_camera_gpu), null, null
                            )

                            handHardware = true
                            faceHardware = true
                        }
                        false -> {

                            activityCameraBinding.cameraButtonHardware.text = resources.getText(R.string.CPUReasoning)
                            activityCameraBinding.cameraButtonHardware.setCompoundDrawablesWithIntrinsicBounds(
                                null,
                                resources.getDrawable(R.drawable.image_camera_cpu), null, null
                            )

                            handHardware = false
                            faceHardware = false
                        }
                    }
                }

            }
            true
        }

        cameraButtonFlip.setOnTouchListener { _, event ->
            when(event.actionMasked){
                MotionEvent.ACTION_DOWN -> {
                    cameraButtonFlip.background =
                        ContextCompat.getDrawable(this,R.drawable.styles_mouse_buttons_down)

                }

                MotionEvent.ACTION_UP -> {
                    if(flipButtonOperateLocked){
                        flipButtonOperateLocked = false
                        flipButton = !flipButton

                        cameraControl = null
                        when (flipButton) {
                            true -> {
                                /**如果设备具有可用的后置摄像头，则返回 true。否则为 False*/
                                if(cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)){
                                    /**视图正确时*/
                                    activityCameraBinding.viewFinder.post {
                                        lensFacing =  CameraSelector.LENS_FACING_BACK
                                        cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                                        //解除所有绑定，防止CameraProvider重复绑定到Lifecycle发生异常
                                        cameraProvider.unbindAll()
                                        preview = Preview.Builder().build()
                                        camera = cameraProvider.bindToLifecycle(
                                            this as LifecycleOwner, cameraSelector, preview, imageAnalyzer)
                                        cameraControl = camera?.cameraControl
                                        // 使用摄像机对象将我们的预览用例与视图连接起来
                                        preview?.setSurfaceProvider(activityCameraBinding.viewFinder.surfaceProvider)
                                        flipButtonOperateLocked = true
                                        cameraButtonFlip.background =
                                            ContextCompat.getDrawable(this,R.drawable.styles_mouse_buttons_up)
                                        parameter.setParameter("cameraLensFacing", lensFacing)
                                    }
                                }


                            }
                            false -> {
                                /**如果设备具有可用的前置摄像头，则返回 true。否则为 False*/
                                if(cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)){
                                    activityCameraBinding.viewFinder.post {
                                        lensFacing =  CameraSelector.LENS_FACING_FRONT
                                        cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                                        //解除所有绑定，防止CameraProvider重复绑定到Lifecycle发生异常
                                        cameraProvider.unbindAll()
                                        preview = Preview.Builder().build()
                                        camera = cameraProvider.bindToLifecycle(
                                            this as LifecycleOwner, cameraSelector, preview, imageAnalyzer)
                                        cameraControl = camera?.cameraControl
                                        // 使用摄像机对象将我们的预览用例与视图连接起来
                                        preview?.setSurfaceProvider(activityCameraBinding.viewFinder.surfaceProvider)
                                        flipButtonOperateLocked = true
                                        cameraButtonFlip.background =
                                            ContextCompat.getDrawable(this,R.drawable.styles_mouse_buttons_up)
                                        parameter.setParameter("cameraLensFacing", lensFacing)
                                    }
                                }

                            }
                        }
                    }
                }

            }
            true
        }

    }


    /**暂停**/
    override fun onPause(){
        handRecognizer?.close()
        handRecognizer = null
        faceRecognizer?.close()
        faceRecognizer = null
        super.onPause()


    }

    /**消毁*/
    override fun onDestroy() {

        orientationEventListener.disable()
        unbindService(connection)// 解绑服务
        stopService(bootingServer) // 停止服务

        reasoningTask.apply {
            handRecognizer = null
            faceRecognizer = null
            shutdown()
            awaitTermination(1000, TimeUnit.MILLISECONDS)
        }



        super.onDestroy()
    }


    /**从后台恢复*/
    override fun onResume() {
        /**相机权限检查*/
        if (!hasPermissions(this)) {
            ActivityCompat.requestPermissions(
                this, permissions.toTypedArray(), permissionsRequestCode)
        } else {
            tools.setHideVirtualKeys(window)
            bindCamera()  //启用相机

            activityCameraBinding.reasoningOverlay.clearCanvas()
            activityCameraBinding.reasoningOverlay.invalidate() // 强制重绘

            /**启用推理*/
            reasoningTask.execute {
                if(handButton)   handReasoningInit(this)
                if(faceButton)   faceReasoningInit(this)
            }

            /**启动服务*/
            bootingServer = Intent(this, BackgroundServices::class.java)
            startService(bootingServer) //启动服务
            bindService(bootingServer!!, connection, Service.BIND_AUTO_CREATE)//绑定服务


        }
        super.onResume()
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

    /** 用于检查是否授予此应用程序所需的所有权限的便捷方法 */
    private fun hasPermissions(context: Context) = permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionsRequestCode && hasPermissions(this)) {
//            bindCamera()

        } else {
            Log.d("权限","如果我们没有所需的权限，我们就无法运行")
            finish()
        }
    }

    /**屏幕方向监听*/
    private val orientationEventListener by lazy {
        object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) {
                    return
                }
                rotateOrientation = when (orientation) {
                    in 45 until 135 -> 180
                    in 135 until 225 -> 90
                    in 225 until 315 -> 0
                    else ->270
                }
                screenOrientation = when (orientation) {
                    in 45 until 135 -> 90
                    in 135 until 225 -> 180
                    in 225 until 315 -> 270
                    else -> 0
                }
            }
        }
    }


    /** 声明和绑定预览和分析用例*/
    @SuppressLint("UnsafeExperimentalUsageError")
    private fun bindCamera() = activityCameraBinding.viewFinder.post {
        Log.d("相机","启用相机")
        //初始化 CameraX，并准备绑定相机用例
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener ({
            // 现在可保证提供摄像机
            cameraProvider = cameraProviderFuture.get()

            // 设置取景器用例以显示相机预览
            preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(activityCameraBinding.viewFinder.display.rotation)
                .build()

            // 设置图像分析用例，实时处理图像帧
            imageAnalyzer = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(activityCameraBinding.viewFinder.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                // 然后，可以将分析器分配给实例
                .also {
                    it.setAnalyzer(reasoningTask) { image ->
                        reasoningRecognize(image)
                    }
                }

            if( lensFacing == CameraSelector.LENS_FACING_FRONT  || lensFacing == CameraSelector.LENS_FACING_BACK ){
                // 每次都创建一个新的相机选择器，执行镜头朝向
                cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                // 使用相同的生命周期所有者将已声明的配置应用于 CameraX
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this as LifecycleOwner, cameraSelector, preview, imageAnalyzer)
                cameraControl = camera?.cameraControl

                // 使用摄像机对象将我们的预览用例与视图连接起来
                preview?.setSurfaceProvider(activityCameraBinding.viewFinder.surfaceProvider)
            }


        }, ContextCompat.getMainExecutor(this))

    }
    /**相机数据送入推理识别*/
    private fun reasoningRecognize(imageProxy: ImageProxy) {
        val frameTime = SystemClock.uptimeMillis() //时间戳
        // 将 RGB 位从帧复制到位图缓冲区
        val bitmapBuffer = Bitmap.createBitmap(
            imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888
        )
        imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
        imageProxy.close()

        val matrix = Matrix().apply {
            postRotate(rotateOrientation.toFloat())//旋转图片 能判断手势任意位置
            if (lensFacing == CameraSelector.LENS_FACING_FRONT){
                postScale(-1f, 1f, imageProxy.width.toFloat(), imageProxy.height.toFloat())// 镜面效果   左 右 手互换
            }

        }

        //旋转位图以匹配我们的模型所期望的
        val rotatedBitmap = Bitmap.createBitmap(
            bitmapBuffer,
            0,
            0,
            bitmapBuffer.width,
            bitmapBuffer.height,
            matrix,
            true
        )

        //将输入 Bitmap 对象转换为 MPImage 对象以运行推理
        val mpImage = BitmapImageBuilder(rotatedBitmap).build()


        reasoningTask.execute{
          if(faceButton)  faceRecognizer?.detectAsync(mpImage, frameTime)
          if(handButton)  handRecognizer?.recognizeAsync(mpImage, frameTime)
        }






    }

    /** 手部推理初始化*/
    @SuppressLint("SuspiciousIndentation")
    private fun handReasoningInit(context :Context){
        cameraButtonHand.background =
            ContextCompat.getDrawable(this,R.drawable.styles_mouse_buttons_stop)

        // 设置常规识别选项，包括使用的线程数 handHardware
        val hardware   =  if(handHardware) Delegate.GPU else Delegate.CPU
        parameter.setParameter("cameraHandHardware", handHardware)
        val baseOptionBuilder = BaseOptions.builder()
        baseOptionBuilder.setDelegate(hardware)   //选择硬件
        baseOptionBuilder.setModelAssetPath("gesture.task") //选择模型

        val baseOptions = baseOptionBuilder.build()
        val optionsBuilder = GestureRecognizer.GestureRecognizerOptions.builder()
        optionsBuilder.setBaseOptions(baseOptions)//设置基本选项
        /**
         * 手掌检测模型最低置信度分数
         *  数值 0.0 - 1.0  默认值 0.5
         * */
        optionsBuilder.setMinHandDetectionConfidence(0.5f)
        /**
         * 手部跟踪的最低置信度分数
         * 在视频模式和实时流模式下，如果跟踪失败，手掌检测模型
         * 数值 0.0 - 1.0   默认值 0.5
         */
        optionsBuilder.setMinTrackingConfidence(0.5f) //
        /**
         *手势检测的最低置信度分数
         * 在手势识别器的视频模式和实时流模式下，分数低于这个阈值，它会触发手掌检测模型。
         * 否则算法用于确定位置用于后续地标检测的手。
         * 数值 0.0 - 1.0  默认值 0.5
         * */
        optionsBuilder.setMinHandPresenceConfidence(0.5f)
        /**
         * IMAGE：单图像输入的模式。
         * VIDEO：视频解码帧的模式。
         * LIVE_STREAM：输入直播模式 数据，例如来自相机的数据。在此模式下，resultListener 必须是 调用以设置侦听器以接收结果 异步。
         * 默认值 IMAGE
         * */
        optionsBuilder.setRunningMode(RunningMode.LIVE_STREAM)
        /** 检测最大手数   默认值 1   **/
        optionsBuilder.setNumHands(2)//设置检测到最大手数
        optionsBuilder.setResultListener(this::returnHandResult)//setResultListener （设置结果侦听器）
        optionsBuilder.setErrorListener(this::returnError)//setErrorListener （设置错误监听器）
        val options = optionsBuilder.build()

        handRecognizer = GestureRecognizer.createFromOptions(context, options)

        cameraButtonHand.background =
            ContextCompat.getDrawable(this,R.drawable.styles_mouse_buttons_down)
    }


    /**返回在此 的调用者识别过程中抛出的错误*/
    private fun returnError(error: RuntimeException) {
        Log.d("识别错误",error.message ?: "未知错误")
    }

    /**脸部推理初始化  **/
    @SuppressLint("SuspiciousIndentation")
    private fun faceReasoningInit(context :Context){
        cameraButtonFace.background =
            ContextCompat.getDrawable(this,R.drawable.styles_mouse_buttons_stop)

        val hardware   =  if(faceHardware) Delegate.GPU else Delegate.CPU
        parameter.setParameter("cameraFaceHardware", faceHardware)
        val faceOptionBuilder = BaseOptions.builder()
        faceOptionBuilder.setDelegate(hardware)
        faceOptionBuilder.setModelAssetPath("face.task")
        val freebaseOptions = faceOptionBuilder.build()
        val faceOptionsBuilder = FaceLandmarker.FaceLandmarkerOptions.builder()
        faceOptionsBuilder.setBaseOptions(freebaseOptions)
        /** 人脸检测的最低置信度分数*/
        faceOptionsBuilder.setMinFaceDetectionConfidence(0.5f)
        /** 人脸跟踪的最低置信度分数*/
        faceOptionsBuilder.setMinTrackingConfidence(0.5f)
        /**人脸存在的最低置信度分数 在人脸地标检测中得分**/
        faceOptionsBuilder.setMinFacePresenceConfidence(0.5f)
        faceOptionsBuilder.setNumFaces(1)
        faceOptionsBuilder.setOutputFaceBlendshapes(true)//人脸地标是否输出人脸混合形状
        faceOptionsBuilder.setRunningMode(RunningMode.LIVE_STREAM)

        faceOptionsBuilder.setResultListener(this::returnFaceResult)
        faceOptionsBuilder.setErrorListener(this::returnError)

        faceRecognizer = FaceLandmarker.createFromOptions(context, faceOptionsBuilder.build())

        cameraButtonFace.background =
            ContextCompat.getDrawable(this,R.drawable.styles_mouse_buttons_down)
    }

    /**返回手部推理结果*/
    private fun returnHandResult(handResults: GestureRecognizerResult, image: MPImage) {
        /***数据绘图**/
        val timeStamp = SystemClock.uptimeMillis() - handResults.timestampMs() //时间戳
        activityCameraBinding.reasoningOverlay.handResults(
            handResults,
            timeStamp,
            image,
            handHardware,
            screenOrientation
        )
        activityCameraBinding.reasoningOverlay.invalidate() // 强制重绘

        AnalyseIdentifyOutcome.handDataProcessing(handResults,image,timeStamp)
        mService?.reporterData(AnalyseIdentifyOutcome.getMouseData(sensitivity,mousePointerVelocity,1))


    }


    private fun returnFaceResult(faceResults: FaceLandmarkerResult, image: MPImage) {

        /**先判断人脸大小是否合适*/
        when(AnalyseIdentifyOutcome.getFaceSize(faceResults,image)){
            0 -> {
                /***数据绘图**/
                val timeStamp = SystemClock.uptimeMillis() - faceResults.timestampMs()
                activityCameraBinding.reasoningOverlay.faceResults(
                    faceResults,
                    timeStamp,
                    image,
                    faceHardware,
                    screenOrientation
                )
                activityCameraBinding.reasoningOverlay.invalidate() // 强制重绘
                AnalyseIdentifyOutcome.faceDataProcessing(image)
                mService?.reporterData(AnalyseIdentifyOutcome.getMouseData(sensitivity,mousePointerVelocity,1))
            }

            1->{
                activityCameraBinding.reasoningOverlay.clearCanvas()
                activityCameraBinding.reasoningOverlay.invalidate() // 强制重绘
                cameraCurrentZoom += 0.05f
                if(cameraCurrentZoom  > 1) cameraCurrentZoom = 1f
                cameraControl?.setLinearZoom(cameraCurrentZoom)
            }
            -1 ->{
                activityCameraBinding.reasoningOverlay.clearCanvas()
                activityCameraBinding.reasoningOverlay.invalidate() // 强制重绘
                cameraCurrentZoom -= 0.05f
                if(cameraCurrentZoom  < 0) cameraCurrentZoom = 0f
                cameraControl?.setLinearZoom(cameraCurrentZoom)
            }
        }


    }



}




