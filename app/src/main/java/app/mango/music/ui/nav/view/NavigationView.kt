package app.mango.music.ui.nav.view

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.AttrRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.mango.music.R

class NavigationView:RecyclerView {

    constructor(context: Context) : super(context){ createMenu()}
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs){ createMenu()}
    constructor(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int) : super(
        context, attrs, defStyleAttr){ createMenu()}

    private var onItemClickListener: OnItemClickListener? = null

    private fun createMenu(){
        this.layoutManager = LinearLayoutManager(context)
        this.adapter =  NavigationAdapter(R.menu.menu_main,context){
            onItemClickListener?.onItemClick(it)
        }
    }

    fun notifyItemChanged(index:Int,value:String){
        adapter?.let{
            (it as NavigationAdapter).notifyItemChanged(index,value)
        }
    }

    fun setOnItemClickListener(listener: OnItemClickListener){
        this.onItemClickListener = listener
    }

    interface OnItemClickListener{
        fun onItemClick(item: NavigationAdapter.Items)
    }
}