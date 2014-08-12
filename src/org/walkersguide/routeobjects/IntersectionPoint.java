package org.walkersguide.routeobjects;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.R;
import org.walkersguide.utils.Globals;
import org.walkersguide.utils.HelperFunctions;

public class IntersectionPoint extends Point {

    private ArrayList<IntersectionWay> subPoints;
    private ArrayList<POIPoint> trafficSignalList;
    private int numberOfStreetsWithName;

    public IntersectionPoint(String name, Double lat, Double lon, String subType, int numberOfStreetsWithName) {
    	super(name, lat, lon, "intersection", subType);
        this.subPoints = new ArrayList<IntersectionWay>();
        this.trafficSignalList = new ArrayList<POIPoint>();
        this.numberOfStreetsWithName = numberOfStreetsWithName;
    }

    public void addSubPoint(IntersectionWay w) {
        subPoints.add(w);
    }

    public ArrayList<IntersectionWay> getSubPoints() {
        return this.subPoints;
    }

    public int getNumberOfStreets() {
        return this.subPoints.size();
    }

    public int getNumberOfBigStreets() {
        return this.numberOfStreetsWithName;
    }

    public void addTrafficSignalToList( POIPoint signal ) {
        if (signal != null)
            this.trafficSignalList.add( signal );
    }

    public ArrayList<POIPoint> getTrafficSignalList() {
        return this.trafficSignalList;
    }

    public String toString() {
        String s = String.format(
                Globals.getContext().getResources().getString(R.string.roIntersection),
                super.getName(), this.subPoints.size() );
        if (super.getTurn() >= 0) {
            s += String.format( Globals.getContext().getResources().getString(R.string.roPointTurn),
                    HelperFunctions.getFormatedDirection(super.getTurn()) );
        } else if (super.getDistance() >= 0 && super.getBearing() >= 0) {
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
        JSONArray trafficSignalArray = new JSONArray();
        for (POIPoint signal : this.trafficSignalList) {
            JSONObject signalJson = signal.toJson();
            if (signalJson != null)
                trafficSignalArray.put( signalJson );
        }
        try {
            jsonObject.put("traffic_signal_list", trafficSignalArray);
        } catch (JSONException e) {}
        JSONArray intersectionWayArray = new JSONArray();
        for (IntersectionWay way : this.subPoints) {
            JSONObject wayJson = way.toJson();
            if (wayJson != null)
                intersectionWayArray.put( wayJson );
            else
                return null;
        }
        try {
            jsonObject.put("sub_points", intersectionWayArray);
        } catch (JSONException e) {
            return null;
        }
        try {
            jsonObject.put("number_of_streets_with_name", this.numberOfStreetsWithName);
        } catch (JSONException e) {}
        return jsonObject;
    }

    public class IntersectionWay {

        private String name;
        private double latitude;
        private double longitude;
        private int intersection_bearing;
        private int wayId;
        private String subType;
        private String surface;
        private int sidewalk;

        public IntersectionWay(String name, Double lat, Double lon, int bearing, String subType) {
            this.latitude = lat;
            this.longitude = lon;
            this.name = name;
            this.intersection_bearing = bearing;
            this.subType = subType;
            this.surface = "";
            this.sidewalk = -1;
            this.wayId = -1;
        }

    	public Double getLatitude() {
	    	return this.latitude;
    	}

    	public Double getLongitude() {
	    	return this.longitude;
    	}

        public String getName() {
            return this.name;
        }

        public int getIntersectionBearing() {
            return this.intersection_bearing;
        }

        public String getSubType() {
            return this.subType;
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

        public void addWayId(int id) {
            this.wayId = id;
        }

        public int getWayId() {
            return this.wayId;
        }

        public String toString() {
            String s = "";
            if (this.getName().toLowerCase().contains( this.getSubType().toLowerCase() )) {
                s += this.getName();
            } else {
                s += this.getName() + " (" + this.getSubType() + ")";
            }
            if (! this.getName().toLowerCase().contains( this.getSurface().toLowerCase() )) {
                s += String.format(
                        Globals.getContext().getResources().getString(R.string.roWaySurface),
                        getSurface() );
            }
            if (this.getSidewalk() >= 0) {
                s += ", " + this.printSidewalk();
            }
            return s;
        }

        public JSONObject toJson() {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("name", this.name);
                jsonObject.put("lat", this.latitude);
                jsonObject.put("lon", this.longitude);
                jsonObject.put("sub_type", this.subType);
                jsonObject.put("intersection_bearing", this.intersection_bearing);
            } catch (JSONException e) {
                return null;
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
            if (this.wayId > -1) {
                try {
                    jsonObject.put("way_id", this.wayId);
                } catch (JSONException e) {}
            }
            return jsonObject;
        }

    }
}
