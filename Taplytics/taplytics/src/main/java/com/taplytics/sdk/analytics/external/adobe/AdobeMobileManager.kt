package com.taplytics.sdk.analytics.external.adobe

import com.taplytics.sdk.analytics.external.TLExecutor
import com.taplytics.sdk.analytics.external.TLExternalAnalyticsManager
import com.taplytics.sdk.utils.TLLog
import java.util.concurrent.ExecutorService

internal object AdobeMobileManager : AdobeSDK {

    private enum class Sdk(val className: String) {
        ANALYTICS("com.adobe.mobile.Analytics"),
        STATIC_METHODS("com.adobe.mobile.StaticMethods");
    }

    val isAvailable: Boolean by lazy {
        try {
            Class.forName(Sdk.ANALYTICS.className)
            true
        } catch (e: Exception) {
            //no adobe
            false
        }
    }

    /**
     * Logs the running experiments and variations to adobe under the event "TL_experiments"
     *
     * @param data the experiments to be logged to adobe
     */
    override fun log(formattedData: Map<String, String>) {
        try {
            //get the track action method
            if (getConfigContext() == null) {
                TLLog.debug("No adobe context found.")
                return
            }

            val adobe = Class.forName(Sdk.ANALYTICS.className)
            val m = adobe.getMethod("trackAction", String::class.java, MutableMap::class.java)

            //logs the event
            m.invoke(adobe, TLExternalAnalyticsManager.TLExperiments, formattedData)
            TLLog.debug("Logged experiment data to Adobe: $formattedData", true)
        } catch (th: Throwable) {
            TLLog.debug("Logging experiment data to Adobe failed: ${th.message ?: ""}", true)
        }
    }

    /**
     * Gets the shared context from Adobe SDK as calling an action before setting a context will fail the SQLite database
     */
    fun getConfigContext(): Any? {
        return try {
            val adobe = Class.forName(Sdk.STATIC_METHODS.className)
            with(adobe.getDeclaredMethod("getSharedContext")) {
                isAccessible = true
                invoke(adobe)
            }
        } catch (th: Throwable) {
            TLLog.debug("No context set yet")
            null
        }
    }

    /**
     * Forces Adobe to create the executor so we can replace it with our own
     */
    override fun setExecutor() {
        try {
            Class.forName(Sdk.STATIC_METHODS.className)?.let { clazz ->
                val m = clazz.getDeclaredMethod("getAnalyticsExecutor")
                m.isAccessible = true

                //grab the old one
                val adobeExecutor = m.invoke(clazz) as ExecutorService
                m.isAccessible = false

                with(clazz.getDeclaredField("analyticsExecutor")) {
                    isAccessible = true
                    //replace it
                    this[clazz] = TLExecutor(adobeExecutor)
                    isAccessible = false
                }

                TLLog.debug("Adobe Mobile connected")
            }
        } catch (th: Throwable) {
            TLLog.debug("Something went wrong when replacing adobe executor")
        }
    }

}