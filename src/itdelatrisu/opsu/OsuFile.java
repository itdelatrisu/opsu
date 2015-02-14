/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014, 2015 Jeffrey Han
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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import org.newdawn.slick.Color;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.util.Log;

/**
 * Data type storing parsed data from OSU files.
 */
public class OsuFile implements Comparable<OsuFile> {
	/** Map of all loaded background images. */
	private static HashMap<OsuFile, Image> bgImageMap = new HashMap<OsuFile, Image>();

	/** Maximum number of cached images before all get erased. */
	private static final int MAX_CACHE_SIZE = 10;

	/** The OSU File object associated with this OsuFile. */
	private File file;

	/**
	 * [General]
	 */

	/** Audio file object. */
	public File audioFilename;

	/** Delay time before music starts (in ms). */
	public int audioLeadIn = 0;

	/** Audio hash (deprecated). */
//	public String audioHash = "";

	/** Start position of music preview (in ms). */
	public int previewTime = -1;

	/** Countdown type (0:disabled, 1:normal, 2:half, 3:double). */
	public byte countdown = 0;

	/** Sound samples ("None", "Normal", "Soft"). */
	public String sampleSet = "";

	/** How often closely placed hit objects will be stacked together. */
	public float stackLeniency = 0.7f;

	/** Game mode (0:osu!, 1:taiko, 2:catch the beat, 3:osu!mania). */
	public byte mode = 0;

	/** Whether the letterbox (top/bottom black bars) appears during breaks. */
	public boolean letterboxInBreaks = false;

	/** Whether the storyboard should be widescreen. */
	public boolean widescreenStoryboard = false;

	/** Whether to show an epilepsy warning. */
	public boolean epilepsyWarning = false;

	/**
	 * [Editor]
	 */

	/** List of editor bookmarks (in ms). */
//	public int[] bookmarks;

	/** Multiplier for "Distance Snap". */
//	public float distanceSpacing = 0f;

	/** Beat division. */
//	public byte beatDivisor = 0;

	/** Size of grid for "Grid Snap". */
//	public int gridSize = 0;

	/** Zoom in the editor timeline. */
//	public int timelineZoom = 0;

	/**
	 * [Metadata]
	 */

	/** Song title. */
	public String title = "", titleUnicode = "";

	/** Song artist. */
	public String artist = "", artistUnicode = "";

	/** Beatmap creator. */
	public String creator = "";

	/** Beatmap difficulty. */
	public String version = "";

	/** Song source. */
	public String source = "";

	/** Song tags (for searching). */
	public String tags = "";

	/** Beatmap ID. */
	public int beatmapID = 0;

	/** Beatmap set ID. */
	public int beatmapSetID = 0;

	/**
	 * [Difficulty]
	 */

	/** HP: Health drain rate (0:easy ~ 10:hard) */
	public float HPDrainRate = 5f;

	/** CS: Size of circles and sliders (0:large ~ 10:small). */
	public float circleSize = 4f;

	/** OD: Affects timing window, spinners, and approach speed (0:easy ~ 10:hard). */
	public float overallDifficulty = 5f;

	/** AR: How long circles stay on the screen (0:long ~ 10:short). */
	public float approachRate = -1f;

	/** Slider movement speed multiplier. */
	public float sliderMultiplier = 1f;

	/** Rate at which slider ticks are placed (x per beat). */
	public float sliderTickRate = 1f;

	/**
	 * [Events]
	 */

	/** Background image file name. */
	public String bg;

	/** Background video file name. */
//	public String video;

	/** All break periods (start time, end time, ...). */
	public ArrayList<Integer> breaks;

	/**
	 * [TimingPoints]
	 */

	/** All timing points. */
	public ArrayList<OsuTimingPoint> timingPoints;

	/** Song BPM range. */
	int bpmMin = 0, bpmMax = 0;

	/**
	 * [Colours]
	 */

	/** Combo colors (max 8). */
	public Color[] combo;

	/**
	 * [HitObjects]
	 */

	/** All hit objects. */
	public OsuHitObject[] objects;

	/** Number of individual objects. */
	public int
		hitObjectCircle = 0,
		hitObjectSlider = 0,
		hitObjectSpinner = 0;

	/** Last object end time (in ms). */
	public int endTime = -1;

	/**
	 * Destroys all cached background images and resets the cache.
	 */
	public static void clearImageCache() {
		for (Image img : bgImageMap.values()) {
			if (img != null && !img.isDestroyed()) {
				try {
					img.destroy();
				} catch (SlickException e) {
					Log.warn(String.format("Failed to destroy image '%s'.", img.getResourceReference()), e);
				}
			}
		}
		resetImageCache();
	}

	/**
	 * Resets the image cache.
	 * This does NOT destroy images, so be careful of memory leaks!
	 */
	public static void resetImageCache() {
		bgImageMap = new HashMap<OsuFile, Image>();
	}

	/**
	 * Constructor.
	 * @param file the file associated with this OsuFile
	 */
	public OsuFile(File file) {
		this.file = file;
	}

	/**
	 * Returns the associated file object.
	 * @return the File object
	 */
	public File getFile() { return file; }

	/**
	 * Returns the song title.
	 * If configured, the Unicode string will be returned instead.
	 * @return the song title
	 */
	public String getTitle() {
		return (Options.useUnicodeMetadata() && !titleUnicode.isEmpty()) ? titleUnicode : title;
	}

	/**
	 * Returns the song artist.
	 * If configured, the Unicode string will be returned instead.
	 * @return the song artist
	 */
	public String getArtist() {
		return (Options.useUnicodeMetadata() && !artistUnicode.isEmpty()) ? artistUnicode : artist;
	}

	/**
	 * Draws the background associated with the OsuFile.
	 * @param width the container width
	 * @param height the container height
	 * @param alpha the alpha value
	 * @param stretch if true, stretch to screen dimensions; otherwise, maintain aspect ratio
	 * @return true if successful, false if any errors were produced
	 */
	public boolean drawBG(int width, int height, float alpha, boolean stretch) {
		if (bg == null)
			return false;
		try {
			Image bgImage = bgImageMap.get(this);
			if (bgImage == null) {
				if (bgImageMap.size() > MAX_CACHE_SIZE)
					clearImageCache();
				bgImage = new Image(bg);
				bgImageMap.put(this, bgImage);
			}

			int swidth = width;
			int sheight = height;
			if (!stretch) {
				// fit image to screen
				if (bgImage.getWidth() / (float) bgImage.getHeight() > width / (float) height)  // x > y
					sheight = (int) (width * bgImage.getHeight() / (float) bgImage.getWidth());
				else
					swidth = (int) (height * bgImage.getWidth() / (float) bgImage.getHeight());
			} else {
				//fill image to screen while keeping aspect ratio
				if (bgImage.getWidth() / (float) bgImage.getHeight() > width / (float) height)  // x > y
					swidth = (int) (height * bgImage.getWidth() / (float) bgImage.getHeight());
				else
					sheight = (int) (width * bgImage.getHeight() / (float) bgImage.getWidth());
			}
			bgImage = bgImage.getScaledCopy(swidth, sheight);

			bgImage.setAlpha(alpha);
			bgImage.drawCentered(width / 2, height / 2);
		} catch (Exception e) {
			Log.warn(String.format("Failed to get background image '%s'.", bg), e);
			bg = null;  // don't try to load the file again until a restart
			return false;
		}
		return true;
	}

	/**
	 * Compares two OsuFile objects first by overall difficulty, then by total objects.
	 */
	@Override
	public int compareTo(OsuFile that) {
		int cmp = Float.compare(this.overallDifficulty, that.overallDifficulty);
		if (cmp == 0)
			cmp = Integer.compare(
					this.hitObjectCircle + this.hitObjectSlider + this.hitObjectSpinner,
					that.hitObjectCircle + that.hitObjectSlider + that.hitObjectSpinner
			);
		return cmp;
	}

	/**
	 * Returns a formatted string: "Artist - Title [Version]"
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("%s - %s [%s]", getArtist(), getTitle(), version);
	}
}