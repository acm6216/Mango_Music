package app.mango.music.ui.main

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.mango.music.R
import app.mango.music.manager.SpManager
import app.mango.music.mark.Watermark
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

abstract class BaseActivity<T : ViewDataBinding> : AppCompatActivity() {

    abstract fun setLayout(): T
    protected lateinit var binding: T

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(setLayout().also {
            binding = it
        }.root)

        SpManager.listen(getString(R.string.key_set_theme_color_filter),
            SpManager.SpBoolListener(false){
                Watermark.instance.saturation(window,it)
            })
    }

    protected inline fun repeatWithViewLifecycle(
        minState: Lifecycle.State = Lifecycle.State.STARTED,
        crossinline block: suspend CoroutineScope.() -> Unit
    ) {
        if (minState == Lifecycle.State.INITIALIZED || minState == Lifecycle.State.DESTROYED) {
            throw IllegalArgumentException("minState must be between INITIALIZED and DESTROYED")
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(minState) {
                block()
            }
        }
    }

    private fun getDarkModeStatus(): Boolean {
        val mode = this.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return mode == Configuration.UI_MODE_NIGHT_YES
    }

    private val activityResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result()
        }

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if(it) onSuccess?.invoke()
            else showPermissionDialog()
    }

    private fun showPermissionDialog() {
        AlertDialog.Builder(this).apply {
            setTitle(R.string.show_request_reason_dialog)
            setMessage(R.string.show_forward_to_settings_dialog)
            setPositiveButton(R.string.show_request_reason_dialog_pos) { _, _ ->
                activityResult.launch(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                )
            }
            setNegativeButton(R.string.show_request_reason_dialog_neg, null)
            show()
        }
    }

    private fun result() {
        onSuccess?.let {
            requestPermission {
                it.invoke()
            }
        }
    }

    private var onSuccess: (() -> Unit)? = null
    protected fun requestPermission(
        unit: (() -> Unit)
    ) {
        onSuccess = unit
        requestPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

}