package com.v2ray.ang

import com.v2ray.ang.util.HttpUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.InetAddress
import java.net.ServerSocket

class HttpUtilTest {

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
}