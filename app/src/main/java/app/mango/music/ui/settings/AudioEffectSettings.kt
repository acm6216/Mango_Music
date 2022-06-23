package app.mango.music.ui.settings

import android.media.audiofx.Equalizer
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.media3.exoplayer.ExoPlayer
import androidx.preference.*
import app.mango.music.R
import app.mango.music.manager.Config
import app.mango.music.manager.SpManager

class AudioEffectSettings:BasePreferenceFragment() {

    private val seekbars = mutableListOf<BandLevel>()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.audio_effect_preferences,rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        findPreference<PreferenceCategory>(R.string.key_set_audio_effect)?.run {
            initBands(this)
        }
    }

    private fun initBands(preferenceCategory: PreferenceCategory) {

        val exoPlayer = ExoPlayer.Builder(requireActivity().applicationContext).build()
        val equalizer = Equalizer(Integer.MAX_VALUE, exoPlayer.audioSessionId)

        val numberFrequencyBands: Short = equalizer.numberOfBands
        val lowerEqualizerBandLevelMilliBel: Short = equalizer.bandLevelRange[0]
        val upperEqualizerBandLevelMilliBel: Short = equalizer.bandLevelRange[1]

        for (i in 0 until numberFrequencyBands) {
            val equalizerBandIndex = i.toShort()
            val seekbar = SeekBarPreference(preferenceCategory.context).apply {
                max = upperEqualizerBandLevelMilliBel - lowerEqualizerBandLevelMilliBel
                title = readableHertz(
                    equalizer.getCenterFreq(equalizerBandIndex)
                )
                key = "${getString(R.string.key_set_audio_effect_enable)}_$i"
                isIconSpaceReserved = false
                showSeekBarValue = true
                this.onPreferenceChangeListener =
                    Preference.OnPreferenceChangeListener { _, newValue ->
                        setBandLevel(
                            i,
                            (newValue.toString().toInt() + lowerEqualizerBandLevelMilliBel).toShort(),
                            numberFrequencyBands,
                            max
                        )
                        true
                    }
            }
            val bandLevel = BandLevel(
                seekbar,
                equalizerBandIndex,
                lowerEqualizerBandLevelMilliBel,
                upperEqualizerBandLevelMilliBel
            )
            setProgress(equalizer,bandLevel)
            seekbars.add(bandLevel)
            preferenceCategory.addPreference(seekbar)
        }

        preferenceCategory.addPreference(Preference(preferenceCategory.context).apply {
            title = getString(R.string.set_audio_effect_reset)
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                resetSeekbar(preferenceCategory)
                true
            }
            isIconSpaceReserved = false
        })

        initPresets(preferenceCategory,equalizer){
            updateSeekBars(equalizer)
        }

        initSeekBersEnable(preferenceCategory)
        exoPlayer.release()
    }

    private fun initPresets(preferenceCategory: PreferenceCategory, equalizer: Equalizer, unit:((Short)->Unit)){

        val presetsCount = equalizer.numberOfPresets
        val displayValue = Array<CharSequence>(presetsCount.toInt()){ "" }

        for (i in 0 until presetsCount) {
            val preset = i.toShort()
            displayValue[i] = equalizer.getPresetName(preset)
        }
        preferenceCategory.addPreference(ListPreference(preferenceCategory.context).apply {
            title = getString(R.string.set_audio_effect_presets)
            key = getString(R.string.key_set_audio_effect_presets)
            entries = displayValue
            entryValues = displayValue
            isIconSpaceReserved = false
            onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                val dv = newValue.toString()
                val index = displayValue.indexOf(dv.subSequence(0,dv.length)).toShort()
                equalizer.usePreset(index)
                unit.invoke(index)
                true
            }
            summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
        })
    }

    private fun initSeekBersEnable(preferenceCategory: PreferenceCategory){
        preferenceCategory.addPreference(SwitchPreferenceCompat(preferenceCategory.context).apply {
            title = getString(R.string.set_audio_effect_seek_enable)
            key = getString(R.string.key_set_audio_effect_seek_enable)
            onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                setSeekBarEnable(newValue!=true)
                true
            }
            preferenceCategory.sharedPreferences?.let {
                setSeekBarEnable(
                    !it.getBoolean(key, false)
                )
            }
            isIconSpaceReserved = false
        })
    }

    private fun resetSeekbar(preferenceCategory: PreferenceCategory){
        AlertDialog.Builder(preferenceCategory.context).apply {
            setTitle(R.string.set_audio_effect_reset)
            setMessage(R.string.set_audio_effect_reset_message)
            setPositiveButton(R.string.set_audio_effect_reset_ok){ _, _ ->
                seekbars.forEach {
                    it.seekBarPreference.apply {
                        value = max / 2
                        onPreferenceChangeListener?.onPreferenceChange(this, max / 2)
                    }
                }
            }
            setNegativeButton(R.string.set_audio_effect_reset_cancel,null)
            show()
        }
    }

    private fun setSeekBarEnable(isEnable:Boolean){
        seekbars.forEach {
            it.seekBarPreference.isEnabled = isEnable
        }
    }

    private fun updateSeekBars(equalizer: Equalizer) {
        val numberFrequencyBands: Short = equalizer.numberOfBands
        for (i in 0 until numberFrequencyBands) {
            val equalizerBandIndex = i.toShort()
            updateSeekBar(equalizer,equalizerBandIndex)
        }
    }

    private fun updateSeekBar(equalizer: Equalizer, equalizerBandIndex: Short) {
        val bandLevel = seekbars[equalizerBandIndex.toInt()]
        setProgress(equalizer,bandLevel,false)
    }

    private fun setBandLevel(index:Int,value:Short,count:Short,max:Int){
        SpManager.apply {
            val oldValue = this.getString(Config.KEY_SETTINGS_AUDIO_EFFECT_VALUE,getDefArray(count,max/2)).toString()
            putString(Config.KEY_SETTINGS_AUDIO_EFFECT_VALUE,putArray(index,value,oldValue))
        }
    }

    private fun putArray(index:Int,value:Short,lastArray:String):String{
        val newValue = lastArray.split("#").map {
            val i = it.substring(it.indexOf("[")+1,it.indexOf(":")).toInt()
            if(i==index){
                val oldValue = it.substring(it.indexOf(":")+1,it.indexOf("]")+1)
                it.replace(oldValue, "$value]")
            }else it
        }
        val result = StringBuilder()
        newValue.forEach { result.append("$it#") }
        return result.substring(0,result.length-1)
    }

    private fun getDefArray(count:Short, value:Int):String{
        val result = StringBuilder()
        for (i in 0 until count){
            result.append("[$i:$value]#")
        }
        return result.substring(0,result.length-1)
    }

    private fun readableHertz(millihertz: Int): String =
        "${millihertz / 1000}Hz"

    private fun setProgress(equalizer: Equalizer, bandLevel: BandLevel, isAuto:Boolean = true) {
        val level = equalizer.getBandLevel(bandLevel.index).toInt()
        bandLevel.seekBarPreference.apply {
            value = level + bandLevel.maxBandLevel
            if(!isAuto) onPreferenceChangeListener?.onPreferenceChange(this,value)
        }
    }

    private data class BandLevel constructor(
        val seekBarPreference: SeekBarPreference,
        val index: Short,
        val lowestBandLevel: Short,
        val maxBandLevel: Short
    )
}