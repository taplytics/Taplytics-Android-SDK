/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.datatypes;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import org.bouncycastle.crypto.util.Pack;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.spy;

/**
 * Created by taplytics on 2017-10-12.
 */

public class TLDeviceInfoTests {

    private TLDeviceInfo deviceInfo;
    private PackageManager mPackageManager;
    private Context mContext;

    @Before
    public void setUp() {
        final WindowManager windowManager = new WindowManager() {
            @Override
            public Display getDefaultDisplay() {
                return null;
            }

            @Override
            public void removeViewImmediate(View view) {

            }

            @Override
            public void addView(View view, ViewGroup.LayoutParams params) {

            }

            @Override
            public void updateViewLayout(View view, ViewGroup.LayoutParams params) {

            }

            @Override
            public void removeView(View view) {

            }
        };

        MockitoAnnotations.initMocks(this);


        mPackageManager = mock(PackageManager.class);
        mContext = mock(Context.class);

        when(mPackageManager.hasSystemFeature(anyString())).thenReturn(false);

        when(mContext.getPackageName()).thenReturn("packageName");
        when(mContext.getSystemService(Context.WINDOW_SERVICE)).thenReturn(windowManager);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);

        deviceInfo = spy(new TLDeviceInfo(mContext));

        try {
            setFinalStatic(Build.VERSION.class.getField("SDK_INT"), 14);
        } catch (Exception e) {

        }
    }

    static void setFinalStatic(Field field, Object newValue) throws Exception {
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(null, newValue);
    }

    @Test
    public void testAndroidTV() {
        doReturn(Configuration.UI_MODE_TYPE_TELEVISION).when(deviceInfo).getCurrentModeType();
        HashMap<String, Object> dic = new HashMap<>();
        deviceInfo.setOSAndCurrentModeTypeForDeviceInfo(dic);
        assertThat(dic.get("cmt")).isEqualTo(Configuration.UI_MODE_TYPE_TELEVISION);
        assertThat(dic.get("os")).isEqualTo("Android TV");
    }

    @Test
    public void testFireTV() {
        when(mPackageManager.hasSystemFeature(anyString())).thenReturn(true);
        doReturn(Configuration.UI_MODE_TYPE_TELEVISION).when(deviceInfo).getCurrentModeType();
        HashMap<String, Object> dic = new HashMap<>();
        deviceInfo.setOSAndCurrentModeTypeForDeviceInfo(dic);
        assertThat(dic.get("cmt")).isEqualTo(Configuration.UI_MODE_TYPE_TELEVISION);
        assertThat(dic.get("os")).isEqualTo("Fire TV");
    }

    @Test
    public void testAndroid() {
        doReturn(Configuration.UI_MODE_TYPE_NORMAL).when(deviceInfo).getCurrentModeType();
        HashMap<String, Object> dic = new HashMap<>();
        deviceInfo.setOSAndCurrentModeTypeForDeviceInfo(dic);
        assertThat(dic.get("cmt")).isEqualTo(Configuration.UI_MODE_TYPE_NORMAL);
        assertThat(dic.get("os")).isEqualTo("Android");
    }
}
