/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.datatypes;

import org.json.JSONException;
import org.json.JSONObject;


//TODO blow away this class
public class TLJSONObject extends JSONObject {

	public TLJSONObject(JSONObject obj) throws JSONException {
		super(obj.toString());
	}

	public boolean hasValue(String key) {
		return (has(key) && !isNull(key));
	}

	public TLJSONObject getTLJSONObject(String key) throws JSONException {
		JSONObject obj = getJSONObject(key);
		return new TLJSONObject(obj);
	}
}
