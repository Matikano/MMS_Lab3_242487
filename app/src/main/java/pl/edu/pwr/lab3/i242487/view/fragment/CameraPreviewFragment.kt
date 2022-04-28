package pl.edu.pwr.lab3.i242487.view.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.viewbinding.ViewBinding
import pl.edu.pwr.lab3.i242487.R
import pl.edu.pwr.lab3.i242487.databinding.FragmentCameraBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.reflect.KProperty1

abstract class CameraPreviewFragment : Fragment() {

    protected lateinit var cameraExecutor: ExecutorService
    protected lateinit var imageAnalyzer: ImageAnalysis

    protected var _binding: ViewBinding? = null


    private fun setupCameraExecutor() {
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupCameraExecutor()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    abstract fun startCamera()

}