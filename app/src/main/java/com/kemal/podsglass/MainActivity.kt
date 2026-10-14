package com.kemal.podsglass

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { PodsTheme { HomeScreen() } }
    }

    override fun onResume() {
        super.onResume()
        if (hasCorePerms()) PodsService.start(this)
    }
}

fun allPerms(): Array<String> = buildList {
    add(Manifest.permission.BLUETOOTH_SCAN)
    add(Manifest.permission.BLUETOOTH_CONNECT)
    add(Manifest.permission.ACCESS_FINE_LOCATION)
    add(Manifest.permission.ACCESS_COARSE_LOCATION)
    if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
}.toTypedArray()

fun Context.hasCorePerms() = listOf(
    Manifest.permission.BLUETOOTH_SCAN,
    Manifest.permission.BLUETOOTH_CONNECT,
    Manifest.permission.ACCESS_FINE_LOCATION
).all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

fun timeAgo(ms: Long): String {
    val s = ms / 1000
    return when {
        s < 60 -> "az önce"
        s < 3600 -> "${s / 60} dk önce"
        s < 86400 -> "${s / 3600} sa önce"
        else -> "${s / 86400} gün önce"
    }
}

@Composable
fun HomeScreen() {
    val ctx = LocalContext.current
    val accent = accentColor()
    val status by PodsState.status.collectAsState()
    val connected by PodsState.connected.collectAsState()
    val name by Prefs.name.collectAsState()

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(2000) } }

    var permsOk by remember { mutableStateOf(ctx.hasCorePerms()) }
    var overlayOk by remember { mutableStateOf(Settings.canDrawOverlays(ctx)) }
    var batteryOk by remember { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        permsOk = ctx.hasCorePerms()
        overlayOk = Settings.canDrawOverlays(ctx)
        batteryOk = ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)
        onPauseOrDispose { }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permsOk = ctx.hasCorePerms()
        if (permsOk) PodsService.start(ctx)
    }

    val nearby = status != null && now - status!!.timestamp < 15_000
    val chip = when {
        connected -> "Bağlı" to Color(0xFF30D158)
        nearby -> "Yakında" to accent
        else -> "Uzakta" to Color.White.copy(alpha = 0.5f)
    }

    GlassBackground(accent) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("PodsGlass", fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f))
                    Text(name, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                }
                StatusChip(chip.first, chip.second)
            }

            // ---- Ana kart: illüstrasyon + pil halkaları
            GlassCard(Modifier.fillMaxWidth(), corner = 34.dp) {
                PodsArt(Modifier.fillMaxWidth().height(210.dp))
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    BatteryRing("Sol", status?.left, status?.leftCharging == true, accent)
                    BatteryRing("Sağ", status?.right, status?.rightCharging == true, accent)
                    BatteryRing("Kutu", status?.case, status?.caseCharging == true, accent)
                }
                if (!nearby) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Kutunun kapağını telefonun yanında aç",
                        Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            // ---- Kurulum
            if (!permsOk || !overlayOk || !batteryOk) {
                GlassCard(Modifier.fillMaxWidth()) {
                    CardTitle("⚙️  Kurulum")
                    SetupRow("Bluetooth ve konum izni", permsOk) { launcher.launch(allPerms()) }
                    SetupRow("Açılır pencere izni", overlayOk) {
                        ctx.startActivity(
                            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))
                        )
                    }
                    SetupRow("Pil kısıtlamasını kaldır", batteryOk) {
                        ctx.startActivity(
                            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}"))
                        )
                    }
                }
            }

            // ---- Son görülen konum
            val loc by Prefs.lastLoc.collectAsState()
            GlassCard(Modifier.fillMaxWidth()) {
                CardTitle("📍  Son Görülen Konum")
                Text(
                    loc?.let { "${timeAgo(now - it.time)} · %.5f, %.5f".format(it.lat, it.lng) }
                        ?: "Henüz kayıt yok. Kulaklık bağlantısı koptuğunda konum otomatik kaydedilir.",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.75f)
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassButton("Haritada Aç", accent, primary = true, enabled = loc != null) {
                        loc?.let {
                            val uri = Uri.parse("geo:${it.lat},${it.lng}?q=${it.lat},${it.lng}(AirPods)")
                            ctx.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        }
                    }
                    GlassButton("Şimdi Kaydet", accent) {
                        captureLocation(ctx) { ok ->
                            Toast.makeText(ctx, if (ok) "Konum kaydedildi" else "Konum alınamadı", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            // ---- Kişiselleştirme
            GlassCard(Modifier.fillMaxWidth()) {
                CardTitle("🎨  Kişiselleştir")
                OutlinedTextField(
                    value = name,
                    onValueChange = { Prefs.setName(it) },
                    label = { Text("Kulaklık adı") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = accent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = accent,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                        cursorColor = accent,
                    )
                )
                Spacer(Modifier.height(16.dp))
                Text("Vurgu rengi", fontSize = 14.sp, color = Color.White.copy(alpha = 0.75f))
                Spacer(Modifier.height(8.dp))
                val sel by Prefs.accent.collectAsState()
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Accents.forEachIndexed { i, c ->
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(if (i == sel) 3.dp else 0.dp, Color.White, CircleShape)
                                .clickable { Prefs.setAccent(i) }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                val glass by Prefs.glass.collectAsState()
                Text("Cam yoğunluğu", fontSize = 14.sp, color = Color.White.copy(alpha = 0.75f))
                Slider(value = glass, onValueChange = { Prefs.setGlass(it) }, valueRange = 0.04f..0.32f)
                val popup by Prefs.popup.collectAsState()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Kutu açılınca pencere göster", Modifier.weight(1f), fontSize = 15.sp)
                    Switch(checked = popup, onCheckedChange = { Prefs.setPopup(it) })
                }
                Spacer(Modifier.height(10.dp))
                GlassButton("Açılır Pencereyi Dene", accent, primary = true, modifier = Modifier.fillMaxWidth()) {
                    ctx.startActivity(Intent(ctx, PopupActivity::class.java))
                }
            }

            // ---- Kontroller
            GlassCard(Modifier.fillMaxWidth()) {
                CardTitle("👆  Sap Kontrolleri")
                listOf(
                    "1 kez bas" to "Oynat / Duraklat, aramayı aç",
                    "2 kez bas" to "Sonraki parça",
                    "3 kez bas" to "Önceki parça",
                    "Basılı tut" to "Gürültü Engelleme ↔ Şeffaf mod",
                    "Kaydır" to "Ses aç / kıs",
                ).forEach { (g, a) ->
                    Row(Modifier.padding(vertical = 5.dp)) {
                        Text(g, Modifier.width(110.dp), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(a, fontSize = 14.sp, color = Color.White.copy(alpha = 0.75f))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Bu hareketler kulaklığın kendi içinde çalışır, Samsung'da da aktif. " +
                        "Basılı tutma hareketini değiştirmek ve telefonu Apple gibi tanıtmak root gerektirir.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.55f)
                )
                Spacer(Modifier.height(12.dp))
                GlassButton("Bluetooth Ayarları", accent, modifier = Modifier.fillMaxWidth()) {
                    ctx.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun CardTitle(t: String) {
    Text(t, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(10.dp))
}

@Composable
fun StatusChip(text: String, color: Color) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.18f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SetupRow(label: String, ok: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(if (ok) "✅" else "⚪️", fontSize = 16.sp)
        Spacer(Modifier.width(10.dp))
        Text(label, Modifier.weight(1f), fontSize = 15.sp)
        if (!ok) GlassButton("Aç", accentColor(), primary = true, onClick = onClick)
    }
}

@Composable
fun GlassButton(
    text: String,
    accent: Color,
    primary: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) = GlassButtonImpl(text, accent, primary, enabled, modifier, onClick)

@Composable
private fun GlassButtonImpl(
    text: String,
    accent: Color,
    primary: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .clip(shape)
            .background(if (primary) accent.copy(alpha = if (enabled) 1f else 0.35f) else Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White.copy(alpha = if (enabled) 1f else 0.6f))
    }
}
