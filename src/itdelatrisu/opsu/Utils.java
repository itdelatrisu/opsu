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

import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.audio.SoundEffect;
import itdelatrisu.opsu.downloads.DownloadNode;

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Cursor;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.newdawn.slick.Animation;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.UnicodeFont;
import org.newdawn.slick.font.effects.ColorEffect;
import org.newdawn.slick.font.effects.Effect;
import org.newdawn.slick.state.StateBasedGame;
import org.newdawn.slick.util.Log;
import org.newdawn.slick.util.ResourceLoader;

import com.sun.jna.platform.FileUtils;

/**
 * Contains miscellaneous utilities.
 */
public class Utils {
	/** Game colors. */
	public static final Color
		COLOR_BLACK_ALPHA     = new Color(0, 0, 0, 0.5f),
		COLOR_WHITE_ALPHA     = new Color(255, 255, 255, 0.5f),
		COLOR_BLUE_DIVIDER    = new Color(49, 94, 237),
		COLOR_BLUE_BACKGROUND = new Color(74, 130, 255),
		COLOR_BLUE_BUTTON     = new Color(40, 129, 237),
		COLOR_ORANGE_BUTTON   = new Color(200, 90, 3),
		COLOR_GREEN_OBJECT    = new Color(26, 207, 26),
		COLOR_BLUE_OBJECT     = new Color(46, 136, 248),
		COLOR_RED_OBJECT      = new Color(243, 48, 77),
		COLOR_ORANGE_OBJECT   = new Color(255, 200, 32),
		COLOR_YELLOW_ALPHA    = new Color(255, 255, 0, 0.4f),
		COLOR_WHITE_FADE      = new Color(255, 255, 255, 1f),
		COLOR_RED_HOVER       = new Color(255, 112, 112),
		COLOR_GREEN           = new Color(137, 201, 79),
		COLOR_LIGHT_ORANGE    = new Color(255,192,128),
		COLOR_LIGHT_GREEN     = new Color(128,255,128),
		COLOR_LIGHT_BLUE      = new Color(128,128,255),
		COLOR_GREEN_SEARCH    = new Color(173, 255, 47);

	/** The default map colors, used when a map does not provide custom colors. */
	public static final Color[] DEFAULT_COMBO = {
		COLOR_ORANGE_OBJECT, COLOR_GREEN_OBJECT,
		COLOR_BLUE_OBJECT, COLOR_RED_OBJECT,
	};

	/** Game fonts. */
	public static UnicodeFont
		FONT_DEFAULT, FONT_BOLD,
		FONT_XLARGE, FONT_LARGE, FONT_MEDIUM, FONT_SMALL;

	/** Back button (shared by other states). */
	private static MenuButton backButton;

	/** Last cursor coordinates. */
	private static int lastX = -1, lastY = -1;

	/** Cursor rotation angle. */
	private static float cursorAngle = 0f;

	/** Stores all previous cursor locations to display a trail. */
	private static LinkedList<Integer>
		cursorX = new LinkedList<Integer>(),
		cursorY = new LinkedList<Integer>();

	/** Time to show volume image, in milliseconds. */
	private static final int VOLUME_DISPLAY_TIME = 1500;

	/** Volume display elapsed time. */
	private static int volumeDisplay = -1;

	/** Set of all Unicode strings already loaded. */
	private static HashSet<String> loadedGlyphs = new HashSet<String>();

	/**
	 * List of illegal filename characters.
	 * @see #cleanFileName(String, char)
	 */
	private final static int[] illegalChars = {
		34, 60, 62, 124, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
		11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23,
		24, 25, 26, 27, 28, 29, 30, 31, 58, 42, 63, 92, 47
	};
	static {
	    Arrays.sort(illegalChars);
	}

	// game-related variables
	private static GameContainer container;
	private static StateBasedGame game;
	private static Input input;

	// This class should not be instantiated.
	private Utils() {}

	/**
	 * Initializes game settings and class data.
	 * @param container the game container
	 * @param game the game object
	 * @throws SlickException
	 */
	public static void init(GameContainer container, StateBasedGame game)
			throws SlickException {
		Utils.container = container;
		Utils.game = game;
		Utils.input = container.getInput();

		// game settings
		container.setTargetFrameRate(Options.getTargetFPS());
		container.setVSync(Options.getTargetFPS() == 60);
		container.setMusicVolume(Options.getMusicVolume() * Options.getMasterVolume());
		container.setShowFPS(false);
		container.getInput().enableKeyRepeat();
		container.setAlwaysRender(true);

		int width = container.getWidth();
		int height = container.getHeight();

		// set the cursor
		try {
			// hide the native cursor
			int min = Cursor.getMinCursorSize();
			IntBuffer tmp = BufferUtils.createIntBuffer(min * min);
			Cursor emptyCursor = new Cursor(min, min, min/2, min/2, 1, tmp, null);
			container.setMouseCursor(emptyCursor, 0, 0);
		} catch (LWJGLException e) {
			ErrorHandler.error("Failed to set the cursor.", e, true);
		}

		// create fonts
		float fontBase;
		if (height <= 600)
			fontBase = 10f;
		else if (height < 800)
			fontBase = 11f;
		else if (height <= 900)
			fontBase = 13f;
		else
			fontBase = 15f;

		try {
			Font javaFont = Font.createFont(Font.TRUETYPE_FONT, ResourceLoader.getResourceAsStream(Options.FONT_NAME));
			Font font    = javaFont.deriveFont(Font.PLAIN, (int) (fontBase * 4 / 3));
			FONT_DEFAULT = new UnicodeFont(font);
			FONT_BOLD    = new UnicodeFont(font.deriveFont(Font.BOLD));
			FONT_XLARGE  = new UnicodeFont(font.deriveFont(fontBase * 3));
			FONT_LARGE   = new UnicodeFont(font.deriveFont(fontBase * 2));
			FONT_MEDIUM  = new UnicodeFont(font.deriveFont(fontBase * 3 / 2));
			FONT_SMALL   = new UnicodeFont(font.deriveFont(fontBase));
			ColorEffect colorEffect = new ColorEffect();
			loadFont(FONT_DEFAULT, 2, colorEffect);
			loadFont(FONT_BOLD, 2, colorEffect);
			loadFont(FONT_XLARGE, 4, colorEffect);
			loadFont(FONT_LARGE, 4, colorEffect);
			loadFont(FONT_MEDIUM, 3, colorEffect);
			loadFont(FONT_SMALL, 1, colorEffect);
		} catch (Exception e) {
			ErrorHandler.error("Failed to load fonts.", e, true);
		}

		// initialize game images
		GameImage.init(width, height);
		for (GameImage img : GameImage.values()) {
			if (img.isPreload())
				img.setDefaultImage();
		}

		// initialize game mods
		GameMod.init(width, height);

		// initialize hit objects
		OsuHitObject.init(width, height);

		// initialize download nodes
		DownloadNode.init(width, height);

		// back button
		if (GameImage.MENU_BACK.getImages() != null) {
			Animation back = GameImage.MENU_BACK.getAnimation(120);
			backButton = new MenuButton(back, back.getWidth() / 2f, height - (back.getHeight() / 2f));
		} else {
			Image back = GameImage.MENU_BACK.getImage();
			backButton = new MenuButton(back, back.getWidth() / 2f, height - (back.getHeight() / 2f));
		}
		backButton.setHoverExpand(MenuButton.Expand.UP_RIGHT);
	}

	/**
	 * Returns the 'menu-back' MenuButton.
	 */
	public static MenuButton getBackButton() { return backButton; }

	/**
	 * Draws a tab image and text centered at a location.
	 * @param x the center x coordinate
	 * @param y the center y coordinate
	 * @param text the text to draw inside the tab
	 * @param selected whether the tab is selected (white) or not (red)
	 * @param isHover whether to include a hover effect (unselected only)
	 */
	public static void drawTab(float x, float y, String text, boolean selected, boolean isHover) {
		Image tabImage = GameImage.MENU_TAB.getImage();
		float tabTextX = x - (Utils.FONT_MEDIUM.getWidth(text) / 2);
		float tabTextY = y - (tabImage.getHeight() / 2.5f);
		Color filter, textColor;
		if (selected) {
			filter = Color.white;
			textColor = Color.black;
		} else {
			filter = (isHover) ? Utils.COLOR_RED_HOVER : Color.red;
			textColor = Color.white;
		}
		tabImage.drawCentered(x, y, filter);
		Utils.FONT_MEDIUM.drawString(tabTextX, tabTextY, text, textColor);
	}

	/**
	 * Draws an animation based on its center.
	 * @param anim the animation to draw
	 * @param x the center x coordinate
	 * @param y the center y coordinate
	 */
	public static void drawCentered(Animation anim, float x, float y) {
		anim.draw(x - (anim.getWidth() / 2f), y - (anim.getHeight() / 2f));
	}

	/**
	 * Returns a bounded value for a base value and displacement.
	 * @param base the initial value
	 * @param diff the value change
	 * @param min the minimum value
	 * @param max the maximum value
	 * @return the bounded value
	 */
	public static int getBoundedValue(int base, int diff, int min, int max) {
		int val = base + diff;
		if (val < min)
			val = min;
		else if (val > max)
			val = max;
		return val;
	}

	/**
	 * Returns a bounded value for a base value and displacement.
	 * @param base the initial value
	 * @param diff the value change
	 * @param min the minimum value
	 * @param max the maximum value
	 * @return the bounded value
	 */
	public static float getBoundedValue(float base, float diff, float min, float max) {
		float val = base + diff;
		if (val < min)
			val = min;
		else if (val > max)
			val = max;
		return val;
	}

	/**
	 * Clamps a value between a lower and upper bound.
	 * @param val the value to clamp
	 * @param low the lower bound
	 * @param high the upper bound
	 * @return the clamped value
	 * @author fluddokt
	 */
	public static float clamp(float val, int low, int high) {
		if (val < low)
			return low;
		if (val > high)
			return high;
		return val;
	}

	/**
	 * Draws the cursor.
	 */
	public static void drawCursor() {
		// determine correct cursor image
		// TODO: most beatmaps don't skin CURSOR_MIDDLE, so how to determine style?
		Image cursor = null, cursorMiddle = null, cursorTrail = null;
		boolean skinned = GameImage.CURSOR.hasSkinImage();
		boolean newStyle = (skinned) ? true : Options.isNewCursorEnabled();
		if (skinned || newStyle) {
			cursor = GameImage.CURSOR.getImage();
			cursorTrail = GameImage.CURSOR_TRAIL.getImage();
		} else {
			cursor = GameImage.CURSOR_OLD.getImage();
			cursorTrail = GameImage.CURSOR_TRAIL_OLD.getImage();
		}
		if (newStyle)
			cursorMiddle = GameImage.CURSOR_MIDDLE.getImage();

		int mouseX = input.getMouseX(), mouseY = input.getMouseY();
		int removeCount = 0;
		int FPSmod = (Options.getTargetFPS() / 60);

		// TODO: use an image buffer
		if (newStyle) {
			// new style: add all points between cursor movements
			if (lastX < 0) {
				lastX = mouseX;
				lastY = mouseY;
				return;
			}
			addCursorPoints(lastX, lastY, mouseX, mouseY);
			lastX = mouseX;
			lastY = mouseY;

			removeCount = (cursorX.size() / (6 * FPSmod)) + 1;
		} else {
			// old style: sample one point at a time
			cursorX.add(mouseX);
			cursorY.add(mouseY);

			int max = 10 * FPSmod;
			if (cursorX.size() > max)
				removeCount = cursorX.size() - max;
		}

		// remove points from the lists
		for (int i = 0; i < removeCount && !cursorX.isEmpty(); i++) {
			cursorX.remove();
			cursorY.remove();
		}

		// draw a fading trail
		float alpha = 0f;
		float t = 2f / cursorX.size();
		Iterator<Integer> iterX = cursorX.iterator();
		Iterator<Integer> iterY = cursorY.iterator();
		while (iterX.hasNext()) {
			int cx = iterX.next();
			int cy = iterY.next();
			alpha += t;
			cursorTrail.setAlpha(alpha);
//			if (cx != x || cy != y)
				cursorTrail.drawCentered(cx, cy);
		}
		cursorTrail.drawCentered(mouseX, mouseY);

		// increase the cursor size if pressed
		final float scale = 1.25f;
		int state = game.getCurrentStateID();
		if (((state == Opsu.STATE_GAME || state == Opsu.STATE_GAMEPAUSEMENU) && isGameKeyPressed()) ||
		    ((input.isMouseButtonDown(Input.MOUSE_LEFT_BUTTON) || input.isMouseButtonDown(Input.MOUSE_RIGHT_BUTTON)) &&
		    !(state == Opsu.STATE_GAME && Options.isMouseDisabled()))) {
			cursor = cursor.getScaledCopy(scale);
			if (newStyle)
				cursorMiddle = cursorMiddle.getScaledCopy(scale);
		}

		// draw the other components
		if (newStyle)
			cursor.setRotation(cursorAngle);
		cursor.drawCentered(mouseX, mouseY);
		if (newStyle)
			cursorMiddle.drawCentered(mouseX, mouseY);
	}

	/**
	 * Adds all points between (x1, y1) and (x2, y2) to the cursor point lists.
	 * @author http://rosettacode.org/wiki/Bitmap/Bresenham's_line_algorithm#Java
	 */
	private static void addCursorPoints(int x1, int y1, int x2, int y2) {
		// delta of exact value and rounded value of the dependent variable
		int d = 0;
		int dy = Math.abs(y2 - y1);
		int dx = Math.abs(x2 - x1);

		int dy2 = (dy << 1);  // slope scaling factors to avoid floating
		int dx2 = (dx << 1);  // point
		int ix = x1 < x2 ? 1 : -1;  // increment direction
		int iy = y1 < y2 ? 1 : -1;

		int k = 5;  // sample size
		if (dy <= dx) {
			for (int i = 0; ; i++) {
				if (i == k) {
					cursorX.add(x1);
					cursorY.add(y1);
					i = 0;
				}
				if (x1 == x2)
					break;
				x1 += ix;
				d += dy2;
				if (d > dx) {
					y1 += iy;
					d -= dx2;
				}
			}
		} else {
			for (int i = 0; ; i++) {
				if (i == k) {
					cursorX.add(x1);
					cursorY.add(y1);
					i = 0;
				}
				if (y1 == y2)
					break;
				y1 += iy;
				d += dx2;
				if (d > dy) {
					x1 += ix;
					d -= dy2;
				}
			}
		}
	}

	/**
	 * Rotates the cursor by a degree determined by a delta interval.
	 * If the old style cursor is being used, this will do nothing.
	 * @param delta the delta interval since the last call
	 */
	public static void updateCursor(int delta) {
		cursorAngle += delta / 40f;
		cursorAngle %= 360;
	}

	/**
	 * Resets all cursor data and skins.
	 */
	public static void resetCursor() {
		GameImage.CURSOR.destroySkinImage();
		GameImage.CURSOR_MIDDLE.destroySkinImage();
		GameImage.CURSOR_TRAIL.destroySkinImage();
		cursorAngle = 0f;
		lastX = lastY = -1;
		cursorX.clear();
		cursorY.clear();
		GameImage.CURSOR.getImage().setRotation(0f);
	}

	/**
	 * Returns true if a game input key is pressed (mouse/keyboard left/right).
	 * @return true if pressed
	 */
	public static boolean isGameKeyPressed() {
		boolean mouseDown = !Options.isMouseDisabled() && (
				input.isMouseButtonDown(Input.MOUSE_LEFT_BUTTON) ||
				input.isMouseButtonDown(Input.MOUSE_RIGHT_BUTTON));
		return (mouseDown ||
				input.isKeyDown(Options.getGameKeyLeft()) ||
				input.isKeyDown(Options.getGameKeyRight()));
	}

	/**
	 * Draws the FPS at the bottom-right corner of the game container.
	 * If the option is not activated, this will do nothing.
	 */
	public static void drawFPS() {
		if (!Options.isFPSCounterEnabled())
			return;

		String fps = String.format("%dFPS", container.getFPS());
		FONT_BOLD.drawString(
				container.getWidth() * 0.997f - FONT_BOLD.getWidth(fps),
				container.getHeight() * 0.997f - FONT_BOLD.getHeight(fps),
				Integer.toString(container.getFPS()), Color.white
		);
		FONT_DEFAULT.drawString(
				container.getWidth() * 0.997f - FONT_BOLD.getWidth("FPS"),
				container.getHeight() * 0.997f - FONT_BOLD.getHeight("FPS"),
				"FPS", Color.white
		);
	}

	/**
	 * Draws the volume bar on the middle right-hand side of the game container.
	 * Only draws if the volume has recently been changed using with {@link #changeVolume(int)}.
	 * @param g the graphics context
	 */
	public static void drawVolume(Graphics g) {
		if (volumeDisplay == -1)
			return;

		int width = container.getWidth(), height = container.getHeight();
		Image img = GameImage.VOLUME.getImage();

		// move image in/out
		float xOffset = 0;
		float ratio = (float) volumeDisplay / VOLUME_DISPLAY_TIME;
		if (ratio <= 0.1f)
			xOffset = img.getWidth() * (1 - (ratio * 10f));
		else if (ratio >= 0.9f)
			xOffset = img.getWidth() * (1 - ((1 - ratio) * 10f));

		img.drawCentered(width - img.getWidth() / 2f + xOffset, height / 2f);
		float barHeight = img.getHeight() * 0.9f;
		float volume = Options.getMasterVolume();
		g.setColor(Color.white);
		g.fillRoundRect(
				width - (img.getWidth() * 0.368f) + xOffset,
				(height / 2f) - (img.getHeight() * 0.47f) + (barHeight * (1 - volume)),
				img.getWidth() * 0.15f, barHeight * volume, 3
		);
	}

	/**
	 * Updates volume display by a delta interval.
	 * @param delta the delta interval since the last call
	 */
	public static void updateVolumeDisplay(int delta) {
		if (volumeDisplay == -1)
			return;

		volumeDisplay += delta;
		if (volumeDisplay > VOLUME_DISPLAY_TIME)
			volumeDisplay = -1;
	}

	/**
	 * Changes the master volume by a unit (positive or negative).
	 * @param units the number of units
	 */
	public static void changeVolume(int units) {
		final float UNIT_OFFSET = 0.05f;
		Options.setMasterVolume(container, Utils.getBoundedValue(Options.getMasterVolume(), UNIT_OFFSET * units, 0f, 1f));
		if (volumeDisplay == -1)
			volumeDisplay = 0;
		else if (volumeDisplay >= VOLUME_DISPLAY_TIME / 10)
			volumeDisplay = VOLUME_DISPLAY_TIME / 10;
	}

	/**
	 * Draws loading progress (OSZ unpacking, OsuFile parsing, sound loading)
	 * at the bottom of the screen.
	 */
	public static void drawLoadingProgress(Graphics g) {
		String text, file;
		int progress;

		// determine current action
		if ((file = OszUnpacker.getCurrentFileName()) != null) {
			text = "Unpacking new beatmaps...";
			progress = OszUnpacker.getUnpackerProgress();
		} else if ((file = OsuParser.getCurrentFileName()) != null) {
			text = (OsuParser.isUpdatingDatabase()) ? "Updating database..." : "Loading beatmaps...";
			progress = OsuParser.getParserProgress();
		} else if ((file = SoundController.getCurrentFileName()) != null) {
			text = "Loading sounds...";
			progress = SoundController.getLoadingProgress();
		} else
			return;

		// draw loading info
		float marginX = container.getWidth() * 0.02f, marginY = container.getHeight() * 0.02f;
		float lineY = container.getHeight() - marginY;
		int lineOffsetY = Utils.FONT_MEDIUM.getLineHeight();
		if (Options.isLoadVerbose()) {
			// verbose: display percentages and file names
			Utils.FONT_MEDIUM.drawString(
					marginX, lineY - (lineOffsetY * 2),
					String.format("%s (%d%%)", text, progress), Color.white);
			Utils.FONT_MEDIUM.drawString(marginX, lineY - lineOffsetY, file, Color.white);
		} else {
			// draw loading bar
			Utils.FONT_MEDIUM.drawString(marginX, lineY - (lineOffsetY * 2), text, Color.white);
			g.setColor(Color.white);
			g.fillRoundRect(marginX, lineY - (lineOffsetY / 2f),
					(container.getWidth() - (marginX * 2f)) * progress / 100f, lineOffsetY / 4f, 4
			);
		}
	}

	/**
	 * Draws a scroll bar.
	 * @param g the graphics context
	 * @param unitIndex the unit index
	 * @param totalUnits the total number of units
	 * @param maxShown the maximum number of units shown at one time
	 * @param unitBaseX the base x coordinate of the units
	 * @param unitBaseY the base y coordinate of the units
	 * @param unitWidth the width of a unit
	 * @param unitHeight the height of a unit
	 * @param unitOffsetY the y offset between units
	 * @param bgColor the scroll bar area background color (null if none)
	 * @param scrollbarColor the scroll bar color
	 * @param right whether or not to place the scroll bar on the right side of the unit
	 */
	public static void drawScrollbar(
			Graphics g, int unitIndex, int totalUnits, int maxShown,
			float unitBaseX, float unitBaseY, float unitWidth, float unitHeight, float unitOffsetY,
			Color bgColor, Color scrollbarColor, boolean right
	) {
		float scrollbarWidth = container.getWidth() * 0.00347f;
		float heightRatio = (float) (2.6701f * Math.exp(-0.81 * Math.log(totalUnits)));
		float scrollbarHeight = container.getHeight() * heightRatio;
		float scrollAreaHeight = unitHeight + unitOffsetY * (maxShown - 1);
		float offsetY = (scrollAreaHeight - scrollbarHeight) * ((float) unitIndex / (totalUnits - maxShown));
		float scrollbarX = unitBaseX + unitWidth - ((right) ? scrollbarWidth : 0);
		if (bgColor != null) {
			g.setColor(bgColor);
			g.fillRect(scrollbarX, unitBaseY, scrollbarWidth, scrollAreaHeight);
		}
		g.setColor(scrollbarColor);
		g.fillRect(scrollbarX, unitBaseY + offsetY, scrollbarWidth, scrollbarHeight);
	}

	/**
	 * Draws a tooltip near the current mouse coordinates, bounded by the
	 * container dimensions.
	 * @param g the graphics context
	 * @param text the tooltip text
	 * @param newlines whether to check for line breaks ('\n')
	 */
	public static void drawTooltip(Graphics g, String text, boolean newlines) {
		int containerWidth = container.getWidth(), containerHeight = container.getHeight();
		int margin = containerWidth / 100, textMarginX = 2;
		int offset = GameImage.CURSOR_MIDDLE.getImage().getWidth() / 2;
		int lineHeight = FONT_SMALL.getLineHeight();
		int textWidth = textMarginX * 2, textHeight = lineHeight;
		if (newlines) {
			String[] lines = text.split("\\n");
			int maxWidth = FONT_SMALL.getWidth(lines[0]);
			for (int i = 1; i < lines.length; i++) {
				int w = FONT_SMALL.getWidth(lines[i]);
				if (w > maxWidth)
					maxWidth = w;
			}
			textWidth += maxWidth;
			textHeight += lineHeight * (lines.length - 1);
		} else
			textWidth += FONT_SMALL.getWidth(text);

		// get drawing coordinates
		int x = input.getMouseX() + offset, y = input.getMouseY() + offset;
		if (x + textWidth > containerWidth - margin)
			x = containerWidth - margin - textWidth;
		else if (x < margin)
			x = margin;
		if (y + textHeight > containerHeight - margin)
			y = containerHeight - margin - textHeight;
		else if (y < margin)
			y = margin;

		// draw tooltip text inside a filled rectangle
		g.setColor(Color.black);
		g.fillRect(x, y, textWidth, textHeight);
		g.setColor(Color.darkGray);
		g.setLineWidth(1);
		g.drawRect(x, y, textWidth, textHeight);
		FONT_SMALL.drawString(x + textMarginX, y, text, Color.white);
	}

	/**
	 * Takes a screenshot.
	 * @author http://wiki.lwjgl.org/index.php?title=Taking_Screen_Shots
	 */
	public static void takeScreenShot() {
		// create the screenshot directory
		File dir = Options.getScreenshotDir();
		if (!dir.isDirectory()) {
			if (!dir.mkdir()) {
				ErrorHandler.error("Failed to create screenshot directory.", null, false);
				return;
			}
		}

		// create file name
		SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd_HHmmss");
		final File file = new File(dir, String.format("screenshot_%s.%s",
				date.format(new Date()), Options.getScreenshotFormat()));

		SoundController.playSound(SoundEffect.SHUTTER);

		// copy the screen to file
		final int width = Display.getWidth();
		final int height = Display.getHeight();
		final int bpp = 3;  // assuming a 32-bit display with a byte each for red, green, blue, and alpha
		final ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * bpp);
		GL11.glReadBuffer(GL11.GL_FRONT);
		GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
		GL11.glReadPixels(0, 0, width, height, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, buffer);
		new Thread() {
			@Override
			public void run() {
				try {
					BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
					for (int x = 0; x < width; x++) {
						for (int y = 0; y < height; y++) {
							int i = (x + (width * y)) * bpp;
							int r = buffer.get(i) & 0xFF;
							int g = buffer.get(i + 1) & 0xFF;
							int b = buffer.get(i + 2) & 0xFF;
							image.setRGB(x, height - (y + 1), (0xFF << 24) | (r << 16) | (g << 8) | b);
						}
					}
					ImageIO.write(image, Options.getScreenshotFormat(), file);
				} catch (Exception e) {
					ErrorHandler.error("Failed to take a screenshot.", e, true);
				}
			}
		}.start();
	}

	/**
	 * Loads a Unicode font.
	 * @param font the font to load
	 * @param padding the top and bottom padding
	 * @param effect the font effect
	 * @throws SlickException
	 */
	@SuppressWarnings("unchecked")
	private static void loadFont(UnicodeFont font, int padding,
			Effect effect) throws SlickException {
		font.setPaddingTop(padding);
		font.setPaddingBottom(padding);
		font.addAsciiGlyphs();
		font.getEffects().add(effect);
		font.loadGlyphs();
	}

	/**
	 * Adds and loads glyphs for an OsuFile's Unicode title and artist strings.
	 * @param osu the OsuFile
	 */
	public static void loadGlyphs(OsuFile osu) {
		if (!Options.useUnicodeMetadata())
			return;

		boolean glyphsAdded = false;
		if (!osu.titleUnicode.isEmpty() && !loadedGlyphs.contains(osu.titleUnicode)) {
			Utils.FONT_LARGE.addGlyphs(osu.titleUnicode);
			Utils.FONT_MEDIUM.addGlyphs(osu.titleUnicode);
			Utils.FONT_DEFAULT.addGlyphs(osu.titleUnicode);
			loadedGlyphs.add(osu.titleUnicode);
			glyphsAdded = true;
		}
		if (!osu.artistUnicode.isEmpty() && !loadedGlyphs.contains(osu.artistUnicode)) {
			Utils.FONT_LARGE.addGlyphs(osu.artistUnicode);
			Utils.FONT_MEDIUM.addGlyphs(osu.artistUnicode);
			Utils.FONT_DEFAULT.addGlyphs(osu.artistUnicode);
			loadedGlyphs.add(osu.artistUnicode);
			glyphsAdded = true;
		}
		if (glyphsAdded) {
			try {
				Utils.FONT_LARGE.loadGlyphs();
				Utils.FONT_MEDIUM.loadGlyphs();
				Utils.FONT_DEFAULT.loadGlyphs();
			} catch (SlickException e) {
				Log.warn("Failed to load glyphs.", e);
			}
		}
	}

	/**
	 * Returns a human-readable representation of a given number of bytes.
	 * @param bytes the number of bytes
	 * @return the string representation
	 * @author aioobe (http://stackoverflow.com/a/3758880)
	 */
	public static String bytesToString(long bytes) {
		if (bytes < 1024)
			return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(1024));
		char pre = "KMGTPE".charAt(exp - 1);
		return String.format("%.1f %cB", bytes / Math.pow(1024, exp), pre);
	}

	/**
	 * Cleans a file name.
	 * @param badFileName the original name string
	 * @param replace the character to replace illegal characters with (or 0 if none)
	 * @return the cleaned file name
	 * @author Sarel Botha (http://stackoverflow.com/a/5626340)
	 */
	public static String cleanFileName(String badFileName, char replace) {
		boolean doReplace = (replace > 0 && Arrays.binarySearch(illegalChars, replace) < 0);
	    StringBuilder cleanName = new StringBuilder();
	    for (int i = 0, n = badFileName.length(); i < n; i++) {
	        int c = badFileName.charAt(i);
	        if (Arrays.binarySearch(illegalChars, c) < 0)
	            cleanName.append((char) c);
	        else if (doReplace)
	        	cleanName.append(replace);
	    }
	    return cleanName.toString();
	}

	/**
	 * Deletes a file or directory.  If a system trash directory is available,
	 * the file or directory will be moved there instead.
	 * @param file the file or directory to delete
	 * @return true if moved to trash, and false if deleted
	 * @throws IOException if given file does not exist
	 */
	public static boolean deleteToTrash(File file) throws IOException {
		if (file == null)
			throw new IOException("File cannot be null.");
		if (!file.exists())
			throw new IOException(String.format("File '%s' does not exist.", file.getAbsolutePath()));

		// move to system trash, if possible
		FileUtils fileUtils = FileUtils.getInstance();
		if (fileUtils.hasTrash()) {
			try {
				fileUtils.moveToTrash(new File[] { file });
				return true;
			} catch (IOException e) {
				Log.warn(String.format("Failed to move file '%s' to trash.", file.getAbsolutePath()), e);
			}
		}

		// delete otherwise
		if (file.isDirectory())
			deleteDirectory(file);
		else
			file.delete();
		return false;
	}

	/**
	 * Recursively deletes all files and folders in a directory, then
	 * deletes the directory itself.
	 * @param dir the directory to delete
	 */
	private static void deleteDirectory(File dir) {
		if (dir == null || !dir.isDirectory())
			return;

		// recursively delete contents of directory
		File[] files = dir.listFiles();
		if (files != null && files.length > 0) {
			for (File file : files) {
				if (file.isDirectory())
					deleteDirectory(file);
				else
					file.delete();
			}
		}

		// delete the directory
		dir.delete();
	}

	/**
	 * Wraps the given string into a list of split lines based on the width.
	 * @param text the text to split
	 * @param font the font used to draw the string
	 * @param width the maximum width of a line
	 * @return the list of split strings
	 * @author davedes (http://slick.ninjacave.com/forum/viewtopic.php?t=3778)
	 */
	public static List<String> wrap(String text, org.newdawn.slick.Font font, int width) {
		List<String> list = new ArrayList<String>();
		String str = text;
		String line = "";
		int i = 0;
		int lastSpace = -1;
		while (i < str.length()) {
			char c = str.charAt(i);
			if (Character.isWhitespace(c))
				lastSpace = i;
			String append = line + c;
			if (font.getWidth(append) > width) {
				int split = (lastSpace != -1) ? lastSpace : i;
				int splitTrimmed = split;
				if (lastSpace != -1 && split < str.length() - 1)
					splitTrimmed++;
				list.add(str.substring(0, split));
				str = str.substring(splitTrimmed);
				line = "";
				i = 0;
				lastSpace = -1;
			} else {
				line = append;
				i++;
			}
		}
		if (str.length() != 0)
			list.add(str);
		return list;
    }
}
