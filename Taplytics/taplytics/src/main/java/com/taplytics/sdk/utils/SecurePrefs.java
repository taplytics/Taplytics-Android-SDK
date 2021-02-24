/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.taplytics.sdk.managers.TLManager;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static com.taplytics.sdk.utils.SecurityUtils.KEY_TRANSFORMATION;
import static com.taplytics.sdk.utils.SecurityUtils.TRANSFORMATION;
import static com.taplytics.sdk.utils.SecurityUtils.decryptValueWithCipher;
import static com.taplytics.sdk.utils.SecurityUtils.encrypt;
import static com.taplytics.sdk.utils.SecurityUtils.getInitializationVector;
import static com.taplytics.sdk.utils.SecurityUtils.getSecretKey;
import static com.taplytics.sdk.utils.SecurityUtils.toSecureKey;


public class SecurePrefs {

    private static SecurePrefs instance = null;

    private Cipher writer;
    private Cipher reader;
    private Cipher keyWriter;
    private SharedPreferences preferences;

    public SecurePrefs() {}

    private SecurePrefs(Context appContext, String preferenceKeySecure, String apiKey) {
        init(appContext, preferenceKeySecure, apiKey);
    }

    /**
     * This will initialize an instance of the SecurePrefs class
     *
     * @param context        your current context.
     * @param preferenceName name of preferences file (preferenceName.xml)
     * @param secureKey      the key used for encryption, finding a good key scheme is hard.
     *                       Hardcoding your key in the application is bad, but better than plaintext preferences. Having the user enter the key upon application launch is a safe(r) alternative, but annoying to the user.
     */
    public void init(Context context, String preferenceName, String secureKey) {
        try {
            this.writer = Cipher.getInstance(TRANSFORMATION);
            this.reader = Cipher.getInstance(TRANSFORMATION);
            this.keyWriter = Cipher.getInstance(KEY_TRANSFORMATION);
            initCiphers(secureKey);

            this.preferences = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE);
        } catch (Throwable e) {
            TLLog.error("secpf init", e);
        }
    }

    private void initCiphers(String secureKey) {
        try {
            IvParameterSpec initializationVectorSpec = getInitializationVector(writer);
            SecretKeySpec secretKey = getSecretKey(secureKey);

            writer.init(Cipher.ENCRYPT_MODE, secretKey, initializationVectorSpec);
            reader.init(Cipher.DECRYPT_MODE, secretKey, initializationVectorSpec);
            keyWriter.init(Cipher.ENCRYPT_MODE, secretKey);
        } catch (Throwable e) {
            TLLog.error("ciphinit", e);
        }
    }


    public void put(String key, String value) {
        try {
            if (value == null) {
                preferences.edit().remove(toSecureKey(key, keyWriter)).apply();
            } else {
                putValue(toSecureKey(key, keyWriter), value);
            }
        } catch (Throwable ignored) {

        }
    }

    public boolean containsKey(String key) {
        return preferences.contains(toSecureKey(key, keyWriter));
    }

    public void removeValue(String key) {
        preferences.edit().remove(toSecureKey(key, keyWriter)).apply();
    }

    public String getAndDecryptString(String key) {
        try {
            if (preferences.contains(toSecureKey(key, keyWriter))) {
                String securedEncodedValue = preferences.getString(toSecureKey(key, keyWriter), "");
                return decryptValueWithCipher(securedEncodedValue, reader);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public void clear() {
        try {
            preferences.edit().clear().apply();
        } catch (Throwable ignored) {

        }
    }

    private void putValue(String key, String value) {
        try {
            String secureValueEncoded = encrypt(value, writer);

            preferences.edit().putString(key, secureValueEncoded).apply();
        } catch (Throwable e) {
            TLLog.error("ptv", e);
        }
    }


    public static SecurePrefs getInstance() {
        if (instance == null) {
            try {
                instance = new SecurePrefs(TLManager.getInstance().getAppContext(), TLUtils.PREFERENCE_KEY_SECURE, TLManager.getInstance().getApiKey());
            } catch (Throwable ignored) {
            }
        }
        return instance;
    }
}
