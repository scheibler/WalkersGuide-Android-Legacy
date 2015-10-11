package org.walkersguide.sensors;

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
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

public class PositionManager implements ConnectionCallbacks, OnConnectionFailedListener,
       com.google.android.gms.location.LocationListener,
       android.location.LocationListener {

    public enum Status {
        DISABLED, SIMULATION, GPS
    }

    public interface PositionListener {
        public void locationChanged(Location location);
    }
    public interface ErrorMessagesListener {
        public void showGPSSettingsDialog();
    }

    private static final String tag = "PositionManager"; // for Log
    private Globals globalData;
    private PositionListener pRawGPSListener, pFilteredListener;
    private ErrorMessagesListener pErrorMessagesListener;
    private Context mContext;
    private Toast messageToast;
    private SettingsManager settingsManager;
    private Vibrator vibrator;
    private DataLogger logger;

    private Status status;
    private boolean simulationActivated;
    private String simulationName;
    private Location currentBestLocation;
    private LocationManager locationManager;
    private LocationRequest locationRequest;
    private GoogleApiClient locationClient;
    private long lastMatchTime;
    private int diffBetweenTwoFixesInMilliseconds;

    // warn, if we get no new location at all
    private Handler mHandler;
    private GPSTimer gpsTimer;

    public PositionManager(Context mContext) {
        this.mContext = mContext;
        globalData = ((Globals) mContext);
        messageToast = Toast.makeText(mContext, "", Toast.LENGTH_SHORT);
        settingsManager = globalData.getSettingsManagerInstance();
        status = Status.DISABLED;
        simulationActivated = false;
        this.simulationName = "";
        this.mHandler = new Handler();
        this.gpsTimer = new GPSTimer();
        vibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        lastMatchTime = System.currentTimeMillis();
        diffBetweenTwoFixesInMilliseconds = 5000;
        //logger = new DataLogger(
        //        settingsManager.getProgramLogFolder() + "/gps_data.txt", false);

        // Figure out if we have a location somewhere that we can use as a current best location
        Location lastKnownSavedLocation = settingsManager.loadLastLocation();
        if (isBetterLocation(lastKnownSavedLocation, currentBestLocation))
            currentBestLocation = lastKnownSavedLocation;
    }

    public void setPositionListener(PositionListener pFilteredListener) {
        this.pFilteredListener = pFilteredListener;
        getLastKnownLocation();
    }

    public void setRawGPSPositionListener(PositionListener pRawGPSListener) {
        this.pRawGPSListener = pRawGPSListener;
    }

    public void setErrorMessageListener(ErrorMessagesListener pErrorMessagesListener) {
        this.pErrorMessagesListener = pErrorMessagesListener;
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
        // check if gps is enabled
        LocationManager lm = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        if (! lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            pErrorMessagesListener.showGPSSettingsDialog();
        } else if (status == Status.DISABLED) {
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
                    if (locationClient != null) {
                        if (locationClient.isConnected())
                            locationClient.disconnect();
                        locationClient = null;
                    }
                    if (locationManager != null) {
                        locationManager.removeUpdates(this);
                        locationManager = null;
                    }
                    mHandler.removeCallbacks(gpsTimer);
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
                    if (locationClient != null) {
                        if (locationClient.isConnected())
                            locationClient.disconnect();
                        locationClient = null;
                    }
                    if (locationManager != null) {
                        locationManager.removeUpdates(this);
                        locationManager = null;
                    }
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
                // fused location provider initialization
                // due to location accuracy problems with the fused provider we use androids location manager again
                //locationClient = new GoogleApiClient.Builder(mContext)
                //    .addApi(LocationServices.API)
                //    .addConnectionCallbacks(this)
                //    .addOnConnectionFailedListener(this)
                //    .build();
                //locationClient.connect();
                locationClient = null;
                locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 0, this);
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
            return false;
        } else if (location == null) {
            return false;
        } else if (currentBestLocation == null) {
            return true;
        }

        // define log variables
        boolean locationAccepted = false;

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isNewer = timeDelta > 0;
        boolean isABitNewer = timeDelta > 10000;
        boolean isSignificantlyNewer = timeDelta > 20000;
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

        // Determine location quality using a combination of timeliness and accuracy
        if (isNewer && isMoreAccurateThanThresholdValue) {
            locationAccepted = true;
        } else if (isNewer && isMoreAccurate && isFromSameProvider) {
            locationAccepted = true;
        } else if (isABitNewer && isABitLessAccurate && isFromSameProvider) {
            locationAccepted = true;
        } else if (isSignificantlyNewer && isSignificantlyLessAccurate) {
            locationAccepted = true;
        } else if (isMuchMuchNewer) {
            locationAccepted = true;
        }

        // log location data
        //logger.appendLog(locationAccepted + ": td = " + timeDelta + " / " + (System.currentTimeMillis() - location.getTime()) + ";    "
        //        + "ad = " + accuracyDelta + " (" + location.getAccuracy() + " / " + currentBestLocation.getAccuracy() + ");   "
        //        + "p = " + location.getProvider() + ";   time = " + DateFormat.getTimeInstance().format(new Date(location.getTime())));
        return locationAccepted;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    @Override public void onLocationChanged(Location newLocation) {
        gpsTimer.updateTimeOfLastFix();
        if(isBetterLocation(newLocation, currentBestLocation)) {
            currentBestLocation = newLocation;
            gpsTimer.updateTimeOfLastAcceptedFix();
            settingsManager.storeLastLocation(currentBestLocation);
            if (pFilteredListener != null) {
                pFilteredListener.locationChanged(currentBestLocation);
            }
            if (globalData.applicationInBackground())
                diffBetweenTwoFixesInMilliseconds = 2000;
            else
                diffBetweenTwoFixesInMilliseconds = 5000;
            // hand the location object to the SensorsManager class to check the compass integrity
            if (pRawGPSListener != null && newLocation != null
                    && System.currentTimeMillis() - lastMatchTime > diffBetweenTwoFixesInMilliseconds) {
                // first case is the fallback for an indoor location object without bearing and speed value
                // if we currently use GPS as bearing source, then we tell the SensorsManager to
                // switch back to compass but we do that carefully
                if (! newLocation.hasBearing()
                        && ! newLocation.hasSpeed()
                        && settingsManager.useGPSAsBearingSource()) {
                    lastMatchTime = System.currentTimeMillis();
                    pRawGPSListener.locationChanged(newLocation);
                // second case requires at least a speed of 2.3 km/h and an accuracy of 20 meters
                } else if (newLocation.hasBearing()
                        && newLocation.hasSpeed() && newLocation.getSpeed() > 0.66
                        && newLocation.hasAccuracy() && newLocation.getAccuracy() < 20.0) {
                    lastMatchTime = System.currentTimeMillis();
                    pRawGPSListener.locationChanged(newLocation);
                // third case requires at least a speed of 4.8 km/h and an accuracy of 30 meters
                } else if (newLocation.hasBearing()
                        && newLocation.hasSpeed() && newLocation.getSpeed() > 1.33
                        && newLocation.hasAccuracy() && newLocation.getAccuracy() < 30.0) {
                    lastMatchTime = System.currentTimeMillis();
                    pRawGPSListener.locationChanged(newLocation);
                }
            }
        }
    }

    // the next three functions belong to the Google FusedProvider
    @Override public void onConnected(Bundle bundle) {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000); // milliseconds
        locationRequest.setFastestInterval(1000); // the fastest rate in milliseconds at which your app can handle location updates
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationServices.FusedLocationApi.requestLocationUpdates(
                locationClient, locationRequest, this);
    }
    @Override public void onConnectionSuspended(int i) {}
    @Override public void onConnectionFailed(ConnectionResult connectionResult) {
        // if connection to fusedProvider fails, take Android's LocationManager as fallback
        // for possible connectionResults look at
        // https://developer.android.com/reference/com/google/android/gms/common/ConnectionResult.html
        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 0, this);
    }

    // the next three functions belong to Android's Location Provider
    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
    @Override public void onProviderEnabled(String provider) {}
    @Override public void onProviderDisabled(String provider) {}

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
                    && currentBestLocation.hasBearing()
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
