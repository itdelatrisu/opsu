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

import java.nio.ByteBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;

/**
 * Represents a rendertarget. For now this maps to an OpenGL FBO via LWJGL.
 *
 * @author Bigpet {@literal <dravorek (at) gmail.com>}
 */
public class Rendertarget {
	/** The dimensions. */
	public final int width, height;

	/** The FBO ID. */
	private final int fboID;

	/** The texture ID. for the color buffer this rendertarget renders into. */
	private final int textureID;

	/** The renderbuffer ID for the depth buffer that this rendertarget renders into. */
	private final int depthBufferID;

	/**
	 * Create a new FBO.
	 * @param width the width
	 * @param height the height
	 */
	private Rendertarget(int width, int height) {
		this.width = width;
		this.height = height;
		fboID = GL30.glGenFramebuffers();
		textureID = GL11.glGenTextures();
		depthBufferID = GL30.glGenRenderbuffers();
	}

	/**
	 * Bind this rendertarget as the primary framebuffer.
	 */
	public void bind() {
		GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, fboID);
	}

	/**
	 * Returns the FBO ID.
	 */
	// NOTE: use judiciously, try to avoid if possible and consider adding a
	// method to this class if you find yourself calling this repeatedly.
	public int getID() {
		return fboID;
	}

	/**
	 * Returns the texture ID.
	 */
	// NOTE: try not to use, could be moved into separate class.
	public int getTextureID() {
		return textureID;
	}

	/**
	 * Bind the default framebuffer.
	 */
	public static void unbind() {
		GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, 0);
	}

	/**
	 * Creates a Rendertarget with a Texture that it renders the color buffer in
	 * and a renderbuffer that it renders the depth to.
	 * @param width the width
	 * @param height the height
	*/
	public static Rendertarget createRTTFramebuffer(int width, int height) {
		int old_framebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
		int old_texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		Rendertarget buffer = new Rendertarget(width,height);
		buffer.bind();

		int fboTexture = buffer.textureID;
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, fboTexture);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, 4, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_INT, (ByteBuffer) null);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

		GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, buffer.depthBufferID);
		GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL11.GL_DEPTH_COMPONENT, width, height);
		GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER, buffer.depthBufferID);

		GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, fboTexture, 0);
		GL20.glDrawBuffers(GL30.GL_COLOR_ATTACHMENT0);

		GL11.glBindTexture(GL11.GL_TEXTURE_2D, old_texture);
		GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, old_framebuffer);

		return buffer;
	}

	/**
	 * Destroy the OpenGL objects associated with this Rendertarget. Do not try
	 * to use this rendertarget with OpenGL after calling this method.
	 */
	public void destroyRTT()
	{
		GL30.glDeleteFramebuffers(fboID);
		GL30.glDeleteRenderbuffers(depthBufferID);
		GL11.glDeleteTextures(textureID);
	}
}
