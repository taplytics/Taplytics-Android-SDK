/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.analytics.external;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Pair;

import com.taplytics.sdk.datatypes.TLOkHttpClient;
import com.taplytics.sdk.managers.TLManager;
import com.taplytics.sdk.utils.TLLog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;

import okhttp3.OkHttpClient;

/**
 * Created by clvcooke on 10/2/15.
 */
public class AmplitudeManager {

    private static AmplitudeManager instance;

    private Method getAmplitudeDBHelper;
    private Method getAmplitudeEvents;
    private Class amplitudeDBHelperClass;
    private Handler amplitudeHandler;

    private SharedPreferences.OnSharedPreferenceChangeListener listener;
    private long greatestAmplitudeId = 0;


    public static AmplitudeManager getInstance() {
        if (instance == null) {
            instance = new AmplitudeManager();
        }
        return instance;
    }

    void unRegisterAmplitudeTracking() {

        try {
            Class amplitudeClass = Class.forName("com.amplitude.api.AmplitudeClient");
            Object instance = amplitudeClass.getMethod("getInstance").invoke(amplitudeClass);
            Field f = instance.getClass().getDeclaredField("httpClient");
            f.setAccessible(true);
            f.set(instance, new OkHttpClient());
        } catch (Throwable e) {
            //
        }

        try {
            if (listener != null) {
                TLManager.getInstance().getAppContext().getSharedPreferences("com.amplitude.api." + TLManager.getInstance().getAppContext().getPackageName(), Context.MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(listener);
            }
        } catch (Throwable e) {
            //
        }
    }


    void registerAmplitudeTracking() {
        SharedPreferences preferences = null;
        try {
            Context context = TLManager.getInstance().getAppContext();
            String name = "com.amplitude.api." + context.getPackageName();
            preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE);
        } catch (Throwable e) {

        }

        try {
            Class amplitudeClass;
            Object instance;
            try {
                 amplitudeClass = Class.forName("com.amplitude.api.Amplitude");
                 instance = amplitudeClass.getMethod("getInstance").invoke(amplitudeClass);
            } catch (Throwable t){
                //We are on an old amplitude version
                amplitudeClass = Class.forName("com.amplitude.api.AmplitudeClient");
                instance = amplitudeClass.getMethod("getInstance").invoke(amplitudeClass);
            }
            Field f = instance.getClass().getDeclaredField("httpClient");
            f.setAccessible(true);
            f.set(instance, new TLOkHttpClient());

        } catch (Throwable e) {
            //
        }

        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key != null && key.equals("com.amplitude.api.lastEventId")) {
                    //get the id
                    try {
                        //this is done in an attempt to get in front of the deletion that occurs at 30 events
                        amplitudeHandler.postAtFrontOfQueue(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    //get the database helper
                                    getAmplitudeDBHelper.setAccessible(true);
                                    Object dbHelper = getAmplitudeDBHelper.invoke(amplitudeDBHelperClass, TLManager.getInstance().getAppContext());
                                    getAmplitudeDBHelper.setAccessible(false);
                                    //due to some async issues we don't know how many events we have missed, so grab events up to the latest id we know about and record that id
                                    getAmplitudeEvents = dbHelper.getClass().getDeclaredMethod("getEvents", long.class, long.class);
                                    getAmplitudeEvents.setAccessible(true);

                                    //the id is irrelevant as I just want the most recent event
                                    //get thirty because there will as most be thirty in the db
                                    Pair<Long, JSONArray> event = (Pair<Long, JSONArray>) getAmplitudeEvents.invoke(dbHelper, Long.MAX_VALUE, 30l);
                                    getAmplitudeEvents.setAccessible(false);
                                    //send the event
                                    for (int i = 0; i < event.second.length(); i++) {
                                        if (event.second.opt(i) instanceof JSONObject) {
                                            JSONObject value = event.second.getJSONObject(i);
                                            if (value.optLong("event_id") > greatestAmplitudeId) {
                                                greatestAmplitudeId = value.optLong("event_id");
                                                TLManager.getInstance().getTlAnalytics().trackSourceEvent(TLExternalAnalyticsManager.TLAnalyticsSourceAmplitude, value.optString("event_type"), null, value.optJSONObject("event_properties"));
                                                TLLog.debug("logged amplitude event");

                                                if (value.optLong("event_id") > greatestAmplitudeId) {
                                                    greatestAmplitudeId = value.optLong(("event_id"));
                                                }
                                                // we can skip some events if our id isn't large enough
                                            } else {
                                                i += greatestAmplitudeId - value.optLong("event_id");
                                            }
                                        }
                                    }

                                } catch (Exception e) {
                                    //do nothing
                                    TLLog.error("error while getting amplitude event: " + e.getMessage());

                                }
                            }
                        });
                    } catch (Exception e) {
                        TLLog.error("error while getting amplitude event: " + e.getMessage());
                    }
                }
            }
        };


        //just flat out grab the class
        try {
            preferences.registerOnSharedPreferenceChangeListener(listener);
            amplitudeDBHelperClass = Class.forName("com.amplitude.api.DatabaseHelper");
            getAmplitudeDBHelper = amplitudeDBHelperClass.getDeclaredMethod("getDatabaseHelper", Context.class);

            Class amplitudeClient = Class.forName("com.amplitude.api.AmplitudeClient");
            Method instance = amplitudeClient.getMethod("getInstance");
            Object client = instance.invoke(amplitudeClient);
            Field thread = amplitudeClient.getDeclaredField("logThread");
            thread.setAccessible(true);
            Object amplitudeLogThread = thread.get(client);
            thread.setAccessible(false);
            Method newHandler = amplitudeLogThread.getClass().getDeclaredMethod("waitForInitialization");
            newHandler.setAccessible(true);
            newHandler.invoke(amplitudeLogThread);
            newHandler.setAccessible(false);
            Field handlerField = amplitudeLogThread.getClass().getDeclaredField("handler");
            handlerField.setAccessible(true);

            amplitudeHandler = (Handler) handlerField.get(amplitudeLogThread);
            handlerField.setAccessible(false);

        } catch (Throwable e) {
            //something went wrong, turn off amplitude integration
            preferences.unregisterOnSharedPreferenceChangeListener(listener);
        }
    }

    public void getValuesAndUpload() {
        try {
            //get the database helper
            getAmplitudeDBHelper.setAccessible(true);
            Object dbHelper = getAmplitudeDBHelper.invoke(amplitudeDBHelperClass, TLManager.getInstance().getAppContext());
            getAmplitudeDBHelper.setAccessible(false);
            //due to some async issues we don't know how many events we have missed, so grab events up to the latest id we know about and record that id
            getAmplitudeEvents = dbHelper.getClass().getDeclaredMethod("getEvents", long.class, long.class);
            getAmplitudeEvents.setAccessible(true);

            //the id is irrelevant as I just want the most recent event
            //get thirty because there will as most be thirty in the db
            LinkedList<JSONObject> events = (LinkedList<JSONObject>) getAmplitudeEvents.invoke(dbHelper, Long.MAX_VALUE, 30l);
            getAmplitudeEvents.setAccessible(false);
            //send the event
            for (JSONObject value : events) {
                if (value.optLong("event_id") > greatestAmplitudeId) {
                    greatestAmplitudeId = value.optLong("event_id");
                    TLManager.getInstance().getTlAnalytics().trackSourceEvent(TLExternalAnalyticsManager.TLAnalyticsSourceAmplitude, value.optString("event_type"), null, value.optJSONObject("event_properties"));
                    TLLog.debug("logged amplitude event");

                    if (value.optLong("event_id") > greatestAmplitudeId) {
                        greatestAmplitudeId = value.optLong(("event_id"));
                    }
                }
            }
        } catch (Throwable e) {
            //do nothing
            TLLog.error("error while getting amplitude event: " + e.getMessage());
        }
    }

}
