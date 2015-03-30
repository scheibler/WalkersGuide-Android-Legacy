package org.walkersguide.sensors;

import org.walkersguide.R;
import org.walkersguide.utils.Globals;
import org.walkersguide.utils.SettingsManager;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.widget.Toast;

public class SensorsManager {

    public interface SensorsListener {
        public void compassValueChanged(int degree);
        public void shakeDetected();
    }

    // general variables
    private SettingsManager settingsManager;
    private SensorsListener sListener;
    private Context mContext;
    private Toast messageToast;

    // sensors
    private SensorManager sensorManager;
    private Sensor sensorAccelerometer;
    private Sensor sensorMagneticField;
    private SensorChangesListener sensorChangesListener;

    // compass specific variables
    private float[] valuesMagneticField;
    private boolean hasMagneticFieldData;
    private int latestBearingValue;
    private int numberOfpotentialOutliers;

    // accelerometer specific variables
    // static ones
    private static final int TIME_THRESHOLD = 100;
    private static final int SHAKE_TIMEOUT = 500;
    private static final int SHAKE_DURATION = 1000;
    private static final int SHAKE_COUNT = 3;
    // others
    private float[] valuesAccelerometer;
    private boolean hasAccelerometerData;
    private int forceThreshold;
    private float mLastX=-1.0f, mLastY=-1.0f, mLastZ=-1.0f;
    private long mLastTime;
    private long mLastShake;
    private long mLastForce;
    private int mShakeCount = 0;

    // gps position manager
    private PositionManager positionManager;

    public SensorsManager(Context mContext) {
        this.mContext = mContext;
        messageToast = Toast.makeText(mContext, "", Toast.LENGTH_SHORT);
        settingsManager = ((Globals) mContext.getApplicationContext()).getSettingsManagerInstance();

        // sensor variables
        sensorManager = (SensorManager)mContext.getSystemService(Context.SENSOR_SERVICE);
        sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorMagneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorChangesListener = new SensorChangesListener();

        // compass specific variables
        valuesMagneticField = new float[3];
        hasMagneticFieldData = false;
        latestBearingValue = -1;
        numberOfpotentialOutliers = 0;

        // accelerometer specific variables
        valuesAccelerometer = new float[3];
        hasAccelerometerData = false;

        // gps position
        positionManager = ((Globals) mContext.getApplicationContext()).getPositionManagerInstance();
        positionManager.setRawGPSPositionListener(new MyPositionListener());
    }

    public void setSensorsListener(SensorsListener sListener) {
        this.sListener = sListener;
        if (sListener != null && latestBearingValue > -1)
            sListener.compassValueChanged(latestBearingValue);
    }

    public void resumeSensors() {
        switch (((Globals) mContext).getSettingsManagerInstance().getShakeIntensity()) {
            case 0:
                forceThreshold = 100;
                break;
            case 1:
                forceThreshold = 350;
                break;
            case 2:
                forceThreshold = 600;
                break;
            case 3:
                forceThreshold = 850;
                break;
            case 4:
                forceThreshold = 1100;
                break;
            default:
                forceThreshold = 600;
                break;
        }

        // accelerometer sensor
        sensorManager.registerListener(sensorChangesListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
        // magnetic field sensor
        sensorManager.registerListener(sensorChangesListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void stopSensors() {
        sensorManager.unregisterListener(sensorChangesListener);
    }

    private void calculateShakeIntensity() {
        // code from http://android.hlidskialf.com/blog/code/android-shake-detection-listener
        long now = System.currentTimeMillis();
        if ((now - mLastForce) > SHAKE_TIMEOUT) {
            mShakeCount = 0;
        }
        if ((now - mLastTime) > TIME_THRESHOLD) {
            long diff = now - mLastTime;
            float speed = Math.abs(
                        valuesAccelerometer[0] + valuesAccelerometer[1] + valuesAccelerometer[2]
                        - mLastX - mLastY - mLastZ) / diff * 10000;
            if (speed > forceThreshold) {
                if ((++mShakeCount >= SHAKE_COUNT) && (now - mLastShake > SHAKE_DURATION)) {
                    mLastShake = now;
                    mShakeCount = 0;
                    if (sListener != null)
                        sListener.shakeDetected();
                }
                mLastForce = now;
            }
            mLastTime = now;
            mLastX = valuesAccelerometer[0];
            mLastY = valuesAccelerometer[1];
            mLastZ = valuesAccelerometer[2];
        }
    }

    private class SensorChangesListener implements SensorEventListener {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        public void onSensorChanged(SensorEvent event) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    System.arraycopy(event.values, 0, valuesAccelerometer, 0, 3);
                    hasAccelerometerData = true;
                    calculateShakeIntensity();
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    System.arraycopy(event.values, 0, valuesMagneticField, 0, 3);
                    hasMagneticFieldData = true;
                    break;
            }

            if (hasAccelerometerData && hasMagneticFieldData) {
                float[] matrixR = new float[9];
                float[] matrixI = new float[9];
                float[] orientationValues = new float[3];
                boolean success = SensorManager.getRotationMatrix(
                        matrixR, matrixI, valuesAccelerometer, valuesMagneticField);
                if(success){
                    SensorManager.getOrientation(matrixR, orientationValues);
                    int bearingFromMagneticField = ((int) Math.round(Math.toDegrees(orientationValues[0])) + 360) % 360;
                    int diff = Math.abs(latestBearingValue - bearingFromMagneticField);
                    if (diff > 45 && diff < 315) {
                        numberOfpotentialOutliers++;
                    } else {
                        numberOfpotentialOutliers = 0;
                    }
                    if (settingsManager.useCompassAsBearingSource()
                            && (numberOfpotentialOutliers == 0 || numberOfpotentialOutliers > 3)
                            && bearingFromMagneticField != latestBearingValue) {
                        latestBearingValue = bearingFromMagneticField;
                        if (sListener != null)
                            sListener.compassValueChanged(latestBearingValue);
                    }
                }
            }
        }
    }

    private class MyPositionListener implements PositionManager.PositionListener {
        private static final int smallThresholdValue = 25;
        private static final int bigThresholdValue = 45;
        private static final int maxNumberOfMatches = 5;
        private int matchCounter;

        public MyPositionListener() {
            this.matchCounter = 0;
        }

        public void locationChanged(Location location) {
            int bearingFromGPS = (int) location.getBearing();
            int bearingFromMagneticField = latestBearingValue;
            if (settingsManager.getFakeCompassValues())
                bearingFromMagneticField = 240 + (int)(Math.random() * ((300 - 240) + 1));

            // check, if the diff between the location bearing value and the current compass is
            // smaller than the defined threshold
            // 0 <= diff <= 180
            int diff = Math.abs(bearingFromGPS - bearingFromMagneticField);
            if (diff > 180)
                diff = 360 - diff;
            if (location.getSpeed() < 3.0) {
                if (diff < smallThresholdValue) {
                    if (matchCounter > 0)
                        matchCounter -= 1;
                } else if (diff > bigThresholdValue) {
                    if (matchCounter < maxNumberOfMatches)
                        matchCounter += 1;
                }
            } else {
                if (diff < smallThresholdValue || diff > 180-smallThresholdValue) {
                    if (matchCounter > 0)
                        matchCounter -= 1;
                } else if (diff > bigThresholdValue && diff < 180-bigThresholdValue) {
                    if (matchCounter < maxNumberOfMatches)
                        matchCounter += 1;
                }
            }

            if (matchCounter == 0 && settingsManager.useGPSAsBearingSource()) {
                settingsManager.setBearingSource(SettingsManager.BearingSource.COMPASS);
                messageToast.setText(
                        mContext.getResources().getString(R.string.messageSwitchedToCompass));
                messageToast.show();
            }
            if (matchCounter == maxNumberOfMatches && settingsManager.useCompassAsBearingSource()) {
                settingsManager.setBearingSource(SettingsManager.BearingSource.GPS);
                messageToast.setText(
                        mContext.getResources().getString(R.string.messageSwitchedToGPS));
                messageToast.show();
            }

            if (settingsManager.useGPSAsBearingSource()
                    && bearingFromGPS != latestBearingValue) {
                latestBearingValue = bearingFromGPS;
                if (sListener != null)
                    sListener.compassValueChanged(latestBearingValue);
            }
        }
        public void displayGPSSettingsDialog() {}
    }
}
