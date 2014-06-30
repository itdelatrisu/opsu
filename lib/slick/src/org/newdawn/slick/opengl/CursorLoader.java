package org.newdawn.slick.opengl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Cursor;
import org.newdawn.slick.util.Log;
import org.newdawn.slick.util.ResourceLoader;

/**
 * A utility to load cursors (thanks go to Kappa for the animated cursor
 * loader)
 * 
 * @author Kevin Glass
 * @author Kappa-One 
 */
public class CursorLoader {
	/** The single instnace of this loader to exist */
	private static CursorLoader single = new CursorLoader();

	/**
	 * Retrieve the single instance of this loader - convinient huh?
	 * 
	 * @return The single instance of the cursor loader
	 */
	public static CursorLoader get() {
		return single;
	}
    
    /**
     * Create a new cursor loader 
     */
	private CursorLoader() {
	}
	
	/**
	 * Get a cursor based on a image reference on the classpath
	 * 
	 * @param ref The reference to the image to be loaded
	 * @param x The x-coordinate of the cursor hotspot (left -> right)
	 * @param y The y-coordinate of the cursor hotspot (bottom -> top)
	 * @return The create cursor
	 * @throws IOException Indicates a failure to load the image
	 * @throws LWJGLException Indicates a failure to create the hardware cursor
	 */
	public Cursor getCursor(String ref,int x,int y) throws IOException, LWJGLException {
		LoadableImageData imageData = null;
		
		imageData = ImageDataFactory.getImageDataFor(ref);
		imageData.configureEdging(false);
		
		ByteBuffer buf = imageData.loadImage(ResourceLoader.getResourceAsStream(ref), true, true, null);
		for (int i=0;i<buf.limit();i+=4) {
			byte red = buf.get(i);
			byte green = buf.get(i+1);
			byte blue = buf.get(i+2);
			byte alpha = buf.get(i+3);
			
			buf.put(i+2, red);
			buf.put(i+1, green);
			buf.put(i, blue);
			buf.put(i+3, alpha);
		}
		
		try {
			int yspot = imageData.getHeight() - y - 1;
			if (yspot < 0) {
				yspot = 0;
			}
			
			return new Cursor(imageData.getTexWidth(), imageData.getTexHeight(), x, yspot, 1, buf.asIntBuffer(), null);
		} catch (Throwable e) {
			Log.info("Chances are you cursor is too small for this platform");
			throw new LWJGLException(e);
		}
	}

	
	/**
	 * Get a cursor based on a set of image data
	 * 
	 * @param buf The image data (stored in RGBA) to load the cursor from
	 * @param x The x-coordinate of the cursor hotspot (left -> right)
	 * @param y The y-coordinate of the cursor hotspot (bottom -> top)
	 * @param width The width of the image data provided
	 * @param height The height of the image data provided
	 * @return The create cursor
	 * @throws IOException Indicates a failure to load the image
	 * @throws LWJGLException Indicates a failure to create the hardware cursor
	 */
	public Cursor getCursor(ByteBuffer buf,int x,int y,int width,int height) throws IOException, LWJGLException {
		for (int i=0;i<buf.limit();i+=4) {
			byte red = buf.get(i);
			byte green = buf.get(i+1);
			byte blue = buf.get(i+2);
			byte alpha = buf.get(i+3);
			
			buf.put(i+2, red);
			buf.put(i+1, green);
			buf.put(i, blue);
			buf.put(i+3, alpha);
		}
		
		try {
			int yspot = height - y - 1;
			if (yspot < 0) {
				yspot = 0;
			}
			return new Cursor(width,height, x, yspot, 1, buf.asIntBuffer(), null);
		} catch (Throwable e) {
			Log.info("Chances are you cursor is too small for this platform");
			throw new LWJGLException(e);
		}
	}
	
	/**
	 * Get a cursor based on a set of image data
	 * 
	 * @param imageData The data from which the cursor can read it's contents
	 * @param x The x-coordinate of the cursor hotspot (left -> right)
	 * @param y The y-coordinate of the cursor hotspot (bottom -> top)
	 * @return The create cursor
	 * @throws IOException Indicates a failure to load the image
	 * @throws LWJGLException Indicates a failure to create the hardware cursor
	 */
	public Cursor getCursor(ImageData imageData,int x,int y) throws IOException, LWJGLException {
		ByteBuffer buf = imageData.getImageBufferData();
		for (int i=0;i<buf.limit();i+=4) {
			byte red = buf.get(i);
			byte green = buf.get(i+1);
			byte blue = buf.get(i+2);
			byte alpha = buf.get(i+3);
			
			buf.put(i+2, red);
			buf.put(i+1, green);
			buf.put(i, blue);
			buf.put(i+3, alpha);
		}
		
		try {
			int yspot = imageData.getHeight() - y - 1;
			if (yspot < 0) {
				yspot = 0;
			}
			return new Cursor(imageData.getTexWidth(), imageData.getTexHeight(), x, yspot, 1, buf.asIntBuffer(), null);
		} catch (Throwable e) {
			Log.info("Chances are you cursor is too small for this platform");
			throw new LWJGLException(e);
		}
	}
	
	/**
	 * Get a cursor based on a image reference on the classpath. The image 
	 * is assumed to be a set/strip of cursor animation frames running from top to 
	 * bottom.
	 * 
	 * @param ref The reference to the image to be loaded
	 * @param x The x-coordinate of the cursor hotspot (left -> right)
	 * @param y The y-coordinate of the cursor hotspot (bottom -> top)
	 * @param width The x width of the cursor
	 * @param height The y height of the cursor
	 * @param cursorDelays image delays between changing frames in animation
	 * 					
	 * @return The created cursor
	 * @throws IOException Indicates a failure to load the image
	 * @throws LWJGLException Indicates a failure to create the hardware cursor
	 */
	public Cursor getAnimatedCursor(String ref,int x,int y, int width, int height, int[] cursorDelays) throws IOException, LWJGLException {
		IntBuffer cursorDelaysBuffer = ByteBuffer.allocateDirect(cursorDelays.length*4).order(ByteOrder.nativeOrder()).asIntBuffer();
		for (int i=0;i<cursorDelays.length;i++) {
			cursorDelaysBuffer.put(cursorDelays[i]);
		}
		cursorDelaysBuffer.flip();

		LoadableImageData imageData = new TGAImageData();
		ByteBuffer buf = imageData.loadImage(ResourceLoader.getResourceAsStream(ref), false, null);
					
		return new Cursor(width, height, x, y, cursorDelays.length, buf.asIntBuffer(), cursorDelaysBuffer);
	}
}
