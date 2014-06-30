package org.newdawn.slick.opengl;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.lwjgl.BufferUtils;

/**
 * A utility to load TGAs. Note: NOT THREAD SAFE
 * 
 * Fresh cut of code but largely influeneced by the TGA loading class
 * provided as part of the Java Monkey Engine (JME). Why not check out 
 * what they're doing over at http://www.jmonkeyengine.com. kudos to 
 * Mark Powell.
 * 
 * @author Kevin Glass
 */
public class TGAImageData implements LoadableImageData {
	/** The width of the texture that needs to be generated */
	private int texWidth;
	/** The height of the texture that needs to be generated */
	private int texHeight;
	/** The width of the TGA image */
	private int width;
	/** The height of the TGA image */
	private int height;
	/** The bit depth of the image */
	private short pixelDepth;

	/**
	 * Create a new TGA Loader
	 */
	public TGAImageData() {
	}

	/**
	 * Flip the endian-ness of the short
	 * 
	 * @param signedShort The short to flip
	 * @return The flipped short
	 */
	private short flipEndian(short signedShort) {
		int input = signedShort & 0xFFFF;
		return (short) (input << 8 | (input & 0xFF00) >>> 8);
	}
	
	/**
	 * @see org.newdawn.slick.opengl.ImageData#getDepth()
	 */
	public int getDepth() {
		return pixelDepth;
	}
	
	/**
	 * @see org.newdawn.slick.opengl.ImageData#getWidth()
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * @see org.newdawn.slick.opengl.ImageData#getHeight()
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * @see org.newdawn.slick.opengl.ImageData#getTexWidth()
	 */
	public int getTexWidth() {
		return texWidth;
	}

	/**
	 * @see org.newdawn.slick.opengl.ImageData#getTexHeight()
	 */
	public int getTexHeight() {
		return texHeight;
	}
	
	/**
	 * @see org.newdawn.slick.opengl.LoadableImageData#loadImage(java.io.InputStream)
	 */
	public ByteBuffer loadImage(InputStream fis) throws IOException {
		return loadImage(fis,true, null);
	}
	
	/**
	 * @see org.newdawn.slick.opengl.LoadableImageData#loadImage(java.io.InputStream, boolean, int[])
	 */
	public ByteBuffer loadImage(InputStream fis, boolean flipped, int[] transparent) throws IOException {
		return loadImage(fis, flipped, false, transparent);
	}
	
	/**
	 * @see org.newdawn.slick.opengl.LoadableImageData#loadImage(java.io.InputStream, boolean, boolean, int[])
	 */
	public ByteBuffer loadImage(InputStream fis, boolean flipped, boolean forceAlpha, int[] transparent) throws IOException {
		if (transparent != null) { 
			forceAlpha = true;
		}
		byte red = 0;
		byte green = 0;
		byte blue = 0;
		byte alpha = 0;
		
		BufferedInputStream bis = new BufferedInputStream(fis, 100000);
		DataInputStream dis = new DataInputStream(bis);
		
		// Read in the Header
		short idLength = (short) dis.read();
		short colorMapType = (short) dis.read();
		short imageType = (short) dis.read();
		short cMapStart = flipEndian(dis.readShort());
		short cMapLength = flipEndian(dis.readShort());
		short cMapDepth = (short) dis.read();
		short xOffset = flipEndian(dis.readShort());
		short yOffset = flipEndian(dis.readShort());
		
		if (imageType != 2) {
			throw new IOException("Slick only supports uncompressed RGB(A) TGA images");
		}
		
		width = flipEndian(dis.readShort());
		height = flipEndian(dis.readShort());
		pixelDepth = (short) dis.read();
		if (pixelDepth == 32) {
			forceAlpha = false;
		}
		
		texWidth = get2Fold(width);
		texHeight = get2Fold(height);
		
		short imageDescriptor = (short) dis.read();
		if ((imageDescriptor & 0x0020) == 0) {
		   flipped = !flipped;
		} 
		
		// Skip image ID
		if (idLength > 0) {
			bis.skip(idLength);
		}
		
		byte[] rawData = null;
		if ((pixelDepth == 32) || (forceAlpha)) {
			pixelDepth = 32;
			rawData = new byte[texWidth * texHeight * 4];
		} else if (pixelDepth == 24) {
			rawData = new byte[texWidth * texHeight * 3];
		} else {
			throw new RuntimeException("Only 24 and 32 bit TGAs are supported");
		}
		
		if (pixelDepth == 24) {
			if (flipped) {
				for (int i = height-1; i >= 0; i--) {
					for (int j = 0; j < width; j++) {
						blue = dis.readByte();
						green = dis.readByte();
						red = dis.readByte();
						
						int ofs = ((j + (i * texWidth)) * 3);
						rawData[ofs] = red;
						rawData[ofs + 1] = green;
						rawData[ofs + 2] = blue;
					}
				}
			} else {
				for (int i = 0; i < height; i++) {
					for (int j = 0; j < width; j++) {
						blue = dis.readByte();
						green = dis.readByte();
						red = dis.readByte();
						
						int ofs = ((j + (i * texWidth)) * 3);
						rawData[ofs] = red;
						rawData[ofs + 1] = green;
						rawData[ofs + 2] = blue;
					}
				}
			}
		} else if (pixelDepth == 32) {
			if (flipped) {
				for (int i = height-1; i >= 0; i--) {
					for (int j = 0; j < width; j++) {
						blue = dis.readByte();
						green = dis.readByte();
						red = dis.readByte();
						if (forceAlpha) {
							alpha = (byte) 255;
						} else {
							alpha = dis.readByte();
						}
						
						int ofs = ((j + (i * texWidth)) * 4);
						
						rawData[ofs] = red;
						rawData[ofs + 1] = green;
						rawData[ofs + 2] = blue;
						rawData[ofs + 3] = alpha;
						
						if (alpha == 0) {
							rawData[ofs + 2] = (byte) 0;
							rawData[ofs + 1] = (byte) 0;
							rawData[ofs] = (byte) 0;
						}
					}
				}
			} else {
				for (int i = 0; i < height; i++) {
					for (int j = 0; j < width; j++) {
						blue = dis.readByte();
						green = dis.readByte();
						red = dis.readByte();
						if (forceAlpha) {
							alpha = (byte) 255;
						} else {
							alpha = dis.readByte();
						}
						
						int ofs = ((j + (i * texWidth)) * 4);
						
						if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
							rawData[ofs] = red;
							rawData[ofs + 1] = green;
							rawData[ofs + 2] = blue;
							rawData[ofs + 3] = alpha;
						} else {
							rawData[ofs] = red;
							rawData[ofs + 1] = green;
							rawData[ofs + 2] = blue;
							rawData[ofs + 3] = alpha;
						}
						
						if (alpha == 0) {
							rawData[ofs + 2] = 0;
							rawData[ofs + 1] = 0;
							rawData[ofs] = 0;
						}
					}
				}
			}
		}
		fis.close();
		
		if (transparent != null) {
	        for (int i=0;i<rawData.length;i+=4) {
	        	boolean match = true;
	        	for (int c=0;c<3;c++) {
	        		if (rawData[i+c] != transparent[c]) {
	        			match = false;
	        		}
	        	}
	  
	        	if (match) {
	        		rawData[i+3] = 0;
	           	}
	        }
        }
		
		// Get a pointer to the image memory
		ByteBuffer scratch = BufferUtils.createByteBuffer(rawData.length);
		scratch.put(rawData);
		
		int perPixel = pixelDepth / 8;
		if (height < texHeight-1) {
			int topOffset = (texHeight-1) * (texWidth*perPixel);
			int bottomOffset = (height-1) * (texWidth*perPixel);
			for (int x=0;x<texWidth*perPixel;x++) {
				scratch.put(topOffset+x, scratch.get(x));
				scratch.put(bottomOffset+(texWidth*perPixel)+x, scratch.get((texWidth*perPixel)+x));
			}
		}
		if (width < texWidth-1) {
			for (int y=0;y<texHeight;y++) {
				for (int i=0;i<perPixel;i++) {
					scratch.put(((y+1)*(texWidth*perPixel))-perPixel+i, scratch.get(y*(texWidth*perPixel)+i));
					scratch.put((y*(texWidth*perPixel))+(width*perPixel)+i, scratch.get((y*(texWidth*perPixel))+((width-1)*perPixel)+i));
				}
			}
		}

		scratch.flip();

		return scratch;
	}

    /**
     * Get the closest greater power of 2 to the fold number
     * 
     * @param fold The target number
     * @return The power of 2
     */
    private int get2Fold(int fold) {
        int ret = 2;
        while (ret < fold) {
            ret *= 2;
        }
        return ret;
    }

	/**
	 * @see org.newdawn.slick.opengl.ImageData#getImageBufferData()
	 */
	public ByteBuffer getImageBufferData() {
		throw new RuntimeException("TGAImageData doesn't store it's image.");
	}
	
	/**
	 * @see org.newdawn.slick.opengl.LoadableImageData#configureEdging(boolean)
	 */
	public void configureEdging(boolean edging) {
	}
}
