package org.walkersguide.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.R;
import org.walkersguide.exceptions.PointListParsingException;
import org.walkersguide.routeobjects.Point;

import android.content.Context;
import android.widget.Toast;

public class POIManager {

    public interface POIListener {
        public void poiPresetUpdateSuccessful();
        public void poiPresetUpdateFailed(String error);
    }

    private POIListener poiListener;
    private Globals globalData;
    private SettingsManager settingsManager;
    private Context mContext;
    private Toast messageToast;
    private boolean downloadInProcess;
    private POIPreset preset;
    private DataDownloader poiDownloader;

    public POIManager(Context context) {
        this.mContext = context;
        this.globalData = ((Globals) mContext);
        this.settingsManager = globalData.getSettingsManagerInstance();
        this.messageToast = Toast.makeText(mContext, "", Toast.LENGTH_SHORT);
        this.downloadInProcess = false;
        this.preset = null;
    }

    public void setPOIListener(POIListener poiListener) {
        this.poiListener = poiListener;
    }

    public void cancel() {
        downloadInProcess = false;
        if (poiDownloader != null) {
            poiDownloader.cancelDownloadProcess();
            preset.setLastLocation(null);
        }
    }

    public void updatePOIList(int newPresetId, int newPresetRange, Point newLocation, int newCompassValue,
            String newSearchString, boolean isInsidePublicTransport) {
        // get the current preset by means of the given preset id
        preset = settingsManager.getPOIPreset(newPresetId);

        if (preset == null || newLocation == null || downloadInProcess == true) {
            return;
        }

        // decide, if the program should download new poi data from server
        boolean downloadNewData = false;
        if (preset.getLastLocation() == null || preset.getLastLocationSinceDownload() == null) {
            downloadNewData = true;
            preset.setPOIListStatus(POIPreset.UpdateStatus.RESETLISTPOSITION);
            messageToast.setText("location null");
            System.out.println("xx download: location null");
        } else if (isInsidePublicTransport) {
            downloadNewData = false;
            System.out.println("xx download: inside public transport");
        } else if (newLocation.distanceTo(preset.getLastLocationSinceDownload()) > (preset.getRange()*1/2)) {
            preset.setPOIListStatus(POIPreset.UpdateStatus.RESETLISTPOSITION);
            downloadNewData = true;
            messageToast.setText("entfernung größer");
            System.out.println("xx download: entfernung größer");
        } else if (! newSearchString.equals(preset.getLastSearchString())) {
            preset.setPOIListStatus(POIPreset.UpdateStatus.RESETLISTPOSITION);
            messageToast.setText("neuer suchstring");
            downloadNewData = true;
            System.out.println("xx download: neuer suchstring");
        } else if(newPresetRange > preset.getRange()) {
            preset.setPOIListStatus(POIPreset.UpdateStatus.HOLDLISTPOSITION);
            messageToast.setText("bigger radius");
            downloadNewData = true;
            System.out.println("xx download: radius größer");
        }

        if (downloadNewData == true) {
            // messageToast.show();
            preset.setLastLocation(newLocation);
            preset.setLastLocationSinceDownload(newLocation);
            preset.setLastCompassValue(newCompassValue);
            preset.setRange(newPresetRange);
            preset.setLastSearchString(newSearchString);
            preset.setDownloadedNewData(true);
            downloadInProcess = true;
            JSONObject requestJson = new JSONObject();
            try {
                requestJson.put("lat", preset.getLastLocation().getLatitude());
                requestJson.put("lon", preset.getLastLocation().getLongitude());
                requestJson.put("radius", preset.getRange());
                requestJson.put("tags", preset.getTags());
                requestJson.put("language", Locale.getDefault().getLanguage());
                requestJson.put("session_id", globalData.getSessionId());
                if (!preset.getLastSearchString().equals("")) {
                    requestJson.put("search", preset.getLastSearchString());
                }
            } catch (JSONException e) {
                return;
            }
            poiDownloader = new DataDownloader(mContext);
            poiDownloader.setDataDownloadListener(new POIDownloadListener() );
            poiDownloader.execute(
                    globalData.getSettingsManagerInstance().getServerPath() + "/get_poi",
                    requestJson.toString() );
            return;
        }

        // but maybe the poi list nevertheless must be sorted
        boolean sort = false;
        int compassDifference = Math.abs(newCompassValue - preset.getLastCompassValue());
        if (compassDifference > 180)
            compassDifference = 360 - compassDifference;
        if (compassDifference >= 20) {
            messageToast.setText("compass changed");
            sort = true;
            System.out.println("xx sort: kompass");
        } else if (newLocation.distanceTo(preset.getLastLocation()) > 10) {
            messageToast.setText("mehr als 10 meter entfernt");
            sort = true;
            System.out.println("xx sort: mehr als 10 meter");
        } else if(newPresetRange < preset.getRange()) {
            messageToast.setText("smaller radius");
            sort = true;
            System.out.println("xx sort: radius kleiner");
        }

        if (sort) {
            // messageToast.show();
            preset.setLastLocation(newLocation);
            preset.setLastCompassValue(newCompassValue);
            preset.setRange(newPresetRange);
            preset.setPOIListStatus(POIPreset.UpdateStatus.HOLDLISTPOSITION);
            sortPOIList(preset.getPOIList());
            return;
        }
        
        if (poiListener != null) {
            poiListener.poiPresetUpdateSuccessful();
        }
    }

    public void sortPOIList(ArrayList<Point> poiList) {
        ArrayList<Point> sortedPOIList = new ArrayList<Point>();
        for(Point poi : poiList) {
            poi.addDistance( preset.getLastLocation().distanceTo(poi) );
            poi.addBearing( preset.getLastLocation().bearingTo(poi) - preset.getLastCompassValue() );
            if (poi.getDistance() < preset.getRange()) {
                sortedPOIList.add( poi );
            } else if (!preset.getLastSearchString().equals("")) {
                sortedPOIList.add( poi );
            }
        }
        // favorites
        if (preset.getTags().contains("favorites")) {
            String searchPattern = "(.*)" + preset.getLastSearchString().toLowerCase().replace(" ", "(.*)") + "(.*)";
            for(Point poi : settingsManager.loadPointsFromFavorites()) {
                if (sortedPOIList.contains(poi))
                    continue;
                poi.addDistance( preset.getLastLocation().distanceTo(poi) );
                poi.addBearing( preset.getLastLocation().bearingTo(poi) - preset.getLastCompassValue() );
                if (!preset.getLastSearchString().equals("")) {
                    if (poi.getName().toLowerCase().matches(searchPattern)) {
                        sortedPOIList.add( poi );
                    }
                } else if (poi.getDistance() < preset.getRange()) {
                    sortedPOIList.add( poi );
                }
            }
        }
        Collections.sort(sortedPOIList);
        preset.setPOIList(sortedPOIList);
        settingsManager.updatePOIPreset(preset);
        if (poiListener != null) {
            poiListener.poiPresetUpdateSuccessful();
        }
    }

    private class POIDownloadListener implements DataDownloader.DataDownloadListener {
        @Override public void dataDownloadedSuccessfully(JSONObject jsonObject) {
            poiDownloader = null;
            downloadInProcess = false;
            try {
                sortPOIList( ObjectParser.parsePointList(jsonObject) );
            } catch (PointListParsingException e) {
                preset.setLastLocation(null);
                if (poiListener != null) {
                    poiListener.poiPresetUpdateFailed(e.getMessage());
                }
            }
        }

        @Override public void dataDownloadFailed(String error) {
            poiDownloader = null;
            downloadInProcess = false;
            preset.setLastLocation(null);
            if (poiListener != null) {
                poiListener.poiPresetUpdateFailed( String.format(
                        mContext.getResources().getString(R.string.messageNetworkError), error));
            }
        }

        @Override public void dataDownloadCanceled() {
            poiDownloader = null;
            JSONObject requestJson = new JSONObject();
            try {
                requestJson.put("language", Locale.getDefault().getLanguage());
                requestJson.put("session_id", globalData.getSessionId());
            } catch (JSONException e) {
                return;
            }
            DataDownloader cancelDownloader = new DataDownloader(mContext);
            cancelDownloader.setDataDownloadListener(new CanceledRequestDownloadListener() );
            cancelDownloader.execute(
                    globalData.getSettingsManagerInstance().getServerPath() + "/cancel_request",
                    requestJson.toString() );
        }
    }

    private class CanceledRequestDownloadListener implements DataDownloader.DataDownloadListener {
        @Override public void dataDownloadedSuccessfully(JSONObject jsonObject) {}
        @Override public void dataDownloadFailed(String error) {}
        @Override public void dataDownloadCanceled() {}
    }
}
