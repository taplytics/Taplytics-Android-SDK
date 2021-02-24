/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.utils.constants;

/**
 * Created by vicvu on 2017-06-07.
 */

public class ViewConstants {

    //Basic view params
    public static final String VIEW = "View";
    public static final String ID = "id";
    public static final String IDENTIFIER = "identifier";
    public static final String FRAG_ID = "fragId";
    public static final String ACTIVITY = "activity";
    public static final String OBJECT = "Object";
    public static final String VALUE = "value";
    public static final String TYPE = "type";

    //TLProperties Params
    public static final String FRAG_IDENTIFIER = "fragIdentifier";
    public static final String INIT_PROPERTIES = "initProperties";
    public static final String ANDROID_IDENTIFIER = "anIdentifier";
    public static final String ANDROID_PROPERTIES = "anProperties";
    public static final String ACTIVITY_ID_FIELD = "_view";
    public static final String VIEW_ID = "anID";
    public static final String RESET_FIELD = "reset";

    //Reflection
    public static final String CLASS_FIELD_NAME = "class";
    public static final String BASE_CLASS_FIELD_NAME = "baseClass";
    public static final String SUB_CLASS_FIELD_NAME = "subClass";
    public static final String METHOD_INFO_FIELD_NAME = "methodInfo";

    //Used as an identifier prefix if the view does not have an identifier
    public static final String TAPLYTICS_TEXT = "tl_text";

    //ListView constants
    public static final String ON_SCROLL_LISTENER_FIELD_NAME = "mOnScrollListener";
    public static final String RECYCLER_FIELD_NAME = "mRecycler";
    public static final String RECYCLER_LISTENER_FIELD_NAME = "mRecyclerListener";
    public static final String HAS_DATA_SET_OBSERVER_FIELD = "hasDataSetObserver";

    public static final String SCROLL_LISTENER_FIELD_NAME = "mScrollListener";
    public static final String HIERARCHY_CHANGE_LISTENER_FIELD_NAME = "mOnHierarchyChangeListener";

    public static final String LIST_OR_FRAGMENT_FIRST_TIME = "listOrFragmentFirstTime";
    public static final String CELL_INFO = "cellInfo";
    public static final String IS_CELL_ELEMENT = "cellElement";
    public static final String LIST_IDENTIFIER = "listIdentifier";
    public static final String IS_IN_RECYCLER_VIEW = "isInRecycler";


    //Various
    public static final String VIEWPAGER_CLASS_NAME = "_viewpager_";
    public static final String SHOULD_RESET = "shouldReset";
    public static final String DECOR_VIEW = "DecorView";
    public static final String IMAGE_FILE_NAME_FIELD = "imgFileName";
    public static final String HAS_ON_CLICK_FIELD = "hasOnClick";

    //positions and sizes
    public static final String POSITION = "position";
    public static final String POSITION_START_X = "startX";
    public static final String POSITION_START_Y = "startY";
    public static final String POSITION_END_X = "endX";
    public static final String POSITION_END_Y = "endY";
    public static final String GET_RAW_WIDTH_METHOD_NAME = "getRawWidth";
    public static final String GET_RAW_HEIGHT_METHOD_NAME = "getRawHeight";
    public static final String GET_REAL_SIZE_METHOD_NAME = "getRealSize";
    public static final String HEIGHT = "height";
    public static final String WIDTH = "width";
    public static final String SCREEN_DIMENSIONS_FIELD = "screenDimensions";

    //Status bar
    public static final String STATUS_BAR_HEIGHT = "status_bar_height";
    public static final String STATUS_BAR_DIMENSION = "dimen";
    public static final String STATUS_BAR_ANDROID = "android";







}