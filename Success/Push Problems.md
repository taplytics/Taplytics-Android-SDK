# Push Problems

If someone comes to you with an issue with push notifications, theres a few steps you can take to quickly check if things are set up.

## Basic Setup Issues:

First and foremost, take their APK (from apkpure.com if necessary, or ask for it) and plop it here:

[APK Decompiler](http://www.javadecompilers.com/apk)

Then open their **AndroidManifest.xml** You should be able to find the following:

[Talytics Push Step 1](https://github.com/taplytics/Taplytics-Android-SDK/blob/master/PUSH.md#1-setup)

Essentially, you're looking for the correct **permission** tags (generally always present), and then you're looking for a **RECEIVER** that contains these Intent filters:

```xml
<intent-filter>
    <action android:name="com.google.android.c2dm.intent.RECEIVE" />
</intent-filter>

<intent-filter>
     <action android:name="taplytics.push.OPEN" />
     <action android:name="taplytics.push.DISMISS" />
</intent-filter>
```

Also note that they need our **Intent Service** present:

` <service android:name="com.taplytics.sdk.TLGcmIntentService" />`

## "But I use Firebase / FCM"

Thats fine. This is google's new push management system and at the moment we still use the old one. What this means is that they MUST have the following in their **build.gradle**:

`compile("com.google.android.gms:play-services-gcm:9.+")`

This allows them to extend the required **BroadcastReceiver** (TLGcmBroadcastReceiver/ TLGcmNonWakefulBroadcastReceiver)

## BroadcastReceiver contents

After all of that, make sure that their broadcastreceiver implementation extends one of the receivers above, and most importantly, that it calls `super.pushOpened()` in the pushOpened call, as this is how we track it. 






