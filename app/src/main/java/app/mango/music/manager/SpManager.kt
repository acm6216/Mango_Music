package app.mango.music.manager

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import kotlin.collections.set

object SpManager {
    abstract class SpListener<T>(val callback: (T) -> Unit) {
        fun onUpdate(newValue: T) = callback(newValue)
    }

    class SpBoolListener(
        val defaultValue: Boolean = false,
        callback: (Boolean) -> Unit
    ) : SpListener<Boolean>(callback)

    class SpFloatListener(
        val defaultValue: Float = 0f,
        callback: (Float) -> Unit
    ) : SpListener<Float>(callback)

    class SpIntListener(
        val defaultValue: Int = 0,
        callback: (Int) -> Unit
    ) : SpListener<Int>(callback)

    class SpStringListener(
        val defaultValue: String = "",
        callback: (String) -> Unit
    ) : SpListener<String>(callback)

    private lateinit var sp: SharedPreferences
    private val listeners: LinkedHashMap<String, SpListener<out Any>> = linkedMapOf()

    fun init(context: Context) {
        sp = PreferenceManager.getDefaultSharedPreferences(context).also {
            it.registerOnSharedPreferenceChangeListener { sp, key ->
                update(sp, key)
            }
        }
    }

    fun putString(key:String,value:String){
        sp.edit()?.putString(key,value)?.apply()
    }
    fun putInt(key:String,value:Int){
        sp.edit()?.putInt(key,value)?.apply()
    }

    fun getString(key: String,def:String):String? = sp.getString(key,def)

    private fun update(sp: SharedPreferences?, key: String) {
        listeners[key]?.let {
            when (it) {
                is SpBoolListener -> it.onUpdate(sp?.getBoolean(key, it.defaultValue) ?: it.defaultValue)
                is SpFloatListener -> it.onUpdate(sp?.getFloat(key, it.defaultValue) ?: it.defaultValue)
                is SpIntListener -> it.onUpdate(sp?.getInt(key, it.defaultValue) ?: it.defaultValue)
                is SpStringListener -> it.onUpdate(sp?.getString(key, it.defaultValue) ?: it.defaultValue)
            }
        }
    }

    fun <K : Any, T : SpListener<K>> listen(key: String, listener: T) {
        listeners[key] = listener
        update(sp, key)
    }

}