/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014 Jeffrey Han
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

import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.util.Log;

/**
 * Game images.
 */
public enum GameImage {
	// Game
	SECTION_PASS ("section-pass.png"),
	SECTION_FAIL ("section-fail.png"),
	WARNINGARROW ("play-warningarrow.png"),
	SKIP ("play-skip.png"),
	COUNTDOWN_READY ("ready.png"),
	COUNTDOWN_3 ("count3.png"),
	COUNTDOWN_2 ("count2.png"),
	COUNTDOWN_1 ("count1.png"),
	COUNTDOWN_GO ("go.png"),
	HITCIRCLE_SELECT ("hitcircleselect.png"),
	UNRANKED ("play-unranked.png"),

	// Game Pause/Fail
	PAUSE_CONTINUE ("pause-continue.png"),
	PAUSE_RETRY ("pause-retry.png"),
	PAUSE_BACK ("pause-back.png"),
	PAUSE_OVERLAY ("pause-overlay.png"),
	FAIL_BACKGROUND ("fail-background.png"),

	// Circle
	HITCIRCLE ("hitcircle.png"),
	HITCIRCLE_OVERLAY ("hitcircleoverlay.png"),
	APPROACHCIRCLE ("approachcircle.png"),

	// Slider
	SLIDER_FOLLOWCIRCLE ("sliderfollowcircle.png"),
	REVERSEARROW ("reversearrow.png"),
	SLIDER_TICK ("sliderscorepoint.png"),

	// Spinner
	SPINNER_CIRCLE ("spinner-circle.png"),
	SPINNER_APPROACHCIRCLE ("spinner-approachcircle.png"),
	SPINNER_METRE ("spinner-metre.png"),
	SPINNER_SPIN ("spinner-spin.png"),
	SPINNER_CLEAR ("spinner-clear.png"),
	SPINNER_OSU ("spinner-osu.png"),

	// Game Score
	SCOREBAR_BG ("scorebar-bg.png"),
	SCOREBAR_COLOUR ("scorebar-colour.png"),
	SCOREBAR_KI ("scorebar-ki.png"),
	SCOREBAR_KI_DANGER ("scorebar-kidanger.png"),
	SCOREBAR_KI_DANGER2 ("scorebar-kidanger2.png"),
	HIT_MISS ("hit0.png"),
	HIT_50 ("hit50.png"),
	HIT_100 ("hit100.png"),
	HIT_300 ("hit300.png"),
	HIT_100K ("hit100k.png"),
	HIT_300K ("hit300k.png"),
	HIT_300G ("hit300g.png"),
	HIT_SLIDER10 ("sliderpoint10.png"),
	HIT_SLIDER30 ("sliderpoint30.png"),
	RANKING_SS ("ranking-X.png"),
	RANKING_SS_SMALL ("ranking-X-small.png"),
	RANKING_SSH ("ranking-XH.png"),
	RANKING_SSH_SMALL ("ranking-XH-small.png"),
	RANKING_S ("ranking-S.png"),
	RANKING_S_SMALL ("ranking-S-small.png"),
	RANKING_SH ("ranking-SH.png"),
	RANKING_SH_SMALL ("ranking-SH-small.png"),
	RANKING_A ("ranking-A.png"),
	RANKING_A_SMALL ("ranking-A-small.png"),
	RANKING_B ("ranking-B.png"),
	RANKING_B_SMALL ("ranking-B-small.png"),
	RANKING_C ("ranking-C.png"),
	RANKING_C_SMALL ("ranking-C-small.png"),
	RANKING_D ("ranking-D.png"),
	RANKING_D_SMALL ("ranking-D-small.png"),
	RANKING_PANEL ("ranking-panel.png"),
	RANKING_PERFECT ("ranking-perfect.png"),
	RANKING_TITLE ("ranking-title.png"),
	RANKING_MAXCOMBO ("ranking-maxcombo.png"),
	RANKING_ACCURACY ("ranking-accuracy.png"),
	DEFAULT_0 ("default-0.png"),
	DEFAULT_1 ("default-1.png"),
	DEFAULT_2 ("default-2.png"),
	DEFAULT_3 ("default-3.png"),
	DEFAULT_4 ("default-4.png"),
	DEFAULT_5 ("default-5.png"),
	DEFAULT_6 ("default-6.png"),
	DEFAULT_7 ("default-7.png"),
	DEFAULT_8 ("default-8.png"),
	DEFAULT_9 ("default-9.png"),
	SCORE_0 ("score-0.png"),
	SCORE_1 ("score-1.png"),
	SCORE_2 ("score-2.png"),
	SCORE_3 ("score-3.png"),
	SCORE_4 ("score-4.png"),
	SCORE_5 ("score-5.png"),
	SCORE_6 ("score-6.png"),
	SCORE_7 ("score-7.png"),
	SCORE_8 ("score-8.png"),
	SCORE_9 ("score-9.png"),
	SCORE_COMMA ("score-comma.png"),
	SCORE_DOT ("score-dot.png"),
	SCORE_PERCENT ("score-percent.png"),
	SCORE_X ("score-x.png");

	/**
	 * The file name.
	 */
	private String filename;

	/**
	 * The default image.
	 */
	private Image defaultImage;

	/**
	 * The beatmap skin image (optional, temporary).
	 */
	private Image skinImage;

	/**
	 * Whether or not the default image has been scaled.
	 */
	private boolean scaled;

	/**
	 * Constructor.
	 */
	GameImage(String filename) {
		this.filename = filename;
	}

	/**
	 * Returns the image associated with this resource.
	 * The skin image takes priority over the default image.
	 */
	public Image getImage() {
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
	 * Sets the default image for this resource.
	 */
	public void setDefaultImage() {
		try {
			if (defaultImage != null && !defaultImage.isDestroyed())
				defaultImage.destroy();

			defaultImage = new Image(filename);
			scaled = false;
		} catch (SlickException e) {
			Log.error(String.format("Failed to set default image '%s'.", filename), e);
		}
	}

	/**
	 * Sets the associated skin image.
	 * If the path does not contain the image, the default image is used.
	 */
	public void setSkinImage(File dir) {
		try {
			// destroy the existing image, if any
			if (skinImage != null && !skinImage.isDestroyed())
				skinImage.destroy();

			// set a new image
			File file = new File(dir, filename);
			if (file.isFile() && !Options.isBeatmapSkinIgnored())
				skinImage = new Image(file.getAbsolutePath());
			else
				skinImage = null;
		} catch (SlickException e) {
			Log.error(String.format("Failed to set skin image '%s'.", filename), e);
		}
	}

	/**
	 * Returns whether or not the image has been scaled.
	 */
	public boolean isScaled() { return (skinImage != null) ? false : scaled; }

	/**
	 * Sets the scaled status of the image.
	 */
	public void setScaled() { if (skinImage == null) this.scaled = true; }
}