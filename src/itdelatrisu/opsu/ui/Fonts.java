/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014, 2015, 2016 Jeffrey Han
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

package itdelatrisu.opsu.ui;

import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.Options;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.newdawn.slick.Color;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.UnicodeFont;
import org.newdawn.slick.font.effects.ColorEffect;
import org.newdawn.slick.font.effects.Effect;
import org.newdawn.slick.util.Log;
import org.newdawn.slick.util.ResourceLoader;

/**
 * Fonts used for drawing.
 */
public class Fonts {
	public static UnicodeFont DEFAULT, BOLD, XLARGE, LARGE, MEDIUM, MEDIUMBOLD, SMALL;

	/** Set of all Unicode strings already loaded per font. */
	private static HashMap<UnicodeFont, HashSet<String>> loadedGlyphs = new HashMap<UnicodeFont, HashSet<String>>();

	// This class should not be instantiated.
	private Fonts() {}

	/**
	 * Initializes all fonts.
	 * @throws SlickException if ASCII glyphs could not be loaded
	 * @throws FontFormatException if any font stream data does not contain the required font tables
	 * @throws IOException if a font stream cannot be completely read
	 */
	public static void init() throws SlickException, FontFormatException, IOException {
		float fontBase = 12f * GameImage.getUIscale();
		Font javaFont = Font.createFont(Font.TRUETYPE_FONT, ResourceLoader.getResourceAsStream(Options.FONT_NAME));
		Font font = javaFont.deriveFont(Font.PLAIN, (int) (fontBase * 4 / 3));
		DEFAULT = new UnicodeFont(font);
		BOLD = new UnicodeFont(font.deriveFont(Font.BOLD));
		XLARGE = new UnicodeFont(font.deriveFont(fontBase * 3));
		LARGE = new UnicodeFont(font.deriveFont(fontBase * 2));
		MEDIUM = new UnicodeFont(font.deriveFont(fontBase * 3 / 2));
		MEDIUMBOLD = new UnicodeFont(font.deriveFont(Font.BOLD, fontBase * 3 / 2));
		SMALL = new UnicodeFont(font.deriveFont(fontBase));
		ColorEffect colorEffect = new ColorEffect();
		loadFont(DEFAULT, colorEffect);
		loadFont(BOLD, colorEffect);
		loadFont(XLARGE, colorEffect);
		loadFont(LARGE, colorEffect);
		loadFont(MEDIUM, colorEffect);
		loadFont(MEDIUMBOLD, colorEffect);
		loadFont(SMALL, colorEffect);
	}

	/**
	 * Loads a Unicode font and its ASCII glyphs.
	 * @param font the font to load
	 * @param effect the font effect
	 * @throws SlickException if the glyphs could not be loaded
	 */
	@SuppressWarnings("unchecked")
	private static void loadFont(UnicodeFont font, Effect effect) throws SlickException {
		font.addAsciiGlyphs();
		font.getEffects().add(effect);
		font.loadGlyphs();
	}

	/**
	 * Adds and loads glyphs for a font.
	 * @param font the font to add the glyphs to
	 * @param s the string containing the glyphs to load
	 */
	public static void loadGlyphs(UnicodeFont font, String s) {
		if (s == null || s.isEmpty())
			return;

		// get set of added strings
		HashSet<String> set = loadedGlyphs.get(font);
		if (set == null) {
			set = new HashSet<String>();
			loadedGlyphs.put(font, set);
		} else if (set.contains(s))
			return;  // string already in set

		// load glyphs
		font.addGlyphs(s);
		set.add(s);
		try {
			font.loadGlyphs();
		} catch (SlickException e) {
			Log.warn(String.format("Failed to load glyphs for string '%s'.", s), e);
		}
	}

	/**
	 * Wraps the given string into a list of split lines based on the width.
	 * @param font the font used to draw the string
	 * @param text the text to split
	 * @param width the maximum width of a line
	 * @return the list of split strings
	 * @author davedes (http://slick.ninjacave.com/forum/viewtopic.php?t=3778)
	 */
	public static List<String> wrap(org.newdawn.slick.Font font, String text, int width) {
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
	 * Draws the string with a pixel border
	 * @param font the Font
	 * @param x x Location
	 * @param y y Location
	 * @param str string to display
	 * @param fg foreground Color
	 * @param bg background Color
	 */
	public static void drawBorderedString(org.newdawn.slick.Font font, float x, float y, String str, Color fg, Color bg) {
		font.drawString(x+1,  y+1 ,str, bg);
		font.drawString(x+1,  y-1 ,str, bg);
		font.drawString(x-1,  y+1 ,str, bg);
		font.drawString(x-1,  y-1 ,str, bg);

		font.drawString(x,  y ,str, fg);
		font.drawString(x,  y ,str, fg);
	}
}
