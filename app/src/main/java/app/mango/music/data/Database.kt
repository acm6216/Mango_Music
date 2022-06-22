package app.mango.music.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "m_favorite")
data class MusicFavorite(
    @PrimaryKey
    @ColumnInfo(name = "media_id")
    val mediaId:String,

    @ColumnInfo(name = "is_favorite")
    val isFavorite:Boolean
)

@Dao
interface MusicDao{
    @Query("SELECT * FROM m_favorite")
    fun getFavorites(): Flow<List<MusicFavorite>>

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(musicFavorite: MusicFavorite)

    @Transaction
    @Delete(entity = MusicFavorite::class)
    fun delete(musicFavorite: MusicFavorite)
}

@Database(
    entities = [MusicFavorite::class],
    version = 1,
    exportSchema = false
)
abstract class MusicDatabase:RoomDatabase(){
    abstract fun musicDao():MusicDao
}