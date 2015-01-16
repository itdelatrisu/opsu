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

import itdelatrisu.opsu.states.Options;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;

/**
 * Game images.
 */
public enum GameImage {
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
			return img.getScaledCopy((h / 3f) / img.getHeight());
		}
	},
	COUNTDOWN_2 ("count2", "png") {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy((h / 3f) / img.getHeight());
		}
	},
	COUNTDOWN_1 ("count1", "png") {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy((h / 3f) / img.getHeight());
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
	SLIDER_FOLLOWCIRCLE ("sliderfollowcircle", "png"),
	REVERSEARROW ("reversearrow", "png"),
	SLIDER_TICK ("sliderscorepoint", "png"),

	// Spinner
	SPINNER_CIRCLE ("spinner-circle", "png"),
	SPINNER_APPROACHCIRCLE ("spinner-approachcircle", "png"),
	SPINNER_METRE ("spinner-metre", "png"),
	SPINNER_SPIN ("spinner-spin", "png"),
	SPINNER_CLEAR ("spinner-clear", "png"),
	SPINNER_OSU ("spinner-osu", "png"),

	// Game Score
	SCOREBAR_BG ("scorebar-bg", "png") {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy(w / 2, img.getHeight());
		}
	},
	SCOREBAR_COLOUR ("scorebar-colour", "png") {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy(w / 2, img.getHeight());
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

	// Non-Game Components
	MENU_BACK ("menu-back", "png", false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy((h * 0.1f) / img.getHeight());
		}
	},
	MENU_BUTTON_BG ("menu-button-background", "png", false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy(w / 2, h / 6);
		}
	},
	MENU_TAB ("selection-tab", "png", false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy((h * 0.033f) / img.getHeight());
		}
	},
	MENU_SEARCH ("search", "png", false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy(Utils.FONT_BOLD.getLineHeight() * 2f / img.getHeight());
		}
	},
	MENU_OPTIONS ("options", "png", false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy(Utils.FONT_BOLD.getLineHeight() * 2f / img.getHeight());
		}
	},
	MENU_MUSICNOTE ("music-note", "png", false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			int r = (int) (Utils.FONT_LARGE.getLineHeight() * 0.75f + Utils.FONT_DEFAULT.getLineHeight());
			return img.getScaledCopy(r, r);
		}
	},
	MENU_LOADER ("loader", "png", false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			int r = (int) (Utils.FONT_LARGE.getLineHeight() * 0.75f + Utils.FONT_DEFAULT.getLineHeight());
			return img.getScaledCopy(r / 48f);
		}
	},
	MENU_BG ("menu-background", "png|jpg", false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			img.setAlpha(0.9f);
			return img.getScaledCopy(w, h);
		}
	},
	MENU_LOGO ("logo", "png", false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy((h / 1.2f) / img.getHeight());
		}
	},
	MENU_PlAY ("menu-play", "png", false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy(MENU_LOGO.getImage().getWidth() * 0.83f / img.getWidth());
		}
	},
	MENU_EXIT ("menu-exit", "png", false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy(MENU_LOGO.getImage().getWidth() * 0.66f / img.getWidth());
		}
	},
	MENU_BUTTON_MID ("button-middle", "png", false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy(w / 2, img.getHeight());
		}
	},
	MENU_BUTTON_LEFT ("button-left", "png", false),
	MENU_BUTTON_RIGHT ("button-right", "png", false),

	MUSIC_PLAY ("music-play", "png", false),
	MUSIC_PAUSE ("music-pause", "png", false),
	MUSIC_NEXT ("music-next", "png", false),
	MUSIC_PREVIOUS ("music-previous", "png", false),

	RANKING_RETRY ("ranking-retry", "png", false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy((h * 0.15f) / img.getHeight());
		}
	},
	RANKING_EXIT ("ranking-back", "png", false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy((h * 0.15f) / img.getHeight());
		}
	},

	REPOSITORY ("repo", "png", false) {
		@Override
		protected Image process_sub(Image img, int w, int h) {
			return img.getScaledCopy((h / 17f) / img.getHeight());
		}
	};

	/**
	 * Image file types.
	 */
	private static final byte
		IMG_PNG = 1,
		IMG_JPG = 2;

	/**
	 * The file name.
	 */
	private String filename;

	/**
	 * Image file type.
	 */
	private byte type;

	/**
	 * Whether or not the image is related to gameplay.
	 * Game images are skinnable per beatmap, while other images are not.
	 */
	private boolean gameImage;

	/**
	 * The default image.
	 */
	private Image defaultImage;

	/**
	 * The beatmap skin image (optional, temporary).
	 */
	private Image skinImage;

	/**
	 * Container dimensions.
	 */
	private static int containerWidth, containerHeight;

	/**
	 * Whether a skin image has been loaded.
	 */
	private static boolean skinImageLoaded = false;

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
	 * Destroys all skin images, if any have been loaded.
	 */
	public static void destroySkinImages() {
		if (skinImageLoaded) {
			for (GameImage img : GameImage.values()) {
				if (img.isGameImage())
					img.destroySkinImage();
			}
			skinImageLoaded = false;
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
	 * Constructor.
	 * @param filename the image file name
	 * @param type the file types (separated by '|')
	 */
	GameImage(String filename, String type) {
		this.filename = filename;
		this.type = getType(type);
		this.gameImage = true;
	}

	/**
	 * Constructor.
	 * @param filename the image file name
	 * @param type the file types (separated by '|')
	 * @param gameImage whether or not the image is related to gameplay
	 */
	GameImage(String filename, String type, boolean gameImage) {
		this.filename = filename;
		this.type = getType(type);
		this.gameImage = gameImage;
	}

	/**
	 * Returns whether or not the image is related to gameplay.
	 * @return true if game image
	 */
	public boolean isGameImage() { return gameImage; }

	/**
	 * Returns the image associated with this resource.
	 * The skin image takes priority over the default image.
	 */
	public Image getImage() {
		if (defaultImage == null)
			setDefaultImage();
		return (skinImage != null) ? skinImage : defaultImage;
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
	 * Returns a list of possible filenames (with extensions).
	 * @return filename list
	 */
	private List<String> getFileNames() {
		List<String> list = new ArrayList<String>(2);
		if ((type & IMG_PNG) != 0)
			list.add(String.format("%s.png", filename));
		if ((type & IMG_JPG) != 0)
			list.add(String.format("%s.jpg", filename));
		return list;
	}

	/**
	 * Sets the default image for this resource.
	 * If the default image has already been loaded, this will do nothing.
	 */
	public void setDefaultImage() {
		if (defaultImage != null)
			return;
		for (String name : getFileNames()) {
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

		// destroy the existing image, if any
		destroySkinImage();

		// beatmap skins disabled
		if (Options.isBeatmapSkinIgnored())
			return false;

		// look for a skin image
		String errorFile = null;
		for (String name : getFileNames()) {
			File file = new File(dir, name);
			if (!file.isFile())
				continue;
			try {
				// try loading the image
				Image img = new Image(file.getAbsolutePath());

				// image successfully loaded
				this.skinImage = img;
				process();
				skinImageLoaded = true;
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
	 * Destroys the associated skin image, if any.
	 */
	private void destroySkinImage() {
		if (skinImage == null)
			return;
		try {
			if (!skinImage.isDestroyed())
				skinImage.destroy();
			skinImage = null;
		} catch (SlickException e) {
			ErrorHandler.error(String.format("Failed to destroy skin image for '%s'.", this.name()), e, true);
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
		setImage(process_sub(getImage(), containerWidth, containerHeight));
	}
}