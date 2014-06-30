package org.newdawn.slick.opengl.pbuffer;

import java.util.HashMap;

import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.Pbuffer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.util.Log;

/**
 * A factory to produce an appropriate render to texture graphics context based on current
 * hardware
 *
 * @author kevin
 */
public class GraphicsFactory {
	/** The graphics list of graphics contexts created */
	private static HashMap graphics = new HashMap();
	/** True if pbuffers are supported */
	private static boolean pbuffer = true;
	/** True if pbuffer render to texture are supported */
	private static boolean pbufferRT = true;
	/** True if fbo are supported */
	private static boolean fbo = true;
	/** True if we've initialised */
	private static boolean init = false;
	
	/**
	 * Initialise offscreen rendering by checking what buffers are supported
	 * by the card
	 * 
	 * @throws SlickException Indicates no buffers are supported
	 */
	private static void init() throws SlickException {
		init = true;
		
		if (fbo) {
			fbo = GLContext.getCapabilities().GL_EXT_framebuffer_object;
		}
		pbuffer = (Pbuffer.getCapabilities() & Pbuffer.PBUFFER_SUPPORTED) != 0;
		pbufferRT = (Pbuffer.getCapabilities() & Pbuffer.RENDER_TEXTURE_SUPPORTED) != 0;
		
		if (!fbo && !pbuffer && !pbufferRT) {
			throw new SlickException("Your OpenGL card does not support offscreen buffers and hence can't handle the dynamic images required for this application.");
		}
		
		Log.info("Offscreen Buffers FBO="+fbo+" PBUFFER="+pbuffer+" PBUFFERRT="+pbufferRT);
	}
	
	/**
	 * Force FBO use on or off
	 * 
	 * @param useFBO True if we should try and use FBO for offscreen images
	 */
	public static void setUseFBO(boolean useFBO) {
		fbo = useFBO;
	}
	
	/**
	 * Check if we're using FBO for dynamic textures
	 * 
	 * @return True if we're using FBOs
	 */
	public static boolean usingFBO() {
		return fbo;
	}

	/**
	 * Check if we're using PBuffer for dynamic textures
	 * 
	 * @return True if we're using PBuffer
	 */
	public static boolean usingPBuffer() {
		return !fbo && pbuffer;
	}
	
	/**
	 * Get a graphics context for a particular image
	 * 
	 * @param image The image for which to retrieve the graphics context
	 * @return The graphics context
	 * @throws SlickException Indicates it wasn't possible to create a graphics context
	 * given available hardware.
	 */
	public static Graphics getGraphicsForImage(Image image) throws SlickException {
		Graphics g = (Graphics) graphics.get(image.getTexture());
		
		if (g == null) {
			g = createGraphics(image);
			graphics.put(image.getTexture(), g);
		}
		
		return g;
	}
	
	/**
	 * Release any graphics context that is assocaited with the given image
	 * 
	 * @param image The image to release
	 * @throws SlickException Indicates a failure to release the context
	 */
	public static void releaseGraphicsForImage(Image image) throws SlickException {
		Graphics g = (Graphics) graphics.remove(image.getTexture());
		
		if (g != null) {
			g.destroy();
		}
	}
	
	/** 
	 * Create an underlying graphics context for the given image
	 * 
	 * @param image The image we want to render to
	 * @return The graphics context created
	 * @throws SlickException
	 */
	private static Graphics createGraphics(Image image) throws SlickException {
		init();
		
		if (fbo) {
			try {
				return new FBOGraphics(image);
			} catch (Exception e) {
				fbo = false;
				Log.warn("FBO failed in use, falling back to PBuffer");
			}
		}
		
		if (pbuffer) {
			if (pbufferRT) {
				return new PBufferGraphics(image);
			} else {
				return new PBufferUniqueGraphics(image);
			}
		}
		
		throw new SlickException("Failed to create offscreen buffer even though the card reports it's possible");
	}
}
