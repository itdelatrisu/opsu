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

import java.util.Iterator;
import java.util.LinkedList;

import org.newdawn.slick.Color;

/**
 * Representation of a Bezier curve with equidistant points.
 * http://pomax.github.io/bezierinfo/#tracing
 *
 * @author fluddokt (https://github.com/fluddokt)
 */
public abstract class EqualDistanceMultiCurve extends Curve {
	/** The angles of the first and last control points for drawing. */
	private float startAngle, endAngle;

	/** The number of points along the curve. */
	private int ncurve;

	/**
	 * Constructor.
	 * @param hitObject the associated OsuHitObject
	 * @param color the color of this curve
	 */
	public EqualDistanceMultiCurve(OsuHitObject hitObject, Color color) {
		super(hitObject, color);

	}

	public void init(LinkedList<CurveType> beziers){
		for(CurveType ct : beziers){
			System.out.println("CT:"+ct);
		}
		// now try to creates points the are equidistant to each other
		this.ncurve = (int) (hitObject.getPixelLength() / CURVE_POINTS_SEPERATION);
		this.curve = new Vec2f[ncurve + 1];

		float distanceAt = 0;
		Iterator<CurveType> iter = beziers.iterator();
		int curPoint = 0;
		CurveType curBezier = iter.next();
		Vec2f lastCurve = curBezier.getCurve()[0];
		float lastDistanceAt = 0;

		// length of Bezier should equal pixel length (in 640x480)
		float pixelLength = hitObject.getPixelLength() * OsuHitObject.getXMultiplier();

		// for each distance, try to get in between the two points that are between it
		for (int i = 0; i < ncurve + 1; i++) {
			int prefDistance = (int) (i * pixelLength / ncurve);
			while (distanceAt < prefDistance) {
				lastDistanceAt = distanceAt;
				lastCurve = curBezier.getCurve()[curPoint];
				distanceAt += curBezier.getCurveDistances()[curPoint++];

				if (curPoint >= curBezier.getCurvesCount()) {
					if (iter.hasNext()) {
						curBezier = iter.next();
						curPoint = 0;
					} else {
						curPoint = curBezier.getCurvesCount() - 1;
						if (lastDistanceAt == distanceAt) {
							// out of points even though the preferred distance hasn't been reached
							break;
						}
					}
				}
			}
			System.out.println("prefDis:"+prefDistance+" "+distanceAt+" "+lastDistanceAt);
			Vec2f thisCurve = curBezier.getCurve()[curPoint];

			// interpolate the point between the two closest distances
			if (distanceAt - lastDistanceAt > 1) {
				float t = (prefDistance - lastDistanceAt) / (distanceAt - lastDistanceAt);
				curve[i] = new Vec2f(lerp(lastCurve.x, thisCurve.x, t), lerp(lastCurve.y, thisCurve.y, t));
			} else
				curve[i] = thisCurve;
		}

//		if (hitObject.getRepeatCount() > 1) {
			Vec2f c1 = curve[0];
			int cnt = 1;
			Vec2f c2 = curve[cnt++];
			while (cnt <= ncurve && c2.cpy().sub(c1).len() < 1)
				c2 = curve[cnt++];
			this.startAngle = (float) (Math.atan2(c2.y - c1.y, c2.x - c1.x) * 180 / Math.PI);

			c1 = curve[ncurve];
			cnt = ncurve - 1;
			c2 = curve[cnt--];
			while (cnt >= 0 && c2.cpy().sub(c1).len() < 1)
				c2 = curve[cnt--];
			this.endAngle = (float) (Math.atan2(c2.y - c1.y, c2.x - c1.x) * 180 / Math.PI);
//		}
	}
	
	@Override
	public float[] pointAt(float t) {
		float indexF = t * ncurve;
		int index = (int) indexF;
		if (index >= ncurve) {
			Vec2f poi = curve[ncurve];
			return new float[] { poi.x, poi.y };
		} else {
			Vec2f poi = curve[index];
			Vec2f poi2 = curve[index + 1];
			float t2 = indexF - index;
			return new float[] {
				lerp(poi.x, poi2.x, t2),
				lerp(poi.y, poi2.y, t2)
			};
		}
	}

	@Override
	public float getEndAngle() { return endAngle; }

	@Override
	public float getStartAngle() { return startAngle; }
}
