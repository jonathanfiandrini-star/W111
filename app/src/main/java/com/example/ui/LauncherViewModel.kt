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
import com.example.data.model.PinTarget
import com.example.data.model.StickyNoteColor
import com.example.data.model.SystemTelemetry
import com.example.data.model.WeatherState
import com.example.data.model.WindowSizeMode
import com.example.data.preferences.LauncherPreferencesRepository
import com.example.data.repository.LauncherRepository
import com.example.data.system.SystemBridge
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    val taskbarPopoverWindowId: String? = null
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
        val contextMenu: Boolean
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
        desktopContextMenuFlow
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        WindowStreams(
            windows = arr[0] as List<DesktopWindowState>,
            activeWindowId = arr[1] as String?,
            popoverWindowId = arr[2] as String?,
            timeMillis = arr[3] as Long,
            banner = arr[4] as String?,
            contextMenu = arr[5] as Boolean
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
            taskbarPopoverWindowId = win.popoverWindowId
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
            launchApp(existing.packageName, overrideSizeMode = nextMode)
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

    fun launchApp(packageName: String, overrideSizeMode: WindowSizeMode? = null) {
        val prefs = uiState.value.preferences
        val existingWin = openWindowsFlow.value.firstOrNull { it.windowId == "app_$packageName" }
        val modeToUse = overrideSizeMode ?: existingWin?.sizeMode ?: prefs.defaultWindowMode

        val launched = repository.launchApp(
            packageName = packageName,
            forceLandscape = prefs.forceLandscapeApps,
            useFreeformBounds = prefs.useFreeformLaunchBounds,
            windowSizeMode = modeToUse
        )
        if (launched) {
            val appLabel = uiState.value.installedAppsByPackage[packageName]?.label ?: packageName
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
            showBanner("Não foi possível abrir este aplicativo.")
        }
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
    fun openDeveloperSettings() = repository.openDeveloperSettings()

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
        const val WINDOW_ID_SETTINGS = "internal_settings"
        const val WINDOW_ID_NOTES = "internal_notes"
    }
}
