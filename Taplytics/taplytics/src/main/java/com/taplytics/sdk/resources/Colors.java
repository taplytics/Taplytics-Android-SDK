/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.resources;

import android.graphics.Color;

import com.taplytics.sdk.utils.ViewUtils;

public class Colors {

    /**
     * Experiment border color
     **/
    private static final String COLOR_TRANSLUCENT_BLUE = "#CC70daff";
    /**
     * Tap mode border color
     **/
    private static final String COLOR_TRANSLUCENT_GREEN = "#CC7fe1a5";
    /**
     * Tap mode dot color
     **/
    private static final String COLOR_TRANSLUCENT_PINK = "#CCe75f68";

    /**
     * Tap highlight element color
     **/
    private static final String COLOR_TRANSPARENT_PINK = "#44e75f68";

    /**
     * Activity mode border color
     **/
    private static final String COLOR_TRANSLUCENT_PURPLE = "#CCba76e5";
    /**
     * Disconnect mode border color
     **/
    private static final String COLOR_TRANSLUCENT_RED = "#CCcc3341";

    public static int getBorderColor(ViewUtils.BORDER_MODE mode) {
        String color;
        switch (mode) {
            case EXPERIMENT:
            default:
                color = COLOR_TRANSLUCENT_BLUE;
                break;
            case TAP:
            case BUTTON:
                color = COLOR_TRANSLUCENT_GREEN;
                break;
            case ACTIVITY:
                color = COLOR_TRANSLUCENT_PURPLE;
                break;
            case DISCONNECT:
                color = COLOR_TRANSLUCENT_RED;
                break;
        }
        return Color.parseColor(color);
    }

    public static int getColorTranslucentPink() {
        return Color.parseColor(COLOR_TRANSLUCENT_PINK);
    }


    public static int getColorTransparentPink() {
        return Color.parseColor(COLOR_TRANSPARENT_PINK);
    }
}
