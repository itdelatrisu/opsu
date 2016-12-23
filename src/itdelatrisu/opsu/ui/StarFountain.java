package itdelatrisu.opsu.ui;

import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.ui.animations.AnimatedValue;
import itdelatrisu.opsu.ui.animations.AnimationEquation;

import org.newdawn.slick.Image;

/**
 * Star fountain consisting of two star streams.
 */
public class StarFountain {
	/** The (approximate) number of stars in each burst. */
	private static final int BURST_SIZE = 80;

	/** Star streams. */
	private final StarStream left, right;

	/** Burst progress. */
	private final AnimatedValue burstProgress = new AnimatedValue(800, 0, 1, AnimationEquation.LINEAR);

	/**
	 * Initializes the star fountain.
	 * @param containerWidth the container width
	 * @param containerHeight the container height
	 */
	public StarFountain(int containerWidth, int containerHeight) {
		Image img = GameImage.STAR2.getImage();
		float xDir = containerWidth * 0.4f, yDir = containerHeight * 0.75f;
		this.left = new StarStream(-img.getWidth(), containerHeight, xDir, -yDir, 0);
		this.right = new StarStream(containerWidth, containerHeight, -xDir, -yDir, 0);
		setStreamProperties(left);
		setStreamProperties(right);
	}

	/**
	 * Sets attributes for the given star stream.
	 */
	private void setStreamProperties(StarStream stream) {
		stream.setDirectionSpread(60f);
		stream.setDurationSpread(1100, 200);
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
		if (wait && (burstProgress.getTime() < burstProgress.getDuration() || !left.isEmpty() || !right.isEmpty()))
			return;

		burstProgress.setTime(0);
	}

	/**
	 * Clears the stars currently in the fountain.
	 */
	public void clear() {
		left.clear();
		right.clear();
		burstProgress.setTime(burstProgress.getDuration());
	}
}
