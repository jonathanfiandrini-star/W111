package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.DesktopItemEntity
import com.example.data.local.PinnedAppEntity
import com.example.data.local.RecentAppEntity
import com.example.data.local.StickyNoteEntity
import com.example.data.model.ActiveOverlay
import com.example.data.model.DesktopItemType
import com.example.data.model.DesktopWindowKind
import com.example.data.model.DesktopWindowState
import com.example.data.model.GridDensity
import com.example.data.model.InstalledApp
import com.example.data.model.LauncherPreferences
import com.example.data.model.PendingAppLaunch
import com.example.data.model.PinTarget
import com.example.data.model.StickyNoteColor
import com.example.data.model.SystemTelemetry
import com.example.data.model.WeatherState
import com.example.data.model.WindowSizeMode
import android.content.Intent
import android.util.Log
import com.example.data.ai.CopilotHistoryTurn
import com.example.data.ai.CopilotRepository
import com.example.data.model.CopilotChatMessage
import com.example.data.model.CopilotUiState
import com.example.data.preferences.LauncherPreferencesRepository
import com.example.data.repository.LauncherRepository
import com.example.data.system.ExternalAppSession
import com.example.data.system.ExternalWindowAction
import com.example.data.system.ExternalWindowOverlayBus
import com.example.data.system.SystemBridge
import com.example.data.system.TaskbarOverlayEntry
import com.example.data.system.TaskbarOverlaySnapshot
import com.example.service.AppWindowOverlayService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.math.roundToInt

data class LauncherUiState(
    val installedApps: List<InstalledApp> = emptyList(),
    val installedAppsByPackage: Map<String, InstalledApp> = emptyMap(),
    val desktopRootItems: List<DesktopItemEntity> = emptyList(),
    val allDesktopItems: List<DesktopItemEntity> = emptyList(),
    val startPinnedApps: List<InstalledApp> = emptyList(),
    val taskbarPinnedApps: List<InstalledApp> = emptyList(),
    val recentApps: List<InstalledApp> = emptyList(),
    val stickyNotes: List<StickyNoteEntity> = emptyList(),
    val preferences: LauncherPreferences = LauncherPreferences(),
    val activeOverlay: ActiveOverlay = ActiveOverlay.None,
    val searchQuery: String = "",
    val telemetry: SystemTelemetry = SystemTelemetry(),
    val weatherState: WeatherState = WeatherState.Loading,
    val currentTimeMillis: Long = System.currentTimeMillis(),
    val statusBanner: String? = null,
    val showDesktopContextMenu: Boolean = false,
    val openWindows: List<DesktopWindowState> = emptyList(),
    val activeWindowId: String? = null,
    val taskbarPopoverWindowId: String? = null,
    val pendingOverlayPermissionLaunch: PendingAppLaunch? = null,
    val showGestureGuideDialog: Boolean = false,
    val showFreeformGuideDialog: Boolean = false
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LauncherRepository = LauncherRepository(
        dao = AppDatabase.getInstance(application).launcherDao(),
        prefsRepo = LauncherPreferencesRepository(application),
        systemBridge = SystemBridge(application)
    )

    private val installedAppsFlow = MutableStateFlow<List<InstalledApp>>(emptyList())
    private val activeOverlayFlow = MutableStateFlow<ActiveOverlay>(ActiveOverlay.None)
    private val searchQueryFlow = MutableStateFlow("")
    private val windowBrightnessFlow = MutableStateFlow(0.8f)
    private val telemetryFlow = MutableStateFlow(SystemTelemetry())
    private val weatherStateFlow = MutableStateFlow<WeatherState>(WeatherState.Loading)
    private val currentTimeFlow = MutableStateFlow(System.currentTimeMillis())
    private val statusBannerFlow = MutableStateFlow<String?>(null)
    private val desktopContextMenuFlow = MutableStateFlow(false)
    private val openWindowsFlow = MutableStateFlow<List<DesktopWindowState>>(emptyList())
    private val activeWindowIdFlow = MutableStateFlow<String?>(null)
    private val taskbarPopoverWindowIdFlow = MutableStateFlow<String?>(null)
    private val pendingOverlayPermissionLaunchFlow = MutableStateFlow<PendingAppLaunch?>(null)
    private val showGestureGuideDialogFlow = MutableStateFlow(false)
    private val showFreeformGuideDialogFlow = MutableStateFlow(false)
    private var pendingPermissionReturnLaunch: PendingAppLaunch? = null
    private var lastExternalLaunchAtMs: Long = 0L
    private var hasPromptedDefaultLauncherThisSession = false

    private val copilotRepository = CopilotRepository()
    private val _copilotUiState = MutableStateFlow(CopilotUiState())
    val copilotUiState: StateFlow<CopilotUiState> = _copilotUiState.asStateFlow()
    private var copilotJob: Job? = null

    private data class CoreStreams(
        val apps: List<InstalledApp>,
        val desktop: List<DesktopItemEntity>,
        val pinned: List<PinnedAppEntity>,
        val notes: List<StickyNoteEntity>,
        val recents: List<RecentAppEntity>
    )

    private data class UiControlStreams(
        val prefs: LauncherPreferences,
        val overlay: ActiveOverlay,
        val query: String,
        val telemetry: SystemTelemetry,
        val weather: WeatherState
    )

    private data class WindowStreams(
        val windows: List<DesktopWindowState>,
        val activeWindowId: String?,
        val popoverWindowId: String?,
        val timeMillis: Long,
        val banner: String?,
        val contextMenu: Boolean,
        val pendingLaunch: PendingAppLaunch?,
        val showGestureGuide: Boolean,
        val showFreeformGuide: Boolean
    )

    private val coreStreamsFlow = combine(
        installedAppsFlow,
        repository.desktopItems,
        repository.pinnedApps,
        repository.stickyNotes,
        repository.recentApps
    ) { apps, desktop, pinned, notes, recents ->
        CoreStreams(apps, desktop, pinned, notes, recents)
    }

    private val uiControlStreamsFlow = combine(
        repository.preferences,
        activeOverlayFlow,
        searchQueryFlow,
        telemetryFlow,
        weatherStateFlow
    ) { prefs, overlay, query, telemetry, weather ->
        UiControlStreams(prefs, overlay, query, telemetry, weather)
    }

    private val windowStreamsFlow = combine(
        openWindowsFlow,
        activeWindowIdFlow,
        taskbarPopoverWindowIdFlow,
        currentTimeFlow,
        statusBannerFlow,
        desktopContextMenuFlow,
        pendingOverlayPermissionLaunchFlow,
        showGestureGuideDialogFlow,
        showFreeformGuideDialogFlow
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        WindowStreams(
            windows = arr[0] as List<DesktopWindowState>,
            activeWindowId = arr[1] as String?,
            popoverWindowId = arr[2] as String?,
            timeMillis = arr[3] as Long,
            banner = arr[4] as String?,
            contextMenu = arr[5] as Boolean,
            pendingLaunch = arr[6] as PendingAppLaunch?,
            showGestureGuide = arr[7] as Boolean,
            showFreeformGuide = arr[8] as Boolean
        )
    }

    val uiState: StateFlow<LauncherUiState> = combine(
        coreStreamsFlow,
        uiControlStreamsFlow,
        windowStreamsFlow
    ) { core, controls, win ->
        val appsMap = core.apps.associateBy { it.packageName }
        val startPins = core.pinned
            .filter { it.target == PinTarget.START.name }
            .mapNotNull { appsMap[it.packageName] }
        val taskbarPins = core.pinned
            .filter { it.target == PinTarget.TASKBAR.name }
            .mapNotNull { appsMap[it.packageName] }
        val recents = core.recents.mapNotNull { appsMap[it.packageName] }
        val rootItems = core.desktop.filter { it.folderId == null }

        LauncherUiState(
            installedApps = core.apps,
            installedAppsByPackage = appsMap,
            desktopRootItems = rootItems,
            allDesktopItems = core.desktop,
            startPinnedApps = startPins,
            taskbarPinnedApps = taskbarPins,
            recentApps = recents,
            stickyNotes = core.notes,
            preferences = controls.prefs,
            activeOverlay = controls.overlay,
            searchQuery = controls.query,
            telemetry = controls.telemetry,
            weatherState = controls.weather,
            currentTimeMillis = win.timeMillis,
            statusBanner = win.banner,
            showDesktopContextMenu = win.contextMenu,
            openWindows = win.windows,
            activeWindowId = win.activeWindowId,
            taskbarPopoverWindowId = win.popoverWindowId,
            pendingOverlayPermissionLaunch = win.pendingLaunch,
            showGestureGuideDialog = win.showGestureGuide,
            showFreeformGuideDialog = win.showFreeformGuide
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LauncherUiState()
    )

    init {
        refreshInstalledApps(seedIfNeeded = true)
        startPeriodicTickers()
        observeWeatherCity()
        observeExternalOverlayActions()
        syncTaskbarOverlaySnapshot()
    }

    private fun syncTaskbarOverlaySnapshot() {
        viewModelScope.launch {
            combine(
                coreStreamsFlow,
                uiControlStreamsFlow,
                openWindowsFlow,
                activeWindowIdFlow
            ) { core, controls, windows, activeWinId ->
                val appsMap = core.apps.associateBy { it.packageName }
                val pinnedBar = core.pinned
                    .filter { it.target == PinTarget.TASKBAR.name }
                    .mapNotNull { appsMap[it.packageName] }
                val pinnedPackages = pinnedBar.map { it.packageName }.toSet()
                val windowsByPkg = windows.filter { it.packageName != null }.associateBy { it.packageName!! }

                val entries = mutableListOf<TaskbarOverlayEntry>()
                pinnedBar.forEach { app ->
                    val openWin = windowsByPkg[app.packageName]
                    entries += TaskbarOverlayEntry(
                        id = openWin?.windowId ?: "pin_${app.packageName}",
                        label = app.label,
                        packageName = app.packageName,
                        kind = DesktopWindowKind.EXTERNAL_APP,
                        isOpen = openWin != null,
                        isActive = openWin != null && !openWin.isMinimized && activeWinId == openWin.windowId,
                        sizeMode = openWin?.sizeMode ?: controls.prefs.defaultWindowMode
                    )
                }
                windows.filter { it.packageName == null || !pinnedPackages.contains(it.packageName) }
                    .forEach { win ->
                        entries += TaskbarOverlayEntry(
                            id = win.windowId,
                            label = win.title,
                            packageName = win.packageName,
                            kind = win.kind,
                            isOpen = true,
                            isActive = !win.isMinimized && activeWinId == win.windowId,
                            sizeMode = win.sizeMode
                        )
                    }

                val weatherTemp = (controls.weather as? WeatherState.Success)?.let { "${it.temperatureCelsius}°C" }
                TaskbarOverlaySnapshot(
                    entries = entries,
                    batteryPercent = controls.telemetry.batteryPercent,
                    isCharging = controls.telemetry.isCharging,
                    wifiConnected = controls.telemetry.wifiConnected,
                    weatherTempLabel = weatherTemp,
                    keepTaskbarOverApps = controls.prefs.keepTaskbarOverApps,
                    anchorTaskbarAtBottomEdge = controls.prefs.anchorTaskbarAtBottomEdge,
                    useFreeformBounds = controls.prefs.useFreeformLaunchBounds
                )
            }.collect { snapshot ->
                ExternalWindowOverlayBus.updateTaskbarSnapshot(snapshot)
            }
        }
    }

    private fun observeExternalOverlayActions() {
        viewModelScope.launch {
            ExternalWindowOverlayBus.actions.collect { action ->
                when (action) {
                    is ExternalWindowAction.Minimize -> handleExternalWindowMinimize(action.packageName)
                    is ExternalWindowAction.Resize -> {
                        val winId = "app_${action.packageName}"
                        openWindowsFlow.value = openWindowsFlow.value.map {
                            if (it.windowId == winId) {
                                it.copy(sizeMode = action.sizeMode, lastFocusedAt = System.currentTimeMillis())
                            } else it
                        }
                    }
                    is ExternalWindowAction.Close -> handleExternalWindowClose(action.packageName)
                    is ExternalWindowAction.SwitchToApp -> {
                        val winId = "app_${action.packageName}"
                        val now = System.currentTimeMillis()
                        val prev = openWindowsFlow.value.map { win ->
                            if (win.kind == DesktopWindowKind.EXTERNAL_APP && win.windowId != winId) {
                                win.copy(isMinimized = true)
                            } else win
                        }
                        val exists = prev.any { it.windowId == winId }
                        openWindowsFlow.value = if (exists) {
                            prev.map {
                                if (it.windowId == winId) {
                                    it.copy(isMinimized = false, sizeMode = action.sizeMode, lastFocusedAt = now)
                                } else it
                            }
                        } else {
                            prev + DesktopWindowState(
                                windowId = winId,
                                kind = DesktopWindowKind.EXTERNAL_APP,
                                title = action.appLabel,
                                packageName = action.packageName,
                                sizeMode = action.sizeMode,
                                isMinimized = false,
                                lastFocusedAt = now
                            )
                        }
                        activeWindowIdFlow.value = winId
                        launch { repository.recordAppLaunch(action.packageName) }
                    }
                }
            }
        }
    }

    private fun startPeriodicTickers() {
        viewModelScope.launch {
            while (true) {
                currentTimeFlow.value = System.currentTimeMillis()
                telemetryFlow.value = repository.readTelemetry(windowBrightnessFlow.value)
                delay(10_000L)
            }
        }
    }

    private fun observeWeatherCity() {
        viewModelScope.launch {
            var lastCity: String? = null
            repository.preferences.collect { prefs ->
                if (prefs.weatherCity != lastCity) {
                    lastCity = prefs.weatherCity
                    refreshWeather(prefs.weatherCity)
                }
            }
        }
    }

    fun refreshInstalledApps(seedIfNeeded: Boolean = false) {
        viewModelScope.launch {
            val apps = repository.loadInstalledApps()
            installedAppsFlow.value = apps
            if (seedIfNeeded) {
                repository.ensureInitialSeed(apps)
            }
            telemetryFlow.value = repository.readTelemetry(windowBrightnessFlow.value)
        }
    }

    fun refreshTelemetry() {
        if (uiState.value.preferences.forceLandscapeApps && repository.canWriteSystemSettings()) {
            repository.enforceLandscapeRotation()
        }
        telemetryFlow.value = repository.readTelemetry(windowBrightnessFlow.value)
    }

    fun onLauncherResumed() {
        val waitingLaunch = pendingPermissionReturnLaunch
        if (waitingLaunch != null) {
            pendingPermissionReturnLaunch = null
            refreshTelemetry()
            if (repository.canDrawOverlays()) {
                showBanner("Sobreposição ativada! Abrindo ${waitingLaunch.appLabel}...")
                launchApp(
                    packageName = waitingLaunch.packageName,
                    overrideSizeMode = waitingLaunch.windowSizeMode,
                    bypassOverlayPrompt = true
                )
                return
            } else {
                showBanner("Permissão de sobreposição ainda desativada")
            }
        }

        // Guard against transient onResume events fired right as an external app is launching
        val elapsedSinceLaunch = System.currentTimeMillis() - lastExternalLaunchAtMs
        if (lastExternalLaunchAtMs > 0L && elapsedSinceLaunch in 0L..1500L) {
            refreshTelemetry()
            return
        }

        AppWindowOverlayService.stopOverlay(getApplication())
        ExternalWindowOverlayBus.clearSession()
        // When returning from an external app, mark external app windows as minimized in the Taskbar
        val updated = openWindowsFlow.value.map { win ->
            if (win.kind == DesktopWindowKind.EXTERNAL_APP) win.copy(isMinimized = true) else win
        }
        openWindowsFlow.value = updated
        if (activeWindowIdFlow.value?.startsWith("app_") == true) {
            activeWindowIdFlow.value = updated.lastOrNull { !it.isMinimized }?.windowId
        }
        refreshInstalledApps(seedIfNeeded = false)
        refreshTelemetry()
    }

    fun handleExternalWindowMinimize(packageName: String) {
        if (packageName.isBlank()) return
        minimizeWindow("app_$packageName")
    }

    fun handleExternalWindowClose(packageName: String) {
        if (packageName.isBlank()) return
        closeWindow("app_$packageName")
    }

    fun refreshWeather(cityOverride: String? = null) {
        val city = cityOverride ?: uiState.value.preferences.weatherCity
        viewModelScope.launch {
            weatherStateFlow.value = WeatherState.Loading
            weatherStateFlow.value = repository.fetchWeather(city)
        }
    }

    fun toggleOverlay(overlay: ActiveOverlay) {
        desktopContextMenuFlow.value = false
        taskbarPopoverWindowIdFlow.value = null
        val current = activeOverlayFlow.value
        val next = if (current == overlay) ActiveOverlay.None else overlay
        activeOverlayFlow.value = next
        if (next != ActiveOverlay.Search) {
            searchQueryFlow.value = ""
        }
        if (next == ActiveOverlay.ActionCenter || next == ActiveOverlay.Widgets) {
            refreshTelemetry()
        }
    }

    fun dismissOverlay() {
        activeOverlayFlow.value = ActiveOverlay.None
        desktopContextMenuFlow.value = false
        taskbarPopoverWindowIdFlow.value = null
        searchQueryFlow.value = ""
    }

    fun setDesktopContextMenuVisible(visible: Boolean) {
        desktopContextMenuFlow.value = visible
        if (visible) {
            activeOverlayFlow.value = ActiveOverlay.None
            taskbarPopoverWindowIdFlow.value = null
        }
    }

    fun updateSearchQuery(query: String) {
        searchQueryFlow.value = query
    }

    // --- Window Management (Minimize, Resize, Close, Restore, Snap) ---

    fun openFolderWindow(folderId: Long) {
        val folder = uiState.value.desktopRootItems.firstOrNull { it.id == folderId } ?: return
        val winId = "folder_$folderId"
        openOrFocusInternalWindow(
            DesktopWindowState(
                windowId = winId,
                kind = DesktopWindowKind.INTERNAL_FOLDER,
                title = folder.title,
                folderId = folderId,
                sizeMode = WindowSizeMode.COMPACT
            )
        )
    }

    fun openSettingsWindow() {
        openOrFocusInternalWindow(
            DesktopWindowState(
                windowId = WINDOW_ID_SETTINGS,
                kind = DesktopWindowKind.INTERNAL_SETTINGS,
                title = "Personalização do Fluent 11",
                sizeMode = WindowSizeMode.COMPACT
            )
        )
    }

    fun openNotesWindow() {
        openOrFocusInternalWindow(
            DesktopWindowState(
                windowId = WINDOW_ID_NOTES,
                kind = DesktopWindowKind.INTERNAL_NOTES,
                title = "Notas Rápidas",
                sizeMode = WindowSizeMode.COMPACT
            )
        )
    }

    private fun openOrFocusInternalWindow(template: DesktopWindowState) {
        dismissOverlay()
        val now = System.currentTimeMillis()
        val currentList = openWindowsFlow.value
        val existing = currentList.firstOrNull { it.windowId == template.windowId }
        val updated = if (existing != null) {
            currentList.map {
                if (it.windowId == template.windowId) {
                    it.copy(title = template.title, isMinimized = false, lastFocusedAt = now)
                } else it
            }
        } else {
            currentList + template.copy(isMinimized = false, lastFocusedAt = now)
        }
        openWindowsFlow.value = updated
        activeWindowIdFlow.value = template.windowId
    }

    fun focusWindow(windowId: String) {
        val now = System.currentTimeMillis()
        openWindowsFlow.value = openWindowsFlow.value.map {
            if (it.windowId == windowId) it.copy(isMinimized = false, lastFocusedAt = now) else it
        }
        activeWindowIdFlow.value = windowId
        taskbarPopoverWindowIdFlow.value = null
    }

    fun minimizeWindow(windowId: String) {
        val updated = openWindowsFlow.value.map {
            if (it.windowId == windowId) it.copy(isMinimized = true) else it
        }
        openWindowsFlow.value = updated
        if (activeWindowIdFlow.value == windowId) {
            activeWindowIdFlow.value = updated
                .filter { !it.isMinimized && it.kind != DesktopWindowKind.EXTERNAL_APP }
                .maxByOrNull { it.lastFocusedAt }
                ?.windowId
        }
        taskbarPopoverWindowIdFlow.value = null
    }

    fun resizeWindow(windowId: String, targetMode: WindowSizeMode? = null) {
        val existing = openWindowsFlow.value.firstOrNull { it.windowId == windowId } ?: return
        val nextMode = targetMode ?: existing.sizeMode.nextMode()
        val now = System.currentTimeMillis()

        openWindowsFlow.value = openWindowsFlow.value.map {
            if (it.windowId == windowId) {
                it.copy(sizeMode = nextMode, isMinimized = false, lastFocusedAt = now)
            } else it
        }
        activeWindowIdFlow.value = windowId
        taskbarPopoverWindowIdFlow.value = null

        if (existing.kind == DesktopWindowKind.EXTERNAL_APP && existing.packageName != null) {
            if (!repository.isSystemFreeformReady()) {
                showBanner("Ative 'Janelas de forma livre' em Opções do Desenvolvedor para redimensionar apps.")
            }
            launchApp(
                existing.packageName,
                overrideSizeMode = nextMode,
                isExplicitResize = true
            )
        }
    }

    fun closeWindow(windowId: String) {
        val updated = openWindowsFlow.value.filterNot { it.windowId == windowId }
        openWindowsFlow.value = updated
        if (activeWindowIdFlow.value == windowId) {
            activeWindowIdFlow.value = updated
                .filter { !it.isMinimized && it.kind != DesktopWindowKind.EXTERNAL_APP }
                .maxByOrNull { it.lastFocusedAt }
                ?.windowId
        }
        if (taskbarPopoverWindowIdFlow.value == windowId) {
            taskbarPopoverWindowIdFlow.value = null
        }
    }

    fun onTaskbarWindowClick(windowId: String) {
        val win = openWindowsFlow.value.firstOrNull { it.windowId == windowId } ?: return
        taskbarPopoverWindowIdFlow.value = null
        activeOverlayFlow.value = ActiveOverlay.None

        if (win.kind == DesktopWindowKind.EXTERNAL_APP && win.packageName != null) {
            launchApp(win.packageName, overrideSizeMode = win.sizeMode)
        } else {
            if (win.isMinimized) {
                focusWindow(windowId)
            } else if (activeWindowIdFlow.value == windowId) {
                minimizeWindow(windowId)
            } else {
                focusWindow(windowId)
            }
        }
    }

    fun toggleTaskbarWindowPopover(windowId: String?) {
        activeOverlayFlow.value = ActiveOverlay.None
        desktopContextMenuFlow.value = false
        taskbarPopoverWindowIdFlow.value = if (taskbarPopoverWindowIdFlow.value == windowId) {
            null
        } else {
            windowId
        }
    }

    fun launchApp(
        packageName: String,
        overrideSizeMode: WindowSizeMode? = null,
        bypassOverlayPrompt: Boolean = false,
        isExplicitResize: Boolean = false
    ) {
        val prefs = uiState.value.preferences
        val existingWin = openWindowsFlow.value.firstOrNull { it.windowId == "app_$packageName" }
        val modeToUse = overrideSizeMode ?: existingWin?.sizeMode ?: prefs.defaultWindowMode
        val appLabel = uiState.value.installedAppsByPackage[packageName]?.label ?: packageName
        val hasOverlayPermission = repository.canDrawOverlays()

        if (!hasOverlayPermission && !bypassOverlayPrompt) {
            pendingOverlayPermissionLaunchFlow.value = PendingAppLaunch(
                packageName = packageName,
                appLabel = appLabel,
                windowSizeMode = modeToUse
            )
            return
        }

        pendingOverlayPermissionLaunchFlow.value = null

        val density = getApplication<Application>().resources.displayMetrics.density
        val reserveTaskbarPx = if (prefs.keepTaskbarOverApps && hasOverlayPermission) {
            (58f * density).roundToInt()
        } else {
            0
        }
        val reserveTopBarPx = if (hasOverlayPermission) {
            (40f * density).roundToInt()
        } else {
            0
        }

        // Start foreground overlay service BEFORE launching external app while launcher is still foreground
        if (hasOverlayPermission) {
            ExternalWindowOverlayBus.setSession(
                ExternalAppSession(
                    packageName = packageName,
                    appLabel = appLabel,
                    sizeMode = modeToUse,
                    forceLandscape = prefs.forceLandscapeApps,
                    useFreeformBounds = prefs.useFreeformLaunchBounds
                )
            )
            AppWindowOverlayService.startOverlay(
                context = getApplication(),
                packageName = packageName,
                appLabel = appLabel,
                sizeMode = modeToUse,
                forceLandscape = prefs.forceLandscapeApps,
                useFreeformBounds = prefs.useFreeformLaunchBounds,
                keepTaskbarOverApps = prefs.keepTaskbarOverApps,
                anchorTaskbarAtBottomEdge = prefs.anchorTaskbarAtBottomEdge
            )
        }

        lastExternalLaunchAtMs = System.currentTimeMillis()
        val launched = repository.launchApp(
            packageName = packageName,
            forceLandscape = prefs.forceLandscapeApps,
            useFreeformBounds = prefs.useFreeformLaunchBounds,
            windowSizeMode = modeToUse,
            reserveBottomTaskbarPx = reserveTaskbarPx,
            reserveTopBarPx = reserveTopBarPx,
            anchorTaskbarAtBottomEdge = prefs.anchorTaskbarAtBottomEdge,
            isExplicitResize = isExplicitResize
        )
        if (launched) {
            val winId = "app_$packageName"
            val now = System.currentTimeMillis()
            val updatedList = if (existingWin != null) {
                openWindowsFlow.value.map {
                    if (it.windowId == winId) {
                        it.copy(
                            title = appLabel,
                            sizeMode = modeToUse,
                            isMinimized = false,
                            lastFocusedAt = now
                        )
                    } else it
                }
            } else {
                openWindowsFlow.value + DesktopWindowState(
                    windowId = winId,
                    kind = DesktopWindowKind.EXTERNAL_APP,
                    title = appLabel,
                    packageName = packageName,
                    sizeMode = modeToUse,
                    isMinimized = false,
                    lastFocusedAt = now
                )
            }
            openWindowsFlow.value = updatedList
            activeWindowIdFlow.value = winId

            viewModelScope.launch { repository.recordAppLaunch(packageName) }
            dismissOverlay()
        } else {
            lastExternalLaunchAtMs = 0L
            if (hasOverlayPermission) {
                AppWindowOverlayService.stopOverlay(getApplication())
                ExternalWindowOverlayBus.clearSession()
            }
            showBanner("Não foi possível abrir este aplicativo.")
        }
    }

    fun confirmOverlayPermissionSetup() {
        val pending = pendingOverlayPermissionLaunchFlow.value
        if (pending != null) {
            pendingPermissionReturnLaunch = pending
            pendingOverlayPermissionLaunchFlow.value = null
        }
        repository.openOverlayPermissionSettings()
    }

    fun launchPendingAppWithoutOverlay() {
        val pending = pendingOverlayPermissionLaunchFlow.value ?: return
        pendingOverlayPermissionLaunchFlow.value = null
        launchApp(
            packageName = pending.packageName,
            overrideSizeMode = pending.windowSizeMode,
            bypassOverlayPrompt = true
        )
    }

    fun dismissOverlayPermissionPrompt() {
        pendingOverlayPermissionLaunchFlow.value = null
    }

    fun openAppInfo(packageName: String) = repository.openAppInfo(packageName)
    fun uninstallApp(packageName: String) = repository.uninstallApp(packageName)

    fun createFolder(title: String) {
        viewModelScope.launch {
            repository.createFolder(title)
            showBanner("Pasta \"$title\" criada na área de trabalho")
        }
    }

    fun addAppToDesktop(app: InstalledApp, folderId: Long?) {
        viewModelScope.launch {
            repository.addAppToDesktop(app, folderId)
            showBanner("${app.label} adicionado")
        }
    }

    fun moveShortcutToFolder(itemId: Long, folderId: Long?) {
        viewModelScope.launch {
            repository.moveShortcutToFolder(itemId, folderId)
            showBanner(if (folderId == null) "Movido para a área de trabalho" else "Movido para a pasta")
        }
    }

    fun renameDesktopItem(itemId: Long, newTitle: String) {
        viewModelScope.launch {
            repository.renameDesktopItem(itemId, newTitle)
            val winId = "folder_$itemId"
            openWindowsFlow.value = openWindowsFlow.value.map {
                if (it.windowId == winId) it.copy(title = newTitle.trim()) else it
            }
        }
    }

    fun deleteDesktopItem(itemId: Long) {
        viewModelScope.launch {
            repository.deleteDesktopItem(itemId)
            closeWindow("folder_$itemId")
            showBanner("Item removido da área de trabalho")
        }
    }

    fun togglePin(packageName: String, target: PinTarget) {
        viewModelScope.launch {
            val isPinned = when (target) {
                PinTarget.START -> uiState.value.startPinnedApps.any { it.packageName == packageName }
                PinTarget.TASKBAR -> uiState.value.taskbarPinnedApps.any { it.packageName == packageName }
            }
            if (isPinned) {
                repository.unpinApp(packageName, target)
                showBanner(
                    if (target == PinTarget.TASKBAR) "Desafixado da Barra de Tarefas"
                    else "Desafixado do Menu Iniciar"
                )
            } else {
                repository.pinApp(packageName, target)
                showBanner(
                    if (target == PinTarget.TASKBAR) "Fixado na Barra de Tarefas"
                    else "Fixado no Menu Iniciar"
                )
            }
        }
    }

    fun addStickyNote(content: String, color: StickyNoteColor) {
        viewModelScope.launch {
            repository.addStickyNote(content, color)
        }
    }

    fun updateStickyNote(note: StickyNoteEntity) {
        viewModelScope.launch {
            repository.updateStickyNote(note)
        }
    }

    fun deleteStickyNote(id: Long) {
        viewModelScope.launch {
            repository.deleteStickyNote(id)
        }
    }

    fun setWeatherCity(city: String) {
        viewModelScope.launch { repository.setWeatherCity(city) }
    }

    fun setShowDesktopLabels(show: Boolean) {
        viewModelScope.launch { repository.setShowDesktopLabels(show) }
    }

    fun setGridDensity(density: GridDensity) {
        viewModelScope.launch { repository.setGridDensity(density) }
    }

    fun setUserDisplayName(name: String) {
        viewModelScope.launch { repository.setUserDisplayName(name) }
    }

    fun setForceLandscapeApps(enabled: Boolean) {
        viewModelScope.launch {
            repository.setForceLandscapeApps(enabled)
            if (enabled) {
                if (repository.canWriteSystemSettings()) {
                    repository.enforceLandscapeRotation()
                    showBanner("Rotação horizontal travada para aplicativos")
                } else {
                    showBanner("Conceda a permissão de sistema para travar a rotação")
                }
            }
            refreshTelemetry()
        }
    }

    fun setUseFreeformLaunchBounds(enabled: Boolean) {
        viewModelScope.launch {
            repository.setUseFreeformLaunchBounds(enabled)
            if (enabled) {
                showBanner("Modo janela livre ativado ao abrir aplicativos")
            }
        }
    }

    fun setDefaultWindowMode(mode: WindowSizeMode) {
        viewModelScope.launch {
            repository.setDefaultWindowMode(mode)
            showBanner("Modo padrão de janela: ${mode.label}")
        }
    }

    fun setKeepTaskbarOverApps(enabled: Boolean) {
        viewModelScope.launch {
            repository.setKeepTaskbarOverApps(enabled)
            showBanner(
                if (enabled) "Barra de Tarefas fixa sobre aplicativos ativada"
                else "Barra de Tarefas oculta ao abrir aplicativos"
            )
        }
    }

    fun setAnchorTaskbarAtBottomEdge(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAnchorTaskbarAtBottomEdge(enabled)
            showBanner(
                if (enabled) "Barra colada na borda 0px (recomendado com Gestos)"
                else "Barra ajustada acima da navegação do sistema"
            )
        }
    }

    fun openGestureGuideDialog() {
        dismissOverlay()
        showGestureGuideDialogFlow.value = true
    }

    fun dismissGestureGuideDialog() {
        showGestureGuideDialogFlow.value = false
    }

    fun openFreeformGuideDialog() {
        dismissOverlay()
        refreshTelemetry()
        showFreeformGuideDialogFlow.value = true
    }

    fun dismissFreeformGuideDialog() {
        showFreeformGuideDialogFlow.value = false
    }

    fun openOwnAppSettings() = repository.openOwnAppSettings()

    // --- Copilot AI (Firebase AI Logic) ---

    fun askCopilotFromSearch(query: String) {
        activeOverlayFlow.value = ActiveOverlay.Copilot
        searchQueryFlow.value = ""
        if (query.isNotBlank()) {
            sendCopilotPrompt(query)
        }
    }

    fun sendCopilotPrompt(rawPrompt: String) {
        val prompt = rawPrompt.trim()
        if (prompt.isEmpty() || _copilotUiState.value.isLoading) return

        val priorHistory = _copilotUiState.value.messages
            .filter { !it.isStreaming && it.text.isNotBlank() }
            .map { CopilotHistoryTurn(isUser = it.isUser, text = it.text) }

        val userMsgId = "u_${System.currentTimeMillis()}"
        val modelMsgId = "m_${System.currentTimeMillis() + 1}"

        _copilotUiState.value = _copilotUiState.value.copy(
            messages = _copilotUiState.value.messages +
                CopilotChatMessage(id = userMsgId, isUser = true, text = prompt) +
                CopilotChatMessage(id = modelMsgId, isUser = false, text = "", isStreaming = true),
            isLoading = true,
            errorMessage = null
        )

        val appLabels = uiState.value.installedApps.map { it.label }
        copilotJob?.cancel()
        copilotJob = viewModelScope.launch {
            val acc = StringBuilder()
            try {
                withTimeout(60_000L) {
                    copilotRepository.streamResponse(
                        prompt = prompt,
                        installedAppNames = appLabels,
                        history = priorHistory
                    ).collect { chunk ->
                        acc.append(chunk)
                        updateStreamingMessage(modelMsgId, acc.toString(), streaming = true)
                    }
                }
                val finalText = acc.toString().trim()
                if (finalText.isNotEmpty()) {
                    updateStreamingMessage(modelMsgId, finalText, streaming = false)
                    _copilotUiState.value = _copilotUiState.value.copy(isLoading = false)
                } else {
                    removeMessage(modelMsgId)
                    _copilotUiState.value = _copilotUiState.value.copy(
                        isLoading = false,
                        errorMessage = "O modelo retornou uma resposta vazia."
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Copilot generation timed out", e)
                removeMessageIfBlank(modelMsgId)
                _copilotUiState.value = _copilotUiState.value.copy(
                    isLoading = false,
                    errorMessage = "A resposta expirou após 60 segundos. Tente novamente."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Copilot Firebase AI generation failed", e)
                removeMessageIfBlank(modelMsgId)
                _copilotUiState.value = _copilotUiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Falha ao consultar o Gemini via Firebase AI Logic."
                )
            }
        }
    }

    fun clearCopilotChat() {
        copilotJob?.cancel()
        _copilotUiState.value = CopilotUiState()
    }

    private fun updateStreamingMessage(id: String, text: String, streaming: Boolean) {
        _copilotUiState.value = _copilotUiState.value.copy(
            messages = _copilotUiState.value.messages.map {
                if (it.id == id) it.copy(text = text, isStreaming = streaming) else it
            }
        )
    }

    private fun removeMessage(id: String) {
        _copilotUiState.value = _copilotUiState.value.copy(
            messages = _copilotUiState.value.messages.filterNot { it.id == id }
        )
    }

    private fun removeMessageIfBlank(id: String) {
        _copilotUiState.value = _copilotUiState.value.copy(
            messages = _copilotUiState.value.messages.filterNot { it.id == id && it.text.isBlank() }
        )
    }

    fun toggleLandscapeLockAction() {
        val prefs = uiState.value.preferences
        if (!repository.canWriteSystemSettings()) {
            repository.openWriteSettingsPermission()
            showBanner("Ative \"Permitir modificar configurações do sistema\"")
            return
        }
        setForceLandscapeApps(!prefs.forceLandscapeApps)
    }

    fun openWriteSettingsPermission() = repository.openWriteSettingsPermission()
    fun openOverlayPermissionSettings() = repository.openOverlayPermissionSettings()
    fun openDeveloperSettings() = repository.openDeveloperSettings()

    fun consumeStartupDefaultLauncherIntent(): Intent? {
        if (hasPromptedDefaultLauncherThisSession) return null
        hasPromptedDefaultLauncherThisSession = true
        return repository.createDefaultLauncherPromptIntent()
    }

    fun getDefaultLauncherPromptIntent(): Intent? = repository.createDefaultLauncherPromptIntent()

    fun onDefaultLauncherRoleResult() {
        refreshTelemetry()
        if (repository.isDefaultLauncher()) {
            showBanner("Fluent 11 definido como launcher padrão!")
        }
    }

    fun setMediaVolume(volume: Int) {
        repository.setMediaVolume(volume)
        refreshTelemetry()
    }

    fun toggleMuteMedia() {
        val current = uiState.value.telemetry
        val target = if (current.mediaVolume > 0) 0 else (current.maxMediaVolume / 2).coerceAtLeast(1)
        setMediaVolume(target)
    }

    fun setWindowBrightness(brightness: Float) {
        windowBrightnessFlow.value = brightness.coerceIn(0.1f, 1.0f)
        refreshTelemetry()
    }

    fun toggleTorch() {
        val result = repository.toggleTorch()
        if (result == null) {
            showBanner("Lanterna indisponível neste dispositivo")
        } else {
            refreshTelemetry()
        }
    }

    fun openWifiPanel() = repository.openWifiPanel()
    fun openBluetoothSettings() = repository.openBluetoothSettings()
    fun openNetworkSettings() = repository.openNetworkSettings()
    fun openDisplaySettings() = repository.openDisplaySettings()
    fun openSystemSettings() = repository.openSystemSettings()
    fun openDefaultHomeSettings() = repository.openDefaultHomeSettings()
    fun openSystemNavigationSettings() = repository.openSystemNavigationSettings()

    fun getFolderChildren(folderId: Long): List<DesktopItemEntity> {
        return uiState.value.allDesktopItems.filter {
            it.folderId == folderId && it.type == DesktopItemType.SHORTCUT.name
        }
    }

    fun showBanner(message: String) {
        viewModelScope.launch {
            statusBannerFlow.value = message
            delay(2800L)
            if (statusBannerFlow.value == message) {
                statusBannerFlow.value = null
            }
        }
    }

    companion object {
        private const val TAG = "LauncherViewModel"
        const val WINDOW_ID_SETTINGS = "internal_settings"
        const val WINDOW_ID_NOTES = "internal_notes"
    }
}
