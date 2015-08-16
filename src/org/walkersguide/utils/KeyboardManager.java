package org.walkersguide.utils;

import android.content.Context;
import android.os.Handler;
import android.view.KeyEvent;

public class KeyboardManager implements Runnable {

    public interface KeyboardListener {
        public void longPressed(KeyEvent event);
        public void headsetButtonPressed(int numberOfClicks);
    }

    private Context mContext;
    private KeyboardListener keyboardListener;
    private int clickCounter;
    private long lastClicked;
    private Handler handler;
    private DataLogger dataLogger;

    public KeyboardManager(Context context) {
        this.mContext = context;
        this.clickCounter = 0;
        this.lastClicked = 0;
        handler = new Handler();
        handler.postDelayed(this, 100);
        dataLogger = new DataLogger(((Globals) mContext).getSettingsManagerInstance().getProgramLogFolder() + "/key_presses.txt", false);
    }

    public void setKeyboardListener(KeyboardListener keyboardListener) {
        this.keyboardListener = keyboardListener;
    }

    public void longPressed(KeyEvent event) {
        if (keyboardListener != null)
            keyboardListener.longPressed(event);
    }

    public void headsetButtonPressed() {
        this.clickCounter += 1;
        this.lastClicked = System.currentTimeMillis();
    }

    public void run() {
        if (System.currentTimeMillis() - this.lastClicked < 2000) {
            dataLogger.appendLog("c = " + this.clickCounter + "   delay = " + (System.currentTimeMillis() - this.lastClicked));
        }
        if (this.clickCounter > 0 && (System.currentTimeMillis() - this.lastClicked) > 1000) {
            if (keyboardListener != null)
                keyboardListener.headsetButtonPressed(this.clickCounter);
            this.clickCounter = 0;
        }
        handler.postDelayed(this, 100);
    }

}
