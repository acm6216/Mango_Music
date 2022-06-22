package app.mango.music.manager

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer

class AudioEffectManager constructor(
    audioSessionId: Int,
    initialState: Boolean = true
) {
    val equalizer = XEqualizer(Integer.MAX_VALUE, audioSessionId)

    val bassBoost = XBassBoost(Integer.MAX_VALUE, audioSessionId)

    init {
        bassBoost.enabled = initialState
        equalizer.enabled = initialState
    }

    fun release() {
        equalizer.release()
        bassBoost.release()
    }

    inner class XEqualizer(priority: Int, audioSessionId: Int) : Equalizer(priority, audioSessionId)

    inner class XBassBoost(priority: Int, audioSessionId: Int) :
        BassBoost(priority, audioSessionId) {
        private val maxRecommendedStrength = 19

        override fun setStrength(strength: Short) {
            super.setStrength((1000F / maxRecommendedStrength * strength).toInt().toShort())
        }

        override fun getRoundedStrength(): Short {
            return (super.getRoundedStrength() / (1000F / maxRecommendedStrength)).toInt().toShort()
        }
    }
}
