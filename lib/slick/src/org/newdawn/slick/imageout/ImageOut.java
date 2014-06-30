package org.newdawn.slick.imageout;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;

/**
 * A static hook to access all the Image output utilities. The list of format strings 
 * provided is not the limit of capability. These are provided for utility, use @see {@link #getSupportedFormats()}
 * for a full list of supported formats.
 *
 * @author kevin
 */
public class ImageOut {
	/** The default setting for writing out the alpha channel */
	private static final boolean  DEFAULT_ALPHA_WRITE = false;
	
	/** The format string for TGA */
	public static String TGA = "tga";
	/** The format string for PNG */
	public static String PNG = "png";
	/** The format string for JPG */
	public static String JPG = "jpg";
	
	/**
	 * Get a list of supported formats
	 * 
	 * @see ImageWriterFactory#getSupportedFormats()
	 * @return The list of supported format strings
	 */
	public static String[] getSupportedFormats() {
		return ImageWriterFactory.getSupportedFormats();
	}

	/**
	 * Write an image out to a specified output stream
	 * 
	 * @param image The image to write out to
	 * @param format The format to write the image out in
	 * @param out The output stream to which the image should be written
	 * @throws SlickException Indicates a failure to write the image in the specified format
	 */
	public static void write(Image image, String format, OutputStream out) throws SlickException {
		write(image, format, out, DEFAULT_ALPHA_WRITE);
	}
	
	/**
	 * Write an image out to a specified output stream
	 * 
	 * @param image The image to write out to
	 * @param format The format to write the image out in
	 * @param out The output stream to which the image should be written
	 * @param writeAlpha True if we should write the alpha channel out (some formats don't support this, like JPG)
	 * @throws SlickException Indicates a failure to write the image in the specified format
	 */
	public static void write(Image image, String format, OutputStream out, boolean writeAlpha) throws SlickException {
		try {
			ImageWriter writer = ImageWriterFactory.getWriterForFormat(format);
			writer.saveImage(image, format, out, writeAlpha);
		} catch (IOException e) {
			throw new SlickException("Unable to write out the image in format: "+format, e);
		}
	}

	/**
	 * Write an image out to a file on the local file system. The format of the output
	 * is determined based on the file name extension
	 * 
	 * @param image The image to be written out
	 * @param dest The destination path to write to
	 * @throws SlickException Indicates a failure to write the image in the determined format
	 */
	public static void write(Image image, String dest) throws SlickException {
		write(image, dest, DEFAULT_ALPHA_WRITE);
	}
	
	/**
	 * Write an image out to a file on the local file system. The format of the output
	 * is determined based on the file name extension
	 * 
	 * @param image The image to be written out
	 * @param dest The destination path to write to
	 * @param writeAlpha True if we should write the alpha channel out (some formats don't support this, like JPG)
	 * @throws SlickException Indicates a failure to write the image in the determined format
	 */
	public static void write(Image image, String dest, boolean writeAlpha) throws SlickException {
		try {
			int ext = dest.lastIndexOf('.');
			if (ext < 0) {
				throw new SlickException("Unable to determine format from: "+dest);
			}
			
			String format = dest.substring(ext+1);
			write(image, format, new FileOutputStream(dest), writeAlpha);
		} catch (IOException e) {
			throw new SlickException("Unable to write to the destination: "+dest, e);
		}
	}
	
	/**
	 * Write an image out to a file on the local file system. 
	 * 
	 * @param image The image to be written out
	 * @param format The format to write the image out in
	 * @param dest The destination path to write to
	 * @throws SlickException Indicates a failure to write the image in the determined format
	 */
	public static void write(Image image, String format, String dest) throws SlickException {
		write(image, format, dest, DEFAULT_ALPHA_WRITE);
	}
	
	/**
	 * Write an image out to a file on the local file system. 
	 * 
	 * @param image The image to be written out
	 * @param format The format to write the image out in
	 * @param dest The destination path to write to
	 * @param writeAlpha True if we should write the alpha channel out (some formats don't support this, like JPG)
	 * @throws SlickException Indicates a failure to write the image in the determined format
	 */
	public static void write(Image image, String format, String dest, boolean writeAlpha) throws SlickException {
		try {
			write(image, format, new FileOutputStream(dest), writeAlpha);
		} catch (IOException e) {
			throw new SlickException("Unable to write to the destination: "+dest, e);
		}
	}
}
