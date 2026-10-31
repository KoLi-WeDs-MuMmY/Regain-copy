package com.example.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val ZenDarkColorScheme = darkColorScheme(
    primary = ZenDarkPrimary,
    onPrimary = ZenDarkOnPrimary,
    primaryContainer = ZenDarkPrimaryContainer,
    onPrimaryContainer = ZenDarkOnPrimaryContainer,
    secondary = ZenDarkSecondary,
    onSecondary = ZenDarkOnSecondary,
    secondaryContainer = ZenDarkSecondaryContainer,
    onSecondaryContainer = ZenDarkOnSecondaryContainer,
    tertiary = ZenDarkTertiary,
    onTertiary = ZenDarkOnTertiary,
    tertiaryContainer = ZenDarkTertiaryContainer,
    onTertiaryContainer = ZenDarkOnTertiaryContainer,
    background = ZenDarkBackground,
    onBackground = ZenDarkTextPrimary,
    surface = ZenDarkSurface,
    onSurface = ZenDarkTextPrimary,
    surfaceVariant = ZenDarkElevatedCard,
    onSurfaceVariant = ZenDarkTextSecondary,
    outline = ZenDarkOutline,
    error = ZenError
)

private val ZenLightColorScheme = lightColorScheme(
    primary = ZenSagePrimary,
    onPrimary = ZenSageOnPrimary,
    primaryContainer = ZenSageContainer,
    onPrimaryContainer = ZenOnSageContainer,
    secondary = ZenEucalyptusSecondary,
    onSecondary = ZenOnSecondary,
    secondaryContainer = ZenSecondaryContainer,
    onSecondaryContainer = ZenOnSecondaryContainer,
    tertiary = ZenTerracottaTertiary,
    onTertiary = ZenOnTertiary,
    tertiaryContainer = ZenTerracottaContainer,
    onTertiaryContainer = ZenOnTerracottaContainer,
    background = ZenCreamBackground,
    onBackground = ZenTextPrimary,
    surface = ZenElevatedCard,
    onSurface = ZenTextPrimary,
    surfaceVariant = ZenLinenSurface,
    onSurfaceVariant = ZenTextSecondary,
    outline = ZenOutline,
    error = ZenError
)

val ZenShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

/**
 * Subtle 3D tactile card surface modifier:
 * - Soft ambient drop shadow
 * - Subtle vertical surface gradient (lighter top face -> deeper bottom face)
 * - Top specular rim highlight & bottom shadow edge for a physical 3D feel
 */
fun Modifier.tactile3DCard(
    cornerRadius: Dp = 28.dp,
    elevation: Dp = 12.dp,
    topColor: Color = Color(0xFF1A1D24),
    bottomColor: Color = Color(0xFF121419),
    accentGlow: Color = Color.Transparent
): Modifier {
    val shape: Shape = RoundedCornerShape(cornerRadius)
    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = if (accentGlow != Color.Transparent) accentGlow else Color.Black,
            spotColor = if (accentGlow != Color.Transparent) accentGlow else Color.Black
        )
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(topColor, bottomColor)
            ),
            shape = shape
        )
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.14f),
                    Color.White.copy(alpha = 0.04f),
                    Color.Black.copy(alpha = 0.55f)
                )
            ),
            shape = shape
        )
}

/**
 * Recessed 3D well / inset trough modifier:
 * Simulates a sunken dark cavity with a dark top inner shadow and subtle bottom rim reflection.
 */
fun Modifier.tactileRecessedWell(
    cornerRadius: Dp = 20.dp,
    wellTopColor: Color = Color(0xFF0A0B0E),
    wellBottomColor: Color = Color(0xFF111318)
): Modifier {
    val shape: Shape = RoundedCornerShape(cornerRadius)
    return this
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(wellTopColor, wellBottomColor)
            ),
            shape = shape
        )
        .drawWithContent {
            drawContent()
            val cr = cornerRadius.toPx()
            // Top inner shadow stroke
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.65f),
                        Color.Transparent,
                        Color.White.copy(alpha = 0.07f)
                    )
                ),
                topLeft = Offset.Zero,
                size = Size(size.width, size.height),
                cornerRadius = CornerRadius(cr, cr),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
}

@Composable
fun RegainZenTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ZenDarkColorScheme else ZenLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ZenShapes,
        content = content
    )
}
