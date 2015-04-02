package itdelatrisu.opsu.replay;

import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.GameMod;
import org.newdawn.slick.Image;

public enum PlaybackSpeed {
	NORMAL(GameImage.REPLAY_1XPLAYBACK, 1f),
	DOUBLE(GameImage.REPLAY_2XPLAYBACK, 2f),
	HALF(GameImage.REPLAY_05XPLAYBACK, 0.5f);

	/** The file name of the button image. */
	private GameImage gameImage;

	/** The speed modifier of the playback. */
	private float modifier;

	PlaybackSpeed(GameImage gameImage, float modifier) {
		this.gameImage = gameImage;
		this.modifier = modifier;
	}

	private static int index = 1;

	public static PlaybackSpeed next() {
		PlaybackSpeed next = values()[index++ % values().length];
		if((GameMod.DOUBLE_TIME.isActive() && next == PlaybackSpeed.DOUBLE) ||
				(GameMod.HALF_TIME.isActive() && next == PlaybackSpeed.HALF))
			next = next();

		return next;
	}

	public static void reset() {
		index = 1;
	}

	/**
	 * Returns the image.
	 * @return the associated image
	 */
	public Image getImage() { return gameImage.getImage(); }

	/**
	 * Returns the speed modifier.
	 * @return the speed
	 */
	public float getModifier() { return modifier; }
}

