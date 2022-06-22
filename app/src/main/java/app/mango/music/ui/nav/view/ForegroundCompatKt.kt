package app.mango.music.ui.nav.view

import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.widget.FrameLayout

fun getForeground(view: View): Drawable? {
    return if (view is FrameLayout) {
        view.foreground
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        && isTargetingMOrAbove(view)) {
        view.foreground
    } else if (view is ForegroundCompatView) {
        (view as ForegroundCompatView).getSupportForeground()
    } else {
        null
    }
}

fun setForeground(view: View, foreground: Drawable?) {
    foreground?:return
    if (view is FrameLayout) {
        view.foreground = foreground
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isTargetingMOrAbove(view)) {
        view.foreground = foreground
    } else if (view is ForegroundCompatView) {
        (view as ForegroundCompatView).setSupportForeground(foreground)
    }
}

private fun isTargetingMOrAbove(view: View): Boolean {
    return view.context.applicationInfo.targetSdkVersion >= Build.VERSION_CODES.M
}