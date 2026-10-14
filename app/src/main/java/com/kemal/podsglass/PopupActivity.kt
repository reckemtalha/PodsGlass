package com.kemal.podsglass

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** iPhone'daki gibi alttan yaylanarak çıkan bağlantı penceresi. */
class PopupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        // Samsung One UI destekliyorsa arkadaki ekranı gerçekten bulanıklaştır (cam efekti).
        if (windowManager.isCrossWindowBlurEnabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.apply { blurBehindRadius = 60 }
        }
        setShowWhenLocked(true)
        setContent {
            PodsTheme {
                PopupSheet {
                    finish()
                    @Suppress("DEPRECATION") overridePendingTransition(0, 0)
                }
            }
        }
    }
}

@Composable
fun PopupSheet(onClose: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val status by PodsState.status.collectAsState()
    val name by Prefs.name.collectAsState()
    val accent = accentColor()

    fun close() {
        scope.launch {
            visible = false
            delay(350)
            onClose()
        }
    }

    LaunchedEffect(Unit) {
        visible = true
        delay(250)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        delay(8000)
        close()
    }

    val dim by animateFloatAsState(if (visible) 0.35f else 0f, tween(300), label = "dim")
    val noRipple = remember { MutableInteractionSource() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = dim))
            .clickable(interactionSource = noRipple, indication = null) { close() },
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(spring(dampingRatio = 0.72f, stiffness = 320f)) { it } +
                fadeIn(tween(200)),
            exit = slideOutVertically(tween(300)) { it } + fadeOut(tween(250))
        ) {
            val shape = RoundedCornerShape(40.dp)
            Column(
                Modifier
                    .navigationBarsPadding()
                    .padding(10.dp)
                    .fillMaxWidth()
                    .clip(shape)
                    .background(
                        Brush.verticalGradient(listOf(Color(0xCC2A2B33), Color(0xE6141519)))
                    )
                    .border(
                        1.dp,
                        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.4f), Color.White.copy(alpha = 0.05f))),
                        shape
                    )
                    .clickable(interactionSource = noRipple, indication = null) { }
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(
                    if (status == null) "Aranıyor…" else "Hazır",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
                AnimatedVisibility(
                    visible = visible,
                    enter = scaleIn(spring(dampingRatio = 0.55f, stiffness = 200f), initialScale = 0.6f) + fadeIn()
                ) {
                    PodsArt(Modifier.fillMaxWidth().height(190.dp))
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    BatteryRing("Sol", status?.left, status?.leftCharging == true, accent, 64.dp)
                    BatteryRing("Sağ", status?.right, status?.rightCharging == true, accent, 64.dp)
                    BatteryRing("Kutu", status?.case, status?.caseCharging == true, accent, 64.dp)
                }
                Spacer(Modifier.height(20.dp))
                GlassButton("Bitti", accent, primary = true, modifier = Modifier.fillMaxWidth()) { close() }
            }
        }
    }
}
