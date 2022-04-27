package pl.edu.pwr.lab3.i242487

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import pl.edu.pwr.lab3.i242487.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    companion object {
        const val PICK_PHOTO_REQUEST_CODE = 101
    }

    private lateinit var mBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mBinding.bPickImage.setOnClickListener {
            pickImageFromGallery()
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if(resultCode == Activity.RESULT_OK){
            when(requestCode){
                PICK_PHOTO_REQUEST_CODE -> {
                    val bitmap = getImageFromData(data)
                    bitmap?.apply{
                        mBinding.ivImage.setImageBitmap(this)
                        processImageTagging(bitmap)
                    }
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun getImageFromData(data: Intent?): Bitmap? {
        val selectedImage = data?.data
        return MediaStore.Images.Media.getBitmap(contentResolver, selectedImage)
    }

    private fun processImageTagging(bitmap: Bitmap){
        val visionImg = FirebaseVisionImage.fromBitmap(bitmap)
        val labeler = FirebaseVision.getInstance()
            .getOnDeviceImageLabeler().processImage(visionImg)
            .addOnSuccessListener {
                tags ->
                mBinding.tvImageLabel.text = tags.joinToString(" "){ it.text }
            }
            .addOnFailureListener {
                ex -> Log.wtf("LAB", ex)
            }
    }

    private fun pickImageFromGallery(){
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }
        startActivityForResult(intent, PICK_PHOTO_REQUEST_CODE)
    }
}