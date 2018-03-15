package com.navisens.pojostick.navigator;

import android.text.TextUtils;

import com.navisens.motiondnaapi.MotionDna;
import com.navisens.pojostick.navisenscore.NavisensCore;
import com.navisens.pojostick.navisenscore.NavisensPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Joseph Chen on 3/14/18.
 * <p>
 *     Plugin to generate simple paths.
 * </p>
 */

public class Navigator implements NavisensPlugin {
    private static final String PLUGIN_IDENTIFIER = "com.navisens.pojostick.navigator";
    private static final int OPERATION_ROUTE = 1;

    private static final double THRESHOLD = 0.25;
    private static final double MARGIN = 0.25;

    private NavisensCore core;
    private NavigableRoutes routes;

    private void addNode(NavigableNode node) {
    }

    private void connectNode(NavigableNode left, NavigableNode right) {

    }

    /**
     * Get a route of latitude longitudes.
     *
     * @param from The starting node
     * @param to The ending node
     * @return A list of nodes representing a polyline path
     */
    public List<NavigableNode> getRoute(NavigableNode from, NavigableNode to) {
        if (routes != null) {
            return routes.path(from, to);
        }

        List<NavigableNode> route = new ArrayList<>(3);

        double diffX = Math.abs(to.latitude - from.latitude);
        double diffY = Math.abs(to.longitude - from.longitude);

        route.add(from);
        if (diffX < diffY) {
            int signY = from.longitude < to.longitude ? 1 : -1;
            route.add(new NavigableNode(to.latitude, from.longitude + diffX * signY));
        } else {
            int signX = from.latitude < to.latitude ? 1 : -1;
            route.add(new NavigableNode(from.latitude + diffY * signX, to.longitude));
        }
        route.add(to);

        return route;
    }

    /**
     * Get a route and also optionally publish it. Otherwise behaves like getRoute
     *
     * @param from from node
     * @param to to node
     * @param publish if true, publish route to all other plugins too
     * @return a route
     */
    public List<NavigableNode> getRoute(NavigableNode from, NavigableNode to, boolean publish) {
        List<NavigableNode> route = getRoute(from, to);
        if (publish && core != null) {
            core.broadcast(PLUGIN_IDENTIFIER, OPERATION_ROUTE, "[" + TextUtils.join(",", route) + "]");
        }
        return route;
    }

    @Override
    public boolean init(NavisensCore navisensCore, Object[] objects) {
        this.core = navisensCore;
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

    }

    @Override
    public void receiveNetworkData(MotionDna motionDna) {

    }

    @Override
    public void receiveNetworkData(MotionDna.NetworkCode networkCode, Map<String, ? extends Object> map) {

    }

    @Override
    public void receivePluginData(String s, int i, Object... objects) {

    }

    @Override
    public void reportError(MotionDna.ErrorCode errorCode, String s) {

    }

    private static class NavigableRoutes {
        Map<NavigableNode, Map<NavigableNode, Double>> nodes = new HashMap<>();

        boolean add(NavigableNode node) {
            if (contains(node)) {
                nodes.put(node, new HashMap<NavigableNode, Double>());
                return true;
            }
            return false;
        }

        boolean connect(NavigableNode from, NavigableNode to) {
            if (contains(from) && contains(to)) {
                double distance = from.distanceTo(to);
                nodes.get(from).put(to, distance);
                nodes.get(to).put(from, distance);
                return true;
            }
            return false;
        }

        boolean contains(NavigableNode node) {
            return nodes.containsKey(node);
        }

        Set<NavigableNode> neighbors(NavigableNode node) {
            Set<NavigableNode> adjacency = new HashSet<>();
            if (contains(node)) {
                adjacency.addAll(nodes.keySet());
            }
            return adjacency;
        }

        List<NavigableNode> path(NavigableNode from, NavigableNode to) {
            // TODO: implement A* algorithm
            List<NavigableNode> route = new ArrayList<>();
            route.add(from);
            route.add(to);
            return route;
        }
    }

    public static class NavigableNode {
        public final double latitude, longitude;

        private final int hashValue;
        private final String stringValue;

        public NavigableNode(double lat, double lng) {
            latitude = lat;
            longitude = lng;
            hashValue = (Double.valueOf(lat).hashCode() << 16) | (Double.valueOf(lng).hashCode() & 0xffff);
            stringValue = "[" + Double.toString(lat) + "," + Double.toString(lng) + "]";
        }

        public double distanceTo(NavigableNode other) {
            // TODO: compute latitude longitudinal distance
            double diffX = other.latitude - latitude;
            double diffY = other.longitude - longitude;
            return Math.sqrt(diffX * diffX + diffY * diffY);
        }

        @Override
        public int hashCode() {
            return hashValue;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof NavigableNode) {
                NavigableNode other = (NavigableNode) obj;
                return latitude == other.latitude && longitude == other.longitude;
            }
            return false;
        }

        @Override
        public String toString() {
            return stringValue;
        }
    }
}
