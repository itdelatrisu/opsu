package org.newdawn.slick.opengl.pbuffer;

import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.opengl.InternalTextureLoader;
import org.newdawn.slick.opengl.SlickCallable;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.util.Log;

/**
 * A graphics implementation that renders to an FBO
 *
 * @author kevin
 */
public class FBOGraphics extends Graphics {
	/** The image we're we're sort of rendering to */
	private Image image;
	/** The ID of the FBO in use */
	private int FBO;
	/** True if this context is valid */
	private boolean valid = true;
	
	/**
	 * Create a new graphics context around an FBO
	 * 
	 * @param image The image we're rendering to
	 * @throws SlickException Indicates a failure to use pbuffers
	 */
	public FBOGraphics(Image image) throws SlickException {
		super(image.getTexture().getTextureWidth(), image.getTexture().getTextureHeight());
		this.image = image;
		
		Log.debug("Creating FBO "+image.getWidth()+"x"+image.getHeight());
		
		boolean FBOEnabled = GLContext.getCapabilities().GL_EXT_framebuffer_object;
		if (!FBOEnabled) {
			throw new SlickException("Your OpenGL card does not support FBO and hence can't handle the dynamic images required for this application.");
		}
	
		init();
	}	

	/**
	 * Check the FBO for completeness as shown in the LWJGL tutorial
	 * 
	 * @throws SlickException Indicates an incomplete FBO
	 */
	private void completeCheck() throws SlickException {
		int framebuffer = EXTFramebufferObject.glCheckFramebufferStatusEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT); 
		switch ( framebuffer ) {
			case EXTFramebufferObject.GL_FRAMEBUFFER_COMPLETE_EXT:
				break;
			case EXTFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_EXT:
				throw new SlickException( "FrameBuffer: " + FBO
						+ ", has caused a GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_EXT exception" );
			case EXTFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT_EXT:
				throw new SlickException( "FrameBuffer: " + FBO
						+ ", has caused a GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT_EXT exception" );
			case EXTFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_EXT:
				throw new SlickException( "FrameBuffer: " + FBO
						+ ", has caused a GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_EXT exception" );
			case EXTFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER_EXT:
				throw new SlickException( "FrameBuffer: " + FBO
						+ ", has caused a GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER_EXT exception" );
			case EXTFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_FORMATS_EXT:
				throw new SlickException( "FrameBuffer: " + FBO
						+ ", has caused a GL_FRAMEBUFFER_INCOMPLETE_FORMATS_EXT exception" );
			case EXTFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER_EXT:
				throw new SlickException( "FrameBuffer: " + FBO
						+ ", has caused a GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER_EXT exception" );
			default:
				throw new SlickException( "Unexpected reply from glCheckFramebufferStatusEXT: " + framebuffer);
		}
	}
	
	/**
	 * Initialise the FBO that will be used to render to
	 * 
	 * @throws SlickException
	 */
	private void init() throws SlickException {
		IntBuffer buffer = BufferUtils.createIntBuffer(1);
		EXTFramebufferObject.glGenFramebuffersEXT(buffer); 
		FBO = buffer.get();

		// for some reason FBOs won't work on textures unless you've absolutely just
		// created them.
		try {
			Texture tex = InternalTextureLoader.get().createTexture(image.getWidth(), image.getHeight(), image.getFilter());
			
			EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, FBO);
			EXTFramebufferObject.glFramebufferTexture2DEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, 
														   EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT,
														   GL11.GL_TEXTURE_2D, tex.getTextureID(), 0);
			
			completeCheck();
			unbind();
			
			// Clear our destination area before using it
			clear();
			flush();
			
			// keep hold of the original content
			drawImage(image, 0, 0);
			image.setTexture(tex);
			
		} catch (Exception e) {
			throw new SlickException("Failed to create new texture for FBO");
		}
	}

	/**
	 * Bind to the FBO created
	 */
	private void bind() {
		EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, FBO);
		GL11.glReadBuffer(EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT);
	}

	/**
	 * Unbind from the FBO created
	 */
	private void unbind() {
		EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, 0);
		GL11.glReadBuffer(GL11.GL_BACK); 
	}
	
	/**
	 * @see org.newdawn.slick.Graphics#disable()
	 */
	protected void disable() {
		GL.flush();
		
		unbind();
		GL11.glPopClientAttrib();
		GL11.glPopAttrib();
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glPopMatrix();
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glPopMatrix();
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		
		SlickCallable.leaveSafeBlock();
	}

	/**
	 * @see org.newdawn.slick.Graphics#enable()
	 */
	protected void enable() {
		if (!valid) {
			throw new RuntimeException("Attempt to use a destroy()ed offscreen graphics context.");
		}
		SlickCallable.enterSafeBlock();
		
		GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
		GL11.glPushClientAttrib(GL11.GL_ALL_CLIENT_ATTRIB_BITS);
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glPushMatrix();
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glPushMatrix();
		
		bind();
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

		IntBuffer buffer = BufferUtils.createIntBuffer(1);
		buffer.put(FBO);
		buffer.flip();
		
		EXTFramebufferObject.glDeleteFramebuffersEXT(buffer);
		valid = false;
	}

	/**
	 * @see org.newdawn.slick.Graphics#flush()
	 */
	public void flush() {
		super.flush();
		
		image.flushPixelData();
	}

}
