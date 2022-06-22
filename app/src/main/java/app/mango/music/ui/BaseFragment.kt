package app.mango.music.ui

import android.app.Activity.RESULT_OK
import android.app.RecoverableSecurityException
import android.content.IntentSender
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.*
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import app.mango.music.R
import app.mango.music.adapters.MusicAdapter
import app.mango.music.data.MusicFavorite
import app.mango.music.service.SongBrowser
import app.mango.music.ui.main.MainActivity
import app.mango.music.viewmodel.MusicViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

abstract class BaseFragment<T:ViewDataBinding>:Fragment() ,MusicAdapter.ItemListener,CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.IO

    interface ControlListener{
        fun togglePlay()
        fun next()
        fun previous()
    }

    abstract fun setBinding():T
    open fun createViewBefore(){}
    protected lateinit var binding:T
    protected val musicViewModel: MusicViewModel by activityViewModels()
    protected val musicAdapter by lazy { MusicAdapter(this) }

    protected val navController:NavController
    get() = findNavController()

    protected val mediaBrowser: SongBrowser
    get() = (requireActivity() as MainActivity).mSongBrowser

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = setBinding().also {
        binding = it
        createViewBefore()
    }.root

    protected fun <T:View> BottomSheetBehavior<T>.addBottomSheetCallback(onStateChanged:((View, Int)->Unit), onSlide:((View, Float)->Unit)? = null){
        addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback(){
            override fun onStateChanged(bottomSheet: View, newState: Int) { onStateChanged.invoke(bottomSheet,newState)}
            override fun onSlide(bottomSheet: View, slideOffset: Float) { onSlide?.invoke(bottomSheet,slideOffset) }
        })
    }

    inline fun repeatWithViewLifecycle(
        minState: Lifecycle.State = Lifecycle.State.STARTED,
        crossinline block: suspend CoroutineScope.() -> Unit
    ) {
        if (minState == Lifecycle.State.INITIALIZED || minState == Lifecycle.State.DESTROYED) {
            throw IllegalArgumentException("minState must be between INITIALIZED and DESTROYED")
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(minState) {
                block()
            }
        }
    }

    protected fun RecyclerView.itemTouchHelper(){
        ItemTouchHelper(ReboundingSwipeActionCallback())
            .attachToRecyclerView(this)
    }

    protected fun RecyclerView.scrollVisibility(top: View?=null,bottom:View?=null){
        addOnScrollListener(object :RecyclerView.OnScrollListener(){
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(-1))
                    top?.visibility = View.INVISIBLE
                else top?.visibility = View.VISIBLE

                if (!recyclerView.canScrollVertically(1))
                    bottom?.visibility = View.INVISIBLE
                else bottom?.visibility = View.VISIBLE
            }
        })
    }

    override fun onClicked(view: View, index: Int,items: List<MediaItem>) {
        launch(Dispatchers.Main) {
            mediaBrowser.browser?.apply {
                clearMediaItems()
                setMediaItems(items)
                seekToDefaultPosition(index)
                prepare()
                play()
            }
        }
    }

    override fun onFavoriteChanged(mediaId: String, newValue: Boolean) {
        musicViewModel.updateFavoriteState(
            MusicFavorite(
                mediaId = mediaId,
                isFavorite = newValue
            )
        )
    }

    override fun onNextChanged(mediaId: String) {
        if(javaClass.simpleName==PlaylistSheetFragment::class.java.simpleName){
            mediaBrowser.removeById(mediaId)
        }
        else {
            mediaBrowser.addToNext(mediaId)
            Toast.makeText(requireContext(), R.string.music_add_next, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDelete(mediaItem: MediaItem) {
        AlertDialog.Builder(requireContext()).apply {
            setMessage(getString(R.string.show_delete_dialog,"${mediaItem.mediaMetadata.title}"))
            setPositiveButton(R.string.show_delete_dialog_pos){_,_->
                performDeleteImage(mediaItem)
            }
            setNegativeButton(R.string.cancel,null)
            show()
        }
    }

    private val permissionDeleteResult =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            if(it.resultCode== RESULT_OK && pendingDeleteImage!=null){
                performDeleteImage(pendingDeleteImage!!)
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        permissionNeededForDelete.observe(viewLifecycleOwner, Observer { intentSender ->
            intentSender?.let {
                permissionDeleteResult.launch(
                    IntentSenderRequest
                        .Builder(intentSender)
                        .build()
                )
            }
        })
    }

    private var pendingDeleteImage: MediaItem? = null
    private val _permissionNeededForDelete = MutableLiveData<IntentSender?>()
    private val permissionNeededForDelete: LiveData<IntentSender?> = _permissionNeededForDelete

    private fun performDeleteImage(media: MediaItem) {
        launch(Dispatchers.IO) {
            try {
                requireActivity().application.contentResolver.delete(
                    media.mediaMetadata.mediaUri!!,
                    "${MediaStore.Audio.Media._ID} = ?",
                    arrayOf(media.mediaId)
                )
            } catch (securityException: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val recoverableSecurityException =
                        securityException as? RecoverableSecurityException
                            ?: throw securityException
                    pendingDeleteImage = media
                    _permissionNeededForDelete.postValue(
                        recoverableSecurityException.userAction.actionIntent.intentSender
                    )
                } else {
                    throw securityException
                }
            }
        }
    }
}