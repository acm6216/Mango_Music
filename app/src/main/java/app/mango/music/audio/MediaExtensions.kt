package app.mango.music.audio

import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import java.util.*

fun Cursor.getSongId(): Long {
    val index = this.getColumnIndex(MediaStore.Audio.Media._ID)
    return if (index < 0) return 0 else this.getLong(index)
}

fun Cursor.getSongTitle(): String {
    val index = this.getColumnIndex(MediaStore.Audio.Media.TITLE)
    return if (index < 0) "" else this.getString(index)
}

fun Cursor.getAlbumId(): Long {
    val index = this.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
    return if (index < 0) return 0 else this.getLong(index)
}

fun Cursor.getAlbumTitle(): String {
    val index = this.getColumnIndex(MediaStore.Audio.Media.ALBUM)
    return if (index < 0) "" else this.getString(index)
}

fun Cursor.getArtistId(): Long {
    val index = this.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID)
    return if (index < 0) 0 else this.getLong(index)
}

fun Cursor.getArtist(): String {
    val index = this.getColumnIndex(MediaStore.Audio.Media.ARTIST)
    return if (index < 0) "" else this.getString(index)
}

fun Cursor.getArtists(): List<String> {
    return this.getArtist().split("/")
}

fun Cursor.getSongSize(): Long {
    val index = this.getColumnIndex(MediaStore.Audio.Media.SIZE)
    return if (index < 0) return 0 else this.getLong(index)
}

fun Cursor.getSongData(): String {
    val index = this.getColumnIndex(MediaStore.Audio.Media.DATA)
    return if (index < 0) "" else this.getString(index)
}

fun Cursor.getDateAdded():String {
    val index = this.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
    return if(index<0) "" else this.getString(index)
}

fun Cursor.getSongDuration(): Long {
    val index = this.getColumnIndex(MediaStore.Audio.Media.DURATION)
    return if (index < 0) return 0 else this.getLong(index)
}

fun Cursor.getSongMimeType(): String {
    val index = this.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
    return if (index < 0) "" else this.getString(index)
}

@RequiresApi(Build.VERSION_CODES.R)
fun Cursor.getSongGenre(): String? {
    val index = this.getColumnIndex(MediaStore.Audio.AudioColumns.GENRE)
    return if (index == -1) null else this.getString(index)
}

fun Cursor.getAlbumArt(): Uri {
    return ContentUris.withAppendedId(
        Uri.parse("content://media/external/audio/albumart/"),
        getAlbumId()
    )
}

fun Cursor.getMediaUri(): Uri {
    return Uri.withAppendedPath(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        getSongId().toString()
    )
}

fun Bundle.setAlbumId(albumId: Long): Bundle {
    putLong(MediaStore.Audio.Media.ALBUM_ID, albumId)
    return this
}

fun Bundle.setArtistId(artistId: Long): Bundle {
    putLong(MediaStore.Audio.Media.ARTIST_ID, artistId)
    return this
}

fun Bundle.setDuration(duration: Long): Bundle {
    putLong(MediaStore.Audio.Media.DURATION, duration)
    return this
}

fun Bundle.setSongData(songData: String): Bundle {
    putString(MediaStore.Audio.Media.DATA, songData)
    return this
}

fun Bundle.setDateAdded(date:String):Bundle{
    putString(MediaStore.Audio.Media.DATE_ADDED,date)
    return this
}

fun MediaMetadata.getDuration(): Long {
    return this.extras?.getLong(MediaStore.Audio.Media.DURATION) ?: 0L
}

fun MediaMetadata.getArtistId(): Long {
    return this.extras?.getLong(MediaStore.Audio.Media.ARTIST_ID) ?: 0L
}

fun MediaMetadata.getAlbumId(): Long {
    return this.extras?.getLong(MediaStore.Audio.Media.ALBUM_ID) ?: 0L
}

fun MediaMetadata.getSongData(): String? {
    return this.extras?.getString(MediaStore.Audio.Media.DATA)
}

fun MediaMetadata.getDateAdded(): String? {
    return this.extras?.getString(MediaStore.Audio.Media.DATE_ADDED)
}

fun MediaItem.partCopy(): MediaItem {
    return MediaItem.Builder()
        .setUri(this.mediaMetadata.mediaUri)
        .setMediaMetadata(this.mediaMetadata)
        .setMimeType(this.localConfiguration?.mimeType)
        .setMediaId(this.mediaId)
        .build()
}

fun MediaItem.Builder.from(cursor: Cursor): MediaItem.Builder {
    setMediaId(cursor.getSongId().toString())
    setMimeType(cursor.getSongMimeType())
    setUri(cursor.getMediaUri())
    return this
}

fun MediaMetadata.Builder.from(cursor: Cursor): MediaMetadata.Builder {
    setArtist(cursor.getArtist())
    setAlbumTitle(cursor.getAlbumTitle())
    setTitle(cursor.getSongTitle())
    setMediaUri(cursor.getMediaUri())
    setAlbumArtist(cursor.getArtist())
    setArtworkUri(cursor.getAlbumArt())
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val genre = cursor.getSongGenre() ?: "Empty"
        setGenre(genre.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() })
    }
    setFolderType(MediaMetadata.FOLDER_TYPE_NONE)
    setIsPlayable(true)
    setExtras(
        Bundle()
            .setAlbumId(cursor.getAlbumId())
            .setArtistId(cursor.getArtistId())
            .setDuration(cursor.getSongDuration())
            .setSongData(cursor.getSongData())
    )
    return this
}