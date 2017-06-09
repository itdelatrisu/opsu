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

package itdelatrisu.opsu.translation;

import itdelatrisu.opsu.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import org.newdawn.slick.util.Log;

/**
 * Provides localisation capabilities to opsu!
 */
public class I18n {

	/** A hardcoded instance of the translation framework */
	private static final I18n FALLBACK;

	/** The list of active languages used to translate opsu! */
	private static final List<I18n> LOCALES;

	/** An array of String values that represent the identifiers of each locale */
	public static String[] localeIDs;

	/** The current locale being used to translate opsu! */
	private static I18n currentLocale;

	/** A mapping of all available translations <code>(key -> value)</code> */
	private final Map<String, String> keys;

	/**
	 * Generalized constructor for the translator using input streams
	 * @param stream A stream that provides data regarding the translation keys
	 * @param fallbackId A fallback ID to identify this locale if the key <code>language.name</code> is not present.
	 */
	private I18n(InputStream stream, String fallbackId) {
		this.keys = new HashMap<String, String>();
		if (stream == null)
			return; // used by the hardcoded fallback translator

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) 
				if (!line.isEmpty() && line.charAt(0) != 35) {
					String[] values = line.split("=", 2);
					this.keys.put(values[0], values[1]);
				}
			
		} catch (IOException e) {
			Log.warn("Could not load all translation keys", e);
		}

		if (!keys.containsKey("language.name"))
			keys.put("language.name", fallbackId);
	}

	/**
	 * Constructor for language files that can be found outside the JAR
	 * @param langFile The reference to the external language file
	 */
	private I18n(File langFile) throws FileNotFoundException {
		this(new FileInputStream(langFile), langFile.getName().substring(0, langFile.getName().length() - 5));
	}

	/**
	 * Constructor for language files found inside the JAR
	 * @param resourceLocation A textual representation of the location of the
	 * JAR resource
	 */
	private I18n(String resourceLocation) {
		this(I18n.class.getResourceAsStream(resourceLocation),
				resourceLocation.substring(resourceLocation.lastIndexOf('/'), resourceLocation.length() - 5));
	}

	/**
	 * <p>
	 * Searches for and loads all locales found. Calling this method again will
	 * clear all active locales and reload them from disk.
	 * </p><p>
	 * Upon calling this method on reload, it is imperative to call any of the
	 * <code>setLocale()</code> commands for the changes to take effect.
	 * </p>
	 * @see #setLocale(String)
	 * @see #setLocale(int)
	 */
	public static void load() {
		if (!LOCALES.isEmpty() || localeIDs != null) {
			LOCALES.clear();
			localeIDs = null;
			currentLocale = null;
			Utils.gc(false);
		}

		final Pattern fileExt = Pattern.compile("(.+).(lang)$");
		currentLocale = FALLBACK;

		try (JarFile opsu = Utils.getJarFile()) {
			Enumeration<JarEntry> entries = opsu.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				String entryName = entry.getName();
				if (fileExt.matcher(entryName).matches())
					addLocale(new I18n(entryName));

			}
		} catch (NullPointerException e) {
			// compensate for running outside of JAR
			for (File langFile : findRecursively(Utils.getRunningDirectory(), fileExt)) 
				try {
					addLocale(new I18n(langFile));
				} catch (Exception e2) {
					Log.error("Could not load translation: " + langFile.getName(), e2);
				}
			
		} catch (IOException e) {
			Log.error("Could not find resources for translation", e);
		}

		localeIDs = new String[LOCALES.size()];

		for (int a = 0; a < localeIDs.length; a++)
			localeIDs[a] = LOCALES.get(a).toString();

	}

	/**
	 * <p>
	 * Set the current locale based on the given identifier. If the identifier
	 * does not match any active locale, this method does nothing
	 * and returns.
	 * </p><p>
	 * <b>Note:</b> The identifiers are based on the language files themselves.
	 * Their identifier is either based on the key <code>language.name</code> or from
	 * their file name if the key is not present.
	 * </p>
	 * @param identifier The name of the language (e.g. English US, English UK,
	 * Japanese...)
	 */
	public static void setLocale(String identifier) {
		if (identifier == null || identifier.isEmpty())
			return;
		if (localeIDs == null)
			load();

		for (int a = 0; a < localeIDs.length; a++)
			if (localeIDs[a].equals(identifier)) {
				currentLocale = LOCALES.get(a);
				break;
			}
	}

	/**
	 * <p>
	 * Set the current locale based on the given index. If the
	 * index is out-of-bounds from the translations, this method does nothing
	 * and returns.
	 * </p><p>
	 * <b>Note:</b> Using this method is discouraged as the indices may change
	 * per call to {@link #load()}. This method is public to allow options
	 * to load the localisation settings.
	 * </p>
	 * @param index The index that denotes the position of the locale in the
	 * options
	 */
	public static void setLocale(int index) {
		if (index < 0 || index >= LOCALES.size())
			return;
		if(LOCALES.isEmpty())
			load();

		currentLocale = LOCALES.get(index);
	}

	/**
	 * Translates a key to the current language
	 * @param key The ID of the translation string
	 * @return A translation of the given key or the key itself when a
	 * translation does not exist.
	 */
	public static String translate(String key) {
		return getCurrentLocale().translateImpl(key);
	}

	/**
	 * Translates a key to the current language and applies formatting via
	 * {@link String#format(String, Object...)}
	 * @param key The ID of the translation string
	 * @param format Arguments to use in formatting. If there are more arguments
	 * than format specifiers, the extra arguments are ignored.
	 * @return A translation of the given key as formatted by the given objects
	 * or the key itself when a translation does not exist.
	 */
	public static String translateFormatted(String key, Object... format) {
		String s = translate(key);

		if (s.equals(key)) // no translation found
			return key;

		try {
			return String.format(s, format);
		} catch (IllegalFormatException var5) {
			// TODO 'silently fail?'
			return key;
		}
	}

	/**
	 * Does the actual translation using the given key
	 * @param key The ID of the translation string
	 * @return A translation of the given key or the key itself when a
	 * translation does not exist
	 */
	private String translateImpl(String key) {
		if (key == null)
			return "null";

		String s = this.keys.get(key);

		// Retry using fallback if no translation key is found
		if (s == null) {
			if (!this.equals(FALLBACK))
				return FALLBACK.translateImpl(key);
			return key;
		}

		// process new lines correctly
		String newlines[] = s.split("\\\\r\\\\n|\\\\n|\\\\r");
		s = "";
		for (int a = 0; a < newlines.length; a++)
			s = s + newlines[a] + (!(a + 1 == newlines.length) ? "\n" : "");

		return s;
	}

	/**
	 * Convenience method for adding in a new locale. This is the preferred way
	 * of adding locales as it merges duplicates.
	 * @param instance The locale to add
	 */
	private static void addLocale(I18n instance) {
		if (!LOCALES.contains(instance))
			LOCALES.add(instance);
		else
			LOCALES.get(LOCALES.indexOf(instance)).keys.putAll(instance.keys);

		if (instance.toString().startsWith("English"))
			currentLocale = instance;
	}

	/**
	 * Return the identifier of the current translator
	 * @return The name of the language associated with the locale
	 */
	@Override
	public String toString() {
		return keys.get("language.name");
	}

	@Override
	public boolean equals(Object that) {
		// compare the two using their identifiers
		return (that instanceof I18n && this.toString().equals(that.toString()));
	}

	@Override
	public int hashCode() {
		// equal objects must have equal hash codes
		return toString().hashCode();
	}

	/**
	 * Get the currently active locale being used to translate string keys.
	 */
	public static I18n getCurrentLocale() {
		// null can only happen if this is called before `load()` is invoked
		return currentLocale == null ? FALLBACK : currentLocale;
	}

	// The hardcoded fallback translator can be found below
	static {
		LOCALES = new ArrayList<I18n>();
		FALLBACK = new I18n(null, null);
		FALLBACK.keys.put("language.name", "Fallback");
		load();
	}

	/**
	 * Finds files with names that match the given pattern, starting from the
	 * given directory to all subdirectories
	 * 
	 * @param source A directory to look for files in
	 * @param regex The pattern used to accept files based on their names, null
	 * if accept all files
	 * @return A list containing the files that match the pattern
	 */
	private static List<File> findRecursively(File source, Pattern regex) {
		List<File> list = new ArrayList<File>();
		if (source.isDirectory()) {
			File[] var0 = source.listFiles();
			if (var0 != null) {
				for (File fl : var0) {
					if (fl.isDirectory() && !(fl.getName().equals(".") || fl.getName().equals(".."))) {
						list.addAll(findRecursively(fl, regex));
					} else if (regex == null || regex.matcher(fl.getName()).matches()) {
						list.add(fl);
					}
				}
			}
		} else {
			return findRecursively(source.getParentFile(), regex);
		}
		return list;
	}
}