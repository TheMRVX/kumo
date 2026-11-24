package com.v2ray.ang.util

import com.v2ray.ang.AppConfig
import com.v2ray.ang.AppConfig.LOOPBACK
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.dto.UrlContentRequest
import okhttp3.Call
import okhttp3.ConnectionSpec
import okhttp3.Credentials
import okhttp3.EventListener
import okhttp3.Handshake
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.TlsVersion
import java.io.File
import java.io.IOException
import java.net.IDN
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MalformedURLException
import java.net.Proxy
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URL
import java.security.cert.CertificateException
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import javax.net.ssl.SSLPeerUnverifiedException

object HttpUtil {

    internal const val INITIAL_TLS_HANDSHAKE_CAP_MS = 5_000
    internal const val INITIAL_DOWNLOAD_HANDSHAKE_CAP_MS = 8_000
    private const val TLS_FALLBACK_CACHE_TTL_MS = 10 * 60 * 1000L
    private const val TLS_FALLBACK_CACHE_MAX_ENTRIES = 64

    internal data class TlsFallbackCacheKey(
        val host: String,
        val port: Int,
        val isProxied: Boolean
    )

    internal var currentTimeMs: () -> Long = { System.currentTimeMillis() }
    internal var clientBuilderFactory: () -> OkHttpClient.Builder = { OkHttpClient.Builder() }
    internal var attemptExecutor: (OkHttpClient, Request, TlsAttemptEventListener) -> Response =
        { client, request, _ -> client.newCall(request).execute() }

    private val TLS_1_2_CONNECTION_SPEC = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
        .tlsVersions(TlsVersion.TLS_1_2)
        .build()

    private val TLS_1_2_SPECS = listOf(TLS_1_2_CONNECTION_SPEC, ConnectionSpec.CLEARTEXT)

    internal class TlsAttemptEventListener : EventListener() {
        @Volatile
        internal var secureConnectCompleted = false

        @Volatile
        internal var responseHeadersReceived = false

        @Volatile
        internal var redirected = false

        override fun connectStart(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) {
            secureConnectCompleted = false
            responseHeadersReceived = false
        }

        override fun secureConnectStart(call: Call) {
            secureConnectCompleted = false
        }

        override fun secureConnectEnd(call: Call, handshake: Handshake?) {
            secureConnectCompleted = true
        }

        override fun requestHeadersStart(call: Call) {
            responseHeadersReceived = false
        }

        override fun responseHeadersEnd(call: Call, response: Response) {
            responseHeadersReceived = true
            if (response.isRedirect) {
                redirected = true
            }
        }
    }

    private sealed interface SubscriptionFetchStep {
        data class Redirect(val nextUrl: String) : SubscriptionFetchStep
        data class Complete(val body: String) : SubscriptionFetchStep
    }

    private val tlsFallbackCache = object : LinkedHashMap<TlsFallbackCacheKey, Long>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<TlsFallbackCacheKey, Long>?): Boolean {
            return size > TLS_FALLBACK_CACHE_MAX_ENTRIES
        }
    }

    internal fun shouldPreferTls12(key: TlsFallbackCacheKey): Boolean {
        val now = currentTimeMs()
        synchronized(tlsFallbackCache) {
            val recordedAt = tlsFallbackCache[key] ?: return false
            if (now - recordedAt > TLS_FALLBACK_CACHE_TTL_MS) {
                tlsFallbackCache.remove(key)
                return false
            }
            return true
        }
    }

    internal fun markTls12Preferred(key: TlsFallbackCacheKey) {
        val now = currentTimeMs()
        synchronized(tlsFallbackCache) {
            val iterator = tlsFallbackCache.entries.iterator()
            while (iterator.hasNext()) {
                if (now - iterator.next().value > TLS_FALLBACK_CACHE_TTL_MS) {
                    iterator.remove()
                }
            }
            tlsFallbackCache[key] = now
        }
    }

    internal fun clearTls12Preference(key: TlsFallbackCacheKey) {
        synchronized(tlsFallbackCache) {
            tlsFallbackCache.remove(key)
        }
    }

    internal fun clearTlsFallbackCacheForTesting() {
        synchronized(tlsFallbackCache) {
            tlsFallbackCache.clear()
        }
    }

    internal fun computeInitialAttemptTimeoutMs(totalTimeoutMs: Int, maxInitialMs: Int): Int {
        val safeTotal = maxOf(1, totalTimeoutMs)
        return minOf(maxInitialMs, maxOf(1, safeTotal / 2))
    }

    internal fun computeRetryTimeoutMs(totalTimeoutMs: Int, elapsedMs: Long): Int {
        val safeTotal = maxOf(1, totalTimeoutMs)
        val floor = minOf(safeTotal, maxOf(1, minOf(3_000, safeTotal / 2)))
        return (safeTotal.toLong() - elapsedMs).coerceIn(floor.toLong(), safeTotal.toLong()).toInt()
    }

    internal fun isRetryableTlsOrTimeout(error: IOException, responseHeadersReceived: Boolean): Boolean {
        if (responseHeadersReceived) return false
        if (error is SocketTimeoutException) return true
        if (error is SSLException) {
            if (error is SSLPeerUnverifiedException) return false
            val visited = HashSet<Throwable>()
            var current: Throwable? = error.cause
            while (current != null && visited.add(current)) {
                if (current is CertificateException) return false
                current = current.cause
            }
            return true
        }
        return false
    }

    /**
     * Converts the domain part of a URL string to its IDN (Punycode, ASCII Compatible Encoding) format.
     *
     * For example, a URL like "https://例子.中国/path" will be converted to "https://xn--fsqu00a.xn--fiqs8s/path".
     *
     * @param str The URL string to convert (can contain non-ASCII characters in the domain).
     * @return The URL string with the domain part converted to ASCII-compatible (Punycode) format.
     */
    fun toIdnUrl(str: String): String {
        val url = URL(str)
        val host = url.host
        val asciiHost = IDN.toASCII(url.host, IDN.ALLOW_UNASSIGNED)
        if (host != asciiHost) {
            return str.replace(host, asciiHost)
        } else {
            return str
        }
    }

    /**
     * Converts a Unicode domain name to its IDN (Punycode, ASCII Compatible Encoding) format.
     * If the input is an IP address or already an ASCII domain, returns the original string.
     *
     * @param domain The domain string to convert (can include non-ASCII internationalized characters).
     * @return The domain in ASCII-compatible (Punycode) format, or the original string if input is an IP or already ASCII.
     */
    fun toIdnDomain(domain: String): String {
        // Return as is if it's a pure IP address (IPv4 or IPv6)
        if (Utils.isPureIpAddress(domain)) {
            return domain
        }

        // Return as is if already ASCII (English domain or already punycode)
        if (domain.all { it.code < 128 }) {
            return domain
        }

        // Otherwise, convert to ASCII using IDN
        return IDN.toASCII(domain, IDN.ALLOW_UNASSIGNED)
    }

    /**
     * Resolves a hostname to an IP address, returns original input if it's already an IP
     *
     * @param host The hostname or IP address to resolve
     * @param ipv6Preferred Whether to prefer IPv6 addresses, defaults to false
     * @return The resolved IP address or the original input (if it's already an IP or resolution fails)
     */
    fun resolveHostToIP(host: String, ipv6Preferred: Boolean = false): List<String>? {
        try {
            // If it's already an IP address, return it as a list
            if (Utils.isPureIpAddress(host)) {
                return null
            }

            // Get all IP addresses
            val addresses = InetAddress.getAllByName(host)
            if (addresses.isEmpty()) {
                return null
            }

            // Sort addresses based on preference
            val sortedAddresses = if (ipv6Preferred) {
                addresses.sortedWith(compareByDescending { it is Inet6Address })
            } else {
                addresses.sortedWith(compareBy { it is Inet6Address })
            }

            val ipList = sortedAddresses.mapNotNull { it.hostAddress }

            LogUtil.i(AppConfig.TAG, "Resolved IPs for $host: ${ipList.joinToString()}")

            return ipList
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to resolve host to IP", e)
            return null
        }
    }


    /**
     * Retrieves the content of a URL as a string.
     *
     * @param url The URL to fetch content from.
     * @param timeout The timeout value in milliseconds.
     * @param httpPort The HTTP port to use.
     * @return The content of the URL as a string.
     */
    fun getUrlContent(request: UrlContentRequest): String? {
        val url = request.url ?: return null
        val requestBuilder = Request.Builder()
            .url(url)
            .get()
            .header("Connection", "close")
        if (request.httpPort != 0 && !request.proxyUsername.isNullOrBlank() && !request.proxyPassword.isNullOrBlank()) {
            requestBuilder.header("Proxy-Authorization", Credentials.basic(request.proxyUsername, request.proxyPassword))
        }
        try {
            return executeWithTlsFallback(
                url = url,
                timeoutMs = request.timeout,
                initialCapMs = INITIAL_TLS_HANDSHAKE_CAP_MS,
                httpPort = request.httpPort,
                proxyUsername = request.proxyUsername,
                proxyPassword = request.proxyPassword,
                followRedirects = true,
                request = requestBuilder.build()
            ) { response ->
                if (!response.isSuccessful) {
                    LogUtil.w(AppConfig.TAG, "Failed to get URL content, code=${response.code}")
                    null
                } else {
                    response.body?.string()
                }
            }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to get URL content", e)
        }
        return null
    }

    /**
     * Checks whether a local HTTP proxy is actively accepting TCP connections on loopback.
     *
     * @param httpPort The configured local HTTP proxy port.
     * @param timeoutMs Short loopback connection timeout in milliseconds.
     * @return True if loopback accepts a TCP connection on [httpPort], false otherwise.
     */
    fun isLocalProxyAvailable(httpPort: Int, timeoutMs: Int = 300): Boolean {
        if (httpPort !in 1..65535) return false
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(LOOPBACK, httpPort), timeoutMs)
            }
            true
        } catch (_: IOException) {
            false
        }
    }

    private const val MAX_SUBSCRIPTION_BODY_BYTES = 5 * 1024 * 1024L

    /**
     * Retrieves the content of a URL as a string with a custom User-Agent header.
     *
     * @param url The URL to fetch content from.
     * @param timeout The timeout value in milliseconds.
     * @param httpPort The HTTP port to use.
     * @return The content of the URL as a string.
     * @throws IOException If an I/O error occurs.
     */
    @Throws(IOException::class)
    fun getUrlContentWithUserAgent(request: UrlContentRequest): String {
        var currentUrl = request.url?.trim()
        var redirects = 0
        val maxRedirects = 5

        while (redirects++ < maxRedirects) {
            val activeUrl = currentUrl ?: throw IOException("Empty subscription URL")
            val scheme = runCatching { URI(Utils.fixIllegalUrl(activeUrl)).scheme?.lowercase() }.getOrNull()
            if (scheme != "https" && scheme != "http") {
                throw IOException("Unsupported URL scheme")
            }

            val requestBuilder = Request.Builder()
                .url(activeUrl)
                .get()
                .header("User-Agent", finalUserAgent)
                .header("Accept", "text/plain, */*;q=0.1")
                .header("Connection", "close")

            applyEmbeddedBasicAuthHeader(activeUrl, requestBuilder)

            val headersMap = JsonUtil.parseHeadersToMap(request.requestHeaders)
            for ((key, value) in headersMap) {
                LogUtil.d(AppConfig.TAG, "Adding custom header: $key")
                try {
                    requestBuilder.header(key, value)
                } catch (_: IllegalArgumentException) {
                }
            }

            if (request.httpPort != 0 && !request.proxyUsername.isNullOrBlank() && !request.proxyPassword.isNullOrBlank()) {
                requestBuilder.header("Proxy-Authorization", Credentials.basic(request.proxyUsername, request.proxyPassword))
            }

            val stepResult = executeWithTlsFallback(
                url = activeUrl,
                timeoutMs = request.timeout,
                initialCapMs = INITIAL_TLS_HANDSHAKE_CAP_MS,
                httpPort = request.httpPort,
                proxyUsername = request.proxyUsername,
                proxyPassword = request.proxyPassword,
                followRedirects = false,
                request = requestBuilder.build()
            ) { response ->
                when {
                    response.isRedirect -> {
                        val location = response.header("Location")
                        if (location.isNullOrEmpty()) {
                            throw IOException("Redirect location not found")
                        }
                        val nextUrl = resolveLocation(activeUrl, location)
                        if (nextUrl.isNullOrEmpty()) {
                            throw IOException("Failed to resolve redirect location")
                        }
                        val nextScheme = runCatching { URI(Utils.fixIllegalUrl(nextUrl)).scheme?.lowercase() }.getOrNull()
                        if (nextScheme != "https" && nextScheme != "http") {
                            throw IOException("Unsupported redirect scheme")
                        }
                        SubscriptionFetchStep.Redirect(nextUrl)
                    }

                    response.isSuccessful -> {
                        val body = response.body
                        val text = if (body == null) {
                            ""
                        } else {
                            readBoundedResponseBody(body.byteStream(), MAX_SUBSCRIPTION_BODY_BYTES)
                        }
                        SubscriptionFetchStep.Complete(text)
                    }

                    else -> {
                        throw IOException("Request failed with status code ${response.code}")
                    }
                }
            }

            when (stepResult) {
                is SubscriptionFetchStep.Redirect -> {
                    currentUrl = stepResult.nextUrl
                    continue
                }

                is SubscriptionFetchStep.Complete -> return stepResult.body
            }
        }
        throw IOException("Too many redirects")
    }

    internal fun readBoundedResponseBody(
        inputStream: java.io.InputStream,
        maxBytes: Long = MAX_SUBSCRIPTION_BODY_BYTES
    ): String {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var totalRead = 0L
        while (true) {
            val read = inputStream.read(buffer)
            if (read == -1) break
            totalRead += read
            if (totalRead > maxBytes) {
                throw IOException("Response body exceeds maximum allowed size")
            }
            output.write(buffer, 0, read)
        }
        return output.toString(Charsets.UTF_8.name())
    }

    private fun applyEmbeddedBasicAuthHeader(rawUrl: String, requestBuilder: Request.Builder) {
        val parsed = runCatching { URL(rawUrl) }.getOrNull() ?: return
        parsed.userInfo?.let { userInfo ->
            val colon = userInfo.indexOf(':')
            val user = runCatching {
                Utils.decodeURIComponent(if (colon >= 0) userInfo.substring(0, colon) else userInfo)
            }.getOrDefault(if (colon >= 0) userInfo.substring(0, colon) else userInfo)
            val pass = runCatching {
                Utils.decodeURIComponent(if (colon >= 0) userInfo.substring(colon + 1) else "")
            }.getOrDefault(if (colon >= 0) userInfo.substring(colon + 1) else "")
            requestBuilder.header("Authorization", Credentials.basic(user, pass))
        }
    }

    private fun <T> executeWithTlsFallback(
        url: String,
        timeoutMs: Int,
        initialCapMs: Int,
        httpPort: Int,
        proxyUsername: String?,
        proxyPassword: String?,
        followRedirects: Boolean,
        request: Request,
        responseHandler: (Response) -> T
    ): T {
        val parsedUri = runCatching { URI(Utils.fixIllegalUrl(url)) }.getOrNull()
        val scheme = parsedUri?.scheme?.lowercase(Locale.ROOT)
        val rawHost = parsedUri?.host
        if (scheme != "https" || rawHost.isNullOrBlank()) {
            val listener = TlsAttemptEventListener()
            val client = buildOkHttpClient(
                timeout = timeoutMs,
                httpPort = httpPort,
                proxyUsername = proxyUsername,
                proxyPassword = proxyPassword,
                followRedirects = followRedirects,
                eventListener = listener
            )
            return attemptExecutor(client, request, listener).use(responseHandler)
        }

        val normalizedHost = toIdnDomain(rawHost.trim('[', ']')).lowercase(Locale.ROOT)
        val effectivePort = if (parsedUri.port > 0) parsedUri.port else 443
        val isProxied = httpPort != 0
        val cacheKey = TlsFallbackCacheKey(normalizedHost, effectivePort, isProxied)

        if (shouldPreferTls12(cacheKey)) {
            val startMs = currentTimeMs()
            val cachedListener = TlsAttemptEventListener()
            val tls12Client = buildOkHttpClient(
                timeout = timeoutMs,
                httpPort = httpPort,
                proxyUsername = proxyUsername,
                proxyPassword = proxyPassword,
                followRedirects = followRedirects,
                connectionSpecs = TLS_1_2_SPECS,
                eventListener = cachedListener
            )
            try {
                return attemptExecutor(tls12Client, request, cachedListener).use(responseHandler)
            } catch (e: IOException) {
                if (!isRetryableTlsOrTimeout(e, cachedListener.responseHeadersReceived)) {
                    throw e
                }
                clearTls12Preference(cacheKey)
                LogUtil.w(
                    AppConfig.TAG,
                    "Cached TLS 1.2 failed for $normalizedHost:$effectivePort (proxied=$isProxied), retrying default TLS",
                    e
                )
                val retryTimeout = computeRetryTimeoutMs(timeoutMs, currentTimeMs() - startMs)
                val fallbackListener = TlsAttemptEventListener()
                val defaultClient = buildOkHttpClient(
                    timeout = retryTimeout,
                    httpPort = httpPort,
                    proxyUsername = proxyUsername,
                    proxyPassword = proxyPassword,
                    followRedirects = followRedirects,
                    eventListener = fallbackListener
                )
                return attemptExecutor(defaultClient, request, fallbackListener).use(responseHandler)
            }
        }

        val startMs = currentTimeMs()
        val initialTimeout = computeInitialAttemptTimeoutMs(timeoutMs, initialCapMs)
        val primaryListener = TlsAttemptEventListener()
        val primaryClient = buildOkHttpClient(
            timeout = initialTimeout,
            httpPort = httpPort,
            proxyUsername = proxyUsername,
            proxyPassword = proxyPassword,
            followRedirects = followRedirects,
            eventListener = primaryListener
        )

        return try {
            attemptExecutor(primaryClient, request, primaryListener).use(responseHandler)
        } catch (e: IOException) {
            if (!isRetryableTlsOrTimeout(e, primaryListener.responseHeadersReceived)) {
                throw e
            }
            val retryTimeout = computeRetryTimeoutMs(timeoutMs, currentTimeMs() - startMs)
            val retryListener = TlsAttemptEventListener()
            if (!primaryListener.secureConnectCompleted) {
                LogUtil.w(
                    AppConfig.TAG,
                    "TLS handshake stalled/failed for $normalizedHost:$effectivePort (proxied=$isProxied), retrying TLS 1.2",
                    e
                )
                val tls12Client = buildOkHttpClient(
                    timeout = retryTimeout,
                    httpPort = httpPort,
                    proxyUsername = proxyUsername,
                    proxyPassword = proxyPassword,
                    followRedirects = followRedirects,
                    connectionSpecs = TLS_1_2_SPECS,
                    eventListener = retryListener
                )
                val result = attemptExecutor(tls12Client, request, retryListener).use(responseHandler)
                if (!retryListener.redirected) {
                    markTls12Preferred(cacheKey)
                }
                result
            } else {
                LogUtil.w(
                    AppConfig.TAG,
                    "Pre-header timeout after TLS 1.3 handshake for $normalizedHost:$effectivePort (proxied=$isProxied), retrying default TLS",
                    e
                )
                val defaultClient = buildOkHttpClient(
                    timeout = retryTimeout,
                    httpPort = httpPort,
                    proxyUsername = proxyUsername,
                    proxyPassword = proxyPassword,
                    followRedirects = followRedirects,
                    eventListener = retryListener
                )
                attemptExecutor(defaultClient, request, retryListener).use(responseHandler)
            }
        }
    }

    private fun buildOkHttpClient(
        timeout: Int,
        httpPort: Int,
        proxyUsername: String?,
        proxyPassword: String?,
        followRedirects: Boolean,
        connectionSpecs: List<ConnectionSpec>? = null,
        eventListener: EventListener? = null
    ): OkHttpClient {
        val builder = clientBuilderFactory()
            .connectTimeout(timeout.toLong(), TimeUnit.MILLISECONDS)
            .readTimeout(timeout.toLong(), TimeUnit.MILLISECONDS)
            .followRedirects(followRedirects)
            .followSslRedirects(followRedirects)

        if (connectionSpecs != null) {
            builder.connectionSpecs(connectionSpecs)
        }
        if (eventListener != null) {
            builder.eventListener(eventListener)
        }

        if (httpPort != 0) {
            builder.proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(LOOPBACK, httpPort)))
            if (!proxyUsername.isNullOrBlank() && !proxyPassword.isNullOrBlank()) {
                builder.proxyAuthenticator { _, response ->
                    if (response.request.header("Proxy-Authorization") != null) {
                        null
                    } else {
                        response.request.newBuilder()
                            .header("Proxy-Authorization", Credentials.basic(proxyUsername, proxyPassword))
                            .build()
                    }
                }
            }
        }

        return builder.build()
    }

    private fun resolveLocation(baseUrl: String, raw: String): String? {
        return try {
            val locUri = URI(raw)
            val baseUri = URI(baseUrl)
            val resolved = if (locUri.isAbsolute) locUri else baseUri.resolve(locUri)
            resolved.toURL().toString()
        } catch (_: Exception) {
            try {
                URL(raw).toString()
            } catch (_: MalformedURLException) {
                try {
                    URL(URL(baseUrl), raw).toString()
                } catch (_: MalformedURLException) {
                    null
                }
            }
        }
    }

    fun downloadToFile(
        request: UrlContentRequest,
        targetFile: File
    ): Boolean {
        val url = request.url ?: return false
        val requestBuilder = Request.Builder()
            .url(url)
            .get()
            .header("Connection", "close")
        if (request.httpPort != 0 && !request.proxyUsername.isNullOrBlank() && !request.proxyPassword.isNullOrBlank()) {
            requestBuilder.header("Proxy-Authorization", Credentials.basic(request.proxyUsername, request.proxyPassword))
        }

        return try {
            executeWithTlsFallback(
                url = url,
                timeoutMs = request.timeout,
                initialCapMs = INITIAL_DOWNLOAD_HANDSHAKE_CAP_MS,
                httpPort = request.httpPort,
                proxyUsername = request.proxyUsername,
                proxyPassword = request.proxyPassword,
                followRedirects = true,
                request = requestBuilder.build()
            ) { response ->
                if (!response.isSuccessful) {
                    LogUtil.w(AppConfig.TAG, "Failed to download file, code=${response.code}, url=$url")
                    return@executeWithTlsFallback false
                }
                val body = response.body ?: return@executeWithTlsFallback false
                body.byteStream().use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                true
            }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to download file: $url", e)
            false
        }
    }
}
