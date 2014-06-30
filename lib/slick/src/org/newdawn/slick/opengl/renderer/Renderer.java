package org.newdawn.slick.opengl.renderer;


/**
 * The static holder for the current GL implementation. Note that this 
 * renderer can only be set before the game has been started.
 * 
 * @author kevin
 */
public class Renderer {		
	/** The indicator for immediate mode renderering (the default) */
	public static final int IMMEDIATE_RENDERER = 1;
	/** The indicator for vertex array based rendering */
	public static final int VERTEX_ARRAY_RENDERER = 2;
	
	/** The indicator for direct GL line renderer (the default) */
	public static final int DEFAULT_LINE_STRIP_RENDERER = 3;
	/** The indicator for consistant quad based lines */
	public static final int QUAD_BASED_LINE_STRIP_RENDERER = 4;
	
	
	/** The renderer in use */
	private static SGL renderer = new ImmediateModeOGLRenderer();
	/** The line strip renderer to use */
	private static LineStripRenderer lineStripRenderer = new DefaultLineStripRenderer();
	
	/** 
	 * Set the renderer to one of the known types
	 * 
	 * @param type The type of renderer to use
	 */
	public static void setRenderer(int type) {
		switch (type) {
			case IMMEDIATE_RENDERER:
				setRenderer(new ImmediateModeOGLRenderer());
				return;
			case VERTEX_ARRAY_RENDERER:
				setRenderer(new VAOGLRenderer());
				return;
		}
		
		throw new RuntimeException("Unknown renderer type: "+type);
	}
	
	/**
	 * Set the line strip renderer to one of the known types
	 * 
	 * @param type The type of renderer to use
	 */
	public static void setLineStripRenderer(int type) {
		switch (type) {
		case DEFAULT_LINE_STRIP_RENDERER:
			setLineStripRenderer(new DefaultLineStripRenderer());
			return;
		case QUAD_BASED_LINE_STRIP_RENDERER:
			setLineStripRenderer(new QuadBasedLineStripRenderer());
			return;
		}
		
		throw new RuntimeException("Unknown line strip renderer type: "+type);
	}
	
	/**
	 * Set the line strip renderer to be used globally
	 * 
	 * @param renderer The line strip renderer to be used
	 */
	public static void setLineStripRenderer(LineStripRenderer renderer) {
		lineStripRenderer = renderer;
	}
	
	/**
	 * Set the renderer to be used
	 * 
	 * @param r The renderer to be used
	 */
	public static void setRenderer(SGL r) {
		renderer = r;
	}
	
	/**
	 * Get the renderer to be used when accessing GL
	 * 
	 * @return The renderer to be used when accessing GL
	 */
	public static SGL get() {
		return renderer;
	}
	
	/**
	 * Get the line strip renderer to use 
	 * 
	 * @return The line strip renderer to use
	 */
	public static LineStripRenderer getLineStripRenderer() {
		return lineStripRenderer;
	}
	
}
