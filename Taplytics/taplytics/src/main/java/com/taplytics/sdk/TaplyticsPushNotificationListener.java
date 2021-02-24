/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * <p>
 * Interface for intercepting custom data from a Taplytics push notification, {@link #pushReceived(org.json.JSONObject)}
 * is called and passed the custom data upon the notification
 * </p>
 */
public interface TaplyticsPushNotificationListener {
    /**
     * Called when the push from Taplytics is received by the device
     * @param customData
     *            the custom data set by you from the Taplytics push interface or API     *
     * @throws JSONException
     */
    void pushReceived(JSONObject customData) throws JSONException;
}
