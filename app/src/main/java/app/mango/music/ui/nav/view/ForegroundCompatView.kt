package app.mango.music.ui.nav.view

import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import androidx.annotation.RestrictTo

interface ForegroundCompatView {
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun getSupportForeground(): Drawable?

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun setSupportForeground(foreground: Drawable?)

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun getSupportForegroundGravity(): Int

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun setSupportForegroundGravity(gravity: Int)

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun setSupportForegroundTintList(tint: ColorStateList?)

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun getSupportForegroundTintList(): ColorStateList?

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun setSupportForegroundTintMode(tintMode: PorterDuff.Mode?)

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun getSupportForegroundTintMode(): PorterDuff.Mode?
}