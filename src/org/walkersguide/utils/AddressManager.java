package org.walkersguide.utils;

import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.R;
import org.walkersguide.routeobjects.Point;

import android.content.Context;

public class AddressManager {
    public interface AddressListener {
        public void cityUpdateSuccessful(String city);
        public void addressUpdateSuccessful(String address);
        public void addressUpdateFailed(String error);
    }

    private Context mContext;
    private AddressListener addressListener;
    private Point currentLocation;
    private String currentAddress, currentCity;
    private boolean downloadInProcess;

    public AddressManager(Context mContext) {
        this.mContext = mContext;
        this.downloadInProcess = false;
    }

    public void setAddressListener(AddressListener addressListener) {
        this.addressListener = addressListener;
    }

    public void updateAddress(Point location) {
        if (location == null || downloadInProcess) {
            return;
        }
        if (currentLocation == null || currentLocation.distanceTo(location) > 30) {
            currentLocation = location;
            currentAddress = "";
            String url = "https://maps.googleapis.com/maps/api/geocode/json?"
                + "latlng=" + String.valueOf(location.getLatitude())
                + "," + String.valueOf(location.getLongitude())
                + "&sensor=false&language=" + Locale.getDefault().getLanguage();
            DataDownloader downloader = new DataDownloader(mContext);
            downloader.setDataDownloadListener(new AddressDownloadListener() );
            downloader.execute(url);
        } else if (!currentAddress.equals("")) {
            addressListener.cityUpdateSuccessful(currentCity);
            addressListener.addressUpdateSuccessful(currentAddress);
        }
    }

    private class AddressDownloadListener implements DataDownloader.DataDownloadListener {
        @Override public void dataDownloadedSuccessfully(JSONObject jsonObject) {
            downloadInProcess = false;
            if (jsonObject == null) {
                currentLocation = null;
                addressListener.addressUpdateFailed(mContext.getResources().getString(R.string.messageUnknownError));
                return;
            }
            try {
                if (!jsonObject.getString("status").equals("OK")) {
                    currentLocation = null;
                    addressListener.addressUpdateFailed( String.format(
                            mContext.getResources().getString(R.string.messageAddressDownloadError),
                            jsonObject.getString("status") ));
                    return;
                }
                JSONObject result = jsonObject.getJSONArray("results").getJSONObject(0);
                // city
                String city1 = "";
                String city2 = "";
                for (int i=0; i<result.getJSONArray("address_components").length(); i++) {
                    JSONObject addrComponent = result.getJSONArray("address_components").getJSONObject(i);
                    if (addrComponent.getJSONArray("types").toString().contains("administrative_area_level_2")) {
                        city1 = addrComponent.getString("long_name");
                    }
                    if (addrComponent.getJSONArray("types").toString().contains("\"locality\"")) {
                        city2 = addrComponent.getString("long_name");
                    }
                }
                if ( (city1.equals("") && city2.equals(""))
                        || (! city1.equals("") && city2.equals("")) ) {
                    currentCity = city1;
                } else {
                    currentCity = city2;
                }
                addressListener.cityUpdateSuccessful(currentCity);
                // location and address
                JSONObject location = result.getJSONObject("geometry").getJSONObject("location");
                Point addressPoint = new Point("Address", location.getDouble("lat"),
                        location.getDouble("lng"));
                if (currentLocation.distanceTo(addressPoint) < 50) {
                    currentAddress = result.getString("formatted_address");
                    addressListener.addressUpdateSuccessful(currentAddress);
                } else {
                    addressListener.addressUpdateFailed(mContext.getResources().getString(R.string.messageNoAddressForThisCoordinates));
                }
            } catch (JSONException e) {
                currentLocation = null;
                addressListener.addressUpdateFailed( String.format(
                        mContext.getResources().getString(R.string.messageJSONError), e.getMessage()));
            }
        }

        @Override public void dataDownloadFailed(String error) {
            downloadInProcess = false;
            currentLocation = null;
            addressListener.addressUpdateFailed( String.format(
                    mContext.getResources().getString(R.string.messageNetworkError), error));
        }

        @Override public void dataDownloadCanceled() {
            downloadInProcess = false;
            currentLocation = null;
        }
    }

}
