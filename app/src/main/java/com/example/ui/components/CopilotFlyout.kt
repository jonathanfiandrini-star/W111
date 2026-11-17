package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CopilotChatMessage
import com.example.data.model.CopilotUiState
import com.example.ui.theme.Win11AccentBlue
import com.example.ui.theme.Win11AccentCyan
import com.example.ui.theme.Win11BorderHighlight
import com.example.ui.theme.Win11BorderSubtle
import com.example.ui.theme.Win11CardElevated
import com.example.ui.theme.Win11CardSurface
import com.example.ui.theme.Win11ErrorCoral
import com.example.ui.theme.Win11MicaSurface
import com.example.ui.theme.Win11NavyBg
import com.example.ui.theme.Win11TextPrimary
import com.example.ui.theme.Win11TextSecondary

@Composable
fun CopilotFlyout(
    copilotState: CopilotUiState,
    onSendPrompt: (String) -> Unit,
    onClearChat: () -> Unit,
    onSaveToNotes: (String) -> Unit,
    onCopiedNotice: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    val quickPrompts = remember {
        listOf(
            "Sugira um app instalado para anotar ideias",
            "Crie uma lista de 4 tarefas para organizar meu dia",
            "Me dê dicas para trabalhar em modo desktop",
            "Resuma como organizar minhas pastas"
        )
    }

    LaunchedEffect(copilotState.messages.size, copilotState.messages.lastOrNull()?.text?.length) {
        if (copilotState.messages.isNotEmpty()) {
            listState.animateScrollToItem(copilotState.messages.lastIndex)
        }
    }

    Surface(
        color = Win11MicaSurface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 10.dp,
        shadowElevation = 22.dp,
        modifier = modifier
            .widthIn(min = 340.dp, max = 440.dp)
            .fillMaxWidth(0.46f)
            .fillMaxHeight(0.88f)
            .border(1.dp, Win11BorderHighlight, RoundedCornerShape(16.dp))
            .testTag("copilot_flyout")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.25f))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CopilotRingIcon(modifier = Modifier.size(26.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Copilot IA",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Win11TextPrimary
                        )
                        Text(
                            text = "Gemini 3.8 Flash",
                            style = MaterialTheme.typography.labelSmall,
                            color = Win11AccentCyan
                        )
                    }
                }

                if (copilotState.messages.isNotEmpty()) {
                    IconButton(
                        onClick = onClearChat,
                        modifier = Modifier.testTag("copilot_clear_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = "Limpar conversa",
                            tint = Win11TextSecondary
                        )
                    }
                }
            }

            HorizontalDivider(color = Win11BorderSubtle)

            // Message feed or Empty State
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (copilotState.messages.isEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = Win11AccentCyan,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Como posso ajudar hoje?",
                            style = MaterialTheme.typography.titleLarge,
                            color = Win11TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Peça sugestões com base nos seus apps instalados, rascunhos de notas ou respostas rápidas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Win11TextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            quickPrompts.forEach { prompt ->
                                Surface(
                                    onClick = { onSendPrompt(prompt) },
                                    color = Win11CardSurface,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Win11BorderSubtle, RoundedCornerShape(10.dp))
                                ) {
                                    Text(
                                        text = prompt,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Win11TextPrimary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(copilotState.messages, key = { it.id }) { msg ->
                            CopilotMessageBubble(
                                message = msg,
                                onSaveToNotes = { onSaveToNotes(msg.text) },
                                onCopy = {
                                    clipboard.setText(AnnotatedString(msg.text))
                                    onCopiedNotice()
                                }
                            )
                        }
                    }
                }
            }

            // Transparent Error Banner
            if (copilotState.errorMessage != null) {
                Surface(
                    color = Win11ErrorCoral.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .border(1.dp, Win11ErrorCoral.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .testTag("copilot_error_banner")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = Win11ErrorCoral,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = copilotState.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = Win11TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Quick chips row when chat already started
            if (copilotState.messages.isNotEmpty() && !copilotState.isLoading) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    quickPrompts.take(2).forEach { prompt ->
                        AssistChip(
                            onClick = { onSendPrompt(prompt) },
                            label = {
                                Text(
                                    text = prompt,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Win11CardSurface,
                                labelColor = Win11TextPrimary
                            )
                        )
                    }
                }
            }

            HorizontalDivider(color = Win11BorderSubtle)

            // Input bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.20f))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = "Pergunte qualquer coisa ao Copilot...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Win11TextSecondary
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(22.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Win11AccentCyan,
                        unfocusedBorderColor = Win11BorderSubtle,
                        focusedContainerColor = Win11CardSurface,
                        unfocusedContainerColor = Win11CardSurface,
                        focusedTextColor = Win11TextPrimary,
                        unfocusedTextColor = Win11TextPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("copilot_input_field")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (inputText.isNotBlank() && !copilotState.isLoading) Win11AccentCyan
                            else Win11CardSurface
                        )
                        .clickable(enabled = inputText.isNotBlank() && !copilotState.isLoading) {
                            val prompt = inputText.trim()
                            inputText = ""
                            onSendPrompt(prompt)
                        }
                        .testTag("copilot_send_button")
                ) {
                    if (copilotState.isLoading) {
                        CircularProgressIndicator(
                            color = Win11AccentCyan,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Enviar para o Copilot",
                            tint = if (inputText.isNotBlank()) Win11NavyBg else Win11TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CopilotMessageBubble(
    message: CopilotChatMessage,
    onSaveToNotes: () -> Unit,
    onCopy: () -> Unit
) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    Column(
        horizontalAlignment = alignment,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            color = if (message.isUser) Win11AccentBlue.copy(alpha = 0.85f) else Win11CardElevated,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .widthIn(max = 360.dp)
                .border(
                    1.dp,
                    if (message.isUser) Win11AccentCyan.copy(alpha = 0.4f) else Win11BorderSubtle,
                    RoundedCornerShape(12.dp)
                )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.text.ifBlank { if (message.isStreaming) "Pensando..." else "" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Win11TextPrimary
                )
                if (!message.isUser && !message.isStreaming && message.text.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onSaveToNotes,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.BookmarkAdd,
                                contentDescription = null,
                                tint = Win11AccentCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Salvar nas Notas",
                                style = MaterialTheme.typography.labelSmall,
                                color = Win11AccentCyan
                            )
                        }
                        TextButton(
                            onClick = onCopy,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = null,
                                tint = Win11TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Copiar",
                                style = MaterialTheme.typography.labelSmall,
                                color = Win11TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CopilotRingIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.18f
        drawCircle(
            brush = Brush.sweepGradient(
                listOf(
                    Win11AccentCyan,
                    Win11AccentBlue,
                    Color(0xFF818CF8),
                    Win11AccentCyan
                )
            ),
            radius = (size.minDimension - stroke) / 2f,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        drawCircle(
            color = Win11AccentCyan.copy(alpha = 0.85f),
            radius = size.minDimension * 0.17f
        )
    }
}
