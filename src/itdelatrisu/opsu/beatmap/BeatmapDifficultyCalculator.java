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

import itdelatrisu.opsu.db.BeatmapDB;
import itdelatrisu.opsu.objects.curves.Curve;
import itdelatrisu.opsu.objects.curves.Vec2f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.newdawn.slick.util.Log;

/**
 * osu!tp's beatmap difficulty algorithm.
 *
 * @author Tom94 (https://github.com/Tom94/AiModtpDifficultyCalculator)
 */
public class BeatmapDifficultyCalculator {
	/** Difficulty types. */
	public static final int DIFFICULTY_SPEED = 0, DIFFICULTY_AIM = 1;

	/** The star scaling factor. */
	private static final double STAR_SCALING_FACTOR = 0.0675;

	/** The scaling factor that favors extremes. */
	private static final double EXTREME_SCALING_FACTOR = 0.5;

	/** The playfield width. */
	private static final float PLAYFIELD_WIDTH = 512f;

	/**
	 * In milliseconds. For difficulty calculation we will only look at the highest strain value in each
	 * time interval of size STRAIN_STEP.This is to eliminate higher influence of stream over aim by simply
	 * having more HitObjects with high strain. The higher this value, the less strains there will be,
	 * indirectly giving long beatmaps an advantage.
	 */
	private static final double STRAIN_STEP = 400;

	/** The weighting of each strain value decays to 0.9 * its previous value. */
	private static final double DECAY_WEIGHT = 0.9;

	/** The beatmap. */
	private final Beatmap beatmap;

	/** The beatmap's hit objects. */
	private tpHitObject[] tpHitObjects;

	/** The computed star rating. */
	private double starRating = -1;

	/** The computed difficulties, indexed by the {@code DIFFICULTY_*} constants. */
	private double[] difficulties = { -1, -1 };

	/** The computed stars, indexed by the {@code DIFFICULTY_*} constants. */
	private double[] stars = { -1, -1 };

	/**
	 * Constructor. Call {@link #calculate()} to run all computations.
	 * <p>
	 * If any parts of the beatmap have not yet been loaded (e.g. timing points,
	 * hit objects), they will be loaded here.
	 * @param beatmap the beatmap
	 */
	public BeatmapDifficultyCalculator(Beatmap beatmap) {
		this.beatmap = beatmap;
		if (beatmap.timingPoints == null)
			BeatmapDB.load(beatmap, BeatmapDB.LOAD_ARRAY);
		BeatmapParser.parseHitObjects(beatmap);
	}

	/**
	 * Returns the beatmap's star rating.
	 */
	public double getStarRating() { return starRating; }

	/**
	 * Returns the difficulty value for a difficulty type.
	 * @param type the difficulty type ({@code DIFFICULTY_* constant})
	 */
	public double getDifficulty(int type) { return difficulties[type]; }

	/**
	 * Returns the star value for a difficulty type.
	 * @param type the difficulty type ({@code DIFFICULTY_* constant})
	 */
	public double getStars(int type) { return stars[type]; }

	/**
	 * Calculates the difficulty values and star ratings for the beatmap.
	 */
	public void calculate() {
		if (beatmap.objects == null || beatmap.timingPoints == null) {
			Log.error(String.format("Trying to calculate difficulty values for beatmap '%s' with %s not yet loaded.",
					beatmap.toString(), (beatmap.objects == null) ? "hit objects" : "timing points"));
			return;
		}

		// Fill our custom tpHitObject class, that carries additional information
		// TODO: apply hit object stacking algorithm?
		HitObject[] hitObjects = beatmap.objects;
		this.tpHitObjects = new tpHitObject[hitObjects.length];
		float circleRadius = (PLAYFIELD_WIDTH / 16.0f) * (1.0f - 0.7f * (beatmap.circleSize - 5.0f) / 5.0f);
		int timingPointIndex = 0;
		float beatLengthBase = 1, beatLength = 1;
		if (!beatmap.timingPoints.isEmpty()) {
			TimingPoint timingPoint = beatmap.timingPoints.get(0);
			if (!timingPoint.isInherited()) {
				beatLengthBase = beatLength = timingPoint.getBeatLength();
				timingPointIndex++;
			}
		}
		for (int i = 0; i < hitObjects.length; i++) {
			HitObject hitObject = hitObjects[i];

			// pass beatLength to hit objects
			int hitObjectTime = hitObject.getTime();
			while (timingPointIndex < beatmap.timingPoints.size()) {
				TimingPoint timingPoint = beatmap.timingPoints.get(timingPointIndex);
				if (timingPoint.getTime() > hitObjectTime)
					break;
				if (!timingPoint.isInherited())
					beatLengthBase = beatLength = timingPoint.getBeatLength();
				else
					beatLength = beatLengthBase * timingPoint.getSliderMultiplier();
				timingPointIndex++;
			}

			tpHitObjects[i] = new tpHitObject(hitObject, circleRadius, beatmap, beatLength);
		}

		if (!calculateStrainValues()) {
			Log.error("Could not compute strain values. Aborting difficulty calculation.");
			return;
		}

		// OverallDifficulty is not considered in this algorithm and neither is HpDrainRate.
		// That means, that in this form the algorithm determines how hard it physically is
		// to play the map, assuming, that too much of an error will not lead to a death.
		// It might be desirable to include OverallDifficulty into map difficulty, but in my
		// personal opinion it belongs more to the weighting of the actual performance
		// and is superfluous in the beatmap difficulty rating.
		// If it were to be considered, then I would look at the hit window of normal HitCircles only,
		// since Sliders and Spinners are (almost) "free" 300s and take map length into account as well.
		difficulties[DIFFICULTY_SPEED] = calculateDifficulty(DIFFICULTY_SPEED);
		difficulties[DIFFICULTY_AIM] = calculateDifficulty(DIFFICULTY_AIM);

		// The difficulty can be scaled by any desired metric.
		// In osu!tp it gets squared to account for the rapid increase in difficulty as the
		// limit of a human is approached. (Of course it also gets scaled afterwards.)
		// It would not be suitable for a star rating, therefore:

		// The following is a proposal to forge a star rating from 0 to 5. It consists of taking
		// the square root of the difficulty, since by simply scaling the easier
		// 5-star maps would end up with one star.
		stars[DIFFICULTY_SPEED] = Math.sqrt(difficulties[DIFFICULTY_SPEED]) * STAR_SCALING_FACTOR;
		stars[DIFFICULTY_AIM] = Math.sqrt(difficulties[DIFFICULTY_AIM]) * STAR_SCALING_FACTOR;

		// Again, from own observations and from the general opinion of the community
		// a map with high speed and low aim (or vice versa) difficulty is harder,
		// than a map with mediocre difficulty in both. Therefore we can not just add
		// both difficulties together, but will introduce a scaling that favors extremes.

		// Another approach to this would be taking Speed and Aim separately to a chosen
		// power, which again would be equivalent. This would be more convenient if
		// the hit window size is to be considered as well.

		// Note: The star rating is tuned extremely tight! Airman (/b/104229) and
		// Freedom Dive (/b/126645), two of the hardest ranked maps, both score ~4.66 stars.
		// Expect the easier kind of maps that officially get 5 stars to obtain around 2 by
		// this metric. The tutorial still scores about half a star.
		// Tune by yourself as you please. ;)
		this.starRating = stars[DIFFICULTY_SPEED] + stars[DIFFICULTY_AIM] +
				Math.abs(stars[DIFFICULTY_SPEED] - stars[DIFFICULTY_AIM]) * EXTREME_SCALING_FACTOR;
	}

	/**
	 * Computes the strain values for the beatmap.
	 * @return true if successful, false otherwise
	 */
	private boolean calculateStrainValues() {
		// Traverse hitObjects in pairs to calculate the strain value of NextHitObject from
		// the strain value of CurrentHitObject and environment.
		if (tpHitObjects.length == 0) {
			Log.warn("Can not compute difficulty of empty beatmap.");
			return false;
		}

		tpHitObject currentHitObject = tpHitObjects[0];
		tpHitObject nextHitObject;
		int index = 0;

		// First hitObject starts at strain 1. 1 is the default for strain values,
		// so we don't need to set it here. See tpHitObject.
		while (++index < tpHitObjects.length) {
			nextHitObject = tpHitObjects[index];
			nextHitObject.calculateStrains(currentHitObject);
			currentHitObject = nextHitObject;
		}

		return true;
	}

	/**
	 * Calculates the difficulty value for a difficulty type.
	 * @param type the difficulty type ({@code DIFFICULTY_* constant})
	 * @return the difficulty value
	 */
	private double calculateDifficulty(int type) {
		// Find the highest strain value within each strain step
		List<Double> highestStrains = new ArrayList<Double>();
		double intervalEndTime = STRAIN_STEP;
		double maximumStrain = 0; // We need to keep track of the maximum strain in the current interval

		tpHitObject previousHitObject = null;
		for (int i = 0; i < tpHitObjects.length; i++) {
			tpHitObject hitObject = tpHitObjects[i];

			// While we are beyond the current interval push the currently available maximum to our strain list
			while (hitObject.baseHitObject.getTime() > intervalEndTime) {
				highestStrains.add(maximumStrain);

				// The maximum strain of the next interval is not zero by default! We need to take the last
				// hitObject we encountered, take its strain and apply the decay until the beginning of the next interval.
				if (previousHitObject == null)
					maximumStrain = 0;
				else {
					double decay = Math.pow(tpHitObject.DECAY_BASE[type], (intervalEndTime - previousHitObject.baseHitObject.getTime()) / 1000);
					maximumStrain = previousHitObject.getStrain(type) * decay;
				}

				// Go to the next time interval
				intervalEndTime += STRAIN_STEP;
			}

			// Obtain maximum strain
			if (hitObject.getStrain(type) > maximumStrain)
				maximumStrain = hitObject.getStrain(type);

			previousHitObject = hitObject;
		}

		// Build the weighted sum over the highest strains for each interval
		double difficulty = 0;
		double weight = 1;
		Collections.sort(highestStrains, Collections.reverseOrder()); // Sort from highest to lowest strain.
		for (double strain : highestStrains) {
			difficulty += weight * strain;
			weight *= DECAY_WEIGHT;
		}

		return difficulty;
	}
}

/**
 * Hit object helper class for calculating strains.
 */
class tpHitObject {
	/**
	 * Factor by how much speed / aim strain decays per second. Those values are results
	 * of tweaking a lot and taking into account general feedback.
	 * Opinionated observation: Speed is easier to maintain than accurate jumps.
	 */
	public static final double[] DECAY_BASE = { 0.3, 0.15 };

	/** Almost the normed diameter of a circle (104 osu pixel). That is -after- position transforming. */
	private static final double ALMOST_DIAMETER = 90;

	/**
	 * Pseudo threshold values to distinguish between "singles" and "streams".
	 * Of course the border can not be defined clearly, therefore the algorithm
	 * has a smooth transition between those values. They also are based on tweaking
	 * and general feedback.
	 */
	private static final double STREAM_SPACING_TRESHOLD = 110, SINGLE_SPACING_TRESHOLD = 125;

	/**
	 * Scaling values for weightings to keep aim and speed difficulty in balance.
	 * Found from testing a very large map pool (containing all ranked maps) and
	 * keeping the average values the same.
	 */
	private static final double[] SPACING_WEIGHT_SCALING = { 1400, 26.25 };

	/**
	 * In milliseconds. The smaller the value, the more accurate sliders are approximated.
	 * 0 leads to an infinite loop, so use something bigger.
	 */
	private static final int LAZY_SLIDER_STEP_LENGTH = 1;

	/** The base hit object. */
	public final HitObject baseHitObject;

	/** The strain values, indexed by the {@code DIFFICULTY_*} constants. */
	private double[] strains = { 1, 1 };

	/** The normalized start and end positions. */
	private Vec2f normalizedStartPosition, normalizedEndPosition;

	/** The slider lengths. */
	private float lazySliderLengthFirst = 0, lazySliderLengthSubsequent = 0;

	/**
	 * Constructor.
	 * @param baseHitObject the base hit object
	 * @param circleRadius the circle radius
	 * @param beatmap the beatmap that contains the hit object
	 * @param beatLength the current beat length
	 */
	public tpHitObject(HitObject baseHitObject, float circleRadius, Beatmap beatmap, float beatLength) {
		this.baseHitObject = baseHitObject;

		// We will scale everything by this factor, so we can assume a uniform CircleSize among beatmaps.
		float scalingFactor = (52.0f / circleRadius);
		normalizedStartPosition = new Vec2f(baseHitObject.getX(), baseHitObject.getY()).scale(scalingFactor);

		// Calculate approximation of lazy movement on the slider
		if (baseHitObject.isSlider()) {
			tpSlider slider = new tpSlider(baseHitObject, beatmap.sliderMultiplier, beatLength);

			// Not sure if this is correct, but here we do not need 100% exact values. This comes pretty darn close in my tests.
			float sliderFollowCircleRadius = circleRadius * 3;

			int segmentLength = slider.getSegmentLength(); // baseHitObject.Length / baseHitObject.SegmentCount;
			int segmentEndTime = baseHitObject.getTime() + segmentLength;

			// For simplifying this step we use actual osu! coordinates and simply scale the length,
			// that we obtain by the ScalingFactor later
			Vec2f cursorPos = new Vec2f(baseHitObject.getX(), baseHitObject.getY());

			// Actual computation of the first lazy curve
			for (int time = baseHitObject.getTime() + LAZY_SLIDER_STEP_LENGTH; time < segmentEndTime; time += LAZY_SLIDER_STEP_LENGTH) {
				Vec2f difference = slider.getPositionAtTime(time).sub(cursorPos);
				float distance = difference.len();

				// Did we move away too far?
				if (distance > sliderFollowCircleRadius) {
					// Yep, we need to move the cursor
					difference.normalize(); // Obtain the direction of difference. We do no longer need the actual difference
					distance -= sliderFollowCircleRadius;
					cursorPos.add(difference.cpy().scale(distance)); // We move the cursor just as far as needed to stay in the follow circle
					lazySliderLengthFirst += distance;
				}
			}

			lazySliderLengthFirst *= scalingFactor;

			// If we have an odd amount of repetitions the current position will be the end of the slider.
			// Note that this will -always- be triggered if baseHitObject.SegmentCount <= 1, because
			// baseHitObject.SegmentCount can not be smaller than 1. Therefore normalizedEndPosition will
			// always be initialized
			if (baseHitObject.getRepeatCount() % 2 == 1)
				normalizedEndPosition = cursorPos.cpy().scale(scalingFactor);

			// If we have more than one segment, then we also need to compute the length of subsequent
			// lazy curves. They are different from the first one, since the first one starts right
			// at the beginning of the slider.
			if (baseHitObject.getRepeatCount() > 1) {
				// Use the next segment
				segmentEndTime += segmentLength;

				for (int time = segmentEndTime - segmentLength + LAZY_SLIDER_STEP_LENGTH; time < segmentEndTime; time += LAZY_SLIDER_STEP_LENGTH) {
					Vec2f difference = slider.getPositionAtTime(time).sub(cursorPos);
					float distance = difference.len();

					// Did we move away too far?
					if (distance > sliderFollowCircleRadius) {
						// Yep, we need to move the cursor
						difference.normalize(); // Obtain the direction of difference. We do no longer need the actual difference
						distance -= sliderFollowCircleRadius;
						cursorPos.add(difference.cpy().scale(distance)); // We move the cursor just as far as needed to stay in the follow circle
						lazySliderLengthSubsequent += distance;
					}
				}

				lazySliderLengthSubsequent *= scalingFactor;

				// If we have an even amount of repetitions the current position will be the end of the slider
				if (baseHitObject.getRepeatCount() % 2 == 0) // == 1)
					normalizedEndPosition = cursorPos.cpy().scale(scalingFactor);
			}
		} else {
			// We have a normal HitCircle or a spinner
			normalizedEndPosition = normalizedStartPosition.cpy(); //baseHitObject.EndPosition * ScalingFactor;
		}
	}

	/**
	 * Returns the strain value for a difficulty type.
	 * @param type the difficulty type ({@code DIFFICULTY_* constant})
	 */
	public double getStrain(int type) { return strains[type]; }

	/**
	 * Calculates the strain values given the previous hit object.
	 * @param previousHitObject the previous hit object
	 */
	public void calculateStrains(tpHitObject previousHitObject) {
		calculateSpecificStrain(previousHitObject, BeatmapDifficultyCalculator.DIFFICULTY_SPEED);
		calculateSpecificStrain(previousHitObject, BeatmapDifficultyCalculator.DIFFICULTY_AIM);
	}

	/**
	 * Returns the spacing weight for a distance.
	 * @param distance the distance
	 * @param type the difficulty type ({@code DIFFICULTY_* constant})
	 */
	private static double spacingWeight(double distance, int type) {
		// Caution: The subjective values are strong with this one
		switch (type) {
		case BeatmapDifficultyCalculator.DIFFICULTY_SPEED:
			double weight;
			if (distance > SINGLE_SPACING_TRESHOLD)
				weight = 2.5;
			else if (distance > STREAM_SPACING_TRESHOLD)
				weight = 1.6 + 0.9 * (distance - STREAM_SPACING_TRESHOLD) / (SINGLE_SPACING_TRESHOLD - STREAM_SPACING_TRESHOLD);
			else if (distance > ALMOST_DIAMETER)
				weight = 1.2 + 0.4 * (distance - ALMOST_DIAMETER) / (STREAM_SPACING_TRESHOLD - ALMOST_DIAMETER);
			else if (distance > ALMOST_DIAMETER / 2)
				weight = 0.95 + 0.25 * (distance - (ALMOST_DIAMETER / 2)) / (ALMOST_DIAMETER / 2);
			else
				weight = 0.95;
			return weight;
		case BeatmapDifficultyCalculator.DIFFICULTY_AIM:
			return Math.pow(distance, 0.99);
		default:
			// Should never happen.
			return 0;
		}
	}

	/**
	 * Calculates the strain value for a difficulty type given the previous hit object.
	 * @param previousHitObject the previous hit object
	 * @param type the difficulty type ({@code DIFFICULTY_* constant})
	 */
	private void calculateSpecificStrain(tpHitObject previousHitObject, int type) {
		double addition = 0;
		double timeElapsed = baseHitObject.getTime() - previousHitObject.baseHitObject.getTime();
		double decay = Math.pow(DECAY_BASE[type], timeElapsed / 1000);

		if (baseHitObject.isSpinner()) {
			// Do nothing for spinners
		} else if (baseHitObject.isSlider()) {
			switch (type) {
			case BeatmapDifficultyCalculator.DIFFICULTY_SPEED:
				// For speed strain we treat the whole slider as a single spacing entity,
				// since "Speed" is about how hard it is to click buttons fast.
				// The spacing weight exists to differentiate between being able to easily
				// alternate or having to single.
				addition = spacingWeight(previousHitObject.lazySliderLengthFirst +
						previousHitObject.lazySliderLengthSubsequent * (Math.max(previousHitObject.baseHitObject.getRepeatCount(), 1) - 1) +
						distanceTo(previousHitObject), type) * SPACING_WEIGHT_SCALING[type];
				break;

			case BeatmapDifficultyCalculator.DIFFICULTY_AIM:
				// For Aim strain we treat each slider segment and the jump after the end of
				// the slider as separate jumps, since movement-wise there is no difference
				// to multiple jumps.
				addition = (spacingWeight(previousHitObject.lazySliderLengthFirst, type) +
						spacingWeight(previousHitObject.lazySliderLengthSubsequent, type) * (Math.max(previousHitObject.baseHitObject.getRepeatCount(), 1) - 1) +
						spacingWeight(distanceTo(previousHitObject), type)) * SPACING_WEIGHT_SCALING[type];
				break;
			}
		} else if (baseHitObject.isCircle()) {
			addition = spacingWeight(distanceTo(previousHitObject), type) * SPACING_WEIGHT_SCALING[type];
		}

		// Scale addition by the time, that elapsed. Filter out HitObjects that are too
		// close to be played anyway to avoid crazy values by division through close to zero.
		// You will never find maps that require this amongst ranked maps.
		addition /= Math.max(timeElapsed, 50);

		strains[type] = previousHitObject.strains[type] * decay + addition;
	}

	/**
	 * Returns the distance to another hit object.
	 * @param other the other hit object
	 */
	public double distanceTo(tpHitObject other) {
		// Scale the distance by circle size.
		return (normalizedStartPosition.cpy().sub(other.normalizedEndPosition)).len();
	}
}

/**
 * Slider helper class to fill in some missing pieces needed in the strain calculations.
 */
class tpSlider {
	/** The slider start time. */
	private final int startTime;

	/** The time duration of the slider, in milliseconds. */
	private final int sliderTime;

	/** The slider Curve. */
	private final Curve curve;

	/**
	 * Constructor.
	 * @param hitObject the hit object
	 * @param sliderMultiplier the slider movement speed multiplier
	 * @param beatLength the beat length
	 */
	public tpSlider(HitObject hitObject, float sliderMultiplier, float beatLength) {
		this.startTime = hitObject.getTime();
		this.sliderTime = (int) hitObject.getSliderTime(sliderMultiplier, beatLength);
		this.curve = hitObject.getSliderCurve(false);
	}

	/**
	 * Returns the time duration of a slider segment, in milliseconds.
	 */
	public int getSegmentLength() { return sliderTime; }

	/**
	 * Returns the coordinates of the slider at a given track position.
	 * @param time the track position
	 */
	public Vec2f getPositionAtTime(int time) {
		float t = (time - startTime) / sliderTime;
		float floor = (float) Math.floor(t);
		t = (floor % 2 == 0) ? t - floor : floor + 1 - t;
		return curve.pointAt(t);
	}
}
