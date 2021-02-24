/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.managers;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * TLTHreadManager Handles all the threading for the application. There are two options; running on a background thread and running
 * on the main UI thread.
 *
 * Please:
 * * Do all non-UI work on the background thread.
 * * Do all UI work on UI Thread
 * * All non-Taplytics callbacks should run on the UI Thread.
 *
 */
public class TLThreadManager {

    // Static
    private static TLThreadManager threadManagerInstance;
    private static final int POOL_SIZE = 4;

    /**
     * TLThreadManager has single instance access. Please do not copy this object, instead call getInstance() every time you want an instance
     * @return The Instance
     */
    public synchronized static TLThreadManager getInstance() {
        if (threadManagerInstance == null) {
            threadManagerInstance = new TLThreadManager();
        }
        return threadManagerInstance;
    }

    // Instance

    // Background ThreadExecutor
    private final ScheduledExecutorService backgroundThreadExecutor;
    // UI Thread Handler
    private final Handler uiThreadHandler;

    /**
     * Private Constructor used by the getInstance Method
     */
    private TLThreadManager() {
        backgroundThreadExecutor = new ScheduledThreadPoolExecutor(POOL_SIZE);
        uiThreadHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Schedules a runnable to run on the background thread.
     *
     * @param runnable The runnable you wish to run
     * @param delay The delay count with respect the the TimeUnit provided
     * @param timeUnit The TimeUnit provided
     */
    public void scheduleOnBackgroundThread(final Runnable runnable, final long delay, final TimeUnit timeUnit) {
        if (runnable == null || timeUnit == null) {
            return;
        }

        backgroundThreadExecutor.schedule(runnable, delay, timeUnit);
    }

    /**
     * Schedules a runnable to run on the UI thread.
     *
     * @param runnable The runnable you wish to run
     * @param delay The delay count with respect the the TimeUnit provided
     * @param timeUnit The TimeUnit provided
     */
    public void scheduleOnUiThread(final Runnable runnable, final long delay, final TimeUnit timeUnit) {
        if (runnable == null || timeUnit == null) {
            return;
        }

        final long delayMilliseconds = timeUnit.toMillis(delay);
        uiThreadHandler.postDelayed(runnable, delayMilliseconds);
    }

    /**
     * Queues a runnable to run on the background thread immediately.
     *
     * @param runnable The runnable you wish to run
     */
    public void runOnBackgroundThread(final Runnable runnable) {
        if (runnable == null) {
            return;
        }

        backgroundThreadExecutor.execute(runnable);
    }

    /**
     * Queues a runnable to run on the UI thread immediately.
     *
     * @param runnable The runnable you wish to run
     */
    public void runOnUiThread(final Runnable runnable) {
        if (runnable == null) {
            return;
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
            return;
        }

        uiThreadHandler.post(runnable);
    }

    // Testing

    /**
     * Used for testing
     * @param threadManager
     */
    synchronized static void setInstance(final TLThreadManager threadManager) {
        threadManagerInstance = threadManager;
    }

    /**
     * User for Testing
     * @param scheduledExecutorService
     * @param handler
     */
    TLThreadManager(final ScheduledExecutorService scheduledExecutorService, final Handler handler) {
        backgroundThreadExecutor = scheduledExecutorService;
        uiThreadHandler = handler;
    }

}


