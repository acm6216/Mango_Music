package app.mango.music.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import app.mango.music.R
import app.mango.music.audio.FavoriteMusic
import app.mango.music.databinding.MusicItemLayoutBinding
import app.mango.music.drawable.MusicSwipeActionDrawable
import app.mango.music.ui.ReboundingSwipeActionCallback
import me.zhanghai.android.fastscroll.PopupTextProvider
import kotlin.math.abs

class MusicAdapter(
    private val itemListener: ItemListener
    ) :
    ListAdapter<FavoriteMusic, MusicAdapter.TaskViewHolder>(
        ListItemDiffCallback()
    ), PopupTextProvider {

    override fun onCurrentListChanged(
        previousList: MutableList<FavoriteMusic>,
        currentList: MutableList<FavoriteMusic>
    ) {
        super.onCurrentListChanged(previousList, currentList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return TaskViewHolder.from(
            parent,
            itemListener,
            layoutInflater
        )
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position), currentList.map { it.mediaItem() }, position)
    }

    override fun getItemViewType(position: Int): Int = ITEM_VIEW_TYPE_TASK

    interface ItemListener {
        fun onClicked(view: View, index: Int, items: List<MediaItem>)
        fun onFavoriteChanged(mediaId: String, newValue: Boolean)
        fun onNextChanged(mediaId:String)
        fun onDelete(mediaItem: MediaItem)
    }

    class TaskViewHolder private constructor(
        val binding: MusicItemLayoutBinding,
        private val itemListener: ItemListener
    ) :
        RecyclerView.ViewHolder(binding.root), ReboundingSwipeActionCallback.ReboundableViewHolder {

        private val starredCornerSize =
            itemView.resources.getDimension(R.dimen.small_component_corner_radius)

        fun bind(favoriteMusic: FavoriteMusic, list: List<MediaItem>, position: Int) {
            binding.favoriteMusic = favoriteMusic.copy()
            binding.listener = itemListener
            favoriteMusic.isFavorite.updateCardViewTopLeftCornerSize()
            binding.root.background = MusicSwipeActionDrawable(binding.root.context)
            binding.root.isActivated = favoriteMusic.isFavorite
            binding.cardView.setOnClickListener {
                itemListener.onClicked(it, position, list)
            }
            binding.cardView.setOnLongClickListener {
                itemListener.onDelete(favoriteMusic.mediaItem())
                true
            }
            binding.executePendingBindings()
        }

        companion object {
            fun from(
                parent: ViewGroup,
                itemListener: ItemListener,
                layoutInflater: LayoutInflater
            ) = TaskViewHolder(
                MusicItemLayoutBinding.inflate(
                    layoutInflater,
                    parent,
                    false
                ), itemListener
            )
        }

        override val reboundableView: View
            get() = binding.cardView

        override fun onReboundOffsetChanged(
            currentSwipePercentage: Float,
            swipeThreshold: Float,
            currentTargetHasMetThresholdOnce: Boolean,
            dampedCoefficient:Float
        ) {
            if (dampedCoefficient<0 || currentTargetHasMetThresholdOnce) return

            val isStarred = binding.favoriteMusic?.isFavorite ?: false

            val interpolation = (currentSwipePercentage / swipeThreshold).coerceIn(0F, 1F)
            val adjustedInterpolation = abs((if (isStarred) 1F else 0F) - interpolation)
            updateCardViewTopLeftCornerSize(adjustedInterpolation)

            val thresholdMet = currentSwipePercentage >= swipeThreshold
            val shouldStar = when {
                thresholdMet && isStarred -> false
                thresholdMet && !isStarred -> true
                else -> return
            }
            binding.root.isActivated = shouldStar
        }

        override fun onReboundedFavorite() {
            binding.favoriteMusic?.let {
                itemListener.onFavoriteChanged(it.mediaItem().mediaId, !it.isFavorite)
                it.isFavorite = !it.isFavorite
            }
        }

        override fun onReboundedNext() {
            binding.favoriteMusic?.let {
                itemListener.onNextChanged(it.mediaItem().mediaId)
            }
        }

        private fun Boolean.updateCardViewTopLeftCornerSize() {
            val interpolation = if (this) 1F else 0F
            updateCardViewTopLeftCornerSize(interpolation)
        }

        private fun updateCardViewTopLeftCornerSize(interpolation: Float) {
            binding.cardView.apply {
                shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                    .setTopLeftCornerSize(interpolation * starredCornerSize)
                    .build()
            }
        }

    }

    companion object {
        const val ITEM_VIEW_TYPE_TASK = 1
    }

    override fun getPopupText(position: Int): String = getItem(position).first()

    private fun FavoriteMusic.first() = metadata().title.toString().first().toString()

    class ListItemDiffCallback : DiffUtil.ItemCallback<FavoriteMusic>() {
        override fun areItemsTheSame(oldItem: FavoriteMusic, newItem: FavoriteMusic): Boolean {
            return oldItem.mediaItem().mediaId == newItem.mediaItem().mediaId
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: FavoriteMusic, newItem: FavoriteMusic): Boolean {
            return oldItem == newItem
        }
    }
}