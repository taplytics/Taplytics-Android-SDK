/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.network;

import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.NoCache;
import com.taplytics.sdk.managers.TLManager;
import com.taplytics.sdk.managers.TLViewManager;
import com.taplytics.sdk.utils.TLLog;
import com.taplytics.sdk.utils.TLUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TLVolleyNetworking extends TLNetworking {

    final static private String ENDPOINT_CLIENT_CONFIG = "/api/v1/clientConfig/";

    //for testing
    void setRequestQueue(RequestQueue requestQueue) {
        this.requestQueue = requestQueue;
    }

    private RequestQueue requestQueue;

    @Override
    public void setupNetworking(boolean isTest) {
        try {
            if (requestQueue == null) {
                requestQueue = new RequestQueue(new NoCache(), new BasicNetwork(new HurlStack()));
            }
            requestQueue.start();
            TLLog.debug("Starting Volley", true);
        } catch (Throwable e) {
            //
        }
    }

    @Override
    public void clientRequest(Map props,
                              final TLNetworkPropertiesResponseListener listener,
                              final Date time) {
        try {
            StringBuilder url = new StringBuilder(environment.getApiEndpointUrl(ENDPOINT_CLIENT_CONFIG));
            final String baseUrl = url.toString();
            TLUtils.addURLParamsFromDic(props, url);

            TLLog.debug("Get Properties From Server, url: " + url.toString().replaceAll(" ", "%20"));

            CustomJSONObjectRequest request = new CustomJSONObjectRequest(Method.GET, url.toString().replaceAll(" ", "%20"), new JSONObject(),
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                handleConfigResponse(response, listener, time);
                                TLLog.debug("TLVolleyNetworking " + response.toString(), false);
                            } catch (Throwable e) {
                                TLLog.error("Parsing TLProperties", e);
                                if (listener != null) {
                                    listener.onError(e);
                                    TLLog.debug("TLVolleyNetworkingError " + e.getMessage(), false);
                                }
                            }
                        }

                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    handleErrorResponse(baseUrl, error, listener);
                }

            });

            request.setTag("get_clientConfig");
            RetryPolicy policy = new DefaultRetryPolicy(15000, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
            request.setRetryPolicy(policy);
            request.setShouldCache(false);
            requestQueue.add(request);
            requestQueue.getCache().clear();
        } catch (Throwable e) {
        }
    }

    @Override
    public void enqueueImagePost(final String type,
                                 final String url,
                                 String tag,
                                 final Date time,
                                 JSONObject params,
                                 final TLNetworkResponseListener listener) {
        try {
            Map imageMap = new HashMap<>();
            if (type.equals("App Icon")) {
                byte[] imageBytes = TLManager.getInstance().getAppIconBytes();
                if (imageBytes != null && imageBytes.length > 0) {
                    imageMap.put("icon", imageBytes);
                } else {
                    TLLog.error("app icon null");
                    return;
                }
            } else {
                imageMap = TLViewManager.getInstance().getImagesToSend();
            }
            MultipartRequest request = new MultipartRequest(Method.POST, url, params, imageMap,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            TLLog.debug("Posted " + type, time);
                            if (listener != null)
                                listener.onResponse(response);
                            TLLog.debug("TLVolleyNetworking " + response.toString(), false);
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    TLLog.requestError(url, "Posting " + type, error);
                    if (listener != null)
                        listener.onError(error);
                    TLLog.debug("TLVolleyNetworkingError " + error.getMessage(), false);
                }
            });
            request.setShouldCache(false);
            request.setTag(tag);
            requestQueue.add(request);
        } catch (Throwable e) {
            if (e.getMessage() != null) {
                TLLog.error(e.getMessage());
                return;
            }
            TLLog.error("Failed to post image");
        }
    }


    @Override
    public void enqueuePost(final String type,
                            final String tag,
                            final String url,
                            final Date time,
                            final JSONObject props,
                            final TLNetworkResponseListener listener) {
        try {

            CustomJSONObjectRequest request = new CustomJSONObjectRequest(Method.POST, url.replaceAll(" ", "%20"), props, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    TLLog.debug("Posted " + type, time);
                    listener.onResponse(response);
                    TLLog.debug("TLVolleyNetworking " + response.toString(), false);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    try {
                        TLLog.requestError(url, "Posting " + type + " failed. Status code: " + String.valueOf(error.networkResponse.statusCode) + "Error has this data: ", error);
                    } catch (Throwable ignored) {

                    }
                    try {
                        if (error.networkResponse.data != null) {
                            String body = new String(error.networkResponse.data, "UTF-8");
                            TLLog.requestError(url, "Error body is: " + body, error);
                            TLLog.debug("TLVolleyNetworkingError " + error.getMessage(), false);
                        }
                    } catch (Throwable ignored) {

                    }
                    listener.onError(error);
                }
            });

            request.setRetryPolicy(new DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, 10, 1.5f));
            request.setTag(tag);
            request.setShouldCache(false);
            requestQueue.add(request);
        } catch (Throwable e) {
            TLLog.error("v e", e);

        }
    }

    @Override
    public void gitRequest(final TLNetworkResponseListener listener) {
        try {
            JsonArrayRequest request = new JsonArrayRequest("https://api.github.com/repos/taplytics/taplytics-android-sdk/releases",
                    new Response.Listener<JSONArray>() {
                        @Override
                        public void onResponse(JSONArray response) {
                            JSONObject release = response.optJSONObject(0);
                            if (listener != null) {
                                listener.onResponse(release);
                                TLLog.debug("TLVolleyNetworking " + response.toString(), false);
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    if (listener != null) {
                        listener.onError(error);
                        TLLog.debug("TLVolleyNetworkingError " + error.getMessage(), false);
                    }
                }
            });
            request.setTag("github_release");
            requestQueue.add(request);
        } catch (Throwable e) {

        }
    }

}
