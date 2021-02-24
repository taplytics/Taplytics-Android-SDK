/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.utils;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.view.ViewGroup;

import com.taplytics.sdk.managers.TLManager;
import com.taplytics.sdk.managers.TLViewManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Iterator;

/**
 * Created by VicV on 11/12/14.
 */
public class ImageUtils {

    public static final int IMAGE_PATH_KEY = 2 << 24;
    public static final int IMAGE_DRAWABLE_KEY = 2 + 2 << 24;
    public static final int IMAGE_BITMAP_KEY = 3 + 2 << 24;

    public enum DPI {
        ldpi, mdpi, tvdpi, hdpi, xhdpi, xxhdpi, xxxhdpi
    }

    public static File saveImageToDisk(String imageUrl, String filename) {
        InputStream is = null;
        OutputStream os = null;
        File file = null;
        try {
            file = new File(TLManager.getInstance().getAppContext().getFilesDir(), filename);

            if (!file.exists() && !file.isDirectory()) {

                URL url = new URL(imageUrl);
                is = url.openStream();

                os = new FileOutputStream(file);

                byte[] b = new byte[2048];
                int length;

                while ((length = is.read(b)) != -1) {
                    os.write(b, 0, length);
                }

                is.close();
                os.close();
                return file;
            }
        } catch (Throwable e) {
            TLLog.error("Saving image to disk failed", e);
            try {
                if (is != null) {
                    is.close();
                }
                if (os != null) {
                    os.close();
                }
            } catch (Throwable io) {
                TLLog.error("something has occurred in nested io catch", io);
            }
        }
        return file;
    }

    public static class SaveImageToDiskTask extends AsyncTask<JSONObject, Void, Void> {

        @Override
        protected final Void doInBackground(JSONObject[] params) {

            try {
                JSONObject images = params[0];
                Iterator<String> keys = images.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    saveImageToDisk(images.optJSONObject(key).optString("path"), images.optJSONObject(key).optString("filename"));
                }
                ViewGroup vg = TLViewManager.getInstance().getCurrentViewGroup();
                if (TLManager.getInstance().getCurrentActivity() != null
                        && TLManager.getInstance().isLiveUpdate()
                        && vg != null) {
                    ViewUtils.setProperties(vg);
                }
            } catch (Exception e) {
                TLLog.error("error saving image to disk in task", e);
            }
            return null;
        }
    }

    public static File getImageFile(Object value) {
        try {
            JSONArray images = (JSONArray) value;
            DPI dpi = DPI.valueOf(getScreenDensityString());
            DPI largestDPI = null;
            String largestFilename = "";
            String filename = "";
            for (int i = 0; i < images.length(); i++) {
                JSONObject image = TLManager.getInstance().getTlProperties().getImages()
                        .optJSONObject(((JSONObject) images.get(i)).optString("image_id"));
                JSONArray tags = image.optJSONArray("deviceTags");
                for (int j = 0; j < tags.length(); j++) {
                    DPI tag = DPI.valueOf(tags.optString(j));
                    if (tag.equals(dpi)) {
                        filename = image.optString("filename");
                        break;
                    }
                    if (largestDPI == null) {
                        largestDPI = tag;
                        largestFilename = image.optString("filename");
                    } else if (tag.ordinal() > largestDPI.ordinal()) {
                        largestDPI = tag;
                        largestFilename = image.optString("filename");
                    }
                }
                if (!filename.equals("")) {
                    break;
                }
            }
            if (filename.equals("")) {
                filename = largestFilename;
            }
            return new File(TLManager.getInstance().getAppContext().getFilesDir(), filename);
        } catch (Exception e) {
            TLLog.error("error retriving file: ", e);
            return new File("sofuh28yr2jkjdkjadal");
        }
    }

    public static String getScreenDensityString() {
        String dpi = "";
        // Note: Not a switch because its a float value
        float val = TLManager.getInstance().getAppContext().getResources().getDisplayMetrics().density;
        if (val >= 0.75) {
            dpi = "ldpi";
        } else if (val == 1) {
            dpi = "mdpi";
        } else if (val > 1 && val < 1.5) {
            dpi = "tvdpi";
        } else if (val == 1.5) {
            dpi = "hdpi";
        } else if (val == 2) {
            dpi = "xhdpi";
        } else if (val == 3) {
            dpi = "xxhdpi";
        } else if (val == 4) {
            dpi = "xxxhdpi";
        }
        return dpi;
    }

    public static Drawable getAppIcon() {
        try {
            //Get package manager
            PackageManager pMan = TLManager.getInstance().getAppContext().getPackageManager();
            //Pull app icon from context with package manager
            // NOTE: You'll see this error out in the logs pretty much every single time.
            // Ignore it unless you see the actual TLLog saying "problem getting app icon".
            // Trust me. This works.
            Drawable icon = TLManager.getInstance().getAppContext().getApplicationInfo().loadIcon(pMan);
            if (icon != null) {
                return icon;
            }
        } catch (Exception e) {
            TLLog.error("problem getting app icon");
        }
        return null;
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        try {
            if (drawable instanceof BitmapDrawable) {
                return ((BitmapDrawable) drawable).getBitmap();
            }

            //Define our bounds based on the icon
            final int width = !drawable.getBounds().isEmpty() ? drawable
                    .getBounds().width() : drawable.getIntrinsicWidth();
            final int height = !drawable.getBounds().isEmpty() ? drawable
                    .getBounds().height() : drawable.getIntrinsicHeight();

            //Just make the bitmap now!
            final Bitmap bitmap = Bitmap.createBitmap(width <= 0 ? 1 : width,
                    height <= 0 ? 1 : height, Bitmap.Config.ARGB_8888);

            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);

            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }


    public static byte[] getAppIconBytes() {
        byte[] bytes = null;
        try {
            Drawable icon = ImageUtils.getAppIcon();
            if (icon != null) {
                //Convert to bitmap
                Bitmap bitmap = ImageUtils.drawableToBitmap(icon);
                //Output to bytes
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                if (bitmap != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                } else {
                    return null;
                }
                bytes = stream.toByteArray();
            }
        } catch (Exception e) {
            return null;
        }
        return bytes;
    }
}
