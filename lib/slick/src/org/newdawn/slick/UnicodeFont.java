
package org.newdawn.slick;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.awt.font.GlyphVector;
import java.awt.font.TextAttribute;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.newdawn.slick.font.Glyph;
import org.newdawn.slick.font.GlyphPage;
import org.newdawn.slick.font.HieroSettings;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureImpl;
import org.newdawn.slick.opengl.renderer.Renderer;
import org.newdawn.slick.opengl.renderer.SGL;
import org.newdawn.slick.util.ResourceLoader;

/**
 * A Slick bitmap font that can display unicode glyphs from a TrueTypeFont.
 * 
 * For efficiency, glyphs are packed on to textures. Glyphs can be loaded to the textures on the fly, when they are first needed
 * for display. However, it is best to load the glyphs that are known to be needed at startup.
 * @author Nathan Sweet <misc@n4te.com>
 */
public class UnicodeFont implements org.newdawn.slick.Font {
	/** The number of display lists that will be cached for strings from this font */
	private static final int DISPLAY_LIST_CACHE_SIZE = 200;
	/** The highest glyph code allowed */
	static private final int MAX_GLYPH_CODE = 0x10FFFF;
	/** The number of glyphs on a page */
	private static final int PAGE_SIZE = 512;
	/** The number of pages */
	private static final int PAGES = MAX_GLYPH_CODE / PAGE_SIZE;
	/** Interface to OpenGL */
	private static final SGL GL = Renderer.get();
	/** A dummy display list used as a place holder */
	private static final DisplayList EMPTY_DISPLAY_LIST = new DisplayList();

	/**
	 * Utility to create a Java font for a TTF file reference
	 * 
	 * @param ttfFileRef The file system or classpath location of the TrueTypeFont file.
	 * @return The font created
	 * @throws SlickException Indicates a failure to locate or load the font into Java's font
	 * system.
	 */
	private static Font createFont (String ttfFileRef) throws SlickException {
		try {
			return Font.createFont(Font.TRUETYPE_FONT, ResourceLoader.getResourceAsStream(ttfFileRef));
		} catch (FontFormatException ex) {
			throw new SlickException("Invalid font: " + ttfFileRef, ex);
		} catch (IOException ex) {
			throw new SlickException("Error reading font: " + ttfFileRef, ex);
		}
	}

	/**
	 * Sorts glyphs by height, tallest first.
	 */
	private static final Comparator heightComparator = new Comparator() {
		public int compare (Object o1, Object o2) {
			return ((Glyph)o1).getHeight() - ((Glyph)o2).getHeight();
		}
	};
	
	/** The AWT font that is being rendered */
	private Font font;
	/** The reference to the True Type Font file that has kerning information */
	private String ttfFileRef;
	/** The ascent of the font */
	private int ascent;
	/** The decent of the font */
	private int descent;
	/** The leading edge of the font */
	private int leading;
	/** The width of a space for the font */
	private int spaceWidth;
	/** The glyphs that are available in this font */
	private final Glyph[][] glyphs = new Glyph[PAGES][];
	/** The pages that have been loaded for this font */
	private final List glyphPages = new ArrayList();
	/** The glyphs queued up to be rendered */
	private final List queuedGlyphs = new ArrayList(256);
	/** The effects that need to be applied to the font */
	private final List effects = new ArrayList();
	
	/** The padding applied in pixels to the top of the glyph rendered area */
	private int paddingTop;
	/** The padding applied in pixels to the left of the glyph rendered area */
	private int paddingLeft;
	/** The padding applied in pixels to the bottom of the glyph rendered area */
	private int paddingBottom;
	/** The padding applied in pixels to the right of the glyph rendered area */
	private int paddingRight;
	/** The padding applied in pixels to horizontal advance for each glyph */
	private int paddingAdvanceX;
	/** The padding applied in pixels to vertical advance for each glyph */
	private int paddingAdvanceY;
	/** The glyph to display for missing glyphs in code points */
	private Glyph missingGlyph;

	/** The width of the glyph page generated */
	private int glyphPageWidth = 512;
	/** The height of the glyph page generated */
	private int glyphPageHeight = 512;
	
	/** True if display list caching is turned on */
	private boolean displayListCaching = true;
	/** The based display list ID */
	private int baseDisplayListID = -1;
	/** The ID of the display list that has been around the longest time */
	private int eldestDisplayListID;
	/** The eldest display list  */
	private DisplayList eldestDisplayList;
	
	/** The map fo the display list generated and cached - modified to allow removal of the oldest entry */
	private final LinkedHashMap displayLists = new LinkedHashMap(DISPLAY_LIST_CACHE_SIZE, 1, true) {
		protected boolean removeEldestEntry (Entry eldest) {
			DisplayList displayList = (DisplayList)eldest.getValue();
			if (displayList != null) eldestDisplayListID = displayList.id;
			return size() > DISPLAY_LIST_CACHE_SIZE;
		}
	};

	/**
	 * Create a new unicode font based on a TTF file
	 * 
	 * @param ttfFileRef The file system or classpath location of the TrueTypeFont file.
	 * @param hieroFileRef The file system or classpath location of the Hiero settings file.
	 * @throws SlickException if the UnicodeFont could not be initialized.
	 */
	public UnicodeFont (String ttfFileRef, String hieroFileRef) throws SlickException {
		this(ttfFileRef, new HieroSettings(hieroFileRef));
	}

	/**
	 * Create a new unicode font based on a TTF file and a set of heiro configuration
	 * 
	 * @param ttfFileRef The file system or classpath location of the TrueTypeFont file.
	 * @param settings The settings configured via the Hiero tool
	 * @throws SlickException if the UnicodeFont could not be initialized.
	 */
	public UnicodeFont (String ttfFileRef, HieroSettings settings) throws SlickException {
		this.ttfFileRef = ttfFileRef;
		Font font = createFont(ttfFileRef);
		initializeFont(font, settings.getFontSize(), settings.isBold(), settings.isItalic());
		loadSettings(settings);
	}

	/**
	 * Create a new unicode font based on a TTF file alone
	 * 
	 * @param ttfFileRef The file system or classpath location of the TrueTypeFont file.
	 * @param size The point size of the font to generated
	 * @param bold True if the font should be rendered in bold typeface
	 * @param italic True if the font should be rendered in bold typeface
	 * @throws SlickException if the UnicodeFont could not be initialized.
	 */
	public UnicodeFont (String ttfFileRef, int size, boolean bold, boolean italic) throws SlickException {
		this.ttfFileRef = ttfFileRef;
		initializeFont(createFont(ttfFileRef), size, bold, italic);
	}

	/**
	 * Creates a new UnicodeFont.
	 * 
	 * @param font The AWT font to render
	 * @param hieroFileRef The file system or classpath location of the Hiero settings file.
	 * @throws SlickException if the UnicodeFont could not be initialized.
	 */
	public UnicodeFont (Font font, String hieroFileRef) throws SlickException {
		this(font, new HieroSettings(hieroFileRef));
	}

	/**
	 * Creates a new UnicodeFont.
	 * 
	 * @param font The AWT font to render
	 * @param settings The settings configured via the Hiero tool
	 */
	public UnicodeFont (Font font, HieroSettings settings) {
		initializeFont(font, settings.getFontSize(), settings.isBold(), settings.isItalic());
		loadSettings(settings);
	}

	/**
	 * Creates a new UnicodeFont.
	 * 
	 * @param font The AWT font to render
	 */
	public UnicodeFont (Font font) {
		initializeFont(font, font.getSize(), font.isBold(), font.isItalic());
	}

	/**
	 * Creates a new UnicodeFont.
	 * 
	 * @param font The AWT font to render
	 * @param size The point size of the font to generated
	 * @param bold True if the font should be rendered in bold typeface
	 * @param italic True if the font should be rendered in bold typeface
	 */
	public UnicodeFont (Font font, int size, boolean bold, boolean italic) {
		initializeFont(font, size, bold, italic);
	}

	/**
	 * Initialise the font to be used based on configuration
	 * 
	 * @param baseFont The AWT font to render
	 * @param size The point size of the font to generated
	 * @param bold True if the font should be rendered in bold typeface
	 * @param italic True if the font should be rendered in bold typeface
	 */
	private void initializeFont(Font baseFont, int size, boolean bold, boolean italic) {
		Map attributes = baseFont.getAttributes();
		attributes.put(TextAttribute.SIZE, new Float(size));
		attributes.put(TextAttribute.WEIGHT, bold ? TextAttribute.WEIGHT_BOLD : TextAttribute.WEIGHT_REGULAR);
		attributes.put(TextAttribute.POSTURE, italic ? TextAttribute.POSTURE_OBLIQUE : TextAttribute.POSTURE_REGULAR);
		try {
			attributes.put(TextAttribute.class.getDeclaredField("KERNING").get(null), TextAttribute.class.getDeclaredField(
				"KERNING_ON").get(null));
		} catch (Exception ignored) {
		}
		font = baseFont.deriveFont(attributes);

		FontMetrics metrics = GlyphPage.getScratchGraphics().getFontMetrics(font);
		ascent = metrics.getAscent();
		descent = metrics.getDescent();
		leading = metrics.getLeading();
		
		// Determine width of space glyph (getGlyphPixelBounds gives a width of zero).
		char[] chars = " ".toCharArray();
		GlyphVector vector = font.layoutGlyphVector(GlyphPage.renderContext, chars, 0, chars.length, Font.LAYOUT_LEFT_TO_RIGHT);
		spaceWidth = vector.getGlyphLogicalBounds(0).getBounds().width;
	}

	/**
	 * Load the hiero setting and configure the unicode font's rendering
	 * 
	 * @param settings The settings to be applied
	 */
	private void loadSettings(HieroSettings settings) {
		paddingTop = settings.getPaddingTop();
		paddingLeft = settings.getPaddingLeft();
		paddingBottom = settings.getPaddingBottom();
		paddingRight = settings.getPaddingRight();
		paddingAdvanceX = settings.getPaddingAdvanceX();
		paddingAdvanceY = settings.getPaddingAdvanceY();
		glyphPageWidth = settings.getGlyphPageWidth();
		glyphPageHeight = settings.getGlyphPageHeight();
		effects.addAll(settings.getEffects());
	}

	/**
	 * Queues the glyphs in the specified codepoint range (inclusive) to be loaded. Note that the glyphs are not actually loaded
	 * until {@link #loadGlyphs()} is called.
	 * 
	 * Some characters like combining marks and non-spacing marks can only be rendered with the context of other glyphs. In this
	 * case, use {@link #addGlyphs(String)}.
	 * 
	 * @param startCodePoint The code point of the first glyph to add
	 * @param endCodePoint The code point of the last glyph to add
	 */
	public void addGlyphs(int startCodePoint, int endCodePoint) {
		for (int codePoint = startCodePoint; codePoint <= endCodePoint; codePoint++)
			addGlyphs(new String(Character.toChars(codePoint)));
	}

	/**
	 * Queues the glyphs in the specified text to be loaded. Note that the glyphs are not actually loaded until
	 * {@link #loadGlyphs()} is called.
	 * 
	 * @param text The text containing the glyphs to be added
	 */
	public void addGlyphs(String text) {
		if (text == null) throw new IllegalArgumentException("text cannot be null.");

		char[] chars = text.toCharArray();
		GlyphVector vector = font.layoutGlyphVector(GlyphPage.renderContext, chars, 0, chars.length, Font.LAYOUT_LEFT_TO_RIGHT);
		for (int i = 0, n = vector.getNumGlyphs(); i < n; i++) {
			int codePoint = text.codePointAt(vector.getGlyphCharIndex(i));
			Rectangle bounds = getGlyphBounds(vector, i, codePoint);
			getGlyph(vector.getGlyphCode(i), codePoint, bounds, vector, i);
		}
	}

	/**
	 * Queues the glyphs in the ASCII character set (codepoints 32 through 255) to be loaded. Note that the glyphs are not actually
	 * loaded until {@link #loadGlyphs()} is called.
	 */
	public void addAsciiGlyphs () {
		addGlyphs(32, 255);
	}

	/**
	 * Queues the glyphs in the NEHE character set (codepoints 32 through 128) to be loaded. Note that the glyphs are not actually
	 * loaded until {@link #loadGlyphs()} is called.
	 */
	public void addNeheGlyphs () {
		addGlyphs(32, 32 + 96);
	}

	/**
	 * Loads all queued glyphs to the backing textures. Glyphs that are typically displayed together should be added and loaded at
	 * the same time so that they are stored on the same backing texture. This reduces the number of backing texture binds required
	 * to draw glyphs.
	 * 
	 * @return True if the glyphs were loaded entirely
	 * @throws SlickException if the glyphs could not be loaded.
	 */
	public boolean loadGlyphs () throws SlickException {
		return loadGlyphs(-1);
	}

	/**
	 * Loads up to the specified number of queued glyphs to the backing textures. This is typically called from the game loop to
	 * load glyphs on the fly that were requested for display but have not yet been loaded.
	 * 
	 * @param maxGlyphsToLoad The maximum number of glyphs to be loaded this time
	 * @return True if the glyphs were loaded entirely
	 * @throws SlickException if the glyphs could not be loaded.
	 */
	public boolean loadGlyphs (int maxGlyphsToLoad) throws SlickException {
		if (queuedGlyphs.isEmpty()) return false;

		if (effects.isEmpty())
			throw new IllegalStateException("The UnicodeFont must have at least one effect before any glyphs can be loaded.");

		for (Iterator iter = queuedGlyphs.iterator(); iter.hasNext();) {
			Glyph glyph = (Glyph)iter.next();
			int codePoint = glyph.getCodePoint();

			// Don't load an image for a glyph with nothing to display.
			if (glyph.getWidth() == 0 || codePoint == ' ') {
				iter.remove();
				continue;
			}

			// Only load the first missing glyph.
			if (glyph.isMissing()) {
				if (missingGlyph != null) {
					if (glyph != missingGlyph) iter.remove();
					continue;
				}
				missingGlyph = glyph;
			}
		}

		Collections.sort(queuedGlyphs, heightComparator);

		// Add to existing pages.
		for (Iterator iter = glyphPages.iterator(); iter.hasNext();) {
			GlyphPage glyphPage = (GlyphPage)iter.next();
			maxGlyphsToLoad -= glyphPage.loadGlyphs(queuedGlyphs, maxGlyphsToLoad);
			if (maxGlyphsToLoad == 0 || queuedGlyphs.isEmpty())
				return true;
		}

		// Add to new pages.
		while (!queuedGlyphs.isEmpty()) {
			GlyphPage glyphPage = new GlyphPage(this, glyphPageWidth, glyphPageHeight);
			glyphPages.add(glyphPage);
			maxGlyphsToLoad -= glyphPage.loadGlyphs(queuedGlyphs, maxGlyphsToLoad);
			if (maxGlyphsToLoad == 0) return true;
		}

		return true;
	}

	/**
	 * Clears all loaded and queued glyphs.
	 */
	public void clearGlyphs () {
		for (int i = 0; i < PAGES; i++)
			glyphs[i] = null;

		for (Iterator iter = glyphPages.iterator(); iter.hasNext();) {
			GlyphPage page = (GlyphPage)iter.next();
			try {
				page.getImage().destroy();
			} catch (SlickException ignored) {
			}
		}
		glyphPages.clear();

		if (baseDisplayListID != -1) {
			GL.glDeleteLists(baseDisplayListID, displayLists.size());
			baseDisplayListID = -1;
		}

		queuedGlyphs.clear();
		missingGlyph = null;
	}

	/**
	 * Releases all resources used by this UnicodeFont. This method should be called when this UnicodeFont instance is no longer
	 * needed.
	 */
	public void destroy () {
		// The destroy() method is just to provide a consistent API for releasing resources.
		clearGlyphs();
	}

	/**
	 * Identical to {@link #drawString(float, float, String, Color, int, int)} but returns a 
	 * DisplayList which provides access to the width and height of the text drawn.
	 * 
	 * @param text The text to render
	 * @param x The horizontal location to render at
	 * @param y The vertical location to render at
	 * @param color The colour to apply as a filter on the text
	 * @param startIndex The start index into the string to start rendering at
	 * @param endIndex The end index into the string to render to
	 * @return The reference to the display list that was drawn and potentiall ygenerated
	 */
	public DisplayList drawDisplayList (float x, float y, String text, Color color, int startIndex, int endIndex) {
		if (text == null) throw new IllegalArgumentException("text cannot be null.");
		if (text.length() == 0) return EMPTY_DISPLAY_LIST;
		if (color == null) throw new IllegalArgumentException("color cannot be null.");

		x -= paddingLeft;
		y -= paddingTop;

		String displayListKey = text.substring(startIndex, endIndex);

		color.bind();
		TextureImpl.bindNone();

		DisplayList displayList = null;
		if (displayListCaching && queuedGlyphs.isEmpty()) {
			if (baseDisplayListID == -1) {
				baseDisplayListID = GL.glGenLists(DISPLAY_LIST_CACHE_SIZE);
				if (baseDisplayListID == 0) {
					baseDisplayListID = -1;
					displayListCaching = false;
					return new DisplayList();
				}
			}
			// Try to use a display list compiled for this text.
			displayList = (DisplayList)displayLists.get(displayListKey);
			if (displayList != null) {
				if (displayList.invalid)
					displayList.invalid = false;
				else {
					GL.glTranslatef(x, y, 0);
					GL.glCallList(displayList.id);
					GL.glTranslatef(-x, -y, 0);
					return displayList;
				}
			} else if (displayList == null) {
				// Compile a new display list.
				displayList = new DisplayList();
				int displayListCount = displayLists.size();
				displayLists.put(displayListKey, displayList);
				if (displayListCount < DISPLAY_LIST_CACHE_SIZE)
					displayList.id = baseDisplayListID + displayListCount;
				else
					displayList.id = eldestDisplayListID;
			}
			displayLists.put(displayListKey, displayList);
		}

		GL.glTranslatef(x, y, 0);

		if (displayList != null) GL.glNewList(displayList.id, SGL.GL_COMPILE_AND_EXECUTE);

		char[] chars = text.substring(0, endIndex).toCharArray();
		GlyphVector vector = font.layoutGlyphVector(GlyphPage.renderContext, chars, 0, chars.length, Font.LAYOUT_LEFT_TO_RIGHT);

		int maxWidth = 0, totalHeight = 0, lines = 0;
		int extraX = 0, extraY = ascent;
		boolean startNewLine = false;
		Texture lastBind = null;
		for (int glyphIndex = 0, n = vector.getNumGlyphs(); glyphIndex < n; glyphIndex++) {
			int charIndex = vector.getGlyphCharIndex(glyphIndex);
			if (charIndex < startIndex) continue;
			if (charIndex > endIndex) break;

			int codePoint = text.codePointAt(charIndex);

			Rectangle bounds = getGlyphBounds(vector, glyphIndex, codePoint);
			Glyph glyph = getGlyph(vector.getGlyphCode(glyphIndex), codePoint, bounds, vector, glyphIndex);

			if (startNewLine && codePoint != '\n') {
				extraX = -bounds.x;
				startNewLine = false;
			}

			Image image = glyph.getImage();
			if (image == null && missingGlyph != null && glyph.isMissing()) image = missingGlyph.getImage();
			if (image != null) {
				// Draw glyph, only binding a new glyph page texture when necessary.
				Texture texture = image.getTexture();
				if (lastBind != null && lastBind != texture) {
					GL.glEnd();
					lastBind = null;
				}
				if (lastBind == null) {
					texture.bind();
					GL.glBegin(SGL.GL_QUADS);
					lastBind = texture;
				}
				image.drawEmbedded(bounds.x + extraX, bounds.y + extraY, image.getWidth(), image.getHeight());
			}

			if (glyphIndex >= 0) extraX += paddingRight + paddingLeft + paddingAdvanceX;
			maxWidth = Math.max(maxWidth, bounds.x + extraX + bounds.width);
			totalHeight = Math.max(totalHeight, ascent + bounds.y + bounds.height);

			if (codePoint == '\n') {
				startNewLine = true; // Mac gives -1 for bounds.x of '\n', so use the bounds.x of the next glyph.
				extraY += getLineHeight();
				lines++;
				totalHeight = 0;
			}
		}
		if (lastBind != null) GL.glEnd();

		if (displayList != null) {
			GL.glEndList();
			// Invalidate the display list if it had glyphs that need to be loaded.
			if (!queuedGlyphs.isEmpty()) displayList.invalid = true;
		}

		GL.glTranslatef(-x, -y, 0);

		if (displayList == null) displayList = new DisplayList();
		displayList.width = (short)maxWidth;
		displayList.height = (short)(lines * getLineHeight() + totalHeight);
		return displayList;
	}

	public void drawString (float x, float y, String text, Color color, int startIndex, int endIndex) {
		drawDisplayList(x, y, text, color, startIndex, endIndex);
	}

	public void drawString (float x, float y, String text) {
		drawString(x, y, text, Color.white);
	}

	public void drawString (float x, float y, String text, Color col) {
		drawString(x, y, text, col, 0, text.length());
	}

	/**
	 * Returns the glyph for the specified codePoint. If the glyph does not exist yet, 
	 * it is created and queued to be loaded.
	 * 
	 * @param glyphCode The code of the glyph to locate
	 * @param codePoint The code point associated with the glyph
	 * @param bounds The bounds of the glyph on the page
	 * @param vector The vector the glyph is part of  
	 * @param index The index of the glyph within the vector
	 * @return The glyph requested
	 */
	private Glyph getGlyph (int glyphCode, int codePoint, Rectangle bounds, GlyphVector vector, int index) {
		if (glyphCode < 0 || glyphCode >= MAX_GLYPH_CODE) {
			// GlyphVector#getGlyphCode sometimes returns negative numbers on OS X.
			return new Glyph(codePoint, bounds, vector, index, this) {
				public boolean isMissing () {
					return true;
				}
			};
		}
		int pageIndex = glyphCode / PAGE_SIZE;
		int glyphIndex = glyphCode & (PAGE_SIZE - 1);
		Glyph glyph = null;
		Glyph[] page = glyphs[pageIndex];
		if (page != null) {
			glyph = page[glyphIndex];
			if (glyph != null) return glyph;
		} else
			page = glyphs[pageIndex] = new Glyph[PAGE_SIZE];
		// Add glyph so size information is available and queue it so its image can be loaded later.
		glyph = page[glyphIndex] = new Glyph(codePoint, bounds, vector, index, this);
		queuedGlyphs.add(glyph);
		return glyph;
	}

	/**
	 * Returns the bounds of the specified glyph.\
	 * 
	 * @param vector The vector the glyph is part of
	 * @param index The index of the glyph within the vector
	 * @param codePoint The code point associated with the glyph
	 */
	private Rectangle getGlyphBounds (GlyphVector vector, int index, int codePoint) {
		Rectangle bounds = vector.getGlyphPixelBounds(index, GlyphPage.renderContext, 0, 0);
		if (codePoint == ' ') bounds.width = spaceWidth;
		return bounds;
	}

	/**
	 * Returns the width of the space character.
	 */
	public int getSpaceWidth () {
		return spaceWidth;
	}

	/**
	 * @see org.newdawn.slick.Font#getWidth(java.lang.String)
	 */
	public int getWidth (String text) {
		if (text == null) throw new IllegalArgumentException("text cannot be null.");
		if (text.length() == 0) return 0;

		if (displayListCaching) {
			DisplayList displayList = (DisplayList)displayLists.get(text);
			if (displayList != null) return displayList.width;
		}

		char[] chars = text.toCharArray();
		GlyphVector vector = font.layoutGlyphVector(GlyphPage.renderContext, chars, 0, chars.length, Font.LAYOUT_LEFT_TO_RIGHT);

		int width = 0;
		int extraX = 0;
		boolean startNewLine = false;
		for (int glyphIndex = 0, n = vector.getNumGlyphs(); glyphIndex < n; glyphIndex++) {
			int charIndex = vector.getGlyphCharIndex(glyphIndex);
			int codePoint = text.codePointAt(charIndex);
			Rectangle bounds = getGlyphBounds(vector, glyphIndex, codePoint);

			if (startNewLine && codePoint != '\n') extraX = -bounds.x;

			if (glyphIndex > 0) extraX += paddingLeft + paddingRight + paddingAdvanceX;
			width = Math.max(width, bounds.x + extraX + bounds.width);

			if (codePoint == '\n') startNewLine = true;
		}

		return width;
	}

	/**
	 * @see org.newdawn.slick.Font#getHeight(java.lang.String)
	 */
	public int getHeight (String text) {
		if (text == null) throw new IllegalArgumentException("text cannot be null.");
		if (text.length() == 0) return 0;

		if (displayListCaching) {
			DisplayList displayList = (DisplayList)displayLists.get(text);
			if (displayList != null) return displayList.height;
		}

		char[] chars = text.toCharArray();
		GlyphVector vector = font.layoutGlyphVector(GlyphPage.renderContext, chars, 0, chars.length, Font.LAYOUT_LEFT_TO_RIGHT);

		int lines = 0, height = 0;
		for (int i = 0, n = vector.getNumGlyphs(); i < n; i++) {
			int charIndex = vector.getGlyphCharIndex(i);
			int codePoint = text.codePointAt(charIndex);
			if (codePoint == ' ') continue;
			Rectangle bounds = getGlyphBounds(vector, i, codePoint);

			height = Math.max(height, ascent + bounds.y + bounds.height);

			if (codePoint == '\n') {
				lines++;
				height = 0;
			}
		}
		return lines * getLineHeight() + height;
	}

	/**
	 * Returns the distance from the y drawing location to the top most pixel of the 
	 * specified text.
	 * 
	 * @param text The text to analyse 
	 * @return The distance fro the y drawing location ot the top most pixel of the specified text
	 */
	public int getYOffset (String text) {
		if (text == null) throw new IllegalArgumentException("text cannot be null.");

		DisplayList displayList = null;
		if (displayListCaching) {
			displayList = (DisplayList)displayLists.get(text);
			if (displayList != null && displayList.yOffset != null) return displayList.yOffset.intValue();
		}

		int index = text.indexOf('\n');
		if (index != -1) text = text.substring(0, index);
		char[] chars = text.toCharArray();
		GlyphVector vector = font.layoutGlyphVector(GlyphPage.renderContext, chars, 0, chars.length, Font.LAYOUT_LEFT_TO_RIGHT);
		int yOffset = ascent + vector.getPixelBounds(null, 0, 0).y;

		if (displayList != null) displayList.yOffset = new Short((short)yOffset);

		return yOffset;
	}

	/**
	 * Returns the TrueTypeFont for this UnicodeFont.
	 * 
	 * @return The AWT Font being rendered 
	 */
	public Font getFont() {
		return font;
	}

	/**
	 * Returns the padding above a glyph on the GlyphPage to allow for effects to be drawn.
	 * 
	 * @return The padding at the top of the glyphs when drawn
	 */
	public int getPaddingTop() {
		return paddingTop;
	}

	/**
	 * Sets the padding above a glyph on the GlyphPage to allow for effects to be drawn.
	 * 
	 * @param paddingTop The padding at the top of the glyphs when drawn
	 */
	public void setPaddingTop(int paddingTop) {
		this.paddingTop = paddingTop;
	}

	/**
	 * Returns the padding to the left of a glyph on the GlyphPage to allow for effects to be drawn.
	 * 
	 * @return The padding at the left of the glyphs when drawn
	 */
	public int getPaddingLeft() {
		return paddingLeft;
	}

	/**
	 * Sets the padding to the left of a glyph on the GlyphPage to allow for effects to be drawn.
	 * 
	 * @param paddingLeft The padding at the left of the glyphs when drawn
	 */
	public void setPaddingLeft(int paddingLeft) {
		this.paddingLeft = paddingLeft;
	}

	/**
	 * Returns the padding below a glyph on the GlyphPage to allow for effects to be drawn.
	 * 
	 * @return The padding at the bottom of the glyphs when drawn
	 */
	public int getPaddingBottom() {
		return paddingBottom;
	}

	/**
	 * Sets the padding below a glyph on the GlyphPage to allow for effects to be drawn.
	 * 
	 * @param paddingBottom The padding at the bottom of the glyphs when drawn
	 */
	public void setPaddingBottom(int paddingBottom) {
		this.paddingBottom = paddingBottom;
	}

	/**
	 * Returns the padding to the right of a glyph on the GlyphPage to allow for effects to be drawn.
	 * 
	 * @return The padding at the right of the glyphs when drawn
	 */
	public int getPaddingRight () {
		return paddingRight;
	}

	/**
	 * Sets the padding to the right of a glyph on the GlyphPage to allow for effects to be drawn.
	 * 
	 * @param paddingRight The padding at the right of the glyphs when drawn
	 */
	public void setPaddingRight (int paddingRight) {
		this.paddingRight = paddingRight;
	}

	/**
	 * Gets the additional amount to offset glyphs on the x axis.
	 * 
	 * @return The padding applied for each horizontal advance (i.e. when a glyph is rendered)
	 */
	public int getPaddingAdvanceX() {
		return paddingAdvanceX;
	}

	/**
	 * Sets the additional amount to offset glyphs on the x axis. This is typically set to a negative number when left or right
	 * padding is used so that glyphs are not spaced too far apart.
	 * 
	 * @param paddingAdvanceX The padding applied for each horizontal advance (i.e. when a glyph is rendered)
	 */
	public void setPaddingAdvanceX (int paddingAdvanceX) {
		this.paddingAdvanceX = paddingAdvanceX;
	}

	/**
	 * Gets the additional amount to offset a line of text on the y axis.
	 * 
	 * @return The padding applied for each vertical advance (i.e. when a glyph is rendered)
	 */
	public int getPaddingAdvanceY () {
		return paddingAdvanceY;
	}

	/**
	 * Sets the additional amount to offset a line of text on the y axis. This is typically set to a negative number when top or
	 * bottom padding is used so that lines of text are not spaced too far apart.
	 * 
	 * @param paddingAdvanceY The padding applied for each vertical advance (i.e. when a glyph is rendered)
	 */
	public void setPaddingAdvanceY (int paddingAdvanceY) {
		this.paddingAdvanceY = paddingAdvanceY;
	}

	/**
	 * Returns the distance from one line of text to the next. This is the sum of the descent, ascent, leading, padding top,
	 * padding bottom, and padding advance y. To change the line height, use {@link #setPaddingAdvanceY(int)}.
	 */
	public int getLineHeight() {
		return descent + ascent + leading + paddingTop + paddingBottom + paddingAdvanceY;
	}

	/**
	 * Gets the distance from the baseline to the y drawing location.
	 * 
	 * @return The ascent of this font
	 */
	public int getAscent() {
		return ascent;
	}

	/**
	 * Gets the distance from the baseline to the bottom of most alphanumeric characters 
	 * with descenders.
	 * 
	 * @return The distance from the baseline to the bottom of the font
	 */
	public int getDescent () {
		return descent;
	}

	/**
	 * Gets the extra distance between the descent of one line of text to the ascent of the next.
	 * 
	 * @return The leading edge of the font
	 */
	public int getLeading () {
		return leading;
	}

	/**
	 * Returns the width of the backing textures.
	 * 
	 * @return The width of the glyph pages in this font
	 */
	public int getGlyphPageWidth () {
		return glyphPageWidth;
	}

	/**
	 * Sets the width of the backing textures. Default is 512.
	 * 
	 * @param glyphPageWidth The width of the glyph pages in this font
	 */
	public void setGlyphPageWidth(int glyphPageWidth) {
		this.glyphPageWidth = glyphPageWidth;
	}

	/**
	 * Returns the height of the backing textures.
	 * 
	 * @return The height of the glyph pages in this font
	 */
	public int getGlyphPageHeight() {
		return glyphPageHeight;
	}

	/**
	 * Sets the height of the backing textures. Default is 512.
	 * 
	 * @param glyphPageHeight The width of the glyph pages in this font
	 */
	public void setGlyphPageHeight(int glyphPageHeight) {
		this.glyphPageHeight = glyphPageHeight;
	}

	/**
	 * Returns the GlyphPages for this UnicodeFont.
	 * 
	 * @return The glyph pages that have been loaded into this font
	 */
	public List getGlyphPages () {
		return glyphPages;
	}

	/**
	 * Returns a list of {@link org.newdawn.slick.font.effects.Effect}s that will be applied 
	 * to the glyphs.
	 * 
	 * @return The list of effects to be applied to the font
	 */
	public List getEffects () {
		return effects;
	}

	/**
	 * Returns true if this UnicodeFont caches the glyph drawing instructions to 
	 * improve performance.
	 * 
	 * @return True if caching is turned on
	 */
	public boolean isCaching () {
		return displayListCaching;
	}

	/**
	 * Sets if this UnicodeFont caches the glyph drawing instructions to improve performance. 
	 * Default is true. Text rendering is very slow without display list caching.
	 * 
	 * @param displayListCaching True if caching should be turned on
	 */
	public void setDisplayListCaching (boolean displayListCaching) {
		this.displayListCaching = displayListCaching;
	}

	/**
	 * Returns the path to the TTF file for this UnicodeFont, or null. If this UnicodeFont was created without specifying the TTF
	 * file, it will try to determine the path using Sun classes. If this fails, null is returned.
	 * 
	 * @return The reference to the font file that the kerning was loaded from
	 */
	public String getFontFile () {
		if (ttfFileRef == null) {
			// Worst case if this UnicodeFont was loaded without a ttfFileRef, try to get the font file from Sun's classes.
			try {
				Object font2D = Class.forName("sun.font.FontManager").getDeclaredMethod("getFont2D", new Class[] {Font.class})
					.invoke(null, new Object[] {font});
				Field platNameField = Class.forName("sun.font.PhysicalFont").getDeclaredField("platName");
				platNameField.setAccessible(true);
				ttfFileRef = (String)platNameField.get(font2D);
			} catch (Throwable ignored) {
			}
			if (ttfFileRef == null) ttfFileRef = "";
		}
		if (ttfFileRef.length() == 0) return null;
		return ttfFileRef;
	}

	/**
	 * A simple descriptor for display lists cached within this font
	 */
	public static class DisplayList {
		/** True if this display list has been invalidated */
		boolean invalid;
		/** The ID of the display list this descriptor represents */
		int id;
		/** The vertical offset to the top of this display list */
		Short yOffset;

		/** The width of rendered text in the list */
		public short width;
		/** The height of the rendered text in the list */
		public short height;
		/** Application data stored in the list */
		public Object userData;

		DisplayList () {
		}
	}
}
