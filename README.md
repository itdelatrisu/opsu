# [opsu!](http://itdelatrisu.github.io/opsu/)
**opsu!** is an unofficial open-source client for [osu!](https://osu.ppy.sh/),
a rhythm game based on popular commercial games such as *Ouendan* and
*Elite Beat Agents*.  It is written in Java using [Slick2D](http://slick.ninjacave.com/)
and [LWJGL](http://lwjgl.org/), wrappers around the OpenGL and OpenAL libraries.

opsu! runs on Windows, OS X, and Linux platforms. A [libGDX port](https://github.com/fluddokt/opsu)
additionally supports Android devices.

## Getting Started
Precompiled binaries for opsu! can be found on the
[releases](https://github.com/itdelatrisu/opsu/releases) page, with the latest
builds at the top.  APK releases can be found [here](https://github.com/fluddokt/opsu/releases).

### Java Setup
The Java Runtime Environment (JRE) must be installed in order to run opsu!.
The download page is located [here](https://www.java.com/en/download/).

### Beatmaps
opsu! requires beatmaps to run, which are available for download on the
[osu! website](https://osu.ppy.sh/p/beatmaplist) and mirror sites such as
[osu!Mirror](https://osu.yas-online.net/) or [Bloodcat](http://bloodcat.com/osu/).
Beatmaps can also be downloaded directly through opsu! in the downloads menu.

If osu! is already installed, this application will attempt to load songs
directly from the osu! program folder.  Otherwise, place songs in the generated
`Songs` folder or set the `BeatmapDirectory` value in the generated
configuration file to the path of the root song directory.

Note that beatmaps are typically delivered as OSZ files.  These can be extracted
with any ZIP tool, and opsu! will automatically extract them into the songs
folder if placed in the `SongPacks` directory.

### First Run
The `Music Offset` value will likely need to be adjusted when playing for the
first time, or whenever hit objects are out of sync with the music.  This and
other game options can be accessed by clicking the "Other Options" button in
the song menu.

## Building
opsu! is distributed as both a Maven and Gradle project.

### Gradle
Gradle builds are built to the `build` directory.
* To run the project, execute the Gradle task `run`.
* To create a single executable JAR file, execute the Gradle task
  `build`. This will compile a jar to `build/libs/opsu-${version}.jar` with
  the libraries, resources and natives packed inside the jar.
  
### Maven
Maven builds are built to the `target` directory.
* To run the project, execute the Maven goal `compile exec:exec`.
* To create a single executable JAR file, execute the Maven goal
  `install -Djar`.  This will link the LWJGL native libraries using a
  [modified version](https://github.com/itdelatrisu/JarSplicePlus) of
  [JarSplice](http://ninjacave.com/jarsplice), which is included in the
  `tools` directory in both its original and modified forms.  The resulting
  file will be located in `target/opsu-${version}-runnable.jar`.

## Credits
This software was created by Jeffrey Han 
([@itdelatrisu](https://github.com/itdelatrisu/)).  All game concepts and
designs are based on work by osu! developer Dean Herbert.  Other credits can
be found [here](CREDITS.md).

## License
**This software is licensed under GNU GPL version 3.**
You can find the full text of the license [here](LICENSE).
