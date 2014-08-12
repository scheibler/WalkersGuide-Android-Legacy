package org.walkersguide.routeobjects;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.R;
import org.walkersguide.utils.Globals;
import org.walkersguide.utils.HelperFunctions;


public class StationPoint extends POIPoint {

    private ArrayList<Line> lines;
    private int stationID;
    private boolean foundInOSMDatabase;
    private String platformNumber;

    public StationPoint(String name, Double lat, Double lon, String subType, boolean acc) {
    	super(name, lat, lon, "station", subType);
        this.lines = new ArrayList<Line>();
        this.foundInOSMDatabase = acc;
        this.platformNumber = "";
    }

    public void addLine(Line l) {
        this.lines.add(l);
    }

    public ArrayList<Line> getLines() {
        return this.lines;
    }

    public void addStationID(int id) {
        this.stationID = id;
    }

    public int getStationID() {
        return this.stationID;
    }

    public void addFoundInOSMDatabase(boolean b) {
        this.foundInOSMDatabase = b;
    }

    public boolean getFoundInOSMDatabase() {
        return this.foundInOSMDatabase;
    }

    public void addPlatformNumber(String platform) {
        this.platformNumber = platform;
    }

    public String getPlatformNumber() {
        return this.platformNumber;
    }

    public String toString() {
        String s = super.getName() + "(" + super.getSubType() + ")";
        if (!this.platformNumber.equals(""))
            s += String.format( Globals.getContext().getResources().getString(R.string.roStationPlatformNumber),
                    this.getPlatformNumber() );
        if (this.lines.size() > 0) {
            String lines = "";
            for (Line l : this.lines) {
                if (lines.equals("")) {
                    lines += l.getNr();
                } else {
                    lines += ", " + l.getNr();
                }
            }
            s += String.format(
                    Globals.getContext().getResources().getString(R.string.roStationLines), lines);
        }
        if (this.foundInOSMDatabase) {
            s += Globals.getContext().getResources().getString(R.string.roStationExactStopPosition);
        }
        if (super.getDistance() >= 0 && super.getBearing() >= 0) {
            s += String.format( Globals.getContext().getResources().getString(R.string.roPointDistanceAndBearing),
                    super.getDistance(), HelperFunctions.getClockDirection(super.getBearing()) );
        } else if (super.getDistance() >= 0) {
            s += String.format( Globals.getContext().getResources().getString(R.string.roPointDistance),
                    super.getDistance() );
        }
        return s;
    }

    public JSONObject toJson() {
        JSONObject jsonObject = super.toJson();
        if (jsonObject == null)
            return null;
        JSONArray linesArray = new JSONArray();
        for (Line line : this.lines) {
            JSONObject lineJson = line.toJson();
            if (lineJson != null)
                linesArray.put( lineJson );
        }
        try {
            jsonObject.put("lines", linesArray);
        } catch (JSONException e) {}
        try {
            jsonObject.put("found_in_osm_database", this.foundInOSMDatabase);
        } catch (JSONException e) {}
        if (this.stationID > -1) {
            try {
                jsonObject.put("stationID", this.stationID);
            } catch (JSONException e) {}
        }
        if (!this.platformNumber.equals("")) {
            try {
                jsonObject.put("platform_number", this.platformNumber);
            } catch (JSONException e) {}
        }
        return jsonObject;
    }

    public class Line {

        private String nr;
        private String to;

        public Line(String nr, String to) {
            this.nr = nr;
            this.to = to;
        }

        public String getNr() {
            return this.nr;
        }

        public String getTo() {
            return this.to;
        }

        public String toString() {
            String s = this.nr;
            if (! to.equals(""))
                s += String.format( Globals.getContext().getResources().getString(R.string.roStationLineDirection), this.to);
            return s;
        }

        public JSONObject toJson() {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("nr", this.nr);
                jsonObject.put("to", this.to);
            } catch (JSONException e) {
                return null;
            }
            return jsonObject;
        }
    }
}
