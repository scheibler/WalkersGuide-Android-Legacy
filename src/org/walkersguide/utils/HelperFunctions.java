package org.walkersguide.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.R;


public class HelperFunctions {

    public static String getFormatedDirection(int direction) {
        if (direction < 0)
            direction += 360;
        if ((direction >= 0) && (direction < 23)) {
            return Globals.getContext().getResources().getString(R.string.directionStraightforward);
        } else if ((direction >= 23) && (direction < 68)) {
            return Globals.getContext().getResources().getString(R.string.directionTurnRightSlightly);
        } else if ((direction >= 68) && (direction < 113)) {
            return Globals.getContext().getResources().getString(R.string.directionTurnRight);
        } else if ((direction >= 113) && (direction < 158)) {
            return Globals.getContext().getResources().getString(R.string.directionTurnRightStrongly);
        } else if ((direction >= 158) && (direction < 203)) {
            return Globals.getContext().getResources().getString(R.string.directionBehindYou);
        } else if ((direction >= 203) && (direction < 248)) {
            return Globals.getContext().getResources().getString(R.string.directionTurnLeftStrongly);
        } else if ((direction >= 248) && (direction < 293)) {
            return Globals.getContext().getResources().getString(R.string.directionTurnLeft);
        } else if ((direction >= 293) && (direction < 338)) {
            return Globals.getContext().getResources().getString(R.string.directionTurnLeftSlightly);
        } else if ((direction >= 338) && (direction < 360)) {
            return Globals.getContext().getResources().getString(R.string.directionStraightforward);
        } else {
            return "";
        }
    }

    public static String getCompassDirection(int direction) {
        if (direction < 0)
            direction += 360;
        if ((direction >= 0) && (direction < 23)) {
            return Globals.getContext().getResources().getString(R.string.directionNorth);
        } else if ((direction >= 23) && (direction < 68)) {
            return Globals.getContext().getResources().getString(R.string.directionNorthEast);
        } else if ((direction >= 68) && (direction < 113)) {
            return Globals.getContext().getResources().getString(R.string.directionEast);
        } else if ((direction >= 113) && (direction < 158)) {
            return Globals.getContext().getResources().getString(R.string.directionSouthEast);
        } else if ((direction >= 158) && (direction < 203)) {
            return Globals.getContext().getResources().getString(R.string.directionSouth);
        } else if ((direction >= 203) && (direction < 248)) {
            return Globals.getContext().getResources().getString(R.string.directionSouthWest);
        } else if ((direction >= 248) && (direction < 293)) {
            return Globals.getContext().getResources().getString(R.string.directionWest);
        } else if ((direction >= 293) && (direction < 338)) {
            return Globals.getContext().getResources().getString(R.string.directionNorthWest);
        } else if ((direction >= 338) && (direction < 360)) {
            return Globals.getContext().getResources().getString(R.string.directionNorth);
        } else {
            return "";
        }
    }

    public static boolean isJSONValid(String s) {
        try {
            new JSONObject(s);
        } catch (JSONException e1) {
            try {
                new JSONArray(s);
            } catch (JSONException e2) {
                return false;
            }
        }
        return true;
    }

}
