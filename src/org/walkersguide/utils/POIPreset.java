package org.walkersguide.utils;

import java.util.ArrayList;

import org.walkersguide.routeobjects.Point;


public class POIPreset implements Comparable<POIPreset> {

    public enum UpdateStatus {
        UNCHANGED, HOLDLISTPOSITION, RESETLISTPOSITION
    }

    private int id;
    private String name;
    private int range;
    private String tags;
    private Point lastLocation;
    private Point lastLocationSinceDownload;
    private int lastCompassValue;
    private String lastSearchString;
    private UpdateStatus poiListStatus;
    private boolean downloadedNewData;
    private ArrayList<Point> poiList;

    public POIPreset(int id, String name, int range, String tags) {
        this.id = id;
        this.name = name;
        this.range = range;
        this.tags = tags;
        this.lastLocation = null;
        this.lastLocationSinceDownload = null;
        this.lastCompassValue = 0;
        this.lastSearchString = "";
        this.poiListStatus = UpdateStatus.UNCHANGED;
        this.downloadedNewData = false;
        this.poiList = new ArrayList<Point>();
    }

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public int getRange() {
        return this.range;
    }

    public void setRange(int range) {
        this.range = range;
    }

    public String getTags() {
        return this.tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Point getLastLocation() {
        return this.lastLocation;
    }

    public void setLastLocation(Point p) {
        this.lastLocation = p;
    }

    public Point getLastLocationSinceDownload() {
        return this.lastLocationSinceDownload;
    }

    public void setLastLocationSinceDownload(Point p) {
        this.lastLocationSinceDownload = p;
    }

    public int getLastCompassValue() {
        return this.lastCompassValue;
    }

    public void setLastCompassValue(int c) {
        this.lastCompassValue = c;
    }

    public String getLastSearchString() {
        return this.lastSearchString;
    }

    public void setLastSearchString(String s) {
        this.lastSearchString = s;
    }

    public ArrayList<Point> getPOIList() {
        return this.poiList;
    }

    public void setPOIList(ArrayList<Point> list) {
        this.poiList = list;
    }

    public UpdateStatus getPOIListStatus() {
        return this.poiListStatus;
    }

    public void setPOIListStatus(UpdateStatus status) {
        this.poiListStatus = status;
    }

    public boolean getDownloadedNewData() {
        return this.downloadedNewData;
    }

    public void setDownloadedNewData(boolean b) {
        this.downloadedNewData = b;
    }

    public String toString() {
        return this.name;
    }

	@Override public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}

	@Override public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof POIPreset))
			return false;
		POIPreset other = (POIPreset) obj;
		if (id != other.id)
			return false;
		return true;
	}

	@Override public int compareTo(POIPreset obj) {
        if (this.id == obj.getId())
            return 0;
        else if (this.id < obj.getId())
            return -1;
        else
            return 1;
    }
}
