package com.navisens.pojostick.navipoints;

import com.navisens.motiondnaapi.MotionDna;
import com.navisens.pojostick.navisenscore.NavisensCore;
import com.navisens.pojostick.navisenscore.NavisensPlugin;

import java.util.HashMap;
import java.util.Map;

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

    private NavisensCore core;
    private double lastHeading;
    private boolean mapsExists = false;

    public NaviPoints() {
        locations = new HashMap<>();
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
        }
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

    public class NaviPointCoord {
        public double latitude, longitude;
        public Double heading;
        public Integer floor;

        public NaviPointCoord(double lat, double lng, Double heading, Integer floor) {
            latitude = lat;
            longitude = lng;
            this.heading = heading;
            this.floor = floor;
        }
    }
}
