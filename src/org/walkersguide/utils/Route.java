package org.walkersguide.utils;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;
import org.walkersguide.R;
import org.walkersguide.routeobjects.FootwaySegment;
import org.walkersguide.routeobjects.RouteObjectWrapper;


public class Route {

    private ArrayList<RouteObjectWrapper> routeList;
    private int pointIndex;
    private int segmentIndex;
    private String routeDescription;
    private int cost;

    public Route() {
        // create an empty source route
        this.routeList = new ArrayList<RouteObjectWrapper>();
        this.routeList.add(new RouteObjectWrapper());
        this.routeList.add(new RouteObjectWrapper(
                new FootwaySegment(
                    Globals.getContext().getResources().getString(R.string.labelFootwayPlaceholder),
                    -1, -1, "footway_place_holder")));
        this.routeList.add(new RouteObjectWrapper());
        // some other variables
        this.routeDescription = "";
        this.pointIndex = 0;
        this.segmentIndex = -1;
        this.cost = 0;
    }

    public Route(ArrayList<RouteObjectWrapper> list) {
        this.routeList = list;
        this.routeDescription = "";
        this.pointIndex = 0;
        this.segmentIndex = -1;
        this.cost = 0;
    }

    public Route(ArrayList<RouteObjectWrapper> list, String description) {
        this.routeList= list;
        this.pointIndex = 0;
        this.segmentIndex = -1;
        this.routeDescription = description;
        this.cost = 0;
    }

    public Route(ArrayList<RouteObjectWrapper> list, String description, int cost) {
        this.routeList= list;
        this.pointIndex = 0;
        this.segmentIndex = -1;
        this.routeDescription = description;
        this.cost = cost;
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
            segmentIndex -= 2;
        }
    }

    public void nextPoint() {
        if (pointIndex < (routeList.size()-2) ) {
            pointIndex += 2;
            segmentIndex += 2;
        }
    }

    public RouteObjectWrapper getRouteObjectAtIndex(int index) {
        if ((index >= 0) && (index < routeList.size()))
            return this.routeList.get(index);
        return new RouteObjectWrapper();
    }

    public void addRouteObjectAtIndex(int index, RouteObjectWrapper object) {
        this.routeList.add(index, object);
    }

    public void replaceRouteObjectAtIndex(int index, RouteObjectWrapper object) {
        if ((index >= 0) && (index < routeList.size()))
            this.routeList.set(index, object);
    }

    public void removeRouteObjectAtIndex(int index) {
        if ((index >= 0) && (index < routeList.size()))
            this.routeList.remove(index);
    }

    public int getSize() {
        return this.routeList.size();
    }

    public int getCost() {
        return this.cost;
    }

    public String getRoutingPointInstruction() {
        RouteObjectWrapper routeObject = this.routeList.get(pointIndex);
        if (this.getNextPointNumber() == 1) {
            return routeObject.getRoutingPointInstruction(0);
        } else if (this.getNextPointNumber() == this.getNumberOfPoints()) {
            return routeObject.getRoutingPointInstruction(2);
        } else {
            return routeObject.getRoutingPointInstruction(1);
        }
    }

    public String getRoutingSegmentInstruction() {
        if (segmentIndex < 0)
            return "";
        if (this.routeList.get( segmentIndex ).getFootwaySegment() != null
                || this.routeList.get( segmentIndex ).getTransportSegment() != null) {
            return this.routeList.get(segmentIndex).getRoutingSegmentInstruction();
        }
        return "";
    }

    public String getRouteDescription() {
        if (this.routeDescription.equals("")) {
            String description = String.format(
                    Globals.getContext().getResources().getString(R.string.labelSourceRouteDescription),
                    this.routeList.get(0).getPoint().getName(),
                    this.routeList.get(this.routeList.size()-1).getPoint().getName() );
            if (this.routeList.size() > 3) {
                description += String.format(
                        Globals.getContext().getResources().getString(R.string.labelSourceRouteDescriptionNumInterPoints),
                        ((this.routeList.size()-3)/2) );
            }
            // calculate length of route
            int distance = 0;
            for (int i=0; i<this.routeList.size()-2; i=i+2) {
                distance += this.routeList.get(i).getPoint().distanceTo(this.routeList.get(i+2).getPoint());
            }
            description += String.format(
                    Globals.getContext().getResources().getString(R.string.labelSourceRouteDescriptionBeeLine),
                    distance);
            return description;
        }
        return this.routeDescription;
    }

    public String getReverseRouteDescription() {
        if (this.routeDescription.equals("")) {
            String description = String.format(
                    Globals.getContext().getResources().getString(R.string.labelSourceRouteDescription),
                    this.routeList.get(this.routeList.size()-1).getPoint().getName(),
                    this.routeList.get(0).getPoint().getName() );
            if (this.routeList.size() > 3) {
                description += String.format(
                        Globals.getContext().getResources().getString(R.string.labelSourceRouteDescriptionNumInterPoints),
                        ((this.routeList.size()-3)/2) );
            }
            // calculate length of route
            int distance = 0;
            for (int i=0; i<this.routeList.size()-2; i=i+2) {
                distance += this.routeList.get(i).getPoint().distanceTo(this.routeList.get(i+2).getPoint());
            }
            description += String.format(
                    Globals.getContext().getResources().getString(R.string.labelSourceRouteDescriptionBeeLine),
                    distance);
            return description;
        }
        return this.routeDescription;
    }

    public String toString() {
        return getRouteDescription();
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
        return prime + this.getRouteDescription().hashCode() + this.getReverseRouteDescription().hashCode();
	}

	@Override public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Route))
			return false;
		Route other = (Route) obj;
        if (this.getRouteDescription().equals(other.getRouteDescription())
                || this.getRouteDescription().equals(other.getReverseRouteDescription())) {
            return true;
        }
        return false;
    }

}
