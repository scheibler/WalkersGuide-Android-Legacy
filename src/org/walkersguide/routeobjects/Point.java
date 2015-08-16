package org.walkersguide.routeobjects;

import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.R;
import org.walkersguide.utils.Globals;
import org.walkersguide.utils.HelperFunctions;

import android.location.Location;
import android.location.LocationManager;

public class Point implements Comparable<Point> {

    private double latitude;
    private double longitude;
    private String name;
    private String type;
    private String subType;
    private int nodeId;
    private int turn;
    private int distance;
    private int bearing;
    private int accuracy;
    private int tactilePaving;
    private int wheelchair;
    private Location locationObject;

    public Point(String name, double lat, double lon) {
        this.latitude = lat;
        this.longitude = lon;
        this.locationObject = null;
        this.name = name;
        this.type = "way_point";
        this.subType = "";
        this.nodeId = -1;
        this.turn = -361;
        this.distance = -1;
        this.bearing = -1;
        this.accuracy = -1;
        this.wheelchair = -1;
        this.tactilePaving = -1;
    }

    public Point(String name, Location location) {
        this.latitude = location.getLatitude();
        this.longitude = location.getLongitude();
        this.name = name;
        this.locationObject = location;
        this.type = "way_point";
        this.subType = "";
        this.nodeId = -1;
        this.turn = -361;
        this.distance = -1;
        this.bearing = -1;
        this.accuracy = (int) Math.round( location.getAccuracy() );
        this.wheelchair = -1;
        this.tactilePaving = -1;
    }

    public Point(String name, double lat, double lon, String type, String subType) {
        this.latitude = lat;
        this.longitude = lon;
        this.locationObject = null;
        this.name = name;
        this.type = type;
        this.subType = subType;
        this.nodeId = -1;
        this.turn = -361;
        this.distance = -1;
        this.bearing = -1;
        this.wheelchair = -1;
        this.tactilePaving = -1;
    } 

    public Point(String name, Location location, String type, String subType) {
        this.latitude = location.getLatitude();
        this.longitude = location.getLongitude();
        this.locationObject = location;
        this.name = name;
        this.type = type;
        this.subType = subType;
        this.nodeId = -1;
        this.turn = -361;
        this.distance = -1;
        this.bearing = -1;
        this.wheelchair = -1;
        this.tactilePaving = -1;
    } 

	public double getLatitude() {
		return this.latitude;
	}

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

	public double getLongitude() {
		return this.longitude;
	}

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public Location getLocationObject() {
        return this.locationObject;
    }

    public void setLocationObject(Location location) {
        this.locationObject = location;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubType() {
        return this.subType;
    }

    public void addNodeId(int id) {
        this.nodeId = id;
    }

    public int getNodeId() {
        return this.nodeId;
    }

    public void addTurn(int turn) {
        this.turn = turn;
    }

    public int getTurn() {
        return this.turn;
    }

    public void addWheelchair(int wheelchair) {
        this.wheelchair = wheelchair;
    }

    public int getWheelchair() {
        return this.wheelchair;
    }

    public String printWheelchair() {
        if (this.wheelchair == 0) {
            return Globals.getContext().getResources().getString(R.string.roPointWheelchairNo);
        } else if (this.wheelchair == 1) {
            return Globals.getContext().getResources().getString(R.string.roPointWheelchairPartly);
        } else if (this.wheelchair == 2) {
            return Globals.getContext().getResources().getString(R.string.roPointWheelchairYes);
        }
        return "";
    }

    public void addTactilePaving(int tp) {
        this.tactilePaving = tp;
    }

    public int getTactilePaving() {
        return this.tactilePaving;
    }

    public String printTactilePaving() {
        if (this.tactilePaving == 0) {
            return Globals.getContext().getResources().getString(R.string.roPointTactileNo);
        } else if (this.tactilePaving == 1) {
            return Globals.getContext().getResources().getString(R.string.roPointTactileYes);
        }
        return "";
    }

    public void addDistance(int dist) {
        this.distance = dist;
    }

    public int getDistance() {
        return this.distance;
    }

    public void addBearing(int bearing) {
        this.bearing = bearing;
        if (this.bearing < 0)
            this.bearing += 360;
    }

    public int getBearing() {
        return this.bearing;
    }

    public void addAccuracy(int acc) {
        this.accuracy = acc;
    }

    public int getAccuracy() {
        return this.accuracy;
    }

    public int distanceTo(Point p) {
        float results[] = new float[3];
        Location.distanceBetween(
                latitude,
                longitude,
                p.getLatitude(),
                p.getLongitude(),
                results );
        return (int) Math.round(results[0]);
    }

    public int bearingTo(Point p) {
        Location start = new Location(LocationManager.GPS_PROVIDER);
        start.setLatitude(latitude);
        start.setLongitude(longitude);
        Location dest = new Location(LocationManager.GPS_PROVIDER);
        // System.out.println("dest name = " + p.getName());
        // System.out.println("dest lon = " + p.getLongitude());
        // System.out.println("dest lat = " + p.getLatitude());
        dest.setLatitude(p.getLatitude());
        dest.setLongitude(p.getLongitude());
        int bearing = (int) Math.round( start.bearingTo(dest) );
        if (bearing < 0)
            return bearing + 360;
        else
            return bearing;
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
            return String.format(Globals.getContext().getResources().getString(R.string.messagePointDescWayPointStart));
        } else if (routeIndex == 1) {
            String nextDirection = HelperFunctions.getFormatedDirection(this.getTurn());
            if (nextDirection.equals(Globals.getContext().getResources().getString(R.string.directionStraightforward))) {
                return String.format(Globals.getContext().getResources().getString(R.string.messagePointDescWayPointInterAhead));
            } else if (nextDirection.equals(Globals.getContext().getResources().getString(R.string.directionBehindYou))) {
                return String.format(Globals.getContext().getResources().getString(R.string.messagePointDescWayPointInterBehind));
            } else if (! nextDirection.equals("")) {
                return String.format(Globals.getContext().getResources().getString(R.string.messagePointDescWayPointInterTurn), nextDirection);
            } else {
                return String.format(Globals.getContext().getResources().getString(R.string.messagePointDescWayPointInterNoTurnValue));
            }
        } else if (routeIndex == 2) {
            return String.format(Globals.getContext().getResources().getString(R.string.messagePointDescWayPointDestination));
        }
        return "";
    }

	public String toString() {
        String s = "";
        if (this.getSubType().equals("") || this.getName().equals(this.getSubType())) {
            s += this.getName();
        } else {
            s += this.getName() + " (" + this.getSubType() + ")";
        }
        if (this instanceof POIPoint) {
            POIPoint poi = (POIPoint) this;
            if (poi.getEntranceList().size() == 1) {
                s += Globals.getContext().getResources().getString(R.string.roPointOneEntrance);
            } else if (poi.getEntranceList().size() > 1) {
                s += String.format(
                        Globals.getContext().getResources().getString(R.string.roPointMultipleEntrances),
                        poi.getEntranceList().size() );
            }
        }
        if (this.getTurn() >= 0) {
            s += String.format( Globals.getContext().getResources().getString(R.string.roPointTurn),
                    HelperFunctions.getFormatedDirection(this.getTurn()) );
        } else if (this.getDistance() >= 0 && this.getBearing() >= 0) {
            s += String.format( Globals.getContext().getResources().getString(R.string.roPointDistanceAndBearing),
                    this.getDistance(), HelperFunctions.getFormatedDirection(this.getBearing()) );
            if (((Globals) Globals.getContext()).getSettingsManagerInstance().useGPSAsBearingSource())
                s += " (GPS)";
        } else if (this.getDistance() >= 0) {
            s += String.format( Globals.getContext().getResources().getString(R.string.roPointDistance),
                    this.getDistance() );
        }
        return s;
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", this.name);
            jsonObject.put("lat", this.latitude);
            jsonObject.put("lon", this.longitude);
            jsonObject.put("type", this.type);
            jsonObject.put("sub_type", this.subType);
        } catch (JSONException e) {
            return null;
        }
        if (this.nodeId > -1) {
            try {
                jsonObject.put("node_id", this.nodeId);
            } catch (JSONException e) {}
        }
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
        if (this.turn > -1) {
            try {
                jsonObject.put("turn", this.turn);
            } catch (JSONException e) {}
        }
        return jsonObject;
    }

	@Override public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(latitude);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(longitude);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Point))
			return false;
		Point other = (Point) obj;
        if (this.name.equals(other.getName())) {
            if (this.distanceTo(other) < 10) {
                return true;
            }
            return false;
        }
        if (this.latitude != other.getLatitude()) {
            return false;
        } else if (this.longitude != other.getLongitude()) {
            return false;
        }
		return true;
	}

	@Override public int compareTo(Point obj) {
        if ((this.latitude == obj.getLatitude()) && (this.longitude == obj.getLongitude()))
            return 0;
        if ((this.distance == -1) && (obj.getDistance() == -1))
            return this.name.compareTo(obj.getName());
        else if (this.distance == -1)
            return 1;
        else if (obj.getDistance() == -1)
            return -1;
        if (this.distance > obj.getDistance())
            return 1;
        else if (this.distance < obj.getDistance())
            return -1;
        else
            return this.name.compareTo(obj.getName());
	}
}
