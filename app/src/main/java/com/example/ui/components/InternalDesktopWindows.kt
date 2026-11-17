package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.ScreenLockLandscape
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.unit.dp
import com.example.data.local.StickyNoteEntity
import com.example.data.model.DesktopWindowState
import com.example.data.model.GridDensity
import com.example.data.model.LauncherPreferences
import com.example.data.model.StickyNoteColor
import com.example.data.model.WindowSizeMode
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
import com.example.ui.theme.Win11NavyBg
import com.example.ui.theme.Win11TextPrimary
import com.example.ui.theme.Win11TextSecondary

@Composable
fun BoxScope.LauncherSettingsWindow(
    windowState: DesktopWindowState,
    preferences: LauncherPreferences,
    canWriteSystemSettings: Boolean,
    canDrawOverlays: Boolean,
    isDefaultLauncher: Boolean,
    isActive: Boolean,
    onFocus: () -> Unit,
    onMinimize: () -> Unit,
    onResizeCycle: () -> Unit,
    onResizeSelect: (WindowSizeMode) -> Unit,
    onClose: () -> Unit,
    onUpdateUserName: (String) -> Unit,
    onUpdateWeatherCity: (String) -> Unit,
    onToggleShowLabels: (Boolean) -> Unit,
    onSelectGridDensity: (GridDensity) -> Unit,
    onSelectDefaultWindowMode: (WindowSizeMode) -> Unit,
    onToggleForceLandscapeApps: (Boolean) -> Unit,
    onToggleFreeformBounds: (Boolean) -> Unit,
    onToggleKeepTaskbarOverApps: (Boolean) -> Unit,
    onToggleAnchorBottomEdge: (Boolean) -> Unit,
    onRequestWriteSettingsPermission: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onOpenDeveloperSettings: () -> Unit,
    onOpenDefaultHomeSettings: () -> Unit,
    onOpenNavigationSettings: () -> Unit,
    onOpenFreeformGuide: () -> Unit
) {
    var nameInput by remember(preferences.userDisplayName) { mutableStateOf(preferences.userDisplayName) }
    var cityInput by remember(preferences.weatherCity) { mutableStateOf(preferences.weatherCity) }

    DesktopWindowFrame(
        windowId = windowState.windowId,
        title = "Personalização do Fluent 11",
        icon = Icons.Rounded.Tune,
        sizeMode = windowState.sizeMode,
        isActive = isActive,
        onFocus = onFocus,
        onMinimize = onMinimize,
        onResizeCycle = onResizeCycle,
        onResizeSelect = onResizeSelect,
        onClose = onClose
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Nome do perfil") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = cityInput,
                    onValueChange = { cityInput = it },
                    label = { Text("Cidade do clima") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        onUpdateUserName(nameInput)
                        onUpdateWeatherCity(cityInput)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Win11AccentCyan,
                        contentColor = Win11NavyBg
                    )
                ) {
                    Text("Salvar perfil e cidade")
                }
            }

            Surface(
                color = Win11CardSurface,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Win11BorderSubtle, RoundedCornerShape(10.dp))
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Exibir nomes nos ícones da área de trabalho",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Win11TextPrimary
                        )
                        Switch(
                            checked = preferences.showDesktopLabels,
                            onCheckedChange = onToggleShowLabels,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Win11NavyBg,
                                checkedTrackColor = Win11AccentCyan
                            )
                        )
                    }

                    Text(
                        text = "Tamanho da grade",
                        style = MaterialTheme.typography.bodySmall,
                        color = Win11TextSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GridDensity.entries.forEach { density ->
                            FilterChip(
                                selected = preferences.gridDensity == density,
                                onClick = { onSelectGridDensity(density) },
                                label = { Text(density.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Win11AccentCyan,
                                    selectedLabelColor = Win11NavyBg
                                )
                            )
                        }
                    }
                }
            }

            Surface(
                color = Win11CardSurface,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Win11BorderSubtle, RoundedCornerShape(10.dp))
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Janelas e Orientação dos Aplicativos",
                        style = MaterialTheme.typography.titleSmall,
                        color = Win11TextPrimary
                    )

                    Text(
                        text = "Tamanho padrão ao abrir aplicativos (Modo Livre)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Win11TextSecondary
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        WindowSizeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = preferences.defaultWindowMode == mode,
                                onClick = { onSelectDefaultWindowMode(mode) },
                                label = { Text(mode.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Win11AccentCyan,
                                    selectedLabelColor = Win11NavyBg
                                )
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Manter apps na horizontal",
                                style = MaterialTheme.typography.labelMedium,
                                color = Win11TextPrimary
                            )
                            Text(
                                text = "Trava a rotação do sistema ao abrir aplicativos",
                                style = MaterialTheme.typography.bodySmall,
                                color = Win11TextSecondary
                            )
                        }
                        Switch(
                            checked = preferences.forceLandscapeApps,
                            onCheckedChange = onToggleForceLandscapeApps,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Win11NavyBg,
                                checkedTrackColor = Win11AccentCyan
                            )
                        )
                    }

                    if (!canWriteSystemSettings) {
                        Surface(
                            onClick = onRequestWriteSettingsPermission,
                            color = Win11AccentCyan.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Win11AccentCyan, RoundedCornerShape(8.dp))
                                .testTag("settings_grant_write_settings_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ScreenLockLandscape,
                                    contentDescription = null,
                                    tint = Win11AccentCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Permitir controle de rotação do sistema",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Win11AccentCyan
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ativar limites de Janela Livre (Freeform)",
                                style = MaterialTheme.typography.labelMedium,
                                color = Win11TextPrimary
                            )
                            Text(
                                text = "Permite que apps abram como janelas redimensionáveis quando Janelas de forma livre estiver ativo nas Opções do Desenvolvedor",
                                style = MaterialTheme.typography.bodySmall,
                                color = Win11TextSecondary
                            )
                        }
                        Switch(
                            checked = preferences.useFreeformLaunchBounds,
                            onCheckedChange = onToggleFreeformBounds,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Win11NavyBg,
                                checkedTrackColor = Win11AccentCyan
                            )
                        )
                    }

                    Surface(
                        onClick = onOpenFreeformGuide,
                        color = Win11NavyBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Win11BorderSubtle, RoundedCornerShape(8.dp))
                            .testTag("settings_freeform_guide_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PictureInPictureAlt,
                                    contentDescription = null,
                                    tint = Win11AccentCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Guia: limitar apps acima da Barra (Janela Livre)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Win11TextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Ver guia",
                                style = MaterialTheme.typography.labelMedium,
                                color = Win11AccentCyan
                            )
                        }
                    }

                    Surface(
                        onClick = onRequestOverlayPermission,
                        color = if (canDrawOverlays) Win11AccentCyan.copy(alpha = 0.14f) else Win11NavyBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (canDrawOverlays) Win11AccentCyan else Win11BorderSubtle,
                                RoundedCornerShape(8.dp)
                            )
                            .testTag("settings_grant_overlay_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Layers,
                                    contentDescription = null,
                                    tint = Win11AccentCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Barra flutuante sobre apps nativos",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Win11TextPrimary
                                    )
                                    Text(
                                        text = if (canDrawOverlays) {
                                            "Permissão ativa: exibe [—] [□] [✕] no topo ao abrir apps"
                                        } else {
                                            "Conceda \"Exibir sobre outros apps\" para mostrar a barra de janela"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Win11TextSecondary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (canDrawOverlays) "Ativa" else "Permitir",
                                style = MaterialTheme.typography.labelMedium,
                                color = Win11AccentCyan
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Manter Barra de Tarefas sobre apps",
                                style = MaterialTheme.typography.labelMedium,
                                color = Win11TextPrimary
                            )
                            Text(
                                text = "Mantém a barra inferior com Iniciar, apps e relógio visível com apps abertos",
                                style = MaterialTheme.typography.bodySmall,
                                color = Win11TextSecondary
                            )
                        }
                        Switch(
                            checked = preferences.keepTaskbarOverApps,
                            onCheckedChange = onToggleKeepTaskbarOverApps,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Win11NavyBg,
                                checkedTrackColor = Win11AccentCyan
                            ),
                            modifier = Modifier.testTag("settings_keep_taskbar_switch")
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Colar Barra na borda 0px (modo Gestos)",
                                style = MaterialTheme.typography.labelMedium,
                                color = Win11TextPrimary
                            )
                            Text(
                                text = "Encosta a Barra de Tarefas no último pixel inferior após mudar o tablet para Gestos",
                                style = MaterialTheme.typography.bodySmall,
                                color = Win11TextSecondary
                            )
                        }
                        Switch(
                            checked = preferences.anchorTaskbarAtBottomEdge,
                            onCheckedChange = onToggleAnchorBottomEdge,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Win11NavyBg,
                                checkedTrackColor = Win11AccentCyan
                            ),
                            modifier = Modifier.testTag("settings_anchor_bottom_switch")
                        )
                    }

                    Surface(
                        onClick = onOpenNavigationSettings,
                        color = Win11NavyBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Win11BorderSubtle, RoundedCornerShape(8.dp))
                            .testTag("settings_gesture_nav_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Fullscreen,
                                    contentDescription = null,
                                    tint = Win11AccentCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Ocultar botões do Android (Gestos)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Win11TextPrimary
                                    )
                                    Text(
                                        text = "Já ocultos no Fluent 11. Para retirá-los também nos outros apps, ative Navegação por Gestos nas configurações do tablet.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Win11TextSecondary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Configurar",
                                style = MaterialTheme.typography.labelMedium,
                                color = Win11AccentCyan
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = onOpenDeveloperSettings) {
                            Text("Opções do Desenvolvedor", color = Win11AccentCyan)
                        }
                        Surface(
                            onClick = onOpenDefaultHomeSettings,
                            color = if (isDefaultLauncher) Win11AccentCyan.copy(alpha = 0.16f) else Win11NavyBg,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    if (isDefaultLauncher) Win11AccentCyan else Win11BorderSubtle,
                                    RoundedCornerShape(8.dp)
                                )
                                .testTag("settings_default_home_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Home,
                                    contentDescription = null,
                                    tint = Win11AccentCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isDefaultLauncher) "Launcher Padrão ✓" else "Definir como Padrão",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Win11TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BoxScope.NotesDesktopWindow(
    windowState: DesktopWindowState,
    stickyNotes: List<StickyNoteEntity>,
    isActive: Boolean,
    onFocus: () -> Unit,
    onMinimize: () -> Unit,
    onResizeCycle: () -> Unit,
    onResizeSelect: (WindowSizeMode) -> Unit,
    onClose: () -> Unit,
    onAddStickyNote: (String, StickyNoteColor) -> Unit,
    onDeleteStickyNote: (Long) -> Unit
) {
    var draftText by remember { mutableStateOf("") }
    var draftColor by remember { mutableStateOf(StickyNoteColor.BLUE) }

    DesktopWindowFrame(
        windowId = windowState.windowId,
        title = "Notas Rápidas",
        icon = Icons.Rounded.StickyNote2,
        sizeMode = windowState.sizeMode,
        isActive = isActive,
        onFocus = onFocus,
        onMinimize = onMinimize,
        onResizeCycle = onResizeCycle,
        onResizeSelect = onResizeSelect,
        onClose = onClose
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = draftText,
                    onValueChange = { draftText = it },
                    placeholder = { Text("Nova anotação rápida...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                StickyNoteColor.entries.forEach { colorOption ->
                    val accent = when (colorOption) {
                        StickyNoteColor.AMBER -> NoteAmberAccent
                        StickyNoteColor.BLUE -> NoteBlueAccent
                        StickyNoteColor.MINT -> NoteMintAccent
                        StickyNoteColor.LAVENDER -> NoteLavenderAccent
                    }
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(accent)
                            .border(
                                width = if (draftColor == colorOption) 2.dp else 1.dp,
                                color = if (draftColor == colorOption) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { draftColor = colorOption }
                    )
                }
                Button(
                    onClick = {
                        if (draftText.isNotBlank()) {
                            onAddStickyNote(draftText, draftColor)
                            draftText = ""
                        }
                    },
                    enabled = draftText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Win11AccentCyan,
                        contentColor = Win11NavyBg
                    )
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Adicionar nota")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                contentPadding = PaddingValues(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(stickyNotes, key = { it.id }) { note ->
                    val token = StickyNoteColor.fromKey(note.colorKey)
                    val (bg, accent) = when (token) {
                        StickyNoteColor.AMBER -> NoteAmberBg to NoteAmberAccent
                        StickyNoteColor.BLUE -> NoteBlueBg to NoteBlueAccent
                        StickyNoteColor.MINT -> NoteMintBg to NoteMintAccent
                        StickyNoteColor.LAVENDER -> NoteLavenderBg to NoteLavenderAccent
                    }
                    Surface(
                        color = bg,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(28.dp)
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
                            IconButton(onClick = { onDeleteStickyNote(note.id) }) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Excluir nota",
                                    tint = Win11TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
