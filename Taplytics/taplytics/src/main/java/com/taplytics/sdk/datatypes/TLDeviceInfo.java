/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.datatypes;

import android.annotation.TargetApi;
import android.app.UiModeManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import com.taplytics.sdk.managers.TLManager;
import com.taplytics.sdk.utils.SecurePrefs;
import com.taplytics.sdk.utils.TLLog;
import com.taplytics.sdk.utils.TLUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import static android.content.Context.UI_MODE_SERVICE;

public class TLDeviceInfo {

    private static final String ID_MAP_PREFERENCE_KEY = "ID_MAP";
    static final String AMAZON_FEATURE_FIRE_TV = "amazon.hardware.fire_tv";

    private final Context context;
    private final String appName;
    private final String appIdentifier;
    private final String appVersionName;
    private final Integer appVersionCode;
    private final DisplayMetrics displayMetrics;
    private String pid;
    private HashMap<String, String> adID = null;

    public TLDeviceInfo(Context context) {
        this.context = context;

        PackageManager packageManager = context.getPackageManager();
        String getAppName = null;
        String getAppIdentifier = null;
        String getAppVersionName = null;
        Integer getAppVersionCode = null;
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            ApplicationInfo info = packageManager.getApplicationInfo(context.getApplicationInfo().processName, 0);
            if (info != null) {
                getAppName = (String) packageManager.getApplicationLabel(info);
                getAppIdentifier = info.processName;
            }
            if (packageInfo != null) {
                getAppVersionName = packageInfo.versionName;
                getAppVersionCode = packageInfo.versionCode;
            }
        } catch (Exception e) {
            TLLog.warning("System Context does not exist: " + e.toString());
        }
        getUniqueDeviceIdFromDisk();
        this.appName = getAppName;
        this.appVersionName = getAppVersionName;
        this.appVersionCode = getAppVersionCode;
        this.appIdentifier = getAppIdentifier;

        this.displayMetrics = new DisplayMetrics();
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        if (display != null) {
            display.getMetrics(displayMetrics);
        }
    }

    public void addDevicePropertiesToURL(StringBuilder url) {
        TLUtils.addURLParamsFromDic(getDeviceProperties(), url);
    }

    public StringBuilder getDeviceProperties(StringBuilder url) {
        return TLUtils.getUrlParamStringFromDic(getDeviceProperties(), url);
    }

    public Map<String, Object> getDeviceProperties() {
        Map<String, Object> dic = new HashMap<>();
        TLManager tlManager = TLManager.getInstance();

        dic.put("lv", (tlManager.isLiveUpdate()) ? "1" : "0");
        if (tlManager.isLiveUpdateSetByDev())
            dic.put("dev", "1");
        if (tlManager.getApiKey() != null)
            dic.put("t", TLManager.getInstance().getApiKey());
        if (appName != null)
            dic.put("an", appName);
        if (appVersionName != null)
            dic.put("av", appVersionName);
        if (appVersionCode != null)
            dic.put("ab", appVersionCode);
        if (appIdentifier != null)
            dic.put("ai", appIdentifier);

        dic.put("sdk", TLManager.TAPLYTICS_SDK_VERSION.get());
        setOSAndCurrentModeTypeForDeviceInfo(dic);
        dic.put("osv", (Build.VERSION.RELEASE != null) ? Build.VERSION.RELEASE : "unknown");
        dic.put("ma", Build.MANUFACTURER == null ? "UNKNOWN" : Build.MANUFACTURER);
        dic.put("br", Build.BRAND == null ? "UNKNOWN" : Build.BRAND);
        dic.put("d", Build.MODEL == null ? "UNKNOWN" : Build.MODEL);
        Locale l = Locale.getDefault();
        if (l != null) {
            try {
                if (Build.VERSION.SDK_INT >= 21) {
                    dic.put("alg", l.toLanguageTag());
                } else {
                    dic.put("alg", l.getLanguage());
                }
                dic.put("alg3", l.getISO3Language());
                dic.put("con", l.getCountry());
                dic.put("con3", l.getISO3Country());
            } catch (Throwable e) {
                //
            }
        }
        if (displayMetrics != null) {
            try {
                dic.put("sdpi", displayMetrics.densityDpi);
                dic.put("sh", displayMetrics.heightPixels);
                dic.put("sw", displayMetrics.widthPixels);
            } catch (Throwable e) {
                //
            }
        }
        TimeZone z = TimeZone.getDefault();
        if (z != null) {
            try {
                dic.put("tz", z.getDisplayName(false, 0));
                dic.put("tzs", z.getOffset(Calendar.getInstance().getTimeInMillis()) / 1000);
                dic.put("tzn", z.getID());
            } catch (Throwable e) {
                //
            }
        }
        try {
            String carrier = getDeviceCarrier();
            if (carrier != null)
                dic.put("ca", carrier);
            String radio = getPhoneRadioType();
            if (radio != null)
                dic.put("rd", radio);
            dic.put("n", getNetworkType());

        } catch (Throwable e) {
            //
        }
        Map<String, String> deviceIDDic = getDeviceUniqueID();
        if (deviceIDDic != null) {
            dic.put("ad", new JSONObject(deviceIDDic));
        }

        TLProperties properties = tlManager.getTlProperties();
        if (properties != null) {
            JSONObject project = properties.getProject();
            JSONObject appUser = properties.getAppUser();
            String session = properties.getSessionID();
            if (project != null && project.has("_id")) {
                try {
                    dic.put("pid", project.getString("_id"));
                } catch (JSONException e) {
                    TLLog.error("Getting project_id", e);
                }
            }
            if (appUser != null && appUser.has("_id")) {
                try {
                    dic.put("au", appUser.getString("_id"));
                } catch (JSONException e) {
                    TLLog.error("Getting appUser_id", e);
                }
            }
            if (session != null)
                dic.put("sid", session);
        } else if (pid != null && !pid.equals("")) {
            dic.put("pid", pid);
        }

        String deviceToken = tlManager.getDeviceToken();
        if (!TLUtils.isEmptyString(deviceToken)) {
            dic.put("dt", deviceToken);
        }
        return dic;
    }

    // Created separate method to run tests. Unable to mock UiModeManager
    int getCurrentModeType() {
        UiModeManager uiModeManager = (UiModeManager)context.getSystemService(UI_MODE_SERVICE);
        return uiModeManager != null ? uiModeManager.getCurrentModeType() : -1;
    }

    void setOSAndCurrentModeTypeForDeviceInfo(Map<String, Object> dic) {
        int currentModeType = getCurrentModeType();
        if (currentModeType >= 0) {
            dic.put("cmt", currentModeType);
        }

        boolean isFireTV = context.getPackageManager().hasSystemFeature(AMAZON_FEATURE_FIRE_TV);
        boolean isAndroidTV = false;
        if (Build.VERSION.SDK_INT >= 13) {
            isAndroidTV = currentModeType == Configuration.UI_MODE_TYPE_TELEVISION;
        }

        if (isFireTV) {
            dic.put("os", "Fire TV");
        } else if (isAndroidTV) {
            dic.put("os", "Android TV");
        } else {
            dic.put("os", "Android");
        }
    }

    //Force a read from prefs, set the adID, hopefully before anything else happens.
    private void getUniqueDeviceIdFromDisk() {
        try {
            if (this.adID == null) {
                if (TLManager.getInstance().getAppContext() != null) {
                    //Make our preferences
                    SecurePrefs prefs = SecurePrefs.getInstance();
                    if (prefs == null || prefs.getAndDecryptString(ID_MAP_PREFERENCE_KEY) == null) {
                        this.adID = createAndSetDeviceUniqueID();
                        return;
                    }
                    //Grab our old stuff
                    String customTrackingId = TLManager.getInstance().getTrackingId();
                    HashMap<String, String> idMap = TLUtils.loadMap(ID_MAP_PREFERENCE_KEY);

                    if (customTrackingId != null && !idMap.get("anAndroidID").equals(customTrackingId)) {
                        idMap = createAndSetDeviceUniqueID();
                    }

                    //If either are null just do it manually.
                    if (idMap == null || idMap.size() == 0) {
                        this.adID = createAndSetDeviceUniqueID();
                    } else {
                        this.adID = idMap;
                    }
                }
            }
        } catch (Throwable e) {
            TLLog.error("gdevid", e);
        }
    }

    private HashMap<String, String> createAndSetDeviceUniqueID() {
        if (this.adID != null) {
            return this.adID;
        }
        HashMap deviceDic = new HashMap<>();

        try {
            String deviceID;
            try {
                if (TLUtils.checkHasAndroidPermission(context, "android.permission.READ_PHONE_STATE")) {
                    deviceID = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
                    if (deviceID != null) {
                        deviceDic.put("UUID", deviceID);
                    }
                }
            } catch (Exception e) {
                //
            }

            String trackingId = TLManager.getInstance().getTrackingId();
            if (trackingId == null) {
                try {
                    if (Build.VERSION.SDK_INT >= 9) {
                        deviceID = getBuildSerial();
                        if (deviceID != null) {
                            deviceDic.put("anBuildSerial", deviceID);
                        }
                    }
                } catch (Exception e) {}

                try {
                    deviceID = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
                    if (deviceID != null) {
                        deviceDic.put("anAndroidID", deviceID);
                    }
                } catch (Exception e) {}
            } else {
                deviceDic.put("anBuildSerial", "unknown");
                deviceDic.put("anAndroidID", trackingId);
            }


            //If this is a test case, we want to make this a brand new device.
            if (TLManager.getInstance().isTest()) {
                UUID id = UUID.randomUUID();
                deviceDic.put("TEST_UUID", id.toString());
            }

            try {
                //Create a generateDeviceDicHash of what we have.
                String deviceIdHash = TLUtils.generateDeviceDicHash(deviceDic);
                deviceDic.put("hash", deviceIdHash);
            } catch (Exception e) {
//
            }

            try {
                //If we're still null, just set it to a random UUID.
                UUID id = UUID.randomUUID();
                deviceDic.put("TL_UUID", id.toString());
            } catch (Exception e) {
                //
            }
            if (TLManager.getInstance().isTest()) {
                deviceDic.remove("anAndroidID");
                deviceDic.remove("anBuildSerial");
                deviceDic.remove("anBuildSerial");
                deviceDic.remove("UUID");
            }

            this.adID = deviceDic;

            TLUtils.saveMap(deviceDic, ID_MAP_PREFERENCE_KEY);

        } catch (Exception e) {
            TLLog.warning("Problem getting unique ID", e);
            TLUtils.saveMap(deviceDic, ID_MAP_PREFERENCE_KEY);
            this.adID = deviceDic;
            return deviceDic;
        }

        TLUtils.saveMap(deviceDic, ID_MAP_PREFERENCE_KEY);
        this.adID = deviceDic;
        return deviceDic;
    }

    /**
     * @return Unique device ID. If it doesn't exist, this calls {@link #createAndSetDeviceUniqueID()} which returns one.
     */
    public Map<String, String> getDeviceUniqueID() {
        if (adID != null) {
            return adID;
        }

        return createAndSetDeviceUniqueID();
    }

    @TargetApi(9)
    private String getBuildSerial() {
        return Build.SERIAL;
    }

    private String getPhoneRadioType() {
        String radio = null;
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (null != telephonyManager) {
                switch (telephonyManager.getPhoneType()) {
                    case 0x00000000: // TelephonyManager.PHONE_TYPE_NONE
                        radio = "none";
                        break;
                    case 0x00000001: // TelephonyManager.PHONE_TYPE_GSM
                        radio = "gsm";
                        break;
                    case 0x00000002: // TelephonyManager.PHONE_TYPE_CDMA
                        radio = "cdma";
                        break;
                    case 0x00000003: // TelephonyManager.PHONE_TYPE_SIP
                        radio = "sip";
                        break;
                    default:
                        radio = null;
                }
            }
        } catch (Throwable e) {
            //
        }
        return radio;
    }

    private String getDeviceCarrier() {
        String carrier = null;
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (null != telephonyManager)
                carrier = telephonyManager.getNetworkOperatorName();
        } catch (Throwable e) {
            //
        }
        return carrier;
    }

    private String getNetworkType() {
        String network_type = "UNKNOWN";
        try {
            if (TLUtils.checkHasAndroidPermission(context, "android.permission.ACCESS_NETWORK_STATE")) {
                ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo network = connectivityManager.getActiveNetworkInfo();
                if (network != null && network.isConnectedOrConnecting()) {
                    if (network.getType() == ConnectivityManager.TYPE_WIFI)
                        network_type = "WIFI";
                    else if (network.getType() == ConnectivityManager.TYPE_MOBILE)
                        network_type = "WWAN";
                }
            } else {
                TLLog.error("Error No ACCESS_NETWORK_STATE permissions to get Network type");
            }
        } catch (Throwable e) {
            //
        }
        return network_type;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }
}
