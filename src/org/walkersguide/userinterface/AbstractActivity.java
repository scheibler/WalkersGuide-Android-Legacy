package org.walkersguide.userinterface;

import org.walkersguide.R;
import org.walkersguide.utils.Globals;

import android.app.Activity;
import android.media.MediaPlayer;

public abstract class AbstractActivity extends Activity {

    @Override public void onPause() {
        super.onPause();
        ((Globals) getApplicationContext()).startActivityTransitionTimer();
    }

    @Override public void onResume() {
        super.onResume();
        Globals globalData = ((Globals) Globals.getContext());
        if (globalData.applicationInBackground()) {
            globalData.getPositionManagerInstance().resumeGPS();
            globalData.getSensorsManagerInstance().resumeSensors();
            //MediaPlayer mp = MediaPlayer.create(this, R.raw.restored);
            //mp.start();
            //System.out.println("xxx app resumed from background");
        }
        globalData.stopActivityTransitionTimer();
    }
}
