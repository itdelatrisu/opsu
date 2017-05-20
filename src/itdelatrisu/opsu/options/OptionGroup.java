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

package itdelatrisu.opsu.options;

import itdelatrisu.opsu.options.Options.GameOption;
import itdelatrisu.opsu.translations.LocaleManager;

/**
 * Option category and related options.
 */
public class OptionGroup {
	/** All option groups. */
	public static final OptionGroup[] ALL_OPTIONS = new OptionGroup[] {
		new OptionGroup("options.general.name", null),
		new OptionGroup("options.general.language.name", new GameOption[] {
			GameOption.DISPLAY_LANGUAGE,
			GameOption.REFRESH_LANGS,
			GameOption.SHOW_UNICODE,
		}),
		new OptionGroup("options.general.updates.name", new GameOption[] {
			GameOption.DISABLE_UPDATER,
		}),
		new OptionGroup("options.graphics.name", null),
		new OptionGroup("options.graphics.layout.name", new GameOption[] {
			GameOption.SCREEN_RESOLUTION,
			GameOption.FULLSCREEN,
		}),
		new OptionGroup("options.graphics.renderer.name", new GameOption[] {
			GameOption.TARGET_FPS,
			GameOption.SHOW_FPS,
		}),
		new OptionGroup("options.graphics.details.name", new GameOption[] {
			GameOption.SNAKING_SLIDERS,
			GameOption.ENABLE_VIDEOS,
			GameOption.ENABLE_STORYBOARDS,
			GameOption.SHOW_COMBO_BURSTS,
			GameOption.SHOW_HIT_LIGHTING,
			GameOption.SHOW_PERFECT_HIT,
			GameOption.SHOW_FOLLOW_POINTS,
			GameOption.SCREENSHOT_FORMAT,
		}),
		new OptionGroup("options.graphics.sliderBeta.name", new GameOption[] {
			GameOption.EXPERIMENTAL_SLIDERS,
			GameOption.EXPERIMENTAL_SLIDERS_MERGE,
			GameOption.EXPERIMENTAL_SLIDERS_SHRINK,
			GameOption.EXPERIMENTAL_SLIDERS_CAPS,
		}),
		new OptionGroup("options.graphics.mainMenu.name", new GameOption[] {
			GameOption.DYNAMIC_BACKGROUND,
			GameOption.PARALLAX,
			GameOption.ENABLE_THEME_SONG,
		}),
		new OptionGroup("options.gameplay.name", null),
		new OptionGroup("options.gameplay.general.name", new GameOption[] {
			GameOption.BACKGROUND_DIM,
			GameOption.FORCE_DEFAULT_PLAYFIELD,
			GameOption.SHOW_HIT_ERROR_BAR,
		}),
		new OptionGroup("options.audio.name", null),
		new OptionGroup("options.audio.volume.name", new GameOption[] {
			GameOption.MASTER_VOLUME,
			GameOption.MUSIC_VOLUME,
			GameOption.EFFECT_VOLUME,
			GameOption.HITSOUND_VOLUME,
			GameOption.DISABLE_SOUNDS,
		}),
		new OptionGroup("options.audio.offset.name", new GameOption[] {
			GameOption.MUSIC_OFFSET,
		}),
		new OptionGroup("options.visuals.skin.name", null),
		new OptionGroup("options.visuals.skin.name", new GameOption[]{
			GameOption.SKIN,
			GameOption.LOAD_HD_IMAGES,
			GameOption.IGNORE_BEATMAP_SKINS,
			GameOption.FORCE_SKIN_CURSOR,
			GameOption.CURSOR_SIZE,
			GameOption.DISABLE_CURSOR,
		}),
		new OptionGroup("options.input.name", null),
		new OptionGroup("options.input.mouse.name", new GameOption[] {
			GameOption.DISABLE_MOUSE_WHEEL,
			GameOption.DISABLE_MOUSE_BUTTONS,
		}),
		new OptionGroup("options.input.keyboard.name", new GameOption[] {
			GameOption.KEY_LEFT,
			GameOption.KEY_RIGHT,
		}),
		new OptionGroup("options.custom.name", null),
		new OptionGroup("options.custom.difficulty.name", new GameOption[] {
			GameOption.FIXED_CS,
			GameOption.FIXED_HP,
			GameOption.FIXED_AR,
			GameOption.FIXED_OD,
			GameOption.FIXED_SPEED,
		}),
		new OptionGroup("options.custom.seeking.name", new GameOption[] {
			GameOption.CHECKPOINT,
			GameOption.REPLAY_SEEKING,
		}),
		new OptionGroup("options.custom.misc.name", new GameOption[] {
			GameOption.ENABLE_WATCH_SERVICE,
			GameOption.LOAD_VERBOSE,
		}),
	};

	/** The category name. */
	private final String category;

	/** The game options. */
	private final GameOption[] options;

	/** Whether this group should be visible (used for filtering in the options menu). */
	private boolean visible = true;

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
	public String getName() { return LocaleManager.translateKey(category); }

	/** Returns the related options. */
	public GameOption[] getOptions() { return options; }

	/** Returns the option at the given index. */
	public GameOption getOption(int i) { return options[i]; }

	/** Sets whether this group should be visible. */
	public void setVisible(boolean visible) { this.visible = visible; }

	/** Returns whether or not this group should be visible. */
	public boolean isVisible() { return visible; };
}
