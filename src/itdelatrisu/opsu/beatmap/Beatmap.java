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

import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.options.Options;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;

import org.newdawn.slick.Color;
import org.newdawn.slick.Image;
import org.newdawn.slick.util.Log;

/**
 * Beatmap structure storing data parsed from OSU files.
 */
public class Beatmap implements Comparable<Beatmap> {
	/** Game modes. */
	public static final byte MODE_OSU = 0, MODE_TAIKO = 1, MODE_CTB = 2, MODE_MANIA = 3;

	/** Background image cache. */
	@SuppressWarnings("serial")
	private static final LRUCache<File, ImageLoader> bgImageCache = new LRUCache<File, ImageLoader>(10) {
		@Override
		public void eldestRemoved(Map.Entry<File, ImageLoader> eldest) {
			if (eldest.getKey() == lastBG)
				lastBG = null;
			ImageLoader imageLoader = eldest.getValue();
			imageLoader.destroy();
		}
	};

	/** The last background image loaded. */
	private static File lastBG;

	/**
	 * Clears the background image cache.
	 * <p>
	 * NOTE: This does NOT destroy the images in the cache, and will cause
	 * memory leaks if all images have not been destroyed.
	 */
	public static void clearBackgroundImageCache() { bgImageCache.clear(); }

	/** The OSU File object associated with this beatmap. */
	private File file;

	/** MD5 hash of this file. */
	public String md5Hash;

	/** The star rating. */
	public double starRating = -1;

	/** The timestamp this beatmap was first loaded. */
	public long dateAdded = 0;

	/** Whether this beatmap is marked as a "favorite". */
	public boolean favorite = false;

	/** Total number of times this beatmap has been played. */
	public int playCount = 0;

	/** The last time this beatmap was played (timestamp). */
	public long lastPlayed = 0;

	/** The local music offset. */
	public int localMusicOffset = 0;

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

	/** Game mode (MODE_* constants). */
	public byte mode = MODE_OSU;

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
	public float circleSize = 5f;

	/** OD: Affects timing window, spinners, and approach speed (0:easy ~ 10:hard). */
	public float overallDifficulty = 5f;

	/** AR: How long circles stay on the screen (0:long ~ 10:short). */
	public float approachRate = -1f;

	/** Slider movement speed multiplier. */
	public float sliderMultiplier = 1.4f;

	/** Rate at which slider ticks are placed (x per beat). */
	public float sliderTickRate = 1f;

	/**
	 * [Events]
	 */

	/** Background image file. */
	public File bg;

	/** Background video file. */
	public File video;

	/** Background video offset time. */
	public int videoOffset = 0;

	/** All break periods (start time, end time, ...). */
	public ArrayList<Integer> breaks;

	/**
	 * [TimingPoints]
	 */

	/** All timing points. */
	public ArrayList<TimingPoint> timingPoints;

	/** Song BPM range. */
	public int bpmMin = 0, bpmMax = 0;

	/**
	 * [Colours]
	 */

	/** Combo colors (max 8). If null, the skin value is used. */
	public Color[] combo;

	/** Slider border color. If null, the skin value is used. */
	public Color sliderBorder;

	/**
	 * [HitObjects]
	 */

	/** All hit objects. */
	public HitObject[] objects;

	/** Number of individual objects. */
	public int
		hitObjectCircle = 0,
		hitObjectSlider = 0,
		hitObjectSpinner = 0;

	/** Last object end time (in ms). */
	public int endTime = -1;

	/**
	 * Constructor.
	 * @param file the file associated with this beatmap
	 */
	public Beatmap(File file) {
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
	 * Returns the list of combo colors (max 8).
	 * If the beatmap does not provide colors, the skin colors will be returned instead.
	 * @return the combo colors
	 */
	public Color[] getComboColors() {
		return (combo != null) ? combo : Options.getSkin().getComboColors();
	}

	/**
	 * Returns the slider border color.
	 * If the beatmap does not provide a color, the skin color will be returned instead.
	 * @return the slider border color
	 */
	public Color getSliderBorderColor() {
		return (sliderBorder != null) ? sliderBorder : Options.getSkin().getSliderBorderColor();
	}

	/**
	 * Loads the beatmap background image.
	 */
	public void loadBackground() {
		if (bg == null || bgImageCache.containsKey(bg) || !bg.isFile())
			return;

		if (lastBG != null) {
			ImageLoader lastImageLoader = bgImageCache.get(lastBG);
			if (lastImageLoader != null && lastImageLoader.isLoading()) {
				lastImageLoader.interrupt();  // only allow loading one image at a time
				bgImageCache.remove(lastBG);
			}
		}
		ImageLoader imageLoader = new ImageLoader(bg);
		bgImageCache.put(bg, imageLoader);
		imageLoader.load(true);
		lastBG = bg;
	}

	/**
	 * Returns whether the beatmap background image is currently loading.
	 * @return true if loading
	 */
	public boolean isBackgroundLoading() {
		if (bg == null)
			return false;
		ImageLoader imageLoader = bgImageCache.get(bg);
		return (imageLoader != null && imageLoader.isLoading());
	}

	/**
	 * Returns whether there is a loaded beatmap background image.
	 * @return true if an image is currently available
	 */
	public boolean hasLoadedBackground() {
		if (bg == null)
			return false;
		ImageLoader imageLoader = bgImageCache.get(bg);
		return (imageLoader != null && imageLoader.getImage() != null);
	}

	/**
	 * Draws the beatmap background image.
	 * @param width the container width
	 * @param height the container height
	 * @param offsetX the x offset (from the screen center)
	 * @param offsetY the y offset (from the screen center)
	 * @param alpha the alpha value
	 * @param stretch if true, stretch to screen dimensions; otherwise, maintain aspect ratio
	 * @return true if successful, false if any errors were produced
	 */
	public boolean drawBackground(int width, int height, float offsetX, float offsetY, float alpha, boolean stretch) {
		if (bg == null)
			return false;

		ImageLoader imageLoader = bgImageCache.get(bg);
		if (imageLoader == null)
			return false;

		Image bgImage = imageLoader.getImage();
		if (bgImage == null)
			return true;

		int swidth = width;
		int sheight = height;
		if (!stretch) {
			// fit image to screen
			if (bgImage.getWidth() / (float) bgImage.getHeight() > width / (float) height)  // x > y
				sheight = (int) (width * bgImage.getHeight() / (float) bgImage.getWidth());
			else
				swidth = (int) (height * bgImage.getWidth() / (float) bgImage.getHeight());
		} else {
			// fill screen while maintaining aspect ratio
			if (bgImage.getWidth() / (float) bgImage.getHeight() > width / (float) height)  // x > y
				swidth = (int) (height * bgImage.getWidth() / (float) bgImage.getHeight());
			else
				sheight = (int) (width * bgImage.getHeight() / (float) bgImage.getWidth());
		}
		if (Options.isParallaxEnabled()) {
			swidth = (int) (swidth * GameImage.PARALLAX_SCALE);
			sheight = (int) (sheight * GameImage.PARALLAX_SCALE);
		}
		bgImage = bgImage.getScaledCopy(swidth, sheight);
		bgImage.setAlpha(alpha);
		if (!Options.isParallaxEnabled() && offsetX == 0f && offsetY == 0f)
			bgImage.drawCentered(width / 2, height / 2);
		else
			bgImage.drawCentered(width / 2 + offsetX, height / 2 + offsetY);
		return true;
	}

	/**
	 * Compares two Beatmap objects first by overall difficulty, then by total objects.
	 */
	@Override
	public int compareTo(Beatmap that) {
		int cmp = Float.compare(this.overallDifficulty, that.overallDifficulty);
		if (cmp == 0)
			cmp = Integer.compare(
					this.hitObjectCircle + this.hitObjectSlider + this.hitObjectSpinner,
					that.hitObjectCircle + that.hitObjectSlider + that.hitObjectSpinner
			);
		return cmp;
	}

	/**
	 * Returns a formatted string: "Source (Artist) - Title [Version]"
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if (!source.isEmpty()) 
			return String.format("%s (%s) - %s [%s]", source, getArtist(), getTitle(), version);

		return String.format("%s - %s [%s]", getArtist(), getTitle(), version);
	}

	/**
	 * Returns the {@link #breaks} field formatted as a string,
	 * or null if the field is null.
	 */
	public String breaksToString() {
		if (breaks == null)
			return null;

		StringBuilder sb = new StringBuilder();
		for (int i : breaks) {
			sb.append(i);
			sb.append(',');
		}
		if (sb.length() > 0)
			sb.setLength(sb.length() - 1);
		return sb.toString();
	}

	/**
	 * Sets the {@link #breaks} field from a string.
	 * @param s the string
	 */
	public void breaksFromString(String s) {
		if (s == null)
			return;

		this.breaks = new ArrayList<Integer>();
		String[] tokens = s.split(",");
		for (int i = 0; i < tokens.length; i++)
			breaks.add(Integer.parseInt(tokens[i]));
	}

	/**
	 * Returns the {@link #timingPoints} field formatted as a string,
	 * or null if the field is null.
	 */
	public String timingPointsToString() {
		if (timingPoints == null)
			return null;

		StringBuilder sb = new StringBuilder();
		for (TimingPoint p : timingPoints) {
			sb.append(p.toString());
			sb.append('|');
		}
		if (sb.length() > 0)
			sb.setLength(sb.length() - 1);
		return sb.toString();
	}

	/**
	 * Sets the {@link #timingPoints} field from a string.
	 * @param s the string
	 */
	public void timingPointsFromString(String s) {
		this.timingPoints = new ArrayList<TimingPoint>();
		if (s == null)
			return;

		String[] tokens = s.split("\\|");
		for (int i = 0; i < tokens.length; i++) {
			try {
				timingPoints.add(new TimingPoint(tokens[i]));
			} catch (Exception e) {
				Log.warn(String.format("Failed to read timing point '%s'.", tokens[i]), e);
			}
		}
		timingPoints.trimToSize();
	}

	/**
	 * Returns the {@link #combo} field formatted as a string,
	 * or null if the field is null or the default combo.
	 */
	public String comboToString() {
		if (combo == null)
			return null;

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < combo.length; i++) {
			Color c = combo[i];
			sb.append(c.getRed());
			sb.append(',');
			sb.append(c.getGreen());
			sb.append(',');
			sb.append(c.getBlue());
			sb.append('|');
		}
		if (sb.length() > 0)
			sb.setLength(sb.length() - 1);
		return sb.toString();
	}

	/**
	 * Sets the {@link #combo} field from a string.
	 * @param s the string
	 */
	public void comboFromString(String s) {
		if (s == null)
			return;

		LinkedList<Color> colors = new LinkedList<Color>();
		String[] tokens = s.split("\\|");
		for (int i = 0; i < tokens.length; i++) {
			String[] rgb = tokens[i].split(",");
			colors.add(new Color(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2])));
		}
		if (!colors.isEmpty())
			this.combo = colors.toArray(new Color[colors.size()]);
	}

	/**
	 * Returns the {@link #sliderBorder} field formatted as a string,
	 * or null if the field is null.
	 */
	public String sliderBorderToString() {
		if (sliderBorder == null)
			return null;

		return String.format("%d,%d,%d", sliderBorder.getRed(), sliderBorder.getGreen(), sliderBorder.getBlue());
	}

	/**
	 * Sets the {@link #sliderBorder} field from a string.
	 * @param s the string
	 */
	public void sliderBorderFromString(String s) {
		if (s == null)
			return;

		String[] rgb = s.split(",");
		this.sliderBorder = new Color(new Color(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2])));
	}

	/**
	 * Increments the play counter and last played time.
	 */
	public void incrementPlayCounter() {
		this.playCount++;
		this.lastPlayed = System.currentTimeMillis();
	}

	/**
	 * Copies non-parsed fields from this beatmap into another beatmap.
	 * @param target the target beatmap
	 */
	public void copyAdditionalFields(Beatmap target) {
		target.starRating = starRating;
		target.dateAdded = dateAdded;
		target.favorite = favorite;
		target.playCount = playCount;
		target.lastPlayed = lastPlayed;
		target.localMusicOffset = localMusicOffset;
	}
}
