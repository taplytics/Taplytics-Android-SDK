/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.datatypes;

import org.json.JSONArray;
import org.json.JSONException;

public class TLJSONArray extends JSONArray {

    public TLJSONArray() {
        super();
    }

    public TLJSONArray(JSONArray arry) throws JSONException {
        super(arry.toString());
    }

    public TLJSONArray removeIndex(int index) throws JSONException {
        TLJSONArray newArry = new TLJSONArray();
        for (int i=0; i < this.length(); i++) {
            if (i != index)
                newArry.put(this.opt(i));
        }
        return newArry;
    }

    public JSONArray getJSONArray() throws JSONException {
        return new JSONArray(this.toString());
    }

    public Boolean containsString(String str) {
        return str != null && this.toString().contains(str);
    }
}
