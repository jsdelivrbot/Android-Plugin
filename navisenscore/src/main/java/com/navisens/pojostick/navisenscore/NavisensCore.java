package com.navisens.pojostick.navisenscore;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.util.SparseArray;
import android.util.SparseIntArray;

import com.navisens.motiondnaapi.MotionDna;
import com.navisens.motiondnaapi.MotionDnaApplication;
import com.navisens.motiondnaapi.MotionDnaInterface;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by Joseph on 9/29/17.
 * <p>
 *     Navisens Core plugin manager
 * </p>
 */

public class NavisensCore {

    @SuppressWarnings("unused")
    public static final int NOTHING      = 0,
                            MOTION_DNA   = 1,
                            NETWORK_DNA  = 2,
                            NETWORK_DATA = 4,
                            PLUGIN_DATA  = 8,
                            ERRORS       = 16,
                            ALL          = 31;

    private static final int REQUEST_MDNA_PERMISSIONS = 1;

    private static MotionDnaApplication motionDna;
    private static NavisensSettings settings;
    private static String devKey;

    private MotionDnaService motionDnaService;
    private Set<com.navisens.pojostick.navisenscore.NavisensPlugin> plugins;
    private SparseArray<Set<com.navisens.pojostick.navisenscore.NavisensPlugin>> subscribers;
    private boolean needsApplySettings;

    public NavisensCore(String devkey, Activity act) {
        NavisensCore.devKey = devkey;
        motionDnaService = new MotionDnaService();
        plugins = new HashSet<>();
        subscribers = new SparseArray<>();
        for (int i = 1; i <= ERRORS; i <<= 1)
            subscribers.put(i, new HashSet<com.navisens.pojostick.navisenscore.NavisensPlugin>());
        needsApplySettings = true;

        settings = new NavisensSettings();
        setActivity(act);
    }

    /*
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (!MotionDnaApplication.checkMotionDnaPermissions(getActivity().getApplicationContext())) {
            System.err.println("ERROR: Insufficient permissions.");
            stop();
        }
    }
    */

    /**
     * Add a NavisensPlugin to the core.
     *
     * @param navisensPlugin The NavisensPlugin class type
     * @param args           Any arguments required to set up NavisensPlugin
     * @return the NavisensPlugin instance if successful, or null otherwise
     */
    @SuppressWarnings("unused")
    public <T extends com.navisens.pojostick.navisenscore.NavisensPlugin> T init(Class<T> navisensPlugin, Object... args) {
        try {
            T plugin = navisensPlugin.newInstance();
            if (!plugin.init(this, args)) {
                plugin.stop();
                return null;
            }
            plugins.add(plugin);
            return plugin;
        } catch (IllegalAccessException | InstantiationException e) {
            return null;
        }
    }

    /**
     * Stops Navisens Core, and releases any resources. Once this is called, calling other functions will result in undefined behavior.
     * Make sure all other plugins are also stopped by calling NavisensCore.stop(plugin) on them. If other plugins are still running, this method will return false, and will not stop Navisens Core.
     * Use NavisensCore.stopAll() to terminate all remaining plugins too.
     *
     * @return whether Navisens Core was stopped successfully
     */
    @SuppressWarnings("unused")
    public boolean stop() {
        if (plugins.size() > 0) {
            return false;
        }
        if (motionDna != null) {
            motionDna.stop();
            motionDna = null;
        }
        return true;
    }

    /**
     * Stops all plugins, and then stops Navisens Core. Will return false if any plugin failed to stop.
     * When returning false, some plugins may stop successfully, but there is no guarantee that all plugins that can be stopped will be stopped.
     *
     * @return whether all plugins were stopped successfully
     */
    @SuppressWarnings("unused")
    public boolean stopAll() {
        Iterator<com.navisens.pojostick.navisenscore.NavisensPlugin> iterator = plugins.iterator();
        com.navisens.pojostick.navisenscore.NavisensPlugin plugin;
        while (iterator.hasNext() && (plugin = iterator.next()) != null) {
            if (!stop(plugin)) return false;
        }
        return stop();
    }

    /**
     * Removes a plugin from the tracked plugins list
     *
     * @return Whether plugin was found and stopped
     */
    @SuppressWarnings("unused")
    public boolean stop(com.navisens.pojostick.navisenscore.NavisensPlugin plugin) {
        if (plugins.contains(plugin)) {
            if (!plugin.stop()) return false;
            remove(plugin);
            return true;
        }
        return false;
    }

    /**
     * Remove a plugin and untrack it. Assumes plugin has stopped or is being forcefully removed.
     *
     * @param plugin The plugin to remove
     */
    @SuppressWarnings("unused")
    public void remove(com.navisens.pojostick.navisenscore.NavisensPlugin plugin) {
        if (plugins.contains(plugin)) {
            plugins.remove(plugin);
            unsubscribe(plugin, ALL);
        }
    }

    /**
     * Broadcast data to all plugins
     * <br>
     * The identifier tag should use the plugin's {@code .getClass().getName()} by default, or a custom unique identifier otherwise
     *
     * @param identifier the identifier to tag this data packet
     * @param data data to broadcast
     */
    @SuppressWarnings("unused")
    public void broadcast(String identifier, Object data) {
        for (com.navisens.pojostick.navisenscore.NavisensPlugin plugin : subscribers.get(PLUGIN_DATA)) {
            plugin.receivePluginData(identifier, data);
        }
    }

    /**
     * Listen for updates on specified channels.
     * <br>
     *     Select from:
     *     <ul>
     *         <li>{@link #MOTION_DNA}</li>
     *         <li>{@link #NETWORK_DNA}</li>
     *         <li>{@link #NETWORK_DATA}</li>
     *         <li>{@link #PLUGIN_DATA}</li>
     *         <li>{@link #ERRORS}</li>
     *     </ul>
     *
     * @param plugin this plugin
     * @param which channels to listen to or'd together
     */
    @SuppressWarnings("unused")
    public void subscribe(com.navisens.pojostick.navisenscore.NavisensPlugin plugin, int which) {
        for (int i = 1; i <= ERRORS; i <<= 1) {
            if ((i & which) > 0) {
                subscribers.get(i).add(plugin);
            }
        }
    }

    /**
     * Stop listening for updates on specified channels.
     * <br>
     *     Select from:
     *     <ul>
     *         <li>{@link #MOTION_DNA}</li>
     *         <li>{@link #NETWORK_DNA}</li>
     *         <li>{@link #NETWORK_DATA}</li>
     *         <li>{@link #PLUGIN_DATA}</li>
     *         <li>{@link #ERRORS}</li>
     *     </ul>
     *
     * @param plugin this plugin
     * @param which channels to terminate or'd together
     */
    @SuppressWarnings("unused")
    public void unsubscribe(com.navisens.pojostick.navisenscore.NavisensPlugin plugin, int which) {
        for (int i = 1; i <= ERRORS; i <<= 1) {
            if ((i & which) > 0) {
                subscribers.get(i).remove(plugin);
            }
        }
    }

    /**
     * Link the current running activity against NavisensCore
     *
     * @param act the current activity
     */
    @SuppressWarnings("unused")
    public void setActivity(Activity act) {
        if (motionDnaService != null) {
            motionDnaService.loadedActivity(act);
        }
    }

    /**
     * Get the motion dna instance
     *
     * @return a motion dna application
     */
    @SuppressWarnings("unused")
    public MotionDnaApplication getMotionDna() {
        return motionDna;
    }

    /**
     * Get the navisens settings
     *
     * @return a navisens settings
     */
    @SuppressWarnings("unused")
    public NavisensSettings getSettings() {
        return settings;
    }

    /**
     * Apply navisens settings
     */
    @SuppressWarnings("unused")
    public void applySettings() {
        motionDnaService.applySettings();
    }

    /**
     * Start the motionDnaService
     */
    @SuppressWarnings("unused")
    public void startServices() {
        motionDnaService.startServices();
    }

    // =========================
    // INTERNALS
    // =========================

    public static class NavisensSettings {
        private Boolean arMode;
        private Integer callbackRate;
        private MotionDna.EstimationMode estimationMode;
        private MotionDna.ExternalPositioningState positioningMode;
        private Integer networkRate;
        private MotionDna.PowerConsumptionMode powerMode;
        private String room, host, port;

        @SuppressWarnings("unused")
        public void requestARMode() {
            overrideARMode(true);
        }

        @SuppressWarnings("unused")
        public void overrideARMode(boolean mode) {
            arMode = mode;
        }

        @SuppressWarnings("unused")
        public void requestCallbackRate(int rate) {
            if (callbackRate == null || rate < callbackRate) {
                overrideCallbackRate(rate);
            }
        }

        @SuppressWarnings("unused")
        public void overrideCallbackRate(int rate) {
            callbackRate = rate;
        }

        @SuppressWarnings("unused")
        public void requestGlobalMode() {
            overrideEstimationMode(MotionDna.EstimationMode.GLOBAL);
        }

        @SuppressWarnings("unused")
        public void overrideEstimationMode(MotionDna.EstimationMode mode) {
            estimationMode = mode;
        }

        @SuppressWarnings("unused")
        public void requestPositioningMode(MotionDna.ExternalPositioningState mode) {
            if (positioningMode == null
                    || (positioningMode != MotionDna.ExternalPositioningState.HIGH_ACCURACY
                    && (mode == MotionDna.ExternalPositioningState.LOW_ACCURACY
                    || mode == MotionDna.ExternalPositioningState.HIGH_ACCURACY))) {
                overridePositioningMode(mode);
            }
        }

        @SuppressWarnings("unused")
        public void overridePositioningMode(MotionDna.ExternalPositioningState mode) {
            positioningMode = mode;
        }

        @SuppressWarnings("unused")
        public void requestNetworkRate(int rate) {
            if (networkRate == null || rate < networkRate) {
                overrideNetworkRate(rate);
            }
        }

        @SuppressWarnings("unused")
        public void overrideNetworkRate(int rate) {
            networkRate = rate;
        }

        @SuppressWarnings("unused")
        public void requestPowerMode(MotionDna.PowerConsumptionMode mode) {
            if (powerMode == null
                    || (mode != null && powerMode.ordinal() < mode.ordinal())) {
                overridePowerMode(mode);
            }
        }

        @SuppressWarnings("unused")
        public void overridePowerMode(MotionDna.PowerConsumptionMode mode) {
            powerMode = mode;
        }

        @SuppressWarnings("unused")
        public void requestHost(String host, String port) {
            overrideHost(host, port);
        }

        @SuppressWarnings("unused")
        public void overrideHost(String host, String port) {
            this.host = host;
            this.port = port;
        }

        @SuppressWarnings("unused")
        public void requestRoom(String room) {
            overrideRoom(room);
        }

        @SuppressWarnings("unused")
        public void overrideRoom(String room) {
            this.room = room;
        }
    }

    private class MotionDnaService implements MotionDnaInterface {
        Activity activity;

        void loadedActivity(Activity act) {
            this.activity = act;
            if (motionDna == null) {
                motionDna = new MotionDnaApplication(this);
                motionDna.runMotionDna(devKey);
                if (needsApplySettings) applySettings();
            } else {
                motionDna.motionDna = this;
            }
        }

        void applySettings() {
            if (motionDna == null) {
                needsApplySettings = true;
                return;
            }
            motionDna.setARModeEnabled(settings.arMode != null ? settings.arMode : false);
            motionDna.setBinaryFileLoggingEnabled(true);
            motionDna.setCallbackUpdateRateInMs(settings.callbackRate != null ? settings.callbackRate : 100);
            motionDna.setExternalPositioningState(settings.positioningMode != null ? settings.positioningMode : MotionDna.ExternalPositioningState.HIGH_ACCURACY);
            motionDna.setMapCorrectionEnabled(true);
            motionDna.setNetworkUpdateRateInMs(settings.networkRate != null ? settings.networkRate : 100);
            motionDna.setPowerMode(settings.powerMode != null ? settings.powerMode : MotionDna.PowerConsumptionMode.PERFORMANCE);
            ActivityCompat.requestPermissions(this.activity, MotionDnaApplication.needsRequestingPermissions(), REQUEST_MDNA_PERMISSIONS);
            needsApplySettings = false;
        }

        void startServices() {
            if (motionDna == null) return;
            if (settings.estimationMode == MotionDna.EstimationMode.GLOBAL) {
                motionDna.setLocationNavisens();
            }
            if (settings.host != null && settings.port != null) {
                motionDna.stopUDP();
                if (settings.room == null) {
                    motionDna.startUDPHostAndPort(settings.host, settings.port);
                } else {
                    motionDna.startUDP(settings.room, settings.host, settings.port);
                }
            }
        }

        @Override
        public void receiveMotionDna(MotionDna motionDna) {
            for (com.navisens.pojostick.navisenscore.NavisensPlugin plugin : subscribers.get(MOTION_DNA)) {
                plugin.receiveMotionDna(motionDna);
            }
        }

        @Override
        public void receiveNetworkData(MotionDna motionDna) {
            for (com.navisens.pojostick.navisenscore.NavisensPlugin plugin : subscribers.get(NETWORK_DNA)) {
                plugin.receiveNetworkData(motionDna);
            }
        }

        @Override
        public void receiveNetworkData(MotionDna.NetworkCode networkCode, Map<String, ? extends
                Object> map) {
            for (com.navisens.pojostick.navisenscore.NavisensPlugin plugin : subscribers.get(NETWORK_DATA)) {
                plugin.receiveNetworkData(networkCode, map);
            }
        }

        @Override
        public void reportError(MotionDna.ErrorCode errorCode, String s) {
//            System.err.println(s);
            for (com.navisens.pojostick.navisenscore.NavisensPlugin plugin : subscribers.get(ERRORS)) {
                plugin.reportError(errorCode, s);
            }
        }

        @Override
        public Context getAppContext() {
            return this.activity.getApplicationContext();
        }

        @Override
        public PackageManager getPkgManager() {
            return this.activity.getPackageManager();
        }
    }
}
