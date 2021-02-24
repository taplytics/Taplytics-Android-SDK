/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */
package com.taplytics.sdk.fcm;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.taplytics.sdk.managers.TLManager;
import com.taplytics.sdk.utils.TLLog;

public class TLFirebaseMessagingServiceLite extends FirebaseMessagingService {

        @Override
        public void onMessageReceived(RemoteMessage message) {
            super.onMessageReceived(message);
            TLLog.debug("FCM Message received");
            TLManager.getInstance().getTlPushManager().processReceivedMessage(message, this);
        }
}
