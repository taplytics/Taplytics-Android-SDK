/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.utils;

import android.util.Log;

import com.taplytics.sdk.analytics.TLAnalyticsManager;
import com.taplytics.sdk.managers.TLManager;

import java.util.Date;

public class TLLog {

    private static TLLog instance = null;

    public static TLLog getInstance() {
        if (instance == null) {
            instance = new TLLog();
        }
        return instance;
    }

    private TLLog() {
    }

    public void setDebugLogging(boolean debugLog) {
        debugLogging = debugLog;
    }

    private static boolean debugLogging = false;

    public void setClientLogging(boolean logging) {
        clientLogging = logging;
    }

    private static boolean clientLogging = false;

    public static String getTag() {
        return TAG;
    }

    private static final String TAG = "Taplytics";

    public static void debug(String log) {
        debug(log, null, false);
    }

    public static void debug(String log, Date time) {
        debug(log, time, false);
    }

    public static void debug(String log, boolean forClients) {
        debug(log, null, forClients);
    }

    public static void debug(String log, Date time, boolean forClients) {
        try {
            if (debugLogging || (clientLogging && forClients)) {
                if (time != null) {
                    Date now = new Date();
                    long offset = now.getTime() - time.getTime();

                    Log.d(TAG, addThreadName(log + ", time: " + offset + "ms"));
                } else {
                    Log.d(TAG, addThreadName(log));
                }
            }
        } catch (Exception e) {
            //
        }
    }

    public static void warning(String log) {
        warning(log, null, false);
    }

    public static void warning(String log, Exception e) {
        warning(log, e, false);
    }

    public static void warning(String log, boolean forClients) {
        warning(log, null, forClients);
    }

    public static void warning(String log, Exception e, boolean forClients) {
        try {
            if (debugLogging || (clientLogging && forClients)) {

                if (e != null)
                    Log.w(TAG, "WARNING: " + log + ", error: " + e.toString());
                else
                    Log.w(TAG, "WARNING: " + log);
            }
        } catch (Exception er) {
            //
        }
    }

    public static void error(String log) {
        error(log, null);
    }

    public static void error(String log, Throwable e) {
        error(log, e, true, false);
    }

    public static void error(String log, Throwable e, boolean sendToServer) {
        error(log, e, sendToServer, false);
    }

    public static void error(String log, Throwable e, boolean sendToServer, boolean forClients) {
        try {
            if (debugLogging || (clientLogging && forClients)) {

                if (e != null)
                    Log.e(TAG, "ERROR: " + log + ", error: " + e.toString());
                else
                    Log.e(TAG, "ERROR: " + log);
            }
            if (TLManager.getInstance().getTlAnalytics() != null) {
                TLManager.getInstance().getTlAnalytics().trackError(log, (e instanceof Exception) ? (Exception) e : new Exception(), sendToServer);
            }
        } catch (Exception er) {
            //
        }
    }

    public static void requestError(String path, String text, Throwable e) {
        try {
            if (debugLogging) {

                if (e != null)
                    Log.e(TAG, "NETWORK ERROR: " + text + ", error: " + e.getMessage());
                else
                    Log.e(TAG, "NETWORK ERROR: " + text);
            }

            if (TLManager.getInstance().isActivityActive()) {
                TLManager.getInstance().getTlAnalytics().trackRequestError(TLAnalyticsManager.TLAnalyticsRequestError, path, text, e);
            } else {
                if (e != null) {
                    TLLog.error("Network Error:" + text + ", error: " + e.toString());
                } else {
                    TLLog.error("Network Error:" + text);
                }
            }
        } catch (Exception er) {
            //
        }
    }

    /**
     * This is for errors that even break our error tracking.
     *
     * @param log
     * @param e
     */
    public static void superError(String log, Exception e) {
        if (debugLogging) {

            if (e != null)
                Log.e(TAG, "SUPER ERROR: " + log + ", error: " + e.toString());
            else
                Log.e(TAG, "SUPER ERROR: " + log);
        }
    }

    private static String addThreadName(String log) {
        Thread thread = Thread.currentThread();
        String threadName = (thread.getName() != null) ? thread.getName() : thread.getId() + "";
        return threadName + ": " + log;
    }

}
