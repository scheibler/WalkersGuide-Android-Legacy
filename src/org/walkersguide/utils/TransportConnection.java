package org.walkersguide.utils;

import java.util.ArrayList;

import org.walkersguide.R;

public class TransportConnection implements Comparable<TransportConnection> {

    private ArrayList<Route> routes;
    private String vehicles;

    public TransportConnection(String vehicles) {
        this.vehicles = vehicles;
        this.routes = new ArrayList<Route>();
    }

    public String getVehicles() {
        return this.vehicles;
    }

    public int getMinimalCost() {
        int minimalCost = 10000;
        for (Route route : this.routes) {
            if (minimalCost > route.getCost()) {
                minimalCost = route.getCost();
            }
        }
        return minimalCost;
    }

    public int getAverageCost() {
        int minimalCost = 0;
        for (Route route : this.routes) {
            minimalCost += route.getCost();
        }
        return minimalCost / routes.size();
    }

    public int getNumberOfRoutes() {
        return this.routes.size();
    }

    public void addRoute(Route route) {
        this.routes.add(route);
    }

    public Route getRouteAtIndex(int index) {
        if ((index >= 0) && (index < routes.size()))
            return routes.get(index);
        return null;
    }

    public String toString() {
        if (routes.size() == 1) {
            return String.format(
                    Globals.getContext().getResources().getString(R.string.tcDescriptionSingleConnection),
                    this.vehicles, this.getMinimalCost(), this.getAverageCost());
        } else {
            return String.format(
                    Globals.getContext().getResources().getString(R.string.tcDescriptionMultipleConnections),
                    this.vehicles, routes.size(), this.getMinimalCost(), this.getAverageCost());
        }
    }

    @Override public int hashCode() {
        final int prime = 31;
        return prime + vehicles.hashCode();
    }

    @Override public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof TransportConnection))
            return false;
        TransportConnection other = (TransportConnection) obj;
        return this.vehicles.equals(other.getVehicles());
    }

    @Override public int compareTo(TransportConnection obj) {
        if (this.getMinimalCost() == obj.getMinimalCost()) {
            return 0;
        } else if (this.getMinimalCost() < obj.getMinimalCost()) {
            return -1;
        } else {
            return 1;
        }
    }
}
