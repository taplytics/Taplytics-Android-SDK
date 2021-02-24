/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.managers;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;

import com.taplytics.sdk.TaplyticsDelayLoadListener;
import com.taplytics.sdk.utils.Functionality;
import com.taplytics.sdk.utils.TLUtils;
import com.taplytics.sdk.utils.ViewUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

/**
 * Created by emir on 2017-08-23.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ViewUtils.class, TLUtils.class})
public class TLDelayLoadImageManagerTest {

    private static final Answer mInstantRunnableAnswer = new Answer() {
        @Override
        public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
            final Runnable runnable = invocationOnMock.getArgument(0);
            runnable.run();
            return null;
        }
    };

    private TLDelayLoadManager mMockDelayLoadManager;
    private TLDelayLoadImageManager mDelayLoadImageManager;
    private TLThreadManager mMockThreadManager;

    @Before
    public void setup() {
        mMockThreadManager = mock(TLThreadManager.class);
        doAnswer(mInstantRunnableAnswer).when(mMockThreadManager).runOnUiThread(any(Runnable.class));
        doAnswer(mInstantRunnableAnswer).when(mMockThreadManager).scheduleOnUiThread(any(Runnable.class), anyLong(), any(TimeUnit.class));
        doAnswer(mInstantRunnableAnswer).when(mMockThreadManager).runOnBackgroundThread(any(Runnable.class));
        doAnswer(mInstantRunnableAnswer).when(mMockThreadManager).scheduleOnBackgroundThread(any(Runnable.class), anyLong(), any(TimeUnit.class));
        TLThreadManager.setInstance(mMockThreadManager);


        mMockDelayLoadManager = mock(TLDelayLoadManager.class);
        TLDelayLoadManager.setInstance(mMockDelayLoadManager);
        mDelayLoadImageManager = new TLDelayLoadImageManager();
    }

    @Test
    public void testDelayLoadImageAddsListenerAndStartsDelayLoadManager() {
        final Activity mockActivity = mock(Activity.class);
        final Drawable mockDrawable = mock(Drawable.class);
        mDelayLoadImageManager.delayLoadImage(mockActivity, mockDrawable, 0);
        verify(mMockDelayLoadManager).addDelayLoadListener(mDelayLoadImageManager);
        verify(mMockDelayLoadManager).start(0, 0);
    }

    @Test
    public void testDelayLoadImageDoesNotRunWhenNoActvity() {
        final Drawable mockDrawable = mock(Drawable.class);
        mDelayLoadImageManager.delayLoadImage(null, mockDrawable, 0);
        verify(mMockDelayLoadManager, never()).addDelayLoadListener(any(TaplyticsDelayLoadListener.class));
        verify(mMockDelayLoadManager, never()).start(anyLong());
    }

    @Test
    public void testDelayLoadImageDoesNotRunWhenPermissionsDisabled() {
        final Drawable mockDrawable = mock(Drawable.class);

        PowerMockito.mockStatic(TLUtils.class);
        BDDMockito.when(TLUtils.isDisabled(Functionality.DELAYLOAD)).thenReturn(true);

        mDelayLoadImageManager.delayLoadImage(null, mockDrawable, 0);
        verify(mMockDelayLoadManager, never()).addDelayLoadListener(any(TaplyticsDelayLoadListener.class));
        verify(mMockDelayLoadManager, never()).start(anyLong());
    }

    @Test
    public void testDelayLoadImageDoesNotRunWhenNoDrawable() {
        final Activity mockActivity = mock(Activity.class);
        mDelayLoadImageManager.delayLoadImage(mockActivity, null, 0);
        verify(mMockDelayLoadManager, never()).addDelayLoadListener(any(TaplyticsDelayLoadListener.class));
        verify(mMockDelayLoadManager, never()).start(anyLong());
    }

    @Test
    public void testUpdateDelayLoad() {
        mDelayLoadImageManager.updateDelayLoad();
        verify(mMockDelayLoadManager, times(1)).addDelayLoadListener(mDelayLoadImageManager);
    }

    @Test
    public void testDelayLoadShowsDelayImageWhenImageDoesNotExistAndTLPropertiesHaveNotBeenLoaded() {
        final int delayViewId = ViewUtils.getDelayViewId();

        final ViewGroup mockViewGroup = mock(ViewGroup.class);
        when(mockViewGroup.findViewById(delayViewId)).thenReturn(null);

        final View mockView = mock(View.class);
        when(mockView.getRootView()).thenReturn(mockViewGroup);

        final Activity mockActivity = mock(Activity.class);
        when(mockActivity.findViewById(android.R.id.content)).thenReturn(mockView);

        final Drawable mockDrawable = mock(Drawable.class);

        final ImageView mockImageView = mock(ImageView.class);
        PowerMockito.mockStatic(ViewUtils.class);
        BDDMockito.when(ViewUtils.getDelayLoadView(mockDrawable)).thenReturn(mockImageView);

        mDelayLoadImageManager.delayLoadImage(mockActivity, mockDrawable, 1);
        verify(mMockThreadManager, times(1)).runOnUiThread(any(Runnable.class));
        verify(mockViewGroup, times(1)).addView(mockImageView);
    }

    @Test
    public void testDelayLoadDoesNotShowDelayImageWhenDelayViewAlreadyExists() {

        final ViewGroup mockViewGroup = mock(ViewGroup.class);
        final View mockExistingDelayView = mock(View.class);
        when(mockViewGroup.findViewById(anyInt())).thenReturn(mockExistingDelayView);

        final View mockView = mock(View.class);
        when(mockView.getRootView()).thenReturn(mockViewGroup);

        final Activity mockActivity = mock(Activity.class);
        when(mockActivity.findViewById(android.R.id.content)).thenReturn(mockView);


        final Drawable mockDrawable = mock(Drawable.class);

        final ImageView mockImageView = mock(ImageView.class);
        PowerMockito.mockStatic(ViewUtils.class);
        BDDMockito.when(ViewUtils.getDelayLoadView(mockDrawable)).thenReturn(mockImageView);

        mDelayLoadImageManager.delayLoadImage(mockActivity, mockDrawable, 1);
        verify(mockViewGroup, never()).addView(any(View.class));
    }

    @Test
    public void testDelayCompleteDoesNothingIfCurrentViewAlreadyHasAnimation() {
        final View mockExistingDelayView = mock(View.class);
        when(mockExistingDelayView.getAnimation()).thenReturn(mock(Animation.class));
        final ViewGroup mockViewGroup = mock(ViewGroup.class);
        when(mockViewGroup.findViewById(anyInt())).thenReturn(mockExistingDelayView);

        final View mockView = mock(View.class);
        when(mockView.getRootView()).thenReturn(mockViewGroup);

        final Activity mockActivity = mock(Activity.class);
        when(mockActivity.findViewById(android.R.id.content)).thenReturn(mockView);

        final ImageView mockImageView = mock(ImageView.class);

        final TLManager mockManager = mock(TLManager.class);
        TLManager.setInstance(mockManager);

        when(mockManager.getCurrentActivity()).thenReturn(mockActivity);
        when(mockManager.isActivityActive()).thenReturn(true);
        mDelayLoadImageManager.delayComplete();

        verify(mockImageView, never()).startAnimation(any(AlphaAnimation.class));
        verify(mockViewGroup, never()).addView(any(View.class));
    }

    @Test
    public void testDelayCompleteDoesNothingIfCurrentActivityIsNotActive() {
        final ViewGroup mockViewGroup = mock(ViewGroup.class);
        final View mockExistingDelayView = mock(View.class);
        when(mockViewGroup.findViewById(anyInt())).thenReturn(mockExistingDelayView);

        final View mockView = mock(View.class);
        when(mockView.getRootView()).thenReturn(mockViewGroup);

        final Activity mockActivity = mock(Activity.class);
        when(mockActivity.findViewById(android.R.id.content)).thenReturn(mockView);

        final ImageView mockImageView = mock(ImageView.class);

        final TLManager mockManager = mock(TLManager.class);
        TLManager.setInstance(mockManager);

        when(mockManager.getCurrentActivity()).thenReturn(mockActivity);
        when(mockManager.isActivityActive()).thenReturn(false);
        mDelayLoadImageManager.delayComplete();

        verify(mockImageView, never()).startAnimation(any(AlphaAnimation.class));
        verify(mockViewGroup, never()).addView(any(View.class));
    }
}