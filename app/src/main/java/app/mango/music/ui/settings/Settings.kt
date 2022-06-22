package app.mango.music.ui.settings

import android.media.audiofx.Equalizer
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.media3.exoplayer.ExoPlayer
import androidx.preference.*
import app.mango.music.R
import app.mango.music.manager.Config
import app.mango.music.manager.SpManager

class Settings : BasePreferenceFragment() {


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        findPreference<ListPreference>(R.string.key_set_theme_dark_mode)?.setOnPreferenceChangeListener { _, newValue ->
            refreshDarkModePreference(newValue.toString())
            true
        }
    }

    private fun refreshDarkModePreference(newValue: String) {
        val defaultValue = resources.getString(R.string.set_theme_dark_mode_def)
        val disabledValue = resources.getStringArray(R.array.set_theme_dark_mode_value)
        if (newValue != defaultValue) {
            if (newValue == disabledValue[1]) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }
}