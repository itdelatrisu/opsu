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

package itdelatrisu.opsu.objects;

import itdelatrisu.opsu.OsuHitObject;

import org.newdawn.slick.Graphics;

/**
 * Dummy hit object, used when another HitObject class cannot be created.
 */
public class DummyObject implements HitObject {
	/** The associated OsuHitObject. */
	private OsuHitObject hitObject;

	/** The scaled starting x, y coordinates. */
	private float x, y;

	/**
	 * Constructor.
	 * @param hitObject the associated OsuHitObject
	 */
	public DummyObject(OsuHitObject hitObject) {
		this.hitObject = hitObject;
		this.x = hitObject.getScaledX();
		this.y = hitObject.getScaledY();
	}

	@Override
	public void draw(Graphics g, int trackPosition) {}

	@Override
	public boolean update(boolean overlap, int delta, int mouseX, int mouseY, boolean keyPressed, int trackPosition) {
		return (trackPosition > hitObject.getTime());
	}

	@Override
	public boolean mousePressed(int x, int y, int trackPosition) { return false; }

	@Override
	public OsuHitObject getHitObject() { return hitObject; }

	@Override
	public void updatePosition() {
		this.x = hitObject.getScaledX();
		this.y = hitObject.getScaledY();
	}

	@Override
	public float[] getPointAt(int trackPosition) { return new float[] { x, y }; }

	@Override
	public int getEndTime() { return hitObject.getTime(); }
}
