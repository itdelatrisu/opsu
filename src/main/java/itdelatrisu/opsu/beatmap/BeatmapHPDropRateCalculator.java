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

import itdelatrisu.opsu.GameData;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.db.BeatmapDB;

/**
 * osu's HP drop rate algorithm.
 *
 * @author peppy (ppy/osu-iPhone:OsuFiletype.m)
 */
public class BeatmapHPDropRateCalculator {
	/** The beatmap. */
	private final Beatmap beatmap;

	/** The HP drain rate. */
	private final float hpDrainRate;

	/** The overall difficulty. */
	private final float overallDifficulty;

	/** The HP drop rate. */
	private float hpDropRate;

	/** The normal HP multiplier. */
	private float hpMultiplierNormal;

	/** The combo-end HP multiplier. */
	private float hpMultiplierComboEnd;

	/**
	 * Constructor. Call {@link #calculate()} to run all computations.
	 * <p>
	 * If any parts of the beatmap have not yet been loaded (e.g. timing points,
	 * hit objects), they will be loaded here.
	 * @param beatmap the beatmap
	 * @param hpDrainRate the HP drain rate
	 */
	public BeatmapHPDropRateCalculator(Beatmap beatmap, float hpDrainRate, float overallDifficulty) {
		this.beatmap = beatmap;
		this.hpDrainRate = hpDrainRate;
		this.overallDifficulty = overallDifficulty;
		if (beatmap.timingPoints == null)
			BeatmapDB.load(beatmap, BeatmapDB.LOAD_ARRAY);
		BeatmapParser.parseHitObjects(beatmap);
	}

	/** Returns the HP drop rate. */
	public float getHpDropRate() { return hpDropRate; }

	/** Returns the normal HP multiplier. */
	public float getHpMultiplierNormal() { return hpMultiplierNormal; }

	/** Returns the combo-end HP multiplier. */
	public float getHpMultiplierComboEnd() { return hpMultiplierComboEnd; }

	/** Calculates the HP drop rate for the beatmap. */
	public void calculate() {
		float lowestHpEver = Utils.mapDifficultyRange(hpDrainRate, 195, 160, 60);
		float lowestHpComboEnd = Utils.mapDifficultyRange(hpDrainRate, 198, 170, 80);
		float lowestHpEnd = Utils.mapDifficultyRange(hpDrainRate, 198, 180, 80);
		float hpRecoveryAvailable = Utils.mapDifficultyRange(hpDrainRate, 8, 4, 0);
		int approachTime = (int) Utils.mapDifficultyRange(overallDifficulty, 1800, 1200, 450);

		float testDrop = 0.05f;
		Health health = new Health();
		hpMultiplierNormal = hpMultiplierComboEnd = 1.0f;

		while (true) {
			health.reset();
			health.setModifiers(hpDrainRate, hpMultiplierNormal, hpMultiplierComboEnd);
			double lowestHp = health.getRawHealth();
			int lastTime = beatmap.objects[0].getTime() - approachTime;
			int comboTooLowCount = 0;
			boolean fail = false;
			int timingPointIndex = 0;
			float beatLengthBase = 1f, beatLength = 1f;

			for (int i = 0; i < beatmap.objects.length; i++) {
				HitObject hitObject = beatmap.objects[i];

				// breaks
				int breakTime = 0;
				if (beatmap.breaks != null) {
					for (int j = 0; j < beatmap.breaks.size(); j += 2) {
						int breakStart = beatmap.breaks.get(j), breakEnd = beatmap.breaks.get(j+1);
						if (breakStart >= lastTime && breakEnd <= hitObject.getTime()) {
							breakTime = breakEnd - breakStart;
							break;
						}
					}
				}
				health.changeHealth(-testDrop * (hitObject.getTime() - lastTime - breakTime));

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

				// compute end time
				int endTime;
				if (hitObject.isCircle())
					endTime = hitObject.getTime();
				else if (hitObject.isSlider()) {
					float sliderTime = hitObject.getSliderTime(beatmap.sliderMultiplier, beatLength);
					float sliderTimeTotal = sliderTime * hitObject.getRepeatCount();
					endTime = hitObject.getTime() + (int) sliderTimeTotal;
				} else
					endTime = hitObject.getEndTime();
				lastTime = endTime;

				if (health.getRawHealth() < lowestHp)
					lowestHp = health.getRawHealth();

				if (health.getRawHealth() <= lowestHpEver) {
					fail = true;
					testDrop *= 0.96f;
					break;
				}

				health.changeHealth(-testDrop * (endTime - hitObject.getTime()));

				// hit objects
				if (hitObject.isSlider()) {
					float tickLengthDiv = 100f * beatmap.sliderMultiplier / beatmap.sliderTickRate / (beatLength / beatLengthBase);
					int tickCount = (int) Math.ceil(hitObject.getPixelLength() / tickLengthDiv) - 1;
					for (int j = 0; j < hitObject.getRepeatCount(); j++)
						health.changeHealthForHit(GameData.HIT_SLIDER30);
					for (int j = 0; j < tickCount * hitObject.getRepeatCount(); j++)
						health.changeHealthForHit(GameData.HIT_SLIDER10);
				} else if (hitObject.isSpinner()) {
					float spinsPerMinute = 100 + (beatmap.overallDifficulty * 15);
					int rotationsNeeded = (int) (spinsPerMinute * (hitObject.getEndTime() - hitObject.getTime()) / 60000f);
					for (int j = 0; j < rotationsNeeded; j++)
						health.changeHealthForHit(GameData.HIT_SPINNERSPIN);
				}
				health.changeHealthForHit(GameData.HIT_300);
				if (i == beatmap.objects.length - 1 || beatmap.objects[i + 1].isNewCombo()) {
					health.changeHealthForHit(GameData.HIT_300G);
					if (health.getRawHealth() < lowestHpComboEnd) {
						if (++comboTooLowCount > 2) {
							fail = true;
							hpMultiplierNormal *= 1.03;
							hpMultiplierComboEnd *= 1.07;
							break;
						}
					}
				}
			}

			if (!fail && health.getRawHealth() < lowestHpEnd) {
				fail = true;
				testDrop *= 0.94f;
				hpMultiplierNormal *= 1.01;
				hpMultiplierComboEnd *= 1.01;
			}

			double recovery = (health.getUncappedRawHealth() - Health.HP_MAX) / beatmap.objects.length;
			if (!fail && recovery < hpRecoveryAvailable) {
				fail = true;
				testDrop *= 0.96;
				hpMultiplierNormal *= 1.01;
				hpMultiplierComboEnd *= 1.02;
			}

			if (fail)
				continue;

			hpDropRate = testDrop;
			break;
		}
	}
}
