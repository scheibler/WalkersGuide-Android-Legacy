package org.walkersguide.utils;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class SensorsManager {

    public interface SensorsListener {
        public void compassChanged(float azimuth);
        public void acceleratorChanged(double accel);
    }

    private SensorsListener sListener;
    private Context mContext;
    private SensorManager mSensorManager;
    private boolean isRunning;

    // compass
    private Sensor mCompass;
    private CompassChangesListener mCompassChangesListener;

    // accelerator
    private Sensor mAccelerator;
    private AcceleratorChangesListener mAcceleratorChangesListener;

    public SensorsManager(Context mContext) {
        this.mContext = mContext;
        this.isRunning = false;
        mSensorManager = (SensorManager)mContext.getSystemService(Context.SENSOR_SERVICE);
        // compass
        mCompass = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        mCompassChangesListener = new CompassChangesListener();
        // accelerator
        mAccelerator = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAcceleratorChangesListener = new AcceleratorChangesListener();
    }

    public void setSensorsListener(SensorsListener sListener) {
        this.sListener = sListener;
    }

    public void resumeSensors() {
        if(!isRunning) {
            // compass
            mSensorManager.registerListener(mCompassChangesListener, mCompass, 
                    SensorManager.SENSOR_DELAY_NORMAL);
            // accelerator
            mSensorManager.registerListener(mAcceleratorChangesListener, mAccelerator, 
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        this.isRunning = true;
    }

    public void stopSensors() {
        if(isRunning) {
            mSensorManager.unregisterListener(mCompassChangesListener);
            mSensorManager.unregisterListener(mAcceleratorChangesListener);
        }
        this.isRunning = false;
    }

    private class CompassChangesListener implements SensorEventListener {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}

        // The following method is required by the SensorEventListener interface;
        // Hook this event to process updates;
        public void onSensorChanged(SensorEvent event) {
            float azimuth = event.values[0];
            // The other values provided are:
            //  float pitch = event.values[1];
            //  float roll = event.values[2];
            sListener.compassChanged(azimuth);
        }
    }

    private class AcceleratorChangesListener implements SensorEventListener {
        double mAccel = 0.00;
        double mAccelCurrent = SensorManager.GRAVITY_EARTH;
        double mAccelLast = SensorManager.GRAVITY_EARTH;

        public void onAccuracyChanged(Sensor sensor, int accuracy) {}

        public void onSensorChanged(SensorEvent event) {
            double x = event.values[0];
            double y = event.values[1];
            double z = event.values[2];
            this.mAccelLast = mAccelCurrent;
            this.mAccelCurrent = (double) Math.sqrt((double) (x*x + y*y + z*z));
            double delta = this.mAccelCurrent - this.mAccelLast;
            this.mAccel = this.mAccel * 0.9f + delta; // perform low-cut filter
            sListener.acceleratorChanged(
                    (double) Math.round(this.mAccel*10)/10 );
        }
    }

}
