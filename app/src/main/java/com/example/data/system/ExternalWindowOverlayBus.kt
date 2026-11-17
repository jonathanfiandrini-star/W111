package com.example.data.system

import com.example.data.model.DesktopWindowKind
import com.example.data.model.WindowSizeMode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

data class ExternalAppSession(
    val packageName: String,
    val appLabel: String,
    val sizeMode: WindowSizeMode,
    val forceLandscape: Boolean
)

data class TaskbarOverlayEntry(
    val id: String,
    val label: String,
    val packageName: String?,
    val kind: DesktopWindowKind,
    val isOpen: Boolean,
    val isActive: Boolean,
    val sizeMode: WindowSizeMode = WindowSizeMode.MAXIMIZED
)

data class TaskbarOverlaySnapshot(
    val entries: List<TaskbarOverlayEntry> = emptyList(),
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val wifiConnected: Boolean = false,
    val weatherTempLabel: String? = null,
    val keepTaskbarOverApps: Boolean = true,
    val anchorTaskbarAtBottomEdge: Boolean = false
)

sealed interface ExternalWindowAction {
    data class Minimize(val packageName: String) : ExternalWindowAction
    data class Resize(val packageName: String, val sizeMode: WindowSizeMode) : ExternalWindowAction
    data class Close(val packageName: String) : ExternalWindowAction
    data class SwitchToApp(val packageName: String, val appLabel: String, val sizeMode: WindowSizeMode) : ExternalWindowAction
}

object ExternalWindowOverlayBus {
    private val _activeSession = MutableStateFlow<ExternalAppSession?>(null)
    val activeSession: StateFlow<ExternalAppSession?> = _activeSession.asStateFlow()

    private val _taskbarSnapshot = MutableStateFlow(TaskbarOverlaySnapshot())
    val taskbarSnapshot: StateFlow<TaskbarOverlaySnapshot> = _taskbarSnapshot.asStateFlow()

    private val _actions = MutableSharedFlow<ExternalWindowAction>(extraBufferCapacity = 24)
    val actions: SharedFlow<ExternalWindowAction> = _actions.asSharedFlow()

    fun setSession(session: ExternalAppSession) {
        _activeSession.value = session
    }

    fun updateSessionMode(packageName: String, mode: WindowSizeMode) {
        val current = _activeSession.value
        if (current != null && current.packageName == packageName) {
            _activeSession.value = current.copy(sizeMode = mode)
        }
    }

    fun updateTaskbarSnapshot(snapshot: TaskbarOverlaySnapshot) {
        _taskbarSnapshot.value = snapshot
    }

    fun clearSession() {
        _activeSession.value = null
    }

    fun emitAction(action: ExternalWindowAction) {
        _actions.tryEmit(action)
    }
}
