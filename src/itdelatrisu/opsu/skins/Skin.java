/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014-2017 Jeffrey Han
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

package itdelatrisu.opsu.skins;

import itdelatrisu.opsu.OpsuConstants;

import java.io.File;

import org.newdawn.slick.Color;

/**
 * Skin configuration (skin.ini).
 */
public class Skin {
	/** The default skin name. */
	public static final String DEFAULT_SKIN_NAME = "Default";

	/** Slider styles. */
	public static final byte
		STYLE_PEPPYSLIDER = 1,   // fallback
		STYLE_MMSLIDER = 2,      // default (requires OpenGL 3.0)
		STYLE_TOONSLIDER = 3,    // not implemented
		STYLE_OPENGLSLIDER = 4;  // not implemented

	/** The latest skin version. */
	protected static final float LATEST_VERSION = 2;

	/** The default list of combos with combo sounds. */
	private static final int[] DEFAULT_CUSTOM_COMBO_BURST_SOUNDS = { 50, 75, 100, 200, 300 };

	/** The default combo colors (used when a beatmap does not provide custom colors). */
	private static final Color[] DEFAULT_COMBO = {
		new Color(255, 192, 0),
		new Color(0, 202, 0),
		new Color(18, 124, 255),
		new Color(242, 24, 57)
	};

	/** The default menu visualization bar color. */
	private static final Color DEFAULT_MENU_GLOW = new Color(0, 78, 155);

	/** The default slider border color. */
	private static final Color DEFAULT_SLIDER_BORDER = new Color(255, 255, 255);

	/** The default slider ball color. */
	private static final Color DEFAULT_SLIDER_BALL = new Color(2, 170, 255);

	/** The default spinner approach circle color. */
	private static final Color DEFAULT_SPINNER_APPROACH_CIRCLE = new Color(77, 139, 217);

	/** The default color of the active text in the song selection menu. */
	private static final Color DEFAULT_SONG_SELECT_ACTIVE_TEXT = new Color(255, 255, 255);

	/** The default color of the inactive text in the song selection menu. */
	private static final Color DEFAULT_SONG_SELECT_INACTIVE_TEXT = new Color(178, 178, 178);

	/** The default color of the stars that fall from the cursor during breaks. */
	private static final Color DEFAULT_STAR_BREAK_ADDITIVE = new Color(255, 182, 193);

	/** The default color of the text on the input overlay. */
	private static final Color DEFAULT_INPUT_OVERLAY_TEXT = new Color(0, 0, 0);

	/** The skin directory. */
	private File dir;

	/**
	 * [General]
	 */

	/** The name of the skin. */
	protected String name = OpsuConstants.PROJECT_NAME + " Default Skin";

	/** The skin author. */
	protected String author = "[various authors]";

	/** The skin version. */
	protected float version = LATEST_VERSION;

	/** When a slider has a reverse, should the ball sprite flip horizontally? */
	protected boolean sliderBallFlip = false;

	/** Should the cursor sprite rotate constantly? */
	protected boolean cursorRotate = true;

	/** Should the cursor expand when clicked? */
	protected boolean cursorExpand = true;

	/** Should the cursor have an origin at the center of the image? (if not, the top-left corner is used) */
	protected boolean cursorCentre = true;

	/** The number of frames in the slider ball animation. */
	protected int sliderBallFrames = 10;

	/** Should the hitcircleoverlay sprite be drawn above the hircircle combo number? */
	protected boolean hitCircleOverlayAboveNumber = true;

	/** Should the sound frequency be modulated depending on the spinner score? */
	protected boolean spinnerFrequencyModulate = false;

	/** Should the normal hitsound always be played? */
	protected boolean layeredHitSounds = true;

	/** Should the spinner fade the playfield? */
	protected boolean spinnerFadePlayfield = true;

	/** Should the last spinner bar blink? */
	protected boolean spinnerNoBlink = false;

	/** Should the slider combo color tint the slider ball? */
	protected boolean allowSliderBallTint = false;

	/** The FPS of animations. */
	protected int animationFramerate = -1;

	/** Should the cursor trail sprite rotate constantly? */
	protected boolean cursorTrailRotate = false;

	/** List of combos with combo sounds. */
	protected int[] customComboBurstSounds = DEFAULT_CUSTOM_COMBO_BURST_SOUNDS;

	/** Should the combo burst sprites appear in random order? */
	protected boolean comboBurstRandom = false;

	/** The slider style to use (see STYLE_* constants). */
	protected byte sliderStyle = STYLE_MMSLIDER;

	/**
	 * [Colours]
	 */

	/** Combo colors (max 8). */
	protected Color[] combo = DEFAULT_COMBO;

	/** The menu visualization bar color. */
	protected Color menuGlow = DEFAULT_MENU_GLOW;

	/** The color for the slider border. */
	protected Color sliderBorder = DEFAULT_SLIDER_BORDER;

	/** The slider ball color. */
	protected Color sliderBall = DEFAULT_SLIDER_BALL;

	/** The spinner approach circle color. */
	protected Color spinnerApproachCircle = DEFAULT_SPINNER_APPROACH_CIRCLE;

	/** The color of text in the currently active group in song selection. */
	protected Color songSelectActiveText = DEFAULT_SONG_SELECT_ACTIVE_TEXT;

	/** The color of text in the inactive groups in song selection. */
	protected Color songSelectInactiveText = DEFAULT_SONG_SELECT_INACTIVE_TEXT;

	/** The color of the stars that fall from the cursor (star2 sprite) in breaks. */
	protected Color starBreakAdditive = DEFAULT_STAR_BREAK_ADDITIVE;

	/** The color of the text on the input overlay. */
	protected Color inputOverlayText = DEFAULT_INPUT_OVERLAY_TEXT;

	/**
	 * [Fonts]
	 */

	/** The prefix for the hitcircle font sprites. */
	protected String hitCirclePrefix = "default";

	/** How much should the hitcircle font sprites overlap? */
	protected int hitCircleOverlap = -2;

	/** The prefix for the score font sprites. */
	protected String scorePrefix = "score";

	/** How much should the score font sprites overlap? */
	protected int scoreOverlap = 0;

	/** The prefix for the combo font sprites. */
	protected String comboPrefix = "score";

	/** How much should the combo font sprites overlap? */
	protected int comboOverlap = 0;

	/**
	 * Constructor.
	 * @param dir the skin directory
	 */
	public Skin(File dir) {
		this.dir = dir;
	}

	/**
	 * Returns the skin directory.
	 */
	public File getDirectory() { return dir; }

	/**
	 * Returns the name of the skin.
	 */
	public String getName() { return name; }

	/**
	 * Returns the skin author.
	 */
	public String getAuthor() { return author; }

	/**
	 * Returns the skin version.
	 */
	public float getVersion() { return version; }

	/**
	 * Returns whether the slider ball should be flipped horizontally during a reverse.
	 */
	public boolean isSliderBallFlipped() { return sliderBallFlip; }

	/**
	 * Returns whether the cursor should rotate.
	 */
	public boolean isCursorRotated() { return cursorRotate; }

	/**
	 * Returns whether the cursor should expand when clicked.
	 */
	public boolean isCursorExpanded() { return cursorExpand; }

	/**
	 * Returns whether the cursor should have an origin in the center.
	 * @return {@code true} if center, {@code false} if top-left corner
	 */
	public boolean isCursorCentered() { return cursorCentre; }

	/**
	 * Returns the number of frames in the slider ball animation.
	 */
	public int getSliderBallFrames() { return sliderBallFrames; }

	/**
	 * Returns whether the hit circle overlay should be drawn above the combo number.
	 */
	public boolean isHitCircleOverlayAboveNumber() { return hitCircleOverlayAboveNumber; }

	/**
	 * Returns whether the sound frequency should be modulated depending on the spinner score.
	 */
	public boolean isSpinnerFrequencyModulated() { return spinnerFrequencyModulate; }

	/**
	 * Returns whether the normal hitsound should always be played (and layered on other sounds).
	 */
	public boolean isLayeredHitSounds() { return layeredHitSounds; }

	/**
	 * Returns whether the playfield should fade for spinners.
	 */
	public boolean isSpinnerFadePlayfield() { return spinnerFadePlayfield; }

	/**
	 * Returns whether the last spinner bar should blink.
	 */
	public boolean isSpinnerNoBlink() { return spinnerNoBlink; }

	/**
	 * Returns whether the slider ball should be tinted with the slider combo color.
	 */
	public boolean isAllowSliderBallTint() { return allowSliderBallTint; }

	/**
	 * Returns the frame rate of animations.
	 * @return the FPS, or {@code -1} if not set
	 */
	public int getAnimationFramerate() { return animationFramerate; }

	/**
	 * Returns whether the cursor trail should rotate.
	 */
	public boolean isCursorTrailRotated() { return cursorTrailRotate; }

	/**
	 * Returns a list of combos with combo sounds.
	 */
	public int[] getCustomComboBurstSounds() { return customComboBurstSounds; }

	/**
	 * Returns whether combo bursts should appear in random order.
	 */
	public boolean isComboBurstRandom() { return comboBurstRandom; }

	/**
	 * Returns the slider style.
	 * <ul>
	 * <li>1: peppysliders (segmented)
	 * <li>2: mmsliders (smooth)
	 * <li>3: toonsliders (smooth, with steps instead of gradient)
	 * <li>4: legacy OpenGL-only sliders
	 * </ul>
	 * @return the style (see STYLE_* constants)
	 */
	public byte getSliderStyle() { return sliderStyle; }

	/**
	 * Returns the list of combo colors (max 8).
	 */
	public Color[] getComboColors() { return combo; }

	/**
	 * Returns the menu visualization bar color.
	 */
	public Color getMenuGlowColor() { return menuGlow; }

	/**
	 * Returns the slider border color.
	 */
	public Color getSliderBorderColor() { return sliderBorder; }

	/**
	 * Returns the slider ball color.
	 */
	public Color getSliderBallColor() { return sliderBall; }

	/**
	 * Returns the spinner approach circle color.
	 */
	public Color getSpinnerApproachCircleColor() { return spinnerApproachCircle; }

	/**
	 * Returns the color of the active text in the song selection menu.
	 */
	public Color getSongSelectActiveTextColor() { return songSelectActiveText; }

	/**
	 * Returns the color of the inactive text in the song selection menu.
	 */
	public Color getSongSelectInactiveTextColor() { return songSelectInactiveText; }

	/**
	 * Returns the color of the stars that fall from the cursor during breaks.
	 */
	public Color getStarBreakAdditiveColor() { return starBreakAdditive; }

	/**
	 * Returns the color of the text on the input overlay.
	 */
	public Color getInputOverlayText() { return inputOverlayText; }

	/**
	 * Returns the prefix for the hit circle font sprites.
	 */
	public String getHitCircleFontPrefix() { return hitCirclePrefix; }

	/**
	 * Returns the amount of overlap between the hit circle font sprites.
	 */
	public int getHitCircleFontOverlap() { return hitCircleOverlap; }

	/**
	 * Returns the prefix for the score font sprites.
	 */
	public String getScoreFontPrefix() { return scorePrefix; }

	/**
	 * Returns the amount of overlap between the score font sprites.
	 */
	public int getScoreFontOverlap() { return scoreOverlap; }

	/**
	 * Returns the prefix for the combo font sprites.
	 */
	public String getComboFontPrefix() { return comboPrefix; }

	/**
	 * Returns the amount of overlap between the combo font sprites.
	 */
	public int getComboFontOverlap() { return comboOverlap; }
}
