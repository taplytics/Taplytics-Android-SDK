/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.managers;

import com.taplytics.sdk.TaplyticsDelayLoadListener;
import com.taplytics.sdk.TaplyticsExperimentsLoadedListener;
import com.taplytics.sdk.utils.Functionality;
import com.taplytics.sdk.utils.TLUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * TLDelayLoadManager
 *
 * This class is used to handle the different scenarios for those wishing to delay loading their application
 * in anticipation of Taplytics loading.
 *
 * This manager handles calling back when Taplytics has started. Currently relies on Taplytics.startTaplytics.
 * It short circuits if we set a max time and the callback hasn't happened yet.
 * It waits a minimum amount of time (if minimum is set) before calling back.
 *
 * Only the first set of min/max is respected. All others are ignored.
 */
public class TLDelayLoadManager implements TaplyticsExperimentsLoadedListener {

    // Static

    /**
     * TLDelayLoadManager instance variable
     */
    private static TLDelayLoadManager delayLoadManagerInstance;

    /**
     * TLDelayLoadManager has single instance access. Please do not copy this object, instead call getInstance() every time you want an instance
     * @return The Instance
     */
    public synchronized static TLDelayLoadManager getInstance() {
        if (delayLoadManagerInstance == null) {
            delayLoadManagerInstance = new TLDelayLoadManager();
        }
        return delayLoadManagerInstance;
    }

    //Instance

    // Collection of LoadListeners
    private final Set<TaplyticsDelayLoadListener> delayLoadListeners;
    // Tracks if we have already completed loading, only set to true once
    volatile boolean delayLoadCompleted;
    // Tracks if we have started loading, only set to true once
    volatile boolean delayLoadStarted;
    // Used to tell if we hav already completed loading. If no minimum is set, we set this to true from the start.
    volatile boolean minimumDelayLoadCompleted;

    // Used to call complete
    Runnable completedRunnable = new Runnable() {
        @Override
        public void run() {
            runComplete();
        }
    };
    // Used to call minimum complete
    Runnable minimumCompletedRunnable = new Runnable() {
        @Override
        public void run() {
            runMinimumComplete();
        }
    };

    /**
     * Private constructor called by the getInstance method
     */
    private TLDelayLoadManager() {
        delayLoadListeners = new HashSet<>();
    }

    /**
     * Adds a TaplyticsDelayLoadListener to set of listeners waiting for callbacks.
     *
     * This does not start the delay, please call the start(...) methods to start the delay with appropriate max min times.
     *
     * Does nothing if Delayload is disabled through the properties.
     *
     * If delay load is already completed calls startDelay and delayCompleted callback immediately.
     *
     * If we've already started but not completed, will call startDelay immediately and queue the listener.
     *
     * Calls back one instance of the object no matter how many times it was added, unless we've already called you back once, at which point we call you back immediately.
     *
     * @param delayLoadListener The delay load listener
     */
    public synchronized void addDelayLoadListener(final TaplyticsDelayLoadListener delayLoadListener) {
        if (!hasPermissionToStart()) {
            return;
        }

        if (delayLoadListener == null) {
            return;
        }

        if (minimumDelayLoadCompleted && delayLoadCompleted) {
            sendFullFlowOnUiThread(delayLoadListener);
            return;
        } else if (delayLoadStarted) {
            sendStartDelayOnUiThread(delayLoadListener);
        }

        delayLoadListeners.add(delayLoadListener);
    }

    /**
     * Starts the delay with said delayTime.
     *
     * If delay time is less than or equal to 0, we call back immediately. If you find yourself doing this, you may as well register for the startTaplytics callbacks in Taplytics.
     *
     * Only the first delays are respected, all others are ignored.
     *
     * @param delayTime in milliseconds
     */
    public void start(final long delayTime) {
        start(delayTime, delayTime);
    }

    /**
     * Starts the delay with said delayTime and minimumDelay.
     *
     * If delay time is less than or equal to 0, we call back immediately. If you find yourself doing this, you may as well register for the startTaplytics callbacks in Taplytics.
     *
     * If minimumDelay is less than or equal to 0, or if it is greater than delayTime, it is ignored.
     *
     * Only the first delays are respected, all others are ignored.
     *
     * @param delayTime
     * @param minimumDelay
     */
    public synchronized void start(final long delayTime, final long minimumDelay) {
        if (!hasPermissionToStart()) {
            return;
        }

        final boolean noDelay = delayTime <= 0;

        final boolean runFullFlow = !delayLoadStarted && noDelay || delayLoadCompleted;
        if (runFullFlow) {
            runFull();
        } else if (!delayLoadStarted) {
            TLManager.getInstance().registerExperimentsLoadedListener(this);

            runStart();

            final boolean shouldRunMin = delayTime > minimumDelay && minimumDelay > 0;
            if (shouldRunMin) {
                TLThreadManager.getInstance().scheduleOnBackgroundThread(minimumCompletedRunnable, minimumDelay, TimeUnit.MILLISECONDS);
            } else {
                minimumDelayLoadCompleted = true;
            }
            TLThreadManager.getInstance().scheduleOnBackgroundThread(completedRunnable, delayTime, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * This method is called when the startTaplytics calls us back in an attempt to short circuit the delay.
     */
    private synchronized void completeEarly() {
        if (!minimumDelayLoadCompleted){
            delayLoadCompleted = true;
            return;
        }

        if (delayLoadCompleted) {
            runFull();
        } else if (delayLoadStarted) {
            runComplete();
        } else {
            runStart();
            runComplete();
        }
    }

    /**
     * Helper method to tell us if we have the permissions required to run the delay.
     *
     * @return rue if we have the permissions, false otherwise
     */
    private boolean hasPermissionToStart() {
        return !TLUtils.isDisabled(Functionality.DELAYLOAD);
    }

    /**
     * Runs full set of callbacks. This happens if there is no delay, or if a listener is added after we have completed delay load once.
     */
    private synchronized void runFull() {
        minimumDelayLoadCompleted = true;
        delayLoadStarted = true;
        delayLoadCompleted = true;

        for (final TaplyticsDelayLoadListener delayLoadListener : delayLoadListeners) {
            sendFullFlowOnUiThread(delayLoadListener);
        }
        delayLoadListeners.clear();
    }

    /**
     * Runs Complete if we have received a startTaplytics callback, otherwise, just reports that we have completed the minimum wait time.
     */
    private synchronized void runMinimumComplete() {
        minimumDelayLoadCompleted = true;

        if (delayLoadCompleted) {
            runComplete();
        }
    }

    /**
     * Runs when we get a callback from startTaplytics. Calls delayComplete on all the listeners, if the minimum delay has already completed. Waits for minimum delay to complete
     * otherwise and just sets the state to completed in order for the minimum callback to call this method appropriately.
     */
    private synchronized void runComplete() {
        delayLoadCompleted = true;

        if (!minimumDelayLoadCompleted) {
            return;
        }

        for (final TaplyticsDelayLoadListener delayLoadListener : delayLoadListeners) {
            sendDelayCompleteOnUiThread(delayLoadListener);
        }
        delayLoadListeners.clear();
    }

    /**
     * Calls back startDelay on all callbacks. Calls back on UI Thread
     */
    private synchronized void runStart() {
        delayLoadStarted = true;

        for (final TaplyticsDelayLoadListener delayLoadListener : delayLoadListeners) {
            sendStartDelayOnUiThread(delayLoadListener);
        }
    }

    /**
     * Sets runnable to callback startDelay on UI Thread
     * @param delayLoadListener
     */
    private void sendStartDelayOnUiThread(final TaplyticsDelayLoadListener delayLoadListener) {
        TLThreadManager.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                delayLoadListener.startDelay();
            }
        });
    }

    /**
     * Sets runnable to callback delayComplete on UI Thread
     * @param delayLoadListener
     */
    private void sendDelayCompleteOnUiThread(final TaplyticsDelayLoadListener delayLoadListener) {
        TLThreadManager.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                delayLoadListener.delayComplete();
            }
        });
    }

    /**
     * Sets runnable to callback startDelay and delayComplete on UI Thread
     * @param delayLoadListener
     */
    private void sendFullFlowOnUiThread(final TaplyticsDelayLoadListener delayLoadListener) {
        TLThreadManager.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                delayLoadListener.startDelay();
                delayLoadListener.delayComplete();
            }
        });
    }

    // Interfaces

    /**
     * Callback from TaplyticsExperimentsLoadedListener. We call the completeEarly method.
     */
    @Override
    public void loaded() {
        completeEarly();
    }

    // Deprecated

    /**
     * Sets the listener and starts with said delayTime. See addDelayLoadListener and start(long) for details.
     * @param delayLoadListener
     * @param delayTime
     */
    @Deprecated
    public void delayLoad(final TaplyticsDelayLoadListener delayLoadListener, final long delayTime) {
        addDelayLoadListener(delayLoadListener);
        start(delayTime);
    }

    /**
     * Sets the listener and starts with said minTime and delayTime. See addDelayLoadListener and start(long, long) for details.
     * @param taplyticsDelayLoadListener
     * @param delayTime
     * @param minTime
     */
    @Deprecated
    public void delayLoad(final TaplyticsDelayLoadListener taplyticsDelayLoadListener, final long delayTime, final long minTime) {
        addDelayLoadListener(taplyticsDelayLoadListener);

        final long defaultTimeout = TLManager.getInstance().getTimeout();
        if (delayTime < defaultTimeout) {
            start(defaultTimeout, minTime);
        } else {
            start(delayTime, minTime);
        }

    }

    // Testing

    /**
     * Used for testing
     * @param delayLoadManager
     */
    synchronized static void setInstance(final TLDelayLoadManager delayLoadManager) {
        delayLoadManagerInstance = delayLoadManager;
    }

    /**
     * Used for testing
     * @param delayLoadListeners
     */
    TLDelayLoadManager(final HashSet<TaplyticsDelayLoadListener> delayLoadListeners) {
        this.delayLoadListeners = delayLoadListeners;
    }

}
