package org.newdawn.slick.opengl.renderer;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/**
 * A renderer that caches all operations into an array, creates an opengl vertex array when
 * required and spits the data down to the card in batch mode
 * 
 * @author kevin
 */
public class VAOGLRenderer extends ImmediateModeOGLRenderer {
	/** The tolerance to rendering immediate */
	private static final int TOLERANCE = 20;
	/** Indicates there is no current geometry buffer */
	public static final int NONE = -1;
	/** The maximum number of vertices draw in one batch */
	public static final int MAX_VERTS = 5000;
	
	/** The type of the geometry array currently being built - i.e. GL_QUADS */
	private int currentType = NONE;
	/** The last colour applied */
	private float[] color = new float[] {1f,1f,1f,1f};
	/** The last texture applied */
	private float[] tex = new float[] {0f,0f};
	/** The index of the next vertex to be created */
	private int vertIndex;
	
	/** The vertex data cached */
	private float[] verts = new float[MAX_VERTS*3];
	/** The vertex colour data cached */
	private float[] cols = new float[MAX_VERTS*4];
	/** The vertex texture coordiante data cached */
	private float[] texs = new float[MAX_VERTS*3];
	
	/** The buffer used to pass the vertex data to the card */
	private FloatBuffer vertices = BufferUtils.createFloatBuffer(MAX_VERTS * 3);
	/** The buffer used to pass the vertex color data to the card */
	private FloatBuffer colors = BufferUtils.createFloatBuffer(MAX_VERTS * 4);
	/** The buffer used to pass the vertex texture coordinate data to the card */
	private FloatBuffer textures = BufferUtils.createFloatBuffer(MAX_VERTS * 2);
	
	/** The stack for entering list creation mode - when we're creating a list we can't use our VAs */
	private int listMode = 0;
	
	/**
	 * @see org.newdawn.slick.opengl.renderer.ImmediateModeOGLRenderer#initDisplay(int, int)
	 */
	public void initDisplay(int width, int height) {
		super.initDisplay(width, height);
		
		startBuffer();
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
		GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
	}

	/**
	 * Start a new buffer for a vertex array 
	 */
	private void startBuffer() {
		vertIndex = 0;
	}
	
	/**
	 * Flush the currently cached data down to the card 
	 */
	private void flushBuffer() {	
		if (vertIndex == 0) {
			return;
		}
		if (currentType == NONE) {
			return;
		}
		
		if (vertIndex < TOLERANCE) {
			GL11.glBegin(currentType);
			for (int i=0;i<vertIndex;i++) {
				GL11.glColor4f(cols[(i*4)+0], cols[(i*4)+1], cols[(i*4)+2], cols[(i*4)+3]);
				GL11.glTexCoord2f(texs[(i*2)+0], texs[(i*2)+1]);
				GL11.glVertex3f(verts[(i*3)+0], verts[(i*3)+1], verts[(i*3)+2]);
			}
			GL11.glEnd();
			currentType = NONE;
			return;
		}
		vertices.clear();
		colors.clear();
		textures.clear();
		
		vertices.put(verts,0,vertIndex*3);
		colors.put(cols,0,vertIndex*4);
		textures.put(texs,0,vertIndex*2);
		
		vertices.flip(); 
		colors.flip(); 
		textures.flip(); 
		
		GL11.glVertexPointer(3,0,vertices);     
		GL11.glColorPointer(4,0,colors);     
		GL11.glTexCoordPointer(2,0,textures);     
		
		GL11.glDrawArrays(currentType, 0, vertIndex);
		currentType = NONE;
	}
	
	/**
	 * Apply the current buffer and restart it
	 */
	private void applyBuffer() {
		if (listMode > 0) {
			return;
		}
		
		if (vertIndex != 0) {
			flushBuffer();
			startBuffer();
		}
		
		super.glColor4f(color[0], color[1], color[2], color[3]);
	}
	
	/**
	 * @see org.newdawn.slick.opengl.renderer.ImmediateModeOGLRenderer#flush()
	 */
	public void flush() {
		super.flush();
		
		applyBuffer();
	}
	
	/**
	 * @see org.newdawn.slick.opengl.renderer.ImmediateModeOGLRenderer#glBegin(int)
	 */
	public void glBegin(int geomType) {
		if (listMode > 0) {
			super.glBegin(geomType);
			return;
		}
		
		if (currentType != geomType) {
			applyBuffer();
			currentType = geomType;
		}
	}

	/**
	 * @see org.newdawn.slick.opengl.renderer.ImmediateModeOGLRenderer#glColor4f(float, float, float, float)
	 */
	public void glColor4f(float r, float g, float b, float a) {
		a *= alphaScale;
		
		color[0] = r;
		color[1] = g;
		color[2] = b;
		color[3] = a;
		
		if (listMode > 0) {
			super.glColor4f(r,g,b,a);
			return;
		}
	}

	/**
	 * @see org.newdawn.slick.opengl.renderer.ImmediateModeOGLRenderer#glEnd()
	 */
	public void glEnd() {
		if (listMode > 0) {
			super.glEnd();
			return;
		}
	}

	/**
	 * @see org.newdawn.slick.opengl.renderer.ImmediateModeOGLRenderer#glTexCoord2f(float, float)
	 */
	public void glTexCoord2f(float u, float v) {
		if (listMode > 0) {
			super.glTexCoord2f(u,v);
			return;
		}
		
		tex[0] = u;
		tex[1] = v;
	}

	/**
	 * @see org.newdawn.slick.opengl.renderer.ImmediateModeOGLRenderer#glVertex2f(float, float)
	 */
	public void glVertex2f(float x, float y) {
		if (listMode > 0) {
			super.glVertex2f(x,y);
			return;
		}
		
		glVertex3f(x,y,0);
	}

	/**
	 * @see org.newdawn.slick.opengl.renderer.ImmediateModeOGLRenderer#glVertex3f(float, float, float)
	 */
	public void glVertex3f(float x, float y, float z) {
		if (listMode > 0) {
			super.glVertex3f(x,y,z);
			return;
		}
		
		verts[(vertIndex*3)+0] = x;
		verts[(vertIndex*3)+1] = y;
		verts[(vertIndex*3)+2] = z;
		cols[(vertIndex*4)+0] = color[0];
		cols[(vertIndex*4)+1] = color[1];
		cols[(vertIndex*4)+2] = color[2];
		cols[(vertIndex*4)+3] = color[3];
		texs[(vertIndex*2)+0] = tex[0];
		texs[(vertIndex*2)+1] = tex[1];
		vertIndex++;
		
		if (vertIndex > MAX_VERTS - 50) {
			if (isSplittable(vertIndex, currentType)) {
				int type = currentType;
				applyBuffer();
				currentType = type;
			}
		}
	}

	/**
	 * Check if the geometry being created can be split at the current index
	 * 
	 * @param count The current index
	 * @param type The type of geometry being built
	 * @return True if the geometry can be split at the current index
	 */
	private boolean isSplittable(int count, int type) {
		switch (type) {
		case GL11.GL_QUADS:
			return count % 4 == 0;
		case GL11.GL_TRIANGLES:
			return count % 3 == 0;
		case GL11.GL_LINE:
			return count % 2 == 0;
		}
		
		return false;
	}
	
	/**
	 * @see org.newdawn.slick.opengl.renderer.ImmediateModeOGLRenderer#glBindTexture(int, int)
	 */
	public void glBindTexture(int target, int id) {
		applyBuffer();
		super.glBindTexture(target, id);
	}

	/**
	 * @see org.newdawn.slick.opengl.renderer.ImmediateModeOGLRenderer#glBlendFunc(int, int)
	 */
	public void glBlendFunc(int src, int dest) {
		applyBuffer();
		super.glBlendFunc(src, dest);
	}

	/**
	 * @see org.newdawn.slick.opengl.renderer.ImmediateModeOGLRenderer#glCallList(int)
	 */
	public void glCallList(int id) {
		applyBuffer();
		super.glCallList(id);
	}

	/**
	 * @see org.newdawn.slick.opengl.renderer.ImmediateModeOGLRenderer#glClear(int)
	 */
	public void glClear(int value) {
		applyBuffer();
		super.glClear(value);
	}

	/**
	 * @see org.newdawn.slick.opengl.renderer.ImmediateModeOGLRenderer#glClipPlane(int, java.nio.DoubleBuffer)
	 */
	public void glClipPlane(int plane, DoubleBuffer buffer) {
		applyBuffer();
		super.glClipPlane(plane, buffer);
	}

	/**
	 * @see org.newdawn.slick.opengl.renderer.ImmediateModeOGLRenderer#glColorMask(boolean, boolean, boolean, boolean)
	 */
	public void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
		applyBuffer();
		super.glColorMask(red, green, blue, alpha);
	}

	/**
	 * @see org.newdawn.slick.opengl.renderer.ImmediateModeOGLRenderer#glDisable(int)
	 */
	public void glDisable(int item) {
		applyBuffer();
		super.glDisable(item);
	}

	/**
	 * @see org.newdawn.slick.opengl.renderer.ImmediateModeOGLRenderer#glEnable(int)
	 */
	public void glEnable(int item) {
		applyBuffer();
		super.glEnable(item);
	}

	/**
	 * @see org.newdawn.slick.opengl.renderer.ImmediateModeOGLRenderer#glLineWidth(float)
	 */
	public void glLineWidth(float width) {
		applyBuffer();
		super.glLineWidth(width);
	}

	/**
	 * @see org.newdawn.slick.opengl.renderer.ImmediateModeOGLRenderer#glPointSize(float)
	 */
	public void glPointSize(float size) {
		applyBuffer();
		super.glPointSize(size);
	}

	/**
	 * @see org.newdawn.slick.opengl.renderer.ImmediateModeOGLRenderer#glPopMatrix()
	 */
	public void glPopMatrix() {
		applyBuffer();
		super.glPopMatrix();
	}

	/**
	 * @see org.newdawn.slick.opengl.renderer.ImmediateModeOGLRenderer#glPushMatrix()
	 */
	public void glPushMatrix() {
		applyBuffer();
		super.glPushMatrix();
	}

	/**
	 * @see org.newdawn.slick.opengl.renderer.ImmediateModeOGLRenderer#glRotatef(float, float, float, float)
	 */
	public void glRotatef(float angle, float x, float y, float z) {
		applyBuffer();
		super.glRotatef(angle, x, y, z);
	}

	/**
	 * @see org.newdawn.slick.opengl.renderer.ImmediateModeOGLRenderer#glScalef(float, float, float)
	 */
	public void glScalef(float x, float y, float z) {
		applyBuffer();
		super.glScalef(x, y, z);
	}

	/**
	 * @see org.newdawn.slick.opengl.renderer.ImmediateModeOGLRenderer#glScissor(int, int, int, int)
	 */
	public void glScissor(int x, int y, int width, int height) {
		applyBuffer();
		super.glScissor(x, y, width, height);
	}

	/**
	 * @see org.newdawn.slick.opengl.renderer.ImmediateModeOGLRenderer#glTexEnvi(int, int, int)
	 */
	public void glTexEnvi(int target, int mode, int value) {
		applyBuffer();
		super.glTexEnvi(target, mode, value);
	}

	/**
	 * @see org.newdawn.slick.opengl.renderer.ImmediateModeOGLRenderer#glTranslatef(float, float, float)
	 */
	public void glTranslatef(float x, float y, float z) {
		applyBuffer();
		super.glTranslatef(x, y, z);
	}

	/**
	 * @see org.newdawn.slick.opengl.renderer.ImmediateModeOGLRenderer#glEndList()
	 */
	public void glEndList() {
		listMode--;
		super.glEndList();
	}

	/**
	 * @see org.newdawn.slick.opengl.renderer.ImmediateModeOGLRenderer#glNewList(int, int)
	 */
	public void glNewList(int id, int option) {
		listMode++;
		super.glNewList(id, option);
	}

	/**
	 * @see org.newdawn.slick.opengl.renderer.SGL#getCurrentColor()
	 */
	public float[] getCurrentColor() {
		return color;
	}
	
	/**
	 * @see org.newdawn.slick.opengl.renderer.SGL#glLoadMatrix(java.nio.FloatBuffer)
	 */
	public void glLoadMatrix(FloatBuffer buffer) {
		flushBuffer();
		super.glLoadMatrix(buffer);
	}
}
