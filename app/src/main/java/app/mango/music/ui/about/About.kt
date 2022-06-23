package app.mango.music.ui.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import app.mango.music.R
import app.mango.music.ui.settings.BasePreferenceFragment

class About: BasePreferenceFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.about_preferences,rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        findPreference<Preference>(R.string.key_about_version)?.summary = getVersionName()
        findPreference<Preference>(R.string.key_about_developer_github)?.setOnPreferenceClickListener {
            it.summary.toString().openUrl()
            true
        }
        findPreference<Preference>(R.string.key_about_github)?.setOnPreferenceClickListener {
            it.summary.toString().openUrl()
            true
        }
    }

    private fun String.openUrl(){
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(this)))
    }

    private fun getVersionName():String{
        val pm = requireContext().packageManager
        val pi = pm?.getPackageInfo(requireContext().packageName, 0)
        return pi?.versionName?:"1.0"
    }
}