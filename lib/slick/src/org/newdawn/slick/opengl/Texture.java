package org.newdawn.slick.opengl;

/**
 * The description of a texture loaded by the TextureLoader utility
 * 
 * @author kevin
 */
public interface Texture {

	/**
	 * Check if the texture has alpha
	 * 
	 * @return True if the texture has alpha
	 */
	public boolean hasAlpha();

	/**
	 * Get the reference from which this texture was loaded
	 * 
	 * @return The reference from which this texture was loaded
	 */
	public String getTextureRef();

	/**
	 * Bind the  GL context to a texture
	 */
	public void bind();

	/**
	 * Get the height of the original image
	 *
	 * @return The height of the original image
	 */
	public int getImageHeight();

	/** 
	 * Get the width of the original image
	 *
	 * @return The width of the original image
	 */
	public int getImageWidth();

	/**
	 * Get the height of the physical texture
	 *
	 * @return The height of physical texture
	 */
	public float getHeight();

	/**
	 * Get the width of the physical texture
	 *
	 * @return The width of physical texture
	 */
	public float getWidth();

	/**
	 * Get the height of the actual texture
	 * 
	 * @return The height of the actual texture
	 */
	public int getTextureHeight();

	/**
	 * Get the width of the actual texture
	 * 
	 * @return The width of the actual texture
	 */
	public int getTextureWidth();

	/**
	 * Destroy the texture reference
	 */
	public void release();

	/**
	 * Get the OpenGL texture ID for this texture
	 * 
	 * @return The OpenGL texture ID
	 */
	public int getTextureID();

	/**
	 * Get the pixel data from the card for this texture
	 * 
	 * @return The texture data from the card for this texture
	 */
	public byte[] getTextureData();
	
	/**
	 * Apply a given texture filter to the texture
	 * 
	 * @param textureFilter The texture filter to apply (GL_LINEAR, GL_NEAREST, etc..)
	 */
	public void setTextureFilter(int textureFilter);

}