/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.datatypes;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertTrue;

public class TLPropertiesTests {
    private TLProperties testProps;
    @Before
    public void setUp() {
        testProps = null;
    }

    @Test
    public void testJSONObjectDataType() throws Throwable {
        JSONObject flagValue = new JSONObject();
        flagValue.put("_id", "testid");
        flagValue.put("key", "test");
        flagValue.put("name", "Test");
        flagValue.put("enabled", true);
        JSONObject flags = new JSONObject();
        flags.put("test", flagValue);
        JSONObject config = new JSONObject();
        config.put("ff", flags);
        testProps = new TLProperties(config);
        JSONObject tlPropsFFs = testProps.getFeatureFlags();
        JSONObject tlFlag = tlPropsFFs.getJSONObject("test");
        assertTrue(tlPropsFFs != null && tlFlag != null && tlFlag.getBoolean("enabled"));
    }

}
