package com.v2ray.ang

import com.v2ray.ang.dto.UrlContentRequest
import com.v2ray.ang.util.HttpUtil
import okhttp3.ConnectionSpec
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.TlsVersion
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.nio.file.Path
import java.security.cert.CertificateException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException

class HttpUtilTest {

    @AfterEach
    fun tearDown() {
        HttpUtil.clientBuilderFactory = { OkHttpClient.Builder() }
        HttpUtil.attemptExecutor = { client, req, _ -> client.newCall(req).execute() }
        HttpUtil.currentTimeMs = { System.currentTimeMillis() }
        HttpUtil.clearTlsFallbackCacheForTesting()
    }

    private fun buildOkResponse(request: Request, code: Int = 200, body: String = "ok"): Response {
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code in 200..299) "OK" else "Error")
            .body(body.toResponseBody("text/plain".toMediaType()))
            .build()
    }

    private fun hasTls12OnlySpec(client: OkHttpClient): Boolean {
        return client.connectionSpecs.any { it.tlsVersions == listOf(TlsVersion.TLS_1_2) } &&
            client.connectionSpecs.contains(ConnectionSpec.CLEARTEXT)
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

    @Test
    fun testGetUrlContentWithUserAgent_httpsBlackholeTriggersTls12FallbackAndCachesHost() {
        val observedReadTimeouts = mutableListOf<Int>()
        val observedTls12Flags = mutableListOf<Boolean>()
        var attempts = 0

        HttpUtil.attemptExecutor = { client, req, _ ->
            attempts++
            observedReadTimeouts.add(client.readTimeoutMillis)
            observedTls12Flags.add(hasTls12OnlySpec(client))
            if (attempts == 1) {
                throw SocketTimeoutException("Read timed out during TLS 1.3 handshake")
            }
            buildOkResponse(req, body = "sub-payload")
        }

        val firstBody = HttpUtil.getUrlContentWithUserAgent(
            UrlContentRequest(url = "https://127.0.0.1:2095/sub", timeout = 15_000)
        )
        assertEquals("sub-payload", firstBody)
        assertEquals(2, attempts)
        assertEquals(5_000, observedReadTimeouts[0])
        assertFalse(observedTls12Flags[0])
        assertTrue(observedTls12Flags[1])
        assertTrue(HttpUtil.shouldPreferTls12(HttpUtil.TlsFallbackCacheKey("127.0.0.1", 2095, isProxied = false)))

        val secondBody = HttpUtil.getUrlContentWithUserAgent(
            UrlContentRequest(url = "https://127.0.0.1:2095/sub", timeout = 15_000)
        )
        assertEquals("sub-payload", secondBody)
        assertEquals(3, attempts)
        assertEquals(15_000, observedReadTimeouts[2])
        assertTrue(observedTls12Flags[2])
    }

    @Test
    fun testGetUrlContentWithUserAgent_realLoopbackSocketTimeoutTriggersTls12Fallback() {
        ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { blackhole ->
            val port = blackhole.localPort
            val acceptThread = Thread {
                runCatching {
                    blackhole.accept().use { socket ->
                        Thread.sleep(2_000)
                        socket.close()
                    }
                }
            }.apply { isDaemon = true; start() }

            val observedTls12Flags = mutableListOf<Boolean>()
            var attempt = 0
            HttpUtil.attemptExecutor = { client, req, listener ->
                attempt++
                observedTls12Flags.add(hasTls12OnlySpec(client))
                if (attempt == 1) {
                    try {
                        client.newCall(req).execute()
                    } catch (e: IOException) {
                        assertFalse(listener.secureConnectCompleted)
                        assertFalse(listener.responseHeadersReceived)
                        throw e
                    }
                } else {
                    buildOkResponse(req, body = "recovered-over-tls12")
                }
            }

            val result = HttpUtil.getUrlContentWithUserAgent(
                UrlContentRequest(url = "https://127.0.0.1:$port/sub", timeout = 1_000)
            )
            assertEquals("recovered-over-tls12", result)
            assertEquals(listOf(false, true), observedTls12Flags)
            acceptThread.join(1_000)
        }
    }

    @Test
    fun testGetUrlContentWithUserAgent_postHandshakeHeaderTimeoutRetriesWithDefaultTls13WithoutCaching() {
        val observedTls12Flags = mutableListOf<Boolean>()
        var attempts = 0

        HttpUtil.attemptExecutor = { client, req, listener ->
            attempts++
            observedTls12Flags.add(hasTls12OnlySpec(client))
            if (attempts == 1) {
                listener.secureConnectCompleted = true
                throw SocketTimeoutException("Slow server headers")
            }
            buildOkResponse(req, body = "tls13-slow-headers-ok")
        }

        val result = HttpUtil.getUrlContentWithUserAgent(
            UrlContentRequest(url = "https://127.0.0.1:443/sub", timeout = 15_000)
        )
        assertEquals("tls13-slow-headers-ok", result)
        assertEquals(listOf(false, false), observedTls12Flags)
        assertFalse(HttpUtil.shouldPreferTls12(HttpUtil.TlsFallbackCacheKey("127.0.0.1", 443, isProxied = false)))
    }

    @Test
    fun testGetUrlContentWithUserAgent_cachedTls12FailureEvictsAndSelfHealsToTls13() {
        val key = HttpUtil.TlsFallbackCacheKey("127.0.0.1", 443, isProxied = false)
        HttpUtil.markTls12Preferred(key)

        val observedTls12Flags = mutableListOf<Boolean>()
        var attempts = 0
        HttpUtil.attemptExecutor = { client, req, _ ->
            attempts++
            observedTls12Flags.add(hasTls12OnlySpec(client))
            if (attempts == 1) {
                throw SSLHandshakeException("Server requires TLS 1.3")
            }
            buildOkResponse(req, body = "healed-tls13")
        }

        val result = HttpUtil.getUrlContentWithUserAgent(
            UrlContentRequest(url = "https://127.0.0.1:443/sub", timeout = 15_000)
        )
        assertEquals("healed-tls13", result)
        assertEquals(listOf(true, false), observedTls12Flags)
        assertFalse(HttpUtil.shouldPreferTls12(key))
    }

    @Test
    fun testGetUrlContentWithUserAgent_httpStatusErrorDoesNotRetry() {
        var attempts = 0
        HttpUtil.attemptExecutor = { _, req, _ ->
            attempts++
            buildOkResponse(req, code = 500, body = "Internal Server Error")
        }

        assertThrows(IOException::class.java) {
            HttpUtil.getUrlContentWithUserAgent(
                UrlContentRequest(url = "https://127.0.0.1:443/sub", timeout = 15_000)
            )
        }
        assertEquals(1, attempts)
    }

    @Test
    fun testGetUrlContent_and_downloadToFile_useSharedFallbackWithDownloadCap(@TempDir tempDir: Path) {
        val contentTimeouts = mutableListOf<Int>()
        var contentAttempts = 0
        HttpUtil.attemptExecutor = { client, req, _ ->
            contentAttempts++
            contentTimeouts.add(client.readTimeoutMillis)
            if (contentAttempts == 1) {
                throw SocketTimeoutException("Handshake stalled")
            }
            buildOkResponse(req, body = "content-ok")
        }

        val content = HttpUtil.getUrlContent(
            UrlContentRequest(url = "https://127.0.0.1:443/api", timeout = 15_000)
        )
        assertEquals("content-ok", content)
        assertEquals(2, contentAttempts)
        assertEquals(5_000, contentTimeouts[0])

        HttpUtil.clearTlsFallbackCacheForTesting()
        val downloadTimeouts = mutableListOf<Int>()
        var downloadAttempts = 0
        HttpUtil.attemptExecutor = { client, req, _ ->
            downloadAttempts++
            downloadTimeouts.add(client.readTimeoutMillis)
            if (downloadAttempts == 1) {
                throw SocketTimeoutException("Handshake stalled")
            }
            buildOkResponse(req, body = "geoip-bytes")
        }

        val targetFile: File = tempDir.resolve("geoip.dat").toFile()
        val downloaded = HttpUtil.downloadToFile(
            UrlContentRequest(url = "https://127.0.0.1:443/geoip.dat", timeout = 15_000),
            targetFile
        )
        assertTrue(downloaded)
        assertEquals(2, downloadAttempts)
        assertEquals(7_500, downloadTimeouts[0])
        assertEquals("geoip-bytes", targetFile.readText())
    }

    @Test
    fun testHttpUrl_bypassesTlsFallbackAndUsesFullTimeout() {
        val observedTimeouts = mutableListOf<Int>()
        var attempts = 0
        HttpUtil.attemptExecutor = { client, _, _ ->
            attempts++
            observedTimeouts.add(client.readTimeoutMillis)
            throw SocketTimeoutException("Plain HTTP timeout")
        }

        assertThrows(SocketTimeoutException::class.java) {
            HttpUtil.getUrlContentWithUserAgent(
                UrlContentRequest(url = "http://127.0.0.1:8080/sub", timeout = 15_000)
            )
        }
        assertEquals(1, attempts)
        assertEquals(15_000, observedTimeouts[0])
    }
}