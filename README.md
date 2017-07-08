# Navisens/Android-Plugin

This repo adds support for additional features on top of our [MotionDna SDK](https://github.com/navisens/Android-SDK) that can be used when building Android applications.

Make sure to complete setup of the Android SDK before proceeding with installing plugins.

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

Then simply import the desired plugin (see below for full list) by adding any dependency with the format

```gradle
dependencies {
    compile 'com.navisens:<plugin name>:<version>'
    
    // ...
}
```

For example, to import version 0.0.2 of our MotionDna maps plugin, use the following:

```gradle
    compile 'com.navisens:motiondnamaps:0.0.2'
```

The source code for projects is also provided to allow for full customization.

## Plugins

The following is a list of all supported plugins. Special setup instructions and relevant stable version numbers linked.

#### [MotionDna maps](motiondnamaps)

Quick and easy maps support built upon [leafletjs](http://leafletjs.com)

#### Coming soon...

Summary info

#### Coming soon too...

Summary info again
