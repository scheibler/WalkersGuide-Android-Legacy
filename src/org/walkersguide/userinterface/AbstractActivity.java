package org.walkersguide.userinterface;

import org.walkersguide.sensors.PositionManager;
import org.walkersguide.utils.Globals;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.provider.Settings;

import com.google.android.gms.common.ConnectionResult;

public abstract class AbstractActivity extends Activity
    implements PositionManager.ErrorMessagesListener {

    private Globals globalData;
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        globalData = ((Globals) Globals.getContext());
        globalData.getPositionManagerInstance().setErrorMessageListener(this);
    }

    @Override public void onPause() {
        super.onPause();
        globalData.startActivityTransitionTimer();
    }

    @Override public void onResume() {
        super.onResume();
        if (globalData.applicationInBackground()) {
            globalData.getPositionManagerInstance().resumeGPS();
            globalData.getSensorsManagerInstance().resumeSensors();
            //MediaPlayer mp = MediaPlayer.create(this, R.raw.restored);
            //mp.start();
            //System.out.println("xxx app resumed from background");
        }
        globalData.stopActivityTransitionTimer();
    }

    @Override public void showGPSSettingsDialog() {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
    }
}
