package com.v2ray.ang.handler

import com.v2ray.ang.dto.BatchImportResult
import com.v2ray.ang.dto.SubscriptionImportError
import com.v2ray.ang.dto.SubscriptionUpdateResult
import com.v2ray.ang.fmt.VlessFmt
import com.v2ray.ang.ui.kumo.components.formatCompactSubscriptionUrl
import com.v2ray.ang.util.HttpUtil
import com.v2ray.ang.util.Utils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.URI

class SubscriptionImportParserTest {

    @Test
    fun extractProxyUrisFromHtml_unescapesAndDeduplicatesKumoPortalNodes() {
        val sampleHtml = """
            <!DOCTYPE html>
            <html lang="en">
            <head><title>KUMO VPN</title></head>
            <body>
              <div class="node-card">
                <code class="node-url">vless://11111111-2222-3333-4444-555555555555@edge1.example.ir:443?encryption=mlkem768x25519plus.native.0rtt.sampleKey&amp;security=reality&amp;type=xhttp&amp;mode=auto&amp;sni=cdn.example.com&amp;fp=chrome&amp;pbk=TestPublicKey123&amp;sid=abcd1234&amp;spx=%2F&amp;fm=%7B%22tcp%22%3A%5B%7B%22type%22%3A%22none%22%7D%5D%7D#%F0%9F%87%A9%F0%9F%87%AA%20Germany%2001</code>
                <button data-copy="vless://11111111-2222-3333-4444-555555555555@edge1.example.ir:443?encryption=mlkem768x25519plus.native.0rtt.sampleKey&amp;security=reality&amp;type=xhttp&amp;mode=auto&amp;sni=cdn.example.com&amp;fp=chrome&amp;pbk=TestPublicKey123&amp;sid=abcd1234&amp;spx=%2F&amp;fm=%7B%22tcp%22%3A%5B%7B%22type%22%3A%22none%22%7D%5D%7D#%F0%9F%87%A9%F0%9F%87%AA%20Germany%2001">Copy</button>
              </div>
              <div class="node-card">
                <code class="node-url">vless://aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee@127.0.0.1:1080?encryption=none&amp;security=none&amp;type=tcp#%D8%B3%D8%B1%D9%88%D8%B1%20%D8%A7%D8%B7%D9%84%D8%A7%D8%B9%D8%A7%D8%AA</code>
              </div>
            </body>
            </html>
        """.trimIndent()

        val uris = AngConfigManager.extractProxyUrisFromHtml(sampleHtml)
        assertEquals(2, uris.size)
        assertFalse(uris[0].contains("&amp;"))

        val firstProfile = VlessFmt.parse(uris[0])
        assertNotNull(firstProfile)
        assertEquals("edge1.example.ir", firstProfile!!.server)
        assertEquals("443", firstProfile.serverPort)
        assertEquals("11111111-2222-3333-4444-555555555555", firstProfile.password)
        assertEquals("mlkem768x25519plus.native.0rtt.sampleKey", firstProfile.method)
        assertEquals("xhttp", firstProfile.network)
        assertEquals("auto", firstProfile.xhttpMode)
        assertEquals("reality", firstProfile.security)
        assertEquals("cdn.example.com", firstProfile.sni)
        assertEquals("chrome", firstProfile.fingerPrint)
        assertEquals("TestPublicKey123", firstProfile.publicKey)
        assertEquals("abcd1234", firstProfile.shortId)
        assertEquals("""{"tcp":[{"type":"none"}]}""", firstProfile.finalMask)
        assertTrue(firstProfile.remarks.contains("Germany 01"))

        val secondProfile = VlessFmt.parse(uris[1])
        assertNotNull(secondProfile)
        assertEquals("127.0.0.1", secondProfile!!.server)
        assertEquals("1080", secondProfile.serverPort)
        assertEquals("سرور اطلاعات", secondProfile.remarks)
    }

    @Test
    fun getQueryParam_handlesBareKeysAndEmbeddedEqualsSafely() {
        val uri = URI("vless://user@example.com:443?flag&token=abc=def==&type=xhttp&=ignored#test")
        val params = VlessFmt.getQueryParam(uri)

        assertEquals("", params["flag"])
        assertEquals("abc=def==", params["token"])
        assertEquals("xhttp", params["type"])
        assertFalse(params.containsKey(""))
    }

    @Test
    fun deriveSubscriptionRemarks_neverReturnsBlank() {
        assertEquals("noah.mrvx.ir", AngConfigManager.deriveSubscriptionRemarks("https://noah.mrvx.ir:2095/v1/yhn"))
        assertEquals("noah.mrvx.ir", AngConfigManager.deriveSubscriptionRemarks("https://noah.mrvx.ir:2095/v1/yhn#"))
        assertEquals("KUMO VIP", AngConfigManager.deriveSubscriptionRemarks("https://noah.mrvx.ir:2095/v1/yhn#KUMO%20VIP"))
        assertEquals("KUMO", AngConfigManager.deriveSubscriptionRemarks("not a valid url"))
    }

    @Test
    fun isValidSubUrl_enforcesHttpsByDefaultAndBlocksUnsafeSchemes() {
        val httpsUrl = "https://noah.mrvx.ir:2095/v1/yhn"
        val publicHttpUrl = "http://noah.mrvx.ir:2095/v1/yhn"

        assertTrue(Utils.isValidSubUrl(httpsUrl, allowHttp = false))
        assertFalse(Utils.isHttpSubscriptionUrl(httpsUrl))

        assertFalse(Utils.isValidSubUrl(publicHttpUrl, allowHttp = false))
        assertTrue(Utils.isValidSubUrl(publicHttpUrl, allowHttp = true))
        assertTrue(Utils.isHttpSubscriptionUrl(publicHttpUrl))

        // Local loopback & LAN remain allowed by default
        assertTrue(Utils.isValidSubUrl("http://127.0.0.1:8080/sub", allowHttp = false))
        assertTrue(Utils.isValidSubUrl("http://192.168.1.25:2095/sub", allowHttp = false))

        // Dangerous/non-web schemes are always rejected even when allowHttp = true
        assertFalse(Utils.isValidSubUrl("javascript:alert(1)", allowHttp = true))
        assertFalse(Utils.isValidSubUrl("file:///etc/passwd", allowHttp = true))
        assertFalse(Utils.isValidSubUrl("data:text/plain;base64,SGk=", allowHttp = true))
        assertFalse(Utils.isValidSubUrl("ftp://example.com/sub", allowHttp = true))
    }

    @Test
    fun readBoundedResponseBody_enforcesMaxByteLimit() {
        val small = ByteArrayInputStream("vless://ok".toByteArray(Charsets.UTF_8))
        assertEquals("vless://ok", HttpUtil.readBoundedResponseBody(small, maxBytes = 64))

        val oversized = ByteArrayInputStream(ByteArray(128) { 'a'.code.toByte() })
        assertThrows(IOException::class.java) {
            HttpUtil.readBoundedResponseBody(oversized, maxBytes = 64)
        }
    }

    @Test
    fun formatCompactSubscriptionUrl_preservesShortAndEllipsizesLongUrls() {
        val shortUrl = "https://noah.mrvx.ir:2095/v1/yhn"
        assertEquals(shortUrl, formatCompactSubscriptionUrl(shortUrl))

        val longUrl = "https://panel.example.com:2095/api/v1/client/subscribe?token=very-long-subscription-secret-1234567890"
        val compacted = formatCompactSubscriptionUrl(longUrl, maxLength = 40)
        assertTrue(compacted.length <= 40)
        assertTrue(compacted.contains("…"))
        assertTrue(compacted.startsWith("https://"))
    }

    @Test
    fun batchAndUpdateResults_propagateErrorsAndSuccessAccurately() {
        val combined = SubscriptionUpdateResult(failureCount = 1, lastError = SubscriptionImportError.FETCH_FAILED) +
            SubscriptionUpdateResult(failureCount = 1, lastError = SubscriptionImportError.NO_CONFIGS_FOUND)
        assertEquals(2, combined.failureCount)
        assertEquals(SubscriptionImportError.NO_CONFIGS_FOUND, combined.lastError)

        val successBatch = BatchImportResult(
            subscriptionCount = 1,
            subscriptionConfigCount = 13,
            matchedSubscriptionId = "sub-1",
            refreshedExisting = true
        )
        assertTrue(successBatch.isSuccess)
        assertEquals(13, successBatch.totalImportedConfigs)

        val failedBatch = BatchImportResult(
            subscriptionCount = 1,
            subscriptionConfigCount = 0,
            matchedSubscriptionId = "sub-1",
            error = SubscriptionImportError.FETCH_FAILED
        )
        assertFalse(failedBatch.isSuccess)
    }
}
