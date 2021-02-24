package com.taplytics.sdk.analytics.external.adobe

import com.taplytics.sdk.analytics.external.TLExecutor
import com.taplytics.sdk.analytics.external.TLExternalAnalyticsManager
import com.taplytics.sdk.utils.TLLog
import java.util.concurrent.ExecutorService

internal object AdobeExpManager : AdobeSDK {

    private enum class Sdk(val className: String) {
        CORE("com.adobe.marketing.mobile.MobileCore");
    }

    val isAvailable: Boolean by lazy {
        try {
            Class.forName(Sdk.CORE.className)
            true
        } catch (e: Exception) {
            //no adobe
            false
        }
    }

    override fun log(formattedData: Map<String, String>) {
        try {
            val adobe = Class.forName(Sdk.CORE.className)
            val m = adobe.getMethod("trackAction", String::class.java, MutableMap::class.java)

            //logs the event
            m.invoke(adobe, TLExternalAnalyticsManager.TLExperiments, formattedData)
            TLLog.debug("Logged experiment data to Adobe Experience: $formattedData", true)
        } catch (th: Throwable) {
            TLLog.debug("Logging experiment data to Adobe Experience failed: ${th.message ?: ""}", true)
        }
    }

    /**
     * Forces Adobe to create the executor so we can replace it with our own
     */
    override fun setExecutor() {
        try {
            val mobileCore = Class.forName(Sdk.CORE.className)
            val getCore = mobileCore.getDeclaredMethod("getCore")

            getCore.isAccessible = true

            val core = getCore.invoke(mobileCore)
            val eventHubField = core.javaClass.getDeclaredField("eventHub")
            eventHubField.isAccessible = true

            val eventHub = eventHubField[core]

            val eventHubThreadServiceField = eventHub.javaClass.getDeclaredField("eventHubThreadService")
            eventHubThreadServiceField.isAccessible = true

            val executorService = eventHubThreadServiceField[eventHub] as ExecutorService
            val tlExecutor = TLExecutor(executorService)
            eventHubThreadServiceField.set(eventHub, tlExecutor)
            eventHubThreadServiceField.isAccessible = false

            TLLog.debug("Adobe Exp connected")
        } catch (th: Throwable) {
            TLLog.debug("Something went wrong when replacing adobe executor")
        }
    }

}
