package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.util.LruCache
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.DesktopWindowKind
import com.example.data.model.WindowSizeMode
import com.example.data.system.ExternalAppSession
import com.example.data.system.ExternalWindowAction
import com.example.data.system.ExternalWindowOverlayBus
import com.example.data.system.SystemBridge
import com.example.data.system.TaskbarOverlayEntry
import com.example.data.system.TaskbarOverlaySnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class AppWindowOverlayService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val iconCache = LruCache<String, Drawable>(64)

    private var windowManager: WindowManager? = null

    // Top-right floating caption bar
    private var captionRootView: View? = null
    private var captionLayoutParams: WindowManager.LayoutParams? = null
    private var snapStripView: LinearLayout? = null
    private var titleLabelView: TextView? = null
    private var modeChipView: TextView? = null
    private var snapButtons: Map<WindowSizeMode, TextView> = emptyMap()

    // Bottom persistent Windows 11 Taskbar
    private var taskbarRootView: View? = null
    private var taskbarCenterRow: LinearLayout? = null
    private var taskbarWidgetsText: TextView? = null
    private var taskbarTrayStatusText: TextView? = null
    private var taskbarTimeText: TextView? = null
    private var taskbarDateText: TextView? = null

    private var currentPackageName: String = ""
    private var currentAppLabel: String = ""
    private var currentSizeMode: WindowSizeMode = WindowSizeMode.MAXIMIZED
    private var forceLandscape: Boolean = true
    private var keepTaskbarOverApps: Boolean = true
    private var snapMenuExpanded: Boolean = false
    private var clockTickerJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as? WindowManager
        startForegroundSafely()
        observeSnapshotAndClock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundSafely()

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        val session = ExternalWindowOverlayBus.activeSession.value
        val pkg = intent?.getStringExtra(EXTRA_PACKAGE_NAME) ?: session?.packageName.orEmpty()
        if (pkg.isBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }

        currentPackageName = pkg
        currentAppLabel = intent?.getStringExtra(EXTRA_APP_LABEL) ?: session?.appLabel ?: pkg
        val modeKey = intent?.getStringExtra(EXTRA_WINDOW_SIZE_MODE)
        currentSizeMode = modeKey?.let { WindowSizeMode.fromKey(it) }
            ?: session?.sizeMode
            ?: WindowSizeMode.MAXIMIZED
        forceLandscape = intent?.getBooleanExtra(EXTRA_FORCE_LANDSCAPE, session?.forceLandscape ?: true) ?: true
        keepTaskbarOverApps = intent?.getBooleanExtra(
            EXTRA_KEEP_TASKBAR,
            ExternalWindowOverlayBus.taskbarSnapshot.value.keepTaskbarOverApps
        ) ?: true

        ensureCaptionOverlayAttached()
        if (keepTaskbarOverApps) {
            ensureTaskbarOverlayAttached()
        } else {
            removeTaskbarOverlayView()
        }
        refreshOverlayLabels()
        renderTaskbarSnapshot(ExternalWindowOverlayBus.taskbarSnapshot.value)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        clockTickerJob?.cancel()
        serviceScope.cancel()
        removeCaptionOverlayView()
        removeTaskbarOverlayView()
        super.onDestroy()
    }

    private fun observeSnapshotAndClock() {
        serviceScope.launch {
            ExternalWindowOverlayBus.taskbarSnapshot.collect { snapshot ->
                keepTaskbarOverApps = snapshot.keepTaskbarOverApps
                if (keepTaskbarOverApps && currentPackageName.isNotBlank()) {
                    ensureTaskbarOverlayAttached()
                    renderTaskbarSnapshot(snapshot)
                } else {
                    removeTaskbarOverlayView()
                }
            }
        }
        clockTickerJob = serviceScope.launch {
            while (isActive) {
                updateClockLabels()
                delay(15_000L)
            }
        }
    }

    private fun startForegroundSafely() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager != null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.overlay_notif_channel_name),
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            manager.createNotificationChannel(channel)
        }

        val returnIntent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_MINIMIZE_EXTERNAL_APP
            putExtra(EXTRA_PACKAGE_NAME, currentPackageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingReturn = PendingIntent.getActivity(
            this,
            101,
            returnIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.overlay_notif_title))
            .setContentText(getString(R.string.overlay_notif_body))
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(pendingReturn)
            .build()

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun ensureCaptionOverlayAttached() {
        if (captionRootView != null) return
        val wm = windowManager ?: return

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
        }

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(4), dp(6), dp(4))
            background = GradientDrawable().apply {
                setColor(COLOR_MICA_BG)
                cornerRadius = dp(12).toFloat()
                setStroke(dp(1), COLOR_BORDER_HIGHLIGHT)
            }
            elevation = dp(10).toFloat()
        }

        val titleView = TextView(this).apply {
            setTextColor(COLOR_TEXT_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            maxWidth = dp(128)
            setPadding(dp(2), 0, dp(6), 0)
        }
        titleLabelView = titleView

        val chipView = TextView(this).apply {
            setTextColor(COLOR_ACCENT_CYAN)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(6), dp(2), dp(6), dp(2))
            background = GradientDrawable().apply {
                setColor(COLOR_CARD_SURFACE)
                cornerRadius = dp(6).toFloat()
            }
            setOnClickListener {
                snapMenuExpanded = !snapMenuExpanded
                snapStripView?.visibility = if (snapMenuExpanded) View.VISIBLE else View.GONE
            }
        }
        modeChipView = chipView

        val minimizeBtn = createCaptionButton(
            symbol = "—",
            contentDesc = "Minimizar aplicativo",
            bgColor = Color.TRANSPARENT,
            textColor = COLOR_TEXT_PRIMARY
        ) {
            onMinimizeClicked()
        }

        val resizeBtn = createCaptionButton(
            symbol = "□",
            contentDesc = "Redimensionar janela do aplicativo",
            bgColor = Color.TRANSPARENT,
            textColor = COLOR_TEXT_PRIMARY
        ) {
            cycleWindowSize()
        }.apply {
            setOnLongClickListener {
                snapMenuExpanded = !snapMenuExpanded
                snapStripView?.visibility = if (snapMenuExpanded) View.VISIBLE else View.GONE
                true
            }
        }

        val closeBtn = createCaptionButton(
            symbol = "✕",
            contentDesc = "Fechar aplicativo e voltar ao desktop",
            bgColor = COLOR_CLOSE_SUBTLE,
            textColor = COLOR_TEXT_PRIMARY
        ) {
            onCloseClicked()
        }

        bar.addView(titleView)
        bar.addView(chipView)
        bar.addView(spacer(dp(6)))
        bar.addView(minimizeBtn)
        bar.addView(spacer(dp(2)))
        bar.addView(resizeBtn)
        bar.addView(spacer(dp(2)))
        bar.addView(closeBtn)

        val strip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            setPadding(dp(6), dp(6), dp(6), dp(6))
            background = GradientDrawable().apply {
                setColor(COLOR_MICA_BG)
                cornerRadius = dp(10).toFloat()
                setStroke(dp(1), COLOR_BORDER_SUBTLE)
            }
            elevation = dp(8).toFloat()
        }
        val stripLP = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(4)
        }

        val buttonsMap = mutableMapOf<WindowSizeMode, TextView>()
        WindowSizeMode.entries.forEachIndexed { idx, mode ->
            if (idx > 0) strip.addView(spacer(dp(4)))
            val chip = TextView(this).apply {
                text = shortModeLabel(mode)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                typeface = Typeface.DEFAULT_BOLD
                setPadding(dp(10), dp(6), dp(10), dp(6))
                minHeight = dp(32)
                gravity = Gravity.CENTER
                setOnClickListener {
                    snapMenuExpanded = false
                    strip.visibility = View.GONE
                    applyWindowSize(mode)
                }
            }
            buttonsMap[mode] = chip
            strip.addView(chip)
        }
        snapButtons = buttonsMap
        snapStripView = strip

        container.addView(bar)
        container.addView(strip, stripLP)

        val overlayType = overlayWindowType()
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(14)
            y = dp(32)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        var dragging = false
        titleView.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    dragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (touchX - event.rawX).roundToInt()
                    val dy = (event.rawY - touchY).roundToInt()
                    if (abs(dx) > dp(4) || abs(dy) > dp(4)) {
                        dragging = true
                    }
                    if (dragging) {
                        params.x = (startX + dx).coerceAtLeast(dp(4))
                        params.y = (startY + dy).coerceAtLeast(dp(8))
                        runCatching { wm.updateViewLayout(container, params) }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragging) {
                        snapMenuExpanded = !snapMenuExpanded
                        strip.visibility = if (snapMenuExpanded) View.VISIBLE else View.GONE
                    }
                    true
                }
                else -> false
            }
        }

        captionLayoutParams = params
        captionRootView = container
        try {
            wm.addView(container, params)
        } catch (e: Exception) {
            Log.e("AppWindowOverlay", "Failed to attach caption overlay", e)
            captionRootView = null
        }
    }

    private fun ensureTaskbarOverlayAttached() {
        if (taskbarRootView != null) return
        val wm = windowManager ?: return

        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(COLOR_TASKBAR_BG)
            elevation = dp(16).toFloat()
        }

        // Top 1dp hairline border
        val divider = View(this).apply {
            setBackgroundColor(COLOR_BORDER_SUBTLE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
            )
        }
        wrapper.addView(divider)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(4), dp(8), dp(4))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(54)
            )
        }

        // Left: Widgets button
        val widgetsBtn = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(6), dp(10), dp(6))
            minimumHeight = dp(42)
            background = GradientDrawable().apply {
                setColor(COLOR_TRANSPARENT_BTN)
                cornerRadius = dp(8).toFloat()
            }
            setOnClickListener { openDesktopOverlay(OVERLAY_TARGET_WIDGETS) }
        }
        val widgetsDot = TextView(this).apply {
            text = "☀"
            setTextColor(COLOR_ACCENT_CYAN)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(0, 0, dp(6), 0)
        }
        val widgetsTxt = TextView(this).apply {
            text = "Widgets"
            setTextColor(COLOR_TEXT_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
        }
        taskbarWidgetsText = widgetsTxt
        widgetsBtn.addView(widgetsDot)
        widgetsBtn.addView(widgetsTxt)
        row.addView(widgetsBtn)

        // Center: Scrollable Start, Search, Copilot, Pinned & Open Apps
        val scroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                marginStart = dp(6)
                marginEnd = dp(6)
            }
        }
        val centerContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            )
        }
        taskbarCenterRow = centerContainer
        scroll.addView(centerContainer)
        row.addView(scroll)

        // Right: Tray + Clock
        val rightCluster = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val trayBtn = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(6), dp(8), dp(6))
            minimumHeight = dp(40)
            background = GradientDrawable().apply {
                setColor(COLOR_TRANSPARENT_BTN)
                cornerRadius = dp(8).toFloat()
            }
            setOnClickListener { openDesktopOverlay(OVERLAY_TARGET_ACTION_CENTER) }
        }
        val trayStatusTxt = TextView(this).apply {
            text = "Wi-Fi • 100%"
            setTextColor(COLOR_TEXT_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.DEFAULT_BOLD
        }
        taskbarTrayStatusText = trayStatusTxt
        trayBtn.addView(trayStatusTxt)

        val clockBtn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(4), dp(8), dp(4))
            minimumHeight = dp(40)
            setOnClickListener { openDesktopOverlay(OVERLAY_TARGET_WIDGETS) }
        }
        val timeTxt = TextView(this).apply {
            setTextColor(COLOR_TEXT_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
        }
        val dateTxt = TextView(this).apply {
            setTextColor(COLOR_TEXT_SECONDARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
        }
        taskbarTimeText = timeTxt
        taskbarDateText = dateTxt
        clockBtn.addView(timeTxt)
        clockBtn.addView(dateTxt)

        rightCluster.addView(trayBtn)
        rightCluster.addView(clockBtn)
        row.addView(rightCluster)

        wrapper.addView(row)

        val navInsetBottom = computeNavigationBarBottomInset()
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.START
            x = 0
            y = navInsetBottom
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        taskbarRootView = wrapper
        try {
            wm.addView(wrapper, params)
        } catch (e: Exception) {
            Log.e("AppWindowOverlay", "Failed to attach taskbar overlay", e)
            taskbarRootView = null
            taskbarCenterRow = null
        }
        updateClockLabels()
    }

    private fun renderTaskbarSnapshot(snapshot: TaskbarOverlaySnapshot) {
        taskbarWidgetsText?.text = snapshot.weatherTempLabel ?: "Widgets"
        val wifiLabel = if (snapshot.wifiConnected) "Wi-Fi" else "Offline"
        val chargeMark = if (snapshot.isCharging) "⚡" else ""
        taskbarTrayStatusText?.text = "$wifiLabel • ${snapshot.batteryPercent}%$chargeMark"

        val container = taskbarCenterRow ?: return
        container.removeAllViews()

        // 1. Start Menu button (4-tile Fluent logo)
        container.addView(
            createTaskbarIconSlot(
                contentDesc = getString(R.string.cd_start_button),
                isActive = false,
                isOpen = false,
                iconView = FluentLogoView(this),
                onClick = { openDesktopOverlay(OVERLAY_TARGET_START) }
            )
        )

        // 2. Search button
        container.addView(
            createTaskbarSymbolSlot(
                symbol = "⌕",
                contentDesc = getString(R.string.cd_search_button),
                textColor = COLOR_TEXT_PRIMARY,
                onClick = { openDesktopOverlay(OVERLAY_TARGET_SEARCH) }
            )
        )

        // 3. Copilot IA button
        container.addView(
            createTaskbarIconSlot(
                contentDesc = "Abrir Copilot IA",
                isActive = false,
                isOpen = false,
                iconView = CopilotBadgeView(this),
                onClick = { openDesktopOverlay(OVERLAY_TARGET_COPILOT) }
            )
        )

        if (snapshot.entries.isNotEmpty()) {
            val sep = View(this).apply {
                setBackgroundColor(COLOR_BORDER_SUBTLE)
                layoutParams = LinearLayout.LayoutParams(dp(1), dp(22)).apply {
                    marginStart = dp(6)
                    marginEnd = dp(6)
                }
            }
            container.addView(sep)
        }

        snapshot.entries.forEach { entry ->
            val activeNow = entry.packageName != null && entry.packageName == currentPackageName
            val slotView = if (entry.packageName != null) {
                val appIcon = loadAppDrawable(entry.packageName)
                if (appIcon != null) {
                    val iv = ImageView(this).apply {
                        setImageDrawable(appIcon)
                        scaleType = ImageView.ScaleType.FIT_CENTER
                    }
                    createTaskbarIconSlot(
                        contentDesc = entry.label,
                        isActive = activeNow,
                        isOpen = entry.isOpen || activeNow,
                        iconView = iv,
                        onClick = { onTaskbarEntryClicked(entry) }
                    )
                } else {
                    createTaskbarSymbolSlot(
                        symbol = entry.label.take(2).uppercase(),
                        contentDesc = entry.label,
                        textColor = COLOR_ACCENT_CYAN,
                        isActive = activeNow,
                        isOpen = entry.isOpen || activeNow,
                        onClick = { onTaskbarEntryClicked(entry) }
                    )
                }
            } else {
                val badge = when (entry.kind) {
                    DesktopWindowKind.INTERNAL_FOLDER -> "📁"
                    DesktopWindowKind.INTERNAL_SETTINGS -> "⚙"
                    DesktopWindowKind.INTERNAL_NOTES -> "📝"
                    DesktopWindowKind.EXTERNAL_APP -> "▢"
                }
                createTaskbarSymbolSlot(
                    symbol = badge,
                    contentDesc = entry.label,
                    textColor = COLOR_ACCENT_CYAN,
                    isActive = false,
                    isOpen = true,
                    onClick = { onTaskbarEntryClicked(entry) }
                )
            }
            container.addView(slotView)
        }
    }

    private fun onTaskbarEntryClicked(entry: TaskbarOverlayEntry) {
        val pkg = entry.packageName
        if (entry.kind == DesktopWindowKind.EXTERNAL_APP && pkg != null) {
            if (pkg == currentPackageName) {
                // Clicking currently active app minimizes back to desktop
                onMinimizeClicked()
            } else {
                // Switch directly to the tapped app while keeping the taskbar in place
                currentPackageName = pkg
                currentAppLabel = entry.label
                currentSizeMode = entry.sizeMode
                refreshOverlayLabels()
                ExternalWindowOverlayBus.setSession(
                    ExternalAppSession(
                        packageName = pkg,
                        appLabel = entry.label,
                        sizeMode = entry.sizeMode,
                        forceLandscape = forceLandscape
                    )
                )
                ExternalWindowOverlayBus.emitAction(
                    ExternalWindowAction.SwitchToApp(pkg, entry.label, entry.sizeMode)
                )
                SystemBridge(this).launchApp(
                    packageName = pkg,
                    forceLandscape = forceLandscape,
                    useFreeformBounds = false,
                    windowSizeMode = entry.sizeMode,
                    reserveBottomTaskbarPx = if (keepTaskbarOverApps) dp(56) else 0
                )
                renderTaskbarSnapshot(ExternalWindowOverlayBus.taskbarSnapshot.value)
            }
        } else {
            val intent = Intent(this, MainActivity::class.java).apply {
                action = ACTION_FOCUS_INTERNAL_WINDOW
                putExtra(EXTRA_WINDOW_ID, entry.id)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            runCatching { startActivity(intent) }
            stopSelf()
        }
    }

    private fun openDesktopOverlay(target: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_OPEN_DESKTOP_OVERLAY
            putExtra(EXTRA_OVERLAY_TARGET, target)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        runCatching { startActivity(intent) }
        stopSelf()
    }

    private fun createTaskbarIconSlot(
        contentDesc: String,
        isActive: Boolean,
        isOpen: Boolean,
        iconView: View,
        onClick: () -> Unit
    ): View {
        val box = FrameLayout(this).apply {
            contentDescription = contentDesc
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44)).apply {
                marginStart = dp(2)
                marginEnd = dp(2)
            }
            background = GradientDrawable().apply {
                setColor(if (isActive) Color.parseColor("#28FFFFFF") else Color.TRANSPARENT)
                cornerRadius = dp(8).toFloat()
            }
            setOnClickListener { onClick() }
        }
        val iconLp = FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER)
        box.addView(iconView, iconLp)

        if (isOpen || isActive) {
            val pill = View(this).apply {
                background = GradientDrawable().apply {
                    setColor(COLOR_ACCENT_CYAN_INT)
                    cornerRadius = dp(2).toFloat()
                }
            }
            val pillLp = FrameLayout.LayoutParams(
                if (isActive) dp(18) else dp(10),
                dp(3),
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            ).apply {
                bottomMargin = dp(2)
            }
            box.addView(pill, pillLp)
        }
        return box
    }

    private fun createTaskbarSymbolSlot(
        symbol: String,
        contentDesc: String,
        textColor: Int,
        isActive: Boolean = false,
        isOpen: Boolean = false,
        onClick: () -> Unit
    ): View {
        val tv = TextView(this).apply {
            text = symbol
            setTextColor(textColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        return createTaskbarIconSlot(
            contentDesc = contentDesc,
            isActive = isActive,
            isOpen = isOpen,
            iconView = tv,
            onClick = onClick
        )
    }

    private fun loadAppDrawable(packageName: String): Drawable? {
        iconCache.get(packageName)?.let { return it }
        return runCatching {
            packageManager.getApplicationIcon(packageName).also { iconCache.put(packageName, it) }
        }.getOrNull()
    }

    private fun updateClockLabels() {
        val now = Date()
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        taskbarTimeText?.text = timeFmt.format(now)
        taskbarDateText?.text = dateFmt.format(now)
    }

    private fun computeNavigationBarBottomInset(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val wm = windowManager
            val insets = wm?.currentWindowMetrics?.windowInsets
            val nav = insets?.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars())
            if (nav != null && nav.bottom > 0) {
                return nav.bottom
            }
        }
        val resId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resId > 0) resources.getDimensionPixelSize(resId) else 0
    }

    private fun createCaptionButton(
        symbol: String,
        contentDesc: String,
        bgColor: Int,
        textColor: Int,
        onClick: () -> Unit
    ): TextView {
        return TextView(this).apply {
            text = symbol
            contentDescription = contentDesc
            setTextColor(textColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(40))
            background = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = dp(7).toFloat()
            }
            setOnClickListener { onClick() }
        }
    }

    private fun spacer(widthPx: Int): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(widthPx, 1)
        }
    }

    private fun refreshOverlayLabels() {
        titleLabelView?.text = currentAppLabel
        modeChipView?.text = shortModeLabel(currentSizeMode)
        snapButtons.forEach { (mode, chip) ->
            val selected = mode == currentSizeMode
            chip.setTextColor(if (selected) COLOR_NAVY_BG else COLOR_TEXT_PRIMARY)
            chip.background = GradientDrawable().apply {
                setColor(if (selected) COLOR_ACCENT_CYAN_INT else COLOR_CARD_SURFACE)
                cornerRadius = dp(6).toFloat()
            }
        }
    }

    private fun cycleWindowSize() {
        applyWindowSize(currentSizeMode.nextMode())
    }

    private fun applyWindowSize(nextMode: WindowSizeMode) {
        currentSizeMode = nextMode
        refreshOverlayLabels()
        ExternalWindowOverlayBus.updateSessionMode(currentPackageName, nextMode)
        ExternalWindowOverlayBus.emitAction(
            ExternalWindowAction.Resize(currentPackageName, nextMode)
        )
        SystemBridge(this).launchApp(
            packageName = currentPackageName,
            forceLandscape = forceLandscape,
            useFreeformBounds = true,
            windowSizeMode = nextMode,
            reserveBottomTaskbarPx = if (keepTaskbarOverApps) dp(56) else 0
        )
    }

    private fun onMinimizeClicked() {
        val pkg = currentPackageName
        ExternalWindowOverlayBus.emitAction(ExternalWindowAction.Minimize(pkg))
        bringLauncherToFront(ACTION_MINIMIZE_EXTERNAL_APP, pkg)
        stopSelf()
    }

    private fun onCloseClicked() {
        val pkg = currentPackageName
        ExternalWindowOverlayBus.emitAction(ExternalWindowAction.Close(pkg))
        bringLauncherToFront(ACTION_CLOSE_EXTERNAL_APP, pkg)
        stopSelf()
    }

    private fun bringLauncherToFront(action: String, packageName: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            this.action = action
            putExtra(EXTRA_PACKAGE_NAME, packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        runCatching { startActivity(intent) }
    }

    private fun removeCaptionOverlayView() {
        val view = captionRootView
        if (view != null) {
            runCatching { windowManager?.removeViewImmediate(view) }
            captionRootView = null
        }
    }

    private fun removeTaskbarOverlayView() {
        val view = taskbarRootView
        if (view != null) {
            runCatching { windowManager?.removeViewImmediate(view) }
            taskbarRootView = null
            taskbarCenterRow = null
        }
    }

    private fun overlayWindowType(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
        @Suppress("DEPRECATION")
        WindowManager.LayoutParams.TYPE_PHONE
    }

    private fun shortModeLabel(mode: WindowSizeMode): String = when (mode) {
        WindowSizeMode.COMPACT -> "Compacta"
        WindowSizeMode.SPLIT_LEFT -> "Esq."
        WindowSizeMode.SPLIT_RIGHT -> "Dir."
        WindowSizeMode.MAXIMIZED -> "Max."
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).roundToInt()

    private class FluentLogoView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val rect = RectF()
        private val colors = intArrayOf(
            Color.parseColor("#FF7BE0FF"),
            Color.parseColor("#FF38BDF8"),
            Color.parseColor("#FF38BDF8"),
            Color.parseColor("#FF2563EB")
        )

        override fun onDraw(canvas: Canvas) {
            val pad = width * 0.10f
            val gap = width * 0.08f
            val tile = (width - pad * 2f - gap) / 2f
            val r = width * 0.09f
            val coords = arrayOf(
                pad to pad,
                (pad + tile + gap) to pad,
                pad to (pad + tile + gap),
                (pad + tile + gap) to (pad + tile + gap)
            )
            coords.forEachIndexed { idx, (x, y) ->
                paint.color = colors[idx]
                rect.set(x, y, x + tile, y + tile)
                canvas.drawRoundRect(rect, r, r, paint)
            }
        }
    }

    private class CopilotBadgeView(context: Context) : View(context) {
        private val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#FF60CDFF")
        }
        private val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#FF2563EB")
        }

        override fun onDraw(canvas: Canvas) {
            val cx = width / 2f
            val cy = height / 2f
            val radius = (minOf(width, height) / 2f) * 0.72f
            outerPaint.strokeWidth = width * 0.14f
            canvas.drawCircle(cx, cy, radius, outerPaint)
            canvas.drawCircle(cx, cy, radius * 0.44f, innerPaint)
        }
    }

    companion object {
        const val ACTION_MINIMIZE_EXTERNAL_APP = "com.example.action.MINIMIZE_EXTERNAL_APP"
        const val ACTION_CLOSE_EXTERNAL_APP = "com.example.action.CLOSE_EXTERNAL_APP"
        const val ACTION_OPEN_DESKTOP_OVERLAY = "com.example.action.OPEN_DESKTOP_OVERLAY"
        const val ACTION_FOCUS_INTERNAL_WINDOW = "com.example.action.FOCUS_INTERNAL_WINDOW"

        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_LABEL = "extra_app_label"
        const val EXTRA_WINDOW_SIZE_MODE = "extra_window_size_mode"
        const val EXTRA_FORCE_LANDSCAPE = "extra_force_landscape"
        const val EXTRA_KEEP_TASKBAR = "extra_keep_taskbar"
        const val EXTRA_OVERLAY_TARGET = "extra_overlay_target"
        const val EXTRA_WINDOW_ID = "extra_window_id"

        const val OVERLAY_TARGET_START = "start"
        const val OVERLAY_TARGET_SEARCH = "search"
        const val OVERLAY_TARGET_WIDGETS = "widgets"
        const val OVERLAY_TARGET_ACTION_CENTER = "action_center"
        const val OVERLAY_TARGET_COPILOT = "copilot"

        private const val CHANNEL_ID = "fluent11_window_overlay"
        private const val NOTIFICATION_ID = 1104

        private val COLOR_MICA_BG = Color.parseColor("#EB1C2029")
        private val COLOR_TASKBAR_BG = Color.parseColor("#EE141822")
        private val COLOR_CARD_SURFACE = Color.parseColor("#FF282E3B")
        private val COLOR_NAVY_BG = Color.parseColor("#FF0B1120")
        private val COLOR_ACCENT_CYAN = Color.parseColor("#FF60CDFF")
        private val COLOR_ACCENT_CYAN_INT = Color.parseColor("#FF60CDFF")
        private val COLOR_BORDER_HIGHLIGHT = Color.parseColor("#5560CDFF")
        private val COLOR_BORDER_SUBTLE = Color.parseColor("#24FFFFFF")
        private val COLOR_TEXT_PRIMARY = Color.parseColor("#FFF3F6FC")
        private val COLOR_TEXT_SECONDARY = Color.parseColor("#FFA6B1C4")
        private val COLOR_CLOSE_SUBTLE = Color.parseColor("#38E81123")
        private val COLOR_TRANSPARENT_BTN = Color.parseColor("#14FFFFFF")

        fun startOverlay(
            context: Context,
            packageName: String,
            appLabel: String,
            sizeMode: WindowSizeMode,
            forceLandscape: Boolean,
            keepTaskbarOverApps: Boolean
        ) {
            if (!Settings.canDrawOverlays(context)) return
            val intent = Intent(context, AppWindowOverlayService::class.java).apply {
                putExtra(EXTRA_PACKAGE_NAME, packageName)
                putExtra(EXTRA_APP_LABEL, appLabel)
                putExtra(EXTRA_WINDOW_SIZE_MODE, sizeMode.key)
                putExtra(EXTRA_FORCE_LANDSCAPE, forceLandscape)
                putExtra(EXTRA_KEEP_TASKBAR, keepTaskbarOverApps)
            }
            runCatching {
                ContextCompat.startForegroundService(context, intent)
            }
        }

        fun stopOverlay(context: Context) {
            runCatching {
                context.stopService(Intent(context, AppWindowOverlayService::class.java))
            }
        }
    }
}
