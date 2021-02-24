/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk;

/**
 * A listener which is triggered whenever a new session has began.
 */
public interface TaplyticsNewSessionListener {

    void onNewSession();
    void onError();

}
