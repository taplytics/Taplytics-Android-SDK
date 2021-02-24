/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.listeners;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.taplytics.sdk.utils.TLLog;
import com.taplytics.sdk.utils.ViewUtils;

/**
 * Created by clvcooke on 9/2/15.
 */
public class TLRecyclerListener implements RecyclerView.RecyclerListener {

    //The old listener that we call once we have done what we needed
    private RecyclerView.RecyclerListener oldListener = null;

    private TLOnRecyclerScrollListener scrollListener = null;

    /**
     * @param oldListener the old RecyclerListener (can be null if there was none)
     */
    public TLRecyclerListener(RecyclerView.RecyclerListener oldListener, TLOnRecyclerScrollListener scrollListener) {
        this.oldListener = oldListener;
        this.scrollListener = scrollListener;
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        //Reset the view back to its initial state before recycling.
        try {
            if (scrollListener == null || scrollListener.getScrollState() ==  0) {
                return;
            }

            if (holder.itemView instanceof ViewGroup) {
                ViewUtils.recursiveReset((ViewGroup) holder.itemView);
            } else {
                ViewUtils.resetView(holder.itemView);
            }

            if (oldListener != null) {
                oldListener.onViewRecycled(holder);
            }
        } catch (Throwable t) {
            TLLog.error("Error recycling", t);
        }
    }
}
