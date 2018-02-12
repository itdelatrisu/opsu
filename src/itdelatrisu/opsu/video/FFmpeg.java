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

package itdelatrisu.opsu.video;

import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.options.Options;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

import craterstudio.io.Streams;
import craterstudio.streams.NullOutputStream;
import craterstudio.text.RegexUtil;
import craterstudio.text.TextValues;
import net.indiespot.media.impl.Extractor;
import net.indiespot.media.impl.VideoMetadata;

/**
 * FFmpeg utilities.
 *
 * @author Riven (base)
 */
public class FFmpeg {
	/** The default file name of the FFmpeg shared library. */
	public static final String DEFAULT_NATIVE_FILENAME = getDefaultNativeFilename();

	/** The FFmpeg shared library location. */
	private static File FFMPEG_PATH = null;

	/** Whether to print FFmpeg errors to stderr. */
	private static final boolean FFMPEG_VERBOSE = false;

	/**
	 * Returns the expected file name of the FFmpeg shared library, based on
	 * the current operating system and architecture.
	 */
	private static String getDefaultNativeFilename() {
		String resourceName = "ffmpeg";
		if (Extractor.isMac)
			resourceName += "-mac";
		else {
			resourceName += Extractor.is64bit ? "64" : "32";
			if (Extractor.isWindows)
				resourceName += ".exe";
		}
		return resourceName;
	}

	/** Returns the FFmpeg shared library location. */
	private static File getNativeLocation() {
		File customLocation = Options.getFFmpegLocation();
		if (customLocation != null && customLocation.isFile())
			return customLocation;
		else
			return FFMPEG_PATH;
	}

	/** Sets the directory in which to look for the FFmpeg shared library. */
	public static void setNativeDir(File dir) {
		FFMPEG_PATH = new File(dir, DEFAULT_NATIVE_FILENAME);
	}

	/** Returns whether the FFmpeg shared library could be found. */
	public static boolean exists() {
		File location = getNativeLocation();
		return location != null && location.isFile();
	}

	/**
	 * Extracts the metadata for a video file.
	 * @param srcMovieFile the source video file
	 */
	public static VideoMetadata extractMetadata(File srcMovieFile) throws IOException {
		File ffmpegFile = getNativeLocation();
		Utils.setExecutable(ffmpegFile);

		Process process = new ProcessBuilder().command(
			ffmpegFile.getAbsolutePath(),
			"-i", srcMovieFile.getAbsolutePath(),
			"-f", "null"
		).start();
		Streams.asynchronousTransfer(process.getInputStream(), System.out, true, false);

		int width = -1, height = -1;
		float framerate = -1;

		try {
			InputStream stderr = process.getErrorStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(stderr));
			for (String line; (line = br.readLine()) != null;) {
				// Look for:
				// "	Stream #0:0: Video: vp6f, yuv420p, 320x240, 314 kb/s, 30 tbr, 1k tbn, 1k tbc"
				// ----------------------------------------------------------^
				if (line.trim().startsWith("Stream #") && line.contains("Video:")) {
					// Parse framerate
					// Note: Can contain 'k' suffix (*1000), see dump.c#print_fps.
					// https://www.ffmpeg.org/doxygen/3.1/dump_8c_source.html#l00119
					String[] fr = RegexUtil.find(line, Pattern.compile("\\s(\\d+(\\.\\d+)?)(k?)\\stbr,"), 1, 3);
					framerate = Float.parseFloat(fr[0]);
					if (!fr[1].isEmpty())
						framerate *= 1000f;

					// Parse width/height
					int[] wh = TextValues.parseInts(RegexUtil.find(line, Pattern.compile("\\s(\\d+)x(\\d+)[\\s,]"), 1, 2));
					width = wh[0];
					height = wh[1];
				}
			}

			if (framerate == -1)
				throw new IOException("Failed to find framerate of video.");

			return new VideoMetadata(width, height, framerate);
		} finally {
			Streams.safeClose(process);
		}
	}

	/**
	 * Returns an RGB24 video stream.
	 * @param srcMovieFile the source video file
	 * @param msec the time offset (in milliseconds)
	 */
	public static InputStream extractVideoAsRGB24(File srcMovieFile, int msec) throws IOException {
		File ffmpegFile = getNativeLocation();
		Utils.setExecutable(ffmpegFile);

		return streamData(new ProcessBuilder().command(
			ffmpegFile.getAbsolutePath(),
			"-ss", String.format("%d.%d", msec / 1000, msec % 1000),
			"-i", srcMovieFile.getAbsolutePath(),
			"-f", "rawvideo",
			"-pix_fmt", "rgb24",
			"-"
		));
	}

	/** Returns a stream. */
	private static InputStream streamData(ProcessBuilder pb) throws IOException {
		Process process = pb.start();
		Streams.asynchronousTransfer(process.getErrorStream(), FFMPEG_VERBOSE ? System.err : new NullOutputStream(), true, false);
		return process.getInputStream();
	}
}
