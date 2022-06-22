package app.mango.music.service

import android.app.PendingIntent
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import app.mango.music.data.BaseMediaSource
import app.mango.music.data.GlobalData
import app.mango.music.data.ITEM_PREFIX
import app.mango.music.manager.AudioEffectManager
import app.mango.music.manager.Config
import app.mango.music.manager.SpManager
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.SPUtils
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * 使用
 * https://gitee.com/lalilu/lmusic/
 */
@UnstableApi
@AndroidEntryPoint
class SongService : MediaLibraryService(), CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.IO
    private lateinit var player: Player
    private lateinit var exoPlayer: ExoPlayer

    private lateinit var audioInit: AudioInit
    private lateinit var mediaLibrarySession: MediaLibrarySession
    private lateinit var mediaController: MediaController

    @Inject
    lateinit var mediaSource: BaseMediaSource

    @Inject
    lateinit var notificationProvider: MusicNotificationProvider

    private val lastPlayedSp: SPUtils by lazy {
        SPUtils.getInstance(Config.LAST_PLAYED_SP)
    }

    private val audioAttributes = AudioAttributes.Builder()
        .setContentType(C.CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    override fun onCreate() {
        super.onCreate()
        exoPlayer = ExoPlayer.Builder(this)
            .setUseLazyPreparation(false)
            .setHandleAudioBecomingNoisy(true)
            .build()
        audioInit = AudioInit(exoPlayer.audioSessionId).bind()

        player = object : ForwardingPlayer(exoPlayer) {
            override fun getMaxSeekToPreviousPosition(): Long = Long.MAX_VALUE
            override fun seekToPrevious() {
                if (player.hasPreviousMediaItem() && player.currentPosition <= maxSeekToPreviousPosition) {
                    seekToPreviousMediaItem()
                    return
                }
                super.seekToPrevious()
            }
        }

        SpManager.listen(
            Config.KEY_SETTINGS_IGNORE_AUDIO_FOCUS,
            SpManager.SpBoolListener(Config.DEFAULT_SETTINGS_IGNORE_AUDIO_FOCUS) {
                exoPlayer.setAudioAttributes(audioAttributes, !it)
            })

        SpManager.listen(
            Config.KEY_SETTINGS_VOLUME,
            SpManager.SpIntListener(Config.DEFAULT_SETTINGS_VOLUME){
                exoPlayer.volume = it.toFloat()/100
            })

        SpManager.listen(Config.KEY_SETTINGS_REPEAT_MODE,
            SpManager.SpIntListener(Config.DEFAULT_SETTINGS_REPEAT_MODE) {
                exoPlayer.shuffleModeEnabled = it == 2
                exoPlayer.repeatMode =
                    if (it == 1) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_ALL
            })

        SpManager.listen(Config.KEY_SETTINGS_PLAYBACK_PARAMETERS,
            SpManager.SpStringListener(Config.DEFAULT_SETTINGS_PLAYBACK_PARAMETERS){
                exoPlayer.playbackParameters = PlaybackParameters(it.toFloat())
        })

        val pendingIntent: PendingIntent =
            packageManager.getLaunchIntentForPackage(packageName).let { sessionIntent ->
                PendingIntent.getActivity(
                    this, 0, sessionIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

        mediaLibrarySession =
            MediaLibrarySession.Builder(this, player, CustomMediaLibrarySessionCallback())
                .setMediaItemFiller(CustomMediaItemFiller())
                .setSessionActivity(pendingIntent)
                .build()

        val controllerFuture =
            MediaController.Builder(this, mediaLibrarySession.token)
                .buildAsync()

        controllerFuture.addListener({
            mediaController = controllerFuture.get()
            mediaController.addListener(GlobalData.playerListener)
            mediaController.addListener(LastPlayedListener())
            GlobalData.getIsPlayingFromPlayer = mediaController::isPlaying
            GlobalData.getPositionFromPlayer = mediaController::getCurrentPosition
        }, MoreExecutors.directExecutor())

        setMediaNotificationProvider(notificationProvider)
    }

    private inner class LastPlayedListener : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            launch {
                lastPlayedSp.put(Config.LAST_PLAYED_ID, mediaItem?.mediaId)
            }
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            val list = List(mediaController.mediaItemCount) {
                mediaController.getMediaItemAt(it).mediaId
            }
            launch {
                lastPlayedSp.put(Config.LAST_PLAYED_LIST, GsonUtils.toJson(list))
            }
        }
    }

    private inner class AudioInit(audioSessionId:Int){

        private val audioEffectManager = AudioEffectManager(audioSessionId)

        fun release() = audioEffectManager.release()

        private val equalizer: AudioEffectManager.XEqualizer
        get() = audioEffectManager.equalizer

        private val bassBoost: AudioEffectManager.XBassBoost
        get() = audioEffectManager.bassBoost

        fun bind():AudioInit{

            val numberFrequencyBands: Short = equalizer.numberOfBands
            val lowerEqualizerBandLevelMilliBel: Short = equalizer.bandLevelRange[0]
            val upperEqualizerBandLevelMilliBel: Short = equalizer.bandLevelRange[1]

            SpManager.listen(Config.KEY_SETTINGS_AUDIO_EFFECT,
                SpManager.SpBoolListener(Config.DEFAULT_SETTINGS_AUDIO_EFFECT){
                    equalizer.enabled = it
                })

            SpManager.listen(Config.KEY_SETTINGS_AUDIO_EFFECT_BOOST,
                SpManager.SpBoolListener(Config.DEFAULT_SETTINGS_AUDIO_EFFECT_BOOST){
                    bassBoost.enabled = it
                })

            SpManager.listen(Config.KEY_SETTINGS_AUDIO_EFFECT_BOOST_VALUE,
                SpManager.SpIntListener(Config.DEFAULT_SETTINGS_AUDIO_EFFECT_BOOST_VALUE){
                    bassBoost.setStrength(it.toShort())
                })

            SpManager.listen(Config.KEY_SETTINGS_AUDIO_EFFECT_VALUE,
                SpManager.SpStringListener(getDefArray(numberFrequencyBands,upperEqualizerBandLevelMilliBel - lowerEqualizerBandLevelMilliBel)){
                    it.split("#").forEachIndexed { index, s ->
                        equalizer.setBandLevel(
                            index.toShort(),
                            (s.substring(s.indexOf(":")+1,s.indexOf("]"))).toShort()
                        )
                    }
                })
            return this
        }
    }

    private fun getDefArray(count:Short, value:Int):String{
        val result = StringBuilder()
        for (i in 0 until count){
            result.append("[$i:$value]#")
        }
        return result.substring(0,result.length-1)
    }

    private inner class CustomMediaItemFiller : MediaSession.MediaItemFiller {
        override fun fillInLocalConfiguration(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItem: MediaItem
        ): MediaItem {
            return mediaSource.getItemById(ITEM_PREFIX + mediaItem.mediaId) ?: mediaItem
        }
    }

    private inner class CustomMediaLibrarySessionCallback :
        MediaLibrarySession.MediaLibrarySessionCallback {
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            return Futures.immediateFuture(LibraryResult.ofItem(mediaSource.getRootItem(), params))
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val item = mediaSource.getItemById(mediaId) ?: return Futures.immediateFuture(
                LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
            )
            return Futures.immediateFuture(LibraryResult.ofItem(item, null))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val children = mediaSource.getChildren(parentId) ?: return Futures.immediateFuture(
                LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
            )
            return Futures.immediateFuture(LibraryResult.ofItemList(children, params))
        }
    }

    override fun onDestroy() {
        player.release()
        audioInit.release()
        mediaLibrarySession.release()
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession {
        return mediaLibrarySession
    }
}