package pl.edu.pwr.lab3.i242487.view.fragment

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

import pl.edu.pwr.lab3.i242487.databinding.FragmentObjectsBinding
import pl.edu.pwr.lab3.i242487.view.GraphicOverlay

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ObjectsFragment : CameraPreviewFragment() {

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val mBinding get() = _binding as FragmentObjectsBinding?

    private var needUpdateGraphicOverlayImageSourceInfo = true

    private var lensFacing = CameraSelector.LENS_FACING_BACK

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentObjectsBinding.inflate(inflater, container, false)

        startCamera()

        return mBinding!!.root
    }

    override fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(mBinding!!.pvCameraPreview.surfaceProvider)
                }


            imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, ObjectImageAnalyzer(mBinding!!.goGraphicOverlay))
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try{
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            }catch (ex: Exception){
                Log.e(this.javaClass.name, "Camera binding failed", ex)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private inner class ObjectImageAnalyzer(val overlay: GraphicOverlay) : ImageAnalysis.Analyzer {

        @SuppressLint("UnsafeOptInUsageError")
        override fun analyze(imageProxy: ImageProxy) {

            if(needUpdateGraphicOverlayImageSourceInfo){
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees

                val isImageFlipped = lensFacing == CameraSelector.LENS_FACING_FRONT

                if(rotationDegrees == 0 || rotationDegrees == 180)
                    overlay.setImageSourceInfo(
                        imageProxy.width, imageProxy.height, isImageFlipped
                    )

                else
                    overlay.setImageSourceInfo(
                        imageProxy.height, imageProxy.width, isImageFlipped
                    )

                needUpdateGraphicOverlayImageSourceInfo = false
            }

            val mediaImage = imageProxy.image
            mediaImage?.let { _ ->
                val image =  InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                val options = ObjectDetectorOptions.Builder()
                    .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                    .enableClassification()
                    .build()

                val objectDetector = ObjectDetection.getClient(options)

                objectDetector.process(image)
                    .addOnSuccessListener {
                        results ->
                        Log.i("OBJECT DETECTOR ", "Detected objects: ${results.size}")
                        overlay.clear()
                        results.forEach{
                            result -> overlay.add(ObjectGraphic(overlay, result))
                                Log.i("ObjectDetector - labels", result.labels.joinToString { it.text })
                        }
                    }
                    .addOnFailureListener{
                            ex -> Log.wtf("LAB", ex)
                    }
                    .addOnCompleteListener{
                        imageProxy.close()
                    }
            }
        }

    }

    private class ObjectGraphic(
        overlay: GraphicOverlay,
        private val detectedObject: DetectedObject
    ) : GraphicOverlay.Graphic(overlay){

        companion object {
            private const val STROKE_WIDTH = 4.0f
            private const val NUM_COLORS = 8

            private val COLORS =
                arrayOf(
                    intArrayOf(Color.BLACK, Color.WHITE),
                    intArrayOf(Color.WHITE, Color.MAGENTA),
                    intArrayOf(Color.BLACK, Color.LTGRAY),
                    intArrayOf(Color.WHITE, Color.RED),
                    intArrayOf(Color.WHITE, Color.BLUE),
                    intArrayOf(Color.WHITE, Color.DKGRAY),
                    intArrayOf(Color.BLACK, Color.CYAN),
                    intArrayOf(Color.BLACK, Color.YELLOW),
                )
        }

        private val numColors = COLORS.size

        private val boxPaints = Array(numColors){ Paint() }

        init {
            for(i in 0 until numColors){
                boxPaints[i] = Paint().apply {
                    color = COLORS[i][1]
                    style = Paint.Style.STROKE
                    strokeWidth = STROKE_WIDTH
                }
            }
        }


        override fun draw(canvas: Canvas) {
            val colorID = if (detectedObject.trackingId == null) 0
                else abs(detectedObject.trackingId!! % NUM_COLORS)

            val rect = RectF(detectedObject.boundingBox)
            val x0 = translateX(rect.left)
            val x1 = translateX(rect.right)
            rect.left = min(x0, x1)
            rect.right = max(x0, x1)
            rect.top = translateY(rect.top)
            rect.bottom = translateY(rect.bottom)

            canvas.drawRect(rect, boxPaints[colorID])
        }

    }

}