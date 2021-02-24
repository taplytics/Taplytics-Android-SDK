package com.taplytics.sdk.managers;

import android.content.Context;
import android.util.Log;

import com.taplytics.sdk.TaplyticsExperimentsLoadedListener;
import com.taplytics.sdk.analytics.TLAnalyticsManager;
import com.taplytics.sdk.datatypes.TLProperties;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by emir on 2017-08-29.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Log.class)
public class TLManagerTest {

    private TLManager mTlManager;
    private Set<TaplyticsExperimentsLoadedListener> mTaplyticsExperimentsLoadedListeners;

    @Before
    public void setup() {
        mTaplyticsExperimentsLoadedListeners = new HashSet<>();
        mTlManager = new TLManager(null, mTaplyticsExperimentsLoadedListeners);
    }

    @Test
    public void testRegisterExperimentsListenerIgnoresNullListener() {
        mTlManager.registerExperimentsLoadedListener(null);

        assertThat(mTaplyticsExperimentsLoadedListeners, Matchers.<TaplyticsExperimentsLoadedListener>empty());
    }

    @Test
    public void testRegisterExperimentsListenerAddsListenerToSet() {
        final Set<TaplyticsExperimentsLoadedListener> taplyticsExperimentsLoadedListeners = new HashSet<>();
        final TLManager tlManager = new TLManager(null, taplyticsExperimentsLoadedListeners);
        final TaplyticsExperimentsLoadedListener mockExperimentsLoadedListener = mock(TaplyticsExperimentsLoadedListener.class);
        tlManager.registerExperimentsLoadedListener(mockExperimentsLoadedListener);

        assertThat(taplyticsExperimentsLoadedListeners, contains(mockExperimentsLoadedListener));
    }

    @Test
    public void testRegisterExperimentsListenerCallsLoadedAndDoesNotAddsListenerToSetWhenAlreadyLoaded() {
        mTlManager.experimentsLoaded = true;
        final TaplyticsExperimentsLoadedListener mockExperimentsLoadedListener = mock(TaplyticsExperimentsLoadedListener.class);
        mTlManager.registerExperimentsLoadedListener(mockExperimentsLoadedListener);

        assertThat(mTaplyticsExperimentsLoadedListeners, Matchers.<TaplyticsExperimentsLoadedListener>empty());
        verify(mockExperimentsLoadedListener, times(1)).loaded();
    }

    @Test
    public void testExperimentsLoadedCallsLoadedForAllListenersAndSetsExperimentsLoadedToTrue() {
        final TaplyticsExperimentsLoadedListener mockExperimentsLoadedListener = mock(TaplyticsExperimentsLoadedListener.class);
        mTaplyticsExperimentsLoadedListeners.add(mockExperimentsLoadedListener);
        mTlManager.experimentsLoaded();

        assertThat(mTaplyticsExperimentsLoadedListeners, Matchers.<TaplyticsExperimentsLoadedListener>empty());
        verify(mockExperimentsLoadedListener, times(1)).loaded();
    }

    @Test
    public void testRegisterExperimentsListenerOnlyAddsOneInstanceOfListener() {
        final TaplyticsExperimentsLoadedListener mockExperimentsLoadedListener = mock(TaplyticsExperimentsLoadedListener.class);
        mTlManager.registerExperimentsLoadedListener(mockExperimentsLoadedListener);
        mTlManager.registerExperimentsLoadedListener(mockExperimentsLoadedListener);

        assertThat(mTaplyticsExperimentsLoadedListeners, contains(mockExperimentsLoadedListener));
        assertThat(mTaplyticsExperimentsLoadedListeners, hasSize(1));
    }

    public static void setTLManagerTestInstance(final TLProperties tlProperties) {
        TLManager.setInstance(new TLManager(tlProperties, null));
    }

    public static void setTLManagerTestInstanceWithAnalyticsManager(final TLProperties tlProperties, final TLAnalyticsManager tlAnalyticsManager) {
        TLManager.setInstance(new TLManager(tlProperties, tlAnalyticsManager));
    }

    @Test
    public void testTaplyticsReturnEarlyIfAlreadyStarted() {
        PowerMockito.mockStatic(Log.class);
        Context context = mock(Context.class);
        TLManager.getInstance().startTaplytics(context, "api_key", null, 4000, null);
        TLManager.getInstance().startTaplytics(context, "api_key", null, 4000, null);

        PowerMockito.verifyStatic(Log.class);
        Log.d("Taplytics", "Taplytics has already started! Taplytics only needs to be started once, preferably in your Application subclass.");
    }

}