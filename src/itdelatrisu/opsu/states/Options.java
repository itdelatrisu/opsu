/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014 Jeffrey Han
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

package itdelatrisu.opsu.states;

import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.GameMod;
import itdelatrisu.opsu.MenuButton;
import itdelatrisu.opsu.Opsu;
import itdelatrisu.opsu.OsuFile;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.audio.SoundEffect;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.lwjgl.input.Keyboard;
import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.BasicGameState;
import org.newdawn.slick.state.StateBasedGame;
import org.newdawn.slick.state.transition.EmptyTransition;
import org.newdawn.slick.state.transition.FadeInTransition;
import org.newdawn.slick.util.Log;

/**
 * "Game Options" state.
 */
public class Options extends BasicGameState {
	/**
	 * Temporary folder for file conversions, auto-deleted upon successful exit.
	 */
	public static final File TMP_DIR = new File(".opsu_tmp/");
	
	/**
	 * File for logging errors.
	 */
	public static final File LOG_FILE = new File(".opsu.log");

	/**
	 * File for storing user options.
	 */
	private static final File OPTIONS_FILE = new File(".opsu.cfg");

	/**
	 * Beatmap directories (where to search for files).
	 */
	private static final String[] BEATMAP_DIRS = {
		"C:/Program Files (x86)/osu!/Songs/",
		"C:/Program Files/osu!/Songs/",
		"Songs/"
	};

	/**
	 * Font file name.
	 */
	public static final String FONT_NAME = "kochi-gothic.ttf";

	/**
	 * The beatmap directory.
	 */
	private static File beatmapDir;

	/**
	 * The OSZ archive directory.
	 */
	private static File oszDir;

	/**
	 * The screenshot directory (created when needed).
	 */
	private static File screenshotDir;

	/**
	 * The current skin directory (for user skins).
	 */
	private static File skinDir;

	/**
	 * The theme song string:
	 * filename, title, artist, length (ms)
	 */
	private static String themeString = "theme.ogg,welcome to osu!,nekodex,48000";

	/**
	 * Game options.
	 */
	private static enum GameOption {
		NULL (null, null),
		SCREEN_RESOLUTION ("Screen Resolution", "Restart to apply resolution changes.") {
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
//		FULLSCREEN ("Fullscreen Mode", "Restart to apply changes.") {
//			@Override
//			public String getValueString() { return fullscreen ? "Yes" : "No"; }
//
//			@Override
//			public void click(GameContainer container) { fullscreen = !fullscreen; }
//		},
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
		MUSIC_VOLUME ("Music Volume", "Global music volume.") {
			@Override
			public String getValueString() { return String.format("%d%%", musicVolume); }

			@Override
			public void drag(GameContainer container, int d) {
				musicVolume = Utils.getBoundedValue(musicVolume, d, 0, 100);
				container.setMusicVolume(getMusicVolume());
			}
		},
		EFFECT_VOLUME ("Effect Volume", "Volume of menu and game sounds.") {
			@Override
			public String getValueString() { return String.format("%d%%", effectVolume); }

			@Override
			public void drag(GameContainer container, int d) { effectVolume = Utils.getBoundedValue(effectVolume, d, 0, 100); }
		},
		HITSOUND_VOLUME ("Hit Sound Volume", "Volume of hit sounds.") {
			@Override
			public String getValueString() { return String.format("%d%%", hitSoundVolume); }

			@Override
			public void drag(GameContainer container, int d) { hitSoundVolume = Utils.getBoundedValue(hitSoundVolume, d, 0, 100); }
		},
		MUSIC_OFFSET ("Music Offset", "Adjust this value if hit objects are out of sync.") {
			@Override
			public String getValueString() { return String.format("%dms", musicOffset); }

			@Override
			public void drag(GameContainer container, int d) { musicOffset = Utils.getBoundedValue(musicOffset, d, -500, 500); }
		},
		SCREENSHOT_FORMAT ("Screenshot Format", "Press F12 to take a screenshot.") {
			@Override
			public String getValueString() { return screenshotFormat[screenshotFormatIndex].toUpperCase(); }

			@Override
			public void click(GameContainer container) { screenshotFormatIndex = (screenshotFormatIndex + 1) % screenshotFormat.length; }
		},
		SHOW_FPS ("Show FPS Counter", "Show an FPS counter in the bottom-right hand corner.") {
			@Override
			public String getValueString() { return showFPS ? "Yes" : "No"; }

			@Override
			public void click(GameContainer container) { showFPS = !showFPS; }
		},
		SHOW_HIT_LIGHTING ("Show Hit Lighting", "Adds an effect behind hit explosions.") {
			@Override
			public String getValueString() { return showHitLighting ? "Yes" : "No"; }

			@Override
			public void click(GameContainer container) { showHitLighting = !showHitLighting; }
		},
		SHOW_COMBO_BURSTS ("Show Combo Bursts", "A character image is displayed at combo milestones.") {
			@Override
			public String getValueString() { return showComboBursts ? "Yes" : "No"; }

			@Override
			public void click(GameContainer container) { showComboBursts = !showComboBursts; }
		},
		SHOW_PERFECT_HIT ("Show Perfect Hits", "Whether to show perfect hit result bursts (300s, slider ticks).") {
			@Override
			public String getValueString() { return showPerfectHit ? "Yes" : "No"; }

			@Override
			public void click(GameContainer container) { showPerfectHit = !showPerfectHit; }
		},
		NEW_CURSOR ("Enable New Cursor", "Use the new cursor style (may cause higher CPU usage).") {
			@Override
			public String getValueString() { return newCursor ? "Yes" : "No"; }

			@Override
			public void click(GameContainer container) {
				newCursor = !newCursor;
				try {
					Utils.loadCursor();
				} catch (SlickException e) {
					Log.error("Failed to load cursor.", e);
				}
			}
		},
		DYNAMIC_BACKGROUND ("Enable Dynamic Backgrounds", "The song background will be used as the main menu background.") {
			@Override
			public String getValueString() { return dynamicBackground ? "Yes" : "No"; }

			@Override
			public void click(GameContainer container) { dynamicBackground = !dynamicBackground; }
		},
		BACKGROUND_DIM ("Background Dim", "Percentage to dim the background image during gameplay.") {
			@Override
			public String getValueString() { return String.format("%d%%", backgroundDim); }

			@Override
			public void drag(GameContainer container, int d) { backgroundDim = Utils.getBoundedValue(backgroundDim, d, 0, 100); }
		},
		FORCE_DEFAULT_PLAYFIELD ("Force Default Playfield", "Override the song background with the default playfield background.") {
			@Override
			public String getValueString() { return forceDefaultPlayfield ? "Yes" : "No"; }

			@Override
			public void click(GameContainer container) { forceDefaultPlayfield = !forceDefaultPlayfield; }
		},
		IGNORE_BEATMAP_SKINS ("Ignore All Beatmap Skins", "Never use skin element overrides provided by beatmaps.") {
			@Override
			public String getValueString() { return ignoreBeatmapSkins ? "Yes" : "No"; }

			@Override
			public void click(GameContainer container) { ignoreBeatmapSkins = !ignoreBeatmapSkins; }
		},
		FIXED_CS ("Fixed Circle Size (CS)", "Determines the size of circles and sliders.") {
			@Override
			public String getValueString() { return (fixedCS == 0f) ? "Disabled" : String.format("%.1f", fixedCS); }

			@Override
			public void drag(GameContainer container, int d) { fixedCS = Utils.getBoundedValue(fixedCS, d / 10f, 0f, 10f); }
		},
		FIXED_HP ("Fixed HP Drain Rate (HP)", "Determines the rate at which health decreases.") {
			@Override
			public String getValueString() { return (fixedHP == 0f) ? "Disabled" : String.format("%.1f", fixedHP); }

			@Override
			public void drag(GameContainer container, int d) { fixedHP = Utils.getBoundedValue(fixedHP, d / 10f, 0f, 10f); }
		},
		FIXED_AR ("Fixed Approach Rate (AR)", "Determines how long hit circles stay on the screen.") {
			@Override
			public String getValueString() { return (fixedAR == 0f) ? "Disabled" : String.format("%.1f", fixedAR); }

			@Override
			public void drag(GameContainer container, int d) { fixedAR = Utils.getBoundedValue(fixedAR, d / 10f, 0f, 10f); }
		},
		FIXED_OD ("Fixed Overall Difficulty (OD)", "Determines the time window for hit results.") {
			@Override
			public String getValueString() { return (fixedOD == 0f) ? "Disabled" : String.format("%.1f", fixedOD); }

			@Override
			public void drag(GameContainer container, int d) { fixedOD = Utils.getBoundedValue(fixedOD, d / 10f, 0f, 10f); }
		},
		LOAD_VERBOSE ("Show Detailed Loading Progress", "Display more specific loading information in the splash screen.") {
			@Override
			public String getValueString() { return loadVerbose ? "Yes" : "No"; }

			@Override
			public void click(GameContainer container) { loadVerbose = !loadVerbose; }
		},
		CHECKPOINT ("Track Checkpoint", "Press CTRL+L while playing to load a checkpoint, and CTRL+S to set one.") {
			@Override
			public String getValueString() {
				return (checkpoint == 0) ? "Disabled" : String.format("%02d:%02d",
						TimeUnit.SECONDS.toMinutes(checkpoint),
						checkpoint - TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(checkpoint)));
			}

			@Override
			public void drag(GameContainer container, int d) { checkpoint = Utils.getBoundedValue(checkpoint, d, 0, 3599); }
		},
		DISABLE_SOUNDS ("Disable All Sound Effects", "May resolve Linux sound driver issues.  Requires a restart.") {
			@Override
			public String getValueString() { return disableSound ? "Yes" : "No"; }

			@Override
			public void click(GameContainer container) { disableSound = !disableSound; }
		},
		KEY_LEFT ("Left Game Key", "Select this option to input a key.") {
			@Override
			public String getValueString() { return Keyboard.getKeyName(getGameKeyLeft()); }

			@Override
			public void click(GameContainer container) {
				keyEntryLeft = true;
				keyEntryRight = false;
			}
		},
		KEY_RIGHT ("Right Game Key", "Select this option to input a key.") {
			@Override
			public String getValueString() { return Keyboard.getKeyName(getGameKeyRight()); }

			@Override
			public void click(GameContainer container) {
				keyEntryLeft = false;
				keyEntryRight = true;
			}
		},
		SHOW_UNICODE ("Prefer Non-English Metadata", "Where available, song titles will be shown in their native language.") {
			@Override
			public String getValueString() { return showUnicode ? "Yes" : "No"; }

			@Override
			public void click(GameContainer container) {
				showUnicode = !showUnicode;
				if (showUnicode) {
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
		ENABLE_THEME_SONG ("Enable Theme Song", "Whether to play the theme song upon starting opsu!") {
			@Override
			public String getValueString() { return themeSongEnabled ? "Yes" : "No"; }

			@Override
			public void click(GameContainer container) { themeSongEnabled = !themeSongEnabled; }
		};

		/**
		 * Option name.
		 */
		private String name;

		/**
		 * Option description.
		 */
		private String description;

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
		 * Returns the option name.
		 * @return the name string
		 */
		public String getName() { return name; }

		/**
		 * Returns the option description
		 * @return the description string
		 */
		public String getDescription() { return description; }

		/**
		 * Returns the value of the option as a string (via override).
		 * @return the value string
		 */
		public String getValueString() { return ""; }

		/**
		 * Processes a mouse click action (via override).
		 * @param container the game container
		 */
		public void click(GameContainer container) {}

		/**
		 * Processes a mouse drag action (via override).
		 * @param container the game container
		 * @param d the dragged distance (modified by multiplier)
		 */
		public void drag(GameContainer container, int d) {}
	};

	/**
	 * Option tab constants.
	 */
	private static final int
		TAB_DISPLAY  = 0,
		TAB_MUSIC    = 1,
		TAB_GAMEPLAY = 2,
		TAB_CUSTOM   = 3,
		TAB_MAX      = 4;  // not a tab

	/**
	 * Option tab names.
	 */
	private static final String[] TAB_NAMES = {
		"Display",
		"Music",
		"Gameplay",
		"Custom"
	};

	/**
	 * Option tab buttons.
	 */
	private static MenuButton[] optionTabs = new MenuButton[TAB_MAX];

	/**
	 * Current tab.
	 */
	private static int currentTab;

	/**
	 * Display options.
	 */
	private static final GameOption[] displayOptions = {
		GameOption.SCREEN_RESOLUTION,
//		GameOption.FULLSCREEN,
		GameOption.TARGET_FPS,
		GameOption.SHOW_FPS,
		GameOption.SHOW_UNICODE,
		GameOption.SCREENSHOT_FORMAT,
		GameOption.NEW_CURSOR,
		GameOption.DYNAMIC_BACKGROUND,
		GameOption.LOAD_VERBOSE
	};

	/**
	 * Music options.
	 */
	private static final GameOption[] musicOptions = {
		GameOption.MUSIC_VOLUME,
		GameOption.EFFECT_VOLUME,
		GameOption.HITSOUND_VOLUME,
		GameOption.MUSIC_OFFSET,
		GameOption.DISABLE_SOUNDS,
		GameOption.ENABLE_THEME_SONG
	};

	/**
	 * Gameplay options.
	 */
	private static final GameOption[] gameplayOptions = {
		GameOption.KEY_LEFT,
		GameOption.KEY_RIGHT,
		GameOption.BACKGROUND_DIM,
		GameOption.FORCE_DEFAULT_PLAYFIELD,
		GameOption.IGNORE_BEATMAP_SKINS,
		GameOption.SHOW_HIT_LIGHTING,
		GameOption.SHOW_COMBO_BURSTS,
		GameOption.SHOW_PERFECT_HIT
	};

	/**
	 * Custom options.
	 */
	private static final GameOption[] customOptions = {
		GameOption.FIXED_CS,
		GameOption.FIXED_HP,
		GameOption.FIXED_AR,
		GameOption.FIXED_OD,
		GameOption.CHECKPOINT
	};

	/**
	 * Max number of options displayed on one screen.
	 */
	private static int maxOptionsScreen = Math.max(
			Math.max(displayOptions.length, musicOptions.length),
			Math.max(gameplayOptions.length, customOptions.length));

	/**
	 * Screen resolutions.
	 */
	private enum Resolution {
		RES_800_600 (800, 600),
		RES_1024_600 (1024, 600),
		RES_1024_768 (1024, 768),
		RES_1280_800 (1280, 800),
		RES_1280_960 (1280, 960),
		RES_1366_768 (1366, 768),
		RES_1440_900 (1440, 900),
		RES_1600_900 (1600, 900),
		RES_1680_1050 (1680, 1050),
		RES_1920_1080 (1920, 1080),
		RES_1920_1200 (1920, 1200),
		RES_2560_1440 (2560, 1440),
		RES_2560_1600 (2560, 1600);

		/**
		 * Screen dimensions.
		 */
		private int width, height;

		/**
		 * Enum values.
		 */
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

	/**
	 * Index (row) in resolutions[][] array.
	 */
	private static Resolution resolution = Resolution.RES_1280_800;

//	/**
//	 * Whether or not the game should run in fullscreen mode.
//	 */
//	private static boolean fullscreen = false;

	/**
	 * Frame limiters.
	 */
	private static final int[] targetFPS = { 60, 120, 240 };
	
	/**
	 * Index in targetFPS[] array.
	 */
	private static int targetFPSindex = 0;

	/**
	 * Whether or not to show the FPS.
	 */
	private static boolean showFPS = false;

	/**
	 * Whether or not to show hit lighting effects.
	 */
	private static boolean showHitLighting = true;

	/**
	 * Whether or not to show combo burst images.
	 */
	private static boolean showComboBursts = true;

	/**
	 * Default music volume.
	 */
	private static int musicVolume = 30;

	/**
	 * Default sound effect volume.
	 */
	private static int effectVolume = 20;

	/**
	 * Default hit sound volume.
	 */
	private static int hitSoundVolume = 20;

	/**
	 * Offset time, in milliseconds, for music position-related elements.
	 */
	private static int musicOffset = -150;

	/**
	 * Screenshot file format.
	 */
	private static String[] screenshotFormat = { "png", "jpg", "bmp" };

	/**
	 * Index in screenshotFormat[] array.
	 */
	private static int screenshotFormatIndex = 0;

	/**
	 * Port binding.
	 */
	private static int port = 49250;

	/**
	 * Whether or not to use the new cursor type.
	 */
	private static boolean newCursor = true;

	/**
	 * Whether or not dynamic backgrounds are enabled.
	 */
	private static boolean dynamicBackground = true;

	/**
	 * Whether or not to display perfect hit results.
	 */
	private static boolean showPerfectHit = true;

	/**
	 * Percentage to dim background images during gameplay.
	 */
	private static int backgroundDim = 30;

	/**
	 * Whether or not to always display the default playfield background.
	 */
	private static boolean forceDefaultPlayfield = false;

	/**
	 * Whether or not to ignore resources in the beatmap folders.
	 */
	private static boolean ignoreBeatmapSkins = false;

	/**
	 * Whether or not to play the theme song.
	 */
	private static boolean themeSongEnabled = true;

	/**
	 * Fixed difficulty overrides.
	 */
	private static float
		fixedCS = 0f, fixedHP = 0f,
		fixedAR = 0f, fixedOD = 0f;

	/**
	 * Whether or not to display the files being loaded in the splash screen.
	 */
	private static boolean loadVerbose = false;

	/**
	 * Track checkpoint time, in seconds.
	 */
	private static int checkpoint = 0;

	/**
	 * Whether or not to disable all sounds.
	 * This will prevent SoundController from loading sound files.
	 * <p>
	 * By default, sound is disabled on Linux due to possible driver issues.
	 */
	private static boolean disableSound =
		(System.getProperty("os.name").toLowerCase().indexOf("linux") > -1);

	/**
	 * Whether or not to display non-English metadata.
	 */
	private static boolean showUnicode = false;

	/**
	 * Left and right game keys.
	 */
	private static int
		keyLeft  = Keyboard.KEY_NONE,
		keyRight = Keyboard.KEY_NONE;

	/**
	 * Key entry states.
	 */
	private static boolean keyEntryLeft = false, keyEntryRight = false;

	/**
	 * Game option coordinate modifiers (for drawing).
	 */
	private int textY, offsetY;

	// game-related variables
	private GameContainer container;
	private StateBasedGame game;
	private Input input;
	private Graphics g;
	private int state;

	public Options(int state) {
		this.state = state;
	}

	@Override
	public void init(GameContainer container, StateBasedGame game)
			throws SlickException {
		this.container = container;
		this.game = game;
		this.input = container.getInput();
		this.g = container.getGraphics();

		int width = container.getWidth();
		int height = container.getHeight();

		// game option coordinate modifiers
		textY = 20 + (Utils.FONT_XLARGE.getLineHeight() * 3 / 2);
		offsetY = (int) (((height * 0.8f) - textY) / maxOptionsScreen);

		// option tabs
		Image tab = GameImage.MENU_TAB.getImage();
		int subtextWidth = Utils.FONT_DEFAULT.getWidth("Click or drag an option to change it.");
		float tabX = (width / 50) + (tab.getWidth() / 2f);
		float tabY = 15 + Utils.FONT_XLARGE.getLineHeight() + (tab.getHeight() / 2f);
		int tabOffset = Math.min(tab.getWidth(), 
				((width - subtextWidth - tab.getWidth()) / 2) / TAB_MAX);
		for (int i = 0; i < optionTabs.length; i++)
			optionTabs[i] = new MenuButton(tab, tabX + (i * tabOffset), tabY);
	}

	@Override
	public void render(GameContainer container, StateBasedGame game, Graphics g)
			throws SlickException {
		g.setBackground(Utils.COLOR_BLACK_ALPHA);
		g.setColor(Color.white);

		int width = container.getWidth();
		int height = container.getHeight();
		int mouseX = input.getMouseX(), mouseY = input.getMouseY();

		// title
		Utils.FONT_XLARGE.drawString(
				(width / 2) - (Utils.FONT_XLARGE.getWidth("GAME OPTIONS") / 2),
				10, "GAME OPTIONS"
		);
		Utils.FONT_DEFAULT.drawString(
				(width / 2) - (Utils.FONT_DEFAULT.getWidth("Click or drag an option to change it.") / 2),
				10 + Utils.FONT_XLARGE.getLineHeight(), "Click or drag an option to change it."
		);

		// game options
		g.setLineWidth(1f);
		g.setFont(Utils.FONT_LARGE);
		switch (currentTab) {
		case TAB_DISPLAY:
			for (int i = 0; i < displayOptions.length; i++)
				drawOption(displayOptions[i], i);
			break;
		case TAB_MUSIC:
			for (int i = 0; i < musicOptions.length; i++)
				drawOption(musicOptions[i], i);
			break;
		case TAB_GAMEPLAY:
			for (int i = 0; i < gameplayOptions.length; i++)
				drawOption(gameplayOptions[i], i);
			break;
		case TAB_CUSTOM:
			for (int i = 0; i < customOptions.length; i++)
				drawOption(customOptions[i], i);
			break;
		}

		// option tabs
		int hoverTab = -1;
		for (int i = 0; i < optionTabs.length; i++) {
			if (optionTabs[i].contains(mouseX, mouseY)) {
				hoverTab = i;
				break;
			}
		}
		for (int i = optionTabs.length - 1; i >= 0; i--) {
			if (i != currentTab)
				Utils.drawTab(optionTabs[i].getX(), optionTabs[i].getY(),
						TAB_NAMES[i], false, i == hoverTab);
		}
		Utils.drawTab(optionTabs[currentTab].getX(), optionTabs[currentTab].getY(),
				TAB_NAMES[currentTab], true, false);
		g.setColor(Color.white);
		g.setLineWidth(2f);
		float lineY = optionTabs[0].getY() + (GameImage.MENU_TAB.getImage().getHeight() / 2f);
		g.drawLine(0, lineY, width, lineY);
		g.resetLineWidth();

		// game mods
		Utils.FONT_LARGE.drawString(width / 30, height * 0.8f, "Game Mods:", Color.white);
		for (GameMod mod : GameMod.values())
			mod.draw();

		Utils.getBackButton().draw();

		// key entry state
		if (keyEntryLeft || keyEntryRight) {
			g.setColor(Utils.COLOR_BLACK_ALPHA);
			g.fillRect(0, 0, width, height);
			g.setColor(Color.white);
			Utils.FONT_LARGE.drawString(
					(width / 2) - (Utils.FONT_LARGE.getWidth("Please enter a letter or digit.") / 2),
					(height / 2) - Utils.FONT_LARGE.getLineHeight(), "Please enter a letter or digit."
			);
		}

		Utils.drawFPS();
		Utils.drawCursor();
	}

	@Override
	public void update(GameContainer container, StateBasedGame game, int delta)
			throws SlickException {
		Utils.updateCursor(delta);
		int mouseX = input.getMouseX(), mouseY = input.getMouseY();
		Utils.getBackButton().hoverUpdate(delta, mouseX, mouseY);
		for (GameMod mod : GameMod.values())
			mod.hoverUpdate(delta, mouseX, mouseY);
	}

	@Override
	public int getID() { return state; }

	@Override
	public void mousePressed(int button, int x, int y) {
		// key entry state
		if (keyEntryLeft || keyEntryRight) {
			keyEntryLeft = keyEntryRight = false;
			return;
		}

		// check mouse button 
		if (button == Input.MOUSE_MIDDLE_BUTTON)
			return;

		// back
		if (Utils.getBackButton().contains(x, y)) {
			SoundController.playSound(SoundEffect.MENUBACK);
			game.enterState(Opsu.STATE_SONGMENU, new EmptyTransition(), new FadeInTransition(Color.black));
			return;
		}

		// option tabs
		for (int i = 0; i < optionTabs.length; i++) {
			if (optionTabs[i].contains(x, y)) {
				if (i != currentTab) {
					currentTab = i;
					SoundController.playSound(SoundEffect.MENUCLICK);
				}
				return;
			}
		}

		// game mods
		for (GameMod mod : GameMod.values()) {
			if (mod.contains(x, y)) {
				boolean prevState = mod.isActive();
				mod.toggle(true);
				if (mod.isActive() != prevState)
					SoundController.playSound(SoundEffect.MENUCLICK);
				return;
			}
		}

		// options (click only)
		GameOption option = getClickedOption(y);
		if (option != GameOption.NULL)
			option.click(container);
	}

	@Override
	public void mouseDragged(int oldx, int oldy, int newx, int newy) {
		// key entry state
		if (keyEntryLeft || keyEntryRight)
			return;

		// check mouse button (right click scrolls faster)
		int multiplier;
		if (input.isMouseButtonDown(Input.MOUSE_RIGHT_BUTTON))
			multiplier = 4;
		else if (input.isMouseButtonDown(Input.MOUSE_LEFT_BUTTON))
			multiplier = 1;
		else
			return;

		// get direction
		int diff = newx - oldx;
		if (diff == 0)
			return;
		diff = ((diff > 0) ? 1 : -1) * multiplier;

		// options (drag only)
		GameOption option = getClickedOption(oldy);
		if (option != GameOption.NULL)
			option.drag(container, diff);
	}

	@Override
	public void keyPressed(int key, char c) {
		// key entry state
		if (keyEntryLeft || keyEntryRight) {
			if (Character.isLetterOrDigit(c)) {
				if (keyEntryLeft && keyRight != key)
					keyLeft = key;
				else if (keyEntryRight && keyLeft != key)
					keyRight = key;
			}
			keyEntryLeft = keyEntryRight = false;
			return;
		}

		switch (key) {
		case Input.KEY_ESCAPE:
			SoundController.playSound(SoundEffect.MENUBACK);
			game.enterState(Opsu.STATE_SONGMENU, new EmptyTransition(), new FadeInTransition(Color.black));
			break;
		case Input.KEY_F12:
			Utils.takeScreenShot();
			break;
		case Input.KEY_TAB:
			int i = 1;
			if (input.isKeyDown(Input.KEY_LSHIFT) || input.isKeyDown(Input.KEY_RSHIFT))
				i = TAB_MAX - 1;
			currentTab = (currentTab + i) % TAB_MAX;
			SoundController.playSound(SoundEffect.MENUCLICK);
			break;
		}
	}

	@Override
	public void enter(GameContainer container, StateBasedGame game)
			throws SlickException {
		currentTab = TAB_DISPLAY;
		Utils.getBackButton().setScale(1f);
		for (GameMod mod : GameMod.values())
			mod.setScale(1f);
	}

	/**
	 * Draws a game option.
	 * @param option the option (OPTION_* constant)
	 * @param pos the position to draw at
	 */
	private void drawOption(GameOption option, int pos) {
		int width = container.getWidth();
		int textHeight = Utils.FONT_LARGE.getLineHeight();
		float y = textY + (pos * offsetY);

		g.setColor(Color.white);
		g.drawString(option.getName(), width / 30, y);
		g.drawString(option.getValueString(), width / 2, y);
		Utils.FONT_SMALL.drawString(width / 30, y + textHeight, option.getDescription());
		g.setColor(Utils.COLOR_WHITE_ALPHA);
		g.drawLine(0, y + textHeight, width, y + textHeight);
	}

	/**
	 * Returns the option clicked.
	 * If no option clicked, -1 will be returned.
	 * @param y the y coordinate
	 * @return the option (OPTION_* constant)
	 */
	private GameOption getClickedOption(int y) {
		GameOption option = GameOption.NULL;

		if (y < textY || y > textY + (offsetY * maxOptionsScreen))
			return option;

		int index = (y - textY + Utils.FONT_LARGE.getLineHeight()) / offsetY;
		switch (currentTab) {
		case TAB_DISPLAY:
			if (index < displayOptions.length)
				option = displayOptions[index];
			break;
		case TAB_MUSIC:
			if (index < musicOptions.length)
				option = musicOptions[index];
			break;
		case TAB_GAMEPLAY:
			if (index < gameplayOptions.length)
				option = gameplayOptions[index];
			break;
		case TAB_CUSTOM:
			if (index < customOptions.length)
				option = customOptions[index];
		}
		return option;
	}

	/**
	 * Returns the target frame rate.
	 * @return the target FPS
	 */
	public static int getTargetFPS() { return targetFPS[targetFPSindex]; }

	/**
	 * Returns the default music volume.
	 * @return the volume [0, 1]
	 */
	public static float getMusicVolume() { return musicVolume / 100f; }

	/**
	 * Returns the default sound effect volume.
	 * @return the sound volume [0, 1]
	 */
	public static float getEffectVolume() { return effectVolume / 100f; }

	/**
	 * Returns the default hit sound volume.
	 * @return the hit sound volume [0, 1]
	 */
	public static float getHitSoundVolume() { return hitSoundVolume / 100f; }

	/**
	 * Returns the music offset time.
	 * @return the offset (in milliseconds)
	 */
	public static int getMusicOffset() { return musicOffset; }

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
	 * @throws SlickException failure to set display mode
	 */
	public static void setDisplayMode(AppGameContainer app) throws SlickException {
		int screenWidth = app.getScreenWidth();
		int screenHeight = app.getScreenHeight();

		// check for larger-than-screen dimensions
		if (screenWidth < resolution.getWidth() || screenHeight < resolution.getHeight())
			resolution = Resolution.RES_800_600;

		app.setDisplayMode(resolution.getWidth(), resolution.getHeight(), false);

		// set borderless window if dimensions match screen size
		if (screenWidth == resolution.getWidth() && screenHeight == resolution.getHeight())
			System.setProperty("org.lwjgl.opengl.Window.undecorated", "true");
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
	public static boolean isFPSCounterEnabled() { return showFPS; }

	/**
	 * Returns whether or not hit lighting effects are enabled.
	 * @return true if enabled
	 */
	public static boolean isHitLightingEnabled() { return showHitLighting; }

	/**
	 * Returns whether or not combo burst effects are enabled.
	 * @return true if enabled
	 */
	public static boolean isComboBurstEnabled() { return showComboBursts; }

	/**
	 * Returns the port number to bind to.
	 * @return the port
	 */
	public static int getPort() { return port; }

	/**
	 * Returns whether or not the new cursor type is enabled.
	 * @return true if enabled
	 */
	public static boolean isNewCursorEnabled() { return newCursor; }

	/**
	 * Returns whether or not the main menu background should be the current track image.
	 * @return true if enabled
	 */
	public static boolean isDynamicBackgroundEnabled() { return dynamicBackground; }

	/**
	 * Returns whether or not to show perfect hit result bursts.
	 * @return true if enabled
	 */
	public static boolean isPerfectHitBurstEnabled() { return showPerfectHit; }

	/**
	 * Returns the background dim level.
	 * @return the alpha level [0, 1]
	 */
	public static float getBackgroundDim() { return (100 - backgroundDim) / 100f; }

	/**
	 * Returns whether or not to override the song background with the default playfield background.
	 * @return true if forced
	 */
	public static boolean isDefaultPlayfieldForced() { return forceDefaultPlayfield; }

	/**
	 * Returns whether or not beatmap skins are ignored.
	 * @return true if ignored
	 */
	public static boolean isBeatmapSkinIgnored() { return ignoreBeatmapSkins; }

	/**
	 * Returns the fixed circle size override, if any.
	 * @return the CS value (0, 10], 0 if disabled
	 */
	public static float getFixedCS() { return fixedCS; }

	/**
	 * Returns the fixed HP drain rate override, if any.
	 * @return the HP value (0, 10], 0 if disabled
	 */
	public static float getFixedHP() { return fixedHP; }

	/**
	 * Returns the fixed approach rate override, if any.
	 * @return the AR value (0, 10], 0 if disabled
	 */
	public static float getFixedAR() { return fixedAR; }

	/**
	 * Returns the fixed overall difficulty override, if any.
	 * @return the OD value (0, 10], 0 if disabled
	 */
	public static float getFixedOD() { return fixedOD; }

	/**
	 * Returns whether or not to render loading text in the splash screen.
	 * @return true if enabled
	 */
	public static boolean isLoadVerbose() { return loadVerbose; }

	/**
	 * Returns the track checkpoint time.
	 * @return the checkpoint time (in ms)
	 */
	public static int getCheckpoint() { return checkpoint * 1000; }

	/**
	 * Returns whether or not all sound effects are disabled.
	 * @return true if disabled
	 */
	public static boolean isSoundDisabled() { return disableSound; }

	/**
	 * Returns whether or not to use non-English metadata where available.
	 * @return true if Unicode preferred
	 */
	public static boolean useUnicodeMetadata() { return showUnicode; }

	/**
	 * Returns whether or not to play the theme song.
	 * @return true if enabled
	 */
	public static boolean isThemSongEnabled() { return themeSongEnabled; }

	/**
	 * Sets the track checkpoint time, if within bounds.
	 * @param time the track position (in ms)
	 * @return true if within bounds
	 */
	public static boolean setCheckpoint(int time) {
		if (time >= 0 && time < 3600) {
			checkpoint = time;
			return true;
		}
		return false;
	}

	/**
	 * Returns the left game key.
	 * @return the left key code
	 */
	public static int getGameKeyLeft() {
		if (keyLeft == Keyboard.KEY_NONE)
			keyLeft = Input.KEY_Z;
		return keyLeft;
	}

	/**
	 * Returns the right game key.
	 * @return the right key code
	 */
	public static int getGameKeyRight() {
		if (keyRight == Keyboard.KEY_NONE)
			keyRight = Input.KEY_X;
		return keyRight;
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

		oszDir = new File("SongPacks/");
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

		screenshotDir = new File("Screenshots/");
		return screenshotDir;
	}

	/**
	 * Returns the current skin directory.
	 * If invalid, this will create a "Skins" folder in the root directory.
	 * @return the skin directory
	 */
	public static File getSkinDir() {
		if (skinDir != null && skinDir.isDirectory())
			return skinDir;

		skinDir = new File("Skins/");
		skinDir.mkdir();
		return skinDir;
	}

	/**
	 * Returns a dummy OsuFile containing the theme song.
	 * @return the theme song OsuFile
	 */
	public static OsuFile getOsuTheme() {
		String[] tokens = themeString.split(",");
		if (tokens.length != 4) {
			Log.error("Theme song string is malformed.");
			return null;
		}

		OsuFile osu = new OsuFile(null);
		osu.audioFilename = new File(tokens[0]);
		osu.title = tokens[1];
		osu.artist = tokens[2];
		try {
			osu.endTime = Integer.parseInt(tokens[3]);
		} catch (NumberFormatException e) {
			Log.error("Theme song length is not a valid integer", e);
			return null;
		}

		return osu;
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
				case "Skin":
					skinDir = new File(value);
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
//				case "Fullscreen":
//					fullscreen = Boolean.parseBoolean(value);
//					break;
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
					showFPS = Boolean.parseBoolean(value);
					break;
				case "ShowUnicode":
					showUnicode = Boolean.parseBoolean(value);
					break;
				case "NewCursor":
					newCursor = Boolean.parseBoolean(value);
					break;
				case "DynamicBackground":
					dynamicBackground = Boolean.parseBoolean(value);
					break;
				case "LoadVerbose":
					loadVerbose = Boolean.parseBoolean(value);
					break;
				case "VolumeMusic":
					i = Integer.parseInt(value);
					if (i >= 0 && i <= 100)
						musicVolume = i;
					break;
				case "VolumeEffect":
					i = Integer.parseInt(value);
					if (i >= 0 && i <= 100)
						effectVolume = i;
					break;
				case "VolumeHitSound":
					i = Integer.parseInt(value);
					if (i >= 0 && i <= 100)
						hitSoundVolume = i;
					break;
				case "Offset":
					i = Integer.parseInt(value);
					if (i >= -500 && i <= 500)
						musicOffset = i;
					break;
				case "DisableSound":
					disableSound = Boolean.parseBoolean(value);
					break;
				case "keyOsuLeft":
					if ((value.length() == 1 && Character.isLetterOrDigit(value.charAt(0))) ||
						(value.length() == 7 && value.startsWith("NUMPAD"))) {
						i = Keyboard.getKeyIndex(value);
						if (keyRight != i)
							keyLeft = i;
					}
					break;
				case "keyOsuRight":
					if ((value.length() == 1 && Character.isLetterOrDigit(value.charAt(0))) ||
						(value.length() == 7 && value.startsWith("NUMPAD"))) {
						i = Keyboard.getKeyIndex(value);
						if (keyLeft != i)
							keyRight = i;
					}
					break;
				case "DimLevel":
					i = Integer.parseInt(value);
					if (i >= 0 && i <= 100)
						backgroundDim = i;
					break;
				case "ForceDefaultPlayfield":
					forceDefaultPlayfield = Boolean.parseBoolean(value);
					break;
				case "IgnoreBeatmapSkins":
					ignoreBeatmapSkins = Boolean.parseBoolean(value);
					break;
				case "HitLighting":
					showHitLighting = Boolean.parseBoolean(value);
					break;
				case "ComboBurst":
					showComboBursts = Boolean.parseBoolean(value);
					break;
				case "PerfectHit":
					showPerfectHit = Boolean.parseBoolean(value);
					break;
				case "FixedCS":
					fixedCS = Float.parseFloat(value);
					break;
				case "FixedHP":
					fixedHP = Float.parseFloat(value);
					break;
				case "FixedAR":
					fixedAR = Float.parseFloat(value);
					break;
				case "FixedOD":
					fixedOD = Float.parseFloat(value);
					break;
				case "Checkpoint":
					setCheckpoint(Integer.parseInt(value));
					break;
				case "MenuMusic":
					themeSongEnabled = Boolean.parseBoolean(value);
					break;
				}
			}
		} catch (IOException e) {
			Log.error(String.format("Failed to read file '%s'.", OPTIONS_FILE.getAbsolutePath()), e);
		} catch (NumberFormatException e) {
			Log.warn("Format error in options file.", e);
			return;
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
			writer.write(String.format("Skin = %s", getSkinDir().getAbsolutePath()));
			writer.newLine();
			writer.write(String.format("ThemeSong = %s", themeString));
			writer.newLine();
			writer.write(String.format("Port = %d", port));
			writer.newLine();
			writer.write(String.format("ScreenResolution = %s", resolution.toString()));
			writer.newLine();
//			writer.write(String.format("Fullscreen = %b", fullscreen));
//			writer.newLine();
			writer.write(String.format("FrameSync = %d", targetFPS[targetFPSindex]));
			writer.newLine();
			writer.write(String.format("FpsCounter = %b", showFPS));
			writer.newLine();
			writer.write(String.format("ShowUnicode = %b", showUnicode));
			writer.newLine();
			writer.write(String.format("ScreenshotFormat = %d", screenshotFormatIndex));
			writer.newLine();
			writer.write(String.format("NewCursor = %b", newCursor));
			writer.newLine();
			writer.write(String.format("DynamicBackground = %b", dynamicBackground));
			writer.newLine();
			writer.write(String.format("LoadVerbose = %b", loadVerbose));
			writer.newLine();
			writer.write(String.format("VolumeMusic = %d", musicVolume));
			writer.newLine();
			writer.write(String.format("VolumeEffect = %d", effectVolume));
			writer.newLine();
			writer.write(String.format("VolumeHitSound = %d", hitSoundVolume));
			writer.newLine();
			writer.write(String.format("Offset = %d", musicOffset));
			writer.newLine();
			writer.write(String.format("DisableSound = %b", disableSound));
			writer.newLine();
			writer.write(String.format("keyOsuLeft = %s", Keyboard.getKeyName(getGameKeyLeft())));
			writer.newLine();
			writer.write(String.format("keyOsuRight = %s", Keyboard.getKeyName(getGameKeyRight())));
			writer.newLine();
			writer.write(String.format("DimLevel = %d", backgroundDim));
			writer.newLine();
			writer.write(String.format("ForceDefaultPlayfield = %b", forceDefaultPlayfield));
			writer.newLine();
			writer.write(String.format("IgnoreBeatmapSkins = %b", ignoreBeatmapSkins));
			writer.newLine();
			writer.write(String.format("HitLighting = %b", showHitLighting));
			writer.newLine();
			writer.write(String.format("ComboBurst = %b", showComboBursts));
			writer.newLine();
			writer.write(String.format("PerfectHit = %b", showPerfectHit));
			writer.newLine();
			writer.write(String.format(Locale.US, "FixedCS = %.1f", fixedCS));
			writer.newLine();
			writer.write(String.format(Locale.US, "FixedHP = %.1f", fixedHP));
			writer.newLine();
			writer.write(String.format(Locale.US, "FixedAR = %.1f", fixedAR));
			writer.newLine();
			writer.write(String.format(Locale.US, "FixedOD = %.1f", fixedOD));
			writer.newLine();
			writer.write(String.format("Checkpoint = %d", checkpoint));
			writer.newLine();
			writer.write(String.format("MenuMusic = %b", themeSongEnabled));
			writer.newLine();
			writer.close();
		} catch (IOException e) {
			Log.error(String.format("Failed to write to file '%s'.", OPTIONS_FILE.getAbsolutePath()), e);
		}
	}
}
