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
 * Representation of a Bezier curve with the distance between each point calculated.
 */
public class Bezier2 {
	/** The control points of the Bezier curve. */
	private Vec2f[] points;

	/** Points along the curve of the Bezier curve. */
	private Vec2f[] curve;

	/** Distances between a point of the curve and the last point. */
	private float[] curveDis;

	/** The number of points along the curve. */
	private int ncurve;

	/** The total distances of this Bezier. */
	private float totalDistance;

	/**
	 * Constructor.
	 * @param points the control points
	 */
	public Bezier2(Vec2f[] points) {
		this.points = points;

		// approximate by finding the length of all points
		// (which should be the max possible length of the curve)
		float approxlength = 0;
		for (int i = 0; i < points.length - 1; i++)
			approxlength += points[i].cpy().sub(points[i + 1]).len();

		// subdivide the curve
		this.ncurve = (int) (approxlength / 4);
		this.curve = new Vec2f[ncurve];
		for (int i = 0; i < ncurve; i++)
			curve[i] = pointAt(i / (float) ncurve);

		// find the distance of each point from the previous point
		this.curveDis = new float[ncurve];
		this.totalDistance = 0;
		for (int i = 0; i < ncurve; i++) {
			curveDis[i] = (i == 0) ? 0 : curve[i].cpy().sub(curve[i - 1]).len();
			totalDistance += curveDis[i];
		}
//		System.out.println("New Bezier2 "+points.length+" "+approxlength+" "+totalDistance());
	}

	/**
	 * Returns the point on the Bezier curve at a value t.
	 * @param t the t value [0, 1]
	 * @return the point [x, y]
	 */
	public Vec2f pointAt(float t) {
		Vec2f c = new Vec2f();
		int n = points.length - 1;
		for (int i = 0; i <= n; i++) {
			c.x += points[i].x * bernstein(i, n, t);
			c.y += points[i].y * bernstein(i, n, t);
		}
		return c;
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
	public int points() { return ncurve; }

	/**
	 * Returns the total distances of this Bezier curve.
	 */
	public float totalDistance() { return totalDistance; }

	/**
	 * Calculates the factorial of a number.
	 */
	private static long factorial(int n) {
		return (n <= 1 || n > 20) ? 1 : n * factorial(n - 1);
	}

	/**
	 * Calculates the Bernstein polynomial.
	 * @param i the index
	 * @param n the degree of the polynomial (i.e. number of points)
	 * @param t the t value [0, 1]
	 */
	private static double bernstein(int i, int n, float t) {
		return factorial(n) / (factorial(i) * factorial(n - i)) *
		       Math.pow(t, i) * Math.pow(1 - t, n - i);
	}
}
