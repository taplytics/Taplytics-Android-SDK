/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.taplytics.sdk.datatypes.TLProperties;
import com.taplytics.sdk.managers.TLManager;
import com.taplytics.sdk.utils.TLReaderWriter.TLWriterListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static com.taplytics.sdk.utils.SecurityUtils.TRANSFORMATION;
import static com.taplytics.sdk.utils.SecurityUtils.decryptValueWithCipher;
import static com.taplytics.sdk.utils.SecurityUtils.encrypt;
import static com.taplytics.sdk.utils.SecurityUtils.getInitializationVector;
import static com.taplytics.sdk.utils.SecurityUtils.getSecretKey;
import static com.taplytics.sdk.utils.TLReaderWriter.writerCipher;

public class TLReaderWriter {

    public interface TLWriterListener {
        void callback(Exception e);
    }

    public interface TLReaderListener {
        void callback(Object json, Exception e);
    }

    public interface TLDeleteFileListener {
        void callback(Exception e);
    }

    public interface TLPropertiesReaderListener {
        void callback(TLProperties properties, Exception e);
    }

    public interface TLEventsReaderListener {
        void callback(JSONArray events, Exception e);
    }

    public interface TLUserAttributesReaderListener {
        void callback(JSONObject attributes, Exception e);
    }

    private static boolean init = false;

    private static TLReaderWriter instance = null;

    public static TLReaderWriter getInstance() {
        instance = (instance == null || !init) ? new TLReaderWriter() : instance;
        return instance;
    }

    static Cipher getWriterCipher() {
        return writerCipher;
    }

    static Cipher writerCipher;

    public static Cipher getReaderCipher() {
        return readerCipher;
    }

    private static Cipher readerCipher;

    private TLReaderWriter() {
        try {
            writerCipher = Cipher.getInstance(TRANSFORMATION);
            readerCipher = Cipher.getInstance(TRANSFORMATION);
            initCiphers(TLManager.getInstance().getApiKey());
        } catch (Exception e) {
            //
        }
    }

    private void initCiphers(String secureKey) {
        try {

            IvParameterSpec ivSpec = getInitializationVector(writerCipher);
            SecretKeySpec secretKey = getSecretKey(secureKey);

            writerCipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            readerCipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            init = true;
        } catch (Throwable e) {
//            TLLog.error("ciphinit", e);
        }
    }

    private static final String PROP_FILE_NAME = "TLProperties.json";

    public void writePropertiesToDisk(JSONObject json) {
        Context context = TLManager.getInstance().getAppContext();
        if (context == null) {
            TLLog.debug("Writing Properties to Disk: No App Context to write JSON to disk");
            return;
        }

        TLUtils.executeAsyncTask(new TLWriterTask(json, PROP_FILE_NAME, context.getFilesDir(), new TLWriterListener() {
            @Override
            public void callback(Exception e) {
                if (e != null)
                    TLLog.warning("Writing JSON to Disk: ", e);
            }
        }));
    }

    public void readTLPropertiesFromDisk(final TLPropertiesReaderListener listener) {
        if (listener == null) {
            TLLog.debug("No listener to read TLProperties from disk");
            return;
        }

        Context context = TLManager.getInstance().getAppContext();
        if (context == null) {
            listener.callback(null, new Exception("No App Context to write JSON to disk"));
            return;
        }

        readFileFromDisk(PROP_FILE_NAME, "JSONObject", new TLReaderListener() {
            @Override
            public void callback(Object obj, Exception e) {
                if (e != null)
                    listener.callback(null, e);
                else if (obj == null)
                    listener.callback(null, null);
                else {
                    try {
                        JSONObject json = (JSONObject) obj;
                        String oldSid = null;
                        if (json.has("sid")) {
                            oldSid = (String) json.remove("sid");
                        }
                        TLProperties prop = new TLProperties(json);
                        if (oldSid != null) {
                            prop.setLastSessionId(oldSid);
                        }
                        listener.callback(prop, null);
                    } catch (Exception e1) {
                        listener.callback(null, e1);
                    } catch (Throwable e2) {
                        listener.callback(null, new Exception());
                    }
                }
            }
        });
    }

    public void deleteTLPropertiesFileFromDisk() {
        TLLog.debug("Delete TLProperties File from disk");
        deleteFileFromDisk(PROP_FILE_NAME, new TLDeleteFileListener() {
            @Override
            public void callback(Exception e) {
                if (e != null)
                    TLLog.warning("Deleting TLProperties From Disk", e);
            }
        });
    }

    /**
     * @param fileName name of file cannot be null
     * @param listener listener with callback for when deletion completes.
     */
    private void deleteFileFromDisk(@NonNull String fileName, final TLDeleteFileListener listener) {
        try {
            Context context = TLManager.getInstance().getAppContext();
            if (context == null) {
                if (listener != null) {
                    listener.callback(new Exception("No App Context to write JSON to disk"));
                }
                return;
            }

            Date time = new Date();
            File file = new File(context.getFilesDir(), fileName);
            if (file.exists()) {
                boolean deleted = file.delete();
                if (deleted)
                    TLLog.debug("Deleted File", time);
            }

            //SUPER SAFETY: Delete old cachefile regardless just in case.
            File cacheFile = new File(context.getCacheDir(), fileName);
            if (cacheFile.exists()) {
                boolean deleted = file.delete();
                if (deleted)
                    TLLog.debug("Deleted File", time);
            }

            if (listener != null) {
                listener.callback(null);
            }
        } catch (Throwable t) {
            //
        }
    }

    private static final String USER_ATTRIBUTES_FILE_NAME = "TLUserAttributes.json";

    public void writeTLAppUserAttributes(JSONObject attributes) {
        Context context = TLManager.getInstance().getAppContext();
        if (context == null) {
            TLLog.debug("Writing User Attributes to Disk: No App Context to write JSON to disk");
            return;
        }

        TLUtils.executeAsyncTask(new TLWriterTask(attributes, USER_ATTRIBUTES_FILE_NAME, context.getFilesDir(), new TLWriterListener() {
            @Override
            public void callback(Exception e) {
                if (e != null)
                    TLLog.warning("Writing JSON to Disk: ", e);
            }
        }));
    }

    public void readTLAppUserAttributesFromDisk(final TLUserAttributesReaderListener listener) {
        if (listener == null) {
            TLLog.debug("No listener to read App User Attributes from disk");
            return;
        }
        Context context = TLManager.getInstance().getAppContext();
        if (context == null) {
            listener.callback(null, new Exception("No App Context to read App User Attributes from Disk!"));
            return;
        }

        readFileFromDisk(USER_ATTRIBUTES_FILE_NAME, "JSONObject", new TLReaderListener() {
            @Override
            public void callback(Object json, Exception e) {
                if (e != null)
                    listener.callback(null, e);
                else if (json == null)
                    listener.callback(null, null);
                else {
                    try {
                        listener.callback((JSONObject) json, null);
                    } catch (Exception e1) {
                        listener.callback(null, e1);
                    }
                }
            }
        });
    }

    public void deleteTLAppUserAttributesFromDisk() {
        TLLog.debug("Delete App User Attributes From Disk");
        deleteFileFromDisk(USER_ATTRIBUTES_FILE_NAME, new TLDeleteFileListener() {
            @Override
            public void callback(Exception e) {
                if (e != null)
                    TLLog.warning("Deleting App User Attributes from disk: " + e.getMessage());
            }
        });
    }

    private void readFileFromDisk(String fileName, String fileType, final TLReaderListener listener) {
        if (fileName == null) {
            Exception e = new Exception("Missing fileName or listener to read from disk");
            TLLog.warning("Reading item from disk: ", e);
            listener.callback(null, e);
            return;
        }

        Context context = TLManager.getInstance().getAppContext();
        Date time = new Date();
        File file = new File(context.getFilesDir(), fileName);

        //UPGRADE PATH -- WE HAVE SWITCHED FROM CACHE DIR TO FILES DIR.

        //First check if the file exists in the files dir.
        boolean firstCheck = file.exists();
        boolean oldExists;
        if (!firstCheck) {
            //if it does not exist, check if if it exists in the cache directory
            File cacheCheck = new File(context.getCacheDir(), fileName);
            oldExists = cacheCheck.exists();

            //if it does, read from there.
            if (oldExists) {
                file = cacheCheck;
            }

        }

        if (file.exists()) {
            try {
                ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
                Object object = in.readObject();
                Object returnObject = null;
                switch (fileType) {
                    case "JSONObject":
                        try {
                            returnObject = new JSONObject(decryptValueWithCipher((String) object, readerCipher));
                        } catch (Throwable e) {
                            returnObject = new JSONObject((String) object);
                        }
                        break;
                    case "JSONArray":
                        try {
                            returnObject = new JSONArray(decryptValueWithCipher((String) object, readerCipher));
                        } catch (Throwable e) {
                            returnObject = new JSONArray((String) object);
                        }
                        break;
                    case "Serializable":
                        returnObject = object;
                        break;
                }
                listener.callback(returnObject, null);
                TLLog.debug("Read item from disk: " + fileType, time);
                in.close();
            } catch (Exception e) {
                TLLog.warning("Reading file from disk: ", e);
                listener.callback(null, e);
            }
        } else {
            TLLog.debug("File does not exist");
            listener.callback(null, new Exception("File does not exist"));
        }
    }

}

class TLWriterTask extends AsyncTask<Void, Void, Void> {
    private JSONObject jsonObject;
    private JSONArray jsonArray;
    private Object serializable;
    private String fileName;
    private File filesDir;
    private final TLWriterListener listener;
    private Exception ex = null;

    public TLWriterTask(Object object, String fileName, File filesDir, final TLWriterListener listener) {
        if (object instanceof JSONObject) {
            this.jsonObject = (JSONObject) object;
        } else if (object instanceof JSONArray) {
            this.jsonArray = (JSONArray) object;
        } else if (object instanceof Serializable) {
            this.serializable = object;
        }
        this.fileName = fileName;
        this.filesDir = filesDir;
        this.listener = listener;
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            writeItemToDisk();
        } catch (Exception e) {
            this.ex = e;
        }
        return null;
    }


    @Override
    protected void onPostExecute(Void result) {
        listener.callback(ex);
    }

    private void writeItemToDisk() throws Exception {
        if ((jsonObject == null && jsonArray == null && serializable == null) || fileName == null || listener == null) {
            TLLog.warning("Missing json, fileName, or listener to write JSON to disk");
            return;
        }
        File file = new File(filesDir, fileName);

        ObjectOutput out = new ObjectOutputStream(new FileOutputStream(file));

        try {
            Object toWrite;
            Date time = new Date();
            if (serializable != null) {
                toWrite = serializable;
            } else {
                toWrite = (jsonObject != null) ? jsonObject.toString() : jsonArray.toString();
                try {
                    toWrite = encrypt((String) toWrite, writerCipher);
                } catch (Throwable ignored) {
                }
            }
            out.writeObject(toWrite);
            out.close();
            TLLog.debug("Wrote JSON to Disk", time);
        } catch (Throwable e) {
            TLLog.warning("RW err", (e instanceof Exception) ? (Exception) e : null);
            out.close();

        }
    }

}
