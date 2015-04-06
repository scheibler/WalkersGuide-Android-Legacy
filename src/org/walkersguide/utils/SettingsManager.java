package org.walkersguide.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.walkersguide.R;
import org.walkersguide.exceptions.RouteParsingException;
import org.walkersguide.routeobjects.FootwaySegment;
import org.walkersguide.routeobjects.Point;
import org.walkersguide.routeobjects.RouteObjectWrapper;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Location;
import android.os.Environment;

public class SettingsManager {

    public enum BearingSource {
        COMPASS, GPS
    }

    // final settings
    private static final int interfaceVersion = 3;
    private static final String programDataFolder = "WalkersGuide";
    private static final String defaultHostURL = "http://walkersguide.org";
    private static final int defaultPort = 19021;
    private static final Double[] routeFactorArray = new Double[]{1.0, 1.5, 2.0, 3.0, 4.0};
    private static final double defaultRouteFactor = 2.0;
    private static final String[] defaultRoutingWayClasses = new String[]{"big_streets", "small_streets", "paved_ways", "unpaved_ways", "unclassified_ways", "steps"};
    private static final boolean defaultShakeForNextRoutePoint = true;
    private static final int defaultShakeIntensity = 2;
    private static final BearingSource defaultBearingSource = BearingSource.COMPASS;
    private static final String emailAddress = "support@walkersguide.org";

    private Context mContext;
    private SharedPreferences settings;
    // poi preset vars
    private ArrayList<POIPreset> poiPresets;
    private int presetIdInRouterFragment;
    private int presetIdInPoiFragment;
    private Route sourceRoute;
    private HashMap<String,Integer> temporaryStartFragmentSettings;
    private HashMap<String,Integer> temporaryRouterFragmentSettings;
    private HashMap<String,Integer> temporaryPOIFragmentSettings;
    // route vars
    private double routeFactor;
    private ArrayList<String> routingWayClasses;
    private boolean shakeForNextRoutePoint;
    private int shakeIntensity;
    private BearingSource bearingSource;
    // server vars
    private String hostURL;
    private int port;
    private String mapVersion;
    private boolean fakeCompassValues;

    public SettingsManager(Context context) {
        this.mContext = context;
        this.settings = context.getSharedPreferences( "WalkersGuideSettings", Context.MODE_PRIVATE);
        // general app settings
        this.presetIdInRouterFragment = 0;
        this.presetIdInPoiFragment = 0;
        this.sourceRoute = new Route();
        this.temporaryStartFragmentSettings = new HashMap<String,Integer>();
        this.temporaryRouterFragmentSettings = new HashMap<String,Integer>();
        this.temporaryPOIFragmentSettings = new HashMap<String,Integer>();
        this.mapVersion = "";

        // load settings
        // first: application settings
        loadApplicationSettings();
        // load preset settings
        loadPOIPresets();
        if (this.poiPresets.size() == 0)
            createDefaultPresets();

        // create necessary folders on the sdcard
        File logFolder = new File(getProgramLogFolder());
        if (!logFolder.exists())
            logFolder.mkdirs();
        File settingsFolder = new File(getProgramSettingsFolder());
        if (!settingsFolder.exists())
            settingsFolder.mkdirs();
        File importFolder = new File(getProgramImportFolder());
        if (!importFolder.exists())
            importFolder.mkdirs();
        this.fakeCompassValues = false;
    }

    /**
     * general settings part
     */

    public String getClientVersion() {
        try {
            return mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            return "";
        }
    }

    public int getInterfaceVersion() {
        return this.interfaceVersion;
    }

    public String getMapVersion() {
        return this.mapVersion;
    }

    public void setMapVersion(String version) {
        this.mapVersion = version;
    }

    public String getEMailAddress() {
        return this.emailAddress;
    }

    public String getProgramLogFolder() {
        return Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/" + this.programDataFolder + "/logs";
    }

    public String getProgramSettingsFolder() {
        return Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/" + this.programDataFolder + "/settings";
    }

    public String getProgramImportFolder() {
        return Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/" + this.programDataFolder + "/import";
    }

    public Route getRouteRequest() {
        return this.sourceRoute;
    }

    public void setRouteRequest(Route route) {
        this.sourceRoute = route;
    }

    public void addToTemporaryStartFragmentSettings(String key, Integer value) {
        this.temporaryStartFragmentSettings.put(key, value);
    }

    public Integer getValueFromTemporaryStartFragmentSettings(String key) {
        return this.temporaryStartFragmentSettings.get(key);
    }

    public void addToTemporaryRouterFragmentSettings(String key, Integer value) {
        this.temporaryRouterFragmentSettings.put(key, value);
    }

    public Integer getValueFromTemporaryRouterFragmentSettings(String key) {
        return this.temporaryRouterFragmentSettings.get(key);
    }

    public void addToTemporaryPOIFragmentSettings(String key, Integer value) {
        this.temporaryPOIFragmentSettings.put(key, value);
    }

    public Integer getValueFromTemporaryPOIFragmentSettings(String key) {
        return this.temporaryPOIFragmentSettings.get(key);
    }

    public String getServerPath() {
        return getHostURL() + ":" + getHostPort();
    }

    public String getHostURL() {
        if (! this.hostURL.startsWith("http"))
            return "http://" + this.hostURL;
        return this.hostURL;
    }

    public void setHostURL(String url) {
        this.hostURL = url;
        storeApplicationSettings();
    }

    public int getHostPort() {
        return this.port;
    }

    public void setHostPort(int port) {
        this.port = port;
        storeApplicationSettings();
    }

    public Double[] getRouteFactorArray() {
        return this.routeFactorArray;
    }

    public double getDefaultRouteFactor() {
        return this.defaultRouteFactor;
    }

    public double getRouteFactor() {
        return this.routeFactor;
    }

    public void setRouteFactor(double factor) {
        this.routeFactor = factor;
        storeApplicationSettings();
    }

    public ArrayList<String> getRoutingWayClasses() {
        return this.routingWayClasses;
    }

    public void setRoutingWayClasses(ArrayList<String> classes) {
        this.routingWayClasses = classes;
        storeApplicationSettings();
    }

    public boolean getShakeForNextRoutePoint() {
        return this.shakeForNextRoutePoint;
    }

    public void setShakeForNextRoutePoint(boolean b) {
        this.shakeForNextRoutePoint = b;
        storeApplicationSettings();
    }

    public int getShakeIntensity() {
        return this.shakeIntensity;
    }

    public void setShakeIntensity(int intensity) {
        this.shakeIntensity = intensity;
        storeApplicationSettings();
    }

    public BearingSource getBearingSource() {
        return this.bearingSource;
    }

    public void setBearingSource(BearingSource source) {
        this.bearingSource = source;
    }

    public boolean useGPSAsBearingSource() {
        if (this.bearingSource == BearingSource.GPS)
            return true;
        return false;
    }

    public boolean useCompassAsBearingSource() {
        if (this.bearingSource == BearingSource.COMPASS)
            return true;
        return false;
    }

    public boolean getFakeCompassValues() {
        return this.fakeCompassValues;
    }

    public void setFakeCompassValues(boolean b) {
        this.fakeCompassValues = b;
    }

    private void loadApplicationSettings() {
        String applicationSettingsString = settings.getString("ApplicationSettings", "");
        JSONObject jsonApplicationSettingsObject = new JSONObject();
        try {
            jsonApplicationSettingsObject = new JSONObject(applicationSettingsString);
        } catch (JSONException e) {
            this.hostURL = this.defaultHostURL;
            this.port = this.defaultPort;
            this.routeFactor = this.defaultRouteFactor;
            this.routingWayClasses = new ArrayList<String>();
            for (int i=0; i<this.defaultRoutingWayClasses.length; i++) {
                this.routingWayClasses.add(this.defaultRoutingWayClasses[i]);
            }
            this.shakeForNextRoutePoint = this.defaultShakeForNextRoutePoint;
            this.shakeIntensity = this.defaultShakeIntensity;
            this.bearingSource = this.defaultBearingSource;
            storeApplicationSettings();
            return;
        }
        try {
            // server settings
            JSONObject jsonServerSettings = jsonApplicationSettingsObject.getJSONObject("server");
            try {
                this.hostURL = jsonServerSettings.getString("host");
            } catch (JSONException e) {
                this.hostURL = this.defaultHostURL;
            }
            try {
                this.port = jsonServerSettings.getInt("port");
            } catch (JSONException e) {
                this.port = defaultPort;
            }
            // route settings
            JSONObject jsonRouteSettings = jsonApplicationSettingsObject.getJSONObject("route");
            try {
                this.routeFactor = jsonRouteSettings.getDouble("routeFactor");
            } catch (JSONException e) {
                this.routeFactor = defaultRouteFactor;
            }
            try {
                this.routingWayClasses = new ArrayList<String>();
                JSONArray jsonWayClassArray = jsonRouteSettings.getJSONArray("routingWayClasses");
                for (int i=0; i<jsonWayClassArray.length(); i++) {
                    if (Arrays.asList(this.defaultRoutingWayClasses).contains(jsonWayClassArray.getString(i)))
                        this.routingWayClasses.add(jsonWayClassArray.getString(i));
                }
            } catch (JSONException e) {
                this.routingWayClasses = new ArrayList<String>();
                for (int i=0; i<this.defaultRoutingWayClasses.length; i++) {
                    this.routingWayClasses.add(this.defaultRoutingWayClasses[i]);
                }
            }
            try {
                this.shakeForNextRoutePoint = jsonRouteSettings.getBoolean("shakeForNextRoutePoint");
            } catch (JSONException e) {
                this.shakeForNextRoutePoint = defaultShakeForNextRoutePoint;
            }
            try {
                this.shakeIntensity = jsonRouteSettings.getInt("shakeIntensity");
                if (this.shakeIntensity < 0 || this.shakeIntensity > 4)
                    this.shakeIntensity = defaultShakeIntensity;
            } catch (JSONException e) {
                this.shakeIntensity = defaultShakeIntensity;
            }
            try {
                if (jsonRouteSettings.getString("bearingSource").equals(BearingSource.GPS.name()))
                    this.bearingSource = BearingSource.GPS;
                else
                    this.bearingSource = BearingSource.COMPASS;
            } catch (JSONException e) {
                this.bearingSource = defaultBearingSource;
            }
            // misc settings
            JSONObject jsonMiscSettings = jsonApplicationSettingsObject.getJSONObject("misc");
            try {
                this.presetIdInPoiFragment = jsonMiscSettings.getInt("presetIdInPoiFragment");
            } catch (JSONException e) {
                this.presetIdInPoiFragment = 0;
            }
            try {
                this.presetIdInRouterFragment = jsonMiscSettings.getInt("presetIdInRouterFragment");
            } catch (JSONException e) {
                this.presetIdInRouterFragment = 0;
            }
        } catch (JSONException e) {
            System.out.println(e.getMessage());
        }
    }

    private void storeApplicationSettings() {
        JSONObject jsonApplicationSettingsObject = new JSONObject();
        // server settings
        try {
            JSONObject jsonServerSettingsObject  = new JSONObject();
            jsonServerSettingsObject.put("host", this.hostURL);
            jsonServerSettingsObject.put("port", this.port);
            jsonApplicationSettingsObject.put("server", jsonServerSettingsObject);
        } catch (JSONException e) {
            System.out.println("server settings couldn't be saved\n" + e.getMessage());
        }
        // route settings
        try {
            JSONObject jsonRouteSettingsObject  = new JSONObject();
            jsonRouteSettingsObject.put("routeFactor", this.routeFactor);
            jsonRouteSettingsObject.put("routingWayClasses", new JSONArray(this.routingWayClasses));
            jsonRouteSettingsObject.put("shakeForNextRoutePoint", this.shakeForNextRoutePoint);
            jsonRouteSettingsObject.put("shakeIntensity", this.shakeIntensity);
            jsonRouteSettingsObject.put("bearingSource", this.defaultBearingSource.name());
            jsonApplicationSettingsObject.put("route", jsonRouteSettingsObject);
        } catch (JSONException e) {
            System.out.println("route settings couldn't be saved\n" + e.getMessage());
        }
        // misc settings
        try {
            JSONObject jsonMiscSettingsObject  = new JSONObject();
            jsonMiscSettingsObject.put("presetIdInPoiFragment", this.presetIdInPoiFragment);
            jsonMiscSettingsObject.put("presetIdInRouterFragment", this.presetIdInRouterFragment);
            jsonApplicationSettingsObject.put("misc", jsonMiscSettingsObject);
        } catch (JSONException e) {
            System.out.println("server settings couldn't be saved\n" + e.getMessage());
        }
        // save settings
        Editor editor = this.settings.edit();
        editor.putString("ApplicationSettings", jsonApplicationSettingsObject.toString());
        editor.commit();
    }


    /**
     * poi preset part
     */

    public int getPresetIdInRouterFragment() {
        return this.presetIdInRouterFragment;
    }

    public void setPresetIdInRouterFragment(int pos) {
        this.presetIdInRouterFragment = pos;
        storeApplicationSettings();
    }

    public int getPresetIdInPoiFragment() {
        return this.presetIdInPoiFragment;
    }

    public void setPresetIdInPoiFragment(int pos) {
        this.presetIdInPoiFragment = pos;
        storeApplicationSettings();
    }

    public ArrayList<POIPreset> getPOIPresetList() {
        return this.poiPresets;
    }

    public ArrayList<POIPreset> getPOIPresetListWithEmptyPreset() {
        ArrayList<POIPreset> listWithEmptyPreset = new ArrayList<POIPreset>(this.poiPresets);
        listWithEmptyPreset.add(0, new POIPreset(
                0, mContext.getResources().getString(R.string.labelPOICategoryDisabled), 0, "") );
        return listWithEmptyPreset;
    }

    public POIPreset getPOIPreset(int id) {
        for (POIPreset preset : this.poiPresets) {
            if (preset.getId() == id)
                return preset;
        }
        return null;
    }

    public int createPOIPreset(String name) {
        int nextPresetId = 0;
        for (POIPreset preset : getPOIPresetList()) {
            if (preset.getId() > nextPresetId) {
                nextPresetId = preset.getId();
            }
        }
        POIPreset newPreset = new POIPreset(nextPresetId+1, name, 250, "");
        this.poiPresets.add( newPreset );
        Collections.sort(this.poiPresets);
        storeApplicationSettings();
        return newPreset.getId();
    }

    public void updatePOIPreset(POIPreset preset) {
        this.poiPresets.remove(preset);
        this.poiPresets.add( preset );
        Collections.sort(this.poiPresets);
        storePOIPresets();
    }

    public void removePOIPreset(POIPreset preset) {
        this.poiPresets.remove(preset);
        Collections.sort(this.poiPresets);
        storePOIPresets();
    }

    private void loadPOIPresets() {
        this.poiPresets = new ArrayList<POIPreset>();
        String poiPresetsString = settings.getString("POIPresets", "[]");
        JSONArray jsonPresetArray = new JSONArray();
        try {
            jsonPresetArray = new JSONArray(poiPresetsString);
        } catch (JSONException e) {
            System.out.println(e.getMessage());
            return;
        }
        for (int i=0; i<jsonPresetArray.length(); i++) {
            try {
                JSONObject jsonPreset = jsonPresetArray.getJSONObject(i);
                POIPreset preset = new POIPreset(
                        jsonPreset.getInt("id"),
                        jsonPreset.getString("name"),
                        jsonPreset.getInt("range"),
                        jsonPreset.getString("tags") );
                poiPresets.add(preset);
            } catch (JSONException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private void storePOIPresets() {
        JSONArray jsonPresetArray = new JSONArray();
        for (POIPreset preset : this.poiPresets) {
            try {
                JSONObject jsonPreset = new JSONObject();
                jsonPreset.put( "id", preset.getId() );
                jsonPreset.put( "name", preset.getName() );
                jsonPreset.put( "range", preset.getRange() );
                jsonPreset.put( "tags", preset.getTags() );
                jsonPresetArray.put(jsonPreset);
            } catch (JSONException e) {
                System.out.println(e.getMessage());
            }
        }
        Editor editor = this.settings.edit();
        editor.putString("POIPresets", jsonPresetArray.toString());
        editor.commit();
    }

    private void createDefaultPresets() {
        POIPreset favorites = new POIPreset(
                1, mContext.getResources().getString(R.string.labelPOICategoryFavorites), 1000, "favorites");
        this.poiPresets.add( favorites );
        POIPreset intersections = new POIPreset(
                2, mContext.getResources().getString(R.string.labelPOICategoryNamedIntersection), 250, "named_intersection");
        this.poiPresets.add( intersections );
        POIPreset transport = new POIPreset(
                3, mContext.getResources().getString(R.string.labelPOICategoryTransport), 1000, "transport");
        this.poiPresets.add( transport );
        POIPreset food = new POIPreset(
                4, mContext.getResources().getString(R.string.labelPOICategoryFood), 500, "food");
        this.poiPresets.add( food );
        POIPreset shops = new POIPreset(
                5, mContext.getResources().getString(R.string.labelPOICategoryShop), 500, "shop");
        this.poiPresets.add( shops );
        setPresetIdInPoiFragment(2);
        setPresetIdInRouterFragment(2);
        storePOIPresets();
        storeApplicationSettings();
    }

    /**
     * load and store last location
     */
    public Location loadLastLocation() {
        String lastLocationString = settings.getString("lastLocation", "{}");
        JSONObject jsonLocation = new JSONObject();
        try {
            jsonLocation = new JSONObject(lastLocationString);
        } catch (JSONException e) {
            System.out.println(e.getMessage());
            return null;
        }
        Location location = null;
        try {
            location = new Location( jsonLocation.getString("provider") );
            location.setLatitude( jsonLocation.getDouble("lat") );
            location.setLongitude( jsonLocation.getDouble("lon") );
            try {
                location.setAccuracy( (float) jsonLocation.getDouble("accuracy") );
            } catch (JSONException e) {}
            try {
                location.setBearing( (float) jsonLocation.getDouble("bearing") );
            } catch (JSONException e) {}
            try {
                location.setAltitude( jsonLocation.getDouble("altitude") );
            } catch (JSONException e) {}
            try {
                location.setTime( jsonLocation.getLong("time") );
            } catch (JSONException e) {}
        } catch (JSONException e) {
            return null;
        }
        return location;
    }

    public void storeLastLocation(Location location) {
        JSONObject jsonLocation = new JSONObject();
        try {
            jsonLocation.put("lat", location.getLatitude());
            jsonLocation.put("lon", location.getLongitude());
            jsonLocation.put("provider", location.getProvider());
        } catch (JSONException e) {
            return;
        }
        try {
            jsonLocation.put("time", location.getTime());
            jsonLocation.put("accuracy", location.getAccuracy());
            jsonLocation.put("altitude", location.getAltitude());
            jsonLocation.put("bearing", location.getBearing());
        } catch (JSONException e) {}
        Editor editor = this.settings.edit();
        editor.putString("lastLocation", jsonLocation.toString());
        editor.commit();
    }

    /**
     * point history
     */

    // source route history
    public ArrayList<Route> loadSourceRoutesFromHistory() {
        System.out.println("xx load sourceroutelist size = " + loadSourceRoutes().size());
        return loadSourceRoutes();
    }

    public void addSourceRouteToHistory(Route sourceRoute) {
        ArrayList<Route> sourceRouteList = loadSourceRoutes();
        if (sourceRouteList.contains(sourceRoute)) {
            sourceRoute = sourceRouteList.get( sourceRouteList.indexOf(sourceRoute) );
            sourceRouteList.remove(sourceRoute);
        }
        sourceRouteList.add(0, sourceRoute);
        storeSourceRoutes(sourceRouteList);
    }

    public void removeSourceRouteFromHistory(Route sourceRoute) {
        ArrayList<Route> sourceRouteList = loadSourceRoutes();
        if (sourceRouteList.contains(sourceRoute)) {
            sourceRouteList.remove(sourceRoute);
            storeSourceRoutes(sourceRouteList);
        }
    }

    public void clearSourceRouteHistory() {
        storeSourceRoutes(new ArrayList<Route>());
    }

    private ArrayList<Route> loadSourceRoutes() {
        ArrayList<Route> sourceRouteList = new ArrayList<Route>();
        try {
        	JSONArray jsonSourceRouteList = new JSONArray( settings.getString("sourceRoutes", "[]"));
            for (int index=0; index<jsonSourceRouteList.length(); index++) {
                try {
                    sourceRouteList.add(
                            new Route(
                                ObjectParser.parseRouteArray( jsonSourceRouteList.getJSONArray(index)) ));
                } catch (RouteParsingException re) {
                    continue;
                }
            }
        } catch (JSONException e) {
            return sourceRouteList;
        }
        return sourceRouteList;
    }

    private void storeSourceRoutes(ArrayList<Route> sourceRouteList) {
        JSONArray jsonSourceRouteList = new JSONArray();
        for (Route s : sourceRouteList) {
            jsonSourceRouteList.put( s.toJson() );
        }
        Editor editor = this.settings.edit();
        editor.putString("sourceRoutes", jsonSourceRouteList.toString());
        editor.commit();
    }

    // blocked ways
    public boolean footwaySegmentBlocked(FootwaySegment footway) {
        ArrayList<FootwaySegment> blockedWays = loadBlockedWays();
        if (blockedWays.contains(footway))
            return true;
        return false;
    }

    public void blockFootwaySegment(FootwaySegment footway) {
        ArrayList<FootwaySegment> blockedWays = loadBlockedWays();
        if (blockedWays.contains(footway)) {
            blockedWays.remove(footway);
        }
        blockedWays.add(0, footway);
        storeBlockedWays(blockedWays);
    }

    public void unblockFootwaySegment(FootwaySegment footway) {
        ArrayList<FootwaySegment> blockedWays = loadBlockedWays();
        if (blockedWays.contains(footway)) {
            blockedWays.remove(footway);
            storeBlockedWays(blockedWays);
        }
    }

    public void clearBlockedWays() {
        storeBlockedWays(new ArrayList<FootwaySegment>());
    }

    public ArrayList<FootwaySegment> loadBlockedWays() {
        ArrayList<FootwaySegment> blockedWays = new ArrayList<FootwaySegment>();
        try {
        	JSONArray jsonBlockedWays = new JSONArray( settings.getString("blockedWays", "[]"));
            for (int index=0; index<jsonBlockedWays.length(); index++) {
                FootwaySegment footway = ObjectParser.parseFootway( jsonBlockedWays.getJSONObject(index));
                if (footway != null)
                    blockedWays.add(footway);
            }
        } catch (JSONException e) {
            return blockedWays;
        }
        return blockedWays;
    }

    private void storeBlockedWays(ArrayList<FootwaySegment> blockedWays) {
        JSONArray jsonBlockedWays = new JSONArray();
        for (FootwaySegment f : blockedWays) {
            jsonBlockedWays.put( f.toJson() );
        }
        Editor editor = this.settings.edit();
        editor.putString("blockedWays", jsonBlockedWays.toString());
        editor.commit();
    }

    // point history
    public ArrayList<Point> loadPointsFromHistory() {
        return loadPointList("pointsFromHistory");
    }

    public void addPointToHistory(Point point) {
        ArrayList<Point> pointsFromHistory = this.loadPointsFromHistory();
        System.out.println("xx add pointlist size = " + pointsFromHistory.size());
        if (pointsFromHistory.contains(point)) {
            System.out.println("xx add contains point");
            pointsFromHistory.remove(point);
        }
        pointsFromHistory.add(0, point);
        System.out.println("xx added pointlist size = " + pointsFromHistory.size());
        storePointList(pointsFromHistory, "pointsFromHistory");
    }

    public void addPointToHistory(RouteObjectWrapper routeObject) {
        if (routeObject.getWayPoint() != null) {
            addPointToHistory(routeObject.getWayPoint());
        } else if (routeObject.getIntersection() != null) {
            addPointToHistory(routeObject.getIntersection());
        } else if (routeObject.getPOI() != null) {
            addPointToHistory(routeObject.getPOI());
        } else if (routeObject.getStation() != null) {
            addPointToHistory(routeObject.getStation());
        }
    }

    public void removePointFromHistory(Point point) {
        ArrayList<Point> pointsFromHistory = this.loadPointsFromHistory();
        if (pointsFromHistory.contains(point)) {
            pointsFromHistory.remove(point);
            storePointList(pointsFromHistory, "pointsFromHistory");
        }
    }

    public void removePointFromHistory(RouteObjectWrapper routeObject) {
        if (routeObject.getWayPoint() != null) {
            removePointFromHistory(routeObject.getWayPoint());
        } else if (routeObject.getIntersection() != null) {
            removePointFromHistory(routeObject.getIntersection());
        } else if (routeObject.getPOI() != null) {
            removePointFromHistory(routeObject.getPOI());
        } else if (routeObject.getStation() != null) {
            removePointFromHistory(routeObject.getStation());
        }
    }

    public void clearPointHistory() {
        storePointList(new ArrayList<Point>(), "pointsFromHistory");
    }

    // favorites
    public ArrayList<Point> loadPointsFromFavorites() {
        return loadPointList("pointsFromFavorites");
    }

    public void addPointToFavorites(Point point) {
        ArrayList<Point> pointsFromFavorites = this.loadPointsFromFavorites();
        if (pointsFromFavorites.contains(point)) {
            pointsFromFavorites.remove(point);
        }
        pointsFromFavorites.add(0, point);
        storePointList(pointsFromFavorites, "pointsFromFavorites");
    }

    public void addPointToFavorites(RouteObjectWrapper routeObject) {
        if (routeObject.getWayPoint() != null) {
            addPointToFavorites(routeObject.getWayPoint());
        } else if (routeObject.getIntersection() != null) {
            addPointToFavorites(routeObject.getIntersection());
        } else if (routeObject.getPOI() != null) {
            addPointToFavorites(routeObject.getPOI());
        } else if (routeObject.getStation() != null) {
            addPointToFavorites(routeObject.getStation());
        }
    }

    public boolean favoritesContains(Point point) {
        ArrayList<Point> pointsFromFavorites = this.loadPointsFromFavorites();
        if (pointsFromFavorites.contains(point))
            return true;
        return false;
    }

    public boolean favoritesContains(RouteObjectWrapper routeObject) {
        if (routeObject.getWayPoint() != null) {
            return favoritesContains(routeObject.getWayPoint());
        } else if (routeObject.getIntersection() != null) {
            return favoritesContains(routeObject.getIntersection());
        } else if (routeObject.getPOI() != null) {
            return favoritesContains(routeObject.getPOI());
        } else if (routeObject.getStation() != null) {
            return favoritesContains(routeObject.getStation());
        }
        return false;
    }

    public void removePointFromFavorites(Point point) {
        ArrayList<Point> pointsFromFavorites = this.loadPointsFromFavorites();
        if (pointsFromFavorites.contains(point)) {
            pointsFromFavorites.remove(point);
            storePointList(pointsFromFavorites, "pointsFromFavorites");
        }
    }

    public void removePointFromFavorites(RouteObjectWrapper routeObject) {
        if (routeObject.getWayPoint() != null) {
            removePointFromFavorites(routeObject.getWayPoint());
        } else if (routeObject.getIntersection() != null) {
            removePointFromFavorites(routeObject.getIntersection());
        } else if (routeObject.getPOI() != null) {
            removePointFromFavorites(routeObject.getPOI());
        } else if (routeObject.getStation() != null) {
            removePointFromFavorites(routeObject.getStation());
        }
    }

    public void clearFavoritePointHistory() {
        storePointList(new ArrayList<Point>(), "pointsFromFavorites");
    }

    private ArrayList<Point> loadPointList(String listName) {
        ArrayList<Point> pointList = new ArrayList<Point>();
        JSONArray jsonPointArray = new JSONArray();
        try {
            jsonPointArray = new JSONArray( settings.getString(listName, "[]") );
            System.out.println("xx privload jsonarray size = " + jsonPointArray.length());
            pointList = ObjectParser.parsePointArray(jsonPointArray);
            System.out.println("xx privload pointlist size = " + pointList.size());
        } catch (JSONException e) {
            return pointList;
        }
        return pointList;
    }

    private void storePointList(ArrayList<Point> pointList, String listName) {
        JSONArray jsonPointArray = new JSONArray();
        for (Point p : pointList) {
            jsonPointArray.put( p.toJson() );
        }
        Editor editor = this.settings.edit();
        editor.putString(listName, jsonPointArray.toString());
        editor.commit();
    }

    /**
     * save and restore application settings
     * for example to transfer them to a new device
     */

    public HashMap<String,Integer> storeAllSettingsToDisk() {
        HashMap<String,Integer> results = new HashMap<String,Integer>();
        String fileName;
        // application settings
        storeApplicationSettings();
        fileName = "ApplicationSettings";
        results.put(fileName,
                storeSingleSettingsFile(getProgramSettingsFolder() + "/" + fileName));
        // poi presets
        storePOIPresets();
        fileName = "POIPresets";
        results.put(fileName,
                storeSingleSettingsFile(getProgramSettingsFolder() + "/" + fileName));
        // favorites
        fileName = "pointsFromFavorites";
        results.put(fileName,
                storeSingleSettingsFile(getProgramSettingsFolder() + "/" + fileName));
        // points from history
        fileName = "pointsFromHistory";
        results.put(fileName,
                storeSingleSettingsFile(getProgramSettingsFolder() + "/" + fileName));
        // routes from history
        fileName = "sourceRoutes";
        results.put(fileName,
                storeSingleSettingsFile(getProgramSettingsFolder() + "/" + fileName));
        return results;
    }

    private Integer storeSingleSettingsFile(String fileName) {
        String[] fileNameParts = fileName.split("/");
        try {
            Writer fw = new FileWriter(fileName, false);
            fw.write(settings.getString(fileNameParts[fileNameParts.length-1], "[]"));
            fw.close();
        } catch (IOException e) {
            return 1;
        }
        return 0;
    }

    public HashMap<String,Integer> restoreAllSettingsFromDisk() {
        HashMap<String,Integer> results = new HashMap<String,Integer>();
        String fileName, content;
        // application settings
        fileName = "ApplicationSettings";
        results.put(fileName,
                restoreSingleSettingsFile(getProgramSettingsFolder() + "/" + fileName));
        // poi presets
        fileName = "POIPresets";
        results.put(fileName,
                restoreSingleSettingsFile(getProgramSettingsFolder() + "/" + fileName));
        // favorites
        fileName = "pointsFromFavorites";
        results.put(fileName,
                restoreSingleSettingsFile(getProgramSettingsFolder() + "/" + fileName));
        // points from history
        fileName = "pointsFromHistory";
        results.put(fileName,
                restoreSingleSettingsFile(getProgramSettingsFolder() + "/" + fileName));
        // routes from history
        fileName = "sourceRoutes";
        results.put(fileName,
                restoreSingleSettingsFile(getProgramSettingsFolder() + "/" + fileName));
        return results;
    }

    private Integer restoreSingleSettingsFile(String fileName) {
        String settings = "";
        // 1.: check if file is available
        File file = new File(fileName);
        if (!file.exists()) {
            return 1;
        }
        // 2.: read file contents and write it into the "settings" string variable
        try {
            StringBuffer datax = new StringBuffer("");
            FileInputStream fIn = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader ( fIn );
            BufferedReader buffreader = new BufferedReader ( isr );
            String readString = buffreader.readLine ( );
            while ( readString != null ) {
                datax.append(readString);
                readString = buffreader.readLine ( );
            }
            isr.close ( );
            settings = datax.toString();
        } catch (IOException e) {
            return 2;
        }
        // 3.: check if the read string is in the json format
        if (! HelperFunctions.isJSONValid(settings))
            return 3;
        // 4.: import read settings into internal phone storage
        String[] fileNameParts = fileName.split("/");
        Editor editor = this.settings.edit();
        editor.putString(fileNameParts[fileNameParts.length-1], settings);
        editor.commit();
        // 5. delete file
        file.delete();
        return 0;
    }

}
