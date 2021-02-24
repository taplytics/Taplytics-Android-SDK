/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk;

import java.util.Map;

public interface TaplyticsRunningFeatureFlagsListener {
    /**
     * Will return the running feature flags. May return asynchronously, as it
     * waits for updated properties to load from Taplytics' servers before returning the running flags.
     *
     * @param featureFlags Map with key being the feature flag's name, and value being the running feature flag's key.
     *                                 Can be null.
     */
    void runningFeatureFlags(Map<String, String> featureFlags);
}
