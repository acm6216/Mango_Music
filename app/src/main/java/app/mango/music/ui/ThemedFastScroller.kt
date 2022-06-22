package app.mango.music.ui

import android.view.ViewGroup
import me.zhanghai.android.fastscroll.FastScroller
import me.zhanghai.android.fastscroll.FastScrollerBuilder

object ThemedFastScroller {
    fun create(view: ViewGroup): FastScroller
        = FastScrollerBuilder(view)
            .useMd2Style()
            .build()
}
