package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CropSquare
import androidx.compose.material.icons.rounded.FilterNone
import androidx.compose.material.icons.rounded.HorizontalRule
import androidx.compose.material.icons.rounded.ViewSidebar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.WindowSizeMode
import kotlinx.coroutines.launch
import com.example.ui.theme.Win11AccentCyan
import com.example.ui.theme.Win11BorderHighlight
import com.example.ui.theme.Win11BorderSubtle
import com.example.ui.theme.Win11CardSurface
import com.example.ui.theme.Win11MicaSurface
import com.example.ui.theme.Win11TextPrimary
import com.example.ui.theme.Win11TextSecondary

@Composable
fun BoxScope.DesktopWindowFrame(
    windowId: String,
    title: String,
    icon: ImageVector,
    sizeMode: WindowSizeMode,
    isActive: Boolean,
    onFocus: () -> Unit,
    onMinimize: () -> Unit,
    onResizeCycle: () -> Unit,
    onResizeSelect: (WindowSizeMode) -> Unit,
    onClose: () -> Unit,
    extraHeaderActions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val targetWidth = when (sizeMode) {
            WindowSizeMode.COMPACT -> (maxWidth * 0.64f).coerceIn(320.dp, 600.dp)
            WindowSizeMode.SPLIT_LEFT,
            WindowSizeMode.SPLIT_RIGHT -> ((maxWidth - 18.dp) / 2f).coerceAtLeast(240.dp)
            WindowSizeMode.MAXIMIZED -> maxWidth
        }
        val targetHeight = when (sizeMode) {
            WindowSizeMode.COMPACT -> (maxHeight * 0.74f).coerceIn(240.dp, 520.dp)
            WindowSizeMode.SPLIT_LEFT,
            WindowSizeMode.SPLIT_RIGHT -> (maxHeight - 12.dp).coerceAtLeast(220.dp)
            WindowSizeMode.MAXIMIZED -> maxHeight
        }

        val animatedWidth by animateDpAsState(
            targetValue = targetWidth,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "windowWidth"
        )
        val animatedHeight by animateDpAsState(
            targetValue = targetHeight,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "windowHeight"
        )

        val alignment = when (sizeMode) {
            WindowSizeMode.COMPACT -> Alignment.Center
            WindowSizeMode.SPLIT_LEFT -> Alignment.CenterStart
            WindowSizeMode.SPLIT_RIGHT -> Alignment.CenterEnd
            WindowSizeMode.MAXIMIZED -> Alignment.Center
        }

        val outerPadding = when (sizeMode) {
            WindowSizeMode.MAXIMIZED -> 0.dp
            WindowSizeMode.SPLIT_LEFT, WindowSizeMode.SPLIT_RIGHT -> 6.dp
            WindowSizeMode.COMPACT -> 8.dp
        }
        val cornerRadius by animateDpAsState(
            targetValue = if (sizeMode == WindowSizeMode.MAXIMIZED) 0.dp else 14.dp,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "windowCorner"
        )
        val borderColor = if (isActive) Win11BorderHighlight else Win11BorderSubtle

        val entranceScale = remember(windowId) { Animatable(0.93f) }
        val entranceAlpha = remember(windowId) { Animatable(0f) }
        LaunchedEffect(windowId) {
            launch {
                entranceScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = 0.84f, stiffness = Spring.StiffnessMediumLow)
                )
            }
            launch {
                entranceAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing)
                )
            }
        }

        Surface(
            color = Win11MicaSurface,
            shape = RoundedCornerShape(cornerRadius),
            tonalElevation = if (isActive) 10.dp else 4.dp,
            shadowElevation = if (isActive) 24.dp else 10.dp,
            modifier = Modifier
                .align(alignment)
                .graphicsLayer {
                    scaleX = entranceScale.value
                    scaleY = entranceScale.value
                    alpha = entranceAlpha.value
                }
                .padding(horizontal = outerPadding, vertical = outerPadding)
                .width(animatedWidth)
                .height(animatedHeight)
                .border(1.dp, borderColor, RoundedCornerShape(cornerRadius))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onFocus
                )
                .testTag("desktop_window_$windowId")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                WindowCaptionBar(
                    windowId = windowId,
                    title = title,
                    icon = icon,
                    sizeMode = sizeMode,
                    onMinimize = onMinimize,
                    onResizeCycle = onResizeCycle,
                    onResizeSelect = onResizeSelect,
                    onClose = onClose,
                    extraHeaderActions = extraHeaderActions
                )
                HorizontalDivider(color = Win11BorderSubtle)
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    content()
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WindowCaptionBar(
    windowId: String,
    title: String,
    icon: ImageVector,
    sizeMode: WindowSizeMode,
    onMinimize: () -> Unit,
    onResizeCycle: () -> Unit,
    onResizeSelect: (WindowSizeMode) -> Unit,
    onClose: () -> Unit,
    extraHeaderActions: @Composable RowScope.() -> Unit = {}
) {
    var snapMenuOpen by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.28f))
            .padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)
    ) {
        // Left: Icon & Window Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Win11AccentCyan,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = Win11TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = sizeMode.label,
                style = MaterialTheme.typography.labelSmall,
                color = Win11TextSecondary.copy(alpha = 0.75f),
                maxLines = 1
            )
        }

        // Optional contextual actions before caption controls
        Row(verticalAlignment = Alignment.CenterVertically) {
            extraHeaderActions()

            // Top-Right Windows 11 Caption Controls: Minimize | Resize/Snap | Close
            CaptionControlButton(
                icon = Icons.Rounded.HorizontalRule,
                contentDescription = "Minimizar janela",
                onClick = onMinimize,
                testTag = "win_btn_minimize_$windowId"
            )

            Box {
                val resizeIcon = when (sizeMode) {
                    WindowSizeMode.MAXIMIZED -> Icons.Rounded.FilterNone
                    WindowSizeMode.SPLIT_LEFT, WindowSizeMode.SPLIT_RIGHT -> Icons.Rounded.ViewSidebar
                    WindowSizeMode.COMPACT -> Icons.Rounded.CropSquare
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(width = 44.dp, height = 40.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .combinedClickable(
                            onClick = onResizeCycle,
                            onLongClick = { snapMenuOpen = true }
                        )
                        .testTag("win_btn_resize_$windowId")
                ) {
                    Icon(
                        imageVector = resizeIcon,
                        contentDescription = "Redimensionar janela",
                        tint = Win11TextPrimary,
                        modifier = Modifier.size(17.dp)
                    )
                }

                DropdownMenu(
                    expanded = snapMenuOpen,
                    onDismissRequest = { snapMenuOpen = false },
                    containerColor = Win11CardSurface
                ) {
                    WindowSizeMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = mode.label,
                                    color = if (mode == sizeMode) Win11AccentCyan else Win11TextPrimary
                                )
                            },
                            onClick = {
                                snapMenuOpen = false
                                onResizeSelect(mode)
                            }
                        )
                    }
                }
            }

            CaptionControlButton(
                icon = Icons.Rounded.Close,
                contentDescription = "Fechar janela",
                isClose = true,
                onClick = onClose,
                testTag = "win_btn_close_$windowId"
            )
        }
    }
}

@Composable
fun CaptionControlButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    testTag: String,
    isClose: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "captionBtnScale"
    )
    val bgColor = when {
        isClose && pressed -> Color(0xFFE81123)
        isClose -> Color(0x22E81123)
        pressed -> Color.White.copy(alpha = 0.16f)
        else -> Color.Transparent
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .size(width = 44.dp, height = 40.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Win11TextPrimary,
            modifier = Modifier.size(18.dp)
        )
    }
}
