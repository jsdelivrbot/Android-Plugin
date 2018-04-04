package com.navisens.pojostick.navihelp;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.support.annotation.NonNull;
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
import com.navisens.pojostick.navisenscore.NavisensCore;
import com.navisens.pojostick.navisenscore.NavisensPlugin;
import com.navisens.pojostick.navishare.NaviShare;
import com.navisens.pojostick.navishare.NaviShareListener;

import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Joseph Chen on 3/21/18.
 * <p>
 *     Tutorial NavisensPlugins.
 * </p>
 */

public class NaviHelp extends Fragment implements NavisensPlugin, NaviShareListener {
    public static final String PLUGIN_IDENTIFIER = "com.navisens.pojostick.navihelp";
    public static final int OPERATION_SHOOT = 500,
                            OPERATION_CHARGE = 501,
                            OPERATION_VIBRATE = 10000;

    private static final String NAVISENS_PLUGINS = "com.navisens.pojostick.",
                                MESSAGE_EXIT = "!",
                                MESSAGE_META = "'",
                                MESSAGE_SHOOT = "@",
                                MESSAGE_CHARGE = "#";
    private static final double GLOBAL_SCALE = 111111;

    private NavisensCore core;
    private NaviShare share;
    private WebView webView, webview;
    private String javascript, color, username;
    private boolean firstTimeSetup, connected;
    private Timer timer;

    public NaviHelp() {
        timer = new Timer();
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
        webView.addJavascriptInterface(new JSInterface(), "JSInterface");

        webView.loadUrl("file:///android_asset/index.1.0.1.html");

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
                if (firstTimeSetup) {
                    firstTimeSetup = false;
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (!MotionDnaApplication.checkMotionDnaPermissions(getActivity().getApplicationContext())) {
            System.err.println("ERROR: Insufficient permissions.");
            stop();
        }
    }

    void appendJS(String js) {
        javascript += js;
    }

    void evaluateJS(WebView view) {
        javascript = String.format(Locale.ENGLISH, "SET_ID('%s');", core.getMotionDna().getDeviceID()) + javascript;
        this.webview = view;
        this.webview.evaluateJavascript(javascript, null);
    }

    // ===== API =====

    public NaviHelp setUsername(String name) {
        username = name.replace("'", "’");
        return this;
    }

    public NaviHelp setColor(String hex) {
        color = hex.replace("'", "’");
        return this;
    }

    public NaviHelp start() {
        if (username == null) {
            username = core.getMotionDna().device_id;
        }
        if (color == null) {
            color = "#" + core.getMotionDna().device_id.substring(0, 6);
        }
        meta(core.getMotionDna().device_id, username, color);
        connected = share.testConnect();
        timer.cancel();
        timer = new Timer();
        timer.schedule(new SyncMetadata(), 5000, 5000);
        return this;
    }

    // ===== Helpers =====

    private void meta(String id, String name, String hex) {
        name = name.replace("'", "’");
        hex = hex.replace("'", "’");
        String js = String.format(Locale.ENGLISH, "META('%s', '%s', '%s');", id, name.length() > 32 ? name.substring(0, 32) : name, hex);
        if (webview != null) {
            webview.evaluateJavascript(js, null);
        } else {
            appendJS(js);
        }
    }

    private void shoot(final String id) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (webview != null) {
                    webview.evaluateJavascript(String.format(Locale.ENGLISH, "SHOOT('%s');", id), null);
                }
            }
        });
    }

    private void charge(final String id) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (webview != null) {
                    webview.evaluateJavascript(String.format(Locale.ENGLISH, "CHARGE('%s');", id), null);
                }
            }
        });
    }

    private void exit(final String id) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (webview != null) {
                    webview.evaluateJavascript(String.format(Locale.ENGLISH, "EXIT('%s');", id), null);
                }
            }
        });
    }

    private void vibrate(final int ms) {
        if (ms > 0 && ms < 5000)
            ((Vibrator) core.getMotionDna().getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(ms);
    }

    // =====

    @Override
    public boolean init(NavisensCore navisensCore, Object[] objects) {
        if (objects.length >= 1 && objects[0] instanceof NaviShare) {
            this.share = (NaviShare) objects[0];
        } else {
            System.err.println("Error in NaviHelp.init: Expected parameter NaviShare is missing.");
            return false;
        }

        this.core = navisensCore;
        this.javascript = "";

        core.subscribe(this, NavisensCore.MOTION_DNA | NavisensCore.NETWORK_DNA | NavisensCore.PLUGIN_DATA);
        core.broadcast(PLUGIN_IDENTIFIER, NavisensCore.OPERATION_INIT);

        core.getSettings().requestARMode();
        core.getSettings().requestCallbackRate(100);
        core.getSettings().requestNetworkRate(0);
        core.getSettings().requestPositioningMode(MotionDna.ExternalPositioningState.HIGH_ACCURACY);
        core.getSettings().overrideEstimationMode(MotionDna.EstimationMode.LOCAL);
        core.applySettings();

        core.getMotionDna().setMapCorrectionEnabled(false);

        return true;
    }

    @Override
    public boolean stop() {
        webView = null;
        if (core != null) {
            share.sendMessage(MESSAGE_EXIT);
            core.remove(this);
            core.broadcast(PLUGIN_IDENTIFIER, NavisensCore.OPERATION_STOP);
        }
        return true;
    }

    @Override
    public void receiveMotionDna(MotionDna motionDna) {
        if (webview != null) {
            MotionDna.Location location = motionDna.getLocation();
            MotionDna.Motion motionType = motionDna.getMotion();

            webview.evaluateJavascript(
                    String.format(Locale.ENGLISH, "MOVE('%s', %f, %f, %f, %d);",
                            motionDna.getID(),
                            GLOBAL_SCALE * location.globalLocation.latitude,
                            GLOBAL_SCALE * location.globalLocation.longitude,
                            location.heading,
                            motionType.motionType.ordinal()),
                    null
            );
        }
    }

    @Override
    public void receiveNetworkData(MotionDna motionDna) {
        receiveMotionDna(motionDna);
    }

    @Override
    public void receiveNetworkData(MotionDna.NetworkCode networkCode, Map<String, ? extends Object> map) {}

    @Override
    public void receivePluginData(String s, int i, Object... objects) {
        if (!s.contains(NAVISENS_PLUGINS)) {
            switch (i) {
                case OPERATION_CHARGE:
                    charge(core.getMotionDna().device_id);
                    share.sendMessage(MESSAGE_CHARGE);
                    break;
                case OPERATION_VIBRATE:
                    if (objects.length == 1 && objects[0] instanceof Integer) {
                        vibrate((int) objects[0]);
                    }
            }
        }
    }

    @Override
    public void reportError(MotionDna.ErrorCode errorCode, String s) {}

    @Override
    public void messageReceived(String s, String s1) {
        if (s1.startsWith(MESSAGE_EXIT)) {
            exit(s);
        } else if (s1.startsWith(MESSAGE_META)) {
            s1 = s1.substring(1);
            String[] vals = s1.split(MESSAGE_META);
            if (vals.length == 2) {
                meta(s, vals[0], vals[1]);
            }
        } else if (s1.startsWith(MESSAGE_SHOOT)){
            shoot(s);
        } else if (s1.startsWith(MESSAGE_CHARGE)) {
            charge(s);
        }
    }

    @Override
    public void roomOccupancyChanged(Map<String, Integer> map) {}

    @Override
    public void serverCapacityExceeded() {
        connected = false;
    }

    @Override
    public void roomCapacityExceeded() {
        connected = false;
    }

    private class JSInterface {
        @SuppressWarnings("unused")
        @JavascriptInterface
        public void shot() {
            shoot(core.getMotionDna().device_id);
            core.broadcast(PLUGIN_IDENTIFIER, OPERATION_SHOOT);
            share.sendMessage(MESSAGE_SHOOT);
        }

        @SuppressWarnings("unused")
        @JavascriptInterface
        public void charged() {
            core.broadcast(PLUGIN_IDENTIFIER, OPERATION_CHARGE);
        }

        @SuppressWarnings("unused")
        @JavascriptInterface
        public void position(double x, double y, double h) {
            core.getMotionDna().setLocationLatitudeLongitudeAndHeadingInDegrees(x / GLOBAL_SCALE, y / GLOBAL_SCALE, h);
        }

        @SuppressWarnings("unused")
        @JavascriptInterface
        public void vibrated(int ms) {
            vibrate(ms);
        }
    }

    private class SyncMetadata extends TimerTask {
        @Override
        public void run() {
            if (!connected) {
                connected = share.testConnect();
            }
            share.sendMessage(MESSAGE_META + username + MESSAGE_META + color);
        }
    }
}
