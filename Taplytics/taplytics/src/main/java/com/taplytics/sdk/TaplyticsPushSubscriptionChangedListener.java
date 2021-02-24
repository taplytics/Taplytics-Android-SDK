/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk;

/**
 * /**
 * A listener that provides callbacks with the use of {@link com.taplytics.sdk.Taplytics#setPushSubscriptionEnabled(boolean, TaplyticsPushSubscriptionChangedListener)}
 * <p/>
 * {@link #success()} is called when the change has been succesfully pushed to the taplytics server
 * <p/>
 * {@link #failure()} is called when a connection could not be established to the taplytics server
 */
public interface TaplyticsPushSubscriptionChangedListener {

    void success();

    public void failure();

}
