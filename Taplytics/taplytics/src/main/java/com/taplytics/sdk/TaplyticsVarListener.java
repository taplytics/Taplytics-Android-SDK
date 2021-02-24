/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk;

/**
 * see {@link TaplyticsVarListener}
 */
public interface TaplyticsVarListener {

    /**
     * Function that will be called when the variable has been updated.
     **/
    void variableUpdated(Object value);
}
