/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.network;

import android.annotation.TargetApi;
import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

import com.android.volley.ExecutorDelivery;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.ResponseDelivery;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.taplytics.sdk.analytics.TLAnalyticsManager;
import com.taplytics.sdk.analytics.TLAppUser;
import com.taplytics.sdk.datatypes.TLDeviceInfo;
import com.taplytics.sdk.datatypes.TLProperties;
import com.taplytics.sdk.managers.TLManager;
import com.taplytics.sdk.managers.TLViewManager;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.powermock.reflect.Whitebox;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * Created by vicvu on 16-05-03.
 * <p/>
 * Testing Volley and
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class TLNetworkingTest {

    private HashMap deviceInfo;
    private TLManager tlManager;
    private TLNetworking tlNetworking;

    @Mock
    Application appContext;

    @Mock
    TLAnalyticsManager tlAnalyticsManager;

    @Mock
    TLViewManager tlViewManager;

    @Mock
    TLAppUser tlAppUser;

    @Mock
    TLDeviceInfo tlDeviceInfo;

    @Spy
    TLManager tlManagerInstanceMock;

    @Before
    @Config(sdk = 23)
    public void setUp() {
        MockitoAnnotations.initMocks(this);
//        tlNetworking = new TLNetworking(false);

        deviceInfo = setupDeviceProperties();
        tlManager = TLManager.getInstance();

        VolleyLog.DEBUG = false;

        Whitebox.setInternalState(tlManager, "apiKey", "93f742e3bf0c6fcccb6fb78f04b9c5a923ad76a0");
        Whitebox.setInternalState(tlManager, "tlNetworking", tlNetworking);
        Whitebox.setInternalState(tlManager, "tlAnalytics", tlAnalyticsManager);
        Whitebox.setInternalState(tlManager, "tlDeviceInfo", tlDeviceInfo);

        doNothing().when(tlAnalyticsManager).trackError(anyString(), any(Exception.class), anyBoolean());

        when(tlAppUser.getUserAttribute(anyString())).thenReturn(null);
        when(tlAppUser.getUserId()).thenReturn("test");
        when(tlAnalyticsManager.getTlAppUser()).thenReturn(tlAppUser);
        when(appContext.getCacheDir()).thenReturn(new File("test"));

        File f = appContext.getCacheDir();
        when(appContext.getFilesDir()).thenReturn(new File(f, "cache/files"));
        when(tlDeviceInfo.getDeviceProperties()).thenReturn(deviceInfo);

        tlManager.setAppContext(appContext);

        tlManagerInstanceMock.setAppContext(appContext);

        HashMap map = getTestImagesToSend();

        when(tlViewManager.getImagesToSend()).thenReturn(map);
    }

    @After
    public void tearDown() {
//        Whitebox.setInternalState(tlManager, "tlNetworking", (Object[]) null);
        tlNetworking = null;
        deviceInfo = null;
        tlManager = null;
        tlProperties = null;
        serverResponse = null;
        volleyProperties = null;
        okHttpProperties = null;
        volleyResponse = null;
        okHTTPResponse = null;
    }

    TLProperties tlProperties = null;
    TLProperties volleyProperties = null;
    TLProperties okHttpProperties = null;

    @Test
    public void testSimpleGetPropertiesFromServerVolley() throws Exception {
        tlNetworking = new TLVolleyNetworking();
        ((TLVolleyNetworking) tlNetworking).setRequestQueue(newVolleyRequestQueueForTest(appContext));
        tlNetworking.setEnvironment(EnvironmentConfig.Prod.INSTANCE);

        TLNetworking.TLNetworkPropertiesResponseListener listener = new TLNetworking.TLNetworkPropertiesResponseListener() {
            @Override
            public void onResponse(TLProperties properties) {
                tlProperties = properties;
            }

            @Override
            public void onError(Throwable error) {
                assertThat(false);
            }
        };

        tlNetworking.getPropertiesFromServer(null, listener);
        await().atMost(15, TimeUnit.SECONDS).until(TLPropertiesReceived());
        volleyProperties = tlProperties;
    }

    @Test
    public void testGetSimpleGetPropertiesFromServerOkHttp() throws Exception {
        tlNetworking = new TLRetrofitNetworking();
        tlNetworking.setupNetworking(true);
        tlNetworking.setEnvironment(EnvironmentConfig.Prod.INSTANCE);

        TLNetworking.TLNetworkPropertiesResponseListener listener = new TLNetworking.TLNetworkPropertiesResponseListener() {
            @Override
            public void onResponse(TLProperties properties) {
                tlProperties = properties;
            }

            @Override
            public void onError(Throwable error) {
                tlProperties = null;
            }
        };

        tlNetworking.getPropertiesFromServer(null, listener);
        await().atMost(15, TimeUnit.SECONDS).until(TLPropertiesReceived());
        okHttpProperties = tlProperties;
    }

    @Test
    public void testVolleyAndOkHTTPResponsesAreSame() throws Exception {
        testSimpleGetPropertiesFromServerVolley();
        testGetSimpleGetPropertiesFromServerOkHttp();
        await().atMost(15, TimeUnit.SECONDS).until(tlPropertiesEqual());
    }

    private JSONObject serverResponse;

    @Test
    public void testVolleyPostTapViewElements() throws Exception {
        TLViewManager.setInstance(tlViewManager);
        TLNetworking.TLNetworkResponseListener listener = basicVolleyTestSetupListener();

        tlNetworking.postTapViewElements(null, listener, getTapViewElementsParams());
        await().atMost(15, TimeUnit.SECONDS).until(responseReceived());
    }

    @Test
    public void testOkHttpTapViewElements() throws Exception {
        TLViewManager.setInstance(tlViewManager);
        TLNetworking.TLNetworkResponseListener listener = basicOkHttpTestSetupListener();

        tlNetworking.postTapViewElements(null, listener, getTapViewElementsParams());
        await().atMost(15, TimeUnit.SECONDS).until(responseReceived());
    }

    JSONObject volleyResponse;
    JSONObject okHTTPResponse;

    @Test
    public void testOkHTTPAndVolleyTapViewPostAreSame() throws Exception {
        testOkHttpTapViewElements();
        testVolleyPostTapViewElements();
        await().atMost(20, TimeUnit.SECONDS).until(responseEqual());
    }

    @Test
    public void testVolleyPostChosenActivity() throws Exception {
        TLViewManager.setInstance(tlViewManager);

        TLNetworking.TLNetworkResponseListener listener = basicVolleyTestSetupListener();
        tlNetworking.postChosenActivity(getActivityObj(), listener);

        await().atMost(15, TimeUnit.SECONDS).until(responseReceived());

    }

    @Test
    public void testOkHttpPostChosenActivity() throws Exception {
        TLViewManager.setInstance(tlViewManager);
        TLNetworking.TLNetworkResponseListener listener = basicOkHttpTestSetupListener();

        tlNetworking.postChosenActivity(getActivityObj(), listener);
        await().atMost(15, TimeUnit.SECONDS).until(responseReceived());
    }

    @Test
    public void testOkHTTPAndVolleyPostChosenActivityAreSame() throws Exception {
        testOkHttpPostChosenActivity();
        testVolleyPostChosenActivity();
        await().atMost(20, TimeUnit.SECONDS).until(responseEqual());
    }

    @Test
    public void testVolleyPostClientEvents() throws Exception {
        TLNetworking.TLNetworkResponseListener listener = basicVolleyTestSetupListener();
        tlNetworking.postClientEvents(getTestEventProps(), listener);
        await().atMost(15, TimeUnit.SECONDS).until(responseReceived());
    }

    @Test
    public void testOkHttpPostClientEvents() throws Exception {
        TLNetworking.TLNetworkResponseListener listener = basicOkHttpTestSetupListener();
        tlNetworking.postClientEvents(getTestEventProps(), listener);
        await().atMost(15, TimeUnit.SECONDS).until(responseReceived());
    }

    @Test
    public void testOkHTTPAndVolleyPostClientEventsAreSame() throws Exception {
        testOkHttpPostClientEvents();
        testVolleyPostClientEvents();
        await().atMost(20, TimeUnit.SECONDS).until(responseEqual());
    }

    @Test
    public void testVolleyUploadAppIcon() throws Exception {
        Whitebox.setInternalState(TLManager.class, "instance", tlManagerInstanceMock);
        when(tlManagerInstanceMock.getApiKey()).thenReturn("93f742e3bf0c6fcccb6fb78f04b9c5a923ad76a0");

        final byte[] appIcon = getAppIcon();
        when(tlManagerInstanceMock.getAppIconBytes()).thenReturn(appIcon);
        TLNetworking.TLNetworkResponseListener listener = basicVolleyTestSetupListener();

        tlNetworking.postAppIcon(listener);
        await().atMost(15, TimeUnit.SECONDS).until(responseReceived());
    }

    @Test
    public void testOkHttpUploadAppIcon() throws Exception {
        Whitebox.setInternalState(TLManager.class, "instance", tlManagerInstanceMock);
        when(tlManagerInstanceMock.getApiKey()).thenReturn("93f742e3bf0c6fcccb6fb78f04b9c5a923ad76a0");
        when(tlManagerInstanceMock.getAppIconBytes()).thenReturn(getAppIcon());
        TLNetworking.TLNetworkResponseListener listener = basicOkHttpTestSetupListener();

        tlNetworking.postAppIcon(listener);
        await().atMost(15, TimeUnit.SECONDS).until(responseReceived());
    }

    @Test
    public void testVolleyOkHttpUploadAppIconAreSame() throws Exception {
        testVolleyUploadAppIcon();
        testOkHttpUploadAppIcon();
        await().atMost(20, TimeUnit.SECONDS).until(responseEqual());
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    private byte[] getAppIcon() throws Exception {
        File file = new File("test/assets/appIcon/totle.png");
        assertTrue(file.exists());
        Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    @Test
    public void testVolleyGetGitRelease() throws Exception {
        TLNetworking.TLNetworkResponseListener listener = basicVolleyTestSetupListener();
        tlNetworking.getCurrentReleaseTag(listener);
        await().atMost(15, TimeUnit.SECONDS).until(responseReceived());
    }

    @Test
    public void testOkHttpGetGitRelease() throws Exception {
        TLNetworking.TLNetworkResponseListener listener = basicOkHttpTestSetupListener();
        tlNetworking.getCurrentReleaseTag(listener);
        await().atMost(50, TimeUnit.SECONDS).until(responseReceived());
    }

    @Test
    public void testOkHTTPAndVolleyGitReleaseSame() throws Exception {
        testOkHttpGetGitRelease();
        testVolleyGetGitRelease();
        await().atMost(20, TimeUnit.SECONDS).until(responseEqual());
    }

    @Test
    public void testVolleyPostAppUserAttributes() throws Exception {
        TLNetworking.TLNetworkResponseListener listener = basicVolleyTestSetupListener();
        tlNetworking.postAppUserAttributes(getTestUserAttributes(), listener);
        await().atMost(15, TimeUnit.SECONDS).until(responseReceived());
    }

    @Test
    public void testOkHttpPostAppUserAttributes() throws Exception {
        TLNetworking.TLNetworkResponseListener listener = basicOkHttpTestSetupListener();
        tlNetworking.postAppUserAttributes(getTestUserAttributes(), listener);
        await().atMost(15, TimeUnit.SECONDS).until(responseReceived());
    }

    @Test
    public void testOkHTTPAndVolleyAppUserAttributesAreSame() throws Exception {
        testSimpleGetPropertiesFromServerVolley();
        Whitebox.setInternalState(tlManager, "tlProperties", tlProperties);
        testOkHttpPostAppUserAttributes();
        testVolleyPostAppUserAttributes();
        await().atMost(20, TimeUnit.SECONDS).until(attributeResponseEqual());
    }

    @Test
    public void testVolleyPostResetAppUser() throws Exception {
        TLNetworking.TLNetworkResponseListener listener = basicVolleyTestSetupListener();
        tlNetworking.postResetAppUser(getTestUserAttributes().put("ad", "8XV7N15C08000474"), listener);
        await().atMost(15, TimeUnit.SECONDS).until(responseReceived());
    }

    @Test
    public void testOkHttpPostResetAppUser() throws Exception {
        TLNetworking.TLNetworkResponseListener listener = basicOkHttpTestSetupListener();
        tlNetworking.postResetAppUser(getTestUserAttributes().put("ad", "8XV7N15C08000474"), listener);
        await().atMost(15, TimeUnit.SECONDS).until(responseReceived());
    }

    @Test
    public void testOkHTTPAndVolleyResetAppUserIsSame() throws Exception {
        testVolleyPostResetAppUser();
        testOkHttpPostResetAppUser();
        await().atMost(20, TimeUnit.SECONDS).until(responseEqual());
    }

    @Test
    public void testVolleyPostGCMToken() throws Exception {
        Whitebox.setInternalState(tlManager, "apiKey", "c5c68f01cacc9e9694ba270ca73b95a56ff64e8b");
        TLNetworking.TLNetworkResponseListener listener = basicVolleyTestSetupListener();
        tlNetworking.postGCMToken(getTestGCMTokenParams(), listener);
        await().atMost(15, TimeUnit.SECONDS).until(responseReceived());
    }

    @Test
    public void testOkHttpPostGCMToken() throws Exception {
        Whitebox.setInternalState(tlManager, "apiKey", "c5c68f01cacc9e9694ba270ca73b95a56ff64e8b");
        TLNetworking.TLNetworkResponseListener listener = basicOkHttpTestSetupListener();
        tlNetworking.postGCMToken(getTestGCMTokenParams(), listener);
        await().atMost(15, TimeUnit.SECONDS).until(responseReceived());
    }

    @Test
    public void testOkHTTPAndVolleyPostGCMTokenAreSame() throws Exception {
        testOkHttpPostGCMToken();
        testVolleyPostGCMToken();
        await().atMost(20, TimeUnit.SECONDS).until(attributeResponseEqual());
    }

    private TLNetworking.TLNetworkResponseListener basicVolleyTestSetupListener() {
        tlNetworking = new TLVolleyNetworking();
        ((TLVolleyNetworking) tlNetworking).setRequestQueue(newVolleyRequestQueueForTest(appContext));
        tlNetworking.setEnvironment(EnvironmentConfig.Prod.INSTANCE);

        return new TLNetworking.TLNetworkResponseListener() {
            @Override
            public void onResponse(JSONObject response) {
                serverResponse = response;
                volleyResponse = response;
            }

            @Override
            public void onError(Throwable error) {
                System.out.print(error.getCause());
                serverResponse = null;
            }
        };
    }

    @Config(sdk = 23)
    private TLNetworking.TLNetworkResponseListener basicOkHttpTestSetupListener() throws Exception {
        tlNetworking = new TLRetrofitNetworking();
        tlNetworking.setupNetworking(true);
        tlNetworking.setEnvironment(EnvironmentConfig.Prod.INSTANCE);

        return new TLNetworking.TLNetworkResponseListener() {
            @Override
            public void onResponse(JSONObject response) {
                serverResponse = response;
                okHTTPResponse = response;
            }

            @Override
            public void onError(Throwable error) {
                serverResponse = null;
            }
        };
    }

    private HashMap getTestImagesToSend() {
        HashMap<String, byte[]> map = new HashMap<>();
        File[] files = new File("test/assets/uploadTestImages").listFiles();
        for (File file : files) {
            Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            map.put(file.getName(), outputStream.toByteArray());
        }
        assertTrue(map.size() == 3);
        return map;
    }

    private Callable<Boolean> tlPropertiesEqual() {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                if (volleyProperties == null || okHttpProperties == null) {
                    return false;
                }
                assertEquals(volleyProperties, okHttpProperties);
                return true;
            }
        };
    }

    private Callable<Boolean> responseEqual() {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                if (volleyResponse == null || okHTTPResponse == null) {
                    return false;
                }
                JSONAssert.assertEquals(volleyResponse, okHTTPResponse, true);
                return true;
            }
        };
    }

    private Callable<Boolean> attributeResponseEqual() {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                if (volleyResponse == null || okHTTPResponse == null) {
                    return false;
                }
                Iterator<?> keys = volleyResponse.keys();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    switch (key) {
                        case "_version":
                        case "version":
                            assertTrue(Math.abs((Integer) volleyResponse.get(key) - (Integer) okHTTPResponse.get(key)) <= 1);
                            break;
                        case "lastSeenDate":
                        case "_lastSeenDate":
                        case "updatedAt":
                        case "_updatedAt":
                            assertTrue(((String) volleyResponse.get(key)).substring(0, 16).equals(((String) okHTTPResponse.get(key)).substring(0, 16)));
                            break;
                        default:
                            if (volleyResponse.get(key) instanceof JSONObject) {
                                JSONAssert.assertEquals((JSONObject) volleyResponse.get(key), (JSONObject) okHTTPResponse.get(key), false);
                            } else if (volleyResponse.get(key) instanceof JSONArray) {
                                JSONAssert.assertEquals((JSONArray) volleyResponse.get(key), (JSONArray) okHTTPResponse.get(key), false);
                            } else {
                                assertEquals(volleyResponse.get(key), (okHTTPResponse.get(key)));
                            }
                            break;
                    }
                }
                return true;
            }
        };
    }

    private Callable<Boolean> TLPropertiesReceived() {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return tlProperties != null;
            }
        };
    }

    private Callable<Boolean> responseReceived() {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return serverResponse != null;
            }
        };
    }

    //Specific test queue that uses a singleThreadExecutor instead of the mainLooper for testing purposes.
    public RequestQueue newVolleyRequestQueueForTest(final Context context) {
        File cacheDir = new File(context.getCacheDir(), "cache/volley");
        Network network = new BasicNetwork(new HurlStack());
        ResponseDelivery responseDelivery = new ExecutorDelivery(Executors.newSingleThreadExecutor());
        RequestQueue queue = new RequestQueue(new DiskBasedCache(cacheDir), network, 4, responseDelivery);
        queue.start();
        return queue;
    }

    private JSONObject getActivityObj() throws Exception {
        return new JSONObject("{\"user_id\":\"53f645b811bf7a592154bdab\",\"user_name\":\"Victor Vucicevich\",\"project_id\":\"564cde89c618d2753f615d82\",\"experiment_id\":\"572cc921f4e3a61400d70a1b\",\"goal_id\":\"573391f2386bc30f003b2093\",\"action\":\"chooseGoalView\",\"viewKey\":\"VisualActivity\",\"imgFileName\":\"1010.jpg\",\"fragments\":[],\"isAndroid\":true}");
    }

    private JSONObject getTapViewElementsParams() throws Exception {
        return new JSONObject("{\"user_id\":\"53f645b811bf7a592154bdab\",\"user_name\":\"Victor Vucicevich\",\"project_id\":\"564cde89c618d2753f615d82\",\"experiment_id\":\"572cc921f4e3a61400d70a1b\",\"chooseButton\":false,\"t\":\"c5c68f01cacc9e9694ba270ca73b95a56ff64e8b\",\"os\":\"Android\",\"views\":[{\"id\":16908290,\"identifier\":\"content\",\"baseClass\":\"FrameLayout\",\"class\":\"ContentFrameLayout\",\"subClasses\":[\"ContentFrameLayout\",\"FrameLayout\",\"ViewGroup\",\"View\"],\"activity\":\"MainActivity\",\"methodInfo\":{\"variables\":[{\"methodName\":\"getPaddingTop\",\"currentValue\":0},{\"methodName\":\"getPaddingBottom\",\"currentValue\":0},{\"methodName\":\"getPaddingRight\",\"currentValue\":0},{\"methodName\":\"getHeight\",\"currentValue\":2308},{\"methodName\":\"getWidth\",\"currentValue\":1440},{\"methodName\":\"isHapticFeedbackEnabled\",\"currentValue\":true},{\"methodName\":\"getAlpha\",\"currentValue\":1},{\"methodName\":\"getVisibility\",\"currentValue\":0},{\"methodName\":\"getPaddingLeft\",\"currentValue\":0}],\"setters\":[{\"paramTypes\":[\"int\"],\"methodName\":\"setBackgroundColor\"},{\"paramTypes\":[\"boolean\"],\"methodName\":\"setHapticFeedbackEnabled\"},{\"paramTypes\":[\"float\"],\"methodName\":\"setAlpha\"},{\"paramTypes\":[\"int\",\"int\",\"int\",\"int\"],\"methodName\":\"setPadding\"},{\"paramTypes\":[\"android.graphics.drawable.Drawable\"],\"methodName\":\"setBackgroundDrawable\"},{\"paramTypes\":[\"int\"],\"methodName\":\"setVisibility\"},{\"methodName\":\"setWidth\",\"paramTypes\":[\"int\"]},{\"methodName\":\"setHeight\",\"paramTypes\":[\"int\"]}]},\"position\":{\"startX\":0,\"startY\":84,\"endX\":1440,\"endY\":2392,\"screenDimensions\":{\"width\":1440,\"height\":2560}},\"imgFileName\":\"16908290.jpg\",\"hasOnClick\":false},{\"id\":2131492937,\"identifier\":\"action_bar_root\",\"baseClass\":\"LinearLayout\",\"class\":\"FitWindowsLinearLayout\",\"subClasses\":[\"FitWindowsLinearLayout\",\"LinearLayout\",\"ViewGroup\",\"View\"],\"activity\":\"MainActivity\",\"methodInfo\":{\"variables\":[],\"setters\":[{\"methodName\":\"setWidth\",\"paramTypes\":[\"int\"]},{\"methodName\":\"setHeight\",\"paramTypes\":[\"int\"]}]},\"position\":{\"startX\":0,\"startY\":84,\"endX\":1440,\"endY\":2392,\"screenDimensions\":{\"width\":1440,\"height\":2560}},\"imgFileName\":\"2131492937.jpg\",\"hasOnClick\":false}],\"exp_id\":\"572cc921f4e3a61400d70a1b\"}");
    }

    private JSONObject getTestEventProps() throws Exception {
        return new JSONObject("{\"t\":\"c5c68f01cacc9e9694ba270ca73b95a56ff64e8b\",\"sid\":\"5734a947ae2b6b2601035cc6\",\"e\":[{\"type\":\"appActive\",\"date\":\"2016-05-12T12:01-0400\",\"prod\":0,\"sid\":\"5734a947ae2b6b2601035cc6\"},{\"type\":\"appActive\",\"date\":\"2016-05-12T12:02-0400\",\"prod\":0,\"sid\":\"5734a947ae2b6b2601035cc6\"},{\"type\":\"viewAppeared\",\"date\":\"2016-05-12T12:02-0400\",\"val\":\"2016-05-12T12:02-0400\",\"prod\":0,\"vKey\":\"MainActivity\",\"sid\":\"5734a947ae2b6b2601035cc6\"},{\"type\":\"viewDisappeared\",\"date\":\"2016-05-12T12:02-0400\",\"val\":\"2016-05-12T12:02-0400\",\"prod\":0,\"vKey\":\"MainActivity\",\"sid\":\"5734a947ae2b6b2601035cc6\"},{\"type\":\"viewTimeOnPage\",\"date\":\"2016-05-12T12:02-0400\",\"val\":1597,\"prod\":0,\"sid\":\"5734a947ae2b6b2601035cc6\"},{\"type\":\"tlHasAppLinking\",\"date\":\"2016-05-12T12:02-0400\",\"sid\":\"5734a91c6d708f7d018423c8\",\"prod\":0},{\"type\":\"viewAppeared\",\"date\":\"2016-05-12T12:03-0400\",\"val\":\"2016-05-12T12:03-0400\",\"sid\":\"5734a9260256c7bc04741a83\",\"prod\":0,\"vKey\":\"MainActivity\"},{\"type\":\"appBackground\",\"date\":\"2016-05-12T12:03-0400\",\"sid\":\"5734a9260256c7bc04741a83\",\"prod\":0},{\"type\":\"viewAppeared\",\"date\":\"2016-05-12T12:03-0400\",\"val\":\"2016-05-12T12:03-0400\",\"sid\":\"5734a9260256c7bc04741a83\",\"prod\":0,\"vKey\":\"UserAttributesActivity\"},{\"type\":\"viewDisappeared\",\"date\":\"2016-05-12T12:03-0400\",\"val\":\"2016-05-12T12:03-0400\",\"prod\":0,\"vKey\":\"MainActivity\",\"sid\":\"5734a947ae2b6b2601035cc6\"},{\"type\":\"viewTimeOnPage\",\"date\":\"2016-05-12T12:03-0400\",\"val\":8130,\"prod\":0,\"sid\":\"5734a947ae2b6b2601035cc6\"},{\"type\":\"viewAppeared\",\"date\":\"2016-05-12T12:03-0400\",\"val\":\"2016-05-12T12:03-0400\",\"prod\":0,\"vKey\":\"MainActivity\",\"sid\":\"5734a947ae2b6b2601035cc6\"},{\"type\":\"viewDisappeared\",\"date\":\"2016-05-12T12:03-0400\",\"val\":\"2016-05-12T12:03-0400\",\"prod\":0,\"vKey\":\"UserAttributesActivity\",\"sid\":\"5734a947ae2b6b2601035cc6\"},{\"type\":\"viewTimeOnPage\",\"date\":\"2016-05-12T12:03-0400\",\"val\":828,\"prod\":0,\"sid\":\"5734a947ae2b6b2601035cc6\"},{\"type\":\"googleAnalytics\",\"date\":\"2016-05-12T12:03-0400\",\"data\":{},\"val\":0,\"sid\":\"5734a947ae2b6b2601035cc6\",\"prod\":0,\"gn\":\"screenview\"},{\"type\":\"goalAchieved\",\"date\":\"2016-05-12T12:03-0400\",\"sid\":\"5734a947ae2b6b2601035cc6\",\"prod\":0,\"gn\":\"Activity Started\"},{\"type\":\"viewAppeared\",\"date\":\"2016-05-12T12:03-0400\",\"val\":\"2016-05-12T12:03-0400\",\"sid\":\"5734a947ae2b6b2601035cc6\",\"prod\":0,\"vKey\":\"EventsActivity\"},{\"type\":\"viewDisappeared\",\"date\":\"2016-05-12T12:03-0400\",\"val\":\"2016-05-12T12:03-0400\",\"sid\":\"5734a947ae2b6b2601035cc6\",\"prod\":0,\"vKey\":\"MainActivity\"},{\"type\":\"viewTimeOnPage\",\"date\":\"2016-05-12T12:03-0400\",\"val\":1812,\"sid\":\"5734a947ae2b6b2601035cc6\",\"prod\":0},{\"type\":\"goalAchieved\",\"date\":\"2016-05-12T12:03-0400\",\"data\":{\"subscriber\":false},\"val\":1,\"sid\":\"5734a947ae2b6b2601035cc6\",\"prod\":0,\"gn\":\"Button Clicked\"},{\"type\":\"googleAnalytics\",\"date\":\"2016-05-12T12:03-0400\",\"data\":{\"&el\":\"yabba\",\"&ec\":\"Clicks\",\"&t\":\"event\"},\"val\":0,\"sid\":\"5734a947ae2b6b2601035cc6\",\"prod\":0,\"gn\":\"event Click\"},{\"type\":\"goalAchieved\",\"date\":\"2016-05-12T12:03-0400\",\"data\":{\"subscriber\":false},\"val\":2,\"sid\":\"5734a947ae2b6b2601035cc6\",\"prod\":0,\"gn\":\"Button Clicked\"},{\"type\":\"googleAnalytics\",\"date\":\"2016-05-12T12:03-0400\",\"data\":{\"&el\":\"yabba\",\"&ec\":\"Clicks\",\"&t\":\"event\"},\"val\":0,\"sid\":\"5734a947ae2b6b2601035cc6\",\"prod\":0,\"gn\":\"event Click\"},{\"type\":\"goalAchieved\",\"date\":\"2016-05-12T12:03-0400\",\"data\":{\"subscriber\":false},\"val\":3,\"sid\":\"5734a947ae2b6b2601035cc6\",\"prod\":0,\"gn\":\"Button Clicked\"},{\"type\":\"googleAnalytics\",\"date\":\"2016-05-12T12:03-0400\",\"data\":{\"&el\":\"yabba\",\"&ec\":\"Clicks\",\"&t\":\"event\"},\"val\":0,\"sid\":\"5734a947ae2b6b2601035cc6\",\"prod\":0,\"gn\":\"event Click\"},{\"type\":\"goalAchieved\",\"date\":\"2016-05-12T12:03-0400\",\"data\":{\"subscriber\":false},\"val\":4,\"sid\":\"5734a947ae2b6b2601035cc6\",\"prod\":0,\"gn\":\"Button Clicked\"},{\"type\":\"googleAnalytics\",\"date\":\"2016-05-12T12:03-0400\",\"data\":{\"&el\":\"yabba\",\"&ec\":\"Clicks\",\"&t\":\"event\"},\"val\":0,\"sid\":\"5734a947ae2b6b2601035cc6\",\"prod\":0,\"gn\":\"event Click\"},{\"type\":\"goalAchieved\",\"date\":\"2016-05-12T12:03-0400\",\"data\":{\"subscriber\":false},\"val\":5,\"sid\":\"5734a947ae2b6b2601035cc6\",\"prod\":0,\"gn\":\"Button Clicked\"},{\"type\":\"googleAnalytics\",\"date\":\"2016-05-12T12:03-0400\",\"data\":{\"&el\":\"yabba\",\"&ec\":\"Clicks\",\"&t\":\"event\"},\"val\":0,\"sid\":\"5734a947ae2b6b2601035cc6\",\"prod\":0,\"gn\":\"event Click\"},{\"type\":\"goalAchieved\",\"date\":\"2016-05-12T12:03-0400\",\"data\":{\"subscriber\":false},\"val\":6,\"sid\":\"5734a947ae2b6b2601035cc6\",\"prod\":0,\"gn\":\"Button Clicked\"},{\"type\":\"googleAnalytics\",\"date\":\"2016-05-12T12:03-0400\",\"data\":{\"&el\":\"yabba\",\"&ec\":\"Clicks\",\"&t\":\"event\"},\"val\":0,\"sid\":\"5734a947ae2b6b2601035cc6\",\"prod\":0,\"gn\":\"event Click\"},{\"type\":\"goalAchieved\",\"date\":\"2016-05-12T12:03-0400\",\"data\":{\"subscriber\":false},\"val\":7,\"sid\":\"5734a947ae2b6b2601035cc6\",\"prod\":0,\"gn\":\"Button Clicked\"},{\"type\":\"googleAnalytics\",\"date\":\"2016-05-12T12:03-0400\",\"data\":{\"&el\":\"yabba\",\"&ec\":\"Clicks\",\"&t\":\"event\"},\"val\":0,\"sid\":\"5734a947ae2b6b2601035cc6\",\"prod\":0,\"gn\":\"event Click\"},{\"type\":\"goalAchieved\",\"date\":\"2016-05-12T12:03-0400\",\"data\":{\"subscriber\":false},\"val\":8,\"sid\":\"5734a947ae2b6b2601035cc6\",\"prod\":0,\"gn\":\"Button Clicked\"},{\"type\":\"googleAnalytics\",\"date\":\"2016-05-12T12:03-0400\",\"data\":{\"&el\":\"yabba\",\"&ec\":\"Clicks\",\"&t\":\"event\"},\"val\":0,\"sid\":\"5734a947ae2b6b2601035cc6\",\"prod\":0,\"gn\":\"event Click\"},{\"type\":\"goalAchieved\",\"date\":\"2016-05-12T12:03-0400\",\"data\":{\"subscriber\":false},\"val\":9,\"sid\":\"5734a947ae2b6b2601035cc6\",\"prod\":0,\"gn\":\"Button Clicked\"},{\"type\":\"googleAnalytics\",\"date\":\"2016-05-12T12:03-0400\",\"data\":{\"&el\":\"yabba\",\"&ec\":\"Clicks\",\"&t\":\"event\"},\"val\":0,\"sid\":\"5734a947ae2b6b2601035cc6\",\"prod\":0,\"gn\":\"event Click\"},{\"type\":\"goalAchieved\",\"date\":\"2016-05-12T12:03-0400\",\"data\":{\"subscriber\":false},\"val\":10,\"sid\":\"5734a947ae2b6b2601035cc6\",\"prod\":0,\"gn\":\"Button Clicked\"},{\"type\":\"googleAnalytics\",\"date\":\"2016-05-12T12:03-0400\",\"data\":{\"&el\":\"yabba\",\"&ec\":\"Clicks\",\"&t\":\"event\"},\"val\":0,\"sid\":\"5734a947ae2b6b2601035cc6\",\"prod\":0,\"gn\":\"event Click\"},{\"type\":\"goalAchieved\",\"date\":\"2016-05-12T12:03-0400\",\"data\":{\"subscriber\":false},\"val\":11,\"sid\":\"5734a947ae2b6b2601035cc6\",\"prod\":0,\"gn\":\"Button Clicked\"},{\"type\":\"googleAnalytics\",\"date\":\"2016-05-12T12:03-0400\",\"data\":{\"&el\":\"yabba\",\"&ec\":\"Clicks\",\"&t\":\"event\"},\"val\":0,\"sid\":\"5734a947ae2b6b2601035cc6\",\"prod\":0,\"gn\":\"event Click\"},{\"type\":\"goalAchieved\",\"date\":\"2016-05-12T12:03-0400\",\"data\":{\"subscriber\":false},\"val\":12,\"sid\":\"5734a947ae2b6b2601035cc6\",\"prod\":0,\"gn\":\"Button Clicked\"},{\"type\":\"googleAnalytics\",\"date\":\"2016-05-12T12:03-0400\",\"data\":{\"&el\":\"yabba\",\"&ec\":\"Clicks\",\"&t\":\"event\"},\"val\":0,\"sid\":\"5734a947ae2b6b2601035cc6\",\"prod\":0,\"gn\":\"event Click\"},{\"type\":\"googleAnalytics\",\"date\":\"2016-05-12T12:03-0400\",\"data\":{\"&el\":\"LORF\",\"&ec\":\"CORF\",\"&t\":\"event\"},\"val\":12,\"sid\":\"5734a947ae2b6b2601035cc6\",\"prod\":0,\"gn\":\"AORF\"},{\"type\":\"googleAnalytics\",\"date\":\"2016-05-12T12:03-0400\",\"data\":{\"&el\":\"LORF\",\"&ec\":\"CORF\",\"&t\":\"event\"},\"val\":12,\"sid\":\"5734a947ae2b6b2601035cc6\",\"prod\":0,\"gn\":\"AORF\"},{\"type\":\"goalAchieved\",\"date\":\"2016-05-12T12:03-0400\",\"data\":{\"subscriber\":false},\"val\":13,\"sid\":\"5734a947ae2b6b2601035cc6\",\"prod\":0,\"gn\":\"Button Clicked\"},{\"type\":\"googleAnalytics\",\"date\":\"2016-05-12T12:03-0400\",\"data\":{\"&el\":\"yabba\",\"&ec\":\"Clicks\",\"&t\":\"event\"},\"val\":0,\"sid\":\"5734a947ae2b6b2601035cc6\",\"prod\":0,\"gn\":\"event Click\"},{\"type\":\"goalAchieved\",\"date\":\"2016-05-12T12:03-0400\",\"data\":{\"subscriber\":false},\"val\":14,\"sid\":\"5734a947ae2b6b2601035cc6\",\"prod\":0,\"gn\":\"Button Clicked\"},{\"type\":\"googleAnalytics\",\"date\":\"2016-05-12T12:03-0400\",\"data\":{\"&el\":\"yabba\",\"&ec\":\"Clicks\",\"&t\":\"event\"},\"val\":0,\"sid\":\"5734a947ae2b6b2601035cc6\",\"prod\":0,\"gn\":\"event Click\"},{\"type\":\"viewDisappeared\",\"date\":\"2016-05-12T12:03-0400\",\"val\":\"2016-05-12T12:03-0400\",\"sid\":\"5734a947ae2b6b2601035cc6\",\"prod\":0,\"vKey\":\"EventsActivity\"},{\"type\":\"viewTimeOnPage\",\"date\":\"2016-05-12T12:03-0400\",\"val\":6176,\"sid\":\"5734a947ae2b6b2601035cc6\",\"prod\":0}]}");
    }

    private JSONObject getTestUserAttributes() throws Exception {
        return new JSONObject("{\"t\":\"93f742e3bf0c6fcccb6fb78f04b9c5a923ad76a0\",\"sid\":\"573a1175c1ad171800af22d8\",\"pid\":\"573493e5a99c6938031fb6bd\",\"k\":\"a4cbf0842807b43a0000\",\"au\":{\"customData\":{\"AAAAAAA\":\"BBBBBBB\",\"customAttribute\":true},\"email\":\"example_email_address@taplytics.com\",\"name\":\"exampleName\",\"age\":100},\"auid\":\"5739ecc668b53c0e02ac939c\"}");
    }

    private JSONObject getTestGCMTokenParams() throws Exception {
        return new JSONObject("{\"ad\":\"8XV7N15C08000474\",\"pid\":\"564cde89c618d2753f615d82\",\"t\":\"c5c68f01cacc9e9694ba270ca73b95a56ff64e8b\",\"auid\":\"5b3662b19161dd0162b28b68\",\"sid\":\"573b7a898266c6940333d0eb\",\"pt\":\"APA91bHUMN6g5oLs5eU7qcDuQPZAd8SMvMGUAu495ZbTqRs5zen-mPTm4KbFcMcEaCXD3kgXTe5_wyHXJSuxOE-lekr0VSvEruqJb8Hr3k9qxFdPXFjqNjxHsQIqPKbq88MEWHlKqxuM\",\"env\":\"prod\",\"os\":\"Android\"}");
    }

    private HashMap setupDeviceProperties() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("osv", "6.0.1");
        map.put("sdk", "1.7.18");
        map.put("n", "WIFI");
        map.put("tz", "EST");
        map.put("alg3", "eng");
        map.put("tzn", "America/Toronto");
        map.put("ma", "Huawei");
        map.put("t", "c5c68f01cacc9e9694ba270ca73b95a56ff64e8b");
        map.put("alg", "en");
        map.put("sh", "2392");
        map.put("adt", "anBuildSerial");
        map.put("tzs", "-14400");
        map.put("rd", "gsm");
        map.put("pid", "564cde89c618d2753f615d82");
        map.put("ai", "com.taplytics.testapp");
        map.put("au", "572795fe94b789c90084e5d9");
        map.put("os", "Android");
        map.put("av", "1.0");
        map.put("ca", "ROGERS");
        map.put("an", "Taplytics Test App");
        map.put("ad", "8XV7N15C08000474");
        map.put("con3", "CAN");
        map.put("con", "CA");
        map.put("sw", "1440");
        map.put("d", "Nexus 6P");
        map.put("sdpi", "560");
        map.put("br", "google");
        map.put("lv", "1");
        map.put("ab", "1");
        return map;
    }
}
