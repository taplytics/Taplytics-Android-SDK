/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.listeners;

import android.database.DataSetObserver;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;

/**
 * Created by clvcooke on 12/4/15.
 */
public class TLListViewDataSetObserver extends DataSetObserver {

    public ViewTreeObserver.OnPreDrawListener getPreDrawListener() {
        return preDrawListener;
    }

    private ViewTreeObserver.OnPreDrawListener preDrawListener;

    AbsListView listView = null;

    public TLListViewDataSetObserver(final AbsListView listView) {
        this.listView = listView;
        preDrawListener = new TLOnPreDrawListener(listView.getViewTreeObserver(), listView);
    }

    @Override
    public void onChanged() {
        listView.getViewTreeObserver().addOnPreDrawListener(preDrawListener);
    }

    @Override
    public void onInvalidated() {
        listView.getViewTreeObserver().addOnPreDrawListener(preDrawListener);
    }
}
