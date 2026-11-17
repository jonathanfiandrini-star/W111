package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.InstalledApp
import com.example.data.model.PinTarget
import com.example.ui.theme.Win11AccentBlue
import com.example.ui.theme.Win11AccentCyan
import com.example.ui.theme.Win11BorderSubtle
import com.example.ui.theme.Win11CardSurface
import com.example.ui.theme.Win11MicaSurface
import com.example.ui.theme.Win11TextPrimary
import com.example.ui.theme.Win11TextSecondary

@Composable
fun StartMenuFlyout(
    userDisplayName: String,
    installedApps: List<InstalledApp>,
    pinnedApps: List<InstalledApp>,
    taskbarPinnedPackages: Set<String>,
    recentApps: List<InstalledApp>,
    onLaunchApp: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onPinToDesktop: (InstalledApp) -> Unit,
    onTogglePin: (String, PinTarget) -> Unit,
    onOpenAppInfo: (String) -> Unit,
    onUninstallApp: (String) -> Unit,
    onOpenLauncherSettings: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAllApps by remember { mutableStateOf(false) }
    val startPinnedSet = remember(pinnedApps) { pinnedApps.map { it.packageName }.toSet() }

    Surface(
        color = Win11MicaSurface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 8.dp,
        shadowElevation = 20.dp,
        modifier = modifier
            .widthIn(max = 580.dp)
            .fillMaxWidth(0.72f)
            .fillMaxHeight(0.84f)
            .border(1.dp, Win11BorderSubtle, RoundedCornerShape(16.dp))
            .testTag("start_menu_flyout")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top search bar trigger
            Surface(
                onClick = onOpenSearch,
                color = Win11CardSurface,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .height(44.dp)
                    .border(1.dp, Win11BorderSubtle, RoundedCornerShape(24.dp))
                    .testTag("start_menu_search_bar")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = Win11AccentCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Pesquisar aplicativos, pastas e notas...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Win11TextSecondary
                    )
                }
            }

            AnimatedContent(
                targetState = showAllApps,
                transitionSpec = {
                    val forward = targetState
                    (slideInHorizontally(
                        animationSpec = tween(220, easing = FastOutSlowInEasing),
                        initialOffsetX = { if (forward) it / 4 else -it / 4 }
                    ) + fadeIn(tween(200))) togetherWith
                        (slideOutHorizontally(
                            animationSpec = tween(180, easing = FastOutSlowInEasing),
                            targetOffsetX = { if (forward) -it / 4 else it / 4 }
                        ) + fadeOut(tween(160)))
                },
                modifier = Modifier.weight(1f),
                label = "startMenuPageTransition"
            ) { allAppsVisible ->
                Column(modifier = Modifier.fillMaxSize()) {
                    if (!allAppsVisible) {
                        // Header row: Fixados + Todos os Apps button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 22.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Fixados",
                                style = MaterialTheme.typography.titleMedium,
                                color = Win11TextPrimary
                            )
                            Surface(
                                onClick = { showAllApps = true },
                                color = Win11CardSurface,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .border(1.dp, Win11BorderSubtle, RoundedCornerShape(6.dp))
                                    .testTag("start_all_apps_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Todos os apps",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Win11TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = Win11TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Pinned Grid
                        val displayPinned = if (pinnedApps.isNotEmpty()) pinnedApps else installedApps.take(10)
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 84.dp),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(displayPinned, key = { it.packageName }) { app ->
                                StartAppGridItem(
                                    app = app,
                                    isPinnedStart = startPinnedSet.contains(app.packageName),
                                    isPinnedTaskbar = taskbarPinnedPackages.contains(app.packageName),
                                    onLaunch = { onLaunchApp(app.packageName) },
                                    onPinToDesktop = { onPinToDesktop(app) },
                                    onTogglePin = { target -> onTogglePin(app.packageName, target) },
                                    onOpenAppInfo = { onOpenAppInfo(app.packageName) },
                                    onUninstallApp = { onUninstallApp(app.packageName) }
                                )
                            }
                        }

                        // Recommended / Recent Section
                        if (recentApps.isNotEmpty()) {
                            HorizontalDivider(color = Win11BorderSubtle)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 22.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "Recomendados",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Win11TextPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(recentApps.take(6), key = { it.packageName }) { app ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Win11CardSurface.copy(alpha = 0.7f))
                                                .clickable { onLaunchApp(app.packageName) }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            AppIconTile(app = app, sizeDp = 26)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = app.label,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Win11TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.widthIn(max = 100.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // All Apps A-Z view
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 22.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Todos os aplicativos (${installedApps.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = Win11TextPrimary
                            )
                            Surface(
                                onClick = { showAllApps = false },
                                color = Win11CardSurface,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .border(1.dp, Win11BorderSubtle, RoundedCornerShape(6.dp))
                                    .testTag("start_back_to_pinned_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = null,
                                        tint = Win11TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Voltar",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Win11TextPrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("all_apps_list")
                        ) {
                            items(installedApps, key = { it.packageName }) { app ->
                                AllAppsRowItem(
                                    app = app,
                                    isPinnedStart = startPinnedSet.contains(app.packageName),
                                    isPinnedTaskbar = taskbarPinnedPackages.contains(app.packageName),
                                    onLaunch = { onLaunchApp(app.packageName) },
                                    onPinToDesktop = { onPinToDesktop(app) },
                                    onTogglePin = { target -> onTogglePin(app.packageName, target) },
                                    onOpenAppInfo = { onOpenAppInfo(app.packageName) },
                                    onUninstallApp = { onUninstallApp(app.packageName) }
                                )
                            }
                        }
                    }
                }
            }

            // Footer
            HorizontalDivider(color = Win11BorderSubtle)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.22f))
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Win11AccentBlue)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = Win11TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = userDisplayName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Win11TextPrimary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onOpenLauncherSettings,
                        modifier = Modifier.testTag("start_launcher_settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "Configurações do Launcher",
                            tint = Win11TextPrimary
                        )
                    }
                    IconButton(
                        onClick = onOpenSystemSettings,
                        modifier = Modifier.testTag("start_system_settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Configurações do Android",
                            tint = Win11TextPrimary
                        )
                    }
                    IconButton(
                        onClick = onOpenSystemSettings,
                        modifier = Modifier.testTag("start_power_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PowerSettingsNew,
                            contentDescription = "Sistema",
                            tint = Win11AccentCyan
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StartAppGridItem(
    app: InstalledApp,
    isPinnedStart: Boolean,
    isPinnedTaskbar: Boolean,
    onLaunch: () -> Unit,
    onPinToDesktop: () -> Unit,
    onTogglePin: (PinTarget) -> Unit,
    onOpenAppInfo: () -> Unit,
    onUninstallApp: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box(contentAlignment = Alignment.TopCenter) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .combinedClickable(
                    onClick = onLaunch,
                    onLongClick = { menuExpanded = true }
                )
                .padding(horizontal = 6.dp, vertical = 8.dp)
        ) {
            AppIconTile(app = app, sizeDp = 40)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodySmall,
                color = Win11TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(74.dp)
            )
        }

        AppContextDropdownMenu(
            expanded = menuExpanded,
            isPinnedStart = isPinnedStart,
            isPinnedTaskbar = isPinnedTaskbar,
            onDismiss = { menuExpanded = false },
            onLaunch = onLaunch,
            onPinToDesktop = onPinToDesktop,
            onTogglePin = onTogglePin,
            onOpenAppInfo = onOpenAppInfo,
            onUninstallApp = onUninstallApp
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AllAppsRowItem(
    app: InstalledApp,
    isPinnedStart: Boolean,
    isPinnedTaskbar: Boolean,
    onLaunch: () -> Unit,
    onPinToDesktop: () -> Unit,
    onTogglePin: (PinTarget) -> Unit,
    onOpenAppInfo: () -> Unit,
    onUninstallApp: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .combinedClickable(
                    onClick = onLaunch,
                    onLongClick = { menuExpanded = true }
                )
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            AppIconTile(app = app, sizeDp = 32)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyMedium,
                color = Win11TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Segurar para opções",
                style = MaterialTheme.typography.bodySmall,
                color = Win11TextSecondary.copy(alpha = 0.7f)
            )
        }

        AppContextDropdownMenu(
            expanded = menuExpanded,
            isPinnedStart = isPinnedStart,
            isPinnedTaskbar = isPinnedTaskbar,
            onDismiss = { menuExpanded = false },
            onLaunch = onLaunch,
            onPinToDesktop = onPinToDesktop,
            onTogglePin = onTogglePin,
            onOpenAppInfo = onOpenAppInfo,
            onUninstallApp = onUninstallApp
        )
    }
}

@Composable
private fun AppContextDropdownMenu(
    expanded: Boolean,
    isPinnedStart: Boolean,
    isPinnedTaskbar: Boolean,
    onDismiss: () -> Unit,
    onLaunch: () -> Unit,
    onPinToDesktop: () -> Unit,
    onTogglePin: (PinTarget) -> Unit,
    onOpenAppInfo: () -> Unit,
    onUninstallApp: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = Win11CardSurface
    ) {
        DropdownMenuItem(
            text = { Text("Abrir", color = Win11TextPrimary) },
            onClick = { onDismiss(); onLaunch() }
        )
        DropdownMenuItem(
            text = {
                Text(
                    if (isPinnedStart) "Desafixar do Iniciar" else "Fixar no Iniciar",
                    color = Win11TextPrimary
                )
            },
            onClick = { onDismiss(); onTogglePin(PinTarget.START) }
        )
        DropdownMenuItem(
            text = {
                Text(
                    if (isPinnedTaskbar) "Desafixar da Barra de Tarefas" else "Fixar na Barra de Tarefas",
                    color = Win11TextPrimary
                )
            },
            onClick = { onDismiss(); onTogglePin(PinTarget.TASKBAR) }
        )
        DropdownMenuItem(
            text = { Text("Adicionar à Área de Trabalho", color = Win11TextPrimary) },
            onClick = { onDismiss(); onPinToDesktop() }
        )
        DropdownMenuItem(
            text = { Text("Informações do app", color = Win11TextPrimary) },
            onClick = { onDismiss(); onOpenAppInfo() }
        )
        DropdownMenuItem(
            text = { Text("Desinstalar", color = Color(0xFFFF99A4)) },
            onClick = { onDismiss(); onUninstallApp() }
        )
    }
}
