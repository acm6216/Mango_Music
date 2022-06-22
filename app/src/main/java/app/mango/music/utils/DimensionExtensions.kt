package app.mango.music.utils

import android.content.Context

object DimensionExtensions {

    private var oneDp = 0f
    fun init(context: Context){
        oneDp = context.dpToDimension(1f)
    }

    fun Int.dpToPx() = this* oneDp
}