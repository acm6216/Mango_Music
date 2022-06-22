package app.mango.music.fetcher

import android.text.TextUtils
import androidx.media3.common.MediaItem
import app.mango.music.audio.getSongData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricSourceFactory @Inject constructor(
    embeddedLyricSource: EmbeddedLyricSource,
    localLyricSource: LocalLyricSource
) {
    interface LyricSource {
        suspend fun loadLyric(mediaItem: MediaItem): Pair<String, String?>?
    }

    private val sources: MutableList<LyricSource> = ArrayList()

    init {
        sources.add(embeddedLyricSource)
        sources.add(localLyricSource)
    }

    suspend fun getLyric(
        mediaItem: MediaItem,
        callback: (String?, String?) -> Unit = { _, _ -> }
    ): Pair<String, String?>? = withContext(Dispatchers.IO) {
        sources.forEach { source ->
            val pair = source.loadLyric(mediaItem)
            if (pair != null) {
                callback(pair.first, pair.second)
                return@withContext pair
            }
        }
        callback(null, null)
        return@withContext null
    }
}

@Singleton
class LyricsSourceFactory @Inject constructor(
    private val embeddedLyricsSource: EmbeddedLyricsSource
) {
    interface LyricsSource {
        suspend fun loadLyrics(mediaItem: MediaItem): Pair<String, String?>?
    }

    suspend fun getLyrics(
        mediaItem: MediaItem,
        callback: (String, String?) -> Unit = { _, _ -> }
    ): Pair<String, String?> = withContext(Dispatchers.IO) {

        val pair = embeddedLyricsSource.loadLyrics(mediaItem)
        if (pair != null) {
            callback(pair.first, pair.second)
            return@withContext pair
        }
        callback("", null)
        return@withContext Pair("", null)
    }
}

class EmbeddedLyricSource @Inject constructor() : LyricSourceFactory.LyricSource {
    override suspend fun loadLyric(mediaItem: MediaItem): Pair<String, String?>? =
        withContext(Dispatchers.IO) {
            val songData = mediaItem.mediaMetadata.getSongData() ?: return@withContext null
            val file = File(songData)
            if (!file.exists()) return@withContext null
            kotlin.runCatching {
                //Logger.getLogger("org.jaudiotagger").level = Level.OFF
                val tag = AudioFileIO.read(file).tag
                val lyric = tag.getFields(FieldKey.LYRICS)
                    .run { if (isNotEmpty()) get(0).toString() else null }
                    ?: return@withContext null
                return@withContext if (TextUtils.isEmpty(lyric)) null
                else Pair(lyric, null)
            }
            null
        }
}

class EmbeddedLyricsSource @Inject constructor() : LyricsSourceFactory.LyricsSource {
    override suspend fun loadLyrics(mediaItem: MediaItem): Pair<String, String?>? =
        withContext(Dispatchers.IO) {
            val songData = mediaItem.mediaMetadata.getSongData() ?: return@withContext null
            val file = File(songData)
            if (!file.exists()) return@withContext null
            kotlin.runCatching {
                val tag = AudioFileIO.read(file).tag
                val lyric = tag.getFirst(FieldKey.LYRICS)
                    .run { ifEmpty { null } }
                    ?: return@withContext null
                return@withContext if (TextUtils.isEmpty(lyric)) null
                else Pair(lyric, null)
            }
            null
        }
}

class LocalLyricSource @Inject constructor() : LyricSourceFactory.LyricSource {
    override suspend fun loadLyric(mediaItem: MediaItem): Pair<String, String?>? =
        withContext(Dispatchers.IO) {
            val songData = mediaItem.mediaMetadata.getSongData() ?: return@withContext null
            val path = songData.substring(0, songData.lastIndexOf('.')) + ".lrc"
            val lrcFile = File(path)

            if (!lrcFile.exists()) return@withContext null

            val lyric = lrcFile.readText()
            return@withContext if (TextUtils.isEmpty(lyric)) null
            else Pair(lyric, null)
        }
}