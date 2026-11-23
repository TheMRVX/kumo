package com.v2ray.ang.ui.kumo

import com.v2ray.ang.ui.kumo.theme.MahoorFontFamily
import com.v2ray.ang.ui.kumo.theme.OutfitFontFamily
import com.v2ray.ang.ui.kumo.theme.PlusJakartaSansFontFamily
import com.v2ray.ang.ui.kumo.theme.kumoTextStyles
import com.v2ray.ang.ui.kumo.theme.kumoTypography
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KumoTypographyTest {

    @Test
    fun persianLocalePreservesMahoorFontAcrossAllStyles() {
        val typography = kumoTypography(isPersian = true)
        val customStyles = kumoTextStyles(isPersian = true)

        assertEquals(MahoorFontFamily, typography.displayLarge.fontFamily)
        assertEquals(MahoorFontFamily, typography.headlineLarge.fontFamily)
        assertEquals(MahoorFontFamily, typography.titleMedium.fontFamily)
        assertEquals(MahoorFontFamily, typography.bodyLarge.fontFamily)
        assertEquals(MahoorFontFamily, typography.labelMedium.fontFamily)

        assertEquals(MahoorFontFamily, customStyles.brandWordmark.fontFamily)
        assertEquals(MahoorFontFamily, customStyles.telemetryValue.fontFamily)
        assertEquals(MahoorFontFamily, customStyles.telemetryLabel.fontFamily)
        assertEquals(MahoorFontFamily, customStyles.pingBadge.fontFamily)
    }

    @Test
    fun englishLocaleUsesOutfitDisplayAndPlusJakartaSansBody() {
        val typography = kumoTypography(isPersian = false)
        val customStyles = kumoTextStyles(isPersian = false)

        assertEquals(OutfitFontFamily, typography.displayLarge.fontFamily)
        assertEquals(OutfitFontFamily, typography.headlineLarge.fontFamily)
        assertEquals(OutfitFontFamily, typography.titleMedium.fontFamily)
        assertEquals(PlusJakartaSansFontFamily, typography.bodyLarge.fontFamily)
        assertEquals(PlusJakartaSansFontFamily, typography.labelMedium.fontFamily)

        assertEquals(OutfitFontFamily, customStyles.brandWordmark.fontFamily)
        assertEquals(OutfitFontFamily, customStyles.telemetryValue.fontFamily)
        assertEquals(PlusJakartaSansFontFamily, customStyles.telemetryLabel.fontFamily)
        assertEquals(OutfitFontFamily, customStyles.pingBadge.fontFamily)
    }
}
