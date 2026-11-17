package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.DesktopItemEntity
import com.example.data.model.InstalledApp
import com.example.ui.theme.Win11AccentCyan
import com.example.ui.theme.Win11CardSurface
import com.example.ui.theme.Win11MicaSurface
import com.example.ui.theme.Win11NavyBg
import com.example.ui.theme.Win11TextPrimary
import com.example.ui.theme.Win11TextSecondary

@Composable
fun DesktopContextMenuDialog(
    onDismiss: () -> Unit,
    onCreateFolder: () -> Unit,
    onAddApp: () -> Unit,
    onOpenNotes: () -> Unit,
    onRefreshApps: () -> Unit,
    onOpenSettings: () -> Unit,
    onSetDefaultLauncher: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Win11MicaSurface,
        title = {
            Text(
                text = "Área de Trabalho",
                style = MaterialTheme.typography.titleMedium,
                color = Win11TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ContextOptionRow(
                    icon = Icons.Rounded.CreateNewFolder,
                    label = "Nova pasta",
                    onClick = { onDismiss(); onCreateFolder() },
                    testTag = "ctx_new_folder"
                )
                ContextOptionRow(
                    icon = Icons.Rounded.Apps,
                    label = "Adicionar aplicativo à área de trabalho",
                    onClick = { onDismiss(); onAddApp() },
                    testTag = "ctx_add_app"
                )
                ContextOptionRow(
                    icon = Icons.Rounded.StickyNote2,
                    label = "Abrir Notas Rápidas (Janela)",
                    onClick = { onDismiss(); onOpenNotes() },
                    testTag = "ctx_open_notes"
                )
                ContextOptionRow(
                    icon = Icons.Rounded.Refresh,
                    label = "Atualizar lista de aplicativos",
                    onClick = { onDismiss(); onRefreshApps() },
                    testTag = "ctx_refresh_apps"
                )
                ContextOptionRow(
                    icon = Icons.Rounded.Tune,
                    label = "Personalizar launcher",
                    onClick = { onDismiss(); onOpenSettings() },
                    testTag = "ctx_launcher_settings"
                )
                ContextOptionRow(
                    icon = Icons.Rounded.Home,
                    label = "Definir como launcher padrão",
                    onClick = { onDismiss(); onSetDefaultLauncher() },
                    testTag = "ctx_default_launcher"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar", color = Win11AccentCyan)
            }
        }
    )
}

@Composable
private fun ContextOptionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Win11CardSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Win11AccentCyan,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Win11TextPrimary
        )
    }
}

@Composable
fun TextInputDialog(
    title: String,
    initialValue: String,
    label: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Win11MicaSurface,
        title = { Text(title, color = Win11TextPrimary) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("text_input_dialog_field")
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(value) },
                enabled = value.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Win11AccentCyan,
                    contentColor = Win11NavyBg
                )
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Win11TextSecondary)
            }
        }
    )
}

@Composable
fun MoveToFolderDialog(
    folders: List<DesktopItemEntity>,
    onDismiss: () -> Unit,
    onSelectFolder: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Win11MicaSurface,
        title = { Text("Mover para pasta", color = Win11TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                folders.forEach { folder ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Win11CardSurface)
                            .clickable { onSelectFolder(folder.id) }
                            .padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Folder,
                            contentDescription = null,
                            tint = Win11AccentCyan
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(folder.title, color = Win11TextPrimary)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Win11TextSecondary)
            }
        }
    )
}

@Composable
fun AddAppPickerDialog(
    installedApps: List<InstalledApp>,
    targetFolderTitle: String?,
    onDismiss: () -> Unit,
    onSelectApp: (InstalledApp) -> Unit
) {
    var filter by remember { mutableStateOf("") }
    val filtered = remember(filter, installedApps) {
        if (filter.isBlank()) installedApps
        else installedApps.filter { it.label.contains(filter.trim(), ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Win11MicaSurface,
        title = {
            Text(
                text = if (targetFolderTitle != null) "Adicionar à pasta \"$targetFolderTitle\""
                else "Adicionar aplicativo à área de trabalho",
                color = Win11TextPrimary
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = filter,
                    onValueChange = { filter = it },
                    placeholder = { Text("Filtrar aplicativos...") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = Win11AccentCyan)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.heightIn(max = 260.dp)
                ) {
                    items(filtered, key = { it.packageName }) { app ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Win11CardSurface)
                                .clickable { onSelectApp(app) }
                                .padding(10.dp)
                        ) {
                            AppIconTile(app = app, sizeDp = 30)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Win11TextPrimary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar", color = Win11AccentCyan)
            }
        }
    )
}

@Composable
fun OverlayPermissionPromptDialog(
    targetAppLabel: String,
    onGrantPermission: () -> Unit,
    onContinueWithoutOverlay: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Win11MicaSurface,
        modifier = Modifier.testTag("overlay_permission_dialog"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = null,
                    tint = Win11AccentCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Ativar barra sobre aplicativos",
                    style = MaterialTheme.typography.titleMedium,
                    color = Win11TextPrimary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Para manter a Barra de Tarefas fixa embaixo e mostrar os botões de minimizar, redimensionar e fechar ao abrir \"$targetAppLabel\", ative a permissão \"Sobrepor a outros apps\" para o Fluent 11.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Win11TextPrimary
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Win11CardSurface)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Como ativar:",
                        style = MaterialTheme.typography.labelLarge,
                        color = Win11AccentCyan
                    )
                    Text(
                        text = "1. Toque em \"Ativar agora\" abaixo.\n2. Ative a chave para o Fluent 11.\n3. Volte para abrir o aplicativo automaticamente com a barra ativa.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Win11TextSecondary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onGrantPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Win11AccentCyan,
                    contentColor = Win11NavyBg
                ),
                modifier = Modifier.testTag("overlay_prompt_grant_btn")
            ) {
                Text("Ativar agora")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onContinueWithoutOverlay,
                modifier = Modifier.testTag("overlay_prompt_skip_btn")
            ) {
                Text("Abrir sem barra", color = Win11TextSecondary)
            }
        }
    )
}

@Composable
fun GestureNavigationGuideDialog(
    anchorTaskbarAtBottomEdge: Boolean,
    onToggleAnchorBottomEdge: (Boolean) -> Unit,
    onOpenSystemSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Win11MicaSurface,
        modifier = Modifier.testTag("gesture_nav_guide_dialog"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Fullscreen,
                    contentDescription = null,
                    tint = Win11AccentCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Colocar barra no lugar dos botões",
                    style = MaterialTheme.typography.titleMedium,
                    color = Win11TextPrimary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Por segurança do Android, outros apps mantêm os 3 botões inferiores até você ativar a Navegação por Gestos no tablet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Win11TextSecondary
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Win11CardSurface)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Passo a passo no tablet / HyperOS:",
                        style = MaterialTheme.typography.labelLarge,
                        color = Win11AccentCyan
                    )
                    Text(
                        text = "1. Toque em \"Abrir Configurações\" abaixo.\n2. Entre em Tela inicial (ou Tela) → Navegação do sistema.\n3. Troque \"Botões\" por \"Gestos\" e marque \"Ocultar indicador de tela cheia\".",
                        style = MaterialTheme.typography.bodySmall,
                        color = Win11TextPrimary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Win11CardSurface)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Colar Barra na borda 0px",
                            style = MaterialTheme.typography.labelMedium,
                            color = Win11TextPrimary
                        )
                        Text(
                            text = "Ative após mudar para Gestos para encostar a barra no último pixel inferior",
                            style = MaterialTheme.typography.bodySmall,
                            color = Win11TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = anchorTaskbarAtBottomEdge,
                        onCheckedChange = onToggleAnchorBottomEdge,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Win11NavyBg,
                            checkedTrackColor = Win11AccentCyan
                        ),
                        modifier = Modifier.testTag("gesture_guide_anchor_switch")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onOpenSystemSettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Win11AccentCyan,
                    contentColor = Win11NavyBg
                ),
                modifier = Modifier.testTag("gesture_guide_open_settings_btn")
            ) {
                Text("Abrir Configurações")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("gesture_guide_close_btn")
            ) {
                Text("Fechar", color = Win11TextSecondary)
            }
        }
    )
}

@Composable
fun FreeformModeGuideDialog(
    useFreeformLaunchBounds: Boolean,
    onToggleFreeformBounds: (Boolean) -> Unit,
    onOpenDeveloperSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Win11MicaSurface,
        modifier = Modifier.testTag("freeform_guide_dialog"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.PictureInPictureAlt,
                    contentDescription = null,
                    tint = Win11AccentCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Apps acima da Barra (Janela Livre)",
                    style = MaterialTheme.typography.titleMedium,
                    color = Win11TextPrimary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "O Fluent 11 já envia o tamanho exato entre a barra superior e a Barra de Tarefas inferior ao abrir aplicativos. Para o Android respeitar esse limite em qualquer app, ative o modo Janela Livre no sistema.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Win11TextSecondary
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Win11CardSurface)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Como ativar no tablet / HyperOS:",
                        style = MaterialTheme.typography.labelLarge,
                        color = Win11AccentCyan
                    )
                    Text(
                        text = "1. Toque em \"Opções do Desenvolvedor\" abaixo.\n2. Ative \"Ativar janelas de forma livre\" e \"Forçar redimensionamento de atividades\".\n3. Volte ao Fluent 11 e abra o app — ele ficará delimitado acima da Barra de Tarefas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Win11TextPrimary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Win11CardSurface)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ajustar limites ao abrir apps",
                            style = MaterialTheme.typography.labelMedium,
                            color = Win11TextPrimary
                        )
                        Text(
                            text = "Envia área útil da Barra de Tarefas para cima automaticamente",
                            style = MaterialTheme.typography.bodySmall,
                            color = Win11TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = useFreeformLaunchBounds,
                        onCheckedChange = onToggleFreeformBounds,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Win11NavyBg,
                            checkedTrackColor = Win11AccentCyan
                        ),
                        modifier = Modifier.testTag("freeform_guide_bounds_switch")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onOpenDeveloperSettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Win11AccentCyan,
                    contentColor = Win11NavyBg
                ),
                modifier = Modifier.testTag("freeform_guide_dev_btn")
            ) {
                Text("Opções do Desenvolvedor")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("freeform_guide_close_btn")
            ) {
                Text("Fechar", color = Win11TextSecondary)
            }
        }
    )
}
