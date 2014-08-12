package org.walkersguide.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.R;


public class HelperFunctions {

    public static int getClockDirection(int clockDirection) {
        if (clockDirection < 0)
            clockDirection += 360;
        if ((clockDirection >= 0) && (clockDirection <= 15)) {
            clockDirection = 12;
        } else if ((clockDirection > 15) && (clockDirection <= 45)) {
            clockDirection = 1;
        } else if ((clockDirection > 45) && (clockDirection <= 75)) {
            clockDirection = 2;
        } else if ((clockDirection > 75) && (clockDirection <= 105)) {
            clockDirection = 3;
        } else if ((clockDirection > 105) && (clockDirection <= 135)) {
            clockDirection = 4;
        } else if ((clockDirection > 135) && (clockDirection <= 165)) {
            clockDirection = 5;
        } else if ((clockDirection > 165) && (clockDirection <= 195)) {
            clockDirection = 6;
        } else if ((clockDirection > 195) && (clockDirection <= 225)) {
            clockDirection = 7;
        } else if ((clockDirection > 225) && (clockDirection <= 255)) {
            clockDirection = 8;
        } else if ((clockDirection > 255) && (clockDirection <= 285)) {
            clockDirection = 9;
        } else if ((clockDirection > 285) && (clockDirection <= 315)) {
            clockDirection = 10;
        } else if ((clockDirection > 315) && (clockDirection <= 345)) {
            clockDirection = 11;
        } else if ((clockDirection > 345) && (clockDirection <= 360)) {
            clockDirection = 12;
        } else {
            clockDirection = -1;
        }
        return clockDirection;
    }

    public static String getFormatedDirection(int direction) {
        if (direction < 0)
            direction += 360;
        if ((direction >= 0) && (direction <= 23)) {
            return Globals.getContext().getResources().getString(R.string.directionStraightforward);
        } else if ((direction > 23) && (direction <= 68)) {
            return Globals.getContext().getResources().getString(R.string.directionTurnRightSlightly);
        } else if ((direction > 68) && (direction <= 113)) {
            return Globals.getContext().getResources().getString(R.string.directionTurnRight);
        } else if ((direction > 113) && (direction <= 158)) {
            return Globals.getContext().getResources().getString(R.string.directionTurnRightStrongly);
        } else if ((direction > 158) && (direction <= 203)) {
            return Globals.getContext().getResources().getString(R.string.directionBehindYou);
        } else if ((direction > 203) && (direction <= 248)) {
            return Globals.getContext().getResources().getString(R.string.directionTurnLeftStrongly);
        } else if ((direction > 248) && (direction <= 293)) {
            return Globals.getContext().getResources().getString(R.string.directionTurnLeft);
        } else if ((direction > 293) && (direction <= 338)) {
            return Globals.getContext().getResources().getString(R.string.directionTurnLeftSlightly);
        } else if ((direction > 338) && (direction <= 359)) {
            return Globals.getContext().getResources().getString(R.string.directionStraightforward);
        } else {
            return "";
        }
    }

    public static String getCompassDirection(int direction) {
        if (direction < 0)
            direction += 360;
        if ((direction >= 0) && (direction <= 23)) {
            return Globals.getContext().getResources().getString(R.string.directionNorth);
        } else if ((direction > 23) && (direction <= 68)) {
            return Globals.getContext().getResources().getString(R.string.directionNorthEast);
        } else if ((direction > 68) && (direction <= 113)) {
            return Globals.getContext().getResources().getString(R.string.directionEast);
        } else if ((direction > 113) && (direction <= 158)) {
            return Globals.getContext().getResources().getString(R.string.directionSouthEast);
        } else if ((direction > 158) && (direction <= 203)) {
            return Globals.getContext().getResources().getString(R.string.directionSouth);
        } else if ((direction > 203) && (direction <= 248)) {
            return Globals.getContext().getResources().getString(R.string.directionSouthWest);
        } else if ((direction > 248) && (direction <= 293)) {
            return Globals.getContext().getResources().getString(R.string.directionWest);
        } else if ((direction > 293) && (direction <= 338)) {
            return Globals.getContext().getResources().getString(R.string.directionNorthWest);
        } else if ((direction > 338) && (direction <= 359)) {
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
