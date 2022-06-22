package app.mango.music.views.lrcview

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.text.format.DateUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Scroller
import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.StyleableRes
import androidx.media3.common.MediaItem
import app.mango.music.App
import app.mango.music.R
import app.mango.music.views.lrcview.LrcEntryKt.Companion.formatTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 根据 https://gitee.com/xinzhongxingit_admin/lrcview_lyrics_highlight_lines 修改
 * 增加协程支持，优化代码
 */
class LrcViewKt @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.IO

    companion object {
        private const val ADJUST_DURATION: Long = 100
        private const val TIMELINE_KEEP_TIME = 4 * DateUtils.SECOND_IN_MILLIS
        private const val LRC_ANIMATION_DURATION = 1000
    }

    private val lrcEntryList = ArrayList<LrcEntryKt>()
    private val lrcPaint = TextPaint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.LEFT
    }
    private val timePaint = TextPaint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        strokeCap = Paint.Cap.ROUND
    }
    private val timeFontMetrics: Paint.FontMetrics = timePaint.fontMetrics

    private lateinit var playDrawable: Drawable
    private var mDividerHeight = 0f
    private var mAnimationDuration: Long = 0
    private var mNormalTextColor = 0
    private var mCurrentTextColor = 0
    private var mTimelineTextColor = 0
    private var mTimelineColor = 0
    private var mTimeTextColor = 0
    private var drawableWidth = 0
    private var timeTextWidth = 0
    private var mDefaultLabel: String? = null
    private var mLrcPadding = 0f
    private var onPlayClickListener: ((Long) -> Boolean)? = null
    private var mAnimator: ValueAnimator? = null
    private val mScroller = Scroller(context)
    private var mOffset = 0f
    private var mCurrentLine = 0
    private var flag: Any? = null
    private var isShowTimeline = false
    private var isTouching = false
    private var isFling = false
    private var textGravity = 0

    private val mSimpleOnGestureListener: SimpleOnGestureListener =
        object : SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                if (hasLrc() && onPlayClickListener != null) {
                    mScroller.forceFinished(true)
                    removeCallbacks(hideTimelineRunnable)
                    isTouching = true
                    isShowTimeline = true
                    invalidate()
                    return true
                }
                return super.onDown(e)
            }

            override fun onScroll(
                e1: MotionEvent, e2: MotionEvent,
                distanceX: Float, distanceY: Float
            ): Boolean {
                if (hasLrc()) {
                    mOffset += -distanceY
                    mOffset = min(mOffset, getOffset(0))
                    mOffset = max(mOffset, getOffset(lrcEntryList.size - 1))
                    invalidate()
                    return true
                }
                return super.onScroll(e1, e2, distanceX, distanceY)
            }

            override fun onFling(
                e1: MotionEvent, e2: MotionEvent,
                velocityX: Float, velocityY: Float
            ): Boolean {
                if (hasLrc()) {
                    mScroller.fling(
                        0, mOffset.toInt(),
                        0, velocityY.toInt(), 0,
                        0, getOffset(lrcEntryList.size - 1).toInt(),
                        getOffset(0).toInt()
                    )
                    isFling = true
                    return true
                }
                return super.onFling(e1, e2, velocityX, velocityY)
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (hasLrc() && isShowTimeline && playDrawable.bounds.contains(
                        e.x.toInt(),
                        e.y.toInt()
                    )
                ) {
                    val centerLine: Int = getCenterLine()
                    val centerLineTime = lrcEntryList[centerLine].time
                    // onPlayClick 消费了才更新 UI
                    onPlayClickListener?.let {
                        if (it.invoke(centerLineTime)) {
                            isShowTimeline = false
                            removeCallbacks(hideTimelineRunnable)
                            mCurrentLine = centerLine
                            invalidate()
                            return true
                        }
                    }
                }
                return super.onSingleTapConfirmed(e)
            }
        }

    private val mGestureDetector = GestureDetector(context, mSimpleOnGestureListener).apply {
        setIsLongpressEnabled(false)
    }

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.LrcViewKt)
        lrcPaint.textSize = ta.getDimensions(R.styleable.LrcViewKt_lrcTextSize, R.dimen.lrc_text_size)
        mDividerHeight = ta.getDimensions(R.styleable.LrcViewKt_lrcDividerHeight, R.dimen.lrc_divider_height)
        mNormalTextColor = ta.getColors(R.styleable.LrcViewKt_lrcNormalTextColor, R.color.lrc_normal_text_color)
        mCurrentTextColor = ta.getColors(R.styleable.LrcViewKt_lrcCurrentTextColor, R.color.lrc_current_text_color)
        mTimelineTextColor = ta.getColors(R.styleable.LrcViewKt_lrcTimelineTextColor, R.color.lrc_timeline_text_color)
        mTimelineColor = ta.getColors(R.styleable.LrcViewKt_lrcTimelineColor, R.color.lrc_timeline_color)
        timePaint.strokeWidth = ta.getDimensions(R.styleable.LrcViewKt_lrcTimelineHeight, R.dimen.lrc_timeline_height)
        mTimeTextColor = ta.getColors(R.styleable.LrcViewKt_lrcTimeTextColor, R.color.lrc_time_text_color)
        timePaint.textSize = ta.getDimensions(R.styleable.LrcViewKt_lrcTimeTextSize, R.dimen.lrc_time_text_size)
        mAnimationDuration = ta.getInt(R.styleable.LrcViewKt_lrcAnimationDuration, LRC_ANIMATION_DURATION).toLong()
        mAnimationDuration = if (mAnimationDuration < 0) LRC_ANIMATION_DURATION.toLong() else mAnimationDuration
        val label = ta.getString(R.styleable.LrcViewKt_lrcLabel)
        mDefaultLabel = if (TextUtils.isEmpty(label)) context.getString(R.string.lrc_label) else label
        mLrcPadding = ta.getDimension(R.styleable.LrcViewKt_lrcPadding, 0f)
        val play = ta.getDrawable(R.styleable.LrcViewKt_lrcPlayDrawable)
        playDrawable = play ?: resources.getDrawable(R.drawable.ic_play, resources.newTheme()).apply {
                setTint(colorControlNormal())
            }
        textGravity = ta.getInteger(R.styleable.LrcViewKt_lrcTextGravity, LrcEntryKt.GRAVITY_CENTER)
        ta.recycle()
        drawableWidth = getDimension(R.dimen.lrc_drawable_width)
        timeTextWidth = getDimension(R.dimen.lrc_time_width)

    }

    private fun colorControlNormal(): Int =
        getColorForAttrId(androidx.appcompat.R.attr.colorControlNormal)

    private fun getColorForAttrId(@AttrRes resId: Int): Int {
        val typedValue = TypedValue()
        val a = context.obtainStyledAttributes(typedValue.data, intArrayOf(resId))
        val color = a.getColor(0, Color.BLUE)
        a.recycle()
        return color
    }

    private fun TypedArray.getColors(@StyleableRes attrId: Int, @ColorRes defColorId: Int) =
        this.getColor(attrId, resources.getColor(defColorId, resources.newTheme()))

    private fun TypedArray.getDimensions(@StyleableRes attrId: Int, @DimenRes defDimId: Int) =
        this.getDimension(attrId, resources.getDimension(defDimId))

    private fun getDimension(@DimenRes dimId: Int) = resources.getDimension(dimId).toInt()

    fun setOnPlayClickListener(unit: ((Long) -> Boolean)) {
        onPlayClickListener = unit
    }

    fun loadLrc(mediaItem: MediaItem){
        launch(Dispatchers.IO) {
            (context.applicationContext as App).apply {
                lyricsFetcher.fetch(mediaItem).run {
                    loadLrc(this.first)
                }
            }
        }
    }

    private fun loadLrc(lrcText: String) {
        launch(Dispatchers.Main) {
            reset()
            flag = lrcText
            onLrcLoaded(LrcEntryKt.parseLrc(lrcText))
            flag = null
        }
    }

    /**
     * 歌词是否有效
     */
    fun hasLrc(): Boolean = lrcEntryList.isNotEmpty()

    /**
     * 刷新歌词
     * @param time 当前播放时间
     */
    fun updateTime(time: Long) {
        launch(Dispatchers.Main) {
            if (hasLrc()) {
                findShowLine(time).notEqual(mCurrentLine) {
                    mCurrentLine = it
                    if (!isShowTimeline) scrollTo(it)
                    else invalidate()
                }
            }
        }
    }

    private inline fun Int.notEqual(value: Int, unit: ((Int) -> Unit)) {
        if (this != value) unit.invoke(this)
    }

    /**
     * 画一行歌词
     * @param y 歌词中心 Y 坐标
     */
    private fun StaticLayout.drawText(canvas: Canvas, y: Float) {
        canvas.save()
        canvas.translate(mLrcPadding, y - this.height / 2)
        this.draw(canvas)
        canvas.restore()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            initEntryList()
            val l = (timeTextWidth - drawableWidth) / 2
            val t = height / 2 - drawableWidth / 2
            playDrawable.setBounds(l, t, l + drawableWidth, t + drawableWidth)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerY = height / 2

        if (!hasLrc()) {
            lrcPaint.color = mCurrentTextColor
            val label = mDefaultLabel.toString()
            StaticLayout.Builder.obtain(
                label, 0, label.length,
                lrcPaint, getLrcWidth().toInt()
            ).setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(1f, 0f)
                .build()
                .drawText(canvas, centerY.toFloat())
            return
        }
        val centerLine = getCenterLine()
        if (isShowTimeline) {
            playDrawable.draw(canvas)
            timePaint.color = mTimelineColor
            canvas.drawLine(
                timeTextWidth.toFloat(), centerY.toFloat(),
                (width - timeTextWidth).toFloat(),
                centerY.toFloat(), timePaint
            )
            timePaint.color = mTimeTextColor
            val timeText = formatTime(lrcEntryList[centerLine].time)
            val timeX = (width - timeTextWidth / 2).toFloat()
            val timeY = centerY - (timeFontMetrics.descent + timeFontMetrics.ascent) / 2
            canvas.drawText(timeText, timeX, timeY, timePaint)
        }
        canvas.translate(0f, mOffset)
        var y = 0f
        for (i in lrcEntryList.indices) {
            if (i > 0) {
                y += (lrcEntryList[i - 1].height() + lrcEntryList[i].height()) / 2 + mDividerHeight
            }
            when {
                i == mCurrentLine -> lrcPaint.color = mCurrentTextColor
                (isShowTimeline && i == centerLine) -> lrcPaint.color = mTimelineTextColor
                else -> lrcPaint.color = mNormalTextColor
            }
            lrcEntryList[i].isInitialized {
                it.drawText(canvas, y)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
            isTouching = false
            if (hasLrc() && !isFling) {
                adjustCenter()
                postDelayed(hideTimelineRunnable, TIMELINE_KEEP_TIME)
            }
        }
        return mGestureDetector.onTouchEvent(event)
    }

    private val hideTimelineRunnable = Runnable {
        if (hasLrc() && isShowTimeline) {
            isShowTimeline = false
            scrollTo(mCurrentLine)
        }
    }

    override fun computeScroll() {
        if (mScroller.computeScrollOffset()) {
            mOffset = mScroller.currY.toFloat()
            invalidate()
        }
        if (isFling && mScroller.isFinished) {
            isFling = false
            if (hasLrc() && !isTouching) {
                adjustCenter()
                postDelayed(hideTimelineRunnable, TIMELINE_KEEP_TIME)
            }
        }
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(hideTimelineRunnable)
        super.onDetachedFromWindow()
    }

    private fun onLrcLoaded(entryList: List<LrcEntryKt>) {
        if (entryList.isNotEmpty()) {
            lrcEntryList.addAll(entryList)
        }
        initEntryList()
        invalidate()
    }

    private fun initEntryList() {
        if (!hasLrc() || width == 0) {
            return
        }
        lrcEntryList.sort()
        for (lrcEntry in lrcEntryList) {
            lrcEntry.init(lrcPaint, getLrcWidth().toInt(), textGravity)
        }
        mOffset = (height / 2).toFloat()
    }

    private fun reset() {
        endAnimation()
        mScroller.forceFinished(true)
        isShowTimeline = false
        isTouching = false
        isFling = false
        removeCallbacks(hideTimelineRunnable)
        lrcEntryList.clear()
        mOffset = 0f
        mCurrentLine = 0
        invalidate()
    }

    /**
     * 滚动到某一行
     */
    private fun scrollTo(line: Int) = scrollTo(line, mAnimationDuration)

    /**
     * 将中心行微调至正中心
     */
    private fun adjustCenter() = scrollTo(getCenterLine(), ADJUST_DURATION)

    private fun scrollTo(line: Int, dur: Long) {
        val offset: Float = getOffset(line)
        endAnimation()
        mAnimator = ValueAnimator.ofFloat(mOffset, offset)
        mAnimator?.run {
            duration = dur
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                mOffset = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun endAnimation() {
        mAnimator?.run {
            if (isRunning) end()
        }
    }

    /**
     * 二分法查找当前时间应该显示的行数（最后一个 <= time 的行数）
     */
    private fun findShowLine(time: Long): Int {
        var left = 0
        var right = lrcEntryList.size
        while (left <= right) {
            val middle = (left + right) / 2
            val middleTime = lrcEntryList[middle].time
            if (time < middleTime) {
                right = middle - 1
            } else {
                if (middle + 1 >= lrcEntryList.size || time < lrcEntryList[middle + 1].time) {
                    return middle
                }
                left = middle + 1
            }
        }
        return 0
    }

    private fun getCenterLine(): Int {
        var centerLine = 0
        var minDistance = Float.MAX_VALUE
        for (i in lrcEntryList.indices) {
            if (abs(mOffset - getOffset(i)) < minDistance) {
                minDistance = abs(mOffset - getOffset(i))
                centerLine = i
            }
        }
        return centerLine
    }

    private fun getOffset(line: Int): Float {
        if (lrcEntryList[line].offset == Float.MIN_VALUE) {
            var offset = (height / 2).toFloat()
            for (i in 1..line) {
                offset -= (lrcEntryList[i - 1].height() + lrcEntryList[i].height()) / 2 + mDividerHeight
            }
            lrcEntryList[line].offset = offset
        }
        return lrcEntryList[line].offset
    }

    private fun getLrcWidth(): Float = width - mLrcPadding * 2
}