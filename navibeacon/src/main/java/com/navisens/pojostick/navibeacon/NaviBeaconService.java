package com.navisens.pojostick.navibeacon;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Joseph Chen on 1/17/18.
 *
 * A background service which ranges for beacons.
 */
@SuppressWarnings("unused")
public class NaviBeaconService extends Service implements BeaconConsumer, RangeNotifier {
    private final IBinder binder = new NaviBeaconBinder();

    private BeaconManager beaconManager;

    private Map<Identifier, NaviBeacon.NaviBeaconData> beacons = new HashMap<>();
    private Region region = new Region("regions0", null, null, null);

    private NaviBeaconCallback callback;

    public void addBeacon(NaviBeacon.NaviBeaconData naviBeaconData) {
        beacons.put(naviBeaconData.id, naviBeaconData);
    }

    public void setPeriod(long period, long between) {
        try {
            beaconManager.setForegroundScanPeriod(period);
            beaconManager.setForegroundBetweenScanPeriod(between);
            beaconManager.updateScanPeriods();
        }
        catch (Exception e) {
        }
    }

    public void setCallback(NaviBeaconCallback naviBeaconCallback) {
        callback = naviBeaconCallback;
    }

    private void bind() {
        if (beaconManager == null) {
            beaconManager = BeaconManager.getInstanceForApplication(getApplicationContext());
            BeaconManager.setAndroidLScanningDisabled(true);

            beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
            beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19"));// support eddystone bluetooth UID Frame
            beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("x,s:0-1=feaa,m:2-2=20,d:3-3,d:4-5,d:6-7,d:8-11,d:12-15"));// support eddystone bluetooth telemetry Frame
            beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("s:0-1=feaa,m:2-2=10,p:3-3:-41,i:4-20v"));

            beaconManager.bind(this);
        }
    }

    public void start() {
        bind();
//        System.out.println("Attempting to begin finding beacons.");
        try {
            beaconManager.startRangingBeaconsInRegion(region);
            beaconManager.startMonitoringBeaconsInRegion(region);
//            System.out.println("Finding beacons...");
        } catch (Exception e) {
        }
    }

    public void stop() {
//        System.out.println("Attempting to stop finding beacons.");
        bind();
        try {
            beaconManager.stopRangingBeaconsInRegion(region);
            beaconManager.stopMonitoringBeaconsInRegion(region);
//            System.out.println("Stopped finding beacons.");
        } catch (Exception e) {
        }
    }

    @Override
    public void onStart(Intent intent, int startId) {
//        System.out.println("onStart");
        bind();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (beaconManager != null) beaconManager.unbind(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> rBeacons, Region region) {
                for (Beacon rBeacon : rBeacons) {
                    if (beacons.containsKey(rBeacon.getId1()) && callback != null) {
                        NaviBeacon.NaviBeaconData data = beacons.get(rBeacon.getId1());
                        callback.onBeaconResponded(rBeacon, data.latitude, data.longitude, data.heading, data.floor);
                    }
                }
            }
        });

        beaconManager.addMonitorNotifier(new MonitorNotifier() {
            @Override
            public void didEnterRegion(Region region) {
//                System.out.println("I just saw an beacon for the first time!");
            }

            @Override
            public void didExitRegion(Region region) {
//                System.out.println("I no longer see an beacon");
            }

            @Override
            public void didDetermineStateForRegion(int state, Region region) {
//                System.out.println("I have just switched from seeing/not seeing beacons: " + state);
            }
        });
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> collection, Region region) {
//        System.out.println("Did range beacons in region");
    }

    public class NaviBeaconBinder extends Binder {
        public NaviBeaconService getService() {
            return NaviBeaconService.this;
        }
    }
}
