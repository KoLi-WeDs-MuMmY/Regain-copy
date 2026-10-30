package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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

@Composable
fun RegainZenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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
