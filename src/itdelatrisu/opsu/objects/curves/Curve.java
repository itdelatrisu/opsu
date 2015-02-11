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

	/** The color of this curve. */
	protected Color color;

	/**
	 * Constructor.
	 * @param hitObject the associated OsuHitObject
	 * @param color the color of this curve
	 */
	protected Curve(OsuHitObject hitObject, Color color) {
		this.hitObject = hitObject;
		this.color = color;
	}

	/**
	 * Returns the point on the curve at a value t.
	 * @param t the t value [0, 1]
	 * @return the point [x, y]
	 */
	public abstract float[] pointAt(float t);

	/**
	 * Draws the full curve to the graphics context.
	 */
	public abstract void draw();

	/**
	 * Returns the angle of the first control point.
	 */
	public abstract float getEndAngle();

	/**
	 * Returns the angle of the last control point.
	 */
	public abstract float getStartAngle();

	/**
	 * Returns the x coordinate of the control point at index i.
	 */
	protected float getX(int i) {
		return (i == 0) ? hitObject.getX() : hitObject.getSliderX()[i - 1];
	}

	/**
	 * Returns the y coordinate of the control point at index i.
	 */
	protected float getY(int i) {
		return (i == 0) ? hitObject.getY() : hitObject.getSliderY()[i - 1];
	}

	/**
	 * Linear interpolation of a and b at t.
	 */
	protected float lerp(float a, float b, float t) {
		return a * (1 - t) + b * t;
	}

	/**
	 * A recursive method to evaluate polynomials in Bernstein form or Bezier curves.
	 * http://en.wikipedia.org/wiki/De_Casteljau%27s_algorithm
	 */
	protected float deCasteljau(float[] a, int i, int order, float t) {
		if (order == 0)
			return a[i];
		return lerp(deCasteljau(a, i, order - 1, t), deCasteljau(a, i + 1, order - 1, t), t);
	}
}
