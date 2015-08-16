package org.walkersguide.routeobjects;

import java.util.ArrayList;
import java.util.Collections;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.R;
import org.walkersguide.utils.Globals;
import org.walkersguide.utils.HelperFunctions;

import android.text.TextUtils;

public class IntersectionPoint extends Point {

    private ArrayList<IntersectionWay> subPoints;
    private ArrayList<POIPoint> trafficSignalList;
    private int numberOfStreetsWithName;
        private IntersectionWay prevWay, nextWay;

    public IntersectionPoint(String name, Double lat, Double lon, String subType, int numberOfStreetsWithName) {
    	super(name, lat, lon, "intersection", subType);
        this.subPoints = new ArrayList<IntersectionWay>();
        this.trafficSignalList = new ArrayList<POIPoint>();
        this.numberOfStreetsWithName = numberOfStreetsWithName;
        this.prevWay = null;
        this.nextWay = null;
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

    public String getIntersectionStructure(int userViewingDirection, int segmentDirection, boolean hideWayInUsersBack) {
        int bearingOfPrevSegment = segmentDirection - 180;
        if (bearingOfPrevSegment < 0)
            bearingOfPrevSegment += 360;
        int bearingOfNextSegment = segmentDirection + super.getTurn();
        if (bearingOfNextSegment >= 360)
            bearingOfNextSegment -= 360;
        // find closest intersection way and calculate relative bearings
        int absPrevWay = 0, absNextWay = 0;
        int minAbsPrevWay = 360, minAbsNextWay = 360;
        for (IntersectionWay way : this.getSubPoints()) {
            way.setRelativeBearing(userViewingDirection);
            // find prev way
            absPrevWay = Math.abs(bearingOfPrevSegment - way.getIntersectionBearing());
            if (absPrevWay > 180)
                absPrevWay = 360 - absPrevWay;
            if (absPrevWay < minAbsPrevWay) {
                minAbsPrevWay = absPrevWay;
                prevWay = way;
            }
            // find next way
            absNextWay = Math.abs(bearingOfNextSegment - way.getIntersectionBearing());
            if (absNextWay > 180)
                absNextWay = 360 - absNextWay;
            if (absNextWay < minAbsNextWay) {
                minAbsNextWay = absNextWay;
                nextWay = way;
            }
        }
        Collections.sort(this.getSubPoints());
        ArrayList<String> streetList = new ArrayList<String>();
        for (IntersectionWay way : this.getSubPoints()) {
            if (hideWayInUsersBack && way == prevWay)
                continue;
            if (super.getTurn() >= 0 && way == nextWay) {
                streetList.add(
                        HelperFunctions.getFormatedDirection(way.getRelativeBearing())
                        + ": *" + way.getName());
            } else {
                streetList.add(
                        HelperFunctions.getFormatedDirection(way.getRelativeBearing())
                        + ": " + way.getName() );
            }
        }
        return TextUtils.join(",", streetList);
    }

    public IntersectionWay getNextWay() {
        return this.nextWay;
    }

    /**
     * routing instruction for this intersection
     * routeIndex:
     *      0: first route object
     *      1: intermediate route object
     *      2:  last route object
     */
    public String getRoutingPointInstruction(int routeIndex) {
        if (routeIndex == 0) {
            return String.format(
                    Globals.getContext().getResources().getString(R.string.messagePointDescInterStart),
                    this.getName());
        } else if (routeIndex == 1) {
            String nextDirection = HelperFunctions.getFormatedDirection(super.getTurn());
            if (nextDirection.equals(Globals.getContext().getResources().getString(R.string.directionStraightforward))) {
                if (nextWay != null) {
                    return String.format(
                            Globals.getContext().getResources().getString(R.string.messagePointDescInterInterStraightWithName),
                            super.getName(), nextWay.getName());
                } else {
                    return String.format(
                            Globals.getContext().getResources().getString(R.string.messagePointDescInterInterStraight),
                            super.getName());
                }
            } else {
                if (nextWay != null) {
                    return String.format(
                            Globals.getContext().getResources().getString(R.string.messagePointDescInterInterWithStreet),
                            super.getName(), nextDirection, nextWay.getName());
                } else {
                    return String.format(
                            Globals.getContext().getResources().getString(R.string.messagePointDescInterInter),
                            super.getName(), nextDirection);
                }
            }
        } else if (routeIndex == 2) {
            return String.format(
                    Globals.getContext().getResources().getString(R.string.messagePointDescInterDestination),
                    this.getName());
        }
        return "";
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

    public class IntersectionWay implements Comparable<IntersectionPoint.IntersectionWay> {

        private String name;
        private double latitude;
        private double longitude;
        private int intersection_bearing, relativeBearing;
        private int wayId;
        private String subType;
        private String surface;
        private int sidewalk;

        public IntersectionWay(String name, Double lat, Double lon, int bearing, String subType) {
            this.latitude = lat;
            this.longitude = lon;
            this.name = name;
            this.intersection_bearing = bearing;
            this.relativeBearing = 0;
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

        public int getRelativeBearing() {
            return this.relativeBearing;
        }

        public void setRelativeBearing(int compassValue) {
            this.relativeBearing = this.intersection_bearing - compassValue;
            if (this.relativeBearing < -157)
                this.relativeBearing += 360;
            if (this.relativeBearing > 202)
                this.relativeBearing -= 360;
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

    	@Override public int hashCode() {
	    	final int prime = 31;
            return prime + this.name.hashCode() + this.intersection_bearing;
    	}

    	@Override public boolean equals(Object obj) {
	    	if (this == obj)
		    	return true;
    		if (obj == null)
	    		return false;
    		if (!(obj instanceof IntersectionPoint.IntersectionWay))
	    		return false;
    		IntersectionPoint.IntersectionWay other = (IntersectionPoint.IntersectionWay) obj;
            if (this.name.equals(other.getName()) && this.intersection_bearing == other.getIntersectionBearing())
                return true;
            return false;
        }

    	@Override public int compareTo(IntersectionPoint.IntersectionWay other) {
            if (this.relativeBearing < other.getRelativeBearing()) {
                return -1;
            } else if (this.relativeBearing > other.getRelativeBearing()) {
                return 1;
            }
            return this.name.compareTo(other.getName());
        }
    }
}
