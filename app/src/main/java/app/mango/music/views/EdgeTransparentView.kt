package app.mango.music.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.FrameLayout
import app.mango.music.R

/**
 * 根据
 * https://gitee.com/tryohang/EdgeTranslucent/
 * 修改
 */
class EdgeTransparentView  @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): FrameLayout(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
    }
    private var position = 0
    private var drawSize = 0f

    private val topMask = 0x01
    private val bottomMask = topMask shl 1
    private val leftMask = topMask shl 2
    private val rightMask = topMask shl 3

    private var mWidth = 0
    private var mHeight = 0

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.EdgeTransparentView)
        position = typedArray.getInt(R.styleable.EdgeTransparentView_edge_position, 0)
        drawSize = typedArray.getDimension(R.styleable.EdgeTransparentView_edge_width, 20f.dp())
        typedArray.recycle()
    }

    override fun dispatchDraw(canvas: Canvas?) {
        super.dispatchDraw(canvas)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        initShader()
        mWidth = width
        mHeight = height
    }

    private val mGradientColors = intArrayOf(-0x1, 0x00000000)
    private val mGradientPosition = floatArrayOf(0f, 1f)
    private fun initShader() {
        paint.shader = LinearGradient(
            0f, 0f, 0f, drawSize,
            mGradientColors, mGradientPosition,
            Shader.TileMode.CLAMP
        )
    }

    override fun drawChild(canvas: Canvas, child: View?, drawingTime: Long): Boolean {
        val layerSave = canvas.saveLayer(
            0f, 0f, width.toFloat(),
            height.toFloat(), null
        )
        val drawChild = super.drawChild(canvas, child, drawingTime)
        if (position == 0 || position and topMask != 0) {
            canvas.drawRect(0f, 0f, mWidth.toFloat(), drawSize, paint)
        }
        if (position == 0 || position and bottomMask != 0) {
            val save = canvas.save()
            canvas.rotate(180f, (mWidth / 2).toFloat(), (mHeight / 2).toFloat())
            canvas.drawRect(0f, 0f, mWidth.toFloat(), drawSize, paint)
            canvas.restoreToCount(save)
        }
        val offset = (mHeight - mWidth) / 2
        if (position == 0 || position and leftMask != 0) {
            val saveCount = canvas.save()
            canvas.rotate(90f, (mWidth / 2).toFloat(), (mHeight / 2).toFloat())
            canvas.translate(0f, offset.toFloat())
            canvas.drawRect(
                (0 - offset).toFloat(),
                0f, (mWidth + offset).toFloat(),
                drawSize, paint
            )
            canvas.restoreToCount(saveCount)
        }
        if (position == 0 || position and rightMask != 0) {
            val saveCount = canvas.save()
            canvas.rotate(270f, (mWidth / 2).toFloat(), (mHeight / 2).toFloat())
            canvas.translate(0f, offset.toFloat())
            canvas.drawRect(
                (0 - offset).toFloat(),
                0f, (mWidth + offset).toFloat(),
                drawSize, paint
            )
            canvas.restoreToCount(saveCount)
        }
        canvas.restoreToCount(layerSave)
        return drawChild
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
    }

    private fun Float.dp(): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, this, context.resources.displayMetrics
        )
    }
}