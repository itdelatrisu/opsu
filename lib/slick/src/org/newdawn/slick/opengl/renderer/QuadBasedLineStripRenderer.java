package org.newdawn.slick.opengl.renderer;

/**
 * A line strip renderer that uses quads to generate lines
 * 
 * @author kevin
 */
public class QuadBasedLineStripRenderer implements LineStripRenderer {
	/** The renderer used to interact with GL */
	private SGL GL = Renderer.get();
	
	/** Maximum number of points allowed in a single strip */
	public static int MAX_POINTS = 10000;
	/** True if antialiasing is currently enabled */
	private boolean antialias;
	/** The width of the lines to draw */
	private float width = 1;
	/** The points to draw */
	private float[] points;
	/** The colours to draw */
	private float[] colours;
	/** The number of points to draw */
	private int pts;
	/** The number of colour points recorded */
	private int cpt;
	
	/** The default renderer used when width = 1 */
	private DefaultLineStripRenderer def = new DefaultLineStripRenderer();
	/** Indicates need to render half colour */
	private boolean renderHalf;
	
	/** True if we shoudl render end caps */
	private boolean lineCaps = false;
	
	/** 
	 * Create a new strip renderer
	 */
	public QuadBasedLineStripRenderer() {
		points = new float[MAX_POINTS * 2];
		colours = new float[MAX_POINTS * 4];
	}
	
	/**
	 * Indicate if we should render end caps
	 * 
	 * @param caps True if we should render end caps
	 */
	public void setLineCaps(boolean caps) {
		this.lineCaps = caps;
	}
	
	/**
	 * @see org.newdawn.slick.opengl.renderer.LineStripRenderer#start()
	 */
	public void start() {
		if (width == 1) {
			def.start();
			return;
		}
		
		pts = 0;
		cpt = 0;
		GL.flush();
		
		float[] col = GL.getCurrentColor();
		color(col[0],col[1],col[2],col[3]);
	}
	
	/**
	 * @see org.newdawn.slick.opengl.renderer.LineStripRenderer#end()
	 */
	public void end() {
		if (width == 1) {
			def.end();
			return;
		}
		
		renderLines(points, pts);
	}
	
	/**
	 * @see org.newdawn.slick.opengl.renderer.LineStripRenderer#vertex(float, float)
	 */
	public void vertex(float x, float y) {
		if (width == 1) {
			def.vertex(x,y);
			return;
		}
		
		points[(pts*2)] = x;
		points[(pts*2)+1] = y;
		pts++;
		
		int index = pts-1;
		color(colours[(index*4)], colours[(index*4)+1], colours[(index*4)+2], colours[(index*4)+3]);
	}
	
	/**
	 * @see org.newdawn.slick.opengl.renderer.LineStripRenderer#setWidth(float)
	 */
	public void setWidth(float width) {
		this.width = width;
	}
	
	/**
	 * @see org.newdawn.slick.opengl.renderer.LineStripRenderer#setAntiAlias(boolean)
	 */
	public void setAntiAlias(boolean antialias) {
		def.setAntiAlias(antialias);
		this.antialias = antialias;
	}

	/**
	 * Render the lines applying antialiasing if required
	 * 
	 * @param points The points to be rendered as lines
	 * @param count The number of points to render
	 */
	public void renderLines(float[] points, int count) {
		if (antialias) {
			GL.glEnable(SGL.GL_POLYGON_SMOOTH);
			renderLinesImpl(points,count,width+1f);
		}

		GL.glDisable(SGL.GL_POLYGON_SMOOTH);
		renderLinesImpl(points,count,width);
		
		if (antialias) {
			GL.glEnable(SGL.GL_POLYGON_SMOOTH);
		}
	}
	
	/**
	 * Render the lines given
	 * 
	 * @param points The points building up the lines
	 * @param count The number of points to render
	 * @param w The width to render at
	 */
	public void renderLinesImpl(float[] points, int count, float w) {
		float width = w / 2;
		
		float lastx1 = 0;
		float lasty1 = 0;
		float lastx2 = 0;
		float lasty2 = 0;

		GL.glBegin(SGL.GL_QUADS);
		for (int i=0;i<count+1;i++) {
			int current = i;
			int next = i+1;
			int prev = i-1;
			if (prev < 0) {
				prev += count;
			}
			if (next >= count) {
				next -= count;
			}
			if (current >= count) {
				current -= count;
			}

			float x1 = points[(current*2)];
			float y1 = points[(current*2)+1];
			float x2 = points[(next*2)];
			float y2 = points[(next*2)+1];
			
			// draw the next segment
			float dx = x2 - x1;
			float dy = y2 - y1;

			if ((dx == 0) && (dy == 0)) {
				continue;
			}
			
			float d2 = (dx*dx)+(dy*dy);
			float d = (float) Math.sqrt(d2);
			dx *= width;
			dy *= width;
			dx /= d;
			dy /= d;
			
			float tx = dy;
			float ty = -dx;

			if (i != 0) {
				bindColor(prev);
				GL.glVertex3f(lastx1,lasty1,0);
				GL.glVertex3f(lastx2,lasty2,0);
				bindColor(current);
				GL.glVertex3f(x1+tx,y1+ty,0);
				GL.glVertex3f(x1-tx,y1-ty,0);
			}
			
			lastx1 = x2-tx;
			lasty1 = y2-ty;
			lastx2 = x2+tx;
			lasty2 = y2+ty;
			
			if (i < count-1) {
				bindColor(current);
				GL.glVertex3f(x1+tx,y1+ty,0);
				GL.glVertex3f(x1-tx,y1-ty,0);
				bindColor(next);
				GL.glVertex3f(x2-tx,y2-ty,0);
				GL.glVertex3f(x2+tx,y2+ty,0);
			}
		}
		
		GL.glEnd();
		
		float step = width <= 12.5f ?  5 : 180 / (float)Math.ceil(width / 2.5); 
		
		// start cap
		if (lineCaps) {
			float dx = points[2] - points[0];
			float dy = points[3] - points[1];
			float fang = (float) Math.toDegrees(Math.atan2(dy,dx)) + 90;
			
			if ((dx != 0) || (dy != 0)) {
				GL.glBegin(SGL.GL_TRIANGLE_FAN);
				bindColor(0);
				GL.glVertex2f(points[0], points[1]);
				for (int i=0;i<180+step;i+=step) {
					float ang = (float) Math.toRadians(fang+i);
					GL.glVertex2f(points[0]+((float) (Math.cos(ang) * width)), 
								  points[1]+((float) (Math.sin(ang) * width)));
				}
				GL.glEnd();
			}
		}
		
		// end cap
		if (lineCaps) {
			float dx = points[(count*2)-2] - points[(count*2)-4];
			float dy = points[(count*2)-1] - points[(count*2)-3];
			float fang = (float) Math.toDegrees(Math.atan2(dy,dx)) - 90;
			
			if ((dx != 0) || (dy != 0)) {
				GL.glBegin(SGL.GL_TRIANGLE_FAN);
				bindColor(count-1);
				GL.glVertex2f(points[(count*2)-2], points[(count*2)-1]);
				for (int i=0;i<180+step;i+=step) {
					float ang = (float) Math.toRadians(fang+i);
					GL.glVertex2f(points[(count*2)-2]+((float) (Math.cos(ang) * width)), 
								  points[(count*2)-1]+((float) (Math.sin(ang) * width)));
				}
				GL.glEnd();
			}
		}
	}

	/**
	 * Bind the colour at a given index in the array 
	 * 
	 * @param index The index of the colour to bind
	 */
	private void bindColor(int index) {
		if (index < cpt) {
			if (renderHalf) {
				GL.glColor4f(colours[(index*4)]*0.5f, colours[(index*4)+1]*0.5f,
						 	 colours[(index*4)+2]*0.5f, colours[(index*4)+3]*0.5f);
			} else {
				GL.glColor4f(colours[(index*4)], colours[(index*4)+1],
							 colours[(index*4)+2], colours[(index*4)+3]);
			}
		}
	}
	
	/**
	 * @see org.newdawn.slick.opengl.renderer.LineStripRenderer#color(float, float, float, float)
	 */
	public void color(float r, float g, float b, float a) {
		if (width == 1) {
			def.color(r,g,b,a);
			return;
		}
		
		colours[(pts*4)] = r;
		colours[(pts*4)+1] = g;
		colours[(pts*4)+2] = b;
		colours[(pts*4)+3] = a;
		cpt++;
	}

	public boolean applyGLLineFixes() {
		if (width == 1) {
			return def.applyGLLineFixes();
		}
		
		return def.applyGLLineFixes();
	}
}
