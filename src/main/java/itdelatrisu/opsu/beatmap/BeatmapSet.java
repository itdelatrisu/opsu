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

import itdelatrisu.opsu.GameMod;
import itdelatrisu.opsu.db.BeatmapDB;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * Data type containing all beatmaps in a beatmap set.
 */
public class BeatmapSet implements Iterable<Beatmap> {
	/** List of associated beatmaps. */
	private final ArrayList<Beatmap> beatmaps;

	/**
	 * Constructor.
	 * @param beatmaps the beatmaps in this set
	 */
	public BeatmapSet(ArrayList<Beatmap> beatmaps) {
		this.beatmaps = beatmaps;
	}

	/**
	 * Returns the number of elements.
	 */
	public int size() { return beatmaps.size(); }

	/**
	 * Returns the beatmap at the given index.
	 * @param index the beatmap index
	 * @throws IndexOutOfBoundsException if the index is out of range
	 */
	public Beatmap get(int index) { return beatmaps.get(index); }

	/**
	 * Removes the beatmap at the given index.
	 * @param index the beatmap index
	 * @return the removed beatmap
	 * @throws IndexOutOfBoundsException if the index is out of range
	 */
	public Beatmap remove(int index) { return beatmaps.remove(index); }

	@Override
	public Iterator<Beatmap> iterator() { return beatmaps.iterator(); }

	/**
	 * Returns an array of strings containing beatmap information.
	 * <ul>
	 * <li>0: {Artist} - {Title} [{Version}]
	 * <li>1: Mapped by {Creator}
	 * <li>2: Length: {}  BPM: {}  Objects: {}
	 * <li>3: Circles: {}  Sliders: {}  Spinners: {}
	 * <li>4: CS:{} HP:{} AR:{} OD:{} Stars:{}
	 * </ul>
	 * @param index the beatmap index
	 * @throws IndexOutOfBoundsException if the index is out of range
	 */
	public String[] getInfo(int index) {
		Beatmap beatmap = beatmaps.get(index);
		float speedModifier = GameMod.getSpeedMultiplier();
		long endTime = (long) (beatmap.endTime / speedModifier);
		int bpmMin = (int) (beatmap.bpmMin * speedModifier);
		int bpmMax = (int) (beatmap.bpmMax * speedModifier);
		float multiplier = GameMod.getDifficultyMultiplier();
		NumberFormat nf = new DecimalFormat("##.#");
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
		info[4] = String.format("CS:%s HP:%s AR:%s OD:%s%s",
				nf.format(Math.min(beatmap.circleSize * multiplier, 10f)),
				nf.format(Math.min(beatmap.HPDrainRate * multiplier, 10f)),
				nf.format(Math.min(beatmap.approachRate * multiplier, 10f)),
				nf.format(Math.min(beatmap.overallDifficulty * multiplier, 10f)),
				(beatmap.starRating >= 0) ? String.format(" Stars:%.2f", beatmap.starRating) : "");
		return info;
	}

	/**
	 * Returns a formatted string for the beatmap set:
	 * "Artist - Title"
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		Beatmap beatmap = beatmaps.get(0);
		return String.format("%s - %s", beatmap.getArtist(), beatmap.getTitle());
	}

	/**
	 * Checks whether the beatmap set matches a given search query.
	 * @param query the search term
	 * @return true if title, artist, creator, source, version, or tag matches query
	 */
	public boolean matches(String query) {
		// search: title, artist, creator, source, version, tags (first beatmap)
		Beatmap beatmap = beatmaps.get(0);
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
		for (int i = 1, n = beatmaps.size(); i < n; i++) {
			beatmap = beatmaps.get(i);
			if (beatmap.version.toLowerCase().contains(query) ||
				beatmap.tags.contains(query))
				return true;
		}

		return false;
	}

	/**
	 * Checks whether the beatmap set matches a given condition.
	 * @param type the condition type (ar, cs, od, hp, bpm, length, star/stars)
	 * @param operator the operator {@literal (=/==, >, >=, <, <=)}
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
				case "star":
				case "stars": v = Math.round(beatmap.starRating * 100) / 100f; break;
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

	/**
	 * Returns whether this beatmap set is a "favorite".
	 */
	public boolean isFavorite() {
		for (Beatmap map : beatmaps) {
			if (map.favorite)
				return true;
		}
		return false;
	}

	/**
	 * Sets the "favorite" status of this beatmap set.
	 * @param flag whether this beatmap set should have "favorite" status
	 */
	public void setFavorite(boolean flag) {
		for (Beatmap map : beatmaps) {
			map.favorite = flag;
			BeatmapDB.updateFavoriteStatus(map);
		}
	}

	/**
	 * Returns whether any beatmap in this set has been played.
	 */
	public boolean isPlayed() {
		for (Beatmap map : beatmaps) {
			if (map.playCount > 0)
				return true;
		}
		return false;
	}
}
