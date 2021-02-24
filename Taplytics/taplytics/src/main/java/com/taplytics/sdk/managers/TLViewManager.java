/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.managers;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.taplytics.sdk.Taplytics;
import com.taplytics.sdk.datatypes.TLProperties;
import com.taplytics.sdk.datatypes.Triplet;
import com.taplytics.sdk.network.TLNetworking;
import com.taplytics.sdk.network.TLSocketManager;
import com.taplytics.sdk.resources.BorderFrameLayout;
import com.taplytics.sdk.resources.TLBorderShape;
import com.taplytics.sdk.utils.Functionality;
import com.taplytics.sdk.utils.ScreenshotUtils;
import com.taplytics.sdk.utils.TLLog;
import com.taplytics.sdk.utils.TLUtils;
import com.taplytics.sdk.utils.ViewUtils;
import com.taplytics.sdk.utils.promises.PromiseListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import static com.taplytics.sdk.utils.ViewUtils.BORDER_MODE;
import static com.taplytics.sdk.utils.ViewUtils.attachInitialListeners;
import static com.taplytics.sdk.utils.ViewUtils.getBorderFrame;
import static com.taplytics.sdk.utils.ViewUtils.getBorderTextViewLayout;
import static com.taplytics.sdk.utils.ViewUtils.getTouchscreen;
import static com.taplytics.sdk.utils.ViewUtils.getViewJSONObject;
import static com.taplytics.sdk.utils.ViewUtils.getViewsAtPoint;
import static com.taplytics.sdk.utils.ViewUtils.isBorderShowing;
import static com.taplytics.sdk.utils.ViewUtils.setProperties;

public class TLViewManager {

    /**
     * Whether or not the images are currently being uploaded. Used to squelch touch events if something is already being uploaded.
     */
    private boolean uploading;

    /**
     * Similar to uploading, however, this is used to determine whether or not to display the uploading text. Different thread from the uploading boolean at times.
     */
    private boolean loading;

    /**
     * User-set touchscreen overlay hide boolean. Basically, the overlay can interfere with some APIs (youtube), this lets you switch it off.
     * <p/>
     * Use: {@link Taplytics#overlayOff()} & {@link Taplytics#overlayOn()}
     */
    private boolean hideOverlay = false;


    /**
     * This indicates where or not Taplytics will enforce view changes. IE if the dashboard has a view set to invisible, it will not be allowed to be set to visible by code.
     * Similar with text changes. More to come maybe.
     */
    private boolean aggressiveViewChanges = false;

    /**
     * Whether or not an experiment is currently active at all and we are in liveUpdate.
     */
    private boolean isTestingExperiment;

    public TLViewManager() {
        try {
            final Context appContext = TLManager.getInstance().getAppContext();
            final PackageManager packageManager = appContext.getPackageManager();
            applicationIconId = packageManager.getApplicationInfo(appContext.getPackageName(), PackageManager.GET_META_DATA).icon;
        } catch (PackageManager.NameNotFoundException e) {
            TLLog.error("Name not found", e);
        }

        //Grab an instance of the class so we don't waste time turning it into a string every time.
        try {
            youtubeClass = Class.forName("com.google.android.youtube.player.YouTubePlayerView");
        } catch (Exception e) {
            //No youtube package available;
        }
    }

    boolean isTestingExperiment() {
        return isTestingExperiment;
    }

    /**
     * Boolean for enabling / disabling the borders. I don't know why, but Cobi says people want it.
     */
    private boolean bordersEnabled = true;

    /**
     * Whether or not a youtube playback is present. If yes, dont apply the touchscreen.
     */
    private boolean youtubePresent = false;

    public Class getYoutubeClass() {
        return youtubeClass;
    }

    private Class youtubeClass = null;

    /**
     * @return {@link #viewsToReset}
     */
    public HashMap getViewsToReset() {
        return viewsToReset;
    }

    /**
     * A map of views that need to be reset back to their baseline.
     */
    private HashMap viewsToReset = new HashMap();



    private BORDER_MODE borderMode = BORDER_MODE.EXPERIMENT;

    public BORDER_MODE getBorderMode() {
        return borderMode;
    }

    /**
     * Set whether an experiment is being tested right now.
     *
     * @param isTestingExperiment is it? is it being tested?
     */
    public void setTestingExperiment(boolean isTestingExperiment) {
        this.isTestingExperiment = isTestingExperiment;
    }

    /**
     * Experiment name.
     */
    private String name;

    /**
     * Variation name
     */
    private String variation;


    public String getExperimentName() {
        return name;
    }

    public String getVariationName() {
        return variation;
    }

    /**
     * The full jsonObject of the views under the clicked area.
     */
    private JSONObject chooseViewObj;

    /**
     * @return {@link #chooseViewObj}
     */
    public JSONObject getChooseViewObj() {
        return chooseViewObj;
    }

    /**
     * The full JSONObject for the chosen activity for an activity goal.
     */
    private JSONObject chooseActivityObj;

    //For tests
    public static void setInstance(TLViewManager instance) {
        TLViewManager.instance = instance;
    }

    private static TLViewManager instance;

    public static TLViewManager getInstance() {
        if (instance != null) {
            return instance;
        } else {
            instance = new TLViewManager();
        }
        return instance;
    }

    /**
     * @return current ViewGroup
     */
    public ViewGroup getCurrentViewGroup() {
        return currentViewGroupWeakRef != null ? currentViewGroupWeakRef.get() : null;
    }

    /**
     * The current ViewGroup that is active. This DOES NOT account for changes in setContentView, it simply finds the view applied right as the activity is started. This ViewGroup is what all of our visual changes are applied to.
     */
    private WeakReference<ViewGroup> currentViewGroupWeakRef = null;

    /**
     * A map of the images waiting to be sent up to the server. We load them up in a hashmap because we want to wait until we've collected them all to send them.
     */
    private Map<String, byte[]> imagesToSend = new HashMap<>();

    /**
     * The offset in pixels that the statusbar provides. Needed because the Y value will be skewed otherwise.
     */
    private long statusBarOffset = -1;

    /**
     * This is the icon id for the application. Its guaranteed to exist, and, it is used as
     * an id when Android requires it (such as view tags)
     */
    private int applicationIconId;

    /**
     * Sets the current viewgroup to the view that is currently placed on the screen.
     */
    public void setCurrentViewGroup(ViewGroup viewGroup) {
        currentViewGroupWeakRef = new WeakReference<>(viewGroup);
        youtubePresent = false;
        attachInitialListeners(viewGroup, false);

        try {
            //If we ARE STILL WAITING for tlproperties, listen to the promise and update the views once we get them.
            TLManager.getInstance().getTlPropertiesPromise().add(new PromiseListener() {
                @Override
                public void succeeded() {
                    updateViewsIfNecessary();
                    super.succeeded();
                }

                @Override
                public void failedOrCancelled(Exception e) {
                    if (e != null) {
                        TLLog.error("Get TLProperties failed or cancelled: ", e);
                    }
                    updateViewsIfNecessary();
                    super.failedOrCancelled(e);
                }
            });

            viewGroup.post(new Runnable() {
                @Override
                public void run() {
                    setStatusBarOffset();
                }
            });
        } catch (Exception e) {
            TLLog.error("setcurrentviewgroup err", e);
        }

		/*
         * This is necessary. If we've enabled the border on one activity, switched to another, closed the border, then came back to the
		 * original activity, we need to be sure to clear it.
		 */
        if (isTestingExperiment) {
            applyTaplyticsBorder();
        } else if (borderMode != BORDER_MODE.EXPERIMENT) {
            removeBorder();
        }

        if (!youtubePresent && !hideOverlay) {
            applyTouchScreenIfViewPresentInActivity();
        }
    }

    void applyTouchScreenIfViewPresentInActivity() {
        try {
            Activity currentActivity = TLManager.getInstance().getCurrentActivity();
            TLProperties props = TLManager.getInstance().getTlProperties();
            if (currentActivity != null && props != null && props.getActivityMap() != null) {
                if (props.getActivityMap().containsValue(currentActivity.getClass().getSimpleName())) {
                    applyTouchscreen(getCurrentViewGroup(), false);
                }
            }
        } catch (Throwable e) {
            TLLog.error("applying touchscreen", e);
        }
    }

    /**
     * Gather the statusbar offset for final value so we don't have to set it every time it's needed.
     */
    private void setStatusBarOffset() {
        try {
            if (statusBarOffset == -1) {
                Rect rectangle = new Rect();
                Window window = TLManager.getInstance().getCurrentActivity().getWindow();
                window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
                statusBarOffset = rectangle.top;
            }
        } catch (Exception e) {
            TLLog.error("Error setting statusbar offset", e);
        }
    }


    /**
     * This applies the border to the screen to notify clients that they are currently viewing an experiment and variation.
     *
     * @param experimentName name of the experiment being shown.
     * @param variation      name of the variation being shown.
     */
    void applyConnectedBorder(String experimentName, String variation) {
        if (TLManager.getInstance().isLiveUpdate()) {
            if (!variation.equals(this.variation) || !experimentName.equals(this.name)) {
                this.name = experimentName;
                this.variation = variation;
                borderMode = BORDER_MODE.EXPERIMENT;
                updateBorderExperimentText();
            }
            applyTaplyticsBorder();
        }
    }

    /**
     * Check if we need to refresh all the views with new info!
     */
    public void updateViewsIfNecessary() {
        try {
            ViewGroup vg = getCurrentViewGroup();
            if (vg != null) {

                if (TLManager.getInstance().isLiveUpdate()) {
                    TLLog.debug(ViewUtils.getViewHierarchy(vg));
                }

                if (TLManager.getInstance().isLiveUpdate()
                        && isBorderShowing(vg)
                        && variation != null
                        && TLManager.getInstance().getTrackingEnabled()
                        && !TLUtils.isDisabled(Functionality.VISUAL)) {
                    updateBorderExperimentText();
                }
                setProperties(vg);

                //Visual
                TLDelayLoadImageManager.getInstance().updateDelayLoad();
            }
        } catch (JSONException e) {
            TLLog.error("Err: setprp", e);
        }
    }

    void updateDialogsIfNecessary() {
        if (!TLUtils.isDisabled(Functionality.DIALOGS)) {
            try {
                //dialogs
                for (ViewGroup vg : ViewUtils.getAllOtherParentViewGroups()) {
                    ViewUtils.setProperties(vg);
                }
            } catch (Throwable e) {
                TLLog.error("Error updating dialog", e);
            }
        }
    }




    /**
     * We have a new experiment or variation, so we need to change the border text.
     */
    private void updateBorderExperimentText() {
        try {
            Activity currentActivity = TLManager.getInstance().getCurrentActivity();
            if (TLManager.getInstance().isLiveUpdate()
                    && currentActivity != null
                    && TLManager.getInstance().isActivityActive()) {
                currentActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final ViewGroup vg = getCurrentViewGroup();
                        if (vg == null) return;

                        if (isBorderShowing(vg)
                                && vg.findViewById(TLBorderShape.getBorderId()).getTag().equals(BORDER_MODE.EXPERIMENT.name())) {
                            if (name == null) {
                                name = "waiting..";
                            }
                            if (variation == null) {
                                variation = "waiting...";
                            }
                            ((TextView) vg.findViewById(TLBorderShape.getTopTextId())).setText(("Experiment: " + name));
                            ((TextView) vg.findViewById(TLBorderShape.getBottomTextId())).setText("Variation: " + variation);
                        }
                    }

                });
            }
        } catch (Throwable e) {
            TLLog.error(getClass().toString(), e);
        }
    }

    /**
     * Enter tap mode.
     * <p/>
     * Makes it so when you click elements, it turns them into jsonObjects and sends their info to the server
     */
    public void enterTapMode(final JSONObject chooseViewObj) {
        this.chooseViewObj = chooseViewObj;
        TLManager.getInstance().getTlPropertiesPromise().add(new PromiseListener() {
            @Override
            public void succeeded() {
                borderMode = chooseViewObj.optBoolean("chooseButton") ? BORDER_MODE.BUTTON : BORDER_MODE.TAP;
                applyTaplyticsBorder();
                TLSocketManager.getInstance().emitSocketEvent("enteredTapMode", chooseViewObj);
                applyTaplyticsOverlaysToDialogsIfNecessary();
                super.succeeded();
            }
        });

    }

    /**
     * Hide tap mode and return to normal experiment mode
     */
    public void hideTapMode() {
        this.chooseViewObj = null;
        borderMode = BORDER_MODE.EXPERIMENT;
        removeDialogBordersIfNecessary();
        applyTaplyticsBorder();
    }

    private void removeDialogBordersIfNecessary() {
        for (ViewGroup vg : ViewUtils.getAllOtherParentViewGroups()) {
            final View borderView = vg != null ? vg.findViewById(TLBorderShape.getBorderId()) : null;
            if (vg != null && borderView != null) {
                vg.removeView(borderView);
            }
        }
    }

    /**
     * Apply a border to the screen indicating that it is connected to the taplytics panel.
     */

    void applyTaplyticsBorder() {
        try {
            Activity currentActivity = TLManager.getInstance().getCurrentActivity();
            if (TLManager.getInstance().isLiveUpdate()
                    && currentActivity != null
                    && TLManager.getInstance().isActivityActive()
                    && bordersEnabled) {
                currentActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // Create an arbitrary layout, fill the screen with it, and give it a border.
                            final ViewGroup vg = getCurrentViewGroup();

                            final Activity activity = TLManager.getInstance().getCurrentActivity();
                            if (vg == null || activity == null) return;

                            if (!ViewUtils.isBorderShowing(vg)
                                    || (vg.findViewById(TLBorderShape.getBorderId()) != null
                                    && vg.findViewById(TLBorderShape.getBorderId()).getTag() != null
                                    && !vg.findViewById(TLBorderShape.getBorderId()).getTag().equals(borderMode.name()))) {
                                // Remove the old border if it did exist.
                                removeBorder();
                                BorderFrameLayout border = getBorderFrame(borderMode);

                                // FrameLayout doesn't allow view stacking. Make the only child a linearLayout.
                                border.addView(getBorderTextViewLayout(activity, borderMode, name, variation));
                                if (borderMode.equals(BORDER_MODE.TAP) || borderMode.equals(BORDER_MODE.BUTTON)) {
                                    border.setOnTouchListener(getViewTouchListener(getCurrentViewGroup()));
                                }

                                // Put the view on top of all the other views
                                vg.addView(border);
                            } else {
                                if (borderMode.equals(BORDER_MODE.EXPERIMENT) && name != null && variation != null && !name.equals("null")
                                        && !variation.equals("null")) {
                                    updateBorderExperimentText();

                                }
                            }
                        } catch (Throwable e) {
                            //For code continuity sake, just adding a try/catch here.
                            TLLog.error("Problem applying the border", e);
                        }
                    }
                });

                if (borderMode.equals(BORDER_MODE.EXPERIMENT)) {
                    isTestingExperiment = true;
                    TLManager.getInstance().setExperimentLoadTimeout(false);
                }
            } else if (!bordersEnabled) {
                removeBorder();
            }
        } catch (Throwable e) {
            TLLog.error("app bd", e);
        }
    }

    public void applyDisconnectBorder() {
        borderMode = BORDER_MODE.DISCONNECT;
        applyTaplyticsBorder();
    }

    public void updateDisconnectBorder(final int attempts) {
        if (!TLManager.getInstance().isLiveUpdate()) return;

        final ViewGroup vg = getCurrentViewGroup();
        final Activity currentActivity = TLManager.getInstance().getCurrentActivity();
        if (vg != null && currentActivity != null
                && TLManager.getInstance().isActivityActive()
                && borderMode.equals(BORDER_MODE.DISCONNECT)
                && bordersEnabled) {
            currentActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        TextView top = (TextView) vg.findViewById(TLBorderShape.getTopTextId());
                        TextView bottom = (TextView) vg.findViewById(TLBorderShape.getBottomTextId());
                        top.setText(TLBorderShape.BORDER_TOP_DISCONNECT_NUMBER + " Reconnect attempt: " + String.valueOf(attempts));
                        if (attempts > 6) {
                            bottom.setText(TLBorderShape.BORDER_BOTTOM_RECONNECT_THIRD);
                        }
                    } catch (Throwable e) {
                        //border fail
                        TLLog.error("Error updating text", e);
                    }
                }
            });
        }
    }

    /**
     * Apply an overlay to the screen to capture touch events
     */
    private void applyTouchscreen(final ViewGroup vg, final boolean dialog) {
        final Activity currentActivity = TLManager.getInstance().getCurrentActivity();
        if (vg != null && currentActivity != null && TLManager.getInstance().isActivityActive()) {
            currentActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    // Create an arbitrary layout, fill the screen with it;
                    if (vg.findViewById(TLBorderShape.getTouchscreenId()) == null) {

                        // Put the view on top of all the other views
                        vg.addView(getTouchscreen(dialog), vg.getChildCount());
                    }
                }
            });
        }
    }

    /**
     * Remove the touch events screen.
     */

    private void removeTouchScreen() {
        final Activity currentActivity = TLManager.getInstance().getCurrentActivity();
        final ViewGroup vg = getCurrentViewGroup();
        if (vg != null && currentActivity != null && TLManager.getInstance().isActivityActive()) {
            currentActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Remove the touchscreen if it exists.
                    View touchscreen = vg.findViewById(TLBorderShape.getTouchscreenId());
                    if (touchscreen != null) {
                        try {
                            vg.removeView(touchscreen);
                        } catch (Throwable e) {
                            //Super safety for wattpad. March 21 2016.
                        }
                    }
                }

            });
        }
    }


    /**
     * Special listener that is turned on when selecting views. Uploads images and touched view info to Taplytics.
     */
    private View.OnTouchListener getViewTouchListener(final ViewGroup vg) {

        return new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, final MotionEvent event) {

                try {
                    if (!uploading && vg != null) {
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            ViewGroup currentViewGroup = getCurrentViewGroup();
                            final BorderFrameLayout border = (BorderFrameLayout) currentViewGroup.findViewById(TLBorderShape.getBorderId());
                            final TextView topView = (TextView) border.findViewById(TLBorderShape.getTopTextId());
                            final TextView bottomView = (TextView) border.findViewById(TLBorderShape.getBottomTextId());
                            if (topView != null && bottomView != null) {

                                topView.setText("Uploading view...");
                                bottomView.setText("");

                                final Handler timedHandler = new Handler();
                                final Handler errorHandler = new Handler();

                                Runnable testRunnable = new Runnable() {
                                    @Override
                                    public void run() {

                                        uploading = true;
                                        loading = true;

                                        final Runnable r = new Runnable() {
                                            public void run() {
                                                if (loading) {
                                                    if (bottomView.getText().length() > 5) {
                                                        bottomView.setText("");
                                                    }
                                                    bottomView.append(".");
                                                    timedHandler.postDelayed(this, 100);
                                                }
                                            }
                                        };

                                        timedHandler.post(r);

                                        JSONArray views = getJSONViewsAtPoint(event.getX(), event.getY(), vg);

                                        TLManager.getInstance().getTlNetworking().postTapViewElements(views, new TLNetworking.TLNetworkResponseListener() {
                                            @Override
                                            public void onResponse(JSONObject response) {
                                                TLLog.debug("Sent view to server.");
                                                topView.setText(TLBorderShape.getTopText(borderMode));
                                                bottomView.setText(TLBorderShape.getBottomText(borderMode));
                                                border.clearCircle();
                                                uploading = false;
                                                loading = false;
                                            }

                                            @Override
                                            public void onError(Throwable error) {
                                                loading = false;

                                                topView.setText("There has been an error uploading your view.");
                                                bottomView.setText("Please try again.");
                                                final Runnable errorRun = new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        uploading = false;
                                                        border.clearCircle();

                                                        topView.setText(TLBorderShape.getTopText(borderMode));
                                                        bottomView.setText(TLBorderShape.getBottomText(borderMode));
                                                    }
                                                };
                                                errorHandler.postDelayed(errorRun, 1700);
                                            }
                                        }, null);
                                    }
                                };

                                timedHandler.postDelayed(testRunnable, 50);

                            }
                        }

                    }
                } catch (Throwable e) {
                    TLLog.error("TouchListener error", e);
                }
                return false;
            }

        };
    }

    /**
     * Get a JSONArray of all the views at the given point.
     */
    private JSONArray getJSONViewsAtPoint(float x, float y, ViewGroup vg) {
        JSONArray views = new JSONArray();
        try {
            for (View view : getViewsAtPoint(x, y, vg)) {
                JSONObject viewJSON = getViewJSONObject(view);
                if (viewJSON != null) {
                    views.put(viewJSON);
                }
            }
        } catch (Throwable e) {
            TLLog.error("Err: vw jsn. ", e);
        }
        return views;
    }

    /**
     * Completely removes the border if possible.
     */
    public void removeBorder() {
        try {
            final Activity currentActivity = TLManager.getInstance().getCurrentActivity();
            if (TLManager.getInstance().isLiveUpdate()
                    && currentActivity != null
                    && TLManager.getInstance().isActivityActive()) {
                currentActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final ViewGroup vg = getCurrentViewGroup();
                            final View borderView = vg != null ? vg.findViewById(TLBorderShape.getBorderId()) : null;
                            if (vg != null && borderView != null) {
                                vg.removeView(borderView);
                            }
                        } catch (Throwable e) {
                            TLLog.error("problem removing border", e);
                        }
                    }


                });
            }
        } catch (Throwable e) {
            TLLog.error("rem bd", e);
        }
    }

    /**
     * Enter the mode to pick the activity for a goal. Basically, change the colour and text of the border.
     *
     * @param obj //
     */
    public void enterActivityMode(JSONObject obj) {
        if (obj != null) {
            borderMode = BORDER_MODE.ACTIVITY;
            applyTaplyticsBorder();
            chooseActivityObj = obj;
            TLSocketManager.getInstance().emitSocketEvent("enteredPickViewMode", obj);
        }
    }

    /**
     * Once the activity has been chosen, we make a fancy object to send up to Taplytics and clear the border*
     */
    public void getFoundActivity() {
        if (chooseActivityObj == null)
            return;
        Activity currentActivity = TLManager.getInstance().getCurrentActivity();
        ViewGroup vg = getCurrentViewGroup();
        if (currentActivity == null || vg == null)
            return;

        try {
            chooseActivityObj.put("viewKey", currentActivity.getClass().getSimpleName());
            String imgFileName = ScreenshotUtils.captureScreenshot(vg.getChildAt(0));
            chooseActivityObj.put("imgFileName", imgFileName);
            JSONArray fragments = new JSONArray();
            for (Map.Entry<Object, Triplet<String, String, Boolean>> entry : TLFragmentManager.getInstance().getFragmentsOnScreen().entrySet()) {
                Triplet value = entry.getValue();
                fragments.put(value.second);
            }
            chooseActivityObj.put("fragments", fragments);
            chooseActivityObj.put("isAndroid", true);
            TLManager.getInstance().getTlNetworking().postChosenActivity(chooseActivityObj, new TLNetworking.TLNetworkResponseListener() {
                @Override
                public void onResponse(JSONObject response) {
                    TLLog.debug("Posted Found Activity");
                }

                @Override
                public void onError(Throwable e) {
                    TLLog.error("Posting Found Activity", e);
                }
            });
        } catch (Throwable e) {
            TLLog.error("Getting Found Activity", e);
        } finally {
            chooseActivityObj = null;
        }
        hideActivityMode();
    }

    /**
     * Change the mode from activity choose mode back to experiment mode
     */
    private void hideActivityMode() {
        borderMode = BORDER_MODE.EXPERIMENT;
        applyTaplyticsBorder();
    }

    /**
     * @return {@link #imagesToSend} and clear it *
     */
    public Map<String, byte[]> getImagesToSend() {
        Map<String, byte[]> sending = new HashMap<>(imagesToSend);
        imagesToSend.clear();
        return sending;
    }

    /**
     * Add to {@link #imagesToSend}
     *
     * @param name  filename of the image
     * @param bytes image bytes
     */
    public void addToImages(String name, byte[] bytes) {
        imagesToSend.put(name, bytes);
    }











    boolean bordersEnabled() {
        return bordersEnabled;
    }

    void setBordersEnabled(boolean bordersEnabled) {
        this.bordersEnabled = bordersEnabled;
    }

    public void hideDisconnectBorder() {
        borderMode = BORDER_MODE.EXPERIMENT;
        if (isTestingExperiment && TLManager.getInstance().isLiveUpdate() && bordersEnabled()) {
            applyTaplyticsBorder();
        } else {
            removeBorder();
        }
    }

    void addUpdatedToBorder() {
        try {
            //If the current experiment was just updated, add an (updated) to it for 2 seconds.
            ViewGroup vg = getCurrentViewGroup();
            if (TLManager.getInstance().isLiveUpdate()
                    && TLManager.getInstance().isActivityActive()
                    && vg != null
                    && ViewUtils.isBorderShowing(vg)
                    && borderMode == BORDER_MODE.EXPERIMENT
                    && isTestingExperiment) {

                //Grab the textview from the border.
                TextView textView = ((TextView) vg.findViewById(TLBorderShape.getBottomTextId()));

                if (textView != null && !textView.getText().toString().contains("(updated)")) {
                    //Grab the current showing text.
                    final String oldText = textView.getText().toString();
                    //Add our stuff.
                    textView.setText(oldText + " (updated)");
                    textView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                final ViewGroup vg = getCurrentViewGroup();
                                if (vg != null
                                        && TLManager.getInstance().isLiveUpdate()
                                        && TLManager.getInstance().isActivityActive()
                                        && ViewUtils.isBorderShowing(vg)
                                        && borderMode == BORDER_MODE.EXPERIMENT
                                        && isTestingExperiment) {
                                    TextView textView = ((TextView) vg.findViewById(TLBorderShape.getBottomTextId()));
                                    //If it contains oldtext it means its still the same experiment (hasn't changed in 2 seconds).
                                    if (textView.getText().toString().contains(oldText) && textView.getText().toString().contains("(updated)")) {
                                        //Go back to old text (remove updated).
                                        textView.setText(oldText);
                                    }
                                }
                            } catch (Throwable e) {
                                TLLog.error("inner error updating border text error", e);
                            }
                        }
                        //2 second delay
                    }, 2000);
                }
            }
        } catch (Throwable e) {
            TLLog.error("error updating border text", e);
        }
    }

    /**
     * @return {@link #applicationIconId}
     */
    public int getApplicationIconId() {
        return applicationIconId;
    }

    public void setYoutubePresent(boolean youtubePresent) {
        this.youtubePresent = youtubePresent;
    }

    /**
     * Sets {@link #hideOverlay} to off, and removes it if its there.
     */
    public void setOverlayOff() {
        hideOverlay = true;
        removeTouchScreen();
    }

    /**
     * Sets {@link #hideOverlay} to on, and re-applies it.
     */
    public void setOverlayOn() {
        hideOverlay = false;
        applyTouchscreen(getCurrentViewGroup(), false);
    }

    void applyTaplyticsOverlaysToDialogsIfNecessary() {
        if (!TLUtils.isDisabled(Functionality.DIALOGS)) {
            boolean needBorder = TLManager.getInstance().isLiveUpdate() && isTestingExperiment() && isBorderShowing(getCurrentViewGroup());
            for (ViewGroup vg : ViewUtils.getAllOtherParentViewGroups()) {
                try {
                    if (needBorder) {
                        if (vg.findViewById(TLBorderShape.getBorderId()) == null) {
                            vg.addView(ViewUtils.getBorderFrame(BORDER_MODE.NOBORDER));
                            vg.findViewById(TLBorderShape.getBorderId()).setOnTouchListener(getViewTouchListener(vg));
                        }
                    }
                    applyTouchscreen(vg, true);
                } catch (Throwable e) {
                    TLLog.error("a", e);
                }
            }
        }
    }

    public boolean hasAggressiveViewChanges() {
        return aggressiveViewChanges;
    }

    public void setAggressiveViewChanges(boolean aggressiveViewChanges) {
        this.aggressiveViewChanges = aggressiveViewChanges;
    }
}