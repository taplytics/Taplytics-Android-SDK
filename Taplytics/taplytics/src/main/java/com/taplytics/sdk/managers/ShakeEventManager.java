/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.managers;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.taplytics.sdk.utils.TLLog;
import com.taplytics.sdk.utils.TLUtils;

import java.util.concurrent.TimeUnit;

/**
 * A manager to detect a shake event. Implement ShakeEvent to any class, and instantiate this manager, then onShake is where the shake is
 * detected.
 *
 * @author VicV
 */
public class ShakeEventManager implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;

    /**
     * Number of movements within the time interval. If our count goes over this number in the interval, we trigger a shake event.
     */
    private static final int MOVEMENTS_TO_TRIGGER = 6;

    /**
     * Threshold. If the number of movements goes OVER this number, restart the counter back at 0.
     */
    private static final int MOVEMENT_COUNT_THRESHOLD = 5;

    /**
     * If the force of the shake was above this, then count it.
     */
    private static final int MOVEMENT_ACCELERATION_THRESHOLD = 5;

    /**
     * Low pass filter amount. Lower for higher sensitivity.
     */
    private static final float FILTER_AMT = 0.8F;

    /**
     * The window within which we wait for a certain number of shakes, in millis.
     */
    private static final int SHAKE_TIME_WINDOW = 500;

    /**
     * A holder for the (rough) gravity force on each axis.
     */
    private float gravity[] = new float[3];

    /**
     * Current number of movements within time time window.
     */
    private int counter;

    /**
     * if the shake manager is active
     */
    private boolean isActive = false;
    private long firstMovementTime;
    private ShakeListener listener;

    public ShakeEventManager() {
    }

    public void setListener(ShakeListener listener) {
        this.listener = listener;
    }

    /**
     * Initialize the shake manager. Strap onto the sensor service and the accelerometer *
     */
    public void init(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        register();
    }

    /**
     * Register a listener to the manager.
     */
    public void register() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        TLThreadManager.getInstance().scheduleOnBackgroundThread(new Runnable() {
            @Override
            public void run() {
                isActive = true;
            }
        }, 200, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if (TLManager.getInstance().isActivityActive() && isActive) {

            float maxAcc = calcMaxAcceleration(sensorEvent);
            if (maxAcc >= MOVEMENT_ACCELERATION_THRESHOLD) {
                if (counter > MOVEMENT_COUNT_THRESHOLD) {
                    counter = 0;
                }
                if (counter == 0) {
                    counter++;
                    firstMovementTime = System.currentTimeMillis();
                } else {
                    long now = System.currentTimeMillis();
                    if ((now - firstMovementTime) < SHAKE_TIME_WINDOW && (now - firstMovementTime) > 10) {
                        counter++;
                        TLLog.debug("Move Count: " + counter);
                    } else {
                        resetAllData();
                        counter++;
                        return;
                    }
                    if (counter >= MOVEMENTS_TO_TRIGGER)
                        if (listener != null)
                            listener.onShake();
                }
            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    public void unregister() {
        sensorManager.unregisterListener(this);
    }

    private float calcMaxAcceleration(SensorEvent event) {
        gravity[0] = calcGravityForce(event.values[0], 0);
        gravity[1] = calcGravityForce(event.values[1], 1);
        gravity[2] = calcGravityForce(event.values[2], 2);

        float accX = event.values[0] - gravity[0];
        float accY = event.values[1] - gravity[1];
        float accZ = event.values[2] - gravity[2];

        float max1 = Math.max(accX, accY);
        return Math.max(max1, accZ);
    }

    /**
     * Run a low pass filter over our acceleration.
     */
    private float calcGravityForce(float currentVal, int index) {
        return FILTER_AMT * gravity[index] + (1 - FILTER_AMT) * currentVal;
    }

    private void resetAllData() {
        counter = 0;
        firstMovementTime = System.currentTimeMillis();
    }


    /**
     * Shake listener interface. Implement on any class to listen for shakes.
     *
     * @author VicV
     */
    public interface ShakeListener {
        void onShake();
    }
}
