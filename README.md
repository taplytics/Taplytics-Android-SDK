#Taplytics-Android SDK

_Taplytics is a native mobile A/B testing and push notification platform that helps you optimize your Android app!_

**[Get started with Taplytics](https://taplytics.com/docs/android-sdk/getting-started)**

**MAJOR UPDATE: SOCKET.IO DEPENDENCY HAS BEEN CHANGED** 

[See more here.](https://github.com/taplytics/Taplytics-Android-SDK/blob/master/Sockets.md)

###**Current Version: [1.7.0](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.7.0)**

## Getting Started

_How do I, as a developer, start using Taplytics?_

1. _Sign up for a free account at [Taplytics.com](https://taplytics.com?utm_source=github&utm_campaign=documentation&utm_medium=content)._
2. _Install the SDK. Steps [here](/START.md)._
3. Create [Experiments](/EXPERIMENTS.md) or send [Push Notifications](/PUSH.md) to your users!

## Changelog

**[1.7.0](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.7.0)**

1. [Changed to new socket.io dependency](https://github.com/taplytics/Taplytics-Android-SDK/blob/master/Sockets.md)
2. Support for ImageButtons and StateDrawables. 

**[1.6.18](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.6.18)**

1. Added a method to enable or disable push subscriptions

**[1.6.17](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.6.17)**

1. Added a synchronous option dynamic variables
2. Added additional documentation for dynamic variables


**[1.6.16](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.6.16)**

1. Fixed an issue where recyclerViews and listViews visual changes did not persist


**[1.6.15](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.6.15)**

1. Bug fixes


**[1.6.14](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.6.14)**

1. Fixed a bug causing Google Analytics events not to be passed to Taplytics properly
2. Added automatic proguard directives to prevent build errors when using proguard
3. Fixed issue where element upload sometimes failed when editing visual experiments


**[1.6.13](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.6.13)**

1. Increased button click accuracy
2. Increased image caching efficiency

**[1.6.12](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.6.12)**

1. Fixed a null pointer exception that could occur with reseting users and settings user attributes rapidly
2. Added text to the shake menu to be more descriptive

**[1.6.11](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.6.11)**

1. Fixed an issue with google play services version compatibility
2. Added ability to push experiment data to Mixpanel, Adobe, Flurry, Amplitude, Localytics and Google Analytics automatically
3. Increased fragment speed by 0.01%


**[1.6.10](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.6.10)**

1. Fixed an issue where sockets were timing out when paused on a breakpoint


**[1.6.9](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.6.9)**

1. Fixed an issue where push notification intents were being overwritten

**[1.6.8](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.6.8)**

1. Cleaned up events that don't need to be logged in production.

**[1.6.7](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.6.7)**

1. Added support for Localytics, Amplitude and Adobe Analytics
2. Increased startup speed
3. Added a `TaplyticsLoadedListener` that can be added to the `startTaplytics` call
4. Fixed a proguard issue that occured when google play services was not being used
5. Fixed an issue where shake events were being triggered on emulators in live update mode

**[1.6.6](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.6.6)**

1. Fixed an issue related to setting user attributes


**[1.6.5](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.6.5)**

1. Fixed an issue related to reseting users

**[1.6.4](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.6.4)**

1. Upgraded to Google Play Services 8.1
2. Added Android M Support
3. Added Push Campaign support

**[1.6.1](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.6.1)**

1. Stopped unopened notifications from being overwritten

**[1.6.0](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.6.0)**

**Big Changes!**

1. **Increased Taplytics speed 50-80% in specific cases.**
2. **Added new Dynamic Variable and Code Block feature.**
2. Full ListView and RecyclerView support.
3. Full GridView support.
4. Activities no longer need to reset when testing variations.
5. Element values can now be reset on the Taplytics dashboard.
6. Lag eliminated when editing fragments.
7. Lag eliminated from setting images.


**[1.5.15](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.5.15)**

1. Added a `showMenu` method to force show the experiment/variation dialog.

**[1.5.14](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.5.14)**

1. Changed `PushNotificationOpenListener` to simply be a set, instead of an add to prevent duplicate tracking.
2. Changed TurnMenu max time to 30 seconds.
3. Fixed some threading issues.

**[1.5.12](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.5.12)**

1. Added a `PushNotificationOpenListener` to track push notification opens.
2. Added a minimum time for delayLoad.
3. Fixed delayLoad images not showing with certain drawables.
4. Added a TurnMenu option to trigger the experiment dialog without shaking the device (for emulators that don't have a shake option).


**[1.5.11](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.5.11)**

1. Fixed app icons not showing in notification sometimes.
2. Added tl_title custom data field to allow changing title of push notifications.
3. Fixed issues with setting padding.

**[1.5.10](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.5.10)**

1. Fixed images not loading correctly.
2. Fixed RecyclerView changes not showing correctly.
3. Views selected / hovered on Taplytics Dashboard will not be highlighted to better visualize changes.

**[1.5.9](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.5.9)**

1. Fixed issue where the wrong experiment was being selected from the shake menu.
2. Added indicator for when the device is not connected to the internet or Taplytics.
3. Added ability to disable the border from the shakemenu, for QA purposes
4. Added socket connection retries
5. Connections to Taplytics made more stable.

**[1.5.7](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.5.7)**

1. PushTokenListener proguarding issues fixed.

**[1.5.6](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.5.6)**

1. Event reconciliation! If you were notcing more events in Taplytics than your other sources, this update fixes things from now on.

## Questions or Need Help

_The Taplytics team is available 24/7 to answer any questions you have. Just email support@taplytics.com or visit [our docs page](https://taplytics.com/docs?utm_source=github&utm_campaign=documentation&utm_medium=content) for more detailed installation and usage information._
