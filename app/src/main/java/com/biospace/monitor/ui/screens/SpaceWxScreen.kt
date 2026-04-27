package com.biospace.monitor.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import com.biospace.monitor.model.*
import com.biospace.monitor.ui.theme.*
import kotlin.math.*

@Composable
fun SpaceWxScreen(sr: SRMetrics, ans: ANSState, sw: SpaceWeatherState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top dials row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpDial(kp = sw.kp, modifier = Modifier.weight(1f))
            SolarWindDial(speed = sw.speed, modifier = Modifier.weight(1f))
        }

        // Bz meter full width
        BzMeter(bz = sw.bz, bt = sw.bt)

        // SR frequency display
        SrFrequencyDisplay(sr = sr)

        // Existing screens
        SchumannScreen(sr = sr, sw = sw)
        AnsScreen(ans = ans)
    }
}

@Composable
private fun KpDial(kp: Double, modifier: Modifier) {
    val dialColor = when {
        kp >= 7 -> Color(0xFFFF2244)
        kp >= 5 -> Color(0xFFFF8C00)
        kp >= 3 -> Color(0xFFFFE000)
        else    -> Color(0xFF00FF88)
    }
    val animatedKp by animateFloatAsState(
        targetValue = (kp / 9.0).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(1200, easing = FastOutSlowInEasing), label = "kp"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF03080F))
            .border(1.dp, dialColor.copy(0.3f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Kp INDEX", color = DimColor, fontSize = 8.sp, letterSpacing = 2.sp,
            fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(8.dp))
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(110.dp)) {
                drawArcDial(animatedKp, dialColor)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(String.format("%.1f", kp), color = dialColor, fontSize = 24.sp,
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
                Text("/ 9.0", color = DimColor, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
        }
        Spacer(Modifier.height(4.dp))
        val label = when {
            kp >= 7 -> "SEVERE STORM"
            kp >= 5 -> "GEOMAGNETIC STORM"
            kp >= 3 -> "UNSETTLED"
            else    -> "QUIET"
        }
        Text(label, color = dialColor, fontSize = 8.sp, letterSpacing = 1.sp,
            fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SolarWindDial(speed: Double, modifier: Modifier) {
    val dialColor = when {
        speed > 700 -> Color(0xFFFF2244)
        speed > 550 -> Color(0xFFFF8C00)
        speed > 400 -> Color(0xFFFFE000)
        else        -> Color(0xFF00FF88)
    }
    val animatedSpeed by animateFloatAsState(
        targetValue = ((speed - 200.0) / 700.0).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(1200, easing = FastOutSlowInEasing), label = "sw"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF03080F))
            .border(1.dp, dialColor.copy(0.3f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("SOLAR WIND", color = DimColor, fontSize = 8.sp, letterSpacing = 2.sp,
            fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(8.dp))
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(110.dp)) {
                drawArcDial(animatedSpeed, dialColor)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(String.format("%.0f", speed), color = dialColor, fontSize = 22.sp,
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
                Text("km/s", color = DimColor, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
        }
        Spacer(Modifier.height(4.dp))
        val label = when {
            speed > 700 -> "EXTREME"
            speed > 550 -> "HIGH"
            speed > 400 -> "ELEVATED"
            else        -> "NOMINAL"
        }
        Text(label, color = dialColor, fontSize = 8.sp, letterSpacing = 1.sp,
            fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
    }
}

private fun DrawScope.drawArcDial(fraction: Float, color: Color) {
    val strokeW = 10f
    val startAngle = 150f
    val sweepTotal = 240f
    val padding = strokeW / 2 + 4f
    val arcSize = Size(size.width - padding * 2, size.height - padding * 2)
    val topLeft = Offset(padding, padding)

    // Track
    drawArc(color = Color(0xFF1A2A2A), startAngle = startAngle, sweepAngle = sweepTotal,
        useCenter = false, topLeft = topLeft, size = arcSize,
        style = Stroke(width = strokeW, cap = StrokeCap.Round))

    // Fill
    if (fraction > 0f) {
        drawArc(color = color, startAngle = startAngle, sweepAngle = sweepTotal * fraction,
            useCenter = false, topLeft = topLeft, size = arcSize,
            style = Stroke(width = strokeW, cap = StrokeCap.Round))
    }

    // Tick marks
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = (size.width / 2f) - padding - strokeW
    for (i in 0..9) {
        val angle = Math.toRadians((startAngle + sweepTotal * i / 9f).toDouble())
        val innerR = r - 6f
        val outerR = r + 2f
        drawLine(Color(0xFF2A3A3A),
            start = Offset(cx + (innerR * cos(angle)).toFloat(), cy + (innerR * sin(angle)).toFloat()),
            end   = Offset(cx + (outerR * cos(angle)).toFloat(), cy + (outerR * sin(angle)).toFloat()),
            strokeWidth = 1.5f)
    }

    // Needle dot at tip
    if (fraction > 0f) {
        val needleAngle = Math.toRadians((startAngle + sweepTotal * fraction).toDouble())
        val nr = (size.width / 2f) - padding
        drawCircle(color = color, radius = 5f,
            center = Offset(cx + (nr * cos(needleAngle)).toFloat(), cy + (nr * sin(needleAngle)).toFloat()))
    }
}

@Composable
private fun BzMeter(bz: Double, bt: Double) {
    val bzColor = when {
        bz < -15 -> Color(0xFFFF2244)
        bz < -5  -> Color(0xFFFF8C00)
        bz < 0   -> Color(0xFFFFE000)
        else     -> Color(0xFF00FF88)
    }
    val animatedBz by animateFloatAsState(
        targetValue = ((bz + 30.0) / 60.0).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(1000, easing = FastOutSlowInEasing), label = "bz"
    )

    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF03080F))
            .border(1.dp, bzColor.copy(0.3f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("IMF Bz", color = DimColor, fontSize = 8.sp, letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace)
                Text(String.format("%.1f nT", bz), color = bzColor, fontSize = 22.sp,
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Bt TOTAL", color = DimColor, fontSize = 8.sp, letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace)
                Text(String.format("%.1f nT", bt), color = CyanColor, fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(12.dp))

        // Horizontal bar: -30 left, 0 center, +30 right
        Box(modifier = Modifier.fillMaxWidth().height(20.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val mid = w / 2f
                val barH = 8f
                val barY = (h - barH) / 2f

                // Background track
                drawRoundRect(color = Color(0xFF1A2A2A), topLeft = Offset(0f, barY),
                    size = Size(w, barH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f))

                // Danger zone (left half = southward = bad)
                drawRoundRect(color = Color(0xFFFF2244).copy(0.08f),
                    topLeft = Offset(0f, barY), size = Size(mid, barH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f))

                // Fill from center
                val fillW = ((animatedBz - 0.5f) * w).let { if (it >= 0) it else -it }
                val fillX = if (animatedBz >= 0.5f) mid else mid - fillW
                drawRoundRect(color = bzColor, topLeft = Offset(fillX, barY),
                    size = Size(fillW, barH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f))

                // Center line
                drawLine(Color(0xFF4A5A5A), Offset(mid, 0f), Offset(mid, h), strokeWidth = 1.5f)
            }
        }

        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("-30 nT", color = Color(0xFFFF2244), fontSize = 7.sp, fontFamily = FontFamily.Monospace)
            Text("SOUTHWARD ← 0 → NORTHWARD", color = DimColor, fontSize = 7.sp,
                fontFamily = FontFamily.Monospace)
            Text("+30 nT", color = GreenColor, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            when {
                bz < -15 -> "EXTREME SOUTHWARD · SEVERE MAGNETOSPHERIC COMPRESSION · ANS CRISIS RISK"
                bz < -5  -> "SOUTHWARD · GEOMAGNETIC COUPLING ACTIVE · ELEVATED ANS LOAD"
                bz < 0   -> "SLIGHTLY SOUTHWARD · MINOR COUPLING · MONITOR"
                else     -> "NORTHWARD · SHIELDING ACTIVE · REDUCED ANS IMPACT"
            },
            color = bzColor, fontSize = 8.sp, letterSpacing = 1.sp, fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun SrFrequencyDisplay(sr: SRMetrics) {
    val srColor = if (Math.abs(sr.f1 - 7.83) > 0.4) Color(0xFFFF8C00) else SrGoldColor
    val infiniteTransition = rememberInfiniteTransition(label = "sr")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween((1000.0 / sr.f1).toInt(), easing = LinearEasing)
        ), label = "pulse"
    )

    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF080600))
            .border(1.dp, srColor.copy(0.3f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Text("SCHUMANN RESONANCE", color = DimColor, fontSize = 8.sp,
            letterSpacing = 3.sp, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(String.format("%.2f Hz", sr.f1), color = srColor, fontSize = 28.sp,
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
                Text("baseline 7.83 Hz  drift ${String.format("%+.2f", sr.drift)} Hz",
                    color = DimColor, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            }
            // Animated pulse circle
            Canvas(modifier = Modifier.size(50.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val maxR = size.width / 2f
                drawCircle(color = srColor.copy(0.15f * (1f - pulse)), radius = maxR * pulse, center = Offset(cx, cy))
                drawCircle(color = srColor.copy(0.8f), radius = 6f, center = Offset(cx, cy))
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SrChip("Q-FACTOR", String.format("%.1f", sr.qFactor), srColor, Modifier.weight(1f))
            SrChip("AMPLITUDE", String.format("%.1f", sr.amplitude), srColor, Modifier.weight(1f))
            SrChip("COHERENCE", "${sr.coherenceScore}%", srColor, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SrChip(label: String, value: String, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(6.dp))
            .background(CardColor)
            .border(1.dp, BorderColor, RoundedCornerShape(6.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = DimColor, fontSize = 7.sp, letterSpacing = 1.sp,
            fontFamily = FontFamily.Monospace)
        Text(value, color = color, fontSize = 13.sp, fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold)
    }
}
