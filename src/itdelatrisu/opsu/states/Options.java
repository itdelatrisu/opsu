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

import itdelatrisu.opsu.GUIMenuButton;
import itdelatrisu.opsu.Opsu;

import java.awt.Font;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.TrueTypeFont;
import org.newdawn.slick.imageout.ImageOut;
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
	public static final File TMP_DIR = new File(".osu_tmp/");

	/**
	 * Directory for screenshots (created when needed).
	 */
	public static final File SCREENSHOT_DIR = new File("screenshot/");

	/**
	 * File for logging errors.
	 */
	public static final File LOG_FILE = new File(".opsu.log");

	/**
	 * Beatmap directories (where to search for files).
	 */
	private static final String[] BEATMAP_DIRS = {
		"C:/Program Files (x86)/osu!/Songs/",
		"C:/Program Files/osu!/Songs/",
		"songs/"
	};

	/**
	 * The current beatmap directory.
	 */
	private static File beatmapDir;

	/**
	 * File for storing user options.
	 */
	private static final String OPTIONS_FILE = ".opsu.cfg";

	/**
	 * Whether or not any changes were made to options.
	 * If false, the options file will not be modified.
	 */
	private static boolean optionsChanged = false;

	/**
	 * Game colors.
	 */
	public static final Color
		COLOR_BLACK_ALPHA     = new Color(0, 0, 0, 0.5f),
		COLOR_BLUE_DIVIDER    = new Color(49, 94, 237),
		COLOR_BLUE_BACKGROUND = new Color(74, 130, 255),
		COLOR_BLUE_BUTTON     = new Color(50, 189, 237),
		COLOR_ORANGE_BUTTON   = new Color(230, 151, 87),
		COLOR_GREEN_OBJECT    = new Color(26, 207, 26),
		COLOR_BLUE_OBJECT     = new Color(46, 136, 248),
		COLOR_RED_OBJECT      = new Color(243, 48, 77),
		COLOR_ORANGE_OBJECT   = new Color(255, 200, 32);

	/**
	 * The default map colors, used when a map does not provide custom colors.
	 */
	public static final Color[] DEFAULT_COMBO = {
		COLOR_GREEN_OBJECT, COLOR_BLUE_OBJECT,
		COLOR_RED_OBJECT, COLOR_ORANGE_OBJECT
	};

	/**
	 * Game fonts.
	 */
	public static TrueTypeFont
		FONT_DEFAULT, FONT_BOLD,
		FONT_XLARGE, FONT_LARGE, FONT_MEDIUM, FONT_SMALL;

	/**
	 * Game mods.
	 */
	public static final int
		MOD_NO_FAIL       = 0,
		MOD_HARD_ROCK     = 1,
		MOD_SUDDEN_DEATH  = 2,
		MOD_SPUN_OUT      = 3,
		MOD_AUTO          = 4,
		MOD_MAX           = 5;  // not a mod

	/**
	 * Whether a mod is active (indexed by MOD_* constants).
	 */
	private static boolean[] modsActive;

	/**
	 * Mod buttons.
	 */
	private static GUIMenuButton[] modButtons;

	/**
	 * Game option constants.
	 */
	private static final int
		OPTIONS_SCREEN_RESOLUTION    = 0,
//		OPTIONS_FULLSCREEN           = ,
		OPTIONS_TARGET_FPS           = 1,
		OPTIONS_MUSIC_VOLUME         = 2,
		OPTIONS_MUSIC_OFFSET         = 3,
		OPTIONS_SCREENSHOT_FORMAT    = 4,
		OPTIONS_DISPLAY_FPS          = 5,
		OPTIONS_HIT_LIGHTING         = 6,
		OPTIONS_COMBO_BURSTS         = 7,
		OPTIONS_MAX                  = 8;  // not an option

	/**
	 * Screen resolutions.
	 */
	private static final int[][] resolutions = {
		{ 800, 600 },
		{ 1024, 600 },
		{ 1024, 768 },
		{ 1280, 800 },
		{ 1280, 960 },
		{ 1366, 768 },
		{ 1440, 900 },
		{ 1680, 1050 },
		{ 1920, 1080 }
	};

	/**
	 * Index (row) in resolutions[][] array.
	 */
	private static int resolutionIndex = 3;

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
	private static int musicVolume = 20;

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
	 * Back button (shared by other states).
	 */
	private static GUIMenuButton backButton;

	/**
	 * Game option coordinate modifiers (for drawing).
	 */
	private int textY, offsetY;

	// game-related variables
	private static GameContainer container;
	private static StateBasedGame game;
	private Input input;
	private int state;
	private boolean init = false;

	public Options(int state) {
		this.state = state;
	}

	@Override
	public void init(GameContainer container, StateBasedGame game)
			throws SlickException {
		Options.container = container;
		Options.game = game;
		this.input = container.getInput();

		// game settings;
		container.setTargetFrameRate(60);
		container.setMouseCursor("cursor.png", 16, 16);
		container.setMusicVolume(getMusicVolume());
		container.setShowFPS(false);
		container.getInput().enableKeyRepeat();
		container.setAlwaysRender(true);

		// create fonts
		float fontBase;
		if (container.getHeight() <= 600)
			fontBase = 9f;
		else if (container.getHeight() < 800)
			fontBase = 10f;
		else if (container.getHeight() <= 900)
			fontBase = 12f;
		else
			fontBase = 14f;

		Font font    = new Font("Lucida Sans Unicode", Font.PLAIN, (int) (fontBase * 4 / 3));
		FONT_DEFAULT = new TrueTypeFont(font, false);
		FONT_BOLD    = new TrueTypeFont(font.deriveFont(Font.BOLD), false);
		FONT_XLARGE  = new TrueTypeFont(font.deriveFont(fontBase * 4), false);
		FONT_LARGE   = new TrueTypeFont(font.deriveFont(fontBase * 2), false);
		FONT_MEDIUM  = new TrueTypeFont(font.deriveFont(fontBase * 3 / 2), false);
		FONT_SMALL   = new TrueTypeFont(font.deriveFont(fontBase), false);

		int width = container.getWidth();
		int height = container.getHeight();

		// game option coordinate modifiers
		textY = 10 + (FONT_XLARGE.getLineHeight() * 3 / 2);
		offsetY = (int) (((height * 0.8f) - textY) / OPTIONS_MAX);

		// game mods
		modsActive = new boolean[MOD_MAX];
		modButtons = new GUIMenuButton[MOD_MAX];
		Image noFailImage = new Image("selection-mod-nofail.png");
		float modScale = (height * 0.12f) / noFailImage.getHeight();
		noFailImage = noFailImage.getScaledCopy(modScale);
		float modButtonOffsetX = noFailImage.getWidth() * 1.5f;
		float modButtonX = (width / 2f) - (modButtonOffsetX * modButtons.length / 2.75f);
		float modButtonY = (height * 0.8f) + (noFailImage.getHeight() / 2);
		modButtons[MOD_NO_FAIL] = new GUIMenuButton(
				noFailImage, modButtonX, modButtonY
		);
		modButtons[MOD_HARD_ROCK] = new GUIMenuButton(
				new Image("selection-mod-hardrock.png").getScaledCopy(modScale),
				modButtonX + modButtonOffsetX, modButtonY
		);
		modButtons[MOD_SUDDEN_DEATH] = new GUIMenuButton(
				new Image("selection-mod-suddendeath.png").getScaledCopy(modScale),
				modButtonX + (modButtonOffsetX * 2), modButtonY
		);
		modButtons[MOD_SPUN_OUT] = new GUIMenuButton(
				new Image("selection-mod-spunout.png").getScaledCopy(modScale),
				modButtonX + (modButtonOffsetX * 3), modButtonY
		);
		modButtons[MOD_AUTO] = new GUIMenuButton(
				new Image("selection-mod-autoplay.png").getScaledCopy(modScale),
				modButtonX + (modButtonOffsetX * 4), modButtonY
		);
		for (int i = 0; i < modButtons.length; i++)
			modButtons[i].getImage().setAlpha(0.5f);

		// back button
		Image back = new Image("menu-back.png");
		float scale = (height * 0.1f) / back.getHeight();
		back = back.getScaledCopy(scale);
		backButton = new GUIMenuButton(back,
				back.getWidth() / 2f,
				height - (back.getHeight() / 2f));

		game.enterState(Opsu.STATE_MAINMENU, new EmptyTransition(), new FadeInTransition(Color.black));
	}

	@Override
	public void render(GameContainer container, StateBasedGame game, Graphics g)
			throws SlickException {
		if (!init)
			return;

		g.setBackground(COLOR_BLACK_ALPHA);
		g.setColor(Color.white);

		int width = container.getWidth();
		int height = container.getHeight();

		// title
		FONT_XLARGE.drawString(
				(width / 2) - (FONT_XLARGE.getWidth("GAME OPTIONS") / 2),
				10, "GAME OPTIONS"
		);
		FONT_DEFAULT.drawString(
				(width / 2) - (FONT_DEFAULT.getWidth("Click or drag an option to change it.") / 2),
				10 + FONT_XLARGE.getHeight(), "Click or drag an option to change it."
		);

		// game options
		g.setLineWidth(1f);
		g.setFont(FONT_LARGE);
		this.drawOption(g, OPTIONS_SCREEN_RESOLUTION, "Screen Resolution",
				String.format("%dx%d", resolutions[resolutionIndex][0], resolutions[resolutionIndex][1]),
				"Restart to apply resolution changes."
		);
//		this.drawOption(g, OPTIONS_FULLSCREEN, "Fullscreen Mode",
//				fullscreen ? "Yes" : "No",
//				"Restart to apply changes."
//		);
		this.drawOption(g, OPTIONS_TARGET_FPS, "Frame Limiter",
				String.format("%dfps", targetFPS[targetFPSindex]),
				"Higher values may cause high CPU usage."
		);
		this.drawOption(g, OPTIONS_MUSIC_VOLUME, "Music Volume",
				String.format("%d%%", musicVolume),
				"Global music volume."
		);
		this.drawOption(g, OPTIONS_MUSIC_OFFSET, "Music Offset",
				String.format("%dms", musicOffset),
				"Adjust this value if hit objects are out of sync."
		);
		this.drawOption(g, OPTIONS_SCREENSHOT_FORMAT, "Screenshot Format",
				screenshotFormat[screenshotFormatIndex].toUpperCase(),
				"Press F12 to take a screenshot."
		);
		this.drawOption(g, OPTIONS_DISPLAY_FPS, "Show FPS Counter",
				showFPS ? "Yes" : "No",
				null
		);
		this.drawOption(g, OPTIONS_HIT_LIGHTING, "Show Hit Lighting",
				showHitLighting ? "Yes" : "No",
				null
		);
		this.drawOption(g, OPTIONS_COMBO_BURSTS, "Show Combo Bursts",
				showComboBursts ? "Yes" : "No",
				null
		);

		// game mods
		FONT_LARGE.drawString(width * 0.02f, height * 0.8f, "Game Mods:", Color.white);
		for (int i = 0; i < modButtons.length; i++)
			modButtons[i].draw();

		backButton.draw();

		drawFPS();
	}

	@Override
	public void update(GameContainer container, StateBasedGame game, int delta)
			throws SlickException {
		init = true;
	}

	@Override
	public int getID() { return state; }

	@Override
	public void mousePressed(int button, int x, int y) {
		// check mouse button 
		if (button == Input.MOUSE_MIDDLE_BUTTON)
			return;

		// back
		if (backButton.contains(x, y)) {
			game.enterState(Opsu.STATE_SONGMENU, new EmptyTransition(), new FadeInTransition(Color.black));
			return;
		}

		// game mods
		for (int i = 0; i < modButtons.length; i++) {
			if (modButtons[i].contains(x, y)) {
				toggleMod(i);

				// mutually exclusive mods
				if (modsActive[MOD_AUTO]) {
					if (i == MOD_AUTO) {
						if (modsActive[MOD_SPUN_OUT])
							toggleMod(MOD_SPUN_OUT);
						if (modsActive[MOD_SUDDEN_DEATH])
							toggleMod(MOD_SUDDEN_DEATH);
					} else if (i == MOD_SPUN_OUT || i == MOD_SUDDEN_DEATH) {
						if (modsActive[i])
							toggleMod(i);
					}
				} else if (modsActive[MOD_SUDDEN_DEATH] && modsActive[MOD_NO_FAIL])
					toggleMod((i == MOD_SUDDEN_DEATH) ? MOD_NO_FAIL : MOD_SUDDEN_DEATH);

				return;
			}
		}

		// options (click only)
		if (isOptionClicked(OPTIONS_SCREEN_RESOLUTION, y)) {
			resolutionIndex = (resolutionIndex + 1) % resolutions.length;
			return;
		}
//		if (isOptionClicked(OPTIONS_FULLSCREEN, y)) {
//			fullscreen = !fullscreen;
//			return;
//		}
		if (isOptionClicked(OPTIONS_TARGET_FPS, y)) {
			targetFPSindex = (targetFPSindex + 1) % targetFPS.length;
			container.setTargetFrameRate(targetFPS[targetFPSindex]);
			return;
		}
		if (isOptionClicked(OPTIONS_SCREENSHOT_FORMAT, y)) {
			screenshotFormatIndex = (screenshotFormatIndex + 1) % screenshotFormat.length;
			return;
		}
		if (isOptionClicked(OPTIONS_DISPLAY_FPS, y)) {
			showFPS = !showFPS;
			return;
		}
		if (isOptionClicked(OPTIONS_HIT_LIGHTING, y)) {
			showHitLighting = !showHitLighting;
			return;
		}
		if (isOptionClicked(OPTIONS_COMBO_BURSTS, y)) {
			showComboBursts = !showComboBursts;
			return;
		}
	}

	@Override
	public void mouseDragged(int oldx, int oldy, int newx, int newy) {
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
		if (isOptionClicked(OPTIONS_MUSIC_VOLUME, oldy)) {
			musicVolume += diff;
			if (musicVolume < 0)
				musicVolume = 0;
			else if (musicVolume > 100)
				musicVolume = 100;
			container.setMusicVolume(getMusicVolume());
			return;
		}
		if (isOptionClicked(OPTIONS_MUSIC_OFFSET, oldy)) {
			musicOffset += diff;
			if (musicOffset < -500)
				musicOffset = -500;
			else if (musicOffset > 500)
				musicOffset = 500;
			return;
		}
	}

	@Override
	public void keyPressed(int key, char c) {
		switch (key) {
		case Input.KEY_ESCAPE:
			game.enterState(Opsu.STATE_SONGMENU, new EmptyTransition(), new FadeInTransition(Color.black));
			break;
		case Input.KEY_F12:
			Options.takeScreenShot();
			break;
		}
	}

	/**
	 * Draws a game option.
	 * @param g the graphics context
	 * @param pos the element position (OPTIONS_* constants)
	 * @param label the option name
	 * @param value the option value
	 * @param notes additional notes (optional)
	 */
	private void drawOption(Graphics g, int pos, String label, String value, String notes) {
		int width = container.getWidth();
		int textHeight = FONT_LARGE.getHeight();
		float y = textY + (pos * offsetY);

		g.drawString(label, width / 50, y);
		g.drawString(value, width / 2, y);
		g.drawLine(0, y + textHeight, width, y + textHeight);
		if (notes != null)
			FONT_SMALL.drawString(width / 50, y + textHeight, notes);
	}

	/**
	 * Returns whether or not an option was clicked.
	 * @param pos the element position (OPTIONS_* constants)
	 * @param y the y coordinate of the click
	 * @return true if clicked
	 */
	private boolean isOptionClicked(int pos, int y) {
		if (y > textY + (offsetY * pos) - FONT_LARGE.getHeight() &&
			y < textY + (offsetY * pos) + FONT_LARGE.getHeight()) {
			optionsChanged = true;
			return true;
		}
		return false;
	}

	/**
	 * Toggles the active status of a game mod.
	 * Note that this does not perform checks for mutual exclusivity.
	 * @param mod the game mod (MOD_* constants)
	 */
	private static void toggleMod(int mod) {
		modButtons[mod].getImage().setAlpha(modsActive[mod] ? 0.5f : 1.0f);
		modsActive[mod] = !modsActive[mod];
	}

	/**
	 * Returns whether or not a game mod is active.
	 * @param mod the game mod (MOD_* constants)
	 * @return true if the mod is active
	 */
	public static boolean isModActive(int mod) { return modsActive[mod]; }

	/**
	 * Returns the image associated with a game mod.
	 * @param mod the game mod (MOD_* constants)
	 * @return the associated image
	 */
	public static Image getModImage(int mod) { return modButtons[mod].getImage(); }

	/**
	 * Returns the 'back' GUIMenuButton.
	 */
	public static GUIMenuButton getBackButton() { return backButton; }

	/**
	 * Returns the default music volume.
	 * @return the volume [0, 1]
	 */
	public static float getMusicVolume() { return musicVolume / 100f; }

	/**
	 * Returns the music offset time.
	 * @return the offset (in milliseconds)
	 */
	public static int getMusicOffset() { return musicOffset; }

	/**
	 * Draws the FPS at the bottom-right corner of the game container.
	 * If the option is not activated, this will do nothing.
	 */
	public static void drawFPS() {
		if (showFPS) {
			String fps = String.format("FPS: %d", container.getFPS());
			FONT_DEFAULT.drawString(
					container.getWidth() - 15 - FONT_DEFAULT.getWidth(fps),
					container.getHeight() - 15 - FONT_DEFAULT.getHeight(fps),
					fps, Color.white
			);
		}
	}

	/**
	 * Takes a screenshot.
	 * @return true if successful
	 */
	public static boolean takeScreenShot() {
		// TODO: should this be threaded?
		try {
			// create the screenshot directory
			if (!SCREENSHOT_DIR.isDirectory()) {
				if (!SCREENSHOT_DIR.mkdir())
					return false;
			}

			// create file name
			SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd_HHmmss");
			String file = date.format(new Date());

			// copy the screen
			Image screen = new Image(container.getWidth(), container.getHeight());
			container.getGraphics().copyArea(screen, 0, 0);
			ImageOut.write(screen, String.format("%s%sscreenshot_%s.%s",
					SCREENSHOT_DIR.getName(), File.separator,
					file, screenshotFormat[screenshotFormatIndex]), false
			);
			screen.destroy();
		} catch (SlickException e) {
			Log.warn("Failed to take a screenshot.", e);
			return false;
		}
		return true;
	}

	/**
	 * Returns the screen resolution.
	 * @return an array containing the resolution [width, height]
	 */
	public static int[] getContainerSize() { return resolutions[resolutionIndex]; }

//	/**
//	 * Returns whether or not fullscreen mode is enabled.
//	 * @return true if enabled
//	 */
//	public static boolean isFullscreen() { return fullscreen; }

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
	 * Returns the current beatmap directory.
	 * If invalid, this will attempt to search for the directory,
	 * and if nothing found, will create one.
	 */
	public static File getBeatmapDir() {
		if (beatmapDir != null && beatmapDir.isDirectory())
			return beatmapDir;

		// search for directory
		for (int i = 0; i < BEATMAP_DIRS.length; i++) {
			beatmapDir = new File(BEATMAP_DIRS[i]);
			if (beatmapDir.isDirectory()) {
				optionsChanged = true;  // force config file creation
				return beatmapDir;
			}
		}
		beatmapDir.mkdir();  // none found, create new directory
		return beatmapDir;
	}

	/**
	 * Reads user options from the options file, if it exists.
	 */
	public static void parseOptions() {
		// if no config file, use default settings
		File file = new File(OPTIONS_FILE);
		if (!file.isFile()) {
			optionsChanged = true;  // force file creation
			saveOptions();
			optionsChanged = false;
			return;
		}

		try (BufferedReader in = new BufferedReader(new FileReader(file))) {
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
				case "ScreenResolution":
					i = Integer.parseInt(value);
					if (i >= 0 && i < resolutions.length)
						resolutionIndex = i;
					break;
//				case "Fullscreen":
//					fullscreen = Boolean.parseBoolean(value);
//					break;
				case "FrameSync":
					i = Integer.parseInt(value);
					if (i >= 0 && i <= targetFPS.length)
						targetFPSindex = i;
					break;
				case "VolumeMusic":
					i = Integer.parseInt(value);
					if (i >= 0 && i <= 100)
						musicVolume = i;
					break;
				case "Offset":
					i = Integer.parseInt(value);
					if (i >= -500 && i <= 500)
						musicOffset = i;
					break;
				case "ScreenshotFormat":
					i = Integer.parseInt(value);
					if (i >= 0 && i < screenshotFormat.length)
						screenshotFormatIndex = i;
					break;
				case "FpsCounter":
					showFPS = Boolean.parseBoolean(value);
					break;
				case "HitLighting":
					showHitLighting = Boolean.parseBoolean(value);
					break;
				case "ComboBurst":
					showComboBursts = Boolean.parseBoolean(value);
					break;
				}
			}
		} catch (IOException e) {
			Log.error(String.format("Failed to read file '%s'.", OPTIONS_FILE), e);
		} catch (NumberFormatException e) {
			Log.warn("Format error in options file.", e);
			return;
		}
	}

	/**
	 * (Over)writes user options to a file.
	 */
	public static void saveOptions() {
		// only overwrite when needed
		if (!optionsChanged)
			return;

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
			if (beatmapDir != null) {
				writer.write(String.format("BeatmapDirectory = %s", beatmapDir.getAbsolutePath()));
				writer.newLine();
			}
			writer.write(String.format("ScreenResolution = %d", resolutionIndex));
			writer.newLine();
//			writer.write(String.format("Fullscreen = %b", fullscreen));
//			writer.newLine();
			writer.write(String.format("FrameSync = %d", targetFPSindex));
			writer.newLine();
			writer.write(String.format("VolumeMusic = %d", musicVolume));
			writer.newLine();
			writer.write(String.format("Offset = %d", musicOffset));
			writer.newLine();
			writer.write(String.format("ScreenshotFormat = %d", screenshotFormatIndex));
			writer.newLine();
			writer.write(String.format("FpsCounter = %b", showFPS));
			writer.newLine();
			writer.write(String.format("HitLighting = %b", showHitLighting));
			writer.newLine();
			writer.write(String.format("ComboBurst = %b", showComboBursts));
			writer.newLine();
			writer.close();
		} catch (IOException e) {
			Log.error(String.format("Failed to write to file '%s'.", OPTIONS_FILE), e);
		}
	}
}
