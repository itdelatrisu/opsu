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

import java.util.ArrayList;
import java.util.Comparator;

import org.newdawn.slick.Color;
import org.newdawn.slick.Image;

/**
 * Node in an OsuGroupList representing a group of OsuFile objects.
 */
public class OsuGroupNode implements Comparable<OsuGroupNode> {
	/**
	 * Menu background image.
	 */
	private static Image bg;

	/**
	 * List of associated OsuFile objects.
	 */
	public ArrayList<OsuFile> osuFiles;

	/**
	 * Index of this OsuGroupNode.
	 */
	public int index = 0;

	/**
	 * Index of selected osuFile.
	 * If not focused, the value will be -1.
	 */
	public int osuFileIndex = -1;

	/**
	 * Links to other OsuGroupNode objects.
	 */
	public OsuGroupNode prev, next;

	/**
	 * Constructor.
	 * @param osuFiles the OsuFile objects in this group
	 */
	public OsuGroupNode(ArrayList<OsuFile> osuFiles) {
		this.osuFiles = osuFiles;
	}

	/**
	 * Compares two OsuGroupNode objects by title.
	 */
	@Override
	public int compareTo(OsuGroupNode that) {
		return this.osuFiles.get(0).title.compareToIgnoreCase(that.osuFiles.get(0).title);
	}

	/**
	 * Compares two OsuGroupNode objects by artist.
	 */
	public static class ArtistOrder implements Comparator<OsuGroupNode> {
		@Override
		public int compare(OsuGroupNode v, OsuGroupNode w) {
			return v.osuFiles.get(0).artist.compareToIgnoreCase(w.osuFiles.get(0).artist);
		}
	}

	/**
	 * Compares two OsuGroupNode objects by creator.
	 */
	public static class CreatorOrder implements Comparator<OsuGroupNode> {
		@Override
		public int compare(OsuGroupNode v, OsuGroupNode w) {
			return v.osuFiles.get(0).creator.compareToIgnoreCase(w.osuFiles.get(0).creator);
		}
	}

	/**
	 * Compares two OsuGroupNode objects by BPM.
	 */
	public static class BPMOrder implements Comparator<OsuGroupNode> {
		@Override
		public int compare(OsuGroupNode v, OsuGroupNode w) {
			return Integer.compare(v.osuFiles.get(0).bpmMax, w.osuFiles.get(0).bpmMax);
		}
	}

	/**
	 * Sets a button background image.
	 */
	public static void setBackground(Image background) {
		bg = background;
	}

	/**
	 * Draws the button.
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param focus true if this is the focused node
	 */
	public void draw(float x, float y, boolean focus) {
		boolean expanded = (osuFileIndex > -1);
		float xOffset = 0f;
		OsuFile osu;
		Color textColor = Color.lightGray;

		if (expanded) {  // expanded
			xOffset = bg.getWidth() / 10f;
			if (focus) {
				bg.draw(x - xOffset, y, Color.white);
				textColor = Color.white;
			} else
				bg.draw(x - xOffset, y, Utils.COLOR_BLUE_BUTTON);
			osu = osuFiles.get(osuFileIndex);
		} else {
			bg.draw(x, y, Utils.COLOR_ORANGE_BUTTON);
			osu = osuFiles.get(0);
		}

		float cx = x + (bg.getWidth() * 0.05f) - xOffset;
		float cy = y + (bg.getHeight() * 0.2f) - 3;

		Utils.FONT_MEDIUM.drawString(cx, cy, osu.title, textColor);
		Utils.FONT_DEFAULT.drawString(cx, cy + Utils.FONT_MEDIUM.getLineHeight() - 4,
				String.format("%s // %s", osu.artist, osu.creator), textColor);
		if (expanded || osuFiles.size() == 1)
			Utils.FONT_BOLD.drawString(cx, cy + Utils.FONT_MEDIUM.getLineHeight() + Utils.FONT_DEFAULT.getLineHeight() - 8,
					osu.version, textColor);
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
		if (osuFileIndex < 0)
			return null;

		OsuFile osu = osuFiles.get(osuFileIndex);
		String[] info = new String[5];
		info[0] = osu.toString();
		info[1] = String.format("Mapped by %s",
				osu.creator);
		info[2] = String.format("Length: %s  BPM: %s  Objects: %d",
				MusicController.getTrackLengthString(),
				(osu.bpmMax <= 0) ? "--" :
				 ((osu.bpmMin == osu.bpmMax) ? osu.bpmMin : String.format("%d-%d", osu.bpmMin, osu.bpmMax)),
				(osu.hitObjectCircle + osu.hitObjectSlider + osu.hitObjectSpinner));
		info[3] = String.format("Circles: %d  Sliders: %d  Spinners: %d",
				osu.hitObjectCircle, osu.hitObjectSlider, osu.hitObjectSpinner);
		info[4] = String.format("CS:%.1f HP:%.1f AR:%.1f OD:%.1f",
				osu.circleSize, osu.HPDrainRate, osu.approachRate, osu.overallDifficulty);
		return info;
	}

	/**
	 * Returns a formatted string for the OsuFile at osuFileIndex:
	 * "Artist - Title [Version]" (version omitted if osuFileIndex is invalid)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if (osuFileIndex == -1)
			return String.format("%s - %s", osuFiles.get(0).artist, osuFiles.get(0).title);
		else
			return osuFiles.get(osuFileIndex).toString();
	}

	/**
	 * Checks whether the node matches a given search query.
	 * @param query the search term
	 * @return true if title, artist, creator, source, version, or tag matches query
	 */
	public boolean matches(String query) {
		OsuFile osu = osuFiles.get(0);

		// search: title, artist, creator, source, version, tags (first OsuFile)
		if (osu.title.toLowerCase().contains(query) ||
			osu.artist.toLowerCase().contains(query) ||
			osu.creator.toLowerCase().contains(query) ||
			osu.source.toLowerCase().contains(query) ||
			osu.version.toLowerCase().contains(query) ||
			osu.tags.contains(query))
			return true;

		// search: version, tags (remaining OsuFiles)
		for (int i = 1; i < osuFiles.size(); i++) {
			osu = osuFiles.get(i);
			if (osu.version.toLowerCase().contains(query) ||
				osu.tags.contains(query))
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
		for (OsuFile osu : osuFiles) {
			// get value
			float osuValue;
			switch (type) {
				case "ar": osuValue = osu.approachRate; break;
				case "cs": osuValue = osu.circleSize; break;
				case "od": osuValue = osu.overallDifficulty; break;
				case "hp": osuValue = osu.HPDrainRate; break;
				case "bpm": osuValue = osu.bpmMax; break;
//				case "length": /* not implemented */ break;
				default: return false;
			}

			// get operator
			boolean met;
			switch (operator) {
				case "=":
				case "==": met = (osuValue == value); break;
				case ">":  met = (osuValue > value);  break;
				case ">=": met = (osuValue >= value); break;
				case "<":  met = (osuValue < value);  break;
				case "<=": met = (osuValue <= value); break;
				default: return false;
			}

			if (met)
				return true;
		}

		return false;
	}
}