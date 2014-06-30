package org.newdawn.slick.muffin;

import java.io.IOException;
import java.util.HashMap;

/**
 * A description of any class with the ability to store state locally
 *
 * @author kappaOne
 */
public interface Muffin {
	/**
	 * Save a file of data
	 *  
	 * @param data The data to store
	 * @param fileName The name of the file to store it against
	 * @throws IOException Indicates a failure to save the state 
	 */
	public abstract void saveFile(HashMap data, String fileName) throws IOException;
	
	/**
	 * Load a file of data from the store
	 * 
	 * @param fileName The name of the file to retrieve
	 * @return The data retrieved
	 * @throws IOException Indicates a failure to load the state 
	 */
	public abstract HashMap loadFile(String fileName) throws IOException;
}
