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
import itdelatrisu.opsu.ui.Fonts;

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
import java.util.Locale;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.newdawn.slick.Color;
import org.newdawn.slick.UnicodeFont;
import org.newdawn.slick.util.Log;

/**
 * Provides localisation capabilities to opsu!
 */
public class I18n {

	/** The list of active languages used to translate opsu! */
	public static final Map<String, I18n> LOCALES;

	/** The file extension used by the localisation framework */
	private static final String LANG_FILE_EXT = ".lang";

	/** A hardcoded instance of the translation framework */
	private static final I18n FALLBACK;

	/** The current locale being used to translate opsu! */
	private static I18n currentLocale;

	/** An array of String values that represent the identifiers of each locale */
	private static String[] localeIDs;

	/** A mapping of all available translations <code>(key -> value)</code> */
	private final Map<String, String> keys;

	static {
		LOCALES = new HashMap<String, I18n>();
		FALLBACK = new I18n(null, null);
		FALLBACK.keys.put("language.name", "Fallback");
		load();
	}

	/**
	 * Generalized constructor for the translator using input streams
	 * @param stream A stream that provides data regarding the translation keys
	 * @param fallbackId A fallback ID to identify this locale if the key
	 * <code>language.name</code> is not present.
	 */
	private I18n(InputStream stream, String fallbackId) {
		this.keys = new HashMap<String, String>();
		if (stream == null)
			return; // used by the hardcoded fallback translator

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (!line.isEmpty() && line.charAt(0) != '#') {
					String[] values = line.split("=", 2);
					this.keys.put(values[0], values[1]);
				}
			}
		} catch (IOException e) {
			Log.warn("Could not load all translation keys", e);
		}

		if (!keys.containsKey("language.name"))
			keys.put("language.name", fallbackId.toLowerCase(Locale.ENGLISH));
	}

	/**
	 * Constructor for language files that can be found outside the JAR
	 * @param langFile The reference to the external language file
	 */
	private I18n(File langFile) throws FileNotFoundException {
		this(new FileInputStream(langFile),
				langFile.getName().substring(0, langFile.getName().length() - LANG_FILE_EXT.length()));
	}

	/**
	 * Constructor for language files found inside the JAR
	 * @param resourceLocation A textual representation of the location of the
	 * JAR resource
	 */
	private I18n(String resourceLocation) {
		this(I18n.class.getResourceAsStream(resourceLocation), resourceLocation
				.substring(resourceLocation.lastIndexOf('/'), resourceLocation.length() - LANG_FILE_EXT.length()));
	}

	/**
	 * <p>
	 * Searches for and loads all locales found. Calling this method again will
	 * clear all active locales and reload them from disk.
	 * </p><p>
	 * Upon calling this method on a reload, it is imperative to call the
	 * {@link #setLocale(String)} command for the changes to take effect.
	 * </p>
	 * @see #setLocale(String)
	 */
	public static void load() {
		LOCALES.clear();
		localeIDs = null;
		currentLocale = FALLBACK;

		try (JarFile opsu = Utils.getJarFile()) {
			Enumeration<JarEntry> entries = opsu.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				String entryName = entry.getName();
				if (entryName.endsWith(LANG_FILE_EXT))
					addLocale(new I18n(entryName));

			}
		} catch (NullPointerException e) {
			// compensate for running outside of JAR context
			for (File langFile : findLanguagesRecursively(Utils.getWorkingDirectory()))
				try {
					addLocale(new I18n(langFile));
				} catch (Exception e2) {
					Log.error("Could not load translation: " + langFile.getName(), e2);
				}

		} catch (IOException e) {
			Log.error("Could not find resources for translation", e);
		}
	}

	/**
	 * <p>
	 * Set the current locale based on the given identifier. If the identifier
	 * does not match any active locale, this method does nothing and returns.
	 * </p><p>
	 * <b>Note:</b> The identifiers are based on the language files themselves.
	 * These may be based on the translation key {@link #toString()
	 * <code>language.name</code>} or from their file name if the key is not
	 * present.
	 * </p>
	 * @param identifier The name of the language
	 * @see #toString()
	 */
	public static void setLocale(String identifier) {
		if (identifier == null || identifier.isEmpty())
			return;
		if (LOCALES.isEmpty())
			load();

		for (Map.Entry<String, I18n> ids : LOCALES.entrySet())
			if (ids.getKey().equals(identifier)) {
				currentLocale = ids.getValue();
				return;
			}
	}

	/**
	 * Translates a key to the current language and draws them to the screen.
	 * @param font The font to draw the translation characters in
	 * @param x The x location at which to draw the string
	 * @param y The y location at which to draw the string
	 * @param key The ID of the translation string
	 * @param color The color to draw the string with
	 * @param startIndex The index of the first character to draw
	 * @param endIndex The index of the last character from the string to draw
	 * @see #translate(String, UnicodeFont)
	 * @see UnicodeFont#drawString(float, float, String, Color, int, int)
	 */
	public static void translateAndDraw(UnicodeFont font, float x, float y, String key, Color color, int startIndex,
			int endIndex) {
		font.drawString(x, y, translate(key, font), color, startIndex, endIndex);
	}

	/**
	 * Translates a key to the current language and draws them to the screen.
	 * @param font The font to draw the translation characters in
	 * @param x The x location at which to draw the string
	 * @param y The y location at which to draw the string
	 * @param key The ID of the translation string
	 * @param color The color to draw the string with
	 * @see #translate(String, UnicodeFont)
	 * @see UnicodeFont#drawString(float, float, String, Color)
	 */
	public static void translateAndDraw(UnicodeFont font, float x, float y, String key, Color color) {
		font.drawString(x, y, translate(key, font), color);
	}

	/**
	 * Translates a key to the current language and draws them to the screen.
	 * @param font The font to draw the translation characters in
	 * @param x The x location at which to draw the string
	 * @param y The y location at which to draw the string
	 * @param key The ID of the translation string
	 * @see #translate(String, UnicodeFont)
	 * @see UnicodeFont#drawString(float, float, String)
	 */
	public static void translateAndDraw(UnicodeFont font, float x, float y, String key) {
		font.drawString(x, y, translate(key, font));
	}

	/**
	 * Translates a key to the current language, applies formatting and draws
	 * them to the screen.
	 * @param font The font to draw the translation characters in
	 * @param x The x location at which to draw the string
	 * @param y The y location at which to draw the string
	 * @param key The ID of the translation string
	 * @param color The color to draw the string with
	 * @param startIndex The index of the first character to draw
	 * @param endIndex The index of the last character from the string to draw
	 * @see #translateFormatted(String, UnicodeFont, Object...)
	 * @see UnicodeFont#drawString(float, float, String, Color, int, int)
	 */
	public static void translateAndDrawFormatted(UnicodeFont font, float x, float y, Color color, int startIndex,
			int endIndex, String key, Object... args) {
		font.drawString(x, y, translateFormatted(key, font, args), color, startIndex, endIndex);
	}

	/**
	 * Translates a key to the current language, applies formatting and draws
	 * them to the screen.
	 * @param font The font to draw the translation characters in
	 * @param x The x location at which to draw the string
	 * @param y The y location at which to draw the string
	 * @param key The ID of the translation string
	 * @param color The color to draw the string with
	 * @see #translateFormatted(String, UnicodeFont, Object...)
	 * @see UnicodeFont#drawString(float, float, String, Color)
	 */
	public static void translateAndDrawFormatted(UnicodeFont font, float x, float y, Color color, String key,
			Object... args) {
		font.drawString(x, y, translateFormatted(key, font, args), color);
	}

	/**
	 * Translates a key to the current language, applies formatting and draws
	 * them to the screen.
	 * @param font The font to draw the translation characters in
	 * @param x The x location at which to draw the string
	 * @param y The y location at which to draw the string
	 * @param key The ID of the translation string
	 * @see #translateFormatted(String, UnicodeFont, Object...)
	 * @see UnicodeFont#drawString(float, float, String)
	 */
	public static void translateAndDrawFormatted(UnicodeFont font, float x, float y, String key, Object... args) {
		font.drawString(x, y, translateFormatted(key, font, args));
	}

	/**
	 * Translates a key to the current language.
	 * @param key The ID of the translation string
	 * @param font The font to load the translation characters in, if there are
	 * translations containing characters outside the ASCII character set
	 * @return A translation of the given key or the key itself when a
	 * translation does not exist.
	 */
	public static String translate(String key, UnicodeFont font) {
		return getCurrentLocale().translateImpl(key, font);
	}

	/**
	 * Translates a key to the current language and applies formatting via
	 * {@link String#format(String, Object...)}
	 * @param key The ID of the translation string
	 * @param format Arguments to use in formatting. If there are more arguments
	 * than format specifiers, the extra arguments are ignored.
	 * @param font The font to load the translation characters in, if there are
	 * translations containing characters outside the ASCII character set
	 * @return A translation of the given key as formatted by the given objects
	 * or the key itself when a translation does not exist.
	 */
	public static String translateFormatted(String key, UnicodeFont font, Object... format) {
		String translation = translate(key, font);

		if (translation.equals(key)) // no translation found or key is empty
			return key;

		try {
			return String.format(translation, format);
		} catch (IllegalFormatException e) {
			return "Format error: " + key;
		}
	}

	/**
	 * Does the actual translation using the given key.
	 * @param key The ID of the translation string
	 * @param font The font to load the translation characters in, if there are
	 * translations containing characters outside the ASCII character set
	 * @return A translation of the given key or the key itself when a
	 * translation does not exist.
	 */
	private String translateImpl(String key, UnicodeFont font) {
		if (key == null)
			return "null";
		if (key.isEmpty())
			return key;

		String translation = this.keys.get(key);
		if (translation == null) // Fallback depends on and duplicates this statement
			return key;

		// process new lines correctly
		String translationLines[] = translation.split("\\\\r\\\\n|\\\\n|\\\\r");
		translation = "";
		for (int i = 0; i < translationLines.length; i++)
			translation += translationLines[i] + ((i + 1 != translationLines.length) ? "\n" : "");

		if (font != null)
			Fonts.loadGlyphs(font, translation);

		return translation;
	}

	/**
	 * Convenience method for adding in a new locale. Any duplicates are merged
	 * on top of the existing one.
	 * @param instance The locale to add/merge
	 */
	private static void addLocale(I18n instance) {
		if (!LOCALES.containsKey(instance.toString()))
			LOCALES.put(instance.toString(), instance);
		else
			LOCALES.get(instance.toString()).keys.putAll(instance.keys);

		if (instance.toString().equals("English (US)"))
			currentLocale = instance;
	}

	/**
	 * Return the identifier of the current locale, may be used to set the
	 * current locale.
	 * @return The name of the language associated with the locale.
	 * @see #setLocale(String)
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
	 * Gets the currently active locale being used to translate string keys.
	 */
	public static I18n getCurrentLocale() {
		// null can only happen if this is called before `load()` is invoked
		return currentLocale == null ? FALLBACK : currentLocale;
	}

	/**
	 * Gets all the active locales' IDs and stores them into an array.
	 */
	public static String[] getActiveLocaleIDs() {
		if(localeIDs == null) {
			List<String> activeLocales = new ArrayList<String>(LOCALES.size());
			for (String localeId : LOCALES.keySet()) {
				activeLocales.add(localeId);
				try {
					// Load glyphs to display in DropdownMenu
					Fonts.loadGlyphs(Fonts.MEDIUM, localeId);
					Fonts.loadGlyphs(Fonts.MEDIUMBOLD, localeId);
				} catch (Exception e) {
					// GL contexts reside only on their caller thread
				}
			}

			localeIDs = activeLocales.toArray(new String[LOCALES.size()]);
		}
		return localeIDs;
	}

	/**
	 * Finds all language files, recursing into the source's subdirectories.
	 * @param source A directory to look for the language files in
	 * @return The list of all language files found within the source and its
	 * subdirectories.
	 */
	private static List<File> findLanguagesRecursively(File source) {
		List<File> langFiles = new ArrayList<File>();
		if (source.isDirectory()) {
			File[] var0 = source.listFiles();
			if (var0 != null)
				for (File candidate : var0)
					if (candidate.isDirectory())
						langFiles.addAll(findLanguagesRecursively(candidate));
					else if (candidate.getName().endsWith(LANG_FILE_EXT))
						langFiles.add(candidate);

		} else
			return findLanguagesRecursively(source.getParentFile());

		return langFiles;
	}
}