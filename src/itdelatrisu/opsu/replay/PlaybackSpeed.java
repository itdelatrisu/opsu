package itdelatrisu.opsu.replay;

import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.GameMod;
import itdelatrisu.opsu.MenuButton;
import org.newdawn.slick.Image;

public enum PlaybackSpeed {
	NORMAL(GameImage.REPLAY_1XPLAYBACK, 1f),
	DOUBLE(GameImage.REPLAY_2XPLAYBACK, 2f),
	HALF(GameImage.REPLAY_05XPLAYBACK, 0.5f);

	/** The file name of the button image. */
	private GameImage gameImage;

	/** The button of the playback. */
	private MenuButton button;

	/** The speed modifier of the playback. */
	private float modifier;

	PlaybackSpeed(GameImage gameImage, float modifier) {
		this.gameImage = gameImage;
		this.modifier = modifier;
	}

	public static void init(int width, int height) {
		// create buttons
		for (PlaybackSpeed playback : PlaybackSpeed.values()) {
			Image img = playback.gameImage.getImage();
			playback.button = new MenuButton(img, width * 0.98f - (img.getWidth() / 2f), height * 0.25f);
			playback.button.setHoverFade();
		}
	}

	private static int index = 1;

	public static PlaybackSpeed next() {
		PlaybackSpeed next = values()[index++ % values().length];
		if((GameMod.DOUBLE_TIME.isActive() && next == PlaybackSpeed.DOUBLE))
			next = next();

		return next;
	}

	public static void reset() {
		index = 1;
	}

	/**
	 * Returns the button.
	 * @return the associated button
	 */
	public MenuButton getButton() { return button; }

	/**
	 * Returns the speed modifier.
	 * @return the speed
	 */
	public float getModifier() { return modifier; }
}

