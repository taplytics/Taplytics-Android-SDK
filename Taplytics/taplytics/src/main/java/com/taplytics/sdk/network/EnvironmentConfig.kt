package com.taplytics.sdk.network

/**
 * This is the configuration for all the supported environments
 *
 * @param envName Name of the environment
 * @param httpPath Path (http or https)
 * @param socketHost Host of the socket
 * @param hostName Name of the host
 * @param eventHost Host of the event
 * @param apiHost Host of the api
 */
internal sealed class EnvironmentConfig(val envName: String,
                                        private val httpPath: String,
                                        private val hostName: String,
                                        private val apiHost: String,
                                        private val eventHost: String,
                                        private val socketHost: String,
                                        private val socketPort: String) {

    private companion object {
        const val SOCKET_PORT_LOCALHOST = ":3002"
        const val SOCKET_PORT = ":443"
    }

    object Dev : EnvironmentConfig("Dev",
            "https://",
            "dev.taplytics.com",
            "dev.taplytics.com",
            "dev.taplytics.com",
            "dev.taplytics.com",
            SOCKET_PORT)

    object Staging : EnvironmentConfig("Staging",
            "https://",
            "staging.taplytics.com",
            "staging.taplytics.com",
            "staging.taplytics.com",
            "staging.taplytics.com",
            SOCKET_PORT)

    object StagingUpcoming : EnvironmentConfig("Staging Upcoming",
            "https://",
            "api-staging-upcoming.taplytics.com",
            "api-staging-upcoming.taplytics.com",
            "api-staging-upcoming.taplytics.com",
            "staging-upcoming.taplytics.com",
            SOCKET_PORT)

    object V3 : EnvironmentConfig("V3 beta",
            "https://",
            "6491480h9-capi.taplytics.com",
            "6491480h9-capi.taplytics.com",
            "6491480h9-capi.taplytics.com",
            "taplytics.com",
            SOCKET_PORT)

    object Prod : EnvironmentConfig("Production",
            "https://",
            "taplytics.com",
            "api.taplytics.com",
            "ping.taplytics.com",
            "taplytics.com",
            SOCKET_PORT)

    class LocalHost(localIP: String, localPort: String) : EnvironmentConfig("Local Host",
            "http://",
            "$localIP:$localPort",
            "$localIP:$localPort",
            "$localIP:$localPort",
            localIP,
            SOCKET_PORT_LOCALHOST
    )

    /**
     * Creates the base url to connect to the api
     *
     * @return The base url: httpPath + apiHost
     */
    fun getBaseUrl() = "$httpPath$apiHost"

    /**
     * Creates a complete URL given the endpoint
     *
     * @param endpoint The endpoint to hit
     * @return Complete URL with endpoint: httpPath + hostName + endpoint
     */
    fun getHostEndpointUrl(endpoint: String) = "$httpPath$hostName$endpoint"

    /**
     * Creates a complete URL given the endpoint
     *
     * @param endpoint The endpoint to hit
     * @return Complete URL with endpoint: httpPath + apiHost + endpoint
     */
    fun getApiEndpointUrl(endpoint: String) = "$httpPath$apiHost$endpoint"

    /**
     * Creates a complete URL given the endpoint
     *
     * @param endpoint The endpoint to hit
     * @return Complete URL with endpoint: httpPath + eventHost + endpoint
     */
    fun getEventEndpointUrl(endpoint: String) = "$httpPath$eventHost$endpoint"

    /**
     * Creates the Socket path
     *
     * @return Socket path: httpPath + socketHost + port
     **/
    fun getSocketPath() = "$httpPath$socketHost$socketPort"

}
