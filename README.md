#Taplytics-Android SDK

_Taplytics is a native mobile A/B testing platform that helps you optimize your Android app!_

**Current Version**: [1.4.10](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.4.10)

## Getting Started

_How do I, as a developer, start using Taplytics?_ 

1. _Sign up for a free account at Taplytics.com._
2. _Install the SDK._
3. _Create an experiment and push it live to your users!_

Read more on how to get started with Taplytics's Android SDK [here](/START.md).

###Push Notifications
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

Now, just follow the docs on [Taplytics](https://taplytics.com/docs/push-notifications/google-push-certificates) to get started with Push Notifications in your application!

###Push Notifications in Eclipse

An extra step is needed for Push Notifications in Eclipse. You must add the Google Play Services library to your project's dependencies. 

To do so, follow the steps in the following link. Click on the `Using Android Studio` dropdown and choose `Using Eclipse with ADT`.

[See how to set up GooglePlay Services on developer.android.com](http://developer.android.com/google/play-services/setup.html). 

Once Google Play Services is added to your application in Eclipse, follow the usual steps listed above to get started with Push on Android with Taplytics!

###Resetting Users

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


###Get Running Experiments and Variations

If you would like to see which variations and experiments are running on the device, there exists a `getRunningExperimentsAndVariations(TaplyticsRunningExperimentsListener listener)` function which provides a callback with a map of the current experiments and their running variation. An example:

```java
  Taplytics.getRunningExperimentsAndVariations(new TaplyticsRunningExperimentsListener() {
                    @Override
                    public void runningExperimentsAndVariation(Map<String, String> experimentsAndVariations) {
                        //TODO: Do something with the map.
                    }
                });
```

**NOTE:**This function runs asynchronously, as it waits for the updated properties to load from Taplytics' servers before returning the running experiments. 
