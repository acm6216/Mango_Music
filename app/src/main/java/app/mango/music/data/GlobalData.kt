package app.mango.music.data

import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import app.mango.music.audio.partCopy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

object GlobalData: CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.IO

    private val handler = Handler(Looper.getMainLooper())
    var getIsPlayingFromPlayer: () -> Boolean = { false }
    var getPositionFromPlayer: () -> Long = { 0L }
        set(value) {
            field = value
            stopUpdate()
            updatePosition()
        }
    private fun stopUpdate() {
        handler.removeCallbacks(this::updatePosition)
    }

    val currentIsPlaying: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val currentMediaItem: MutableStateFlow<MediaItem?> = MutableStateFlow(null)
    val currentPlaylist: MutableStateFlow<List<MediaItem>> = MutableStateFlow(emptyList())
    val currentPosition: MutableStateFlow<Long> = MutableStateFlow(0L)

    val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            launch { currentMediaItem.emit(mediaItem) }
            updatePosition(true)
        }
    }

    private var lastPlayState = false
    fun updatePosition(force: Boolean = false) {
        if (force) {
            val isPlaying = getIsPlayingFromPlayer()
            val position = getPositionFromPlayer()
            launch {
                currentIsPlaying.emit(isPlaying)
                currentPosition.emit(position)
            }
            return
        }
        val isPlaying = getIsPlayingFromPlayer()
        if (lastPlayState == isPlaying) {
            val position = getPositionFromPlayer()
            launch {
                currentIsPlaying.emit(isPlaying)
                currentPosition.emit(position)
            }
        } else {
            lastPlayState = isPlaying
        }
        handler.postDelayed(this::updatePosition, 100)
    }

    suspend fun updateCurrentMediaItem(targetMediaItemId: String) = withContext(Dispatchers.IO) {
        val mediaItem = currentMediaItem.value
            ?: return@withContext

        if (mediaItem.mediaId == targetMediaItemId) {
            currentMediaItem.emit(mediaItem.partCopy())
        }
    }

}