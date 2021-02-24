/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.resources;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.shapes.Shape;

/**
 * Defined shape that wraps around border of screen. Used for when connected to test devices.
 *
 * @author vic
 */
public class TLHighlightShape extends ShapeDrawable {

    private static final int OVERLAY_ID = 399293499;

    // Getters are to suppress the warnings we get when we just supply a straight integer as a resource.
    public static int getOverlayId() {
        return OVERLAY_ID;
    }

    private final Paint fillPaint, strokePaint, highLightPaint;

    private int top = 0, left = 0, right = 0, bottom = 0;

    private boolean shouldHighlight;

    public TLHighlightShape(int strokeColor, int left, int top, int right, int bottom, boolean highlight) {

        super(new RectShape());
        this.top = top;
        this.left = left;
        this.right = right;
        this.bottom = bottom;

        this.shouldHighlight = highlight;

        fillPaint = new Paint(this.getPaint());
        fillPaint.setColor(Color.TRANSPARENT);
        highLightPaint = new Paint(this.getPaint());
        highLightPaint.setColor(Colors.getColorTransparentPink());

        strokePaint = new Paint(fillPaint);
        strokePaint.setColor(strokeColor);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(9);

    }

    @Override
    protected void onDraw(Shape shape, Canvas canvas, Paint paint) {
        Rect highLight = new Rect(left, top, right, bottom);
        shape.draw(canvas, fillPaint);
        if (shouldHighlight) {
            canvas.drawRect(highLight, highLightPaint);
        }
        canvas.drawRect(highLight, strokePaint);
    }

    public void updateParams(int left, int top, int right, int bottom) {
        this.left = left;
        this.right = right;
        this.top = top;
        this.bottom = bottom;
        this.invalidateSelf();
    }


}
