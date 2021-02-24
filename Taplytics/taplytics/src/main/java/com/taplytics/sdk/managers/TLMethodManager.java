/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.managers;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.taplytics.sdk.utils.TLLog;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * A class to handle methods and all of their getters and setter.
 * <p/>
 * Created by VicV on 9/24/14.
 */
public class TLMethodManager {

    private static TLMethodManager instance;

    private static int METHOD_TAG_INT = 0;

    public static TLMethodManager getInstance() {
        if (instance != null) {
            return instance;
        } else {
            instance = new TLMethodManager();
        }
        return instance;
    }

    private static HashMap<String, HashSet<String>> setterMethods = new HashMap<>();

    public static int getMethodTagInt() {
        return METHOD_TAG_INT;
    }

    public HashMap<String, HashSet<String>> getSetterMethods() {
        return setterMethods;
    }

    private static HashMap<String, HashSet<String>> getterMethods = new HashMap<>();

    public HashMap<String, HashSet<String>> getGetterMethods() {
        return getterMethods;
    }

    private static HashSet<String> mKnownClasses = new HashSet<>();

    private TLMethodManager() {


        try {
            String packageName = TLManager.getInstance().getAppContext().getPackageName(); //use getPackageName() in case you wish to use yours
            final PackageManager pm = TLManager.getInstance().getAppContext().getPackageManager();
            final ApplicationInfo applicationInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            METHOD_TAG_INT = applicationInfo.icon;
        } catch (Exception e) {
            TLLog.error("error setting ");
        }
        // Here we initialize a bunch of HashSets ONCE. Because keeping ArrayLists is way more intensive.

        // TODO: Make a better way to do all this because jesus this is a lot of repetition.
        mKnownClasses = new HashSet<>(knownClassTypes);

        setterMethods.put("View", viewSetters);
        getterMethods.put("View", viewGetters);

        textViewSetters.addAll(viewSetters);
        setterMethods.put("TextView", textViewSetters);
        textViewGetters.addAll(viewGetters);
        getterMethods.put("TextView", textViewGetters);

        imageViewSetters.addAll(viewSetters);
        setterMethods.put("ImageView", imageViewSetters);
        imageViewGetters.addAll(viewGetters);
        getterMethods.put("ImageView", imageViewGetters);

        // TODO: Re-implement all of these

        // progressBarSetters.addAll(viewSetters);
        // setterMethods.put("ProgressBar", progressBarSetters);
        // progressBarSetters.addAll(viewGetters);
        // getterMethods.put("ProgressBar", progressBarGetters);
        //

        // absListViewSetters.addAll(viewSetters);
        // setterMethods.put("AbsListView", absListViewSetters);
        // absListViewSetters.addAll(viewGetters);
        // getterMethods.put("AbsListView", absListViewGetters);
        //
        // gridViewSetters.addAll(absListViewSetters);
        // setterMethods.put("GridView", gridViewSetters);
        // gridViewGetters.addAll(absListViewGetters);
        // getterMethods.put("GridView", gridViewGetters);
        //
        // listViewSetters.addAll(absListViewSetters);
        // setterMethods.put("ListView", listViewSetters);
        // listViewGetters.addAll(absListViewGetters);
        // getterMethods.put("ListView", listViewGetters);
        //
        // expandableListViewSetters.addAll(listViewSetters);
        // setterMethods.put("ExpandableListView", expandableListViewSetters);
        // expandableListViewGetters.addAll(listViewGetters);
        // getterMethods.put("ExpandableListView", listViewGetters);
        //
        // videoViewSetters.addAll(viewSetters);
        // setterMethods.put("VideoView", videoViewSetters);
        // videoViewGetters.addAll(viewGetters);
        // getterMethods.put("VideoView", videoViewGetters);
        //
        // compoundButtonSetters.addAll(textViewSetters);
        // setterMethods.put("CompoundButton", compoundButtonSetters);
        // compoundButtonGetters.addAll(textViewGetters);
        // getterMethods.put("CompoundButton", compoundButtonGetters);
        //
        // switchSetters.addAll(compoundButtonSetters);
        // setterMethods.put("Switch", switchSetters);
        // switchGetters.addAll(compoundButtonGetters);
        // getterMethods.put("Switch", switchGetters);
        //
        // toggleButtonSetters.addAll(compoundButtonSetters);
        // setterMethods.put("ToggleButton", toggleButtonSetters);
        // toggleButtonGetters.addAll(compoundButtonGetters);
        // getterMethods.put("ToggleButton", toggleButtonGetters);
        //
        // checkedTextViewSetters.addAll(textViewSetters);
        // setterMethods.put("CheckedTextView", checkedTextViewSetters);
        // checkedTextViewGetters.addAll(textViewGetters);
        // getterMethods.put("CheckedTextView", checkedTextViewGetters);
        //
        // autoCompleteTextViewSetters.addAll(textViewSetters);
        // setterMethods.put("AutoCompleteTextView", autoCompleteTextViewSetters);
        // autoCompleteTextViewGetters.addAll(textViewGetters);
        // getterMethods.put("AutoCompleteTextView", autoCompleteTextViewGetters);

    }

    /**
     * This is a list of all known child classes of views. This is to determine what the basetype of a found view is, and by extension, what
     * properties can be set
     */
    private static final List<String> knownClassTypes = Arrays.asList("AnalogClock", "ImageView", "KeyboardView", "MediaRouteButton", "ProgressBar",
            "Space", "SurfaceView", "TextView", "TextureView", "ViewGroup", "ViewStub", "AbsListView", "AbsSeekBar", "AbsSpinner", "AbsoluteLayout",
            "AdapterView", "AdapterViewAnimator", "AdapterViewFlipper", "AppWidgetHostView", "AutoCompleteTextView", "Button", "CalendarView",
            "CheckBox", "CheckedTextView", "Chronometer", "CompoundButton", "ContentLoadingProgressBar", "DatePicker", "DialerFilter",
            "DigitalClock", "DrawerLayout", "EditText", "ExpandableListView", "ExtractEditText", "FragmentBreadCrumbs", "FragmentTabHost",
            "FrameLayout", "GLSurfaceView", "Gallery", "GestureOverlayView", "GridLayout", "GridView", "HorizontalScrollView", "ImageButton",
            "ImageSwitcher", "LinearLayout", "ListView", "MediaController", "MultiAutoCompleteTextView", "NumberPicker", "PagerTabStrip",
            "PagerTitleStrip", "QuickContactBadge", "RadioButton", "RadioGroup", "RatingBar", "RelativeLayout", "ScrollView", "SearchView",
            "SeekBar", "SlidingDrawer", "SlidingPaneLayout", "Spinner", "StackView", "SwipeRefreshLayout", "Switch", "TabHost", "TabWidget",
            "TableLayout", "TableRow", "TextClock", "TimePicker", "ToggleButton", "TwoLineListItem", "VideoView", "ViewAnimator", "ViewFlipper",
            "ViewFlipper", "ViewPager", "WebView", "ZoomButton", "ZoomControls", "View");

    public HashSet<String> getKnownClasses() {
        return mKnownClasses;
    }

	/*
     * Standard Views
	 */

    // TODO: Reimplement everything
    // private static final HashSet<String> viewSetters = new HashSet<>(Arrays.asList("setAlpha", "setBackgroundColor",
    // "setHapticFeedbackEnabled",
    // "setKeepScreenOn", "setPadding", "setRotation", "setRotationX", "setRotationY", "setScaleX", "setScaleY",
    // "setScrollBarDefaultDelayBeforeFade", "setScrollBarSize", "setScrollBarFadeDuration", "setTextAlignment", "setTextDirection",
    // "setVisibility"));
    //
    // private static final HashSet<String> viewGetters = new HashSet<>(Arrays.asList("getAlpha", "getBackground",
    // "isHapticFeedbackEnabled",
    // "getKeepScreenOn", "getPaddingLeft", "getPaddingTop", "getPaddingRight", "getPaddingBottom", "getRotation", "getScaleX", "getScaleY",
    // "getScrollBarDefaultDelayBeforeFade", "getScrollBarFadeDuration", "getScrollBarSize", "getTextAlignment", "getTextDirection",
    // "getVisibility"));

    private static final HashSet<String> viewSetters = new HashSet<>(Arrays.asList("setAlpha", "setHapticFeedbackEnabled", "setVisibility",
            "setPadding", "setWidth", "setHeight", "setBackgroundColor", "setBackgroundDrawable"));

    private static final HashSet<String> viewGetters = new HashSet<>(Arrays.asList("getAlpha", "isHapticFeedbackEnabled", "getVisibility",
            "getWidth", "getHeight", "getPaddingLeft", "getPaddingTop", "getPaddingRight", "getPaddingBottom", "getBackground"));

	/*
     * TextView
	 * 
	 * Direct Subclasses: Button, CheckedTextView, Chronometer, DigitalClock,EditText,TextClock
	 */
    // TODO: Reimplement everything
    // private static final HashSet<String> textViewSetters = new HashSet<String>(Arrays.asList("setAutoLinkMask", "setText",
    // "setEllipsize", "setHint",
    // "setGravity", "setTypeFace", "setHeight", "setLineSpacing", "setLines", "setLinksClickable", "setMaxLines", "setAllCaps",
    // "setTextColor",
    // "setTextIsSelectable", "setWidth", "setTextSize", "setHintTextColor", "setHighLightColor", "setLinkTextColors", "setLinkTextColor",
    // "setTextLocale", "setCursorVisible", "setSingleLine"));
    //
    // private static final HashSet<String> textViewGetters = new HashSet<String>(Arrays.asList("getAutoLinkMask", "getText",
    // "getEllipsize", "getHint",
    // "getGravity", "getTypeFace", "getHeight", "getLineSpacingExtra", "getMaxLines", "getLinksClickable", "getCurrentTextColor",
    // "getCurrentHintTextColor", "isTextSelectable", "getWidth", "getTextSize", "getHighlightColor", "getLinkTextColors", "getTextLocale",
    // "isCursorVisible"));

    // TODO: Replace this with old one.
    private static final HashSet<String> textViewSetters = new HashSet<>(Arrays.asList("setText", "setHint", "setLineSpacing", "setMaxLines",
            "setTextSize", "setGravity"));

    private static final HashSet<String> textViewGetters = new HashSet<>(Arrays.asList("getText", "getHint", "getLineSpacingExtra", "getMaxLines",
            "getTextSize", "getGravity"));

	/*
	 * ImageView
	 * 
	 * Direct subclasses: ImageButton, QuickContactBadge
	 */

    private static final HashSet<String> imageViewSetters = new HashSet<>(Arrays.asList("setImageAlpha", "setScaleType", "setImageDrawable"));

    private static final HashSet<String> imageViewGetters = new HashSet<>(Arrays.asList("getImageAlpha", "getScaleType", "getDrawable"));

    // /*
    // * ProgressBar
    // *
    // * Direct subclasses: AbsSeekBar, ContentLoadingProgessBar
    // */
    //
    // private static final HashSet<String> progressBarSetters = new HashSet<String>(Arrays.asList("setMax", "setProgress",
    // "getProgressDrawable"));
    //
    // private static final HashSet<String> progressBarGetters = new HashSet<String>(Arrays.asList("getMax", "getProgress",
    // "setProgressDrawable"));
    //
    // /*
    // * AbsListView
    // *
    // * direct subclasses: GridView, ListView
    // */
    //
    // private static final HashSet<String> absListViewSetters = new HashSet<String>(Arrays.asList("setFriction", "setPadding",
    // "setVerticalScrollbarEnabled", "getOverScrollMode"));
    //
    // private static final HashSet<String> absListViewGetters = new HashSet<String>(Arrays.asList("getListPaddingLeft",
    // "getListPaddingBottom",
    // "getListPaddingRight", "getListPaddingTop", "isFastScrollBarEnabled", "setOverScrollMode", "isVerticalScrollbarEnabled"));
    //
    // /*
    // * GridView
    // */
    //
    // private static final HashSet<String> gridViewSetters = new HashSet<String>(Arrays.asList("setColumnWidth", "setGravity",
    // "setHorizontalSpacing",
    // "setNumColumns", "setStretchMode", "setVerticalSpacing"));
    //
    // private static final HashSet<String> gridViewGetters = new HashSet<String>(Arrays.asList("getColumnWidth", "getGravity",
    // "getHorizontalSpacing",
    // "getNumColumns", "getStretchMode", "getVerticalSpacing"));
    //
    // /*
    // * ListView
    // *
    // * Direct Subclasses: ExpandableListView
    // */
    //
    // private static final HashSet<String> listViewSetters = new HashSet<String>(Arrays.asList("setDivider", "setDividerHeight",
    // "setHeaderDividersEnabled", "setFooterDividersEnabled"));
    //
    // private static final HashSet<String> listViewGetters = new HashSet<String>(Arrays.asList("getDivider", "getDividerHeight",
    // "areHeaderDividersEnabled", "areFooterDividersEnabled"));
    //
    // /*
    // * ExpandableListView
    // *
    // * Direct Subclasses: ExpandableListView
    // */
    //
    // private static final HashSet<String> expandableListViewSetters = new HashSet<String>(Arrays.asList("setChildDivider"));
    //
    // private static final HashSet<String> expandableListViewGetters = new HashSet<String>(Arrays.asList("getChildDivier"));
    //
    // /*
    // * VideoView
    // *
    // * NOTE: getVideoPath is not an actual method, catch that and use reflection to attempt to get video path:
    // *
    // * Uri mUri = null; try { Field mUriField = VideoView.class.getDeclaredField("mUri"); mUriField.setAccessible(true); mUri =
    // * (Uri)mUriField.get(video); } catch(Exception e) {}
    // */
    //
    // private static final HashSet<String> videoViewSetters = new HashSet<String>(Arrays.asList("setVideoPath"));
    //
    // private static final HashSet<String> videoViewGetters = new HashSet<String>(Arrays.asList("getVideoPath"));
    //
    // /*
    // * CompoundButton
    // *
    // * Direct subclasses: CheckBox, RadioButton, Switch, ToggleButton
    // */
    //
    // private static final HashSet<String> compoundButtonSetters = new HashSet<String>(Arrays.asList("setButtonDrawable", "setChecked"));
    //
    // private static final HashSet<String> compoundButtonGetters = new HashSet<String>(Arrays.asList("isChecked"));
    //
    // /*
    // * Switch
    // */
    //
    // private static final HashSet<String> switchSetters = new HashSet<String>(Arrays.asList("setSwtichPadding", "setTextOff", "setTextOn",
    // "setSwitchTypeface", "setThumbDrawable", "seTrackDrawable", "setThumbTextPadding"));
    //
    // private static final HashSet<String> switchGetters = new HashSet<String>(Arrays.asList("getSwitchPadding", "getTextOff", "getTextOn",
    // "getThumbDrawable", "getTrackDrawable", "getTextOff", "getTextOn", "getThumbTextPadding"));
    //
    // /*
    // * ToggleButton
    // */
    //
    // private static final HashSet<String> toggleButtonSetters = new HashSet<String>(Arrays.asList("setTextOff", "setTextOn",
    // "setBackgroundDrawable",
    // "setChecked"));
    //
    // private static final HashSet<String> toggleButtonGetters = new HashSet<String>(Arrays.asList("getTextOff", "getTextOn"));
    //
    // /*
    // * CheckedTextView
    // */
    //
    // private static final HashSet<String> checkedTextViewSetters = new HashSet<String>(Arrays.asList("setCheckMarkDrawable",
    // "setChecked"));
    //
    // private static final HashSet<String> checkedTextViewGetters = new HashSet<String>(Arrays.asList("getCheckMarkDrawable",
    // "isChecked"));
    //
    // /*
    // * AutoCompleteTextView
    // *
    // * Parent: TextView (EditText)
    // */
    //
    // private static final HashSet<String> autoCompleteTextViewSetters = new HashSet<String>(Arrays.asList("setCompletionHint",
    // "setDropdownBackground", "setDropDownHeight", "setDropDownWidth"));
    //
    // private static final HashSet<String> autoCompleteTextViewGetters = new HashSet<String>(Arrays.asList("getCompletionHint",
    // "getDropDownBackground", "getDropdownHeight", "getDropDownWidth"));

}
