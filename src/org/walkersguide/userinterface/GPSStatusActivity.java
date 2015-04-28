package org.walkersguide.userinterface;

import java.text.DateFormat;
import java.util.Date;

import org.walkersguide.R;
import org.walkersguide.sensors.PositionManager;
import org.walkersguide.sensors.SensorsManager;
import org.walkersguide.utils.Globals;
import org.walkersguide.utils.SettingsManager;

import android.location.Location;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

public class GPSStatusActivity extends  AbstractActivity {

    private Globals globalData;
    private PositionManager positionManager;
    private SensorsManager sensorsManager;
    private SettingsManager settingsManager;
    private LinearLayout mainLayout;

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
        // load layout
        setContentView(R.layout.activity_gps_status);
        mainLayout = (LinearLayout) findViewById(R.id.linearLayoutMain);
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

    private class MyPositionListener implements PositionManager.PositionListener {
        public void locationChanged(Location location) {
            if (location != null) {
                ((TextView) mainLayout.findViewById(R.id.labelLatitude)).setText(
                String.format("%1$s %2$f", getResources().getString(R.string.labelLatitude),
                    location.getLatitude()) );
            } else {
                ((TextView) mainLayout.findViewById(R.id.labelLatitude)).setText(
                getResources().getString(R.string.labelLatitude));
            }
            if (location != null) {
                ((TextView) mainLayout.findViewById(R.id.labelLongitude)).setText(
                String.format("%1$s %2$f", getResources().getString(R.string.labelLongitude),
                    location.getLongitude()) );
            } else {
                ((TextView) mainLayout.findViewById(R.id.labelLongitude)).setText(
                getResources().getString(R.string.labelLongitude));
            }
            if (location != null) {
                if (location.getProvider().equals("gps")) {
                    ((TextView) mainLayout.findViewById(R.id.labelProvider)).setText(
                    String.format("%1$s %2$s", getResources().getString(R.string.labelProvider),
                        getResources().getString(R.string.locationProviderGPS)) );
                } else if (location.getProvider().equals("network")) {
                    ((TextView) mainLayout.findViewById(R.id.labelProvider)).setText(
                    String.format("%1$s %2$s", getResources().getString(R.string.labelProvider),
                        getResources().getString(R.string.locationProviderNetwork)) );
                } else if (location.getProvider().equals("fused")) {
                    ((TextView) mainLayout.findViewById(R.id.labelProvider)).setText(
                    String.format("%1$s %2$s", getResources().getString(R.string.labelProvider),
                        getResources().getString(R.string.locationProviderFused)) );
                } else {
                    ((TextView) mainLayout.findViewById(R.id.labelProvider)).setText(
                    String.format("%1$s %2$s", getResources().getString(R.string.labelProvider),
                        location.getProvider()) );
                }
            } else {
                ((TextView) mainLayout.findViewById(R.id.labelProvider)).setText(
                getResources().getString(R.string.labelProvider));
            }
            if (location != null
                    && location.getExtras() != null
                    && location.getExtras().containsKey("satellites")) {
                ((TextView) mainLayout.findViewById(R.id.labelSatellites)).setText(
                String.format("%1$s %2$d", getResources().getString(R.string.labelSatellites),
                    location.getExtras().getInt("satellites")) );
            } else {
                ((TextView) mainLayout.findViewById(R.id.labelSatellites)).setText(
                getResources().getString(R.string.labelSatellites));
            }
            if (location != null && location.hasAccuracy()) {
                ((TextView) mainLayout.findViewById(R.id.labelAccuracy)).setText(
                String.format("%1$s %2$.0f Meter", getResources().getString(R.string.labelAccuracy),
                    location.getAccuracy()) );
            } else {
                ((TextView) mainLayout.findViewById(R.id.labelAccuracy)).setText(
                getResources().getString(R.string.labelAccuracy));
            }
            if (location != null && location.hasAltitude()) {
                ((TextView) mainLayout.findViewById(R.id.labelAltitude)).setText(
                String.format("%1$s %2$.0f Meter", getResources().getString(R.string.labelAltitude),
                    location.getAltitude()) );
            } else {
                ((TextView) mainLayout.findViewById(R.id.labelAltitude)).setText(
                getResources().getString(R.string.labelAltitude));
            }
            if (location != null && location.hasSpeed()) {
                ((TextView) mainLayout.findViewById(R.id.labelSpeed)).setText(
                String.format("%1$s %2$.1f km/h", getResources().getString(R.string.labelSpeed),
                    location.getSpeed() * 3.6) );
            } else {
                ((TextView) mainLayout.findViewById(R.id.labelSpeed)).setText(
                getResources().getString(R.string.labelSpeed));
            }
            if (location != null && location.hasBearing()) {
                ((TextView) mainLayout.findViewById(R.id.labelBearing)).setText(
                String.format("%1$s %2$.0f°", getResources().getString(R.string.labelBearing),
                    location.getBearing()) );
            } else {
                ((TextView) mainLayout.findViewById(R.id.labelBearing)).setText(
                getResources().getString(R.string.labelBearing));
            }
            if (location != null) {
                ((TextView) mainLayout.findViewById(R.id.labelTime)).setText(
                String.format("%1$s %2$s %3$s", getResources().getString(R.string.labelTime),
                    DateFormat.getTimeInstance().format(new Date(location.getTime())),
                    DateFormat.getDateInstance().format(new Date(location.getTime())) ));
            } else {
                ((TextView) mainLayout.findViewById(R.id.labelTime)).setText(
                getResources().getString(R.string.labelTime));
            }
        }
    }

    private class MySensorsListener implements SensorsManager.SensorsListener {
        public void compassValueChanged(int degree) {
            ((TextView) mainLayout.findViewById(R.id.labelCompass)).setText(
                String.format("%1$s %2$d°", getResources().getString(R.string.labelCompass), degree));
            if (settingsManager.useGPSAsBearingSource()) {
                ((TextView) mainLayout.findViewById(R.id.labelBearingSource)).setText(
                    getResources().getString(R.string.labelUseGPSAsBearingSource));
            } else {
                ((TextView) mainLayout.findViewById(R.id.labelBearingSource)).setText(
                    getResources().getString(R.string.labelUseAccMagAsBearingSource));
            }
        }
        public void shakeDetected() {}
    }
}
