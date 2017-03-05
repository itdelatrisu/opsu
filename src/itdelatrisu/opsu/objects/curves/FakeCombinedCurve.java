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
import itdelatrisu.opsu.render.LegacyCurveRenderState;
import org.newdawn.slick.Color;

import java.util.LinkedList;

public class FakeCombinedCurve extends Curve {

	private LinkedList<Utils.Pair<Integer, Integer>> pointsToRender;

	public FakeCombinedCurve(Vec2f[] points) {
		super(new HitObject(0, 0, 0), false);
		this.curve = points;
		pointsToRender = new LinkedList<>();
	}

	public void initForFrame() {
		pointsToRender.clear();
	}

	public void addRange(int from, int to) {
		pointsToRender.add(new Utils.Pair<>(from, to));
	}

	@Override
	public void draw(Color color) {
		if (legacyRenderState == null)
			legacyRenderState = new LegacyCurveRenderState(hitObject, curve);
		legacyRenderState.draw(color, borderColor, pointsToRender);
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
