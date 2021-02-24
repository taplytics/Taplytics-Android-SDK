/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.utils;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewStub;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.taplytics.sdk.datatypes.Triplet;
import com.taplytics.sdk.listeners.TLAggressiveTextWatcher;
import com.taplytics.sdk.listeners.TLHierarchyChangeListener;
import com.taplytics.sdk.listeners.TLListRecyclerListener;
import com.taplytics.sdk.listeners.TLListViewDataSetObserver;
import com.taplytics.sdk.listeners.TLOnRecyclerScrollListener;
import com.taplytics.sdk.listeners.TLOnScrollListener;
import com.taplytics.sdk.listeners.TLRecyclerListener;
import com.taplytics.sdk.listeners.TLRecyclerViewDataSetObserver;
import com.taplytics.sdk.managers.TLDialogManager;
import com.taplytics.sdk.managers.TLFragmentManager;
import com.taplytics.sdk.managers.TLManager;
import com.taplytics.sdk.managers.TLMethodManager;
import com.taplytics.sdk.managers.TLViewManager;
import com.taplytics.sdk.resources.BorderFrameLayout;
import com.taplytics.sdk.resources.Colors;
import com.taplytics.sdk.resources.TLBorderShape;
import com.taplytics.sdk.resources.TLHighlightShape;
import com.taplytics.sdk.resources.TouchScreenOverlay;
import com.taplytics.sdk.utils.constants.ViewConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import static com.taplytics.sdk.utils.constants.ViewConstants.ACTIVITY;
import static com.taplytics.sdk.utils.constants.ViewConstants.ACTIVITY_ID_FIELD;
import static com.taplytics.sdk.utils.constants.ViewConstants.ANDROID_IDENTIFIER;
import static com.taplytics.sdk.utils.constants.ViewConstants.ANDROID_PROPERTIES;
import static com.taplytics.sdk.utils.constants.ViewConstants.BASE_CLASS_FIELD_NAME;
import static com.taplytics.sdk.utils.constants.ViewConstants.CELL_INFO;
import static com.taplytics.sdk.utils.constants.ViewConstants.CLASS_FIELD_NAME;
import static com.taplytics.sdk.utils.constants.ViewConstants.DECOR_VIEW;
import static com.taplytics.sdk.utils.constants.ViewConstants.FRAG_ID;
import static com.taplytics.sdk.utils.constants.ViewConstants.FRAG_IDENTIFIER;
import static com.taplytics.sdk.utils.constants.ViewConstants.GET_RAW_HEIGHT_METHOD_NAME;
import static com.taplytics.sdk.utils.constants.ViewConstants.GET_RAW_WIDTH_METHOD_NAME;
import static com.taplytics.sdk.utils.constants.ViewConstants.GET_REAL_SIZE_METHOD_NAME;
import static com.taplytics.sdk.utils.constants.ViewConstants.HAS_DATA_SET_OBSERVER_FIELD;
import static com.taplytics.sdk.utils.constants.ViewConstants.HAS_ON_CLICK_FIELD;
import static com.taplytics.sdk.utils.constants.ViewConstants.HEIGHT;
import static com.taplytics.sdk.utils.constants.ViewConstants.HIERARCHY_CHANGE_LISTENER_FIELD_NAME;
import static com.taplytics.sdk.utils.constants.ViewConstants.ID;
import static com.taplytics.sdk.utils.constants.ViewConstants.IDENTIFIER;
import static com.taplytics.sdk.utils.constants.ViewConstants.IMAGE_FILE_NAME_FIELD;
import static com.taplytics.sdk.utils.constants.ViewConstants.INIT_PROPERTIES;
import static com.taplytics.sdk.utils.constants.ViewConstants.IS_CELL_ELEMENT;
import static com.taplytics.sdk.utils.constants.ViewConstants.IS_IN_RECYCLER_VIEW;
import static com.taplytics.sdk.utils.constants.ViewConstants.LIST_IDENTIFIER;
import static com.taplytics.sdk.utils.constants.ViewConstants.LIST_OR_FRAGMENT_FIRST_TIME;
import static com.taplytics.sdk.utils.constants.ViewConstants.METHOD_INFO_FIELD_NAME;
import static com.taplytics.sdk.utils.constants.ViewConstants.OBJECT;
import static com.taplytics.sdk.utils.constants.ViewConstants.ON_SCROLL_LISTENER_FIELD_NAME;
import static com.taplytics.sdk.utils.constants.ViewConstants.POSITION;
import static com.taplytics.sdk.utils.constants.ViewConstants.POSITION_END_X;
import static com.taplytics.sdk.utils.constants.ViewConstants.POSITION_END_Y;
import static com.taplytics.sdk.utils.constants.ViewConstants.POSITION_START_X;
import static com.taplytics.sdk.utils.constants.ViewConstants.POSITION_START_Y;
import static com.taplytics.sdk.utils.constants.ViewConstants.RECYCLER_FIELD_NAME;
import static com.taplytics.sdk.utils.constants.ViewConstants.RECYCLER_LISTENER_FIELD_NAME;
import static com.taplytics.sdk.utils.constants.ViewConstants.RESET_FIELD;
import static com.taplytics.sdk.utils.constants.ViewConstants.SCREEN_DIMENSIONS_FIELD;
import static com.taplytics.sdk.utils.constants.ViewConstants.SCROLL_LISTENER_FIELD_NAME;
import static com.taplytics.sdk.utils.constants.ViewConstants.SHOULD_RESET;
import static com.taplytics.sdk.utils.constants.ViewConstants.STATUS_BAR_ANDROID;
import static com.taplytics.sdk.utils.constants.ViewConstants.STATUS_BAR_DIMENSION;
import static com.taplytics.sdk.utils.constants.ViewConstants.STATUS_BAR_HEIGHT;
import static com.taplytics.sdk.utils.constants.ViewConstants.SUB_CLASS_FIELD_NAME;
import static com.taplytics.sdk.utils.constants.ViewConstants.TYPE;
import static com.taplytics.sdk.utils.constants.ViewConstants.VALUE;
import static com.taplytics.sdk.utils.constants.ViewConstants.VIEW;
import static com.taplytics.sdk.utils.constants.ViewConstants.VIEWPAGER_CLASS_NAME;
import static com.taplytics.sdk.utils.constants.ViewConstants.VIEW_ID;
import static com.taplytics.sdk.utils.constants.ViewConstants.WIDTH;

/**
 * Utility class for View things!
 */
public class ViewUtils {

    private static final String IS_IN_LIST_VIEW = "isInListView";
    private static final String TEXT_IDENTIFIER_ADDITION = "tl_text";

    /**
     * This is the int we use to store whether or not we've applied a listener to a view because there is no way to remove the layout listener given that they will all be different
     *
     * @return
     */
    static int getVisibilityTag() {
        try {
            if (Build.VERSION.SDK_INT < 23) {
                return TLManager.getInstance().getAppContext().getResources().getColor(android.R.color.black);
            } else {
                return TLManager.getInstance().getAppContext().getColor(android.R.color.black);
            }
        } catch (Throwable t) {
            return 105;
        }
    }

    /**
     * This is the int we use to store whether or not we've applied a textwatcher to a textview because there is no way to remove the textview given that they will all be different
     *
     * @return
     */
    static int getTextWatcherTag() {
        try {
            if (Build.VERSION.SDK_INT < 23) {
                return TLManager.getInstance().getAppContext().getResources().getColor(android.R.color.white);
            } else {
                return TLManager.getInstance().getAppContext().getColor(android.R.color.white);
            }
        } catch (Throwable t) {
            return 106;
        }
    }


    /**
     * Get a textwatcher from the provided textview if we have assigned one.
     */
    static TLAggressiveTextWatcher getTLTextWatcherFromTextView(TextView  v){
        try {

            Field field = TextView.class.getDeclaredField("mListeners");
            //Make it accessible
            field.setAccessible(true);
            //Grab the listener
            ArrayList<TextWatcher> listeners = ( ArrayList<TextWatcher>) field.get(v);

            for(TextWatcher w : listeners){
                if(w instanceof TLAggressiveTextWatcher){
                    return (TLAggressiveTextWatcher) w;
                }
            }
        } catch (Throwable e) {
            TLLog.error("error getting textWatcher", e);
        }
        return null;
    }



    private static int getFalseListViewId() {
        return FALSE_LIST_VIEW_ID;
    }

    private static final int FALSE_LIST_VIEW_ID = 399293499;


    public static int getDelayViewId() {
        return DELAY_VIEW_ID;
    }

    private static final int DELAY_VIEW_ID = 399293499;


    /**
     * Used to set up a TextView at the top of the test border.
     *
     * @param activity
     * @param text     The text to fill the view
     * @param id       The id to assign to this TextView
     * @return A shiny new TextView to fill the space we need it in
     */
    private static TextView setupBorderTextView(Activity activity, String text, int id, int bottomMargin, int rightMargin, int leftMargin) {

        TextView view = new TextView(activity);
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.setMargins(leftMargin, 0, rightMargin, bottomMargin);
        view.setText(text);
        view.setTextColor(Color.WHITE);
        view.setGravity(Gravity.CENTER_HORIZONTAL);
        view.setId(id);
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        view.setLayoutParams(params);

        return view;
    }


    public enum BORDER_MODE {
        EXPERIMENT, TAP, ACTIVITY, BUTTON, DISCONNECT, NOBORDER
    }

    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels Context to get resources and device
     *           specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static int convertDpToPixel(float dp) {
        Resources resources = TLManager.getInstance().getAppContext().getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return Math.round(dp * (metrics.densityDpi / 160f));
    }

    /**
     * This method converts device specific pixels to density independent pixels.
     *
     * @param px A value in px (pixels) unit. Which we need to convert into db Context to get resources and device specific display metrics
     * @return A float value to represent dp equivalent to px value
     */
    public static int convertPixelsToDp(float px) {
        Resources resources = TLManager.getInstance().getAppContext().getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return Math.round(px / (metrics.densityDpi / 160f));
    }

    public static float convertPixelsToSp(float px) {
        Resources resources = TLManager.getInstance().getAppContext().getResources();
        float scaledDensity = resources.getDisplayMetrics().scaledDensity;
        return px / scaledDensity;
    }

    public static AlertDialog.Builder getListDialogBuilder(Activity activity, String title, String[] data, DialogInterface.OnClickListener onClickListener) {
        return new AlertDialog.Builder(activity).setTitle(title).setItems(data, onClickListener)
                .setCancelable(true);
    }

    public static boolean isBorderShowing(ViewGroup vg) {
        try {
            return vg != null && vg.findViewById(TLBorderShape.getBorderId()) != null;
        } catch (Exception e) {
            //Somehow in Kobo this caused an NPE... Don't know how, but I'm gonna catch it.
            return false;
        }
    }

    /**
     * Move a dialog window to a specified gravity.
     *
     * @param dialog  The active dialog;
     * @param gravity The gravity. ex: Gravity.BOTTOM for bottom of screen.
     */
    public static void moveDialog(AlertDialog dialog, int gravity) {
        Window window = dialog.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();

        wlp.gravity = gravity;
        wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        window.setAttributes(wlp);
    }

    /**
     * Simple check for whether or not a dialog is showing *
     */
    public static boolean dialogShowing(AlertDialog dialog) {
        return (dialog != null && dialog.isShowing());
    }

    /**
     * Get a nice collection of all the views on the screen currently. Used to iterate through everything when finding a view at a given
     * point.
     */
    public static ArrayList<View> findAllViews(ViewGroup viewGroup) {
        if (viewGroup == null) return new ArrayList<>(0);

        ArrayList<View> views = new ArrayList<>();

        try {
            if (!(viewGroup.getClass().getName().contains(DECOR_VIEW))) {
                views.add(viewGroup);
            }

            for (int i = 0, N = viewGroup.getChildCount(); i < N; i++) {
                View child = viewGroup.getChildAt(i);
                if (child instanceof ViewGroup && child.getId() != TLBorderShape.getTouchscreenId() && child.getId() != TLBorderShape.getBorderId()) {
                    views.addAll(findAllViews((ViewGroup) child));
                } else if (child != null && !(child instanceof ViewStub) && child.getId() != TLBorderShape.getTouchscreenId()
                        && child.getId() != TLBorderShape.getBorderId()) {
                    views.add(child);
                }

            }
        } catch (Exception e) {
            TLLog.error("Finding all views: ", e);
        }
        return views;
    }

    /**
     * Apply our TLHierarchyChangeListener if it has not already been applied.
     *
     * @param viewGroup
     *      parent ViewGroup
     * @param hasSupport
     *      Whether or not the support fragment library is present
     */
     private static void applyHierarchyChangeListener(ViewGroup viewGroup, boolean hasSupport){
        try {
            //We only want to apply this to basic views so we don't hit fragments or lists.
            if(viewGroup instanceof LinearLayout || viewGroup instanceof RelativeLayout || viewGroup instanceof FrameLayout || viewGroup.getClass().getSimpleName().equals("CoordinatorLayout")) {

                ViewGroup.OnHierarchyChangeListener listener;
                Class viewGroupClass = viewGroup.getClass();

                //Have to get to ViewGroup. At max this only loops twice.
                //Will only run at start of activity as well. No more taxing than our ListViews.
                while (viewGroupClass != ViewGroup.class && viewGroupClass != Object.class) {
                    viewGroupClass = viewGroupClass.getSuperclass();
                }

                if (viewGroupClass == ViewGroup.class) {

                    //Get the field
                    Field field = viewGroupClass.getDeclaredField(HIERARCHY_CHANGE_LISTENER_FIELD_NAME);
                    //Make it accessible
                    field.setAccessible(true);

                    //Grab the listener
                    listener = (ViewGroup.OnHierarchyChangeListener) field.get(viewGroup);

                    //No need to re-apply the listener if it already exists.
                    // But, we do want to replace it with our own if it exists and is not ours.
                    if (listener == null || !(listener instanceof TLHierarchyChangeListener)) {
                        //Strap our own flavor of hierarchy change listener onto the viewGroup while still keeping the old one intact (if any).
                        field.set(viewGroup, new TLHierarchyChangeListener(listener, hasSupport));
                    }
                }
            }
        } catch (Throwable e){
            TLLog.error("could not apply hierarchy change listener",e);
        }
    }

    /**
     * Helper function to check out children of a view group. Pass in 0 for the level as this is a recursive function.
     * @param viewGroup
     * @param level
     */
    public static void printOutChildren(ViewGroup viewGroup, int level) {
        String space = "";
        for (int i = 0; i < level; i++) {
            space += "-";
        }
        System.out.println(space + " " + viewGroup.getClass().getSimpleName());
        int newLevel = level + 1;
        for (int i = 0, N = viewGroup.getChildCount(); i < N; i++) {
            try {
                final View child = viewGroup.getChildAt(i);
                System.out.println(space + "- " + viewGroup.getClass().getSimpleName());
                printOutChildren((ViewGroup)child, newLevel);
            } catch (Exception e) {

            }
        }
    }


    /**
     * Run this the first time an activity loads. This simply attaches any listeners we need. *
     */
    public static void attachInitialListeners(ViewGroup viewGroup, final boolean hasSupport) {
        try {
            if (!TLManager.getInstance().getTrackingEnabled())
                return;
            if (TLViewManager.getInstance().getYoutubeClass() != null) {
                checkIfYouTube(viewGroup);
            }
            boolean mHasSupport = hasSupport;
            for (int i = 0, N = viewGroup.getChildCount(); i < N; i++) {
                try {
                    final View child = viewGroup.getChildAt(i);
                    if (child instanceof ViewGroup && child.getId() != TLBorderShape.getTouchscreenId()
                            && child.getId() != TLBorderShape.getBorderId()) {
                        if (FragmentUtils.checkIfFragment((ViewGroup) child, mHasSupport)) {
                            mHasSupport = true;
                        }
                        if (TLViewManager.getInstance().getYoutubeClass() != null) {
                            checkIfYouTube(child);
                        }
                        attachInitialListeners((ViewGroup) child, mHasSupport);

                    }
                    if (child instanceof AbsListView && !TLUtils.isDisabled(Functionality.LISTVIEWS)) {
                        //Main chunk of ListView logic
                        AbsListView listView = ((AbsListView) child);
                        AbsListView.OnScrollListener listener;
                        Class absListViewClass = AbsListView.class;
                        try {

                            //Get the field
                            Field field = absListViewClass.getDeclaredField(ON_SCROLL_LISTENER_FIELD_NAME);
                            //Make it accessible
                            field.setAccessible(true);
                            //Grab the listener
                            listener = (AbsListView.OnScrollListener) field.get(listView);

                            TLOnScrollListener scrollListener = null;
                            //No need to re-apply the listener.
                            if (!(listener instanceof TLOnScrollListener)) {
                                //Strap our own flavor of scroll listener onto the listview while still keeping the old one intact.
                                scrollListener =  new TLOnScrollListener(listener);
                                field.set(listView, scrollListener);
                            }

                            // Do the same thing for the recyclerListener
                            // This is needed as when a view is recycled the adapter may be making some assumptions about the convertView
                            // that our changes have made incorrect. This will fix that by resetting the view to its prior state before
                            // recycling it
                            field = absListViewClass.getDeclaredField(RECYCLER_FIELD_NAME);

                            field.setAccessible(true);

                            Object recycler = field.get(listView);

                            Field recyclerField = recycler.getClass().getDeclaredField(RECYCLER_LISTENER_FIELD_NAME);

                            recyclerField.setAccessible(true);

                            AbsListView.RecyclerListener recyclerListener = (AbsListView.RecyclerListener) recyclerField.get(recycler);
                            if (!(recyclerListener instanceof TLListRecyclerListener)) {
                                recyclerField.set(recycler, new TLListRecyclerListener(recyclerListener, scrollListener));
                            }

                            //check if we have already added a dataset observer by adding/modifying the tag
                            boolean hasObserver = true;
                            Object o = listView.getTag(TLViewManager.getInstance().getApplicationIconId());
                            if (o == null) {
                                HashMap map = new HashMap();
                                map.put(HAS_DATA_SET_OBSERVER_FIELD, true);
                                listView.setTag(TLViewManager.getInstance().getApplicationIconId(), map);
                                hasObserver = false;
                            } else if (o instanceof HashMap) {
                                HashMap map = (HashMap) o;
                                if (!map.containsKey(HAS_DATA_SET_OBSERVER_FIELD)) {
                                    map.put(HAS_DATA_SET_OBSERVER_FIELD, true);
                                    listView.setTag(TLViewManager.getInstance().getApplicationIconId(), map);
                                    hasObserver = false;
                                } else {
                                    hasObserver = (boolean) map.get(HAS_DATA_SET_OBSERVER_FIELD);
                                }
                            }

                            //now we are going to add a observable element to the listAdapter, this is to prevent the list items from changing
                            //out under us when people call notifyDataSetChanged. Basically this will notify us as well.
                            if (!hasObserver) {
                                listView.getAdapter().registerDataSetObserver(new TLListViewDataSetObserver(listView));
                            }
                        } catch (Exception e) {
                            TLLog.error("Problem getting OnScrollListener for ListView", e);
                        }
                    } else {
                        //Putting this in a try/catch so it will just fail out if they don't have the Recycler dependency
                        try {

                            if (child instanceof RecyclerView && !TLUtils.isDisabled(Functionality.RECYCLERVIEWS)) {
                                RecyclerView recView = ((RecyclerView) child);
                                RecyclerView.OnScrollListener listener;
                                Class recyclerViewClass = RecyclerView.class;
                                try {


                                    //Get the field
                                    Field field = recyclerViewClass.getDeclaredField(SCROLL_LISTENER_FIELD_NAME);
                                    //Make it accessible
                                    field.setAccessible(true);
                                    //Grab the listener
                                    listener = (RecyclerView.OnScrollListener) field.get(recView);


                                    TLOnRecyclerScrollListener scrollListener = null;
                                    //No need to re-apply the listener.
                                    if (!(listener instanceof TLOnRecyclerScrollListener)) {
                                        scrollListener = new TLOnRecyclerScrollListener(listener);
                                        //Strap our own flavor of scroll listener onto the RecyclerView while still keeping the old one intact.
                                        field.set(recView,scrollListener);
                                    }


                                    field = recyclerViewClass.getDeclaredField(RECYCLER_LISTENER_FIELD_NAME);

                                    field.setAccessible(true);

                                    RecyclerView.RecyclerListener recyclerListener = (RecyclerView.RecyclerListener) field.get(recView);

                                    if (!(recyclerListener instanceof TLRecyclerListener)) {
                                        field.set(recView, new TLRecyclerListener(recyclerListener, scrollListener));
                                    }

                                    //check if we have already added a dataset observer by adding/modifying the tag
                                    boolean hasObserver = true;
                                    Object o = recView.getTag(TLViewManager.getInstance().getApplicationIconId());
                                    if (o == null) {
                                        HashMap map = new HashMap();
                                        map.put(HAS_DATA_SET_OBSERVER_FIELD, true);
                                        recView.setTag(TLViewManager.getInstance().getApplicationIconId(), map);
                                        hasObserver = false;
                                    } else if (o instanceof HashMap) {
                                        HashMap map = (HashMap) o;
                                        if (!map.containsKey(HAS_DATA_SET_OBSERVER_FIELD)) {
                                            map.put(HAS_DATA_SET_OBSERVER_FIELD, true);
                                            recView.setTag(TLViewManager.getInstance().getApplicationIconId(), map);
                                            hasObserver = false;
                                        }
                                    }

                                    //now we are going to add a observable element to the listAdapter, this is to prevent the list items from changing
                                    //out under us when people call notifyDatasetChanged. Basically this will notify us as well
                                    if (!hasObserver) {
                                        recView.getAdapter().registerAdapterDataObserver(new TLRecyclerViewDataSetObserver(recView));
                                    }
                                } catch (Exception e) {
                                    TLLog.error("Problem getting OnScrollListener for RecyclerView", e);
                                }

                                //Don't apply this listener to the system window.
                            } else if (!viewGroup.getClass().getSimpleName().contains("DecorView")){
                                //Apply our hierarchy change listener to apply visual changes when views are added / removed
                                applyHierarchyChangeListener(viewGroup, hasSupport);
                            }
                        } catch (Throwable e) {
                            //They don't have recycler view, no big deal.
                        }
                    }
                } catch (Exception inner) {
                    TLLog.error("Attaching listeners:", inner);

                }
            }
        } catch (Exception outer) {
            TLLog.error("Attaching listeners:", outer);
        }
    }

    /**
     * Checks whether or not there is a youtube player on the screen.
     * If there is, we must note it and not apply the {@link TouchScreenOverlay}
     *
     * @param child Viewgroup being checked
     */
    private static void checkIfYouTube(View child) {
        if (TLViewManager.getInstance().getYoutubeClass().isInstance(child)) {
            TLViewManager.getInstance().setYoutubePresent(true);
        }
    }

    /**
     * In the event that we can't find an id (due to compilation changes), we will try to grab the view from the identifier.
     */
    private static View findViewByIdentifier(String identifier, ViewGroup vg) {
        if (identifier == null || identifier.equals("")) {
            return null;
        } else if (identifier.contains(TEXT_IDENTIFIER_ADDITION) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            try {
                String textToFind = identifier.replace(TEXT_IDENTIFIER_ADDITION, "");
                ArrayList<View> outViews = new ArrayList<>();
                vg.findViewsWithText(outViews, textToFind, View.FIND_VIEWS_WITH_TEXT);
                if (outViews.size() > 0) {
                    View foundView = outViews.get(0);
                    if (foundView.getContentDescription() == null) {
                        foundView.setContentDescription(identifier);
                    }
                    return foundView;
                } else {
                    vg.findViewsWithText(outViews, identifier, View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
                    if (outViews.size() > 0) {
                        return outViews.get(0);
                    }
                }
                return null;
            } catch (Throwable e) {
                return null;
            }
        } else {
            Context c = TLManager.getInstance().getAppContext();
            try {
                Resources res = c.getResources();
                int id = res.getIdentifier(identifier, ID, c.getPackageName());
                return vg.findViewById(id);
            } catch (Exception e) {
                return null;
            }
        }
    }

    /**
     * Turn the view into a jsonObject *
     */
    public static JSONObject getViewJSONObject(View view) throws JSONException {
        JSONObject currentView = new JSONObject();

        try {
            String identifier = getViewIdentifier(view);
            int id = getOrMakeViewId(view);

            currentView.put(ID, id);
            currentView.put(IDENTIFIER, identifier);

            if (view.getId() == View.NO_ID && identifier.equals("")) {
                return null;
            }

            Class<?> baseType = getKnownViewClassBase(view.getClass());
            String type = view.getClass().getSimpleName();

            currentView.put(BASE_CLASS_FIELD_NAME, baseType.getSimpleName());
            if (!baseType.getSimpleName().equals(type)) {
                currentView.put(CLASS_FIELD_NAME, type);
            }

            currentView.put(SUB_CLASS_FIELD_NAME, getClassHierarchy(view));

            if (TLManager.getInstance().getCurrentActivity() != null) {
                currentView.put(ACTIVITY, TLManager.getInstance().getCurrentActivitySimpleClassName());
            }

            currentView.put(METHOD_INFO_FIELD_NAME, MethodUtils.getMethodInfo(view.getClass(), view));

            currentView.put(POSITION, getViewLocation(view));

            Object fragmentId = FragmentUtils.isViewInFragment(view);
            if (fragmentId != null && (((fragmentId instanceof Integer) && (int) fragmentId != -2) || ((fragmentId instanceof String) && !fragmentId.equals("")))) {
                currentView.put(FRAG_ID, fragmentId);
                String fragIdentifier = TLFragmentManager.getInstance().getFragmentsOnScreen().get(fragmentId).second;
                currentView.put(FRAG_IDENTIFIER, fragIdentifier);

                try {
                    //Have to identify these as separate 'cells' to stop overriding of same id views.
                    if (fragIdentifier.contains(VIEWPAGER_CLASS_NAME)) {
                        JSONObject cellInfo = new JSONObject();
                        String[] bits = fragIdentifier.split("_");
                        if (bits.length > 0) {
                            cellInfo.put(POSITION, Integer.valueOf(bits[bits.length - 1]));
                            currentView.put(CELL_INFO, cellInfo);
                        }
                    }
                } catch (Exception e) {
                    TLLog.debug("error setting viewpager fragment id");
                }
            }

            AbsListView listView = getListView(view);
            if (listView != null) {
                currentView.put(IS_IN_LIST_VIEW, true);
                int position = getListViewPosition(listView, view);
                Adapter adapter = listView.getAdapter();
                boolean hasHeader = false;
                if (adapter != null && adapter.getCount() > 0 && adapter.getItemViewType(position) != AbsListView.ITEM_VIEW_TYPE_HEADER_OR_FOOTER) {
                    if (adapter.getItemViewType(0) == AbsListView.ITEM_VIEW_TYPE_HEADER_OR_FOOTER) {
                        hasHeader = true;
                    }
                    if (position != ListView.INVALID_POSITION) {
                        JSONObject cellInfo = new JSONObject();
                        if (hasHeader)
                            cellInfo.put(POSITION, getListViewPosition(listView, view) - 1);
                        else
                            cellInfo.put(POSITION, getListViewPosition(listView, view));
                        String listIdentifier = TLManager.getInstance().getAppContext().getResources().getResourceEntryName(listView.getId());
                        cellInfo.put(LIST_IDENTIFIER, listIdentifier);
                        currentView.put(CELL_INFO, cellInfo);
                    }
                }
            }

            try {
                RecyclerView recView = getRecView(view);
                if (recView != null) {
                    currentView.put(IS_IN_RECYCLER_VIEW, true);
                    int position = getRecViewPosition(recView, view);
                    if (position != RecyclerView.NO_POSITION) {
                        JSONObject cellInfo = new JSONObject();
                        cellInfo.put(POSITION, getRecViewPosition(recView, view));
                        String listIdentifier = TLManager.getInstance().getAppContext().getResources().getResourceEntryName(recView.getId());
                        cellInfo.put(LIST_IDENTIFIER, listIdentifier);
                        currentView.put(CELL_INFO, cellInfo);
                    }
                }
            } catch (Throwable t) {
                //They dont have recycler views
                TLLog.error("recview err", (t instanceof Exception) ? (Exception) t : null);
            }


            String imgFileName = ScreenshotUtils.captureScreenshot(view);
            currentView.put(IMAGE_FILE_NAME_FIELD, imgFileName);

            //Now if they have onClickListeners
            if (Build.VERSION.SDK_INT >= 15) {
                currentView.put(HAS_ON_CLICK_FIELD, view.hasOnClickListeners());
            }

        } catch (Exception e) {
            return currentView;
        }
        return currentView;
    }

    /**
     * @return a JSONObject containing the location of the view. endX, endY, startX, and startY, and the width/height of the screen in
     * pixels
     */
    private static JSONObject getViewLocation(View view) throws JSONException {
        JSONObject positionObject = new JSONObject();
        JSONObject screenObject = new JSONObject();

        int loc[] = new int[2];
        view.getLocationOnScreen(loc);
        positionObject.put(POSITION_START_X, loc[0]);
        positionObject.put(POSITION_START_Y, loc[1]);
        positionObject.put(POSITION_END_X, loc[0] + view.getWidth());
        positionObject.put(POSITION_END_Y, loc[1] + view.getHeight());

        if (TLManager.getInstance().getCurrentActivity() != null) {
            WindowManager w = TLManager.getInstance().getCurrentActivity().getWindowManager();
            Display d = w.getDefaultDisplay();
            DisplayMetrics metrics = new DisplayMetrics();
            d.getMetrics(metrics);

            int widthPixels = 0;
            int heightPixels = 0;

            try {
                widthPixels = metrics.widthPixels;
                heightPixels = metrics.heightPixels;
            } catch (Exception ignored) {
            }
            // includes window decorations (statusbar bar/menu bar)
            if (Build.VERSION.SDK_INT >= 14 && Build.VERSION.SDK_INT < 17)
                try {
                    widthPixels = (Integer) Display.class.getMethod(GET_RAW_WIDTH_METHOD_NAME).invoke(d);
                    heightPixels = (Integer) Display.class.getMethod(GET_RAW_HEIGHT_METHOD_NAME).invoke(d);
                } catch (Exception ignored) {
                    TLLog.error("getting screen dimensions, but probably fine to ignore", ignored);
                }

            // includes window decorations (statusbar bar/menu bar)
            if (Build.VERSION.SDK_INT >= 17)
                try {
                    Point realSize = new Point();
                    Display.class.getMethod(GET_REAL_SIZE_METHOD_NAME, Point.class).invoke(d, realSize);
                    widthPixels = realSize.x;
                    heightPixels = realSize.y;
                } catch (Exception ignored) {
                }
            screenObject.put(HEIGHT, widthPixels);
            screenObject.put(WIDTH, heightPixels);
        }
        positionObject.put(SCREEN_DIMENSIONS_FIELD, screenObject);
        TLLog.debug(positionObject.toString());

        return positionObject;
    }

    /**
     * @return a JSONArray of the entire class hierarchy of the given view, as simple names *
     */
    private static JSONArray getClassHierarchy(View v) {
        JSONArray viewClassHierarchy = new JSONArray();
        try {
            Class c = v.getClass();
            while (c != null && !c.getSimpleName().equals(VIEW)) {
                viewClassHierarchy.put(c.getSimpleName());
                c = c.getSuperclass();
            }
            if (c != null) {
                viewClassHierarchy.put(c.getSimpleName());
            }
        } catch (Exception e) {
            TLLog.error("Class hierarchy: ", e);
        }
        return viewClassHierarchy;
    }

    public static String getViewHierarchy( View v) {
        StringBuilder desc = new StringBuilder();
        getViewHierarchy(v, desc, 0);
        return desc.toString();
    }

    private static void getViewHierarchy(View v, StringBuilder desc, int margin) {
        desc.append(getViewMessage(v, margin));
        if (v instanceof ViewGroup) {
            margin++;
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                getViewHierarchy(vg.getChildAt(i), desc, margin);
            }
        }
    }

    private static String getViewMessage(View v, int marginOffset) {
        String repeated = new String(new char[marginOffset]).replace("\0", "  ");
        try {
            String resourceId = v.getResources() != null ? (v.getId() > 0 ? v.getResources().getResourceName(v.getId()) : "no_id") : "no_resources" + ((v instanceof TextView)?(" || text: "+(((TextView) v).getText().toString())):(""));
            return repeated + "[" + v.getClass().getSimpleName() + "] " + resourceId + "\n";
        } catch (Resources.NotFoundException e) {
            return repeated + "[" + v.getClass().getSimpleName() + "] name_not_found\n";
        }
    }

    /**
     * Get the _known_ view class base *
     */
    private static Class<?> getKnownViewClassBase(Class<?> viewClass) {
        Class<?> base = null;
        if (TLMethodManager.getInstance().getKnownClasses().contains(viewClass.getSimpleName())) {
            return viewClass;
        } else if (!viewClass.getSimpleName().equals(OBJECT)) {
            base = getKnownViewClassBase(viewClass.getSuperclass());
        }
        return base;
    }


    /**
     * Function to update all current list/recyclerview known positions.
     *
     * This informs the listviews to re-check the known items in a list that are to be changed when switching variations etc in a debug / liveupdate setting.
     *
     * While this looks ridiculous, previously we just re-calculated on _every_ scroll of a listview. This bogged apps down a LOT in dev mode. This is much better.
     *
     * @param knownRecyclerPositions
     */
    public static void resetListItemPositionsIfNecessary(HashMap<String, HashSet<Integer>> knownRecyclerPositions) {
        if (TLManager.getInstance().getIsActive() && TLManager.getInstance().isLiveUpdate() && TLViewManager.getInstance().getCurrentViewGroup() != null){
                for(String identifier : knownRecyclerPositions.keySet()){
                    try {
                        View v = findViewByIdentifier(identifier, TLViewManager.getInstance().getCurrentViewGroup());
                        if (v != null) {
                            if (v instanceof AbsListView) {
                                //Get the field
                                Field field = v.getClass().getDeclaredField(ON_SCROLL_LISTENER_FIELD_NAME);
                                //Make it accessible
                                field.setAccessible(true);
                                //Grab the listener
                                AbsListView.OnScrollListener listener = (AbsListView.OnScrollListener) field.get(v);
                                if (listener != null && (listener instanceof TLOnScrollListener)) {
                                    ((TLOnScrollListener) listener).resetItems();
                                }
                            } else if (v instanceof RecyclerView) {
                                //Get the field
                                Field field = v.getClass().getDeclaredField(SCROLL_LISTENER_FIELD_NAME);
                                //Make it accessible
                                field.setAccessible(true);
                                //Grab the listener
                                RecyclerView.OnScrollListener listener = (RecyclerView.OnScrollListener) field.get(v);
                                if (listener != null && (listener instanceof TLOnRecyclerScrollListener)) {
                                    //Reset the item list on this scroll listener
                                    ((TLOnRecyclerScrollListener) listener).resetItems();
                                }
                            }
                        }
                    } catch (Throwable t){
                        TLLog.error("Error getting list class info");
                    }
                }
        }
    }

    /**
     * Set all the visual properties given back to us from the server, if any *
     */
    public static void setProperties(final ViewGroup vg) throws JSONException {
        try {
            if (vg != null
                    && !TLUtils.isDisabled(Functionality.VISUAL)
                    && TLManager.getInstance().getTlProperties() != null
                    && TLManager.getInstance().getTrackingEnabled()) {
                TLLog.debug("Setting properties to view: " + vg.getClass().getSimpleName()+"Current main viewgroup is "+TLViewManager.getInstance().getCurrentViewGroup().getClass().getSimpleName());

                // Get a list of the views in the current activity
                final JSONArray views = TLManager.getInstance().getTlProperties().getViews();
                final HashMap<String, String> activityMap = TLManager.getInstance().getTlProperties().getActivityMap();

                for (Object key : TLViewManager.getInstance().getViewsToReset().keySet()) {
                    views.put(TLViewManager.getInstance().getViewsToReset().get(key));
                }



                if (views != null) {
                    JSONObject initProps;
                    for (int i = views.length() - 1; i >= 0; i--) {
                        final JSONObject viewObject = (JSONObject) views.opt(i);

                        // Get an array of the properties given back to us
                        try {
                            initProps = viewObject.optJSONObject(INIT_PROPERTIES);
                            if (initProps == null) {
                                continue;
                            }
                        } catch (Exception e) {
                            continue;
                        }
                        String activityId = viewObject.optString(ACTIVITY_ID_FIELD);
                        Object fragId = initProps.opt(FRAG_ID);
                        String fragIdentifier = initProps.optString(FRAG_IDENTIFIER);

                    /*
                     * We have two maps -- an activity map and a fragment map. This block first checks that the view is on the right
					 * activity. Then, we check if there are any fragment identifications, and if there are, check if we're on the correct
					 * fragment.
					 */
                        HashMap<Object, Triplet<String, String, Boolean>> fragmentsOnScreen = TLFragmentManager.getInstance().getFragmentsOnScreen();
                        boolean correctActivity = activityMap.containsKey(activityId) &&
                                activityMap.get(activityId).equals(TLManager.getInstance().getCurrentActivitySimpleClassName());

                        boolean hasFragIdentifier = fragIdentifier != null;
                        boolean fragIdentifierCheck = false;
                        if (hasFragIdentifier) {
                            fragIdentifierCheck = fragmentsOnScreen.size() > 0 && fragmentsOnScreen.containsKey(fragId) && fragmentsOnScreen.get(fragId).second.equals(fragIdentifier);
                        }

                        boolean fragIdCheck = fragIdentifierCheck || fragId == null || (fragId instanceof Integer && ((Integer) fragId == 0 || checkFragMapForTag(fragIdentifier)));

                        boolean viewPagerCheck = ((fragmentsOnScreen.containsKey(fragId) &&
                                (!TLFragmentManager.getInstance().getViewPagerFragments().isEmpty())
                                || TLFragmentManager.getInstance().isChangingFragmentNames()));

                        if (correctActivity && ((fragIdCheck || TLViewManager.getInstance().hasAggressiveViewChanges()) || viewPagerCheck)) {


                            final int id = initProps.optInt(VIEW_ID);
                            final String anIdentifier = initProps.optString(ANDROID_IDENTIFIER);
                            boolean listItem = initProps.has(IS_IN_LIST_VIEW);
                            final boolean recyclerItem = initProps.has(IS_IN_RECYCLER_VIEW);
                            final boolean cellElement = viewObject.optBoolean(IS_CELL_ELEMENT);
                            JSONObject cellInfo = initProps.optJSONObject(CELL_INFO);
                            final boolean reset = viewObject.optBoolean(RESET_FIELD, false);

                            // If we have a list item, we want to delay the property changes by like 11ms to allow the views to load first.
                            // If we have a list item IN a fragment, then we need to wait way longer for the fragment then other thing to load.
                            // However, the fragment thing might only be true for Wordpress. So come back to look at this sometime.
                            TLLog.debug("now looking at view with props: id: "+id+" anidentifier: "+anIdentifier+" listItem:"+listItem+" recyclerItem: "+recyclerItem+" cellElement: "+cellElement+" cellInfo:"+(cellInfo!=null?cellInfo.toString():"nope")+" reset: "+reset);
                            try {
                                if ((listItem || recyclerItem) && cellElement) {
                                    if (checkIfFirstTime(vg)) {
                                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                applyMethodsToListItems(vg, id, viewObject, reset, anIdentifier);
                                            }
                                        }, (fragId instanceof Integer && (Integer) fragId == 0 || fragId == null) ? 10 : 285);
                                    }
                                    applyMethodsToListItems(vg, id, viewObject, reset, anIdentifier);
                                } else if ((listItem || recyclerItem) && cellInfo != null && cellInfo.length() > 0) {
                                    final int position = cellInfo.optInt(POSITION);
                                    View v = findViewByIdOrIdentifier(id, anIdentifier, vg);
                                    if (v != null) {
                                        Object obj = v.getTag(TLViewManager.getInstance().getApplicationIconId());
                                        HashMap map = (obj != null && obj instanceof HashMap) ? (HashMap) obj : new HashMap();
                                        map.put(SHOULD_RESET, true);
                                        v.setTag(TLViewManager.getInstance().getApplicationIconId(), map);
                                    }

                                    if (position != ListView.INVALID_POSITION && checkIfFirstTime(vg)) {
                                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                applyMethodsToCell(recyclerItem, position, vg, id, viewObject, reset, anIdentifier);
                                            }
                                        }, (fragId instanceof Integer && (Integer) fragId == 0 || fragId == null) ? 10 : 285);
                                    }
                                    applyMethodsToCell(recyclerItem, position, vg, id, viewObject, reset, anIdentifier);

                                } else if (fragIdentifier != null && !fragIdentifier.equals("")) {
                                    applyMethodsToViewInFragment(vg, id, fragIdentifier, viewObject, fragId, reset, anIdentifier);
                                } else {
                                    applyMethodsToView(vg, id, viewObject, reset, anIdentifier);
                                }
                            } catch (Exception e) {
                                TLLog.error("Some issues with lists or something", e);
                            }

                        }
                    }
                }

            }
        } catch (Exception e)

        {
            TLLog.error("Critical: SetProps didn't work", e);
        }

    }

    private static boolean checkFragMapForTag(String fragIdentifier) {
        try {
            HashMap<Object, Triplet<String, String, Boolean>> map = TLFragmentManager.getInstance().getFragmentsOnScreen();
            for (Map.Entry<Object, Triplet<String, String, Boolean>> entry : map.entrySet()) {
                try {
                    if (entry.getValue().second.equals(fragIdentifier)) {
                        return true;
                    }
                } catch (Exception e) {
                    //next;
                }
            }

        } catch (Exception e) {
            TLLog.error("Exception checking fragment map for tag");
        }
        return false;
    }

    /**
     * uses findViewById but falls back to the identifier if necessary.
     */
    private static View findViewByIdOrIdentifier(int id, Object anIdentifier, ViewGroup vg) {
        if (anIdentifier instanceof String) {
            return findViewByIdentifier((String) anIdentifier, vg) == null ? vg.findViewById(id) : findViewByIdentifier((String) anIdentifier, vg);
        } else {
            return vg.findViewById(id);
        }
    }

    /**
     * Utility method used to get a view's ID when its necessary to generate one if it doesn't exist.
     * <p>
     * Works only on TextViews as it uses a hash of the text on the item. Will return {$View#NO_ID}
     *
     * @param view
     * @return id (View.NO_ID if none present)
     */
    static int getOrMakeViewId(View view) {
        int id = View.NO_ID;
        if (view != null) {
            try {
                id = view.getId();
                if (id == View.NO_ID) {
                    String identifier = getViewIdentifier(view);
                    if (!identifier.equals("")) {
                        id = Math.abs(identifier.hashCode());
                    }
                }
            } catch (Throwable e) {
                // Just keep -1;
                TLLog.error("Error getting view ID: ", e);
            }
        }
        return id;
    }

    /**
     * Utility method used to get a view's identifier when its necessary to generate one if it doesn't exist.
     * <p>
     * Works only on TextViews as it uses the text on the item .
     *
     * @param view
     * @return identifier (empty string if none present)
     */
    private static String getViewIdentifier(View view) {
        String identifier = "";
        try {
            identifier = TLManager.getInstance().getAppContext().getResources().getResourceEntryName(view.getId());
        } catch (Throwable e) {
            // Just keep ""
            TLLog.error("Error getting identifier: ", e);
        }
        //If we have a textview, set the identifier as the text of that textview so we can find that.
        try {
            if (identifier.equals("")) {
                if (view instanceof TextView) {
                    String text = (String) ((TextView) view).getText();
                    if (text != null && !text.equals("")) {
                        identifier = TEXT_IDENTIFIER_ADDITION + text;
                    }
                }
            }
        } catch (Throwable e) {
            TLLog.error("Error creating identifier: ", e);
        }
        return identifier;
    }


    private static void applyMethodsToCell(boolean recyclerView, int position, ViewGroup vg, int id,
                                           final JSONObject viewObject, boolean reset, String anIdentifier) {
        try {
            if ((recyclerView && TLUtils.isDisabled(Functionality.RECYCLERVIEWS)) || (!recyclerView && TLUtils.isDisabled(Functionality.LISTVIEWS)))
                return;
            View v = findViewByIdOrIdentifier(id, anIdentifier, vg);
            if (v != null) {
                View list = recyclerView ? getRecView(v) : getListView(v);
                if (list != null) {
                    View itemView = recyclerView ? getRecViewByPositionInRecyclerView(position, (RecyclerView) list) : getViewByPositionInListView(position, (ListView) list);
                    if (itemView != null) {
                        View viewItem = null;
                        //If not a viewgroup, its just a list of views. Confirm this one is correct.
                        if (!(itemView instanceof ViewGroup)) {
                            Context c = TLManager.getInstance().getAppContext();
                            int itemId = c.getResources().getIdentifier(anIdentifier, ID, c.getPackageName());
                            if (id == itemId) {
                                viewItem = itemView;
                            }
                        } else {
                            viewItem = findViewByIdOrIdentifier(id, anIdentifier, (ViewGroup) itemView);
                        }
                        if (viewItem != null) {
                            applyMethods(viewObject, viewItem, reset, true);
                            TLLog.debug("made it! applying methods to cell");
                        }
                    }
                }
            }
        } catch (Throwable t) {
            //Safe
        }
    }

    /**
     * Because we know the item is in a listview, what we do is change the id of every view we change to 0, and store the old id. That way
     * we can keep doing a findViewById and not get the same id every time. Then, we reset the id after we apply our methods.
     */
    private static void applyMethodsToListItems(ViewGroup vg, int id, JSONObject viewObject, boolean reset, String anIdentifier) {
        if (vg instanceof AbsListView) {
            if (TLUtils.isDisabled(Functionality.LISTVIEWS)) {
                return;
            }
        } else {
            try {
                if (vg instanceof RecyclerView) {
                    if (TLUtils.isDisabled(Functionality.RECYCLERVIEWS)) {
                        return;
                    }
                }
            } catch (Exception e) {
                //Don't have RecyclerViews
            }
        }
        ArrayList<Pair<View, Integer>> viewsWithSameId = new ArrayList<>();

        while (findViewByIdOrIdentifier(id, anIdentifier, vg) != null) {
            try {
                // Create a view object
                View v = findViewByIdOrIdentifier(id, anIdentifier, vg);
                if (v != null) {

                    //add non-reset tag
                    Object obj = v.getTag(TLViewManager.getInstance().getApplicationIconId());
                    HashMap map = (obj != null && obj instanceof HashMap) ? (HashMap) obj : new HashMap();
                    map.put(SHOULD_RESET, false);
                    v.setTag(TLViewManager.getInstance().getApplicationIconId(), map);

                    applyMethods(viewObject, v, reset, true);
                    viewsWithSameId.add(new Pair<>(v, ViewUtils.getOrMakeViewId(v)));
                    v.setId(0);
                }
            } catch (Throwable t) {
                //List header probably got caught in the mix
            }
        }

        for (Pair<View, Integer> p : viewsWithSameId) {
            try {
                (p.first).setId(p.second);
            } catch (Throwable t) {
                //Don't fail out of the loop
            }

        }
    }

    /**
     * Simply get all the info we need then apply our methods to the view *
     */
    private static void applyMethods(JSONObject viewObject, View v, boolean reset, boolean addTag) {

        try {
            JSONObject anProps = viewObject.optJSONObject(ANDROID_PROPERTIES);
            if (anProps != null) {
                Iterator<?> keys = anProps.keys();

                // Iterate through all the methods, pass their values to be applied
                while (keys.hasNext()) {
                    String methodName = (String) keys.next();
                    try {
                        if (anProps.get(methodName) instanceof JSONObject) {
                            JSONObject method = anProps.optJSONObject(methodName);
                            Object value = method.opt(VALUE);
                            String parameterType = method.optString(TYPE);
                            MethodUtils.applyMethod(v, methodName, value, parameterType, reset, addTag);
                            if (reset) {
                                TLViewManager.getInstance().getViewsToReset().remove(viewObject.optJSONObject(INIT_PROPERTIES).optInt(VIEW_ID));
                            }
                        }
                    } catch (Throwable t) {
                        TLLog.error("methods", t);
                    }
                }

            }
        } catch (Throwable t) {
            TLLog.warning("Problem applying methods", (t instanceof Exception) ? (Exception) t : new Exception());
        }
    }


    private static void applyMethodsToView(ViewGroup vg, int id, JSONObject viewObject, boolean reset, String anIdentifier) {
        {
            if (vg != null) {
                // Create a view object
                View v = findViewByIdOrIdentifier(id, anIdentifier, vg);

                if (v != null) {
                    // apply
                    TLLog.debug("Found View: " + v.getClass().getSimpleName() + " " + anIdentifier);
                    applyMethods(viewObject, v, reset, false);
                }
            }
        }
    }

    /**
     * Apply methods to a vgiew contained in a fragment.
     * <p>
     * If the view we're looking for is known to be in a fragment, things can act differently.
     *
     * @param vg             The ViewGroup. Will either be the root view OR a viewpager.
     * @param id             Id from server.
     * @param fragIdentifier Identifier for fragment. Should contain _viewpager_ if its a viewpager.b
     * @param viewObject     The necessary View JSONObject from TlProperties for applying methods.
     * @param anIdentifier
     */
    private static void applyMethodsToViewInFragment(ViewGroup vg, int id, String fragIdentifier, JSONObject viewObject, Object fragId, boolean reset, String anIdentifier) {

        if (!TLUtils.isDisabled(Functionality.SUPPORTFRAGMENTS) && !TLUtils.isDisabled(Functionality.FRAGMENTS)) {
            try {
                //Do some magic if we're a viewpager.
                if (!TLUtils.isDisabled(Functionality.VIEWPAGERS) && fragIdentifier != null && !fragIdentifier.equals("") && fragIdentifier.contains("_viewpager_")) {
                    ViewPager pager;
                    //The GlobalLayoutListener puts us here with the pager itself as the ViewGroup.
                    //Regular usage puts us here without the parent ViewGroup.
                    final ViewGroup currentViewGroup = TLViewManager.getInstance().getCurrentViewGroup();
                    if (vg instanceof ViewPager) {
                        pager = (ViewPager) vg;
                    } else if (currentViewGroup != null && findViewByIdOrIdentifier(id, fragId, vg) instanceof ViewPager) {
                        pager = (ViewPager) findViewByIdOrIdentifier(id, fragId, currentViewGroup);
                    } else {
                        pager = findViewPagerRecursively(vg);
                        if (pager == null) {
                            return;
                        }
                    }
                    //Grab the adapter from the viewpager
                    PagerAdapter adapter = pager.getAdapter();

                    //Grab the number we got
                    String bits[] = fragIdentifier.split("_");
                    //Pray to the proper gods its an integer
                    int number = Integer.valueOf(bits[bits.length - 1]);

                    ViewGroup fromPager = null;
                    for (int i = number - 2 < 0 ? 0 : number - 2; i < pager.getChildCount(); i++) {
                        Object tracker = pager.getChildAt(i).getTag(TLFragmentManager.getInstance().getFragmentTrackingId());
                        if (tracker != null) {
                            if (pager.getChildAt(i).getTag(TLFragmentManager.getInstance().getFragmentTrackingId()).equals(fragIdentifier)) {
                                fromPager = (ViewGroup) pager.getChildAt(i);
                            }
                        }
                    }
                    if (fromPager == null) {
                        Fragment frag = FragmentUtils.findFragmentInViewPager(pager, adapter, number);
                        //Grab the view from the fragment!
                        if (frag != null && frag.getView() != null && frag.getView() instanceof ViewGroup) {
                            vg = (ViewGroup) frag.getView();
                        } else {
                            return;
                        }
                    } else {
                        vg = fromPager;
                    }
                }

            } catch (Throwable e) {
                TLLog.error("vp fail", e);
                //Just fall back to regular fragments in this worst case.
            }
            try {
                // Create a view object
                if (vg != null) {
                    View v = findViewByIdOrIdentifier(id, anIdentifier, vg);


                    if (v != null) {
                        // apply
                        TLLog.debug("Found View: " + v.getClass().getSimpleName());
                        applyMethods(viewObject, v, reset, false);
                    }
                }
            } catch (Throwable e) {
//
            }
        }
    }

    public static View findViewFromJSON(JSONObject tapElement) {
        try {
            //Grab our ID
            Integer id = Integer.valueOf(tapElement.optString(VIEW_ID));
            String identifier = tapElement.optString(ANDROID_IDENTIFIER);

            //Get the list info
            boolean listItem = tapElement.has(IS_IN_LIST_VIEW);
            final boolean recyclerItem = tapElement.has(IS_IN_RECYCLER_VIEW);
            JSONObject cellInfo = tapElement.optJSONObject(CELL_INFO);


            //Work our way down to the cell item.
            final ViewGroup currentViewGroup = TLViewManager.getInstance().getCurrentViewGroup();
            if (currentViewGroup == null) return null;

            if ((listItem || recyclerItem) && cellInfo != null && cellInfo.length() > 0) {
                final int position = cellInfo.optInt(POSITION);
                if (position != ListView.INVALID_POSITION) {
                    if (listItem) {

                        //So, getListView actually works upwards, not downwards (Faster than traversing from top to bottom). Make sure to pass the view itself to the getListView method.
                        ListView listView = (ListView) getListView(findViewByIdOrIdentifier(id, identifier, currentViewGroup));
                        if (listView != null) {
                            View cell = getViewByPositionInListView(position, listView);
                            if (cell != null) {
                                return findViewByIdOrIdentifier(id, identifier, (ViewGroup) cell);
                            }
                        }
                    } else {
                        RecyclerView recyclerView = getRecView(findViewByIdOrIdentifier(id, identifier, currentViewGroup));
                        if (recyclerView != null) {
                            View cell = getRecViewByPositionInRecyclerView(position, recyclerView);
                            if (cell != null) {
                                return findViewByIdOrIdentifier(id, identifier, (ViewGroup) cell);
                            }
                        }
                    }
                }
            }
            return findViewByIdOrIdentifier(id, identifier, currentViewGroup);
        } catch (Throwable e) {
            TLLog.error("error finding view from json", e);
        }
        return null;
    }

    private static ViewPager findViewPagerRecursively(ViewGroup vg) {
        ViewPager pager = null;

        if (vg.getChildCount() > 0) {
            for (int i = 0; i < vg.getChildCount(); i++) {
                View v = vg.getChildAt(i);
                if (v instanceof ViewPager) {
                    pager = (ViewPager) v;
                } else if (v instanceof ViewGroup) {
                    pager = findViewPagerRecursively((ViewGroup) v);
                }
            }
        }
        return pager;
    }


    /**
     * Determines if given points are inside view
     *
     * @param x x coordinate of touched point
     * @param y y coordinate of touched point
     * @return true if the points are within view bounds, false otherwise
     */
    public static ArrayList<View> getViewsAtPoint(float x, float y, ViewGroup vg) {

        ArrayList<View> viewList = new ArrayList<>();
        ArrayList<View> currentViews = findAllViews(vg);
        ListIterator<View> it = currentViews.listIterator();

        while (it.hasNext()) {
            try {
                View view = it.next();

                if (viewList.contains(view))
                    continue;

                int location[] = new int[2];
                view.getLocationInWindow(location);
                int viewX = location[0];
                int viewY = location[1];

                // point is inside view bounds
                if ((x > viewX && x < (viewX + view.getWidth())) && (y > viewY && y < (viewY + view.getHeight()))) {
                    if (view instanceof AbsListView) {
                        View child;
                        int childLoc[] = new int[2];

                        int childCount = ((AbsListView) view).getChildCount();
                        for (int i = 0; i < childCount; i++) {
                            child = ((AbsListView) view).getChildAt(i);
                            View vu = new View(vg.getContext());

                            child.getLocationOnScreen(childLoc);
                            int childX = childLoc[0];
                            int childY = childLoc[1];
                            if (((x > childX && x < (childX + view.getWidth())) && (y > childY && y < (childY + view.getHeight())))) {
                                if (child instanceof ViewGroup) {
                                    ArrayList<View> listChildren = ViewUtils.findAllViews((ViewGroup) child);
                                    vu.setTag(i);
                                    vu.setId(getFalseListViewId());
                                    ((ViewGroup) child).addView(vu);
                                    for (View v : listChildren) {
                                        it.add(v);
                                    }
                                } else {
                                    it.add(child);
                                }
                                break;
                            }

                        }

                    }
                    if (!viewList.contains(view)) {
                        viewList.add(view);
                    }

                }
            } catch (Throwable t) {
                //Getting view locations
            }
        }
        Collections.reverse(viewList);
        return viewList;
    }

    /**
     * Returns any view below this point that has onclick listeners.
     *
     * @param x  x coordinate of touched point
     * @param y  y coordinate of touched point
     * @param vg
     * @return true if the points are within view bounds, false otherwise
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    public static ArrayList<View> getClickablesAtPoint(float x, float y, ArrayList<View> vg, boolean dialog) {
        ArrayList<View> views = new ArrayList<>();
        if (vg != null) {
            for (View view : vg) {
                try {
                    if (view instanceof Button || ((Build.VERSION.SDK_INT >= 15) && view.hasOnClickListeners())) {
                        int location[] = new int[2];
                        int viewX;
                        long viewY;
                        if (!dialog) {
                            view.getLocationOnScreen(location);
                            viewX = location[0];
                            viewY = location[1] - getStatusBarHeight(TLManager.getInstance().getAppContext());
                        } else {
                            view.getLocationInWindow(location);
                            viewX = location[0];
                            viewY = view.getTop() - getStatusBarHeight(TLManager.getInstance().getAppContext());
                        }
                        // point is inside view bounds
                        if ((x > viewX && x < (viewX + view.getWidth())) && (y > viewY && y < (viewY + view.getHeight())) && view.getId() != -1) {
                            views.add(view);
                        }

                    }
                } catch (Throwable e) {
                    TLLog.error("problem getting clickable view", e);
                }
            }
        }
        return views;
    }

    /**
     * @return height of status bar
     */
    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier(STATUS_BAR_HEIGHT, STATUS_BAR_DIMENSION, STATUS_BAR_ANDROID);
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    public static BorderFrameLayout getBorderFrame(BORDER_MODE mode) {

        BorderFrameLayout border = new BorderFrameLayout(TLManager.getInstance().getAppContext());
        try {
            FrameLayout.LayoutParams borderParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT);

            // Need to push down the top border to be below status bar
            borderParams.setMargins(0, mode == BORDER_MODE.NOBORDER ? 0 : ViewUtils.getStatusBarHeight(TLManager.getInstance().getAppContext()), 0, 0);

            if (mode != BORDER_MODE.NOBORDER) {
                if (android.os.Build.VERSION.SDK_INT >= 16) {
                    border.setBackground(new TLBorderShape(Colors.getBorderColor(mode), TLBorderShape.STROKE_WIDTH));
                } else {
                    border.setBackgroundDrawable(new TLBorderShape(Colors.getBorderColor(mode), TLBorderShape.STROKE_WIDTH));
                }
            }
            border.setLayoutParams(borderParams);
            border.setId(TLBorderShape.getBorderId());
            border.setTag(mode.name());
        } catch (Exception e) {
            TLLog.error("border", e);
        }
        return border;
    }

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    public static BorderFrameLayout getBorderOverlayForDialog() {

        BorderFrameLayout border = new BorderFrameLayout(TLManager.getInstance().getAppContext());
        try {
            FrameLayout.LayoutParams borderParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT);

            // Need to push down the top border to be below status bar
            borderParams.setMargins(0, 0, 0, 0);

        } catch (Throwable e) {
            TLLog.error("border", e);
        }
        return border;
    }

    public static TouchScreenOverlay getTouchscreen(boolean dialog) {
        TouchScreenOverlay screen = new TouchScreenOverlay(TLManager.getInstance().getAppContext(), dialog);
        FrameLayout.LayoutParams borderParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);

        // Need to push down the top border to be below status bar
        borderParams.setMargins(0, ViewUtils.getStatusBarHeight(TLManager.getInstance().getAppContext()), 0, 0);

        screen.setLayoutParams(borderParams);
        screen.setId(TLBorderShape.getTouchscreenId());

        return screen;
    }

    /**
     * The top textview of the border
     *
     * @return A fully initialized TextView to fill a space in the border.
     */
    public static RelativeLayout getBorderTextViewLayout(Activity activity, BORDER_MODE mode, String experimentName, String variationName) {

        RelativeLayout borderLinearLayout = new RelativeLayout(TLManager.getInstance().getAppContext());
        LinearLayout centerLinearLayout = new LinearLayout(TLManager.getInstance().getAppContext());
        LinearLayout leftLinearLayout = new LinearLayout(TLManager.getInstance().getAppContext());
        LinearLayout rightLinearLayout = new LinearLayout(TLManager.getInstance().getAppContext());

        // borderLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
        centerLinearLayout.setOrientation(LinearLayout.VERTICAL);

        // Add the textViews
        String topText;
        String bottomText;

        if (mode.equals(BORDER_MODE.EXPERIMENT)) {

            topText = ("Experiment: " + (experimentName == null || experimentName.equals("") ? "loading..." : experimentName));
            bottomText = ("Variation: " + (variationName == null || variationName.equals("") ? "loading..." : variationName));

        } else {
            topText = TLBorderShape.getTopText(mode);
            bottomText = TLBorderShape.getBottomText(mode);
        }

        centerLinearLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                TLDialogManager.getInstance().setupExperimentDialog();
                return true;
            }
        });

        if (TLManager.getInstance().isUpdateAvailable()) {
            TextView updateView = ViewUtils.setupBorderTextView(activity, "Taplytics SDK\nUpdate Available", 0,
                    0, ViewUtils.convertDpToPixel(TLBorderShape.STROKE_WIDTH), 0);
            updateView.setGravity(Gravity.CENTER);
            updateView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8);
            rightLinearLayout.addView(updateView);
            rightLinearLayout.setGravity(Gravity.END);
        }

        TextView topView = ViewUtils.setupBorderTextView(activity, topText, TLBorderShape.getTopTextId(), -8, 0, 0);
        TextView bottomView = ViewUtils.setupBorderTextView(activity, bottomText, TLBorderShape.getBottomTextId(), 0, 0, 0);

        centerLinearLayout.addView(topView);
        centerLinearLayout.addView(bottomView);

        // Center
        RelativeLayout.LayoutParams centerLayoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        centerLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        centerLinearLayout.setLayoutParams(centerLayoutParams);

        // leftLinearLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
        // ViewGroup.LayoutParams.WRAP_CONTENT));

        RelativeLayout.LayoutParams rightLayoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        rightLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        rightLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL);

        rightLinearLayout.setLayoutParams(rightLayoutParams);

        borderLinearLayout
                .setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.convertDpToPixel(TLBorderShape.TOP_HEIGHT)));

        borderLinearLayout.addView(leftLinearLayout);
        borderLinearLayout.addView(centerLinearLayout);
        borderLinearLayout.addView(rightLinearLayout);

        return borderLinearLayout;
    }

    public static ViewGroup getParent(View view) {
        return (ViewGroup) view.getParent();
    }


    /**
     * Checks to ensure that the button click goal on a listview happened on the correct list at the correct position.
     **/
    public static boolean checkForListViewClick(JSONObject initProperties, View clickable) throws Exception {
        JSONObject cellInfo = initProperties.optJSONObject(ViewConstants.CELL_INFO);
        boolean isRecycler = initProperties.optBoolean(ViewConstants.IS_IN_RECYCLER_VIEW);
        int expectedPosition = cellInfo.optInt(ViewConstants.POSITION, -1);
        String expectedListIdentifier = cellInfo.optString(ViewConstants.LIST_IDENTIFIER);
        View listView = isRecycler ? getRecView(clickable) : getListView(clickable);

        if (listView != null) {
            String clickableListIdentifier = TLManager.getInstance().getAppContext().getResources().getResourceEntryName(listView.getId());
            //Check that this is the correct list.
            if (clickableListIdentifier.equals(expectedListIdentifier)) {
                int clickablePosition = isRecycler ? getRecViewPosition((RecyclerView) listView, clickable) : getListViewPosition((AbsListView) listView, clickable);
                //Check that this is the correct position
                if (expectedPosition == clickablePosition) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Used to determine whether or not a touch was a click.
     *
     * @return Dp value of the distance between touch-down and touch-up.
     */
    public static float getMoveDistance(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        float distanceInPx = (float) Math.sqrt(dx * dx + dy * dy);
        return convertPixelsToDp(distanceInPx);
    }

    /**
     * Get a listview on the screen if the view passed is inside of one.
     *
     * @param v the view WITHIN a listview.
     * @return the ListView
     */
    private static AbsListView getListView(View v) {
        try {
            ViewGroup parent = ViewUtils.getParent(v);
            ViewGroup currentViewGroup = TLViewManager.getInstance().getCurrentViewGroup();
            if (parent != currentViewGroup && (parent instanceof AbsListView)) {
                return (AbsListView) parent;
            } else if (parent != currentViewGroup && parent != null) {
                return getListView(parent);
            } else {
                return null;
            }
        } catch (Throwable e) {
            return null;
        }
    }

    private static int getListViewPosition(AbsListView lv, View view) {
        int position = lv.getPositionForView(view);
        try {
            if (position == ListView.INVALID_POSITION) {
                return getListViewPosition(lv, ViewUtils.getParent(view));
            } else {
                return position;
            }
        } catch (Throwable e) {
            return ListView.INVALID_POSITION;
        }
    }

    private static View getViewByPositionInListView(int pos, ListView listView) {
        try {
            int firstPosition = listView.getFirstVisiblePosition() - listView.getHeaderViewsCount(); // This is the same as child #0
            int wantedChild = pos - firstPosition;
            if (wantedChild < 0 || wantedChild >= listView.getChildCount()) {
                return null;
            }
            return listView.getChildAt(wantedChild);
        } catch (Throwable e) {
            return null;
        }
    }

    private static View getRecViewByPositionInRecyclerView(int pos, RecyclerView recView) {
            try {
                //NEW METHOD, NOVEMBER 2018
                return recView.findViewHolderForAdapterPosition(pos).itemView;

               //In the absolute worst case, we somehow fail out trying to access the adapter internally.
                //Fall back on old method.
            } catch(Throwable t) {
                int firstListItemPosition = 0;

                RecyclerView.LayoutManager manager = recView.getLayoutManager();

                try {
                    if (manager instanceof LinearLayoutManager) {
                        firstListItemPosition = ((LinearLayoutManager) manager).findFirstVisibleItemPosition();
                    }
                    int wanted = pos - firstListItemPosition;
                    if (wanted < 0 || wanted >= recView.getChildCount()) {
                        return null;
                    } else {
                        return recView.getChildAt(wanted);
                    }
                } catch (Throwable e) {
                    return null;
                }
            }
    }

    /**
     * Check if the view we clicked is in a fragment that we have on the screen.
     *
     * @return the id of the fragment if it is in one, otherwise -2
     */
    private static RecyclerView getRecView(View v) {
        try {
            ViewGroup parent = ViewUtils.getParent(v);
            ViewGroup currentViewGroup = TLViewManager.getInstance().getCurrentViewGroup();
            if (parent != currentViewGroup && (parent instanceof RecyclerView)) {
                return (RecyclerView) parent;
            } else if (parent != currentViewGroup && parent != null) {
                return getRecView(parent);
            } else {
                return null;
            }
        } catch (Throwable e) {
            return null;
        }
    }

    private static int getRecViewPosition(RecyclerView rv, View view) {
        int position = RecyclerView.NO_POSITION;
        try {
            View parent = view;
            RecyclerView.LayoutManager layoutManager =  rv.getLayoutManager();
            while (position == RecyclerView.NO_POSITION && parent != null) {
                try {
                    if (layoutManager != null) {
                        //----NEW METHOD AS OF NOVEMBER 2018----//
                        position = layoutManager.getPosition(view);
                        //-------------------------------------//
                    }
                } catch (Throwable t){
                    //
                }

                //OLD method is now fail-over in case of old SDKs
                try {
                    if (position == RecyclerView.NO_POSITION) {
                        //TODO: Fix & TEST deprecation (ok as of November 2018) (#144816993)
                        position = rv.getChildPosition(parent);
                        if (position == RecyclerView.NO_POSITION) {
                            position = rv.getChildLayoutPosition(view);
                        }
                    }
                } catch (Throwable e) {
                    parent = (View) parent.getParent();
                    if (view instanceof RecyclerView || parent instanceof RecyclerView) {
                        return RecyclerView.NO_POSITION;
                    }
                }
            }
        } catch (Throwable e) {
            return RecyclerView.NO_POSITION;
        }
        return position;
    }


    /**
     * @param drawable the drawable we will fill the screen with
     * @return essentially a splash screen.
     */
    public static ImageView getDelayLoadView(final Drawable drawable) {
        final Context appContext = TLManager.getInstance().getAppContext();
        if (appContext == null) {
            return null;
        }
        final ImageView screen = new ImageView(appContext);
        try {
            final FrameLayout.LayoutParams borderParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
            // Need to push down the top border to be below status bar
            final int statusBarHeight = ViewUtils.getStatusBarHeight(appContext);
            borderParams.setMargins(0, statusBarHeight, 0, 0);

            screen.setLayoutParams(borderParams);
            screen.setId(getDelayViewId());
            screen.setImageDrawable(drawable);
            screen.setScaleType(ImageView.ScaleType.FIT_XY);
        } catch (Throwable e) {
            TLLog.error("Problem getting delay load view", e);
            return null;
        }
        return screen;
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void outlineView(View v, boolean highlight) {
        int loc[] = new int[2];
        v.getLocationOnScreen(loc);
        int startX = loc[0];
        int startY = loc[1];
        int endX = loc[0] + v.getWidth();
        int endY = loc[1] + v.getHeight();


        final ViewGroup currentViewGroup = TLViewManager.getInstance().getCurrentViewGroup();
        final Activity currentActivity = TLManager.getInstance().getCurrentActivity();
        if (currentViewGroup == null || currentActivity == null) return;

        if (currentViewGroup.findViewById(TLHighlightShape.getOverlayId()) == null) {

            final BorderFrameLayout border = new BorderFrameLayout(TLManager.getInstance().getAppContext());
            try {
                FrameLayout.LayoutParams borderParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT);
                border.setLayoutParams(borderParams);
                border.setId(TLHighlightShape.getOverlayId());

                if (android.os.Build.VERSION.SDK_INT >= 16) {
                    border.setBackground(new TLHighlightShape(Colors.getColorTranslucentPink(), startX, startY, endX, endY, highlight));
                } else {
                    border.setBackgroundDrawable(new TLHighlightShape(Colors.getColorTranslucentPink(), startX, startY, endX, endY, highlight));
                }

                final ViewGroup vg = (ViewGroup) currentActivity.findViewById(android.R.id.content).getRootView();

//                border.setAlpha(0f);
                vg.addView(border);
                animateOverlay(border);

            } catch (Throwable e) {
                TLLog.error("border", e);
            }
        } else {
            TLHighlightShape shape = (TLHighlightShape) currentViewGroup.findViewById(TLHighlightShape.getOverlayId()).getBackground();
            shape.updateParams(startX, startY, endX, endY);
            animateOverlay(currentViewGroup.findViewById(TLHighlightShape.getOverlayId()));
        }

    }

    private static void animateOverlay(final View v) {
        AlphaAnimation anim = new AlphaAnimation(0f, 1f);
        anim.setDuration(500);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                AlphaAnimation anim = new AlphaAnimation(1f, .5f);
                anim.setDuration(300);
                anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        AlphaAnimation anim = new AlphaAnimation(.5f, 1f);
                        anim.setDuration(300);
                        v.startAnimation(anim);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                v.startAnimation(anim);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        anim.setInterpolator(new LinearInterpolator());
        v.startAnimation(anim);

    }


    //Reset this view and all its children
    public static void recursiveReset(ViewGroup v) {
        for (int i = 0; i < v.getChildCount(); i++) {
            View view = v.getChildAt(i);
            resetView(view);
            if (view instanceof ViewGroup) {
                recursiveReset((ViewGroup) view);
            }
        }
    }

    public static void resetView(View view) {
        Object viewTag = view.getTag(TLViewManager.getInstance().getApplicationIconId());
        if (viewTag != null && viewTag instanceof HashMap) {
            Object shouldReset = ((HashMap) viewTag).get(SHOULD_RESET);
            if (shouldReset == null || (shouldReset instanceof Boolean
                    && (boolean) shouldReset)) {

                for (Object key : ((HashMap) viewTag).keySet()) {
                    if (key instanceof String) {
                        String methodName = (String) key;
                        Object o = ((HashMap) viewTag).get(key);
                        if (o instanceof HashMap) {
                            HashMap hash = (HashMap) o;
                            Object object = hash.get(MethodUtils.PARAMETER_TYPE);
                            if (object instanceof String) {
                                String parameter = (String) object;
                                MethodUtils.applyMethod(view, methodName, hash.get(methodName), parameter,
                                        true, false);
                            }
                        }
                    }
                }
            }
        }
        view.setTag(TLViewManager.getInstance().getApplicationIconId(), null);
    }

    //returns if this is the view groups first time through setProps,
    //if so sets that its no longer its first time and returns true
    private static boolean checkIfFirstTime(ViewGroup vg) {
        Object o = vg.getTag(TLViewManager.getInstance().getApplicationIconId());
        if (o != null && o instanceof HashMap) {
            HashMap hashMap = (HashMap) o;
            o = hashMap.get(LIST_OR_FRAGMENT_FIRST_TIME);
            if (o != null && o instanceof Boolean) {
                return (boolean) o;
            } else {
                //As of now this will never get hit, but its here for futureproofing,
                //as this is what it should do if this situation ever arises
                hashMap.put(LIST_OR_FRAGMENT_FIRST_TIME, false);
                vg.setTag(TLViewManager.getInstance().getApplicationIconId(), hashMap);
            }
        } else {
            HashMap map = new HashMap();
            map.put(LIST_OR_FRAGMENT_FIRST_TIME, false);
            vg.setTag(TLViewManager.getInstance().getApplicationIconId(), map);
        }
        return true;
    }

    //find the matching value for the methodName for this view and replace it
    static void cacheValue(View view, Object value, String methodName, String parameterType) throws JSONException {
        final JSONArray views = TLManager.getInstance().getTlProperties().getViews();
        int id = ViewUtils.getOrMakeViewId(view);
        String identifier = view.getResources().getResourceName(id);
        //find the right view
        int k = -1;
        for (int i = 0; i < views.length(); i++) {
            if (((JSONObject) views.get(i)).optJSONObject(INIT_PROPERTIES).optInt(VIEW_ID) == id && ((JSONObject) views.get(i)).optJSONObject(INIT_PROPERTIES).optString(ANDROID_IDENTIFIER).equals(identifier)) {
                if (views.getJSONObject(i).has(ANDROID_PROPERTIES)) {
                    k = i;
                    break;
                }
            }
        }

        if (k != -1) {
            JSONObject method = views.getJSONObject(k).optJSONObject(ANDROID_PROPERTIES).optJSONObject(methodName);
            method.put(VALUE, value);
            method.put(TYPE, parameterType);
        }
    }

    @SuppressWarnings("unchecked")
    /**
     * Returns a list of all 'View Roots' which are View Parents.
     *
     * The most toplevel view as far as we are usually concerned is the decorview.
     *
     * However, dialogs and toasts exist at a parallel level, as children of 'ViewParents'
     *
     * The decorview used for {@link TLViewManager#currentViewGroupWeakRef} is the child of one of these.
     */
    public static List<ViewParent> getViewRoots() {

        List<ViewParent> viewRoots = new ArrayList<>();

        try {
            Object windowManager;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                windowManager = Class.forName("android.view.WindowManagerGlobal")
                        .getMethod("getInstance").invoke(null);
            } else {
                Field f = Class.forName("android.view.WindowManagerImpl")
                        .getDeclaredField("sWindowManager");
                f.setAccessible(true);
                windowManager = f.get(null);
            }

            Field rootsField = windowManager.getClass().getDeclaredField("mRoots");
            rootsField.setAccessible(true);

            Field stoppedField = Class.forName("android.view.ViewRootImpl")
                    .getDeclaredField("mStopped");
            stoppedField.setAccessible(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                List<ViewParent> viewParents = (List<ViewParent>) rootsField.get(windowManager);
                // Filter out inactive view roots
                boolean first = true;
                for (ViewParent viewParent : viewParents) {
                    boolean stopped = (boolean) stoppedField.get(viewParent);
                    if (!stopped) {
                        if (!first) {
                            viewRoots.add(viewParent);
                        } else {
                            first = false;
                        }
                    }
                }
            } else {
                ViewParent[] viewParents = (ViewParent[]) rootsField.get(windowManager);
                // Filter out inactive view roots
                boolean first = true;
                for (ViewParent viewParent : viewParents) {
                    boolean stopped = (boolean) stoppedField.get(viewParent);
                    if (!stopped) {
                        if (!first) {
                            viewRoots.add(viewParent);
                        } else {
                            first = false;
                        }
                    }
                }
            }
        } catch (Throwable e) {
            TLLog.error("Error getting viewparents");
        }
        return viewRoots;
    }

    /**
     * Dialog ViewParents have an 'mView' field which is the toplevel viewgroup for that
     * branch of the view tree. This also contains toasts and whatnot.
     *
     * @return All viewgroups that are children of all current ViewParents that have an mView field.
     */
    public static List<ViewGroup> getAllOtherParentViewGroups() {
        List<ViewGroup> viewGroups = new ArrayList<>();
        for (ViewParent p : ViewUtils.getViewRoots()) {
            try {
                Field field = p.getClass().getDeclaredField("mView");
                field.setAccessible(true);
                ViewGroup v = (ViewGroup) field.get(p);
                viewGroups.add(v);
            } catch (Throwable e) {
                TLLog.error("Error getting viewgroups");

            }
        }
        return viewGroups;
    }
}