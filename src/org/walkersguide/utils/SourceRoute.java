package org.walkersguide.utils;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;
import org.walkersguide.R;
import org.walkersguide.routeobjects.FootwaySegment;
import org.walkersguide.routeobjects.RouteObjectWrapper;

public class SourceRoute {

    private ArrayList<RouteObjectWrapper> routeList;

    public SourceRoute() {
        this.routeList = new ArrayList<RouteObjectWrapper>();
        this.routeList.add(new RouteObjectWrapper());
        this.routeList.add(new RouteObjectWrapper(
                new FootwaySegment(
                    Globals.getContext().getResources().getString(R.string.labelFootwayPlaceholder),
                    -1, -1, "footway_place_holder")));
        this.routeList.add(new RouteObjectWrapper());
    }

    public SourceRoute(ArrayList<RouteObjectWrapper> list) {
        this.routeList = list;
    }

    public ArrayList<RouteObjectWrapper> getRouteList() {
        return this.routeList;
    }

    public RouteObjectWrapper getRouteObjectAtIndex(int index) {
        return this.routeList.get(index);
    }

    public void addRouteObjectAtIndex(int index, RouteObjectWrapper object) {
        this.routeList.add(index, object);
    }

    public void replaceRouteObjectAtIndex(int index, RouteObjectWrapper object) {
        this.routeList.set(index, object);
    }

    public void removeRouteObjectAtIndex(int index) {
        this.routeList.remove(index);
    }

    public int getSize() {
        return this.routeList.size();
    }

    public String toString() {
        String description = String.format(
                Globals.getContext().getResources().getString(R.string.labelSourceRouteDescription),
                this.routeList.get(0).getPoint().getName(),
                this.routeList.get(this.routeList.size()-1).getPoint().getName() );
        if (this.routeList.size() > 3) {
            description += String.format(
                    Globals.getContext().getResources().getString(R.string.labelSourceRouteDescriptionNumInterPoints),
                    ((this.routeList.size()-3)/2) );
        }
        return description;
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
		if (!(obj instanceof SourceRoute))
			return false;
        ArrayList<RouteObjectWrapper> otherList = ((SourceRoute) obj).getRouteList();
		if (this.routeList == null && otherList == null) {
            return true;
		}
        if ( (this.routeList == null && otherList != null)
		        || (this.routeList != null && otherList == null) ) {
			return false;
        }
        if (this.routeList.size() == 0 && otherList.size() == 0) {
            return true;
		}
        if (this.routeList.size() != otherList.size()) {
            return false;
        }
        // compare start and destination points
        // if the start point of this object is neither the start or destination point of the given
        // source route, it's not the same
        // analog for the destination point of this route
        int permittedDistance = 25;
        if (this.routeList.get(0).getPoint().distanceTo(otherList.get(0).getPoint()) > permittedDistance
                && this.routeList.get(0).getPoint().distanceTo(otherList.get(otherList.size()-1).getPoint()) > permittedDistance) {
            return false;
        }
        if (this.routeList.get(this.routeList.size()-1).getPoint().distanceTo(otherList.get(otherList.size()-1).getPoint()) > permittedDistance
                && this.routeList.get(this.routeList.size()-1).getPoint().distanceTo(otherList.get(0).getPoint()) > permittedDistance) {
            return false;
        }
        // check via points of source routes
        for (int i=2; i<this.routeList.size()-2; i=i+2) {
            if (! this.routeList.get(i).getPoint().equals(otherList.get(i).getPoint())) {
                return false;
            }
        }
		return true;
	}

}
