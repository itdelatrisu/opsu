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

import itdelatrisu.opsu.Utils;

import org.newdawn.slick.util.Log;

/**
 * Data type representing a timing point.
 */
public class TimingPoint {
	/** Timing point start time/offset (in ms). */
	private int time = 0;

	/** Time per beat (in ms). [NON-INHERITED] */
	private float beatLength = 0f;

	/** Slider multiplier. [INHERITED] */
	private int velocity = 0;

	/** Beats per measure. */
	private int meter = 4;

	/** Sound sample type. */
	private byte sampleType = 1;

	/** Custom sound sample type. */
	private byte sampleTypeCustom = 0;

	/** Volume of samples. [0, 100] */
	private int sampleVolume = 100;

	/** Whether or not this timing point is inherited. */
	private boolean inherited = false;

	/** Whether or not Kiai Mode is active. */
	private boolean kiai = false;

	/**
	 * Constructor.
	 * @param line the line to be parsed
	 */
	public TimingPoint(String line) {
		/**
		 * [TIMING POINT FORMATS]
		 * Non-inherited:
		 *   offset,msPerBeat,meter,sampleType,sampleSet,volume,inherited,kiai
		 *
		 * Inherited:
		 *   offset,velocity,meter,sampleType,sampleSet,volume,inherited,kiai
		 */
		// TODO: better support for old formats
		String[] tokens = line.split(",");
		try {
			this.time = (int) Float.parseFloat(tokens[0]);  // rare float
			this.meter = Integer.parseInt(tokens[2]);
			this.sampleType = Byte.parseByte(tokens[3]);
			this.sampleTypeCustom = Byte.parseByte(tokens[4]);
			this.sampleVolume = Integer.parseInt(tokens[5]);
//			this.inherited = Utils.parseBoolean(tokens[6]);
			if (tokens.length > 7)
				this.kiai = Utils.parseBoolean(tokens[7]);
		} catch (ArrayIndexOutOfBoundsException e) {
			Log.debug(String.format("Error parsing timing point: '%s'", line));
		}

		// tokens[1] is either beatLength (positive) or velocity (negative)
		float beatLength = Float.parseFloat(tokens[1]);
		if (beatLength > 0)
			this.beatLength = beatLength;
		else {
			this.velocity = (int) beatLength;
			this.inherited = true;
		}
	}

	/**
	 * Returns the timing point start time/offset.
	 * @return the start time (in ms)
	 */
	public int getTime() { return time; }

	/**
	 * Returns the beat length. [NON-INHERITED]
	 * @return the time per beat (in ms)
	 */
	public float getBeatLength() { return beatLength; }

	/**
	 * Returns the slider multiplier. [INHERITED]
	 */
	public float getSliderMultiplier() { return Utils.clamp(-velocity, 10, 1000) / 100f; }

	/**
	 * Returns the meter.
	 * @return the number of beats per measure
	 */
	public int getMeter() { return meter; }

	/**
	 * Returns the sample type.
	 * <ul>
	 * <li>0: none
	 * <li>1: normal
	 * <li>2: soft
	 * <li>3: drum
	 * </ul>
	 */
	public byte getSampleType() { return sampleType; }

	/**
	 * Returns the custom sample type.
	 * <ul>
	 * <li>0: default
	 * <li>1: custom 1
	 * <li>2: custom 2
	 * </ul>
	 */
	public byte getSampleTypeCustom() { return sampleTypeCustom; }

	/**
	 * Returns the sample volume.
	 * @return the sample volume [0, 1]
	 */
	public float getSampleVolume() { return sampleVolume / 100f; }

	/**
	 * Returns whether or not this timing point is inherited.
	 * @return the inherited
	 */
	public boolean isInherited() { return inherited; }

	/**
	 * Returns whether or not Kiai Time is active.
	 * @return true if active
	 */
	public boolean isKiaiTimeActive() { return kiai; }

	@Override
	public String toString() {
		if (inherited)
			return String.format("%d,%d,%d,%d,%d,%d,%d,%d",
				time, velocity, meter, (int) sampleType,
				(int) sampleTypeCustom, sampleVolume, 1, (kiai) ? 1: 0);
		else
			return String.format("%d,%g,%d,%d,%d,%d,%d,%d",
				time, beatLength, meter, (int) sampleType,
				(int) sampleTypeCustom, sampleVolume, 0, (kiai) ? 1: 0);
	}
}