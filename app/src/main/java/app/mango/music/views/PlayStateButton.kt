package app.mango.music.views

import android.content.Context
import android.util.AttributeSet
import app.mango.music.drawable.PlayStateDrawable
import com.google.android.material.floatingactionbutton.FloatingActionButton

class PlayStateButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): FloatingActionButton(context, attrs, defStyleAttr){

    private val playStateDrawable = PlayStateDrawable(context)
    init {
        setImageDrawable(playStateDrawable)
    }
    fun setInfinite(infinite:Boolean) = playStateDrawable.setInfinite(infinite)
}