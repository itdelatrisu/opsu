
package org.newdawn.slick.font;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.font.GlyphMetrics;
import java.awt.font.GlyphVector;

import org.newdawn.slick.Image;
import org.newdawn.slick.UnicodeFont;

/**
 * Represents the glyph in a font for a unicode codepoint.
 * 
 * @author Nathan Sweet <misc@n4te.com>
 */
public class Glyph {
	/** The code point in which this glyph is found */
	private int codePoint;
	/** The width of this glyph in pixels */
	private short width;
	/** The height of this glyph in pixels */
	private short height;
	/** The offset on the y axis to draw the glyph at */
	private short yOffset;
	/** True if the glyph isn't defined */
	private boolean isMissing;
	/** The shape drawn for this glyph */
	private Shape shape;
	/** The image generated for this glyph */
	private Image image;

	/**
	 * Create a new glyph
	 * 
	 * @param codePoint The code point in which this glyph can be found
	 * @param bounds The bounds that this glrph can fill
	 * @param vector The vector this glyph is part of
	 * @param index The index of this glyph within the vector
	 * @param unicodeFont The font this glyph forms part of
	 */
	public Glyph(int codePoint, Rectangle bounds, GlyphVector vector, int index, UnicodeFont unicodeFont) {
		this.codePoint = codePoint;

		GlyphMetrics metrics = vector.getGlyphMetrics(index);
		int lsb = (int)metrics.getLSB();
		if (lsb > 0) lsb = 0;
		int rsb = (int)metrics.getRSB();
		if (rsb > 0) rsb = 0;

		int glyphWidth = bounds.width - lsb - rsb;
		int glyphHeight = bounds.height;
		if (glyphWidth > 0 && glyphHeight > 0) {
			int padTop = unicodeFont.getPaddingTop();
			int padRight = unicodeFont.getPaddingRight();
			int padBottom = unicodeFont.getPaddingBottom();
			int padLeft = unicodeFont.getPaddingLeft();
			int glyphSpacing = 1; // Needed to prevent filtering problems.
			width = (short)(glyphWidth + padLeft + padRight + glyphSpacing);
			height = (short)(glyphHeight + padTop + padBottom + glyphSpacing);
			yOffset = (short)(unicodeFont.getAscent() + bounds.y - padTop);
		}

		shape = vector.getGlyphOutline(index, -bounds.x + unicodeFont.getPaddingLeft(), -bounds.y + unicodeFont.getPaddingTop());

		isMissing = !unicodeFont.getFont().canDisplay((char)codePoint);
	}

	/**
	 * The unicode codepoint the glyph represents.
	 * 
	 * @return The codepoint the glyph represents
	 */
	public int getCodePoint () {
		return codePoint;
	}

	/**
	 * Returns true if the font does not have a glyph for this codepoint.
	 * 
	 * @return True if this glyph is not defined in the given code point
	 */
	public boolean isMissing () {
		return isMissing;
	}

	/**
	 * The width of the glyph's image.
	 * 
	 * @return The width in pixels of the glyphs image
	 */
	public int getWidth () {
		return width;
	}

	/**
	 * The height of the glyph's image.
	 * 
	 * @return The height in pixels of the glyphs image
	 */
	public int getHeight () {
		return height;
	}

	/**
	 * The shape to use to draw this glyph. This is set to null after the glyph is stored 
	 * in a GlyphPage.
	 * 
	 * @return The shape drawn for this glyph
	 */
	public Shape getShape () {
		return shape;
	}

	/**
	 * Set the shape that should be drawn for this glyph
	 * 
	 * @param shape The shape that should be drawn for this glyph
	 */
	public void setShape(Shape shape) {
		this.shape = shape;
	}

	/**
	 * The image to use for this glyph. This is null until after the glyph is stored in a 
	 * GlyphPage.
	 * 
	 * @return The image that has been generated for this glyph
	 */
	public Image getImage () {
		return image;
	}

	/**
	 * Set the image that has been generated for this glyph
	 * 
	 * @param image The image that has been generated for this glyph
	 */
	public void setImage(Image image) {
		this.image = image;
	}

	/**
	 * The distance from drawing y location to top of this glyph, causing the glyph to sit 
	 * on the baseline.
	 * 
	 * @return The offset on the y axis this glyph should be drawn at
	 */
	public int getYOffset() {
		return yOffset;
	}
}
