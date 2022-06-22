package app.mango.music.adapters

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import app.mango.music.audio.Artist
import app.mango.music.databinding.ItemArtistBinding
import app.mango.music.utils.layoutInflater
import me.zhanghai.android.fastscroll.PopupTextProvider

class ArtistAdapter(private val listener: ArtistItemListener): ListAdapter<Artist, ArtistAdapter.ArtistViewHolder>(
    ArtistDiffCallback()
), PopupTextProvider {

    interface ArtistItemListener {
        fun onArtistClick(view: View, artist: Artist)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistViewHolder =
        ArtistViewHolder.from(parent)

    override fun onBindViewHolder(holder: ArtistViewHolder, position: Int) {
        holder.bind(getItem(position), listener)
    }

    class ArtistViewHolder private constructor(
        val binding: ItemArtistBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(artist: Artist, listener: ArtistItemListener) {
            binding.artistRoot.setOnClickListener {
                listener.onArtistClick(it, artist)
            }
            binding.mediaItem = artist.mediaItem
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup) = ArtistViewHolder(
                ItemArtistBinding.inflate(parent.context.layoutInflater, parent, false)
            )
        }
    }

    override fun getPopupText(position: Int): String = getItem(position).first()

    private fun Artist.first() = mediaItem.mediaMetadata.artist.toString().first().toString()

    class ArtistDiffCallback : DiffUtil.ItemCallback<Artist>() {

        override fun areItemsTheSame(oldItem: Artist, newItem: Artist): Boolean {
            return oldItem.artistId == newItem.artistId
        }

        override fun areContentsTheSame(oldItem: Artist, newItem: Artist): Boolean {
            return oldItem == newItem
        }
    }
}