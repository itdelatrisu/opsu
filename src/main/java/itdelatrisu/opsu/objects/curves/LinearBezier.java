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

import itdelatrisu.opsu.beatmap.HitObject;

import java.util.LinkedList;

/**
 * Representation of Bezier curve with equidistant points.
 * http://pomax.github.io/bezierinfo/#tracing
 *
 * @author fluddokt (https://github.com/fluddokt)
 */
public class LinearBezier extends EqualDistanceMultiCurve {
	/**
	 * Constructor.
	 * @param hitObject the associated HitObject
	 * @param line whether a new curve should be generated for each sequential pair
	 */
	public LinearBezier(HitObject hitObject, boolean line) {
		this(hitObject, line, true);
	}

	/**
	 * Constructor.
	 * @param hitObject the associated HitObject
	 * @param line whether a new curve should be generated for each sequential pair
	 * @param scaled whether to use scaled coordinates
	 */
	public LinearBezier(HitObject hitObject, boolean line, boolean scaled) {
		super(hitObject, scaled);

		LinkedList<CurveType> beziers = new LinkedList<CurveType>();

		// Beziers: splits points into different Beziers if has the same points (red points)
		// a b c - c d - d e f g
		// Lines: generate a new curve for each sequential pair
		// ab  bc  cd  de  ef  fg
		int controlPoints = hitObject.getSliderX().length + 1;
		LinkedList<Vec2f> points = new LinkedList<Vec2f>();  // temporary list of points to separate different Bezier curves
		Vec2f lastPoi = null;
		for (int i = 0; i < controlPoints; i++) {
			Vec2f tpoi = new Vec2f(getX(i), getY(i));
			if (line) {
				if (lastPoi != null) {
					points.add(tpoi);
					beziers.add(new Bezier2(points.toArray(new Vec2f[0])));
					points.clear();
				}
			} else if (lastPoi != null && tpoi.equals(lastPoi)) {
				if (points.size() >= 2)
					beziers.add(new Bezier2(points.toArray(new Vec2f[0])));
				points.clear();
			}
			points.add(tpoi);
			lastPoi = tpoi;
		}
		if (line || points.size() < 2) {
			// trying to continue Bezier with less than 2 points
			// probably ending on a red point, just ignore it
		} else {
			beziers.add(new Bezier2(points.toArray(new Vec2f[0])));
			points.clear();
		}

		init(beziers);
	}
}
