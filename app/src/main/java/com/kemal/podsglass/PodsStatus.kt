package com.kemal.podsglass

import kotlinx.coroutines.flow.MutableStateFlow

data class PodsStatus(
    val left: Int?,          // 0–100, null = bilinmiyor / kutuda değil
    val right: Int?,
    val case: Int?,
    val leftCharging: Boolean,
    val rightCharging: Boolean,
    val caseCharging: Boolean,
    val rssi: Int,
    val timestamp: Long,
)

object PodsState {
    val status = MutableStateFlow<PodsStatus?>(null)
    val connected = MutableStateFlow(false)
}

/**
 * AirPods, kutu açıldığında ve kullanımdayken Apple'ın "proximity pairing"
 * BLE yayınını (üretici kimliği 0x004C, tip 0x07) gönderir. Bu yayındaki
 * pil ve şarj alanları şifresiz olduğu için root olmadan okunabilir.
 */
object AppleProximityParser {
    fun parse(data: ByteArray, rssi: Int): PodsStatus? {
        if (data.size < 9 || data[0] != 0x07.toByte()) return null
        val hex = data.joinToString("") { "%02x".format(it) }
        return try {
            val flip = (hex[10].digitToInt(16) and 0x02) == 0
            val l = hex[if (flip) 12 else 13].digitToInt(16)
            val r = hex[if (flip) 13 else 12].digitToInt(16)
            val c = hex[15].digitToInt(16)
            val chg = hex[14].digitToInt(16)
            PodsStatus(
                left = pct(l),
                right = pct(r),
                case = pct(c),
                leftCharging = (chg and (if (flip) 0b10 else 0b01)) != 0,
                rightCharging = (chg and (if (flip) 0b01 else 0b10)) != 0,
                caseCharging = (chg and 0b100) != 0,
                rssi = rssi,
                timestamp = System.currentTimeMillis(),
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun pct(v: Int): Int? = if (v in 0..10) v * 10 else null
}
