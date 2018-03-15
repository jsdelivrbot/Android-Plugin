# NaviBeacon

This plugin can be used to compute and send simple routes to other plugins.

The latest stable version is `0.3.2`, and it is built on top of NavisensCore `3.1`.

Add the following to your app's dependencies:

```gradle
    compile 'com.navisens:navigator:0.3.2'
```

## Setup

1. No setup required.

## API

`Navigator` is used to create routes between two points that can be rendered in the `NavisensMaps` plugin.

## Initialization

These functions facilitate configuring internal settings for routing.

```java
// In your MainActivity
@Override
protected void onCreate(Bundle savedInstanceState) {
// ...
  NavisensCore core = new NavisensCore(DEV_KEY, this);
  Navigator navigator = core.init(Navigator.class);
}
```

There are no other functions at the current time for configuring. Future releases will allow developers to set up navigable paths and maps so the algorithm can determine the best path.

## Usage

These functions are the core functionality of the Navigator plugin.

#### `List<NavigableNode> getRoute(NavigableNode from, NavigableNode to)`

Get a route between a from node and a to node. If no navigable paths have been configured, then a default algorithm will be used that constrains routes to 45- and 90-degree angle pathways.

#### `List<NavigableNode> getRoute(NavigableNode from, NavigableNode to, boolean publish)`

Functions the same way as `getRoute` above, but if `publish` is true, then also broadcast the computed pathway to all other plugins. If the maps plugin exists, then it will render the computed route.

