package com.biospace.monitor.ui.screens

import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.biospace.monitor.model.ChatMessage
import com.biospace.monitor.ui.theme.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

private val Context.logDataStore by preferencesDataStore("health_log_prefs")
private val LOG_KEY = stringPreferencesKey("health_entries")

private data class HealthEntry(
    val timestamp: Long,
    val systolic: String,
    val diastolic: String,
    val heartRate: String,
    val o2sat: String,
    val respRate: String,
    val notes: String
)

private fun HealthEntry.toJson() = JSONObject().apply {
    put("ts", timestamp)
    put("sys", systolic)
    put("dia", diastolic)
    put("hr", heartRate)
    put("o2", o2sat)
    put("rr", respRate)
    put("notes", notes)
}.toString()

private fun jsonToEntry(s: String) = try {
    val j = JSONObject(s)
    HealthEntry(
        timestamp  = j.optLong("ts", 0L),
        systolic   = j.optString("sys", ""),
        diastolic  = j.optString("dia", ""),
        heartRate  = j.optString("hr", ""),
        o2sat      = j.optString("o2", ""),
        respRate   = j.optString("rr", ""),
        notes      = j.optString("notes", "")
    )
} catch (e: Exception) { null }

@Composable
fun LogChatScreen(
    messages: List<ChatMessage>,
    connected: Boolean,
    onSend: (text: String, nick: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val entriesJson by context.logDataStore.data
        .map { it[LOG_KEY] ?: "[]" }
        .collectAsState(initial = "[]")

    val entries = remember(entriesJson) {
        try {
            val arr = JSONArray(entriesJson)
            (0 until arr.length()).mapNotNull { jsonToEntry(arr.getString(it)) }
                .sortedByDescending { it.timestamp }.take(20)
        } catch (e: Exception) { emptyList() }
    }

    fun saveEntry(entry: HealthEntry) {
        scope.launch {
            context.logDataStore.edit { prefs ->
                val arr = try { JSONArray(prefs[LOG_KEY] ?: "[]") } catch (e: Exception) { JSONArray() }
                arr.put(entry.toJson())
                // Keep last 50
                val trimmed = JSONArray()
                val start = maxOf(0, arr.length() - 50)
                for (i in start until arr.length()) trimmed.put(arr.get(i))
                prefs[LOG_KEY] = trimmed.toString()
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        HealthLogSection(entries = entries, onSave = { saveEntry(it) })
        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = BorderColor)
        Spacer(Modifier.height(14.dp))
        // Chat takes remaining space — give it a fixed height so it's usable
        Box(modifier = Modifier.fillMaxWidth().height(500.dp)) {
            ChatScreen(messages = messages, connected = connected, onSend = onSend)
        }
    }
}

@Composable
private fun HealthLogSection(entries: List<HealthEntry>, onSave: (HealthEntry) -> Unit) {
    var systolic  by remember { mutableStateOf("") }
    var diastolic by remember { mutableStateOf("") }
    var heartRate by remember { mutableStateOf("") }
    var o2sat     by remember { mutableStateOf("") }
    var respRate  by remember { mutableStateOf("") }
    var notes     by remember { mutableStateOf("") }
    var saved     by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF03080F))
            .border(1.dp, GreenColor.copy(0.3f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("HEALTH METRICS LOG", color = GreenColor, fontSize = 11.sp,
            letterSpacing = 3.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        Text("MANUAL VITALS ENTRY · SAVED LOCALLY",
            color = DimColor, fontSize = 8.sp, letterSpacing = 1.sp)
        HorizontalDivider(color = BorderColor)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LogField("SYSTOLIC", systolic, "mmHg", Modifier.weight(1f)) { systolic = it; saved = false }
            LogField("DIASTOLIC", diastolic, "mmHg", Modifier.weight(1f)) { diastolic = it; saved = false }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LogField("HEART RATE", heartRate, "bpm", Modifier.weight(1f)) { heartRate = it; saved = false }
            LogField("O2 SAT", o2sat, "%", Modifier.weight(1f)) { o2sat = it; saved = false }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LogField("RESP RATE", respRate, "br/min", Modifier.weight(1f)) { respRate = it; saved = false }
            Spacer(Modifier.weight(1f))
        }

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it; saved = false },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Notes / symptoms...", color = DimColor, fontSize = 11.sp,
                fontFamily = FontFamily.Monospace) },
            minLines = 2,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = GreenColor.copy(0.6f),
                unfocusedBorderColor = BorderColor,
                cursorColor = GreenColor,
                focusedContainerColor = Color(0xFF060E14),
                unfocusedContainerColor = Color(0xFF060E14)
            ),
            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
            shape = RoundedCornerShape(8.dp)
        )

        Button(
            onClick = {
                onSave(HealthEntry(
                    timestamp = System.currentTimeMillis(),
                    systolic = systolic, diastolic = diastolic,
                    heartRate = heartRate, o2sat = o2sat,
                    respRate = respRate, notes = notes
                ))
                systolic = ""; diastolic = ""; heartRate = ""
                o2sat = ""; respRate = ""; notes = ""
                saved = true
            },
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GreenColor.copy(0.15f))
        ) {
            Text(if (saved) "LOGGED ✓" else "LOG ENTRY",
                color = if (saved) GreenColor else GreenColor,
                fontSize = 12.sp, letterSpacing = 2.sp, fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold)
        }

        if (entries.isNotEmpty()) {
            HorizontalDivider(color = BorderColor)
            Text("RECENT ENTRIES", color = DimColor, fontSize = 8.sp, letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace)
            entries.take(5).forEach { e ->
                LogEntryCard(e)
            }
        }
    }
}

@Composable
private fun LogField(label: String, value: String, unit: String, modifier: Modifier, onChange: (String) -> Unit) {
    Column(modifier = modifier) {
        Text(label, color = DimColor, fontSize = 7.sp, letterSpacing = 1.sp,
            fontFamily = FontFamily.Monospace)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(unit, color = DimColor.copy(0.5f), fontSize = 11.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = GreenColor.copy(0.6f),
                unfocusedBorderColor = BorderColor,
                cursorColor = GreenColor,
                focusedContainerColor = Color(0xFF060E14),
                unfocusedContainerColor = Color(0xFF060E14)
            ),
            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
            shape = RoundedCornerShape(6.dp)
        )
    }
}

@Composable
private fun LogEntryCard(entry: HealthEntry) {
    val fmt = remember { SimpleDateFormat("MM/dd HH:mm", Locale.US) }
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(CardColor)
            .border(1.dp, BorderColor, RoundedCornerShape(6.dp))
            .padding(8.dp)
    ) {
        Text(fmt.format(Date(entry.timestamp)), color = CyanColor, fontSize = 8.sp,
            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (entry.systolic.isNotEmpty()) MetricChip("BP", "${entry.systolic}/${entry.diastolic}")
            if (entry.heartRate.isNotEmpty()) MetricChip("HR", "${entry.heartRate} bpm")
            if (entry.o2sat.isNotEmpty()) MetricChip("O2", "${entry.o2sat}%")
            if (entry.respRate.isNotEmpty()) MetricChip("RR", "${entry.respRate} br/m")
        }
        if (entry.notes.isNotEmpty()) {
            Spacer(Modifier.height(3.dp))
            Text(entry.notes, color = DimColor, fontSize = 9.sp, lineHeight = 13.sp)
        }
    }
}

@Composable
private fun MetricChip(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = DimColor, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
        Text(value, color = GreenColor, fontSize = 9.sp, fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold)
    }
}
