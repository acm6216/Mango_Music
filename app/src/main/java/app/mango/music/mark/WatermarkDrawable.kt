package app.mango.music.mark

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import androidx.annotation.IntRange
import kotlin.math.sqrt

internal class WatermarkDrawable(val context: Context) : Drawable() {

    private val paint: Paint = Paint()
    var text = ""
    var textColor = 0
    var textSize = 0f
    var rotation = 0f

    override fun draw(canvas: Canvas) {
        val width = bounds.right
        val height = bounds.bottom
        val diagonal = sqrt((width * width + height * height).toDouble()).toInt()
        paint.color = textColor
        paint.textSize =
            spToPx(textSize).toFloat()
        paint.isAntiAlias = true
        val textWidth = paint.measureText(text)
        canvas.drawColor(0x00000000)
        canvas.rotate(rotation)
        var index = 0
        var fromX: Float
        var positionY = diagonal / 10
        while (positionY <= diagonal) {
            fromX = -width + index++ % 2 * textWidth
            var positionX = fromX
            while (positionX < width) {
                canvas.drawText(text, positionX, positionY.toFloat(), paint)
                positionX += textWidth * 2
            }
            positionY += diagonal / 10
        }
        canvas.save()
        canvas.restore()
    }

    override fun setAlpha(@IntRange(from = 0, to = 255) alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private fun spToPx(spValue: Float): Int {
        val fontScale = context.resources.displayMetrics.scaledDensity
        return (spValue * fontScale + 0.5f).toInt()
    }
}