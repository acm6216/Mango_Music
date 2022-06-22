package app.mango.music.utils

import kotlinx.coroutines.flow.SharingStarted

val WhileViewSubscribed = SharingStarted.WhileSubscribed(5_000)