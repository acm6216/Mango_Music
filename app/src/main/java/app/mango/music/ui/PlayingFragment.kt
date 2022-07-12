package app.mango.music.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.content.FileProvider
import androidx.media3.common.Player
import app.mango.music.BuildConfig
import app.mango.music.R
import app.mango.music.audio.PlayDuration
import app.mango.music.audio.PlayPosition
import app.mango.music.audio.getDuration
import app.mango.music.data.MusicFavorite
import app.mango.music.databinding.FragmentPlayingBinding
import app.mango.music.data.GlobalData
import app.mango.music.manager.Config
import app.mango.music.manager.SpManager
import app.mango.music.ui.settings.AudioEffectFragment
import app.mango.music.views.IconStateButton
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class PlayingFragment : BaseFragment<FragmentPlayingBinding>(), BaseFragment.ControlListener {

    private val bottomPlaylist: PlaylistSheetFragment by lazy(LazyThreadSafetyMode.NONE) {
        requireActivity().supportFragmentManager.findFragmentById(R.id.play_list) as PlaylistSheetFragment
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.listener = this

        binding.apply {
            playerSeekbar.setProgressListener {
                mediaBrowser.browser?.seekTo(it.toLong())
            }
            buttonShare.setOnClickListener { share() }
            buttonKeyDown.setOnClickListener { navController.navigateUp() }
            buttonQueue.setOnClickListener { bottomPlaylist.open() }

            buttonRepeat.setOnClickListener {
                SpManager.putInt(
                    Config.KEY_SETTINGS_REPEAT_MODE,
                    if(it.isActivated) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ONE
                )
            }
            buttonVolume.setOnClickListener {
                AudioEffectFragment()
                    .show(childFragmentManager,null)
            }
            buttonShuffle.setOnClickListener {
                SpManager.putInt(
                    Config.KEY_SETTINGS_REPEAT_MODE,
                    if(it.isActivated) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ALL
                )
            }
            SpManager.listen(Config.KEY_SETTINGS_REPEAT_MODE,
                SpManager.SpIntListener(Config.DEFAULT_SETTINGS_REPEAT_MODE) {
                    buttonShuffle.isActivated = it == 2
                    buttonRepeat.isActivated = it == 1
                })
        }

        repeatWithViewLifecycle {
            launch {
                GlobalData.currentIsPlaying.collect {
                    binding.buttonPlay.isChecked = it
                }
            }
            launch {
                GlobalData.currentMediaItem.collect {
                    it?.let { media ->
                        binding.media = media
                        musicViewModel.mediaFavoriteId.value = media.mediaId
                        binding.duration = PlayDuration(media.mediaMetadata.getDuration().toInt())
                    }
                }
            }
            launch {
                GlobalData.currentPosition.collect {
                    binding.position = PlayPosition(it.toInt())
                }
            }
            launch {
                musicViewModel.mediaFavorite.collect {
                    binding.buttonFavorite.isFavorite(it)
                }
            }
        }
    }

    private fun IconStateButton.isFavorite(musicFavorite: MusicFavorite) {
        isActivated = musicFavorite.isFavorite
        setOnClickListener {
            onFavoriteChanged(musicFavorite.mediaId, !musicFavorite.isFavorite)
        }
    }

    override fun setBinding(): FragmentPlayingBinding =
        FragmentPlayingBinding.inflate(layoutInflater)

    override fun togglePlay() {
        mediaBrowser.togglePlay()
    }

    override fun next() {
        mediaBrowser.browser?.seekToNext()
    }

    override fun previous() {
        mediaBrowser.browser?.seekToPrevious()
    }

    private fun share(){
        GlobalData.currentMediaItem.value?:return
        val uri = GlobalData.currentMediaItem.value!!.mediaMetadata.mediaUri
        val share = Intent(Intent.ACTION_SEND)
        share.putExtra(Intent.EXTRA_STREAM, uri)
        share.type = "audio/*"
        share.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        requireContext().startActivity(Intent.createChooser(share, getString(R.string.music_share_file)))
    }

    companion object {
        private const val TAG = "PlayingFragment"
    }
}