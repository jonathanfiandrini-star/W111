package com.example.data.model

import androidx.compose.ui.graphics.ImageBitmap

data class InstalledApp(
    val packageName: String,
    val activityName: String,
    val label: String,
    val icon: ImageBitmap?
)

enum class DesktopItemType {
    SHORTCUT,
    FOLDER
}

enum class PinTarget {
    START,
    TASKBAR
}

enum class StickyNoteColor(val key: String, val label: String) {
    AMBER("amber", "Âmbar"),
    BLUE("blue", "Azul Mica"),
    MINT("mint", "Menta"),
    LAVENDER("lavender", "Lavanda");

    companion object {
        fun fromKey(key: String): StickyNoteColor =
            entries.firstOrNull { it.key == key } ?: BLUE
    }
}

enum class GridDensity(val key: String, val label: String, val cellSizeDp: Int) {
    COMPACT("compact", "Compacta", 82),
    STANDARD("standard", "Padrão", 94),
    LARGE("large", "Grande", 108);

    companion object {
        fun fromKey(key: String): GridDensity =
            entries.firstOrNull { it.key == key } ?: STANDARD
    }
}

enum class WindowSizeMode(val key: String, val label: String) {
    COMPACT("compact", "Compacta"),
    SPLIT_LEFT("split_left", "Dividida Esq."),
    SPLIT_RIGHT("split_right", "Dividida Dir."),
    MAXIMIZED("maximized", "Maximizada");

    fun nextMode(): WindowSizeMode = when (this) {
        COMPACT -> SPLIT_LEFT
        SPLIT_LEFT -> SPLIT_RIGHT
        SPLIT_RIGHT -> MAXIMIZED
        MAXIMIZED -> COMPACT
    }

    companion object {
        fun fromKey(key: String): WindowSizeMode =
            entries.firstOrNull { it.key == key } ?: COMPACT
    }
}

enum class DesktopWindowKind {
    INTERNAL_FOLDER,
    INTERNAL_SETTINGS,
    INTERNAL_NOTES,
    EXTERNAL_APP
}

data class DesktopWindowState(
    val windowId: String,
    val kind: DesktopWindowKind,
    val title: String,
    val packageName: String? = null,
    val folderId: Long? = null,
    val sizeMode: WindowSizeMode = WindowSizeMode.COMPACT,
    val isMinimized: Boolean = false,
    val lastFocusedAt: Long = System.currentTimeMillis()
)

data class LauncherPreferences(
    val weatherCity: String = "São Paulo",
    val showDesktopLabels: Boolean = true,
    val gridDensity: GridDensity = GridDensity.STANDARD,
    val userDisplayName: String = "Usuário",
    val hasSeededDefaults: Boolean = false,
    val forceLandscapeApps: Boolean = true,
    val useFreeformLaunchBounds: Boolean = false,
    val defaultWindowMode: WindowSizeMode = WindowSizeMode.MAXIMIZED,
    val keepTaskbarOverApps: Boolean = true
)

data class SystemTelemetry(
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val wifiConnected: Boolean = false,
    val bluetoothEnabled: Boolean = false,
    val isTorchOn: Boolean = false,
    val torchAvailable: Boolean = false,
    val mediaVolume: Int = 0,
    val maxMediaVolume: Int = 15,
    val windowBrightness: Float = 0.75f,
    val ramUsedGb: Float = 0f,
    val ramTotalGb: Float = 0f,
    val storageUsedGb: Float = 0f,
    val storageTotalGb: Float = 0f,
    val canWriteSystemSettings: Boolean = false,
    val isRotationLockedLandscape: Boolean = false,
    val isDefaultLauncher: Boolean = false,
    val canDrawOverlays: Boolean = false
)

sealed interface WeatherState {
    data object Loading : WeatherState
    data class Success(
        val cityName: String,
        val country: String,
        val temperatureCelsius: Int,
        val conditionLabel: String,
        val weatherCode: Int,
        val windSpeedKmh: Int,
        val isDay: Boolean
    ) : WeatherState

    data class Error(val message: String, val cityName: String) : WeatherState
}

sealed interface ActiveOverlay {
    data object None : ActiveOverlay
    data object StartMenu : ActiveOverlay
    data object Search : ActiveOverlay
    data object Widgets : ActiveOverlay
    data object ActionCenter : ActiveOverlay
    data object Copilot : ActiveOverlay
    data class AddAppPicker(val targetFolderId: Long? = null) : ActiveOverlay
}

data class CopilotChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val isStreaming: Boolean = false
)

data class CopilotUiState(
    val messages: List<CopilotChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
