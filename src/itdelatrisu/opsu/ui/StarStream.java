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

import fluddokt.opsu.fake.*;

import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.objects.curves.Vec2f;
import itdelatrisu.opsu.ui.animations.AnimatedValue;
import itdelatrisu.opsu.ui.animations.AnimationEquation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/*
import org.newdawn.slick.Image;
*/

/**
 * Star stream.
 */
public class StarStream {
	/** The origin of the star stream. */
	private final Vec2f position;

	/** The direction of the star stream. */
	private final Vec2f direction;

	/** The maximum number of stars to draw at once. */
	private final int maxStars;

	/** The spread of the stars' starting position. */
	private float positionSpread = 0f;

	/** The spread of the stars' direction. */
	private float directionSpread = 0f;

	/** The base (mean) duration for which stars are shown, in ms. */
	private int durationBase = 1300;

	/** The spread of the stars' duration, in ms. */
	private int durationSpread = 300;

	/** The base (mean) scale at which stars are drawn. */
	private float scaleBase = 1f;

	/** The spread of the stars' scale.*/
	private float scaleSpread = 0f;

	/** The star image. */
	private final Image starImg;

	/** The current list of stars. */
	private final List<Star> stars;

	/** Random number generator instance. */
	private final Random random;

	/** Contains data for a single star. */
	private class Star {
		/** The star position offset. */
		private final Vec2f offset;

		/** The star direction vector. */
		private final Vec2f dir;

		/** The star image rotation angle. */
		private final int angle;

		/** The star image scale. */
		private final float scale;

		/** The star animation progress. */
		private final AnimatedValue animatedValue;

		/**
		 * Creates a star with the given properties.
		 * @param offset the position offset vector
		 * @param direction the direction vector
		 * @param angle the image rotation angle
		 * @param scale the image scale
		 * @param duration the time, in milliseconds, to show the star
		 * @param eqn the animation equation to use
		 */
		public Star(Vec2f offset, Vec2f direction, int angle, float scale, int duration, AnimationEquation eqn) {
			this.offset = offset;
			this.dir = direction;
			this.angle = angle;
			this.scale = scale;
			this.animatedValue = new AnimatedValue(duration, 0f, 1f, eqn);
		}

		/**
		 * Draws the star.
		 */
		public void draw() {
			float t = animatedValue.getValue();
			starImg.setImageColor(1f, 1f, 1f, Math.min((1 - t) * 5f, 1f));
			starImg.drawEmbedded(
				offset.x + t * dir.x, offset.y + t * dir.y,
				starImg.getWidth() * scale, starImg.getHeight() * scale, angle
			);
		}

		/**
		 * Updates the animation by a delta interval.
		 * @param delta the delta interval since the last call
		 * @return true if an update was applied, false if the animation was not updated
		 */
		public boolean update(int delta) { return animatedValue.update(delta); }
	}

	/**
	 * Initializes the star stream.
	 * @param x the x position
	 * @param y the y position
	 * @param dirX the x-axis direction
	 * @param dirY the y-axis direction
	 * @param k the maximum number of stars to draw at once (excluding bursts)
	 */
	public StarStream(float x, float y, float dirX, float dirY, int k) {
		this.position = new Vec2f(x, y);
		this.direction = new Vec2f(dirX, dirY);
		this.maxStars = k;
		this.starImg = GameImage.STAR2.getImage().copy();
		this.stars = new ArrayList<Star>(k);
		this.random = new Random();
	}

	/**
	 * Set the direction spread of this star stream.
	 * @param spread the spread of the stars' starting position
	 */
	public void setPositionSpread(float spread) { this.positionSpread = spread; }

	/**
	 * Sets the direction of this star stream.
	 * @param dirX the new x-axis direction
	 * @param dirY the new y-axis direction
	 */
	public void setDirection(float dirX, float dirY) { direction.set(dirX, dirY); }

	/**
	 * Set the direction spread of this star stream.
	 * @param spread the spread of the stars' direction
	 */
	public void setDirectionSpread(float spread) { this.directionSpread = spread; }

	/**
	 * Sets the duration base and spread of this star stream.
	 * @param base the base (mean) duration for which stars are shown, in ms
	 * @param spread the spread of the stars' duration, in ms
	 */
	public void setDurationSpread(int base, int spread) {
		this.durationBase = base;
		this.durationSpread = spread;
	}

	/**
	 * Sets the scale base and spread of this star stream.
	 * @param base the base (mean) scale at which stars are drawn
	 * @param spread the spread of the stars' scale
	 */
	public void setScaleSpread(float base, float spread) {
		this.scaleBase = base;
		this.scaleSpread = spread;
	}

	/**
	 * Draws the star stream.
	 */
	public void draw() {
		if (stars.isEmpty())
			return;

		starImg.startUse();
		for (Star star : stars)
			star.draw();
		starImg.endUse();
	}

	/**
	 * Updates the stars in the stream by a delta interval.
	 * @param delta the delta interval since the last call
	 */
	public void update(int delta) {
		// update current stars
		Iterator<Star> iter = stars.iterator();
		while (iter.hasNext()) {
			Star star = iter.next();
			if (!star.update(delta))
				iter.remove();
		}

		// create new stars
		for (int i = stars.size(); i < maxStars; i++) {
			if (Math.random() < ((i < maxStars / 4) ? 0.25 : 0.66))
				break;  // stagger spawning new stars

			stars.add(createStar());
		}
	}

	/**
	 * Creates a new star with randomized properties.
	 */
	private Star createStar() {
		float distanceRatio = Utils.clamp((float) getGaussian(0.65, 0.25), 0.2f, 0.925f);
		Vec2f offset = position.cpy().add(direction.cpy().nor().normalize().scale((float) getGaussian(0, positionSpread)));
		Vec2f dir = direction.cpy().scale(distanceRatio).add((float) getGaussian(0, directionSpread), (float) getGaussian(0, directionSpread));
		int angle = (int) getGaussian(0, 22.5);
		float scale = (float) getGaussian(scaleBase, scaleSpread);
		int duration = Math.max(0, (int) (distanceRatio * getGaussian(durationBase, durationSpread)));
		AnimationEquation eqn = random.nextBoolean() ? AnimationEquation.IN_OUT_QUAD : AnimationEquation.OUT_QUAD;

		return new Star(offset, dir, angle, scale, duration, eqn);
	}

	/**
	 * Creates a burst of stars instantly.
	 * @param count the number of stars to create
	 */
	public void burst(int count) {
		for (int i = 0; i < count; i++)
			stars.add(createStar());
	}

	/**
	 * Clears the stars currently in the stream.
	 */
	public void clear() { stars.clear(); }

	/**
	 * Returns whether there are any stars currently in this stream.
	 */
	public boolean isEmpty() { return stars.isEmpty(); }

	/**
	 * Returns the next pseudorandom, Gaussian ("normally") distributed {@code double} value
	 * with the given mean and standard deviation.
	 * @param mean the mean
	 * @param stdDev the standard deviation
	 */
	private double getGaussian(double mean, double stdDev) {
		return mean + random.nextGaussian() * stdDev;
	}
}
