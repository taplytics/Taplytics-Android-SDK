/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.managers;

import android.app.Activity;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.RemoteMessage;
import com.taplytics.sdk.TaplyticsPushNotificationIntentListener;
import com.taplytics.sdk.TaplyticsPushNotificationListener;
import com.taplytics.sdk.TaplyticsPushOpenedListener;
import com.taplytics.sdk.TaplyticsPushTokenListener;
import com.taplytics.sdk.analytics.TLAnalyticsManager;
import com.taplytics.sdk.datatypes.TLProperties;
import com.taplytics.sdk.network.TLNetworking;
import com.taplytics.sdk.utils.Functionality;
import com.taplytics.sdk.utils.ImageUtils;
import com.taplytics.sdk.utils.TLLog;
import com.taplytics.sdk.utils.TLUtils;
import com.taplytics.sdk.utils.promises.PromiseListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


/**
 * Created by matthewkuzyk on 1/16/15.
 * <p/>
 * Edited by @vicv from 04/20/15 onward.
 */
public class TLPushManager {

    private Boolean checkedPushToken = false;
    private static Boolean hasNewPush = false;

    private Boolean manualTokenRegistration = false;
    private int appIcon;

    private static final int MAX_ATTEMPTS = 10;
    private static final int BACKOFF_MILLI_SECONDS = 10;

    public static final String OPEN_ACTION = "taplytics.push.OPEN";
    public static final String DISMISS_ACTION = "taplytics.push.DISMISS";


    //TODO: Make these user-configurable in the push payload.
    // The id of the channel.
    private static String DEFAULT_CHANNEL_ID = "taplytics_channel_id";
    // The user-visible name of the channel.
    private static CharSequence DEFAULT_CHANNEL_NAME = "Notifications";
    // The user-visible description of the channel.
    private static String DEFAULT_CHANNEL_DESCRIPTION = "Push Notifications";
    // The Android importance of the channel.
    private static int DEFAULT_IMPORTANCE = NotificationManager.IMPORTANCE_DEFAULT;
    /**
     * Key to note that the push image is also to be used as the notification icon
     **/
    private static final String TL_PUSH_KEY_IMAGE_ICON = "tl_image_icon";

    /**
     * Key to note whether a push icon should be shown at all
     **/
    private static final String TL_PUSH_KEY_LARGE_ICON = "tl_large_icon";


    /**
     * The listener that the client can implement for custom push notification data *
     */
    private List<TaplyticsPushNotificationListener> pushNotificationListeners = null;

    void setPushTokenListener(TaplyticsPushTokenListener pushTokenListener) {
        this.pushTokenListener = pushTokenListener;
        if (token != null && this.pushTokenListener != null) {
            this.pushTokenListener.pushTokenReceived(token);
        }
    }

    private TaplyticsPushTokenListener pushTokenListener = null;

    public void setPushOpenedListener(TaplyticsPushOpenedListener listener) {
        pushOpenedListener = listener;
    }

    public void removePushOpenedListener() {
        pushOpenedListener = null;
    }

    public TaplyticsPushOpenedListener getPushOpenedListener() {
        return pushOpenedListener;
    }

    private TaplyticsPushOpenedListener pushOpenedListener = null;


    /**
     * The listener for setting the push notification open intent
     */
    private TaplyticsPushNotificationIntentListener pushIntentListener = null;

    public TLPushManager(Map<String, Object> options) {
        this.pushNotificationListeners = new ArrayList<>();
        this.saveTokenAttempts = 0;
        try{
            Class.forName(FirebaseInstanceId.class.getName());
        } catch (Throwable t){
            TLKillSwitchManager.getInstance().disableFunction(Functionality.PUSH);
        }
        hasNewPush = TLManager.getInstance().hasImplementedNewPush();

        if (options != null){
            Object manualTokenRegistration = options.get("manualPushToken");
            this.manualTokenRegistration = manualTokenRegistration != null && (boolean) manualTokenRegistration;
        }
    }

    private int saveTokenAttempts;

    public String token = null;

     void getToken(boolean reset) {
        if ((checkedPushToken && !reset) || !TLManager.getInstance().getTrackingEnabled()
                || TLUtils.isDisabled(Functionality.PUSH)) {
            return;
        }

        try {
            JSONObject project = null;
            if (TLManager.getInstance().getTlProperties() != null) {
                project = TLManager.getInstance().getTlProperties().getProject();
            }
            if (project == null) {
                return;
            }

            JSONObject credentials = project.optJSONObject("credentials");
            if (credentials == null) {
                TLLog.debug("Project does not have push FCM credentials", true);
                return;
            }

            if (!manualTokenRegistration) {
                FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
                    @Override
                    public void onSuccess(InstanceIdResult instanceIdResult) {
                        TLLog.debug("token success: " + instanceIdResult.getToken());

                    }
                }).addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            TLLog.error("getInstanceId failed for getToken", task.getException());
                            return;
                        }
                        if (task.getResult() != null) {
                            String token = task.getResult().getToken();
                            // Get new Instance ID token
                            savePushtoken(token, false);
                        } else {
                            TLLog.error("No token");
                        }
                    }
                });
            }

        } catch (Exception e) {
            TLLog.error("Getting FCM Token", e);
        }
    }

    private void postToken(JSONObject token) {
        try {
            if (token == null || token.length() == 0 || token == JSONObject.NULL) {
                return;
            }

            JSONObject params = new JSONObject();
            TLLog.debug("Creating params");

            TLProperties properties = TLManager.getInstance().getTlProperties();
            if (properties == null) {
                return;
            }

            Map deviceDicMap = TLManager.getInstance().getTLDeviceInfo().getDeviceUniqueID();
            if (deviceDicMap != null) {
                params.put("ad", new JSONObject(deviceDicMap));
            }
            String project_id = properties.getProject() != null ? properties.getProject().optString("_id") : null;
            if (project_id != null) {
                params.put("pid", project_id);
            }
            if (TLManager.getInstance().getApiKey() != null) {
                params.put("t", TLManager.getInstance().getApiKey());
            }
            String appUser_id = (properties.getAppUser() != null) ? properties.getAppUser().optString("_id") : null;
            if (appUser_id != null) {
                params.put("auid", appUser_id);
            }
            String session_id = properties.getSessionID();
            if (session_id != null) {
                params.put("sid", session_id);
            }
            TLLog.debug("Posting FCM Token: " + token.optString("token"));
            TLLog.debug("Posting FCM Token, environment: " + token.optString("env"));

            params.put("pt", token.optString("token"));
            params.put("env", token.optString("env"));
            params.put("os", "Android");

            TLManager.getInstance().getTlNetworking().postGCMToken(params, new TLNetworking.TLNetworkResponseListener() {
                @Override
                public void onResponse(JSONObject response) {
                    TLLog.debug("Got Token Post Response!");
                }

                @Override
                public void onError(Throwable error) {
                    TLLog.error("Posting FCM Token", error);
                }
            });
        } catch (JSONException e) {
            TLLog.error("Posting FCM Token", e);
        }
    }

    public static void pushReceived(Intent intent) {
        if (!TLManager.getInstance().getTrackingEnabled() || TLUtils.isDisabled(Functionality.PUSH))
            return;

        TLLog.debug("Receiving push notification");
        try {
            if (TLManager.getInstance().getTlAnalytics() == null) {
                TLManager.getInstance().setupTLAnalytics();
            }
            TLManager.getInstance().getTlAnalytics().trackPushNotificationInteraction(intent.getExtras(), TLAnalyticsManager.PushEvent.RECEIVED);
        } catch (Throwable e) {
            TLLog.error("Tracking Push Received", e);
        }

    }

    public void savePushtoken(final String pushToken, boolean force) {
        if (pushToken == null || TLUtils.isDisabled(Functionality.PUSH)) return;
        JSONObject pushTokenJSON = new JSONObject();
        try {
            TLLog.debug("Saving token: "+pushToken);
            pushTokenJSON.put("token", pushToken);
            pushTokenJSON.put("env", TLManager.getInstance().isLiveUpdate() ? "sandbox" : "prod");

            if (TLManager.getInstance().getTlProperties() == null) {
                if (saveTokenAttempts < MAX_ATTEMPTS) {
                    saveTokenAttempts++;
                    TLManager.getInstance().getTlPropertiesPromise().add(new PromiseListener() {
                        @Override
                        public void succeeded() {
                            savePushtoken(pushToken, false);
                            super.succeeded();
                        }
                    });

                } else {
                    TLLog.error("Saving push token failed after " + saveTokenAttempts + " attempts, no TL properties!");
                }

                return;
            }

            JSONObject user = TLManager.getInstance().getTlProperties().getAppUser();
            if (user == null) {
                return;
            }

            JSONArray devices = user.optJSONArray("deviceInfo");
            if (devices == null) {
                return;
            }
            boolean hasToken = false;

            for (int i = 0; i < devices.length(); i++) {
                JSONObject device = devices.getJSONObject(i);

                String deviceToken;
                if (pushTokenJSON.optString("env").equals("sandbox")) {
                    deviceToken = device.optString("pushTokenSandbox");
                    if (deviceToken != null && pushTokenJSON.optString("token").equals(deviceToken)) {
                        hasToken = true;
                    }
                }
                if (pushTokenJSON.optString("env").equals("prod")) {
                    deviceToken = device.optString("pushToken");
                    if (deviceToken != null && pushTokenJSON.optString("token").equals(deviceToken)) {
                        hasToken = true;
                    }
                }

            }
            TLLog.debug("Saved push token: " + pushTokenJSON.optString("token"), true);

            if (pushTokenJSON.has("token")) {
                token = pushTokenJSON.optString("token");
            }

            if (pushTokenListener != null) {
                pushTokenListener.pushTokenReceived(pushTokenJSON.optString("token"));
            }

            if (!hasToken || force) {
                postToken(pushTokenJSON);
            }

        } catch (Exception e) {
            TLLog.error("Checking FCM Push Token", e);
        } finally {
            checkedPushToken = true;
        }
    }

    void logAndCopyToken(){
        try {
            if (TLUtils.isDisabled(Functionality.PUSH)) {
                return;
            }
            FirebaseInstanceId.getInstance().getInstanceId()
                    .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                        @Override
                        public void onComplete(@NonNull Task<InstanceIdResult> task) {
                            if (!task.isSuccessful()) {
                                TLLog.warning("get token getInstanceId failed", task.getException());
                                return;
                            }

                            if (task.getResult() != null) {
                                // Get new Instance ID token
                                String token = task.getResult().getToken();
                                Toast.makeText(TLManager.getInstance().getCurrentActivity(), "Taplytics FCM Token Copied to Clipboard: \n\n" + token, Toast.LENGTH_SHORT).show();
                                Log.d("Taplytics", "Taplytics FCM Token copied to clipboard: \n\n" + token);
                                try {
                                    final android.content.ClipboardManager clipboardManager = (ClipboardManager) TLManager.getInstance().getAppContext().getSystemService(Application.CLIPBOARD_SERVICE);
                                    ClipData clipData = ClipData.newPlainText("Source Text", token);
                                    clipboardManager.setPrimaryClip(clipData);
                                } catch (Throwable t) {
                                    TLLog.error("Error copying token", t);
                                }

                            } else {
                                TLLog.warning("Token result is null, please ensure push is set up on your project or contact support");
                                Toast.makeText(TLManager.getInstance().getCurrentActivity(), "Token result is null, please ensure push is set up on your project or contact support", Toast.LENGTH_SHORT).show();

                            }
                        }
                    });
        } catch (Throwable t){
            TLLog.error("Firebase not available", t);

        }
    }

    /**
     * Build the push notification and then send it to the device
     *
     * @param extras           the extras attached to the intent. Contains the message, as well as tl_id and custom data
     * @throws JSONException
     */
    public void sendNotification(Bundle extras, Context context) throws JSONException {
        if (TLUtils.isDisabled(Functionality.PUSH)) {
            return;
        }

        String msg = extras.getString("message");
        String imageUrl = extras.getString("image_url");
        Bitmap pushImage = null;
        boolean useImageAsIcon = true;
        boolean showLargeIcon = false;

        if (imageUrl != null) {
            try {
                String encodedURL = URLEncoder.encode(imageUrl, "utf-8");
                File pushImageFile = ImageUtils.saveImageToDisk(imageUrl, encodedURL);
                pushImage = BitmapFactory.decodeFile(pushImageFile.getAbsolutePath());
                try {
                    pushImageFile.delete();
                } catch (Throwable t) {
                    TLLog.error("Error deleting push image", t);
                }
            } catch (Throwable t) {
                TLLog.error("Error parsing push image", t);
                Log.w("Taplytics", "Error parsing push image. " + (msg != null ? "Building push with message only." : ""));
                pushImage = null;
                if (msg == null) {
                    return;
                }
            }
        }

        String title = extras.getString("title");;
        int priority = NotificationCompat.PRIORITY_DEFAULT;
        JSONObject customData = new JSONObject();
        if (extras.getString("custom_keys") != null) {
            customData = new JSONObject(extras.getString("custom_keys"));
        }

        sendDataToPushListeners(customData);

        NotificationManager mNotificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        try {
            PackageManager pm = TLManager.getInstance().getAppContext().getPackageManager();
            TLLog.debug("Getting App Context and package name");

            Context appContext = TLManager.getInstance().getAppContext();
            if (appContext == null) {
                TLLog.debug("App Context is null");
                return;
            }
            String packageName = appContext.getPackageName();
            if (packageName == null) {
                TLLog.debug("package name is null");
                return;
            }

            Intent intent = null;

            if (customData.has("tl_activity")) {
                try {
                    String classname = customData.getString("tl_activity");
                    final Activity currentActivity = TLManager.getInstance().getCurrentActivity();
                    if (currentActivity != null) {
                        intent = new Intent(currentActivity, Class.forName(classname));
                        intent.putExtras(extras);
                    }
                    TLLog.debug("Built launch intent with custom activity");
                } catch (Exception e) {
                    TLLog.warning("Problem creating push", e);
                }
            }
            if (pushIntentListener != null) {
                intent = pushIntentListener.setPushNotificationIntent(customData);
            }

            if (customData.has("tl_title")) {
                try {
                    title = customData.getString("tl_title");
                } catch (Exception e) {
                    TLLog.error("Error getting title");
                }
            }

            if (customData.has("tl_priority")) {
                try {
                    priority = customData.getInt("tl_priority");
                } catch (Exception e) {
                    TLLog.error("Error getting priority");
                }
            }

            if (customData.has("tl_silent")) {
                try {
                    if (customData.optBoolean("tl_silent")) {
                        return;
                    }
                } catch (Exception e) {
                    TLLog.error("Error getting silent");
                }
            }

            if (customData.has(TL_PUSH_KEY_IMAGE_ICON)) {
                try {
                    useImageAsIcon = customData.optBoolean(TL_PUSH_KEY_IMAGE_ICON, true);
                } catch (Exception e) {
                    TLLog.error("Error getting push icon boolean");
                }
            }

            if (customData.has(TL_PUSH_KEY_LARGE_ICON)) {
                try {
                    showLargeIcon = customData.optBoolean(TL_PUSH_KEY_LARGE_ICON, false);
                } catch (Exception e) {
                    TLLog.error("Error getting large icon boolean");
                }
            }

            if (intent == null) {
                if (hasNewPush) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        intent = new Intent(appContext, TLManager.getInstance().getPushBroadcastReceiver());
                    } else {
                        intent = new Intent(OPEN_ACTION);
                    }
                } else {
                    intent = pm.getLaunchIntentForPackage(packageName);
                    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    if (intent == null) {
                        TLLog.debug("Launch intent is null");
                        return;
                    }
                }
                TLLog.debug("Built launch intent");
            }

            if (extras.getString("custom_keys") != null) {
                intent.putExtra("custom_keys", extras.getString("custom_keys"));
            }

            int notificationId = (new Random()).nextInt();
            intent.putExtra("tl_notif", true);
            intent.putExtra("tl_id", extras.getString("tl_id"));
            if (hasNewPush) {
                intent.putExtra("tl_receiver", true);
                intent.setAction(OPEN_ACTION);
            }
            if (pushImage != null) {
                intent.putExtra("tl_image", 1);
            }

            PendingIntent contentIntent;

            if (hasNewPush) {
                contentIntent = PendingIntent.getBroadcast(context, notificationId,
                        intent, PendingIntent.FLAG_UPDATE_CURRENT);
            } else {
                contentIntent = PendingIntent.getActivity(context, notificationId,
                        intent, PendingIntent.FLAG_UPDATE_CURRENT);
            }

            Intent deleteIntent = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                deleteIntent = new Intent(appContext, TLManager.getInstance().getPushBroadcastReceiver());
            } else {
                deleteIntent = new Intent(DISMISS_ACTION);
            }

            if (hasNewPush) {
                deleteIntent.putExtra("tl_receiver", true);
                deleteIntent.setAction(DISMISS_ACTION);
            }

            PendingIntent deletePendingIntent = PendingIntent.getBroadcast(context, notificationId, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            Bitmap largeIcon;

            Icon smallIcon = null;

            //Use the image provided as the actual small icon for the push notification.
            if (useImageAsIcon && pushImage != null) {
                /*
                 *  Because android's default cropping is so silly, we will actually only RE-SIZE the image if the width is over 1.5x the height.
                 * Otherwise, the image will actually look ok with Android's default cropping so we wont touch it.
                 * */
                if (pushImage.getWidth() > 1.5 * pushImage.getHeight()) {
                    try {
                        largeIcon = Bitmap.createScaledBitmap(pushImage, pushImage.getHeight(), pushImage.getHeight(), false);
                    } catch (Throwable e) {
                        //Just in case memory problems happen or certain android versions have bitmap issues.
                        largeIcon = pushImage;
                    }
                } else {
                    largeIcon = pushImage;
                }
            } else if (!showLargeIcon) {
                largeIcon = null;
            } else {
                try {
                    Drawable icon = pm.getApplicationIcon(TLManager.getInstance().getAppContext().getApplicationInfo());
                    int width = icon.getIntrinsicWidth();
                    int height = icon.getIntrinsicHeight();
                    largeIcon = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(largeIcon);
                    icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                    icon.draw(canvas);
                } catch (Exception e) {
                    Drawable icon = pm.getApplicationIcon(packageName);
                    BitmapDrawable icon_bd = (BitmapDrawable) icon;
                    largeIcon = icon_bd.getBitmap();
                }

                if (Build.VERSION.SDK_INT >= 23) {
                    TLLog.debug("Using small icon from large icon");
                    smallIcon = Icon.createWithBitmap(largeIcon);
                }

            }

            ApplicationInfo appInfo = pm.getApplicationInfo(
                    TLManager.getInstance().getAppContext().getPackageName(), PackageManager.GET_META_DATA);

            Resources resources = pm.getResourcesForApplication(appInfo);

            int smallIconId;
            this.appIcon = pm.getApplicationInfo(packageName, 0).icon;

            if (appInfo != null && appInfo.metaData != null && appInfo.metaData.getInt("com.taplytics.sdk.notification_icon") != 0) {
                smallIconId = appInfo.metaData.getInt("com.taplytics.sdk.notification_icon");
            } else {
                smallIconId = appIcon;
            }
            TLLog.debug("Building notification", true);

            Notification notification = getNotification(pushImage, msg,
                    mNotificationManager, largeIcon, smallIconId,
                    smallIcon, appContext, pm, resources,
                    deletePendingIntent, priority, title,
                    contentIntent);

            notification.flags |= Notification.FLAG_AUTO_CANCEL;
            mNotificationManager.notify(notificationId, notification);

        } catch (Throwable e) {
            TLLog.error("Sending Push", e);
        }
    }

    private Notification getNotification(Bitmap pushImage, String msg,
                                         NotificationManager mNotificationManager, Bitmap bitmap,
                                         int smallIconId, Icon smallIcon, final Context appContext, PackageManager pm, Resources resources,
                                         PendingIntent deletePendingIntent, int priority,
                                         String title, PendingIntent contentIntent) {

        // No Multiline support for API < 16.
        if (Build.VERSION.SDK_INT < 16) {

            return buildCompatNotification(pushImage, msg,
                    mNotificationManager, bitmap,
                    smallIconId, appContext, pm,
                    deletePendingIntent, priority, title,
                    contentIntent);

        } else {

            return buildNotification(pushImage, msg, mNotificationManager, bitmap,
                    smallIconId, smallIcon, appContext, pm, resources,
                    deletePendingIntent, priority, title,
                    contentIntent);

        }
    }

    public void processReceivedMessage(RemoteMessage message, Context c){
        if(TLUtils.isDisabled(Functionality.PUSH)){
            return;
        }

        if (message == null)
            return;

        //Get all of the data contained in the message object
        Map<String, String> data = message.getData();


        Bundle bundle = new Bundle();
        //Depending on where the message comes from and what version of android is running
        //The data might be in either a notification object or just the data object.
        RemoteMessage.Notification notification = message.getNotification();


        if(notification != null) {
            bundle.putString("message", notification.getBody());
            if(notification.getTitle() != null) {
                data.put("tl_title", notification.getTitle());
            }
            bundle.putString("custom_keys", data.toString());
        } else {
            //Put it all into a bundle which is what we're expecting
            for(String key : data.keySet()){
                bundle.putString(key, data.get(key));
            }
        }

        if (data.get("tl_id") != null) {
            try {
                TLManager.getInstance().getTlPushManager().sendNotification(bundle, c);
            } catch(JSONException ex) {
                TLLog.error("Sending FCM push notification", ex);
            }
        } else {
            TLLog.debug("Ignoring - Not a Taplytics push");
        }
    }

    private Notification buildCompatNotification(Bitmap pushImage, String msg,
                                                 NotificationManager mNotificationManager, Bitmap bitmap,
                                                 int smallIconId, final Context appContext, PackageManager pm,
                                                 PendingIntent deletePendingIntent, int priority,
                                                 String title, PendingIntent contentIntent) {

        //Swap notification style depending on whether we have an image or not.
        NotificationCompat.Style notificationStyle;
        if (pushImage != null) {
            notificationStyle = new NotificationCompat.BigPictureStyle().bigPicture(pushImage).setSummaryText(msg).bigLargeIcon(null);
        } else {
            notificationStyle = new NotificationCompat.BigTextStyle().bigText(msg);
        }

        NotificationCompat.Builder mBuilder = getNotificationCompatBuilder(mNotificationManager);

        mBuilder.setAutoCancel(true)
                .setLargeIcon(bitmap)
                .setSmallIcon(smallIconId)
                .setContentTitle(appContext.getApplicationInfo().loadLabel(pm))
                .setStyle(notificationStyle)
                .setContentText(msg)
                .setDeleteIntent(deletePendingIntent)
                .setDefaults(-1).setAutoCancel(true).setPriority(priority);

        if (title != null) {
            mBuilder.setContentTitle(title);
        }

        mBuilder.setContentIntent(contentIntent);
        return mBuilder.build();

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private Notification buildNotification(final Bitmap pushImage, String msg,
                                           NotificationManager mNotificationManager, final Bitmap bitmap, int smallIconId, Icon smallIcon, final Context appContext,
                                           PackageManager pm, Resources resources, PendingIntent deletePendingIntent,
                                           int priority, String title, PendingIntent contentIntent) {

        //Get the builder for the notification.
        // Not sure if possible for it to fail completely, just returning null in that case.
        Notification.Builder mBuilder;
        try {
            mBuilder = getAndroidOBuilder(mNotificationManager);
        } catch (Throwable e) {
            mBuilder = new Notification.Builder(TLManager.getInstance().getAppContext());
        }

        if (mBuilder == null) {
            TLLog.error("Notif builder null");
            return null;
        }

        mBuilder.setAutoCancel(true)
                .setLargeIcon(bitmap)
                .setContentTitle(appContext.getApplicationInfo().loadLabel(pm))
                .setContentText(msg)
                .setDeleteIntent(deletePendingIntent)
                .setDefaults(-1).setAutoCancel(true).setPriority(priority);

        // If API < 17 we cannot timestamp.
        if (Build.VERSION.SDK_INT >= 17) {
            mBuilder.setShowWhen(true);
        }

        if (title != null) {
            mBuilder.setContentTitle(title);
        }

        if (pushImage != null) {

                /*
                    To modify layout of default notification to add multiline support we must modify
                    default behavior of Android's getStandardView method.
                */
            Notification.BigPictureStyle bigPictureStyle = new Notification.BigPictureStyle() {
                @Override
                protected RemoteViews getStandardView(int layoutId) {
                    final RemoteViews remoteViews = super.getStandardView(layoutId);
                    try {
                        int id = Resources.getSystem().getIdentifier("text", "id", "android");

                        remoteViews.setBoolean(id, "setSingleLine", false);

                    } catch (Throwable e) {
                        TLLog.error("Modifying layout of notification: ", e);
                    }
                    return remoteViews;
                }

                @Override
                public Notification.BigPictureStyle bigLargeIcon(Bitmap b) {
                    return super.bigLargeIcon((Bitmap) null);
                }

            };

            Notification.Style notificationStyle = bigPictureStyle.bigPicture(pushImage).setSummaryText(msg).bigLargeIcon((Bitmap) null);
            mBuilder.setStyle(notificationStyle);
            mBuilder.setLargeIcon(bitmap);

        } else {

            Notification.BigTextStyle bigTextStyle = new Notification.BigTextStyle();
            Notification.Style notificationStyle = bigTextStyle.bigText(msg);
            mBuilder.setStyle(notificationStyle);

        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M & smallIcon != null) {
            mBuilder.setSmallIcon(smallIcon);
        } else if (checkIconResourceExists(resources, smallIconId)) {
            mBuilder.setSmallIcon(smallIconId);
        } else if (checkIconResourceExists(resources, this.appIcon)) {
            mBuilder.setSmallIcon(appIcon);
        }

        /*
            We cannot reach getStandardView method through default calls on API>=24.
            So we call create BigContentView, which explicitly calls getStandardView.
        */
        if (Build.VERSION.SDK_INT >= 24) {

            try {
                RemoteViews remoteView = mBuilder.createBigContentView();
                if (remoteView != null) {

                    mBuilder.setCustomBigContentView(remoteView);

                }
            } catch (Throwable e) {
                TLLog.error("Modifying layout of notification: ", e);
            }
        }


        mBuilder.setContentIntent(contentIntent);

        return mBuilder.build();
    }

    /**
     * Set and create a notification channel for this app
     *
     * @param mNotificationManager TODO: Allow for creating new channels.
     */
    private void setAndCreateChannel(NotificationManager mNotificationManager) {
        // Configure the notification channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(DEFAULT_CHANNEL_ID, DEFAULT_CHANNEL_NAME, DEFAULT_IMPORTANCE);
            mChannel.setDescription(DEFAULT_CHANNEL_DESCRIPTION);
            mNotificationManager.createNotificationChannel(mChannel);
        }
    }

    /**
     * Create builder with channel on base android libs
     */
    private Notification.Builder getAndroidOBuilder(NotificationManager mNotificationManager) {
        setAndCreateChannel(mNotificationManager);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return new Notification.Builder(TLManager.getInstance().getAppContext(), DEFAULT_CHANNEL_ID);
            }
        } catch (Throwable t) {
            TLLog.error("Error creating android O notif builder with channel ID: " + DEFAULT_CHANNEL_ID + ". Reverting normal builder.");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            return new Notification.Builder(TLManager.getInstance().getAppContext());
        }
        return null;
    }

    /**
     * Create builder with channel on compat libs
     */
    @NonNull
    private NotificationCompat.Builder getAndroidOCompatBuilder(NotificationManager mNotificationManager) {
        setAndCreateChannel(mNotificationManager);
        try {

            return new NotificationCompat.Builder(TLManager.getInstance().getAppContext(), DEFAULT_CHANNEL_ID);
        } catch (Throwable t) {
            TLLog.error("Error creating android O Compat notif builder with channel ID: " + DEFAULT_CHANNEL_ID + ". Reverting to normal builder.");
            return new NotificationCompat.Builder(TLManager.getInstance().getAppContext());
        }
    }

    /**
     * @return correct notification builder depending on Android version.
     */
    private NotificationCompat.Builder getNotificationCompatBuilder(NotificationManager mNotificationManager) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            return getAndroidOCompatBuilder(mNotificationManager);
        } else {
            return new NotificationCompat.Builder(TLManager.getInstance().getAppContext());
        }
    }

    /**
     * The interface through which to set the notification listeners
     *
     * @param pushListener the listener to add
     */
    public void addPushNotificationListener(TaplyticsPushNotificationListener pushListener) {
        if (this.pushNotificationListeners != null && !pushNotificationListeners.contains(pushListener)) {
            this.pushNotificationListeners.add(pushListener);
        }
    }

    /**
     * The interface through which to remove the notification listeners
     *
     * @param pushListener the listener to remove
     */
    public void removePushNotificationListener(TaplyticsPushNotificationListener pushListener) {
        if (this.pushNotificationListeners != null && pushListener != null) {
            this.pushNotificationListeners.remove(pushListener);
        }
    }

    /**
     * Loops through all the push listeners and sends data to them
     *
     * @param customData the custom data that is sent to the listener
     * @throws JSONException
     */
    private void sendDataToPushListeners(JSONObject customData) throws JSONException {
        if (this.pushNotificationListeners != null) {
            for (TaplyticsPushNotificationListener listener : this.pushNotificationListeners) {
                listener.pushReceived(customData);
            }
        }
    }

    /**
     * Sets the push intent listener
     *
     * @param listener the listener that will be set
     */
    public void setPushIntentListener(TaplyticsPushNotificationIntentListener listener) {
        if (listener != null) {
            this.pushIntentListener = listener;
        }
    }

    boolean hasCheckedPushToken() {
        return checkedPushToken;
    }

    void clearListeners() {
        pushIntentListener = null;
        if (pushNotificationListeners != null) {
            pushNotificationListeners.clear();
        }

        pushOpenedListener = null;

    }

    public static void pushOpen(Intent intent) {
        try {
            if (TLManager.getInstance().getTlAnalytics() == null) {
                TLManager.getInstance().setupTLAnalytics();
            }
            TLManager.getInstance().getTlAnalytics().trackPushNotificationInteraction(intent.getExtras(), TLAnalyticsManager.PushEvent.OPENED);
        } catch (Throwable e) {
            TLLog.error("Tracking Push Opened", e);
        }
    }

    public static void pushDismissed(Intent intent) {
        try {
            if (TLManager.getInstance().getTlAnalytics() == null) {
                TLManager.getInstance().setupTLAnalytics();
            }
            TLManager.getInstance().getTlAnalytics().trackPushNotificationInteraction(intent.getExtras(), TLAnalyticsManager.PushEvent.DISMISSED);
        } catch (Throwable e) {
            TLLog.error("Tracking Push Dismissed", e);
        }
    }

    void deleteAndRenewToken() {
        if (TLUtils.isDisabled(Functionality.PUSH)) {
            return;
        }
        Toast.makeText(TLManager.getInstance().getCurrentActivity(), "Taplytics FCM Token to be deleted: \n\n" + token, Toast.LENGTH_SHORT).show();
        Log.d("Taplytics", "Taplytics FCM Token to be deleted: \n\n" + token);
        new DeleteTokenTask().execute();

    }

    void forceSavePushToken() {
        Toast.makeText(TLManager.getInstance().getCurrentActivity(), "FORCING a save of the following Token: \n\n" + token, Toast.LENGTH_SHORT).show();
        Log.d("Taplytics", "Taplytics FCM Token to be saved: \n\n" + token);        savePushtoken(this.token, true);
    }

    static class DeleteTokenTask extends AsyncTask<Void, Void, Void>{

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                FirebaseInstanceId.getInstance().deleteInstanceId();

            } catch (Throwable t) {
                TLLog.error("Deleting token", t);
            }
            return null;
        }

         @Override
         protected void onPostExecute(Void aVoid) {
             TLManager.getInstance().getTlPushManager().logAndCopyToken();
             super.onPostExecute(aVoid);
         }
    }

    private boolean checkIconResourceExists(Resources resources, int resId) {
        boolean foundIcon = false;

        try {
            foundIcon = resources.getDrawable(resId, null) != null;
        } catch (Exception e) {
            TLLog.error("Error getting small icon resource", e);
        }
        return foundIcon;
    }

}
