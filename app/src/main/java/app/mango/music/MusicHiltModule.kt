package app.mango.music

import android.content.Context
import androidx.room.Room
import app.mango.music.data.MusicDao
import app.mango.music.data.MusicDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Singleton

@Module
@ExperimentalCoroutinesApi
@InstallIn(SingletonComponent::class)
object MusicHiltModule {
    @Provides
    @Singleton
    fun provideLMusicDatabase(@ApplicationContext context: Context): MusicDatabase {
        return Room.databaseBuilder(
            context,
            MusicDatabase::class.java,
            "music_database.db"
        ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideTaskDao(appDatabase: MusicDatabase): MusicDao {
        return appDatabase.musicDao()
    }
}