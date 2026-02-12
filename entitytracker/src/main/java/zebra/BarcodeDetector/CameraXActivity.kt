    package zebra.BarcodeDetector


    import android.content.SharedPreferences
    import android.hardware.camera2.CameraMetadata
    import android.os.Build
    import android.os.Bundle
    import android.os.Handler
    import android.provider.Settings
    import android.util.Log
    import android.util.Size
    import android.view.View
    import android.widget.ToggleButton
    import androidx.annotation.OptIn
    import androidx.appcompat.app.AppCompatActivity
    import androidx.camera.camera2.interop.ExperimentalCamera2Interop
    import androidx.camera.core.CameraSelector
    import androidx.camera.core.ImageAnalysis
    import androidx.camera.core.ImageCapture
    import androidx.camera.core.resolutionselector.ResolutionSelector
    import androidx.camera.core.resolutionselector.ResolutionSelector.PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE
    import androidx.camera.core.resolutionselector.ResolutionStrategy
    import androidx.camera.view.LifecycleCameraController

    import kotlinx.coroutines.asCoroutineDispatcher
    import kotlinx.coroutines.runBlocking
    import zebra.BarcodeDetector.OverlayView.Companion.BARCODETOHIGHLIGHT
    import zebra.BarcodeDetector.OverlayView.Companion.CAMERA_RESOLUTION_HEIGHT
    import zebra.BarcodeDetector.OverlayView.Companion.CAMERA_RESOLUTION_WIDTH
    import zebra.BarcodeDetector.OverlayView.Companion.CHOSEN_SCENE
    import zebra.BarcodeDetector.OverlayView.Companion.ZOOM_RATIO
    import zebra.BarcodeDetector.databinding.ActivityCameraXactivityBinding
    import java.util.concurrent.ExecutorService
    import java.util.concurrent.Executors


    class CameraXActivity : AppCompatActivity() {
        private lateinit var viewBinding: ActivityCameraXactivityBinding

        //-103-define a lateinit var entityTrackerAnalyzer: EntityTrackerAnalyzer

        private lateinit var workerExecutor: ExecutorService

        val executor = Executors.newFixedThreadPool(3);
        private lateinit var sharedPreferences: SharedPreferences


        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            title = "zebra/ai/entityTracker exerciser"
            supportActionBar?.hide()

            Log.i(TAG, "isZebra device = $isZEBRA")
            Log.i(TAG, "Device details:\n${getDeviceDetails()}")

            viewBinding = ActivityCameraXactivityBinding.inflate(layoutInflater)
            setContentView(viewBinding.root)

            val toggle: ToggleButton = viewBinding.toggleAnalyzerMode
            toggle.setOnClickListener { view: View -> onClickToggleAnalyzerMode(view) }

            runBlocking(Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {
                sharedPreferences = getSharedPreferences("SettingsPreferences", MODE_PRIVATE)
                loadSettings()
            }


            if(!Build.MANUFACTURER.contains("ZEBRA", true)){
                isZEBRA = false
                viewBinding.toggleAnalyzerMode.visibility = View.GONE
                Log.w(TAG, "Non-Zebra device detected, forcing isZEBRA = false")
            }
            else {
                isZEBRA = true
                viewBinding.toggleAnalyzerMode.isChecked = true
                Log.i(TAG, "Zebra device detected in onCreate()")
            }

            workerExecutor = Executors.newSingleThreadExecutor()  //was in oncreate originally

        }


        override fun onPause() {
            super.onPause()
        }

        override fun onStop() {
            super.onStop()
            cameraController.clearImageAnalysisAnalyzer()
            //-106-uncomment when defining the barcode decoder to properly dispose of it when the app is not in foreground
//            barcodeDecoder?.dispose()
//            barcodeDecoder = null

        }

        override fun onDestroy() {
            super.onDestroy()

            cameraController.clearImageAnalysisAnalyzer()

            //-107-uncomment when defining the barcode decoder
//            barcodeDecoder?.dispose()
//            barcodeDecoder = null

            workerExecutor.shutdown()
        }


        private fun onClickToggleAnalyzerMode(view: View){

        }

        override fun onResume() {
            super.onResume()

            val timebegin = System.currentTimeMillis()

            setupCameraAndAnalyzers()
        if(isZEBRA) {
            initZETA()

            //-102-setting entityTrackerAnalyzer as current analyzer


            //-109-remove this sample analyzer when adding the -102-eta analyzer
            cameraController.setImageAnalysisAnalyzer(
                workerExecutor,
                ImageAnalysis.Analyzer { imageProxy ->
                    val currentTime = System.currentTimeMillis()
                    val bitmap = imageProxy.toBitmap()
                    val rotation = imageProxy.imageInfo.rotationDegrees.toFloat()
                    val matrix = android.graphics.Matrix().apply { postRotate(rotation) }
                    val debugBMP = android.graphics.Bitmap.createBitmap(
                        bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                    )
                    Log.d(TAG, "#WORKSHOP Frame received at $currentTime with resolution ${imageProxy.width}x${imageProxy.height}")
                    imageProxy.close()
                }
            )
            Log.i(TAG, "A sample analyzer has been set to show how frames are provisioned to the app.")
            //end of -109-

        }


        periodJobOnCanvas(VIEW_RESET_PERIOD_MS)

        viewBinding.tvOCRout.visibility = View.VISIBLE
        viewBinding.overlayView.visibility = View.VISIBLE
    }


    private fun loadSettings() {
        val imageSize = sharedPreferences.getInt("IMAGESIZE", 1)
        when(imageSize){
            0 -> {
                CAMERA_RESOLUTION_WIDTH = 480
                CAMERA_RESOLUTION_HEIGHT = 640
            }
            1 -> {
                CAMERA_RESOLUTION_WIDTH = 1080
                CAMERA_RESOLUTION_HEIGHT = 1920
            }
            2 -> {
                CAMERA_RESOLUTION_WIDTH = 1536
                CAMERA_RESOLUTION_HEIGHT = 2048
            }
            3 -> {
                CAMERA_RESOLUTION_WIDTH = 1920
                CAMERA_RESOLUTION_HEIGHT = 2560
            }
            4 -> {
                CAMERA_RESOLUTION_WIDTH = -1
                CAMERA_RESOLUTION_HEIGHT = -1
            }
        }

        val scene = sharedPreferences.getInt("SCENE", 0)
        when(scene) {
            0 -> {
                CHOSEN_SCENE = CameraMetadata.CONTROL_SCENE_MODE_STEADYPHOTO
            }
            1 -> {
                CHOSEN_SCENE = CameraMetadata.CONTROL_SCENE_MODE_ACTION
            }
            2 -> {
                CHOSEN_SCENE = CameraMetadata.CONTROL_SCENE_MODE_SPORTS
            }
            3 -> {
                CHOSEN_SCENE = CameraMetadata.CONTROL_SCENE_MODE_BARCODE
            }
        }



        val zoom = sharedPreferences.getInt("ZOOM", 0)
        when(zoom)  { //not yet implemented
            0 -> {
                ZOOM_RATIO = 1.0
            }
            1 -> {
                ZOOM_RATIO = 1.5
            }
            2 -> {
                ZOOM_RATIO = 2.0
            }
            3 -> {
                ZOOM_RATIO = 3.0
            }
            4 -> {
                ZOOM_RATIO = 5.0
            }
        }

        BARCODETOHIGHLIGHT = sharedPreferences.getString("BARCODESTOHIGHLIGHT", "").toString()

        isZEBRA = sharedPreferences.getBoolean("IS_ZEBRA_MODE", false)


    }

    var readCounter=0
    private fun periodJobOnCanvas(timeInterval: Long) {
        val handler = Handler()
        val runnable = object : Runnable {
            override fun run() {
                viewBinding.overlayView.readRate=readCounter
                readCounter=0

                viewBinding.overlayView.invalidate()

                viewBinding.overlayView.clq.clear()



                handler.postDelayed(this, timeInterval)

            }
        }
        handler.postDelayed(runnable, timeInterval)
    }



    private fun initZETA() {
        //-105-here init the AISuite SDK

        //-104-here prepare the barcode settings



//            try {

                //-100-here instantiate a barcode object.



                //-101-here use entityTrackerAnalyzer: pass the barcode detector and take care of thed ecoding  results





//            } catch (e: IOException) {
//                Log.e(TAG, "Fatal error: load failed - " + e.message)
//                this@CameraXActivity.finish()
//            }

    }


        //-108-bc variable definition    private var barcodeDecoder: BarcodeDecoder? = null

    private val fpsQueue = ArrayDeque<Long>(1)


    private fun setOutputtextInMainThread( txt: String){
        runOnUiThread {
            viewBinding.tvOCRout.text = txt
        }
    }
    private fun appendOutputtextInMainThread( txt: String){
        runOnUiThread {
            viewBinding.tvOCRout.text = txt + "" + viewBinding.tvOCRout.text
        }
    }




    //load png
    private fun loadBitmapFromAsset(): ByteArray {
        val inputStream = assets.open("technologies.png")
        val buffer = ByteArray(inputStream.available())
        inputStream.read(buffer)
        inputStream.close()
        return buffer
    }

    private lateinit var cameraController: LifecycleCameraController

    @OptIn(ExperimentalCamera2Interop::class)
    private fun setupCameraAndAnalyzers() {
        // 0. Init vals from settings
        val resolutionSelectorHighest = ResolutionSelector.Builder()
            .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
            .setAllowedResolutionMode(PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE)
            .build()

        val resolutionSelectorAsSettings = ResolutionSelector.Builder()
            .setResolutionFilter { supportedSizes, _ ->
                // "Something else": Manually filter the supported sizes list.
                // We check for width/height matches in both orientations (Portrait/Landscape)
                val targetW = CAMERA_RESOLUTION_WIDTH
                val targetH = CAMERA_RESOLUTION_HEIGHT

                val exactMatch = supportedSizes.firstOrNull {
                    (it.width == targetW && it.height == targetH) ||
                            (it.width == targetH && it.height == targetW)
                }

                // If the hardware supports your exact pixels, return ONLY that size.
                // This forces CameraX to use it or crash, ensuring it doesn't silent-fallback to 1600x1200.
                if (exactMatch != null) {
                    listOf(exactMatch)
                } else {
                    supportedSizes // If strictly not supported, let normal strategy handle it
                }
            }
            .setResolutionStrategy(ResolutionStrategy(
                Size(CAMERA_RESOLUTION_WIDTH, CAMERA_RESOLUTION_HEIGHT),
                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
            ))
            .build()

        // 1. Initialize the LifecycleCameraController and bind it to the lifecycle
        cameraController = LifecycleCameraController(this)
        cameraController.bindToLifecycle(this)
        cameraController.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        cameraController.imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
        cameraController.imageCaptureMode = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
        cameraController.imageAnalysisResolutionSelector = if(CAMERA_RESOLUTION_WIDTH>-1)   resolutionSelectorAsSettings else resolutionSelectorHighest
        cameraController.setZoomRatio(ZOOM_RATIO.toFloat())

        // 2. Link the controller to the PreviewView
        viewBinding.viewFinder.controller = cameraController

    }


    private fun requestPermissions() {}


    private fun getDeviceDetails() :String {
        val _android_id = "A_ID=" + Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)
        val deviceDetails = "${Build.MANUFACTURER}\n" +
                "${Build.MODEL}\n" +
                "${Build.DISPLAY}\n" +
                "${BuildConfig.APPLICATION_ID}-" +
                "${BuildConfig.VERSION_NAME}," +
                "${_android_id}\n"
        return deviceDetails
    }

    companion object {
        private const val TAG = "ZETA-WORSKOP"
        public var isZEBRA  =  false
        public const val VIEW_RESET_PERIOD_MS = 100L
    }

}