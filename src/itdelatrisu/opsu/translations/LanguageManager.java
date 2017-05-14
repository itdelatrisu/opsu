/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014-2017 Jeffrey Han
 *
 * opsu! is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opsu! is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with opsu!.  If not, see <http://www.gnu.org/licenses/>.
 */

package itdelatrisu.opsu.translations;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import org.newdawn.slick.util.Log;

import craterstudio.io.Streams;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.options.Options.GameOption;

/**
 * Provides basic translation capabilities to opsu!
 * 
 * @author Lyonlancer5
 */
public final class LanguageManager {
	
	// TODO: Bidirectional language support

	/**
	 * A list of languages that can be used to translate opsu!
	 */
	public static List<LanguageManager> translationIndices;
	
	/**
	 * An array of String values that represent the identifiers of each LanguageManager,
	 * used in {@link GameOption#getValueString()}
	 */
	public static String[] translationIds;
	
    
    /**
     * The active locale being used to translate opsu!<br>
     */
    public static LanguageManager currentLocale;
    
    /**
     * The index selected in the configuration panel, used in conjunction with {@link #currentLocale} 
     */
    public static int currentLocaleIndex;
    
	/**
     * Pattern that matches numeric variable placeholders in a resource string, such as "<code>%d</code>", "<code>%3$d</code>" and "<code>%.2f</code>"
     */
    private static final Pattern NUMERIC_VARIABLE_PATTERN = Pattern.compile("%(\\d+\\$)?[\\d\\.]*[df]");

	/**
	 * The hardcoded instance of the translation framework that uses English (en_US)
	 */
    private static LanguageManager defaultInstance;
    
    /**
     * A mapping of all available translations <code>(key -> value)</code>
     */
    private final HashMap<String, String> keys;
    
    /**
     * The identifier used by this instance of LanguageManager
     */
    private final String identifier;

    
    /**
     * Generalized constructor for the translator using input streams
     * 
     * @param stream A stream that provides data regarding the translation keys
     * @param identifier An ID to identify this language manager
     */
    private LanguageManager(InputStream stream, String identifier){
    	this.keys = new HashMap<>();
    	this.identifier = identifier;
    	
    	BufferedReader reader = null;
        try
        {
        	//TODO: Do we use UTF-8 or UCS-2/UTF-16?
            reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null ) {
                if(!line.isEmpty() && line.charAt(0) != 35){
                	String[] values = line.split("=", 2);
                	this.keys.put(values[0], NUMERIC_VARIABLE_PATTERN.matcher(values[1]).replaceAll("%$1s"));
                }
            }
        }
        catch (IOException var7)
        {
        	Log.warn("Could not load translation keys", var7);
        }
        finally
        {
        	Streams.safeClose(reader);
        }
    }
    
    /**
     * Constructor for language files that can be found outside the JAR
     * 
     * @param langFile The reference to the external language file
     */
    public LanguageManager(File langFile) throws FileNotFoundException {
    	this(new FileInputStream(langFile), langFile.getName());
    }
    
    /**
     * Constructor for language files found inside the JAR
     * 
     * @param resourceLocation A textual representation of the location of the JAR resource
     */
    public LanguageManager(String resourceLocation)
    {
    	this(LanguageManager.class.getResourceAsStream(resourceLocation),
    			resourceLocation.substring(resourceLocation.lastIndexOf('/'), resourceLocation.length() - 5));
    }
    
    public static void setup(){
    	if(translationIndices != null || translationIds != null || defaultInstance != null) return;
    	
    	List<LanguageManager> indexed = new ArrayList<>();
    	if(Utils.isJarRunning()){
    		final Pattern langFileExtension = Pattern.compile("(.+).(LANG|lang)$");
    		
    		JarFile opsu = Utils.getJarFile();
    		Enumeration<JarEntry> entries = opsu.entries();
    		while(entries.hasMoreElements()){
    			JarEntry entry = entries.nextElement();
    			String entryName = entry.getName();
    			
    			if(langFileExtension.matcher(entryName).matches()){
    				indexed.add(new LanguageManager(entryName));
    				if(entryName.contains("English US.lang")){
    					currentLocaleIndex = indexed.size() - 1;
    					defaultInstance = indexed.get(currentLocaleIndex);
    					currentLocale = defaultInstance;
    				}
    				Log.info("Added language file: " + entryName);
    			}
    		}
    		
    		try{
    			opsu.close();
    		} catch (IOException e) {
    			//ignore
    		}
    	}
    	
    	translationIndices = Collections.unmodifiableList(indexed);
    	translationIds = new String[translationIndices.size()];
    	
    	for(int a = 0; a < translationIds.length; a++){
    		translationIds[a] = translationIndices.get(a).toString();
    	}
    }
    
    /**
     * Set the current locale as another LanguageManager instance based on the identifier
     * @param identifier The name of the language
     */
    public static void setLocaleFrom(String identifier){
    	if(identifier == null) return;
    	
    	for(int a = 0; a < translationIds.length; a++){
    		if(translationIds[a].equals(identifier)){
    			currentLocale = translationIndices.get(a);
    			return;
    		}
    	}
    	
    	currentLocale = defaultInstance;
    	currentLocaleIndex = translationIndices.indexOf(currentLocale);
    }
    
    /**
     * Convenience method for updating the locale with the given index
     * 
     * @param index The index that denotes the position of the translation in the options
     */
    public static void updateLocale(int index){
    	if(index < 0) return;
    	
    	currentLocaleIndex = index;
    	currentLocale = translationIndices.get(currentLocaleIndex);
    }

    /**
     * Translate a key to the current language without formatting
     * @param key The ID of the translation string
     * 
     * @return A translation of the given key or the key itself when a translation does not exist.
     */
    public String translateKey(String key)
    {
        String s = this.keys.get(key);
        
        //Always fall back to English if no translation key is found in another language
        if(s == null && this != defaultInstance){
        	return defaultInstance.translateKey(key);
        }
        
        return s == null ? key : s;
    }

    /**
     * Translate a key to the current language and applies {@link String#format(String, Object...)}
     * @param key The ID of the translation string
     * @param format Additional formatting objects to be used
     * 
     * @return A translation of the given key as formatted by the given objects
     * or the key itself when a translation does not exist.
     * 
     * @see String#format(String, Object...)
     */
    public String translateKeyFormatted(String key, Object... format)
    {
        String s = this.translateKey(key);
        
        //This means the translation does not exist
        if(s.equals(key)) return key;
        
        try
        {
            return String.format(s, format);
        }
        catch (IllegalFormatException var5)
        {
        	Log.warn("Format error: " + s, var5);
            return key;
        }
    }

    /**
     * Checks whether the given key has a translation that can be found
     * 
     * @return True if a translation exists, false otherwise
     */
    public boolean canTranslate(String key)
    {
        return this.keys.containsKey(key);
    }
    
    
    /**
     * Return the identifier of the current translator
     * @return The name of the language "file"
     */
    public String toString(){
    	return identifier;
    }
    
    @Override
    public boolean equals(Object obj) {
    	if(obj instanceof LanguageManager){
    		return ((LanguageManager) obj).identifier.equals(identifier)
    				&& obj.hashCode() == this.hashCode();
    	}
    	
    	return false;
    }
    
    @Override
    public int hashCode() {
    	return identifier.hashCode() + keys.hashCode();
    }
    
}
