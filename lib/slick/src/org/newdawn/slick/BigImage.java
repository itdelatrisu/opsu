package org.newdawn.slick;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.newdawn.slick.opengl.ImageData;
import org.newdawn.slick.opengl.ImageDataFactory;
import org.newdawn.slick.opengl.LoadableImageData;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.renderer.SGL;
import org.newdawn.slick.opengl.renderer.Renderer;
import org.newdawn.slick.util.OperationNotSupportedException;
import org.newdawn.slick.util.ResourceLoader;

/**
 * An image implementation that handles loaded images that are larger than the 
 * maximum texture size supported by the card. In most cases it makes sense
 * to make sure all of your image resources are less than 512x512 in size when
 * using OpenGL. However, in the rare circumstances where this isn't possible
 * this implementation can be used to draw a tiled version of the image built
 * from several smaller textures. 
 * 
 * This implementation does come with limitations and some performance impact
 * however - so use only when absolutely required.
 *
 * TODO: The code in here isn't pretty, really needs revisiting with a comment stick.
 * 
 * @author kevin
 */
public class BigImage extends Image {
	/** The renderer to use for all GL operations */
	protected static SGL GL = Renderer.get();
	
	/**
	 * Get the maximum size of an image supported by the underlying
	 * hardware.
	 * 
	 * @return The maximum size of the textures supported by the underlying
	 * hardware.
	 */
	public static final int getMaxSingleImageSize() {
		IntBuffer buffer = BufferUtils.createIntBuffer(16);
		GL.glGetInteger(SGL.GL_MAX_TEXTURE_SIZE, buffer);
		
		return buffer.get(0);
	}
	
	/** The last image that we put into "in use" mode */
	private static Image lastBind;
	
	/** The images building up this sub-image */
	private Image[][] images;
	/** The number of images on the xaxis */
	private int xcount;
	/** The number of images on the yaxis */
	private int ycount;
	/** The real width of the whole image - maintained even when scaled */
	private int realWidth;
	/** The real hieght of the whole image - maintained even when scaled */
	private int realHeight;
	
	/**
	 * Create a new big image. Empty contructor for cloning only
	 */
	private BigImage() {
		inited = true;
	}
	
	/**
	 * Create a new big image by loading it from the specified reference
	 * 
	 * @param ref The reference to the image to load
	 * @throws SlickException Indicates we were unable to locate the resource
	 */
	public BigImage(String ref) throws SlickException {
		this(ref, Image.FILTER_NEAREST);
	}

	/**
	 * Create a new big image by loading it from the specified reference
	 * 
	 * @param ref The reference to the image to load
	 * @param filter The image filter to apply (@see #Image.FILTER_NEAREST)
	 * @throws SlickException Indicates we were unable to locate the resource
	 */
	public BigImage(String ref,int filter) throws SlickException {
		
		build(ref, filter, getMaxSingleImageSize());
	}

	/**
	 * Create a new big image by loading it from the specified reference
	 * 
	 * @param ref The reference to the image to load
	 * @param filter The image filter to apply (@see #Image.FILTER_NEAREST)
	 * @param tileSize The maximum size of the tiles to use to build the bigger image
	 * @throws SlickException Indicates we were unable to locate the resource
	 */
	public BigImage(String ref, int filter, int tileSize) throws SlickException {
		build(ref, filter, tileSize);
	}

	/**
	 * Create a new big image by loading it from the specified image data
	 * 
	 * @param data The pixelData to use to create the image
	 * @param imageBuffer The buffer containing texture data
	 * @param filter The image filter to apply (@see #Image.FILTER_NEAREST)
	 */
	public BigImage(LoadableImageData data, ByteBuffer imageBuffer, int filter) {
		build(data, imageBuffer, filter, getMaxSingleImageSize());
	}
	
	/**
	 * Create a new big image by loading it from the specified image data
	 * 
	 * @param data The pixelData to use to create the image
	 * @param imageBuffer The buffer containing texture data
	 * @param filter The image filter to apply (@see #Image.FILTER_NEAREST)
	 * @param tileSize The maximum size of the tiles to use to build the bigger image
	 */
	public BigImage(LoadableImageData data, ByteBuffer imageBuffer, int filter, int tileSize) {
		build(data, imageBuffer, filter, tileSize);
	}

	/**
	 * Get a sub tile of this big image. Useful for debugging
	 * 
	 * @param x The x tile index
	 * @param y The y tile index
	 * @return The image used for this tile
	 */
	public Image getTile(int x, int y) {
		return images[x][y];
	}
	
	/**
	 * Create a new big image by loading it from the specified reference
	 * 
	 * @param ref The reference to the image to load
	 * @param filter The image filter to apply (@see #Image.FILTER_NEAREST)
	 * @param tileSize The maximum size of the tiles to use to build the bigger image
	 * @throws SlickException Indicates we were unable to locate the resource
	 */
	private void build(String ref, int filter, int tileSize) throws SlickException {
		try {
			final LoadableImageData data = ImageDataFactory.getImageDataFor(ref);
			final ByteBuffer imageBuffer = data.loadImage(ResourceLoader.getResourceAsStream(ref), false, null);
			build(data, imageBuffer, filter, tileSize);
		} catch (IOException e) {
			throw new SlickException("Failed to load: "+ref, e);
		}
	}
	
	/**
	 * Create an big image from a image data source. 
	 * 
	 * @param data The pixelData to use to create the image
	 * @param imageBuffer The buffer containing texture data
	 * @param filter The filter to use when scaling this image
	 * @param tileSize The maximum size of the tiles to use to build the bigger image
	 */
	private void build(final LoadableImageData data, final ByteBuffer imageBuffer, int filter, int tileSize) {
		final int dataWidth = data.getTexWidth();
		final int dataHeight = data.getTexHeight();
		
		realWidth = width = data.getWidth();
		realHeight = height = data.getHeight();
		
		if ((dataWidth <= tileSize) && (dataHeight <= tileSize)) {
			images = new Image[1][1];
			ImageData tempData = new ImageData() {
				public int getDepth() {
					return data.getDepth();
				}

				public int getHeight() {
					return dataHeight;
				}

				public ByteBuffer getImageBufferData() {
					return imageBuffer;
				}

				public int getTexHeight() {
					return dataHeight;
				}

				public int getTexWidth() {
					return dataWidth;
				}

				public int getWidth() {
					return dataWidth;
				}
			};
			images[0][0] = new Image(tempData, filter);
			xcount = 1;
			ycount = 1;
			inited = true;
			return;
		}
		
		xcount = ((realWidth-1) / tileSize) + 1;
		ycount = ((realHeight-1) / tileSize) + 1;
		
		images = new Image[xcount][ycount];
		int components = data.getDepth() / 8;
		
		for (int x=0;x<xcount;x++) {
			for (int y=0;y<ycount;y++) {
				int finalX = ((x+1) * tileSize);
				int finalY = ((y+1) * tileSize);
				final int imageWidth = Math.min((realWidth - (x*tileSize)), tileSize); 
				final int imageHeight = Math.min((realHeight - (y*tileSize)), tileSize);
				
				final int xSize = tileSize;
				final int ySize = tileSize;
				
				final ByteBuffer subBuffer = BufferUtils.createByteBuffer(tileSize*tileSize*components);
				int xo = x*tileSize*components;

				byte[] byteData = new byte[xSize*components];
				for (int i=0;i<ySize;i++) {
					int yo = (((y * tileSize) + i) * dataWidth) * components;
					imageBuffer.position(yo+xo);
					
					imageBuffer.get(byteData, 0, xSize*components);
					subBuffer.put(byteData);
				}
				
				subBuffer.flip();
				ImageData imgData = new ImageData() {
					public int getDepth() {
						return data.getDepth();
					}

					public int getHeight() {
						return imageHeight;
					}

					public int getWidth() {
						return imageWidth;
					}
					
					public ByteBuffer getImageBufferData() {
						return subBuffer;
					}

					public int getTexHeight() {
						return ySize;
					}

					public int getTexWidth() {
						return xSize;
					}
				};
				images[x][y] = new Image(imgData, filter);
			}
		}
		
		inited = true;
	}
	
	/**
	 * Not supported in BigImage
	 * 
	 * @see org.newdawn.slick.Image#bind()
	 */
	public void bind() {
		throw new OperationNotSupportedException("Can't bind big images yet");
	}

	/**
	 * Not supported in BigImage
	 * 
	 * @see org.newdawn.slick.Image#copy()
	 */
	public Image copy() {
		throw new OperationNotSupportedException("Can't copy big images yet");
	}
	
	/**
	 * @see org.newdawn.slick.Image#draw()
	 */
	public void draw() {
		draw(0,0);
	}

	/**
	 * @see org.newdawn.slick.Image#draw(float, float, org.newdawn.slick.Color)
	 */
	public void draw(float x, float y, Color filter) {
		draw(x,y,width,height,filter);
	}

	/**
	 * @see org.newdawn.slick.Image#draw(float, float, float, org.newdawn.slick.Color)
	 */
	public void draw(float x, float y, float scale, Color filter) {
		draw(x,y,width*scale,height*scale,filter);
	}

	/**
	 * @see org.newdawn.slick.Image#draw(float, float, float, float, org.newdawn.slick.Color)
	 */
	public void draw(float x, float y, float width, float height, Color filter) {
		float sx = width / realWidth;
		float sy = height / realHeight;
		
		GL.glTranslatef(x,y,0);
		GL.glScalef(sx,sy,1);

		float xp = 0;
		float yp = 0;
		
		for (int tx=0;tx<xcount;tx++) {
			yp = 0;
			for (int ty=0;ty<ycount;ty++) {
				Image image = images[tx][ty];

				image.draw(xp,yp,image.getWidth(), image.getHeight(), filter);
			
				yp += image.getHeight();
				if (ty == ycount - 1) {
					xp += image.getWidth();
				}
			}
			
		}
		
		GL.glScalef(1.0f/sx,1.0f/sy,1);
		GL.glTranslatef(-x,-y,0);
	}

	/**
	 * @see org.newdawn.slick.Image#draw(float, float, float, float, float, float, float, float)
	 */
	public void draw(float x, float y, float x2, float y2, float srcx, float srcy, float srcx2, float srcy2) {
		int srcwidth = (int) (srcx2 - srcx);
		int srcheight = (int) (srcy2 - srcy);

		Image subImage = getSubImage((int) srcx,(int) srcy,srcwidth,srcheight);

		int width = (int) (x2 - x);
		int height = (int) (y2 - y);

		subImage.draw(x,y,width,height);
	}

	/**
	 * @see org.newdawn.slick.Image#draw(float, float, float, float, float, float)
	 */
	public void draw(float x, float y, float srcx, float srcy, float srcx2, float srcy2) {
		int srcwidth = (int) (srcx2 - srcx);
		int srcheight = (int) (srcy2 - srcy);

		draw(x,y,srcwidth,srcheight,srcx,srcy,srcx2,srcy2);
	}

	/**
	 * @see org.newdawn.slick.Image#draw(float, float, float, float)
	 */
	public void draw(float x, float y, float width, float height) {
		draw(x,y,width,height,Color.white);
	}

	/**
	 * @see org.newdawn.slick.Image#draw(float, float, float)
	 */
	public void draw(float x, float y, float scale) {
		draw(x,y,scale,Color.white);
	}

	/**
	 * @see org.newdawn.slick.Image#draw(float, float)
	 */
	public void draw(float x, float y) {
		draw(x,y,Color.white);
	}

	/**
	 * @see org.newdawn.slick.Image#drawEmbedded(float, float, float, float)
	 */
	public void drawEmbedded(float x, float y, float width, float height) {
		float sx = width / realWidth;
		float sy = height / realHeight;

		float xp = 0;
		float yp = 0;
		
		for (int tx=0;tx<xcount;tx++) {
			yp = 0;
			for (int ty=0;ty<ycount;ty++) {
				Image image = images[tx][ty];

				if ((lastBind == null) || (image.getTexture() != lastBind.getTexture())) {
					if (lastBind != null) {
						lastBind.endUse();
					}
					lastBind = image;
					lastBind.startUse();
				}
				image.drawEmbedded(xp+x,yp+y,image.getWidth(), image.getHeight());
			
				yp += image.getHeight();
				if (ty == ycount - 1) {
					xp += image.getWidth();
				}
			}
			
		}
	}

	/**
	 * @see org.newdawn.slick.Image#drawFlash(float, float, float, float)
	 */
	public void drawFlash(float x, float y, float width, float height) {
		float sx = width / realWidth;
		float sy = height / realHeight;
		
		GL.glTranslatef(x,y,0);
		GL.glScalef(sx,sy,1);

		float xp = 0;
		float yp = 0;
		
		for (int tx=0;tx<xcount;tx++) {
			yp = 0;
			for (int ty=0;ty<ycount;ty++) {
				Image image = images[tx][ty];

				image.drawFlash(xp,yp,image.getWidth(), image.getHeight());
			
				yp += image.getHeight();
				if (ty == ycount - 1) {
					xp += image.getWidth();
				}
			}
			
		}
		
		GL.glScalef(1.0f/sx,1.0f/sy,1);
		GL.glTranslatef(-x,-y,0);
	}

	/**
	 * @see org.newdawn.slick.Image#drawFlash(float, float)
	 */
	public void drawFlash(float x, float y) {
		drawFlash(x,y,width,height);
	}

	/**
	 * Not supported in BigImage
	 * 
	 * @see org.newdawn.slick.Image#endUse()
	 */
	public void endUse() {
		if (lastBind != null) {
			lastBind.endUse();
		}
		lastBind = null;
	}

	/**
	 * Not supported in BigImage
	 * 
	 * @see org.newdawn.slick.Image#startUse()
	 */
	public void startUse() {
	}
	
	/**
	 * Not supported in BigImage
	 * 
	 * @see org.newdawn.slick.Image#ensureInverted()
	 */
	public void ensureInverted() {
		throw new OperationNotSupportedException("Doesn't make sense for tiled operations");
	}

	/**
	 * Not supported in BigImage
	 * 
	 * @see org.newdawn.slick.Image#getColor(int, int)
	 */
	public Color getColor(int x, int y) {
		throw new OperationNotSupportedException("Can't use big images as buffers");
	}

	/**
	 * @see org.newdawn.slick.Image#getFlippedCopy(boolean, boolean)
	 */
	public Image getFlippedCopy(boolean flipHorizontal, boolean flipVertical) {
		BigImage image = new BigImage();
		
		image.images = images;
		image.xcount = xcount;
		image.ycount = ycount;
		image.width = width;
		image.height = height;
		image.realWidth = realWidth;
		image.realHeight = realHeight;
		
		if (flipHorizontal) {
			Image[][] images = image.images;
			image.images = new Image[xcount][ycount];
			
			for (int x=0;x<xcount;x++) {
				for (int y=0;y<ycount;y++) {
					image.images[x][y] = images[xcount-1-x][y].getFlippedCopy(true, false);
				}
			}
		}
		
		if (flipVertical) {
			Image[][] images = image.images;
			image.images = new Image[xcount][ycount];
			
			for (int x=0;x<xcount;x++) {
				for (int y=0;y<ycount;y++) {
					image.images[x][y] = images[x][ycount-1-y].getFlippedCopy(false, true);
				}
			}
		}
		
		return image;
	}

	/**
	 * Not supported in BigImage
	 * 
	 * @see org.newdawn.slick.Image#getGraphics()
	 */
	public Graphics getGraphics() throws SlickException {
		throw new OperationNotSupportedException("Can't use big images as offscreen buffers");
	}

	/** 
	 * @see org.newdawn.slick.Image#getScaledCopy(float)
	 */
	public Image getScaledCopy(float scale) {
		return getScaledCopy((int) (scale * width), (int) (scale * height));
	}

	/**
	 * @see org.newdawn.slick.Image#getScaledCopy(int, int)
	 */
	public Image getScaledCopy(int width, int height) {
		BigImage image = new BigImage();
		
		image.images = images;
		image.xcount = xcount;
		image.ycount = ycount;
		image.width = width;
		image.height = height;
		image.realWidth = realWidth;
		image.realHeight = realHeight;
		
		return image;
	}

	/**
	 * @see org.newdawn.slick.Image#getSubImage(int, int, int, int)
	 */
	public Image getSubImage(int x, int y, int width, int height) {
		BigImage image = new BigImage();
		
		image.width = width;
		image.height = height;
		image.realWidth = width;
		image.realHeight = height;
		image.images = new Image[this.xcount][this.ycount];

		float xp = 0;
		float yp = 0;
		int x2 = x+width;
		int y2 = y+height;
		
		int startx = 0;
		int starty = 0;
		boolean foundStart = false;
		
		for (int xt=0;xt<xcount;xt++) {
			yp = 0;
			starty = 0;
			foundStart = false;
			for (int yt=0;yt<ycount;yt++) {
				Image current = images[xt][yt];
				
				int xp2 = (int) (xp + current.getWidth());
				int yp2 = (int) (yp + current.getHeight());
				
				// if the top corner of the subimage is inside the area
				// we want or the bottom corrent of the image is, then consider using the
				// image
				
				// this image contributes to the sub image we're attempt to retrieve
				int targetX1 = (int) Math.max(x, xp);
				int targetY1 = (int) Math.max(y, yp);
				int targetX2 = Math.min(x2, xp2);
				int targetY2 = Math.min(y2, yp2);
				
				int targetWidth = targetX2 - targetX1;
				int targetHeight = targetY2 - targetY1;
				
				if ((targetWidth > 0) && (targetHeight > 0)) {
					Image subImage = current.getSubImage((int) (targetX1 - xp), (int) (targetY1 - yp), 
														(targetX2 - targetX1), 
														(targetY2 - targetY1));
					foundStart = true;
					image.images[startx][starty] = subImage;
					starty++;
					image.ycount = Math.max(image.ycount, starty);
				}
				
				yp += current.getHeight();
				if (yt == ycount - 1) {
					xp += current.getWidth();
				}
			}
			if (foundStart) {
				startx++;
				image.xcount++;
			}
		}
		
		return image;
	}

	/**
	 * Not supported in BigImage
	 * 
	 * @see org.newdawn.slick.Image#getTexture()
	 */
	public Texture getTexture() {
		throw new OperationNotSupportedException("Can't use big images as offscreen buffers");
	}

	/**
	 * @see org.newdawn.slick.Image#initImpl()
	 */
	protected void initImpl() {
		throw new OperationNotSupportedException("Can't use big images as offscreen buffers");
	}

	/**
	 * @see org.newdawn.slick.Image#reinit()
	 */
	protected void reinit() {
		throw new OperationNotSupportedException("Can't use big images as offscreen buffers");
	}

	/**
	 * Not supported in BigImage
	 * 
	 * @see org.newdawn.slick.Image#setTexture(org.newdawn.slick.opengl.Texture)
	 */
	public void setTexture(Texture texture) {
		throw new OperationNotSupportedException("Can't use big images as offscreen buffers");
	}
	
	/**
	 * Get a sub-image that builds up this image. Note that the offsets 
	 * used will depend on the maximum texture size on the OpenGL hardware
	 * 
	 * @param offsetX The x position of the image to return
	 * @param offsetY The y position of the image to return
	 * @return The image at the specified offset into the big image
	 */
	public Image getSubImage(int offsetX, int offsetY) {
	     return images[offsetX][offsetY];
	}

	/**
	 * Get a count of the number images that build this image up horizontally
	 * 
	 * @return The number of sub-images across the big image
	 */
	public int getHorizontalImageCount() {
	     return xcount;
	}

	/**
	 * Get a count of the number images that build this image up vertically
	 * 
	 * @return The number of sub-images down the big image
	 */
	public int getVerticalImageCount() {
	    return ycount;
	} 
	
	/**
	 * @see org.newdawn.slick.Image#toString()
	 */
	public String toString() {
		return "[BIG IMAGE]";
	}
	
	/**
	 * Destroy the image and release any native resources. 
	 * Calls on a destroyed image have undefined results
	 */
	public void destroy() throws SlickException {
		for (int tx=0;tx<xcount;tx++) {
			for (int ty=0;ty<ycount;ty++) {
				Image image = images[tx][ty];
				image.destroy();
			}
		}
	}

	/**
	 * @see org.newdawn.slick.Image#draw(float, float, float, float, float, float, float, float, org.newdawn.slick.Color)
	 */
	public void draw(float x, float y, float x2, float y2, float srcx,
			float srcy, float srcx2, float srcy2, Color filter) {	
		int srcwidth = (int) (srcx2 - srcx);
		int srcheight = (int) (srcy2 - srcy);

		Image subImage = getSubImage((int) srcx,(int) srcy,srcwidth,srcheight);

		int width = (int) (x2 - x);
		int height = (int) (y2 - y);

		subImage.draw(x,y,width,height,filter);
	}

	/**
	 * @see org.newdawn.slick.Image#drawCentered(float, float)
	 */
	public void drawCentered(float x, float y) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @see org.newdawn.slick.Image#drawEmbedded(float, float, float, float, float, float, float, float, org.newdawn.slick.Color)
	 */
	public void drawEmbedded(float x, float y, float x2, float y2, float srcx,
			float srcy, float srcx2, float srcy2, Color filter) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @see org.newdawn.slick.Image#drawEmbedded(float, float, float, float, float, float, float, float)
	 */
	public void drawEmbedded(float x, float y, float x2, float y2, float srcx,
			float srcy, float srcx2, float srcy2) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @see org.newdawn.slick.Image#drawFlash(float, float, float, float, org.newdawn.slick.Color)
	 */
	public void drawFlash(float x, float y, float width, float height, Color col) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @see org.newdawn.slick.Image#drawSheared(float, float, float, float)
	 */
	public void drawSheared(float x, float y, float hshear, float vshear) {
		throw new UnsupportedOperationException();
	}
}
