package app.mango.music.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener

/**
 * 根据
 * https://github.com/rjpacket/IndicatorView/
 * 修改
 */
class IndicatorView  @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr){

    private val pointPaint = Paint().apply {
        isAntiAlias = true
        color = colorControlNormal()
    }
    private var childCount = 0
    private val selectPointPaint = Paint().apply {
        isAntiAlias = true
        color = colorPrimary()
    }
    private var selectPosition = 0
    private var scrollPosition = 0
    private var ratio = 0f
    private val pointSpace = 16.dp().toInt()
    private val radius = 4.dp().toInt()

    private val pointViews = ArrayList<PointView>()
    private lateinit var barView: BarView

    private fun Int.dp(): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), resources.displayMetrics)

    private fun colorControlNormal(): Int =
        getColorForAttrId(androidx.appcompat.R.attr.colorControlNormal)

    private fun colorPrimary(): Int = getColorForAttrId(androidx.appcompat.R.attr.colorPrimary)

    private fun getColorForAttrId(resId: Int): Int {
        val typedValue = TypedValue()
        val a = context.obtainStyledAttributes(typedValue.data, intArrayOf(resId))
        val color = a.getColor(0, Color.BLUE)
        a.recycle()
        return color
    }

    fun setViewPager(viewPager: ViewPager) {
        childCount = viewPager.adapter!!.count
        initPoints()
        viewPager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                scrollPosition = position
                ratio = positionOffset
                if (ratio >= 1 || ratio <= 0) { return }
                compute()
                invalidate()
            }

            override fun onPageSelected(position: Int) {
                selectPosition = position
                ratio = 0f
                compute()
                invalidate()
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
        ratio = 0f
        compute()
        invalidate()
    }

    private fun initPoints() {
        for (i in 0 until childCount) {
            pointViews.add(PointView())
        }
        barView = BarView()
    }

    private fun compute() {
        //计算球位置
        pointViews.forEachIndexed { index, pointView ->
            pointView.radius = radius
            pointView.x = radius * (2 * index + 1) + pointSpace * index
            pointView.y = radius
            pointView.isChecked = selectPosition == index
        }
        //计算bar位置
        val (selectX, selectY) = pointViews[selectPosition]
        barView.radius = radius
        if (selectPosition <= scrollPosition) {
            //往右是增加右边圆的圆心
            if (ratio <= 0.5) {
                barView.leftX = selectX
                barView.leftY = selectY
                barView.rightX = (selectX + (2 * radius + pointSpace) * ratio).toInt()
                barView.rightY = selectY
                barView.bezierTopX = (selectX + selectY) / 2
                barView.bezierTopY = (radius * 1.0 / 4 + 3 * ratio * radius / 2).toInt()
                barView.bezierBottomY = (radius * 1.0 * 7 / 4 - 3 * ratio * radius / 2).toInt()
            } else {
                barView.leftX = (selectX + (2 * radius + pointSpace) * ratio).toInt()
                barView.leftY = selectY
                val (x, y) = pointViews[selectPosition + 1]
                barView.rightX = x
                barView.rightY = y
                barView.bezierTopX = (barView.leftX + barView.rightX) / 2
                barView.bezierTopY = (radius * 1.0 * 7 / 4 - 3 * ratio * radius / 2).toInt()
                barView.bezierBottomY = (radius * 1.0 / 4 + 3 * ratio * radius / 2).toInt()
            }
        } else {
            //往左是减少左边圆的圆心
            if (ratio >= 0.5) {
                barView.rightX = selectX
                barView.rightY = selectY
                barView.leftX = (selectX - (2 * radius + pointSpace) * (1 - ratio)).toInt()
                barView.leftY = selectY
                barView.bezierTopX = (barView.leftX + barView.rightX) / 2
                barView.bezierTopY = (radius * 1.0 * 7 / 4 - 3 * ratio * radius / 2).toInt()
                barView.bezierBottomY = (radius * 1.0 / 4 + 3 * ratio * radius / 2).toInt()
            } else {
                barView.rightX = (selectX - (2 * radius + pointSpace) * (1 - ratio)).toInt()
                barView.rightY = selectY
                val (x, y) = pointViews[selectPosition - 1]
                barView.leftX = x
                barView.leftY = y
                barView.bezierTopX = (barView.leftX + barView.rightX) / 2
                barView.bezierTopY = (radius * 1.0 / 4 + 3 * ratio * radius / 2).toInt()
                barView.bezierBottomY = (radius * 1.0 * 7 / 4 - 3 * ratio * radius / 2).toInt()
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        //设置view宽高 不设置就是默认全屏view，没办法改变位置
        if (pointViews.isNotEmpty()) {
            val width = pointViews[pointViews.size - 1].x + radius
            val height = radius * 4
            setMeasuredDimension(width, height)
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        pointViews.forEach {
            canvas.drawCircle(
                it.x.toFloat(),
                it.y.toFloat(),
                it.radius.toFloat(),
                if (it.isChecked) selectPointPaint else pointPaint
            )
        }
        canvas.drawCircle(
            barView.leftX.toFloat(),
            barView.leftY.toFloat(),
            barView.radius.toFloat(),
            selectPointPaint
        )
        canvas.drawCircle(
            barView.rightX.toFloat(),
            barView.rightY.toFloat(),
            barView.radius.toFloat(),
            selectPointPaint
        )

        val bezierPath = Path()
        bezierPath.moveTo(barView.leftX.toFloat(), 0f)
        bezierPath.quadTo(barView.bezierTopX.toFloat(),
            barView.bezierTopY.toFloat(), barView.rightX.toFloat(), 0f)
        bezierPath.lineTo(barView.rightX.toFloat(), (2 * radius).toFloat())
        bezierPath.quadTo(
            barView.bezierTopX.toFloat(),
            barView.bezierBottomY.toFloat(),
            barView.leftX.toFloat(),
            (2 * radius).toFloat()
        )
        bezierPath.lineTo(barView.leftX.toFloat(), 0f)
        canvas.drawPath(bezierPath, selectPointPaint)
    }

    data class PointView(
        var x:Int = 0,var y:Int = 0,
        var radius:Int = 0,var isChecked:Boolean = false
    )

    data class BarView(
        var leftX:Int = 0,var leftY:Int = 0,
        var rightX:Int = 0,var rightY:Int = 0,
        var radius:Int = 0,var bezierTopX:Int = 0,
        var bezierTopY:Int = 0,var bezierBottomY:Int = 0
    )
}