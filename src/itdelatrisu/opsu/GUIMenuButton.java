/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014 Jeffrey Han
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

package itdelatrisu.opsu;

import org.newdawn.slick.Animation;
import org.newdawn.slick.Color;
import org.newdawn.slick.Image;

/**
 * A convenience class for menu buttons.
 * Consists of an image or animation and coordinates.
 */
public class GUIMenuButton {
	/**
	 * The image associated with the button.
	 */
	private Image img;

	/**
	 * The left and right parts of the button (optional).
	 */
	private Image imgL, imgR;

	/**
	 * The animation associated with the button.
	 */
	private Animation anim;

	/**
	 * The center coordinates.
	 */
	private float x, y;

	/**
	 * The x and y radius of the button.
	 */
	private float xRadius, yRadius;

	/**
	 * Creates a new button from an Image.
	 */
	public GUIMenuButton(Image img, float x, float y) {
		this.img = img;
		this.x   = x;
		this.y   = y;

		xRadius = img.getWidth() / 2f;
		yRadius = img.getHeight() / 2f;
	}

	/**
	 * Creates a new button from a 3-part Image.
	 */
	public GUIMenuButton(Image imgCenter, Image imgLeft, Image imgRight,
			float x, float y) {
		this.img  = imgCenter;
		this.imgL = imgLeft;
		this.imgR = imgRight;
		this.x    = x;
		this.y    = y;

		xRadius = (img.getWidth() + imgL.getWidth() + imgR.getWidth()) / 2f;
		yRadius = img.getHeight() / 2f;
	}

	/**
	 * Creates a new button from an Animation.
	 */
	public GUIMenuButton(Animation anim, float x, float y) {
		this.anim = anim;
		this.x    = x;
		this.y    = y;

		xRadius = anim.getWidth() / 2f;
		yRadius = anim.getHeight() / 2f;
	}

	/**
	 * Sets/returns new center coordinates.
	 */
	public void setX(float x) { this.x = x; }
	public void setY(float y) { this.y = y; }
	public float getX() { return x; }
	public float getY() { return y; }

	/**
	 * Returns the associated image or animation.
	 */
	public Image getImage() { return img; }
	public Animation getAnimation() { return anim; }

	/**
	 * Draws the button.
	 */
	public void draw() {
		if (img != null) {
			if (imgL == null)
				img.draw(x - xRadius, y - yRadius);
			else {
				img.draw(x - xRadius + imgL.getWidth(), y - yRadius);
				imgL.draw(x - xRadius, y - yRadius);
				imgR.draw(x + xRadius - imgR.getWidth(), y - yRadius);
			}
		} else
			anim.draw(x - xRadius, y - yRadius);
	}

	/**
	 * Draw the button with a color filter.
	 * @param filter the color to filter with when drawing
	 */
	public void draw(Color filter) {
		if (img != null) {
			if (imgL == null)
				img.draw(x - xRadius, y - yRadius, filter);
			else {
				img.draw(x - xRadius + imgL.getWidth(), y - yRadius, filter);
				imgL.draw(x - xRadius, y - yRadius, filter);
				imgR.draw(x + xRadius - imgR.getWidth(), y - yRadius, filter);
			}
		} else
			anim.draw(x - xRadius, y - yRadius, filter);
	}

	/**
	 * Returns true if the coordinates are within the button bounds.
	 */
	public boolean contains(float cx, float cy) {
		return ((cx > x - xRadius && cx < x + xRadius) &&
				(cy > y - yRadius && cy < y + yRadius));
	}
}