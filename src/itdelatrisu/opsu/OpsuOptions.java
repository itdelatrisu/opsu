package itdelatrisu.opsu;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.lwjgl.input.Keyboard;
import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.util.Log;

public class OpsuOptions {
	/**
	 * The beatmap directory.
	 */
	public File beatmapDir;
	/**
	 * The OSZ archive directory.
	 */
	public File oszDir;
	/**
	 * The screenshot directory (created when needed).
	 */
	public File screenshotDir;
	/**
	 * The current skin directory (for user skins).
	 */
	public File skinDir;
	/**
	 * Index (row) in resolutions[][] array.
	 */
	public int resolutionIndex = 3;
	/**
	 * Index in targetFPS[] array.
	 */
	public int targetFPSindex = 0;
	/**
	 * Whether or not to show the FPS.
	 */
	public boolean showFPS = false;
	/**
	 * Whether or not to show hit lighting effects.
	 */
	public boolean showHitLighting = true;
	/**
	 * Whether or not to show combo burst images.
	 */
	public boolean showComboBursts = true;
	/**
	 * Default music volume.
	 */
	public int musicVolume = 30;
	/**
	 * Default sound effect volume.
	 */
	public int effectVolume = 20;
	/**
	 * Default hit sound volume.
	 */
	public int hitSoundVolume = 20;
	/**
	 * Index in screenshotFormat[] array.
	 */
	public int screenshotFormatIndex = 0;
	/**
	 * Port binding.
	 */
	public int port = 49250;
	/**
	 * Whether or not to use the new cursor type.
	 */
	public boolean newCursor = true;
	/**
	 * Whether or not dynamic backgrounds are enabled.
	 */
	public boolean dynamicBackground = true;
	/**
	 * Whether or not to display perfect hit results.
	 */
	public boolean showPerfectHit = true;
	/**
	 * Percentage to dim background images during gameplay.
	 */
	public int backgroundDim = 30;
	/**
	 * Whether or not to always display the default playfield background.
	 */
	public boolean forceDefaultPlayfield = false;
	/**
	 * Whether or not to ignore resources in the beatmap folders.
	 */
	public boolean ignoreBeatmapSkins = false;
	/**
	 * Fixed difficulty overrides.
	 */
	public float fixedCS = 0f;
	/**
	 * Fixed difficulty overrides.
	 */
	public float fixedHP = 0f;
	/**
	 * Fixed difficulty overrides.
	 */
	public float fixedAR = 0f;
	/**
	 * Fixed difficulty overrides.
	 */
	public float fixedOD = 0f;
	/**
	 * Whether or not to display the files being loaded in the splash screen.
	 */
	public boolean loadVerbose = false;
	/**
	 * Track checkpoint time, in seconds.
	 */
	public int checkpoint = 0;
	/**
	 * Whether or not to disable all sounds.
	 * This will prevent SoundController from loading sound files.
	 * <p>
	 * By default, sound is disabled on Linux due to possible driver issues.
	 */
	public boolean disableSound = (System.getProperty("os.name").toLowerCase().indexOf("linux") > -1);
	/**
	 * Whether or not to display non-English metadata.
	 */
	public boolean showUnicode = false;
	/**
	 * Left and right game keys.
	 */
	public int keyLeft = Keyboard.KEY_NONE;
	/**
	 * Left and right game keys.
	 */
	public int keyRight = Keyboard.KEY_NONE;
	/**
	 * Offset time, in milliseconds, for music position-related elements.
	 */
	public int musicOffset = -150;
	/**
	 * Temporary folder for file conversions, auto-deleted upon successful exit.
	 */
	public static final File TMP_DIR = new File(".opsu_tmp/");
	/**
	 * File for logging errors.
	 */
	public static final File LOG_FILE = new File(".opsu.log");
	/**
	 * Font file name.
	 */
	public static final String FONT_NAME = "kochi-gothic.ttf";
	/**
	 * File for storing user options.
	 */
	public static final File OPTIONS_FILE = new File(".opsu.cfg");
	/**
	 * Beatmap directories (where to search for files).
	 */
	public static final String[] BEATMAP_DIRS = {
		"C:/Program Files (x86)/osu!/Songs/",
		"C:/Program Files/osu!/Songs/",
		"Songs/"
	};
	/**
	 * Screen resolutions.
	 */
	public static final int[][] resolutions = {
		{ 800, 600 },
		{ 1024, 600 },
		{ 1024, 768 },
		{ 1280, 800 },
		{ 1280, 960 },
		{ 1366, 768 },
		{ 1440, 900 },
		{ 1600, 900 },
		{ 1680, 1050 },
		{ 1920, 1080 },
		{ 1920, 1200 },
		{ 2560, 1440 },
		{ 2560, 1600 }
	};
	/**
	 * Frame limiters.
	 */
	public static final int[] targetFPS = { 60, 120, 240 };
	/**
	 * Screenshot file format.
	 */
	public static final String[] screenshotFormat = { "png", "jpg", "bmp" };

	/**
	 * Returns the fixed circle size override, if any.
	 * @return the CS value (0, 10], 0 if disabled
	 */
	public float getFixedCS() { return fixedCS; }

	/**
	 * Returns the fixed approach rate override, if any.
	 * @return the AR value (0, 10], 0 if disabled
	 */
	public float getFixedAR() { return fixedAR; }

	/**
	 * Returns the fixed HP drain rate override, if any.
	 * @return the HP value (0, 10], 0 if disabled
	 */
	public float getFixedHP() { return fixedHP; }

	/**
	 * Returns the fixed overall difficulty override, if any.
	 * @return the OD value (0, 10], 0 if disabled
	 */
	public float getFixedOD() { return fixedOD; }

	/**
	 * Returns whether or not beatmap skins are ignored.
	 * @return true if ignored
	 */
	public boolean isBeatmapSkinIgnored() { return ignoreBeatmapSkins; }

	/**
	 * Returns whether or not to override the song background with the default playfield background.
	 * @return true if forced
	 */
	public boolean isDefaultPlayfieldForced() { return forceDefaultPlayfield; }

	/**
	 * Returns the background dim level.
	 * @return the alpha level [0, 1]
	 */
	public float getBackgroundDim() { return (100 - backgroundDim) / 100f; }

	/**
	 * Returns whether or not to show perfect hit result bursts.
	 * @return true if enabled
	 */
	public boolean isPerfectHitBurstEnabled() { return showPerfectHit; }

	/**
	 * Returns whether or not the main menu background should be the current track image.
	 * @return true if enabled
	 */
	public boolean isDynamicBackgroundEnabled() { return dynamicBackground; }

	/**
	 * Returns whether or not the new cursor type is enabled.
	 * @return true if enabled
	 */
	public boolean isNewCursorEnabled() { return newCursor; }

	/**
	 * Returns the port number to bind to.
	 * @return the port
	 */
	public int getPort() { return port; }

	/**
	 * Returns whether or not combo burst effects are enabled.
	 * @return true if enabled
	 */
	public boolean isComboBurstEnabled() { return showComboBursts; }

	/**
	 * Returns whether or not hit lighting effects are enabled.
	 * @return true if enabled
	 */
	public boolean isHitLightingEnabled() { return showHitLighting; }

	/**
	 * Returns whether or not the FPS counter display is enabled.
	 * @return true if enabled
	 */
	public boolean isFPSCounterEnabled() { return showFPS; }

	/**
	 * Returns whether or not to render loading text in the splash screen.
	 * @return true if enabled
	 */
	public boolean isLoadVerbose() { return loadVerbose; }

	/**
	 * Returns the track checkpoint time.
	 * @return the checkpoint time (in ms)
	 */
	public int getCheckpoint() { return checkpoint * 1000; }

	/**
	 * Returns whether or not all sound effects are disabled.
	 * @return true if disabled
	 */
	public boolean isSoundDisabled() { return disableSound; }

	/**
	 * Returns whether or not to use non-English metadata where available.
	 * @return true if Unicode preferred
	 */
	public boolean useUnicodeMetadata() { return showUnicode; }

	/**
	 * Sets the track checkpoint time, if within bounds.
	 * @param time the track position (in ms)
	 * @return true if within bounds
	 */
	public boolean setCheckpoint(int time) {
		if (time >= 0 && time < 3600) {
			checkpoint = time;
			return true;
		}
		return false;
	}

	/**
	 * Returns the left game key.
	 * @return the left key code
	 */
	public int getGameKeyLeft() {
		if (keyLeft == Keyboard.KEY_NONE)
			keyLeft = Input.KEY_Z;
		return keyLeft;
	}

	/**
	 * Returns the right game key.
	 * @return the right key code
	 */
	public int getGameKeyRight() {
		if (keyRight == Keyboard.KEY_NONE)
			keyRight = Input.KEY_X;
		return keyRight;
	}

	/**
	 * Returns the beatmap directory.
	 * If invalid, this will attempt to search for the directory,
	 * and if nothing found, will create one.
	 * @return the beatmap directory
	 */
	public File getBeatmapDir() {
		if (beatmapDir != null && beatmapDir.isDirectory())
			return beatmapDir;
	
		// search for directory
		for (int i = 0; i < OpsuOptions.BEATMAP_DIRS.length; i++) {
			beatmapDir = new File(OpsuOptions.BEATMAP_DIRS[i]);
			if (beatmapDir.isDirectory())
				return beatmapDir;
		}
		beatmapDir.mkdir();  // none found, create new directory
		return beatmapDir;
	}

	/**
	 * Returns the OSZ archive directory.
	 * If invalid, this will create and return a "SongPacks" directory.
	 * @return the OSZ archive directory
	 */
	public File getOSZDir() {
		if (oszDir != null && oszDir.isDirectory())
			return oszDir;
	
		oszDir = new File("SongPacks/");
		oszDir.mkdir();
		return oszDir;
	}

	/**
	 * Returns the screenshot directory.
	 * If invalid, this will return a "Screenshot" directory.
	 * @return the screenshot directory
	 */
	public File getScreenshotDir() {
		if (screenshotDir != null && screenshotDir.isDirectory())
			return screenshotDir;
	
		screenshotDir = new File("Screenshots/");
		return screenshotDir;
	}

	/**
	 * Returns the current skin directory.
	 * If invalid, this will create a "Skins" folder in the root directory.
	 * @return the skin directory
	 */
	public File getSkinDir() {
		if (skinDir != null && skinDir.isDirectory())
			return skinDir;
	
		skinDir = new File("Skins/");
		skinDir.mkdir();
		return skinDir;
	}

	/**
	 * Reads user options from the options file, if it exists.
	 */
	public void parseOptions() {
		// if no config file, use default settings
		if (!OpsuOptions.OPTIONS_FILE.isFile()) {
			saveOptions();
			return;
		}
	
		try (BufferedReader in = new BufferedReader(new FileReader(OpsuOptions.OPTIONS_FILE))) {
			String line;
			String name, value;
			int i;
			while ((line = in.readLine()) != null) {
				line = line.trim();
				if (line.length() < 2 || line.charAt(0) == '#')
					continue;
				int index = line.indexOf('=');
				if (index == -1)
					continue;
				name = line.substring(0, index).trim();
				value = line.substring(index + 1).trim();
				switch (name) {
				case "BeatmapDirectory":
					beatmapDir = new File(value);
					break;
				case "OSZDirectory":
					oszDir = new File(value);
					break;
				case "ScreenshotDirectory":
					screenshotDir = new File(value);
					break;
				case "Skin":
					skinDir = new File(value);
					break;
				case "Port":
					i = Integer.parseInt(value);
					if (i > 0 && i <= 65535)
						port = i;
					break;
				case "ScreenResolution":
					i = Integer.parseInt(value);
					if (i >= 0 && i < OpsuOptions.resolutions.length)
						resolutionIndex = i;
					break;
	//				case "Fullscreen":
	//					fullscreen = Boolean.parseBoolean(value);
	//					break;
				case "FrameSync":
					i = Integer.parseInt(value);
					if (i >= 0 && i <= OpsuOptions.targetFPS.length)
						targetFPSindex = i;
					break;
				case "ScreenshotFormat":
					i = Integer.parseInt(value);
					if (i >= 0 && i < OpsuOptions.screenshotFormat.length)
						screenshotFormatIndex = i;
					break;
				case "FpsCounter":
					showFPS = Boolean.parseBoolean(value);
					break;
				case "ShowUnicode":
					showUnicode = Boolean.parseBoolean(value);
					break;
				case "NewCursor":
					newCursor = Boolean.parseBoolean(value);
					break;
				case "DynamicBackground":
					dynamicBackground = Boolean.parseBoolean(value);
					break;
				case "LoadVerbose":
					loadVerbose = Boolean.parseBoolean(value);
					break;
				case "VolumeMusic":
					i = Integer.parseInt(value);
					if (i >= 0 && i <= 100)
						musicVolume = i;
					break;
				case "VolumeEffect":
					i = Integer.parseInt(value);
					if (i >= 0 && i <= 100)
						effectVolume = i;
					break;
				case "VolumeHitSound":
					i = Integer.parseInt(value);
					if (i >= 0 && i <= 100)
						hitSoundVolume = i;
					break;
				case "Offset":
					i = Integer.parseInt(value);
					if (i >= -500 && i <= 500)
						musicOffset = i;
					break;
				case "DisableSound":
					disableSound = Boolean.parseBoolean(value);
					break;
				case "keyOsuLeft":
					if ((value.length() == 1 && Character.isLetterOrDigit(value.charAt(0))) ||
						(value.length() == 7 && value.startsWith("NUMPAD"))) {
						i = Keyboard.getKeyIndex(value);
						if (keyRight != i)
							keyLeft = i;
					}
					break;
				case "keyOsuRight":
					if ((value.length() == 1 && Character.isLetterOrDigit(value.charAt(0))) ||
						(value.length() == 7 && value.startsWith("NUMPAD"))) {
						i = Keyboard.getKeyIndex(value);
						if (keyLeft != i)
							keyRight = i;
					}
					break;
				case "DimLevel":
					i = Integer.parseInt(value);
					if (i >= 0 && i <= 100)
						backgroundDim = i;
					break;
				case "ForceDefaultPlayfield":
					forceDefaultPlayfield = Boolean.parseBoolean(value);
					break;
				case "IgnoreBeatmapSkins":
					ignoreBeatmapSkins = Boolean.parseBoolean(value);
					break;
				case "HitLighting":
					showHitLighting = Boolean.parseBoolean(value);
					break;
				case "ComboBurst":
					showComboBursts = Boolean.parseBoolean(value);
					break;
				case "PerfectHit":
					showPerfectHit = Boolean.parseBoolean(value);
					break;
				case "FixedCS":
					fixedCS = Float.parseFloat(value);
					break;
				case "FixedHP":
					fixedHP = Float.parseFloat(value);
					break;
				case "FixedAR":
					fixedAR = Float.parseFloat(value);
					break;
				case "FixedOD":
					fixedOD = Float.parseFloat(value);
					break;
				case "Checkpoint":
					setCheckpoint(Integer.parseInt(value));
					break;
				}
			}
		} catch (IOException e) {
			Log.error(String.format("Failed to read file '%s'.", OpsuOptions.OPTIONS_FILE.getAbsolutePath()), e);
		} catch (NumberFormatException e) {
			Log.warn("Format error in options file.", e);
			return;
		}
	}

	/**
	 * (Over)writes user options to a file.
	 */
	public void saveOptions() {
		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(OpsuOptions.OPTIONS_FILE), "utf-8"))) {
			// header
			SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM dd, yyyy");
			String date = dateFormat.format(new Date());
			writer.write("# opsu! configuration");
			writer.newLine();
			writer.write("# last updated on ");
			writer.write(date);
			writer.newLine();
			writer.newLine();
	
			// options
			writer.write(String.format("BeatmapDirectory = %s", getBeatmapDir().getAbsolutePath()));
			writer.newLine();
			writer.write(String.format("OSZDirectory = %s", getOSZDir().getAbsolutePath()));
			writer.newLine();
			writer.write(String.format("ScreenshotDirectory = %s", getScreenshotDir().getAbsolutePath()));
			writer.newLine();
			writer.write(String.format("Skin = %s", getSkinDir().getAbsolutePath()));
			writer.newLine();
			writer.write(String.format("Port = %d", port));
			writer.newLine();
			writer.write(String.format("ScreenResolution = %d", resolutionIndex));
			writer.newLine();
	//			writer.write(String.format("Fullscreen = %b", fullscreen));
	//			writer.newLine();
			writer.write(String.format("FrameSync = %d", targetFPSindex));
			writer.newLine();
			writer.write(String.format("FpsCounter = %b", showFPS));
			writer.newLine();
			writer.write(String.format("ShowUnicode = %b", showUnicode));
			writer.newLine();
			writer.write(String.format("ScreenshotFormat = %d", screenshotFormatIndex));
			writer.newLine();
			writer.write(String.format("NewCursor = %b", newCursor));
			writer.newLine();
			writer.write(String.format("DynamicBackground = %b", dynamicBackground));
			writer.newLine();
			writer.write(String.format("LoadVerbose = %b", loadVerbose));
			writer.newLine();
			writer.write(String.format("VolumeMusic = %d", musicVolume));
			writer.newLine();
			writer.write(String.format("VolumeEffect = %d", effectVolume));
			writer.newLine();
			writer.write(String.format("VolumeHitSound = %d", hitSoundVolume));
			writer.newLine();
			writer.write(String.format("Offset = %d", musicOffset));
			writer.newLine();
			writer.write(String.format("DisableSound = %b", disableSound));
			writer.newLine();
			writer.write(String.format("keyOsuLeft = %s", Keyboard.getKeyName(getGameKeyLeft())));
			writer.newLine();
			writer.write(String.format("keyOsuRight = %s", Keyboard.getKeyName(getGameKeyRight())));
			writer.newLine();
			writer.write(String.format("DimLevel = %d", backgroundDim));
			writer.newLine();
			writer.write(String.format("ForceDefaultPlayfield = %b", forceDefaultPlayfield));
			writer.newLine();
			writer.write(String.format("IgnoreBeatmapSkins = %b", ignoreBeatmapSkins));
			writer.newLine();
			writer.write(String.format("HitLighting = %b", showHitLighting));
			writer.newLine();
			writer.write(String.format("ComboBurst = %b", showComboBursts));
			writer.newLine();
			writer.write(String.format("PerfectHit = %b", showPerfectHit));
			writer.newLine();
			writer.write(String.format(Locale.US, "FixedCS = %.1f", fixedCS));
			writer.newLine();
			writer.write(String.format(Locale.US, "FixedHP = %.1f", fixedHP));
			writer.newLine();
			writer.write(String.format(Locale.US, "FixedAR = %.1f", fixedAR));
			writer.newLine();
			writer.write(String.format(Locale.US, "FixedOD = %.1f", fixedOD));
			writer.newLine();
			writer.write(String.format("Checkpoint = %d", checkpoint));
			writer.newLine();
			writer.close();
		} catch (IOException e) {
			Log.error(String.format("Failed to write to file '%s'.", OpsuOptions.OPTIONS_FILE.getAbsolutePath()), e);
		}
	}

	/**
	 * Returns the screenshot file format.
	 * @return the file extension ("png", "jpg", "bmp")
	 */
	public String getScreenshotFormat() { return OpsuOptions.screenshotFormat[screenshotFormatIndex]; }

	/**
	 * Returns the music offset time.
	 * @return the offset (in milliseconds)
	 */
	public int getMusicOffset() { return musicOffset; }

	/**
	 * Returns the default sound effect volume.
	 * @return the sound volume [0, 1]
	 */
	public float getEffectVolume() { return effectVolume / 100f; }

	/**
	 * Returns the default hit sound volume.
	 * @return the hit sound volume [0, 1]
	 */
	public float getHitSoundVolume() { return hitSoundVolume / 100f; }

	/**
	 * Returns the default music volume.
	 * @return the volume [0, 1]
	 */
	public float getMusicVolume() { return musicVolume / 100f; }

	/**
	 * Returns the target frame rate.
	 * @return the target FPS
	 */
	public int getTargetFPS() { return OpsuOptions.targetFPS[targetFPSindex]; }

	/**
	 * Sets the container size and makes the window borderless if the container
	 * size is identical to the screen resolution.
	 * <p>
	 * If the configured resolution is larger than the screen size, the smallest
	 * available resolution will be used.
	 * @param app the game container
	 * @throws SlickException failure to set display mode
	 */
	public void setDisplayMode(AppGameContainer app) throws SlickException {
		int screenWidth = app.getScreenWidth();
		int screenHeight = app.getScreenHeight();
		if (screenWidth < OpsuOptions.resolutions[resolutionIndex][0] || screenHeight < OpsuOptions.resolutions[resolutionIndex][1])
			resolutionIndex = 0;
	
		int containerWidth = OpsuOptions.resolutions[resolutionIndex][0];
		int containerHeight = OpsuOptions.resolutions[resolutionIndex][1];
		app.setDisplayMode(containerWidth, containerHeight, false);
		if (screenWidth == containerWidth && screenHeight == containerHeight)
			System.setProperty("org.lwjgl.opengl.Window.undecorated", "true");
	}
}