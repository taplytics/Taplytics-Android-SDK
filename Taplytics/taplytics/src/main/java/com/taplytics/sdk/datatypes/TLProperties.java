/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.datatypes;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.taplytics.sdk.analytics.TLAnalyticsManager;
import com.taplytics.sdk.utils.ImageUtils;
import com.taplytics.sdk.utils.TLListItemOrganizerAsyncTask;
import com.taplytics.sdk.utils.TLLog;
import com.taplytics.sdk.utils.TLUtils;
import com.taplytics.sdk.utils.ViewUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class TLProperties {
    private String sessionID;
    private String appName;
    private String deepLink;
    private TLJSONObject project;
    private TLJSONObject analyticsSettings;
    private TLJSONObject dynamicVars;
    private JSONObject images;
    private JSONArray experiments;
    private JSONArray experimentIDs;
    private JSONArray variationIDs;
    private JSONObject featureFlags;
    private JSONObject appUser;
    private JSONArray views;
    private JSONObject activities;
    private JSONArray geofences;
    private JSONArray oldGeofences;
    private HashMap<String, HashSet<Integer>> knownRecyclerPositions = null;
    private HashMap<String, HashSet<Integer>> knownListPositions = null;

    /**
     * This is the previous session id. This is used when the experiment load times out, as
     * the session used will be the loaded from disk.
     */
    private String lastSessionId = null;

    public String getLastSessionId() {
        return lastSessionId;
    }

    public void setLastSessionId(String lastSessionId) {
        this.lastSessionId = lastSessionId;
    }


    private double eventTime = TLAnalyticsManager.EVENT_DELAY_FLAG;
    private JSONObject clientControl;
    private JSONObject disabledEvents;
    private JSONObject pushToAnalyticsSources;

    private HashMap<String, String> activityMap = new HashMap<>();

    public HashMap<String, String> getActivityMap() {
        return activityMap;
    }

    public JSONObject getActivities() {
        return activities;
    }

    public void setSessionID(String sessionID) {
        this.sessionID = sessionID;
    }

    public String getSessionID() {
        return this.sessionID;
    }

    public String getAppName() {
        return this.appName;
    }

    public TLJSONObject getProject() {
        return this.project;
    }

    public TLJSONObject getAnalyticsSettings() {
        return this.analyticsSettings;
    }

    public JSONArray getExperiments() {
        return this.experiments;
    }

    public JSONArray getExperimentIDs() {
        return this.experimentIDs;
    }

    public JSONObject getFeatureFlags() {
        return this.featureFlags;
    }

    public TLJSONObject getDynamicVars() {
        return dynamicVars;
    }

    public JSONArray getVariationIDs() {
        return this.variationIDs;
    }

    public JSONObject getAppUser() {
        return this.appUser;
    }

    public JSONArray getViews() {
        return this.views;
    }

    public JSONObject getImages() {
        return this.images;
    }

    public String getDeepLink() {
        return deepLink;
    }

    public JSONArray getGeofences() {
        return this.geofences;
    }

    public JSONArray getOldGeofences() {
        return this.oldGeofences;
    }

    public void setViews(JSONArray views) {
        this.views = views;
    }

    public void setAppUser(JSONObject updatedAppUser) {
        this.appUser = updatedAppUser;
    }

    public void setGeofences(JSONArray updatedFences) {
        this.geofences = updatedFences;
    }

    public void setOldGeofences(JSONArray oldfences) {
        this.oldGeofences = oldfences;
    }

    public double getEventTime() {
        return eventTime;
    }

    public JSONObject getClientControl() {
        return clientControl;
    }

    public JSONObject getDisabledEvents() {
        return disabledEvents;
    }

    public JSONObject getPushToAnalyticsSources() {
        return pushToAnalyticsSources;
    }

    public HashSet<Integer> getKnownRecyclerPositions(String identifier) {
        HashSet<Integer> positions = new HashSet<>();

        try {
            HashSet<Integer> knownPositions = knownRecyclerPositions.get(identifier);
            HashSet<Integer> unknownPositions = knownRecyclerPositions.get("unknown");

            if (knownPositions != null) {
                positions.addAll(knownPositions);
            }
            if (unknownPositions != null) {
                positions.addAll(unknownPositions);
            }
        } catch (Throwable e) {
            //
        }
        return positions;
    }

    /**
     * Update all currently active known recycler positions so we know to clear/update them in debug mode.
     *
     * @param knownRecyclerPositions
     */
    public void setKnownRecyclerPositions(HashMap<String, HashSet<Integer>> knownRecyclerPositions) {
        this.knownRecyclerPositions = knownRecyclerPositions;
        ViewUtils.resetListItemPositionsIfNecessary(knownRecyclerPositions);
    }

    public HashSet<Integer> getKnownListPositions(String identifier) {
        HashSet<Integer> positions = new HashSet<>();
        try {
            HashSet<Integer> knownPositions = knownListPositions.get(identifier);
            HashSet<Integer> unknownPositions = knownListPositions.get("unknown");
            if (knownPositions != null) {
                positions.addAll(knownPositions);
            }
            if (unknownPositions != null) {
                positions.addAll(unknownPositions);
            }
        } catch (Throwable e) {
            //
        }
        return positions;
    }

    public void setKnownListPositions(HashMap<String, HashSet<Integer>> knownListPositions) {
        this.knownListPositions = knownListPositions;
    }

    public TLProperties(JSONObject jsonObj) throws Throwable {
        if (jsonObj != null) {
            Object projectDic = jsonObj.opt("projectInfo");
            if (projectDic != null && projectDic instanceof JSONObject) {
                this.project = new TLJSONObject((JSONObject) projectDic);

                Object projectName = project.opt("name");
                if (projectName != null && projectName instanceof String)
                    this.appName = (String) projectName;

                Object deepLink = project.opt("deviceToken");
                if (deepLink != null && deepLink instanceof String) {
                    this.deepLink = (String) deepLink;
                }
            }

            Object sessID = jsonObj.opt("sid");
            if (sessID != null && sessID instanceof String)
                this.sessionID = (String) sessID;

            Object analyticSet = jsonObj.opt("as");
            if (analyticSet != null && analyticSet instanceof JSONObject) {
                this.analyticsSettings = new TLJSONObject((JSONObject) analyticSet);
            }

            Object exps = jsonObj.opt("experiments");
            if (exps != null && exps instanceof JSONArray)
                this.experiments = (JSONArray) exps;

            Object expIDs = jsonObj.opt("exp");
            if (expIDs != null && expIDs instanceof JSONArray)
                this.experimentIDs = (JSONArray) expIDs;

            this.featureFlags = jsonObj.optJSONObject("ff");

            Object varIDs = jsonObj.opt("var");
            if (varIDs != null && varIDs instanceof JSONArray)
                this.variationIDs = (JSONArray) varIDs;

            Object dynamicVars = jsonObj.opt("dynamicVars");
            if (dynamicVars != null && dynamicVars instanceof JSONObject)
                this.dynamicVars = new TLJSONObject((JSONObject) dynamicVars);

            Object appUser = jsonObj.opt("au");
            if (appUser != null && appUser instanceof JSONObject)
                this.appUser = (JSONObject) appUser;

            Object views = jsonObj.opt("views");
            if (views != null && views instanceof JSONArray) {
                this.views = (JSONArray) views;
                try {
                    TLUtils.executeAsyncTask(new TLListItemOrganizerAsyncTask(), views);
                } catch (Throwable e) {
                    TLLog.error("error launching organizer task", e);
                }
            }

            Object images = jsonObj.opt("images");
            if (images != null && images instanceof JSONObject) {
                this.images = (JSONObject) images;
                TLUtils.executeAsyncTask(new ImageUtils.SaveImageToDiskTask(), this.images);
            }

            Object geofences = jsonObj.opt("regions");
            if (geofences != null && geofences instanceof JSONArray)
                this.geofences = (JSONArray) geofences;

            Object eventDelay = jsonObj.opt("eventDelay");
            if (eventDelay != null && eventDelay instanceof Double)
                this.eventTime = (double) eventDelay;

            Object pas = jsonObj.opt("pas");
            if (pas != null && pas instanceof JSONObject) {
                this.pushToAnalyticsSources = (JSONObject) pas;
            }

            Object clientControl = jsonObj.opt("clientControl");
            if (clientControl != null && clientControl instanceof JSONObject) {
                this.clientControl = (JSONObject) clientControl;
                this.disabledEvents = this.clientControl.optJSONObject("disabledEvents");
            }

            Object activities = jsonObj.opt("activities");
            if (activities != null && activities instanceof JSONObject) {
                // TODO: Send this differently? 2nd TODO: Figure out what I meant by this.
                this.activities = (JSONObject) activities;
                Iterator<?> keys = this.activities.keys();

                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    if (!activityMap.containsKey(key) && this.activities.opt(key) != null && (this.activities.opt(key) instanceof JSONObject)) {
                        JSONObject activity = (JSONObject) this.activities.get(key);
                        String id = activity.optString("_id");
                        if (id != null) {
                            activityMap.put(id, key);
                        }
                    }
                }
            }
        }
    }

    /**
     * @param withStatus whether or not we want to append the status (active / draft) to the names
     * @return A list of experiment names associated to this OS.
     */
    public String[] getExperimentNames(boolean withStatus) {
        String[] experimentsArray;
        ArrayList<String> experiments = new ArrayList<>();
        try {
            JSONObject exp;
            for (int i = 0; i < this.experiments.length(); i++) {
                exp = this.experiments.getJSONObject(i);
                //We have to iterate through the distribution filters here so we only show experiments associated with Android.
                if (exp.has("distFilters") && exp.optJSONArray("distFilters") != null) {
                    JSONArray distFilters = exp.optJSONArray("distFilters");
                    if (distFilters.length() == 0) {
                        experiments.add(withStatus ? exp.optString("name") + " (" + exp.optString("status") + ")" : exp.optString("name"));
                    }
                    for (int j = 0; j < distFilters.length(); j++) {
                        JSONObject filter = distFilters.optJSONObject(j);
                        //We have to grab the filter associated with the osType.
                        if (filter != null && filter.has("type") && (filter.get("type") instanceof String) && filter.get("type").equals("osType")) {
                            if (filter.has("values") && filter.optJSONArray("values") != null) {
                                JSONArray values = filter.optJSONArray("values");
                                //Then just iterate through all the values in this filter until we find android.
                                for (int k = 0; k < values.length(); k++) {
                                    if (values.get(k) instanceof String && values.get(k).equals("Android")) {
                                        //Add the status if we have withStatus.
                                        experiments.add(withStatus ? exp.optString("name") + " (" + exp.optString("status") + ")" : exp.optString("name"));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            //Convert the ArrayList to String Array.
            experimentsArray = experiments.toArray(new String[experiments.size()]);
        } catch (JSONException e) {
            return null;
        }
        return experimentsArray;
    }

    public String[] getVariationNames(String name) {
        String[] vars;
        try {
            JSONArray variations = null;
            for (int i = 0; i < experiments.length(); i++) {
                if (experiments.getJSONObject(i).getString("name").equals(name)) {
                    variations = experiments.getJSONObject(i).optJSONArray("variations");
                }
            }
            if (variations == null) {
                return null;
            }
            vars = new String[variations.length() + 1];

            for (int i = 0; i < variations.length(); i++) {
                vars[i] = variations.getJSONObject(i).optString("name");
            }
            vars[variations.length()] = "baseline";
        } catch (JSONException e) {
            return null;
        }
        return vars;
    }

    public String[] getFeatureFlagNames() {
        String[] featureFlagArray;
        ArrayList<String> flags = new ArrayList<>();
        JSONObject flagsObject = this.featureFlags;
        try {
            Iterator<?> keys = this.featureFlags.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                JSONObject flagObject = flagsObject.getJSONObject(key);
                String name = flagObject.getString("name");
                String status = flagObject.getString("status");
                if (flagsObject.get(key) instanceof JSONObject) {
                    flags.add(name + " (" + status + ")");
                }
            }
            //Convert the ArrayList to String Array.
            featureFlagArray = flags.toArray(new String[flags.size()]);
        } catch (Throwable e) {
            return null;
        }
        return featureFlagArray;
    }

    @Nullable
    public JSONObject getExperimentByName(@NonNull String name) {
        JSONObject experiment = null;
        try {
            for (int i = 0; i < experiments.length(); i++) {
                experiment = experiments.getJSONObject(i);
                if (experiment.optString("name").equals(name))
                    return experiment;
            }
        } catch (Throwable e) {
            return null;
        }
        return experiment;
    }

    @Nullable
    public JSONObject getVariationByName(@NonNull JSONObject experiment, @NonNull String name) {
        JSONObject variation = null;
        if (name.equals("baseline")) {
            return experiment.optJSONObject("baseline");
        } else {
            final JSONArray variations = experiment.optJSONArray("variations");
            if (variations == null) {
                return null;
            }

            try {
                for (int i = 0; i < variations.length(); i++) {
                    variation = variations.getJSONObject(i);
                    if (variation.optString("name").equals(name)) {
                        return variation;
                    }
                }
            } catch (JSONException e) {
                return null;
            }
            return variation;
        }
    }

    public JSONObject getFeatureFlagByName(JSONObject featureFlags, String name) {
        JSONObject featureFlag = null;
        Iterator<String> allKeys = featureFlags.keys();
        try {
            while (allKeys.hasNext()) {
                String key = (String) allKeys.next();
                if (featureFlags.get(key) instanceof JSONObject) {
                    JSONObject flag = featureFlags.getJSONObject(key);
                    if (flag.getString("name").equals(name)) {
                        featureFlag = flag;
                    }
                }
            }
        } catch (Throwable e) {
            return null;
        }
        return featureFlag;
    }

}
