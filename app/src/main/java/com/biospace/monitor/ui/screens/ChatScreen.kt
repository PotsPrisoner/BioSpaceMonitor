package com.biospace.monitor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.biospace.monitor.model.ChatMessage
import com.biospace.monitor.ui.theme.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.content.Context

private val Context.chatDataStore by preferencesDataStore("chat_prefs")
private val NICK_KEY = stringPreferencesKey("nickname")

@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    connected: Boolean,
    onSend: (text: String, nick: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var text by remember { mutableStateOf("") }
    var nick by remember { mutableStateOf("") }
    var showNickDialog by remember { mutableStateOf(false) }

    val savedNick by remember {
        context.chatDataStore.data.map { it[NICK_KEY] ?: "" }
    }.collectAsState(initial = "")

    LaunchedEffect(savedNick) {
        if (savedNick.isNotEmpty()) nick = savedNick
        else showNickDialog = true
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    fun saveNick(n: String) {
        scope.launch { context.chatDataStore.edit { it[NICK_KEY] = n } }
    }

    fun doSend() {
        val t = text.trim()
        val n = nick.trim().ifEmpty { "ANON" }
        if (t.isEmpty()) return
        onSend(t, n)
        text = ""
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(7.dp).clip(RoundedCornerShape(50))
                    .background(if (connected) Color(0xFF00FF88) else Color(0xFFFF4444)))
                Text(if (connected) "LIVE" else "OFFLINE",
                    color = if (connected) Color(0xFF00FF88) else Color(0xFFFF4444),
                    fontSize = 9.sp, letterSpacing = 3.sp, fontFamily = FontFamily.Monospace)
                Text("· GLOBAL CHANNEL", color = DimColor, fontSize = 9.sp, letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace)
            }
            TextButton(onClick = { showNickDialog = true },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                Text(nick.ifEmpty { "SET CALLSIGN" }, color = CyanColor, fontSize = 9.sp,
                    letterSpacing = 2.sp, fontFamily = FontFamily.Monospace)
            }
        }
        HorizontalDivider(color = BorderColor, thickness = 1.dp)
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            items(messages, key = { it.id.ifEmpty { it.timestamp.toString() + it.nick } }) { msg ->
                MessageBubble(msg)
            }
        }
        HorizontalDivider(color = BorderColor, thickness = 1.dp)
        Row(
            modifier = Modifier.fillMaxWidth().background(CardColor)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("transmit...", color = DimColor, fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { doSend() }),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = CyanColor.copy(alpha = 0.6f), unfocusedBorderColor = BorderColor,
                    cursorColor = CyanColor,
                    focusedContainerColor = Color(0xFF060E14), unfocusedContainerColor = Color(0xFF060E14)
                ),
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                shape = RoundedCornerShape(8.dp)
            )
            IconButton(
                onClick = { doSend() },
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp))
                    .background(if (text.isNotBlank()) CyanColor.copy(0.15f) else Color.Transparent)
                    .border(1.dp, if (text.isNotBlank()) CyanColor.copy(0.5f) else BorderColor, RoundedCornerShape(8.dp))
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send",
                    tint = if (text.isNotBlank()) CyanColor else DimColor, modifier = Modifier.size(18.dp))
            }
        }
    }

    if (showNickDialog) {
        var draftNick by remember { mutableStateOf(nick) }
        AlertDialog(
            onDismissRequest = { if (nick.isNotEmpty()) showNickDialog = false },
            containerColor = CardColor,
            title = { Text("SET CALLSIGN", color = CyanColor, fontSize = 13.sp,
                letterSpacing = 3.sp, fontFamily = FontFamily.Monospace) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Your display name in the global channel.",
                        color = DimColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    OutlinedTextField(
                        value = draftNick, onValueChange = { if (it.length <= 20) draftNick = it },
                        singleLine = true,
                        placeholder = { Text("e.g. SENSOR-7", color = DimColor, fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedBorderColor = CyanColor, unfocusedBorderColor = BorderColor,
                            cursorColor = CyanColor,
                            focusedContainerColor = Color(0xFF060E14), unfocusedContainerColor = Color(0xFF060E14)
                        ),
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val n = draftNick.trim().ifEmpty { "ANON" }
                    nick = n; saveNick(n); showNickDialog = false
                }) {
                    Text("CONFIRM", color = CyanColor, fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp, letterSpacing = 2.sp)
                }
            },
            dismissButton = if (nick.isNotEmpty()) {
                { TextButton(onClick = { showNickDialog = false }) {
                    Text("CANCEL", color = DimColor, fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp, letterSpacing = 2.sp)
                }}
            } else null
        )
    }
}

@Composable
private fun MessageBubble(msg: ChatMessage) {
    val timeStr = remember(msg.timestamp) {
        SimpleDateFormat("HH:mm", Locale.US).format(Date(msg.timestamp))
    }
    when {
        msg.isSystem -> {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(msg.text, color = DimColor.copy(alpha = 0.6f), fontSize = 9.sp,
                    letterSpacing = 2.sp, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
            }
        }
        msg.mine -> {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                Box(modifier = Modifier.widthIn(max = 280.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 4.dp, bottomStart = 12.dp, bottomEnd = 12.dp))
                    .background(Color(0xFF0A2235))
                    .border(1.dp, CyanColor.copy(0.3f), RoundedCornerShape(topStart = 12.dp, topEnd = 4.dp, bottomStart = 12.dp, bottomEnd = 12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(msg.text, color = Color.White, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                }
                Text(timeStr, color = DimColor, fontSize = 8.sp, fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 2.dp, end = 4.dp))
            }
        }
        else -> {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                Text(msg.nick, color = CyanColor.copy(alpha = 0.8f), fontSize = 9.sp,
                    letterSpacing = 2.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
                Box(modifier = Modifier.widthIn(max = 280.dp)
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp))
                    .background(CardColor)
                    .border(1.dp, BorderColor, RoundedCornerShape(topStart = 4.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(msg.text, color = Color(0xFFCCDDEE), fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                }
                Text(timeStr, color = DimColor, fontSize = 8.sp, fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 2.dp, start = 4.dp))
            }
        }
    }
}
