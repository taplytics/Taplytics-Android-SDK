/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.datatypes;

import org.json.JSONObject;

/**
 * Created by VicV on 1/28/15.
 */
public class TLAnalyticsData {

    private String eventName;
    private Object value;
    private JSONObject metaData;
    private String source;

    public TLAnalyticsData(String eventName, Object value, JSONObject metaData, String source) {
        this.eventName = eventName;
        this.value = value;
        this.metaData = metaData;
        this.source = source;
    }

    public TLAnalyticsData(String eventName, Object value, JSONObject metaData) {
        this.eventName = eventName;
        this.value = value;
        this.metaData = metaData;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public JSONObject getMetaData() {
        return metaData;
    }

    public void setMetaData(JSONObject metaData) {
        this.metaData = metaData;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    @Override
    public String toString() {
        return "Name: " + eventName + ". Value: " + value + ". Data: " + metaData.toString() + ".";
    }
}
