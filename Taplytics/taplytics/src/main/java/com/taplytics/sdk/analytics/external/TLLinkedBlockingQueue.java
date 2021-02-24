/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.analytics.external;

import com.taplytics.sdk.managers.TLManager;
import com.taplytics.sdk.utils.TLLog;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by VicV on 12/12/14.
 * <p/>
 * This is some crazy stuff.
 * <p/>
 * Deep, deep within Google Analytics this queue exits as the final transport queue to the google analytics server.
 * <p/>
 * So, what we do is replace the existing queue with our own as soon as we can.
 * <p/>
 * Whenever something is added to the field, we want to copy it over to Taplytics!
 */
public class TLLinkedBlockingQueue extends LinkedBlockingQueue {

    String fieldName = null;

    @Override
    public boolean add(Object o) {
        if (!TLManager.getInstance().getTrackingEnabled())
            return super.add(o);
        try {
            if (fieldName == null) {
                for (Field f : o.getClass().getDeclaredFields()) {
                    if (f.getType().equals(Map.class)) {
                        fieldName = f.getName();
                        break;
                    }
                }
            }
            if (fieldName != null) {
                Field f = o.getClass().getDeclaredField(fieldName);
                f.setAccessible(true);
                Map<String, String> map = new HashMap<>((Map<String, String>) f.get(o));
                TLExternalAnalyticsManager.getInstance().parseAndSendGAEvent(map);
            }

        } catch (Exception e) {
            TLLog.error("GA Break adding", e);
        }
        return super.add(o);
    }

    @Override
    public Object poll() {
        return super.poll();
    }

    @Override
    public boolean addAll(Collection c) {
        return super.addAll(c);
    }
}
