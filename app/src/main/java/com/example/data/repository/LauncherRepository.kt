package com.example.data.repository

import com.example.data.local.DesktopItemEntity
import com.example.data.local.LauncherDao
import com.example.data.local.PinnedAppEntity
import com.example.data.local.RecentAppEntity
import com.example.data.local.StickyNoteEntity
import com.example.data.model.DesktopItemType
import com.example.data.model.GridDensity
import com.example.data.model.InstalledApp
import com.example.data.model.LauncherPreferences
import com.example.data.model.PinTarget
import com.example.data.model.StickyNoteColor
import com.example.data.model.SystemTelemetry
import com.example.data.model.WeatherState
import com.example.data.model.WindowSizeMode
import com.example.data.preferences.LauncherPreferencesRepository
import com.example.data.system.SystemBridge
import com.example.data.weather.OpenMeteoClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class LauncherRepository(
    private val dao: LauncherDao,
    private val prefsRepo: LauncherPreferencesRepository,
    private val systemBridge: SystemBridge
) {
    val desktopItems: Flow<List<DesktopItemEntity>> = dao.observeAllDesktopItems()
    val pinnedApps: Flow<List<PinnedAppEntity>> = dao.observePinnedApps()
    val stickyNotes: Flow<List<StickyNoteEntity>> = dao.observeStickyNotes()
    val recentApps: Flow<List<RecentAppEntity>> = dao.observeRecentApps()
    val preferences: Flow<LauncherPreferences> = prefsRepo.preferencesFlow

    suspend fun loadInstalledApps(): List<InstalledApp> = systemBridge.loadInstalledApps()

    suspend fun ensureInitialSeed(installedApps: List<InstalledApp>) {
        val prefs = prefsRepo.preferencesFlow.first()
        if (prefs.hasSeededDefaults) return

        if (dao.getDesktopItemCount() == 0 && installedApps.isNotEmpty()) {
            val folderId = dao.insertDesktopItem(
                DesktopItemEntity(
                    type = DesktopItemType.FOLDER.name,
                    title = "Utilitários",
                    orderIndex = 0
                )
            )

            // Put up to 4 shortcuts directly on desktop and up to 3 inside the folder
            val desktopCandidates = installedApps.take(4)
            desktopCandidates.forEachIndexed { idx, app ->
                dao.insertDesktopItem(
                    DesktopItemEntity(
                        type = DesktopItemType.SHORTCUT.name,
                        title = app.label,
                        packageName = app.packageName,
                        folderId = null,
                        orderIndex = idx + 1
                    )
                )
            }

            val folderCandidates = installedApps.drop(4).take(3)
            folderCandidates.forEachIndexed { idx, app ->
                dao.insertDesktopItem(
                    DesktopItemEntity(
                        type = DesktopItemType.SHORTCUT.name,
                        title = app.label,
                        packageName = app.packageName,
                        folderId = folderId,
                        orderIndex = idx
                    )
                )
            }

            // Pin up to 8 apps to Start Menu and 4 to Taskbar
            val startPins = installedApps.take(8).mapIndexed { index, app ->
                PinnedAppEntity(app.packageName, PinTarget.START.name, index)
            }
            val taskbarPins = installedApps.take(4).mapIndexed { index, app ->
                PinnedAppEntity(app.packageName, PinTarget.TASKBAR.name, index)
            }
            dao.insertPinnedApps(startPins + taskbarPins)
        }

        if (dao.getStickyNoteCount() == 0) {
            dao.insertStickyNote(
                StickyNoteEntity(
                    content = "Dica: toque e segure no papel de parede para criar pastas ou personalizar a área de trabalho.",
                    colorKey = StickyNoteColor.BLUE.key
                )
            )
        }

        prefsRepo.markDefaultsSeeded()
    }

    suspend fun recordAppLaunch(packageName: String) {
        val existing = dao.getRecentApp(packageName)
        dao.upsertRecentApp(
            RecentAppEntity(
                packageName = packageName,
                lastLaunchedAt = System.currentTimeMillis(),
                launchCount = (existing?.launchCount ?: 0) + 1
            )
        )
    }

    suspend fun createFolder(title: String) {
        val count = dao.getDesktopItemCount()
        dao.insertDesktopItem(
            DesktopItemEntity(
                type = DesktopItemType.FOLDER.name,
                title = title.trim().ifEmpty { "Nova Pasta" },
                orderIndex = count
            )
        )
    }

    suspend fun addAppToDesktop(app: InstalledApp, folderId: Long?) {
        val count = dao.getDesktopItemCount()
        dao.insertDesktopItem(
            DesktopItemEntity(
                type = DesktopItemType.SHORTCUT.name,
                title = app.label,
                packageName = app.packageName,
                folderId = folderId,
                orderIndex = count
            )
        )
    }

    suspend fun moveShortcutToFolder(itemId: Long, folderId: Long?) {
        val item = dao.getDesktopItemById(itemId) ?: return
        dao.updateDesktopItem(item.copy(folderId = folderId))
    }

    suspend fun renameDesktopItem(itemId: Long, newTitle: String) {
        val item = dao.getDesktopItemById(itemId) ?: return
        val sanitized = newTitle.trim()
        if (sanitized.isNotEmpty()) {
            dao.updateDesktopItem(item.copy(title = sanitized))
        }
    }

    suspend fun deleteDesktopItem(itemId: Long) {
        dao.deleteDesktopItemAndChildren(itemId)
    }

    suspend fun pinApp(packageName: String, target: PinTarget) {
        dao.insertPinnedApp(
            PinnedAppEntity(
                packageName = packageName,
                target = target.name,
                orderIndex = (System.currentTimeMillis() % 100000).toInt()
            )
        )
    }

    suspend fun unpinApp(packageName: String, target: PinTarget) {
        dao.removePinnedApp(packageName, target.name)
    }

    suspend fun addStickyNote(content: String, color: StickyNoteColor) {
        if (content.isBlank()) return
        dao.insertStickyNote(
            StickyNoteEntity(
                content = content.trim(),
                colorKey = color.key,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateStickyNote(note: StickyNoteEntity) {
        dao.updateStickyNote(note.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteStickyNote(id: Long) {
        dao.deleteStickyNote(id)
    }

    suspend fun setWeatherCity(city: String) = prefsRepo.setWeatherCity(city)
    suspend fun setShowDesktopLabels(show: Boolean) = prefsRepo.setShowDesktopLabels(show)
    suspend fun setGridDensity(density: GridDensity) = prefsRepo.setGridDensity(density)
    suspend fun setUserDisplayName(name: String) = prefsRepo.setUserDisplayName(name)
    suspend fun setForceLandscapeApps(enabled: Boolean) = prefsRepo.setForceLandscapeApps(enabled)
    suspend fun setUseFreeformLaunchBounds(enabled: Boolean) = prefsRepo.setUseFreeformLaunchBounds(enabled)
    suspend fun setDefaultWindowMode(mode: WindowSizeMode) = prefsRepo.setDefaultWindowMode(mode)
    suspend fun setKeepTaskbarOverApps(enabled: Boolean) = prefsRepo.setKeepTaskbarOverApps(enabled)
    suspend fun setAnchorTaskbarAtBottomEdge(enabled: Boolean) = prefsRepo.setAnchorTaskbarAtBottomEdge(enabled)

    suspend fun fetchWeather(cityQuery: String): WeatherState = withContext(Dispatchers.IO) {
        try {
            val geo = OpenMeteoClient.geocodingApi.searchCity(name = cityQuery.trim())
            val location = geo.results?.firstOrNull()
                ?: return@withContext WeatherState.Error(
                    message = "Cidade não encontrada",
                    cityName = cityQuery
                )

            val forecast = OpenMeteoClient.forecastApi.getCurrentWeather(
                latitude = location.latitude,
                longitude = location.longitude
            )
            val current = forecast.currentWeather
                ?: return@withContext WeatherState.Error(
                    message = "Sem dados climáticos no momento",
                    cityName = location.name
                )

            WeatherState.Success(
                cityName = location.name,
                country = location.country ?: "",
                temperatureCelsius = current.temperature.roundToInt(),
                conditionLabel = OpenMeteoClient.weatherCodeToLabelPt(current.weathercode),
                weatherCode = current.weathercode,
                windSpeedKmh = current.windspeed.roundToInt(),
                isDay = current.isDay == 1
            )
        } catch (_: Exception) {
            WeatherState.Error(
                message = "Sem conexão para atualizar o clima",
                cityName = cityQuery
            )
        }
    }

    fun launchApp(
        packageName: String,
        forceLandscape: Boolean = true,
        useFreeformBounds: Boolean = false,
        windowSizeMode: WindowSizeMode = WindowSizeMode.MAXIMIZED,
        reserveBottomTaskbarPx: Int = 0
    ): Boolean = systemBridge.launchApp(
        packageName = packageName,
        forceLandscape = forceLandscape,
        useFreeformBounds = useFreeformBounds,
        windowSizeMode = windowSizeMode,
        reserveBottomTaskbarPx = reserveBottomTaskbarPx
    )

    fun canWriteSystemSettings(): Boolean = systemBridge.canWriteSystemSettings()
    fun canDrawOverlays(): Boolean = systemBridge.canDrawOverlays()
    fun isDefaultLauncher(): Boolean = systemBridge.isDefaultLauncher()
    fun createDefaultLauncherPromptIntent() = systemBridge.createDefaultLauncherPromptIntent()
    fun openWriteSettingsPermission() = systemBridge.openWriteSettingsPermission()
    fun openOverlayPermissionSettings() = systemBridge.openOverlayPermissionSettings()
    fun openDeveloperSettings() = systemBridge.openDeveloperSettings()
    fun enforceLandscapeRotation(): Boolean = systemBridge.enforceLandscapeRotation()
    fun openAppInfo(packageName: String) = systemBridge.openAppInfo(packageName)
    fun uninstallApp(packageName: String) = systemBridge.uninstallApp(packageName)
    fun openWifiPanel() = systemBridge.openWifiPanel()
    fun openBluetoothSettings() = systemBridge.openBluetoothSettings()
    fun openNetworkSettings() = systemBridge.openNetworkSettings()
    fun openDisplaySettings() = systemBridge.openDisplaySettings()
    fun openSystemSettings() = systemBridge.openSystemSettings()
    fun openDefaultHomeSettings() = systemBridge.openDefaultHomeSettings()
    fun openSystemNavigationSettings() = systemBridge.openSystemNavigationSettings()
    fun setMediaVolume(volume: Int) = systemBridge.setMediaVolume(volume)
    fun toggleTorch(): Boolean? = systemBridge.toggleTorch()
    fun readTelemetry(windowBrightness: Float): SystemTelemetry = systemBridge.readTelemetry(windowBrightness)
}
