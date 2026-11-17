package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.CropSquare
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.HorizontalRule
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ActiveOverlay
import com.example.data.model.DesktopWindowKind
import com.example.data.model.DesktopWindowState
import com.example.data.model.InstalledApp
import com.example.data.model.SystemTelemetry
import com.example.data.model.WeatherState
import com.example.data.model.WindowSizeMode
import com.example.ui.theme.Win11AccentBlue
import com.example.ui.theme.Win11AccentCyan
import com.example.ui.theme.Win11BorderHighlight
import com.example.ui.theme.Win11BorderSubtle
import com.example.ui.theme.Win11CardSurface
import com.example.ui.theme.Win11MicaSurface
import com.example.ui.theme.Win11NavyBg
import com.example.ui.theme.Win11TaskbarSurface
import com.example.ui.theme.Win11TextPrimary
import com.example.ui.theme.Win11TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun Taskbar(
    activeOverlay: ActiveOverlay,
    pinnedApps: List<InstalledApp>,
    openWindows: List<DesktopWindowState>,
    activeWindowId: String?,
    appsByPackage: Map<String, InstalledApp>,
    telemetry: SystemTelemetry,
    weatherState: WeatherState,
    currentTimeMillis: Long,
    onToggleStart: () -> Unit,
    onToggleSearch: () -> Unit,
    onToggleWidgets: () -> Unit,
    onToggleActionCenter: () -> Unit,
    onOpenDesktopMenu: () -> Unit,
    onLaunchPinnedApp: (String) -> Unit,
    onWindowClick: (String) -> Unit,
    onWindowLongClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val dateObj = remember(currentTimeMillis) { Date(currentTimeMillis) }

    val openByPackage = remember(openWindows) {
        openWindows.filter { it.packageName != null }.associateBy { it.packageName!! }
    }
    val pinnedPackageSet = remember(pinnedApps) {
        pinnedApps.map { it.packageName }.toSet()
    }
    val unpinnedOpenWindows = remember(openWindows, pinnedPackageSet) {
        openWindows.filter { win ->
            win.packageName == null || !pinnedPackageSet.contains(win.packageName)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Win11TaskbarSurface)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // Hairline Fluent top divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Win11BorderSubtle)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(horizontal = 8.dp)
        ) {
            // Left: Widgets button
            TaskbarWidgetsButton(
                isActive = activeOverlay is ActiveOverlay.Widgets,
                weatherState = weatherState,
                onClick = onToggleWidgets
            )

            // Center: Start, Search, Desktop Tools, Pinned Apps & Active/Minimized Windows
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TaskbarIconButton(
                        isActive = activeOverlay is ActiveOverlay.StartMenu,
                        onClick = onToggleStart,
                        testTag = "taskbar_start_button"
                    ) {
                        FluentStartLogo(modifier = Modifier.size(22.dp))
                    }

                    TaskbarIconButton(
                        isActive = activeOverlay is ActiveOverlay.Search,
                        onClick = onToggleSearch,
                        testTag = "taskbar_search_button"
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = stringResource(R.string.cd_search_button),
                            tint = Win11TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    TaskbarIconButton(
                        isActive = false,
                        onClick = onOpenDesktopMenu,
                        testTag = "taskbar_desktop_button"
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CreateNewFolder,
                            contentDescription = stringResource(R.string.cd_desktop_overview),
                            tint = Win11AccentCyan,
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    if (pinnedApps.isNotEmpty() || unpinnedOpenWindows.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .width(1.dp)
                                .height(22.dp)
                                .background(Win11BorderSubtle)
                        )
                    }

                    // Pinned apps (highlight if currently open/minimized)
                    pinnedApps.forEach { app ->
                        val openWin = openByPackage[app.packageName]
                        TaskbarPinnedAppButton(
                            app = app,
                            isOpen = openWin != null,
                            isActive = openWin != null && !openWin.isMinimized && activeWindowId == openWin.windowId,
                            onClick = {
                                if (openWin != null) onWindowClick(openWin.windowId)
                                else onLaunchPinnedApp(app.packageName)
                            },
                            onLongClick = {
                                if (openWin != null) onWindowLongClick(openWin.windowId)
                            }
                        )
                    }

                    // Dynamic open/minimized windows (folders, settings, notes, unpinned apps)
                    unpinnedOpenWindows.forEach { win ->
                        TaskbarOpenWindowButton(
                            window = win,
                            app = win.packageName?.let(appsByPackage::get),
                            isActive = !win.isMinimized && activeWindowId == win.windowId,
                            onClick = { onWindowClick(win.windowId) },
                            onLongClick = { onWindowLongClick(win.windowId) }
                        )
                    }
                }
            }

            // Right: System Tray + Clock
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SystemTrayButton(
                    isActive = activeOverlay is ActiveOverlay.ActionCenter,
                    telemetry = telemetry,
                    onClick = onToggleActionCenter
                )

                ClockTrayButton(
                    isActive = activeOverlay is ActiveOverlay.Widgets,
                    timeText = timeFormatter.format(dateObj),
                    dateText = dateFormatter.format(dateObj),
                    onClick = onToggleWidgets
                )
            }
        }
    }
}

@Composable
fun TaskbarWindowControlPopover(
    window: DesktopWindowState,
    app: InstalledApp?,
    onMinimize: () -> Unit,
    onResizeCycle: () -> Unit,
    onResizeSelect: (WindowSizeMode) -> Unit,
    onClose: () -> Unit,
    onRestore: () -> Unit,
    onOpenAppInfo: ((String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Win11MicaSurface,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 10.dp,
        shadowElevation = 22.dp,
        modifier = modifier
            .width(340.dp)
            .border(1.dp, Win11BorderHighlight, RoundedCornerShape(14.dp))
            .testTag("taskbar_window_popover")
    ) {
        Column {
            // Top Header with Windows 11 Top-Right Minimize, Resize, Close buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.25f))
                    .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (app != null) {
                        AppIconTile(app = app, sizeDp = 22)
                    } else {
                        Icon(
                            imageVector = when (window.kind) {
                                DesktopWindowKind.INTERNAL_FOLDER -> Icons.Rounded.FolderOpen
                                DesktopWindowKind.INTERNAL_SETTINGS -> Icons.Rounded.Tune
                                DesktopWindowKind.INTERNAL_NOTES -> Icons.Rounded.StickyNote2
                                DesktopWindowKind.EXTERNAL_APP -> Icons.Rounded.Widgets
                            },
                            contentDescription = null,
                            tint = Win11AccentCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = window.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Win11TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    CaptionControlButton(
                        icon = Icons.Rounded.HorizontalRule,
                        contentDescription = "Minimizar",
                        onClick = onMinimize,
                        testTag = "popover_btn_minimize"
                    )
                    CaptionControlButton(
                        icon = Icons.Rounded.CropSquare,
                        contentDescription = "Redimensionar",
                        onClick = onResizeCycle,
                        testTag = "popover_btn_resize"
                    )
                    CaptionControlButton(
                        icon = Icons.Rounded.Close,
                        contentDescription = "Fechar da barra",
                        isClose = true,
                        onClick = onClose,
                        testTag = "popover_btn_close"
                    )
                }
            }

            HorizontalDivider(color = Win11BorderSubtle)

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "Redimensionar janela",
                    style = MaterialTheme.typography.bodySmall,
                    color = Win11TextSecondary
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    WindowSizeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = window.sizeMode == mode,
                            onClick = { onResizeSelect(mode) },
                            label = { Text(mode.label, style = MaterialTheme.typography.bodySmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Win11AccentCyan,
                                selectedLabelColor = Win11NavyBg
                            )
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        onClick = onRestore,
                        color = Win11CardSurface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, Win11BorderSubtle, RoundedCornerShape(8.dp))
                            .testTag("popover_btn_restore")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = if (window.isMinimized) "Restaurar janela" else "Trazer ao foco",
                                style = MaterialTheme.typography.labelMedium,
                                color = Win11AccentCyan
                            )
                        }
                    }

                    if (window.kind == DesktopWindowKind.EXTERNAL_APP && window.packageName != null && onOpenAppInfo != null) {
                        Surface(
                            onClick = { onOpenAppInfo(window.packageName) },
                            color = Win11CardSurface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .border(1.dp, Win11BorderSubtle, RoundedCornerShape(8.dp))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = "Info do app",
                                    tint = Win11TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Info",
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
private fun TaskbarWidgetsButton(
    isActive: Boolean,
    weatherState: WeatherState,
    onClick: () -> Unit
) {
    val bgColor = if (isActive) Color.White.copy(alpha = 0.14f) else Color.Transparent
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .height(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp)
            .testTag("taskbar_widgets_button")
    ) {
        val tempText = (weatherState as? WeatherState.Success)?.let { "${it.temperatureCelsius}°C" }
        Icon(
            imageVector = if (tempText != null) Icons.Rounded.WbSunny else Icons.Rounded.Widgets,
            contentDescription = stringResource(R.string.cd_widgets_button),
            tint = Win11AccentCyan,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = tempText ?: "Widgets",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Win11TextPrimary,
                maxLines = 1
            )
            if (weatherState is WeatherState.Success) {
                Text(
                    text = weatherState.cityName,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = Win11TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TaskbarIconButton(
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String,
    content: @Composable () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) Color.White.copy(alpha = 0.14f) else Color.Transparent)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        content()
        if (isActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
                    .width(16.dp)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(Win11AccentCyan)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskbarPinnedAppButton(
    app: InstalledApp,
    isOpen: Boolean,
    isActive: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) Color.White.copy(alpha = 0.16f) else Color.Transparent)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("taskbar_app_${app.packageName}")
    ) {
        AppIconTile(app = app, sizeDp = 28)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 2.dp)
                .width(if (isActive) 18.dp else if (isOpen) 10.dp else 5.dp)
                .height(3.dp)
                .clip(CircleShape)
                .background(
                    if (isOpen) Win11AccentCyan else Win11TextSecondary.copy(alpha = 0.65f)
                )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskbarOpenWindowButton(
    window: DesktopWindowState,
    app: InstalledApp?,
    isActive: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isActive) Color.White.copy(alpha = 0.16f)
                else Color.White.copy(alpha = 0.06f)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("taskbar_win_${window.windowId}")
    ) {
        if (app != null) {
            AppIconTile(app = app, sizeDp = 28)
        } else {
            Icon(
                imageVector = when (window.kind) {
                    DesktopWindowKind.INTERNAL_FOLDER -> Icons.Rounded.FolderOpen
                    DesktopWindowKind.INTERNAL_SETTINGS -> Icons.Rounded.Tune
                    DesktopWindowKind.INTERNAL_NOTES -> Icons.Rounded.StickyNote2
                    DesktopWindowKind.EXTERNAL_APP -> Icons.Rounded.Widgets
                },
                contentDescription = window.title,
                tint = Win11AccentCyan,
                modifier = Modifier.size(22.dp)
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 2.dp)
                .width(if (isActive) 18.dp else 10.dp)
                .height(3.dp)
                .clip(CircleShape)
                .background(Win11AccentCyan)
        )
    }
}

@Composable
private fun SystemTrayButton(
    isActive: Boolean,
    telemetry: SystemTelemetry,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) Color.White.copy(alpha = 0.14f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp)
            .testTag("taskbar_system_tray")
    ) {
        Icon(
            imageVector = if (telemetry.wifiConnected) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
            contentDescription = stringResource(R.string.cd_action_center),
            tint = if (telemetry.wifiConnected) Win11TextPrimary else Win11TextSecondary,
            modifier = Modifier.size(16.dp)
        )
        Icon(
            imageVector = if (telemetry.mediaVolume == 0) Icons.AutoMirrored.Rounded.VolumeOff
            else Icons.AutoMirrored.Rounded.VolumeUp,
            contentDescription = null,
            tint = Win11TextPrimary,
            modifier = Modifier.size(16.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (telemetry.isCharging) Icons.Rounded.BatteryChargingFull
                else Icons.Rounded.BatteryFull,
                contentDescription = null,
                tint = if (telemetry.isCharging) Win11AccentCyan else Win11TextPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "${telemetry.batteryPercent}%",
                style = MaterialTheme.typography.labelSmall,
                color = Win11TextPrimary
            )
        }
    }
}

@Composable
private fun ClockTrayButton(
    isActive: Boolean,
    timeText: String,
    dateText: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) Color.White.copy(alpha = 0.10f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp)
            .testTag("taskbar_clock")
    ) {
        Text(
            text = timeText,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            ),
            color = Win11TextPrimary
        )
        Text(
            text = dateText,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = Win11TextSecondary
        )
    }
}

@Composable
fun FluentStartLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val gap = size.width * 0.09f
        val tileW = (size.width - gap) / 2f
        val tileH = (size.height - gap) / 2f
        val radius = CornerRadius(size.width * 0.10f, size.width * 0.10f)

        val topLeftBrush = Brush.linearGradient(listOf(Color(0xFF7BE0FF), Win11AccentCyan))
        val topRightBrush = Brush.linearGradient(listOf(Win11AccentCyan, Color(0xFF38BDF8)))
        val bottomLeftBrush = Brush.linearGradient(listOf(Color(0xFF38BDF8), Win11AccentBlue))
        val bottomRightBrush = Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)))

        drawRoundRect(
            brush = topLeftBrush,
            topLeft = Offset(0f, 0f),
            size = Size(tileW, tileH),
            cornerRadius = radius
        )
        drawRoundRect(
            brush = topRightBrush,
            topLeft = Offset(tileW + gap, 0f),
            size = Size(tileW, tileH),
            cornerRadius = radius
        )
        drawRoundRect(
            brush = bottomLeftBrush,
            topLeft = Offset(0f, tileH + gap),
            size = Size(tileW, tileH),
            cornerRadius = radius
        )
        drawRoundRect(
            brush = bottomRightBrush,
            topLeft = Offset(tileW + gap, tileH + gap),
            size = Size(tileW, tileH),
            cornerRadius = radius
        )
    }
}
