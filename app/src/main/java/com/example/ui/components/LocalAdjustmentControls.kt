package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FilterType
import kotlin.math.roundToInt

@Composable
fun LocalAdjustmentControls(
    brightness: Float,
    contrast: Float,
    saturation: Float,
    selectedFilter: FilterType,
    onBrightnessChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onFilterChange: (FilterType) -> Unit,
    onRotate: () -> Unit,
    onFlipHorizontal: () -> Unit,
    onResetLocal: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Quick Filters Carousel
        Text(
            text = "Filter Warna Instan",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(FilterType.entries.toTypedArray()) { filter ->
                val isSelected = filter == selectedFilter
                ElevatedFilterChip(
                    selected = isSelected,
                    onClick = { onFilterChange(filter) },
                    label = { Text(filter.displayName, fontSize = 12.sp) },
                    colors = FilterChipDefaults.elevatedFilterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("filter_chip_${filter.name}")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tool Sliders
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AdjustmentSliderRow(
                icon = Icons.Default.Brightness6,
                label = "Kecerahan",
                valueDisplay = "${brightness.roundToInt()}%",
                value = brightness,
                valueRange = -80f..80f,
                onValueChange = onBrightnessChange,
                testTag = "slider_brightness"
            )

            AdjustmentSliderRow(
                icon = Icons.Default.Contrast,
                label = "Kontras",
                valueDisplay = String.format("%.2fx", contrast),
                value = contrast,
                valueRange = 0.5f..1.8f,
                onValueChange = onContrastChange,
                testTag = "slider_contrast"
            )

            AdjustmentSliderRow(
                icon = Icons.Default.InvertColors,
                label = "Saturasi",
                valueDisplay = String.format("%.2fx", saturation),
                value = saturation,
                valueRange = 0.0f..2.0f,
                onValueChange = onSaturationChange,
                testTag = "slider_saturation"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Transform and reset buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = onRotate,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("btn_rotate")
            ) {
                Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Putar 90°")
            }

            FilledTonalIconButton(
                onClick = onFlipHorizontal,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("btn_flip")
            ) {
                Icon(Icons.Default.Flip, contentDescription = "Balik Horizontal")
            }

            FilledTonalIconButton(
                onClick = onResetLocal,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("btn_reset_local")
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = "Reset Nilai Slider")
            }
        }
    }
}

@Composable
fun AdjustmentSliderRow(
    icon: ImageVector,
    label: String,
    valueDisplay: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    testTag: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(75.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .weight(1f)
                .testTag(testTag)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = valueDisplay,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(42.dp)
        )
    }
}
