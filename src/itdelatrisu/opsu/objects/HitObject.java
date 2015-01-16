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

import org.newdawn.slick.Graphics;

/**
 * Hit object interface.
 */
public interface HitObject {
	/**
	 * Draws the hit object to the graphics context.
	 * @param trackPosition the current track position
	 * @param currentObject true if this is the current hit object
	 * @param g the graphics context
	 */
	public void draw(int trackPosition, boolean currentObject, Graphics g);

	/**
	 * Updates the hit object.
	 * @param overlap true if the next object's start time has already passed
	 * @param delta the delta interval since the last call
	 * @param mouseX the x coordinate of the mouse
	 * @param mouseY the y coordinate of the mouse
	 * @return true if object ended
	 */
	public boolean update(boolean overlap, int delta, int mouseX, int mouseY);

	/**
	 * Processes a mouse click.
	 * @param x the x coordinate of the mouse
	 * @param y the y coordinate of the mouse
	 * @return true if a hit result was processed
	 */
	public boolean mousePressed(int x, int y);
}
