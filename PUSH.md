Setting up Push Notifications using Taplytics is simple. Follow the steps below to get started.

| # | Step |
| - | ----------------- |
| 1 | Setup: [Android Studio](#android-studio), [Eclipse](#eclipse) |
| 2 | [Receiving Push Notifications](#1-receiving-push-notifictions) |
| 3 | [Resetting Users](#2-resetting-users) |


## 1. Setup

### Android Studio

If you wish to use Push Notifications on Taplytics, you must add the following permissions (replace `com.yourpackagename` with your app's package name) to your Android Manifest:

```xml
<uses-permission android:name="com.google.android.c2dm.permission.RECEIVE" />
<permission android:name="com.yourpackagename.permission.C2D_MESSAGE"/>
<uses-permission android:name="com.yourpackagename.permission.C2D_MESSAGE" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
```
And you must add the following receiver and service under your application tag:

```xml
<receiver
    android:name="com.taplytics.sdk.TLGcmBroadcastReceiver"
    android:permission="com.google.android.c2dm.permission.SEND" >
    <intent-filter>
        <action android:name="com.google.android.c2dm.intent.RECEIVE" />
    </intent-filter>
</receiver>
<service android:name="com.taplytics.sdk.TLGcmIntentService" />
```

---

### Eclipse

An extra step is needed for Push Notifications in Eclipse. You must add the Google Play Services library to your project's dependencies. 

To do so, follow the steps in the following link. Click on the `Using Android Studio` dropdown and choose `Using Eclipse with ADT`.

[See how to set up GooglePlay Services on developer.android.com](http://developer.android.com/google/play-services/setup.html).

Once Google Play Services is added to your application in Eclipse, follow the steps [listed above](#android-studio) to get started with Push on Android with Taplytics!

---

## 2. Receiving Push Notifictions

In order to be able to send your users Push Notifications, we'll need you to upload your Google Cloud Messaging credentials. Please follow [this guide](https://taplytics.com/docs/guides/push-notifications/google-push-certificates) to do so.

### Activity Routing

By default, when a notification sent by Taplytics is clicked, it will open up the main activity of the application. However, it may be desirable to route your users to a different Activity. This can be done on the Taplytics Push page.

Simply add a custom data value to the push with the key `tl_activity` and with the **full** (_including package name_) class name of your activity. For example:


![image](http://staging.taplytics.com/assets/docs/push/push_activity.png)

---

## 3. Resetting Users

Sometimes, it may be useful to reset an app user for push notifications. For instance, if a user is logged out in your app, it may be desirable for them to no longer receive push notifications. If you wish to turn off push notifications for an app user, it can be done as such:

```java
TaplyticsResetUserListener listener = new TaplyticsResetUserListener() {
  @Override
  public void finishedResettingUser() {
    // Do stuff
  }
};

Taplytics.resetAppUser(listener);
```

Now, the device that the app is currently running on will no longer receive push notifications until the app user attributes are updated again.
