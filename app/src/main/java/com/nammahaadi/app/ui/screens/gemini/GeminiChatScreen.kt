package com.nammahaadi.app.ui.screens.gemini

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.ai.client.generativeai.GenerativeModel
import com.nammahaadi.app.BuildConfig
import com.nammahaadi.app.data.model.PathModel
import com.nammahaadi.app.viewmodel.MapViewModel
import kotlinx.coroutines.launch

data class ChatMessage(val text: String, val isUser: Boolean)

private const val GREETING_EN =
    "Namaskara! 🙏 I'm your Namma-Haadi assistant. " +
            "Ask me about path safety, directions, or monsoon conditions!"

private const val GREETING_KN =
    "ನಮಸ್ಕಾರ! 🙏 ನಾನು ನಿಮ್ಮ ನಮ್ಮ-ಹಾದಿ ಸಹಾಯಕ. " +
            "ದಾರಿ ಸುರಕ್ಷತೆ, ನಿರ್ದೇಶನ ಅಥವಾ ಮಳೆಗಾಲದ ಪರಿಸ್ಥಿತಿ ಬಗ್ಗೆ ಕೇಳಿ!"

private val SUGGESTIONS_EN = listOf(
    "🌊 Any flooded paths nearby?",
    "🌙 Safe to travel after 6 PM?",
    "🛤️ Which paths are safe today?",
    "🌧️ Monsoon safety tips",
    "📍 How do I trace a new path?"
)

private val SUGGESTIONS_KN = listOf(
    "🌊 ಹತ್ತಿರದಲ್ಲಿ ಪ್ರವಾಹ ಇದೆಯೇ?",
    "🌙 ಸಂಜೆ 6 ಗಂಟೆ ನಂತರ ಸುರಕ್ಷಿತವೇ?",
    "🛤️ ಇಂದು ಯಾವ ದಾರಿ ಸುರಕ್ಷಿತ?",
    "🌧️ ಮಳೆಗಾಲದ ಸುರಕ್ಷತಾ ಸಲಹೆ",
    "📍 ಹೊಸ ದಾರಿ ಹೇಗೆ ಸೇರಿಸುವುದು?"
)

@Composable
fun GeminiChatScreen(mapVm: MapViewModel = viewModel()) {
    val paths by mapVm.paths.collectAsStateWithLifecycle()

    var isKannada by remember { mutableStateOf(false) }

    // ✅ FIX 1: Greeting is derived from isKannada, not stored as fixed state.
    // When toggle changes, the greeting shown updates automatically.
    val greeting = if (isKannada) GREETING_KN else GREETING_EN

    // Messages list — first item always reflects current language greeting
    var messages by remember {
        mutableStateOf(listOf(ChatMessage(GREETING_EN, isUser = false)))
    }

    // ✅ FIX 1 cont: When language toggles, replace the greeting message
    LaunchedEffect(isKannada) {
        val newGreeting = if (isKannada) GREETING_KN else GREETING_EN
        // Replace only the first message (the greeting), keep rest of chat intact
        messages = listOf(ChatMessage(newGreeting, isUser = false)) +
                messages.drop(1)
    }

    var input by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val model = remember {
        GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    fun buildPathContext(paths: List<PathModel>): String {
        if (paths.isEmpty()) return "No paths have been mapped yet in this area."
        val summary = paths.joinToString("\n") { path ->
            "- \"${path.name}\": ${path.status} " +
                    "(${if (path.safeAfterDark) "safe after dark" else "unsafe after dark"})"
        }
        return "Current community-reported path conditions:\n$summary"
    }

    fun sendMessage(userMsg: String) {
        if (userMsg.isBlank() || isLoading) return
        messages = messages + ChatMessage(userMsg, isUser = true)
        input = ""
        isLoading = true
        scope.launch {
            try {
                val languageInstruction = if (isKannada)
                    "IMPORTANT: You MUST reply only in Kannada (ಕನ್ನಡ) language. Do not use English."
                else
                    "IMPORTANT: You MUST reply only in English language. Do not use Kannada."

                val prompt = """
                    You are the Namma-Haadi assistant helping rural villagers in
                    Karnataka, India navigate local footpaths and shortcuts safely.
                    Answer in simple, friendly language. Keep responses concise
                    (2-4 sentences). Use relevant emojis.
                    
                    $languageInstruction
                    
                    ${buildPathContext(paths)}
                    
                    User question: $userMsg
                """.trimIndent()

                val response = model.generateContent(prompt)
                messages = messages + ChatMessage(
                    response.text ?: if (isKannada)
                        "ಕ್ಷಮಿಸಿ, ಅರ್ಥವಾಗಲಿಲ್ಲ. ದಯವಿಟ್ಟು ಮತ್ತೆ ಪ್ರಯತ್ನಿಸಿ."
                    else
                        "Sorry, I could not understand. Please try again.",
                    isUser = false
                )
            } catch (e: Exception) {
                messages = messages + ChatMessage(
                    if (isKannada)
                        "❌ ಸಂಪರ್ಕ ಸಮಸ್ಯೆ. ನಿಮ್ಮ ಇಂಟರ್ನೆಟ್ ಪರಿಶೀಲಿಸಿ."
                    else
                        "❌ Could not reach assistant. Check your connection and try again.",
                    isUser = false
                )
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {

        // ── Header ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🤖", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isKannada) "ಹಾದಿ ಸಹಾಯಕ" else "Haadi Assistant",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                // ✅ FIX 2: Show path count as soon as paths arrive from Firestore.
                // "Loading" only shown briefly on first launch; updates reactively.
                Text(
                    text = when {
                        paths.isEmpty() ->
                            if (isKannada) "ದಾರಿ ಮಾಹಿತಿ ತರುತ್ತಿದ್ದೇವೆ..."
                            else "Loading path data..."
                        isKannada -> "✅ ${paths.size} ದಾರಿಗಳು ಲೋಡ್ ಆಗಿದೆ"
                        else -> "✅ ${paths.size} paths loaded"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (paths.isEmpty()) Color.Gray else Color(0xFF4CAF50)
                )
            }

            // ── EN / ಕನ್ನಡ Toggle ─────────────────────────────────────────
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1976D2))
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Button(
                        onClick = { isKannada = false },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isKannada) Color.White else Color.Transparent,
                            contentColor = if (!isKannada) Color(0xFF1976D2) else Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Text("EN", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { isKannada = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isKannada) Color.White else Color.Transparent,
                            contentColor = if (isKannada) Color(0xFF1976D2) else Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Text("ಕನ್ನಡ", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        HorizontalDivider()

        // ── Chat Messages ──────────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages) { msg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (msg.isUser)
                        Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (msg.isUser)
                                Color(0xFF1976D2) else Color(0xFFF0F4F8)
                        ),
                        shape = RoundedCornerShape(
                            topStart = 16.dp, topEnd = 16.dp,
                            bottomStart = if (msg.isUser) 16.dp else 4.dp,
                            bottomEnd = if (msg.isUser) 4.dp else 16.dp
                        ),
                        modifier = Modifier.widthIn(max = 280.dp)
                    ) {
                        Text(
                            text = msg.text,
                            modifier = Modifier.padding(12.dp, 10.dp),
                            color = if (msg.isUser) Color.White else Color(0xFF1A1A1A),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            if (isLoading) {
                item {
                    Row {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)),
                            shape = RoundedCornerShape(
                                topStart = 16.dp, topEnd = 16.dp,
                                bottomStart = 4.dp, bottomEnd = 16.dp
                            )
                        ) {
                            Text(
                                text = "✦ ${if (isKannada) "ಯೋಚಿಸುತ್ತಿದ್ದೇನೆ..." else "Thinking..."}",
                                modifier = Modifier.padding(12.dp, 10.dp),
                                color = Color(0xFF888888),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        // ── Suggestion Chips ───────────────────────────────────────────────
        if (!isLoading) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val suggestions = if (isKannada) SUGGESTIONS_KN else SUGGESTIONS_EN
                suggestions.forEach { suggestion ->
                    SuggestionChip(
                        onClick = { sendMessage(suggestion) },
                        label = {
                            Text(suggestion, style = MaterialTheme.typography.bodySmall)
                        }
                    )
                }
            }
        }

        HorizontalDivider()

        // ── Input Row ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(if (isKannada) "ದಾರಿ ಸುರಕ್ಷತೆ ಬಗ್ಗೆ ಕೇಳಿ..." else "Ask about path safety...")
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { sendMessage(input.trim()) },
                enabled = input.isNotBlank() && !isLoading,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
            ) {
                Text(if (isLoading) "..." else if (isKannada) "ಕಳುಹಿಸಿ" else "Send")
            }
        }
    }
}