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

package itdelatrisu.opsu;

import java.util.Arrays;
import java.util.Collections;

import org.newdawn.slick.Image;
import org.newdawn.slick.Input;

/**
 * Game mods.
 */
public enum GameMod {
	EASY          (0, GameImage.MOD_EASY, Input.KEY_Q, 0.5f),
	NO_FAIL       (1, GameImage.MOD_NO_FAIL, Input.KEY_W, 0.5f),
	HARD_ROCK     (2, GameImage.MOD_HARD_ROCK, Input.KEY_A, 1.06f),
	SUDDEN_DEATH  (3, GameImage.MOD_SUDDEN_DEATH, Input.KEY_S),
	SPUN_OUT      (4, GameImage.MOD_SPUN_OUT, Input.KEY_V, 0.9f),
	AUTO          (5, GameImage.MOD_AUTO, Input.KEY_B);
//	HALF_TIME     (, GameImage.MOD_HALF_TIME, Input.KEY_E, 0.3f),
//	DOUBLE_TIME   (, GameImage.MOD_DOUBLE_TIME, Input.KEY_D, 1.12f),
//	HIDDEN        (, GameImage.MOD_HIDDEN, Input.KEY_F, 1.06f),
//	FLASHLIGHT    (, GameImage.MOD_FLASHLIGHT, Input.KEY_G, 1.12f);

	/**
	 * The ID of the mod (used for positioning).
	 */
	private int id;

	/**
	 * The file name of the mod image.
	 */
	private GameImage image;

	/**
	 * The shortcut key associated with the mod.
	 */
	private int key;

	/**
	 * Score multiplier.
	 */
	private float multiplier;

	/**
	 * Whether or not this mod is active.
	 */
	private boolean active = false;

	/**
	 * The button containing the mod image (displayed in OptionsMenu screen).
	 */
	private MenuButton button;

	/**
	 * Total number of mods.
	 */
	private static final int SIZE = GameMod.values().length;

	/**
	 * Returns the total number of game mods.
	 * @return the number of mods
	 */
	public static int size() { return SIZE; }

	/**
	 * Returns an array of GameMod objects in reverse order.
	 * @return all game mods in reverse order
	 */
	public static GameMod[] valuesReversed() {
		GameMod[] mods = GameMod.values();
		Collections.reverse(Arrays.asList(mods));
		return mods;
	}

	/**
	 * Constructor.
	 * @param id the ID of the mod (for positioning).
	 * @param image the GameImage
	 * @param key the shortcut key
	 */
	GameMod(int id, GameImage image, int key) {
		this.id = id;
		this.image = image;
		this.key = key;
		this.multiplier = 1f;
	}

	/**
	 * Constructor.
	 * @param id the ID of the mod (for positioning).
	 * @param image the GameImage
	 * @param key the shortcut key
	 * @param multiplier the score multiplier
	 */
	GameMod(int id, GameImage image, int key, float multiplier) {
		this.id = id;
		this.image = image;
		this.key = key;
		this.multiplier = multiplier;
	}

	/**
	 * Initializes the game mod.
	 * @param width the container width
	 * @param height the container height
	 */
	public void init(int width, int height) {
		Image img = image.getImage();

		// find coordinates
		float offsetX = img.getWidth() * 1.5f;
		float x = (width / 2f) - (offsetX * SIZE / 2.75f);
		float y = (height * 0.8f) + (img.getHeight() / 2);

		// create button
		img.setAlpha(0.5f);
		this.button = new MenuButton(img, x + (offsetX * id), y);
		this.button.setHoverScale(1.15f);
	}

	/**
	 * Returns the shortcut key for the mod.
	 * @return the key
	 * @see org.newdawn.slick.Input
	 */
	public int getKey() { return key; }

	/**
	 * Returns the score multiplier for the mod.
	 * @return the multiplier
	 */
	public float getMultiplier() { return multiplier; }

	/**
	 * Toggles the active status of the mod.
	 * @param checkInverse if true, perform checks for mutual exclusivity
	 */
	public void toggle(boolean checkInverse) {
		button.getImage().setAlpha(active ? 0.5f : 1.0f);
		active = !active;

		if (checkInverse) {
			if (AUTO.isActive()) {
				if (this == AUTO) {
					if (SPUN_OUT.isActive())
						SPUN_OUT.toggle(false);
					if (SUDDEN_DEATH.isActive())
						SUDDEN_DEATH.toggle(false);
				} else if (this == SPUN_OUT || this == SUDDEN_DEATH) {
					if (active)
						toggle(false);
				}
			}
			if (SUDDEN_DEATH.isActive() && NO_FAIL.isActive()) {
				if (this == SUDDEN_DEATH)
					NO_FAIL.toggle(false);
				else
					SUDDEN_DEATH.toggle(false);
			}
			if (EASY.isActive() && HARD_ROCK.isActive()) {
				if (this == EASY)
					HARD_ROCK.toggle(false);
				else
					EASY.toggle(false);
			}
		}
	}

	/**
	 * Returns whether or not the mod is active.
	 * @return true if active
	 */
	public boolean isActive() { return active; }

	/**
	 * Returns the image associated with the mod.
	 * @return the associated image
	 */
	public Image getImage() { return image.getImage(); }

	/**
	 * Returns the mod ID.
	 * @return the mod ID
	 */
	public int getID() { return id; }

	/**
	 * Draws the game mod.
	 */
	public void draw() { button.draw(); }

	/**
	 * Checks if the coordinates are within the image bounds.
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @return true if within bounds
	 */
	public boolean contains(float x, float y) { return button.contains(x, y); }

	/**
	 * Sets the current button scale (for hovering).
	 * @param scale the new scale (default 1.0f)
	 */
	public void setScale(float scale) { button.setScale(scale); }

	/**
	 * Updates the scale of the button depending on whether or not the cursor
	 * is hovering over the button.
	 * @param delta the delta interval
	 * @param x the x coordinate
	 * @param y the y coordinate
	 */
	public void hoverUpdate(int delta, float x, float y) { button.hoverUpdate(delta, x, y); }
}
