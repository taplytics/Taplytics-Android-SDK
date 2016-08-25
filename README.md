#Taplytics-Android SDK

_Taplytics is a native mobile A/B testing and push notification platform that helps you optimize your Android app!_

**[Get started with Taplytics](https://taplytics.com/docs/android-sdk/getting-started)** | **[View the Javadoc](https://s3.amazonaws.com/cdn.taplytics.com/javadoc/index.html)** |   	 **[FAQ](https:/88/github.com/taplytics/Taplytics-Android-SDK/blob/master/FAQ/FAQ.md)**

###**Current Version: [1.11.0](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.11.0)**

###Big News: [Push has changed and is better in 1.9.0+](https://github.com/taplytics/Taplytics-Android-SDK/blob/master/FAQ/push%20update.md)

###Big News: Retrofit2 can be used in place of Volley in 1.8.0+

## Getting Started
_How do I, as a developer, start using Taplytics?_

1. _Sign up for a free account at [Taplytics.com](https://taplytics.com?utm_source=github&utm_campaign=documentation&utm_medium=content)._
2. _Install the SDK. Steps [here](/START.md)._
3. Create [Experiments](/EXPERIMENTS.md) or send [Push Notifications](/PUSH.md) to your users!

## Changelog

**[1.11.0](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.11.0)**

1. Fixed networking library warnings in the ART runtime environment.
2. Fixed networking library detection (Volley/Retrofit)
3. Removed `getUserAttributes` for security. Replaced with `getSessionInfo`. 
4. Internal code cleanup to make things pretty.

**[1.10.8](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.10.8)**

1. Now compiling with Gradle 3.0, Android tools 2.2.0-beta1, to SDK 24
2. Fixed timeouts triggering sending data to external sources more than once.
3. Support library 24.2.0 support.
4. Fixed timeout messaging occurring after successful load.


**[1.10.7](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.10.7)**

1. getUserAttributes now contains session ID
2. Updated safety around logging
3. Fixed some slowdowns caused by the Taplytics overlay

**[1.10.6](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.10.6)**

1. Update pairing


**[1.10.5](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.10.5)**

1. Async option update to work better with fragments.

**[1.10.4](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.10.4)**


1. ### **Timeout declaration have been removed from callbacks and added as a Taplytics starting parameter**
	For example, you now can start Taplytics as such:
	`Taplytics.startTaplytics(Context, ApiKey, Options, TIMEOUT, listener)`

	When this timeout is reached, Taplytics will continue and ONLY use values stored on disk for the remainder of the session. 

2. Improve code block timeout and cache interaction.
3. Make fewer socket connection calls on debug app startup.
4. Kickoff all variableupdated and visual editor changes with disk or default values immediately after timeout.
5. Devices which have a delay to start Taplytics (such as via segment) will now track the main activity start more consistently.
6. Debug devices which time out can now kick off pairing without needing to restart the app. 
 


**[1.10.2](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.10.2)**

1. Load variables from disk faster.
2. Timeout getRunningExperimentsAndVariations if the TaplyticsExperimentsLoadedListener timed out previously.

**[1.10.1](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.10.1)**

1. Perfomance updates for applications that don't contain support libraries.
2. Added the ability to retrive current user attributes. Use `Taplytics.getUserAttributes(new UserAttributesRetrievedListener)`.
3. If you are in a session which timed out and you are in debug mode and testing experiments via the website or shake menu, you can now properly switch between variations and experiments. Previously the timeout would make this impossible as we never used a new config, but this made testing in these situations difficult. 


**[1.10.0](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.10.0)**

1. **Change in how timeouts are handled.** Now, if the TLExperimentsLoadedListener times out, Taplytics will only use what data is stored on the disk for that entire session. Taplytics will still attempt to load the proper data in the background and will save that to disk to be ready for use for the next session. This prevents bad data in the event that an experiment did not properly load (due to bad internet). The default timeout is four seconds. 

**[1.9.16](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.9.16)**

1. Changed the looper that advanced pairing uses to allow more consistent pairing.

**[1.9.15](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.9.15)**

1. Segment spec change.

**[1.9.14](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.9.14)**

1. Added starting option to fix problems whne delaying the starting of taplytics, such as with segment.

**[1.9.13](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.9.13)**

1. Added a Non Wakeful broadcast receiver that can be used for push.
2. Updated Segment experiment sending to follow spec.

**[1.9.12](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.9.12)**

1. Added a few proguard changes to the consumer file to ensure that taplytics functionality remains stable regardless of your obfuscations.
2. Fixed a bad recursive call when searching for viewpagers.

**[1.9.11](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.9.11)**

1. **DialogFragment support!** You can now modify dialogFragments and use them for goals just like any other view. Just make sure you add to the backstack and call `.show `using the same tag, for example:
	```java
	    	fragmentTransaction.addToBackStack("example tag");
       	exampleDialog.show(fragmentTransaction, "example tag");
    ```
2. Time on Page goals now have consistent time tracking for Fragments.
3. Minor proguard changes.

**[1.9.10](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.9.10)**

1. Fixed another potential issue with Adobe events.
2. Send experiment data to segment (enable on dashboard)

**[1.9.9](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.9.9)**

1. Fixed Adobe event tracking to work with Adobe 4.11
2. Updated geofence logic to ensure geofences won't be lost if they are not successfully added the first time.

**[1.9.8](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.9.8)**

1. In the event that there are multiple `onClickListener`s stacked on top of each other, Taplytics will default to tracking the _first_ one it finds set up with button clicks. This shouldn't cause issues so long as your app does not have two button click goals stacked on top of each other.

2. **You can no longer ProGuard Taplytics**, hopefully. Taplytics is already proguarded by us. Here is the reasoning behind this:
  * Recently, many clients have put options in ProGuard that inadvertently proguarded Taplytics a second time. As you can imagine, this makes it impossible for us to use _our_ mappings to find the issue with that stacktrace, as we are referencing a special obfuscation (our animal names!)
  * Much of Taplytics runs on Reflection to make things more efficient and to access some fields that normally can't be accessed by a library. ProGuard checks method invocations to determine what is used and what isn't, removing those it deems not being used -- however, it is impossible for ProGuard to know when a method is accessed via reflection. This leads to some listeners and other internal systems being removed incorrectly.
  * To really compress and obfuscate Taplytics, we overload our obfuscations aggressively. This means that many methods and classes will share obfuscated names. The compiler understands this within itself. However, in the event that it gets proguarded again by another system, this can cause unsatisfied links and missing classes.
  * Retrofit services don't play well with being proguarded twice.

**[1.9.7](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.9.7)**

1. Button click goals in RecyclerViews/ListViews


**[1.9.6](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.9.6)**

1. Added timeouts to ExperimentsLoadedListener and RunningExperimentsListener.

**[1.9.5](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.9.5)**

1. Fixed an issue with 'Time on View' goals.
2. Fixed an issue where support fragments were sometimes counting opens twice.
3. Added more animals to the ProGuard file. Specifically birds. _Very important._

**[1.9.3](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.9.3)**

1. Use an extremely unique ID instead of possibly non-unique device settings and information.
2. Upgraded SDK to Android 24. DID NOT compile with jack, so that you can still use java 7 if you wish.
3. Button click conversions fix!
4. Sessions less easy to re-trigger.

**[1.9.2](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.9.2)**

1. Android sometimes sets a device ID to `0123456789ABCDEF` on Chinese devices, custom ROMs, and emulators. This was breaking distributions as they were all seen as the same device. This has been fixed.
2. fixed `delayComplete()` being called twice.

**[1.9.1](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.9.1)**

1. Simply a version number bump to allow a fix for #15 if needed on CI.

**[1.9.0](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.9.0)**

1. Workaround for when view ids change in release mode vs debug mode.
2. **[Big new push notification changes.](https://github.com/taplytics/Taplytics-Android-SDK/blob/master/PUSH.md#4-custom-data-and-tracking-push-interactions)**
3. Socketio safety.
4. Fully deprecated code experiments. Use code blocks and dynamic variables now.
5. General code cleanup.


**[1.8.3](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.8.3)**

1. Can now use JSONObjects as Dynamic Variables.

**[1.8.2](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.8.2)**

1. _Faster_ button click detection!

**[1.8.1](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.8.1)**

1. Fixed a problem accessing Mixpanel Tokens.

**[1.8.0](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.8.0)**

1. You can now use Retrofit2 if you wish and remove Volley. More info [here](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.8.0)
2. `getRunningExperimentsAndVariations` will no longer return null, and will instead return an empty HashMap in the event that the configuration has not been loaded.
3. Fixed a race condition in which a session ID could be null if it the config was being loaded at the same time as the appUser being reset.

**[1.7.24](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.7.24)**

1. Improved view change timing on support fragments to allow for guaranteed first-load changes in most cases.

**[1.7.23](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.7.23)**

1. Fixed a problem with event sending if user attributes have been set prior to starting Taplytics.
2. New Feature: [Setting Attributes on First Launch](https://github.com/taplytics/Taplytics-Android-SDK/blob/master/START.md#user-attributes-on-first-launch)
3. New Feature: [Test Specific Experiments and Variations](https://github.com/taplytics/Taplytics-Android-SDK/blob/master/EXPERIMENTS.md#testing-specific-experiments)
4. Limit size of metadata to disallow heavy network calls.
5. Starting options to entirely disable borders during tests.
6. Upgraded build tools and support libs to most recent (23.0.3 at the time, as well as support libs 23.3.+)
7. Deleted all releases that had a bug regarding #2 in this list. If you were using that, please contact us.

**[1.7.18](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.7.18)**

1. Security check fix surrounding (unused) local IPs.
2. Complex view hierarchy debug mode improvements.

**[1.7.17](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.7.17)**

1. Added Taplytics Session Listener.

**[1.7.16](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.7.16)**

1. Fixed potential `ListView` hang.
2. Enforce button colors on Taplytics dialogs.

**[1.7.15](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.7.15)**

1. Fixed some initialization errors on Chinese phones.
2. Persistent experiment caching.

**[1.7.13](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.7.13)**

1. Draft experiments returned in `getRunningExperiments` callback.


## Questions or Need Help

_The Taplytics team is available 24/7 to answer any questions you have. Just email support@taplytics.com or visit [our docs page](https://taplytics.com/docs?utm_source=github&utm_campaign=documentation&utm_medium=content) for more detailed installation and usage information._
