package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.DesktopItemEntity
import com.example.data.model.DesktopItemType
import com.example.data.model.GridDensity
import com.example.data.model.InstalledApp
import com.example.ui.theme.Win11AccentCyan
import com.example.ui.theme.Win11BorderSubtle
import com.example.ui.theme.Win11CardSurface
import com.example.ui.theme.Win11MicaSurface
import com.example.ui.theme.Win11TextPrimary
import com.example.ui.theme.Win11TextSecondary

@Composable
fun DesktopGridArea(
    rootItems: List<DesktopItemEntity>,
    allItems: List<DesktopItemEntity>,
    appsByPackage: Map<String, InstalledApp>,
    gridDensity: GridDensity,
    showLabels: Boolean,
    onLaunchApp: (String) -> Unit,
    onOpenFolder: (Long) -> Unit,
    onRenameRequest: (DesktopItemEntity) -> Unit,
    onMoveToFolderRequest: (DesktopItemEntity) -> Unit,
    onRemoveFromDesktop: (DesktopItemEntity) -> Unit,
    onPinToTaskbar: (String) -> Unit,
    onOpenAppInfo: (String) -> Unit,
    onUninstallApp: (String) -> Unit,
    onRequestAddApp: () -> Unit,
    onRequestCreateFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val folders = remember(rootItems) {
        rootItems.filter { it.type == DesktopItemType.FOLDER.name }
    }

    if (rootItems.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = Win11MicaSurface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .border(1.dp, Win11BorderSubtle, RoundedCornerShape(16.dp))
                    .padding(horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CreateNewFolder,
                        contentDescription = null,
                        tint = Win11AccentCyan,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Área de trabalho vazia",
                        style = MaterialTheme.typography.titleMedium,
                        color = Win11TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Toque e segure no fundo ou adicione atalhos e pastas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Win11TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FluentChipButton(
                            label = "Nova Pasta",
                            onClick = onRequestCreateFolder,
                            testTag = "empty_create_folder_btn"
                        )
                        FluentChipButton(
                            label = "Adicionar App",
                            onClick = onRequestAddApp,
                            testTag = "empty_add_app_btn"
                        )
                    }
                }
            }
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = gridDensity.cellSizeDp.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("desktop_grid")
    ) {
        items(rootItems, key = { it.id }) { item ->
            val isFolder = item.type == DesktopItemType.FOLDER.name
            val folderChildren = remember(allItems, item.id) {
                if (isFolder) allItems.filter { it.folderId == item.id } else emptyList()
            }
            val app = item.packageName?.let { appsByPackage[it] }

            DesktopTile(
                item = item,
                app = app,
                folderChildren = folderChildren,
                appsByPackage = appsByPackage,
                availableFolders = folders,
                showLabel = showLabels,
                onClick = {
                    if (isFolder) {
                        onOpenFolder(item.id)
                    } else if (item.packageName != null) {
                        onLaunchApp(item.packageName)
                    }
                },
                onRenameRequest = { onRenameRequest(item) },
                onMoveToFolderRequest = { onMoveToFolderRequest(item) },
                onRemoveFromDesktop = { onRemoveFromDesktop(item) },
                onPinToTaskbar = { item.packageName?.let(onPinToTaskbar) },
                onOpenAppInfo = { item.packageName?.let(onOpenAppInfo) },
                onUninstallApp = { item.packageName?.let(onUninstallApp) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DesktopTile(
    item: DesktopItemEntity,
    app: InstalledApp?,
    folderChildren: List<DesktopItemEntity>,
    appsByPackage: Map<String, InstalledApp>,
    availableFolders: List<DesktopItemEntity>,
    showLabel: Boolean,
    onClick: () -> Unit,
    onRenameRequest: () -> Unit,
    onMoveToFolderRequest: () -> Unit,
    onRemoveFromDesktop: () -> Unit,
    onPinToTaskbar: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onUninstallApp: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isFolder = item.type == DesktopItemType.FOLDER.name

    Box(contentAlignment = Alignment.TopCenter) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { menuExpanded = true }
                )
                .padding(horizontal = 6.dp, vertical = 8.dp)
                .testTag("desktop_item_${item.id}")
        ) {
            if (isFolder) {
                FolderPreviewIcon(
                    children = folderChildren,
                    appsByPackage = appsByPackage
                )
            } else {
                AppIconTile(app = app, sizeDp = 48)
            }

            if (showLabel) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = Win11TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(78.dp)
                )
            }
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            containerColor = Win11CardSurface
        ) {
            DropdownMenuItem(
                text = { Text(if (isFolder) "Abrir pasta" else "Abrir", color = Win11TextPrimary) },
                onClick = {
                    menuExpanded = false
                    onClick()
                }
            )
            DropdownMenuItem(
                text = { Text("Renomear", color = Win11TextPrimary) },
                onClick = {
                    menuExpanded = false
                    onRenameRequest()
                }
            )
            if (!isFolder && availableFolders.isNotEmpty()) {
                DropdownMenuItem(
                    text = { Text("Mover para pasta...", color = Win11TextPrimary) },
                    onClick = {
                        menuExpanded = false
                        onMoveToFolderRequest()
                    }
                )
            }
            if (!isFolder && item.packageName != null) {
                DropdownMenuItem(
                    text = { Text("Fixar na Barra de Tarefas", color = Win11TextPrimary) },
                    onClick = {
                        menuExpanded = false
                        onPinToTaskbar()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Informações do app", color = Win11TextPrimary) },
                    onClick = {
                        menuExpanded = false
                        onOpenAppInfo()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Desinstalar app", color = Win11TextPrimary) },
                    onClick = {
                        menuExpanded = false
                        onUninstallApp()
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("Remover da área de trabalho", color = Color(0xFFFF99A4)) },
                onClick = {
                    menuExpanded = false
                    onRemoveFromDesktop()
                }
            )
        }
    }
}

@Composable
fun AppIconTile(
    app: InstalledApp?,
    sizeDp: Int = 44,
    modifier: Modifier = Modifier
) {
    if (app?.icon != null) {
        Image(
            bitmap = app.icon,
            contentDescription = app.label,
            modifier = modifier
                .size(sizeDp.dp)
                .clip(RoundedCornerShape(10.dp))
        )
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(sizeDp.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF28344B), Color(0xFF1D2536))
                    )
                )
                .border(1.dp, Win11BorderSubtle, RoundedCornerShape(10.dp))
        ) {
            Icon(
                imageVector = Icons.Rounded.Apps,
                contentDescription = app?.label ?: "Aplicativo",
                tint = Win11AccentCyan,
                modifier = Modifier.size((sizeDp * 0.56f).dp)
            )
        }
    }
}

@Composable
private fun FolderPreviewIcon(
    children: List<DesktopItemEntity>,
    appsByPackage: Map<String, InstalledApp>
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xD92B3548),
                        Color(0xCC1B2230)
                    )
                )
            )
            .border(1.dp, Win11BorderSubtle, RoundedCornerShape(12.dp))
            .padding(5.dp)
    ) {
        val previewApps = children.take(4).mapNotNull { it.packageName?.let(appsByPackage::get) }
        if (previewApps.isEmpty()) {
            Icon(
                imageVector = Icons.Rounded.Folder,
                contentDescription = "Pasta",
                tint = Win11AccentCyan,
                modifier = Modifier.size(30.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    previewApps.getOrNull(0)?.let { AppIconTile(it, sizeDp = 17) }
                        ?: Spacer(modifier = Modifier.size(17.dp))
                    previewApps.getOrNull(1)?.let { AppIconTile(it, sizeDp = 17) }
                        ?: Spacer(modifier = Modifier.size(17.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    previewApps.getOrNull(2)?.let { AppIconTile(it, sizeDp = 17) }
                        ?: Spacer(modifier = Modifier.size(17.dp))
                    previewApps.getOrNull(3)?.let { AppIconTile(it, sizeDp = 17) }
                        ?: Spacer(modifier = Modifier.size(17.dp))
                }
            }
        }
    }
}

@Composable
fun FluentChipButton(
    label: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = Win11CardSurface,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .height(48.dp)
            .border(1.dp, Win11BorderSubtle, RoundedCornerShape(8.dp))
            .testTag(testTag)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Win11TextPrimary
            )
        }
    }
}
