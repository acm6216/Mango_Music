package app.mango.music.ui.nav

import android.view.View
import android.widget.ImageView
import androidx.annotation.FloatRange
import androidx.core.view.marginTop
import androidx.core.view.updatePadding
import app.mango.music.R
import app.mango.music.utils.normalize
import com.google.android.material.shape.MaterialShapeDrawable

interface OnSlideAction {
    fun onSlide(
        sheet: View,
        @FloatRange(
            from = -1.0,
            fromInclusive = true,
            to = 1.0,
            toInclusive = true
        ) slideOffset: Float
    )
}

class HalfClockwiseRotateSlideAction(
    private val view: View
) : OnSlideAction {

    override fun onSlide(sheet: View, slideOffset: Float) {
        view.rotation = slideOffset.normalize(
            -1F,
            0F,
            0F,
            180F
        )
    }
}

class ForegroundSheetTransformSlideAction(
    private val foregroundView: View,
    private val foregroundShapeDrawable: MaterialShapeDrawable,
    private val profileImageView: ImageView
) : OnSlideAction {

    private val foregroundMarginTop = foregroundView.marginTop
    private var systemTopInset: Int = 0
    private val foregroundZ = foregroundView.z
    private val profileImageOriginalZ = profileImageView.z

    private fun getPaddingTop(): Int {
        if (systemTopInset == 0) {
            systemTopInset = foregroundView.getTag(R.id.tag_system_window_inset_top) as? Int? ?: 0
        }
        return systemTopInset
    }

    override fun onSlide(sheet: View, slideOffset: Float) {
        val progress = slideOffset.normalize(0F, 0.25F, 1F, 0F)
        profileImageView.scaleX = progress
        profileImageView.scaleY = progress
        foregroundShapeDrawable.interpolation = progress

        foregroundView.translationY = -(1 - progress) * foregroundMarginTop
        val topPaddingProgress = slideOffset.normalize(0F, 0.9F, 0F, 1F)
        foregroundView.updatePadding(top = (getPaddingTop() * topPaddingProgress).toInt())

        if (slideOffset > 0 && foregroundZ <= profileImageView.z) {
            //profileImageView.z = profileImageOriginalZ
        } else if (slideOffset <= 0 && foregroundZ >= profileImageView.z) {
            //profileImageView.z = foregroundZ + 1
        }
    }
}

class AlphaSlideAction(
    private val view: View,
    private val reverse: Boolean = false
) : OnSlideAction {

    override fun onSlide(sheet: View, slideOffset: Float) {
        val alpha = slideOffset.normalize(-1F, 0F, 0F, 1F)
        view.alpha = if (!reverse) alpha else 1F - alpha
    }
}
