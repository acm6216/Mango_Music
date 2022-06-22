package app.mango.music.ui

import android.os.Bundle
import android.view.View
import app.mango.music.adapters.ViewPagerAdapter
import app.mango.music.data.GlobalData
import app.mango.music.databinding.FragmentDisplayBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class DisplayFragment:BaseFragment<FragmentDisplayBinding>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewAdapter = ViewPagerAdapter(binding.root.context){
            mediaBrowser.browser?.seekTo(it)
            if(mediaBrowser.browser?.isPlaying != true)
                mediaBrowser.togglePlay()
        }
        binding.viewPager.adapter = viewAdapter
        binding.indicator.setViewPager(binding.viewPager)

        repeatWithViewLifecycle {
            launch {
                GlobalData.currentMediaItem.collect {
                    it?.let {
                        viewAdapter.updateMediaItem(it)
                    }
                }
            }
            launch {
                GlobalData.currentPosition.collect {
                    viewAdapter.updatePosition(it)
                }
            }
        }

    }

    override fun setBinding(): FragmentDisplayBinding = FragmentDisplayBinding.inflate(layoutInflater)
}