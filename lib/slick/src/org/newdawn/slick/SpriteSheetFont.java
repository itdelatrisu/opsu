package org.newdawn.slick;

import java.io.UnsupportedEncodingException;

import org.newdawn.slick.util.Log;

/**
 * A font implementation that will use the graphics inside a SpriteSheet for its data.
 * This is useful when your font has a fixed width and height for each character as
 * opposed to the more complex AngelCodeFont that allows different sizes and kerning
 * for each character.
 *
 * @author Onno Scheffers
 */
public class SpriteSheetFont implements Font {
	/** The SpriteSheet containing the bitmap font */
	private SpriteSheet font;
	/** First character in the SpriteSheet */
	private char startingCharacter;
	/** Width of each character in pixels */
	private int charWidth;
	/** Height of each character in pixels */
	private int charHeight;
	/** Number of characters in SpriteSheet horizontally */
	private int horizontalCount;
	/** Total number of characters in SpriteSheet */
	private int numChars;

	/**
	 * Create a new font based on a SpriteSheet. The SpriteSheet should hold your
	 * fixed-width character set in ASCII order. To only get upper-case characters
	 * working you would usually set up a SpriteSheet with characters for these values:
	 * <pre>
	 *   !"#$%&'()*+,-./
	 *  0123456789:;<=>?
	 *  &#0064;ABCDEFGHIJKLMNO
	 *  PQRSTUVWXYZ[\]^_<pre>
	 * In this set, ' ' (SPACE) would be the startingCharacter of your characterSet.
	 *
	 * @param font              The SpriteSheet holding the font data.
	 * @param startingCharacter The first character that is defined in the SpriteSheet.
	 */
	public SpriteSheetFont(SpriteSheet font, char startingCharacter) {
		this.font = font;
		this.startingCharacter = startingCharacter;
		horizontalCount = font.getHorizontalCount();
		int verticalCount = font.getVerticalCount();
		charWidth = font.getWidth() / horizontalCount;
		charHeight = font.getHeight() / verticalCount;
		numChars = horizontalCount * verticalCount;
	}

	/**
	 * @see org.newdawn.slick.Font#drawString(float, float, java.lang.String)
	 */
	public void drawString(float x, float y, String text) {
		drawString(x, y, text, Color.white);
	}

	/**
	 * @see org.newdawn.slick.Font#drawString(float, float, java.lang.String, org.newdawn.slick.Color)
	 */
	public void drawString(float x, float y, String text, Color col) {
		drawString(x,y,text,col,0,text.length()-1);
	}
	
	/**
	 * @see Font#drawString(float, float, String, Color, int, int)
	 */
	public void drawString(float x, float y, String text, Color col, int startIndex, int endIndex) {
		try {
			byte[] data = text.getBytes("US-ASCII");
			for (int i = 0; i < data.length; i++) {
				int index = data[i] - startingCharacter;
				if (index < numChars) {
					int xPos = (index % horizontalCount);
					int yPos = (index / horizontalCount);
					
					if ((i >= startIndex) || (i <= endIndex)) {
						font.getSprite(xPos, yPos)
								.draw(x + (i * charWidth), y, col);
					}
				}
			}
		} catch (UnsupportedEncodingException e) {
			// Should never happen, ASCII is supported pretty much anywhere
			Log.error(e);
		}
	}

	/**
	 * @see org.newdawn.slick.Font#getHeight(java.lang.String)
	 */
	public int getHeight(String text) {
		return charHeight;
	}

	/**
	 * @see org.newdawn.slick.Font#getWidth(java.lang.String)
	 */
	public int getWidth(String text) {
		return charWidth * text.length();
	}

	/**
	 * @see org.newdawn.slick.Font#getLineHeight()
	 */
	public int getLineHeight() {
		return charHeight;
	}
}
