package org.walkersguide.utils;

import java.util.ArrayList;
import java.util.Collections;

import org.json.JSONArray;
import org.json.JSONObject;
import org.walkersguide.routeobjects.FootwaySegment;
import org.walkersguide.routeobjects.IntersectionPoint;
import org.walkersguide.routeobjects.POIPoint;
import org.walkersguide.routeobjects.Point;
import org.walkersguide.routeobjects.RouteObjectWrapper;
import org.walkersguide.routeobjects.StationPoint;
import org.walkersguide.routeobjects.TransportSegment;


public class Route {

    private ArrayList<RouteObjectWrapper> routeList;
    private ArrayList<Point> subPointList;
    private int pointIndex;
    private int segmentIndex;
    private int direction;
    private String routeDescription;

    public Route(ArrayList<RouteObjectWrapper> list, String description) {
        this.routeList= list;
        subPointList = new ArrayList<Point>();
        this.pointIndex = 0;
        this.segmentIndex = -1;
        this.direction = 1;
        this.routeDescription = description;
    }

    public ArrayList<RouteObjectWrapper> getRouteList() {
        return this.routeList;
    }

    public RouteObjectWrapper getNextPoint() {
        return this.routeList.get(pointIndex);
    }

    public RouteObjectWrapper getPreviousPoint() {
        if (pointIndex > 1)
            return this.routeList.get(pointIndex-2);
        return new RouteObjectWrapper();
    }

    public RouteObjectWrapper getNextSegment() {
        if (segmentIndex < 0)
            return new RouteObjectWrapper();
        if (this.routeList.get( segmentIndex ).getFootwaySegment() != null)
            return this.routeList.get( segmentIndex );
        else if (this.routeList.get( segmentIndex ).getTransportSegment() != null)
            return this.routeList.get( segmentIndex );
        else
            return new RouteObjectWrapper();
    }

    public ArrayList<Point> getSegmentSubPoints() {
        return this.subPointList;
    }

    public int getListPosition() {
        return this.pointIndex;
    }

    public void setListPosition(int position) {
        if ((pointIndex >= 0) && (pointIndex < routeList.size())) {
            if ((position % 2) == 1)
                this.pointIndex = position+1;
            else
                this.pointIndex = position;
            this.segmentIndex = this.pointIndex -1;
            this.direction = 1;
        }
    }

    public int getNextPointNumber() {
        return (this.pointIndex / 2) + 1;
    }

    public int getNumberOfPoints() {
        if (routeList.size() > 0)
            return (this.routeList.size()+1) / 2;
        return 0;
    }

    public void previousPoint() {
        if (pointIndex > 1) {
            pointIndex -= 2;
            if (direction == 0)
                segmentIndex -= 2;
            direction = 0;
            // load subPoint array
            subPointList = new ArrayList<Point>();
            if ( getNextSegment().getFootwaySegment() != null) {
                subPointList = (ArrayList<Point>) getNextSegment().getFootwaySegment().getPOIs().clone();
                Collections.reverse( subPointList );
            }
        }
    }

    public void nextPoint() {
        if (pointIndex < (routeList.size()-2) ) {
            pointIndex += 2;
            if (direction == 1)
                segmentIndex += 2;
            direction = 1;
            // load subPoint array
            subPointList = new ArrayList<Point>();
            if ( getNextSegment().getFootwaySegment() != null)
                subPointList = (ArrayList<Point>) getNextSegment().getFootwaySegment().getPOIs().clone();
        }
    }

    public RouteObjectWrapper getRouteObjectAtIndex(int index) {
        if ((index >= 0) && (index < routeList.size()))
            return this.routeList.get(index);
        return new RouteObjectWrapper();
    }

    public void addWayPoint(Point p) {
        this.routeList.add(new RouteObjectWrapper(p));
    }

    public void addIntersection(IntersectionPoint p) {
        this.routeList.add(new RouteObjectWrapper(p));
    }

    public void addPOI(POIPoint p) {
        this.routeList.add(new RouteObjectWrapper(p));
    }

    public void addStation(StationPoint p) {
        this.routeList.add(new RouteObjectWrapper(p));
    }

    public void addFootwaySegment(FootwaySegment s) {
        this.routeList.add(new RouteObjectWrapper(s));
    }

    public void addTransportSegment(TransportSegment s) {
        this.routeList.add(new RouteObjectWrapper(s));
    }

    public String toString() {
        return routeDescription;
    }

    public JSONArray toJson() {
        JSONArray array = new JSONArray();
        for (RouteObjectWrapper routeObject : this.routeList) {
            JSONObject routeObjectJson = routeObject.toJson();
            if (routeObjectJson != null) {
                array.put( routeObjectJson );
            } else {
                return null;
            }
        }
        return array;
    }

	@Override public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((routeList == null) ? 0 : routeList.hashCode());
		return result;
	}

	@Override public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Route))
			return false;
		Route other = (Route) obj;
		if (routeList == null) {
			if (other.getRouteList() != null)
				return false;
		} else if (routeList.size() != other.getRouteList().size()) {
            return false;
		} else if (!routeList.equals(other.getRouteList())) {
			return false;
        }
		return true;
	}

}
