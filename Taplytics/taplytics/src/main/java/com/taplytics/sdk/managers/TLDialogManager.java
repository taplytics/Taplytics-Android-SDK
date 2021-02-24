/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.managers;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.taplytics.sdk.Taplytics;
import com.taplytics.sdk.TaplyticsResetUserListener;
import com.taplytics.sdk.datatypes.TLProperties;
import com.taplytics.sdk.network.TLSocketManager;
import com.taplytics.sdk.resources.Colors;
import com.taplytics.sdk.utils.TLLog;
import com.taplytics.sdk.utils.ViewUtils;
import com.taplytics.sdk.utils.promises.Promise;
import com.taplytics.sdk.utils.promises.PromiseListener;

import org.json.JSONObject;

/**
 * Created by VicV on 6/26/15.
 * <p/>
 * A special manager just for dialogs.
 * Important to be a stateful manager because dialogs leak way too easily.
 */
public class TLDialogManager {

    private AlertDialog fromDialog, experimentDialog, variationDialog, featureFlagDialog, pushDialog;


    private static TLDialogManager instance = null;

    public static TLDialogManager getInstance() {
        if (instance == null) {
            instance = new TLDialogManager();
        }
        return instance;
    }

    /**
     * Ridiculously ugly method that will show the experiment dialog for you *
     */

    private void showExperiments() {
        if (TLManager.getInstance().getTlProperties() != null) {
            final TLProperties props = TLManager.getInstance().getTlProperties();
            experimentDialog = ViewUtils.getListDialogBuilder(TLManager.getInstance().getCurrentActivity(), "Experiments", props.getExperimentNames(true),
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface expDialog, final int exp) {
                            try {
                                // Generate Variation Dialog
                                final String experimentName = ((String) experimentDialog.getListView().getItemAtPosition(exp)).replaceAll(" \\(draft\\)", "").replaceAll(" \\(active\\)", "");
                                variationDialog = ViewUtils.getListDialogBuilder(TLManager.getInstance().getCurrentActivity(), "Variation",
                                        props.getVariationNames(experimentName), new DialogInterface.OnClickListener() {

                                            @Override
                                            public void onClick(DialogInterface varDialog, int var) {
                                                try {
                                                    if (!TLViewManager.getInstance().getBorderMode().equals(ViewUtils.BORDER_MODE.DISCONNECT)) {
                                                        TLManager.getInstance().setExperimentLoadTimeout(false);

                                                        JSONObject experiment = props.getExperimentByName(experimentName);
                                                        String var_name = variationDialog.getListView()
                                                                .getItemAtPosition(var).toString();
                                                        JSONObject variation = props.getVariationByName(experiment, var_name);

                                                        // Switch the variation
                                                        TLManager.getInstance().switchVariation(
                                                                experiment.optString("_id"),
                                                                var_name.equals("baseline") ? var_name : variation
                                                                        .optString("_id"), experimentName, var_name);
                                                    } else {
                                                        if (TLManager.getInstance().getCurrentActivity() != null && TLManager.getInstance().isActivityActive()) {
                                                            Toast toast = Toast
                                                                    .makeText(
                                                                            TLManager.getInstance().getCurrentActivity(),
                                                                            "No connection to Taplytics. Please check your internet connection or restart the app.",
                                                                            Toast.LENGTH_LONG);
                                                            TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
                                                            if (v != null)
                                                                v.setGravity(Gravity.CENTER);
                                                            toast.show();
                                                        }

                                                    }
                                                } catch (Exception third) {
                                                    failNicely("third", third);
                                                }
                                            }
                                        }).create();
                                variationDialog.setOnKeyListener(new Dialog.OnKeyListener() {

                                    @Override
                                    public boolean onKey(DialogInterface arg0, int keyCode, KeyEvent event) {
                                        try {
                                            if (keyCode == KeyEvent.KEYCODE_BACK) {
                                                variationDialog.dismiss();
                                                experimentDialog.show();
                                            }
                                        } catch (Exception fourth) {
                                            TLLog.error("Error clicking dialog");
                                        }
                                        return true;
                                    }
                                });
                                variationDialog.show();
                                experimentDialog.dismiss();
                                experimentDialog = null;
                                ViewUtils.moveDialog(variationDialog, Gravity.BOTTOM);
                            } catch (Exception second) {
                                failNicely("second", second);
                            }
                        }
                    }).create();
            experimentDialog.dismiss();
            experimentDialog.show();

            fromDialog.dismiss();
            fromDialog = null;
            ViewUtils.moveDialog(experimentDialog, Gravity.BOTTOM);
        } else {
            try {
                if (TLManager.getInstance().getCurrentActivity() != null && TLManager.getInstance().isActivityActive()) {
                    Toast toast = Toast
                            .makeText(
                                    TLManager.getInstance().getCurrentActivity(),
                                    "No experiments found. If you have created an experiment, check your api key and internet connection.",
                                    Toast.LENGTH_LONG);
                    TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
                    if (v != null)
                        v.setGravity(Gravity.CENTER);
                    toast.show();
                }
            } catch (Exception e) {
                TLLog.error("something dialog", e);// This is hacky so I'm just being safe...
            }

        }
    }

    private void showPushDialog() {
        if (TLManager.getInstance().getTlProperties() != null) {
            String[] items = {"Copy/Show Token", "Renew token", "Force save token"};
            pushDialog = ViewUtils.getListDialogBuilder(TLManager.getInstance().getCurrentActivity(), "Push Utils", items,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, final int item) {
                            try {
                               switch(item) {
                                   case 0: //copy/show
                                       TLManager.getInstance().getTlPushManager().logAndCopyToken();
                                       break;
                                   case 1: //delete n refresh
                                       TLManager.getInstance().getTlPushManager().deleteAndRenewToken();
                                       break;
                                   case 2: //FORCE a save of the current token.
                                       TLManager.getInstance().getTlPushManager().forceSavePushToken();
                                       break;
                               }

                            } catch (Exception e) {
                                TLLog.error("push error", e);
                            }
                        }
                    }).create();
        }
        ViewUtils.moveDialog(pushDialog, Gravity.BOTTOM);
        pushDialog.show();
    }

    private void showFeatureFlags() {
        if (TLManager.getInstance().getTlProperties() != null) {
            final TLProperties props = TLManager.getInstance().getTlProperties();
            featureFlagDialog = ViewUtils.getListDialogBuilder(TLManager.getInstance().getCurrentActivity(), "Feature Flags", props.getFeatureFlagNames(),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, final int ff) {
                            try {
                                final String flagName = featureFlagDialog.getListView().getItemAtPosition(ff).toString().replaceAll(" \\(draft\\)", "").replaceAll(" \\(active\\)", "");
                                final JSONObject flag = props.getFeatureFlagByName(props.getFeatureFlags(), flagName);
                                final String flagKey = flag.optString("key");
                                final String flagId = flag.optString("_id");

                                // Switch the feature flag
                                TLManager.getInstance().switchFeatureFlag(flagName, flagKey, flagId);

                            } catch (Exception e) {
                                TLLog.error("loading feature flag config", e);
                            }
                        }
                    }).create();
        }
        ViewUtils.moveDialog(featureFlagDialog, Gravity.BOTTOM);
        featureFlagDialog.show();
    }

    public void setupExperimentDialog() {
        try {
            //TODO: Break up. I mean, look at this shit lol.
            //TODO: The references to current activity may need to be updated now that it's a weak reference
            if (TLManager.getInstance().getCurrentActivity() != null) {
                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        final String[] InitialOptions = {"Show Experiments", "Show Feature Flags", "Clear", (TLViewManager.getInstance().bordersEnabled() ? "Disable" : "Enable") + " Borders", "Reset User",  "Push Utils"};

                        try {
                            if (fromDialog != null && !ViewUtils.dialogShowing(fromDialog)) {
                                fromDialog = null;
                            }
                            if (TLManager.getInstance().getCurrentActivity() != null && TLManager.getInstance().isActivityActive() && fromDialog == null) {
                                fromDialog = ViewUtils.getListDialogBuilder(TLManager.getInstance().getCurrentActivity(), "Taplytics", InitialOptions, new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        try {
                                            // Generate Experiment dialog
                                            switch (which) {
                                                case 0:
                                                    showExperiments();
                                                    break;
                                                case 1:
                                                    showFeatureFlags();
                                                    break;
                                                case 2:
                                                    //This is the "clear"
                                                    //Basically we get rid of everything and just go back to whatever the device is bucketed to.
                                                    TLViewManager.getInstance().removeBorder();
                                                    TLViewManager.getInstance().setTestingExperiment(false);

                                                    //Add a promise to a getProperties call and ask for null.
                                                    Promise<Object> tlPropertiesPromise = new Promise<>();
                                                    tlPropertiesPromise.add(new PromiseListener<Object>() {

                                                        @Override
                                                        public void succeeded() {
//                                                            //Just restart the activity flat out. No remorse.
                                                            Activity activity = TLManager.getInstance().getCurrentActivity();
//                                                            activity.recreate();
                                                            Intent intent = activity.getIntent();
                                                            activity.finish();
                                                            activity.overridePendingTransition(0, 0);
                                                            activity.startActivity(intent);
                                                            activity.overridePendingTransition(0, 0);

                                                            super.succeeded();
                                                        }
                                                    });

                                                    TLManager.getInstance().getPropertiesFromServer(null, tlPropertiesPromise);

                                                    break;
                                                case 3:
                                                    //Disable/Enable the border!
                                                    boolean newSetting = !TLViewManager.getInstance().bordersEnabled();
                                                    TLViewManager.getInstance().setBordersEnabled(newSetting);
                                                    if (!newSetting) {
                                                        TLViewManager.getInstance().removeBorder();
                                                    } else {
                                                        TLViewManager.getInstance().applyTaplyticsBorder();
                                                    }
                                                    break;
                                                case 4:
                                                    Taplytics.resetAppUser(new TaplyticsResetUserListener() {
                                                        @Override
                                                        public void finishedResettingUser() {
                                                            Toast.makeText(TLManager.getInstance().getCurrentActivity(), "Done resetting user",Toast.LENGTH_SHORT).show();;
                                                        }
                                                    });
                                                    break;
                                                case 5:
                                                    showPushDialog();
                                                    break;
                                                default:
                                                    break;
                                            }
                                        } catch (Exception first) {
                                            failNicely("first", first);
                                        }
                                    }
                                }).create();

                                //Put the dialog on the bottom of the screen. Only because thats how it is on iOS.
                                ViewUtils.moveDialog(fromDialog, Gravity.BOTTOM);
                                fromDialog.dismiss();
                                fromDialog.show();

                                //Add the extra Taplytics info to the dialog.
                                insertExtraInfo();

                            }
                        } catch (Exception e) {
                            TLLog.error("dialog err", e);
                        }
                    }
                });
            }
        } catch (Exception e) {
            TLLog.error("dialog err", e);
        }

    }

    private void insertExtraInfo() {
        try {
            //This beautiful block of code just reaches inside the dialog generated by Android and finds the layout containing the title.
            ViewGroup vg = (ViewGroup) ((ViewGroup) ((ViewGroup) ((ViewGroup) ((FrameLayout) fromDialog.getWindow().getDecorView()).getChildAt(0)).getChildAt(0)).getChildAt(0)).getChildAt(0);
            LinearLayout layout;

            //Before android 5.0, its the second child. But 4.4.4+ its the second we want.
            if (vg.getChildAt(0) instanceof LinearLayout) {
                layout = (LinearLayout) vg.getChildAt(0);
            } else if (vg.getChildAt(1) instanceof LinearLayout) {
                layout = (LinearLayout) vg.getChildAt(1);
            } else {
                return;
            }

            //Switch the layout to be vertical because its horizontal for some reason.
            layout.setOrientation(LinearLayout.VERTICAL);

            //Add our connectivity information to the dialog.
            final Activity currentActivity = TLManager.getInstance().getCurrentActivity();
            if (currentActivity == null) return;

            TextView view = new TextView(currentActivity);
            String connectivity = "  Connectivity: ";
            if (TLManager.getInstance().isLiveUpdate() && TLSocketManager.getInstance().isConnected) {
                connectivity += "connected.";
            } else {
                connectivity += " not connected to Taplytics.";
                view.setTextColor(Colors.getBorderColor(ViewUtils.BORDER_MODE.DISCONNECT));
            }
            TextView info = new TextView(currentActivity);
            info.setText("This pop-up will only appear on devices with debug builds, or on release build devices previously connected to Taplytics in debug. \n");
            info.setTextSize(9);
            layout.addView(info, 2);
            view.setText(connectivity);
            layout.addView(view, 3);

            //Add the experiment info if we have any. Just because. May remove later and only add if borders are disabled.
            if (TLViewManager.getInstance().isTestingExperiment()) {
                TextView view2 = new TextView(currentActivity);
                TextView view3 = new TextView(currentActivity);
                if (TLViewManager.getInstance().getExperimentName() != null && !TLViewManager.getInstance().getExperimentName().equals("")) {
                    view2.setText("  Experiment: " + TLViewManager.getInstance().getExperimentName());
                    layout.addView(view2, 4);
                }
                if (TLViewManager.getInstance().getVariationName() != null && !TLViewManager.getInstance().getVariationName().equals("")) {
                    view3.setText("  Variation: " + TLViewManager.getInstance().getVariationName());
                    layout.addView(view3, 5);
                }
            }
        } catch (Exception e) {
            TLLog.error("Error adding extra info to dialog", e);
        }
    }

    /**
     * There is probably a case where generating dialogs break, so we want to fail out nicely with a Toast.
     *
     * @param which
     * @param e
     */
    private void failNicely(String which, Exception e) {
        TLLog.error("dialog err " + which, e);
        final Activity currentActivity = TLManager.getInstance().getCurrentActivity();
        if (TLManager.getInstance().isActivityActive() && currentActivity != null) {
            try {
                Toast toast = Toast.makeText(currentActivity,
                        "There was an error creating your experiment list. Please contact taplytics for help.",
                        Toast.LENGTH_LONG);

                TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
                if (v != null)
                    v.setGravity(Gravity.CENTER);
                toast.show();
            } catch (Exception superError) {
                //Toast broke.
            }
        }
        dismissAllDialogs();
    }

    public void dismissAllDialogs() {
        try {
            if (fromDialog != null && fromDialog.isShowing()) {
                fromDialog.dismiss();
                fromDialog = null;
            }
            if (experimentDialog != null && (fromDialog != null && fromDialog.isShowing())) {
                experimentDialog.dismiss();
                experimentDialog = null;
            }
            if (variationDialog != null && variationDialog.isShowing()) {
                variationDialog.dismiss();
                variationDialog = null;
            }
        } catch (Exception e) {
            TLLog.error("problem killing dialogs", e);
        }
    }

    public boolean dialogsShowing() {
        return (ViewUtils.dialogShowing(fromDialog) || ViewUtils.dialogShowing(experimentDialog) || ViewUtils.dialogShowing(variationDialog));
    }
}
