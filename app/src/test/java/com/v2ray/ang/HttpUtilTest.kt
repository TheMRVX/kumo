package com.v2ray.ang

import com.v2ray.ang.util.HttpUtil
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.security.cert.CertificateException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException

class HttpUtilTest {

    @AfterEach
    fun tearDown() {
        HttpUtil.currentTimeMs = { System.currentTimeMillis() }
        HttpUtil.clearTlsFallbackCacheForTesting()
    }

    @Test
    fun testIdnToASCII() {
        // Regular URL remains unchanged
        val regularUrl = "https://example.com/path"
        assertEquals(regularUrl, HttpUtil.toIdnUrl(regularUrl))

        // Non-ASCII URL converts to ASCII (Punycode)
        val nonAsciiUrl = "https://例子.测试/path"
        val expectedNonAscii = "https://xn--fsqu00a.xn--0zwm56d/path"
        assertEquals(expectedNonAscii, HttpUtil.toIdnUrl(nonAsciiUrl))

        // Mixed URL only converts the host part
        val mixedUrl = "https://例子.com/测试"
        val expectedMixed = "https://xn--fsqu00a.com/测试"
        assertEquals(expectedMixed, HttpUtil.toIdnUrl(mixedUrl))

        // URL with Basic Authentication using regular domain
        val basicAuthUrl = "https://user:password@example.com/path"
        assertEquals(basicAuthUrl, HttpUtil.toIdnUrl(basicAuthUrl))

        // URL with Basic Authentication using non-ASCII domain
        val basicAuthNonAscii = "https://user:password@例子.测试/path"
        val expectedBasicAuthNonAscii = "https://user:password@xn--fsqu00a.xn--0zwm56d/path"
        assertEquals(expectedBasicAuthNonAscii, HttpUtil.toIdnUrl(basicAuthNonAscii))

        // URL with non-ASCII username and password
        val nonAsciiAuth = "https://用户:密码@example.com/path"
        // Basic auth credentials should remain unchanged as they're percent-encoded separately
        assertEquals(nonAsciiAuth, HttpUtil.toIdnUrl(nonAsciiAuth))
    }

    @Test
    fun testIsLocalProxyAvailable_invalidPortsReturnFalse() {
        assertFalse(HttpUtil.isLocalProxyAvailable(0))
        assertFalse(HttpUtil.isLocalProxyAvailable(-1))
        assertFalse(HttpUtil.isLocalProxyAvailable(65536))
    }

    @Test
    fun testIsLocalProxyAvailable_detectsListeningAndClosedLoopbackPort() {
        val port: Int
        ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { serverSocket ->
            port = serverSocket.localPort
            assertTrue(HttpUtil.isLocalProxyAvailable(port))
        }
        assertFalse(HttpUtil.isLocalProxyAvailable(port))
    }

    @Test
    fun testIsRetryableTlsOrTimeout_allowsPreHeaderTimeoutAndHandshakeFailures() {
        assertTrue(HttpUtil.isRetryableTlsOrTimeout(SocketTimeoutException("Read timed out"), responseHeadersReceived = false))
        assertTrue(HttpUtil.isRetryableTlsOrTimeout(SSLHandshakeException("Handshake failed"), responseHeadersReceived = false))
        assertTrue(HttpUtil.isRetryableTlsOrTimeout(SSLException("Connection reset"), responseHeadersReceived = false))

        assertFalse(HttpUtil.isRetryableTlsOrTimeout(SocketTimeoutException("Body timed out"), responseHeadersReceived = true))
        assertFalse(HttpUtil.isRetryableTlsOrTimeout(SSLHandshakeException("Late failure"), responseHeadersReceived = true))
        assertFalse(HttpUtil.isRetryableTlsOrTimeout(SSLException("Late SSL error"), responseHeadersReceived = true))
    }

    @Test
    fun testIsRetryableTlsOrTimeout_rejectsCertAndUnverifiedErrors() {
        assertFalse(HttpUtil.isRetryableTlsOrTimeout(SSLPeerUnverifiedException("Hostname mismatch"), responseHeadersReceived = false))
        assertFalse(HttpUtil.isRetryableTlsOrTimeout(SSLException(CertificateException("Expired")), responseHeadersReceived = false))
        assertFalse(
            HttpUtil.isRetryableTlsOrTimeout(
                SSLException(RuntimeException(CertificateException("Untrusted"))),
                responseHeadersReceived = false
            )
        )
        assertFalse(HttpUtil.isRetryableTlsOrTimeout(IOException("HTTP 500"), responseHeadersReceived = false))
    }

    @Test
    fun testComputeAttemptTimeouts_splitsAndClampsCorrectly() {
        assertEquals(5000, HttpUtil.computeInitialAttemptTimeoutMs(15000, 5000))
        assertEquals(2500, HttpUtil.computeInitialAttemptTimeoutMs(5000, 5000))
        assertEquals(7500, HttpUtil.computeInitialAttemptTimeoutMs(15000, 8000))
        assertEquals(8000, HttpUtil.computeInitialAttemptTimeoutMs(20000, 8000))

        assertEquals(10000, HttpUtil.computeRetryTimeoutMs(15000, 5000L))
        assertEquals(3000, HttpUtil.computeRetryTimeoutMs(15000, 14000L))
    }

    @Test
    fun testTlsFallbackCache_keysByHostPortProxyExpiresAndEvicts() {
        var now = 1_000_000L
        HttpUtil.currentTimeMs = { now }

        val keyDirect = HttpUtil.TlsFallbackCacheKey("example.com", 443, isProxied = false)
        val keyProxied = HttpUtil.TlsFallbackCacheKey("example.com", 443, isProxied = true)
        val keyOtherPort = HttpUtil.TlsFallbackCacheKey("example.com", 2095, isProxied = false)

        assertFalse(HttpUtil.shouldPreferTls12(keyDirect))
        HttpUtil.markTls12Preferred(keyDirect)

        assertTrue(HttpUtil.shouldPreferTls12(keyDirect))
        assertFalse(HttpUtil.shouldPreferTls12(keyProxied))
        assertFalse(HttpUtil.shouldPreferTls12(keyOtherPort))

        now += 600_001L
        assertFalse(HttpUtil.shouldPreferTls12(keyDirect))

        HttpUtil.markTls12Preferred(keyDirect)
        assertTrue(HttpUtil.shouldPreferTls12(keyDirect))
        HttpUtil.clearTls12Preference(keyDirect)
        assertFalse(HttpUtil.shouldPreferTls12(keyDirect))

        for (i in 0 until 65) {
            now += 1L
            HttpUtil.markTls12Preferred(HttpUtil.TlsFallbackCacheKey("host-$i.example", 443, isProxied = false))
        }
        assertFalse(HttpUtil.shouldPreferTls12(HttpUtil.TlsFallbackCacheKey("host-0.example", 443, isProxied = false)))
        assertTrue(HttpUtil.shouldPreferTls12(HttpUtil.TlsFallbackCacheKey("host-64.example", 443, isProxied = false)))
    }
}