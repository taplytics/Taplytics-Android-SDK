/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.network;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.taplytics.sdk.managers.TLManager;
import com.taplytics.sdk.utils.TLLog;

import org.apache.http.HttpEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.ContentType;
import org.apache.http.entity.mime.content.StringBody;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class MultipartRequest extends JsonRequest<JSONObject> {

    MultipartEntityBuilder entity = MultipartEntityBuilder.create();
    HttpEntity httpentity;
    Response.Listener<JSONObject> mListener;
    Response.ErrorListener mErrorListener;

    /* To hold the parameter name and the File to upload */
    private Map<String, String> headers = new HashMap<>();

    public MultipartRequest(int method,
                            String url,
                            JSONObject jsonRequest,
                            Map<String, byte[]> files,
                            Response.Listener<JSONObject> listener,
                            Response.ErrorListener errorListener) {
        super(method, url, (jsonRequest == null) ? null : jsonRequest.toString(), listener, errorListener);
        mErrorListener = errorListener;
        mListener = listener;
        buildMultipartEntity(files);
        // entity.
        if (jsonRequest != null) {
            try {
                entity.addPart("body", new StringBody(jsonRequest.toString(), ContentType.APPLICATION_JSON));
            } catch (Exception e) {
                TLLog.error("Adding string to multipart entity", e);
            }
        }
    }

    private void buildMultipartEntity(Map<String, byte[]> files) {
        for (Map.Entry<String, byte[]> entry : files.entrySet()) {
            entity.addPart("image", new ByteArrayBody(entry.getValue(), entry.getKey()));
        }
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        Map headers = new HashMap();
        try {
            headers.put(TLNetworking.ROUTING_TOKEN_HEADER_KEY, TLManager.getInstance().getRoutingToken());
        } catch (Throwable t) {
            TLLog.error("Error adding routing token", t);
        }
        return headers;
    }

    @Override
    public String getBodyContentType() {
        return httpentity.getContentType().getValue();
    }

    @Override
    public byte[] getBody() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            httpentity = entity.build();
            httpentity.writeTo(bos);
        } catch (IOException e) {
            VolleyLog.e("IOException writing to ByteArrayOutputStream");
        }
        return bos.toByteArray();
    }

    @Override
    protected void deliverResponse(JSONObject response) {
        mListener.onResponse(response);
    }

    @Override
    public void deliverError(VolleyError error) {
        if (error != null) {
            mErrorListener.onErrorResponse(error);
        }
        TLLog.error("Volley error", error);
    }

    @Override
    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            return Response.success(new JSONObject(jsonString), HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JSONException je) {
            return Response.error(new ParseError(je));
        }
    }


}
