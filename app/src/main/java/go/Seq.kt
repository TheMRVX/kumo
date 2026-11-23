package go

import android.content.Context

/**
 * Lightweight compatibility shim for Go mobile runtime (`go.Seq`) when the prebuilt
 * `libv2ray.aar` native artifact is not bundled in this build environment.
 */
object Seq {
    @Volatile
    private var appContext: Context? = null

    @JvmStatic
    fun setContext(ctx: Context?) {
        appContext = ctx?.applicationContext
    }

    @JvmStatic
    fun getContext(): Context? = appContext
}
