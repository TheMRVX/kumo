package libv2ray

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Lightweight in-memory compatibility stubs for `libv2ray` (from AndroidLibXrayLite)
 * so KUMO VPN compiles and runs cleanly in environments without the prebuilt Go AAR.
 */
interface CoreCallbackHandler {
    fun startup(): Long
    fun shutdown(): Long
    fun onEmitStatus(l: Long, s: String?): Long
}

interface ProcessFinder {
    fun findProcessByConnection(
        network: String,
        srcIP: String,
        srcPort: Long,
        destIP: String,
        destPort: Long
    ): Long
}

class CoreController(private val callback: CoreCallbackHandler?) {
    private val running = AtomicBoolean(false)

    @Volatile
    private var processFinder: ProcessFinder? = null

    val isRunning: Boolean
        get() = running.get()

    fun registerProcessFinder(finder: ProcessFinder?) {
        processFinder = finder
    }

    @Throws(Exception::class)
    fun startLoop(configContent: String, tunFd: Int) {
        running.set(true)
        callback?.startup()
        callback?.onEmitStatus(0L, "started")
    }

    @Throws(Exception::class)
    fun stopLoop() {
        running.set(false)
        callback?.shutdown()
        callback?.onEmitStatus(0L, "stopped")
    }

    fun queryAllOutboundTrafficStats(): String = ""

    @Throws(Exception::class)
    fun measureDelay(url: String): Long = if (running.get()) 42L else -1L
}

object Libv2ray {
    @JvmStatic
    fun initCoreEnv(assetPath: String, deviceId: String) {
        // No-op in stub mode
    }

    @JvmStatic
    fun reconcileBrowserDialer(dialerAddr: String) {
        // No-op in stub mode
    }

    @JvmStatic
    fun checkVersionX(): String = "Xray-stub"

    @JvmStatic
    fun measureOutboundDelay(config: String, testUrl: String): Long = -1L

    @JvmStatic
    fun newCoreController(handler: CoreCallbackHandler): CoreController = CoreController(handler)

    @JvmStatic
    fun fetchTlsCertSha256(requestJson: String): String =
        """{"sha256":"","error":"libv2ray native binary unavailable"}"""

    @JvmStatic
    fun fetchQuicCertSha256(requestJson: String): String =
        """{"sha256":"","error":"libv2ray native binary unavailable"}"""
}
