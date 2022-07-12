package app.mango.music.views

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import androidx.media3.common.MediaItem
import app.mango.music.fetcher.getCoverFromMediaItem
import app.mango.music.utils.dpToDimension
import coil.loadAny
import coil.transform.RoundedCornersTransformation
import com.google.android.material.floatingactionbutton.FloatingActionButton

class PlayStateButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): FloatingActionButton(context, attrs, defStyleAttr){

    private var coverMediaItem:MediaItem? = null
    private val obj = ObjectAnimator.ofFloat(this, "rotation", 0f, 360f).apply{
        duration = 20000
        interpolator = LinearInterpolator()
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.RESTART
    }

    init {
        coverMediaItem?.let { setCover(it) }
    }

    fun isPlaying(isPlaying:Boolean){
        if(isPlaying) {
            if(obj.isPaused) obj.resume() else obj.start()
        }
        else obj.pause()
    }

    fun setCover(mediaItem: MediaItem?){
        mediaItem?:return
        coverMediaItem = mediaItem
        val samplingTo = width
        loadAny(mediaItem.getCoverFromMediaItem()) {
            if (samplingTo > 0) size(samplingTo)
            allowHardware(false)
            crossfade(150)
            transformations(RoundedCornersTransformation(context.dpToDimension(56f)/2f))
        }
    }
}