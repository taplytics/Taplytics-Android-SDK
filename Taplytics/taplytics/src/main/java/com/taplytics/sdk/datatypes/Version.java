/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.datatypes;

import com.taplytics.sdk.utils.TLLog;

/**
 * Created by VicV on 11/19/14.
 */
public class Version implements Comparable<Version> {

    private String version;

    public final String get() {
        return this.version;
    }

    public Version(String version) {
        try {
            if (version == null)
                throw new IllegalArgumentException("Version can not be null");
            if (!version.matches("[0-9]+(\\.[0-9]+)*"))
                throw new IllegalArgumentException("Invalid version format");
            this.version = version;
        } catch (Throwable e) {
            TLLog.error("Version number invalid, defaulting to 0", e);
            this.version = "0";
        }
    }

    @Override
    public int compareTo(Version that) {
        try {
            if (that == null)
                return 1;
            String[] thisParts = this.get().split("\\.");
            String[] thatParts = that.get().split("\\.");
            int length = Math.max(thisParts.length, thatParts.length);
            for (int i = 0; i < length; i++) {
                int thisPart = i < thisParts.length ? Integer.parseInt(thisParts[i]) : 0;
                int thatPart = i < thatParts.length ? Integer.parseInt(thatParts[i]) : 0;
                if (thisPart < thatPart)
                    return -1;
                if (thisPart > thatPart)
                    return 1;
            }
            return 0;
        } catch (Throwable e) {
            return 0;
        }
    }

    @Override
    public boolean equals(Object that) {
        return this == that || that != null && ((Object) this).getClass() == that.getClass() && this.compareTo((Version) that) == 0;
    }

}