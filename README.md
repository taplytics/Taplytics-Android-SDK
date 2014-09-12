# Taplytics-Android-SDK

_Description: Taplytics is a native mobile A/B testing platform that helps you optimize your Android app!_

## Project Setup

_How do I, as a developer, start using Taplytics?_ 

1. _Sign up for a free account at Taplytics.com._
2. _Install the SDK._
3. _Create an experiment and push it live to your users!_

## Eclipse Installation Instructions

1. _Clone the Taplyitcs Android SDK as a submodule into your desired directory using:_

  ```
  git submodule add git@github.com:taplytics/taplytics-android-sdk.git Taplytics
  ```
  
2. _Import the project into eclipse._
3. _Mark the SDK as a library in the SDK project’s properties (Right click the project in the project list, select properties. Next click Android and check the ‘Is Library’ option)_
4. _Add the library to your project (right click the project, select properties, click android, click add, select the sdk)_
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
6. _Finally, add the proper permissions, and the Application class to your app’s AndroidManifest.xml in the Application tag._

  ```xml
  <uses-permission android:name="android.permission.INTERNET" />
  <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
  <application
    android:name=".ExampleApplication"
    ...
  ```
  
7. _That's it! Now build and run your app, you can start creating experiments with Taplytics!_

## Android Studio Installation Instructions

1. _In your module’s build.gradle, add the url to the sdk, as well as mavenCentral():_

  ```
  repositories {                                                                                              
    mavenCentral() 
    maven { url 'https://github.com/taplytics/Taplytics-Android-SDK/raw/master/AndroidStudio/’ }
  }      
  ```
  
2. _In your module’s build.gradle dependencies, compile Taplytics and all of its dependencies by adding ‘transitive =  true’_

  ```
  dependencies {                                                                   
    //Taplytics                                                                        
    compile('com.taplytics.sdk:taplytics:+@aar') {              	
      transitive = true                                                           
    }                                                                                       
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


####Fixing common gradle issues

  This import may cause conflicts with your existing dependencies. So, if you see an error during the build step that says: `‘Multiple dex files define…’`, then you can exclude the conflicting libraries the following ways:
  
  Example: the conflicting library is the v4 support library:
    
  Underneath `transitive=true':
    
    
    //You can exclude by artifact name
      exclude module: ‘support-v4’
    
    //Or, you can exclude by group
      exclude group: 'com.google.support'
        
    //Or, you can exclude by group and artifact
      exclude group: 'com.google.support', module: ‘support-v4’
    
