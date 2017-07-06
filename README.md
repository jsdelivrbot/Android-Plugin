# Navisens/Android-Plugin

This repo adds support for additional features on top of our [MotionDna SDK](https://github.com/navisens/Android-SDK) that can be used when building Android applications.

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

For example, to import our MotionDna maps support, use the following:

```gradle
    compile 'com.navisens:motiondnaapi:0.0.0'
```

The source code for projects are also provided to allow for full customization.

## Plugins

The following is a list of all supported plugins. Special setup instructions and relevant stable version numbers linked.

#### [MotionDna maps](motiondnamaps)

Quick and easy maps support

#### Coming soon...

Summary info

#### Coming soon too...

Summary info again
