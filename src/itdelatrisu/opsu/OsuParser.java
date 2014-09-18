/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014 Jeffrey Han
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

package itdelatrisu.opsu;

import itdelatrisu.opsu.states.Options.OpsuOptions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

import org.newdawn.slick.Color;
import org.newdawn.slick.util.Log;

/**
 * Parser for OSU files.
 */
public class OsuParser {
	/**
	 * The current file being parsed.
	 */
	private static File currentFile;

	/**
	 * The current directory number while parsing.
	 */
	private static int currentDirectoryIndex = -1;

	/**
	 * The total number of directories to parse.
	 */
	private static int totalDirectories = -1;

	// This class should not be instantiated.
	private OsuParser() {}

	/**
	 * Invokes parser for each OSU file in a root directory.
	 * @param root the root directory (search has depth 1)
	 * @param width the container width
	 * @param height the container height
	 */
	public static void parseAllFiles(File root, int width, int height, OpsuOptions options) {
		// initialize hit objects
		OsuHitObject.init(width, height);

		// progress tracking
		File[] folders = root.listFiles();
		currentDirectoryIndex = 0;
		totalDirectories = folders.length;

		for (File folder : folders) {
			currentDirectoryIndex++;
			if (!folder.isDirectory())
				continue;
			File[] files = folder.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.toLowerCase().endsWith(".osu");
				}
			});
			if (files.length < 1)
				continue;

			// create a new group entry
			ArrayList<OsuFile> osuFiles = new ArrayList<OsuFile>();
			for (File file : files) {
				currentFile = file;

				// Parse hit objects only when needed to save time/memory.
				// Change boolean to 'true' to parse them immediately.
				parseFile(file, osuFiles, false, options);
			}
			if (!osuFiles.isEmpty()) {  // add entry if non-empty
				Collections.sort(osuFiles);
				Opsu.groups.addSongGroup(osuFiles);
			}
		}

		currentFile = null;
		currentDirectoryIndex = -1;
		totalDirectories = -1;
	}

	/**
	 * Parses an OSU file.
	 * @param file the file to parse
	 * @param osuFiles the song group
	 * @param parseObjects if true, hit objects will be fully parsed now
	 * @return the new OsuFile object
	 */
	private static OsuFile parseFile(File file, ArrayList<OsuFile> osuFiles, boolean parseObjects, OpsuOptions options) {
		OsuFile osu = new OsuFile(file, options);

		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {

			// initialize timing point list
			osu.timingPoints = new ArrayList<OsuTimingPoint>();

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
							case "AudioFilename":
								osu.audioFilename = new File(file.getParent() + File.separator + tokens[1]);
								break;
							case "AudioLeadIn":
								osu.audioLeadIn = Integer.parseInt(tokens[1]);
								break;
//							case "AudioHash":  // deprecated
//								osu.audioHash = tokens[1];
//								break;
							case "PreviewTime":
								osu.previewTime = Integer.parseInt(tokens[1]);
								break;
							case "Countdown":
								osu.countdown = Byte.parseByte(tokens[1]);
								break;
							case "SampleSet":
								osu.sampleSet = tokens[1];
								break;
							case "StackLeniency":
								osu.stackLeniency = Float.parseFloat(tokens[1]);
								break;
							case "Mode":
								osu.mode = Byte.parseByte(tokens[1]);

								/* Non-Opsu! standard files not implemented (obviously). */
								if (osu.mode != 0)
									return null;

								break;
							case "LetterboxInBreaks":
								osu.letterboxInBreaks = (Integer.parseInt(tokens[1]) == 1);
								break;
							case "WidescreenStoryboard":
								osu.widescreenStoryboard = (Integer.parseInt(tokens[1]) == 1);
								break;
							case "EpilepsyWarning":
								osu.epilepsyWarning = (Integer.parseInt(tokens[1]) == 1);
							default:
								break;
							}
						} catch (Exception e) {
							Log.warn(String.format("Failed to read line '%s' for file '%s'.",
									line, file.getAbsolutePath()), e);
						}
					}
					break;
				case "[Editor]":
					while ((line = in.readLine()) != null) {
						line = line.trim();
						if (!isValidLine(line))
							continue;
						if (line.charAt(0) == '[')
							break;
						/* Not implemented. */
//						if ((tokens = tokenize(line)) == null)
//							continue;
//						try {
//							switch (tokens[0]) {
//							case "Bookmarks":
//								String[] bookmarks = tokens[1].split(",");
//								osu.bookmarks = new int[bookmarks.length];
//								for (int i = 0; i < bookmarks.length; i++)
//									osu.bookmarks[i] = Integer.parseInt(bookmarks[i]);
//								break;
//							case "DistanceSpacing":
//								osu.distanceSpacing = Float.parseFloat(tokens[1]);
//								break;
//							case "BeatDivisor":
//								osu.beatDivisor = Byte.parseByte(tokens[1]);
//								break;
//							case "GridSize":
//								osu.gridSize = Integer.parseInt(tokens[1]);
//								break;
//							case "TimelineZoom":
//								osu.timelineZoom = Integer.parseInt(tokens[1]);
//								break;
//							default:
//								break;
//							}
//						} catch (Exception e) {
//							Log.warn(String.format("Failed to read editor line '%s' for file '%s'.",
//									line, file.getAbsolutePath()), e);
//						}
					}
					break;
				case "[Metadata]":
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
							case "Title":
								osu.title = tokens[1];
								break;
							case "TitleUnicode":
								osu.titleUnicode = tokens[1];
								break;
							case "Artist":
								osu.artist = tokens[1];
								break;
							case "ArtistUnicode":
								osu.artistUnicode = tokens[1];
								break;
							case "Creator":
								osu.creator = tokens[1];
								break;
							case "Version":
								osu.version = tokens[1];
								break;
							case "Source":
								osu.source = tokens[1];
								break;
							case "Tags":
								osu.tags = tokens[1].toLowerCase();
								break;
							case "BeatmapID":
								osu.beatmapID = Integer.parseInt(tokens[1]);
								break;
							case "BeatmapSetID":
								osu.beatmapSetID = Integer.parseInt(tokens[1]);
								break;
							}
						} catch (Exception e) {
							Log.warn(String.format("Failed to read metadata '%s' for file '%s'.",
									line, file.getAbsolutePath()), e);
						}
					}
					break;
				case "[Difficulty]":
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
							case "HPDrainRate":
								osu.HPDrainRate = Float.parseFloat(tokens[1]);
								break;
							case "CircleSize":
								osu.circleSize = Float.parseFloat(tokens[1]);
								break;
							case "OverallDifficulty":
								osu.overallDifficulty = Float.parseFloat(tokens[1]);
								break;
							case "ApproachRate":
								osu.approachRate = Float.parseFloat(tokens[1]);
								break;
							case "SliderMultiplier":
								osu.sliderMultiplier = Float.parseFloat(tokens[1]);
								break;
							case "SliderTickRate":
								osu.sliderTickRate = Float.parseFloat(tokens[1]);
								break;
							}
						} catch (Exception e) {
							Log.warn(String.format("Failed to read difficulty '%s' for file '%s'.",
									line, file.getAbsolutePath()), e);
						}
					}
					if (osu.approachRate == -1f)  // not in old format
						osu.approachRate = osu.overallDifficulty;
					break;
				case "[Events]":
					while ((line = in.readLine()) != null) {
						line = line.trim();
						if (!isValidLine(line))
							continue;
						if (line.charAt(0) == '[')
							break;
						tokens = line.split(",");
						switch (tokens[0]) {
						case "0":  // background
							tokens[2] = tokens[2].replaceAll("^\"|\"$", "");
							String ext = OsuParser.getExtension(tokens[2]);
							if (ext.equals("jpg") || ext.equals("png"))
								osu.bg = file.getParent() + File.separator + tokens[2];
							break;
						case "2":  // break periods
							try {
								if (osu.breaks == null)  // optional, create if needed
									osu.breaks = new ArrayList<Integer>();
								osu.breaks.add(Integer.parseInt(tokens[1]));
								osu.breaks.add(Integer.parseInt(tokens[2]));
							} catch (Exception e) {
								Log.warn(String.format("Failed to read break period '%s' for file '%s'.",
										line, file.getAbsolutePath()), e);
							}
							break;
						default:
							/* Not implemented. */
							break;
						}
					}
					break;
				case "[TimingPoints]":
					while ((line = in.readLine()) != null) {
						line = line.trim();
						if (!isValidLine(line))
							continue;
						if (line.charAt(0) == '[')
							break;

						try {
							// parse timing point
							OsuTimingPoint timingPoint = new OsuTimingPoint(line);

							// calculate BPM
							if (!timingPoint.isInherited()) {
								int bpm = Math.round(60000 / timingPoint.getBeatLength());
								if (osu.bpmMin == 0)
									osu.bpmMin = osu.bpmMax = bpm;
								else if (bpm < osu.bpmMin)
									osu.bpmMin = bpm;
								else if (bpm > osu.bpmMax)
									osu.bpmMax = bpm;
							}

							osu.timingPoints.add(timingPoint);
						} catch (Exception e) {
							Log.warn(String.format("Failed to read timing point '%s' for file '%s'.",
									line, file.getAbsolutePath()), e);
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
							switch (tokens[0]) {
							case "Combo1":
							case "Combo2":
							case "Combo3":
							case "Combo4":
							case "Combo5":
							case "Combo6":
							case "Combo7":
							case "Combo8":
								String[] rgb = tokens[1].split(",");
								colors.add(new Color(
									Integer.parseInt(rgb[0]),
									Integer.parseInt(rgb[1]),
									Integer.parseInt(rgb[2])
								));
							default:
								break;
							}
						} catch (Exception e) {
							Log.warn(String.format("Failed to read color '%s' for file '%s'.",
									line, file.getAbsolutePath()), e);
						}
					}
					if (!colors.isEmpty())
						osu.combo = colors.toArray(new Color[colors.size()]);
					break;
				case "[HitObjects]":
					int type = 0;
					while ((line = in.readLine()) != null) {
						line = line.trim();
						if (!isValidLine(line))
							continue;
						if (line.charAt(0) == '[')
							break;
						/* Only type counts parsed at this time. */
						tokens = line.split(",");
						try {
							type = Integer.parseInt(tokens[3]);
							if ((type & OsuHitObject.TYPE_CIRCLE) > 0)
								osu.hitObjectCircle++;
							else if ((type & OsuHitObject.TYPE_SLIDER) > 0)
								osu.hitObjectSlider++;
							else //if ((type & OsuHitObject.TYPE_SPINNER) > 0)
								osu.hitObjectSpinner++;
						} catch (Exception e) {
							Log.warn(String.format("Failed to read hit object '%s' for file '%s'.",
									line, file.getAbsolutePath()), e);
						}
					}

					try {
						// map length = last object end time (TODO: end on slider?)
						if ((type & OsuHitObject.TYPE_SPINNER) > 0) {
							// some 'endTime' fields contain a ':' character (?)
							int index = tokens[5].indexOf(':');
							if (index != -1)
								tokens[5] = tokens[5].substring(0, index);
							osu.endTime = Integer.parseInt(tokens[5]);
						} else if (type != 0)
							osu.endTime = Integer.parseInt(tokens[2]);
					} catch (Exception e) {
						Log.warn(String.format("Failed to read hit object end time '%s' for file '%s'.",
								line, file.getAbsolutePath()), e);
					}
					break;
				default:
					line = in.readLine();
					break;
				}
			}
		} catch (IOException e) {
			Log.error(String.format("Failed to read file '%s'.", file.getAbsolutePath()), e);
		}

		// if no custom colors, use the default color scheme
		if (osu.combo == null)
			osu.combo = Utils.DEFAULT_COMBO;

		// parse hit objects now?
		if (parseObjects)
			parseHitObjects(osu);

		// add OsuFile to song group
		osuFiles.add(osu);
		return osu;
	}

	/**
	 * Parses all hit objects in an OSU file.
	 * @param osu the OsuFile to parse
	 */
	public static void parseHitObjects(OsuFile osu) {
		if (osu.objects != null)  // already parsed
			return;

		osu.objects = new OsuHitObject[(osu.hitObjectCircle
				+ osu.hitObjectSlider + osu.hitObjectSpinner)];

		try (BufferedReader in = new BufferedReader(new FileReader(osu.getFile()))) {
			String line = in.readLine();
			while (line != null) {
				line = line.trim();
				if (!line.equals("[HitObjects]"))
					line = in.readLine();
				else
					break;
			}
			if (line == null) {
				Log.warn(String.format("No hit objects found in OsuFile '%s'.", osu.toString()));
				return;
			}

			// combo info
			int comboIndex = 0;   // color index
			int comboNumber = 1;  // combo number

			int objectIndex = 0;
			while ((line = in.readLine()) != null && objectIndex < osu.objects.length) {
				line = line.trim();
				if (!isValidLine(line))
					continue;
				if (line.charAt(0) == '[')
					break;

				// lines must have at minimum 5 parameters
				int tokenCount = line.length() - line.replace(",", "").length();
				if (tokenCount < 4)
					continue;

				try {
					// create a new OsuHitObject for each line
					OsuHitObject hitObject = new OsuHitObject(line);

					// set combo info
					// - new combo: get next combo index, reset combo number
					// - else:      maintain combo index, increase combo number
					if (hitObject.isNewCombo()) {
						comboIndex = (comboIndex + 1) % osu.combo.length;
						comboNumber = 1;
					}
					hitObject.setComboIndex(comboIndex);
					hitObject.setComboNumber(comboNumber++);

					osu.objects[objectIndex++] = hitObject;
				} catch (Exception e) {
					Log.warn(String.format("Failed to read hit object '%s' for OsuFile '%s'.",
							line, osu.toString()), e);
				}
			}
		} catch (IOException e) {
			Log.error(String.format("Failed to read file '%s'.", osu.getFile().getAbsolutePath()), e);
		}
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

	/**
	 * Returns the file extension of a file.
	 */
	public static String getExtension(String file) {
		int i = file.lastIndexOf('.');
		return (i != -1) ? file.substring(i + 1).toLowerCase() : "";
	}

	/**
	 * Returns the name of the current file being parsed, or null if none.
	 */
	public static String getCurrentFileName() {
		return (currentFile != null) ? currentFile.getName() : null;
	}

	/**
	 * Returns the progress of file parsing, or -1 if not parsing.
	 * @return the completion percent [0, 100] or -1
	 */
	public static int getParserProgress() {
		if (currentDirectoryIndex == -1 || totalDirectories == -1)
			return -1;

		return currentDirectoryIndex * 100 / totalDirectories;
	}
}