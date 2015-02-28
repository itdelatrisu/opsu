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
import itdelatrisu.opsu.OsuHitObject;
import itdelatrisu.opsu.Utils;

import java.util.Iterator;
import java.util.LinkedList;

import org.newdawn.slick.Color;
import org.newdawn.slick.Image;

/**
 * Representation of a Bezier curve with equidistant points.
 * http://pomax.github.io/bezierinfo/#tracing
 *
 * @author fluddokt (https://github.com/fluddokt)
 */
public class LinearBezier extends Curve {
	/** The angles of the first and last control points for drawing. */
	private float startAngle, endAngle;

	/** List of Bezier curves in the set of points. */
	private LinkedList<Bezier2> beziers = new LinkedList<Bezier2>();

	/** Points along the curve at equal distance. */
	private Vec2f[] curve;

	/** The number of points along the curve. */
	private int ncurve;

	/**
	 * Constructor.
	 * @param hitObject the associated OsuHitObject
	 * @param color the color of this curve
	 */
	public LinearBezier(OsuHitObject hitObject, Color color) {
		super(hitObject, color);

		// splits points into different Beziers if has the same points (red points)
		int controlPoints = hitObject.getSliderX().length + 1;
		LinkedList<Vec2f> points = new LinkedList<Vec2f>();  // temporary list of points to separate different Bezier curves
		Vec2f lastPoi = null;
		for (int i = 0; i < controlPoints; i++) {
			Vec2f tpoi = new Vec2f(getX(i), getY(i));
			if (lastPoi != null && tpoi.equals(lastPoi)) {
				if (points.size() >= 2)
					beziers.add(new Bezier2(points.toArray(new Vec2f[0])));
				points.clear();
			}
			points.add(tpoi);
			lastPoi = tpoi;
		}
		if (points.size() < 2) {
			// trying to continue Bezier with less than 2 points
			// probably ending on a red point, just ignore it
		} else {
			beziers.add(new Bezier2(points.toArray(new Vec2f[0])));
			points.clear();
		}

		// find the length of all beziers
//		int totalDistance = 0;
//		for (Bezier2 bez : beziers) {
//			totalDistance += bez.totalDistance();
//		}

		// now try to creates points the are equidistant to each other
		this.ncurve = (int) (hitObject.getPixelLength() / 5f);
		this.curve = new Vec2f[ncurve + 1];

		float distanceAt = 0;
		Iterator<Bezier2> iter = beziers.iterator();
		int curPoint = 0;
		Bezier2 curBezier = iter.next();
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

				if (curPoint >= curBezier.points()) {
					if (iter.hasNext()) {
						curBezier = iter.next();
						curPoint = 0;
					} else {
						curPoint = curBezier.points() - 1;
						if(lastDistanceAt == distanceAt){
							//out of points even though we haven't reached the preferred distance.
							break;
						}
					}
				}
			}
			Vec2f thisCurve = curBezier.getCurve()[curPoint];

			// interpolate the point between the two closest distances
			if (distanceAt - lastDistanceAt > 1) {
				float t = (prefDistance - lastDistanceAt) / (distanceAt - lastDistanceAt);
				curve[i] = new Vec2f(lerp(lastCurve.x, thisCurve.x, t), lerp(lastCurve.y, thisCurve.y, t));
//				System.out.println("Dis "+i+" "+prefDistance+" "+lastDistanceAt+" "+distanceAt+" "+curPoint+" "+t);
			} else
				curve[i] = thisCurve;
		}

//		if (hitObject.getRepeatCount() > 1) {
			Vec2f c1 = curve[0];
			int cnt = 1;
			Vec2f c2 = curve[cnt++];
			while (c2.cpy().sub(c1).len() < 1)
				c2 = curve[cnt++];
			this.startAngle = (float) (Math.atan2(c2.y - c1.y, c2.x - c1.x) * 180 / Math.PI);
			c1 = curve[ncurve - 1];
			cnt = ncurve - 2;
			c2 = curve[cnt];
			while (c2.cpy().sub(c1).len() < 1)
				c2 = curve[cnt--];
			this.endAngle = (float) (Math.atan2(c2.y - c1.y, c2.x - c1.x) * 180 / Math.PI);
//		}
//		System.out.println("Total Distance: "+totalDistance+" "+distanceAt+" "+beziers.size()+" "+hitObject.getPixelLength()+" "+OsuHitObject.getXMultiplier());
	}

	@Override
	public float[] pointAt(float t) {
		float indexF = t * ncurve;
		int index = (int) indexF;
		if (index >= ncurve) {
			Vec2f poi = curve[ncurve - 1];
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
	public void draw() {
		Image hitCircle = GameImage.HITCIRCLE.getImage();
		Image hitCircleOverlay = GameImage.HITCIRCLE_OVERLAY.getImage();
		for (int i = curve.length - 2; i >= 0; i--)
			hitCircleOverlay.drawCentered(curve[i].x, curve[i].y, Utils.COLOR_WHITE_FADE);
		for (int i = curve.length - 2; i >= 0; i--)
			hitCircle.drawCentered(curve[i].x, curve[i].y, color);
	}

	@Override
	public float getEndAngle() { return endAngle; }

	@Override
	public float getStartAngle() { return startAngle; }
}
