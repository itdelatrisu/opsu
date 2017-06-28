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

import itdelatrisu.opsu.options.Options;
import itdelatrisu.opsu.ui.Fonts;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.newdawn.slick.Animation;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.util.ResourceLoader;

/**
 * Game images.
 */
public enum GameImage {
	// Cursor
	CURSOR ("cursor", "png"),
	CURSOR_MIDDLE ("cursormiddle", "png"),
	CURSOR_TRAIL ("cursortrail", "png"),

	// Game
	INPUTOVERLAY_KEY ("inputoverlay-key", "png"),
	INPUTOVERLAY_BACKGROUND ("inputoverlay-background", "png"),
	SECTION_PASS ("section-pass", "png"),
	SECTION_FAIL ("section-fail", "png"),
	WARNINGARROW ("play-warningarrow", "png"),
	SKIP ("play-skip", "play-skip-%d", "png"),
	COUNTDOWN_READY ("ready", "png") {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy((h / 3f) / img.getHeight());
		}
	},
	COUNTDOWN_3 ("count3", "png") {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return COUNTDOWN_READY.process_sub(img, w, h);
		}
	},
	COUNTDOWN_2 ("count2", "png") {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return COUNTDOWN_READY.process_sub(img, w, h);
		}
	},
	COUNTDOWN_1 ("count1", "png") {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return COUNTDOWN_READY.process_sub(img, w, h);
		}
	},
	COUNTDOWN_GO ("go", "png") {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy((h / 3f) / img.getHeight());
		}
	},
	HITCIRCLE_SELECT ("hitcircleselect", "png"),
	UNRANKED ("play-unranked", "png"),
	FOLLOWPOINT ("followpoint", "png"),

	// Game Pause/Fail
	PAUSE_CONTINUE ("pause-continue", "png"),
	PAUSE_RETRY ("pause-retry", "png"),
	PAUSE_BACK ("pause-back", "png"),
	PAUSE_REPLAY ("pause-replay", "png"),
	PAUSE_OVERLAY ("pause-overlay", "png|jpg") {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			img.setAlpha(0.7f);
			return img.getScaledCopy(w, h);
		}
	},
	FAIL_BACKGROUND ("fail-background", "png|jpg") {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			img.setAlpha(0.7f);
			return img.getScaledCopy(w, h);
		}
	},

	// Circle
	HITCIRCLE ("hitcircle", "png"),
	HITCIRCLE_OVERLAY ("hitcircleoverlay", "png"),
	APPROACHCIRCLE ("approachcircle", "png"),

	// Slider
	SLIDER_GRADIENT ("slidergradient", "png"),
	SLIDER_GRADIENT_EXPERIMENTAL ("slidergradient_ex", "png"),
	SLIDER_BALL ("sliderb", "sliderb%d", "png"),
	SLIDER_FOLLOWCIRCLE ("sliderfollowcircle", "png"),
	REVERSEARROW ("reversearrow", "png"),
	SLIDER_TICK ("sliderscorepoint", "png"),

	// Spinner
	SPINNER_CIRCLE ("spinner-circle", "png"),
	SPINNER_APPROACHCIRCLE ("spinner-approachcircle", "png") {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy(1.8f);
		}
	},
	SPINNER_METRE ("spinner-metre", "png") {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy(w, h);
		}
	},
	SPINNER_SPIN ("spinner-spin", "png"),
	SPINNER_CLEAR ("spinner-clear", "png"),
	SPINNER_OSU ("spinner-osu", "png"),
	SPINNER_RPM ("spinner-rpm", "png") {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy(0.8f);
		}
	},

	// Game Data
	COMBO_BURST ("comboburst", "comboburst-%d", "png"),
	SCOREBAR_BG ("scorebar-bg", "png"),
	SCOREBAR_COLOUR ("scorebar-colour", "scorebar-colour-%d", "png"),
	//TODO scorebar-marker?
	SCOREBAR_KI ("scorebar-ki", "png"),
	SCOREBAR_KI_DANGER ("scorebar-kidanger", "png"),
	SCOREBAR_KI_DANGER2 ("scorebar-kidanger2", "png"),
	HIT_MISS ("hit0", "png"),
	HIT_50 ("hit50", "png"),
	HIT_100 ("hit100", "png"),
	HIT_300 ("hit300", "png"),
	HIT_100K ("hit100k", "png"),
	HIT_300K ("hit300k", "png"),
	HIT_300G ("hit300g", "png"),
	HIT_SLIDER10 ("sliderpoint10", "png"),
	HIT_SLIDER30 ("sliderpoint30", "png"),
	RANKING_SS ("ranking-X", "png"),
	RANKING_SS_SMALL ("ranking-X-small", "png"),
	RANKING_SSH ("ranking-XH", "png"),
	RANKING_SSH_SMALL ("ranking-XH-small", "png"),
	RANKING_S ("ranking-S", "png"),
	RANKING_S_SMALL ("ranking-S-small", "png"),
	RANKING_SH ("ranking-SH", "png"),
	RANKING_SH_SMALL ("ranking-SH-small", "png"),
	RANKING_A ("ranking-A", "png"),
	RANKING_A_SMALL ("ranking-A-small", "png"),
	RANKING_B ("ranking-B", "png"),
	RANKING_B_SMALL ("ranking-B-small", "png"),
	RANKING_C ("ranking-C", "png"),
	RANKING_C_SMALL ("ranking-C-small", "png"),
	RANKING_D ("ranking-D", "png"),
	RANKING_D_SMALL ("ranking-D-small", "png"),
	RANKING_PANEL ("ranking-panel", "png"),
	RANKING_PERFECT ("ranking-perfect", "png"),
	RANKING_TITLE ("ranking-title", "png"),
	RANKING_MAXCOMBO ("ranking-maxcombo", "png"),
	RANKING_ACCURACY ("ranking-accuracy", "png"),
	RANKING_GRAPH ("ranking-graph", "png", false, false),
	DEFAULT_0 ("default-0", "png"),
	DEFAULT_1 ("default-1", "png"),
	DEFAULT_2 ("default-2", "png"),
	DEFAULT_3 ("default-3", "png"),
	DEFAULT_4 ("default-4", "png"),
	DEFAULT_5 ("default-5", "png"),
	DEFAULT_6 ("default-6", "png"),
	DEFAULT_7 ("default-7", "png"),
	DEFAULT_8 ("default-8", "png"),
	DEFAULT_9 ("default-9", "png"),
	SCORE_0 ("score-0", "png"),
	SCORE_1 ("score-1", "png"),
	SCORE_2 ("score-2", "png"),
	SCORE_3 ("score-3", "png"),
	SCORE_4 ("score-4", "png"),
	SCORE_5 ("score-5", "png"),
	SCORE_6 ("score-6", "png"),
	SCORE_7 ("score-7", "png"),
	SCORE_8 ("score-8", "png"),
	SCORE_9 ("score-9", "png"),
	SCORE_COMMA ("score-comma", "png"),
	SCORE_DOT ("score-dot", "png"),
	SCORE_PERCENT ("score-percent", "png"),
	SCORE_X ("score-x", "png"),
	LIGHTING ("lighting", "png"),

	// Game Mods
	MOD_EASY ("selection-mod-easy", "png", false, false),
	MOD_NO_FAIL ("selection-mod-nofail", "png", false, false),
	MOD_HARD_ROCK ("selection-mod-hardrock", "png", false, false),
	MOD_SUDDEN_DEATH ("selection-mod-suddendeath", "png", false, false),
	MOD_SPUN_OUT ("selection-mod-spunout", "png", false, false),
	MOD_AUTO ("selection-mod-autoplay", "png", false, false),
	MOD_HALF_TIME ("selection-mod-halftime", "png", false, false),
	MOD_DOUBLE_TIME ("selection-mod-doubletime", "png", false, false),
	MOD_HIDDEN ("selection-mod-hidden", "png", false, false),
	MOD_FLASHLIGHT ("selection-mod-flashlight", "png", false, false),
	MOD_RELAX ("selection-mod-relax", "png", false, false),
	MOD_AUTOPILOT ("selection-mod-relax2", "png", false, false),

	// Selection Buttons
	SELECTION_MODS ("selection-mods", "png", false, false),
	SELECTION_MODS_OVERLAY ("selection-mods-over", "png", false, false),
	SELECTION_RANDOM ("selection-random", "png", false, false),
	SELECTION_RANDOM_OVERLAY ("selection-random-over", "png", false, false),
	SELECTION_OPTIONS ("selection-options", "png", false, false),
	SELECTION_OPTIONS_OVERLAY ("selection-options-over", "png", false, false),
	SELECTION_OTHER_OPTIONS ("selection-selectoptions", "png", false, false),
	SELECTION_OTHER_OPTIONS_OVERLAY ("selection-selectoptions-over", "png", false, false),

	// Replay Speed Buttons
	REPLAY_PLAYBACK_NORMAL ("playback-normal", "png", false, false),
	REPLAY_PLAYBACK_DOUBLE ("playback-double", "png", false, false),
	REPLAY_PLAYBACK_HALF ("playback-half", "png", false, false),

	// Non-Game Components
	VOLUME ("volume-bg", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy((h * 0.3f) / img.getHeight());
		}
	},
	MENU_BACK ("menu-back", "menu-back-%d", "png", false, true),
	MENU_BACK_CHEVRON ("menu-back-chevron", "png"),
	MENU_BACK_SLOPE("menu-back-slope", "png"),
	MENU_BUTTON_BG ("menu-button-background", "png", false, false),
	MENU_TAB ("selection-tab", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy((h * 0.033f) / img.getHeight());
		}
	},
	MENU_MUSICNOTE ("music-note", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			int r = (int) ((Fonts.LARGE.getLineHeight() + Fonts.DEFAULT.getLineHeight() - 8) / getUIscale());
			return img.getScaledCopy(r, r);
		}
	},
	MENU_LOADER ("loader", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			int r = (int) ((Fonts.LARGE.getLineHeight() + Fonts.DEFAULT.getLineHeight() - 8) / getUIscale());
			return img.getScaledCopy(r / 48f);
		}
	},
	MENU_BG ("menu-background", "png|jpg", false, true) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			img.setAlpha(0.9f);
			return img.getScaledCopy(w, h);
		}
	},
	MENU_LOGO ("logo", "png", false, true) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy(0.8f);
		}
	},
	MENU_PLAY ("menu-play", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy(0.8f);
		}
	},
	MENU_EXIT ("menu-exit", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy(0.8f);
		}
	},
	MENU_BUTTON_MID ("button-middle", "png", false, false),
	MENU_BUTTON_LEFT ("button-left", "png", false, false),
	MENU_BUTTON_RIGHT ("button-right", "png", false, false),
	STAR ("star", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy((MENU_BUTTON_BG.getImage().getHeight() * 0.16f) / img.getHeight());
		}
	},
	STAR2 ("star2", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy((MENU_BUTTON_BG.getImage().getHeight() * 0.33f) / img.getHeight());
		}
	},

	// Music Player Buttons
	MUSIC_PLAY ("music-play", "png", false, false),
	MUSIC_PAUSE ("music-pause", "png", false, false),
	MUSIC_NEXT ("music-next", "png", false, false),
	MUSIC_PREVIOUS ("music-previous", "png", false, false),
	MUSIC_NOW_PLAYING ("music-now-playing", "png", false, false),

	DOWNLOADS ("downloads", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy((h * 0.45f) / img.getHeight());
		}
	},
	SEARCH_BG ("search-background", "png|jpg", false, true) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			img.setAlpha(0.8f);
			return img.getScaledCopy(w, h);
		}
	},
	DELETE ("delete", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			int lineHeight = Fonts.DEFAULT.getLineHeight();
			return img.getScaledCopy(lineHeight, lineHeight);
		}
	},
	HISTORY ("history", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy((h * 0.0278f) / img.getHeight());
		}
	},
	REPOSITORY ("repo", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy((h / 17f) / img.getHeight());
		}
	},
	DOWNLOAD ("download", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy((h / 14f) / img.getHeight());
		}
	},
	UPDATE ("update", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy((h / 14f) / img.getHeight());
		}
	},
	CHEVRON_DOWN ("chevron-down", "png", false, false),
	CHEVRON_RIGHT ("chevron-right", "png", false, false),
	CHEVRON_LEFT ("chevron-left", "png", false, false),

	// Options menu
	SEARCH ("search", "png", false, false),
	CONTROL_SLIDER_BALL ("control-sliderball", "png", false, false),
	CONTROL_CHECK_ON ("control-check-on", "png", false, false),
	CONTROL_CHECK_OFF ("control-check-off", "png", false, false),
	MENU_NAV_AUDIO ("menu-nav-audio", "png", false, false),
	MENU_NAV_CUSTOM ("menu-nav-custom", "png", false, false),
	MENU_NAV_GAMEPLAY ("menu-nav-gameplay", "png", false, false),
	MENU_NAV_GENERAL ("menu-nav-general", "png", false, false),
	MENU_NAV_GRAPHICS ("menu-nav-graphics", "png", false, false),
	MENU_NAV_INPUT ("menu-nav-input", "png", false, false),
	MENU_NAV_SKIN ("menu-nav-skin", "png", false, false),

	// User selection menu
	USER ("user", "user%d", "png", false, false),

	// TODO: ensure this image hasn't been modified (checksum?)
	ALPHA_MAP ("alpha", "png", false, false);

	/** Image file types. */
	private static final byte
		IMG_PNG = 1,
		IMG_JPG = 2;

	/** The file name. */
	private final String filename;

	/** The formatted file name string (for loading multiple images). */
	private String filenameFormat;

	/** Image file type. */
	private final byte type;

	/**
	 * Whether or not the image is skinnable by a beatmap.
	 * These images are typically related to gameplay.
	 */
	private final boolean beatmapSkinnable;

	/** Whether or not to preload the image when the program starts. */
	private final boolean preload;

	/** The default image. */
	private Image defaultImage;

	/** The default image array. */
	private Image[] defaultImages;

	/** Whether the image is currently skinned by a game skin. */
	private boolean isSkinned = false;

	/** The beatmap skin image (optional, temporary). */
	private Image skinImage;

	/** The beatmap skin image array (optional, temporary). */
	private Image[] skinImages;

	/** Container dimensions. */
	private static int containerWidth, containerHeight;

	/** Value to scale UI components by. */
	private static float uiscale;

	/** The unscaled container height that uiscale is based on. */
	private static final int UNSCALED_HEIGHT = 768;

	/** Value to scale backgrounds for the parallax effect. */
	public static final float PARALLAX_SCALE = 1.008f;

	/** Filename suffix for HD images. */
	public static final String HD_SUFFIX = "@2x";

	/** Image HD/SD suffixes. */
	private static final String[]
		SUFFIXES_HD = new String[] { HD_SUFFIX, "" },
		SUFFIXES_SD = new String[] { "" };

	/**
	 * Initializes the GameImage class with container dimensions.
	 * @param width the container width
	 * @param height the container height
	 */
	public static void init(int width, int height) {
		containerWidth = width;
		containerHeight = height;
		uiscale = (float) containerHeight / UNSCALED_HEIGHT;
	}

	/**
	 * Returns the UI scale.
	 */
	public static float getUIscale() { return uiscale; }

	/**
	 * Clears all image references.
	 * This does NOT destroy images, so be careful of memory leaks!
	 */
	public static void clearReferences() {
		for (GameImage img : GameImage.values()) {
			img.defaultImage = img.skinImage = null;
			img.defaultImages = img.skinImages = null;
			img.isSkinned = false;
		}
	}

	/**
	 * Returns the bitmask image type from a type string.
	 * @param type the type string
	 * @return the byte bitmask
	 */
	private static byte getType(String type) {
		byte b = 0;
		String[] s = type.split("\\|");
		for (int i = 0; i < s.length; i++) {
			if (s[i].equals("png"))
				b |= IMG_PNG;
			else if (s[i].equals("jpg"))
				b |= IMG_JPG;
		}
		return b;
	}

	/**
	 * Returns the image file name, with extension, by first looking through
	 * the given directory and then the default resource locations (unless
	 * dirOnly is true).
	 * @param filename the base file name
	 * @param dir the directory to search first (if non-null)
	 * @param type the file type bitmask (IMG_*)
	 * @param dirOnly if true and dir is non-null, will not search default resource locations
	 * @return the full file name, or null if no file found
	 */
	private static String getImageFileName(String filename, File dir, byte type, boolean dirOnly) {
		ArrayList<String> names = new ArrayList<String>(2);
		if ((type & IMG_PNG) != 0)
			names.add(String.format("%s.png", filename));
		if ((type & IMG_JPG) != 0)
			names.add(String.format("%s.jpg", filename));
		int size = names.size();

		// look through directory
		if (dir != null) {
			for (int i = 0; i < size; i++) {
				File file = new File(dir, names.get(i));
				if (file.isFile())
					return file.getAbsolutePath();
			}
		}

		// look through default resource path
		if (!dirOnly || dir == null) {
			for (int i = 0; i < size; i++) {
				if (ResourceLoader.resourceExists(names.get(i)))
					return names.get(i);
			}
		}

		return null;
	}

	/**
	 * Returns an array of HD/SD file name suffixes based on the current options
	 * and UI scale.
	 */
	private static String[] getSuffixes() {
		return (Options.loadHDImages() && uiscale >= 1) ? SUFFIXES_HD : SUFFIXES_SD;
	}

	/**
	 * Constructor for game-related images (beatmap-skinnable and preloaded).
	 * @param filename the image file name
	 * @param type the file types (separated by '|')
	 */
	GameImage(String filename, String type) {
		this(filename, type, true, false);
	}

	/**
	 * Constructor for an array of game-related images (beatmap-skinnable and preloaded).
	 * @param filename the image file name
	 * @param filenameFormat the formatted file name string (for loading multiple images)
	 * @param type the file types (separated by '|')
	 */
	GameImage(String filename, String filenameFormat, String type) {
		this(filename, type, true, false);
		this.filenameFormat = filenameFormat;
	}

	/**
	 * Constructor for general images.
	 * @param filename the image file name
	 * @param type the file types (separated by '|')
	 * @param beatmapSkinnable whether or not the image is beatmap-skinnable
	 * @param preload whether or not to preload the image
	 */
	GameImage(String filename, String type, boolean beatmapSkinnable, boolean preload) {
		this.filename = filename;
		this.type = getType(type);
		this.beatmapSkinnable = beatmapSkinnable;
		this.preload = preload;
	}

	/**
	 * Constructor for an array of general images.
	 * @param filename the image file name
	 * @param filenameFormat the formatted file name string (for loading multiple images)
	 * @param type the file types (separated by '|')
	 * @param beatmapSkinnable whether or not the image is beatmap-skinnable
	 * @param preload whether or not to preload the image
	 */
	GameImage(String filename, String filenameFormat, String type, boolean beatmapSkinnable, boolean preload) {
		this(filename, type, beatmapSkinnable, preload);
		this.filenameFormat = filenameFormat;
	}

	/**
	 * Returns whether or not the image is beatmap-skinnable.
	 * @return true if beatmap-skinnable
	 */
	public boolean isBeatmapSkinnable() { return beatmapSkinnable; }

	/**
	 * Returns whether or not to preload the image when the program starts.
	 * @return true if preload
	 */
	public boolean isPreload() { return preload; }

	/**
	 * Returns the image associated with this resource.
	 * The beatmap skin image takes priority over the default image.
	 */
	public Image getImage() {
		setDefaultImage();
		return (skinImage != null) ? skinImage : defaultImage;
	}

	/**
	 * Returns the image associated with this resource.
	 * This ignores beatmap skin images.
	 */
	public Image getDefaultImage() {
		setDefaultImage();
		return defaultImage;
	}

	/**
	 * Returns an Animation based on the image array.
	 * If no image array exists, returns the single image as an animation.
	 */
	public Animation getAnimation() {
		Image[] images = getImages();
		if (images == null)
			images = new Image[] { getImage() };

		int fps = Options.getSkin().getAnimationFramerate();
		if (fps == -1)
			fps = images.length;

		return new Animation(images, 1000 / fps);
	}

	/**
	 * Returns an Animation based on the image array.
	 * If no image array exists, returns the single image as an animation.
	 * @param duration the duration to show each frame in the animation
	 */
	public Animation getAnimation(int duration){
		Image[] images = getImages();
		if (images == null)
			images = new Image[] { getImage() };

		return new Animation(images, duration);
	}

	/**
	 * Returns the image array associated with this resource.
	 * The beatmap skin images takes priority over the default images.
	 */
	public Image[] getImages() {
		setDefaultImage();
		return (skinImages != null) ? skinImages : defaultImages;
	}

	/**
	 * Returns the image array associated with this resource.
	 * This ignores beatmap skin images.
	 */
	public Image[] getDefaultImages() {
		setDefaultImage();
		return defaultImages;
	}

	/**
	 * Sets the image associated with this resource to another image.
	 * The beatmap skin image takes priority over the default image.
	 * @param img the image to set
	 */
	public void setImage(Image img) {
		if (skinImage != null)
			this.skinImage = img;
		else
			this.defaultImage = img;
	}

	/**
	 * Sets an image associated with this resource to another image.
	 * The beatmap skin image takes priority over the default image.
	 * @param img the image to set
	 * @param index the index in the image array
	 */
	public void setImage(Image img, int index) {
		if (skinImages != null) {
			if (index < skinImages.length)
				this.skinImages[index] = img;
		} else {
			if (index < defaultImages.length)
				this.defaultImages[index] = img;
		}
	}

	/**
	 * Sets the default image for this resource.
	 * If the default image has already been loaded, this will do nothing.
	 */
	public void setDefaultImage() {
		if (defaultImage != null || defaultImages != null || Options.getSkin() == null)
			return;

		// check for multiple images first, then single images

		// try to load from skin directory
		File skinDir = Options.getSkin().getDirectory();
		if (skinDir != null) {
			if ((filenameFormat != null && (defaultImages = loadImageArray(skinDir)) != null) ||
			    (defaultImage = loadImageSingle(skinDir)) != null) {
				isSkinned = true;
				process();
				return;
			}
		}

		// try to load default image
		if ((filenameFormat != null && (defaultImages = loadImageArray(null)) != null) ||
		    (defaultImage = loadImageSingle(null)) != null) {
			isSkinned = false;
			process();
			return;
		}

		ErrorHandler.error(String.format("Could not find default image '%s'.", filename), null, false);
	}

	/**
	 * Sets the associated beatmap skin image.
	 * If the path does not contain the image, the default image is used.
	 * @param dir the image directory to search
	 * @return true if a new skin image is loaded, false otherwise
	 */
	public boolean setBeatmapSkinImage(File dir) {
		if (dir == null)
			return false;

		// destroy the existing images, if any
		destroyBeatmapSkinImage();

		// beatmap skins disabled
		if (Options.isBeatmapSkinIgnored())
			return false;

		// try to load multiple images
		if ((skinImages = loadImageArray(dir)) != null) {
			process();
			return true;
		}

		// try to load a single image
		if ((skinImage = loadImageSingle(dir)) != null) {
			process();
			return true;
		}

		return false;
	}

	/**
	 * Attempts to load multiple Images from the GameImage.
	 * @param dir the image directory to search, or null to use the default resource locations
	 * @return an array of the loaded images, or null if not found
	 */
	private Image[] loadImageArray(File dir) {
		if (filenameFormat != null) {
			for (String suffix : getSuffixes()) {
				List<Image> list = new ArrayList<Image>();
				int i = 0;
				while (true) {
					// look for next image
					String filenameFormatted = String.format(filenameFormat + suffix, i++);
					String name = getImageFileName(filenameFormatted, dir, type, true);
					if (name == null)
						break;

					// add image to list
					try {
						Image img = new Image(name);
						if (suffix.equals(HD_SUFFIX))
							img = img.getScaledCopy(0.5f);
						list.add(img);
					} catch (SlickException e) {
						ErrorHandler.error(String.format("Failed to set image '%s'.", name), null, false);
						break;
					}
				}
				if (!list.isEmpty())
					return list.toArray(new Image[list.size()]);
			}
		}
		return null;
	}

	/**
	 * Attempts to load a single Image from the GameImage.
	 * @param dir the image directory to search, or null to use the default resource locations
	 * @return the loaded image, or null if not found
	 */
	private Image loadImageSingle(File dir) {
		for (String suffix : getSuffixes()) {
			String name = getImageFileName(filename + suffix, dir, type, true);
			if (name != null) {
				try {
					Image img = new Image(name);
					if (suffix.equals(HD_SUFFIX))
						img = img.getScaledCopy(0.5f);
					return img;
				} catch (SlickException e) {
					ErrorHandler.error(String.format("Failed to set image '%s'.", filename), null, false);
				}
			}
		}
		return null;
	}

	/**
	 * Returns whether the default image loaded is part of a game skin.
	 * @return true if a game skin image is loaded, false if the default image is loaded
	 */
	public boolean hasGameSkinImage() { return isSkinned; }

	/**
	 * Returns whether a beatmap skin image is currently loaded.
	 * @return true if a beatmap skin image exists
	 */
	public boolean hasBeatmapSkinImage() { return (skinImage != null && !skinImage.isDestroyed()); }

	/**
	 * Returns whether beatmap skin images are currently loaded.
	 * @return true if any beatmap skin image exists
	 */
	public boolean hasBeatmapSkinImages() { return (skinImages != null); }

	/**
	 * Destroys the associated beatmap skin image(s), if any.
	 */
	public void destroyBeatmapSkinImage() {
		if (skinImage == null && skinImages == null)
			return;
		try {
			if (skinImage != null) {
				if (!skinImage.isDestroyed())
					skinImage.destroy();
				skinImage = null;
			}
			if (skinImages != null) {
				for (int i = 0; i < skinImages.length; i++) {
					if (!skinImages[i].isDestroyed())
						skinImages[i].destroy();
				}
				skinImages = null;
			}
		} catch (SlickException e) {
			ErrorHandler.error(String.format("Failed to destroy beatmap skin images for '%s'.", this.name()), e, true);
		}
	}

	/**
	 * Sub-method for image processing actions (via an override).
	 * @param img the image to process
	 * @param w the container width
	 * @param h the container height
	 * @return the processed image
	 */
	protected Image process_sub(Image img, int w, int h) { return img; }

	/**
	 * Performs individual post-loading actions on the image.
	 */
	private void process() {
		int unscaledWidth = UNSCALED_HEIGHT * containerWidth / containerHeight;
		if (skinImages != null) {
			for (int i = 0; i < skinImages.length; i++)
				setImage(process_sub(getImages()[i], unscaledWidth, UNSCALED_HEIGHT).getScaledCopy(getUIscale()), i);
		} else if (defaultImages != null && skinImage == null) {
			for (int i = 0; i < defaultImages.length; i++)
				setImage(process_sub(getImages()[i], unscaledWidth, UNSCALED_HEIGHT).getScaledCopy(getUIscale()), i);
		} else
			setImage(process_sub(getImage(), unscaledWidth, UNSCALED_HEIGHT).getScaledCopy(getUIscale()));
	}
}