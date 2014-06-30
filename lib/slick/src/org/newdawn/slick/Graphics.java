package org.newdawn.slick;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;

import org.lwjgl.BufferUtils;
import org.newdawn.slick.geom.Rectangle;
import org.newdawn.slick.geom.Shape;
import org.newdawn.slick.geom.ShapeRenderer;
import org.newdawn.slick.opengl.TextureImpl;
import org.newdawn.slick.opengl.renderer.LineStripRenderer;
import org.newdawn.slick.opengl.renderer.Renderer;
import org.newdawn.slick.opengl.renderer.SGL;
import org.newdawn.slick.util.FastTrig;
import org.newdawn.slick.util.Log;

/**
 * A graphics context that can be used to render primatives to the accelerated
 * canvas provided by LWJGL.
 * 
 * @author kevin
 */
public class Graphics {
	/** The renderer to use for all GL operations */
	protected static SGL GL = Renderer.get();
	/** The renderer to use line strips */
	private static LineStripRenderer LSR = Renderer.getLineStripRenderer();

	/** The normal drawing mode */
	public static int MODE_NORMAL = 1;

	/** Draw to the alpha map */
	public static int MODE_ALPHA_MAP = 2;

	/** Draw using the alpha blending */
	public static int MODE_ALPHA_BLEND = 3;

	/** Draw multiplying the source and destination colours */
	public static int MODE_COLOR_MULTIPLY = 4;
	
	/** Draw adding the existing colour to the new colour */
	public static int MODE_ADD = 5;
	
	/** Draw blending the new image into the old one by a factor of it's colour */
	public static int MODE_SCREEN = 6;
	
	/** The default number of segments that will be used when drawing an oval */
	private static final int DEFAULT_SEGMENTS = 50;

	/** The last graphics context in use */
	protected static Graphics currentGraphics = null;
	
	/** The default font to use */
	protected static Font DEFAULT_FONT;

	/** The last set scale */
	private float sx = 1;
	/** The last set scale */
	private float sy = 1;
	
	/**
	 * Set the current graphics context in use
	 * 
	 * @param current The graphics context that should be considered current
	 */
	public static void setCurrent(Graphics current) {
		if (currentGraphics != current) {
			if (currentGraphics != null) {
				currentGraphics.disable();
			}
			currentGraphics = current;
			currentGraphics.enable();
		}
	}
	
	/** The font in use */
	private Font font;

	/** The current color */
	private Color currentColor = Color.white;

	/** The width of the screen */
	protected int screenWidth;

	/** The height of the screen */
	protected int screenHeight;

	/** True if the matrix has been pushed to the stack */
	private boolean pushed;

	/** The graphics context clipping */
	private Rectangle clip;

	/** Buffer used for setting the world clip */
	private DoubleBuffer worldClip = BufferUtils.createDoubleBuffer(4);

	/** The buffer used to read a screen pixel */
	private ByteBuffer readBuffer = BufferUtils.createByteBuffer(4);

	/** True if we're antialias */
	private boolean antialias;

	/** The world clip recorded since last set */
	private Rectangle worldClipRecord;

	/** The current drawing mode */
	private int currentDrawingMode = MODE_NORMAL;

	/** The current line width */
	private float lineWidth = 1;

	/** The matrix stack */
	private ArrayList stack = new ArrayList();
	/** The index into the stack we're using */
	private int stackIndex;

	/**
	 * Default constructor for sub-classes
	 */
	public Graphics() {	
	}
	
	/**
	 * Create a new graphics context. Only the container should be doing this
	 * really
	 * 
	 * @param width
	 *            The width of the screen for this context
	 * @param height
	 *            The height of the screen for this context
	 */
	public Graphics(int width, int height) {
		if (DEFAULT_FONT == null) {
			AccessController.doPrivileged(new PrivilegedAction() {
				public Object run() {
					try {
						DEFAULT_FONT = new AngelCodeFont(
								"org/newdawn/slick/data/defaultfont.fnt",
								"org/newdawn/slick/data/defaultfont.png");
					} catch (SlickException e) {
						Log.error(e);
					}
					return null; // nothing to return
				}
			});
		}
		
		this.font = DEFAULT_FONT;
		screenWidth = width;
		screenHeight = height;
	}

	/**
	 * Set the dimensions considered by the graphics context
	 * 
	 * @param width The width of the graphics context
	 * @param height The height of the graphics context
	 */
	void setDimensions(int width, int height) {
		screenWidth = width;
		screenHeight = height;
	}
	
	/**
	 * Set the drawing mode to use. This mode defines how pixels are drawn to
	 * the graphics context. It can be used to draw into the alpha map.
	 * 
	 * The mode supplied should be one of {@link Graphics#MODE_NORMAL} or
	 * {@link Graphics#MODE_ALPHA_MAP} or {@link Graphics#MODE_ALPHA_BLEND}
	 * 
	 * @param mode
	 *            The mode to apply.
	 */
	public void setDrawMode(int mode) {
		predraw();
		currentDrawingMode = mode;
		if (currentDrawingMode == MODE_NORMAL) {
			GL.glEnable(SGL.GL_BLEND);
			GL.glColorMask(true, true, true, true);
			GL.glBlendFunc(SGL.GL_SRC_ALPHA, SGL.GL_ONE_MINUS_SRC_ALPHA);
		}
		if (currentDrawingMode == MODE_ALPHA_MAP) {
			GL.glDisable(SGL.GL_BLEND);
			GL.glColorMask(false, false, false, true);
		}
		if (currentDrawingMode == MODE_ALPHA_BLEND) {
			GL.glEnable(SGL.GL_BLEND);
			GL.glColorMask(true, true, true, false);
			GL.glBlendFunc(SGL.GL_DST_ALPHA, SGL.GL_ONE_MINUS_DST_ALPHA);
		}
		if (currentDrawingMode == MODE_COLOR_MULTIPLY) {
			GL.glEnable(SGL.GL_BLEND);
			GL.glColorMask(true, true, true, true);
			GL.glBlendFunc(SGL.GL_ONE_MINUS_SRC_COLOR, SGL.GL_SRC_COLOR);
		}
		if (currentDrawingMode == MODE_ADD) {
			GL.glEnable(SGL.GL_BLEND);
			GL.glColorMask(true, true, true, true);
			GL.glBlendFunc(SGL.GL_ONE, SGL.GL_ONE);
		}
		if (currentDrawingMode == MODE_SCREEN) {
			GL.glEnable(SGL.GL_BLEND);
			GL.glColorMask(true, true, true, true);
			GL.glBlendFunc(SGL.GL_ONE, SGL.GL_ONE_MINUS_SRC_COLOR);
		}
		postdraw();
	}

	/**
	 * Clear the state of the alpha map across the entire screen. This sets
	 * alpha to 0 everywhere, meaning in {@link Graphics#MODE_ALPHA_BLEND}
	 * nothing will be drawn.
	 */
	public void clearAlphaMap() {
		pushTransform();
		GL.glLoadIdentity();
		
		int originalMode = currentDrawingMode;
		setDrawMode(MODE_ALPHA_MAP);
		setColor(new Color(0,0,0,0));
		fillRect(0, 0, screenWidth, screenHeight);
		setColor(currentColor);
		setDrawMode(originalMode);
		
		popTransform();
	}

	/**
	 * Must be called before all OpenGL operations to maintain context for
	 * dynamic images
	 */
	private void predraw() {
		setCurrent(this);
	}

	/**
	 * Must be called after all OpenGL operations to maintain context for
	 * dynamic images
	 */
	private void postdraw() {
	}

	/**
	 * Enable rendering to this graphics context
	 */
	protected void enable() {
	}

	/**
	 * Flush this graphics context to the underlying rendering context
	 */
	public void flush() {
		if (currentGraphics == this) {
			currentGraphics.disable();
			currentGraphics = null;
		}
	}

	/**
	 * Disable rendering to this graphics context
	 */
	protected void disable() {
	}

	/**
	 * Get the current font
	 * 
	 * @return The current font
	 */
	public Font getFont() {
		return font;
	}

	/**
	 * Set the background colour of the graphics context. This colour
	 * is used when clearing the context. Note that calling this method
	 * alone does not cause the context to be cleared.
	 * 
	 * @param color
	 *            The background color of the graphics context
	 */
	public void setBackground(Color color) {
		predraw();
		GL.glClearColor(color.r, color.g, color.b, color.a);
		postdraw();
	}

	/**
	 * Get the current graphics context background color
	 * 
	 * @return The background color of this graphics context
	 */
	public Color getBackground() {
		predraw();
		FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
		GL.glGetFloat(SGL.GL_COLOR_CLEAR_VALUE, buffer);
		postdraw();

		return new Color(buffer);
	}

	/**
	 * Clear the graphics context
	 */
	public void clear() {
		predraw();
		GL.glClear(SGL.GL_COLOR_BUFFER_BIT);
		postdraw();
	}

	/**
	 * Reset the transformation on this graphics context
	 */
	public void resetTransform() {
		sx = 1;
		sy = 1;
		
		if (pushed) {
			predraw();
			GL.glPopMatrix();
			pushed = false;
			postdraw();
		}
	}

	/**
	 * Check if we've pushed the previous matrix, if not then push it now.
	 */
	private void checkPush() {
		if (!pushed) {
			predraw();
			GL.glPushMatrix();
			pushed = true;
			postdraw();
		}
	}

	/**
	 * Apply a scaling factor to everything drawn on the graphics context
	 * 
	 * @param sx
	 *            The scaling factor to apply to the x axis
	 * @param sy
	 *            The scaling factor to apply to the y axis
	 */
	public void scale(float sx, float sy) {
		this.sx = this.sx * sx;
		this.sy = this.sy * sy;
		
		checkPush();

		predraw();
		GL.glScalef(sx, sy, 1);
		postdraw();
	}

	/**
	 * Apply a rotation to everything draw on the graphics context
	 * 
	 * @param rx
	 *            The x coordinate of the center of rotation
	 * @param ry
	 *            The y coordinate of the center of rotation
	 * @param ang
	 *            The angle (in degrees) to rotate by
	 */
	public void rotate(float rx, float ry, float ang) {
		checkPush();

		predraw();
		translate(rx, ry);
		GL.glRotatef(ang, 0, 0, 1);
		translate(-rx, -ry);
		postdraw();
	}

	/**
	 * Apply a translation to everything drawn to the context
	 * 
	 * @param x
	 *            The amount to translate on the x-axis
	 * @param y
	 *            The amount of translate on the y-axis
	 */
	public void translate(float x, float y) {
		checkPush();

		predraw();
		GL.glTranslatef(x, y, 0);
		postdraw();
	}

	/**
	 * Set the font to be used when rendering text
	 * 
	 * @param font
	 *            The font to be used when rendering text
	 */
	public void setFont(Font font) {
		this.font = font;
	}

	/**
	 * Reset to using the default font for this context
	 */
	public void resetFont() {
		font = DEFAULT_FONT;
	}

	/**
	 * Set the color to use when rendering to this context
	 * 
	 * @param color
	 *            The color to use when rendering to this context
	 */
	public void setColor(Color color) {
		if (color == null) {
			return;
		}
		
		currentColor = new Color(color);
		predraw();
		currentColor.bind();
		postdraw();
	}

	/**
	 * Get the color in use by this graphics context
	 * 
	 * @return The color in use by this graphics context
	 */
	public Color getColor() {
		return new Color(currentColor);
	}

	/**
	 * Draw a line on the canvas in the current colour
	 * 
	 * @param x1
	 *            The x coordinate of the start point
	 * @param y1
	 *            The y coordinate of the start point
	 * @param x2
	 *            The x coordinate of the end point
	 * @param y2
	 *            The y coordinate of the end point
	 */
	public void drawLine(float x1, float y1, float x2, float y2) {
		float lineWidth = this.lineWidth - 1;
		
		if (LSR.applyGLLineFixes()) {
			if (x1 == x2) {
				if (y1 > y2) {
					float temp = y2;
					y2 = y1;
					y1 = temp;
				}
				float step = 1 / sy;
				lineWidth = lineWidth / sy;
				fillRect(x1-(lineWidth/2.0f),y1-(lineWidth/2.0f),lineWidth+step,(y2-y1)+lineWidth+step);
				return;
			} else if (y1 == y2) {
				if (x1 > x2) {
					float temp = x2;
					x2 = x1;
					x1 = temp;
				}
				float step = 1 / sx;
				lineWidth = lineWidth / sx;
				fillRect(x1-(lineWidth/2.0f),y1-(lineWidth/2.0f),(x2-x1)+lineWidth+step,lineWidth+step);
				return;
			}
		}
		
		predraw();
		currentColor.bind();
		TextureImpl.bindNone();

		LSR.start();
		LSR.vertex(x1,y1);
		LSR.vertex(x2,y2);
		LSR.end();
		
		postdraw();
	}

	/**
	 * Draw the outline of the given shape.
	 * 
	 * @param shape
	 *            The shape to draw.
	 * @param fill
	 *            The fill type to apply
	 */
	public void draw(Shape shape, ShapeFill fill) {
		predraw();
		TextureImpl.bindNone();

		ShapeRenderer.draw(shape, fill);

		currentColor.bind();
		postdraw();
	}

	/**
	 * Draw the the given shape filled in.
	 * 
	 * @param shape
	 *            The shape to fill.
	 * @param fill
	 *            The fill type to apply
	 */
	public void fill(Shape shape, ShapeFill fill) {
		predraw();
		TextureImpl.bindNone();

		ShapeRenderer.fill(shape, fill);

		currentColor.bind();
		postdraw();
	}

	/**
	 * Draw the outline of the given shape.
	 * 
	 * @param shape
	 *            The shape to draw.
	 */
	public void draw(Shape shape) {
		predraw();
		TextureImpl.bindNone();
		currentColor.bind();

		ShapeRenderer.draw(shape);

		postdraw();
	}

	/**
	 * Draw the the given shape filled in.
	 * 
	 * @param shape
	 *            The shape to fill.
	 */
	public void fill(Shape shape) {
		predraw();
		TextureImpl.bindNone();
		currentColor.bind();

		ShapeRenderer.fill(shape);

		postdraw();
	}

	/**
	 * Draw the the given shape filled in with a texture
	 * 
	 * @param shape
	 *            The shape to texture.
	 * @param image
	 *            The image to tile across the shape
	 */
	public void texture(Shape shape, Image image) {
		texture(shape, image, 0.01f, 0.01f, false);
	}

	/**
	 * Draw the the given shape filled in with a texture
	 * 
	 * @param shape
	 *            The shape to texture.
	 * @param image
	 *            The image to tile across the shape
	 * @param fill
	 *            The shape fill to apply
	 */
	public void texture(Shape shape, Image image, ShapeFill fill) {
		texture(shape, image, 0.01f, 0.01f, fill);
	}

	/**
	 * Draw the the given shape filled in with a texture
	 * 
	 * @param shape
	 *            The shape to texture.
	 * @param image
	 *            The image to tile across the shape
	 * @param fit
	 *            True if we want to fit the image on to the shape
	 */
	public void texture(Shape shape, Image image, boolean fit) {
		if (fit) {
			texture(shape, image, 1, 1, true);
		} else {
			texture(shape, image, 0.01f, 0.01f, false);
		}
	}

	/**
	 * Draw the the given shape filled in with a texture
	 * 
	 * @param shape
	 *            The shape to texture.
	 * @param image
	 *            The image to tile across the shape
	 * @param scaleX
	 *            The scale to apply on the x axis for texturing
	 * @param scaleY
	 *            The scale to apply on the y axis for texturing
	 */
	public void texture(Shape shape, Image image, float scaleX, float scaleY) {
		texture(shape, image, scaleX, scaleY, false);
	}

	/**
	 * Draw the the given shape filled in with a texture
	 * 
	 * @param shape
	 *            The shape to texture.
	 * @param image
	 *            The image to tile across the shape
	 * @param scaleX
	 *            The scale to apply on the x axis for texturing
	 * @param scaleY
	 *            The scale to apply on the y axis for texturing
	 * @param fit
	 *            True if we want to fit the image on to the shape
	 */
	public void texture(Shape shape, Image image, float scaleX, float scaleY,
			boolean fit) {
		predraw();
		TextureImpl.bindNone();
		currentColor.bind();

		if (fit) {
			ShapeRenderer.textureFit(shape, image, scaleX, scaleY);
		} else {
			ShapeRenderer.texture(shape, image, scaleX, scaleY);
		}
		
		postdraw();
	}

	/**
	 * Draw the the given shape filled in with a texture
	 * 
	 * @param shape
	 *            The shape to texture.
	 * @param image
	 *            The image to tile across the shape
	 * @param scaleX
	 *            The scale to apply on the x axis for texturing
	 * @param scaleY
	 *            The scale to apply on the y axis for texturing
	 * @param fill
	 *            The shape fill to apply
	 */
	public void texture(Shape shape, Image image, float scaleX, float scaleY,
			ShapeFill fill) {
		predraw();
		TextureImpl.bindNone();
		currentColor.bind();

		ShapeRenderer.texture(shape, image, scaleX, scaleY, fill);

		postdraw();
	}

	/**
	 * Draw a rectangle to the canvas in the current colour
	 * 
	 * @param x1
	 *            The x coordinate of the top left corner
	 * @param y1
	 *            The y coordinate of the top left corner
	 * @param width
	 *            The width of the rectangle to draw
	 * @param height
	 *            The height of the rectangle to draw
	 */
	public void drawRect(float x1, float y1, float width, float height) {
		float lineWidth = getLineWidth();
		
		drawLine(x1,y1,x1+width,y1);
		drawLine(x1+width,y1,x1+width,y1+height);
		drawLine(x1+width,y1+height,x1,y1+height);
		drawLine(x1,y1+height,x1,y1);
	}

	/**
	 * Clear the clipping being applied. This will allow graphics to be drawn
	 * anywhere on the screen
	 */
	public void clearClip() {
		clip = null;
		predraw();
		GL.glDisable(SGL.GL_SCISSOR_TEST);
		postdraw();
	}

	/**
	 * Set clipping that controls which areas of the world will be drawn to.
	 * Note that world clip is different from standard screen clip in that it's
	 * defined in the space of the current world coordinate - i.e. it's affected
	 * by translate, rotate, scale etc.
	 * 
	 * @param x
	 *            The x coordinate of the top left corner of the allowed area
	 * @param y
	 *            The y coordinate of the top left corner of the allowed area
	 * @param width
	 *            The width of the allowed area
	 * @param height
	 *            The height of the allowed area
	 */
	public void setWorldClip(float x, float y, float width, float height) {
		predraw();
		worldClipRecord = new Rectangle(x, y, width, height);
		
		GL.glEnable(SGL.GL_CLIP_PLANE0);
		worldClip.put(1).put(0).put(0).put(-x).flip();
		GL.glClipPlane(SGL.GL_CLIP_PLANE0, worldClip);
		GL.glEnable(SGL.GL_CLIP_PLANE1);
		worldClip.put(-1).put(0).put(0).put(x + width).flip();
		GL.glClipPlane(SGL.GL_CLIP_PLANE1, worldClip);

		GL.glEnable(SGL.GL_CLIP_PLANE2);
		worldClip.put(0).put(1).put(0).put(-y).flip();
		GL.glClipPlane(SGL.GL_CLIP_PLANE2, worldClip);
		GL.glEnable(SGL.GL_CLIP_PLANE3);
		worldClip.put(0).put(-1).put(0).put(y + height).flip();
		GL.glClipPlane(SGL.GL_CLIP_PLANE3, worldClip);
		postdraw();
	}

	/**
	 * Clear world clipping setup. This does not effect screen clipping
	 */
	public void clearWorldClip() {
		predraw();
		worldClipRecord = null;
		GL.glDisable(SGL.GL_CLIP_PLANE0);
		GL.glDisable(SGL.GL_CLIP_PLANE1);
		GL.glDisable(SGL.GL_CLIP_PLANE2);
		GL.glDisable(SGL.GL_CLIP_PLANE3);
		postdraw();
	}

	/**
	 * Set the world clip to be applied
	 * 
	 * @see #setWorldClip(float, float, float, float)
	 * @param clip
	 *            The area still visible
	 */
	public void setWorldClip(Rectangle clip) {
		if (clip == null) {
			clearWorldClip();
		} else {
			setWorldClip(clip.getX(), clip.getY(), clip.getWidth(), clip
					.getHeight());
		}
	}

	/**
	 * Get the last set world clip or null of the world clip isn't set
	 * 
	 * @return The last set world clip rectangle
	 */
	public Rectangle getWorldClip() {
		return worldClipRecord;
	}

	/**
	 * Set the clipping to apply to the drawing. Note that this clipping takes
	 * no note of the transforms that have been applied to the context and is
	 * always in absolute screen space coordinates.
	 * 
	 * @param x
	 *            The x coordinate of the top left corner of the allowed area
	 * @param y
	 *            The y coordinate of the top left corner of the allowed area
	 * @param width
	 *            The width of the allowed area
	 * @param height
	 *            The height of the allowed area
	 */
	public void setClip(int x, int y, int width, int height) {
		predraw();
		
		if (clip == null) {
			GL.glEnable(SGL.GL_SCISSOR_TEST);
			clip = new Rectangle(x, y, width, height);
		} else {
			clip.setBounds(x,y,width,height);
		}
		
		GL.glScissor(x, screenHeight - y - height, width, height);
		postdraw();
	}

	/**
	 * Set the clipping to apply to the drawing. Note that this clipping takes
	 * no note of the transforms that have been applied to the context and is
	 * always in absolute screen space coordinates.
	 * 
	 * @param rect
	 *            The rectangle describing the clipped area in screen
	 *            coordinates
	 */
	public void setClip(Rectangle rect) {
		if (rect == null) {
			clearClip();
			return;
		}

		setClip((int) rect.getX(), (int) rect.getY(), (int) rect.getWidth(),
				(int) rect.getHeight());
	}

	/**
	 * Return the currently applied clipping rectangle
	 * 
	 * @return The current applied clipping rectangle or null if no clipping is
	 *         applied
	 */
	public Rectangle getClip() {
		return clip;
	}

	/**
	 * Tile a rectangle with a pattern specifing the offset from the top corner
	 * that one tile should match
	 * 
	 * @param x
	 *            The x coordinate of the rectangle
	 * @param y
	 *            The y coordinate of the rectangle
	 * @param width
	 *            The width of the rectangle
	 * @param height
	 *            The height of the rectangle
	 * @param pattern
	 *            The image to tile across the rectangle
	 * @param offX
	 *            The offset on the x axis from the top left corner
	 * @param offY
	 *            The offset on the y axis from the top left corner
	 */
	public void fillRect(float x, float y, float width, float height,
			Image pattern, float offX, float offY) {
		int cols = ((int) Math.ceil(width / pattern.getWidth())) + 2;
		int rows = ((int) Math.ceil(height / pattern.getHeight())) + 2;

		Rectangle preClip = getWorldClip();
		setWorldClip(x, y, width, height);

		predraw();
		// Draw all the quads we need
		for (int c = 0; c < cols; c++) {
			for (int r = 0; r < rows; r++) {
				pattern.draw(c * pattern.getWidth() + x - offX, r
						* pattern.getHeight() + y - offY);
			}
		}
		postdraw();

		setWorldClip(preClip);
	}

	/**
	 * Fill a rectangle on the canvas in the current color
	 * 
	 * @param x1
	 *            The x coordinate of the top left corner
	 * @param y1
	 *            The y coordinate of the top left corner
	 * @param width
	 *            The width of the rectangle to fill
	 * @param height
	 *            The height of the rectangle to fill
	 */
	public void fillRect(float x1, float y1, float width, float height) {
		predraw();
		TextureImpl.bindNone();
		currentColor.bind();

		GL.glBegin(SGL.GL_QUADS);
		GL.glVertex2f(x1, y1);
		GL.glVertex2f(x1 + width, y1);
		GL.glVertex2f(x1 + width, y1 + height);
		GL.glVertex2f(x1, y1 + height);
		GL.glEnd();
		postdraw();
	}

	/**
	 * Draw an oval to the canvas
	 * 
	 * @param x1
	 *            The x coordinate of the top left corner of a box containing
	 *            the oval
	 * @param y1
	 *            The y coordinate of the top left corner of a box containing
	 *            the oval
	 * @param width
	 *            The width of the oval
	 * @param height
	 *            The height of the oval
	 */
	public void drawOval(float x1, float y1, float width, float height) {
		drawOval(x1, y1, width, height, DEFAULT_SEGMENTS);
	}

	/**
	 * Draw an oval to the canvas
	 * 
	 * @param x1
	 *            The x coordinate of the top left corner of a box containing
	 *            the oval
	 * @param y1
	 *            The y coordinate of the top left corner of a box containing
	 *            the oval
	 * @param width
	 *            The width of the oval
	 * @param height
	 *            The height of the oval
	 * @param segments
	 *            The number of line segments to use when drawing the oval
	 */
	public void drawOval(float x1, float y1, float width, float height,
			int segments) {
		drawArc(x1, y1, width, height, segments, 0, 360);
	}

	/**
	 * Draw an oval to the canvas
	 * 
	 * @param x1
	 *            The x coordinate of the top left corner of a box containing
	 *            the arc
	 * @param y1
	 *            The y coordinate of the top left corner of a box containing
	 *            the arc
	 * @param width
	 *            The width of the arc
	 * @param height
	 *            The height of the arc
	 * @param start
	 *            The angle the arc starts at
	 * @param end
	 *            The angle the arc ends at
	 */
	public void drawArc(float x1, float y1, float width, float height,
			float start, float end) {
		drawArc(x1, y1, width, height, DEFAULT_SEGMENTS, start, end);
	}

	/**
	 * Draw an oval to the canvas
	 * 
	 * @param x1
	 *            The x coordinate of the top left corner of a box containing
	 *            the arc
	 * @param y1
	 *            The y coordinate of the top left corner of a box containing
	 *            the arc
	 * @param width
	 *            The width of the arc
	 * @param height
	 *            The height of the arc
	 * @param segments
	 *            The number of line segments to use when drawing the arc
	 * @param start
	 *            The angle the arc starts at
	 * @param end
	 *            The angle the arc ends at
	 */
	public void drawArc(float x1, float y1, float width, float height,
			int segments, float start, float end) {
		predraw();
		TextureImpl.bindNone();
		currentColor.bind();

		while (end < start) {
			end += 360;
		}

		float cx = x1 + (width / 2.0f);
		float cy = y1 + (height / 2.0f);

		LSR.start();
		int step = 360 / segments;

		for (int a = (int) start; a < (int) (end + step); a += step) {
			float ang = a;
			if (ang > end) {
				ang = end;
			}
			float x = (float) (cx + (FastTrig.cos(Math.toRadians(ang)) * width / 2.0f));
			float y = (float) (cy + (FastTrig.sin(Math.toRadians(ang)) * height / 2.0f));

			LSR.vertex(x,y);
		}
		LSR.end();
		postdraw();
	}

	/**
	 * Fill an oval to the canvas
	 * 
	 * @param x1
	 *            The x coordinate of the top left corner of a box containing
	 *            the oval
	 * @param y1
	 *            The y coordinate of the top left corner of a box containing
	 *            the oval
	 * @param width
	 *            The width of the oval
	 * @param height
	 *            The height of the oval
	 */
	public void fillOval(float x1, float y1, float width, float height) {
		fillOval(x1, y1, width, height, DEFAULT_SEGMENTS);
	}

	/**
	 * Fill an oval to the canvas
	 * 
	 * @param x1
	 *            The x coordinate of the top left corner of a box containing
	 *            the oval
	 * @param y1
	 *            The y coordinate of the top left corner of a box containing
	 *            the oval
	 * @param width
	 *            The width of the oval
	 * @param height
	 *            The height of the oval
	 * @param segments
	 *            The number of line segments to use when filling the oval
	 */
	public void fillOval(float x1, float y1, float width, float height,
			int segments) {
		fillArc(x1, y1, width, height, segments, 0, 360);
	}

	/**
	 * Fill an arc to the canvas (a wedge)
	 * 
	 * @param x1
	 *            The x coordinate of the top left corner of a box containing
	 *            the arc
	 * @param y1
	 *            The y coordinate of the top left corner of a box containing
	 *            the arc
	 * @param width
	 *            The width of the arc
	 * @param height
	 *            The height of the arc
	 * @param start
	 *            The angle the arc starts at
	 * @param end
	 *            The angle the arc ends at
	 */
	public void fillArc(float x1, float y1, float width, float height,
			float start, float end) {
		fillArc(x1, y1, width, height, DEFAULT_SEGMENTS, start, end);
	}

	/**
	 * Fill an arc to the canvas (a wedge)
	 * 
	 * @param x1
	 *            The x coordinate of the top left corner of a box containing
	 *            the arc
	 * @param y1
	 *            The y coordinate of the top left corner of a box containing
	 *            the arc
	 * @param width
	 *            The width of the arc
	 * @param height
	 *            The height of the arc
	 * @param segments
	 *            The number of line segments to use when filling the arc
	 * @param start
	 *            The angle the arc starts at
	 * @param end
	 *            The angle the arc ends at
	 */
	public void fillArc(float x1, float y1, float width, float height,
			int segments, float start, float end) {
		predraw();
		TextureImpl.bindNone();
		currentColor.bind();

		while (end < start) {
			end += 360;
		}

		float cx = x1 + (width / 2.0f);
		float cy = y1 + (height / 2.0f);

		GL.glBegin(SGL.GL_TRIANGLE_FAN);
		int step = 360 / segments;

		GL.glVertex2f(cx, cy);

		for (int a = (int) start; a < (int) (end + step); a += step) {
			float ang = a;
			if (ang > end) {
				ang = end;
			}

			float x = (float) (cx + (FastTrig.cos(Math.toRadians(ang)) * width / 2.0f));
			float y = (float) (cy + (FastTrig.sin(Math.toRadians(ang)) * height / 2.0f));

			GL.glVertex2f(x, y);
		}
		GL.glEnd();

		if (antialias) {
			GL.glBegin(SGL.GL_TRIANGLE_FAN);
			GL.glVertex2f(cx, cy);
			if (end != 360) {
				end -= 10;
			}

			for (int a = (int) start; a < (int) (end + step); a += step) {
				float ang = a;
				if (ang > end) {
					ang = end;
				}

				float x = (float) (cx + (FastTrig.cos(Math.toRadians(ang + 10))
						* width / 2.0f));
				float y = (float) (cy + (FastTrig.sin(Math.toRadians(ang + 10))
						* height / 2.0f));

				GL.glVertex2f(x, y);
			}
			GL.glEnd();
		}

		postdraw();
	}

	/**
	 * Draw a rounded rectangle
	 * 
	 * @param x
	 *            The x coordinate of the top left corner of the rectangle
	 * @param y
	 *            The y coordinate of the top left corner of the rectangle
	 * @param width
	 *            The width of the rectangle
	 * @param height
	 *            The height of the rectangle
	 * @param cornerRadius
	 *            The radius of the rounded edges on the corners
	 */
	public void drawRoundRect(float x, float y, float width, float height,
			int cornerRadius) {
		drawRoundRect(x, y, width, height, cornerRadius, DEFAULT_SEGMENTS);
	}

	/**
	 * Draw a rounded rectangle
	 * 
	 * @param x
	 *            The x coordinate of the top left corner of the rectangle
	 * @param y
	 *            The y coordinate of the top left corner of the rectangle
	 * @param width
	 *            The width of the rectangle
	 * @param height
	 *            The height of the rectangle
	 * @param cornerRadius
	 *            The radius of the rounded edges on the corners
	 * @param segs
	 *            The number of segments to make the corners out of
	 */
	public void drawRoundRect(float x, float y, float width, float height,
			int cornerRadius, int segs) {
		if (cornerRadius < 0)
			throw new IllegalArgumentException("corner radius must be > 0");
		if (cornerRadius == 0) {
			drawRect(x, y, width, height);
			return;
		}

		int mr = (int) Math.min(width, height) / 2;
		// make sure that w & h are larger than 2*cornerRadius
		if (cornerRadius > mr) {
			cornerRadius = mr;
		}

		drawLine(x + cornerRadius, y, x + width - cornerRadius, y);
		drawLine(x, y + cornerRadius, x, y + height - cornerRadius);
		drawLine(x + width, y + cornerRadius, x + width, y + height
				- cornerRadius);
		drawLine(x + cornerRadius, y + height, x + width - cornerRadius, y
				+ height);

		float d = cornerRadius * 2;
		// bottom right - 0, 90
		drawArc(x + width - d, y + height - d, d, d, segs, 0, 90);
		// bottom left - 90, 180
		drawArc(x, y + height - d, d, d, segs, 90, 180);
		// top right - 270, 360
		drawArc(x + width - d, y, d, d, segs, 270, 360);
		// top left - 180, 270
		drawArc(x, y, d, d, segs, 180, 270);
	}

	/**
	 * Fill a rounded rectangle
	 * 
	 * @param x
	 *            The x coordinate of the top left corner of the rectangle
	 * @param y
	 *            The y coordinate of the top left corner of the rectangle
	 * @param width
	 *            The width of the rectangle
	 * @param height
	 *            The height of the rectangle
	 * @param cornerRadius
	 *            The radius of the rounded edges on the corners
	 */
	public void fillRoundRect(float x, float y, float width, float height,
			int cornerRadius) {
		fillRoundRect(x, y, width, height, cornerRadius, DEFAULT_SEGMENTS);
	}

	/**
	 * Fill a rounded rectangle
	 * 
	 * @param x
	 *            The x coordinate of the top left corner of the rectangle
	 * @param y
	 *            The y coordinate of the top left corner of the rectangle
	 * @param width
	 *            The width of the rectangle
	 * @param height
	 *            The height of the rectangle
	 * @param cornerRadius
	 *            The radius of the rounded edges on the corners
	 * @param segs
	 *            The number of segments to make the corners out of
	 */
	public void fillRoundRect(float x, float y, float width, float height,
			int cornerRadius, int segs) {
		if (cornerRadius < 0)
			throw new IllegalArgumentException("corner radius must be > 0");
		if (cornerRadius == 0) {
			fillRect(x, y, width, height);
			return;
		}

		int mr = (int) Math.min(width, height) / 2;
		// make sure that w & h are larger than 2*cornerRadius
		if (cornerRadius > mr) {
			cornerRadius = mr;
		}

		float d = cornerRadius * 2;

		fillRect(x + cornerRadius, y, width - d, cornerRadius);
		fillRect(x, y + cornerRadius, cornerRadius, height - d);
		fillRect(x + width - cornerRadius, y + cornerRadius, cornerRadius,
				height - d);
		fillRect(x + cornerRadius, y + height - cornerRadius, width - d,
				cornerRadius);
		fillRect(x + cornerRadius, y + cornerRadius, width - d, height - d);

		// bottom right - 0, 90
		fillArc(x + width - d, y + height - d, d, d, segs, 0, 90);
		// bottom left - 90, 180
		fillArc(x, y + height - d, d, d, segs, 90, 180);
		// top right - 270, 360
		fillArc(x + width - d, y, d, d, segs, 270, 360);
		// top left - 180, 270
		fillArc(x, y, d, d, segs, 180, 270);
	}

	/**
	 * Set the with of the line to be used when drawing line based primitives
	 * 
	 * @param width
	 *            The width of the line to be used when drawing line based
	 *            primitives
	 */
	public void setLineWidth(float width) {
		predraw();
		this.lineWidth = width;
		LSR.setWidth(width);
		GL.glPointSize(width);
		postdraw();
	}

	/**
	 * Get the width of lines being drawn in this context
	 * 
	 * @return The width of lines being draw in this context
	 */
	public float getLineWidth() {
		return lineWidth;
	}

	/**
	 * Reset the line width in use to the default for this graphics context
	 */
	public void resetLineWidth() {
		predraw();
		
		Renderer.getLineStripRenderer().setWidth(1.0f);
		GL.glLineWidth(1.0f);
		GL.glPointSize(1.0f);
		
		postdraw();
	}

	/**
	 * Indicate if we should antialias as we draw primitives
	 * 
	 * @param anti
	 *            True if we should antialias
	 */
	public void setAntiAlias(boolean anti) {
		predraw();
		antialias = anti;
		LSR.setAntiAlias(anti);
		if (anti) {
			GL.glEnable(SGL.GL_POLYGON_SMOOTH);
		} else {
			GL.glDisable(SGL.GL_POLYGON_SMOOTH);
		}
		postdraw();
	}

	/**
	 * True if antialiasing has been turned on for this graphics context
	 * 
	 * @return True if antialiasing has been turned on for this graphics context
	 */
	public boolean isAntiAlias() {
		return antialias;
	}

	/**
	 * Draw a string to the screen using the current font
	 * 
	 * @param str
	 *            The string to draw
	 * @param x
	 *            The x coordinate to draw the string at
	 * @param y
	 *            The y coordinate to draw the string at
	 */
	public void drawString(String str, float x, float y) {
		predraw();
		font.drawString(x, y, str, currentColor);
		postdraw();
	}

	/**
	 * Draw an image to the screen
	 * 
	 * @param image
	 *            The image to draw to the screen
	 * @param x
	 *            The x location at which to draw the image
	 * @param y
	 *            The y location at which to draw the image
	 * @param col
	 *            The color to apply to the image as a filter
	 */
	public void drawImage(Image image, float x, float y, Color col) {
		predraw();
		image.draw(x, y, col);
		currentColor.bind();
		postdraw();
	}

	/**
	 * Draw an animation to this graphics context
	 * 
	 * @param anim
	 *            The animation to be drawn
	 * @param x
	 *            The x position to draw the animation at
	 * @param y
	 *            The y position to draw the animation at
	 */
	public void drawAnimation(Animation anim, float x, float y) {
		drawAnimation(anim, x, y, Color.white);
	}

	/**
	 * Draw an animation to this graphics context
	 * 
	 * @param anim
	 *            The animation to be drawn
	 * @param x
	 *            The x position to draw the animation at
	 * @param y
	 *            The y position to draw the animation at
	 * @param col
	 *            The color to apply to the animation as a filter
	 */
	public void drawAnimation(Animation anim, float x, float y, Color col) {
		predraw();
		anim.draw(x, y, col);
		currentColor.bind();
		postdraw();
	}

	/**
	 * Draw an image to the screen
	 * 
	 * @param image
	 *            The image to draw to the screen
	 * @param x
	 *            The x location at which to draw the image
	 * @param y
	 *            The y location at which to draw the image
	 */
	public void drawImage(Image image, float x, float y) {
		drawImage(image, x, y, Color.white);
	}

	/**
	 * Draw a section of an image at a particular location and scale on the
	 * screen
	 * 
	 * @param image
	 *            The image to draw a section of
	 * @param x
	 *            The x position to draw the image
	 * @param y
	 *            The y position to draw the image
	 * @param x2
	 *            The x position of the bottom right corner of the drawn image
	 * @param y2
	 *            The y position of the bottom right corner of the drawn image
	 * @param srcx
	 *            The x position of the rectangle to draw from this image (i.e.
	 *            relative to the image)
	 * @param srcy
	 *            The y position of the rectangle to draw from this image (i.e.
	 *            relative to the image)
	 * @param srcx2
	 *            The x position of the bottom right cornder of rectangle to
	 *            draw from this image (i.e. relative to the image)
	 * @param srcy2
	 *            The t position of the bottom right cornder of rectangle to
	 *            draw from this image (i.e. relative to the image)
	 */
	public void drawImage(Image image, float x, float y, float x2, float y2,
			float srcx, float srcy, float srcx2, float srcy2) {
		predraw();
		image.draw(x, y, x2, y2, srcx, srcy, srcx2, srcy2);
		currentColor.bind();
		postdraw();
	}

	/**
	 * Draw a section of an image at a particular location and scale on the
	 * screen
	 * 
	 * @param image
	 *            The image to draw a section of
	 * @param x
	 *            The x position to draw the image
	 * @param y
	 *            The y position to draw the image
	 * @param srcx
	 *            The x position of the rectangle to draw from this image (i.e.
	 *            relative to the image)
	 * @param srcy
	 *            The y position of the rectangle to draw from this image (i.e.
	 *            relative to the image)
	 * @param srcx2
	 *            The x position of the bottom right cornder of rectangle to
	 *            draw from this image (i.e. relative to the image)
	 * @param srcy2
	 *            The t position of the bottom right cornder of rectangle to
	 *            draw from this image (i.e. relative to the image)
	 */
	public void drawImage(Image image, float x, float y, float srcx,
			float srcy, float srcx2, float srcy2) {
		drawImage(image, x, y, x + image.getWidth(), y + image.getHeight(),
				srcx, srcy, srcx2, srcy2);
	}

	/**
	 * Copy an area of the rendered screen into an image. The width and height
	 * of the area are assumed to match that of the image
	 * 
	 * @param target
	 *            The target image
	 * @param x
	 *            The x position to copy from
	 * @param y
	 *            The y position to copy from
	 */
	public void copyArea(Image target, int x, int y) {
		int format = target.getTexture().hasAlpha() ? SGL.GL_RGBA : SGL.GL_RGB;
		target.bind();
		GL.glCopyTexImage2D(SGL.GL_TEXTURE_2D, 0, format, x, screenHeight
				- (y + target.getHeight()), target.getTexture()
				.getTextureWidth(), target.getTexture().getTextureHeight(), 0);
		target.ensureInverted();
	}

	/**
	 * Translate an unsigned int into a signed integer
	 * 
	 * @param b
	 *            The byte to convert
	 * @return The integer value represented by the byte
	 */
	private int translate(byte b) {
		if (b < 0) {
			return 256 + b;
		}

		return b;
	}

	/**
	 * Get the colour of a single pixel in this graphics context
	 * 
	 * @param x
	 *            The x coordinate of the pixel to read
	 * @param y
	 *            The y coordinate of the pixel to read
	 * @return The colour of the pixel at the specified location
	 */
	public Color getPixel(int x, int y) {
		predraw();
		GL.glReadPixels(x, screenHeight - y, 1, 1, SGL.GL_RGBA,
				SGL.GL_UNSIGNED_BYTE, readBuffer);
		postdraw();

		return new Color(translate(readBuffer.get(0)), translate(readBuffer
				.get(1)), translate(readBuffer.get(2)), translate(readBuffer
				.get(3)));
	}

	/**
	 * Get an ara of pixels as RGBA values into a buffer
	 * 
	 * @param x The x position in the context to grab from
	 * @param y The y position in the context to grab from
	 * @param width The width of the area to grab from 
	 * @param height The hiehgt of the area to grab from
	 * @param target The target buffer to grab into
	 */
	public void getArea(int x, int y, int width, int height, ByteBuffer target)
	{
		if (target.capacity() < width * height * 4) 
		{
			throw new IllegalArgumentException("Byte buffer provided to get area is not big enough");
		}
		
		predraw();	
		GL.glReadPixels(x, screenHeight - y - height, width, height, SGL.GL_RGBA,
				SGL.GL_UNSIGNED_BYTE, target);
		postdraw();
	}
	
	/**
	 * Draw a section of an image at a particular location and scale on the
	 * screen
	 * 
	 * @param image
	 *            The image to draw a section of
	 * @param x
	 *            The x position to draw the image
	 * @param y
	 *            The y position to draw the image
	 * @param x2
	 *            The x position of the bottom right corner of the drawn image
	 * @param y2
	 *            The y position of the bottom right corner of the drawn image
	 * @param srcx
	 *            The x position of the rectangle to draw from this image (i.e.
	 *            relative to the image)
	 * @param srcy
	 *            The y position of the rectangle to draw from this image (i.e.
	 *            relative to the image)
	 * @param srcx2
	 *            The x position of the bottom right cornder of rectangle to
	 *            draw from this image (i.e. relative to the image)
	 * @param srcy2
	 *            The t position of the bottom right cornder of rectangle to
	 *            draw from this image (i.e. relative to the image)
	 * @param col
	 *            The color to apply to the image as a filter
	 */
	public void drawImage(Image image, float x, float y, float x2, float y2,
			float srcx, float srcy, float srcx2, float srcy2, Color col) {
		predraw();
		image.draw(x, y, x2, y2, srcx, srcy, srcx2, srcy2, col);
		currentColor.bind();
		postdraw();
	}

	/**
	 * Draw a section of an image at a particular location and scale on the
	 * screen
	 * 
	 * @param image
	 *            The image to draw a section of
	 * @param x
	 *            The x position to draw the image
	 * @param y
	 *            The y position to draw the image
	 * @param srcx
	 *            The x position of the rectangle to draw from this image (i.e.
	 *            relative to the image)
	 * @param srcy
	 *            The y position of the rectangle to draw from this image (i.e.
	 *            relative to the image)
	 * @param srcx2
	 *            The x position of the bottom right cornder of rectangle to
	 *            draw from this image (i.e. relative to the image)
	 * @param srcy2
	 *            The t position of the bottom right cornder of rectangle to
	 *            draw from this image (i.e. relative to the image)
	 * @param col
	 *            The color to apply to the image as a filter
	 */
	public void drawImage(Image image, float x, float y, float srcx,
			float srcy, float srcx2, float srcy2, Color col) {
		drawImage(image, x, y, x + image.getWidth(), y + image.getHeight(),
				srcx, srcy, srcx2, srcy2, col);
	}

	/**
	 * Draw a line with a gradient between the two points.
	 * 
	 * @param x1
	 *            The starting x position to draw the line
	 * @param y1
	 *            The starting y position to draw the line
	 * @param red1
	 *            The starting position's shade of red
	 * @param green1
	 *            The starting position's shade of green
	 * @param blue1
	 *            The starting position's shade of blue
	 * @param alpha1
	 *            The starting position's alpha value
	 * @param x2
	 *            The ending x position to draw the line
	 * @param y2
	 *            The ending y position to draw the line
	 * @param red2
	 *            The ending position's shade of red
	 * @param green2
	 *            The ending position's shade of green
	 * @param blue2
	 *            The ending position's shade of blue
	 * @param alpha2
	 *            The ending position's alpha value
	 */
	public void drawGradientLine(float x1, float y1, float red1, float green1,
									float blue1, float alpha1, float x2, float y2, float red2,
									float green2, float blue2, float alpha2) {
		predraw();

		TextureImpl.bindNone();

		GL.glBegin(SGL.GL_LINES);

		GL.glColor4f(red1, green1, blue1, alpha1);
		GL.glVertex2f(x1, y1);

		GL.glColor4f(red2, green2, blue2, alpha2);
		GL.glVertex2f(x2, y2);

		GL.glEnd();

		postdraw();
	}

	/**
	 * Draw a line with a gradient between the two points.
	 * 
	 * @param x1
	 *            The starting x position to draw the line
	 * @param y1
	 *            The starting y position to draw the line
	 * @param Color1
	 *            The starting position's color
	 * @param x2
	 *            The ending x position to draw the line
	 * @param y2
	 *            The ending y position to draw the line
	 * @param Color2
	 *            The ending position's color
	 */
	public void drawGradientLine(float x1, float y1, Color Color1, float x2,
								 float y2, Color Color2) {
		predraw();

		TextureImpl.bindNone();

		GL.glBegin(SGL.GL_LINES);

		Color1.bind();
		GL.glVertex2f(x1, y1);

		Color2.bind();
		GL.glVertex2f(x2, y2);

		GL.glEnd();

		postdraw();
	}
	
	/**
	 * Push the current state of the transform from this graphics contexts
	 * onto the underlying graphics stack's transform stack. An associated 
	 * popTransform() must be performed to restore the state before the end
	 * of the rendering loop.
	 */
	public void pushTransform() {
		predraw();
		
		FloatBuffer buffer;
		if (stackIndex >= stack.size()) {
			buffer = BufferUtils.createFloatBuffer(18);
			stack.add(buffer);
		} else {
			buffer = (FloatBuffer) stack.get(stackIndex);
		}
		
		GL.glGetFloat(SGL.GL_MODELVIEW_MATRIX, buffer);
		buffer.put(16, sx);
		buffer.put(17, sy);
		stackIndex++;
		
		postdraw();
	}
	
	/**
	 * Pop a previously pushed transform from the stack to the current. This should
	 * only be called if a transform has been previously pushed.
	 */
	public void popTransform() {
		if (stackIndex == 0) {
			throw new RuntimeException("Attempt to pop a transform that hasn't be pushed");
		}
		
		predraw();
		
		stackIndex--;
		FloatBuffer oldBuffer = (FloatBuffer) stack.get(stackIndex);
		GL.glLoadMatrix(oldBuffer);
		sx = oldBuffer.get(16);
		sy = oldBuffer.get(17);
		
		postdraw();
	}
	
	/**
	 * Dispose this graphics context, this will release any underlying resourses. However
	 * this will also invalidate it's use
	 */
	public void destroy() {
		
	}
}
