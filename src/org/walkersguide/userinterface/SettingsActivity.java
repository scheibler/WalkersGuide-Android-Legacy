package org.walkersguide.userinterface;

import java.util.HashMap;

import org.walkersguide.R;
import org.walkersguide.utils.Globals;
import org.walkersguide.utils.SettingsManager;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

public class SettingsActivity extends  AbstractActivity {

    private static final int SETTINGSIMPORTED = 1;
    private Globals globalData;
    private SettingsManager settingsManager;
    private Vibrator vibrator;
    private RelativeLayout mainLayout;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (globalData == null) {
            globalData = ((Globals) getApplicationContext());
        }
        settingsManager = globalData.getSettingsManagerInstance();
        vibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        setContentView(R.layout.activity_settings);
        mainLayout = (RelativeLayout) findViewById(R.id.linearLayoutMain);

        Button buttonStayActive = (Button) mainLayout.findViewById(R.id.buttonStayActive);
        if (settingsManager.getStayActiveInBackground()) {
            buttonStayActive.setTag(1);
        } else {
            buttonStayActive.setTag(0);
        }
        buttonStayActive.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Button buttonStayActive = (Button) mainLayout.findViewById(R.id.buttonStayActive);
                if ((Integer) buttonStayActive.getTag() == 0) {
                    buttonStayActive.setTag(1);
                } else {
                    buttonStayActive.setTag(0);
                }
                updateUserInterface();
            }
        });

        Button buttonShakeNextPoint = (Button) mainLayout.findViewById(R.id.buttonShakeNextPoint);
        if (settingsManager.getShakeForNextRoutePoint()) {
            buttonShakeNextPoint.setTag(1);
        } else {
            buttonShakeNextPoint.setTag(0);
        }
        buttonShakeNextPoint.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Button buttonShakeNextPoint = (Button) mainLayout.findViewById(R.id.buttonShakeNextPoint);
                if ((Integer) buttonShakeNextPoint.getTag() == 0) {
                    buttonShakeNextPoint.setTag(1);
                } else {
                    buttonShakeNextPoint.setTag(0);
                }
                updateUserInterface();
            }
        });

        Spinner spinnerShakeIntensity = (Spinner) mainLayout.findViewById(R.id.spinnerShakeIntensity);
        ArrayAdapter<CharSequence> adapterShakeIntensity = ArrayAdapter.createFromResource(this,
                R.array.arrayShakeIntensity, android.R.layout.simple_spinner_item);
        adapterShakeIntensity.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerShakeIntensity.setAdapter(adapterShakeIntensity);
        spinnerShakeIntensity.setSelection(settingsManager.getShakeIntensity());

        // server part
        // url
        EditText editServerURL = (EditText) mainLayout.findViewById(R.id.editServerURL);
        editServerURL.setText(settingsManager.getHostURL());
        Button buttonDeleteServerURL = (Button) mainLayout.findViewById(R.id.buttonDeleteServerURL);
        buttonDeleteServerURL.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                EditText editServerURL = (EditText) mainLayout.findViewById(R.id.editServerURL);
                editServerURL.setText("");
                editServerURL.postDelayed(new Runnable() {
                    public void run() {
                        EditText editServerURL = (EditText) mainLayout.findViewById(R.id.editServerURL);
                        editServerURL.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN , 0, 0, 0));
                        editServerURL.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP , 0, 0, 0));
                    }
                }, 200);
            }
        });

        // port
        EditText editServerPort = (EditText) mainLayout.findViewById(R.id.editServerPort);
        editServerPort.setText( String.valueOf(settingsManager.getHostPort()) );
        Button buttonDeleteServerPort = (Button) mainLayout.findViewById(R.id.buttonDeleteServerPort);
        buttonDeleteServerPort.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                EditText editServerPort = (EditText) mainLayout.findViewById(R.id.editServerPort);
                editServerPort.setText("");
                editServerPort.postDelayed(new Runnable() {
                    public void run() {
                        EditText editServerPort = (EditText) mainLayout.findViewById(R.id.editServerPort);
                        editServerPort.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN , 0, 0, 0));
                        editServerPort.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP , 0, 0, 0));
                    }
                }, 200);
            }
        });

        // cancel
        Button buttonCancel = (Button) mainLayout.findViewById(R.id.buttonCancel);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                setResult(RESULT_CANCELED, null);
                finish();
                return;
            }
        });

        // ok
        Button buttonOK = (Button) mainLayout.findViewById(R.id.buttonOK);
        buttonOK.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (! storeApplicationSettings()) {
                    return;
                }
                setResult(RESULT_OK, null);
                finish();
            }
        });

        Button buttonStoreToDisk = (Button) mainLayout.findViewById(R.id.buttonStoreToDisk);
        buttonStoreToDisk.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (! storeApplicationSettings()) {
                    return;
                }
                HashMap<String,Integer> results = settingsManager.storeAllSettingsToDisk();
                String output = "";
                boolean exportSuccessful = true;
                for (String key : results.keySet()) {
                    switch (results.get(key)) {
                        case 0:
                            output += String.format(
                                getResources().getString(R.string.messageImExOK), key);
                            break;
                        case 1:
                            output += String.format(
                                getResources().getString(R.string.messageImExWriteError), key);
                            exportSuccessful = false;
                            break;
                        default:
                            output += String.format(
                                getResources().getString(R.string.messageImExUnknownError), key);
                            exportSuccessful = false;
                    }
                }
                String message;
                if (exportSuccessful) {
                    message = String.format(
                            getResources().getString(R.string.messageExportSuccessful),
                            settingsManager.getProgramSettingsFolder(), output);
                } else {
                    message = String.format(
                            getResources().getString(R.string.messageExportFailed),
                            settingsManager.getProgramSettingsFolder(), output);
                }
                Intent intent = new Intent(getApplicationContext(), DialogActivity.class);
                intent.putExtra("message", message);
                startActivity(intent);
            }
        });

        Button buttonRestoreFromDisk = (Button) mainLayout.findViewById(R.id.buttonRestoreFromDisk);
        buttonRestoreFromDisk.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                HashMap<String,Integer> results = settingsManager.restoreAllSettingsFromDisk();
                String output = "";
                boolean importSuccessful = true;
                for (String key : results.keySet()) {
                    switch (results.get(key)) {
                        case 0:
                            output += String.format(
                                getResources().getString(R.string.messageImExOK), key);
                            break;
                        case 1:
                            output += String.format(
                                getResources().getString(R.string.messageImExFileNotFoundError), key);
                            importSuccessful = false;
                            break;
                        case 2:
                            output += String.format(
                                getResources().getString(R.string.messageImExReadError), key);
                            importSuccessful = false;
                            break;
                        case 3:
                            output += String.format(
                                getResources().getString(R.string.messageImExJSONError), key);
                            importSuccessful = false;
                            break;
                        default:
                            output += String.format(
                                getResources().getString(R.string.messageImExUnknownError), key);
                            importSuccessful = false;
                    }
                }
                String message;
                if (importSuccessful) {
                    message = String.format(
                            getResources().getString(R.string.messageImportSuccessful),
                            settingsManager.getProgramSettingsFolder(), output);
                } else {
                    message = String.format(
                            getResources().getString(R.string.messageImportFailed),
                            settingsManager.getProgramSettingsFolder(), output);
                }
                Intent intent = new Intent(getApplicationContext(), DialogActivity.class);
                intent.putExtra("message", message);
                startActivityForResult(intent, SETTINGSIMPORTED);
            }
        });

        updateUserInterface();
    }

    public boolean storeApplicationSettings() {
        // server url
        EditText editServerURL = (EditText) mainLayout.findViewById(R.id.editServerURL);
        String url = editServerURL.getText().toString();
        if (url.equals("")) {
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.messageServerURLEmpty),
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        // port
        EditText editServerPort = (EditText) mainLayout.findViewById(R.id.editServerPort);
        if (editServerPort.getText().toString().equals("")) {
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.messageServerPortEmpty),
                    Toast.LENGTH_SHORT).show();
            return false;
        }
        int port = 0;
        try {
            port = Integer.parseInt( editServerPort.getText().toString() );
            if ((port <= 0) || (port > 65535)) {
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.messageServerPortInvalid),
                        Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch(NumberFormatException nfe) {
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.messageServerPortInvalid),
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        // other settings
        Button buttonStayActive = (Button) mainLayout.findViewById(R.id.buttonStayActive);
        if ((Integer) buttonStayActive.getTag() == 0) {
            settingsManager.setStayActiveInBackground(false);
        } else {
            settingsManager.setStayActiveInBackground(true);
        }
        Button buttonShakeNextPoint = (Button) mainLayout.findViewById(R.id.buttonShakeNextPoint);
        if ((Integer) buttonShakeNextPoint.getTag() == 0) {
            settingsManager.setShakeForNextRoutePoint(false);
        } else {
            settingsManager.setShakeForNextRoutePoint(true);
        }
        Spinner spinnerShakeIntensity = (Spinner) mainLayout.findViewById(R.id.spinnerShakeIntensity);
        settingsManager.setShakeIntensity(spinnerShakeIntensity.getSelectedItemPosition());
        settingsManager.setHostURL(url);
        settingsManager.setHostPort(port);
        return true;
    }

    public void updateUserInterface() {
        Button buttonStayActive = (Button) mainLayout.findViewById(R.id.buttonStayActive);
        if ((Integer) buttonStayActive.getTag() == 0) {
            buttonStayActive.setText(getResources().getString(R.string.buttonStayActiveNo));
        } else {
            buttonStayActive.setText(getResources().getString(R.string.buttonStayActiveYes));
        }
        Button buttonShakeNextPoint = (Button) mainLayout.findViewById(R.id.buttonShakeNextPoint);
        if ((Integer) buttonShakeNextPoint.getTag() == 0) {
            buttonShakeNextPoint.setText(getResources().getString(R.string.buttonShakeNextPointNo));
        } else {
            buttonShakeNextPoint.setText(getResources().getString(R.string.buttonShakeNextPointYes));
        }
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data){
        // See which child activity is calling us back.
        switch (requestCode) {
            case SETTINGSIMPORTED:
                globalData.resetSettingsManagerInstance();
                finish();
            default:
                break;
        }
    }

}
