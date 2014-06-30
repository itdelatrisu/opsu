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

import java.io.File;
import java.util.ArrayList;

import org.newdawn.slick.Color;
import org.newdawn.slick.Image;
import org.newdawn.slick.util.Log;

/**
 * Data type storing parsed data from OSU files.
 */
public class OsuFile implements Comparable<OsuFile> {
	/**
	 * The OSU File object associated with this OsuFile.
	 */
	private File file;

	/* [General] */
	public File audioFilename;                  // audio file object
	public int audioLeadIn = 0;                 // delay before music starts (in ms)
//	public String audioHash = "";               // audio hash (deprecated)
	public int previewTime = -1;                // start position of music preview (in ms)
	public byte countdown = 0;                  // countdown type (0:disabled, 1:normal, 2:half, 3:double)
	public String sampleSet = "";               // ? ("Normal", "Soft")
	public float stackLeniency = 0.7f;          // how often closely placed hit objects will be stacked together
	public byte mode = 0;                       // game mode (0:osu!, 1:taiko, 2:catch the beat, 3:osu!mania)
	public boolean letterboxInBreaks = false;   // whether the letterbox (top/bottom black bars) appears during breaks
	public boolean widescreenStoryboard = false;// whether the storyboard should be widescreen
	public boolean epilepsyWarning = false;     // whether to show an epilepsy warning

	/* [Editor] */
	/* Not implemented. */
//	public int[] bookmarks;                     // list of editor bookmarks (in ms)
//	public float distanceSpacing = 0f;          // multiplier for "Distance Snap"
//	public byte beatDivisor = 0;                // beat division
//	public int gridSize = 0;                    // size of grid for "Grid Snap"
//	public int timelineZoom = 0;                // zoom in the editor timeline

	/* [Metadata] */
	public String title = "";                   // song title
	public String titleUnicode = "";            // song title (unicode)
	public String artist = "";                  // song artist
	public String artistUnicode = "";           // song artist (unicode)
	public String creator = "";                 // beatmap creator
	public String version = "";                 // beatmap difficulty
	public String source = "";                  // song source
//	public String[] tags;                       // song tags, for searching -> different structure
	public int beatmapID = 0;                   // beatmap ID
	public int beatmapSetID = 0;                // beatmap set ID

	/* [Difficulty] */
	public float HPDrainRate = 5f;              // HP drain (0:easy ~ 10:hard)
	public float circleSize = 4f;               // size of circles
	public float overallDifficulty = 5f;        // affects timing window, spinners, and approach speed (0:easy ~ 10:hard)
	public float approachRate = -1f;            // how long circles stay on the screen (0:long ~ 10:short) **not in old format**
	public float sliderMultiplier = 1f;         // slider movement speed multiplier
	public float sliderTickRate = 1f;           // rate at which slider ticks are placed (x per beat)

	/* [Events] */
	//Background and Video events (0)
	public String bg;                           // background image path
	private Image bgImage;                      // background image (created when needed)
//	public Video bgVideo;                       // background video (not implemented)
	//Break Periods (2)
	public ArrayList<Integer> breaks;           // break periods (start time, end time, ...)
	//Storyboard elements (not implemented)

	/* [TimingPoints] */
	public ArrayList<OsuTimingPoint> timingPoints; // timing points
	int bpmMin = 0, bpmMax = 0;                 // min and max BPM

	/* [Colours] */
	public Color[] combo;                       // combo colors (R,G,B), max 5

	/* [HitObjects] */
	public OsuHitObject[] objects;              // hit objects
	public int hitObjectCircle = 0;             // number of circles
	public int hitObjectSlider = 0;             // number of sliders
	public int hitObjectSpinner = 0;            // number of spinners

	/**
	 * Constructor.
	 * @param file the file associated with this OsuFile
	 */
	public OsuFile(File file) {
		this.file = file;
	}

	/**
	 * Returns the associated file object.
	 */
	public File getFile() { return file; }

	/**
	 * Draws the background associated with the OsuFile.
	 * @param width the container width
	 * @param height the container height
	 * @param alpha the alpha value
	 * @return true if successful, false if any errors were produced
	 */
	public boolean drawBG(int width, int height, float alpha) {
		if (bg == null)
			return false;
		try {
			if (bgImage == null)
				bgImage = new Image(bg).getScaledCopy(width, height);
			bgImage.setAlpha(alpha);
			bgImage.draw();
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
		return String.format("%s - %s [%s]", artist, title, version);
	}
}