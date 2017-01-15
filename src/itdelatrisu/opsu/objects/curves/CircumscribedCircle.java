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

package itdelatrisu.opsu.objects.curves;

import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.beatmap.HitObject;

/**
 * Representation of a curve along a Circumscribed Circle of three points.
 * http://en.wikipedia.org/wiki/Circumscribed_circle
 *
 * @author fluddokt (https://github.com/fluddokt)
 */
public class CircumscribedCircle extends Curve {
	/** PI constants. */
	private static final float
		TWO_PI  = (float) (Math.PI * 2),
		HALF_PI = (float) (Math.PI / 2);

	/** The center of the Circumscribed Circle. */
	private Vec2f circleCenter;

	/** The radius of the Circumscribed Circle. */
	private float radius;

	/** The three points to create the Circumscribed Circle from. */
	private Vec2f start, mid, end;

	/** The three angles relative to the circle center. */
	private float startAng, endAng, midAng;

	/** The start and end angles for drawing. */
	private float drawStartAngle, drawEndAngle;

	/**
	 * Constructor.
	 * @param hitObject the associated HitObject
	 */
	public CircumscribedCircle(HitObject hitObject) {
		this(hitObject, true);
	}

	/**
	 * Constructor.
	 * @param hitObject the associated HitObject
	 * @param scaled whether to use scaled coordinates
	 */
	public CircumscribedCircle(HitObject hitObject, boolean scaled) {
		super(hitObject, scaled);

		// construct the three points
		this.start = new Vec2f(getX(0), getY(0));
		this.mid   = new Vec2f(getX(1), getY(1));
		this.end   = new Vec2f(getX(2), getY(2));

		// find the circle center
		Vec2f mida = start.midPoint(mid);
		Vec2f midb = end.midPoint(mid);
		Vec2f nora = mid.cpy().sub(start).nor();
		Vec2f norb = mid.cpy().sub(end).nor();

		this.circleCenter = intersect(mida, nora, midb, norb);

		// find the angles relative to the circle center
		Vec2f startAngPoint = start.cpy().sub(circleCenter);
		Vec2f midAngPoint   = mid.cpy().sub(circleCenter);
		Vec2f endAngPoint   = end.cpy().sub(circleCenter);

		this.startAng = (float) Math.atan2(startAngPoint.y, startAngPoint.x);
		this.midAng   = (float) Math.atan2(midAngPoint.y, midAngPoint.x);
		this.endAng   = (float) Math.atan2(endAngPoint.y, endAngPoint.x);

		// find the angles that pass through midAng
		if (!isIn(startAng, midAng, endAng)) {
			if (Math.abs(startAng + TWO_PI - endAng) < TWO_PI && isIn(startAng + (TWO_PI), midAng, endAng))
				startAng += TWO_PI;
			else if (Math.abs(startAng - (endAng + TWO_PI)) < TWO_PI && isIn(startAng, midAng, endAng + (TWO_PI)))
				endAng += TWO_PI;
			else if (Math.abs(startAng - TWO_PI - endAng) < TWO_PI && isIn(startAng - (TWO_PI), midAng, endAng))
				startAng -= TWO_PI;
			else if (Math.abs(startAng - (endAng - TWO_PI)) < TWO_PI && isIn(startAng, midAng, endAng - (TWO_PI)))
				endAng -= TWO_PI;
			else
				throw new RuntimeException(String.format("Cannot find angles between midAng (%.3f %.3f %.3f).", startAng, midAng, endAng));
		}

		// find an angle with an arc length of pixelLength along this circle
		this.radius = startAngPoint.len();
		float pixelLength = hitObject.getPixelLength() * HitObject.getXMultiplier();
		float arcAng = pixelLength / radius;  // len = theta * r / theta = len / r

		// now use it for our new end angle
		this.endAng = (endAng > startAng) ? startAng + arcAng : startAng - arcAng;

		// finds the angles to draw for repeats
		this.drawEndAngle   = (float) ((endAng   + (startAng > endAng ? HALF_PI : -HALF_PI)) * 180 / Math.PI);
		this.drawStartAngle = (float) ((startAng + (startAng > endAng ? -HALF_PI : HALF_PI)) * 180 / Math.PI);

		// calculate points
		float step = hitObject.getPixelLength() / CURVE_POINTS_SEPERATION;
		curve = new Vec2f[(int) step + 1];
		for (int i = 0; i < curve.length; i++)
			curve[i] = pointAt(i / step);
	}

	/**
	 * Checks to see if "b" is between "a" and "c"
	 * @return true if b is between a and c
	 */
	private boolean isIn(float a, float b, float c) {
		return (b > a && b < c) || (b < a && b > c);
	}

	/**
	 * Finds the point of intersection between the two parametric lines
	 * {@code A = a + ta*t} and {@code B = b + tb*u}.
	 * http://gamedev.stackexchange.com/questions/44720/
	 * @param a  the initial position of the line A
	 * @param ta the direction of the line A
	 * @param b  the initial position of the line B
	 * @param tb the direction of the line B
	 * @return the point at which the two lines intersect
	 */
	private Vec2f intersect(Vec2f a, Vec2f ta, Vec2f b, Vec2f tb) {
		// xy = a + ta * t = b + tb * u
		// t =(b + tb*u -a)/ta
		//t(x) == t(y)
		//(b.x + tb.x*u -a.x)/ta.x = (b.y + tb.y*u -a.y)/ta.y
		// b.x*ta.y + tb.x*u*ta.y -a.x*ta.y = b.y*ta.x + tb.y*u*ta.x -a.y*ta.x
		// tb.x*u*ta.y - tb.y*u*ta.x= b.y*ta.x  -a.y*ta.x -b.x*ta.y +a.x*ta.y
		//u *(tb.x*ta.y - tb.y*ta.x) = (b.y-a.y)ta.x +(a.x-b.x)ta.y
		//u = ((b.y-a.y)ta.x +(a.x-b.x)ta.y) / (tb.x*ta.y - tb.y*ta.x);

		float des = tb.x * ta.y - tb.y * ta.x;
		if (Math.abs(des) < 0.00001f)
			throw new RuntimeException("Vectors are parallel.");
		float u = ((b.y - a.y) * ta.x + (a.x - b.x) * ta.y) / des;
		return b.cpy().add(tb.x * u, tb.y * u);
	}

	@Override
	public Vec2f pointAt(float t) {
		float ang = Utils.lerp(startAng, endAng, t);
		return new Vec2f(
			(float) (Math.cos(ang) * radius + circleCenter.x),
			(float) (Math.sin(ang) * radius + circleCenter.y)
		);
	}

	@Override
	public float getEndAngle() { return drawEndAngle; }

	@Override
	public float getStartAngle() { return drawStartAngle; }
}
