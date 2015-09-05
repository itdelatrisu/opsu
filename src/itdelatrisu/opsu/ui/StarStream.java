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

package itdelatrisu.opsu.ui;

import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.ui.animations.AnimatedValue;
import itdelatrisu.opsu.ui.animations.AnimationEquation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.newdawn.slick.Image;

/**
 * Horizontal star stream.
 */
public class StarStream {
	/** The container dimensions. */
	private final int containerWidth, containerHeight;

	/** The star image. */
	private final Image starImg;

	/** The current list of stars. */
	private final List<Star> stars;

	/** The maximum number of stars to draw at once. */
	private static final int MAX_STARS = 20;

	/** Random number generator instance. */
	private final Random random;

	/** Contains data for a single star. */
	private class Star {
		/** The star animation progress. */
		private final AnimatedValue animatedValue;

		/** The star properties. */
		private final int distance, yOffset, angle;

		/**
		 * Creates a star with the given properties.
		 * @param duration the time, in milliseconds, to show the star
		 * @param distance the distance for the star to travel in {@code duration}
		 * @param yOffset the vertical offset from the center of the container
		 * @param angle the rotation angle
		 * @param eqn the animation equation to use
		 */
		public Star(int duration, int distance, int yOffset, int angle, AnimationEquation eqn) {
			this.animatedValue = new AnimatedValue(duration, 0f, 1f, eqn);
			this.distance = distance;
			this.yOffset = yOffset;
			this.angle = angle;
		}

		/**
		 * Draws the star.
		 */
		public void draw() {
			float t = animatedValue.getValue();
			starImg.setAlpha(Math.min((1 - t) * 5f, 1f));
			starImg.setRotation(angle);
			starImg.draw(containerWidth - (distance * t), ((containerHeight - starImg.getHeight()) / 2) + yOffset);
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
	 * @param width the container width
	 * @param height the container height
	 */
	public StarStream(int width, int height) {
		this.containerWidth = width;
		this.containerHeight = height;
		this.starImg = GameImage.STAR2.getImage().copy();
		this.stars = new ArrayList<Star>();
		this.random = new Random();
	}

	/**
	 * Draws the star stream.
	 */
	public void draw() {
		for (Star star : stars)
			star.draw();
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
		for (int i = stars.size(); i < MAX_STARS; i++) {
			if (Math.random() < ((i < 5) ? 0.25 : 0.66))
				break;

			// generate star properties
			float distanceRatio = Utils.clamp((float) getGaussian(0.65, 0.25), 0.2f, 0.925f);
			int distance = (int) (containerWidth * distanceRatio);
			int duration = (int) (distanceRatio * getGaussian(1300, 300));
			int yOffset = (int) getGaussian(0, containerHeight / 20);
			int angle = (int) getGaussian(0, 22.5);
			AnimationEquation eqn = random.nextBoolean() ? AnimationEquation.IN_OUT_QUAD : AnimationEquation.OUT_QUAD;

			stars.add(new Star(duration, distance, angle, yOffset, eqn));
		}
	}

	/**
	 * Clears the stars currently in the stream.
	 */
	public void clear() { stars.clear(); }

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
