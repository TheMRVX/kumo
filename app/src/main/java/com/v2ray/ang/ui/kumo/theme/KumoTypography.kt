package com.v2ray.ang.ui.kumo.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.os.ConfigurationCompat
import com.v2ray.ang.R

/** Preserved Persian typeface with full 4-weight optical family. */
val MahoorFontFamily = FontFamily(
    Font(R.font.font_mahoor_regular, FontWeight.Normal),
    Font(R.font.font_mahoor_medium, FontWeight.Medium),
    Font(R.font.font_mahoor_semibold, FontWeight.SemiBold),
    Font(R.font.font_mahoor_bold, FontWeight.Bold)
)

/** Geometric display typeface for English wordmarks, headlines, titles, and numeric telemetry. */
val OutfitFontFamily = FontFamily(
    Font(R.font.outfit, FontWeight.Normal)
)

/** Refined contemporary sans-serif for English body copy, controls, and labels. */
val PlusJakartaSansFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal)
)

/** Adds OpenType tabular figures (`tnum`) so numeric telemetry and ping counters never jitter horizontally. */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")

@Immutable
data class KumoCustomTextStyles(
    val brandWordmark: TextStyle,
    val telemetryValue: TextStyle,
    val telemetryLabel: TextStyle,
    val pingBadge: TextStyle
)

fun kumoTextStyles(isPersian: Boolean): KumoCustomTextStyles {
    val displayFamily = if (isPersian) MahoorFontFamily else OutfitFontFamily
    val labelFamily = if (isPersian) MahoorFontFamily else PlusJakartaSansFontFamily

    return KumoCustomTextStyles(
        brandWordmark = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = if (isPersian) 2.2.sp else 2.6.sp
        ),
        telemetryValue = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.sp,
            fontFeatureSettings = "tnum"
        ),
        telemetryLabel = TextStyle(
            fontFamily = labelFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            letterSpacing = if (isPersian) 0.8.sp else 0.9.sp
        ),
        pingBadge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.sp,
            fontFeatureSettings = "tnum"
        )
    )
}

fun kumoTypography(isPersian: Boolean): Typography {
    val displayFamily = if (isPersian) MahoorFontFamily else OutfitFontFamily
    val bodyFamily = if (isPersian) MahoorFontFamily else PlusJakartaSansFontFamily

    return Typography(
        displayLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = (-0.5).sp
        ),
        displayMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = (-0.3).sp
        ),
        displaySmall = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = (-0.2).sp
        ),
        headlineLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = (-0.15).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 19.sp,
            lineHeight = 26.sp,
            letterSpacing = (-0.1).sp
        ),
        headlineSmall = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.sp
        ),
        titleLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            letterSpacing = (-0.1).sp
        ),
        titleMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            letterSpacing = 0.sp
        ),
        titleSmall = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            letterSpacing = 0.1.sp
        ),
        bodySmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            letterSpacing = 0.15.sp
        ),
        labelLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.15.sp
        ),
        labelMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.25.sp
        ),
        labelSmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            letterSpacing = 0.4.sp
        )
    )
}

private val PersianKumoTypography: Typography = kumoTypography(isPersian = true)
private val EnglishKumoTypography: Typography = kumoTypography(isPersian = false)
private val PersianKumoTextStyles: KumoCustomTextStyles = kumoTextStyles(isPersian = true)
private val EnglishKumoTextStyles: KumoCustomTextStyles = kumoTextStyles(isPersian = false)

val LocalKumoTextStyles = staticCompositionLocalOf { EnglishKumoTextStyles }

/** Default typography (English) for non-themed contexts and previews. */
val KumoTypography: Typography = EnglishKumoTypography

@Composable
fun rememberIsPersianLocale(): Boolean {
    val configuration = LocalConfiguration.current
    return remember(configuration) {
        ConfigurationCompat.getLocales(configuration)[0]?.language.equals("fa", ignoreCase = true)
    }
}

@Composable
fun rememberKumoTypography(isPersian: Boolean = rememberIsPersianLocale()): Typography =
    if (isPersian) PersianKumoTypography else EnglishKumoTypography

@Composable
fun rememberKumoTextStyles(isPersian: Boolean = rememberIsPersianLocale()): KumoCustomTextStyles =
    if (isPersian) PersianKumoTextStyles else EnglishKumoTextStyles

object KumoTextStyles {
    val brandWordmark: TextStyle
        @Composable get() = LocalKumoTextStyles.current.brandWordmark

    val telemetryValue: TextStyle
        @Composable get() = LocalKumoTextStyles.current.telemetryValue

    val telemetryLabel: TextStyle
        @Composable get() = LocalKumoTextStyles.current.telemetryLabel

    val pingBadge: TextStyle
        @Composable get() = LocalKumoTextStyles.current.pingBadge
}
