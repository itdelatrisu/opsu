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

import itdelatrisu.opsu.states.SongMenu;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;

/**
 * Game images.
 */
public enum GameImage {
	// Cursor
	CURSOR ("cursor", "png") {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy(1 + ((h - 600) / 1000f));
		}
	},
	CURSOR_MIDDLE ("cursormiddle", "png") {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return CURSOR.process_sub(img, w, h);
		}
	},
	CURSOR_TRAIL ("cursortrail", "png") {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return CURSOR.process_sub(img, w, h);
		}
	},
	CURSOR_OLD ("cursor2", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return CURSOR.process_sub(img, w, h);
		}
	},
	CURSOR_TRAIL_OLD ("cursortrail2", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return CURSOR.process_sub(img, w, h);
		}
	},

	// Game
	SECTION_PASS ("section-pass", "png"),
	SECTION_FAIL ("section-fail", "png"),
	WARNINGARROW ("play-warningarrow", "png"),
	SKIP ("play-skip", "png") {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy((h * 0.1f) / img.getHeight());
		}
	},
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
			return COUNTDOWN_READY.process_sub(img, w, h);
		}
	},
	HITCIRCLE_SELECT ("hitcircleselect", "png"),
	UNRANKED ("play-unranked", "png"),
	PLAYFIELD ("playfield", "png|jpg", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			img.setAlpha(0.7f);
			return img.getScaledCopy(w, h);
		}
	},

	// Game Pause/Fail
	PAUSE_CONTINUE ("pause-continue", "png"),
	PAUSE_RETRY ("pause-retry", "png"),
	PAUSE_BACK ("pause-back", "png"),
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
	SLIDER_BALL ("sliderb", "sliderb%d", "png"),
	SLIDER_FOLLOWCIRCLE ("sliderfollowcircle", "png"),
	REVERSEARROW ("reversearrow", "png"),
	SLIDER_TICK ("sliderscorepoint", "png"),

	// Spinner
	SPINNER_CIRCLE ("spinner-circle", "png") {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy(h * 9 / 10, h * 9 / 10);
		}
	},
	SPINNER_APPROACHCIRCLE ("spinner-approachcircle", "png") {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy(h * 9 / 10, h * 9 / 10);
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

	// Score Data
	COMBO_BURST ("comboburst", "comboburst-%d", "png"),
	SCOREBAR_BG ("scorebar-bg", "png") {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy((w * 0.565f) / img.getWidth());
		}
	},
	SCOREBAR_COLOUR ("scorebar-colour", "scorebar-colour-%d", "png") {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy((w * 0.521f) / img.getWidth());
		}
	},
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
	RANKING_SS ("ranking-X", "png") {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy((h / 2f) / img.getHeight());
		}
	},
	RANKING_SS_SMALL ("ranking-X-small", "png"),
	RANKING_SSH ("ranking-XH", "png") {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return RANKING_SS.process_sub(img, w, h);
		}
	},
	RANKING_SSH_SMALL ("ranking-XH-small", "png"),
	RANKING_S ("ranking-S", "png") {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return RANKING_SS.process_sub(img, w, h);
		}
	},
	RANKING_S_SMALL ("ranking-S-small", "png"),
	RANKING_SH ("ranking-SH", "png") {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return RANKING_SS.process_sub(img, w, h);
		}
	},
	RANKING_SH_SMALL ("ranking-SH-small", "png"),
	RANKING_A ("ranking-A", "png") {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return RANKING_SS.process_sub(img, w, h);
		}
	},
	RANKING_A_SMALL ("ranking-A-small", "png"),
	RANKING_B ("ranking-B", "png") {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return RANKING_SS.process_sub(img, w, h);
		}
	},
	RANKING_B_SMALL ("ranking-B-small", "png"),
	RANKING_C ("ranking-C", "png") {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return RANKING_SS.process_sub(img, w, h);
		}
	},
	RANKING_C_SMALL ("ranking-C-small", "png"),
	RANKING_D ("ranking-D", "png") {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return RANKING_SS.process_sub(img, w, h);
		}
	},
	RANKING_D_SMALL ("ranking-D-small", "png"),
	RANKING_PANEL ("ranking-panel", "png") {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy((h * 0.63f) / img.getHeight());
		}
	},
	RANKING_PERFECT ("ranking-perfect", "png") {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy((h * 0.16f) / img.getHeight());
		}
	},
	RANKING_TITLE ("ranking-title", "png") {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy((h * 0.15f) / img.getHeight());
		}
	},
	RANKING_MAXCOMBO ("ranking-maxcombo", "png") {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy((h * 0.05f) / img.getHeight());
		}
	},
	RANKING_ACCURACY ("ranking-accuracy", "png") {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy((h * 0.05f) / img.getHeight());
		}
	},
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
	LIGHTING1 ("lighting1", "png"),

	// Game Mods
	MOD_EASY ("selection-mod-easy", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy((h / 12f) / img.getHeight());
		}
	},
	MOD_NO_FAIL ("selection-mod-nofail", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return MOD_EASY.process_sub(img, w, h);
		}
	},
	MOD_HARD_ROCK ("selection-mod-hardrock", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return MOD_EASY.process_sub(img, w, h);
		}
	},
	MOD_SUDDEN_DEATH ("selection-mod-suddendeath", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return MOD_EASY.process_sub(img, w, h);
		}
	},
	MOD_SPUN_OUT ("selection-mod-spunout", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return MOD_EASY.process_sub(img, w, h);
		}
	},
	MOD_AUTO ("selection-mod-autoplay", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return MOD_EASY.process_sub(img, w, h);
		}
	},
	MOD_HALF_TIME ("selection-mod-halftime", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return MOD_EASY.process_sub(img, w, h);
		}
	},
	MOD_DOUBLE_TIME ("selection-mod-doubletime", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return MOD_EASY.process_sub(img, w, h);
		}
	},
	MOD_HIDDEN ("selection-mod-hidden", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return MOD_EASY.process_sub(img, w, h);
		}
	},
	MOD_FLASHLIGHT ("selection-mod-flashlight", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return MOD_EASY.process_sub(img, w, h);
		}
	},
	MOD_RELAX ("selection-mod-relax", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return MOD_EASY.process_sub(img, w, h);
		}
	},
	MOD_AUTOPILOT ("selection-mod-relax2", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return MOD_EASY.process_sub(img, w, h);
		}
	},

	// Selection Buttons
	SELECTION_MODS ("selection-mods", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy((h * 0.115f) / img.getHeight());
		}
	},
	SELECTION_MODS_OVERLAY ("selection-mods-over", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return SELECTION_MODS.process_sub(img, w, h);
		}
	},
	SELECTION_RANDOM ("selection-random", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return SELECTION_MODS.process_sub(img, w, h);
		}
	},
	SELECTION_RANDOM_OVERLAY ("selection-random-over", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return SELECTION_MODS.process_sub(img, w, h);
		}
	},
	SELECTION_OPTIONS ("selection-options", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return SELECTION_MODS.process_sub(img, w, h);
		}
	},
	SELECTION_OPTIONS_OVERLAY ("selection-options-over", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return SELECTION_MODS.process_sub(img, w, h);
		}
	},
	SELECTION_OTHER_OPTIONS ("selection-selectoptions", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return SELECTION_MODS.process_sub(img, w, h);
		}
	},
	SELECTION_OTHER_OPTIONS_OVERLAY ("selection-selectoptions-over", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return SELECTION_MODS.process_sub(img, w, h);
		}
	},

	// Non-Game Components
	VOLUME ("volume-bg", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy((h * 0.3f) / img.getHeight());
		}
	},
	MENU_BACK ("menu-back", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy((h * 0.1f) / img.getHeight());
		}
	},
	MENU_BUTTON_BG ("menu-button-background", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			// TODO: scale these properly (messy due to non-cropped images)
			return img.getScaledCopy(w / 2, (int) (h * 0.95f) / SongMenu.MAX_SONG_BUTTONS);
		}
	},
	MENU_TAB ("selection-tab", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy((h * 0.033f) / img.getHeight());
		}
	},
	MENU_MUSICNOTE ("music-note", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			int r = (int) (Utils.FONT_LARGE.getLineHeight() * 0.75f + Utils.FONT_DEFAULT.getLineHeight());
			return img.getScaledCopy(r, r);
		}
	},
	MENU_LOADER ("loader", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			int r = (int) (Utils.FONT_LARGE.getLineHeight() * 0.75f + Utils.FONT_DEFAULT.getLineHeight());
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
			return img.getScaledCopy((h / 1.2f) / img.getHeight());
		}
	},
	MENU_PlAY ("menu-play", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy(MENU_LOGO.getImage().getWidth() * 0.83f / img.getWidth());
		}
	},
	MENU_EXIT ("menu-exit", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy(MENU_LOGO.getImage().getWidth() * 0.66f / img.getWidth());
		}
	},
	MENU_BUTTON_MID ("button-middle", "png", false, false),
	MENU_BUTTON_LEFT ("button-left", "png", false, false),
	MENU_BUTTON_RIGHT ("button-right", "png", false, false),

	// Music Player Buttons
	MUSIC_PLAY ("music-play", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy((h / 18f) / img.getHeight());
		}
	},
	MUSIC_PAUSE ("music-pause", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return MUSIC_PLAY.process_sub(img, w, h);
		}
	},
	MUSIC_NEXT ("music-next", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return MUSIC_PLAY.process_sub(img, w, h);
		}
	},
	MUSIC_PREVIOUS ("music-previous", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return MUSIC_PLAY.process_sub(img, w, h);
		}
	},

	RANKING_RETRY ("ranking-retry", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy((h * 0.15f) / img.getHeight());
		}
	},
	RANKING_EXIT ("ranking-back", "png", false, false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy((h * 0.15f) / img.getHeight());
		}
	},
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
			int lineHeight = Utils.FONT_DEFAULT.getLineHeight();
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
	};

	/** Image file types. */
	private static final byte
		IMG_PNG = 1,
		IMG_JPG = 2;

	/** The file name. */
	private String filename;

	/** The formatted file name string (for loading multiple images). */
	private String filenameFormat;

	/** Image file type. */
	private byte type;

	/**
	 * Whether or not the image is skinnable by a beatmap.
	 * These images are typically related to gameplay.
	 */
	private boolean skinnable;

	/** Whether or not to preload the image when the program starts. */
	private boolean preload;

	/** The default image. */
	private Image defaultImage;

	/** The default image array. */
	private Image[] defaultImages;

	/** The beatmap skin image (optional, temporary). */
	private Image skinImage;

	/** The beatmap skin image array (optional, temporary). */
	private Image[] skinImages;

	/** Container dimensions. */
	private static int containerWidth, containerHeight;

	/**
	 * Initializes the GameImage class with container dimensions.
	 * @param width the container width
	 * @param height the container height
	 */
	public static void init(int width, int height) {
		containerWidth = width;
		containerHeight = height;
	}

	/**
	 * Clears all image references.
	 * This does NOT destroy images, so be careful of memory leaks!
	 */
	public static void clearReferences() {
		for (GameImage img : GameImage.values()) {
			img.defaultImage = img.skinImage = null;
			img.defaultImages = img.skinImages = null;
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
	 * Returns a list of possible filenames (with extensions).
	 * @return filename list
	 */
	private static List<String> getFileNames(String filename, byte type) {
		List<String> list = new ArrayList<String>(2);
		if ((type & IMG_PNG) != 0)
			list.add(String.format("%s.png", filename));
		if ((type & IMG_JPG) != 0)
			list.add(String.format("%s.jpg", filename));
		return list;
	}

	/**
	 * Constructor for game-related images (skinnable and preloaded).
	 * @param filename the image file name
	 * @param type the file types (separated by '|')
	 */
	GameImage(String filename, String type) {
		this(filename, type, true, true);
	}

	/**
	 * Constructor for an array of game-related images (skinnable and preloaded).
	 * @param filename the image file name
	 * @param filenameFormat the formatted file name string (for loading multiple images)
	 * @param type the file types (separated by '|')
	 */
	GameImage(String filename, String filenameFormat, String type) {
		this(filename, type, true, true);
		this.filenameFormat = filenameFormat;
	}

	/**
	 * Constructor for general images.
	 * @param filename the image file name
	 * @param type the file types (separated by '|')
	 * @param skinnable whether or not the image is skinnable
	 * @param preload whether or not to preload the image
	 */
	GameImage(String filename, String type, boolean skinnable, boolean preload) {
		this.filename = filename;
		this.type = getType(type);
		this.skinnable = skinnable;
		this.preload = preload;
	}

	/**
	 * Returns whether or not the image is skinnable.
	 * @return true if skinnable
	 */
	public boolean isSkinnable() { return skinnable; }

	/**
	 * Returns whether or not to preload the image when the program starts.
	 * @return true if preload
	 */
	public boolean isPreload() { return preload; }

	/**
	 * Returns the image associated with this resource.
	 * The skin image takes priority over the default image.
	 */
	public Image getImage() {
		setDefaultImage();
		return (skinImage != null) ? skinImage : defaultImage;
	}

	/**
	 * Returns the image array associated with this resource.
	 * The skin images takes priority over the default images.
	 */
	public Image[] getImages() {
		setDefaultImage();
		return (skinImages != null) ? skinImages : defaultImages;
	}

	/**
	 * Sets the image associated with this resource to another image.
	 * The skin image takes priority over the default image.
	 */
	public void setImage(Image img) {
		if (skinImage != null)
			this.skinImage = img;
		else
			this.defaultImage = img;
	}

	/**
	 * Sets an image associated with this resource to another image.
	 * The skin image takes priority over the default image.
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
		if (defaultImage != null || defaultImages != null)
			return;

		// load image array
		if (filenameFormat != null) {
			List<Image> list = new ArrayList<Image>();
			int i = 0;
			boolean loaded;
			do {
				loaded = false;
				for (String name : getFileNames(String.format(filenameFormat, i), type)) {
					try {
						// try loading the image
						Image img = new Image(name);

						// image successfully loaded
						list.add(img);
						loaded = true;
						break;
					} catch (SlickException | RuntimeException e) {
						continue;
					}
				}
				i++;
			} while (loaded);
			if (!list.isEmpty()) {
				this.defaultImages = list.toArray(new Image[list.size()]);
				process();
				return;
			}
		}

		// load single image
		for (String name : getFileNames(filename, type)) {
			try {
				// try loading the image
				Image img = new Image(name);

				// image successfully loaded
				this.defaultImage = img;
				process();
				return;
			} catch (SlickException | RuntimeException e) {
				continue;
			}
		}
		ErrorHandler.error(String.format("Failed to set default image '%s'.", filename), null, false);
	}

	/**
	 * Sets the associated skin image.
	 * If the path does not contain the image, the default image is used.
	 * @return true if a new skin image is loaded, false otherwise
	 */
	public boolean setSkinImage(File dir) {
		if (dir == null)
			return false;

		// destroy the existing images, if any
		destroySkinImage();

		// beatmap skins disabled
		if (Options.isBeatmapSkinIgnored())
			return false;

		// look for multiple skin images
		if (filenameFormat != null) {
			List<Image> list = new ArrayList<Image>();
			int i = 0;
			boolean loaded;
			do {
				loaded = false;
				for (String name : getFileNames(String.format(filenameFormat, i), type)) {
					File file = new File(dir, name);
					if (!file.isFile())
						continue;
					try {
						// try loading the image
						Image img = new Image(file.getAbsolutePath());

						// image successfully loaded
						list.add(img);
						loaded = true;
						break;
					} catch (SlickException | RuntimeException e) {
						continue;
					}
				}
				i++;
			} while (loaded);
			if (!list.isEmpty()) {
				this.skinImages = list.toArray(new Image[list.size()]);
				process();
				return true;
			}
		}

		// look for a skin image
		String errorFile = null;
		for (String name : getFileNames(filename, type)) {
			File file = new File(dir, name);
			if (!file.isFile())
				continue;
			try {
				// try loading the image
				Image img = new Image(file.getAbsolutePath());

				// image successfully loaded
				this.skinImage = img;
				process();
				return true;
			} catch (SlickException | RuntimeException e) {
				errorFile = file.getAbsolutePath();
				continue;
			}
		}
		skinImage = null;
		if (errorFile != null)
			ErrorHandler.error(String.format("Failed to set skin image '%s'.", errorFile), null, false);
		return false;
	}

	/**
	 * Returns whether a skin image is currently loaded.
	 * @return true if skin image exists
	 */
	public boolean hasSkinImage() { return (skinImage != null && !skinImage.isDestroyed()); }

	/**
	 * Returns whether skin images are currently loaded.
	 * @return true if any skin image exists
	 */
	public boolean hasSkinImages() { return (skinImages != null); }

	/**
	 * Destroys the associated skin image(s), if any.
	 */
	public void destroySkinImage() {
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
			ErrorHandler.error(String.format("Failed to destroy skin images for '%s'.", this.name()), e, true);
		}
	}

	/**
	 * Sub-method for image processing actions (via an override).
	 * @param img the image to process
	 * @param w the container width
	 * @param h the container height
	 * @return the processed image
	 */
	protected Image process_sub(Image img, int w, int h) {
		return img;
	}

	/**
	 * Performs individual post-loading actions on the image.
	 */
	private void process() {
		if (skinImages != null) {
			for (int i = 0; i < skinImages.length; i++)
				setImage(process_sub(getImages()[i], containerWidth, containerHeight), i);
		} else if (defaultImages != null && skinImage == null) {
			for (int i = 0; i < defaultImages.length; i++)
				setImage(process_sub(getImages()[i], containerWidth, containerHeight), i);
		} else
			setImage(process_sub(getImage(), containerWidth, containerHeight));
	}
}