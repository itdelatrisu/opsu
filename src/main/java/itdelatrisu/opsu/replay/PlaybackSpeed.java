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

package itdelatrisu.opsu.replay;

import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.GameMod;
import itdelatrisu.opsu.ui.MenuButton;

import org.newdawn.slick.Image;

/**
 * Playback speeds for replays.
 *
 * @author DarkTigrus (https://github.com/DarkTigrus)
 */
public enum PlaybackSpeed {
	NORMAL (GameImage.REPLAY_PLAYBACK_NORMAL, 1f),
	DOUBLE (GameImage.REPLAY_PLAYBACK_DOUBLE, 2f),
	HALF (GameImage.REPLAY_PLAYBACK_HALF, 0.5f);

	/** The button image. */
	private final GameImage gameImage;

	/** The playback speed modifier. */
	private final float modifier;

	/** The button. */
	private MenuButton button;

	/** Enum values. */
	private static PlaybackSpeed[] values = PlaybackSpeed.values();

	/**
	 * Initializes the playback buttons.
	 * @param width the container width
	 * @param height the container height
	 */
	public static void init(int width, int height) {
		for (PlaybackSpeed playback : PlaybackSpeed.values()) {
			Image img = playback.gameImage.getImage();
			playback.button = new MenuButton(img, width * 0.98f - (img.getWidth() / 2f), height * 0.25f);
			playback.button.setHoverFade();
		}
	}

	/**
	 * Constructor.
	 * @param gameImage the button image
	 * @param modifier the speed modifier
	 */
	PlaybackSpeed(GameImage gameImage, float modifier) {
		this.gameImage = gameImage;
		this.modifier = modifier;
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

	/**
	 * Returns the next playback speed.
	 */
	public PlaybackSpeed next() {
		PlaybackSpeed next = values[(this.ordinal() + 1) % values.length];
		if ((GameMod.DOUBLE_TIME.isActive() && next == PlaybackSpeed.DOUBLE))
			next = next.next();
		return next;
	}
}
