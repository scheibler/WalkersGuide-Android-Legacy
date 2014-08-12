package org.walkersguide.userinterface;

import java.util.ArrayList;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.R;
import org.walkersguide.exceptions.RouteParsingException;
import org.walkersguide.routeobjects.RouteObjectWrapper;
import org.walkersguide.utils.DataDownloader;
import org.walkersguide.utils.Globals;
import org.walkersguide.utils.ObjectParser;
import org.walkersguide.utils.Route;
import org.walkersguide.utils.SettingsManager;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class TransportRouteChooserActivity extends Activity {

    private Globals globalData;
    private SettingsManager settingsManager;
    private Vibrator vibrator;
    private LinearLayout mainLayout;
    private Dialog dialog;
    private Handler progressHandler;
    private ProgressUpdater progressUpdater;
    private DataDownloader routeDownloader;

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

        // parse list of transport routes
        ArrayList<Route> transportRoutes = new ArrayList<Route>();
        CharSequence error = "";
        Intent sender=getIntent();
        try {
            JSONObject jsonTransportRoutes = new JSONObject(
                    sender.getExtras().getString("transport_routes") );
            transportRoutes = ObjectParser.parseMultipleRoutes(jsonTransportRoutes);
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

        TextView labelRouteDescription = (TextView) mainLayout.findViewById(R.id.labelRouteDescription);
        labelRouteDescription.setText(sender.getExtras().getString("description"));

        ListView listviewTransportRoutes = (ListView) mainLayout.findViewById(R.id.listTransportRoutes);
        RouteListAdapter adapterList = new RouteListAdapter(this,
                android.R.layout.simple_list_item_1, transportRoutes);
        listviewTransportRoutes.setAdapter(adapterList);
        listviewTransportRoutes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, final View view,
                    int position, long id) {
                showTransportRouteDetailsDialog(position);
            }
        });
        // if there are no list entrys
        TextView textViewEmptyListView = (TextView) mainLayout.findViewById(R.id.labelEmptyTransportRouteList);
        listviewTransportRoutes.setEmptyView(textViewEmptyListView);
    }

    private void showTransportRouteDetailsDialog(int objectIndex) {
        ListView listviewTransportRoutes = (ListView) mainLayout.findViewById(R.id.listTransportRoutes);
        Route route = (Route) listviewTransportRoutes.getItemAtPosition(objectIndex);
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_route_details);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnKeyListener(new OnKeyListener() {
            @Override public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    if (routeDownloader != null) {
                        routeDownloader.cancelDownloadProcess();
                    }
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
        buttonStartRouting.setTag(objectIndex + 1);
        buttonStartRouting.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Button buttonStartRouting = (Button) dialog.findViewById(R.id.buttonOK);
                if ((Integer) buttonStartRouting.getTag() > 0) {
                    queryFootwayRoute();
                } else {
                    routeDownloader.cancelDownloadProcess();
                }
            }
        });

        Button buttonCancel = (Button) dialog.findViewById(R.id.buttonCancel);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (routeDownloader != null) {
                    routeDownloader.cancelDownloadProcess();
                }
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

    public void queryFootwayRoute() {
        ListView listviewTransportRoutes = (ListView) mainLayout.findViewById(R.id.listTransportRoutes);
        Button buttonStartRouting = (Button) dialog.findViewById(R.id.buttonOK);
        Route route = (Route) listviewTransportRoutes.getItemAtPosition((Integer) buttonStartRouting.getTag() - 1);
        JSONArray routeJson = route.toJson();
        if (routeJson == null) {
            Intent intent = new Intent(getApplicationContext(), DialogActivity.class);
            intent.putExtra("message",
                    getResources().getString(R.string.messageEmptyJSONObject));
            startActivity(intent);
            return;
        }
        // options
        JSONObject optionsJson = new JSONObject();
        try {
            optionsJson.put("route_factor", settingsManager.getRouteFactor());
            optionsJson.put("language", Locale.getDefault().getLanguage());
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
        buttonStartRouting.setText(getResources().getString(R.string.buttonStartRoutingClicked));
        buttonStartRouting.setTag((Integer) buttonStartRouting.getTag() * -1);
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

    private class ProgressUpdater implements Runnable {
        public void run() {
            vibrator.vibrate(50);
            progressHandler.postDelayed(this, 2000);
        }
    }

    public class RouteListAdapter extends ArrayAdapter<Route> {
        private Context ctx;
        private LayoutInflater m_inflater = null;
        public ArrayList<Route> wrapperArray;

        public RouteListAdapter(Context context, int textViewResourceId, ArrayList<Route> array) {
            super(context, textViewResourceId);
            this.wrapperArray = array;
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
            Route wrapperObject = getItem(position);
            holder.contents.setText( (position+1) + ". " + wrapperObject.toString());
            return convertView;
        }

        @Override public int getCount() {
            if (wrapperArray != null)
                return wrapperArray.size();
            return 0;
        }

        @Override public Route getItem(int position) {
            return wrapperArray.get(position);
        }

        public void setArrayList(ArrayList<Route> array) {
            this.wrapperArray= array;
            notifyDataSetChanged();
        }

        private class EntryHolder {
            public TextView contents;
        }
    }

}
