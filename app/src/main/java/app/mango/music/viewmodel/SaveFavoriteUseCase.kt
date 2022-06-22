package app.mango.music.viewmodel

import app.mango.music.data.MusicDao
import app.mango.music.data.MusicFavorite
import javax.inject.Inject

class SaveFavoriteUseCase @Inject constructor(
    private val dao: MusicDao
) {
    suspend operator fun invoke(musicFavorite: MusicFavorite) = dao.save(musicFavorite)
}