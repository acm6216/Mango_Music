package app.mango.music.ui.main

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.annotation.MenuRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import app.mango.music.R
import app.mango.music.audio.MusicBox
import app.mango.music.audio.ReadyWhenState
import app.mango.music.databinding.ActivityMainBinding
import app.mango.music.data.BaseMediaSource
import app.mango.music.data.GlobalData
import app.mango.music.service.SongBrowser
import app.mango.music.ui.MusicLibraryFragmentDirections
import app.mango.music.ui.PlayingFragmentDirections
import app.mango.music.ui.nav.*
import app.mango.music.ui.settings.ListSettingsFragment
import app.mango.music.viewmodel.MusicViewModel
import app.mango.music.views.PlayStateButton
import com.google.android.material.transition.MaterialElevationScale
import com.google.android.material.transition.MaterialFadeThrough
import com.google.android.material.transition.MaterialSharedAxis

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>(), Toolbar.OnMenuItemClickListener,
    NavController.OnDestinationChangedListener {

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun setLayout(): ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)

    @Inject
    lateinit var mSongBrowser: SongBrowser

    @Inject
    lateinit var mediaSource: BaseMediaSource

    private var globalMusicBox = MusicBox.MUSIC_LIBRARY

    private val navController: NavController
        get() = findNavController(R.id.nav_host_fragment_content_main)

    private val currentNavigationFragment: Fragment?
        get() = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
            ?.childFragmentManager
            ?.fragments
            ?.first()

    private val bottomNavDrawer: BottomNavDrawerFragment by lazy(LazyThreadSafetyMode.NONE) {
        supportFragmentManager.findFragmentById(R.id.bottom_nav_drawer) as BottomNavDrawerFragment
    }
    private val musicViewModel: MusicViewModel by viewModels()

    private val navOptions = NavOptions.Builder()
        .setLaunchSingleTop(true)
        .setEnterAnim(R.anim.enter_in)
        .setPopEnterAnim(R.anim.enter_out)
        .setExitAnim(R.anim.exit_in)
        .setPopExitAnim(R.anim.exit_out)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding.fab.bind()
        binding.run {
            bottomAppBarContentContainer.setOnClickListener {
                bottomNavDrawer.toggle()
            }
            bottomAppBar.setOnMenuItemClickListener(this@MainActivity)
        }
        bottomNavDrawer.bind()
        navController.addOnDestinationChangedListener(this)

        lifecycle.addObserver(mSongBrowser)
        mediaSource.whenReady { ready ->
            musicViewModel.whenReady.value = ReadyWhenState(System.currentTimeMillis(), ready)
        }

        requestPermission {
            mediaSource.loadSync()
        }

        repeatWithViewLifecycle {
            launch {
                GlobalData.currentIsPlaying.collect {
                    binding.fab.isPlaying(it)
                }
            }
            launch {
                GlobalData.currentMediaItem.collect {
                    binding.fab.setCover(it)
                }
            }
        }
    }

    private fun PlayStateButton.bind(){
        setShowMotionSpecResource(R.animator.fab_show)
        setHideMotionSpecResource(R.animator.fab_hide)
        setOnClickListener {
            navigateToPlaying()
        }
    }

    private fun BottomNavDrawerFragment.bind() {
        addOnSlideAction(HalfClockwiseRotateSlideAction(binding.bottomAppBarChevron))
        addOnSlideAction(AlphaSlideAction(binding.bottomAppBarTitle, true))
        addOnStateChangedAction(
            ShowHideFabStateAction(
                binding.fab, navController, intArrayOf(
                    R.id.search_fragment, R.id.settings_fragment, R.id.playing_fragment
                )
            )
        )
        addOnStateChangedAction(ChangeSettingsMenuStateAction { showSettings ->
            binding.bottomAppBar.replaceMenu(
                if (showSettings) R.menu.bottom_app_bar_settings_menu
                else getBottomAppBarMenuForDestination()
            )
        })

        setNavigationListener { id, titleId, _ ->
            binding.bottomAppBarTitle.setText(titleId)
            when (id) {
                R.id.to_about -> navigateToStyle()
                else -> navigateToHome(
                    titleId, when (id) {
                        R.id.to_album -> MusicBox.ALBUM
                        R.id.to_artist -> MusicBox.ARTIST
                        R.id.to_favorite -> MusicBox.FAVORITE
                        else -> MusicBox.MUSIC_LIBRARY
                    }
                )
            }
        }
    }

    private fun navigateToHome(@StringRes titleRes: Int, musicBox: MusicBox) {
        binding.bottomAppBarTitle.text = getString(titleRes)
        globalMusicBox = musicBox
        currentNavigationFragment?.apply {
            exitTransition = MaterialFadeThrough().apply {
                duration = resources.getInteger(R.integer.motion_duration_large).toLong()
            }
        }
        val directions = MusicLibraryFragmentDirections.actionGlobalHomeFragment(musicBox)
        navController.navigate(directions)
    }

    private fun setBottomAppBarForSearch() {
        hideBottomAppBar()
        binding.fab.hide()
    }

    private fun navigateToSearch() {
        bottomNavDrawer.close()
        currentNavigationFragment?.apply {
            exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply {
                duration = resources.getInteger(R.integer.motion_duration_large).toLong()
            }
            reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).apply {
                duration = resources.getInteger(R.integer.motion_duration_large).toLong()
            }
        }
        navController.navigate(R.id.search_fragment)
    }

    private fun navigateToSettings() {
        bottomNavDrawer.close()
        currentNavigationFragment?.apply {
            exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply {
                duration = resources.getInteger(R.integer.motion_duration_large).toLong()
            }
            reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).apply {
                duration = resources.getInteger(R.integer.motion_duration_large).toLong()
            }
        }
        navController.navigate(R.id.settings_fragment)
    }

    private fun navigateToStyle() {
        bottomNavDrawer.close()
        currentNavigationFragment?.apply {
            exitTransition = MaterialElevationScale(false).apply {
                duration = resources.getInteger(R.integer.motion_duration_large).toLong()
            }
            reenterTransition = MaterialElevationScale(true).apply {
                duration = resources.getInteger(R.integer.motion_duration_large).toLong()
            }
        }
        navController.navigate(R.id.about_fragment)
    }

    private fun navigateToPlaying() {
        bottomNavDrawer.close()
        currentNavigationFragment?.apply {
            exitTransition = null
            reenterTransition = null
        }
        navController.navigate(
            PlayingFragmentDirections.mainToPlaying(),
            navOptions
        )
    }

    @MenuRes
    private fun getBottomAppBarMenuForDestination(): Int {
        val dest = navController.currentDestination!!
        return when (dest.id) {
            R.id.detail_fragment -> R.menu.bottom_app_bar_detail_menu
            else -> R.menu.bottom_app_bar_home_menu
        }
    }

    private fun setBottomAppBarForHome() {
        binding.run {
            fab.setImageState(intArrayOf(-android.R.attr.state_activated), true)
            bottomAppBar.visibility = View.VISIBLE
            bottomAppBarTitle.visibility = View.VISIBLE
            bottomAppBar.performShow()
            fab.show()
        }
    }

    private fun setBottomAppBarForDetail() {
        binding.run {
            fab.setImageState(intArrayOf(android.R.attr.state_activated), true)
            bottomAppBar.visibility = View.VISIBLE
            bottomAppBarTitle.visibility = View.VISIBLE
            bottomAppBar.performShow()
            fab.show()
        }
    }

    private fun hideBottomAppBar() {
        binding.run {
            bottomAppBar.performHide()
            bottomAppBar.animate().setListener(object : AnimatorListenerAdapter() {
                var isCanceled = false
                override fun onAnimationEnd(animation: Animator?) {
                    if (isCanceled) return

                    bottomAppBar.visibility = View.GONE
                    fab.visibility = View.INVISIBLE
                }

                override fun onAnimationCancel(animation: Animator?) {
                    isCanceled = true
                }
            })
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_search -> navigateToSearch()
            R.id.menu_settings -> navigateToSettings()
            R.id.menu_filters -> ListSettingsFragment()
                .show(supportFragmentManager, null)
        }
        return true
    }

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        binding.bottomAppBar.replaceMenu(getBottomAppBarMenuForDestination())
        when (destination.id) {
            R.id.search_fragment, R.id.settings_fragment, R.id.playing_fragment -> setBottomAppBarForSearch()
            R.id.detail_fragment -> setBottomAppBarForDetail()
            else -> {
                setBottomAppBarForHome()
            }
        }
    }
}