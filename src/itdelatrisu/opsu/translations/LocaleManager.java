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

/**
 * Provides translation capabilities to opsu!
 * 
 * @author Lyonlancer5
 */
public class LocaleManager {

	/**
	 * Pattern that matches numeric variable placeholders in a resource string,
	 * such as "<code>%d</code>", "<code>%3$d</code>" and "<code>%.2f</code>"
	 */
	private static final Pattern NUMERIC_VARIABLE_PATTERN = Pattern.compile("%(\\d+\\$)?[\\d\\.]*[df]");

	/**
	 * The hardcoded instance of the translation framework that uses English
	 * (en_US)
	 */
	private static final LocaleManager DEFAULT_LOCALE;

	// List variables used by Options
	/**
	 * An array of String values that represent the identifiers of each
	 * LanguageManager, used in {@link GameOption#getValueString()}
	 */
	public static String[] translationIds;

	/**
	 * A list of languages that can be used to translate opsu!
	 */
	private static List<LocaleManager> translationIndices;

	// Variables that define the current locale
	/**
	 * The active locale being used to translate opsu!<br>
	 */
	private static LocaleManager currentLocale;

	/**
	 * The index selected in the configuration panel, used in conjunction with
	 * {@link #currentLocale}
	 */
	private static int currentLocaleIndex;

	/**
	 * A mapping of all available translations <code>(key -> value)</code>
	 */
	private final HashMap<String, String> keys;

	/**
	 * Generalized constructor for the translator using input streams
	 * 
	 * @param stream
	 *            A stream that provides data regarding the translation keys
	 * @param fallbackId
	 *            An ID to identify this language manager
	 */
	private LocaleManager(InputStream stream, String fallbackId) {
		this.keys = new HashMap<>();
		if (stream == null)
			return;

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
			String line;
			while ((line = reader.readLine()) != null) {
				if (!line.isEmpty() && line.charAt(0) != 35) {
					String[] values = line.split("=", 2);
					this.keys.put(values[0], NUMERIC_VARIABLE_PATTERN.matcher(values[1]).replaceAll("%$1s"));
				}
			}

			reader.close();
		} catch (IOException e) {
			Log.warn("Could not load translation keys", e);
		}

		if (!keys.containsKey("language.name"))
			keys.put("language.name", fallbackId);
	}

	/**
	 * Constructor for language files that can be found outside the JAR
	 * 
	 * @param langFile
	 *            The reference to the external language file
	 */
	public LocaleManager(File langFile) throws FileNotFoundException {
		this(new FileInputStream(langFile), langFile.getName().substring(0, langFile.getName().length() - 5));
	}

	/**
	 * Constructor for language files found inside the JAR
	 * 
	 * @param resourceLocation
	 *            A textual representation of the location of the JAR resource
	 */
	public LocaleManager(String resourceLocation) {
		this(LocaleManager.class.getResourceAsStream(resourceLocation),
				resourceLocation.substring(resourceLocation.lastIndexOf('/'), resourceLocation.length() - 5));
	}

	/**
	 * Loads the language manager's assets. <br>
	 * This method can safely be called multiple times as it will simply reload
	 * the language files from disk.
	 */
	public static void loadAssets() {

		if (translationIndices != null && translationIds != null) {

			Log.info("Reloading languages");
			translationIndices.clear();
			translationIds = null;
			currentLocale = null;
			currentLocaleIndex = -1;

			Utils.gc(false);
		}
		;

		final Pattern langFileExtension = Pattern.compile("(.+).(LANG|lang)$");
		if (translationIndices == null)
			translationIndices = new ArrayList<>();

		// Add English as the first in line
		translationIndices.add(DEFAULT_LOCALE);
		currentLocale = DEFAULT_LOCALE;
		currentLocaleIndex = translationIndices.size() - 1;

		if (Utils.isJarRunning()) {
			JarFile opsu = Utils.getJarFile();
			Enumeration<JarEntry> entries = opsu.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				String entryName = entry.getName();
				if (langFileExtension.matcher(entryName).matches()) {
					LocaleManager lm = new LocaleManager(entryName);
					// prevent duplicates
					if (translationIndices.contains(lm))
						continue;
					translationIndices.add(lm);
					Log.info("Added translation file: " + entryName);
				}
			}

			try {
				opsu.close();
			} catch (IOException e) {
				// ignore
			}
		}
		// compensate for running in developer mode
		else {
			try {
				File codeSource = Utils.getWorkingDirectory();
				for (File langFile : Utils.findFilesRecursively(codeSource, langFileExtension)) {
					LocaleManager lm = new LocaleManager(langFile);
					// prevent duplicates
					if (translationIndices.contains(lm))
						continue;
					translationIndices.add(lm);
					Log.info("Added translation file: " + langFile.getName());
				}
			} catch (Exception e) {
				Log.error("Could not find resources for translation", e);
			}
		}

		translationIds = new String[translationIndices.size()];

		for (int a = 0; a < translationIds.length; a++) {
			translationIds[a] = translationIndices.get(a).toString();
		}

		if (translationIndices.size() - 1 <= 0) {
			Log.warn("No additional translation files found");
		} else {
			Log.info("Loaded translations");
		}
	}

	/**
	 * Set the current locale as another one based on the given identifier. If
	 * the identifier does not match any, this method does nothing and returns.
	 * 
	 * @param identifier
	 *            The name of the language (e.g. English US, Japanese, Chinese)
	 */
	public static void setLocaleFrom(String identifier) {
		if (identifier == null)
			return;
		if (translationIds == null)
			loadAssets();

		for (int a = 0; a < translationIds.length; a++) {
			if (translationIds[a].equals(identifier)) {
				currentLocale = translationIndices.get(a);
				return;
			}
		}

		currentLocale = DEFAULT_LOCALE;
		currentLocaleIndex = translationIndices.indexOf(currentLocale);
	}

	/**
	 * Convenience method for updating the locale with the given index. If the
	 * index is out-of-bounds from the translations, this method does nothing
	 * and returns.
	 * 
	 * @param index
	 *            The index that denotes the position of the translation in the
	 *            options
	 */
	public static void updateLocale(int index) {
		if (index < 0 || index >= translationIndices.size())
			return;
		if (translationIndices == null)
			loadAssets();

		currentLocaleIndex = index;
		currentLocale = translationIndices.get(currentLocaleIndex);
	}

	/**
	 * Translate a key to the current language without formatting
	 * 
	 * @param key
	 *            The ID of the translation string
	 * @return A translation of the given key or the key itself when a
	 *         translation does not exist.
	 */
	public static String translateKey(String key) {
		return getCurrentLocale().translateKeyImpl(key);
	}

	/**
	 * Translate a key to the current language and applies
	 * {@link String#format(String, Object...)}
	 * 
	 * @param key
	 *            The ID of the translation string
	 * @param format
	 *            Additional formatting objects to be used
	 * 
	 * @return A translation of the given key as formatted by the given objects
	 *         or the key itself when a translation does not exist.
	 * 
	 * @see String#format(String, Object...)
	 */
	public static String translateKeyFormatted(String key, Object... format) {
		String s = translateKey(key);

		// This means the translation does not exist
		if (s.equals(key))
			return key;

		try {
			return String.format(s, format);
		} catch (IllegalFormatException var5) {
			Log.warn("Format error: " + s, var5);
			return key;
		}
	}

	/**
	 * Does the actual translation using the given key
	 * 
	 * @param key
	 *            The ID of the translation string
	 * @return A translation of the given key or the key itself when a
	 *         translation does not exist
	 */
	private String translateKeyImpl(String key) {
		String s = this.keys.get(key);

		// Always fall back to English if no translation key is found
		if (s == null) {
			if (this != DEFAULT_LOCALE)
				return DEFAULT_LOCALE.translateKeyImpl(key);
			return key;
		}

		String textStr[] = s.split("\\\\r\\\\n|\\\\n|\\\\r");
		s = "";
		for (int a = 0; a < textStr.length; a++) {
			s = s + textStr[a] + (!(a + 1 == textStr.length) ? "\n" : "");
		}
		return s;
	}

	/**
	 * Return the identifier of the current translator
	 * 
	 * @return The name of the language
	 */
	public String toString() {
		return keys.get("language.name");
	}

	// Special cases
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof LocaleManager) {
			return ((LocaleManager) obj).toString().equals(toString());
		}

		return false;
	}

	/**
	 * Retrieve the map containing all available translation keys for the
	 * current locale. This map is <b>unmodifiable</b> as it should only be
	 * handled by the manager. <br>
	 * <br>
	 * To add your own translations, add in <code>*.lang</code> files in the JAR
	 * or classpath.
	 */
	public Map<String, String> getTranslations() {
		return Collections.unmodifiableMap(keys);
	}

	/**
	 * Get the currently active locale being used to translate strings. Useful
	 * for providing information about the current locale.
	 */
	public static LocaleManager getCurrentLocale() {
		return currentLocale == null ? DEFAULT_LOCALE : currentLocale;
	}

	// Below is the hardcoded English US as a solution to the NPE problem
	static {
		DEFAULT_LOCALE = new LocaleManager(null, null);
		String[] str = new String[] { "language.name=English (US)", "ui.button.back=Back", "ui.button.play=Play",
				"ui.button.exit=Exit", "ui.button.cancel=Cancel", "ui.menu.exit=Are you sure you want to exit %s?",
				"ui.menu.beatmap.manage=What do you want to do with this beatmap?",
				"ui.menu.delete=Are you sure you wish to delete '%s' from disk?",
				"ui.menu.scores.manage=Score Management",
				"ui.menu.mods=Mods provide different ways to enjoy gameplay. Some have an effect on the score you can achieve during ranked play. Others are just for fun.",
				"ui.button.yes=Yes", "ui.button.no=No", "ui.button.scores.clear=Clear local scores",
				"ui.button.favorites.add=Add to Favorites", "ui.button.favorites.remove=Remove from Favorites",
				"ui.button.delete=Delete...", "ui.button.cancel=Cancel",
				"ui.button.delete.confirm=Yes, delete this beatmap!",
				"ui.button.delete.group=Yes, delete all difficulties!", "ui.button.delete.song=Yes, but this one only!",
				"ui.button.delete.cancel=Nooooo! I didn't mean to!", "ui.button.reload.confirm=Let's do it!",
				"ui.button.scores.delete=Delete score", "ui.button.close=Close", "ui.button.about.web=Visit Website",
				"ui.button.about.repo=Browse the Source", "ui.button.about.issue=Report bugs here",
				"ui.button.about.issue.placeholder=[Type your description here. Feel free to delete the info below if it's not relevant.]",
				"ui.button.about.credits=Credits",
				"ui.notifications.audio.localOffset.notChanged=Offset can only be changed while game is not paused.",
				"ui.notifications.audio.localOffset.changed=Local beatmap offset set to %dms",
				"ui.notifications.audio.localOffset.using=Using local beatmap offset (%dms)",
				"ui.notifications.gameplay.customDifficulty=Playing with custom difficulty settings.",
				"ui.notifications.update.available=A new update is available!",
				"ui.notifications.update.download.failed=Update failed due to a connection error.",
				"ui.notifications.update.download.complete=Update has finished downloading.",
				"ui.notifications.update.completed=opsu! is now up to date!",
				"ui.notifications.update.completed.view=opsu! is now up to date!\nClick here to see what's changed!",
				"ui.notifications.web.noView=The web page could not be opened.",
				"ui.notifications.user.new=Enter a name for the user.",
				"ui.notifications.user.new.invalidName=You can't use that name.",
				"ui.notifications.user.new.error=Something wrong happened.",
				"ui.notifications.user.new.created=New user created.\nEnjoy the game! :)",
				"ui.notifications.download.preview.failed=Failed to download track preview.",
				"ui.notifications.options.restart=Restart to apply changes.",
				"ui.notifications.options.checkpoint.save=Checkpoint saved.",
				"ui.notifications.menu.playback.previous=<< Prev", "ui.notifications.menu.playback.play=Play",
				"ui.notifications.menu.playback.pause=Pause", "ui.notifications.menu.playback.unpause=Unpause",
				"ui.notifications.menu.playback.next=>> Next",
				"ui.notifications.download.start.error=The download could not be started.",
				"ui.notifications.download.failed=Download failed due to a connection error.",
				"ui.notifications.download.complete=Download complete: %s",
				"ui.notifications.replay.notFound=Replay file not found.",
				"ui.notifications.replay.imported=Imported %d replay%s.",
				"ui.notifications.replay.error=Failed to load replay data.\nSee log for details.",
				"ui.notifications.audio.track.notFound=Could not find track '%s'.",
				"ui.notifications.audio.track.preview.failed=Failed to load track preview. See log for details.",
				"ui.notifications.graphics.framesync=Frame limiter: %s",
				"ui.notifications.audio.beatmap.notFound=Unable to load the beatmap audio.",
				"ui.notifications.graphics.fullscreen.notAvailable=Fullscreen mode is not available at resolution %s",
				"ui.notifications.mouse.buttons.enabled=Mouse buttons are enabled.",
				"ui.notifications.mouse.buttons.disabled=Mouse buttons are disabled.",
				"ui.notifications.beatmaps.changed=Changes in Songs folder detected.\nHit F5 to refresh.",
				"ui.notifications.beatmaps.imported=Imported %d new beatmap pack%s.",
				"ui.notifications.beatmaps.import.none=No Standard beatmaps could be loaded.",
				"ui.notifications.beatmaps.import.none.exp=No beatmaps could be loaded.",
				"ui.notifications.skins.imported=Imported %d new skin%s.",
				"ui.notifications.graphics.glWarning=WARNING:\nRunning in OpenGL software mode.\nYou may experience severely degraded performance.\n\nThis can usually be resolved by updating your graphics drivers.",
				"ui.notifications.audio.alNotLoaded=Looks like sound isn't working right now. Sorry!\n\nRestarting the game will probably fix this.",
				"ui.notifications.visuals.video.error=Failed to load beatmap video.\nSee log for details.",
				"ui.notifications.audio.error=Failed to load %d audio files.",
				"ui.notifications.audio.error.view=Failed to load %d audio files.\nSee log for details.",
				"ui.notifications.graphics.screenshot=Saved screenshot to %s",
				"ui.updater.state.checking=Checking for updates...",
				"ui.updater.state.error.connection=Connection error", "ui.updater.state.error.internal=Internal error",
				"ui.updater.state.upToDate=Up to date!",
				"ui.updater.state.update.available=Update available!\nClick to download.",
				"ui.updater.state.update.downloading=Downloading update...",
				"ui.updater.state.update.downloading.progress=Downloading update...\n%.1f%% complete (%s/%s)",
				"ui.updater.state.update.downloaded=Download complete.\nClick to restart.",
				"ui.updater.state.update.queued=Update queued.", "options.name=Options",
				"options.desc=Change the way opsu! behaves", "options.search=Type to search!",
				"options.graphics.resolution=Resolution",
				"options.graphics.resolution.desc=Change the in-game resolution",
				"options.graphics.fullscreen=Fullscreen mode",
				"options.graphics.fullscreen.desc=Switch to dedicated fullscreen mode",
				"options.graphics.framesync=Frame limiter",
				"options.graphics.framesync.desc=Higher values means higher CPU usage",
				"options.graphics.framesync.show=Show FPS counter",
				"options.graphics.framesync.show.desc=Show a subtle FPS counter in the bottom right corner of the screen",
				"options.graphics.screenshotFormat=Screenshot format",
				"options.graphics.screenshotFormat.desc=Press F12 to take a screenshot.",
				"options.i18n=Select language:", "options.i18n.desc=Choose the language to show opsu! in",
				"options.i18n.refresh=Refresh Language files",
				"options.i18n.refresh.desc=Reloads all language files from disk",
				"options.i18n.unicode=Prefer metadata in original language",
				"options.i18n.unicode.desc=Where available, song titles will be shown in their native language (and character-set).",
				"options.audio.volume.master=Master", "options.audio.volume.master.desc=Global volume level",
				"options.audio.volume.music=Music", "options.audio.volume.music.desc=Music volume",
				"options.audio.volume.fx=Effects", "options.audio.volume.fx.desc=Menu and game sound effects volume",
				"options.audio.volume.hitSounds=Hit sounds", "options.audio.volume.hitSounds.desc=Hit sounds volume",
				"options.audio.uOffset=Universal offset",
				"options.audio.uOffset.desc=Adjust this value if hit objects are out of sync",
				"options.audio.disableFx=Disable all sound effects",
				"options.audio.disableFx.desc=May resolve Linux sound driver issues.\nRequires a restart.",
				"options.key.left=Left game key",
				"options.key.left.desc=Change the keybind for the keyboard left mouse click",
				"options.key.right=Right game key",
				"options.key.right.desc=Change the keybind for the keyboard right mouse click",
				"options.mouse.threadedInput=Threaded Input (Not Yet Implemented)",
				"options.mouse.threadedInput.desc=Poll mouse input in a seperate thread (Windows only)",
				"options.mouse.ignoreWheel=Disable mouse wheel in play mode",
				"options.mouse.ignoreWheel.desc=During play, you can use the mouse wheel to adjust the volume and pause the game.\nThis will disable that functionality.",
				"options.mouse.ignoreButtons=Disable mouse buttons in play mode",
				"options.mouse.ignoreButtons.desc=This option will disable all mouse buttons.\nSpecifically for people who use their keyboard to click.",
				"options.mouse.noCursor=Disable cursor", "options.mouse.noCursor.desc=Hides the cursor sprite.",
				"options.visuals.skin=Skin", "options.visuals.skin.desc=Change the visuals of the game",
				"options.visuals.skin.useHD=Load HD images",
				"options.visuals.skin.useHD.desc=Loads HD (@2x) images when available.\nIncreases memory usage and loading times.",
				"options.visuals.dimLevel=Background dim",
				"options.visuals.dimLevel.desc=Percentage to dim the background image during gameplay.",
				"options.visuals.playfieldOverride=Force default playfield",
				"options.visuals.playfieldOverride.desc=Overrides the song background with the default playfield background.",
				"options.visuals.video=Background video",
				"options.visuals.video.desc=Enables background video playback.\nIf you get a large amount of lag on beatmaps with video, try disabling this feature.",
				"options.visuals.storyboards=Storyboards (Not Yet Implemented)",
				"options.visuals.storyboards.desc=<Insert storyboard description>",
				"options.visuals.ignoreBeatmapSkins=Ignore all beatmap skins",
				"options.visuals.ignoreBeatmapSkins.desc=Defaults game settings to never use skin element overrides provided by beatmaps.",
				"options.visuals.useSkinCursor=Always use skin cursor",
				"options.visuals.useSkinCursor.desc=The selected skin's cursor will override any beatmap-specific cursor modifications.",
				"options.visuals.cursorSize=Cursor size", "options.visuals.cursorSize.desc=Change the cursor scale.",
				"options.visuals.dynamicBG=Dynamic backgrounds",
				"options.visuals.dynamicBG.desc=The current beatmap background will be used as the main menu background.",
				"options.sliders.snaking=Snaking sliders",
				"options.sliders.snaking.desc=Sliders gradually snake out from their starting point.",
				"options.sliders.beta=Use experimental sliders",
				"options.sliders.beta.desc=Render sliders using the experimental slider style.",
				"options.sliders.beta.caps=Draw slider caps",
				"options.sliders.beta.caps.desc=Draw caps (end circles) on sliders.\nOnly applies to experimental sliders.",
				"options.sliders.beta.shrink=Shrinking sliders",
				"options.sliders.beta.shrink.desc=Sliders shrink toward their ending point when the ball passes.\nOnly applies to experimental sliders.",
				"options.sliders.beta.merge=Merging sliders",
				"options.sliders.beta.merge.desc=For overlapping sliders, don't draw the edges and combine the slider tracks where they cross.\nOnly applies to experimental sliders.",
				"options.gameplay.hitLighting=Hit lighting",
				"options.gameplay.hitLighting.desc=Adds a subtle glow behind hit explosions which lights the playfield.",
				"options.gameplay.comboBurst=Combo bursts",
				"options.gameplay.comboBurst.desc=A character image bursts from the side of the screen at combo milestones.",
				"options.gameplay.perfectHits=Perfect hits",
				"options.gameplay.perfectHits.desc=Shows perfect hit result bursts (300s, slider ticks).",
				"options.gameplay.followPoints=Follow points",
				"options.gameplay.followPoints.desc=Shows follow points between hit objects.",
				"options.gameplay.accumeter=Hit error bar",
				"options.gameplay.accumeter.desc=Shows precisely how accurate you were with each hit.",
				"options.gameplay.fixedCS=Fixed CS",
				"options.gameplay.fixedCS.desc=Determines the size of circles and sliders.",
				"options.gameplay.fixedHP=Fixed HP",
				"options.gameplay.fixedHP.desc=Determines the rate at which health decreases.",
				"options.gameplay.fixedAR=Fixed AR",
				"options.gameplay.fixedAR.desc=Determines how long hit circles stay on the screen.",
				"options.gameplay.fixedOD=Fixed OD",
				"options.gameplay.fixedOD.desc=Determines the time window for hit results.",
				"options.gameplay.fixedSpeed=Fixed speed",
				"options.gameplay.fixedSpeed.desc=Determines the speed of the music.",
				"options.gameplay.checkpoint=Track checkpoint",
				"options.gameplay.checkpoint.desc=Press Ctrl+L while playing to load a checkpoint, and Ctrl+S to set one.",
				"options.misc.parallax=Parallax",
				"options.misc.parallax.desc=Add a parallax effect based on the current cursor position.",
				"options.misc.themeSong=Theme song",
				"options.misc.themeSong.desc=opsu! will play themed music throughout the game, instead of using random beatmaps.",
				"options.misc.replaySeek=Replay seeking",
				"options.misc.replaySeek.desc=Enable a seeking bar on the left side of the screen during replays.",
				"options.misc.updater=Disable automatic updates",
				"options.misc.updater.desc=Disable checking for updates when the game starts.",
				"options.misc.watchService=Watch service",
				"options.misc.watchService.desc=Watch the beatmap directory for changes. Requires a restart.",
				"options.misc.verbose=Detailed loading progress",
				"options.misc.verbose.desc=Display more verbose loading progress in the splash screen.",
				"options.misc.showUnsupportedMaps=Display maps that are designed for other game modes",
				"options.misc.showUnsupportedMaps.desc=Only applies to Taiko/Catch-the-Beat beatmaps",
				"options.general.name=GENERAL", "options.general.language.name=LANGUAGE",
				"options.general.updates.name=UPDATES", "options.graphics.name=GRAPHICS",
				"options.graphics.layout.name=LAYOUT", "options.graphics.renderer.name=RENDERER",
				"options.graphics.details.name=DETAIL SETTINGS",
				"options.graphics.sliderBeta.name=EXPERIMENTAL SLIDERS", "options.graphics.mainMenu.name=MAIN MENU",
				"options.gameplay.name=GAMEPLAY", "options.gameplay.general.name=GENERAL", "options.audio.name=AUDIO",
				"options.audio.volume.name=VOLUME", "options.audio.offset.name=OFFSET ADJUSTMENT",
				"options.visuals.skin.name=SKIN", "options.input.name=INPUT", "options.input.mouse.name=MOUSE",
				"options.input.keyboard.name=KEYBOARD", "options.custom.name=CUSTOM",
				"options.custom.difficulty.name=DIFFICULTY", "options.custom.seeking.name=SEEKING",
				"options.custom.misc.name=MISCELLANEOUS", "ui.button.mods.resetAll=Reset All Mods", "mod.easy=Easy",
				"mod.easy.desc=Reduces overall difficulty - larger circles, more forgiving HP drain, less accuracy required.",
				"mod.noFail=NoFail", "mod.noFail.desc=You can't fail. No matter what.", "mod.halfTime=HalfTime",
				"mod.halfTime.desc=Less zoom.", "mod.hardRock=HardRock",
				"mod.hardRock.desc=Everything just got a bit harder...", "mod.suddenDeath=SuddenDeath",
				"mod.suddenDeath.desc=Miss a note and fail.", "mod.perfect=Perfect", "mod.perfect.desc=SS or quit.",
				"mod.doubleTime=DoubleTime", "mod.doubleTime.desc=Zoooooooooom.", "mod.nightcore=Nightcore",
				"mod.nightcore.desc=uguuuuuuuu", "mod.hidden=Hidden",
				"mod.hidden.desc=Play with no approach circles and fading notes for a slight score advantage.",
				"mod.flashlight=Flashlight", "mod.flashlight.desc=Restricted view area.", "mod.relax=Relax",
				"mod.relax.desc=You don't need to click.\nGive your clicking/tapping finger a break from the heat of things.\n**UNRANKED**",
				"mod.autopilot=Autopilot",
				"mod.autopilot.desc=Automatic cursor movement - just follow the rhythm.\n**UNRANKED**",
				"mod.spunout=SpunOut", "mod.spunout.desc=Spinners will be automatically completed.",
				"mod.autoplay=Autoplay", "mod.autoplay.desc=Watch a perfect automated play through the song.",
				"mod.category.easy=Difficulty Reduction", "mod.category.hard=Difficulty Increase",
				"mod.category.special=Special" };

		for (String line : str) {
			String[] values = line.split("=", 2);
			DEFAULT_LOCALE.keys.put(values[0], NUMERIC_VARIABLE_PATTERN.matcher(values[1]).replaceAll("%$1s"));
		}
	}
}
