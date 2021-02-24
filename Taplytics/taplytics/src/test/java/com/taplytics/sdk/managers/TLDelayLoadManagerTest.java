/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.managers;

import com.taplytics.sdk.TaplyticsDelayLoadListener;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by emir on 2017-08-22.
 */
public class TLDelayLoadManagerTest {

    private static final Answer mInstantRunnableAnswer = new Answer() {
        @Override
        public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
            final Runnable runnable = invocationOnMock.getArgument(0);
            runnable.run();
            return null;
        }
    };

    private HashSet<TaplyticsDelayLoadListener> mDelayLoadListeners;
    private TLDelayLoadManager mTlDelayLoadManager;
    private TLThreadManager mInstantRunThreadManager;
    private TLThreadManager mInstantUiDelayBackgroundThreadManager;


    @Before
    public void setup() {
        mInstantRunThreadManager = mock(TLThreadManager.class);
        doAnswer(mInstantRunnableAnswer).when(mInstantRunThreadManager).runOnUiThread(any(Runnable.class));
        doAnswer(mInstantRunnableAnswer).when(mInstantRunThreadManager).scheduleOnUiThread(any(Runnable.class), anyLong(), any(TimeUnit.class));
        doAnswer(mInstantRunnableAnswer).when(mInstantRunThreadManager).runOnBackgroundThread(any(Runnable.class));
        doAnswer(mInstantRunnableAnswer).when(mInstantRunThreadManager).scheduleOnBackgroundThread(any(Runnable.class), anyLong(), any(TimeUnit.class));

        mInstantUiDelayBackgroundThreadManager = mock(TLThreadManager.class);
        doAnswer(mInstantRunnableAnswer).when(mInstantUiDelayBackgroundThreadManager).runOnUiThread(any(Runnable.class));
        doAnswer(mInstantRunnableAnswer).when(mInstantUiDelayBackgroundThreadManager).scheduleOnUiThread(any(Runnable.class), anyLong(), any(TimeUnit.class));
        doNothing().when(mInstantUiDelayBackgroundThreadManager).runOnBackgroundThread(any(Runnable.class));
        doNothing().when(mInstantUiDelayBackgroundThreadManager).scheduleOnBackgroundThread(any(Runnable.class), anyLong(), any(TimeUnit.class));

        TLThreadManager.setInstance(mInstantRunThreadManager);
        mDelayLoadListeners = new HashSet<>();
        mTlDelayLoadManager = new TLDelayLoadManager(mDelayLoadListeners);
    }

    @Test
    public void testAddDelayLoadListenerAddsToListeners() {
        final TaplyticsDelayLoadListener mockDelayLoadListener = mock(TaplyticsDelayLoadListener.class);
        mTlDelayLoadManager.addDelayLoadListener(mockDelayLoadListener);

        assertTrue(mDelayLoadListeners.contains(mockDelayLoadListener));
    }

    @Test
    public void testAddDelayLoadListenerDoesNotAddToList() {
        mTlDelayLoadManager.addDelayLoadListener(null);
        assertThat(mDelayLoadListeners, Matchers.empty());
    }

    @Test
    public void testAddDelayLoadListenerCallsStartDelayOnUiThread() {
        TLThreadManager.setInstance(mInstantUiDelayBackgroundThreadManager);

        final TaplyticsDelayLoadListener mockDelayLoadListener = mock(TaplyticsDelayLoadListener.class);
        mTlDelayLoadManager.delayLoadStarted = true;
        mTlDelayLoadManager.addDelayLoadListener(mockDelayLoadListener);

        verify(mInstantUiDelayBackgroundThreadManager, times(1)).runOnUiThread(any(Runnable.class));
        verify(mInstantUiDelayBackgroundThreadManager, never()).scheduleOnBackgroundThread(any(Runnable.class), anyLong(), any(TimeUnit.class));
        verify(mInstantUiDelayBackgroundThreadManager, never()).runOnBackgroundThread(any(Runnable.class));
    }

    @Test
    public void testmCompleteCompletesOnUiThread() {
        TLThreadManager.setInstance(mInstantUiDelayBackgroundThreadManager);

        final TaplyticsDelayLoadListener mockDelayLoadListener = mock(TaplyticsDelayLoadListener.class);
        mDelayLoadListeners.add(mockDelayLoadListener);
        mTlDelayLoadManager.minimumDelayLoadCompleted = true;
        mTlDelayLoadManager.delayLoadStarted = true;
        mTlDelayLoadManager.completedRunnable.run();

        verify(mInstantUiDelayBackgroundThreadManager, times(1)).runOnUiThread(any(Runnable.class));
        verify(mInstantUiDelayBackgroundThreadManager, never()).scheduleOnBackgroundThread(any(Runnable.class), anyLong(), any(TimeUnit.class));
        verify(mInstantUiDelayBackgroundThreadManager, never()).runOnBackgroundThread(any(Runnable.class));
    }

    @Test
    public void testmCompleteCompleteSetsCompletedToTrue() {
        mTlDelayLoadManager.delayLoadStarted = true;
        mTlDelayLoadManager.completedRunnable.run();

        assertThat(mTlDelayLoadManager.delayLoadCompleted, is(Boolean.TRUE));
    }

    @Test
    public void testAddDelayLoadListenerCallsStartDelay() {
        final TaplyticsDelayLoadListener mockDelayLoadListener = mock(TaplyticsDelayLoadListener.class);
        mTlDelayLoadManager.delayLoadStarted = true;
        mTlDelayLoadManager.addDelayLoadListener(mockDelayLoadListener);

        verify(mockDelayLoadListener, times(1)).startDelay();
        assertThat(mDelayLoadListeners, contains(mockDelayLoadListener));
    }

    @Test
    public void testAddDelayLoadListenerDoesNotCallDelayComplete() {
        final TaplyticsDelayLoadListener mockDelayLoadListener = mock(TaplyticsDelayLoadListener.class);
        mTlDelayLoadManager.delayLoadStarted = true;
        mTlDelayLoadManager.addDelayLoadListener(mockDelayLoadListener);

        verify(mockDelayLoadListener, never()).delayComplete();
        assertThat(mDelayLoadListeners, contains(mockDelayLoadListener));
    }

    @Test
    public void testAddDelayLoadListenerOnlyCallsStartWhenCompletedButMinimumNotHit() {
        final TaplyticsDelayLoadListener mockDelayLoadListener = mock(TaplyticsDelayLoadListener.class);
        mTlDelayLoadManager.delayLoadStarted = true;
        mTlDelayLoadManager.delayLoadCompleted = true;

        mTlDelayLoadManager.addDelayLoadListener(mockDelayLoadListener);

        verify(mockDelayLoadListener, times(1)).startDelay();
        verify(mockDelayLoadListener, never()).delayComplete();
        assertThat(mDelayLoadListeners, contains(mockDelayLoadListener));
    }

    @Test
    public void testAddDelayLoadListenerOnlyCallsStartWhenNotCompletedButMinimumHit() {
        final TaplyticsDelayLoadListener mockDelayLoadListener = mock(TaplyticsDelayLoadListener.class);
        mTlDelayLoadManager.delayLoadStarted = true;
        mTlDelayLoadManager.minimumDelayLoadCompleted = true;


        mTlDelayLoadManager.addDelayLoadListener(mockDelayLoadListener);

        verify(mockDelayLoadListener, times(1)).startDelay();
        verify(mockDelayLoadListener, never()).delayComplete();
        assertThat(mDelayLoadListeners, contains(mockDelayLoadListener));
    }

    @Test
    public void testAddDelayLoadListenerCallsFullFlow() {
        final TaplyticsDelayLoadListener mockDelayLoadListener = mock(TaplyticsDelayLoadListener.class);
        mTlDelayLoadManager.delayLoadStarted = true;
        mTlDelayLoadManager.minimumDelayLoadCompleted = true;
        mTlDelayLoadManager.delayLoadCompleted = true;
        mTlDelayLoadManager.addDelayLoadListener(mockDelayLoadListener);

        verify(mockDelayLoadListener, times(1)).startDelay();
        verify(mockDelayLoadListener, times(1)).delayComplete();
        assertThat(mDelayLoadListeners, Matchers.<TaplyticsDelayLoadListener>empty());
    }

    @Test
    public void testAddDelayLoadListenerReturnsWhenNullListener() {
        mTlDelayLoadManager.addDelayLoadListener(null);
        assertThat(mDelayLoadListeners, Matchers.<TaplyticsDelayLoadListener>empty());
    }

    @Test
    public void testStartNoDelaySendsFullFlowMessages() {
        final TaplyticsDelayLoadListener mockDelayLoadListener = mock(TaplyticsDelayLoadListener.class);
        mTlDelayLoadManager.addDelayLoadListener(mockDelayLoadListener);
        mTlDelayLoadManager.start(0);
        verify(mockDelayLoadListener, times(1)).startDelay();
        verify(mockDelayLoadListener, times(1)).delayComplete();
        assertThat(mDelayLoadListeners, Matchers.<TaplyticsDelayLoadListener>empty());
    }

    @Test
    public void testStartNegativeDelaySendsFullFlowMessages() {
        final TaplyticsDelayLoadListener mockDelayLoadListener = mock(TaplyticsDelayLoadListener.class);
        mTlDelayLoadManager.addDelayLoadListener(mockDelayLoadListener);
        mTlDelayLoadManager.start(-1);
        verify(mockDelayLoadListener, times(1)).startDelay();
        verify(mockDelayLoadListener, times(1)).delayComplete();
        assertThat(mDelayLoadListeners, Matchers.<TaplyticsDelayLoadListener>empty());
    }

    @Test
    public void testStartDelaySendsStartMessage() {
        TLThreadManager.setInstance(mInstantUiDelayBackgroundThreadManager);

        final TaplyticsDelayLoadListener mockDelayLoadListener = mock(TaplyticsDelayLoadListener.class);
        mTlDelayLoadManager.addDelayLoadListener(mockDelayLoadListener);
        mTlDelayLoadManager.start(1);
        verify(mockDelayLoadListener, times(1)).startDelay();
        verify(mockDelayLoadListener, never()).delayComplete();
        verify(mInstantUiDelayBackgroundThreadManager).scheduleOnBackgroundThread(mTlDelayLoadManager.completedRunnable, 1l, TimeUnit.MILLISECONDS);
        assertThat(mDelayLoadListeners, contains(mockDelayLoadListener));
        assertThat(mTlDelayLoadManager.delayLoadStarted, Matchers.is(Boolean.TRUE));
        assertThat(mTlDelayLoadManager.delayLoadCompleted, Matchers.is(Boolean.FALSE));
    }

    @Test
    public void testStartDelaySendsNothingWhenStartedButNotComplete() {
        final TaplyticsDelayLoadListener mockDelayLoadListener = mock(TaplyticsDelayLoadListener.class);
        mDelayLoadListeners.add(mockDelayLoadListener);
        mTlDelayLoadManager.delayLoadStarted = true;
        mTlDelayLoadManager.start(1);
        verify(mockDelayLoadListener, never()).startDelay();
        verify(mockDelayLoadListener, never()).delayComplete();
        assertThat(mDelayLoadListeners, contains(mockDelayLoadListener));
        assertThat(mTlDelayLoadManager.delayLoadCompleted, Matchers.is(Boolean.FALSE));
    }

    @Test
    public void testStartDelaySendsSendsFullFlowComplete() {
        final TaplyticsDelayLoadListener mockDelayLoadListener = mock(TaplyticsDelayLoadListener.class);
        mDelayLoadListeners.add(mockDelayLoadListener);
        mTlDelayLoadManager.delayLoadStarted = true;
        mTlDelayLoadManager.delayLoadCompleted = true;
        mTlDelayLoadManager.start(1);
        verify(mockDelayLoadListener, times(1)).startDelay();
        verify(mockDelayLoadListener, times(1)).delayComplete();
        assertThat(mDelayLoadListeners, Matchers.<TaplyticsDelayLoadListener>empty());
    }

    @Test
    public void testAddDelayLoadListenerOnlyAddsOneOfEachListener() {
        final TaplyticsDelayLoadListener mockDelayLoadListener = mock(TaplyticsDelayLoadListener.class);
        mDelayLoadListeners.add(mockDelayLoadListener);
        mDelayLoadListeners.add(mockDelayLoadListener);

        assertThat(mDelayLoadListeners, hasSize(1));
        assertThat(mDelayLoadListeners, contains(mockDelayLoadListener));
    }

    @Test
    public void testEachCallBackGetsOnlyOneOfEachNoMatterTheOrderOfOperationsAndQueueIsEmptyAtEndWithDelay() {
        TLThreadManager.setInstance(mInstantUiDelayBackgroundThreadManager);

        final TaplyticsDelayLoadListener mockDelayLoadListener1 = mock(TaplyticsDelayLoadListener.class);
        mTlDelayLoadManager.addDelayLoadListener(mockDelayLoadListener1);

        mTlDelayLoadManager.start(1);

        final TaplyticsDelayLoadListener mockDelayLoadListener2 = mock(TaplyticsDelayLoadListener.class);
        mTlDelayLoadManager.addDelayLoadListener(mockDelayLoadListener2);

        mTlDelayLoadManager.start(1);

        final TaplyticsDelayLoadListener mockDelayLoadListener3 = mock(TaplyticsDelayLoadListener.class);
        mTlDelayLoadManager.addDelayLoadListener(mockDelayLoadListener3);

        mTlDelayLoadManager.completedRunnable.run();

        final TaplyticsDelayLoadListener mockDelayLoadListener4 = mock(TaplyticsDelayLoadListener.class);
        mTlDelayLoadManager.addDelayLoadListener(mockDelayLoadListener4);

        mTlDelayLoadManager.start(1);

        final TaplyticsDelayLoadListener mockDelayLoadListener5 = mock(TaplyticsDelayLoadListener.class);
        mTlDelayLoadManager.addDelayLoadListener(mockDelayLoadListener5);


        mTlDelayLoadManager.minimumCompletedRunnable.run();

        final TaplyticsDelayLoadListener mockDelayLoadListener6 = mock(TaplyticsDelayLoadListener.class);
        mTlDelayLoadManager.addDelayLoadListener(mockDelayLoadListener6);

        mTlDelayLoadManager.start(1);

        final TaplyticsDelayLoadListener mockDelayLoadListener7 = mock(TaplyticsDelayLoadListener.class);
        mTlDelayLoadManager.addDelayLoadListener(mockDelayLoadListener7);

        verify(mockDelayLoadListener1, times(1)).startDelay();
        verify(mockDelayLoadListener2, times(1)).startDelay();
        verify(mockDelayLoadListener3, times(1)).startDelay();
        verify(mockDelayLoadListener4, times(1)).startDelay();
        verify(mockDelayLoadListener5, times(1)).startDelay();
        verify(mockDelayLoadListener6, times(1)).startDelay();
        verify(mockDelayLoadListener7, times(1)).startDelay();

        verify(mockDelayLoadListener1, times(1)).delayComplete();
        verify(mockDelayLoadListener2, times(1)).delayComplete();
        verify(mockDelayLoadListener3, times(1)).delayComplete();
        verify(mockDelayLoadListener4, times(1)).delayComplete();
        verify(mockDelayLoadListener5, times(1)).delayComplete();
        verify(mockDelayLoadListener6, times(1)).delayComplete();
        verify(mockDelayLoadListener7, times(1)).delayComplete();

        assertThat(mDelayLoadListeners, Matchers.<TaplyticsDelayLoadListener>empty());
    }

    @Test
    public void testStartWithPositiveDelayRegistersForTaplyticsLoaded() {
        final TLManager mockTlManager = mock(TLManager.class);
        TLManager.setInstance(mockTlManager);

        mTlDelayLoadManager.start(1);

        verify(mockTlManager).registerExperimentsLoadedListener(mTlDelayLoadManager);
    }

    @Test
    public void testStartWithDelayDoesNotRegisterForTaplyticsLoadedIfAlreadyCompleted() {
        final TLManager mockTlManager = mock(TLManager.class);
        TLManager.setInstance(mockTlManager);
        mTlDelayLoadManager.delayLoadCompleted = true;
        mTlDelayLoadManager.start(1);

        verify(mockTlManager, never()).registerExperimentsLoadedListener(mTlDelayLoadManager);
    }

    @Test
    public void testStartWithDelayDoesNotRegisterForTaplyticsLoadedIfAlreadyStarted() {
        final TLManager mockTlManager = mock(TLManager.class);
        TLManager.setInstance(mockTlManager);
        mTlDelayLoadManager.delayLoadStarted = true;
        mTlDelayLoadManager.start(1);

        verify(mockTlManager, never()).registerExperimentsLoadedListener(mTlDelayLoadManager);
    }

    @Test
    public void testStartWithNoDelayRegistersForTaplyticsLoaded() {
        final TLManager mockTlManager = mock(TLManager.class);
        TLManager.setInstance(mockTlManager);

        mTlDelayLoadManager.start(0);

        verify(mockTlManager, never()).registerExperimentsLoadedListener(mTlDelayLoadManager);
    }


    @Test
    public void testLoadedCallsOnlyCompleteWhenAlreadyStartedBackNonCompleteListeners() {
        final TLManager mockTlManager = mock(TLManager.class);
        TLManager.setInstance(mockTlManager);

        final TaplyticsDelayLoadListener mockDelayLoadListener = mock(TaplyticsDelayLoadListener.class);
        mTlDelayLoadManager.addDelayLoadListener(mockDelayLoadListener);
        mTlDelayLoadManager.delayLoadStarted = true;
        mTlDelayLoadManager.minimumDelayLoadCompleted = true;
        mTlDelayLoadManager.loaded();
        verify(mockDelayLoadListener, never()).startDelay();
        verify(mockDelayLoadListener, times(1)).delayComplete();

        assertThat(mTlDelayLoadManager.delayLoadCompleted, is(Boolean.TRUE));
        assertThat(mDelayLoadListeners, Matchers.<TaplyticsDelayLoadListener>empty());
    }

    @Test
    public void testLoadedCallsWhenCompletedutNotMinimumReachedDoNothing() {
        final TLManager mockTlManager = mock(TLManager.class);
        TLManager.setInstance(mockTlManager);

        final TaplyticsDelayLoadListener mockDelayLoadListener = mock(TaplyticsDelayLoadListener.class);
        mTlDelayLoadManager.addDelayLoadListener(mockDelayLoadListener);
        mTlDelayLoadManager.delayLoadStarted = true;
        mTlDelayLoadManager.delayLoadCompleted = true;
        mTlDelayLoadManager.minimumDelayLoadCompleted = false;
        mTlDelayLoadManager.loaded();
        verify(mockDelayLoadListener, never()).startDelay();
        verify(mockDelayLoadListener, never()).delayComplete();

        assertThat(mDelayLoadListeners, contains(mockDelayLoadListener));
        assertThat(mDelayLoadListeners, hasSize(1));
    }

    @Test
    public void testLoadedCallsNothingIfAndSetsStatusToCompeltedMinCompletedNotSet() {
        final TLManager mockTlManager = mock(TLManager.class);
        TLManager.setInstance(mockTlManager);

        final TaplyticsDelayLoadListener mockDelayLoadListener = mock(TaplyticsDelayLoadListener.class);
        mTlDelayLoadManager.addDelayLoadListener(mockDelayLoadListener);
        mTlDelayLoadManager.delayLoadStarted = true;
        mTlDelayLoadManager.loaded();
        verify(mockDelayLoadListener, never()).startDelay();
        verify(mockDelayLoadListener, never()).delayComplete();

        assertThat(mTlDelayLoadManager.delayLoadCompleted, is(Boolean.TRUE));
        assertThat(mDelayLoadListeners, contains(mockDelayLoadListener));
    }

    @Test
    public void testLoadedCallsBackFullFlowForNonCompleteListeners() {
        final TLManager mockTlManager = mock(TLManager.class);
        TLManager.setInstance(mockTlManager);

        final TaplyticsDelayLoadListener mockDelayLoadListener = mock(TaplyticsDelayLoadListener.class);
        mTlDelayLoadManager.addDelayLoadListener(mockDelayLoadListener);
        mTlDelayLoadManager.minimumDelayLoadCompleted = true;
        mTlDelayLoadManager.loaded();
        verify(mockDelayLoadListener, times(1)).startDelay();
        verify(mockDelayLoadListener, times(1)).delayComplete();
        assertThat(mTlDelayLoadManager.delayLoadStarted, is(Boolean.TRUE));
        assertThat(mTlDelayLoadManager.delayLoadCompleted, is(Boolean.TRUE));

        assertThat(mDelayLoadListeners, Matchers.<TaplyticsDelayLoadListener>empty());
    }


    @Test
    public void testmCompleteDoesNothingWhenLoadedAlreadyCalled() {
        final TLManager mockTlManager = mock(TLManager.class);
        TLManager.setInstance(mockTlManager);

        TLThreadManager.setInstance(mInstantUiDelayBackgroundThreadManager);

        final TaplyticsDelayLoadListener mockDelayLoadListener = mock(TaplyticsDelayLoadListener.class);
        mTlDelayLoadManager.addDelayLoadListener(mockDelayLoadListener);
        mTlDelayLoadManager.start(1);
        verify(mockDelayLoadListener, times(1)).startDelay();
        verify(mockDelayLoadListener, times(0)).delayComplete();
        mTlDelayLoadManager.loaded();
        verify(mockDelayLoadListener, times(1)).startDelay();
        verify(mockDelayLoadListener, times(1)).delayComplete();

        assertThat(mDelayLoadListeners, Matchers.<TaplyticsDelayLoadListener>empty());

        mTlDelayLoadManager.completedRunnable.run();

        verify(mockDelayLoadListener, times(1)).startDelay();
        verify(mockDelayLoadListener, times(1)).delayComplete();
    }

    @Test
    public void testStartSetsMinimumCompletedIfNoMinimumNeeded() {
        final TLThreadManager mockTlThreadManager = mock(TLThreadManager.class);
        TLThreadManager.setInstance(mockTlThreadManager);

        final TLManager mockTlManager = mock(TLManager.class);
        TLManager.setInstance(mockTlManager);
        mTlDelayLoadManager.start(2, 2);

        assertThat(mTlDelayLoadManager.minimumDelayLoadCompleted, is(Boolean.TRUE));
    }

    @Test
    public void testStartWithMinAndDelaySetsListenerWithMinimumCallback() {
        final TLThreadManager mockTlThreadManager = mock(TLThreadManager.class);
        TLThreadManager.setInstance(mockTlThreadManager);

        final TLManager mockTlManager = mock(TLManager.class);
        TLManager.setInstance(mockTlManager);
        mTlDelayLoadManager.start(2, 1);

        verify(mockTlThreadManager, times(1)).scheduleOnBackgroundThread(mTlDelayLoadManager.minimumCompletedRunnable, 1, TimeUnit.MILLISECONDS);

        assertThat(mTlDelayLoadManager.minimumDelayLoadCompleted, is(Boolean.FALSE));
    }

    @Test
    public void testStartWithMinEqualsMaxAndDelayDoesNotSetListenerWithMinimumCallback() {
        final TLThreadManager mockTlThreadManager = mock(TLThreadManager.class);
        TLThreadManager.setInstance(mockTlThreadManager);

        final TLManager mockTlManager = mock(TLManager.class);
        TLManager.setInstance(mockTlManager);
        mTlDelayLoadManager.start(2, 2);

        verify(mockTlThreadManager, never()).scheduleOnBackgroundThread(eq(mTlDelayLoadManager.minimumCompletedRunnable), anyLong(), any(TimeUnit.class));
        assertThat(mTlDelayLoadManager.minimumDelayLoadCompleted, is(Boolean.TRUE));
    }

    @Test
    public void testStartWithMinGreaterThanMaxAndDelayDoesNotSetListenerWithMinimumCallback() {
        final TLThreadManager mockTlThreadManager = mock(TLThreadManager.class);
        TLThreadManager.setInstance(mockTlThreadManager);

        final TLManager mockTlManager = mock(TLManager.class);
        TLManager.setInstance(mockTlManager);
        mTlDelayLoadManager.start(2, 3);

        verify(mockTlThreadManager, never()).scheduleOnBackgroundThread(eq(mTlDelayLoadManager.minimumCompletedRunnable), anyLong(), any(TimeUnit.class));
        assertThat(mTlDelayLoadManager.minimumDelayLoadCompleted, is(Boolean.TRUE));
    }

    @Test
    public void testStartWithNoDelayDoesNotSetListenerWithMinimumCallback() {
        final TLThreadManager mockTlThreadManager = mock(TLThreadManager.class);
        TLThreadManager.setInstance(mockTlThreadManager);

        final TLManager mockTlManager = mock(TLManager.class);
        TLManager.setInstance(mockTlManager);
        mTlDelayLoadManager.start(0, -1);

        verify(mockTlThreadManager, never()).scheduleOnBackgroundThread(eq(mTlDelayLoadManager.minimumCompletedRunnable), anyLong(), any(TimeUnit.class));
    }

    @Test
    public void testStartmMinimumCallbackSetsMinimumComplete() {
        final TLThreadManager mockTlThreadManager = mock(TLThreadManager.class);
        TLThreadManager.setInstance(mockTlThreadManager);

        final TLManager mockTlManager = mock(TLManager.class);
        TLManager.setInstance(mockTlManager);
        mTlDelayLoadManager.start(0, -1);

        verify(mockTlThreadManager, never()).scheduleOnBackgroundThread(eq(mTlDelayLoadManager.minimumCompletedRunnable), anyLong(), any(TimeUnit.class));
        assertThat(mTlDelayLoadManager.minimumDelayLoadCompleted, is(Boolean.TRUE));
    }


    @Test
    public void testStartNegativeMinCallbackSetsMinimumCompleteAndDoesNotSetCallack() {
        final TLThreadManager mockTlThreadManager = mock(TLThreadManager.class);
        TLThreadManager.setInstance(mockTlThreadManager);

        final TLManager mockTlManager = mock(TLManager.class);
        TLManager.setInstance(mockTlManager);
        mTlDelayLoadManager.start(1, -1);

        verify(mockTlThreadManager, never()).scheduleOnBackgroundThread(eq(mTlDelayLoadManager.minimumCompletedRunnable), anyLong(), any(TimeUnit.class));
        assertThat(mTlDelayLoadManager.minimumDelayLoadCompleted, is(Boolean.TRUE));
    }

    @Test
    public void testStartWith0MinCallbackSetsMinimumCompleteAndDoesNotSetCallack() {
        final TLThreadManager mockTlThreadManager = mock(TLThreadManager.class);
        TLThreadManager.setInstance(mockTlThreadManager);

        final TLManager mockTlManager = mock(TLManager.class);
        TLManager.setInstance(mockTlManager);
        mTlDelayLoadManager.start(1, 0);

        verify(mockTlThreadManager, never()).scheduleOnBackgroundThread(eq(mTlDelayLoadManager.minimumCompletedRunnable), anyLong(), any(TimeUnit.class));
        assertThat(mTlDelayLoadManager.minimumDelayLoadCompleted, is(Boolean.TRUE));
    }

    @Test
    public void testStartWithNoMinSetsMinimumComplete() {
        final TLThreadManager mockTlThreadManager = mock(TLThreadManager.class);
        TLThreadManager.setInstance(mockTlThreadManager);

        final TLManager mockTlManager = mock(TLManager.class);
        TLManager.setInstance(mockTlManager);
        mTlDelayLoadManager.start(1);

        verify(mockTlThreadManager, never()).scheduleOnBackgroundThread(eq(mTlDelayLoadManager.minimumCompletedRunnable), anyLong(), any(TimeUnit.class));
        assertThat(mTlDelayLoadManager.minimumDelayLoadCompleted, is(Boolean.TRUE));
    }

    @Test
    public void testStartWithNoDelaySetsMinComplete() {
        final TLThreadManager mockTlThreadManager = mock(TLThreadManager.class);
        TLThreadManager.setInstance(mockTlThreadManager);

        final TLManager mockTlManager = mock(TLManager.class);
        TLManager.setInstance(mockTlManager);
        mTlDelayLoadManager.start(0);

        verify(mockTlThreadManager, never()).scheduleOnBackgroundThread(eq(mTlDelayLoadManager.minimumCompletedRunnable), anyLong(), any(TimeUnit.class));
        assertThat(mTlDelayLoadManager.minimumDelayLoadCompleted, is(Boolean.TRUE));
    }


    @Test
    public void testmMinimumRunnaleSetsMinimumCompleted() {
        mTlDelayLoadManager.minimumCompletedRunnable.run();

        assertThat(mTlDelayLoadManager.minimumDelayLoadCompleted, is(Boolean.TRUE));
    }

    @Test
    public void testmMinimumRunnaleDoesNotCallRunCompleteWhenItHasNotBeenCompleted() {
        final TaplyticsDelayLoadListener mockDelayLoadListener = mock(TaplyticsDelayLoadListener.class);
        mDelayLoadListeners.add(mockDelayLoadListener);
        mTlDelayLoadManager.minimumCompletedRunnable.run();

        verify(mockDelayLoadListener, never()).startDelay();
        verify(mockDelayLoadListener, never()).delayComplete();
    }

    @Test
    public void testmMinimumRunnaleCallsRunCompleteIfAlreadyCompleted() {
        final TaplyticsDelayLoadListener mockDelayLoadListener = mock(TaplyticsDelayLoadListener.class);
        mDelayLoadListeners.add(mockDelayLoadListener);
        mTlDelayLoadManager.delayLoadCompleted = true;
        mTlDelayLoadManager.minimumCompletedRunnable.run();

        verify(mockDelayLoadListener, times(0)).startDelay();
        verify(mockDelayLoadListener, times(1)).delayComplete();
    }

}