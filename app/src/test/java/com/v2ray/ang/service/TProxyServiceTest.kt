package com.v2ray.ang.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TProxyServiceTest {

    @Test
    fun loadNativeLibrary_returnsTrueWhenLibraryLoads() {
        var loadedName: String? = null
        val loaded = TProxyService.loadNativeLibrary(
            loader = { name -> loadedName = name },
            onLoadFailure = { error("Unexpected failure callback") }
        )

        assertTrue(loaded)
        assertEquals("hev-socks5-tunnel", loadedName)
    }

    @Test
    fun loadNativeLibrary_catchesUnsatisfiedLinkErrorAndReturnsFalse() {
        var capturedError: UnsatisfiedLinkError? = null
        val loaded = TProxyService.loadNativeLibrary(
            loader = { throw UnsatisfiedLinkError("dlopen failed: library \"libhev-socks5-tunnel.so\" not found") },
            onLoadFailure = { error -> capturedError = error }
        )

        assertFalse(loaded)
        assertTrue(capturedError?.message?.contains("libhev-socks5-tunnel.so") == true)
    }
}
