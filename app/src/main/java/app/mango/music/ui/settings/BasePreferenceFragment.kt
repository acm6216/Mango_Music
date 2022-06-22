package app.mango.music.ui.settings

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.preference.*
import androidx.recyclerview.widget.RecyclerView

abstract class BasePreferenceFragment : PreferenceFragmentCompat() {

    private fun setAllPreferencesToAvoidHavingExtraSpace(preference: Preference) {
        preference.isIconSpaceReserved = false
        if (preference is PreferenceGroup)
            preference.forEach {
                setAllPreferencesToAvoidHavingExtraSpace(it)
            }
    }

    protected fun <T:Preference> findPreference(strId:Int) = findPreference<T>(getString(strId))

    override fun setPreferenceScreen(preferenceScreen: PreferenceScreen) {
        setAllPreferencesToAvoidHavingExtraSpace(preferenceScreen)
        super.setPreferenceScreen(preferenceScreen)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this::class.java.superclass.superclass.getDeclaredField("mList").run {
            isAccessible = true
            (get(this@BasePreferenceFragment) as RecyclerView)
                .overScrollMode = View.OVER_SCROLL_NEVER
        }
    }

    companion object{
        private const val TAG = "BasePreferenceFragment"
    }

}