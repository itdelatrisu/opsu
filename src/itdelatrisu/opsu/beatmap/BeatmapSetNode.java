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
import itdelatrisu.opsu.Options;
import itdelatrisu.opsu.ui.Colors;
import itdelatrisu.opsu.ui.Fonts;

import org.newdawn.slick.Color;
import org.newdawn.slick.Image;

/**
 * Node in an BeatmapSetList representing a beatmap set.
 */
public class BeatmapSetNode {
	/** The associated beatmap set. */
	private BeatmapSet beatmapSet;

	/** Index of the selected beatmap (-1 if not focused). */
	public int beatmapIndex = -1;

	/** Index of this node. */
	public int index = 0;

	/** Links to other nodes. */
	public BeatmapSetNode prev, next;

	/**
	 * Constructor.
	 * @param beatmapSet the beatmap set
	 */
	public BeatmapSetNode(BeatmapSet beatmapSet) {
		this.beatmapSet = beatmapSet;
	}

	/**
	 * Returns the associated beatmap set.
	 * @return the beatmap set
	 */
	public BeatmapSet getBeatmapSet() { return beatmapSet; }

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
		Color textColor = Options.getSkin().getSongSelectInactiveTextColor();

		// get drawing parameters
		if (expanded) {
			x -= bg.getWidth() / 10f;
			if (focus) {
				bgColor = Color.white;
				textColor = Options.getSkin().getSongSelectActiveTextColor();
			} else
				bgColor = Colors.BLUE_BUTTON;
			beatmap = beatmapSet.get(beatmapIndex);
		} else {
			bgColor = Colors.ORANGE_BUTTON;
			beatmap = beatmapSet.get(0);
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
			Fonts.loadGlyphs(Fonts.MEDIUM, beatmap.titleUnicode);
			Fonts.loadGlyphs(Fonts.DEFAULT, beatmap.artistUnicode);
		}
		Fonts.MEDIUM.drawString(cx, cy, beatmap.getTitle(), textColor);
		Fonts.DEFAULT.drawString(cx, cy + Fonts.MEDIUM.getLineHeight() - 2,
				String.format("%s // %s", beatmap.getArtist(), beatmap.creator), textColor);
		if (expanded || beatmapSet.size() == 1)
			Fonts.BOLD.drawString(cx, cy + Fonts.MEDIUM.getLineHeight() + Fonts.DEFAULT.getLineHeight() - 4,
					beatmap.version, textColor);
	}

	/**
	 * Returns an array of strings containing beatmap information for the
	 * selected beatmap, or null if none selected.
	 * @see BeatmapSet#getInfo(int)
	 */
	public String[] getInfo() { return (beatmapIndex < 0) ? null : beatmapSet.getInfo(beatmapIndex); }

	/**
	 * Returns a formatted string for the beatmap at {@code beatmapIndex}:
	 * "Artist - Title [Version]" (version omitted if {@code beatmapIndex} is invalid)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if (beatmapIndex == -1)
			return beatmapSet.toString();
		else
			return beatmapSet.get(beatmapIndex).toString();
	}
}