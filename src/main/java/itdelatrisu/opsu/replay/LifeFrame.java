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
	/** Time. */
	private final int time;

	/** Percentage. */
	private final float percentage;

	/**
	 * Constructor.
	 * @param time the time
	 * @param percentage the percentage
	 */
	public LifeFrame(int time, float percentage) {
		this.time = time;
		this.percentage = percentage;
	}

	/**
	 * Returns the frame time.
	 */
	public int getTime() { return time; }

	/**
	 * Returns the frame percentage.
	 */
	public float getPercentage() { return percentage; }

	@Override
	public String toString() {
		return String.format("(%d, %.2f)", time, percentage);
	}
}
