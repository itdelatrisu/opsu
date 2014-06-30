package org.newdawn.slick.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * A resource loading location that searches somewhere on the classpath
 * 
 * @author kevin
 */
public class FileSystemLocation implements ResourceLocation {
	/** The root of the file system to search */
	private File root;
	
	/**
	 * Create a new resoruce location based on the file system
	 * 
	 * @param root The root of the file system to search
	 */
	public FileSystemLocation(File root) {
		this.root = root;
	}
	
	/**
	 * @see ResourceLocation#getResource(String)
	 */
	public URL getResource(String ref) {
		try {
			File file = new File(root, ref);
			if (!file.exists()) {
				file = new File(ref);
			}
			if (!file.exists()) {
				return null;
			}
			
			return file.toURI().toURL();
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * @see ResourceLocation#getResourceAsStream(String)
	 */
	public InputStream getResourceAsStream(String ref) {
		try {
			File file = new File(root, ref);
			if (!file.exists()) {
				file = new File(ref);
			}
			return new FileInputStream(file);
		} catch (IOException e) {
			return null;
		}
	}

}
