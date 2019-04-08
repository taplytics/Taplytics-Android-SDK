## Overview

With the push for Google to start using FCM from GCM, this doc is to help ease the transition from Taplytics push implementation using GCM to FCM.

## First and Foremost

Make sure you have added your app to Firebase, instructions can be found [here](https://firebase.google.com/docs/android/setup).

## Deleting old implementation code

From your `AndroidManifest.xml` you can delete the following snippets that were needed before:

```
<uses-permission android:name="com.google.android.c2dm.permission.RECEIVE"/>
<permission android:name="com.yourpackagename.permission.C2D_MESSAGE
"/>
<uses-permission android:name="com.yourpackagename.permission.C2D_ME
SSAGE"/>
<uses-permission android:name="android.permission.WAKE_LOCK"/>
```

and also the intent service:

```
<service android:name="com.taplytics.sdk.TLGcmIntentService"/>
```

## Add Firebase libraries

In your app `build.gradle` dependencies add the following:

```
implementation 'com.google.firebase:firebase-messaging:17.+'
implementation 'com.google.firebase:firebase-core:16.0.8'
```

Add the following to the end of your `build.gradle` file if you haven't already: 

```
apply plugin: 'com.google.gms.google-services'
```

## Add New FirebaseMessagingService

Then you can add under your `<application>` tag in your `AndroidManifest` file the following service:

```
<service android:name="com.taplytics.sdk.fcm.TLFirebaseMessagingService">
  <intent-filter>
     <action android:name="com.google.firebase.MESSAGING_EVENT" />
  </intent-filter>
</service>
```

## Other notes

- Keep in mind Firebase is supported only in API level 14 and up
- Minimum version of firebase-messaging support is `17.0.0`



