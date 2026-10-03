package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun BeforeAfterCanvas(
    currentBitmap: Bitmap?,
    originalBitmap: Bitmap?,
    isHoldingBefore: Boolean,
    isSplitView: Boolean,
    splitPosition: Float,
    onSplitPositionChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    if (currentBitmap == null) {
        Box(
            modifier = modifier
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .testTag("empty_canvas_box"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Pilih atau ambil foto untuk memulai edit",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F172A))
            .testTag("canvas_viewport"),
        contentAlignment = Alignment.Center
    ) {
        val containerWidth = maxWidth
        val containerHeight = maxHeight

        val activeBitmap = if (isHoldingBefore && originalBitmap != null) {
            originalBitmap
        } else {
            currentBitmap
        }

        if (!isSplitView || originalBitmap == null) {
            // Normal Single View (or hold-to-compare)
            Image(
                bitmap = activeBitmap.asImageBitmap(),
                contentDescription = if (isHoldingBefore) "Foto Asli" else "Foto Hasil Edit",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("single_photo_view")
            )
        } else {
            // Interactive Split View (Before / After)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val newPos = (change.position.x / size.width).coerceIn(0.05f, 0.95f)
                            onSplitPositionChange(newPos)
                        }
                    }
                    .testTag("split_photo_view")
            ) {
                // Background: After (Edited)
                Image(
                    bitmap = currentBitmap.asImageBitmap(),
                    contentDescription = "Hasil Edit",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                // Foreground clipped to left split: Before (Original)
                Image(
                    bitmap = originalBitmap.asImageBitmap(),
                    contentDescription = "Foto Asli",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            val clipWidth = size.width * splitPosition
                            clipRect(
                                left = 0f,
                                top = 0f,
                                right = clipWidth,
                                bottom = size.height,
                                clipOp = ClipOp.Intersect
                            ) {
                                this@drawWithContent.drawContent()
                            }
                        }
                )

                // Divider Line and Drag Handle
                val dividerX = containerWidth * splitPosition
                Box(
                    modifier = Modifier
                        .offset { IntOffset(dividerX.toPx().roundToInt() - 1.dp.toPx().roundToInt(), 0) }
                        .width(2.dp)
                        .background(Color.White)
                        .fillMaxSize()
                )

                // Drag Knob
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (dividerX.toPx() - 18.dp.toPx()).roundToInt(),
                                (containerHeight.toPx() / 2 - 18.dp.toPx()).roundToInt()
                            )
                        }
                        .size(36.dp)
                        .background(Color.White, CircleShape)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Geser Perbandingan",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Left "SEBELUM" Label
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "SEBELUM (ASLI)",
                        color = Color.White,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Right "SESUDAH" Label
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "SESUDAH (AI)",
                        color = Color.White,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Holding indicator badge
        AnimatedVisibility(
            visible = isHoldingBefore,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 4.dp
            ) {
                Text(
                    text = "Menampilkan Foto Asli",
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}
