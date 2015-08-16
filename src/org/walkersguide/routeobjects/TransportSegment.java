package org.walkersguide.routeobjects;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.R;
import org.walkersguide.utils.Globals;

public class TransportSegment {

    private String line;
    private String to;
    private String departure_time;
    private long departureTimeMillis;
    private String duration;
    private String arrival_time;
    private long arrivalTimeMillis;
    private ArrayList<String> stops;

    public TransportSegment(String line, String direction, String departure_time, String duration, String arrival_time) {
        this.line = line;
        this.to = direction;
        this.departure_time = departure_time;
        this.departureTimeMillis = 0;
        this.duration = duration;
        this.arrival_time = arrival_time;
        this.arrivalTimeMillis = 0;
        this.stops = new ArrayList<String>();
    }

    public String getName() {
        return "Linie " + this.line + " nach " + this.to;
    }

    public String getLine() {
        return this.line;
    }

    public String getDepartureTime() {
        return this.departure_time;
    }

    public String getArrivaltime() {
        return this.arrival_time;
    }

    public String getDuration() {
        return this.duration;
    }

    public int getNumberOfStops() {
        return this.stops.size();
    }

    public void addStop(String stop) {
        this.stops.add(stop);
    }

    public ArrayList<String> getStops() {
        return this.stops;
    }

    public long getDepartureTimeMillis() {
        return this.departureTimeMillis;
    }

    public void addDepartureMillis(long value) {
        this.departureTimeMillis = value;
    }

    public long getArrivaltimeMillis() {
        return this.arrivalTimeMillis;
    }

    public void addArrivalTimeMillis(long value) {
        this.arrivalTimeMillis = value;
    }

    public String getRoutingSegmentInstruction() {
        return String.format(
                Globals.getContext().getResources().getString(R.string.messageSegmentDescTransport),
                this.getName(), this.getDepartureTime(),
                this.getDuration(), this.getNumberOfStops());
    }

    public String toString() {
        String s = String.format(
                Globals.getContext().getResources().getString(R.string.roTransportDescription),
                this.line, this.to, this.departure_time, this.arrival_time);
        if (this.stops.size() > 0) {
            s += String.format(
                    Globals.getContext().getResources().getString(R.string.roTransportIntermediateStops),
                    this.stops.size() );
        }
        return s;
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("line", this.line);
            jsonObject.put("direction", this.to);
            jsonObject.put("type", "transport");
            jsonObject.put("departure_time", this.departure_time);
            jsonObject.put("duration", this.duration);
            jsonObject.put("arrival_time", this.arrival_time);
        } catch (JSONException e) {
            return null;
        }
        JSONArray stopArray = new JSONArray();
        for (String stop : this.stops) {
            stopArray.put(stop);
        }
        try {
            jsonObject.put("stops", stopArray);
        } catch (JSONException e) {}
        try {
            jsonObject.put("departure_time_millis", this.departureTimeMillis);
        } catch (JSONException e) {}
        try {
            jsonObject.put("arrival_time_millis", this.arrivalTimeMillis);
        } catch (JSONException e) {}
        return jsonObject;
    }

}
