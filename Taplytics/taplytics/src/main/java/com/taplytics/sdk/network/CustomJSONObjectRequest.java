/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.network;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.taplytics.sdk.managers.TLManager;
import com.taplytics.sdk.utils.TLLog;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class CustomJSONObjectRequest extends JsonObjectRequest {

        public CustomJSONObjectRequest(int method, String url, JSONObject jsonRequest, Response.Listener listener, Response.ErrorListener errorListener)
        {
            super(method, url, jsonRequest, listener, errorListener);
        }

        @Override
        public Map getHeaders() throws AuthFailureError {
            Map headers = new HashMap();
            try {
                headers.put(TLNetworking.ROUTING_TOKEN_HEADER_KEY, TLManager.getInstance().getRoutingToken());
            } catch (Throwable t){
                TLLog.error("Error adding routing token", t);
            }
            return headers;
        }

}
