/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk;

import java.util.HashMap;

/**
 * Created by vicvu on 16-07-28.
 */
public interface SessionInfoRetrievedListener {
    void sessionInfoRetrieved(HashMap sessionInfo);
    void onError(HashMap sessionInfo);
}
