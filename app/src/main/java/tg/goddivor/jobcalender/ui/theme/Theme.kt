package tg.goddivor.jobcalender.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext

enum class ThemeMode { FOLLOW_SYSTEM, LIGHT, DARK, AMOLED }

private val LightScheme = lightColorScheme(
    primary = BluePrimaryLight,
    onPrimary = BlueOnPrimaryLight,
    primaryContainer = BluePrimaryContainerLight,
    onPrimaryContainer = BlueOnPrimaryContainerLight,
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceContainerLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = DangerLight,
    errorContainer = DangerContainerLight,
)

private val DarkScheme = darkColorScheme(
    primary = BluePrimaryDark,
    onPrimary = BlueOnPrimaryDark,
    primaryContainer = BluePrimaryContainerDark,
    onPrimaryContainer = BlueOnPrimaryContainerDark,
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceContainerDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = DangerDark,
    errorContainer = DangerContainerDark,
)

private val AmoledScheme = DarkScheme.copy(
    background = SurfaceAmoled,
    surface = SurfaceAmoled,
    surfaceVariant = SurfaceContainerAmoled,
    surfaceContainer = SurfaceContainerAmoled,
    surfaceContainerHigh = SurfaceContainerHighAmoled,
    outlineVariant = OutlineVariantAmoled,
)

@Composable
fun JobCalenderTheme(
    themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }

    val scheme = when {
        // Dynamic colour reaches the accent only; the four semantic colours below never follow it.
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        themeMode == ThemeMode.AMOLED -> AmoledScheme
        dark -> DarkScheme
        else -> LightScheme
    }

    val semantic = when {
        themeMode == ThemeMode.AMOLED -> AmoledSemanticColors
        dark -> DarkSemanticColors
        else -> LightSemanticColors
    }

    CompositionLocalProvider(LocalSemanticColors provides semantic) {
        MaterialTheme(
            colorScheme = scheme,
            typography = JobCalenderTypography,
            content = content,
        )
    }
}

/** Entry point for the four semantic colours: `JobCalenderTheme.semantic.danger`. */
object JobCalenderTheme {
    val semantic: SemanticColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSemanticColors.current
}
