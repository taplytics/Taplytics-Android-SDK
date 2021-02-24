/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.listeners;

import android.view.View;
import android.view.ViewTreeObserver;

/**
 * A listener which listens for global layout changes and checks that views are FORCED to keep their assigned visibilities by Taplytics.
 */
public class TLAggressiveVisibilityListener implements ViewTreeObserver.OnGlobalLayoutListener {

    private View v;

    private int visibility = View.VISIBLE;

    public TLAggressiveVisibilityListener getListener(View view, Object finalValue) {
        v = view;
        try {
            visibility = (int) finalValue;
        } catch (Throwable ignored) {
        }
        return this;
    }

    @Override
    public void onGlobalLayout() {
        try {
            if (visibility != v.getVisibility()) {
                v.setVisibility(visibility);
            }
        } catch (Throwable ignored) {

        }
    }
}