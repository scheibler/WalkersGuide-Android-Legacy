package org.walkersguide.userinterface;

import java.util.ArrayList;
import java.util.Collections;

import org.walkersguide.R;
import org.walkersguide.routeobjects.Point;
import org.walkersguide.routeobjects.RouteObjectWrapper;
import org.walkersguide.utils.Globals;
import org.walkersguide.utils.PositionManager;
import org.walkersguide.utils.SettingsManager;
import org.walkersguide.utils.SourceRoute;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class HistoryActivity extends Activity {

    private enum SubView {
        ROUTES, ROUTEPOINTS, FAVORITEPOINTS
    }

    private static final int OBJECTDETAILS = 1;
    private Globals globalData;
    private SettingsManager settingsManager;
    private PositionManager positionManager;
    private Point currentLocation;
    private LinearLayout mainLayout;
    private SubView activeSubView;
    private boolean showButtons;
    private Toast messageToast;
    private Dialog dialog;

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_history);
        mainLayout = (LinearLayout) findViewById(R.id.linearLayoutMain);
        messageToast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);
        if (globalData == null) {
            globalData = ((Globals) getApplicationContext());
        }
        settingsManager = globalData.getSettingsManagerInstance();
        positionManager = globalData.getPositionManagerInstance();

        Intent sender = getIntent();
        activeSubView = SubView.ROUTES;
        showButtons = true;
        if (sender.getExtras() != null) {
            String subView = sender.getExtras().getString("subView", "");
            if (subView.equals("ROUTES")) {
                activeSubView = SubView.ROUTES;
            } else if (subView.equals("ROUTEPOINTS")) {
                activeSubView = SubView.ROUTEPOINTS;
            } else if (subView.equals("FAVORITEPOINTS")) {
                activeSubView = SubView.FAVORITEPOINTS;
            }
            if (sender.getExtras().getInt("showButtons", -1) == 0) {
                showButtons = false;
            }
        }

        // radio buttons
        RadioGroup radioHistorySource = (RadioGroup) findViewById(R.id.radioHistorySource);
        radioHistorySource.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.buttonRoutes) {
                    activeSubView = SubView.ROUTES;
                } else if (checkedId == R.id.buttonFavoritePoints) {
                    activeSubView = SubView.FAVORITEPOINTS;
                } else {
                    activeSubView = SubView.ROUTEPOINTS;
                }
                updateUserInterface();
            }
        });
        // check sub view
        if (activeSubView == SubView.ROUTES) {
            radioHistorySource.check(R.id.buttonRoutes);
        } else if (activeSubView == SubView.FAVORITEPOINTS) {
            radioHistorySource.check(R.id.buttonFavoritePoints);
        } else {
            radioHistorySource.check(R.id.buttonRoutePoints);
        }

        ListView listview = (ListView) mainLayout.findViewById(R.id.listHistory);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                Intent sender = getIntent();
                SourceRoute sourceRoute;
                if (activeSubView == SubView.ROUTES) {
                    sourceRoute = (SourceRoute) parent.getItemAtPosition(position);
                    if (sender.getExtras() != null && sender.getExtras().getInt("route", -1) > -1) {
                        settingsManager.setRouteRequest(sourceRoute);
                        finish();
                    } else {
                        showSourceRouteDetailsDialog(position);
                    }
                } else {
                    Point object = (Point) parent.getItemAtPosition(position);
                    if (sender.getExtras() != null && sender.getExtras().getInt("objectIndex", -1) > -1) {
                        sourceRoute = settingsManager.getRouteRequest();
                        sourceRoute.replaceRouteObjectAtIndex(sender.getExtras().getInt("objectIndex"), new RouteObjectWrapper(object));
                        settingsManager.setRouteRequest(sourceRoute);
                        finish();
                    } else {
                        Intent intent = new Intent(getApplicationContext(), RouteObjectDetailsActivity.class);
                        intent.putExtra("route_object", object.toJson().toString());
                        startActivity(intent);
                    }
                }
            }
        });
        listview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override public boolean onItemLongClick(AdapterView<?> parent, final View view, int position, long id) {
                if (activeSubView == SubView.ROUTES) {
                    showSourceRouteActionsMenuDialog(position);
                } else {
                    showRoutePointActionsMenuDialog(position);
                }
                return true;
            }
        });
        updateUserInterface();
    }

    private void updateUserInterface() {
        TextView labelHeader = (TextView) mainLayout.findViewById(R.id.labelHeader);
        RadioGroup radioHistorySource = (RadioGroup) findViewById(R.id.radioHistorySource);
        if (showButtons) {
            labelHeader.setVisibility(View.GONE);
            radioHistorySource.setVisibility(View.VISIBLE);
        } else {
            if (activeSubView == SubView.ROUTES) {
                labelHeader.setText(getResources().getString(R.string.labelHistoryActivityRouteHeading));
            } else if (activeSubView == SubView.FAVORITEPOINTS) {
                labelHeader.setText(getResources().getString(R.string.labelHistoryActivityFavoritePointHeading));
            } else {
                labelHeader.setText(getResources().getString(R.string.labelHistoryActivityRoutePointHeading));
            }
            radioHistorySource.setVisibility(View.GONE);
            labelHeader.setVisibility(View.VISIBLE);
        }
        ListView listview = (ListView) mainLayout.findViewById(R.id.listHistory);
        if (activeSubView == SubView.ROUTES) {
            ArrayAdapter<SourceRoute> routeAdapter = new ArrayAdapter<SourceRoute>(this,
                    android.R.layout.simple_list_item_1, settingsManager.loadSourceRoutesFromHistory() );
            listview.setAdapter(routeAdapter);
        } else {
            ArrayList<Point> pointList;
            if (activeSubView == SubView.FAVORITEPOINTS) {
                pointList = settingsManager.loadPointsFromFaorites();
            } else {
                pointList = settingsManager.loadPointsFromHistory();
            }
            for (Point point : pointList) {
                if (currentLocation != null && point != null)
                    point.addDistance(currentLocation.distanceTo(point));
            }
            Collections.sort(pointList);
            ArrayAdapter<Point> pointAdapter = new ArrayAdapter<Point>(this,
                    android.R.layout.simple_list_item_1, pointList);
            listview.setAdapter(pointAdapter);
        }
        // if there are no list entrys
        TextView textViewEmptyListView = (TextView) mainLayout.findViewById(R.id.labelEmptyList);
        if (activeSubView == SubView.ROUTES) {
            if (settingsManager.loadSourceRoutesFromHistory().size() == 0)
                textViewEmptyListView.setText(
                        getResources().getString(R.string.labelNoRoutesInHistory));
        } else if (activeSubView == SubView.FAVORITEPOINTS) {
            if (settingsManager.loadPointsFromFaorites().size() == 0)
                textViewEmptyListView.setText(
                        getResources().getString(R.string.labelNoFavoritePointsInHistory));
        } else {
            if (settingsManager.loadPointsFromHistory().size() == 0)
                textViewEmptyListView.setText(
                        getResources().getString(R.string.labelNoRoutePointsInHistory));
        }
        listview.setEmptyView(textViewEmptyListView);
    }

    private void showSourceRouteDetailsDialog(int objectIndex) {
        ListView listView = (ListView) mainLayout.findViewById(R.id.listHistory);
        SourceRoute sourceRoute = (SourceRoute) listView.getItemAtPosition(objectIndex);
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_route_details);
        dialog.setCanceledOnTouchOutside(false);
        // heading
        TextView label = (TextView) dialog.findViewById(R.id.labelRouteDescription);
        label.setText( String.format(
                    getResources().getString(R.string.labelDetailsSourceRouteDescription),
                    sourceRoute.getRouteList().get(0).getPoint().getName(),
                    sourceRoute.getRouteList().get(sourceRoute.getRouteList().size()-1).getPoint().getName() ));

        Button buttonSelect = (Button) dialog.findViewById(R.id.buttonOK);
        buttonSelect.setId(objectIndex);
        buttonSelect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                ListView listView = (ListView) mainLayout.findViewById(R.id.listHistory);
                SourceRoute sourceRoute = (SourceRoute) listView.getItemAtPosition(view.getId());
                settingsManager.setRouteRequest(sourceRoute);
                finish();
                dialog.dismiss();
            }
        });

        Button buttonCancel = (Button) dialog.findViewById(R.id.buttonCancel);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        ListView listRouteObjects = (ListView) dialog.findViewById(R.id.listRouteObjects);
        ArrayAdapter<RouteObjectWrapper> adapter = new ArrayAdapter<RouteObjectWrapper>(
                this, android.R.layout.simple_list_item_1, sourceRoute.getRouteList() );
        listRouteObjects.setAdapter(adapter);
        dialog.show();
    }

    private void showSourceRouteActionsMenuDialog(int objectIndex) {
        ListView listView = (ListView) mainLayout.findViewById(R.id.listHistory);
        SourceRoute sourceRoute = (SourceRoute) listView.getItemAtPosition(objectIndex);
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_scrollable_empty);
        dialog.setCanceledOnTouchOutside(false);
        LinearLayout dialogLayout = (LinearLayout) dialog.findViewById(R.id.linearLayoutMain);
        TextView label;
        LayoutParams lp = new LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT );
        LayoutParams lpMarginTop = new LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT );
        ((MarginLayoutParams) lpMarginTop).topMargin =
                Math.round(15 * getApplicationContext().getResources().getDisplayMetrics().density);
        // heading
        label = new TextView(this);
        label.setLayoutParams(lp);
        label.setText( String.format(
                    getResources().getString(R.string.labelActionsSourceRouteDescription),
                    sourceRoute.getRouteList().get(0).getPoint().getName(),
                    sourceRoute.getRouteList().get(sourceRoute.getRouteList().size()-1).getPoint().getName() ));
        dialogLayout.addView(label);

        Button buttonSelect = new Button(this);
        buttonSelect.setLayoutParams(lp);
        buttonSelect.setId(objectIndex);
        buttonSelect.setText(getResources().getString(R.string.buttonSelectRoute));
        buttonSelect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                ListView listView = (ListView) mainLayout.findViewById(R.id.listHistory);
                SourceRoute sourceRoute = (SourceRoute) listView.getItemAtPosition(view.getId());
                settingsManager.setRouteRequest(sourceRoute);
                finish();
                dialog.dismiss();
            }
        });
        dialogLayout.addView(buttonSelect);

        Button buttonObjectDetails = new Button(this);
        buttonObjectDetails.setLayoutParams(lp);
        buttonObjectDetails.setId(objectIndex);
        buttonObjectDetails.setText(getResources().getString(R.string.buttonRouteDetails));
        buttonObjectDetails.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                dialog.dismiss();
                showSourceRouteDetailsDialog(view.getId());
            }
        });
        dialogLayout.addView(buttonObjectDetails);

        Button buttonRemoveFromHistory = new Button(this);
        buttonRemoveFromHistory.setLayoutParams(lp);
        buttonRemoveFromHistory.setId(objectIndex);
        buttonRemoveFromHistory.setText(getResources().getString(R.string.buttonRemoveFromHistory));
        buttonRemoveFromHistory.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                ListView listView = (ListView) mainLayout.findViewById(R.id.listHistory);
                SourceRoute sourceRoute = (SourceRoute) listView.getItemAtPosition(view.getId());
                settingsManager.removeSourceRouteFromHistory(sourceRoute);
                updateUserInterface();
                dialog.dismiss();
            }
        });
        dialogLayout.addView(buttonRemoveFromHistory);

        Button buttonRemoveAllFromHistory = new Button(this);
        buttonRemoveAllFromHistory.setLayoutParams(lp);
        buttonRemoveAllFromHistory.setText(getResources().getString(R.string.buttonRemoveAllFromHistory));
        buttonRemoveAllFromHistory.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                showCleanHistoryDialog();
                dialog.dismiss();
            }
        });
        dialogLayout.addView(buttonRemoveAllFromHistory);

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

    private void showRoutePointActionsMenuDialog(int objectIndex) {
        ListView listView = (ListView) mainLayout.findViewById(R.id.listHistory);
        RouteObjectWrapper object = new RouteObjectWrapper(
                (Point) listView.getItemAtPosition(objectIndex) );
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_scrollable_empty);
        dialog.setCanceledOnTouchOutside(false);
        LinearLayout dialogLayout = (LinearLayout) dialog.findViewById(R.id.linearLayoutMain);
        TextView label;
        LayoutParams lp = new LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT );
        LayoutParams lpMarginTop = new LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT );
        ((MarginLayoutParams) lpMarginTop).topMargin =
                Math.round(15 * getApplicationContext().getResources().getDisplayMetrics().density);
        // heading
        label = new TextView(this);
        label.setLayoutParams(lp);
        label.setText( String.format(
                    getResources().getString(R.string.labelActionsRoutePointDescription),
                    object.getPoint().getName() ));
        dialogLayout.addView(label);

        Button buttonObjectDetails = new Button(this);
        buttonObjectDetails.setLayoutParams(lp);
        buttonObjectDetails.setId(objectIndex);
        buttonObjectDetails.setText(getResources().getString(R.string.buttonRouteObjectDetails));
        buttonObjectDetails.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                ListView listView = (ListView) mainLayout.findViewById(R.id.listHistory);
                RouteObjectWrapper object = new RouteObjectWrapper(
                        (Point) listView.getItemAtPosition(view.getId()) );
                Intent intent = new Intent(getApplicationContext(), RouteObjectDetailsActivity.class);
                intent.putExtra("route_object", object.toJson().toString());
                startActivity(intent);
                dialog.dismiss();
            }
        });
        dialogLayout.addView(buttonObjectDetails);

        Button buttonSimulation = new Button(this);
        buttonSimulation.setLayoutParams(lp);
        buttonSimulation.setId(objectIndex);
        if ((currentLocation.distanceTo(object.getPoint()) == 0) && (positionManager.getStatus() == PositionManager.Status.SIMULATION)) {
            buttonSimulation.setText(getResources().getString(R.string.buttonStopSimulation));
        } else {
            buttonSimulation.setText(getResources().getString(R.string.buttonPointSimulation));
        }
        buttonSimulation.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                ListView listView = (ListView) mainLayout.findViewById(R.id.listHistory);
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

        Button buttonToFavorites = new Button(this);
        buttonToFavorites.setLayoutParams(lp);
        buttonToFavorites.setId(objectIndex);
        if (settingsManager.favoritesContains(object)) {
            buttonToFavorites.setText(getResources().getString(R.string.buttonToFavoritesClicked));
        } else {
            buttonToFavorites.setText(getResources().getString(R.string.buttonToFavorites));
        }
        buttonToFavorites.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                ListView listView = (ListView) mainLayout.findViewById(R.id.listHistory);
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

        // only for point history, not for favorites
        if (activeSubView == SubView.ROUTEPOINTS) {
            Button buttonRemoveFromHistory = new Button(this);
            buttonRemoveFromHistory.setLayoutParams(lp);
            buttonRemoveFromHistory.setId(objectIndex);
            buttonRemoveFromHistory.setText(getResources().getString(R.string.buttonRemoveFromHistory));
            buttonRemoveFromHistory.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    ListView listView = (ListView) mainLayout.findViewById(R.id.listHistory);
                    RouteObjectWrapper object = new RouteObjectWrapper(
                            (Point) listView.getItemAtPosition(view.getId()) );
                    settingsManager.removePointFromHistory(object);
                    updateUserInterface();
                    dialog.dismiss();
                }
            });
            dialogLayout.addView(buttonRemoveFromHistory);
        }

        Button buttonRemoveAllFromHistory = new Button(this);
        buttonRemoveAllFromHistory.setLayoutParams(lp);
        if (activeSubView == SubView.FAVORITEPOINTS) {
            buttonRemoveAllFromHistory.setText(getResources().getString(R.string.buttonRemoveAllFromFavorites));
        } else {
            buttonRemoveAllFromHistory.setText(getResources().getString(R.string.buttonRemoveAllFromHistory));
        }
        buttonRemoveAllFromHistory.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                showCleanHistoryDialog();
                dialog.dismiss();
            }
        });
        dialogLayout.addView(buttonRemoveAllFromHistory);

        // new object
        label = new TextView(this);
        label.setLayoutParams(lpMarginTop);
        label.setText(getResources().getString(R.string.labelAsNewRouteObject));
        dialogLayout.addView(label);
        SourceRoute sourceRoute = settingsManager.getRouteRequest();
        int index = 0;
        for (RouteObjectWrapper routeObject : sourceRoute.getRouteList()) {
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
                    ListView listView = (ListView) mainLayout.findViewById(R.id.listHistory);
                    RouteObjectWrapper object = new RouteObjectWrapper(
                            (Point) listView.getItemAtPosition(view.getId()) );
                    SourceRoute sourceRoute = settingsManager.getRouteRequest();
                    sourceRoute.replaceRouteObjectAtIndex(view.getId(), object);
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

    private void showCleanHistoryDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(getResources().getString(R.string.cleanHistoryDialogTitle));
        if (activeSubView == SubView.ROUTES) {
            alertDialogBuilder.setMessage(getResources().getString(R.string.cleanRouteHistoryMessage));
        } else if (activeSubView == SubView.FAVORITEPOINTS) {
            alertDialogBuilder.setMessage(getResources().getString(R.string.cleanFavoriteListMessage));
        } else {
            alertDialogBuilder.setMessage(getResources().getString(R.string.cleanPointHistoryMessage));
        }
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setPositiveButton(
                getResources().getString(R.string.dialogYes),
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int id) {
                if (activeSubView == SubView.ROUTES) {
                    settingsManager.clearSourceRouteHistory();
                } else if (activeSubView == SubView.FAVORITEPOINTS) {
                    settingsManager.clearFavoritePointHistory();
                } else {
                    settingsManager.clearPointHistory();
                }
                updateUserInterface();
            }
        });
        alertDialogBuilder.setNegativeButton(
                getResources().getString(R.string.dialogNo),
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int id) {
                dialog.cancel();
            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    @Override public void onPause() {
        super.onPause();
        positionManager.stopGPS();
    }

    @Override public void onResume() {
        super.onResume();
        settingsManager = globalData.getSettingsManagerInstance();
        positionManager.setPositionListener(new MyPositionListener());
        positionManager.resumeGPS();
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data){
        // See which child activity is calling us back.
        switch (requestCode) {
            case OBJECTDETAILS:
                // This is the standard resultCode that is sent back if the
                // activity crashed or didn't doesn't supply an explicit result.
                if (resultCode == RESULT_CANCELED) {
                    break;
                } else {
                    finish();
                }
            default:
                break;
        }
    }

    private class MyPositionListener implements PositionManager.PositionListener {
        public void locationChanged(Location location) {
            if (location == null)
                return;
            currentLocation = new Point("", location);
            positionManager.stopGPS();
            updateUserInterface();
        }
        
        public void displayGPSSettingsDialog() {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }
    }

}
