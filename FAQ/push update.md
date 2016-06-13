## Push Update

Push notifications have changed in Taplytics. While the old way of doing things is still available, its suggested you move on to the new, more logical way.

### Changes:

1. **Push notification tracking is now far more accurate.** Previously, if the user was on an activity and the push notification sent them there, it would not be tracked unless a special flag was set on that activity. Now, its a guaranteed track regardless of the situation. 
2. **Take action on push receive, open, or dismiss**. Now, Taplytics gives you an interface that informs you  when a push from taplytics has been received, allowing you to take action prior to any interactions. On top of push opened, Taplytics now also offers the ability to take action when a push has been dismissed. 
3. **Silent push notifications and push priority.** If you would like to send a push notification that is silent (does not actually show up), you now can. simply add the `tl_silent` flag to the custom data. Similarly, you are now able to define the. Read more about this on the android website [here](https://developer.android.com/design/patterns/notifications.html), and the implementation [here](https://github.com/taphotoplytics/Taplytics-Android-SDK/blob/master/PUSH.md#5-special-push-options-title-silent-push-etc).
4. **A much less confusing way of doing things**. Extend `TLGCMBroadcastReceiver`, override the functions you want (pushOpen, pushReceive, pushDismissed), add it to your manifest, and you're good to go!

**[Read here for more information and an example implementation that can be easily adapted!](https://github.com/taplytics/Taplytics-Android-SDK/blob/master/PUSH.md#4-custom-data-and-tracking-push-interactions)**