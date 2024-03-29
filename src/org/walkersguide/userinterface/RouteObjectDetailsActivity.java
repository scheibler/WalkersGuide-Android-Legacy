package org.walkersguide.userinterface;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.R;
import org.walkersguide.exceptions.DepartureListParsingException;
import org.walkersguide.exceptions.RouteParsingException;
import org.walkersguide.routeobjects.FootwaySegment;
import org.walkersguide.routeobjects.IntersectionPoint;
import org.walkersguide.routeobjects.POIPoint;
import org.walkersguide.routeobjects.Point;
import org.walkersguide.routeobjects.RouteObjectWrapper;
import org.walkersguide.routeobjects.StationPoint;
import org.walkersguide.routeobjects.TransportSegment;
import org.walkersguide.sensors.PositionManager;
import org.walkersguide.sensors.SensorsManager;
import org.walkersguide.utils.DataDownloader;
import org.walkersguide.utils.Globals;
import org.walkersguide.utils.HelperFunctions;
import org.walkersguide.utils.KeyboardManager;
import org.walkersguide.utils.ObjectParser;
import org.walkersguide.utils.Route;
import org.walkersguide.utils.SettingsManager;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


public class RouteObjectDetailsActivity extends  AbstractActivity {

    // view ids
    private final int spinnerEntrancesId = 29381455;
    private final int wayLayoutId = 11973432;
    private final int departureLayoutId = 11973433;
    private final int trafficSignalLayoutId = 11973434;
    private final int interactiveModeID = 99123123;
    private final int labelDistanceAndBearingId = 38457192;
    private final int rgBearingId = 500;
    private final int rbWalkId = 501;
    private final int rbNorthId = 502;
    private final int rbCompassId = 503;
    private final int rgDeparturesId = 600;
    private final int rbNextHourId = 601;
    private final int rbNextTwoHoursId = 602;
    private final int rbAllConnectionsId = 603;

    private Globals globalData;
    private KeyboardManager keyboardManager;
    private PositionManager positionManager;
    private SensorsManager sensorsManager;
    private SettingsManager settingsManager;
    private TextToSpeech ttsInstance;
    private Vibrator vibrator;
    private DataDownloader followWayDownloader;
    private RelativeLayout mainLayout;
    private Dialog dialog;
    private LayoutParams lp;
    private Toast messageToast;
    private RouteObjectWrapper routeObject;
    private Point currentLocation, lastLocation, lastSpokenLocation;
    private IntersectionPoint.IntersectionWay nextWay, lastSpokenWay;
    private int currentCompassValue, lastCompassValue;
    private int prevSegmentBearing, nextSegmentBearing;
    private int lastIntersectionWay;
    private Handler progressHandler;
    private ProgressUpdater progressUpdater;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentCompassValue = -1;
        lastCompassValue = -1;
        lastIntersectionWay = -1;

        if (globalData == null) {
            globalData = ((Globals) getApplicationContext());
        }
        // keyboard manager
        keyboardManager = globalData.getKeyboardManagerInstance();
        // position manager
        positionManager = globalData.getPositionManagerInstance();
        // sensors manager
        sensorsManager = globalData.getSensorsManagerInstance();
        // settings manager
        settingsManager = globalData.getSettingsManagerInstance();
        // tts instance
        ttsInstance = globalData.getTTSInstance();
        // vibrator
        vibrator = (Vibrator) this.getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        // progress bar: vibration during route calculation
        progressHandler = new Handler();
        progressUpdater = new ProgressUpdater();

        Intent sender=getIntent();
        prevSegmentBearing = sender.getExtras().getInt("prevSegmentBearing", -1);
        nextSegmentBearing = sender.getExtras().getInt("nextSegmentBearing", -1);

        // load route object
        nextWay = lastSpokenWay = null;
        routeObject = ObjectParser.parseRouteObject(
                sender.getExtras().getString("route_object") );
        if (routeObject.isEmpty()) {
            Intent intent = new Intent(getApplicationContext(), DialogActivity.class);
            intent.putExtra("message", getResources().getString(R.string.messageEmptyRoutePoint));
            startActivity(intent);
            finish();
        }

        // load layout
        setContentView(R.layout.activity_route_object_details);
        mainLayout = (RelativeLayout) findViewById(R.id.linearLayoutMain);
        lp = new LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT );
        messageToast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);

        // buttons
        Button buttonActionMenu = (Button) mainLayout.findViewById(R.id.buttonActionMenu);
        buttonActionMenu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                showActionsMenuDialog();
            }
        });
        if (routeObject.getTransportSegment() != null
                || sender.getExtras().getInt("hideActionsButton", 0) == 1) {
            buttonActionMenu.setVisibility(View.GONE);
        }
        Button buttonCancel = (Button) mainLayout.findViewById(R.id.buttonCancel);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                finish();
            }
        });

        createUserInterface();
    }

    public void createUserInterface() {
        LinearLayout preferencesLayout = (LinearLayout) mainLayout.findViewById(R.id.linearLayoutPreferences);
        if (preferencesLayout.getChildCount() > 0)
            preferencesLayout.removeAllViews();
        // label for name and type
        TextView labelObjectName = new TextView(this);
        labelObjectName.setLayoutParams(lp);
        preferencesLayout.addView( labelObjectName );
        TextView labelObjectType = new TextView(this);
        labelObjectType.setLayoutParams(lp);
        preferencesLayout.addView( labelObjectType );

        // add distance label if the route object is a point
        if (routeObject.getPoint() != null) {
            TextView labelDistanceAndBearing = new TextView(this);
            labelDistanceAndBearing.setLayoutParams(lp);
            labelDistanceAndBearing.setId( labelDistanceAndBearingId );
            labelDistanceAndBearing.setText(
                    getResources().getString(R.string.labelObjectDistanceAndBearingUnknown));
            preferencesLayout.addView( labelDistanceAndBearing );
        }

        // preferences label
        TextView label = new TextView(this);
        label.setLayoutParams(lp);
        label.setText(getResources().getString(R.string.labelPreferencesHeading));
        preferencesLayout.addView( label );

        // set data for the specific point type
        if (routeObject.getWayPoint() != null) {
            Point wayPoint = routeObject.getWayPoint();
            // name and type
            labelObjectName.setText( String.format(
                        getResources().getString(R.string.labelObjectName),
                        wayPoint.getName() ));
            labelObjectType.setText( String.format(
                        getResources().getString(R.string.labelObjectType),
                        wayPoint.getSubType() ));
            // latitude and longitude
            label = new TextView(this);
            label.setLayoutParams(lp);
            label.setText( String.format("%1$s %2$f",
                    getResources().getString(R.string.labelLatitude), wayPoint.getLatitude() ));
            preferencesLayout.addView(label);
            label = new TextView(this);
            label.setLayoutParams(lp);
            label.setText( String.format("%1$s %2$f",
                    getResources().getString(R.string.labelLongitude), wayPoint.getLongitude() ));
            preferencesLayout.addView(label);

        } else if (routeObject.getIntersection() != null) {
            IntersectionPoint intersection = routeObject.getIntersection();
            // name and type
            labelObjectName.setText( String.format(
                        getResources().getString(R.string.labelObjectName),
                        intersection.getName() ));
            labelObjectType.setText( String.format(
                        getResources().getString(R.string.labelIntersectionObjectType),
                        intersection.getSubType() ));

            // general properties
            if (intersection.getTactilePaving() > -1) {
                label = new TextView(this);
                label.setLayoutParams(lp);
                label.setText(intersection.printTactilePaving());
                preferencesLayout.addView( label);
            }
            if (intersection.getWheelchair() > -1) {
                label = new TextView(this);
                label.setLayoutParams(lp);
                label.setText(intersection.printWheelchair());
                preferencesLayout.addView( label);
            }

            // traffic signal list
            if (intersection.getTrafficSignalList().size() > 0) {
                label = new TextView(this);
                label.setLayoutParams(lp);
                label.setText( String.format(
                        getResources().getString(R.string.labelTrafficSignals),
                        intersection.getTrafficSignalList().size() ));
                preferencesLayout.addView(label);
                // traffic signal sub layout
                LinearLayout trafficSignalLayout = new LinearLayout(this);
                trafficSignalLayout.setLayoutParams(lp);
                trafficSignalLayout.setOrientation(LinearLayout.VERTICAL);
                trafficSignalLayout.setId(trafficSignalLayoutId);
                preferencesLayout.addView(trafficSignalLayout);
            }

            // list all intersection ways
            label = new TextView(this);
            label.setLayoutParams(lp);
            label.setText( String.format(
                    getResources().getString(R.string.labelNumberOfIntersectionWays),
                    intersection.getNumberOfStreets() ));
            preferencesLayout.addView(label);

            RadioGroup rgBearing = new RadioGroup(this);
            rgBearing.setLayoutParams(lp);
            rgBearing.setId(rgBearingId);
            rgBearing.setOrientation(RadioGroup.HORIZONTAL);
            // walking direction radio button
            if (prevSegmentBearing >= 0) {
                RadioButton rbWalk  = new RadioButton(this);
                rbWalk.setLayoutParams(new RadioGroup.LayoutParams(
                        LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f));
                rbWalk.setId(rbWalkId);
                rbWalk.setText(String.format(
                        getResources().getString(R.string.radioButtonWalkingDirection),
                        HelperFunctions.getCompassDirection(prevSegmentBearing) ));
                rgBearing.addView(rbWalk);
            }
            // north direction radio button
            RadioButton rbNorth  = new RadioButton(this);
            rbNorth.setLayoutParams(new RadioGroup.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f));
            rbNorth.setId(rbNorthId);
            rbNorth.setText(getResources().getString(R.string.radioButtonDirectionNorth));
            rgBearing.addView(rbNorth);
            // dynamic compass radio button
            RadioButton rbCompass  = new RadioButton(this);
            rbCompass.setLayoutParams(new RadioGroup.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f));
            rbCompass.setId(rbCompassId);
            rbCompass.setText(String.format(
                        getResources().getString(R.string.radioButtonDirectionCompass),
                        HelperFunctions.getCompassDirection(currentCompassValue) ));
            rgBearing.addView(rbCompass);
            // action listener
            rgBearing.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    	IntersectionPoint intersection = routeObject.getIntersection();
                    for (IntersectionPoint.IntersectionWay way : intersection.getSubPoints()) {
                        if (checkedId == rbWalkId) {
                            way.setRelativeBearing(prevSegmentBearing);
                        } else if (checkedId == rbNorthId) {
                            way.setRelativeBearing(0);
                        } else if (checkedId == rbCompassId) {
                            way.setRelativeBearing(currentCompassValue);
                        }
                    }
                    Collections.sort(intersection.getSubPoints());
                    updateIntersectionData();
                }
            });
            preferencesLayout.addView(rgBearing);

            LinearLayout wayLayout = new LinearLayout(this);
            wayLayout.setLayoutParams(lp);
            wayLayout.setOrientation(LinearLayout.VERTICAL);
            wayLayout.setId(wayLayoutId);
            preferencesLayout.addView(wayLayout);

            // calculate next intersection way if we got a positive value for nextSegmentBearing
            int absValue = 0;
            int minAbsValue = 360;
            for (IntersectionPoint.IntersectionWay way : intersection.getSubPoints()) {
                absValue = Math.abs(nextSegmentBearing - way.getIntersectionBearing());
                if (absValue > 180)
                    absValue = 360 - absValue;
                if (nextSegmentBearing > -1 && absValue < minAbsValue) {
                    nextWay = way;
                    minAbsValue = absValue;
                }
            }

        } else if ((routeObject.getPOI() != null) || (routeObject.getStation() != null)) {
            POIPoint poi;
            if (routeObject.getStation() != null)
                poi = routeObject.getStation();
            else
                poi = routeObject.getPOI();
            // name and type
            labelObjectName.setText( String.format(
                        getResources().getString(R.string.labelObjectName),
                        poi.getName() ));
            labelObjectType.setText( String.format(
                        getResources().getString(R.string.labelObjectType),
                        poi.getSubType() ));

            // outer building
            label = new TextView(this);
            label.setLayoutParams(lp);
            if (poi.getOuterBuilding() != null) {
                if (poi.getOuterBuilding().getName().equals(""))
                    label.setText(getResources().getString(R.string.labelWithinBuildingYes));
                else
                    label.setText( String.format(
                                getResources().getString(R.string.labelWithinBuildingName),
                                poi.getOuterBuilding().getName() ));
            } else {
                label.setText(getResources().getString(R.string.labelWithinBuildingNo));
            }
            preferencesLayout.addView(label);

            // entrance combo box
            ArrayList<POIPoint> entranceList = null;
            if (poi.getEntranceList().size() > 0) {
                entranceList = new ArrayList<POIPoint>( poi.getEntranceList() );
            } else if (poi.getOuterBuilding() != null) {
                if (poi.getOuterBuilding().getEntranceList().size() > 0)
                    entranceList = new ArrayList<POIPoint>(
                            poi.getOuterBuilding().getEntranceList() );
            }
            if (entranceList != null) {
                POIPoint noEntrance = new POIPoint(
                        getResources().getString(R.string.labelNoEntranceChoosen),
                        poi.getLatitude(), poi.getLongitude(), "poi",
                        getResources().getString(R.string.labelBuildingEntrance) );
                entranceList.add(0, noEntrance);
                LinearLayout entranceLayout = new LinearLayout(this);
                entranceLayout.setLayoutParams(lp);
                entranceLayout.setOrientation(LinearLayout.HORIZONTAL);
                // set label
                label = new TextView(this);
                label.setLayoutParams( new LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT ));
                label.setText(getResources().getString(R.string.labelBuildingEntrances));
                entranceLayout.addView(label);
                // combo box
                Spinner spinnerEntrances = new Spinner(this);
                spinnerEntrances.setLayoutParams(lp);
                spinnerEntrances.setId( spinnerEntrancesId);
                EntranceListAdapter adapter = new EntranceListAdapter(this,
                        android.R.layout.simple_spinner_item, entranceList );
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerEntrances.setAdapter(adapter);
                spinnerEntrances.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(AdapterView<?> parent, View view,
                        int pos, long id) {
                        updateDistanceAndBearing();
                    }
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
                entranceLayout.addView(spinnerEntrances);
                preferencesLayout.addView(entranceLayout);
            }

            // other poi properties
            if (! poi.getAddress().equals("")) {
                label = new TextView(this);
                label.setLayoutParams(lp);
                label.setAutoLinkMask(Linkify.MAP_ADDRESSES);
                label.setText( String.format(
                            getResources().getString(R.string.labelPOIAddress),
                            poi.getAddress() ));
                preferencesLayout.addView(label);
            }
            if (! poi.getOpeningHours().equals("")) {
                label = new TextView(this);
                label.setLayoutParams(lp);
                label.setText( String.format(
                            getResources().getString(R.string.labelPOIOpeningHours),
                            poi.getOpeningHours() ));
                preferencesLayout.addView(label);
            }
            if (! poi.getPhone().equals("")) {
                label = new TextView(this);
                label.setLayoutParams(lp);
                label.setAutoLinkMask(Linkify.PHONE_NUMBERS);
                label.setText( String.format(
                            getResources().getString(R.string.labelPOIPhone),
                            poi.getPhone() ));
                preferencesLayout.addView(label);
            }
            if (! poi.getEmail().equals("")) {
                label = new TextView(this);
                label.setLayoutParams(lp);
                label.setAutoLinkMask(Linkify.EMAIL_ADDRESSES);
                label.setText( String.format(
                            getResources().getString(R.string.labelPOIEmail),
                            poi.getEmail() ));
                preferencesLayout.addView(label);
            }
            if (! poi.getWebsite().equals("")) {
                label = new TextView(this);
                label.setLayoutParams(lp);
                label.setAutoLinkMask(Linkify.WEB_URLS);
                label.setText( String.format(
                            getResources().getString(R.string.labelPOIWebsite),
                            poi.getWebsite() ));
                preferencesLayout.addView(label);
            }

            // general properties
            if (poi.getTactilePaving() > -1) {
                label = new TextView(this);
                label.setLayoutParams(lp);
                label.setText(poi.printTactilePaving());
                preferencesLayout.addView( label);
            }
            if (poi.getWheelchair() > -1) {
                label = new TextView(this);
                label.setLayoutParams(lp);
                label.setText(poi.printWheelchair());
                preferencesLayout.addView( label);
            }
            if (poi.hasTrafficSignalsAccessibility()) {
                label = new TextView(this);
                label.setLayoutParams(lp);
                label.setText(poi.printTrafficSignalsAccessibility());
                preferencesLayout.addView( label);
            }

            if (routeObject.getStation() != null) {
                StationPoint station = routeObject.getStation();
                // list all lines
                if (! station.getLines().isEmpty()) {
                    label = new TextView(this);
                    label.setLayoutParams(lp);
                    label.setText(getResources().getString(R.string.labelAvailableTransportLines));
                    preferencesLayout.addView(label);
                    for (StationPoint.Line line : station.getLines()) {
                        label = new TextView(this);
                        label.setLayoutParams(lp);
                        label.setText( line.toString() );
                        preferencesLayout.addView(label);
                    }
                }

                // departure layout
                label = new TextView(this);
                label.setLayoutParams(lp);
                label.setText(getResources().getString(R.string.labelNextDepartures));
                preferencesLayout.addView(label);

                RadioGroup rgDepartures = new RadioGroup(this);
                rgDepartures.setLayoutParams(lp);
                rgDepartures.setId(rgDeparturesId);
                rgDepartures.setOrientation(RadioGroup.HORIZONTAL);
                // next hour
                RadioButton rbNextHour  = new RadioButton(this);
                rbNextHour.setLayoutParams(new RadioGroup.LayoutParams(
                            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f));
                rbNextHour.setId(rbNextHourId);
                rbNextHour.setText(getResources().getString(R.string.radioButtonDeparturesNextHour));
                rgDepartures.addView(rbNextHour);
                // next two hours
                RadioButton rbNextTwoHours  = new RadioButton(this);
                rbNextTwoHours.setLayoutParams(new RadioGroup.LayoutParams(
                            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f));
                rbNextTwoHours.setId(rbNextTwoHoursId);
                rbNextTwoHours.setText(getResources().getString(R.string.radioButtonDeparturesNextTwoHours));
                rgDepartures.addView(rbNextTwoHours);
                // all connections
                RadioButton rbAllConnections  = new RadioButton(this);
                rbAllConnections.setLayoutParams(new RadioGroup.LayoutParams(
                            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f));
                rbAllConnections.setId(rbAllConnectionsId);
                rbAllConnections.setText(getResources().getString(R.string.radioButtonAllDepartures));
                rgDepartures.addView(rbAllConnections);
                // action listener
                rgDepartures.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                        updateDepartureTable();
                    }
                });
                preferencesLayout.addView(rgDepartures);

                LinearLayout departureLayout = new LinearLayout(this);
                departureLayout.setLayoutParams(lp);
                departureLayout.setId(departureLayoutId);
                departureLayout.setOrientation(LinearLayout.VERTICAL);
                preferencesLayout.addView(departureLayout);

                // query latest departures from server
                station.setNextDepartures(null);
                station.setQueryDeparturesError("");
                JSONObject requestJson = new JSONObject();
                try {
                    requestJson.put("lat", station.getLatitude());
                    requestJson.put("lon", station.getLongitude());
                    requestJson.put("language", Locale.getDefault().getLanguage());
                    requestJson.put("session_id", globalData.getSessionId());
                } catch (JSONException e) {
                    return;
                }
                try {
                    requestJson.put("vehicles", TextUtils.join("+", station.getVehicles()));
                } catch (JSONException e) {}
                DataDownloader downloader = new DataDownloader(RouteObjectDetailsActivity.this);
                downloader.setDataDownloadListener(new DLListener() );
                downloader.execute(
                        globalData.getSettingsManagerInstance().getServerPath() + "/get_departures",
                        requestJson.toString() );

                // check first radio button
                rgDepartures.check(rbNextHourId);
            }

        } else if (routeObject.getFootwaySegment() != null) {
            FootwaySegment footway = routeObject.getFootwaySegment();
            // name and type
            labelObjectName.setText( String.format(
                        getResources().getString(R.string.labelObjectName),
                        footway.getName() ));
            labelObjectType.setText( String.format(
                        getResources().getString(R.string.labelObjectType),
                        footway.getSubType() ));

            // footway segment properties
            label = new TextView(this);
            label.setLayoutParams(lp);
            label.setText( String.format(
                        getResources().getString(R.string.labelFootwayLength),
                        footway.getDistance() ));
            preferencesLayout.addView(label);
            label = new TextView(this);
            label.setLayoutParams(lp);
            label.setText( String.format(
                        getResources().getString(R.string.labelFootwayBearing),
                        HelperFunctions.getCompassDirection( footway.getBearing()) ));
            preferencesLayout.addView(label);
            if (! footway.getSurface().equals("")) {
                label = new TextView(this);
                label.setLayoutParams(lp);
                label.setText( String.format(
                            getResources().getString(R.string.labelFootwaySurface),
                            footway.getSurface() ));
                preferencesLayout.addView(label);
            }
            if (footway.getSidewalk() > -1) {
                label = new TextView(this);
                label.setLayoutParams(lp);
                label.setText(footway.printSidewalk());
                preferencesLayout.addView(label);
            }

        } else if (routeObject.getTransportSegment() != null) {
            TransportSegment transport = routeObject.getTransportSegment();
            // name and type
            labelObjectName.setText( String.format(
                        getResources().getString(R.string.labelObjectName),
                        transport.getName() ));
            if (transport.getLine().startsWith("T")) {
                labelObjectType.setText(getResources().getString(R.string.labelObjectTypeTram));
            } else if (transport.getLine().startsWith("B")) {
                labelObjectType.setText(getResources().getString(R.string.labelObjectTypeBus));
            } else if (transport.getLine().startsWith("R")) {
                labelObjectType.setText(getResources().getString(R.string.labelObjectTypeRTrain));
            } else if (transport.getLine().startsWith("I")) {
                labelObjectType.setText(getResources().getString(R.string.labelObjectTypeITrain));
            } else if (transport.getLine().startsWith("S")) {
                labelObjectType.setText(getResources().getString(R.string.labelObjectTypeLightrail));
            } else if (transport.getLine().startsWith("U")) {
                labelObjectType.setText(getResources().getString(R.string.labelObjectTypeSubway));
            } else if (transport.getLine().startsWith("F")) {
                labelObjectType.setText(getResources().getString(R.string.labelObjectTypeFerry));
            } else {
                labelObjectType.setText(getResources().getString(R.string.labelObjectTypeUnknown));
            }

            // public transport segment properties
            label = new TextView(this);
            label.setLayoutParams(lp);
            label.setText( String.format(
                        getResources().getString(R.string.labelTransportDeparture),
                        transport.getDepartureTime() ));
            preferencesLayout.addView(label);
            label = new TextView(this);
            label.setLayoutParams(lp);
            label.setText( String.format(
                        getResources().getString(R.string.labelTransportArrival),
                        transport.getArrivaltime() ));
            preferencesLayout.addView(label);
            label = new TextView(this);
            label.setLayoutParams(lp);
            label.setText( String.format(
                        getResources().getString(R.string.labelTransportDuration),
                        transport.getDuration() ));
            preferencesLayout.addView(label);

            if (transport.getNumberOfStops() > 0) {
                label = new TextView(this);
                label.setLayoutParams(lp);
                label.setText( String.format(
                            getResources().getString(R.string.labelTransportNumInterStops),
                            transport.getNumberOfStops() ));
                preferencesLayout.addView(label);
                int index = 1;
                for (String stop : transport.getStops()) {
                    label = new TextView(this);
                    label.setLayoutParams(lp);
                    label.setText( String.format(
                                getResources().getString(R.string.labelTransportInterStop),
                                index, stop ));
                    preferencesLayout.addView(label);
                    index += 1;
                }
            } else {
                label = new TextView(this);
                label.setLayoutParams(lp);
                label.setText(getResources().getString(R.string.labelTransportNoInterStops));
                preferencesLayout.addView(label);
            }
        } else {
            labelObjectName.setText("");
            labelObjectType.setText(getResources().getString(R.string.labelObjectTypeUnsupported));
        }
    }

    @Override public void onPause() {
        super.onPause();
        if (followWayDownloader != null) {
            followWayDownloader.cancelDownloadProcess();
        }
    }

    @Override public void onResume() {
        super.onResume();
        settingsManager = globalData.getSettingsManagerInstance();
        keyboardManager.setKeyboardListener(new MyKeyboardListener());
        sensorsManager.setSensorsListener(new MySensorsListener());
        positionManager.setPositionListener(new MyPositionListener());
    }

    public void updateDistanceAndBearing() {
        if (routeObject.getPoint() == null
                || currentLocation == null) {
            return;
        }
        TextView labelDistanceAndBearing = (TextView) findViewById(labelDistanceAndBearingId);
        Point destination;
        // if the building has an entrance
        Spinner spinnerEntrances = (Spinner) findViewById(spinnerEntrancesId);
        if (spinnerEntrances != null) {
            destination = (POIPoint) spinnerEntrances.getSelectedItem();
        } else {
            destination = routeObject.getPoint();
        }
        labelDistanceAndBearing.setText( String.format(
                    getResources().getString(R.string.labelObjectDistanceAndBearing),
                    currentLocation.distanceTo(destination),
                    HelperFunctions.getFormatedDirection(
                        currentLocation.bearingTo(destination) - currentCompassValue) ));
    }

    public void updateIntersectionData() {
        // if the current point is an intersection, list all traffic signals and streets
        IntersectionPoint intersection = routeObject.getIntersection();
        if (intersection == null
                || currentLocation == null) {
            return;
        }
        LinearLayout wayLayout = (LinearLayout) findViewById(wayLayoutId);
        if (wayLayout.getChildCount() > 0)
            wayLayout.removeAllViews();
        int index = 1;
        for (IntersectionPoint.IntersectionWay way : intersection.getSubPoints()) {
            Button wayTextField = new Button(this);
            wayTextField.setLayoutParams(lp);
            wayTextField.setId(index - 1);
            if (way == nextWay) {
                wayTextField.setText( String.format(
                            getResources().getString(R.string.labelInterWayDetailsNextWay),
                            index,
                            HelperFunctions.getFormatedDirection(way.getRelativeBearing()),
                            way.toString() ));
            } else {
                wayTextField.setText( String.format(
                            getResources().getString(R.string.labelInterWayDetails),
                            index,
                            HelperFunctions.getFormatedDirection(way.getRelativeBearing()),
                            way.toString() ));
            }
            wayTextField.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    showChooseWayDialog(view.getId());
                }
            });
            wayLayout.addView(wayTextField);
            index++;
        }
        if (intersection.getTrafficSignalList().size() > 0) {
            ArrayList<POIPoint> trafficSignalList = new ArrayList<POIPoint>();
            for(POIPoint signal : intersection.getTrafficSignalList()) {
                signal.addBearing( currentLocation.bearingTo(signal) - currentCompassValue);
                signal.addDistance( currentLocation.distanceTo(signal) );
                trafficSignalList.add(signal);
            }
            Collections.sort(trafficSignalList);
            LinearLayout trafficSignalLayout = (LinearLayout) findViewById(trafficSignalLayoutId);
            if (trafficSignalLayout.getChildCount() > 0)
                trafficSignalLayout.removeAllViews();
            for(POIPoint signal : trafficSignalList) {
                TextView signalTextField = new TextView(this);
                signalTextField.setLayoutParams(lp);
                signalTextField.setText( String.format(
                            getResources().getString(R.string.labelTrafficSignalDistanceAndBearing),
                            signal.getDistance(),
                            HelperFunctions.getFormatedDirection(signal.getBearing()),
                            signal.getName() ));
                trafficSignalLayout.addView(signalTextField);
            }
        }
    }

    public void updateDepartureTable() {
        // if the current point is a station, list the next departures
        StationPoint station = routeObject.getStation();
        if (station == null)
            return;
        RadioGroup rgDepartures = (RadioGroup) findViewById(rgDeparturesId);
        LinearLayout departureLayout = (LinearLayout) findViewById(departureLayoutId);
        if (departureLayout.getChildCount() > 0)
            departureLayout.removeAllViews();
        TextView label;
        if (station.getNextDepartures() == null) {
            label = new TextView(this);
            label.setLayoutParams(lp);
            if (station.getQueryDeparturesError().equals("")) {
                label.setText(getResources().getString(R.string.messagePleaseWait));
            } else {
                label.setText(station.getQueryDeparturesError());
            }
            departureLayout.addView(label);
        } else {
            int numberOfEntries = 0;
            for (StationPoint.Departure departure : station.getNextDepartures()) {
                if (
                        (departure.getRemaining() < 60
                            && rgDepartures.getCheckedRadioButtonId() == rbNextHourId)
                        || (departure.getRemaining() >= 60
                            && departure.getRemaining() < 120
                            && rgDepartures.getCheckedRadioButtonId() == rbNextTwoHoursId)
                        || (departure.getRemaining() >= 120
                            && rgDepartures.getCheckedRadioButtonId() == rbAllConnectionsId)
                        ) {
                    label= new TextView(this);
                    label.setLayoutParams(lp);
                    label.setText( String.format(
                                getResources().getString(R.string.labelDepartureTableRow),
                                departure.getLine(), departure.getDirection(),
                                departure.getRemaining(), departure.getTime() ));
                    departureLayout.addView(label);
                    numberOfEntries++;
                }
            }
            if (numberOfEntries == 0) {
                label = new TextView(this);
                label.setLayoutParams(lp);
                label.setText(getResources().getString(R.string.messageFoundNoDepartures));
                departureLayout.addView(label);
            }
        }
    }

    private void showActionsMenuDialog() {
        Point point = routeObject.getPoint();
        FootwaySegment footway = routeObject.getFootwaySegment();
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_scrollable_empty);
        dialog.setCanceledOnTouchOutside(false);
        LinearLayout dialogLayout = (LinearLayout) dialog.findViewById(R.id.linearLayoutMain);
        TextView label;
        LayoutParams lpMarginTop = new LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT );
        ((MarginLayoutParams) lpMarginTop).topMargin =
                Math.round(15 * getApplicationContext().getResources().getDisplayMetrics().density);
        // heading
        label = new TextView(this);
        label.setLayoutParams(lp);
        if (point != null)
            label.setText( String.format(
                        getResources().getString(R.string.labelActionsRoutePointDescription),
                        point.getName() ));
        if (footway != null)
            label.setText( String.format(
                        getResources().getString(R.string.labelActionsRoutePointDescription),
                        footway.getName() ));
        dialogLayout.addView(label);

        if (footway != null) {
            Button buttonBlockFootwaySegment = new Button(this);
            buttonBlockFootwaySegment.setLayoutParams(lp);
            if (settingsManager.footwaySegmentBlocked(footway))
                buttonBlockFootwaySegment.setText(getResources().getString(R.string.buttonUnblockFootwaySegment));
            else
                buttonBlockFootwaySegment.setText(getResources().getString(R.string.buttonBlockFootwaySegment));
            buttonBlockFootwaySegment.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    FootwaySegment footway = routeObject.getFootwaySegment();
                    if (settingsManager.footwaySegmentBlocked(footway)) {
                        settingsManager.unblockFootwaySegment(footway);
                    } else {
                        Intent intent = new Intent(getApplicationContext(), BlockWaySegmentActivity.class);
                        intent.putExtra("footway_object", footway.toJson().toString());
                        startActivity(intent);
                    }
                    dialog.dismiss();
                }
            });
            dialogLayout.addView(buttonBlockFootwaySegment);
        }

        if (point != null) {
            Button buttonSimulation = new Button(this);
            buttonSimulation.setLayoutParams(lp);
            if ((currentLocation.distanceTo(routeObject.getPoint()) == 0) && (positionManager.getStatus() == PositionManager.Status.SIMULATION)) {
                buttonSimulation.setText(getResources().getString(R.string.buttonStopSimulation));
            } else {
                buttonSimulation.setText(getResources().getString(R.string.buttonPointSimulation));
            }
            buttonSimulation.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    if ((currentLocation.distanceTo(routeObject.getPoint()) == 0) && (positionManager.getStatus() == PositionManager.Status.SIMULATION)) {
                        positionManager.changeStatus(PositionManager.Status.GPS, null);
                        messageToast.setText(
                            getResources().getString(R.string.messageSimulationStopped));
                    } else {
                        positionManager.changeStatus(PositionManager.Status.SIMULATION, routeObject.getPoint());
                        messageToast.setText(
                            getResources().getString(R.string.messagePositionSimulated));
                    }
                    messageToast.show();
                    dialog.dismiss();
                }
            });
            dialogLayout.addView(buttonSimulation);

            Button buttonToFavorites = new Button(this);
            buttonToFavorites.setLayoutParams(lp);
            if (settingsManager.favoritesContains(routeObject)) {
                buttonToFavorites.setText(getResources().getString(R.string.buttonToFavoritesClicked));
            } else {
                buttonToFavorites.setText(getResources().getString(R.string.buttonToFavorites));
            }
            buttonToFavorites.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    if (settingsManager.favoritesContains(routeObject)) {
                        settingsManager.removePointFromFavorites(routeObject);
                        messageToast.setText(
                            getResources().getString(R.string.messageRemovedFromFavorites));
                    } else {
                        settingsManager.addPointToFavorites(routeObject);
                        messageToast.setText(
                            getResources().getString(R.string.messageAddedToFavorites));
                    }
                    messageToast.show();
                    dialog.dismiss();
                }
            });
            dialogLayout.addView(buttonToFavorites);

            // new object
            label = new TextView(this);
            label.setLayoutParams(lpMarginTop);
            label.setText(getResources().getString(R.string.labelAsNewRouteObject));
            dialogLayout.addView(label);
            Route sourceRoute = settingsManager.getRouteRequest();
            int index = 0;
            for (RouteObjectWrapper object : sourceRoute.getRouteList()) {
                Button buttonRouteObject = new Button(this);
                buttonRouteObject.setLayoutParams(lp);
                buttonRouteObject.setId(index);
                if (index == 0) {
                    buttonRouteObject.setText(getResources().getString(R.string.buttonAsNewStartPoint));
                } else if (index == sourceRoute.getSize()-1) {
                    buttonRouteObject.setText(getResources().getString(R.string.buttonAsNewDestinationPoint));
                } else if (index % 2 == 0) {
                    if (sourceRoute.getSize() == 5) {
                        buttonRouteObject.setText(
                                getResources().getString(R.string.buttonAsNewSingleIntermediatePoint));
                    } else {
                        buttonRouteObject.setText( String.format(
                                    getResources().getString(R.string.buttonAsNewMultiIntermediatePoint),
                                    (index/2) ));
                    }
                } else {
                    // skip route segments
                    index += 1;
                    continue;
                }
                buttonRouteObject.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view) {
                        Spinner spinnerEntrances = (Spinner) findViewById(spinnerEntrancesId);
                        if (spinnerEntrances != null && spinnerEntrances.getSelectedItemPosition() > 0) {
                            POIPoint entrance = (POIPoint) spinnerEntrances.getSelectedItem();
                            routeObject.getPoint().setName(routeObject.getPoint().getName()
                                + " (" + entrance.getName() + ")");
                            routeObject.getPoint().setLatitude(entrance.getLatitude());
                            routeObject.getPoint().setLongitude(entrance.getLongitude());
                        }
                        Route sourceRoute = settingsManager.getRouteRequest();
                        sourceRoute.replaceRouteObjectAtIndex(view.getId(), routeObject);
                        settingsManager.setRouteRequest(sourceRoute);
                        Intent resultData = new Intent();
                        resultData.putExtra("fragment", "start");
                        setResult(RESULT_OK, resultData);
                        finish();
                    }
                });
                dialogLayout.addView(buttonRouteObject);
                index += 1;
            }
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

    private void showChooseWayDialog(int index) {
        IntersectionPoint.IntersectionWay intersectionWay = routeObject.getIntersection().getSubPoints().get(index);
        FootwaySegment footway = new FootwaySegment(intersectionWay.getName(), 25,
            intersectionWay.getIntersectionBearing(), intersectionWay.getSubType());
        footway.addWayId(intersectionWay.getWayId());

        dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_scrollable_empty);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnKeyListener(new OnKeyListener() {
            @Override public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    if (followWayDownloader != null) {
                        followWayDownloader.cancelDownloadProcess();
                    }
                    dialog.dismiss();
                }
                return true;
            }
        });
        LinearLayout dialogLayout = (LinearLayout) dialog.findViewById(R.id.linearLayoutMain);
        TextView label= new TextView(this);
        label.setLayoutParams(lp);
        label.setText( String.format(
                    getResources().getString(R.string.labelInterWayDialogHeading),
                    intersectionWay.getName(),
                    HelperFunctions.getFormatedDirection(intersectionWay.getIntersectionBearing() - currentCompassValue) ));
        dialogLayout.addView(label);

        // buttons
        Button buttonBlockFootwaySegment = new Button(this);
        buttonBlockFootwaySegment.setLayoutParams(lp);
        buttonBlockFootwaySegment.setId(index);
        if (settingsManager.footwaySegmentBlocked(footway))
            buttonBlockFootwaySegment.setText(getResources().getString(R.string.buttonUnblockFootwaySegment));
        else
            buttonBlockFootwaySegment.setText(getResources().getString(R.string.buttonBlockFootwaySegment));
        buttonBlockFootwaySegment.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                IntersectionPoint.IntersectionWay intersectionWay = 
                    routeObject.getIntersection().getSubPoints().get(view.getId());
                FootwaySegment footway = new FootwaySegment(intersectionWay.getName(), 25,
                    intersectionWay.getIntersectionBearing(), intersectionWay.getSubType());
                footway.addWayId(intersectionWay.getWayId());
                if (settingsManager.footwaySegmentBlocked(footway)) {
                    settingsManager.unblockFootwaySegment(footway);
                } else {
                    Intent intent = new Intent(getApplicationContext(), BlockWaySegmentActivity.class);
                    intent.putExtra("footway_object", footway.toJson().toString());
                    startActivity(intent);
                }
                dialog.dismiss();
            }
        });
        dialogLayout.addView(buttonBlockFootwaySegment);

        Button buttonRoute = new Button(this);
        buttonRoute.setLayoutParams(lp);
        buttonRoute.setId(index);
        buttonRoute.setText(getResources().getString(R.string.buttonInterWayRoute));
        buttonRoute.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                progressHandler.postDelayed(progressUpdater, 100);
                calculateRouteForIntersectionWay(view.getId(), false);
            }
        });
        dialogLayout.addView(buttonRoute);

        Button buttonFullRoute = new Button(this);
        buttonFullRoute.setLayoutParams(lp);
        buttonFullRoute.setId(index);
        buttonFullRoute.setText(getResources().getString(R.string.buttonInterWayRouteAll));
        buttonFullRoute.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                progressHandler.postDelayed(progressUpdater, 100);
                calculateRouteForIntersectionWay(view.getId(), true);
            }
        });
        dialogLayout.addView(buttonFullRoute);

        Button buttonCancel = new Button(this);
        buttonCancel.setLayoutParams(lp);
        buttonCancel.setText(getResources().getString(R.string.dialogCancel));
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (followWayDownloader != null) {
                    followWayDownloader.cancelDownloadProcess();
                }
                dialog.dismiss();
            }
        });
        dialogLayout.addView(buttonCancel);
        dialog.show();
    }

    private boolean calculateRouteForIntersectionWay(int intersectionWayIndex, boolean allIntersections) {
        IntersectionPoint.IntersectionWay intersectionWay =
            routeObject.getIntersection().getSubPoints().get(intersectionWayIndex);
        // start
        JSONObject startPointJson = routeObject.toJson();
        if (startPointJson == null) {
            Intent intent = new Intent(getApplicationContext(), DialogActivity.class);
            intent.putExtra("message",
                    getResources().getString(R.string.messageEmptyStartPoint));
            startActivity(intent);
            return false;
        }
        // create post request
        JSONObject requestJson = new JSONObject();
        try {
            JSONObject optionsJson = new JSONObject();
            optionsJson.put("way_id", intersectionWay.getWayId());
            optionsJson.put("bearing", intersectionWay.getIntersectionBearing());
            optionsJson.put("language", Locale.getDefault().getLanguage());
            optionsJson.put("session_id", globalData.getSessionId());
            if (allIntersections)
                optionsJson.put("add_all_intersections", "yes");
            else
                optionsJson.put("add_all_intersections", "no");
            requestJson.put("options", optionsJson);
            requestJson.put("start_point", startPointJson);
        } catch (JSONException e) {
            Intent intent = new Intent(getApplicationContext(), DialogActivity.class);
            intent.putExtra("message",
                    getResources().getString(R.string.messageEmptyJSONObject));
            startActivity(intent);
            return false;
        }
        if (followWayDownloader != null) {
            followWayDownloader.cancelDownloadProcess();
            Toast.makeText(this, getResources().getString(R.string.messageRouteComputationCanceled),
                    Toast.LENGTH_LONG).show();
        } else {
            followWayDownloader = new DataDownloader(RouteObjectDetailsActivity.this);
            followWayDownloader.setDataDownloadListener(new FollowWayDLListener() );
            followWayDownloader.execute(
                    globalData.getSettingsManagerInstance().getServerPath() + "/follow_this_way",
                    requestJson.toString() );
            Toast.makeText(this, getResources().getString(R.string.messageRouteComputationStarted),
                    Toast.LENGTH_LONG).show();
        }
        return true;
    }

    private class MyKeyboardListener implements KeyboardManager.KeyboardListener {
        public void longPressed(KeyEvent event) {}
        public void headsetButtonPressed(int numberOfClicks) {
            if (numberOfClicks == 1) {
                TextView labelDistanceAndBearing = (TextView) findViewById(labelDistanceAndBearingId);
                if (labelDistanceAndBearing != null) {
                    ttsInstance.speak(labelDistanceAndBearing.getText().toString(), TextToSpeech.QUEUE_FLUSH, null);
                    lastSpokenLocation = currentLocation;
                }
            }
        }
    }

    private class MyPositionListener implements PositionManager.PositionListener {
        public void locationChanged(Location location) {
            currentLocation = new Point(
                    String.format(
                        getResources().getString(R.string.locationNameCurrentPositionWithAccuracy),
                        (int) Math.round(location.getAccuracy()) ),
                    location);
            updateDistanceAndBearing();
            if (lastLocation == null
                    || lastLocation.distanceTo(currentLocation) > 5) {
                lastLocation = currentLocation;
                updateIntersectionData();
            }
            // check intersection radio button
            RadioGroup rgBearing = (RadioGroup) findViewById(rgBearingId);
            if (rgBearing != null) {
                if (rgBearing.getCheckedRadioButtonId() == -1) {
                    // nothing checked yet
                    if (currentLocation.distanceTo(routeObject.getPoint()) < 25) {
                        rgBearing.check(rbCompassId);
                    } else if (prevSegmentBearing >= 0) {
                        rgBearing.check(rbWalkId);
                    } else {
                        rgBearing.check(rbNorthId);
                    }
                }
            }
            // speak distance from time to time
            TextView labelDistanceAndBearing = (TextView) findViewById(labelDistanceAndBearingId);
            if (labelDistanceAndBearing != null
                    && location.getSpeed() < 3.0
                    && (lastSpokenLocation == null || currentLocation.distanceTo(lastSpokenLocation) > 20)) {
                lastSpokenLocation = currentLocation;
                ttsInstance.speak(labelDistanceAndBearing.getText().toString(), TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    }

    private class MySensorsListener implements SensorsManager.SensorsListener {
        public void compassValueChanged(int degree) {
            currentCompassValue = degree;
            updateDistanceAndBearing();
            // update compass radio button label, if available
            RadioButton rbCompass  = (RadioButton) findViewById(rbCompassId);
            if (rbCompass != null) {
                rbCompass.setText(String.format(
                            getResources().getString(R.string.radioButtonDirectionCompass),
                            HelperFunctions.getCompassDirection(currentCompassValue) ));
            }
            // rest is for intersections only
            IntersectionPoint intersection = routeObject.getIntersection();
            int diff = Math.abs(currentCompassValue - lastCompassValue);
            if (diff > 180)
                diff = 360 - diff;
            if (currentLocation != null
                    && intersection != null
                    && diff >= 15) {
                lastCompassValue = currentCompassValue;
                // access direction radio group
                RadioGroup rgBearing = (RadioGroup) findViewById(rgBearingId);
                if (rgBearing.getCheckedRadioButtonId() == rbCompassId) {
                    // sort intersection ways
                    for (IntersectionPoint.IntersectionWay way : intersection.getSubPoints()) {
                        way.setRelativeBearing(currentCompassValue);
                    }
                    Collections.sort(intersection.getSubPoints());
                    // speak the intersection street name, which is in front of the user
                    IntersectionPoint.IntersectionWay wayInFrontOfUser = null;
                    for (IntersectionPoint.IntersectionWay way : intersection.getSubPoints()) {
                        String direction = HelperFunctions.getFormatedDirection(way.getRelativeBearing());
                        if (direction.equals(getResources().getString(R.string.directionStraightforward))) {
                            wayInFrontOfUser = way;
                        }
                    }
                    if (wayInFrontOfUser != null && wayInFrontOfUser != lastSpokenWay) {
                        lastSpokenWay = wayInFrontOfUser;
                        messageToast.cancel();
                        messageToast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);
                        if (wayInFrontOfUser == nextWay) {
                            messageToast.setText( String.format(
                                        getResources().getString(R.string.messageInterWayNameNextPoint),
                                        wayInFrontOfUser.getName() ));
                        } else {
                            messageToast.setText(wayInFrontOfUser.getName());
                        }
                        messageToast.show();
                    }
                }
                updateIntersectionData();
            }
        }
        public void shakeDetected() {}
    }

    private class FollowWayDLListener implements DataDownloader.DataDownloadListener {
        @Override public void dataDownloadedSuccessfully(JSONObject jsonObject) {
            progressHandler.removeCallbacks(progressUpdater);
            followWayDownloader = null;
            try {
                Route route = ObjectParser.parseSingleRoute(jsonObject);
                globalData.setValue("route", route);
                globalData.setValue("newRoute", true);
                Intent resultData = new Intent();
                resultData.putExtra("fragment", "router");
                setResult(RESULT_OK, resultData);
                finish();
            } catch (RouteParsingException e) {
                Intent intent = new Intent(getApplicationContext(), DialogActivity.class);
                intent.putExtra("message", String.format(
                        getResources().getString(R.string.messageRouteParsingFailed), e.getMessage()) );
                startActivity(intent);
            }
        }

        @Override public void dataDownloadFailed(String error) {
            progressHandler.removeCallbacks(progressUpdater);
            followWayDownloader = null;
            Intent intent = new Intent(getApplicationContext(), DialogActivity.class);
            intent.putExtra("message", String.format(
                        getResources().getString(R.string.messageNetworkError), error) );
            startActivity(intent);
        }

        @Override public void dataDownloadCanceled() {
            progressHandler.removeCallbacks(progressUpdater);
            followWayDownloader = null;
            JSONObject requestJson = new JSONObject();
            try {
                requestJson.put("language", Locale.getDefault().getLanguage());
                requestJson.put("session_id", globalData.getSessionId());
            } catch (JSONException e) {
                return;
            }
            DataDownloader cancelDownloader = new DataDownloader(getApplicationContext());
            cancelDownloader.setDataDownloadListener(new CanceledRequestDownloadListener() );
            cancelDownloader.execute(
                    globalData.getSettingsManagerInstance().getServerPath() + "/cancel_request",
                    requestJson.toString() );
        }
    }

    private class DLListener implements DataDownloader.DataDownloadListener {
        @Override public void dataDownloadedSuccessfully(JSONObject jsonObject) {
            StationPoint station = routeObject.getStation();
            try {
                station.setNextDepartures(
                        ObjectParser.parseStationDepartureList(station, jsonObject));
            } catch (DepartureListParsingException e) {
                station.setQueryDeparturesError(e.getMessage());
            }
            updateDepartureTable();
        }

        @Override public void dataDownloadFailed(String error) {
            StationPoint station = routeObject.getStation();
            station.setQueryDeparturesError(String.format(
                        getResources().getString(R.string.messageNetworkError), error));
            updateDepartureTable();
        }

        @Override public void dataDownloadCanceled() {
            StationPoint station = routeObject.getStation();
            station.setQueryDeparturesError(getResources().getString(R.string.messageDownloadCanceled));
            updateDepartureTable();
        }
    }

    private class CanceledRequestDownloadListener implements DataDownloader.DataDownloadListener {
        @Override public void dataDownloadedSuccessfully(JSONObject jsonObject) {}
        @Override public void dataDownloadFailed(String error) {}
        @Override public void dataDownloadCanceled() {}
    }


    public class EntranceListAdapter extends ArrayAdapter<POIPoint> {
        private Context ctx;
        private LayoutInflater m_inflater = null;
        public ArrayList<POIPoint> entranceArray;

        public EntranceListAdapter(Context context, int textViewResourceId, ArrayList<POIPoint> array) {
            super(context, textViewResourceId);
            this.entranceArray = array;
            this.ctx = context;
            m_inflater = LayoutInflater.from(ctx);
        }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            EntryHolder holder;
            if (convertView == null) {
                holder = new EntryHolder();
                convertView = m_inflater.inflate(R.layout.table_row_one_column, parent, false);
                holder.entranceName = (TextView) convertView.findViewById(R.id.labelColumnOne);
                convertView.setTag(holder);
            } else {
                holder = (EntryHolder) convertView.getTag();
            }
            POIPoint poi = getItem(position);
            holder.entranceName.setText( poi.getName() );
            return convertView;
        }

        @Override public View getDropDownView(int position, View convertView, ViewGroup parent) {
            EntryHolder holder;
            if (convertView == null) {
                holder = new EntryHolder();
                convertView = m_inflater.inflate(R.layout.table_row_one_column, parent, false);
                holder.entranceName = (TextView) convertView.findViewById(R.id.labelColumnOne);
                convertView.setTag(holder);
            } else {
                holder = (EntryHolder) convertView.getTag();
            }
            POIPoint poi = getItem(position);
            holder.entranceName.setText( String.format(
                        getResources().getString(R.string.labelEntranceDropDown),
                        poi.getName(),
                        currentLocation.distanceTo(poi),
                        HelperFunctions.getFormatedDirection(currentLocation.bearingTo(poi)-currentCompassValue) ));
            return convertView;
        }

        @Override public int getCount() {
            if (entranceArray!= null)
                return entranceArray.size();
            return 0;
        }

        @Override public POIPoint getItem(int position) {
            return entranceArray.get(position);
        }

        public void setArrayList(ArrayList<POIPoint> array) {
            this.entranceArray = array;
            notifyDataSetChanged();
        }

        private class EntryHolder {
            public TextView entranceName;
        }
    }

    private class ProgressUpdater implements Runnable {
        public void run() {
            vibrator.vibrate(50);
            progressHandler.postDelayed(this, 2000);
        }
    }
}
