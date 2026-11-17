package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.StickyNoteEntity
import com.example.data.model.StickyNoteColor
import com.example.data.model.SystemTelemetry
import com.example.data.model.WeatherState
import com.example.ui.theme.NoteAmberAccent
import com.example.ui.theme.NoteAmberBg
import com.example.ui.theme.NoteBlueAccent
import com.example.ui.theme.NoteBlueBg
import com.example.ui.theme.NoteLavenderAccent
import com.example.ui.theme.NoteLavenderBg
import com.example.ui.theme.NoteMintAccent
import com.example.ui.theme.NoteMintBg
import com.example.ui.theme.Win11AccentCyan
import com.example.ui.theme.Win11BorderSubtle
import com.example.ui.theme.Win11CardSurface
import com.example.ui.theme.Win11MicaSurface
import com.example.ui.theme.Win11NavyBg
import com.example.ui.theme.Win11TextPrimary
import com.example.ui.theme.Win11TextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun WidgetsPanelFlyout(
    currentTimeMillis: Long,
    weatherCity: String,
    weatherState: WeatherState,
    telemetry: SystemTelemetry,
    stickyNotes: List<StickyNoteEntity>,
    onRefreshWeather: () -> Unit,
    onChangeWeatherCity: (String) -> Unit,
    onAddStickyNote: (String, StickyNoteColor) -> Unit,
    onUpdateStickyNote: (StickyNoteEntity) -> Unit,
    onDeleteStickyNote: (Long) -> Unit,
    onExpandNotes: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCityDialog by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<StickyNoteEntity?>(null) }
    var showCreateNoteDialog by remember { mutableStateOf(false) }

    Surface(
        color = Win11MicaSurface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 8.dp,
        shadowElevation = 20.dp,
        modifier = modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth(0.55f)
            .fillMaxHeight(0.92f)
            .border(1.dp, Win11BorderSubtle, RoundedCornerShape(16.dp))
            .testTag("widgets_panel_flyout")
    ) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                ClockAndCalendarWidget(currentTimeMillis = currentTimeMillis)
            }

            item {
                WeatherWidgetCard(
                    weatherCity = weatherCity,
                    weatherState = weatherState,
                    onRefresh = onRefreshWeather,
                    onEditCity = { showCityDialog = true }
                )
            }

            item {
                SystemMonitorWidget(telemetry = telemetry)
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Notas Rápidas",
                        style = MaterialTheme.typography.titleMedium,
                        color = Win11TextPrimary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = onExpandNotes,
                            modifier = Modifier.size(34.dp).testTag("widgets_expand_notes_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.OpenInNew,
                                contentDescription = "Abrir em janela",
                                tint = Win11AccentCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Surface(
                            onClick = { showCreateNoteDialog = true },
                            color = Win11AccentCyan,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("widgets_add_note_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = stringResource(R.string.cd_add_note),
                                    tint = Win11NavyBg,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Nova nota",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Win11NavyBg
                                )
                            }
                        }
                    }
                }
            }

            if (stickyNotes.isEmpty()) {
                item {
                    Surface(
                        color = Win11CardSurface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Win11BorderSubtle, RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = "Nenhuma nota rápida ainda. Crie um lembrete acima.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Win11TextSecondary,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            } else {
                items(stickyNotes, key = { it.id }) { note ->
                    StickyNoteCard(
                        note = note,
                        onEdit = { editingNote = note },
                        onDelete = { onDeleteStickyNote(note.id) }
                    )
                }
            }
        }
    }

    if (showCityDialog) {
        var cityInput by remember(weatherCity) { mutableStateOf(weatherCity) }
        AlertDialog(
            onDismissRequest = { showCityDialog = false },
            containerColor = Win11CardSurface,
            title = { Text("Alterar cidade do clima", color = Win11TextPrimary) },
            text = {
                OutlinedTextField(
                    value = cityInput,
                    onValueChange = { cityInput = it },
                    label = { Text("Nome da cidade") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onChangeWeatherCity(cityInput)
                        showCityDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Win11AccentCyan, contentColor = Win11NavyBg)
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCityDialog = false }) {
                    Text("Cancelar", color = Win11TextSecondary)
                }
            }
        )
    }

    if (showCreateNoteDialog) {
        StickyNoteEditorDialog(
            initialContent = "",
            initialColor = StickyNoteColor.BLUE,
            title = "Nova nota rápida",
            onDismiss = { showCreateNoteDialog = false },
            onSave = { content, color ->
                onAddStickyNote(content, color)
                showCreateNoteDialog = false
            }
        )
    }

    editingNote?.let { note ->
        StickyNoteEditorDialog(
            initialContent = note.content,
            initialColor = StickyNoteColor.fromKey(note.colorKey),
            title = "Editar nota",
            onDismiss = { editingNote = null },
            onSave = { content, color ->
                onUpdateStickyNote(note.copy(content = content, colorKey = color.key))
                editingNote = null
            }
        )
    }
}

@Composable
private fun ClockAndCalendarWidget(currentTimeMillis: Long) {
    val timeText = remember(currentTimeMillis) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(currentTimeMillis))
    }
    val dateHeader = remember(currentTimeMillis) {
        SimpleDateFormat("EEEE, dd 'de' MMMM", Locale("pt", "BR"))
            .format(Date(currentTimeMillis))
            .replaceFirstChar { it.uppercase() }
    }
    val calendar = remember(currentTimeMillis) {
        Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
    }
    val todayDay = calendar.get(Calendar.DAY_OF_MONTH)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfMonthOffset = remember(currentTimeMillis) {
        val c = Calendar.getInstance().apply {
            timeInMillis = currentTimeMillis
            set(Calendar.DAY_OF_MONTH, 1)
        }
        c.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sunday
    }

    Surface(
        color = Win11CardSurface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Win11BorderSubtle, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(0.95f)) {
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontFamily = com.example.ui.theme.JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Win11AccentCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateHeader,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Win11TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Compact 7-column calendar grid
            Column(modifier = Modifier.weight(1.25f)) {
                val weekHeaders = listOf("D", "S", "T", "Q", "Q", "S", "S")
                Row(modifier = Modifier.fillMaxWidth()) {
                    weekHeaders.forEach { d ->
                        Text(
                            text = d,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = Win11TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                val totalSlots = firstDayOfMonthOffset + daysInMonth
                val rows = (totalSlots + 6) / 7
                for (r in 0 until rows) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (c in 0 until 7) {
                            val dayNum = r * 7 + c - firstDayOfMonthOffset + 1
                            val isValid = dayNum in 1..daysInMonth
                            val isToday = isValid && dayNum == todayDay
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(18.dp)
                                    .clip(CircleShape)
                                    .background(if (isToday) Win11AccentCyan else Color.Transparent)
                            ) {
                                if (isValid) {
                                    Text(
                                        text = dayNum.toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = if (isToday) Win11NavyBg else Win11TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherWidgetCard(
    weatherCity: String,
    weatherState: WeatherState,
    onRefresh: () -> Unit,
    onEditCity: () -> Unit
) {
    Surface(
        color = Win11CardSurface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Win11BorderSubtle, RoundedCornerShape(14.dp))
            .testTag("weather_widget_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onEditCity)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = weatherCity,
                        style = MaterialTheme.typography.titleSmall,
                        color = Win11TextPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Alterar cidade",
                        tint = Win11AccentCyan,
                        modifier = Modifier.size(14.dp)
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = stringResource(R.string.cd_refresh_weather),
                        tint = Win11TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            when (weatherState) {
                is WeatherState.Loading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = Win11AccentCyan,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Atualizando previsão...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Win11TextSecondary
                        )
                    }
                }
                is WeatherState.Success -> {
                    val weatherIcon = when {
                        weatherState.weatherCode >= 95 -> Icons.Rounded.Thunderstorm
                        weatherState.weatherCode in 51..82 -> Icons.Rounded.WaterDrop
                        weatherState.weatherCode in 2..48 -> Icons.Rounded.Cloud
                        !weatherState.isDay -> Icons.Rounded.NightsStay
                        else -> Icons.Rounded.WbSunny
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = weatherIcon,
                                contentDescription = weatherState.conditionLabel,
                                tint = Win11AccentCyan,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "${weatherState.temperatureCelsius}°C",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontFamily = com.example.ui.theme.JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Win11TextPrimary
                                )
                                Text(
                                    text = weatherState.conditionLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Win11TextSecondary
                                )
                            }
                        }
                        Text(
                            text = "Vento ${weatherState.windSpeedKmh} km/h",
                            style = MaterialTheme.typography.labelSmall,
                            color = Win11TextSecondary
                        )
                    }
                }
                is WeatherState.Error -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = weatherState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Win11TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onRefresh) {
                            Text("Tentar", color = Win11AccentCyan)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemMonitorWidget(telemetry: SystemTelemetry) {
    Surface(
        color = Win11CardSurface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Win11BorderSubtle, RoundedCornerShape(14.dp))
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = "Desempenho do Dispositivo",
                style = MaterialTheme.typography.titleSmall,
                color = Win11TextPrimary
            )

            val ramProgress = if (telemetry.ramTotalGb > 0f) {
                (telemetry.ramUsedGb / telemetry.ramTotalGb).coerceIn(0f, 1f)
            } else 0f

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Memory,
                    contentDescription = null,
                    tint = Win11AccentCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Memória RAM", style = MaterialTheme.typography.bodySmall, color = Win11TextPrimary)
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f / %.1f GB", telemetry.ramUsedGb, telemetry.ramTotalGb),
                            style = MaterialTheme.typography.labelSmall,
                            color = Win11TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { ramProgress },
                        color = Win11AccentCyan,
                        trackColor = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                    )
                }
            }

            val storageProgress = if (telemetry.storageTotalGb > 0f) {
                (telemetry.storageUsedGb / telemetry.storageTotalGb).coerceIn(0f, 1f)
            } else 0f

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Storage,
                    contentDescription = null,
                    tint = Win11AccentCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Armazenamento", style = MaterialTheme.typography.bodySmall, color = Win11TextPrimary)
                        Text(
                            text = String.format(Locale.getDefault(), "%.0f / %.0f GB", telemetry.storageUsedGb, telemetry.storageTotalGb),
                            style = MaterialTheme.typography.labelSmall,
                            color = Win11TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { storageProgress },
                        color = Win11AccentCyan,
                        trackColor = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun StickyNoteCard(
    note: StickyNoteEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colorToken = StickyNoteColor.fromKey(note.colorKey)
    val (bg, accent) = when (colorToken) {
        StickyNoteColor.AMBER -> NoteAmberBg to NoteAmberAccent
        StickyNoteColor.BLUE -> NoteBlueBg to NoteBlueAccent
        StickyNoteColor.MINT -> NoteMintBg to NoteMintAccent
        StickyNoteColor.LAVENDER -> NoteLavenderBg to NoteLavenderAccent
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .clickable(onClick = onEdit)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium,
                color = Win11TextPrimary,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Excluir nota",
                    tint = Win11TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun StickyNoteEditorDialog(
    initialContent: String,
    initialColor: StickyNoteColor,
    title: String,
    onDismiss: () -> Unit,
    onSave: (String, StickyNoteColor) -> Unit
) {
    var content by remember { mutableStateOf(initialContent) }
    var selectedColor by remember { mutableStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Win11CardSurface,
        title = { Text(title, color = Win11TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text("Escreva um lembrete rápido...") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StickyNoteColor.entries.forEach { colorOption ->
                        val accent = when (colorOption) {
                            StickyNoteColor.AMBER -> NoteAmberAccent
                            StickyNoteColor.BLUE -> NoteBlueAccent
                            StickyNoteColor.MINT -> NoteMintAccent
                            StickyNoteColor.LAVENDER -> NoteLavenderAccent
                        }
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(accent)
                                .border(
                                    width = if (selectedColor == colorOption) 2.5.dp else 1.dp,
                                    color = if (selectedColor == colorOption) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = colorOption }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(content, selectedColor) },
                enabled = content.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Win11AccentCyan, contentColor = Win11NavyBg)
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Win11TextSecondary)
            }
        }
    )
}
