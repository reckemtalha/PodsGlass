package com.kemal.podsglass

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
    }
}

data class SavedLocation(val lat: Double, val lng: Double, val time: Long, val accuracy: Float)

/** Kullanıcı ayarları – hepsi canlı (StateFlow) olarak arayüze yansır. */
object Prefs {
    private var sp: SharedPreferences? = null

    val accent = MutableStateFlow(0)
    val glass = MutableStateFlow(0.14f)
    val popup = MutableStateFlow(true)
    val name = MutableStateFlow("AirPods Pro 3")
    val lastLoc = MutableStateFlow<SavedLocation?>(null)

    fun init(ctx: Context) {
        if (sp != null) return
        val p = ctx.applicationContext.getSharedPreferences("podsglass", Context.MODE_PRIVATE)
        sp = p
        accent.value = p.getInt("accent", 0)
        glass.value = p.getFloat("glass", 0.14f)
        popup.value = p.getBoolean("popup", true)
        name.value = p.getString("name", "AirPods Pro 3") ?: "AirPods Pro 3"
        if (p.contains("lat")) {
            lastLoc.value = SavedLocation(
                Double.fromBits(p.getLong("lat", 0)),
                Double.fromBits(p.getLong("lng", 0)),
                p.getLong("time", 0),
                p.getFloat("acc", 0f)
            )
        }
    }

    fun setAccent(v: Int) { accent.value = v; sp?.edit()?.putInt("accent", v)?.apply() }
    fun setGlass(v: Float) { glass.value = v; sp?.edit()?.putFloat("glass", v)?.apply() }
    fun setPopup(v: Boolean) { popup.value = v; sp?.edit()?.putBoolean("popup", v)?.apply() }
    fun setName(v: String) { name.value = v; sp?.edit()?.putString("name", v)?.apply() }

    fun saveLocation(l: SavedLocation) {
        lastLoc.value = l
        sp?.edit()
            ?.putLong("lat", l.lat.toRawBits())
            ?.putLong("lng", l.lng.toRawBits())
            ?.putLong("time", l.time)
            ?.putFloat("acc", l.accuracy)
            ?.apply()
    }
}
