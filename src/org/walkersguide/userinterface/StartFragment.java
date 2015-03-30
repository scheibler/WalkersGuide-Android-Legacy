package org.walkersguide.userinterface;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.MainActivity;
import org.walkersguide.R;
import org.walkersguide.exceptions.RouteParsingException;
import org.walkersguide.routeobjects.FootwaySegment;
import org.walkersguide.routeobjects.POIPoint;
import org.walkersguide.routeobjects.Point;
import org.walkersguide.routeobjects.RouteObjectWrapper;
import org.walkersguide.sensors.PositionManager;
import org.walkersguide.utils.AddressManager;
import org.walkersguide.utils.DataDownloader;
import org.walkersguide.utils.Globals;
import org.walkersguide.utils.KeyboardManager;
import org.walkersguide.utils.ObjectParser;
import org.walkersguide.utils.Route;
import org.walkersguide.utils.SettingsManager;

import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;



public class StartFragment extends Fragment {

    public interface MessageFromStartFragmentListener{
        public void switchToOtherFragment(String fragmentName);
    }

    private static final int ROUTEIMPORT = 1;
    private static final int TRANSPORTROUTELOADED = 2;
    private int webserverInterfaceVersion;
    private MessageFromStartFragmentListener mSFListener;
    private Globals globalData;
    private DataDownloader downloader, routeDownloader;
    private SettingsManager settingsManager;
    private PositionManager positionManager;
    private AddressManager addressManager;
    private KeyboardManager keyboardManager;
    private Route sourceRoute;
    private Point currentLocation, locationSinceLastFullUpdate;
    private String locationStatus;
    private RelativeLayout mainLayout;
    private LinearLayout routeLayout;
    private Dialog dialog;
    private Toast messageToast;
    private Handler progressHandler;
    private ProgressUpdater progressUpdater;
    private Vibrator vibrator;

    @Override public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (globalData == null) {
            globalData = ((Globals) getActivity().getApplicationContext());
        }
        settingsManager = globalData.getSettingsManagerInstance();
        positionManager = globalData.getPositionManagerInstance();
        addressManager = globalData.getAddressManagerInstance();
        keyboardManager = globalData.getKeyboardManagerInstance();
        vibrator = (Vibrator) getActivity().getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        progressHandler = new Handler();
        progressUpdater = new ProgressUpdater();

        // check if the system clock is synched
        int autoTime = Settings.System.getInt(getActivity().getContentResolver(), Settings.System.AUTO_TIME, -1);
        if (autoTime == 0) {
            showTimeSettingsDialog();
        }

        // check if the client uses no old server interface
        webserverInterfaceVersion = 0;
        DataDownloader downloader = new DataDownloader(getActivity());
        downloader.setDataDownloadListener(new VersionDownloadListener() );
        downloader.execute( "get",
                globalData.getSettingsManagerInstance().getServerPath(), "get_version");

        // check if we can find a bug report from a previous app crash
        if (getBugReportFile() != null)
            showBugReportDialog();

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mSFListener = (MessageFromStartFragmentListener) ((MainActivity) activity).getMessageFromStartFragmentListener();
        } catch (ClassCastException e) {
             throw new ClassCastException(activity.toString()
                     + " must implement MessageFromStartFragmentListener");
        }
    }

    @Override public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        messageToast = Toast.makeText(getActivity(), "", Toast.LENGTH_SHORT);
        if (globalData == null) {
            globalData = ((Globals) getActivity().getApplicationContext());
        }
        // load last route request from history or create a new one
        if (settingsManager.loadSourceRoutesFromHistory() != null
                && settingsManager.loadSourceRoutesFromHistory().size() > 0) {
            this.sourceRoute = settingsManager.loadSourceRoutesFromHistory().get(0);
        } else {
            this.sourceRoute = new Route();
        }
        settingsManager.setRouteRequest(sourceRoute);
        locationStatus = "";
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_start, container, false);
        mainLayout = (RelativeLayout) view.findViewById(R.id.linearLayoutMain);
        routeLayout = (LinearLayout) view.findViewById(R.id.linearLayoutRoute);

        // Buttons
        Button buttonNewRoute = (Button) mainLayout.findViewById(R.id.buttonNewRoute);
        buttonNewRoute.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                showRouteDialog();
            }
        });

        Button buttonIntermediatePoint = (Button) mainLayout.findViewById(R.id.buttonIntermediatePoint);
        buttonIntermediatePoint.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (sourceRoute.getSize() > 3) {
                    showAddObjectToRouteDialog();
                } else {
                    sourceRoute.addRouteObjectAtIndex(2, new RouteObjectWrapper());
                    sourceRoute.addRouteObjectAtIndex(3, new RouteObjectWrapper(
                            new FootwaySegment(
                                getResources().getString(R.string.labelFootwayPlaceholder),
                                -1, -1, "footway_place_holder")));
                    updateRouteRequest();
                }
            }
        });

        Button buttonStartRouting = (Button) view.findViewById(R.id.buttonStartRouting);
        buttonStartRouting.setTag(0);
        buttonStartRouting.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Button buttonStartRouting= (Button) mainLayout.findViewById(R.id.buttonStartRouting);
                if ((Integer) buttonStartRouting.getTag() == 0) {
                    queryFootwayRoute();
                } else {
                    cancelRouteDownloadProcess();
                }
            }
        });

        updateRouteRequest();
        return view;
    }

    public void queryFootwayRoute() {
        JSONObject requestJson = new JSONObject();
        // check webserver interface version
        if (webserverInterfaceVersion > settingsManager.getInterfaceVersion()) {
            showOldWebserverInterfaceVersionDialog();
            return;
        }
        // check if source route has empty objects and get the number of public transport segments
        int index = 0;
        int numberOfTransportSegments = 0;
        for (RouteObjectWrapper object : sourceRoute.getRouteList()) {
            if (object.isEmpty()) {
                Intent intent = new Intent(getActivity(), DialogActivity.class);
                if (index == 0) {
                    intent.putExtra("message",
                            getResources().getString(R.string.messageEmptyStartPoint));
                } else if (index == sourceRoute.getSize()-1) {
                    intent.putExtra("message",
                            getResources().getString(R.string.messageEmptyDestinationPoint));
                } else {
                    intent.putExtra("message",
                            getResources().getString(R.string.messageEmptyIntermediatePoint));
                }
                startActivity(intent);
                return;
            }
            if (object.getFootwaySegment() != null && object.getFootwaySegment().getSubType().equals("transport_place_holder")) {
                numberOfTransportSegments += 1;
            }
            index += 1;
        }
        // get source route
        JSONArray routeRequestJson = sourceRoute.toJson();
        if (routeRequestJson == null) {
            Intent intent = new Intent(getActivity(), DialogActivity.class);
            intent.putExtra("message",
                    getResources().getString(R.string.messageEmptyJSONObject));
            startActivity(intent);
            return;
        }
        // options
        ArrayList<Integer> blockedWayIds = new ArrayList<Integer>();
        for (FootwaySegment footway : settingsManager.loadBlockedWays())
            blockedWayIds.add(footway.getWayId());
        JSONObject optionsJson = new JSONObject();
        try {
            optionsJson.put("number_of_possible_routes", 3);
            optionsJson.put("route_factor", settingsManager.getRouteFactor());
            optionsJson.put("allowed_way_classes", new JSONArray(settingsManager.getRoutingWayClasses()));
            optionsJson.put("language", Locale.getDefault().getLanguage());
            optionsJson.put("session_id", globalData.getSessionId());
            optionsJson.put("blocked_ways", TextUtils.join(",", blockedWayIds));
        } catch (JSONException e) {
            Intent intent = new Intent(getActivity(), DialogActivity.class);
            intent.putExtra("message",
                    getResources().getString(R.string.messageEmptyJSONObject));
            startActivity(intent);
            return;
        }
        // add all to request object
        try {
            requestJson.put("source_route", routeRequestJson);
            requestJson.put("options", optionsJson);
        } catch (JSONException e) {
            Intent intent = new Intent(getActivity(), DialogActivity.class);
            intent.putExtra("message",
                    getResources().getString(R.string.messageEmptyJSONObject));
            startActivity(intent);
            return;
        }
        if (numberOfTransportSegments > 1) {
            Intent intent = new Intent(getActivity(), DialogActivity.class);
            intent.putExtra("message",
                    getResources().getString(R.string.messageMultipleTransportSegments));
            startActivity(intent);
            return;
        } else if (numberOfTransportSegments == 1) {
            routeDownloader = new DataDownloader(getActivity());
            routeDownloader.setDataDownloadListener(new TransportRouteDownloadListener() );
            routeDownloader.execute( "post",
                    globalData.getSettingsManagerInstance().getServerPath() + "/get_transport_routes",
                    requestJson.toString() );
        } else {
            routeDownloader = new DataDownloader(getActivity());
            routeDownloader.setDataDownloadListener(new RouteDownloadListener() );
            routeDownloader.execute( "post",
                    globalData.getSettingsManagerInstance().getServerPath() + "/get_route",
                    requestJson.toString() );
        }
        Toast.makeText(getActivity(), getResources().getString(R.string.messageRouteComputationStarted),
                Toast.LENGTH_LONG).show();
        progressHandler.postDelayed(progressUpdater, 100);
        // add points to history
        //System.out.println("xx start point type = " + sourceRoute.getRouteObjectAtIndex(0).getPoint().getSubType());
        if (sourceRoute.getRouteObjectAtIndex(0).getIntersection() != null
                || sourceRoute.getRouteObjectAtIndex(0).getPOI() != null
                || sourceRoute.getRouteObjectAtIndex(0).getStation() != null) {
            //System.out.println("xx add start");
            settingsManager.addPointToHistory(sourceRoute.getRouteObjectAtIndex(0));
        }
        //System.out.println("xx startfragment destination = " + sourceRoute.getRouteObjectAtIndex(sourceRoute.getSize()-1).getPoint().getSubType());
        if (sourceRoute.getRouteObjectAtIndex(sourceRoute.getSize()-1).getIntersection() != null
                || sourceRoute.getRouteObjectAtIndex(sourceRoute.getSize()-1).getPOI() != null
                || sourceRoute.getRouteObjectAtIndex(sourceRoute.getSize()-1).getStation() != null) {
            //System.out.println("xx add destination");
            settingsManager.addPointToHistory(sourceRoute.getRouteObjectAtIndex(sourceRoute.getSize()-1));
        }
        Point startPoint = sourceRoute.getRouteObjectAtIndex(0).getPoint();
        if (startPoint.getName().startsWith(getResources().getString(R.string.locationNameCurrentPosition))) {
            startPoint.setName(String.format("%.4f, %.4f", startPoint.getLatitude(), startPoint.getLongitude()));
            sourceRoute.replaceRouteObjectAtIndex(0, new RouteObjectWrapper(startPoint));
        }
        Point destinationPoint = sourceRoute.getRouteObjectAtIndex(sourceRoute.getSize()-1).getPoint();
        if (destinationPoint.getName().startsWith(getResources().getString(R.string.locationNameCurrentPosition))) {
            destinationPoint.setName(String.format("%.4f, %.4f", destinationPoint.getLatitude(), destinationPoint.getLongitude()));
            sourceRoute.replaceRouteObjectAtIndex(sourceRoute.getSize()-1, new RouteObjectWrapper(destinationPoint));
        }
        settingsManager.addSourceRouteToHistory(sourceRoute);
        // button
        Button buttonStartRouting= (Button) mainLayout.findViewById(R.id.buttonStartRouting);
        buttonStartRouting.setTag(1);
        buttonStartRouting.setText(getResources().getString(R.string.buttonStartRoutingClicked));
    }

    private void cancelRouteDownloadProcess() {
        routeDownloader.cancelDownloadProcess();
        JSONObject requestJson = new JSONObject();
        try {
            requestJson.put("language", Locale.getDefault().getLanguage());
            requestJson.put("session_id", globalData.getSessionId());
        } catch (JSONException e) {
            return;
        }
        routeDownloader = new DataDownloader(getActivity());
        routeDownloader.setDataDownloadListener(new CanceledRequestDownloadListener() );
        routeDownloader.execute( "post",
                globalData.getSettingsManagerInstance().getServerPath() + "/cancel_request",
                requestJson.toString() );
    }

    public void updateRouteRequest() {
        routeLayout.removeAllViews();
        LayoutParams lp = new LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT );
        int index = 0;
        int distance = 0;
        for (RouteObjectWrapper object : sourceRoute.getRouteList()) {
            if (currentLocation != null && object.getPoint() != null) {
                object.getPoint().addDistance(currentLocation.distanceTo(object.getPoint()));
            }
            Button buttonRouteObject = new Button(getActivity());
            buttonRouteObject.setLayoutParams(lp);
            buttonRouteObject.setId(index);
            if (index == 0) {
                if (settingsManager.getValueFromTemporaryStartFragmentSettings("currentPositionActive") != null
                        && settingsManager.getValueFromTemporaryStartFragmentSettings("currentPositionActive") == 1
                        && ! object.getPoint().getName().startsWith(getResources().getString(R.string.locationNameCurrentPosition)) ) {
                    buttonRouteObject.setText( String.format(
                            getResources().getString(R.string.buttonRouteObjectStartPoint), index+1,
                            getResources().getString(R.string.locationNameCurrentPosition) + ": " + object.toString()) );
                } else {
                    buttonRouteObject.setText( String.format(
                            getResources().getString(R.string.buttonRouteObjectStartPoint),
                            index+1, object.toString()) );
                }
            } else if (index == sourceRoute.getSize()-1) {
                if (settingsManager.getValueFromTemporaryStartFragmentSettings("currentPositionActive") != null
                        && settingsManager.getValueFromTemporaryStartFragmentSettings("currentPositionActive") == 2
                        && ! object.getPoint().getName().startsWith(getResources().getString(R.string.locationNameCurrentPosition)) ) {
                    buttonRouteObject.setText( String.format(
                            getResources().getString(R.string.buttonRouteObjectDestinationPoint), sourceRoute.getSize(),
                            getResources().getString(R.string.locationNameCurrentPosition) + ": " + object.toString()) );
                } else {
                    buttonRouteObject.setText( String.format(
                            getResources().getString(R.string.buttonRouteObjectDestinationPoint),
                            sourceRoute.getSize(), object.toString()) );
                }
            } else if (index % 2 == 0) {
                if (sourceRoute.getSize() == 5) {
                    buttonRouteObject.setText( String.format(
                            getResources().getString(R.string.buttonRouteObjectIntermediatePoint),
                            index+1, object.toString()) );
                } else {
                    buttonRouteObject.setText( String.format(
                            getResources().getString(R.string.buttonRouteObjectMultiIntermediatePoint),
                            index+1, (int) index/2, object.toString()) );
                }
            } else {
                if (object.getFootwaySegment().getSubType().equals("transport_place_holder"))
                    buttonRouteObject.setText( String.format(
                            getResources().getString(R.string.buttonRouteObjectTransportSegment), index+1) );
                else
                    buttonRouteObject.setText( String.format(
                            getResources().getString(R.string.buttonRouteObjectFootwaySegment), index+1) );
            }
            buttonRouteObject.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    int index = view.getId();
                    if (index % 2 == 0) {
                        showRouteObjectDialog(index);
                    } else {
                        if (sourceRoute.getRouteObjectAtIndex(index).getFootwaySegment().getSubType().equals("transport_place_holder")) {
                            sourceRoute.replaceRouteObjectAtIndex(index, new RouteObjectWrapper(
                                    new FootwaySegment(
                                        getResources().getString(R.string.labelFootwayPlaceholder),
                                        -1, -1, "footway_place_holder")));
                        } else {
                            sourceRoute.replaceRouteObjectAtIndex(index, new RouteObjectWrapper(
                                    new FootwaySegment(
                                        getResources().getString(R.string.labelTransportPlaceholder),
                                        -1, -1, "transport_place_holder")));
                        }
                    }
                    updateRouteRequest();
                }
            });
            routeLayout.addView(buttonRouteObject);
            index += 1;
        }
    }

    public void updateCurrentLocationObjectOnly() {
        int routeIndex = -1;
        String buttonText = "";
        RouteObjectWrapper object;
        if (settingsManager.getValueFromTemporaryStartFragmentSettings("currentPositionActive") == 1) {
            routeIndex = 0;
            object = sourceRoute.getRouteObjectAtIndex(routeIndex);
            if (! object.getPoint().getName().startsWith(getResources().getString(R.string.locationNameCurrentPosition)) ) {
                buttonText = String.format( getResources().getString(R.string.buttonRouteObjectStartPoint), routeIndex+1,
                        getResources().getString(R.string.locationNameCurrentPosition) + ": " + object.toString());
            } else {
                buttonText = String.format( getResources().getString(R.string.buttonRouteObjectStartPoint),
                            routeIndex+1, object.toString());
            }
        } else if (settingsManager.getValueFromTemporaryStartFragmentSettings("currentPositionActive") == 2) {
            routeIndex = sourceRoute.getSize()-1;
            object = sourceRoute.getRouteObjectAtIndex(routeIndex);
            if (! object.getPoint().getName().startsWith(getResources().getString(R.string.locationNameCurrentPosition)) ) {
                buttonText = String.format( getResources().getString(R.string.buttonRouteObjectDestinationPoint),
                        sourceRoute.getSize(), getResources().getString(R.string.locationNameCurrentPosition) + ": " + object.toString());
            } else {
                buttonText = String.format( getResources().getString(R.string.buttonRouteObjectDestinationPoint),
                            sourceRoute.getSize(), object.toString());
            }
        }
        Button buttonRouteObject = (Button) routeLayout.findViewById(routeIndex);
        if (buttonRouteObject != null) {
            buttonRouteObject.setText(buttonText);
        }
    }

    public void showTimeSettingsDialog() {
        dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.dialog_scrollable_empty);
        dialog.setCanceledOnTouchOutside(false);
        LinearLayout dialogLayout = (LinearLayout) dialog.findViewById(R.id.linearLayoutMain);
        LayoutParams lp = new LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT );
        // heading and dialog text
        TextView labelHeading = new TextView(getActivity());
        labelHeading.setLayoutParams(lp);
        labelHeading.setText(getResources().getString(R.string.labelTimeSettingsDialogHeading));
        dialogLayout.addView(labelHeading);
        TextView labelDescription = new TextView(getActivity());
        labelDescription.setLayoutParams(lp);
        labelDescription.setText(getResources().getString(R.string.labelTimeSettingsDialogDescription));
        dialogLayout.addView(labelDescription);

        // buttons
        Button buttonShowTimeSettings = new Button(getActivity());
        buttonShowTimeSettings.setLayoutParams(lp);
        buttonShowTimeSettings.setText(getResources().getString(R.string.buttonShowTimeSettings));
        buttonShowTimeSettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(Settings.ACTION_DATE_SETTINGS);
                startActivity(intent);
                dialog.dismiss();
            }
        });
        dialogLayout.addView(buttonShowTimeSettings);
        Button buttonCancel = new Button(getActivity());
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

    public void showBugReportDialog() {
        dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.dialog_scrollable_empty);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnKeyListener(new OnKeyListener() {
            @Override public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    if (getBugReportFile() != null) {
                        File file = new File(getBugReportFile());
                        file.delete();
                    }
                    dialog.dismiss();
                }
                return true;
            }
        });
        LinearLayout dialogLayout = (LinearLayout) dialog.findViewById(R.id.linearLayoutMain);
        LayoutParams lp = new LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT );
        // heading and dialog text
        TextView labelHeading = new TextView(getActivity());
        labelHeading.setLayoutParams(lp);
        labelHeading.setText(getResources().getString(R.string.labelBugReportHeading));
        dialogLayout.addView(labelHeading);
        TextView labelDescription = new TextView(getActivity());
        labelDescription.setLayoutParams(lp);
        labelDescription.setText(getResources().getString(R.string.labelBugReportDescription));
        dialogLayout.addView(labelDescription);

        // buttons
        Button buttonShowBugReportDetails = new Button(getActivity());
        buttonShowBugReportDetails.setLayoutParams(lp);
        buttonShowBugReportDetails.setText(getResources().getString(R.string.buttonShowBugReportDetails));
        buttonShowBugReportDetails.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), DialogActivity.class);
                intent.putExtra("message", String.format(
                        getResources().getString(R.string.messageBugReport),
                        getBugReportContents(getBugReportFile()) ));
                startActivity(intent);
            }
        });
        dialogLayout.addView(buttonShowBugReportDetails);
        Button buttonSendBugReport = new Button(getActivity());
        buttonSendBugReport.setLayoutParams(lp);
        buttonSendBugReport.setText(getResources().getString(R.string.buttonSendBugReport));
        buttonSendBugReport.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                String bugReportFileName = getBugReportFile();
                if (bugReportFileName != null) {
                    File file = new File(bugReportFileName);
                    // send to server
                    JSONObject requestJson = new JSONObject();
                    try {
                        requestJson.put("file_name", bugReportFileName);
                        requestJson.put("bug_report", getBugReportContents(bugReportFileName));
                        requestJson.put("language", Locale.getDefault().getLanguage());
                    } catch (JSONException e) {
                        file.delete();
                        dialog.dismiss();
                        return;
                    }
                    DataDownloader downloader = new DataDownloader(getActivity());
                    downloader.setDataDownloadListener(new BugReportDownloadListener() );
                    downloader.execute( "post",
                            globalData.getSettingsManagerInstance().getServerPath() + "/get_bug_report",
                            requestJson.toString() );
                    file.delete();
                }
                dialog.dismiss();
            }
        });
        dialogLayout.addView(buttonSendBugReport);
        Button buttonDeleteBugReport = new Button(getActivity());
        buttonDeleteBugReport.setLayoutParams(lp);
        buttonDeleteBugReport.setText(getResources().getString(R.string.buttonDeleteBugReport));
        buttonDeleteBugReport.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (getBugReportFile() != null) {
                    File file = new File(getBugReportFile());
                    file.delete();
                }
                dialog.dismiss();
            }
        });
        dialogLayout.addView(buttonDeleteBugReport);
        dialog.show();
    }

    public void showRouteObjectDialog(int objectIndex) {
        RouteObjectWrapper object = sourceRoute.getRouteObjectAtIndex(objectIndex);
        TextView label;
        dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.dialog_scrollable_empty);
        dialog.setCanceledOnTouchOutside(false);
        LinearLayout dialogLayout = (LinearLayout) dialog.findViewById(R.id.linearLayoutMain);
        LayoutParams lp = new LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT );
        // heading
        label = new TextView(getActivity());
        label.setLayoutParams(lp);
        if (objectIndex == 0) {
            if (object.isEmpty()) {
                label.setText(getResources().getString(R.string.labelRODialogEmptyStartPoint));
            } else {
                label.setText( String.format(
                        getResources().getString(R.string.labelRODialogStartPoint),
                        object.getPoint().getName()) );
            }
        } else if (objectIndex == sourceRoute.getSize()-1) {
            if (object.isEmpty()) {
                label.setText(getResources().getString(R.string.labelRODialogEmptyDestinationPoint));
            } else {
                label.setText( String.format(
                        getResources().getString(R.string.labelRODialogDestinationPoint),
                        object.getPoint().getName()) );
            }
        } else {
            if (sourceRoute.getSize() == 5) {
                if (object.isEmpty()) {
                    label.setText(getResources().getString(R.string.labelRODialogEmptyIntermediatePoint));
                } else {
                    label.setText( String.format(
                            getResources().getString(R.string.labelRODialogIntermediatePoint),
                            object.getPoint().getName()) );
                }
            } else {
                if (object.isEmpty()) {
                    label.setText( String.format(
                            getResources().getString(R.string.labelRODialogEmptyMultiIntermediatePoint),
                            (objectIndex/2)) );
                } else {
                    label.setText( String.format(
                            getResources().getString(R.string.labelRODialogMultiIntermediatePoint),
                            (objectIndex/2), object.getPoint().getName()) );
                }
            }
        }
        dialogLayout.addView(label);

        // delete via route point
        if (objectIndex > 0 && objectIndex < sourceRoute.getRouteList().size()-1) {
            Button buttonDeleteIntermediatePoint = new Button(getActivity());
            buttonDeleteIntermediatePoint.setLayoutParams(lp);
            buttonDeleteIntermediatePoint.setId(objectIndex);
            buttonDeleteIntermediatePoint.setText(getResources().getString(R.string.buttonDeleteIntermediatePoint));
            buttonDeleteIntermediatePoint.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    sourceRoute.removeRouteObjectAtIndex(view.getId());
                    sourceRoute.removeRouteObjectAtIndex(view.getId());
                    updateRouteRequest();
                    dialog.dismiss();
                }
            });
            dialogLayout.addView(buttonDeleteIntermediatePoint);
        }

        if(currentLocation != null && ! object.isEmpty()) {
            // route object details
            Button buttonObjectDetails = new Button(getActivity());
            buttonObjectDetails.setLayoutParams(lp);
            buttonObjectDetails.setId(objectIndex);
            buttonObjectDetails.setText(getResources().getString(R.string.buttonRouteObjectDetails));
            buttonObjectDetails.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    Intent intent = new Intent(getActivity(), RouteObjectDetailsActivity.class);
                    intent.putExtra("route_object",
                        sourceRoute.getRouteObjectAtIndex(view.getId()).toJson().toString());
                    startActivity(intent);
                    dialog.dismiss();
                }
            });
            dialogLayout.addView(buttonObjectDetails);

            // simulation
            Button buttonSimulation = new Button(getActivity());
            buttonSimulation.setLayoutParams(lp);
            buttonSimulation.setId(objectIndex);
            if ((currentLocation.distanceTo(object.getPoint()) == 0) && (positionManager.getStatus() == PositionManager.Status.SIMULATION)) {
                buttonSimulation.setText(getResources().getString(R.string.buttonStopSimulation));
            } else {
                buttonSimulation.setText(getResources().getString(R.string.buttonPointSimulation));
            }
            buttonSimulation.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    RouteObjectWrapper object = sourceRoute.getRouteObjectAtIndex(view.getId());
                    if ((currentLocation.distanceTo(object.getPoint()) == 0) && (positionManager.getStatus() == PositionManager.Status.SIMULATION)) {
                        positionManager.changeStatus(PositionManager.Status.GPS, null);
                        messageToast.setText(
                            getResources().getString(R.string.messageSimulationStopped));
                    } else {
                        positionManager.changeStatus(PositionManager.Status.SIMULATION, object.getPoint());
                        messageToast.setText(
                            getResources().getString(R.string.messagePositionSimulated));
                    }
                    messageToast.show();
                    dialog.dismiss();
                }
            });
            dialogLayout.addView(buttonSimulation);

            // to favorites
            Button buttonToFavorites = new Button(getActivity());
            buttonToFavorites.setLayoutParams(lp);
            buttonToFavorites.setId(objectIndex);
            if (settingsManager.favoritesContains(object)) {
                buttonToFavorites.setText(getResources().getString(R.string.buttonToFavoritesClicked));
            } else {
                buttonToFavorites.setText(getResources().getString(R.string.buttonToFavorites));
            }
            buttonToFavorites.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    RouteObjectWrapper object = sourceRoute.getRouteObjectAtIndex(view.getId());
                    if (settingsManager.favoritesContains(object)) {
                        settingsManager.removePointFromFavorites(object);
                        messageToast.setText(
                            getResources().getString(R.string.messageRemovedFromFavorites));
                    } else {
                        settingsManager.addPointToFavorites(object);
                        messageToast.setText(
                            getResources().getString(R.string.messageAddedToFavorites));
                    }
                    messageToast.show();
                    dialog.dismiss();
                }
            });
            dialogLayout.addView(buttonToFavorites);
        }

        // new object
        label = new TextView(getActivity());
        label.setLayoutParams(lp);
        label.setText(getResources().getString(R.string.labelRODialogNewObject));
        dialogLayout.addView(label);

        // current position
        if (objectIndex == 0) {
            Button buttonCurrentLocation = new Button(getActivity());
            buttonCurrentLocation.setLayoutParams(lp);
            buttonCurrentLocation.setText(getResources().getString(R.string.buttonCurrentLocation));
            buttonCurrentLocation.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    settingsManager.addToTemporaryStartFragmentSettings("currentPositionActive", 1);
                    positionManager.getLastKnownLocation();
                    dialog.dismiss();
                }
            });
            dialogLayout.addView(buttonCurrentLocation);
        }

        // address button
        Button buttonAddress = new Button(getActivity());
        buttonAddress.setLayoutParams(lp);
        buttonAddress.setId(objectIndex);
        buttonAddress.setText(getResources().getString(R.string.buttonAddress));
        buttonAddress.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent activityEnterAddress = new Intent(getActivity(), EnterAddressActivity.class);
                activityEnterAddress.putExtra("objectIndex", view.getId());
                startActivity(activityEnterAddress);
                dialog.dismiss();
            }
        });
        dialogLayout.addView(buttonAddress);

        // poi
        Button buttonPOI = new Button(getActivity());
        buttonPOI.setLayoutParams(lp);
        buttonPOI.setId(objectIndex);
        buttonPOI.setText(getResources().getString(R.string.buttonPOI));
        buttonPOI.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                settingsManager.addToTemporaryPOIFragmentSettings("routeRequestPosition", view.getId());
                mSFListener.switchToOtherFragment("poi");
                dialog.dismiss();
            }
        });
        dialogLayout.addView(buttonPOI);

        // history
        Button buttonHistory = new Button(getActivity());
        buttonHistory.setLayoutParams(lp);
        buttonHistory.setId(objectIndex);
        buttonHistory.setText(getResources().getString(R.string.buttonHistory));
        buttonHistory.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent activityEnterHistory = new Intent(getActivity(), HistoryActivity.class);
                activityEnterHistory.putExtra("subView", "ROUTEPOINTS");
                activityEnterHistory.putExtra("showButtons", 0);
                activityEnterHistory.putExtra("objectIndex", view.getId());
                startActivity(activityEnterHistory);
                dialog.dismiss();
            }
        });
        dialogLayout.addView(buttonHistory);

        // favorites
        Button buttonFavorites = new Button(getActivity());
        buttonFavorites.setLayoutParams(lp);
        buttonFavorites.setId(objectIndex);
        buttonFavorites.setText(getResources().getString(R.string.buttonFavorites));
        buttonFavorites.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent activityEnterHistory = new Intent(getActivity(), HistoryActivity.class);
                activityEnterHistory.putExtra("subView", "FAVORITEPOINTS");
                activityEnterHistory.putExtra("showButtons", 0);
                activityEnterHistory.putExtra("objectIndex", view.getId());
                startActivity(activityEnterHistory);
                dialog.dismiss();
            }
        });
        dialogLayout.addView(buttonFavorites);

        Button buttonCancel = new Button(getActivity());
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

    public void showAddObjectToRouteDialog() {
        dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.dialog_scrollable_empty);
        dialog.setCanceledOnTouchOutside(false);
        LinearLayout dialogLayout = (LinearLayout) dialog.findViewById(R.id.linearLayoutMain);
        LayoutParams lp = new LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT );
        // heading
        TextView label = new TextView(getActivity());
        label.setLayoutParams(lp);
        label.setText(getResources().getString(R.string.labelAOTRDialogHeading));
        dialogLayout.addView(label);

        int index = 0;
        for (RouteObjectWrapper object : sourceRoute.getRouteList()) {
            if (index == 0 || index % 2 == 1) {
                index += 1;
                continue;
            }
            Button buttonAddObject = new Button(getActivity());
            buttonAddObject.setLayoutParams(lp);
            buttonAddObject.setId(index);
            if (index == sourceRoute.getSize()-1) {
                buttonAddObject.setText(getResources().getString(R.string.buttonAddObjectBeforeDestination));
            } else if (sourceRoute.getSize() == 5) {
                buttonAddObject.setText(getResources().getString(R.string.buttonAddObjectBeforeIntermediate));
            } else {
                buttonAddObject.setText( String.format(
                        getResources().getString(R.string.buttonAddObjectBeforeNIntermediate), (index/2)) );
            }
            buttonAddObject.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    int index = view.getId();
                    sourceRoute.addRouteObjectAtIndex(index, new RouteObjectWrapper());
                    sourceRoute.addRouteObjectAtIndex(index+1, new RouteObjectWrapper(
                            new FootwaySegment(
                                getResources().getString(R.string.labelFootwayPlaceholder),
                                -1, -1, "footway_place_holder")));
                    updateRouteRequest();
                    dialog.dismiss();
                }
            });
            dialogLayout.addView(buttonAddObject);
            index += 1;
        }

        Button buttonCancel = new Button(getActivity());
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

    public void showRouteDialog() {
        dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.dialog_scrollable_with_buttons);
        dialog.setCanceledOnTouchOutside(false);
        LinearLayout dialogTopLayout = (LinearLayout) dialog.findViewById(R.id.linearLayoutTop);
        LinearLayout dialogLayout = (LinearLayout) dialog.findViewById(R.id.linearLayoutMain);
        LayoutParams lp = new LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT );
        LayoutParams lpMarginTop = new LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT );
        ((MarginLayoutParams) lpMarginTop).topMargin =
                Math.round(15 * getActivity().getResources().getDisplayMetrics().density);

        // new route
        TextView label = new TextView(getActivity());
        label.setLayoutParams(lp);
        label.setText(getResources().getString(R.string.labelRouteDialogNewRoute));
        dialogLayout.addView(label);

        Button buttonNewEmptyRoute = new Button(getActivity());
        buttonNewEmptyRoute.setLayoutParams(lp);
        buttonNewEmptyRoute.setText(getResources().getString(R.string.buttonNewEmptyRoute));
        buttonNewEmptyRoute.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                settingsManager.addToTemporaryStartFragmentSettings("currentPositionActive", 0);
                sourceRoute = new Route();
                updateRouteRequest();
                dialog.dismiss();
            }
        });
        dialogLayout.addView(buttonNewEmptyRoute);

        Button buttonNewRouteFromHistory = new Button(getActivity());
        buttonNewRouteFromHistory.setLayoutParams(lp);
        buttonNewRouteFromHistory.setText(getResources().getString(R.string.buttonNewRouteFromHistory));
        buttonNewRouteFromHistory.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent activityEnterHistory = new Intent(getActivity(), HistoryActivity.class);
                activityEnterHistory.putExtra("subView", "ROUTES");
                activityEnterHistory.putExtra("showButtons", 0);
                activityEnterHistory.putExtra("route", 1);
                startActivity(activityEnterHistory);
                dialog.dismiss();
            }
        });
        dialogLayout.addView(buttonNewRouteFromHistory);

        Button buttonNewRouteFromFileImport = new Button(getActivity());
        buttonNewRouteFromFileImport.setLayoutParams(lp);
        buttonNewRouteFromFileImport.setText(getResources().getString(R.string.buttonNewRouteFromFileImport));
        buttonNewRouteFromFileImport.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), RouteImportActivity.class);
                startActivityForResult(intent, ROUTEIMPORT);
                dialog.dismiss();
            }
        });
        dialogLayout.addView(buttonNewRouteFromFileImport);

        // additional actions heading
        label = new TextView(getActivity());
        label.setLayoutParams(lpMarginTop);
        label.setText(getResources().getString(R.string.labelRouteDialogAdditionalActions));
        dialogLayout.addView(label);

        // switch start and destination
        Button buttonSwitchStartAndDestination = new Button(getActivity());
        buttonSwitchStartAndDestination.setLayoutParams(lp);
        buttonSwitchStartAndDestination.setText(getResources().getString(R.string.buttonSwitchStartAndDestination));
        buttonSwitchStartAndDestination.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (settingsManager.getValueFromTemporaryStartFragmentSettings("currentPositionActive") != null) {
                    if (settingsManager.getValueFromTemporaryStartFragmentSettings("currentPositionActive") == 1) {
                        settingsManager.addToTemporaryStartFragmentSettings("currentPositionActive", 2);
                    } else if (settingsManager.getValueFromTemporaryStartFragmentSettings("currentPositionActive") == 2) {
                        settingsManager.addToTemporaryStartFragmentSettings("currentPositionActive", 1);
                    }
                }
                RouteObjectWrapper tmp = sourceRoute.getRouteObjectAtIndex(0);
                sourceRoute.replaceRouteObjectAtIndex(0,
                        sourceRoute.getRouteObjectAtIndex(sourceRoute.getSize()-1));
                sourceRoute.replaceRouteObjectAtIndex(sourceRoute.getSize()-1, tmp);
                updateRouteRequest();
                dialog.dismiss();
            }
        });
        dialogLayout.addView(buttonSwitchStartAndDestination);

        Button buttonOpenBlockedWays = new Button(getActivity());
        buttonOpenBlockedWays.setLayoutParams(lp);
        buttonOpenBlockedWays.setText(getResources().getString(R.string.buttonOpenBlockedWays));
        buttonOpenBlockedWays.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent activityEnterHistory = new Intent(getActivity(), HistoryActivity.class);
                activityEnterHistory.putExtra("subView", "BLOCKEDWAYS");
                activityEnterHistory.putExtra("showButtons", 0);
                startActivity(activityEnterHistory);
                dialog.dismiss();
            }
        });
        dialogLayout.addView(buttonOpenBlockedWays);

        Button buttonExportRouteToFile = new Button(getActivity());
        buttonExportRouteToFile.setLayoutParams(lp);
        buttonExportRouteToFile.setText(getResources().getString(R.string.buttonExportRouteToFile));
        buttonExportRouteToFile.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                String fileName = String.format("%s/%s.--.%s.route",
                    settingsManager.getProgramImportFolder(),
                    sourceRoute.getRouteObjectAtIndex(0).getPoint().getName().replace(" ","."),
                    sourceRoute.getRouteObjectAtIndex(sourceRoute.getSize()-1).getPoint().getName().replace(" ",".") );
                try {
                    Writer fw = new FileWriter(fileName, false);
                    fw.write(sourceRoute.toJson().toString());
                    fw.close();
                } catch (IOException e) {
                    Intent intent = new Intent(getActivity(), DialogActivity.class);
                    intent.putExtra("message",
                            getResources().getString(R.string.messageRouteExportError) );
                    startActivity(intent);
                    dialog.dismiss();
                    return;
                }
                Intent intent = new Intent(getActivity(), DialogActivity.class);
                intent.putExtra("message",
                        getResources().getString(R.string.messageRouteExportSuccessful) );
                startActivity(intent);
                dialog.dismiss();
            }
        });
        dialogLayout.addView(buttonExportRouteToFile);

        // route factor
        label = new TextView(getActivity());
        label.setLayoutParams(lpMarginTop);
        label.setText(getResources().getString(R.string.labelSettingsActivityRouteFactor));
        dialogLayout.addView(label);
        Spinner spinnerRouteFactor = new Spinner(getActivity());
        spinnerRouteFactor.setId(0);
        ArrayAdapter<Double> adapter = new ArrayAdapter<Double>(getActivity(),
                android.R.layout.simple_spinner_item, settingsManager.getRouteFactorArray());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRouteFactor.setAdapter(adapter);
        // select choosen route factor
        int index = adapter.getPosition(settingsManager.getRouteFactor());
        if (index == -1) {
            index = adapter.getPosition(settingsManager.getDefaultRouteFactor());
            if (index == -1) {
                index = 0;
            }
        }
        spinnerRouteFactor.setSelection(index);
        dialogLayout.addView(spinnerRouteFactor);

        // way class checkboxes
        label = new TextView(getActivity());
        label.setLayoutParams(lpMarginTop);
        label.setText(getResources().getString(R.string.labelRouteDialogWayClasses));
        dialogLayout.addView(label);
        ArrayList<String> routingWayClasses = settingsManager.getRoutingWayClasses();
        CheckBox checkBoxBigStreets = new CheckBox(getActivity());
        checkBoxBigStreets.setId(1);
        checkBoxBigStreets.setLayoutParams(lp);
        checkBoxBigStreets.setText(getResources().getString(R.string.checkBoxBigStreets));
        if (routingWayClasses.contains("big_streets"))
            checkBoxBigStreets.setChecked(true);
        else
            checkBoxBigStreets.setChecked(false);
        dialogLayout.addView(checkBoxBigStreets);
        CheckBox checkBoxSmallStreets = new CheckBox(getActivity());
        checkBoxSmallStreets.setId(2);
        checkBoxSmallStreets.setLayoutParams(lp);
        checkBoxSmallStreets.setText(getResources().getString(R.string.checkBoxSmallStreets));
        if (routingWayClasses.contains("small_streets"))
            checkBoxSmallStreets.setChecked(true);
        else
            checkBoxSmallStreets.setChecked(false);
        dialogLayout.addView(checkBoxSmallStreets);
        CheckBox checkBoxPavedWays = new CheckBox(getActivity());
        checkBoxPavedWays.setId(3);
        checkBoxPavedWays.setLayoutParams(lp);
        checkBoxPavedWays.setText(getResources().getString(R.string.checkBoxPavedWays));
        if (routingWayClasses.contains("paved_ways"))
            checkBoxPavedWays.setChecked(true);
        else
            checkBoxPavedWays.setChecked(false);
        dialogLayout.addView(checkBoxPavedWays);
        CheckBox checkBoxUnpavedWays = new CheckBox(getActivity());
        checkBoxUnpavedWays.setId(4);
        checkBoxUnpavedWays.setLayoutParams(lp);
        checkBoxUnpavedWays.setText(getResources().getString(R.string.checkBoxUnpavedWays));
        if (routingWayClasses.contains("unpaved_ways"))
            checkBoxUnpavedWays.setChecked(true);
        else
            checkBoxUnpavedWays.setChecked(false);
        dialogLayout.addView(checkBoxUnpavedWays);
        CheckBox checkBoxUnclassifiedWays = new CheckBox(getActivity());
        checkBoxUnclassifiedWays.setId(5);
        checkBoxUnclassifiedWays.setLayoutParams(lp);
        checkBoxUnclassifiedWays.setText(getResources().getString(R.string.checkBoxUnclassifiedWays));
        if (routingWayClasses.contains("unclassified_ways"))
            checkBoxUnclassifiedWays.setChecked(true);
        else
            checkBoxUnclassifiedWays.setChecked(false);
        dialogLayout.addView(checkBoxUnclassifiedWays);
        CheckBox checkBoxSteps = new CheckBox(getActivity());
        checkBoxSteps.setId(6);
        checkBoxSteps.setLayoutParams(lp);
        checkBoxSteps.setText(getResources().getString(R.string.checkBoxSteps));
        if (routingWayClasses.contains("steps"))
            checkBoxSteps.setChecked(true);
        else
            checkBoxSteps.setChecked(false);
        dialogLayout.addView(checkBoxSteps);

        // ok
        Button buttonOK = (Button) dialogTopLayout.findViewById(R.id.buttonOK);
        buttonOK.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                LinearLayout dialogLayout = (LinearLayout) dialog.findViewById(R.id.linearLayoutMain);
                // indirection factor
                Spinner spinnerRouteFactor = (Spinner) dialogLayout.findViewById(0);
                double routeFactor = (Double) spinnerRouteFactor.getSelectedItem();
                settingsManager.setRouteFactor(routeFactor);
                // way classes
                ArrayList<String> routingWayClasses = new ArrayList<String>();
                CheckBox checkBoxBigStreets = (CheckBox) dialogLayout.findViewById(1);
                if (checkBoxBigStreets.isChecked())
                    routingWayClasses.add("big_streets");
                CheckBox checkBoxSmallStreets = (CheckBox) dialogLayout.findViewById(2);
                if (checkBoxSmallStreets.isChecked())
                    routingWayClasses.add("small_streets");
                CheckBox checkBoxPavedWays = (CheckBox) dialogLayout.findViewById(3);
                if (checkBoxPavedWays.isChecked())
                    routingWayClasses.add("paved_ways");
                CheckBox checkBoxUnpavedWays = (CheckBox) dialogLayout.findViewById(4);
                if (checkBoxUnpavedWays.isChecked())
                    routingWayClasses.add("unpaved_ways");
                CheckBox checkBoxUnclassifiedWays = (CheckBox) dialogLayout.findViewById(5);
                if (checkBoxUnclassifiedWays.isChecked())
                    routingWayClasses.add("unclassified_ways");
                CheckBox checkBoxSteps = (CheckBox) dialogLayout.findViewById(6);
                if (checkBoxSteps.isChecked())
                    routingWayClasses.add("steps");
                settingsManager.setRoutingWayClasses(routingWayClasses);
                dialog.dismiss();
            }
        });

        // cancel
        Button buttonCancel = (Button) dialogTopLayout.findViewById(R.id.buttonCancel);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    public void showOldWebserverInterfaceVersionDialog() {
        dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.dialog_scrollable_empty);
        dialog.setCanceledOnTouchOutside(false);
        LinearLayout dialogLayout = (LinearLayout) dialog.findViewById(R.id.linearLayoutMain);
        LayoutParams lp = new LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT );
        // heading
        TextView label = new TextView(getActivity());
        label.setLayoutParams(lp);
        label.setText(getResources().getString(R.string.messageVersionRequestAppTooOld));
        dialogLayout.addView(label);
        Button buttonDownloadNewAppVersion = new Button(getActivity());
        buttonDownloadNewAppVersion.setLayoutParams(lp);
        buttonDownloadNewAppVersion.setText(getResources().getString(R.string.buttonDownloadNewAppVersion));
        buttonDownloadNewAppVersion.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                String downloadURL = "http://walkersguide.org";
                if (Locale.getDefault().getLanguage().equals("de"))
                    downloadURL += "/de/downloads/";
                else
                    downloadURL += "/en/downloads/";
                startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(downloadURL)));
                dialog.dismiss();
            }
        });
        dialogLayout.addView(buttonDownloadNewAppVersion);
        Button buttonCancel = new Button(getActivity());
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

    @Override public void onPause() {
        super.onPause();
        positionManager.setPositionListener(null);
        progressHandler.removeCallbacks(progressUpdater);
        settingsManager.setRouteRequest(sourceRoute);
        if (routeDownloader != null) {
            cancelRouteDownloadProcess();
        }
    }

    @Override public void onResume() {
        super.onResume();	
        settingsManager = globalData.getSettingsManagerInstance();
        // check if the current position is still active
        sourceRoute = settingsManager.getRouteRequest();
        if (settingsManager.getValueFromTemporaryStartFragmentSettings("currentPositionActive") != null) {
            //System.out.println("xx flag = " + settingsManager.getValueFromTemporaryStartFragmentSettings("currentPositionActive"));
            //System.out.println("xx old = " + currentLocation.getName() + ",  " + currentLocation.getLatitude() + "   " + currentLocation.getLongitude());
            //System.out.println("xx new = " + sourceRoute.getRouteObjectAtIndex(0).getPoint().getName() + ",  " + sourceRoute.getRouteObjectAtIndex(0).getPoint().getLatitude() + "    " + sourceRoute.getRouteObjectAtIndex(0).getPoint().getLongitude());
            //System.out.println("xx is equal = " + sourceRoute.getRouteObjectAtIndex(0).getPoint().equals(currentLocation));
            if (settingsManager.getValueFromTemporaryStartFragmentSettings("currentPositionActive") == 1
                    && ! sourceRoute.getRouteObjectAtIndex(0).getPoint().equals(currentLocation) ) {
                settingsManager.addToTemporaryStartFragmentSettings("currentPositionActive", 0);
            } else if (settingsManager.getValueFromTemporaryStartFragmentSettings("currentPositionActive") == 2
                    && ! sourceRoute.getRouteObjectAtIndex(sourceRoute.getSize()-1).getPoint().equals(currentLocation) ) {
                settingsManager.addToTemporaryStartFragmentSettings("currentPositionActive", 0);
            }
        }
        System.out.println("xx flag = " + settingsManager.getValueFromTemporaryStartFragmentSettings("currentPositionActive"));
        addressManager.setAddressListener(new MyAddressListener());
        keyboardManager.setKeyboardListener(null);
        positionManager.setPositionListener(new MyPositionListener());
        updateRouteRequest();
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data){
        // See which child activity is calling us back.
        switch (requestCode) {
            case ROUTEIMPORT:
            case TRANSPORTROUTELOADED:
                // This is the standard resultCode that is sent back if the
                // activity crashed or didn't doesn't supply an explicit result.
                if (resultCode == getActivity().RESULT_CANCELED) {
                    break;
                } else {
                    if (data.getStringExtra("fragment").equals("router"))
                        mSFListener.switchToOtherFragment("router");
                    break;
                }
            default:
                break;
        }
    }

    public String getBugReportFile() {
        File logFolder = new File(settingsManager.getProgramLogFolder());
        File[] bugReports = logFolder.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".log");
            }
        });
        if (bugReports != null && bugReports.length > 0) {
            return settingsManager.getProgramLogFolder()
                    + "/" + bugReports[0].getName();
        }
        return null;
    }

    public String getBugReportContents(String fileName) {
        File file = new File(fileName);
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        }
        catch (IOException e) {
            return "";
        }
        return text.toString();
    }

    private class MyPositionListener implements PositionManager.PositionListener {
        public void locationChanged(Location location) {
            if (location == null) {
                return;
            }
            currentLocation = new Point(
                    getResources().getString(R.string.locationNameCurrentPosition), location);
            if (locationSinceLastFullUpdate == null)
                locationSinceLastFullUpdate = currentLocation;
            // if the user chooses the current position as route starting point, update that point
            // as fast as you get a new location object
            if (settingsManager.getValueFromTemporaryStartFragmentSettings("currentPositionActive") != null
                    && settingsManager.getValueFromTemporaryStartFragmentSettings("currentPositionActive") > 0) {
                int accuracy = (int) Math.round( location.getAccuracy() );
                String objectName;
                if (positionManager.getStatus() == PositionManager.Status.SIMULATION) {
                    objectName = String.format(
                            getResources().getString(R.string.locationNameSimulationWithDescription),
                            positionManager.getSimulationObjectName() );
                } else {
                    objectName = String.format(
                            getResources().getString(R.string.locationNameCurrentPositionWithAccuracy), accuracy);
                }
                int index = 0;
                if (settingsManager.getValueFromTemporaryStartFragmentSettings("currentPositionActive") == 2) {
                    index = sourceRoute.getSize()-1;
                }
                sourceRoute.replaceRouteObjectAtIndex(index, new RouteObjectWrapper(
                        new Point(objectName, location) ));
                addressManager.updateAddress(sourceRoute.getRouteObjectAtIndex(index).getPoint());
                updateCurrentLocationObjectOnly();
            }
            // update distance of whole source route from time to time
            // if we would do that constantly, it would destroy the source route view in the start tab
            if (currentLocation.distanceTo(locationSinceLastFullUpdate) > 50
                    && location.getSpeed() < 3.0) {
                locationSinceLastFullUpdate = currentLocation;
                updateRouteRequest();
            }
        }

        public void displayGPSSettingsDialog() {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }
    }

    private class MyAddressListener implements AddressManager.AddressListener {
        public void addressUpdateSuccessful(String address) {
            int index = 0;
            if (settingsManager.getValueFromTemporaryStartFragmentSettings("currentPositionActive") == 2) {
                index = sourceRoute.getSize()-1;
            }
            Point wayPoint = sourceRoute.getRouteObjectAtIndex(index).getPoint();
            String objectName = String.format(
                    getResources().getString(R.string.locationNameAddressWithAccuracy),
                    address, wayPoint.getAccuracy() );
            sourceRoute.replaceRouteObjectAtIndex(index, new RouteObjectWrapper(
                        new POIPoint(address, currentLocation.getLocationObject(),
                            getResources().getString(R.string.locationNameAddress)) ));
            updateCurrentLocationObjectOnly();
        }
        public void addressUpdateFailed(String error) {}
        public void cityUpdateSuccessful(String city) {}
    }

    private class VersionDownloadListener implements DataDownloader.DataDownloadListener {
        @Override public void dataDownloadedSuccessfully(JSONObject jsonObject) {
            if (jsonObject == null) {
                Intent intent = new Intent(getActivity(), DialogActivity.class);
                intent.putExtra("message",
                        getResources().getString(R.string.messageVersionRequestFailed) );
                startActivity(intent);
                return;
            }
            try {
                if (jsonObject.has("map_version")) {
                    settingsManager.setMapVersion(jsonObject.getString("map_version"));
                }
                webserverInterfaceVersion = jsonObject.getInt("interface");
                if (webserverInterfaceVersion > settingsManager.getInterfaceVersion()) {
                    showOldWebserverInterfaceVersionDialog();
                }
            } catch (JSONException e) {
                Intent intent = new Intent(getActivity(), DialogActivity.class);
                intent.putExtra("message",
                        getResources().getString(R.string.messageVersionRequestFailed) );
                startActivity(intent);
            }
        }

        @Override public void dataDownloadFailed(String error) {
            Intent intent = new Intent(getActivity(), DialogActivity.class);
            intent.putExtra("message", String.format(
                        getResources().getString(R.string.messageNetworkError), error) );
            startActivity(intent);
        }

        @Override public void dataDownloadCanceled() {}
    }

    private class BugReportDownloadListener implements DataDownloader.DataDownloadListener {
        @Override public void dataDownloadedSuccessfully(JSONObject jsonObject) {
            if (jsonObject == null) {
                Intent intent = new Intent(getActivity(), DialogActivity.class);
                intent.putExtra("message",
                        getResources().getString(R.string.messageBugReportSendingFailed) );
                startActivity(intent);
            } else {
                Intent intent = new Intent(getActivity(), DialogActivity.class);
                intent.putExtra("message",
                        getResources().getString(R.string.messageBugReportSendingSucceeded) );
                startActivity(intent);
            }
        }

        @Override public void dataDownloadFailed(String error) {
            Intent intent = new Intent(getActivity(), DialogActivity.class);
            intent.putExtra("message", String.format(
                        getResources().getString(R.string.messageNetworkError), error) );
            startActivity(intent);
        }

        @Override public void dataDownloadCanceled() {}
    }

    private class RouteDownloadListener implements DataDownloader.DataDownloadListener {
        @Override public void dataDownloadedSuccessfully(JSONObject jsonObject) {
            progressHandler.removeCallbacks(progressUpdater);
            routeDownloader = null;
            Button buttonStartRouting= (Button) mainLayout.findViewById(R.id.buttonStartRouting);
            buttonStartRouting.setTag(0);
            buttonStartRouting.setText(getResources().getString(R.string.buttonStartRouting));
            try {
                Route route = ObjectParser.parseSingleRoute(jsonObject);
                globalData.setValue("route", route);
                globalData.setValue("newRoute", true);
                vibrator.vibrate(300);
                mSFListener.switchToOtherFragment("router");
            } catch (RouteParsingException e) {
                Intent intent = new Intent(getActivity(), DialogActivity.class);
                intent.putExtra("message", String.format(
                        getResources().getString(R.string.messageRouteParsingFailed), e.getMessage()) );
                startActivity(intent);
            }
        }

        @Override public void dataDownloadFailed(String error) {
            routeDownloader = null;
            progressHandler.removeCallbacks(progressUpdater);
            Button buttonStartRouting= (Button) mainLayout.findViewById(R.id.buttonStartRouting);
            buttonStartRouting.setTag(0);
            buttonStartRouting.setText(getResources().getString(R.string.buttonStartRouting));
            Intent intent = new Intent(getActivity(), DialogActivity.class);
            intent.putExtra("message", String.format(
                        getResources().getString(R.string.messageNetworkError), error) );
            startActivity(intent);
        }

        @Override public void dataDownloadCanceled() {
            routeDownloader = null;
            progressHandler.removeCallbacks(progressUpdater);
            Button buttonStartRouting= (Button) mainLayout.findViewById(R.id.buttonStartRouting);
            buttonStartRouting.setTag(0);
            buttonStartRouting.setText(getResources().getString(R.string.buttonStartRouting));
        }
    }

    private class TransportRouteDownloadListener implements DataDownloader.DataDownloadListener {
        @Override public void dataDownloadedSuccessfully(JSONObject jsonObject) {
            routeDownloader = null;
            progressHandler.removeCallbacks(progressUpdater);
            Button buttonStartRouting= (Button) mainLayout.findViewById(R.id.buttonStartRouting);
            buttonStartRouting.setTag(0);
            buttonStartRouting.setText(getResources().getString(R.string.buttonStartRouting));
            Intent intent = new Intent(getActivity(), TransportRouteChooserActivity.class);
            intent.putExtra("transport_routes", jsonObject.toString());
            intent.putExtra("description", String.format(
                    getResources().getString(R.string.messageTransportRouteDescription),
                    sourceRoute.getRouteObjectAtIndex(0).getPoint().getName(),
                    sourceRoute.getRouteObjectAtIndex(sourceRoute.getSize()-1).getPoint().getName()) );
            startActivityForResult(intent, TRANSPORTROUTELOADED);
        }

        @Override public void dataDownloadFailed(String error) {
            routeDownloader = null;
            progressHandler.removeCallbacks(progressUpdater);
            Button buttonStartRouting= (Button) mainLayout.findViewById(R.id.buttonStartRouting);
            buttonStartRouting.setTag(0);
            buttonStartRouting.setText(getResources().getString(R.string.buttonStartRouting));
            Intent intent = new Intent(getActivity(), DialogActivity.class);
            intent.putExtra("message", String.format(
                        getResources().getString(R.string.messageNetworkError), error) );
            startActivity(intent);
        }

        @Override public void dataDownloadCanceled() {
            routeDownloader = null;
            progressHandler.removeCallbacks(progressUpdater);
            Button buttonStartRouting= (Button) mainLayout.findViewById(R.id.buttonStartRouting);
            buttonStartRouting.setTag(0);
            buttonStartRouting.setText(getResources().getString(R.string.buttonStartRouting));
        }
    }

    private class CanceledRequestDownloadListener implements DataDownloader.DataDownloadListener {
        @Override public void dataDownloadedSuccessfully(JSONObject jsonObject) {}
        @Override public void dataDownloadFailed(String error) {}
        @Override public void dataDownloadCanceled() {}
    }

    private class ProgressUpdater implements Runnable {
        public void run() {
            vibrator.vibrate(50);
            progressHandler.postDelayed(this, 2000);
        }
    }
}
