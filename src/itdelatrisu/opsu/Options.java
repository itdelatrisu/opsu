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

import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.beatmap.Beatmap;
import itdelatrisu.opsu.skins.Skin;
import itdelatrisu.opsu.skins.SkinLoader;
import itdelatrisu.opsu.ui.UI;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.lwjgl.input.Keyboard;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.util.ClasspathLocation;
import org.newdawn.slick.util.FileSystemLocation;
import org.newdawn.slick.util.Log;
import org.newdawn.slick.util.ResourceLoader;

/**
 * Handles all user options.
 */
public class Options {
	/** The config directory. */
	private static final File CONFIG_DIR = getXDGBaseDir("XDG_CONFIG_HOME", ".config");

	/** The data directory. */
	private static final File DATA_DIR = getXDGBaseDir("XDG_DATA_HOME", ".local/share");

	/** File for logging errors. */
	public static final File LOG_FILE = new File(CONFIG_DIR, ".opsu.log");

	/** File for storing user options. */
	private static final File OPTIONS_FILE = new File(CONFIG_DIR, ".opsu.cfg");

	/** Beatmap directories (where to search for files). */
	private static final String[] BEATMAP_DIRS = {
		"C:/Program Files (x86)/osu!/Songs/",
		"C:/Program Files/osu!/Songs/",
		new File(DATA_DIR, "Songs/").getPath()
	};

	/** Skin directories (where to search for skins). */
	private static final String[] SKIN_ROOT_DIRS = {
		"C:/Program Files (x86)/osu!/Skins/",
		"C:/Program Files/osu!/Skins/",
		new File(DATA_DIR, "Skins/").getPath()
	};

	/** Cached beatmap database name. */
	public static final File BEATMAP_DB = new File(DATA_DIR, ".opsu.db");

	/** Score database name. */
	public static final File SCORE_DB = new File(DATA_DIR, ".opsu_scores.db");

	/** Font file name. */
	public static final String FONT_NAME = "DroidSansFallback.ttf";

	/** Version file name. */
	public static final String VERSION_FILE = "version";

	/** Repository address. */
	public static final URI REPOSITORY_URI = URI.create("https://github.com/itdelatrisu/opsu");

	/** Issue reporting address. */
	public static final String ISSUES_URL = "https://github.com/itdelatrisu/opsu/issues/new?title=%s&body=%s";

	/** Address containing the latest version file. */
	public static final String VERSION_REMOTE = "https://raw.githubusercontent.com/itdelatrisu/opsu/gh-pages/version";

	/** The beatmap directory. */
	private static File beatmapDir;

	/** The OSZ archive directory. */
	private static File oszDir;

	/** The screenshot directory (created when needed). */
	private static File screenshotDir;

	/** The replay directory (created when needed). */
	private static File replayDir;

	/** The root skin directory. */
	private static File skinRootDir;

	/** Port binding. */
	private static int port = 49250;

	/**
	 * Returns the directory based on the XDG base directory specification for
	 * Unix-like operating systems, only if the system property "XDG" has been defined.
	 * @param env the environment variable to check (XDG_*_*)
	 * @param fallback the fallback directory relative to ~home
	 * @return the XDG base directory, or the working directory if unavailable
	 */
	private static File getXDGBaseDir(String env, String fallback) {
		if (System.getProperty("XDG") == null)
			return new File("./");

		String OS = System.getProperty("os.name").toLowerCase();
		if (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0) {
			String rootPath = System.getenv(env);
			if (rootPath == null) {
				String home = System.getProperty("user.home");
				if (home == null)
					return new File("./");
				rootPath = String.format("%s/%s", home, fallback);
			}
			File dir = new File(rootPath, "opsu");
			if (!dir.isDirectory())
				dir.mkdir();
			return dir;
		} else
			return new File("./");
	}

	/**
	 * The theme song string:
	 * {@code filename,title,artist,length(ms)}
	 */
	private static String themeString = "theme.ogg,On the Bach,Jingle Punks,66000";

	/** Game options. */
	public enum GameOption {
		NULL (null, null),
		SCREEN_RESOLUTION ("Screen Resolution", "Restart (Ctrl+Shift+F5) to apply resolution changes.") {
			@Override
			public String getValueString() { return resolution.toString(); }

			@Override
			public void click(GameContainer container) {
				do {
					resolution = resolution.next();
				} while (resolution != Resolution.RES_800_600 &&
						(container.getScreenWidth() < resolution.getWidth() ||
						 container.getScreenHeight() < resolution.getHeight()));
			}
		},
//		FULLSCREEN ("Fullscreen Mode", "Restart to apply changes.", false),
		SKIN ("Skin", "Restart (Ctrl+Shift+F5) to apply skin changes.") {
			@Override
			public String getValueString() { return skinName; }

			@Override
			public void click(GameContainer container) {
				skinDirIndex = (skinDirIndex + 1) % skinDirs.length;
				skinName = skinDirs[skinDirIndex];
			}
		},
		TARGET_FPS ("Frame Limiter", "Higher values may cause high CPU usage.") {
			@Override
			public String getValueString() {
				return String.format((getTargetFPS() == 60) ? "%dfps (vsync)" : "%dfps", getTargetFPS());
			}

			@Override
			public void click(GameContainer container) {
				targetFPSindex = (targetFPSindex + 1) % targetFPS.length;
				container.setTargetFrameRate(getTargetFPS());
				container.setVSync(getTargetFPS() == 60);
			}
		},
		MASTER_VOLUME ("Master Volume", "Global volume level.", 35, 0, 100) {
			@Override
			public void drag(GameContainer container, int d) {
				super.drag(container, d);
				container.setMusicVolume(getMasterVolume() * getMusicVolume());
			}
		},
		MUSIC_VOLUME ("Music Volume", "Volume of music.", 80, 0, 100) {
			@Override
			public void drag(GameContainer container, int d) {
				super.drag(container, d);
				container.setMusicVolume(getMasterVolume() * getMusicVolume());
			}
		},
		EFFECT_VOLUME ("Effect Volume", "Volume of menu and game sounds.", 70, 0, 100),
		HITSOUND_VOLUME ("Hit Sound Volume", "Volume of hit sounds.", 30, 0, 100),
		MUSIC_OFFSET ("Music Offset", "Adjust this value if hit objects are out of sync.", -75, -500, 500) {
			@Override
			public String getValueString() { return String.format("%dms", val); }
		},
		SCREENSHOT_FORMAT ("Screenshot Format", "Press F12 to take a screenshot.") {
			@Override
			public String getValueString() { return screenshotFormat[screenshotFormatIndex].toUpperCase(); }

			@Override
			public void click(GameContainer container) { screenshotFormatIndex = (screenshotFormatIndex + 1) % screenshotFormat.length; }
		},
		SHOW_FPS ("Show FPS Counter", "Show an FPS counter in the bottom-right hand corner.", true),
		SHOW_HIT_LIGHTING ("Show Hit Lighting", "Adds an effect behind hit explosions.", true),
		SHOW_COMBO_BURSTS ("Show Combo Bursts", "A character image is displayed at combo milestones.", true),
		SHOW_PERFECT_HIT ("Show Perfect Hits", "Whether to show perfect hit result bursts (300s, slider ticks).", true),
		SHOW_FOLLOW_POINTS ("Show Follow Points", "Whether to show follow points between hit objects.", true),
		NEW_CURSOR ("Enable New Cursor", "Use the new cursor style (may cause higher CPU usage).", true) {
			@Override
			public void click(GameContainer container) {
				super.click(container);
				UI.getCursor().reset();
			}
		},
		DYNAMIC_BACKGROUND ("Enable Dynamic Backgrounds", "The song background will be used as the main menu background.", true),
		BACKGROUND_DIM ("Background Dim", "Percentage to dim the background image during gameplay.", 50, 0, 100),
		FORCE_DEFAULT_PLAYFIELD ("Force Default Playfield", "Override the song background with the default playfield background.", false),
		IGNORE_BEATMAP_SKINS ("Ignore All Beatmap Skins", "Never use skin element overrides provided by beatmaps.", false),
		FIXED_CS ("Fixed Circle Size (CS)", "Determines the size of circles and sliders.", 0, 0, 100) {
			@Override
			public String getValueString() { return (val == 0) ? "Disabled" : String.format("%.1f", val / 10f); }
		},
		FIXED_HP ("Fixed HP Drain Rate (HP)", "Determines the rate at which health decreases.", 0, 0, 100) {
			@Override
			public String getValueString() { return (val == 0) ? "Disabled" : String.format("%.1f", val / 10f); }
		},
		FIXED_AR ("Fixed Approach Rate (AR)", "Determines how long hit circles stay on the screen.", 0, 0, 100) {
			@Override
			public String getValueString() { return (val == 0) ? "Disabled" : String.format("%.1f", val / 10f); }
		},
		FIXED_OD ("Fixed Overall Difficulty (OD)", "Determines the time window for hit results.", 0, 0, 100) {
			@Override
			public String getValueString() { return (val == 0) ? "Disabled" : String.format("%.1f", val / 10f); }
		},
		LOAD_VERBOSE ("Show Detailed Loading Progress", "Display more specific loading information in the splash screen.", false),
		CHECKPOINT ("Track Checkpoint", "Press Ctrl+L while playing to load a checkpoint, and Ctrl+S to set one.", 0, 0, 3599) {
			@Override
			public String getValueString() {
				return (val == 0) ? "Disabled" : String.format("%02d:%02d",
						TimeUnit.SECONDS.toMinutes(val),
						val - TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(val)));
			}
		},
		DISABLE_SOUNDS ("Disable All Sound Effects", "May resolve Linux sound driver issues.  Requires a restart.",
				(System.getProperty("os.name").toLowerCase().indexOf("linux") > -1)),
		KEY_LEFT ("Left Game Key", "Select this option to input a key.") {
			@Override
			public String getValueString() { return Keyboard.getKeyName(getGameKeyLeft()); }
		},
		KEY_RIGHT ("Right Game Key", "Select this option to input a key.") {
			@Override
			public String getValueString() { return Keyboard.getKeyName(getGameKeyRight()); }
		},
		SHOW_UNICODE ("Prefer Non-English Metadata", "Where available, song titles will be shown in their native language.", false) {
			@Override
			public void click(GameContainer container) {
				super.click(container);
				if (bool) {
					try {
						Utils.FONT_LARGE.loadGlyphs();
						Utils.FONT_MEDIUM.loadGlyphs();
						Utils.FONT_DEFAULT.loadGlyphs();
					} catch (SlickException e) {
						Log.warn("Failed to load glyphs.", e);
					}
				}
			}
		},
		ENABLE_THEME_SONG ("Enable Theme Song", "Whether to play the theme song upon starting opsu!", true),
		SHOW_HIT_ERROR_BAR ("Show Hit Error Bar", "Shows precisely how accurate you were with each hit.", false),
		LOAD_HD_IMAGES ("Load HD Images", "Loads HD (@2x) images when available. Increases memory usage and loading times.", true),
		DISABLE_MOUSE_WHEEL ("Disable mouse wheel in play mode", "During play, you can use the mouse wheel to adjust the volume and pause the game.\nThis will disable that functionality.", false),
		DISABLE_MOUSE_BUTTONS ("Disable mouse buttons in play mode", "This option will disable all mouse buttons.\nSpecifically for people who use their keyboard to click.", false);

		/** Option name. */
		private String name;

		/** Option description. */
		private String description;

		/** The boolean value for the option (if applicable). */
		protected boolean bool;

		/** The integer value for the option (if applicable). */
		protected int val;

		/** The upper and lower bounds on the integer value (if applicable). */
		private int max, min;

		/** Whether or not this is a numeric option. */
		private boolean isNumeric;

		/**
		 * Constructor.
		 * @param name the option name
		 * @param description the option description
		 */
		GameOption(String name, String description) {
			this.name = name;
			this.description = description;
		}

		/**
		 * Constructor.
		 * @param name the option name
		 * @param description the option description
		 * @param value the default boolean value
		 */
		GameOption(String name, String description, boolean value) {
			this(name, description);
			this.bool = value;
			this.isNumeric = false;
		}

		/**
		 * Constructor.
		 * @param name the option name
		 * @param description the option description
		 * @param value the default integer value
		 */
		GameOption(String name, String description, int value, int min, int max) {
			this(name, description);
			this.val = value;
			this.min = min;
			this.max = max;
			this.isNumeric = true;
		}

		/**
		 * Returns the option name.
		 * @return the name string
		 */
		public String getName() { return name; }

		/**
		 * Returns the option description.
		 * @return the description string
		 */
		public String getDescription() { return description; }

		/**
		 * Returns the boolean value for the option, if applicable.
		 * @return the boolean value
		 */
		public boolean getBooleanValue() { return bool; }

		/**
		 * Returns the integer value for the option, if applicable.
		 * @return the integer value
		 */
		public int getIntegerValue() { return val; }

		/**
		 * Sets the boolean value for the option.
		 * @param value the new boolean value
		 */
		public void setValue(boolean value) { this.bool = value; }

		/**
		 * Sets the integer value for the option.
		 * @param value the new integer value
		 */
		public void setValue(int value) { this.val = value; }

		/**
		 * Returns the value of the option as a string (via override).
		 * <p>
		 * By default, this returns "{@code val}%" if this is an numeric option,
		 * or "Yes" or "No" based on the {@code bool} field otherwise.
		 * @return the value string
		 */
		public String getValueString() {
			if (isNumeric)
				return String.format("%d%%", val);
			else
				return (bool) ? "Yes" : "No";
		}

		/**
		 * Processes a mouse click action (via override).
		 * <p>
		 * By default, this inverts the current {@code bool} field.
		 * @param container the game container
		 */
		public void click(GameContainer container) { bool = !bool; }

		/**
		 * Processes a mouse drag action (via override).
		 * <p>
		 * By default, if this is a numeric option, the {@code val} field will
		 * be shifted by {@code d} within the given bounds.
		 * @param container the game container
		 * @param d the dragged distance (modified by multiplier)
		 */
		public void drag(GameContainer container, int d) {
			if (isNumeric)
				val = Utils.getBoundedValue(val, d, min, max);
		}
	};

	/** Screen resolutions. */
	private enum Resolution {
		RES_800_600 (800, 600),
		RES_1024_600 (1024, 600),
		RES_1024_768 (1024, 768),
		RES_1280_720 (1280, 720),
		RES_1280_800 (1280, 800),
		RES_1280_960 (1280, 960),
		RES_1280_1024 (1280, 1024),
		RES_1366_768 (1366, 768),
		RES_1440_900 (1440, 900),
		RES_1600_900 (1600, 900),
		RES_1600_1200 (1600, 1200),
		RES_1680_1050 (1680, 1050),
		RES_1920_1080 (1920, 1080),
		RES_1920_1200 (1920, 1200),
		RES_2560_1440 (2560, 1440),
		RES_2560_1600 (2560, 1600),
		RES_3840_2160 (3840, 2160);

		/** Screen dimensions. */
		private int width, height;

		/** Enum values. */
		private static Resolution[] values = Resolution.values();

		/**
		 * Constructor.
		 * @param width the screen width
		 * @param height the screen height
		 */
		Resolution(int width, int height) {
			this.width = width;
			this.height = height;
		}

		/**
		 * Returns the screen width.
		 */
		public int getWidth() { return width; }

		/**
		 * Returns the screen height.
		 */
		public int getHeight() { return height; }

		/**
		 * Returns the next (larger) Resolution.
		 */
		public Resolution next() { return values[(this.ordinal() + 1) % values.length]; }

		@Override
		public String toString() { return String.format("%sx%s", width, height); }
	}

	/** Current screen resolution. */
	private static Resolution resolution = Resolution.RES_1024_768;

	/** The available skin directories. */
	private static String[] skinDirs;

	/** The index in the skinDirs array. */
	private static int skinDirIndex = 0;

	/** The name of the skin. */
	private static String skinName = "Default";

	/** The current skin. */
	private static Skin skin;

	/** Frame limiters. */
	private static final int[] targetFPS = { 60, 120, 240 };

	/** Index in targetFPS[] array. */
	private static int targetFPSindex = 0;

	/** Screenshot file formats. */
	private static String[] screenshotFormat = { "png", "jpg", "bmp" };

	/** Index in screenshotFormat[] array. */
	private static int screenshotFormatIndex = 0;

	/** Left and right game keys. */
	private static int
		keyLeft  = Keyboard.KEY_NONE,
		keyRight = Keyboard.KEY_NONE;

	// This class should not be instantiated.
	private Options() {}

	/**
	 * Returns the target frame rate.
	 * @return the target FPS
	 */
	public static int getTargetFPS() { return targetFPS[targetFPSindex]; }

	/**
	 * Sets the target frame rate to the next available option, and sends a
	 * bar notification about the action.
	 * @param container the game container
	 */
	public static void setNextFPS(GameContainer container) {
		GameOption.TARGET_FPS.click(container);
		UI.sendBarNotification(String.format("Frame limiter: %s", GameOption.TARGET_FPS.getValueString()));
	}

	/**
	 * Returns the master volume level.
	 * @return the volume [0, 1]
	 */
	public static float getMasterVolume() { return GameOption.MASTER_VOLUME.getIntegerValue() / 100f; }

	/**
	 * Sets the master volume level (if within valid range).
	 * @param container the game container
	 * @param volume the volume [0, 1]
	 */
	public static void setMasterVolume(GameContainer container, float volume) {
		if (volume >= 0f && volume <= 1f) {
			GameOption.MASTER_VOLUME.setValue((int) (volume * 100f));
			MusicController.setVolume(getMasterVolume() * getMusicVolume());
		}
	}

	/**
	 * Returns the default music volume.
	 * @return the volume [0, 1]
	 */
	public static float getMusicVolume() { return GameOption.MUSIC_VOLUME.getIntegerValue() / 100f; }

	/**
	 * Returns the default sound effect volume.
	 * @return the sound volume [0, 1]
	 */
	public static float getEffectVolume() { return GameOption.EFFECT_VOLUME.getIntegerValue() / 100f; }

	/**
	 * Returns the default hit sound volume.
	 * @return the hit sound volume [0, 1]
	 */
	public static float getHitSoundVolume() { return GameOption.HITSOUND_VOLUME.getIntegerValue() / 100f; }

	/**
	 * Returns the music offset time.
	 * @return the offset (in milliseconds)
	 */
	public static int getMusicOffset() { return GameOption.MUSIC_OFFSET.getIntegerValue(); }

	/**
	 * Returns the screenshot file format.
	 * @return the file extension ("png", "jpg", "bmp")
	 */
	public static String getScreenshotFormat() { return screenshotFormat[screenshotFormatIndex]; }

	/**
	 * Sets the container size and makes the window borderless if the container
	 * size is identical to the screen resolution.
	 * <p>
	 * If the configured resolution is larger than the screen size, the smallest
	 * available resolution will be used.
	 * @param app the game container
	 */
	public static void setDisplayMode(Container app) {
		int screenWidth = app.getScreenWidth();
		int screenHeight = app.getScreenHeight();

		// check for larger-than-screen dimensions
		if (screenWidth < resolution.getWidth() || screenHeight < resolution.getHeight())
			resolution = Resolution.RES_800_600;

		try {
			app.setDisplayMode(resolution.getWidth(), resolution.getHeight(), false);
		} catch (SlickException e) {
			ErrorHandler.error("Failed to set display mode.", e, true);
		}

		// set borderless window if dimensions match screen size
		boolean borderless = (screenWidth == resolution.getWidth() && screenHeight == resolution.getHeight());
		System.setProperty("org.lwjgl.opengl.Window.undecorated", Boolean.toString(borderless));
	}

//	/**
//	 * Returns whether or not fullscreen mode is enabled.
//	 * @return true if enabled
//	 */
//	public static boolean isFullscreen() { return fullscreen; }

	/**
	 * Returns whether or not the FPS counter display is enabled.
	 * @return true if enabled
	 */
	public static boolean isFPSCounterEnabled() { return GameOption.SHOW_FPS.getBooleanValue(); }

	/**
	 * Returns whether or not hit lighting effects are enabled.
	 * @return true if enabled
	 */
	public static boolean isHitLightingEnabled() { return GameOption.SHOW_HIT_LIGHTING.getBooleanValue(); }

	/**
	 * Returns whether or not combo burst effects are enabled.
	 * @return true if enabled
	 */
	public static boolean isComboBurstEnabled() { return GameOption.SHOW_COMBO_BURSTS.getBooleanValue(); }

	/**
	 * Returns the port number to bind to.
	 * @return the port
	 */
	public static int getPort() { return port; }

	/**
	 * Returns whether or not the new cursor type is enabled.
	 * @return true if enabled
	 */
	public static boolean isNewCursorEnabled() { return GameOption.NEW_CURSOR.getBooleanValue(); }

	/**
	 * Returns whether or not the main menu background should be the current track image.
	 * @return true if enabled
	 */
	public static boolean isDynamicBackgroundEnabled() { return GameOption.DYNAMIC_BACKGROUND.getBooleanValue(); }

	/**
	 * Returns whether or not to show perfect hit result bursts.
	 * @return true if enabled
	 */
	public static boolean isPerfectHitBurstEnabled() { return GameOption.SHOW_PERFECT_HIT.getBooleanValue(); }

	/**
	 * Returns whether or not to show follow points.
	 * @return true if enabled
	 */
	public static boolean isFollowPointEnabled() { return GameOption.SHOW_FOLLOW_POINTS.getBooleanValue(); }

	/**
	 * Returns the background dim level.
	 * @return the alpha level [0, 1]
	 */
	public static float getBackgroundDim() { return (100 - GameOption.BACKGROUND_DIM.getIntegerValue()) / 100f; }

	/**
	 * Returns whether or not to override the song background with the default playfield background.
	 * @return true if forced
	 */
	public static boolean isDefaultPlayfieldForced() { return GameOption.FORCE_DEFAULT_PLAYFIELD.getBooleanValue(); }

	/**
	 * Returns whether or not beatmap skins are ignored.
	 * @return true if ignored
	 */
	public static boolean isBeatmapSkinIgnored() { return GameOption.IGNORE_BEATMAP_SKINS.getBooleanValue(); }

	/**
	 * Returns the fixed circle size override, if any.
	 * @return the CS value (0, 10], 0f if disabled
	 */
	public static float getFixedCS() { return GameOption.FIXED_CS.getIntegerValue() / 10f; }

	/**
	 * Returns the fixed HP drain rate override, if any.
	 * @return the HP value (0, 10], 0f if disabled
	 */
	public static float getFixedHP() { return GameOption.FIXED_HP.getIntegerValue() / 10f; }

	/**
	 * Returns the fixed approach rate override, if any.
	 * @return the AR value (0, 10], 0f if disabled
	 */
	public static float getFixedAR() { return GameOption.FIXED_AR.getIntegerValue() / 10f; }

	/**
	 * Returns the fixed overall difficulty override, if any.
	 * @return the OD value (0, 10], 0f if disabled
	 */
	public static float getFixedOD() { return GameOption.FIXED_OD.getIntegerValue() / 10f; }

	/**
	 * Returns whether or not to render loading text in the splash screen.
	 * @return true if enabled
	 */
	public static boolean isLoadVerbose() { return GameOption.LOAD_VERBOSE.getBooleanValue(); }

	/**
	 * Returns the track checkpoint time.
	 * @return the checkpoint time (in ms)
	 */
	public static int getCheckpoint() { return GameOption.CHECKPOINT.getIntegerValue() * 1000; }

	/**
	 * Returns whether or not all sound effects are disabled.
	 * @return true if disabled
	 */
	public static boolean isSoundDisabled() { return GameOption.DISABLE_SOUNDS.getBooleanValue(); }

	/**
	 * Returns whether or not to use non-English metadata where available.
	 * @return true if Unicode preferred
	 */
	public static boolean useUnicodeMetadata() { return GameOption.SHOW_UNICODE.getBooleanValue(); }

	/**
	 * Returns whether or not to play the theme song.
	 * @return true if enabled
	 */
	public static boolean isThemeSongEnabled() { return GameOption.ENABLE_THEME_SONG.getBooleanValue(); }

	/**
	 * Sets the track checkpoint time, if within bounds.
	 * @param time the track position (in ms)
	 * @return true if within bounds
	 */
	public static boolean setCheckpoint(int time) {
		if (time >= 0 && time < 3600) {
			GameOption.CHECKPOINT.setValue(time);
			return true;
		}
		return false;
	}

	/**
	 * Returns whether or not to show the hit error bar.
	 * @return true if enabled
	 */
	public static boolean isHitErrorBarEnabled() { return GameOption.SHOW_HIT_ERROR_BAR.getBooleanValue(); }

	/**
	 * Returns whether or not to load HD (@2x) images.
	 * @return true if HD images are enabled, false if only SD images should be loaded
	 */
	public static boolean loadHDImages() { return GameOption.LOAD_HD_IMAGES.getBooleanValue(); }

	/**
	 * Returns whether or not the mouse wheel is disabled during gameplay.
	 * @return true if disabled
	 */
	public static boolean isMouseWheelDisabled() { return GameOption.DISABLE_MOUSE_WHEEL.getBooleanValue(); }

	/**
	 * Returns whether or not the mouse buttons are disabled during gameplay.
	 * @return true if disabled
	 */
	public static boolean isMouseDisabled() { return GameOption.DISABLE_MOUSE_BUTTONS.getBooleanValue(); }

	/**
	 * Toggles the mouse button enabled/disabled state during gameplay and
	 * sends a bar notification about the action.
	 */
	public static void toggleMouseDisabled() {
		GameOption.DISABLE_MOUSE_BUTTONS.click(null);
		UI.sendBarNotification((GameOption.DISABLE_MOUSE_BUTTONS.getBooleanValue()) ?
			"Mouse buttons are disabled." : "Mouse buttons are enabled.");
	}

	/**
	 * Returns the left game key.
	 * @return the left key code
	 */
	public static int getGameKeyLeft() {
		if (keyLeft == Keyboard.KEY_NONE)
			setGameKeyLeft(Input.KEY_Z);
		return keyLeft;
	}

	/**
	 * Returns the right game key.
	 * @return the right key code
	 */
	public static int getGameKeyRight() {
		if (keyRight == Keyboard.KEY_NONE)
			setGameKeyRight(Input.KEY_X);
		return keyRight;
	}

	/**
	 * Sets the left game key.
	 * This will not be set to the same key as the right game key, nor to any
	 * reserved keys (see {@link #isValidGameKey(int)}).
	 * @param key the keyboard key
	 * @return {@code true} if the key was set, {@code false} if it was rejected
	 */
	public static boolean setGameKeyLeft(int key) {
		if ((key == keyRight && key != Keyboard.KEY_NONE) || !isValidGameKey(key))
			return false;
		keyLeft = key;
		return true;
	}

	/**
	 * Sets the right game key.
	 * This will not be set to the same key as the left game key, nor to any
	 * reserved keys (see {@link #isValidGameKey(int)}).
	 * @param key the keyboard key
	 * @return {@code true} if the key was set, {@code false} if it was rejected
	 */
	public static boolean setGameKeyRight(int key) {
		if ((key == keyLeft && key != Keyboard.KEY_NONE) || !isValidGameKey(key))
			return false;
		keyRight = key;
		return true;
	}

	/**
	 * Checks if the given key is a valid game key.
	 * @param key the keyboard key
	 * @return {@code true} if valid, {@code false} otherwise
	 */
	private static boolean isValidGameKey(int key) {
		return (key != Keyboard.KEY_ESCAPE && key != Keyboard.KEY_SPACE &&
		        key != Keyboard.KEY_UP && key != Keyboard.KEY_DOWN &&
		        key != Keyboard.KEY_F7 && key != Keyboard.KEY_F10 && key != Keyboard.KEY_F12);
	}

	/**
	 * Returns the beatmap directory.
	 * If invalid, this will attempt to search for the directory,
	 * and if nothing found, will create one.
	 * @return the beatmap directory
	 */
	public static File getBeatmapDir() {
		if (beatmapDir != null && beatmapDir.isDirectory())
			return beatmapDir;

		// search for directory
		for (int i = 0; i < BEATMAP_DIRS.length; i++) {
			beatmapDir = new File(BEATMAP_DIRS[i]);
			if (beatmapDir.isDirectory())
				return beatmapDir;
		}
		beatmapDir.mkdir();  // none found, create new directory
		return beatmapDir;
	}

	/**
	 * Returns the OSZ archive directory.
	 * If invalid, this will create and return a "SongPacks" directory.
	 * @return the OSZ archive directory
	 */
	public static File getOSZDir() {
		if (oszDir != null && oszDir.isDirectory())
			return oszDir;

		oszDir = new File(DATA_DIR, "SongPacks/");
		oszDir.mkdir();
		return oszDir;
	}

	/**
	 * Returns the screenshot directory.
	 * If invalid, this will return a "Screenshot" directory.
	 * @return the screenshot directory
	 */
	public static File getScreenshotDir() {
		if (screenshotDir != null && screenshotDir.isDirectory())
			return screenshotDir;

		screenshotDir = new File(DATA_DIR, "Screenshots/");
		return screenshotDir;
	}

	/**
	 * Returns the replay directory.
	 * If invalid, this will return a "Replay" directory.
	 * @return the replay directory
	 */
	public static File getReplayDir() {
		if (replayDir != null && replayDir.isDirectory())
			return replayDir;

		replayDir = new File(DATA_DIR, "Replays/");
		return replayDir;
	}

	/**
	 * Returns the current skin directory.
	 * If invalid, this will create a "Skins" folder in the root directory.
	 * @return the skin directory
	 */
	public static File getSkinRootDir() {
		if (skinRootDir != null && skinRootDir.isDirectory())
			return skinRootDir;

		// search for directory
		for (int i = 0; i < SKIN_ROOT_DIRS.length; i++) {
			skinRootDir = new File(SKIN_ROOT_DIRS[i]);
			if (skinRootDir.isDirectory())
				return skinRootDir;
		}
		skinRootDir.mkdir();  // none found, create new directory
		return skinRootDir;
	}

	/**
	 * Loads the skin given by the current skin directory.
	 * If the directory is invalid, the default skin will be loaded.
	 */
	public static void loadSkin() {
		File root = getSkinRootDir();
		File skinDir = new File(root, skinName);
		if (!skinDir.isDirectory()) {  // invalid skin name
			skinName = Skin.DEFAULT_SKIN_NAME;
			skinDir = null;
		}

		// create available skins list
		File[] dirs = SkinLoader.getSkinDirectories(root);
		skinDirs = new String[dirs.length + 1];
		skinDirs[0] = Skin.DEFAULT_SKIN_NAME;
		for (int i = 0; i < dirs.length; i++)
			skinDirs[i + 1] = dirs[i].getName();

		// set skin and modify resource locations
		ResourceLoader.removeAllResourceLocations();
		if (skinDir == null)
			skin = new Skin(null);
		else {
			// set skin index
			for (int i = 1; i < skinDirs.length; i++) {
				if (skinDirs[i].equals(skinName)) {
					skinDirIndex = i;
					break;
				}
			}

			// load the skin
			skin = SkinLoader.loadSkin(skinDir);
			ResourceLoader.addResourceLocation(new FileSystemLocation(skinDir));
		}
		ResourceLoader.addResourceLocation(new ClasspathLocation());
		ResourceLoader.addResourceLocation(new FileSystemLocation(new File(".")));
		ResourceLoader.addResourceLocation(new FileSystemLocation(new File("./res/")));
	}

	/**
	 * Returns the current skin.
	 * @return the skin, or null if no skin is loaded (see {@link #loadSkin()})
	 */
	public static Skin getSkin() { return skin; }

	/**
	 * Returns a dummy Beatmap containing the theme song.
	 * @return the theme song beatmap
	 */
	public static Beatmap getThemeBeatmap() {
		String[] tokens = themeString.split(",");
		if (tokens.length != 4) {
			ErrorHandler.error("Theme song string is malformed.", null, false);
			return null;
		}

		Beatmap beatmap = new Beatmap(null);
		beatmap.audioFilename = new File(tokens[0]);
		beatmap.title = tokens[1];
		beatmap.artist = tokens[2];
		try {
			beatmap.endTime = Integer.parseInt(tokens[3]);
		} catch (NumberFormatException e) {
			ErrorHandler.error("Theme song length is not a valid integer", e, false);
			return null;
		}

		return beatmap;
	}

	/**
	 * Reads user options from the options file, if it exists.
	 */
	public static void parseOptions() {
		// if no config file, use default settings
		if (!OPTIONS_FILE.isFile()) {
			saveOptions();
			return;
		}

		try (BufferedReader in = new BufferedReader(new FileReader(OPTIONS_FILE))) {
			String line;
			String name, value;
			int i;
			while ((line = in.readLine()) != null) {
				line = line.trim();
				if (line.length() < 2 || line.charAt(0) == '#')
					continue;
				int index = line.indexOf('=');
				if (index == -1)
					continue;
				name = line.substring(0, index).trim();
				value = line.substring(index + 1).trim();
				try {
					switch (name) {
					case "BeatmapDirectory":
						beatmapDir = new File(value);
						break;
					case "OSZDirectory":
						oszDir = new File(value);
						break;
					case "ScreenshotDirectory":
						screenshotDir = new File(value);
						break;
					case "ReplayDirectory":
						replayDir = new File(value);
						break;
					case "SkinDirectory":
						skinRootDir = new File(value);
						break;
					case "ThemeSong":
						themeString = value;
						break;
					case "Port":
						i = Integer.parseInt(value);
						if (i > 0 && i <= 65535)
							port = i;
						break;
					case "ScreenResolution":
						try {
							Resolution res = Resolution.valueOf(String.format("RES_%s", value.replace('x', '_')));
							resolution = res;
						} catch (IllegalArgumentException e) {}
						break;
//					case "Fullscreen":
//						GameOption.FULLSCREEN.setValue(Boolean.parseBoolean(value));
//						break;
					case "Skin":
						skinName = value;
						break;
					case "FrameSync":
						i = Integer.parseInt(value);
						for (int j = 0; j < targetFPS.length; j++) {
							if (i == targetFPS[j])
								targetFPSindex = j;
						}
						break;
					case "ScreenshotFormat":
						i = Integer.parseInt(value);
						if (i >= 0 && i < screenshotFormat.length)
							screenshotFormatIndex = i;
						break;
					case "FpsCounter":
						GameOption.SHOW_FPS.setValue(Boolean.parseBoolean(value));
						break;
					case "ShowUnicode":
						GameOption.SHOW_UNICODE.setValue(Boolean.parseBoolean(value));
						break;
					case "NewCursor":
						GameOption.NEW_CURSOR.setValue(Boolean.parseBoolean(value));
						break;
					case "DynamicBackground":
						GameOption.DYNAMIC_BACKGROUND.setValue(Boolean.parseBoolean(value));
						break;
					case "LoadVerbose":
						GameOption.LOAD_VERBOSE.setValue(Boolean.parseBoolean(value));
						break;
					case "VolumeUniversal":
						i = Integer.parseInt(value);
						if (i >= 0 && i <= 100)
							GameOption.MASTER_VOLUME.setValue(i);
						break;
					case "VolumeMusic":
						i = Integer.parseInt(value);
						if (i >= 0 && i <= 100)
							GameOption.MUSIC_VOLUME.setValue(i);
						break;
					case "VolumeEffect":
						i = Integer.parseInt(value);
						if (i >= 0 && i <= 100)
							GameOption.EFFECT_VOLUME.setValue(i);
						break;
					case "VolumeHitSound":
						i = Integer.parseInt(value);
						if (i >= 0 && i <= 100)
							GameOption.HITSOUND_VOLUME.setValue(i);
						break;
					case "Offset":
						i = Integer.parseInt(value);
						if (i >= -500 && i <= 500)
							GameOption.MUSIC_OFFSET.setValue(i);
						break;
					case "DisableSound":
						GameOption.DISABLE_SOUNDS.setValue(Boolean.parseBoolean(value));
						break;
					case "keyOsuLeft":
						setGameKeyLeft(Keyboard.getKeyIndex(value));
						break;
					case "keyOsuRight":
						setGameKeyRight(Keyboard.getKeyIndex(value));
						break;
					case "MouseDisableWheel":
						GameOption.DISABLE_MOUSE_WHEEL.setValue(Boolean.parseBoolean(value));
						break;
					case "MouseDisableButtons":
						GameOption.DISABLE_MOUSE_BUTTONS.setValue(Boolean.parseBoolean(value));
						break;
					case "DimLevel":
						i = Integer.parseInt(value);
						if (i >= 0 && i <= 100)
							GameOption.BACKGROUND_DIM.setValue(i);
						break;
					case "ForceDefaultPlayfield":
						GameOption.FORCE_DEFAULT_PLAYFIELD.setValue(Boolean.parseBoolean(value));
						break;
					case "IgnoreBeatmapSkins":
						GameOption.IGNORE_BEATMAP_SKINS.setValue(Boolean.parseBoolean(value));
						break;
					case "HitLighting":
						GameOption.SHOW_HIT_LIGHTING.setValue(Boolean.parseBoolean(value));
						break;
					case "ComboBurst":
						GameOption.SHOW_COMBO_BURSTS.setValue(Boolean.parseBoolean(value));
						break;
					case "PerfectHit":
						GameOption.SHOW_PERFECT_HIT.setValue(Boolean.parseBoolean(value));
						break;
					case "FollowPoints":
						GameOption.SHOW_FOLLOW_POINTS.setValue(Boolean.parseBoolean(value));
						break;
					case "ScoreMeter":
						GameOption.SHOW_HIT_ERROR_BAR.setValue(Boolean.parseBoolean(value));
						break;
					case "LoadHDImages":
						GameOption.LOAD_HD_IMAGES.setValue(Boolean.parseBoolean(value));
						break;
					case "FixedCS":
						GameOption.FIXED_CS.setValue((int) (Float.parseFloat(value) * 10f));
						break;
					case "FixedHP":
						GameOption.FIXED_HP.setValue((int) (Float.parseFloat(value) * 10f));
						break;
					case "FixedAR":
						GameOption.FIXED_AR.setValue((int) (Float.parseFloat(value) * 10f));
						break;
					case "FixedOD":
						GameOption.FIXED_OD.setValue((int) (Float.parseFloat(value) * 10f));
						break;
					case "Checkpoint":
						setCheckpoint(Integer.parseInt(value));
						break;
					case "MenuMusic":
						GameOption.ENABLE_THEME_SONG.setValue(Boolean.parseBoolean(value));
						break;
					}
				} catch (NumberFormatException e) {
					Log.warn(String.format("Format error in options file for line: '%s'.", line), e);
					continue;
				}
			}
		} catch (IOException e) {
			ErrorHandler.error(String.format("Failed to read file '%s'.", OPTIONS_FILE.getAbsolutePath()), e, false);
		}
	}

	/**
	 * (Over)writes user options to a file.
	 */
	public static void saveOptions() {
		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(OPTIONS_FILE), "utf-8"))) {
			// header
			SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM dd, yyyy");
			String date = dateFormat.format(new Date());
			writer.write("# opsu! configuration");
			writer.newLine();
			writer.write("# last updated on ");
			writer.write(date);
			writer.newLine();
			writer.newLine();

			// options
			writer.write(String.format("BeatmapDirectory = %s", getBeatmapDir().getAbsolutePath()));
			writer.newLine();
			writer.write(String.format("OSZDirectory = %s", getOSZDir().getAbsolutePath()));
			writer.newLine();
			writer.write(String.format("ScreenshotDirectory = %s", getScreenshotDir().getAbsolutePath()));
			writer.newLine();
			writer.write(String.format("ReplayDirectory = %s", getReplayDir().getAbsolutePath()));
			writer.newLine();
			writer.write(String.format("SkinDirectory = %s", getSkinRootDir().getAbsolutePath()));
			writer.newLine();
			writer.write(String.format("ThemeSong = %s", themeString));
			writer.newLine();
			writer.write(String.format("Port = %d", port));
			writer.newLine();
			writer.write(String.format("ScreenResolution = %s", resolution.toString()));
			writer.newLine();
//			writer.write(String.format("Fullscreen = %b", isFullscreen()));
//			writer.newLine();
			writer.write(String.format("Skin = %s", skinName));
			writer.newLine();
			writer.write(String.format("FrameSync = %d", targetFPS[targetFPSindex]));
			writer.newLine();
			writer.write(String.format("FpsCounter = %b", isFPSCounterEnabled()));
			writer.newLine();
			writer.write(String.format("ShowUnicode = %b", useUnicodeMetadata()));
			writer.newLine();
			writer.write(String.format("ScreenshotFormat = %d", screenshotFormatIndex));
			writer.newLine();
			writer.write(String.format("NewCursor = %b", isNewCursorEnabled()));
			writer.newLine();
			writer.write(String.format("DynamicBackground = %b", isDynamicBackgroundEnabled()));
			writer.newLine();
			writer.write(String.format("LoadVerbose = %b", isLoadVerbose()));
			writer.newLine();
			writer.write(String.format("VolumeUniversal = %d", GameOption.MASTER_VOLUME.getIntegerValue()));
			writer.newLine();
			writer.write(String.format("VolumeMusic = %d", GameOption.MUSIC_VOLUME.getIntegerValue()));
			writer.newLine();
			writer.write(String.format("VolumeEffect = %d", GameOption.EFFECT_VOLUME.getIntegerValue()));
			writer.newLine();
			writer.write(String.format("VolumeHitSound = %d", GameOption.HITSOUND_VOLUME.getIntegerValue()));
			writer.newLine();
			writer.write(String.format("Offset = %d", getMusicOffset()));
			writer.newLine();
			writer.write(String.format("DisableSound = %b", isSoundDisabled()));
			writer.newLine();
			writer.write(String.format("keyOsuLeft = %s", Keyboard.getKeyName(getGameKeyLeft())));
			writer.newLine();
			writer.write(String.format("keyOsuRight = %s", Keyboard.getKeyName(getGameKeyRight())));
			writer.newLine();
			writer.write(String.format("MouseDisableWheel = %b", isMouseWheelDisabled()));
			writer.newLine();
			writer.write(String.format("MouseDisableButtons = %b", isMouseDisabled()));
			writer.newLine();
			writer.write(String.format("DimLevel = %d", GameOption.BACKGROUND_DIM.getIntegerValue()));
			writer.newLine();
			writer.write(String.format("ForceDefaultPlayfield = %b", isDefaultPlayfieldForced()));
			writer.newLine();
			writer.write(String.format("IgnoreBeatmapSkins = %b", isBeatmapSkinIgnored()));
			writer.newLine();
			writer.write(String.format("HitLighting = %b", isHitLightingEnabled()));
			writer.newLine();
			writer.write(String.format("ComboBurst = %b", isComboBurstEnabled()));
			writer.newLine();
			writer.write(String.format("PerfectHit = %b", isPerfectHitBurstEnabled()));
			writer.newLine();
			writer.write(String.format("FollowPoints = %b", isFollowPointEnabled()));
			writer.newLine();
			writer.write(String.format("ScoreMeter = %b", isHitErrorBarEnabled()));
			writer.newLine();
			writer.write(String.format("LoadHDImages = %b", loadHDImages()));
			writer.newLine();
			writer.write(String.format(Locale.US, "FixedCS = %.1f", getFixedCS()));
			writer.newLine();
			writer.write(String.format(Locale.US, "FixedHP = %.1f", getFixedHP()));
			writer.newLine();
			writer.write(String.format(Locale.US, "FixedAR = %.1f", getFixedAR()));
			writer.newLine();
			writer.write(String.format(Locale.US, "FixedOD = %.1f", getFixedOD()));
			writer.newLine();
			writer.write(String.format("Checkpoint = %d", GameOption.CHECKPOINT.getIntegerValue()));
			writer.newLine();
			writer.write(String.format("MenuMusic = %b", isThemeSongEnabled()));
			writer.newLine();
			writer.close();
		} catch (IOException e) {
			ErrorHandler.error(String.format("Failed to write to file '%s'.", OPTIONS_FILE.getAbsolutePath()), e, false);
		}
	}
}
