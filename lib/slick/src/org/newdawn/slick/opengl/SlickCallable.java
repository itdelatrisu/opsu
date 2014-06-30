package org.newdawn.slick.opengl;

import org.lwjgl.opengl.GL11;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.opengl.renderer.Renderer;

/**
 * A utility to allow performing GL operations without contaminating the
 * Slick OpenGL state. Note this will not protect you from OpenGL programming errors
 * like a glBegin() without a glEnd(), or glPush() without glPop() etc.
 * 
 * Expected usage:
 * 
 * <code>
 * SlickCallable callable = new SlickCallable() {
 * 	   public performGLOperations() throws SlickException {
 * 			GL.glTranslate(0,0,1);
 * 			glBegin(GL.GL_POLYGONS);
 *  			glVertex(..);
 *              ...
 *          glEnd();
 *     }
 * }
 * callable.call();
 * </code>
 * 
 * Alternatively you can use the static methods directly
 * 
 * <code>
 * SlickCallable.enterSafeBlock();
 * 
 * GL.glTranslate(0,0,1);
 * glBegin(GL.GL_POLYGONS);
 *     glVertex(..);
 *     ...
 * glEnd();
 * 
 * SlickCallable.leaveSafeBlock();
 * </code>
 * 
 * @author kevin
 */
public abstract class SlickCallable {
	/** The last texture used */
	private static Texture lastUsed;
	/** True if we're in a safe block */
	private static boolean inSafe = false;
	
	
	/**
	 * Enter a safe block ensuring that all the OpenGL state that slick 
	 * uses is safe before touching the GL state directly.
	 */
	public static void enterSafeBlock() 
	{
		if (inSafe) {
			return;
		}
		
		Renderer.get().flush();
		lastUsed = TextureImpl.getLastBind();
		TextureImpl.bindNone();
		GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
		GL11.glPushClientAttrib(GL11.GL_ALL_CLIENT_ATTRIB_BITS);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glPushMatrix();
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glPushMatrix();
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		
		inSafe = true;
	}

	/**
	 * Leave a safe block ensuring that all of Slick's OpenGL state is
	 * restored since the last enter.
	 */
	public static void leaveSafeBlock() 
	{
		if (!inSafe) {
			return;
		}

		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glPopMatrix();
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glPopMatrix();
		GL11.glPopClientAttrib();
		GL11.glPopAttrib();
		
		if (lastUsed != null) {
			lastUsed.bind();
		} else {
			TextureImpl.bindNone();
		}
		
		inSafe = false;
	}
	
	/**
	 * Cause this callable to perform it's GL operations (@see performGLOperations()). This
	 * method will block until the GL operations have been performed.
	 *
	 * @throws SlickException Indicates a failure while performing the GL operations or 
	 * maintaing SlickState
	 */
	public final void call() throws SlickException {
		enterSafeBlock();
		
		performGLOperations();
		
		leaveSafeBlock();
	}
	
	/**
	 * Perform the GL operations that this callable is intended to. This operations should
	 * not effect the slick OpenGL state.
	 * 
	 * @throws SlickException Indicates a failure of some sort. This is user exception
	 */
	protected abstract void performGLOperations() throws SlickException;
}
