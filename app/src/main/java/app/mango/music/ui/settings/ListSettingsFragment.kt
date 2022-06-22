package app.mango.music.ui.settings

import app.mango.music.databinding.MenuBottomSheetDialogLayoutBinding

class ListSettingsFragment: BaseBottomSheetDialog<MenuBottomSheetDialogLayoutBinding>() {

    override fun setLayout(): MenuBottomSheetDialogLayoutBinding = MenuBottomSheetDialogLayoutBinding.inflate(layoutInflater)
}