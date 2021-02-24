/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.managers;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.taplytics.sdk.BuildConfig;
import com.taplytics.sdk.CodeBlockListener;
import com.taplytics.sdk.TLActivityLifecycleCallbacks;
import com.taplytics.sdk.TLDynamicVariableManager;
import com.taplytics.sdk.TLGcmBroadcastReceiver;
import com.taplytics.sdk.TaplyticsExperimentsLoadedListener;
import com.taplytics.sdk.TaplyticsExperimentsUpdatedListener;
import com.taplytics.sdk.TaplyticsNewSessionListener;
import com.taplytics.sdk.TaplyticsPushTokenListener;
import com.taplytics.sdk.TaplyticsRunningExperimentsListener;
import com.taplytics.sdk.analytics.TLAnalyticsManager;
import com.taplytics.sdk.analytics.external.adobe.AdobeManager;
import com.taplytics.sdk.analytics.external.TLExternalAnalyticsManager;
import com.taplytics.sdk.datatypes.TLDeviceInfo;
import com.taplytics.sdk.datatypes.TLJSONArray;
import com.taplytics.sdk.datatypes.TLJSONObject;
import com.taplytics.sdk.datatypes.TLProperties;
import com.taplytics.sdk.datatypes.Triplet;
import com.taplytics.sdk.datatypes.Version;
import com.taplytics.sdk.listeners.TLFlushListener;
import com.taplytics.sdk.managers.ShakeEventManager.ShakeListener;
import com.taplytics.sdk.network.EnvironmentConfig;
import com.taplytics.sdk.network.TLNetworking;
import com.taplytics.sdk.network.TLRetrofitNetworking;
import com.taplytics.sdk.network.TLSocketManager;
import com.taplytics.sdk.network.TLVolleyNetworking;
import com.taplytics.sdk.utils.FragmentUtils;
import com.taplytics.sdk.utils.Functionality;
import com.taplytics.sdk.utils.ImageUtils;
import com.taplytics.sdk.utils.SecurePrefs;
import com.taplytics.sdk.utils.SecurityUtils;
import com.taplytics.sdk.utils.TLLog;
import com.taplytics.sdk.utils.TLReaderWriter;
import com.taplytics.sdk.utils.TLUtils;
import com.taplytics.sdk.utils.promises.Promise;
import com.taplytics.sdk.utils.promises.PromiseListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class TLManager implements ShakeListener {

    private static TLManager instance = null;

    /**
     * The current Taplytics SDK version *
     */
    public static final Version TAPLYTICS_SDK_VERSION = new Version(BuildConfig.VER_NAME);
    private static final String KEY_DEVICE_TOKEN = "DEVICE_TOKEN";
    private static final int MAX_SESSION_TIME = 1440; // 24 hours in minutes
    private static final int MAX_DEFAULT_CALLBACK_TIME = 4000;
    private int CALLBACK_TIME = 4000;

    /**
     * Networking manager for Taplytics *
     */
    private TLNetworking tlNetworking = null;

    /**
     * Device information
     */
    private TLDeviceInfo tlDeviceInfo = null;

    /**
     * Analytics manager for Taplytics *
     */
    private TLAnalyticsManager tlAnalytics = null;

    /**
     * Push manager for Taplytics *
     */
    private TLPushManager tlPush = null;

    /**
     * Map of starting options. Stored so we can return as part of the clientconfig time logging.
     */
    private Map<String, Object> startingOptions = new HashMap<>();

    /**
     * User's API key *
     */
    private String apiKey = null;

    /**
     * Routing key, SHA-256 hash off their API Key
     */
    private String routingToken = null;

    /**
     * Taken from the pairing deeplink.
     * If set, is added to clientConfig call so that we can re-pair easily.
     */
    private String deviceToken = null;

    /**
     * Whether or not the application is in live update mode.
     * This means that the socket connection is open and the border will show. *
     */
    private boolean liveUpdate = false;

    /**
     * Whether or not the activity lifecycle callbacks have already been registered. *
     */
    private boolean activityLifeCycleCallbacksOn = false;

    /**
     * Whether or not we've initiated if this build is going to have liveUpdate enabled. *
     */
    private boolean initLiveUpdate = false;

    /**
     * This is a safety switch. Toggle this to turn off stuff if Taplytics is breaking stuff for people *
     */
    private boolean isActive = true;

    /**
     * Whether or not the client has enabled the special shake menu (triggered by orientation changes.
     * See {@link TLActivityLifecycleCallbacks#onActivityResumed(Activity)}.
     */
    private boolean turnMenuEnabled = false;

    /**
     * Whether or not the user toggled live update or if we did *
     * If set then we add a "dev=1" parameter to the clientConfig call
     * This allows pairing without deeplink
     */
    private boolean userSetLiveUpdate = false;

    /**
     * Whether or not the current activity is active (true = active, false = in background) *
     */
    private boolean isActivityActive = false;

    /**
     * HashMap of experiments specified by the user in the starting options to force the user into.
     */
    private HashMap userTestExperiments = null;

    /**
     * Whether or not a new update is available for Taplytics *
     */
    private boolean updateAvailable = false;

    /**
     * Whether or not we have requested initial server properties.
     */
    private boolean hasRequestedInitialServerProperties = false;

    /**
     * Whether or not we have loaded properties from server.
     */
    private boolean hasLoadedPropertiesFromServer = false;

    /**
     * FastMode does not send any events or attribute updates. Uses new endpoint.
     */
    public boolean isFastMode() {
        return fastMode;
    }

    private boolean fastMode = false;

    public boolean hasLoadedPropertiesFromServer() {
        return hasLoadedPropertiesFromServer;
    }

    /**
     * Whether or not the app has backgrounded.
     */
    private boolean hasAppBackgrounded = false;

    /**
     * Tracks the last activity date to see if its been more then maxActivityIntervalMins for a new session.
     */
    private Date lastActivityDate = null;

    /**
     * The maximum activity interval to create a new session
     */
    private double maxActivityIntervalMinutes = 10;

    /**
     * If the user is currently being reset, used to decide if we should resend the push token
     */
    private boolean isResettingUser = false;

    /**
     * Caches user id from setting user attributes to device if turned on, used for userBucketing projects
     */
    private boolean userBucketing = false;

    /**
     * If sockets are enabled for this build
     */
    public final boolean thereAreSockets = socketsEnabled();


    /**
     * If we should show a socket dialog when we have an activity
     */
    private boolean showSocketDialog = false;

    /**
     * Listeners that gets called when the properties have been loaded (gets called once)
     */
    private Set<TaplyticsExperimentsLoadedListener> experimentsLoadedListeners;
    volatile boolean experimentsLoaded;

    private Promise experimentsLoadedPromise = new Promise();

    /**
     * A listener that gets called when the experiments are updated (for use in testing only)
     */
    private TaplyticsExperimentsUpdatedListener experimentUpdatedListener = null;

    /**
     * A listener that gets called when taplytics enters a new session. See {@link #checkLastActivityForNewSession()}
     */
    private TaplyticsNewSessionListener newSessionListener = null;

    /**
     * Whether or not this is a test.
     */
    private boolean isTest = false;

    private void setMaxActivityIntervalMinutes(double minutes) {
        maxActivityIntervalMinutes = minutes;
    }

    public boolean hasSentAppLink() {
        return sentAppLink;
    }

    private boolean sentAppLink = false;

    public boolean areFragmentsEnabled() {
        return fragmentsEnabled;
    }

    /**
     * Whether or not we've enabled fragment tracking and stuff (just in case it causes a crash in the app)
     */
    private boolean fragmentsEnabled = true;

    private Boolean isDebug;

    /**
     * A promise that can be checked across the SDK to see if tlProperties has successfully been loaded. If it is not yet finished, you can strap a listener on it to wait until its done.
     */
    private Promise tlPropertiesPromise = new Promise();

    /**
     * @return {@link #tlPropertiesPromise}
     */
    public Promise getTlPropertiesPromise() {
        return tlPropertiesPromise;
    }

    /**
     * A promise that can be checked across the SDK to see if tlProperties has successfully been loaded from DISK.
     * If it is not yet finished, you can strap a listener on it to wait until its done.
     */
    private Promise tlPropertiesDiskPromise = new Promise();

    /**
     * TLProperties instance *
     */
    private TLProperties tlProperties = null;

    public void setAppContext(Context appContext) {
        this.appContext = appContext;
    }

    /**
     * trackingId
     */

    private String trackingId = null;

    public String getTrackingId() {
        return this.trackingId;
    }

    /**
     * The APPLICATION context. Do not confuse with activity context *
     */
    private Context appContext = null;

    /**
     * The current activity. Use this as the context for dialogs and resources *
     */
    private WeakReference<Activity> currentActivityWeakRef = null;

    /**
     * The current activities simple class name
     */

    private String currentActivitySimpleClassName = null;

    private Executor executor;


    private TLActivityLifecycleCallbacks activityCallbacks;

    TLActivityLifecycleCallbacks getActivityCallbacks() {
        return activityCallbacks;
    }

    /**
     * An instance of our shake manager *
     */
    private ShakeEventManager shakeManager = null;

    /**
     * Get the instance of the TLManager *
     */
    public synchronized static TLManager getInstance() {
        if (instance == null) {
            instance = new TLManager();
        }
        return instance;
    }

    synchronized static void setInstance(final TLManager tlManager) {
        instance = tlManager;
    }

    public void reset() {
        if (shakeManager != null) {
            shakeManager.unregister();

        }
        if (activityCallbacks != null) {
            TLActivityLifecycleCallbacks.unregisterCallbacks(getActivityCallbacks());
        }
        if (tlAnalytics != null) {
            TLExternalAnalyticsManager.getInstance().unRegisterExternalAnalytics();
        }
        if (tlPush != null && tlPush.hasCheckedPushToken()) {
            tlPush.clearListeners();
        }
        instance = null;
    }

    public boolean isAsyncEnabled() {
        return isAsyncEnabled;
    }

    private boolean isAsyncEnabled = false;

    /**
     * @return application's API key *
     */
    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
        this.routingToken = SecurityUtils.getSHA256Hash(apiKey);
        TLLog.debug("Taplyitcs Routing Token: " + this.routingToken);
    }

    /**
     * @return application's Routing Token
     */
    public String getRoutingToken() {
        return routingToken;
    }

    /**
     * @return {@link com.taplytics.sdk.managers.TLManager#userBucketing} *
     */
    public boolean isUserBucketingEnabled() {
        return userBucketing;
    }

    /**
     * @return {@link com.taplytics.sdk.managers.TLManager#liveUpdate} *
     */
    public boolean isLiveUpdate() {
        return liveUpdate;
    }

    /**
     * @param isRegistered Whether or not the lifecycle callbacks have been registered
     */
    public void setActivityCallbacksRegistered(boolean isRegistered) {
        activityLifeCycleCallbacksOn = isRegistered;
    }

    /**
     * @param backgrounded Whether or not the app has been backgrounded
     */
    public void setAppHasBackgrounded(boolean backgrounded) {
        hasAppBackgrounded = backgrounded;
    }

    /**
     * @return {@link #updateAvailable}
     */
    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    /**
     * @return {@link #isActivityActive} *
     */
    public boolean isActivityActive() {
        return isActivityActive;
    }

    /**
     * Set whether or not the activity is the foreground of background.
     *
     * @param isActivityActive Whether or not the activity is active or in the background
     */
    public void setActivityActive(boolean isActivityActive) {
        this.isActivityActive = isActivityActive;

        if (isActivityActive)
            checkLastActivityForNewSession();
        if (hasAppBackgrounded)
            hasAppBackgrounded = false;
    }

    public TLUtils.DebugCheckType getDebugCheckType() {
        return debugCheckType;
    }

    private TLUtils.DebugCheckType debugCheckType = TLUtils.DebugCheckType.FLAG_ONLY;

    /**
     * @return {@link #isActive} *
     */
    public boolean getIsActive() {
        return isActive;
    }

    /**
     * @return isActive and is has not opted out
     */
    public boolean getTrackingEnabled() {
        return isActive && !TLKillSwitchManager.getInstance().hasOptedOut;
    }

    /**
     * @return {@link #tlProperties} *
     */
    public TLProperties getTlProperties() {
        return tlProperties;
    }

    /**
     * @return {@link #tlNetworking} *
     */
    public TLNetworking getTlNetworking() {
        return tlNetworking;
    }

    /**
     * @return {@link #tlDeviceInfo} *
     */
    public TLDeviceInfo getTLDeviceInfo() {
        return tlDeviceInfo;
    }

    /**
     * @return {@link #tlAnalytics} *
     */
    public TLAnalyticsManager getTlAnalytics() {
        return tlAnalytics;
    }

    /**
     * @return {@link #tlPush} *
     */
    public TLPushManager getTlPushManager() {
        return tlPush;
    }

    /**
     * @return {@link #appContext} *
     */
    public Context getAppContext() {
        return appContext;
    }

    /**
     * return current activity
     */
    public Activity getCurrentActivity() {
        return currentActivityWeakRef != null ? currentActivityWeakRef.get() : null;
    }

    /**
     * Return true if the given activity is equal to the current activity and the current activity is not null
     */
    public boolean isCurrentActivityEqualTo(Activity activity) {
        return getCurrentActivity() == activity;
    }

    /**
     * return {@link #currentActivitySimpleClassName} *
     */
    public String getCurrentActivitySimpleClassName() {
        return currentActivitySimpleClassName;
    }

    public boolean isExperimentLoadTimeout() {
        return experimentLoadTimeout;
    }

    public void setExperimentLoadTimeout(boolean experimentLoadTimeout) {
        this.experimentLoadTimeout = experimentLoadTimeout;
    }

    /**
     * @return whether or not the {@link TaplyticsExperimentsLoadedListener} associated with taplytics starting reached its timeout.
     * <p/>
     * If this is true, we will not be using any new configs sent down by taplytics for the remainder of the session.
     * <p/>
     * This is because if a client has proceeded despite this timeout being hit, their interactions with the application
     * will have been purely with the default values and we do not want to say that the users are in any variation.
     */
    private boolean experimentLoadTimeout = false;

    /**
     * Set the current activity. If its new, clear all the fragments, and set it as active.
     *
     * @param activity Activity sent from {@link com.taplytics.sdk.TLActivityLifecycleCallbacks#onActivityStarted(android.app.Activity)}
     */
    public void setCurrentActivity(@NonNull final Activity activity) {
        try {
            if (!isActive) {
                currentActivityWeakRef = null;
                return;
            }

            if (showSocketDialog) {
                //show the socket dialog
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new AlertDialog.Builder(activity)
                                .setTitle("Taplytics has changed its socket dependency!")
                                .setMessage("Please update it to ensure Taplytics works properly. Thank you! (This will only be shown to debug devices)")
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .setNegativeButton("Read More", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent i = new Intent(Intent.ACTION_VIEW);
                                        i.setData(Uri.parse("https://github.com/taplytics/Taplytics-Android-SDK/blob/master/SOCKETS.md"));
                                        dialog.dismiss();
                                        activity.startActivity(i);
                                    }
                                }).show();
                    }
                });
            }

            if (!isCurrentActivityEqualTo(activity)) {
                for (Map.Entry<Object, Triplet<String, String, Boolean>> entry : TLFragmentManager.getInstance().getFragmentsOnScreen().entrySet()) {
                    if (entry.getValue().first.equals(getCurrentActivitySimpleClassName())) {
                        FragmentUtils.removeOldFragment(entry.getKey(), false, entry.getValue().third);
                    }
                }

                currentActivityWeakRef = new WeakReference<>(activity);
                isActivityActive = true;
                currentActivitySimpleClassName = activity.getClass().getSimpleName();

                checkLastActivityForNewSession();

                TLFragmentManager.getInstance().getFragmentsOnScreen().clear();
                TLFragmentManager.getInstance().setHasExecutedTransactions(false);
                TLFragmentManager.getInstance().getViewPagerFragments().clear();
                TLLog.debug("Setting current activity");

                Intent intent = activity.getIntent();
                Bundle bundle = intent.getExtras();
                if (bundle != null && bundle.getBoolean("tl_notif", false) && bundle.getString("tl_id") != null && bundle.getString("tl_receiver") == null) {
                    tlAnalytics.trackPushNotificationInteraction(bundle, TLAnalyticsManager.PushEvent.OPENED);
                }

                FragmentUtils.attachBackStackListener(activity);
            }


        } catch (Exception e) {
            TLLog.error("Error setting activity.", e);
        }
    }

    /**
     * Check for whether or not we see an app link here and then pair the device.
     *
     * @param a An incoming activity
     */
    public void checkActivityForAppLink(Activity a) {
        if (!isActive || hasSentAppLink())
            return;

        TLLog.debug("Checking for app link");

        try {
            if (a != null) {
                Intent intent = a.getIntent();
                if (intent != null && intent.getAction() != null) {
                    TLLog.debug("Intent data string:" + intent.getDataString());
                    if (intent.getAction().equals(Intent.ACTION_VIEW) && intent.getDataString() != null
                            && intent.getDataString().startsWith("tl-")) {
                        TLLog.debug("Found App Link!");
                        TLSocketManager.getInstance().pairDeviceFromLink(intent.getDataString());
                    }
                }

            }
        } catch (Exception e) {
            TLLog.error("Error finding app link", e);
            //Safety net
        }
    }

    private TLManager() {
        executor = new SynchronousExecutor();
        experimentsLoadedListeners = new HashSet<>();
    }

    TLManager(final TLProperties properties, final TLAnalyticsManager tlAnalyticsManager) {
        this();
        tlProperties = properties;
        tlAnalytics = tlAnalyticsManager;
    }

    TLManager(final Executor executor, final Set<TaplyticsExperimentsLoadedListener> experimentsLoadedListeners) {
        this.executor = executor;
        this.experimentsLoadedListeners = experimentsLoadedListeners;
    }

    public int getTimeout() {
        return CALLBACK_TIME;
    }

    public boolean getHasAppBackgrounded() {
        return hasAppBackgrounded;
    }

    /**
     * Starting taplytics! Kick everything off. Start our {@link com.taplytics.sdk.network.TLNetworking} and
     * {@link com.taplytics.sdk.analytics.TLAnalyticsManager}, and fill our {@link com.taplytics.sdk.datatypes.TLDeviceInfo}; Also go
     * through all the options and set them up. Then, connect to the server.
     *
     * @param appContext The application's context. Must come from an application class, or 'getApplicationContext()'
     * @param apiKey     User's api key
     * @param options    The options for starting taplytics
     */
    public void startTaplytics(final Context appContext, final String apiKey, final Map<String, Object> options, final int timeOut, final TaplyticsExperimentsLoadedListener listener) {
        if (apiKey.equals(this.apiKey)) {
            Log.d("Taplytics", "Taplytics has already started! Taplytics only needs to be started once, preferably in your Application subclass.");
            return;
        }

        if (options != null) {
            if (options.containsKey("async")) {
                isAsyncEnabled = (boolean) options.get("async");
                if (isAsyncEnabled) {
                    executor = Executors.newSingleThreadExecutor();
                }
            }
            if (startingOptions == null || startingOptions.size() == 0) {
                this.startingOptions = options;
            }
        }

        if (Build.VERSION.SDK_INT >= 14 && (!activityLifeCycleCallbacksOn)) {
            activityCallbacks = TLActivityLifecycleCallbacks.registerTLActivityLifecycleCallbacks(appContext);
        }

        CALLBACK_TIME = timeOut;

        // Set up the experiments loaded listener and its timeouts ASAP
        // because the activity lifecycle callbacks kickoff
        // getting properties from server right away which causes a weird async race condition with the promise.
        registerExperimentsLoadedListener(listener);
        setupExperimentLoadedListener();

        executor.execute(new Runnable() {
            @Override
            public void run() {
                startTaplyticsInternal(appContext, apiKey, options);
            }
        });
    }

    public HashMap getUserTestExperiments() {
        return userTestExperiments;
    }

    void startTaplyticsInternal(Context appContext, String apiKey, final Map<String, Object> options) {
        try {
            Log.d("Taplytics", "Starting taplytics version " + TAPLYTICS_SDK_VERSION.get());

            if (appContext == null || apiKey == null) {
                Log.d("Taplytics", "Failed to start Taplytics, missing App Context or API Key");
                return;
            }

            if (!TLUtils.checkHasAndroidPermission(appContext, "android.permission.INTERNET")) {
                Log.w("Taplytics", "Failed to find the necessary android.permission.INTERNET permission.");
                return;
            }

            if (options != null && options.containsKey("testcase") && options.get("testcase") instanceof Boolean) {
                this.isTest = (Boolean) options.get("testcase");
            }

            if (options != null && options.get("trackingId") != null && options.get("trackingId") instanceof String) {
                this.trackingId = (String) options.get("trackingId");
            }

            TLLog.debug("Start Taplytics with API Key: " + apiKey, true);
            this.appContext = appContext;
            setApiKey(apiKey);

            TLKillSwitchManager.getInstance().hasUserOptedOutTracking(appContext);

            boolean userSetRetrofit = false;
            if (options != null) {
                Object useRetrofit = options.get("retrofit");
                if (useRetrofit != null && useRetrofit instanceof Boolean) {
                    userSetRetrofit = (Boolean) useRetrofit;
                }
            }


            TLUtils.upgradePreferences();
            this.tlDeviceInfo = new TLDeviceInfo(appContext);
            if (this.tlNetworking == null) {
                this.tlNetworking = TLNetworking.shouldUseVolleyAsNetworkingLibrary(userSetRetrofit) ? new TLVolleyNetworking() : new TLRetrofitNetworking();
            }

            if (tlAnalytics == null) {
                this.tlAnalytics = new TLAnalyticsManager();
                if (options != null && options.containsKey("reporting_time") && options.get("reporting_time") instanceof Integer) {
                    this.tlAnalytics.setLiveReportingTime(Long.valueOf((Integer) (options.get("reporting_time"))));
                }
            } else {
                tlAnalytics.recreateDBHelper();
            }

            this.tlAnalytics = tlAnalytics == null ? new TLAnalyticsManager() : tlAnalytics;


            this.tlPush = new TLPushManager(options);


            // Initialize our live update status if it hasn't been yet.
            if (!initLiveUpdate) {
                checkLiveUpdate(true, options);
                initLiveUpdate = true;
            }

            EnvironmentConfig envConfig = EnvironmentConfig.Prod.INSTANCE;

            //If Taplytics is started with a delay or with segment, the activityLifecycleCallbacks will not be applied at the correct time. This ensures that we still get the client config in this situation.
            boolean getInitialPropertiesDueToDelayedStart = false;

            // Run through the options we were given, if there is one
            if (options != null) {
                Object debugLogging = options.get("debugLogging");
                Object logging = options.get("logging");
                Object liveUpdate = options.get("liveUpdate");
                Object shakeMenu = options.get("shakeMenu");
                Object server = options.get("server");
                Object fastMode = options.get("fastMode");
                Object fragments = options.get("fragments");
                Object sessionInterval = options.get("sessionMinutes");
                Object turnMenu = options.get("turnMenu");
                Object localAddress = options.get("localIP");
                Object localPort = options.get("localPort");
                Object testExperiments = options.get("testExperiments");
                Object borders = options.get("disableBorders");
                Object isDebug = options.get("isDebug");
                Object delayedStartTaplytics = options.get("delayedStartTaplytics");
                Object base64 = options.get("base64");
                Object changingFragments = options.get("ignoreFragmentClassNames");
                Object mainSocketThread = options.get("socketThreads");
                Object debugCheckType = options.get("debugCheckType");
                Object aggressive = options.get("aggressive");
                Object newAdobeFormat = options.get("newAdobeFormat");
                Object expAmplitudeEventFormat = options.get("expAmplitudeEventFormat");
                Object userBucketing = options.get("userBucketing");

                if (liveUpdate != null && liveUpdate instanceof Boolean) {
                    this.liveUpdate = (Boolean) liveUpdate;
                    this.userSetLiveUpdate = true;
                }

                if (userBucketing != null && userBucketing instanceof Boolean) {
                    this.userBucketing = (Boolean) userBucketing;
                }

                if (aggressive != null && aggressive instanceof Boolean) {
                    TLViewManager.getInstance().setAggressiveViewChanges((boolean) aggressive);
                }

                if (isDebug != null && isDebug instanceof Boolean) {
                    this.isDebug = (boolean) isDebug;
                }

                if (changingFragments != null && changingFragments instanceof Boolean) {
                    TLFragmentManager.getInstance().setChangingFragmentNames((Boolean) changingFragments);
                }

                if (testExperiments != null && testExperiments instanceof HashMap) {
                    userTestExperiments = (HashMap) testExperiments;
                }

                if (fragments != null && fragments instanceof Boolean) {
                    this.fragmentsEnabled = (Boolean) fragments;
                }

                if (borders != null && borders instanceof Boolean) {
                    TLViewManager.getInstance().setBordersEnabled(!((Boolean) borders));
                }

                if (base64 != null && base64 instanceof Boolean) {
                    base64Enabled = (boolean) base64;
                }

                if (debugLogging != null && debugLogging instanceof Boolean) {
                    TLLog.getInstance().setDebugLogging((Boolean) debugLogging);
                }

                if (logging != null && logging instanceof Boolean) {
                    TLLog.getInstance().setClientLogging((Boolean) logging);

                    if ((Boolean) logging) {
                        if (tlAnalytics.getTlAppUser().getUserAttribute("user_id") != null) {
                            TLLog.debug("App User user_id: " + tlAnalytics.getTlAppUser().getUserAttribute("user_id"), true);
                        }
                        if (tlAnalytics.getTlAppUser().getUserAttribute("email") != null) {
                            TLLog.debug("App User email: " + tlAnalytics.getTlAppUser().getUserAttribute("email"), true);
                        }
                        if (this.liveUpdate) {
                            TLLog.debug("App started in debug mode", true);
                        } else {
                            TLLog.debug("App started in prod mode", true);

                        }
                    }
                }

                if (mainSocketThread != null && mainSocketThread instanceof Boolean) {
                    TLSocketManager.getInstance().setSocketConnectOnMainThread((Boolean) mainSocketThread);
                }

                if (debugCheckType != null && debugCheckType instanceof String) {
                    this.debugCheckType = TLUtils.DebugCheckType.fromString((String) debugCheckType);
                }

                if (newAdobeFormat != null) {
                    //Legacy -- if a client was setting via boolean. 0 = false = original format (to be safe)
                    if (newAdobeFormat instanceof Boolean) {
                        final boolean isNewAdobeFormat = (Boolean) newAdobeFormat;
                        TLExternalAnalyticsManager.getInstance()
                                .setAdobeFormat(isNewAdobeFormat ? AdobeManager.Format.BASELINE_A : AdobeManager.Format.ORIGINAL);
                    } else if (newAdobeFormat instanceof Integer) {
                        final int newAdobeFormatIndex = (int) newAdobeFormat;
                        TLExternalAnalyticsManager.getInstance()
                                .setAdobeFormat(AdobeManager.Format.values()[newAdobeFormatIndex]);
                    }
                }

                if (expAmplitudeEventFormat != null && expAmplitudeEventFormat instanceof Boolean) {
                    TLExternalAnalyticsManager.getInstance().setAmplitudeFormat((Boolean) expAmplitudeEventFormat);
                }

                if (delayedStartTaplytics != null && delayedStartTaplytics instanceof Boolean) {
                    //For some reason, people delay Taplytics (such as people using segment) which means our activity lifecycle callbacks wont be attached at the right time.
                    getInitialPropertiesDueToDelayedStart = ((Boolean) delayedStartTaplytics);
                    try {
                        if ((currentActivityWeakRef == null || currentActivityWeakRef.get() == null) && activityLifeCycleCallbacksOn && activityCallbacks != null) {
                            Activity activity = TLUtils.getActivity();
                            if (activity != null && activity.getClass().getName().contains(appContext.getPackageName())) {
                                activityCallbacks.onActivityStarted(activity);
                            }
                        }
                    } catch (Throwable e) {
                        //just in case.
                    }
                }

                if (shakeMenu != null && shakeMenu instanceof Boolean) {
                    if (!(Boolean) shakeMenu) {
                        TLKillSwitchManager.getInstance().disableFunction(Functionality.SHAKEMENU);
                        if (shakeManager != null) {
                            shakeManager.unregister();
                        }
                    }
                }

                if (server instanceof String) {
                    String serverStr = (String) server;
                    if (serverStr.equals("local") || server == "localHost") {
                        envConfig = getLocalHostConfig(localAddress, localPort);
                    } else if (serverStr.equals("staging")) {
                        envConfig = EnvironmentConfig.Staging.INSTANCE;
                    } else if (serverStr.equals("staging-upcoming")) {
                        envConfig = EnvironmentConfig.StagingUpcoming.INSTANCE;
                    } else if (serverStr.equals("dev")) {
                        envConfig = EnvironmentConfig.Dev.INSTANCE;
                    }
                }

                if (fastMode != null && fastMode instanceof Boolean) {
                    if ((Boolean) fastMode) {
                        envConfig = EnvironmentConfig.V3.INSTANCE;
                        this.fastMode = true;
                        this.userBucketing = true;
                        TLKillSwitchManager.getInstance().disableFunction(Functionality.EVENTS);
                        TLKillSwitchManager.getInstance().disableFunction(Functionality.PUSH);
                        TLKillSwitchManager.getInstance().disableFunction(Functionality.ANALYTICS);
                    }
                }

                if (sessionInterval != null && sessionInterval instanceof Number) {
                    try {
                        Number sessionMinutes = (Number) sessionInterval;
                        if (sessionMinutes.intValue() > MAX_SESSION_TIME) {
                            TLLog.warning("Session background time is greater then 24hrs, setting to 24hrs.");
                            sessionMinutes = MAX_SESSION_TIME;
                        }
                        setMaxActivityIntervalMinutes(sessionMinutes.doubleValue());
                    } catch (Exception e) {
                        TLLog.error("problem setting session interval time", e);
                    }
                }

                // Get manually disabled sources from start options (for example to disable Mixpanel)
                if (options.containsKey("disable")) {
                    Object disableOptions = options.get("disable");
                    if (disableOptions instanceof ArrayList) {
                        HashSet<String> set = new HashSet<>((ArrayList<String>) disableOptions);
                        //Add this list to the manager of disabled things.
                        TLKillSwitchManager.getInstance().setDisabledFunctions(set);
                    }
                }

                if (turnMenu != null && turnMenu instanceof Boolean) {
                    turnMenuEnabled = (Boolean) turnMenu;
                }
            }

            tlNetworking.setEnvironment(envConfig);

            tlAnalytics.readDiskEvents();

            if (pendingRunningExpListeners == null) {
                pendingRunningExpListeners = new ArrayList<>();
            }

            final String key = this.apiKey;// Grab whatever saved TLProperties we may have.
            TLReaderWriter.getInstance().readTLPropertiesFromDisk(new TLReaderWriter.TLPropertiesReaderListener() {
                @Override
                public void callback(TLProperties properties, Exception e) {
                    if (e != null) {
                        TLLog.warning("Reading TLProperties from disk", e);
                        tlPropertiesDiskPromise.cancel();
                    } else if (properties != null) {
                        JSONObject project = properties.getProject();
                        if (project != null) {
                            if (shouldBeKilled(properties)) {
                                return;
                            }

                            if (project.has("token")) {
                                try {
                                    String token = project.getString("token");
                                    if (token == null || (key.equals(token))) {
                                        tlProperties = properties;
                                        tlPropertiesDiskPromise.finish();
                                        checkLiveUpdate(false, null);

                                        //Check for deeplinks as soon as possible.
                                        isListeningForDeeplink(tlProperties.getDeepLink());
                                    } else {
                                        TLReaderWriter.getInstance().deleteTLPropertiesFileFromDisk();
                                    }
                                } catch (Exception e1) {
                                    TLLog.error("Getting token from disk TLProperties", e1);
                                }
                                TLKillSwitchManager.getInstance().updateDisabledFunctionality(properties);
                            }
                        }
                        tlAnalytics.startAnalyticsSourceTracking();
                    }

                }

            });

            // Only get initial properties from server here if no activity life cycle notifications.
            // Otherwise wait for activity to be created before getting settings from the server (to fix sessions issue)
            if (Build.VERSION.SDK_INT < 14 || (!activityLifeCycleCallbacksOn || getInitialPropertiesDueToDelayedStart)) {
                getInitialPropertiesFromServer();
            }

            //Send up our app icon
            if (isDebug) {
                tlNetworking.postAppIcon(null);
            }

            setupShakeMenu();

        } catch (Throwable e) {
            TLLog.error("Master catch!", e);
        }
    }

    /**
     * Creates and returns a local host environment config
     *
     * @param localAddress Local Adress from options
     * @param localPort    Local port from options
     * @return The Local Host as an {@link EnvironmentConfig}
     */
    private EnvironmentConfig getLocalHostConfig(@Nullable Object localAddress,
                                                 @Nullable Object localPort) {
        final String localAddressStr;
        if (localAddress instanceof String) {
            localAddressStr = (String) localAddress;
        } else {
            localAddressStr = "";
            TLLog.warning("localIP is null! Set in Start Taplytics options.");
        }

        final String localPortStr;
        if (localPort instanceof String) {
            localPortStr = (String) localPort;
        } else {
            localPortStr = "";
            TLLog.warning("localPort is null! Set in Start Taplytics options.");
        }

        return new EnvironmentConfig.LocalHost(localAddressStr, localPortStr);
    }

    private void setupExperimentLoadedListener() {
        try {
            final PromiseListener experimentsLoadedFromServerPromiseListener = new PromiseListener() {
                @Override
                public void succeeded() {
                    experimentsLoaded();
                }

                @Override
                public void failedOrCancelled() {
                    Log.w("Taplytics", "Taplytics experiment load has timed out.");
                    experimentLoadTimeout = true;

                    if (tlPropertiesDiskPromise != null) {
                        final PromiseListener experimentsLoadedFromDiskPromiseListener = new PromiseListener() {

                            @Override
                            public void succeeded() {
                                super.succeeded();
                                experimentsLoaded();
                                performPendingRunningExpListeners();
                                TLFeatureFlagManager.getInstance().performPendingRunningFlagListeners();
                                tlPropertiesDiskPromise.finish();
                            }

                            @Override
                            public void failedOrCancelled() {
                                super.failed();
                                experimentsLoaded();
                                performPendingRunningExpListeners();
                                TLFeatureFlagManager.getInstance().performPendingRunningFlagListeners();
                                if (tlProperties != null) {
                                    tlPropertiesDiskPromise.finish();
                                } else {
                                    tlPropertiesDiskPromise.fail(new Exception("no properties on disk"));
                                }
                            }
                        };
                        tlPropertiesDiskPromise.add(experimentsLoadedFromDiskPromiseListener);
                    }
                }
            };
            experimentsLoadedPromise.add(experimentsLoadedFromServerPromiseListener);

            final int timeoutinMilliseconds = CALLBACK_TIME < 0 ? MAX_DEFAULT_CALLBACK_TIME : CALLBACK_TIME;
            experimentsLoadedPromise.timeout(timeoutinMilliseconds);
        } catch (Exception e) {
            //fizzle
        }
    }

    /**
     * Registers Experiments Loadlistener only if taplytics hasn't already loaded. Queues it up for callback when Taplytics eventually loads.
     *
     * @param experimentsLoadedListener
     */
    public void registerExperimentsLoadedListener(
            final TaplyticsExperimentsLoadedListener experimentsLoadedListener) {
        if (experimentsLoadedListener == null) {
            return;
        }

        if (experimentsLoaded) {
            experimentsLoadedListener.loaded();
            return;
        }
        experimentsLoadedListeners.add(experimentsLoadedListener);
    }

    /**
     * Calls back all load listeners and clears queue
     */
    void experimentsLoaded() {
        experimentsLoaded = true;

        for (final TaplyticsExperimentsLoadedListener experimentsLoadedListener : experimentsLoadedListeners) {
            experimentsLoadedListener.loaded();
        }
        experimentsLoadedListeners.clear();
    }

    /**
     * Set up and register the shake menu if needed and allowed.
     */
    private void setupShakeMenu() {
        // Start up our shake manager.
        if (isDebug == null) {
            isDebug = TLUtils.checkIsDebug(appContext);
        }
        if (isDebug || isLiveUpdate() && !TLUtils.isDisabled(Functionality.SHAKEMENU) && shakeManager == null) {
            shakeManager = new ShakeEventManager();
            shakeManager.setListener(this);
            shakeManager.init(appContext);
        }
    }

    /**
     * Get initial properties from the server, only call if there is a new Activity, or app has been
     * backgrounded for longer then maxActivityIntervalMins.
     */
    public void getInitialPropertiesFromServer() {
        //If we've done this already, or if Taplytics is disabled, or if taplytics hasn't been set up yet, just return.
        if (hasRequestedInitialServerProperties || !isActive || tlNetworking == null)
            return;

        //We might be calling Taplytics from the background. In which case, the app isn't technically active. Ignore that.
        if (!hasAppBackgrounded) {
            getTlAnalytics().trackTLEvent(TLAnalyticsManager.TLAnalyticsEventAppActive);
        }

        hasRequestedInitialServerProperties = true;

        getPropertiesFromServer(null, null);

        checkGitTagForUpdate();
    }

    /**
     * if app has backgrounded and activity is now active after a number of minutes that is greater
     * then the maxActivityIntervalMins, get a new session from the server.
     */
    public void checkLastActivityForNewSession() {
        if (lastActivityDate != null && hasAppBackgrounded) {
            Date now = new Date();
            if ((now.getTime() - lastActivityDate.getTime()) > maxActivityIntervalMinutes * 60 * 1000) {
                hasRequestedInitialServerProperties = false;
                getNewSessionId();
            }
        }
        lastActivityDate = new Date();
    }

    /**
     * Starts a new Taplytics session
     *
     * @param newSessionListener Callback invoked with the result of the operation
     */
    public void startNewSession(@Nullable final TaplyticsNewSessionListener newSessionListener) {
        if (tlAnalytics == null) {
            return;
        }

        final boolean hasSessionNumBeenIncremented =
                tlAnalytics.getTlAppUser().incrementNewSessionCounter(new Date());
        if (!hasSessionNumBeenIncremented && !isLiveUpdate()) {
            TLLog.warning("Too many new sessions attempted");
            if (newSessionListener != null) {
                newSessionListener.onError();
            }
            return;
        }

        lastActivityDate = new Date();

        tlAnalytics.getTlAppUser().flushAppUserAttributes(new TLFlushListener() {
            @Override
            public void flushCompleted(boolean success) {
                getNewSessionId(newSessionListener);
                TLLog.debug("Started new session!", true);
            }

            @Override
            public void flushFailed() {
                getNewSessionId(newSessionListener);
            }
        });
    }

    /**
     * Get initial properties from the server, only call if there is a new Activity, or app has been
     * backgrounded for longer then maxActivityIntervalMins.
     */
    private void getNewSessionId(final TaplyticsNewSessionListener listener) {
        if (tlProperties != null) {
            tlProperties.setSessionID(null);
        }
        Promise sessionPromise = new Promise();
        sessionPromise.add(new PromiseListener() {
            @Override
            public void succeeded() {
                TLDynamicVariableManager.getInstance().clearSynchronousVariables();
                TLDynamicVariableManager.getInstance().checkForVariableUpdates();
                if (listener != null) {
                    listener.onNewSession();
                }
                if (newSessionListener != null) {
                    newSessionListener.onNewSession();
                }
                super.succeeded();
            }

            @Override
            public void failedOrCancelled() {
                if (listener != null) {
                    listener.onError();
                }
                if (newSessionListener != null) {
                    newSessionListener.onError();
                }
                super.failedOrCancelled();
            }
        });
        //If we've started a new session
        experimentLoadTimeout = false;
        getPropertiesFromServer(null, sessionPromise);

    }


    private void getNewSessionId() {
        getNewSessionId(null);
    }

    /**
     * Determine whether or not Taplytics is listening for a specific URI
     *
     * @param uriString The uri we are assumed to be listening to
     * @return Whether or not the user has set up this device to listen.
     */
    @SuppressWarnings("WrongConstant")
    private boolean isListeningForDeeplink(String uriString) {
        if (appContext != null) {
            final PackageManager mgr = appContext.getPackageManager();
            //Construct a fake intent and see if any of our activities are listening to it!
            String deepLink = (uriString != null) ? "tl-" + uriString + "://" : "";
            Intent testIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(deepLink));
            try {
                @SuppressLint("WrongConstant")
                List<ResolveInfo> list = mgr.queryIntentActivities(testIntent, PackageManager.GET_INTENT_FILTERS | PackageManager.MATCH_DEFAULT_ONLY); // |
                return list.size() > 0;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    public Class getPushBroadcastReceiver() {
        return pushBroadcastReceiver != null ? pushBroadcastReceiver : TLGcmBroadcastReceiver.class;
    }

    private Class pushBroadcastReceiver = null;

    /**
     * Determine whether or not the application has the new push receiver filters set up.
     *
     * @return Whether or not the user has set up the application to listen for push actions.
     */
    @SuppressWarnings("WrongConstant")
    boolean hasImplementedNewPush() {
        if (appContext != null) {
            final PackageManager mgr = appContext.getPackageManager();
            Intent testIntent = new Intent(TLPushManager.OPEN_ACTION);
            try {
                @SuppressLint("WrongConstant")
                List<ResolveInfo> list = mgr.queryBroadcastReceivers(testIntent, PackageManager.GET_INTENT_FILTERS);
                if (list != null) {
                    for (ResolveInfo resolveInfo : list) {
                        ActivityInfo activityInfo = resolveInfo != null ? resolveInfo.activityInfo : null;

                        if (activityInfo != null && activityInfo.packageName != null && activityInfo.name != null) {
                            if (appContext.getPackageName().equals(activityInfo.packageName)) {
                                pushBroadcastReceiver = Class.forName(activityInfo.name);
                                return true;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                TLLog.error("Error querying broadcast receivers", e);
                return false;
            }
        }
        return false;
    }


    /**
     * Grab the most recent release tag from github. If we get one, check if an update is available.
     * Only check on liveUpdate builds
     */
    private void checkGitTagForUpdate() {
        if (!liveUpdate) return;

        tlNetworking.getCurrentReleaseTag(new TLNetworking.TLNetworkResponseListener() {
            @Override
            public void onResponse(JSONObject response) {
                TLLog.debug("Got tag response: " + response);
                if (response != null && response != JSONObject.NULL && response.length() > 0 && response.has("tag_name")) {
                    updateAvailable = checkIfSDKUpdate(new Version(response.optString("tag_name")));
                    if (updateAvailable) {
                        // NON TLLog log to users.
                        Log.d(TLLog.getTag(), "A Taplytics SDK update is available. Please update to ensure best functionality");
                    }
                }
            }

            @Override
            public void onError(Throwable error) {
                TLLog.error("git tag error", error);
            }
        });
    }

    /**
     * @param upstream The version code that we have received from GitHub
     * @return Whether or not an update is available.
     */
    private boolean checkIfSDKUpdate(Version upstream) {
        try {
            int comparison = TAPLYTICS_SDK_VERSION.compareTo(upstream);
            switch (comparison) {
                case -1:
                    return true;
                default:
                    return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @param props   Map of various properties to append to the request for tlproperties
     * @param promise A network promise. Finish the promise once we get a response.
     */
    public void getPropertiesFromServer(Map<String, Object> props, final Promise<?> promise) {
        if (!isActive || tlNetworking == null)
            return;

        //Create a new promise that listeners can listen to if they want to.
        tlPropertiesPromise = tlPropertiesPromise.isComplete() ? ((promise == null) ? new Promise<>() : promise) : tlPropertiesPromise;

        if (pendingRunningExpListeners == null) {
            this.pendingRunningExpListeners = new ArrayList<>();
        }
        //Queue up our sending of experiment data to external sources
        pendingRunningExpListeners.add(new TaplyticsRunningExperimentsListener() {
            @Override
            public void runningExperimentsAndVariation(Map<String, String> experimentsAndVariations) {
                TLExternalAnalyticsManager.getInstance().sendExperimentDataToAnalyticsSources(experimentsAndVariations, null);
            }
        });

        tlNetworking.getPropertiesFromServer(props, new TLNetworking.TLNetworkPropertiesResponseListener() {
            @Override
            public void onResponse(TLProperties properties) {
                TLLog.debug("Got TLProperties!!");
                try {
                    hasLoadedPropertiesFromServer = true;
                    Boolean hasOldProps = (tlProperties != null);

                    if (hasOldProps) {
                        properties.setOldGeofences(tlProperties.getGeofences());
                    }

                    //If we timed out, we're showing users stuff from cache so we don't want to update this.
                    if (!isExperimentLoadTimeout()) {
                        tlProperties = properties;
                        TLViewManager.getInstance().applyTouchScreenIfViewPresentInActivity();
                        TLViewManager.getInstance().applyTaplyticsOverlaysToDialogsIfNecessary();
                        TLLog.debug("Got properties successfully!", true);
                    } else if (tlProperties != null && tlProperties.getLastSessionId() != null || (tlProperties != null && isTest())) {
                        //Use the last saved session id on disk for this session
                        tlProperties.setSessionID(tlProperties.getLastSessionId());
                        TLLog.debug("Getting properties timed out. Loading from cache", true);
                    }

                    if (shouldBeKilled(properties)) {
                        return;
                    }

                    //If we've timed out on the first load, we still need this id to be able to connect to our socket room.
                    //Dev clientconfig calls are notoriously slow, so this is for safety.
                    if (properties != null && tlDeviceInfo != null && properties.getProject() != null) {
                        tlDeviceInfo.setPid(properties.getProject().optString("_id"));
                    }

                    //Now that we have TLProperties, update any disabled / enabled functionality.
                    TLKillSwitchManager.getInstance().updateDisabledFunctionality(properties);

                    checkLiveUpdate(false, null);
                    tlAnalytics.getTlAppUser().flushAppUserAttributes(null);
                    performPendingRunningExpListeners();
                    TLFeatureFlagManager.getInstance().performPendingRunningFlagListeners();

                    if (liveUpdate) {
                        if (!TLSocketManager.getInstance().isConnected)
                            TLSocketManager.getInstance().connectSocketIO(null, false);

                        if (properties != null && properties.getDeepLink() != null && isListeningForDeeplink(properties.getDeepLink()) && !sentAppLink)
                            TLSocketManager.getInstance().sendHasAppLinking();
                    }

                    // check for push token here (make sure you check only once, global flag)
                    if (appContext != null) {
//                        if (TLUtils.checkHasAndroidPermission(appContext, "com.google.android.c2dm.permission.RECEIVE")) {
                        TLLog.debug("Getting Push token");
//                        }
                        tlPush.getToken(isResettingUser);
                    }

                    //Finish both promises.
                    if (tlPropertiesPromise != null) {
                        tlPropertiesPromise.finish();
                    }

                    if (experimentsLoadedPromise != null) {
                        experimentsLoadedPromise.finish();
                    }

                    //if the new geofences are different than the old ones

                    //if there is a clientControl flag to clear the event cache
                    if (properties != null && properties.getClientControl() != null) {
                        if (properties.getClientControl().optBoolean("clearEventQueue", false)) {
                            getTlAnalytics().clearEventQueue();
                        }
                    }

                    if (!hasOldProps) {
                        tlAnalytics.startAnalyticsSourceTracking();
                    }

                    if (experimentUpdatedListener != null && isLiveUpdate()) {
                        experimentUpdatedListener.onExperimentUpdate();
                    }


                } catch (Exception e) {
                    TLLog.error("error processing tlproperties response", e);
                }
            }

            @Override
            public void onError(Throwable error) {
                TLLog.error("Getting Properties from server", error);
                performPendingRunningExpListeners();
                TLFeatureFlagManager.getInstance().performPendingRunningFlagListeners();
                if (promise != null) {
                    promise.cancel();
                }
                if (tlPropertiesPromise != null) {
                    tlPropertiesPromise.cancel();
                }
            }
        });
    }


    /**
     * Check whether or not live updates are to occur. If the app is in debug mode, we may want to allow it. However, if the user has
     * disabled liveUpdates, turn it off
     *
     * @param isInit  Whether or not we've initialized this all yet.
     * @param options the provided options to Taplytics. Used to determine if {@link #userSetLiveUpdate}
     */

    private void checkLiveUpdate(boolean isInit, Map options) {
        try {
            final boolean setLive;
            JSONObject enabledObj = null;
            boolean fromUserDevice = false;
            boolean socketThreads = false;
            String debugCheckType = "";

            if (tlProperties != null && tlProperties.getProject() != null) {
                JSONObject settings = tlProperties.getProject().optJSONObject("settings");
                if (settings != null) {
                    enabledObj = settings.optJSONObject("enabledForReleaseModes");
                    fromUserDevice = settings.optBoolean("setFromUserDevice", false);
                    socketThreads = settings.optBoolean("socketThreads", false);
                    debugCheckType = settings.optString("debugCheckType");
                    try {
                        boolean aggressive = settings.getBoolean("aggressive");
                        TLViewManager.getInstance().setAggressiveViewChanges(aggressive);
                    } catch (Throwable ignored) {

                    }
                }
            }

            if (options != null) {
                if (options.containsKey("isDebug") && options.get("isDebug") instanceof Boolean) {
                    isDebug = (boolean) options.get("isDebug");
                }
                if (debugCheckType.equals("") && options.containsKey("debugCheckType") && options.get("debugCheckType") instanceof String) {
                    debugCheckType = (String) options.get("debugCheckType");
                }
            }

            if (socketThreads) {
                TLSocketManager.getInstance().setSocketConnectOnMainThread(true);
            }


            if (!debugCheckType.equals("")) {
                this.debugCheckType = TLUtils.DebugCheckType.fromString(debugCheckType);
            }

            if (isDebug == null) {
                isDebug = TLUtils.checkIsDebug(appContext);
            }

            if (enabledObj != null) {
                if (isDebug)
                    setLive = enabledObj.optBoolean("dev", true);
                else
                    setLive = enabledObj.optBoolean("appStore", false);
            } else {
                setLive = isDebug;
            }

            if (options != null) {
                if (options.containsKey("liveUpdate")) {
                    liveUpdate = (Boolean) options.get("liveUpdate");
                    userSetLiveUpdate = true;
                }
            }

            //This is still magic
            if (!userSetLiveUpdate && thereAreSockets) {
                if (fromUserDevice) {
                    boolean updateData = (liveUpdate != setLive);
                    this.liveUpdate = setLive;
                    if (updateData && !isInit) {
                        getPropertiesFromServer(null, null);
                    }
                } else {
                    this.liveUpdate = setLive;
                }
            }

            // If developer has old socket lib then show a dialog
            if (isDebug && !thereAreSockets && (setLive || userSetLiveUpdate)) {
                try {
                    Class.forName("com.github.nkzawa.socketio.client.Socket");
                    //throw up a dialog when we get an activity
                    showSocketDialog = true;
                } catch (Exception e) {
                    //its all good, this is what we want
                }
            }
            if (!thereAreSockets && liveUpdate) {
                Log.w("Taplytics", "Device is in liveUpdate mode, but there is no socket library!");
            }
            setupShakeMenu();
        } catch (Throwable ignored) {
            //
        }
    }


    /**
     * Method to call into {@link TLCodeBlockManager} from {@link com.taplytics.sdk.Taplytics} to increase obfuscation
     * (instead of calling directly into the manager).
     *
     * @param name     name of code block
     * @param listener listener for codeblock
     */
    public void runCodeBlock(String name, CodeBlockListener listener) {
        TLCodeBlockManager.getInstance().runCodeBlock(name, listener);
    }


    /**
     * Method to call into {@link TLCodeBlockManager} from {@link com.taplytics.sdk.Taplytics} to increase obfuscation
     * (instead of calling directly into the manager).
     *
     * @param name     name of code block
     * @param listener listener for codeblock
     */
    public void runCodeBlockSync(String name, CodeBlockListener listener) {
        TLCodeBlockManager.getInstance().runCodeBlockSync(name, listener);
    }


    /**
     * Get the variables from tlProperties.
     *
     * @param variables     JSONObject of variables from TLProperties
     * @param variableNames An array of variable names to match up with the variables
     * @return A Mapping of Variable Name - Variable
     * @throws JSONException
     */
    private Map<String, Object> getVariablesForExperiment(JSONObject variables, JSONArray variableNames) throws JSONException {
        Map<String, Object> varDic = new HashMap<>();
        try {
            for (int i = 0; i < variableNames.length(); i++) {
                TLJSONObject varName = new TLJSONObject(variableNames.getJSONObject(i));
                String id = (varName.hasValue("_id")) ? varName.optString("_id") : "";
                String name = (varName.hasValue("name")) ? varName.optString("name") : "";
                if (id != null && name != null && !id.equals("") && !name.equals("")) {
                    if (variables.has(id))
                        varDic.put(name, variables.get(id));
                }
            }
        } catch (Exception e) {
            TLLog.error("Getting vars", e);
        }
        return varDic;
    }

    /**
     * Change the variation to the given parameters.
     */
    public void switchVariation(final String experimentId, final String variationId,
                                final String experimentName, final String variationName) {
        try {
            Map<String, Object> expMap = new HashMap<>();
            expMap.put("exp", experimentId);
            expMap.put("var", variationId);

            //Here is some magic stuff.
            //Essentially what this does is determine if a view needs to be changed back to its original stag
            if (TLViewManager.getInstance().isTestingExperiment()) {
                if (TLViewManager.getInstance().getVariationName() != null && !TLViewManager.getInstance().getVariationName().equals(variationName)) {
                    JSONArray views = tlProperties.getViews();
                    for (int i = 0; i < views.length(); i++) {
                        JSONObject viewObject = (JSONObject) views.get(i);
                        JSONObject initProps = viewObject.optJSONObject("initProperties");
                        int id = initProps.optInt("anID");
                        //If the view isn't in the map of views to reset, add it in there.
                        if (!TLViewManager.getInstance().getViewsToReset().containsKey(id)
                                || !((JSONObject) TLViewManager.getInstance().getViewsToReset().get(id)).has("anProperties")) {
                            viewObject.put("reset", true);
                            TLViewManager.getInstance().getViewsToReset().put(id, viewObject);
                        }
                    }
                }
            }

            Promise<Object> tlPropertiesPromise = new Promise<>();

            tlPropertiesPromise.add(new PromiseListener<Object>() {
                @Override
                public void succeeded() {
                    TLViewManager.getInstance().applyConnectedBorder(experimentName, variationName);
                    TLDynamicVariableManager.getInstance().notifyChangeIfNecessary(experimentName, variationName);
                    TLViewManager.getInstance().updateViewsIfNecessary();
                    TLViewManager.getInstance().updateDialogsIfNecessary();

                    super.succeeded();
                }
            });

            TLManager.getInstance().getPropertiesFromServer(expMap, tlPropertiesPromise);
        } catch (Exception e) {
            TLLog.error("error switching variations", e);
        }
    }

    /**
     * Change the variation to the given parameters.
     */
    public void switchFeatureFlag(final String flagName, final String flagKey,
                                  final String flagId) {
        try {
            Map<String, Object> flagMap = new HashMap<>();
            flagMap.put("exp", flagId);

            Promise<Object> tlPropertiesPromise = new Promise<>();

            //Here is some magic stuff.
            //Essentially what this does is determine if a view needs to be changed back to its original stag
            if (TLViewManager.getInstance().isTestingExperiment()) {
                if (TLViewManager.getInstance().getVariationName() != null && !TLViewManager.getInstance().getVariationName().equals(flagKey)) {
                    JSONArray views = tlProperties.getViews();
                    for (int i = 0; i < views.length(); i++) {
                        JSONObject viewObject = (JSONObject) views.get(i);
                        JSONObject initProps = viewObject.optJSONObject("initProperties");
                        int id = initProps.optInt("anID");
                        //If the view isn't in the map of views to reset, add it in there.
                        if (!TLViewManager.getInstance().getViewsToReset().containsKey(id)
                                || !((JSONObject) TLViewManager.getInstance().getViewsToReset().get(id)).has("anProperties")) {
                            viewObject.put("reset", true);
                            TLViewManager.getInstance().getViewsToReset().put(id, viewObject);
                        }
                    }
                }
            }

            tlPropertiesPromise.add(new PromiseListener<Object>() {
                @Override
                public void succeeded() {
                    TLViewManager.getInstance().applyConnectedBorder(flagName, flagKey);
                    TLDynamicVariableManager.getInstance().notifyChangeIfNecessary(flagName, flagKey);
                    TLViewManager.getInstance().updateViewsIfNecessary();
                    TLViewManager.getInstance().updateDialogsIfNecessary();
                    super.succeeded();
                }
            });

            TLManager.getInstance().getPropertiesFromServer(flagMap, tlPropertiesPromise);
        } catch (Exception e) {
            TLLog.error("error switching feature flags", e);
        }
    }

    public void experimentUpdated(String exp_id, final String exp_name, final String var_name) {

        Map<String, Object> expMap = new HashMap<>();
        expMap.put("exp", exp_id);

        Promise<Object> tlPropertiesPromise = new Promise<>();
        tlPropertiesPromise.add(new PromiseListener<Object>() {

            @Override
            public void succeeded() {
                TLDynamicVariableManager.getInstance().notifyChangeIfNecessary(exp_name, var_name);
                TLViewManager.getInstance().updateViewsIfNecessary();
                TLViewManager.getInstance().updateDialogsIfNecessary();
                TLViewManager.getInstance().addUpdatedToBorder();
                super.succeeded();
            }
        });

        TLManager.getInstance().getPropertiesFromServer(expMap, tlPropertiesPromise);
    }

    private ArrayList<TaplyticsRunningExperimentsListener> pendingRunningExpListeners;

    /**
     * Get the running experiments and variations
     *
     * @param listener {@link TaplyticsRunningExperimentsListener}
     */
    public void getRunningExperimentsAndVariations(
            final TaplyticsRunningExperimentsListener listener) {
        if (!isActive) {
            listener.runningExperimentsAndVariation(null);
            return;
        }

        if (hasLoadedPropertiesFromServer || isExperimentLoadTimeout()) {
            sendRunningExperimentsAndVariations(listener);
        } else {
            if (pendingRunningExpListeners == null)
                this.pendingRunningExpListeners = new ArrayList<>();
            pendingRunningExpListeners.add(listener);
        }
    }

    /**
     * Perform callbacks for running experiments and variations
     */
    private void performPendingRunningExpListeners() {
        if (pendingRunningExpListeners != null) {
            try {
                ArrayList<TaplyticsRunningExperimentsListener> listeners = (ArrayList) pendingRunningExpListeners.clone();
                pendingRunningExpListeners = null;
                for (TaplyticsRunningExperimentsListener listener : listeners) {
                    sendRunningExperimentsAndVariations(listener);
                }
            } catch (Throwable e) {
                TLLog.error("Error running listeners", e);
            }
        }

    }

    /**
     * Send running experiments and variations to callback
     *
     * @param listener {@link TaplyticsRunningExperimentsListener}
     */
    private void sendRunningExperimentsAndVariations(TaplyticsRunningExperimentsListener
                                                             listener) {
        Map<String, String> expVarMap = new HashMap<>();
        if (tlProperties != null && tlProperties.getExperiments() != null) {
            JSONArray experiments = tlProperties.getExperiments();
            JSONObject projectSettings = tlProperties.getProject() != null ? tlProperties.getProject().optJSONObject("settings") : null;
            Boolean showDraftExperiments = projectSettings != null && projectSettings.optBoolean("showDraftExperiments");

            try {
                TLJSONArray experiment_ids = tlProperties.getExperimentIDs() != null ? new TLJSONArray(tlProperties.getExperimentIDs()) : null;

                for (int i = 0; i < experiments.length(); i++) {
                    JSONObject exp = experiments.optJSONObject(i);
                    if (exp == null) {
                        continue;
                    }
                    String exp_id = exp.optString("_id");
                    String status = exp.optString("status");

                    // If the showDraftExperiments project setting is enabled allow through draft and active experiments.
                    if (exp_id != null && experiment_ids != null && experiment_ids.containsString(exp_id) && status != null &&
                            ((showDraftExperiments && status.equals("draft")) || status.equals("active"))) {
                        String expName = exp.has("name") ? exp.optString("name") : exp.optString("_id");
                        String varName = "baseline";
                        JSONArray variations = exp.optJSONArray("variations");

                        // check if experiment variation exists in the list of running variations
                        if (variations != null && tlProperties.getVariationIDs() != null) {
                            for (int v = 0; v < variations.length(); v++) {
                                TLJSONObject var = new TLJSONObject(variations.optJSONObject(v));
                                if (var.hasValue("_id")
                                        && tlProperties.getVariationIDs().toString().contains("\"" + var.optString("_id") + "\"")) {
                                    varName = var.has("name") ? var.optString("name") : var.optString("_id");
                                    break;
                                }
                            }
                        }
                        expVarMap.put(expName, varName);
                    }
                }
            } catch (Exception ex) {
                TLLog.error("Getting running experiments and variations", ex);
            } finally {
                listener.runningExperimentsAndVariation(expVarMap);
            }
        } else {
            listener.runningExperimentsAndVariation(expVarMap);
        }
        TLLog.debug("Running experiments and variations: " + expVarMap.toString(), true);
    }

    /**
     * TLManager is our shake listener. Pass info over to the shake manager if we've been shaken.
     */
    @SuppressLint("NewApi")
    @Override
    public void onShake() {
        TLLog.debug("Shake Menu triggered");
        try {
            if (shakeManager != null && !TLDialogManager.getInstance().dialogsShowing() && !TLUtils.isDisabled(Functionality.SHAKEMENU)
                    && isLiveUpdate() && isActivityActive) {
                TLDialogManager.getInstance().setupExperimentDialog();
            }
            if (isActive && isLiveUpdate() && !TLSocketManager.getInstance().isConnected) {
                TLViewManager.getInstance().applyDisconnectBorder();
            }
        } catch (Exception e) {
            TLLog.error("onshake", e);
        }
    }


    /**
     * In debug, this is what forces updates directly from the socket connection.
     *
     * @param json The View or Image being updated.
     */
    public void updateDataFromSocket(JSONObject json) {
        if (json == null || tlProperties == null || TLUtils.isDisabled(Functionality.VISUAL))
            return;

        JSONObject view = json.optJSONObject("ViewElement");
        JSONObject image = json.optJSONObject("Image");
        JSONObject variable = json.optJSONObject("Variable");
        if (view != null) {
            updateViewFromSocket(view);
        }
        if (image != null) {
            updateImageFromSocket(image);
        }
        if (variable != null) {
            TLDynamicVariableManager.getInstance().updateFromSocket(variable);
        }
        TLViewManager.getInstance().addUpdatedToBorder();

        TLLog.debug("Visual edit successfully applied!", true);
    }

    /**
     * Update the given view from a socket. Iterate through the views and remove old ones that aren't being changed any more. Then, update
     * the new one
     *
     * @param view JSonobject of view info.
     */
    private void updateViewFromSocket(JSONObject view) {
        String key = view.optString("key");
        Boolean android = view.optBoolean("android");
        if (key != null && android) {
            TLLog.debug("Got Updated View from Socket: " + key);
            try {
                JSONArray jViews = tlProperties.getViews();
                TLJSONArray views = (jViews != null) ? new TLJSONArray(jViews) : new TLJSONArray();
                for (int i = 0; i < views.length(); i++) {
                    JSONObject obj = views.optJSONObject(i);
                    if (obj != null) {
                        String objKey = obj.optString("key");
                        if (objKey != null && key.equals(objKey)) {
                            views = views.removeIndex(i);
                        }
                    }
                }
                views.put(view);
                tlProperties.setViews(views.getJSONArray());
            } catch (JSONException e) {
                TLLog.error("Update View From Socket JSONException", e);
            }

            TLViewManager.getInstance().updateViewsIfNecessary();
            TLViewManager.getInstance().updateDialogsIfNecessary();
        }
    }

    /**
     * Update images from socket.
     *
     * @param image The image being updated
     */
    private void updateImageFromSocket(JSONObject image) {
        String image_id = image.optString("_id");
        if (!image_id.equals("") && TLManager.getInstance().getAppContext() != null && tlProperties != null) {
            TLLog.debug("Got Update Image From Socket: " + image_id);
            try {
                JSONObject images = tlProperties.getImages();
                if (images == null) {
                    images = new JSONObject();
                }
                // Get rid of the old one.
                if (images.has(image_id)) {
                    images.remove(image_id);
                }
                // Add the new one.
                images.put(image_id, image);

                // This is so we can re-use the async task that is used in TLProperties
                // which in production saves all the images at once
                JSONObject singleImageJSON = new JSONObject();
                singleImageJSON.put(image_id, image);
                // Save the image to disk.
                TLUtils.executeAsyncTask(new ImageUtils.SaveImageToDiskTask(), singleImageJSON);
            } catch (JSONException e) {
                TLLog.error("Updating Image From Socket JSONException", e);
            }
        }
    }

    private AlertDialog pairAlert = null;

    private void destroyPairAlert() {
        this.pairAlert = null;
    }

    public void pairTokenSuccessful() {
        if (!liveUpdate) {
            TLLog.debug("live update being set to true due to pair token.", true);
            liveUpdate = thereAreSockets;
        }

        setupShakeMenu();

        this.getPropertiesFromServer(null, null);

        final Activity activity = getCurrentActivity();
        if (activity != null && isActivityActive) {
            try {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle("Taplytics Pairing Successful!");
                builder.setCancelable(false);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        destroyPairAlert();
                    }
                });

                pairAlert = builder.create();
                pairAlert.show();
            } catch (Exception e) {
                TLLog.error("Error showing pair dialog", e);
            }
        }
    }

    public void resetTLProperties() {
        this.tlProperties = null;
    }

    /**
     * Determines whether or not Taplytics should be completely disabled for this build of the app *
     **/
    private boolean shouldBeKilled(TLProperties tlProperties) {
        if (tlProperties != null) {
            JSONObject project = tlProperties.getProject();
            if (project != null) {
                JSONArray killBuilds = project.optJSONObject("settings").optJSONArray("killForBuilds");
                int currentCode = TLUtils.getAppCode();
                String currentVersion = TLUtils.getAppVersion();
                try {
                    if (killBuilds != null) {
                        for (int i = 0; i < killBuilds.length(); i++) {
                            String buildCode = ((JSONObject) killBuilds.get(i)).optString("appBuild");
                            String buildVersion = ((JSONObject) killBuilds.get(i)).optString("appVersion");
                            if (String.valueOf(currentCode).equals(buildCode) || currentVersion.equals(buildVersion)) {
                                isActive = false;
                                TLLog.debug("TAPLYTICS IS NOW DISABLED");
                                try {
                                    //Just unregister our shakemanager JUST in case
                                    if (shakeManager != null) {
                                        shakeManager.unregister();
                                    }
                                    if (activityCallbacks != null) {
                                        TLActivityLifecycleCallbacks.unregisterCallbacks(getActivityCallbacks());
                                    }
                                    if (tlAnalytics != null) {
                                        TLExternalAnalyticsManager.getInstance().unRegisterExternalAnalytics();
                                    }
                                    if (tlPush != null && tlPush.hasCheckedPushToken()) {
                                        tlPush.clearListeners();
                                    }
                                } catch (Exception e) {
                                    TLLog.error("error unregistering things for killswitch", e);
                                }
                                return !isActive;
                            }
                        }
                        isActive = true;
                    }
                } catch (Exception ex) {
                    TLLog.error("error checking kill builds", ex);
                }
            }
        }
        return !isActive;
    }

    public void setSentAppLink(boolean sentAppLink) {
        this.sentAppLink = sentAppLink;
    }

    public void setPushTokenListener(TaplyticsPushTokenListener listener) {
        if (getTlPushManager() != null) {
            getTlPushManager().setPushTokenListener(listener);
        }
    }

    public boolean isTurnMenuEnabled() {
        return turnMenuEnabled;
    }

    public void showMenu() {
        if (isActivityActive() &&
                isLiveUpdate() &&
                getTrackingEnabled()
                && !TLDialogManager.getInstance().dialogsShowing()) {
            TLDialogManager.getInstance().setupExperimentDialog();
        }
    }

    public void setResettingUser(boolean resettingUser) {
        isResettingUser = resettingUser;
    }

    public void executeRunnable(Runnable runnable) {
        if (executor != null) {
            executor.execute(runnable);
        }
    }

    public void setExperimentUpdatedListener(TaplyticsExperimentsUpdatedListener listener) {
        experimentUpdatedListener = listener;
    }

    public void setNewSessionListener(TaplyticsNewSessionListener listener) {
        newSessionListener = listener;
    }

    public void notifyNewSessionStarted() {
        if (newSessionListener == null) {
            return;
        }

        newSessionListener.onNewSession();
    }

    public void notifyErrorStartingNewSession() {
        if (newSessionListener == null) {
            return;
        }

        newSessionListener.onError();
    }

    public boolean isLiveUpdateSetByDev() {
        return userSetLiveUpdate;
    }

    public String getDeviceToken() {
        try {
            if (TLUtils.isEmptyString(deviceToken)) {
                SecurePrefs prefs = SecurePrefs.getInstance();
                deviceToken = prefs != null ? prefs.getAndDecryptString(KEY_DEVICE_TOKEN) : null;
                TLLog.debug("Getting device token from prefs: " + deviceToken);
            }
        } catch (Throwable e) {
            TLLog.error("gdevt", e);
            return null;
        }
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        try {
            this.deviceToken = deviceToken;
            SecurePrefs prefs = SecurePrefs.getInstance();
            if (prefs != null) {
                prefs.put(KEY_DEVICE_TOKEN, deviceToken);
                TLLog.debug("Saving device token to prefs: " + deviceToken);
            }
        } catch (Throwable e) {
            TLLog.error("sdevt", e);
        }
    }

    public void setupTLAnalytics() {
        tlAnalytics = new TLAnalyticsManager();
    }

    public boolean isTest() {

        return isTest;
    }

    private boolean base64Enabled = true;

    public boolean base64Enabled() {
        return base64Enabled && !TLUtils.isDisabled(Functionality.BASE64);
    }

    public Promise getTlPropertiesDiskPromise() {
        return tlPropertiesDiskPromise;
    }

    private static class SynchronousExecutor implements Executor {
        @Override
        public void execute(@NonNull Runnable command) {
            command.run();
        }
    }

    public static boolean socketsEnabled() {
        try {
            Class.forName("io.socket.client.Socket");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    //Here to mock for testing.
    public byte[] getAppIconBytes() {
        return ImageUtils.getAppIconBytes();
    }

    public Map<String, Object> getStartingOptions() {
        return startingOptions;
    }

    public void setTlNetworkingMock(TLNetworking tlNetworkingMock) {
        tlNetworking = tlNetworkingMock;
    }
}
