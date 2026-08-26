package tg.goddivor.jobcalender.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext

/** What decides between light and dark. Pure black is not one of these: it modifies dark. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Where the colours come from. Dynamic is only offered from Android 12 onwards. */
enum class AppPalette { DEFAULT, DYNAMIC }

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

/** Pure black applies to any dark scheme, the wallpaper's included. */
private fun ColorScheme.blackened(): ColorScheme = copy(
    background = SurfaceAmoled,
    surface = SurfaceAmoled,
    surfaceContainer = SurfaceContainerAmoled,
    surfaceContainerHigh = SurfaceContainerHighAmoled,
)

private val AmoledScheme = DarkScheme.copy(
    background = SurfaceAmoled,
    surface = SurfaceAmoled,
    surfaceVariant = SurfaceContainerAmoled,
    surfaceContainer = SurfaceContainerAmoled,
    surfaceContainerHigh = SurfaceContainerHighAmoled,
    outlineVariant = OutlineVariantAmoled,
)

/** True when this device can extract a palette from the wallpaper. */
val dynamicColorAvailable: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
fun JobCalenderTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    palette: AppPalette = AppPalette.DEFAULT,
    amoled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val pureBlack = dark && amoled

    val scheme = when {
        // Dynamic colour reaches the accent only; the four semantic colours below never follow it.
        palette == AppPalette.DYNAMIC && dynamicColorAvailable -> {
            val context = LocalContext.current
            val dynamic =
                if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (pureBlack) dynamic.blackened() else dynamic
        }
        pureBlack -> AmoledScheme
        dark -> DarkScheme
        else -> LightScheme
    }

    val semantic = when {
        pureBlack -> AmoledSemanticColors
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
