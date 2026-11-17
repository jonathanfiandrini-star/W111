package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.DesktopItemEntity
import com.example.data.local.StickyNoteEntity
import com.example.data.model.DesktopItemType
import com.example.data.model.InstalledApp
import com.example.ui.theme.Win11AccentCyan
import com.example.ui.theme.Win11BorderSubtle
import com.example.ui.theme.Win11CardSurface
import com.example.ui.theme.Win11MicaSurface
import com.example.ui.theme.Win11TextPrimary
import com.example.ui.theme.Win11TextSecondary

@Composable
fun SearchFlyout(
    query: String,
    installedApps: List<InstalledApp>,
    recentApps: List<InstalledApp>,
    desktopItems: List<DesktopItemEntity>,
    stickyNotes: List<StickyNoteEntity>,
    onQueryChange: (String) -> Unit,
    onLaunchApp: (String) -> Unit,
    onOpenFolder: (Long) -> Unit,
    onOpenWidgets: () -> Unit,
    onAskCopilot: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    val trimmedQuery = query.trim()
    val filteredApps = remember(trimmedQuery, installedApps) {
        if (trimmedQuery.isEmpty()) installedApps.take(12)
        else installedApps.filter {
            it.label.contains(trimmedQuery, ignoreCase = true) ||
                it.packageName.contains(trimmedQuery, ignoreCase = true)
        }
    }
    val filteredFolders = remember(trimmedQuery, desktopItems) {
        val folders = desktopItems.filter { it.type == DesktopItemType.FOLDER.name }
        if (trimmedQuery.isEmpty()) emptyList()
        else folders.filter { it.title.contains(trimmedQuery, ignoreCase = true) }
    }
    val filteredNotes = remember(trimmedQuery, stickyNotes) {
        if (trimmedQuery.isEmpty()) emptyList()
        else stickyNotes.filter { it.content.contains(trimmedQuery, ignoreCase = true) }
    }

    Surface(
        color = Win11MicaSurface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 8.dp,
        shadowElevation = 20.dp,
        modifier = modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth(0.70f)
            .fillMaxHeight(0.82f)
            .border(1.dp, Win11BorderSubtle, RoundedCornerShape(16.dp))
            .testTag("search_flyout")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        text = "Digite o nome do app, pasta ou nota...",
                        color = Win11TextSecondary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = Win11AccentCyan
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Limpar busca",
                                tint = Win11TextSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Win11AccentCyan,
                    unfocusedBorderColor = Win11BorderSubtle,
                    focusedContainerColor = Win11CardSurface,
                    unfocusedContainerColor = Win11CardSurface,
                    focusedTextColor = Win11TextPrimary,
                    unfocusedTextColor = Win11TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .testTag("search_input_field")
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Win11CardSurface)
                    .border(1.dp, Win11AccentCyan.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                    .clickable { onAskCopilot(trimmedQuery) }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("search_ask_copilot_btn")
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = Win11AccentCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (trimmedQuery.isEmpty()) "Perguntar ao Copilot IA (Gemini)"
                    else "Perguntar \"$trimmedQuery\" ao Copilot IA",
                    style = MaterialTheme.typography.labelMedium,
                    color = Win11TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                contentPadding = PaddingValues(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (trimmedQuery.isEmpty() && recentApps.isNotEmpty()) {
                    item {
                        Text(
                            text = "Recentes",
                            style = MaterialTheme.typography.titleSmall,
                            color = Win11TextSecondary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(recentApps.take(4), key = { "recent_${it.packageName}" }) { app ->
                        SearchAppRow(app = app, onClick = { onLaunchApp(app.packageName) })
                    }
                    item { Spacer(modifier = Modifier.height(6.dp)) }
                }

                if (filteredFolders.isNotEmpty()) {
                    item {
                        Text(
                            text = "Pastas na área de trabalho",
                            style = MaterialTheme.typography.titleSmall,
                            color = Win11TextSecondary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(filteredFolders, key = { "folder_${it.id}" }) { folder ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Win11CardSurface.copy(alpha = 0.7f))
                                .clickable { onOpenFolder(folder.id) }
                                .padding(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Folder,
                                contentDescription = null,
                                tint = Win11AccentCyan,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = folder.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Win11TextPrimary
                            )
                        }
                    }
                }

                if (filteredNotes.isNotEmpty()) {
                    item {
                        Text(
                            text = "Notas rápidas",
                            style = MaterialTheme.typography.titleSmall,
                            color = Win11TextSecondary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(filteredNotes, key = { "note_${it.id}" }) { note ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Win11CardSurface.copy(alpha = 0.7f))
                                .clickable(onClick = onOpenWidgets)
                                .padding(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.StickyNote2,
                                contentDescription = null,
                                tint = Win11AccentCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = note.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Win11TextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = if (trimmedQuery.isEmpty()) "Aplicativos sugeridos" else "Aplicativos (${filteredApps.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = Win11TextSecondary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                if (filteredApps.isEmpty() && filteredFolders.isEmpty() && filteredNotes.isEmpty()) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = Win11TextSecondary,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Nenhum resultado para \"$trimmedQuery\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Win11TextSecondary
                            )
                        }
                    }
                } else {
                    items(filteredApps, key = { "app_${it.packageName}" }) { app ->
                        SearchAppRow(app = app, onClick = { onLaunchApp(app.packageName) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchAppRow(
    app: InstalledApp,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Win11CardSurface.copy(alpha = 0.55f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        AppIconTile(app = app, sizeDp = 32)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyMedium,
                color = Win11TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Aplicativo",
                style = MaterialTheme.typography.bodySmall,
                color = Win11TextSecondary
            )
        }
    }
}
