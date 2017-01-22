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
 * Representation of a Centripetal Catmull–Rom spline.
 * (Currently not technically Centripetal Catmull–Rom.)
 * http://en.wikipedia.org/wiki/Centripetal_Catmull%E2%80%93Rom_spline
 *
 * @author fluddokt (https://github.com/fluddokt)
 */
public class CentripetalCatmullRom extends CurveType {
	/** The time values of the Catmull curve. */
	private float [] time;

	/** The control points of the Catmull curve. */
	private Vec2f[] points;

	/**
	 * Constructor.
	 * @param points the control points of the curve
	 */
	protected CentripetalCatmullRom(Vec2f[] points) {
		if (points.length != 4)
			throw new RuntimeException(String.format("Need exactly 4 points to initialize CentripetalCatmullRom, %d provided.", points.length));

		this.points = points;
		time = new float[4];
		time[0] = 0;
		float approxLength = 0;
		for (int i = 1; i < 4; i++) {
			float len = 0;
			if (i > 0)
				len = points[i].cpy().sub(points[i - 1]).len();
			if (len <= 0)
				len += 0.0001f;
			approxLength += len;
			// time[i] = (float) Math.sqrt(len) + time[i-1];// ^(0.5)
			time[i] = i;
		}

		init(approxLength / 2);
	}

	@Override
	public Vec2f pointAt(float t) {
		t = t * (time[2] - time[1]) + time[1];

		Vec2f A1 = points[0].cpy().scale((time[1] - t) / (time[1] - time[0]))
			  .add(points[1].cpy().scale((t - time[0]) / (time[1] - time[0])));
		Vec2f A2 = points[1].cpy().scale((time[2] - t) / (time[2] - time[1]))
			  .add(points[2].cpy().scale((t - time[1]) / (time[2] - time[1])));
		Vec2f A3 = points[2].cpy().scale((time[3] - t) / (time[3] - time[2]))
			  .add(points[3].cpy().scale((t - time[2]) / (time[3] - time[2])));

		Vec2f B1 = A1.cpy().scale((time[2] - t) / (time[2] - time[0]))
			  .add(A2.cpy().scale((t - time[0]) / (time[2] - time[0])));
		Vec2f B2 = A2.cpy().scale((time[3] - t) / (time[3] - time[1]))
			  .add(A3.cpy().scale((t - time[1]) / (time[3] - time[1])));

		Vec2f C = B1.cpy().scale((time[2] - t) / (time[2] - time[1]))
			 .add(B2.cpy().scale((t - time[1]) / (time[2] - time[1])));

		return C;
	}
}
