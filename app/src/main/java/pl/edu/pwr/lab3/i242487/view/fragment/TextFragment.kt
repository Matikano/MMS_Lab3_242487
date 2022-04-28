package pl.edu.pwr.lab3.i242487.view.fragment

import android.annotation.SuppressLint
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
import androidx.fragment.app.Fragment
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import pl.edu.pwr.lab3.i242487.R
import pl.edu.pwr.lab3.i242487.databinding.FragmentTextBinding

class TextFragment : CameraPreviewFragment() {


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val mBinding get() = _binding as FragmentTextBinding?

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentTextBinding.inflate(inflater, container, false)

        mBinding!!.tvLabels.text = resources.getString(R.string.tv_labels, "")

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

            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, TextLabelingImageAnalyzer())
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


    private inner class TextLabelingImageAnalyzer : ImageAnalysis.Analyzer {

        @SuppressLint("UnsafeOptInUsageError")
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            mediaImage?.let { _ ->
                val image =  InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                recognizer.process(image)
                    .addOnSuccessListener { text ->
                        mBinding?.apply { tvLabels.text = resources.getString(
                            R.string.tv_labels,
                            text.textBlocks.joinToString(""){it.text}
                        ) }
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