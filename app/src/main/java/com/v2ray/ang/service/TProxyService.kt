package com.v2ray.ang.service

import android.content.Context
import android.os.ParcelFileDescriptor
import com.v2ray.ang.AppConfig
import com.v2ray.ang.contracts.Tun2SocksControl
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.util.LogUtil
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages the tun2socks process that handles VPN traffic
 */
class TProxyService(
    private val context: Context,
    private val vpnInterface: ParcelFileDescriptor,
    private val isRunningProvider: () -> Boolean,
    private val restartCallback: () -> Unit
) : Tun2SocksControl {
    private val isTunnelStarted = AtomicBoolean(false)

    companion object {
        private const val NATIVE_LIB_NAME = "hev-socks5-tunnel"

        @JvmStatic
        @Suppress("FunctionName")
        private external fun TProxyStartService(configPath: String, fd: Int): Boolean

        @JvmStatic
        @Suppress("FunctionName")
        private external fun TProxyStopService(): Boolean

        @JvmStatic
        @Suppress("FunctionName")
        private external fun TProxyIsRunning(): Boolean

        @JvmStatic
        @Suppress("FunctionName")
        private external fun TProxyGetStats(): LongArray?

        internal fun loadNativeLibrary(
            loader: (String) -> Unit = System::loadLibrary,
            onLoadFailure: (UnsatisfiedLinkError) -> Unit = { error ->
                LogUtil.w(
                    AppConfig.TAG,
                    "StartCore-VPN: tun2socks init — native library lib$NATIVE_LIB_NAME.so unavailable; skipping JNI tunnel",
                    error
                )
            }
        ): Boolean {
            return try {
                loader(NATIVE_LIB_NAME)
                true
            } catch (e: UnsatisfiedLinkError) {
                onLoadFailure(e)
                false
            }
        }

        internal val isNativeLibraryLoaded: Boolean by lazy { loadNativeLibrary() }
    }

    /**
     * Starts the tun2socks process with the appropriate parameters.
     */
    override fun startTun2Socks() {
        val configContent = buildConfig()
        val configFile = File(context.filesDir, "hev-socks5-tunnel.yaml").apply {
            writeText(configContent)
        }
        LogUtil.d(AppConfig.TAG, "StartCore-VPN: tun2socks start — config written to ${configFile.name}")

        if (!isNativeLibraryLoaded) {
            LogUtil.w(
                AppConfig.TAG,
                "StartCore-VPN: tun2socks start — lib$NATIVE_LIB_NAME.so not loaded; skipping TProxyStartService"
            )
            return
        }

        try {
            TProxyStartService(configFile.absolutePath, vpnInterface.fd)
            isTunnelStarted.set(true)
        } catch (e: UnsatisfiedLinkError) {
            LogUtil.e(AppConfig.TAG, "StartCore-VPN: tun2socks start — TProxyStartService link failure", e)
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "StartCore-VPN: tun2socks start — TProxyStartService failed", e)
        }
    }

    private fun buildConfig(): String {
        val socksPort = SettingsManager.getSocksPort()
        val socksUsername = SettingsManager.getSocksUsername()
        val socksPassword = SettingsManager.getSocksPassword()
        val vpnConfig = SettingsManager.getCurrentVpnInterfaceAddressConfig()
        val escapedSocksUsername = socksUsername?.replace("'", "''")
        val escapedSocksPassword = socksPassword?.replace("'", "''")
        return buildString {
            appendLine("tunnel:")
            appendLine("  mtu: ${SettingsManager.getVpnMtu()}")
            appendLine("  ipv4: ${vpnConfig.ipv4Client}")

            if (MmkvManager.decodeSettingsBool(AppConfig.PREF_IPV6_ENABLED)) {
                appendLine("  ipv6: '${vpnConfig.ipv6Client}'")
            }

            appendLine("socks5:")
            appendLine("  port: ${socksPort}")
            appendLine("  address: ${AppConfig.LOOPBACK}")
            appendLine("  udp: 'udp'")
            if (escapedSocksUsername != null && escapedSocksPassword != null) {
                appendLine("  username: '${escapedSocksUsername}'")
                appendLine("  password: '${escapedSocksPassword}'")
            }

            // Read-write timeout settings
            val timeoutSetting = MmkvManager.decodeSettingsString(AppConfig.PREF_HEV_TUNNEL_RW_TIMEOUT) ?: AppConfig.HEVTUN_RW_TIMEOUT
            val parts = timeoutSetting.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            val tcpTimeout = parts.getOrNull(0)?.toIntOrNull() ?: 300
            val udpTimeout = parts.getOrNull(1)?.toIntOrNull() ?: 60

            appendLine("misc:")
            appendLine("  tcp-read-write-timeout: ${tcpTimeout * 1000}")
            appendLine("  udp-read-write-timeout: ${udpTimeout * 1000}")
            appendLine("  log-level: ${MmkvManager.decodeSettingsString(AppConfig.PREF_HEV_TUNNEL_LOGLEVEL, AppConfig.DEFAULT_HEV_TUNNEL_LOGLEVEL)}")
        }
    }

    /**
     * Stops the tun2socks process
     */
    override fun stopTun2Socks() {
        if (!isNativeLibraryLoaded || !isTunnelStarted.compareAndSet(true, false)) {
            return
        }
        try {
            LogUtil.i(AppConfig.TAG, "StartCore-VPN: tun2socks stop — TProxyStopService")
            TProxyStopService()
        } catch (e: UnsatisfiedLinkError) {
            LogUtil.e(AppConfig.TAG, "StartCore-VPN: tun2socks stop — TProxyStopService link failure", e)
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "StartCore-VPN: tun2socks stop — failed to stop hev-socks5-tunnel", e)
        }
    }
}
