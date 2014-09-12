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

package itdelatrisu.opsu;

import java.awt.Font;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Cursor;
import org.newdawn.slick.Animation;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.UnicodeFont;
import org.newdawn.slick.font.effects.ColorEffect;
import org.newdawn.slick.imageout.ImageOut;
import org.newdawn.slick.state.BasicGameState;
import org.newdawn.slick.state.StateBasedGame;
import org.newdawn.slick.util.Log;
import org.newdawn.slick.util.ResourceLoader;

/**
 * Contains miscellaneous utilities.
 */
public abstract class Utils extends BasicGameState {
	/**
	 * Game colors.
	 */
	public static final Color
		COLOR_BLACK_ALPHA     = new Color(0, 0, 0, 0.5f),
		COLOR_WHITE_ALPHA     = new Color(255, 255, 255, 0.5f),
		COLOR_BLUE_DIVIDER    = new Color(49, 94, 237),
		COLOR_BLUE_BACKGROUND = new Color(74, 130, 255),
		COLOR_BLUE_BUTTON     = new Color(50, 189, 237),
		COLOR_ORANGE_BUTTON   = new Color(230, 151, 87),
		COLOR_GREEN_OBJECT    = new Color(26, 207, 26),
		COLOR_BLUE_OBJECT     = new Color(46, 136, 248),
		COLOR_RED_OBJECT      = new Color(243, 48, 77),
		COLOR_ORANGE_OBJECT   = new Color(255, 200, 32),
		COLOR_YELLOW_ALPHA    = new Color(255, 255, 0, 0.4f);

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
	public static UnicodeFont
		FONT_DEFAULT, FONT_BOLD,
		FONT_XLARGE, FONT_LARGE, FONT_MEDIUM, FONT_SMALL;

	/**
	 * Back button (shared by other states).
	 */
	private static GUIMenuButton backButton;

	/**
	 * Tab image (shared by other states).
	 */
	private static Image tab;

	/**
	 * Cursor image and trail.
	 */
	private static Image cursor, cursorTrail, cursorMiddle;

	/**
	 * Last cursor coordinates.
	 */
	private static int lastX = -1, lastY = -1;
	
	/**
	 * Stores all previous cursor locations to display a trail.
	 */
	private static LinkedList<Integer>
		cursorX = new LinkedList<Integer>(),
		cursorY = new LinkedList<Integer>();

	/**
	 * Set of all Unicode strings already loaded.
	 */
	private static HashSet<String> loadedGlyphs = new HashSet<String>();

	// game-related variables
	protected GameContainer container;
	protected StateBasedGame game;
	protected Input input;
	private final int id;
	protected final OpsuOptions options;

	protected Utils(int id, OpsuOptions options) {
		super();
		this.id = id;
		this.options = options;
	}

	@Override
	public final int getID() {
		return id;
	}

	/**
	 * Initializes game settings and class data.
	 * @param container the game container
	 * @param game the game object
	 * @throws SlickException
	 */
	@Override
	public void init(GameContainer container, StateBasedGame game)
			throws SlickException {
		this.container = container;
		this.game = game;
		this.input = container.getInput();

	}
	
	public static void initializeContainer(GameContainer container, OpsuOptions options) throws SlickException {
		// game settings
		container.setTargetFrameRate(options.getTargetFPS());
		container.setMusicVolume(options.getMusicVolume());
		container.setShowFPS(false);
		container.getInput().enableKeyRepeat();
		container.setAlwaysRender(true);

		int width = container.getWidth();
		int height = container.getHeight();

		// set the cursor
		try {
			// hide the native cursor
			Cursor emptyCursor = new Cursor(1, 1, 0, 0, 1, BufferUtils.createIntBuffer(1), null);
			container.setMouseCursor(emptyCursor, 0, 0);
		} catch (LWJGLException e) {
			Log.error("Failed to set the cursor.", e);
		}
		loadCursor(container, options);

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
			Font javaFont = Font.createFont(Font.TRUETYPE_FONT, ResourceLoader.getResourceAsStream(OpsuOptions.FONT_NAME));
			Font font    = javaFont.deriveFont(Font.PLAIN, (int) (fontBase * 4 / 3));
			FONT_DEFAULT = new UnicodeFont(font);
			FONT_BOLD    = new UnicodeFont(font.deriveFont(Font.BOLD));
			FONT_XLARGE  = new UnicodeFont(font.deriveFont(fontBase * 4));
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
			Log.error("Failed to load fonts.", e);
		}

		// tab image
		tab = new Image("selection-tab.png");
		float tabScale = (height * 0.033f) / tab.getHeight();
		tab = tab.getScaledCopy(tabScale);

		// back button
		Image back = new Image("menu-back.png");
		float scale = (height * 0.1f) / back.getHeight();
		back = back.getScaledCopy(scale);
		backButton = new GUIMenuButton(back,
				back.getWidth() / 2f,
				height - (back.getHeight() / 2f));

		// set default game images
		for (GameImage img : GameImage.values())
			img.setDefaultImage();

		// initialize game mods
		for (GameMod mod : GameMod.values())
			mod.init(width, height);

		// initialize sorts
		for (SongSort sort : SongSort.values())
			sort.init(tab, width, height);
	}

	/**
	 * Returns the 'selection-tab' image.
	 */
	public static Image getTabImage() { return tab; }

	/**
	 * Returns the 'menu-back' GUIMenuButton.
	 */
	public static GUIMenuButton getBackButton() { return backButton; }

	/**
	 * Draws an image based on its center with a color filter.
	 * @param img the image to draw
	 * @param x the center x coordinate
	 * @param y the center y coordinate
	 * @param color the color filter to apply
	 */
	public static void drawCentered(Image img, float x, float y, Color color) {
		img.draw(x - (img.getWidth() / 2f), y - (img.getHeight() / 2f), color);
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
	 * Loads the cursor images.
	 * @throws SlickException 
	 */
	public static void loadCursor(GameContainer container, OpsuOptions options) throws SlickException {
		// destroy old cursors, if they exist
		if (cursor != null)
			cursor.destroy();
		if (cursorTrail != null)
			cursorTrail.destroy();
		if (cursorMiddle != null)
			cursorMiddle.destroy();
		cursor = cursorTrail = cursorMiddle = null;

		// TODO: cleanup
		boolean skinCursor = new File(options.getSkinDir(), "cursor.png").isFile();
		if (options.newCursor) {
			// load new cursor type
			// if skin cursor exists but middle part does not, don't load default middle
			if (skinCursor && !new File(options.getSkinDir(), "cursormiddle.png").isFile())
				;
			else {
				cursorMiddle = new Image("cursormiddle.png");
				cursor = new Image("cursor.png");
				cursorTrail = new Image("cursortrail.png");
			}
		}
		if (cursorMiddle == null) {
			// load old cursor type
			// default is stored as *2.png, but load skin cursor if it exists
			if (skinCursor)
				cursor = new Image("cursor.png");
			else
				cursor = new Image("cursor2.png");
			if (new File(options.getSkinDir(), "cursortrail.png").isFile())
				cursorTrail = new Image("cursortrail.png");
			else
				cursorTrail = new Image("cursortrail2.png");
		}

		// scale the cursor
		float scale = 1 + ((container.getHeight() - 600) / 1000f);
		cursor = cursor.getScaledCopy(scale);
		cursorTrail = cursorTrail.getScaledCopy(scale);
		if (cursorMiddle != null)
			cursorMiddle = cursorMiddle.getScaledCopy(scale);
	}

	/**
	 * Draws the cursor.
	 */
	public void drawCursor() {
		// TODO: use an image buffer

		int x = input.getMouseX();
		int y = input.getMouseY();

		int removeCount = 0;
		int FPSmod = (options.getTargetFPS() / 60);

		// if middle exists, add all points between cursor movements
		if (cursorMiddle != null) {
			if (lastX < 0) {
				lastX = x;
				lastY = y;
				return;
			}
			addCursorPoints(lastX, lastY, x, y);
			lastX = x;
			lastY = y;

			removeCount = (cursorX.size() / (6 * FPSmod)) + 1;
		}

		// else, sample one point at a time
		else {
			cursorX.add(x);
			cursorY.add(y);

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
		cursorTrail.drawCentered(x, y);

		// increase the cursor size if pressed
		int state = game.getCurrentStateID();
		float scale = 1f;
		if (((state == Opsu.STATE_GAME || state == Opsu.STATE_GAMEPAUSEMENU) && isGameKeyPressed()) ||
			(input.isMouseButtonDown(Input.MOUSE_LEFT_BUTTON) || input.isMouseButtonDown(Input.MOUSE_RIGHT_BUTTON)))
			scale = 1.25f;

		// draw the other components
		Image cursorScaled = cursor.getScaledCopy(scale);
		cursorScaled.setRotation(cursor.getRotation());
		cursorScaled.drawCentered(x, y);
		if (cursorMiddle != null) {
			Image cursorMiddleScaled = cursorMiddle.getScaledCopy(scale);
			cursorMiddleScaled.setRotation(cursorMiddle.getRotation());
			cursorMiddleScaled.drawCentered(x, y);
		}
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
		if (cursorMiddle == null)
			return;

		cursor.rotate(delta / 40f);
	}

	/**
	 * Returns true if a game input key is pressed (mouse/keyboard left/right).
	 * @return true if pressed
	 */
	public boolean isGameKeyPressed() {
		return (input.isMouseButtonDown(Input.MOUSE_LEFT_BUTTON) ||
				input.isMouseButtonDown(Input.MOUSE_RIGHT_BUTTON) ||
				input.isKeyDown(options.getGameKeyLeft()) ||
				input.isKeyDown(options.getGameKeyRight()));
	}

	/**
	 * Draws the FPS at the bottom-right corner of the game container.
	 * If the option is not activated, this will do nothing.
	 */
	public void drawFPS() {
		if (!options.isFPSCounterEnabled())
			return;

		String fps = String.format("FPS: %d", container.getFPS());
		FONT_DEFAULT.drawString(
				container.getWidth() - 15 - FONT_DEFAULT.getWidth(fps),
				container.getHeight() - 15 - FONT_DEFAULT.getHeight(fps),
				fps, Color.white
		);
	}

	/**
	 * Takes a screenshot.
	 * @return true if successful
	 */
	public boolean takeScreenShot() {
		// TODO: should this be threaded?
		try {
			// create the screenshot directory
			File dir = options.getScreenshotDir();
			if (!dir.isDirectory()) {
				if (!dir.mkdir())
					return false;
			}

			// create file name
			SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd_HHmmss");
			File file = new File(dir, String.format("screenshot_%s.%s",
					date.format(new Date()), options.getScreenshotFormat()));

			SoundController.playSound(SoundController.SOUND_SHUTTER);

			// copy the screen
			Image screen = new Image(container.getWidth(), container.getHeight());
			container.getGraphics().copyArea(screen, 0, 0);
			ImageOut.write(screen, file.getAbsolutePath(), false);
			screen.destroy();
		} catch (SlickException e) {
			Log.warn("Failed to take a screenshot.", e);
			return false;
		}
		return true;
	}

	/**
	 * Loads a Unicode font.
	 * @param font the font to load
	 * @param padding the top and bottom padding
	 * @param colorEffect the ColorEffect
	 * @throws SlickException
	 */
	@SuppressWarnings("unchecked")
	private static void loadFont(UnicodeFont font, int padding,
			ColorEffect colorEffect) throws SlickException {
		font.setPaddingTop(padding);
		font.setPaddingBottom(padding);
		font.addAsciiGlyphs();
		font.getEffects().add(colorEffect);
		font.loadGlyphs();
	}

	/**
	 * Adds and loads glyphs for an OsuFile's Unicode title and artist strings.
	 * @param osu the OsuFile
	 */
	public static void loadGlyphs(OsuFile osu, OpsuOptions options) {
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
		if (glyphsAdded && options.useUnicodeMetadata()) {
			try {
				Utils.FONT_LARGE.loadGlyphs();
				Utils.FONT_MEDIUM.loadGlyphs();
				Utils.FONT_DEFAULT.loadGlyphs();
			} catch (SlickException e) {
				Log.warn("Failed to load glyphs.", e);
			}
		}
	}
}