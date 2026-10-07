package com.v2ray.ang.ui.kumo.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KumoDarkColorScheme = darkColorScheme(
    primary = KumoColor.AkatsukiRedVibrant,
    onPrimary = Color.White,
    primaryContainer = KumoColor.AkatsukiRedDeep,
    onPrimaryContainer = Color.White,
    secondary = KumoColor.AkatsukiRed,
    onSecondary = Color.White,
    background = KumoColor.PureBlack,
    onBackground = KumoColor.TextPrimary,
    surface = KumoColor.ObsidianCard,
    onSurface = KumoColor.TextPrimary,
    surfaceVariant = KumoColor.ElevatedSurface,
    onSurfaceVariant = KumoColor.TextSecondary,
    outline = KumoColor.WhiteHairline,
    outlineVariant = KumoColor.WhiteHairlineSubtle,
)

@Composable
fun KumoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KumoDarkColorScheme,
        content = content
    )
}
