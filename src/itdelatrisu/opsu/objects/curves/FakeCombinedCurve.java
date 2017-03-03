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

public class FakeCombinedCurve extends Curve {

	public FakeCombinedCurve(Vec2f[] points) {
		super(new HitObject(0, 0, 0), false);
		this.curve = points;
	}

	@Override
	public Vec2f pointAt(float t) {
		return null;
	}

	@Override
	public float getEndAngle() {
		return 0;
	}

	@Override
	public float getStartAngle() {
		return 0;
	}

}
