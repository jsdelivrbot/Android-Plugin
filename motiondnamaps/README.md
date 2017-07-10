# Motion Dna Maps

This is a plugin that provides a quick-and-easy map built upon [leafletjs](http://leafletjs.com/).

The latest stable version is `0.1.1`.

To include the plugin in your code, add the following to your app's dependencies:

```gradle
    compile 'com.navisens:motiondnamaps:0.1.1'
```

## Setup

1. Add the following to your Manifest:
```xml
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

2. The `MotionDnaMaps` is a fragment, and thus can be added into any activity as you would a fragment. For example, in the `onCreate` function, add this:
```java
    MotionDnaMaps maps = new MotionDnaMaps("/* YOUR DEV KEY */");
    getFragmentManager().beginTransaction().replace(R.id.content, maps).addToBackStack(null).commitAllowingStateLoss();
```

The `R.id.content` is the id of the layout element you want to add the fragment into.

## Useful Information

* Use `commitAllowingStateLoss` unless you wish to override and implement state saving of the fragment yourself.
* It is recommended that you handle the `android:configChanges` of any layouts with the `MotionDnaMaps` object yourself (you can simply do nothing), to increase orientation change loading speeds. When the screen state changes, the fragment will be destroyed and recreated as per Android requirements, and may take some time. `MotionDnaMaps` will automatically save all data anytime a configuration change is detected (and the fragment restarted). Handling orientations changes yourself will prevent the fragment from restarting. (Although `MotionDnaMaps` will still save everything on configuration changes, it will no longer need to rebuild and reinstantiate all map properties).
* It is recommended as an alternative to the above change, that you set the orientation of your app to portrait mode only, and prevent unnecessary configuration changes, as our SDK currently is most effective in the portrait mode state.
* Note that `MotionDnaMaps` will automatically consume scroll events, so if you wish to include the map in a scroll layout, please add margins, or ensure that the map never covers the entire screen, as it will consume all scroll events, making it impossible to continue scrolling if it ever consumes the entire displayable area of a scroll layout.

## Video Tutorial

Coming Soon!

## API

As `MotionDnaMaps` is intended to be a quick placeholder not meant for full customization support in production environments, only some small control is provided for the developer. If you wish to customize, the source code is provided alongside this document.

## Setup

Setup should be done all at once. Since setup functions return a reference of the `MotionDnaMaps` object, you can chain setup calls together.

Example:
```java
MotionDnaMaps maps = new MotionDnaMaps(DEV_KEY)
                         .addMap(MotionDnaMaps.Maps.OSM_Mapnik)
                         .addControls();
```

#### `MotionDnaMaps(String devKey)`

This is the constructor you will use to create a maps instance. A dev key is required. Do not use the `MotionDnaMaps()` constructor as that one is used for the fragment upon reinitialization only. If you are not familiar with how fragments work, please review the documentation provided on the [Android page](https://developer.android.com/guide/components/fragments.html).

Once you have created a `MotionDnaMaps` object, you can use it to call further setup functions. Make sure to call all setup functions upon object creation. If you try to invoke a setup function while the fragment is in display, you will need to restart the fragment for those changes to take effect.

#### `MotionDnaMaps.Maps`

These are the default tiling servers provided. You may use a custom tiling server instead with the [`addMap(url, json)`](#motiondnamaps-addmapstring-url-string-jsonoptions) method instead.

 * `OSM_Mapnik`: Open Street Maps, does not require a key, no custom map style
 * `OSM_France`: Open Street Maps, does not require a key, custom map style is France, slighty higher zoom compared to OSM_Mapnik
 * `Thunderforest`: Thunderforest tiling servers, requires a key, default style is 'outdoors'
 * `Mapbox`: Mapbox tiling servers, requires a key, default style is 'mapbox.streets'
 * `Esri`: Esri tiling servers, not fully implemented yet, current access does not require key, but has missing tiles at high zooms

#### `MotionDnaMaps addMap(MotionDnaMaps.Maps name)`

Use this to add a basic map which does not require additional setup fields. Supported maps are the `OSM_Mapnik`, `OSM_France`, and `Esri`

#### `MotionDnaMaps addMap(MotionDnaMaps.Maps name, String key)`

Use this to add a map which requires an access key. Valid maps are `Thunderforest` and `Mapbox`. A default styling will be selected. To specify a custom styling, use [`addMap(name, key, mapid)`](#motiondnamaps-addmapmotiondnamapsmaps-name-string-key-string-mapid)

#### `MotionDnaMaps addMap(MotionDnaMaps.Maps name, String key, String mapid)`

Use this to add a map which requires an access key, and specify a custom map theme.

#### `MotionDnaMaps addMap(String url, String jsonOptions)`

Use this to add a custom map tiling server.

`url` should point to a tiling server
`jsonOptions` should include any variables included with your `url`

Example:
```java
addMap("http://{s}.tile.thunderforest.com/outdoors/{z}/{x}/{y}.png?apikey={apikey}", "{
    apikey: "/* THUNDERFOREST KEY */"
}");
```

For more information, please see documentation with [Leaflet](http://leafletjs.com/reference-1.1.0.html#tilelayer)

#### `MotionDnaMaps addControls()`

Use this to enable the user to control more features on the map. If this method is called, users will be able to do the following:

* Set a custom location
* Set a custom heading
* View all data points
* View stats on their movement (e.g. how long they stood still)

Note that this method will not do anything if you also call [`useLocalOnly()`](#motiondnamaps-uselocalonly).

#### `MotionDnaMaps preventRestart()`

Use this to prevent the map from running clean-up whenever the app stops and starts again. This will retain all data points from the user's previous instance, along with the map's position, zoom level, etc.

Note: This is a dangerous method to call, as it prevents cleaning up of browser memory. Although all data is compressed, overly using this method can result in the browser reaching it's maximum memory, preventing further tracking of points. It is advised to use this only while within the same application instance, when the current activity needs to be destroyed and recreated again.

#### `MotionDnaMaps useLocalOnly()`

This will set navigation to local cartesian coordinates, preventing usage of GPS localization. Furthermore, no default map will be added.

You can use the custom [`addMap(url, json)`](#motiondnamaps-addmapstring-url-string-jsonoptions) with this to set custom map tiles that better reflect local coordinates (for example an open grid or virtual world).

Note: the [`addControls()`](#motiondnamaps-addcontrols) will be disabled when local coordinates are enabled, preventing the user from setting a custom location and heading.

## State Changes

The following methods are used to control the state of the maps object, and will return whether they executed successfully. They always will, unless an invalid state is reached (for example trying to call [`resume()`](#boolean-resume) after calling [`stop()`](#void-stop)

#### `boolean pause()`

Pauses the MotionDna algorithm, which can save battery when not in use, but remembers the user's last location, heading, etc. (as opposed to stopping the algorithm alltogether).

Use [`resume()`](#boolean-pause) to resume the algorithm.

#### `boolean resume()`

Resumes the MotionDna algorithm if it was paused.

#### `boolean save()`

Saves the current viewport. This allows destroying the fragment and re-attaching afterwards at the same state.

#### `boolean restart()`

Restarts the current viewport cache. This will reset all tracked points, along with resetting the user's view position and zoom level.

#### `void stop()`

Stops the MotionDna algorithm. Call this before the fragment is detached permanently.
