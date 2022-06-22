package app.mango.music.ui.nav.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.XmlResourceParser
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.RippleDrawable
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.mango.music.R
import app.mango.music.databinding.NavigationDividerBinding
import app.mango.music.databinding.NavigationItemBinding
import app.mango.music.drawable.AutoMirrorDrawable
import app.mango.music.utils.dpToDimensionPixelSize
import app.mango.music.utils.getColorStateListByAttr
import app.mango.music.utils.getColorStateListCompat
import app.mango.music.utils.layoutInflater
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel

class NavigationAdapter(menuId: Int, val context: Context, private val bind: ((item: Items) -> Unit)) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var menus: XmlResourceParser = context.resources.getLayout(menuId)
    private val items = ArrayList<Items>()

    init {
        var type = menus.eventType
        while (type != XmlResourceParser.END_DOCUMENT) {
            if (type != XmlResourceParser.START_TAG) {
                type = menus.next()
                continue
            }
            if (menus.name == "item") {
                items.add(
                    Items(
                    menus.getAttributeResourceValue(0, -1),
                    menus.getAttributeResourceValue(1, -1),
                    if(menus.attributeCount == 3) false
                    else menus.getAttributeBooleanValue(2,false),
                    menus.getAttributeResourceValue(if(menus.attributeCount==3) 2 else 3, -1),
                    ViewType.ITEM
                )
                )
            }
            else if(menus.name=="group"){
                items.add(
                    Items(
                        R.drawable.icon, R.id.toolbar,false,R.string.app_name,
                        ViewType.DIVIDER
                    )
                )
            }
            type = menus.next()
        }
    }

    enum class ViewType {
        ITEM,
        DIVIDER
    }

    data class Items(val icon: Int, val id: Int, val checked:Boolean, val title: Int, val viewType: ViewType, var tag: String = "")

    inner class ItemHolder(val binding: NavigationItemBinding) : RecyclerView.ViewHolder(binding.root)
    inner class DividerHolder(val binding: NavigationDividerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when(ViewType.values()[viewType]){
            ViewType.ITEM -> ItemHolder(NavigationItemBinding.inflate(parent.context.layoutInflater,parent,false)).apply {
                binding.root.background = createItemBackground(context)
                binding.root.foregroundCompat = createItemForeground(context)
                binding.navIcon.imageTintList = binding.navIcon.imageTintList?.let {
                    NavigationItemColor.create(
                        it, binding.navIcon.context
                    )
                }
                binding.navTitle.setTextColor(
                    NavigationItemColor.create(
                        binding.navTitle.textColors, binding.navTitle.context
                    )
                )
                binding.navTag.setTextColor(
                    NavigationItemColor.create(
                        binding.navTag.textColors, binding.navTitle.context
                    )
                )
            }
            ViewType.DIVIDER -> DividerHolder(NavigationDividerBinding.inflate(parent.context.layoutInflater,parent,false))
        }

    @SuppressLint("PrivateResource")
    private fun createItemBackground(context: Context): Drawable =
        createItemShapeDrawable(
            context.getColorStateListCompat(com.google.android.material.R.color.mtrl_navigation_item_background_color), context
        )

    private fun createItemForeground(context: Context): Drawable {
        val controlHighlightColor = context.getColorStateListByAttr(android.R.attr.colorControlHighlight)
        val mask = createItemShapeDrawable(ColorStateList.valueOf(Color.WHITE), context)
        return RippleDrawable(controlHighlightColor, null, mask)
    }

    private fun createItemShapeDrawable(fillColor: ColorStateList, context: Context): Drawable {
        val materialShapeDrawable = MaterialShapeDrawable(
            ShapeAppearanceModel.builder(
                context, R.style.ShapeAppearance_Navigation, 0
            ).build()
        ).apply { this.fillColor = fillColor }
        val rightInset = context.dpToDimensionPixelSize(8)
        return AutoMirrorDrawable(InsetDrawable(materialShapeDrawable, 0, 0, rightInset, 0))
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
        throw UnsupportedOperationException()

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        val item = items[position]
        when (ViewType.values()[getItemViewType(position)]) {
            ViewType.ITEM -> {
                val binding = (holder as ItemHolder).binding
                binding.navTag.text = item.tag
                binding.navIcon.setImageResource(item.icon)
                binding.navTitle.setText(item.title)
                binding.root.setOnClickListener {
                    bind.invoke(item)
                    if(item.checked) {
                        notifyItemChanged(lastPosition)
                        lastPosition = position
                        (binding.root as CheckableForegroundLinearLayout).isChecked = true
                    }
                }
                (binding.root as CheckableForegroundLinearLayout).isChecked = lastPosition == position
            }
            else -> {}
        }
    }

    companion object{
        var lastPosition = 1
    }

    override fun getItemCount(): Int = items.size
    override fun getItemViewType(position: Int): Int = items[position].viewType.ordinal

    fun notifyItemChanged(index: Int, value: String) {
        if (index < 0 || index > items.size - 1) return
        items[index].tag = value
        notifyItemChanged(index)
    }

    private var View.foregroundCompat: Drawable?
        get() = getForeground(this)
        set(value) {
            setForeground(this, value)
        }
}