package org.walkersguide.sensors;

import org.walkersguide.R;
import org.walkersguide.utils.Globals;
import org.walkersguide.utils.SettingsManager;

import android.content.Context;
import android.hardware.GeomagneticField;
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
    private int bearingOfDevice;
    private SettingsManager settingsManager;
    private SensorsListener sListener;
    private Context mContext;
    private Toast messageToast;

    // sensors
    private SensorManager sensorManager;
    private Sensor sensorAccelerometer;
    private Sensor sensorMagneticField;
    private SensorChangesListener sensorChangesListener;
    private long timeOfLastCompassValue;

    // compass specific variables
    private float[] valuesMagneticField;
    private boolean hasMagneticFieldData;
    private float differenceToTrueNorth;
    private int bearingOfMagneticField[];

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
        bearingOfDevice = -1;
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
        differenceToTrueNorth = 0.0f;
        timeOfLastCompassValue = System.currentTimeMillis();
        bearingOfMagneticField = new int[5];
        for (int i : bearingOfMagneticField)
            i = 0;

        // accelerometer specific variables
        valuesAccelerometer = new float[3];
        hasAccelerometerData = false;

        // gps position
        positionManager = ((Globals) mContext.getApplicationContext()).getPositionManagerInstance();
        positionManager.setRawGPSPositionListener(new MyPositionListener());
    }

    public void setSensorsListener(SensorsListener sListener) {
        this.sListener = sListener;
        if (sListener != null && bearingOfDevice > -1)
            sListener.compassValueChanged(bearingOfDevice);
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
                SensorManager.SENSOR_DELAY_UI);
        // magnetic field sensor
        sensorManager.registerListener(sensorChangesListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_UI);
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
                if(success && System.currentTimeMillis() - timeOfLastCompassValue > 200) {
                    SensorManager.getOrientation(matrixR, orientationValues);
                    System.arraycopy(bearingOfMagneticField, 0, bearingOfMagneticField, 1, bearingOfMagneticField.length-1);
                    bearingOfMagneticField[0] = ((int) Math.round(Math.toDegrees(orientationValues[0]) + differenceToTrueNorth) + 360) % 360;
                    timeOfLastCompassValue = System.currentTimeMillis();

                    // calculate average compass value
                    // Mitsuta method: http://abelian.org/vlf/bearings.html
                    int sum = bearingOfMagneticField[0];
                    int D = bearingOfMagneticField[0];
                    int delta = 0;
                    for (int i=1; i<bearingOfMagneticField.length; i++) {
                        delta = bearingOfMagneticField[i] - D;
                        if (delta < -180) {
                            D = D + delta + 360;
                        } else if (delta < 180) {
                            D = D + delta;
                        } else {
                            D = D + delta - 360;
                        }
                        sum += D;
                    }
                    int average = (((int) sum / bearingOfMagneticField.length) + 360) % 360;

                    // decide, if we accept the compass value as new device wide bearing value
                    if (settingsManager.useCompassAsBearingSource()
                            && bearingOfDevice != average) {
                        bearingOfDevice = average;
                        if (sListener != null)
                            sListener.compassValueChanged(bearingOfDevice);
                    }
                }
            }
        }
    }

    private class MyPositionListener implements PositionManager.PositionListener {
        private static final int smallThresholdValue = 20;
        private static final int bigThresholdValueWalk = 30;
        private static final int bigThresholdValueDrive = 40;
        private static final int maxNumberOfMatches = 5;
        private int matchCounter;

        public MyPositionListener() {
            this.matchCounter = 0;
        }

        public void locationChanged(Location location) {
            // geomagnetic offset
            if (location.hasAltitude()) {
                GeomagneticField geoField = new GeomagneticField(
                        Double.valueOf(location.getLatitude()).floatValue(),
                        Double.valueOf(location.getLongitude()).floatValue(),
                        Double.valueOf(location.getAltitude()).floatValue(),
                        System.currentTimeMillis());
                differenceToTrueNorth = geoField.getDeclination();
            }

            // load bearing values of GPS and compass
            int bearingOfGPS = bearingOfMagneticField[0];
            if (location.hasBearing())
                bearingOfGPS = (int) location.getBearing();

            // check, if the diff between the location bearing value and the current compass is
            // smaller than the defined threshold
            // 0 <= diff <= 180
            int diff = Math.abs(bearingOfGPS - bearingOfMagneticField[0]);
            if (diff > 180)
                diff = 360 - diff;
            if (location.hasSpeed() && location.getSpeed() > 3.0) {
                if (diff < smallThresholdValue || diff > 180-smallThresholdValue) {
                    if (matchCounter > 0)
                        matchCounter -= 1;
                } else if (diff > bigThresholdValueDrive && diff < 180-bigThresholdValueDrive) {
                    if (matchCounter < maxNumberOfMatches)
                        matchCounter += 1;
                }
            } else {
                if (diff < smallThresholdValue) {
                    if (matchCounter > 0)
                        matchCounter -= 1;
                } else if (diff > bigThresholdValueWalk) {
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
                    && bearingOfGPS != bearingOfDevice) {
                bearingOfDevice = bearingOfGPS;
                if (sListener != null)
                    sListener.compassValueChanged(bearingOfDevice);
            }
        }
    }
}
