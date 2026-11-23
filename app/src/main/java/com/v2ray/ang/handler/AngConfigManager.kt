package com.v2ray.ang.handler

import android.content.Context
import android.graphics.Bitmap
import android.text.TextUtils
import com.v2ray.ang.AppConfig
import com.v2ray.ang.core.CoreConfigManager
import com.v2ray.ang.dto.BatchImportResult
import com.v2ray.ang.dto.SubscriptionImportError
import com.v2ray.ang.dto.SubscriptionUpdateResult
import com.v2ray.ang.dto.UrlContentRequest
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.dto.entities.SubscriptionCache
import com.v2ray.ang.dto.entities.SubscriptionItem
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.extension.isNotNullEmpty
import com.v2ray.ang.fmt.CustomFmt
import com.v2ray.ang.fmt.Hysteria2Fmt
import com.v2ray.ang.fmt.ShadowsocksFmt
import com.v2ray.ang.fmt.SocksFmt
import com.v2ray.ang.fmt.TrojanFmt
import com.v2ray.ang.fmt.V2rayNFmt
import com.v2ray.ang.fmt.VlessFmt
import com.v2ray.ang.fmt.VmessFmt
import com.v2ray.ang.fmt.WireguardFmt
import com.v2ray.ang.util.HttpUtil
import com.v2ray.ang.util.JsonUtil
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.QRCodeDecoder
import com.v2ray.ang.util.Utils
import java.net.URI

object AngConfigManager {

    private data class ParsedProfile(
        val profile: ProfileItem,
        val rawConfig: String? = null,
    )

    private data class SubscriptionUpsertResult(
        val guid: String,
        val existed: Boolean,
    )

    private data class SubscriptionBatchParseResult(
        val subscriptions: List<SubscriptionUpsertResult>,
        val sawBlockedHttpUrl: Boolean,
    )

    private val embeddedProxyUriRegex = Regex(
        """(?i)\b(?:vless|vmess|trojan|ss|socks|socks4|socks5|wireguard|hysteria2|hy2|v2rayn)://[^\s"'<>`\\]+"""
    )

    // Parser mapping for different config types (lazy initialized)
    private val configFmtParsers: Map<String, (String) -> ProfileItem?> by lazy {
        mapOf(
            EConfigType.VMESS.protocolScheme to VmessFmt::parse,
            EConfigType.SHADOWSOCKS.protocolScheme to ShadowsocksFmt::parse,
            EConfigType.SOCKS.protocolScheme to SocksFmt::parse,
            AppConfig.SOCKS4 to SocksFmt::parse,
            AppConfig.SOCKS5 to SocksFmt::parse,
            EConfigType.TROJAN.protocolScheme to TrojanFmt::parse,
            EConfigType.VLESS.protocolScheme to VlessFmt::parse,
            EConfigType.WIREGUARD.protocolScheme to WireguardFmt::parse,
            EConfigType.HYSTERIA2.protocolScheme to Hysteria2Fmt::parse,
            AppConfig.HY2 to Hysteria2Fmt::parse,
        )
    }

    /**
     * Shares the configuration to the clipboard.
     *
     * @param context The context.
     * @param guid The GUID of the configuration.
     * @return The result code.
     */
    fun share2Clipboard(context: Context, guid: String): Int {
        try {
            val conf = shareConfig(guid)
            if (TextUtils.isEmpty(conf)) {
                return -1
            }

            Utils.setClipboard(context, conf)

        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to share config to clipboard", e)
            return -1
        }
        return 0
    }

    /**
     * Shares non-custom configurations to the clipboard.
     *
     * @param context The context.
     * @param serverList The list of server GUIDs.
     * @return The number of configurations shared.
     */
    fun shareNonCustomConfigsToClipboard(context: Context, serverList: List<String>): Int {
        try {
            val sb = StringBuilder()
            for (guid in serverList) {
                val url = shareConfig(guid)
                if (TextUtils.isEmpty(url)) {
                    continue
                }
                sb.append(url)
                sb.appendLine()
            }
            if (sb.count() > 0) {
                Utils.setClipboard(context, sb.toString())
            }
            return sb.lines().count() - 1
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to share non-custom configs to clipboard", e)
            return -1
        }
    }

    /**
     * Shares the configuration as a QR code.
     *
     * @param guid The GUID of the configuration.
     * @return The QR code bitmap.
     */
    fun share2QRCode(guid: String): Bitmap? {
        try {
            val conf = shareConfig(guid)
            if (TextUtils.isEmpty(conf)) {
                return null
            }
            return QRCodeDecoder.createQRCode(conf)

        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to share config as QR code", e)
            return null
        }
    }

    /**
     * Shares the full content of the configuration to the clipboard.
     *
     * @param context The context.
     * @param guid The GUID of the configuration.
     * @return The result code.
     */
    fun shareFullContent2Clipboard(context: Context, guid: String?): Int {
        try {
            if (guid == null) return -1
            val result = CoreConfigManager.getV2rayConfig(context, guid)
            if (result.status) {
                Utils.setClipboard(context, result.content)
            } else {
                return -1
            }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to share full content to clipboard", e)
            return -1
        }
        return 0
    }

    /**
     * Shares the configuration.
     *
     * @param guid The GUID of the configuration.
     * @return The configuration string.
     */
    private fun shareConfig(guid: String): String {
        try {
            val config = MmkvManager.decodeServerConfig(guid) ?: return ""

            return config.configType.protocolScheme + when (config.configType) {
                EConfigType.VMESS -> VmessFmt.toUri(config)
                EConfigType.SHADOWSOCKS -> ShadowsocksFmt.toUri(config)
                EConfigType.SOCKS -> SocksFmt.toUri(config)
                EConfigType.VLESS -> VlessFmt.toUri(config)
                EConfigType.TROJAN -> TrojanFmt.toUri(config)
                EConfigType.WIREGUARD -> WireguardFmt.toUri(config)
                EConfigType.HYSTERIA2 -> Hysteria2Fmt.toUri(config)
                else -> {}
            }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to share config for GUID: $guid", e)
            return ""
        }
    }

    /**
     * Imports a batch of configurations.
     *
     * @param server The server string.
     * @param subid The subscription ID.
     * @param append Whether to append the configurations.
     * @return A pair containing the number of configurations and subscriptions imported.
     */
    fun importBatchConfig(server: String?, subid: String, append: Boolean): Pair<Int, Int> {
        val result = importBatchConfigDetailed(server, subid, append)
        return result.directConfigCount to result.subscriptionCount
    }

    /**
     * Imports configurations or subscription URLs atomically and returns a structured result
     * including matched subscription ID, fetched server count, and specific failure reason.
     */
    fun importBatchConfigDetailed(server: String?, subid: String, append: Boolean): BatchImportResult {
        val trimmed = server?.trim().orEmpty()
        if (trimmed.isEmpty()) {
            return BatchImportResult(error = SubscriptionImportError.EMPTY_INPUT)
        }

        return try {
            var directCount = parseBatchConfig(Utils.decode(trimmed), subid, append)
            if (directCount <= 0) {
                directCount = parseBatchConfig(trimmed, subid, append)
            }
            if (directCount <= 0) {
                directCount = parseCustomConfigServer(trimmed, subid, append)
            }
            if (directCount <= 0 && trimmed.contains("<")) {
                val extracted = extractProxyUrisFromHtml(trimmed)
                if (extracted.isNotEmpty()) {
                    directCount = parseBatchConfig(extracted.joinToString("\n"), subid, append)
                }
            }

            var subParse = parseBatchSubscriptionDetailed(trimmed)
            if (subParse.subscriptions.isEmpty() && !subParse.sawBlockedHttpUrl) {
                val decoded = Utils.decode(trimmed)
                if (decoded.isNotBlank()) {
                    subParse = parseBatchSubscriptionDetailed(decoded)
                }
            }

            if (subParse.subscriptions.isNotEmpty()) {
                var combinedUpdate = SubscriptionUpdateResult()
                var lastSubId: String? = null
                subParse.subscriptions.forEach { entry ->
                    lastSubId = entry.guid
                    val subItem = MmkvManager.decodeSubscription(entry.guid)
                    if (subItem != null) {
                        combinedUpdate += updateConfigViaSub(SubscriptionCache(entry.guid, subItem))
                    }
                }
                val allExisted = subParse.subscriptions.all { it.existed }

                if (combinedUpdate.configCount > 0 && lastSubId != null) {
                    ensureSelectedServerExists(lastSubId)
                    return BatchImportResult(
                        directConfigCount = directCount,
                        subscriptionCount = subParse.subscriptions.size,
                        subscriptionConfigCount = combinedUpdate.configCount,
                        matchedSubscriptionId = lastSubId,
                        refreshedExisting = allExisted,
                        error = SubscriptionImportError.NONE
                    )
                }

                if (directCount > 0) {
                    return BatchImportResult(
                        directConfigCount = directCount,
                        subscriptionCount = subParse.subscriptions.size,
                        subscriptionConfigCount = combinedUpdate.configCount,
                        matchedSubscriptionId = lastSubId,
                        refreshedExisting = allExisted,
                        error = SubscriptionImportError.NONE
                    )
                }

                val resolvedError = combinedUpdate.lastError.takeIf { it != SubscriptionImportError.NONE }
                    ?: SubscriptionImportError.FETCH_FAILED
                return BatchImportResult(
                    directConfigCount = 0,
                    subscriptionCount = subParse.subscriptions.size,
                    subscriptionConfigCount = 0,
                    matchedSubscriptionId = lastSubId,
                    refreshedExisting = allExisted,
                    error = resolvedError
                )
            }

            if (directCount > 0) {
                ensureSelectedServerExists(subid)
                return BatchImportResult(
                    directConfigCount = directCount,
                    error = SubscriptionImportError.NONE
                )
            }

            BatchImportResult(
                error = if (subParse.sawBlockedHttpUrl) {
                    SubscriptionImportError.HTTP_NOT_ALLOWED
                } else {
                    SubscriptionImportError.INVALID_INPUT
                }
            )
        } catch (e: ProfileStorageException) {
            LogUtil.e(AppConfig.TAG, "Failed to store imported profiles", e)
            BatchImportResult(error = SubscriptionImportError.STORAGE_ERROR)
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to import batch config", e)
            BatchImportResult(error = SubscriptionImportError.INVALID_INPUT)
        }
    }

    private fun ensureSelectedServerExists(preferredGroupId: String) {
        val currentGuid = MmkvManager.getSelectServer()
        if (!currentGuid.isNullOrBlank() && MmkvManager.decodeServerConfig(currentGuid) != null) {
            return
        }
        val firstGuid = MmkvManager.decodeServerList(preferredGroupId).firstOrNull()
            ?: MmkvManager.decodeAllServerList().firstOrNull()
        if (!firstGuid.isNullOrBlank()) {
            MmkvManager.setSelectServer(firstGuid)
        }
    }

    /**
     * Parses a batch of subscriptions, matching existing URLs for in-place refresh
     * and flagging plain HTTP URLs when the HTTP subscription toggle is disabled.
     */
    private fun parseBatchSubscriptionDetailed(servers: String?): SubscriptionBatchParseResult {
        if (servers.isNullOrBlank()) {
            return SubscriptionBatchParseResult(emptyList(), false)
        }

        val allowHttp = MmkvManager.decodeSettingsBool(AppConfig.PREF_ALLOW_HTTP_SUBSCRIPTION, false)
        val matched = mutableListOf<SubscriptionUpsertResult>()
        var sawBlockedHttp = false

        try {
            servers.lines()
                .map { it.trim().removePrefix("\uFEFF") }
                .filter { it.isNotEmpty() }
                .distinct()
                .forEach { str ->
                    when {
                        Utils.isValidSubUrl(str, allowHttp = allowHttp) -> {
                            matched += upsertSubscriptionForUrl(str, allowHttp)
                        }

                        !allowHttp && Utils.isHttpSubscriptionUrl(str) -> {
                            sawBlockedHttp = true
                        }
                    }
                }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to parse batch subscription", e)
        }
        return SubscriptionBatchParseResult(matched, sawBlockedHttp)
    }

    /**
     * Parses a batch of configurations.
     *
     * @param servers The servers string.
     * @param subid The subscription ID.
     * @param append Whether to append the configurations.
     * @return The number of configurations parsed.
     */
    private fun parseBatchConfig(servers: String?, subid: String, append: Boolean): Int {
        try {
            if (servers == null) {
                return 0
            }
            val subItem = MmkvManager.decodeSubscription(subid)

            // Parse all configs first (no I/O during parsing)
            val configs = mutableListOf<ProfileItem>()
            val v2raynLines = mutableListOf<String>()

            servers.lines()
                .map { it.trim().removePrefix("\uFEFF") }
                .filter { it.isNotEmpty() }
                .distinct()
                .reversed()
                .forEach { line ->
                    if (line.startsWith(AppConfig.V2RAYNFMTS, ignoreCase = true)) {
                        v2raynLines.add(line)
                    } else {
                        val config = parseConfig(line, subid, subItem)
                        if (config != null) {
                            configs.add(config)
                        }
                    }
                }

            val v2raynConfigs = V2rayNFmt.parse(v2raynLines, subid)
            val allConfigs = v2raynConfigs + configs

            if (allConfigs.isNotEmpty()) {
                commitProfiles(
                    configs = allConfigs.map(::ParsedProfile),
                    subid = subid,
                    append = append,
                )
            }

            return allConfigs.size
        } catch (e: ProfileStorageException) {
            throw e
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to parse batch config", e)
        }
        return 0
    }

    /**
     * Commits parsed profiles before removing the profiles they replace.
     *
     * @param configs The parsed profiles to save.
     * @param subid The subscription ID.
     * @param append Whether to append to the existing server list.
     */
    private fun commitProfiles(
        configs: List<ParsedProfile>,
        subid: String,
        append: Boolean,
    ) {
        val keyToProfile = linkedMapOf<String, ProfileItem>()
        val rawConfigs = mutableMapOf<String, String>()

        configs.forEach { parsed ->
            val key = Utils.getUuid()
            keyToProfile[key] = parsed.profile
            parsed.rawConfig?.let { raw -> rawConfigs[key] = raw }
        }

        MmkvManager.saveServerProfiles(
            profiles = keyToProfile,
            rawConfigs = rawConfigs,
            subscriptionId = subid,
            append = append,
        )
    }

    /**
     * Parses a custom configuration server.
     *
     * @param server The server string.
     * @param subid The subscription ID.
     * @param append Whether to append the configurations.
     * @return The number of configurations parsed.
     */
    private fun parseCustomConfigServer(server: String?, subid: String, append: Boolean): Int {
        if (server == null) {
            return 0
        }
        if (server.contains("inbounds")
            && server.contains("outbounds")
            && server.contains("routing")
        ) {
            try {
                val serverList: Array<Any> =
                    JsonUtil.fromJson(server, Array<Any>::class.java) ?: arrayOf()

                if (serverList.isNotEmpty()) {
                    val configs = serverList.reversed().map { srv ->
                        val config = CustomFmt.parse(JsonUtil.toJson(srv))
                        config.subscriptionId = subid
                        config.description = generateDescription(config)
                        ParsedProfile(
                            profile = config,
                            rawConfig = JsonUtil.toJsonPretty(srv) ?: "",
                        )
                    }
                    commitProfiles(configs, subid, append)
                    return configs.size
                }
            } catch (e: ProfileStorageException) {
                throw e
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to parse custom config server JSON array", e)
            }

            try {
                // For compatibility
                val config = CustomFmt.parse(server)
                config.subscriptionId = subid
                config.description = generateDescription(config)
                commitProfiles(
                    configs = listOf(ParsedProfile(config, server)),
                    subid = subid,
                    append = append,
                )
                return 1
            } catch (e: ProfileStorageException) {
                throw e
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to parse custom config server as single config", e)
            }
            return 0
        } else if (server.startsWith("[Interface]") && server.contains("[Peer]")) {
            try {
                val config = WireguardFmt.parseWireguardConfFile(server)
                config.subscriptionId = subid
                config.description = generateDescription(config)
                commitProfiles(
                    configs = listOf(ParsedProfile(config, server)),
                    subid = subid,
                    append = append,
                )
                return 1
            } catch (e: ProfileStorageException) {
                throw e
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to parse WireGuard config file", e)
            }
            return 0
        } else {
            return 0
        }
    }

    /**
     * Parses the configuration from a QR code or string.
     * Only parses and returns ProfileItem, does not save.
     *
     * @param str The configuration string.
     * @param subid The subscription ID.
     * @param subItem The subscription item.
     * @return The parsed ProfileItem or null if parsing fails or filtered out.
     */
    private fun parseConfig(
        str: String?,
        subid: String,
        subItem: SubscriptionItem?
    ): ProfileItem? {
        try {
            if (str == null || TextUtils.isEmpty(str)) {
                return null
            }

            val normalized = str.trim()
            val config = configFmtParsers.firstNotNullOfOrNull { (scheme, parser) ->
                if (normalized.startsWith(scheme, ignoreCase = true)) {
                    val canonical = scheme + normalized.substring(scheme.length)
                    parser(canonical)
                } else {
                    null
                }
            }

            if (config == null) {
                return null
            }

            // Apply filter
            if (subItem?.filter.isNotNullEmpty() && config.remarks.isNotNullEmpty()) {
                val matched = Regex(pattern = subItem?.filter.orEmpty())
                    .containsMatchIn(input = config.remarks)
                if (!matched) return null
            }

            config.subscriptionId = subid
            config.description = generateDescription(config)

            return config
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to parse config", e)
            return null
        }
    }

    /**
     * Updates the configuration via all subscriptions.
     *
     * @return Detailed result of the subscription update operation.
     */
    fun updateConfigViaSubAll(): SubscriptionUpdateResult {
        return try {
            val subscriptions = MmkvManager.decodeSubscriptions()
            subscriptions.fold(SubscriptionUpdateResult()) { acc, subscription ->
                acc + updateConfigViaSub(subscription)
            }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to update config via all subscriptions", e)
            SubscriptionUpdateResult()
        }
    }

    /**
     * Updates the configuration via a subscription.
     *
     * @param it The subscription item.
     * @return Subscription update result.
     */
    fun updateConfigViaSub(it: SubscriptionCache): SubscriptionUpdateResult {
        try {
            // Check if disabled
            if (!it.subscription.enabled) {
                return SubscriptionUpdateResult(skipCount = 1)
            }

            // Validate subscription info
            val rawUrl = it.subscription.url.trim()
            if (TextUtils.isEmpty(it.guid) || rawUrl.isEmpty()) {
                return SubscriptionUpdateResult(skipCount = 1)
            }
            if (it.subscription.remarks.isBlank()) {
                it.subscription.remarks = deriveSubscriptionRemarks(rawUrl)
            }

            val url = HttpUtil.toIdnUrl(rawUrl)
            val allowHttp = it.subscription.allowInsecureUrl ||
                MmkvManager.decodeSettingsBool(AppConfig.PREF_ALLOW_HTTP_SUBSCRIPTION, false)
            if (!Utils.isValidSubUrl(url, allowHttp = allowHttp)) {
                val err = if (Utils.isHttpSubscriptionUrl(url)) {
                    SubscriptionImportError.HTTP_NOT_ALLOWED
                } else {
                    SubscriptionImportError.INVALID_INPUT
                }
                return SubscriptionUpdateResult(failureCount = 1, lastError = err)
            }
            LogUtil.i(AppConfig.TAG, "Update subscription: starting sub=${it.guid}")
            val userAgent = it.subscription.userAgent
            val requestHeaders = it.subscription.requestHeaders
            val proxyUsername = SettingsManager.getSocksUsername()
            val proxyPassword = SettingsManager.getSocksPassword()

            val httpPort = SettingsManager.getHttpPort()
            var configText = if (HttpUtil.isLocalProxyAvailable(httpPort)) {
                try {
                    HttpUtil.getUrlContentWithUserAgent(
                        UrlContentRequest(
                            url = url,
                            userAgent = userAgent,
                            requestHeaders = requestHeaders,
                            timeout = 15000,
                            httpPort = httpPort,
                            proxyUsername = proxyUsername,
                            proxyPassword = proxyPassword
                        )
                    )
                } catch (e: Exception) {
                    LogUtil.w(
                        AppConfig.TAG,
                        "Update subscription: proxy request failed for sub=${it.guid}, falling back to direct",
                        e
                    )
                    ""
                }
            } else {
                LogUtil.i(
                    AppConfig.TAG,
                    "Update subscription: local proxy unavailable on port=$httpPort for sub=${it.guid}, using direct connection"
                )
                ""
            }
            if (configText.isEmpty()) {
                configText = try {
                    HttpUtil.getUrlContentWithUserAgent(
                        UrlContentRequest(
                            url = url,
                            userAgent = userAgent,
                            requestHeaders = requestHeaders
                        )
                    )
                } catch (e: Exception) {
                    LogUtil.e(
                        AppConfig.TAG,
                        "Update subscription: direct fetch failed for sub=${it.guid}",
                        e
                    )
                    ""
                }
            }
            if (configText.isEmpty()) {
                return SubscriptionUpdateResult(
                    failureCount = 1,
                    lastError = SubscriptionImportError.FETCH_FAILED
                )
            }

            val count = parseConfigViaSub(configText, it.guid, false)
            if (count > 0) {
                it.subscription.lastUpdated = System.currentTimeMillis()
                MmkvManager.encodeSubscription(it.guid, it.subscription)
                LogUtil.i(AppConfig.TAG, "Subscription updated: sub=${it.guid}, count=$count")
                return SubscriptionUpdateResult(
                    configCount = count,
                    successCount = 1
                )
            } else {
                // Got response but no valid configs parsed
                LogUtil.w(AppConfig.TAG, "Update subscription: no valid configs parsed for sub=${it.guid}")
                return SubscriptionUpdateResult(
                    failureCount = 1,
                    lastError = SubscriptionImportError.NO_CONFIGS_FOUND
                )
            }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to update config via subscription sub=${it.guid}", e)
            return SubscriptionUpdateResult(
                failureCount = 1,
                lastError = SubscriptionImportError.FETCH_FAILED
            )
        }
    }

    /**
     * Removes invalid server configurations for a subscription.
     *
     * @param subId The subscription ID.
     */
    fun removeInvalidServer(subId: String) {
        val serverList = MmkvManager.decodeServerList(subId)
        val invalidServers = serverList.filter {
            val aff = MmkvManager.decodeServerAffiliationInfo(it)
            aff != null && aff.testDelayMillis < 0L
        }
        MmkvManager.removeServers(invalidServers, subId)
    }

    /**
     * Sorts servers by test results for a subscription.
     *
     * @param subId The subscription ID.
     */
    fun sortByTestResultsForSub(subId: String) {
        val serverList = MmkvManager.decodeServerList(subId)
        if (serverList.isEmpty()) return

        val sorted = serverList
            .map { guid ->
                val delay =
                    MmkvManager.decodeServerAffiliationInfo(guid)?.testDelayMillis ?: 0L
                guid to if (delay <= 0L) Long.MAX_VALUE else delay
            }
            .sortedBy { it.second }
            .map { it.first }
            .toMutableList()
        MmkvManager.encodeServerList(sorted, subId)
    }

    /**
     * Parses the configuration via a subscription.
     *
     * @param server The server string.
     * @param subid The subscription ID.
     * @param append Whether to append the configurations.
     * @return The number of configurations parsed.
     */
    private fun parseConfigViaSub(server: String?, subid: String, append: Boolean): Int {
        var count = parseBatchConfig(Utils.decode(server), subid, append)
        if (count <= 0) {
            count = parseBatchConfig(server, subid, append)
        }
        if (count <= 0) {
            count = parseCustomConfigServer(server, subid, append)
        }
        if (count <= 0 && !server.isNullOrBlank()) {
            val extractedUris = extractProxyUrisFromHtml(server)
            if (extractedUris.isNotEmpty()) {
                count = parseBatchConfig(extractedUris.joinToString("\n"), subid, append)
            }
        }
        return count
    }

    /**
     * Safely extracts embedded proxy configuration URIs from raw HTML subscription portal pages
     * using plain-text scanning and HTML entity unescaping (no WebView or script execution).
     */
    internal fun extractProxyUrisFromHtml(rawContent: String?): List<String> {
        if (rawContent.isNullOrBlank()) return emptyList()
        return embeddedProxyUriRegex.findAll(rawContent)
            .map { match -> unescapeBasicHtmlEntities(match.value).trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .toList()
    }

    private fun unescapeBasicHtmlEntities(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&#38;", "&")
            .replace("&#x26;", "&", ignoreCase = true)
            .replace("&quot;", "\"")
            .replace("&#34;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
    }

    /**
     * Derives a non-blank display label for a subscription URL from its fragment or hostname.
     */
    internal fun deriveSubscriptionRemarks(url: String): String {
        return runCatching {
            val uri = URI(Utils.fixIllegalUrl(url.trim()))
            val fragment = uri.rawFragment?.let { Utils.decodeURIComponent(it) }?.trim()
            when {
                !fragment.isNullOrEmpty() -> fragment
                !uri.host.isNullOrBlank() -> uri.host
                else -> "KUMO"
            }
        }.getOrDefault("KUMO")
    }

    /**
     * Inserts a new subscription URL or returns the existing matching subscription for in-place refresh.
     */
    private fun upsertSubscriptionForUrl(url: String, allowHttp: Boolean): SubscriptionUpsertResult {
        val normalizedUrl = url.trim()
        val subscriptions = MmkvManager.decodeSubscriptions()
        val existing = subscriptions.firstOrNull { it.subscription.url.trim() == normalizedUrl }
        if (existing != null) {
            existing.subscription.enabled = true
            if (existing.subscription.remarks.isBlank()) {
                existing.subscription.remarks = deriveSubscriptionRemarks(normalizedUrl)
            }
            if (allowHttp && Utils.isHttpSubscriptionUrl(normalizedUrl)) {
                existing.subscription.allowInsecureUrl = true
            }
            MmkvManager.encodeSubscription(existing.guid, existing.subscription)
            return SubscriptionUpsertResult(guid = existing.guid, existed = true)
        }

        val subItem = SubscriptionItem().apply {
            remarks = deriveSubscriptionRemarks(normalizedUrl)
            this.url = normalizedUrl
            enabled = true
            allowInsecureUrl = allowHttp && Utils.isHttpSubscriptionUrl(normalizedUrl)
        }
        val newGuid = Utils.getUuid()
        MmkvManager.encodeSubscription(newGuid, subItem)
        return SubscriptionUpsertResult(guid = newGuid, existed = false)
    }

    /** Generates a description for the profile.
     *
     * @param profile The profile item.
     * @return The generated description.
     */
    fun generateDescription(profile: ProfileItem): String {
        // Hide xxx:xxx:***/xxx.xxx.xxx.***
        val server = profile.server
        val port = profile.serverPort
        if (server.isNullOrBlank() && port.isNullOrBlank()) return ""

        val addrPart = server?.let {
            if (it.contains(":"))
                it.split(":").take(2).joinToString(":", postfix = ":***")
            else
                it.split('.').dropLast(1).joinToString(".", postfix = ".***")
        } ?: ""

        return "$addrPart : ${port ?: ""}"
    }
}
