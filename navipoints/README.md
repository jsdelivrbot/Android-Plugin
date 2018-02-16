# NaviPoints

This plugin provides a simple implementation of the idea of keeping track of named points, and using these points to initialize the user's location easily and intuitively.

We named this plugin Points because it was originally inspired by the "Points Of Interest" or POIs concept of initializing user location. We simply extended the acronym to POInts to make it easier to remember.

The latest stable version is `0.4.0`, and it is built on top of NavisensCore `3.1`.

Add the following to your app's dependencies:

```gradle
    compile 'com.navisens:navipoints:0.4.0'
```

## Setup

1. No additional set up is required.

## API

`NaviPoints` allows you to add named points, with a latitude, longitude, and possibly heading and floor number. You can then set the user's location easily by the name of a point, or query for nearby points. If you have the `NavisensMaps` plugin in the same project, the `NavisensMaps` project will automagically display the `NaviPoints` that you have added and you can then use the UI provided in the `Maps` plugin to initialize the user location.

Feel free to use the source code provided to extend functionality or customize behavior!

## Initialization

There are no initialization methods. Just add it to the `core` to begin!

```java
// In your MainActivity
@Override
protected void onCreate(Bundle savedInstanceState) {
  // ...
  NavisensCore core = new NavisensCore(DEV_KEY, this);
  NaviPoints points = core.init(NaviPoints.class);
}
```

## Interface

These methods allow you to use the basic points support.

#### `void add(String id, double latitude, double longitude, Double heading, Integer floor)`

Begins keeping track of a point named `id`, and at location `latitude` and `longitude`. The `id` field must be unique. Optional `heading` and `floor` parameters are provided, in order to better set the user's location using the `setLocation` method call.

#### `void remove(String id)`

Stops tracking the point with the corresponding `id`, if it exists.

#### `void setLocation(String id)`

A simple way to set the user's location by the `id` of a loaded point.
