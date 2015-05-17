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

package itdelatrisu.opsu.beatmap;

import itdelatrisu.opsu.GameData.Grade;
import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.GameMod;
import itdelatrisu.opsu.Options;
import itdelatrisu.opsu.Utils;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.newdawn.slick.Color;
import org.newdawn.slick.Image;

/**
 * Node in an BeatmapSetList representing a group of beatmaps.
 */
public class BeatmapSetNode {
	/** List of associated beatmaps. */
	public ArrayList<Beatmap> beatmaps;

	/** Index of this node. */
	public int index = 0;

	/** Index of the selected beatmap (-1 if not focused). */
	public int beatmapIndex = -1;

	/** Links to other nodes. */
	public BeatmapSetNode prev, next;

	/**
	 * Constructor.
	 * @param beatmaps the beatmaps in this group
	 */
	public BeatmapSetNode(ArrayList<Beatmap> beatmaps) {
		this.beatmaps = beatmaps;
	}

	/**
	 * Draws the button.
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param grade the highest grade, if any
	 * @param focus true if this is the focused node
	 */
	public void draw(float x, float y, Grade grade, boolean focus) {
		Image bg = GameImage.MENU_BUTTON_BG.getImage();
		boolean expanded = (beatmapIndex > -1);
		Beatmap beatmap;
		bg.setAlpha(0.9f);
		Color bgColor;
		Color textColor = Color.lightGray;

		// get drawing parameters
		if (expanded) {
			x -= bg.getWidth() / 10f;
			if (focus) {
				bgColor = Color.white;
				textColor = Color.white;
			} else
				bgColor = Utils.COLOR_BLUE_BUTTON;
			beatmap = beatmaps.get(beatmapIndex);
		} else {
			bgColor = Utils.COLOR_ORANGE_BUTTON;
			beatmap = beatmaps.get(0);
		}
		bg.draw(x, y, bgColor);

		float cx = x + (bg.getWidth() * 0.043f);
		float cy = y + (bg.getHeight() * 0.2f) - 3;

		// draw grade
		if (grade != Grade.NULL) {
			Image gradeImg = grade.getMenuImage();
			gradeImg.drawCentered(cx - bg.getWidth() * 0.01f + gradeImg.getWidth() / 2f, y + bg.getHeight() / 2.2f);
			cx += gradeImg.getWidth();
		}

		// draw text
		if (Options.useUnicodeMetadata()) {  // load glyphs
			Utils.loadGlyphs(Utils.FONT_MEDIUM, beatmap.titleUnicode, null);
			Utils.loadGlyphs(Utils.FONT_DEFAULT, null, beatmap.artistUnicode);
		}
		Utils.FONT_MEDIUM.drawString(cx, cy, beatmap.getTitle(), textColor);
		Utils.FONT_DEFAULT.drawString(cx, cy + Utils.FONT_MEDIUM.getLineHeight() - 2,
				String.format("%s // %s", beatmap.getArtist(), beatmap.creator), textColor);
		if (expanded || beatmaps.size() == 1)
			Utils.FONT_BOLD.drawString(cx, cy + Utils.FONT_MEDIUM.getLineHeight() + Utils.FONT_DEFAULT.getLineHeight() - 4,
					beatmap.version, textColor);
	}

	/**
	 * Returns an array of strings containing song information.
	 * <ul>
	 * <li>0: {Artist} - {Title} [{Version}]
	 * <li>1: Mapped by {Creator}
	 * <li>2: Length: {}  BPM: {}  Objects: {}
	 * <li>3: Circles: {}  Sliders: {}  Spinners: {}
	 * <li>4: CS:{} HP:{} AR:{} OD:{}
	 * </ul>
	 */
	public String[] getInfo() {
		if (beatmapIndex < 0)
			return null;

		Beatmap beatmap = beatmaps.get(beatmapIndex);
		float speedModifier = GameMod.getSpeedMultiplier();
		long endTime = (long) (beatmap.endTime / speedModifier);
		int bpmMin = (int) (beatmap.bpmMin * speedModifier);
		int bpmMax = (int) (beatmap.bpmMax * speedModifier);
		float multiplier = GameMod.getDifficultyMultiplier();
		String[] info = new String[5];
		info[0] = beatmap.toString();
		info[1] = String.format("Mapped by %s", beatmap.creator);
		info[2] = String.format("Length: %d:%02d  BPM: %s  Objects: %d",
				TimeUnit.MILLISECONDS.toMinutes(endTime),
				TimeUnit.MILLISECONDS.toSeconds(endTime) -
				TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(endTime)),
				(bpmMax <= 0) ? "--" : ((bpmMin == bpmMax) ? bpmMin : String.format("%d-%d", bpmMin, bpmMax)),
				(beatmap.hitObjectCircle + beatmap.hitObjectSlider + beatmap.hitObjectSpinner));
		info[3] = String.format("Circles: %d  Sliders: %d  Spinners: %d",
				beatmap.hitObjectCircle, beatmap.hitObjectSlider, beatmap.hitObjectSpinner);
		info[4] = String.format("CS:%.1f HP:%.1f AR:%.1f OD:%.1f",
				Math.min(beatmap.circleSize * multiplier, 10f),
				Math.min(beatmap.HPDrainRate * multiplier, 10f),
				Math.min(beatmap.approachRate * multiplier, 10f),
				Math.min(beatmap.overallDifficulty * multiplier, 10f));
		return info;
	}

	/**
	 * Returns a formatted string for the beatmap at {@code beatmapIndex}:
	 * "Artist - Title [Version]" (version omitted if {@code beatmapIndex} is invalid)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if (beatmapIndex == -1)
			return String.format("%s - %s", beatmaps.get(0).getArtist(), beatmaps.get(0).getTitle());
		else
			return beatmaps.get(beatmapIndex).toString();
	}

	/**
	 * Checks whether the node matches a given search query.
	 * @param query the search term
	 * @return true if title, artist, creator, source, version, or tag matches query
	 */
	public boolean matches(String query) {
		Beatmap beatmap = beatmaps.get(0);

		// search: title, artist, creator, source, version, tags (first beatmap)
		if (beatmap.title.toLowerCase().contains(query) ||
			beatmap.titleUnicode.toLowerCase().contains(query) ||
			beatmap.artist.toLowerCase().contains(query) ||
			beatmap.artistUnicode.toLowerCase().contains(query) ||
			beatmap.creator.toLowerCase().contains(query) ||
			beatmap.source.toLowerCase().contains(query) ||
			beatmap.version.toLowerCase().contains(query) ||
			beatmap.tags.contains(query))
			return true;

		// search: version, tags (remaining beatmaps)
		for (int i = 1; i < beatmaps.size(); i++) {
			beatmap = beatmaps.get(i);
			if (beatmap.version.toLowerCase().contains(query) ||
				beatmap.tags.contains(query))
				return true;
		}

		return false;
	}

	/**
	 * Checks whether the node matches a given condition.
	 * @param type the condition type (ar, cs, od, hp, bpm, length)
	 * @param operator the operator (=/==, >, >=, <, <=)
	 * @param value the value
	 * @return true if the condition is met
	 */
	public boolean matches(String type, String operator, float value) {
		for (Beatmap beatmap : beatmaps) {
			// get value
			float v;
			switch (type) {
				case "ar": v = beatmap.approachRate; break;
				case "cs": v = beatmap.circleSize; break;
				case "od": v = beatmap.overallDifficulty; break;
				case "hp": v = beatmap.HPDrainRate; break;
				case "bpm": v = beatmap.bpmMax; break;
				case "length": v = beatmap.endTime / 1000; break;
				default: return false;
			}

			// get operator
			boolean met;
			switch (operator) {
				case "=":
				case "==": met = (v == value); break;
				case ">":  met = (v > value);  break;
				case ">=": met = (v >= value); break;
				case "<":  met = (v < value);  break;
				case "<=": met = (v <= value); break;
				default: return false;
			}

			if (met)
				return true;
		}

		return false;
	}
}