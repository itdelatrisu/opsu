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

import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.options.Options;
import itdelatrisu.opsu.ui.UI;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * Unpacker for OSZ (ZIP) archives.
 */
public class OszUnpacker {
	/** The index of the current file being unpacked. */
	private static int fileIndex = -1;

	/** The total number of files to unpack. */
	private static File[] files;

	// This class should not be instantiated.
	private OszUnpacker() {}

	/**
	 * Invokes the unpacker for each OSZ archive in a root directory.
	 * @param root the root directory
	 * @param dest the destination directory
	 * @return an array containing the new (unpacked) directories
	 */
	public static File[] unpackAllFiles(File root, File dest) {
		List<File> dirs = new ArrayList<File>();

		// find all OSZ files
		files = root.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".osz");
			}
		});
		if (files == null || files.length < 1) {
			files = null;
			return new File[0];
		}

		// unpack OSZs
		BeatmapWatchService ws = (Options.isWatchServiceEnabled()) ? BeatmapWatchService.get() : null;
		if (ws != null)
			ws.pause();
		for (File file : files) {
			fileIndex++;
			String dirName = file.getName().substring(0, file.getName().lastIndexOf('.'));
			File songDir = new File(dest, dirName);
			if (!songDir.isDirectory()) {
				songDir.mkdir();
				Utils.unzip(file, songDir);
				file.delete();  // delete the OSZ when finished
				dirs.add(songDir);
			}
		}
		if (ws != null)
			ws.resume();

		fileIndex = -1;
		files = null;

		if (!dirs.isEmpty()) {
			String text = String.format("Imported %d new beatmap pack%s.", dirs.size(), dirs.size() == 1 ? "" : "s");
			UI.getNotificationManager().sendNotification(text);
		}

		return dirs.toArray(new File[dirs.size()]);
	}

	/**
	 * Returns the name of the current file being unpacked, or null if none.
	 */
	public static String getCurrentFileName() {
		if (files == null || fileIndex == -1)
			return null;

		return files[fileIndex].getName();
	}

	/**
	 * Returns the progress of file unpacking, or -1 if not unpacking.
	 * @return the completion percent [0, 100] or -1
	 */
	public static int getUnpackerProgress() {
		if (files == null || fileIndex == -1)
			return -1;

		return (fileIndex + 1) * 100 / files.length;
	}
}