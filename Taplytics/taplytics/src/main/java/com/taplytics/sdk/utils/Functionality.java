/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.utils;

/**
 * Created by VicV on 6/8/15.
 */
public enum Functionality {
    VIEWTRACKING("views"),
    EVENTS("events"),
    ANALYTICS("analytics"),
    EXTERNAL("external"),
    GOOGLE("google"),
    MIXPANEL("mixpanel"),
    FLURRY("flurry"),
    ADOBE("adobe"),
    LOCALYTICS("localytics"),
    AMPLITUDE("amplitude"),
    SEGMENT("segment"),
    LISTVIEWS("listviews"),
    RECYCLERVIEWS("recyclerviews"),
    VISUAL("visual"),
    ERRORS("errors"),
    SOCKETS("sockets"),
    VIEWPAGERS("viewpagers"),
    SUPPORTFRAGMENTS("supportfragments"),
    FRAGMENTS("fragments"),
    PUSH("push"),
    GEOFENCES("geofences"),
    CODE("code"),
    BUTTONS("buttons"),
    DYNAMICVARS("dynamicvars"),
    DELAYLOAD("delay"),
    SHAKEMENU("shakemenu"),
    REQUESTERRORS("requesterrors"),
    EVENTSTHROTTLED("eventsThrottled"),
    BASE64("base64"),
    DIALOGS("dialogs");

    private String text;

    Functionality(String text) {
        this.text = text;
    }

    public String getText() {
        return this.text;
    }

    public static Functionality fromString(String text) {
        if (text != null) {
            for (Functionality b : Functionality.values()) {
                if (text.equalsIgnoreCase(b.text)) {
                    return b;
                }
            }
        }
        return null;
    }
}

