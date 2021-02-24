/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.datatypes;

import com.taplytics.sdk.analytics.external.AmplitudeManager;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * Created by vicvu on 2016-10-25.
 */

public class TLOkHttpClient extends OkHttpClient {
    @Override
    public Call newCall(Request request) {
        try {
            AmplitudeManager.getInstance().getValuesAndUpload();
        } catch (Throwable e){
            //
        }
        return super.newCall(request);
    }
}
