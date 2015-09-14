# [opsu!](http://itdelatrisu.github.io/opsu/)
**opsu!** is an unofficial open-source client for the rhythm game
[osu!](https://osu.ppy.sh/).  It is written in Java using
[Slick2D](http://slick.ninjacave.com/) and  [LWJGL](http://lwjgl.org/),
wrappers around the OpenGL and OpenAL libraries.

opsu! runs on Windows, OS X, and Linux platforms.
A [libGDX port](https://github.com/fluddokt/opsu) additionally supports Android
devices.

## Getting Started
Precompiled binaries for opsu! can be found on the
[releases](https://github.com/itdelatrisu/opsu/releases) page, with the latest
builds at the top.  APK releases can be found
[here](https://github.com/fluddokt/opsu/releases).

### Java Setup
The Java Runtime Environment (JRE) must be installed in order to run opsu!.
The download page is located [here](https://www.java.com/en/download/).

### Beatmaps
opsu! requires beatmaps to run, which are available for download on the
[osu! website](https://osu.ppy.sh/p/beatmaplist) and mirror sites such as
[osu!Mirror](https://osu.yas-online.net/) and [Bloodcat](http://bloodcat.com/osu/).
Beatmaps can also be downloaded directly through opsu! in the downloads menu.

If osu! is already installed, this application will attempt to load beatmaps
directly from the osu! program folder.  Otherwise, place beatmaps in the
generated `Songs` folder or set the "BeatmapDirectory" value in the generated
configuration file to the path of the root beatmap directory.

Note that beatmaps are typically delivered as OSZ files.  These can be extracted
with any ZIP tool, and opsu! will automatically extract them into the beatmap
folder if placed in the `SongPacks` directory.

### First Run
The "Music Offset" value will likely need to be adjusted when playing for the
first time, or whenever hit objects are out of sync with the music.  This and
other game options can be accessed by clicking the "Other Options" button in
the song menu.

### Directory Structure
The following files and folders will be created by opsu! as needed:
* `.opsu.cfg`: The configuration file.  Most (but not all) of the settings can
  be changed through the options menu.
* `.opsu.db`: The beatmap cache database.
* `.opsu_scores.db`: The scores database.
* `.opsu.log`: The error log.  All critical errors displayed in-game are also
  logged to this file, and other warnings not shown are logged as well.
* `Songs/`: The beatmap directory (not used if an osu! installation is detected).
  The parser searches all of its subdirectories for .osu files to load.
* `SongPacks/`: The beatmap pack directory.  The unpacker extracts all .osz
  files within this directory to the beatmap directory.
* `Skins/`: The skins directory.  Each skin must be placed in a folder within
  this directory.  Any game resource (in `res/`) can be skinned by placing a
  file with the same name in a skin folder.  Skins can be selected in the
  options menu.
* `Screenshots/`: The screenshot directory. Screenshots can be taken by
  pressing the F12 key.
* `Replays/`: The replay directory.  Replays of each completed game are saved
  as .osr files, and can be viewed at a later time or shared with others.
* `ReplayImport/`: The replay import directory.  The importer moves all .osr
  files within this directory to the replay directory and saves the scores in
  the scores database.  Replays can be imported from osu! as well as opsu!.
* `Natives/`: The native libraries directory.

## Building
opsu! is distributed as both a [Maven](https://maven.apache.org/) and
[Gradle](https://gradle.org/) project.

### Maven
Maven builds are built to the `target` directory.
* To run the project, execute the Maven goal `compile`.
* To create a single executable jar, execute the Maven goal `package -Djar`.
  This will compile a jar to `target/opsu-${version}.jar` with the libraries,
  resources and natives packed inside the jar.  Setting the "XDG" property
  (`-DXDG=true`) will make the application use XDG folders under Unix-like
  operating systems.

### Gradle
Gradle builds are built to the `build` directory.
* To run the project, execute the Gradle task `run`.
* To create a single executable jar, execute the Gradle task `jar`.
  This will compile a jar to `build/libs/opsu-${version}.jar` with the libraries,
  resources and natives packed inside the jar.  Setting the "XDG" property
  (`-PXDG=true`) will make the application use XDG folders under Unix-like
  operating systems.

## Credits
This software was created by Jeffrey Han 
([@itdelatrisu](https://github.com/itdelatrisu/)).  All game concepts and
designs are based on work by osu! developer Dean Herbert.  Other credits can
be found [here](CREDITS.md).

## License
**This software is licensed under GNU GPL version 3.**
You can find the full text of the license [here](LICENSE).
