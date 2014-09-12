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
import itdelatrisu.opsu.GameMod;
import itdelatrisu.opsu.Opsu;
import itdelatrisu.opsu.OpsuOptions;
import itdelatrisu.opsu.SoundController;
import itdelatrisu.opsu.Utils;

import java.util.concurrent.TimeUnit;

import org.lwjgl.input.Keyboard;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.StateBasedGame;
import org.newdawn.slick.state.transition.EmptyTransition;
import org.newdawn.slick.state.transition.FadeInTransition;
import org.newdawn.slick.util.Log;

/**
 * "Game Options" state.
 */
public class Options extends Utils {
	/**
	 * Game options.
	 */
	private static enum GameOption {
		NULL,
		SCREEN_RESOLUTION,
//		FULLSCREEN,
		TARGET_FPS,
		MUSIC_VOLUME,
		EFFECT_VOLUME,
		HITSOUND_VOLUME,
		MUSIC_OFFSET,
		SCREENSHOT_FORMAT,
		SHOW_FPS,
		SHOW_HIT_LIGHTING,
		SHOW_COMBO_BURSTS,
		NEW_CURSOR,
		DYNAMIC_BACKGROUND,
		SHOW_PERFECT_HIT,
		BACKGROUND_DIM,
		FORCE_DEFAULT_PLAYFIELD,
		IGNORE_BEATMAP_SKINS,
		FIXED_CS,
		FIXED_HP,
		FIXED_AR,
		FIXED_OD,
		LOAD_VERBOSE,
		CHECKPOINT,
		DISABLE_SOUNDS,
		KEY_LEFT,
		KEY_RIGHT,
		SHOW_UNICODE;
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
	private GUIMenuButton[] optionTabs = new GUIMenuButton[TAB_MAX];

	/**
	 * Current tab.
	 */
	private int currentTab;

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
		GameOption.DISABLE_SOUNDS
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

//	/**
//	 * Whether or not the game should run in fullscreen mode.
//	 */
//	private static boolean fullscreen = false;

	/**
	 * Key entry states.
	 */
	private boolean keyEntryLeft = false, keyEntryRight = false;

	/**
	 * Game option coordinate modifiers (for drawing).
	 */
	private int textY, offsetY;

	private Graphics g;
	public Options(int state, OpsuOptions options) {
		super(state, options);
	}

	@Override
	public void init(GameContainer container, StateBasedGame game)
			throws SlickException {
		super.init(container, game);

		this.g = container.getGraphics();

		int width = container.getWidth();
		int height = container.getHeight();

		// game option coordinate modifiers
		textY = 20 + (Utils.FONT_XLARGE.getLineHeight() * 3 / 2);
		offsetY = (int) (((height * 0.8f) - textY) / maxOptionsScreen);

		// option tabs
		Image tab = Utils.getTabImage();
		int subtextWidth = Utils.FONT_DEFAULT.getWidth("Click or drag an option to change it.");
		float tabX = (width / 50) + (tab.getWidth() / 2f);
		float tabY = 15 + Utils.FONT_XLARGE.getLineHeight() + (tab.getHeight() / 2f);
		float tabOffset = Math.min(tab.getWidth(), 
				((width - subtextWidth - tab.getWidth()) / 2) / TAB_MAX);
		for (int i = 0; i < optionTabs.length; i++)
			optionTabs[i] = new GUIMenuButton(tab, tabX + (i * tabOffset), tabY);
	}

	@Override
	public void render(GameContainer container, StateBasedGame game, Graphics g)
			throws SlickException {
		g.setBackground(Utils.COLOR_BLACK_ALPHA);
		g.setColor(Color.white);

		int width = container.getWidth();
		int height = container.getHeight();

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
		g.setColor(Color.white);
		Image tab = optionTabs[0].getImage();
		float tabTextY = optionTabs[0].getY() - (tab.getHeight() / 2f);
		for (int i = optionTabs.length - 1; i >= 0; i--) {
			tab.setAlpha((i == currentTab) ? 1.0f : 0.7f);
			optionTabs[i].draw();
			float tabTextX = optionTabs[i].getX() - (Utils.FONT_MEDIUM.getWidth(TAB_NAMES[i]) / 2);
			Utils.FONT_MEDIUM.drawString(tabTextX, tabTextY, TAB_NAMES[i], Color.white);
		}
		g.setLineWidth(2f);
		float lineY = optionTabs[0].getY() + (tab.getHeight() / 2f);
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

		drawFPS();
		drawCursor();
	}

	@Override
	public void update(GameContainer container, StateBasedGame game, int delta)
			throws SlickException {
		Utils.updateCursor(delta);
	}

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
			SoundController.playSound(SoundController.SOUND_MENUBACK);
			game.enterState(Opsu.STATE_SONGMENU, new EmptyTransition(), new FadeInTransition(Color.black));
			return;
		}

		// option tabs
		for (int i = 0; i < optionTabs.length; i++) {
			if (optionTabs[i].contains(x, y)) {
				if (i != currentTab) {
					currentTab = i;
					SoundController.playSound(SoundController.SOUND_MENUCLICK);
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
					SoundController.playSound(SoundController.SOUND_MENUCLICK);
				return;
			}
		}

		// options (click only)
		switch (getClickedOption(y)) {
		case SCREEN_RESOLUTION:
			do {
				options.resolutionIndex = (options.resolutionIndex + 1) % OpsuOptions.resolutions.length;
			} while (options.resolutionIndex != 0 &&
					(container.getScreenWidth() < OpsuOptions.resolutions[options.resolutionIndex][0] ||
					 container.getScreenHeight() < OpsuOptions.resolutions[options.resolutionIndex][1]));
			break;
//		case FULLSCREEN:
//			fullscreen = !fullscreen;
//			break;
		case TARGET_FPS:
			options.targetFPSindex = (options.targetFPSindex + 1) % OpsuOptions.targetFPS.length;
			container.setTargetFrameRate(options.getTargetFPS());
			break;
		case SCREENSHOT_FORMAT:
			options.screenshotFormatIndex = (options.screenshotFormatIndex + 1) % OpsuOptions.screenshotFormat.length;
			break;
		case SHOW_FPS:
			options.showFPS = !options.showFPS;
			break;
		case SHOW_HIT_LIGHTING:
			options.showHitLighting = !options.showHitLighting;
			break;
		case SHOW_COMBO_BURSTS:
			options.showComboBursts = !options.showComboBursts;
			break;
		case NEW_CURSOR:
			options.newCursor = !options.newCursor;
			try {
				Utils.loadCursor(container, options);
			} catch (SlickException e) {
				Log.error("Failed to load cursor.", e);
			}
			break;
		case DYNAMIC_BACKGROUND:
			options.dynamicBackground = !options.dynamicBackground;
			break;
		case SHOW_PERFECT_HIT:
			options.showPerfectHit = !options.showPerfectHit;
			break;
		case FORCE_DEFAULT_PLAYFIELD:
			options.forceDefaultPlayfield = !options.forceDefaultPlayfield;
			break;
		case IGNORE_BEATMAP_SKINS:
			options.ignoreBeatmapSkins = !options.ignoreBeatmapSkins;
			break;
		case LOAD_VERBOSE:
			options.loadVerbose = !options.loadVerbose;
			break;
		case DISABLE_SOUNDS:
			options.disableSound = !options.disableSound;
			break;
		case KEY_LEFT:
			keyEntryLeft = true;
			keyEntryRight = false;
			break;
		case KEY_RIGHT:
			keyEntryLeft = false;
			keyEntryRight = true;
			break;
		case SHOW_UNICODE:
			options.showUnicode = !options.showUnicode;
			if (options.showUnicode) {
				try {
					Utils.FONT_LARGE.loadGlyphs();
					Utils.FONT_MEDIUM.loadGlyphs();
					Utils.FONT_DEFAULT.loadGlyphs();
				} catch (SlickException e) {
					Log.warn("Failed to load glyphs.", e);
				}
			}
			break;
		default:
			break;
		}
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
		switch (getClickedOption(oldy)) {
		case MUSIC_VOLUME:
			options.musicVolume = getBoundedValue(options.musicVolume, diff, 0, 100);
			container.setMusicVolume(options.getMusicVolume());
			break;
		case EFFECT_VOLUME:
			options.effectVolume = getBoundedValue(options.effectVolume, diff, 0, 100);
			break;
		case HITSOUND_VOLUME:
			options.hitSoundVolume = getBoundedValue(options.hitSoundVolume, diff, 0, 100);
			break;
		case MUSIC_OFFSET:
			options.musicOffset = getBoundedValue(options.musicOffset, diff, -500, 500);
			break;
		case BACKGROUND_DIM:
			options.backgroundDim = getBoundedValue(options.backgroundDim, diff, 0, 100);
			break;
		case FIXED_CS:
			options.fixedCS = getBoundedValue(options.fixedCS, diff / 10f, 0f, 10f);
			break;
		case FIXED_HP:
			options.fixedHP = getBoundedValue(options.fixedHP, diff / 10f, 0f, 10f);
			break;
		case FIXED_AR:
			options.fixedAR = getBoundedValue(options.fixedAR, diff / 10f, 0f, 10f);
			break;
		case FIXED_OD:
			options.fixedOD = getBoundedValue(options.fixedOD, diff / 10f, 0f, 10f);
			break;
		case CHECKPOINT:
			options.checkpoint = getBoundedValue(options.checkpoint, diff * multiplier, 0, 3599);
			break;
		default:
			break;
		}
	}

	/**
	 * Returns a bounded value for when an option is dragged.
	 * @param var the initial value
	 * @param diff the value change
	 * @param min the minimum value
	 * @param max the maximum value
	 * @return the bounded value
	 */
	private int getBoundedValue(int var, int diff, int min, int max) {
		int val = var + diff;
		if (val < min)
			val = min;
		else if (val > max)
			val = max;
		return val;
	}

	/**
	 * Returns a bounded value for when an option is dragged.
	 * @param var the initial value
	 * @param diff the value change
	 * @param min the minimum value
	 * @param max the maximum value
	 * @return the bounded value
	 */
	private float getBoundedValue(float var, float diff, float min, float max) {
		float val = var + diff;
		if (val < min)
			val = min;
		else if (val > max)
			val = max;
		return val;
	}

	@Override
	public void keyPressed(int key, char c) {
		// key entry state
		if (keyEntryLeft || keyEntryRight) {
			if (Character.isLetterOrDigit(c)) {
				if (keyEntryLeft && options.keyRight != key)
					options.keyLeft = key;
				else if (keyEntryRight && options.keyLeft != key)
					options.keyRight = key;
			}
			keyEntryLeft = keyEntryRight = false;
			return;
		}

		switch (key) {
		case Input.KEY_ESCAPE:
			SoundController.playSound(SoundController.SOUND_MENUBACK);
			game.enterState(Opsu.STATE_SONGMENU, new EmptyTransition(), new FadeInTransition(Color.black));
			break;
		case Input.KEY_F12:
			takeScreenShot();
			break;
		case Input.KEY_TAB:
			int i = 1;
			if (input.isKeyDown(Input.KEY_LSHIFT) || input.isKeyDown(Input.KEY_RSHIFT))
				i = TAB_MAX - 1;
			currentTab = (currentTab + i) % TAB_MAX;
			SoundController.playSound(SoundController.SOUND_MENUCLICK);
			break;
		}
	}

	@Override
	public void enter(GameContainer container, StateBasedGame game)
			throws SlickException {
		currentTab = TAB_DISPLAY;
	}

	/**
	 * Draws a game option.
	 * @param option the option (OPTION_* constant)
	 * @param pos the position to draw at
	 */
	private void drawOption(GameOption option, int pos) {
		switch (option) {
		case SCREEN_RESOLUTION:
			drawOption(pos, "Screen Resolution",
					String.format("%dx%d", OpsuOptions.resolutions[options.resolutionIndex][0], OpsuOptions.resolutions[options.resolutionIndex][1]),
					"Restart to apply resolution changes."
			);
			break;
//		case FULLSCREEN:
//			drawOption(pos, "Fullscreen Mode",
//					fullscreen ? "Yes" : "No",
//					"Restart to apply changes."
//			);
//			break;
		case TARGET_FPS:
			drawOption(pos, "Frame Limiter",
					String.format("%dfps", options.getTargetFPS()),
					"Higher values may cause high CPU usage."
			);
			break;
		case SCREENSHOT_FORMAT:
			drawOption(pos, "Screenshot Format",
					OpsuOptions.screenshotFormat[options.screenshotFormatIndex].toUpperCase(),
					"Press F12 to take a screenshot."
			);
			break;
		case SHOW_FPS:
			drawOption(pos, "Show FPS Counter",
					options.showFPS ? "Yes" : "No",
					"Show an FPS counter in the bottom-right hand corner."
			);
			break;
		case SHOW_UNICODE:
			drawOption(pos, "Prefer Non-English Metadata",
					options.showUnicode ? "Yes" : "No",
					"Where available, song titles will be shown in their native language."
			);
			break;
		case NEW_CURSOR:
			drawOption(pos, "Enable New Cursor",
					options.newCursor ? "Yes" : "No",
					"Use the new cursor style (may cause higher CPU usage)."
			);
			break;
		case DYNAMIC_BACKGROUND:
			drawOption(pos, "Enable Dynamic Backgrounds",
					options.dynamicBackground ? "Yes" : "No",
					"The song background will be used as the main menu background."
			);
			break;
		case LOAD_VERBOSE:
			drawOption(pos, "Show Detailed Loading Progress",
					options.loadVerbose ? "Yes" : "No",
					"Display more specific loading information in the splash screen."
			);
			break;
		case MUSIC_VOLUME:
			drawOption(pos, "Music Volume",
					String.format("%d%%", options.musicVolume),
					"Global music volume."
			);
			break;
		case EFFECT_VOLUME:
			drawOption(pos, "Effect Volume",
					String.format("%d%%", options.effectVolume),
					"Volume of menu and game sounds."
			);
			break;
		case HITSOUND_VOLUME:
			drawOption(pos, "Hit Sound Volume",
					String.format("%d%%", options.hitSoundVolume),
					"Volume of hit sounds."
			);
			break;
		case MUSIC_OFFSET:
			drawOption(pos, "Music Offset",
					String.format("%dms", options.musicOffset),
					"Adjust this value if hit objects are out of sync."
			);
			break;
		case DISABLE_SOUNDS:
			drawOption(pos, "Disable All Sound Effects",
					options.disableSound ? "Yes" : "No",
					"May resolve Linux sound driver issues.  Requires a restart."
			);
			break;
		case KEY_LEFT:
			drawOption(pos, "Left Game Key",
					Keyboard.getKeyName(options.getGameKeyLeft()),
					"Select this option to input a key."
			);
			break;
		case KEY_RIGHT:
			drawOption(pos, "Right Game Key",
					Keyboard.getKeyName(options.getGameKeyRight()),
					"Select this option to input a key."
			);
			break;
		case BACKGROUND_DIM:
			drawOption(pos, "Background Dim",
					String.format("%d%%", options.backgroundDim),
					"Percentage to dim the background image during gameplay."
			);
			break;
		case FORCE_DEFAULT_PLAYFIELD:
			drawOption(pos, "Force Default Playfield",
					options.forceDefaultPlayfield ? "Yes" : "No",
					"Override the song background with the default playfield background."
			);
			break;
		case IGNORE_BEATMAP_SKINS:
			drawOption(pos, "Ignore All Beatmap Skins",
					options.ignoreBeatmapSkins ? "Yes" : "No",
					"Never use skin element overrides provided by beatmaps."
			);
			break;
		case SHOW_HIT_LIGHTING:
			drawOption(pos, "Show Hit Lighting",
					options.showHitLighting ? "Yes" : "No",
					"Adds an effect behind hit explosions."
			);
			break;
		case SHOW_COMBO_BURSTS:
			drawOption(pos, "Show Combo Bursts",
					options.showComboBursts ? "Yes" : "No",
					"A character image is displayed at combo milestones."
			);
			break;
		case SHOW_PERFECT_HIT:
			drawOption(pos, "Show Perfect Hits",
					options.showPerfectHit ? "Yes" : "No",
					"Whether to show perfect hit result bursts (300s, slider ticks)."
			);
			break;
		case FIXED_CS:
			drawOption(pos, "Fixed Circle Size (CS)",
					(options.fixedCS == 0f) ? "Disabled" : String.format("%.1f", options.fixedCS),
					"Determines the size of circles and sliders."
			);
			break;
		case FIXED_HP:
			drawOption(pos, "Fixed HP Drain Rate (HP)",
					(options.fixedHP == 0f) ? "Disabled" : String.format("%.1f", options.fixedHP),
					"Determines the rate at which health decreases."
			);
			break;
		case FIXED_AR:
			drawOption(pos, "Fixed Approach Rate (AR)",
					(options.fixedAR == 0f) ? "Disabled" : String.format("%.1f", options.fixedAR),
					"Determines how long hit circles stay on the screen."
			);
			break;
		case FIXED_OD:
			drawOption(pos, "Fixed Overall Difficulty (OD)",
					(options.fixedOD == 0f) ? "Disabled" : String.format("%.1f", options.fixedOD),
					"Determines the time window for hit results."
			);
			break;
		case CHECKPOINT:
			drawOption(pos, "Track Checkpoint",
					(options.checkpoint == 0) ? "Disabled" : String.format("%02d:%02d",
							TimeUnit.SECONDS.toMinutes(options.checkpoint),
							options.checkpoint - TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(options.checkpoint))),
					"Press CTRL+L while playing to load a checkpoint, and CTRL+S to set one."
			);
			break;
		default:
			break;
		}
	}

	/**
	 * Draws a game option.
	 * @param pos the element position
	 * @param label the option name
	 * @param value the option value
	 * @param notes additional notes
	 */
	private void drawOption(int pos, String label, String value, String notes) {
		int width = container.getWidth();
		int textHeight = Utils.FONT_LARGE.getLineHeight();
		float y = textY + (pos * offsetY);

		g.setColor(Color.white);
		g.drawString(label, width / 30, y);
		g.drawString(value, width / 2, y);
		Utils.FONT_SMALL.drawString(width / 30, y + textHeight, notes);
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

//	/**
//	 * Returns whether or not fullscreen mode is enabled.
//	 * @return true if enabled
//	 */
//	public static boolean isFullscreen() { return fullscreen; }

}
