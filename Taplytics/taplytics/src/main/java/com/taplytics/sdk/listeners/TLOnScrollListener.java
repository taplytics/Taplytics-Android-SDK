/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.listeners;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

import com.taplytics.sdk.managers.TLManager;
import com.taplytics.sdk.managers.TLViewManager;
import com.taplytics.sdk.resources.TLHighlightShape;
import com.taplytics.sdk.utils.TLLog;
import com.taplytics.sdk.utils.ViewUtils;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by VicV on 5/12/15.
 * <p>
 * When the user scrolls, we check to see if a new view exists, and if its a known position we want to change, change it!
 */
public class TLOnScrollListener implements AbsListView.OnScrollListener {

    int getScrollState() {
        return scrollState;
    }

    private int scrollState = 0;

    private HashMap<Integer, Boolean> items = null;


    /**
     * The old OnScrollListener that we hijacked*
     */
    private AbsListView.OnScrollListener oldListener;

    /**
     * The last known first visible position*
     */
    private int firstVisiblePosition = -1;

    /**
     * The last known last visible position*
     */

    private int lastVisiblePosition = -1;

    /**
     * The last known item count *
     */
    private int lastKnownItemCount = -1;

    private boolean hasHeader =  false;

    /**
     * The old y of the first view in the list
     * Since the first view often changes this value is not very accurate
     * but in the cases where it is inaccurate we can already detect the view has been swapped out
     */
    private int oldY = -1;


    /**
     * Resets the entire list of known items so that they get cleaned up if necessary.
     */
    public void resetItems(){
        items = null;
    }

    public TLOnScrollListener(AbsListView.OnScrollListener oldListener) {
        this.oldListener = oldListener;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

        this.scrollState = scrollState;

        //make sure to trigger the old listeners
        if (oldListener != null) {
            oldListener.onScrollStateChanged(view, scrollState);
        }
        try {
            if (view.getAdapter() != null && view.getAdapter().getItemViewType(0) == AbsListView.ITEM_VIEW_TYPE_HEADER_OR_FOOTER) {
                hasHeader = true;
            }
        } catch (Throwable e){
            TLLog.error("Error checking if header exists", e);
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        try {
            if (items == null) {
                items = new HashMap<>();
                HashSet<Integer> numbers = TLManager.getInstance().getTlProperties().getKnownListPositions(TLManager.getInstance().getAppContext().getResources().getResourceEntryName(view.getId()));
                if (numbers != null) {
                    for (int i : numbers) {
                        items.put(i, false);
                    }
                }
            }

            //grab the last visible item (method only gives first)
            int lastVisibleItem = view.getLastVisiblePosition();

            if(hasHeader) {
                firstVisibleItem++;
                lastVisibleItem++;
            }

            /**
             * The delta y section is based on how the list view detects if it needs to obtain a view
             * in order to fill a gap at the top or bottom of the list. It would fill the gap by calling
             * getView() and update a view without us knowing. By checking for when those conditions are met
             * and updating the views when they are this problem is avoided. Full method can be found at
             * AbsListView.trackMotionScroll()
             */

            //we are defining y as the top of the first visible child. Not completely accurate but good enough
            int newY = 0;
            View topChild = view.getChildAt(0);
            if (topChild != null) {
                newY = topChild.getTop();
            }

            int incrementalDeltaY = newY - oldY;
            oldY = newY;
            int absIncrementalDeltaY = Math.abs(incrementalDeltaY);

            int bottom = 0;
            View bottomChild = view.getChildAt(visibleItemCount - 1);
            if (bottomChild != null) {
                bottom = bottomChild.getBottom();
            }
            //If the first or last visible item has changed or the list has changed position enough to do a fill, which would replace the bottom or top view
            //So update the views
            if (TLManager.getInstance().isActivityActive()
                    && (firstVisibleItem != firstVisiblePosition
                    || lastVisibleItem != lastVisiblePosition
                    || lastKnownItemCount != totalItemCount
                    || view.getPaddingTop() - newY < absIncrementalDeltaY
                    || bottom - (view.getHeight() - view.getPaddingBottom()) < absIncrementalDeltaY)) {
                lastKnownItemCount = totalItemCount;
                firstVisiblePosition = firstVisibleItem;
                lastVisiblePosition = lastVisibleItem;
                if (items != null) {
                    //Use copy to avoid concurrent modifications;
                    HashMap<Integer, Boolean> itemsCopy = items;
                    for (int i : itemsCopy.keySet()) {
                        if (i >= firstVisibleItem && i <= lastVisibleItem) {
                            if (!itemsCopy.get(i)) {
                                ViewUtils.setProperties(view);
                                itemsCopy.put(i, true);
                            }
                        } else {
                            if (itemsCopy.get(i)) {
                                itemsCopy.put(i, false);
                            }
                        }
                    }
                    items = itemsCopy;
                }
            }


            ViewGroup vg = TLViewManager.getInstance().getCurrentViewGroup();
            if (TLManager.getInstance().isLiveUpdate() && TLManager.getInstance().isActivityActive() && vg != null && vg.findViewById(TLHighlightShape.getOverlayId()) != null) {
                    vg.removeView(vg.findViewById(TLHighlightShape.getOverlayId()));
            }
        } catch (Throwable e) {
            TLLog.error("List scroll error", e);
        }
        //Trigger the old listener.
        if (oldListener != null) {
            oldListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
        }
    }
}
