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

import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.options.Options.GameOption;
import itdelatrisu.opsu.translation.I18n;

import org.newdawn.slick.Image;
import org.newdawn.slick.UnicodeFont;

/**
 * Option category and related options.
 */
public class OptionGroup {
	/** All option groups. */
	public static final OptionGroup[] ALL_OPTIONS = new OptionGroup[] {
		new OptionGroup("optionGroup.general", GameImage.MENU_NAV_GENERAL),
		new OptionGroup("optionGroup.general.language", new GameOption[] {
			GameOption.DISPLAY_LANGUAGE,
			GameOption.SHOW_UNICODE,
		}),
		new OptionGroup("optionGroup.general.updates", new GameOption[] {
			GameOption.DISABLE_UPDATER,
		}),
		new OptionGroup("optionGroup.graphics", GameImage.MENU_NAV_GRAPHICS),
		new OptionGroup("optionGroup.graphics.layout", new GameOption[] {
			GameOption.SCREEN_RESOLUTION,
			GameOption.FULLSCREEN,
		}),
		new OptionGroup("optionGroup.graphics.renderer", new GameOption[] {
			GameOption.TARGET_FPS,
			GameOption.SHOW_FPS,
		}),
		new OptionGroup("optionGroup.graphics.details", new GameOption[] {
			GameOption.SNAKING_SLIDERS,
			GameOption.ENABLE_VIDEOS,
			GameOption.SHOW_COMBO_BURSTS,
			GameOption.SHOW_HIT_LIGHTING,
			GameOption.SHOW_PERFECT_HIT,
			GameOption.SHOW_FOLLOW_POINTS,
			GameOption.SCREENSHOT_FORMAT,
		}),
		new OptionGroup("optionGroup.graphics.newSliders", new GameOption[] {
			GameOption.EXPERIMENTAL_SLIDERS,
			GameOption.EXPERIMENTAL_SLIDERS_MERGE,
			GameOption.EXPERIMENTAL_SLIDERS_SHRINK,
			GameOption.EXPERIMENTAL_SLIDERS_CAPS,
		}),
		new OptionGroup("optionGroup.graphics.mainMenu", new GameOption[] {
			GameOption.DYNAMIC_BACKGROUND,
			GameOption.PARALLAX,
			GameOption.ENABLE_THEME_SONG,
		}),
		new OptionGroup("optionGroup.gameplay", GameImage.MENU_NAV_GAMEPLAY),
		new OptionGroup("optionGroup.gameplay.general", new GameOption[] {
			GameOption.BACKGROUND_DIM,
			GameOption.FORCE_DEFAULT_PLAYFIELD,
			GameOption.SHOW_HIT_ERROR_BAR,
			GameOption.ALWAYS_SHOW_KEY_OVERLAY,
		}),
		new OptionGroup("optionGroup.audio", GameImage.MENU_NAV_AUDIO),
		new OptionGroup("optionGroup.audio.volume", new GameOption[] {
			GameOption.MASTER_VOLUME,
			GameOption.MUSIC_VOLUME,
			GameOption.EFFECT_VOLUME,
			GameOption.HITSOUND_VOLUME,
			GameOption.DISABLE_SOUNDS,
		}),
		new OptionGroup("optionGroup.audio.offset", new GameOption[] {
			GameOption.MUSIC_OFFSET,
		}),
		new OptionGroup("optionGroup.visuals", GameImage.MENU_NAV_SKIN),
		new OptionGroup("optionGroup.visuals.skin", new GameOption[]{
			GameOption.SKIN,
			GameOption.LOAD_HD_IMAGES,
			GameOption.IGNORE_BEATMAP_SKINS,
			GameOption.FORCE_SKIN_CURSOR,
			GameOption.CURSOR_SIZE,
			GameOption.DISABLE_CURSOR,
		}),
		new OptionGroup("optionGroup.input", GameImage.MENU_NAV_INPUT),
		new OptionGroup("optionGroup.input.mouse", new GameOption[] {
			GameOption.DISABLE_MOUSE_WHEEL,
			GameOption.DISABLE_MOUSE_BUTTONS,
		}),
		new OptionGroup("optionGroup.input.keyboard", new GameOption[] {
			GameOption.KEY_LEFT,
			GameOption.KEY_RIGHT,
		}),
		new OptionGroup("optionGroup.custom", GameImage.MENU_NAV_CUSTOM),
		new OptionGroup("optionGroup.custom.difficulty", new GameOption[] {
			GameOption.FIXED_CS,
			GameOption.FIXED_HP,
			GameOption.FIXED_AR,
			GameOption.FIXED_OD,
			GameOption.FIXED_SPEED,
		}),
		new OptionGroup("optionGroup.custom.seeking", new GameOption[] {
			GameOption.CHECKPOINT,
			GameOption.REPLAY_SEEKING,
		}),
		new OptionGroup("optionGroup.custom.misc", new GameOption[] {
			GameOption.ENABLE_WATCH_SERVICE,
			GameOption.LOAD_VERBOSE,
		}),
	};

	/** The category name. */
	private final String category;

	/** The icon, if this is a section header. */
	private final GameImage icon;

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
		this.icon = null;
	}

	/**
	 * Creates an option group with the given category name and icon, effectively being a section.
	 * @param category the category name
	 * @param icon the icon to be used for this section
	 */
	public OptionGroup(String category, GameImage icon) {
		this.category = category;
		this.options = null;
		this.icon = icon;
	}


	/**
	 * Returns the category name. 
	 * @param font The font to use in displaying the translation strings
	 */
	public String getName(UnicodeFont font) { return I18n.translate(category, font); }

	/** Returns the related options. */
	public GameOption[] getOptions() { return options; }

	/** Returns the related icon. */
	public Image getIcon() { return icon.getImage(); }

	/** Returns the option at the given index. */
	public GameOption getOption(int i) { return options[i]; }

	/** Sets whether this group should be visible. */
	public void setVisible(boolean visible) { this.visible = visible; }

	/** Returns whether or not this group should be visible. */
	public boolean isVisible() { return visible; };
}
