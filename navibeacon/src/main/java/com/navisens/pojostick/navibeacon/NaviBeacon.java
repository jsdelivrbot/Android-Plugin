package com.navisens.pojostick.navibeacon;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.navisens.motiondnaapi.MotionDna;
import com.navisens.pojostick.navisenscore.NavisensCore;
import com.navisens.pojostick.navisenscore.NavisensPlugin;
import com.navisens.pojostick.navibeacon.NaviBeaconService.NaviBeaconBinder;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.Identifier;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * Created by Joseph on 12/30/17.
 * <p>
 *     NaviBeacon provides functionality for setting location with beacons
 * </p>
 */
@SuppressWarnings("unused")
public class NaviBeacon implements NavisensPlugin {
    private static final String PLUGIN_IDENTIFIER = "com.navisens.pojostick.navibeacon",
                                MAPS_IDENTIFIER = "com.navisens.pojostick.navisensmaps";
    private static final int OPERATION_ADD = 1;

    private static final double THRESHOLD = 0.25;
    private static final double MARGIN = 0.25;

    private NavisensCore core;
    private NaviBeaconService naviBeaconService;
    private ServiceConnection serviceConnection;
    private boolean connected;

    private long period = 500;
    private long between = 500;
    private NaviBeaconCallback callback = new DefaultNaviBeaconCallback();
    private Queue<NaviBeaconData> beacons = new LinkedList<>();

    private double lastHeading;
    private boolean mapsExists = false;

    public NaviBeacon() {
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
//                System.out.println("Service connected");
                NaviBeaconBinder binder = (NaviBeaconBinder) service;
                naviBeaconService = binder.getService();
                naviBeaconService.start();
                naviBeaconService.setPeriod(period, between);
                naviBeaconService.setCallback(callback);
                while (!beacons.isEmpty()) {
                    naviBeaconService.addBeacon(beacons.remove());
                }
                connected = true;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
//                System.out.println("Service disconnected");
                naviBeaconService.stop();
                connected = false;
            }
        };
        connected = false;
    }

    /**
     * Add a beacon to track. If you do not provide the latitude and longitude, please implement
     * your own callback and add it with the {@link #setBeaconCallback} method.
     * @param id the identifier of the beacon
     * @param latitude (optional) the latitude of a user standing near the beacon
     * @param longitude (optional) the longitude of a user standing near the beacon
     * @param heading (optional) the heading of a user standing near the beacon
     * @param floor (optional) the floor number of a user standing near the beacon
     * @return A reference to this object
     */
    public NaviBeacon addBeacon(Identifier id, Double latitude, Double longitude, Double heading, Integer floor) {
        beacons.add(new NaviBeaconData(id, latitude, longitude, heading, floor));
        if (mapsExists && latitude != null && longitude != null) {
            sendBeacon(latitude, longitude);
        }
        return this;
    }

    /**
     * Set the scanning period and delay. Scanning period is how long to scan for bluetooth beacons,
     * while the delay is how long to wait between each scanning session (saves battery). If you set
     * a scanning period that is too small or a delay that is too long, you may miss beacons.
     *
     * @param p the scanning period
     * @param b the delay period
     * @return A reference to this object
     */
    public NaviBeacon setScanningPeriod(long p, long b) {
        period = p;
        between = b;
        return this;
    }

    /**
     * Resume scanning for beacons if it was paused.
     */
    public void resumeScanning() {
        if (naviBeaconService != null) {
            naviBeaconService.start();
        }
    }

    /**
     * Pause scanning of beacons to save battery.
     */
    public void pauseScanning() {
        if (naviBeaconService != null) {
            naviBeaconService.stop();
        }
    }

    /**
     * Implement a custom callback to handle custom location initialization or action for each
     * beacon scanned. Make sure to add beacons with the {@link #addBeacon} method so they are
     * reported to your callback appopriately.
     * @param naviBeaconCallback A callback to handle whenever a beacon is ranged
     * @return A reference to this object
     */
    public NaviBeacon setBeaconCallback(NaviBeaconCallback naviBeaconCallback) {
        callback = naviBeaconCallback;
        return this;
    }

    private void sendBeacon(double lat, double lng) {
        if (core != null) {
            core.broadcast(PLUGIN_IDENTIFIER, OPERATION_ADD, lat, lng);
        }
    }

    @Override
    public boolean init(NavisensCore navisensCore, Object[] objects) {
        this.core = navisensCore;

        core.subscribe(this, NavisensCore.MOTION_DNA | NavisensCore.PLUGIN_DATA);
        core.broadcast(PLUGIN_IDENTIFIER, NavisensCore.OPERATION_INIT);

        Intent intent = new Intent(core.getMotionDna().getApplicationContext(), NaviBeaconService.class);
        core.getMotionDna().getApplicationContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

//        System.out.println("Initialized NaviBeacon");

        return true;
    }

    @Override
    public boolean stop() {
        core.remove(this);
        core.broadcast(PLUGIN_IDENTIFIER, NavisensCore.OPERATION_STOP);
        if (connected)
            naviBeaconService.stop();
        if (serviceConnection != null)
            core.getMotionDna().getApplicationContext().unbindService(serviceConnection);
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
                    if (naviBeaconService != null) {
                        for (NaviBeaconData data : naviBeaconService.beacons.values()) {
                            sendBeacon(data.latitude, data.longitude);
                        }
                    } else {
                        for (NaviBeaconData data : beacons) {
                            sendBeacon(data.latitude, data.longitude);
                        }
                    }
                } else if (operation == NavisensCore.OPERATION_STOP) {
                    mapsExists = false;
                }
        }
    }

    @Override
    public void reportError(MotionDna.ErrorCode errorCode, String s) {
    }

    public class DefaultNaviBeaconCallback implements NaviBeaconCallback {
        boolean resetRequired = true;

        @Override
        public void onBeaconResponded(Beacon beacon, Double latitude, Double longitude, Double heading, Integer floor) {
            if (beacon.getDistance() < THRESHOLD) {
                if (core != null && resetRequired) {
                    resetRequired = false;
                    if (latitude != null && longitude != null) {
                        core.getMotionDna().setLocationLatitudeLongitudeAndHeadingInDegrees(latitude, longitude, lastHeading);
                    }
                    if (heading != null) {
                        core.getMotionDna().setHeadingInDegrees(heading);
                    }
                    if (floor != null) {
                        core.getMotionDna().setFloorNumber(floor);
                    }
                    core.getSettings().overrideEstimationMode(MotionDna.EstimationMode.LOCAL);
                }
            } else if (beacon.getDistance() > THRESHOLD + MARGIN){
                resetRequired = true;
            }
        }
    }

    class NaviBeaconData {
        Identifier id;
        Double latitude;
        Double longitude;
        Double heading;
        Integer floor;

        NaviBeaconData(Identifier id, Double latitude, Double longitude, Double heading, Integer floor) {
            this.id = id;
            this.latitude = latitude;
            this.longitude = longitude;
            this.heading = heading;
            this.floor = floor;
        }
    }
}
