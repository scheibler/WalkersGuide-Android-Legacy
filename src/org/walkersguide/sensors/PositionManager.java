package org.walkersguide.sensors;

import java.text.SimpleDateFormat;

import org.walkersguide.R;
import org.walkersguide.routeobjects.Point;
import org.walkersguide.userinterface.DialogActivity;
import org.walkersguide.utils.DataLogger;
import org.walkersguide.utils.Globals;
import org.walkersguide.utils.SettingsManager;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

public class PositionManager {

    public enum Status {
        DISABLED, SIMULATION, GPS
    }

    public interface PositionListener {
        public void locationChanged(Location location);
        public void displayGPSSettingsDialog();
    }

	private static final String tag = "PositionManager"; // for Log
    private PositionListener pRawGPSListener, pFilteredListener;
    private Context mContext;
    private Toast messageToast;
    private SettingsManager settingsManager;
    private DataLogger dataLogger;
    private boolean startLogging;
    private Vibrator vibrator;

    private Status status;
    private boolean simulationActivated;
    private String simulationName;
    private boolean showSpeedMessage;
    private Location currentBestLocation;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private long lastMatchTime;

    // warn, if we get no new location at all
    private Handler mHandler;
    private GPSTimer gpsTimer;

    public PositionManager(Context mContext) {
        this.mContext = mContext;
        Globals globalData = ((Globals) mContext);
        messageToast = Toast.makeText(mContext, "", Toast.LENGTH_SHORT);
        settingsManager = globalData.getSettingsManagerInstance();
        dataLogger = new DataLogger(settingsManager.getProgramLogFolder() + "/00_gps_data.txt");
        startLogging = true;
        status = Status.DISABLED;
        simulationActivated = false;
        this.simulationName = "";
        this.mHandler = new Handler();
        this.gpsTimer = new GPSTimer();
        vibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        lastMatchTime = System.currentTimeMillis();

        // Figure out if we have a location somewhere that we can use as a current best location
        //mGoogleApiClient = new GoogleApiClient.Builder(this)
        //    .addConnectionCallbacks(this)
        //    .addOnConnectionFailedListener(this)
        //    .addApi(LocationServices.API).build();
        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener();
        Location lastKnownSavedLocation = settingsManager.loadLastLocation();
        if( isBetterLocation(lastKnownSavedLocation, currentBestLocation)) {
            currentBestLocation = lastKnownSavedLocation;
            dataLogger.appendLog("xxx saved ist besser .. " + lastKnownSavedLocation.toString());
        }
        Location lastKnownNetworkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if( isBetterLocation(lastKnownNetworkLocation, currentBestLocation) ) {
            currentBestLocation = lastKnownNetworkLocation;
            dataLogger.appendLog("xxx netzwerk ist besser .. " + lastKnownNetworkLocation.toString());
        }
        Location lastKnownGPSLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if( isBetterLocation(lastKnownGPSLocation, currentBestLocation) ) {
            currentBestLocation = lastKnownGPSLocation;
            dataLogger.appendLog("xxx gps ist besser .. " + lastKnownGPSLocation.toString());
        }
        dataLogger.appendLog("initialisierung fertig");
        startLogging = false;
        showSpeedMessage = false;
    }

    public void setPositionListener(PositionListener pFilteredListener) {
        this.pFilteredListener = pFilteredListener;
        getLastKnownLocation();
    }

    public void setRawGPSPositionListener(PositionListener pRawGPSListener) {
        this.pRawGPSListener = pRawGPSListener;
    }

    public void startLogging() {
        dataLogger = new DataLogger(settingsManager.getProgramLogFolder() + "/00_gps_data.txt");
        startLogging = true;
        showSpeedMessage = false;
    }

    public void stopLogging() {
        startLogging = false;
    }

    public boolean isLogging() {
        return startLogging;
    }

    public void getLastKnownLocation() {
        if (pFilteredListener != null) {
            if (currentBestLocation != null) {
                pFilteredListener.locationChanged(currentBestLocation);
            } else {
                messageToast.setText("no location found");
                messageToast.show();
            }
        }
    }

    public void resumeGPS() {
        if (status == Status.DISABLED) {
            changeStatus(Status.GPS, null);
        }
    }

    public void stopGPS() {
        if (status == Status.GPS) {
            changeStatus(Status.DISABLED, null);
        }
    }

    public void changeStatus(Status newStatus, Point simulatedPosition) {
        switch (newStatus) {
            case DISABLED:
                if (status == Status.GPS) {
                    locationManager.removeUpdates(locationListener);
                    locationListener = null;
                    locationManager = null;
                    mHandler.removeCallbacks(gpsTimer);
                    System.out.println("xx gps disabled");
                }
                status = newStatus;
                return;
            case SIMULATION:
                if (simulatedPosition == null) {
                    Toast.makeText(mContext,
                            mContext.getResources().getString(R.string.messagePosSimulationFailedNoLocation),
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if (status == Status.GPS) {
                    locationManager.removeUpdates(locationListener);
                    locationListener = null;
                    locationManager = null;
                    mHandler.removeCallbacks(gpsTimer);
                }
                if (settingsManager.useGPSAsBearingSource()) {
                    settingsManager.setBearingSource(SettingsManager.BearingSource.COMPASS);
                    messageToast.setText(
                            mContext.getResources().getString(R.string.messageSwitchedToCompass));
                    messageToast.show();
                }
                if (simulatedPosition.getLocationObject() != null) {
                    currentBestLocation = simulatedPosition.getLocationObject();
                } else {
                    currentBestLocation = new Location("gps");
                    currentBestLocation.setLatitude(simulatedPosition.getLatitude());
                    currentBestLocation.setLongitude(simulatedPosition.getLongitude());
                    currentBestLocation.setTime(System.currentTimeMillis());
                }
                simulationName = simulatedPosition.getName();
                if (pFilteredListener != null)
                    pFilteredListener.locationChanged(currentBestLocation);
                simulationActivated = true;
                status = newStatus;
                return;
            case GPS:
                locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
                locationListener = new LocationListener();
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 0, locationListener);
                gpsTimer.reset();
                mHandler.postDelayed(gpsTimer, 5000);
                currentBestLocation = settingsManager.loadLastLocation();
                if (pFilteredListener != null)
                    pFilteredListener.locationChanged(currentBestLocation);
                simulationActivated = false;
                status = newStatus;
                return;
            default:
                return;
        }
    }

    public Status getStatus() {
        return this.status;
    }

    public String getSimulationObjectName() {
        if (status == Status.SIMULATION)
            return simulationName;
        return "";
    }

    public boolean isBetterLocation(Location location, Location currentBestLocation) {
        // some trivial tests
        if (this.status == Status.SIMULATION) {
            dataLogger.appendLog("simulation");
            return false;
        } else if (location == null) {
            dataLogger.appendLog("new location is null");
            return false;
        } else if (currentBestLocation == null) {
            dataLogger.appendLog("current best location is null");
            return true;
        }

        // define log variables
        boolean locationAccepted = false;
        int logFlag = 0;
        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isOlder = timeDelta < -2000;
        boolean isNewer = timeDelta > 0;
        boolean isABitNewer = timeDelta > 5000;
        boolean isSignificantlyNewer = timeDelta > 10000;
        boolean isMuchMuchNewer = timeDelta > 180000;
        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        int accuracyThresholdValue = 15;
        boolean isMoreAccurateThanThresholdValue = location.getAccuracy() <= accuracyThresholdValue;
        boolean isMoreAccurate = false;
        boolean isABitLessAccurate = false;
        boolean isSignificantlyLessAccurate = false;
        if (location.getAccuracy() < (2*accuracyThresholdValue)) {
            isMoreAccurate = accuracyDelta <= 0;
            isABitLessAccurate = accuracyDelta <= 10;
            isSignificantlyLessAccurate = accuracyDelta <= 30;
        } else {
            isMoreAccurate = accuracyDelta < 0;
            isABitLessAccurate = accuracyDelta < 10;
            isSignificantlyLessAccurate = accuracyDelta < 30;
        }
        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // if the location is older than the currentBestLocation, drop it
        if (isOlder) {
            dataLogger.appendLog("new location is much older");
            return false;
        }

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurateThanThresholdValue) {
            locationAccepted = true;
            logFlag = 1;
        } else if (isNewer && isMoreAccurate && isFromSameProvider) {
            locationAccepted = true;
            logFlag = 2;
        } else if (isABitNewer && isABitLessAccurate && isFromSameProvider) {
            locationAccepted = true;
            logFlag = 3;
        } else if (isSignificantlyNewer && isSignificantlyLessAccurate) {
            locationAccepted = true;
            logFlag = 4;
        } else if (isMuchMuchNewer) {
            locationAccepted = true;
            logFlag = 5;
        }

        int distance = Math.round(location.distanceTo(currentBestLocation));
        double speed = 0.0;
        if (timeDelta >= 1) {
            speed = ( (distance*1.0) / (timeDelta/1000) );
        }
        if (startLogging == true) {
            SimpleDateFormat formater = new SimpleDateFormat("HH:mm:ss, dd.MM.yyyy");
            if (speed > 3 && !showSpeedMessage) {
                //messageToast.setText("Warn: Speed = " + String.format("%.2f", speed) + " m/s. " + distance + " / "
                //        + timeDelta);
                //messageToast.show();
                showSpeedMessage = true;
                dataLogger.appendLog( String.format("%d; SpeEd* = %.1f (%d/%d); %s; acc_new = %.1f, acc_old = %.1f; bearing = %.1f; stamp = %s",
                            logFlag, speed, distance, timeDelta, location.getProvider(), location.getAccuracy(),
                            currentBestLocation.getAccuracy(), location.getBearing(), formater.format(location.getTime()) ));
            } else {
                dataLogger.appendLog( String.format("%d; Speed = %.1f (%d/%d); %s; acc_new = %.1f, acc_old = %.1f; bearing = %.1f; stamp = %s",
                            logFlag, speed, distance, timeDelta, location.getProvider(), location.getAccuracy(),
                            currentBestLocation.getAccuracy(), location.getBearing(), formater.format(location.getTime()) ));
            }
        }
        return locationAccepted;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    private class LocationListener implements android.location.LocationListener {

        private final String tag = LocationListener.class.getSimpleName();

        @Override public void onLocationChanged(Location newLocation) {
            gpsTimer.updateTimeOfLastFix();
            if(isBetterLocation(newLocation, currentBestLocation)) {
                currentBestLocation = newLocation;
                gpsTimer.updateTimeOfLastAcceptedFix();
                settingsManager.storeLastLocation(currentBestLocation);
                if (pFilteredListener != null)
                    pFilteredListener.locationChanged(currentBestLocation);
                if (newLocation != null
                        && ! newLocation.getProvider().equals("gps")
                        && settingsManager.useGPSAsBearingSource()) {
                    settingsManager.setBearingSource(SettingsManager.BearingSource.COMPASS);
                    messageToast.setText(
                            mContext.getResources().getString(R.string.messageSwitchedToCompass));
                    messageToast.show();
                }
                if (pRawGPSListener != null
                        && System.currentTimeMillis() - lastMatchTime > 5000
                        && newLocation != null
                        && newLocation.getProvider().equals("gps")
                        && newLocation.hasBearing()
                        && newLocation.hasSpeed()
                        && newLocation.hasAccuracy()
                        && ( (newLocation.getSpeed() > 0.66 && newLocation.getAccuracy() < 20.0)
                            || (newLocation.getSpeed() > 1.33 && newLocation.getAccuracy() < 40.0)) ) {
                    lastMatchTime = System.currentTimeMillis();
                    pRawGPSListener.locationChanged(newLocation);
                }
            }
        }

        @Override public void onStatusChanged(String provider, int status, Bundle extras) {
            // This is called when the GPS status alters
            switch (status) {
                case LocationProvider.OUT_OF_SERVICE:
                    Log.v(tag, "Status Changed: Out of Service");
                    // Toast.makeText(mContext, "Status Changed: Out of Service, Toast.LENGTH_SHORT).show();
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    Log.v(tag, "Status Changed: Temporarily Unavailable");
                    // Toast.makeText(mContext, "Status Changed: Temporarily Unavailable", Toast.LENGTH_SHORT).show();
                    break;
                case LocationProvider.AVAILABLE:
                    Log.v(tag, "Status Changed: Available");
                    // Toast.makeText(mContext, "Status Changed: Available", Toast.LENGTH_SHORT).show();
                    break;
            }
        }

        @Override public void onProviderDisabled(String provider) {
            /* this is called if/when the GPS is disabled in settings */
            if (pFilteredListener != null)
                pFilteredListener.displayGPSSettingsDialog();
        }

        @Override public void onProviderEnabled(String provider) {
            Log.v(tag, "Enabled");
            // Toast.makeText(mContext, "GPS reEnabled", Toast.LENGTH_SHORT).show();
        }
    }

    private class GPSTimer implements Runnable {
        private long timeOfLastFix;
        private long timeOfLastAcceptedFix;
        private boolean positionManagementRestarted;
        private boolean noFixMessage;
        private boolean noAcceptedFixMessage;

        public GPSTimer() {
            this.positionManagementRestarted = false;
            this.noFixMessage = false;
            reset();
        }

        public void reset() {
            this.timeOfLastFix = System.currentTimeMillis();
            this.timeOfLastAcceptedFix = System.currentTimeMillis();
            this.noAcceptedFixMessage = false;
        }

        public void updateTimeOfLastFix() {
            this.timeOfLastFix = System.currentTimeMillis();
        }

        public void updateTimeOfLastAcceptedFix() {
            this.timeOfLastAcceptedFix = System.currentTimeMillis();
        }

        public void run() {
            long currentTime = System.currentTimeMillis();
            if ( ((currentTime - timeOfLastAcceptedFix) > 15000)
                    && getStatus() == Status.GPS
                    && currentBestLocation != null
                    && currentBestLocation.getProvider().equals("gps")
                    && noAcceptedFixMessage == false ) {
                noAcceptedFixMessage = true;
                long[] pattern = {0, 100, 75, 100, 75, 100, 75, 100};
                vibrator.vibrate(pattern, -1);
            }
            if ( ((currentTime - timeOfLastAcceptedFix) < 15000) && (noAcceptedFixMessage == true) ) {
                noAcceptedFixMessage = false;
            }
            if ( ((currentTime - timeOfLastFix) > 30000) && (positionManagementRestarted == false) ) {
                stopGPS();
                resumeGPS();
                positionManagementRestarted = true;
            }
            if ( ((currentTime - timeOfLastFix) > 60000) && (noFixMessage == false) ) {
                Intent intent = new Intent(mContext.getApplicationContext(), DialogActivity.class);
                intent.putExtra("message", mContext.getResources().getString(R.string.messageGotNoLocationAtAll));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
                noFixMessage = true;
            }
            if ( ((currentTime - timeOfLastFix) < 60000) && (noFixMessage == true) ) {
                noFixMessage = false;
            }
            mHandler.postDelayed(this, 3000);
        }
    }

}
