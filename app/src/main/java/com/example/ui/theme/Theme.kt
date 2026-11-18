package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Immutable
data class MarginDeskSemanticColors(
    val profitContainer: Color,
    val profitContent: Color,
    val profitAccent: Color,
    val lossContainer: Color,
    val lossContent: Color,
    val lowStockContainer: Color,
    val lowStockContent: Color,
    val cardBorder: Color,
    val surfaceElevated: Color,
)

private val DefaultSemanticColors = MarginDeskSemanticColors(
    profitContainer = Emerald950,
    profitContent = Emerald300,
    profitAccent = Emerald500,
    lossContainer = Coral950,
    lossContent = Coral400,
    lowStockContainer = Amber950,
    lowStockContent = Amber300,
    cardBorder = Slate700,
    surfaceElevated = Slate800,
)

val LocalMarginDeskColors = staticCompositionLocalOf { DefaultSemanticColors }

private val DarkSlateColorScheme = darkColorScheme(
    primary = Emerald500,
    onPrimary = DeepSlate950,
    primaryContainer = Emerald900,
    onPrimaryContainer = Emerald300,
    secondary = Emerald400,
    onSecondary = DeepSlate950,
    secondaryContainer = Slate750,
    onSecondaryContainer = Slate100,
    tertiary = Amber500,
    onTertiary = DeepSlate950,
    background = DeepSlate950,
    onBackground = Slate100,
    surface = Slate850,
    onSurface = Slate100,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate400,
    surfaceContainerLowest = DeepSlate950,
    surfaceContainerLow = Slate850,
    surfaceContainer = Slate800,
    surfaceContainerHigh = Slate750,
    surfaceContainerHighest = Slate700,
    outline = Slate700,
    outlineVariant = Slate750,
    error = Coral400,
    onError = Coral950,
    errorContainer = Coral950,
    onErrorContainer = Coral200,
)

@Suppress("unused")
private val LightFallbackColorScheme = lightColorScheme(
    primary = Emerald600,
    onPrimary = Color.White,
    background = LightSurface,
    surface = LightCard,
    surfaceVariant = LightVariant,
)

@Composable
fun MarginDeskTheme(
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(context)
    } else {
        DarkSlateColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
        }
    }

    CompositionLocalProvider(LocalMarginDeskColors provides DefaultSemanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MarginDeskTypography,
            content = content,
        )
    }
}
