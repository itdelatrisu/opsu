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

import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.beatmap.OsuHitObject;

import org.newdawn.slick.Color;
import org.newdawn.slick.Image;

/**
 * Representation of a curve.
 *
 * @author fluddokt (https://github.com/fluddokt)
 */
public abstract class Curve {
	/** Points generated along the curve should be spaced this far apart. */
	protected static float CURVE_POINTS_SEPERATION = 5;

	/** The associated OsuHitObject. */
	protected OsuHitObject hitObject;

	/** The scaled starting x, y coordinates. */
	protected float x, y;

	/** The scaled slider x, y coordinate lists. */
	protected float[] sliderX, sliderY;

	/** Points along the curve (set by inherited classes). */
	protected Vec2f[] curve;

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
	public void draw(Color color) {
		if (curve == null)
			return;

		Image hitCircle = GameImage.HITCIRCLE.getImage();
		Image hitCircleOverlay = GameImage.HITCIRCLE_OVERLAY.getImage();
		for (int i = 0; i < curve.length; i++)
			hitCircleOverlay.drawCentered(curve[i].x, curve[i].y, Utils.COLOR_WHITE_FADE);
		for (int i = 0; i < curve.length; i++)
			hitCircle.drawCentered(curve[i].x, curve[i].y, color);
	}

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
