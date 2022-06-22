package app.mango.music.viewmodel

import app.mango.music.data.MusicDao
import javax.inject.Inject

class QueryFavoriteUseCase @Inject constructor(
    private val dao: MusicDao
) {
    operator fun invoke() = dao.getFavorites()
}