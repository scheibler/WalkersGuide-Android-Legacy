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
    private int foundInOSMDatabase;
    private String platformNumber;
    private ArrayList<Departure> nextDepartures;
    private String queryDeparturesError;

    public StationPoint(String name, Double lat, Double lon, String subType) {
    	super(name, lat, lon, "station", subType);
        this.lines = new ArrayList<Line>();
        this.foundInOSMDatabase = -1;
        this.platformNumber = "";
        this.nextDepartures = null;
        this.queryDeparturesError = "";
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
        if (b)
            this.foundInOSMDatabase = 1;
        else
            this.foundInOSMDatabase = 0;
    }

    public int getFoundInOSMDatabase() {
        return this.foundInOSMDatabase;
    }

    public void addPlatformNumber(String platform) {
        this.platformNumber = platform;
    }

    public String getPlatformNumber() {
        return this.platformNumber;
    }

    public ArrayList<Departure> getNextDepartures() {
        return this.nextDepartures;
    }

    public void setNextDepartures(ArrayList<Departure> departureList) {
        this.nextDepartures = departureList;
    }

    public String getQueryDeparturesError() {
        return this.queryDeparturesError;
    }

    public void setQueryDeparturesError(String error) {
        this.queryDeparturesError = error;
    }

    public String toString() {
        String s = super.getName() + "(" + super.getSubType() + ")";
        if (!this.platformNumber.equals(""))
            s += String.format( Globals.getContext().getResources().getString(R.string.roStationPlatformNumber),
                    this.getPlatformNumber() );
        if (super.getEntranceList().size() == 1) {
            s += Globals.getContext().getResources().getString(R.string.roPointOneEntrance);
        } else if (super.getEntranceList().size() > 1) {
            s += String.format(
                    Globals.getContext().getResources().getString(R.string.roPointMultipleEntrances),
                    super.getEntranceList().size() );
        }
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
        if (this.foundInOSMDatabase == 0) {
            s += Globals.getContext().getResources().getString(R.string.roStationNoExactStopPosition);
        } else if (this.foundInOSMDatabase == 1) {
            s += Globals.getContext().getResources().getString(R.string.roStationExactStopPosition);
        }
        if (super.getDistance() >= 0 && super.getBearing() >= 0) {
            s += String.format( Globals.getContext().getResources().getString(R.string.roPointDistanceAndBearing),
                    super.getDistance(), HelperFunctions.getFormatedDirection(super.getBearing()) );
            if (((Globals) Globals.getContext()).getSettingsManagerInstance().useGPSAsBearingSource())
                s += " (GPS)";
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
            if (this.foundInOSMDatabase == 0) {
                jsonObject.put("accuracy", false);
            } else if (this.foundInOSMDatabase == 1) {
                jsonObject.put("accuracy", true);
            }
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

    public class Departure {

        private String line;
        private String direction;
        private int remaining;
        private String time;

        public Departure(String line, String direction, int remaining, String time) {
            this.line = line;
            this.direction = direction;
            this.remaining = remaining;
            this.time = time;
        }

        public String getLine() {
            return this.line;
        }

        public String getDirection() {
            return this.direction;
        }

        public int getRemaining() {
            return this.remaining;
        }

        public String getTime() {
            return this.time;
        }
    }
}
