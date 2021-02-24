/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */
package com.taplytics.sdk.fcm;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.taplytics.sdk.managers.TLManager;
import com.taplytics.sdk.utils.Functionality;
import com.taplytics.sdk.utils.TLLog;
import com.taplytics.sdk.utils.TLUtils;

public class TLFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        if(TLUtils.isDisabled(Functionality.PUSH)){
            return;
        }
        TLLog.debug("Got new FCM Push Token: " + s);
        TLManager.getInstance().getTlPushManager().savePushtoken(s, false);
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);

        TLLog.debug("FCM Message received");

        try {
            TLManager.getInstance().getTlPushManager().processReceivedMessage(message, this);
        } catch (Throwable T) {
            TLLog.error("Message received error", T);
        }

    }

}