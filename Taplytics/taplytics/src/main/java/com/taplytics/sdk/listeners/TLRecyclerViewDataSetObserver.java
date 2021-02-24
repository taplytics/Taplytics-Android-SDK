/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.listeners;

import android.support.v7.widget.RecyclerView;
import android.view.ViewTreeObserver;

/**
 * Created by clvcooke on 12/3/15.
 */
public class TLRecyclerViewDataSetObserver extends RecyclerView.AdapterDataObserver {

    private RecyclerView recyclerView;
    private ViewTreeObserver.OnPreDrawListener preDrawListener;

    //Taking in the recyclerView we are attached to. No leaks to worry about as this observer stays attached to the recyclerView
    public TLRecyclerViewDataSetObserver(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        preDrawListener = new TLOnPreDrawListener(recyclerView.getViewTreeObserver(), recyclerView);

    }

    //The dataset has changed, a relayout may have occurred.
    @Override
    public void onChanged() {
        recyclerView.getViewTreeObserver().addOnPreDrawListener(preDrawListener);
    }


    //This method will also be called by onItemRangeChanged(int positionStart, int itemCount, Object payload)
    @Override
    public void onItemRangeChanged(final int positionStart, final int itemCount) {
        onChanged();
    }

    @Override
    public void onItemRangeInserted(int positionStart, int itemCount) {
        //with new items put in a layout change has probably occurred
        onChanged();
    }

    @Override
    public void onItemRangeRemoved(int positionStart, int itemCount) {
        onChanged();
    }

    @Override
    public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
        onChanged();
    }



}
