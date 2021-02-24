/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.managers;

import android.os.Handler;
import android.os.Looper;

import com.taplytics.sdk.utils.ViewUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by emir on 2017-08-22.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Looper.class)
public class TLThreadManagerTest {

    final ScheduledExecutorService mScheduledExecutorMock = mock(ScheduledExecutorService.class);
    final Handler mMockHandler = mock(Handler.class);
    final TLThreadManager mThreadManager = new TLThreadManager(mScheduledExecutorMock, mMockHandler);

    @Test
    public void testRunOnUiThreadWithNullRunnableDoesNotRun() {
        mThreadManager.runOnUiThread(null);
        verify(mMockHandler, never()).post(any(Runnable.class));
    }

    @Test
    public void testRunOnUiThreadCallsPostonHandlerIfNotOnMainThread() {
        PowerMockito.mockStatic(Looper.class);
        final Looper mockLooper = mock(Looper.class);
        BDDMockito.when(Looper.getMainLooper()).thenReturn(mockLooper);
        BDDMockito.when(Looper.myLooper()).thenReturn(null);

        final Runnable mockRunnable = mock(Runnable.class);
        mThreadManager.runOnUiThread(mockRunnable);
        verify(mMockHandler).post(mockRunnable);
    }

    @Test
    public void testRunOnUiThreadDoesNotCallPostonHandlerWhenOnUiThread() {
        PowerMockito.mockStatic(Looper.class);
        final Looper mockLooper = mock(Looper.class);
        BDDMockito.when(Looper.getMainLooper()).thenReturn(mockLooper);
        BDDMockito.when(Looper.myLooper()).thenReturn(mockLooper);

        final Runnable mockRunnable = mock(Runnable.class);
        mThreadManager.runOnUiThread(mockRunnable);
        verify(mockRunnable, times(1)).run();
        verify(mMockHandler, never()).post(mockRunnable);
    }

    @Test
    public void testRunOnBackgroundThreadWithNullRunnableDoesNotRun() {
        mThreadManager.runOnBackgroundThread(null);
        verify(mScheduledExecutorMock, never()).execute(any(Runnable.class));
    }

    @Test
    public void testRunOnBackgroundThreadCallsExecuteOnScheduler() {
        final Runnable mockRunnable = mock(Runnable.class);
        mThreadManager.runOnBackgroundThread(mockRunnable);
        verify(mScheduledExecutorMock).execute(mockRunnable);
    }

    @Test
    public void testScheduleOnBackgroundThreadWithNullRunnableDoesNotRun() {
        final Runnable mockRunnable = mock(Runnable.class);
        mThreadManager.scheduleOnBackgroundThread(mockRunnable, 0, null);
        verify(mScheduledExecutorMock, never()).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }

    @Test
    public void testScheduleOnBackgroundThreadWithNullTimeUnitDoesNotRun() {
        mThreadManager.scheduleOnBackgroundThread(null, 0, TimeUnit.DAYS);
        verify(mScheduledExecutorMock, never()).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }

    @Test
    public void testScheduleOnBackgroundThreadCallsSchedule() {
        final Runnable mockRunnable = mock(Runnable.class);
        mThreadManager.scheduleOnBackgroundThread(mockRunnable, 0, TimeUnit.DAYS);
        verify(mScheduledExecutorMock).schedule(mockRunnable, 0, TimeUnit.DAYS);
    }

    @Test
    public void testScheduleOnUiThreadWithNullTimeUnitDoesNotRun() {
        final Runnable mockRunnable = mock(Runnable.class);
        mThreadManager.scheduleOnUiThread(mockRunnable, 0, null);
        verify(mMockHandler, never()).postDelayed(any(Runnable.class), anyLong());
    }

    @Test
    public void testScheduleOnUiThreadWithNullRunnableDoesNotRun() {
        mThreadManager.scheduleOnUiThread(null, 0, TimeUnit.DAYS);
        verify(mMockHandler, never()).postDelayed(any(Runnable.class), anyLong());
    }

    @Test
    public void testScheduleOnUiThreadThreadCallsSchedule() {
        final Runnable mockRunnable = mock(Runnable.class);
        mThreadManager.scheduleOnUiThread(mockRunnable, 0, TimeUnit.DAYS);
        verify(mMockHandler).postDelayed(any(Runnable.class), eq(0l));
    }

}