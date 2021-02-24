/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.listeners;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ViewGroup;

import com.taplytics.sdk.datatypes.TLProperties;
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
public class TLOnRecyclerScrollListener extends RecyclerView.OnScrollListener {

    private HashMap<Integer, Boolean> items = null;

    private int scrollState = 0;

    int getScrollState() {
        return scrollState;
    }

    /**
     * The onScrollListener we hijacked, if any*
     */
    private RecyclerView.OnScrollListener oldListener;

    /**
     * The last known first visible position on the RecyclerView*
     */
    private int firstVisiblePosition = -1;

    /**
     * The last known last visible position on the RecyclerView*
     */
    private int lastVisiblePosition = -1;

    /**
     * The last known amount of views in the RecyclerView
     */

    private int lastKnownAmount = -1;

    public TLOnRecyclerScrollListener(RecyclerView.OnScrollListener oldListener) {

        this.oldListener = oldListener;
    }

    private RecyclerView.LayoutManager layoutManager;

    /**
     * Resets the entire list of known items so that they get cleaned up if necessary.
     */
    public void resetItems(){
        items = null;
    }

    @Override
    public void onScrolled(RecyclerView recView, int dx, int dy) {

        try {
            if (items == null) {
                items = new HashMap<>();
                TLManager manager = TLManager.getInstance();
                TLProperties props = manager.getTlProperties();
                HashSet<Integer> numbers = props.getKnownRecyclerPositions(manager.getAppContext().getResources().getResourceEntryName(recView.getId()));
                if (numbers != null) {
                    for (int i : numbers) {
                        if (items.get(i) == null) {
                            items.put(i, false);
                        }
                    }

                }
            }

            if (items != null && items.size() > 0) {

                //Items can be set to null whenever. Avoid that by using a copy.
                 HashMap<Integer, Boolean> itemsCopy = items;

                //Just set these to -1 now because for some reason RecyclerView OnScrollListener doesn't provide any.
                int firstListItemPosition = -1;
                int lastListItemPosition = -1;
                int amount = -1;

                if (layoutManager == null) {
                    layoutManager = recView.getLayoutManager();
                }

                if (layoutManager != null) {
                    if (layoutManager instanceof LinearLayoutManager) {
                        //Grab our first and last visible positions
                        firstListItemPosition = ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
                        lastListItemPosition = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
                        amount = layoutManager.getChildCount();
                    }


                    //If the first or last visible position has changed, update the view ASAP.
                    if (TLManager.getInstance().isActivityActive() && ((firstVisiblePosition != firstListItemPosition || lastVisiblePosition != lastListItemPosition || amount != lastKnownAmount))) {
                        //Update the first/last visible positions.
                        firstVisiblePosition = firstListItemPosition;
                        lastVisiblePosition = lastListItemPosition;
                        lastKnownAmount = amount;

                        //Check our known items. If a known position exists between the top/bottom showing ones, set our properties!
                        for (int i : itemsCopy.keySet()) {
                            if (i >= firstListItemPosition && i <= lastListItemPosition) {
                                if (!itemsCopy.get(i)) {
                                    ViewUtils.setProperties(recView);
                                    itemsCopy.put(i, true);
                                }
                            } else {
                                if (itemsCopy.get(i)) {
                                    itemsCopy.put(i, false);
                                }
                            }
                        }

                    }
                }

                //Remove the view highlighting when the user scrolls.
                final ViewGroup vg = TLViewManager.getInstance().getCurrentViewGroup();
                if (TLManager.getInstance().isLiveUpdate() && TLManager.getInstance().isActivityActive() && vg != null) {
                    if (vg.findViewById(TLHighlightShape.getOverlayId()) != null) {
                        vg.removeView(vg.findViewById(TLHighlightShape.getOverlayId()));
                    }
                }
                items = itemsCopy;
            }
        } catch (Throwable e) {
            TLLog.error("Recycler scroll error", e);
        }

        //Trigger the old listener
        if (oldListener != null) {
            oldListener.onScrolled(recView, dx, dy);
        }
    }

    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        scrollState = newState;
        //Trigger the old listener
        if (oldListener != null) {
            oldListener.onScrollStateChanged(recyclerView, newState);
        }
    }

}
