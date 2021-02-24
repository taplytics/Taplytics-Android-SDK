package com.taplytics.sdk.analytics;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.android.volley.ExecutorDelivery;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.ResponseDelivery;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.taplytics.sdk.datatypes.TLProperties;
import com.taplytics.sdk.listeners.TLFlushListener;
import com.taplytics.sdk.managers.TLManager;
import com.taplytics.sdk.network.TLNetworking;
import com.taplytics.sdk.utils.TLDatabaseHelper;
import com.taplytics.sdk.utils.TLUtils;
import com.taplytics.sdk.utils.promises.Promise;
import com.taplytics.sdk.utils.promises.PromiseListener;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static junit.framework.Assert.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.gt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.spy;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Looper.class, TLManager.class, TLUtils.class})
public class TLAnalyticsManagerTest {

    TLAnalyticsManager tlAnalyticsManager;
    TLManager tlManager;
    TLNetworking tlNetworkingMock;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(Looper.class);

        tlManager = TLManager.getInstance();
        tlManager.setupTLAnalytics();

        tlAnalyticsManager = TLManager.getInstance().getTlAnalytics();
        tlNetworkingMock = mock(TLNetworking.class);
    }

    @Test
    public void testGetSplitEventsBySessionIDMap() throws JSONException {
        TLDatabaseHelper mockDBHelper = mock(TLDatabaseHelper.class);
        tlAnalyticsManager.setDBHelperMock(mockDBHelper);

        ArrayList<JSONObject> events = new ArrayList<>();
        ArrayList<JSONObject> eventsWithSessionID1 = new ArrayList<>();
        ArrayList<JSONObject> eventsWithSessionID2 = new ArrayList<>();

        String sessionID1 = "session_id_1";
        String sessionID2 = "session_id_2";

        HashMap<String, ArrayList<JSONObject>> expected = new HashMap<>();
        expected.put(sessionID1, eventsWithSessionID1);
        expected.put(sessionID2, eventsWithSessionID2);

        for (int i = 0; i < 5; i++) {
            JSONObject event = createJSONObjectWithSessionIDAndEventName(sessionID1, "event" + i);
            events.add(event);
            eventsWithSessionID1.add(event);
        }

        for (int i = 5; i < 10; i++) {
            JSONObject event = createJSONObjectWithSessionIDAndEventName(sessionID2, "event" + i);
            events.add(event);
            eventsWithSessionID2.add(event);
        }

        // mock DB Helper
        when(mockDBHelper.getEvents(100)).thenReturn(events);
        HashMap<String, ArrayList<JSONObject>> splitEvents = tlAnalyticsManager.getSplitEventsBySessionIDMap();
        verify(mockDBHelper).getEvents(100);
        verify(mockDBHelper).deleteEvents(anyInt());

        assertThat(splitEvents).isNotEmpty();
        assertEquals(splitEvents, expected);

        assertThat(splitEvents).hasSize(2);
        assertThat(splitEvents).containsKeys("session_id_1", "session_id_2");

        assertThat(splitEvents.get("session_id_1")).hasSize(5);
        assertThat(splitEvents.get("session_id_2")).hasSize(5);
        assertThat(splitEvents.get("session_id_1")).isEqualTo(eventsWithSessionID1);
        assertThat(splitEvents.get("session_id_2")).isEqualTo(eventsWithSessionID2);
    }

    @Test
    public void testPostEventsForBatchIDCalledClientEventsOnResponse() throws JSONException {

        TLFlushListener tlFlushListenerMock = mock(TLFlushListener.class);
        tlManager.setTlNetworkingMock(tlNetworkingMock);

        ArrayList<JSONObject> events = new ArrayList<>();
        String sessionID1 = "session_id_1";

        for (int i = 0; i < 5; i++) {
            JSONObject event = createJSONObjectWithSessionIDAndEventName(sessionID1, "event" + i);
            events.add(event);
        }

        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) throws Throwable {
                ((TLNetworking.TLNetworkResponseListener) invocation.getArguments()[1]).onResponse(new JSONObject());
                return null;
            }
        }).when(tlNetworkingMock).postClientEvents(any(JSONObject.class), any(TLNetworking.TLNetworkResponseListener.class));

        tlAnalyticsManager.postEventsBatchForSessionID(events, sessionID1, tlFlushListenerMock);

        verify(tlNetworkingMock).postClientEvents(any(JSONObject.class), any(TLNetworking.TLNetworkResponseListener.class));
        verify(tlFlushListenerMock).flushCompleted(true);
    }

    @Test
    public void testPostEventsForBatchIDCalledClientEventsOnFailed() throws JSONException {
        TLNetworking tlNetworkingMock = mock(TLNetworking.class);
        TLFlushListener tlFlushListenerMock = mock(TLFlushListener.class);

        TLAnalyticsManager analyticsManagerSpy = spy(tlAnalyticsManager);
        tlManager.setTlNetworkingMock(tlNetworkingMock);

        ArrayList<JSONObject> events = new ArrayList<>();
        String sessionID1 = "session_id_1";

        for (int i = 0; i < 5; i++) {
            JSONObject event = createJSONObjectWithSessionIDAndEventName(sessionID1, "event" + i);
            events.add(event);
        }

        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) throws Throwable {
                ((TLNetworking.TLNetworkResponseListener) invocation.getArguments()[1]).onError(new Throwable());
                return null;
            }
        }).when(tlNetworkingMock).postClientEvents(any(JSONObject.class), any(TLNetworking.TLNetworkResponseListener.class));

        analyticsManagerSpy.postEventsBatchForSessionID(events, sessionID1, tlFlushListenerMock);

        verify(tlNetworkingMock).postClientEvents(any(JSONObject.class), any(TLNetworking.TLNetworkResponseListener.class));
        verify(tlFlushListenerMock).flushFailed();
        verify(analyticsManagerSpy).writeAllQueuedEventsToDB(events);
    }

    @Test
    public void testSetSessionIDAndProdOnEvent() throws JSONException {
        PowerMockito.mockStatic(TLManager.class);
        TLManager tlManagerSpy = spy(tlManager);
        BDDMockito.when(TLManager.getInstance()).thenReturn(tlManagerSpy);

        TLProperties tlPropertiesMock = mock(TLProperties.class);
        when(tlPropertiesMock.getSessionID()).thenReturn("mock_session_id");
        when(tlManagerSpy.getTlProperties()).thenReturn(tlPropertiesMock);
        when(tlManagerSpy.isLiveUpdate()).thenReturn(true);

        JSONObject dic = new JSONObject();
        tlAnalyticsManager.setSessionIDAndProdOnEvent(dic);
        assertThat(dic.get("sid")).isEqualTo("mock_session_id");
        assertThat(dic.get("prod")).isEqualTo(0);
    }

    @Test
    public void testWriteEventToDB() {
        TLDatabaseHelper mockDBHelper = mock(TLDatabaseHelper.class);
        ExecutorService dbExecutorMock = mock(ExecutorService.class);

        TLAnalyticsManager analyticsManagerSpy = spy(tlAnalyticsManager);
        analyticsManagerSpy.setDBExecutorMock(dbExecutorMock);
        analyticsManagerSpy.setDBHelperMock(mockDBHelper);

        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) throws Throwable {
                ((Runnable) invocation.getArguments()[0]).run();
                return null;
            }
        }).when(dbExecutorMock).submit(any(Runnable.class));

        analyticsManagerSpy.writeEventToDB(new JSONObject());

        verify(analyticsManagerSpy).setSessionIDAndProdOnEvent(any(JSONObject.class));
        verify(mockDBHelper).writeEvent(any(JSONObject.class));
        verify(dbExecutorMock).submit(any(Runnable.class));
        verify(analyticsManagerSpy).queueTLEventsUpdate((TLFlushListener) isNull());
    }

    @Test
    public void testWriteAllQueuedEventsToDB() throws JSONException {
        TLDatabaseHelper mockDBHelper = mock(TLDatabaseHelper.class);
        ExecutorService dbExecutorMock = mock(ExecutorService.class);

        ArrayList<JSONObject> events = new ArrayList<>();
        events.add(new JSONObject("{\"gn\": \"event1\"}"));
        events.add(new JSONObject("{\"gn\": \"event2\"}"));
        events.add(new JSONObject("{\"gn\": \"event3\"}"));

        TLAnalyticsManager analyticsManagerSpy = spy(tlAnalyticsManager);
        analyticsManagerSpy.setDBExecutorMock(dbExecutorMock);
        analyticsManagerSpy.setDBHelperMock(mockDBHelper);

        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) throws Throwable {
                ((Runnable) invocation.getArguments()[0]).run();
                return null;
            }
        }).when(dbExecutorMock).submit(any(Runnable.class));

        analyticsManagerSpy.writeAllQueuedEventsToDB(events);

        assertThat(events).isEmpty();
        verify(mockDBHelper, times(3)).writeEvent(any(JSONObject.class));
        verify(dbExecutorMock).submit(any(Runnable.class));
        verify(analyticsManagerSpy, times(3)).setSessionIDAndProdOnEvent(any(JSONObject.class));
        verify(analyticsManagerSpy).queueTLEventsUpdate((TLFlushListener) isNull());
    }

    @Test
    public void testQueueEventUntilPropertiesLoaded() throws JSONException {
        JSONObject obj = new JSONObject("{\"gn\": \"event1\"}");

        TLManager tlManagerSpy = spy(tlManager);
        TLAnalyticsManager tlAnalyticsManagerSpy = spy(tlAnalyticsManager);

        Promise promiseMock = mock(Promise.class);
        PowerMockito.mockStatic(TLManager.class);
        BDDMockito.when(TLManager.getInstance()).thenReturn(tlManagerSpy);
        when(tlManagerSpy.getTlPropertiesPromise()).thenReturn(promiseMock);

        tlAnalyticsManagerSpy.queueEventUntilPropertiesLoaded(obj);
        assertThat(tlAnalyticsManagerSpy.getEventsQueue().get(0)).isEqualTo(obj);
        assertThat(tlAnalyticsManagerSpy.getTlPropertiesPromiseListener()).isNotNull();
    }

    @Test
    public void testQueueEventUntilPropertiesLoadedPromiseExists() throws JSONException {
        JSONObject obj = new JSONObject("{\"gn\": \"event1\"}");

        TLManager tlManagerSpy = spy(tlManager);
        TLAnalyticsManager tlAnalyticsManagerSpy = spy(tlAnalyticsManager);

        Promise promiseMock = mock(Promise.class);
        PowerMockito.mockStatic(TLManager.class);
        BDDMockito.when(TLManager.getInstance()).thenReturn(tlManagerSpy);

        tlAnalyticsManagerSpy.queueEventUntilPropertiesLoaded(obj);
        assertThat(tlAnalyticsManagerSpy.getEventsQueue().get(0)).isEqualTo(obj);
        verify(promiseMock, times(0)).add(any(PromiseListener.class));
    }

    @Test
    public void testQueueEventUntilPropertiesLoadedCalledSucceeded() throws JSONException {
        JSONObject obj = new JSONObject("{\"gn\": \"event1\"}");

        TLManager tlManagerSpy = spy(tlManager);
        TLAnalyticsManager tlAnalyticsManagerSpy = spy(tlAnalyticsManager);

        Promise promiseMock = mock(Promise.class);
        PowerMockito.mockStatic(TLManager.class);
        BDDMockito.when(TLManager.getInstance()).thenReturn(tlManagerSpy);
        when(tlManagerSpy.getTlPropertiesPromise()).thenReturn(promiseMock);

        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) throws Throwable {
                ((PromiseListener) invocation.getArguments()[0]).succeeded();
                return null;
            }
        }).when(promiseMock).add(any(PromiseListener.class));

        tlAnalyticsManagerSpy.queueEventUntilPropertiesLoaded(obj);
        assertThat(tlAnalyticsManager.getTlPropertiesPromiseListener()).isNull();
        verify(tlAnalyticsManagerSpy).writeAllQueuedEventsToDB(any(ArrayList.class));
    }

    @Test
    public void testQueueEventUntilPropertiesLoadedCalledFailedOrCancelled() throws JSONException {
        JSONObject obj = new JSONObject("{\"gn\": \"event1\"}");

        TLManager tlManagerSpy = spy(tlManager);
        TLAnalyticsManager tlAnalyticsManagerSpy = spy(tlAnalyticsManager);

        Promise promiseMock = mock(Promise.class);
        PowerMockito.mockStatic(TLManager.class);
        BDDMockito.when(TLManager.getInstance()).thenReturn(tlManagerSpy);
        when(tlManagerSpy.getTlPropertiesPromise()).thenReturn(promiseMock);

        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) throws Throwable {
                ((PromiseListener) invocation.getArguments()[0]).failedOrCancelled();
                return null;
            }
        }).when(promiseMock).add(any(PromiseListener.class));

        tlAnalyticsManagerSpy.queueEventUntilPropertiesLoaded(obj);
        assertThat(tlAnalyticsManager.getTlPropertiesPromiseListener()).isNull();
        verify(tlAnalyticsManagerSpy).writeAllQueuedEventsToDB(any(ArrayList.class));
    }

    @Test
    public void testPingsInBackgroundPastLimit() {
        TLManager tlManagerSpy = spy(tlManager);
        TLAnalyticsManager tlAnalyticsManagerSpy = spy(tlAnalyticsManager);

        when(tlManagerSpy.getHasAppBackgrounded()).thenReturn(true);
        PowerMockito.mockStatic(TLManager.class);
        BDDMockito.when(TLManager.getInstance()).thenReturn(tlManagerSpy);


        Whitebox.setInternalState(tlAnalyticsManagerSpy, "clientPostFailureCount", 5);

        Handler flushEventsQueueHandler = mock(Handler.class);
        when(flushEventsQueueHandler.postDelayed(any(Runnable.class), anyLong())).thenReturn(true);
        Whitebox.setInternalState(tlAnalyticsManagerSpy, "flushEventsQueueHandler", flushEventsQueueHandler);

        tlAnalyticsManagerSpy.queueTLEventsUpdate(null);

        verify(flushEventsQueueHandler, never()).postDelayed(any(Runnable.class), anyLong());
    }

    @Test
    public void testPingNoPreviousFail() throws JSONException {
        //60 comes from REPORTING_INT in TLAnalyticsManager
        double BASE_DELAY_VALUE = 60;

        TLManager tlManagerSpy = spy(tlManager);
        TLAnalyticsManager tlAnalyticsManagerSpy = spy(tlAnalyticsManager);

        //Make the "random" delay predictable (return value is RANDOM_VALUE)
        PowerMockito.mockStatic(TLUtils.class);

        //Provide a mock database with 1 "event"
        TLDatabaseHelper mockDBHelper = mock(TLDatabaseHelper.class);
        when(mockDBHelper.getEventCount()).thenReturn(1);
        tlAnalyticsManagerSpy.setDBHelperMock(mockDBHelper);

        PowerMockito.mockStatic(TLManager.class);
        BDDMockito.when(TLManager.getInstance()).thenReturn(tlManagerSpy);

        //Probably not needed, but just to be safe
        Whitebox.setInternalState(tlAnalyticsManagerSpy, "clientPostFailureCount", 0);

        //Create our own spyable flushEventsQueue
        Handler flushEventsQueueHandler = mock(Handler.class);
        when(flushEventsQueueHandler.postDelayed(any(Runnable.class), anyLong())).thenReturn(true);
        Whitebox.setInternalState(tlAnalyticsManagerSpy, "flushEventsQueueHandler", flushEventsQueueHandler);

        tlAnalyticsManagerSpy.queueTLEventsUpdate(null);

        //Verify the correct delay was calculated
        verify(flushEventsQueueHandler).postDelayed(any(Runnable.class), eq(Math.round(BASE_DELAY_VALUE) * 1000));
    }

    @Test
    public void testPingLessThanMaxBackoffDelay() {
        //60 comes from REPORTING_INT in TLAnalyticsManager
        double BASE_DELAY_VALUE = 60;

        int NUMBER_OF_PINGS = 3;

        TLManager tlManagerSpy = spy(tlManager);
        TLAnalyticsManager tlAnalyticsManagerSpy = spy(tlAnalyticsManager);

        //Make the "random" delay predictable (return value is RANDOM_VALUE)
        PowerMockito.mockStatic(TLUtils.class);

        //Provide a mock database with 1 "event"
        TLDatabaseHelper mockDBHelper = mock(TLDatabaseHelper.class);
        when(mockDBHelper.getEventCount()).thenReturn(1);
        tlAnalyticsManagerSpy.setDBHelperMock(mockDBHelper);

        PowerMockito.mockStatic(TLManager.class);
        BDDMockito.when(TLManager.getInstance()).thenReturn(tlManagerSpy);

        //Probably not needed, but just to be safe
        Whitebox.setInternalState(tlAnalyticsManagerSpy, "clientPostFailureCount", NUMBER_OF_PINGS);

        //Create our own spyable flushEventsQueue
        Handler flushEventsQueueHandler = mock(Handler.class);
        when(flushEventsQueueHandler.postDelayed(any(Runnable.class), anyLong())).thenReturn(true);
        Whitebox.setInternalState(tlAnalyticsManagerSpy, "flushEventsQueueHandler", flushEventsQueueHandler);

        tlAnalyticsManagerSpy.queueTLEventsUpdate(null);

        //Verify the correct delay was calculated
        verify(flushEventsQueueHandler).postDelayed(any(Runnable.class), gt((long) Math.pow(2, NUMBER_OF_PINGS)));
    }

    @Test
    public void testPingMaxBackoffDelay() {
        //60 comes from REPORTING_INT in TLAnalyticsManager
        double BASE_DELAY_VALUE = 60;

        int NUMBER_OF_PINGS = 9;

        TLManager tlManagerSpy = spy(tlManager);
        TLAnalyticsManager tlAnalyticsManagerSpy = spy(tlAnalyticsManager);

        //Make the "random" delay predictable (return value is RANDOM_VALUE)
        PowerMockito.mockStatic(TLUtils.class);

        //Provide a mock database with 1 "event"
        TLDatabaseHelper mockDBHelper = mock(TLDatabaseHelper.class);
        when(mockDBHelper.getEventCount()).thenReturn(1);
        tlAnalyticsManagerSpy.setDBHelperMock(mockDBHelper);

        PowerMockito.mockStatic(TLManager.class);
        BDDMockito.when(TLManager.getInstance()).thenReturn(tlManagerSpy);

        //Probably not needed, but just to be safe
        Whitebox.setInternalState(tlAnalyticsManagerSpy, "clientPostFailureCount", NUMBER_OF_PINGS);

        //Create our own spyable flushEventsQueue
        Handler flushEventsQueueHandler = mock(Handler.class);
        when(flushEventsQueueHandler.postDelayed(any(Runnable.class), anyLong())).thenReturn(true);
        Whitebox.setInternalState(tlAnalyticsManagerSpy, "flushEventsQueueHandler", flushEventsQueueHandler);

        tlAnalyticsManagerSpy.queueTLEventsUpdate(null);

        //Verify the correct delay was calculated
        verify(flushEventsQueueHandler).postDelayed(any(Runnable.class), eq(Math.round(BASE_DELAY_VALUE + TLAnalyticsManager.MAX_BACKOFF_TIME) * 1000));
    }

    @Test
    public void testAppActiveResetsBackgroundPingCount() {
        String event = TLAnalyticsManager.TLAnalyticsEventAppActive;

        TLManager tlManagerSpy = spy(tlManager);
        TLAnalyticsManager tlAnalyticsManagerSpy = spy(tlAnalyticsManager);

        PowerMockito.mockStatic(TLManager.class);
        BDDMockito.when(TLManager.getInstance()).thenReturn(tlManagerSpy);


        tlAnalyticsManagerSpy.trackTLEvent(event);

        verify(tlAnalyticsManagerSpy).resetClientPostFailureCount();
    }

    @Test
    public void testNetworkErrorIncrementsPostFailCount() throws JSONException {
        final TLAnalyticsManager tlAnalyticsManagerSpy = spy(tlAnalyticsManager);
        TLManager tlManagerSpy = spy(tlManager);


        TLNetworking tlNetworking = new TLNetworking() {
            @Override
            public void enqueuePost(String type, String tag, String url, Date time, JSONObject props, TLNetworkResponseListener listener) {
                listener.onError(null);
            }

            @Override
            public void gitRequest(TLNetworkResponseListener listener) {

            }

            @Override
            protected void enqueueImagePost(String type, String url, String tag, Date time, JSONObject params, TLNetworkResponseListener listener) {

            }

            @Override
            public void clientRequest(Map<String, Object> props, TLNetworkPropertiesResponseListener listener, Date time) {

            }

            @Override
            public void setupNetworking(boolean isTest) {

            }
        };

        ArrayList<JSONObject> events = new ArrayList<>();
        events.add(new JSONObject("{\"gn\": \"event1\"}"));
        events.add(new JSONObject("{\"gn\": \"event2\"}"));
        events.add(new JSONObject("{\"gn\": \"event3\"}"));

        PowerMockito.mockStatic(TLManager.class);
        BDDMockito.when(TLManager.getInstance()).thenReturn(tlManagerSpy);

        Whitebox.setInternalState(tlManagerSpy, "tlNetworking", tlNetworking);

        boolean wait = true;

        TLFlushListener listener = new TLFlushListener() {
            @Override
            public void flushCompleted(boolean success) {
                //Should not occur
                assert (false);
            }

            @Override
            public void flushFailed() {
                int count = Whitebox.getInternalState(tlAnalyticsManagerSpy, "clientPostFailureCount");
                assertEquals(count, 1);
            }
        };


        tlAnalyticsManagerSpy.postEventsBatchForSessionID(events, "session_id", listener);

        while (wait) {
            int count = Whitebox.getInternalState(tlAnalyticsManagerSpy, "clientPostFailureCount");
            wait = count != 1;
        }

    }

    public RequestQueue newVolleyRequestQueueForTest(final Context context) {
        File cacheDir = new File(context.getCacheDir(), "cache/volley");
        Network network = new BasicNetwork(new HurlStack());
        ResponseDelivery responseDelivery = new ExecutorDelivery(Executors.newSingleThreadExecutor());
        RequestQueue queue = new RequestQueue(new DiskBasedCache(cacheDir), network, 4, responseDelivery);
        queue.start();
        return queue;
    }


    public JSONObject createJSONObjectWithSessionIDAndEventName(String sessionID, String eventName) {
        try {
            return new JSONObject("{\"t\": \"goalAchieved\", \"gn\":\"" + eventName + "\", \"sid\": \"" + sessionID + "\", \"type\": \"tlType\"}");
        } catch (Exception e) {
            return null;
        }
    }

}
