package org.newdawn.slick.opengl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * An image data source that can load images from a stream
 *
 * @author kevin
 */
public interface LoadableImageData extends ImageData {
	/**
	 * Configure the edging that can be used to make texture edges
	 * loop more cleanly
	 * 
	 * @param edging True if we should edge
	 */
	public void configureEdging(boolean edging);
	
	/**
	 * Load a image from the specified stream
	 * 
	 * @param fis The stream from which we'll load the TGA
	 * @throws IOException Indicates a failure to read the TGA
	 * @return The byte buffer containing texture data
	 */
	public ByteBuffer loadImage(InputStream fis) throws IOException;

	/**
	 * Load a image from the specified stream
	 * 
	 * @param fis The stream from which we'll load the TGA
	 * @param flipped True if we loading in flipped mode (used for cursors)
	 * @param transparent The colour to interpret as transparent or null if none
	 * @return The byte buffer containing texture data
	 * @throws IOException Indicates a failure to read the TGA
	 */
	public ByteBuffer loadImage(InputStream fis, boolean flipped, int[] transparent)
			throws IOException;
	
	/**
	 * Load a image from the specified stream
	 * 
	 * @param fis The stream from which we'll load the TGA
	 * @param flipped True if we loading in flipped mode (used for cursors)
	 * @param forceAlpha Force the output to have an alpha channel
	 * @param transparent The colour to interpret as transparent or null if none
	 * @return The byte buffer containing texture data
	 * @throws IOException Indicates a failure to read the TGA
	 */
	public ByteBuffer loadImage(InputStream fis, boolean flipped, boolean forceAlpha, int[] transparent)
			throws IOException;
}
