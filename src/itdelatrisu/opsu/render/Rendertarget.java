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
import fluddokt.opsu.fake.gl.*;

/*

import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

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
	
	/** The ID of the vertex buffer associated with this rendertarget. */
	private final int vboID;
	
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
		vboID = GL15.glGenBuffers();
		
		/*
		fboID = EXTFramebufferObject.glGenFramebuffersEXT();
		vboID = GL15.glGenBuffers();
		textureID = GL11.glGenTextures();
		depthBufferID = EXTFramebufferObject.glGenRenderbuffersEXT();
		*/
		fbo = new FrameBuffer(Format.RGBA8888, width, height, true);
	}

	/**
	 * Bind this rendertarget as the primary framebuffer.
	 */
	public void bind() {
		/*
		EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, fboID);
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
	 * Get the ID of the VBO associated with this Rendertarget.
	 * @return OpenGL buffer ID for the VBO
	 */
	public int getVbo() {
		return vboID;
	}
	
	/**
	 * Get the FBO ID.
	 * @return the OpenGL FBO ID
	 */
	/*
	public int getID() {
		return fboID;
	}
	*/

	/**
	 * Get the texture ID of the texture this rendertarget renders into.
	 * @return the OpenGL texture ID
	 */
	/*
	public int getTextureID() {
		return textureID;
	}
	*/

	/**
	 * Bind the default framebuffer.
	 */
	public static void unbind() {
		/*
		EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, 0);
		*/
		if(currentFBO != null)
			currentFBO.end();
	}

	/**
	 * Creates a Rendertarget with a Texture that it renders the color buffer in
	 * and a renderbuffer that it renders the depth to.
	 * @param width the width
	 * @param height the height
	 * @return the newly created Rendertarget instance
	*/
	public static Rendertarget createRTTFramebuffer(int width, int height) {
		/*
		int old_framebuffer = GL11.glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT);
		int old_texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		*/
		Rendertarget buffer = new Rendertarget(width,height);
		/*
		buffer.bind();

		int fboTexture = buffer.textureID;
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, fboTexture);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, 4, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_INT, (ByteBuffer) null);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

		EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, buffer.depthBufferID);
		EXTFramebufferObject.glRenderbufferStorageEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, GL11.GL_DEPTH_COMPONENT, width, height);
		EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, EXTFramebufferObject.GL_RENDERBUFFER_EXT, buffer.depthBufferID);

		EXTFramebufferObject.glFramebufferTexture2DEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT, GL11.GL_TEXTURE_2D, fboTexture, 0);

		GL11.glBindTexture(GL11.GL_TEXTURE_2D, old_texture);
		EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, old_framebuffer);
		*/
	
		return buffer;
	}

	/**
	 * Destroy the OpenGL objects associated with this Rendertarget. Do not try
	 * to use this rendertarget with OpenGL after calling this method.
	 */
	public void destroyRTT() {
		/*
		EXTFramebufferObject.glDeleteFramebuffersEXT(fboID);
		EXTFramebufferObject.glDeleteRenderbuffersEXT(depthBufferID);
		GL11.glDeleteTextures(textureID);
		GL15.glDeleteBuffers(vboID);
		
		*/
		fbo.dispose();
	}
}
