package app.mango.music.ui.settings

import android.os.Bundle
import app.mango.music.R

class ListSettings:BasePreferenceFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.list_preference, rootKey)
    }
}