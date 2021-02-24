/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.analytics;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.taplytics.sdk.SessionInfoRetrievedListener;
import com.taplytics.sdk.TaplyticsPushSubscriptionChangedListener;
import com.taplytics.sdk.TaplyticsResetUserListener;
import com.taplytics.sdk.TaplyticsSetUserAttributesListener;
import com.taplytics.sdk.datatypes.TLProperties;
import com.taplytics.sdk.listeners.TLFlushListener;
import com.taplytics.sdk.managers.TLManager;
import com.taplytics.sdk.managers.TLThreadManager;
import com.taplytics.sdk.network.TLNetworking;
import com.taplytics.sdk.utils.SecurePrefs;
import com.taplytics.sdk.utils.TLLog;
import com.taplytics.sdk.utils.TLReaderWriter;
import com.taplytics.sdk.utils.TLUtils;
import com.taplytics.sdk.utils.promises.Promise;
import com.taplytics.sdk.utils.promises.PromiseListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.taplytics.sdk.utils.constants.AppUserConstants.AGE;
import static com.taplytics.sdk.utils.constants.AppUserConstants.ANONYMOUS_PREFIX;
import static com.taplytics.sdk.utils.constants.AppUserConstants.API_KEY;
import static com.taplytics.sdk.utils.constants.AppUserConstants.APP_USER_ID;
import static com.taplytics.sdk.utils.constants.AppUserConstants.AVATAR_URL;
import static com.taplytics.sdk.utils.constants.AppUserConstants.CUSTOM_DATA;
import static com.taplytics.sdk.utils.constants.AppUserConstants.EMAIL;
import static com.taplytics.sdk.utils.constants.AppUserConstants.FIRST_NAME;
import static com.taplytics.sdk.utils.constants.AppUserConstants.GENDER;
import static com.taplytics.sdk.utils.constants.AppUserConstants.LAST_NAME;
import static com.taplytics.sdk.utils.constants.AppUserConstants.NAME;
import static com.taplytics.sdk.utils.constants.AppUserConstants.PROJECT_ID;
import static com.taplytics.sdk.utils.constants.AppUserConstants.PUSH_ENABLED;
import static com.taplytics.sdk.utils.constants.AppUserConstants.SESSION_ID;
import static com.taplytics.sdk.utils.constants.AppUserConstants.SESSION_INFO_SESSION_ID_KEY;
import static com.taplytics.sdk.utils.constants.AppUserConstants.SPECIAL_KEY;
import static com.taplytics.sdk.utils.constants.AppUserConstants.SPECIAL_KEY_VALUE;
import static com.taplytics.sdk.utils.constants.AppUserConstants.USER_ATTRIBUTES;
import static com.taplytics.sdk.utils.constants.AppUserConstants.USER_ID;
import static com.taplytics.sdk.utils.constants.DeviceConstants.DEVICE_ID_DICTIONARY;

public class TLAppUser {

    private static final long REPORTING_TIMEOUT = 30;
    private static final long MAX_START_NEW_SESSION_TIME = 60000;
    private static final int MAX_NEW_SESSION = 3;

    private int newSessionCounter;
    private Date lastNewSession = null;

    private JSONObject userAttributes;

    private String userID;

    private boolean queuedUserAttributes = false;

    private JSONObject getUserAttributes() {
        if (userAttributes == null) {
            return new JSONObject();
        }
        return userAttributes;
    }

    private static final Map<String, String> appUserKeys;

    private Boolean flushedUser = false;
    private Boolean flushedEvents = false;
    private final static int MAX_CONFIG_STRING_BYTES = 20000;

    static {
        Map<String, String> keyMap = new HashMap<>();
        keyMap.put(USER_ID, "String");
        keyMap.put(EMAIL, "String");
        keyMap.put(NAME, "String");
        keyMap.put(GENDER, "String");
        keyMap.put(AGE, "Number");
        keyMap.put(FIRST_NAME, "String");
        keyMap.put(LAST_NAME, "String");
        keyMap.put(AVATAR_URL, "String");
        appUserKeys = Collections.unmodifiableMap(keyMap);
    }

    TLAppUser() {
        userID = getUserId();
    }

    /**
     * Updates the users attributes based on the attributes passed in
     *
     * @param attributes The new updated user attributes
     * @param preLoaded  Set to true if you would like to update an attribute and then call start.
     *                   Ex. If I update a user attribute like cart to 1, and my experiment targets
     *                   all users with an attribute cart with value 1, this user will be included in the experiment.
     *                   <p>
     *                   Set to false if you would like to update an attribute after startTaplytics is called.
     *                   Ex. If I update a user attribute like cart to 1, and my experiment targets
     *                   all users with an attribute cart with value 1, this user will NOT be included in the experiment.
     * @param listener   Listens for when set attributes is finished loading properties
     */
    public void updateAppUserAttributes(final JSONObject attributes,
                                        final boolean preLoaded,
                                        final TaplyticsSetUserAttributesListener listener) {

        try {
            if (!TLManager.getInstance().getIsActive() ||
                    !TLManager.getInstance().getTrackingEnabled() ||
                    attributes == null) {
                return;
            }

            if (this.userAttributes == null) {
                this.userAttributes = new JSONObject();
            }

            String oldId = getUserId();
            if (oldId == null) {
                oldId = "";
            }
            final String oldEmail = this.userAttributes.optString(EMAIL, "");

            final Iterator<?> keys = attributes.keys();
            while (keys.hasNext()) {
                final String key = (String) keys.next();

                if (appUserKeys.containsKey(key)) {
                    addAppUserValueToUserAttributes(key, attributes.get(key), preLoaded);
                } else if (key.equals(CUSTOM_DATA)) {
                    mergeAndAddCustomDataToUserAttributes(key, attributes);
                } else {
                    addDirectlyToCustomData(key, attributes.get(key));
                }
            }

            TLLog.debug("Update App User Attributes: " + this.userAttributes.toString(), true);

            final String newUserId = this.userAttributes.optString(USER_ID, "");
            final String newEmail = this.userAttributes.optString(EMAIL, "");

            if (preLoaded || (oldEmail.equals(newEmail) && oldId.equals(newUserId))) {
                // Create a flush listener if a listener has been passed in
                TLFlushListener flushListener = null;
                if (listener != null) {
                    flushListener = new TLFlushListener() {
                        @Override
                        public void flushCompleted(boolean success) {
                            listener.finishedSettingUserAttributes();
                        }

                        @Override
                        public void flushFailed() {
                            listener.finishedSettingUserAttributes();
                        }
                    };
                }

                queueUserAppAttributesUpdate(flushListener);
                return;
            }

            final boolean newUser = !newUserId.equals("") && !oldId.equals(newUserId);
            if (TLManager.getInstance().isFastMode() && newUser) {
                handleFastModeNewUser(listener);
            } else {
                // Once we get to this point, we know that we have a new user. Once we flush the
                // details to the backend, we invoke this flush listener
                TLFlushListener flushListener = new TLFlushListener() {
                    @Override
                    public void flushCompleted(boolean success) {
                        if (listener != null) {
                            listener.finishedSettingUserAttributes();
                        }

                        // Notify the client of the start of a new session
                        TLManager.getInstance().notifyNewSessionStarted();
                    }

                    @Override
                    public void flushFailed() {
                        if (listener != null) {
                            listener.finishedSettingUserAttributes();
                        }

                        // Notify client of error when trying to start new session
                        TLManager.getInstance().notifyErrorStartingNewSession();
                    }
                };

                flushAppUserAttributes(flushListener);
            }

        } catch (Exception e) {
            TLLog.error("Issue when updating user attributes: " + e.getMessage());
        }
    }

    /**
     * Cleans the value object and adds it to {@link TLAppUser#userAttributes}. If the key passed in
     * is {@link com.taplytics.sdk.utils.constants.AppUserConstants#USER_ID}, then it is cached.
     *
     * @param key       Current attribute key
     * @param value     Current Attribute value
     * @param preLoaded
     * @throws Exception
     */
    private void addAppUserValueToUserAttributes(String key, Object value, boolean preLoaded) throws Exception {
        final Object cleanedValue = cleanAttributeValue(appUserKeys.get(key), value);
        if (cleanedValue == null) {
            return;
        }

        this.userAttributes.put(key, cleanedValue);

        if (!key.equals(USER_ID)) {
            return;
        }

        String cleanValString = (String) cleanedValue;
        if (preLoaded) {
            this.userID = cleanValString.isEmpty() ? generateAnonymousId() : cleanValString;
        }
        cacheUserId(preLoaded, cleanValString);
    }

    /**
     * Takes the custom data passed in by a user and stores that in {@link TLAppUser#userAttributes}.
     *
     * @param key        Current attribute key
     * @param attributes Updated attributes to apply to user
     * @throws JSONException
     */
    private void mergeAndAddCustomDataToUserAttributes(String key, JSONObject attributes) throws JSONException {
        if (!this.userAttributes.has(key)) {
            this.userAttributes.put(key, attributes.getJSONObject(key));
            return;
        }

        final JSONObject existObj = this.userAttributes.getJSONObject(key);
        final JSONObject newObj = attributes.getJSONObject(key);
        final Iterator<?> newKeys = newObj.keys();
        while (newKeys.hasNext()) {
            String newKey = (String) newKeys.next();
            existObj.put(newKey, newObj.get(newKey));
        }
        this.userAttributes.put(key, existObj);
    }

    /**
     * Used to add data to custom data that has a key not defined in app user keys and is not
     * the custom data key
     *
     * @param key   Current attribute key
     * @param value Current Attribute value
     * @throws JSONException
     */
    private void addDirectlyToCustomData(String key, Object value) throws JSONException {
        JSONObject customData;
        if (this.userAttributes.has(CUSTOM_DATA)) {
            customData = this.userAttributes.getJSONObject(CUSTOM_DATA);
        } else {
            customData = new JSONObject();
        }
        customData.put(key, value);
        if (customData.length() > 0) {
            this.userAttributes.put(CUSTOM_DATA, customData);
        }
    }

    /**
     * Gets the currently known user id.
     * If none available, anonymous user is assumed and a new ID is made if user bucketing is enabled
     *
     * @return
     */
    @Nullable
    public String getUserId() {
        boolean userBucketingEnabled = TLManager.getInstance().isUserBucketingEnabled();
        if (userID != null && !userID.isEmpty()) {
            return userID;
        }

        if (TLManager.getInstance().getApiKey() == null) {
            if (userAttributes != null && userAttributes.has(USER_ID)) {
                userID = userAttributes.optString(USER_ID);
            }
        } else {
            String id = SecurePrefs.getInstance().getAndDecryptString(USER_ID);
            if (!userBucketingEnabled && id != null && !id.startsWith(ANONYMOUS_PREFIX)) {
                return id;
            }

            if ((id == null || id.isEmpty()) && userBucketingEnabled) {
                userID = generateAnonymousId();
                cacheUserId(userID);
            } else if (id != null) {
                userID = id;
            }
        }
        return userID;
    }

    private String generateAnonymousId() {
        return (ANONYMOUS_PREFIX + UUID.randomUUID().toString());
    }

    void cacheUserId(String user_id) {
        userID = (user_id == null || user_id.isEmpty()) ? generateAnonymousId() : user_id;
        SecurePrefs.getInstance().put(USER_ID, userID);
    }

    void clearCachedUserId() {
        userID = null;
        SecurePrefs.getInstance().removeValue(USER_ID);
    }

    void cacheUserId(boolean preload, final String userId) {
        //If this is is set before taplytics starts, we can't save the value as we require the SDK key to do so.
        // This waits for when we know we have the SDK key to save.
        if (preload) {
            TLManager.getInstance().getTlPropertiesPromise().add(new PromiseListener() {
                @Override
                public void succeeded() {
                    cacheUserId(userId);
                    super.succeeded();
                }

                @Override
                public void failedOrCancelled() {
                    cacheUserId(userId);
                    super.failedOrCancelled();
                }
            });
        } else {
            cacheUserId(userId);
        }
    }

    public String getQueuedUserAttributesAsString() {
        if (userAttributes != null) {
            try {
                String attributesString = userAttributes.toString();
                if (attributesString.getBytes("UTF-8").length < MAX_CONFIG_STRING_BYTES) {
                    return attributesString;
                } else {
                    TLLog.warning("User attributes is longer than max byes, will not send in config call");
                }
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    public void getSessionInfo(final SessionInfoRetrievedListener listener) {
        try {
            PromiseListener attributesPromiseListener = new PromiseListener() {
                @Override
                public void succeeded() {
                    listener.sessionInfoRetrieved(sessionInfoFromProperties());
                    super.succeeded();
                }

                @Override
                public void failedOrCancelled() {
                    if (TLManager.getInstance().getTlProperties() != null) {
                        listener.onError(sessionInfoFromProperties());
                    } else {
                        listener.onError(new HashMap());
                    }
                    super.failedOrCancelled();
                }

            };
            TLManager.getInstance().getTlPropertiesPromise().add(attributesPromiseListener);

        } catch (Throwable e) {
            listener.onError(new HashMap());
        }
    }

    private HashMap sessionInfoFromProperties() {
        HashMap map = new HashMap();
        try {
            JSONObject appUser = TLManager.getInstance().getTlProperties().getAppUser();
            if (appUser != null && !appUser.optString("_id").equals("")) {
                map.put(APP_USER_ID, appUser.optString("_id"));
            }

            String sessionId = TLManager.getInstance().getTlProperties().getSessionID();
            if (sessionId != null) {
                map.put(SESSION_INFO_SESSION_ID_KEY, sessionId);
            }

        } catch (Throwable e) {
            //
        }
        return map;
    }


    void readUserAttributesFromDisk() {
        TLReaderWriter.getInstance().readTLAppUserAttributesFromDisk(new TLReaderWriter.TLUserAttributesReaderListener() {
            @Override
            public void callback(JSONObject attributes, Exception e) {
                try {
                    if (e != null)
                        TLLog.warning("Reading User Attributes from disk", e);
                    else if (attributes != null && attributes.optBoolean("resetAppUser", false))
                        resetAppUser(null);
                    else if (attributes != null) {
                        updateAppUserAttributes(attributes, false, null);
                        queueUserAppAttributesUpdate(null);
                    }
                } catch (Throwable e1) {
                    TLLog.error("Reading from disk");
                }
            }
        });
    }

    public void queueUserAppAttributesUpdate(@Nullable final TLFlushListener flushListener) {
        if (flushListener != null && queuedUserAttributes) {
            flushListener.flushCompleted(false);
        }
        TLReaderWriter.getInstance().writeTLAppUserAttributes(userAttributes);
        if (!queuedUserAttributes) {
            queuedUserAttributes = true;
            TLThreadManager.getInstance().scheduleOnBackgroundThread(new Runnable() {
                @Override
                public void run() {
                    queueUserAppAttributesUpdate(flushListener);
                }
            }, TLManager.getInstance().isLiveUpdate() ? 5 : Math.round((REPORTING_TIMEOUT + TLUtils.getRandomTime(10.0)) * 1000), TimeUnit.MILLISECONDS);

        }
    }

    private Object cleanAttributeValue(String type, Object value) throws Exception {
        if (type == null || value == null)
            throw new Exception("No type or value to clean attribute value");

        if (type.equals("String") && value instanceof String)
            return value;
        else if (type.equals("Number") && value instanceof Number)
            return value;
        else if (type.equals("JSONObject") && value instanceof JSONObject)
            return value;
        else
            return null;
    }

    /**
     * Sends the results to the client config
     *
     * @param listener
     */
    void handleFastModeNewUser(@Nullable final TaplyticsSetUserAttributesListener listener) {
        TLManager.getInstance().getTlProperties().setSessionID(null);
        Promise<Object> promise = new Promise<>();
        if (listener != null) {
            promise.add(new PromiseListener() {
                @Override
                public void succeeded() {
                    super.succeeded();
                    listener.finishedSettingUserAttributes();
                }

                @Override
                public void failed() {
                    super.failed();
                    listener.finishedSettingUserAttributes();
                }
            });
        }

        TLManager.getInstance().getPropertiesFromServer(null, promise);
    }

    /**
     * Sends the app user attributes to the backend
     *
     * @param flushListener
     */
    public void flushAppUserAttributes(@Nullable final TLFlushListener flushListener) {
        if (TLManager.getInstance().isFastMode()) {
            if (flushListener != null) {
                flushListener.flushCompleted(true);
            }
            return;
        }

        final TLManager tlManager = TLManager.getInstance();
        final TLProperties properties = tlManager.getTlProperties();

        if (properties == null || properties.getSessionID() == null) {
            queueUserAppAttributesUpdate(flushListener);
            return;
        }

        if (!tlManager.getTrackingEnabled() || userAttributes == null) {
            if (flushListener != null) {
                flushListener.flushCompleted(false);
            }
            return;
        }

        try {
            final JSONObject attributesCopy = new JSONObject(userAttributes.toString());
            try {
                JSONObject params = new JSONObject();
                // If we dont have user attributes to act on, we're done
                if (userAttributes.length() <= 0) {
                    return;
                }

                if (tlManager.getApiKey() != null) {
                    params.put(API_KEY, tlManager.getApiKey());
                }

                if (properties.getProject() != null) {
                    params.put(PROJECT_ID, properties.getProject().getString("_id"));
                }

                //Weird "k" value
                params.put(SPECIAL_KEY, SPECIAL_KEY_VALUE);

                //"au"
                params.put(USER_ATTRIBUTES, userAttributes);

                final JSONObject appUser = properties.getAppUser();
                if (appUser != null && appUser.has("_id")) {
                    params.put(APP_USER_ID, appUser.getString("_id"));
                }

                if (properties.getSessionID() != null) {
                    params.put(SESSION_ID, properties.getSessionID());
                }

                if (params.equals(JSONObject.NULL) || params.length() == 0) {
                    userAttributes = null;
                    TLReaderWriter.getInstance().deleteTLAppUserAttributesFromDisk();
                    return;
                }

                if (params.optJSONObject(CUSTOM_DATA) != null &&
                        params.optJSONObject(CUSTOM_DATA)
                                .toString()
                                .getBytes("UTF-8")
                                .length > MAX_CONFIG_STRING_BYTES) {
                    params.remove(CUSTOM_DATA);
                    TLLog.warning("Custom Data is too large. Removing from update.");
                }

                tlManager.getTlNetworking().postAppUserAttributes(params, new TLNetworking.TLNetworkResponseListener() {
                    @Override
                    public void onResponse(final JSONObject response) {
                        TLLog.debug("Flushed App User Attributes!");
                        if (response == null || response.length() == 0) {
                            TLLog.debug("No response from posting app user attributes");
                            return;
                        }

                        TLProperties tlProperties = TLManager.getInstance().getTlProperties();

                        if (tlProperties == null) {
                            // haven't received properties yet, do nothing
                            return;
                        }

                        //if the app user has changed
                        if (tlProperties.getAppUser() != null &&
                                tlProperties.getAppUser()
                                        .optString("_id")
                                        .equals(response.optString("_id"))) {
                            return;
                        }

                        tlProperties.setSessionID(null);
                        tlProperties.setAppUser(response);

                        Promise<Object> promise = new Promise<>();
                        promise.add(new PromiseListener() {
                            @Override
                            public void succeeded() {
                                super.succeeded();
                                if (flushListener != null) {
                                    flushListener.flushCompleted(true);

                                }
                                queuedUserAttributes = false;
                            }

                            @Override
                            public void failed() {
                                super.failed();
                                if (flushListener != null) {
                                    flushListener.flushCompleted(false);
                                }
                                queuedUserAttributes = false;
                            }
                        });

                        TLManager.getInstance().getPropertiesFromServer(null, promise);
                    }

                    @Override
                    public void onError(Throwable error) {
                        TLLog.error("Flushing App User Attributes", error);
                        userAttributes = attributesCopy;
                        queuedUserAttributes = false;
                        if (flushListener != null) {
                            flushListener.flushFailed();
                        }
                    }
                });

                userAttributes = null;
                TLReaderWriter.getInstance().deleteTLAppUserAttributesFromDisk();
            } catch (Exception e) {
                TLLog.error("Flushing App User Attributes", e);
                userAttributes = attributesCopy;
                queueUserAppAttributesUpdate(flushListener);
            }
        } catch (JSONException e) {
            TLLog.error("Copying app user attributes", e);
            queueUserAppAttributesUpdate(flushListener);
        }
    }

    /**
     * Resets the app user locally
     *
     * @param listener Callback to be invoked when finished
     */
    private void resetAppUserLocal(@Nullable final TaplyticsResetUserListener listener) {
        final TLManager tlManager = TLManager.getInstance();

        TLReaderWriter.getInstance().deleteTLAppUserAttributesFromDisk();
        TLReaderWriter.getInstance().deleteTLPropertiesFileFromDisk();
        userAttributes = new JSONObject();

        Promise<Object> propertiesPromise = new Promise<>();
        propertiesPromise.add(new PromiseListener<Object>() {
                                  @Override
                                  public void succeeded() {
                                      if (listener != null) {
                                          listener.finishedResettingUser();
                                      }

                                      tlManager.setResettingUser(false);
                                  }

                                  @Override
                                  public void failedOrCancelled() {
                                      //User just has no experiments now
                                      listener.finishedResettingUser();
                                      tlManager.setResettingUser(false);
                                  }
                              }
        );

        tlManager.resetTLProperties();
        clearCachedUserId();
        tlManager.setResettingUser(true);
        tlManager.getPropertiesFromServer(null, propertiesPromise);
    }

    /**
     * Sends the app user data to the backend to reset
     *
     * @param listener Callback to be invoked when finished
     */
    private void sendResetAppUser(@Nullable final TaplyticsResetUserListener listener) {
        //TODO: FIGURE OUT WHAT TO DO WITH CACHED USER ID IN FAILURE STATE
        TLLog.debug("Resetting app user");
        try {
            if (TLManager.getInstance().isFastMode()) {
                resetAppUserLocal(listener);
                return;
            }

            final TLManager tlManager = TLManager.getInstance();

            // If we dont have any properties to work with, we're done
            final TLProperties properties = tlManager.getTlProperties();
            if (properties == null) {
                return;
            }

            JSONObject params = new JSONObject();

            if (tlManager.getApiKey() != null) {
                params.put(API_KEY, tlManager.getApiKey());
            }

            if (properties.getSessionID() != null) {
                params.put(SESSION_ID, properties.getSessionID());
            }

            JSONObject appUser = properties.getAppUser();

            if (appUser != null && appUser.has("_id")) {
                params.put(APP_USER_ID, appUser.getString("_id"));
            }

            Map<String, String> deviceIDDic = TLManager.getInstance().getTLDeviceInfo().getDeviceUniqueID();
            if (deviceIDDic != null) {
                params.put(DEVICE_ID_DICTIONARY, new JSONObject(deviceIDDic));
            }

            tlManager.getTlNetworking().postResetAppUser(params, new TLNetworking.TLNetworkResponseListener() {
                @Override
                public void onResponse(JSONObject response) {
                    TLLog.debug("Reset App User!");
                    TLReaderWriter.getInstance().deleteTLAppUserAttributesFromDisk();
                    TLReaderWriter.getInstance().deleteTLPropertiesFileFromDisk();

                    Promise<Object> propertiesPromise = new Promise<>();
                    propertiesPromise.add(new PromiseListener<Object>() {
                        @Override
                        public void succeeded() {
                            if (listener != null) {
                                listener.finishedResettingUser();
                            }

                            tlManager.setResettingUser(false);

                            // Once we are done resetting the app user and got the properties,
                            // tell the client we started a new session
                            tlManager.notifyNewSessionStarted();
                        }
                    });

                    tlManager.resetTLProperties();
                    clearCachedUserId();
                    tlManager.setResettingUser(true);
                    tlManager.getPropertiesFromServer(null, propertiesPromise);
                }

                @Override
                public void onError(Throwable error) {
                    TLLog.error("Resetting App User", error);
                    TLLog.error("Error resetting App User", error, true, true);
                    try {
                        final JSONObject appUser = getUserAttributes();
                        appUser.put("resetAppUser", true);
                        TLReaderWriter.getInstance().writeTLAppUserAttributes(appUser);
                    } catch (Throwable e) {
                        TLLog.error("Error writing resetAppUser", e);
                    }

                    if (listener != null) {
                        listener.finishedResettingUser();
                    }

                    // Once we are done resetting the app user and failed to get the properties,
                    // tell the client there was an error starting a new session
                    tlManager.notifyErrorStartingNewSession();
                }
            });
        } catch (Exception e) {
            TLLog.error("Resetting App User Attributes", e);
            if (listener != null) {
                listener.finishedResettingUser();
            }
        }

    }

    public void resetAppUser(@Nullable final TaplyticsResetUserListener listener) {
        final TLManager tlManager = TLManager.getInstance();
        final TLProperties props = tlManager.getTlProperties();

        // If we dont have any props or session id, we need to fetch the props and then attempt
        // to reset the user
        if (props == null || props.getSessionID() == null) {
            fetchPropertiesAndResetUser(listener);
            return;
        }

        flushedUser = false;
        flushedEvents = false;

        final Promise<?> flushPromise = new Promise<>();
        flushPromise.add(new PromiseListener() {
            @Override
            public void succeeded() {
                sendResetAppUser(listener);
            }

            @Override
            public void failed() {
                sendResetAppUser(listener);
            }
        });

        flushAppUserAttributes(new TLFlushListener() {
            @Override
            public void flushCompleted(boolean success) {
                flushedUser = true;
                if (flushedEvents) {
                    flushPromise.finish();
                }
            }

            @Override
            public void flushFailed() {
                flushedUser = false;
                sendResetAppUser(listener);
            }
        });

        tlManager.getTlAnalytics().flushEventsQueue(new TLFlushListener() {
            @Override
            public void flushCompleted(boolean success) {
                flushedEvents = true;
                if (flushedUser) {
                    flushPromise.finish();
                }
            }

            @Override
            public void flushFailed() {
                flushedEvents = false;
                sendResetAppUser(listener);
            }
        });
    }

    /**
     * Fetches {@link TLProperties} from backend and resets app user accordingly.
     *
     * @param listener Callback to be invoked on completion
     */
    private void fetchPropertiesAndResetUser(@Nullable final TaplyticsResetUserListener listener) {
        final TLManager tlManager = TLManager.getInstance();

        final PromiseListener<Object> propertyListener = new PromiseListener<Object>() {
            @Override
            public void succeeded() {
                if (TLManager.getInstance().isFastMode()) {
                    resetAppUserLocal(listener);
                } else {
                    resetAppUser(listener);
                }
                super.succeeded();
            }

            @Override
            public void failedOrCancelled() {
                sendResetAppUser(null);
                if (listener != null) {
                    listener.finishedResettingUser();
                }
            }
        };

        if (!tlManager.getTlPropertiesPromise().isComplete()) {
            tlManager.getTlPropertiesPromise().add(propertyListener);
            return;
        }

        final Promise<Object> tlPropertiesPromise = new Promise<>();
        tlPropertiesPromise.add(propertyListener);
        tlManager.getPropertiesFromServer(null, tlPropertiesPromise);
    }

    public String getUserAttribute(String key) {
        if (userAttributes != null) {
            return userAttributes.optString(key, null);
        }
        return null;
    }

    public void changePushSubscription(boolean pushEnabled, final TaplyticsPushSubscriptionChangedListener listener) {
        try {
            if (userAttributes == null) {
                userAttributes = new JSONObject();
            }
            userAttributes.put(PUSH_ENABLED, pushEnabled);
            flushAppUserAttributes(listener == null ? null : new TLFlushListener() {
                @Override
                public void flushCompleted(boolean success) {
                    listener.success();
                }

                @Override
                public void flushFailed() {
                    listener.failure();
                }
            });
        } catch (JSONException e) {
            //do nothing
        }
    }

    /**
     * Increments the {@link #newSessionCounter}
     *
     * @param date The time the new session counter will be incremented
     * @return Returns true if the counter was successfully incremented, false otherwise
     */
    public boolean incrementNewSessionCounter(@NonNull Date date) {
        // If no record of lastNewSession, set it to default
        if (lastNewSession == null) {
            lastNewSession = date;
            newSessionCounter = 1;
            return true;
        }

        // Reset session counter if we've exceeded the max number of sessions
        if (date.getTime() - lastNewSession.getTime() > MAX_START_NEW_SESSION_TIME) {
            newSessionCounter = 1;
            lastNewSession = date;
            return true;
        }

        // Increment counter if we havent reached the number of max sessions
        if (newSessionCounter < MAX_NEW_SESSION) {
            newSessionCounter++;
            return true;
        }

        return false;
    }

}