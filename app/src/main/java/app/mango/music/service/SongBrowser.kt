package app.mango.music.service

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.SessionToken
import app.mango.music.data.ALL_ID
import app.mango.music.data.BaseMediaSource
import app.mango.music.data.GlobalData
import app.mango.music.data.ITEM_PREFIX
import app.mango.music.manager.Config
import com.blankj.utilcode.util.GsonUtils
import com.google.common.reflect.TypeToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class SongBrowser @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val mediaSource: BaseMediaSource
) : DefaultLifecycleObserver, CoroutineScope, EnhanceBrowser {

    companion object{
        private const val TAG = "SongBrowser"
    }

    override val coroutineContext: CoroutineContext = Dispatchers.IO
    private lateinit var browserFuture: ListenableFuture<MediaBrowser>

    private val lastPlayedSp: SharedPreferences by lazy {
        context.getSharedPreferences(Config.LAST_PLAYED_SP, Context.MODE_PRIVATE)
    }

    val browser: MediaBrowser?
        get() = if (browserFuture.isDone) browserFuture.get() else null

    var originPlaylistIds: List<String> = emptyList()

    @UnstableApi
    override fun onStart(owner: LifecycleOwner) {
        browserFuture = MediaBrowser.Builder(
            context, SessionToken(context, ComponentName(context, SongService::class.java))
        ).setListener(MyBrowserListener()).buildAsync()
        browserFuture.addListener({ onConnected() }, MoreExecutors.directExecutor())
    }

    override fun onStop(owner: LifecycleOwner) {
        MediaBrowser.releaseFuture(browserFuture)
    }

    private inner class MyBrowserListener : MediaBrowser.Listener {
        override fun onChildrenChanged(
            browser: MediaBrowser,
            parentId: String,
            itemCount: Int,
            params: MediaLibraryService.LibraryParams?
        ) {}
    }

    private fun onConnected() {
        val browser = browserFuture.get() ?: return

        if (browser.mediaItemCount == 0 || browser.currentMediaItem == null) {
            recoverLastPlayedItem()
        }

        browser.addListener(object : Player.Listener {
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                val ids = MutableList(browser.mediaItemCount) {
                    return@MutableList browser.getMediaItemAt(it).mediaId
                }

                launch(Dispatchers.Main) {
                    originPlaylistIds = ids
                    GlobalData.currentPlaylist.emit(ids.mapNotNull {
                        mediaSource.getItemById(
                            ITEM_PREFIX + it
                        )
                    })
                }
            }
        })
    }

    override fun togglePlay(): Boolean {
        when (browser?.isPlaying) {
            true -> browser?.pause()
            false -> browser?.play()
            else -> {}
        }
        return browser?.isPlaying == true
    }

    private fun recoverLastPlayedItem() =
        launch(Dispatchers.IO) {
            val items = recoverLastPlayedList()
            val index = lastPlayedSp.getString(Config.LAST_PLAYED_ID, null)?.let { id ->
                items.indexOfFirst { it.mediaId == id }
            }?.coerceAtLeast(0) ?: 0
            withContext(Dispatchers.Main) {
                browser?.setMediaItems(items)
                browser?.repeatMode = Player.REPEAT_MODE_ALL
                browser?.seekToDefaultPosition(index)
                browser?.prepare()
            }
        }

    private suspend fun recoverLastPlayedList(): List<MediaItem> =
        withContext(Dispatchers.IO) {
            lastPlayedSp.getString(Config.LAST_PLAYED_LIST, null)?.let { json ->
                val typeToken = object : TypeToken<List<String>>() {}
                return@withContext GsonUtils.fromJson<List<String>>(json, typeToken.type)
                    .mapNotNull { mediaSource.getItemById(ITEM_PREFIX + it) }
            }
            return@withContext mediaSource.getChildren(ALL_ID) ?: emptyList()
        }

    override fun playByUri(uri: Uri): Boolean {
        browser?.apply {
            addMediaItem(currentMediaItemIndex, MediaItem.fromUri(uri))
            seekToDefaultPosition(currentMediaItemIndex)
            prepare()
            play()
        }
        return true
    }

    override fun playById(mediaId: String): Boolean {
        return try {
            val index = originPlaylistIds.indexOf(mediaId)
            browser?.seekToDefaultPosition(index)
            index >= 0
        }catch (e:Exception){
            e.printStackTrace()
            false
        }
    }

    override fun playById(mediaId: String, playWhenReady: Boolean): Boolean {
        if (playById(mediaId) && playWhenReady) {
            browser?.apply {
                prepare()
                play()
            }
            return true
        }
        return false
    }

    override fun addToNext(mediaId: String): Boolean {
        val currentIndex = browser?.currentMediaItemIndex ?: return false
        if (currentIndex < 0) return false

        val nowIndex = originPlaylistIds.indexOf(mediaId)
        if (currentIndex == nowIndex || (currentIndex + 1) == nowIndex) return false

        if (nowIndex >= 0) {
            val targetIndex = if (nowIndex < currentIndex) currentIndex else currentIndex + 1
            browser?.moveMediaItem(nowIndex, targetIndex)
        } else {
            val item = mediaSource.getItemById(ITEM_PREFIX + mediaId) ?: return false
            browser?.addMediaItem(currentIndex + 1, item)
        }
        return true
    }

    private var lastRemovedItem: MediaItem? = null
    private var lastRemovedIndex: Int = -1
    private var lastPlayIndex: Int = -1

    override fun removeById(mediaId: String): Boolean {
        browser ?: return false
        return try {
            lastRemovedIndex = originPlaylistIds.indexOf(mediaId)
            if (lastRemovedIndex == browser!!.currentMediaItemIndex) {
                GlobalData.currentMediaItem.tryEmit(
                    browser!!.getMediaItemAt(browser!!.nextMediaItemIndex)
                )
            }
            lastPlayIndex = browser!!.currentMediaItemIndex
            lastRemovedItem = browser!!.getMediaItemAt(lastRemovedIndex)
            browser!!.removeMediaItem(lastRemovedIndex)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun revokeRemove(): Boolean {
        if (lastRemovedIndex < 0 || lastRemovedItem == null || browser == null)
            return false

        if (lastRemovedIndex >= browser!!.mediaItemCount) {
            browser!!.addMediaItem(lastRemovedItem!!)
        } else {
            browser!!.addMediaItem(lastRemovedIndex, lastRemovedItem!!)
        }

        if (lastPlayIndex == lastRemovedIndex) {
            browser!!.seekToDefaultPosition(lastPlayIndex)
        }
        return true
    }

    override fun moveByDelta(mediaId: String, delta: Int): Boolean {
        return try {
            val index = originPlaylistIds.indexOf(mediaId)
            browser?.moveMediaItem(index, index + delta)
            true
        } catch (e: Exception) {
            false
        }
    }
}