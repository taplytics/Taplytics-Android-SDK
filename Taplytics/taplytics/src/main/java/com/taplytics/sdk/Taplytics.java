/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.taplytics.sdk.analytics.TLAnalyticsManager;
import com.taplytics.sdk.analytics.external.TLExternalAnalyticsManager;
import com.taplytics.sdk.listeners.TLFlushListener;
import com.taplytics.sdk.managers.TLDelayLoadImageManager;
import com.taplytics.sdk.managers.TLDelayLoadManager;
import com.taplytics.sdk.managers.TLFeatureFlagManager;
import com.taplytics.sdk.managers.TLKillSwitchManager;
import com.taplytics.sdk.managers.TLManager;
import com.taplytics.sdk.managers.TLPushManager;
import com.taplytics.sdk.managers.TLViewManager;
import com.taplytics.sdk.network.TLSocketManager;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Main class for using Taplytics. Call {@link #startTaplytics(Context, String)} with the main application context and your Taplytics
 * apiKey.
 * </p>
 * <p/>
 * <p>
 * Once you have started Taplytics, you can log events using {@link #logEvent(String, Number, JSONObject)}. You can also create {@link TaplyticsVar Dynamic Variables}
 * </p>
 * <p/>
 * <p>
 * Taplytics will retrieve experiment configurations from our servers and log event information back to our servers. Your application will
 * require the android permission: <tt>android.permission.INTERNET</tt>.
 * </p>
 * <p/>
 * <p>
 * Example Taplytics implementation:
 * </p>
 * <p/>
 * <p/>
 * <pre>
 * <code>
 *  public class ExampleApplication extends Application {
 *      {@literal @}Override
 *      public void onCreate() {
 *          super.onCreate();
 *          Taplytics.startTaplytics(this, "YOUR TAPLYTICS API KEY");
 *      }
 *
 *      public void somethingAwesomeHappened() {
 *          Taplytics.logEvent("somethingAwesome", 42, null);
 *      }
 *
 *      public void loadedUserInfo(User user) {
 *          try {
 *              JSONObject attributes = new JSONObject();
 *              attributes.put("email", "johnDoe@taplytics.com");
 *              attributes.put("name", "John Doe");
 *              attributes.put("age", 25);
 *              attributes.put("gender", "male");
 *              attributes.put("avatarUrl", "https://pbs.twimg.com/profile_images/497895869270618112/1zbNvWlD.png");
 *
 *              JSONObject customData = new JSONObject();
 *              customData.put("paidSubscriber", true);
 *              customData.put("subscriptionPlan", "yearly");
 *              attributes.put("customData", customData);
 *
 *              Taplytics.setUserAttributes(attributes);
 *          } catch (JSONException e) { }
 *      }
 * }
 * </code>
 * </pre>
 */

@SuppressWarnings("unused")
public class Taplytics {

    public static final int TAPLYTICS_DEFAULT_TIMEOUT = 4000;

    /**
     * Start Taplytics SDK with your associated API Key. Call this method when your Application is created.
     * <p/>
     * Do not place this in an Activity onCreate, it must be an Application class.
     *
     * @param appContext application context to track
     * @param apiKey     your Taplytics API Key
     */
    public static void startTaplytics(Context appContext, String apiKey) {
        TLManager.getInstance().startTaplytics(appContext, apiKey, null, TAPLYTICS_DEFAULT_TIMEOUT, null);
    }

    /**
     * Start Taplytics SDK with your associated API Key. Call this method when your Application is created as well as a timeout.
     * <p/>
     * If the timeout is reached, taplytics will use only values loaded from cache or the default values.
     * Taplytics will continue to cache values in the background, but those will not be used until the next session.
     *
     * @param appContext application context to track
     * @param apiKey     your Taplytics API Key
     * @param timeout    a timeout (in millis) for taplytics loading. The default is 4 seconds.
     */
    public static void startTaplytics(Context appContext, String apiKey, int timeout) {
        TLManager.getInstance().startTaplytics(appContext, apiKey, null, timeout, null);
    }

    /**
     * Optionally Start Taplytics with your associated API Key and specific options. Call this method in when your Application is created.
     * <p/>
     * Options include: - "liveUpdate" true or false. Taplytics will auto-detect an app store or development build, but to force production
     * mode set to false, or true for development mode.
     *
     * @param appContext application context to track
     * @param apiKey     your Taplytics API Key
     * @param options    Map of Taplytics start options
     */
    public static void startTaplytics(Context appContext, String apiKey, HashMap<String, Object> options) {
        TLManager.getInstance().startTaplytics(appContext, apiKey, options, TAPLYTICS_DEFAULT_TIMEOUT, null);
    }

    /**
     * Optionally Start Taplytics with your associated API Key and specific options. Call this method in when your Application is created.
     * <p/>
     * Options include: - "liveUpdate" true or false. Taplytics will auto-detect an app store or development build, but to force production
     * mode set to false, or true for development mode.
     * <p/>
     * If the timeout is reached, taplytics will use only values loaded from cache or the default values.
     * Taplytics will continue to cache values in the background, but those will not be used until the next session.
     *
     * @param appContext application context to track
     * @param apiKey     your Taplytics API Key
     * @param options    Map of Taplytics start options
     * @param timeout    a timeout (in millis) for taplytics loading. The default is 4 seconds.
     */

    public static void startTaplytics(Context appContext, String apiKey, Map<String, Object> options, int timeout) {
        TLManager.getInstance().startTaplytics(appContext, apiKey, options, timeout, null);
    }

    /**
     * Optionally start taplytics with your options, as well as a listener ({@link TaplyticsExperimentsLoadedListener}) that will be triggered when
     * Taplytics has completed loading experiments from the server, or when the default timeout of 4s has been reached.
     *
     * @param appContext application context to track
     * @param apiKey     your Taplytics API Key
     * @param options    Map of Taplytics start options
     * @param listener   {@link TaplyticsExperimentsLoadedListener} to inform when experiments have been fully loaded.
     */
    public static void startTaplytics(Context appContext, String apiKey, HashMap<String, Object> options, TaplyticsExperimentsLoadedListener listener) {
        TLManager.getInstance().startTaplytics(appContext, apiKey, options, TAPLYTICS_DEFAULT_TIMEOUT, listener);
    }


    /**
     * Optionally start taplytics with your options, as well as a listener and set the timeout for Taplytics loading.
     * The listener is a ({@link TaplyticsExperimentsLoadedListener}) that will be triggered when Taplytics has completed loading experiments from the server, or when this timeout has been reached.
     * <p/>
     * If the timeout is reached, taplytics will use only values loaded from cache or the default values.
     * Taplytics will continue to cache values in the background, but those will not be used until the next session.
     *
     * @param appContext application context to track
     * @param apiKey     your Taplytics API Key
     * @param options    Map of Taplytics start options
     * @param listener   {@link TaplyticsExperimentsLoadedListener} to inform when experiments have been fully loaded.
     * @param timeout    a timeout (in millis) for taplytics loading. The default is 4 seconds.
     */
    public static void startTaplytics(Context appContext, String apiKey, HashMap<String, Object> options, int timeout, TaplyticsExperimentsLoadedListener listener) {
        TLManager.getInstance().startTaplytics(appContext, apiKey, options, timeout, listener);
    }


    /**
     * Checks if a feature flag for an associated key is enabled
     *
     * @param key   feature flag key
     */
    public static boolean featureFlagEnabled(final String key) {
        return featureFlagEnabled(key, false);
    }

    /**
     * Checks if a feature flag for an associated key is enabled
     *
     * @param key   feature flag key
     * @param defaultValue   default value of the feature flag. If there is no connection or the feature flag does not load then default value will be returned
     */
    public static boolean featureFlagEnabled(final String key, final boolean defaultValue) {
        return TLFeatureFlagManager.getInstance().featureFlagEnabled(key, defaultValue);
    }


    /**
     * Runs a codeblock if the variation has it enabled. Will check the server first for information before deciding to run.
     *
     * @param name     code block name
     * @param listener {@link CodeBlockListener}
     */
    public static void runCodeBlock(final String name, final CodeBlockListener listener) {
        TLManager.getInstance().executeRunnable(new Runnable() {
            @Override
            public void run() {
                TLManager.getInstance().runCodeBlock(name, listener);
            }
        });
    }


    /**
     * Force the saving of a push token
     */
    public static void savePushToken(final String token){
        TLManager.getInstance().executeRunnable(new Runnable() {
            @Override
            public void run() {
                TLManager.getInstance().getTlPushManager().savePushtoken(token, true);
            }
        });
    }

    /**
     * Runs a codeblock if the variation has it enabled. Synchronous, so if it hasn't received information from the server of whether or not it should run, it will NOT run.
     *
     * @param name     code block name
     * @param listener {@link CodeBlockListener}
     */
    public static void runCodeBlockSync(final String name, final CodeBlockListener listener) {
        TLManager.getInstance().executeRunnable(new Runnable() {
            @Override
            public void run() {
                TLManager.getInstance().runCodeBlockSync(name, listener);
            }
        });
    }

    /**
     * Get all running experiments and variations.
     *
     * @param listener which returns a HashMap of all currently running experiments and their variations.
     *                 May return async, as it waits for properties to be loaded from Taplytics servers.
     */
    public static void getRunningExperimentsAndVariations(final TaplyticsRunningExperimentsListener listener) {
        TLManager.getInstance().executeRunnable(new Runnable() {
            @Override
            public void run() {
                TLManager.getInstance().getRunningExperimentsAndVariations(listener);
            }
        });
    }

    /**
     * Get all running feature flags.
     *
     * @param listener which returns a HashMap of all currently running feature flags.
     *                 May return async, as it waits for properties to be loaded from Taplytics servers.
     */
    public static void getRunningFeatureFlags(final TaplyticsRunningFeatureFlagsListener listener) {
        TLManager.getInstance().executeRunnable(new Runnable() {
            @Override
            public void run() {
                TLFeatureFlagManager.getInstance().getRunningFeatureFlags(listener);
            }
        });
    }

    /**
     * Adds a listener for push notifications that will be called when the push is received.
     *
     * @param listener which returns a JSONObject of the notification custom data payload.
     *                 Callback called when notification is received.
     */
    @Deprecated
    public static void addPushNotificationListener(final TaplyticsPushNotificationListener listener) {
        TLManager.getInstance().executeRunnable(new Runnable() {
            @Override
            public void run() {
                if (TLManager.getInstance().getTlPushManager() != null) {
                    TLManager.getInstance().getTlPushManager().addPushNotificationListener(listener);
                } else {
                    Log.w("Taplytics", "Taplytics not yet instantiated. Call Taplytics.startTaplytics before any other Taplytics call.");
                }
            }
        });
    }

    /**
     * Removes a listener for push notifications that will no longer be called when the push is received
     *
     * @param listener the listener that will be removed.
     */
    @Deprecated
    public static void removePushNotificationListener(final TaplyticsPushNotificationListener listener) {
        TLManager.getInstance().executeRunnable(new Runnable() {
            @Override
            public void run() {
                if (TLManager.getInstance().getTlPushManager() != null) {
                    TLManager.getInstance().getTlPushManager().removePushNotificationListener(listener);
                } else {
                    Log.w("Taplytics", "Taplytics not yet instantiated. Call Taplytics.startTaplytics before any other Taplytics call.");
                }
            }
        });
    }

    /**
     * Sets the listener for push notifications that will be called when the push is received, intended to set the Intent the push notification launches when opened.
     *
     * @param listener A {@link TaplyticsPushNotificationIntentListener}.
     */
    @Deprecated
    public static void setPushNotificationIntentListener(final TaplyticsPushNotificationIntentListener listener) {
        TLManager.getInstance().executeRunnable(new Runnable() {
            @Override
            public void run() {
                if (TLManager.getInstance().getTlPushManager() != null) {
                    TLManager.getInstance().getTlPushManager().setPushIntentListener(listener);
                } else {
                    Log.w("Taplytics", "Taplytics not yet instantiated. Call Taplytics.startTaplytics before any other Taplytics call.");
                }
            }
        });
    }


    /**
     * Set user attributes for this user, this will allow Taplytics to segment your users based on: gender, age, and any custom data  that you
     * set. See the Class definition above for an example. Listens for the setUserAttributes call to finish completely. Returns when the call to
     * set user attributes is finished.
     * @param attributes JSONObject of User Attributes, available fields:
     *                   <ul>
     *                   <li>"email", String, user's unique email address</li>
     *                   <li>"user_id", String, user's unique user_id</li>
     *                   <li>"name", String, user's full name</li>
     *                   <li>"gender", String, user's gender: "male" or "female"</li>
     *                   <li>"age", Number, user's age</li>
     *                   <li>"avatarUrl", String, user's avatar URL to be used on the Taplytics website</li>
     *                   <li>All other params will be put into a 'Custom Data' field.</li>
     *                   </ul>sp
     * @param listener A {@link TaplyticsSetUserAttributesListener}.
     */
    public static void setUserAttributes(final JSONObject attributes, final TaplyticsSetUserAttributesListener listener) {
        TLManager.getInstance().executeRunnable(new Runnable() {
            boolean preload = false;

            @Override
            public void run() {
                if (TLManager.getInstance().getTlAnalytics() == null) {
                    TLManager.getInstance().setupTLAnalytics();
                    preload = true;
                }
                TLManager.getInstance().getTlAnalytics().getTlAppUser().updateAppUserAttributes(attributes, preload, listener);
            }
        });
    }

    /**
     * Set user attributes for this user, this will allow Taplytics to segment your users based on: gender, age, and any custom data  that you
     * set. See the Class definition above for an example.
     *
     * @param attributes JSONObject of User Attributes, available fields:
     *                   <ul>
     *                   <li>"email", String, user's unique email address</li>
     *                   <li>"user_id", String, user's unique user_id</li>
     *                   <li>"name", String, user's full name</li>
     *                   <li>"gender", String, user's gender: "male" or "female"</li>
     *                   <li>"age", Number, user's age</li>
     *                   <li>"avatarUrl", String, user's avatar URL to be used on the Taplytics website</li>
     *                   <li>All other params will be put into a 'Custom Data' field.</li>
     *                   </ul>sp
     */
    public static void setUserAttributes(final JSONObject attributes) {
        TLManager.getInstance().executeRunnable(new Runnable() {
            boolean preload = false;

            @Override
            public void run() {
                if (TLManager.getInstance().getTlAnalytics() == null) {
                    TLManager.getInstance().setupTLAnalytics();
                    preload = true;
                }
                TLManager.getInstance().getTlAnalytics().getTlAppUser().updateAppUserAttributes(attributes, preload, null);
            }
        });
    }

    /**
     * Retrieves the user attributes assigned to the current user at the current time.
     */
    public static void getSessionInfo(final SessionInfoRetrievedListener listener) {
        TLManager.getInstance().executeRunnable(new Runnable() {
            @Override
            public void run() {
                if (TLManager.getInstance().getTlAnalytics() != null) {
                    TLManager.getInstance().getTlAnalytics().getTlAppUser().getSessionInfo(listener);
                } else {
                    Log.w("Taplytics", "Taplytics not yet instantiated. Call Taplytics.startTaplytics before any other Taplytics call.");
                }
            }
        });
    }


    /**
     * Log event to Taplytics that a event has been hit.
     *
     * @param eventName experiment event name
     */
    public static void logEvent(String eventName) {
        logEvent(eventName, null, null);
    }

    /**
     * Log event to Taplytics that an event has been achieved with a Number value, and optional metaData.
     *
     * @param eventName experiment event name
     * @param value     event Number value
     * @param metaData  optional metaData JSONObject to segment your results by
     */
    public static void logEvent(final String eventName, final Number value, final JSONObject metaData) {
        TLManager.getInstance().executeRunnable(new Runnable() {
            @Override
            public void run() {
                if (TLManager.getInstance().getTlAnalytics() != null) {
                    TLManager.getInstance().getTlAnalytics().trackEvent(eventName, value, metaData);
                } else {
                    Log.w("Taplytics", "Taplytics not yet instantiated. Call Taplytics.startTaplytics before any other Taplytics call.");
                }
            }
        });
    }

    /**
     * Log event to Taplytics that an event has been achieved with a Number value.
     *
     * @param eventName experiment event name
     * @param value     event Number value
     */
    public static void logEvent(String eventName, Number value) {
        logEvent(eventName, value, null);
    }

    /**
     * Report revenue to Taplytics
     *
     * @param eventName revenue event name
     * @param revenue   revenue Number value
     */
    public static void logRevenue(String eventName, Number revenue) {
        logRevenue(eventName, revenue, null);
    }

    /**
     * Report revenue to Taplytics with optional metaData
     *
     * @param eventName revenue event name
     * @param revenue   revenue Number value
     * @param metaData  optional metaData JSONObject to segment your revenue results by.
     *                  <p>
     *                  For example you can pass a map with purchase type to Taplytics:
     *                  </p>
     *                  <p/>
     *                  <code>
     *                  JSONObject metData = new JSONObject();
     *                  metaData.put("accountType", "proUser");
     *                  Taplytics.logRevenue("inAppPurcahse", 1.0, metaData);
     *                  </code>
     */
    public static void logRevenue(final String eventName, final Number revenue, final JSONObject metaData) {
        TLManager.getInstance().executeRunnable(new Runnable() {
            @Override
            public void run() {
                if (TLManager.getInstance().getTlAnalytics() != null) {
                    TLManager.getInstance().getTlAnalytics().trackRevenue(eventName, revenue, metaData);
                } else {
                    Log.w("Taplytics", "Taplytics not yet instantiated. Call Taplytics.startTaplytics before any other Taplytics call.");
                }
            }
        });
    }

    /**
     * ADVANCED PAIRING:
     * <p/>
     * <p/>
     * Link a device (even in release mode) to Taplytics.
     * <p/>
     * NOTE: This is used only for deeplink pairing, and is unnecessary if your main activity does NOT have a singleTask flag.
     * <p/>
     * <p/>
     * Retrieve deeplink through Taplytics deeplink intercepted via either email or SMS device pairing.
     * It contains your Taplytics URL scheme and device token.
     *
     * @param link The entire deeplink including the scheme.
     */
    public static void deviceLink(final String link) {
        TLManager.getInstance().executeRunnable(new Runnable() {
            @Override
            public void run() {
                if (TLManager.socketsEnabled())
                    TLSocketManager.getInstance().pairDeviceFromLink(link);
            }
        });
    }

    /**
     * Resets the user entirely. This device will no longer be associated with the user, and a new user will be created for the device to associate with.
     *
     * @param listener a {@link com.taplytics.sdk.TaplyticsResetUserListener} which provides a callback when the user has been successfully reset.
     */
    public static void resetAppUser(final TaplyticsResetUserListener listener) {
        TLManager.getInstance().executeRunnable(new Runnable() {
            @Override
            public void run() {
                if (TLManager.getInstance().getTlAnalytics() != null) {
                    TLManager.getInstance().getTlAnalytics().getTlAppUser().resetAppUser(listener);
                } else {
                    Log.w("Taplytics", "Taplytics not yet instantiated. Call Taplytics.startTaplytics before any other Taplytics call.");
                }
            }
        });
    }


    /**
     * Taplytics creates a splash screen with the provided image. The image will fade automatically after the given time, or when Taplytics has successfully loaded visual changes on the provided activity.
     *
     * @param activity The activity (typically main activity) that will be covered in a splash image.
     * @param image    A Drawable image that will be the splash screen.
     * @param maxTime  Regardless of the results of Taplytics, the image will fade after this time.
     */
    public static void delayLoad(final Activity activity, final Drawable image, final int maxTime) {
        TLManager.getInstance().executeRunnable(new Runnable() {
            @Override
            public void run() {
                TLDelayLoadImageManager.getInstance().delayLoadImage(activity, image, maxTime);
            }
        });
    }

    /**
     * Taplytics creates a splash screen with the provided image. The image will fade automatically after the given time, or when Taplytics has successfully loaded visual changes on the provided activity.
     *
     * @param activity The activity (typically main activity) that will be covered in a splash image.
     * @param image    A Drawable image that will be the splash screen.
     * @param maxTime  Regardless of the results of Taplytics, the image will fade after this time.
     * @param minTime  The minimum time it will take for the image to fade
     */
    public static void delayLoad(final Activity activity, final Drawable image, final int maxTime, final int minTime) {
        TLManager.getInstance().executeRunnable(new Runnable() {
            @Override
            public void run() {
                TLDelayLoadImageManager.getInstance().delayLoadImage(activity, image, maxTime, minTime);
            }
        });
    }

    /**
     * Taplytics provides callbacks for starting and finishing a delayed load of the current activity.
     *
     * @param listener a {@link com.taplytics.sdk.TaplyticsDelayLoadListener} a listener which provide callbacks when a splash screen or delay page should be shown, and when it should be hidden.
     * @param maxTime  Regardless of the results of Taplytics, the image will fade after this time.
     */
    public static void delayLoad(final int maxTime, final TaplyticsDelayLoadListener listener) {
        TLManager.getInstance().executeRunnable(new Runnable() {
            @Override
            public void run() {
                TLDelayLoadManager.getInstance().delayLoad(listener, maxTime);
            }
        });
    }

    /**
     * Taplytics provides callbacks for starting and finishing a delayed load of the current activity.
     *
     * @param listener a {@link com.taplytics.sdk.TaplyticsDelayLoadListener} a listener which provide callbacks when a splash screen or delay page should be shown, and when it should be hidden.
     * @param maxTime  Regardless of the results of Taplytics, the image will fade after this time.
     * @param minTime  The minimum time it will take for the image to fade
     */
    public static void delayLoad(final int maxTime, final int minTime, final TaplyticsDelayLoadListener listener) {
        TLManager.getInstance().executeRunnable(new Runnable() {
            @Override
            public void run() {
                TLDelayLoadManager.getInstance().delayLoad(listener, maxTime, minTime);
            }
        });
    }

    /**
     * Taplytics has a touchscreen overlay applied on top of every screen. This passes all touches through
     * and does not interfere with the UI, it is simply used to track clicks. However, the youtube API
     * and some specific openGL surfaces don't allow being drawn over, so this removes those views.
     * <p/>
     * This is a global toggle. Use {@link Taplytics#overlayOn()} to re-apply.
     */
    public static void overlayOff() {
        TLViewManager.getInstance().setOverlayOff();
    }

    /**
     * See {@link #overlayOff()}
     */
    public static void overlayOn() {
        TLViewManager.getInstance().setOverlayOn();
    }


    /**
     * Taplytics provides the option of quickly switching your Google Analytics to ALSO send to Taplytics.
     * <p/>
     * In this case, the event will still be sent to Google Analytics as well.
     *
     * @param tracker      the Google Analytics Tracker used to send events.
     * @param eventBuilder the Google Analytics HitBuilder created to send event data.
     */
    public static void logGAEvent(final Object tracker, final Object eventBuilder) {
        TLManager.getInstance().executeRunnable(new Runnable() {
            @Override
            public void run() {
                TLExternalAnalyticsManager.getInstance().logGAEvent(tracker, eventBuilder);
            }
        });
    }

    /**
     * Set a listener that returns the user's GCM token once it's received.
     *
     * @param listener A listener which returns the GCM push token if it exists.
     */
    public static void setTaplyticsPushTokenListener(final TaplyticsPushTokenListener listener) {
        TLManager.getInstance().executeRunnable(new Runnable() {
            @Override
            public void run() {
                TLManager.getInstance().setPushTokenListener(listener);
            }
        });
    }

    /**
     * A listener that will be triggered if a user opens a push notification that was sent via Taplytics.
     *
     * @param pushOpenedListener
     */
    @Deprecated
    public static void setPushNotificationOpenedListener(final TaplyticsPushOpenedListener pushOpenedListener) {
        TLManager.getInstance().executeRunnable(new Runnable() {
            @Override
            public void run() {
                TLManager.getInstance().getTlPushManager().setPushOpenedListener(pushOpenedListener);
            }
        });
    }

    /**
     * Removes the listener for push notifications. See {@link #setPushNotificationOpenedListener(TaplyticsPushOpenedListener)}
     */
    @Deprecated
    public static void removePushNotificationOpenedListener() {
        TLManager.getInstance().executeRunnable(new Runnable() {
            @Override
            public void run() {
                TLManager.getInstance().getTlPushManager().removePushOpenedListener();
            }
        });
    }

    /**
     * Show the experiment / variation menu. Useful for QA testing when shaking the device is not possible.
     **/
    public static void showMenu() {
        TLManager.getInstance().executeRunnable(new Runnable() {
            @Override
            public void run() {
                TLManager.getInstance().showMenu();
            }
        });
    }


    /**
     * A listener for debugging. Triggered whenever Taplytics retrieves new information from the server. See: {@link TaplyticsExperimentsUpdatedListener}
     **/
    public static void setTaplyticsExperimentsUpdatedListener(final TaplyticsExperimentsUpdatedListener experimentsUpdatedListener) {
        TLManager.getInstance().executeRunnable(new Runnable() {
            @Override
            public void run() {
                TLManager.getInstance().setExperimentUpdatedListener(experimentsUpdatedListener);
            }
        });
    }

    /**
     * A listener for sessions. Whenever a new session is defined, this listener will be triggered.
     *
     * @param newSessionListener
     */
    public static void setTaplyticsNewSessionListener(final TaplyticsNewSessionListener newSessionListener) {
        TLManager.getInstance().executeRunnable(new Runnable() {
            @Override
            public void run() {
                TLManager.getInstance().setNewSessionListener(newSessionListener);
            }
        });
    }

    /**
     * Change whether or not a user should be receiving push notifications.
     *
     * @param pushEnabled
     * @param listener
     */
    public static void setPushSubscriptionEnabled(final boolean pushEnabled, final TaplyticsPushSubscriptionChangedListener listener) {
        TLManager.getInstance().executeRunnable(new Runnable() {
            @Override
            public void run() {
                TLManager.getInstance().getTlAnalytics().getTlAppUser().changePushSubscription(pushEnabled, listener);
            }
        });
    }

    /**
     * In the event that you use Taplytics to send notifications, but build your own, you may need
     * to implement this method to track push opens. Place this method wherever you track push opens,
     * and make sure you include the tl_id that was included in the taplytics intent bundle.
     *
     * Set custom keys to null if you do not have any.
     *
     * @param tl_id       Taplytics push's tl_id contained in the bundle
     * @param custom_keys
     * Custom keys, if there are any. Null if not.
     */
    public static void trackPushOpen(final String tl_id, final JSONObject custom_keys) {
        TLManager.getInstance().executeRunnable(new Runnable() {
            @Override
            public void run() {
                if (TLManager.getInstance().getTlAnalytics() != null) {
                    try {
                        TLManager.getInstance().getTlAnalytics().trackPushEvent(tl_id,custom_keys,TLAnalyticsManager.PushEvent.OPENED);
                    } catch (Throwable ignored) {
                    }
                }
            }
        });
    }

    /**
     * In the event that you use Taplytics to send notifications, but build your own, you may need
     * to implement this method to track push dismissed events. Place this method wherever you track push dismissed events,
     * and make sure you include the tl_id that was included in the taplytics intent bundle.
     *
     * Set custom keys to null if you do not have any.
     *
     * @param tl_id       Taplytics push's tl_id contained in the bundle
     * @param custom_keys
     * Custom keys, if there are any. Null if not.
     */
    public static void trackPushDismissed(final String tl_id, final JSONObject custom_keys) {
        TLManager.getInstance().executeRunnable(new Runnable() {

            @Override
            public void run() {
                if(TLManager.getInstance().getTlAnalytics() != null){
                    try {
                        TLManager.getInstance().getTlAnalytics().trackPushEvent(tl_id,custom_keys,TLAnalyticsManager.PushEvent.DISMISSED);
                    } catch (Throwable ignored) {
                    }
                }
            }
        });
    }

    /**
     * In the event that you use Taplytics to send notifications, but build your own, you may need
     * to implement this method to track push received events. Place this method wherever you track push received events,
     * and make sure you include the tl_id that was included in the taplytics intent bundle.
     *
     * Set custom keys to null if you do not have any.
     *
     * @param tl_id       Taplytics push's tl_id contained in the bundle
     * @param custom_keys
     * Custom keys, if there are any. Null if not.
     */
    public static void trackPushReceived(final String tl_id, final JSONObject custom_keys) {
        TLManager.getInstance().executeRunnable(new Runnable() {

            @Override
            public void run() {
                if(TLManager.getInstance().getTlAnalytics() != null){
                    try {
                        TLManager.getInstance().getTlAnalytics().trackPushEvent(tl_id,custom_keys,TLAnalyticsManager.PushEvent.RECEIVED);
                    } catch (Throwable ignored) {
                    }
                }
            }
        });
    }

    public static final String TaplyticsOptionSourceMixpanel = "Mixpanel";
    public static final String TaplyticsOptionSourceGoogleAnalytics = "GA";
    public static final String TaplyticsOptionSourceAdobe = "Adobe";
    public static final String TaplyticsOptionSourceFlurry = "Flurry";
    public static final String TaplyticsOptionSourceLocalytics = "Localytics";
    public static final String TaplyticsOptionSourceAmplitude = "Amplitude";
    public static final String TaplyticsOptionSourceSegment = "Segment";

    /**
     * Needs to be called after Flurry has started to automatically gather Flurry information.
     */
    public static void startFlurrySession() {
        TLManager.getInstance().executeRunnable(new Runnable() {
            @Override
            public void run() {
                TLExternalAnalyticsManager.getInstance().setupFlurryTracking();
            }
        });
    }

    /**
     * End the current user session and create a new session.
     * Calls taplytics to retrieve new experiment data if any.
     *
     * @param newSessionListener Callback which returns when the new session has begun
     */
    public static void startNewSession(final TaplyticsNewSessionListener newSessionListener) {
        TLManager.getInstance().executeRunnable(new Runnable() {
            @Override
            public void run() {
                TLManager.getInstance().startNewSession(newSessionListener);
            }
        });
    }

    /**
     * Calling this method will disable all user tracking.
     */
    public static void optOutUserTracking(final Context appContext) {
        TLManager.getInstance().executeRunnable(new Runnable() {
            @Override
            public void run() {
                TLKillSwitchManager.getInstance().optOutUserTracking(appContext);
            }
        });
    }

    /**
     * Calling this method will enable all user tracking.
     */
    public static void optInUserTracking(final Context appContext) {
        TLManager.getInstance().executeRunnable(new Runnable() {
            @Override
            public void run() {
                TLKillSwitchManager.getInstance().optInUserTracking(appContext);
            }
        });
    }

    /**
     * Returns if the user has opted out of user tracking.
     *
     * @param listener Callback to return if user has opted out of user tracking.
     */
    public static void hasUserOptedOutTracking(final Context appContext, final TaplyticsHasUserOptedOutListener listener) {
        TLManager.getInstance().executeRunnable(new Runnable() {
            @Override
            public void run() {
                listener.hasUserOptedOutTracking(TLKillSwitchManager.getInstance().hasUserOptedOutTracking(appContext));
            }
        });
    }
}
