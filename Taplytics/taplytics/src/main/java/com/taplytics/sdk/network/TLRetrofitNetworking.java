/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.network;

import com.taplytics.sdk.managers.TLManager;
import com.taplytics.sdk.managers.TLViewManager;
import com.taplytics.sdk.utils.TLLog;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;

public class TLRetrofitNetworking extends TLNetworking {

    private Retrofit retrofit;
    private TaplyticsService taplyticsService;
    private OkHttpClient okHttpClient;

    @Override
    public void setupNetworking(boolean isTest) {
        try {
            HashMap options = (HashMap) TLManager.getInstance().getStartingOptions();
            if (options.containsKey("retrofitTest")) {
                retrofit = (Retrofit) options.get("retrofitTest");
            } else {
                if (okHttpClient == null) {
                    OkHttpClient.Builder builder = new OkHttpClient.Builder();
                    try {
                        if (TLManager.getInstance().base64Enabled()) {
                            builder.addInterceptor(new Base64EncodeRequestInterceptor());
                        }
                        builder.addInterceptor(getLoggingInterceptor());
                    } catch (Throwable t) {
                        //OH NO
                    }
                    okHttpClient = builder.build();
                }

                if (retrofit == null) {
                    Retrofit.Builder builder =
                            new Retrofit.Builder()
                                    .baseUrl(environment.getBaseUrl())
                                    .client(okHttpClient);
                    if (isTest) {
                        builder.callbackExecutor(Executors.newSingleThreadExecutor());
                    }
                    retrofit = builder.build();
                }
            }
            taplyticsService = taplyticsService != null ? taplyticsService : retrofit.create(TaplyticsService.class);
            TLLog.debug("Started Retrofit and okHTTP", true);
        } catch (Throwable e) {
            TLLog.error("Error starting Retrofit and okHTTP. Falling back to Volley.", e, true, true);
        }
    }

    @Override
    public void clientRequest(Map<String, Object> props,
                              final TLNetworkPropertiesResponseListener listener,
                              final Date time) {
        try {
            //OKHTTP CALL
            Call call = taplyticsService.clientConfigRequest(props, TLManager.getInstance().getRoutingToken());
            TLLog.debug("Get Properties From Server, url: " + call.request().url().toString());

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                    try {
                        JSONObject responseObject = new JSONObject(response.body().string());
                        handleConfigResponse(responseObject, listener, time);
                    } catch (Throwable e) {
                        TLLog.error("Parsing TLProperties", e);
                        if (listener != null) {
                            listener.onError(e);
                        }
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    try {
                        handleErrorResponse(call.request().url().toString(), t, listener);
                    } catch (Throwable ignored) {
                        //
                    }
                }
            });
        } catch (Throwable t) {
            TLLog.error("error getting TLProperties from Server", t);
        }
    }


    @Override
    protected void enqueueImagePost(final String type,
                                    final String url,
                                    final String tag,
                                    final Date time,
                                    JSONObject params,
                                    final TLNetworkResponseListener listener) {
        try {
            ArrayList<MultipartBody.Part> parts = new ArrayList<>();
            RequestBody body;
            MultipartBody.Part partBody;
            MultipartBody.Part imagePart = null;
            if (type.equals("App Icon")) {
                imagePart = MultipartBody.Part.createFormData("image", "icon", RequestBody.create(MediaType.parse("image/jpg"), TLManager.getInstance().getAppIconBytes()));
                partBody = MultipartBody.Part.createFormData("body", null, RequestBody.create(MediaType.parse("application/json"), params.toString()));
            } else {
                for (Map.Entry<String, byte[]> entry : TLViewManager.getInstance().getImagesToSend().entrySet()) {
                    body = RequestBody.create(MediaType.parse("image/jpg"), entry.getValue());
                    partBody = MultipartBody.Part.createFormData("image", entry.getKey(), body);
                    parts.add(partBody);
                }
                body = RequestBody.create(MediaType.parse("application/json"), params.toString());
                partBody = MultipartBody.Part.createFormData("body", null, body);
            }
            Call call = null;
            switch (type) {
                case ("Activity Info"):
                    call = taplyticsService.postChosenActivity(parts, partBody);
                    break;
                case ("View Info"):
                    call = taplyticsService.postTapViewElements(parts, partBody);
                    break;
                case ("App Icon"):
                    call = taplyticsService.postAppIcon(imagePart, partBody);
                    break;
            }

            call.enqueue(
                    new Callback() {
                        @Override
                        public void onResponse(Call call, retrofit2.Response response) {
                            try {
                                TLLog.debug("Posted " + type, time);
                                if (listener != null) {
                                    JSONObject responseObject = new JSONObject(((ResponseBody) response.body()).string());
                                    listener.onResponse(responseObject);
                                }
                            } catch (Exception e) {
                                TLLog.error("Error parsing " + type + " post response");
                            }
                        }

                        @Override
                        public void onFailure(Call call, Throwable e) {
                            TLLog.requestError(url, "Posting " + type, e);
                            if (listener != null)
                                listener.onError(e);
                        }
                    });
        } catch (Throwable e) {
            //
        }
    }

    @Override
    public void enqueuePost(final String type,
                            String tag,
                            final String url,
                            final Date time,
                            JSONObject props,
                            final TLNetworkResponseListener listener) {
        try {
            Call call = null;
            switch (type) {
                case ("Client Events"):
                    if (retrofit.baseUrl().toString().contains("localhost")) {
                        call = taplyticsService.postClientEvents(TLManager.getInstance().getTLDeviceInfo().getDeviceProperties(), RequestBody.create(MediaType.parse("application/json"), props.toString()), TLManager.getInstance().getRoutingToken());
                    } else {
                        call = taplyticsService.postClientEvents(environment.getEventEndpointUrl(ENDPOINT_CLIENT_EVENT), TLManager.getInstance().getTLDeviceInfo().getDeviceProperties(), RequestBody.create(MediaType.parse("application/json"), props.toString()));
                    }
                    break;
                case ("User Attributes"):
                    call = taplyticsService.postAppUserAttributes(RequestBody.create(MediaType.parse("application/json"), props.toString()), TLManager.getInstance().getRoutingToken());
                    break;
                case ("GCM Token"):
                    call = taplyticsService.postGCMToken(RequestBody.create(MediaType.parse("application/json"), props.toString()), TLManager.getInstance().getRoutingToken());
                    break;
                case ("Reset App User"):
                    call = taplyticsService.postResetAppUser(RequestBody.create(MediaType.parse("application/json"), props.toString()), TLManager.getInstance().getRoutingToken());
                    break;
            }

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                    try {
                        TLLog.debug("Posted " + type, time);
                        JSONObject responseObject = new JSONObject(response.body().string());
                        listener.onResponse(responseObject);
                    } catch (Throwable e) {
                        TLLog.error("Parsing " + type + " Result", e);
                        if (listener != null) {
                            listener.onError(e);
                        }
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    try {
                        TLLog.requestError(url, "Posting " + type, t);
                    } catch (Throwable ignored) {

                    }
                    if (listener != null)
                        listener.onError(t);
                }
            });
        } catch (Throwable e) {
            TLLog.error("Error posting", e);
        }
    }

    @Override
    public void gitRequest(final TLNetworkResponseListener listener) {
        try {
            Request request = new Request.Builder()
                    .url("https://api.github.com/repos/taplytics/taplytics-android-sdk/releases")
                    .tag("github_release")
                    .build();

            okHttpClient.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    if (listener != null) {
                        listener.onError(e);
                    }
                }

                @Override
                public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                    try {
                        JSONObject release = new JSONArray(response.body().string()).optJSONObject(0);
                        if (listener != null) {
                            listener.onResponse(release);
                        }
                    } catch (Exception e) {
                        //
                    }
                }
            });
        } catch (Throwable e) {
            //
        }
    }

    private HttpLoggingInterceptor getLoggingInterceptor() {
        final HttpLoggingInterceptor logging = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
            @Override
            public void log(@NotNull String s) {
                TLLog.debug(s, false);
            }
        });
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        return logging;
    }

}
