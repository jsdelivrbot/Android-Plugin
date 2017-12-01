package com.navisens.pojostick.navishare;

import java.util.Map;

/**
 * Created by Joseph on 12/1/17.
 *
 * This defines the listener interface to collect events for NaviShare
 */
public interface NaviShareListener {
    void messageReceived(String deviceID, String message);
    void roomOccupancyChanged(Map<String, Integer> roomOccupancy);
    void serverCapacityExceeded();
    void roomCapacityExceeded();
}
