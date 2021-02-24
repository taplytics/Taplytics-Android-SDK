/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.resources;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.taplytics.sdk.analytics.TLAnalyticsManager;
import com.taplytics.sdk.compat.GestureDetectorCompat;
import com.taplytics.sdk.datatypes.Triplet;
import com.taplytics.sdk.managers.TLFragmentManager;
import com.taplytics.sdk.managers.TLManager;
import com.taplytics.sdk.utils.Functionality;
import com.taplytics.sdk.utils.TLLog;
import com.taplytics.sdk.utils.TLUtils;
import com.taplytics.sdk.utils.ViewUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import static com.taplytics.sdk.utils.ViewUtils.checkForListViewClick;
import static com.taplytics.sdk.utils.constants.ViewConstants.ANDROID_IDENTIFIER;
import static com.taplytics.sdk.utils.constants.ViewConstants.CELL_INFO;
import static com.taplytics.sdk.utils.constants.ViewConstants.FRAG_IDENTIFIER;
import static com.taplytics.sdk.utils.constants.ViewConstants.INIT_PROPERTIES;

/**
 * Created by VicV on 10/13/14.
 * <p/>
 * An overlay across every app screen that tracks clicks.
 */
public class TouchScreenOverlay extends FrameLayout implements GestureDetector.OnGestureListener {

    /**
     * Max allowed duration for a "click", in millis/
     */
    private static final int MAX_CLICK_DURATION = 1000;

    /**
     * Max allowed distance to move during a "click", in dp.
     */
    private static final int MAX_CLICK_DISTANCE = 15;

    private boolean dialog = false;

    private GestureDetectorCompat mDetector;

    public TouchScreenOverlay(Context context) {
        super(context);
        mDetector = new GestureDetectorCompat(context, this);
    }

    public TouchScreenOverlay(Context appContext, boolean dialog) {
        this(appContext);
        this.dialog = dialog;
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        return super.onGenericMotionEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        mDetector.onTouchEvent(e);
        return super.onTouchEvent(e);
    }

    /**
     * Determines whether or not the given viewObjobject and tapped view are the same.
     * If so, track a goal.
     */
    private boolean tappedViewIsGoal(Resources res, JSONObject viewObj, View clickable) {
        try {
            String identifier = res.getResourceEntryName(clickable.getId());
            JSONObject initProperties = viewObj.optJSONObject(INIT_PROPERTIES);
            if (identifier.equals(initProperties.optString(ANDROID_IDENTIFIER))) {

                //Check if this is a button contained within a listView
                if (initProperties.has(CELL_INFO)) {
                    if (checkForListViewClick(initProperties, clickable)) {
                        clickGoalAchieved(identifier, viewObj);
                        return true;
                    }

                    //Check if this item exists on the correct fragment
                } else if (initProperties.has(FRAG_IDENTIFIER)) {
                    for (Triplet t : TLFragmentManager.getInstance().getFragmentsOnScreen().values()) {
                        if (t.second.equals(initProperties.opt(FRAG_IDENTIFIER))) {
                            clickGoalAchieved(identifier, viewObj);
                            return true;
                        }
                    }

                    //Otherwise, this is just an Activity with the button on it
                } else {
                    clickGoalAchieved(identifier, viewObj);
                    return true;
                }

            }
        } catch (Exception ex) {
            TLLog.error("Touch Overlay Problem in", ex);
        }
        return false;
    }

    /**
     * Determine whether or not the motionevent is related to a touch goal
     */
    private void determineIfTouchGoal(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();
        if (TLManager.getInstance().getTlProperties() != null) {
            TLManager instance = TLManager.getInstance();
            JSONArray views = instance.getTlProperties().getViews();
            Resources res = instance.getAppContext().getResources();
            ArrayList<View> viewsWithOnClick = ViewUtils.getClickablesAtPoint(x, y, ViewUtils.findAllViews((ViewGroup) this.getRootView()), dialog);
            TLLog.debug("Found " + viewsWithOnClick.size() + " clickable views.");

            for (View clickable : viewsWithOnClick) {
                for (int i = 0; i < views.length(); i++) {
                    try {
                        JSONObject viewObj = views.optJSONObject(i);
                        if (tappedViewIsGoal(res, viewObj, clickable)) {
                            return;
                        }
                    } catch (Exception ex) {
                        TLLog.error("Touch Overlay Problem in", ex);
                    }
                }
            }
        }
    }

    @Override
    public boolean onDown(MotionEvent e) {
        try {
            if (!TLUtils.isDisabled(Functionality.BUTTONS)) {
                determineIfTouchGoal(e);
            }
        } catch (Throwable ex) {
            TLLog.error("Touch Overlay Problem", ex);
        }
        return true;
    }

    private void clickGoalAchieved(String identifier, JSONObject viewObj) {
        TLLog.debug("Touch Goal Achieved: "+identifier, true);
        TLManager.getInstance().getTlAnalytics().trackViewEvent(TLAnalyticsManager.TLAnalyticsEventTouchUp, viewObj);
        TLManager.getInstance().checkLastActivityForNewSession();
    }


    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onShowPress(MotionEvent e) {
//        TLLog.debug("showPress!");

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
//        TLLog.debug("singletapup!");

        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
//        TLLog.debug("scroll!");

        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
//        TLLog.debug("fling!");

        return false;
    }
}
