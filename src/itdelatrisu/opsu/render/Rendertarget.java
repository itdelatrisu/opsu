/*
 *  opsu! - an open-source osu! client
 *  Copyright (C) 2014, 2015 Jeffrey Han
 *
 *  opsu! is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  opsu! is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with opsu!.  If not, see <http://www.gnu.org/licenses/>.
 */
package itdelatrisu.opsu.render;

import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;

import fluddokt.opsu.fake.Graphics;

/*
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
*/

/**
 * Represents a rendertarget. For now this maps to an OpenGL FBO via LWJGL.
 *
 * @author Bigpet {@literal <dravorek (at) gmail.com>}
 */
public class Rendertarget {
	/** The dimensions. */
	public final int width, height;

	static FrameBuffer currentFBO = null;
	FrameBuffer fbo;
	/** The FBO ID. */
	//private final int fboID;

	/** The texture ID for the color buffer this rendertarget renders into. */
	//private final int textureID;

	/** The renderbuffer ID for the depth buffer that this rendertarget renders into. */
	//private final int depthBufferID;

	/**
	 * Create a new FBO.
	 * @param width the width
	 * @param height the height
	 */
	private Rendertarget(int width, int height) {
		
		this.width = width;
		this.height = height;
		/*
		fboID = GL30.glGenFramebuffers();
		textureID = GL11.glGenTextures();
		depthBufferID = GL30.glGenRenderbuffers();
		*/
		fbo = new FrameBuffer(Format.RGBA8888, width, height, true);
	}

	/**
	 * Bind this rendertarget as the primary framebuffer.
	 */
	public void bind() {
		/*
		GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, fboID);
		Gdx.gl.glFramebufferRenderbuffer(Gdx.gl.GL_FRAMEBUFFER, Gdx.gl.GL_COLOR_ATTACHMENT0, Gdx.gl.GL_RENDERBUFFER, textureID);
		Gdx.gl.glFramebufferTexture2D(Gdx.gl.GL_FRAMEBUFFER, Gdx.gl.GL_COLOR_ATTACHMENT0, Gdx.gl.GL_TEXTURE_2D, textureID, 0);
		Gdx.gl.glFramebufferRenderbuffer(Gdx.gl.GL_FRAMEBUFFER, Gdx.gl.GL_DEPTH_ATTACHMENT, Gdx.gl.GL_RENDERBUFFER, depthBufferID);
		*/
		fbo.begin();
		currentFBO = fbo;
	}
	
	public void draw(int x, int y, int w, int h) {
		Graphics.getGraphics().drawTexture(
				fbo.getColorBufferTexture(), x, y, w, h
			);
	}

	/**
	 * Returns the FBO ID.
	 */
	// NOTE: use judiciously, try to avoid if possible and consider adding a
	// method to this class if you find yourself calling this repeatedly.
	/*
	public int getID() {
		return fboID;
	}
	*/

	/**
	 * Returns the texture ID.
	 */
	// NOTE: try not to use, could be moved into separate class.
	/*
	public int getTextureID() {
		return textureID;
	}
	*/

	/**
	 * Bind the default framebuffer.
	 */
	public static void unbind() {
		//GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, 0);
		if(currentFBO != null)
			currentFBO.end();
	}

	/**
	 * Creates a Rendertarget with a Texture that it renders the color buffer in
	 * and a renderbuffer that it renders the depth to.
	 * @param width the width
	 * @param height the height
	*/
	public static Rendertarget createRTTFramebuffer(int width, int height) {
		//int old_framebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
		//int old_texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		Rendertarget buffer = new Rendertarget(width,height);
		//buffer.bind();

		/*
		int fboTexture = buffer.textureID;
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, fboTexture);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, null);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP_TO_EDGE);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP_TO_EDGE);
		
		GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, buffer.depthBufferID);
		GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL11.GL_DEPTH_COMPONENT16, width, height);
		GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER, buffer.depthBufferID);
		
		GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, fboTexture, 0);
		//GL20.glDrawBuffers(GL30.GL_COLOR_ATTACHMENT0);
		GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL30.GL_RENDERBUFFER, fboTexture);
		
		 */
		/*
		int texH = height;
		int texW = width;
		int tb = buffer.textureID;
		int rb = buffer.depthBufferID;
		int fb = buffer.fboID;
		Gdx.gl.glBindTexture(Gdx.gl.GL_TEXTURE_2D, tb);
		Gdx.gl.glTexParameteri(Gdx.gl.GL_TEXTURE_2D, Gdx.gl.GL_TEXTURE_MIN_FILTER, Gdx.gl.GL_NEAREST);
		Gdx.gl.glTexParameteri(Gdx.gl.GL_TEXTURE_2D, Gdx.gl.GL_TEXTURE_MAG_FILTER, Gdx.gl.GL_NEAREST);
		Gdx.gl.glTexParameteri(Gdx.gl.GL_TEXTURE_2D, Gdx.gl.GL_TEXTURE_WRAP_S, Gdx.gl.GL_CLAMP_TO_EDGE);
		Gdx.gl.glTexParameteri(Gdx.gl.GL_TEXTURE_2D, Gdx.gl.GL_TEXTURE_WRAP_T, Gdx.gl.GL_CLAMP_TO_EDGE);
		Gdx.gl.glTexImage2D(Gdx.gl.GL_TEXTURE_2D, 0,  Gdx.gl.GL_RGBA, texH, texH, 0, Gdx.gl.GL_RGBA, Gdx.gl.GL_UNSIGNED_BYTE, 
		//bbuf
		null
		);
		
		Gdx.gl.glBindRenderbuffer(Gdx.gl.GL_RENDERBUFFER, rb);
		Gdx.gl.glRenderbufferStorage(Gdx.gl.GL_RENDERBUFFER, Gdx.gl.GL_DEPTH_COMPONENT16, texW, texH);
		//Gdx.gl.glBindRenderbuffer(Gdx.gl.GL_RENDERBUFFER, rb2);
		//Gdx.gl.glRenderbufferStorage(Gdx.gl.GL_RENDERBUFFER, Gdx.gl.GL_DEPTH_COMPONENT16, texW, texH);
	

		// Bind the framebuffer
		Gdx.gl.glBindFramebuffer(Gdx.gl.GL_FRAMEBUFFER, fb);
		 
		// specify texture as color attachment
		Gdx.gl.glFramebufferRenderbuffer(Gdx.gl.GL_FRAMEBUFFER, Gdx.gl.GL_COLOR_ATTACHMENT0, Gdx.gl.GL_RENDERBUFFER, tb);
		Gdx.gl.glFramebufferTexture2D(Gdx.gl.GL_FRAMEBUFFER, Gdx.gl.GL_COLOR_ATTACHMENT0, Gdx.gl.GL_TEXTURE_2D, tb, 0);
		 
		// attach render buffer as depth buffer
		Gdx.gl.glFramebufferRenderbuffer(Gdx.gl.GL_FRAMEBUFFER, Gdx.gl.GL_DEPTH_ATTACHMENT, Gdx.gl.GL_RENDERBUFFER, rb);
		//Gdx.gl.glFramebufferRenderbuffer(Gdx.gl.GL_FRAMEBUFFER, Gdx.gl.GL_STENCIL_ATTACHMENT, Gdx.gl.GL_RENDERBUFFER, rb2);
		
		switch( GL20.glCheckFramebufferStatus(GL20.GL_FRAMEBUFFER)){
		 case GL20.GL_FRAMEBUFFER_COMPLETE:
	            System.out.println("Frame Buffer Complete");
	            break;

	        case GL20.GL_FRAMEBUFFER_UNSUPPORTED:
	        	System.out.println("Frame Buffer unsupported");
	            break;

	        default:
	        	System.out.println("Frame Buffer unknown" + GL20.glCheckFramebufferStatus(GL20.GL_FRAMEBUFFER) );
		}
		
		
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, old_texture);
		GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, old_framebuffer);
*/
	
		return buffer;
	}

	/**
	 * Destroy the OpenGL objects associated with this Rendertarget. Do not try
	 * to use this rendertarget with OpenGL after calling this method.
	 */
	public void destroyRTT() {
		//GL30.glDeleteFramebuffers(fboID);
		//GL30.glDeleteRenderbuffers(depthBufferID);
		//GL11.glDeleteTextures(textureID);
		fbo.dispose();
	}
}
