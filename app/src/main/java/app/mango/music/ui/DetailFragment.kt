package app.mango.music.ui

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import app.mango.music.R
import app.mango.music.audio.*
import app.mango.music.databinding.FragmentDetailBinding
import app.mango.music.manager.SpManager
import com.google.android.material.transition.MaterialContainerTransform
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class DetailFragment:BaseFragment<FragmentDetailBinding>() {

    companion object{
        private const val TAG = "DetailFragment"
    }

    private val args: DetailFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.nav_host_fragment_content_main
            duration = resources.getInteger(R.integer.motion_duration_large).toLong()
            scrimColor = Color.TRANSPARENT
        }
        musicViewModel.albumArtistId.value = args.id
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            navigationIcon.setOnClickListener {
                navController.navigateUp()
            }
            musicBox = args.musicBox
            attachmentRecyclerView.run {
                ThemedFastScroller.create(this)
                scrollVisibility(binding.divider)
                itemTouchHelper()
                SpManager.listen(getString(R.string.key_set_list_filter_grid),
                    SpManager.SpIntListener(getString(R.string.set_list_filter_grid_def).toInt()) {
                        layoutManager = GridLayoutManager(context,it)
                    })
                post {
                    adapter = musicAdapter
                }
            }
        }

        repeatWithViewLifecycle {
            when(args.musicBox) {
                MusicBox.ALBUM -> launch {
                    musicViewModel.albumDetail.collect { it->
                        if(it.isEmpty()) return@collect
                        val find = it.first()
                        val duration = it.sumOf { it.metadata().getDuration() }
                        musicAdapter.submitList(it)
                        binding.detail = Detail(find.mediaItem(),it.size,duration)
                        binding.executePendingBindings()
                    }
                }
                MusicBox.ARTIST -> launch {
                    musicViewModel.artistDetail.collect { it ->
                        if(it.isEmpty()) return@collect
                        val find = it.first()
                        val duration = it.sumOf { it.metadata().getDuration() }
                        musicAdapter.submitList(it)
                        binding.detail = Detail(find.mediaItem(),it.size,duration)
                        binding.executePendingBindings()
                    }
                }
                else -> {}
            }
        }
    }

    override fun setBinding(): FragmentDetailBinding = FragmentDetailBinding.inflate(layoutInflater)

}