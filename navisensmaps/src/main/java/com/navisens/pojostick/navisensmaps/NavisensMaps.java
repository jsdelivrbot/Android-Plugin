package com.navisens.pojostick.navisensmaps;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.navisens.motiondnaapi.MotionDna;
import com.navisens.motiondnaapi.MotionDnaApplication;
import com.navisens.motiondnaapi.MotionDnaInterface;
import com.navisens.pojostick.navisenscore.NavisensCore;
import com.navisens.pojostick.navisenscore.NavisensPlugin;

import java.util.Locale;
import java.util.Map;

/**
 * Created by Joseph on 6/23/17.
 * <p>
 * Navisens MotionDna map support via leaflet
 */

public class NavisensMaps extends Fragment implements NavisensPlugin {
    // TODO: test if GPS faster when pass into js as map center
    // https://stackoverflow.com/questions/10524381/gps-android-get-positioning-only-once

    /**
     * Usable map types are:
     * <ul>
     * No API key required
     * <li>{@link #OSM_Mapnik}</li>
     * <li>{@link #OSM_France}</li>
     * <br>
     * API key required, and custom styles available
     * <li>{@link #Thunderforest}</li>
     * <li>{@link #Mapbox}</li>
     * <br>
     * WIP no key required
     * <li>{@link #Esri}</li>
     * </ul>
     */
    @SuppressWarnings({"WeakerAccess", "unused"})
    public enum Maps {
        /**
         * Open Street Maps, does not require a key, no custom map style
         */
        OSM_Mapnik("OpenStreetMap_Mapnik"),
        /**
         * Open Street Maps, does not require a key, custom map style is France, slighty higher zoom compared to {@link #OSM_Mapnik}
         */
        OSM_France("OpenStreetMap_France"),
        /**
         * Thunderforest tiling servers, requires a key, default style is 'outdoors'
         */
        Thunderforest("Thunderforest"),
        /**
         * Mapbox tiling servers, requires a key, default style is 'mapbox.streets'
         */
        Mapbox("Mapbox"),
        /**
         * Esri tiling servers, not fully implemented yet, current access does not require key, but has missing tiles at high zooms
         */
        Esri("Esri");

        private final String name;

        Maps(String s) {
            name = s;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    private static final int REQUEST_MDNA_PERMISSIONS = 1;
    private static final String DEFAULT_MAP = "addMap_OpenStreetMap_Mapnik();";
    private static final double LOCAL_SCALING = 1; // Math.pow(2, -17);

    private static MotionDna.LocationStatus lastLocation = MotionDna.LocationStatus.UNINITIALIZED;
    private static boolean customLocation = false, shouldRestart = true;

    private WebView webView;
    private boolean useDefaultMap = true, useLocal = false, shareLocation = false;

    private NavisensCore core;

    private WebView webview;
    private String javascript;
    private double x, y, h;

    public NavisensMaps() {
        super();
    }

    @Override
    public boolean init(NavisensCore core, Object[] args) {
        this.core = core;
        this.javascript = "RUN(%b);";

        core.subscribe(this, NavisensCore.MOTION_DNA | NavisensCore.NETWORK_DNA);

        core.getSettings().requestGlobalMode();
        core.getSettings().requestPositioningMode(MotionDna.ExternalPositioningState.HIGH_ACCURACY);
        core.applySettings();

        return true;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        core.setActivity(this.getActivity());
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        webView = new WebView(getActivity()) {
            // https://stackoverflow.com/a/44278258
            @Override
            public boolean onTouchEvent(MotionEvent ev) {
                int action = ev.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        // Disallow ScrollView to intercept touch events.
                        this.getParent().requestDisallowInterceptTouchEvent(true);
                        break;

                    case MotionEvent.ACTION_UP:
                        // Allow ScrollView to intercept touch events.
                        this.getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }

                // Handle MapView's touch events.
                super.onTouchEvent(ev);
                return true;
            }
        };

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webView.addJavascriptInterface(new JavaScriptInterface(), "JSInterface");

        webView.loadUrl("file:///android_asset/index.0.0.9.html");

        this.setRetainInstance(true);

        webView.setWebViewClient(new WebViewClient() {
            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                redirectUrl(url);
                return true;
            }

            @TargetApi(Build.VERSION_CODES.N)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                redirectUrl(request.getUrl().toString());
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (shouldRestart) {
                    shouldRestart = false;
                    restart();
                    core.startServices();
                }
                evaluateJS(view);
            }

            private void redirectUrl(String url) {
                try {
                    Uri uri = Uri.parse(url);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                } catch (Exception e) {
                    // ignore bad url requests
                }
            }
        });

        return webView;
    }

    @Override
    public void onPause() {
        super.onPause();

        save();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (!MotionDnaApplication.checkMotionDnaPermissions(getActivity().getApplicationContext())) {
            System.err.println("ERROR: Insufficient permissions.");
            stop();
        }
    }

    /**
     * Add a map which does not require credentials
     *
     * @param name The type of map to add
     * @return a reference to this object
     */
    @SuppressWarnings("unused")
    public NavisensMaps addMap(Maps name) {
        appendJS(String.format("addMap_%s();", name));
        useDefaultMap = false;
        return this;
    }

    /**
     * Add a map with credentials, but use the default style
     *
     * @param name The type of map to add
     * @param key  Login credentials key for the given map type
     * @return a reference to this object
     */
    @SuppressWarnings("unused")
    public NavisensMaps addMap(Maps name, String key) {
        appendJS(String.format("addMap_%s('%s');", name, key));
        useDefaultMap = false;
        return this;
    }

    /**
     * Add a map with credentials, and specify a map style
     *
     * @param name  The type of map to add
     * @param key   Login credentials key for the given map type
     * @param mapid The map style, for example Mapbox maps define the style 'mapbox.streets'
     * @return a reference to this object
     */
    @SuppressWarnings("unused")
    public NavisensMaps addMap(Maps name, String key, String mapid) {
        appendJS(String.format("addMap_%s('%s', '%s');", name, key, mapid));
        useDefaultMap = false;
        return this;
    }

    /**
     * Add a custom user-defined map
     *
     * @param url         The url to the tiling server
     * @param jsonOptions A JavaScript object stored as JSON, including any parameters required by the url. See leaflet TileLayer for more documentation details
     * @return a reference to this object
     */
    @SuppressWarnings("unused")
    public NavisensMaps addMap(String url, String jsonOptions) {
        appendJS(String.format("addMap('%s', '%s');", url, jsonOptions));
        useDefaultMap = false;
        return this;
    }

    /**
     * Enable user control over the map. This gives the user the ability to set custom location and heading, which will disable NavisensLocation when in global mode.
     * <br>
     * NOTE: At the current time, user controls are not supported while in local mode, and calls to this method will be ignored if {@link #useLocalOnly()} is called.
     *
     * @return a reference to this object
     */
    @SuppressWarnings("unused")
    public NavisensMaps addControls() {
        appendJS("UI();");
        return this;
    }

    /**
     * Prevent maps from reinitializing, using the last saved state as the starting point.
     * <br>
     * NOTE: This method will not destroy save state on restart and may cause Maps to run out of memory. Make sure to call the {@link #restart()} function as necessary.
     *
     * @return a reference to this object
     */
    @SuppressWarnings("unused")
    public NavisensMaps preventRestart() {
        shouldRestart = false;
        return this;
    }

    /**
     * Only provide the local cartesian coordinates. The user will begin at coordinates (0, 0), and location services will begin immediately. No default map will be used.
     * <br>
     * Normally, when using global mode by default, location services will require both GPS and user movement (about 1-2 blocks of walking) before location is fully initialized.
     * <br>
     * NOTE: At the current time, local mode does not support enabling controls, and will disregard calls to {@link #addControls()}.
     *
     * @return a reference to this object
     */
    @SuppressWarnings("unused")
    public NavisensMaps useLocalOnly() {
        javascript = "setSimple();" + javascript;
        useLocal = true;
        customLocation = true;
        core.getSettings().overrideEstimationMode(MotionDna.EstimationMode.LOCAL);
        useDefaultMap = false;
        return this;
    }

    /**
     * Hide plotting markers, and prevent user from accessing
     *
     * @return a reference to this object
     */
    @SuppressWarnings("unused")
    public NavisensMaps hideMarkers() {
        appendJS("hideClustering();");
        return this;
    }

    /**
     * Pause the location services
     *
     * @return whether location services were paused successfully
     */
    @SuppressWarnings("unused")
    public boolean pause() {
        if (core.getMotionDna() != null) {
            core.getMotionDna().pause();
            return true;
        }
        return false;
    }

    /**
     * Resume providing location services
     *
     * @return whether location services were resumed successfully
     */
    @SuppressWarnings("unused")
    public boolean resume() {
        if (core.getMotionDna() != null) {
            core.getMotionDna().resume();
            return true;
        }
        return false;
    }

    /**
     * Save the current display, user location, and map zoom temporarily. When the activity restarts, this will be cleared unless {@link #preventRestart()} is called.
     *
     * @return whether save was called successfully
     */
    @SuppressWarnings("unused")
    public boolean save() {
        if (webView != null)
            webView.evaluateJavascript("if (typeof SAVE !== 'undefined') SAVE();", null);
        return webView != null;
    }

    /**
     * Restart Maps cache to clear the currently displayed location and path
     *
     * @return whether restart was called successfully
     */
    @SuppressWarnings("unused")
    public boolean restart() {
        if (webView != null)
            webView.evaluateJavascript("START();", null);
        return webView != null;
    }

    /**
     * Signals all components to terminate.
     * @return whether stop was successful
     */
    @SuppressWarnings("unused")
    public boolean stop() {
        if (webView != null)
            webView.evaluateJavascript("STOP();", null);
        webView = null;
        core.remove(this);
        return true;
    }

    /**
     * Display paths and markers
     * @return whether displaying paths worked
     */
    @SuppressWarnings("unused")
    public NavisensMaps showPath() {
        appendJS("DEBUG();");
        return this;
    }

    public NavisensMaps enableLocationSharing() {
        shareLocation = true;
        return this;
    }

    void appendJS(String js) {
        javascript += js;
    }

    void evaluateJS(WebView view) {
        javascript = String.format(Locale.ENGLISH, "SET_ID('%s');", core.getMotionDna().getDeviceID()) + javascript;
        this.webview = view;
        this.webview.evaluateJavascript(
                String.format(javascript, !useLocal && lastLocation == MotionDna.LocationStatus.UNINITIALIZED),
                null);
        if (useDefaultMap)
            this.webview.evaluateJavascript(DEFAULT_MAP, null);
        if (shareLocation)
            core.getMotionDna().startUDP();
    }

    @Override
    public void receiveMotionDna(MotionDna motionDna) {
        if (this.webview != null) {
            MotionDna.Location location = motionDna.getLocation();

            if (motionDna.getMotion() != null) System.out.printf("%s(%s): %s\n", motionDna.getDeviceName(), motionDna.getID(), motionDna.getMotion().motionType);
            if (motionDna.getMotion() == null || motionDna.getMotion().motionType == null) {
                return;
            }
            if (useLocal) {
                x = location.localLocation.x * LOCAL_SCALING;
                y = location.localLocation.y * LOCAL_SCALING;
                h = location.heading;
                // System.out.println(x + ", " + y + ", " + h);
                this.webview.evaluateJavascript(
                        String.format(Locale.ENGLISH, "if (typeof SESSION_RELOADED !== 'undefined') addPoint('%s', %.7f, %.7f, %d);",
                                motionDna.getID(),
                                y,
                                x,
                                motionDna.getMotion().motionType.ordinal()),
                        null
                );
                this.webview.evaluateJavascript(
                        String.format(Locale.ENGLISH, "if (typeof SESSION_RELOADED !== 'undefined') move('%s', %.7f, %.7f, %.7f, %d);",
                                motionDna.getID(),
                                y,
                                x,
                                h,
                                motionDna.getMotion().motionType.ordinal()),
                        null
                );
            } else {
                if (location.locationStatus != null && lastLocation != location.locationStatus) {
                    switch (location.locationStatus) {
                        case NAVISENS_INITIALIZING:
                            this.webview.evaluateJavascript("acquiredGPS();", null);
                            break;
                        case NAVISENS_INITIALIZED:
                            this.webview.evaluateJavascript("acquiredLocation();", null);
                    }
                    lastLocation = location.locationStatus;
                }

                if (customLocation || location.locationStatus == MotionDna.LocationStatus.NAVISENS_INITIALIZED) {
                    this.webview.evaluateJavascript(
                            String.format(Locale.ENGLISH, "if (typeof SESSION_RELOADED !== 'undefined') addPoint('%s', %.7f, %.7f, %d);",
                                    motionDna.getID(),
                                    location.globalLocation.latitude,
                                    location.globalLocation.longitude,
                                    motionDna.getMotion().motionType.ordinal()),
                            null
                    );
                }
                this.webview.evaluateJavascript(
                        String.format(Locale.ENGLISH, "if (typeof SESSION_RELOADED !== 'undefined') move('%s', %.7f, %.7f, %.7f, %d);",
                                motionDna.getID(),
                                location.globalLocation.latitude,
                                location.globalLocation.longitude,
                                location.heading,
                                motionDna.getMotion().motionType.ordinal()),
                        null
                );
            }
        }
    }

    @Override
    public void receiveNetworkData(MotionDna motionDna) {
        receiveMotionDna(motionDna);
    }

    @Override
    public void receiveNetworkData(MotionDna.NetworkCode networkCode, Map<String, ? extends Object> map) {
    }

    @Override
    public void receivePluginData(String s, Object o) {
    }

    @Override
    public void reportError(MotionDna.ErrorCode errorCode, String s) {
    }

    private class JavaScriptInterface {
        @SuppressWarnings("unused")
        @JavascriptInterface
        public void customLocationInitialized(double lat, double lng, double heading) {
            if (useLocal) {
//                motionDna.setCartesianOffsetInMeters(lng - motionDnaService.x, lat - motionDnaService.y);
//                motionDna.setHeadingInDegrees(heading);
//                motionDna.setLocalHeadingOffsetInDegrees(heading - motionDnaService.h);
            } else {
                core.getMotionDna().setLocationLatitudeLongitudeAndHeadingInDegrees(lat, lng, heading);
            }
            customLocation = true;
        }
    }
}
