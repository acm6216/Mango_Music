package app.mango.music

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import app.mango.music.fetcher.EmbeddedCoverFetcher
import app.mango.music.fetcher.EmbeddedLyricFetchers
import app.mango.music.fetcher.LyricsFetcher
import app.mango.music.manager.SpManager
import app.mango.music.utils.DimensionExtensions
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App:Application(), ImageLoaderFactory {

    @Inject
    lateinit var lyricFetchers: EmbeddedLyricFetchers

    @Inject
    lateinit var lyricsFetcher: LyricsFetcher

    @Inject
    lateinit var coverFetcher: EmbeddedCoverFetcher

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .componentRegistry {
                add(coverFetcher)
                add(lyricFetchers)
            }.build()

    override fun onCreate() {
        super.onCreate()
        SpManager.init(this)
        DimensionExtensions.init(this)
        DynamicColors.applyToActivitiesIfAvailable(this)
        setupDarkModePreference()
    }

    private fun setupDarkModePreference() {
        val defaultValue = resources.getString(R.string.set_theme_dark_mode_def)
        val disabledValue = resources.getStringArray(R.array.set_theme_dark_mode_value)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        val darkModeSetting =
            sharedPreferences.getString(resources.getString(R.string.key_set_theme_dark_mode), defaultValue)

        if (!darkModeSetting.equals(defaultValue)) {
            if (darkModeSetting.equals(disabledValue[1])) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }
    }

}