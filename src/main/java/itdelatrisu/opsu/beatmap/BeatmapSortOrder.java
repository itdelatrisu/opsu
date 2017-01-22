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

import java.util.Comparator;

/**
 * Beatmap sorting orders.
 */
public enum BeatmapSortOrder {
	TITLE   ("Title",       new TitleOrder()),
	ARTIST  ("Artist",      new ArtistOrder()),
	CREATOR ("Creator",     new CreatorOrder()),
	BPM     ("BPM",         new BPMOrder()),
	LENGTH  ("Length",      new LengthOrder()),
	DATE    ("Date Added",  new DateOrder()),
	PLAYS   ("Most Played", new PlayOrder());

	/** The name of the sort. */
	private final String name;

	/** The comparator for the sort. */
	private final Comparator<BeatmapSetNode> comparator;

	/** Current sort. */
	private static BeatmapSortOrder currentSort = TITLE;

	/**
	 * Returns the current sort.
	 * @return the current sort
	 */
	public static BeatmapSortOrder current() { return currentSort; }

	/**
	 * Sets a new sort.
	 * @param sort the new sort
	 */
	public static void set(BeatmapSortOrder sort) { currentSort = sort; }

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
			for (Beatmap beatmap : v.getBeatmapSet()) {
				if (beatmap.endTime > vMax)
					vMax = beatmap.endTime;
			}
			for (Beatmap beatmap : w.getBeatmapSet()) {
				if (beatmap.endTime > wMax)
					wMax = beatmap.endTime;
			}
			return Integer.compare(vMax, wMax);
		}
	}

	/**
	 * Compares two BeatmapSetNode objects by date added.
	 * Uses the latest beatmap added in each set for comparison.
	 */
	private static class DateOrder implements Comparator<BeatmapSetNode> {
		@Override
		public int compare(BeatmapSetNode v, BeatmapSetNode w) {
			long vMax = 0, wMax = 0;
			for (Beatmap beatmap : v.getBeatmapSet()) {
				if (beatmap.dateAdded > vMax)
					vMax = beatmap.dateAdded;
			}
			for (Beatmap beatmap : w.getBeatmapSet()) {
				if (beatmap.dateAdded > wMax)
					wMax = beatmap.dateAdded;
			}
			return Long.compare(vMax, wMax);
		}
	}

	/**
	 * Compares two BeatmapSetNode objects by total plays
	 * (summed across all beatmaps in each set).
	 */
	private static class PlayOrder implements Comparator<BeatmapSetNode> {
		@Override
		public int compare(BeatmapSetNode v, BeatmapSetNode w) {
			int vTotal = 0, wTotal = 0;
			for (Beatmap beatmap : v.getBeatmapSet())
				vTotal += beatmap.playCount;
			for (Beatmap beatmap : w.getBeatmapSet())
				wTotal += beatmap.playCount;
			return Integer.compare(vTotal, wTotal);
		}
	}

	/**
	 * Constructor.
	 * @param name the sort name
	 * @param comparator the comparator for the sort
	 */
	BeatmapSortOrder(String name, Comparator<BeatmapSetNode> comparator) {
		this.name = name;
		this.comparator = comparator;
	}

	/**
	 * Returns the sort name.
	 * @return the name
	 */
	public String getName() { return name; }

	/**
	 * Returns the comparator for the sort.
	 * @return the comparator
	 */
	public Comparator<BeatmapSetNode> getComparator() { return comparator; }

	@Override
	public String toString() { return name; }
}