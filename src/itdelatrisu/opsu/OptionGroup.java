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

import itdelatrisu.opsu.Options.GameOption;

/**
 * Option category and related options.
 */
public class OptionGroup {
	/** All option groups. */
	public static final OptionGroup[] ALL_OPTIONS = new OptionGroup[] {
		new OptionGroup("GENERAL", null),
		new OptionGroup("GENERAL", new GameOption[]{
			GameOption.DISABLE_UPDATER,
			GameOption.ENABLE_WATCH_SERVICE,
		}),
		new OptionGroup("LANGUAGE", new GameOption[] {
			GameOption.SHOW_UNICODE,
		}),
		new OptionGroup("GRAPHICS", null),
		new OptionGroup("RENDERER", new GameOption[]{
			GameOption.SCREEN_RESOLUTION,
			GameOption.FULLSCREEN,
			GameOption.TARGET_FPS,
			GameOption.SHOW_FPS,
			GameOption.SCREENSHOT_FORMAT,
		}),
		new OptionGroup("SLIDERS", new GameOption[] {
			GameOption.SNAKING_SLIDERS,
		}),
		new OptionGroup("MISC", new GameOption[] {
			GameOption.DYNAMIC_BACKGROUND,
			GameOption.PARALLAX
		}),
		new OptionGroup("SKIN", null),
		new OptionGroup("SKIN", new GameOption[]{
			GameOption.SKIN,
			GameOption.LOAD_HD_IMAGES,
			GameOption.LOAD_VERBOSE,
			GameOption.IGNORE_BEATMAP_SKINS,
		}),
		new OptionGroup("CURSOR", new GameOption[] {
			GameOption.CURSOR_SIZE,
			GameOption.DISABLE_CURSOR
		}),
		new OptionGroup("AUDIO", null),
		new OptionGroup("VOLUME", new GameOption[]{
			GameOption.MASTER_VOLUME,
			GameOption.MUSIC_VOLUME,
			GameOption.EFFECT_VOLUME,
			GameOption.HITSOUND_VOLUME,
		}),
		new OptionGroup("MISC", new GameOption[] {
			GameOption.MUSIC_OFFSET,
			GameOption.DISABLE_SOUNDS,
			GameOption.ENABLE_THEME_SONG
		}),
		new OptionGroup("GAMEPLAY", null),
		new OptionGroup("GENERAL", new GameOption[] {
			GameOption.BACKGROUND_DIM,
			GameOption.FORCE_DEFAULT_PLAYFIELD,
			GameOption.ENABLE_VIDEOS,
			GameOption.SHOW_HIT_LIGHTING,
			GameOption.SHOW_COMBO_BURSTS,
			GameOption.SHOW_PERFECT_HIT,
			GameOption.SHOW_FOLLOW_POINTS,
			GameOption.SHOW_HIT_ERROR_BAR
		}),
		new OptionGroup("INPUT", null),
		new OptionGroup("KEY MAPPING", new GameOption[] {
			GameOption.KEY_LEFT,
			GameOption.KEY_RIGHT,
		}),
		new OptionGroup("MOUSE", new GameOption[] {
			GameOption.DISABLE_MOUSE_WHEEL,
			GameOption.DISABLE_MOUSE_BUTTONS,
		}),
		new OptionGroup("CUSTOM", null),
		new OptionGroup("DIFFICULTY", new GameOption[] {
			GameOption.FIXED_CS,
			GameOption.FIXED_HP,
			GameOption.FIXED_AR,
			GameOption.FIXED_OD,
		}),
		new OptionGroup("MISC", new GameOption[] {
			GameOption.CHECKPOINT,
			GameOption.REPLAY_SEEKING,
		}),
	};

	/** The category name. */
	private final String category;

	/** The game options. */
	private final GameOption[] options;

	/**
	 * If this group should not be shown in the optionsmenu because its suboptions
	 * do not match the search string.
	 */
	private boolean filtered;

	/**
	 * Creates an option group with the given category name and options.
	 * @param category the category name
	 * @param options the related options
	 */
	public OptionGroup(String category, GameOption[] options) {
		this.category = category;
		this.options = options;
	}

	/** Returns the category name. */
	public String getName() { return category; }

	/** Returns the related options. */
	public GameOption[] getOptions() { return options; }

	/** Returns the option at the given index. */
	public GameOption getOption(int i) { return options[i]; }

	/**
	 * Set the filtered flag to show or hide this group.
	 * @param filtered wether to filter this group or not
	 */
	public void setFiltered(boolean filtered) { this.filtered = filtered; }

	/** Returns wether or not this group should be filtered. */
	public boolean isFiltered() { return filtered; };

}
