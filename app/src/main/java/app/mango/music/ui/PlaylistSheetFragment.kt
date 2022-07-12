package app.mango.music.ui

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.mango.music.R
import app.mango.music.audio.FavoriteMusic
import app.mango.music.databinding.FragmentPlaylistBinding
import app.mango.music.data.GlobalData
import app.mango.music.manager.SpManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class PlaylistSheetFragment : BaseFragment<FragmentPlaylistBinding>() {

    override fun setBinding(): FragmentPlaylistBinding =
        FragmentPlaylistBinding.inflate(layoutInflater)

    private lateinit var behavior: BottomSheetBehavior<ConstraintLayout>
    private var current: MediaItem? = null

    override fun createViewBefore() {
        binding.apply {
            behavior = BottomSheetBehavior.from(lessonsSheet)
            behavior.state = STATE_HIDDEN
            val backCallback = requireActivity()
                .onBackPressedDispatcher.addCallback(viewLifecycleOwner, false) {
                    behavior.state = STATE_HIDDEN
                }
            behavior.addBottomSheetCallback(
                onStateChanged = {_,newState->
                    backCallback.isEnabled = newState == STATE_EXPANDED
                    if (newState == STATE_EXPANDED) {
                        current?.run {
                            val index = musicAdapter.currentList
                                .indexOfFirst { it.mediaItem().mediaId == mediaId }
                            binding.playlist.scrollToPos(index)
                        }
                    }
                }
            )
        }
    }

    fun open() {
        if (this::behavior.isInitialized)
            behavior.state = STATE_EXPANDED
    }

    private fun RecyclerView.scrollToPos(index: Int) {
        when {
            index < 0 -> {}
            index < 20 -> smoothScrollToPosition(index)
            else -> scrollToPosition(index)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.adapter = musicAdapter

        repeatWithViewLifecycle {
            launch {
                musicViewModel.playlist.collect { list ->
                    musicAdapter.submitList(list)
                    binding.playlistTitle.text = getString(R.string.playlist_title,list.size)
                }
            }
            launch {
                GlobalData.currentMediaItem.value?.let {
                    current = it
                }
            }
        }
        binding.run {
            playlist.apply {
                itemTouchHelper()
                scrollVisibility(binding.playlistTitleDivider)
                ThemedFastScroller.create(this)
                SpManager.listen(getString(R.string.key_set_list_filter_grid),
                    SpManager.SpIntListener(getString(R.string.set_list_filter_grid_def).toInt()) {
                        layoutManager = GridLayoutManager(context, it)
                    })
            }
            collapsePlaylist.setOnClickListener {
                behavior.state = STATE_HIDDEN
            }
        }
    }
}
