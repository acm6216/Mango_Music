package app.mango.music.ui.settings

import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.WindowInsetsControllerCompat
import androidx.databinding.ViewDataBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

abstract class BaseBottomSheetDialog<T:ViewDataBinding>: BottomSheetDialogFragment() {

    abstract fun setLayout():T
    protected lateinit var binding:T

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = setLayout().also {
        binding = it
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.run {
            window?.setDimAmount(0f)
            val bottomSheet = this::class.java.getDeclaredField("bottomSheet").apply {
                isAccessible = true
            }.get(this) as FrameLayout
            val behavior = this::class.java.getDeclaredField("behavior").apply {
                isAccessible = true
            }.get(this) as BottomSheetBehavior<FrameLayout>
            behavior.skipCollapsed = true
            setOnShowListener {
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                bottomSheet.background = ColorDrawable(Color.TRANSPARENT)
                WindowInsetsControllerCompat(window!!,window!!.decorView).isAppearanceLightStatusBars = !isNight()
            }
        }
    }

    private fun isNight():Boolean{
        val currentNightMode = requireContext().resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }
}