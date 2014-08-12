package org.walkersguide.userinterface;

import java.util.ArrayList;

import org.walkersguide.MainActivity;
import org.walkersguide.R;
import org.walkersguide.routeobjects.FootwaySegment;
import org.walkersguide.routeobjects.IntersectionPoint;
import org.walkersguide.routeobjects.Point;
import org.walkersguide.routeobjects.RouteObjectWrapper;
import org.walkersguide.utils.AddressManager;
import org.walkersguide.utils.CompassCalibrationValidator;
import org.walkersguide.utils.Globals;
import org.walkersguide.utils.HelperFunctions;
import org.walkersguide.utils.KeyboardManager;
import org.walkersguide.utils.POIManager;
import org.walkersguide.utils.POIPreset;
import org.walkersguide.utils.PositionManager;
import org.walkersguide.utils.Route;
import org.walkersguide.utils.SensorsManager;
import org.walkersguide.utils.SettingsManager;

import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.AccessibilityDelegate;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


public class RouterFragment extends Fragment {

    public interface MessageFromRouterFragmentListener{
        public void switchToOtherFragment(String fragmentName);
    }

    private MessageFromRouterFragmentListener mRouterFListener;
    private static final int OBJECTDETAILS = 1;
    private Globals globalData;
    private LinearLayout mainLayout;
    private RelativeLayout routeListLayout, nextPointLayout;
    private Dialog dialog;
    private Toast messageToast;
    private SettingsManager settingsManager;
    private POIManager poiManager;
    private PositionManager positionManager;
    private SensorsManager sensorsManager;
    private AddressManager addressManager;
    private KeyboardManager keyboardManager;
    private CompassCalibrationValidator compassCalibrationValidator;
    private Point currentLocation, lastSpokenLocation;
    private int currentCompassValue;
    private String currentAddress, gpsStatusText;
    private Route route;
    private boolean pointFoundBearing, pointFoundDistance, labelDistanceFocused;
    private int oldBearingValue;
    private int currentPointNumber;
    private ArrayList<Point> processedPOIList;
    private int numberOfHighSpeeds;
    private Handler mHandler, gpsStatusHandler;
    private RouteSimulator routeSimulator;
    private GPSStatusUpdater gpsStatusUpdater;

    @Override public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (globalData == null) {
            globalData = ((Globals) getActivity().getApplicationContext());
        }
        settingsManager = globalData.getSettingsManagerInstance();
        poiManager = globalData.getPOIManagerInstance();
        positionManager = globalData.getPositionManagerInstance();
        sensorsManager = globalData.getSensorsManagerInstance();
        addressManager = globalData.getAddressManagerInstance();
        compassCalibrationValidator = globalData.getCompasscalibrationvalidatorInstance();
        keyboardManager = globalData.getKeyboardManagerInstance();
        processedPOIList = new ArrayList<Point>();
        mHandler = new Handler();
        routeSimulator = new RouteSimulator();
        gpsStatusHandler = new Handler();
        gpsStatusUpdater = new GPSStatusUpdater();
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mRouterFListener = (MessageFromRouterFragmentListener) ((MainActivity) activity).getMessageFromRouterFragmentListener();
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement MessageFromRouterFragmentListener");
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentAddress = "";
        currentPointNumber = -1;
        oldBearingValue = -1;
        pointFoundBearing = false;
        pointFoundDistance = false;
        labelDistanceFocused = false;
        numberOfHighSpeeds = 0;
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_router, container, false);
        messageToast = Toast.makeText(getActivity(), "", Toast.LENGTH_SHORT);
        mainLayout = (LinearLayout) view.findViewById(R.id.linearLayoutMain);
        nextPointLayout = (RelativeLayout) mainLayout.findViewById(R.id.linearLayoutNextPoint);
        routeListLayout = (RelativeLayout) mainLayout.findViewById(R.id.linearLayoutRouteList);

        // main layout
        Button buttonSwitchRouteView = (Button) mainLayout.findViewById(R.id.buttonSwitchRouteView);
        buttonSwitchRouteView.setTag(0);
        buttonSwitchRouteView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Button buttonSwitchRouteView = (Button) mainLayout.findViewById(R.id.buttonSwitchRouteView);
                if ((Integer) buttonSwitchRouteView.getTag() == 0) {
                    // switch to the next point sub view
                    buttonSwitchRouteView.setTag(1);
                } else {
                    buttonSwitchRouteView.setTag(0);
                }
                updateUserInterface( );
            }
        });
        buttonSwitchRouteView.setAccessibilityDelegate(new AccessibilityDelegate() {
            @Override public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
                super.onPopulateAccessibilityEvent(host, event);
                if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
                    labelDistanceFocused = false;
                }
            }
        });

        Spinner spinnerAdditionalOptions = (Spinner) mainLayout.findViewById(R.id.spinnerAdditionalOptions);
        spinnerAdditionalOptions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String oldOption = ((String) parent.getItemAtPosition(
                        settingsManager.getValueFromTemporaryRouterFragmentSettings("spinnerAdditionalOptions") ));
                String newOption = ((String) parent.getItemAtPosition(pos));
                if (oldOption.equals(newOption))
                    return;
                settingsManager.addToTemporaryRouterFragmentSettings("spinnerAdditionalOptions", pos);
                updateLabelStatus();
                updateUserInterface();
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        spinnerAdditionalOptions.setAccessibilityDelegate(new AccessibilityDelegate() {
            @Override public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
                super.onPopulateAccessibilityEvent(host, event);
                if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
                    labelDistanceFocused = false;
                }
            }
        });
        ArrayAdapter<CharSequence> adapterAdditionalOptions = ArrayAdapter.createFromResource(getActivity(),
                R.array.arrayAdditionalOptionsRF, android.R.layout.simple_spinner_item);
        adapterAdditionalOptions.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAdditionalOptions.setAdapter(adapterAdditionalOptions);

        TextView labelStatus = (TextView) mainLayout.findViewById(R.id.labelStatus);
        labelStatus.setAccessibilityDelegate(new AccessibilityDelegate() {
            @Override public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
                super.onPopulateAccessibilityEvent(host, event);
                if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
                    labelDistanceFocused = false;
                }
            }
        });

        // next point layout
        TextView labelNextSegmentDescription = (TextView) nextPointLayout.findViewById(R.id.labelNextSegmentDescription);
        labelNextSegmentDescription.setAccessibilityDelegate(new AccessibilityDelegate() {
            @Override public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
                super.onPopulateAccessibilityEvent(host, event);
                if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
                    labelDistanceFocused = false;
                }
            }
        });

        TextView labelNextPointDescription = (TextView) nextPointLayout.findViewById(R.id.labelNextPointDescription);
        labelNextPointDescription.setAccessibilityDelegate(new AccessibilityDelegate() {
            @Override public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
                super.onPopulateAccessibilityEvent(host, event);
                if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
                    labelDistanceFocused = false;
                }
            }
        });

        TextView labelDetailInformation = (TextView) nextPointLayout.findViewById(R.id.labelDetailInformation);
        labelDetailInformation.setAccessibilityDelegate(new AccessibilityDelegate() {
            @Override public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
                super.onPopulateAccessibilityEvent(host, event);
                if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
                    labelDistanceFocused = false;
                }
            }
        });

        TextView labelDistance= (TextView) nextPointLayout.findViewById(R.id.labelDistance);
        labelDistance.setAccessibilityDelegate(new AccessibilityDelegate() {
            @Override public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
                super.onPopulateAccessibilityEvent(host, event);
                if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
                    lastSpokenLocation = currentLocation;
                    labelDistanceFocused = true;
                }
            }
        });
        labelDistance.setOnFocusChangeListener(new OnFocusChangeListener() {
            public void onFocusChange(View v,boolean hasFocus) {
                if (hasFocus) {
                    lastSpokenLocation = currentLocation;
                    labelDistanceFocused = true;
                } else {
                    labelDistanceFocused = false;
                }
            }
        });

        Button buttonPrevPoint= (Button) nextPointLayout.findViewById(R.id.buttonPrevPoint);
        buttonPrevPoint.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                previousRoutePoint();
            }
        });
        buttonPrevPoint.setAccessibilityDelegate(new AccessibilityDelegate() {
            @Override public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
                super.onPopulateAccessibilityEvent(host, event);
                if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
                    labelDistanceFocused = false;
                }
            }
        });

        TextView labelCurrentPoint= (TextView) nextPointLayout.findViewById(R.id.labelCurrentPoint);
        labelCurrentPoint.setAccessibilityDelegate(new AccessibilityDelegate() {
            @Override public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
                super.onPopulateAccessibilityEvent(host, event);
                if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
                    labelDistanceFocused = false;
                }
            }
        });

        Button buttonNextPoint= (Button) nextPointLayout.findViewById(R.id.buttonNextPoint);
        buttonNextPoint.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                nextRoutePoint();
            }
        });
        buttonNextPoint.setAccessibilityDelegate(new AccessibilityDelegate() {
            @Override public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
                super.onPopulateAccessibilityEvent(host, event);
                if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
                    labelDistanceFocused = false;
                }
            }
        });

        Button buttonDetails = (Button) nextPointLayout.findViewById(R.id.buttonDetails);
        buttonDetails.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (route == null) {
                    messageToast.setText(getResources().getString(R.string.messageNoRouteLoaded));
                    messageToast.show();
                    return;
                }
                RouteObjectWrapper object = route.getNextPoint();
                if (object.isEmpty()) {
                    messageToast.setText(getResources().getString(R.string.messageNoFurtherRoutePoint));
                    messageToast.show();
                    return;
                }
                Intent intent = new Intent(getActivity(), RouteObjectDetailsActivity.class);
                intent.putExtra("route_object", object.toJson().toString());
                // try to get way segment after this point
                RouteObjectWrapper nextSegment = route.getRouteObjectAtIndex( route.getListPosition() + 1);
                if (nextSegment.getFootwaySegment() != null)
                    intent.putExtra("nextSegmentBearing", ((FootwaySegment) nextSegment.getFootwaySegment()).getBearing() );
                else
                    intent.putExtra("nextSegmentBearing", -1);
                startActivityForResult(intent, OBJECTDETAILS);
            }
        });
        buttonDetails.setAccessibilityDelegate(new AccessibilityDelegate() {
            @Override public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
                super.onPopulateAccessibilityEvent(host, event);
                if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
                    labelDistanceFocused = false;
                }
            }
        });

        Spinner spinnerPresets = (Spinner) nextPointLayout.findViewById(R.id.spinnerPresets);
        spinnerPresets.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                int presetId = ((POIPreset) parent.getItemAtPosition(pos)).getId();
                if (presetId == settingsManager.getPresetIdInRouterFragment()) {
                    return;
                }
                poiManager.cancel();
                settingsManager.setPresetIdInRouterFragment(presetId);
                if (presetId == 0) {
                    return;
                }
                POIPreset preset = settingsManager.getPOIPreset(presetId);
                preset.setPOIListStatus(POIPreset.UpdateStatus.RESETLISTPOSITION);
                settingsManager.updatePOIPreset(preset);
                // update poi list
                queryPOIListUpdate();
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        spinnerPresets.setAccessibilityDelegate(new AccessibilityDelegate() {
            @Override public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
                super.onPopulateAccessibilityEvent(host, event);
                if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
                    labelDistanceFocused = false;
                }
            }
        });
        ArrayAdapter<POIPreset> adapter = new ArrayAdapter<POIPreset>(getActivity(),
                android.R.layout.simple_spinner_item, settingsManager.getPOIPresetListWithEmptyPreset());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPresets.setAdapter(adapter);

        Button buttonSimulation = (Button) nextPointLayout.findViewById(R.id.buttonSimulation);
        buttonSimulation.setTag(0);
        buttonSimulation.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (route == null) {
                    messageToast.setText(getResources().getString(R.string.messageNoRouteLoaded));
                    messageToast.show();
                    return;
                }
                showSimulationDialog();
            }
        });
        buttonSimulation.setAccessibilityDelegate(new AccessibilityDelegate() {
            @Override public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
                super.onPopulateAccessibilityEvent(host, event);
                if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
                    labelDistanceFocused = false;
                }
            }
        });

        // route list sub view
        ListView listview = (ListView) routeListLayout.findViewById(R.id.listRouteSegments);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, final View view,
                        int position, long id) {
                RouteObjectWrapper object = route.getRouteObjectAtIndex(position);
                if (object.isEmpty()) {
                    messageToast.setText(getResources().getString(R.string.messageNoFurtherRoutePoint));
                    messageToast.show();
                    return;
                }
                Intent intent = new Intent(getActivity(), RouteObjectDetailsActivity.class);
                intent.putExtra("route_object", object.toJson().toString());
                // try to get way segment after this point
                RouteObjectWrapper nextSegment = route.getRouteObjectAtIndex( route.getListPosition() + 1);
                if (nextSegment.getFootwaySegment() != null)
                    intent.putExtra("nextSegmentBearing", ((FootwaySegment) nextSegment.getFootwaySegment()).getBearing() );
                else
                    intent.putExtra("nextSegmentBearing", -1);
                startActivityForResult(intent, OBJECTDETAILS);
            }
        });
        listview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override public boolean onItemLongClick(AdapterView<?> parent, final View view,
                        int position, long id) {
                route.setListPosition(position);
                Button buttonSwitchRouteView = (Button) mainLayout.findViewById(R.id.buttonSwitchRouteView);
                buttonSwitchRouteView.setTag(1);
                updateUserInterface();
                return true;
            }
        });
        TextView textViewEmptyListView = (TextView) routeListLayout.findViewById(R.id.labelEmptyList);
        textViewEmptyListView.setText(getResources().getString(R.string.messageNoRouteLoaded));
        listview.setEmptyView(textViewEmptyListView);

        updateUserInterface();
        return view;
    }

    public synchronized void updateUserInterface() {
        Button buttonSwitchRouteView = (Button) mainLayout.findViewById(R.id.buttonSwitchRouteView);
        if ((Integer) buttonSwitchRouteView.getTag() == 0) {
            buttonSwitchRouteView.setText(getResources().getString(R.string.buttonSwitchRouteView));
            nextPointLayout.setVisibility(View.GONE);
            routeListLayout.setVisibility(View.VISIBLE);
            if (route != null) {
                ListView listview = (ListView) routeListLayout.findViewById(R.id.listRouteSegments);
                RouteObjectListAdapter listAdapter = new RouteObjectListAdapter(
                        getActivity(), android.R.layout.simple_list_item_1, route.getRouteList(), route.getListPosition() );
                listview.setAdapter(listAdapter);
                listview.setSelectionFromTop(route.getListPosition(), 0);
            }
        } else {
            buttonSwitchRouteView.setText(getResources().getString(R.string.buttonSwitchRouteViewClicked));
            routeListLayout.setVisibility(View.GONE);
            nextPointLayout.setVisibility(View.VISIBLE);
            // poi presets
            Spinner spinnerPresets = (Spinner) nextPointLayout.findViewById(R.id.spinnerPresets);
            ArrayAdapter<POIPreset> presetsAdapter = (ArrayAdapter<POIPreset>) spinnerPresets.getAdapter();
            presetsAdapter.notifyDataSetChanged();
            int index = presetsAdapter.getPosition(
                    settingsManager.getPOIPreset(settingsManager.getPresetIdInRouterFragment()) );
            if (index == -1)
                spinnerPresets.setSelection(0);
            else
                spinnerPresets.setSelection( index );
        }
        // options spinner
        Spinner spinnerAdditionalOptions = (Spinner) mainLayout.findViewById(R.id.spinnerAdditionalOptions);
        if (settingsManager.getValueFromTemporaryRouterFragmentSettings("spinnerAdditionalOptions") != null) {
            spinnerAdditionalOptions.setSelection(
                    settingsManager.getValueFromTemporaryRouterFragmentSettings("spinnerAdditionalOptions"));
        } else {
            ArrayAdapter<String> adapterAdditionalOptions =
                (ArrayAdapter<String>) spinnerAdditionalOptions.getAdapter();
            int index = adapterAdditionalOptions.getPosition(
                    getResources().getString(R.string.arrayAADisabled));
            if (index == -1)
                index = 0;
            spinnerAdditionalOptions.setSelection(index);
            settingsManager.addToTemporaryRouterFragmentSettings("spinnerAdditionalOptions", index);
        }
        TextView labelStatus = (TextView) mainLayout.findViewById(R.id.labelStatus);
        if ( ((String) spinnerAdditionalOptions.getSelectedItem()).equals(getResources().getString(R.string.arrayAADisabled)) ) {
            labelStatus.setVisibility(View.GONE);
        } else {
            labelStatus.setVisibility(View.VISIBLE);
        }
        Button buttonSimulation = (Button) nextPointLayout.findViewById(R.id.buttonSimulation);
        if ((Integer) buttonSimulation.getTag() > 0) {
            buttonSimulation.setText(getResources().getString(R.string.buttonSimulationClicked));
        } else {
            buttonSimulation.setText(getResources().getString(R.string.buttonSimulation));
        }
    }

    public void updateLabelStatus() {
        Spinner spinnerAdditionalOptions = (Spinner) mainLayout.findViewById(R.id.spinnerAdditionalOptions);
        TextView labelStatus = (TextView) mainLayout.findViewById(R.id.labelStatus);
        if ( ((String) spinnerAdditionalOptions.getSelectedItem()).equals(getResources().getString(R.string.arrayAAAddress)) ) {
            labelStatus.setText( String.format(
                    getResources().getString(R.string.messageCurrentAddress), currentAddress));
        } else if ( ((String) spinnerAdditionalOptions.getSelectedItem()).equals(getResources().getString(R.string.arrayAALatestPOI)) ) {
            if (processedPOIList != null && processedPOIList.size() > 0) {
                Point poi = processedPOIList.get(processedPOIList.size()-1);
                poi.addDistance(currentLocation.distanceTo(poi));
                poi.addBearing(currentLocation.bearingTo(poi) - currentCompassValue);
                labelStatus.setText( String.format(
                        getResources().getString(R.string.messageLatestPOI),
                        poi.toString()) );
            } else {
                labelStatus.setText(getResources().getString(R.string.messageNoPOIInRange));
            }
        } else if ( ((String) spinnerAdditionalOptions.getSelectedItem()).equals(getResources().getString(R.string.arrayAARouteDescription)) ) {
            if (route != null) {
                labelStatus.setText( String.format(
                        getResources().getString(R.string.messageRouteDescription),
                        route.toString()) );
            } else {
                labelStatus.setText(getResources().getString(R.string.messageNoRouteLoaded));
            }
        } else if ( ((String) spinnerAdditionalOptions.getSelectedItem()).equals(getResources().getString(R.string.arrayAAGPSStatus)) ) {
            labelStatus.setText(gpsStatusText);
        } else {
            labelStatus.setText("");
        }
    }

    public void updateRouteCurrentPositionFragment() {
        if (route == null || currentLocation == null) {
            return;
        }
        TextView labelNextSegmentDescription = (TextView) nextPointLayout.findViewById(R.id.labelNextSegmentDescription);
        labelNextSegmentDescription.setText("");
        TextView labelNextPointDescription = (TextView) nextPointLayout.findViewById(R.id.labelNextPointDescription);
        labelNextPointDescription.setText("");
        TextView labelDetailInformation = (TextView) nextPointLayout.findViewById(R.id.labelDetailInformation);
        labelDetailInformation.setText("");
        TextView labelDistance= (TextView) nextPointLayout.findViewById(R.id.labelDistance);
        labelDistance.setText("");
        TextView labelCurrentPoint= (TextView) nextPointLayout.findViewById(R.id.labelCurrentPoint);
        labelCurrentPoint.setText("");
        RouteObjectWrapper nextPoint = route.getNextPoint();
        RouteObjectWrapper prevPoint = route.getPreviousPoint();
        RouteObjectWrapper segment = route.getNextSegment();

        // get next sub point of current segment if available
        /*int currentBearing = currentLocation.bearingTo(nextPoint.getPoint());
        int currentSubBearing = -1;
        int currentSubDistance = -1;
        Point subPoint = null;
        Iterator<Point> listIterator = route.getSegmentSubPoints().iterator();
        while (listIterator.hasNext()) {
            subPoint = listIterator.next();
            currentSubDistance = currentLocation.distanceTo(subPoint);
            currentSubBearing = currentLocation.bearingTo(subPoint);
            if ((currentSubDistance > 20) && (Math.abs(currentSubBearing - currentBearing) < 45))
                break;
            listIterator.remove();
        }*/
        /* String desc = "Kein Subpoint verfügbar";
        if (subPoint != null) {
            desc = currentSubBearing + " Grad; " + currentSubDistance + "m; noch " + route.getSegmentSubPoints().size() + " SubPunkte;";
            if (subPoint instanceof IntersectionPoint)
                desc += "\n" + ((IntersectionPoint) subPoint).toString();
            else if (subPoint instanceof POIPoint)
                desc += "\n" + ((POIPoint) subPoint).toString();
            else if (subPoint instanceof StationPoint)
                desc += "\n" + ((StationPoint) subPoint).toString();
            else
                desc += "\n" + subPoint.toString();
        }
        labelDetailInformation.setText(desc); */
        /*currentBearing = currentSubBearing;
        if (currentBearing == -1)
            currentBearing = currentLocation.bearingTo(nextPoint.getPoint());*/

        int currentBearing = currentLocation.bearingTo(nextPoint.getPoint());
        int currentDistance = currentLocation.distanceTo(nextPoint.getPoint());
        int clockDirection = HelperFunctions.getClockDirection(currentBearing - currentCompassValue);
        if (oldBearingValue == -1)
            oldBearingValue = currentBearing;

        // tell the user, when the next route point is reached
        // either by bearing or distance
        int threshold_angle = 75;
        int calculated_angle = 0;
        if (segment.getFootwaySegment() != null) {
            calculated_angle = Math.min(Math.abs(segment.getFootwaySegment().getBearing() - currentBearing),
                    (360 - Math.abs(segment.getFootwaySegment().getBearing() - currentBearing)) );
            if ((currentDistance < 25) && (pointFoundDistance == false)) {
                pointFoundDistance = true;
                if (segment.getFootwaySegment().getDistance() > 25) {
                    Toast.makeText(getActivity(),
                            getResources().getString(R.string.messageTwentyFiveMeters),
                            Toast.LENGTH_SHORT).show();
                }
            }
            if ((pointFoundBearing == false) && (pointFoundDistance == true)) {
                if (nextPoint.getIntersection() != null
                        && nextPoint.getIntersection().getNumberOfBigStreets() > 1) {
                    threshold_angle = 40;
                }
                if (calculated_angle > threshold_angle || currentDistance <= 5) {
                    pointFoundBearing = true;
                    if (nextPoint.getPoint().getTurn() > -1) {
                        String nextDirection = HelperFunctions.getFormatedDirection(nextPoint.getPoint().getTurn());
                        if (nextDirection.equals(getResources().getString(R.string.directionStraightforward))) {
                            Toast.makeText(getActivity(),
                                    String.format(
                                        getResources().getString(R.string.messageReachedIntermediatePointAhead),
                                        route.getNextPointNumber(), route.getNumberOfPoints() ),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getActivity(),
                                    String.format(
                                        getResources().getString(R.string.messageReachedIntermediatePointTurn),
                                        route.getNextPointNumber(), route.getNumberOfPoints(),
                                        HelperFunctions.getFormatedDirection(nextPoint.getPoint().getTurn()) ),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getActivity(),
                                String.format(
                                    getResources().getString(R.string.messageReachedDestinationPoint),
                                    route.getNextPointNumber(), route.getNumberOfPoints() ),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
        // transport segment
        if (segment.getTransportSegment() != null) {
            if ((currentDistance < 75) && (pointFoundDistance == false)) {
                pointFoundDistance = true;
                Toast.makeText(getActivity(),
                        String.format(
                            getResources().getString(R.string.messageReachedStation),
                            nextPoint.getPoint().getName() ),
                        Toast.LENGTH_SHORT).show();
            }
        }

        String pointDescription = "";
        if (nextPoint.getIntersection() != null) {
            if (route.getNextPointNumber() == 1) {
                pointDescription = String.format(
                            getResources().getString(R.string.messagePointDescInterStart),
                            nextPoint.getIntersection().getName());
            } else if (route.getNextPointNumber() == route.getNumberOfPoints()) {
                pointDescription = String.format(
                            getResources().getString(R.string.messagePointDescInterDestination),
                            nextPoint.getIntersection().getName());
            } else {
                String nextStreetName = "";
                String streets = "";
                int absValue = 0;
                int bearingOfPrevSegment = segment.getFootwaySegment().getBearing() - 180;
                if (bearingOfPrevSegment < 0)
                    bearingOfPrevSegment += 360;
                int bearingOfNextSegment = segment.getFootwaySegment().getBearing() + nextPoint.getPoint().getTurn();
                if (bearingOfNextSegment >= 360)
                    bearingOfNextSegment -= 360;
                for (IntersectionPoint.IntersectionWay way : nextPoint.getIntersection().getSubPoints()) {
                    absValue = Math.abs( bearingOfNextSegment - way.getIntersectionBearing());
                    if ((absValue < 10) || (absValue > 350)) {
                        nextStreetName = way.getName();
                    }
                    absValue = way.getIntersectionBearing() - currentCompassValue;
                    if (streets.equals(""))
                        streets += HelperFunctions.getFormatedDirection(absValue) + ": " + way.getName();
                    else
                        streets += ", " + HelperFunctions.getFormatedDirection(absValue) + ": " + way.getName();
                }
                if (nextPoint.getIntersection().getTrafficSignalList().size() > 0) {
                    labelDetailInformation.setText( String.format(
                                getResources().getString(R.string.messageDynamicIntersectionDescriptionWithTS),
                                streets, nextPoint.getIntersection().getTrafficSignalList().size()) );
                } else {
                    labelDetailInformation.setText( String.format(
                                getResources().getString(R.string.messageDynamicIntersectionDescription), streets) );
                }
                int turn = nextPoint.getIntersection().getTurn();
                if ((turn > 22) && (turn < 338)) {
                    if (!nextStreetName.equals("")) {
                        pointDescription = String.format(
                                getResources().getString(R.string.messagePointDescInterInterWithStreet),
                                nextPoint.getIntersection().getName(),
                                HelperFunctions.getFormatedDirection(turn),
                                nextStreetName);
                    } else {
                        pointDescription = String.format(
                                getResources().getString(R.string.messagePointDescInterInter),
                                nextPoint.getIntersection().getName(),
                                HelperFunctions.getFormatedDirection(turn));
                    }
                } else {
                    pointDescription = String.format(
                            getResources().getString(R.string.messagePointDescInterInterStraight),
                            nextPoint.getIntersection().getName());
                }
            }
            labelNextPointDescription.setText(pointDescription);
        } else if (nextPoint.getPOI() != null) {
            if (route.getNextPointNumber() == 1) {
                labelNextPointDescription.setText( String.format(
                            getResources().getString(R.string.messagePointDescPOIStart),
                            nextPoint.getPOI().getName() ));
            } else if (route.getNextPointNumber() == route.getNumberOfPoints()) {
                labelNextPointDescription.setText( String.format(
                            getResources().getString(R.string.messagePointDescPOIDestination),
                            nextPoint.getPOI().getName() ));
            } else {
                labelNextPointDescription.setText( String.format(
                            getResources().getString(R.string.messagePointDescPOIInter),
                            nextPoint.getPOI().getName() ));
            }
        } else if (nextPoint.getStation() != null) {
            if (route.getNextPointNumber() == 1) {
                labelNextPointDescription.setText( String.format(
                            getResources().getString(R.string.messagePointDescStationStart),
                            nextPoint.getStation().getName(),
                            nextPoint.getStation().getSubType() ));
            } else if (route.getNextPointNumber() == route.getNumberOfPoints()) {
                labelNextPointDescription.setText( String.format(
                            getResources().getString(R.string.messagePointDescStationDestination),
                            nextPoint.getStation().getName(),
                            nextPoint.getStation().getSubType() ));
            } else {
                labelNextPointDescription.setText( String.format(
                            getResources().getString(R.string.messagePointDescStationInter),
                            nextPoint.getStation().getName(),
                            nextPoint.getStation().getSubType() ));
            }
            if (nextPoint.getStation().getFoundInOSMDatabase() == false)
                labelNextPointDescription.setText( String.format(
                            getResources().getString(R.string.messagePointDescStationNoPos),
                            labelNextPointDescription.getText().toString() ));
        } else {
            if (route.getNextPointNumber() == 1) {
                labelNextPointDescription.setText(getResources().getString(R.string.messagePointDescWayPointStart));
            } else if (route.getNextPointNumber() == route.getNumberOfPoints()) {
                labelNextPointDescription.setText(getResources().getString(R.string.messagePointDescWayPointDestination));
            } else {
                String nextDirection = HelperFunctions.getFormatedDirection(nextPoint.getPoint().getTurn());
                if (nextDirection.equals(getResources().getString(R.string.directionStraightforward))) {
                    labelNextPointDescription.setText(
                            getResources().getString(R.string.messagePointDescWayPointInterAhead));
                } else {
                    labelNextPointDescription.setText( String.format(
                            getResources().getString(R.string.messagePointDescWayPointInterTurn), nextDirection));
                }
            }
        }

        String segmentDescription = "";
        if (segment.getFootwaySegment() != null) {
            segmentDescription = String.format(
                    getResources().getString(R.string.messageSegmentDescFootway),
                    segment.getFootwaySegment().getDistance(),
                    segment.getFootwaySegment().getName(),
                    segment.getFootwaySegment().getSubType() );
            if (! segment.getFootwaySegment().getSurface().equals(""))
                segmentDescription += String.format(
                        getResources().getString(R.string.roWaySurface),
                        segment.getFootwaySegment().getSurface() );
            if (segment.getFootwaySegment().getSidewalk() >= 0)
                segmentDescription += ", " + segment.getFootwaySegment().printSidewalk();
            labelNextSegmentDescription.setText(segmentDescription);
        } else if (segment.getTransportSegment() != null) {
            labelNextSegmentDescription.setText( String.format(
                    getResources().getString(R.string.messageSegmentDescTransport),
                    segment.getTransportSegment().getName(),
                    segment.getTransportSegment().getDepartureTime(),
                    segment.getTransportSegment().getDuration(),
                    segment.getTransportSegment().getNumberOfStops() ));
        } else {
            labelNextSegmentDescription.setText( String.format(
                    getResources().getString(R.string.messageSegmentDescStart), currentDistance));
        }

        // refresh distance and bearing to the next point
        labelCurrentPoint.setText( String.format(
                    getResources().getString(R.string.messagePointProgress),
                    route.getNextPointNumber(), route.getNumberOfPoints() ));
        labelDistance.setText( String.format(
                    getResources().getString(R.string.messageDistanceAndBearing),
                    currentDistance, clockDirection, currentCompassValue));

        // speak updated contents of labelDistance field if focused
        if (labelDistanceFocused && currentLocation.distanceTo(lastSpokenLocation) > 20) {
            lastSpokenLocation = currentLocation;
            labelDistance.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
        }
    }

    public void showSimulationDialog() {
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
        label.setText(getResources().getString(R.string.labelSimulationHeading));
        dialogLayout.addView(label);

        Button buttonSimulation = (Button) nextPointLayout.findViewById(R.id.buttonSimulation);
        int simulationStatus = (Integer) buttonSimulation.getTag();
        if (simulationStatus == 0) {
            Button buttonStartSimulation = new Button(getActivity());
            buttonStartSimulation.setLayoutParams(lp);
            buttonStartSimulation.setText(getResources().getString(R.string.buttonStartSimulation));
            buttonStartSimulation.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    Button buttonSimulation = (Button) nextPointLayout.findViewById(R.id.buttonSimulation);
                    buttonSimulation.setTag(2);
                    mHandler.postDelayed(routeSimulator, 100);
                    updateUserInterface();
                    dialog.dismiss();
                }
            });
            dialogLayout.addView(buttonStartSimulation);
        } else {
            if (routeSimulator.paused()) { // paused
                Button buttonResumeSimulation = new Button(getActivity());
                buttonResumeSimulation.setLayoutParams(lp);
                buttonResumeSimulation.setText(getResources().getString(R.string.buttonResumeSimulation));
                buttonResumeSimulation.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view) {
                        routeSimulator.resume();
                        Button buttonSimulation = (Button) nextPointLayout.findViewById(R.id.buttonSimulation);
                        buttonSimulation.setTag(2);
                        updateUserInterface();
                        dialog.dismiss();
                    }
                });
                dialogLayout.addView(buttonResumeSimulation);
            } else { // running
                Button buttonPauseSimulation = new Button(getActivity());
                buttonPauseSimulation.setLayoutParams(lp);
                buttonPauseSimulation.setText(getResources().getString(R.string.buttonPauseSimulation));
                buttonPauseSimulation.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view) {
                        routeSimulator.pause();
                        Button buttonSimulation = (Button) nextPointLayout.findViewById(R.id.buttonSimulation);
                        buttonSimulation.setTag(1);
                        updateUserInterface();
                        dialog.dismiss();
                    }
                });
                dialogLayout.addView(buttonPauseSimulation);
            }
            Button buttonJumpToNextPoint = new Button(getActivity());
            buttonJumpToNextPoint.setLayoutParams(lp);
            buttonJumpToNextPoint.setText(getResources().getString(R.string.buttonJumpToNextPoint));
            buttonJumpToNextPoint.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    routeSimulator.jumpToNextPoint();
                    dialog.dismiss();
                }
            });
            dialogLayout.addView(buttonJumpToNextPoint);
            Button buttonStopSimulation = new Button(getActivity());
            buttonStopSimulation.setLayoutParams(lp);
            buttonStopSimulation.setText(getResources().getString(R.string.buttonStopSimulation));
            buttonStopSimulation.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    Button buttonSimulation = (Button) nextPointLayout.findViewById(R.id.buttonSimulation);
                    buttonSimulation.setTag(0);
                    mHandler.removeCallbacks(routeSimulator);
                    positionManager.changeStatus(PositionManager.Status.GPS, null);
                    updateUserInterface();
                    dialog.dismiss();
                }
            });
            dialogLayout.addView(buttonStopSimulation);
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

    @Override public void onPause() {
        super.onPause();
        Button buttonSwitchRouteView = (Button) mainLayout.findViewById(R.id.buttonSwitchRouteView);
        settingsManager.addToTemporaryRouterFragmentSettings("buttonSwitchRouteView",
                (Integer) buttonSwitchRouteView.getTag());
        Button buttonSimulation = (Button) nextPointLayout.findViewById(R.id.buttonSimulation);
        settingsManager.addToTemporaryRouterFragmentSettings("buttonSimulation",
                (Integer) buttonSimulation.getTag());
        gpsStatusHandler.removeCallbacks(gpsStatusUpdater);
        positionManager.stopGPS();
        sensorsManager.stopSensors();
        // stop simulation
        if ((Integer) buttonSimulation.getTag() > 0) {
            mHandler.removeCallbacks(routeSimulator);
        }
        poiManager.cancel();
    }

    @Override public void onResume() {
        super.onResume();	
        if (globalData == null) {
            globalData = ((Globals) getActivity().getApplicationContext());
        }
        settingsManager = globalData.getSettingsManagerInstance();
        // restore temporary data
        Button buttonSwitchRouteView = (Button) mainLayout.findViewById(R.id.buttonSwitchRouteView);
        if (settingsManager.getValueFromTemporaryRouterFragmentSettings("buttonSwitchRouteView") != null)
            buttonSwitchRouteView.setTag(settingsManager.getValueFromTemporaryRouterFragmentSettings("buttonSwitchRouteView"));
        Button buttonSimulation = (Button) nextPointLayout.findViewById(R.id.buttonSimulation);
        buttonSimulation.setTag(0);
        if (positionManager.getStatus() == PositionManager.Status.SIMULATION) {
            if (settingsManager.getValueFromTemporaryRouterFragmentSettings("buttonSimulation") != null)
                buttonSimulation.setTag(settingsManager.getValueFromTemporaryRouterFragmentSettings("buttonSimulation"));
        }
        updateUserInterface();

        // get new route
        if (globalData.getValue("route") != null) {
            if ((route == null) || ((Boolean) globalData.getValue("newRoute") == true)) {
                route = (Route) globalData.getValue("route");
                globalData.setValue("newRoute", false);
                // change to route list subfragment
                buttonSwitchRouteView.setTag(0);
                // show route description
                Spinner spinnerAdditionalOptions = (Spinner) mainLayout.findViewById(R.id.spinnerAdditionalOptions);
                ArrayAdapter<String> adapterAdditionalOptions =
                    (ArrayAdapter<String>) spinnerAdditionalOptions.getAdapter();
                int index = adapterAdditionalOptions.getPosition(
                        getResources().getString(R.string.arrayAARouteDescription));
                if (index == -1)
                    index = 0;
                spinnerAdditionalOptions.setSelection(index);
                settingsManager.addToTemporaryRouterFragmentSettings("spinnerAdditionalOptions", index);
                updateLabelStatus();
                updateUserInterface();
            }
        }

        // poi manager
        poiManager.setPOIListener(new MyPOIListener());
        // address manager
        addressManager.setAddressListener(new MyAddressListener());
        // keyboard manager
        keyboardManager.setKeyboardListener(new MyKeyboardListener());
        // sensors manager
        sensorsManager.setSensorsListener(new MySensorsListener());
        sensorsManager.resumeSensors();
        // position manager
        positionManager.setPositionListener(new MyPositionListener());
        positionManager.resumeGPS();
        // warn if we got no route
        if (route == null) {
            messageToast.setText(getResources().getString(R.string.messageNoRouteLoaded));
            messageToast.show();
        }
        // resume simulation
        if ((Integer) buttonSimulation.getTag() > 0) {
            mHandler.postDelayed(routeSimulator, 100);
        }
        // gps quality hanler
        gpsStatusHandler.postDelayed(gpsStatusUpdater, 100);
    }

    public void queryPOIListUpdate() {
        POIPreset preset = settingsManager.getPOIPreset(settingsManager.getPresetIdInRouterFragment());
        boolean isInsidePublicTransport = false;
        if (numberOfHighSpeeds > 0)
            isInsidePublicTransport = true;
        poiManager.updatePOIList(preset.getId(), preset.getRange(), currentLocation,
                currentCompassValue, "", isInsidePublicTransport);
    }

    public void queryAddressUpdate() {
        if (numberOfHighSpeeds == 0)
            addressManager.updateAddress(currentLocation);
    }

    public void previousRoutePoint() {
        if (route == null) {
            messageToast.setText(getResources().getString(R.string.messageNoRouteLoaded));
            messageToast.show();
            return;
        } else if (route.getNextPointNumber() == 1) {
            messageToast.setText(getResources().getString(R.string.messageFirstRoutePoint));
            messageToast.show();
            return;
        } else {
            Toast.makeText(getActivity(),
                    getResources().getString(R.string.messagePreviousPoint),
                    Toast.LENGTH_LONG).show();
        }
        route.previousPoint();
        if (currentLocation.distanceTo(route.getNextPoint().getPoint()) > 25) {
            pointFoundBearing = false;
            pointFoundDistance = false;
        }
        oldBearingValue = -1;
        // switch to the next route command screen if the route list is shown
        Button buttonSwitchRouteView = (Button) mainLayout.findViewById(R.id.buttonSwitchRouteView);
        if ((Integer) buttonSwitchRouteView.getTag() == 0) {
            buttonSwitchRouteView.setTag(1);
            updateUserInterface();
        }
        updateRouteCurrentPositionFragment();
    }

    public void nextRoutePoint() {
        if (route == null) {
            messageToast.setText(getResources().getString(R.string.messageNoRouteLoaded));
            messageToast.show();
            return;
        } else if (route.getNextPointNumber() == route.getNumberOfPoints()) {
            messageToast.setText(getResources().getString(R.string.messageLastRoutePoint));
            messageToast.show();
            return;
        }
        route.nextPoint();
        RouteObjectWrapper segment = route.getNextSegment();
        if (segment.getFootwaySegment() != null) {
            Toast.makeText(getActivity(),
                    String.format(
                        getResources().getString(R.string.messageNextPointFootway),
                        segment.getFootwaySegment().getDistance(),
                        segment.getFootwaySegment().getName()),
                    Toast.LENGTH_LONG).show();
        } else if (segment.getTransportSegment() != null) {
            Toast.makeText(getActivity(),
                    String.format(
                        getResources().getString(R.string.messageNextPointTransport),
                        segment.getTransportSegment().getName(),
                        segment.getTransportSegment().getDepartureTime()),
                    Toast.LENGTH_LONG).show();
        }
        oldBearingValue = -1;
        pointFoundBearing = false;
        pointFoundDistance = false;
        // switch to the next route command screen if the route list is shown
        Button buttonSwitchRouteView = (Button) mainLayout.findViewById(R.id.buttonSwitchRouteView);
        if ((Integer) buttonSwitchRouteView.getTag() == 0) {
            buttonSwitchRouteView.setTag(1);
            updateUserInterface();
        }
        updateRouteCurrentPositionFragment();
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data){
        // See which child activity is calling us back.
        switch (requestCode) {
            case OBJECTDETAILS:
                // This is the standard resultCode that is sent back if the
                // activity crashed or didn't doesn't supply an explicit result.
                if (resultCode == getActivity().RESULT_CANCELED) {
                    break;
                } else {
                    if (data.getStringExtra("fragment").equals("start"))
                        mRouterFListener.switchToOtherFragment("start");
                    break;
                }
            default:
                break;
        }
    }

    private class MyKeyboardListener implements KeyboardManager.KeyboardListener {
        public void longPressed(KeyEvent event) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    previousRoutePoint();
                    return;
                case KeyEvent.KEYCODE_VOLUME_UP:
                    nextRoutePoint();
                    return;
                default:
                    return;
            }
        }
    }

    private class MyPOIListener implements POIManager.POIListener {
        public void poiPresetUpdateSuccessful() {
            POIPreset preset = settingsManager.getPOIPreset(settingsManager.getPresetIdInRouterFragment());
            if (preset.getPOIListStatus() != POIPreset.UpdateStatus.UNCHANGED) {
                for (Point poi : preset.getPOIList()) {
                    if (poi.getDistance() > 30)
                        break;
                    if (!processedPOIList.contains(poi)) {
                        processedPOIList.add(poi);
                        String poiLabel = String.format(
                                getResources().getString(R.string.messageLatestPOI),
                                poi.toString());
                        messageToast.setText(poiLabel);
                        messageToast.show();
                        updateLabelStatus();
                    }
                }
            }
        }
        public void poiPresetUpdateFailed(String error) {}
    }

    private class MyAddressListener implements AddressManager.AddressListener {
        public void addressUpdateSuccessful(String address) {
            if (!currentAddress.equals(address)) {
                currentAddress = address;
                String addressLabel = String.format(
                        getResources().getString(R.string.messageCurrentAddress), address);
                messageToast.setText(addressLabel);
                messageToast.show();
                updateLabelStatus();
            }
        }
        public void addressUpdateFailed(String error) {
            currentAddress = error;
            updateLabelStatus();
        }
        public void cityUpdateSuccessful(String city) {}
    }

    private class MyPositionListener implements PositionManager.PositionListener {
        public void locationChanged(Location location) {
            if (location == null)
                return;
            boolean startForFirstTime = false;
            currentLocation = new Point(
                    getResources().getString(R.string.locationNameCurrentPosition), location);
            currentLocation.addAccuracy( (int) Math.round(location.getAccuracy()) );
            if (lastSpokenLocation == null) {
                lastSpokenLocation = currentLocation;
            }
            if (location.getSpeed() >= 5.0) {
                numberOfHighSpeeds = 2;
            } else if (numberOfHighSpeeds > 0) {
                numberOfHighSpeeds -= 1;
            }
            updateRouteCurrentPositionFragment();

            // nearest poi if the user hasn't disabled the feature
            if (settingsManager.getPresetIdInRouterFragment() > 0) {
                queryPOIListUpdate();
            }
            // address monitor
            Spinner spinnerAdditionalOptions = (Spinner) mainLayout.findViewById(R.id.spinnerAdditionalOptions);
            if ( ((String) spinnerAdditionalOptions.getSelectedItem()).equals(getResources().getString(R.string.arrayAAAddress)) ) {
                queryAddressUpdate();
            }
        }

        public void displayGPSSettingsDialog() {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }
    }

    private class MySensorsListener implements SensorsManager.SensorsListener {
        public void compassChanged(float degree) {
            currentCompassValue = (int) Math.round(degree);
            updateRouteCurrentPositionFragment();
            // check calibration of compass
            compassCalibrationValidator.validate(
                    currentLocation.getLocationObject(), currentCompassValue);
        }

        public void acceleratorChanged(double accel) {
            if (settingsManager.getShakeForNextRoutePoint() == true
                    && accel >= settingsManager.getShakeIntensity()
                    && pointFoundDistance == true) {
                nextRoutePoint();
            }
        }
    }

    public class RouteObjectListAdapter extends ArrayAdapter<RouteObjectWrapper> {
        private Context ctx;
        private LayoutInflater m_inflater = null;
        public ArrayList<RouteObjectWrapper> wrapperArray;
        private int selectedPosition;

        public RouteObjectListAdapter(Context context, int textViewResourceId, ArrayList<RouteObjectWrapper> array, int selectedPosition) {
            super(context, textViewResourceId);
            this.wrapperArray = array;
            this.ctx = context;
            m_inflater = LayoutInflater.from(ctx);
            this.selectedPosition = selectedPosition;
        }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            EntryHolder holder;
            if (convertView == null) {
                holder = new EntryHolder();
                convertView = m_inflater.inflate(R.layout.table_row_two_column, parent, false);
                holder.number = (TextView) convertView.findViewById(R.id.labelColumnOne);
                holder.contents= (TextView) convertView.findViewById(R.id.labelColumnTwo);
                convertView.setTag(holder);
            } else {
                holder = (EntryHolder) convertView.getTag();
            }
            RouteObjectWrapper wrapperObject = getItem(position);
            if (position == selectedPosition)
                holder.number.setText("* " + String.valueOf(position+1) +". ");
            else
                holder.number.setText(String.valueOf(position+1) +". ");
            holder.contents.setText(wrapperObject.toString());
            return convertView;
        }

        @Override public int getCount() {
            if (wrapperArray != null)
                return wrapperArray.size();
            return 0;
        }

        @Override public RouteObjectWrapper getItem(int position) {
            return wrapperArray.get(position);
        }

        public void setArrayList(ArrayList<RouteObjectWrapper> array) {
            this.wrapperArray= array;
            notifyDataSetChanged();
        }

        private class EntryHolder {
            public TextView number, contents;
        }
    }

    private class RouteSimulator implements Runnable {
        private int nextPointNumber;
        private boolean pause;
        private boolean jumpToNextPoint;
        private int speed;
        private int walkingSpeed;
        private int transportSpeed;

        public RouteSimulator() {
            this.nextPointNumber = 0;
            this.pause = false;
            this.jumpToNextPoint = false;
            this.speed = 0;
            this.walkingSpeed = 3;
            this.transportSpeed = 30;
        }

        public void pause() {
            this.pause = true;
        }

        public void resume() {
            this.pause = false;
        }

        public boolean paused() {
            return this.pause;
        }

        public void jumpToNextPoint() {
            jumpToNextPoint = true;
        }

        public void run() {
            Point nextPoint = route.getNextPoint().getPoint();
            Point prevPoint = route.getPreviousPoint().getPoint();
            if(prevPoint == null) {
                prevPoint = nextPoint;
            }
            if (nextPointNumber != route.getNextPointNumber())  {
                nextPointNumber = route.getNextPointNumber();
                positionManager.changeStatus(PositionManager.Status.SIMULATION, prevPoint);
                if (route.getNextSegment().getFootwaySegment() != null) {
                    speed = walkingSpeed;
                } else {
                    speed = transportSpeed;
                }
            } else if ((!pause) && (currentLocation.distanceTo(nextPoint) > speed)) {
                int numberOfParts = prevPoint.distanceTo(nextPoint) / speed;
                double newLatitude = 0.0;
                double newLongitude = 0.0;
                if (jumpToNextPoint) {
                    newLatitude = nextPoint.getLatitude() - ((nextPoint.getLatitude() - prevPoint.getLatitude()) / (double) numberOfParts);
                    newLongitude = nextPoint.getLongitude() - ((nextPoint.getLongitude() - prevPoint.getLongitude()) / (double) numberOfParts);
                    jumpToNextPoint = false;
                } else {
                    newLatitude = currentLocation.getLatitude() + ((nextPoint.getLatitude() - prevPoint.getLatitude()) / (double) numberOfParts);
                    newLongitude = currentLocation.getLongitude() + ((nextPoint.getLongitude() - prevPoint.getLongitude()) / (double) numberOfParts);
                }
                // build location object
                Location l = new Location(getResources().getString(R.string.locationProviderGPS));
                l.setLatitude(newLatitude);
                l.setLongitude(newLongitude);
                l.setAccuracy(0);
                l.setBearing(currentCompassValue);
                l.setSpeed(speed);
                l.setTime(System.currentTimeMillis());
                // set new position
                positionManager.changeStatus(PositionManager.Status.SIMULATION,
                        new Point(getResources().getString(R.string.locationNameCurrentPosition), l));
            }
            mHandler.postDelayed(this, 1000);
        }
    }

    private class GPSStatusUpdater implements Runnable {
        public void run() {
            Location location = null;
            if (currentLocation != null && currentLocation.getLocationObject() != null) {
                location = currentLocation.getLocationObject();
            }
            // get signal source
            if (positionManager.getStatus() == PositionManager.Status.DISABLED) {
                gpsStatusText = getResources().getString(R.string.messageGPSStatusSignalDisabled);
            } else if (positionManager.getStatus() == PositionManager.Status.SIMULATION) {
                gpsStatusText = getResources().getString(R.string.messageGPSStatusSignalSimulation);
            } else {
                if (location == null) {
                    gpsStatusText = getResources().getString(R.string.messageGPSStatusSignalNotAvailable);
                } else {
                    if (location.getProvider().equals("gps")) {
                        gpsStatusText = String.format(
                                getResources().getString(R.string.messageGPSStatusSignalProvider),
                                getResources().getString(R.string.locationProviderGPS));
                    } else if (location.getProvider().equals("network")) {
                        gpsStatusText = String.format(
                                getResources().getString(R.string.messageGPSStatusSignalProvider),
                                getResources().getString(R.string.locationProviderNetwork));
                    } else {
                        gpsStatusText = String.format(
                                getResources().getString(R.string.messageGPSStatusSignalProvider),
                                location.getProvider());
                    }
                }
            }
            // get details of location object if available
            if (location != null) {
                gpsStatusText = String.format(
                        getResources().getString(R.string.messageGPSStatusSignalDetails),
                        gpsStatusText, location.getAccuracy(),
                        ((System.currentTimeMillis() - location.getTime()) / 1000),
                        location.getSpeed() );
            }
            updateLabelStatus();
            gpsStatusHandler.postDelayed(this, 1000);
        }
    }

}
