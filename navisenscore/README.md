# Navisens Core

This is the core upon which all other Navisens Plugins are built on. The `NavisensCore` is a required component of any project that uses any Navisens Plugins. For a list of plugins available, see [the main page.](https://github.com/navisens/Android-Plugin)

The latest stable version is `3.1.0`, and it is built on top of Android SDK version `1` (recommended Android SDK `1.2.0-SNAPSHOT`).

Add the following to your app's dependencies above all other plugins:

```gradle
    compile 'com.navisens:navisenscore:3.1.0'
    // compile '<other Navisens Plugins>'
```

## Setup

1. All setup is done on the [main plugins page](https://github.com/navisens/Android-Plugin).

## API

`NavisensCore` provides some useful utility functions to help structure your plugins.

## Setup

`NavisensCore` has one constructor, which must be called early in app creation. This sets up any framework required to get your other plugins up and running!

#### `NavisensCore(String devkey, Activity act)`

The `NavisensCore` requires your developer's key in order to function. Please contact us to apply for a developer's key from [here](https://developer.navisens.com) if you do not have one and wish to try out `Navisens MotionDna`. Also check out the quickstart if you aren't familiar with our SDK tools [here](https://github.com/navisens/NaviDocs).

The constructor also requires an activity reference. This is so we can retrieve the relevant permissions and other app settings, so please make sure to use a valid activity at the time of creation.

## Plugin Management and Other

The following methods are provided to help in managing your plugins. The basic operations allow add a plugin or stop a plugin. You should not need to call any other methods in the `NavisensCore`, as most plugins will make all relevant calls for you. Only call methods not listed if you absolutely know what you are doing :D.

The plugin structure is designed to be easily expandable. Check out our tutorials if you are curious in developing your own plugins!

#### `public <T extends NavisensPlugin> T init(Class<T> navisensPlugin, Object... args)`

This is your main entry point for adding and initializing any plugin. While it may seem complicated, this method is actually very simple. Simply pass in the class of the plugin you wish to initialize, and a new object will be returned for you. Internally, we also add multiple hooks for setting up the plugin, but you don't need to worry about that!

The following example assumes you have imported a plugin called `NavisensMaps` and would like to initialize an instance of it.
```java
// In your MainActivity
@Override
protected void onCreate(Bundle savedInstanceState) {
  // ...
  NavisensCore core = new NavisensCore(DEV_KEY, this);
  NavisensMaps maps = core.init(NavisensMaps.class)
                                .useLocalOnly()
                                .enableLocationSharing();
  // Notice that we can call methods of NavisensMaps directly after initializing!
}
```

#### `boolean stop()`

Stops the `NavisensCore`. Please make sure that all other plugins have completed running, otherwise this method will return false.

#### `boolean stop(NavisensPlugin plugin)`

Stop a specific `NavisensPlugin`. This will return false if for some reason the plugin you passed in can not be stopped, or it does not exist anymore (i.e. it was already stopped).

#### `boolean stopAll()`

Interrupts and stops all plugins, then shuts down the `NavisensCore`. This method will return false if any plugin refuses to stop. In this case, you may either call `stopAll` again at a later time, or simply quit your application.

#### `void setActivity(Activity act)`

If you ever need to change the activity, and you haven't started using any `NavisensPlugin`, either because you haven't added any yet, or the ones you've added haven't started up yet, then call this function. This will ensure maximum success when requesting for device-specific features and permissions once `NavisensCore` is first needed.
