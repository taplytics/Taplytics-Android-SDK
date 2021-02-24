/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.analytics.external;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.taplytics.sdk.Reflektor;
import com.taplytics.sdk.analytics.external.adobe.AdobeManager;
import com.taplytics.sdk.datatypes.TLProperties;
import com.taplytics.sdk.managers.TLManager;
import com.taplytics.sdk.managers.TLThreadManager;
import com.taplytics.sdk.utils.Functionality;
import com.taplytics.sdk.utils.TLLog;
import com.taplytics.sdk.utils.TLUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import dalvik.system.DexFile;

/**
 * Created by VicV on 12/8/14.
 * <p/>
 * Manager for all external analytics managers.
 */
public class TLExternalAnalyticsManager {

    public static final String TLAnalyticsSourceGA = "googleAnalytics";
    public static final String TLAnalyticsSourceMixPanel = "mixpanel";
    public static final String TLAnalyticsSourceFlurry = "flurry";
    public static final String TLAnalyticsSourceAmplitude = "amplitude";

    public static final String TLExperiments = "TL_experiments";
    public static final String TLExperimentsSegment = "Experiment Viewed";


    private static TLExternalAnalyticsManager instance;

    private AmplitudeManager amplitudeManager;

    //TODO: REFACTOR BACK TO OLD WAY THAT DOESNT USE ABSEXTERNALSOURCEMANAGERS
    public ArrayList<AbsExternalAnalyticsManager> getKnownManagers() {
        return knownManagers;
    }

    private ArrayList<AbsExternalAnalyticsManager> knownManagers = new ArrayList<>();

    private boolean mixpanelEnabled = false;

    private boolean flurryEnabled = false;

    private boolean googleAnalyticsEnabled = false;

    private boolean localyticsEnabled = false;

    private boolean amplitudeEnabled = false;

    private boolean segmentEnabled = false;

    private boolean expAmplitudeEventFormat = false;

    private final AdobeManager adobeManager;

    public void setAdobeFormat(AdobeManager.Format format) {
        adobeManager.setFormat(format);
    }

    public void setAmplitudeFormat(Boolean formatAsEvents) {
        this.expAmplitudeEventFormat = formatAsEvents;
    }


    public TLExternalAnalyticsManager() {
        adobeManager = new AdobeManager();
    }

    private PendingIntent mixpanelPendingIntent;

    public static TLExternalAnalyticsManager getInstance() {
        if (instance == null) {
            instance = new TLExternalAnalyticsManager();
        }
        return instance;
    }

    public static enum AdobeFormat {
        ORIGINAL,
        BASELINE_A,
        TL_EXP_1
    }

    /**
     * Simply make a reflective call to a kn own class in MixPanel. If it causes an exception, it doesnt exist. *
     */
    private boolean appHasMixpanel() {
        try {
            Class.forName("com.mixpanel.android.mpmetrics.MixpanelAPI");
            return true;
        } catch (Exception e) {
            // No Mixpanel
            mixpanelEnabled = false;
            return false;
        }
    }

    /**
     * Simply make a reflective call to a kn own class in flurry. If it causes an exception, it doesnt exist.
     */
    private boolean appHasFlurry() {
        try {
            Class.forName("com.flurry.android.FlurryAgent");
            return true;
        } catch (Exception e) {
            // No Flurry
            flurryEnabled = false;
            return false;
        }
    }

    /**
     * Simple make a reflective call to a known class in Adobe. If it causes an exception it doesn't exist
     */
    private Boolean appHasAdobe() {
        return adobeManager.appHasAdobe();
    }


    /**
     * Simply make a reflective call to a known class in GoogleAnalytics. If it causes an exception, it doesnt exist. *
     */
    private boolean appHasGoogleAnalytics() {
        try {
            Class.forName("com.google.android.gms.analytics.GoogleAnalytics");
            return true;
        } catch (Exception e) {
            googleAnalyticsEnabled = false;
            return false;
        }
    }

    private boolean appHasLocalytics() {
        try {
            Class.forName("com.localytics.android.AnalyticsListener");
            return true;
        } catch (Exception e) {
            localyticsEnabled = false;
            return false;
        }
    }

    /**
     * Simply make a reflective call to a known class in Amplitude. If it causes an exception, it doesnt exist. *
     */
    private boolean appHasAmplitude() {
        try {
            Class.forName("com.amplitude.api.Amplitude");
            return true;
        } catch (Exception e) {
            amplitudeEnabled = false;
            return false;
        }
    }

    /**
     * Simply make a reflective call to a known class in Segment. If it causes an exception, it doesnt exist. *
     */
    private boolean appHasSegment() {
        try {
            Class.forName("com.segment.analytics.Analytics");
            return true;
        } catch (Exception e) {
            segmentEnabled = false;
            return false;
        }
    }

    public void enableMixpanel() {
        mixpanelEnabled = true;
    }

    public void enableGoogleAnalytics() {
        googleAnalyticsEnabled = true;
    }

    public void enableLocalytics() {
        localyticsEnabled = true;
    }

    public void enableAdobe() {
        adobeManager.setEnabled(true);
    }

    public void enableFlurry() {
        flurryEnabled = true;
    }

    public void enableAmplitude() {
        amplitudeEnabled = true;
    }

    public void enableSegment() {
        segmentEnabled = true;
    }

    /**
     * Force Mixpanel to flush its data so its timer resets. *
     */
    public void forceFlushAndSendToTaplytics() {
        TLUtils.executeAsyncTask(new MixPanelBackgroundDumper(), knownManagers);
    }

    private class MixPanelBackgroundDumper extends AsyncTask<ArrayList<AbsExternalAnalyticsManager>, Void, Boolean> {

        @Override
        protected final Boolean doInBackground(ArrayList<AbsExternalAnalyticsManager>[] params) {
            try {
                for (AbsExternalAnalyticsManager param : params[0]) {
                    param.trackToTaplyticsAndFlush();
                }

            } catch (Exception e) {
                return false;
            }
            return true;
        }

    }

    /**
     * Force Mixpanel to flush its data so its timer resets. *
     */
    private void resetFlurrySessions() {
        TLUtils.executeAsyncTask(new FlurryMapSetter());
    }


    //What we have here are a bunch of things we grab the first time we run Taplytics (if they exist)
    //We save them so we don't have to run through the complex stuff ever again.
    private Method flurryEventManagerMethod;
    private Method flurryEventLoggerMethod;
    private Class flurryEventLogger, flurryEventManager;
    private Field[] flurryEventManagerFields = null;
    private Field flurryEventListField = null;
    private Field flurryEventNameField = null;
    private Method flurryEventHashMethod = null;

    private class FlurryMapSetter extends AsyncTask<Void, Void, Void> {

        @Override
        protected final Void doInBackground(Void... params) {
            try {
                if (flurryEventManagerMethod == null || flurryEventLoggerMethod == null || flurryEventLogger == null || flurryEventManager == null || flurryEventManagerFields == null) {
                    //Get EVERY class in our app + sdk.
                    DexFile dexFile = new DexFile(TLManager.getInstance().getAppContext().getPackageCodePath());
                    //Iterate through EVERY class in the app + sdk
                    ArrayList<String> flurryClasses = new ArrayList<>();
                    for (Enumeration<String> iter = dexFile.entries(); iter.hasMoreElements(); ) {
                        try {
                            String className = iter.nextElement();
                            if (className.contains("flurry.sdk")) {
                                flurryClasses.add(className);
                                Class c = Class.forName(iter.nextElement());
                                if (flurryEventManager == null) {
                                    Field[] fields = null;
                                    try {
                                        fields = c.getDeclaredFields();
                                    } catch (Throwable e) {
                                        //
                                    }
                                    // The class we need is called FlurryEventManager.
                                    // Its not actually called that but we will call it that.
                                    // It is the only class within flurry with more than 40 fields,
                                    if (fields != null && fields.length > 30) {
////                                        TLLog.debug("Found flurry event manager: " + c.getCanonicalName());
                                        flurryEventManager = c;
                                        flurryEventManagerFields = fields;
                                    }
                                } else {
                                    if (!isClassFlurryEventManager(c)) {
                                        flurryClasses.remove(className);
                                    } else {
                                        break;
                                    }
                                }
                            }
                        } catch (Throwable e) {
//                            TLLog.error("flr problems", e);
                        }
                    }

                    //Re-iterate through the classes, just in case we missed the one we wanted the first time through (we needed the flurryeventmanager to find flurryeventlogger, the other one we need)
                    if (flurryEventLogger == null) {
                        for (String className : flurryClasses) {
                            try {
                                Class c = Class.forName(className);
                                if (isClassFlurryEventManager(c)) {
                                    break;
                                }

                            } catch (Throwable e) {
//                                TLLog.error("flr problems", e);
                                //Ignore, its expected.
                            }
                        }
                    }


                    //Now we have found eventLogger and eventManager
                    if (flurryEventLogger != null) {
                        for (Method m : flurryEventLogger.getDeclaredMethods()) {

                            //This is how we find our actual instance of the eventLogger (returns instance of itself)
                            if (m.getReturnType() == flurryEventLogger) {
                                flurryEventLoggerMethod = m;
                            } else if (m.getReturnType() == flurryEventManager && m.getParameterTypes().length == 0) {
                                //This is the method that returns the instance of the EventManager, from within the EventLogger, but we need an instance of the EventLogger to get the proper one.
                                flurryEventManagerMethod = m;
                            }
                            if (flurryEventManagerMethod != null && flurryEventLoggerMethod != null) {
                                break;
                            }
                        }
                    }
                }

                if (flurryEventLoggerMethod != null && flurryEventManagerMethod != null && flurryEventManagerFields != null && flurryEventLogger != null && flurryEventManager != null) {
                    //Finally grab our instance of the eventManager
                    flurryEventLoggerMethod.setAccessible(true);
                    flurryEventManagerMethod.setAccessible(true);
                    Object flurryEventManagerInstance = flurryEventManagerMethod.invoke(flurryEventLoggerMethod.invoke(flurryEventManager));
                    if (flurryEventManagerInstance != null) {
                        for (Field f : flurryEventManagerFields) {
                            if (flurryEventListField == null && f.getType() == List.class) {
                                ParameterizedType listType = (ParameterizedType) f.getGenericType();
                                Class<?> listClass = (Class<?>) listType.getActualTypeArguments()[0];
                                for (Field listField : listClass.getDeclaredFields()) {
                                    if (flurryEventListField == null) {
                                        if (listField.getType() == Map.class) {
                                            flurryEventListField = f;
//                                            TLLog.debug("Found flurry events list field");
                                            break;
                                        }
                                    }
                                }
                                for (Field listField : listClass.getFields()) {
                                    if (listField.getType() == String.class) {
                                        flurryEventNameField = listField;
//                                        TLLog.debug("Found flurry event name field");
                                        break;
                                    }
                                }

                                for (Method m : listClass.getDeclaredMethods()) {
                                    if (m.getReturnType() == Map.class) {
                                        flurryEventHashMethod = m;
//                                        TLLog.debug("Found flurry event hash field");

                                        break;
                                    }
                                }
                            }

                            if (f.getType() == Map.class && f.getType() != TLHashMap.class) {
                                //There are two maps in this class. Just change them both. Doesnt seem to break anything.
                                f.setAccessible(true);
                                Map current = (Map) f.get(flurryEventManagerInstance);
                                TLHashMap map = new TLHashMap();
                                if (current != null) {
                                    map.putAll(current);
                                }
                                f.set(flurryEventManagerInstance, map);
                            }
                        }
                    }

                }

            } catch (Throwable e) {
//                TLLog.error("Flurry problems", e instanceof Exception ? (Exception) e : null);

            }
            return null;
        }

    }

    private boolean isClassFlurryEventManager(Class c) {
        Method[] decMethods;
        Method[] regMethods;
        ArrayList<Method> methods = new ArrayList<>();

        try {
            decMethods = c.getDeclaredMethods();
            regMethods = c.getMethods();
            Collections.addAll(methods, decMethods);
            Collections.addAll(methods, regMethods);
        } catch (Throwable e) {
            //
        }

        for (Method method : methods) {
            if (method.getReturnType() == flurryEventManager) {
                //Found flurry event logger
                TLLog.debug("found flurry Event Logger" + c.getCanonicalName());
                flurryEventLogger = c;
                break;
            }
        }
        //Get out of this loop when we find it.
        return flurryEventLogger != null;
    }

    HashMap getFlurryEventHashForName(String key) {
        try {
            if (flurryEventListField != null) {
                flurryEventLoggerMethod.setAccessible(true);
                flurryEventManagerMethod.setAccessible(true);
                flurryEventNameField.setAccessible(true);
                flurryEventHashMethod.setAccessible(true);
                flurryEventListField.setAccessible(true);
                Object eventManager = flurryEventManagerMethod.invoke(flurryEventLoggerMethod.invoke(flurryEventManager));
                List l = (List) flurryEventListField.get(eventManager);
                for (Object o : l) {
                    if (flurryEventNameField.get(o).equals(key)) {
                        return (HashMap) flurryEventHashMethod.invoke(o);
                    }
                }
            }
        } catch (Throwable e) {
            return null;
        }
        return null;
    }

    /**
     * Here we pull out the event name from the map and make that our source event name. We also take the value (if it exists) then just toss everything into the metadata.
     *
     * @param map The raw map from a google analytics event.
     */
    void parseAndSendGAEvent(Map<String, String> map) {
        if (!TLManager.getInstance().getTrackingEnabled())
            return;
        try {
            JSONObject metaData = new JSONObject(map);
            Object value = null;
            String eventName = "";
            //We don't want this if this is a screenview tracking because we do this implicitly.
            if (map.containsKey("&t") && map.get("&t").equals("screenview")) {
                return;
            }
            if (map.containsKey("&ev")) {
                value = map.get("&ev");
            }
            if (map.containsKey("&ea")) {
                eventName = map.get("&ea");
            }

            //Track our newly grabbed event!
            TLManager.getInstance().getTlAnalytics()
                    .trackSourceEvent(TLExternalAnalyticsManager.TLAnalyticsSourceGA, eventName, value, metaData);
        } catch (Exception e) {
            TLLog.error(this.getClass().getCanonicalName(), e);
        }

    }

    /**
     * Get the source analytics engine started. Start up mixpanel if it exists, then try to grab google's main tracker thread too.
     */
    public void setupExternalAnalyticsManager() {
        if (mixpanelEnabled && appHasMixpanel()) {
            knownManagers.add(MixpanelManager.getInstance());
        }
        if (googleAnalyticsEnabled && appHasGoogleAnalytics()) {
            try {

                //Grab the thread if it exists (wait 7 secs for it to be initialized by the actual app);
                final Handler handler = new Handler(Looper.getMainLooper());

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        checkForGAThread();
                    }
                }, 3000);


            } catch (Exception e) {
                //External analytics issues.
                TLLog.error("ExtA GA issues", e);
            }
        }

        if (amplitudeEnabled && appHasAmplitude()) {
            try {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            amplitudeManager = AmplitudeManager.getInstance();
                            amplitudeManager.registerAmplitudeTracking();
                        } catch (Throwable ignored) {

                        }
                    }
                }, 2000);

            } catch (Throwable e) {
                //
            }
        }

        if (localyticsEnabled && appHasLocalytics()) {
            setupLocalytics();
        }

        if (adobeManager.isEnabled() && adobeManager.appHasAdobe()) {
            //Force adobe to create the executor we need to grab
            adobeManager.setExecutor();
        }

        if (flurryEnabled && appHasFlurry()) {
            //Force flurry to propagate, and then set up the tracking with the hopes that the propagation has successfully went through.
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {

                @Override
                public void run() {
                    try {
                        //Force flurry to instantiate a class we need by forcing a basic event through.
                        Class c = Class.forName("com.flurry.android.FlurryAgent");
                        if (c != null) {
                            Method m = c.getMethod("logEvent", String.class);
                            m.invoke(c, "app started");
                        }
                    } catch (Throwable e) {
//                        TLLog.error("flr", e instanceof Exception ? (Exception) e : null);
                    }

                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {

                        @Override
                        public void run() {
                            setupFlurryTracking();
                        }
                    }, 750);
                }
            }, 1000);
        }


        if (knownManagers.size() > 0) {
            if (mixpanelEnabled && appHasMixpanel()) {
                // Start up a real quick alarm that repeats.
                AlarmManager mixpanelAlarmManager = (AlarmManager) TLManager.getInstance().getAppContext().getSystemService(Context.ALARM_SERVICE);

                // The receiver we use to get the flush info.
                BroadcastReceiver receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        forceFlushAndSendToTaplytics();
                    }
                };

                // Register the receiver to the app.
                TLManager.getInstance().getAppContext().registerReceiver(receiver, new IntentFilter("com.taplytics"));
                mixpanelPendingIntent = PendingIntent.getBroadcast(TLManager.getInstance().getAppContext(), 109, new Intent("com.taplytics"), 0);

                // Do the first one in 35 seconds, just in case. After that, do it every 52 seconds. (Mixpanel's default is 60 seconds.)
                mixpanelAlarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, 35000, 52000, mixpanelPendingIntent);
            }
        }


    }

    public void unRegisterExternalAnalytics() {
        unregisterMixpanelTracking();
        if (amplitudeManager != null) {
            try {
                amplitudeManager.unRegisterAmplitudeTracking();
            } catch (Throwable e) {
                //safety
            }
        }

    }

    private void unregisterMixpanelTracking() {
        if (mixpanelPendingIntent != null) {
            try {
                ((AlarmManager) TLManager.getInstance().getAppContext().getSystemService(Context.ALARM_SERVICE)).cancel(mixpanelPendingIntent);
            } catch (Throwable e) {
                //Safety
            }
        }
    }

    public void logGAEvent(Object t, Object h) {
        try {

            Class trackerClass = Class.forName("com.google.android.gms.analytics.Tracker");
            //Make sure these are usable objects.
            if (t.getClass().equals(trackerClass) && h instanceof Map && !TLUtils.isDisabled(Functionality.GOOGLE)) {
                Map hitBuilder = (Map) h;
                trackerClass.getMethod("send", Map.class).invoke(t, hitBuilder);
                String eventName = "";
                Number value = 0;
                JSONObject metaData = new JSONObject();

                //Grab the event name if it exists.
                if (hitBuilder.containsKey("&ea")) {
                    eventName = (String) hitBuilder.get("&ea");
                    hitBuilder.remove("&ea");
                }

                //Grab the event value IF its a number.
                if (hitBuilder.containsKey("&ev")) {
                    Object val = hitBuilder.get("&ev");
                    if (val instanceof String) {
                        try {
                            //Convert back to a Long
                            value = Long.valueOf((String) val);
                            hitBuilder.remove("&ev");
                        } catch (Exception e) {
                            //Not a long
                        }
                    }
                }

                //In case an event name wasn't set, just fallback to the type of event it is.
                if (eventName.equals("")) {
                    if (hitBuilder.containsKey("&t")) {
                        eventName = (String) hitBuilder.get("&t");
                        hitBuilder.remove("&t");
                    }
                }

                //Round up the rest of the values into our metaData
                for (Object k : hitBuilder.keySet()) {
                    if (k instanceof String) {
                        String key = (String) k;
                        try {
                            metaData.put(key, hitBuilder.get(key));
                        } catch (Exception e) {
                            //Safety catch here so we still send even on failures.
                            TLLog.error("error adding to GA metadata map", e);
                        }
                    }
                }

                //Send off our source event.
                if (TLManager.getInstance().getTlAnalytics() != null) {
                    TLManager.getInstance().getTlAnalytics()
                            .trackSourceEvent(TLExternalAnalyticsManager.TLAnalyticsSourceGA, eventName, value, metaData);
                } else {
                    Log.w("Taplytics", "Taplytics not yet instantiated. Call Taplytics.startTaplytics before any other Taplytics call.");
                }
            }

        } catch (Throwable e) {
            //Doing this in a try/catch to avoid compiler errors if they dont have GA
            TLLog.error("GoogleAnalytics error", (e instanceof Exception) ? (Exception) e : null);
        }
    }


    /**
     * Start the flurry tracking stuff by replacing a crucial hashmap within the flurry sdk. *
     */
    public void setupFlurryTracking() {
        //CRAZY FLURRY STUFF
        resetFlurrySessions();

    }


    /**
     * Get the google analytics thread by name, and then change the LinkedBlockingQueue within it to our own (see {@link com.taplytics.sdk.analytics.external.TLLinkedBlockingQueue})
     */
    private void checkForGAThread() {
        try {
            //Snag the google analytics thread from thin air (waiting for 3 secs)
            Thread thread = TLUtils.getThreadByName("GAThread");
            if (thread != null) {
                Field[] fields = thread.getClass().getDeclaredFields();
                for (Field f : fields) {
                    if (f.getType().equals(LinkedBlockingQueue.class)) {
                        f.setAccessible(true);
                        f.set(thread, new TLLinkedBlockingQueue());
                    }
                }
            }
        } catch (Exception e) {
            TLLog.error("GA Thread", e);
        }
    }


    //creates a fake localytics analytics listener, then adds it to localytics. Which will track all their events automatically
    private void setupLocalytics() {

        try {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        //get the necessary classes and methods
                        Class localytics = Class.forName("com.localytics.android.Localytics");
                        Class listenerInterface = Class.forName("com.localytics.android.AnalyticsListener");
                        Method addListener;
                        try {
                            addListener = localytics.getMethod("addAnalyticsListener", listenerInterface);
                        } catch (Throwable t) {
                            addListener = localytics.getMethod("setAnalyticsListener", listenerInterface);

                        }
                        //add a the proxy listener, the proxy class will receive any method calls
                        addListener.invoke(localytics, Proxy.newProxyInstance(listenerInterface.getClassLoader(), new Class[]{listenerInterface}, new InvocationHandler() {
                            @Override
                            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                                switch (method.getName()) {
                                    //the method called whenever an event is "tagged"
                                    case "localyticsDidTagEvent":
                                        try {
                                            //get the event details and send to taplytics
                                            String eventName = (String) args[0];
                                            Map<String, String> attributes = (Map) args[1];
                                            long valueIncrease = (long) args[2];
                                            JSONObject metaData = null;
                                            if (attributes != null) {
                                                metaData = new JSONObject(attributes);
                                            }
                                            TLManager.getInstance().getTlAnalytics().trackSourceEvent("localytics", eventName, valueIncrease, metaData);
                                        } catch (Exception e) {
                                            TLLog.error("Failed to log localytics event: " + e.getMessage());
                                        }
                                        break;
                                    //the hashcode is needed for when the listener is put into the hashset of listeners by localytics
                                    case "hashCode":
                                        return hashCode();

                                }
                                return null;
                            }
                        }));
                    } catch (Throwable e) {
                        TLLog.debug("Problem setting up localyics tracking: " + e.getMessage());
                    }


                }
            });

        } catch (Throwable e) {
            TLLog.error("Error when posting localytics runnable " + e.getMessage());
        }
    }

    //Sends the experiment information to the external analytics sources which are enabled
    //Waits 5 seconds so that any analytics sources that should be enabled are enabled
    public void sendExperimentDataToAnalyticsSources(final Map<String, String> data, final Object gaTracker) {
        TLThreadManager.getInstance().scheduleOnBackgroundThread(new Runnable() {
            @Override
            public void run() {
                if (TLManager.getInstance().getCurrentActivity() == null) {
                    return;
                }

                TLProperties properties = TLManager.getInstance().getTlProperties();
                if (!(properties != null && data != null && data.size() != 0 && properties.getPushToAnalyticsSources() != null)) {
                    return;
                }

                JSONObject pas = properties.getPushToAnalyticsSources();
                if (amplitudeEnabled && appHasAmplitude() && pas.optBoolean("amplitude", false)) {
                    if (expAmplitudeEventFormat) {
                        logToAmplitude(data);
                    } else {
                        setAmplitudeUserProperties(data);
                    }
                }

                if (adobeManager.isEnabled() &&
                        adobeManager.appHasAdobe() &&
                        pas.optBoolean("adobe", false)) {
                    adobeManager.log(data);
                }

                if (localyticsEnabled && appHasLocalytics() && pas.optBoolean("localytics", false)) {
                    logToLocalytics(data);
                }
                if (flurryEnabled && appHasFlurry() && pas.optBoolean("flurry", false)) {
                    logToFlurry(data);
                }
                if (mixpanelEnabled && appHasMixpanel() /*&& pas.optBoolean("mixpanel", false)*/) {
                    registerMixpanelSuperProperties(data);
                }
                if (googleAnalyticsEnabled && appHasGoogleAnalytics() && pas.optBoolean("googleAnalytics", false)) {
                    if (gaTracker != null) {
                        logToGA(data, gaTracker);
                        // keep for backwards compatibility
                        setGAProperties(data, gaTracker);
                    }
                }
                if (segmentEnabled && appHasSegment() && pas.optBoolean("segment", false)) {
                    logToSegment(data);
                }
            }
        }, 5000, TimeUnit.MILLISECONDS);
    }


    /**
     * Logs the running experiments and variations to amplitude under the event "TL_experiments"
     *
     * @param data the experiments to be logged to amplitude
     */
    private void logToAmplitude(Map<String, String> data) {
        try {
            //get an instance of the amplitude class
            Class amplitude = Class.forName("com.amplitude.api.Amplitude");
            Method m = amplitude.getMethod("getInstance");
            Object i = m.invoke(amplitude);
            //get the logging method and log the event
            Method logger = i.getClass().getMethod("logEvent", String.class, JSONObject.class);
            logger.invoke(i, TLExperiments, new JSONObject(data));
            TLLog.debug("Logged experiment data to Amplitude: " + data.toString(), true);
        } catch (Exception e) {
            TLLog.debug("Logging experiment data to Amplitude failed: " + e.getMessage(), true);
        }
    }

    private static void setAmplitudeUserProperties(Map<String, String> data) {
        try {
            //get an instance of the amplitude class
            Class amplitude = Class.forName("com.amplitude.api.Amplitude");
            Method m = amplitude.getMethod("getInstance");
            Object i = m.invoke(amplitude);
            //get the logging method and log the event
            JSONObject obj = new JSONObject();
            obj.put("TL_Experiments", formatForAmplitude(data));
            Method logger = i.getClass().getMethod("setUserProperties", JSONObject.class);
            logger.invoke(i, obj);
            TLLog.debug("Set experiment data as Amplitude User Property: " + obj.toString(), true);
        } catch (Exception e) {
            TLLog.debug("Setting experiment data as Amplitude properties failed: " + e.getMessage(), true);
        }
    }

    /**
     * Logs the running experiments and variations to localytics under the event "TL_experiments"
     *
     * @param data the experiments to be logged to localytics
     */
    private void logToLocalytics(Map<String, String> data) {
        try {
            //gets the tagEvent method
            Class localytics = Class.forName("com.localytics.android.Localytics");
            Method m = localytics.getMethod("tagEvent", String.class, Map.class);
            //logs the event
            m.invoke(localytics, TLExperiments, data);
            TLLog.debug("Logged experiment data to localytics!", true);
        } catch (Exception e) {
            TLLog.debug("Logging experiment data to localytics failed: " + e.getMessage(), true);
        }
    }

    /**
     * Logs the running experiments and variations to flurry under the event "TL_experiments"
     *
     * @param data the experiments to be logged to flurry
     */
    private void logToFlurry(Map<String, String> data) {
        try {
            //gets the log event method
            Class flurry = Class.forName("com.flurry.android.FlurryAgent");
            Method m = flurry.getMethod("logEvent", String.class, Map.class);
            //logs the event
            m.invoke(flurry, TLExperiments, data);
            TLLog.debug("Logged experiment data to Flurry: " + data.toString(), true);
        } catch (Throwable e) {
            TLLog.debug("Logging experiment data to flurry failed: " + e.getMessage(), true);
        }
    }

    private static void logToSegment(Map<String, String> data) {
        try {
            TLProperties props = TLManager.getInstance().getTlProperties();
            HashMap map = new HashMap();
            for (String key : data.keySet()) {
                JSONObject experiment = props.getExperimentByName(key);
                JSONObject variation = props.getVariationByName(experiment, data.get(key));
                map.put("experiment_id", experiment.optString("_id"));
                map.put("experiment_name", key);
                map.put("variation_id", variation.optString("_id").equals("") ? "baseline" : variation.optString("_id"));
                map.put("variation_name", data.get(key));
                Reflektor properties = new Reflektor("com.segment.analytics.Properties");
                properties.getTargetClass().getSuperclass().getDeclaredMethod("putAll", Map.class).invoke(properties.getTarget(), map);
                Object analytics = Reflektor.invokeStatic("com.segment.analytics.Analytics", "with", new Class[]{Context.class}, new Object[]{TLManager.getInstance().getAppContext()});
                analytics.getClass().getDeclaredMethod("track", String.class, properties.getTargetClass()).invoke(analytics, TLExperimentsSegment, properties.getTarget());
            }

            TLLog.debug("Logged experiment data to segment: " + data.toString(), true);
        } catch (Exception e) {
            TLLog.debug("Logging experiment data to segment failed: " + e.getMessage());
        }
    }


    /**
     * Registers the currently running experiments as super properties with the mixpanel tracker
     * `
     *
     * @param data the experiments to be set as a super property for mixpanel
     */
    private void registerMixpanelSuperProperties(@Nullable Map<String, String> data) {
        try {
            //get the mixpanelAPI class
            final Class<?> mixpanelApi = Class.forName("com.mixpanel.android.mpmetrics.MixpanelAPI");

            //get the instace map
            final Field f = mixpanelApi.getDeclaredField("sInstanceMap");
            f.setAccessible(true);

            final Map<?, ?> mixpanelMap = (Map<?, ?>) f.get(null);

            //grab the token, as we need a token to get an instance of the mixpanel object
            final String token = (String) mixpanelMap.keySet().toArray()[0];

            //now that we have the token we can do things normally
            final Method mixpanelInstanceMethod = mixpanelApi.getMethod("getInstance", Context.class, String.class);
            final Object mixpanel = mixpanelInstanceMethod.invoke(null, TLManager.getInstance().getAppContext(), token);

            final JSONObject formattedData = new JSONObject(getFormattedMixpanelData(data));

            //get the registration method
            final Method register = mixpanelApi.getMethod("registerSuperProperties", JSONObject.class);
            register.invoke(mixpanel, formattedData);

            TLLog.debug("Registered super properties with Mixpanel: \n" + formattedData.toString(4), true);
        } catch (Exception e) {
            TLLog.debug("Mixpanel registering super properties failed: " + e.getMessage(), true);
        }
    }

    /**
     * Formats data to be sent to Mixpanel
     *
     * @param data Data to be formatted
     * @return Formatted data in the form
     */
    @NonNull
    private Map<String, List<String>> getFormattedMixpanelData(@Nullable final Map<String, String> data) {

        final Map<String, List<String>> props = new HashMap<>();
        final List<String> formattedDataArray = new ArrayList<>();

        if (data != null) {
            for (final Map.Entry<String, String> entry : data.entrySet()) {
                final String formattedData = entry.getKey() + ":" + entry.getValue();
                formattedDataArray.add(formattedData);
            }
        }

        props.put("TL_Experiments", formattedDataArray);
        return props;
    }

    void logToGA(Map<String, String> data, Object gaTracker) {
        try {
            Reflektor tracker = new Reflektor(gaTracker);
            Reflektor builder = new Reflektor("com.google.android.gms.analytics.HitBuilders$EventBuilder");

            for (String key : data.keySet()) {
                String val = data.get(key);
                if (val != null) {
                    builder.invoke("setCategory", new Class[]{String.class}, new Object[]{TLExperiments});
                    builder.invoke("setAction", new Class[]{String.class}, new Object[]{key});
                    builder.invoke("setLabel", new Class[]{String.class}, new Object[]{val});

                    Method m = builder.getTargetClass().getMethod("build");
                    Object dic = m.invoke(builder.getTarget());
                    if (dic != null && dic.getClass().equals(HashMap.class)) {
                        tracker.invoke("send", new Class[]{Map.class}, new Object[]{dic});
                    }
                }
            }
            TLLog.debug("Logged experiment data to GA: " + data.toString(), true);
        } catch (Exception e) {
            TLLog.debug("Logging experiment data to GA failed: " + e.getMessage(), true);
        }
    }

    /**
     * Sets properties on all GA events, using the given tracker
     *
     * @param data the experiments to set as properties for GA
     * @param o    the tracker object which is used to log events
     */
    public void setGAProperties(Map<String, String> data, Object o) {
        try {
            if (o != null) {
                Reflektor tracker = new Reflektor(o);
                for (String key : data.keySet()) {
                    tracker.invoke("set", new Class[]{String.class, String.class}, new Object[]{"&" + key.replace(" ", "_"), data.get(key)});
                }
            }
        } catch (Throwable e) {
            TLLog.debug("Logging experiment data to GA failed: " + e.getMessage());
        }
    }

    static Map<String, String> formatForAdobe(Map<String, String> data, AdobeFormat newAdobeFormat) {
        Map<String, String> formatted = new HashMap<>();

        switch (newAdobeFormat) {
            case BASELINE_A:
                if (data == null || data.isEmpty()) {
                    formatted.put("tl_exp", "");
                    return formatted;
                }
                StringBuilder dataAsStr = new StringBuilder();
                for (Map.Entry<String, String> exp : data.entrySet()) {
                    if (dataAsStr.length() != 0) {
                        dataAsStr.append(", ");
                    }
                    if (exp.getValue() == "baseline") {
                        dataAsStr.append(exp.getKey())
                                .append(":A");
                    } else {
                        dataAsStr.append(exp.getKey())
                                .append(":")
                                .append(exp.getValue());
                    }
                }
                formatted.put("tl_exp", dataAsStr.toString());
                break;
            case TL_EXP_1:
                int count = 1;
                for (Map.Entry<String, String> exp : data.entrySet()) {
                    formatted.put("experiment_" + count, exp.getKey() + " | " + exp.getValue());
                    count++;
                }
                formatted.put("tl_exp", "1");
                break;
        }

        return formatted;
    }

    static JSONArray formatForAmplitude(Map<String, String> data) {
        JSONArray formattedExperiments = new JSONArray();
        try {
            for (Map.Entry<String, String> exp : data.entrySet()) {
                formattedExperiments.put(exp.getKey() + ":" + exp.getValue());
            }
        } catch (Throwable t) {
            TLLog.error("Error formatting data for amplitude", t);
        }
        return formattedExperiments;
    }
}
