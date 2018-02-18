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

package itdelatrisu.opsu.skins;

import itdelatrisu.opsu.ErrorHandler;
import itdelatrisu.opsu.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;

import org.newdawn.slick.Color;
import org.newdawn.slick.util.Log;

/**
 * Loads skin configuration files.
 */
public class SkinLoader {
	/** Name of the skin configuration file. */
	private static final String CONFIG_FILENAME = "skin.ini";

	// This class should not be instantiated.
	private SkinLoader() {}

	/**
	 * Returns a list of all subdirectories in the Skins directory.
	 * @param root the root directory (search has depth 1)
	 * @return an array of skin directories
	 */
	public static File[] getSkinDirectories(File root) {
		ArrayList<File> dirs = new ArrayList<File>();
		for (File dir : root.listFiles()) {
			if (dir.isDirectory())
				dirs.add(dir);
		}
		return dirs.toArray(new File[dirs.size()]);
	}

	/**
	 * Loads a skin configuration file.
	 * If 'skin.ini' is not found, or if any fields are not specified, the
	 * default values will be used.
	 * @param dir the skin directory
	 * @return the loaded skin
	 */
	public static Skin loadSkin(File dir) {
		File skinFile = new File(dir, CONFIG_FILENAME);
		Skin skin = new Skin(dir);
		if (!skinFile.isFile())  // missing skin.ini
			return skin;

		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(skinFile), "UTF-8"))) {
			String line = in.readLine();
			String tokens[] = null;
			while (line != null) {
				line = line.trim();
				if (!isValidLine(line)) {
					line = in.readLine();
					continue;
				}
				switch (line) {
				case "[General]":
					while ((line = in.readLine()) != null) {
						line = line.trim();
						if (!isValidLine(line))
							continue;
						if (line.charAt(0) == '[')
							break;
						if ((tokens = tokenize(line)) == null)
							continue;
						try {
							switch (tokens[0]) {
							case "Name":
								skin.name = tokens[1];
								break;
							case "Author":
								skin.author = tokens[1];
								break;
							case "Version":
								if (tokens[1].equalsIgnoreCase("latest"))
									skin.version = Skin.LATEST_VERSION;
								else
									skin.version = Float.parseFloat(tokens[1]);
								break;
							case "SliderBallFlip":
								skin.sliderBallFlip = Utils.parseBoolean(tokens[1]);
								break;
							case "CursorRotate":
								skin.cursorRotate = Utils.parseBoolean(tokens[1]);
								break;
							case "CursorExpand":
								skin.cursorExpand = Utils.parseBoolean(tokens[1]);
								break;
							case "CursorCentre":
								skin.cursorCentre = Utils.parseBoolean(tokens[1]);
								break;
							case "SliderBallFrames":
								skin.sliderBallFrames = Integer.parseInt(tokens[1]);
								break;
							case "HitCircleOverlayAboveNumber":
								skin.hitCircleOverlayAboveNumber = Utils.parseBoolean(tokens[1]);
								break;
							case "spinnerFrequencyModulate":
								skin.spinnerFrequencyModulate = Utils.parseBoolean(tokens[1]);
								break;
							case "LayeredHitSounds":
								skin.layeredHitSounds = Utils.parseBoolean(tokens[1]);
								break;
							case "SpinnerFadePlayfield":
								skin.spinnerFadePlayfield = Utils.parseBoolean(tokens[1]);
								break;
							case "SpinnerNoBlink":
								skin.spinnerNoBlink = Utils.parseBoolean(tokens[1]);
								break;
							case "AllowSliderBallTint":
								skin.allowSliderBallTint = Utils.parseBoolean(tokens[1]);
								break;
							case "AnimationFramerate":
								skin.animationFramerate = Integer.parseInt(tokens[1]);
								break;
							case "CursorTrailRotate":
								skin.cursorTrailRotate = Utils.parseBoolean(tokens[1]);
								break;
							case "CustomComboBurstSounds":
								String[] split = tokens[1].split(",");
								int[] customComboBurstSounds = new int[split.length];
								for (int i = 0; i < split.length; i++)
									customComboBurstSounds[i] = Integer.parseInt(split[i]);
								skin.customComboBurstSounds = customComboBurstSounds;
								break;
							case "ComboBurstRandom":
								skin.comboBurstRandom = Utils.parseBoolean(tokens[1]);
								break;
							case "SliderStyle":
								skin.sliderStyle = Byte.parseByte(tokens[1]);
								break;
							default:
								break;
							}
						} catch (Exception e) {
							Log.warn(String.format("Failed to read line '%s' for file '%s'.",
									line, skinFile.getAbsolutePath()), e);
						}
					}
					break;
				case "[Colours]":
					LinkedList<Color> colors = new LinkedList<Color>();
					while ((line = in.readLine()) != null) {
						line = line.trim();
						if (!isValidLine(line))
							continue;
						if (line.charAt(0) == '[')
							break;
						if ((tokens = tokenize(line)) == null)
							continue;
						try {
							String[] rgb = tokens[1].split(",");
							Color color = new Color(
								Integer.parseInt(rgb[0].trim()),
								Integer.parseInt(rgb[1].trim()),
								Integer.parseInt(rgb[2].trim())
							);
							switch (tokens[0]) {
							case "Combo1":
							case "Combo2":
							case "Combo3":
							case "Combo4":
							case "Combo5":
							case "Combo6":
							case "Combo7":
							case "Combo8":
								colors.add(color);
								break;
							case "MenuGlow":
								skin.menuGlow = color;
								break;
							case "SliderBorder":
								skin.sliderBorder = color;
								break;
							case "SliderBall":
								skin.sliderBall = color;
								break;
							case "SpinnerApproachCircle":
								skin.spinnerApproachCircle = color;
								break;
							case "SongSelectActiveText":
								skin.songSelectActiveText = color;
								break;
							case "SongSelectInactiveText":
								skin.songSelectInactiveText = color;
								break;
							case "StarBreakAdditive":
								skin.starBreakAdditive = color;
								break;
							case "InputOverlayText":
								skin.inputOverlayText = color;
								break;
							default:
								break;
							}
						} catch (Exception e) {
							Log.warn(String.format("Failed to read color '%s' for file '%s'.",
									line, skinFile.getAbsolutePath()), e);
						}
					}
					if (!colors.isEmpty())
						skin.combo = colors.toArray(new Color[colors.size()]);
					break;
				case "[Fonts]":
					while ((line = in.readLine()) != null) {
						line = line.trim();
						if (!isValidLine(line))
							continue;
						if (line.charAt(0) == '[')
							break;
						if ((tokens = tokenize(line)) == null)
							continue;
						try {
							switch (tokens[0]) {
							case "HitCirclePrefix":
								skin.hitCirclePrefix = tokens[1];
								break;
							case "HitCircleOverlap":
								skin.hitCircleOverlap = Integer.parseInt(tokens[1]);
								break;
							case "ScorePrefix":
								skin.scorePrefix = tokens[1];
								break;
							case "ScoreOverlap":
								skin.scoreOverlap = Integer.parseInt(tokens[1]);
								break;
							case "ComboPrefix":
								skin.comboPrefix = tokens[1];
								break;
							case "ComboOverlap":
								skin.comboOverlap = Integer.parseInt(tokens[1]);
								break;
							default:
								break;
							}
						} catch (Exception e) {
							Log.warn(String.format("Failed to read color '%s' for file '%s'.",
									line, skinFile.getAbsolutePath()), e);
						}
					}
					break;
				default:
					line = in.readLine();
					break;
				}
			}
		} catch (IOException e) {
			ErrorHandler.error(String.format("Failed to read file '%s'.", skinFile.getAbsolutePath()), e, false);
		}

		return skin;
	}

	/**
	 * Returns false if the line is too short or commented.
	 */
	private static boolean isValidLine(String line) {
		return (line.length() > 1 && !line.startsWith("//"));
	}

	/**
	 * Splits line into two strings: tag, value.
	 * If no ':' character is present, null will be returned.
	 */
	private static String[] tokenize(String line) {
		int index = line.indexOf(':');
		if (index == -1) {
			Log.debug(String.format("Failed to tokenize line: '%s'.", line));
			return null;
		}

		String[] tokens = new String[2];
		tokens[0] = line.substring(0, index).trim();
		tokens[1] = line.substring(index + 1).trim();
		return tokens;
	}
}
