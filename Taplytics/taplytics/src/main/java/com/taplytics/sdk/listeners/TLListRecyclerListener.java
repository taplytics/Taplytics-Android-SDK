/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.listeners;

import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

import com.taplytics.sdk.utils.ViewUtils;

/**
 * Created by clvcooke on 9/1/15.
 * <p>
 * This resets the views if they are being recycled, so that our changes do not persist on unwanted views.
 */
public class TLListRecyclerListener implements AbsListView.RecyclerListener {

    private AbsListView.RecyclerListener oldListener;

    private TLOnScrollListener scrollListener;


    /**
     * @param oldListener the old recyclerListener (can be null if there was none)
     * @param scrollListener
     */

    public TLListRecyclerListener(AbsListView.RecyclerListener oldListener, TLOnScrollListener scrollListener) {
        this.oldListener = oldListener;
        this.scrollListener = scrollListener;
    }

    @Override
    public void onMovedToScrapHeap(View view) {

        if (scrollListener != null && scrollListener.getScrollState() != 0 && view != null) {
            //reset the view if we have touched it
            if (view instanceof ViewGroup) {
                ViewUtils.recursiveReset((ViewGroup) view);
            } else {
                ViewUtils.resetView(view);
            }
        }

        //call old listener
        if (oldListener != null) {
            oldListener.onMovedToScrapHeap(view);
        }
    }

}



