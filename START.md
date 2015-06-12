## Project Setup

| Table of Contents |
| ----------------- |
| [Android Studio Installation](#android-studio-installation) |
| [Eclipse Installation](#eclipse-installation) |

### Android Studio Installation

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
    compile("com.squareup.okhttp:okhttp-urlconnection:+")
    compile("com.squareup.okhttp:okhttp:+")
   
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
    compile("com.google.android.gms:play-services-gcm:7.5.0")
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
  
7. _That's it! Now build and run your app, you can start creating experiments with Taplytics!_


### Eclipse Installation

1. _Download the taplytics.jar [here](https://github.com/taplytics/Taplytics-Android-SDK/raw/master/taplytics.jar)_
2. _Copy the jar into your 'libs' directory in your project._
3. _Right click the jar in Eclipse, click Build Path > add to build path_
4. **NEW:** _Add Google Play Services to your project by following the steps listed [here.](http://developer.android.com/google/play-services/setup.html) Be sure to change the dropdown to "Eclipse with ADT"
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
