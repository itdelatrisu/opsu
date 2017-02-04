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

package itdelatrisu.opsu.replay;

/**
 * Captures a single life frame.
 *
 * @author smoogipooo (https://github.com/smoogipooo/osu-Replay-API/)
 */
public class LifeFrame {
	/** The sample interval, in milliseconds, when saving replays. */
	public static final int SAMPLE_INTERVAL = 2000;

	/** Time, in milliseconds. */
	private final int time;

	/** Health. */
	private final float health;

	/**
	 * Constructor.
	 * @param time the time (in ms)
	 * @param health the health [0,1]
	 */
	public LifeFrame(int time, float health) {
		this.time = time;
		this.health = health;
	}

	/**
	 * Returns the frame time, in milliseconds.
	 */
	public int getTime() { return time; }

	/**
	 * Returns the health.
	 */
	public float getHealth() { return health; }

	@Override
	public String toString() {
		return String.format("(%d, %.2f)", time, health);
	}
}
