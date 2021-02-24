/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk;

import com.taplytics.sdk.datatypes.TLProperties;
import com.taplytics.sdk.managers.TLManager;
import com.taplytics.sdk.managers.TLManagerTest;
import com.taplytics.sdk.utils.promises.Promise;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.powermock.reflect.Whitebox;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;
import static org.assertj.core.api.Assertions.assertThat;

public class TaplyticsVarTest {

    private static final String STRING_VARIABLE = "exampleStringVariable";
    private static final String BOOLEAN_VARIABLE = "exampleBooleanVariable";
    private static final String NUMBER_VARIABLE = "exampleNumberVariable";
    private static final String FLOAT_VARIABLE = "exampleFloatVariable";
    private static final String JSON_VARIABLE = "exampleJSONVariable";
    private JSONObject value;

    private TLProperties tlProperties;

    @Before
    public void setUp() throws Throwable {
        value = new JSONObject();
        value.put("str", "yes");
        value.put("bool", true);
        value.put("dub", 1.5);

        JSONObject props = new JSONObject();
        JSONObject dynamicVariables = new JSONObject();
        {
            JSONObject variable = new JSONObject();
            variable.put("value", "baseline-value");
            dynamicVariables.put(STRING_VARIABLE, variable);
        }
        {
            JSONObject variable = new JSONObject();
            variable.put("value", true);
            dynamicVariables.put(BOOLEAN_VARIABLE, variable);
        }
        {
            JSONObject variable = new JSONObject();
            variable.put("value", 1);
            dynamicVariables.put(NUMBER_VARIABLE, variable);
        }
        {
            JSONObject variable = new JSONObject();
            variable.put("value", 1.5f);
            dynamicVariables.put(FLOAT_VARIABLE, variable);
        }
        {

            JSONObject variable = new JSONObject();
            variable.put("value", value);
            dynamicVariables.put(JSON_VARIABLE, variable);
        }
        props.put("dynamicVars", dynamicVariables);
        tlProperties = new TLProperties(props);
        TLManagerTest.setTLManagerTestInstance((tlProperties));
    }

    @After
    public void tearDown() {
        Map variableMap = (Map) Whitebox.getInternalState(TLDynamicVariableManager.getInstance(), "variableMap");
        variableMap.clear();
        Map synchronousVariableValueMap = (Map) Whitebox.getInternalState(TLDynamicVariableManager.getInstance(), "synchronousVariableValueMap");
        synchronousVariableValueMap.clear();
        Whitebox.setInternalState(TLManager.getInstance(), "tlPropertiesPromise", new Promise());
    }

    @Test
    public void stringVariableAsBoolean_sync_shouldUseDefault() {
        TaplyticsVar<Boolean> var = new TaplyticsVar<>(STRING_VARIABLE, false);
        assertThat(var.get())
                .isFalse();
    }

    @Test
    public void stringVariableAsBoolean_async_shouldUseDefault() {
        simulatePropertiesNotLoaded();
        TaplyticsVar<Boolean> var = new TaplyticsVar(STRING_VARIABLE, false, new TaplyticsVarListener() {
            @Override
            public void variableUpdated(Object value) {
                // This is based on the current behaviour. Maybe should update regardless?
                fail("since the value remains the default we shouldn't get here");
            }
        });
        simulateReceivingProperties();
        assertThat(var.get())
                .isFalse();
    }

    @Test
    public void async_wrongClass_shouldCallbackWithCorrect() {
        simulatePropertiesNotLoaded();

        final Boolean[] hasUpdated = new Boolean[1];
        TaplyticsVar<Float> var = new TaplyticsVar<>(NUMBER_VARIABLE, 1.5f, Double.class, new TaplyticsVarListener() {
            @Override
            public void variableUpdated(Object value) {
                assertEquals((int) value, 1);
                hasUpdated[0] = Boolean.TRUE; // track that we reached the updated callback
            }
        });

        assertThat(var.get())
                .isEqualTo(1.5f);

        simulateReceivingProperties();

        assertEquals(1, var.get());
        assertThat(hasUpdated[0])
                .describedAs("TaplyticsVarListener.variableUpdated not called!")
                .isTrue();
    }

    @Test
    public void async_shouldCallback() {
        simulatePropertiesNotLoaded();

        final Boolean[] hasUpdated = new Boolean[1];
        TaplyticsVar<String> var = new TaplyticsVar(STRING_VARIABLE, "default-value", new TaplyticsVarListener() {
            @Override
            public void variableUpdated(Object value) {
                assertThat(value)
                        .isEqualTo("baseline-value");
                hasUpdated[0] = Boolean.TRUE; // track that we reached the updated callback
            }
        });

        assertThat(var.get())
                .isEqualTo("default-value");

        simulateReceivingProperties();

        assertThat(var.get())
                .isEqualTo("baseline-value");
        assertThat(hasUpdated[0])
                .describedAs("TaplyticsVarListener.variableUpdated not called!")
                .isTrue();
    }

    @Test
    public void async_null_shouldCallbackWithCorrect() {
        simulatePropertiesNotLoaded();

        final Boolean[] hasUpdated = new Boolean[1];
        TaplyticsVar<String> var = new TaplyticsVar(STRING_VARIABLE, null, String.class, new TaplyticsVarListener() {
            @Override
            public void variableUpdated(Object value) {
                assertThat(value)
                        .isEqualTo("baseline-value");
                hasUpdated[0] = Boolean.TRUE; // track that we reached the updated callback
            }
        });

        assertEquals(var.get(), null);

        simulateReceivingProperties();

        assertThat(var.get())
                .isEqualTo("baseline-value");
        assertThat(hasUpdated[0])
                .describedAs("TaplyticsVarListener.variableUpdated not called!")
                .isTrue();
    }

    @Test
    public void async_null_shouldCallbackWithDefault() {
        simulatePropertiesNotLoaded();

        final Boolean[] hasUpdated = new Boolean[1];
        hasUpdated[0] = Boolean.FALSE;
        TaplyticsVar<String> var = new TaplyticsVar(STRING_VARIABLE, null, Boolean.class, new TaplyticsVarListener() {
            @Override
            public void variableUpdated(Object value) {
                assertEquals(null, value);
                hasUpdated[0] = Boolean.TRUE; // track that we reached the updated callback
            }
        });

        assertNull(var.get());

        simulateReceivingProperties();

        assertNull(var.get());

        assertThat(hasUpdated[0])
                .describedAs("TaplyticsVarListener.variableUpdated called!")
                .isTrue();
    }

    @Test
    public void async_wrongClass_shouldCallbackWithActual() {
        simulatePropertiesNotLoaded();

        final Boolean[] hasUpdated = new Boolean[1];
        TaplyticsVar<String> var = new TaplyticsVar(STRING_VARIABLE, "default-value", Boolean.class, new TaplyticsVarListener() {
            @Override
            public void variableUpdated(Object value) {
                assertThat(value)
                        .isEqualTo("baseline-value");
                hasUpdated[0] = Boolean.TRUE; // track that we reached the updated callback
            }
        });

        assertEquals(var.get(), "default-value");

        simulateReceivingProperties();

        assertThat(var.get())
                .isEqualTo("baseline-value");
        assertThat(hasUpdated[0])
                .describedAs("TaplyticsVarListener.variableUpdated not called!")
                .isTrue();
    }

    @Test
    public void booleanVariableAsString_sync_shouldUseDefault() {
        TaplyticsVar<String> var = new TaplyticsVar<>(BOOLEAN_VARIABLE, "default-value");
        assertThat(var.get())
                .isEqualTo("default-value");
    }

    @Test
    public void booleanVariableAsString_sync_class_correct_shouldUseDefault() {
        TaplyticsVar<String> var = new TaplyticsVar<>(BOOLEAN_VARIABLE, "default-value", String.class);
        assertThat(var.get())
                .isEqualTo("default-value");
    }

    @Test
    public void booleanVariableAsString_sync_class_wrong_shouldUseDefault() {
        TaplyticsVar<String> var = new TaplyticsVar<>(BOOLEAN_VARIABLE, "default-value", Boolean.class);
        assertThat(var.get())
                .isEqualTo("default-value");
    }

    @Test
    public void booleanVariableAsString_sync_class_right_null_shouldUseCorrect() {
        TaplyticsVar<String> var = new TaplyticsVar<>(BOOLEAN_VARIABLE, null, Boolean.class);
        assertEquals(true, var.get());
    }

    @Test
    public void JSONvar_sync_class_wrong_null_shouldUseDefault() {
        TaplyticsVar<String> var = new TaplyticsVar<>(JSON_VARIABLE, null, String.class);
        assertEquals(null, var.get());
    }

    @Test
    public void JSONvar_sync_class_wrong_null_shouldUseCorrect() throws JSONException {
        TaplyticsVar<JSONObject> var = new TaplyticsVar<>(JSON_VARIABLE, null, JSONObject.class);
        final JSONObject actual = var.get();
        JSONAssert.assertEquals(value, actual, false);
    }

    @Test
    public void JSONvar_sync_class_wrong_nonnull_shouldUseCorrect() throws JSONException {
        TaplyticsVar<JSONObject> var = new TaplyticsVar<>(JSON_VARIABLE, value, JSONObject.class);
        JSONAssert.assertEquals(value, var.get(), false);
    }

    @Test
    public void booleanVariableAsString_sync_class_right_shouldUseDefault() {
        TaplyticsVar<String> var = new TaplyticsVar<>(BOOLEAN_VARIABLE, "default-value", String.class);
        assertThat(var.get())
                .isEqualTo("default-value");
    }

    @Test
    public void numberVariableAsString_shouldUseDefault() {
        TaplyticsVar<String> var = new TaplyticsVar<>(NUMBER_VARIABLE, "default-value");
        assertThat(var.get())
                .isEqualTo("default-value");
    }

    @Test
    public void numberVariable_shouldGetCorrectValue() {
        TaplyticsVar<Number> var = new TaplyticsVar<>(NUMBER_VARIABLE, (Number) 1.5);
        assertThat(var.get()).isEqualTo((Number) 1);
    }

    @Test
    public void floatVariable_incorrect_shouldGetCorrectValue() {
        TaplyticsVar<Float> var = new TaplyticsVar<>(NUMBER_VARIABLE, 1.5f);
        assertEquals(1, var.get());
    }

    @Test
    public void floatVariable_incorrect_shouldGetDefaultValue_withInCorrectClass() {
        TaplyticsVar<Float> var = new TaplyticsVar<>(NUMBER_VARIABLE, 1.5f, Double.class);
        assertEquals(1, var.get());
    }


    @Test
    public void floatVariable_shouldGetCorrectValue() {
        TaplyticsVar<Float> var = new TaplyticsVar<>(FLOAT_VARIABLE, 1.5f);
        assertEquals(1.5, var.get());
    }


    @Test
    public void stringVariable_shouldGetCorrectValue() {
        TaplyticsVar<String> var = new TaplyticsVar<>(STRING_VARIABLE, "default-value");
        assertThat(var.get())
                .isEqualTo("baseline-value");
    }

    @Test
    public void missingVariable_shouldGetDefaultValue() {
        TaplyticsVar<String> var = new TaplyticsVar<>("non-existent-variable", "default-value");
        assertThat(var.get())
                .isEqualTo("default-value");
    }

    @Test
    public void missingVariable_async_shouldGetDefaultValue() {
        final Boolean[] hasUpdated = new Boolean[1];
        simulatePropertiesNotLoaded();
        TaplyticsVar<String> var = new TaplyticsVar("non-existent-variable", "default-value", new TaplyticsVarListener() {
            @Override
            public void variableUpdated(Object value) {
                assertThat(value)
                        .isEqualTo("default-value");
                hasUpdated[0] = true;
            }
        });

        simulateReceivingProperties();
        assertThat(var.get())
                .isEqualTo("default-value");
        assertThat(hasUpdated[0])
                .describedAs("TaplyticsVarListener.variableUpdated not called!")
                .isTrue();
    }

    private void simulatePropertiesNotLoaded() {
        // for async remove tl properties
        Whitebox.setInternalState(TLManager.getInstance(), "tlProperties", (Object[]) null);
    }

    private void simulateReceivingProperties() {
        Whitebox.setInternalState(TLManager.getInstance(), "tlProperties", tlProperties);
        TLManager.getInstance().getTlPropertiesPromise().finish(tlProperties);
    }
}
