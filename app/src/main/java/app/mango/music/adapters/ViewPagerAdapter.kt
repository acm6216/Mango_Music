package app.mango.music.adapters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.media3.common.MediaItem
import androidx.viewbinding.ViewBinding
import androidx.viewpager.widget.PagerAdapter
import app.mango.music.databinding.ViewPagerCoverBinding
import app.mango.music.databinding.ViewPagerLyricBinding
import app.mango.music.utils.layoutInflater

class ViewPagerAdapter(context: Context,val unit:((Long)->Unit)):PagerAdapter() {

    private val layoutInflater = context.layoutInflater
    private val views = mutableListOf<ViewBinding>()

    private fun <T:ViewBinding> MutableList<T>.lyric():ViewPagerLyricBinding
        = this[1] as ViewPagerLyricBinding

    private fun <T:ViewBinding> MutableList<T>.cover():ViewPagerCoverBinding
        = this[0] as ViewPagerCoverBinding

    init {
        views.add(ViewPagerCoverBinding.inflate(layoutInflater))
        views.add(ViewPagerLyricBinding.inflate(layoutInflater).apply {
            lrcView.setOnPlayClickListener{
                unit.invoke(it)
                true
            }
        })
    }

    fun updatePosition(position:Long) = views.lyric().lrcView.updateTime(position)

    fun updateMediaItem(mediaItem: MediaItem){
        views.run {
            cover().apply {
                media = mediaItem
                executePendingBindings()
            }
            lyric().apply {
                media = mediaItem
                executePendingBindings()
            }
        }
    }

    override fun getCount(): Int = views.size

    override fun isViewFromObject(view: View, obj: Any): Boolean = view == obj

    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
        container.removeView(views[position].root)
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        container.addView(views[position].root)
        return views[position].root
    }
}