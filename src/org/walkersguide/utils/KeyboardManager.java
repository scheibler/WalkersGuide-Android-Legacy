package org.walkersguide.utils;

import android.content.Context;
import android.view.KeyEvent;

public class KeyboardManager {

    public interface KeyboardListener {
        public void longPressed(KeyEvent event);
    }

    private Context mContext;
    private KeyboardListener keyboardListener;

    public KeyboardManager(Context context) {
        this.mContext = context;
    }

    public void setKeyboardListener(KeyboardListener keyboardListener) {
        this.keyboardListener = keyboardListener;
    }

    public void sendKey(KeyEvent event) {
        if (keyboardListener != null)
            keyboardListener.longPressed(event);
    }

}
