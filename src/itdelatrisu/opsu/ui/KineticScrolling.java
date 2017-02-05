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

package itdelatrisu.opsu.ui;

/**
 * Drag to scroll based on:
 * http://ariya.ofilabs.com/2013/11/javascript-kinetic-scrolling-part-2.html
 *
 * @author fluddokt (https://github.com/fluddokt)
 */
public class KineticScrolling {
	/** The moving averaging constant. */
	private static final float AVG_CONST = 0.2f, ONE_MINUS_AVG_CONST = 1 - AVG_CONST;

	/** The constant used to determine how fast the target position will be reached. */
	private static final int TIME_CONST = 200;

	/** The constant used to determine how much of the velocity will be used to launch to the target. */
	private static final float AMPLITUDE_CONST = 0.25f;

	/** The current position. */
	private float position;

	/** The offset to scroll to the target position. */
	private float amplitude;

	/** The current target to scroll to. */
	private float target;

	/** The total amount of time since the mouse button was released. */
	private float totalDelta;

	/** The maximum and minimum value the position can reach. */
	private float max = Float.MAX_VALUE, min = -Float.MAX_VALUE;

	/** Whether the mouse is currently pressed or not. */
	private boolean pressed = false;

	/** The change in position since the last update. */
	private float deltaPosition;

	/** The moving average of the velocity. */
	private float avgVelocity;

	/** The speed multiplier (divides {@link #TIME_CONST}). */
	private float speedMultiplier = 1f;

	/** Whether or not to allow overscrolling. */
	private boolean allowOverScroll;

	/**
	 * Enable or disable the overscrolling flag.
	 * @param allowOverScroll whether or not to allow overscrolling
	 */
	public void setAllowOverScroll(boolean allowOverScroll) {
		this.allowOverScroll = allowOverScroll;
	}

	/**
	 * Returns the current position.
	 * @return the position
	 */
	public float getPosition() { return position; }

	/**
	 * Returns the target position.
	 * @return the target position
	 */
	public float getTargetPosition() { return target; }

	/**
	 * Returns the minimum value.
	 * @return the min
	 */
	public float getMin() { return min; }

	/**
	 * Returns the minimum value.
	 * @return the max
	 */
	public float getMax() { return max; }

	/**
	 * Returns if the mouse state is currently pressed.
	 * @return true if pressed
	 */
	public boolean isPressed() { return pressed; }

	/**
	 * Updates the scrolling.
	 * @param delta the elapsed time since the last update
	 */
	public void update(float delta) {
		if (!pressed) {
			totalDelta += delta;
			position = target + (float) (-amplitude * Math.exp(-totalDelta / (TIME_CONST / speedMultiplier)));
		} else {
			avgVelocity = (ONE_MINUS_AVG_CONST * avgVelocity + AVG_CONST * (deltaPosition * 1000f / delta));

			position += deltaPosition;
			target = position;
			deltaPosition = 0;
		}
		if (allowOverScroll && pressed) {
			return;
		}
		if (position > max) {
			if (allowOverScroll) {
				scrollToPosition(max);
			} else {
				amplitude = 0;
				target = position = max;
			}
		}
		if (position < min) {
			if (allowOverScroll) {
				scrollToPosition(min);
			} else {
				amplitude = 0;
				target = position = min;
			}
		}
	}

	/**
	 * Scrolls to the position.
	 * @param newPosition the position to scroll to
	 */
	public void scrollToPosition(float newPosition) {
		pressed();
		released();
		amplitude = newPosition - position;
		target = newPosition;
		totalDelta = 0;
	}

	/**
	 * Scrolls to an offset from target.
	 * @param offset the offset from the target to scroll to
	 */
	public void scrollOffset(float offset) {
		scrollToPosition(target + offset);
	}

	/**
	 * Sets the position.
	 * @param newPosition the position to be set
	 */
	public void setPosition(float newPosition) {
		pressed();
		released();
		target = newPosition;
		position = target;
	}

	/**
	 * Set the position relative to an offset.
	 * @param offset the offset from the position
	 */
	public void addOffset(float offset) {
		setPosition(position + offset);
	}

	/**
	 * Call this when the mouse button has been pressed.
	 */
	public void pressed() {
		if (pressed)
			return;
		pressed = true;
		avgVelocity = 0;
	}

	/**
	 * Call this when the mouse button has been released.
	 */
	public void released() {
		if (!pressed)
			return;
		pressed = false;
		amplitude = AMPLITUDE_CONST * avgVelocity;
		target = Math.round(target + amplitude);
		totalDelta = 0;
	}

	/**
	 * Call this when the mouse has been dragged.
	 * @param distance the amount that the mouse has been dragged
	 */
	public void dragged(float distance) {
		deltaPosition += distance;
	}

	/**
	 * Set the minimum and maximum bound.
	 * @param min the minimum bound
	 * @param max the maximum bound
	 */
	public void setMinMax(float min, float max) {
		this.min = min;
		this.max = max;
	}

	/**
	 * Sets the multiplier for how fast the target position will be reached.
	 * @param multiplier the speed multiplier (e.g. 1f = normal speed, 2f = reaches in half the time)
	 * @throws IllegalArgumentException if the multiplier is negative or zero
	 */
	public void setSpeedMultiplier(float multiplier) {
		if (multiplier <= 0f)
			throw new IllegalArgumentException("Speed multiplier must be above zero.");
		this.speedMultiplier = multiplier;
	}

	/**
	 * Returns the speed multiplier.
	 */
	public float getSpeedMultiplier() { return speedMultiplier; }
}
