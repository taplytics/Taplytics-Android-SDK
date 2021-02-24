/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import com.taplytics.sdk.BuildConfig;
import com.taplytics.sdk.managers.TLKillSwitchManager;
import com.taplytics.sdk.managers.TLManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public class TLUtils {

    private static final String PREFERENCE_KEY = "TAPLYTICS_PREFS";
    static final String PREFERENCE_KEY_SECURE = "TAPLYTICS_PREFS_SECURE";

    public enum DebugCheckType {
        BOTH("both"), FLAG_ONLY("flag"), CONFIG_ONLY("config");
        private final String text;

        DebugCheckType(final String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }

        public static DebugCheckType fromString(String text) {
            for (DebugCheckType b : DebugCheckType.values()) {
                if (b.text.equalsIgnoreCase(text)) {
                    return b;
                }
            }
            return BOTH;
        }
    }

    /**
     * Executes an asynctask on the appropriate executor depending on android version with no params
     */
    public static void executeAsyncTask(AsyncTask task) {
        //When an AsyncTask with no parameters is created, its going to be a Void.. type.
        //However, if an async task is called with an ambiguous parameter type, it will be of type Object.
        //This means internally, if we were to just call executeAsyncTask(), it would create an Object array param
        //which would crash all asynctasks instantiated with 'Void'
        executeAsyncTask(task, new Void[]{});
    }

    /**
     * Executes an asynctask on the appropriate executor depending on android version
     */
    public static <TypedAsyncTask extends AsyncTask, Param> TypedAsyncTask executeAsyncTask(TypedAsyncTask task, Param... params) {
        //Before Android 13, async tasks were executed on a thread pool.
        //After that, execute() ran in serial. This switches it.
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                if (params != null) {
                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
                } else {
                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            } else {
                if (params != null) {
                    task.execute(params);
                } else {
                    task.execute();
                }
            }
        } catch (Throwable e) {
            TLLog.error("Error executing async task: " + (task == null ? "" : task.getClass().getSimpleName()), e);
        }
        return null;
    }

    private static Random rand;

    public static void addURLParamsFromDic(Map<String, Object> dic, StringBuilder url) {
        try {
            if (dic == null || url == null)
                return;

            Integer index = 0;

            StringBuilder builder = new StringBuilder();

            for (Map.Entry<String, Object> entry : dic.entrySet()) {
                if (index != 0) {
                    builder.append("&");
                }
                builder.append(entry.getKey()).append("=").append(entry.getValue().toString());
                index++;
            }
            if (TLManager.getInstance().base64Enabled()) {
                String base64 = new String(Base64.encode(builder.toString().getBytes(), Base64.NO_WRAP));
                url.append(base64);
            } else {
                url.append(builder.toString());
            }
        } catch (Throwable e) {
            TLLog.error("Error appending base64 query params", e);
        }
    }

    public static StringBuilder getUrlParamStringFromDic(Map<String, Object> dic, StringBuilder paramsStringBuilder) {
        if (dic == null)
            return null;

        if (paramsStringBuilder == null) {
            paramsStringBuilder = new StringBuilder();
        }

        Integer index = 0;
        boolean hasQ = paramsStringBuilder.toString().contains("?");

        for (Map.Entry<String, Object> entry : dic.entrySet()) {
            if (index == 0 && !hasQ)
                paramsStringBuilder.append("?");
            else
                paramsStringBuilder.append("&");
            paramsStringBuilder.append(entry.getKey()).append("=").append(entry.getValue().toString());
            index++;
        }
        return paramsStringBuilder;
    }

    static boolean isJSONValid(String test) {
        try {
            new JSONObject(test);
        } catch (JSONException ex) {
            try {
                new JSONArray(test);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }

    public static boolean checkHasAndroidPermission(Context context, String permission) {
        int res = context.checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    private static Boolean configDebug;

    /**
     * Is {@link BuildConfig#DEBUG} still broken for library projects? If so, use this.</p>
     * <p>
     * See: https://code.google.com/p/android/issues/detail?id=52962</p>
     *
     * @return {@code true} if this is a debug build, {@code false} if it is a production build.
     */
    private static boolean isDebugBuildFromConfig() {
        if (configDebug == null) {
            try {
                final Class<?> activityThread = Class.forName("android.app.ActivityThread");
                final Method currentPackage = activityThread.getMethod("currentPackageName");
                final String packageName = (String) currentPackage.invoke(null, (Object[]) null);
                final Class<?> buildConfig = Class.forName(packageName + ".BuildConfig");
                final Field DEBUG = buildConfig.getField("DEBUG");
                DEBUG.setAccessible(true);
                configDebug = DEBUG.getBoolean(null);
            } catch (final Throwable t) {
                final String message = t.getMessage();
                if (message != null && message.contains("BuildConfig")) {
                    // Proguard obfuscated build. Most likely a production build.
                    configDebug = false;
                } else {
                    configDebug = false;
                }
            }
        }
        return configDebug;
    }

    public static boolean checkIsDebug(Context appContext) {
        boolean isDebugFromFlags = (0 != (appContext.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));
        Boolean isDebugFromConfig = isDebugBuildFromConfig();
        DebugCheckType type = TLManager.getInstance().getDebugCheckType();
        if (type == DebugCheckType.CONFIG_ONLY) {
            return isDebugFromConfig;
        } else if (type == DebugCheckType.FLAG_ONLY) {
            return isDebugFromFlags;
        } else {
            return isDebugFromConfig && isDebugFromFlags;
        }
    }

    public static Thread getThreadByName(String threadName) {
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getName().equals(threadName)) return t;
        }
        return null;
    }

    public static void runOnMainThread(Runnable runnable) {
        Activity currentActivity = TLManager.getInstance().getCurrentActivity();
        if (currentActivity != null) {
            currentActivity.runOnUiThread(runnable);
        } else {
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(runnable);
        }
    }

    public static double getRandomTime(double maxTime) {
        if (rand == null)
            rand = new Random();

        return rand.nextDouble() * maxTime;
    }

    public static String getAppVersion() {
        try {
            PackageInfo info = TLManager.getInstance().getAppContext().getPackageManager().getPackageInfo(TLManager.getInstance().getAppContext().getPackageName(), 0);
            return info.versionName;
        } catch (Exception e) {
            TLLog.error("appVersion error", e);
            return "";
        }
    }

    public static int getAppCode() {
        try {
            PackageInfo info = TLManager.getInstance().getAppContext().getPackageManager().getPackageInfo(TLManager.getInstance().getAppContext().getPackageName(), 0);
            return info.versionCode;
        } catch (Exception e) {
            TLLog.error("appVersion error", e);
            return 0;
        }
    }

    /**
     * Check to see if this function is disabled. Put here to make code prettier.
     */
    public static boolean isDisabled(Functionality function) {
        try {
            return TLKillSwitchManager.getInstance().getDisabledFunctions().contains(function.getText());
        } catch (Throwable e) {
            return false;
        }
    }

    public static boolean isEmptyString(String str) {
        return str == null || str.length() == 0;
    }

    /**
     * Simply launches the activity which has the MAIN/LAUNCH intent filter.
     */
    public static void launchMainActivity() {
        try {
            Context context = TLManager.getInstance().getAppContext();
            PackageManager pm = context.getPackageManager();
            String packageName = context.getPackageName();
            Intent intent = pm.getLaunchIntentForPackage(packageName);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            if (TLManager.getInstance().getCurrentActivity() != null) {
                context = TLManager.getInstance().getCurrentActivity();
            } else {
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
        } catch (Exception e) {
            TLLog.error("Error launching main activity", e);
        }
    }


    //Move our preferences from unencrypted to encrypted!
    public static void upgradePreferences() {
        try {
            Context context = TLManager.getInstance().getAppContext();
            if (context != null) {
                SharedPreferences prefs = context.getSharedPreferences(TLUtils.PREFERENCE_KEY, Context.MODE_PRIVATE);
                if (prefs != null && prefs.getAll().size() > 0) {
                    Map<String, ?> allPrefs = prefs.getAll();
                    for (String key : allPrefs.keySet()) {
                        SecurePrefs.getInstance().put(key, (String) allPrefs.get(key));
                    }
                    prefs.edit().clear().apply();
                }
            }
        } catch (Throwable e) {
            TLLog.error("pref up", e);
        }
    }

    public static String generateDeviceDicHash(HashMap<String, String> values) {
        try {

            String idString = "";
            for (String key : values.keySet()) {
                String id = values.get(key);
                idString = idString.equals("") ? id : idString + "-" + id;
            }
            MessageDigest m;
            m = MessageDigest.getInstance(SecurityUtils.decodeBase64String("TUQ1"));
            m.reset();
            m.update(idString.getBytes());
            byte[] digest = m.digest();
            BigInteger bigInt = new BigInteger(1, digest);
            String hashtext = bigInt.toString(16);
            // Now we need to zero pad it if you actually want the full 32 chars.
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            return hashtext;
        } catch (Exception e) {
            TLLog.error("Error generating id hash");
            return "";
        }
    }

    public static void saveMap(HashMap<String, String> inputMap, String key) {
        try {
            SecurePrefs securePrefs = SecurePrefs.getInstance();
            if (securePrefs != null) {
                JSONObject jsonObject = new JSONObject(inputMap);
                String jsonString = jsonObject.toString();
                securePrefs.removeValue(key);
                securePrefs.put(key, jsonString);
            }
        } catch (Throwable e) {
            TLLog.error("Error saving map to memory", e);
        }
    }

    public static HashMap<String, String> loadMap(String prefKey) {
        HashMap<String, String> outputMap = new HashMap<>();
        SecurePrefs pSharedPref = SecurePrefs.getInstance();
        try {
            if (pSharedPref != null) {
                String jsonString = pSharedPref.getAndDecryptString(prefKey);
                if (jsonString != null) {
                    JSONObject jsonObject = new JSONObject(jsonString);
                    Iterator<String> keysItr = jsonObject.keys();
                    while (keysItr.hasNext()) {
                        String key = keysItr.next();
                        String value = (String) jsonObject.get(key);
                        outputMap.put(key, value);
                    }
                }
            }
        } catch (Throwable e) {
            TLLog.error("Error getting map from memory", e);
        }
        return outputMap;
    }

    public static Activity getActivity() {
        try {
            Class activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
            Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
            activitiesField.setAccessible(true);

            Map<Object, Object> activities = (Map<Object, Object>) activitiesField.get(activityThread);
            if (activities == null)
                return null;

            for (Object activityRecord : activities.values()) {
                Class activityRecordClass = activityRecord.getClass();
                Field pausedField = activityRecordClass.getDeclaredField("paused");
                pausedField.setAccessible(true);
                if (!pausedField.getBoolean(activityRecord)) {
                    Field activityField = activityRecordClass.getDeclaredField("activity");
                    activityField.setAccessible(true);
                    return (Activity) activityField.get(activityRecord);
                }
            }
        } catch (Throwable e) {
            return null;
        }
        return null;
    }

}
