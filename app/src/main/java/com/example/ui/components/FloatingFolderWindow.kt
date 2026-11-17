package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.DesktopItemEntity
import com.example.data.model.DesktopWindowState
import com.example.data.model.InstalledApp
import com.example.data.model.WindowSizeMode
import com.example.ui.theme.Win11AccentCyan
import com.example.ui.theme.Win11CardSurface
import com.example.ui.theme.Win11TextPrimary
import com.example.ui.theme.Win11TextSecondary

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BoxScope.FloatingFolderWindow(
    windowState: DesktopWindowState,
    folder: DesktopItemEntity,
    children: List<DesktopItemEntity>,
    appsByPackage: Map<String, InstalledApp>,
    isActive: Boolean,
    onFocus: () -> Unit,
    onMinimize: () -> Unit,
    onResizeCycle: () -> Unit,
    onResizeSelect: (WindowSizeMode) -> Unit,
    onClose: () -> Unit,
    onRenameFolder: () -> Unit,
    onAddAppToFolder: () -> Unit,
    onLaunchApp: (String) -> Unit,
    onMoveOutToDesktop: (Long) -> Unit,
    onDeleteItem: (Long) -> Unit
) {
    DesktopWindowFrame(
        windowId = windowState.windowId,
        title = folder.title,
        icon = Icons.Rounded.FolderOpen,
        sizeMode = windowState.sizeMode,
        isActive = isActive,
        onFocus = onFocus,
        onMinimize = onMinimize,
        onResizeCycle = onResizeCycle,
        onResizeSelect = onResizeSelect,
        onClose = onClose,
        extraHeaderActions = {
            IconButton(
                onClick = onAddAppToFolder,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Adicionar aplicativo à pasta",
                    tint = Win11AccentCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onRenameFolder,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Renomear pasta",
                    tint = Win11TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    ) {
        if (children.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Esta pasta está vazia",
                        style = MaterialTheme.typography.titleSmall,
                        color = Win11TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Toque no botão + na barra superior para adicionar aplicativos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Win11TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 88.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(children, key = { it.id }) { child ->
                    var menuOpen by remember { mutableStateOf(false) }
                    val app = child.packageName?.let(appsByPackage::get)

                    Box(contentAlignment = Alignment.TopCenter) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .combinedClickable(
                                    onClick = { child.packageName?.let(onLaunchApp) },
                                    onLongClick = { menuOpen = true }
                                )
                                .padding(8.dp)
                        ) {
                            AppIconTile(app = app, sizeDp = 44)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = child.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = Win11TextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }

                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                            containerColor = Win11CardSurface
                        ) {
                            DropdownMenuItem(
                                text = { Text("Abrir", color = Win11TextPrimary) },
                                onClick = {
                                    menuOpen = false
                                    child.packageName?.let(onLaunchApp)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Mover para área de trabalho", color = Win11TextPrimary) },
                                onClick = {
                                    menuOpen = false
                                    onMoveOutToDesktop(child.id)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Remover da pasta", color = Color(0xFFFF99A4)) },
                                onClick = {
                                    menuOpen = false
                                    onDeleteItem(child.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
