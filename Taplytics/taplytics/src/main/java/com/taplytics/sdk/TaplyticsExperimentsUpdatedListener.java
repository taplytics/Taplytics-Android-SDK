/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk;

/**
 * DEBUGGING ONLY.
 *
 * A listener that is triggered every time Taplytics has received information from the server.
 *
 * This can be handy if sockets are entirely disabled but you still wish to force your own
 * changes for testing.
 */
public interface TaplyticsExperimentsUpdatedListener {

    void onExperimentUpdate();

}
