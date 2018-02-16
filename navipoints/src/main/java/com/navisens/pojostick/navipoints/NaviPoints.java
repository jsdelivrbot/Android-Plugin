package com.navisens.pojostick.navipoints;

import android.support.annotation.NonNull;

import com.navisens.motiondnaapi.MotionDna;
import com.navisens.pojostick.navisenscore.NavisensCore;
import com.navisens.pojostick.navisenscore.NavisensPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by Joseph Chen on 2/14/18.
 * <p>
 *     System for keeping track of points of interest
 * </p>
 */
public class NaviPoints implements NavisensPlugin {
    private static final String PLUGIN_IDENTIFIER = "com.navisens.pojostick.navipoints",
                                MAPS_IDENTIFIER = "com.navisens.pojostick.navisensmaps";
    private static final int OPERATION_ADD = 1,
                             OPERATION_REMOVE = 2;

    private final Map<String, NaviPointCoord> locations;
    private final SortedMap<NaviPointCoord, String> search;

    private NavisensCore core;
    private double lastHeading;
    private boolean mapsExists = false;

    public NaviPoints() {
        locations = new HashMap<>();
        search = new TreeMap<>();
    }

    /**
     * Keep track of a named point
     * @param id a unique identifier
     * @param latitude the latitude of this point
     * @param longitude the longitude of this point
     * @param heading an optional heading - if the heading is null, then the heading will remain
     *                unchanged when setting the user's location with the setLocation method
     */
    public void add(String id, double latitude, double longitude, Double heading, Integer floor) {
        NaviPointCoord coord = new NaviPointCoord(latitude, longitude, heading, floor);
        locations.put(id, coord);
        search.put(coord, id);
        if (mapsExists) {
            sendPoint(OPERATION_ADD, id, latitude, longitude);
        }
    }

    /**
     * Remove a named point to stop tracking it
     * @param id The unique id
     */
    public void remove(String id) {
        if (locations.containsKey(id)) {
            NaviPointCoord coord = locations.remove(id);
            if (mapsExists) {
                sendPoint(OPERATION_REMOVE, id, coord.latitude, coord.longitude);
            }
            search.remove(coord);
        }
    }

    /**
     * Find all points within the specified boundaries. Note that if getting the boundaries bounding
     * a circle, for example using the geodesic distance as the radius, the boundaries are not
     * rectancles, and should be computed correctly to get all requested points.
     * @param fromLatitude latitude of from coordinate
     * @param fromLongitude longitude of from coordinate
     * @param toLatitude latitude of to coordinate
     * @param toLongitude longitude of to coordinate
     * @return a map between the names and locations of each point. You can access location of a
     * NaviPointCoord by accessing the properties latitude and longitude
     */
    public Map<String, NaviPointCoord> pointsInBoundary(double fromLatitude, double fromLongitude,
                                                        double toLatitude, double toLongitude) {
        NaviPointCoord from = new NaviPointCoord(fromLatitude, fromLongitude, null, null),
                to = new NaviPointCoord(toLatitude, toLongitude, null, null);
        if (from.compareTo(to) > 0) {
            NaviPointCoord temp = from;
            from = to;
            to = temp;
        }
        Map<NaviPointCoord, String> points = search.subMap(from, to);
        Map<String, NaviPointCoord> places = new HashMap<>();
        for (Map.Entry<NaviPointCoord, String> point : points.entrySet()) {
            places.put(point.getValue(), point.getKey());
        }
        return places;
    }

    /**
     * Set the user location to the location mapped with a certain id, if it exists
     * @param id The name of the location that was added earlier
     */
    public void setLocation(String id) {
        if (locations.containsKey(id)) {
            NaviPointCoord coord = locations.get(id);
            core.getMotionDna().setLocationLatitudeLongitudeAndHeadingInDegrees(coord.latitude, coord.longitude, lastHeading);
            if (coord.heading != null) {
                core.getMotionDna().setHeadingInDegrees(coord.heading);
            }
            if (coord.floor != null) {
                core.getMotionDna().setFloorNumber(coord.floor);
            }
            core.getSettings().overrideEstimationMode(MotionDna.EstimationMode.LOCAL);
        }
    }

    private void sendPoint(int operation, String id, double lat, double lng) {
        if (core != null) {
            core.broadcast(PLUGIN_IDENTIFIER, operation, id, lat, lng);
        }
    }

    @Override
    public boolean init(NavisensCore navisensCore, Object[] objects) {
        this.core = navisensCore;

        core.subscribe(this, NavisensCore.MOTION_DNA | NavisensCore.PLUGIN_DATA);
        core.broadcast(PLUGIN_IDENTIFIER, NavisensCore.OPERATION_INIT);

        return true;
    }

    @Override
    public boolean stop() {
        core.remove(this);
        core.broadcast(PLUGIN_IDENTIFIER, NavisensCore.OPERATION_STOP);
        return true;
    }

    @Override
    public void receiveMotionDna(MotionDna motionDna) {
        MotionDna.Location location = motionDna.getLocation();
        if (location != null) {
            lastHeading = location.heading;
        }
    }

    @Override
    public void receiveNetworkData(MotionDna motionDna) {
    }

    @Override
    public void receiveNetworkData(MotionDna.NetworkCode networkCode, Map<String, ?> map) {
    }

    @Override
    public void receivePluginData(String id, int operation, Object... payload) {
        switch (id) {
            case MAPS_IDENTIFIER:
                mapsExists = true;
                if (operation == NavisensCore.OPERATION_INIT ||
                        operation == NavisensCore.OPERATION_ACK && payload.length == 1 && PLUGIN_IDENTIFIER.equals(payload[0])) {
                    for (Map.Entry<String, NaviPointCoord> coord : locations.entrySet()) {
                        sendPoint(OPERATION_ADD, coord.getKey(), coord.getValue().latitude, coord.getValue().longitude);
                    }
                } else if (operation == NavisensCore.OPERATION_STOP) {
                    mapsExists = false;
                }
        }
    }

    @Override
    public void reportError(MotionDna.ErrorCode errorCode, String s) {
    }

    public class NaviPointCoord implements Comparable<NaviPointCoord> {
        public double latitude, longitude;
        public Double heading;
        public Integer floor;

        public NaviPointCoord(double lat, double lng, Double heading, Integer floor) {
            latitude = lat;
            longitude = lng;
            this.heading = heading;
            this.floor = floor;
        }

        @Override
        public int compareTo(@NonNull NaviPointCoord other) {
            double deltaLat = latitude - other.latitude;
            if (deltaLat < 0) return -1;
            if (deltaLat > 0) return 1;
            double deltaLng = longitude - other.longitude;
            if (deltaLng < 0) return -1;
            if (deltaLng > 0) return 1;
            return 0;
        }
    }
}
