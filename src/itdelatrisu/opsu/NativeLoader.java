/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014, 2015 Jeffrey Han
 *
 * opsu! is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opsu! is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with opsu!.  If not, see <http://www.gnu.org/licenses/>.
 */

package itdelatrisu.opsu;

import itdelatrisu.opsu.audio.GStreamerPlayer;
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.audio.SlickPlayer;
import org.gstreamer.Gst;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static itdelatrisu.opsu.Utils.copyDirectory;
import static itdelatrisu.opsu.Utils.extractResource;

public class NativeLoader {
	public static final Name osName = getOsName();
	public static final Arch osArch = getOsArch();

	public enum Name {
		WINDOWS,
		DARWIN,
		UNIX,
		UNSUPPORTED
	}

	public enum Arch {
		x86,
		x86_64,
		UNSUPPORTED
	}

	// reversed list order to satisfy dependency
	private static final String[] WINDOWS_DEPENDENCY = {
			// "lwjgl",
			// "openal",
			"libintl-8",
			"libwinpthread-1",
			"libglib-2.0-0",
			"libgmodule-2.0-0",
			"libz",
			"libffi-6",
			"libgobject-2.0-0",
			"libgio-2.0-0",
			"libxml2-2",
			"libgstreamer-0.10-0",
			"libgstinterfaces-0.10-0",
			"libgstpbutils-0.10-0",
			"libgstbase-0.10-0",
			"libgstaudio-0.10-0",
			"libgsttag-0.10-0",
			"libgstriff-0.10-0",
			"libogg-0",
			"liborc-0.4-0",
			"libgstcontroller-0.10-0",
			"libgcc_s_sjlj-1",
			"libstdc++-6"
	};

	private static final String[] DARWIN_DEPENDENCY = {
			// TODO
	};

	// Linux libraries are referenced by the convention lib**name**.so and loaded based on the name
	private static final String[] UNIX_DEPENDENCY = {
			"gstreamer-0.10",
			"gstbase-0.10",
			"gstpbutils-0.10",
			"gstinterfaces-0.10",
			"gstcontroller-0.10",
			"gsttag-0.10",
			"gstaudio-0.10",
			"gstriff-0.10",
			"SoundTouch",
			"mad",
			"ogg",
			"vorbisenc",
			"vorbis"
	};

	public static void init() {
		String absoluteLibPath;
		String libPath = null;
		String[] libraries;

		switch (osName) {
			case WINDOWS: {
				libraries = WINDOWS_DEPENDENCY;
				switch (osArch) {
					case x86:
						libPath = "native\\win32";
						break;
					case x86_64:
						libPath = "native\\win64";
						break;
					case UNSUPPORTED:
					default:
						// throw new RuntimeException("Unsupported operating system");
				}
				break;
			}

			case DARWIN: {
				libraries = DARWIN_DEPENDENCY;
				switch (osArch) {
					case x86:
					case x86_64:
						libPath = "native/darwin";
						break;
					case UNSUPPORTED:
					default:
						// throw new RuntimeException("Unsupported operating system.");
				}
				break;
			}
			case UNIX: {
				libraries = UNIX_DEPENDENCY;
				switch (osArch) {
					case x86:
						libPath = "native/linux32";
						break;
					case x86_64:
						libPath = "native/linux64";
						break;
					case UNSUPPORTED:
					default:
						// throw new RuntimeException("Unsupported operating system.");
				}
				break;
			}
			case UNSUPPORTED:
			default:
				throw new RuntimeException("Unsupported operating system.");
		}

		absoluteLibPath = System.getProperty("user.dir") + File.separator + libPath;

		// getting resource folder
		// @author (user1079877) (http://stackoverflow.com/a/20073154)
		File jarFile = new File(Opsu.class.getProtectionDomain().getCodeSource().getLocation().getPath());

		// run with JAR file
		if (jarFile.isFile()) {
			JarFile jar = null;
			try {
				jar = new JarFile(jarFile);
			} catch (IOException e) {} // never happens
			Enumeration<JarEntry> entries = jar.entries(); // gives ALL entries in jar
			while (entries.hasMoreElements()) {
				final String name = entries.nextElement().getName();
				if (name.startsWith(libPath + "/")) { // filter according to the path
					extractResource("/" + name, name);
				}
			}
			try {
				jar.close();
			} catch (IOException e) {
				ErrorHandler.error(null, e, true);
			}
		} else {
			// run with IDE
			final URL url = Opsu.class.getResource("/" + libPath);
			if (url != null) {
				try {
					File libDirectory = new File(url.toURI());
					copyDirectory(libDirectory, absoluteLibPath);
				} catch (URISyntaxException e) {}
			}
		}

		// set library environment
		try {
			addLibraryPath(absoluteLibPath);
			if (System.getProperty("jna.library.path") == null)
				System.setProperty("jna.library.path", absoluteLibPath);
		} catch (Exception e) {
			ErrorHandler.error(null, e, true);
		}

		// load libraries
		for (String library : libraries) {
			System.loadLibrary(library);
		}

		// initialize gstreamer
		Gst.init("MusicPlayer", new String[]{"--gst-plugin-path=" + libPath + File.separator +  "plugins"});
		if (GStreamerPlayer.check()) {
			MusicController.player = new GStreamerPlayer();
		} else
			MusicController.player = new SlickPlayer();
	}

	private static Name getOsName() {
		String osName = System.getProperty("os.name");
		if (osName.toLowerCase().startsWith("windows")) {
			return Name.WINDOWS;
		} else if (osName.equalsIgnoreCase("Mac OS X") || osName.equalsIgnoreCase("Darwin")) {
			return Name.DARWIN;
		} else if (osName.equalsIgnoreCase("Linux") || osName.contains("BSD")) {
			return Name.UNIX;
		}

		return Name.UNSUPPORTED;
	}

	private static Arch getOsArch() {
		String arch = System.getProperty("os.arch");
		if (arch.equalsIgnoreCase("i386") || arch.equalsIgnoreCase("i686") || arch.equalsIgnoreCase("x86")) {
			return Arch.x86;
		} else if (arch.equalsIgnoreCase("x86_64") || arch.equalsIgnoreCase("x64") || arch.equalsIgnoreCase("amd64")
				|| arch.equalsIgnoreCase("universal")) {
			return Arch.x86_64;
		}
		return Arch.UNSUPPORTED;
	}

	/**
	 * Adds the specified path to the java library path
	 *
	 * @param pathToAdd the path to add
	 * @throws Exception
	 * @author Fahd Shariff (http://fahdshariff.blogspot.ru/2011/08/changing-java-library-path-at-runtime.html)
	 */
	private static void addLibraryPath(String pathToAdd) throws Exception {
		final Field usrPathsField = ClassLoader.class.getDeclaredField("usr_paths");
		usrPathsField.setAccessible(true);

		// get array of paths
		final String[] paths = (String[]) usrPathsField.get(null);

		// check if the path to add is already present
		for (String path : paths) {
			if (path.equals(pathToAdd)) {
				return;
			}
		}

		//add the new path
		final String[] newPaths = new String[paths.length + 1];
		newPaths[0] = pathToAdd;
		System.arraycopy(paths, 0, newPaths, 1, paths.length);
		usrPathsField.set(null, newPaths);
	}
}
