package app.mango.music.views

import android.content.Context
import android.util.AttributeSet
import android.widget.Checkable
import androidx.appcompat.widget.AppCompatImageView
import app.mango.music.drawable.PlayPauseDrawable

class PlayButtonMin @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): AppCompatImageView(context, attrs, defStyleAttr), Checkable {

    private var _checked = false

    private val drawable = PlayPauseDrawable(context)

    override fun setChecked(checked: Boolean) {
        _checked = checked
        if (!checked) {
            drawable.setPlay(true)
        } else {
            drawable.setPause(true)
        }
        setImageDrawable(drawable)
    }

    override fun isChecked(): Boolean = _checked

    override fun toggle() {
        isChecked = !_checked
    }
}