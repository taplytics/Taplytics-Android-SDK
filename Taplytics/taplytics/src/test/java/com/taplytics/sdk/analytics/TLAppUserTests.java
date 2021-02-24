/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.analytics;

import com.taplytics.sdk.datatypes.TLDeviceInfo;
import com.taplytics.sdk.datatypes.TLProperties;
import com.taplytics.sdk.listeners.TLFlushListener;
import com.taplytics.sdk.managers.TLManager;
import com.taplytics.sdk.network.TLNetworking;
import com.taplytics.sdk.utils.SecurePrefs;
import com.taplytics.sdk.utils.constants.AppUserConstants;
import com.taplytics.sdk.utils.promises.Promise;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.Map;

import static com.taplytics.sdk.utils.constants.AppUserConstants.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SecurePrefs.class, TLManager.class})
public class TLAppUserTests {

    private TLAppUser tlAppUser;
    private TLManager tlManager;
    private TLAnalyticsManager tlAnalyticsManager;
    private TLNetworking tlNetworking;
    private SecurePrefs mockSecurePrefs;
    private String userId = "user1";

    @Before
    public void setUp() {

        MockitoAnnotations.initMocks(this);

        mockStatic(SecurePrefs.class);
        mockStatic(TLManager.class);
        mockSecurePrefs = mock(SecurePrefs.class);
        tlManager = mock(TLManager.class);
        tlAnalyticsManager = mock(TLAnalyticsManager.class);
        tlNetworking = mock(TLNetworking.class);

        when(TLManager.getInstance()).thenReturn(tlManager);
        when(tlManager.getTlAnalytics()).thenReturn(tlAnalyticsManager);
        when(tlManager.getTlNetworking()).thenReturn(tlNetworking);

        when(tlManager.getIsActive()).thenReturn(true);
        when(tlManager.getTrackingEnabled()).thenReturn(true);
        when(tlManager.getApiKey()).thenReturn("API_KEY");

        when(SecurePrefs.getInstance()).thenReturn(mockSecurePrefs);
        when(mockSecurePrefs.getAndDecryptString(USER_ID)).thenReturn(null);

        tlAppUser = spy(new TLAppUser());
    }

    @Test
    public void testGetUserIdUserBucketingDisabledAndUserCached() {
        when(mockSecurePrefs.getAndDecryptString(USER_ID)).thenReturn(userId);
        when(tlManager.isUserBucketingEnabled()).thenReturn(false);
        String uid = tlAppUser.getUserId();
        verify(mockSecurePrefs, times(0)).put(AppUserConstants.USER_ID, userId);
        // invoked twice because creating TLAppUser calls getUserId once
        verify(mockSecurePrefs, times(2)).getAndDecryptString(USER_ID);
        assertThat(uid).isEqualTo("user1");
    }

    @Test
    public void testGetUserIdUserBucketingEnabledAndUserCached() {
        when(mockSecurePrefs.getAndDecryptString(USER_ID)).thenReturn(userId);
        when(tlManager.isUserBucketingEnabled()).thenReturn(true);
        String uid = tlAppUser.getUserId();
        verify(mockSecurePrefs, times(0)).put(AppUserConstants.USER_ID, userId);
        verify(mockSecurePrefs, times(2)).getAndDecryptString(USER_ID);
        assertThat(uid).isEqualTo("user1");
    }

    @Test
    public void testGetUserIdUserBucketingDisabledAndUserNotCached() {
        when(tlManager.isUserBucketingEnabled()).thenReturn(false);
        String uid = tlAppUser.getUserId();
        verify(mockSecurePrefs, times(0)).put(AppUserConstants.USER_ID, userId);
        verify(mockSecurePrefs, times(2)).getAndDecryptString(USER_ID);
        assertThat(uid).isEqualTo(null);
    }

    @Test
    public void testGetUserIdUserBucketingEnabledAndUserNotCached() {
        when(tlManager.isUserBucketingEnabled()).thenReturn(true);
        String uid = tlAppUser.getUserId();
        verify(mockSecurePrefs, times(0)).put(AppUserConstants.USER_ID, userId);
        verify(mockSecurePrefs, times(2)).getAndDecryptString(USER_ID);
        assertThat(uid.startsWith("TL_user"));
    }

    @Test
    public void testCacheUserId() {
        String userId = "user1";

        tlAppUser.cacheUserId(userId);
        verify(mockSecurePrefs, times(1)).put(USER_ID, userId);
    }

    @Test
    public void testCacheUserIdWithPreload() {
        Promise tlPropertiesPromise = new Promise();
        when(TLManager.getInstance().getTlPropertiesPromise()).thenReturn(tlPropertiesPromise);
        String userId = "user1";

        tlAppUser.cacheUserId(true, userId);
        tlPropertiesPromise.finish();
        verify(mockSecurePrefs, times(1)).put(USER_ID, userId);
    }

    @Test
    public void testCacheUserIdWithNoProperties() {
        Promise tlPropertiesPromise = new Promise();
        when(TLManager.getInstance().getTlPropertiesPromise()).thenReturn(tlPropertiesPromise);
        String userId = "user1";

        tlAppUser.cacheUserId(true, userId);
        tlPropertiesPromise.cancel();
        verify(mockSecurePrefs, times(1)).put(USER_ID, userId);
    }

    @Test
    public void testClearCacheUserId() {
        tlAppUser.clearCachedUserId();
        verify(mockSecurePrefs, times(1)).removeValue(USER_ID);
    }

    @Test
    public void testQueueUserIdPreloaded() throws Exception {
        Promise tlPropertiesPromise = new Promise();
        when(tlManager.getTlPropertiesPromise()).thenReturn(tlPropertiesPromise);
        doNothing().when(tlAppUser).queueUserAppAttributesUpdate(null);

        String userId = "user1";
        JSONObject attributes = new JSONObject();
        attributes.put("user_id", userId);

        tlAppUser.updateAppUserAttributes(attributes, true, null);
        tlPropertiesPromise.finish();

        verify(tlAppUser).queueUserAppAttributesUpdate(null);
        verify(mockSecurePrefs, times(1)).put(USER_ID, userId);
    }

    @Test
    public void testUserIdFlushed() throws Exception {
        Promise tlPropertiesPromise = new Promise();
        when(tlManager.getTlPropertiesPromise()).thenReturn(tlPropertiesPromise);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                TLFlushListener listener = (TLFlushListener) invocation.getArguments()[0];
                listener.flushCompleted(true);
                return null;
            }
        }).when(tlAppUser).flushAppUserAttributes(any(TLFlushListener.class));

        String userId = "user1";
        JSONObject attributes = new JSONObject();
        attributes.put("user_id", userId);

        tlAppUser.updateAppUserAttributes(attributes, false, null);
        tlPropertiesPromise.finish();

        verify(tlAppUser).getUserId();

        verify(tlAppUser).flushAppUserAttributes(any(TLFlushListener.class));
        verify(tlManager).notifyNewSessionStarted();
        verify(mockSecurePrefs, times(1)).put(USER_ID, userId);
    }

    @Test
    public void testHandleFastModeUser() throws Exception {
        Promise tlPropertiesPromise = new Promise();
        when(tlManager.getTlPropertiesPromise()).thenReturn(tlPropertiesPromise);
        when(tlManager.isFastMode()).thenReturn(true);
        doNothing().when(tlAppUser).handleFastModeNewUser(null);

        String userId = "user1";
        JSONObject attributes = new JSONObject();
        attributes.put("user_id", userId);

        tlAppUser.updateAppUserAttributes(attributes, false, null);
        tlPropertiesPromise.finish();

        verify(tlAppUser).getUserId();

        verify(tlAppUser).handleFastModeNewUser(null);
        verify(mockSecurePrefs, times(1)).put(USER_ID, userId);
    }

    @Test
    public void testResetAppUserStartsNewSession() {
        when(tlManager.isUserBucketingEnabled()).thenReturn(true);

        TLProperties props = mock(TLProperties.class);
        when(tlManager.getTlProperties()).thenReturn(props);
        when(props.getSessionID()).thenReturn("123");

        TLDeviceInfo deviceInfo = mock(TLDeviceInfo.class);
        when(tlManager.getTLDeviceInfo()).thenReturn(deviceInfo);
        when(deviceInfo.getDeviceUniqueID()).thenReturn(mock(HashMap.class));

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                TLFlushListener listener = (TLFlushListener) invocation.getArguments()[0];
                listener.flushCompleted(true);
                return null;
            }
        }).when(tlAppUser).flushAppUserAttributes(any(TLFlushListener.class));

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                TLFlushListener listener = (TLFlushListener) invocation.getArguments()[0];
                listener.flushCompleted(true);
                return null;
            }
        }).when(tlAnalyticsManager).flushEventsQueue(any(TLFlushListener.class));

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                TLNetworking.TLNetworkResponseListener listener =
                        (TLNetworking.TLNetworkResponseListener) invocation.getArguments()[1];
                listener.onResponse(any(JSONObject.class));
                return null;
            }
        }).when(tlNetworking)
                .postResetAppUser(any(JSONObject.class), any(TLNetworking.TLNetworkResponseListener.class));

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Promise<?> promise = (Promise<?>) invocation.getArguments()[1];
                promise.finish();
                return null;
            }
        }).when(tlManager).getPropertiesFromServer((Map) isNull(), any(Promise.class));

        tlAppUser.resetAppUser(null);

        verify(tlManager).notifyNewSessionStarted();
    }

}
