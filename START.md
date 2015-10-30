IYou can get started with using Taplytics on Android in minutes. Just follow the steps below:

|#  |Step                                                                       |
|---|---                                                                        |
|1  |  Installation: [Android Studio](#android-studio), or [Eclipse](#eclipse)     |
|2  | [Initialize](#2-initialization) SDK                                       |
|3  | [Setting User Attributes](#3-user-attributes) (optional)                  |
|4  | [Tracking Events](#4-track-events) (optional)                             |

You can use Taplytics to create [Experiments](https://taplytics.com/docs/android-sdk/experiments) as well as send [Push Notifications](https://taplytics.com/docs/android-sdk/push-notifications) to your app.

## 1. Installation

### Android Studio

1. _In your module’s build.gradle, add the url to the sdk._

    ```
    repositories {                                                                                              
        maven { url "https://github.com/taplytics/Taplytics-Android-SDK/raw/master/AndroidStudio/" }
    }
    ```
    
2. _In your *module’s* build.gradle dependencies (not your project's build.gradle), compile Taplytics and its dependencies._

    ```
    dependencies {                                                                   
        //Taplytics                                                                        
        compile("com.taplytics.sdk:taplytics:+@aar")  
        
        //Dependencies for taplytics
        compile("com.mcxiaoke.volley:library:+")
     
        //Excluding org.json due to compiler warnings
        //socket.io connections only made on debug devices OR if making live changes to a release build.
        //No socket.io connection will be made on your release devices unless explicitly told to do so. 
        compile("com.github.nkzawa:socket.io-client:+") {
                exclude group: 'org.json'
        }
        compile("com.github.nkzawa:engine.io-client:+") {
                exclude group: 'org.json'
        }
        
        //Only include this if you wish to enable push notifications:
        compile("com.google.android.gms:play-services-gcm:8.1.0")
    }    
    ```
    
3. _Override your Application’s onCreate() method (not your main activity) and call Taplytics.startTaplytics(). If you don't have an Application class, create one. It should look like this:_

    ```java
    public class ExampleApplication extends Application {
        @Override
        public void onCreate() {
            super.onCreate();
            Taplytics.startTaplytics(this, "YOUR TAPLYTICS API KEY");
        }
    }
    ```

4. _Now, add the proper permissions, and the Application class to your app’s AndroidManifest.xml in the Application tag._

     ```xml
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <application
        android:name=".ExampleApplication"
    ...
    ```

5. _Finally, add the following intent-filter tag to the end of your *MAIN* activity:_
    
    First, [get your Taplytics URL Scheme from your Project's Settings](https://taplytics.com/dashboard):

    ![image](https://taplytics.com/assets/docs/install-sdk/url-scheme.png)
    
    Then, add it to your manifest:

    ```xml
        ...
             <intent-filter>
                    <action android:name="android.intent.action.VIEW"/>
                    <category android:name="android.intent.category.DEFAULT"/>
                    <category android:name="android.intent.category.BROWSABLE"/>
                    <data android:scheme="YOUR URL SCHEME"/>
            </intent-filter>
    </activity>
    ```

6. _Add the following to your Proguard rules:_
    (Only if using Support Fragments)

    ```
    -keep class android.support.v4.app.Fragment { *; }
    -keep class android.support.v4.view.ViewPager
    -keepclassmembers class android.support.v4.view.ViewPager$LayoutParams {*;}
    ```

    (Only if using Mixpanel)
    
    ```
    -keep class com.mixpanel.android.mpmetrics.MixpanelAPI { *;}
    ```

    (Only if using Flurry)
    
    ```
    -keep class com.flurry.android.FlurryAgent { *; }
    ```
        
    (Only if you see gradle compiler errors with com.okio)

    ```
    -dontwarn okio.**
    -dontwarn java.nio.file.*
    -dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
    ```
	(Only if you are seeing proguard issues with com.google.android.gms)
	
	```
	-dontwarn com.taplytics.**
	```

7. _That's it! Now build and run your app, you can start creating experiments with Taplytics!_

---

### Eclipse

1. _Download the taplytics.jar [here](https://github.com/taplytics/Taplytics-Android-SDK/raw/master/taplytics.jar)_
2. _Copy the jar into your 'libs' directory in your project._
3. _Right click the jar in Eclipse, click Build Path > add to build path_
4. **NEW:** _Add Google Play Services to your project by following the steps listed [here.](http://developer.android.com/google/play-services/setup.html) Be sure to change the dropdown to "Eclipse with ADT"_
5. _Override your application’s onCreate() method (not your main activity) and call Taplytics.startTaplytics(). It should look like this:_

    ```java
    public class ExampleApplication extends Application {
        @Override
        public void onCreate() {
            super.onCreate();
            Taplytics.startTaplytics(this, "YOUR TAPLYTICS API KEY");
        }
    }
    ```

6. _Add the proper permissions, and the Application class to your app’s AndroidManifest.xml in the Application tag._

    ```xml
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <application
        android:name=".ExampleApplication"
        ...
    ```

7. _Finally, add the following intent-filter tag to the end of your *MAIN* activity:_
    
    First, [get your Taplytics URL Scheme from your Project's Settings](https://taplytics.com/dashboard):

    ![image](https://taplytics.com/assets/docs/install-sdk/url-scheme.png)
    
    Then, add it to your manifest:

    ```xml
    ...
             <intent-filter>
                    <action android:name="android.intent.action.VIEW"/>
                    <category android:name="android.intent.category.DEFAULT"/>
                    <category android:name="android.intent.category.BROWSABLE"/>
                    <data android:scheme="YOUR URL SCHEME"/>
            </intent-filter>
    </activity>
    ```

8. _Add the following to your Proguard rules:_
    
    (Only if using Support Fragments)

    ```
    -keep class android.support.v4.app.Fragment { *; }
    -keep class android.support.v4.view.ViewPager
    -keepclassmembers class android.support.v4.view.ViewPager$LayoutParams {*;}
    ```

    (Only if using Mixpanel)
    
    ```
    -keep class com.mixpanel.android.mpmetrics.MixpanelAPI { *;}
    ```

    (Only if using Flurry)
    
    ```
    -keep class com.flurry.android.FlurryAgent { *; }
    ```
    
    (Only if you see gradle compiler errors with com.okio or com.nio)

    ```
    -dontwarn okio.**
    -dontwarn java.nio.file.*
    -dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
    ```

9. _That's it! Now build and run your app, you can start creating experiments with Taplytics!_

---

### Segment

The Taplytics SDK can also be installed via Segment. You can find install instructions [here](https://taplytics.com/docs/segment-integration)

---

## Setup

### 2. Initialization

Taplytics can be started with a few options to help you use it during development.

First, the base method:

```java
Taplytics.startTaplytics(this, "Your Api Key");
```

Or, add a map of options.

```java
Hashmap<String, Object> options = new Hashmap<>();
options.put("optionName",optionValue);
Taplytics.startTaplytics(this, "Your Api Key");
```

Possible options are:

| Option Name  | Values | Default | Explanation
|---|---|---|---|
|liveUpdate   | boolean: true/false  | true  | Disable live update to remove the border, and activity refreshing in your debug builds to test the functionality of your applications as if they were in release mode. Note that this functionality is always disabled in release builds.  |   
| shakeMenu | boolean: true/false   | true  | In your debug builds, disable the quick menu that appears when you shake your device. This menu is never present in release builds.|   
| sessionMinutes | int > 0  | 10 | If you do your own analytics alongside taplytics, it helps to define your sessions to be the same length to reconcile your data. Set this to be the same timing interval that your app counts sessions. |   
| turnMenu | boolean: true/false | false | If you are doing visual testing on an emulator, or UI automation, many emulators do not have the ability to shake the device. So, to pop up the taplytics menu on such devices, set turnMenu to true, and simply rotate the device from portrait/landscape twice in a row within 30 seconds and this menu will show.|   


#### The Border / Shake menu. 

When connected to an experiment on a **debug** build, a border will show around your app window. This shows which experiment and variation you are currently viewing.

You can long-press on the top of the border to switch experiments, or shake your device and pick from the menu, or select an experiment from the Taplytics website.

**The border and shake menu will _NOT_ appear on release builds.**

---

### 3. User Attributes

It's possible to send custom user attributes to Taplytics using a JSONObject of user info. 

The possible fields are:

|Parameter  |Type         |
|---      |---          |
|email      | String    |
|user_id    | String    |
|firstname  | String    |
|lastname   | String    |
|name     | String    |
|age      | Number    |
|gender     | String    |

You can also add anything else you would like to this JSONObject and it will also be passed to Taplytics. 

An example with custom data: 

```java
JSONObject attributes = new JSONObject();
attributes.put("email", "johnDoe@taplytics.com");
attributes.put("name", "John Doe");
attributes.put("age", 25);
attributes.put("gender", "male");
attributes.put("avatarUrl", "https://someurl.com/someavatar.png");

attributes.put("someCustomAttribute",50);
attributes.put("paidSubscriber", true);
attributes.put("subscriptionPlan", "yearly");

Taplytics.setUserAttributes(attributes);
```

--- 

### 4. Track Events

####Automatic Events

Some events are automatically tracked by Taplytics and will appear on your dashboard. These events are:

* App Start
* Activity and/or Fragment load
* Activity and/or Fragment destroy
* Activity pause
* App background
* Viewpager changes

App terminate is also tracked, but this is only true when your MAIN activity is at the bottom of your activity stack, and the user exits the app from that activity.

No changes are needed in your code for this event tracking to occur. 

####Custom Events

To log your own events, simply call:    

```java
Taplytics.logEvent("Your Event Name");
```

You can also log events with numerical values:

```java
Number num = 0;
Taplytics.logEvent("Your Event Name", num);
```
    
And with custom object data:

```java
Number num = 0;
JSONObject customInfo = new JSONObject();
customInfo.put("some title",someValue)
Taplytics.logEvent("Your Event Name", num, customInfo);
```

####Revenue Logging

It's also possible to log revenue.

Revenue logging is the same as event logging, only call `logRevenue`:

```java
Number someRevenue = 10000000;  
Taplytics.logRevenue("Revenue Name", someRevenue);
```
    
And similarly, with custom object data:

```java 
Number someRevenue = 10000000;
JSONObject customInfo = new JSONObject();
customInfo.put("some rag",someValue)
    
Taplytics.logRevenue("Revenue Name", someRevenue, customInfo);
```

### 5. Receiving External Analytics
At the moment, Taplytics supports Mixpanel, Google Analytics, Adobe Analytics, Flurry, Localytics and Amplitude as a source of external analytics.

####Mixpanel, Adobe, Localytics and Amplitude

When the Taplytics SDK is installed alongside any of these sources, all of your existing and future analytics events will be sent to both the source _and_ Taplytics.

####Flurry

To properly support sending Flurry data, you simply need to tell Taplytics whenever a new Flurry session begins. This can be done directly after Flurry initialization.

```java
    
    FlurryAgent.init(this, "your flurry API key");

    FlurryAgent.setFlurryAgentListener(new FlurryAgentListener() {
            @Override
            public void onSessionStarted() {
                Taplytics.startFlurrySession();
            }
        });
```

#####Google Analytics 7.0.0-

If you are using Google Analytics 7.0.0 and below, all Google Analytics will automatically be sent to both Google Analytics _and_ Taplytics.

#####Google Analytics 7.3.0+

If you are using Google Analytics 7.3.0 or above, you have the option of changing things to send your Google Analytics to both Google _and_ Taplytics.

Simply find all instances of `tracker.send(new Hitbuilder...)` and replace them with `Taplytics.logGAEvent(tracker, new Hitbuilder...)`

You can do with with a simple find/replace in your application.

An example:

```java
Tracker t = TrackerManager.getInstance().getGoogleAnalyticsTracker(TrackerManager.TrackerName.APP_TRACKER, getApplication());
t.send(new HitBuilders.EventBuilder().setCategory("someCategry").setAction("someAction").setLabel("someLabel").setValue(12).build());
```

Would be changed to:

```java
Tracker t = TrackerManager.getInstance().getGoogleAnalyticsTracker(TrackerManager.TrackerName.APP_TRACKER, getApplication());
Taplytics.logGAEvent(t, new HitBuilders.EventBuilder().setCategory("someCategry").setAction("someAction").setLabel("someLabel").setValue(12).build());
```

---

##6. Sending to External Analytics
Taplytics can send experiment data to external analytics sources on startup. This integration is automatic with the exception of Google Analytics where the tracker instance must be passed as a startup option to Taplytics.
```
HashMap<String, Object> options = new HashMap<>();
options.put("gaTracker", tracker);
Taplytics.startTaplytics(this, "YOUR API KEY", options);
```

---

## Advanced

### Device Pairing

Link a device (even in release mode) to Taplytics.

**NOTE: This is used only for deeplink pairing, and is unnecessary if your main activity does NOT have a singleTask flag.**

Retrieve deeplink through Taplytics deeplink intercepted via either email or SMS device pairing. It contains your Taplytics URL scheme and device token. If you wish to intercept the deeplink and then pair the device yourself in your application's code, call this method, like so:

```java
private void handleDeepLink(Intent intent) {
    String tlDeeplink = intent.getDataString(); //example deep link: 'tl-506f596f://e10651f9ef6b'
    if (tlDeeplink == null) {
            // No deeplink found
            return;
    }
    Taplytics.deviceLink(tlDeeplink);
}
```
