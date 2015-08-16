package org.walkersguide.userinterface;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.walkersguide.R;
import org.walkersguide.routeobjects.FootwaySegment;
import org.walkersguide.routeobjects.Point;
import org.walkersguide.routeobjects.RouteObjectWrapper;
import org.walkersguide.sensors.PositionManager;
import org.walkersguide.utils.FilterableAdapter;
import org.walkersguide.utils.Globals;
import org.walkersguide.utils.Route;
import org.walkersguide.utils.SettingsManager;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class HistoryActivity extends AbstractActivity {

    private enum SubView {
        ROUTES, BLOCKEDWAYS, ROUTEPOINTS, FAVORITEPOINTS
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
            } else if (subView.equals("BLOCKEDWAYS")) {
                activeSubView = SubView.BLOCKEDWAYS;
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
                } else if (checkedId == R.id.buttonRoutePoints) {
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
        } else if (activeSubView == SubView.ROUTEPOINTS) {
            radioHistorySource.check(R.id.buttonRoutePoints);
        }

        EditText editSearch = (EditText) mainLayout.findViewById(R.id.editSearch);
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
                ListView listview = (ListView) mainLayout.findViewById(R.id.listHistory);
                SimpleFilterableAdapter<Object> adapter = (SimpleFilterableAdapter<Object>) listview.getAdapter();
                adapter.getFilter().filter(cs, new Filter.FilterListener() {
                    public void onFilterComplete(int count) {
                        EditText editSearch = (EditText) mainLayout.findViewById(R.id.editSearch);
                        TextView textViewEmptyListView = (TextView) mainLayout.findViewById(R.id.labelEmptyList);
                        if (count == 0) {
                            if (activeSubView == SubView.ROUTES) {
                                textViewEmptyListView.setText(
                                        getResources().getString(R.string.labelNoRoutesInHistory));
                            } else if (activeSubView == SubView.BLOCKEDWAYS) {
                                textViewEmptyListView.setText(
                                        getResources().getString(R.string.labelNoBlockedWaysInHistory));
                            } else if (activeSubView == SubView.FAVORITEPOINTS) {
                                textViewEmptyListView.setText(
                                        getResources().getString(R.string.labelNoFavoritePointsInHistory));
                            } else if (activeSubView == SubView.ROUTEPOINTS) {
                                textViewEmptyListView.setText(
                                        getResources().getString(R.string.labelNoRoutePointsInHistory));
                            }
                            messageToast.setText(textViewEmptyListView.getText().toString());
                        } else {
                            textViewEmptyListView.setText("");
                            ListView listview = (ListView) mainLayout.findViewById(R.id.listHistory);
                            SimpleFilterableAdapter<Object> adapter =
                                (SimpleFilterableAdapter<Object>) listview.getAdapter();
                            messageToast.setText(adapter.getItem(0).toString());
                        }
                        if (! editSearch.getText().toString().equals(""))
                            messageToast.show();
                    }
                });
            }
            @Override public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
            @Override public void afterTextChanged(Editable arg0) {}
        });

        Button buttonDeleteSearch = (Button) mainLayout.findViewById(R.id.buttonDeleteSearch);
        buttonDeleteSearch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                EditText editSearch = (EditText) mainLayout.findViewById(R.id.editSearch);
                editSearch.setText("");
            }
        });
        buttonDeleteSearch.setOnLongClickListener(new OnLongClickListener() {
            @Override public boolean onLongClick(View v) {
                EditText editSearch = (EditText) mainLayout.findViewById(R.id.editSearch);
                editSearch.setText("");
                editSearch.postDelayed(new Runnable() {
                    public void run() {
                        EditText editSearch = (EditText) mainLayout.findViewById(R.id.editSearch);
                        editSearch.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN , 0, 0, 0));
                        editSearch.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP , 0, 0, 0));
                    }
                }, 200);
                return true;
            }
        });

        ListView listview = (ListView) mainLayout.findViewById(R.id.listHistory);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                Intent sender = getIntent();
                Route sourceRoute;
                if (activeSubView == SubView.ROUTES) {
                    sourceRoute = (Route) parent.getItemAtPosition(position);
                    if (sender.getExtras() != null && sender.getExtras().getInt("route", -1) > -1) {
                        settingsManager.setRouteRequest(sourceRoute);
                        finish();
                    } else {
                        showSourceRouteDetailsDialog(position);
                    }
                } else if (activeSubView == SubView.BLOCKEDWAYS) {
                    FootwaySegment object = (FootwaySegment) parent.getItemAtPosition(position);
                    Intent intent = new Intent(getApplicationContext(), RouteObjectDetailsActivity.class);
                    intent.putExtra("route_object", object.toJson().toString());
                    startActivity(intent);
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
                } else if (activeSubView == SubView.BLOCKEDWAYS) {
                    showBlockedWaysActionsMenuDialog(position);
                } else {
                    showRoutePointActionsMenuDialog(position);
                }
                return true;
            }
        });
        TextView textViewEmptyListView = (TextView) mainLayout.findViewById(R.id.labelEmptyList);
        listview.setEmptyView(textViewEmptyListView);

        updateUserInterface();
    }

    private synchronized void updateUserInterface() {
        TextView labelHeader = (TextView) mainLayout.findViewById(R.id.labelHeader);
        RadioGroup radioHistorySource = (RadioGroup) findViewById(R.id.radioHistorySource);
        if (showButtons) {
            labelHeader.setVisibility(View.GONE);
            radioHistorySource.setVisibility(View.VISIBLE);
        } else {
            if (activeSubView == SubView.ROUTES) {
                labelHeader.setText(getResources().getString(R.string.labelHistoryActivityRouteHeading));
            } else if (activeSubView == SubView.BLOCKEDWAYS) {
                labelHeader.setText(getResources().getString(R.string.labelHistoryActivityBlockedWaysHeading));
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
            SimpleFilterableAdapter<Route> routeAdapter = new SimpleFilterableAdapter<Route>(this,
                    android.R.layout.simple_list_item_1, settingsManager.loadSourceRoutesFromHistory() );
            listview.setAdapter(routeAdapter);
        } else if (activeSubView == SubView.BLOCKEDWAYS) {
            SimpleFilterableAdapter<FootwaySegment> blockedAdapter = new SimpleFilterableAdapter<FootwaySegment>(this,
                    android.R.layout.simple_list_item_1, settingsManager.loadBlockedWays() );
            listview.setAdapter(blockedAdapter);
        } else {
            ArrayList<Point> pointList;
            if (activeSubView == SubView.FAVORITEPOINTS) {
                pointList = settingsManager.loadPointsFromFavorites();
            } else {
                pointList = settingsManager.loadPointsFromHistory();
            }
            for (Point point : pointList) {
                if (currentLocation != null && point != null)
                    point.addDistance(currentLocation.distanceTo(point));
            }
            Collections.sort(pointList);
            SimpleFilterableAdapter<Point> pointAdapter = new SimpleFilterableAdapter<Point>(this,
                    android.R.layout.simple_list_item_1, pointList);
            listview.setAdapter(pointAdapter);
        }

        EditText editSearch = (EditText) mainLayout.findViewById(R.id.editSearch);
        editSearch.setText(editSearch.getText().toString());
    }

    private void showSourceRouteDetailsDialog(int objectIndex) {
        ListView listView = (ListView) mainLayout.findViewById(R.id.listHistory);
        Route sourceRoute = (Route) listView.getItemAtPosition(objectIndex);
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
                Route sourceRoute = (Route) listView.getItemAtPosition(view.getId());
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
        Route sourceRoute = (Route) listView.getItemAtPosition(objectIndex);
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
                Route sourceRoute = (Route) listView.getItemAtPosition(view.getId());
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
                Route sourceRoute = (Route) listView.getItemAtPosition(view.getId());
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

    private void showBlockedWaysActionsMenuDialog(int objectIndex) {
        ListView listView = (ListView) mainLayout.findViewById(R.id.listHistory);
        FootwaySegment footway = (FootwaySegment) listView.getItemAtPosition(objectIndex);
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_scrollable_empty);
        dialog.setCanceledOnTouchOutside(false);
        LinearLayout dialogLayout = (LinearLayout) dialog.findViewById(R.id.linearLayoutMain);
        TextView label;
        LayoutParams lp = new LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT );
        LayoutParams lpMarginTop = new LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT );
        // heading
        label = new TextView(this);
        label.setLayoutParams(lp);
        label.setText( String.format(
                    getResources().getString(R.string.labelActionsRoutePointDescription),
                    footway.getName() ));
        dialogLayout.addView(label);

        Button buttonObjectDetails = new Button(this);
        buttonObjectDetails.setLayoutParams(lp);
        buttonObjectDetails.setId(objectIndex);
        buttonObjectDetails.setText(getResources().getString(R.string.buttonRouteObjectDetails));
        buttonObjectDetails.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                ListView listView = (ListView) mainLayout.findViewById(R.id.listHistory);
                FootwaySegment footway = (FootwaySegment) listView.getItemAtPosition(view.getId());
                Intent intent = new Intent(getApplicationContext(), RouteObjectDetailsActivity.class);
                intent.putExtra("route_object", footway.toJson().toString());
                startActivity(intent);
                dialog.dismiss();
            }
        });
        dialogLayout.addView(buttonObjectDetails);

        Button buttonBlockFootwaySegment = new Button(this);
        buttonBlockFootwaySegment.setLayoutParams(lp);
        buttonBlockFootwaySegment.setId(objectIndex);
        if (settingsManager.footwaySegmentBlocked(footway))
            buttonBlockFootwaySegment.setText(getResources().getString(R.string.buttonUnblockFootwaySegment));
        else
            buttonBlockFootwaySegment.setText(getResources().getString(R.string.buttonBlockFootwaySegment));
        buttonBlockFootwaySegment.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                ListView listView = (ListView) mainLayout.findViewById(R.id.listHistory);
                FootwaySegment footway = (FootwaySegment) listView.getItemAtPosition(view.getId());
                if (settingsManager.footwaySegmentBlocked(footway)) {
                    settingsManager.unblockFootwaySegment(footway);
                } else {
                    settingsManager.blockFootwaySegment(footway);
                }
                updateUserInterface();
                dialog.dismiss();
            }
        });
        dialogLayout.addView(buttonBlockFootwaySegment);

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
                updateUserInterface();
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
                updateUserInterface();
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
        Route sourceRoute = settingsManager.getRouteRequest();
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
                    Route sourceRoute = settingsManager.getRouteRequest();
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
        } else if (activeSubView == SubView.BLOCKEDWAYS) {
            alertDialogBuilder.setMessage(getResources().getString(R.string.cleanBlockedWaysMessage));
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
                } else if (activeSubView == SubView.BLOCKEDWAYS) {
                    settingsManager.clearBlockedWays();
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

    @Override public void onResume() {
        super.onResume();
        settingsManager = globalData.getSettingsManagerInstance();
        positionManager.setPositionListener(new MyPositionListener());
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
            if (currentLocation == null
                    || currentLocation.getLocationObject().distanceTo(location) > 50.0) {
                currentLocation = new Point("", location);
                updateUserInterface();
            }
        }
    }

    private class SimpleFilterableAdapter<ObjectType> extends FilterableAdapter<ObjectType, String> {
        public SimpleFilterableAdapter(Context context, int resourceId, List<ObjectType> objects) {
            super(context, resourceId, objects);
        }
        @Override protected String prepareFilter(CharSequence seq) {
            return seq.toString().toLowerCase();
        }
        @Override protected boolean passesFilter(ObjectType object, String constraint) {
            String repr = object.toString().toLowerCase();
            if (repr.contains(constraint))
                return true;
            return false;
        }
    }
}
