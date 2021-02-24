/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.analytics.external;

import com.taplytics.sdk.Taplytics;
import com.taplytics.sdk.managers.TLManager;
import com.taplytics.sdk.utils.TLLog;

import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * Handles Mixpanel events
 */
public class MixpanelManager extends AbsExternalAnalyticsManager {

    /**
     * Singleton object representing the class
     */
    private static MixpanelManager instance;

    /**
     * Contains the known default user attribute keys that Mixpanel uses that we should ignore
     */
    private final HashSet<String> keysToIgnore = new HashSet<>(Arrays.asList("$android_os",
            "$android_model", "$android_brand", "$android_os_version", "$android_lib_version",
            "$android_manufacturer", "$android_app_version", "$token", "$distinct_id"));

    /**
     * Mixpanel token
     */
    private String token;

    /**
     * Mixpanel API reference
     */
    private Object mixpanelAPI;

    /**
     * Enum based on the two table used by Mixpanel
     */
    public enum Table {
        EVENTS("events"), PEOPLE("people");

        private final String mTableName;

        Table(String name) {
            mTableName = name;
        }

        public String getName() {
            return mTableName.toUpperCase();
        }
    }

    /**
     * Creates a {@link MixpanelManager} object
     */
    private MixpanelManager() {
        setupExternalManager("mixpanel", 4, this);
        fetchMixpanelToken();
    }

    /**
     * Retrieves an instance of the class.
     *
     * @return {@link #instance}
     */
    public static MixpanelManager getInstance() {
        if (instance == null) {
            instance = new MixpanelManager();
        }
        return instance;
    }

    /**
     * Populates {@link #token} with the data from the Mixpanel API
     */
    private void fetchMixpanelToken() {
        try {
            if (!appHasSource()) {
                return;
            }

            final Class<?> api = Class.forName("com.mixpanel.android.mpmetrics.MixpanelAPI");

            final Field f = api.getDeclaredField("sInstanceMap");
            f.setAccessible(true);

            final Map<?, ?> m = (Map<?, ?>) f.get(api);
            if (m.size() <= 0) {
                return;
            }

            for (Object key : m.keySet()) {
                token = (String) key;
                if (token.equals("taplytics")) {
                    continue;
                }

                final Map<?, ?> map = (Map<?, ?>) m.get(key);
                mixpanelAPI = map.get(TLManager.getInstance().getAppContext());
                break;
            }
        } catch (Exception e) {
            TLLog.error("error setting up mixpanel");
        }
    }

    @Override
    public boolean appHasSource() {
        try {
            Class.forName("com.mixpanel.android.mpmetrics.MixpanelAPI");
            return true;
        } catch (Exception e) {
            // No Mixpanel
            return false;
        }
    }

    boolean retry = true;

    @Override
    public void flush() {
        if (token != null && mixpanelAPI != null && getCursorCount(Table.EVENTS.getName()) > 0) {
            try {
                mixpanelAPI.getClass().getMethod("flush").invoke(mixpanelAPI);
            } catch (Exception e) {
                TLLog.error("flush error", e);
            }
        } else if (token == null && retry) {
            fetchMixpanelToken();
            retry = false;
        }
    }

    @Override
    public void trackToTaplyticsAndFlush() {
        for (String event : getTableResults(Table.EVENTS.getName(), "data")) {
            try {
                JSONObject eventJson = new JSONObject(event);
                if (eventJson != JSONObject.NULL) {
                    trackTaplyticsEventFromData(eventJson);
                }
            } catch (Exception e) {
                TLLog.debug("MPErr: " + e.getMessage());
            }
        }

        if (getCursorCount(Table.PEOPLE.getName()) > 0) {
            final ArrayList<JSONObject> objects = new ArrayList<>();
            for (String attribute : getTableResults(Table.PEOPLE.getName(), "data")) {
                try {
                    objects.add(new JSONObject(attribute));
                } catch (Exception e) {
                    TLLog.debug("MPErr" + e.getMessage());
                }
            }
            trackTaplyticsUserAttributesFromMixpanel(objects);
        }
        flush();
    }

    /**
     * Grab whatever user attributes mixpanel has and convert them into Taplytics ones. *
     */
    public void trackTaplyticsUserAttributesFromMixpanel(ArrayList<JSONObject> attributes) {
        JSONObject customData = new JSONObject();
        JSONObject taplyticsAttributes = new JSONObject();
        for (JSONObject attribute : attributes) {
            try {
                // $set is what mixpanel uses to add stuff.
                if (attribute.has("$set")) {
                    JSONObject data = attribute.optJSONObject("$set");
                    Iterator<?> keys = data.keys();
                    while (keys.hasNext()) {
                        String key = (String) keys.next();
                        if (!keysToIgnore.contains(key)) {
                            customData.put(key, data.get(key));
                        }
                    }
                    // $add is what they use for incrementing stuff like revenue.
                } else if (attribute.has("$add")) {
                    JSONObject addData = attribute.optJSONObject("$add");
                    Iterator<?> keys = addData.keys();
                    while (keys.hasNext()) {
                        String key = (String) keys.next();
                        customData.put(key, addData.get(key));
                    }
                }
                taplyticsAttributes.put("customData", customData);
            } catch (Exception e) {
                TLLog.error("MPUA", e);
            }
        }
        Taplytics.setUserAttributes(taplyticsAttributes);
    }

    /**
     * Convert a JSONObject from Mixpanel data to a Taplytics event *
     */
    public void trackTaplyticsEventFromData(JSONObject object) {
        try {
            if (object.equals(JSONObject.NULL) ||
                    object.length() == 0 ||
                    !object.has("event")) {
                return;
            }

            final JSONObject taplyticsMetaData = new JSONObject();
            final String goalName = object.optString("event");
            final JSONObject data = object.optJSONObject("properties");
            if (data == null || data.equals(JSONObject.NULL) || data.length() == 0) {
                TLManager.getInstance()
                        .getTlAnalytics()
                        .trackSourceEvent(TLExternalAnalyticsManager.TLAnalyticsSourceMixPanel, goalName);
                return;
            }
            Iterator<?> keys = data.keys();
            // iterate through all of the data keys and find the custom ones and the ones we want. Filter out all of the other
            // stuff.
            while (keys.hasNext()) {
                String key = (String) keys.next();
                // Change the time key to be a TLTime key so we don't conflict with other stuff. Later on we use TLTime to set the
                // event time. If another event for SOME reason sets a "time" metadata key, we don't want to interfere.
                if (!key.startsWith("$") &&
                        !key.equals("distinct_id") &&
                        !key.equals("token") &&
                        !key.equals("mp_lib") &&
                        !key.equals("time")) {
                    taplyticsMetaData.put(key, data.get(key));
                } else if (key.equals("time") || key.equals("$time")) {
                    taplyticsMetaData.put("TLTime", data.get(key));
                }
            }
            TLManager.getInstance()
                    .getTlAnalytics()
                    .trackSourceEvent(TLExternalAnalyticsManager.TLAnalyticsSourceMixPanel, goalName, null, taplyticsMetaData);
        } catch (Exception e) {
            TLLog.error("Json error", e);
        }
    }

}
