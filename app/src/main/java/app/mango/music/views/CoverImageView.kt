package app.mango.music.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import androidx.annotation.IntRange
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.animation.addListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt

/**
 * 根据
 * https://gitee.com/lalilu/lmusic/
 * 修改
 */
class CoverImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs), CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.IO

    @IntRange(from = 0, to = 2000)
    var samplingValue: Int = 200
        set(value) {
            field = value
            launch(Dispatchers.IO) {
                samplingBitmap = createSamplingBitmap(sourceBitmap, value)
            }
        }

    private var bitmapPainter: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var sourceBitmap: Bitmap? = null
    private var samplingBitmap: Bitmap? = null

    private var sourceRect = Rect()
    private var destRect = RectF()

    private var newBitmapPainter: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var newSourceBitmap: Bitmap? = null
    private var newSamplingBitmap: Bitmap? = null

    private var newSourceRect = Rect()
    private var newDestRect = RectF()

    override fun onDraw(canvas: Canvas) {
        destRect.set(0f, 0f, width.toFloat(), height.toFloat())
        newDestRect.set(0f, 0f, width.toFloat(), height.toFloat())

        sourceBitmap?.let {
            sourceRect.set(0, 0, it.width, it.height)
            canvas.drawBitmap(it, sourceRect, destRect, bitmapPainter)
        }
        newSourceBitmap?.let {
            newSourceRect.set(0, 0, it.width, it.height)
            canvas.drawBitmap(it, newSourceRect, newDestRect, newBitmapPainter)
        }
    }

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 300
        addListener(onStart = {
            newBitmapPainter.alpha = 0
            invalidate()
        }, onEnd = {
            newBitmapPainter.alpha = 0
            sourceBitmap = newSourceBitmap
            samplingBitmap = newSamplingBitmap
            newSourceBitmap = null
            newSamplingBitmap = null
            invalidate()
        })
        addUpdateListener {
            val value = it.animatedValue as Float
            newBitmapPainter.alpha = (value * 255).roundToInt()
            invalidate()
        }
    }

    private suspend fun crossFade() = withContext(Dispatchers.Main) {
        if (animator.isStarted || animator.isRunning) animator.end()
        animator.start()
    }

    /**
     * 重采样图片，降低图片大小，用于Blur
     *
     * @param source 源图
     * @param samplingValue 输出图片的最大边大小
     * @return 经过重采样的Bitmap
     */
    private suspend inline fun createSamplingBitmap(source: Bitmap?, samplingValue: Int): Bitmap? =
        withContext(Dispatchers.IO) {
            source ?: return@withContext source

            val width = source.width
            val height = source.height
            val matrix = Matrix()

            val scaleWidth = samplingValue.toFloat() / width
            val scaleHeight = samplingValue.toFloat() / height
            matrix.postScale(scaleWidth, scaleHeight)

            return@withContext Bitmap.createBitmap(
                source, 0, 0, width, height, matrix, false
            )
        }

    /**
     * 外部创建Coil的ImageRequest，传入onSucceed的Drawable
     */
    fun loadImageFromDrawable(drawable: Drawable) = launch(Dispatchers.IO) {
        newSourceBitmap = drawable.toBitmap().addShadow(
            Color.argb(55, 0, 0, 0),
            Color.TRANSPARENT,
            0.25f,
            GradientDrawable.Orientation.TOP_BOTTOM,
            GradientDrawable.Orientation.BOTTOM_TOP
        )
        newSamplingBitmap = createSamplingBitmap(newSourceBitmap, samplingValue)
        crossFade()
    }

    fun clearImage() = launch(Dispatchers.IO) {
        this@CoverImageView.sourceBitmap = null
        this@CoverImageView.samplingBitmap = null
        this@CoverImageView.newSourceBitmap = null
        this@CoverImageView.newSamplingBitmap = null
        refresh()
    }

    private suspend inline fun refresh() =
        withContext(Dispatchers.Main) {
            invalidate()
        }

    private fun Drawable.toBitmap(): Bitmap {
        val w = this.intrinsicWidth
        val h = this.intrinsicHeight

        val config = Bitmap.Config.ARGB_8888
        val bitmap = Bitmap.createBitmap(w, h, config)
        val canvas = Canvas(bitmap)
        this.setBounds(0, 0, w, h)
        this.draw(canvas)
        return bitmap
    }

    private fun Bitmap.addShadow(
        fromColor: Int, toColor: Int, percent: Float,
        vararg orientation: GradientDrawable.Orientation
    ): Bitmap {
        orientation.forEach {
            val mBackShadowColors = intArrayOf(fromColor, toColor)
            val mBackShadowDrawableLR = GradientDrawable(it, mBackShadowColors)
            val bound = Rect(0, 0, width, height)
            val percentHeight = (height * percent).roundToInt()
            val percentWidth = (width * percent).roundToInt()

            when (it) {
                GradientDrawable.Orientation.TOP_BOTTOM -> bound.set(0, 0, width, percentHeight)
                GradientDrawable.Orientation.RIGHT_LEFT -> bound.set(0, 0, percentWidth, height)
                GradientDrawable.Orientation.BOTTOM_TOP -> bound.set(0, height - percentHeight, width, height)
                GradientDrawable.Orientation.LEFT_RIGHT -> bound.set(width - percentWidth, 0, width, height)
                else -> {}
            }
            mBackShadowDrawableLR.bounds = bound
            mBackShadowDrawableLR.gradientType = GradientDrawable.LINEAR_GRADIENT
            mBackShadowDrawableLR.draw(Canvas(this))
        }
        return this
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec)
    }
}