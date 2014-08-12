package org.walkersguide.userinterface;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.R;
import org.walkersguide.utils.DataDownloader;
import org.walkersguide.utils.Globals;
import org.walkersguide.utils.POIPreset;
import org.walkersguide.utils.SettingsManager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class POIPresetsActivity extends Activity {

    private static final int ENTERPRESETNAME = 1879;
    private Globals globalData;
    private SettingsManager settingsManager;
    private RelativeLayout mainLayout;
    private String supportedPOITags;
    private int presetId;
    private HashMap<String,Integer> checkBoxIDs;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkBoxIDs = new HashMap<String,Integer>();
        if (globalData == null) {
            globalData = ((Globals) getApplicationContext());
        }
        settingsManager = globalData.getSettingsManagerInstance();
        Intent sender=getIntent();

        if (supportedPOITags == null) {
            supportedPOITags = "";
            DataDownloader downloader = new DataDownloader(getApplicationContext());
            downloader.setDataDownloadListener(new DLListener() );
            downloader.execute( "get",
                    globalData.getSettingsManagerInstance().getServerPath(),
                    "/get_all_supported_poi_tags" );
        }

        // load layout
        setContentView(R.layout.activity_poi_presets);
        mainLayout = (RelativeLayout) findViewById(R.id.linearLayoutMain);

        // choose preset
        Spinner spinnerPresets = (Spinner) mainLayout.findViewById(R.id.spinnerPresets);
        ArrayAdapter<POIPreset> adapter = new ArrayAdapter<POIPreset>(this,
                android.R.layout.simple_spinner_item, settingsManager.getPOIPresetList() );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPresets.setAdapter(adapter);
        spinnerPresets.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view,
                int pos, long id) {
                presetId = ((POIPreset) parent.getItemAtPosition(pos)).getId();
                updateUserInterface();
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        // load preset with given id
        presetId = sender.getExtras().getInt("presetId");
        if (settingsManager.getPOIPreset(presetId) == null)
            presetId = 1;

        // new button
        Button buttonNewPreset = (Button) mainLayout.findViewById(R.id.buttonNewPreset);
        buttonNewPreset.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent activityEnterPresetName = new Intent(getApplicationContext(), EnterPOIPresetNameActivity.class);
                startActivityForResult(activityEnterPresetName, ENTERPRESETNAME);
            }
        });

        // store changes
        Button buttonStorePreset = (Button) mainLayout.findViewById(R.id.buttonStorePreset);
        buttonStorePreset.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (updatePreset()) {
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.messagePOIPresetChangesSavedSuccessfully),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        // delete button
        Button buttonDeletePreset = (Button) mainLayout.findViewById(R.id.buttonDeletePreset);
        buttonDeletePreset.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                POIPreset preset = settingsManager.getPOIPreset(presetId);
                if (preset == null) {
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.messageNoPOIPreset),
                            Toast.LENGTH_SHORT).show();
                } else {
                    settingsManager.removePOIPreset( settingsManager.getPOIPreset( presetId ));
                    presetId = 1;
                    if (settingsManager.getPOIPresetList().size() > 0)
                        presetId = settingsManager.getPOIPresetList().get(0).getId();
                    Spinner spinnerPresets = (Spinner) mainLayout.findViewById(R.id.spinnerPresets);
                    ArrayAdapter<POIPreset> presetsAdapter = (ArrayAdapter<POIPreset>) spinnerPresets.getAdapter();
                    presetsAdapter.notifyDataSetChanged();
                    updateUserInterface();
                }
            }
        });

        // ok and cancel buttons
        Button buttonOK = (Button) mainLayout.findViewById(R.id.buttonOK);
        buttonOK.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (updatePreset()) {
                    Intent intent = new Intent();
                    intent.putExtra( "presetId", String.valueOf(presetId));
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
        });
        Button buttonCancel = (Button) mainLayout.findViewById(R.id.buttonCancel);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                setResult(RESULT_CANCELED, null);
                finish();
            }
        });
        updateUserInterface();
    }

    public synchronized void updateUserInterface() {
        TextView labelError = (TextView) mainLayout.findViewById(R.id.labelCheckboxError);
        LinearLayout checkBoxLayout = (LinearLayout) mainLayout.findViewById(R.id.linearLayoutCheckboxes);
    	POIPreset preset = settingsManager.getPOIPreset( presetId );
        if (preset == null) {
            checkBoxLayout.removeAllViews();
            labelError.setText(getResources().getString(R.string.messageNoPOIPreset));
            labelError.setVisibility(View.VISIBLE);
            return;
        }

        // spinner
        Spinner spinnerPresets = (Spinner) mainLayout.findViewById(R.id.spinnerPresets);
        ArrayAdapter<POIPreset> presetsAdapter = (ArrayAdapter<POIPreset>) spinnerPresets.getAdapter();
        spinnerPresets.setSelection(presetsAdapter.getPosition(preset));

        // load checkboxes
        int checkBoxStartID = 29482913;
    	LayoutParams lp = new LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT );
        if (supportedPOITags.equals("")) {
            labelError.setText(getResources().getString(R.string.messageNoPOICategories));
            labelError.setVisibility(View.VISIBLE);
        } else {
            labelError.setVisibility(View.GONE);
            for(String tag : supportedPOITags.split("\\+")) {
                if (!checkBoxIDs.containsKey(tag)) {
                    checkBoxIDs.put(tag, checkBoxStartID++);
                }
                CheckBox checkBox = (CheckBox) checkBoxLayout.findViewById( checkBoxIDs.get(tag) );
                if (checkBox == null) {
                    checkBox = new CheckBox(getApplicationContext());
                    checkBox.setId( checkBoxIDs.get(tag) );
                    checkBox.setLayoutParams(lp);
                    if (tag.equals("favorites")) {
                        checkBox.setText(getResources().getString(R.string.labelPOICategoryFavorites));
                    } else if (tag.equals("transport")) {
                        checkBox.setText(getResources().getString(R.string.labelPOICategoryTransport));
                    } else if (tag.equals("food")) {
                        checkBox.setText(getResources().getString(R.string.labelPOICategoryFood));
                    } else if (tag.equals("tourism")) {
                        checkBox.setText(getResources().getString(R.string.labelPOICategoryTourism));
                    } else if (tag.equals("shop")) {
                        checkBox.setText(getResources().getString(R.string.labelPOICategoryShop));
                    } else if (tag.equals("education")) {
                        checkBox.setText(getResources().getString(R.string.labelPOICategoryEducation));
                    } else if (tag.equals("health")) {
                        checkBox.setText(getResources().getString(R.string.labelPOICategoryHealth));
                    } else if (tag.equals("entertainment")) {
                        checkBox.setText(getResources().getString(R.string.labelPOICategoryEntertainment));
                    } else if (tag.equals("finance")) {
                        checkBox.setText(getResources().getString(R.string.labelPOICategoryFinance));
                    } else if (tag.equals("public_service")) {
                        checkBox.setText(getResources().getString(R.string.labelPOICategoryPublicService));
                    } else if (tag.equals("named_intersection")) {
                        checkBox.setText(getResources().getString(R.string.labelPOICategoryNamedIntersection));
                    } else if (tag.equals("other_intersection")) {
                        checkBox.setText(getResources().getString(R.string.labelPOICategoryOtherIntersection));
                    } else if (tag.equals("trash")) {
                        checkBox.setText(getResources().getString(R.string.labelPOICategoryTrash));
                    } else {
                        checkBox.setText(tag);
                    }
                    checkBoxLayout.addView(checkBox);
                }
                // should it be checked
                if (preset.getTags().contains(tag))
                    checkBox.setChecked(true);
                else
                    checkBox.setChecked(false);
            }
        }
    }

    private boolean updatePreset() {
        POIPreset preset = settingsManager.getPOIPreset( presetId );
        if (preset == null) {
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.messageNoPOIPreset),
                    Toast.LENGTH_SHORT).show();
            return false;
        }
        String checkedTagsString = "";
        for (Map.Entry<String, Integer> entry : checkBoxIDs.entrySet()) {
            CheckBox box = (CheckBox) mainLayout.findViewById( entry.getValue() );
            if (box.isChecked()) {
                checkedTagsString += entry.getKey() + "+";
            }
        }
        if (checkedTagsString.length() > 0) {
            checkedTagsString = checkedTagsString.substring(0, checkedTagsString.length()-1 );
        } else {   // no categories
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.messageNoPOICategoryChecked),
                    Toast.LENGTH_SHORT).show();
            return false;
        }
        // store
        if (!checkedTagsString.equals(preset.getTags())) {
            preset.setLastLocation(null);
            preset.setTags(checkedTagsString);
            settingsManager.updatePOIPreset( preset );
        }
        return true;
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data){
        // See which child activity is calling us back.
        switch (requestCode) {
            case ENTERPRESETNAME:
                // This is the standard resultCode that is sent back if the
                // activity crashed or didn't doesn't supply an explicit result.
                if (resultCode == RESULT_CANCELED) {
                    break;
                } else {
                    presetId = settingsManager.createPOIPreset( data.getStringExtra("presetName") );
                    /*Spinner spinnerPresets = (Spinner) mainLayout.findViewById(R.id.spinnerPresets);
                    ArrayAdapter<POIPreset> adapter = new ArrayAdapter<POIPreset>(this,
                            android.R.layout.simple_spinner_item, settingsManager.getPOIPresetList() );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerPresets.setAdapter(adapter);*/
                    updateUserInterface();
                    break;
                }
            default:
                break;
        }
    }

    private class DLListener implements DataDownloader.DataDownloadListener {
        @Override public void dataDownloadedSuccessfully(JSONObject jsonObject) {
            CharSequence  text = "";
            try {
                if (jsonObject == null) {
                    text = "Unknown error, shouldn't happen";
                } else if (! jsonObject.getString("error").equals("")) {
                    text = jsonObject.getString("error");
                } else {
                    supportedPOITags = jsonObject.getString("supported_poi_tags");
                    updateUserInterface();
                }
            } catch (JSONException e) {
                text = e.getMessage();
            }
        }
        @Override public void dataDownloadFailed(String error) {}
        @Override public void dataDownloadCanceled() {}
    }
}
