/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk;

/**
 * Interface for receiving GCM tokens. If there is a need to user the GCM token in another place,
 * this callback is triggered when we receive the GCM push token from the server.
 */
public interface TaplyticsPushTokenListener {

    void pushTokenReceived(String token);
}
