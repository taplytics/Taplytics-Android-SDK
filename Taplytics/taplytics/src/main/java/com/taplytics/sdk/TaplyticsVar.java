/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.taplytics.sdk.utils.TLLog;

import java.lang.ref.WeakReference;

/**
 * A dynamic variable that can be changed any time via the Taplytics dashboard
 */
public class TaplyticsVar<T> {

    /**
     * Value stored
     */
    @Nullable
    T value;

    /**
     * Listener to be invoked upon a change in the variable value
     */
    @Nullable
    private WeakReference<TaplyticsVarListener> listener;

    /**
     * This is the synchronous version of the variable. It will return a value right after the constructor
     * and will return that variable for the entire session
     *
     * @param name         the name of the variable
     * @param defaultValue the default value for the variable
     */
    public TaplyticsVar(@NonNull String name, @Nullable final T defaultValue) {
        this(name, defaultValue, null, null, true);
    }

    /**
     * This is the synchronous version of the variable. It will return a value right after the constructor
     * and will return that variable for the entire session
     *
     * @param name         the name of the variable
     * @param defaultValue the default value for the variable
     * @param type         Type of the value
     */
    public TaplyticsVar(@NonNull String name,
                        @Nullable final T defaultValue,
                        @Nullable Class<?> type) {
        this(name, defaultValue, type, null, true);
    }

    /**
     * This is the async version of the variable. The listener will callback when and only
     * when we have the correct value for this variable
     *
     * @param name         the name of the variable
     * @param defaultValue the default value for the variable
     * @param listener     the listener to be called when the variables value has been set
     *                     this _can_ get called during the constructor, but it also might not.
     */
    public TaplyticsVar(@NonNull String name,
                        @Nullable final T defaultValue,
                        @NonNull TaplyticsVarListener listener) {
        this(name, defaultValue, null, listener, false);
    }

    /**
     * This is the async version of the variable. The listener will callback when and only when we
     * have the correct value for this variable
     *
     * @param name         the name of the variable
     * @param defaultValue the default value for the variable
     * @param type         Type of the value
     * @param listener     the listener to be called when the variables value has been set
     *                     this _can_ get called during the constructor, but it also might not.
     */
    public TaplyticsVar(@NonNull String name,
                        @Nullable final T defaultValue,
                        @Nullable Class<?> type,
                        @NonNull TaplyticsVarListener listener) {
        this(name, defaultValue, type, listener, false);
    }

    /**
     * Creates an async or sync version of the variable depending on the parameters passed.
     *
     * @param name         The name of the variable
     * @param defaultValue The default value for the variable
     * @param type         Type of the value
     * @param listener     The listener to be called when the variables value has been set
     *                     this _can_ get called during the constructor, but it also might not.
     * @param synchronous  When set to true, will treat this as a synchronous operation
     */
    private TaplyticsVar(@NonNull String name,
                         @Nullable final T defaultValue,
                         @Nullable Class<?> type,
                         @Nullable TaplyticsVarListener listener,
                         boolean synchronous) {
        try {
            if (defaultValue != null) {
                type = defaultValue.getClass();
            } else {
                TLLog.warning("If you supply a null default value, please supply a class type.");
            }
            this.listener = new WeakReference<>(listener);
            value = TLDynamicVariableManager
                    .getInstance()
                    .setAndReturnVariableValue(name, defaultValue, this, synchronous, type);
        } catch (Exception e) {
            value = defaultValue;
            TLLog.error("exception starting TaplyticsVar", e);
        }
    }

    /**
     * Retrieves value of variable
     *
     * @return Value of variable
     */
    @Nullable
    public T get() {
        TLLog.debug("Got variable value: " + value, true);
        return value;
    }

    /**
     * Updates value and calls the listener of the variable if one exists
     *
     * @param newValue Value to be passed into the callback
     */
    void updateValue(T newValue) {
        this.value = newValue;

        if (listener == null || listener.get() == null) {
            TLLog.debug("TaplyticsVar - Listener not set");
            return;
        }

        listener.get().variableUpdated(newValue);
    }

}
