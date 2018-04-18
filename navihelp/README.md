# NaviHelp

This plugin works with the NaviHelpMe tutorial to demonstrate how to use plugins and build your own!

The latest stable version is `1.2.1`, and it is built on top of NavisensCore `3.1`.

Add the following to your app's dependencies:

```gradle
  compile 'com.navisens:navihelp:1.2.1'
```

## Setup

1. You will need to add the latest version of `NaviShare` (we used 0.3.0) to use `NaviHelp`.

## API

`NaviHelp` is a simple space shooter style game, where you control your ship by walking around. It is designed to run with multiple people! The API below sets up some basic functions and provides areas of expansion, explained in detail in NaviHelpMe!

Feel free to use the source code provided to extend functionality or customize behavior!

## Initialization

There are no initialization methods. When adding it to the `core`, you must provide a reference to a `NaviShare` object! Since `NaviHelp` is a fragment, we can add it to our view by simply adding the fragment to the main content container.

```java
// In your MainActivity
@Override
protected void onCreate(Bundle savedInstanceState) {
  // ...
  NavisensCore core = new NavisensCore(DEV_KEY, this);
  NaviShare share = core.init(NaviShare.class);
  NaviHelp help = core.init(NaviHelp.class, share);
  getFragmentManager().beginTransaction().add(android.R.id.content, help).commit();
}
```

## Setup

These methods should be called when you create a new `NaviHelp` instance.

#### `NaviHelp setUsername(String name)`

If you're playing multiplayer, make sure to choose a memorable username! Current implementation limits the name to 32 characters.

#### `NaviHelp setColor(String hex)`

You get to customize your ship with your favorite color! You can provide any css-compatible non-transparent value. Below are a few examples of setting your ship to the color yellow:

```css
  help.setColor("#ffff00");
  help.setColor("rgb(255, 255, 0)");
  help.setColor("yellow");
  help.setColor("hsl(60, 100%, 50%)");
```

## Interface

These methods can be called after setup is complete.

#### `NaviHelp start()`

This tells the game that you have customized your ship (in the Setup above) and are ready to join the server!
