package org.walkersguide.userinterface;

import java.text.DateFormat;
import java.util.Date;

import org.walkersguide.R;
import org.walkersguide.utils.CompassCalibrationValidator;
import org.walkersguide.utils.Globals;
import org.walkersguide.utils.PositionManager;
import org.walkersguide.utils.SensorsManager;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class GPSStatusActivity extends  Activity {

    private Globals globalData;
    private PositionManager positionManager;
    private SensorsManager sensorsManager;
    private CompassCalibrationValidator compassCalibrationValidator;
    private LinearLayout mainLayout;
    private Location currentLocation;
    private int compass;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (globalData == null) {
            globalData = ((Globals) getApplicationContext());
        }

        // position manager
        positionManager = globalData.getPositionManagerInstance();
        // sensors manager
        sensorsManager = globalData.getSensorsManagerInstance();
        // compass calibration validator
        compassCalibrationValidator = globalData.getCompasscalibrationvalidatorInstance();

        // load layout
        setContentView(R.layout.activity_gps_status);
        mainLayout = (LinearLayout) findViewById(R.id.linearLayoutMain);

        Button buttonLogging = (Button) mainLayout.findViewById(R.id.buttonLogging);
        buttonLogging.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (positionManager.isLogging()) {
                    positionManager.stopLogging();
                } else {
                    positionManager.startLogging();
                }
                updateUserInterface();
            }
        });
    }

    @Override public void onPause() {
        super.onPause();
        positionManager.stopGPS();
        sensorsManager.stopSensors();
    }

    @Override public void onResume() {
        super.onResume();
        sensorsManager.setSensorsListener(new MySensorsListener());
        sensorsManager.resumeSensors();
        positionManager.setPositionListener(new MyPositionListener());
        positionManager.resumeGPS();
        updateUserInterface();
    }

    public void updateUserInterface() {
        if (currentLocation != null) {
            ((TextView) mainLayout.findViewById(R.id.labelLatitude)).setText(
                    String.format("%1$s %2$f", getResources().getString(R.string.labelLatitude),
                        currentLocation.getLatitude()) );
            ((TextView) mainLayout.findViewById(R.id.labelLongitude)).setText(
                    String.format("%1$s %2$f", getResources().getString(R.string.labelLongitude),
                        currentLocation.getLongitude()) );
            if (currentLocation.getProvider().equals("gps")) {
                ((TextView) mainLayout.findViewById(R.id.labelProvider)).setText(
                        String.format("%1$s %2$s", getResources().getString(R.string.labelProvider),
                            getResources().getString(R.string.locationProviderGPS)) );
            } else if (currentLocation.getProvider().equals("network")) {
                ((TextView) mainLayout.findViewById(R.id.labelProvider)).setText(
                        String.format("%1$s %2$s", getResources().getString(R.string.labelProvider),
                            getResources().getString(R.string.locationProviderNetwork)) );
            } else {
                ((TextView) mainLayout.findViewById(R.id.labelProvider)).setText(
                        String.format("%1$s %2$s", getResources().getString(R.string.labelProvider),
                            currentLocation.getProvider()) );
            }
            if ((currentLocation.getExtras() != null) && (currentLocation.getExtras().containsKey("satellites") == true)) {
                ((TextView) mainLayout.findViewById(R.id.labelSatellites)).setText(
                        String.format("%1$s %2$d", getResources().getString(R.string.labelSatellites),
                            currentLocation.getExtras().getInt("satellites")) );
            }
            if (currentLocation.hasAccuracy()) {
                ((TextView) mainLayout.findViewById(R.id.labelAccuracy)).setText(
                    String.format( getResources().getString(R.string.labelAccuracyFormated),
                        currentLocation.getAccuracy()) );
            }
            if (currentLocation.hasAltitude()) {
                ((TextView) mainLayout.findViewById(R.id.labelAltitude)).setText(
                    String.format( getResources().getString(R.string.labelAltitudeFormated),
                        currentLocation.getAltitude()) );
            }
            if (currentLocation.hasSpeed()) {
                ((TextView) mainLayout.findViewById(R.id.labelSpeed)).setText(
                    String.format( getResources().getString(R.string.labelSpeedFormated),
                        currentLocation.getSpeed()) );
            }
            if (currentLocation.hasBearing()) {
                ((TextView) mainLayout.findViewById(R.id.labelBearing)).setText(
                    String.format( getResources().getString(R.string.labelBearingFormated),
                        currentLocation.getBearing()) );
            }
            ((TextView) mainLayout.findViewById(R.id.labelCompass)).setText(
                    String.format( getResources().getString(R.string.labelCompassFormated), compass) );
            ((TextView) mainLayout.findViewById(R.id.labelTime)).setText(
                    String.format( getResources().getString(R.string.labelTimeFormated),
                        DateFormat.getTimeInstance().format(new Date(currentLocation.getTime())),
                        DateFormat.getDateInstance().format(new Date(currentLocation.getTime())) ));
        }
        Button buttonLogging = (Button) mainLayout.findViewById(R.id.buttonLogging);
        if (positionManager.isLogging()) {
            buttonLogging.setText(getResources().getString(R.string.buttonStopLogging));
        } else {
            buttonLogging.setText(getResources().getString(R.string.buttonStartLogging));
        }
    }

    private class MyPositionListener implements PositionManager.PositionListener {
        public void locationChanged(Location location) {
            if (location != null) {
                currentLocation = location;
                updateUserInterface();
            }
        }

        public void displayGPSSettingsDialog() {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }
    }

    private class MySensorsListener implements SensorsManager.SensorsListener {
        public void compassChanged(float degree) {
            compass = (int) Math.round(degree);
            compassCalibrationValidator.validate(currentLocation, compass);
            updateUserInterface();
        }
        public void acceleratorChanged(double accel) {}
    }
}
