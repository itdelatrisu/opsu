package org.newdawn.slick.muffin;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import javax.jnlp.BasicService;
import javax.jnlp.FileContents;
import javax.jnlp.PersistenceService;
import javax.jnlp.ServiceManager;

import org.newdawn.slick.util.Log;

/**
 * A muffin load/save implementation based on using Webstart Muffins (a bit like cookies only 
 * for webstart)
 * 
 * @author kappaOne
 */
public class WebstartMuffin implements Muffin {

	/**
	 * @see org.newdawn.slick.muffin.Muffin#saveFile(java.util.HashMap, java.lang.String)
	 */
	public void saveFile(HashMap scoreMap, String fileName) throws IOException {

		PersistenceService ps;
		BasicService bs;
		URL configURL;

		try {
			ps = (PersistenceService) ServiceManager
					.lookup("javax.jnlp.PersistenceService");
			bs = (BasicService) ServiceManager
					.lookup("javax.jnlp.BasicService");
			URL baseURL = bs.getCodeBase();
			// System.out.println("CodeBase was " + baseURL);
			configURL = new URL(baseURL, fileName);
		} catch (Exception e) {
			Log.error(e);
			throw new IOException("Failed to save state: ");
		}

		try {
			ps.delete(configURL);
		} catch (Exception e) {
			Log.info("No exisiting Muffin Found - First Save");
		}
		
		try {
			ps.create(configURL, 1024); // 1024 bytes for our data

			FileContents fc = ps.get(configURL);
			DataOutputStream oos = new DataOutputStream(fc
					.getOutputStream(false));

			// scroll through hashMap and write key and value to file
			Set keys = scoreMap.keySet(); // get the keys

			// get values using keys
			for (Iterator i = keys.iterator(); i.hasNext();) {
				String key = (String) i.next();

				oos.writeUTF(key);

				if (fileName.endsWith("Number")) {
					double value = ((Double) scoreMap.get(key)).doubleValue();
					oos.writeDouble(value);
				} else {
					String value = (String) scoreMap.get(key);
					oos.writeUTF(value);
				}
			}

			oos.flush();
			oos.close();
		} catch (Exception e) {
			Log.error(e);
			throw new IOException("Failed to store map of state data");
		}
	}

	/**
	 * @see org.newdawn.slick.muffin.Muffin#loadFile(java.lang.String)
	 */
	public HashMap loadFile(String fileName) throws IOException {
		HashMap hashMap = new HashMap();

		try {
			PersistenceService ps = (PersistenceService) ServiceManager
					.lookup("javax.jnlp.PersistenceService");
			BasicService bs = (BasicService) ServiceManager
					.lookup("javax.jnlp.BasicService");
			URL baseURL = bs.getCodeBase();
			URL configURL = new URL(baseURL, fileName);
			FileContents fc = ps.get(configURL);
			DataInputStream ois = new DataInputStream(fc.getInputStream());

			// read in data from muffin
			String key;

			// load hashMap as <String, Int> or <String, String>
			if (fileName.endsWith("Number")) {
				double value;
				// while not end of file
				while ((key = ois.readUTF()) != null) {
					value = ois.readDouble();
					// load value into hashMap
					hashMap.put(key, new Double(value));
				}
			} else {
				String value;
				// while not end of file
				while ((key = ois.readUTF()) != null) {
					value = ois.readUTF();
					// load value into hashMap
					hashMap.put(key, value);
				}
			}

			ois.close();
		} catch (EOFException e) {
			// End of the file reached, do nothing
		} catch (IOException e) {
			// No data there - thats ok, just not saved before
		} catch (Exception e) {
			Log.error(e);
			throw new IOException("Failed to load state from webstart muffin");
		}

		return hashMap;
	}
}
