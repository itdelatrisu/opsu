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

/**
 * Representation of a Bezier curve with the distance between each point calculated.
 *
 * @author fluddokt (https://github.com/fluddokt)
 */
public class Bezier2 extends CurveType {
	/** The control points of the Bezier curve. */
	private Vec2f[] points;

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

		init(approxlength);
	}

	@Override
	public Vec2f pointAt(float t) {
		Vec2f c = new Vec2f();
		int n = points.length - 1;
		for (int i = 0; i <= n; i++) {
			double b = bernstein(i, n, t);
			c.x += points[i].x * b;
			c.y += points[i].y * b;
		}
		return c;
	}

	/**
	 * Calculates the binomial coefficient.
	 * http://en.wikipedia.org/wiki/Binomial_coefficient#Binomial_coefficient_in_programming_languages
	 */
	private static long binomialCoefficient(int n, int k) {
		if (k < 0 || k > n)
			return 0;
		if (k == 0 || k == n)
			return 1;
		k = Math.min(k, n - k);  // take advantage of symmetry
		long c = 1;
		for (int i = 0; i < k; i++)
			c = c * (n - i) / (i + 1);
		return c;
	}

	/**
	 * Calculates the Bernstein polynomial.
	 * @param i the index
	 * @param n the degree of the polynomial (i.e. number of points)
	 * @param t the t value [0, 1]
	 */
	private static double bernstein(int i, int n, float t) {
		return binomialCoefficient(n, i) * Math.pow(t, i) * Math.pow(1 - t, n - i);
	}
}
