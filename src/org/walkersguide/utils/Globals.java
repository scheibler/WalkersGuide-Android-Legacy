package org.walkersguide.utils;

import java.util.HashMap;

import android.app.Application;
import android.content.Context;

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
    private SettingsManager settingsManagerInstance = null;
    private PositionManager positionManagerInstance;
    private SensorsManager sensorsManagerInstance;
    private POIManager poiManagerInstance;
    private AddressManager addressManagerInstance;
    private KeyboardManager keyboardManagerInstance;
    private CompassCalibrationValidator compasscalibrationvalidatorInstance;

    @Override public void onCreate() {
        super.onCreate();
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

    public CompassCalibrationValidator getCompasscalibrationvalidatorInstance() {
        if (this.compasscalibrationvalidatorInstance == null)
            this.compasscalibrationvalidatorInstance = new CompassCalibrationValidator(getApplicationContext());
        return this.compasscalibrationvalidatorInstance;
    }

}
