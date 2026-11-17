package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.ScreenLockLandscape
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.SystemTelemetry
import com.example.ui.theme.Win11AccentCyan
import com.example.ui.theme.Win11BorderSubtle
import com.example.ui.theme.Win11CardSurface
import com.example.ui.theme.Win11MicaSurface
import com.example.ui.theme.Win11NavyBg
import com.example.ui.theme.Win11TextPrimary
import com.example.ui.theme.Win11TextSecondary
import kotlin.math.roundToInt

@Composable
fun ActionCenterFlyout(
    telemetry: SystemTelemetry,
    forceLandscapeApps: Boolean,
    onOpenWifi: () -> Unit,
    onOpenBluetooth: () -> Unit,
    onToggleTorch: () -> Unit,
    onToggleMute: () -> Unit,
    onOpenNetwork: () -> Unit,
    onOpenDefaultHome: () -> Unit,
    onToggleLandscapeLock: () -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onVolumeChange: (Int) -> Unit,
    onOpenSystemSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Win11MicaSurface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 8.dp,
        shadowElevation = 20.dp,
        modifier = modifier
            .width(340.dp)
            .border(1.dp, Win11BorderSubtle, RoundedCornerShape(16.dp))
            .testTag("action_center_flyout")
    ) {
        Column {
            Column(modifier = Modifier.padding(16.dp)) {
                // Row 1 of Quick Tiles
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    QuickActionTile(
                        icon = if (telemetry.wifiConnected) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
                        label = "Wi-Fi",
                        active = telemetry.wifiConnected,
                        onClick = onOpenWifi,
                        testTag = "action_tile_wifi",
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        icon = if (telemetry.bluetoothEnabled) Icons.Rounded.Bluetooth else Icons.Rounded.BluetoothDisabled,
                        label = "Bluetooth",
                        active = telemetry.bluetoothEnabled,
                        onClick = onOpenBluetooth,
                        testTag = "action_tile_bluetooth",
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        icon = if (telemetry.isTorchOn) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
                        label = "Lanterna",
                        active = telemetry.isTorchOn,
                        onClick = onToggleTorch,
                        testTag = "action_tile_torch",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Row 2 of Quick Tiles
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    QuickActionTile(
                        icon = if (telemetry.mediaVolume == 0) Icons.AutoMirrored.Rounded.VolumeOff
                        else Icons.AutoMirrored.Rounded.VolumeUp,
                        label = if (telemetry.mediaVolume == 0) "Mudo" else "Som",
                        active = telemetry.mediaVolume > 0,
                        onClick = onToggleMute,
                        testTag = "action_tile_sound",
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        icon = Icons.Rounded.AirplanemodeActive,
                        label = "Redes",
                        active = false,
                        onClick = onOpenNetwork,
                        testTag = "action_tile_network",
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        icon = Icons.Rounded.Home,
                        label = "Launcher",
                        active = true,
                        onClick = onOpenDefaultHome,
                        testTag = "action_tile_home",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Landscape app lock quick bar
                val landscapeReady = forceLandscapeApps && telemetry.canWriteSystemSettings
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (landscapeReady) Win11AccentCyan.copy(alpha = 0.16f) else Win11CardSurface)
                        .border(
                            1.dp,
                            if (landscapeReady) Win11AccentCyan else Win11BorderSubtle,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable(onClick = onToggleLandscapeLock)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("action_tile_landscape_lock")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.ScreenLockLandscape,
                            contentDescription = "Manter apps na horizontal",
                            tint = if (landscapeReady) Win11AccentCyan else Win11TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Apps sempre na horizontal",
                            style = MaterialTheme.typography.labelMedium,
                            color = Win11TextPrimary
                        )
                    }
                    Text(
                        text = when {
                            !telemetry.canWriteSystemSettings -> "Permitir"
                            forceLandscapeApps -> "Ativo"
                            else -> "Desligado"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (landscapeReady) Win11AccentCyan else Win11TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Brightness Slider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.BrightnessHigh,
                        contentDescription = "Brilho",
                        tint = Win11TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Slider(
                        value = telemetry.windowBrightness,
                        onValueChange = onBrightnessChange,
                        valueRange = 0.1f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = Win11AccentCyan,
                            activeTrackColor = Win11AccentCyan,
                            inactiveTrackColor = Win11CardSurface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("brightness_slider")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${(telemetry.windowBrightness * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Win11TextSecondary
                    )
                }

                // Volume Slider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (telemetry.mediaVolume == 0) Icons.AutoMirrored.Rounded.VolumeOff
                        else Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = "Volume",
                        tint = Win11TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Slider(
                        value = telemetry.mediaVolume.toFloat(),
                        onValueChange = { onVolumeChange(it.roundToInt()) },
                        valueRange = 0f..telemetry.maxMediaVolume.toFloat().coerceAtLeast(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Win11AccentCyan,
                            activeTrackColor = Win11AccentCyan,
                            inactiveTrackColor = Win11CardSurface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("volume_slider")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val volPct = ((telemetry.mediaVolume.toFloat() / telemetry.maxMediaVolume.coerceAtLeast(1)) * 100).roundToInt()
                    Text(
                        text = "$volPct%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Win11TextSecondary
                    )
                }
            }

            HorizontalDivider(color = Win11BorderSubtle)

            // Footer: Battery & System Settings
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.22f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (telemetry.isCharging) Icons.Rounded.BatteryChargingFull
                        else Icons.Rounded.BatteryFull,
                        contentDescription = null,
                        tint = if (telemetry.isCharging) Win11AccentCyan else Win11TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${telemetry.batteryPercent}%${if (telemetry.isCharging) " • Carregando" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Win11TextPrimary
                    )
                }

                IconButton(
                    onClick = onOpenSystemSettings,
                    modifier = Modifier.testTag("action_center_settings_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Configurações do sistema",
                        tint = Win11TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val bgColor = if (active) Win11AccentCyan else Win11CardSurface
    val contentColor = if (active) Win11NavyBg else Win11TextPrimary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .height(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(bgColor)
                .border(
                    1.dp,
                    if (active) Win11AccentCyan else Win11BorderSubtle,
                    RoundedCornerShape(10.dp)
                )
                .clickable(onClick = onClick)
                .testTag(testTag)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = Win11TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
