/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.network;

import android.util.Base64;

import com.taplytics.sdk.utils.TLLog;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;


/**
 * This is a network interceptor for retrofit which intercepts the network calls to base64 the query parameters.
 */
class Base64EncodeRequestInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        try {
            Request originalRequest = chain.request();
            HttpUrl originalURL = originalRequest.url();
            String query = originalURL.encodedQuery();
            HttpUrl.Builder urlBuilder = new HttpUrl.Builder();
            byte[] queryEncoded;

            if (query != null) {
                queryEncoded = Base64.encode(query.getBytes(), 0);
                urlBuilder.encodedPath(originalURL.encodedPath() + new String(queryEncoded));
            } else {
                urlBuilder.encodedPath(originalURL.encodedPath());
            }
            HttpUrl url = urlBuilder.scheme(originalURL.scheme())
                    .port(originalURL.port())
                    .host(originalURL.host())
                    .build();

            return chain.proceed(originalRequest.newBuilder().url(url).build());
        } catch (Throwable e) {
            TLLog.error("Error fixing url", e);
            return chain.proceed(chain.request());
        }
    }
}