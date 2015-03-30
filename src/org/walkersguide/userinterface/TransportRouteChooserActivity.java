package org.walkersguide.userinterface;

import java.util.ArrayList;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.R;
import org.walkersguide.exceptions.RouteParsingException;
import org.walkersguide.routeobjects.FootwaySegment;
import org.walkersguide.routeobjects.RouteObjectWrapper;
import org.walkersguide.utils.DataDownloader;
import org.walkersguide.utils.Globals;
import org.walkersguide.utils.ObjectParser;
import org.walkersguide.utils.Route;
import org.walkersguide.utils.SettingsManager;
import org.walkersguide.utils.TransportConnection;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class TransportRouteChooserActivity extends AbstractActivity {

    private Globals globalData;
    private SettingsManager settingsManager;
    private Vibrator vibrator;
    private LinearLayout mainLayout;
    private Dialog dialog;
    private Handler progressHandler;
    private ProgressUpdater progressUpdater;
    private DataDownloader routeDownloader;
    private ArrayList<TransportConnection> transportConnectionList;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
        globalData = ((Globals) getApplicationContext());
        settingsManager = globalData.getSettingsManagerInstance();
        vibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        progressHandler = new Handler();
        progressUpdater = new ProgressUpdater();

        // layouts
        setContentView(R.layout.activity_transport_route_chooser);
        mainLayout = (LinearLayout) findViewById(R.id.linearLayoutMain);

        // parse list of transport connections
        transportConnectionList = new ArrayList<TransportConnection>();
        CharSequence error = "";
        Intent sender=getIntent();
        try {
            JSONObject jsonTransportRoutes = new JSONObject(
                    sender.getExtras().getString("transport_routes") );
            transportConnectionList = ObjectParser.parseMultipleRoutes(jsonTransportRoutes);
            if (transportConnectionList.size() == 0)
                error = getResources().getString(R.string.labelNoTransportRoutes);
        } catch (JSONException e) {
            error = e.getMessage();
        } catch (RouteParsingException e) {
            error = e.getMessage();
        }
        if (!error.equals("")) {
            Intent intent = new Intent(getApplicationContext(), DialogActivity.class);
            intent.putExtra("message", error);
            startActivity(intent);
            finish();
        }
        // sort second list by departure time
        // first comes sorted from the server by cost
        /*Collections.sort(transportRoutesByDepartureTime, new Comparator<Route>() {
            public int compare(Route r1, Route r2) {
                int r1MinutesTillDeparture = 0;
                int r1NumberOfTransportSegments = 0;
                for (RouteObjectWrapper object : r1.getRouteList()) {
                    TransportSegment transportSegment = object.getTransportSegment();
                    if (transportSegment != null) {
                        if (r1NumberOfTransportSegments == 0) {
                            r1MinutesTillDeparture = (int) (transportSegment.getDepartureTimeMillis() - System.currentTimeMillis()) / 60000;
                        }
                        r1NumberOfTransportSegments += 1;
                    }
                }
                int r2MinutesTillDeparture = 0;
                int r2NumberOfTransportSegments = 0;
                for (RouteObjectWrapper object : r2.getRouteList()) {
                    TransportSegment transportSegment = object.getTransportSegment();
                    if (transportSegment != null) {
                        if (r2NumberOfTransportSegments == 0) {
                            r2MinutesTillDeparture = (int) (transportSegment.getDepartureTimeMillis() - System.currentTimeMillis()) / 60000;
                        }
                        r2NumberOfTransportSegments += 1;
                    }
                }
                // first parameter: minutes till departure
                if (r1MinutesTillDeparture < r2MinutesTillDeparture) {
                    return -1;
                } else if (r1MinutesTillDeparture > r2MinutesTillDeparture) {
                    return 1;
                }
                // second parameter: number of transport segments
                if (r1NumberOfTransportSegments < r2NumberOfTransportSegments) {
                    return -1;
                } else if (r1NumberOfTransportSegments > r2NumberOfTransportSegments) {
                    return 1;
                }
                return 0;
            }
        });*/

        TextView labelRouteDescription = (TextView) mainLayout.findViewById(R.id.labelRouteDescription);
        labelRouteDescription.setText(sender.getExtras().getString("description"));

        // radio buttons
        RadioGroup radioRouteSortCriteria = (RadioGroup) findViewById(R.id.radioRouteSortCriteria);
        radioRouteSortCriteria.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                ExpandableListView listViewConnections = (ExpandableListView) mainLayout.findViewById(R.id.listTransportRoutes);
                RouteListAdapter rlAdapter = (RouteListAdapter) listViewConnections.getExpandableListAdapter();
                if (checkedId == R.id.radioButtonRecommendedConnections) {
                    ArrayList<TransportConnection> subList = new ArrayList<TransportConnection>();
                    int maxCost = (int) (1.33 * transportConnectionList.get(0).getMinimalCost());
                    for (TransportConnection connection : transportConnectionList) {
                        if (connection.getMinimalCost() < maxCost) {
                            subList.add(connection);
                        }
                    }
                    rlAdapter.setArrayList(subList);
                } else {
                    rlAdapter.setArrayList(transportConnectionList);
                }
            }
        });

        ExpandableListView listViewConnections = (ExpandableListView) mainLayout.findViewById(R.id.listTransportRoutes);
        RouteListAdapter adapter = new RouteListAdapter(this, transportConnectionList);
        listViewConnections.setAdapter(adapter);
        listViewConnections.setOnChildClickListener(new OnChildClickListener() {
            @Override public boolean onChildClick(ExpandableListView parent, View v,
                    int groupPosition, int childPosition, long id) {
                showTransportRouteDetailsDialog(groupPosition, childPosition);
                return true;
            }
        });
        // if there are no list entrys
        TextView textViewEmptyListView = (TextView) mainLayout.findViewById(R.id.labelEmptyTransportRouteList);
        listViewConnections.setEmptyView(textViewEmptyListView);

        // cancel button
        Button buttonCancel = (Button) mainLayout.findViewById(R.id.buttonCancel);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                finish();
            }
        });

        // check sub view
        radioRouteSortCriteria.check(R.id.radioButtonRecommendedConnections);
    }

    private void showTransportRouteDetailsDialog(int groupPosition, int childPosition) {
        ExpandableListView listViewConnections = (ExpandableListView) mainLayout.findViewById(R.id.listTransportRoutes);
        RouteListAdapter rlAdapter = (RouteListAdapter) listViewConnections.getExpandableListAdapter();
        Route route = rlAdapter.getChild(groupPosition, childPosition);
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_route_details);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnKeyListener(new OnKeyListener() {
            @Override public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    cancelRouteDownloadProcess();
                    dialog.dismiss();
                }
                return true;
            }
        });
        // heading
        TextView label = (TextView) dialog.findViewById(R.id.labelRouteDescription);
        label.setText(route.toString());

        Button buttonStartRouting = (Button) dialog.findViewById(R.id.buttonOK);
        buttonStartRouting.setText(getResources().getString(R.string.buttonStartRouting));
        buttonStartRouting.setTag(groupPosition*1000000 + childPosition + 1);
        System.out.println("xx gid = " + groupPosition + " cid = " + childPosition);
        buttonStartRouting.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Button buttonStartRouting = (Button) dialog.findViewById(R.id.buttonOK);
                int tag = (Integer) buttonStartRouting.getTag();
                if (tag > 0) {
                    int groupPosition = Math.round(tag / 1000000);
                    int childPosition = tag - groupPosition*1000000 - 1;
                    System.out.println("xx- gid = " + groupPosition + " cid = " + childPosition);
                    queryFootwayRoute(groupPosition, childPosition);
                } else {
                    cancelRouteDownloadProcess();
                }
            }
        });

        Button buttonCancel = (Button) dialog.findViewById(R.id.buttonCancel);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                cancelRouteDownloadProcess();
                dialog.dismiss();
            }
        });

        ListView listRouteObjects = (ListView) dialog.findViewById(R.id.listRouteObjects);
        ArrayAdapter<RouteObjectWrapper> adapter = new ArrayAdapter<RouteObjectWrapper>(
                this, android.R.layout.simple_list_item_1, route.getRouteList());
        listRouteObjects.setAdapter(adapter);
        listRouteObjects.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, final View view,
                    int position, long id) {
                RouteObjectWrapper object = (RouteObjectWrapper) parent.getItemAtPosition(position);
                if (object.isEmpty()) {
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.messageEmptyRoutePoint),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                Intent intent = new Intent(getApplicationContext(), RouteObjectDetailsActivity.class);
                intent.putExtra("route_object", object.toJson().toString());
                startActivity(intent);
            }
        });
        dialog.show();
    }

    public void queryFootwayRoute(int groupPosition, int childPosition) {
        ExpandableListView listViewConnections = (ExpandableListView) mainLayout.findViewById(R.id.listTransportRoutes);
        RouteListAdapter rlAdapter = (RouteListAdapter) listViewConnections.getExpandableListAdapter();
        Route route = rlAdapter.getChild(groupPosition, childPosition);
        JSONArray routeJson = route.toJson();
        if (routeJson == null) {
            Intent intent = new Intent(getApplicationContext(), DialogActivity.class);
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
            optionsJson.put("route_factor", settingsManager.getRouteFactor());
            optionsJson.put("allowed_way_classes", new JSONArray(settingsManager.getRoutingWayClasses()));
            optionsJson.put("language", Locale.getDefault().getLanguage());
            optionsJson.put("session_id", globalData.getSessionId());
            optionsJson.put("blocked_ways", TextUtils.join(",", blockedWayIds));
        } catch (JSONException e) {
            Intent intent = new Intent(getApplicationContext(), DialogActivity.class);
            intent.putExtra("message",
                    getResources().getString(R.string.messageEmptyJSONObject));
            startActivity(intent);
            return;
        }
        // add all to request object
        JSONObject requestJson = new JSONObject();
        try {
            requestJson.put("source_route", routeJson);
            requestJson.put("options", optionsJson);
        } catch (JSONException e) {
            Intent intent = new Intent(getApplicationContext(), DialogActivity.class);
            intent.putExtra("message",
                    getResources().getString(R.string.messageEmptyJSONObject));
            startActivity(intent);
            return;
        }
        // send request
        routeDownloader = new DataDownloader(TransportRouteChooserActivity.this);
        routeDownloader.setDataDownloadListener(new SingleRouteDownloadListener() );
        routeDownloader.execute( "post",
                globalData.getSettingsManagerInstance().getServerPath() + "/get_route",
                requestJson.toString() );
        Toast.makeText(getApplicationContext(), getResources().getString(R.string.messageRouteComputationStarted),
                Toast.LENGTH_LONG).show();
        progressHandler.postDelayed(progressUpdater, 100);
        // button label and tag
        Button buttonStartRouting = (Button) dialog.findViewById(R.id.buttonOK);
        buttonStartRouting.setText(getResources().getString(R.string.buttonStartRoutingClicked));
        buttonStartRouting.setTag((Integer) buttonStartRouting.getTag() * -1);
    }

    private void cancelRouteDownloadProcess() {
        if (routeDownloader == null)
            return;
        routeDownloader.cancelDownloadProcess();
        JSONObject requestJson = new JSONObject();
        try {
            requestJson.put("language", Locale.getDefault().getLanguage());
            requestJson.put("session_id", globalData.getSessionId());
        } catch (JSONException e) {
            return;
        }
        routeDownloader = new DataDownloader(this);
        routeDownloader.setDataDownloadListener(new CanceledRequestDownloadListener() );
        routeDownloader.execute( "post",
                globalData.getSettingsManagerInstance().getServerPath() + "/cancel_request",
                requestJson.toString() );
    }

    @Override public void onPause() {
        super.onPause();
        cancelRouteDownloadProcess();
    }

    @Override public void onResume() {
        super.onResume();
    }

    private class SingleRouteDownloadListener implements DataDownloader.DataDownloadListener {
        @Override public void dataDownloadedSuccessfully(JSONObject jsonObject) {
            progressHandler.removeCallbacks(progressUpdater);
            routeDownloader = null;
            Button buttonStartRouting = (Button) dialog.findViewById(R.id.buttonOK);
            buttonStartRouting.setTag((Integer) buttonStartRouting.getTag() * -1);
            buttonStartRouting.setText(getResources().getString(R.string.buttonStartRouting));
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
                intent.putExtra("message", e.getMessage());
                startActivity(intent);
            }
        }

        @Override public void dataDownloadFailed(String error) {
            progressHandler.removeCallbacks(progressUpdater);
            routeDownloader = null;
            Button buttonStartRouting = (Button) dialog.findViewById(R.id.buttonOK);
            buttonStartRouting.setTag((Integer) buttonStartRouting.getTag() * -1);
            buttonStartRouting.setText(getResources().getString(R.string.buttonStartRouting));
            Intent intent = new Intent(getApplicationContext(), DialogActivity.class);
            intent.putExtra("message", String.format(
                        getResources().getString(R.string.messageNetworkError), error) );
            startActivity(intent);
        }

        @Override public void dataDownloadCanceled() {
            progressHandler.removeCallbacks(progressUpdater);
            routeDownloader = null;
            Button buttonStartRouting = (Button) dialog.findViewById(R.id.buttonOK);
            buttonStartRouting.setTag((Integer) buttonStartRouting.getTag() * -1);
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

    public class RouteListAdapter extends BaseExpandableListAdapter {
        private Context ctx;
        private LayoutInflater m_inflater = null;
        private ArrayList<TransportConnection> connections;

        public RouteListAdapter(Context context, ArrayList<TransportConnection> connections) {
            this.connections = connections;
            this.ctx = context;
            m_inflater = LayoutInflater.from(ctx);
        }

        @Override public View getGroupView(int groupPosition, boolean isExpanded,
                View convertView, ViewGroup parent) {
            EntryHolder holder;
            if (convertView == null) {
                holder = new EntryHolder();
                convertView = m_inflater.inflate(R.layout.table_row_one_column, parent, false);
                holder.contents= (TextView) convertView.findViewById(R.id.labelColumnOne);
                convertView.setTag(holder);
            } else {
                holder = (EntryHolder) convertView.getTag();
            }
            TransportConnection connection = getGroup(groupPosition);
            holder.contents.setText( (groupPosition+1) + ". " + connection.toString());
            return convertView;
        }

        @Override public TransportConnection getGroup(int groupPosition) {
            return connections.get(groupPosition);
        }

        @Override public int getGroupCount() {
            return connections.size();
        }

        @Override public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override public View getChildView(int groupPosition, int childPosition,
                boolean isLastChild, View convertView, ViewGroup parent) {
            EntryHolder holder;
            if (convertView == null) {
                holder = new EntryHolder();
                convertView = m_inflater.inflate(R.layout.table_row_one_column, parent, false);
                holder.contents= (TextView) convertView.findViewById(R.id.labelColumnOne);
                convertView.setTag(holder);
            } else {
                holder = (EntryHolder) convertView.getTag();
            }
            Route route = getChild(groupPosition, childPosition);
            holder.contents.setText((groupPosition+1) + "." + (childPosition+1) + ". " + route.toString());
            return convertView;
        }

        @Override public Route getChild(int groupPosition, int childPosition) {
            return connections.get(groupPosition).getRouteAtIndex(childPosition);
        }

        @Override public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override public int getChildrenCount(int groupPosition) {
            return connections.get(groupPosition).getNumberOfRoutes();
        }

        @Override public boolean hasStableIds() {
            return false;
        }

        @Override public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        public void setArrayList(ArrayList<TransportConnection> connections) {
            this.connections = connections;
            notifyDataSetChanged();
        }

        private class EntryHolder {
            public TextView contents;
        }
    }

}
