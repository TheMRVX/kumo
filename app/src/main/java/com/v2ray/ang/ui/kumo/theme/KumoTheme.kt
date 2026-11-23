package com.v2ray.ang.ui.kumo.theme

import android.app.Activity
import android.provider.Settings
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.v2ray.ang.ui.compose.LocalDarkTheme

/**
 * Provides whether the user has enabled reduced-motion at the Android system level
 * (`Settings.Global.ANIMATOR_DURATION_SCALE == 0f`).
 */
val LocalReducedMotion = compositionLocalOf { false }

private val KumoDarkColorScheme = darkColorScheme(
    primary = KumoColor.AkatsukiRedVibrant,
    onPrimary = Color.White,
    primaryContainer = KumoColor.AkatsukiRedDeep,
    onPrimaryContainer = Color.White,
    secondary = KumoColor.AkatsukiRed,
    onSecondary = Color.White,
    secondaryContainer = KumoColor.ElevatedSurface,
    onSecondaryContainer = KumoColor.TextPrimary,
    tertiary = KumoColor.PingGood,
    onTertiary = KumoColor.PureBlack,
    error = KumoColor.PingBad,
    onError = Color.White,
    errorContainer = KumoColor.ErrorSurface,
    onErrorContainer = Color.White,
    background = KumoColor.PureBlack,
    onBackground = KumoColor.TextPrimary,
    surface = KumoColor.ObsidianCard,
    onSurface = KumoColor.TextPrimary,
    surfaceVariant = KumoColor.ElevatedSurface,
    onSurfaceVariant = KumoColor.TextSecondary,
    surfaceContainerLowest = KumoColor.PureBlack,
    surfaceContainerLow = KumoColor.ObsidianCard,
    surfaceContainer = KumoColor.ElevatedSurface,
    surfaceContainerHigh = KumoColor.SubtleSurface,
    surfaceContainerHighest = KumoColor.HoverSurface,
    outline = KumoColor.WhiteHairline,
    outlineVariant = KumoColor.WhiteHairlineSubtle,
    scrim = KumoColor.PureBlack
)

private val KumoShapes = Shapes(
    extraSmall = RoundedCornerShape(KumoRadius.xs),
    small = RoundedCornerShape(KumoRadius.sm),
    medium = RoundedCornerShape(KumoRadius.md),
    large = RoundedCornerShape(KumoRadius.lg),
    extraLarge = RoundedCornerShape(KumoRadius.xl)
)

@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        val scale = runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
        }.getOrDefault(1f)
        scale == 0f
    }
}

@Composable
fun KumoTheme(
    content: @Composable () -> Unit
) {
    val reducedMotion = rememberReducedMotion()
    val isPersian = rememberIsPersianLocale()
    val typography = rememberKumoTypography(isPersian)
    val customTextStyles = rememberKumoTextStyles(isPersian)
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            WindowCompat.getInsetsController(activity.window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    CompositionLocalProvider(
        LocalDarkTheme provides true,
        LocalReducedMotion provides reducedMotion,
        LocalKumoTextStyles provides customTextStyles
    ) {
        MaterialTheme(
            colorScheme = KumoDarkColorScheme,
            typography = typography,
            shapes = KumoShapes,
            content = content
        )
    }
}
