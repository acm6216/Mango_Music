package app.mango.music.ui.settings

import android.os.Bundle
import app.mango.music.R
import app.mango.music.databinding.FragmentSettingsBinding
import app.mango.music.ui.BaseFragment
import com.google.android.material.transition.MaterialSharedAxis

class SettingsFragment:BaseFragment<FragmentSettingsBinding>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply {
            duration = resources.getInteger(R.integer.motion_duration_large).toLong()
        }
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).apply {
            duration = resources.getInteger(R.integer.motion_duration_large).toLong()
        }
    }

    override fun setBinding(): FragmentSettingsBinding = FragmentSettingsBinding.inflate(layoutInflater)
}