package org.walkersguide.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.R;
import org.walkersguide.exceptions.DepartureListParsingException;
import org.walkersguide.exceptions.PointListParsingException;
import org.walkersguide.exceptions.RouteParsingException;
import org.walkersguide.routeobjects.FootwaySegment;
import org.walkersguide.routeobjects.IntersectionPoint;
import org.walkersguide.routeobjects.POIPoint;
import org.walkersguide.routeobjects.Point;
import org.walkersguide.routeobjects.RouteObjectWrapper;
import org.walkersguide.routeobjects.StationPoint;
import org.walkersguide.routeobjects.TransportSegment;

/**
 * This class parses the JSON formated server answers of queried routes and poi
 * and translates them into the defined route objects like intersections, poi and stations
 */

public class ObjectParser {

    /**
     * This function parses a single incoming route
     * It's a wrapper for the parseRouteArray function in which the actual parsing of the containing
     * route objects takes place
     * This function is called when a single footway route was recived and filters possible
     * transmission errors
     * Input is a JSON object from server
     * Returns a Route object or a routeParsingexception in case of failure
     */
    public static Route parseSingleRoute(JSONObject jsonObject) throws RouteParsingException {
        Route route = null;
        CharSequence  text = "";
        try {
            if (jsonObject == null) {
                text = Globals.getContext().getResources().getString(R.string.messageUnknownError);
            } else if (! jsonObject.getString("error").equals("")) {
                text = String.format(
                        Globals.getContext().getResources().getString(R.string.messageErrorFromServer),
                        jsonObject.getString("error") );
            } else {                    
                JSONArray points = jsonObject.getJSONArray("route");
                String description = jsonObject.getString("description");
                route = new Route(parseRouteArray(points), description);
            }
        } catch (JSONException e) {
            text = String.format(
                    Globals.getContext().getResources().getString(R.string.messageJSONError),
                    e.getMessage() );
        } catch (RouteParsingException e) {
            text = e.getMessage();
        }
        if (!text.equals("")) {
            throw new RouteParsingException(String.valueOf(text));
        }
        return route;
    }

    /**
     * This function parses multiple  incoming routes
     * It's a wrapper for the parseRouteArray function in which the actual parsing of the containing
     * route objects takes place
     * This function is called when a set of public transport routes was recived
     * It filters possible transmission errors
     * Input is a JSON object from server
     * Returns a list of TransportConnection objects or a routeParsingexception in case of failure
     */
    public static ArrayList<TransportConnection> parseMultipleRoutes(JSONObject jsonObject) throws RouteParsingException {
        ArrayList<TransportConnection> connectionList = new ArrayList<TransportConnection>();
        CharSequence  text = "";
        try {
            if (jsonObject == null) {
                text = Globals.getContext().getResources().getString(R.string.messageUnknownError);
            } else if (! jsonObject.getString("error").equals("")) {
                text = String.format(
                        Globals.getContext().getResources().getString(R.string.messageErrorFromServer),
                        jsonObject.getString("error") );
            } else {
                JSONObject jsonConnectionList = jsonObject.getJSONObject("transport_routes");
                for(Iterator<String> iter = jsonConnectionList.keys(); iter.hasNext();) {
                    TransportConnection connection = new TransportConnection(iter.next());
                    JSONArray jsonConnection = jsonConnectionList.getJSONArray(connection.getVehicles());
                    for (int i=0; i<jsonConnection.length(); i++) {
                        JSONObject jsonRoute = jsonConnection.getJSONObject(i);
                        connection.addRoute(
                            new Route(
                                parseRouteArray( jsonRoute.getJSONArray("route")),
                                jsonRoute.getString("description"),
                                jsonRoute.getInt("cost")
                            ));
                    }
                    if (connection.getNumberOfRoutes() > 0)
                        connectionList.add(connection);
                }
            }
        } catch (JSONException e) {
            text = String.format(
                    Globals.getContext().getResources().getString(R.string.messageJSONError),
                    e.getMessage() );
        } catch (RouteParsingException e) {
            text = e.getMessage();
        }
        if (!text.equals("")) {
            throw new RouteParsingException(String.valueOf(text));
        }
        Collections.sort(connectionList);
        return connectionList;
    }

    /**
     * This function parses a recived route from server
     * Input is a JSON-Array which contains the json encoded route objects
     * It's called from the wrapper functions parseSingleRoute and parseMultipleRoutes which
     * already stripped a possible error message from server
     * Returns a list of RouteObjectWrapper objects or a routeparsingexception in case of failure
     */
    public static ArrayList<RouteObjectWrapper> parseRouteArray(JSONArray points) throws RouteParsingException {
        ArrayList<RouteObjectWrapper> routeObjectList = new ArrayList<RouteObjectWrapper>();
        CharSequence  text = "";
        int control = 0;
        try {
            for (int i=0; i<points.length(); i++) {
                JSONObject point = points.getJSONObject(i);
                if (point.getString("type").equals("intersection")) {
                    IntersectionPoint intersection = parseIntersection(point);
                    if (intersection != null) {
                        routeObjectList.add( new RouteObjectWrapper(intersection) );
                        control++;
                    } else {
                        text = Globals.getContext().getResources().getString(R.string.messageRouteObjectParsingError);
                        break;
                    }
                } else if (point.getString("type").equals("poi")) {
                    POIPoint poi = parsePOI(point);
                    if (poi != null) {
                        routeObjectList.add( new RouteObjectWrapper(poi) );
                        control++;
                    } else {
                        text = Globals.getContext().getResources().getString(R.string.messageRouteObjectParsingError);
                        break;
                    }
                } else if (point.getString("type").equals("station")) {
                    StationPoint station = parseStation(point);
                    if (station != null) {
                        routeObjectList.add( new RouteObjectWrapper(station) );
                        control++;
                    } else {
                        text = Globals.getContext().getResources().getString(R.string.messageRouteObjectParsingError);
                        break;
                    }
                } else if (point.getString("type").equals("way_point")) {
                    Point wayPoint = parseWayPoint(point);
                    if (wayPoint != null) {
                        routeObjectList.add( new RouteObjectWrapper(wayPoint) );
                        control++;
                    } else {
                        text = Globals.getContext().getResources().getString(R.string.messageRouteObjectParsingError);
                        break;
                    }
                } else if (point.getString("type").equals("footway")) {
                    FootwaySegment footway = parseFootway(point);
                    if (footway != null) {
                        routeObjectList.add( new RouteObjectWrapper(footway) );
                        control--;
                    } else {
                        text = Globals.getContext().getResources().getString(R.string.messageRouteObjectParsingError);
                        break;
                    }
                } else if (point.getString("type").equals("transport")) {
                    TransportSegment transport = parseTransport(point);
                    if (transport != null) {
                        routeObjectList.add( new RouteObjectWrapper(transport) );
                        control--;
                    } else {
                        text = Globals.getContext().getResources().getString(R.string.messageRouteObjectParsingError);
                        break;
                    }
                } else {
                    text = Globals.getContext().getResources().getString(R.string.messageUnsupportedRouteObject);
                    break;
                }
                if (control < 0) {
                    text = Globals.getContext().getResources().getString(R.string.messageInvalidRouteTwoSegments);
                    break;
                }
                if (control > 1) {
                    text = Globals.getContext().getResources().getString(R.string.messageInvalidRouteTwoPoints);
                    break;
                }
            }
            if (control == 0)
                text = Globals.getContext().getResources().getString(R.string.messageInvalidRouteNoDestination);
            if (routeObjectList.size() == 0)
                text = Globals.getContext().getResources().getString(R.string.messageInvalidRouteEmpty);
        } catch (JSONException e) {
            text = String.format(
                    Globals.getContext().getResources().getString(R.string.messageJSONError),
                    e.getMessage() );
        }
        if (!text.equals("")) {
            throw new RouteParsingException(String.valueOf(text));
        }
        return routeObjectList;
    }

    /**
     * parses the input of the poi query
     * This function is a wrapper and filters the possible transmission errors
     * The actual parsing of the poi points takes place in the parsePointArray function
     * This wrapper is called from the activities and fragments and returns a list of poi points or
     * an PointListParsingException
     */
    public static ArrayList<Point> parsePointList(JSONObject jsonPoints) throws PointListParsingException {
        ArrayList<Point> poiList = new ArrayList<Point>();
        CharSequence  text = "";
        try {
            if (jsonPoints == null) {
                text = Globals.getContext().getResources().getString(R.string.messageUnknownError);
            } else if (! jsonPoints.getString("error").equals("")) {
                text = String.format(
                        Globals.getContext().getResources().getString(R.string.messageErrorFromServer),
                        jsonPoints.getString("error") );
            } else {
                JSONArray poi = jsonPoints.getJSONArray("poi");
                poiList = parsePointArray(poi);
            }
        } catch (JSONException e) {
            text = String.format(
                    Globals.getContext().getResources().getString(R.string.messageJSONError),
                    e.getMessage() );
        }
        if (!text.equals("")) {
            throw new PointListParsingException(String.valueOf(text));
        }
        return poiList;
    }

    /**
     * In this funtion the actual point parsing takes place
     * Input is an JSON Array
     * The function detects the point type and calls the corresponding object parsing function
     * POI with a corrupted formating are skipped
     * returns a list of poi
     */
    public static ArrayList<Point> parsePointArray(JSONArray jsonPoints) {
        ArrayList<Point> poiList = new ArrayList<Point>();
        for (int i=0; i<jsonPoints.length(); i++) {
            try {
                JSONObject point = jsonPoints.getJSONObject(i);
                if (point.getString("type").equals("intersection")) {
                    IntersectionPoint intersection = parseIntersection(point);
                    if (intersection != null) {
                        poiList.add(intersection);
                    }
                } else if (point.getString("type").equals("poi")) {
                    POIPoint poi = parsePOI(point);
                    if (poi != null) {
                        poiList.add(poi);
                    }
                } else if (point.getString("type").equals("station")) {
                    StationPoint station = parseStation(point);
                    if (station != null) {
                        poiList.add(station);
                    }
                } else if (point.getString("type").equals("way_point")) {
                    Point wayPoint = parseWayPoint(point);
                    if (wayPoint != null) {
                        poiList.add(wayPoint);
                    }
                }
            } catch (JSONException e) {}
        }
        return poiList;
    }

    /*
     * This function parses a single route object
     * Input is a json formated string
     * returns the corresponding RouteObjectWrapper object or an empty one if the parsing failed
     */
    public static RouteObjectWrapper parseRouteObject(String point) {
        try {
            return parseRouteObject(new JSONObject(point));
        } catch (JSONException e) {
            return new RouteObjectWrapper();
        }
    }

    /*
     * This function parses a single route object
     * Input is a json object
     * returns the corresponding RouteObjectWrapper object or an empty one if the parsing failed
     */
    public static RouteObjectWrapper parseRouteObject(JSONObject point) {
        try {
            if (point.getString("type").equals("intersection")) {
                IntersectionPoint intersection = parseIntersection(point);
                if (intersection != null) {
                    return new RouteObjectWrapper( intersection );
                }
            } else if (point.getString("type").equals("poi")) {
                POIPoint poi = parsePOI(point);
                if (poi != null) {
                    return new RouteObjectWrapper( poi );
                }
            } else if (point.getString("type").equals("station")) {
                StationPoint station = parseStation(point);
                if (station != null) {
                    return new RouteObjectWrapper( station );
                }
            } else if (point.getString("type").equals("way_point")) {
                Point wayPoint = parseWayPoint(point);
                if (wayPoint != null) {
                    return new RouteObjectWrapper( wayPoint );
                }
            } else if (point.getString("type").equals("footway")) {
                FootwaySegment footway = parseFootway(point);
                if (footway != null) {
                    return new RouteObjectWrapper( footway );
                }
            } else if (point.getString("type").equals("transport")) {
                TransportSegment transport = parseTransport(point);
                if (transport != null) {
                    return new RouteObjectWrapper( transport );
                }
            }
        } catch (JSONException e) {}
        return new RouteObjectWrapper();
    }

    /**
     * This function parses a WayPoint object
     * Input is a JSOn object from server
     * Returns a Point object or null in case of failure
     */
    public static Point parseWayPoint(JSONObject p) {
    	Point point = new Point("", 0.0, 0.0);
        try {
            point = new Point(
                    p.getString("name"),
                    p.getDouble("lat"),
                    p.getDouble("lon") );
        } catch (JSONException e) {
        return null;
        }
        try {
            point.addNodeId(p.getInt("node_id"));
        } catch (JSONException e) {}
        try {
            point.addTurn(p.getInt("turn"));
        } catch (JSONException e) {}
        try {
            point.addWheelchair(p.getInt("wheelchair"));
        } catch (JSONException e) {}
        try {
            point.addTactilePaving(p.getInt("tactile_paving"));
        } catch (JSONException e) {}
        return point;
    }

    /**
     * This function parses a IntersectionPoint object
     * Input is a JSOn object from server
     * Returns a IntersectionPoint object or null in case of failure
     */
    public static IntersectionPoint parseIntersection(JSONObject p) {
    	IntersectionPoint intersection = new IntersectionPoint("", 0.0, 0.0, "", 0);
        try {
            intersection = new IntersectionPoint(
                    p.getString("name"),
                    p.getDouble("lat"),
                    p.getDouble("lon"),
                    p.getString("sub_type"),
                    p.getInt("number_of_streets_with_name") );
        } catch (JSONException e) {
            return null;
        }
        try {
            JSONArray subPoints = p.getJSONArray("sub_points");
            for (int j=0; j<subPoints.length(); j++) {
                JSONObject subPoint = subPoints.getJSONObject(j);
                IntersectionPoint.IntersectionWay intersectionWay = intersection.new IntersectionWay(
                        subPoint.getString("name"),
                        subPoint.getDouble("lat"),
                        subPoint.getDouble("lon"),
                        subPoint.getInt("intersection_bearing"),
                        subPoint.getString("sub_type") );
                try {
                    intersectionWay.addSurface(subPoint.getString("surface"));
                } catch (JSONException e) {}
                try {
                    intersectionWay.addSidewalk(subPoint.getInt("sidewalk"));
                } catch (JSONException e) {}
                try {
                    intersectionWay.addWayId(subPoint.getInt("way_id"));
                } catch (JSONException e) {}
                intersection.addSubPoint( intersectionWay );
            }
        } catch (JSONException e) {}
        // general node properties
        try {
            intersection.addNodeId(p.getInt("node_id"));
        } catch (JSONException e) {}
        try {
            intersection.addTurn(p.getInt("turn"));
        } catch (JSONException e) {}
        try {
            intersection.addWheelchair(p.getInt("wheelchair"));
        } catch (JSONException e) {}
        try {
            intersection.addTactilePaving(p.getInt("tactile_paving"));
        } catch (JSONException e) {}
        // traffic signal list
        try {
            JSONArray trafficSignalList = p.getJSONArray("traffic_signal_list");
            for (int j=0; j<trafficSignalList.length(); j++) {
                intersection.addTrafficSignalToList(
                    parsePOI( trafficSignalList.getJSONObject(j) ));
            }
        } catch (JSONException e) {}
        return intersection;
    }

    /**
     * This function parses a StationPoint object
     * Input is a JSOn object from server
     * Returns a StationPoint object or null in case of failure
     */
    public static StationPoint parseStation(JSONObject p) {
        StationPoint station = new StationPoint("", 0.0, 0.0, "");
        try {
            station = new StationPoint(
                    p.getString("name"),
                    p.getDouble("lat"),
                    p.getDouble("lon"),
                    p.getString("sub_type") );
        } catch (JSONException e) {
            return null;
        }
        // general node properties
        try {
            station.addNodeId(p.getInt("node_id"));
        } catch (JSONException e) {}
        try {
            station.addTurn(p.getInt("turn"));
        } catch (JSONException e) {}
        try {
            station.addWheelchair(p.getInt("wheelchair"));
        } catch (JSONException e) {}
        try {
            station.addTactilePaving(p.getInt("tactile_paving"));
        } catch (JSONException e) {}
        // poi properties
        try {
            station.addAddress(p.getString("address"));
        } catch (JSONException e) {}
        try {
            station.addOpeningHours(p.getString("opening_hours"));
        } catch (JSONException e) {}
        try {
            station.addWebsite(p.getString("website"));
        } catch (JSONException e) {}
        try {
            station.addEmail(p.getString("email"));
        } catch (JSONException e) {}
        try {
            station.addPhone(p.getString("phone"));
        } catch (JSONException e) {}
        // get outer building if available
        try {
            station.addOuterBuilding(
                    parsePOI( p.getJSONObject("is_inside") ));
        } catch (JSONException e) {}
        // try to get the entrance list of the building
        try {
            JSONArray entranceList = p.getJSONArray("entrance_list");
            for (int j=0; j<entranceList.length(); j++) {
                station.addEntranceToList(
                    parsePOI( entranceList.getJSONObject(j) ));
            }
        } catch (JSONException e) {}
        // entrance string
        try {
            station.addEntranceType(p.getString("entrance"));
        } catch (JSONException e) {}
        // station properties
        try {
            JSONArray vehicles = p.getJSONArray("vehicles");
            for (int j=0; j<vehicles.length(); j++) {
                station.addVehicle(vehicles.getString(j));
            }
        } catch (JSONException e) {}
        try {
            JSONArray lines = p.getJSONArray("lines");
            for (int j=0; j<lines.length(); j++) {
                station.addLine(
                        station.new Line(
                            lines.getJSONObject(j).getString("nr"),
                            lines.getJSONObject(j).getString("to")
                        ) );
            }
        } catch (JSONException e) {}
        try {
            station.addStationID(p.getInt("station_id"));
        } catch (JSONException e) {}
        try {
            station.addPlatformNumber(p.getString("platform_number"));
        } catch (JSONException e) {}
        try {
            if (p.getString("accuracy").equals("true"))
                station.addFoundInOSMDatabase( true );
            else
                station.addFoundInOSMDatabase( false );
        } catch (JSONException e) {}
        return station;
    }

    /**
     * This function parses a POIPoint object
     * Input is a JSOn object from server
     * Returns a POIPoint object or null in case of failure
     */
    public static POIPoint parsePOI(JSONObject p) {
        POIPoint poi = new POIPoint("", 0.0, 0.0, "");
        try {
            poi = new POIPoint(
                    p.getString("name"),
                    p.getDouble("lat"),
                    p.getDouble("lon"),
                    p.getString("sub_type") );
        } catch (JSONException e) {
            return null;
        }
        // general node properties
        try {
            poi.addNodeId(p.getInt("node_id"));
        } catch (JSONException e) {}
        try {
            poi.addTurn(p.getInt("turn"));
        } catch (JSONException e) {}
        try {
            poi.addWheelchair(p.getInt("wheelchair"));
        } catch (JSONException e) {}
        try {
            poi.addTactilePaving(p.getInt("tactile_paving"));
        } catch (JSONException e) {}
        // poi properties
        try {
            poi.addAddress(p.getString("address"));
        } catch (JSONException e) {}
        try {
            poi.addOpeningHours(p.getString("opening_hours"));
        } catch (JSONException e) {}
        try {
            poi.addWebsite(p.getString("website"));
        } catch (JSONException e) {}
        try {
            poi.addEmail(p.getString("email"));
        } catch (JSONException e) {}
        try {
            poi.addPhone(p.getString("phone"));
        } catch (JSONException e) {}
        try {
            poi.addTrafficSignalsSound(p.getInt("traffic_signals_sound"));
        } catch (JSONException e) {}
        try {
            poi.addTrafficSignalsVibration(p.getInt("traffic_signals_vibration"));
        } catch (JSONException e) {}
        // get outer building if available
        try {
            poi.addOuterBuilding(
                    parsePOI( p.getJSONObject("is_inside") ));
        } catch (JSONException e) {}
        // try to get the entrance list of the building
        try {
            JSONArray entranceList = p.getJSONArray("entrance_list");
            for (int j=0; j<entranceList.length(); j++) {
                poi.addEntranceToList(
                    parsePOI( entranceList.getJSONObject(j) ));
            }
        } catch (JSONException e) {}
        // entrance string
        try {
            poi.addEntranceType(p.getString("entrance"));
        } catch (JSONException e) {}
        return poi;
    }

    /**
     * This function parses a FootwaySegment object
     * Input is a JSOn object from server
     * Returns a FootwaySegment object or null in case of failure
     */
    public static FootwaySegment parseFootway(JSONObject s) {
        FootwaySegment footway = new FootwaySegment("", 0, 0, "");
        try {
            footway = new FootwaySegment(
                    s.getString("name"),
                    s.getInt("distance"),
                    s.getInt("bearing"),
                    s.getString("sub_type") );
        } catch (JSONException e) {
            return null;
        }
        try {
            JSONArray pois = s.getJSONArray("pois");
            for (int j=0; j<pois.length(); j++) {
                JSONObject obj = pois.getJSONObject(j);
                if (obj.getString("type").equals("station")) {
                    footway.addPOI( parseStation(obj) );
                } else if (obj.getString("type").equals("poi")) {
                    footway.addPOI( parsePOI(obj) );
                } else if (obj.getString("type").equals("intersection")) {
                    footway.addPOI( parseIntersection(obj) );
                } else if (obj.getString("type").equals("way_point")) {
                    footway.addPOI( parseWayPoint(obj) );
                } else {
                    continue;
                }
            }
        } catch (JSONException e) {}
        try {
            footway.addWayClass(s.getInt("way_class"));
        } catch (JSONException e) {}
        try {
            footway.addWayId(s.getInt("way_id"));
        } catch (JSONException e) {}
        try {
            footway.addSurface(s.getString("surface"));
        } catch (JSONException e) {}
        try {
            footway.addSidewalk(s.getInt("sidewalk"));
        } catch (JSONException e) {}
        try {
            footway.addWheelchair(s.getInt("wheelchair"));
        } catch (JSONException e) {}
        try {
            footway.addTactilePaving(s.getInt("tactile_paving"));
        } catch (JSONException e) {}
        return footway;
    }

    /**
     * This function parses a TransportSegment object
     * Input is a JSOn object from server
     * Returns a TransportSegment object or null in case of failure
     */
    public static TransportSegment parseTransport(JSONObject s) {
        TransportSegment transport = new TransportSegment("", "", "", "", "");
        try {
            transport = new TransportSegment(
                    s.getString("line"),
                    s.getString("direction"),
                    s.getString("departure_time"),
                    s.getString("duration"),
                    s.getString("arrival_time") );
        } catch (JSONException e) {
            return null;
        }
        try {
            JSONArray stops = s.getJSONArray("stops");
            for (int j=0; j<stops.length(); j++) {
                transport.addStop(stops.getString(j));
            }
        } catch (JSONException e) {}
        try {
            transport.addDepartureMillis(s.getLong("departure_time_millis"));
        } catch (JSONException e) {}
        try {
            transport.addArrivalTimeMillis(s.getLong("arrival_time_millis"));
        } catch (JSONException e) {}
        return transport;
    }

    /**
     * parses the input of the station departures query
     * This wrapper is called from the activities and fragments and returns
     * a list of StationPoint.Departure objects or a DepartureListParsingException
     */
    public static ArrayList<StationPoint.Departure> parseStationDepartureList(StationPoint station, JSONObject jsonDepartures)
        throws DepartureListParsingException {
        ArrayList<StationPoint.Departure> departureList = new ArrayList<StationPoint.Departure>();
        CharSequence  text = "";
        try {
            if (jsonDepartures == null) {
                text = Globals.getContext().getResources().getString(R.string.messageUnknownError);
            } else if (! jsonDepartures.getString("error").equals("")) {
                text = String.format(
                        Globals.getContext().getResources().getString(R.string.messageErrorFromServer),
                        jsonDepartures.getString("error") );
            } else {
                JSONArray jsonDepartureList = jsonDepartures.getJSONArray("departures");
                for (int i=0; i<jsonDepartureList.length(); i++) {
                    try {
                        JSONObject departure = jsonDepartureList.getJSONObject(i);
                        departureList.add(station.new Departure(
                                departure.getString("nr"),
                                departure.getString("to"),
                                departure.getInt("remaining"),
                                departure.getString("time") ));
                    } catch (JSONException e) {}
                }
            }
        } catch (JSONException e) {
            text = String.format(
                    Globals.getContext().getResources().getString(R.string.messageJSONError),
                    e.getMessage() );
        }
        if (!text.equals("")) {
            throw new DepartureListParsingException(String.valueOf(text));
        }
        return departureList;
    }

}
