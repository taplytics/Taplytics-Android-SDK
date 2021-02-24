/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.resources;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.shapes.Shape;

import com.taplytics.sdk.utils.ViewUtils;

/**
 * Defined shape that wraps around border of screen. Used for when connected to test devices.
 *
 * @author vic
 */
public class TLBorderShape extends ShapeDrawable {

    // For ID assignment, arbitrary number.
    private static final int BORDER_TOP_TEXTVIEW_ID = 399293491;
    private static final int BORDER_BOTTOM_TEXTVIEW_ID = 399293492;

    private static final int BORDER_ID = 399293493;

    public static int getTouchscreenId() {
        return TOUCHSCREEN_ID;
    }

    private static final int TOUCHSCREEN_ID = 399293494;

    // Getters are to suppress the warnings we get when we just supply a straight integer as a resource.
    public static int getBorderId() {
        return BORDER_ID;
    }

    public static int getBottomTextId() {
        return BORDER_BOTTOM_TEXTVIEW_ID;
    }

    public static int getTopTextId() {
        return BORDER_TOP_TEXTVIEW_ID;
    }

    private static final String BORDER_TOP_TAP_TEXT = "View Selection Mode";
    private static final String BORDER_BOTTOM_TAP_TEXT = "Tap any view.";
    private static final String BORDER_BOTTOM_ACTIVITY_TEXT = "Press the button on Taplytics when you've arrived";
    private static final String BORDER_TOP_ACTIVITY_TEXT = "Navigate to desired activity or screen";
    private static final String BORDER_TOP_CLICK_BUTTON_TEXT = "Choose a button to track clicks.";
    private static final String BORDER_TOP_DISCONNECT = "Connection lost. Attempting to reconnect.";
    public static final String BORDER_TOP_DISCONNECT_NUMBER = "Connection lost.";
    public static final String BORDER_BOTTOM_RECONNECT = "Check your network connection.";
    public static final String BORDER_BOTTOM_RECONNECT_THIRD = "Try reloading the app.";

    public static final float STROKE_WIDTH = 2;
    public static final float TOP_HEIGHT = 35;

    private final Paint fillPaint, strokePaint;

    private final int strokeWidth;

    public TLBorderShape(int strokeColor, float strokeWidth) {

        super(new RectShape());
        this.strokeWidth = ViewUtils.convertDpToPixel(strokeWidth);

        fillPaint = new Paint(this.getPaint());

        fillPaint.setColor(Color.TRANSPARENT);
        strokePaint = new Paint(fillPaint);
        Paint topPaint = new Paint(fillPaint);
        topPaint.setStyle(Style.FILL);
        topPaint.setColor(strokeColor);
        strokePaint.setStyle(Style.FILL);
        strokePaint.setStrokeWidth(strokeWidth);
        strokePaint.setColor(strokeColor);
    }

    @Override
    protected void onDraw(Shape shape, Canvas canvas, Paint paint) {
        int topHeight = ViewUtils.convertDpToPixel(TOP_HEIGHT);

        int width = canvas.getWidth();
        int height = canvas.getHeight();
        Rect top = new Rect(0, 0, width, topHeight);
        Rect left = new Rect(0, topHeight, strokeWidth, height - strokeWidth);
        Rect right = new Rect(width - strokeWidth, topHeight, width, height - strokeWidth);
        Rect bottom = new Rect(0, height - strokeWidth, width, height);
        shape.draw(canvas, fillPaint);
        canvas.drawRect(top, strokePaint);
        canvas.drawRect(left, strokePaint);
        canvas.drawRect(right, strokePaint);
        canvas.drawRect(bottom, strokePaint);

    }

    public static String getTopText(ViewUtils.BORDER_MODE mode) {
        switch (mode) {
            case TAP:
                return BORDER_TOP_TAP_TEXT;
            case ACTIVITY:
                return BORDER_TOP_ACTIVITY_TEXT;
            case BUTTON:
                return BORDER_TOP_CLICK_BUTTON_TEXT;
            case DISCONNECT:
                return BORDER_TOP_DISCONNECT;
            default:
                return "";
        }
    }

    public static String getBottomText(ViewUtils.BORDER_MODE mode) {
        switch (mode) {
            case TAP:
                return BORDER_BOTTOM_TAP_TEXT;
            case ACTIVITY:
                return BORDER_BOTTOM_ACTIVITY_TEXT;
            case DISCONNECT:
                return BORDER_BOTTOM_RECONNECT;
            case BUTTON:
            default:
                return "";
        }
    }

}
