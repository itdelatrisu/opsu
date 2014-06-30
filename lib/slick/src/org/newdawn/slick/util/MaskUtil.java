package org.newdawn.slick.util;

import org.newdawn.slick.opengl.renderer.Renderer;
import org.newdawn.slick.opengl.renderer.SGL;

/**
 * A utility to provide full screen masking
 * 
 * @author kevin
 */
public class MaskUtil {
	/** The renderer to use for all GL operations */
	protected static SGL GL = Renderer.get();

	/**
	 * Start defining the screen mask. After calling this use graphics functions to 
	 * mask out the area
	 */
	public static void defineMask() {
		GL.glDepthMask(true);
		GL.glClearDepth(1);
		GL.glClear(SGL.GL_DEPTH_BUFFER_BIT);
		GL.glDepthFunc(SGL.GL_ALWAYS);
		GL.glEnable(SGL.GL_DEPTH_TEST);
		GL.glDepthMask(true);
		GL.glColorMask(false, false, false, false);
	}
	
	/**
	 * Finish defining the screen mask
	 */
	public static void finishDefineMask() {
		GL.glDepthMask(false);
		GL.glColorMask(true, true, true, true);
	}
	
	/**
	 * Start drawing only on the masked area
	 */
	public static void drawOnMask() {
		GL.glDepthFunc(SGL.GL_EQUAL);
	}

	/**
	 * Start drawing only off the masked area
	 */
	public static void drawOffMask() {
		GL.glDepthFunc(SGL.GL_NOTEQUAL);
	}
	
	/**
	 * Reset the masked area - should be done after you've finished rendering
	 */
	public static void resetMask() {
		GL.glDepthMask(true);
		GL.glClearDepth(0);
		GL.glClear(SGL.GL_DEPTH_BUFFER_BIT);
		GL.glDepthMask(false);
		
		GL.glDisable(SGL.GL_DEPTH_TEST);
	}
}
