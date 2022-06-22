package app.mango.music.fetcher

import androidx.media3.common.MediaItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricsFetcher  @Inject constructor(
    private val lyricSourceFactory: LyricsSourceFactory
) {

    suspend fun fetch(
        data: MediaItem
    ): Pair<String, String?> = lyricSourceFactory.getLyrics(data)

}