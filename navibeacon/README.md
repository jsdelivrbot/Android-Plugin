# NaviBeacon

This plugin helps facilitate connecting with nearby beacons and allows automatically initializing user location with beacons, or doing custom actions upon entering beacon range.

The latest stable version is `0.8.2`, and it is built on top of NavisensCore `3.1`.

Add the following to your app's dependencies:

```gradle
    compile 'com.navisens:navibeacon:0.8.2'
```

## Setup

1. You must add the following to your app's dependencies:
```gradle
    compile 'org.altbeacon:android-beacon-library:2.+'
```
2. Your device must have support for bluetooth capabilities.

## API

`NaviBeacon` makes use of beacons that you have already set up. Beacons are finicky and take some time to configure. You should have the beacon's UUID recorded in order to tell our SDK which beacons to range for. When a device enters range of one of your beacons, you can have the plugin execute certain actions. By default, this plugin will attempt to set the device's location to a pre-set global location and heading.

When setting up your beacon, you should record a latitude, longitude, and heading that a user would be at and facing. For example, if you have a beacon set up at a front desk, you might record the latitude and longitude of a user standing in front of the front desk, and facing directly at the desk. If you do not add these latitude, longitude, and heading readings, then you should manually configure a callback to respond to beacon ranging.

Also note that the computed distances when using beacons are not very accurate unless taken over multiple samples, and thus there is some expected latency. The user should stay within range of the beacon for at least 5 seconds before the signal is stable enough for usage. If you implement a custom callback, you may choose to compute your own estimations with less latency at the expense for less accuracy.

Feel free to use the source code provided to extend functionality or customize behavior!

## Initialization

These methods facilitate configuring internal settings for beacons.

```java
// In your MainActivity
@Override
protected void onCreate(Bundle savedInstanceState) {
  // ...
  NavisensCore core = new NavisensCore(DEV_KEY, this);
  NaviBeacon beacon = core.init(NaviBeacon.class)
      .addBeacon("/* Beacon UUID */", latitude, longitude, heading);
}
```

#### `NaviBeacon addBeacon(org.altbeacon.beacon.Identifier id, Double latitude, Double longitude, Double heading, Integer floor)`

This method allows you to add a beacon so the plugin will begin tracking and attempting to range for that beacon. The `id` is a format specified by AltBeacon. Here is an example of how to set an `Identifier`:

```java
  Identifier.fromUuid(UUID.fromString("01234567-89AB-CDEF-0123-456789ABCDEF"))
```

The `latitude`, `longitude`, `heading`, and `floor` parameters are nullable. If the heading is null, then only the latitude and longitude will be used. If either of latitude and longitude are null, then a location will not be initialized (but a heading may still be set). The default behavior will set the device location to the parameters provided. The floor will be set if there is a non-null floor number. If you wish to do custom actions (for example greeting the user upon entering beacon range), you should look at the [`setBeaconCallback`](#navibeacon-setbeaconcallbacknavibeaconcallback-navibeaconcallback) method.

#### `NaviBeacon setScanningPeriod(long period, long delay)`

Bluetooth scanning uses quite a bit of battery power. You can use this function to increase or decrease the frequency at which samples are taken. Make sure not to exceed the advertisement rate of your beacon, or you may get bad values. The `period` is the amount of active scanning time in milliseconds. The longer this is, the larger the window for finding a beacon, but battery usage is increased. The `delay` parameter specifies how long to wait between each scanning session. More delay will use less battery but decrease the accuracy of scanning. By default, the period and delay are both set to 500 milliseconds. Setting period to 100 and delay to 0, for example, will continuously scan for beacons and converge quicker, but drain battery faster.

#### `NaviBeacon setBeaconCallback(NaviBeaconCallback naviBeaconCallback)`

Use this to set a custom callback for whenever a beacon is ranged. See the [NaviBeaconCallback](#navibeaconcallback) section below for more details.

## Control

These methods allow you to control beacon scanning behavior while the app is running.

#### `void resumeScanning()`

If scanning was paused recently, continue scanning for nearby beacons.

#### `void pauseScanning()`

If currently scanning for beacons, stop doing so until `resumeScanning` is called.

## NaviBeaconCallback

This callback has only one function (below) that must be implemented. If you set a custom callback, simply implement this interface to receive the relevant events. The way this plugin is set up only allows you to set one callback instead of having multiple listeners.

#### `void onBeaconResponded(org.altbeacon.beacon.Beacon beacon, Double latitude, Double longitude, Double heading, Integer floor)`

This is the callback that must be implemented to execute custom behavior. The `beacon` parameter is an AltBeacon beacon and provides useful methods to receive information about the signal strength such as `getId1`, `getRSSI`, and `getDistance`. You may also choose to write custom estimation or otherwise scale the values as necessary to match your own beacon specificiations. The `latitude`, `longitude`, and `heading` parameters are the same ones that you provided when you called `addBeacon`. They are here purely for convenience and do not need to be used. Here is the code for the Default implementation:

```java
public class DefaultNaviBeaconCallback implements NaviBeaconCallback {
    boolean resetRequired = true;

    @Override
    public void onBeaconResponded(Beacon beacon, Double latitude, Double longitude, Double heading, Integer floor) {
        if (beacon.getDistance() < THRESHOLD) {                   // You must be within THRESHOLD distance from beacon
            if (core != null && resetRequired) {                  // Prevent continuously setting of the device location
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
            }
        } else if (beacon.getDistance() > THRESHOLD + MARGIN){    // User has left the beacon's margin
            resetRequired = true;
        }
    }
}
```
