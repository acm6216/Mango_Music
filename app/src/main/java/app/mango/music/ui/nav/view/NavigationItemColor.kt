package app.mango.music.ui.nav.view

import android.content.Context
import android.content.res.ColorStateList
import app.mango.music.utils.themeColor

object NavigationItemColor {
    private val CHECKED_STATE_SET = intArrayOf(android.R.attr.state_checked)
    private val DISABLED_STATE_SET = intArrayOf(-android.R.attr.state_enabled)
    private val EMPTY_STATE_SET = intArrayOf()

    fun create(color: ColorStateList, context: Context): ColorStateList {
        val checkedColor = context.themeColor(android.R.attr.colorPrimary)
        val defaultColor = color.defaultColor
        val disabledColor = color.getColorForState(DISABLED_STATE_SET, defaultColor)
        return ColorStateList(
            arrayOf(DISABLED_STATE_SET, CHECKED_STATE_SET, EMPTY_STATE_SET),
            intArrayOf(disabledColor, checkedColor, defaultColor)
        )
    }

}
