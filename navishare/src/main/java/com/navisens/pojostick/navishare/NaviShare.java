package com.navisens.pojostick.navishare;

import com.navisens.motiondnaapi.MotionDna;
import com.navisens.pojostick.navisenscore.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Joseph on 11/30/17.
 * <p>
 *     NaviShare provides functionality for communicating with other devices
 * </p>
 */
@SuppressWarnings("unused")
public class NaviShare implements NavisensPlugin {
    private static final long QUERY_INTERVAL = 500000000;

    private String host, port, room;

    private boolean changed;
    private boolean configured;
    private boolean connected;
    private NavisensCore core;

    private final Set<String> rooms;
    private final Set<NaviShareListener> listeners;

    private long roomsQueriedAt;

    @SuppressWarnings("unused")
    public NaviShare() {
        this.rooms = new HashSet<>();
        this.listeners = new HashSet<>();
        this.roomsQueriedAt = System.nanoTime();
    }

    /**
     * Configure a server. This does not connect to it yet, but is required before calling connect.
     *
     * @param host server ip
     * @param port server port
     * @return a reference to this NaviShare
     */
    @SuppressWarnings("unused")
    public NaviShare configure(String host, String port) {
        core.getSettings().overrideHost(null, null);

        this.host = host;
        this.port = port;
        this.changed = true;
        this.configured = true;

        return this;
    }

    /**
     * Connect to a room in the server.
     *
     * @param room The room to connect to
     * @return Whether this device connected to a server
     */
    @SuppressWarnings("unused")
    public boolean connect(String room) {
        core.getSettings().overrideRoom(null);

        if (configured) {
            if (connected && !changed) {
                core.getMotionDna().setUDPRoom(room);
            } else {
                this.disconnect();
                core.getMotionDna().startUDP(room, host, port);
                this.connected = true;
            }
            this.changed = false;
            return true;
        }
        return false;
    }

    /**
     * Disconnect from the server.
     */
    @SuppressWarnings("unused")
    public void disconnect() {
        core.getMotionDna().stopUDP();
        this.connected = false;
    }

    /**
     * Connect to a public test server. Don't use this for official release, as it is public
     * and can get filled up quickly. Each organization can claim one room, so if you need
     * multiple independent servers within your organization, you should test on a private
     * server instead, and use configure(host, port) to target that server instead.
     */
    @SuppressWarnings("unused")
    public boolean testConnect() {
        if (!connected) {
            this.disconnect();
            core.getMotionDna().startUDP();
            return this.connected = true;
        }
        return false;
    }

    /**
     * Send a message to all other devices on the network
     *
     * @param msg A string to send, recommended web safe
     */
    @SuppressWarnings("unused")
    public void sendMessage(String msg) {
        if (connected) {
            core.getMotionDna().sendUDPPacket(msg);
        }
    }

    /**
     * Add a listener to receive network events
     *
     * @param listener The listener to receive events
     * @return Whether the listener was added successfully
     */
    @SuppressWarnings("unused")
    public boolean addListener(NaviShareListener listener) {
        return this.listeners.add(listener);
    }

    /**
     * Remove a listener to stop receiving events
     *
     * @param listener Ths listener to remove
     * @return Whether the listener was removed successfully
     */
    @SuppressWarnings("unused")
    public boolean removeListener(NaviShareListener listener) {
        return this.listeners.remove(listener);
    }

    /**
     * Track a room so you can query its status. Call refreshRoomStatus to
     * receive a new roomStatus event
     *
     * @param room A room to track
     * @return Whether the room was added to be tracked or not
     */
    @SuppressWarnings("unused")
    public boolean trackRoom(String room) {
        return this.rooms.add(room);
    }

    /**
     * Stop tracking a room. Call refreshRoomStatus to receive a new roomStatusEvent
     *
     * @param room A room to stop tracking
     * @return Whether the room was removed from tracking or not
     */
    @SuppressWarnings("unused")
    public boolean untrackRoom(String room) {
        return this.rooms.remove(room);
    }

    /**
     * Refresh the status of any tracked rooms. Updates will be received from the
     * roomOccupancyChanged event
     * @return Whether a refresh request was sent or not
     */
    @SuppressWarnings("unused")
    public boolean refreshRoomStatus() {
        final long now = System.nanoTime();
        if (connected && now - roomsQueriedAt > QUERY_INTERVAL) {
            roomsQueriedAt = now;
            core.getMotionDna().sendUDPQueryRooms(rooms.toArray(new String[0]));
            return true;
        }
        return false;
    }

    @Override
    public boolean init(NavisensCore navisensCore, Object[] objects) {
        this.core = navisensCore;

        core.subscribe(this, NavisensCore.NETWORK_DATA);

        core.getSettings().requestNetworkRate(100);
        core.applySettings();

        return true;
    }

    @Override
    public boolean stop() {
        this.disconnect();
        core.remove(this);
        return true;
    }

    @Override
    public void receiveMotionDna(MotionDna motionDna) {
    }

    @Override
    public void receiveNetworkData(MotionDna motionDna) {
    }

    @Override
    public void receiveNetworkData(MotionDna.NetworkCode networkCode, Map<String, ?> map) {
        switch (networkCode) {
            case RAW_NETWORK_DATA:
                for (NaviShareListener listener : listeners)
                    listener.messageReceived((String) map.get("ID"), (String) map.get("payload"));
                break;
            case ROOM_CAPACITY_STATUS:
                for (NaviShareListener listener : listeners)
                    listener.roomOccupancyChanged((Map<String, Integer>) map.get("payload"));
                break;
            case EXCEEDED_SERVER_ROOM_CAPACITY:
                for (NaviShareListener listener : listeners)
                    listener.serverCapacityExceeded();
                this.disconnect();
                break;
            case EXCEEDED_ROOM_CONNECTION_CAPACITY:
                for (NaviShareListener listener : listeners)
                    listener.roomCapacityExceeded();
                this.disconnect();
                break;
        }
    }

    @Override
    public void receivePluginData(String id, int operator, Object... payload) {
    }

    @Override
    public void reportError(MotionDna.ErrorCode errorCode, String s) {
    }
}
