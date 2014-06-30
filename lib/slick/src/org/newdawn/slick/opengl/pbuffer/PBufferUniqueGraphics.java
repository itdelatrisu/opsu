package org.newdawn.slick.opengl.pbuffer;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.Pbuffer;
import org.lwjgl.opengl.PixelFormat;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.opengl.SlickCallable;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureImpl;
import org.newdawn.slick.opengl.InternalTextureLoader;
import org.newdawn.slick.util.Log;

/**
 * A graphics implementation that renders to a PBuffer using a unique context, i.e.
 * without render to texture
 *
 * @author kevin
 */
public class PBufferUniqueGraphics extends Graphics {
	/** The pbuffer we're going to render to */
	private Pbuffer pbuffer;
	/** The image we're we're sort of rendering to */
	private Image image;
	
	/**
	 * Create a new graphics context around a pbuffer
	 * 
	 * @param image The image we're rendering to
	 * @throws SlickException Indicates a failure to use pbuffers
	 */
	public PBufferUniqueGraphics(Image image) throws SlickException {
		super(image.getTexture().getTextureWidth(), image.getTexture().getTextureHeight());
		this.image = image;
		
		Log.debug("Creating pbuffer(unique) "+image.getWidth()+"x"+image.getHeight());
		if ((Pbuffer.getCapabilities() & Pbuffer.PBUFFER_SUPPORTED) == 0) {
			throw new SlickException("Your OpenGL card does not support PBuffers and hence can't handle the dynamic images required for this application.");
		}
	
		init();
	}	

	/**
	 * Initialise the PBuffer that will be used to render to
	 * 
	 * @throws SlickException
	 */
	private void init() throws SlickException {
		try {
			Texture tex = InternalTextureLoader.get().createTexture(image.getWidth(), image.getHeight(), image.getFilter());

			pbuffer = new Pbuffer(screenWidth, screenHeight, new PixelFormat(8, 0, 0), null, null);
			// Initialise state of the pbuffer context.
			pbuffer.makeCurrent();

			initGL();
			image.draw(0,0);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex.getTextureID());
			GL11.glCopyTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 0, 0, 
								  tex.getTextureWidth(), 
								  tex.getTextureHeight(), 0);
			image.setTexture(tex);
			
			Display.makeCurrent();
		} catch (Exception e) {
			Log.error(e);
			throw new SlickException("Failed to create PBuffer for dynamic image. OpenGL driver failure?");
		}
	}

	/**
	 * @see org.newdawn.slick.Graphics#disable()
	 */
	protected void disable() {
		// Bind the texture after rendering.
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, image.getTexture().getTextureID());
		GL11.glCopyTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 0, 0, 
							  image.getTexture().getTextureWidth(), 
							  image.getTexture().getTextureHeight(), 0);
		
		try {
			Display.makeCurrent();
		} catch (LWJGLException e) {
			Log.error(e);
		}
		
		SlickCallable.leaveSafeBlock();
	}

	/**
	 * @see org.newdawn.slick.Graphics#enable()
	 */
	protected void enable() {
		SlickCallable.enterSafeBlock();
		
		try {
			if (pbuffer.isBufferLost()) {
				pbuffer.destroy();
				init();
			}

			pbuffer.makeCurrent();
		} catch (Exception e) {
			Log.error("Failed to recreate the PBuffer");
			Log.error(e);
			throw new RuntimeException(e);
		}
		
		// Put the renderer contents to the texture
		TextureImpl.unbind();
		initGL();
	}
	
	/**
	 * Initialise the GL context
	 */
	protected void initGL() {
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glShadeModel(GL11.GL_SMOOTH);        
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_LIGHTING);                    
        
		GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);                
        GL11.glClearDepth(1);                                       
        
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        GL11.glViewport(0,0,screenWidth,screenHeight);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glLoadIdentity();
		
		enterOrtho();
	}
	
	/**
	 * Enter the orthographic mode 
	 */
	protected void enterOrtho() {
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		GL11.glOrtho(0, screenWidth, 0, screenHeight, 1, -1);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
	}
	
	/**
	 * @see org.newdawn.slick.Graphics#destroy()
	 */
	public void destroy() {
		super.destroy();
		
		pbuffer.destroy();
	}
	
	/**
	 * @see org.newdawn.slick.Graphics#flush()
	 */
	public void flush() {
		super.flush();
		
		image.flushPixelData();
	}
}
