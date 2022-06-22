package app.mango.music.audio

import androidx.media3.common.MediaItem

data class Album(val albumId:String,val albumTitle:String,val mediaItem: MediaItem)
data class Artist(val artistId:String,val artistName:String,val mediaItem: MediaItem)
data class Detail(val mediaItem: MediaItem,val count:Int,val duration:Long)
data class FavoriteMusic(var isFavorite:Boolean,private val mediaItem: MediaItem){
    fun metadata() = mediaItem.mediaMetadata
    fun mediaItem() = mediaItem
}
open class PlayValue(val value:Int)
data class PlayPosition(private val position:Int):PlayValue(position)
data class PlayDuration(private val duration:Int):PlayValue(duration)
data class ReadyWhenState(val time:Long,val isReady:Boolean)