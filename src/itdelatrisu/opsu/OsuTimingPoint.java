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

/**
 * Data type representing a timing point.
 */
public class OsuTimingPoint {
	public int time;               // start time/offset (in ms)
	public float beatLength;       // (non-inherited) ms per beat
	public int velocity = 0;       // (inherited) slider multiplier = -100 / value
	public int meter;              // beats per measure
	public byte sampleType;        // sound samples (0:none, 1:normal, 2:soft, 3:drum)
	public byte sampleTypeCustom;  // custom samples (0:default, 1:custom1, 2:custom2)
	public int sampleVolume;       // volume of samples (0~100)
	public boolean inherited;      // is this timing point inherited?
	public boolean kiai;           // is Kiai Mode active?

	/**
	 * Constructor.
	 */
	public OsuTimingPoint() {}
}