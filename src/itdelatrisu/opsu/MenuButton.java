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

package itdelatrisu.opsu;

import org.newdawn.slick.Animation;
import org.newdawn.slick.Color;
import org.newdawn.slick.Font;
import org.newdawn.slick.Image;

/**
 * A convenience class for menu buttons.
 * Consists of an image or animation and coordinates.
 * Multi-part images and animations currently do not support hover updates.
 */
public class MenuButton {
	/** The image associated with the button. */
	private Image img;

	/** The left and right parts of the button (optional). */
	private Image imgL, imgR;

	/** The animation associated with the button. */
	private Animation anim;

	/** The center coordinates. */
	private float x, y;

	/** The x and y radius of the button (scaled). */
	private float xRadius, yRadius;

	/** The text to draw on the button. */
	private String text;

	/** The font to draw the text with. */
	private Font font;

	/** The color to draw the text with. */
	private Color color;

	/** Hover action types. */
	private enum HoverAction { NONE, EXPAND, FADE };

	/** The hover action for this button. */
	private HoverAction hoverAction = HoverAction.NONE;

	/** The current and max scale of the button (for hovering). */
	private float scale = 1f, hoverScale = 1.25f;

	/** The current and base alpha level of the button (for hovering). */
	private float alpha = 1f, baseAlpha = 0.75f;

	/** The scaled expansion direction for the button (for hovering). */
	private Expand dir = Expand.CENTER;

	/** Scaled expansion directions (for hovering). */
	public enum Expand { CENTER, UP, RIGHT, LEFT, DOWN, UP_RIGHT, UP_LEFT, DOWN_RIGHT, DOWN_LEFT; }

	/**
	 * Creates a new button from an Image.
	 * @param img the image
	 * @param x the center x coordinate
	 * @param y the center y coordinate
	 */
	public MenuButton(Image img, float x, float y) {
		this.img = img;
		this.x = x;
		this.y = y;
		this.xRadius = img.getWidth() / 2f;
		this.yRadius = img.getHeight() / 2f;
	}

	/**
	 * Creates a new button from a 3-part Image.
	 * @param imgCenter the center image
	 * @param imgLeft the left image
	 * @param imgRight the right image
	 * @param x the center x coordinate
	 * @param y the center y coordinate
	 */
	public MenuButton(Image imgCenter, Image imgLeft, Image imgRight, float x, float y) {
		this.img  = imgCenter;
		this.imgL = imgLeft;
		this.imgR = imgRight;
		this.x  = x;
		this.y  = y;
		this.xRadius = (img.getWidth() + imgL.getWidth() + imgR.getWidth()) / 2f;
		this.yRadius = img.getHeight() / 2f;
	}

	/**
	 * Creates a new button from an Animation.
	 * @param anim the animation
	 * @param x the center x coordinate
	 * @param y the center y coordinate
	 */
	public MenuButton(Animation anim, float x, float y) {
		this.anim = anim;
		this.x = x;
		this.y = y;
		this.xRadius = anim.getWidth() / 2f;
		this.yRadius = anim.getHeight() / 2f;
	}

	/**
	 * Sets a new center x coordinate.
	 */
	public void setX(float x) { this.x = x; }

	/**
	 * Sets a new center y coordinate.
	 */
	public void setY(float y) { this.y = y; }

	/**
	 * Returns the center x coordinate.
	 */
	public float getX() { return x; }

	/**
	 * Returns the center y coordinate.
	 */
	public float getY() { return y; }

	/**
	 * Sets text to draw in the middle of the button.
	 * @param text the text to draw
	 * @param font the font to use when drawing
	 * @color the color to draw the text
	 */
	public void setText(String text, Font font, Color color) {
		this.text = text;
		this.font = font;
		this.color = color;
	}

	/**
	 * Returns the associated image.
	 */
	public Image getImage() { return img; }

	/**
	 * Returns the associated animation.
	 */
	public Animation getAnimation() { return anim; }

	/**
	 * Draws the button.
	 */
	public void draw() {
		if (img != null) {
			if (imgL == null) {
				if (hoverAction == HoverAction.EXPAND) {
					Image imgScaled = (scale == 1f) ? img : img.getScaledCopy(scale);
					imgScaled.setAlpha(img.getAlpha());
					imgScaled.draw(x - xRadius, y - yRadius);
				} else if (hoverAction == HoverAction.FADE) {
					float a = img.getAlpha();
					img.setAlpha(alpha);
					img.draw(x - xRadius, y - yRadius);
					img.setAlpha(a);
				} else
					img.draw(x - xRadius, y - yRadius);
			} else {
				if (hoverAction == HoverAction.FADE) {
					float a = img.getAlpha(), aL = imgL.getAlpha(), aR = imgR.getAlpha();
					img.setAlpha(alpha);
					imgL.setAlpha(alpha);
					imgR.setAlpha(alpha);
					img.draw(x - xRadius + imgL.getWidth(), y - yRadius);
					imgL.draw(x - xRadius, y - yRadius);
					imgR.draw(x + xRadius - imgR.getWidth(), y - yRadius);
					img.setAlpha(a);
					imgL.setAlpha(aL);
					imgR.setAlpha(aR);
				} else {
					img.draw(x - xRadius + imgL.getWidth(), y - yRadius);
					imgL.draw(x - xRadius, y - yRadius);
					imgR.draw(x + xRadius - imgR.getWidth(), y - yRadius);
				}
			}
		} else
			anim.draw(x - xRadius, y - yRadius);
		if (text != null)
			font.drawString(x - font.getWidth(text) / 2f, y - font.getLineHeight() / 2f, text, color);
	}

	/**
	 * Draw the button with a color filter.
	 * @param filter the color to filter with when drawing
	 */
	public void draw(Color filter) {
		if (img != null) {
			if (imgL == null) {
				if (hoverAction == HoverAction.EXPAND) {
					Image imgScaled = (scale == 1f) ? img : img.getScaledCopy(scale);
					imgScaled.setAlpha(img.getAlpha());
					imgScaled.draw(x - xRadius, y - yRadius, filter);
				} else if (hoverAction == HoverAction.FADE) {
					float a = img.getAlpha();
					img.setAlpha(alpha);
					img.draw(x - xRadius, y - yRadius, filter);
					img.setAlpha(a);
				} else
					img.draw(x - xRadius, y - yRadius, filter);
			} else {
				if (hoverAction == HoverAction.FADE) {
					float a = img.getAlpha(), aL = imgL.getAlpha(), aR = imgR.getAlpha();
					img.setAlpha(alpha);
					imgL.setAlpha(alpha);
					imgR.setAlpha(alpha);
					img.draw(x - xRadius + imgL.getWidth(), y - yRadius, filter);
					imgL.draw(x - xRadius, y - yRadius, filter);
					imgR.draw(x + xRadius - imgR.getWidth(), y - yRadius, filter);
					img.setAlpha(a);
					imgL.setAlpha(aL);
					imgR.setAlpha(aR);
				} else {
					img.draw(x - xRadius + imgL.getWidth(), y - yRadius, filter);
					imgL.draw(x - xRadius, y - yRadius, filter);
					imgR.draw(x + xRadius - imgR.getWidth(), y - yRadius, filter);
				}
			}
		} else
			anim.draw(x - xRadius, y - yRadius, filter);
		if (text != null)
			font.drawString(x - font.getWidth(text) / 2f, y - font.getLineHeight() / 2f, text, color);
	}

	/**
	 * Returns true if the coordinates are within the button bounds.
	 * @param cx the x coordinate
	 * @param cy the y coordinate
	 */
	public boolean contains(float cx, float cy) {
		return ((cx > x - xRadius && cx < x + xRadius) &&
				(cy > y - yRadius && cy < y + yRadius));
	}

	/**
	 * Resets the hover fields for the button.
	 */
	public void resetHover() {
		if (hoverAction == HoverAction.EXPAND) {
			this.scale = 1f;
			setHoverRadius();
		} else if (hoverAction == HoverAction.FADE)
			this.alpha = baseAlpha;
	}

	/**
	 * Sets the hover action to "expand".
	 */
	public void setHoverExpand() { this.hoverAction = HoverAction.EXPAND; }

	/**
	 * Sets the hover action to "expand".
	 * @param scale the maximum scale factor (default 1.25f)
	 */
	public void setHoverExpand(float scale) { setHoverExpand(scale, this.dir); }

	/**
	 * Sets the hover action to "expand".
	 * @param dir the expansion direction
	 */
	public void setHoverExpand(Expand dir) { setHoverExpand(this.hoverScale, dir); }

	/**
	 * Sets the hover action to "expand".
	 * @param scale the maximum scale factor (default 1.25f)
	 * @param dir the expansion direction
	 */
	public void setHoverExpand(float scale, Expand dir) {
		this.hoverAction = HoverAction.EXPAND;
		this.hoverScale = scale;
		this.dir = dir;
	}

	/**
	 * Sets the hover action to "fade".
	 */
	public void setHoverFade() { this.hoverAction = HoverAction.FADE; }

	/**
	 * Sets the hover action to "fade".
	 * @param baseAlpha the base alpha level to fade in from (default 0.7f)
	 */
	public void setHoverFade(float baseAlpha) {
		this.hoverAction = HoverAction.FADE;
		this.baseAlpha = baseAlpha;
	}

	/**
	 * Processes a hover action depending on whether or not the cursor
	 * is hovering over the button.
	 * @param delta the delta interval
	 * @param cx the x coordinate
	 * @param cy the y coordinate
	 */
	public void hoverUpdate(int delta, float cx, float cy) {
		if (hoverAction == HoverAction.NONE)
			return;

		boolean isHover = contains(cx, cy);
		if (hoverAction == HoverAction.EXPAND) {
			// scale the button
			int sign;
			if (isHover && scale < hoverScale)
				sign = 1;
			else if (!isHover && scale > 1f)
				sign = -1;
			else
				return;
			scale = Utils.getBoundedValue(scale, sign * (hoverScale - 1f) * delta / 100f, 1, hoverScale);
			setHoverRadius();
		} else {
			// fade the button
			int sign;
			if (isHover && alpha < 1f)
				sign = 1;
			else if (!isHover && alpha > baseAlpha)
				sign = -1;
			else
				return;
			alpha = Utils.getBoundedValue(alpha, sign * (1f - baseAlpha) * delta / 200f, baseAlpha, 1f);
		}
	}

	/**
	 * Set x and y radius of the button based on current scale factor
	 * and expansion direction.
	 */
	private void setHoverRadius() {
		int xOffset = 0, yOffset = 0;
		if (dir != Expand.CENTER) {
			// offset by difference between normal/scaled image dimensions
			xOffset = (int) ((scale - 1f) * img.getWidth());
			yOffset = (int) ((scale - 1f) * img.getHeight());
			if (dir == Expand.UP || dir == Expand.DOWN)
				xOffset = 0;    // no horizontal offset
			if (dir == Expand.RIGHT || dir == Expand.LEFT)
				yOffset = 0;    // no vertical offset
			if (dir == Expand.RIGHT || dir == Expand.DOWN_RIGHT || dir == Expand.UP_RIGHT)
				xOffset *= -1;  // flip x for right
			if (dir == Expand.DOWN ||  dir == Expand.DOWN_LEFT || dir == Expand.DOWN_RIGHT)
				yOffset *= -1;  // flip y for down
		}
		this.xRadius = ((img.getWidth() * scale) + xOffset) / 2f;
		this.yRadius = ((img.getHeight() * scale) + yOffset) / 2f;
	}
}