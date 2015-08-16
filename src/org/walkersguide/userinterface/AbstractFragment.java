package org.walkersguide.userinterface;

import org.walkersguide.utils.Globals;

import android.app.Fragment;

public abstract class AbstractFragment extends Fragment {
    @Override public void onResume() {
        super.onResume();	
        System.out.println("xxx abstract frag onResume");
        Globals globalData = ((Globals) Globals.getContext());
        globalData.getAddressManagerInstance().setAddressListener(null);
        globalData.getKeyboardManagerInstance().setKeyboardListener(null);
        globalData.getPOIManagerInstance().setPOIListener(null);
        globalData.getPositionManagerInstance().setPositionListener(null);
        globalData.getSensorsManagerInstance().setSensorsListener(null);
    }
}

