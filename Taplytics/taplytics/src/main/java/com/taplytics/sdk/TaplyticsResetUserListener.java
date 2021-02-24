/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk;

/**
 * <p>
 * Interface for listening to when the Taplytics User has finished resetting.
 * </p>
 */
public interface TaplyticsResetUserListener {

    /**
     * Will callback when Taplytics is finished resetting the User.
     */
    public void finishedResettingUser();

}
