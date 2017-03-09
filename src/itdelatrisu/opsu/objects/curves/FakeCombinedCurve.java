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
import itdelatrisu.opsu.render.LegacyCurveRenderState;

import java.util.ArrayList;
import java.util.List;

import org.newdawn.slick.Color;

/**
 * Combined curve (for merging sliders).
 *
 * @author yugecin (https://github.com/yugecin)
 */
public class FakeCombinedCurve extends Curve {
	/** The current points to render (pairs of indices: from, to). */
	private List<Integer> pointsToRender = new ArrayList<Integer>();

	/**
	 * Constructor.
	 * @param points the list of curve points
	 */
	public FakeCombinedCurve(Vec2f[] points) {
		super(new HitObject("0,0,0,1,2"), false);
		this.curve = points;
	}

	/** Clears the list of points to render. */
	public void clearPoints() { pointsToRender.clear(); }

	/**
	 * Adds a range of points to render.
	 * @param from the start index to render
	 * @param to the end point index to render
	 */
	public void addRange(int from, int to) {
		pointsToRender.add(from);
		pointsToRender.add(to);
	}

	@Override
	public void draw(Color color) {
		if (legacyRenderState == null)
			legacyRenderState = new LegacyCurveRenderState(hitObject, curve);
		legacyRenderState.draw(color, borderColor, pointsToRender);
	}

	@Override
	public Vec2f pointAt(float t) { return null; }

	@Override
	public float getEndAngle() { return 0; }

	@Override
	public float getStartAngle() { return 0; }
}
