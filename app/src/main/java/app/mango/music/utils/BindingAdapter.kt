package app.mango.music.utils

import android.annotation.SuppressLint
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.media3.common.MediaItem
import app.mango.music.R
import app.mango.music.audio.*
import app.mango.music.fetcher.getCoverFromMediaItem
import app.mango.music.views.CoverImageView
import app.mango.music.views.lrcview.LrcViewKt
import coil.loadAny
import coil.transform.RoundedCornersTransformation
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

@BindingAdapter("info_type","info_detail")
fun setMusicInfo(view: TextView,musicBox: MusicBox,detail: Detail?){
    detail?:return
    val mediaItem = detail.mediaItem
    view.text = when(musicBox){
        MusicBox.ARTIST ->"ARTIST ID:${mediaItem.mediaMetadata.getArtistId()}"
        else -> "ALBUM ID:${mediaItem.mediaMetadata.getAlbumId()}"
    }
}

private fun Long.duration() = getSafeDateFormat(if(this>1000*60*60) "H:mm:ss" else "mm:ss").format(Date(this))

private val SDF_THREAD_LOCAL: ThreadLocal<Map<String, SimpleDateFormat>> =
    object : ThreadLocal<Map<String, SimpleDateFormat>>() {
        override fun initialValue(): Map<String, SimpleDateFormat> {
            return HashMap()
        }
    }
@SuppressLint("SimpleDateFormat")
private fun getSafeDateFormat(pattern: String): SimpleDateFormat {
    val sdfMap: HashMap<String, SimpleDateFormat> =
        SDF_THREAD_LOCAL.get() as HashMap<String, SimpleDateFormat>
    var simpleDateFormat = sdfMap[pattern]
    if (simpleDateFormat == null) {
        simpleDateFormat = SimpleDateFormat(pattern)
        sdfMap[pattern] = simpleDateFormat
    }
    return simpleDateFormat
}

@BindingAdapter("music_info")
fun setMusicInfo(textView: TextView,mediaItem: MediaItem?){
    mediaItem?:return
    textView.text = "${mediaItem.mediaMetadata.artist} - ${mediaItem.mediaMetadata.albumTitle}"
}

@BindingAdapter("music_duration")
fun setMusicDuration(view: TextView,detail: Detail?){
    detail?:return
    view.text = detail.duration.duration()
}

@BindingAdapter("music_duration")
fun setMusicDuration(view: TextView, value: PlayValue?){
    value?:return
    view.text = value.value.toLong().duration()
}

@BindingAdapter("title_type","title_detail")
fun setMusicDetailTitle(view: TextView,musicBox: MusicBox,mediaItem: MediaItem?){
    mediaItem?:return
    view.text = when(musicBox){
        MusicBox.ARTIST -> mediaItem.mediaMetadata.artist
        else -> mediaItem.mediaMetadata.albumTitle
    }
}

@SuppressLint("SetTextI18n")
@BindingAdapter("music_type")
fun setMusicType(textView: TextView, mediaItem: MediaItem?) {
    mediaItem ?: return
    val strings = mediaItem.localConfiguration!!.mimeType!!.split("/").toTypedArray()
    textView.text = "${strings[strings.size - 1].uppercase()} - ${mediaItem.mediaMetadata.getDuration().duration()}"
}

@BindingAdapter(value = ["loadCover", "samplingValue"], requireAll = false)
fun loadCover(imageView: CoverImageView, mediaItem: MediaItem?, samplingValue: Int = -1) {
    mediaItem ?: run {
        imageView.clearImage()
        return
    }
    val samplingTo = if (samplingValue <= 0)
        imageView.width else samplingValue

    imageView.loadAny(mediaItem.getCoverFromMediaItem()) {
        if (samplingTo > 0) size(samplingTo)
        allowHardware(false)
        target(onSuccess = {
            imageView.loadImageFromDrawable(it)
        }, onError = {
            imageView.clearImage()
        }).build()
    }
}

@BindingAdapter("load_lyric")
fun LrcViewKt.loadLyric(mediaItem: MediaItem?){
    mediaItem?:return
    loadLrc(mediaItem)
}

@BindingAdapter(value = ["loadCover", "samplingValue"], requireAll = false)
fun loadCover(imageView: ImageView, mediaItem: MediaItem?, samplingValue: Int = -1) {
    mediaItem ?: return
    val samplingTo = if (samplingValue <= 0)
        imageView.width else samplingValue

    imageView.loadAny(mediaItem.getCoverFromMediaItem()) {
        if (samplingTo > 0) size(samplingTo)
        error(R.drawable.ic_music)
        crossfade(150)
    }
}

@BindingAdapter("loadMusicCover")
fun loadMusicCover(imageView: ImageView,mediaItem: MediaItem?){
    mediaItem?:return
    val samplingTo = imageView.width

    imageView.loadAny(mediaItem.getCoverFromMediaItem()) {
        if (samplingTo > 0) size(samplingTo)
        error(R.drawable.ic_music)
        crossfade(150)
        transformations(RoundedCornersTransformation(imageView.context.dpToDimension(42/2f)))
    }
}

