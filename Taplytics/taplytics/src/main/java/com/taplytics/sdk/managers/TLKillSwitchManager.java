/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.managers;

import android.content.Context;
import android.content.SharedPreferences;

import com.taplytics.sdk.TLActivityLifecycleCallbacks;
import com.taplytics.sdk.analytics.TLAnalyticsManager;
import com.taplytics.sdk.analytics.external.TLExternalAnalyticsManager;
import com.taplytics.sdk.datatypes.TLProperties;
import com.taplytics.sdk.network.TLSocketManager;
import com.taplytics.sdk.utils.Functionality;
import com.taplytics.sdk.utils.TLLog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;

/**
 * Created by VicV on 6/9/15.
 * Options:
 * View tracking: "view".
 * manual event logging: "events"
 * All analytics: "analytics"
 * Extenrnal Analytics: "external"
 * Google analytics: "google"
 * Mixpanel: "mixpanel"
 * Flurry: "flurry"
 * Localytics: "localytics"
 * Listviews: "listviews"
 * "recyclerviews: "recyclerviews"
 * all visual editing: "visual"
 * error tracking: "errors"
 * sockets: "sockets"
 * viewpagers: "viewpagers"
 * support fragments: "support fragments"
 * fragments: "fragments"
 * push notifications: "push"
 * code experiments: "code"
 * button presses: "buttons"
 * Shake menu: "shakemenu
 * geofences: "geofences"
 */
public class TLKillSwitchManager {

    private static String OPT_OUT_KEY = "TL_OPT_OUT";
    private static String PREFERENCE_KEY = "TAPLYTICS_PREFS_OPT_OUT";

    public boolean hasOptedOut = false;

    private static SharedPreferences sharedPreferences;

    /**
     * Instance of {@link TLKillSwitchManager}
     **/
    private static TLKillSwitchManager instance;

    /**
     * @return {@link #instance}
     */
    public static TLKillSwitchManager getInstance() {
        if (instance != null) {
            return instance;
        } else {
            instance = new TLKillSwitchManager();
        }
        return instance;
    }

    /**
     * Sets the current disabled functions AFTER adding inclusive additions.
     *
     * @param disabledFunctions A HashSet of disabled functions by String.
     */
    public void setDisabledFunctionsInclusive(HashSet<String> disabledFunctions) {
        this.disabledFunctions = addInclusiveKillSwitches(disabledFunctions);
    }

    /**
     * Sets the current disabled functions AS IS.
     *
     * @param disabledFunctions A HashSet of disabled functions by String.
     */
    public void setDisabledFunctions(HashSet<String> disabledFunctions) {
        this.disabledFunctions = disabledFunctions;
    }

    /**
     * The set of disabled functionality chosen.
     */
    private HashSet<String> disabledFunctions = new HashSet<>();

    /**
     * @return {@link #disabledFunctions}
     */
    public HashSet getDisabledFunctions() {
        return disabledFunctions;
    }


    /**
     * Some killswitches cover a bunch of things. If analytics are disabled, that encompasses a few things.
     * To avoid doing two checks in one place, we just include all the inclusive killswitches.
     *
     * @param disabled HashSet of disabled sources
     * @return HashSet of disabled sources with inclusive switches.
     */
    private HashSet addInclusiveKillSwitches(HashSet<String> disabled) {
        if (disabled.contains(Functionality.ANALYTICS.getText())) {
            disabled.add(Functionality.EVENTS.getText());
            disabled.add(Functionality.VIEWTRACKING.getText());
            disabled.add(Functionality.BUTTONS.getText());
            disabled.add(Functionality.EXTERNAL.getText());
        }
        if (disabled.contains(Functionality.EXTERNAL.getText())) {
            disabled.add(Functionality.GOOGLE.getText());
            disabled.add(Functionality.FLURRY.getText());
            disabled.add(Functionality.MIXPANEL.getText());
            disabled.add(Functionality.LOCALYTICS.getText());
        }
        if (disabled.contains(Functionality.VISUAL.getText())) {
            disabled.add(Functionality.RECYCLERVIEWS.getText());
            disabled.add(Functionality.LISTVIEWS.getText());
            disabled.add(Functionality.FRAGMENTS.getText());
            disabled.add(Functionality.SUPPORTFRAGMENTS.getText());
            disabled.add(Functionality.VIEWPAGERS.getText());
        }
        return disabled;
    }


    /**
     * Same as {@link com.taplytics.sdk.managers.TLKillSwitchManager#addInclusiveKillSwitches(java.util.HashSet)}
     * but backwards. If someone enables ALL analytic, then enable them all.
     *
     * @param enabled
     */
    private void removeInclusiveKillswitches(HashSet<String> enabled) {
        if (enabled.contains(Functionality.ANALYTICS.getText())) {
            disabledFunctions.remove(Functionality.EVENTS.getText());
            disabledFunctions.remove(Functionality.VIEWTRACKING.getText());
            disabledFunctions.remove(Functionality.BUTTONS.getText());
            disabledFunctions.remove(Functionality.EXTERNAL.getText());
        }
        if (enabled.contains(Functionality.EXTERNAL.getText())) {
            disabledFunctions.remove(Functionality.GOOGLE.getText());
            disabledFunctions.remove(Functionality.FLURRY.getText());
            disabledFunctions.remove(Functionality.MIXPANEL.getText());
            disabledFunctions.remove(Functionality.LOCALYTICS.getText());
        }
        if (enabled.contains(Functionality.VISUAL.getText())) {
            disabledFunctions.remove(Functionality.RECYCLERVIEWS.getText());
            disabledFunctions.remove(Functionality.LISTVIEWS.getText());
            disabledFunctions.remove(Functionality.FRAGMENTS.getText());
            disabledFunctions.remove(Functionality.SUPPORTFRAGMENTS.getText());
            disabledFunctions.remove(Functionality.VIEWPAGERS.getText());
        }
    }

    public void disableFunction(Functionality function) {
        disabledFunctions.add(function.getText());
    }

    public void enableFunction(Functionality function) {
        disabledFunctions.remove(function);
    }

    /**
     * If we grabbed TLProperties, check for disabled (or enabled!) functionality)
     **/
    public void updateDisabledFunctionality(TLProperties tlProperties) {
        //Obvious check.
        if (tlProperties != null) {
            JSONObject project = tlProperties.getProject();
            try {
                if (project != null) {
                    //Grab our settings
                    JSONArray disabledFunctionality = project.optJSONObject("settings").optJSONArray("disable");
                    if (disabledFunctionality != null) {
                        //Grab our current set of disabled functions
                        //Iterate through all disabled functions given by the server
                        for (int i = 0; i < disabledFunctionality.length(); i++) {
                            if (disabledFunctionality.get(i) != null && disabledFunctionality.get(i) instanceof String) {
                                disabledFunctions.add((String) disabledFunctionality.get(i));
                            }
                        }

                        //Set this as the new set of disabled functions
                        setDisabledFunctionsInclusive(disabledFunctions);
                    }
                }

                //Same as above but now we REMOVE them.
                /*This exists because if someone releases a version of their app with the starting
                options disabling things, we should also have the option to re-enable them through the server.*/
                if (project != null) {
                    JSONArray enabledFunctionality = project.optJSONObject("settings").optJSONArray("enable");
                    if (enabledFunctionality != null) {
                        HashSet enabledFunctions = new HashSet();
                        for (int i = 0; i < enabledFunctionality.length(); i++) {
                            if (enabledFunctionality.get(i) != null && enabledFunctionality.get(i) instanceof String) {
                                disabledFunctions.remove(enabledFunctionality.get(i));
                                //Keep a HashSet of all the enabled functionality to use later.
                                enabledFunctions.add(enabledFunctionality.get(i));
                            }
                        }

                        //Just like the inclusive disables, make sure we enable inclusively.
                        removeInclusiveKillswitches(enabledFunctions);

                    }
                }
            } catch (Exception ex) {
                TLLog.error("error checking disabled sources", ex);
            }
        }
    }

    private SharedPreferences getLocalSharedPreferencesForContext(Context appContext) {
        if (sharedPreferences == null && appContext != null) {
            sharedPreferences = appContext.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE);
        }
        return sharedPreferences;
    }

    public void optInUserTracking(Context appContext) {
        Context context = appContext != null ? appContext : TLManager.getInstance().getAppContext();
        if (getLocalSharedPreferencesForContext(context) == null) {
            TLLog.error("Missing appContext to optInUserTracking");
            return;
        }

        hasOptedOut = false;
        if (sharedPreferences != null && sharedPreferences.contains(OPT_OUT_KEY)) {
            sharedPreferences.edit().remove(OPT_OUT_KEY).apply();
        }

        TLLog.debug("Opt IN User Tracking");
        if (TLManager.getInstance().getTlAnalytics() != null) {
            TLAnalyticsManager analyticsManager = TLManager.getInstance().getTlAnalytics();
            analyticsManager.trackTLEvent(TLAnalyticsManager.TLAnalyticsOptInTracking);
        }

        TLManager.getInstance().getPropertiesFromServer(null, null);
    }

    public void optOutUserTracking(Context appContext) {
        Context context = appContext != null ? appContext : TLManager.getInstance().getAppContext();
        if (getLocalSharedPreferencesForContext(context) == null) {
            TLLog.error("Missing appContext to optOutUserTracking");
            return;
        }

        if(TLManager.getInstance().getActivityCallbacks()!= null){
            TLActivityLifecycleCallbacks.unregisterCallbacks(TLManager.getInstance().getActivityCallbacks());
        }

        TLExternalAnalyticsManager.getInstance().unRegisterExternalAnalytics();

        TLLog.debug("Opt OUT User Tracking");
        if(TLManager.getInstance().getTlAnalytics() != null) {
            TLAnalyticsManager analyticsManager = TLManager.getInstance().getTlAnalytics();
            analyticsManager.trackTLEvent(TLAnalyticsManager.TLAnalyticsOptOutTracking);
            analyticsManager.flushEventsQueue(null);
            analyticsManager.getTlAppUser().flushAppUserAttributes(null);
        }
        
        if (TLManager.getInstance().isLiveUpdate()) {
            TLSocketManager.getInstance().disconnectSocketIO();
        }

        hasOptedOut = true;
        if (sharedPreferences != null && !sharedPreferences.contains(OPT_OUT_KEY)) {
            sharedPreferences.edit().putBoolean(OPT_OUT_KEY, true).apply();
        }

    }

    public boolean hasUserOptedOutTracking(Context appContext) {
        Context context = appContext != null ? appContext : TLManager.getInstance().getAppContext();
        if (getLocalSharedPreferencesForContext(context) == null) {
            TLLog.error("Missing appContext to hasUserOptedOutTracking");
            return false;
        }

        hasOptedOut = sharedPreferences != null && sharedPreferences.contains(OPT_OUT_KEY);
        return hasOptedOut;
    }
}
