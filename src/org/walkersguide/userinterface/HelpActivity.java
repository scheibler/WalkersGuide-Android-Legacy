package org.walkersguide.userinterface;

import org.walkersguide.R;
import org.walkersguide.utils.Globals;
import org.walkersguide.utils.SettingsManager;

import android.app.Activity;
import android.os.Bundle;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class HelpActivity extends Activity {

    private Globals globalData;
    private SettingsManager settingsManager;
    private RelativeLayout mainLayout;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (globalData == null) {
            globalData = ((Globals) getApplicationContext());
        }
        settingsManager = globalData.getSettingsManagerInstance();
        setContentView(R.layout.activity_help);
        mainLayout = (RelativeLayout) findViewById(R.id.linearLayoutMain);

        TextView labelClientVersion = (TextView) mainLayout.findViewById(R.id.labelClientVersion);
        labelClientVersion.setText( String.format(
                    getResources().getString(R.string.labelClientVersion),
                    settingsManager.getClientVersion()) );
        TextView labelInterfaceVersion = (TextView) mainLayout.findViewById(R.id.labelInterfaceVersion);
        labelInterfaceVersion.setText( String.format(
                    getResources().getString(R.string.labelInterfaceVersion),
                    String.valueOf(settingsManager.getInterfaceVersion())) );
        TextView labelMapVersion = (TextView) mainLayout.findViewById(R.id.labelMapVersion);
        if (settingsManager.getMapVersion().equals("")) {
            labelMapVersion.setText(getResources().getString(R.string.labelMapVersionUnknown));
        } else {
            labelMapVersion.setText( String.format(
                        getResources().getString(R.string.labelMapVersionKnown),
                        settingsManager.getMapVersion()) );
        }
    }
}
