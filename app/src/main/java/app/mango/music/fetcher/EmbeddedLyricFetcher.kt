package app.mango.music.fetcher

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import androidx.media3.common.MediaItem
import app.mango.music.R
import coil.bitmap.BitmapPool
import coil.decode.DataSource
import coil.decode.Options
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.size.Size
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 根据
 * https://gitee.com/lalilu/lmusic/
 * 修改
 */
@Singleton
@SuppressLint("UseCompatLoadingForDrawables")
class EmbeddedLyricFetchers @Inject constructor(
    @ApplicationContext private val mContext: Context,
    private val lyricSourceFactory: LyricSourceFactory
) : Fetcher<MediaItem> {
    private val lrcIcon = mContext.resources.getDrawable(R.drawable.icon, mContext.theme)

    override suspend fun fetch(
        pool: BitmapPool,
        data: MediaItem,
        size: Size,
        options: Options
    ): FetchResult {
        val pair = lyricSourceFactory.getLyric(data)
        if (TextUtils.isEmpty(pair?.first)) throw NullPointerException()

        return DrawableResult(
            drawable = lrcIcon,
            isSampled = false,
            dataSource = DataSource.DISK
        )
    }

    override fun key(data: MediaItem): String {
        return "embedded_lyric_${data.mediaId}"
    }
}
