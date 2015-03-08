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
import itdelatrisu.opsu.downloads.Download;
import itdelatrisu.opsu.downloads.DownloadNode;

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
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
		input = container.getInput();

		// game settings
		container.setTargetFrameRate(Options.getTargetFPS());
		container.setVSync(Options.getTargetFPS() == 60);
		container.setMusicVolume(Options.getMusicVolume() * Options.getMasterVolume());
		container.setShowFPS(false);
		container.getInput().enableKeyRepeat();
		container.setAlwaysRender(true);
		container.setUpdateOnlyWhenVisible(false);

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

		// initialize UI components
		UI.init(container, game);
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
	public static float clamp(float val, float low, float high) {
		if (val < low)
			return low;
		if (val > high)
			return high;
		return val;
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
		if (badFileName == null)
			return null;

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

	/**
	 * Returns a the contents of a URL as a string.
	 * @param url the remote URL
	 * @return the contents as a string, or null if any error occurred
	 */
	public static String readDataFromUrl(URL url) throws IOException {
		// open connection
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setConnectTimeout(Download.CONNECTION_TIMEOUT);
		conn.setReadTimeout(Download.READ_TIMEOUT);
		conn.setUseCaches(false);
		try {
			conn.connect();
		} catch (SocketTimeoutException e) {
			Log.warn("Connection to server timed out.", e);
			throw e;
		}

		if (Thread.interrupted())
			return null;

		// read contents
		try (InputStream in = conn.getInputStream()) {
			BufferedReader rd = new BufferedReader(new InputStreamReader(in));
			StringBuilder sb = new StringBuilder();
			int c;
			while ((c = rd.read()) != -1)
				sb.append((char) c);
			return sb.toString();
		} catch (SocketTimeoutException e) {
			Log.warn("Connection to server timed out.", e);
			throw e;
		}
	}
}
