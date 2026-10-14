package com.kemal.podsglass

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

class PodsService : Service() {

    companion object {
        private const val CHANNEL = "pods"
        private const val NOTIF_ID = 1
        fun start(ctx: Context) {
            try {
                ctx.startForegroundService(Intent(ctx, PodsService::class.java))
            } catch (_: Exception) {
            }
        }
    }

    private var scanner: BluetoothLeScanner? = null
    private var lastClose = 0L
    private var lastPopup = 0L
    private var lastText = ""

    private val scanCb = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) = handle(result)
        override fun onBatchScanResults(results: MutableList<ScanResult>) = results.forEach { handle(it) }
    }

    private val btReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            val dev: BluetoothDevice? = if (Build.VERSION.SDK_INT >= 33) {
                i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            } else {
                @Suppress("DEPRECATION") i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }
            if (dev == null || !isPods(dev)) return
            when (i.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> PodsState.connected.value = true
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    PodsState.connected.value = false
                    // Bağlantı koptuğu an telefonun konumunu "son görülen yer" olarak kaydet.
                    captureLocation(this@PodsService)
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        createChannel()
        goForeground()
        val f = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        ContextCompat.registerReceiver(this, btReceiver, f, ContextCompat.RECEIVER_EXPORTED)
        startScan()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        stopScan()
        try { unregisterReceiver(btReceiver) } catch (_: Exception) {}
        super.onDestroy()
    }

    // ---------- Bildirim / ön plan ----------

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "AirPods durumu", NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun buildNotif(text: String) = NotificationCompat.Builder(this, CHANNEL)
        .setSmallIcon(R.drawable.ic_stat)
        .setContentTitle(Prefs.name.value)
        .setContentText(text)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setSilent(true)
        .setContentIntent(
            PendingIntent.getActivity(
                this, 0, Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
        .build()

    private fun goForeground() {
        val withLoc = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val base = ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        val notif = buildNotif("AirPods aranıyor…")
        try {
            ServiceCompat.startForeground(
                this, NOTIF_ID, notif,
                if (withLoc) base or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else base
            )
        } catch (_: Exception) {
            // Arka plandan yeniden başlatılırsa konum tipi reddedilebilir.
            try { ServiceCompat.startForeground(this, NOTIF_ID, notif, base) } catch (_: Exception) {}
        }
    }

    private fun updateNotif(s: PodsStatus) {
        fun f(v: Int?, c: Boolean) = if (v == null) "—" else (if (c) "⚡" else "") + "%$v"
        val text = "Sol ${f(s.left, s.leftCharging)} · Sağ ${f(s.right, s.rightCharging)} · Kutu ${f(s.case, s.caseCharging)}"
        if (text == lastText) return
        lastText = text
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotif(text))
    }

    // ---------- BLE tarama ----------

    @SuppressLint("MissingPermission")
    private fun startScan() {
        val adapter = getSystemService(BluetoothManager::class.java)?.adapter ?: return
        scanner = adapter.bluetoothLeScanner ?: return
        val filter = ScanFilter.Builder()
            .setManufacturerData(76, byteArrayOf(0x07), byteArrayOf(-1))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .setReportDelay(0)
            .build()
        try {
            scanner?.startScan(listOf(filter), settings, scanCb)
        } catch (_: SecurityException) {
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopScan() {
        try { scanner?.stopScan(scanCb) } catch (_: Exception) {}
    }

    private fun handle(r: ScanResult) {
        val data = r.scanRecord?.getManufacturerSpecificData(76) ?: return
        val s = AppleProximityParser.parse(data, r.rssi) ?: return
        onStatus(s)
    }

    private fun onStatus(s: PodsStatus) {
        val now = System.currentTimeMillis()
        val prev = PodsState.status.value
        // Etraftaki başka AirPods'ları elemek için zayıf sinyalleri yok say.
        if (s.rssi < -75) return
        if (prev != null && now - prev.timestamp < 3000 && s.rssi < prev.rssi - 10) return

        PodsState.status.value = s
        updateNotif(s)

        if (s.rssi >= -60) {
            // 20 sn görünmeyip yeniden çok yakında beliren = kutu kapağı az önce açıldı.
            val fresh = now - lastClose > 20_000
            if (fresh && now - lastPopup > 45_000 && Prefs.popup.value && Settings.canDrawOverlays(this)) {
                lastPopup = now
                startActivity(
                    Intent(this, PopupActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                )
            }
            lastClose = now
        }
    }

    private fun isPods(dev: BluetoothDevice): Boolean = try {
        @SuppressLint("MissingPermission")
        val n = dev.name ?: ""
        n.contains("AirPods", ignoreCase = true) || n == Prefs.name.value
    } catch (_: SecurityException) {
        false
    }
}

/** Telefonun o anki konumunu alır ve "son görülen konum" olarak kaydeder. */
@SuppressLint("MissingPermission")
fun captureLocation(ctx: Context, onDone: ((Boolean) -> Unit)? = null) {
    val fine = ctx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ctx.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!fine && !coarse) { onDone?.invoke(false); return }

    val lm = ctx.getSystemService(LocationManager::class.java)
    val providers = listOf(LocationManager.FUSED_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
    val provider = providers.firstOrNull { lm.hasProvider(it) && lm.isProviderEnabled(it) }
    if (provider == null) { onDone?.invoke(false); return }

    try {
        lm.getCurrentLocation(provider, null, ctx.mainExecutor) { loc ->
            val l = loc ?: providers
                .mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
                .maxByOrNull { it.time }
            if (l != null) {
                Prefs.saveLocation(SavedLocation(l.latitude, l.longitude, System.currentTimeMillis(), l.accuracy))
            }
            onDone?.invoke(l != null)
        }
    } catch (_: Exception) {
        onDone?.invoke(false)
    }
}
