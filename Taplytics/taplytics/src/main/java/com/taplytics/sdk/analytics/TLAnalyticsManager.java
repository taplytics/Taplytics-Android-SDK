/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.analytics;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Pair;

import com.taplytics.sdk.Taplytics;
import com.taplytics.sdk.TaplyticsPushOpenedListener;
import com.taplytics.sdk.analytics.external.TLExternalAnalyticsManager;
import com.taplytics.sdk.datatypes.FragHashMap;
import com.taplytics.sdk.datatypes.TLProperties;
import com.taplytics.sdk.listeners.TLFlushListener;
import com.taplytics.sdk.managers.TLManager;
import com.taplytics.sdk.network.TLNetworking;
import com.taplytics.sdk.utils.Functionality;
import com.taplytics.sdk.utils.TLDatabaseHelper;
import com.taplytics.sdk.utils.TLLog;
import com.taplytics.sdk.utils.TLUtils;
import com.taplytics.sdk.utils.promises.PromiseListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by @jonathannorris
 * <p/>
 * This is the manager for all things analytics.
 */
public class TLAnalyticsManager {

    private static final long REPORTING_INT = 60;

    public void setLiveReportingTime(long liveReportingInt) {
        LIVE_REPORTING_INT = liveReportingInt;
    }

    private long LIVE_REPORTING_INT = 5;

    public static final int BACKOFF_INCREMENT = 5;
    public static final int RANDOM_DELAY_RANGE = 60;
    public static final int MAX_BACKOFF_TIME = 300;
    public static final double EVENT_DELAY_FLAG = -1;
    public static final int MAX_EVENT_METADATA_BYTES = 50000;
    public static final int MAX_CLIENT_POST_FAILURE_COUNT = 5;

    /**
     * These are the actual event types that we track.
     */
    public static final String TLAnalyticsEventTouchUp = "touchUp";
    public static final String TLAnalyticsEventTimeOnActivity = "viewTimeOnPage";
    public static final String TLAnalyticsEventActivityAppeared = "viewAppeared";
    public static final String TLAnalyticsEventActivityDisappeared = "viewDisappeared";
    public static final String TLAnalyticsEventAppActive = "appActive";
    public static final String TLAnalyticsEventAppBackground = "appBackground";
    public static final String TLAnalyticsEventAppForeground = "appForeground";
    public static final String TLAnalyticsEventAppTerminate = "appTerminate";
    public static final String TLAnalyticsEventRevenue = "revenue";
    public static final String TLAnalyticsEventGoalAchieved = "goalAchieved";
    public static final String TLAnalyticsEventHasAppLinking = "tlHasAppLinking";
    public static final String TLAnalyticsEventPushOpened = "pushOpened";
    public static final String TLAnalyticsEventPushDismissed = "pushDismissed";
    public static final String TLAnalyticsEventPushReceived = "pushReceived";
    public static final String TLAnalyticsEventError = "tlError";
    public static final String TLAnalyticsRequestError = "tlRequestFailed";
    public static final String TLAnalyticsGeofenceEnter = "tlGeofenceEnter";
    public static final String TLAnalyticsGeofenceExit = "tlGeofenceExit";
    public static final String TLAnalyticsOptInTracking = "optInTracking";
    public static final String TLAnalyticsOptOutTracking = "optOutTracking";

    private boolean flushing = false;

    private int clientPostFailureCount = 0;

    // public static final String TLAnalyticsEventBackgroundFetch = "appBackgroundFetch";

    /**
     * These are the known sources *
     */
    public static final String SOURCE_MIXPANEL = "Mixpanel";
    public static final String SOURCE_FLURRY = "Flurry";
    public static final String SOURCE_GA = "GoogleAnalytics";

    private boolean sourceTrackingStarted = false;

    public ArrayList<String> disabledSources;

    /**
     * This is a simple map of an activity name, and the time that the user started on the activity. Used to track activity time.
     */
    private static Map<String, Long> activityMap = new HashMap<>();

    /**
     * This is a map of all fragments on the screen currently. The integer is the id, and then the Pair is a a pair of the Tag, and the time
     * started on the fragment.
     */
    private static FragHashMap<Object, Pair<String, Long>> fragmentMap = new FragHashMap<>();

    private static Map<String, Pair<Boolean, Integer>> errorEventMap = new HashMap<>();

    /**
     * The application user, see: {@link com.taplytics.sdk.analytics.TLAppUser} *
     */
    private TLAppUser tlAppUser = null;

    /**
     * Handler used for flushing the event queue in the background. *
     */
    private Handler flushEventsQueueHandler = new Handler(Looper.getMainLooper());

    /**
     * This is the runnable to asynchronously flush the events queue. This is a runnable instead of a timertask because timertasks actually
     * don't properly get garbage collected and fill up memory. Google it.
     */

    private class FlushEventsQueueRunnable implements Runnable {

        TLFlushListener listener;

        FlushEventsQueueRunnable(TLFlushListener flushListener) {
            this.listener = flushListener;
        }

        @Override
        public void run() {
            flushEventsQueue(listener);
        }
    }

    /**
     * The date format used for the analytics *
     */
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ", Locale.US);

    /**
     * Queue to hold events while waiting for properties to load.
     */
    private ArrayList<JSONObject> eventsQueue = new ArrayList<>();

    ArrayList<JSONObject> getEventsQueue() {
        return eventsQueue;
    }

    /**
     * Listener to wait for tlProperties to load.
     */
    private PromiseListener tlPropertiesPromiseListener;

    PromiseListener getTlPropertiesPromiseListener() {
        return tlPropertiesPromiseListener;
    }

    /**
     * The way that the analytics manager interacts with the events sqlite db
     */
    private TLDatabaseHelper dbHelper = null;
    private ExecutorService dbExecutor = null;

    void setDBExecutorMock(ExecutorService executor) {
        dbExecutor = executor;
    }

    private Date eventUpdateTime = null;

    public enum PushEvent {
        OPENED("opened"),
        DISMISSED("dismissed"),
        RECEIVED("received");

        private String name;

        PushEvent(String name) {
            this.name = name;
        }

        public String getName() {
            return name.toLowerCase();
        }
    }

    /**
     * Instantiate the analytics manager, and {@link #tlAppUser} *
     */
    public TLAnalyticsManager() {
        this.tlAppUser = new TLAppUser();
        this.dbHelper = new TLDatabaseHelper(TLManager.getInstance().getAppContext());
        this.dbExecutor = Executors.newSingleThreadExecutor();
        this.dbExecutor.submit(new Runnable() {
            @Override
            public void run() {
                //open the database
                dbHelper.init();
            }
        });

        clearEventsIfNecessary();
    }

    public void recreateDBHelper() {
        this.dbHelper = new TLDatabaseHelper(TLManager.getInstance().getAppContext());
        dbHelper.deleteEvents(1000);
    }

    /**
     * Returns {@link #tlAppUser} *
     */
    public TLAppUser getTlAppUser() {
        return tlAppUser;
    }

    public void resetClientPostFailureCount() {
        clientPostFailureCount = 0;
    }

    /**
     * Here we actually track every button that we press. *
     */
    public void trackViewEvent(String eventName, JSONObject viewObj) {
        if (!TLManager.getInstance().getTrackingEnabled() ||
                TLUtils.isDisabled(Functionality.BUTTONS) ||
                TLUtils.isDisabled(Functionality.EVENTS)) {
            return;
        }

        if (eventName == null || viewObj == null) {
            return;
        }

        try {
            JSONObject dic = getTLEventData(eventName, null, null);

            if (viewObj.has("key")) {
                dic.put("eKey", viewObj.optString("key"));
            }

            Activity currentActivity = TLManager.getInstance().getCurrentActivity();
            if (viewObj.has("vKey") && currentActivity != null) {
                dic.put("vKey", currentActivity.getClass().getSimpleName());
            }

            pushTLEvent(dic);
        } catch (JSONException e) {
            TLLog.error("Track View Event", e);
        }
    }

    /**
     * @return {@link #activityMap} *
     */
    public Map<String, Long> getActivityMap() {
        return activityMap;
    }

    /**
     * @return {@link #fragmentMap} *
     */
    public static Map<Object, Pair<String, Long>> getFragmentMap() {
        return fragmentMap;
    }

    /**
     * Start Analytics source tracking. *
     */
    public void startAnalyticsSourceTracking() {
        if (!TLManager.getInstance().getTrackingEnabled() || TLUtils.isDisabled(Functionality.EXTERNAL))
            return;

        TLProperties props = TLManager.getInstance().getTlProperties();
        JSONObject settings = (props != null) ? props.getAnalyticsSettings() : null;

        if (settings != null && !sourceTrackingStarted) {
            // Start Mixpanel Analytics source tracking
            if (!hasDisabledSource(Taplytics.TaplyticsOptionSourceMixpanel) && settings.optBoolean("mpSource", false) && !TLUtils.isDisabled(Functionality.MIXPANEL)) {
                TLExternalAnalyticsManager.getInstance().enableMixpanel();
            }

            // Start Google Analytics source tracking
            if (!hasDisabledSource(Taplytics.TaplyticsOptionSourceGoogleAnalytics) && settings.optBoolean("gaSource", false) && !TLUtils.isDisabled(Functionality.GOOGLE)) {
                TLExternalAnalyticsManager.getInstance().enableGoogleAnalytics();
                TLLog.debug("Enabling GA");
            } else {
                TLLog.debug("GA not enabled.");
            }

            //start localytics tracking
            if (!hasDisabledSource(Taplytics.TaplyticsOptionSourceLocalytics) && settings.optBoolean("llSource", false) && !TLUtils.isDisabled(Functionality.LOCALYTICS)) {
                TLExternalAnalyticsManager.getInstance().enableLocalytics();
            }

            //start flurry analytics tracking
            if (!hasDisabledSource(Taplytics.TaplyticsOptionSourceFlurry) && settings.optBoolean("flSource", false) && !TLUtils.isDisabled(Functionality.FLURRY)) {
                TLExternalAnalyticsManager.getInstance().enableFlurry();
            }

            if (!hasDisabledSource(Taplytics.TaplyticsOptionSourceAdobe) && settings.optBoolean("adbSource", false) && !TLUtils.isDisabled(Functionality.ADOBE)) {
                TLExternalAnalyticsManager.getInstance().enableAdobe();
            }

            //start amplitude analytics tracking
            if (!hasDisabledSource(Taplytics.TaplyticsOptionSourceAmplitude) && settings.optBoolean("ampSource", false) && !TLUtils.isDisabled(Functionality.AMPLITUDE)) {
                TLExternalAnalyticsManager.getInstance().enableAmplitude();

            }

            //start segment analytics tracking
            if (!hasDisabledSource(Taplytics.TaplyticsOptionSourceSegment) && !TLUtils.isDisabled(Functionality.SEGMENT)) {
                TLExternalAnalyticsManager.getInstance().enableSegment();
            }

            TLExternalAnalyticsManager.getInstance().setupExternalAnalyticsManager();
            sourceTrackingStarted = true;
        }
    }

    private boolean hasDisabledSource(String source) {
        if (disabledSources != null) {
            for (String disable : disabledSources) {
                if (disable.equals(source))
                    return true;
            }
        }
        return false;
    }

    private void clearEventsIfNecessary() {
        if (TLManager.getInstance().getStartingOptions() != null && TLManager.getInstance().getStartingOptions().containsKey("clearEvents")) {
            dbHelper.deleteEvents(dbHelper.getEventCount());
        }
    }

    /**
     * Track an event. *
     */
    public void trackEvent(String eventName, Object value, JSONObject metaData) {
        if (!TLManager.getInstance().getTrackingEnabled() || TLUtils.isDisabled(Functionality.EVENTS)) {
            return;
        }

        if (eventName != null) {
            TLLog.debug("Log event: " + eventName + ", value: " + value, true);
            try {
                JSONObject dic = getTLEventData(TLAnalyticsEventGoalAchieved, value, metaData);
                dic.put("gn", eventName);
                pushTLEvent(dic);
            } catch (JSONException e) {
                TLLog.error("Track Event", e);
            }
        }
    }

    public void trackTLActivityEvent(String event, Object value, String classname) {
        trackTLActivityEvent(event, value, classname, null);
    }

    /**
     * Track a basic Taplytics Activity event! *
     */
    public void trackTLActivityEvent(String event, Object value, String className, JSONObject metaData) {
        if (!TLManager.getInstance().getTrackingEnabled() || TLUtils.isDisabled(Functionality.EVENTS))
            return;

        if (event != null) {
            TLLog.debug("Log TL Event: " + event + " of: " + className + ", value: " + value);
            try {
                JSONObject dic = getTLEventData(event, value, metaData);
                if (value != null) {
                    dic.put("val", value);
                }

                dic.put("vKey", className);

                pushTLEvent(dic);
            } catch (Exception e) {
                TLLog.error("Tracking TLEvent", e);
            }
        }
    }

    /**
     * Track an error in the network. *
     */
    public void trackRequestError(String eventName, String path, String text, Throwable e) {
        if (!TLManager.getInstance().getTrackingEnabled() || TLUtils.isDisabled(Functionality.EVENTS) || TLUtils.isDisabled(Functionality.REQUESTERRORS))
            return;

        if (eventName != null) {
            JSONObject metaData = new JSONObject();

            try {
                TLLog.debug("Log request error: " + eventName + ", value: " + path);
                if (e != null && e.getMessage() != null && !e.getMessage().equals("")) {
                    metaData.put("message", e.getMessage());

                    try {
                        StringWriter sw = new StringWriter();
                        e.printStackTrace(new PrintWriter(sw));
                        String exceptionAsString = sw.toString();
                        metaData.put("stacktrace", exceptionAsString);
                    } catch (Exception exc) {
                        TLLog.superError("Broke tracking request error.", exc);
                    }
                }
                if (text != null) {
                    metaData.put("logText", text);
                }

                JSONObject dic = getTLEventData(TLAnalyticsRequestError, 0, metaData);
                dic.put("gn", path);
                pushTLEvent(dic);

            } catch (JSONException ex) {
                TLLog.error("Track Event", ex);
            }
        }
    }

    /**
     * Tracking revenue. Accepts a Number with the event *
     */
    public void trackRevenue(String eventName, Number revenue, JSONObject metaData) {
        if (!TLManager.getInstance().getTrackingEnabled() || TLUtils.isDisabled(Functionality.EVENTS))
            return;

        if (eventName != null) {
            TLLog.debug("Log Revenue: " + eventName + ", revenue: " + revenue);

            try {
                // Grab an event dictionary.
                JSONObject dic = getTLEventData(TLAnalyticsEventRevenue, revenue, metaData);
                dic.put("gn", eventName);
                pushTLEvent(dic);
            } catch (JSONException e) {
                TLLog.error("Track Revenue", e);
            }
        }
    }

    /**
     * Simply track an event by name *
     */
    public void trackTLEvent(String event) {
        trackTLEvent(event, null, null);
    }

    public void trackTLEvent(String event, Object value, JSONObject metaData) {
        trackTLEvent(event, value, metaData, null);
    }

    /**
     * Track a basic Taplytics event! Such as activities switching and whatnot. *
     */
    public void trackTLEvent(String event, Object value, JSONObject metaData, String goalName) {
        if (!TLManager.getInstance().getTrackingEnabled() || TLUtils.isDisabled(Functionality.EVENTS))
            return;

        if (event != null) {
            TLLog.debug("Log TL Event: " + event + ", value: " + value);

            try {
                if (event == TLAnalyticsEventAppActive) {
                    resetClientPostFailureCount();
                }
                JSONObject dic = getTLEventData(event, value, metaData);
                if (event.equals(TLAnalyticsEventActivityAppeared) || event.equals(TLAnalyticsEventActivityDisappeared)) {
                    dic.put("val", dateFormat.format(new Date()));
                    dic.put("vKey", value);
                }
                if (goalName != null) {
                    dic.put("gn", goalName);
                }

                pushTLEvent(dic);
            } catch (Exception e) {
                TLLog.error("Tracking TLEvent", e);
            }
        }
    }

    public void trackError(String log, Exception e, boolean sendToServer) {
        if (!TLManager.getInstance().getTrackingEnabled() || TLUtils.isDisabled(Functionality.EVENTS) || TLUtils.isDisabled(Functionality.ERRORS))
            return;
        //Create a base count.
        int count = 1;
        try {
            JSONObject metaData = new JSONObject();

            if (log != null && !log.equals("")) {

                // Check if we have seen this error before
                if (errorEventMap.containsKey(log)) {
                    //If we have, increment the amount.
                    count = errorEventMap.get(log).second;
                    count++;

                    //This boolean denotes whether or not this error is already waiting to be sent. If it is, just increment the number, and leave.
                    if (errorEventMap.get(log).first) {
                        errorEventMap.put(log, new Pair<>(true, count));
                        return;
                    }
                }

                metaData.put("taplyticsMessage", log);
            }

            JSONObject dic = null;
            if (e != null && e.getMessage() != null && !e.getMessage().equals("")) {
                metaData.put("message", e.getMessage());

                try {
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    String exceptionAsString = sw.toString();
                    metaData.put("stacktrace", exceptionAsString);
                } catch (Exception exc) {
                    TLLog.superError("Broke tracking error.", exc);
                }

                dic = getTLEventData(TLAnalyticsEventError, null, metaData);

            } else if (e == null || e.getMessage() == null || e.getMessage().equals("")) {
                try {
                    dic = getTLEventData(TLAnalyticsEventError, null, metaData);
                } catch (Exception excep) {
                    TLLog.superError("Broke tracking error.", excep);
                }

            }

            if (dic != null && sendToServer) {
                if (log != null) {
                    //Pairs have final values so we have to make a new pair here.
                    errorEventMap.put(log, new Pair<>(true, count));
                    pushTLEvent(dic);
                } else {
                    pushTLEvent(dic);
                }
            }

        } catch (Exception ex) {
            TLLog.superError("Broke tracking error.", e);
        }
    }

    /**
     * Track error events to remove duplicate errors from tracking.
     *
     * @param dic
     * @throws JSONException
     */
    private void trackErrorEvent(JSONObject dic) throws JSONException {
        if (dic == null) {
            return;
        }

        if (!dic.get("type").equals(TLAnalyticsEventError) || !dic.has("data")) {
            return;
        }

        //If this is an error we need to now add the number of times we've seen it.
        final JSONObject data = dic.optJSONObject("data");
        if (!data.has("taplyticsMessage")) {
            return;
        }

        final String messageKey = data.optString("taplyticsMessage");
        if (!errorEventMap.containsKey(messageKey)) {
            return;
        }

        //Grab the number and put it as the value.
        dic.put("val", errorEventMap.get(messageKey).second);
        //Put this back in the map but switch the boolean to false because we've now sent this section.
        errorEventMap.put(messageKey, new Pair<>(false, errorEventMap.get(messageKey).second));
    }

    /**
     * Create a taplytics JSONObject out of the given event data *
     */
    private JSONObject getTLEventData(String eventName, Object value, JSONObject metaData) throws JSONException {
        if (eventName == null) {
            return null;
        }

        final TLManager tlManager = TLManager.getInstance();
        final TLProperties properties = tlManager.getTlProperties();

        final Date tlTime;
        if (metaData != null && metaData.has("TLTime")) {
            tlTime = new Date(Long.parseLong(metaData.optString("TLTime")));
        } else {
            tlTime = new Date();
        }

        final JSONObject dic = new JSONObject();
        dic.put("type", eventName);
        dic.put("date", dateFormat.format(tlTime));

        if (metaData != null) {
            try {
                if (metaData.toString().getBytes("UTF-8").length < MAX_EVENT_METADATA_BYTES) {
                    dic.put("data", metaData);
                } else {
                    TLLog.warning("Event Metadata too large, will not be added.");
                }
            } catch (Throwable ignored) {

            }
        }

        if (value != null) {
            dic.put("val", value);
        }

        if (properties != null && properties.getSessionID() != null) {
            dic.put("sid", properties.getSessionID());
        }

        dic.put("prod", tlManager.isLiveUpdate() ? 0 : 1);
        return dic;
    }

    /**
     * Add the event to the database and queue an update *
     */
    private void pushTLEvent(final JSONObject dic) {
        if (dic == null) return;

        if (!isEventDisabled(dic)) {
            // If we don't have a session ID AND we've timed out, then we don't care about these events.
            if ((!dic.has("sid") && TLManager.getInstance().isExperimentLoadTimeout() && TLManager.getInstance().getTlProperties() != null && TLManager.getInstance().getTlProperties().getLastSessionId() == null)) {
                return;
            }
        }

        if (TLManager.getInstance().hasLoadedPropertiesFromServer()) {
            setSessionIDAndProdOnEvent(dic);
            writeEventToDB(dic);
        } else {
            queueEventUntilPropertiesLoaded(dic);
        }
    }

    /**
     * This method takes a JSONObject and adds session ID and prod values to it
     *
     * @param dic
     */
    void setSessionIDAndProdOnEvent(JSONObject dic) {
        if (dic == null) {
            return;
        }

        final TLProperties properties = TLManager.getInstance().getTlProperties();

        String sessionID = null;
        if (properties != null) {
            sessionID = properties.getSessionID() == null ? properties.getLastSessionId() : properties.getSessionID();
        }

        try {
            if (!dic.has("sid") && sessionID != null) {
                dic.put("sid", sessionID);
            }
            if (!dic.has("prod")) {
                dic.put("prod", TLManager.getInstance().isLiveUpdate() ? 0 : 1);
            }
        } catch (Exception e) {
            TLLog.error("Error putting sid or prod in dic", e);
        }
    }

    /**
     * Writes the JSONObject to the DB
     *
     * @param dic
     */
    void writeEventToDB(final JSONObject dic) {
        setSessionIDAndProdOnEvent(dic);

        //throw a runnable on to the executors queue
        dbExecutor.submit(new Runnable() {
            @Override
            public void run() {
                dbHelper.writeEvent(dic);
                queueTLEventsUpdate(null);
            }
        });
    }

    /**
     * Writes the array of JSONObjects to the DB, also sets sessionID and prod values.
     *
     * @param events
     */
    void writeAllQueuedEventsToDB(ArrayList<JSONObject> events) {
        // copy queued events and clear eventsQueue if dbExecutor is async
        final ArrayList<JSONObject> eventsToDB = new ArrayList<>(events);
        events.clear();
        TLLog.debug("Write queued events to events DB, length: " + eventsToDB.size());

        for (final JSONObject dic : eventsToDB) {
            setSessionIDAndProdOnEvent(dic);
        }

        //throw a runnable on to the executors queue
        dbExecutor.submit(new Runnable() {
            @Override
            public void run() {
                for (final JSONObject dic : eventsToDB) {
                    dbHelper.writeEvent(dic);
                }
                queueTLEventsUpdate(null);
            }
        });
    }

    /**
     * Queues the JSONObject if there isn't a tlPropertiesPromiseListener that exists.
     * This method waits for properties be loaded so we can set the session ID before saving to the DB.
     *
     * @param dic
     */
    void queueEventUntilPropertiesLoaded(final JSONObject dic) {
        eventsQueue.add(dic);
        if (tlPropertiesPromiseListener != null) {
            return;
        }

        tlPropertiesPromiseListener = new PromiseListener() {
            @Override
            public void succeeded() {
                super.succeeded();
                tlPropertiesPromiseListener = null;
                writeAllQueuedEventsToDB(eventsQueue);
            }

            @Override
            public void failedOrCancelled() {
                super.failedOrCancelled();
                TLLog.debug("queueEventUntilPropertiesLoaded getTlPropertiesPromise failed or cancelled");
                // if we fail to get properties to set the session_id, write events to DB anyway.
                this.succeeded();
            }
        };

        TLManager.getInstance().getTlPropertiesPromise().add(tlPropertiesPromiseListener);
    }

    /**
     * Take in the events JSONObject and check our list of disabled event types/goal names to see if this event should be sent to the server
     */
    private boolean isEventDisabled(JSONObject dic) {
        if (TLManager.getInstance().getTlProperties() == null) {
            return false;
        }

        //get the list of disabled events from tlProperties
        final JSONObject disabledEvents =
                TLManager.getInstance().getTlProperties().getDisabledEvents();
        final String type = dic.optString("type");

        //if we don't have a list of disabled events or an entry for this event type then this event isn't disabled
        if (disabledEvents == null || !disabledEvents.has(type)) {
            return false;
        }

        if (type.length() == 0) {
            return false;
        }

        //get the list of filters, if there is no list for this type its not disabled
        final JSONArray array = disabledEvents.optJSONArray(type);
        if (array == null) {
            return false;
        }

        //loop through all the filters for this type of event
        for (int i = 0; i < array.length(); i++) {
            final JSONObject filter = array.optJSONObject(i);
            //if there is filter for this event type we assume the event is disabled until one of the filter values doesn't match
            //the values in the event
            boolean disabled = true;

            final Iterator<String> keys = filter.keys();

            while (keys.hasNext()) {
                final String key = keys.next();
                final Object eventValue = dic.opt(key);
                final Object filterValue = filter.opt(key);
                if ((filterValue == null && eventValue != null) ||
                        (filterValue != null && !filterValue.equals(eventValue))) {
                    disabled = false;
                    break;
                }
            }

            if (disabled) {
                return true;
            }
        }

        return false;
    }

    /**
     * Load up any events we may have saved to disk that still need to be sent up *
     */
    public void readDiskEvents() {
        if (!TLManager.getInstance().getTrackingEnabled() ||
                TLUtils.isDisabled(Functionality.EVENTS)) {
            return;
        }

        tlAppUser.readUserAttributesFromDisk();
    }

    /**
     * Set the time for the next update push attempt *
     *
     * @param flushListener
     */
    void queueTLEventsUpdate(TLFlushListener flushListener) {
        if (flushListener != null) {
            flushListener.flushCompleted(true);
        }

        if (TLManager.getInstance().getHasAppBackgrounded() &&
                clientPostFailureCount >= MAX_CLIENT_POST_FAILURE_COUNT) {
            return;
        }

        if (eventUpdateTime == null && dbHelper.getEventCount() > 0) {
            eventUpdateTime = new Date();

            // set flushing false here as safety if TLFlushListener doesn't return
            flushing = false;

            /** Delay changes based on {@link TLManager#isLiveUpdate} **/
            double delaySec = getFlushDelayInSeconds();

            TLLog.debug("Requeueing events with delay: " + delaySec);
            // Set the update to the handler. If it happens to be called again, it will overwrite the old time..
            flushEventsQueueHandler.postDelayed(new FlushEventsQueueRunnable(flushListener), Math.round(delaySec) * 1000);
        }
    }

    public double getFlushDelayInSeconds() {
        TLProperties properties = TLManager.getInstance().getTlProperties();
        double delay = (properties != null && properties.getEventTime() != EVENT_DELAY_FLAG) ? properties.getEventTime() : REPORTING_INT;

        if (TLManager.getInstance().isLiveUpdate()) {
            delay = LIVE_REPORTING_INT;
        }

        if (clientPostFailureCount > 0) {
            double rand = Math.random() * RANDOM_DELAY_RANGE;
            double randomDelay = Math.pow(2, clientPostFailureCount) + rand;
            delay += Math.min(randomDelay, MAX_BACKOFF_TIME);
        }

        return delay;
    }

    /**
     * Simply track an event by name *
     */
    public void trackSourceEvent(String source, String event) {
        trackSourceEvent(source, event, null, null);
    }

    /**
     * Track an event from a different source, such as Mixpanel or Flurry *
     */
    public void trackSourceEvent(String source, String eventName, Object value, JSONObject metaData) {
        if (!TLManager.getInstance().getTrackingEnabled() ||
                TLUtils.isDisabled(Functionality.EVENTS) ||
                TLUtils.isDisabled(Functionality.EXTERNAL)) {
            return;
        }

        if (eventName == null) {
            return;
        }

        TLLog.debug("Log event: " + eventName + ", value: " + value + ". From source: " + source + ". Metadata: " + (metaData == null ? "none" : metaData.toString()));
        try {
            final JSONObject dic = getTLEventData(source, value, metaData);
            dic.put("gn", eventName);
            pushTLEvent(dic);
        } catch (Throwable t) {
            TLLog.error("Track source event", (t instanceof Exception) ? (Exception) t : null);
        }
    }

    /**
     * Flush the events queue to the Taplytics dashboard. *
     *
     * @param flushListener
     */
    public void flushEventsQueue(final TLFlushListener flushListener) {
        if (!TLManager.getInstance().getTrackingEnabled() || TLUtils.isDisabled(Functionality.EVENTS))
            return;

        dbExecutor.submit(new Runnable() {
            @Override
            public void run() {
                if (flushing) {
                    queueTLEventsUpdate(flushListener);
                    return;
                }

                flushing = true;
                eventUpdateTime = null;
                final HashMap<String, ArrayList<JSONObject>> bucketedEvents = getSplitEventsBySessionIDMap();
                final int length = bucketedEvents.size();
                final AtomicInteger count = new AtomicInteger();
                final AtomicBoolean hasFlushFailed = new AtomicBoolean(false);
                final TLFlushListener batchListener = new TLFlushListener() {
                    @Override
                    public void flushCompleted(boolean success) {
                        if (count.incrementAndGet() == length) {
                            flushing = false;
                            if (dbHelper.getEventCount() > 0) {
                                flushEventsQueueHandler.post(new FlushEventsQueueRunnable(flushListener));
                            } else if (flushListener != null) {
                                if (hasFlushFailed.get()) {
                                    flushListener.flushFailed();
                                } else {
                                    flushListener.flushCompleted(success);
                                }
                            }
                        }
                    }

                    @Override
                    public void flushFailed() {
                        hasFlushFailed.set(true);
                        this.flushCompleted(false);
                    }
                };

                if (bucketedEvents.size() > 0) {
                    for (Map.Entry<String, ArrayList<JSONObject>> entry : bucketedEvents.entrySet()) {
                        postEventsBatchForSessionID(entry.getValue(), entry.getKey(), batchListener);
                    }
                } else {
                    flushListener.flushCompleted(true);
                }

            }
        });
    }

    /**
     * Posts to the /clientEvents API the events with the session ID and a flush listener
     *
     * @param eventArray
     * @param sessionID
     * @param flushListener
     */
    void postEventsBatchForSessionID(final ArrayList<JSONObject> eventArray, final String sessionID, final TLFlushListener flushListener) {
        try {
            TLManager tlManager = TLManager.getInstance();
            JSONObject params = new JSONObject();
            if (tlManager.getApiKey() != null)
                params.put("t", tlManager.getApiKey());
            params.put("sid", sessionID);
            params.put("e", new JSONArray(eventArray));
            tlManager.getTlNetworking().postClientEvents(params, new TLNetworking.TLNetworkResponseListener() {
                @Override
                public void onResponse(JSONObject response) {
                    TLLog.debug("Flushed Event Queue! for session: " + sessionID);

                    resetClientPostFailureCount();

                    if (flushListener != null) {
                        flushListener.flushCompleted(true);
                    }
                }

                @Override
                public void onError(Throwable error) {
                    if (clientPostFailureCount <= MAX_CLIENT_POST_FAILURE_COUNT) {
                        TLLog.error("Flushing Event Queue, for session: " + sessionID, error);
                    }

                    // If events failed to save, write them all to the DB to try again.
                    writeAllQueuedEventsToDB(eventArray);

                    ++clientPostFailureCount;

                    if (flushListener != null) {
                        flushListener.flushFailed();
                    }
                }
            });
        } catch (Exception e) {
            TLLog.error("Flushing Events Queue", e);
            if (flushListener != null) {
                flushListener.flushFailed();
            }
        }
    }

    /**
     * Gets 100 events from the DB and buckets them according to session ID. Returns a hashmap with
     * session ID as the key and an array of events that were in that session.
     *
     * @return HashMap
     */
    HashMap<String, ArrayList<JSONObject>> getSplitEventsBySessionIDMap() {
        try {
            final ArrayList<JSONObject> tlEventsCopy = dbHelper.getEvents(100);
            HashMap<String, ArrayList<JSONObject>> bucketedEvents = new HashMap<>();
            dbHelper.deleteEvents(tlEventsCopy.size());

            for (JSONObject dic : tlEventsCopy) {
                setSessionIDAndProdOnEvent(dic);
                trackErrorEvent(dic);

                String sid = (String) dic.get("sid");
                if (bucketedEvents.containsKey(sid)) {
                    bucketedEvents.get(sid).add(dic);
                } else if (sid != null) {
                    ArrayList<JSONObject> sessionEvents = new ArrayList<>();
                    sessionEvents.add(dic);
                    bucketedEvents.put(sid, sessionEvents);
                } else {
                    // if somehow there isn't a sessionID here write the event back to the DB
                    writeEventToDB(dic);
                }
            }

            return bucketedEvents;
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * Tracks the amount of time spent on an activity. Called after activity is closed/paused/backgrounded
     *
     * @param className Name of the class just left
     */
    public static void trackActivityTime(String className) {
        try {
            if (activityMap.containsKey(className)) {
                Long time = (System.currentTimeMillis() - activityMap.get(className)) / 1000;
                activityMap.remove(className);
                TLLog.debug("Time on activity: "
                        + String.format("%d min, %d sec", TimeUnit.MILLISECONDS.toMinutes(time),
                        TimeUnit.MILLISECONDS.toSeconds(time) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time))));

                TLManager.getInstance().getTlAnalytics().trackTLActivityEvent(TLAnalyticsManager.TLAnalyticsEventTimeOnActivity, time, className);
            } else {
                TLLog.debug("Problem: Activity not in activity map, cant track time");
            }
        } catch (Exception e) {
            TLLog.error("Error tracking activity time", e);
        }
    }

    /**
     * Tracks the amount of time spent on an activity. Called after activity is closed/paused/backgrounded
     *
     * @param id Name of the class just left
     */
    public static void trackFragmentTime(Object id, String className, boolean isBackground) {
        try {
            if (fragmentMap.containsKey(id)) {
                Long time = (System.currentTimeMillis() - (Long) ((Pair) fragmentMap.get(id)).second) / 1000;
                if (!isBackground) {
                    fragmentMap.remove(id);
                }
                TLLog.debug("Time on fragment: "
                        + String.format("%d min, %d sec", TimeUnit.MILLISECONDS.toMinutes(time),
                        TimeUnit.MILLISECONDS.toSeconds(time) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time))));

                TLManager.getInstance().getTlAnalytics().trackTLActivityEvent(TLAnalyticsManager.TLAnalyticsEventTimeOnActivity, time, className);
            } else {
                TLLog.debug("Problem: Id not in fragment map, cant track time");
            }
        } catch (Exception e) {
            TLLog.error("Error tracking fragment time", e);
        }
    }

    /**
     * This method is for use by the @link{Taplytics#trackPushOpen} method.
     * In the event that our users are building their own notifications, our automatic
     * tracking will not track them. So this lets them have push open tracking regardless.
     *
     * @param id          Taplytics push's tl_id.
     * @param custom_keys Custom keys, if there are any.
     * @param pushEvent   is the type of push event to be tracked
     */
    public void trackPushEvent(String id, JSONObject custom_keys, PushEvent pushEvent) {
        try {
            Bundle b = new Bundle();
            b.putString("tl_id", id);
            if (custom_keys != null) {
                b.putString("custom_keys", custom_keys.toString());
            }
            trackPushNotificationInteraction(b, pushEvent);
        } catch (Throwable t) {
            TLLog.error("t p o", t);
        }
    }

    /**
     * Tracks when a push notification event has occurred
     *
     * @param bundle    is the bundle of TLManager
     * @param pushEvent is the type of push event to be tracked
     */
    public void trackPushNotificationInteraction(Bundle bundle, PushEvent pushEvent) {
        try {
            if (!TLManager.getInstance().getTrackingEnabled() || TLUtils.isDisabled(Functionality.EVENTS)
                    || TLUtils.isDisabled(Functionality.PUSH))
                return;

            TLLog.debug("Push notification " + pushEvent.getName());
            if (bundle.getString("tl_id") == null || !TLManager.getInstance().getTrackingEnabled()) {
                return;
            }

            JSONObject metaData = null;
            if (bundle.get("custom_keys") != null) {
                metaData = new JSONObject((String) bundle.get("custom_keys"));
            }
            if (bundle.get("tl_image") != null) {
                if (metaData == null) {
                    metaData = new JSONObject();
                }
                metaData.put("tl_image", bundle.get("tl_image"));
            }
            String eventName = "";
            switch (pushEvent) {
                case OPENED:
                    eventName = TLAnalyticsEventPushOpened;
                    break;
                case DISMISSED:
                    eventName = TLAnalyticsEventPushDismissed;
                    break;
                case RECEIVED:
                    eventName = TLAnalyticsEventPushReceived;
                    break;
            }
            JSONObject dic = getTLEventData(eventName, null, metaData);
            if (dic != null) {
                dic.put("gn", bundle.getString("tl_id"));
                pushTLEvent(dic);

                if (pushEvent == PushEvent.OPENED) {
                    TaplyticsPushOpenedListener pushOpenedListener = TLManager.getInstance().getTlPushManager().getPushOpenedListener();
                    if (pushOpenedListener != null) {
                        pushOpenedListener.pushOpened(bundle);
                    }
                }
            }
        } catch (Exception e) {
            //Being careful about stuff
        }
    }

    public void clearEventQueue() {
        dbExecutor.submit(new Runnable() {
            @Override
            public void run() {
                //Delete all the events (there is a max of 1000 events)
                dbHelper.deleteEvents(1000);
            }
        });
    }

    /**
     * Set DB Helper mocks for testing
     *
     * @param mockDBHelper
     */
    void setDBHelperMock(TLDatabaseHelper mockDBHelper) {
        dbHelper = mockDBHelper;
    }

}
