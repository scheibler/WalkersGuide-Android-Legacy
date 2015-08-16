package org.walkersguide.userinterface;

import org.walkersguide.sensors.PositionManager;
import org.walkersguide.utils.Globals;
import org.walkersguide.utils.RemoteControlReceiver;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.provider.Settings;

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
        System.out.println("xxx abstract activity onResume");
        // null all listeners
        globalData.getAddressManagerInstance().setAddressListener(null);
        globalData.getKeyboardManagerInstance().setKeyboardListener(null);
        globalData.getPOIManagerInstance().setPOIListener(null);
        globalData.getPositionManagerInstance().setPositionListener(null);
        globalData.getSensorsManagerInstance().setSensorsListener(null);

        // resume position and sensors if necessary
        if (globalData.applicationInBackground()
                && ! globalData.getSettingsManagerInstance().getStayActiveInBackground()) {
            globalData.getPositionManagerInstance().resumeGPS();
            globalData.getSensorsManagerInstance().resumeSensors();
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            audioManager.registerMediaButtonEventReceiver(
                    new ComponentName(getPackageName(), RemoteControlReceiver.class.getName()));
        }
        globalData.stopActivityTransitionTimer();
    }

    @Override public void showGPSSettingsDialog() {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
    }
}
