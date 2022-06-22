package app.mango.music.views.lrcview

import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.text.format.DateUtils
import java.util.*
import java.util.regex.Pattern

data class LrcEntryKt(val time:Long,val text:String):Comparable<LrcEntryKt> {

    private lateinit var staticLayout: StaticLayout
    var offset = Float.MIN_VALUE

    override fun compareTo(other: LrcEntryKt) = (time - other.time).toInt()

    fun init(paint: TextPaint, width: Int, gravity: Int) {
        val align = when (gravity) {
            GRAVITY_LEFT -> Layout.Alignment.ALIGN_NORMAL
            GRAVITY_CENTER -> Layout.Alignment.ALIGN_CENTER
            GRAVITY_RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
            else -> Layout.Alignment.ALIGN_CENTER
        }
        staticLayout = StaticLayout.Builder.obtain(
            text, 0, text.length,
            paint, width
        ).setAlignment(align)
            .setLineSpacing(0f, 1f)
            .build()
    }

    fun height() = if(this::staticLayout.isInitialized) staticLayout.height else 0

    fun isInitialized(unit:((StaticLayout)->Unit)){
        if(this::staticLayout.isInitialized)
            unit.invoke(staticLayout)
    }

    companion object{

        const val GRAVITY_LEFT = 1
        const val GRAVITY_CENTER = 2
        const val GRAVITY_RIGHT = 3

        fun formatTime(milli:Long):String {
            val m = (milli / DateUtils.MINUTE_IN_MILLIS).toInt()
            val s = ((milli / DateUtils.SECOND_IN_MILLIS) % 60).toInt()
            val mm = String.format(Locale.getDefault(), "%02d", m)
            val ss = String.format(Locale.getDefault(), "%02d", s)
            return "$mm:$ss"
        }

        fun parseLrc(lrcText: String): List<LrcEntryKt> {
            if (TextUtils.isEmpty(lrcText)) {
                return emptyList()
            }
            val entryList: MutableList<LrcEntryKt> = ArrayList()
            lrcText.split("\n").forEach{
                parseLine(it).run{
                    if (isNotEmpty()) { entryList.addAll(this) }
                }
            }
            entryList.sort()
            return entryList
        }

        private fun parseLine(lines: String): List<LrcEntryKt> {
            if (TextUtils.isEmpty(lines)) {
                return emptyList()
            }
            val line = lines.trim()
            val lineMatcher =
                Pattern.compile("((\\[\\d\\d:\\d\\d\\.\\d{2,3}])+)(.+)").matcher(line)
            if (!lineMatcher.matches()) {
                return emptyList()
            }
            val times = lineMatcher.group(1)?:"00:00:00"
            val text = lineMatcher.group(3)?:""
            val entryList: MutableList<LrcEntryKt> = ArrayList()

            val timeMatcher = Pattern.compile("\\[(\\d\\d):(\\d\\d)\\.(\\d){2,3}]").matcher(times)
            while (timeMatcher.find()) {
                val min = (timeMatcher.group(1)?:"0").toLong()
                val sec = (timeMatcher.group(2)?:"0").toLong()
                val mil = (timeMatcher.group(3)?:"0").toLong()
                val time =
                    min * DateUtils.MINUTE_IN_MILLIS + sec * DateUtils.SECOND_IN_MILLIS + if (mil >= 100L) mil else mil * 10
                entryList.add(LrcEntryKt(time, text))
            }
            return entryList
        }
    }

}