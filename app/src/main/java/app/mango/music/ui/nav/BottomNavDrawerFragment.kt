package app.mango.music.ui.nav

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.LinearLayoutCompat
import app.mango.music.databinding.FragmentBottomNavDrawerBinding
import app.mango.music.ui.BaseFragment
import app.mango.music.ui.nav.view.NavigationAdapter
import app.mango.music.ui.nav.view.NavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior

class BottomNavDrawerFragment : BaseFragment<FragmentBottomNavDrawerBinding>() {

    override fun setBinding(): FragmentBottomNavDrawerBinding =
        FragmentBottomNavDrawerBinding.inflate(layoutInflater)

    private val behavior: BottomSheetBehavior<LinearLayoutCompat> by lazy(LazyThreadSafetyMode.NONE) {
        BottomSheetBehavior.from(binding.backgroundContainer)
    }
    private val bottomSheetCallback = BottomNavigationDrawerCallback()

    private var navEvent:((Int,Int,Int)->Unit)? = null
    private val navigationListeners = object : NavigationView.OnItemClickListener{
        override fun onItemClick(item: NavigationAdapter.Items) {
            navEvent?.invoke(item.id, item.title,item.icon)
            close()
        }
    }

    private val closeDrawerOnBackPressed = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            close()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this, closeDrawerOnBackPressed)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.run {
            toolbar.setNavigationOnClickListener { close() }
            nav.setOnItemClickListener(navigationListeners)
            root.setOnTouchListener { _, motionEvent ->
                when(motionEvent.action){
                    MotionEvent.ACTION_DOWN -> {
                        if(behavior.state!=BottomSheetBehavior.STATE_HIDDEN) {
                            close()
                            true
                        }else false
                    }
                    else -> false
                }
            }
            bottomSheetCallback.apply {
                /*addOnSlideAction(ForegroundSheetTransformSlideAction(
                binding.foregroundContainer,
                backgroundShapeDrawable,
                binding.profileImageView
            ))*/
                // Scrim view transforms
                /*addOnSlideAction(AlphaSlideAction(scrimView))
            addOnStateChangedAction(VisibilityStateAction(scrimView))
            // Foreground transforms

            // Recycler transforms
            addOnStateChangedAction(ScrollToTopStateAction(navRecyclerView))
            // Close the sandwiching account picker if open
            addOnStateChangedAction(object : OnStateChangedAction {
                override fun onStateChanged(sheet: View, newState: Int) {
                    sandwichAnim?.cancel()
                    sandwichProgress = 0F
                }
            })*/
                // If the drawer is open, pressing the system back button should close the drawer.
                addOnStateChangedAction(object : OnStateChangedAction {
                    override fun onStateChanged(sheet: View, newState: Int) {
                        closeDrawerOnBackPressed.isEnabled =
                            newState != BottomSheetBehavior.STATE_HIDDEN
                    }
                })
            }
        }

        behavior.addBottomSheetCallback(bottomSheetCallback)
        behavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    fun close() {
        behavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    private fun open() {
        behavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
    }

    fun toggle(){
        if(behavior.state!=BottomSheetBehavior.STATE_HIDDEN) close()
        else open()
    }

    fun addOnSlideAction(action: OnSlideAction) {
        bottomSheetCallback.addOnSlideAction(action)
    }

    fun addOnStateChangedAction(action: OnStateChangedAction) {
        bottomSheetCallback.addOnStateChangedAction(action)
    }

    fun addNavigationListener(unit:((Int,Int,Int)->Unit)) {
        navEvent = unit
    }

}