/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static com.taplytics.sdk.utils.TLUtils.isJSONValid;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by VicV on 7/8/15.
 */
public class TLUtilsTest {

    @Test
    public void testIsJSONValid() throws JSONException {
        JSONObject object = new JSONObject();
        assertTrue(isJSONValid(object.toString()));
        object.put("basicstring", "test");
        assertTrue(isJSONValid(object.toString()));
        object.put("someweirdcharacter", "�");
        assertTrue(isJSONValid(object.toString()));
        assertTrue(object.toString() != null);

        JSONArray array = new JSONArray();
        array.put(1);
        array.put(true);
        array.put(object);
        array.put(6, 40);
        assertTrue(isJSONValid(object.toString()));
        object.put("basicString", " \"&#39; \\u001e\\v \\u0003\\u001f&quot;\\u0003\\u001d %\\b)\\v#\"");
        String jsonString = object.toString();
        jsonString += "dsfkjbdsflkjb lkjb lkj3bl4kj3b2l4l23j kb  f-09-09-08- a.sjfbska / /gfdgadis \\sd ";
        assertTrue(isJSONValid(jsonString));
    }

    //TODO: TEST BETTER YOU LAZY MAN
    @Test
    public void testIsJSONInvalid() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("basicString", "abcd");
        String failure = object.toString();
        //Remove brackets
        assertFalse(isJSONValid(failure.substring(0, failure.length() - 1)));
        assertFalse(isJSONValid(failure.substring(1, failure.length())));
        assertFalse(isJSONValid("string"));
        assertFalse(isJSONValid("{string}"));
    }

    //This is because we used to have a stupid method (still commented out) in tlutils instead of doing this.
    @Test
    public void questionMarkTest() {
        String test = "hey ? how are youy";
        assertTrue(test.length() > 0);
        assertTrue(test.contains("?"));
    }

}
