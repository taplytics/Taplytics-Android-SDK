/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk;

import android.content.Intent;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * <p>
 * Interface for setting the intent that is launched for a push notification, it is passed custom data {@link #setPushNotificationIntent(org.json.JSONObject)}
 * </p>
 */
public interface TaplyticsPushNotificationIntentListener {
    /**
     *
     * @param customData the custom data passed from the push notification
     * @return an Intent that will be set as the launch intent for all push notifications
     * @throws JSONException
     */
    Intent setPushNotificationIntent(JSONObject customData) throws JSONException;
}
