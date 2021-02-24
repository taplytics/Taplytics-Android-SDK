package com.taplytics.sdk.analytics.external.adobe

interface AdobeSDK {

    /**
     * Logs the running experiments and variations to adobe under the event "TL_experiments"
     *
     * @param formattedData the formatted experiments to be logged to adobe
     */
    fun log(formattedData: Map<String, String>)

    /**
     * Forces Adobe to create the executor so we can replace it with our own
     */
    fun setExecutor()

}