/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.listeners;

import android.view.View;
import android.view.ViewGroup;

import com.taplytics.sdk.utils.TLLog;
import com.taplytics.sdk.utils.ViewUtils;

/**
 * Created by arpitpatel on 2018-03-02.
 *
 * Listens for changes in the overall viewhierarchy of the given view.
 * Applies visual edits to added views.
 *
 * This adds supports for adding visual edits on inflated views after the page has loaded.
 */
public class TLHierarchyChangeListener implements ViewGroup.OnHierarchyChangeListener {

    private ViewGroup.OnHierarchyChangeListener originalHierarchyListener = null;
    private boolean hasSupport;

    public TLHierarchyChangeListener(ViewGroup.OnHierarchyChangeListener originalHierarchyListener, boolean hasSupport) {
        this.originalHierarchyListener = originalHierarchyListener;
        this.hasSupport = hasSupport;
    }

    @Override
    public void onChildViewAdded(View parent, View child) {
        try {
            //If the child we added was a ViewGroup, apply all visual changes to its children.
            if(child instanceof ViewGroup) {
                //Attach our listeners to the new added stuff as well.
                ViewUtils.attachInitialListeners((ViewGroup) child, hasSupport);
                ViewUtils.setProperties((ViewGroup) child);
            }
        } catch (Throwable e) {
            TLLog.error("error applying changes to added child",e);
        }

        //Call their original listener if there was one.
        if (originalHierarchyListener != null) {
            originalHierarchyListener.onChildViewAdded(parent, child);
        }

    }

    @Override
    public void onChildViewRemoved(View parent, View child) {
        if (originalHierarchyListener != null) {
            originalHierarchyListener.onChildViewRemoved(parent, child);
        }
    }
}
