package org.newdawn.slick.opengl.renderer;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.opengl.EXTSecondaryColor;
import org.lwjgl.opengl.EXTTextureMirrorClamp;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * The description of the OpenGL functions used Slick. Any other rendering method will
 * need to emulate these.
 * 
 * @author kevin
 */
public interface SGL {
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_TEXTURE_2D = GL11.GL_TEXTURE_2D;

	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_RGBA = GL11.GL_RGBA;
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_RGB = GL11.GL_RGB;
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_UNSIGNED_BYTE = GL11.GL_UNSIGNED_BYTE;
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_LINEAR = GL11.GL_LINEAR;
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_NEAREST = GL11.GL_NEAREST;
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_TEXTURE_MIN_FILTER = GL11.GL_TEXTURE_MIN_FILTER;
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_TEXTURE_MAG_FILTER = GL11.GL_TEXTURE_MAG_FILTER;
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_POINT_SMOOTH = GL11.GL_POINT_SMOOTH;
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_POLYGON_SMOOTH = GL11.GL_POLYGON_SMOOTH;
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_LINE_SMOOTH = GL11.GL_LINE_SMOOTH;
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_SCISSOR_TEST = GL11.GL_SCISSOR_TEST;

	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_MODULATE = GL11.GL_MODULATE;
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_TEXTURE_ENV = GL11.GL_TEXTURE_ENV;
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_TEXTURE_ENV_MODE = GL11.GL_TEXTURE_ENV_MODE;

	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_QUADS = GL11.GL_QUADS;
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_POINTS = GL11.GL_POINTS;
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_LINES = GL11.GL_LINES;
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_LINE_STRIP = GL11.GL_LINE_STRIP;
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_TRIANGLES = GL11.GL_TRIANGLES;
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_TRIANGLE_FAN = GL11.GL_TRIANGLE_FAN;

	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_SRC_ALPHA = GL11.GL_SRC_ALPHA;
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_ONE = GL11.GL_ONE;
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_ONE_MINUS_DST_ALPHA = GL11.GL_ONE_MINUS_DST_ALPHA;
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_DST_ALPHA = GL11.GL_DST_ALPHA;
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_ONE_MINUS_SRC_ALPHA = GL11.GL_ONE_MINUS_SRC_ALPHA;

	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_COMPILE = GL11.GL_COMPILE;
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_MAX_TEXTURE_SIZE = GL11.GL_MAX_TEXTURE_SIZE;
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_COLOR_BUFFER_BIT = GL11.GL_COLOR_BUFFER_BIT;
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_DEPTH_BUFFER_BIT = GL11.GL_DEPTH_BUFFER_BIT;
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_BLEND = GL11.GL_BLEND;
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_COLOR_CLEAR_VALUE = GL11.GL_COLOR_CLEAR_VALUE;
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_LINE_WIDTH = GL11.GL_LINE_WIDTH;
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_CLIP_PLANE0 = GL11.GL_CLIP_PLANE0;
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_CLIP_PLANE1 = GL11.GL_CLIP_PLANE1;
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_CLIP_PLANE2 = GL11.GL_CLIP_PLANE2;
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_CLIP_PLANE3 = GL11.GL_CLIP_PLANE3;
	
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_COMPILE_AND_EXECUTE = GL11.GL_COMPILE_AND_EXECUTE;

	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_RGBA8 = GL11.GL_RGBA;
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_RGBA16 = GL11.GL_RGBA16;
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_BGRA = GL12.GL_BGRA;
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_MIRROR_CLAMP_TO_EDGE_EXT = EXTTextureMirrorClamp.GL_MIRROR_CLAMP_TO_EDGE_EXT;

	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_TEXTURE_WRAP_S = GL11.GL_TEXTURE_WRAP_S;
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_TEXTURE_WRAP_T = GL11.GL_TEXTURE_WRAP_T;

	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_CLAMP = GL11.GL_CLAMP;

	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_COLOR_SUM_EXT = EXTSecondaryColor.GL_COLOR_SUM_EXT;

	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_ALWAYS = GL11.GL_ALWAYS;

	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_DEPTH_TEST = GL11.GL_DEPTH_TEST;

	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_NOTEQUAL = GL11.GL_NOTEQUAL;
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_EQUAL = GL11.GL_EQUAL;
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_SRC_COLOR = GL11.GL_SRC_COLOR;
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_ONE_MINUS_SRC_COLOR = GL11.GL_ONE_MINUS_SRC_COLOR;
	/** OpenGL Enum - @url http://www.opengl.org/documentation */
	public static final int GL_MODELVIEW_MATRIX = GL11.GL_MODELVIEW_MATRIX;
	
	/**
	 * Flush the current state of the renderer down to GL
	 */
	public void flush();
	
	/**
	 * Initialise the display
	 * 
	 * @param width The width of the display
	 * @param height The height of the display
	 */
	public void initDisplay(int width, int height);

	/**
	 * Enter orthographic mode
	 * 
	 * @param xsize The size of the ortho display
	 * @param ysize The size of the ortho display
	 */
	public void enterOrtho(int xsize, int ysize);

	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 * 
	 * @param red
	 * @param green
	 * @param blue
	 * @param alpha
	 */
	public void glClearColor(float red, float green, float blue, float alpha);

	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 * 
	 * @param plane
	 * @param buffer
	 */
	public void glClipPlane(int plane, DoubleBuffer buffer);
	
	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 * 
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 */
	public void glScissor(int x, int y, int width, int height);
	
	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 * 
	 * @param width
	 */
	public void glLineWidth(float width);
	
	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 * 
	 * @param value
	 */
	public void glClear(int value);
	
	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 * 
	 * @param red
	 * @param green
	 * @param blue
	 * @param alpha
	 */
	public void glColorMask(boolean red, boolean green, boolean blue, boolean alpha);
	
	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 */
	public void glLoadIdentity();
	
	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 * 
	 * @param id
	 * @param ret
	 */
	public void glGetInteger(int id, IntBuffer ret);
	
	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 * 
	 * @param id
	 * @param ret
	 */
	public void glGetFloat(int id, FloatBuffer ret);
	
	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 * 
	 * @param item
	 */
	public void glEnable(int item);
	
	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 * 
	 * @param item
	 */
	public void glDisable(int item);
	
	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 * 
	 * @param target
	 * @param id
	 */
	public void glBindTexture(int target, int id);
	
	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 * 
	 * @param target
	 * @param level
	 * @param format
	 * @param type
	 * @param pixels
	 */
	public void glGetTexImage(int target, int level, int format, int type, ByteBuffer pixels);
	
	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 * 
	 * @param buffer
	 */
	public void glDeleteTextures(IntBuffer buffer);

	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 * 
	 * @param r
	 * @param g
	 * @param b
	 * @param a
	 */
	public void glColor4f(float r, float g, float b, float a);
	
	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 * 
	 * @param u
	 * @param v
	 */
	public void glTexCoord2f(float u, float v);
	
	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 * 
	 * @param x
	 * @param y
	 * @param z
	 */
	public void glVertex3f(float x, float y, float z);

	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 * 
	 * @param x
	 * @param y
	 */
	public void glVertex2f(float x, float y);
	
	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 * 
	 * @param angle
	 * @param x
	 * @param y
	 * @param z
	 */
	public void glRotatef(float angle, float x, float y, float z);
	
	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 * 
	 * @param x
	 * @param y
	 * @param z
	 */
	public void glTranslatef(float x, float y, float z);
	
	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 * 
	 * @param geomType
	 */
	public void glBegin(int geomType);
	
	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 */
	public void glEnd();
	
	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 * 
	 * @param target
	 * @param mode
	 * @param value
	 */
	public void glTexEnvi(int target, int mode, int value);
	
	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 * 
	 * @param size
	 */
	public void glPointSize(float size);
	
	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 * 
	 * @param x
	 * @param y
	 * @param z
	 */
	public void glScalef(float x, float y, float z);
	
	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 */
	public void glPushMatrix();
	
	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 */
	public void glPopMatrix();
	
	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 * 
	 * @param src
	 * @param dest
	 */
	public void glBlendFunc(int src, int dest);
	
	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 * 
	 * @param count
	 * @return The index of the lists
	 */
	public int glGenLists(int count);
	
	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 * 
	 * @param id
	 * @param option
	 */
	public void glNewList(int id, int option);
	
	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 */
	public void glEndList();
	
	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 * 
	 * @param id
	 */
	public void glCallList(int id);
	
	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 * 
	 * @param target
	 * @param level
	 * @param internalFormat
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @param border
	 */
	public void glCopyTexImage2D(int	target, int level, int internalFormat,
			 							int x, int y, int width, int height, int border);
	
	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 * 
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @param format
	 * @param type
	 * @param pixels
	 */
	public void glReadPixels(int x, int y, int width, int height, int format, int type,
		     						ByteBuffer pixels);
	
	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 * 
	 * @param target
	 * @param param
	 * @param value
	 */
	public void glTexParameteri(int target, int param, int value);
	
	/**
	 * Get the current colour being rendered
	 * 
	 * @return The current colour being rendered
	 */
	public float[] getCurrentColor();

	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 * 
	 * @param list
	 * @param count
	 */
	public void glDeleteLists(int list, int count);
	
	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 * 
	 * @param mask
	 */
	public void glDepthMask(boolean mask);
	
	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 * 
	 * @param value
	 */
	public void glClearDepth(float value);
	
	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 * 
	 * @param func
	 */
	public void glDepthFunc(int func);
	
	/**
	 * Set the scaling we'll apply to any colour binds in this renderer
	 * 
	 * @param alphaScale The scale to apply to any colour binds
	 */
	public void setGlobalAlphaScale(float alphaScale);

	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 * 
	 * @param buffer
	 */
	public void glLoadMatrix(FloatBuffer buffer);

	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 * 
	 * @param ids
	 */
	public void glGenTextures(IntBuffer ids);

	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 */
	public void glGetError();

	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 */
	public void glTexImage2D(int target, int i, int dstPixelFormat,
			int get2Fold, int get2Fold2, int j, int srcPixelFormat,
			int glUnsignedByte, ByteBuffer textureBuffer);

	/**
	 * OpenGL Method - @url http://www.opengl.org/documentation/
	 */
	public void glTexSubImage2D(int glTexture2d, int i, int pageX, int pageY,
			int width, int height, int glBgra, int glUnsignedByte,
			ByteBuffer scratchByteBuffer);

	/**
	 * Check if the mirror clamp extension is available
	 * 
	 * @return True if the mirro clamp extension is available
	 */
	public boolean canTextureMirrorClamp();

	public boolean canSecondaryColor();

	public void glSecondaryColor3ubEXT(byte b, byte c, byte d);
}
