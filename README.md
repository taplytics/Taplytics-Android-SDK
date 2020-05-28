# Taplytics-Android SDK

_Taplytics is a native mobile A/B testing and push notification platform that helps you optimize your Android / Android TV / Fire TV app!_

**[Get started with Taplytics](https://taplytics.com/docs/android-sdk/getting-started)** | **[View the Javadoc](https://s3.amazonaws.com/cdn.taplytics.com/javadoc/index.html)** |   	 **[FAQ](https://github.com/taplytics/Taplytics-Android-SDK/tree/master/FAQ)** |  **[Commercial License / Terms](http://taplytics.com/terms)**

### **Current Version: [2.8.2](https://github.com/taplytics/Taplytics-Android-SDK/releases/latest)**

#### Note: Migrate your project to v2.0.0 to use pushes. Instructions for migrating can be found [here](/MIGRATE.md). 

## Getting Started
_How do I, as a developer, start using Taplytics?_

1. _Sign up for an account at [Taplytics.com](https://taplytics.com?utm_source=github&utm_campaign=documentation&utm_medium=content)._
2. _Install the SDK. Steps [here](/START.md)._
3. Create [Experiments](/EXPERIMENTS.md) or send [Push Notifications](/PUSH.md) to your users!

## Questions or Need Help

_The Taplytics team is available 24/7 to answer any questions you have. Just email support@taplytics.com or visit [our docs page](https://taplytics.com/docs?utm_source=github&utm_campaign=documentation&utm_medium=content) for more detailed installation and usage information._

## Changelog
**[2.8.2](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/2.8.2)**

- Fixed rare stability issues


**[2.8.0](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/2.8.0)**

- Added the ability to provide a default value to Feature Flags
- Updates for upcoming speed enhancements


**[2.7.1](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/2.7.1)**

- Fixed minor crashes 

**[2.7.0](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/2.7.0)**

- Added `userBucketing` option for user based bucketing enabled projects

_Note: Contact support@taplytics.com to get your project user based bucketing enabled!_

**[2.5.0](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/2.5.0)**

- Added `logging` option to enable debug logging
- Updated recycler view visual editing

**[2.4.0](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/2.4.0)**

- Improved functionality on the `aggressive` option for visual editing on recycler views

**[2.3.0](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/2.3.0)**

- Fixed upgrading issues with push notifications on devices using older versions of Android

**[2.2.0](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/2.2.0)**

- Added TLFirebaseMessagingServiceLite class 

**[2.1.0](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/2.1.0)**

1. Fixed intermittent crash caused by stack overflow.

**[2.0.0](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/2.0.0)**

1. Migration from GCM to FCM push notification system.

**[1.31.0](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.31.0)**

1. Added manual device tracking option.

**[1.29.0](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.29.0)**

1. Fixed push notification icon image for closed and open notifications.

**[1.28.0](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.28.0)**

1. Improved functionality of visual editing.
2. Add large icon support for push notifications.

**[1.27.0](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.27.0)**

1. Added new call to `setUserAttributes` that has a callback parameter to determine when the call has ended.
2. Changed the default debug check type default to `FLAG_ONLY`.

**[1.26.0](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.26.0)**

1. Added support for opt-in/opt-out for compliance with GDPR.

**[1.25.0](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.25.0)**

1. Modified backoffs for network attempts when app is backgrounded

**[1.24.1](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.24.1)**

1. Added getRunningFeatureFlags functionality.

**[1.23.0](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.23.0)**

1. Added support for feature flags.
2. Improved user attribute lifecycle.
3. Enhanced push support for Oreo+ setups.

**[1.22.2](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.22.2)**

1. Enhanced support for views added on runtime.
2. Ensured that onError callbacks are being called.

**[1.22.1](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.22.1)**

1. Better support for Multithreaded Application classes.
2. Improved encoding of URL parameters.

**[1.22.0](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.22.0)**

1. Added support for multiline rich push notifications.
2. Improved error support for creating new sessions by adding an on error callback to `TaplyticsNewSessionListener`.
3. Enhanced usability of `resetAppUser`.
4. Ensured that new session rate limit is being followed.

**[1.21.2](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.21.2)**

1. Added more granular language support.
2. Google Analytics integration maintenance.

**[1.21.1](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.21.1)**

1. Update Adobe integration
2. Updated event batching method to capture all events in a session more efficiently.

**[1.21.0](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.21.0)**

1. Added support for Android TV and Fire TV!
2. Properly track fragments in a fragment's `childFragmentManager`.

**[1.20.0](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.20.0)**

1. Added support for notifications for Android O.

**[1.19.2](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.19.2)**

1. Improved performance of app lifecycle event tracking.
2. Fixed occurrences where potential memory leaks can happen.
3. Ensured that visual changes get applied from disk when launching the app with no internet connection.

 **[1.19.1](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.19.1)**

1. Refactored Taplytics.delayLoad and Taplytics.startActivity. These now can handle multiple listeners and delay load properly short circuits when startActivity is complete.

 **[1.19.0](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.19.0)**

1. Improved visual editing on ListViews and RecyclerViews inside of ViewPagers.
2. Modified all AsyncTasks to operate on separate thread pool for newer Android SDK versions
3. Support for visual editing on ListViews and RecyclerViews with row elements that are single Views. Previously only supported rows which are ViewGroups.

 **[1.18.2](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.18.2)**

1. Enhanced support for visual editing on text elements with no IDs.
2. Improved performance of first-page experiment goals.
3. Track received push notifications immediately instead of waiting for app open.
4. Added extra sdk-side verification of deeplink URLs for link pairing.

**[1.18.1](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.18.1)**

1. Fixed potential crash if some actions were performed before `startTaplytics` was called.

**[1.18.0](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.18.0)**

1. [Added support for Rich Push Notifications](https://github.com/taplytics/Taplytics-Android-SDK/blob/master/PUSH.md#3-image-push)

**[1.17.2](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.17.2)**

1. Updated performance of link pairing on release builds.

**[1.17.1](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.17.1)**

1. Removed socket dependency for debug pushes. This is only for clients who use this lib as a push lib only. To pair device and test experiments, you must have the socket dependency in your app.

**[1.17.0](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.17.0)**

1. Added ability to visual edit any TextView (or elements subclassing textviews such as buttons or your own custom views) that do not have an android identifier.

**[1.16.1](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.16.1)**

1. Fix volley logging causing bad error reporting.

**[1.16.0](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.16.0)**

1. Updated the behaviour of Dynamic Variables with respect to sessions:

    In the event that a new session begins, Taplytics will now ensure that async variables have their values updated in the event that they have changed in the new session. Further, sync variables will also have a new value so long as they are re-initialized.

2. Added support for ListView and RecyclerView header/footer visual editing.

**[1.15.0](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.15.0)**

1. Added manual push dismissed/received tracking. This is meant only for clients who do not use Taplytics to _build_ notifications, but build themselves.

    Use as follows:
     `Taplytics.trackPushReceived(tl_id, custom_keys)`

    `Taplytics.trackPushDismissed(tl_id, custom_keys)`

     where tl_id and custom_keys are retrieved from the bundle within the Taplytics notification intent.

2. Greatly improved visual editing on carousels and ViewPagers.

**[1.14.0](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.14.0)**

1. Added manual push open tracking. This is meant only for clients who do not use Taplytics to _build_ notifications, but build themselves.

    Use as follows:
`Taplytics.trackPushOpen(tl_id, custom_keys)`

    where tl_id and custom_keys are retrieved from the bundle within the Taplytics notification intent.

**[1.13.5](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.13.5)**

1. Added ability to aggressively force visual changes on elements that may be modified by code in the future.

 For example, a TextView that may be updated in an activity's OnCreate previously would have overridden Taplytics' changes, but now it should remain the values sent by Taplytics. This currently works for **Visibility** and **Text** changes.

 Simply add the "aggressive" starting option to Taplytics will enable this feature.

**[1.13.4](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.13.4)**

1. Added extra security precautions around events databases.
2. Fixed problems related to geofencing push notifications under bad network conditions.

**[1.13.3](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.13.3)**

1. Added another step to the check for whether a device is in dev mode or not. Previously, it only checked for the `FLAG_DEBUGGABLE` flag on the application info. Now, there is also a check on your applications `BuildConfig.DEBUG` parameter to add extra safety around this entire process.
    * Also added a starting option for "`debugCheckType`" with options "`flag`" or "`config`" if you do not want both to be checked.
2. Added a "`isDebug`" starting option, which allows you to manually control the debug status of a device based on your own parameters. For example, if you were to pass in `BuildConfig.DEBUG` as this option, the debug status would be determined based directly on the current value of that parameter.

**[1.13.2](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.13.2)**

1. Added the ability to provide null as a default value for dynamic variables. Null values can not be supplied on the dashboard as of yet.

**[1.13.1](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.13.1)**

1. Change callback timing of `getRunningExperimentsAndVariations` to be more closely tied to the completion of the `propertiesLoaded` callback.
2. Resetting users has improved retry logic under low and no-connectivity situations.
3. Fixed bad interaction between TwinPrime SDK and Socket.io libraries.

**[1.13.0](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.13.0)**

1. Added a new `startNewSession` function which allows for manually starting a new user session. This also allows for manually fetching new Taplytics experiments for the user if there are any. [See the docs here](https://github.com/taplytics/Taplytics-Android-SDK/blob/master/EXPERIMENTS.md#startnewsession)

**[1.12.6](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.12.6)**

1. Modified timing of propertiesLoaded callback to allow for creation / updating dynamic variables in the propertiesLoaded callback with much better reliability.
2. Updated socket retry logic to back off better.
3. Perform check on deep-links to improve performance in apps with many deep links and Taplytics advanced pairing.


**[1.12.5](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.12.5)**

1. Fixed issue in data retrieval for apps with very short package names.

**[1.12.4](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.12.4)**

1. Fixed threading issue when starting up Flurry tracking

**[1.12.3](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.12.3)**

1. Fixed an issue in which some activities would not receive visual changes in race conditions. Fragments unaffected.
2. Fixed an issue where new dynamic variables were not being sent from code to initialize on Taplytics website.

**[1.12.2](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.12.2)**

1. Update event saving for those who use multiple SDK keys for different environments.

	Previously, stored events were not associated with a project and would be sent to the project associated with the current SDK key. This meant that stored events had the potential to be sent to the incorrect project. This mainly effected those using multiple SDK keys to debug.

2. Add some additional logs for support.

3. Add additional option to ignore fragment classnames in projects which heavily obfuscate activities and fragments, causing them to have different names upon each build.


**[1.12.1](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.12.1)**

1. Security optimizations. Taplytics will now encrypt all data. Taplytics data in of itself is not sensitive data. However, custom data appended to events as well as potential user attributes set by the client may contain sensitive data. While not easily obtained, rooted devices had the ability to view this data. This is now handled by encrypting all data saved on the device.

2. Reduced lag when editing List/Recyclers in debug mode.

**[1.12.0](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.12.0)**

1. Explicitly require HashMap in options for startTaplytics

	Due to operations done within Taplytics, this can no longer be an ArrayMap and must explicitly be a HashMap.

2. Optimized and refactored ListView and RecyclerView visual editing.

	Visual edits made on RecyclerViews are now far more reliable due to a change in the identification of which list cell needs to be changed. Visual edits on List/RecyclerViews also now operate much more efficiently.

3. Fixed bug in dynamic variables with numbers which are both floats or integers.

	When a dynamic variable's default value was a float or integer, all variations were expected to be of the same type. This was problematic when parsing JSON, as the system saw any numbers with no decimal as an integer, even if the type was a float. This resulted in the default value always being returned. This has been handled and will no longer be an issue.

4. Various small efficiency improvements in width/height visual edits. 	

**[1.11.11](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.11.11)**

1. DelayLoad should no longer immediately return during liveUpdate builds.

	delayLoad now waits for a pass/fail on the config request before triggering. Previously, on connected debug builds, it would trigger after visual edits have been applied, even if just from disk.

2. Track Adobe Analytics `trackState` calls.

	Previously Taplytics would only track `trackAction`, but now it also allows for `trackState`

3. Added safety around new external integration methods.

	Simple safety checks and catches to ensure no errors showing up. Errors are expected in these integrations and our attempts do deal with them, however they are squelched. This update just ensures that such errors don't reach crashlytics and cause confusion.

4. Update Taplytics internal Retrofit interceptor.

	Taplytics uses a networkInterceptor to do a simple obfuscation of network traffic through Taplytics (using its own okKttpClient). This may not work depending on the app's okHttp/retrofit versions (older). While not technically an issue, extra safety has been added in this area.


**[1.11.10](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.11.10)**

1. Updated Flurry, Amplitude, Adobe, Localytics, and Mixpanel integrations to allow event handling on their newest SDK versions.

	Flurry integration now also supports property maps.

2. Fixed error when user's country code does not have associated ISO language.

	For example, es_LG is unknown by the Android system, and will no longer cause an error when the ISO3 language code is searched for.

3. Added additional socket call to better interact with the new experiment selector on the User Insights page.


**[1.11.9](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.11.9)**

1. Fixed ExperimentUpdatedListener returning wrong value on initial use of Shake Menu.

	In a race condition for large projects, the listener was triggered  before new updated values were cached fully. Additionally, if a project initially timed out, the SDK would not have the cached data to reference.

**[1.11.8](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.11.8)**

1. Decreased sensitivity of shake menu listener.

	Using the most recent android build tools, it seems that the shake menu appears a lot more frequently. The low-pass filter used on the device accelerometer for detecting shakes has been increased to be far less sensitive.

2. Added startup safety for Taplytics (internal)

	In past updates, safeties had been put in place during the startTaplytics call internally. More comprehensive checks, weak references for leak safety, etc. In this update, these checks and safeties have been extended to many of the static variables and their static initializations in core Taplytics classes.

**[1.11.7](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.11.7)**

1. Fixed slowdown in visual edits on ViewPagers

	Previously there was a slight delay when applying visual edits to an element on a ViewPager. This slowdown has been eliminated in this update by preemptively making the visual update before the viewpager is visible.

	Technical Explanation:

	Android unfortunately does not contain a standard method to retrieve a fragment from a ViewPager by its position, but rather it only contains a `getCurrentFragment` method which returns the current position as an integer. Taplytics does not make use of `getItem` or `instantiateItem` as it may incidentally create a new instance of the fragment depending on the developer's implementation, and we wish to remain as non-intrusive as possible. Its common for developers to implement methods in which to track the fragments in a ViewPager, but as a library, Taplytics does not want to make assumptions of the existence of such functions.

	In the past, Taplytics applied changes to every view child in the ViewPager, however this was deemed far too inefficient, especially within ViewPager with more than 5 pages. Soon after, Taplytics relied on a reflective call into the ViewPager's `mCurrentFragment` field to make visual changes, but this only allows changes to be made on the current fragment showing, which is where this mentioned delay originated.

	Now, Taplytics is able to identify which views within a ViewPager require changes and apply them before the ViewPager is on the screen. This is due to a tagging system that identifies which fragment needs changes, which also allows for an efficient search for these fragments. Additionally, Taplytics will only change the current fragment as well as fragments directly to the left and right of the current fragment, allowing for the most efficient visual edits of ViewPagers yet.

2. Fix Taplytics not being able to properly track fragments on newest support libraries.

	This is a simple Proguard issue. Android's FragmentManager contained a `mExecutingTransactions` field which Taplytics uses to ensure that certain interactions with the FragmentManager are safe. As an extra precaution, if there is an issue surrounding this field, Taplytics will not risk interacting with fragments.

	In the _most_ recent support fragment updates, this field was moved by Android, and so our tests picked up these changes and notified us of a necessary change within our own Proguard configuration which keeps this field's name.

**[1.11.6](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.11.6)**

1. Change base method used for finding views.

	 Taplytics previously relied on `findViewById` in base viewgroups to find views that need to be modified. This became unreliable over time as the applications changed given that ids can change.

	Taplytics will now first check the view identifier first before the id. **This means that if the _name_ (identifier) of the view is changed to something new, Taplytics will not be able to modify it using the old experiment, and it will need to be set up again.**

2. Fragments will now too rely on the identifier of the fragment container instead of the ID for similar reasons to the ones stated above.

3. Fixed an issue in which Android studio would output many warnings surrounding Proguard and inner classes.

**[1.11.5](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.11.5)**

1. Fix crash on specific Lenovo devices that do not have carrier info.
2. Button click goals on buttons with same ID across multiple fragments will now be properly tracked.

**[1.11.4](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.11.4)**

1. Fix variableUpdated not being called when switching variations via shake menu
2. Fixed edge case in which delayComplete() could be called twice if thread containing delayComplete() is locked.

**[1.11.3](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.11.3)**

1. Fixed potential google developer console warning regarding Device IDs.
2. Changed button click goals to check for current fragment before tracking to avoid duplicates.
3. Switched events endpoint.

**[1.11.2](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.11.2)**

1. Fix support fragment tracking in 24.2.0 libs.
2. No longer use `findViewById` for button click tracking. Only use resource identifiers.

**[1.11.1](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.11.1)**

1. Fixed Proguard error

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
4. Kickoff all variableUpdated and visual editor changes with disk or default values immediately after timeout.
5. Devices which have a delay to start Taplytics (such as via segment) will now track the main activity start more consistently.
6. Debug devices which time out can now kick off pairing without needing to restart the app.



**[1.10.2](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.10.2)**

1. Load variables from disk faster.
2. Timeout getRunningExperimentsAndVariations if the TaplyticsExperimentsLoadedListener timed out previously.

**[1.10.1](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.10.1)**

1. Performance updates for applications that don't contain support libraries.
2. Added the ability to retrive current user attributes. Use `Taplytics.getUserAttributes(new UserAttributesRetrievedListener)`.
3. If you are in a session which timed out and you are in debug mode and testing experiments via the website or shake menu, you can now properly switch between variations and experiments. Previously the timeout would make this impossible as we never used a new config, but this made testing in these situations difficult.

**[1.10.0](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.10.0)**

1. **Change in how timeouts are handled.** Now, if the TLExperimentsLoadedListener times out, Taplytics will only use what data is stored on the disk for that entire session. Taplytics will still attempt to load the proper data in the background and will save that to disk to be ready for use for the next session. This prevents bad data in the event that an experiment did not properly load (due to bad internet). The default timeout is four seconds.

**[1.9.16](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.9.16)**

1. Changed the looper that advanced pairing uses to allow more consistent pairing.

**[1.9.15](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.9.15)**

1. Segment spec change.

**[1.9.14](https://github.com/taplytics/Taplytics-Android-SDK/releases/tag/1.9.14)**

1. Added starting option to fix problems when delaying the starting of Taplytics, such as with segment.

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

  ------
### [Full historical changelog can be found here](https://github.com/taplytics/Taplytics-Android-SDK/releases/latest)
