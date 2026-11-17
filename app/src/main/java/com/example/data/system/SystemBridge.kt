package com.example.data.system

import android.app.Activity
import android.app.ActivityManager
import android.app.ActivityOptions
import android.app.PendingIntent
import android.app.role.RoleManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.display.DisplayManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.util.LruCache
import android.view.Display
import android.view.Surface
import android.view.WindowInsets
import android.view.WindowManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.R
import com.example.data.model.InstalledApp
import com.example.data.model.SystemTelemetry
import com.example.data.model.WindowSizeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SystemBridge(private val context: Context) {

    private val iconCache = LruCache<String, ImageBitmap>(180)
    private var torchCameraId: String? = null
    private var lastTorchState: Boolean = false

    init {
        discoverTorchCamera()
    }

    private fun discoverTorchCamera() {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
            for (id in cameraManager.cameraIdList) {
                val chars = cameraManager.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val lensFacing = chars.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    torchCameraId = id
                    break
                }
            }
            if (torchCameraId == null) {
                torchCameraId = cameraManager.cameraIdList.firstOrNull { id ->
                    cameraManager.getCameraCharacteristics(id)
                        .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                }
            }
        } catch (_: Exception) {
            torchCameraId = null
        }
    }

    suspend fun loadInstalledApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(mainIntent, 0)
        }

        val selfPackage = context.packageName
        resolveInfos
            .asSequence()
            .filter { it.activityInfo?.packageName != null && it.activityInfo.packageName != selfPackage }
            .distinctBy { it.activityInfo.packageName }
            .map { info ->
                val pkg = info.activityInfo.packageName
                val activity = info.activityInfo.name
                val label = info.loadLabel(pm)?.toString()?.trim().takeUnless { it.isNullOrEmpty() } ?: pkg
                val cached = iconCache.get(pkg) ?: runCatching {
                    val drawable = info.loadIcon(pm)
                    drawableToImageBitmap(drawable).also { iconCache.put(pkg, it) }
                }.getOrNull()

                InstalledApp(
                    packageName = pkg,
                    activityName = activity,
                    label = label,
                    icon = cached
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    private fun drawableToImageBitmap(drawable: Drawable): ImageBitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap.asImageBitmap()
        }
        val width = drawable.intrinsicWidth.coerceIn(72, 160)
        val height = drawable.intrinsicHeight.coerceIn(72, 160)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap.asImageBitmap()
    }

    fun canWriteSystemSettings(): Boolean = Settings.System.canWrite(context)

    fun canDrawOverlays(): Boolean = Settings.canDrawOverlays(context)

    fun isDefaultLauncher(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                return roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            }
        }
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.resolveActivity(
                homeIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        return resolved?.activityInfo?.packageName == context.packageName
    }

    fun createDefaultLauncherPromptIntent(): Intent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null &&
                roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            ) {
                return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
            }
        }
        return null
    }

    fun openWriteSettingsPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        startSettingsIntent(intent, Settings.ACTION_SETTINGS)
    }

    fun openOverlayPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        startSettingsIntent(intent, Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    }

    fun openDeveloperSettings() {
        startSettingsIntent(
            Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
            Settings.ACTION_SETTINGS
        )
    }

    fun openOwnAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        )
        startSettingsIntent(intent, Settings.ACTION_SETTINGS)
    }

    fun hasFreeformFeature(): Boolean = runCatching {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT)
    }.getOrDefault(false)

    fun isFreeformSystemEnabled(): Boolean = runCatching {
        Settings.Global.getInt(context.contentResolver, "enable_freeform_support", 0) == 1
    }.getOrDefault(false)

    fun isForceResizableEnabled(): Boolean = runCatching {
        Settings.Global.getInt(context.contentResolver, "force_resizable_activities", 0) == 1
    }.getOrDefault(false)

    fun isSystemFreeformReady(): Boolean = hasFreeformFeature() || isFreeformSystemEnabled()

    fun enforceLandscapeRotation(): Boolean {
        if (!canWriteSystemSettings()) return false
        return try {
            val currentRotation = getCurrentDisplayRotation()
            val targetRotation = if (currentRotation == Surface.ROTATION_270) {
                Surface.ROTATION_270
            } else {
                Surface.ROTATION_90
            }
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                0
            )
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.USER_ROTATION,
                targetRotation
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun getCurrentDisplayRotation(): Int {
        return runCatching {
            val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            val display = dm?.getDisplay(Display.DEFAULT_DISPLAY)
            if (display != null) {
                display.rotation
            } else {
                @Suppress("DEPRECATION")
                (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay?.rotation
                    ?: Surface.ROTATION_90
            }
        }.getOrDefault(Surface.ROTATION_90)
    }

    fun launchApp(
        packageName: String,
        forceLandscape: Boolean = true,
        useFreeformBounds: Boolean = true,
        windowSizeMode: WindowSizeMode = WindowSizeMode.MAXIMIZED,
        reserveBottomTaskbarPx: Int = 0,
        reserveTopBarPx: Int = 0,
        anchorTaskbarAtBottomEdge: Boolean = false,
        isExplicitResize: Boolean = false
    ): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            if (windowSizeMode == WindowSizeMode.SPLIT_LEFT || windowSizeMode == WindowSizeMode.SPLIT_RIGHT) {
                intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
            }
            if (isExplicitResize) {
                intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            }

            if (forceLandscape) {
                enforceLandscapeRotation()
            }

            val shouldApplyBounds = useFreeformBounds || windowSizeMode != WindowSizeMode.MAXIMIZED
            val options = if (shouldApplyBounds) {
                // Use makeBasic() when requesting freeform bounds so OEMs don't override freeform animation/windowing
                ActivityOptions.makeBasic()
            } else {
                runCatching {
                    ActivityOptions.makeCustomAnimation(
                        context,
                        R.anim.win11_app_enter,
                        R.anim.win11_app_exit
                    )
                }.getOrElse { ActivityOptions.makeBasic() }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                runCatching {
                    options.setPendingIntentBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                    )
                }
            }

            if (shouldApplyBounds) {
                val (screenW, screenH, topInsetPx, navBottomPx) = readScreenFrameMetrics()
                val density = context.resources.displayMetrics.density
                val topBound = maxOf(reserveTopBarPx, topInsetPx)
                val navAllowance = if (anchorTaskbarAtBottomEdge) 0 else navBottomPx
                val bottomReserved = reserveBottomTaskbarPx + navAllowance
                val usableBottom = (screenH - bottomReserved).coerceAtLeast(topBound + 280)
                val usableHeight = (usableBottom - topBound).coerceAtLeast(280)
                val edgeInset = (2f * density).toInt().coerceAtLeast(2)
                val maxW = (screenW - edgeInset * 2).coerceAtLeast(480)

                val rect = when (windowSizeMode) {
                    WindowSizeMode.COMPACT -> {
                        val w = (screenW * 0.68f).toInt().coerceIn(480, maxW)
                        val h = (usableHeight * 0.80f).toInt().coerceIn(300, usableHeight)
                        val l = ((screenW - w) / 2).coerceAtLeast(edgeInset)
                        val t = (topBound + (usableHeight - h) / 2).coerceAtLeast(topBound)
                        Rect(l, t, l + w, t + h)
                    }
                    WindowSizeMode.SPLIT_LEFT -> {
                        val halfW = (screenW / 2 - edgeInset).coerceAtLeast(360)
                        Rect(edgeInset, topBound, edgeInset + halfW, usableBottom)
                    }
                    WindowSizeMode.SPLIT_RIGHT -> {
                        val halfW = (screenW / 2 - edgeInset).coerceAtLeast(360)
                        Rect(screenW - halfW - edgeInset, topBound, screenW - edgeInset, usableBottom)
                    }
                    WindowSizeMode.MAXIMIZED -> {
                        Rect(edgeInset, topBound, screenW - edgeInset, usableBottom)
                    }
                }
                options.setLaunchBounds(rect)
            }

            val bundle = options.toBundle()
            if (context !is Activity && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingResult = runCatching {
                    PendingIntent.getActivity(
                        context,
                        packageName.hashCode(),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    ).send(context, 0, null, null, null, null, bundle)
                }
                if (pendingResult.isFailure) {
                    context.startActivity(intent, bundle)
                }
            } else {
                context.startActivity(intent, bundle)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private data class ScreenFrameMetrics(
        val widthPx: Int,
        val heightPx: Int,
        val topInsetPx: Int,
        val navBottomPx: Int
    )

    private fun readScreenFrameMetrics(): ScreenFrameMetrics {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && wm != null) {
            val metrics = wm.currentWindowMetrics
            val bounds = metrics.bounds
            val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
            )
            val rawW = bounds.width().coerceAtLeast(1)
            val rawH = bounds.height().coerceAtLeast(1)
            return ScreenFrameMetrics(
                widthPx = maxOf(rawW, rawH),
                heightPx = minOf(rawW, rawH),
                topInsetPx = insets.top,
                navBottomPx = insets.bottom
            )
        }
        val dm = context.resources.displayMetrics
        val longSide = maxOf(dm.widthPixels, dm.heightPixels)
        val shortSide = minOf(dm.widthPixels, dm.heightPixels)
        return ScreenFrameMetrics(
            widthPx = longSide,
            heightPx = shortSide,
            topInsetPx = 0,
            navBottomPx = 0
        )
    }

    fun openAppInfo(packageName: String) {
        runCatching {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun uninstallApp(packageName: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun openWifiPanel() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_WIFI)
        } else {
            Intent(Settings.ACTION_WIFI_SETTINGS)
        }
        startSettingsIntent(intent, fallbackAction = Settings.ACTION_WIFI_SETTINGS)
    }

    fun openBluetoothSettings() {
        startSettingsIntent(Intent(Settings.ACTION_BLUETOOTH_SETTINGS), Settings.ACTION_SETTINGS)
    }

    fun openNetworkSettings() {
        startSettingsIntent(Intent(Settings.ACTION_WIRELESS_SETTINGS), Settings.ACTION_SETTINGS)
    }

    fun openDisplaySettings() {
        startSettingsIntent(Intent(Settings.ACTION_DISPLAY_SETTINGS), Settings.ACTION_SETTINGS)
    }

    fun openSystemSettings() {
        startSettingsIntent(Intent(Settings.ACTION_SETTINGS), Settings.ACTION_SETTINGS)
    }

    fun openDefaultHomeSettings() {
        startSettingsIntent(Intent(Settings.ACTION_HOME_SETTINGS), Settings.ACTION_SETTINGS)
    }

    fun openSystemNavigationSettings() {
        val candidates = listOf(
            Intent("com.android.settings.GESTURE_NAVIGATION_SETTINGS"),
            Intent("android.settings.SYSTEM_NAVIGATION_SETTINGS"),
            Intent(Settings.ACTION_DISPLAY_SETTINGS),
            Intent(Settings.ACTION_SETTINGS)
        )
        for (candidate in candidates) {
            candidate.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val resolved = candidate.resolveActivity(context.packageManager)
            if (resolved != null) {
                val started = runCatching { context.startActivity(candidate) }.isSuccess
                if (started) return
            }
        }
    }

    private fun startSettingsIntent(primary: Intent, fallbackAction: String) {
        try {
            primary.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(primary)
        } catch (_: Exception) {
            runCatching {
                val fallback = Intent(fallbackAction).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            }
        }
    }

    fun setMediaVolume(volume: Int) {
        runCatching {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume.coerceIn(0, max), 0)
        }
    }

    fun toggleTorch(): Boolean? {
        val camId = torchCameraId ?: return null
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return null
            val next = !lastTorchState
            cameraManager.setTorchMode(camId, next)
            lastTorchState = next
            next
        } catch (_: Exception) {
            null
        }
    }

    fun readTelemetry(windowBrightness: Float): SystemTelemetry {
        val batteryIntent = runCatching {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull()
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPercent = if (level >= 0 && scale > 0) (level * 100) / scale else 100
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val capabilities = cm?.getNetworkCapabilities(cm.activeNetwork)
        val wifiConnected = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothEnabled = try {
            btManager?.adapter?.isEnabled == true
        } catch (_: SecurityException) {
            false
        }

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val mediaVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 8
        val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.coerceAtLeast(1) ?: 15

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memInfo)
        val totalRamGb = memInfo.totalMem / (1024f * 1024f * 1024f)
        val availRamGb = memInfo.availMem / (1024f * 1024f * 1024f)
        val usedRamGb = (totalRamGb - availRamGb).coerceAtLeast(0f)

        val statFs = StatFs(Environment.getDataDirectory().path)
        val totalStorageGb = (statFs.blockCountLong * statFs.blockSizeLong) / (1024f * 1024f * 1024f)
        val availStorageGb = (statFs.availableBlocksLong * statFs.blockSizeLong) / (1024f * 1024f * 1024f)
        val usedStorageGb = (totalStorageGb - availStorageGb).coerceAtLeast(0f)

        val canWriteSettings = canWriteSystemSettings()
        val isLandscapeLocked = runCatching {
            val autoRotate = Settings.System.getInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                1
            )
            val userRot = Settings.System.getInt(
                context.contentResolver,
                Settings.System.USER_ROTATION,
                Surface.ROTATION_0
            )
            autoRotate == 0 && (userRot == Surface.ROTATION_90 || userRot == Surface.ROTATION_270)
        }.getOrDefault(false)

        return SystemTelemetry(
            batteryPercent = batteryPercent.coerceIn(0, 100),
            isCharging = isCharging,
            wifiConnected = wifiConnected,
            bluetoothEnabled = bluetoothEnabled,
            isTorchOn = lastTorchState,
            torchAvailable = torchCameraId != null,
            mediaVolume = mediaVol,
            maxMediaVolume = maxVol,
            windowBrightness = windowBrightness,
            ramUsedGb = usedRamGb,
            ramTotalGb = totalRamGb,
            storageUsedGb = usedStorageGb,
            storageTotalGb = totalStorageGb,
            canWriteSystemSettings = canWriteSettings,
            isRotationLockedLandscape = isLandscapeLocked,
            isDefaultLauncher = isDefaultLauncher(),
            canDrawOverlays = canDrawOverlays(),
            hasFreeformFeature = hasFreeformFeature(),
            freeformSystemEnabled = isFreeformSystemEnabled(),
            forceResizableEnabled = isForceResizableEnabled()
        )
    }
}
