/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.listeners;

/**
 * Created by matthewkuzyk on 3/27/15.
 */
public interface TLFlushListener {
    void flushCompleted(boolean success);
    void flushFailed();
}
