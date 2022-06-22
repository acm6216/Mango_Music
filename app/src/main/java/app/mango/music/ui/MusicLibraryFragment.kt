package app.mango.music.ui

import android.os.Bundle
import android.view.View
import androidx.core.view.doOnPreDraw
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import app.mango.music.R
import app.mango.music.adapters.AlbumAdapter
import app.mango.music.adapters.ArtistAdapter
import app.mango.music.audio.Album
import app.mango.music.audio.Artist
import app.mango.music.audio.MusicBox
import app.mango.music.databinding.FragmentMusicLibraryBinding
import app.mango.music.manager.SpManager
import com.google.android.material.transition.MaterialElevationScale
import com.google.android.material.transition.MaterialFadeThrough
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MusicLibraryFragment : BaseFragment<FragmentMusicLibraryBinding>(),
    AlbumAdapter.AlbumItemListener,
    ArtistAdapter.ArtistItemListener {

    private val args: MusicLibraryFragmentArgs by navArgs()

    private val albumAdapter = AlbumAdapter(this)
    private val artistAdapter = ArtistAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFadeThrough().apply {
            duration = resources.getInteger(R.integer.motion_duration_large).toLong()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }

        repeatWithViewLifecycle {
            when (args.musicBox) {
                MusicBox.MUSIC_LIBRARY -> launch {
                    musicViewModel.favoriteSongs.collect {
                        musicAdapter.submitList(it)
                    }
                }
                MusicBox.ALBUM -> launch {
                    musicViewModel.albums.collect {
                        albumAdapter.submitList(it)
                    }
                }
                MusicBox.ARTIST -> launch {
                    musicViewModel.artists.collect {
                        artistAdapter.submitList(it)
                    }
                }
                else -> launch {
                    musicViewModel.favorited.collect {
                        musicAdapter.submitList(it)
                    }
                }
            }
        }

        binding.lifecycleOwner = viewLifecycleOwner
        binding.recyclerView.run {
            ThemedFastScroller.create(this)
            adapter = when (args.musicBox) {
                MusicBox.MUSIC_LIBRARY, MusicBox.FAVORITE -> {
                    itemTouchHelper()
                    musicAdapter
                }
                MusicBox.ALBUM -> albumAdapter
                else -> artistAdapter
            }

            SpManager.listen(getString(R.string.key_set_list_filter_grid),
                SpManager.SpIntListener(getString(R.string.set_list_filter_grid_def).toInt()) {
                    layoutManager = GridLayoutManager(context,it+when (args.musicBox) {
                        MusicBox.MUSIC_LIBRARY, MusicBox.FAVORITE -> 0
                        else -> 2
                    })
                })
        }

    }

    override fun onAlbumClick(view: View, album: Album) {
        navigateToDetail(view, album.albumId, MusicBox.ALBUM)
    }

    override fun onArtistClick(view: View, artist: Artist) {
        navigateToDetail(view, artist.artistId, MusicBox.ARTIST)
    }

    private fun navigateToDetail(view: View, id: String, musicBox: MusicBox) {
        exitTransition = MaterialElevationScale(false).apply {
            duration = resources.getInteger(R.integer.motion_duration_large).toLong()
        }
        reenterTransition = MaterialElevationScale(true).apply {
            duration = resources.getInteger(R.integer.motion_duration_large).toLong()
        }
        val cardDetailTransitionName = getString(R.string.music_card_detail_transition_name)
        val extras = FragmentNavigatorExtras(view to cardDetailTransitionName)
        val directions =
            MusicLibraryFragmentDirections.actionMusicLibraryFragmentToDetailFragment(id, musicBox)
        navController.navigate(directions, extras)
    }

    override fun setBinding(): FragmentMusicLibraryBinding =
        FragmentMusicLibraryBinding.inflate(layoutInflater)
}