package org.walkersguide.routeobjects;

import org.json.JSONObject;



public class RouteObjectWrapper {

    private Point wayPoint;
    private IntersectionPoint intersection;
    private StationPoint station;
    private POIPoint poi;
    private FootwaySegment footway;
    private TransportSegment transport;
    private boolean isEmpty;

    public RouteObjectWrapper() {
        this.isEmpty = true;
    }

    public RouteObjectWrapper( Point p) {
        this.isEmpty = false;
        if (p instanceof IntersectionPoint) {
            this.intersection = (IntersectionPoint) p;
        } else if (p instanceof StationPoint) {
            this.station = (StationPoint) p;
        } else if (p instanceof POIPoint) {
            this.poi = (POIPoint) p;
        } else {
            this.wayPoint = p;
        }
    }

    public RouteObjectWrapper( FootwaySegment s) {
        this.footway = s;
        this.isEmpty = false;
    }

    public RouteObjectWrapper( TransportSegment s) {
        this.transport = s;
        this.isEmpty = false;
    }

    public boolean isEmpty() {
        return this.isEmpty;
    }

    public Point getPoint() {
        if (wayPoint != null)
            return this.wayPoint;
        else if (intersection != null)
            return (Point) this.intersection;
        else if (station !=null)
            return (Point) this.station;
        else if (poi != null)
            return (Point) this.poi;
        return null;
    }

    public Point getWayPoint() {
        return this.wayPoint;
    }

    public IntersectionPoint getIntersection() {
        return this.intersection;
    }

    public StationPoint getStation() {
        return this.station;
    }

    public POIPoint getPOI() {
        return this.poi;
    }

    public FootwaySegment getFootwaySegment() {
        return this.footway;
    }

    public TransportSegment getTransportSegment() {
        return this.transport;
    }

    public String toString() {
        if (wayPoint != null)
            return this.wayPoint.toString();
        else if (intersection != null)
            return this.intersection.toString();
        else if (station !=null)
            return this.station.toString();
        else if (poi != null)
            return this.poi.toString();
        else if (footway != null)
            return this.footway.toString();
        else if (transport != null)
            return this.transport.toString();
        return "";
    }

    public JSONObject toJson() {
        if (wayPoint != null)
            return this.wayPoint.toJson();
        else if (intersection != null)
            return this.intersection.toJson();
        else if (station !=null)
            return this.station.toJson();
        else if (poi != null)
            return this.poi.toJson();
        else if (footway != null)
            return this.footway.toJson();
        else if (transport != null)
            return this.transport.toJson();
        return null;
    }
}
