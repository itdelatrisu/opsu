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

import itdelatrisu.opsu.options.Options;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;

/**
 * Input overlay key.
 *
 * @author yugecin (https://github.com/yugecin)
 */
public class InputOverlayKey {
	/** Time, in ms, of the shrink/expand key animation. */
	private static final int ANIMATION_TIME = 100;
	/** The final scale of the input keys when the key is pressed. */
	private static final float ACTIVE_SCALE = 0.75f;

	/** The bits in the keystate that corresponds to this key. */
	private final int targetKey;
	/** The bits that may not be set for this key to be down. */
	private final int ignoredKey;
	/** The color of the button when the key is down. */
	private final Color activeColor;
	/** The initial text of the button. */
	private final String initialText;

	/** How long the key has been down, used for the scale animation. */
	private int downtime;
	/** Whether or not this key is currently down */
	private boolean down;
	/** The text that will be displayed on this button.*/
	private String text;
	/** The amount of times this key has been pressed. */
	private int presses;

	/**
	 * Constructor.
	 * @param initialText the initial text of the button
	 * @param targetKey the bits in the keystate that corresponds to this key
	 * @param ignoredKey the bits that may not be set for this key to be down
	 * @param activeColor the color of the button when the key is down
	 */
	public InputOverlayKey(String initialText, int targetKey, int ignoredKey, Color activeColor) {
		this.initialText = initialText;
		this.targetKey = targetKey;
		this.ignoredKey = ignoredKey;
		this.activeColor = activeColor;
	}

	/** Resets all data. */
	public void reset() {
		down = false;
		downtime = 0;
		presses = 0;
		text = initialText;
	}

	/**
	 * Updates this key by a delta interval.
	 * @param keystates the current key states
	 * @param countkeys whether to increment the key count
	 * @param delta the delta interval since the last call
	 */
	public void update(int keystates, boolean countkeys, int delta) {
		boolean wasdown = down;
		down = (keystates & targetKey) == targetKey && (keystates & ignoredKey) == 0;
		if (!wasdown && down) {
			if (countkeys)
				presses++;
			text = Integer.toString(presses);
		}
		if (down && downtime < ANIMATION_TIME)
			downtime = Math.min(ANIMATION_TIME, downtime + delta);
		else if (!down && downtime > 0)
			downtime = Math.max(0, downtime - delta);
	}

	/**
	 * Renders this key.
	 * @param g the graphics context
	 * @param x the x position
	 * @param y the y position
	 * @param baseImage the key image
	 */
	public void render(Graphics g, int x, int y, Image baseImage) {
		g.pushTransform();
		float scale = 1f;
		if (downtime > 0) {
			float progress = downtime / (float) ANIMATION_TIME;
			scale -= (1f - ACTIVE_SCALE) * progress;
			g.scale(scale, scale);
			x /= scale;
			y /= scale;
		}
		baseImage.drawCentered(x, y, down ? activeColor : Color.white);
		x -= Fonts.MEDIUMBOLD.getWidth(text) / 2;
		y -= Fonts.MEDIUMBOLD.getLineHeight() / 2;
		/*
		// shadow (TODO)
		g.pushTransform();
		g.scale(1.1f, 1.1f);
		float shadowx = x / 1.1f - Fonts.MEDIUMBOLD.getWidth(text) * 0.05f;
		float shadowy = y / 1.1f - Fonts.MEDIUMBOLD.getLineHeight() * 0.05f;
		Fonts.MEDIUMBOLD.drawString(shadowx, shadowy, text, Color.black);
		g.popTransform();
		*/
		Fonts.MEDIUMBOLD.drawString(x, y, text, Options.getSkin().getInputOverlayText());
		g.popTransform();
	}
}
