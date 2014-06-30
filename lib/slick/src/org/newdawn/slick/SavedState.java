package org.newdawn.slick;

import java.io.IOException;
import java.util.HashMap;

import javax.jnlp.ServiceManager;

import org.newdawn.slick.muffin.FileMuffin;
import org.newdawn.slick.muffin.Muffin;
import org.newdawn.slick.muffin.WebstartMuffin;
import org.newdawn.slick.util.Log;

/**
 * A utility to allow game setup/state to be stored locally. This utility will adapt to the
 * current enviornment (webstart or file based). Note that this will not currently
 * work in an applet.
 * 
 * @author kappaOne
 */
public class SavedState {
	/** file name of where the scores will be saved */
	private String fileName;
	/** Type of Muffin to use */
	private Muffin muffin;
	/** hash map where int data will be stored */
	private HashMap numericData = new HashMap();
	/** hash map where string data will be stored */
	private HashMap stringData = new HashMap();
	
	/**
	 * Create and Test to see if the app is running 
	 * as webstart or local app and select the appropriate 
	 * muffin type
	 * 
	 * @param fileName name of muffin where data will be saved
	 * @throws SlickException Indicates a failure to load the stored state
	 */
	public SavedState(String fileName) throws SlickException {
		this.fileName = fileName;
		
		if (isWebstartAvailable()) {
			muffin = new WebstartMuffin();
		}
		else {
			muffin = new FileMuffin();
		}
		
		try {
			load();
		} catch (IOException e) {
			throw new SlickException("Failed to load state on startup",e);
		}
	}	

	/**
	 * Get number stored at given location
	 * 
	 * @param nameOfField The name of the number to retrieve
	 * @return The number saved at this location
	 */
	public double getNumber(String nameOfField) {
		return getNumber(nameOfField, 0);
	}
	
	/**
	 * Get number stored at given location
	 * 
	 * @param nameOfField The name of the number to retrieve
	 * @param defaultValue The value to return if the specified value hasn't been set
	 * @return The number saved at this location
	 */
	public double getNumber(String nameOfField, double defaultValue) {
		Double value = ((Double)numericData.get(nameOfField));
		
		if (value == null) {
			return defaultValue;
		}
		
		return value.doubleValue();
	}
	
	/**
	 * Save the given value at the given location
	 * will overwrite any previous value at this location
	 * 
	 * @param nameOfField The name to store the value against
	 * @param value The value to store
	 */
	public void setNumber(String nameOfField, double value){
		numericData.put(nameOfField, new Double(value));
	}

	/**
	 * Get the String at the given location
	 * 
	 * @param nameOfField location of string
	 * @return String stored at the location given
	 */
	public String getString(String nameOfField) {
		return getString(nameOfField, null);
	}
	
	/**
	 * Get the String at the given location
	 * 
	 * @param nameOfField location of string
	 * @param defaultValue The value to return if the specified value hasn't been set
	 * @return String stored at the location given
	 */
	public String getString(String nameOfField, String defaultValue) {
		String value = (String) stringData.get(nameOfField);
		
		if (value == null) {
			return defaultValue;
		}
		
		return value;
	}
	
	/**
	 * Save the given value at the given location
	 * will overwrite any previous value at this location
	 * 
	 * @param nameOfField location to store int
	 * @param value The value to store
	 */
	public void setString(String nameOfField, String value){
		stringData.put(nameOfField, value);
	}
	
	/**
	 * Save the stored data to file/muffin
	 * 
	 * @throws IOException Indicates it wasn't possible to store the state
	 */
	public void save() throws IOException {
		muffin.saveFile(numericData, fileName + "_Number");
		muffin.saveFile(stringData, fileName + "_String");
	}
	
	/**
	 * Load the data from file/muffin
	 * 
	 * @throws IOException Indicates it wasn't possible to load the state
	 */
	public void load() throws IOException {
		numericData = muffin.loadFile(fileName + "_Number");
		stringData = muffin.loadFile(fileName + "_String");
	}
	
	/**
	 * Will delete all current data held in Score
	 */
	public void clear() {
		numericData.clear();
		stringData.clear();
	}
	
	/**
	 * Quick test to see if running through Java webstart
	 * 
	 * @return True if jws running
	 */
	private boolean isWebstartAvailable() {
		try {
			Class.forName("javax.jnlp.ServiceManager");
			// this causes to go and see if the service is available
			ServiceManager.lookup("javax.jnlp.PersistenceService");
			Log.info("Webstart detected using Muffins");
		} catch (Exception e) {
			Log.info("Using Local File System");
			return false;
		}
		return true;
	}
}