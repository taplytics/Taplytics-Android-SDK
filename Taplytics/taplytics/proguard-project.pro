-optimizationpasses 20
-overloadaggressively
#-dontshrink
-dontnote
#-dontoptimize

-keepattributes Exceptions,LineNumberTable,Signature,*Annotation*,SourceFile,EnclosingMethod
#-keepparameternames
#-renamesourcefileattribute SourceFile
#-optimizations !code/simplification/arithmetic
-adaptresourcefilenames ../../Tools/proguardictionary.txt
-mergeinterfacesaggressively
#-flattenpackagehierarchy
-repackageclasses 'com.taplytics'
-useuniqueclassmembernames
-allowaccessmodification
-dontskipnonpubliclibraryclasses
-obfuscationdictionary ../../Tools/proguardictionary.txt
-classobfuscationdictionary ../../Tools/proguardictionary.txt
-renamesourcefileattribute SourceFile

-keepclassmembers class * extends java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-dontwarn okio.**
-dontwarn java.nio.file.*
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn retrofit2.Platform$Java8
-dontwarn com.mixpanel.**



# Keep - Library. Keep all public and protected classes, fields, and methods.
#-keep public class * {
#    public protected <fields>;
#    public protected <methods>;
#}

-keep public class com.taplytics.sdk.Taplytics {
    public protected <fields>;
    public protected <methods>;
	public protected *;
}

-keep public interface com.taplytics.sdk.TaplyticsPushNotificationListener {
	public protected *;
	<methods>;
}

-keep public interface com.taplytics.sdk.TaplyticsPushNotificationIntentListener {
	public protected *;
	<methods>;
}

-keep public interface com.taplytics.sdk.TaplyticsDelayLoadListener {
	public protected *;
	<methods>;
}

-keep public interface com.taplytics.sdk.TaplyticsResetUserListener {
	public protected *;
	<methods>;
}

-keep public interface com.taplytics.sdk.TaplyticsPushTokenListener {
	public protected *;
	<methods>;
}

-keep public interface com.taplytics.sdk.TaplyticsPushOpenedListener {
	public protected *;
	<methods>;
}

-keep public class com.taplytics.sdk.TaplyticsRunningExperimentsListener {
	public protected *;
	<methods>;
}

-keep public interface com.taplytics.sdk.TaplyticsRunningFeatureFlagsListener {
	public protected private *;
	<methods>;
}

-keep public interface com.taplytics.sdk.TaplyticsHasUserOptedOutListener {
	public protected private *;
	<methods>;
}

-keep public interface com.taplytics.sdk.TaplyticsVarListener {
	public protected *;
	<methods>;
}

-keep public interface com.taplytics.sdk.CodeBlockListener {
	public protected *;
	<methods>;
}

-keep public class com.taplytics.sdk.TaplyticsExperimentsLoadedListener {
	public protected *;
	<methods>;
}

-keep public interface com.taplytics.sdk.TaplyticsNewSessionListener {
	public protected *;
	<methods>;
}

-keep public interface com.taplytics.sdk.TaplyticsExperimentsUpdatedListener {
	public protected *;
	<methods>;
}

-keep public class com.taplytics.sdk.TaplyticsVar {*;}


-keep public interface com.taplytics.sdk.CodeBlockListener {
	public protected *;
	<methods>;
}

-keep public interface com.taplytics.sdk.SessionInfoRetrievedListener {
    public protected *;
    <methods>;
}

-keep public interface com.taplytics.sdk.TaplyticsPushSubscriptionChangedListener {
	public protected *;
	<methods>;
}

-keep public interface com.taplytics.sdk.TaplyticsSetUserAttributesListener {
	public protected *;
	<methods>;
}

-keepnames public class com.taplytics.sdk.TaplyticsVar
-keepnames public class com.taplytics.sdk.TLGcmIntentService
-keepnames public class com.taplytics.sdk.TLGeofenceEventService
-keep public class com.taplytics.sdk.TLBootReceiver
-keep public class com.taplytics.sdk.TLGcmBroadcastReceiver
-keep public class com.taplytics.sdk.fcm.TLFirebaseMessagingService
-keep public class com.taplytics.sdk.fcm.TLFirebaseMessagingServiceLite


-keep public class com.taplytics.sdk.TLGcmBroadcastReceiver{
	public protected *;
	<methods>;
}

-keep public class com.taplytics.sdk.fcm.TLFirebaseMessagingService{
	public protected *;
	<methods>;
}


-keep public class com.taplytics.sdk.fcm.TLFirebaseMessagingServiceLite{
	public protected *;
	<methods>;
}

-keep public class com.taplytics.sdk.TLGcmBroadcastReceiverNonWakeful

-keep public class com.taplytics.sdk.TLGcmBroadcastReceiverNonWakeful {
		public protected *;
    	<methods>;
}

# Also keep - Enumerations. Keep the special static methods that are required in
# enumeration classes.
-keepclassmembers enum  * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class * extends java.util.ListResourceBundle {
    protected Object[][] getContents();
}

-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final *** NULL;
}

-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}

-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

#Keeping google play services class that we use reflection for
-keep public class com.google.android.gms.gcm.GoogleCloudMessaging
-keep public class com.google.android.gms.location.GeofencingEvent
-keep public class com.google.android.gms.location.Geofence
-keep public class com.google.android.gms.common.api.GoogleApiClient$Builder
-keep public class com.google.android.gms.common.api.GoogleApiClient$OnConnectionFailedListener
-keep public class com.google.android.gms.common.api.GoogleApiClient$ConnectionCallbacks
-keep public class com.google.android.gms.location.LocationServices
-keep public class com.google.android.gms.common.api.GoogleApiClient
-keep public class com.google.android.gms.analytics.Tracker
-keep class com.adobe.mobile.Analytics
-keep class com.adobe.mobile.AnalyticsTimedAction
-keep class com.adobe.mobile.Analytics {*;}
-keep class com.adobe.mobile.StaticMethods {*;}
-keep class com.adobe.marketing.mobile.MobileCore {*;}
-keep class android.support.v4.app.FragmentManagerImpl
-keepclassmembers class android.support.v4.app.FragmentManagerImpl {*;}
-keep class android.support.v4.app.FragmentManager$*{*;}
-keepclassmembernames class android.support.v4.app.FragmentManager$*{*;}


