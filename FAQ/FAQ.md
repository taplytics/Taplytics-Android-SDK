#Taplytics FAQ

##Q. Why is Taplytics making my app take 15+ seconds to load?

In 99% of cases, this is the fault of Instant Run being enabled. Due to the nature of libraries not being able to have the `vmSafeMode` tag (it is application specific), this process cannot currently be sped up.

**[Here is a response from an Android Studio developer on the problem](https://www.reddit.com/r/androiddev/comments/4m2v25/instant_run_causing_incredibly_long_app_start/d3s9iox)**

##Q. Will the border or shake menu show up on our user's devices?

No.

The border and menu will only ever appear on **_debug devices_** and _release mode devices that have **previously been paired to taplytics**_. 

If a user has a rooted device and modifies your application to be read as debug mode, then yes this may appear. However, if a user is doing that, you will have bigger problems than the Taplytics border.


##Q. What is 'Rejecting Re-Init on Previously Failed Classes'

If you see this:

![image](rejected.jpg)

It's nothing to worry about, at all. 

Taplytics has two different networking options -- Retrofit+OkHttp, or Volley. 

If you choose to use one, the classes/methods relying on the other will be purged out, and that is what you are seeing. 

The animal names are how we obfuscate things!

##Q. How do I pair Taplytics to a release build?

**[You need to have add a few things to your app, as seen here.](https://github.com/taplytics/Taplytics-Android-SDK/blob/master/START.md#device-pairing)**

Also don't forget, your socket library has to be in your release builds (which may not be by default -- ask your devs!)

##Q. Does Taplytics work with Segment?

**Yes! But only with Segment v4**

Add the following to your build.gradle:

```gradle
compile 'com.segment.analytics.android:analytics:+'
compile('com.segment.analytics.android.integrations:taplytics:+')
//Change from debugCompile to compile if you would like pairing on release builds as well.
debugCompile('io.socket:socket.io-client:+') {
    // excluding org.json which is provided by Android
    exclude group: 'org.json', module: 'json'
}
```
As well as any other integrations you are using in segment.

Then, initialize Segment in your Application's onCreate:

```java
Analytics analytics = new Analytics.Builder(this, "SEGMENT_API_KEY").use(TaplyticsIntegration.FACTORY).build();
```
add a .use() for any other integrations you are using.

Remember to enable the Taplytics integration on your Segment dashboard.