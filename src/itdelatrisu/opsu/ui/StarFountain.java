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

import fluddokt.opsu.fake.Image;
import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.ui.animations.AnimatedValue;
import itdelatrisu.opsu.ui.animations.AnimationEquation;

/*
import org.newdawn.slick.Image;
*/

/**
 * Star fountain consisting of two star streams.
 */
public class StarFountain {
	/** The (approximate) number of stars in each burst. */
	private static final int BURST_SIZE = 125;

	/** Star streams. */
	private final StarStream left, right;

	/** Burst progress. */
	private final AnimatedValue burstProgress = new AnimatedValue(1000, 0, 1, AnimationEquation.LINEAR);

	/** The maximum direction offsets. */
	private final float xDirection, yDirection;

	/** Motion types. */
	private enum Motion {
		NONE {
			@Override
			public void init(StarFountain fountain) {
				fountain.left.setDirection(0, fountain.yDirection);
				fountain.right.setDirection(0, fountain.yDirection);
				fountain.left.setDirectionSpread(20f);
				fountain.right.setDirectionSpread(20f);
				fountain.left.setDurationSpread(1000, 200);
				fountain.right.setDurationSpread(1000, 200);
			}
		},
		OUTWARD_SWEEP {
			@Override
			public void init(StarFountain fountain) {
				fountain.left.setDirectionSpread(0f);
				fountain.right.setDirectionSpread(0f);
				fountain.left.setDurationSpread(850, 0);
				fountain.right.setDurationSpread(850, 0);
			}

			@Override
			public void update(StarFountain fountain) {
				float t = fountain.burstProgress.getValue();
				fountain.left.setDirection(fountain.xDirection - fountain.xDirection * 2f * t, fountain.yDirection);
				fountain.right.setDirection(-fountain.xDirection + fountain.xDirection * 2f * t, fountain.yDirection);
			}
		},
		INWARD_SWEEP {
			@Override
			public void init(StarFountain fountain) { OUTWARD_SWEEP.init(fountain); }

			@Override
			public void update(StarFountain fountain) {
				float t = fountain.burstProgress.getValue();
				fountain.left.setDirection(-fountain.xDirection + fountain.xDirection * 2f * t, fountain.yDirection);
				fountain.right.setDirection(fountain.xDirection - fountain.xDirection * 2f * t, fountain.yDirection);
			}
		};

		/** Initializes the streams in the fountain. */
		public void init(StarFountain fountain) {}

		/** Updates the streams in the fountain. */
		public void update(StarFountain fountain) {}
	}

	/** The current motion. */
	private Motion motion = Motion.NONE;

	/**
	 * Initializes the star fountain.
	 * @param containerWidth the container width
	 * @param containerHeight the container height
	 */
	public StarFountain(int containerWidth, int containerHeight) {
		Image img = GameImage.STAR2.getImage();
		float xOffset = containerWidth * 0.125f;
		this.xDirection = containerWidth / 2f - xOffset;
		this.yDirection = -containerHeight * 0.85f;
		this.left = new StarStream(xOffset - img.getWidth() / 2f, containerHeight, 0, yDirection, 0);
		this.right = new StarStream(containerWidth - xOffset - img.getWidth() / 2f, containerHeight, 0, yDirection, 0);
		left.setScaleSpread(1.1f, 0.2f);
		right.setScaleSpread(1.1f, 0.2f);
	}

	/**
	 * Draws the star fountain.
	 */
	public void draw() {
		left.draw();
		right.draw();
	}

	/**
	 * Updates the stars in the fountain by a delta interval.
	 * @param delta the delta interval since the last call
	 */
	public void update(int delta) {
		left.update(delta);
		right.update(delta);

		if (burstProgress.update(delta)) {
			motion.update(this);
			int size = Math.round((float) delta / burstProgress.getDuration() * BURST_SIZE);
			left.burst(size);
			right.burst(size);
		}
	}

	/**
	 * Creates a burst of stars to be processed during the next {@link #update(int)} call.
	 * @param wait if {@code true}, will not burst if a previous burst is in progress
	 */
	public void burst(boolean wait) {
		if (wait && (!burstProgress.isFinished() || !left.isEmpty() || !right.isEmpty()))
			return;  // previous burst in progress

		burstProgress.setTime(0);

		Motion lastMotion = motion;
		motion = Motion.values()[(int) (Math.random() * Motion.values().length)];
		if (motion == lastMotion)  // don't do the same sweep twice
			motion = Motion.NONE;
		motion.init(this);
	}

	/**
	 * Clears the stars currently in the fountain.
	 */
	public void clear() {
		left.clear();
		right.clear();
		burstProgress.setTime(burstProgress.getDuration());
		motion = Motion.NONE;
		motion.init(this);
	}
}
