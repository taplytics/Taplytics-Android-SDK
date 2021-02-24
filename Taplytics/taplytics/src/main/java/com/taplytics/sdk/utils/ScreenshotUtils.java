/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.utils;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;
import android.view.View;

import com.taplytics.sdk.managers.TLViewManager;

import org.json.JSONException;

import java.io.ByteArrayOutputStream;

/**
 * Created by VicV on 10/6/14.
 * <p/>
 * Utilities for capturing screenshots.
 */
public class ScreenshotUtils {

    public static String captureScreenshot(View view) throws JSONException {
        String fileName = null;
        try {
            Bitmap bitmap;
            view.setDrawingCacheEnabled(true);

            Bitmap cache = view.getDrawingCache();

            //Sometimes, the cache just doesn't exist. Especially for imagebuttons.
            if (cache == null) {
                bitmap = getViewBitmapFromCanvas(view);
            } else {
                bitmap = Bitmap.createBitmap(cache);
            }

            view.setDrawingCacheEnabled(false);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            Bitmap.CompressFormat compressionFormat;

            //Generally we can't get here if a view doesn't have an id. Sometimes its possible (system views or text views).
            //The filename is the view's id, otherwise a hashcode of the view's classname.
            int filename = ViewUtils.getOrMakeViewId(view)!=-1?ViewUtils.getOrMakeViewId(view):view.getClass().getSimpleName().hashCode();

            //Safety: if the size is going to be huge, use a jpeg instead of a png.
            if (getBitMapSize(bitmap) > 750000) {
                compressionFormat = Bitmap.CompressFormat.JPEG;
                fileName = String.valueOf(filename) + ".jpg";
            } else {
                compressionFormat = Bitmap.CompressFormat.PNG;
                fileName = String.valueOf(filename) + ".png";
            }

            bitmap.compress(compressionFormat, 60, outputStream);
            byte[] imageBytes = outputStream.toByteArray();
            TLViewManager.getInstance().addToImages(fileName, imageBytes);

        } catch (Exception e) {
            TLLog.error("Error creating screenshot: ", e);
        }

        return fileName;
    }

    /**
     * If we don't have a drawing cache, we have to draw the view directly to a canvas.
     */
    public static Bitmap getViewBitmapFromCanvas(View v) {
        Bitmap bitmap = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        v.layout(0, 0, v.getWidth(), v.getHeight());
        v.draw(canvas);
        return bitmap;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    private static int getBitMapSize(Bitmap data) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1) {
                return data.getRowBytes() * data.getHeight();
            } else {
                return data.getByteCount();
            }
        } catch (Exception e) {
            TLLog.error(ScreenshotUtils.class.getSimpleName(), e);
        }
        //If we fail somehow, assume we want to use JPEG just to be safe.
        return 800000;
    }
}
