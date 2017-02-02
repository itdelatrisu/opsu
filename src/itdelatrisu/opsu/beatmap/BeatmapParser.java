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

package itdelatrisu.opsu.beatmap;

import itdelatrisu.opsu.ErrorHandler;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.db.BeatmapDB;
import itdelatrisu.opsu.io.MD5InputStreamWrapper;
import itdelatrisu.opsu.options.Options;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.newdawn.slick.Color;
import org.newdawn.slick.util.Log;

/**
 * Parser for beatmaps.
 */
public class BeatmapParser {
	/** The string lookup database. */
	private static HashMap<String, String> stringdb = new HashMap<String, String>();

	/** The expected pattern for beatmap directories, used to find beatmap set IDs. */
	private static final String DIR_MSID_PATTERN = "^\\d+ .*";

	/** The current file being parsed. */
	private static File currentFile;

	/** The current directory number while parsing. */
	private static int currentDirectoryIndex = -1;

	/** The total number of directories to parse. */
	private static int totalDirectories = -1;

	/** Parser statuses. */
	public enum Status { NONE, PARSING, CACHE, INSERTING };

	/** The current status. */
	private static Status status = Status.NONE;

	/** If no Provider supports a MessageDigestSpi implementation for the MD5 algorithm. */
	private static boolean hasNoMD5Algorithm = false;

	// This class should not be instantiated.
	private BeatmapParser() {}

	/**
	 * Invokes parser for each OSU file in a root directory and
	 * adds the beatmaps to a new BeatmapSetList.
	 * @param root the root directory (search has depth 1)
	 */
	public static void parseAllFiles(File root) { parseAllFiles(root, null); }

	/**
	 * Invokes parser for each OSU file in a root directory and
	 * adds the beatmaps to a new BeatmapSetList.
	 * @param root the root directory (search has depth 1)
	 * @param oldBeatmapList the old beatmap list to copy non-parsed fields from
	 */
	public static void parseAllFiles(File root, BeatmapSetList oldBeatmapList) {
		// create a new beatmap list
		BeatmapSetList.create();

		// create a new watch service
		if (Options.isWatchServiceEnabled())
			BeatmapWatchService.create();

		// parse all directories
		parseDirectories(root.listFiles(), oldBeatmapList);
	}

	/**
	 * Invokes parser for each directory in the given array and
	 * adds the beatmaps to the existing BeatmapSetList.
	 * @param dirs the array of directories to parse
	 * @return the last BeatmapSetNode parsed, or null if none
	 */
	public static BeatmapSetNode parseDirectories(File[] dirs) {
		return parseDirectories(dirs, null);
	}

	/**
	 * Invokes parser for each directory in the given array and
	 * adds the beatmaps to the existing BeatmapSetList.
	 * @param dirs the array of directories to parse
	 * @param oldBeatmapList the old beatmap list to copy non-parsed fields from
	 * @return the last BeatmapSetNode parsed, or null if none
	 */
	public static BeatmapSetNode parseDirectories(File[] dirs, BeatmapSetList oldBeatmapList) {
		if (dirs == null)
			return null;

		// progress tracking
		status = Status.PARSING;
		currentDirectoryIndex = 0;
		totalDirectories = dirs.length;

		// get last modified map from database
		Map<String, BeatmapDB.LastModifiedMapEntry> lastModifiedMap = BeatmapDB.getLastModifiedMap();

		// beatmap lists
		List<ArrayList<Beatmap>> allBeatmaps = new LinkedList<ArrayList<Beatmap>>();
		List<Beatmap> cachedBeatmaps = new LinkedList<Beatmap>();  // loaded from database
		List<Beatmap> parsedBeatmaps = new LinkedList<Beatmap>();  // loaded from parser

		// watch service
		BeatmapWatchService ws = (Options.isWatchServiceEnabled()) ? BeatmapWatchService.get() : null;

		// parse directories
		BeatmapSetNode lastNode = null;
		long timestamp = System.currentTimeMillis();
		for (File dir : dirs) {
			currentDirectoryIndex++;
			if (!dir.isDirectory())
				continue;

			// find all OSU files
			File[] files = dir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.toLowerCase().endsWith(".osu");
				}
			});
			if (files == null || files.length < 1)
				continue;

			// create a new group entry
			ArrayList<Beatmap> beatmaps = new ArrayList<Beatmap>(files.length);
			for (File file : files) {
				currentFile = file;

				// check if beatmap is cached
				String beatmapPath = String.format("%s/%s", dir.getName(), file.getName());
				if (lastModifiedMap != null) {
					BeatmapDB.LastModifiedMapEntry entry = lastModifiedMap.get(beatmapPath);
					if (entry != null) {
						// check last modified times
						if (entry.getLastModified() == file.lastModified()) {
							if (entry.getMode() == Beatmap.MODE_OSU) {  // only support standard mode
								// add to cached beatmap list
								Beatmap beatmap = new Beatmap(file);
								beatmaps.add(beatmap);
								cachedBeatmaps.add(beatmap);
							}
							continue;
						} else  // out of sync, delete cache entry and re-parse
							BeatmapDB.delete(dir.getName(), file.getName());
					}
				}

				// parse beatmap
				Beatmap beatmap = null;
				try {
					// Parse hit objects only when needed to save time/memory.
					// Change boolean to 'true' to parse them immediately.
					beatmap = parseFile(file, dir, beatmaps, false);
				} catch (Exception e) {
					ErrorHandler.error(String.format("Failed to parse beatmap file '%s'.",
							file.getAbsolutePath()), e, true);
				}

				// add to parsed beatmap list
				if (beatmap != null) {
					// copy non-parsed fields
					Beatmap oldBeatmap;
					if (oldBeatmapList != null && (oldBeatmap = oldBeatmapList.getBeatmapFromHash(beatmap.md5Hash)) != null)
						oldBeatmap.copyAdditionalFields(beatmap);

					// add timestamp
					if (beatmap.dateAdded < 1)
						beatmap.dateAdded = timestamp;

					// only support standard mode
					if (beatmap.mode == Beatmap.MODE_OSU)
						beatmaps.add(beatmap);

					parsedBeatmaps.add(beatmap);
				}
			}

			// add group entry if non-empty
			if (!beatmaps.isEmpty()) {
				beatmaps.trimToSize();
				allBeatmaps.add(beatmaps);
				if (ws != null)
					ws.registerAll(dir.toPath());
			}

			// stop parsing files (interrupted)
			if (Thread.interrupted())
				break;
		}

		// load cached entries from database
		if (!cachedBeatmaps.isEmpty()) {
			status = Status.CACHE;

			// Load array fields only when needed to save time/memory.
			// Change flag to 'LOAD_ALL' to load them immediately.
			BeatmapDB.load(cachedBeatmaps, BeatmapDB.LOAD_NONARRAY);
		}

		// add group entries to BeatmapSetList
		for (ArrayList<Beatmap> beatmaps : allBeatmaps) {
			Collections.sort(beatmaps);
			lastNode = BeatmapSetList.get().addSongGroup(beatmaps);
		}

		// clear string DB
		stringdb = new HashMap<String, String>();

		// add beatmap entries to database
		if (!parsedBeatmaps.isEmpty()) {
			status = Status.INSERTING;
			BeatmapDB.insert(parsedBeatmaps);
		}

		status = Status.NONE;
		currentFile = null;
		currentDirectoryIndex = -1;
		totalDirectories = -1;
		return lastNode;
	}

	/**
	 * Parses a beatmap.
	 * @param file the file to parse
	 * @param dir the directory containing the beatmap
	 * @param beatmaps the song group
	 * @param parseObjects if true, hit objects will be fully parsed now
	 * @return the new beatmap
	 */
	private static Beatmap parseFile(File file, File dir, ArrayList<Beatmap> beatmaps, boolean parseObjects) {
		Beatmap beatmap = new Beatmap(file);
		beatmap.timingPoints = new ArrayList<TimingPoint>();

		try (
			InputStream bis = new BufferedInputStream(new FileInputStream(file));
			MD5InputStreamWrapper md5stream = (!hasNoMD5Algorithm) ? new MD5InputStreamWrapper(bis) : null;
			BufferedReader in = new BufferedReader(new InputStreamReader((md5stream != null) ? md5stream : bis, "UTF-8"));
		) {
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
								File audioFileName = new File(dir, tokens[1]);
								if (!beatmaps.isEmpty()) {
									// if possible, reuse the same File object from another Beatmap in the group
									File groupAudioFileName = beatmaps.get(0).audioFilename;
									if (groupAudioFileName != null &&
									    tokens[1].equalsIgnoreCase(groupAudioFileName.getName()))
										audioFileName = groupAudioFileName;
								}
								if (!audioFileName.isFile()) {
									// try to find the file with a case-insensitive match
									boolean match = false;
									for (String s : dir.list()) {
										if (s.equalsIgnoreCase(tokens[1])) {
											audioFileName = new File(dir, s);
											match = true;
											break;
										}
									}
									if (!match) {
										Log.error(String.format("Audio file '%s' not found in directory '%s'.", tokens[1], dir.getName()));
										return null;
									}
								}
								beatmap.audioFilename = audioFileName;
								break;
							case "AudioLeadIn":
								beatmap.audioLeadIn = Integer.parseInt(tokens[1]);
								break;
//							case "AudioHash":  // deprecated
//								beatmap.audioHash = tokens[1];
//								break;
							case "PreviewTime":
								beatmap.previewTime = Integer.parseInt(tokens[1]);
								break;
							case "Countdown":
								beatmap.countdown = Byte.parseByte(tokens[1]);
								break;
							case "SampleSet":
								beatmap.sampleSet = getDBString(tokens[1]);
								break;
							case "StackLeniency":
								beatmap.stackLeniency = Float.parseFloat(tokens[1]);
								break;
							case "Mode":
								beatmap.mode = Byte.parseByte(tokens[1]);
								break;
							case "LetterboxInBreaks":
								beatmap.letterboxInBreaks = Utils.parseBoolean(tokens[1]);
								break;
							case "WidescreenStoryboard":
								beatmap.widescreenStoryboard = Utils.parseBoolean(tokens[1]);
								break;
							case "EpilepsyWarning":
								beatmap.epilepsyWarning = Utils.parseBoolean(tokens[1]);
								break;
//							case "SpecialStyle":  // mania only
//								beatmap.specialStyle = Utils.parseBoolean(tokens[1]);
//								break;
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
//								beatmap.bookmarks = new int[bookmarks.length];
//								for (int i = 0; i < bookmarks.length; i++)
//									osu.bookmarks[i] = Integer.parseInt(bookmarks[i]);
//								break;
//							case "DistanceSpacing":
//								beatmap.distanceSpacing = Float.parseFloat(tokens[1]);
//								break;
//							case "BeatDivisor":
//								beatmap.beatDivisor = Byte.parseByte(tokens[1]);
//								break;
//							case "GridSize":
//								beatmap.gridSize = Integer.parseInt(tokens[1]);
//								break;
//							case "TimelineZoom":
//								beatmap.timelineZoom = Integer.parseInt(tokens[1]);
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
								beatmap.title = getDBString(tokens[1]);
								break;
							case "TitleUnicode":
								beatmap.titleUnicode = getDBString(tokens[1]);
								break;
							case "Artist":
								beatmap.artist = getDBString(tokens[1]);
								break;
							case "ArtistUnicode":
								beatmap.artistUnicode = getDBString(tokens[1]);
								break;
							case "Creator":
								beatmap.creator = getDBString(tokens[1]);
								break;
							case "Version":
								beatmap.version = getDBString(tokens[1]);
								break;
							case "Source":
								beatmap.source = getDBString(tokens[1]);
								break;
							case "Tags":
								beatmap.tags = getDBString(tokens[1].toLowerCase());
								break;
							case "BeatmapID":
								beatmap.beatmapID = Integer.parseInt(tokens[1]);
								break;
							case "BeatmapSetID":
								beatmap.beatmapSetID = Integer.parseInt(tokens[1]);
								break;
							}
						} catch (Exception e) {
							Log.warn(String.format("Failed to read metadata '%s' for file '%s'.",
									line, file.getAbsolutePath()), e);
						}
						if (beatmap.beatmapSetID <= 0) {  // try to determine MSID from directory name
							if (dir != null && dir.isDirectory()) {
								String dirName = dir.getName();
								if (!dirName.isEmpty() && dirName.matches(DIR_MSID_PATTERN))
									beatmap.beatmapSetID = Integer.parseInt(dirName.substring(0, dirName.indexOf(' ')));
							}
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
								beatmap.HPDrainRate = Float.parseFloat(tokens[1]);
								break;
							case "CircleSize":
								beatmap.circleSize = Float.parseFloat(tokens[1]);
								break;
							case "OverallDifficulty":
								beatmap.overallDifficulty = Float.parseFloat(tokens[1]);
								break;
							case "ApproachRate":
								beatmap.approachRate = Float.parseFloat(tokens[1]);
								break;
							case "SliderMultiplier":
								beatmap.sliderMultiplier = Float.parseFloat(tokens[1]);
								break;
							case "SliderTickRate":
								beatmap.sliderTickRate = Float.parseFloat(tokens[1]);
								break;
							}
						} catch (Exception e) {
							Log.warn(String.format("Failed to read difficulty '%s' for file '%s'.",
									line, file.getAbsolutePath()), e);
						}
					}
					if (beatmap.approachRate == -1f)  // not in old format
						beatmap.approachRate = beatmap.overallDifficulty;
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
						case "0":  // background image
							tokens[2] = tokens[2].replaceAll("^\"|\"$", "");
							String ext = BeatmapParser.getExtension(tokens[2]);
							if (ext.equals("jpg") || ext.equals("png"))
								beatmap.bg = new File(dir, getDBString(tokens[2]));
							break;
						case "1":
						case "Video":  // background video
							tokens[2] = tokens[2].replaceAll("^\"|\"$", "");
							beatmap.video = new File(dir, getDBString(tokens[2]));
							beatmap.videoOffset = Integer.parseInt(tokens[1]);
							break;
						case "2":  // break periods
							try {
								if (beatmap.breaks == null)  // optional, create if needed
									beatmap.breaks = new ArrayList<Integer>();
								beatmap.breaks.add(Integer.parseInt(tokens[1]));
								beatmap.breaks.add(Integer.parseInt(tokens[2]));
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
					if (beatmap.breaks != null)
						beatmap.breaks.trimToSize();
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
							TimingPoint timingPoint = new TimingPoint(line);
							beatmap.timingPoints.add(timingPoint);

							// calculate BPM
							if (!timingPoint.isInherited()) {
								int bpm = Math.round(60000 / timingPoint.getBeatLength());
								if (beatmap.bpmMin == 0) {
									beatmap.bpmMin = beatmap.bpmMax = bpm;
								} else if (bpm < beatmap.bpmMin) {
									beatmap.bpmMin = bpm;
								} else if (bpm > beatmap.bpmMax) {
									beatmap.bpmMax = bpm;
								}
							}
						} catch (Exception e) {
							Log.warn(String.format("Failed to read timing point '%s' for file '%s'.",
									line, file.getAbsolutePath()), e);
						}
					}
					beatmap.timingPoints.trimToSize();
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
								Integer.parseInt(rgb[0]),
								Integer.parseInt(rgb[1]),
								Integer.parseInt(rgb[2])
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
							case "SliderBorder":
								beatmap.sliderBorder = color;
								break;
							default:
								break;
							}
						} catch (Exception e) {
							Log.warn(String.format("Failed to read color '%s' for file '%s'.",
									line, file.getAbsolutePath()), e);
						}
					}
					if (!colors.isEmpty())
						beatmap.combo = colors.toArray(new Color[colors.size()]);
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
							if ((type & HitObject.TYPE_CIRCLE) > 0)
								beatmap.hitObjectCircle++;
							else if ((type & HitObject.TYPE_SLIDER) > 0)
								beatmap.hitObjectSlider++;
							else //if ((type & HitObject.TYPE_SPINNER) > 0)
								beatmap.hitObjectSpinner++;
						} catch (Exception e) {
							Log.warn(String.format("Failed to read hit object '%s' for file '%s'.",
									line, file.getAbsolutePath()), e);
						}
					}

					try {
						// map length = last object end time (TODO: end on slider?)
						if ((type & HitObject.TYPE_SPINNER) > 0) {
							// some 'endTime' fields contain a ':' character (?)
							int index = tokens[5].indexOf(':');
							if (index != -1)
								tokens[5] = tokens[5].substring(0, index);
							beatmap.endTime = Integer.parseInt(tokens[5]);
						} else if (type != 0)
							beatmap.endTime = Integer.parseInt(tokens[2]);
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
			if (md5stream != null)
				beatmap.md5Hash = md5stream.getMD5();
		} catch (IOException e) {
			ErrorHandler.error(String.format("Failed to read file '%s'.", file.getAbsolutePath()), e, false);
		} catch (NoSuchAlgorithmException e) {
			ErrorHandler.error("Failed to get MD5 hash stream.", e, true);

			// retry without MD5
			hasNoMD5Algorithm = true;
			return parseFile(file, dir, beatmaps, parseObjects);
		}

		// no associated audio file?
		if (beatmap.audioFilename == null)
			return null;

		// parse hit objects now?
		if (parseObjects)
			parseHitObjects(beatmap);

		return beatmap;
	}

	/**
	 * Parses all hit objects in a beatmap.
	 * @param beatmap the beatmap to parse
	 */
	public static void parseHitObjects(Beatmap beatmap) {
		if (beatmap.objects != null)  // already parsed
			return;

		beatmap.objects = new HitObject[(beatmap.hitObjectCircle + beatmap.hitObjectSlider + beatmap.hitObjectSpinner)];

		try (BufferedReader in = new BufferedReader(new FileReader(beatmap.getFile()))) {
			String line = in.readLine();
			while (line != null) {
				line = line.trim();
				if (!line.equals("[HitObjects]"))
					line = in.readLine();
				else
					break;
			}
			if (line == null) {
				Log.warn(String.format("No hit objects found in Beatmap '%s'.", beatmap.toString()));
				return;
			}

			// combo info
			Color[] combo = beatmap.getComboColors();
			int comboIndex = 0;   // color index
			int comboNumber = 1;  // combo number

			int objectIndex = 0;
			boolean first = true;
			while ((line = in.readLine()) != null && objectIndex < beatmap.objects.length) {
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
					// create a new HitObject for each line
					HitObject hitObject = new HitObject(line);

					// set combo info
					// - new combo: get next combo index, reset combo number
					// - else:      maintain combo index, increase combo number
					if (hitObject.isNewCombo() || first) {
						int skip = (hitObject.isSpinner() ? 0 : 1) + hitObject.getComboSkip();
						for (int i = 0; i < skip; i++) {
							comboIndex = (comboIndex + 1) % combo.length;
							comboNumber = 1;
						}
						first = false;
					}

					hitObject.setComboIndex(comboIndex);
					hitObject.setComboNumber(comboNumber++);

					beatmap.objects[objectIndex++] = hitObject;
				} catch (Exception e) {
					Log.warn(String.format("Failed to read hit object '%s' for beatmap '%s'.",
							line, beatmap.toString()), e);
				}
			}

			// check that all objects were parsed
			if (objectIndex != beatmap.objects.length)
				ErrorHandler.error(String.format("Parsed %d objects for beatmap '%s', %d objects expected.",
						objectIndex, beatmap.toString(), beatmap.objects.length), null, true);
		} catch (IOException e) {
			ErrorHandler.error(String.format("Failed to read file '%s'.", beatmap.getFile().getAbsolutePath()), e, false);
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
	 * @param file the file name
	 */
	public static String getExtension(String file) {
		int i = file.lastIndexOf('.');
		return (i != -1) ? file.substring(i + 1).toLowerCase() : "";
	}

	/**
	 * Returns the name of the current file being parsed, or null if none.
	 */
	public static String getCurrentFileName() {
		if (status == Status.PARSING)
			return (currentFile != null) ? currentFile.getName() : null;
		else
			return (status == Status.NONE) ? null : "";
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

	/**
	 * Returns the current parser status.
	 */
	public static Status getStatus() { return status; }

	/**
	 * Returns the String object in the database for the given String.
	 * If none, insert the String into the database and return the original String.
	 * @param s the string to retrieve
	 * @return the string object
	 */
	public static String getDBString(String s) {
		String DBString = stringdb.get(s);
		if (DBString == null) {
			stringdb.put(s, s);
			return s;
		} else
			return DBString;
	}
}