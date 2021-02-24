/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.utils;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.taplytics.sdk.analytics.TLAnalyticsManager;
import com.taplytics.sdk.datatypes.Triplet;
import com.taplytics.sdk.managers.TLFragmentManager;
import com.taplytics.sdk.managers.TLManager;
import com.taplytics.sdk.managers.TLViewManager;
import com.taplytics.sdk.resources.TLLinearLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.taplytics.sdk.utils.ViewUtils.attachInitialListeners;

/**
 * Created by VicV on 10/29/14.
 * <p>
 * Utilities for dealing with fragments.
 */
public class FragmentUtils {

    @SuppressWarnings("NewApi")
    @SuppressLint("NewApi")
    /**Check if the current ViewGroup is actually a fragment container. If it is, we add a listener to it which will update the view if necessary from the server.**/
    static boolean checkIfFragment(final ViewGroup viewGroup, boolean knownNotSupport) {
        if (!TLManager.getInstance().areFragmentsEnabled()) {
            return false;
        }

        try {
            Activity currentActivity = TLManager.getInstance().getCurrentActivity();
            if (currentActivity != null && viewGroup.findViewById(generateFalseViewId(viewGroup)) == null) {

                // Right now, this is only good for basic fragments. NOT for support fragments.
                if (Build.VERSION.SDK_INT >= 11 && !TLUtils.isDisabled(Functionality.FRAGMENTS)
                        && currentActivity.getFragmentManager().findFragmentById(viewGroup.getId()) != null) {

                    viewGroup.removeOnLayoutChangeListener(TLFragmentManager.getInstance().getOnLayoutChangeListenerForFragment());
                    viewGroup.addOnLayoutChangeListener(TLFragmentManager.getInstance().getOnLayoutChangeListenerForFragment());
                    ViewUtils.setProperties(viewGroup);
                    markFragmentChecked(viewGroup);
                    return false;

                } else if ((!TLUtils.isDisabled(Functionality.VIEWPAGERS) && !TLUtils.isDisabled(Functionality.SUPPORTFRAGMENTS)) && hasViewPagers() && Class.forName("android.support.v4.view.ViewPager").isInstance(viewGroup)) {
                    final ViewPager viewPager = (ViewPager) viewGroup;
                    final PagerAdapter adapter = viewPager.getAdapter();

                    Object o = viewPager.getTag(TLViewManager.getInstance().getApplicationIconId());
                    if (o == null) {
                        HashMap map = new HashMap();
                        map.put("hasGlobalLayoutListener", true);
                        viewPager.setTag(TLViewManager.getInstance().getApplicationIconId(), map);
                    } else if (o instanceof HashMap) {
                        HashMap map = (HashMap) o;
                        if (map.containsKey("hasGlobalLayoutListener")) {
                            return false;
                        } else {
                            map.put("hasGlobalLayoutListener", true);
                            viewPager.setTag(TLViewManager.getInstance().getApplicationIconId(), map);
                        }
                    }
                    // Nice to have: Same ID on different pages somehow.
                    // TODO: Update viewpager stuff to handle changes even when all views are presented at once (#144743795)
                    viewGroup.getViewTreeObserver().addOnGlobalLayoutListener(new TLGlobalLayoutListener(viewPager, viewGroup, adapter));
                    markFragmentChecked(viewGroup);
                    return false;
                } else {
                    try {
                        // Don't waste time hitting this block if we know we aren't a support fragment
                        if (!knownNotSupport && !TLUtils.isDisabled(Functionality.SUPPORTFRAGMENTS)) {

                            try {
                                // Support Fragments. Same as regular fragments only with 9 metric tons of reflection.
                                final FragmentManager supportFragmentManager = (FragmentManager) currentActivity.getClass()
                                        .getMethod("getSupportFragmentManager")
                                        .invoke(currentActivity);


                                if (supportFragmentManager != null) {
                                    //Make sure that we aren't currently executing transactions elsewhere.
                                    if (!TLFragmentManager.getInstance().hasExecutedTransactions()) {
                                        // Get a handler that can be used to post to the main thread

                                        if (TLManager.getInstance().isAsyncEnabled()) {
                                            Handler mainHandler = new Handler(TLManager.getInstance().getCurrentActivity().getMainLooper());
                                            mainHandler.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    executePendingFragmentManagerTransactions(supportFragmentManager);
                                                }
                                            });
                                        } else {
                                            executePendingFragmentManagerTransactions(supportFragmentManager);
                                        }
                                    }

                                    // Grab the fragment manager.
                                    final Object supportFragment = supportFragmentManager.findFragmentById(viewGroup.getId());

                                    // Toss our listener on there and remove the old one
                                    if (supportFragment != null) {
                                        viewGroup.removeOnLayoutChangeListener(TLFragmentManager.getInstance().getOnLayoutChangeListenerForSupportFragment());
                                        viewGroup.addOnLayoutChangeListener(TLFragmentManager.getInstance().getOnLayoutChangeListenerForSupportFragment());
                                        ViewUtils.setProperties(viewGroup);
                                        markFragmentChecked(viewGroup);
                                        return false;
                                    }
                                } else {
                                    return true;
                                }
                            } catch (Throwable e) {
                                return true;
                            }
                        }
                    } catch (Throwable e) {
                        TLLog.error("F sup: ", (e instanceof Exception) ? (Exception) e : new Exception());
                        return false;
                    }
                }
            }
        } catch (Throwable e) {
            TLLog.error("fragstuff", e);
        }
        return false;
    }

    /**
     * Checks if fragment manager has any pending transactions and executes them
     * @param fragmentManager
     */
    private static void executePendingFragmentManagerTransactions(FragmentManager fragmentManager) {
        try {
            if (!TLFragmentManager.getInstance().hasExecutedTransactions()) {
                Field f = TLFragmentManager.getInstance().getExecutingTransactionsField();
                if (f == null) {
                    f = fragmentManager.getClass().getDeclaredField("mExecutingActions");
                    TLFragmentManager.getInstance().setExecutingTransactionsField(f);
                }
                if (f != null) {
                    f.setAccessible(true);
                    if (!f.getBoolean(fragmentManager)) {
                        fragmentManager.executePendingTransactions();
                        TLFragmentManager.getInstance().setHasExecutedTransactions(true);
                    }
                }
            }
        } catch (Throwable e) {
            TLLog.error("Error getting fragmentManager executing transactions field", e);
        }
    }

    /**
     * Check if the support fragment has a childFragmentManager and sets listener and properties on
     * each fragment
     * @param fragment
     */
    @SuppressWarnings("NewApi")
    @SuppressLint("NewApi")
    public static void trackChildFragmentsForSupportFragment(android.support.v4.app.Fragment fragment) {
        if (Build.VERSION.SDK_INT < 11) return;

        FragmentManager childFragmentManager = null;
        try {
            childFragmentManager = (FragmentManager)fragment.getClass()
                                                            .getMethod("getChildFragmentManager")
                                                            .invoke(fragment);
        } catch (Exception e) {
            // no child fragment manager
        }
        if (childFragmentManager == null) return;

        TLLog.debug("found child fragment manager on support fragment: " + fragment.getClass().getSimpleName());
        List<Fragment> fragments = childFragmentManager.getFragments();

        if (fragments == null) return;

        for (Fragment f : fragments) {
            TLLog.debug("Child fragment: " + f.getClass().getSimpleName());
            View view = f.getView();
            if (view == null || !(view instanceof ViewGroup)) continue;
            ViewGroup viewGroup = (ViewGroup)view;

            // add listener and set the properties
            viewGroup.addOnLayoutChangeListener(TLFragmentManager.getInstance().getOnLayoutChangeListenerForSupportFragment());
            try {
                ViewUtils.setProperties(viewGroup);
            } catch (JSONException e) {
                TLLog.error("ViewUtils.setProperties JSON Exception", e);
            }
            markFragmentChecked(viewGroup);

            // check if the child fragments also have child fragment managers
            trackChildFragmentsForSupportFragment(f);
        }
    }

    /**
     * Check if the fragment has a childFragmentManager and sets listener and properties on
     * each fragment
     * getFragments() is only supported on API level 26
     * @param fragment
     */
    @TargetApi(26)
    public static void trackChildFragmentsForFragment(android.app.Fragment fragment) {
        android.app.FragmentManager childFragmentManager = null;
        try {
            childFragmentManager = (android.app.FragmentManager)fragment.getClass()
                                                                        .getMethod("getChildFragmentManager")
                                                                        .invoke(fragment);
        } catch (Exception e) {
            // no child fragment manager
        }
        if (childFragmentManager == null) return;

        TLLog.debug("found child fragment manager on fragment: " + fragment.getClass().getSimpleName());
        List<android.app.Fragment> fragments = childFragmentManager.getFragments();

        if (fragments == null) return;

        for (android.app.Fragment f : fragments) {
            TLLog.debug("Child fragment: " + f.getClass().getSimpleName());
            View view = f.getView();
            if (view == null || !(view instanceof ViewGroup)) continue;
            ViewGroup viewGroup = (ViewGroup)view;

            // add listener and set the properties
            viewGroup.addOnLayoutChangeListener(TLFragmentManager.getInstance().getOnLayoutChangeListenerForFragment());
            try {
                ViewUtils.setProperties(viewGroup);
            } catch (JSONException e) {
                TLLog.error("ViewUtils.setProperties JSON Exception", e);
            }
            markFragmentChecked(viewGroup);

            // check if the child fragments also have child fragment managers
            trackChildFragmentsForFragment(f);
        }
    }

    private static boolean hasViewPagers() {
        try {
            Class.forName("android.support.v4.view.ViewPager");
        } catch (Throwable e) {
            return false;
        }
        return true;
    }

    /**
     * Mostly for initial load, what this does is note that Taplytics has looked at this fragment.
     * In some cases (such as atimi), the system does not notice that the fragment is loaded by the time
     * setProperties is called. This notifys the fragmentmanager not to attachInitialListeners,
     * and let the TLViewmanager to do it now.
     *
     * @param viewGroup Fragment viewgroup.
     */
    private static void markFragmentChecked(ViewGroup viewGroup) {
        if (viewGroup.getChildCount() > 0) {
            Object o = viewGroup.getTag(TLViewManager.getInstance().getApplicationIconId());
            HashMap map = null;
            if (o == null) {
                map = new HashMap();
            } else if (o instanceof HashMap) {
                map = (HashMap) o;
            }
            if (map != null) {
                map.put("fragmentChecked", true);
                viewGroup.setTag(TLViewManager.getInstance().getApplicationIconId(), map);
            }
        }
    }


    private static Field positionField;
    private static Field objectField;

    private static boolean getViewpagerFields(Object item) {
        try {
            positionField = item.getClass().getDeclaredField("position");
            positionField.setAccessible(true);
            objectField = item.getClass().getDeclaredField("object");
            objectField.setAccessible(true);
        } catch (Throwable t) {
            return false;
        }
        return positionField != null && objectField != null;
    }

    /**
     * ViewPagers have a field called mItems.
     * Each item has position and fragment.
     * Here, we're searching for that field to use.
     *
     * @param pager ViewpagerClass
     * @return Viewpager$ItemInfo
     */
    private static Field getItemsField(Class pager) {
        Field f = null;
        try {
            try {
                f = pager.getDeclaredField("mItems");
            } catch (Throwable ignored) {
                //Not here!
            }
            if (f == null && !pager.getName().equals("android.support.v4.view.ViewPager") && !pager.getSimpleName().equals("Object")) {
                f = getItemsField(pager.getSuperclass());
            }
        } catch (Throwable t) {
            TLLog.error("g i f", t);
        }
        return f;
    }

    /**
     * Add a tracking tag to each current page in the viewpager if we have not seen it.
     * Then, apply properties to it.
     */
    private static void updateViewPages(ViewPager viewPager) {

        try {
            //Get the array of all item info.
            Field f = getItemsField(viewPager.getClass());
            f.setAccessible(true);

            //Iterate over each one.
            ArrayList items = (ArrayList) f.get(viewPager);
            for (Object mItem : items) {
                //Grab the positions and each fragment.
                if ((positionField != null && objectField != null) || getViewpagerFields(mItem)) {
                    positionField.setAccessible(true);
                    objectField.setAccessible(true);
                    Object frag = objectField.get(mItem);
                    if (frag instanceof Fragment) {
                        Fragment fragment = (Fragment) frag;
                        View v = fragment.getView();

                        //Check if the tag exists yet. If not, add it, and setProperties.
                        if (v != null && v instanceof ViewGroup && v.getTag(TLFragmentManager.getInstance().getFragmentTrackingId()) == null) {
                            String tag = fragment.getClass().getSimpleName() + "_viewpager_" + positionField.get(mItem);
                            addTrackingTag((ViewGroup) v, tag);
                            try {
                                ViewUtils.setProperties((ViewGroup) v.getParent());
                                ViewUtils.attachInitialListeners((ViewGroup) v.getParent(),true);
                            } catch (Throwable e) {
                                // Special catch because setProperties breaks when called later than activity runtime.
                            }
                        }
                    }
                }
            }
        } catch (Throwable e) {
            TLLog.error("uvp e", e);
        }

    }

    /**
     * Check for differences and apply changes if a new fragment has been detected. *
     */
    public static void checkForFragmentDifferences(final ViewGroup viewGroup, final ViewGroup fragView, final Object fragId, final String tag, final String className, boolean isGone,
                                                   boolean wasGone) {
        try {
            // See if we've already added our tracking tag to the fragment. If so, don't update on our terms.
            if (fragView.getTag(TLFragmentManager.getInstance().getFragmentTrackingId()) == null) {

                addTrackingTag(fragView, tag);

                // Remove the old fragment if its there.
                removeOldFragment(fragId, true, false);

                // Add a new one.
                addNewFragment(fragId, tag, className, viewGroup);

                // Update the views if necessary.
                ViewUtils.setProperties(TLViewManager.getInstance().getCurrentViewGroup());

            } else if ((fragView.getVisibility() == View.GONE || isGone) && viewGroup.getParent() instanceof ViewGroup) {
                //View has been hidden or something, so we're going to say that its gone and remove it.
                removeOldFragment(fragId, true, false);
                // Then we're gonna check the parent's fragment to see if one was drawn on top of it.
                checkIfFragment((ViewGroup) viewGroup.getParent(), true);
            } else if (wasGone && !isGone && viewGroup.getParent() instanceof ViewGroup) {
                // If something was actually removed, then we'll add a new one
                if (removeOldFragment(getFragId(((ViewGroup) viewGroup.getParent())), true, false)) {
                    addNewFragment(fragId, tag, className, viewGroup);
                }
            }
        } catch (Throwable e) {
            // Special catch because setProperties breaks when called later than activity runtime.
        }
    }

    /**
     * Generate an ID to give our false view. It is based off of the viewgroup itself to avoid confusion if there are nested fragments
     *
     * @return and integer that is the id + or - 1000 the viewgroup id.
     */
    private static int generateFalseViewId(ViewGroup vg) {
        int id = 0;
        // Just a quick check to make sure dont go over the max int..
        try {
            if (vg.getId() <= 0 || Integer.MAX_VALUE - vg.getId() > 1000) {
                id = vg.getId() + 1000;
            } else {
                id = vg.getId() - 1000;
            }
            //THIS IS TO AVOID CONFLICTS.
            while (vg.findViewById(id) != null && !(vg.findViewById(id) instanceof TLLinearLayout) && id > 0) {
                id--;
            }
        } catch (Throwable e) {
            // you never know..
        }
        return id;
    }

    /**
     * Remove the fragment with the id from our maps and track it
     *
     * @return whether or not the fragment was actually present in our list
     */

    public static boolean removeOldFragment(Object id, boolean removeFromScreen, boolean wasBackground) {
        try {
            // Check if the fragment is on screen at all.
            if (!TLFragmentManager.getInstance().getFragmentsOnScreen().containsKey(id)) {
                return false;
            }

            if (removeFromScreen) {
                TLFragmentManager.getInstance().getFragmentsOnScreen().remove(id);
            }

            if (TLAnalyticsManager.getFragmentMap().containsKey(id) && !wasBackground) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.putOpt("fragment", true);

                if (!TLUtils.isDisabled(Functionality.VIEWTRACKING)) {
                    String className = TLAnalyticsManager.getFragmentMap().get(id).first;
                    TLManager.getInstance()
                            .getTlAnalytics()
                            .trackTLActivityEvent(TLAnalyticsManager.TLAnalyticsEventActivityDisappeared, null,
                                    className);

                    ViewUtils.attachInitialListeners(TLViewManager.getInstance().getCurrentViewGroup(),false);
                    TLViewManager.getInstance().updateViewsIfNecessary();

                    // Track the time we spent on this fragment
                    TLAnalyticsManager.trackFragmentTime(id, className, false);
                }
            }
            return true;
        } catch (Throwable e) {
            TLLog.error("fragment removal:", e);
            return false;
        }
    }

    /**
     * Remove the fragment with the id from our maps and track it *
     */
    private static void removeOldViewpagerFragment(Object id) {
        try {
            // Check if the fragment is on screen at all.
            boolean onScreen = TLFragmentManager.getInstance().getFragmentsOnScreen().containsKey(id);

            Triplet<String, String, Boolean> frag = TLFragmentManager.getInstance().getFragmentsOnScreen().remove(id);

            if (onScreen) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.putOpt("fragment", true);

                String className = TLAnalyticsManager.getFragmentMap().get(id).first;

                if (!TLUtils.isDisabled(Functionality.VIEWTRACKING)) {
                    TLManager
                            .getInstance()
                            .getTlAnalytics()
                            .trackTLActivityEvent(TLAnalyticsManager.TLAnalyticsEventActivityDisappeared, null, className, jsonObject);

                    // Track the time we spent on this fragment
                    TLAnalyticsManager.trackFragmentTime(id, className, false);
                }

                TLFragmentManager.getInstance().getViewPagerFragments().add(frag.second);
            }

        } catch (Throwable e) {
            TLLog.error("VP remove:", e);
        }
    }

    @SuppressWarnings("unchecked")
    @SuppressLint("unchecked")
    /** Add the fragment with the id to our maps and track it **/
    public static void addNewFragment(Object id, Object tag, String className, ViewGroup viewGroup) {
        try {

            if ((!TLFragmentManager.getInstance().getFragmentsOnScreen().containsKey(id) || (TLFragmentManager.getInstance().getFragmentsOnScreen()
                    .containsKey(id) && !TLFragmentManager.getInstance().getFragmentsOnScreen().get(id).second.equals(tag)))) {
                // Add the fragment to our list of onscreen fragments
                TLLog.debug("ADDING FRAGMENT TO THING");
                TLFragmentManager
                        .getInstance()
                        .getFragmentsOnScreen().put(id,
                        new Triplet(TLManager.getInstance().getCurrentActivity().getClass().getSimpleName(), (className + ((tag == null) ? ""
                                : "_" + tag.toString())), false));

                JSONObject jsonObject = new JSONObject();
                jsonObject.putOpt("fragment", true);

                if (!TLUtils.isDisabled(Functionality.VIEWTRACKING)) {
                    // Track the fragment.
                    TLManager.getInstance().getTlAnalytics().trackTLActivityEvent(TLAnalyticsManager.TLAnalyticsEventActivityAppeared, null, className, jsonObject);
                    // Keep it in our analytics map
                    TLAnalyticsManager.getFragmentMap().put(id, new Pair<>(className, System.currentTimeMillis()));
                }
                TLManager.getInstance().checkLastActivityForNewSession();


                //This section checks to see whether or not we have identified that this fragment has been checked yet.
                //If we get here BEFORE TLViewManager did, then we're going to not have any listeners on the viewgroups.
                //This ensures all listeners are attached regardless of load time.
                if (viewGroup.getTag(TLViewManager.getInstance().getApplicationIconId()) == null) {
                    attachInitialListeners(viewGroup, false);
                } else {
                    Object o = viewGroup.getTag(TLViewManager.getInstance().getApplicationIconId());
                    if (o instanceof HashMap) {
                        HashMap map = (HashMap) o;
                        if (!map.containsKey("fragmentChecked")) {
                            attachInitialListeners(viewGroup, false);
                        }
                    }
                }


            }
        } catch (Throwable e) {
            TLLog.error("adding f: ", e);
        }
    }

    @SuppressWarnings("unchecked")
    @SuppressLint("unchecked")
    /** Add the fragment with the id to our maps and track it **/
    private static void addNewViewPagerFragment(Object id, Object tag, String className) {
        try {

            if ((!TLFragmentManager.getInstance().getFragmentsOnScreen().containsKey(id) || (TLFragmentManager.getInstance().getFragmentsOnScreen()
                    .containsKey(id) && !TLFragmentManager.getInstance().getFragmentsOnScreen().get(id).second.equals(tag)))) {
                // Add the fragment to our list of onscreen fragments
                TLFragmentManager.getInstance().getFragmentsOnScreen()
                        .put(id, new Triplet(TLManager.getInstance().getCurrentActivitySimpleClassName(), tag, false));

                JSONObject jsonObject = new JSONObject();
                jsonObject.putOpt("fragment", true);

                if (!TLUtils.isDisabled(Functionality.VIEWTRACKING)) {
                    // Track the fragment.
                    TLManager.getInstance().getTlAnalytics().trackTLActivityEvent(TLAnalyticsManager.TLAnalyticsEventActivityAppeared, null, className, jsonObject);
                    TLAnalyticsManager.getFragmentMap().put(id, new Pair<>(className, System.currentTimeMillis()));

                }
                TLManager.getInstance().checkLastActivityForNewSession();
                TLFragmentManager.getInstance().getViewPagerFragments().add((String) tag);
            }
        } catch (Throwable e) {
            TLLog.error("adding vp: ", e);
        }
    }

    public static void resumeFragments() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.putOpt("fragment", true);

            for (Map.Entry<Object, Triplet<String, String, Boolean>> entry : TLFragmentManager.getInstance().getFragmentsOnScreen().entrySet()) {
                try {
                    if (!TLUtils.isDisabled(Functionality.VIEWTRACKING)) {
                        // Track the fragment.
                        entry.getValue().third = false;
                        TLManager.getInstance().getTlAnalytics().trackTLActivityEvent(TLAnalyticsManager.TLAnalyticsEventActivityAppeared, null, TLAnalyticsManager.getFragmentMap().get(entry.getKey()).first, jsonObject);
                        TLAnalyticsManager.getFragmentMap().put(entry.getKey(), new Pair<>(TLAnalyticsManager.getFragmentMap().get(entry.getKey()).first, System.currentTimeMillis()));
                    }
                } catch (Throwable e) {
                    //
                }
            }
        } catch (Throwable e) {
            //
        }
    }

    /**
     * Add a tracking tag to the given viewgroup
     * <p>
     * We do this to be able to confirm that we HAVE indeed been back to this fragment.
     *
     * @param vg  the ViewGroup to add the view to.
     * @param tag The tag of the fragment.
     */
    private static void addTrackingTag(ViewGroup vg, Object tag) {
        if (tag != null) {
            vg.setTag(TLFragmentManager.getInstance().getFragmentTrackingId(), tag);
        }
    }

    /**
     * Check if the view we clickes is in a fragment that we have on the screen.
     *
     * @return the id of the fragment if it is in one, otherwise -2
     */
    static Object isViewInFragment(View v) {
        try {
            ViewGroup parent = ViewUtils.getParent(v);
            if (parent != TLViewManager.getInstance().getCurrentViewGroup()) {
                if (TLFragmentManager.getInstance().getFragmentsOnScreen().containsKey(getFragId(parent))) {
                    return getFragId(parent);
                } else {
                    return isViewInFragment(parent);
                }
            } else {
                return -2;
            }
        } catch (Throwable e) {
            return -2;
        }
    }


    public static void attachBackStackListener(Activity activity) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                if (activity.getFragmentManager() != null) {
                    android.app.FragmentManager.OnBackStackChangedListener listener = TLFragmentManager.getInstance().getBackStackChangedListener();
                    activity.getFragmentManager().removeOnBackStackChangedListener(listener);
                    activity.getFragmentManager().addOnBackStackChangedListener(listener);
                }
            }
        } catch (Throwable e) {
            TLLog.error("Error adding standard backstack listener");
        }

        try {
            FragmentManager supportFragmentManager = (FragmentManager) activity.getClass()
                    .getMethod("getSupportFragmentManager")
                    .invoke(activity);

            FragmentManager.OnBackStackChangedListener listener = TLFragmentManager.getInstance().getSupportBackStackChangedListener();
            supportFragmentManager.removeOnBackStackChangedListener(listener);
            supportFragmentManager.addOnBackStackChangedListener(listener);

        } catch (Throwable e) {
            TLLog.error("Error adding support backstack changed listener", e);
        }
    }

    static class TLGlobalLayoutListener implements ViewTreeObserver.OnGlobalLayoutListener {
        private WeakReference<ViewPager> viewPagerWeakRef;
        private WeakReference<ViewGroup> viewGroupWeakRef;
        private WeakReference<PagerAdapter> pagerAdapterWeakRef;

        public TLGlobalLayoutListener(ViewPager pager, ViewGroup group, PagerAdapter adapter) {
            this.viewPagerWeakRef = new WeakReference<>(pager);
            this.viewGroupWeakRef = new WeakReference<>(group);
            this.pagerAdapterWeakRef = new WeakReference<>(adapter);
        }

        public ViewPager getViewPager() {
            return viewPagerWeakRef != null ? viewPagerWeakRef.get() : null;
        }

        public ViewGroup getViewGroup() {
            return viewGroupWeakRef != null ? viewGroupWeakRef.get() : null;
        }

        public PagerAdapter getPagerAdapter() {
            return pagerAdapterWeakRef != null ? pagerAdapterWeakRef.get() : null;
        }

        public boolean hasViewPagerAndViewGroup() {
            return getViewPager() != null && getViewGroup() != null;
        }

        @Override
        public void onGlobalLayout() {
            try {
                //If the people have implemented a FragmentPagerAdapter, we don't have to do anything.
                //But, some people just don't do that for some reason.
                final ViewPager viewPager = getViewPager();
                final ViewGroup viewGroup = getViewGroup();
                PagerAdapter pagerAdapter = getPagerAdapter();
                if (!hasViewPagerAndViewGroup()) {
                    return;
                }

                int item = viewPager.getCurrentItem();
                String currentFragmentString = "";
                if (pagerAdapter == null) {
                    pagerAdapter = viewPager.getAdapter();
                }

                Fragment currentFragment = findFragmentInViewPager(viewPager, pagerAdapter, item);
                if (currentFragment != null) {
                    currentFragmentString = currentFragment.getClass().getSimpleName();
                }

                //Don't bother tracking things that didn't work correctly.
                if (currentFragmentString.equals(""))
                    return;

                //Append _viewpager_ to this so we can identify it as a viewpager elsewhere.
                String tag = currentFragmentString + "_viewpager_" + item;

                // Check if the current viewGroup has the tracking tag on it
                if (viewGroup.getTag(TLFragmentManager.getInstance().getFragmentTrackingId()) == null) {
                    addTrackingTag(viewGroup, tag);
                    addNewViewPagerFragment(getFragId(viewGroup), tag, currentFragmentString);
                    updateViewPages(viewPager);
                } else if (!(viewGroup.getTag(TLFragmentManager.getInstance().getFragmentTrackingId()).equals(tag))) {
                    viewGroup.setTag(TLFragmentManager.getInstance().getFragmentTrackingId(), tag);
                    removeOldViewpagerFragment(getFragId(viewGroup));
                    addNewViewPagerFragment(getFragId(viewGroup), tag, currentFragmentString);
                    updateViewPages(viewPager);
                }

            } catch (Throwable ex) {
                TLLog.error("frg", ex);
            }
        }
    }

    static Fragment findFragmentInViewPager(ViewPager pager, PagerAdapter pagerAdapter, int item) {
        Fragment currentFragment = null;

        //Check if we have a proper adapter.
        try {
            if ((pagerAdapter instanceof FragmentPagerAdapter || pagerAdapter instanceof FragmentStatePagerAdapter)) {
                if ((pager != null && pager.getCurrentItem() == item) || pager == null) {
                    Class adapterClass = (pagerAdapter instanceof FragmentPagerAdapter) ? FragmentPagerAdapter.class : FragmentStatePagerAdapter.class;
                    //Grab the fragment through reflection to avoid interference.
                    Field currentPrimaryFragmentField = null;
                    //See if we've done this already and just grab the field (to save time/processing)
                    if (TLFragmentManager.getInstance().getCurrentPrimaryFragmentField(adapterClass) != null) {

                        currentPrimaryFragmentField = TLFragmentManager.getInstance().getCurrentPrimaryFragmentField(adapterClass);
                        currentFragment = getFragmentFromCurrentFragmentField(currentPrimaryFragmentField, pagerAdapter);
                        if (currentFragment != null) {
                            return currentFragment;
                        }
                    } else {
                        try {
                            currentPrimaryFragmentField = adapterClass.getDeclaredField("mCurrentPrimaryItem");
                        } catch (Throwable e) {
                            //Do  avoid name change issues, just find the field that returns a fragment in FragmentPagerAdapter (theres only one)
                            //The field we are looking for is simply mCurrentPrimaryFragment
                            for (Field f : adapterClass.getDeclaredFields()) {
                                if (f.getType() == Fragment.class) {
                                    currentPrimaryFragmentField = f;
                                    break;
                                }
                            }
                        }
                    }
                    if (currentPrimaryFragmentField != null) {
                        currentFragment = getFragmentFromCurrentFragmentField(currentPrimaryFragmentField, pagerAdapter);
                        TLFragmentManager.getInstance().setViewPagerCurrentFragmentField(currentPrimaryFragmentField, adapterClass);
                    }
                }
            }
            if (currentFragment == null && pagerAdapter != null) {
                try {
                    //Here, we just guess at what method it is. If they're not using a FragmentPagerAdapter
                    // and instead are using their own special implementation, its a very good guess that they
                    // implemented a getFragmentAtPosition function (both RetailMeNot and Cartwheel did).
                    // So, as an educated guess, find a method that returns a Fragment when given an Int.
                    if (TLFragmentManager.getInstance().getViewPagerGetFragmentMethod() == null) {
                        for (Method m : pagerAdapter.getClass().getDeclaredMethods()) {
                            if (m.getReturnType().equals(Fragment.class)) {
                                if (m.getParameterTypes().length == 1) {
                                    for (Class<?> c : m.getParameterTypes()) {
                                        if (c.equals(int.class)) {
                                            TLFragmentManager.getInstance().setViewPagerGetFragmentMethod(m);
                                        }
                                    }
                                }
                            }
                        }

                        //Then, if we got the method, grab the fragment.
                        if (TLFragmentManager.getInstance().getViewPagerGetFragmentMethod() != null) {
                            currentFragment = (Fragment) TLFragmentManager.getInstance().getViewPagerGetFragmentMethod()
                                    .invoke(pagerAdapter, item);
                        }
                    }
                } catch (Throwable e) {
                    //this is a known error we'll probably see a bunch.
                    return null;
                }
            }

        } catch (Throwable e) {
            TLLog.error("problem getting vp", e);
        }
        return currentFragment;
    }

    private static Fragment getFragmentFromCurrentFragmentField(Field f, PagerAdapter adapter) throws Exception {
        f.setAccessible(true);
        Object frag = f.get(adapter);
        return frag == null ? null : (Fragment) frag;
    }

    public static Object getFragId(View v) {
        try {
            String name = TLManager.getInstance().getAppContext().getResources().getResourceEntryName(v.getId());
            if (name != null && !name.equals("")) {
                return name;
            } else {
                return v.getId();
            }
        } catch (Throwable e) {
            return v.getId();
        }
    }
}


