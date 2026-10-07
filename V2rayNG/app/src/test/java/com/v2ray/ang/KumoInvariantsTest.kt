package com.v2ray.ang

import com.v2ray.ang.enums.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KumoInvariantsTest {

    @Test
    fun test_languageEnumInvariant() {
        val supportedLanguages = Language.entries.map { it.name }
        assertEquals(listOf("AUTO", "ENGLISH", "PERSIAN"), supportedLanguages)
        assertEquals(3, Language.entries.size)
    }

    @Test
    fun test_languageCodes() {
        assertEquals("auto", Language.AUTO.code)
        assertEquals("", Language.AUTO.languageTag)
        assertEquals("en", Language.ENGLISH.code)
        assertEquals("en", Language.ENGLISH.languageTag)
        assertEquals("fa", Language.PERSIAN.code)
        assertEquals("fa", Language.PERSIAN.languageTag)
    }

    @Test
    fun test_appPackageInvariant() {
        assertTrue(AppConfig.ANG_PACKAGE == "com.v2ray.ang" || AppConfig.ANG_PACKAGE == "com.v2ray.ang.fdroid")
    }

    @Test
    fun test_assetsDirectoryInvariant() {
        assertEquals("assets", AppConfig.DIR_ASSETS)
    }
}
