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

package itdelatrisu.opsu.ui;

import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.ui.animations.AnimatedValue;
import itdelatrisu.opsu.ui.animations.AnimationEquation;

import org.newdawn.slick.Animation;
import org.newdawn.slick.Color;
import org.newdawn.slick.Font;
import org.newdawn.slick.Image;

/**
 * A convenience class for menu buttons, consisting of an image or animation
 * and coordinates. Multi-part images currently do not support effects.
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

	/** Effect types. */
	private static final int
		EFFECT_EXPAND = 1,
		EFFECT_FADE   = 2,
		EFFECT_ROTATE = 4;

	/** The hover actions for this button. */
	private int hoverEffect = 0;

	/** The hover animation duration, in milliseconds. */
	private int animationDuration = 100;

	/** The hover animation equation. */
	private AnimationEquation animationEqn = AnimationEquation.LINEAR;

	/** Whether the animation is advancing forwards (if advancing automatically). */
	private boolean autoAnimationForward = true;

	/** The scale of the button. */
	private AnimatedValue scale;

	/** The default max scale of the button. */
	private static final float DEFAULT_SCALE_MAX = 1.25f;

	/** The alpha level of the button. */
	private AnimatedValue alpha;

	/** The default base alpha level of the button. */
	private static final float DEFAULT_ALPHA_BASE = 0.75f;

	/** The scaled expansion direction for the button. */
	private Expand dir = Expand.CENTER;

	/** Scaled expansion directions. */
	public enum Expand { CENTER, UP, RIGHT, LEFT, DOWN, UP_RIGHT, UP_LEFT, DOWN_RIGHT, DOWN_LEFT; }

	/** The rotation angle of the button. */
	private AnimatedValue angle;

	/** The default max rotation angle of the button. */
	private static final float DEFAULT_ANGLE_MAX = 30f;

	/** The last scale at which the button was drawn. */
	private float lastScale = 1f;

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
	 * @param x the x coordinate
	 */
	public void setX(float x) { this.x = x; }

	/**
	 * Sets a new center y coordinate.
	 * @param y the y coordinate
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
	 * Returns the last scale at which the button was drawn.
	 */
	public float getLastScale() { return lastScale; }

	/**
	 * Sets text to draw in the middle of the button.
	 * @param text the text to draw
	 * @param font the font to use when drawing
	 * @param color the color to draw the text
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
	public void draw() { draw(Color.white, 1f); }

	/**
	 * Draws the button with a color filter.
	 * @param filter the color to filter with when drawing
	 */
	public void draw(Color filter) { draw(filter, 1f); }

	/**
	 * Draw the button with a color filter at the given scale.
	 * @param filter the color to filter with when drawing
	 * @param scaleOverride the scale to use when drawing (only works for normal images)
	 */
	@SuppressWarnings("deprecation")
	public void draw(Color filter, float scaleOverride) {
		// animations: get current frame
		Image image = this.img;
		if (image == null) {
			anim.updateNoDraw();
			image = anim.getCurrentFrame();
		}

		// normal images
		if (imgL == null) {
			float xScaleOffset = 0f, yScaleOffset = 0f;
			if (scaleOverride != 1f) {
				image = image.getScaledCopy(scaleOverride);
				xScaleOffset = image.getWidth() / 2f - xRadius;
				yScaleOffset = image.getHeight() / 2f - yRadius;
			}
			lastScale = scaleOverride;
			if (hoverEffect == 0)
				image.draw(x - xRadius, y - yRadius, filter);
			else {
				float oldAlpha = image.getAlpha();
				float oldAngle = image.getRotation();
				if ((hoverEffect & EFFECT_EXPAND) > 0) {
					if (scale.getValue() != 1f) {
						image = image.getScaledCopy(scale.getValue());
						image.setAlpha(oldAlpha);
						if (scaleOverride != 1f) {
							xScaleOffset = image.getWidth() / 2f - xRadius;
							yScaleOffset = image.getHeight() / 2f - yRadius;
						}
						lastScale *= scale.getValue();
					}
				}
				if ((hoverEffect & EFFECT_FADE) > 0)
					image.setAlpha(alpha.getValue());
				if ((hoverEffect & EFFECT_ROTATE) > 0)
					image.setRotation(angle.getValue());
				image.draw(x - xRadius - xScaleOffset, y - yRadius - yScaleOffset, filter);
				if (image == this.img) {
					image.setAlpha(oldAlpha);
					image.setRotation(oldAngle);
				}
			}
		}

		// 3-part images
		else {
			if (hoverEffect == 0) {
				image.draw(x - xRadius + imgL.getWidth(), y - yRadius, filter);
				imgL.draw(x - xRadius, y - yRadius, filter);
				imgR.draw(x + xRadius - imgR.getWidth(), y - yRadius, filter);
			} else if ((hoverEffect & EFFECT_FADE) > 0) {
				float a = image.getAlpha(), aL = imgL.getAlpha(), aR = imgR.getAlpha();
				float currentAlpha = alpha.getValue();
				image.setAlpha(currentAlpha);
				imgL.setAlpha(currentAlpha);
				imgR.setAlpha(currentAlpha);
				image.draw(x - xRadius + imgL.getWidth(), y - yRadius, filter);
				imgL.draw(x - xRadius, y - yRadius, filter);
				imgR.draw(x + xRadius - imgR.getWidth(), y - yRadius, filter);
				image.setAlpha(a);
				imgL.setAlpha(aL);
				imgR.setAlpha(aR);
			}
		}

		// text
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
	 * Returns true if the coordinates are within the button bounds and the
	 * pixel at the specified location has an alpha level above the given bound.
	 * @param cx the x coordinate
	 * @param cy the y coordinate
	 * @param alpha the alpha level lower bound
	 */
	public boolean contains(float cx, float cy, float alpha) {
		Image image = this.img;
		if (image == null)
			image = anim.getCurrentFrame();
		float xRad = img.getWidth() / 2f, yRad = img.getHeight() / 2f;

		return ((cx > x - xRad && cx < x + xRad) &&
				(cy > y - yRad && cy < y + yRad) &&
				image.getAlphaAt((int) (cx - (x - xRad)), (int) (cy - (y - yRad))) > alpha);
	}

	/**
	 * Resets the hover fields for the button.
	 */
	public void resetHover() {
		if ((hoverEffect & EFFECT_EXPAND) > 0) {
			scale.setTime(0);
			setHoverRadius();
		}
		if ((hoverEffect & EFFECT_FADE) > 0)
			alpha.setTime(0);
		if ((hoverEffect & EFFECT_ROTATE) > 0)
			angle.setTime(0);
		autoAnimationForward = true;
	}

	/**
	 * Removes all hover effects that have been set for the button.
	 */
	public void removeHoverEffects() {
		this.hoverEffect = 0;
		this.scale = null;
		this.alpha = null;
		this.angle = null;
		autoAnimationForward = true;
	}

	/**
	 * Sets the hover animation duration.
	 * @param duration the duration, in milliseconds
	 */
	public void setHoverAnimationDuration(int duration) {
		this.animationDuration = duration;
		if (scale != null)
			scale.setDuration(duration);
		if (alpha != null)
			alpha.setDuration(duration);
		if (angle != null)
			angle.setDuration(duration);
	}

	/**
	 * Sets the hover animation equation.
	 * @param eqn the equation to use
	 */
	public void setHoverAnimationEquation(AnimationEquation eqn) {
		this.animationEqn = eqn;
		if (scale != null)
			scale.setEquation(eqn);
		if (alpha != null)
			alpha.setEquation(eqn);
		if (angle != null)
			angle.setEquation(eqn);
	}

	/**
	 * Sets the "expand" hover effect.
	 */
	public void setHoverExpand() { setHoverExpand(DEFAULT_SCALE_MAX, this.dir); }

	/**
	 * Sets the "expand" hover effect.
	 * @param scale the maximum scale factor
	 */
	public void setHoverExpand(float scale) { setHoverExpand(scale, this.dir); }

	/**
	 * Sets the "expand" hover effect.
	 * @param dir the expansion direction
	 */
	public void setHoverExpand(Expand dir) { setHoverExpand(DEFAULT_SCALE_MAX, dir); }

	/**
	 * Sets the "expand" hover effect.
	 * @param scale the maximum scale factor
	 * @param dir the expansion direction
	 */
	public void setHoverExpand(float scale, Expand dir) {
		hoverEffect |= EFFECT_EXPAND;
		this.scale = new AnimatedValue(animationDuration, 1f, scale, animationEqn);
		this.dir = dir;
	}

	/**
	 * Sets the "fade" hover effect.
	 */
	public void setHoverFade() { setHoverFade(DEFAULT_ALPHA_BASE); }

	/**
	 * Sets the "fade" hover effect.
	 * @param baseAlpha the base alpha level to fade in from
	 */
	public void setHoverFade(float baseAlpha) {
		hoverEffect |= EFFECT_FADE;
		this.alpha = new AnimatedValue(animationDuration, baseAlpha, 1f, animationEqn);
	}

	/**
	 * Returns the current alpha level from the "fade" hover effect.
	 * @return the alpha level, or 1 if "fade" is not set
	 */
	public float getHoverAlpha() { return ((hoverEffect & EFFECT_FADE) > 0) ? alpha.getValue() : 1f; }

	/**
	 * Sets the "rotate" hover effect.
	 */
	public void setHoverRotate() { setHoverRotate(DEFAULT_ANGLE_MAX); }

	/**
	 * Sets the "rotate" hover effect.
	 * @param maxAngle the maximum rotation angle, in degrees
	 */
	public void setHoverRotate(float maxAngle) {
		hoverEffect |= EFFECT_ROTATE;
		this.angle = new AnimatedValue(animationDuration, 0f, maxAngle, animationEqn);
	}

	/**
	 * Processes a hover action depending on whether or not the cursor
	 * is hovering over the button.
	 * @param delta the delta interval
	 * @param cx the x coordinate
	 * @param cy the y coordinate
	 */
	public void hoverUpdate(int delta, float cx, float cy) {
		hoverUpdate(delta, contains(cx, cy));
	}

	/**
	 * Processes a hover action depending on whether or not the cursor
	 * is hovering over the button, only if the specified pixel of the
	 * image has an alpha level above the given bound.
	 * @param delta the delta interval
	 * @param cx the x coordinate
	 * @param cy the y coordinate
	 * @param alpha the alpha level lower bound
	 */
	public void hoverUpdate(int delta, float cx, float cy, float alpha) {
		hoverUpdate(delta, contains(cx, cy, alpha));
	}

	/**
	 * Processes a hover action depending on whether or not the cursor
	 * is hovering over the button.
	 * @param delta the delta interval
	 * @param isHover true if the cursor is currently hovering over the button
	 */
	public void hoverUpdate(int delta, boolean isHover) {
		if (hoverEffect == 0)
			return;

		int d = delta * (isHover ? 1 : -1);

		// scale the button
		if ((hoverEffect & EFFECT_EXPAND) > 0) {
			if (scale.update(d))
				setHoverRadius();
		}

		// fade the button
		if ((hoverEffect & EFFECT_FADE) > 0)
			alpha.update(d);

		// rotate the button
		if ((hoverEffect & EFFECT_ROTATE) > 0)
			angle.update(d);
	}

	/**
	 * Automatically advances the hover animation in a loop.
	 * @param delta the delta interval
	 * @param reverseAtEnd whether to reverse or restart the animation upon reaching the end
	 */
	public void autoHoverUpdate(int delta, boolean reverseAtEnd) {
		if (hoverEffect == 0)
			return;

		int time = ((hoverEffect & EFFECT_EXPAND) > 0) ? scale.getTime() :
		           ((hoverEffect & EFFECT_FADE)   > 0) ? alpha.getTime() :
		           ((hoverEffect & EFFECT_ROTATE) > 0) ? angle.getTime() : -1;
		if (time == -1)
			return;

		int d = delta * (autoAnimationForward ? 1 : -1);
		if (Utils.clamp(time + d, 0, animationDuration) == time) {
			if (reverseAtEnd)
				autoAnimationForward = !autoAnimationForward;
			else {
				if ((hoverEffect & EFFECT_EXPAND) > 0)
					scale.setTime(0);
				if ((hoverEffect & EFFECT_FADE) > 0)
					alpha.setTime(0);
				if ((hoverEffect & EFFECT_ROTATE) > 0)
					angle.setTime(0);
			}
		}

		hoverUpdate(delta, autoAnimationForward);
	}

	/**
	 * Set x and y radius of the button based on current scale factor
	 * and expansion direction.
	 */
	private void setHoverRadius() {
		Image image = this.img;
		if (image == null)
			image = anim.getCurrentFrame();

		int xOffset = 0, yOffset = 0;
		float currentScale = scale.getValue();
		if (dir != Expand.CENTER) {
			// offset by difference between normal/scaled image dimensions
			xOffset = (int) ((currentScale - 1f) * image.getWidth());
			yOffset = (int) ((currentScale - 1f) * image.getHeight());
			if (dir == Expand.UP || dir == Expand.DOWN)
				xOffset = 0;    // no horizontal offset
			if (dir == Expand.RIGHT || dir == Expand.LEFT)
				yOffset = 0;    // no vertical offset
			if (dir == Expand.RIGHT || dir == Expand.DOWN_RIGHT || dir == Expand.UP_RIGHT)
				xOffset *= -1;  // flip x for right
			if (dir == Expand.DOWN ||  dir == Expand.DOWN_LEFT || dir == Expand.DOWN_RIGHT)
				yOffset *= -1;  // flip y for down
		}
		this.xRadius = ((image.getWidth() * currentScale) + xOffset) / 2f;
		this.yRadius = ((image.getHeight() * currentScale) + yOffset) / 2f;
	}
}
