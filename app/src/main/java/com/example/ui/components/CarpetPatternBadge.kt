package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CarpetPatternBadge(
    patternType: Int,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val baseColors = listOf(
        Color(0xFF880E4F), // Persian Ruby
        Color(0xFF1A237E), // Sapphire Blue
        Color(0xFF33691E), // Vintage Forest
        Color(0xFFE0E0E0), // Fluffy Light Grey
        Color(0xFF3E2723), // Nordic Charcoal
        Color(0xFFBF360C)  // Boho Terracotta
    )
    val accentColors = listOf(
        Color(0xFFFFD54F), // Gold
        Color(0xFF80D8FF), // Light Blue
        Color(0xFFAED581), // Light Green
        Color(0xFFFF8A80), // Soft Coral
        Color(0xFFFFF9C4), // Cream
        Color(0xFFFFE082)  // Amber Gold
    )

    val safeIndex = (patternType % baseColors.size).let { if (it < 0) 0 else it }
    val baseColor = baseColors[safeIndex]
    val accentColor = accentColors[safeIndex]

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(baseColor)
            .border(1.dp, Color.Black.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height

            // Obwódka wewnętrzna frędzli dywanu
            drawRect(
                color = accentColor.copy(alpha = 0.35f),
                topLeft = Offset(w * 0.1f, h * 0.1f),
                size = Size(w * 0.8f, h * 0.8f),
                style = Stroke(width = 2f)
            )

            when (safeIndex) {
                0 -> {
                    // Klasyczny medalion perski
                    drawCircle(
                        color = accentColor,
                        center = Offset(w / 2, h / 2),
                        radius = w * 0.22f
                    )
                    drawCircle(
                        color = baseColor,
                        center = Offset(w / 2, h / 2),
                        radius = w * 0.12f
                    )
                }
                1 -> {
                    // Geometryczne romby
                    drawLine(
                        color = accentColor,
                        start = Offset(w / 2, h * 0.2f),
                        end = Offset(w * 0.8f, h / 2),
                        strokeWidth = 3f
                    )
                    drawLine(
                        color = accentColor,
                        start = Offset(w * 0.8f, h / 2),
                        end = Offset(w / 2, h * 0.8f),
                        strokeWidth = 3f
                    )
                    drawLine(
                        color = accentColor,
                        start = Offset(w / 2, h * 0.8f),
                        end = Offset(w * 0.2f, h / 2),
                        strokeWidth = 3f
                    )
                    drawLine(
                        color = accentColor,
                        start = Offset(w * 0.2f, h / 2),
                        end = Offset(w / 2, h * 0.2f),
                        strokeWidth = 3f
                    )
                }
                2 -> {
                    // Wzór orientalny roślinny
                    drawCircle(color = accentColor, center = Offset(w * 0.3f, h * 0.3f), radius = w * 0.12f)
                    drawCircle(color = accentColor, center = Offset(w * 0.7f, h * 0.7f), radius = w * 0.12f)
                    drawCircle(color = accentColor, center = Offset(w * 0.5f, h * 0.5f), radius = w * 0.16f)
                }
                3 -> {
                    // Shaggy puszysta faktura
                    for (i in 1..4) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.8f),
                            start = Offset(w * 0.2f * i, h * 0.2f),
                            end = Offset(w * 0.2f * i, h * 0.8f),
                            strokeWidth = 4f
                        )
                    }
                }
                4 -> {
                    // Skandynawskie zygzaki
                    drawLine(color = accentColor, start = Offset(w * 0.2f, h * 0.3f), end = Offset(w * 0.5f, h * 0.5f), strokeWidth = 3f)
                    drawLine(color = accentColor, start = Offset(w * 0.5f, h * 0.5f), end = Offset(w * 0.8f, h * 0.3f), strokeWidth = 3f)
                    drawLine(color = accentColor, start = Offset(w * 0.2f, h * 0.6f), end = Offset(w * 0.5f, h * 0.8f), strokeWidth = 3f)
                    drawLine(color = accentColor, start = Offset(w * 0.5f, h * 0.8f), end = Offset(w * 0.8f, h * 0.6f), strokeWidth = 3f)
                }
                else -> {
                    // Boho pasy i ornament
                    drawRect(color = accentColor, topLeft = Offset(w * 0.15f, h * 0.35f), size = Size(w * 0.7f, h * 0.1f))
                    drawRect(color = accentColor, topLeft = Offset(w * 0.15f, h * 0.55f), size = Size(w * 0.7f, h * 0.1f))
                }
            }
        }
    }
}
