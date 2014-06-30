package org.newdawn.slick.opengl.renderer;

/**
 * The default version of the renderer relies of GL calls to do everything. 
 * Unfortunately this is driver dependent and often implemented inconsistantly
 * 
 * @author kevin
 */
public class DefaultLineStripRenderer implements LineStripRenderer {
	/** The access to OpenGL */
	private SGL GL = Renderer.get();
	
	/**
	 * @see org.newdawn.slick.opengl.renderer.LineStripRenderer#end()
	 */
	public void end() {
		GL.glEnd();
	}

	/**
	 * @see org.newdawn.slick.opengl.renderer.LineStripRenderer#setAntiAlias(boolean)
	 */
	public void setAntiAlias(boolean antialias) {
		if (antialias) {
			GL.glEnable(SGL.GL_LINE_SMOOTH);
		} else {
			GL.glDisable(SGL.GL_LINE_SMOOTH);
		}
	}

	/**
	 * @see org.newdawn.slick.opengl.renderer.LineStripRenderer#setWidth(float)
	 */
	public void setWidth(float width) {
		GL.glLineWidth(width);
	}

	/**
	 * @see org.newdawn.slick.opengl.renderer.LineStripRenderer#start()
	 */
	public void start() {
		GL.glBegin(SGL.GL_LINE_STRIP);
	}

	/**
	 * @see org.newdawn.slick.opengl.renderer.LineStripRenderer#vertex(float, float)
	 */
	public void vertex(float x, float y) {
		GL.glVertex2f(x,y);
	}

	/**
	 * @see org.newdawn.slick.opengl.renderer.LineStripRenderer#color(float, float, float, float)
	 */
	public void color(float r, float g, float b, float a) {
		GL.glColor4f(r, g, b, a);
	}

	/**
	 * @see org.newdawn.slick.opengl.renderer.LineStripRenderer#setLineCaps(boolean)
	 */
	public void setLineCaps(boolean caps) {
	}

	/**
	 * @see org.newdawn.slick.opengl.renderer.LineStripRenderer#applyGLLineFixes()
	 */
	public boolean applyGLLineFixes() {
		return true;
	}

}
