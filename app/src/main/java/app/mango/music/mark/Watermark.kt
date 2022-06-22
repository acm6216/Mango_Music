package app.mango.music.mark

import android.app.Activity
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import android.R

internal class Watermark private constructor() {

    private var text = ""
    private var textColor = -0x51515152
    private var textSize = 18f
    private var rotation = -25f

    fun setText(text: String): Watermark {
        this.text = text
        return sInstance
    }

    fun setTextColor(color: Int): Watermark {
        textColor = color
        return sInstance
    }

    fun setTextSize(size: Float): Watermark {
        textSize = size
        return sInstance
    }

    fun setRotation(degrees: Float): Watermark {
        rotation = degrees
        return sInstance
    }

    @JvmOverloads
    fun show(activity: Activity, text: String = this.text): Watermark {
        val drawable = WatermarkDrawable(activity)
        drawable.text = text
        drawable.textColor = textColor
        drawable.textSize = textSize
        drawable.rotation = rotation
        val rootView = activity.findViewById<ViewGroup>(R.id.content)
        val layout = FrameLayout(activity)
        layout.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        layout.background = drawable
        rootView.addView(layout)
        return instance
    }

    fun saturation(window: Window?, isShow:Boolean){
        window?.decorView?.setLayerType(View.LAYER_TYPE_HARDWARE, Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
                setSaturation(if(isShow) 0f else 1f)
            })
        })
    }

    companion object {
        private lateinit var sInstance: Watermark
        val instance: Watermark
            get() {
                if (!this::sInstance.isInitialized) {
                    synchronized(Watermark::class.java) {
                        sInstance = Watermark()
                    }
                }
                return sInstance
            }
    }

}