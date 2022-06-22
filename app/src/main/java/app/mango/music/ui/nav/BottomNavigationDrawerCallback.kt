package app.mango.music.ui.nav

import android.annotation.SuppressLint
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import app.mango.music.utils.normalize
import com.google.android.material.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlin.math.max

class BottomNavigationDrawerCallback : BottomSheetBehavior.BottomSheetCallback() {

    private val onSlideActions: MutableList<OnSlideAction> = mutableListOf()
    private val onStateChangedActions: MutableList<OnStateChangedAction> = mutableListOf()

    private var lastSlideOffset = -1.0F
    private var halfExpandedSlideOffset = Float.MAX_VALUE

    override fun onSlide(sheet: View, slideOffset: Float) {
        if (halfExpandedSlideOffset == Float.MAX_VALUE)
            calculateInitialHalfExpandedSlideOffset(sheet)

        lastSlideOffset = slideOffset
        val trueOffset = if (slideOffset <= halfExpandedSlideOffset) {
            slideOffset.normalize(-1F, halfExpandedSlideOffset, -1F, 0F)
        } else {
            slideOffset.normalize(halfExpandedSlideOffset, 1F, 0F, 1F)
        }

        onSlideActions.forEach { it.onSlide(sheet, trueOffset) }
    }

    override fun onStateChanged(sheet: View, newState: Int) {
        if (newState == BottomSheetBehavior.STATE_HALF_EXPANDED) {
            halfExpandedSlideOffset = lastSlideOffset
            onSlide(sheet, lastSlideOffset)
        }

        onStateChangedActions.forEach { it.onStateChanged(sheet, newState) }
    }

    @SuppressLint("PrivateResource")
    private fun calculateInitialHalfExpandedSlideOffset(sheet: View) {
        val parent = sheet.parent as CoordinatorLayout
        val behavior = BottomSheetBehavior.from(sheet)

        val halfExpandedOffset = parent.height * (1 - behavior.halfExpandedRatio)
        val peekHeightMin = parent.resources.getDimensionPixelSize(
            R.dimen.design_bottom_sheet_peek_height_min
        )
        val peek = max(peekHeightMin, parent.height - parent.width * 9 / 16)
        val collapsedOffset = max(
            parent.height - peek,
            max(0, parent.height - sheet.height)
        )
        halfExpandedSlideOffset =
            (collapsedOffset - halfExpandedOffset) / (parent.height - collapsedOffset)
    }

    fun addOnSlideAction(action: OnSlideAction): Boolean {
        return onSlideActions.add(action)
    }

    fun removeOnSlideAction(action: OnSlideAction): Boolean {
        return onSlideActions.remove(action)
    }

    fun addOnStateChangedAction(action: OnStateChangedAction): Boolean {
        return onStateChangedActions.add(action)
    }

    fun removeOnStateChangedAction(action: OnStateChangedAction): Boolean {
        return onStateChangedActions.remove(action)
    }
}