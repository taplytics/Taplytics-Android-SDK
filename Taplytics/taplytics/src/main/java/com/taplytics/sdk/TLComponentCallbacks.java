/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */
package com.taplytics.sdk;

import android.content.ComponentCallbacks;
import android.content.res.Configuration;
import android.util.Log;

import com.taplytics.sdk.managers.TLManager;
import com.taplytics.sdk.managers.TLViewManager;
import com.taplytics.sdk.utils.TLLog;
import com.taplytics.sdk.utils.ViewUtils;


/**
 * Created by vic on 2018-12-03.
 */
class TLComponentCallbacks implements ComponentCallbacks {

    /**
     * For Lists inside of fragments, screen rotation acts strange.
     *
     * Essentially, the list will recycle itself after a rotation but BEFORE visual edits.
     *
     * This just sets it all up as best it can.
     * @param newConfig
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if(!TLManager.getInstance().isActivityActive() || !TLViewManager.getInstance().hasAggressiveViewChanges() || TLViewManager.getInstance().getCurrentViewGroup() == null) {
            return;
        }

        try {

            //Go wild if aggressive is true
            TLViewManager.getInstance().updateViewsIfNecessary();

            TLViewManager.getInstance().getCurrentViewGroup().post(new Runnable() {
                @Override
                public void run() {
                    try {
                        TLViewManager.getInstance().updateViewsIfNecessary();
                        ViewUtils.attachInitialListeners(TLViewManager.getInstance().getCurrentViewGroup(), false);
                    } catch (Throwable t) {
                        // Safety
                    }
                }
            });

            //Do it after the view has rendered as well.
            TLViewManager.getInstance().getCurrentViewGroup().postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        TLViewManager.getInstance().updateViewsIfNecessary();
                        ViewUtils.attachInitialListeners(TLViewManager.getInstance().getCurrentViewGroup(), false);
                    } catch (Throwable t) {
                        // Safety
                    }
                }
            }, 50);

            //Do it after the view has rendered as well.
            TLViewManager.getInstance().getCurrentViewGroup().postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        TLViewManager.getInstance().updateViewsIfNecessary();
                        ViewUtils.attachInitialListeners(TLViewManager.getInstance().getCurrentViewGroup(), false);
                    } catch (Throwable t) {
                        // Safety
                    }
                }
            }, 150);
        } catch (Throwable t){
            //
        }

    }

    @Override
    public void onLowMemory() {
        // Not very useful atm
    }
}
