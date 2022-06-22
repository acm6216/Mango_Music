package app.mango.music.utils

import android.graphics.Color
import androidx.palette.graphics.Palette

fun Palette?.getAutomaticColor(): Int {
    if (this == null) return Color.DKGRAY
    var oldColor = this.getDarkVibrantColor(Color.LTGRAY)
    if (isLightColor(oldColor))
        oldColor = this.getDarkMutedColor(Color.LTGRAY)
    return oldColor
}

fun isLightColor(color: Int): Boolean {
    val darkness =
        1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(
            color
        )) / 255
    return darkness < 0.5
}