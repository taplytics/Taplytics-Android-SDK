/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.utils;

import android.os.AsyncTask;

import com.taplytics.sdk.managers.TLManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by vicvu on 2016-10-30.
 *
 * This is an asynctask used to organize our list and recycler items into known sets.
 *
 * What we want, is for when a list is shown, we ONLY apply setProperties to
 * views which are known to be in that list and ONLY apply it to the positions we want.
 */

public class TLListItemOrganizerAsyncTask extends AsyncTask {

    @Override
    protected Object doInBackground(Object[] params) {
        try {

            JSONArray views = (JSONArray) params[0];

            //Separate list for recycler items and list items.
            //Could honestly probably be the same list, but will look into that in the future.
            HashMap<String, HashSet<Integer>> listItems = new HashMap<>();
            HashMap<String, HashSet<Integer>> recItems = new HashMap<>();

            JSONObject initProps, viewObject;
            String identifier;
            for (int i = 0; i < views.length(); i++) {
                try {
                    viewObject = views.getJSONObject(i);
                    initProps = viewObject.optJSONObject("initProperties");
                    identifier = initProps.optString("anIdentifier");
                    JSONObject cellInfo = initProps.optJSONObject("cellInfo");
                    if (cellInfo != null) {
                        if (identifier != null && !identifier.equals("")) {
                            final int position = cellInfo.getInt("position");
                            String listIdentifier = cellInfo.has("listIdentifier") ? cellInfo.getString("listIdentifier") : "unknown";
                            if (initProps.optBoolean("isInRecycler")) {
                                HashSet<Integer> positions = recItems.get(listIdentifier);
                                if (positions == null) {
                                    positions = new HashSet<>();
                                }
                                positions.add(position);
                                recItems.put(listIdentifier, positions);
                            } else if (initProps.optBoolean("isInListView")) {
                                HashSet<Integer> positions = listItems.get(listIdentifier);
                                if (positions == null) {
                                    positions = new HashSet<>();
                                }
                                positions.add(position);
                                listItems.put(listIdentifier, positions);
                            }
                        }
                    }
                } catch (Throwable e) {
                    //
                }
            }

            TLManager.getInstance().getTlProperties().setKnownRecyclerPositions(recItems);
            TLManager.getInstance().getTlProperties().setKnownListPositions(listItems);

        } catch (Throwable e) {
//
        }
        return null;
    }
}
