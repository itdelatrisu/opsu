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

import itdelatrisu.opsu.GameData.Grade;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.newdawn.slick.Color;
import org.newdawn.slick.Image;

/**
 * Node in an OsuGroupList representing a group of OsuFile objects.
 */
public class OsuGroupNode {
	/** List of associated OsuFile objects. */
	public ArrayList<OsuFile> osuFiles;

	/** Index of this OsuGroupNode. */
	public int index = 0;

	/** Index of selected osuFile (-1 if not focused). */
	public int osuFileIndex = -1;

	/** Links to other OsuGroupNode objects. */
	public OsuGroupNode prev, next;

	/**
	 * Constructor.
	 * @param osuFiles the OsuFile objects in this group
	 */
	public OsuGroupNode(ArrayList<OsuFile> osuFiles) {
		this.osuFiles = osuFiles;
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
		boolean expanded = (osuFileIndex > -1);
		OsuFile osu;
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
			osu = osuFiles.get(osuFileIndex);
		} else {
			bgColor = Utils.COLOR_ORANGE_BUTTON;
			osu = osuFiles.get(0);
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
			Utils.loadGlyphs(Utils.FONT_MEDIUM, osu.titleUnicode, null);
			Utils.loadGlyphs(Utils.FONT_DEFAULT, null, osu.artistUnicode);
		}
		Utils.FONT_MEDIUM.drawString(cx, cy, osu.getTitle(), textColor);
		Utils.FONT_DEFAULT.drawString(cx, cy + Utils.FONT_MEDIUM.getLineHeight() - 2,
				String.format("%s // %s", osu.getArtist(), osu.creator), textColor);
		if (expanded || osuFiles.size() == 1)
			Utils.FONT_BOLD.drawString(cx, cy + Utils.FONT_MEDIUM.getLineHeight() + Utils.FONT_DEFAULT.getLineHeight() - 4,
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
		float speedModifier = GameMod.getSpeedMultiplier();
		long endTime = (long) (osu.endTime / speedModifier);
		int bpmMin = (int) (osu.bpmMin * speedModifier);
		int bpmMax = (int) (osu.bpmMax * speedModifier);
		float multiplier = GameMod.getDifficultyMultiplier();
		String[] info = new String[5];
		info[0] = osu.toString();
		info[1] = String.format("Mapped by %s", osu.creator);
		info[2] = String.format("Length: %d:%02d  BPM: %s  Objects: %d",
				TimeUnit.MILLISECONDS.toMinutes(endTime),
				TimeUnit.MILLISECONDS.toSeconds(endTime) -
				TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(endTime)),
				(bpmMax <= 0) ? "--" : ((bpmMin == bpmMax) ? bpmMin : String.format("%d-%d", bpmMin, bpmMax)),
				(osu.hitObjectCircle + osu.hitObjectSlider + osu.hitObjectSpinner));
		info[3] = String.format("Circles: %d  Sliders: %d  Spinners: %d",
				osu.hitObjectCircle, osu.hitObjectSlider, osu.hitObjectSpinner);
		info[4] = String.format("CS:%.1f HP:%.1f AR:%.1f OD:%.1f",
				Math.min(osu.circleSize * multiplier, 10f),
				Math.min(osu.HPDrainRate * multiplier, 10f),
				Math.min(osu.approachRate * multiplier, 10f),
				Math.min(osu.overallDifficulty * multiplier, 10f));
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
			return String.format("%s - %s", osuFiles.get(0).getArtist(), osuFiles.get(0).getTitle());
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
			osu.titleUnicode.toLowerCase().contains(query) ||
			osu.artist.toLowerCase().contains(query) ||
			osu.artistUnicode.toLowerCase().contains(query) ||
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
				case "length": osuValue = osu.endTime / 1000; break;
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