package org.walkersguide.userinterface;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.R;
import org.walkersguide.routeobjects.POIPoint;
import org.walkersguide.routeobjects.Point;
import org.walkersguide.routeobjects.RouteObjectWrapper;
import org.walkersguide.sensors.PositionManager;
import org.walkersguide.utils.AddressManager;
import org.walkersguide.utils.DataDownloader;
import org.walkersguide.utils.Globals;
import org.walkersguide.utils.Route;
import org.walkersguide.utils.SettingsManager;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

public class EnterAddressActivity extends AbstractActivity {

    private Globals globalData;
    private PositionManager positionManager;
    private SettingsManager settingsManager;
    private AddressManager addressManager;
    private InputMethodManager inputManager;
    private LinearLayout mainLayout;
    private Dialog dialog;
    private int routeRequestPosition;
    private String addressString;
    private ArrayList<POIPoint> addressPointList;
    private Toast messageToast;

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
        mainLayout = (LinearLayout) findViewById(R.id.linearLayoutMain);
        messageToast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);

        Button buttonDeleteStreet = (Button) mainLayout.findViewById(R.id.buttonDeleteStreet);
        buttonDeleteStreet.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                EditText editStreet = (EditText) mainLayout.findViewById(R.id.editStreet);
                editStreet.setText("");
                editStreet.postDelayed(new Runnable() {
                    public void run() {
                        EditText editStreet = (EditText) mainLayout.findViewById(R.id.editStreet);
                        editStreet.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN , 0, 0, 0));
                        editStreet.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP , 0, 0, 0));
                    }
                }, 200);
            }
        });

        EditText editCity = (EditText) mainLayout.findViewById(R.id.editCity);
        editCity.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (event != null) {
                    queryCoordinates();
                    EditText editCity = (EditText) mainLayout.findViewById(R.id.editCity);
                    inputManager.hideSoftInputFromWindow(
                            editCity.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                }
                return false;
            }
        });

        Button buttonDeleteCity = (Button) mainLayout.findViewById(R.id.buttonDeleteCity);
        buttonDeleteCity.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                EditText editCity = (EditText) mainLayout.findViewById(R.id.editCity);
                editCity.setText("");
                editCity.postDelayed(new Runnable() {
                    public void run() {
                        EditText editCity = (EditText) mainLayout.findViewById(R.id.editCity);
                        editCity.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN , 0, 0, 0));
                        editCity.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP , 0, 0, 0));
                    }
                }, 200);
            }
        });

        Button buttonOK = (Button) mainLayout.findViewById(R.id.buttonOK);
        buttonOK.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                queryCoordinates();
                EditText editCity = (EditText) mainLayout.findViewById(R.id.editCity);
                inputManager.hideSoftInputFromWindow(
                        editCity.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        });

        Button buttonCancel = (Button) mainLayout.findViewById(R.id.buttonCancel);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                setResult(RESULT_CANCELED, null);
                finish();
            }
        });

        Intent sender = getIntent();
        routeRequestPosition = sender.getExtras().getInt("objectIndex", -1);
	}

    private void showChooseAddressFromListDialog() {
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_scrollable_empty);
        dialog.setCanceledOnTouchOutside(false);
        LinearLayout dialogLayout = (LinearLayout) dialog.findViewById(R.id.linearLayoutMain);
        LayoutParams lp = new LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT );
        // heading
        TextView label = new TextView(this);
        label.setLayoutParams(lp);
        label.setText(getResources().getString(R.string.labelChooseAddressFromListDialogHeading));
        dialogLayout.addView(label);

        int index = 0;
        for (POIPoint address : addressPointList) {
            Button buttonAddress = new Button(this);
            buttonAddress.setLayoutParams(lp);
            buttonAddress.setText(address.getName());
            buttonAddress.setId(index);
            buttonAddress.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    POIPoint address = addressPointList.get(view.getId());
                    if (routeRequestPosition == -1) {
                        positionManager.changeStatus(PositionManager.Status.SIMULATION, address);
                        settingsManager.addPointToHistory(address);
                    } else {
                        Route sourceRoute = settingsManager.getRouteRequest();
                        sourceRoute.replaceRouteObjectAtIndex(routeRequestPosition,
                                new RouteObjectWrapper(address) );
                        settingsManager.setRouteRequest(sourceRoute);
                    }
                    finish();
                }
            });
            dialogLayout.addView(buttonAddress);
            index += 1;
        }

        Button buttonCancel = new Button(this);
        buttonCancel.setLayoutParams(lp);
        buttonCancel.setText(getResources().getString(R.string.dialogCancel));
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialogLayout.addView(buttonCancel);
        dialog.show();
    }

    private void queryCoordinates() {
        EditText editCity = (EditText) mainLayout.findViewById(R.id.editCity);
        EditText editStreet = (EditText) mainLayout.findViewById(R.id.editStreet);
        addressString = editStreet.getText().toString();
        if (! addressString.toLowerCase().contains(editCity.getText().toString().toLowerCase())) {
            addressString = editCity.getText().toString() + " " + addressString;
        }
        try {
            String url = "https://maps.googleapis.com/maps/api/geocode/json?"
                + "address=" + URLEncoder.encode(addressString, "UTF-8")
                + "&sensor=false&language=" + Locale.getDefault().getLanguage();
            DataDownloader downloader = new DataDownloader(EnterAddressActivity.this);
            downloader.setDataDownloadListener(new DLListener() );
            downloader.execute(url);
        } catch(UnsupportedEncodingException e) {
            messageToast.setText(getResources().getString(R.string.messageEncodingError));
            messageToast.show();
        }
    }

    @Override public void onResume() {
        super.onResume();
        addressManager.setAddressListener(new MyAddressListener());
        positionManager.setPositionListener(new MyPositionListener());
        addressPointList = new ArrayList<POIPoint>();
    }

    private class MyPositionListener implements PositionManager.PositionListener {
        public void locationChanged(Location location) {
            addressManager.updateAddress(new Point("", location));
            positionManager.setPositionListener(null);
        }
    }

    private class MyAddressListener implements AddressManager.AddressListener {
        public void addressUpdateSuccessful(String address) {}
        public void addressUpdateFailed(String error) {}
        public void cityUpdateSuccessful(String city) {
            EditText editCity = (EditText) mainLayout.findViewById(R.id.editCity);
            editCity.setText(city);
        }
    }

    private class DLListener implements DataDownloader.DataDownloadListener {
        @Override public void dataDownloadedSuccessfully(JSONObject jsonObject) {
            CharSequence  text = "";
            try {
                if (jsonObject == null) {
                    text = getResources().getString(R.string.messageUnknownError);
                } else if (!jsonObject.getString("status").equals("OK")) {
                    text = String.format(getResources().getString(R.string.messageErrorFromServer),
                            jsonObject.getString("status") );;
                } else {
                    addressPointList = new ArrayList<POIPoint>();
                    JSONArray results = jsonObject.getJSONArray("results");
                    for (int i=0; i<results.length(); i++) {
                        JSONObject addressObject = results.getJSONObject(i);
                        JSONObject addressObjectLocation = addressObject.getJSONObject("geometry").getJSONObject("location");
                        POIPoint address = new POIPoint(
                                addressObject.getString("formatted_address"),
                                addressObjectLocation.getDouble("lat"),
                                addressObjectLocation.getDouble("lng"),
                                getResources().getString(R.string.locationNameAddress) );
                        address.addAddress(address.getName());
                        addressPointList.add(address);
                    }
                    if (addressPointList.size() == 0) {
                        messageToast.setText(getResources().getString(R.string.messageFoundNoAddress));
                        messageToast.show();
                    } else if (addressPointList.size() == 1) {
                        if (routeRequestPosition == -1) {
                            positionManager.changeStatus(PositionManager.Status.SIMULATION, addressPointList.get(0));
                            settingsManager.addPointToHistory(addressPointList.get(0));
                        } else {
                            Route sourceRoute = settingsManager.getRouteRequest();
                            sourceRoute.replaceRouteObjectAtIndex(routeRequestPosition,
                                    new RouteObjectWrapper(addressPointList.get(0)) );
                            settingsManager.setRouteRequest(sourceRoute);
                        }
                        finish();
                    } else {
                        showChooseAddressFromListDialog();
                    }
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
