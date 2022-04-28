package pl.edu.pwr.lab3.i242487.view.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import pl.edu.pwr.lab3.i242487.R
import pl.edu.pwr.lab3.i242487.databinding.FragmentCameraBinding

class CameraFragment : CameraPreviewFragment() {


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val mBinding get() = _binding as FragmentCameraBinding?


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)

        mBinding!!.apply{
            tvLabels.text = resources.getString(R.string.tv_labels, "")
        }

        startCamera()
        return mBinding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        imageAnalyzer.clearAnalyzer()
        cameraExecutor.shutdown()
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
                    it.setAnalyzer(cameraExecutor, LabelingImageAnalyzer())
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

    private inner class LabelingImageAnalyzer : ImageAnalysis.Analyzer {
        @SuppressLint("UnsafeOptInUsageError")
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            mediaImage?.let { _ ->
                val image =  InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
                labeler.process(image)
                    .addOnSuccessListener { tags ->
                        mBinding?.apply {
                            tvLabels.text = resources.getString(
                                R.string.tv_labels,
                                tags.joinToString(" "){
                                    it.text
                                })
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
}