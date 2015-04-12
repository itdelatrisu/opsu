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

/**
 * Representation of a curve with the distance between each point calculated.
 *
 * @author fluddokt (https://github.com/fluddokt)
 */
public abstract class CurveType {

	/** Points along the curve of the Bezier curve. */
	private Vec2f[] curve;

	/** Distances between a point of the curve and the last point. */
	private float[] curveDis;

	/** The number of points along the curve. */
	private int ncurve;

	/** The total distances of this Bezier. */
	private float totalDistance;

	public abstract Vec2f pointAt(float t);
	
	public void init(float approxlength){
		// subdivide the curve
		this.ncurve = (int) (approxlength / 4) + 1;
		this.curve = new Vec2f[ncurve];
		System.out.println(approxlength+" "+ncurve);
		for (int i = 0; i < ncurve; i++)
			curve[i] = pointAt(i / (float) (ncurve - 1));

		// find the distance of each point from the previous point
		this.curveDis = new float[ncurve];
		this.totalDistance = 0;
		for (int i = 0; i < ncurve; i++) {
			curveDis[i] = (i == 0) ? 0 : curve[i].cpy().sub(curve[i - 1]).len();
			totalDistance += curveDis[i];
			System.out.println("curvedis:"+curveDis[i]);
		}
	}
	/**
	 * Returns the points along the curve of the Bezier curve.
	 */
	public Vec2f[] getCurve() { return curve; }

	/**
	 * Returns the distances between a point of the curve and the last point.
	 */
	public float[] getCurveDistances() { return curveDis; }

	/**
	 * Returns the number of points along the curve.
	 */
	public int getCurvesCount() { return ncurve; }

	/**
	 * Returns the total distances of this Bezier curve.
	 */
	public float totalDistance() { return totalDistance; }

}
