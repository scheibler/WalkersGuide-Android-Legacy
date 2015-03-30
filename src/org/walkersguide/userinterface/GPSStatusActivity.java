package org.walkersguide.userinterface;

import java.text.DateFormat;
import java.util.Date;

import org.walkersguide.R;
import org.walkersguide.sensors.PositionManager;
import org.walkersguide.sensors.SensorsManager;
import org.walkersguide.utils.Globals;
import org.walkersguide.utils.SettingsManager;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class GPSStatusActivity extends  AbstractActivity {

    private Globals globalData;
    private PositionManager positionManager;
    private SensorsManager sensorsManager;
    private SettingsManager settingsManager;
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
        // settings manager
        settingsManager = globalData.getSettingsManagerInstance();
        compass = -1;

        // load layout
        setContentView(R.layout.activity_gps_status);
        mainLayout = (LinearLayout) findViewById(R.id.linearLayoutMain);

        Button buttonLogging = (Button) mainLayout.findViewById(R.id.buttonLogging);
        buttonLogging.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (positionManager.isLogging()) {
                    positionManager.stopLogging();
                    settingsManager.setFakeCompassValues(false);
                } else {
                    positionManager.startLogging();
                    settingsManager.setFakeCompassValues(true);
                }
                updateUserInterface();
            }
        });
    }

    @Override public void onPause() {
        super.onPause();
        sensorsManager.setSensorsListener(null);
        positionManager.setPositionListener(null);
    }

    @Override public void onResume() {
        super.onResume();
        sensorsManager.setSensorsListener(new MySensorsListener());
        positionManager.setPositionListener(new MyPositionListener());
    }

    public synchronized void updateUserInterface() {
        if (currentLocation != null) {
            ((TextView) mainLayout.findViewById(R.id.labelLatitude)).setText(
                    String.format("%1$s %2$f", getResources().getString(R.string.labelLatitude),
                        currentLocation.getLatitude()) );
        } else {
            ((TextView) mainLayout.findViewById(R.id.labelLatitude)).setText(
                getResources().getString(R.string.labelLatitude));
        }
        if (currentLocation != null) {
            ((TextView) mainLayout.findViewById(R.id.labelLongitude)).setText(
                    String.format("%1$s %2$f", getResources().getString(R.string.labelLongitude),
                        currentLocation.getLongitude()) );
        } else {
            ((TextView) mainLayout.findViewById(R.id.labelLongitude)).setText(
                getResources().getString(R.string.labelLongitude));
        }
        if (currentLocation != null) {
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
        } else {
            ((TextView) mainLayout.findViewById(R.id.labelProvider)).setText(
                getResources().getString(R.string.labelProvider));
        }
        if (currentLocation != null
                && currentLocation.getExtras() != null
                && currentLocation.getExtras().containsKey("satellites")) {
            ((TextView) mainLayout.findViewById(R.id.labelSatellites)).setText(
                String.format("%1$s %2$d", getResources().getString(R.string.labelSatellites),
                    currentLocation.getExtras().getInt("satellites")) );
        } else {
            ((TextView) mainLayout.findViewById(R.id.labelSatellites)).setText(
                getResources().getString(R.string.labelSatellites));
        }
        if (currentLocation != null && currentLocation.hasAccuracy()) {
            ((TextView) mainLayout.findViewById(R.id.labelAccuracy)).setText(
                String.format("%1$s %2$.0f Meter", getResources().getString(R.string.labelAccuracy),
                    currentLocation.getAccuracy()) );
        } else {
            ((TextView) mainLayout.findViewById(R.id.labelAccuracy)).setText(
                getResources().getString(R.string.labelAccuracy));
        }
        if (currentLocation != null && currentLocation.hasAltitude()) {
            ((TextView) mainLayout.findViewById(R.id.labelAltitude)).setText(
                String.format("%1$s %2$.0f Meter", getResources().getString(R.string.labelAltitude),
                    currentLocation.getAltitude()) );
        } else {
            ((TextView) mainLayout.findViewById(R.id.labelAltitude)).setText(
                getResources().getString(R.string.labelAltitude));
        }
        if (currentLocation != null && currentLocation.hasSpeed()) {
            ((TextView) mainLayout.findViewById(R.id.labelSpeed)).setText(
                String.format("%1$s %2$.1f km/h", getResources().getString(R.string.labelSpeed),
                    currentLocation.getSpeed() * 3.6) );
        } else {
            ((TextView) mainLayout.findViewById(R.id.labelSpeed)).setText(
                getResources().getString(R.string.labelSpeed));
        }
        if (currentLocation != null && currentLocation.hasBearing()) {
            ((TextView) mainLayout.findViewById(R.id.labelBearing)).setText(
                String.format("%1$s %2$.0f°", getResources().getString(R.string.labelBearing),
                    currentLocation.getBearing()) );
        } else {
            ((TextView) mainLayout.findViewById(R.id.labelBearing)).setText(
                getResources().getString(R.string.labelBearing));
        }
        if (currentLocation != null) {
            ((TextView) mainLayout.findViewById(R.id.labelTime)).setText(
                String.format("%1$s %2$s %3$s", getResources().getString(R.string.labelTime),
                    DateFormat.getTimeInstance().format(new Date(currentLocation.getTime())),
                    DateFormat.getDateInstance().format(new Date(currentLocation.getTime())) ));
        } else {
            ((TextView) mainLayout.findViewById(R.id.labelTime)).setText(
                getResources().getString(R.string.labelTime));
        }
        if (compass >= 0) {
            ((TextView) mainLayout.findViewById(R.id.labelCompass)).setText(
                String.format("%1$s %2$d°", getResources().getString(R.string.labelCompass), compass));
        } else {
            ((TextView) mainLayout.findViewById(R.id.labelCompass)).setText(
                getResources().getString(R.string.labelCompass));
        }
        if (settingsManager.useGPSAsBearingSource()) {
            ((TextView) mainLayout.findViewById(R.id.labelBearingSource)).setText(
                getResources().getString(R.string.labelUseGPSAsBearingSource));
        } else {
            ((TextView) mainLayout.findViewById(R.id.labelBearingSource)).setText(
                getResources().getString(R.string.labelUseAccMagAsBearingSource));
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
            currentLocation = location;
            updateUserInterface();
        }
        public void displayGPSSettingsDialog() {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }
    }

    private class MySensorsListener implements SensorsManager.SensorsListener {
        public void compassValueChanged(int degree) {
            compass = degree;
            updateUserInterface();
        }
        public void shakeDetected() {}
    }
}
