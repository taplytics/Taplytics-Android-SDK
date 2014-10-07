# Taplytics-Android-SDK

_Description: Taplytics is a native mobile A/B testing platform that helps you optimize your Android app!_

**Current Version**: 1.0.9

## Project Setup

_How do I, as a developer, start using Taplytics?_ 

1. _Sign up for a free account at Taplytics.com._
2. _Install the SDK._
3. _Create an experiment and push it live to your users!_


## Android Studio Installation Instructions

1. _In your module’s build.gradle, add the url to the sdk._

  ```
  repositories {                                                                                              
    maven { url "https://github.com/taplytics/Taplytics-Android-SDK/raw/master/AndroidStudio/" }
  }      
  ```
  
2. _In your module’s build.gradle dependencies, compile Taplytics._

  ```
  dependencies {                                                                   
    //Taplytics                                                                        
    compile("com.taplytics.sdk:taplytics:+@aar")                                                         
  }    
  ```
  
  
3. _Override your application’s onCreate() method (not your main activity) and call Taplytics.startTaplytics(). It should look like this:_

  ```java
  public class ExampleApplication extends Application {
    @Override
    public void onCreate() {
      super.onCreate();
      Taplytics.startTaplytics(this, "YOUR TAPLYTICS API KEY");
    }
  }
  ```
4. _Finally, add the proper permissions, and the Application class to your app’s AndroidManifest.xml in the Application tag._

  ```xml
  <uses-permission android:name="android.permission.INTERNET" />
  <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
  <application
    android:name=".ExampleApplication"
    ...
  ```
  
5. _That's it! Now build and run your app, you can start creating experiments with Taplytics!_


## Eclipse Installation Instructions

1. _Download the taplytics.jar [here](https://github.com/taplytics/Taplytics-Android-SDK/raw/master/taplytics.jar)_
2. _Copy the jar into your 'libs' directory in your project._
3. _Right click the jar in Eclipse, click Build Path > add to build path_
4. _Override your application’s onCreate() method (not your main activity) and call Taplytics.startTaplytics(). It should look like this:_

  ```java
  public class ExampleApplication extends Application {
    @Override
    public void onCreate() {
      super.onCreate();
      Taplytics.startTaplytics(this, "YOUR TAPLYTICS API KEY");
    }
  }
  ```
5. _Finally, add the proper permissions, and the Application class to your app’s AndroidManifest.xml in the Application tag._

  ```xml
  <uses-permission android:name="android.permission.INTERNET" />
  <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
  <application
    android:name=".ExampleApplication"
    ...
  ```
  
6. _That's it! Now build and run your app, you can start creating experiments with Taplytics!_
