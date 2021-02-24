/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk;

import android.app.Activity;

import com.taplytics.sdk.analytics.TLAnalyticsManager;
import com.taplytics.sdk.datatypes.TLProperties;
import com.taplytics.sdk.managers.TLManagerTest;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by taplytics on 2017-09-25.
 */
public class TLActivityLifecycleCallbacksManagerTest {

    TLAnalyticsManager tlAnalyticsManager;

    private TLActivityLifecycleCallbacksManager tlActivityLifecycleCallbacksManager;
    private Activity activity;

    @Before
    public void setUp() throws Exception {
        tlAnalyticsManager = mock(TLAnalyticsManager.class);

        TLManagerTest.setTLManagerTestInstanceWithAnalyticsManager(mock(TLProperties.class), tlAnalyticsManager);

        tlActivityLifecycleCallbacksManager = TLActivityLifecycleCallbacksManager.getInstance();
        activity = mock(Activity.class);
    }

    @Test
    public void testOnActivityResumeWasBackgrounded() throws Exception {
        // Test background
        tlActivityLifecycleCallbacksManager.activityPaused(activity);
        Thread.sleep(1000);
        tlActivityLifecycleCallbacksManager.activityResumed(activity);
        verify(tlAnalyticsManager, times(1)).trackTLEvent(TLAnalyticsManager.TLAnalyticsEventAppForeground);
        verify(tlAnalyticsManager, times(1)).trackTLEvent(TLAnalyticsManager.TLAnalyticsEventAppActive);
    }

    @Test
    public void testOnActivityResumeWasNotBackgrounded() throws Exception {
        // Test background
        tlActivityLifecycleCallbacksManager.activityPaused(activity);
        tlActivityLifecycleCallbacksManager.activityResumed(activity);
        verify(tlAnalyticsManager, times(0)).trackTLEvent(TLAnalyticsManager.TLAnalyticsEventAppForeground);
        verify(tlAnalyticsManager, times(0)).trackTLEvent(TLAnalyticsManager.TLAnalyticsEventAppActive);
    }

}