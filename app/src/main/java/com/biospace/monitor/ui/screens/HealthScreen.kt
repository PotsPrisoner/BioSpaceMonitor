package com.biospace.monitor.ui.screens

import android.content.Intent
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import com.biospace.monitor.model.*
import com.biospace.monitor.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HealthScreen(
    assessment: IntegratedAssessment,
    ans: ANSState,
    sw: SpaceWeatherState,
    weather: WeatherState,
    sr: SRMetrics
) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth()) {
        EkgHeader(score = assessment.score)
        Spacer(Modifier.height(12.dp))
        ScoreRow(assessment)
        Spacer(Modifier.height(12.dp))
        VitalsSection(ans, sw, weather, sr)
        Spacer(Modifier.height(12.dp))
        SymptomsSection(ans.symptoms)
        Spacer(Modifier.height(12.dp))
        NarrativeSection(assessment)
        Spacer(Modifier.height(12.dp))
        ProtocolSection(assessment.protocols)
        Spacer(Modifier.height(16.dp))
        ReportButton(assessment, ans, sw, weather, sr, context)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun EkgHeader(score: Int) {
    val ekgColor = healthColor(score)
    val statusText = when {
        score <= 25 -> "ALL CLEAR · AUTONOMIC LOAD NOMINAL"
        score <= 50 -> "TAKE IT EASY · ELEVATED ENVIRONMENTAL BURDEN"
        score <= 75 -> "MONITOR CLOSELY · SIGNIFICANT ANS STRESS LOAD"
        else        -> "SEEK MEDICAL ATTENTION · CRITICAL LOAD INDEX"
    }
    val infiniteTransition = rememberInfiniteTransition(label = "ekg")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "phase"
    )
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF030D0A))
            .border(1.dp, ekgColor.copy(0.4f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text("BIOSPACE HEALTH", color = ekgColor, fontSize = 11.sp,
            letterSpacing = 4.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Canvas(modifier = Modifier.fillMaxWidth().height(60.dp)) {
            drawEkg(phase, ekgColor)
        }
        Spacer(Modifier.height(8.dp))
        Text(statusText, color = ekgColor, fontSize = 9.sp, letterSpacing = 2.sp,
            fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth())
    }
}

private fun DrawScope.drawEkg(phase: Float, color: Color) {
    val w = size.width
    val h = size.height
    val mid = h / 2f
    val segW = w / 4f
    val offset = -(phase * w)
    val path = Path()
    path.moveTo(offset, mid)
    for (i in -1..5) {
        val x = offset + i * segW
        path.lineTo(x + segW * 0.15f, mid)
        path.lineTo(x + segW * 0.20f, mid - h * 0.15f)
        path.lineTo(x + segW * 0.25f, mid)
        path.lineTo(x + segW * 0.35f, mid - h * 0.45f)
        path.lineTo(x + segW * 0.38f, mid + h * 0.35f)
        path.lineTo(x + segW * 0.42f, mid - h * 0.12f)
        path.lineTo(x + segW * 0.48f, mid)
        path.lineTo(x + segW * 0.60f, mid - h * 0.10f)
        path.lineTo(x + segW * 0.70f, mid)
        path.lineTo(x + segW, mid)
    }
    clipRect(left = 0f, top = 0f, right = w, bottom = h) {
        drawPath(path, color = color, style = Stroke(width = 2.5f))
    }
    drawLine(color.copy(alpha = 0.08f), Offset(0f, mid), Offset(w, mid), strokeWidth = 1f)
}

@Composable
private fun ScoreRow(assessment: IntegratedAssessment) {
    val scoreColor = healthColor(assessment.score)
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF04021A))
            .border(1.dp, scoreColor.copy(0.3f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("${assessment.score}", color = scoreColor, fontSize = 52.sp,
            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
        Column(modifier = Modifier.weight(1f)) {
            Text(assessment.label, color = scoreColor, fontSize = 12.sp,
                letterSpacing = 3.sp, fontFamily = FontFamily.Monospace)
            Text("INTEGRATED BODY BURDEN INDEX", color = DimColor, fontSize = 8.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniDriver("SPACE", assessment.spaceScore, 40, CyanColor, Modifier.weight(1f))
                MiniDriver("SR", assessment.srScore, 30, SrGoldColor, Modifier.weight(1f))
                MiniDriver("ENV", assessment.envScore, 30, GreenColor, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MiniDriver(label: String, score: Int, max: Int, color: Color, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = DimColor, fontSize = 7.sp, letterSpacing = 1.sp, fontFamily = FontFamily.Monospace)
        Text("$score/$max", color = color, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        Box(modifier = Modifier.fillMaxWidth().height(3.dp)
            .clip(RoundedCornerShape(2.dp)).background(BorderColor)) {
            Box(modifier = Modifier
                .fillMaxWidth(score.toFloat() / max.coerceAtLeast(1))
                .height(3.dp).clip(RoundedCornerShape(2.dp)).background(color))
        }
    }
}

@Composable
private fun VitalsSection(ans: ANSState, sw: SpaceWeatherState, weather: WeatherState, sr: SRMetrics) {
    val pressDelta = if (weather.pressureHistory.size >= 2)
        weather.pressureHistory.last() - weather.pressureHistory.first() else 0.0
    val pressStr = if (pressDelta >= 0) "+${String.format("%.1f", pressDelta)}"
                   else String.format("%.1f", pressDelta)
    val bzSev = when { sw.bz < -10 -> "HIGH"; sw.bz < -5 -> "MOD"; else -> "LOW" }
    val kpSev = when { sw.kp >= 5 -> "HIGH"; sw.kp >= 3 -> "MOD"; else -> "LOW" }
    val pressSev = if (Math.abs(pressDelta) > 3) "MOD" else "LOW"

    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF03080F))
            .border(1.dp, CyanColor.copy(0.2f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("VITAL TRIGGER ANALYSIS", color = CyanColor, fontSize = 11.sp,
            letterSpacing = 3.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        Text("REAL-TIME ENVIRONMENTAL & SPACE WEATHER DRIVER MAPPING",
            color = DimColor, fontSize = 8.sp, letterSpacing = 1.sp)
        HorizontalDivider(color = BorderColor, thickness = 1.dp)

        VitalRow(
            vital = "HEART RATE / HRV",
            status = ans.hrvImpact,
            statusColor = statusColor(ans.hrvImpact),
            mechanism = "Kp ${String.format("%.1f", sw.kp)} → geomagnetic loading → ${if (sw.kp >= 3) "↑HR / ↓HRV likely" else "HR nominal"}. SR f₁ ${String.format("%.2f", sr.f1)}Hz ELF entrains cardiac rhythm.",
            drivers = listOf("Kp ${String.format("%.1f", sw.kp)}" to kpSev,
                "Bz ${String.format("%.1f", sw.bz)}" to bzSev,
                "SR ${String.format("%.2f", sr.f1)}Hz" to "INFO",
                "ΔP ${pressStr}hPa" to pressSev)
        )
        VitalRow(
            vital = "BLOOD PRESSURE",
            status = when { sw.speed > 600 -> "ELEVATED RISK"; sw.speed > 450 -> "WATCH"; else -> "NOMINAL" },
            statusColor = when { sw.speed > 600 -> Color(0xFFFF2244); sw.speed > 450 -> Color(0xFFFF8C00); else -> GreenColor },
            mechanism = "Solar wind ${String.format("%.0f", sw.speed)} km/s → magnetospheric compression → baroreflex strain. Barometric delta ${pressStr} hPa adds orthostatic load.",
            drivers = listOf("SW ${String.format("%.0f", sw.speed)}km/s" to if (sw.speed > 600) "HIGH" else "LOW",
                "Kp ${String.format("%.1f", sw.kp)}" to kpSev,
                "ΔP ${pressStr}hPa" to pressSev,
                "RH ${weather.humidity}%" to "INFO")
        )
        VitalRow(
            vital = "AUTONOMIC TONE",
            status = ans.hrvImpact,
            statusColor = statusColor(ans.hrvImpact),
            mechanism = "Sympathetic bias ${String.format("%.0f", ans.sympatheticBias)}%. Bz ${String.format("%.1f", sw.bz)} nT southward → parasympathetic withdrawal. SR coherence ${sr.coherenceScore}% modulates vagal tone.",
            drivers = listOf("Symp ${String.format("%.0f", ans.sympatheticBias)}%" to if (ans.sympatheticBias > 65) "HIGH" else "LOW",
                "Bz ${String.format("%.1f", sw.bz)}" to bzSev,
                "SR Q ${String.format("%.1f", sr.qFactor)}" to "INFO",
                "Coh ${sr.coherenceScore}%" to if (sr.coherenceScore < 50) "MOD" else "LOW")
        )
        VitalRow(
            vital = "CORTISOL AXIS",
            status = ans.cortisolAxis,
            statusColor = statusColor(ans.cortisolAxis),
            mechanism = "G${sw.gScale} storm + ${sw.flareClass} flare → HPA axis activation → cortisol spike within 2–4hr of onset. SR amplitude ${String.format("%.1f", sr.amplitude)} amplifies stress response.",
            drivers = listOf("G-Scale ${sw.gScale}" to if (sw.gScale >= 3) "HIGH" else if (sw.gScale >= 1) "MOD" else "LOW",
                sw.flareClass to if (sw.flareClass.startsWith("X")) "HIGH" else if (sw.flareClass.startsWith("M")) "MOD" else "LOW",
                "SR amp ${String.format("%.1f", sr.amplitude)}" to "INFO")
        )
        VitalRow(
            vital = "MELATONIN",
            status = ans.melatonin,
            statusColor = statusColor(ans.melatonin),
            mechanism = "Kp ≥ 4 → documented melatonin suppression via pineal magnetoreception. Current Kp ${String.format("%.1f", sw.kp)} ${if (sw.kp >= 4) "→ SUPPRESSED" else "→ within normal range"}.",
            drivers = listOf("Kp ${String.format("%.1f", sw.kp)}" to kpSev,
                "SR drift ${String.format("%.2f", sr.drift)}Hz" to if (Math.abs(sr.drift) > 0.3) "MOD" else "LOW")
        )
        VitalRow(
            vital = "RESPIRATION",
            status = if (Math.abs(sr.f1 - 7.83) > 0.4) "DISRUPTED" else "NOMINAL",
            statusColor = if (Math.abs(sr.f1 - 7.83) > 0.4) Color(0xFFFF8C00) else GreenColor,
            mechanism = "SR f₁ ${String.format("%.2f", sr.f1)}Hz vs 7.83Hz baseline. ELF entrainment of medullary respiratory rhythm. TEC ${String.format("%.1f", sw.tecLocal)} TECU modulates ionospheric cavity.",
            drivers = listOf("SR f₁ ${String.format("%.2f", sr.f1)}Hz" to if (Math.abs(sr.f1 - 7.83) > 0.4) "MOD" else "LOW",
                "TEC ${String.format("%.1f", sw.tecLocal)}" to "INFO",
                "RH ${weather.humidity}%" to "INFO")
        )
        VitalRow(
            vital = "NEUROLOGICAL",
            status = if (sr.coherenceScore < 40) "DISRUPTED" else if (sr.coherenceScore < 60) "REDUCED" else "NOMINAL",
            statusColor = if (sr.coherenceScore < 40) Color(0xFFFF2244) else if (sr.coherenceScore < 60) Color(0xFFFF8C00) else GreenColor,
            mechanism = "SR coherence ${sr.coherenceScore}%. TEC Δ${String.format("%.1f", sw.tecLocal - sw.tecMedian)} TECU. Bz ${String.format("%.1f", sw.bz)} nT fluctuation disrupts magnetite-based neural signaling and cryptochrome compass.",
            drivers = listOf("SR coh ${sr.coherenceScore}%" to if (sr.coherenceScore < 40) "HIGH" else "LOW",
                "TEC Δ${String.format("%.1f", sw.tecLocal - sw.tecMedian)}" to "INFO",
                "Bz ${String.format("%.1f", sw.bz)}" to bzSev)
        )
        VitalRow(
            vital = "INFLAMMATION",
            status = if (sw.kp >= 5 || sw.gScale >= 3) "ELEVATED" else if (sw.kp >= 3) "WATCH" else "NOMINAL",
            statusColor = if (sw.kp >= 5 || sw.gScale >= 3) Color(0xFFFF2244) else if (sw.kp >= 3) Color(0xFFFF8C00) else GreenColor,
            mechanism = "Solar wind density ${String.format("%.1f", sw.density)} p/cm³ + G${sw.gScale} storm → mast cell activation pathway. ${sw.cmeEvents.size} CME event(s) in 7d window.",
            drivers = listOf("Density ${String.format("%.1f", sw.density)}" to if (sw.density > 10) "MOD" else "LOW",
                "G-Scale ${sw.gScale}" to if (sw.gScale >= 3) "HIGH" else "LOW",
                "CME ${sw.cmeEvents.size}" to if (sw.cmeEvents.isNotEmpty()) "MOD" else "LOW")
        )
        VitalRow(
            vital = "OXYGEN SAT",
            status = if (weather.pressureHpa < 1005) "WATCH" else "NOMINAL",
            statusColor = if (weather.pressureHpa < 1005) Color(0xFFFF8C00) else GreenColor,
            mechanism = "Barometric ${String.format("%.0f", weather.pressureHpa)} hPa — low pressure reduces O₂ partial pressure. Delta ${pressStr} hPa over observation window adds orthostatic O₂ demand.",
            drivers = listOf("P ${String.format("%.0f", weather.pressureHpa)}hPa" to if (weather.pressureHpa < 1005) "MOD" else "LOW",
                "ΔP ${pressStr}" to pressSev)
        )
    }
}

@Composable
private fun VitalRow(
    vital: String,
    status: String,
    statusColor: Color,
    mechanism: String,
    drivers: List<Pair<String, String>>
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CardColor)
            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text(vital, color = TextColor, fontSize = 11.sp,
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                .background(statusColor.copy(0.15f))
                .border(1.dp, statusColor.copy(0.5f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text(status, color = statusColor, fontSize = 8.sp, letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace)
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(mechanism, color = DimColor, fontSize = 9.sp, lineHeight = 14.sp,
            fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())) {
            drivers.forEach { (label, sev) ->
                val chipColor = when (sev) {
                    "HIGH" -> Color(0xFFFF2244)
                    "MOD"  -> Color(0xFFFF8C00)
                    "INFO" -> CyanColor
                    else   -> GreenColor
                }
                Box(modifier = Modifier.clip(RoundedCornerShape(3.dp))
                    .background(chipColor.copy(0.1f))
                    .border(1.dp, chipColor.copy(0.4f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)) {
                    Text(label, color = chipColor, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun SymptomsSection(symptoms: List<SymptomPrediction>) {
    if (symptoms.isEmpty()) return
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF060216))
            .border(1.dp, VioletColor.copy(0.3f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("SYMPTOM PREDICTIONS", color = VioletColor, fontSize = 11.sp,
            letterSpacing = 3.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        symptoms.forEach { sym ->
            val sevColor = when (sym.severity) {
                "high"     -> Color(0xFFFF2244)
                "moderate" -> Color(0xFFFF8C00)
                else       -> GreenColor
            }
            Column(modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CardColor)
                .border(1.dp, sevColor.copy(0.3f), RoundedCornerShape(8.dp))
                .padding(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${sym.icon} ${sym.name}", color = TextColor, fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                    Text("${sym.probability}%", color = sevColor, fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth().height(3.dp)
                    .clip(RoundedCornerShape(2.dp)).background(BorderColor)) {
                    Box(modifier = Modifier.fillMaxWidth(sym.probability / 100f).height(3.dp)
                        .clip(RoundedCornerShape(2.dp)).background(sevColor))
                }
                Spacer(Modifier.height(5.dp))
                Text(sym.mechanism, color = DimColor, fontSize = 9.sp, lineHeight = 14.sp)
                if (sym.drivers.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        sym.drivers.take(4).forEach { d ->
                            Box(modifier = Modifier.clip(RoundedCornerShape(3.dp))
                                .background(VioletColor.copy(0.1f))
                                .border(1.dp, VioletColor.copy(0.3f), RoundedCornerShape(3.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)) {
                                Text(d, color = VioletColor, fontSize = 7.sp,
                                    fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NarrativeSection(assessment: IntegratedAssessment) {
    Box(modifier = Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(10.dp))
        .background(Color(0xFF050318))
        .border(1.dp, VioletColor.copy(0.3f), RoundedCornerShape(10.dp))
        .padding(13.dp)) {
        Column {
            Text("CLINICAL NARRATIVE", color = VioletColor, fontSize = 9.sp,
                letterSpacing = 3.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(8.dp))
            Text(assessment.narrative, color = TextColor, fontSize = 10.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun ProtocolSection(protocols: List<String>) {
    Box(modifier = Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(10.dp))
        .background(Color(0xFF030C14))
        .border(1.dp, CyanColor.copy(0.3f), RoundedCornerShape(10.dp))
        .padding(12.dp)) {
        Column {
            Text("MANAGEMENT PROTOCOL", color = CyanColor, fontSize = 9.sp,
                letterSpacing = 3.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(8.dp))
            protocols.forEach { item ->
                Row(modifier = Modifier.padding(bottom = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("▸", color = CyanColor, fontSize = 9.sp)
                    Text(item, color = DimColor, fontSize = 9.sp, lineHeight = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun ReportButton(
    assessment: IntegratedAssessment,
    ans: ANSState,
    sw: SpaceWeatherState,
    weather: WeatherState,
    sr: SRMetrics,
    context: android.content.Context
) {
    Button(
        onClick = {
            val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            val report = buildString {
                appendLine("=======================================")
                appendLine("  BIOSPACE HEALTH REPORT")
                appendLine("  Generated: $ts")
                appendLine("  Location: ${weather.locationName}")
                appendLine("=======================================")
                appendLine("BODY BURDEN INDEX: ${assessment.score}/100  |  ${assessment.label}")
                appendLine("Space: ${assessment.spaceScore}/40  SR: ${assessment.srScore}/30  Env: ${assessment.envScore}/30")
                appendLine()
                appendLine("-- SPACE WEATHER --")
                appendLine("Kp: ${String.format("%.2f", sw.kp)}  SW Speed: ${String.format("%.0f", sw.speed)} km/s  Density: ${String.format("%.1f", sw.density)} p/cm3")
                appendLine("Bz: ${String.format("%.1f", sw.bz)} nT  Bt: ${String.format("%.1f", sw.bt)} nT  Flare: ${sw.flareClass}")
                appendLine("G${sw.gScale} S${sw.sScale} R${sw.rScale}  TEC: ${String.format("%.1f", sw.tecLocal)} TECU  CME(7d): ${sw.cmeEvents.size}")
                appendLine()
                appendLine("-- SCHUMANN RESONANCE --")
                appendLine("f1: ${String.format("%.2f", sr.f1)} Hz  Drift: ${String.format("%.2f", sr.drift)} Hz  Q: ${String.format("%.1f", sr.qFactor)}")
                appendLine("Amplitude: ${String.format("%.1f", sr.amplitude)}  Coherence: ${sr.coherenceScore}%")
                appendLine()
                appendLine("-- LOCAL ENVIRONMENT --")
                appendLine("Temp: ${String.format("%.1f", weather.tempF)}F  Humidity: ${weather.humidity}%  Pressure: ${String.format("%.1f", weather.pressureHpa)} hPa")
                appendLine("Wind: ${String.format("%.1f", weather.windMph)} mph")
                appendLine()
                appendLine("-- ANS STATE --")
                appendLine("Load: ${ans.loadIndex}  Sympathetic: ${String.format("%.0f", ans.sympatheticBias)}%")
                appendLine("HRV: ${ans.hrvImpact}  Cortisol: ${ans.cortisolAxis}  Melatonin: ${ans.melatonin}")
                appendLine()
                appendLine("-- CLINICAL NARRATIVE --")
                appendLine(assessment.narrative)
                appendLine()
                appendLine("-- PROTOCOL --")
                assessment.protocols.forEachIndexed { i, p -> appendLine("${i + 1}. $p") }
                appendLine()
                appendLine("BioSpace Monitor | Not medical advice")
                appendLine("=======================================")
            }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "BioSpace Health Report $ts")
                putExtra(Intent.EXTRA_TEXT, report)
            }
            context.startActivity(Intent.createChooser(intent, "Share Report"))
        },
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CyanColor.copy(0.15f))
    ) {
        Text("DOWNLOAD / SHARE REPORT", color = CyanColor, fontSize = 12.sp,
            letterSpacing = 2.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

private fun healthColor(score: Int): Color = when {
    score <= 25 -> Color(0xFF00FF88)
    score <= 50 -> Color(0xFFFFE000)
    score <= 75 -> Color(0xFFFF8C00)
    else        -> Color(0xFFFF2244)
}

private fun statusColor(status: String): Color = when (status.uppercase()) {
    "NORMAL", "NOMINAL", "OPTIMAL" -> Color(0xFF00FF88)
    "ELEVATED", "WATCH", "REDUCED" -> Color(0xFFFF8C00)
    "SUPPRESSED", "DISRUPTED", "HIGH", "CRITICAL" -> Color(0xFFFF2244)
    else -> Color(0xFFFFE000)
}
