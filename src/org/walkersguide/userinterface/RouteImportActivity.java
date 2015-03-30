package org.walkersguide.userinterface;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.walkersguide.R;
import org.walkersguide.exceptions.RouteParsingException;
import org.walkersguide.routeobjects.FootwaySegment;
import org.walkersguide.routeobjects.RouteObjectWrapper;
import org.walkersguide.utils.Globals;
import org.walkersguide.utils.ObjectParser;
import org.walkersguide.utils.Route;
import org.walkersguide.utils.SettingsManager;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class RouteImportActivity extends AbstractActivity {

    private Globals globalData;
    private SettingsManager settingsManager;
    private LinearLayout mainLayout;
    File[] availableFiles;

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_route_import);
        if (globalData == null) {
            globalData = ((Globals) getApplicationContext());
        }
        settingsManager = globalData.getSettingsManagerInstance();
        mainLayout = (LinearLayout) findViewById(R.id.linearLayoutMain);
        LayoutParams lp = new LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT );

        RadioGroup radioAvailableRoutes = (RadioGroup) mainLayout.findViewById(R.id.radioAvailableRoutes);
        CheckBox checkBoxDeleteAfterImport = (CheckBox) mainLayout.findViewById(R.id.checkBoxDeleteAfterImport);
        TextView labelNoRoutesToImport = (TextView) mainLayout.findViewById(R.id.labelNoRoutesToImport);
        // list files in import folder
        availableFiles = (new File(settingsManager.getProgramImportFolder())).listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".route");
            }
        });
        if (availableFiles == null || availableFiles.length == 0) {
            radioAvailableRoutes.setVisibility(View.GONE);
            checkBoxDeleteAfterImport.setVisibility(View.GONE);
        } else {
            labelNoRoutesToImport.setVisibility(View.GONE);
            for (int i=0; i<availableFiles.length; i++) {
                RadioButton radioFile  = new RadioButton(this);
                radioFile.setId(i);
                radioFile.setLayoutParams(lp);
                radioFile.setText(availableFiles[i].getName());
                radioAvailableRoutes.addView(radioFile);
            }
        }

        Button buttonOK = (Button) mainLayout.findViewById(R.id.buttonOK);
        buttonOK.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                RadioGroup radioAvailableRoutes = (RadioGroup) mainLayout.findViewById(R.id.radioAvailableRoutes);
                if (radioAvailableRoutes.getCheckedRadioButtonId() == -1) {
                    Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.messageChooseRouteFile), Toast.LENGTH_LONG).show();
                    return;
                }
                File routeFile = availableFiles[radioAvailableRoutes.getCheckedRadioButtonId()];
                StringBuffer datax = new StringBuffer("");
                try {
                    FileInputStream fIn = new FileInputStream(routeFile);
                    InputStreamReader isr = new InputStreamReader ( fIn );
                    BufferedReader buffreader = new BufferedReader ( isr );
                    String readString = buffreader.readLine ( );
                    while ( readString != null ) {
                        datax.append(readString);
                        readString = buffreader.readLine ( );
                    }
                    isr.close ( );
                } catch (IOException e) {
                    Intent intent = new Intent(getApplicationContext(), DialogActivity.class);
                    intent.putExtra("message", String.format(
                                getResources().getString(R.string.messageImExFileNotFoundError), ""));
                    startActivity(intent);
                    return;
                }
                // parse route array
                ArrayList<RouteObjectWrapper> routeList = new ArrayList<RouteObjectWrapper>();
                try {
                    routeList = ObjectParser.parseRouteArray(
                            new JSONArray(datax.toString() ));
                } catch (JSONException e) {
                    Intent intent = new Intent(getApplicationContext(), DialogActivity.class);
                    intent.putExtra("message", String.format(
                                getResources().getString(R.string.messageImExJSONError), routeFile.getName() ));
                    startActivity(intent);
                    return;
                } catch (RouteParsingException e) {
                    Intent intent = new Intent(getApplicationContext(), DialogActivity.class);
                    intent.putExtra("message", e.getMessage());
                    startActivity(intent);
                    return;
                }
                // should the file been deleted
                CheckBox checkBoxDeleteAfterImport = (CheckBox) mainLayout.findViewById(R.id.checkBoxDeleteAfterImport);
                if (checkBoxDeleteAfterImport.isChecked()) {
                    routeFile.delete();
                }
                // check if it's a source route or a already calculated one
                boolean isSourceRoute = false;
                for (RouteObjectWrapper object : routeList) {
                    if (object.getFootwaySegment() != null &&
                            (object.getFootwaySegment().getSubType().equals("footway_place_holder")
                             || object.getFootwaySegment().getSubType().equals("transport_place_holder")) ) {
                        isSourceRoute = true;
                        break;
                    }
                }
                Route sourceRoute;
                Intent resultData = new Intent();
                if (isSourceRoute) {
                    sourceRoute = new Route(routeList);
                    resultData.putExtra("fragment", "");
                } else {
                    ArrayList<RouteObjectWrapper> sourceRouteList = new ArrayList<RouteObjectWrapper>();
                    sourceRouteList.add(0, routeList.get(0));
                    sourceRouteList.add(1, new RouteObjectWrapper(new FootwaySegment(
                                getResources().getString(R.string.labelFootwayPlaceholder),
                                -1, -1, "footway_place_holder")));
                    sourceRouteList.add(2, routeList.get(routeList.size()-1));
                    sourceRoute = new Route(sourceRouteList);
                    globalData.setValue("route", new Route(routeList, ""));
                    globalData.setValue("newRoute", true);
                    resultData.putExtra("fragment", "router");
                }
                settingsManager.setRouteRequest(sourceRoute);
                settingsManager.addSourceRouteToHistory(sourceRoute);
                setResult(RESULT_OK, resultData);
                finish();
            }
        });

        Button buttonCancel = (Button) mainLayout.findViewById(R.id.buttonCancel);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                setResult(RESULT_CANCELED, null);
                finish();
            }
        });
    }

}
