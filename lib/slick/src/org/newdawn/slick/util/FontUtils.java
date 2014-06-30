package org.newdawn.slick.util;

import java.awt.Graphics2D;

import org.newdawn.slick.Color;
import org.newdawn.slick.Font;

/**
 * Simple utility class to support justified text
 * 
 * http://slick.javaunlimited.net/viewtopic.php?t=2640
 * 
 * @author zenzei
 */
public class FontUtils {
	/**
	 * Alignment indicators
	 */
	public class Alignment {
		/** Left alignment */
		public static final int LEFT = 1;
		/** Center alignment */
		public static final int CENTER = 2;
		/** Right alignment */
		public static final int RIGHT = 3;
		/** Justify alignment */
		public static final int JUSTIFY = 4;
	}

	/**
	 * Draw text left justified
	 * 
	 * @param font The font to draw with
	 * @param s The string to draw
	 * @param x The x location to draw at
	 * @param y The y location to draw at
	 */
	public static void drawLeft(Font font, String s, int x, int y) {
		drawString(font, s, Alignment.LEFT, x, y, 0, Color.white);
	}

	/**
	 * Draw text center justified
	 * 
	 * @param font The font to draw with
	 * @param s The string to draw
	 * @param x The x location to draw at
	 * @param y The y location to draw at
	 * @param width The width to fill with the text
	 */
	public static void drawCenter(Font font, String s, int x, int y, int width) {
		drawString(font, s, Alignment.CENTER, x, y, width, Color.white);
	}

	/**
	 * Draw text center justified
	 * 
	 * @param font The font to draw with
	 * @param s The string to draw
	 * @param x The x location to draw at
	 * @param y The y location to draw at
	 * @param width The width to fill with the text
	 * @param color The color to draw in
	 */
	public static void drawCenter(Font font, String s, int x, int y, int width,
			Color color) {
		drawString(font, s, Alignment.CENTER, x, y, width, color);
	}

	/**
	 * Draw text right justified
	 * 
	 * @param font The font to draw with
	 * @param s The string to draw
	 * @param x The x location to draw at
	 * @param y The y location to draw at
	 * @param width The width to fill with the text
	 */
	public static void drawRight(Font font, String s, int x, int y, int width) {
		drawString(font, s, Alignment.RIGHT, x, y, width, Color.white);
	}

	/**
	 * Draw text right justified
	 * 
	 * @param font The font to draw with
	 * @param s The string to draw
	 * @param x The x location to draw at
	 * @param y The y location to draw at
	 * @param width The width to fill with the text
	 * @param color The color to draw in
	 */
	public static void drawRight(Font font, String s, int x, int y, int width,
			Color color) {
		drawString(font, s, Alignment.RIGHT, x, y, width, color);
	}

	/**
	 * Draw a string
	 * 
	 * @param font The font to draw with
	 * @param s The text to draw
	 * @param alignment The alignment to apply
	 * @param x The x location to draw at
	 * @param y The y location to draw at
	 * @param width The width to fill with the string 
	 * @param color The color to draw in 
	 * @return The final x coordinate of the text
	 */
	public static final int drawString(Font font, final String s,
			final int alignment, final int x, final int y, final int width,
			Color color) {
		int resultingXCoordinate = 0;
		if (alignment == Alignment.LEFT) {
			font.drawString(x, y, s, color);
		} else if (alignment == Alignment.CENTER) {
			font.drawString(x + (width / 2) - (font.getWidth(s) / 2), y, s,
					color);
		} else if (alignment == Alignment.RIGHT) {
			font.drawString(x + width - font.getWidth(s), y, s, color);
		} else if (alignment == Alignment.JUSTIFY) {
			// calculate left width
			int leftWidth = width - font.getWidth(s);
			if (leftWidth <= 0) {
				// no width left, use standard draw string
				font.drawString(x, y, s, color);
			}

			return FontUtils.drawJustifiedSpaceSeparatedSubstrings(font, s, x,
					y, FontUtils.calculateWidthOfJustifiedSpaceInPixels(font,
							s, leftWidth));
		}

		return resultingXCoordinate;
	}

	/**
	 * Calculates and returns the width of a single justified space for the
	 * given {@link String}, in pixels.
	 * 
	 * @param font The font to draw with
	 * @param s
	 *            The given non-null {@link String} to use to calculate the
	 *            width of a space for.
	 * @param leftWidth
	 *            The integer specifying the left width buffer to use to
	 *            calculate how much space a space should take up in
	 *            justification.
	 * @return The width of a single justified space for the given
	 *         {@link String}, in pixels.
	 */
	private static int calculateWidthOfJustifiedSpaceInPixels(final Font font,
			final String s, final int leftWidth) {
		int space = 0; // hold total space; hold space width in pixel
		int curpos = 0; // current string position

		// count total space
		while (curpos < s.length()) {
			if (s.charAt(curpos++) == ' ') {
				space++;
			}
		}

		if (space > 0) {
			// width left plus with total space
			// space width (in pixel) = width left / total space
			space = (leftWidth + (font.getWidth(" ") * space)) / space;
		}
		return space;
	}

	/**
	 * Draws justified-space separated substrings based on the given
	 * {@link String} and the given starting x and y coordinates to the given
	 * {@link Graphics2D} instance.
	 * 
	 * @param font
	 *            The font to draw with
	 * @param s
	 *            The non-null {@link String} to draw as space-separated
	 *            substrings.
	 * @param x
	 *            The given starting x-coordinate position to use to draw the
	 *            {@link String}.
	 * @param y
	 *            The given starting y-coordinate position to use to draw the
	 *            {@link String}.
	 * @param justifiedSpaceWidth
	 *            The integer specifying the width of a justified space
	 *            {@link String}, in pixels.
	 * @return The resulting x-coordinate of the current cursor after the
	 *         drawing operation completes.
	 * @throws NullPointerException
	 *             Throws a {@link NullPointerException} if any of the given
	 *             arguments are null.
	 */
	private static int drawJustifiedSpaceSeparatedSubstrings(Font font,
			final String s, final int x, final int y,
			final int justifiedSpaceWidth) {
		int curpos = 0;
		int endpos = 0;
		int resultingXCoordinate = x;
		while (curpos < s.length()) {
			endpos = s.indexOf(' ', curpos); // find space
			if (endpos == -1) {
				endpos = s.length(); // no space, draw all string directly
			}
			String substring = s.substring(curpos, endpos);

			font.drawString(resultingXCoordinate, y, substring);

			resultingXCoordinate += font.getWidth(substring)
					+ justifiedSpaceWidth; // increase
			// x-coordinate
			curpos = endpos + 1;
		}

		return resultingXCoordinate;
	}
}