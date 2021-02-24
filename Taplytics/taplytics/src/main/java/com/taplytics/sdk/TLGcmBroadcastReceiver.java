/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.taplytics.sdk.managers.TLManager;
import com.taplytics.sdk.managers.TLPushManager;
import com.taplytics.sdk.utils.TLUtils;

/**
 * Extend this class to make use of the pushOpened, pushDismissed, and pushReceived functions!
 * <p/>
 * See method docs for more information.
 */
public class TLGcmBroadcastReceiver extends BroadcastReceiver {

    public void onReceive(final Context context, final Intent intent) {
        if (intent != null) {
            TLManager.getInstance().executeRunnable(new Runnable() {
                @Override
                public void run() {
                    switch (intent.getAction()) {
                        case (TLPushManager.OPEN_ACTION):
                            pushOpened(context, intent);
                            TLPushManager.pushOpen(intent);
                            break;
                        case (TLPushManager.DISMISS_ACTION):
                            pushDismissed(context, intent);
                            TLPushManager.pushDismissed(intent);
                            break;
                        default:
                            pushReceived(context, intent);
                            TLPushManager.pushReceived(intent);
                            break;
                    }
                }
            });
        }
    }

    /**
     * A user clicked on the notification! Do whatever you want here!
     * <p/>
     * If you extend this and do not call through to the super your main activity will not launch by default.
     *
     * @param context Receiver context
     * @param intent  Intent that contains the custom data provided on the Taplytics dashboard.
     */
    public void pushOpened(Context context, Intent intent) {
        TLUtils.launchMainActivity();
    }

    /**
     * The push has been dismissed :(
     *
     * @param context Receiver context
     * @param intent  Intent that contains the custom data provided on the Taplytics dashboard.
     */
    public void pushDismissed(Context context, Intent intent) {
    }

    /**
     * The push was received, but not opened yet!
     * <p/>
     * If you add the custom data of tl_silent = true to the push notification,
     * there will be no push notification presented to the user. However, this will
     * still be triggered, meaning you can use this to remotely trigger something
     * within the application!
     *
     * @param context Receiver context
     * @param intent  Intent that contains the custom data provided on the Taplytics dashboard.
     */
    public void pushReceived(Context context, Intent intent) {
    }

}
