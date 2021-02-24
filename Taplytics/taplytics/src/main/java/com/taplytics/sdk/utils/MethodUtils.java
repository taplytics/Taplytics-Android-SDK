/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.taplytics.sdk.listeners.TLAggressiveTextWatcher;
import com.taplytics.sdk.listeners.TLAggressiveVisibilityListener;
import com.taplytics.sdk.managers.TLManager;
import com.taplytics.sdk.managers.TLMethodManager;
import com.taplytics.sdk.managers.TLViewManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import static com.taplytics.sdk.utils.ImageUtils.IMAGE_BITMAP_KEY;
import static com.taplytics.sdk.utils.ImageUtils.IMAGE_DRAWABLE_KEY;
import static com.taplytics.sdk.utils.ImageUtils.IMAGE_PATH_KEY;

/**
 * Created by VicV on 10/10/14.
 * <p/>
 * A utility class for common actions used using method reflection and methods.
 */
public class MethodUtils {

    public static final String PARAMETER_TYPE = "parameterType";

    /**
     * Get aall method information. We get all of the setters, as well as their associated parameter type. Then, we get all of the currently
     * set variables from the getters.
     *
     * @param viewClass Class of the view we wish to get method info from
     * @return a JsonObject containing all known setters and getters with their associated information
     * @throws org.json.JSONException
     */
    public static JSONObject getMethodInfo(Class<?> viewClass, View view) throws JSONException {
        // All of the setter information
        JSONArray setters = new JSONArray();

        // All of the currently set variables
        JSONArray currentVars = new JSONArray();

        // Grab all the methods
        HashSet<Method> methods = new HashSet<>(Arrays.asList(viewClass.getMethods()));

        String classNameForMethods = getMethodViewClass(viewClass);

        // Grab our known setter and getter names.
        HashSet<String> setterNames = TLMethodManager.getInstance().getSetterMethods().get(classNameForMethods);
        HashSet<String> getterNames = TLMethodManager.getInstance().getGetterMethods().get(classNameForMethods);

        // Iterate through every single method
        for (Method method : methods) {
            String methodName = method.getName();

            JSONObject methodObject = new JSONObject();
            JSONArray methodParameters = new JSONArray();

            // Check if the method is a setter. Will have to be a bit more rigorous later on.
            if (setterNames.contains(methodName)) {

                // Get the parameter types. This is necessary for creating the method call later on.
                for (Class<?> paramType : method.getParameterTypes()) {

                    // Add the parameter to the object, but first check if its an array or not as to not crash stuff.
                    methodParameters.put((paramType.isArray() ? paramType.getComponentType() : paramType).getName());
                }

                // construct our object
                methodObject.put("paramTypes", methodParameters);
                methodObject.put("methodName", methodName);

                setters.put(methodObject);

                // Take out any other methods of this name so we don't flood it all.

                // NOTE: May have to specifically pick and choose certain ones.
                setterNames.remove(methodName);

                // Now we basically want to get all the info possible for the views
            } else if (getterNames.contains(methodName)) {
                JSONObject variable = new JSONObject();
                try {
                    Class<?> returnType = method.getReturnType();
                    if (isValidType(returnType)) {
                        // Invoke all the getters, store their data.
                        Object object = getMethodObject(view, method);

                        // Save the method name and value
                        variable.put("methodName", method.getName());

                        // Send a string null instead of a null
                        variable.put("currentValue", object == null ? "null" : object);

                        currentVars.put(variable);

                        // NOTE: May have to specifically pick and choose certain ones.
                        getterNames.remove(methodName);
                    }
                } catch (Exception e) {
                    TLLog.error("something", e);
                    // Something is wrong. Private method?
                }
            }
        }

        // Width and height are part of LayoutParams and not the view itself.
        JSONObject widthMethod = new JSONObject();
        JSONArray widthMethodParams = new JSONArray();
        widthMethod.put("methodName", "setWidth");
        widthMethodParams.put(int.class.getName());
        widthMethod.put("paramTypes", widthMethodParams);

        setters.put(widthMethod);
        JSONObject heightMethod = new JSONObject();
        JSONArray heightMethodParams = new JSONArray();
        heightMethod.put("methodName", "setHeight");
        heightMethodParams.put(int.class.getName());
        heightMethod.put("paramTypes", heightMethodParams);
        setters.put(heightMethod);

        JSONObject methodInfo = new JSONObject();
        methodInfo.put("variables", currentVars);
        methodInfo.put("setters", setters);

        return methodInfo;
    }

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    /** Apply the modification method **/
    static void applyMethod(final View v, final String methodName, final Object val, final String parameterType, final boolean reset, final boolean addTag) {
        Activity currentActivity = TLManager.getInstance().getCurrentActivity();
        if (currentActivity != null) {

            Object value = val;
            boolean containsMethod = false;

            try {
                //If the view has a reset tag, OR if the value is null, we should reset it back to baseline.
                if (reset
                        || (value == null
                        || value == JSONObject.NULL
                        || isEmptyJSONObject(value)
                        || ((value instanceof JSONArray) && ((JSONArray) value).length() == 0))) {
                    //Grab the tag of the view which is the object containing the values that need to reset.
                    Object o = v.getTag(TLViewManager.getInstance().getApplicationIconId());
                    if (o != null && o instanceof HashMap && ((HashMap) o).containsKey(methodName)) {
                        value = ((HashMap) o).get(methodName);
                        //Switch the value of the method to be the baseline value.
                        if (value != null && value instanceof HashMap) {
                            containsMethod = true;
                            value = ((HashMap) value).get(methodName);
                        }
                    }
                }
            } catch (Exception e) {
                TLLog.error("resetting view error", e);
            }

            if (value != null && value != JSONObject.NULL && !isEmptyJSONObject(value) && !((value instanceof JSONArray)
                    && ((JSONArray) value).length() == 0)) {
                //If our map doesn't contain this method yet, add it to the view tag.
                if (!containsMethod && (addTag || TLManager.getInstance().isLiveUpdate())) {
                    addMethodToViewTag(v, methodName, parameterType);
                }


                final Object finalValue = value;
                currentActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            switch (methodName) {
                                // SetWidth and SetHeight are special because they are not applied to the views, just the views layout
                                // parameters.
                                case "setWidth":
                                    if (finalValue != JSONObject.NULL) {
                                        float widthDp = ((Number) finalValue).floatValue();
                                        v.getLayoutParams().width = ViewUtils.convertDpToPixel(widthDp);
                                        v.requestLayout();
                                    }
                                    break;
                                case "setHeight":
                                    if (finalValue != JSONObject.NULL) {
                                        float heightDp = ((Number) finalValue).floatValue();
                                        v.getLayoutParams().height = ViewUtils.convertDpToPixel(heightDp);
                                        v.requestLayout();
                                    }
                                    break;
                                // SetPadding is special because it has four parameters
                                case "setPadding":
                                    JSONObject params = (JSONObject) finalValue;
                                    int left = (params.has("left") && !params.isNull("left")) ? ViewUtils.convertDpToPixel(params.getInt("left")) : v
                                            .getPaddingLeft();
                                    int right = (params.has("right") && !params.isNull("right")) ? ViewUtils.convertDpToPixel(params.getInt("right")) : v
                                            .getPaddingRight();
                                    int top = (params.has("top") && !params.isNull("top")) ? ViewUtils.convertDpToPixel(params.getInt("top")) : v
                                            .getPaddingTop();
                                    int bottom = (params.has("bottom") && !params.isNull("bottom")) ? ViewUtils.convertDpToPixel(params.getInt("bottom"))
                                            : v.getPaddingBottom();
                                    (v.getClass().getMethod(methodName, int.class, int.class, int.class, int.class)).invoke(v, left, top, right, bottom);
                                    v.invalidate();
                                    break;
                                case "setBackgroundDrawable":
                                    if (finalValue instanceof Drawable) {
                                        if (Build.VERSION.SDK_INT >= 9) {
                                            v.setBackground((Drawable) finalValue);
                                        } else {
                                            v.setBackgroundDrawable((Drawable) finalValue);
                                        }
                                    } else if ((finalValue instanceof JSONArray) && ((JSONArray) finalValue).length() != 0) {
                                        File imgFile = ImageUtils.getImageFile(finalValue);
                                        if (imgFile.exists()) {
                                            Drawable d = Drawable.createFromPath(imgFile.getAbsolutePath());
                                            //Replace the file path with a drawable so we don't have to recreate it again
                                            ViewUtils.cacheValue(v, finalValue, methodName, parameterType);
                                            if (Build.VERSION.SDK_INT >= 9) {
                                                v.setBackground(d);
                                            } else {
                                                v.setBackgroundDrawable(d);
                                            }
                                        }
                                    } else if (finalValue instanceof String && finalValue.equals("null")) {
                                        v.setBackground(null);
                                    }
                                    break;
                                case "setBackgroundColor":
                                    switch (parameterType) {
                                        case "int":
                                            if (finalValue instanceof Number) {
                                                (v.getClass().getMethod(methodName, int.class)).invoke(v, ((Number) finalValue).intValue());
                                                break;
                                            }
                                            break;
                                        case "float":
                                            if (finalValue instanceof Number) {
                                                (v.getClass().getMethod(methodName, float.class)).invoke(v, ((Number) finalValue).floatValue());
                                                break;
                                            }
                                            break;
                                        case "tlColor":
                                            if (finalValue instanceof Integer) {
                                                (v.getClass().getMethod(methodName, int.class)).invoke(v, (Integer) finalValue);
                                            } else if (finalValue instanceof Drawable) {
                                                v.setBackground((Drawable) finalValue);
                                            } else if (finalValue instanceof String && finalValue.equals("null")) {
                                                v.setBackground(null);
                                            } else {
                                                Integer color = DataUtils.getColorFromJSONObject(finalValue);
                                                if (color != null) {
                                                    (v.getClass().getMethod(methodName, int.class)).invoke(v, color);
                                                    break;
                                                }
                                                break;
                                            }
                                            break;
                                    }
                                    break;

                                case "setHintText":
                                    String hintText = (String) finalValue;
                                    ((TextView) v).setHint(hintText.replace("\\n", "\n"));
                                    break;
                                case "setText":
                                    String text = null;

                                    if(finalValue instanceof String){
                                        text =  (String) finalValue;
                                    } else if (finalValue instanceof StringBuilder){
                                        //Spannables and whatnot weren't previously updating.
                                        text = finalValue.toString();
                                    }

                                    if(text == null){
                                        return;
                                    }

                                    final String formatted = text.replace("\\n", "\n");
                                    if (TLViewManager.getInstance().hasAggressiveViewChanges()) {

                                        //Grab the text watcher if it exists.
                                        TLAggressiveTextWatcher watcher = ViewUtils.getTLTextWatcherFromTextView((TextView) v);

                                        //Make sure we reset this as well so that the watcher doesn't continue stopping changes.
                                        if (reset) {
                                            if (v.getTag(ViewUtils.getTextWatcherTag()) != null) {
                                                v.setTag(ViewUtils.getTextWatcherTag(), null);
                                                if (watcher != null) {
                                                    //Speed apps up by making sure we don't just keep adding these over and over.
                                                    ((TextView) v).removeTextChangedListener(watcher);
                                                }
                                            }
                                        } else {
                                            if (v.getTag(ViewUtils.getTextWatcherTag()) == null) {
                                                v.setTag(ViewUtils.getTextWatcherTag(), formatted);
                                                if (watcher == null) {
                                                    ((TextView) v).addTextChangedListener(new TLAggressiveTextWatcher().getTextWatcher((TextView) v, formatted));
                                                }
                                            } else {

                                                //UPDATE the enforced text to expect the incoming change.
                                                if (watcher != null){
                                                    watcher.updateEnforcedText(formatted);
                                                }
                                            }
                                        }
                                    }

                                    /*-----*/
                                    ((TextView) v).setText(formatted);
                                    /*----*/

                                    break;
                                case "setTextSize":
                                    float pixelSize = (finalValue instanceof Float) ? (Float) finalValue : ((Integer) finalValue).floatValue();
                                    ((TextView) v).setTextSize(TypedValue.COMPLEX_UNIT_PX, pixelSize);
                                    break;
                                case "setImageDrawable":
                                    if (finalValue instanceof Drawable) {
                                        if (Build.VERSION.SDK_INT >= 9) {
                                            ((ImageView) v).setImageDrawable((Drawable) finalValue);
                                            if (TLManager.getInstance().isTest()) {
                                                v.setTag(IMAGE_DRAWABLE_KEY, finalValue);
                                            }
                                        }
                                    } else if (finalValue instanceof Bitmap) {
                                        ((ImageView) v).setImageBitmap((Bitmap) finalValue);
                                        if (TLManager.getInstance().isTest()) {
                                            v.setTag(IMAGE_BITMAP_KEY, finalValue);
                                        }
                                        //if its not a bitmap or Drawable we have been given a file path, so get the file and turn that into a drawable
                                    } else if ((finalValue instanceof JSONArray) && ((JSONArray) finalValue).length() != 0) {
                                        File imageViewFile = ImageUtils.getImageFile(finalValue);
                                        if (imageViewFile.exists()) {
                                            Drawable d = Drawable.createFromPath(imageViewFile.getAbsolutePath());
                                            //Replace the file path with a drawable so we don't have to recreate it again
                                            ViewUtils.cacheValue(v, d, methodName, parameterType);
                                            if (Build.VERSION.SDK_INT >= 9) {
                                                ((ImageView) v).setImageDrawable(d);
                                                if (TLManager.getInstance().isTest()) {
                                                    v.setTag(IMAGE_PATH_KEY, imageViewFile.getAbsolutePath());
                                                }
                                            }
                                        }
                                    }
                                    break;
                                case "setVisibility":
                                    if (TLViewManager.getInstance().hasAggressiveViewChanges()) {
                                        if (v.getTag(ViewUtils.getVisibilityTag()) == null) {
                                            v.setTag(ViewUtils.getVisibilityTag(), finalValue);
                                            v.getViewTreeObserver().addOnGlobalLayoutListener(new TLAggressiveVisibilityListener().getListener(v, finalValue));
                                        }
                                    }
                                    if (finalValue instanceof Number) {
                                        (v.getClass().getMethod(methodName, int.class)).invoke(v, ((Number) finalValue).intValue());
                                    }
                                    break;
                                default:
                                    switch (parameterType) {
                                        case "int":
                                            if (finalValue instanceof Number) {
                                                (v.getClass().getMethod(methodName, int.class)).invoke(v, ((Number) finalValue).intValue());
                                            }
                                            break;
                                        case "float":
                                            if (finalValue instanceof Number) {
                                                (v.getClass().getMethod(methodName, float.class)).invoke(v, ((Number) finalValue).floatValue());
                                                break;
                                            }
                                            break;
                                        case "tlColor":
                                            if (finalValue instanceof Integer) {
                                                (v.getClass().getMethod(methodName, int.class)).invoke(v, (Integer) finalValue);
                                            } else if (finalValue instanceof Drawable) {
                                                v.setBackground((Drawable) finalValue);
                                            } else {
                                                Integer color = DataUtils.getColorFromJSONObject(finalValue);
                                                if (color != null) {
                                                    (v.getClass().getMethod(methodName, int.class)).invoke(v, color);
                                                    break;
                                                }
                                            }
                                            break;
                                    /*
                                     * NOTE: The only time we get SP back is for text size. IF THAT IS NO LONGER TRUE, CHANGE THIS METHOD.
									 * 
									 * 2 is equal to TypedValue.COMPLEX_UNIT_SP
									 */
                                        case "sp":
                                            if (finalValue instanceof Number) {
                                                (v.getClass().getMethod(methodName, int.class, float.class)).invoke(v, 2, ((Number) finalValue).floatValue());
                                            }
                                            break;
                                        // Future proofing. In case something comes down the stream that is dp but isn't height or width of a view.
                                        case "dp":
                                            if (finalValue instanceof Number) {
                                                int pixel = ViewUtils.convertDpToPixel(((Number) finalValue).floatValue());
                                                (v.getClass().getMethod(methodName, int.class)).invoke(v, pixel);
                                            }
                                            break;
                                        case "ScaleType":
                                            if (finalValue instanceof Number) {
                                                ImageView.ScaleType type = ImageView.ScaleType.values()[((Number) finalValue).intValue()];
                                                (v.getClass().getMethod(methodName, ImageView.ScaleType.class)).invoke(v, type);
                                            }
                                            break;
                                        default:
                                            Class<?> param = Class.forName(parameterType);
                                            (v.getClass().getMethod(methodName, param)).invoke(v, finalValue);
                                            break;
                                    }
                                    break;
                            }

                        } catch (SecurityException | ClassNotFoundException | InvocationTargetException | IllegalAccessException
                                | NoSuchMethodException e) {
                            TLLog.error("Method errors on " + methodName, e);
                        } catch (Exception e) {
                            TLLog.error(e.getCause() + ": " + v.getClass() + ", " + methodName + ", " + e.getMessage(), e);
                        }


                    }
                });
            }
        }
    }

    private static boolean isEmptyJSONObject(Object v) {
        if (v instanceof JSONObject) {
            JSONObject json = (JSONObject) v;
            for (Iterator<String> iter = json.keys(); iter.hasNext(); ) {
                String key = iter.next();
                try {
                    if (json.get(key) != JSONObject.NULL && json.get(key) != null) {
                        return false;
                    }
                } catch (Exception e) {
                    //
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Add the method containing the baseline values to the viewtag of the view.
     * <p/>
     * This is so we can reset to baseline when needed. Used mostly for liveupdate,
     * but also for things such as listviews/recyclerviews in release mode.
     * <p/>
     * Takes the setter method sent down by the server, and switches it to an associated
     * getter and retrieves the baseline value.
     * <p/>
     * Then when it needs to reset, it uses these values as the value to set to.
     *
     * @param v             The view to get things from
     * @param methodName    The current method to get the baseline value of
     * @param parameterType Parameter type required for the method.
     */
    private static void addMethodToViewTag(final View v, final String methodName, final String parameterType) {
        try {

            //If the map already has the value just quit.
            Object obj = v.getTag(TLViewManager.getInstance().getApplicationIconId());
            final HashMap map = (obj != null && obj instanceof HashMap) ? (HashMap) obj : new HashMap();
            if (map.containsKey(methodName))
                return;

            Object value;
            final HashMap newMap = new HashMap();
            newMap.put(PARAMETER_TYPE, parameterType);
            switch (methodName) {
                //Padding is split into a bunch of different parameters
                case "setPadding":
                    int top = v.getPaddingTop();
                    int bottom = v.getPaddingLeft();
                    int left = v.getPaddingRight();
                    int right = v.getPaddingBottom();
                    final JSONObject paddingObject = new JSONObject();
                    paddingObject.put("top", top);
                    paddingObject.put("bottom", bottom);
                    paddingObject.put("left", left);
                    paddingObject.put("right", right);
                    value = paddingObject;
                    newMap.put(methodName, value);

                    break;
                //Getwidth and getheight need to be done on the UI thread once the view is drawn.
                case "setWidth":
                case "setHeight":
                    try {
                        v.requestLayout();
                        Object number = v.getClass().getMethod(methodName.replaceAll("set", "get")).invoke(v);
                        float floatVal = -1;
                        if (number instanceof Float) {
                            floatVal = (Float) number;
                        } else if (number instanceof Integer) {
                            floatVal = ((Integer) number).floatValue();
                        }
                        final float finalFloatVal = floatVal;

                        if (finalFloatVal != -1) {
                            newMap.put(methodName, (ViewUtils.convertPixelsToDp(finalFloatVal)));
                            newMap.put(PARAMETER_TYPE, parameterType);
                            map.put(methodName, newMap);
                        }
                        v.setTag(TLViewManager.getInstance().getApplicationIconId(), map);
                    } catch (Exception e) {
                        TLLog.error("Error saving height or width to map", e);
                    }
                    return;
                case "setTextColor":
                    value = v.getClass().getMethod("getCurrentTextColor").invoke(v);
                    break;
                //The background color is just null if we cant et it.
                case "setBackgroundColor":
                    try {
                        ColorDrawable drawable = (ColorDrawable) v.getBackground();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                            value = drawable.getColor();
                            //Before honeycomb you couldnt get a drawable color so
                            //We have to actually draw the drawable on a canvas to grab a color from it.
                        } else {
                            Bitmap mBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
                            Canvas mCanvas = new Canvas(mBitmap);
                            Rect mBounds = new Rect();
                            ColorDrawable colorDrawable = (ColorDrawable) v.getBackground();

                            mBounds.set(colorDrawable.getBounds()); // Save the original bounds.
                            colorDrawable.setBounds(0, 0, 1, 1); // Change the bounds.

                            colorDrawable.draw(mCanvas);
                            value = mBitmap.getPixel(0, 0);
                            colorDrawable.setBounds(mBounds); // Restore the original bounds.
                        }
                    } catch (Exception e) {
                        //Background wasn't a color to begin with
                        value = v.getBackground();
                        if (value == null) {
                            value = "null";
                        }
                        break;
                    }
                    break;
                //There is a special distinction between bitmapdrawable and regular drawables.
                case "setImageDrawable":
                    if (v instanceof ImageView) {
                        value = (((ImageView) v).getDrawable() instanceof BitmapDrawable) ? (((BitmapDrawable) ((ImageView) v).getDrawable()).getBitmap()) : (((ImageView) v).getDrawable());
                    } else {
                        //If its not an imageview just snag the background as is.
                        value = v.getBackground();
                    }
                    break;
                case "setBackgroundDrawable":
                    value = v.getBackground();
                    if (value == null) {
                        value = "null";
                    }
                    break;
                default:
                    value = v.getClass().getMethod(methodName.replaceAll("set", "get")).invoke(v);
                    break;
            }

            newMap.put(methodName, value);
            map.put(methodName, newMap);


            v.setTag(TLViewManager.getInstance().getApplicationIconId(), map);
        } catch (Exception e) {
            TLLog.error("error adding method to view tag", e);
        }

    }

    /**
     * @param viewClass The current view class
     * @return The lowest known view class in which we have identified wanted methods for in TLMethodManager
     */
    public static String getMethodViewClass(Class<?> viewClass) {
        String base = null;
        if (TLMethodManager.getInstance().getSetterMethods().containsKey(viewClass.getSimpleName())) {
            return viewClass.getSimpleName();
        } else if (!viewClass.getSimpleName().equals("View")) {
            base = getMethodViewClass(viewClass.getSuperclass());
        }
        return base;
    }

    /**
     * Basically all of the serializable types we can properly pass back and forth in JSON.
     *
     * @param returnType The type which the class returns
     * @return Whether or not this is a type that we can handle
     */
    private static boolean isValidType(Class<?> returnType) {
        return (returnType.equals(Integer.class) || returnType.equals(Boolean.class) || returnType.equals(CharSequence.class)
                || returnType.equals(String.class) || returnType.equals(float.class) || returnType.equals(int.class) || returnType.equals(long.class)
                || returnType.equals(short.class) || returnType.equals(Number.class) || returnType.equals(CharSequence.class)
                || returnType.equals(double.class) || returnType.equals(boolean.class) || returnType.equals(byte.class)
                || returnType.equals(StringBuilder.class) || returnType.equals(StringBuffer.class) || returnType.equals(Float.class)
                || returnType.equals(Double.class) || returnType.equals(Long.class) || returnType.equals(Short.class) || returnType
                .equals(Byte.class));
    }

    /**
     * This method will attempt to invoke a method on a class and its superclasses until it succeeds. This is because we pass in base
     * classes, but many methods are not on base classes.
     *
     * @param viewClass The class we are invoking a method on
     * @param method    The method that we are invoking
     * @return An Object
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    private static Object getMethodObject(View viewClass, Method method) throws InvocationTargetException, IllegalAccessException {
        try {
            return method.invoke(viewClass);
        } catch (IllegalArgumentException e) {
            return getMethodObject(viewClass, method);
        }
    }

}
