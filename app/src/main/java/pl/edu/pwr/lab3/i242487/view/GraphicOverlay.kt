package pl.edu.pwr.lab3.i242487.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.util.AttributeSet
import android.view.View
import com.google.firebase.components.Preconditions

public class GraphicOverlay(context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {

    init {
        addOnLayoutChangeListener {
                _, _, _, _, _, _, _, _, _ -> needUpdateTransformation = true
        }
    }

    private val lock = Any()

    private var graphics = mutableListOf<Graphic>()
    private val transformationMatrix = Matrix()

    private var imageWidth: Int = 0
    private var imageHeight: Int = 0

    private var scaleFactor = 1.0f

    private var postScaleWidthOffset: Float = 0f
    private var postScaleHeightOffset: Float = 0f

    private var isImageFlipped: Boolean?= null
    private var needUpdateTransformation = true

    public abstract class Graphic(private var overlay: GraphicOverlay){

        val context: Context = overlay.context.applicationContext

        val isImageFlipped = overlay.isImageFlipped

        val transformationMatrix = overlay.transformationMatrix

        public abstract fun draw(canvas: Canvas)

        public fun scale(imagePixel: Float): Float = imagePixel * overlay.scaleFactor

        public fun translateX(x: Float): Float =
            when(isImageFlipped){
                true -> overlay.width - (scale(x) - overlay.postScaleWidthOffset)
                false -> scale(x) - overlay.postScaleWidthOffset
                null -> 0f
            }


        public fun translateY(x: Float): Float = scale(x) - overlay.postScaleHeightOffset

        public fun postInvalidate() = overlay.postInvalidate()
    }

    public fun clear() {
        synchronized(lock){
            graphics.clear()
        }
        postInvalidate()
    }

    public fun add(graphic: Graphic){
        synchronized(lock){
            graphics.add(graphic)
        }
    }

    public fun remove(graphic: Graphic){
        synchronized(lock){
            graphics.remove(graphic)
        }
        postInvalidate()
    }

    public fun setImageSourceInfo(imageWidth: Int, imageHeight: Int, isFlipped: Boolean){
        Preconditions.checkState(imageWidth > 0, "image width must be positive")
        Preconditions.checkState(imageHeight > 0, "image height must be positive")

        synchronized(lock){
            this.imageWidth = imageWidth
            this.imageHeight = imageHeight
            isImageFlipped = isFlipped
            needUpdateTransformation = true
        }
        postInvalidate()
    }

    private fun updateTransformationIfNeeded(){
        if(!needUpdateTransformation || imageWidth <= 0 || imageHeight <= 0)
            return

        val viewAspectRatio = (width / height).toFloat()
        val imageAspectRatio = (imageWidth / imageHeight).toFloat()

        postScaleHeightOffset = 0f
        postScaleWidthOffset = 0f

        if(viewAspectRatio > imageAspectRatio){
            scaleFactor = (width / imageWidth).toFloat()
            postScaleHeightOffset =  (width / imageAspectRatio - height) / 2
        } else {
            scaleFactor = (height / imageHeight).toFloat()
            postScaleWidthOffset = (height * imageAspectRatio - width) / 2
        }

        transformationMatrix.apply {
            reset()
            setScale(scaleFactor, scaleFactor)
            postTranslate(-postScaleWidthOffset, -postScaleHeightOffset)
        }

        if(isImageFlipped!!)
            transformationMatrix.postScale(-1f, 1f, width / 2f, height / 2f)

        needUpdateTransformation = false
    }

    protected override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.let{ canvas ->
            synchronized(lock){
                updateTransformationIfNeeded()
                graphics.forEach { graphic ->  graphic.draw(canvas) }
            }
        }
    }

}