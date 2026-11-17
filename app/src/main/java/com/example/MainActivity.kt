package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.data.ai.configureAppCheck
import com.example.data.model.StickyNoteColor
import com.example.service.AppWindowOverlayService
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.DesktopItemEntity
import com.example.data.model.ActiveOverlay
import com.example.data.model.DesktopItemType
import com.example.data.model.DesktopWindowKind
import com.example.data.model.PinTarget
import com.example.ui.LauncherUiState
import com.example.ui.LauncherViewModel
import com.example.ui.components.ActionCenterFlyout
import com.example.ui.components.AddAppPickerDialog
import com.example.ui.components.CopilotFlyout
import com.example.ui.components.DesktopContextMenuDialog
import com.example.ui.components.DesktopGridArea
import com.example.ui.components.FloatingFolderWindow
import com.example.ui.components.GestureNavigationGuideDialog
import com.example.ui.components.LauncherSettingsWindow
import com.example.ui.components.MoveToFolderDialog
import com.example.ui.components.NotesDesktopWindow
import com.example.ui.components.OverlayPermissionPromptDialog
import com.example.ui.components.SearchFlyout
import com.example.ui.components.StartMenuFlyout
import com.example.ui.components.Taskbar
import com.example.ui.components.TaskbarWindowControlPopover
import com.example.ui.components.TextInputDialog
import com.example.ui.components.WallpaperBackground
import com.example.ui.components.WidgetsPanelFlyout
import com.example.ui.theme.Fluent11Theme
import com.example.ui.theme.Win11AccentCyan
import com.example.ui.theme.Win11BorderHighlight
import com.example.ui.theme.Win11MicaSurface
import com.example.ui.theme.Win11TextPrimary

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check with launch intent before any UI or ViewModel creation
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        applyImmersiveNavigation()
        handleOverlayActionIntent(intent)
        setContent {
            Fluent11Theme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val copilotState by viewModel.copilotUiState.collectAsStateWithLifecycle()

                val defaultLauncherPromptLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) {
                    viewModel.onDefaultLauncherRoleResult()
                }

                val triggerDefaultLauncherFlow: () -> Unit = {
                    val promptIntent = viewModel.getDefaultLauncherPromptIntent()
                    if (promptIntent != null) {
                        defaultLauncherPromptLauncher.launch(promptIntent)
                    } else {
                        viewModel.openDefaultHomeSettings()
                    }
                }

                LaunchedEffect(Unit) {
                    val startupIntent = viewModel.consumeStartupDefaultLauncherIntent()
                    if (startupIntent != null) {
                        runCatching { defaultLauncherPromptLauncher.launch(startupIntent) }
                    }
                }

                LaunchedEffect(uiState.telemetry.windowBrightness) {
                    val lp = window.attributes
                    lp.screenBrightness = uiState.telemetry.windowBrightness
                    window.attributes = lp
                }

                Fluent11LauncherScreen(
                    uiState = uiState,
                    copilotState = copilotState,
                    viewModel = viewModel,
                    onRequestDefaultLauncher = triggerDefaultLauncherFlow
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOverlayActionIntent(intent)
    }

    private fun handleOverlayActionIntent(intent: Intent?) {
        val pkg = intent?.getStringExtra(AppWindowOverlayService.EXTRA_PACKAGE_NAME).orEmpty()
        when (intent?.action) {
            AppWindowOverlayService.ACTION_MINIMIZE_EXTERNAL_APP -> {
                viewModel.handleExternalWindowMinimize(pkg)
            }
            AppWindowOverlayService.ACTION_CLOSE_EXTERNAL_APP -> {
                viewModel.handleExternalWindowClose(pkg)
            }
            AppWindowOverlayService.ACTION_OPEN_DESKTOP_OVERLAY -> {
                val target = intent.getStringExtra(AppWindowOverlayService.EXTRA_OVERLAY_TARGET)
                val overlay = when (target) {
                    AppWindowOverlayService.OVERLAY_TARGET_START -> ActiveOverlay.StartMenu
                    AppWindowOverlayService.OVERLAY_TARGET_SEARCH -> ActiveOverlay.Search
                    AppWindowOverlayService.OVERLAY_TARGET_WIDGETS -> ActiveOverlay.Widgets
                    AppWindowOverlayService.OVERLAY_TARGET_ACTION_CENTER -> ActiveOverlay.ActionCenter
                    AppWindowOverlayService.OVERLAY_TARGET_COPILOT -> ActiveOverlay.Copilot
                    else -> ActiveOverlay.None
                }
                if (overlay != ActiveOverlay.None) {
                    viewModel.toggleOverlay(overlay)
                }
            }
            AppWindowOverlayService.ACTION_FOCUS_INTERNAL_WINDOW -> {
                val winId = intent.getStringExtra(AppWindowOverlayService.EXTRA_WINDOW_ID).orEmpty()
                if (winId.isNotBlank()) {
                    viewModel.focusWindow(winId)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applyImmersiveNavigation()
        viewModel.onLauncherResumed()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applyImmersiveNavigation()
        }
    }

    private fun applyImmersiveNavigation() {
        runCatching {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.navigationBars())
        }
    }
}

@Composable
fun Fluent11LauncherScreen(
    uiState: LauncherUiState,
    copilotState: com.example.data.model.CopilotUiState,
    viewModel: LauncherViewModel,
    onRequestDefaultLauncher: () -> Unit
) {
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var renameTargetItem by remember { mutableStateOf<DesktopItemEntity?>(null) }
    var moveTargetItem by remember { mutableStateOf<DesktopItemEntity?>(null) }

    val visibleInternalWindows = remember(uiState.openWindows) {
        uiState.openWindows
            .filter { !it.isMinimized && it.kind != DesktopWindowKind.EXTERNAL_APP }
            .sortedBy { it.lastFocusedAt }
    }

    val hasAnyOverlay = uiState.activeOverlay != ActiveOverlay.None ||
        uiState.showDesktopContextMenu ||
        uiState.taskbarPopoverWindowId != null ||
        uiState.pendingOverlayPermissionLaunch != null ||
        uiState.showGestureGuideDialog ||
        visibleInternalWindows.isNotEmpty() ||
        showCreateFolderDialog ||
        renameTargetItem != null ||
        moveTargetItem != null

    // On a HOME launcher, back button closes active dialogs/flyouts/windows cleanly instead of exiting
    BackHandler(enabled = hasAnyOverlay) {
        when {
            uiState.showGestureGuideDialog -> viewModel.dismissGestureGuideDialog()
            uiState.pendingOverlayPermissionLaunch != null -> viewModel.dismissOverlayPermissionPrompt()
            showCreateFolderDialog -> showCreateFolderDialog = false
            renameTargetItem != null -> renameTargetItem = null
            moveTargetItem != null -> moveTargetItem = null
            uiState.showDesktopContextMenu -> viewModel.setDesktopContextMenuVisible(false)
            uiState.taskbarPopoverWindowId != null -> viewModel.toggleTaskbarWindowPopover(null)
            uiState.activeOverlay != ActiveOverlay.None -> viewModel.dismissOverlay()
            visibleInternalWindows.isNotEmpty() -> {
                val topWindowId = uiState.activeWindowId ?: visibleInternalWindows.last().windowId
                viewModel.minimizeWindow(topWindowId)
            }
        }
    }

    val taskbarPinnedPackages = remember(uiState.taskbarPinnedApps) {
        uiState.taskbarPinnedApps.map { it.packageName }.toSet()
    }
    val folders = remember(uiState.desktopRootItems) {
        uiState.desktopRootItems.filter { it.type == DesktopItemType.FOLDER.name }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Wallpaper + gesture capture
        WallpaperBackground(
            onBackgroundTap = { viewModel.dismissOverlay() },
            onBackgroundLongPress = { viewModel.setDesktopContextMenuVisible(true) }
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Desktop Workspace above Taskbar
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                DesktopGridArea(
                    rootItems = uiState.desktopRootItems,
                    allItems = uiState.allDesktopItems,
                    appsByPackage = uiState.installedAppsByPackage,
                    gridDensity = uiState.preferences.gridDensity,
                    showLabels = uiState.preferences.showDesktopLabels,
                    onLaunchApp = { viewModel.launchApp(it) },
                    onOpenFolder = { folderId -> viewModel.openFolderWindow(folderId) },
                    onRenameRequest = { renameTargetItem = it },
                    onMoveToFolderRequest = { moveTargetItem = it },
                    onRemoveFromDesktop = { viewModel.deleteDesktopItem(it.id) },
                    onPinToTaskbar = { pkg -> viewModel.togglePin(pkg, PinTarget.TASKBAR) },
                    onOpenAppInfo = { viewModel.openAppInfo(it) },
                    onUninstallApp = { viewModel.uninstallApp(it) },
                    onRequestAddApp = {
                        viewModel.toggleOverlay(ActiveOverlay.AddAppPicker(targetFolderId = null))
                    },
                    onRequestCreateFolder = { showCreateFolderDialog = true }
                )

                // Subtle Setup Pill when overlay permission is missing and no window/overlay is open
                androidx.compose.animation.AnimatedVisibility(
                    visible = !uiState.telemetry.canDrawOverlays &&
                        uiState.statusBanner == null &&
                        uiState.activeOverlay == ActiveOverlay.None &&
                        visibleInternalWindows.isEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 10.dp, end = 12.dp)
                ) {
                    Surface(
                        color = Win11MicaSurface,
                        shape = RoundedCornerShape(20.dp),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, Win11BorderHighlight, RoundedCornerShape(20.dp))
                            .clickable { viewModel.openOverlayPermissionSettings() }
                            .testTag("desktop_overlay_setup_chip")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Layers,
                                contentDescription = null,
                                tint = Win11AccentCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Ativar barra e controles sobre apps",
                                style = MaterialTheme.typography.labelMedium,
                                color = Win11TextPrimary
                            )
                        }
                    }
                }

                // Visible Desktop Windows (Folders, Settings, Notes) ordered by focus timestamp
                visibleInternalWindows.forEach { win ->
                    key(win.windowId) {
                        val isActive = uiState.activeWindowId == win.windowId
                        when (win.kind) {
                            DesktopWindowKind.INTERNAL_FOLDER -> {
                                val folder = win.folderId?.let { id ->
                                    uiState.desktopRootItems.firstOrNull { it.id == id }
                                }
                                if (folder != null) {
                                    FloatingFolderWindow(
                                        windowState = win,
                                        folder = folder,
                                        children = viewModel.getFolderChildren(folder.id),
                                        appsByPackage = uiState.installedAppsByPackage,
                                        isActive = isActive,
                                        onFocus = { viewModel.focusWindow(win.windowId) },
                                        onMinimize = { viewModel.minimizeWindow(win.windowId) },
                                        onResizeCycle = { viewModel.resizeWindow(win.windowId) },
                                        onResizeSelect = { mode -> viewModel.resizeWindow(win.windowId, mode) },
                                        onClose = { viewModel.closeWindow(win.windowId) },
                                        onRenameFolder = { renameTargetItem = folder },
                                        onAddAppToFolder = {
                                            viewModel.toggleOverlay(
                                                ActiveOverlay.AddAppPicker(targetFolderId = folder.id)
                                            )
                                        },
                                        onLaunchApp = { viewModel.launchApp(it) },
                                        onMoveOutToDesktop = { childId ->
                                            viewModel.moveShortcutToFolder(childId, folderId = null)
                                        },
                                        onDeleteItem = { childId -> viewModel.deleteDesktopItem(childId) }
                                    )
                                }
                            }
                            DesktopWindowKind.INTERNAL_SETTINGS -> {
                                LauncherSettingsWindow(
                                    windowState = win,
                                    preferences = uiState.preferences,
                                    canWriteSystemSettings = uiState.telemetry.canWriteSystemSettings,
                                    canDrawOverlays = uiState.telemetry.canDrawOverlays,
                                    isDefaultLauncher = uiState.telemetry.isDefaultLauncher,
                                    isActive = isActive,
                                    onFocus = { viewModel.focusWindow(win.windowId) },
                                    onMinimize = { viewModel.minimizeWindow(win.windowId) },
                                    onResizeCycle = { viewModel.resizeWindow(win.windowId) },
                                    onResizeSelect = { mode -> viewModel.resizeWindow(win.windowId, mode) },
                                    onClose = { viewModel.closeWindow(win.windowId) },
                                    onUpdateUserName = { viewModel.setUserDisplayName(it) },
                                    onUpdateWeatherCity = { viewModel.setWeatherCity(it) },
                                    onToggleShowLabels = { viewModel.setShowDesktopLabels(it) },
                                    onSelectGridDensity = { viewModel.setGridDensity(it) },
                                    onSelectDefaultWindowMode = { viewModel.setDefaultWindowMode(it) },
                                    onToggleForceLandscapeApps = { viewModel.setForceLandscapeApps(it) },
                                    onToggleFreeformBounds = { viewModel.setUseFreeformLaunchBounds(it) },
                                    onToggleKeepTaskbarOverApps = { viewModel.setKeepTaskbarOverApps(it) },
                                    onToggleAnchorBottomEdge = { viewModel.setAnchorTaskbarAtBottomEdge(it) },
                                    onRequestWriteSettingsPermission = { viewModel.openWriteSettingsPermission() },
                                    onRequestOverlayPermission = { viewModel.openOverlayPermissionSettings() },
                                    onOpenDeveloperSettings = { viewModel.openDeveloperSettings() },
                                    onOpenDefaultHomeSettings = onRequestDefaultLauncher,
                                    onOpenNavigationSettings = { viewModel.openGestureGuideDialog() }
                                )
                            }
                            DesktopWindowKind.INTERNAL_NOTES -> {
                                NotesDesktopWindow(
                                    windowState = win,
                                    stickyNotes = uiState.stickyNotes,
                                    isActive = isActive,
                                    onFocus = { viewModel.focusWindow(win.windowId) },
                                    onMinimize = { viewModel.minimizeWindow(win.windowId) },
                                    onResizeCycle = { viewModel.resizeWindow(win.windowId) },
                                    onResizeSelect = { mode -> viewModel.resizeWindow(win.windowId, mode) },
                                    onClose = { viewModel.closeWindow(win.windowId) },
                                    onAddStickyNote = { content, color ->
                                        viewModel.addStickyNote(content, color)
                                    },
                                    onDeleteStickyNote = { viewModel.deleteStickyNote(it) }
                                )
                            }
                            DesktopWindowKind.EXTERNAL_APP -> Unit
                        }
                    }
                }

                // Left Sliding Widgets Panel
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.activeOverlay is ActiveOverlay.Widgets,
                    enter = slideInHorizontally(
                        initialOffsetX = { -it / 2 },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    ) + fadeIn(),
                    exit = slideOutHorizontally(targetOffsetX = { -it / 2 }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = 10.dp, top = 8.dp)
                ) {
                    WidgetsPanelFlyout(
                        currentTimeMillis = uiState.currentTimeMillis,
                        weatherCity = uiState.preferences.weatherCity,
                        weatherState = uiState.weatherState,
                        telemetry = uiState.telemetry,
                        stickyNotes = uiState.stickyNotes,
                        onRefreshWeather = { viewModel.refreshWeather() },
                        onChangeWeatherCity = { viewModel.setWeatherCity(it) },
                        onAddStickyNote = { text, color -> viewModel.addStickyNote(text, color) },
                        onUpdateStickyNote = { viewModel.updateStickyNote(it) },
                        onDeleteStickyNote = { viewModel.deleteStickyNote(it) },
                        onExpandNotes = { viewModel.openNotesWindow() }
                    )
                }

                // Centered Start Menu Flyout
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.activeOverlay is ActiveOverlay.StartMenu,
                    enter = slideInVertically(
                        initialOffsetY = { it / 3 },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    ) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it / 3 }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp, top = 8.dp)
                ) {
                    StartMenuFlyout(
                        userDisplayName = uiState.preferences.userDisplayName,
                        installedApps = uiState.installedApps,
                        pinnedApps = uiState.startPinnedApps,
                        taskbarPinnedPackages = taskbarPinnedPackages,
                        recentApps = uiState.recentApps,
                        onLaunchApp = { viewModel.launchApp(it) },
                        onOpenSearch = { viewModel.toggleOverlay(ActiveOverlay.Search) },
                        onPinToDesktop = { app -> viewModel.addAppToDesktop(app, folderId = null) },
                        onTogglePin = { pkg, target -> viewModel.togglePin(pkg, target) },
                        onOpenAppInfo = { viewModel.openAppInfo(it) },
                        onUninstallApp = { viewModel.uninstallApp(it) },
                        onOpenLauncherSettings = { viewModel.openSettingsWindow() },
                        onOpenSystemSettings = { viewModel.openSystemSettings() }
                    )
                }

                // Centered Quick Search Flyout
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.activeOverlay is ActiveOverlay.Search,
                    enter = slideInVertically(
                        initialOffsetY = { it / 3 },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    ) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it / 3 }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp, top = 8.dp)
                ) {
                    SearchFlyout(
                        query = uiState.searchQuery,
                        installedApps = uiState.installedApps,
                        recentApps = uiState.recentApps,
                        desktopItems = uiState.allDesktopItems,
                        stickyNotes = uiState.stickyNotes,
                        onQueryChange = { viewModel.updateSearchQuery(it) },
                        onLaunchApp = { viewModel.launchApp(it) },
                        onOpenFolder = { folderId -> viewModel.openFolderWindow(folderId) },
                        onOpenWidgets = { viewModel.toggleOverlay(ActiveOverlay.Widgets) },
                        onAskCopilot = { query -> viewModel.askCopilotFromSearch(query) }
                    )
                }

                // Right-side Copilot AI Flyout (Firebase AI Logic)
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.activeOverlay is ActiveOverlay.Copilot,
                    enter = slideInHorizontally(
                        initialOffsetX = { it / 2 },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    ) + fadeIn(),
                    exit = slideOutHorizontally(targetOffsetX = { it / 2 }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 10.dp, top = 8.dp)
                ) {
                    CopilotFlyout(
                        copilotState = copilotState,
                        onSendPrompt = { viewModel.sendCopilotPrompt(it) },
                        onClearChat = { viewModel.clearCopilotChat() },
                        onSaveToNotes = { text ->
                            viewModel.addStickyNote(text, StickyNoteColor.BLUE)
                            viewModel.showBanner("Resposta salva nas Notas Rápidas")
                        },
                        onCopiedNotice = {
                            viewModel.showBanner("Texto copiado para a área de transferência")
                        }
                    )
                }

                // Bottom-Right Action Center Flyout
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.activeOverlay is ActiveOverlay.ActionCenter,
                    enter = slideInVertically(
                        initialOffsetY = { it / 3 },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    ) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it / 3 }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 10.dp)
                ) {
                    ActionCenterFlyout(
                        telemetry = uiState.telemetry,
                        forceLandscapeApps = uiState.preferences.forceLandscapeApps,
                        onOpenWifi = { viewModel.openWifiPanel() },
                        onOpenBluetooth = { viewModel.openBluetoothSettings() },
                        onToggleTorch = { viewModel.toggleTorch() },
                        onToggleMute = { viewModel.toggleMuteMedia() },
                        onOpenNetwork = { viewModel.openNetworkSettings() },
                        onOpenDefaultHome = onRequestDefaultLauncher,
                        onOpenOverlayPermission = { viewModel.openOverlayPermissionSettings() },
                        onToggleLandscapeLock = { viewModel.toggleLandscapeLockAction() },
                        onBrightnessChange = { viewModel.setWindowBrightness(it) },
                        onVolumeChange = { viewModel.setMediaVolume(it) },
                        onOpenSystemSettings = { viewModel.openSystemSettings() },
                        onOpenNavigationSettings = { viewModel.openGestureGuideDialog() }
                    )
                }

                // Taskbar Window Preview & Top-Right Controls Popover
                val popoverWin = uiState.taskbarPopoverWindowId?.let { popId ->
                    uiState.openWindows.firstOrNull { it.windowId == popId }
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = popoverWin != null,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 4 }),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                ) {
                    if (popoverWin != null) {
                        TaskbarWindowControlPopover(
                            window = popoverWin,
                            app = popoverWin.packageName?.let(uiState.installedAppsByPackage::get),
                            onMinimize = { viewModel.minimizeWindow(popoverWin.windowId) },
                            onResizeCycle = { viewModel.resizeWindow(popoverWin.windowId) },
                            onResizeSelect = { mode -> viewModel.resizeWindow(popoverWin.windowId, mode) },
                            onClose = { viewModel.closeWindow(popoverWin.windowId) },
                            onRestore = { viewModel.onTaskbarWindowClick(popoverWin.windowId) },
                            onOpenAppInfo = { pkg -> viewModel.openAppInfo(pkg) }
                        )
                    }
                }

                // Transient Status Banner
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.statusBanner != null,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                ) {
                    uiState.statusBanner?.let { msg ->
                        Surface(
                            color = Win11MicaSurface,
                            shape = RoundedCornerShape(20.dp),
                            shadowElevation = 8.dp,
                            modifier = Modifier.border(1.dp, Win11BorderHighlight, RoundedCornerShape(20.dp))
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.labelMedium,
                                color = Win11TextPrimary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Windows 11 Centered Taskbar
            val activity = androidx.compose.ui.platform.LocalContext.current as? ComponentActivity
            Taskbar(
                activeOverlay = uiState.activeOverlay,
                pinnedApps = uiState.taskbarPinnedApps,
                openWindows = uiState.openWindows,
                activeWindowId = uiState.activeWindowId,
                appsByPackage = uiState.installedAppsByPackage,
                telemetry = uiState.telemetry,
                weatherState = uiState.weatherState,
                currentTimeMillis = uiState.currentTimeMillis,
                anchorAtBottomEdge = uiState.preferences.anchorTaskbarAtBottomEdge,
                onBackClick = { activity?.onBackPressedDispatcher?.onBackPressed() },
                onToggleStart = { viewModel.toggleOverlay(ActiveOverlay.StartMenu) },
                onToggleSearch = { viewModel.toggleOverlay(ActiveOverlay.Search) },
                onToggleCopilot = { viewModel.toggleOverlay(ActiveOverlay.Copilot) },
                onToggleWidgets = { viewModel.toggleOverlay(ActiveOverlay.Widgets) },
                onToggleActionCenter = { viewModel.toggleOverlay(ActiveOverlay.ActionCenter) },
                onOpenDesktopMenu = { viewModel.setDesktopContextMenuVisible(true) },
                onLaunchPinnedApp = { viewModel.launchApp(it) },
                onWindowClick = { winId -> viewModel.onTaskbarWindowClick(winId) },
                onWindowLongClick = { winId -> viewModel.toggleTaskbarWindowPopover(winId) }
            )
        }
    }

    // Dialogs & Modals
    if (uiState.showDesktopContextMenu) {
        DesktopContextMenuDialog(
            onDismiss = { viewModel.setDesktopContextMenuVisible(false) },
            onCreateFolder = { showCreateFolderDialog = true },
            onAddApp = { viewModel.toggleOverlay(ActiveOverlay.AddAppPicker(targetFolderId = null)) },
            onOpenNotes = { viewModel.openNotesWindow() },
            onRefreshApps = {
                viewModel.refreshInstalledApps(seedIfNeeded = false)
                viewModel.showBanner("Lista de aplicativos atualizada")
            },
            onOpenSettings = { viewModel.openSettingsWindow() },
            onSetDefaultLauncher = onRequestDefaultLauncher
        )
    }

    if (showCreateFolderDialog) {
        TextInputDialog(
            title = "Criar nova pasta",
            initialValue = "Nova Pasta",
            label = "Nome da pasta",
            confirmText = "Criar",
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = { name ->
                viewModel.createFolder(name)
                showCreateFolderDialog = false
            }
        )
    }

    renameTargetItem?.let { target ->
        TextInputDialog(
            title = "Renomear item",
            initialValue = target.title,
            label = "Novo nome",
            confirmText = "Salvar",
            onDismiss = { renameTargetItem = null },
            onConfirm = { newName ->
                viewModel.renameDesktopItem(target.id, newName)
                renameTargetItem = null
            }
        )
    }

    moveTargetItem?.let { target ->
        if (folders.isNotEmpty()) {
            MoveToFolderDialog(
                folders = folders,
                onDismiss = { moveTargetItem = null },
                onSelectFolder = { folderId ->
                    viewModel.moveShortcutToFolder(target.id, folderId)
                    moveTargetItem = null
                }
            )
        }
    }

    val addAppOverlay = uiState.activeOverlay as? ActiveOverlay.AddAppPicker
    if (addAppOverlay != null) {
        val targetFolder = addAppOverlay.targetFolderId?.let { id ->
            uiState.desktopRootItems.firstOrNull { it.id == id }
        }
        AddAppPickerDialog(
            installedApps = uiState.installedApps,
            targetFolderTitle = targetFolder?.title,
            onDismiss = { viewModel.dismissOverlay() },
            onSelectApp = { app ->
                viewModel.addAppToDesktop(app, addAppOverlay.targetFolderId)
                if (targetFolder != null) {
                    viewModel.openFolderWindow(targetFolder.id)
                } else {
                    viewModel.dismissOverlay()
                }
            }
        )
    }

    val pendingLaunch = uiState.pendingOverlayPermissionLaunch
    if (pendingLaunch != null) {
        OverlayPermissionPromptDialog(
            targetAppLabel = pendingLaunch.appLabel,
            onGrantPermission = { viewModel.confirmOverlayPermissionSetup() },
            onContinueWithoutOverlay = { viewModel.launchPendingAppWithoutOverlay() },
            onDismiss = { viewModel.dismissOverlayPermissionPrompt() }
        )
    }

    if (uiState.showGestureGuideDialog) {
        GestureNavigationGuideDialog(
            anchorTaskbarAtBottomEdge = uiState.preferences.anchorTaskbarAtBottomEdge,
            onToggleAnchorBottomEdge = { viewModel.setAnchorTaskbarAtBottomEdge(it) },
            onOpenSystemSettings = { viewModel.openSystemNavigationSettings() },
            onDismiss = { viewModel.dismissGestureGuideDialog() }
        )
    }
}
