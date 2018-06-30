/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014-2018 Jeffrey Han
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

import com.github.francesco149.koohii.Koohii;

/**
 * Koohii's beatmap difficulty algorithm.
 * 
 * @author Francesco149 (https://github.com/Francesco149/Koohii)
 */
public class BeatmapDifficultyCalculator {

	/** SR calculator instance */
	private final Koohii.DiffCalc calc;

	/** Cached star rating, used for computation with mods */
	private double cachedStarRating = -1;
	/** Cached active mod list */
	private int activeMods = 0;

	/**
	 * Constructor. All computations are automatically done in this call and is
	 * assigned to the beatmap provided.
	 * <p>
	 * If any parts of the beatmap have not yet been loaded (e.g. timing points,
	 * hit objects), they will be loaded here.
	 * 
	 * @param beatmap the beatmap
	 */
	public BeatmapDifficultyCalculator(Beatmap beatmap) {
		if (beatmap.timingPoints == null)
			BeatmapDB.load(beatmap, BeatmapDB.LOAD_ARRAY);
		BeatmapParser.parseHitObjects(beatmap);

		this.calc = new Koohii.DiffCalc();
		cachedStarRating = calc.calc(convert(beatmap)).total;
		if (beatmap.starRating == -1)
			beatmap.starRating = cachedStarRating;
	}

	/**
	 * Returns the beatmap's star rating (applying any active mods).
	 */
	public double getStarRating() {
		if (activeMods != GameMod.getModState()) {
			activeMods = GameMod.getModState();
			cachedStarRating = calc.calc(activeMods).total;
		}

		return cachedStarRating;
	}

	/**
	 * Internal method for converting opsu's beatmap object to Koohii's beatmap
	 * object.
	 * 
	 * @param mapToConvert The map to convert
	 * @return An instance of Koohii's map, based from opsu's beatmap metadata.
	 */
	private static Koohii.Map convert(Beatmap mapToConvert) {
		Koohii.Map converted = new Koohii.Map();
		converted.mode = mapToConvert.mode;
		converted.title = mapToConvert.title;
		converted.title_unicode = mapToConvert.titleUnicode;
		converted.artist = mapToConvert.artist;
		converted.artist_unicode = mapToConvert.artistUnicode;
		converted.creator = mapToConvert.creator;
		converted.version = mapToConvert.version;
		converted.ncircles = mapToConvert.hitObjectCircle;
		converted.nsliders = mapToConvert.hitObjectSlider;
		converted.nspinners = mapToConvert.hitObjectSpinner;
		converted.hp = mapToConvert.HPDrainRate;
		converted.cs = mapToConvert.circleSize;
		converted.od = mapToConvert.overallDifficulty;
		converted.ar = mapToConvert.approachRate;
		converted.sv = mapToConvert.sliderMultiplier;
		converted.tick_rate = mapToConvert.sliderTickRate;

		for (TimingPoint timing : mapToConvert.timingPoints) {
			Koohii.Timing convTiming = new Koohii.Timing();
			convTiming.time = timing.getTime();

			if (timing.isInherited()) {
				convTiming.ms_per_beat = timing.getSliderMultiplier();
				convTiming.change = false;
			} else {
				convTiming.ms_per_beat = timing.getBeatLength();
				convTiming.change = true;
			}

			converted.tpoints.add(convTiming);

		}

		for (HitObject object : mapToConvert.objects) {
			Koohii.HitObject convHitObj = new Koohii.HitObject();
			convHitObj.time = object.getTime();
			convHitObj.type = object.getType();
			if ((HitObject.TYPE_CIRCLE & object.getType()) != 0) {
				Koohii.Circle convCircle = new Koohii.Circle();
				convCircle.pos = new Koohii.Vector2(object.getX(), object.getY());
				convHitObj.data = convCircle;
			}
			if ((HitObject.TYPE_SLIDER & object.getType()) != 0) {
				Koohii.Slider convSlider = new Koohii.Slider();
				convSlider.pos = new Koohii.Vector2(object.getX(), object.getY());
				convSlider.distance = object.getPixelLength();
				convSlider.repetitions = object.getRepeatCount();
				convHitObj.data = convSlider;
			}
			converted.objects.add(convHitObj);
		}
		return converted;
	}
}
