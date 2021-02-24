/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.utils;

import android.graphics.Color;

import org.json.JSONObject;

public class DataUtils {

    public static Integer getColorFromJSONObject(Object obj) {
        if (obj instanceof JSONObject) {
            JSONObject json = (JSONObject)obj;
            int r = json.optInt("r", -1);
            int g = json.optInt("g", -1);
            int b = json.optInt("b", -1);
            int a = json.optInt("a", -1);
            if (r != -1 && g != -1 && b != -1 && a != -1) {
                a = a * 255;
                return Color.argb(a, r, g, b);
            }
        }
        return null;
    }
}
