package org.walkersguide.routeobjects;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.R;
import org.walkersguide.utils.Globals;
import org.walkersguide.utils.HelperFunctions;

import android.location.Location;
import android.text.TextUtils;


public class POIPoint extends Point {

    private String address;
    private String phone;
    private String email;
    private String website;
    private String openingHours;
    private POIPoint outerBuilding;
    private ArrayList<POIPoint> entranceList;
    private String entranceType;
    private int trafficSignalsSound;
    private int trafficSignalsVibration;

    public POIPoint(String name, Double lat, Double lon, String subType) {
    	super(name, lat, lon, "poi", subType);
        this.address = "";
        this.website = "";
        this.phone = "";
        this.email = "";
        this.openingHours = "";
        this.entranceList = new ArrayList<POIPoint>();
        this.outerBuilding = null;
        this.entranceType = "";
        this.trafficSignalsSound = -1;
        this.trafficSignalsVibration = -1;
    }

    public POIPoint(String name, Location location, String subType) {
    	super(name, location, "poi", subType);
        this.address = "";
        this.website = "";
        this.phone = "";
        this.email = "";
        this.openingHours = "";
        this.entranceList = new ArrayList<POIPoint>();
        this.outerBuilding = null;
        this.entranceType = "";
        this.trafficSignalsSound = -1;
        this.trafficSignalsVibration = -1;
    }

    public POIPoint(String name, Double lat, Double lon, String type, String subType) {
    	super(name, lat, lon, type, subType);
        this.address = "";
        this.website = "";
        this.phone = "";
        this.email = "";
        this.openingHours = "";
        this.entranceList = new ArrayList<POIPoint>();
        this.outerBuilding = null;
        this.entranceType = "";
        this.trafficSignalsSound = -1;
        this.trafficSignalsVibration = -1;
    }

    public POIPoint(String name, Location location, String type, String subType) {
    	super(name, location, type, subType);
        this.address = "";
        this.website = "";
        this.phone = "";
        this.email = "";
        this.openingHours = "";
        this.entranceList = new ArrayList<POIPoint>();
        this.outerBuilding = null;
        this.entranceType = "";
        this.trafficSignalsSound = -1;
        this.trafficSignalsVibration = -1;
    }

    public void addAddress(String addr) {
        this.address = addr;
    }

    public String getAddress() {
        return this.address;
    }

    public void addPhone(String phone) {
        this.phone = phone;
    }

    public String getPhone() {
        return this.phone;
    }

    public void addWebsite(String website) {
        this.website = website;
    }

    public String getWebsite() {
        return this.website;
    }

    public void addEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return this.email;
    }

    public void addOpeningHours(String oh) {
        this.openingHours = oh;
    }

    public String getOpeningHours() {
        return this.openingHours;
    }

    public void addEntranceToList( POIPoint entrance ) {
        if (entrance != null)
            this.entranceList.add( entrance );
    }

    public ArrayList<POIPoint> getEntranceList() {
        return this.entranceList;
    }

    public void addOuterBuilding( POIPoint building ) {
        this.outerBuilding = building;
    }

    public POIPoint getOuterBuilding() {
        return this.outerBuilding;
    }

    public void addEntranceType( String type ) {
        this.entranceType = type;
    }

    public String getEntranceType() {
        return this.entranceType;
    }

    public void addTrafficSignalsSound(int value) {
        this.trafficSignalsSound = value;
    }

    public void addTrafficSignalsVibration(int value) {
        this.trafficSignalsVibration = value;
    }

    public boolean hasTrafficSignalsAccessibility() {
        if (this.trafficSignalsSound > -1 || this.trafficSignalsVibration > -1)
            return true;
        return false;
    }

    public String printTrafficSignalsAccessibility() {
        ArrayList<String> trafficSignalProperties = new ArrayList<String>();
        if (this.trafficSignalsSound == 0) {
            trafficSignalProperties.add(String.format(
                    Globals.getContext().getResources().getString(R.string.roPointTrafficSignalsSound),
                    Globals.getContext().getResources().getString(R.string.dialogNo)));
        } else if (this.trafficSignalsSound == 1) {
            trafficSignalProperties.add(String.format(
                    Globals.getContext().getResources().getString(R.string.roPointTrafficSignalsSound),
                    Globals.getContext().getResources().getString(R.string.dialogYes)));
        }
        if (this.trafficSignalsVibration == 0) {
            trafficSignalProperties.add(String.format(
                    Globals.getContext().getResources().getString(R.string.roPointTrafficSignalsVibration),
                    Globals.getContext().getResources().getString(R.string.dialogNo)));
        } else if (this.trafficSignalsVibration == 1) {
            trafficSignalProperties.add(String.format(
                    Globals.getContext().getResources().getString(R.string.roPointTrafficSignalsVibration),
                    Globals.getContext().getResources().getString(R.string.dialogYes)));
        }
        return TextUtils.join(", ", trafficSignalProperties);
    }

    /**
     * routing instruction for this poi point
     * routeIndex:
     *      0: first route object
     *      1: intermediate route object
     *      2:  last route object
     */
    public String getRoutingPointInstruction(int routeIndex) {
        if (routeIndex == 0) {
            return String.format(
                    Globals.getContext().getResources().getString(R.string.messagePointDescStationStart),
                    this.toString());
        } else if (routeIndex == 1) {
            String nextDirection = HelperFunctions.getFormatedDirection(super.getTurn());
            if (nextDirection.equals(Globals.getContext().getResources().getString(R.string.directionStraightforward))) {
                return String.format(
                        Globals.getContext().getResources().getString(R.string.messagePointDescStationInterAhead),
                        this.toString());
            } else if (nextDirection.equals(Globals.getContext().getResources().getString(R.string.directionBehindYou))) {
                return String.format(
                        Globals.getContext().getResources().getString(R.string.messagePointDescStationInterBehind),
                        this.toString());
            } else if (! nextDirection.equals("")) {
                return String.format(
                        Globals.getContext().getResources().getString(R.string.messagePointDescStationInterTurn),
                        this.toString(), nextDirection);
            } else {
                return String.format(
                        Globals.getContext().getResources().getString(R.string.messagePointDescStationInterNoTurnValue),
                        this.toString());
            }
        } else if (routeIndex == 2) {
            return String.format(
                    Globals.getContext().getResources().getString(R.string.messagePointDescStationDestination),
                    this.toString());
        }
        return "";
    }

    public JSONObject toJson() {
        JSONObject jsonObject = super.toJson();
        if (jsonObject == null)
            return null;
        JSONArray entranceArray = new JSONArray();
        for (POIPoint entrance : this.entranceList) {
            JSONObject entranceJson = entrance.toJson();
            if (entranceJson != null)
                entranceArray.put( entranceJson );
        }
        try {
            jsonObject.put("entrance_list", entranceArray);
        } catch (JSONException e) {}
        if (this.outerBuilding != null) {
            JSONObject outer = this.outerBuilding.toJson();
            try {
                if (outer != null)
                    jsonObject.put("is_inside", outer);
            } catch (JSONException e) {}
        }
        if (! this.entranceType.equals("")) {
            try {
                jsonObject.put("entrance", this.entranceType);
            } catch (JSONException e) {}
        }
        if (! this.address.equals("")) {
            try {
                jsonObject.put("address", this.address);
            } catch (JSONException e) {}
        }
        if (! this.phone.equals("")) {
            try {
                jsonObject.put("phone", this.phone);
            } catch (JSONException e) {}
        }
        if (! this.website.equals("")) {
            try {
                jsonObject.put("website", this.website);
            } catch (JSONException e) {}
        }
        if (! this.email.equals("")) {
            try {
                jsonObject.put("email", this.email);
            } catch (JSONException e) {}
        }
        if (! this.openingHours.equals("")) {
            try {
                jsonObject.put("opening_hours", this.openingHours);
            } catch (JSONException e) {}
        }
        if (this.trafficSignalsSound > -1) {
            try {
                jsonObject.put("traffic_signals_sound", this.trafficSignalsSound);
            } catch (JSONException e) {}
        }
        if (this.trafficSignalsVibration > -1) {
            try {
                jsonObject.put("traffic_signals_vibration", this.trafficSignalsVibration);
            } catch (JSONException e) {}
        }
        return jsonObject;
    }
}
