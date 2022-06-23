package app.mango.music.ui.about

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import app.mango.music.R
import app.mango.music.databinding.FragmentAboutBinding
import app.mango.music.ui.BaseFragment
import app.mango.music.utils.DimensionExtensions.dpToPx
import com.google.android.material.transition.MaterialFade

class AboutFragment: BaseFragment<FragmentAboutBinding>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFade().apply {
            duration = resources.getInteger(R.integer.motion_duration_medium).toLong()
        }
        returnTransition = MaterialFade().apply {
            duration = resources.getInteger(R.integer.motion_duration_medium).toLong()
        }
    }

    fun Int.dp() = dpToPx()

    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { navController.navigateUp() }

        //气泡 带 阴影

        /*val shapePathModel = ShapeAppearanceModel.builder()
            .setAllCorners(RoundedCornerTreatment())
            .setAllCornerSizes(16.dp())
            .setRightEdge(object :TriangleEdgeTreatment(8.dp(),false){
                override fun getEdgePath(
                    length: Float,
                    center: Float,
                    interpolation: Float,
                    shapePath: ShapePath
                ) {
                    super.getEdgePath(length, 12.dp(), interpolation, shapePath)
                }
            }).build()
            <!--android:clipChildren="false"-->
        val backgroundDrawable = MaterialShapeDrawable(shapePathModel).apply {
            setTint(requireContext().themeColor(R.attr.colorSecondary))
            paintStyle = Paint.Style.FILL

            shadowCompatibilityMode = MaterialShapeDrawable.SHADOW_COMPAT_MODE_ALWAYS
            initializeElevationOverlay(requireContext())
            elevation = 4.dp()
            setShadowColor(0x155e5e5e.toInt())
            shadowVerticalOffset = 2.dp().toInt()

        }
        binding.test.background = backgroundDrawable*/

        /*val shapePathModel = ShapeAppearanceModel.builder()
            .setAllCorners(RoundedCornerTreatment())
            .setAllCornerSizes(10.dp())
            .setAllEdges(TriangleEdgeTreatment(8.dp(),true))
            .build()
        val backgroundDrawable = MaterialShapeDrawable(shapePathModel).apply {
            setTint(requireContext().getColorByAttr(R.attr.colorSecondary))
            paintStyle = Paint.Style.FILL_AND_STROKE
        }
        binding.test.background = backgroundDrawable*/

    }

    override fun setBinding(): FragmentAboutBinding = FragmentAboutBinding.inflate(layoutInflater)
}