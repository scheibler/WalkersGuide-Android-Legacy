package org.walkersguide.userinterface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.walkersguide.MainActivity;
import org.walkersguide.R;
import org.walkersguide.routeobjects.POIPoint;
import org.walkersguide.routeobjects.Point;
import org.walkersguide.routeobjects.RouteObjectWrapper;
import org.walkersguide.sensors.PositionManager;
import org.walkersguide.sensors.SensorsManager;
import org.walkersguide.utils.AddressManager;
import org.walkersguide.utils.Globals;
import org.walkersguide.utils.HelperFunctions;
import org.walkersguide.utils.KeyboardManager;
import org.walkersguide.utils.ObjectParser;
import org.walkersguide.utils.POIManager;
import org.walkersguide.utils.POIPreset;
import org.walkersguide.utils.Route;
import org.walkersguide.utils.SettingsManager;

import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class POIFragment extends Fragment {

    public interface MessageFromPOIFragmentListener{
        public void switchToOtherFragment(String fragmentName);
    }

    private MessageFromPOIFragmentListener mPOIFListener;
    private static final int OBJECTDETAILS = 1;
    private static final int CHOOSEPOICATEGORIES = 2;
    private Globals globalData;
    private POIManager poiManager;
    private PositionManager positionManager;
    private SensorsManager sensorsManager;
    private SettingsManager settingsManager;
    private AddressManager addressManager;
    private KeyboardManager keyboardManager;
    private Vibrator vibrator;
    private LinearLayout mainLayout, radiusLayout, searchLayout;
    private Dialog dialog;
    private ArrayList<Point> poiList;
    private int numberOfHighSpeeds;
    private String currentAddress, currentSearchString, gpsStatusText;
    private int currentRadius;
    private Point currentLocation;
    private int currentCompassValue, tempCompassValue, lastCompassValue;
    private Handler gpsStatusHandler, progressHandler;
    private GPSStatusUpdater gpsStatusUpdater;
    private ProgressUpdater progressUpdater;
    private Toast messageToast;
    private Integer[] radiusValues;

    @Override public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (globalData == null) {
            globalData = ((Globals) getActivity().getApplicationContext());
        }
        settingsManager = globalData.getSettingsManagerInstance();
        positionManager = globalData.getPositionManagerInstance();
        poiManager = globalData.getPOIManagerInstance();
        sensorsManager = globalData.getSensorsManagerInstance();
        addressManager = globalData.getAddressManagerInstance();
        keyboardManager = globalData.getKeyboardManagerInstance();
        vibrator = (Vibrator) getActivity().getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        gpsStatusHandler = new Handler();
        gpsStatusUpdater = new GPSStatusUpdater();
        progressHandler = new Handler();
        progressUpdater = new ProgressUpdater();
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mPOIFListener = (MessageFromPOIFragmentListener) ((MainActivity) activity).getMessageFromPOIFragmentListener();
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement MessageFromPOIFragmentListener");
        }
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentAddress = "";
        currentSearchString = "";
        currentRadius = 0;
        currentCompassValue = -1;
        lastCompassValue = -1;
        tempCompassValue = -1;
        poiList = new ArrayList<Point>();
        numberOfHighSpeeds = 0;
        radiusValues = new Integer[]{125, 250, 375, 500, 750, 1000, 1500, 2000, 3500, 5000, 10000};
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        settingsManager = globalData.getSettingsManagerInstance();
        messageToast = Toast.makeText(getActivity(), "", Toast.LENGTH_SHORT);
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_pois, container, false);
        mainLayout = (LinearLayout) view.findViewById(R.id.linearLayoutMain);
        radiusLayout = (LinearLayout) mainLayout.findViewById(R.id.linearLayoutRadius);
        searchLayout = (LinearLayout) mainLayout.findViewById(R.id.linearLayoutSearch);

        // presets spinner
        Spinner spinnerPresets = (Spinner) mainLayout.findViewById(R.id.spinnerPresets);
        spinnerPresets.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                int presetId = ((POIPreset) parent.getItemAtPosition(pos)).getId();
                if (presetId == settingsManager.getPresetIdInPoiFragment()) {
                    return;
                }
                poiManager.cancel();
                POIPreset preset = settingsManager.getPOIPreset(presetId);
                preset.setPOIListStatus(POIPreset.UpdateStatus.RESETLISTPOSITION);
                settingsManager.updatePOIPreset(preset);
                settingsManager.setPresetIdInPoiFragment(presetId);
                // change radius spinner selection
                currentRadius = preset.getRange();
                // update poi list
                queryPOIListUpdate();
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        POIPresetListAdapter presetListAdapter = new POIPresetListAdapter(getActivity(),
                android.R.layout.simple_spinner_item, settingsManager.getPOIPresetList() );
        presetListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPresets.setAdapter(presetListAdapter);

        Button buttonPOIPresetSettings = (Button) mainLayout.findViewById(R.id.buttonPOIPresetSettings);
        buttonPOIPresetSettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), POIPresetsActivity.class);
                intent.putExtra("presetId", settingsManager.getPresetIdInPoiFragment() );
                startActivityForResult(intent, CHOOSEPOICATEGORIES);
            }
        });

        // Buttons
        Button buttonRefresh = (Button) mainLayout.findViewById(R.id.buttonRefresh);
        buttonRefresh.setTag(0);
        buttonRefresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (settingsManager.getPOIPresetList().size() == 0) {
                    messageToast.setText(
                        getResources().getString(R.string.labelNoPresetAvailable));
                    messageToast.show();
                    return;
                }
                Spinner spinnerAdditionalOptions = (Spinner) mainLayout.findViewById(R.id.spinnerAdditionalOptions);
                Button buttonRefresh = (Button) mainLayout.findViewById(R.id.buttonRefresh);
                int status = (Integer) buttonRefresh.getTag();
                if (status == 0) {
                    buttonRefresh.setTag(1);
                    queryPOIListUpdate();
                    if ( ((String) spinnerAdditionalOptions.getSelectedItem()).equals(getResources().getString(R.string.arrayAAAddress)) )
                        queryAddressUpdate();
                } else {
                    buttonRefresh.setTag(0);
                    poiManager.cancel();
                }
                updateUserInterface();
            }
        });
        buttonRefresh.setOnLongClickListener(new OnLongClickListener() {
            @Override public boolean onLongClick(View v) {
                Spinner spinnerAdditionalOptions = (Spinner) mainLayout.findViewById(R.id.spinnerAdditionalOptions);
                Button buttonRefresh = (Button) mainLayout.findViewById(R.id.buttonRefresh);
                int status = (Integer) buttonRefresh.getTag();
                if (status == 0) {
                    messageToast.setText(getResources().getString(R.string.messageRefreshMonitorStarted));
                    messageToast.show();
                    buttonRefresh.setTag(2);
                    queryPOIListUpdate();
                    if ( ((String) spinnerAdditionalOptions.getSelectedItem()).equals(getResources().getString(R.string.arrayAAAddress)) )
                        queryAddressUpdate();
                } else {
                    buttonRefresh.setTag(0);
                    poiManager.cancel();
                }
                updateUserInterface();
                return true;
            }
        });

        Button buttonFilterPOIList = (Button) mainLayout.findViewById(R.id.buttonFilterPOIList);
        buttonFilterPOIList.setTag(0);
        buttonFilterPOIList.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Button buttonFilterPOIList = (Button) mainLayout.findViewById(R.id.buttonFilterPOIList);
                if ((Integer) buttonFilterPOIList.getTag() == 0) {
                    buttonFilterPOIList.setTag(1);
                } else {
                    buttonFilterPOIList.setTag(0);
                }
                updateUserInterface();
                updatePOIList();
            }
        });

        Spinner spinnerAdditionalOptions = (Spinner) mainLayout.findViewById(R.id.spinnerAdditionalOptions);
        spinnerAdditionalOptions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (settingsManager.getPOIPresetList().size() == 0) {
                    messageToast.setText(
                        getResources().getString(R.string.labelNoPresetAvailable));
                    messageToast.show();
                    return;
                }
                String oldOption = ((String) parent.getItemAtPosition(
                        settingsManager.getValueFromTemporaryPOIFragmentSettings("spinnerAdditionalOptions") ));
                String newOption = ((String) parent.getItemAtPosition(pos));
                if (oldOption.equals(newOption)) {
                    return;
                }
                if (newOption.equals(getResources().getString(R.string.arrayAAAddress))) {
                    queryAddressUpdate();
                }
                settingsManager.addToTemporaryPOIFragmentSettings("spinnerAdditionalOptions", pos);
                updateLabelStatus();
                updateUserInterface();
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        ArrayList<CharSequence> listAdditionalOptions = new ArrayList<CharSequence>(Arrays.asList(
                getResources().getStringArray(R.array.arrayAdditionalOptionsPF) ));
        AdditionalOptionsAdapter adapterAdditionalOptions = new AdditionalOptionsAdapter(
                getActivity(), android.R.layout.simple_list_item_1, listAdditionalOptions);
        adapterAdditionalOptions.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAdditionalOptions.setAdapter(adapterAdditionalOptions);

        // radius layout
        Button buttonSmallerRadius = (Button) radiusLayout.findViewById(R.id.buttonSmallerRadius);
        buttonSmallerRadius.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                poiManager.cancel();
                Spinner spinnerRadius = (Spinner) radiusLayout.findViewById(R.id.spinnerRadius);
                if (spinnerRadius.getSelectedItemPosition() > 0) {
                    currentRadius = radiusValues[spinnerRadius.getSelectedItemPosition()-1];
                    queryPOIListUpdate();
                }
            }
        });

        Spinner spinnerRadius = (Spinner) radiusLayout.findViewById(R.id.spinnerRadius);
        spinnerRadius.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                POIPreset preset = settingsManager.getPOIPreset(settingsManager.getPresetIdInPoiFragment());
                if (preset.getRange() == (Integer) parent.getItemAtPosition(pos)) {
                    return;
                }
                poiManager.cancel();
                currentRadius = (Integer) parent.getItemAtPosition(pos);
                queryPOIListUpdate();
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        ArrayList<Integer> radiusList = new ArrayList<Integer>(Arrays.asList(radiusValues));
        RadiusListAdapter radiusAdapter = new RadiusListAdapter(
                getActivity(), android.R.layout.simple_list_item_1, radiusList);
        radiusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRadius.setAdapter(radiusAdapter);

        Button buttonBiggerRadius = (Button) radiusLayout.findViewById(R.id.buttonBiggerRadius);
        buttonBiggerRadius.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                poiManager.cancel();
                Spinner spinnerRadius = (Spinner) radiusLayout.findViewById(R.id.spinnerRadius);
                if (spinnerRadius.getSelectedItemPosition() < radiusValues.length-1) {
                    currentRadius = radiusValues[spinnerRadius.getSelectedItemPosition()+1];
                    queryPOIListUpdate();
                }
            }
        });

        // search layout
        EditText searchField = (EditText) searchLayout.findViewById(R.id.editSearch);
        searchField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (event != null) {
                    poiManager.cancel();
                    EditText searchField = (EditText) searchLayout.findViewById(R.id.editSearch);
                    currentSearchString = searchField.getText().toString();
                    queryPOIListUpdate();
                    InputMethodManager inputManager = (InputMethodManager)
                            getActivity().getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                   inputManager.hideSoftInputFromWindow(
                           searchField.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                }
                return false;
            }
        });

        Button buttonDelete = (Button) searchLayout.findViewById(R.id.buttonDelete);
        buttonDelete.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                EditText searchField = (EditText) searchLayout.findViewById(R.id.editSearch);
                searchField.setText("");
                currentSearchString = "";
                searchField.postDelayed(new Runnable() {
                    public void run() {
                        EditText searchField = (EditText) mainLayout.findViewById(R.id.editSearch);
                        searchField.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN , 0, 0, 0));
                        searchField.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP , 0, 0, 0));
                    }
                }, 200);
            }
        });
        buttonDelete.setOnLongClickListener(new OnLongClickListener() {
            @Override public boolean onLongClick(View v) {
                EditText searchField = (EditText) searchLayout.findViewById(R.id.editSearch);
                // delete contents of search field
                searchField.setText("");
                currentSearchString = "";
                // disable keyboard if shown
                InputMethodManager inputManager = (InputMethodManager)
                        getActivity().getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
               inputManager.hideSoftInputFromWindow(
                       searchField.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                // switch additionalOptions spinner to "disabled"
                Spinner spinnerAdditionalOptions = (Spinner) mainLayout.findViewById(R.id.spinnerAdditionalOptions);
                ArrayAdapter<String> adapterAdditionalOptions =
                    (ArrayAdapter<String>) spinnerAdditionalOptions.getAdapter();
                int index = adapterAdditionalOptions.getPosition(
                        getResources().getString(R.string.arrayAADisabled));
                if (index == -1)
                    index = 0;
                spinnerAdditionalOptions.setSelection(index);
                settingsManager.addToTemporaryPOIFragmentSettings("spinnerAdditionalOptions", index);
                updateUserInterface();
                // query new data without search string from server
                poiManager.cancel();
                queryPOIListUpdate();
                return true;
            }
        });

        ListView listview = (ListView) mainLayout.findViewById(R.id.listPOIs);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, final View view,
                    int position, long id) {
                RouteObjectWrapper object = new RouteObjectWrapper(
                        (Point) parent.getItemAtPosition(position) );
                if (settingsManager.getValueFromTemporaryPOIFragmentSettings("routeRequestPosition") != null) {
                    POIPoint poi = null;
                    if (object.getStation() != null) {
                        poi = object.getStation();
                    } else if (object.getPOI() != null) {
                        poi = object.getPOI();
                    }
                    if (poi != null && poi.getEntranceList().size() > 0) {
                        showEntrancesDialog(poi, position);
                    } else {
                        Route sourceRoute = settingsManager.getRouteRequest();
                        sourceRoute.replaceRouteObjectAtIndex(settingsManager.getValueFromTemporaryPOIFragmentSettings("routeRequestPosition"), object);
                        settingsManager.setRouteRequest(sourceRoute);
                        mPOIFListener.switchToOtherFragment("start");
                    }
                } else {
                    Intent intent = new Intent(getActivity(), RouteObjectDetailsActivity.class);
                    intent.putExtra("route_object", object.toJson().toString());
                    startActivityForResult(intent, OBJECTDETAILS);
                }
            }
        });
        listview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override public boolean onItemLongClick(AdapterView<?> parent, final View view,
                        int position, long id) {
                showActionsMenuDialog(position);
                return true;
            }
        });
        // if there are no list entrys
        TextView textViewEmptyListView = (TextView) mainLayout.findViewById(R.id.labelEmptyList);
        if (settingsManager.getPOIPresetList().size() == 0)
            textViewEmptyListView.setText(getResources().getString(R.string.labelNoPresetAvailable));
        else
            textViewEmptyListView.setText(getResources().getString(R.string.labelFoundNoPOI));
        listview.setEmptyView(textViewEmptyListView);

        return view;
    }

    @Override public void onPause() {
        super.onPause();
        Button buttonRefresh = (Button) mainLayout.findViewById(R.id.buttonRefresh);
        settingsManager.addToTemporaryPOIFragmentSettings("buttonRefreshStatus",
                (Integer) buttonRefresh.getTag());
        Button buttonFilterPOIList = (Button) mainLayout.findViewById(R.id.buttonFilterPOIList);
        settingsManager.addToTemporaryPOIFragmentSettings("buttonFilterPOIListStatus",
                (Integer) buttonFilterPOIList.getTag());
        ListView listview = (ListView) mainLayout.findViewById(R.id.listPOIs);
        settingsManager.addToTemporaryPOIFragmentSettings("lastListPosition",
                listview.getFirstVisiblePosition());
        settingsManager.addToTemporaryPOIFragmentSettings("routeRequestPosition", null);
        gpsStatusHandler.removeCallbacks(gpsStatusUpdater);
        progressHandler.removeCallbacks(progressUpdater);
        positionManager.setPositionListener(null);
        sensorsManager.setSensorsListener(null);
        poiManager.cancel();
    }

    @Override public void onResume() {
        super.onResume();	
        if (globalData == null) {
            globalData = ((Globals) getActivity().getApplicationContext());
        }
        settingsManager = globalData.getSettingsManagerInstance();
        sensorsManager.setSensorsListener(new MySensorsListener());
        positionManager.setPositionListener(new MyPositionListener());
        poiManager.setPOIListener(new MyPOIListener());
        addressManager.setAddressListener(new MyAddressListener());
        keyboardManager.setKeyboardListener(new MyKeyboardListener());
        // start progress handler
        progressHandler.postDelayed(progressUpdater, 100);
        // restore user interface
        Spinner spinnerAdditionalOptions = (Spinner) mainLayout.findViewById(R.id.spinnerAdditionalOptions);
        Button buttonRefresh = (Button) mainLayout.findViewById(R.id.buttonRefresh);
        buttonRefresh.setTag(1);
        if (settingsManager.getValueFromTemporaryPOIFragmentSettings("buttonRefreshStatus") != null) {
            if (settingsManager.getValueFromTemporaryPOIFragmentSettings("buttonRefreshStatus") == 2) {
                buttonRefresh.setTag(2);
            }
            if (settingsManager.getValueFromTemporaryPOIFragmentSettings("buttonRefreshStatus") == -1) {
                buttonRefresh.setTag(0);
            }
        }
        Button buttonFilterPOIList = (Button) mainLayout.findViewById(R.id.buttonFilterPOIList);
        if (settingsManager.getValueFromTemporaryPOIFragmentSettings("buttonFilterPOIListStatus") != null)
            buttonFilterPOIList.setTag(settingsManager.getValueFromTemporaryPOIFragmentSettings("buttonFilterPOIListStatus"));
        // set current poi preset range
        if (settingsManager.getPOIPresetList().size() > 0) {
            POIPreset preset = settingsManager.getPOIPreset(settingsManager.getPresetIdInPoiFragment());
            if (preset == null) {
                preset = settingsManager.getPOIPresetList().get(0);
                settingsManager.setPresetIdInPoiFragment(preset.getId());
            }
            currentRadius = preset.getRange();
        } else {
            // no poi preset category available
            buttonRefresh.setTag(0);
            ArrayAdapter<String> adapterAdditionalOptions =
                (ArrayAdapter<String>) spinnerAdditionalOptions.getAdapter();
            settingsManager.addToTemporaryPOIFragmentSettings("spinnerAdditionalOptions",
                    adapterAdditionalOptions.getPosition( getResources().getString(R.string.arrayAADisabled)) );
        }
        updateUserInterface(  );
        // update poi list and address
        queryPOIListUpdate();
        if ( ((String) spinnerAdditionalOptions.getSelectedItem()).equals(getResources().getString(R.string.arrayAAAddress)) )
            queryAddressUpdate();
    }

    public synchronized void updateUserInterface() {
        // refresh button
        Button buttonRefresh = (Button) mainLayout.findViewById(R.id.buttonRefresh);
        int status = (Integer) buttonRefresh.getTag();
        if (status == 0) {
            buttonRefresh.setText(getResources().getString(R.string.buttonRefresh));
        } else if (status == 1) {
            buttonRefresh.setText(getResources().getString(R.string.buttonRefreshClicked));
        } else {
            buttonRefresh.setText(getResources().getString(R.string.buttonRefreshLongClicked));
        }
        // filter button
        Button buttonFilterPOIList = (Button) mainLayout.findViewById(R.id.buttonFilterPOIList);
        status = (Integer) buttonFilterPOIList.getTag();
        if (status > 0)
            buttonFilterPOIList.setText(getResources().getString(R.string.buttonFilterPOIListClicked));
        else
            buttonFilterPOIList.setText(getResources().getString(R.string.buttonFilterPOIList));
        // spinner additionalOptions
        // first: set selection
        Spinner spinnerAdditionalOptions = (Spinner) mainLayout.findViewById(R.id.spinnerAdditionalOptions);
        if (settingsManager.getValueFromTemporaryPOIFragmentSettings("spinnerAdditionalOptions") != null) {
            spinnerAdditionalOptions.setSelection(
                    settingsManager.getValueFromTemporaryPOIFragmentSettings("spinnerAdditionalOptions"));
        } else {
            ArrayAdapter<String> adapterAdditionalOptions =
                (ArrayAdapter<String>) spinnerAdditionalOptions.getAdapter();
            int index = adapterAdditionalOptions.getPosition(
                    getResources().getString(R.string.arrayAADisabled));
            if (index == -1)
                index = 0;
            spinnerAdditionalOptions.setSelection(index);
            settingsManager.addToTemporaryPOIFragmentSettings("spinnerAdditionalOptions", index);
        }
        // show sublayouts, depends on the spinner, defined above
        TextView labelStatus = (TextView) mainLayout.findViewById(R.id.labelStatus);
        if ( ((String) spinnerAdditionalOptions.getSelectedItem()).equals(getResources().getString(R.string.arrayAADisabled)) ) {
            labelStatus.setVisibility(View.GONE);
            searchLayout.setVisibility(View.GONE);
            radiusLayout.setVisibility(View.GONE);
        } else if ( ((String) spinnerAdditionalOptions.getSelectedItem()).equals(getResources().getString(R.string.arrayAARadius)) ) {
            labelStatus.setVisibility(View.GONE);
            searchLayout.setVisibility(View.GONE);
            radiusLayout.setVisibility(View.VISIBLE);
            Spinner spinnerRadius = (Spinner) radiusLayout.findViewById(R.id.spinnerRadius);
            RadiusListAdapter radiusAdapter = (RadiusListAdapter) spinnerRadius.getAdapter();
            spinnerRadius.setSelection(radiusAdapter.getPosition(currentRadius));
        } else if ( ((String) spinnerAdditionalOptions.getSelectedItem()).equals(getResources().getString(R.string.arrayAASearch)) ) {
            labelStatus.setVisibility(View.GONE);
            radiusLayout.setVisibility(View.GONE);
            searchLayout.setVisibility(View.VISIBLE);
        } else {
            radiusLayout.setVisibility(View.GONE);
            searchLayout.setVisibility(View.GONE);
            labelStatus.setVisibility(View.VISIBLE);
        }

        // poi presets spinner
        Spinner spinnerPresets = (Spinner) mainLayout.findViewById(R.id.spinnerPresets);
        POIPresetListAdapter presetsAdapter = (POIPresetListAdapter) spinnerPresets.getAdapter();
        presetsAdapter.notifyDataSetChanged();
        int index = presetsAdapter.getPosition(
                settingsManager.getPOIPreset(settingsManager.getPresetIdInPoiFragment()) );
        if (index == -1)
            spinnerPresets.setSelection( 1 );
        else
            spinnerPresets.setSelection( index );
    }

    public void updateLabelStatus() {
        Spinner spinnerAdditionalOptions = (Spinner) mainLayout.findViewById(R.id.spinnerAdditionalOptions);
        TextView labelStatus = (TextView) mainLayout.findViewById(R.id.labelStatus);
        if ( ((String) spinnerAdditionalOptions.getSelectedItem()).equals(getResources().getString(R.string.arrayAAAddress)) ) {
            labelStatus.setText( String.format(
                    getResources().getString(R.string.messageCurrentAddress), currentAddress));
        } else if ( ((String) spinnerAdditionalOptions.getSelectedItem()).equals(getResources().getString(R.string.arrayAAGPSStatus)) ) {
            labelStatus.setText(gpsStatusText);
        } else {
            labelStatus.setText("");
        }
    }

    public void updatePOIList() {
        POIPreset preset = settingsManager.getPOIPreset(settingsManager.getPresetIdInPoiFragment());
        Button buttonFilterPOIList = (Button) mainLayout.findViewById(R.id.buttonFilterPOIList);
        ListView listview = (ListView) mainLayout.findViewById(R.id.listPOIs);
        int lastListPosition = listview.getFirstVisiblePosition();
        //System.out.println("xx before: pos = " + lastListPosition + ", adapter = " + listview.getAdapter());
        if (settingsManager.getValueFromTemporaryPOIFragmentSettings("lastListPosition") != null && listview.getAdapter() == null) {
            lastListPosition = settingsManager.getValueFromTemporaryPOIFragmentSettings("lastListPosition");
        }
        ArrayAdapter<Point> adapterList = new ArrayAdapter<Point>(getActivity(),
                android.R.layout.simple_list_item_1, preset.getPOIList());
        if ((Integer) buttonFilterPOIList.getTag() > 0) {
            ArrayList<Point> filteredPOIList = new ArrayList<Point>();
            for(Point poi : preset.getPOIList()) {
                if ((poi.getBearing() > 285) || (poi.getBearing() < 75)) {
                    filteredPOIList.add( poi );
                }
            }
            Collections.sort(filteredPOIList);
            adapterList = new ArrayAdapter<Point>(getActivity(),
                    android.R.layout.simple_list_item_1, filteredPOIList);
        }
        listview.setAdapter(adapterList);
        // restore list position
        if (preset.getPOIListStatus() != POIPreset.UpdateStatus.RESETLISTPOSITION) {
            listview.setSelectionFromTop(lastListPosition, 0);
        }
        // notify user, when data downloaded successfully
        if (preset.getDownloadedNewData()) {
            vibrator.vibrate(300);
            preset.setDownloadedNewData(false);
        }
        // set status values of preset to default
        preset.setPOIListStatus(POIPreset.UpdateStatus.UNCHANGED);
        settingsManager.updatePOIPreset(preset);
    }

    private void showEntrancesDialog(POIPoint poi, int listIndex) {
        dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.dialog_scrollable_empty);
        dialog.setCanceledOnTouchOutside(false);
        LinearLayout dialogLayout = (LinearLayout) dialog.findViewById(R.id.linearLayoutMain);
        LayoutParams lp = new LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT );
        TextView label;
        // heading and description
        label = new TextView(getActivity());
        label.setLayoutParams(lp);
        label.setText( String.format(
                    getResources().getString(R.string.labelEntranceDialogHeading),
                    poi.getName() ));
        dialogLayout.addView(label);
        label = new TextView(getActivity());
        label.setLayoutParams(lp);
        if (poi.getEntranceList().size() == 1) {
            label.setText(getResources().getString(R.string.labelEntranceDialogSingle));
        } else {
            label.setText( String.format(
                        getResources().getString(R.string.labelEntranceDialogMultiple),
                        poi.getEntranceList().size() ));
        }
        dialogLayout.addView(label);

        Button buttonNoEntrance = new Button(getActivity());
        buttonNoEntrance.setLayoutParams(lp);
        buttonNoEntrance.setId(listIndex);
        buttonNoEntrance.setText(getResources().getString(R.string.buttonNoEntranceButCoordinates));
        buttonNoEntrance.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                ListView listView = (ListView) mainLayout.findViewById(R.id.listPOIs);
                RouteObjectWrapper object = new RouteObjectWrapper(
                        (Point) listView.getItemAtPosition(view.getId()) );
                Route sourceRoute = settingsManager.getRouteRequest();
                sourceRoute.replaceRouteObjectAtIndex(settingsManager.getValueFromTemporaryPOIFragmentSettings("routeRequestPosition"), object);
                settingsManager.setRouteRequest(sourceRoute);
                dialog.dismiss();
                mPOIFListener.switchToOtherFragment("start");
            }
        });
        dialogLayout.addView(buttonNoEntrance);

        int index = 0;
        for (POIPoint entrance : poi.getEntranceList()) {
            Button buttonEntrance = new Button(getActivity());
            buttonEntrance.setLayoutParams(lp);
            buttonEntrance.setId(index*1000000 + listIndex);
            buttonEntrance.setText(String.format(
                    getResources().getString(R.string.labelEntranceDropDown),
                    entrance.getName(),
                    currentLocation.distanceTo(entrance),
                    HelperFunctions.getFormatedDirection(currentLocation.bearingTo(entrance)-currentCompassValue) ));
            buttonEntrance.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    int entranceIndex = Math.round(view.getId()/1000000);
                    int listIndex = view.getId() - entranceIndex*1000000;
                    ListView listView = (ListView) mainLayout.findViewById(R.id.listPOIs);
                    RouteObjectWrapper object = new RouteObjectWrapper(
                            (Point) listView.getItemAtPosition(listIndex) );
                    // get selected entrance
                    POIPoint poi = object.getPOI();
                    if (poi == null)
                        poi = object.getStation();
                    POIPoint entrance = poi.getEntranceList().get(entranceIndex);
                    // create a new object with the lat and lon values of the entrance
                    RouteObjectWrapper newObject = ObjectParser.parseRouteObject(object.toJson());
                    newObject.getPoint().setName(object.getPoint().getName()
                            + " (" + entrance.getName() + ")");
                    newObject.getPoint().setLatitude(entrance.getLatitude());
                    newObject.getPoint().setLongitude(entrance.getLongitude());
                    Route sourceRoute = settingsManager.getRouteRequest();
                    sourceRoute.replaceRouteObjectAtIndex(settingsManager.getValueFromTemporaryPOIFragmentSettings("routeRequestPosition"), newObject);
                    settingsManager.setRouteRequest(sourceRoute);
                    dialog.dismiss();
                    mPOIFListener.switchToOtherFragment("start");
                }
            });
            dialogLayout.addView(buttonEntrance);
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

    private void showActionsMenuDialog(int objectIndex) {
        ListView listView = (ListView) mainLayout.findViewById(R.id.listPOIs);
        RouteObjectWrapper routeObject = new RouteObjectWrapper(
                (Point) listView.getItemAtPosition(objectIndex) );
        dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.dialog_scrollable_empty);
        dialog.setCanceledOnTouchOutside(false);
        LinearLayout dialogLayout = (LinearLayout) dialog.findViewById(R.id.linearLayoutMain);
        TextView label;
        LayoutParams lp = new LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT );
        LayoutParams lpMarginTop = new LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT );
        ((MarginLayoutParams) lpMarginTop).topMargin =
                Math.round(15 * getActivity().getApplicationContext().getResources().getDisplayMetrics().density);
        // heading
        label = new TextView(getActivity());
        label.setLayoutParams(lp);
        label.setText( String.format(
                    getResources().getString(R.string.labelActionsRoutePointDescription),
                    routeObject.getPoint().getName() ));
        dialogLayout.addView(label);

        Button buttonObjectDetails = new Button(getActivity());
        buttonObjectDetails.setLayoutParams(lp);
        buttonObjectDetails.setId(objectIndex);
        buttonObjectDetails.setText(getResources().getString(R.string.buttonRouteObjectDetails));
        buttonObjectDetails.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                ListView listView = (ListView) mainLayout.findViewById(R.id.listPOIs);
                RouteObjectWrapper object = new RouteObjectWrapper(
                        (Point) listView.getItemAtPosition(view.getId()) );
                Intent intent = new Intent(getActivity(), RouteObjectDetailsActivity.class);
                intent.putExtra("route_object", object.toJson().toString());
                startActivity(intent);
                dialog.dismiss();
            }
        });
        dialogLayout.addView(buttonObjectDetails);

        Button buttonSimulation = new Button(getActivity());
        buttonSimulation.setLayoutParams(lp);
        buttonSimulation.setId(objectIndex);
        if ((currentLocation.distanceTo(routeObject.getPoint()) == 0) && (positionManager.getStatus() == PositionManager.Status.SIMULATION)) {
            buttonSimulation.setText(getResources().getString(R.string.buttonStopSimulation));
        } else {
            buttonSimulation.setText(getResources().getString(R.string.buttonPointSimulation));
        }
        buttonSimulation.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                ListView listView = (ListView) mainLayout.findViewById(R.id.listPOIs);
                RouteObjectWrapper object = new RouteObjectWrapper(
                        (Point) listView.getItemAtPosition(view.getId()) );
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

        Button buttonToFavorites = new Button(getActivity());
        buttonToFavorites.setLayoutParams(lp);
        buttonToFavorites.setId(objectIndex);
        if (settingsManager.favoritesContains(routeObject)) {
            buttonToFavorites.setText(getResources().getString(R.string.buttonToFavoritesClicked));
        } else {
            buttonToFavorites.setText(getResources().getString(R.string.buttonToFavorites));
        }
        buttonToFavorites.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                ListView listView = (ListView) mainLayout.findViewById(R.id.listPOIs);
                RouteObjectWrapper object = new RouteObjectWrapper(
                        (Point) listView.getItemAtPosition(view.getId()) );
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

        // new object
        label = new TextView(getActivity());
        label.setLayoutParams(lpMarginTop);
        label.setText(getResources().getString(R.string.labelAsNewRouteObject));
        dialogLayout.addView(label);
        Route sourceRoute = settingsManager.getRouteRequest();
        int index = 0;
        for (RouteObjectWrapper object : sourceRoute.getRouteList()) {
            Button buttonRouteObject = new Button(getActivity());
            buttonRouteObject.setLayoutParams(lp);
            buttonRouteObject.setId(index*1000000 + objectIndex);
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
                    int routeRequestIndex = Math.round(view.getId()/1000000);
                    int listIndex = view.getId() - routeRequestIndex*1000000;
                    ListView listView = (ListView) mainLayout.findViewById(R.id.listPOIs);
                    RouteObjectWrapper object = new RouteObjectWrapper(
                            (Point) listView.getItemAtPosition(listIndex) );
                    Route sourceRoute = settingsManager.getRouteRequest();
                    sourceRoute.replaceRouteObjectAtIndex(routeRequestIndex, object);
                    settingsManager.setRouteRequest(sourceRoute);
                    dialog.dismiss();
                    mPOIFListener.switchToOtherFragment("start");
                }
            });
            dialogLayout.addView(buttonRouteObject);
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

    public void queryPOIListUpdate() {
        Button buttonRefresh = (Button) mainLayout.findViewById(R.id.buttonRefresh);
        // if we refresh the poi list, change the button label
        if ((Integer) buttonRefresh.getTag() == 0) {
            buttonRefresh.setTag(1);
            updateUserInterface();
        }
        // if the monitor is activated, use the last compass value, otherwise the poi list refreshes
        // much to often
        if ((Integer) buttonRefresh.getTag() == 2) {
            boolean isInsidePublicTransport = false;
            if (numberOfHighSpeeds > 0)
                isInsidePublicTransport = true;
            poiManager.updatePOIList(settingsManager.getPresetIdInPoiFragment(), currentRadius,
                    currentLocation, lastCompassValue, currentSearchString, isInsidePublicTransport);
        } else {
            poiManager.updatePOIList(settingsManager.getPresetIdInPoiFragment(), currentRadius,
                    currentLocation, currentCompassValue, currentSearchString, false);
        }
    }

    public void queryAddressUpdate() {
        if (numberOfHighSpeeds == 0)
            addressManager.updateAddress(currentLocation);
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
                        mPOIFListener.switchToOtherFragment("start");
                    else
                        mPOIFListener.switchToOtherFragment("router");
                    break;
                }
            case CHOOSEPOICATEGORIES:
                // This is the standard resultCode that is sent back if the
                // activity crashed or didn't doesn't supply an explicit result.
                if (resultCode == getActivity().RESULT_CANCELED) {
                    break;
                } else {
                    int presetId = Integer.parseInt(data.getStringExtra("presetId"));
                    settingsManager.setPresetIdInPoiFragment(presetId);
                    break;
                }
            default:
                break;
        }
    }

    private class MyKeyboardListener implements KeyboardManager.KeyboardListener {
        public void longPressed(KeyEvent event) {
            Spinner spinnerAdditionalOptions;
            ArrayAdapter<String> adapterAdditionalOptions;
            int newIndex;
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    spinnerAdditionalOptions = (Spinner) mainLayout.findViewById(R.id.spinnerAdditionalOptions);
                    adapterAdditionalOptions = (ArrayAdapter<String>) spinnerAdditionalOptions.getAdapter();
                    newIndex = settingsManager.getValueFromTemporaryPOIFragmentSettings("spinnerAdditionalOptions") + 1;
                    if (newIndex == -1 || newIndex == adapterAdditionalOptions.getCount())
                        newIndex = 0;
                    spinnerAdditionalOptions.setSelection(newIndex);
                    settingsManager.addToTemporaryPOIFragmentSettings("spinnerAdditionalOptions", newIndex);
                    updateUserInterface();
                    messageToast.setText(adapterAdditionalOptions.getItem(newIndex));
                    messageToast.show();
                    return;
                case KeyEvent.KEYCODE_VOLUME_UP:
                    spinnerAdditionalOptions = (Spinner) mainLayout.findViewById(R.id.spinnerAdditionalOptions);
                    adapterAdditionalOptions = (ArrayAdapter<String>) spinnerAdditionalOptions.getAdapter();
                    newIndex = settingsManager.getValueFromTemporaryPOIFragmentSettings("spinnerAdditionalOptions") - 1;
                    if (newIndex < 0)
                        newIndex = adapterAdditionalOptions.getCount() - 1;
                    spinnerAdditionalOptions.setSelection(newIndex);
                    settingsManager.addToTemporaryPOIFragmentSettings("spinnerAdditionalOptions", newIndex);
                    updateUserInterface();
                    messageToast.setText(adapterAdditionalOptions.getItem(newIndex));
                    messageToast.show();
                    return;
                default:
                    return;
            }
        }
    }

    private class MyPOIListener implements POIManager.POIListener {
        public void poiPresetUpdateSuccessful() {
            // first change refresh button label
            Button buttonRefresh = (Button) mainLayout.findViewById(R.id.buttonRefresh);
            if ((Integer) buttonRefresh.getTag() == 1) {
                buttonRefresh.setTag(0);
                updateUserInterface();
            }
            POIPreset preset = settingsManager.getPOIPreset(settingsManager.getPresetIdInPoiFragment());
            ListView listview = (ListView) mainLayout.findViewById(R.id.listPOIs);
            if (preset.getPOIListStatus() != POIPreset.UpdateStatus.UNCHANGED) {
                updatePOIList();
            } else if (listview.getAdapter() == null) {
                updatePOIList();
            }
        }

        public void poiPresetUpdateFailed(String error) {
            // first change refresh button label
            Button buttonRefresh = (Button) mainLayout.findViewById(R.id.buttonRefresh);
            buttonRefresh.setTag(-1);
            Intent intent = new Intent(getActivity(), DialogActivity.class);
            intent.putExtra("message", error);
            startActivity(intent);
        }
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
            currentLocation = new Point(
                    getResources().getString(R.string.locationNameCurrentPosition), location);
            if (location.getSpeed() > 3.0) {
                numberOfHighSpeeds = 2;
            } else if (numberOfHighSpeeds > 0) {
                numberOfHighSpeeds -= 1;
            }

            // continuous update if auto refresh is activated
            Spinner spinnerAdditionalOptions = (Spinner) mainLayout.findViewById(R.id.spinnerAdditionalOptions);
            Button buttonRefresh = (Button) mainLayout.findViewById(R.id.buttonRefresh);
            if ((Integer) buttonRefresh.getTag() == 2) {
                queryPOIListUpdate();
                if ( ((String) spinnerAdditionalOptions.getSelectedItem()).equals(getResources().getString(R.string.arrayAAAddress)) )
                    queryAddressUpdate();
            }
        }
    }

    private class MySensorsListener implements SensorsManager.SensorsListener {
        public void compassValueChanged(int degree) {
            currentCompassValue = degree;
            Button buttonRefresh = (Button) mainLayout.findViewById(R.id.buttonRefresh);
            if ( ((Integer) buttonRefresh.getTag() == 2)) {
                int diff = Math.abs(currentCompassValue - lastCompassValue);
                if (diff > 180)
                    diff = 360 - diff;
                int tempDiff = Math.abs(currentCompassValue - tempCompassValue);
                if (tempDiff > 180)
                    tempDiff = 360 - tempDiff;
                tempCompassValue = currentCompassValue;
                if (diff > 60 && tempDiff < 5) {
                    lastCompassValue = currentCompassValue;
                    queryPOIListUpdate();
                }
            }
        }
        public void shakeDetected() {
            if (settingsManager.getPOIPresetList().size() == 0) {
                messageToast.setText(
                    getResources().getString(R.string.labelNoPresetAvailable));
                messageToast.show();
                return;
            }
            if (settingsManager.getShakeForNextRoutePoint() == true) {
                Spinner spinnerAdditionalOptions = (Spinner) mainLayout.findViewById(R.id.spinnerAdditionalOptions);
                Button buttonRefresh = (Button) mainLayout.findViewById(R.id.buttonRefresh);
                if ((Integer) buttonRefresh.getTag() == 0) {
                    buttonRefresh.setTag(1);
                    updateUserInterface();
                    queryPOIListUpdate();
                    if ( ((String) spinnerAdditionalOptions.getSelectedItem()).equals(getResources().getString(R.string.arrayAAAddress)) )
                        queryAddressUpdate();
                    messageToast.setText("Aktualisiere");
                    messageToast.show();
                } else if ((Integer) buttonRefresh.getTag() == 1) {
                    buttonRefresh.setTag(0);
                    poiManager.cancel();
                    updateUserInterface();
                    messageToast.setText("Abgebrochen");
                    messageToast.show();
                }
            }
        }
    }

    public class POIPresetListAdapter extends ArrayAdapter<POIPreset> {
        private Context ctx;
        private LayoutInflater m_inflater = null;
        public ArrayList<POIPreset> presetArray;

        public POIPresetListAdapter(Context context, int textViewResourceId, ArrayList<POIPreset> array) {
            super(context, textViewResourceId);
            this.presetArray = array;
            this.ctx = context;
            m_inflater = LayoutInflater.from(ctx);
        }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            EntryHolder holder;
            if (convertView == null) {
                holder = new EntryHolder();
                convertView = m_inflater.inflate(R.layout.table_row_one_column, parent, false);
                holder.contents= (TextView) convertView.findViewById(R.id.labelColumnOne);
                convertView.setTag(holder);
            } else {
                holder = (EntryHolder) convertView.getTag();
            }
            POIPreset preset = getItem(position);
            holder.contents.setText( String.format(
                        getResources().getString(R.string.labelPOIPresetComboClosed),
                        preset.getName(), preset.getRange() ));
            return convertView;
        }

        @Override public View getDropDownView(int position, View convertView, ViewGroup parent) {
            EntryHolder holder;
            if (convertView == null) {
                holder = new EntryHolder();
                convertView = m_inflater.inflate(R.layout.table_row_one_column, parent, false);
                holder.contents= (TextView) convertView.findViewById(R.id.labelColumnOne);
                convertView.setTag(holder);
            } else {
                holder = (EntryHolder) convertView.getTag();
            }
            POIPreset preset = getItem(position);
            holder.contents.setText( String.format(
                        getResources().getString(R.string.labelPOIPresetComboOpened),
                        preset.getName(), preset.getRange() ));
            return convertView;
        }

        @Override public int getCount() {
            if (presetArray != null)
                return presetArray.size();
            return 0;
        }

        @Override public POIPreset getItem(int position) {
            return presetArray.get(position);
        }

        @Override public int getPosition(POIPreset item) {
            if (presetArray.contains(item))
                return presetArray.indexOf(item);
            return 0;
        }

        public void setArrayList(ArrayList<POIPreset> array) {
            this.presetArray = array;
            notifyDataSetChanged();
        }

        private class EntryHolder {
            public TextView contents;
        }
    }

    public class AdditionalOptionsAdapter extends ArrayAdapter<CharSequence> {
        private Context ctx;
        private LayoutInflater m_inflater = null;
        public ArrayList<CharSequence> additionalOptionsArray;

        public AdditionalOptionsAdapter(Context context, int textViewResourceId, ArrayList<CharSequence> array) {
            super(context, textViewResourceId);
            this.additionalOptionsArray = array;
            this.ctx = context;
            m_inflater = LayoutInflater.from(ctx);
        }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            EntryHolder holder;
            if (convertView == null) {
                holder = new EntryHolder();
                convertView = m_inflater.inflate(R.layout.table_row_one_column, parent, false);
                holder.contents= (TextView) convertView.findViewById(R.id.labelColumnOne);
                convertView.setTag(holder);
            } else {
                holder = (EntryHolder) convertView.getTag();
            }
            holder.contents.setText(getItem(position));
            return convertView;
        }

        @Override public View getDropDownView(int position, View convertView, ViewGroup parent) {
            EntryHolder holder;
            if (convertView == null) {
                holder = new EntryHolder();
                convertView = m_inflater.inflate(R.layout.table_row_one_column, parent, false);
                holder.contents= (TextView) convertView.findViewById(R.id.labelColumnOne);
                convertView.setTag(holder);
            } else {
                holder = (EntryHolder) convertView.getTag();
            }
            if (position == 0) {
                holder.contents.setText(
                            getResources().getString(R.string.arrayAADisabledOpen) );
            } else {
                holder.contents.setText(getItem(position));
            }
            return convertView;
        }

        @Override public int getCount() {
            if (additionalOptionsArray != null)
                return additionalOptionsArray.size();
            return 0;
        }

        @Override public CharSequence getItem(int position) {
            return additionalOptionsArray.get(position);
        }

        @Override public int getPosition(CharSequence item) {
            if (additionalOptionsArray.contains(item))
                return additionalOptionsArray.indexOf(item);
            return 0;
        }

        public void setArrayList(ArrayList<CharSequence> array) {
            this.additionalOptionsArray = array;
            notifyDataSetChanged();
        }

        private class EntryHolder {
            public TextView contents;
        }
    }

    public class RadiusListAdapter extends ArrayAdapter<Integer> {
        private Context ctx;
        private LayoutInflater m_inflater = null;
        public ArrayList<Integer> radiusArray;

        public RadiusListAdapter(Context context, int textViewResourceId, ArrayList<Integer> array) {
            super(context, textViewResourceId);
            this.radiusArray = array;
            this.ctx = context;
            m_inflater = LayoutInflater.from(ctx);
        }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            EntryHolder holder;
            if (convertView == null) {
                holder = new EntryHolder();
                convertView = m_inflater.inflate(R.layout.table_row_one_column, parent, false);
                holder.contents= (TextView) convertView.findViewById(R.id.labelColumnOne);
                convertView.setTag(holder);
            } else {
                holder = (EntryHolder) convertView.getTag();
            }
            Integer radiusObject = getItem(position);
            holder.contents.setText( String.format(
                        getResources().getString(R.string.labelRadiusSpinnerObject), radiusObject ));
            return convertView;
        }

        @Override public View getDropDownView(int position, View convertView, ViewGroup parent) {
            EntryHolder holder;
            if (convertView == null) {
                holder = new EntryHolder();
                convertView = m_inflater.inflate(R.layout.table_row_one_column, parent, false);
                holder.contents= (TextView) convertView.findViewById(R.id.labelColumnOne);
                convertView.setTag(holder);
            } else {
                holder = (EntryHolder) convertView.getTag();
            }
            Integer radiusObject = getItem(position);
            holder.contents.setText( String.format(
                        getResources().getString(R.string.labelRadiusSpinnerObject), radiusObject ));
            return convertView;
        }

        @Override public int getCount() {
            if (radiusArray != null)
                return radiusArray.size();
            return 0;
        }

        @Override public Integer getItem(int position) {
            return radiusArray.get(position);
        }

        @Override public int getPosition(Integer item) {
            if (radiusArray.contains(item))
                return radiusArray.indexOf(item);
            return 0;
        }

        public void setArrayList(ArrayList<Integer> array) {
            this.radiusArray = array;
            notifyDataSetChanged();
        }

        private class EntryHolder {
            public TextView contents;
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
                    } else if (location.getProvider().equals("fused")) {
                        gpsStatusText = String.format(
                                getResources().getString(R.string.messageGPSStatusSignalProvider),
                                getResources().getString(R.string.locationProviderFused));
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
                        location.getSpeed()*3.6 );
            }
            updateLabelStatus();
            gpsStatusHandler.postDelayed(this, 1000);
        }
    }

    private class ProgressUpdater implements Runnable {
        public void run() {
            Button buttonRefresh = (Button) mainLayout.findViewById(R.id.buttonRefresh);
            if ((Integer) buttonRefresh.getTag() == 1) {
                vibrator.vibrate(50);
            }
            progressHandler.postDelayed(this, 2000);
        }
    }
}
