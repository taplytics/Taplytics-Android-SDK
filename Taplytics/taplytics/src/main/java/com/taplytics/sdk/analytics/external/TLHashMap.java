/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.analytics.external;

import android.os.Handler;
import android.os.Looper;

import com.taplytics.sdk.managers.TLManager;

import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by VicV on 1/16/15.
 */
public class TLHashMap extends HashMap {
    @Override
    public Object put(final Object key, Object value) {
//        TLLog.debug("ADDED SOMETHING TO FLURRY: KEY:" + key.toString() + ". VAL: " + value.toString());
        try {
            int intVal = (int) value.getClass().getFields()[0].get(value);
            if (intVal > 0) {
                new Handler(Looper.getMainLooper())
                        .postDelayed(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            //Don't have a value set, just use zero
                                            if (key.toString().split("\\.").length < 2) {
                                                trackFlurryEvent(key.toString(), 0);
                                            }
                                        } catch (Throwable e) {
                                            //
                                        }
                                    }
                                }, 200);
                return super.put(key, value);
            }
        } catch (Throwable e) {
            //Because this is intentionally thrown sometimes and we want to ignore it.
        }
        try {
            new Handler(Looper.getMainLooper())
                    .postDelayed(
                            new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        //Don't have a value set, just use zero
                                        if (key.toString().split("\\.").length < 2) {
                                            trackFlurryEvent(key.toString(), 0);
                                        }
                                    } catch (Throwable e) {
                                        //
                                    }
                                }
                            }, 200);
        } catch (Throwable e) {
//            TLLog.warning("flurry put track", e instanceof Exception ? (Exception) e : null);
        }

        return super.put(key, value);
    }

    @Override
    public Object get(Object key) {
        try {
            final String keyString = key.toString();
            final Object cx = super.get(key);
            if (cx != null) {
                final int size = cx.getClass().getDeclaredFields()[0].getInt(cx);
                new Handler(Looper.getMainLooper())
                        .postDelayed(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            int newSize = cx.getClass().getDeclaredFields()[0].getInt(cx);
                                            if (newSize != size) {
                                                //If we are seeing a get, basically the size is changing.
                                                if (keyString.split("\\.").length < 2) {

                                                    trackFlurryEvent(keyString, newSize);


                                                }
                                            }
                                        } catch (Throwable e) {
                                            //
                                        }
                                    }
                                }
                                , 4);


            }
        } catch (Throwable e) {
//            TLLog.error("flurry get", e instanceof Exception ? (Exception) e : null);
        }
        return super.get(key);
    }

    private void trackFlurryEvent(String key, int size) {
        try {
            JSONObject events = new JSONObject();

            try {
                HashMap map = TLExternalAnalyticsManager.getInstance().getFlurryEventHashForName(key);
                events = new JSONObject(map);
            } catch (Throwable e) {
                //
            }

            TLManager.getInstance().getTlAnalytics()
                    .trackSourceEvent(TLExternalAnalyticsManager.TLAnalyticsSourceFlurry,
                            key, size, events);

        } catch (Throwable e) {
            //
        }

    }
}
