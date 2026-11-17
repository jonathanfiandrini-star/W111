package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
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
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DesktopGridArea(
    rootItems: List<DesktopItemEntity>,
    allItems: List<DesktopItemEntity>,
    appsByPackage: Map<String, InstalledApp>,
    installedApps: List<InstalledApp> = emptyList(),
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

    val appPages = remember(installedApps) {
        if (installedApps.isEmpty()) emptyList() else installedApps.chunked(24)
    }
    val totalPages = 1 + appPages.size
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { totalPages })
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (totalPages > 1) 34.dp else 0.dp)
                .testTag("desktop_pager")
        ) { page ->
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
            val depthScale = lerp(0.94f, 1f, (1f - pageOffset).coerceIn(0f, 1f))
            val depthAlpha = lerp(0.58f, 1f, (1f - pageOffset).coerceIn(0f, 1f))

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = depthScale
                        scaleY = depthScale
                        alpha = depthAlpha
                    }
            ) {
                if (page == 0) {
                    if (rootItems.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
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
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = gridDensity.cellSizeDp.dp),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
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
                } else {
                    val pageApps = appPages.getOrNull(page - 1).orEmpty()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = if (appPages.size > 1) {
                                "Biblioteca de Aplicativos • Página $page de ${appPages.size}"
                            } else {
                                "Biblioteca de Aplicativos (${installedApps.size})"
                            },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Win11TextSecondary,
                            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                        )
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = gridDensity.cellSizeDp.dp),
                            contentPadding = PaddingValues(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("desktop_apps_page_$page")
                        ) {
                            items(pageApps, key = { it.packageName }) { app ->
                                AppPageTile(
                                    app = app,
                                    showLabel = showLabels,
                                    onLaunch = { onLaunchApp(app.packageName) },
                                    onPinToTaskbar = { onPinToTaskbar(app.packageName) },
                                    onOpenAppInfo = { onOpenAppInfo(app.packageName) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (totalPages > 1) {
            Surface(
                color = Win11MicaSurface.copy(alpha = 0.85f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
                    .border(1.dp, Win11BorderSubtle, RoundedCornerShape(16.dp))
                    .testTag("desktop_page_indicator")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    repeat(totalPages) { index ->
                        val selected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (selected) 20.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) Win11AccentCyan
                                    else Win11TextSecondary.copy(alpha = 0.45f)
                                )
                                .clickable {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(
                                            page = index,
                                            animationSpec = spring(
                                                dampingRatio = 0.85f,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        )
                                    }
                                }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppPageTile(
    app: InstalledApp,
    showLabel: Boolean,
    onLaunch: () -> Unit,
    onPinToTaskbar: () -> Unit,
    onOpenAppInfo: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "appPageTileScale"
    )

    Box(contentAlignment = Alignment.TopCenter) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(RoundedCornerShape(10.dp))
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = androidx.compose.material3.ripple(),
                    onClick = onLaunch,
                    onLongClick = { menuExpanded = true }
                )
                .padding(horizontal = 6.dp, vertical = 8.dp)
        ) {
            AppIconTile(app = app, sizeDp = 48)
            if (showLabel) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = app.label,
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
                text = { Text("Abrir", color = Win11TextPrimary) },
                onClick = { menuExpanded = false; onLaunch() }
            )
            DropdownMenuItem(
                text = { Text("Fixar na Barra de Tarefas", color = Win11TextPrimary) },
                onClick = { menuExpanded = false; onPinToTaskbar() }
            )
            DropdownMenuItem(
                text = { Text("Informações do app", color = Win11TextPrimary) },
                onClick = { menuExpanded = false; onOpenAppInfo() }
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
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "desktopTileScale"
    )

    Box(contentAlignment = Alignment.TopCenter) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(RoundedCornerShape(10.dp))
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = androidx.compose.material3.ripple(),
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
