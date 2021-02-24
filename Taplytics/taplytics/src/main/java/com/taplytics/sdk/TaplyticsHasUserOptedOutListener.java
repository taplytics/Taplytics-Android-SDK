/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk;

/**
 * Interface for receiving if the user has opted out of tracking.
 */
public interface TaplyticsHasUserOptedOutListener {

    void hasUserOptedOutTracking(boolean hasOptedOut);
}