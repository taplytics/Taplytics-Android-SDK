/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.taplytics.sdk.datatypes.TLJSONObject;
import com.taplytics.sdk.datatypes.TLProperties;
import com.taplytics.sdk.managers.TLManager;
import com.taplytics.sdk.managers.TLViewManager;
import com.taplytics.sdk.network.TLSocketManager;
import com.taplytics.sdk.utils.TLLog;
import com.taplytics.sdk.utils.TLUtils;
import com.taplytics.sdk.utils.promises.Promise;
import com.taplytics.sdk.utils.promises.PromiseListener;

import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Responsible for updating and keeping {@link TaplyticsVar} in sync with the dashboard
 */
public class TLDynamicVariableManager {

    private final WeakHashMap<String, TaplyticsVar<?>> variableMap = new WeakHashMap<>();

    private final HashMap<String, Object> synchronousVariableValueMap = new HashMap<>();

    /**
     * Instance of {@link TLDynamicVariableManager}
     **/
    private static TLDynamicVariableManager instance;

    /**
     * @return {@link TLDynamicVariableManager#instance}
     */
    @NonNull
    public static TLDynamicVariableManager getInstance() {
        if (instance == null) {
            instance = new TLDynamicVariableManager();
        }
        return instance;
    }

    /**
     * Sets the value for the {@link TaplyticsVar}. Value be set to default if the value cannot
     * be set correctly.
     *
     * @param name         Name of the variable
     * @param defaultValue Default value of the variable
     * @param var          {@link TaplyticsVar} to set the value of
     * @param synchronous  Is this a synchronous operation
     * @param type         Type of the variable's value
     * @param <T>          of the {@link TaplyticsVar}
     * @return The value of the {@link TaplyticsVar}
     */
    <T> T setAndReturnVariableValue(@NonNull final String name,
                                    @Nullable final T defaultValue,
                                    @NonNull final TaplyticsVar<T> var,
                                    boolean synchronous,
                                    @Nullable final Class<?> type) {
        T variableValue = defaultValue;
        if (TLManager.getInstance().isLiveUpdate()) {
            variableMap.put(name, var);
        }

        //if we aren't in live update mode and we are in synchronous mode we need to make sure
        // this is the only value of the variable which is returned for this session
        if (!TLManager.getInstance().isLiveUpdate() &&
                synchronous &&
                synchronousVariableValueMap.containsKey(name)) {
            //check if we have already returned a value for this variable
            //if so just return that value
            return (T) synchronousVariableValueMap.get(name);
        }

        if (TLManager.getInstance().getTlProperties() != null) {
            variableValue = getVarValueFromProperties(name, defaultValue, type);
            var.updateValue(variableValue);
        } else if (!synchronous) {
            //Wait for TLProperties to update if we are not synchronous
            fetchPropertiesAndUpdateVar(name, defaultValue, var, type);
        }

        //if we are synchronous and we are not in live update mode we put the value in a map so we don't return a differnt value later on
        if (!TLManager.getInstance().isLiveUpdate() && synchronous) {
            synchronousVariableValueMap.put(name, variableValue);
        }

        TLLog.debug("Variable '" + name + "' set to: " + variableValue, true);
        return variableValue;
    }

    /**
     * Fetches properties from backend and will update the dynamic variable if the value has changed.
     *
     * @param name         Name of the variable
     * @param defaultValue Default value of the variable
     * @param var          The stored {@link TaplyticsVar}
     * @param type         Type of the object
     * @param <T>          Type of TaplyticsVar
     */
    private <T> void fetchPropertiesAndUpdateVar(@NonNull final String name,
                                                 @Nullable final T defaultValue,
                                                 @NonNull final TaplyticsVar<T> var,
                                                 @Nullable final Class<?> type) {
        PromiseListener<Void> variableListener = new PromiseListener<Void>() {
            @Override
            public void succeeded() {
                super.completed();
                // If the properties we get back are null, try to call the variables listener
                // with defaultValue and return
                if (TLManager.getInstance().getTlProperties() == null) {
                    var.updateValue(defaultValue);
                    return;
                }

                final TLJSONObject vars = TLManager.getInstance().getTlProperties().getDynamicVars();

                // If there are no dynamic variables with the name supplied, create a variable
                // with defaultValue and return
                final boolean varNotFound = vars != null && !vars.has(name);
                if (vars == null || varNotFound) {
                    socketPush(name, defaultValue, type);
                    var.updateValue(defaultValue);
                    return;
                }

                JSONObject obj = null;
                if (vars != null) {
                    obj = vars.optJSONObject(name);
                }

                // We really don't want this if it doesn't even have a value. Just push to the server and stick to default;
                if ((obj == null || !obj.has("value")) && TLManager.getInstance().isLiveUpdate()) {
                    socketPush(name, defaultValue, type);
                    return;
                }

                final Object variableValue = getVarValueFromProperties(name, defaultValue, type);

                final boolean isVarInMap = variableMap.containsKey(name) &&
                        variableMap.get(name) instanceof TaplyticsVar;
                final TaplyticsVar<?> variable;
                if (TLManager.getInstance().isLiveUpdate() && isVarInMap) {
                    //Change on stored variable.
                    variable = (TaplyticsVar<?>) variableMap.get(name);
                } else {
                    //Change right here right now if it still exists.
                    variable = var;
                }

                if (variable == null) {
                    return;
                }

                String variableType = type.getSimpleName();
                if (variableType.equals("JSONObject")) {
                    variableType = "JSON";
                }

                updateVariableValue(variable, variableValue, name, defaultValue, variableType);
            }

            //If we have failed, the correct value for this session is the default value
            @Override
            public void failedOrCancelled() {
                super.failedOrCancelled();
                var.updateValue(defaultValue);
            }
        };
        TLManager.getInstance().getTlPropertiesPromise().add(variableListener);
    }

    /**
     * Updates the value of the variable from the data in properties.
     *
     * @param name         Name of the variable to update
     * @param defaultValue Default value of the variable
     * @param type         Type of the variable
     * @param <T>          Return type
     * @return Value of the variable
     */
    private <T> T getVarValueFromProperties(@NonNull final String name,
                                            @Nullable T defaultValue,
                                            @NonNull final Class<?> type) {
        T variableValue = defaultValue;

        // Grab all our variables.
        final TLJSONObject vars = TLManager.getInstance().getTlProperties().getDynamicVars();

        // If the variable doesnt exist, create a new one with a default value
        if (vars == null || vars.optJSONObject(name) == null || vars.optJSONObject(name).opt("value") == null) {
            socketPush(name, defaultValue, type);
            return variableValue;
        }

        try {
            variableValue = getValueFromJsonVar(vars, name, type);
        } catch (Exception e) {
            socketPush(name, defaultValue, type);
            TLLog.warning(e.getMessage());
        }

        if (!TLManager.getInstance().isLiveUpdate()) {
            return variableValue;
        }

        Object oldDefault = vars.optJSONObject(name).opt("defaultValue");
        Object currentDefault = defaultValue;

        // If the variable is JSON, we need to do some work to get the correct default values
        if (vars.optJSONObject(name).optString("variableType").equals("JSON") || type == JSONObject.class) {
            oldDefault = oldDefault == null ? new Object().toString() : oldDefault.toString();
            currentDefault = currentDefault == null ? null : currentDefault.toString();
        }

        // If the old default isnt the same as the current default, flush data to backend
        if (!oldDefault.equals(currentDefault)) {
            socketPush(name, defaultValue, type);
        }

        return variableValue;
    }

    /**
     * This method will return the object stored in vars with the given name.
     *
     * @param vars JSONObject that contains the variables
     * @param name Name of the object
     * @param type Type of the object
     * @param <T>  Return type
     * @return The value from the JSON as a type T
     * @throws Exception Thrown if value cannot be found from variable JSON
     */
    private <T> T getValueFromJsonVar(@NonNull TLJSONObject vars,
                                      @NonNull final String name,
                                      @NonNull final Class<?> type) throws Exception {
        final Object value = vars.optJSONObject(name).opt("value");
        final String valueClass = value.getClass().getSimpleName();
        final String typeClass = type.getSimpleName();

        if (value.getClass().equals(type) || valueClass.toLowerCase().equals(typeClass.toLowerCase())) {
            return (T) value;
        } else if (value instanceof String && vars.optJSONObject(name).optString("variableType").equals("JSON")) {
            return (T) new JSONObject((String) value);
        } else if (valueClass.contains("Double") || typeClass.contains("Double") ||
                valueClass.contains("Float") || typeClass.contains("Float") ||
                typeClass.contains("Number")) {
            return (T) value;
        } else {
            throw new IllegalArgumentException("Variable types do no match. For " +
                    name + " expected: " + type + " but found: " +
                    value.getClass() + ". Check variable config and try clearing app data.");
        }
    }


    /**
     * A function to create a JSONObject to push to the server so we can officially create a variable.
     *
     * @param name         Name of the variable
     * @param defaultValue Default value of the variable
     * @param type         Variable Type
     */
    private void socketPush(@NonNull final String name,
                            @Nullable final Object defaultValue,
                            @Nullable final Class<?> type) {
        if (!TLManager.getInstance().isLiveUpdate()) {
            return;
        }

        TLSocketManager.getInstance().getSocketRoomPromise().add(new PromiseListener<Void>() {
            @Override
            public void succeeded() {
                super.succeeded();
                try {
                    String variableType = "unknown";
                    try {
                        variableType = (type == null ? defaultValue.getClass().getSimpleName() : type.getSimpleName());
                    } catch (Throwable t) {
                        //Safety.
                    }

                    final Object defValueToPush;
                    if (variableType.equals("JSONObject")) {
                        variableType = "JSON";
                        defValueToPush = defaultValue.toString();
                    } else {
                        defValueToPush = defaultValue;
                    }

                    final JSONObject object = new JSONObject();
                    object.put("name", name);
                    object.put("createdAt", new Date());
                    object.put("defaultVal", defValueToPush);
                    object.put("variableType", variableType);

                    TLSocketManager.getInstance().emitSocketEvent("newDynamicVar", object);
                } catch (Exception e) {
                    TLLog.error("Problem pushing dynamic var to socket", e);
                }
            }
        });
    }


    /**
     * Updates the value from the socket and will invoke any {@link TaplyticsVar} callbacks if
     * the value has changed.
     *
     * @param arg Variable data
     */
    public void updateFromSocket(@NonNull JSONObject arg) {
        final String name = arg.optString("name");
        final TLManager tlManager = TLManager.getInstance();
        final TLViewManager viewManager = TLViewManager.getInstance();

        final String experimentName = viewManager.getExperimentName();
        final String variationName = viewManager.getVariationName();

        if (experimentName == null || variationName == null) {
            return;
        }

        final TLProperties tlProperties = tlManager.getTlProperties();
        final JSONObject experiment = tlProperties.getExperimentByName(experimentName);

        final String variation;
        if (name.equals("baseline")) {
            variation = name;
        } else {
            variation = tlProperties.getVariationByName(experiment, variationName).optString("_id");
        }

        final Map<String, Object> expMap = new HashMap<>();
        expMap.put("exp", experiment.optString("_id"));
        expMap.put("var", variation);

        Promise<Object> tlPropertiesPromise = new Promise<>();
        tlPropertiesPromise.add(new PromiseListener<Object>() {
            @Override
            public void succeeded() {
                final JSONObject variable =
                        tlManager.getTlProperties().getDynamicVars().optJSONObject(name);
                final Object value = variable.opt("value");
                final Object defaultValue = variable.opt("defaultValue");
                final String varType = variable.optString("variableType");

                //Grab the value in the map if it exists.
                TLUtils.runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (value == null) {
                            return;
                        }

                        if (!variableMap.containsKey(name) ||
                                !(variableMap.get(name) instanceof TaplyticsVar)) {
                            return;
                        }

                        TaplyticsVar var = variableMap.get(name);
                        //update it
                        updateVariableValue(var, value, name, defaultValue, varType);
                    }
                });

                super.succeeded();
            }
        });
        TLLog.debug("Client updated variable: " + name);
        tlManager.getPropertiesFromServer(expMap, tlPropertiesPromise);
    }


    /**
     * If the experiment and/or variation is changed through the shake menu, update the variables
     * using {@link TLProperties#getDynamicVars()}
     *
     * @param experimentName Name of the experiment changed to
     * @param variationName  Name of the variation changed to
     */
    public void notifyChangeIfNecessary(@NonNull String experimentName,
                                        @NonNull String variationName) {
        final TLProperties properties = TLManager.getInstance().getTlProperties();
        if (properties == null) {
            return;
        }

        final JSONObject experiment = properties.getExperimentByName(experimentName);

        // If experiment has no dynamic variables, then we are done
        final boolean hasDynamicVariables = experiment.optBoolean("hasDynamicVariables", false);
        if (experiment == null || !hasDynamicVariables) {
            return;
        }

        //Grab the variation we've switched to.
        final JSONObject variation = properties.getVariationByName(experiment, variationName);
        if (variation == null) {
            return;
        }

        final JSONObject dynamicVars = properties.getDynamicVars();
        final Iterator<String> varNames = dynamicVars.keys();
        while (varNames.hasNext()) {
            try {
                final String varName = varNames.next();

                if (!(dynamicVars.get(varName) instanceof JSONObject)) {
                    continue;
                }

                final JSONObject varObject = ((JSONObject) dynamicVars.get(varName));
                if (varObject == null) {
                    continue;
                }

                final TaplyticsVar<?> var = variableMap.get(varObject.optString("name"));
                if (var == null) {
                    continue;
                }

                //Update it!
                updateVariableValue(var,
                        varObject.get("value"),
                        varObject.getString("name"),
                        varObject.get("defaultValue"),
                        varObject.optString("variableType"));

            } catch (Throwable e) {
                TLLog.error("notifying change inner", e);
            }
        }
    }

    /**
     * Updates the variable value if there is a change in its value. The variable's callback will not
     * be called if the value has not changed, even if a new session has started.
     *
     * @param variable     The stored {@link TaplyticsVar}
     * @param value        The value received from the server / TLProperties
     * @param varName      The name of the variable
     * @param defaultValue Default value of the variable (fallback)
     * @param variableType The type this variable will be cast to.
     */
    private <T> void updateVariableValue(@Nullable TaplyticsVar<T> variable,
                                         @Nullable Object value,
                                         @NonNull String varName,
                                         @Nullable Object defaultValue,
                                         @NonNull String variableType) {
        if (variable == null) {
            return;
        }

        final T object = variable.value;
        if (variableType.equals("JSON")) {
            try {
                value = new JSONObject(String.valueOf(value));
            } catch (Exception e) {
                TLLog.error("JSON cast error", e);
            }
        }

        //Don't change things if we don't have to.
        final boolean isVarJsonAndHasSameValue = variableType.equals("JSON") &&
                value != null &&
                value.toString().equals(object.toString());
        final boolean isValueEqualToObject = value != null && value.equals(object);
        if (isValueEqualToObject || isVarJsonAndHasSameValue) {
            return;
        }

        try {
            //Force a casting to make sure the types match up.
            T castTestObject = (T) value;
        } catch (Throwable t) {
            TLLog.warning("Casting issue on dynamic variable", (t instanceof Exception) ? (Exception) t : null);
            socketPush(varName, defaultValue, null);
            return;
        }

        final T castedVal = (T) value;

        TLLog.debug("Variable '" + varName + "' updated to: " + castedVal, true);

        // Update variable
        variable.updateValue(castedVal);
    }

    /**
     * Clears the map {@link #synchronousVariableValueMap}
     */
    public void clearSynchronousVariables() {
        synchronousVariableValueMap.clear();
    }

    /**
     * Checks {@link TLProperties#getDynamicVars()} for dynamic variables and updates accordingly.
     */
    public void checkForVariableUpdates() {
        final JSONObject dynamicVars = TLManager.getInstance().getTlProperties().getDynamicVars();
        for (String name : variableMap.keySet()) {
            TaplyticsVar<?> var = variableMap.get(name);
            JSONObject propVar = dynamicVars.optJSONObject((String) name);

            if (propVar == null) {
                continue;
            }
            updateVariableValue(var,
                    propVar.opt("value"),
                    (String) name,
                    propVar.opt("defaultValue"),
                    propVar.optString("variableType"));
        }
    }
}
