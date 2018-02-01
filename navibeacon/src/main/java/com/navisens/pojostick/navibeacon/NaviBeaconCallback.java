package com.navisens.pojostick.navibeacon;

import org.altbeacon.beacon.Beacon;

/**
 * Created by Joseph Chen on 1/31/18.
 * <p>
 *     Listener called by service whenever beacon is ranged
 * </p>
 */

public interface NaviBeaconCallback {
    public void onBeaconResponded(Beacon beacon, Double latitude, Double longitude, Double heading);
}
