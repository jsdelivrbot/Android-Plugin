# Navisens/Android-Plugin

This repo adds support for additional features on top of our [MotionDna SDK](https://github.com/navisens/Android-SDK) that can be used when building Android applications.

Make sure to complete setup of the Android SDK before proceeding with installing plugins.

All of the source code is provided, so if there are features that you need or would like changed, feel free to write your own plugins too, using the provided source as starter if necessary! If you are interested in publishing your own plugins, check out our tutorials [here](https://github.com/navisens/NaviDocs/tree/master/Tutorials).

## Setup

Plugins are compiled into your project by including the following repository in your project `build.gradle`

```gradle
allprojects {
    repositories {
        // ...

        maven {
            url 'https://raw.github.com/navisens/Android-Plugin/repositories'
        }
    }
}
```

Then simply import `NavisensCore` along with any desired plugins (see below for full list) by adding any dependency with the format

```gradle
dependencies {
    compile 'com.navisens:navisenscore:<version>'
    compile 'com.navisens:<plugin name>:<version>'
    
    // ...
}
```

For example, to import version 0.0.6 of our `NavisensMaps` plugin, use the following:

```gradle
    compile 'com.navisens:navisenscore:2.0.1'
    compile 'com.navisens:navisensmaps:0.0.6'
```

The source code for projects is also provided to allow for full customization.

## Core

You must have the **[Navisens Core](navisenscore)** set up before you can use any plugins.

## Plugins

The following is a list of all supported plugins. Special setup instructions and relevant stable version numbers linked.

#### [Navisens Maps](navisensmaps)

Quick and easy maps support built upon [leafletjs](http://leafletjs.com)

#### [NaviShare](navishare)

Wrapper which makes it easy to connect to servers and share location or even raw data between devices.

#### [NaviBeacon](navibeacon)

Adds support for syncing your location with beacons using the [Altbeacon](https://github.com/AltBeacon/android-beacon-library) specifications.

#### [NaviPoints](navipoints)

Adds a simple way of adding named points and initializing user location based on these points.
