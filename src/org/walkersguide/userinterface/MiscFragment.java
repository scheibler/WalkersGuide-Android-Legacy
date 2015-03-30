package org.walkersguide.userinterface;

import org.walkersguide.R;
import org.walkersguide.sensors.PositionManager;
import org.walkersguide.utils.Globals;
import org.walkersguide.utils.KeyboardManager;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MiscFragment extends Fragment {

    private Globals globalData;
    private PositionManager positionManager;
    private KeyboardManager keyboardManager;
    private RelativeLayout mainLayout;
    private Toast messageToast;

    @Override public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (globalData == null) {
            globalData = ((Globals) getActivity().getApplicationContext());
        }
        positionManager = globalData.getPositionManagerInstance();
        keyboardManager = globalData.getKeyboardManagerInstance();
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_misc, container, false);
        mainLayout = (RelativeLayout) view.findViewById(R.id.linearLayoutMain);
        messageToast = Toast.makeText(getActivity(), "", Toast.LENGTH_SHORT);

        Button buttonDisableSimulation = (Button) mainLayout.findViewById(R.id.buttonDisableSimulation);
        buttonDisableSimulation.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                positionManager.changeStatus(PositionManager.Status.GPS, null);
                updateUserInterface();
            }
        });

        Button buttonSimulateAddress = (Button) mainLayout.findViewById(R.id.buttonSimulateAddress);
        buttonSimulateAddress.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), EnterAddressActivity.class);
                intent.putExtra("objectIndex", -1);
                startActivity(intent);
            }
        });

        Button buttonGPSStatus = (Button) mainLayout.findViewById(R.id.buttonGPSStatus);
        buttonGPSStatus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent activityGPSStatus = new Intent(getActivity(), GPSStatusActivity.class);
                startActivity(activityGPSStatus);
            }
        });

        Button buttonOpenHistory = (Button) mainLayout.findViewById(R.id.buttonOpenHistory);
        buttonOpenHistory.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent activityEnterHistory = new Intent(getActivity(), HistoryActivity.class);
                activityEnterHistory.putExtra("subView", "ROUTEPOINTS");
                activityEnterHistory.putExtra("showButtons", 1);
                startActivity(activityEnterHistory);
            }
        });

        Button buttonOpenBlockedWays = (Button) mainLayout.findViewById(R.id.buttonOpenBlockedWays);
        buttonOpenBlockedWays.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent activityEnterHistory = new Intent(getActivity(), HistoryActivity.class);
                activityEnterHistory.putExtra("subView", "BLOCKEDWAYS");
                activityEnterHistory.putExtra("showButtons", 0);
                startActivity(activityEnterHistory);
            }
        });

        Button buttonAddFavorite = (Button) mainLayout.findViewById(R.id.buttonAddFavorite);
        buttonAddFavorite.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent activityAddMarker = new Intent(getActivity(), AddFavoriteActivity.class);
                startActivity(activityAddMarker);
            }
        });

        Button buttonOpenSettings = (Button) mainLayout.findViewById(R.id.buttonOpenSettings);
        buttonOpenSettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent activitySettings = new Intent(getActivity(), SettingsActivity.class);
                startActivity(activitySettings);
            }
        });

        Button buttonOpenHelp = (Button) mainLayout.findViewById(R.id.buttonOpenHelp);
        buttonOpenHelp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent activityHelp = new Intent(getActivity(), HelpActivity.class);
                startActivity(activityHelp);
            }
        });

        updateUserInterface();
        return view;
    }

    private void updateUserInterface() {
        Button buttonDisableSimulation = (Button) mainLayout.findViewById(R.id.buttonDisableSimulation);
        TextView labelCurrentSimulation = (TextView) mainLayout.findViewById(R.id.labelCurrentSimulation);
        if (positionManager.getStatus() == PositionManager.Status.SIMULATION) {
            labelCurrentSimulation.setText( String.format(
                        getResources().getString(R.string.labelCurrentSimulation),
                        positionManager.getSimulationObjectName()) );
            buttonDisableSimulation.setVisibility( View.VISIBLE );
        } else {
            labelCurrentSimulation.setText(getResources().getString(R.string.labelCurrentSimulationDisabled));
            buttonDisableSimulation.setVisibility( View.GONE );
        }
    }

    @Override public void onPause() {
        super.onPause();
    }

    @Override public void onResume() {
        super.onResume();	
        keyboardManager.setKeyboardListener(null);
        updateUserInterface();
    }
}
