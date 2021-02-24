/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.resources;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import com.taplytics.sdk.managers.TLViewManager;
import com.taplytics.sdk.utils.ViewUtils;

/**
 * Created by VicV
 * <p/>
 * Layout used for the border, includes circle drawing stuff.
 */
public class BorderFrameLayout extends FrameLayout {

    private static class Circle {
        int radius, x, y;

        Circle(int X, int Y, int r) {
            radius = r;
            x = X;
            y = Y;
        }
    }

    private Paint mCirclePaint;
    private Paint mSmallCirclePaint;

    // Radius limit in pixels
    private final static int RADIUS = 40;
    private final static int RADIUS_SMALL = 3;

    public void clearCircle() {
        mCircle = null;
        invalidate();
    }

    /**
     * Currently Drawn Circle
     **/
    private Circle mCircle;

    /**
     * Default constructor
     */
    public BorderFrameLayout(final Context context) {
        super(context);
        init();
    }

    private void init() {
        mCirclePaint = new Paint();
        mCirclePaint.setColor(Colors.getBorderColor(TLViewManager.getInstance().getBorderMode()));
        mCirclePaint.setStrokeWidth(20);
        mCirclePaint.setStyle(Paint.Style.FILL);
        mSmallCirclePaint = new Paint();
        mSmallCirclePaint.setColor(Colors.getColorTranslucentPink());
        mSmallCirclePaint.setStrokeWidth(2);
        mSmallCirclePaint.setStyle(Paint.Style.FILL);
    }

    @Override
    public void onDraw(final Canvas canvas) {
        if (mCircle != null) {
            canvas.drawCircle(mCircle.x, mCircle.y, mCircle.radius, mCirclePaint);
            canvas.drawCircle(mCircle.x, mCircle.y, RADIUS_SMALL, mSmallCirclePaint);

        }
    }

    @SuppressWarnings("all")
    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        boolean handled = false;
        int xTouch;
        int yTouch;

        // get touch event coordinates and make transparent circle from it
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (TLViewManager.getInstance().getBorderMode().equals(ViewUtils.BORDER_MODE.TAP)
                        || TLViewManager.getInstance().getBorderMode().equals(ViewUtils.BORDER_MODE.BUTTON)) {
                    xTouch = (int) event.getX(0);
                    yTouch = (int) event.getY(0);

                    if (mCircle == null) {
                        mCircle = makeCircle(xTouch, yTouch);
                    }
                }

                invalidate();
                handled = true;
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                invalidate();
                handled = true;
                break;

            case MotionEvent.ACTION_MOVE:
                invalidate();
                handled = true;
                break;

            case MotionEvent.ACTION_UP:
                invalidate();
                handled = true;
                break;

            case MotionEvent.ACTION_POINTER_UP:
                invalidate();
                handled = true;
                break;

            case MotionEvent.ACTION_CANCEL:
                handled = true;
                break;

            default:
                break;
        }

        return (TLViewManager.getInstance().getBorderMode().equals(ViewUtils.BORDER_MODE.TAP) || TLViewManager.getInstance().getBorderMode()
                .equals((ViewUtils.BORDER_MODE.BUTTON))) ? (handled || super.onTouchEvent(event)) : super.onTouchEvent(event);
    }

    /**
     * Creates a circle
     */
    private Circle makeCircle(final int xTouch, final int yTouch) {
        return new Circle(xTouch, yTouch, RADIUS);

    }
}
