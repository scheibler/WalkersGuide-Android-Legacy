package org.walkersguide.userinterface;

import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.R;
import org.walkersguide.routeobjects.POIPoint;
import org.walkersguide.routeobjects.Point;
import org.walkersguide.routeobjects.RouteObjectWrapper;
import org.walkersguide.utils.AddressManager;
import org.walkersguide.utils.DataDownloader;
import org.walkersguide.utils.Globals;
import org.walkersguide.utils.PositionManager;
import org.walkersguide.utils.SettingsManager;
import org.walkersguide.utils.SourceRoute;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class EnterAddressActivity extends Activity {

    private Globals globalData;
    private PositionManager positionManager;
    private SettingsManager settingsManager;
    private AddressManager addressManager;
    private InputMethodManager inputManager;
    private EditText editStreet, editCity;
    private String addressString;
    private int routeRequestPosition;

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_enter_address);
        if (globalData == null) {
            globalData = ((Globals) getApplicationContext());
        }
        settingsManager = globalData.getSettingsManagerInstance();
        inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        addressManager = globalData.getAddressManagerInstance();
        positionManager = globalData.getPositionManagerInstance();

        Button buttonDeleteStreet = (Button) findViewById(R.id.buttonDeleteStreet);
        buttonDeleteStreet.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                editStreet.setText("");
                editStreet.postDelayed(new Runnable() {
                    public void run() {
                        editStreet.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN , 0, 0, 0));
                        editStreet.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP , 0, 0, 0));
                    }
                }, 200);
            }
        });

        editCity = (EditText) findViewById(R.id.editCity);
        editCity.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (event != null) {
                    queryCoordinates();
                    inputManager.hideSoftInputFromWindow(
                            editCity.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                }
                return false;
            }
        });

        Button buttonDeleteCity = (Button) findViewById(R.id.buttonDeleteCity);
        buttonDeleteCity.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                editCity.setText("");
                editCity.postDelayed(new Runnable() {
                    public void run() {
                        editCity.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN , 0, 0, 0));
                        editCity.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP , 0, 0, 0));
                    }
                }, 200);
            }
        });

        Button buttonOK = (Button) findViewById(R.id.buttonOK);
        buttonOK.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                queryCoordinates();
                inputManager.hideSoftInputFromWindow(
                        editCity.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        });

        Button buttonCancel = (Button) findViewById(R.id.buttonCancel);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                setResult(RESULT_CANCELED, null);
                finish();
            }
        });

        Intent sender = getIntent();
        routeRequestPosition = sender.getExtras().getInt("objectIndex", -1);
	}

    private void queryCoordinates() {
        addressString = editStreet.getText().toString();
        if (! addressString.toLowerCase().contains(editCity.getText().toString().toLowerCase())) {
            addressString = editCity.getText().toString() + " " + addressString;
        }
        System.out.print("xx address query = " + addressString);
        DataDownloader downloader = new DataDownloader(EnterAddressActivity.this);
        downloader.setDataDownloadListener(new DLListener() );
        downloader.execute( "get",
                globalData.getSettingsManagerInstance().getServerPath(),
                "/get_coordinates?address=" + addressString
                + "&language=" + Locale.getDefault().getLanguage() );
    }

    @Override public void onResume() {
        super.onResume();
        editStreet = (EditText) findViewById(R.id.editStreet);
        editCity = (EditText) findViewById(R.id.editCity);
        addressManager.setAddressListener(new MyAddressListener());
        positionManager.setPositionListener(new MyPositionListener());
        positionManager.getLastKnownLocation();
    }

    private class MyPositionListener implements PositionManager.PositionListener {
        public void locationChanged(Location location) {
            addressManager.updateAddress(new Point("", location));
        }
        public void displayGPSSettingsDialog() {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }
    }

    private class MyAddressListener implements AddressManager.AddressListener {
        public void addressUpdateSuccessful(String address) {}
        public void addressUpdateFailed(String error) {}
        public void cityUpdateSuccessful(String city) {
            editCity.setText(city);
        }
    }

    private class DLListener implements DataDownloader.DataDownloadListener {
        @Override public void dataDownloadedSuccessfully(JSONObject jsonObject) {
            CharSequence  text = "";
            try {
                if (jsonObject == null) {
                    text = getResources().getString(R.string.messageUnknownError);
                } else if (! jsonObject.getString("error").equals("")) {
                    text = String.format(getResources().getString(R.string.messageErrorFromServer),
                            jsonObject.getString("error") );;
                } else {
                    JSONObject coordinates = jsonObject.getJSONObject("coordinates");
                    POIPoint address = new POIPoint( addressString,
                            coordinates.getDouble("lat"), coordinates.getDouble("lon"),
                            getResources().getString(R.string.locationNameAddress) );
                    address.addAddress(addressString);
                    if (routeRequestPosition == -1) {
                        positionManager.changeStatus(PositionManager.Status.SIMULATION, address);
                        settingsManager.addPointToHistory(address);
                    } else {
                        SourceRoute sourceRoute = settingsManager.getRouteRequest();
                        sourceRoute.replaceRouteObjectAtIndex(routeRequestPosition,
                                new RouteObjectWrapper(address) );
                        settingsManager.setRouteRequest(sourceRoute);
                    }
                    finish();
                    return;
                }
            } catch (JSONException e) {
                text = String.format(getResources().getString(R.string.messageJSONError),
                        e.getMessage() );
            }
            Intent intent = new Intent(getApplicationContext(), DialogActivity.class);
            intent.putExtra("message", text);
            startActivity(intent);
        }

        @Override public void dataDownloadFailed(String error) {
            Intent intent = new Intent(getApplicationContext(), DialogActivity.class);
            intent.putExtra("message", String.format(
                        getResources().getString(R.string.messageNetworkError), error) );
            startActivity(intent);
        }

        @Override public void dataDownloadCanceled() {}
    }

}
