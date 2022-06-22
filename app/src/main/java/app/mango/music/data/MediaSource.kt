package app.mango.music.data

import android.net.Uri
import androidx.annotation.IntDef
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MediaMetadata.FOLDER_TYPE_PLAYLISTS
import com.google.common.collect.ImmutableList
import kotlin.collections.LinkedHashSet

const val STATE_CREATED = 1
const val STATE_INITIALIZING = 2
const val STATE_INITIALIZED = 3
const val STATE_ERROR = 4

@IntDef(
    STATE_CREATED,
    STATE_INITIALIZING,
    STATE_INITIALIZED,
    STATE_ERROR
)
@Retention(AnnotationRetention.SOURCE)
annotation class ReadyState

interface MediaSource {
    suspend fun load()
    fun whenReady(performAction: (Boolean) -> Unit): Boolean
}

const val ROOT_ID = "[rootID]"
const val ALBUM_ID = "[albumID]"
const val GENRE_ID = "[genreID]"
const val ARTIST_ID = "[artistID]"
const val ALL_ID = "[allID]"

const val ALBUM_PREFIX = "[album]"
const val GENRE_PREFIX = "[genre]"
const val ARTIST_PREFIX = "[artist]"
const val ITEM_PREFIX = "[item]"
const val ALL_PREFIX = "[all]"

abstract class AbstractMediaSource : MediaSource {
    private var treeNodes: MutableMap<String, MediaItemNode> = mutableMapOf()

    private val readyListener = mutableListOf<(Boolean) -> Unit>()
    private fun buildMediaItem(
        title: String,
        mediaId: String,
        isPlayable: Boolean,
        @MediaMetadata.FolderType folderType: Int,
        album: String? = null,
        artist: String? = null,
        genre: String? = null,
        sourceUri: Uri? = null,
        imageUri: Uri? = null,
    ): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setAlbumTitle(album)
            .setTitle(title)
            .setArtist(artist)
            .setGenre(genre)
            .setFolderType(folderType)
            .setIsPlayable(isPlayable)
            .setArtworkUri(imageUri)
            .build()
        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(metadata)
            .setUri(sourceUri)
            .build()
    }

    abstract fun getAlbumIdFromMediaItem(mediaItem: MediaItem): String
    abstract fun getArtistIdFromMediaItem(mediaItem: MediaItem): String
    abstract fun songItemToAlbumItem(mediaItem: MediaItem): MediaItem
    abstract fun songItemToArtistItem(mediaItem: MediaItem): MediaItem
    abstract fun songItemToGenreItem(mediaItem: MediaItem): MediaItem

    fun getRootItem(): MediaItem {
        return treeNodes[ROOT_ID]!!.item
    }

    fun getItemById(id: String): MediaItem? {
        return treeNodes[id]?.item
    }

    fun getChildren(parentId: String): List<MediaItem>? {
        return treeNodes[parentId]?.getChildren()
    }

    inner class MediaItemNode(val item: MediaItem) {
        private val children: LinkedHashSet<MediaItem> = LinkedHashSet()

        fun addChild(childID: String) {
            treeNodes[childID]?.let {
                children.add(it.item)
            }
        }

        fun getChildren(): List<MediaItem> {
            return ImmutableList.copyOf(children)
        }
    }

    @ReadyState
    var readyState: Int = STATE_CREATED
        set(value) {
            when (value) {
                STATE_INITIALIZED,
                STATE_ERROR -> synchronized(readyListener) {
                    field = value
                    readyListener.forEach { it.invoke(value != STATE_ERROR) }
                }
                else -> field = value
            }
        }

    override fun whenReady(performAction: (Boolean) -> Unit): Boolean {
        return when (readyState) {
            STATE_CREATED, STATE_INITIALIZING -> {
                readyListener += performAction
                false
            }
            else -> {
                performAction.invoke(readyState != STATE_ERROR)
                true
            }
        }
    }

    companion object{
        private const val TAG = "MediaSource"
    }

    private fun initialize() {
        treeNodes.clear()
        treeNodes[ROOT_ID] = MediaItemNode(
            buildMediaItem(
                title = "Root Folder",
                mediaId = ROOT_ID,
                isPlayable = false,
                folderType = MediaMetadata.FOLDER_TYPE_MIXED
            )
        )
        treeNodes[ALBUM_ID] = MediaItemNode(
            buildMediaItem(
                title = "Album Folder",
                mediaId = ALBUM_ID,
                isPlayable = false,
                folderType = MediaMetadata.FOLDER_TYPE_MIXED
            )
        )
        treeNodes[ARTIST_ID] = MediaItemNode(
            buildMediaItem(
                title = "Artist Folder",
                mediaId = ARTIST_ID,
                isPlayable = false,
                folderType = MediaMetadata.FOLDER_TYPE_MIXED
            )
        )
        treeNodes[GENRE_ID] =
            MediaItemNode(
                buildMediaItem(
                    title = "Genre Folder",
                    mediaId = GENRE_ID,
                    isPlayable = false,
                    folderType = MediaMetadata.FOLDER_TYPE_MIXED
                )
            )
        treeNodes[ALL_ID] =
            MediaItemNode(
                buildMediaItem(
                    title = "All Items Folder",
                    mediaId = ALL_ID,
                    isPlayable = false,
                    folderType = FOLDER_TYPE_PLAYLISTS
                )
            )
        treeNodes[ROOT_ID]!!.addChild(ALBUM_ID)
        treeNodes[ROOT_ID]!!.addChild(ARTIST_ID)
        treeNodes[ROOT_ID]!!.addChild(GENRE_ID)
        treeNodes[ROOT_ID]!!.addChild(ALL_ID)
    }

    @Throws(Exception::class)
    fun initialize(data: List<MediaItem>) {
        readyState = STATE_INITIALIZING
        initialize()
        data.forEach {
            val idInTree = ITEM_PREFIX + it.mediaId
            val albumFolderIdInTree = ALBUM_PREFIX + getAlbumIdFromMediaItem(it)
            val artistFolderIdInTree = ARTIST_PREFIX + getArtistIdFromMediaItem(it)
            val genreFolderIdInTree = GENRE_PREFIX + it.mediaMetadata.genre

            treeNodes[idInTree] = MediaItemNode(it)

            if (!treeNodes.containsKey(albumFolderIdInTree)) {
                treeNodes[albumFolderIdInTree] = MediaItemNode(songItemToAlbumItem(it))
                treeNodes[ALBUM_ID]?.addChild(albumFolderIdInTree)
            }
            if (!treeNodes.containsKey(artistFolderIdInTree)) {
                treeNodes[artistFolderIdInTree] = MediaItemNode(songItemToArtistItem(it))
                treeNodes[ARTIST_ID]?.addChild(artistFolderIdInTree)
            }
            if (!treeNodes.containsKey(genreFolderIdInTree)) {
                treeNodes[genreFolderIdInTree] = MediaItemNode(songItemToGenreItem(it))
                treeNodes[GENRE_ID]?.addChild(genreFolderIdInTree)
            }

            treeNodes[albumFolderIdInTree]?.addChild(idInTree)
            treeNodes[artistFolderIdInTree]?.addChild(idInTree)
            treeNodes[genreFolderIdInTree]?.addChild(idInTree)
            treeNodes[ALL_ID]!!.addChild(idInTree)
        }
        readyState = STATE_INITIALIZED
    }
}
