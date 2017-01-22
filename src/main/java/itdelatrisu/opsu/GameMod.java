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

package itdelatrisu.opsu;

import itdelatrisu.opsu.ui.Fonts;
import itdelatrisu.opsu.ui.MenuButton;
import itdelatrisu.opsu.ui.animations.AnimationEquation;

import java.util.Arrays;
import java.util.Collections;

import org.newdawn.slick.Color;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;

/**
 * Game mods.
 */
public enum GameMod {
	EASY          (Category.EASY, 0, GameImage.MOD_EASY, "EZ", 2, Input.KEY_Q, 0.5f,
	              "Easy", "Reduces overall difficulty - larger circles, more forgiving HP drain, less accuracy required."),
	NO_FAIL       (Category.EASY, 1, GameImage.MOD_NO_FAIL, "NF", 1, Input.KEY_W, 0.5f,
	              "NoFail", "You can't fail.  No matter what."),
	HALF_TIME     (Category.EASY, 2, GameImage.MOD_HALF_TIME, "HT", 256, Input.KEY_E, 0.3f,
	              "HalfTime", "Less zoom."),
	HARD_ROCK     (Category.HARD, 0, GameImage.MOD_HARD_ROCK, "HR", 16, Input.KEY_A, 1.06f,
	              "HardRock", "Everything just got a bit harder..."),
	SUDDEN_DEATH  (Category.HARD, 1, GameImage.MOD_SUDDEN_DEATH, "SD", 32, Input.KEY_S, 1f,
	              "SuddenDeath", "Miss a note and fail."),
//	PERFECT       (Category.HARD, 1, GameImage.MOD_PERFECT, "PF", 64, Input.KEY_S, 1f,
//	              "Perfect", "SS or quit."),
	DOUBLE_TIME   (Category.HARD, 2, GameImage.MOD_DOUBLE_TIME, "DT", 64, Input.KEY_D, 1.12f,
	              "DoubleTime", "Zoooooooooom."),
//	NIGHTCORE     (Category.HARD, 2, GameImage.MOD_NIGHTCORE, "NT", 64, Input.KEY_D, 1.12f,
//	              "Nightcore", "uguuuuuuuu"),
	HIDDEN        (Category.HARD, 3, GameImage.MOD_HIDDEN, "HD", 8, Input.KEY_F, 1.06f,
	              "Hidden", "Play with no approach circles and fading notes for a slight score advantage."),
	FLASHLIGHT    (Category.HARD, 4, GameImage.MOD_FLASHLIGHT, "FL", 1024, Input.KEY_G, 1.12f,
	              "Flashlight", "Restricted view area."),
	RELAX         (Category.SPECIAL, 0, GameImage.MOD_RELAX, "RL", 128, Input.KEY_Z, 0f,
	              "Relax", "You don't need to click.\nGive your clicking/tapping finger a break from the heat of things.\n**UNRANKED**"),
	AUTOPILOT     (Category.SPECIAL, 1, GameImage.MOD_AUTOPILOT, "AP", 8192, Input.KEY_X, 0f,
	              "Relax2", "Automatic cursor movement - just follow the rhythm.\n**UNRANKED**"),
	SPUN_OUT      (Category.SPECIAL, 2, GameImage.MOD_SPUN_OUT, "SO", 4096, Input.KEY_C, 0.9f,
	              "SpunOut", "Spinners will be automatically completed."),
	AUTO          (Category.SPECIAL, 3, GameImage.MOD_AUTO, "", 2048, Input.KEY_V, 1f,
	              "Autoplay", "Watch a perfect automated play through the song.");

	/** Mod categories. */
	public enum Category {
		EASY    (0, "Difficulty Reduction", Color.green),
		HARD    (1, "Difficulty Increase", Color.red),
		SPECIAL (2, "Special", Color.white);

		/** Drawing index. */
		private final int index;

		/** Category name. */
		private final String name;

		/** Text color. */
		private final Color color;

		/** The coordinates of the category. */
		private float x, y;

		/**
		 * Constructor.
		 * @param index the drawing index
		 * @param name the category name
		 * @param color the text color
		 */
		Category(int index, String name, Color color) {
			this.index = index;
			this.name = name;
			this.color = color;
		}

		/**
		 * Initializes the category.
		 * @param width the container width
		 * @param height the container height
		 */
		public void init(int width, int height) {
			float multY = Fonts.LARGE.getLineHeight() * 2 + height * 0.06f;
			float offsetY = GameImage.MOD_EASY.getImage().getHeight() * 1.5f;
			this.x = width / 30f;
			this.y = multY + Fonts.LARGE.getLineHeight() * 3f + offsetY * index;
		}

		/**
		 * Returns the category name.
		 */
		public String getName() { return name; }

		/**
		 * Returns the text color.
		 */
		public Color getColor() { return color; }

		/**
		 * Returns the x coordinate of the category.
		 */
		public float getX() { return x; }

		/**
		 * Returns the y coordinate of the category.
		 */
		public float getY() { return y; }
	}

	/** The category for the mod. */
	private final Category category;

	/** The index in the category (for positioning). */
	private final int categoryIndex;

	/** The file name of the mod image. */
	private final GameImage image;

	/** The abbreviation for the mod. */
	private final String abbrev;

	/**
	 * Bit value associated with the mod.
	 * See the osu! API: https://github.com/peppy/osu-api/wiki#mods
	 */
	private final int bit;

	/** The shortcut key associated with the mod. */
	private final int key;

	/** The score multiplier. */
	private final float multiplier;

	/** The name of the mod. */
	private final String name;

	/** The description of the mod. */
	private final String description;

	/** Whether or not this mod is active. */
	private boolean active = false;

	/** The button containing the mod image (displayed in OptionsMenu screen). */
	private MenuButton button;

	/** Total number of mods. */
	public static final int SIZE = values().length;

	/** Array of GameMod objects in reverse order. */
	public static final GameMod[] VALUES_REVERSED;
	static {
		VALUES_REVERSED = values();
		Collections.reverse(Arrays.asList(VALUES_REVERSED));
	}

	/** The last calculated score multiplier, or -1f if it must be recalculated. */
	private static float scoreMultiplier = -1f;

	/** The last calculated track speed multiplier, or -1f if it must be recalculated. */
	private static float speedMultiplier = -1f;

	/** The last calculated difficulty multiplier, or -1f if it must be recalculated. */
	private static float difficultyMultiplier = -1f;

	/**
	 * Initializes the game mods.
	 * @param width the container width
	 * @param height the container height
	 */
	public static void init(int width, int height) {
		// initialize categories
		for (Category c : Category.values())
			c.init(width, height);

		// create buttons
		float baseX = Category.EASY.getX() + Fonts.LARGE.getWidth(Category.EASY.getName()) * 1.25f;
		float offsetX = GameImage.MOD_EASY.getImage().getWidth() * 2.1f;
		for (GameMod mod : GameMod.values()) {
			Image img = mod.image.getImage();
			mod.button = new MenuButton(img,
					baseX + (offsetX * mod.categoryIndex) + img.getWidth() / 2f,
					mod.category.getY());
			mod.button.setHoverAnimationDuration(300);
			mod.button.setHoverAnimationEquation(AnimationEquation.IN_OUT_BACK);
			mod.button.setHoverExpand(1.2f);
			mod.button.setHoverRotate(10f);

			// reset state
			mod.active = false;
		}

		scoreMultiplier = speedMultiplier = difficultyMultiplier = -1f;
	}

	/**
	 * Returns the current score multiplier from all active mods.
	 */
	public static float getScoreMultiplier() {
		if (scoreMultiplier < 0f) {
			float multiplier = 1f;
			for (GameMod mod : GameMod.values()) {
				if (mod.isActive())
					multiplier *= mod.getMultiplier();
			}
			scoreMultiplier = multiplier;
		}
		return scoreMultiplier;
	}

	/**
	 * Returns the current track speed multiplier from all active mods.
	 */
	public static float getSpeedMultiplier() {
		if (speedMultiplier < 0f) {
			if (DOUBLE_TIME.isActive())
				speedMultiplier = 1.5f;
			else if (HALF_TIME.isActive())
				speedMultiplier = 0.75f;
			else
				speedMultiplier = 1f;
		}
		return speedMultiplier;
	}

	/**
	 * Returns the current difficulty multiplier from all active mods.
	 */
	public static float getDifficultyMultiplier() {
		if (difficultyMultiplier < 0f) {
			if (HARD_ROCK.isActive())
				difficultyMultiplier = 1.4f;
			else if (EASY.isActive())
				difficultyMultiplier = 0.5f;
			else
				difficultyMultiplier = 1f;
		}
		return difficultyMultiplier;
	}

	/**
	 * Returns the current game mod state (bitwise OR of active mods).
	 */
	public static int getModState() {
		int state = 0;
		for (GameMod mod : GameMod.values()) {
			if (mod.isActive())
				state |= mod.getBit();
		}
		return state;
	}

	/**
	 * Sets the active states of all game mods to the given state.
	 * @param state the state (bitwise OR of active mods)
	 */
	public static void loadModState(int state) {
		scoreMultiplier = speedMultiplier = difficultyMultiplier = -1f;
		for (GameMod mod : GameMod.values())
			mod.active = ((state & mod.getBit()) > 0);
	}

	/**
	 * Returns a comma-separated string of mod names of active mods in the given state.
	 * @param state the state (bitwise OR of active mods)
	 */
	public static String getModString(int state) {
		StringBuilder sb = new StringBuilder();
		for (GameMod mod : GameMod.values()) {
			if ((state & mod.getBit()) > 0) {
				sb.append(mod.getName());
				sb.append(',');
			}
		}
		if (sb.length() > 0) {
			sb.setLength(sb.length() - 1);
			return sb.toString();
		} else
			return "None";
	}

	/**
	 * Constructor.
	 * @param category the category for the mod
	 * @param categoryIndex the index in the category
	 * @param image the GameImage
	 * @param abbrev the two-letter abbreviation
	 * @param bit the bit
	 * @param key the shortcut key
	 * @param multiplier the score multiplier
	 * @param name the name
	 * @param description the description
	 */
	GameMod(Category category, int categoryIndex, GameImage image, String abbrev,
			int bit, int key, float multiplier, String name, String description) {
		this.category = category;
		this.categoryIndex = categoryIndex;
		this.image = image;
		this.abbrev = abbrev;
		this.bit = bit;
		this.key = key;
		this.multiplier = multiplier;
		this.name = name;
		this.description = description;
	}

	/**
	 * Returns the abbreviated name of the mod.
	 * @return the two-letter abbreviation
	 */
	public String getAbbreviation() { return abbrev; }

	/**
	 * Returns the bit associated with the mod.
	 * @return the bit
	 */
	public int getBit() { return bit; }

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
	 * Returns the name of the mod.
	 * @return the name
	 */
	public String getName() { return name; }

	/**
	 * Returns a description of the mod.
	 * @return the description
	 */
	public String getDescription() { return description; }

	/**
	 * Toggles the active status of the mod.
	 * @param checkInverse if true, perform checks for mutual exclusivity
	 */
	public void toggle(boolean checkInverse) {
		active = !active;
		scoreMultiplier = speedMultiplier = difficultyMultiplier = -1f;

		if (checkInverse) {
			if (AUTO.isActive()) {
				if (this == AUTO) {
					SPUN_OUT.active = false;
					SUDDEN_DEATH.active = false;
					RELAX.active = false;
					AUTOPILOT.active = false;
				} else if (this == SPUN_OUT || this == SUDDEN_DEATH || this == RELAX || this == AUTOPILOT)
					this.active = false;
			}
			if (active && (this == SUDDEN_DEATH || this == NO_FAIL || this == RELAX || this == AUTOPILOT)) {
				SUDDEN_DEATH.active = false;
				NO_FAIL.active = false;
				RELAX.active = false;
				AUTOPILOT.active = false;
				active = true;
			}
			if (AUTOPILOT.isActive() && SPUN_OUT.isActive()) {
				if (this == AUTOPILOT)
					SPUN_OUT.active = false;
				else
					AUTOPILOT.active = false;
			}
			if (EASY.isActive() && HARD_ROCK.isActive()) {
				if (this == EASY)
					HARD_ROCK.active = false;
				else
					EASY.active = false;
			}
			if (HALF_TIME.isActive() && DOUBLE_TIME.isActive()) {
				if (this == HALF_TIME)
					DOUBLE_TIME.active = false;
				else
					HALF_TIME.active = false;
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
	 * Resets the hover fields for the button.
	 */
	public void resetHover() { button.resetHover(); }

	/**
	 * Updates the scale of the button depending on whether or not the cursor
	 * is hovering over the button.
	 * @param delta the delta interval
	 * @param x the x coordinate
	 * @param y the y coordinate
	 */
	public void hoverUpdate(int delta, float x, float y) { button.hoverUpdate(delta, x, y); }

	/**
	 * Updates the scale of the button depending on whether or not the cursor
	 * is hovering over the button.
	 * @param delta the delta interval
	 * @param isHover true if the cursor is currently hovering over the button
	 */
	public void hoverUpdate(int delta, boolean isHover) { button.hoverUpdate(delta, isHover); }
}
