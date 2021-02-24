/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.managers;

import com.taplytics.sdk.TaplyticsRunningFeatureFlagsListener;
import com.taplytics.sdk.datatypes.TLProperties;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TLFeatureFlagManagerTest {
    public static final String FLAG_ID = "_id";
    public static final String FLAG_KEY = "key";
    public static final String FLAG_NAME = "name";
    public static final String FLAG_ENABLED = "enabled";
    public static final String FLAG_STATUS = "status";

    private CountDownLatch lock = new CountDownLatch(1);

    TLProperties testProps;
    TLFeatureFlagManager flagManager;
    TLManager mockTLManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        testProps = null;
        flagManager = new TLFeatureFlagManager();
        mockTLManager = null;
    }

    public void constructTLPropsForFlag(String id, String key, String name, Boolean enabled, String status) {
        try {
            JSONObject flagValue = new JSONObject();
            flagValue.put(FLAG_ID, id);
            flagValue.put(FLAG_KEY, key);
            flagValue.put(FLAG_NAME, name);
            flagValue.put(FLAG_ENABLED, enabled);
            flagValue.put(FLAG_STATUS, status);
            JSONObject flags = new JSONObject();
            flags.put("test", flagValue);
            JSONObject config = new JSONObject();
            config.put("ff", flags);
            testProps = new TLProperties(config);
        } catch(Throwable e) {
            testProps = null;
        }
    }

    @Test
    public void testFFEnableOnEnabledFlag() {
        constructTLPropsForFlag("12223", "test", "Test", true, "active");
        mockTLManager = mock(TLManager.class);
        TLManager.setInstance(mockTLManager);
        when(TLManager.getInstance().getTlProperties()).thenReturn(testProps);
        assertNotNull(testProps);
        assertTrue(flagManager.featureFlagEnabled("test", false));
    }

    @Test
    public void testFFEnableOnDisabledFlag() {
        constructTLPropsForFlag("12223", "test", "Test", false, "draft");
        mockTLManager = mock(TLManager.class);
        TLManager.setInstance(mockTLManager);
        when(TLManager.getInstance().getTlProperties()).thenReturn(testProps);
        assertNotNull(testProps);
        assertFalse(flagManager.featureFlagEnabled("test", false));
    }


    @Test
    public void testFFEnableOnDisabledFlagWithDefaultTrue() {
        constructTLPropsForFlag("12223", "test", "Test", false, "draft");
        mockTLManager = mock(TLManager.class);
        TLManager.setInstance(mockTLManager);
        when(TLManager.getInstance().getTlProperties()).thenReturn(testProps);
        assertNotNull(testProps);
        assertFalse(flagManager.featureFlagEnabled("test", true));
    }

    @Test
    public void testFFEnableOnNullFlag() {
        testProps = null;
        mockTLManager = mock(TLManager.class);
        TLManager.setInstance(mockTLManager);
        when(TLManager.getInstance().getTlProperties()).thenReturn(testProps);
        assertNull(testProps);
        assertFalse(flagManager.featureFlagEnabled("test", false));
    }

    @Test
    public void testFFEnableOnNullFlagDefaultTrue() {
        testProps = null;
        mockTLManager = mock(TLManager.class);
        TLManager.setInstance(mockTLManager);
        when(TLManager.getInstance().getTlProperties()).thenReturn(testProps);
        assertNull(testProps);
        assertTrue(flagManager.featureFlagEnabled("test", true));
    }


    @Test
    public void testGetRunningFeatureFlagsBeforePropsLoaded() throws Throwable {
        constructTLPropsForFlag("12223", "test", "Test", true, "active");
        mockTLManager = mock(TLManager.class);
        TLManager.setInstance(mockTLManager);
        when(TLManager.getInstance().getTlProperties()).thenReturn(testProps);
        when(TLManager.getInstance().hasLoadedPropertiesFromServer()).thenReturn(false);
        when(TLManager.getInstance().getTrackingEnabled()).thenReturn(true);
        flagManager.getRunningFeatureFlags(new TaplyticsRunningFeatureFlagsListener() {
            @Override
            public void runningFeatureFlags(Map<String, String> featureFlags) {
                assertNotNull(testProps);
                assertEquals("test", featureFlags.get("Test"));
            }
        });
        flagManager.performPendingRunningFlagListeners();
        lock.await(2000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testGetRunningFeatureFlagsAfterPropsLoaded() throws Throwable {
        constructTLPropsForFlag("12223", "test", "Test", true, "active");
        mockTLManager = mock(TLManager.class);
        TLManager.setInstance(mockTLManager);
        when(TLManager.getInstance().getTlProperties()).thenReturn(testProps);
        when(TLManager.getInstance().hasLoadedPropertiesFromServer()).thenReturn(true);
        when(TLManager.getInstance().getTrackingEnabled()).thenReturn(true);
        flagManager.getRunningFeatureFlags(new TaplyticsRunningFeatureFlagsListener() {
            @Override
            public void runningFeatureFlags(Map<String, String> featureFlags) {
                assertNotNull(testProps);
                assertEquals("test", featureFlags.get("Test"));
            }
        });
        lock.await(2000, TimeUnit.MILLISECONDS);
    }
}
