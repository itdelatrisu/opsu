# [opsu!](https://itdelatrisu.github.io/opsu/)
**opsu!** is an unofficial open-source client for the rhythm game
[osu!](https://osu.ppy.sh/), written in Java using
[Slick2D](http://slick.ninjacave.com/) and  [LWJGL](http://lwjgl.org/)
(wrappers around OpenGL and OpenAL).

opsu! runs on Windows, OS X, and Linux.
A [libGDX port](https://github.com/fluddokt/opsu) also supports Android devices.

## Getting Started
Get a copy of opsu! from the [releases](https://github.com/itdelatrisu/opsu/releases)
page.  APK releases can be found [here](https://github.com/fluddokt/opsu/releases).

### Java Setup
The Java Runtime Environment (JRE) 7 or higher must be installed in order to run
opsu!. The download page is located [here](https://www.java.com/en/download/).

### Beatmaps
opsu! requires "beatmaps" to run, which contain the songs and gameplay data.
These can be downloaded directly through opsu! in the downloads menu, or
manually from the [osu! website](https://osu.ppy.sh/p/beatmaplist) (requires
registration) and mirror sites like [Bloodcat](http://bloodcat.com/osu/).
Place any manually downloaded beatmaps (in .osz format) in the `Import/`
directory for opsu! to unpack them automatically.

If osu! is installed, opsu! will attempt to read beatmaps from the osu!
installation location.  The beatmap directory can also be changed by setting
the "BeatmapDirectory" value in the generated configuration file.

### First Run
opsu! will parse all beatmaps when launched, which can take a while for the
first time.  If no beatmaps are found, the game will prompt you to download some
to get started.

Game settings can be changed in the options menu, accessed by clicking the
"Other Options" button in the song menu.  The "Music Offset" value will likely
need to be adjusted initially, or whenever hit objects are out of sync with the
music.

### Directory Structure
The following files and folders will be created by opsu! as needed:
* `.opsu.cfg`: The configuration file.  Most (but not all) of the settings can
  be changed through the options menu.
* `.opsu.db`: The beatmap cache database.
* `.opsu_scores.db`: The scores and player database.
* `.opsu.log`: The error log.  All critical errors displayed in-game are also
  logged to this file, and other warnings not shown are logged as well.
* `Songs/`: The beatmap directory (not used if an osu! installation is detected).
  The parser searches all of its subdirectories for .osu files to load.
* `Skins/`: The skins directory.  Each skin must be placed in a folder within
  this directory.  Any game resource (in `res/`) can be skinned by placing a
  file with the same name in a skin folder.  Skins can be selected in the
  options menu.
* `Replays/`: The replay directory.  Replays of each completed game are saved
  as .osr files, and can be viewed at a later time or shared with others.
* `Import/`: The import directory.  All beatmap packs (.osz) and skin
  packs (.osk) are unpacked to the proper location.  All replays (.osr) are
  moved to the replay directory, and their scores saved to the scores database.
* `Screenshots/`: The screenshot directory. Screenshots can be taken by
  pressing the F12 key.
* `Natives/`: The native libraries directory.
* `Temp/`: The temporary files directory.  Deleted when opsu! exits.

## Building
opsu! is distributed as both a [Maven](https://maven.apache.org/) and
[Gradle](https://gradle.org/) project.

### Maven
Maven builds are built to the `target` directory.
* To run the project, execute the Maven goal `compile`.
* To create a single executable jar, execute the Maven goal `package -Djar`.
  This will compile a jar to `target/opsu-${version}.jar` with the libraries,
  resources and natives packed inside the jar.
  * Setting the "XDG" property (`-DXDG=true`) will make the application use XDG
    folders under Unix-like operating systems.
  * Setting the "exclude" property to "ffmpeg" (`-Dexclude=ffmpeg`) will exclude
    FFmpeg shared libraries from the jar.

### Gradle
Gradle builds are built to the `build` directory.
* To run the project, execute the Gradle task `run`.
* To create a single executable jar, execute the Gradle task `jar`.
  This will compile a jar to `build/libs/opsu-${version}.jar` with the libraries,
  resources and natives packed inside the jar.
  * Setting the "XDG" property (`-PXDG=true`) will make the application use XDG
    folders under Unix-like operating systems.
  * Setting the "excludeFFmpeg" property (`-PexcludeFFmpeg`) will exclude
    FFmpeg shared libraries from the jar.

## Contributing
See the [contributing guidelines](CONTRIBUTING.md).

## Credits
This software was created by Jeffrey Han
([@itdelatrisu](https://github.com/itdelatrisu/)).  All game concepts and
designs are based on work by [osu!](https://osu.ppy.sh/) developer Dean Herbert
([@ppy](https://github.com/ppy)).  Other credits can be found [here](CREDITS.md).

## License
**This software is licensed under GNU GPL version 3.**
You can find the full text of the license [here](LICENSE).
