package org.walkersguide;

import org.walkersguide.exceptions.CustomExceptionHandler;
import org.walkersguide.sensors.PositionManager;
import org.walkersguide.sensors.SensorsManager;
import org.walkersguide.userinterface.AbstractActivity;
import org.walkersguide.userinterface.MiscFragment;
import org.walkersguide.userinterface.POIFragment;
import org.walkersguide.userinterface.RouterFragment;
import org.walkersguide.userinterface.StartFragment;
import org.walkersguide.utils.Globals;
import org.walkersguide.utils.KeyboardManager;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.Toast;




public class MainActivity extends AbstractActivity {

    private MyMessageFromStartFragmentListener mSFListener;
    private MyMessageFromRouterFragmentListener mRouterFListener;
    private MyMessageFromPOIFragmentListener mPOIFListener;
    private ActionBar.Tab startTab;
    private ActionBar.Tab routerTab;
    private ActionBar.Tab poisTab;
    private ActionBar.Tab miscTab;
    private MyTabListener<RouterFragment> routerListener;
    private Toast messageToast;
    private Dialog dialog;
    private boolean backButtonClicked, wasLongPressed;
    private Globals globalData;
    private AudioManager audioManager;
    private KeyboardManager keyboardManager;
    private PositionManager positionManager;
    private SensorsManager sensorsManager;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        messageToast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);
        backButtonClicked = false;
        wasLongPressed = false;
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (globalData == null) {
            globalData = ((Globals) getApplicationContext());
        }
        keyboardManager = globalData.getKeyboardManagerInstance();
        positionManager = globalData.getPositionManagerInstance();
        positionManager.resumeGPS();
        sensorsManager = globalData.getSensorsManagerInstance();
        sensorsManager.resumeSensors();

        // log to sd card
        // Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(
        //         "/sdcard/<desired_local_path>", "http://<desired_url>/upload.php"));
        Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler( getApplicationContext() ));

        //ActionBar
        ActionBar actionbar = getActionBar();
        actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionbar.setDisplayShowTitleEnabled(false);
        actionbar.setDisplayShowHomeEnabled(false);

        String tabName = getResources().getString(R.string.startFragmentName);
        startTab = actionbar.newTab()
            .setText(tabName)
            .setTabListener(new MyTabListener<StartFragment>(
                this, tabName, StartFragment.class));

        tabName = getResources().getString(R.string.routerFragmentName);
        routerTab = actionbar.newTab()
            .setText(tabName)
            .setTabListener(new MyTabListener<RouterFragment>(
                this, tabName, RouterFragment.class));

        tabName = getResources().getString(R.string.poiFragmentName);
        poisTab = actionbar.newTab()
            .setText(tabName)
            .setTabListener(new MyTabListener<POIFragment>(
                this, tabName, POIFragment.class));

        tabName = getResources().getString(R.string.miscFragmentName);
        miscTab = actionbar.newTab()
            .setText(tabName)
            .setTabListener(new MyTabListener<MiscFragment>(
                this, tabName, MiscFragment.class));

        actionbar.addTab(startTab);
        actionbar.addTab(routerTab);
        actionbar.addTab(poisTab);
        actionbar.addTab(miscTab);

        if (savedInstanceState != null) {
            actionbar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
        }
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        // other possible key codes: KEYCODE_MEDIA_PLAY, KEYCODE_MEDIA_PAUSE, KEYCODE_HEADSETHOOK
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {
                    if (event.isLongPress()) {
                        wasLongPressed = true;
                        keyboardManager.sendKey(event);
                    }
                }
                if (action == KeyEvent.ACTION_UP) {
                    if (wasLongPressed) {
                        wasLongPressed = false;
                    } else {
                        audioManager.adjustVolume(
                                AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                    }
                }
                return true;

            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_DOWN) {
                    if (event.isLongPress()) {
                        wasLongPressed = true;
                        keyboardManager.sendKey(event);
                    }
                }
                if (action == KeyEvent.ACTION_UP) {
                    if (wasLongPressed) {
                        wasLongPressed = false;
                    } else {
                        audioManager.adjustVolume(
                                AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                    }
                }
                return true;

            case KeyEvent.KEYCODE_FOCUS:
                return true;

            case KeyEvent.KEYCODE_CAMERA:
                if (action == KeyEvent.ACTION_DOWN) {
                    if (event.isLongPress()) {
                        wasLongPressed = true;
                        // select previous tab
                        int newTabIndex = getActionBar().getSelectedNavigationIndex() - 1;
                        if (newTabIndex < 0)
                            newTabIndex = getActionBar().getNavigationItemCount() - 1;
                        messageToast.setText( String.format(
                                getResources().getString(R.string.previousTabMessage),
                                getActionBar().getTabAt(newTabIndex).getText() ));
                        messageToast.show();
                        getActionBar().setSelectedNavigationItem(newTabIndex);
                    }
                }
                if (action == KeyEvent.ACTION_UP) {
                    if (wasLongPressed) {
                        wasLongPressed = false;
                    } else {
                        // select next tab
                        int newTabIndex = getActionBar().getSelectedNavigationIndex() + 1;
                        if (newTabIndex >= getActionBar().getNavigationItemCount())
                            newTabIndex = 0;
                        messageToast.setText( String.format(
                                getResources().getString(R.string.nextTabMessage),
                                getActionBar().getTabAt(newTabIndex).getText() ));
                        messageToast.show();
                        getActionBar().setSelectedNavigationItem(newTabIndex);
                    }
                }
                return true;

            case KeyEvent.KEYCODE_BACK:
                if (action == KeyEvent.ACTION_UP && !backButtonClicked) {
                    backButtonClicked = true;
                    showExitDialog();
                    return true;
                }
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    private void showExitDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(
                getResources().getString(R.string.exitDialogTitle));
        alertDialogBuilder.setMessage(
                getResources().getString(R.string.exitDialogMessage));
        alertDialogBuilder.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0 && backButtonClicked == true) {
                    backButtonClicked = false;
                    finish();
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_MENU && event.getRepeatCount() == 0 && backButtonClicked == true) {
                    backButtonClicked = false;
                    dialog.cancel();
                    return true;
                }
                return false;
            }
        });
        alertDialogBuilder.setPositiveButton(
                getResources().getString(R.string.dialogYes),
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int id) {
                backButtonClicked = false;
                finish();
            }
        });
        alertDialogBuilder.setNegativeButton(
                getResources().getString(R.string.dialogNo),
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int id) {
                backButtonClicked = false;
            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("tab", getActionBar().getSelectedNavigationIndex());
    }

    @Override public void onDestroy() {
        super.onDestroy();
        globalData.killSessionId();
        globalData.stopActivityTransitionTimer();
        positionManager.stopGPS();
        sensorsManager.stopSensors();
    }

    public MyMessageFromStartFragmentListener getMessageFromStartFragmentListener() {
        if (mSFListener == null)
            mSFListener = new MyMessageFromStartFragmentListener(); 
        return mSFListener;
    }

    public MyMessageFromRouterFragmentListener getMessageFromRouterFragmentListener() {
        if (mRouterFListener == null)
            mRouterFListener = new MyMessageFromRouterFragmentListener(); 
        return mRouterFListener;
    }

    public MyMessageFromPOIFragmentListener getMessageFromPOIFragmentListener() {
        if (mPOIFListener == null)
            mPOIFListener = new MyMessageFromPOIFragmentListener(); 
        return mPOIFListener;
    }


    public static class MyTabListener<T extends Fragment> implements ActionBar.TabListener {
        private Fragment mFragment;
        private final Activity mActivity;
        private final String mTag;
        private final Class<T> mClass;

        /** Constructor used each time a new tab is created.
         * @param activity  The host Activity, used to instantiate the fragment
         * @param tag  The identifier tag for the fragment
         * @param clz  The fragment's Class, used to instantiate the fragment
         */
        public MyTabListener(Activity activity, String tag, Class<T> clz) {
            mActivity = activity;
            mTag = tag;
            mClass = clz;
        }
        
        /* The following are each of the ActionBar.TabListener callbacks */
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            // Check if the fragment is already initialized
            mFragment = (Fragment) mActivity.getFragmentManager().findFragmentByTag(mTag);
            if (mFragment != null) {
                // If it exists, simply attach it in order to show it
                ft.attach(mFragment);
            } else {
                // If not, instantiate and add it to the activity
                mFragment = Fragment.instantiate(mActivity, mClass.getName());
                ft.add(android.R.id.content, mFragment, mTag);
            }
        }
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            if (mFragment != null) {
                // Detach the fragment, because another one is being attached
                ft.detach(mFragment);
            }
        }
        public void onTabReselected(Tab tab, FragmentTransaction ft) {
            // User selected the already selected tab. Usually do nothing.
        }
    }

    private class MyMessageFromStartFragmentListener implements StartFragment.MessageFromStartFragmentListener {
        public void switchToOtherFragment(String fragmentName) {
            if (fragmentName == "router")
                routerTab.select();
            else if (fragmentName == "poi")
                poisTab.select();
        }
    }

    private class MyMessageFromRouterFragmentListener implements RouterFragment.MessageFromRouterFragmentListener {
        public void switchToOtherFragment(String fragmentName) {
            if (fragmentName == "start")
                startTab.select();
        }
    }

    private class MyMessageFromPOIFragmentListener implements POIFragment.MessageFromPOIFragmentListener {
        public void switchToOtherFragment(String fragmentName) {
            if (fragmentName == "router")
                routerTab.select();
            else if (fragmentName == "start")
                startTab.select();
        }
    }

}
