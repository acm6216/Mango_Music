package app.mango.music.ui

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import app.mango.music.R
import app.mango.music.databinding.FragmentSearchBinding
import app.mango.music.manager.SpManager
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SearchFragment:BaseFragment<FragmentSearchBinding>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply {
            duration = resources.getInteger(R.integer.motion_duration_large).toLong()
        }
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).apply {
            duration = resources.getInteger(R.integer.motion_duration_large).toLong()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.searchToolbar.setNavigationOnClickListener {
            navController.navigateUp()
        }
        binding.viewmodel = musicViewModel
        binding.lifecycleOwner = this
        binding.executePendingBindings()
        binding.recyclerView.run {
            adapter = musicAdapter
            ThemedFastScroller.create(this)
            SpManager.listen(getString(R.string.key_set_list_filter_grid),
                SpManager.SpIntListener(getString(R.string.set_list_filter_grid_def).toInt()) {
                    layoutManager = GridLayoutManager(context,it)
                })
            itemTouchHelper()
            scrollVisibility(binding.divider)
        }

        repeatWithViewLifecycle {
            launch {
                musicViewModel.searchSongs.collect {
                    musicAdapter.submitList(it)
                }
            }
        }
    }

    override fun setBinding(): FragmentSearchBinding = FragmentSearchBinding.inflate(layoutInflater)

}