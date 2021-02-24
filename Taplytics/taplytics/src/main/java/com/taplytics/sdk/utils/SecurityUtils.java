/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.utils;

import android.util.Base64;

import com.taplytics.sdk.managers.TLManager;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by vicvu on 2016-11-07.
 * <p>
 * Utilities for doing AES things for strings in various places.
 * <p>
 * Note the ECB warning is just a lint error.
 */

public class SecurityUtils {

    static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    static final String KEY_TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final String SECRET_KEY_HASH_TRANSFORMATION = "SHA-256";
    private static final String CHARSET = "UTF-8";

    static IvParameterSpec getInitializationVector(Cipher writer) {
        try {
            String packageName = TLManager.getInstance().getAppContext().getPackageName();
            byte[] iv = new byte[writer.getBlockSize()];
            System.arraycopy(packageName.getBytes(), 0, iv, 0, writer.getBlockSize());
            return new IvParameterSpec(iv);
        } catch (Throwable e) {
            try {
                byte[] iv = new byte[writer.getBlockSize()];
                String packageName = TLManager.getInstance().getAppContext().getPackageName() + ".taplytics.taplytics";
                System.arraycopy(packageName.getBytes(), 0, iv, 0, writer.getBlockSize());
                return new IvParameterSpec(iv);
            } catch (Throwable ignored) {

            }
            TLLog.error("giv", e);
        }
        return null;
    }

    static SecretKeySpec getSecretKey(String key) {
        try {
            byte[] keyBytes = createKeyBytes(key);
            return new SecretKeySpec(keyBytes, TRANSFORMATION);
        } catch (Throwable e) {
            TLLog.error("seck", e);
        }
        return null;
    }

    private static byte[] createKeyBytes(String key) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        try {
            MessageDigest md = MessageDigest.getInstance(SECRET_KEY_HASH_TRANSFORMATION);
            md.reset();
            return md.digest(key.getBytes(CHARSET));
        } catch (Throwable e) {
            TLLog.error("keyb", e);
        }
        return null;
    }

    static String toSecureKey(String key, Cipher keyWriter) {
        try {
            return encrypt(key, keyWriter);
        } catch (Throwable e) {
            TLLog.error("tokey", e);
        }
        return key;
    }

    static String encrypt(String value, Cipher writer) {
        byte[] secureValue;
        try {
            secureValue = convertBytesWithCipher(writer, value.getBytes(SecurityUtils.CHARSET));

            return Base64.encodeToString(secureValue, Base64.NO_WRAP);
        } catch (Throwable e) {
            TLLog.error("encr", e);
            return value;
        }
    }


    static String decryptValueWithCipher(String secureEncryptedValue, Cipher reader) {
        byte[] encryptedValue = Base64.decode(secureEncryptedValue, Base64.NO_WRAP);
        byte[] value = convertBytesWithCipher(reader, encryptedValue);
        try {
            if (value != null) {
                return new String(value, SecurityUtils.CHARSET);
            }
        } catch (Throwable e) {
            TLLog.error("decr", e);
        }
        return null;
    }


    private static byte[] convertBytesWithCipher(Cipher cipher, byte[] bs) {
        try {
            return cipher.doFinal(bs);
        } catch (Throwable e) {
            //
        }
        return null;
    }

    public static String decodeBase64String(String string) {
        try {
            byte[] data = Base64.decode(string, Base64.DEFAULT);
            return new String(data, "UTF-8");
        } catch (Throwable e) {
            return string;
        }
    }

    static boolean decodeSQLQueries() {
        try {
            if (sqlQueries.length == 8) {
                String[] copy = new String[sqlQueries.length + 1];
                for (int i = 0; i < sqlQueries.length; i++) {
                    copy[i] = decodeBase64String(sqlQueries[i]);
                }
                copy[8] = "done";
                sqlQueries = copy;
                return true;
            } else {
                return false;
            }

        } catch (Throwable t) {
            TLLog.error("e sql q dsc", t);
        }
        return true;
    }

    /**
     * [0] = TLEventTable
     * [1] = CREATE TABLE IF NOT EXISTS
     * [2] = (timestamp long, event TEXT)
     * [3] = DROP TABLE IF EXISTS
     * [4] = DELETE FROM
     * [5] = WHERE timestamp IN (SELECT timestamp FROM
     * [6] = ORDER BY timestamp ASC LIMIT %d)
     * [7] = SELECT Count(1) FROM
     */
    static String[] sqlQueries = new String[]{"VExFdmVudFRhYmxl", "Q1JFQVRFIFRBQkxFIElGIE5PVCBFWElTVFM=", "KHRpbWVzdGFtcCBsb25nLCBldmVudCBURVhUKQ==", "RFJPUCBUQUJMRSBJRiBFWElTVFM=", "REVMRVRFIEZST00=", "V0hFUkUgdGltZXN0YW1wIElOIChTRUxFQ1QgdGltZXN0YW1wIEZST00=", "T1JERVIgQlkgdGltZXN0YW1wIEFTQyBMSU1JVCAlZCk=", "U0VMRUNUIENvdW50KDEpIEZST00="};

    public static String getSHA256Hash(String token) {
        try {
            MessageDigest digest = null;
            try {
                digest = MessageDigest.getInstance("SHA-256");
            } catch (Throwable e1) {
                TLLog.error("Error getting hash", e1);
            }
            digest.reset();
            return bin2hex(digest.digest(token.getBytes())).toLowerCase();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String bin2hex(byte[] data) {
        StringBuilder hex = new StringBuilder(data.length * 2);
        for (byte b : data)
            hex.append(String.format("%02x", b & 0xFF));
        return hex.toString();
    }
}
