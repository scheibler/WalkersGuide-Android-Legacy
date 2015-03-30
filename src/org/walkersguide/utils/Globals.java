package org.walkersguide.utils;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import org.walkersguide.R;
import org.walkersguide.sensors.PositionManager;
import org.walkersguide.sensors.SensorsManager;

import android.app.Application;
import android.content.Context;
import android.media.MediaPlayer;

/**
 * global values which are used to communicate between fragments and activities
 *
 * List of used variables (hashmap keys):
 *      Point: newRouteStart
 *      Point: newRouteDestination
 *      Route: route
 **/

public class Globals extends Application {

    private HashMap<String, Object> memory = new HashMap<String, Object>();
    private static Context applicationContext;
    private String uniqueId;
    private SettingsManager settingsManagerInstance = null;
    private PositionManager positionManagerInstance;
    private SensorsManager sensorsManagerInstance;
    private POIManager poiManagerInstance;
    private AddressManager addressManagerInstance;
    private KeyboardManager keyboardManagerInstance;

    private Timer mActivityTransitionTimer;
    private TimerTask mActivityTransitionTimerTask;
    private boolean wasInBackground;
    private final long MAX_ACTIVITY_TRANSITION_TIME_MS = 2000;

    @Override public void onCreate() {
        super.onCreate();
        wasInBackground = false;
        applicationContext = this;
    }

    public Object getValue(String key) {
        return memory.get(key);
    }

    public void setValue(String key, Object o) {
        memory.put(key, o);
    }

    public static Context getContext() {
        return applicationContext;
    }

    public String getSessionId() {
        if (uniqueId == null)
            uniqueId = UUID.randomUUID().toString();
        return uniqueId;
    }

    public void killSessionId() {
        uniqueId = null;
    }

    public boolean applicationInBackground() {
        return this.wasInBackground;
    }

    public void setApplicationInBackground(boolean b) {
        this.wasInBackground = b;
    }

    public SettingsManager getSettingsManagerInstance() {
        if (this.settingsManagerInstance == null)
            this.settingsManagerInstance = new SettingsManager(getApplicationContext());
        return this.settingsManagerInstance;
    }

    public void resetSettingsManagerInstance() {
        this.settingsManagerInstance = null;
    }

    public PositionManager getPositionManagerInstance() {
        if (this.positionManagerInstance == null)
            this.positionManagerInstance = new PositionManager(getApplicationContext());
        return this.positionManagerInstance;
    }

    public SensorsManager getSensorsManagerInstance() {
        if (this.sensorsManagerInstance == null)
            this.sensorsManagerInstance = new SensorsManager(getApplicationContext());
        return this.sensorsManagerInstance;
    }

    public POIManager getPOIManagerInstance() {
        if (this.poiManagerInstance == null)
            this.poiManagerInstance = new POIManager(getApplicationContext());
        return this.poiManagerInstance;
    }

    public AddressManager getAddressManagerInstance() {
        if (this.addressManagerInstance == null)
            this.addressManagerInstance = new AddressManager(getApplicationContext());
        return this.addressManagerInstance;
    }

    public KeyboardManager getKeyboardManagerInstance() {
        if (this.keyboardManagerInstance == null)
            this.keyboardManagerInstance = new KeyboardManager(getApplicationContext());
        return this.keyboardManagerInstance;
    }

    public void startActivityTransitionTimer() {
        this.mActivityTransitionTimer = new Timer();
        this.mActivityTransitionTimerTask = new TimerTask() {
            public void run() {
                // is run, when application was sent to background or the screen was turned off
                Globals globalData = ((Globals) Globals.getContext());
                globalData.setApplicationInBackground(true);
                globalData.getPositionManagerInstance().stopGPS();
                globalData.getSensorsManagerInstance().stopSensors();
                //MediaPlayer mp = MediaPlayer.create(globalData.getContext(), R.raw.paused);
                //mp.start();
                //System.out.println("xxx background entered");
            }
        };
        this.mActivityTransitionTimer.schedule(mActivityTransitionTimerTask,
                MAX_ACTIVITY_TRANSITION_TIME_MS);
    }

    public void stopActivityTransitionTimer() {
        if (this.mActivityTransitionTimerTask != null) {
            this.mActivityTransitionTimerTask.cancel();
        }
        if (this.mActivityTransitionTimer != null) {
            this.mActivityTransitionTimer.cancel();
        }
        setApplicationInBackground(false);
    }
}
