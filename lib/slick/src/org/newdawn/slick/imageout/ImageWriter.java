package org.newdawn.slick.imageout;

import java.io.IOException;
import java.io.OutputStream;

import org.newdawn.slick.Image;

/**
 * The description of any class that can produce data to an output stream reprsenting
 * some image in memory.
 *
 * @author Jon 
 */
public interface ImageWriter {
	/**
	 * Save an Image to an given location
	 * 
	 * @param image The image to be written
	 * @param format The format that this writer is expected to be produced in
	 * @param out The output stream to which the image data should be written
	 * @param writeAlpha True if we should write alpha information to the file
	 * @throws IOException Indicates a failure to write out the image to the specified location
	 */
	void saveImage(Image image, String format, OutputStream out, boolean writeAlpha) throws IOException; 
}
