package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.ui.theme.Win11AccentBlue
import com.example.ui.theme.Win11AccentCyan
import com.example.ui.theme.Win11NavyBg

@Composable
fun WallpaperBackground(
    onBackgroundTap: () -> Unit,
    onBackgroundLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onBackgroundTap() },
                    onLongPress = { onBackgroundLongPress() }
                )
            }
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_bloom_dark_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Subtle Mica depth vignette and ambient cyan bloom glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Win11NavyBg.copy(alpha = 0.25f),
                        Color.Transparent,
                        Win11NavyBg.copy(alpha = 0.52f)
                    )
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Win11AccentCyan.copy(alpha = 0.14f),
                        Win11AccentBlue.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.45f),
                    radius = size.maxDimension * 0.45f
                ),
                center = Offset(size.width * 0.5f, size.height * 0.45f),
                radius = size.maxDimension * 0.45f
            )
        }
    }
}
