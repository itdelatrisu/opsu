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

import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.beatmap.HitObject;
import itdelatrisu.opsu.options.Options;
import itdelatrisu.opsu.render.CurveRenderState;
import itdelatrisu.opsu.render.LegacyCurveRenderState;
import itdelatrisu.opsu.skins.Skin;
import itdelatrisu.opsu.ui.Colors;

import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GLContext;
import org.newdawn.slick.Color;
import org.newdawn.slick.Image;
import org.newdawn.slick.util.Log;

/**
 * Representation of a curve.
 *
 * @author fluddokt (https://github.com/fluddokt)
 */
public abstract class Curve {
	/** Points generated along the curve should be spaced this far apart. */
	protected static float CURVE_POINTS_SEPERATION = 5;

	/** The curve border color. */
	protected static Color borderColor;

	/** Whether mmsliders are supported. */
	private static boolean mmsliderSupported = false;

	/** The associated HitObject. */
	protected HitObject hitObject;

	/** The scaled starting x, y coordinates. */
	protected float x, y;

	/** The scaled slider x, y coordinate lists. */
	protected float[] sliderX, sliderY;

	/** Per-curve render-state used for the new style curve renders. */
	private CurveRenderState renderState;

	/** Per-curve render-state used for the legacy style curve renders. */
	protected LegacyCurveRenderState legacyRenderState;

	/** Points along the curve (set by inherited classes). */
	protected Vec2f[] curve;

	/**
	 * Constructor.
	 * @param hitObject the associated HitObject
	 * @param scaled whether to use scaled coordinates
	 */
	protected Curve(HitObject hitObject, boolean scaled) {
		this.hitObject = hitObject;
		if (scaled) {
			this.x = hitObject.getScaledX();
			this.y = hitObject.getScaledY();
			this.sliderX = hitObject.getScaledSliderX();
			this.sliderY = hitObject.getScaledSliderY();
		} else {
			this.x = hitObject.getX();
			this.y = hitObject.getY();
			this.sliderX = hitObject.getSliderX();
			this.sliderY = hitObject.getSliderY();
		}
		this.renderState = null;
	}

	/**
	 * Set the width and height of the container that Curves get drawn into.
	 * Should be called before any curves are drawn.
	 * @param width the container width
	 * @param height the container height
	 * @param circleDiameter the circle diameter
	 * @param borderColor the curve border color
	 */
	public static void init(int width, int height, float circleDiameter, Color borderColor) {
		Curve.borderColor = borderColor;

		ContextCapabilities capabilities = GLContext.getCapabilities();
		mmsliderSupported = capabilities.OpenGL30;
		if (mmsliderSupported) {
			CurveRenderState.init(width, height, circleDiameter);
			LegacyCurveRenderState.init(width, height, circleDiameter);
		} else {
			if (Options.getSkin().getSliderStyle() != Skin.STYLE_PEPPYSLIDER)
				Log.warn("New slider style requires OpenGL 3.0.");
		}
	}

	/**
	 * Returns the points along the curve.
	 */
	public Vec2f[] getCurvePoints() { return curve; }

	/**
	 * Returns the point on the curve at a value t.
	 * @param t the t value [0, 1]
	 * @return the position vector
	 */
	public abstract Vec2f pointAt(float t);

	/**
	 * Draws the full curve to the graphics context.
	 * @param color the color filter
	 */
	public void draw(Color color) { draw(color, 1f); }

	/**
	 * Draws the curve in the range [0, t] (where the full range is [0, 1]) to the graphics context.
	 * @param color the color filter
	 * @param t set the curve interval to [0, t]
	 */
	public void draw(Color color, float t) {
		if (curve == null)
			return;

		t = Utils.clamp(t, 0f, 1f);

		// peppysliders
		if (Options.getSkin().getSliderStyle() == Skin.STYLE_PEPPYSLIDER || !mmsliderSupported) {
			int drawUpTo = (int) (curve.length * t);
			Image hitCircle = GameImage.HITCIRCLE.getImage();
			Image hitCircleOverlay = GameImage.HITCIRCLE_OVERLAY.getImage();
			for (int i = 0; i < drawUpTo; i++)
				hitCircleOverlay.drawCentered(curve[i].x, curve[i].y, Colors.WHITE_FADE);
			for (int i = 0; i < drawUpTo; i++)
				hitCircle.drawCentered(curve[i].x, curve[i].y, color);
		}

		// mmsliders
		else {
			if (renderState == null)
				renderState = new CurveRenderState(hitObject, curve);
			renderState.draw(color, borderColor, t);
		}
	}

	/**
	 * Draws a section of the curve to the graphics context.
	 * @param color the color filter
	 * @param from the start index to draw from
	 * @param to end the index to draw to (exclusive)
	 */
	public void draw(Color color, int from, int to) {
		if (curve == null)
			return;
		if (legacyRenderState == null)
			legacyRenderState = new LegacyCurveRenderState(hitObject, curve);
		legacyRenderState.draw(color, borderColor, from, to);
	}

	/**
	 * Splices a section of the curve for drawing, based on the given curve point indices.
	 * @param from the start index to splice
	 * @param to the end index to splice
	 */
	public void splice(int from, int to) {
		if (legacyRenderState == null)
			return;
		legacyRenderState.splice(from, to);
	}

	/**
	 * Returns the angle of the first control point.
	 */
	public abstract float getEndAngle();

	/**
	 * Returns the angle of the last control point.
	 */
	public abstract float getStartAngle();

	/**
	 * Returns the scaled x coordinate of the control point at index i.
	 * @param i the control point index
	 */
	public float getX(int i) { return (i == 0) ? x : sliderX[i - 1]; }

	/**
	 * Returns the scaled y coordinate of the control point at index i.
	 * @param i the control point index
	 */
	public float getY(int i) { return (i == 0) ? y : sliderY[i - 1]; }

	/**
	 * Discards the slider cache (only used for mmsliders).
	 */
	public void discardGeometry() {
		if (renderState != null)
			renderState.discardGeometry();
		if (legacyRenderState != null)
			legacyRenderState.discardGeometry();
	}
}
