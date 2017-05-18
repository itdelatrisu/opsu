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

import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.options.Options.GameOption;

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
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import org.newdawn.slick.util.Log;

import craterstudio.io.Streams;

/**
 * Provides translation capabilities to opsu!
 * @author Lyonlancer5
 */
public final class LocaleManager {
	
	/**
     * Pattern that matches numeric variable placeholders in a resource string, such as "<code>%d</code>", "<code>%3$d</code>" and "<code>%.2f</code>"
     */
    private static final Pattern NUMERIC_VARIABLE_PATTERN = Pattern.compile("%(\\d+\\$)?[\\d\\.]*[df]");
    
    /** Used to synchronize between threads */
    private static final Object LOCK = new Object();
	
	//List variables used by Options
	/**
	 * An array of String values that represent the identifiers of each LanguageManager,
	 * used in {@link GameOption#getValueString()}
	 */
	public static String[] translationIds;
	
	/**
	 * A list of languages that can be used to translate opsu!
	 */
	private static List<LocaleManager> translationIndices;
	
	
	//Variables that define the current locale
    /**
     * The active locale being used to translate opsu!<br>
     */
	private static LocaleManager currentLocale;
    
    /**
     * The index selected in the configuration panel, used in conjunction with {@link #currentLocale} 
     */
	private static int currentLocaleIndex;
    
	/**
	 * The hardcoded instance of the translation framework that uses English (en_US)
	 */
    private static LocaleManager defaultLocale;
    
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
     * @param fallbackId An ID to identify this language manager
     */
    private LocaleManager(InputStream stream, String fallbackId){
    	this.keys = new HashMap<>();
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
        
        String v0 = this.keys.get("language.name");
        this.identifier = v0 == null ? fallbackId : v0;
    }
    
    /**
     * Constructor for language files that can be found outside the JAR
     * 
     * @param langFile The reference to the external language file
     */
    public LocaleManager(File langFile) throws FileNotFoundException {
    	this(new FileInputStream(langFile), langFile.getName().substring(0, langFile.getName().length() - 5));
    }
    
    /**
     * Constructor for language files found inside the JAR
     * 
     * @param resourceLocation A textual representation of the location of the JAR resource
     */
    public LocaleManager(String resourceLocation)
    {
    	this(LocaleManager.class.getResourceAsStream(resourceLocation),
    			resourceLocation.substring(resourceLocation.lastIndexOf('/'), resourceLocation.length() - 5));
    }
    
    /**
     * Loads the language manager's assets.
     * This method can safely be called multiple times as it will simply
     * reload the language files from disk.
     */
    public static void loadAssets(){
    	synchronized(LOCK){
    		if(translationIndices != null && translationIds != null 
        			&& defaultLocale != null ) {
        		
        		Log.info("Reloading languages");
        		translationIndices.clear();
        		translationIds = null;
        		defaultLocale = null;
        		currentLocale = null;
        		currentLocaleIndex = -1;
        		
        		Utils.gc(false);
        	};

    		final Pattern langFileExtension = Pattern.compile("(.+).(LANG|lang)$");
        	translationIndices = new ArrayList<>();
        	
        	if(Utils.isJarRunning()){
        		
        		JarFile opsu = Utils.getJarFile();
        		Enumeration<JarEntry> entries = opsu.entries();
        		while(entries.hasMoreElements()){
        			JarEntry entry = entries.nextElement();
        			String entryName = entry.getName();
        			
        			if(langFileExtension.matcher(entryName).matches()){
        				translationIndices.add(new LocaleManager(entryName));
        				if(entryName.contains("en_US")){
        					currentLocaleIndex = translationIndices.size() - 1;
        					defaultLocale = translationIndices.get(currentLocaleIndex);
        					currentLocale = defaultLocale;
        					Log.info("Found fallback language");
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
        	//compensate for running in developer mode
        	else
        	{    		
        		try {
            		File codeSource = new File(LocaleManager.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            		for(File langFile : Utils.findFilesRecursively(codeSource, langFileExtension)){
            			translationIndices.add(new LocaleManager(langFile));
            			if(langFile.getName().contains("en_US")){
            				currentLocaleIndex = translationIndices.size() - 1;
            				defaultLocale = translationIndices.get(currentLocaleIndex);
            				currentLocale = defaultLocale;
        					Log.info("Found fallback language");
            			}
            			Log.info("Added language file: " + langFile.getName());
            		}
        		} catch (Exception e) {
        			Log.error("Could not find resources for translation", e);
        		}
        	}
        	
        	translationIds = new String[translationIndices.size()];
        	
        	for(int a = 0; a < translationIds.length; a++){
        		translationIds[a] = translationIndices.get(a).toString();
        	}
        	
        	
        	Log.info("Loaded translations");
    	}
    }
    
    /**
     * Set the current locale as another one based on the given identifier.
     * If the identifier does not match any, this method does nothing and returns.
     * @param identifier The name of the language (e.g. English US, Japanese, Chinese)
     */
    public static void setLocaleFrom(String identifier){
    	synchronized(LOCK){
    		if(identifier == null) return;
        	if(translationIds == null) loadAssets();
        	
        	for(int a = 0; a < translationIds.length; a++){
        		if(translationIds[a].equals(identifier)){
        			currentLocale = translationIndices.get(a);
        			return;
        		}
        	}
        	
        	currentLocale = defaultLocale;
        	currentLocaleIndex = translationIndices.indexOf(currentLocale);
    	}
    }
    
    /**
     * Convenience method for updating the locale with the given index.
     * If the index is out-of-bounds from the translations, this method does nothing and returns.
     * 
     * @param index The index that denotes the position of the translation in the options
     */
    public static void updateLocale(int index){
    	synchronized (LOCK) {
    		if(index < 0 || index >= translationIndices.size()) return;
        	if(translationIndices == null) loadAssets();
        	
        	currentLocaleIndex = index;
        	currentLocale = translationIndices.get(currentLocaleIndex);
		}
    }
    
    /**
     * Translate a key to the current language without formatting
     * @param key The ID of the translation string
     * @return A translation of the given key or the key itself when a translation does not exist.
     */
    public static String translateKey(String key){
    	return currentLocale.translateKeyImpl(key);
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
    public static String translateKeyFormatted(String key, Object... format)
    {
        String s = translateKey(key);
        
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
     * Does the actual translation using the given key
     * @param key The ID of the translation string
     * @return A translation of the given key or the key itself when a translation does not exist
     */
    private String translateKeyImpl(String key)
    {
        String s = this.keys.get(key);
        
        //Always fall back to English if no translation key is found in another language
        if(s == null) {
        	if(this != defaultLocale) return defaultLocale.translateKeyImpl(key);
        	return key;
        }
        
        String textStr[] = s.split("\\\\r\\\\n|\\\\n|\\\\r");
        s = "";
        for(int a = 0; a < textStr.length; a++){
    		s = s + textStr[a] + (!(a + 1 == textStr.length) ? "\n" : "");
        }
        return s;
    }
    
    /**
     * Return the identifier of the current translator
     * @return The name of the language 
     */
    public String toString(){
    	return identifier;
    }
    
    //Special cases
    @Override
    public boolean equals(Object obj) {
    	if(obj instanceof LocaleManager){
    		return ((LocaleManager) obj).identifier.equals(identifier);
    	}
    	
    	return false;
    }
    
    @Override
    public int hashCode() {
    	return identifier.hashCode() + keys.hashCode();
    }
    
    /**
     * Retrieve the map containing all available translation keys for the current locale.
     * This map is <b>unmodifiable</b> as it should only be handled by the manager. 
     * <br><br>
     * To add your own translations, add in <code>*.lang</code> files in the JAR or classpath.
     */
    public Map<String, String> getTranslations(){
    	return Collections.unmodifiableMap(keys);
    }
    
    /**
     * Get the currently active locale being used to translate strings.
     * Useful for providing information about the current locale.
     */
    public static LocaleManager getCurrentLocale(){
    	return currentLocale;
    }
    
}
