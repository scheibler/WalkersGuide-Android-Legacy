package org.walkersguide.routeobjects;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.R;
import org.walkersguide.utils.Globals;

public class FootwaySegment {

    private String name;
    private int bearing;
    private int distance;
    private int wayId;
    private int wayClass;
    private ArrayList<Point> pois;
    private String type;
    private String subType;
    private String surface;
    private int sidewalk;
    private int tactilePaving;
    private int wheelchair;

    public FootwaySegment(String name, int dist, int bearing, String subType) {
        this.name = name;
        this.distance = dist;
        this.bearing = bearing;
        this.type = "footway";
        this.subType= subType;
        this.wayId = -1;
        this.wayClass = -1;
        this.surface = "";
        this.sidewalk = -1;
        this.wheelchair = -1;
        this.tactilePaving = -1;
        this.pois = new ArrayList<Point>();
    }

    public String getName() {
        return this.name;
    }

    public void addName(String name) {
        this.name = name;
    }

    public int getDistance() {
        return this.distance;
    }

    public int getBearing() {
        return this.bearing;
    }

    public String getSubType() {
        return this.subType;
    }

    public void addPOI(Point row) {
        this.pois.add(row);
    }

    public ArrayList<Point> getPOIs() {
        return this.pois;
    }

    public void addWayClass(int value) {
        this.wayClass = value;
    }

    public int getWayClass() {
        return this.wayClass;
    }

    public void addWayId(int id) {
        this.wayId = id;
    }

    public int getWayId() {
        return this.wayId;
    }

    public void addWheelchair(int wheelchair) {
        this.wheelchair = wheelchair;
    }

    public int getWheelchair() {
        return this.wheelchair;
    }

    public void addTactilePaving(int tp) {
        this.tactilePaving = tp;
    }

    public int getTactilePaving() {
        return this.tactilePaving;
    }


    public void addSurface(String surface) {
        this.surface = surface;
    }

    public String getSurface() {
        return this.surface;
    }

    public void addSidewalk(int sidewalk) {
        this.sidewalk = sidewalk;
    }

    public int getSidewalk() {
        return this.sidewalk;
    }

    public String printSidewalk() {
        if (this.sidewalk == 0) {
            return Globals.getContext().getResources().getString(R.string.roWaySidewalkNo);
        } else if (this.sidewalk == 1) {
            return Globals.getContext().getResources().getString(R.string.roWaySidewalkLeft);
        } else if (this.sidewalk == 2) {
            return Globals.getContext().getResources().getString(R.string.roWaySidewalkRight);
        } else if (this.sidewalk == 3) {
            return Globals.getContext().getResources().getString(R.string.roWaySidewalkBoth);
        }
        return "";
    }

    public String toString() {
        String s = "";
        if (this.getName().equals(this.getSubType())
                || this.getSubType().equals("footway_place_holder")
                || this.getSubType().equals("transport_place_holder")) {
            s += this.getName();
        } else {
            s += this.getName() + " (" + this.getSubType() + ")";
        }
        if (this.getDistance() > -1) {
            s += String.format(
                    Globals.getContext().getResources().getString(R.string.roWayDistance),
                    this.getDistance() );
        }
        if (! this.surface.equals(""))
            s += String.format(
                    Globals.getContext().getResources().getString(R.string.roWaySurface),
                    this.getSurface() );
        return s;
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", this.name);
            jsonObject.put("distance", this.distance);
            jsonObject.put("bearing", this.bearing);
            jsonObject.put("type", this.type);
            jsonObject.put("sub_type", this.subType);
        } catch (JSONException e) {
            return null;
        }
        JSONArray poiArray = new JSONArray();
        for (Point p : this.pois) {
            JSONObject pointJson = p.toJson();
            if (pointJson != null)
                poiArray.put( pointJson );
        }
        try {
            jsonObject.put("pois", poiArray);
        } catch (JSONException e) {}
        if (this.tactilePaving > -1) {
            try {
                jsonObject.put("tactile_paving", this.tactilePaving);
            } catch (JSONException e) {}
        }
        if (this.wheelchair > -1) {
            try {
                jsonObject.put("wheelchair", this.wheelchair);
            } catch (JSONException e) {}
        }
        if (this.sidewalk > -1) {
            try {
                jsonObject.put("sidewalk", this.sidewalk);
            } catch (JSONException e) {}
        }
        if (!this.surface.equals("")) {
            try {
                jsonObject.put("surface", this.surface);
            } catch (JSONException e) {}
        }
        if (this.wayClass > -1) {
            try {
                jsonObject.put("way_class", this.wayClass);
            } catch (JSONException e) {}
        }
        if (this.wayId > -1) {
            try {
                jsonObject.put("way_id", this.wayId);
            } catch (JSONException e) {}
        }
        return jsonObject;
    }

	@Override public int hashCode() {
        return this.wayId;
    }

	@Override public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof FootwaySegment))
			return false;
		FootwaySegment other = (FootwaySegment) obj;
        if (this.wayId == other.getWayId())
            return true;
        return false;
    }
}
