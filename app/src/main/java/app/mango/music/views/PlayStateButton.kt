package app.mango.music.views

import android.content.Context
import android.util.AttributeSet
import androidx.media3.common.MediaItem
import app.mango.music.fetcher.getCoverFromMediaItem
import coil.loadAny
import coil.transform.RoundedCornersTransformation
import com.google.android.material.floatingactionbutton.FloatingActionButton

class PlayStateButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): FloatingActionButton(context, attrs, defStyleAttr){

    fun setCover(mediaItem: MediaItem?){
        mediaItem?:return
        val samplingTo = width
        loadAny(mediaItem.getCoverFromMediaItem()) {
            if (samplingTo > 0) size(samplingTo)
            allowHardware(false)
            crossfade(150)
            transformations(RoundedCornersTransformation(width/2f))

        }
    }
}