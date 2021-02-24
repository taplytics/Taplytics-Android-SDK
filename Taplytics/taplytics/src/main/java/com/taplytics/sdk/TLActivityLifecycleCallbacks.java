/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.content.ComponentCallbacks;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;

import com.taplytics.sdk.managers.TLManager;
import com.taplytics.sdk.managers.TLViewManager;

/**
 * This is for tracking activity lifecycle. Page views, etc.
 *
 * @author Vicv
 */
@TargetApi(14)
public class TLActivityLifecycleCallbacks implements ActivityLifecycleCallbacks {

    @TargetApi(14)
    public static TLActivityLifecycleCallbacks registerTLActivityLifecycleCallbacks(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= 14) {
            if (context.getApplicationContext() instanceof Application) {
                final Application app = (Application) context.getApplicationContext();
                TLActivityLifecycleCallbacks callbacks = new TLActivityLifecycleCallbacks();
                app.registerActivityLifecycleCallbacks(callbacks);
                app.registerComponentCallbacks(new TLComponentCallbacks());
                TLManager.getInstance().setActivityCallbacksRegistered(true);
                return callbacks;
            }
        }
        return null;
    }



    /**
     * Activity creation. No tracking here. *
     */
    @Override
    public void onActivityCreated(final Activity activity, final Bundle bundle) {
        TLManager.getInstance().executeRunnable(new Runnable() {
            @Override
            public void run() {
            TLActivityLifecycleCallbacksManager.getInstance().activityCreated(activity, bundle);
            }
        });
    }

    /**
     * Activity destroyed. Clear all of our fragments we've been holding on to. If there is no new activity to fill the spot, its an app
     * terminate
     */
    @Override
    public void onActivityDestroyed(final Activity activity) {
        TLManager.getInstance().executeRunnable(new Runnable() {
            @Override
            public void run() {
                TLActivityLifecycleCallbacksManager.getInstance().activityDestroyed(activity);
            }
        });
    }

    @Override
    public void onActivityPaused(final Activity activity) {
        TLManager.getInstance().executeRunnable(new Runnable() {
            @Override
            public void run() {
                TLActivityLifecycleCallbacksManager.getInstance().activityPaused(activity);
            }
        });
    }

    /**
     * Activity resumed. The promise here is set to finish so we know we didn't fully background the app, but rather just switched
     * activities.
     */
    @Override
    public void onActivityResumed(final Activity activity) {
        TLManager.getInstance().executeRunnable(new Runnable() {
            @Override
            public void run() {
                TLActivityLifecycleCallbacksManager.getInstance().activityResumed(activity);
            }
        });
    }


    /**
     * This isn't useful to us at the moment *
     */
    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
    }



    /**
     * Set the activity once its actually started *
     */
    @Override
    public void onActivityStarted(final Activity activity) {
        TLManager.getInstance().executeRunnable(new Runnable() {
            @Override
            public void run() {
                TLActivityLifecycleCallbacksManager.getInstance().activityStarted(activity);
            }
        });
    }

    /**
     * The activity has actually stopped, so we can now track that the view itself has disappeared *
     */
    @Override
    public void onActivityStopped(final Activity activity) {
        TLManager.getInstance().executeRunnable(new Runnable() {
            @Override
            public void run() {
                TLActivityLifecycleCallbacksManager.getInstance().activityStopped(activity);
            }
        });
    }

    public static void unregisterCallbacks(TLActivityLifecycleCallbacks callbacks) {
        if (android.os.Build.VERSION.SDK_INT >= 14) {
            Context context = TLManager.getInstance().getAppContext();
            if (context != null) {
                final Application app = (Application) context.getApplicationContext();
                app.unregisterActivityLifecycleCallbacks(callbacks);
                TLManager.getInstance().setActivityCallbacksRegistered(false);
            }
        }
    }



}

