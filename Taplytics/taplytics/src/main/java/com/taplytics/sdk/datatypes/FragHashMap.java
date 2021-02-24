/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.datatypes;

import com.taplytics.sdk.managers.TLManager;

import java.util.HashMap;

/**
 * Created by vicvu on 2016-09-15.
 */

public class FragHashMap<T, Y> extends HashMap {

    @Override
    public boolean containsKey(Object key) {
        try {
            boolean containsKey = super.containsKey(key);
            if (!containsKey) {
                if (key instanceof Integer) {
                    String newKey = TLManager.getInstance().getAppContext().getResources().getResourceEntryName((int) key);
                    containsKey = super.containsKey(newKey);
                }
            }
            return containsKey;
        } catch (Throwable e) {
            //
        }
        return super.containsKey(key);
    }

    @Override
    public Object get(Object key) {
        try {
            boolean containsKey = super.containsKey(key);
            if (!containsKey) {
                if (key instanceof Integer) {
                    String newKey = TLManager.getInstance().getAppContext().getResources().getResourceEntryName((int) key);
                    return super.get(newKey);
                }
            }
        } catch (Throwable e) {
//
        }
        return super.get(key);
    }

    @Override
    public Object remove(Object key) {
        try {
            boolean containsKey = super.containsKey(key);
            if (!containsKey) {
                if (key instanceof Integer) {
                    String newKey = TLManager.getInstance().getAppContext().getResources().getResourceEntryName((int) key);
                    return super.remove(newKey);
                }
            }
        } catch (Throwable e) {
//
        }
        return super.remove(key);
    }

}
