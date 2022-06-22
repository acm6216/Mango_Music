package app.mango.music.data

import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MediaMetadata.FOLDER_TYPE_PLAYLISTS
import app.mango.music.audio.from
import app.mango.music.audio.getAlbumId
import app.mango.music.audio.getArtistId
import app.mango.music.manager.Config
import app.mango.music.manager.SpManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

const val unknownArtist = "<unknown>"
const val minDurationLimit = 30 * 1000

@Singleton
class BaseMediaSource @Inject constructor(
    @ApplicationContext private val mContext: Context
) : CoroutineScope, AbstractMediaSource() {

    override val coroutineContext: CoroutineContext = Dispatchers.IO
    private val targetUri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    private val minSizeLimit = 0
    private var artistFilter = unknownArtist
    private var minDurationFilter = minDurationLimit
    private var contentResolverState = false

    private val baseProjection = ArrayList(
        listOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.MIME_TYPE
        )
    ).also {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            it.add(MediaStore.Audio.Media.GENRE)
        }
    }.toTypedArray()

    init {
        mContext.contentResolver
            .registerContentObserver(targetUri, true, MediaSourceObserver())

        SpManager.listen(
            Config.KEY_SETTINGS_MEDIA_UNKNOWN_FILTER,
            SpManager.SpBoolListener(Config.DEFAULT_SETTINGS_MEDIA_UNKNOWN_FILTER) {
                artistFilter = if (it) unknownArtist else ""
                if(contentResolverState) loadSync()
            })
    }

    inner class MediaSourceObserver : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            launch(Dispatchers.IO) {
                initialize(loadMediaItems())
            }
        }
    }

    override fun getAlbumIdFromMediaItem(mediaItem: MediaItem): String {
        return mediaItem.mediaMetadata.getAlbumId().toString()
    }

    override fun getArtistIdFromMediaItem(mediaItem: MediaItem): String {
        return mediaItem.mediaMetadata.getArtistId().toString()
    }

    override fun songItemToAlbumItem(mediaItem: MediaItem): MediaItem {
        return mediaItem.buildUpon()
            .setMediaMetadata(
                mediaItem.mediaMetadata.buildUpon()
                    .setIsPlayable(false)
                    .setFolderType(FOLDER_TYPE_PLAYLISTS)
                    .build()
            ).build()
    }

    override fun songItemToArtistItem(mediaItem: MediaItem): MediaItem {
        return mediaItem.buildUpon()
            .setMediaMetadata(
                mediaItem.mediaMetadata.buildUpon()
                    .setIsPlayable(false)
                    .setFolderType(FOLDER_TYPE_PLAYLISTS)
                    .build()
            ).build()
    }

    override fun songItemToGenreItem(mediaItem: MediaItem): MediaItem {
        return mediaItem.buildUpon()
            .setMediaId(mediaItem.mediaMetadata.genre.toString())
            .setMediaMetadata(
                mediaItem.mediaMetadata.buildUpon()
                    .setIsPlayable(false)
                    .setFolderType(FOLDER_TYPE_PLAYLISTS)
                    .build()
            ).build()
    }

    fun loadSync() = launch {
        contentResolverState = true
        load()
    }

    override suspend fun load() {
        try {
            initialize(loadMediaItems())
        } catch (e: Exception) {
            readyState = STATE_ERROR
        }
    }

    private suspend fun loadMediaItems(): MutableList<MediaItem> =
        withContext(Dispatchers.IO) {
            val cursor = searchForMedia(
                projection = baseProjection,
                selection = "${MediaStore.Audio.Media.SIZE} >= ? " +
                        "and ${MediaStore.Audio.Media.DURATION} >= ? " +
                        "and ${MediaStore.Audio.Artists.ARTIST} != ?",
                selectionArgs = arrayOf("$minSizeLimit", "$minDurationFilter", artistFilter),
                sortOrder = "${MediaStore.Audio.Media._ID} DESC"
            ) ?: return@withContext ArrayList()

            return@withContext ArrayList<MediaItem>().apply {
                while (cursor.moveToNext()) {
                    val mediaItem = MediaItem.Builder()
                    .from(cursor)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .from(cursor)
                            .build()
                    ).build()
                    add(mediaItem)
                }
            }
        }

    private fun searchForMedia(
        selection: String? = null,
        projection: Array<String>? = null,
        selectionArgs: Array<String>? = null,
        sortOrder: String? = null
    ): Cursor? {
        return mContext.contentResolver.query(
            targetUri,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )
    }
}