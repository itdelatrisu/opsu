package org.newdawn.slick.opengl;

import java.io.IOException;
import java.io.InputStream;

import org.newdawn.slick.loading.DeferredResource;
import org.newdawn.slick.loading.LoadingList;
import org.newdawn.slick.opengl.renderer.SGL;

/**
 * A texture proxy that can be used to load a texture at a later date while still
 * allowing elements to reference it
 *
 * @author kevin
 */
public class DeferredTexture extends TextureImpl implements DeferredResource {
	/** The stream to read the texture from */
	private InputStream in;
	/** The name of the resource to load */
	private String resourceName;
	/** True if the image should be flipped */
	private boolean flipped;
	/** The filter to apply to the texture */
	private int filter;
	/** The texture we're proxying for */
	private TextureImpl target;
	/** The color to be transparent */
	private int[] trans;
	
	/**
	 * Create a new deferred texture
	 * 
	 * @param in The input stream from which to read the texture
	 * @param resourceName The name to give the resource
 	 * @param flipped True if the image should be flipped
	 * @param filter The filter to apply
	 * @param trans The colour to defined as transparent
	 */
	public DeferredTexture(InputStream in, String resourceName, boolean flipped, int filter, int[] trans) {
		this.in = in;
		this.resourceName = resourceName;
		this.flipped = flipped;
		this.filter = filter;
		this.trans = trans;
		
		LoadingList.get().add(this);
	}

	/**
	 * @see org.newdawn.slick.loading.DeferredResource#load()
	 */
	public void load() throws IOException {
		boolean before = InternalTextureLoader.get().isDeferredLoading();
		InternalTextureLoader.get().setDeferredLoading(false);
		target = InternalTextureLoader.get().getTexture(in, resourceName, flipped, filter, trans);
		InternalTextureLoader.get().setDeferredLoading(before);
	}
	
	/**
	 * Check if the target has been obtained already
	 */
	private void checkTarget() {
		if (target == null) {
			try {
				load();
				LoadingList.get().remove(this);
				return;
			} catch (IOException e) {
				throw new RuntimeException("Attempt to use deferred texture before loading and resource not found: "+resourceName);
			}
		}
	}
	
	/**
	 * @see org.newdawn.slick.opengl.TextureImpl#bind()
	 */
	public void bind() {
		checkTarget();

		target.bind();
	}

	/**
	 * @see org.newdawn.slick.opengl.TextureImpl#getHeight()
	 */
	public float getHeight() {
		checkTarget();

		return target.getHeight();
	}

	/**
	 * @see org.newdawn.slick.opengl.TextureImpl#getImageHeight()
	 */
	public int getImageHeight() {
		checkTarget();
		return target.getImageHeight();
	}

	/**
	 * @see org.newdawn.slick.opengl.TextureImpl#getImageWidth()
	 */
	public int getImageWidth() {
		checkTarget();
		return target.getImageWidth();
	}

	/**
	 * @see org.newdawn.slick.opengl.TextureImpl#getTextureHeight()
	 */
	public int getTextureHeight() {
		checkTarget();
		return target.getTextureHeight();
	}

	/**
	 * @see org.newdawn.slick.opengl.TextureImpl#getTextureID()
	 */
	public int getTextureID() {
		checkTarget();
		return target.getTextureID();
	}

	/**
	 * @see org.newdawn.slick.opengl.TextureImpl#getTextureRef()
	 */
	public String getTextureRef() {
		checkTarget();
		return target.getTextureRef();
	}

	/**
	 * @see org.newdawn.slick.opengl.TextureImpl#getTextureWidth()
	 */
	public int getTextureWidth() {
		checkTarget();
		return target.getTextureWidth();
	}

	/**
	 * @see org.newdawn.slick.opengl.TextureImpl#getWidth()
	 */
	public float getWidth() {
		checkTarget();
		return target.getWidth();
	}

	/**
	 * @see org.newdawn.slick.opengl.TextureImpl#release()
	 */
	public void release() {
		checkTarget();
		target.release();
	}

	/**
	 * @see org.newdawn.slick.opengl.TextureImpl#setAlpha(boolean)
	 */
	public void setAlpha(boolean alpha) {
		checkTarget();
		target.setAlpha(alpha);
	}

	/**
	 * @see org.newdawn.slick.opengl.TextureImpl#setHeight(int)
	 */
	public void setHeight(int height) {
		checkTarget();
		target.setHeight(height);
	}

	/**
	 * @see org.newdawn.slick.opengl.TextureImpl#setTextureHeight(int)
	 */
	public void setTextureHeight(int texHeight) {
		checkTarget();
		target.setTextureHeight(texHeight);
	}

	/**
	 * @see org.newdawn.slick.opengl.TextureImpl#setTextureID(int)
	 */
	public void setTextureID(int textureID) {
		checkTarget();
		target.setTextureID(textureID);
	}

	/**
	 * @see org.newdawn.slick.opengl.TextureImpl#setTextureWidth(int)
	 */
	public void setTextureWidth(int texWidth) {
		checkTarget();
		target.setTextureWidth(texWidth);
	}

	/**
	 * @see org.newdawn.slick.opengl.TextureImpl#setWidth(int)
	 */
	public void setWidth(int width) {
		checkTarget();
		target.setWidth(width);
	}
	
	/**
	 * @see org.newdawn.slick.opengl.TextureImpl#getTextureData()
	 */
	public byte[] getTextureData() {
		checkTarget();
		return target.getTextureData();
	}

	/**
	 * @see org.newdawn.slick.loading.DeferredResource#getDescription()
	 */
	public String getDescription() {
		return resourceName;
	}
	
    /**
	 * @see org.newdawn.slick.opengl.Texture#hasAlpha()
	 */
    public boolean hasAlpha() {
		checkTarget();
		return target.hasAlpha();
    }
    
    /**
     * @see org.newdawn.slick.opengl.Texture#setTextureFilter(int)
     */
	public void setTextureFilter(int textureFilter) {
		checkTarget();
		target.setTextureFilter(textureFilter);
	}
}
