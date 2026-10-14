package com.kemal.podsglass

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Accents = listOf(
    Color(0xFF0A84FF), // mavi
    Color(0xFFBF5AF2), // mor
    Color(0xFFFF375F), // pembe
    Color(0xFFFF9F0A), // turuncu
    Color(0xFF30D158), // yeşil
    Color(0xFF64D2FF), // camgöbeği
)

@Composable
fun accentColor(): Color {
    val i by Prefs.accent.collectAsState()
    return Accents[i.coerceIn(Accents.indices)]
}

@Composable
fun PodsTheme(content: @Composable () -> Unit) {
    val accent = accentColor()
    MaterialTheme(colorScheme = darkColorScheme(primary = accent, secondary = accent)) {
        CompositionLocalProvider(LocalContentColor provides Color.White, content = content)
    }
}

/** Arkada yavaşça süzülen, bulanık renk lekeleri – Liquid Glass hissi. */
@Composable
fun GlassBackground(accent: Color, content: @Composable BoxScope.() -> Unit) {
    val t = rememberInfiniteTransition(label = "bg")
    val a by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse),
        label = "a"
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF05060A), Color(0xFF0E1020))))
    ) {
        Canvas(Modifier.fillMaxSize().blur(90.dp)) {
            val w = size.width
            val h = size.height
            drawCircle(accent.copy(alpha = 0.55f), w * 0.45f, Offset(w * (0.2f + 0.3f * a), h * 0.18f))
            drawCircle(Color(0xFFBF5AF2).copy(alpha = 0.35f), w * 0.40f, Offset(w * (0.9f - 0.4f * a), h * 0.5f))
            drawCircle(Color(0xFF64D2FF).copy(alpha = 0.30f), w * 0.35f, Offset(w * 0.3f, h * (0.85f - 0.15f * a)))
        }
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Dp = 28.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val g by Prefs.glass.collectAsState()
    val shape = RoundedCornerShape(corner)
    Column(
        modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = (g * 1.5f).coerceAtMost(1f)),
                        Color.White.copy(alpha = g * 0.5f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.06f))),
                shape
            )
            .padding(20.dp),
        content = content
    )
}

/** Kod ile çizilmiş AirPods Pro + kutu illüstrasyonu (havada süzülür). */
@Composable
fun PodsArt(modifier: Modifier = Modifier, float: Boolean = true) {
    val t = rememberInfiniteTransition(label = "f")
    val dy by t.animateFloat(
        -6f, 6f,
        infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dy"
    )
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val white = Brush.linearGradient(listOf(Color.White, Color(0xFFD5D8E0)), Offset.Zero, Offset(w, h))

        // Kutu
        val cw = h * 0.62f * 0.78f
        val ch = h * 0.62f
        val cx = (w - cw) / 2
        val cy = h * 0.36f
        drawRoundRect(white, Offset(cx, cy), Size(cw, ch), CornerRadius(cw * 0.3f))
        drawLine(Color(0x33000000), Offset(cx + 2f, cy + ch * 0.36f), Offset(cx + cw - 2f, cy + ch * 0.36f), 2.dp.toPx())
        drawCircle(Color(0xFF30D158), 3.dp.toPx(), Offset(w / 2, cy + ch * 0.62f))

        // Kulaklıklar
        val off = if (float) dy.dp.toPx() else 0f
        val budH = h * 0.62f
        bud(Offset(w / 2 - cw * 1.15f, h * 0.06f + off), budH, mirror = false, brush = white)
        bud(Offset(w / 2 + cw * 1.15f, h * 0.06f - off), budH, mirror = true, brush = white)
    }
}

private fun DrawScope.bud(top: Offset, height: Float, mirror: Boolean, brush: Brush) {
    val s = if (mirror) -1f else 1f
    val headW = height * 0.40f
    val headH = height * 0.36f
    val stemW = height * 0.10f
    // sap
    drawRoundRect(
        brush,
        Offset(top.x + s * headW * 0.12f - stemW / 2, top.y + headH * 0.55f),
        Size(stemW, height * 0.62f),
        CornerRadius(stemW / 2)
    )
    // kulak ucu (silikon)
    val tipCx = top.x - s * headW * 0.42f
    drawOval(Color(0xFFBFC3CC), Offset(tipCx - headW * 0.22f, top.y + headH * 0.18f), Size(headW * 0.44f, headH * 0.6f))
    // gövde
    drawOval(brush, Offset(top.x - headW / 2, top.y), Size(headW, headH))
    // mikrofon ızgarası
    drawCircle(Color(0x55000000), headW * 0.07f, Offset(top.x + s * headW * 0.18f, top.y + headH * 0.3f))
}

@Composable
fun BatteryRing(label: String, pct: Int?, charging: Boolean, accent: Color, diameter: Dp = 76.dp) {
    val p by animateFloatAsState(
        (pct ?: 0) / 100f,
        spring(dampingRatio = 0.7f, stiffness = 60f),
        label = "p"
    )
    val col = when {
        pct == null -> Color.White.copy(alpha = 0.3f)
        charging -> Color(0xFF30D158)
        pct <= 20 -> Color(0xFFFF453A)
        else -> accent
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val st = 7.dp.toPx()
                val tl = Offset(st / 2, st / 2)
                val sz = Size(size.width - st, size.height - st)
                drawArc(
                    color = Color.White.copy(alpha = 0.12f), startAngle = 0f, sweepAngle = 360f,
                    useCenter = false, topLeft = tl, size = sz, style = Stroke(st)
                )
                drawArc(
                    color = col, startAngle = -90f, sweepAngle = 360f * p,
                    useCenter = false, topLeft = tl, size = sz, style = Stroke(st, cap = StrokeCap.Round)
                )
            }
            Text(
                if (pct == null) "—" else (if (charging) "⚡" else "") + "%$pct",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
    }
}
