package org.walkersguide.utils;

import org.walkersguide.R;
import org.walkersguide.userinterface.DialogActivity;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.widget.Toast;

public class CompassCalibrationValidator {

    private static final int thresholdValue = 30;
    private static final int maxNumberOfMatches = 5;
    private static final int secondsBetweenMatches = 15;
    private static final int minutesBetweenMessages = 5;
    private int matchCounter;
    private long lastMatchTime, lastMessageTime;
    private Context mContext;

    public CompassCalibrationValidator(Context context) {
        this.matchCounter = 0;
        this.lastMessageTime = 0;
        this.lastMatchTime = 0;
        this.mContext = context;
    }

    public boolean validate(Location location, int currentCompassValue) {
        long currentTime = System.currentTimeMillis();
        if (location != null
                && location.hasAccuracy()
                && location.hasBearing()
                && location.hasSpeed()
                && location.getProvider().equals("gps")
                && location.getAccuracy() < 30.0
                && location.getSpeed() > 1.0
                && currentTime - location.getTime() < 1000
                && currentTime - lastMessageTime > minutesBetweenMessages*60*1000) {
            // check, if the diff between the location bearing value and the current compass is
            // smaller than the defined threshold
            int diff = Math.abs( ((int) location.getBearing()) - currentCompassValue );
            if (diff > thresholdValue
                    && diff < 360-thresholdValue
                    && location.getSpeed() < 5.0) {
                if (currentTime - lastMatchTime > secondsBetweenMatches*1000) {
                    matchCounter += 1;
                    lastMatchTime = currentTime;
                    if (matchCounter >= 3) {
                        Toast.makeText(mContext, "compass: diff = " + diff + "; count = " + matchCounter,
                                Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                matchCounter = 0;
            }
            if (matchCounter >= maxNumberOfMatches) {
                lastMessageTime = currentTime;
                matchCounter = 0;
                Intent intent = new Intent(mContext.getApplicationContext(), DialogActivity.class);
                intent.putExtra("message", mContext.getResources().getString(R.string.messageCalibrateCompass));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
                return true;
            }
        }
        return false;
    }
}
