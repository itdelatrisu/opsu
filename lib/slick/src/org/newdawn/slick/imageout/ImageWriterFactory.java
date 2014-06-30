package org.newdawn.slick.imageout;

import java.util.HashMap;

import javax.imageio.ImageIO;

import org.newdawn.slick.SlickException;

/**
 * A factory to produce image writers based on format names
 *
 * @author kevin
 */
public class ImageWriterFactory {
	/** The map from format names to image writer instances */
	private static HashMap writers = new HashMap();
	
	// Initialise the list of writers based on the classes we know about
	static {
		String[] formats = ImageIO.getWriterFormatNames();
		ImageIOWriter writer = new ImageIOWriter();
		for (int i=0;i<formats.length;i++) {
			registerWriter(formats[i], writer);
		}
		
		TGAWriter tga = new TGAWriter();
		registerWriter("tga", tga);
	}
	
	/**
	 * Register an image writer with the factory. This will allow users 
	 * to use it to write out the explicit format
	 * 
	 * @param format The format (usually extension) of the files that will be written out
	 * @param writer The writer to use for the given format
	 */
	public static void registerWriter(String format, ImageWriter writer) {
		writers.put(format, writer);
	}
	
	/**
	 * Get the list of support format strings for this factory
	 * 
	 * @return The list of support format strings for this factory
	 */
	public static String[] getSupportedFormats() {
		return (String[]) writers.keySet().toArray(new String[0]);
	}
	
	/**
	 * Get a Slick image writer for the given format
	 *  
	 * @param format The format of the image to write
	 * @return The image write to use to produce these images
	 * @throws SlickException
	 */
	public static ImageWriter getWriterForFormat(String format) throws SlickException
	{
		ImageWriter writer = (ImageWriter) writers.get(format);
		if (writer != null) {
			return writer;
		}
		
		writer = (ImageWriter) writers.get(format.toLowerCase());
		if (writer != null) {
			return writer;
		}
		
		writer = (ImageWriter) writers.get(format.toUpperCase());
		if (writer != null) {
			return writer;
		}
		
		throw new SlickException("No image writer available for: "+format);
	}
}
