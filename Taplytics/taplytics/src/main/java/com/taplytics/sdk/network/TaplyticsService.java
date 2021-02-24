/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.network;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.QueryMap;
import retrofit2.http.Url;

/**
 * Created by vicvu on 16-05-03.
 */
interface TaplyticsService {
    @GET("/api/v1/clientConfig/")
    Call<ResponseBody> clientConfigRequest(@QueryMap Map<String, Object> params, @Header(TLNetworking.ROUTING_TOKEN_HEADER_KEY) String token);

    @Multipart
    @POST("/api/v1/experimentElements/")
    Call<ResponseBody> postTapViewElements(@Part List<MultipartBody.Part> partList, @Part MultipartBody.Part params);

    @Multipart
    @POST("/api/v1/chosenView/")
    Call<ResponseBody> postChosenActivity(@Part List<MultipartBody.Part> partList, @Part MultipartBody.Part params);

    @POST
    Call<ResponseBody> postClientEvents(@Url String fullUrl, @QueryMap Map<String, Object> params, @Body RequestBody body);

    @POST("/api/v1/clientEvents/")
    Call<ResponseBody> postClientEvents(@QueryMap Map<String, Object> params, @Body RequestBody body, @Header(TLNetworking.ROUTING_TOKEN_HEADER_KEY) String token);

    @Multipart
    @POST("/api/v1/images/")
    Call<ResponseBody> postAppIcon(@Part MultipartBody.Part appIcon, @Part MultipartBody.Part params);

    @POST("/api/v1/clientAppUser/")
    Call<ResponseBody> postAppUserAttributes(@Body RequestBody body, @Header(TLNetworking.ROUTING_TOKEN_HEADER_KEY) String token);

    @POST("/api/v1/resetAppUser/")
    Call<ResponseBody> postResetAppUser(@Body RequestBody body, @Header(TLNetworking.ROUTING_TOKEN_HEADER_KEY) String token);

    @POST("/api/v1/pushToken/")
    Call<ResponseBody> postGCMToken(@Body RequestBody body, @Header(TLNetworking.ROUTING_TOKEN_HEADER_KEY) String token);
}

