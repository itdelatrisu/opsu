/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014, 2015 Jeffrey Han
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

package itdelatrisu.opsu.objects.curves;

import itdelatrisu.opsu.OsuHitObject;

import org.newdawn.slick.Color;

/**
 * Representation of a curve.
 *
 * @author fluddokt (https://github.com/fluddokt)
 */
public abstract class Curve {
	/** The associated OsuHitObject. */
	protected OsuHitObject hitObject;

	/** The scaled starting x, y coordinates. */
	protected float x, y;

	/** The scaled slider x, y coordinate lists. */
	protected float[] sliderX, sliderY;

	/**
	 * Constructor.
	 * @param hitObject the associated OsuHitObject
	 * @param color the color of this curve
	 */
	protected Curve(OsuHitObject hitObject, Color color) {
		this.hitObject = hitObject;
		this.x = hitObject.getScaledX();
		this.y = hitObject.getScaledY();
		this.sliderX = hitObject.getScaledSliderX();
		this.sliderY = hitObject.getScaledSliderY();
	}

	/**
	 * Returns the point on the curve at a value t.
	 * @param t the t value [0, 1]
	 * @return the point [x, y]
	 */
	public abstract float[] pointAt(float t);

	/**
	 * Draws the full curve to the graphics context.
	 * @param color the color filter
	 */
	public abstract void draw(Color color);

	/**
	 * Returns the angle of the first control point.
	 */
	public abstract float getEndAngle();

	/**
	 * Returns the angle of the last control point.
	 */
	public abstract float getStartAngle();

	/**
	 * Returns the scaled x coordinate of the control point at index i.
	 * @param i the control point index
	 */
	public float getX(int i) { return (i == 0) ? x : sliderX[i - 1]; }

	/**
	 * Returns the scaled y coordinate of the control point at index i.
	 * @param i the control point index
	 */
	public float getY(int i) { return (i == 0) ? y : sliderY[i - 1]; }

	/**
	 * Linear interpolation of a and b at t.
	 */
	protected float lerp(float a, float b, float t) {
		return a * (1 - t) + b * t;
	}
}
