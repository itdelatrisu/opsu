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

package itdelatrisu.opsu.ui.animations;

import itdelatrisu.opsu.Utils;

/**
 * Utility class for updating a value using an animation equation.
 */
public class AnimatedValue {
	/** The animation duration, in milliseconds. */
	private int duration;

	/** The current time, in milliseconds. */
	private int time;

	/** The base value. */
	private float base;

	/** The maximum difference from the base value. */
	private float diff;

	/** The current value. */
	private float value;

	/** The animation equation to use. */
	private AnimationEquation eqn;

	/**
	 * Constructor.
	 * @param duration the total animation duration, in milliseconds
	 * @param min the minimum value
	 * @param max the maximum value
	 * @param eqn the animation equation to use
	 */
	public AnimatedValue(int duration, float min, float max, AnimationEquation eqn) {
		this.time = 0;
		this.duration = duration;
		this.value = min;
		this.base = min;
		this.diff = max - min;
		this.eqn = eqn;
	}

	/**
	 * Returns the current value.
	 */
	public float getValue() { return value; }

	/**
	 * Returns the current animation time, in milliseconds.
	 */
	public int getTime() { return time; }

	/**
	 * Sets the animation time manually.
	 * @param time the new time, in milliseconds
	 */
	public void setTime(int time) {
		this.time = Utils.clamp(time, 0, duration);
		updateValue();
	}

	/**
	 * Returns the total animation duration, in milliseconds.
	 */
	public int getDuration() { return duration; }

	/**
	 * Sets the animation duration.
	 * @param duration the new duration, in milliseconds
	 */
	public void setDuration(int duration) {
		this.duration = duration;
		int newTime = Utils.clamp(time, 0, duration);
		if (time != newTime) {
			this.time = newTime;
			updateValue();
		}
	}

	/**
	 * Returns the animation equation being used.
	 */
	public AnimationEquation getEquation() { return eqn; }

	/**
	 * Sets the animation equation to use.
	 * @param eqn the new equation
	 */
	public void setEquation(AnimationEquation eqn) {
		this.eqn = eqn;
		updateValue();
	}

	/**
	 * Updates the animation by a delta interval.
	 * @param delta the delta interval since the last call.
	 * @return true if an update was applied, false if the animation was not updated
	 */
	public boolean update(int delta) {
		int newTime = Utils.clamp(time + delta, 0, duration);
		if (time != newTime) {
			this.time = newTime;
			updateValue();
			return true;
		}
		return false;
	}

	/**
	 * Recalculates the value by applying the animation equation with the current time.
	 */
	private void updateValue() {
		float t = eqn.calc((float) time / duration);
		this.value = base + (t * diff);
	}

	/**
	 * Returns whether the animation has completed.
	 * @return true if {@link #getTime()} equals {@link #getDuration()}
	 */
	public boolean isFinished() { return time == duration; }
}
