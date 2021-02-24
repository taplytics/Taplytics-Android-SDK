/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;

import com.taplytics.sdk.analytics.TLAnalyticsManager;
import com.taplytics.sdk.analytics.external.TLExternalAnalyticsManager;
import com.taplytics.sdk.datatypes.Triplet;
import com.taplytics.sdk.managers.TLDialogManager;
import com.taplytics.sdk.managers.TLFragmentManager;
import com.taplytics.sdk.managers.TLManager;
import com.taplytics.sdk.managers.TLViewManager;
import com.taplytics.sdk.network.TLSocketManager;
import com.taplytics.sdk.utils.FragmentUtils;
import com.taplytics.sdk.utils.Functionality;
import com.taplytics.sdk.utils.TLLog;
import com.taplytics.sdk.utils.TLUtils;
import com.taplytics.sdk.utils.promises.Promise;
import com.taplytics.sdk.utils.promises.PromiseListener;

import java.util.ArrayList;
import java.util.Map;

class TLActivityLifecycleCallbacksManager {

    /**
     * This is a promise made to determine when the app is backgrounded. See onActivityPaused for implementation
     */
    private Promise<Object> backgroundPromise;


    /**
     * Promise to determine the timeout of the TurnMenu. Basically we don't want it to pop up if you've been turning the device naturally.
     */
    private Promise turnMenuPromise;

    /**
     * The list of the last few orientation changes. just to make sure we've been alternating. There is probably a better way to track this?
     */
    private ArrayList<Integer> orientationList;

    private static final TLActivityLifecycleCallbacksManager ourInstance = new TLActivityLifecycleCallbacksManager();

    public static TLActivityLifecycleCallbacksManager getInstance() {
        return ourInstance;
    }

    private TLActivityLifecycleCallbacksManager() {
        backgroundPromise = new Promise<>();
        turnMenuPromise = new Promise();
        orientationList = new ArrayList<>();
    }

    void activityCreated(final Activity activity, final Bundle bundle) {
        try {
            TLLog.debug("Activity has been created: " + activity.getClass().getSimpleName());
            // If first Activity created get initial properties from server
            TLManager.getInstance().getInitialPropertiesFromServer();
        } catch (Exception e) {
            TLLog.error("Activity null", e);
        }
    }

    void activityDestroyed(final Activity activity) {
        try {
            String className = activity.getClass().getSimpleName();
            TLLog.debug("Activity has been destroyed: " + className);
            /*
             * Check if the activity we're finishing is also the current activity (meaning no new one has been put in place). If it is, that
             * means we need to finish our background promise and its safe to track an app terminate.
             */
            if (TLManager.getInstance().isCurrentActivityEqualTo(activity) && !TLUtils.isDisabled(Functionality.VIEWTRACKING)) {
                backgroundPromise.finish();
                TLManager.getInstance().getTlAnalytics().trackTLEvent(TLAnalyticsManager.TLAnalyticsEventAppTerminate);
            }
        } catch (Exception e) {
            TLLog.error("Problem destroying activity: ", e);
        }
    }

    void activityPaused(final Activity activity) {
        try {
            TLLog.debug("Activity Paused");
            // Make a promise. If the app does not start an activity within the
            // given time-frame, then the app can be assumed to be backgrounded.
            backgroundPromise = new Promise<>();
            backgroundPromise.add(new com.taplytics.sdk.utils.promises.PromiseListener<Object>() {
                @Override
                public void failed() {
                    TLManager.getInstance().setAppHasBackgrounded(true);
                    if (!TLUtils.isDisabled(Functionality.VIEWTRACKING)) {
                        TLManager.getInstance().getTlAnalytics().trackTLEvent(TLAnalyticsManager.TLAnalyticsEventAppBackground);
                    }
                    TLManager.getInstance().getTlAnalytics().flushEventsQueue(null);
                    TLManager.getInstance().getTlAnalytics().getTlAppUser().flushAppUserAttributes(null);
                    TLLog.debug("App assumed to be backgrounded");

                    if (TLManager.getInstance().isLiveUpdate()) {
                        TLSocketManager.getInstance().disconnectSocketIO();
                    }

                    if (TLExternalAnalyticsManager.getInstance() != null &&
                            TLExternalAnalyticsManager.getInstance().getKnownManagers().size() > 0) {
                        TLExternalAnalyticsManager.getInstance().forceFlushAndSendToTaplytics();
                    }
                    super.failed();
                }
            });

            // Get rid of all of our shake menu dialogs.
            TLDialogManager.getInstance().dismissAllDialogs();


            //Set the new activity if necessary
            if (TLManager.getInstance().isCurrentActivityEqualTo(activity)) {
                TLManager.getInstance().setActivityActive(false);
            }

            // arbitrary time. Fix later.
            backgroundPromise.timeout(800);
        } catch (Exception e) {
            TLLog.error("Error pausing: ", e);
        }
    }

    void activityResumed(final Activity activity) {
        try {
            TLLog.debug("Activity Resumed: " + activity.getClass().getSimpleName());

            if (TLManager.getInstance().isCurrentActivityEqualTo(activity)) {
                TLManager.getInstance().setActivityActive(true);
            }
            // If this promise succeeds, then the app isn't backgrounded.
            backgroundPromise.finish();

            // If promise failed, then the app was backgrounded
            if (backgroundPromise.hasFailed()) {
                TLManager.getInstance().getTlAnalytics().trackTLEvent(TLAnalyticsManager.TLAnalyticsEventAppForeground);
                TLManager.getInstance().getTlAnalytics().trackTLEvent(TLAnalyticsManager.TLAnalyticsEventAppActive);
            }

            // In case changes to the panel happened when we were in the background, we want to make sure it changed here.
            if (TLManager.getInstance().isLiveUpdate() && TLManager.getInstance().getTlPropertiesPromise().isFinished()) {
                TLViewManager.getInstance().updateViewsIfNecessary();
            }

            // When the activity resumes, we're going to check if it was launched via the intent of taplytics.
            TLManager.getInstance().checkActivityForAppLink(activity);

            if (!TLUtils.isDisabled(Functionality.FRAGMENTS) && !TLUtils.isDisabled(Functionality.VIEWTRACKING)) {
                FragmentUtils.resumeFragments();
            }


            //This is the "turn menu".
            //Emulators don't have a shake option. But they do have a 'turn' option.
            //Essentially, we wait to see if the device was turned three times (back to original state).
            if (TLManager.getInstance().isTurnMenuEnabled() &&
                    TLManager.getInstance().isActivityActive() &&
                    TLManager.getInstance().isLiveUpdate() &&
                    TLManager.getInstance().getTrackingEnabled()) {
                //If its the first time or our promise has completed either via timeout or by showing the menu.
                if (orientationList.isEmpty() || turnMenuPromise.isFinished() || turnMenuPromise.hasFailed()) {
                    //Add our orientation to the list.
                    orientationList.clear();
                    orientationList.add(activity.getResources().getConfiguration().orientation);
                    turnMenuPromise = new Promise();
                    turnMenuPromise.add(new PromiseListener() {
                        @Override
                        public void failedOrCancelled() {
                            orientationList.clear();
                        }
                    });
                    //Just timeout after 30 seconds because by that point you'd figure these turns aren't intentional or something.
                    turnMenuPromise.timeout(30000);
                } else {
                    //Grab the last orientation.
                    int pastOrientation = orientationList.get(orientationList.size() - 1);
                    //Grab the current one.
                    int currentOrientation = activity.getResources().getConfiguration().orientation;

                    //Check if there is actually a new orientation.
                    if (currentOrientation != pastOrientation) {
                        //Add the new one to the list
                        orientationList.add(currentOrientation);
                        //Bam. Show that dialog.
                        if (orientationList.size() > 3) {
                            TLDialogManager.getInstance().setupExperimentDialog();
                            //Finish the promise now.
                            turnMenuPromise.finish();
                        }
                    }
                }
            }
        } catch (Exception e) {
            TLLog.error("Error resuming: ", e);
        }
    }

    void activityStarted(final Activity activity) {
        try {
            String className = activity.getClass().getSimpleName();
            TLLog.debug("Activity Started: " + className);

            // Track this stuff if not disabled.
            if (!TLUtils.isDisabled(Functionality.VIEWTRACKING)) {
                TLManager.getInstance().getTlAnalytics().trackTLActivityEvent(TLAnalyticsManager.TLAnalyticsEventActivityAppeared, null, className);
                TLManager.getInstance().getTlAnalytics().getActivityMap().put(className, System.currentTimeMillis());
            }
            // Set the activity now.
            TLManager.getInstance().setCurrentActivity(activity);

            //When the activity starts, we're going to check if it was launched via the intent of taplytics.
            TLManager.getInstance().checkActivityForAppLink(activity);

            if (TLManager.getInstance().isLiveUpdate() && !TLSocketManager.getInstance().isConnected) {
                TLSocketManager.getInstance().connectSocketIO(null, false);
            }
            try {
                TLViewManager.getInstance().setCurrentViewGroup((ViewGroup) activity.findViewById(android.R.id.content).getRootView());
            } catch (Throwable e) {
                TLViewManager.getInstance().setCurrentViewGroup((ViewGroup) activity.getWindow().findViewById(android.R.id.content).getRootView());
            }

        } catch (Throwable e) {
            TLLog.error("error starting activity", e);
        }
    }

    void activityStopped(final Activity activity) {
        try {
            String className = activity.getClass().getSimpleName();
            TLLog.debug("App Activity Stopped: " + className);

            if (!TLUtils.isDisabled(Functionality.VIEWTRACKING)) {
                TLManager.getInstance().getTlAnalytics().trackTLActivityEvent(TLAnalyticsManager.TLAnalyticsEventActivityDisappeared, null, className);
                if (!TLUtils.isDisabled(Functionality.FRAGMENTS) && TLFragmentManager.getInstance().getFragmentsOnScreen() != null) {
                    for (Map.Entry<Object, Triplet<String, String, Boolean>> entry : TLFragmentManager.getInstance().getFragmentsOnScreen().entrySet()) {
                        if (entry.getValue().first.equals(activity.getClass().getSimpleName())) {
                            String fragmentClassName = TLAnalyticsManager.getFragmentMap().get(entry.getKey()).first;
                            TLManager.getInstance().getTlAnalytics().trackTLActivityEvent(TLAnalyticsManager.TLAnalyticsEventActivityDisappeared, null, fragmentClassName);
                            entry.getValue().third = true;

                            // Track the time we spent on this fragment
                            TLAnalyticsManager.trackFragmentTime(entry.getKey(), fragmentClassName, true);
                        }
                    }
                }
                TLAnalyticsManager.trackActivityTime(className);
            }
        } catch (Exception e) {
            TLLog.error("Error stopping activity: ", e);
        }
    }
}
