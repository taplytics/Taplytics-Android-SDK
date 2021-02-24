/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.managers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.os.Build;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.taplytics.sdk.datatypes.Triplet;
import com.taplytics.sdk.utils.FragmentUtils;
import com.taplytics.sdk.utils.Functionality;
import com.taplytics.sdk.utils.TLLog;
import com.taplytics.sdk.utils.TLUtils;
import com.taplytics.sdk.utils.ViewUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static com.taplytics.sdk.utils.FragmentUtils.getFragId;

/**
 * Created by VicV on 11/5/14.
 * <p/>
 * Fragment manager. Put fragment things here and in Fragment Utils.
 */
public class TLFragmentManager {

    private static TLFragmentManager instance;

    public static TLFragmentManager getInstance() {
        if (instance == null) {
            instance = new TLFragmentManager();
        }
        return instance;
    }

    public int getFragmentTrackingId() {
        return FRAGMENT_TRACKING_TAG_ID;
    }

    /**
     * Instead of adding views to the fragments, we'll just set our own tags to it
     **/
    private final int FRAGMENT_TRACKING_TAG_ID;

    private Method viewPagerGetFragmentMethod = null;
    private Field currentPrimaryFragmentField = null;
    private Field currentPrimaryFragmentStateField = null;
    private Field executingTransactionsField = null;

    /**
     * In the event that the client for some reason changes fragment names, our visual editing on fragments wont work as it requires the current fragment classname!
     * <p>
     * If this is set to true, we will ignore the fragment classname and rely solely on the ID.
     */
    private boolean changingFragmentNames = false;

    public boolean isChangingFragmentNames() {
        return changingFragmentNames;
    }

    void setChangingFragmentNames(boolean changingFragmentNames) {
        this.changingFragmentNames = changingFragmentNames;
    }

    public Field getExecutingTransactionsField() {
        return executingTransactionsField;
    }

    public void setExecutingTransactionsField(Field executingTransactionsField) {
        this.executingTransactionsField = executingTransactionsField;
    }

    public boolean hasExecutedTransactions() {
        return hasExecutedTransactions;
    }

    public void setHasExecutedTransactions(boolean hasExecutedTransactions) {
        this.hasExecutedTransactions = hasExecutedTransactions;
    }

    private boolean hasExecutedTransactions = false;

    public HashMap<Object, Triplet<String, String, Boolean>> getFragmentsOnScreen() {
        return fragmentsOnScreen;
    }

    HashSet<String> viewPagerFragments = new HashSet<>();

    public HashSet<String> getViewPagerFragments() {
        return viewPagerFragments;
    }

    private HashMap<Object, Triplet<String, String, Boolean>> fragmentsOnScreen = new HashMap<>();

    public View.OnLayoutChangeListener getOnLayoutChangeListenerForFragment() {
        return onLayoutChangeListenerForFragment;
    }

    public View.OnLayoutChangeListener getOnLayoutChangeListenerForSupportFragment() {
        return onLayoutChangeListenerForSupportFragment;
    }

    public android.app.FragmentManager.OnBackStackChangedListener getBackStackChangedListener() {
        return backStackChangedListener;
    }

    private android.app.FragmentManager.OnBackStackChangedListener backStackChangedListener;

    public FragmentManager.OnBackStackChangedListener getSupportBackStackChangedListener() {
        return supportBackStackChangedListener;
    }

    private FragmentManager.OnBackStackChangedListener supportBackStackChangedListener;


    private View.OnLayoutChangeListener onLayoutChangeListenerForFragment, onLayoutChangeListenerForSupportFragment;

    @SuppressWarnings("NewApi")
    @SuppressLint("NewApi")
    public TLFragmentManager() {
        FRAGMENT_TRACKING_TAG_ID = TLViewManager.getInstance().getApplicationIconId() + 100;
        if (Build.VERSION.SDK_INT >= 11) {

            onLayoutChangeListenerForFragment = new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    try {
                        // Check for the fragment.
                        final Activity currentActivity = TLManager.getInstance().getCurrentActivity();

                        if (currentActivity != null
                                && currentActivity.getFragmentManager().findFragmentById(v.getId()) != null
                                && v instanceof ViewGroup) {
                            boolean isGone = ((left + right == 0) || (bottom + top == 0));
                            boolean wasGone = ((oldLeft + oldRight == 0) || (oldBottom + oldTop == 0));
                            Fragment f = currentActivity.getFragmentManager().findFragmentById(v.getId());
                            if (f != null) {
                                Object tag = f.getTag();
                                if (tag == null) {
                                    tag = (f.getClass().getSimpleName());
                                } else {
                                    tag = tag.toString();
                                }
                                View fragView = f.getView();
                                if (fragView != null && fragView instanceof ViewGroup) {
                                    try {
                                        if (fragView instanceof RecyclerView) {
                                            fragView = (ViewGroup) fragView.getParent();
                                        }
                                    } catch (Throwable t) {
                                        //Don't have recyclers
                                    }
                                    FragmentUtils.checkForFragmentDifferences((ViewGroup) v, (ViewGroup) fragView, getFragId(v), (String) tag, f.getClass().getSimpleName(), isGone, wasGone);
                                }
                            }
                        }
                    } catch (Throwable e) {
                        TLLog.error("F Ex, reg, lc: ", e);
                    }
                }
            };

            onLayoutChangeListenerForSupportFragment = new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    try {
                        if (TLManager.getInstance().getCurrentActivity() instanceof FragmentActivity) {
                            final FragmentManager supportFragmentManager = ((FragmentActivity) TLManager.getInstance().getCurrentActivity()).getSupportFragmentManager();
                            final android.support.v4.app.Fragment currentFragment = supportFragmentManager.findFragmentById(v.getId());

                            Object tag = currentFragment.getTag();

                            if (tag == null) {
                                tag = (currentFragment.getClass().getSimpleName());
                            } else {
                                tag = tag.toString();
                            }
                            View fragView = currentFragment.getView();
                            boolean wasGone = oldLeft == 0 && oldTop == 0 && oldRight == 0 && oldBottom == 0;
                            boolean isGone = ((left + right == 0) || (bottom + top == 0));
                            if (v instanceof ViewGroup && fragView instanceof ViewGroup) {
                                try {
                                    if (fragView instanceof RecyclerView) {
                                        fragView = (ViewGroup) fragView.getParent();
                                    }
                                } catch (Throwable t) {
                                    //Dont have recyclers
                                }
                                Object id = getFragId(v);
                                FragmentUtils.checkForFragmentDifferences((ViewGroup) v, (ViewGroup) fragView, id, (String) tag, currentFragment
                                        .getClass().getSimpleName(), isGone, wasGone);
                            }
                        }
                    } catch (Throwable e) {
                        TLLog.error("F Ex sup: ", e);
                    }

                }
            };
        }

        //Add a little fragment backStack listener. In a try catch in case there is no support lib.
        if (!TLUtils.isDisabled(Functionality.DIALOGS)) {
            try {
                supportBackStackChangedListener = new FragmentManager.OnBackStackChangedListener() {
                    @Override
                    public void onBackStackChanged() {
                        try {
                            List<android.support.v4.app.Fragment> fragments = ((FragmentActivity) TLManager.getInstance().getCurrentActivity()).getSupportFragmentManager().getFragments();
                            if (fragments != null) {
                                for (android.support.v4.app.Fragment fragment : fragments) {
                                    setInitialPropertiesForNewSupportFragment(fragment);
                                    if (fragment instanceof DialogFragment) {
                                        DialogFragment f = (DialogFragment) fragment;
                                        if (f.getDialog().isShowing()) {
                                            // Add a new one.
                                            Object tag = fragment.getTag();
                                            if (tag == null) {
                                                tag = (f.getClass().getSimpleName());
                                            } else {
                                                tag = tag.toString();
                                            }
                                            FragmentUtils.addNewFragment(tag.hashCode(), tag + "_dialog", f
                                                    .getClass().getSimpleName(), (ViewGroup) fragment.getView());
                                            TLViewManager.getInstance().applyTaplyticsOverlaysToDialogsIfNecessary();
                                            ViewUtils.setProperties((ViewGroup) f.getView());
                                        }
                                        return;
                                    }
                                }
                            }
                            for (Map.Entry<Object, Triplet<String, String, Boolean>> entry : TLFragmentManager.getInstance().getFragmentsOnScreen().entrySet()) {
                                if (entry.getValue().second.endsWith("_dialog")) {
                                    FragmentUtils.removeOldFragment(entry.getKey(), false, false);
                                }
                            }
                        } catch (Throwable e) {
                            TLLog.error("Error setting support dialog things");
                        }
                    }
                };
            } catch (Throwable e) {
                TLLog.error("Probably no support, error creating support backstack listener", e);
            }

            try {
                backStackChangedListener = new android.app.FragmentManager.OnBackStackChangedListener() {
                    @Override
                    public void onBackStackChanged() {
                        try {
                            android.app.FragmentManager fm = TLManager.getInstance().getCurrentActivity().getFragmentManager();
                            for (int i = 0; i < fm.getBackStackEntryCount(); i++) {
                                android.app.FragmentManager.BackStackEntry entry = fm.getBackStackEntryAt(i);

                                if (entry.getName() != null && !entry.getName().equals("")) {
                                    Fragment fragment = fm.findFragmentByTag(entry.getName());
                                    setInitialPropertiesForNewFragment(fragment);

                                    if (fragment instanceof android.app.DialogFragment) {
                                        android.app.DialogFragment f = (android.app.DialogFragment) fragment;

                                        if (f.getDialog().isShowing()) {
                                            // Add a new one.
                                            Object tag = fragment.getTag();
                                            if (tag == null) {
                                                tag = (f.getClass().getSimpleName());
                                            } else {
                                                tag = tag.toString();
                                            }
                                            FragmentUtils.addNewFragment(tag.hashCode(), tag + "_dialog", f
                                                    .getClass().getSimpleName(), (ViewGroup) fragment.getView());
                                            TLViewManager.getInstance().applyTaplyticsOverlaysToDialogsIfNecessary();
                                            ViewUtils.setProperties((ViewGroup) f.getView());
                                        }
                                        return;
                                    }
                                }
                            }
                            for (Map.Entry<Object, Triplet<String, String, Boolean>> entry : TLFragmentManager.getInstance().getFragmentsOnScreen().entrySet()) {
                                if (entry.getValue().second.endsWith("_dialog")) {
                                    FragmentUtils.removeOldFragment(entry.getKey(), false, false);
                                }
                            }

                        } catch (Throwable e) {
                            TLLog.error("Error setting dialog things");
                        }
                    }
                };
            } catch (Throwable e) {
                TLLog.error("Error creating backstack listener for regular dialogs", e);
            }
        }
    }

    public Method getViewPagerGetFragmentMethod() {
        return viewPagerGetFragmentMethod;
    }

    public void setViewPagerGetFragmentMethod(Method viewPagerGetFragmentMethod) {
        this.viewPagerGetFragmentMethod = viewPagerGetFragmentMethod;
    }

    public void setViewPagerCurrentFragmentField(Field currentPrimaryFragmentField, Class c) {
        if (c == FragmentPagerAdapter.class) {
            this.currentPrimaryFragmentField = currentPrimaryFragmentField;
        } else if (c == FragmentStatePagerAdapter.class) {
            this.currentPrimaryFragmentStateField = currentPrimaryFragmentField;
        }
    }

    public Field getCurrentPrimaryFragmentField(Class c) {
        if (c == FragmentPagerAdapter.class) {
            return currentPrimaryFragmentField;
        } else {
            return currentPrimaryFragmentStateField;
        }
    }

    private void setInitialPropertiesForNewFragment(android.app.Fragment fragment) {
        if (fragment == null) return;

        try {
            // This will set the initial properties for a new fragment
            Object fragId = FragmentUtils.getFragId(fragment.getView());

            FragmentUtils.addNewFragment(fragId.hashCode(), fragId, fragment.getClass().getSimpleName(), (ViewGroup) fragment.getView());
            FragmentUtils.trackChildFragmentsForFragment(fragment);
        } catch(Throwable e) {
            TLLog.error("Error setting initial properties for new Fragment", e);
        }
    }

    private void setInitialPropertiesForNewSupportFragment(android.support.v4.app.Fragment fragment) {
        if (fragment == null) return;

        try {
            // This will set the initial properties for a new fragment
            Object fragId = FragmentUtils.getFragId(fragment.getView());

            FragmentUtils.addNewFragment(fragId.hashCode(), fragId, fragment.getClass().getSimpleName(), (ViewGroup) fragment.getView());
            FragmentUtils.trackChildFragmentsForSupportFragment(fragment);
        } catch(Throwable e) {
            TLLog.error("Error setting initial properties for new Fragment", e);
        }
    }
}
