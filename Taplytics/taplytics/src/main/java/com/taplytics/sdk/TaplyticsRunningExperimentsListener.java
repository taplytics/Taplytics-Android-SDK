/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk;

import java.util.Map;

/**
 * <p>
 * Interface for a getting running experiments and variations, the {@link #runningExperimentsAndVariation(java.util.Map)}
 * method may return asynchronously.
 * </p>
 */
public interface TaplyticsRunningExperimentsListener {

    /**
     * Will return the running experiments and variations. May return asynchronously, as it
     * waits for updated properties to load from Taplytics' servers before returning the running experiments.
     *
     * @param experimentsAndVariations Map with key of the experiment name, and value of the running variation's name (or baseline).
     *                                 Can be null.
     */
    void runningExperimentsAndVariation(Map<String, String> experimentsAndVariations);
}
