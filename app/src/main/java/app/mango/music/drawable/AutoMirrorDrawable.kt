package app.mango.music.drawable

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.appcompat.graphics.drawable.DrawableWrapper
import androidx.core.graphics.drawable.DrawableCompat

@SuppressLint("RestrictedApi")
class AutoMirrorDrawable(drawable: Drawable) : DrawableWrapper(drawable) {
    override fun draw(canvas: Canvas) {
        if (needMirroring()) {
            val centerX = bounds.exactCenterX()
            canvas.scale(-1f, 1f, centerX, 0f)
            super.draw(canvas)
            canvas.scale(-1f, 1f, centerX, 0f)
        } else {
            super.draw(canvas)
        }
    }

    override fun onLayoutDirectionChanged(layoutDirection: Int): Boolean {
        super.onLayoutDirectionChanged(layoutDirection)

        return true
    }

    override fun isAutoMirrored(): Boolean = true

    private fun needMirroring(): Boolean = layoutDirectionCompat == View.LAYOUT_DIRECTION_RTL

    override fun getPadding(padding: Rect): Boolean {
        val hasPadding = super.getPadding(padding)
        if (needMirroring()) {
            val paddingStart = padding.left
            val paddingEnd = padding.right
            padding.left = paddingEnd
            padding.right = paddingStart
        }
        return hasPadding
    }

    private var Drawable.layoutDirectionCompat: Int
        get() = DrawableCompat.getLayoutDirection(this)
        set(value) {
            DrawableCompat.setLayoutDirection(this, value)
        }
}