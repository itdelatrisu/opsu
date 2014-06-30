
package org.newdawn.slick.font;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.newdawn.slick.Color;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.UnicodeFont;
import org.newdawn.slick.font.effects.Effect;
import org.newdawn.slick.opengl.TextureImpl;
import org.newdawn.slick.opengl.renderer.Renderer;
import org.newdawn.slick.opengl.renderer.SGL;

/**
 * Stores a number of glyphs on a single texture.
 * 
 * @author Nathan Sweet <misc@n4te.com>
 */
public class GlyphPage {
	/** The interface to OpenGL */
	private static final SGL GL = Renderer.get();

	/** The maxium size of an individual glyph */
	public static final int MAX_GLYPH_SIZE = 256;

	/** A temporary working buffer */
    private static ByteBuffer scratchByteBuffer = ByteBuffer.allocateDirect(MAX_GLYPH_SIZE * MAX_GLYPH_SIZE * 4);

    static {
		scratchByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    }
    
    /** A temporary working buffer */
    private static IntBuffer scratchIntBuffer = scratchByteBuffer.asIntBuffer();
    
    
	/** A temporary image used to generate the glyph page */
	private static BufferedImage scratchImage = new BufferedImage(MAX_GLYPH_SIZE, MAX_GLYPH_SIZE, BufferedImage.TYPE_INT_ARGB);
	/** The graphics context form the temporary image */
	private static Graphics2D scratchGraphics = (Graphics2D)scratchImage.getGraphics();
	
	static {
		scratchGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		scratchGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		scratchGraphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
	}
	
	/** The render context in which the glyphs will be generated */
    public static FontRenderContext renderContext = scratchGraphics.getFontRenderContext();
	
	/**
	 * Get the scratch graphics used to generate the page of glyphs
	 * 
	 * @return The scratch graphics used to build the page
	 */
	public static Graphics2D getScratchGraphics() {
		return scratchGraphics;
	}
	
	/** The font this page is part of */
	private final UnicodeFont unicodeFont;
	/** The width of this page's image */
	private final int pageWidth;
	/** The height of this page's image */
	private final int pageHeight;
	/** The image containing the glyphs */
	private final Image pageImage;
	/** The x position of the page */
	private int pageX;
	/** The y position of the page */
	private int pageY;
	/** The height of the last row on the page */
	private int rowHeight;
	/** True if the glyphs are ordered */
	private boolean orderAscending;
	/** The list of glyphs on this page */
	private final List pageGlyphs = new ArrayList(32);

	/**
	 * Create a new page of glyphs
	 * 
	 * @param unicodeFont The font this page forms part of
	 * @param pageWidth The width of the backing texture.
	 * @param pageHeight The height of the backing texture.
	 * @throws SlickException if the backing texture could not be created.
	 */
	public GlyphPage(UnicodeFont unicodeFont, int pageWidth, int pageHeight) throws SlickException {
		this.unicodeFont = unicodeFont;
		this.pageWidth = pageWidth;
		this.pageHeight = pageHeight;

		pageImage = new Image(pageWidth, pageHeight);
	}

	/**
	 * Loads glyphs to the backing texture and sets the image on each loaded glyph. Loaded glyphs are removed from the list.
	 * 
	 * If this page already has glyphs and maxGlyphsToLoad is -1, then this method will return 0 if all the new glyphs don't fit.
	 * This reduces texture binds when drawing since glyphs loaded at once are typically displayed together.
	 * @param glyphs The glyphs to load.
	 * @param maxGlyphsToLoad This is the maximum number of glyphs to load from the list. Set to -1 to attempt to load all the
	 *           glyphs.
	 * @return The number of glyphs that were actually loaded.
	 * @throws SlickException if the glyph could not be rendered.
	 */
	public int loadGlyphs (List glyphs, int maxGlyphsToLoad) throws SlickException {
		if (rowHeight != 0 && maxGlyphsToLoad == -1) {
			// If this page has glyphs and we are not loading incrementally, return zero if any of the glyphs don't fit.
			int testX = pageX;
			int testY = pageY;
			int testRowHeight = rowHeight;
			for (Iterator iter = getIterator(glyphs); iter.hasNext();) {
				Glyph glyph = (Glyph)iter.next();
				int width = glyph.getWidth();
				int height = glyph.getHeight();
				if (testX + width >= pageWidth) {
					testX = 0;
					testY += testRowHeight;
					testRowHeight = height;
				} else if (height > testRowHeight) {
					testRowHeight = height;
				}
				if (testY + testRowHeight >= pageWidth) return 0;
				testX += width;
			}
		}

		Color.white.bind();
		pageImage.bind();

		int i = 0;
		for (Iterator iter = getIterator(glyphs); iter.hasNext();) {
			Glyph glyph = (Glyph)iter.next();
			int width = Math.min(MAX_GLYPH_SIZE, glyph.getWidth());
			int height = Math.min(MAX_GLYPH_SIZE, glyph.getHeight());

			if (rowHeight == 0) {
				// The first glyph always fits.
				rowHeight = height;
			} else {
				// Wrap to the next line if needed, or break if no more fit.
				if (pageX + width >= pageWidth) {
					if (pageY + rowHeight + height >= pageHeight) break;
					pageX = 0;
					pageY += rowHeight;
					rowHeight = height;
				} else if (height > rowHeight) {
					if (pageY + height >= pageHeight) break;
					rowHeight = height;
				}
			}

			renderGlyph(glyph, width, height);
			pageGlyphs.add(glyph);

			pageX += width;

			iter.remove();
			i++;
			if (i == maxGlyphsToLoad) {
				// If loading incrementally, flip orderAscending so it won't change, since we'll probably load the rest next time.
				orderAscending = !orderAscending;
				break;
			}
		}

		TextureImpl.bindNone();

		// Every other batch of glyphs added to a page are sorted the opposite way to attempt to keep same size glyps together.
		orderAscending = !orderAscending;

		return i;
	}

	/**
	 * Loads a single glyph to the backing texture, if it fits.
	 * 
	 * @param glyph The glyph to be rendered
	 * @param width The expected width of the glyph
	 * @param height The expected height of the glyph
	 * @throws SlickException if the glyph could not be rendered.
	 */
	private void renderGlyph(Glyph glyph, int width, int height) throws SlickException {
		// Draw the glyph to the scratch image using Java2D.
		scratchGraphics.setComposite(AlphaComposite.Clear);
		scratchGraphics.fillRect(0, 0, MAX_GLYPH_SIZE, MAX_GLYPH_SIZE);
		scratchGraphics.setComposite(AlphaComposite.SrcOver);
		scratchGraphics.setColor(java.awt.Color.white);
		for (Iterator iter = unicodeFont.getEffects().iterator(); iter.hasNext();)
			((Effect)iter.next()).draw(scratchImage, scratchGraphics, unicodeFont, glyph);
		glyph.setShape(null); // The shape will never be needed again.

		WritableRaster raster = scratchImage.getRaster();
		int[] row = new int[width];
		for (int y = 0; y < height; y++) {
			raster.getDataElements(0, y, width, 1, row);
			scratchIntBuffer.put(row);
		}
		GL.glTexSubImage2D(SGL.GL_TEXTURE_2D, 0, pageX, pageY, width, height, SGL.GL_BGRA, SGL.GL_UNSIGNED_BYTE,
			scratchByteBuffer);
		scratchIntBuffer.clear();

		glyph.setImage(pageImage.getSubImage(pageX, pageY, width, height));
	}

	/**
	 * Returns an iterator for the specified glyphs, sorted either ascending or descending.
	 * 
	 * @param glyphs The glyphs to return if present
	 * @return An iterator of the sorted list of glyphs
	 */
	private Iterator getIterator(List glyphs) {
		if (orderAscending) return glyphs.iterator();
		final ListIterator iter = glyphs.listIterator(glyphs.size());
		return new Iterator() {
			public boolean hasNext () {
				return iter.hasPrevious();
			}

			public Object next () {
				return iter.previous();
			}

			public void remove () {
				iter.remove();
			}
		};
	}

	/**
	 * Returns the glyphs stored on this page.
	 * 
	 * @return A list of {@link Glyph} elements on this page
	 */
	public List getGlyphs () {
		return pageGlyphs;
	}

	/**
	 * Returns the backing texture for this page.
	 * 
	 * @return The image of this page of glyphs
	 */
	public Image getImage () {
		return pageImage;
	}
}
