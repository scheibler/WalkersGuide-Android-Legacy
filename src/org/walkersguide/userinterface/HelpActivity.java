package org.walkersguide.userinterface;

import org.walkersguide.R;
import org.walkersguide.utils.Globals;
import org.walkersguide.utils.SettingsManager;

import android.os.Bundle;
import android.text.util.Linkify;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class HelpActivity extends AbstractActivity {

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
        TextView labelEMailAddress = (TextView) mainLayout.findViewById(R.id.labelEMailAddress);
        labelEMailAddress.setAutoLinkMask(Linkify.EMAIL_ADDRESSES);
        labelEMailAddress.setText( String.format(
                    getResources().getString(R.string.labelSupportEMailAddress),
                    settingsManager.getEMailAddress() ));
        TextView labelWebsite = (TextView) mainLayout.findViewById(R.id.labelWebsite);
        labelWebsite.setAutoLinkMask(Linkify.WEB_URLS);
        labelWebsite.setText( String.format(
                    getResources().getString(R.string.labelProjectWebsite),
                    settingsManager.getHostURL() ));
    }
}
