package com.taplytics.sdk.analytics.external.adobe

class AdobeManager {

    companion object {
        private const val KEY_TL_EXP = "tl_exp"
    }

    enum class Format {
        ORIGINAL,
        BASELINE_A,
        TL_EXP_1;

        fun apply(data: Map<String, String>?): Map<String, String> {
            if (data == null) {
                return mapOf()
            }

            val formatted = hashMapOf<String, String>()
            when (this) {
                ORIGINAL -> {
                    // No formatting needed
                    return data
                }
                BASELINE_A -> {
                    if (data.isEmpty()) {
                        formatted[KEY_TL_EXP] = ""
                        return formatted
                    }

                    val dataAsStr = StringBuilder()
                    data.forEach { entry ->
                        val key = entry.key
                        val value = entry.value

                        if (dataAsStr.isNotEmpty()) {
                            dataAsStr.append(", ")
                        }

                        val valueString = if (value === "baseline") "A" else value
                        dataAsStr.append(key)
                                .append(":")
                                .append(valueString)
                    }

                    formatted[KEY_TL_EXP] = dataAsStr.toString()
                    return formatted
                }
                TL_EXP_1 -> {
                    var count = 1
                    data.forEach { entry ->
                        val key = entry.key
                        val value = entry.value

                        formatted["experiment_$count"] = "$key | $value"
                        count++
                    }
                    formatted[KEY_TL_EXP] = "1"
                    return formatted
                }
            }
        }
    }

    var isEnabled = false
    var format = Format.ORIGINAL
    private val adobeSDKs: List<AdobeSDK> = mutableListOf()

    fun appHasAdobe(): Boolean {
        if (AdobeMobileManager.isAvailable) {
            (adobeSDKs as MutableList).add(AdobeMobileManager)
        }

        if (AdobeExpManager.isAvailable) {
            (adobeSDKs as MutableList).add(AdobeExpManager)
        }

        return adobeSDKs.isNotEmpty()
    }

    /**
     * Logs the running experiments and variations to adobe under the event "TL_experiments"
     *
     * @param data the experiments to be logged to adobe
     */
    fun log(data: Map<String, String>) {
        val formattedData = format.apply(data)
        adobeSDKs.forEach { it.log(formattedData) }
    }

    /**
     * Forces Adobe to create the executor so we can replace it with our own
     */
    fun setExecutor() {
        adobeSDKs.forEach { it.setExecutor() }
    }

}