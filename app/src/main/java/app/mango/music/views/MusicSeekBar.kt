package app.mango.music.views

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatSeekBar

class MusicSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): AppCompatSeekBar(context, attrs, defStyleAttr){

    private var isTouch = false
    private var progressChanged:((Int)->Unit)? = null
    private val seekbarListener = object:OnSeekBarChangeListener{
        override fun onProgressChanged(seekbar: SeekBar, p1: Int, p2: Boolean) {

        }

        override fun onStartTrackingTouch(seekbar: SeekBar) {

        }

        override fun onStopTrackingTouch(seekbar: SeekBar) {
            progressChanged?.invoke(seekbar.progress)
        }
    }

    init {
        setOnSeekBarChangeListener(seekbarListener)
    }

    fun setProgressListener(unit:((Int)->Unit)){
        progressChanged = unit
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        isTouch = (event.action!=MotionEvent.ACTION_UP && event.action!=MotionEvent.ACTION_CANCEL)
        return super.onTouchEvent(event)
    }

    override fun setProgress(progress: Int) {
        if(isTouch) return
        super.setProgress(progress)
    }
}