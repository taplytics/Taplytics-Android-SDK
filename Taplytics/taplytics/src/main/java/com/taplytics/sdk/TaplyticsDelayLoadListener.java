/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk;

/**
 * A listener that provides callbacks with the use of {@link com.taplytics.sdk.Taplytics#delayLoad}
 * <p/>
 * {@link #startDelay()} is called immediately. Put your delay code in this callback (splash image, loading screen, etc).
 * <p/>
 * {@link #delayComplete()} is called either when Taplytics has either completed loading visual changes, or the given max time has been reached)
 */
public interface TaplyticsDelayLoadListener {
    void startDelay();

    void delayComplete();
}
