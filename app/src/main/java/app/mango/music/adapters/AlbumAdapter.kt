package app.mango.music.adapters

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import app.mango.music.audio.Album
import app.mango.music.databinding.ItemAlbumBinding
import app.mango.music.utils.layoutInflater
import me.zhanghai.android.fastscroll.PopupTextProvider

class AlbumAdapter(private val listener: AlbumItemListener): ListAdapter<Album, AlbumAdapter.AlbumViewHolder>(
    AlbumDiffCallback()
), PopupTextProvider {

    interface AlbumItemListener {
        fun onAlbumClick(view: View, album: Album)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder =
        AlbumViewHolder.from(parent)

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        holder.bind(getItem(position), listener)
    }

    class AlbumViewHolder private constructor(
        val binding: ItemAlbumBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(album: Album, listener: AlbumItemListener) {
            binding.albumRoot.setOnClickListener {
                listener.onAlbumClick(it, album)
            }
            binding.mediaItem = album.mediaItem
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup) = AlbumViewHolder(
                ItemAlbumBinding.inflate(parent.context.layoutInflater, parent, false)
            )
        }
    }

    override fun getPopupText(position: Int): String = getItem(position).first()

    private fun Album.first() = albumTitle.first().toString()

    class AlbumDiffCallback : DiffUtil.ItemCallback<Album>() {

        override fun areItemsTheSame(oldItem: Album, newItem: Album): Boolean {
            return oldItem.albumId == newItem.albumId
        }

        override fun areContentsTheSame(oldItem: Album, newItem: Album): Boolean {
            return oldItem == newItem
        }
    }
}