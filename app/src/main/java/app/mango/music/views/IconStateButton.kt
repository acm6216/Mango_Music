package app.mango.music.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatImageView
import app.mango.music.R
import app.mango.music.utils.interpolate
import app.mango.music.utils.interpolateArgb
import app.mango.music.utils.themeInterpolator
import kotlin.math.abs
import kotlin.math.sin

class IconStateButton  @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): AppCompatImageView(context, attrs, defStyleAttr) {

    private val defaultDrawable = IconStateActionDrawable(context)

    init {
        setImageDrawable(defaultDrawable)
    }

    inner class IconStateActionDrawable(val context: Context) : Drawable() {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorPrimary()
            style = Paint.Style.FILL
        }

        private val icon = drawable.apply {
            setTintMode(PorterDuff.Mode.MULTIPLY)
        }

        private val iconIntrinsicWidth = icon.intrinsicWidth
        private val iconIntrinsicHeight = icon.intrinsicHeight

        @ColorInt
        private val iconTint = colorControlNormal()
        @ColorInt
        private val iconTintActive = colorPrimary()

        private val iconMaxScaleAddition = 0.5F

        private var progress = 0F
            set(value) {
                val constrained = value.coerceIn(0F, 1F)
                if (constrained != field) {
                    field = constrained
                    callback?.invalidateDrawable(this)
                }
            }
        private var progressAnim: ValueAnimator? = null
        private val dur = context.resources.getInteger(R.integer.motion_duration_medium)
        private val interrupt = context.themeInterpolator(R.attr.motionInterpolatorPersistent)

        override fun onBoundsChange(bounds: Rect?) {
            if (bounds == null)  return
            update()
        }

        private fun update() {
            callback?.invalidateDrawable(this)
        }

        override fun isStateful(): Boolean = true

        override fun onStateChange(state: IntArray?): Boolean {
            val initialProgress = progress
            val newProgress = if (state?.contains(android.R.attr.state_activated) == true) {
                1F
            } else {
                0F
            }
            progressAnim?.cancel()
            progressAnim = ValueAnimator.ofFloat(initialProgress, newProgress).apply {
                addUpdateListener {
                    progress = animatedValue as Float
                }
                interpolator = interrupt
                duration = (abs(newProgress - initialProgress) * dur).toLong()
            }
            progressAnim?.start()
            return newProgress == initialProgress
        }

        override fun draw(canvas: Canvas) {

            val range = interpolate(
                0F,
                Math.PI.toFloat(),
                progress
            )

            val additive = (sin(range.toDouble()) * iconMaxScaleAddition).coerceIn(0.0, 1.0)
            val scaleFactor = 1 + additive
            icon.setBounds(
                (iconIntrinsicWidth / 2F - (iconIntrinsicWidth / 2F) * scaleFactor).toInt(),
                (iconIntrinsicHeight/2F - (iconIntrinsicHeight / 2F) * scaleFactor).toInt(),
                (iconIntrinsicWidth / 2F + (iconIntrinsicWidth / 2F) * scaleFactor).toInt(),
                (iconIntrinsicHeight/2F + (iconIntrinsicHeight / 2F) * scaleFactor).toInt()
            )

            icon.setTint(
                interpolateArgb(iconTint, iconTintActive, 0F, 0.15F, progress)
            )

            icon.draw(canvas)
        }

        override fun setAlpha(alpha: Int) {
            paint.alpha = alpha
        }

        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

        override fun setColorFilter(filter: ColorFilter?) {
            paint.colorFilter = filter
        }

        private fun colorControlNormal(): Int = getColorForAttrId(androidx.appcompat.R.attr.colorControlNormal)

        private fun getColorForAttrId(@AttrRes resId:Int):Int{
            val typedValue = TypedValue()
            val a = context.obtainStyledAttributes(typedValue.data, intArrayOf(resId))
            val color = a.getColor(0, Color.BLUE)
            a.recycle()
            return color
        }

        private fun colorPrimary(): Int = getColorForAttrId(androidx.appcompat.R.attr.colorPrimary)

    }

}