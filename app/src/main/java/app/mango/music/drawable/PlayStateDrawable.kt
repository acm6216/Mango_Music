package app.mango.music.drawable

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.core.animation.doOnEnd
import app.mango.music.R
import app.mango.music.utils.dpToDimension
import app.mango.music.utils.interpolate
import app.mango.music.utils.themeInterpolator
import kotlin.math.abs
import kotlin.random.Random

class PlayStateDrawable(val context: Context) : Drawable() {

    private fun colorControlNormal(): Int = getColorForAttrId(androidx.appcompat.R.attr.colorControlNormal)

    private fun getColorForAttrId(@AttrRes resId:Int):Int{
        val typedValue = TypedValue()
        val a = context.obtainStyledAttributes(typedValue.data, intArrayOf(resId))
        val color = a.getColor(0, Color.BLUE)
        a.recycle()
        return color
    }

    private val paint = Paint().apply {
        color = colorControlNormal()
    }

    private val height = intrinsicHeight.toFloat()

    private var progress = 0F
        set(value) {
            val constrained = value.coerceIn(0F, 1F)
            if (constrained != field) {
                field = constrained
                callback?.invalidateDrawable(this)
            }
        }
    private var progressAnim: ValueAnimator? = null

    private val random:Random = Random.Default
    init {
        setAnimate()
    }

    private val dur = context.resources.getInteger(R.integer.motion_duration_medium)
    private val interrupt = context.themeInterpolator(R.attr.motionInterpolatorPersistent)

    private fun update() {
        callback?.invalidateDrawable(this)
    }

    override fun onBoundsChange(bounds: Rect?) {
        if (bounds == null)  return
        update()
    }

    private var isInfinite = false
    private val edge = context.dpToDimension(5f).toInt()
    private val mid = context.dpToDimension(3.5f).toInt()
    private val weight = context.dpToDimension(3f).toInt()
    private val small = context.dpToDimension(8f).toInt()
    private val large = context.dpToDimension(4f).toInt()

    override fun draw(canvas: Canvas) {
        val learn = interpolate(
            0,large,
            0F,1F,
            progress
        )

        canvas.drawRect(Rect(
            edge,
            small-learn,
            edge+weight,
            height.toInt()-small+learn
        ),paint)

        canvas.drawRect(Rect(
            edge+mid+weight,
            large-learn,
            weight*2+edge+mid,
            height.toInt()-large+learn
        ),paint)

        canvas.drawRect(Rect(
            weight*2+edge+mid*2,
            small-learn,
            weight*3+edge+mid*2,
            height.toInt()-small+learn
        ),paint)

        canvas.drawRect(Rect(
            -weight/3,
            (small*1.1).toInt(),
            edge-weight,
            (height-small*1.1).toInt()
        ),paint)

        canvas.drawRect(Rect(
            weight*2+mid*2+edge*2+weight/4,
            (small*1.1).toInt(),
            weight*3+mid*2+edge*2+weight/4,
            (height-small*1.1).toInt()
        ),paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    private fun setAnimate(){
        val newProgress = random.nextDouble(0.0,1.0).toFloat()
        val initialProgress = progress
        progressAnim?.cancel()
        progressAnim = ValueAnimator.ofFloat(initialProgress, newProgress).apply {
            addUpdateListener {
                progress = animatedValue as Float
            }
            doOnEnd {
                if(isInfinite) setAnimate()
            }
            interpolator = interrupt
            duration = (abs(newProgress - initialProgress) * dur).toLong()
        }
        progressAnim?.start()
    }

    fun setInfinite(infinite:Boolean){
        if(!isInfinite && infinite) setAnimate()
        isInfinite = infinite
    }

    override fun setColorFilter(cf: ColorFilter?) {
        cf?.let { paint.colorFilter = cf }
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun getIntrinsicHeight(): Int = context.dpToDimension(24f).toInt()

    override fun getIntrinsicWidth(): Int = context.dpToDimension(24f).toInt()

}