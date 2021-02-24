/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.listeners;

import android.util.Log;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.taplytics.sdk.utils.TLLog;
import com.taplytics.sdk.utils.ViewUtils;

class TLOnPreDrawListener implements ViewTreeObserver.OnPreDrawListener {
    private ViewGroup listView = null;

    private ViewTreeObserver viewTreeObserver = null;

    public TLOnPreDrawListener(ViewTreeObserver viewTreeObserver, ViewGroup listView) {
        this.viewTreeObserver = viewTreeObserver;
        this.listView = listView;
    }

    @Override
    public boolean onPreDraw() {
        try {
            if (!viewTreeObserver.isAlive()) {
                listView.getViewTreeObserver().removeOnPreDrawListener(this);
            } else {
                viewTreeObserver.removeOnPreDrawListener(this);
            }
            try {
                ViewUtils.setProperties(listView);
            } catch (Exception e) {
                TLLog.error("Exception while updating listView in onPreDraw (inner)", e);
            }
            //skipping a frame to remove flicker
        } catch (Exception e) {
            TLLog.error("Exception while updating listView in onPreDraw (outer)", e);
        }
        return false;
    }
}