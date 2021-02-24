/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk;


/**
 * A listener that provides callbacks with the use of {@link com.taplytics.sdk.Taplytics#startTaplytics}
 * <p/>
 * {@link #loaded()}  is called when taplytics has successfully received experiment and variation information.
 */
public interface TaplyticsExperimentsLoadedListener {

    void loaded();
}
