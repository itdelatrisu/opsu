package org.newdawn.slick;

import java.io.IOException;
import java.io.InputStream;

import org.newdawn.slick.opengl.ImageData;
import org.newdawn.slick.opengl.InternalTextureLoader;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureImpl;
import org.newdawn.slick.opengl.pbuffer.GraphicsFactory;
import org.newdawn.slick.opengl.renderer.Renderer;
import org.newdawn.slick.opengl.renderer.SGL;
import org.newdawn.slick.util.Log;

/**
 * An image loaded from a file and renderable to the canvas
 *
 * @author kevin
 */
public class Image implements Renderable {
	/** The top left corner identifier */
	public static final int TOP_LEFT = 0;
	/** The top right corner identifier */
	public static final int TOP_RIGHT = 1;
	/** The bottom right corner identifier */
	public static final int BOTTOM_RIGHT = 2;
	/** The bottom left corner identifier */
	public static final int BOTTOM_LEFT = 3;
	
	/** The renderer to use for all GL operations */
	protected static SGL GL = Renderer.get();
	
	/** The sprite sheet currently in use */
	protected static Image inUse;
	/** Use Linear Filtering */
	public static final int FILTER_LINEAR = 1;
	/** Use Nearest Filtering */
	public static final int FILTER_NEAREST = 2;
	
	/** The OpenGL texture for this image */
	protected Texture texture;
	/** The width of the image */
	protected int width;
	/** The height of the image */
	protected int height;
	/** The texture coordinate width to use to find our image */
	protected float textureWidth;
	/** The texture coordinate height to use to find our image */
	protected float textureHeight;
	/** The x texture offset to use to find our image */
	protected float textureOffsetX;
	/** The y texture offset to use to find our image */
	protected float textureOffsetY;
    /** Angle to rotate the image to. */
	protected float angle;
	/** The alpha to draw the image at */
	protected float alpha = 1.0f;
	/** The name given for the image */
	protected String ref;
	/** True if this image's state has been initialised */
	protected boolean inited = false;
	/** A pixelData holding the pixel data if it's been read for this texture */
	protected byte[] pixelData;
	/** True if the image has been destroyed */
	protected boolean destroyed;

	/** The x coordinate of the centre of rotation */
    protected float centerX; 
    /** The y coordinate of the centre of rotation */
    protected float centerY; 
    
    /** A meaningful name provided by the user of the image to tag it */
    protected String name;
    
    /** The colours for each of the corners */
    protected Color[] corners;
    /** The OpenGL max filter */
    private int filter = SGL.GL_LINEAR;
    
    /** True if the image should be flipped vertically */
    private boolean flipped;
    /** The transparent colour set if any */
    private Color transparent;
    
	/**
	 * Create a texture as a copy of another
	 * 
	 * @param other The other texture to copy
	 */
	protected Image(Image other) {
		this.width = other.getWidth();
		this.height = other.getHeight();
		this.texture = other.texture;
		this.textureWidth = other.textureWidth;
		this.textureHeight = other.textureHeight;
		this.ref = other.ref;
		this.textureOffsetX = other.textureOffsetX;
		this.textureOffsetY = other.textureOffsetY;
	
		centerX = width / 2;
		centerY = height / 2;
		inited = true;
	}
	
	/**
	 * Cloning constructor - only used internally.
	 */
	protected Image() {
	}
	
    /**
	 * Creates an image using the specified texture
	 * 
	 * @param texture
	 *            The texture to use
	 */
	public Image(Texture texture) {
		this.texture = texture;
		ref = texture.toString();
		clampTexture();
	}
	    
	/**
	 * Create an image based on a file at the specified location
	 * 
	 * @param ref
	 *            The location of the image file to load
	 * @throws SlickException
	 *             Indicates a failure to load the image
	 */
	public Image(String ref) throws SlickException  {
		this(ref, false);
	}

	/**
	 * Create an image based on a file at the specified location
	 * 
	 * @param ref The location of the image file to load
	 * @param trans The color to be treated as transparent
	 * @throws SlickException Indicates a failure to load the image
	 */
	public Image(String ref, Color trans) throws SlickException  {
		this(ref, false, FILTER_LINEAR, trans);
	}
	
	/**
	 * Create an image based on a file at the specified location
	 * 
	 * @param ref The location of the image file to load
	 * @param flipped True if the image should be flipped on the y-axis on load
	 * @throws SlickException Indicates a failure to load the image
	 */
	public Image(String ref, boolean flipped) throws SlickException {
		this(ref, flipped, FILTER_LINEAR);
	}

	/**
	 * Create an image based on a file at the specified location
	 * 
	 * @param ref The location of the image file to load
	 * @param flipped True if the image should be flipped on the y-axis on load
	 * @param filter The filtering method to use when scaling this image
	 * @throws SlickException Indicates a failure to load the image
	 */
	public Image(String ref, boolean flipped, int filter) throws SlickException {
		this(ref, flipped, filter, null);
	}
	
	/**
	 * Create an image based on a file at the specified location
	 * 
	 * @param ref The location of the image file to load
	 * @param flipped True if the image should be flipped on the y-axis on load
	 * @param f The filtering method to use when scaling this image
	 * @param transparent The color to treat as transparent
	 * @throws SlickException Indicates a failure to load the image
	 */
	public Image(String ref, boolean flipped, int f, Color transparent) throws SlickException {
		this.filter = f == FILTER_LINEAR ? SGL.GL_LINEAR : SGL.GL_NEAREST;
		this.transparent = transparent;
		this.flipped = flipped;
		
		try {
			this.ref = ref;
			int[] trans = null;
			if (transparent != null) {
				trans = new int[3];
				trans[0] = (int) (transparent.r * 255);
				trans[1] = (int) (transparent.g * 255);
				trans[2] = (int) (transparent.b * 255);
			}
			texture = InternalTextureLoader.get().getTexture(ref, flipped, filter, trans);
		} catch (IOException e) {
			Log.error(e);
			throw new SlickException("Failed to load image from: "+ref, e);
		}
	}
	
	/**
	 * Set the image filtering to be used. Note that this will also affect any
	 * image that was derived from this one (i.e. sub-images etc)
	 * 
	 * @param f The filtering mode to use
	 */
	public void setFilter(int f) {
		this.filter = f == FILTER_LINEAR ? SGL.GL_LINEAR : SGL.GL_NEAREST;

		texture.bind();
		GL.glTexParameteri(SGL.GL_TEXTURE_2D, SGL.GL_TEXTURE_MIN_FILTER, filter); 
        GL.glTexParameteri(SGL.GL_TEXTURE_2D, SGL.GL_TEXTURE_MAG_FILTER, filter); 
	}
	
	/**
	 * Create an empty image
	 * 
	 * @param width The width of the image
	 * @param height The height of the image
	 * @throws SlickException Indicates a failure to create the underlying resource
	 */
	public Image(int width, int height) throws SlickException {
		this(width, height, FILTER_NEAREST);
	}
	
	/**
	 * Create an empty image
	 * 
	 * @param width The width of the image
	 * @param height The height of the image
	 * @param f The filter to apply to scaling the new image
	 * @throws SlickException Indicates a failure to create the underlying resource
	 */
	public Image(int width, int height, int f) throws SlickException {
		ref = super.toString();
		this.filter = f == FILTER_LINEAR ? SGL.GL_LINEAR : SGL.GL_NEAREST;
		
		try {
			texture = InternalTextureLoader.get().createTexture(width, height, this.filter);
		} catch (IOException e) {
			Log.error(e);
			throw new SlickException("Failed to create empty image "+width+"x"+height);
		}
		
		init();
	}
	
	/**
	 * Create an image based on a file at the specified location
	 * 
	 * @param in The input stream to read the image from
	 * @param ref The name that should be assigned to the image
	 * @param flipped True if the image should be flipped on the y-axis  on load
	 * @throws SlickException Indicates a failure to load the image
	 */
	public Image(InputStream in, String ref, boolean flipped) throws SlickException {
		this(in, ref, flipped, FILTER_LINEAR);
	}

	/**
	 * Create an image based on a file at the specified location
	 * 
	 * @param in The input stream to read the image from
	 * @param ref The name that should be assigned to the image
	 * @param flipped True if the image should be flipped on the y-axis on load
	 * @param filter The filter to use when scaling this image
	 * @throws SlickException Indicates a failure to load the image
	 */
	public Image(InputStream in, String ref, boolean flipped,int filter) throws SlickException {
		load(in, ref, flipped, filter, null);
	}
	
	/**
	 * Create an image from a pixelData of pixels
	 * 
	 * @param buffer The pixelData to use to create the image
	 */
	Image(ImageBuffer buffer) {
		this(buffer, FILTER_LINEAR);
        TextureImpl.bindNone();
	}
	
	/**
	 * Create an image from a pixelData of pixels
	 * 
	 * @param buffer The pixelData to use to create the image
	 * @param filter The filter to use when scaling this image
	 */
	Image(ImageBuffer buffer, int filter) {
		this((ImageData) buffer, filter);
        TextureImpl.bindNone();
	}

	/**
	 * Create an image from a image data source
	 * 
	 * @param data The pixelData to use to create the image
	 */
	public Image(ImageData data) {
		this(data, FILTER_LINEAR);
	}
	
	/**
	 * Create an image from a image data source. Note that this method uses 
	 * 
	 * @param data The pixelData to use to create the image
	 * @param f The filter to use when scaling this image
	 */
	public Image(ImageData data, int f) {
		try {
			this.filter = f == FILTER_LINEAR ? SGL.GL_LINEAR : SGL.GL_NEAREST;
			texture = InternalTextureLoader.get().getTexture(data, this.filter);
			ref = texture.toString();
		} catch (IOException e) {
			Log.error(e);
		}
	}

	/** 
	 * Get the OpenGL image filter in use
	 * 
	 * @return The filter for magnification
	 */
	public int getFilter() {
		return filter;
	}
	
	/** 
	 * Get the reference to the resource this image was loaded from, if any. Note that
	 * this can be null in the cases where an image was programatically generated.
	 * 
	 * @return The reference to the resource the reference was loaded from
	 */
	public String getResourceReference() {
		return ref;
	}
	
	/**
	 * Set the filter to apply when drawing this image
	 * 
	 * @param r The red component of the filter colour
	 * @param g The green component of the filter colour
	 * @param b The blue component of the filter colour
	 * @param a The alpha component of the filter colour
	 */
	public void setImageColor(float r, float g, float b, float a) {
		setColor(TOP_LEFT, r, g, b, a);
		setColor(TOP_RIGHT, r, g, b, a);
		setColor(BOTTOM_LEFT, r, g, b, a);
		setColor(BOTTOM_RIGHT, r, g, b, a);
	}
	
	/**
	 * Set the filter to apply when drawing this image
	 * 
	 * @param r The red component of the filter colour
	 * @param g The green component of the filter colour
	 * @param b The blue component of the filter colour
	 */
	public void setImageColor(float r, float g, float b) {
		setColor(TOP_LEFT, r, g, b);
		setColor(TOP_RIGHT, r, g, b);
		setColor(BOTTOM_LEFT, r, g, b);
		setColor(BOTTOM_RIGHT, r, g, b);
	}
	
	/** 
	 * Set the color of the given corner when this image is rendered. This is 
	 * useful lots of visual effect but especially light maps
	 * 
	 * @param corner The corner identifier for the corner to be set
	 * @param r The red component value to set (between 0 and 1)
	 * @param g The green component value to set (between 0 and 1)
	 * @param b The blue component value to set (between 0 and 1)
	 * @param a The alpha component value to set (between 0 and 1)
	 */
	public void setColor(int corner, float r, float g, float b, float a) {
		if (corners == null) {
			corners = new Color[] {new Color(1,1,1,1f),new Color(1,1,1,1f), new Color(1,1,1,1f), new Color(1,1,1,1f)};
		}
		
		corners[corner].r = r;
		corners[corner].g = g;
		corners[corner].b = b;
		corners[corner].a = a;
	}

	/** 
	 * Set the color of the given corner when this image is rendered. This is 
	 * useful lots of visual effect but especially light maps
	 * 
	 * @param corner The corner identifier for the corner to be set
	 * @param r The red component value to set (between 0 and 1)
	 * @param g The green component value to set (between 0 and 1)
	 * @param b The blue component value to set (between 0 and 1)
	 */
	public void setColor(int corner, float r, float g, float b) {
		if (corners == null) {
			corners = new Color[] {new Color(1,1,1,1f),new Color(1,1,1,1f), new Color(1,1,1,1f), new Color(1,1,1,1f)};
		}
		
		corners[corner].r = r;
		corners[corner].g = g;
		corners[corner].b = b;
	}
	
	/**
	 * Clamp the loaded texture to it's edges
	 */
	public void clampTexture() {
        if (GL.canTextureMirrorClamp()) {
        	GL.glTexParameteri(SGL.GL_TEXTURE_2D, SGL.GL_TEXTURE_WRAP_S, SGL.GL_MIRROR_CLAMP_TO_EDGE_EXT);
        	GL.glTexParameteri(SGL.GL_TEXTURE_2D, SGL.GL_TEXTURE_WRAP_T, SGL.GL_MIRROR_CLAMP_TO_EDGE_EXT);
        } else {
        	GL.glTexParameteri(SGL.GL_TEXTURE_2D, SGL.GL_TEXTURE_WRAP_S, SGL.GL_CLAMP);
        	GL.glTexParameteri(SGL.GL_TEXTURE_2D, SGL.GL_TEXTURE_WRAP_T, SGL.GL_CLAMP);
        }
	}
	
	/**
	 * Give this image a meaningful tagging name. Can be used as user data/identifier
	 * for the image.
	 * 
	 * @param name The name to assign the image
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Return a meaningful tagging name that has been assigned to this image. 
	 * 
	 * @return A name or null if the name hasn't been set
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Get a graphics context that can be used to draw to this image
	 * 
	 * @return The graphics context used to render to this image
	 * @throws SlickException Indicates a failure to create a graphics context
	 */
	public Graphics getGraphics() throws SlickException {
		return GraphicsFactory.getGraphicsForImage(this);
	}
	
	/**
	 * Load the image
	 * 
	 * @param in The input stream to read the image from
	 * @param ref The name that should be assigned to the image
	 * @param flipped True if the image should be flipped on the y-axis  on load
	 * @param f The filter to use when scaling this image
	 * @param transparent The color to treat as transparent
	 * @throws SlickException Indicates a failure to load the image
	 */
	private void load(InputStream in, String ref, boolean flipped, int f, Color transparent) throws SlickException {
		this.filter = f == FILTER_LINEAR ? SGL.GL_LINEAR : SGL.GL_NEAREST;
		
		try {
			this.ref = ref;
			int[] trans = null;
			if (transparent != null) {
				trans = new int[3];
				trans[0] = (int) (transparent.r * 255);
				trans[1] = (int) (transparent.g * 255);
				trans[2] = (int) (transparent.b * 255);
			}
			texture = InternalTextureLoader.get().getTexture(in, ref, flipped, filter, trans);
		} catch (IOException e) {
			Log.error(e);
			throw new SlickException("Failed to load image from: "+ref, e);
		}
	}

	/**
	 * Bind to the texture of this image
	 */
	public void bind() {
		texture.bind();
	}

	/**
	 * Reinitialise internal data
	 */
	protected void reinit() {
		inited = false;
		init();
	}
	
	/**
	 * Initialise internal data
	 */
	protected final void init() {
		if (inited) {
			return;
		}
		
		inited = true;
		if (texture != null) {
			width = texture.getImageWidth();
			height = texture.getImageHeight();
			textureOffsetX = 0;
			textureOffsetY = 0;
			textureWidth = texture.getWidth();
			textureHeight = texture.getHeight();
		}
		
		initImpl();
	
		centerX = width / 2;
		centerY = height / 2;
	}

	/**
	 * Hook for subclasses to perform initialisation
	 */
	protected void initImpl() {
		
	}
	
	/**
	 * Draw this image at the current location
	 */
	public void draw() {
		draw(0,0);
	}
	
	/**
	 * Draw the image based on it's center 
	 * 
	 * @param x The x coordinate to place the image's center at
	 * @param y The y coordinate to place the image's center at
	 */
	public void drawCentered(float x, float y) {
		draw(x-(getWidth()/2),y-(getHeight()/2));
	}
	
	/**
	 * Draw this image at the specified location
	 * 
	 * @param x The x location to draw the image at
	 * @param y The y location to draw the image at
	 */
	public void draw(float x, float y) {
		init();
		draw(x,y,width,height);
	}
	
	/**
	 * Draw this image at the specified location
	 * 
	 * @param x The x location to draw the image at
	 * @param y The y location to draw the image at
	 * @param filter The color to filter with when drawing
	 */
	public void draw(float x, float y, Color filter) {
		init();
		draw(x,y,width,height, filter);
	}

	/**
	 * Draw this image as part of a collection of images
	 * 
	 * @param x The x location to draw the image at
	 * @param y The y location to draw the image at
	 * @param width The width to render the image at
	 * @param height The height to render the image at
	 */
	public void drawEmbedded(float x,float y,float width,float height) {
		init();
		
		if (corners == null) {
		    GL.glTexCoord2f(textureOffsetX, textureOffsetY);
			GL.glVertex3f(x, y, 0);
			GL.glTexCoord2f(textureOffsetX, textureOffsetY + textureHeight);
			GL.glVertex3f(x, y + height, 0);
			GL.glTexCoord2f(textureOffsetX + textureWidth, textureOffsetY
					+ textureHeight);
			GL.glVertex3f(x + width, y + height, 0);
			GL.glTexCoord2f(textureOffsetX + textureWidth, textureOffsetY);
			GL.glVertex3f(x + width, y, 0);
		} else {
			corners[TOP_LEFT].bind();
		    GL.glTexCoord2f(textureOffsetX, textureOffsetY);
			GL.glVertex3f(x, y, 0);
			corners[BOTTOM_LEFT].bind();
			GL.glTexCoord2f(textureOffsetX, textureOffsetY + textureHeight);
			GL.glVertex3f(x, y + height, 0);
			corners[BOTTOM_RIGHT].bind();
			GL.glTexCoord2f(textureOffsetX + textureWidth, textureOffsetY
					+ textureHeight);
			GL.glVertex3f(x + width, y + height, 0);
			corners[TOP_RIGHT].bind();
			GL.glTexCoord2f(textureOffsetX + textureWidth, textureOffsetY);
			GL.glVertex3f(x + width, y, 0);
		}
	}

	/**
	 * Get the x offset in texels into the source texture
	 * 
	 * @return The x offset 
	 */
	public float getTextureOffsetX() {
		init();
		
		return textureOffsetX;
	}

	/**
	 * Get the y offset in texels into the source texture
	 * 
	 * @return The y offset 
	 */
	public float getTextureOffsetY() {
		init();
		
		return textureOffsetY;
	}

	/**
	 * Get the width in texels into the source texture
	 * 
	 * @return The width
	 */
	public float getTextureWidth() {
		init();
		
		return textureWidth;
	}

	/**
	 * Get the height in texels into the source texture
	 * 
	 * @return The height
	 */
	public float getTextureHeight() {
		init();
		
		return textureHeight;
	}
	
	/**
	 * Draw the image with a given scale
	 * 
	 * @param x The x position to draw the image at
	 * @param y The y position to draw the image at
	 * @param scale The scaling to apply
	 */
	public void draw(float x,float y,float scale) {
		init();
		draw(x,y,width*scale,height*scale,Color.white);
	}
	
	/**
	 * Draw the image with a given scale
	 * 
	 * @param x The x position to draw the image at
	 * @param y The y position to draw the image at
	 * @param scale The scaling to apply
	 * @param filter The colour filter to adapt the image with
	 */
	public void draw(float x,float y,float scale,Color filter) {
		init();
		draw(x,y,width*scale,height*scale,filter);
	}
	
	/**
	 * Draw this image at a specified location and size
	 * 
	 * @param x
	 *            The x location to draw the image at
	 * @param y
	 *            The y location to draw the image at
	 * @param width
	 *            The width to render the image at
	 * @param height
	 *            The height to render the image at
	 */
	public void draw(float x,float y,float width,float height) {
		init();
		draw(x,y,width,height,Color.white);
	}

	/**
	 * Draw this image at a specified location and size
	 * 
	 * @param x The x location to draw the image at
	 * @param y The y location to draw the image at
	 * @param hshear The amount to shear the bottom points by horizontally
	 * @param vshear The amount to shear the right points by vertically
	 */
    public void drawSheared(float x,float y, float hshear, float vshear) { 
    	this.drawSheared(x, y, hshear, vshear, Color.white);
    }
	/**
	 * Draw this image at a specified location and size
	 * 
	 * @param x The x location to draw the image at
	 * @param y The y location to draw the image at
	 * @param hshear The amount to shear the bottom points by horizontally
	 * @param vshear The amount to shear the right points by vertically
	 * @param filter The colour filter to apply
	 */
    public void drawSheared(float x,float y, float hshear, float vshear, Color filter) { 
    	if (alpha != 1) {
    		if (filter == null) {
    			filter = Color.white;
    		}
    		
    		filter = new Color(filter);
    		filter.a *= alpha;
    	}
        if (filter != null) { 
            filter.bind(); 
        } 
        
        texture.bind(); 
        
        GL.glTranslatef(x, y, 0);
        if (angle != 0) {
	        GL.glTranslatef(centerX, centerY, 0.0f); 
	        GL.glRotatef(angle, 0.0f, 0.0f, 1.0f); 
	        GL.glTranslatef(-centerX, -centerY, 0.0f); 
        }
        
        GL.glBegin(SGL.GL_QUADS); 
        	init();
		
		    GL.glTexCoord2f(textureOffsetX, textureOffsetY);
			GL.glVertex3f(0, 0, 0);
			GL.glTexCoord2f(textureOffsetX, textureOffsetY + textureHeight);
			GL.glVertex3f(hshear, height, 0);
			GL.glTexCoord2f(textureOffsetX + textureWidth, textureOffsetY
					+ textureHeight);
			GL.glVertex3f(width + hshear, height + vshear, 0);
			GL.glTexCoord2f(textureOffsetX + textureWidth, textureOffsetY);
			GL.glVertex3f(width, vshear, 0);
        GL.glEnd(); 
        
        if (angle != 0) {
	        GL.glTranslatef(centerX, centerY, 0.0f); 
	        GL.glRotatef(-angle, 0.0f, 0.0f, 1.0f); 
	        GL.glTranslatef(-centerX, -centerY, 0.0f); 
        }
        GL.glTranslatef(-x, -y, 0);
    } 
    
	/**
	 * Draw this image at a specified location and size
	 * 
	 * @param x The x location to draw the image at
	 * @param y The y location to draw the image at
	 * @param width The width to render the image at
	 * @param height The height to render the image at
	 * @param filter The color to filter with while drawing
	 */
    public void draw(float x,float y,float width,float height,Color filter) { 
    	if (alpha != 1) {
    		if (filter == null) {
    			filter = Color.white;
    		}
    		
    		filter = new Color(filter);
    		filter.a *= alpha;
    	}
        if (filter != null) { 
            filter.bind(); 
        } 
       
        texture.bind(); 
        
        GL.glTranslatef(x, y, 0);
        if (angle != 0) {
	        GL.glTranslatef(centerX, centerY, 0.0f); 
	        GL.glRotatef(angle, 0.0f, 0.0f, 1.0f); 
	        GL.glTranslatef(-centerX, -centerY, 0.0f); 
        }
        
        GL.glBegin(SGL.GL_QUADS); 
            drawEmbedded(0,0,width,height); 
        GL.glEnd(); 
        
        if (angle != 0) {
	        GL.glTranslatef(centerX, centerY, 0.0f); 
	        GL.glRotatef(-angle, 0.0f, 0.0f, 1.0f); 
	        GL.glTranslatef(-centerX, -centerY, 0.0f); 
        }
        GL.glTranslatef(-x, -y, 0);
    } 

	/**
	 * Draw this image at a specified location and size as a silohette
	 * 
	 * @param x The x location to draw the image at
	 * @param y The y location to draw the image at
	 * @param width The width to render the image at
	 * @param height The height to render the image at
	 */
	public void drawFlash(float x,float y,float width,float height) {
		drawFlash(x,y,width,height,Color.white);
	}
	
	/**
	 * Set the centre of the rotation when applied to this image
	 * 
	 * @param x The x coordinate of center of rotation relative to the top left corner of the image
	 * @param y The y coordinate of center of rotation relative to the top left corner of the image
	 */
	public void setCenterOfRotation(float x, float y) {
		centerX = x;
		centerY = y;
	}

	/**
	 * Get the x component of the center of rotation of this image
	 * 
	 * @return The x component of the center of rotation 
	 */
	public float getCenterOfRotationX() {
		init();
		
		return centerX;
	}
	
	/**
	 * Get the y component of the center of rotation of this image
	 * 
	 * @return The y component of the center of rotation 
	 */
	public float getCenterOfRotationY() {
		init();
		
		return centerY;
	}
	
	/**
	 * Draw this image at a specified location and size as a silohette
	 * 
	 * @param x The x location to draw the image at
	 * @param y The y location to draw the image at
	 * @param width The width to render the image at
	 * @param height The height to render the image at
	 * @param col The color for the sillohette
	 */
	public void drawFlash(float x,float y,float width,float height, Color col) {
		init();
		
		col.bind();
		texture.bind();

		if (GL.canSecondaryColor()) {
			GL.glEnable(SGL.GL_COLOR_SUM_EXT);
			GL.glSecondaryColor3ubEXT((byte)(col.r * 255), 
													 (byte)(col.g * 255), 
													 (byte)(col.b * 255));
		}
		
		GL.glTexEnvi(SGL.GL_TEXTURE_ENV, SGL.GL_TEXTURE_ENV_MODE, SGL.GL_MODULATE);

        GL.glTranslatef(x, y, 0);
        if (angle != 0) {
	        GL.glTranslatef(centerX, centerY, 0.0f); 
	        GL.glRotatef(angle, 0.0f, 0.0f, 1.0f); 
	        GL.glTranslatef(-centerX, -centerY, 0.0f); 
        }
        
		GL.glBegin(SGL.GL_QUADS);
			drawEmbedded(0,0,width,height);
		GL.glEnd();

        if (angle != 0) {
	        GL.glTranslatef(centerX, centerY, 0.0f); 
	        GL.glRotatef(-angle, 0.0f, 0.0f, 1.0f); 
	        GL.glTranslatef(-centerX, -centerY, 0.0f); 
        }
        GL.glTranslatef(-x, -y, 0);
        
		if (GL.canSecondaryColor()) {
			GL.glDisable(SGL.GL_COLOR_SUM_EXT);
		}
	}

	/**
	 * Draw this image at a specified location and size in a white silohette
	 * 
	 * @param x The x location to draw the image at
	 * @param y The y location to draw the image at
	 */
	public void drawFlash(float x,float y) {
		drawFlash(x,y,getWidth(),getHeight());
	}
	
    /**
     * Set the angle to rotate this image to.  The angle will be normalized to 
     * be 0 <= angle < 360.  The image will be rotated around its center.
     * 
     * @param angle The angle to be set
     */
    public void setRotation(float angle) { 
        this.angle = angle % 360.0f; 
    } 
    
    /**
     * Get the current angle of rotation for this image.
     * The image will be rotated around its center.
     * 
     * @return The current angle.
     */
    public float getRotation() { 
        return angle; 
    } 
    
    /**
     * Get the alpha value to use when rendering this image
     * 
     * @return The alpha value to use when rendering this image
     */
    public float getAlpha() {
    	return alpha;
    }
    
    /**
     * Set the alpha value to use when rendering this image
     * 
     * @param alpha The alpha value to use when rendering this image
     */
    public void setAlpha(float alpha) {
    	this.alpha = alpha;
    }
    
    /**
     * Add the angle provided to the current rotation.  The angle will be normalized to 
     * be 0 <= angle < 360.  The image will be rotated around its center.
     *  
     * @param angle The angle to add.
     */
    public void rotate(float angle) { 
        this.angle += angle;
        this.angle = this.angle % 360;
    } 

	/**
	 * Get a sub-part of this image. Note that the create image retains a reference to the
	 * image data so should anything change it will affect sub-images too.
	 * 
	 * @param x The x coordinate of the sub-image
	 * @param y The y coordinate of the sub-image
	 * @param width The width of the sub-image
	 * @param height The height of the sub-image
	 * @return The image represent the sub-part of this image
	 */
	public Image getSubImage(int x,int y,int width,int height) {
		init();
		
		float newTextureOffsetX = ((x / (float) this.width) * textureWidth) + textureOffsetX;
		float newTextureOffsetY = ((y / (float) this.height) * textureHeight) + textureOffsetY;
		float newTextureWidth = ((width / (float) this.width) * textureWidth);
		float newTextureHeight = ((height / (float) this.height) * textureHeight);
		
		Image sub = new Image();
		sub.inited = true;
		sub.texture = this.texture;
		sub.textureOffsetX = newTextureOffsetX;
		sub.textureOffsetY = newTextureOffsetY;
		sub.textureWidth = newTextureWidth;
		sub.textureHeight = newTextureHeight;
		
		sub.width = width;
		sub.height = height;
		sub.ref = ref;
		sub.centerX = width / 2;
		sub.centerY = height / 2;
		
		return sub;
	}

	/**
	 * Draw a section of this image at a particular location and scale on the screen
	 * 
	 * @param x The x position to draw the image
	 * @param y The y position to draw the image
	 * @param srcx The x position of the rectangle to draw from this image (i.e. relative to this image)
	 * @param srcy The y position of the rectangle to draw from this image (i.e. relative to this image)
	 * @param srcx2 The x position of the bottom right cornder of rectangle to draw from this image (i.e. relative to this image)
	 * @param srcy2 The t position of the bottom right cornder of rectangle to draw from this image (i.e. relative to this image)
	 */
	public void draw(float x, float y, float srcx, float srcy, float srcx2, float srcy2) {
		draw(x,y,x+width,y+height,srcx,srcy,srcx2,srcy2);
	}
	
	/**
	 * Draw a section of this image at a particular location and scale on the screen
	 * 
	 * @param x The x position to draw the image
	 * @param y The y position to draw the image
	 * @param x2 The x position of the bottom right corner of the drawn image
	 * @param y2 The y position of the bottom right corner of the drawn image
	 * @param srcx The x position of the rectangle to draw from this image (i.e. relative to this image)
	 * @param srcy The y position of the rectangle to draw from this image (i.e. relative to this image)
	 * @param srcx2 The x position of the bottom right cornder of rectangle to draw from this image (i.e. relative to this image)
	 * @param srcy2 The t position of the bottom right cornder of rectangle to draw from this image (i.e. relative to this image)
	 */
	public void draw(float x, float y, float x2, float y2, float srcx, float srcy, float srcx2, float srcy2) {
		draw(x,y,x2,y2,srcx,srcy,srcx2,srcy2,Color.white);
	}
	
	/**
	 * Draw a section of this image at a particular location and scale on the screen
	 * 
	 * @param x The x position to draw the image
	 * @param y The y position to draw the image
	 * @param x2 The x position of the bottom right corner of the drawn image
	 * @param y2 The y position of the bottom right corner of the drawn image
	 * @param srcx The x position of the rectangle to draw from this image (i.e. relative to this image)
	 * @param srcy The y position of the rectangle to draw from this image (i.e. relative to this image)
	 * @param srcx2 The x position of the bottom right cornder of rectangle to draw from this image (i.e. relative to this image)
	 * @param srcy2 The t position of the bottom right cornder of rectangle to draw from this image (i.e. relative to this image)
	 * @param filter The colour filter to apply when drawing
	 */
	public void draw(float x, float y, float x2, float y2, float srcx, float srcy, float srcx2, float srcy2, Color filter) {
		init();

    	if (alpha != 1) {
    		if (filter == null) {
    			filter = Color.white;
    		}
    		
    		filter = new Color(filter);
    		filter.a *= alpha;
    	}
		filter.bind();
		texture.bind();
		
        GL.glTranslatef(x, y, 0);
        if (angle != 0) {
	        GL.glTranslatef(centerX, centerY, 0.0f); 
	        GL.glRotatef(angle, 0.0f, 0.0f, 1.0f); 
	        GL.glTranslatef(-centerX, -centerY, 0.0f); 
        }
        
        GL.glBegin(SGL.GL_QUADS); 
			drawEmbedded(0,0,x2-x,y2-y,srcx,srcy,srcx2,srcy2);
        GL.glEnd(); 
        
        if (angle != 0) {
	        GL.glTranslatef(centerX, centerY, 0.0f); 
	        GL.glRotatef(-angle, 0.0f, 0.0f, 1.0f); 
	        GL.glTranslatef(-centerX, -centerY, 0.0f); 
        }
        GL.glTranslatef(-x, -y, 0);
        
//		GL.glBegin(SGL.GL_QUADS);
//		drawEmbedded(x,y,x2,y2,srcx,srcy,srcx2,srcy2);
//		GL.glEnd();
	}
	
	/**
	 * Draw a section of this image at a particular location and scale on the screen, while this
	 * is image is "in use", i.e. between calls to startUse and endUse.
	 * 
	 * @param x The x position to draw the image
	 * @param y The y position to draw the image
	 * @param x2 The x position of the bottom right corner of the drawn image
	 * @param y2 The y position of the bottom right corner of the drawn image
	 * @param srcx The x position of the rectangle to draw from this image (i.e. relative to this image)
	 * @param srcy The y position of the rectangle to draw from this image (i.e. relative to this image)
	 * @param srcx2 The x position of the bottom right cornder of rectangle to draw from this image (i.e. relative to this image)
	 * @param srcy2 The t position of the bottom right cornder of rectangle to draw from this image (i.e. relative to this image)
	 */
	public void drawEmbedded(float x, float y, float x2, float y2, float srcx, float srcy, float srcx2, float srcy2) {
		drawEmbedded(x,y,x2,y2,srcx,srcy,srcx2,srcy2,null);
	}
	
	/**
	 * Draw a section of this image at a particular location and scale on the screen, while this
	 * is image is "in use", i.e. between calls to startUse and endUse.
	 * 
	 * @param x The x position to draw the image
	 * @param y The y position to draw the image
	 * @param x2 The x position of the bottom right corner of the drawn image
	 * @param y2 The y position of the bottom right corner of the drawn image
	 * @param srcx The x position of the rectangle to draw from this image (i.e. relative to this image)
	 * @param srcy The y position of the rectangle to draw from this image (i.e. relative to this image)
	 * @param srcx2 The x position of the bottom right cornder of rectangle to draw from this image (i.e. relative to this image)
	 * @param srcy2 The t position of the bottom right cornder of rectangle to draw from this image (i.e. relative to this image)
	 * @param filter The colour filter to apply when drawing
	 */
	public void drawEmbedded(float x, float y, float x2, float y2, float srcx, float srcy, float srcx2, float srcy2, Color filter) {
		if (filter != null) {
			filter.bind();
		}
		
		float mywidth = x2 - x;
		float myheight = y2 - y;
		float texwidth = srcx2 - srcx;
		float texheight = srcy2 - srcy;

		float newTextureOffsetX = (((srcx) / (width)) * textureWidth)
				+ textureOffsetX;
		float newTextureOffsetY = (((srcy) / (height)) * textureHeight)
				+ textureOffsetY;
		float newTextureWidth = ((texwidth) / (width))
				* textureWidth;
		float newTextureHeight = ((texheight) / (height))
				* textureHeight;

		GL.glTexCoord2f(newTextureOffsetX, newTextureOffsetY);
		GL.glVertex3f(x,y, 0.0f);
		GL.glTexCoord2f(newTextureOffsetX, newTextureOffsetY
				+ newTextureHeight);
		GL.glVertex3f(x,(y + myheight), 0.0f);
		GL.glTexCoord2f(newTextureOffsetX + newTextureWidth,
				newTextureOffsetY + newTextureHeight);
		GL.glVertex3f((x + mywidth),(y + myheight), 0.0f);
		GL.glTexCoord2f(newTextureOffsetX + newTextureWidth,
				newTextureOffsetY);
		GL.glVertex3f((x + mywidth),y, 0.0f);
	}
	
	/**
	 * Draw the image in a warper rectangle. The effects this can 
	 * have are many and varied, might be interesting though.
	 * 
	 * @param x1 The top left corner x coordinate
	 * @param y1 The top left corner y coordinate
	 * @param x2 The top right corner x coordinate
	 * @param y2 The top right corner y coordinate
	 * @param x3 The bottom right corner x coordinate
	 * @param y3 The bottom right corner y coordinate
	 * @param x4 The bottom left corner x coordinate
	 * @param y4 The bottom left corner y coordinate
	 */
	public void drawWarped(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {
        Color.white.bind();
        texture.bind();

        GL.glTranslatef(x1, y1, 0);
        if (angle != 0) {
            GL.glTranslatef(centerX, centerY, 0.0f);
            GL.glRotatef(angle, 0.0f, 0.0f, 1.0f);
            GL.glTranslatef(-centerX, -centerY, 0.0f);
        }

        GL.glBegin(SGL.GL_QUADS);
        init();

        GL.glTexCoord2f(textureOffsetX, textureOffsetY);
        GL.glVertex3f(0, 0, 0);
        GL.glTexCoord2f(textureOffsetX, textureOffsetY + textureHeight);
        GL.glVertex3f(x2 - x1, y2 - y1, 0);
        GL.glTexCoord2f(textureOffsetX + textureWidth, textureOffsetY
                + textureHeight);
        GL.glVertex3f(x3 - x1, y3 - y1, 0);
        GL.glTexCoord2f(textureOffsetX + textureWidth, textureOffsetY);
        GL.glVertex3f(x4 - x1, y4 - y1, 0);
        GL.glEnd();

        if (angle != 0) {
            GL.glTranslatef(centerX, centerY, 0.0f);
            GL.glRotatef(-angle, 0.0f, 0.0f, 1.0f);
            GL.glTranslatef(-centerX, -centerY, 0.0f);
        }
        GL.glTranslatef(-x1, -y1, 0);
    }
	
	/**
	 * Get the width of this image
	 * 
	 * @return The width of this image
	 */
	public int getWidth() {
		init();
		return width;
	}

	/**
	 * Get the height of this image
	 * 
	 * @return The height of this image
	 */
	public int getHeight() {
		init();
		return height;
	}
	
	/**
	 * Get a copy of this image. This is a shallow copy and does not 
	 * duplicate image adata.
	 * 
	 * @return The copy of this image
	 */
	public Image copy() {
		init();
		return getSubImage(0,0,width,height);
	}

	/**
	 * Get a scaled copy of this image with a uniform scale
	 * 
	 * @param scale The scale to apply
	 * @return The new scaled image
	 */
	public Image getScaledCopy(float scale) {
		init();
		return getScaledCopy((int) (width*scale),(int) (height*scale));
	}
	
	/**
	 * Get a scaled copy of this image
	 * 
	 * @param width The width of the copy
	 * @param height The height of the copy
	 * @return The new scaled image
	 */
	public Image getScaledCopy(int width, int height) {
		init();
		Image image = copy();
		image.width = width;
		image.height = height;
		image.centerX = width / 2;
		image.centerY = height / 2;
		return image;
	}
	
	/**
	 * Make sure the texture cordinates are inverse on the y axis
	 */
	public void ensureInverted() {
		if (textureHeight > 0) {
			textureOffsetY = textureOffsetY + textureHeight;
			textureHeight = -textureHeight;
		}
	}
	
	/**
	 * Get a copy image flipped on potentially two axis
	 * 
	 * @param flipHorizontal True if we want to flip the image horizontally
	 * @param flipVertical True if we want to flip the image vertically
	 * @return The flipped image instance
	 */
	public Image getFlippedCopy(boolean flipHorizontal, boolean flipVertical) {
		init();
		Image image = copy();
		
		if (flipHorizontal) {
			image.textureOffsetX = textureOffsetX + textureWidth;
			image.textureWidth = -textureWidth;
		}
		if (flipVertical) {
			image.textureOffsetY = textureOffsetY + textureHeight;
			image.textureHeight = -textureHeight;
		}
		
		return image;
	}

	/**
	 * End the use of this sprite sheet and release the lock. 
	 * 
	 * @see #startUse
	 */
	public void endUse() {
		if (inUse != this) {
			throw new RuntimeException("The sprite sheet is not currently in use");
		}
		inUse = null;
		GL.glEnd();
	}
	
	/**
	 * Start using this sheet. This method can be used for optimal rendering of a collection 
	 * of sprites from a single sprite sheet. First, startUse(). Then render each sprite by
	 * calling renderInUse(). Finally, endUse(). Between start and end there can be no rendering
	 * of other sprites since the rendering is locked for this sprite sheet.
	 */
	public void startUse() {
		if (inUse != null) {
			throw new RuntimeException("Attempt to start use of a sprite sheet before ending use with another - see endUse()");
		}
		inUse = this;
		init();

		Color.white.bind();
		texture.bind();
		GL.glBegin(SGL.GL_QUADS);
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		init();
		
		return "[Image "+ref+" "+width+"x"+height+"  "+textureOffsetX+","+textureOffsetY+","+textureWidth+","+textureHeight+"]";
	}
	
	/**
	 * Get the OpenGL texture holding this image
	 * 
	 * @return The OpenGL texture holding this image
	 */
	public Texture getTexture() {
		return texture;
	}
	
	/**
	 * Set the texture used by this image
	 * 
	 * @param texture The texture used by this image
	 */
	public void setTexture(Texture texture) {
		this.texture = texture;
		reinit();
	}

	/**
	 * Translate an unsigned int into a signed integer
	 * 
	 * @param b The byte to convert
	 * @return The integer value represented by the byte
	 */
	private int translate(byte b) {
		if (b < 0) {
			return 256 + b;
		}
		
		return b;
	}
	
	/**
	 * Get the colour of a pixel at a specified location in this image
	 * 
	 * @param x The x coordinate of the pixel
	 * @param y The y coordinate of the pixel
	 * @return The Color of the pixel at the specified location
	 */
	public Color getColor(int x, int y) {
		if (pixelData == null) {
			pixelData = texture.getTextureData();
		}
		
		int xo = (int) (textureOffsetX * texture.getTextureWidth());
		int yo = (int) (textureOffsetY * texture.getTextureHeight());
		
		if (textureWidth < 0) {
			x = xo - x;
		} else {
			x = xo + x;
		} 
		
		if (textureHeight < 0) {
			y = yo - y;
		} else {
			y = yo + y;
		}
		
		int offset = x + (y * texture.getTextureWidth());
		offset *= texture.hasAlpha() ? 4 : 3;
		
		if (texture.hasAlpha()) {
			return new Color(translate(pixelData[offset]),translate(pixelData[offset+1]),
							 translate(pixelData[offset+2]),translate(pixelData[offset+3]));
		} else {
			return new Color(translate(pixelData[offset]),translate(pixelData[offset+1]),
					 	     translate(pixelData[offset+2]));
		}
	}
	
	/**
	 * Check if this image has been destroyed
	 * 
	 * @return True if this image has been destroyed
	 */
	public boolean isDestroyed() {
		return destroyed;
	}
	
	/**
	 * Destroy the image and release any native resources. 
	 * Calls on a destroyed image have undefined results
	 * 
	 * @throws SlickException Indicates a failure to release resources on the graphics card
	 */
	public void destroy() throws SlickException {
		if (isDestroyed()) {
			return;
		}
		
		destroyed = true;
		texture.release();
		GraphicsFactory.releaseGraphicsForImage(this);
	}
	
	/**
	 * Flush the current pixel data to force a re-read next update
	 */
	public void flushPixelData() {
		pixelData = null;
	}
}
