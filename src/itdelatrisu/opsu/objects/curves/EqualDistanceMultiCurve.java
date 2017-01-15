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

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Representation of multiple curve with equidistant points.
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
	 * @param hitObject the associated HitObject
	 */
	public EqualDistanceMultiCurve(HitObject hitObject) {
		this(hitObject, true);
	}

	/**
	 * Constructor.
	 * @param hitObject the associated HitObject
	 * @param scaled whether to use scaled coordinates
	 */
	public EqualDistanceMultiCurve(HitObject hitObject, boolean scaled) {
		super(hitObject, scaled);
	}

	/**
	 * Initialize the curve points with equal distance.
	 * Must be called by inherited classes.
	 * @param curvesList a list of curves to join
	 */
	public void init(LinkedList<CurveType> curvesList){
		// now try to creates points the are equidistant to each other
		this.ncurve = (int) (hitObject.getPixelLength() / CURVE_POINTS_SEPERATION);
		this.curve = new Vec2f[ncurve + 1];

		float distanceAt = 0;
		Iterator<CurveType> iter = curvesList.iterator();
		int curPoint = 0;
		CurveType curCurve = iter.next();
		Vec2f lastCurve = curCurve.getCurvePoint()[0];
		float lastDistanceAt = 0;

		// length of Curve should equal pixel length (in 640x480)
		float pixelLength = hitObject.getPixelLength() * HitObject.getXMultiplier();

		// for each distance, try to get in between the two points that are between it
		for (int i = 0; i < ncurve + 1; i++) {
			int prefDistance = (int) (i * pixelLength / ncurve);
			while (distanceAt < prefDistance) {
				lastDistanceAt = distanceAt;
				lastCurve = curCurve.getCurvePoint()[curPoint];
				curPoint++;

				if (curPoint >= curCurve.getCurvesCount()) {
					if (iter.hasNext()) {
						curCurve = iter.next();
						curPoint = 0;
					} else {
						curPoint = curCurve.getCurvesCount() - 1;
						if (lastDistanceAt == distanceAt) {
							// out of points even though the preferred distance hasn't been reached
							break;
						}
					}
				}
				distanceAt += curCurve.getCurveDistances()[curPoint];
			}
			Vec2f thisCurve = curCurve.getCurvePoint()[curPoint];

			// interpolate the point between the two closest distances
			if (distanceAt - lastDistanceAt > 1) {
				float t = (prefDistance - lastDistanceAt) / (distanceAt - lastDistanceAt);
				curve[i] = new Vec2f(Utils.lerp(lastCurve.x, thisCurve.x, t), Utils.lerp(lastCurve.y, thisCurve.y, t));
			} else
				curve[i] = thisCurve;
		}

//		if (hitObject.getRepeatCount() > 1) {
			Vec2f c1 = curve[0];
			int cnt = 1;

			if (cnt > ncurve) {
				return;
			}

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
	public Vec2f pointAt(float t) {
		float indexF = t * ncurve;
		int index = (int) indexF;
		if (index >= ncurve)
			return curve[ncurve].cpy();
		else {
			Vec2f poi = curve[index];
			Vec2f poi2 = curve[index + 1];
			float t2 = indexF - index;
			return new Vec2f(
				Utils.lerp(poi.x, poi2.x, t2),
				Utils.lerp(poi.y, poi2.y, t2)
			);
		}
	}

	@Override
	public float getEndAngle() { return endAngle; }

	@Override
	public float getStartAngle() { return startAngle; }
}
