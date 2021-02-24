/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.managers;

import com.taplytics.sdk.TaplyticsRunningFeatureFlagsListener;
import com.taplytics.sdk.datatypes.TLProperties;
import com.taplytics.sdk.utils.TLLog;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TLFeatureFlagManager {

    private static TLFeatureFlagManager instance = null;

    private ArrayList<TaplyticsRunningFeatureFlagsListener> pendingRunningFlagListeners;

    public static TLFeatureFlagManager getInstance() {
        if (instance == null) {
            instance = new TLFeatureFlagManager();
        }
        return instance;
    }

    public boolean featureFlagEnabled(final String key, boolean defaultValue) {
        try {
            TLProperties tlProperties = TLManager.getInstance().getTlProperties();
            if (tlProperties != null && tlProperties.getFeatureFlags() != null) {
                JSONObject featureFlags = tlProperties.getFeatureFlags();
                JSONObject featureFlag = featureFlags.optJSONObject(key);
                if (featureFlag != null) {
                    TLLog.debug("Got feature flag " + key, true);
                    return featureFlag.opt("enabled") != null && featureFlag.optBoolean("enabled");
                }
                else {
                    TLLog.warning("Unable to load feature flag with key " + key + ". Flag not present in config", true);
                }
            }
        } catch (Throwable e) {
            TLLog.error("Getting running feature flags", e);
        }

        return defaultValue;
    }

    public void performPendingRunningFlagListeners() {
        if (pendingRunningFlagListeners != null) {
            try {
                ArrayList<TaplyticsRunningFeatureFlagsListener> listeners = (ArrayList) pendingRunningFlagListeners.clone();
                pendingRunningFlagListeners = null;
                for (TaplyticsRunningFeatureFlagsListener listener : listeners) {
                    sendRunningFeatureFlags(listener);
                }
            } catch (Throwable e) {
                TLLog.error("Error running listeners", e);
            }
        }

    }

    public void getRunningFeatureFlags(final TaplyticsRunningFeatureFlagsListener listener) {
        TLManager tlManager = TLManager.getInstance();
        if (!tlManager.getTrackingEnabled()) {
            listener.runningFeatureFlags(null);
            return;
        }

        if (tlManager.hasLoadedPropertiesFromServer() || tlManager.isExperimentLoadTimeout()) {
            sendRunningFeatureFlags(listener);
        } else {
            if (pendingRunningFlagListeners == null) {
                this.pendingRunningFlagListeners = new ArrayList<>();
            }
            pendingRunningFlagListeners.add(listener);
        }
    }

    public void sendRunningFeatureFlags(final  TaplyticsRunningFeatureFlagsListener listener) {
        TLProperties tlProperties = TLManager.getInstance().getTlProperties();
        Map<String, String> flagMap = new HashMap<>();
        if (tlProperties != null && tlProperties.getFeatureFlags() != null) {
            JSONObject featureFlags = tlProperties.getFeatureFlags();
            JSONObject projectSettings = tlProperties.getProject() != null ? tlProperties.getProject().optJSONObject("settings") : null;
            Boolean showDraftExperiments = projectSettings != null && projectSettings.optBoolean("showDraftExperiments");
            try {
                Iterator<String> keys = featureFlags.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    if (featureFlags.opt(key) instanceof JSONObject) {
                        JSONObject featureFlag = featureFlags.optJSONObject(key);
                        String status = featureFlag.optString("status");
                        boolean enabled = featureFlag.optBoolean("enabled");
                        // If the showDraftExperiments project setting is enabled return draft flags as well.
                        if (((showDraftExperiments && status.equals("draft")) || (status.equals("active"))) && enabled) {
                            String flagName = featureFlag.optString("name");
                            flagMap.put(flagName, key);
                        }
                    }
                }
            } catch (Throwable e) {
                TLLog.error("Getting running feature flags", e);
            } finally {
                listener.runningFeatureFlags(flagMap);
            }
        }
        else {
            listener.runningFeatureFlags(flagMap);
        }
    }

}