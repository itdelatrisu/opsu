package org.newdawn.slick.opengl;

import org.newdawn.slick.opengl.renderer.Renderer;

/**
 * A collection of utilities to allow aid interaction with the GL provider
 * 
 * @author kevin
 */
public final class GLUtils {

	/**
	 * Check that we're in the right place to be doing GL operations
	 */
	public static void checkGLContext() {
        try {
        	Renderer.get().glGetError();
        } catch (NullPointerException e) {
        	throw new RuntimeException("OpenGL based resources (images, fonts, sprites etc) must be loaded as part of init() or the game loop. They cannot be loaded before initialisation.");
        }
	}
}
