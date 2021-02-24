/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.managers;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;

import com.taplytics.sdk.TaplyticsDelayLoadListener;
import com.taplytics.sdk.utils.Functionality;
import com.taplytics.sdk.utils.TLLog;
import com.taplytics.sdk.utils.TLUtils;
import com.taplytics.sdk.utils.ViewUtils;

/**
 * TLDelayLoadImageManager
 * <p>
 * This class is used to handle the different scenarios for those wishing to delay loading their application
 * in anticipation of Taplytics loading.
 * <p>
 * This Manager displays and fades out an image on a given Activity instance.
 */
public class TLDelayLoadImageManager implements TaplyticsDelayLoadListener {

    // Static

    // Starting Alpha Constant
    private static final float FROM_ALPHA = 1.0f;
    // Ending Alpha Constant
    private static final float TO_ALPHA = 0.0f;
    // TODO: Do something with this (#144744749) How long it takes for the splash screen to fade out.
    // Time the image spends fading
    private static final long FADE_TIME_IN_MILLISECONDS = 500;
    // Instance of TLDelayLoadImageManager used by getInstance()
    private static TLDelayLoadImageManager delayLoadImageManagerInstance;

    /**
     * TLDelayLoadImageManager has single instance access. Please do not copy this object, instead call getInstance() every time you want an instance
     *
     * @return The Instance
     */
    public synchronized static TLDelayLoadImageManager getInstance() {
        if (delayLoadImageManagerInstance == null) {
            delayLoadImageManagerInstance = new TLDelayLoadImageManager();
        }
        return delayLoadImageManagerInstance;
    }

    // Instance

    /**
     * Sets the DelayLoadListener again
     */
    public synchronized void updateDelayLoad() {
        TLDelayLoadManager.getInstance().addDelayLoadListener(this);
    }

    /**
     * Shows a loading page while we wait for Taplytics to load, has a maxtime which is a timeout
     *
     * @param activity
     * @param drawable
     * @param maxTime  Timeout for completion
     */
    public void delayLoadImage(final Activity activity, final Drawable drawable, final long maxTime) {
        delayLoadImage(activity, drawable, maxTime, 0);

    }

    /**
     * Shows a loading page while we wait for Taplytics to load, has a maxtime which is a timeout
     * <p>
     * Does not run if delay is not disabled
     *
     * @param activity
     * @param drawable
     * @param maxTime  Timeout for completion
     * @param minTime  Minimum time that the splash screen will stay up.
     */
    public void delayLoadImage(final Activity activity, final Drawable drawable, final long maxTime, final long minTime) {
        if (activity == null || drawable == null || isDelayDisabled()) {
            return;
        }

        runShowDelayImageOnUiThread(activity, drawable);
        TLDelayLoadManager.getInstance().addDelayLoadListener(this);
        TLDelayLoadManager.getInstance().start(maxTime, minTime);
    }

    /**
     * Fades the image after delay is complete. Clears saved image instance.
     */
    private void hideDelayImage() {
        final Activity currentActivity = TLManager.getInstance().getCurrentActivity();
        if (currentActivity == null || !TLManager.getInstance().isActivityActive()) {
            return;
        }

        final View view = currentActivity.findViewById(android.R.id.content);
        if (view == null) {
            return;
        }

        final ViewGroup viewGroup = (ViewGroup) view.getRootView();
        if (viewGroup == null) {
            return;
        }

        TLLog.debug("delay image: executed, delay done");

        final int delayViewId = ViewUtils.getDelayViewId();
        final View delayView = viewGroup.findViewById(delayViewId);
        if (delayView == null) {
            return;
        }

        final Animation animation = delayView.getAnimation();
        if (animation != null) {
            return;
        }

        final AlphaAnimation alphaAnimation = new AlphaAnimation(FROM_ALPHA, TO_ALPHA);
        alphaAnimation.setDuration(FADE_TIME_IN_MILLISECONDS);
        delayView.startAnimation(alphaAnimation);
        final AnimationListener animationListener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                viewGroup.removeView(delayView);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        };
        alphaAnimation.setAnimationListener(animationListener);
        alphaAnimation.setFillAfter(true);

        TLLog.debug("delay image: image hidden");
    }

    /**
     * Shows the image at the start of the delay and saves the instance of the image
     *
     * @param activity
     * @param drawable
     */
    private void showDelayImage(final Activity activity, final Drawable drawable) {
        final View view = activity.findViewById(android.R.id.content);
        if (view == null) {
            return;
        }

        final ViewGroup viewGroup = (ViewGroup) view.getRootView();
        if (viewGroup == null) {
            return;
        }
        final int delayViewId = ViewUtils.getDelayViewId();
        final boolean alreadyHasTaplyticsDelayView = viewGroup.findViewById(delayViewId) != null;
        if (alreadyHasTaplyticsDelayView) {
            return;
        }
        // Create an arbitrary layout, fill the screen with it;
        final ImageView delayLoadView = ViewUtils.getDelayLoadView(drawable);
        if (delayLoadView == null) {
            TLLog.debug("delay image: view is null");
            return;
        }

        // Put the view on top of all the other views
        viewGroup.addView(delayLoadView);
        TLLog.debug("delay image: image added");

    }

    /**
     * Runs the image delay if permissions for delay are not disabled.
     *
     * @param activity
     * @param drawable
     */
    private void runShowDelayImageOnUiThread(final Activity activity, final Drawable drawable) {
        if (activity == null || drawable == null || isDelayDisabled()) {
            return;
        }

        final Runnable showDelayImageRunnable = new Runnable() {
            @Override
            public void run() {
                showDelayImage(activity, drawable);
            }
        };
        TLThreadManager.getInstance().runOnUiThread(showDelayImageRunnable);
    }

    /**
     * Helper method to determine if delay is disabled
     *
     * @return true if delay is disabled, false otherwise
     */
    private boolean isDelayDisabled() {
        return TLUtils.isDisabled(Functionality.DELAYLOAD);
    }

    // Interfaces

    @Override
    public void startDelay() {

    }

    @Override
    public void delayComplete() {
        hideDelayImage();
    }

    // Testing

    synchronized static void setInstance(final TLDelayLoadImageManager loadImageManager) {
        delayLoadImageManagerInstance = loadImageManager;
    }


}