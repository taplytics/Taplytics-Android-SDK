/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.network;

import android.os.AsyncTask;
import android.util.Log;

import com.taplytics.sdk.analytics.TLAnalyticsManager;
import com.taplytics.sdk.analytics.TLAppUser;
import com.taplytics.sdk.datatypes.TLProperties;
import com.taplytics.sdk.managers.TLManager;
import com.taplytics.sdk.managers.TLViewManager;
import com.taplytics.sdk.utils.Functionality;
import com.taplytics.sdk.utils.TLLog;
import com.taplytics.sdk.utils.TLReaderWriter;
import com.taplytics.sdk.utils.TLUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public abstract class TLNetworking {

    final static String ROUTING_TOKEN_HEADER_KEY = "Taplytics-Routing-Token";

    final static protected String ENDPOINT_CLIENT_EVENT = "/api/v1/clientEvents/";
    final static private String ENDPOINT_RESET_APP_USER = "/api/v1/resetAppUser";
    final static private String ENDPOINT_CHOSEN_VIEW = "/api/v1/chosenView";
    final static private String ENDPOINT_EXPERIMENT_ELEMENTS = "/api/v1/experimentElements";
    final static private String ENDPOINT_IMAGES = "/api/v1/images";
    final static private String ENDPOINT_PUSH_TOKEN = "/api/v1/pushToken";
    final static private String ENDPOINT_CLIENT_APP_USER = "/api/v1/clientAppUser";

    public interface TLNetworkPropertiesResponseListener {
        void onResponse(TLProperties properties);

        void onError(Throwable error);
    }

    public interface TLNetworkResponseListener {
        void onResponse(JSONObject response);

        void onError(Throwable error);
    }

    @Deprecated
    public enum TLServer {
        TL_DEV, TL_PRODUCTION, TL_STAGING, TL_LOCALHOST, TL_V3
    }

    protected EnvironmentConfig environment = EnvironmentConfig.Prod.INSTANCE;

    private static Boolean useVolley = null;

    public static boolean shouldUseVolleyAsNetworkingLibrary(boolean userSetRetrofit) {
        if (useVolley == null) {
            useVolley = !userSetRetrofit && isVolleyInstalled();
        }
        return useVolley;
    }

    private static boolean isVolleyInstalled() {
        try {
            Class.forName("com.android.Volley");
            return true;
        } catch (Exception ex) {
            try {
                Class.forName("com.android.volley.toolbox.Volley");
                return true;
            } catch (Exception e) {
                // No volley present
            }
            // No volley present
        }
        try {
            Class.forName("retrofit2.Retrofit");
            return false;
        } catch (Exception e) {
            Log.e("Taplytics", "No correct networking library is present. " +
                    "Please add add either Retrofit2 or Volley to your build.gradle");
        }
        return true;
    }

    public abstract void enqueuePost(final String type,
                                     final String tag,
                                     final String url,
                                     final Date time,
                                     final JSONObject props,
                                     final TLNetworkResponseListener listener);

    public abstract void gitRequest(final TLNetworkResponseListener listener);

    protected abstract void enqueueImagePost(final String type,
                                             final String url,
                                             final String tag,
                                             final Date time,
                                             final JSONObject params,
                                             final TLNetworkResponseListener listener);

    public abstract void clientRequest(final Map<String, Object> props,
                                       final TLNetworkPropertiesResponseListener listener,
                                       final Date time);

    public abstract void setupNetworking(boolean isTest);

    /**
     * Set the environment to make requests to
     *
     * @param env Environment Config
     */
    public void setEnvironment(final EnvironmentConfig env) {
        if (env != this.environment) {
            this.environment = env;
            TLLog.debug("Server switched to " + environment.getEnvName());
        }
        setupNetworking(false);
    }

    /**
     * Returns the SocketIO path
     *
     * @return SocketIO path
     **/
    String getSocketIOPath() {
        return environment.getSocketPath();
    }

    /**
     * Retrieve the current environment the sdk is connected to
     *
     * @return The environment we are connected to
     */
    EnvironmentConfig getEnvironment() {
        return environment;
    }

    /**
     * Handling the response from the TLProperties request.
     */
    void handleConfigResponse(final JSONObject response,
                              final TLNetworkPropertiesResponseListener listener,
                              final Date time) {
        try {
            if (time != null) {
                final Date now = new Date();
                final long responseTime = now.getTime() - time.getTime();

                Map<String, Object> startingOptions = TLManager.getInstance().getStartingOptions();

                JSONObject customData = null;
                if (startingOptions != null) {
                    startingOptions.remove("savePropsToDisk");
                    customData = new JSONObject(startingOptions);
                }

                if (customData == null) {
                    customData = new JSONObject();
                }

                customData.put("timeOut", TLManager.getInstance().getTimeout());

                TLManager.getInstance().getTlAnalytics().trackTLEvent("tlClientConfig", responseTime, customData);
                TLLog.debug("Got Properties Response! Time: " + responseTime);
            }

            TLProperties properties = new TLProperties(response);
            if (listener != null) {
                listener.onResponse(properties);
            }
            TLReaderWriter.getInstance().writePropertiesToDisk(response);
        } catch (Throwable e) {
            TLLog.error("Parsing TLProperties", e);
            if (listener != null) {
                listener.onError(e);
            }
        }
    }

    /**
     * Handling a TLProperties Error
     **/
    void handleErrorResponse(String url, Throwable t, TLNetworkPropertiesResponseListener listener) {
        TLLog.requestError(url, "Getting Properties From Server", t);
        if (listener != null) {
            listener.onError(t);
        }
    }

    private Map getPropertiesMapInfo(Map<String, Object> props) {
        final TLManager tlManager = TLManager.getInstance();
        //try to attach the user id and email to the request if we have them
        //if the analytics manager is defined TLAppUser will be instantiated already, so no risk of null
        TLAnalyticsManager tlAnalyticsManager = tlManager.getTlAnalytics();
        TLAppUser appUser = (tlAnalyticsManager != null) ? tlAnalyticsManager.getTlAppUser() : null;
        if (appUser != null && (appUser.getUserId() != null && !appUser.getUserId().isEmpty())) {
            props.put("uid", appUser.getUserId());
        }
        String email = (appUser != null) ? appUser.getUserAttribute("email") : null;
        if (email != null) {
            props.put("email", email);
        }
        props.putAll(tlManager.getTLDeviceInfo().getDeviceProperties());
        if (TLManager.getInstance().getUserTestExperiments() != null) {
            try {
                JSONObject expObject = new JSONObject(TLManager.getInstance().getUserTestExperiments());
                props.put("uev", expObject);
            } catch (Exception e) {
                //Continue
            }
        }
        final String queuedAttributes = (appUser != null) ? appUser.getQueuedUserAttributesAsString() : null;
        if (queuedAttributes != null) {
            props.put("aua", queuedAttributes);
        }

        return props;
    }

    public void getPropertiesFromServer(Map<String, Object> props, final TLNetworkPropertiesResponseListener listener) {
        if (!TLManager.getInstance().getTrackingEnabled())
            return;
        TLUtils.executeAsyncTask(new GetPropertiesRequestTask(), props, listener);
    }

    public class GetPropertiesRequestTask extends AsyncTask<Object, Void, Void> {
        @Override
        protected Void doInBackground(Object... params) {
            Map props = (params[0] == null) ? new HashMap<>() : (Map) params[0];
            final TLNetworkPropertiesResponseListener listener = (TLNetworkPropertiesResponseListener) params[1];
            props = getPropertiesMapInfo(props);
            final Date time = new Date();
            clientRequest(props, listener, time);
            return null;
        }
    }

    public void postChosenActivity(JSONObject activityObj, final TLNetworkResponseListener listener) {
        try {
            if (!TLManager.getInstance().getTrackingEnabled())
                return;
            final String cleanURL = environment.getHostEndpointUrl(ENDPOINT_CHOSEN_VIEW).replaceAll(" ", "%20");
            JSONObject params = new JSONObject();
            try {
                params.put("projectToken", TLManager.getInstance().getApiKey());
                params.put("viewDic", activityObj);
            } catch (JSONException e) {
                TLLog.error("postChosenActivity", e);
            }

            if (params.length() == 0) {
                return;
            }
            final Date time = new Date();
            enqueueImagePost("Activity Info", cleanURL, "post_chosenView", time, params, listener);
        } catch (Throwable e) {
            //
        }
    }

    public void postTapViewElements(JSONArray views, final TLNetworkResponseListener listener, JSONObject params) {
        if (!TLManager.getInstance().getTrackingEnabled())
            return;

        final String cleanURL = environment.getHostEndpointUrl(ENDPOINT_EXPERIMENT_ELEMENTS)
                .replaceAll(" ", "%20");
        JSONObject chooseViewObj = TLViewManager.getInstance().getChooseViewObj();
        try {
            if (params == null) {
                params = new JSONObject(chooseViewObj.toString());
                params.put("t", TLManager.getInstance().getApiKey());
                params.put("os", "Android");
                params.put("views", views);
                params.put("exp_id", chooseViewObj.optString("experiment_id"));
            }

            if (params.length() == 0) {
                return;
            }

            final Date time = new Date();
            enqueueImagePost("View Info", cleanURL, "post_viewInfo", time, params, listener);

        } catch (Throwable e) {
            TLLog.error("Setting POST experimentElements properties", e);
        }
    }

    public void postAppIcon(final TLNetworkResponseListener listener) {
        if (!TLManager.getInstance().getTrackingEnabled())
            return;
        TLUtils.executeAsyncTask(new AppIconBackgroundPoster(), listener);
    }

    private class AppIconBackgroundPoster extends AsyncTask<TLNetworkResponseListener, Void, Void> {

        @Override
        protected final Void doInBackground(TLNetworkResponseListener[] listeners) {
            try {
                final TLNetworkResponseListener listener = listeners[0];

                final String cleanURL =
                        environment.getHostEndpointUrl(ENDPOINT_IMAGES).replaceAll(" ", "%20");

                JSONObject params = new JSONObject();
                try {
                    params.put("projectToken", TLManager.getInstance().getApiKey());
                    params.put("isAppIcon", true);
                    params.put("isAndroid", true);
                } catch (JSONException e) {
                    TLLog.error("postAppIcon", e);
                }
                enqueueImagePost("App Icon", cleanURL, "post_appIcon", new Date(), params, listener);
            } catch (Throwable e) {
                return null;
            }
            return null;
        }

    }

    /**
     * Gets the current release tag from Github, which is used to determine if the user has an SDK update available.
     **/
    public void getCurrentReleaseTag(final TLNetworkResponseListener listener) {
        if (!TLManager.getInstance().getTrackingEnabled()) {
            return;
        }
        TLUtils.executeAsyncTask(new GetReleaseTagTask(), listener);
    }

    private class GetReleaseTagTask extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... params) {
            final TLNetworkResponseListener listener = (TLNetworkResponseListener) params[0];
            gitRequest(listener);
            return null;
        }

    }

    public void postGCMToken(JSONObject props, final TLNetworkResponseListener listener) {
        try {
            if (!TLManager.getInstance().getTrackingEnabled() || props == null || props.length() == 0)
                return;

            final String url = environment.getApiEndpointUrl(ENDPOINT_PUSH_TOKEN);
            final Date time = new Date();
            enqueuePost("GCM Token", "post_pushToken", url, time, props, listener);
        } catch (Throwable e) {
            //
        }
    }

    public void postResetAppUser(JSONObject props, final TLNetworkResponseListener listener) {
        try {
            if (!TLManager.getInstance().getTrackingEnabled() || props == null || props.length() == 0)
                return;
            final String url = environment.getApiEndpointUrl(ENDPOINT_RESET_APP_USER);
            final Date time = new Date();
            enqueuePost("Reset App User", "post_resetAppUser", url, time, props, listener);
        } catch (Throwable e) {
            //
        }
    }

    public void postAppUserAttributes(JSONObject props, final TLNetworkResponseListener listener) {
        try {
            if (!TLManager.getInstance().getTrackingEnabled())
                return;
            final String url = environment.getApiEndpointUrl(ENDPOINT_CLIENT_APP_USER);
            final Date time = new Date();
            enqueuePost("User Attributes", "post_clientAppUser", url, time, props, listener);
        } catch (Throwable e) {
            //
        }
    }

    public void postClientEvents(final JSONObject props, final TLNetworkResponseListener listener) {
        try {
            if (!TLManager.getInstance().getTrackingEnabled() ||
                    props == null ||
                    props.length() == 0 ||
                    TLUtils.isDisabled(Functionality.EVENTS)) {
                return;
            }

            //if this client is being throttled we will not send events to the server
            if (TLUtils.isDisabled(Functionality.EVENTSTHROTTLED)) {
                listener.onResponse(null);
                return;
            }
            enqueuePost("Client Events",
                    "post_clientEvents",
                    environment.getEventEndpointUrl(ENDPOINT_CLIENT_EVENT),
                    new Date(),
                    props,
                    listener);

        } catch (Throwable e) {
            //
        }
    }

}
