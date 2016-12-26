package itdelatrisu.opsu;

import itdelatrisu.opsu.Options.GameOption;

/**
 * Option category and related options.
 */
public class OptionGroup {
	/** All option groups. */
	public static final OptionGroup[] ALL_OPTIONS = new OptionGroup[]{
		new OptionGroup("Display", new GameOption[]{
			GameOption.SCREEN_RESOLUTION,
//			GameOption.FULLSCREEN,
			GameOption.SKIN,
			GameOption.TARGET_FPS,
			GameOption.SHOW_FPS,
			GameOption.SHOW_UNICODE,
			GameOption.SCREENSHOT_FORMAT,
			GameOption.DYNAMIC_BACKGROUND,
			GameOption.LOAD_HD_IMAGES,
			GameOption.LOAD_VERBOSE
		}),
		new OptionGroup("Music", new GameOption[] {
			GameOption.MASTER_VOLUME,
			GameOption.MUSIC_VOLUME,
			GameOption.EFFECT_VOLUME,
			GameOption.HITSOUND_VOLUME,
			GameOption.MUSIC_OFFSET,
			GameOption.DISABLE_SOUNDS,
			GameOption.ENABLE_THEME_SONG
		}),
		new OptionGroup("Gameplay", new GameOption[] {
			GameOption.BACKGROUND_DIM,
			GameOption.FORCE_DEFAULT_PLAYFIELD,
			GameOption.IGNORE_BEATMAP_SKINS,
			GameOption.SNAKING_SLIDERS,
			GameOption.SHOW_HIT_LIGHTING,
			GameOption.SHOW_COMBO_BURSTS,
			GameOption.SHOW_PERFECT_HIT,
			GameOption.SHOW_FOLLOW_POINTS,
			GameOption.SHOW_HIT_ERROR_BAR
		}),
		new OptionGroup("Input", new GameOption[] {
			GameOption.KEY_LEFT,
			GameOption.KEY_RIGHT,
			GameOption.DISABLE_MOUSE_WHEEL,
			GameOption.DISABLE_MOUSE_BUTTONS,
			GameOption.CURSOR_SIZE,
			GameOption.NEW_CURSOR,
			GameOption.DISABLE_CURSOR
		}),
		new OptionGroup("Custom", new GameOption[] {
			GameOption.FIXED_CS,
			GameOption.FIXED_HP,
			GameOption.FIXED_AR,
			GameOption.FIXED_OD,
			GameOption.CHECKPOINT,
			GameOption.REPLAY_SEEKING,
			GameOption.DISABLE_UPDATER,
			GameOption.ENABLE_WATCH_SERVICE
		})
	};

	/** The category name. */
	private final String category;

	/** The game options. */
	private final GameOption[] options;

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
}
