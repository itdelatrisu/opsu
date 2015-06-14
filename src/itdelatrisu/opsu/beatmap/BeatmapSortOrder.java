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

import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.ui.MenuButton;
import itdelatrisu.opsu.ui.UI;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import org.newdawn.slick.Image;

/**
 * Beatmap sorting orders.
 */
public enum BeatmapSortOrder {
	TITLE   (0, "Title",   new TitleOrder()),
	ARTIST  (1, "Artist",  new ArtistOrder()),
	CREATOR (2, "Creator", new CreatorOrder()),
	BPM     (3, "BPM",     new BPMOrder()),
	LENGTH  (4, "Length",  new LengthOrder());

	/** The ID of the sort (used for tab positioning). */
	private int id;

	/** The name of the sort. */
	private String name;

	/** The comparator for the sort. */
	private Comparator<BeatmapSetNode> comparator;

	/** The tab associated with the sort (displayed in Song Menu screen). */
	private MenuButton tab;

	/** Total number of sorts. */
	private static final int SIZE = values().length;

	/** Array of BeatmapSortOrder objects in reverse order. */
	public static final BeatmapSortOrder[] VALUES_REVERSED;
	static {
		VALUES_REVERSED = values();
		Collections.reverse(Arrays.asList(VALUES_REVERSED));
	}

	/** Current sort. */
	private static BeatmapSortOrder currentSort = TITLE;

	/**
	 * Returns the current sort.
	 * @return the current sort
	 */
	public static BeatmapSortOrder getSort() { return currentSort; }

	/**
	 * Sets a new sort.
	 * @param sort the new sort
	 */
	public static void setSort(BeatmapSortOrder sort) { BeatmapSortOrder.currentSort = sort; }

	/**
	 * Compares two BeatmapSetNode objects by title.
	 */
	private static class TitleOrder implements Comparator<BeatmapSetNode> {
		@Override
		public int compare(BeatmapSetNode v, BeatmapSetNode w) {
			return v.getBeatmapSet().get(0).title.compareToIgnoreCase(w.getBeatmapSet().get(0).title);
		}
	}

	/**
	 * Compares two BeatmapSetNode objects by artist.
	 */
	private static class ArtistOrder implements Comparator<BeatmapSetNode> {
		@Override
		public int compare(BeatmapSetNode v, BeatmapSetNode w) {
			return v.getBeatmapSet().get(0).artist.compareToIgnoreCase(w.getBeatmapSet().get(0).artist);
		}
	}

	/**
	 * Compares two BeatmapSetNode objects by creator.
	 */
	private static class CreatorOrder implements Comparator<BeatmapSetNode> {
		@Override
		public int compare(BeatmapSetNode v, BeatmapSetNode w) {
			return v.getBeatmapSet().get(0).creator.compareToIgnoreCase(w.getBeatmapSet().get(0).creator);
		}
	}

	/**
	 * Compares two BeatmapSetNode objects by BPM.
	 */
	private static class BPMOrder implements Comparator<BeatmapSetNode> {
		@Override
		public int compare(BeatmapSetNode v, BeatmapSetNode w) {
			return Integer.compare(v.getBeatmapSet().get(0).bpmMax, w.getBeatmapSet().get(0).bpmMax);
		}
	}

	/**
	 * Compares two BeatmapSetNode objects by length.
	 * Uses the longest beatmap in each set for comparison.
	 */
	private static class LengthOrder implements Comparator<BeatmapSetNode> {
		@Override
		public int compare(BeatmapSetNode v, BeatmapSetNode w) {
			int vMax = 0, wMax = 0;
			for (int i = 0, size = v.getBeatmapSet().size(); i < size; i++) {
				Beatmap beatmap = v.getBeatmapSet().get(i);
				if (beatmap.endTime > vMax)
					vMax = beatmap.endTime;
			}
			for (int i = 0, size = w.getBeatmapSet().size(); i < size; i++) {
				Beatmap beatmap = w.getBeatmapSet().get(i);
				if (beatmap.endTime > wMax)
					wMax = beatmap.endTime;
			}
			return Integer.compare(vMax, wMax);
		}
	}

	/**
	 * Constructor.
	 * @param id the ID of the sort (for tab positioning)
	 * @param name the sort name
	 * @param comparator the comparator for the sort
	 */
	BeatmapSortOrder(int id, String name, Comparator<BeatmapSetNode> comparator) {
		this.id = id;
		this.name = name;
		this.comparator = comparator;
	}

	/**
	 * Initializes the sort tab.
	 * @param containerWidth the container width
	 * @param bottomY the bottom y coordinate
	 */
	public void init(int containerWidth, float bottomY) {
		Image tab = GameImage.MENU_TAB.getImage();
		int tabWidth = tab.getWidth();
		float buttonX = containerWidth / 2f;
		float tabOffset = (containerWidth - buttonX - tabWidth) / (SIZE - 1);
		if (tabOffset > tabWidth) {  // prevent tabs from being spaced out
			tabOffset = tabWidth;
			buttonX = (containerWidth * 0.99f) - (tabWidth * SIZE);
		}
		this.tab = new MenuButton(tab,
				(buttonX + (tabWidth / 2f)) + (id * tabOffset),
				bottomY - (tab.getHeight() / 2f)
		);
	}

	/**
	 * Returns the comparator for the sort.
	 * @return the comparator
	 */
	public Comparator<BeatmapSetNode> getComparator() { return comparator; }

	/**
	 * Checks if the coordinates are within the image bounds.
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @return true if within bounds
	 */
	public boolean contains(float x, float y) { return tab.contains(x, y); }

	/**
	 * Draws the sort tab.
	 * @param selected whether the tab is selected (white) or not (red)
	 * @param isHover whether to include a hover effect (unselected only)
	 */
	public void draw(boolean selected, boolean isHover) {
		UI.drawTab(tab.getX(), tab.getY(), name, selected, isHover);
	}
}