package com.taplytics.sdk.analytics.external;

import com.google.android.gms.analytics.Tracker;
import com.taplytics.sdk.utils.TLLog;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Created by taplytics on 2017-11-02.
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest(TLLog.class)
public class TLExternalAnalyticsManagerTest {

    TLExternalAnalyticsManager externalAnalyticsManager = Mockito.spy(TLExternalAnalyticsManager.getInstance());

    @Test
    public void testLogToGAWithEvent() {
        Tracker mTracker = mock(Tracker.class);

        HashMap<String, String> map = new HashMap<>();
        map.put("exp", "var");
        externalAnalyticsManager.logToGA(map, mTracker);

        verify(mTracker).send(any(HashMap.class));
    }

    @Test
    public void testLogToGAWithNoEvent() {
        Tracker mTracker = mock(Tracker.class);

        HashMap<String, String> map = new HashMap<>();
        externalAnalyticsManager.logToGA(map, mTracker);

        verify(mTracker, never()).send(any(HashMap.class));
    }

}
