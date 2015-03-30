package org.walkersguide.userinterface;

import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.R;
import org.walkersguide.routeobjects.POIPoint;
import org.walkersguide.routeobjects.Point;
import org.walkersguide.sensors.PositionManager;
import org.walkersguide.utils.DataDownloader;
import org.walkersguide.utils.Globals;
import org.walkersguide.utils.SettingsManager;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class AddFavoriteActivity extends AbstractActivity {

    private Globals globalData;
    private SettingsManager settingsManager;
    private PositionManager positionManager;
    private LinearLayout mainLayout;
    private Point currentPosition;
    private Toast messageToast;
    private boolean storeOnServer = false;

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_favorite);
        mainLayout = (LinearLayout) findViewById(R.id.linearLayoutMain);
        messageToast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);

        if (globalData == null) {
            globalData = ((Globals) getApplicationContext());
        }
        settingsManager = globalData.getSettingsManagerInstance();
        positionManager = globalData.getPositionManagerInstance();

        EditText editName = (EditText) mainLayout.findViewById(R.id.editName);
        editName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    EditText editName = (EditText) mainLayout.findViewById(R.id.editName);
                    if (editName.getText().toString().equals("")) {
                    messageToast.setText(getResources().getString(R.string.messageNoFavoriteName));
                            messageToast.show();
                        return false;
                    }
                    POIPoint favorite = new POIPoint(editName.getText().toString(),
                            currentPosition.getLatitude(), currentPosition.getLongitude(), "favorite");
                    settingsManager.addPointToFavorites(favorite);
                    messageToast.setText(getResources().getString(R.string.messageStoredFavoriteLocally));
                    messageToast.show();
                    setResult(RESULT_OK, null);
                    finish();
                }
                return false;
            }
        });

        Button buttonDelete = (Button) mainLayout.findViewById(R.id.buttonDelete);
        buttonDelete.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                EditText editName = (EditText) mainLayout.findViewById(R.id.editName);
                editName.setText("");
                editName.postDelayed(new Runnable() {
                    public void run() {
                        EditText editName = (EditText) mainLayout.findViewById(R.id.editName);
                        editName.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN , 0, 0, 0));
                        editName.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP , 0, 0, 0));
                    }
                }, 200);
            }
        });

        Button buttonOK = (Button) findViewById(R.id.buttonOK);
        buttonOK.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                EditText editName = (EditText) mainLayout.findViewById(R.id.editName);
                if (editName.getText().toString().equals("")) {
                    messageToast.setText(getResources().getString(R.string.messageNoFavoriteName));
                    messageToast.show();
                    return;
                }
                POIPoint favorite = new POIPoint(editName.getText().toString(),
                        currentPosition.getLatitude(), currentPosition.getLongitude(), "favorite");
                if (storeOnServer) {
                    // store on server
                    DataDownloader downloader = new DataDownloader(AddFavoriteActivity.this);
                    downloader.setDataDownloadListener(new DLListener() );
                    downloader.execute( "get",
                            settingsManager.getServerPath(), "/add_marker?"
                            + "lat=" + favorite.getLatitude()
                            + "&lon=" + favorite.getLongitude()
                            + "&name=" + favorite.getName() );
                } else {
                    // store favorite localy
                    settingsManager.addPointToFavorites(favorite);
                    messageToast.setText(getResources().getString(R.string.messageStoredFavoriteLocally));
                    messageToast.show();
                    setResult(RESULT_OK, null);
                    finish();
                }
            }
        });

        Button buttonCancel = (Button) findViewById(R.id.buttonCancel);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                setResult(RESULT_CANCELED, null);
                finish();
            }
        });
	}

    public synchronized void updateUserInterface() {
        TextView labelLatitude = (TextView) mainLayout.findViewById(R.id.labelLatitude);
        labelLatitude.setText( String.format("%1$s %2$f",
                getResources().getString(R.string.labelLatitude), currentPosition.getLatitude() ));
        TextView labelLongitude = (TextView) mainLayout.findViewById(R.id.labelLongitude);
        labelLongitude.setText( String.format("%1$s %2$f",
                getResources().getString(R.string.labelLongitude), currentPosition.getLongitude() ));
        TextView labelAccuracy = (TextView) mainLayout.findViewById(R.id.labelAccuracy);
        labelAccuracy.setText( String.format(
                getResources().getString(R.string.labelRoundedAccuracyFormated),
                currentPosition.getAccuracy() ));
    }

    @Override public void onPause() {
        super.onPause();
        positionManager.setPositionListener(null);
    }

    @Override public void onResume() {
        super.onResume();
        positionManager.setPositionListener(new MyPositionListener());
    }

    private class MyPositionListener implements PositionManager.PositionListener {
        public void locationChanged(Location location) {
            if (location == null)
                return;
            currentPosition = new Point(
                    getResources().getString(R.string.locationNameCurrentPosition), location);
            updateUserInterface();
        }
        public void displayGPSSettingsDialog() {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }
    }


    private class DLListener implements DataDownloader.DataDownloadListener {
        @Override public void dataDownloadedSuccessfully(JSONObject jsonObject) {
            CharSequence  text = "";
            try {
                if (jsonObject == null)
                    text = getResources().getString(R.string.messageUnknownError);
                if (! jsonObject.getString("error").equals(""))
                    text = String.format(getResources().getString(R.string.messageErrorFromServer),
                            jsonObject.getString("error") );;
                if (jsonObject.getString("status").equals("ok")) {
                    messageToast.setText(getResources().getString(R.string.messageStoredFavoriteRemotely));
                    messageToast.show();
                    setResult(RESULT_OK, null);
                    finish();
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
        }

        @Override public void dataDownloadCanceled() {}
    }

}
