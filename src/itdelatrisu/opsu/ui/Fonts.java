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

package itdelatrisu.opsu.ui;

import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.options.Options;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

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
	public static UnicodeFont DEFAULT, BOLD, XLARGE, LARGE, MEDIUM, MEDIUMBOLD, SMALL, SMALLBOLD;

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
		Font javaFontMain = Font.createFont(Font.TRUETYPE_FONT, ResourceLoader.getResourceAsStream(Options.FONT_MAIN));
		Font javaFontBold = Font.createFont(Font.TRUETYPE_FONT, ResourceLoader.getResourceAsStream(Options.FONT_BOLD));
		Font javaFontCJK = Font.createFont(Font.TRUETYPE_FONT, ResourceLoader.getResourceAsStream(Options.FONT_CJK));
		Font fontMain = javaFontMain.deriveFont(Font.PLAIN, (int) (fontBase * 4 / 3));
		Font fontBold = javaFontBold.deriveFont(Font.PLAIN, (int) (fontBase * 4 / 3));
		Font fontCJK = javaFontCJK.deriveFont(Font.PLAIN, (int) (fontBase * 4 / 3));
		DEFAULT = new UnicodeFont(fontMain);
		BOLD = new UnicodeFont(fontBold.deriveFont(Font.BOLD));
		XLARGE = new UnicodeFont(fontMain.deriveFont(fontBase * 3));
		LARGE = new UnicodeFont(fontMain.deriveFont(fontBase * 2));
		MEDIUM = new UnicodeFont(fontMain.deriveFont(fontBase * 3 / 2));
		MEDIUMBOLD = new UnicodeFont(fontBold.deriveFont(Font.BOLD, fontBase * 3 / 2));
		SMALL = new UnicodeFont(fontMain.deriveFont(fontBase));
		SMALLBOLD = new UnicodeFont(fontBold.deriveFont(Font.BOLD, fontBase));
		ColorEffect colorEffect = new ColorEffect();
		loadFont(DEFAULT, colorEffect, new UnicodeFont(fontCJK));
		loadFont(BOLD, colorEffect, new UnicodeFont(fontCJK.deriveFont(Font.BOLD)));
		loadFont(XLARGE, colorEffect, new UnicodeFont(fontCJK.deriveFont(fontBase * 3)));
		loadFont(LARGE, colorEffect, new UnicodeFont(fontCJK.deriveFont(fontBase * 2)));
		loadFont(MEDIUM, colorEffect, new UnicodeFont(fontCJK.deriveFont(fontBase * 3 / 2)));
		loadFont(MEDIUMBOLD, colorEffect, new UnicodeFont(fontCJK.deriveFont(Font.BOLD, fontBase * 3 / 2)));
		loadFont(SMALL, colorEffect, new UnicodeFont(fontCJK.deriveFont(fontBase)));
		loadFont(SMALLBOLD, colorEffect, new UnicodeFont(fontCJK.deriveFont(Font.BOLD, fontBase)));
	}

	/**
	 * Loads a Unicode font and its ASCII glyphs.
	 * @param font the font to load
	 * @param effect the font effect
	 * @param backup the backup font
	 * @throws SlickException if the glyphs could not be loaded
	 */
	@SuppressWarnings("unchecked")
	private static void loadFont(UnicodeFont font, Effect effect, UnicodeFont backup) throws SlickException {
		font.addBackupFont(backup);
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
	 * Adds and loads glyphs for a font.
	 * @param font the font to add the glyphs to
	 * @param c the character to load
	 */
	public static void loadGlyphs(UnicodeFont font, char c) {
		font.addGlyphs(c, c);
		try {
			font.loadGlyphs();
		} catch (SlickException e) {
			Log.warn(String.format("Failed to load glyphs for codepoint '%d'.", (int) c), e);
		}
	}

	/**
	 * Wraps the given string into a list of split lines based on the width.
	 * @param font the font used to draw the string
	 * @param text the text to split
	 * @param width the maximum width of a line
	 * @param newlines true if the "\n" character should break a line
	 * @return the list of split strings
	 * @author davedes (http://slick.ninjacave.com/forum/viewtopic.php?t=3778)
	 */
	public static List<String> wrap(org.newdawn.slick.Font font, String text, int width, boolean newlines) {
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
			if (font.getWidth(append) > width || (newlines && c == '\n')) {
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
