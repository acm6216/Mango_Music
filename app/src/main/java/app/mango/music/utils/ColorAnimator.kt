package app.mango.music.utils

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Build
import androidx.palette.graphics.Palette

object ColorUtils {
    fun isLightColor(color: Int): Boolean {
        val darkness =
            1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(
                color
            )) / 255
        return darkness < 0.5
    }

    fun getAutomaticColor(palette: Palette?): Int {
        if (palette == null) return Color.DKGRAY
        //var oldColor = palette.getDarkMutedColor(Color.LTGRAY) text
        var oldColor = palette.getLightVibrantColor(Color.LTGRAY)
        //if (!isLightColor(oldColor))
        //    oldColor = palette.getLightVibrantColor(Color.LTGRAY)
        return oldColor
    }
}
object ColorAnimator {
    private val colorMap: LinkedHashMap<Int, Int> = LinkedHashMap()
    private var transitionDuration = 600L

    fun setBgColorFromPalette(
        palette: Palette?,
        setColor: (Int) -> Unit
    ) {
        val plColor = ColorUtils.getAutomaticColor(palette)
        setBgColor(plColor, setColor)
    }

    fun setBgColor(
        plColor: Int,
        setColor: (Int) -> Unit
    ) {
        val id = setColor.hashCode()
        val oldColor = colorMap[id] ?: Color.DKGRAY

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ValueAnimator.ofArgb(oldColor, plColor).apply {
                duration = transitionDuration
                addUpdateListener {
                    val color = it.animatedValue as Int
                    setColor(color)
                }
            }.start()
        } else {
            setColor(plColor)
        }
        colorMap[id] = plColor
    }
}
