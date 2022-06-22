package app.mango.music.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import app.mango.music.audio.*
import app.mango.music.data.*
import app.mango.music.manager.SpManager
import app.mango.music.utils.WhileViewSubscribed
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MusicViewModel @Inject constructor(
    mediaSource: BaseMediaSource,
    queryFavoriteUseCase: QueryFavoriteUseCase,
    private val saveFavoriteUseCase: SaveFavoriteUseCase
) :ViewModel() {

    companion object{
        private const val TAG = "MusicViewModel"
        private const val SORT_DEF = "add date"
    }

    val whenReady = MutableStateFlow(ReadyWhenState(System.currentTimeMillis(),false))

    private val sortBy = MutableStateFlow(SORT_DEF)

    init {
        SpManager.listen("key_set_list_filter_sort",
            SpManager.SpStringListener(SORT_DEF){
                sortBy.value = it
            }
        )
    }

    private fun List<MediaItem>?.sort(sortBy:String):List<MediaItem> = when {
        this==null -> emptyList()
        sortBy == SORT_DEF -> sortedBy { it.mediaMetadata.getDateAdded() }
        else -> sortedWith { o1, o2 -> Collator.getInstance(Locale.CHINESE).compare(o1.mediaMetadata.title, o2.mediaMetadata.title) }
    }

    private val songs = combine(sortBy,whenReady) { sort,ready ->
        if(ready.isReady) mediaSource.getChildren(ALL_ID).sort(sort)
        else emptyList()
    }.stateIn(viewModelScope, WhileViewSubscribed, emptyList())

    private val favorites = queryFavoriteUseCase()

    private fun List<MusicFavorite>.contains(id:String):Boolean{
        forEach {
            if(it.mediaId == id&&it.isFavorite)
                return true
        }
        return false
    }

    val favoriteSongs = combine(songs,favorites) { list,fas ->
        list.map { FavoriteMusic(fas.contains(it.mediaId),it) }
    }.stateIn(viewModelScope, WhileViewSubscribed, emptyList())

    val playlist = combine(GlobalData.currentPlaylist,favorites){ list,fas ->
        list.map { FavoriteMusic(fas.contains(it.mediaId),it) }
    }.stateIn(viewModelScope, WhileViewSubscribed, emptyList())

    val favorited = favoriteSongs.transform { list ->
        emit(list.filter { it.isFavorite })
    }

    val mediaFavoriteId = MutableStateFlow("")
    val mediaFavorite = combine(favorited,mediaFavoriteId) { fa,id->
        val faState = fa.find { it.mediaItem().mediaId==id }
        MusicFavorite(id,faState != null)
    }

    val searchKey = MutableStateFlow("")
    val searchSongs = combine(searchKey,favoriteSongs){ k,fas ->
        val key = k.lowercase()
        fas.filter {
            it.metadata().title.toString().lowercase().contains(key)||
                    it.metadata().artist.toString().lowercase().contains(key)||
                    it.metadata().albumTitle.toString().lowercase().contains(key)
        }
    }

    val albums = favorites.transform {
        val data = mediaSource.getChildren(ALBUM_ID)?: emptyList()
        emit(
            data.map {
                Album(it.mediaMetadata.getAlbumId().toString(),
                    it.mediaMetadata.albumTitle.toString(),
                    it
                )
            }
        )
    }
    val albumArtistId = MutableStateFlow("")
    private val albumSongs = albumArtistId.transform { id ->
        emit(mediaSource.getChildren(ALBUM_PREFIX +id)?: emptyList())
    }
    val albumDetail = combine(albumSongs,favorites) { list,fas ->
        list.map { FavoriteMusic(fas.contains(it.mediaId),it) }
    }.stateIn(viewModelScope, WhileViewSubscribed, emptyList())

    val artists = favoriteSongs.transform {
        val data = mediaSource.getChildren(ARTIST_ID)?: emptyList()
        emit(
            data.map {
                Artist(it.mediaMetadata.getArtistId().toString(),
                    it.mediaMetadata.albumTitle.toString(),
                    it
                )
            }
        )
    }
    private val artistSongs = albumArtistId.transform { id->
        emit(mediaSource.getChildren(ARTIST_PREFIX +id)?: emptyList())
    }
    val artistDetail = combine(artistSongs,favorites) { list,fas ->
        list.map { FavoriteMusic(fas.contains(it.mediaId),it) }
    }.stateIn(viewModelScope, WhileViewSubscribed, emptyList())

    fun updateFavoriteState(musicFavorite: MusicFavorite){
        viewModelScope.launch {
            saveFavoriteUseCase(musicFavorite)
        }
    }

}