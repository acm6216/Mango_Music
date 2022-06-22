package app.mango.music.drawable

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.Property
import android.util.TypedValue
import android.view.animation.DecelerateInterpolator
import androidx.annotation.AttrRes
import androidx.core.animation.doOnEnd
import app.mango.music.R
import kotlin.math.roundToInt

class PlayPauseDrawable(val context: Context) : Drawable() {
    private val leftPauseBar = Path()
    private val rightPauseBar = Path()
    private val paint = Paint().apply {
        color = colorControlNormal()
    }
    private val pauseBarWidth: Float
    private val pauseBarHeight: Float
    private val pauseBarDistance: Float
    private var width = 0f
    private var height = 0f
    private var progress = 0f
    private var isPlay = false
    private var isPlaySet = false
    private var animator: Animator? = null

    private fun colorControlNormal(): Int = getColorForAttrId(androidx.appcompat.R.attr.colorControlNormal)

    private fun getColorForAttrId(@AttrRes resId:Int):Int{
        val typedValue = TypedValue()
        val a = context.obtainStyledAttributes(typedValue.data, intArrayOf(resId))
        val color = a.getColor(0, Color.BLUE)
        a.recycle()
        return color
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        if (bounds.width() > 0 && bounds.height() > 0) {
            width = bounds.width().toFloat()
            height = bounds.height().toFloat()
        }
    }

    override fun draw(canvas: Canvas) {
        leftPauseBar.rewind()
        rightPauseBar.rewind()
        val barDist = leap(pauseBarDistance, 0f, progress)
        val rawBarWidth = leap(pauseBarWidth, pauseBarHeight / 1.75f, progress)
        val barWidth = if (progress == 1f) rawBarWidth.roundToInt().toFloat() else rawBarWidth
        val firstBarTopLeft = leap(0f, barWidth, progress)
        val secondBarTopRight = leap(2f * barWidth + barDist, barWidth + barDist, progress)
        leftPauseBar.moveTo(0f, 0f)
        leftPauseBar.lineTo(firstBarTopLeft, -pauseBarHeight)
        leftPauseBar.lineTo(barWidth, -pauseBarHeight)
        leftPauseBar.lineTo(barWidth, 0f)
        leftPauseBar.close()
        rightPauseBar.moveTo(barWidth + barDist, 0f)
        rightPauseBar.lineTo(barWidth + barDist, -pauseBarHeight)
        rightPauseBar.lineTo(secondBarTopRight, -pauseBarHeight)
        rightPauseBar.lineTo(2 * barWidth + barDist, 0f)
        rightPauseBar.close()
        val saveCount = canvas.save()
        canvas.translate(leap(0f, pauseBarHeight / 8f, progress), 0f)
        val rotationProgress = if (isPlay) 1f - progress else progress
        val startingRotation = if (isPlay) 90f else 0f
        canvas.rotate(
            leap(startingRotation, startingRotation + 90f, rotationProgress),
            width / 2f,
            height / 2f
        )
        canvas.translate(
            (width / 2f - (2f * barWidth + barDist) / 2f).roundToInt().toFloat(),
            (height / 2f + pauseBarHeight / 2f).roundToInt().toFloat()
        )
        canvas.drawPath(leftPauseBar, paint)
        canvas.drawPath(rightPauseBar, paint)
        canvas.restoreToCount(saveCount)
    }

    private val pausePlayAnimator: Animator
        get() {
            isPlaySet = !isPlaySet
            return ObjectAnimator.ofFloat(
                this,
                PROGRESS,
                if (isPlay) 1f else 0f,
                if (isPlay) 0f else 1f
            ).apply {
                doOnEnd { isPlay = !isPlay }
            }
        }

    private fun setProgress(progress: Float) {
        this.progress = progress
        invalidateSelf()
    }

    private fun getProgress(): Float = progress

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(cf: ColorFilter?) {
        paint.colorFilter = cf
        invalidateSelf()
    }

    override fun setTint(color:Int){
        paint.color = color
        invalidateSelf()
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    fun setPlay(animate: Boolean) {
        if (animate) {
            if (!isPlaySet) togglePlayPause()
        } else {
            isPlaySet = true
            isPlay = true
            setProgress(1f)
        }
    }

    fun setPause(animate: Boolean) {
        if (animate) {
            if (isPlaySet) {
                togglePlayPause()
            }
        } else {
            isPlaySet = false
            isPlay = false
            setProgress(0f)
        }
    }

    private fun togglePlayPause() {
        animator?.cancel()
        animator = pausePlayAnimator
        animator?.let{
            it.interpolator = DecelerateInterpolator()
            it.duration = PLAY_PAUSE_ANIMATION_DURATION
            it.start()
        }
    }

    companion object {
        private const val PLAY_PAUSE_ANIMATION_DURATION: Long = 250
        private val PROGRESS: Property<PlayPauseDrawable, Float> =
            object : Property<PlayPauseDrawable, Float>(
                Float::class.java, "progress"
            ) {
                override fun get(d: PlayPauseDrawable): Float {
                    return d.getProgress()
                }

                override fun set(d: PlayPauseDrawable, value: Float) {
                    d.setProgress(value)
                }
            }

        private fun leap(a: Float, b: Float, t: Float): Float {
            return a + (b - a) * t
        }
    }

    init {
        val res = context.resources
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        pauseBarWidth = res.getDimensionPixelSize(R.dimen.pause_bar_width).toFloat()
        pauseBarHeight = res.getDimensionPixelSize(R.dimen.pause_bar_height).toFloat()
        pauseBarDistance = res.getDimensionPixelSize(R.dimen.pause_bar_distance).toFloat()
    }
}
