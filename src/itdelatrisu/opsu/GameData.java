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

import itdelatrisu.opsu.audio.HitSound;
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.audio.SoundEffect;
import itdelatrisu.opsu.beatmap.Beatmap;
import itdelatrisu.opsu.beatmap.Health;
import itdelatrisu.opsu.beatmap.HitObject;
import itdelatrisu.opsu.downloads.Updater;
import itdelatrisu.opsu.objects.curves.Curve;
import itdelatrisu.opsu.options.Options;
import itdelatrisu.opsu.replay.LifeFrame;
import itdelatrisu.opsu.replay.Replay;
import itdelatrisu.opsu.replay.ReplayFrame;
import itdelatrisu.opsu.ui.Colors;
import itdelatrisu.opsu.ui.Fonts;
import itdelatrisu.opsu.ui.UI;
import itdelatrisu.opsu.ui.animations.AnimationEquation;
import itdelatrisu.opsu.user.UserList;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.LinkedBlockingDeque;

import org.newdawn.slick.Animation;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;

/**
 * Holds game data and renders all related elements.
 */
public class GameData {
	/** Time, in milliseconds, for a hit result to remain existent. */
	public static final int HITRESULT_TIME = 833;

	/** Time, in milliseconds, for a hit result to fade. */
	public static final int HITRESULT_FADE_TIME = 500;

	/** Time, in milliseconds, for a hit circle to fade. */
	public static final int HITCIRCLE_FADE_TIME = 300;

	/** Duration, in milliseconds, of a combo pop effect. */
	private static final int COMBO_POP_TIME = 250;

	/** Time, in milliseconds, for a hit error tick to fade. */
	private static final int HIT_ERROR_FADE_TIME = 5000;

	/** Size of a hit circle at the end of the hit animation. */
	private static final float HITCIRCLE_ANIM_SCALE = 1.38f;

	/** Size of the hit result text at the end of its animation. */
	private static final float HITCIRCLE_TEXT_ANIM_SCALE = 1.28f;

	/** Time, in milliseconds, for the hit result text to bounce. */
	private static final int HITCIRCLE_TEXT_BOUNCE_TIME = 100;

	/** Time, in milliseconds, for the hit result text to fade. */
	private static final int HITCIRCLE_TEXT_FADE_TIME = 833;

	/** Letter grades. */
	public enum Grade {
		NULL (null, null),
		SS  (GameImage.RANKING_SS,  GameImage.RANKING_SS_SMALL),
		SSH (GameImage.RANKING_SSH, GameImage.RANKING_SSH_SMALL), // silver
		S   (GameImage.RANKING_S,   GameImage.RANKING_S_SMALL),
		SH  (GameImage.RANKING_SH,  GameImage.RANKING_SH_SMALL),  // silver
		A   (GameImage.RANKING_A,   GameImage.RANKING_A_SMALL),
		B   (GameImage.RANKING_B,   GameImage.RANKING_B_SMALL),
		C   (GameImage.RANKING_C,   GameImage.RANKING_C_SMALL),
		D   (GameImage.RANKING_D,   GameImage.RANKING_D_SMALL);

		/** GameImages associated with this grade (large and small sizes). */
		private final GameImage large, small;

		/** Large-size image scaled for use in song menu. */
		private Image menuImage;

		/**
		 * Clears all image references.
		 * This does NOT destroy images, so be careful of memory leaks!
		 */
		public static void clearReferences() {
			for (Grade grade : Grade.values())
				grade.menuImage = null;
		}

		/**
		 * Constructor.
		 * @param large the large size image
		 * @param small the small size image
		 */
		Grade(GameImage large, GameImage small) {
			this.large = large;
			this.small = small;
		}

		/**
		 * Returns the large size grade image.
		 */
		public Image getLargeImage() { return large.getImage(); }

		/**
		 * Returns the small size grade image.
		 */
		public Image getSmallImage() { return small.getImage(); }

		/**
		 * Returns the large size grade image scaled for song menu use.
		 */
		public Image getMenuImage() {
			if (menuImage != null)
				return menuImage;

			Image img = getSmallImage();
			if (!small.hasBeatmapSkinImage())  // save default image only
				this.menuImage = img;
			return img;
		}
	}

	/** Hit result types. */
	public static final int
		HIT_MISS             = 0,
		HIT_50               = 1,
		HIT_100              = 2,
		HIT_300              = 3,
		HIT_100K             = 4,   // 100-Katu
		HIT_300K             = 5,   // 300-Katu
		HIT_300G             = 6,   // Geki
		HIT_SLIDER10         = 7,
		HIT_SLIDER30         = 8,
		HIT_MAX              = 9,
		HIT_SLIDER_REPEAT    = 10,
		HIT_ANIMATION_RESULT = 11,
		HIT_SPINNERSPIN      = 12,
		HIT_SPINNERBONUS     = 13,
		HIT_MU               = 14;  // Mu

	/** Random number generator (for score animation). **/
	private static Random random = new Random();

	/** Hit result-related images (indexed by HIT_* constants to HIT_MAX). */
	private Image[] hitResults;

	/** Counts of each hit result so far (indexed by HIT_* constants to HIT_MAX). */
	private int[] hitResultCount;

	/** Total objects including slider hits/ticks (for determining Full Combo status). */
	private int fullObjectCount;

	/** The current combo streak. */
	private int combo;

	/** The max combo streak obtained. */
	private int comboMax;

	/** The current combo pop timer, in milliseconds. */
	private int comboPopTime;

	/**
	 * Hit result types accumulated this streak (bitmask), for Katu/Geki status.
	 * <ul>
	 * <li>&1: 100
	 * <li>&2: 50/Miss
	 * </ul>
	 */
	private byte comboEnd;

	/** Combo burst images. */
	private Image[] comboBurstImages;

	/** Index of the current combo burst image. */
	private int comboBurstIndex;

	/** Alpha level of the current combo burst image (for fade out). */
	private float comboBurstAlpha;

	/** Current x coordinate of the combo burst image (for sliding animation). */
	private float comboBurstX;

	/** Time offsets for obtaining each hit result (indexed by HIT_* constants to HIT_MAX). */
	private int[] hitResultOffset;

	/** List of hit result objects associated with hit objects. */
	private LinkedBlockingDeque<HitObjectResult> hitResultList;

	/**
	 * Class to store hit error information.
	 * @author fluddokt
	 */
	private class HitErrorInfo {
		/** The correct hit time. */
		private final int time;

		/** The coordinates of the hit. */
		@SuppressWarnings("unused")
		private final int x, y;

		/** The difference between the correct and actual hit times. */
		private final int timeDiff;

		/**
		 * Constructor.
		 * @param time the correct hit time
		 * @param x the x coordinate of the hit
		 * @param y the y coordinate of the hit
		 * @param timeDiff the difference between the correct and actual hit times
		 */
		public HitErrorInfo(int time, int x, int y, int timeDiff) {
			this.time = time;
			this.x = x;
			this.y = y;
			this.timeDiff = timeDiff;
		}
	}

	/** List containing recent hit error information. */
	private LinkedBlockingDeque<HitErrorInfo> hitErrorList;

	/** List containing all hit error time differences. */
	private List<Integer> hitErrors;

	/** Performance string containing hit error averages and unstable rate. */
	private String performanceString = null;

	/** Hit object types, used for drawing results. */
	public enum HitObjectType { CIRCLE, SLIDERTICK, SLIDER_FIRST, SLIDER_LAST, SPINNER }

	/** Hit result helper class. */
	private class HitObjectResult {
		/** Object start time. */
		public final int time;

		/** Hit result. */
		public final int result;

		/** Object coordinates. */
		public final float x, y;

		/** Combo color. */
		public final Color color;

		/** The type of the hit object. */
		public final HitObjectType hitResultType;

		/** Slider curve. */
		public final Curve curve;

		/** Whether or not to expand when animating. */
		public final boolean expand;

		/** Whether or not to hide the hit result. */
		public final boolean hideResult;

		/** Alpha level (for fading out). */
		public float alpha = 1f;

		/**
		 * Constructor.
		 * @param time the result's starting track position
		 * @param result the hit result (HIT_* constants)
		 * @param x the center x coordinate
		 * @param y the center y coordinate
		 * @param color the color of the hit object
		 * @param hitResultType the hit object type
		 * @param curve the slider curve (or null if not applicable)
		 * @param expand whether or not the hit result animation should expand (if applicable)
		 * @param hideResult whether or not to hide the hit result (but still show the other animations)
		 */
		public HitObjectResult(int time, int result, float x, float y, Color color,
				HitObjectType hitResultType, Curve curve, boolean expand, boolean hideResult) {
			this.time = time;
			this.result = result;
			this.x = x;
			this.y = y;
			this.color = color;
			this.hitResultType = hitResultType;
			this.curve = curve;
			this.expand = expand;
			this.hideResult = hideResult;
		}
	}

	/** Current game score. */
	private long score;

	/** Displayed game score (for animation, slightly behind score). */
	private long scoreDisplay;

	/** Displayed game score percent (for animation, slightly behind score percent). */
	private float scorePercentDisplay;

	/** Health. */
	private Health health = new Health();

	/** The difficulty multiplier used in the score formula. */
	private int difficultyMultiplier = 2;

	/** Default text symbol images. */
	private Image[] defaultSymbols;

	/** Score text symbol images. */
	private HashMap<Character, Image> scoreSymbols;

	/** Scorebar animation. */
	private Animation scorebarColour;

	/** The associated score data. */
	private ScoreData scoreData;

	/** The associated replay. */
	private Replay replay;

	/** Whether this object is used for gameplay (true) or score viewing (false). */
	private boolean isGameplay;

	/** Container dimensions. */
	private int width, height;

	/**
	 * Constructor for gameplay.
	 * @param width container width
	 * @param height container height
	 */
	public GameData(int width, int height) {
		this.width = width;
		this.height = height;
		this.isGameplay = true;

		clear();
	}

	/**
	 * Constructor for score viewing.
	 * This will initialize all parameters and images needed for the
	 * {@link #drawRankingElements(Graphics, Beatmap, int)} method.
	 * @param s the ScoreData object
	 * @param width container width
	 * @param height container height
	 */
	public GameData(ScoreData s, int width, int height) {
		this.width = width;
		this.height = height;
		this.isGameplay = false;

		this.scoreData = s;
		this.score = s.score;
		this.comboMax = s.combo;
		this.fullObjectCount = (s.perfect) ? s.combo : -1;
		this.hitResultCount = new int[HIT_MAX];
		hitResultCount[HIT_300] = s.hit300;
		hitResultCount[HIT_100] = s.hit100;
		hitResultCount[HIT_50] = s.hit50;
		hitResultCount[HIT_300G] = s.geki;
		hitResultCount[HIT_300K] = 0;
		hitResultCount[HIT_100K] = s.katu;
		hitResultCount[HIT_MISS] = s.miss;
		this.replay = (s.replayString == null) ? null :
			new Replay(new File(Options.getReplayDir(), String.format("%s.osr", s.replayString)));

		loadImages();
	}

	/**
	 * Clears all data and re-initializes object.
	 */
	public void clear() {
		score = 0;
		scoreDisplay = 0;
		scorePercentDisplay = 0f;
		health.reset();
		hitResultCount = new int[HIT_MAX];
		if (hitResultList != null) {
			for (HitObjectResult hitResult : hitResultList) {
				if (hitResult.curve != null)
					hitResult.curve.discardGeometry();
			}
		}
		hitResultList = new LinkedBlockingDeque<HitObjectResult>();
		hitErrorList = new LinkedBlockingDeque<HitErrorInfo>();
		hitErrors = new ArrayList<Integer>();
		performanceString = null;
		fullObjectCount = 0;
		combo = 0;
		comboMax = 0;
		comboPopTime = COMBO_POP_TIME;
		comboEnd = 0;
		comboBurstIndex = -1;
		scoreData = null;
	}

	/**
	 * Loads all game score images.
	 */
	public void loadImages() {
		// gameplay-specific images
		if (isGameplay()) {
			// combo burst images
			if (GameImage.COMBO_BURST.hasBeatmapSkinImages() ||
			    (!GameImage.COMBO_BURST.hasBeatmapSkinImage() && GameImage.COMBO_BURST.getImages() != null))
				comboBurstImages = GameImage.COMBO_BURST.getImages();
			else
				comboBurstImages = new Image[]{ GameImage.COMBO_BURST.getImage() };

			// scorebar-colour animation
			scorebarColour = null;
			if (GameImage.SCOREBAR_COLOUR.getImages() != null)
				scorebarColour = GameImage.SCOREBAR_COLOUR.getAnimation();

			// default symbol images
			defaultSymbols = new Image[10];
			defaultSymbols[0] = GameImage.DEFAULT_0.getImage();
			defaultSymbols[1] = GameImage.DEFAULT_1.getImage();
			defaultSymbols[2] = GameImage.DEFAULT_2.getImage();
			defaultSymbols[3] = GameImage.DEFAULT_3.getImage();
			defaultSymbols[4] = GameImage.DEFAULT_4.getImage();
			defaultSymbols[5] = GameImage.DEFAULT_5.getImage();
			defaultSymbols[6] = GameImage.DEFAULT_6.getImage();
			defaultSymbols[7] = GameImage.DEFAULT_7.getImage();
			defaultSymbols[8] = GameImage.DEFAULT_8.getImage();
			defaultSymbols[9] = GameImage.DEFAULT_9.getImage();
		}

		// score symbol images
		scoreSymbols = new HashMap<Character, Image>(14);
		scoreSymbols.put('0', GameImage.SCORE_0.getImage());
		scoreSymbols.put('1', GameImage.SCORE_1.getImage());
		scoreSymbols.put('2', GameImage.SCORE_2.getImage());
		scoreSymbols.put('3', GameImage.SCORE_3.getImage());
		scoreSymbols.put('4', GameImage.SCORE_4.getImage());
		scoreSymbols.put('5', GameImage.SCORE_5.getImage());
		scoreSymbols.put('6', GameImage.SCORE_6.getImage());
		scoreSymbols.put('7', GameImage.SCORE_7.getImage());
		scoreSymbols.put('8', GameImage.SCORE_8.getImage());
		scoreSymbols.put('9', GameImage.SCORE_9.getImage());
		scoreSymbols.put(',', GameImage.SCORE_COMMA.getImage());
		scoreSymbols.put('.', GameImage.SCORE_DOT.getImage());
		scoreSymbols.put('%', GameImage.SCORE_PERCENT.getImage());
		scoreSymbols.put('x', GameImage.SCORE_X.getImage());

		// hit result images
		hitResults = new Image[HIT_MAX];
		hitResults[HIT_MISS]     = GameImage.HIT_MISS.getImage();
		hitResults[HIT_50]       = GameImage.HIT_50.getImage();
		hitResults[HIT_100]      = GameImage.HIT_100.getImage();
		hitResults[HIT_300]      = GameImage.HIT_300.getImage();
		hitResults[HIT_100K]     = GameImage.HIT_100K.getImage();
		hitResults[HIT_300K]     = GameImage.HIT_300K.getImage();
		hitResults[HIT_300G]     = GameImage.HIT_300G.getImage();
		hitResults[HIT_SLIDER10] = GameImage.HIT_SLIDER10.getImage();
		hitResults[HIT_SLIDER30] = GameImage.HIT_SLIDER30.getImage();
	}

	/**
	 * Returns a default text symbol image for a digit.
	 * @param i the digit [0-9]
	 */
	public Image getDefaultSymbolImage(int i) { return defaultSymbols[i]; }

	/**
	 * Returns a score text symbol image for a character.
	 * @param c the character [0-9,.%x]
	 */
	public Image getScoreSymbolImage(char c) { return scoreSymbols.get(c); }

	/**
	 * Sets the array of hit result offsets.
	 * @param hitResultOffset the time offset array (of size {@link #HIT_MAX})
	 */
	public void setHitResultOffset(int[] hitResultOffset) { this.hitResultOffset = hitResultOffset; }

	/**
	 * Draws a number with defaultSymbols.
	 * @param n the number to draw
	 * @param x the center x coordinate
	 * @param y the center y coordinate
	 * @param scale the scale to apply
	 * @param alpha the alpha level
	 */
	public void drawSymbolNumber(int n, float x, float y, float scale, float alpha) {
		int length = (int) (Math.log10(n) + 1);
		float digitWidth = getDefaultSymbolImage(0).getWidth() * scale;
		float cx = x + ((length - 1) * (digitWidth / 2));

		for (int i = 0; i < length; i++) {
			Image digit = getDefaultSymbolImage(n % 10).getScaledCopy(scale);
			digit.setAlpha(alpha);
			digit.drawCentered(cx, y);
			cx -= digitWidth;
			n /= 10;
		}
	}

	/**
	 * Draws a string of scoreSymbols.
	 * @param str the string to draw
	 * @param x the starting x coordinate
	 * @param y the y coordinate
	 * @param scale the scale to apply
	 * @param alpha the alpha level
	 * @param rightAlign align right (true) or left (false)
	 */
	public void drawSymbolString(String str, float x, float y, float scale, float alpha, boolean rightAlign) {
		char[] c = str.toCharArray();
		float cx = x;
		if (rightAlign) {
			for (int i = c.length - 1; i >= 0; i--) {
				Image digit = getScoreSymbolImage(c[i]);
				if (scale != 1.0f)
					digit = digit.getScaledCopy(scale);
				cx -= digit.getWidth();
				digit.setAlpha(alpha);
				digit.draw(cx, y);
				digit.setAlpha(1f);
			}
		} else {
			for (int i = 0; i < c.length; i++) {
				Image digit = getScoreSymbolImage(c[i]);
				if (scale != 1.0f)
					digit = digit.getScaledCopy(scale);
				digit.setAlpha(alpha);
				digit.draw(cx, y);
				digit.setAlpha(1f);
				cx += digit.getWidth();
			}
		}
	}

	/**
	 * Draws a string of scoreSymbols of fixed width.
	 * @param str the string to draw
	 * @param x the starting x coordinate
	 * @param y the y coordinate
	 * @param scale the scale to apply
	 * @param alpha the alpha level
	 * @param fixedsize the width to use for all symbols
	 * @param rightAlign align right (true) or left (false)
	 */
	public void drawFixedSizeSymbolString(String str, float x, float y, float scale, float alpha, float fixedsize, boolean rightAlign) {
		char[] c = str.toCharArray();
		float cx = x;
		if (rightAlign) {
			for (int i = c.length - 1; i >= 0; i--) {
				Image digit = getScoreSymbolImage(c[i]);
				if (scale != 1.0f)
					digit = digit.getScaledCopy(scale);
				cx -= fixedsize;
				digit.setAlpha(alpha);
				digit.draw(cx + (fixedsize - digit.getWidth()) / 2, y);
				digit.setAlpha(1f);
			}
		} else {
			for (int i = 0; i < c.length; i++) {
				Image digit = getScoreSymbolImage(c[i]);
				if (scale != 1.0f)
					digit = digit.getScaledCopy(scale);
				digit.setAlpha(alpha);
				digit.draw(cx + (fixedsize - digit.getWidth()) / 2, y);
				digit.setAlpha(1f);
				cx += fixedsize;
			}
		}
	}

	/**
	 * Draws game elements:
	 *   scorebar, score, score percentage, map progress circle,
	 *   mod icons, combo count, combo burst, hit error bar, and grade.
	 * @param g the graphics context
	 * @param breakPeriod if true, will not draw scorebar and combo elements, and will draw grade
	 * @param firstObject true if the first hit object's start time has not yet passed
	 * @param alpha the alpha level at which to render all elements (except the hit error bar)
	 */
	@SuppressWarnings("deprecation")
	public void drawGameElements(Graphics g, boolean breakPeriod, boolean firstObject, float alpha) {
		boolean relaxAutoPilot = (GameMod.RELAX.isActive() || GameMod.AUTOPILOT.isActive());
		int margin = (int) (width * 0.008f);
		float uiScale = GameImage.getUIscale();

		// score
		if (!relaxAutoPilot)
			drawFixedSizeSymbolString((scoreDisplay < 100000000) ? String.format("%08d", scoreDisplay) : Long.toString(scoreDisplay),
					width - margin, 0, 1f, alpha, getScoreSymbolImage('0').getWidth() - 2, true);

		// score percentage
		int symbolHeight = getScoreSymbolImage('0').getHeight();
		if (!relaxAutoPilot)
			drawSymbolString(
					String.format((scorePercentDisplay < 10f) ? "0%.2f%%" : "%.2f%%", scorePercentDisplay),
					width - margin, symbolHeight, 0.60f, alpha, true);

		// map progress circle
		Beatmap beatmap = MusicController.getBeatmap();
		int firstObjectTime = beatmap.objects[0].getTime();
		int trackPosition = MusicController.getPosition(true);
		float circleDiameter = symbolHeight * 0.60f;
		int circleX = (int) (width - margin - (  // max width: "100.00%"
				getScoreSymbolImage('1').getWidth() +
				getScoreSymbolImage('0').getWidth() * 4 +
				getScoreSymbolImage('.').getWidth() +
				getScoreSymbolImage('%').getWidth()
		) * 0.60f - circleDiameter);
		if (!relaxAutoPilot) {
			float oldWhiteAlpha = Colors.WHITE_ALPHA.a;
			Colors.WHITE_ALPHA.a = alpha;
			g.setAntiAlias(true);
			g.setLineWidth(2f);
			g.setColor(Colors.WHITE_ALPHA);
			g.drawOval(circleX, symbolHeight, circleDiameter, circleDiameter);
			if (trackPosition > firstObjectTime) {
				// map progress (white)
				float progress = Math.min((float) (trackPosition - firstObjectTime) / (beatmap.endTime - firstObjectTime), 1f);
				g.fillArc(circleX, symbolHeight, circleDiameter, circleDiameter, -90, -90 + (int) (360f * progress));
			} else {
				// lead-in time (yellow)
				float progress = (float) trackPosition / firstObjectTime;
				float oldYellowAlpha = Colors.YELLOW_ALPHA.a;
				Colors.YELLOW_ALPHA.a *= alpha;
				g.setColor(Colors.YELLOW_ALPHA);
				g.fillArc(circleX, symbolHeight, circleDiameter, circleDiameter, -90 + (int) (360f * progress), -90);
				Colors.YELLOW_ALPHA.a = oldYellowAlpha;
			}
			g.setAntiAlias(false);
			Colors.WHITE_ALPHA.a = oldWhiteAlpha;
		}

		// mod icons
		if ((firstObject && trackPosition < firstObjectTime) || GameMod.AUTO.isActive()) {
			int modWidth = GameMod.AUTO.getImage().getWidth();
			float modX = (width * 0.98f) - modWidth;
			int modCount = 0;
			for (GameMod mod : GameMod.VALUES_REVERSED) {
				if (mod.isActive()) {
					mod.getImage().setAlpha(alpha);
					mod.getImage().draw(
							modX - (modCount * (modWidth / 2f)),
							symbolHeight + circleDiameter + 10
					);
					mod.getImage().setAlpha(1f);
					modCount++;
				}
			}
		}

		// hit error bar
		if (Options.isHitErrorBarEnabled() && !hitErrorList.isEmpty()) {
			// fade out with last tick
			float hitErrorAlpha = 1f;
			Color white = new Color(Color.white);
			if (trackPosition - hitErrorList.getFirst().time > HIT_ERROR_FADE_TIME * 0.9f)
				hitErrorAlpha = (HIT_ERROR_FADE_TIME - (trackPosition - hitErrorList.getFirst().time)) / (HIT_ERROR_FADE_TIME * 0.1f);

			// draw bar
			float hitErrorX = width / uiScale / 2;
			float hitErrorY = height / uiScale - margin - 10;
			float barY = (hitErrorY - 3) * uiScale, barHeight = 6 * uiScale;
			float tickY = (hitErrorY - 10) * uiScale, tickHeight = 20 * uiScale;
			float oldAlphaBlack = Colors.BLACK_ALPHA.a;
			Colors.BLACK_ALPHA.a = hitErrorAlpha;
			g.setColor(Colors.BLACK_ALPHA);
			g.fillRect((hitErrorX - 3 - hitResultOffset[HIT_50]) * uiScale, tickY,
					(hitResultOffset[HIT_50] * 2) * uiScale, tickHeight);
			Colors.BLACK_ALPHA.a = oldAlphaBlack;
			Colors.LIGHT_ORANGE.a = hitErrorAlpha;
			g.setColor(Colors.LIGHT_ORANGE);
			g.fillRect((hitErrorX - 3 - hitResultOffset[HIT_50]) * uiScale, barY,
					(hitResultOffset[HIT_50] * 2) * uiScale, barHeight);
			Colors.LIGHT_ORANGE.a = 1f;
			Colors.LIGHT_GREEN.a = hitErrorAlpha;
			g.setColor(Colors.LIGHT_GREEN);
			g.fillRect((hitErrorX - 3 - hitResultOffset[HIT_100]) * uiScale, barY,
					(hitResultOffset[HIT_100] * 2) * uiScale, barHeight);
			Colors.LIGHT_GREEN.a = 1f;
			Colors.LIGHT_BLUE.a = hitErrorAlpha;
			g.setColor(Colors.LIGHT_BLUE);
			g.fillRect((hitErrorX - 3 - hitResultOffset[HIT_300]) * uiScale, barY,
					(hitResultOffset[HIT_300] * 2) * uiScale, barHeight);
			Colors.LIGHT_BLUE.a = 1f;
			white.a = hitErrorAlpha;
			g.setColor(white);
			g.fillRect((hitErrorX - 1.5f) * uiScale, tickY, 3 * uiScale, tickHeight);

			// draw ticks
			float tickWidth = 2 * uiScale;
			for (HitErrorInfo info : hitErrorList) {
				int time = info.time;
				float tickAlpha = 1 - ((float) (trackPosition - time) / HIT_ERROR_FADE_TIME);
				white.a = tickAlpha * hitErrorAlpha;
				g.setColor(white);
				g.fillRect((hitErrorX + info.timeDiff - 1) * uiScale, tickY, tickWidth, tickHeight);
			}
		}

		if (!breakPeriod && !relaxAutoPilot) {
			// scorebar
			float healthRatio = health.getHealthDisplay() / 100f;
			if (firstObject) {  // gradually move ki before map begins
				if (firstObjectTime >= 1500 && trackPosition < firstObjectTime - 500)
					healthRatio = (float) trackPosition / (firstObjectTime - 500);
			}
			Image scorebar = GameImage.SCOREBAR_BG.getImage();
			Image colour;
			if (scorebarColour != null) {
				scorebarColour.updateNoDraw();  // TODO deprecated method
				colour = scorebarColour.getCurrentFrame();
			} else
				colour = GameImage.SCOREBAR_COLOUR.getImage();
			float colourX = 4 * uiScale, colourY = 15 * uiScale;
			Image colourCropped = colour.getSubImage(0, 0, (int) (645 * uiScale * healthRatio), colour.getHeight());

			scorebar.setAlpha(alpha);
			scorebar.draw(0, 0);
			scorebar.setAlpha(1f);
			colourCropped.setAlpha(alpha);
			colourCropped.draw(colourX, colourY);
			colourCropped.setAlpha(1f);

			Image ki = null;
			if (health.getHealth() >= 50f)
				ki = GameImage.SCOREBAR_KI.getImage();
			else if (health.getHealth() >= 25f)
				ki = GameImage.SCOREBAR_KI_DANGER.getImage();
			else
				ki = GameImage.SCOREBAR_KI_DANGER2.getImage();
			if (comboPopTime < COMBO_POP_TIME)
				ki = ki.getScaledCopy(1f + (0.45f * (1f - (float) comboPopTime / COMBO_POP_TIME)));
			ki.setAlpha(alpha);
			ki.drawCentered(colourX + colourCropped.getWidth(), colourY);
			ki.setAlpha(1f);

			// combo burst
			if (comboBurstIndex != -1 && comboBurstAlpha > 0f) {
				Image comboBurst = comboBurstImages[comboBurstIndex];
				comboBurst.setAlpha(comboBurstAlpha);
				comboBurstImages[comboBurstIndex].draw(comboBurstX, height - comboBurst.getHeight());
			}

			// combo count
			if (combo > 0) {
				float comboPop = 1 - ((float) comboPopTime / COMBO_POP_TIME);
				float comboPopBack  = 1 + comboPop * 0.45f;
				float comboPopFront = 1 + comboPop * 0.08f;
				String comboString = String.format("%dx", combo);
				if (comboPopTime != COMBO_POP_TIME)
					drawSymbolString(comboString, margin, height - margin - (symbolHeight * comboPopBack), comboPopBack, 0.5f * alpha, false);
				drawSymbolString(comboString, margin, height - margin - (symbolHeight * comboPopFront), comboPopFront, alpha, false);
			}
		} else if (!relaxAutoPilot) {
			// grade
			Grade grade = getGrade();
			if (grade != Grade.NULL) {
				Image gradeImage = grade.getSmallImage();
				float gradeScale = symbolHeight * 0.75f / gradeImage.getHeight();
				gradeImage = gradeImage.getScaledCopy(gradeScale);
				gradeImage.setAlpha(alpha);
				gradeImage.draw(circleX - gradeImage.getWidth(), symbolHeight);
			}
		}
	}

	/**
	 * Draws ranking elements: score, results, ranking, game mods.
	 * @param g the graphics context
	 * @param beatmap the beatmap
	 * @param time the animation time
	 */
	public void drawRankingElements(Graphics g, Beatmap beatmap, int time) {
		// TODO Version 2 skins
		float symbolTextScale = 1.15f;
		float uiScale = GameImage.getUIscale();
		Image zeroImg = getScoreSymbolImage('0');

		// animation timings
		int animationTime = 400, offsetTime = 150, gradeAnimationTime = 1000, whiteAnimationTime = 2200;
		int rankStart = 50, comboStart = 1800, perfectStart = 2700, gradeStart = 2800, whiteStart = 3800;
		int graphEnd = whiteStart + 100;

		// ranking panel
		GameImage.RANKING_PANEL.getImage().draw(0, (int) (102 * uiScale));

		// score
		float scoreTextScale = 1.33f;
		int scoreWidth = Math.max(8, (int) Math.log10(score) + 1);
		int correctDigitsWidth = Math.min(scoreWidth, (int) ((double) time / whiteStart * scoreWidth));
		long correctDigitsFactor = (long) Math.pow(10, scoreWidth - correctDigitsWidth);
		long displayScore = score / correctDigitsFactor * correctDigitsFactor + Math.abs(random.nextLong()) % correctDigitsFactor;
		drawFixedSizeSymbolString(
			(displayScore < 100000000) ? String.format("%08d", displayScore) : Long.toString(displayScore),
			180 * uiScale, 120 * uiScale,
			scoreTextScale, 1f, zeroImg.getWidth() * scoreTextScale, false
		);

		// result counts
		if (time >= rankStart) {
			float rankResultScale = 0.5f;
			float resultHitInitialX = 64, resultHitInitialY = 256;
			float resultInitialX = 128;
			float resultInitialY = resultHitInitialY - (zeroImg.getHeight() * symbolTextScale) / 2f;
			float resultOffsetX = 320, resultOffsetY = 96;
			int[] rankDrawOrder = { HIT_300, HIT_100, HIT_50, HIT_300G, HIT_100K, HIT_MISS };
			int[] rankResultOrder = {
				hitResultCount[HIT_300], hitResultCount[HIT_100],
				hitResultCount[HIT_50], hitResultCount[HIT_300G],
				hitResultCount[HIT_100K] + hitResultCount[HIT_300K], hitResultCount[HIT_MISS]
			};
			for (int i = 0; i < rankDrawOrder.length; i++) {
				float offsetX = i < 3 ? 0 : resultOffsetX;
				float offsetY = (resultOffsetY * (i % 3));
				int startTime = rankStart + i * animationTime;
				if (time >= startTime) {
					float t = Math.min((float) (time - startTime) / animationTime, 1f);
					float tp = AnimationEquation.OUT_CUBIC.calc(t);
					float scale = 2f - tp;
					float alpha = tp;
					Image img = hitResults[rankDrawOrder[i]].getScaledCopy(rankResultScale * scale);
					img.setAlpha(alpha);
					img.drawCentered(
						(resultHitInitialX + offsetX) * uiScale, (resultHitInitialY + offsetY) * uiScale
					);
				}
				if (time >= startTime + offsetTime) {
					float t = Math.min((float) (time - startTime) / animationTime, 1f);
					float tp = AnimationEquation.OUT_CUBIC.calc(t);
					float alpha = tp;
					offsetX += -64f * (1f - tp);
					drawSymbolString(String.format("%dx", rankResultOrder[i]),
						(resultInitialX + offsetX) * uiScale, (resultInitialY + offsetY) * uiScale, symbolTextScale, alpha, false
					);
				}
			}
		}

		// combo and accuracy
		float accuracyX = 291;
		float textY = 480;
		float numbersY = textY + 48;
		if (time >= comboStart) {
			float t = Math.min((float) (time - comboStart) / animationTime, 1f);
			float alpha = t;
			Image img = GameImage.RANKING_MAXCOMBO.getImage();
			img.setAlpha(alpha);
			img.draw(8 * uiScale, textY * uiScale);
			img.setAlpha(1f);
		}
		if (time >= comboStart + offsetTime) {
			float t = Math.min((float) (time - (comboStart + offsetTime)) / animationTime, 1f);
			float tp = AnimationEquation.OUT_CUBIC.calc(t);
			float alpha = tp;
			float offsetX = -15f * (1f - tp);
			drawSymbolString(
				String.format("%dx", comboMax),
				(24 + offsetX) * uiScale, numbersY * uiScale, symbolTextScale, alpha, false
			);
		}
		if (time >= comboStart + animationTime) {
			float t = Math.min((float) (time - (comboStart + animationTime)) / animationTime, 1f);
			float alpha = t;
			Image img = GameImage.RANKING_ACCURACY.getImage();
			img.setAlpha(alpha);
			img.draw(accuracyX * uiScale, textY * uiScale);
			img.setAlpha(1f);
		}
		if (time >= comboStart + animationTime + offsetTime) {
			float t = Math.min((float) (time - (comboStart + animationTime + offsetTime)) / animationTime, 1f);
			float tp = AnimationEquation.OUT_CUBIC.calc(t);
			float alpha = tp;
			float offsetX = -62f * (1f - tp);
			drawSymbolString(
				String.format("%02.2f%%", getScorePercent()),
				(accuracyX + 20 + offsetX) * uiScale, numbersY * uiScale, symbolTextScale, alpha, false
			);
		}

		// graph
		float graphX = 416 * uiScale;
		float graphY = 688 * uiScale;
		Image graphImg = GameImage.RANKING_GRAPH.getImage();
		graphImg.drawCentered(graphX, graphY);
		if (replay != null && replay.lifeFrames != null && replay.lifeFrames.length > 0) {
			float margin = 8 * uiScale;
			float cx = graphX - graphImg.getWidth() / 2f + margin;
			float cy = graphY - graphImg.getHeight() / 2f + margin;
			float graphWidth = graphImg.getWidth() - margin * 2f;
			float graphHeight = graphImg.getHeight() - margin * 2f;
			g.setClip((int) cx, (int) cy, (int) (graphWidth * ((float) time / graphEnd)), (int) graphHeight);
			float lastXt = cx;
			float lastYt = cy + graphHeight * (1f - replay.lifeFrames[0].getHealth());
			g.setLineWidth(2 * uiScale);
			if (replay.lifeFrames.length == 1) {
				g.setColor(replay.lifeFrames[0].getHealth() >= 0.5f ? Colors.GREEN : Color.red);
				g.drawLine(lastXt, lastYt, lastXt + graphWidth, lastYt);
			} else {
				int minTime = replay.lifeFrames[0].getTime();
				int maxTime = replay.lifeFrames[replay.lifeFrames.length - 1].getTime();
				int totalTime = maxTime - minTime;
				Color lastColor = null;
				for (int i = 1; i < replay.lifeFrames.length; i++) {
					float xt = cx + graphWidth * ((float) (replay.lifeFrames[i].getTime() - minTime) / totalTime);
					float yt = cy + graphHeight * (1f - replay.lifeFrames[i].getHealth());
					Color color = replay.lifeFrames[i].getHealth() >= 0.5f ? Colors.GREEN : Color.red;
					if (color != lastColor)
						g.setColor(color);
					g.drawLine(lastXt, lastYt, xt, yt);
					lastXt = xt;
					lastYt = yt;
					lastColor = color;
				}
			}
			g.clearClip();
		}

		// full combo
		if (time >= perfectStart) {
			float t = Math.min((float) (time - perfectStart) / animationTime, 1f);
			float tp = AnimationEquation.OUT_CUBIC.calc(t);
			float scale = 1.1f - 0.1f * tp;
			float alpha = tp;
			if (comboMax == fullObjectCount) {
				Image img = GameImage.RANKING_PERFECT.getImage().getScaledCopy(scale);
				img.setAlpha(alpha);
				img.drawCentered(graphX, graphY);
			}
		}

		// grade
		if (time >= gradeStart) {
			float t = Math.min((float) (time - gradeStart) / gradeAnimationTime, 1f);
			float tp = AnimationEquation.IN_CUBIC.calc(t);
			float scale = 1.5f - 0.5f * tp;
			float alpha = tp;
			Grade grade = getGrade();
			if (grade != Grade.NULL) {
				Image img = grade.getLargeImage();
				float x = width - 8 * uiScale - img.getWidth() / 2f;
				float y = 100 * uiScale + img.getHeight() / 2f;
				img = img.getScaledCopy(scale);
				img.setAlpha(alpha);
				img.drawCentered(x, y);
			}
		}

		// header
		Image rankingTitle = GameImage.RANKING_TITLE.getImage();
		g.setColor(Colors.BLACK_ALPHA);
		g.fillRect(0, 0, width, 96 * uiScale);
		rankingTitle.draw(width - 24 * uiScale - rankingTitle.getWidth(), 0);
		float marginX = width * 0.01f, marginY = height * 0.002f;
		Fonts.LARGE.drawString(marginX, marginY,
			String.format("%s - %s [%s]", beatmap.getArtist(), beatmap.getTitle(), beatmap.version), Color.white);
		Fonts.MEDIUM.drawString(marginX, marginY + Fonts.LARGE.getLineHeight() - 3,
			String.format("Beatmap by %s", beatmap.creator), Color.white);
		String player = (scoreData.playerName == null) ? "" : String.format(" by %s", scoreData.playerName);
		Fonts.MEDIUM.drawString(marginX, marginY + Fonts.LARGE.getLineHeight() + Fonts.MEDIUM.getLineHeight() - 5,
			String.format("Played%s on %s.", player, scoreData.getTimeString()), Color.white);

		// mod icons
		if (scoreData.mods != 0) {
			int modWidth = GameMod.AUTO.getImage().getWidth();
			int modHeight = GameMod.AUTO.getImage().getHeight();
			float modX = (width * 0.98f) - modWidth;
			int modCount = 0;
			for (GameMod mod : GameMod.VALUES_REVERSED) {
				if ((scoreData.mods & mod.getBit()) > 0) {
					if (time >= animationTime * modCount) {
						float t = Math.min((float) (time - animationTime * modCount) / animationTime, 1f);
						float tp = AnimationEquation.OUT_CUBIC.calc(t);
						float scale = 2f - tp;
						float alpha = tp;
						Image img = mod.getImage().getScaledCopy(scale);
						img.setAlpha(alpha);
						img.drawCentered(
							modX - (modCount * (modWidth / 2f)) + modWidth / 2f,
							height / 2f + modHeight / 2f
						);
						modCount++;
					}
				}
			}
		}

		// white flash
		if (time >= whiteStart && time < whiteStart + whiteAnimationTime) {
			float t = (float) (time - whiteStart) / whiteAnimationTime;
			float alpha = 0.75f - 0.75f * AnimationEquation.OUT_CUBIC.calc(t);
			float oldWhiteAlpha = Colors.WHITE_FADE.a;
			Colors.WHITE_FADE.a = alpha;
			g.setColor(Colors.WHITE_FADE);
			g.fillRect(0, 0, width, height);
			Colors.WHITE_FADE.a = oldWhiteAlpha;
		}
	}

	/**
	 * Draws stored hit results and removes them from the list as necessary.
	 * @param trackPosition the current track position (in ms)
	 * @param over true if drawing elements over hit objects, false for under
	 */
	public void drawHitResults(int trackPosition, boolean over) {
		Iterator<HitObjectResult> iter = hitResultList.iterator();
		while (iter.hasNext()) {
			HitObjectResult hitResult = iter.next();
			if (hitResult.time + HITRESULT_TIME > trackPosition) {
				// results drawn OVER hit objects
				if (over) {
					// spinner
					if (hitResult.hitResultType == HitObjectType.SPINNER && hitResult.result != HIT_MISS &&
					    Options.getSkin().getVersion() == 1) {
						Image spinnerOsu = GameImage.SPINNER_OSU.getImage();
						spinnerOsu.setAlpha(hitResult.alpha);
						spinnerOsu.drawCentered(width / 2, height / 4);
						spinnerOsu.setAlpha(1f);
					}

					// hit lighting
					else if (Options.isHitLightingEnabled() && !hitResult.hideResult && hitResult.result != HIT_MISS &&
						hitResult.result != HIT_SLIDER30 && hitResult.result != HIT_SLIDER10) {
						// TODO: add particle system
						Image lighting = GameImage.LIGHTING.getImage();
						lighting.setAlpha(hitResult.alpha);
						lighting.drawCentered(hitResult.x, hitResult.y, hitResult.color);
					}

					// hit result
					if (!hitResult.hideResult && (
					    hitResult.hitResultType == HitObjectType.CIRCLE ||
					    hitResult.hitResultType == HitObjectType.SLIDER_FIRST ||
					    hitResult.hitResultType == HitObjectType.SLIDER_LAST ||
					    hitResult.hitResultType == HitObjectType.SPINNER ||
					    (hitResult.hitResultType == HitObjectType.SLIDERTICK && Options.getSkin().getVersion() == 1))) {
						float scaleProgress = AnimationEquation.IN_OUT_BOUNCE.calc(
							(float) Utils.clamp(trackPosition - hitResult.time, 0, HITCIRCLE_TEXT_BOUNCE_TIME) / HITCIRCLE_TEXT_BOUNCE_TIME);
						float scale = 1f + (HITCIRCLE_TEXT_ANIM_SCALE - 1f) * scaleProgress;
						float fadeProgress = AnimationEquation.OUT_CUBIC.calc(
							(float) Utils.clamp((trackPosition - hitResult.time) - HITCIRCLE_FADE_TIME, 0, HITCIRCLE_TEXT_FADE_TIME) / HITCIRCLE_TEXT_FADE_TIME);
						float alpha = 1f - fadeProgress;
						Image scaledHitResult = hitResults[hitResult.result].getScaledCopy(scale);
						scaledHitResult.setAlpha(alpha);
						scaledHitResult.drawCentered(hitResult.x, hitResult.y);
					}

					hitResult.alpha = 1 - ((float) (trackPosition - hitResult.time) / HITRESULT_FADE_TIME);
				}

				// results drawn UNDER hit objects
				else {
					// hit animations (only draw when the "Hidden" mod is not enabled)
					if (!GameMod.HIDDEN.isActive())
						drawHitAnimations(hitResult, trackPosition);
				}
			} else {
				if (hitResult.curve != null)
					hitResult.curve.discardGeometry();
				iter.remove();
			}
		}
	}

	/**
	 * Draw the hit animations:
	 *   circles, reverse arrows, slider curves (fading out and/or expanding).
	 * @param hitResult the hit result
	 * @param trackPosition the current track position (in ms)
	 */
	private void drawHitAnimations(HitObjectResult hitResult, int trackPosition) {
		// fade out slider curve
		if (hitResult.result != HIT_SLIDER_REPEAT && hitResult.curve != null &&
		    !(Options.isExperimentalSliderStyle() && Options.isExperimentalSliderShrinking())) {
			float progress = AnimationEquation.OUT_CUBIC.calc(
				(float) Utils.clamp(trackPosition - hitResult.time, 0, HITCIRCLE_FADE_TIME) / HITCIRCLE_FADE_TIME);
			float alpha = 1f - progress;
			float oldWhiteAlpha = Colors.WHITE_FADE.a;
			float oldColorAlpha = hitResult.color.a;
			Colors.WHITE_FADE.a = hitResult.color.a = alpha;
			if (Options.isExperimentalSliderStyle())
				hitResult.curve.draw(hitResult.color, Options.isExperimentalSliderMerging() ? 1 : 0, hitResult.curve.getCurvePoints().length);
			else
				hitResult.curve.draw(hitResult.color);
			Colors.WHITE_FADE.a = oldWhiteAlpha;
			hitResult.color.a = oldColorAlpha;
		}

		// miss, don't draw an animation
		if (hitResult.result == HIT_MISS)
			return;

		// not a circle?
		if (hitResult.hitResultType != HitObjectType.CIRCLE &&
		    hitResult.hitResultType != HitObjectType.SLIDER_FIRST &&
		    hitResult.hitResultType != HitObjectType.SLIDER_LAST)
			return;

		// slider follow circle
		if (hitResult.expand && hitResult.result != HIT_SLIDER_REPEAT && (
		    hitResult.hitResultType == HitObjectType.SLIDER_FIRST ||
		    hitResult.hitResultType == HitObjectType.SLIDER_LAST)) {
			float progress = AnimationEquation.OUT_CUBIC.calc(
				(float) Utils.clamp(trackPosition - hitResult.time, 0, HITCIRCLE_FADE_TIME) / HITCIRCLE_FADE_TIME);
			float scale = 1f - 0.2f * progress;
			float alpha = 1f - progress;
			Image fc = GameImage.SLIDER_FOLLOWCIRCLE.getImage().getScaledCopy(scale);
			fc.setAlpha(alpha);
			fc.drawCentered(hitResult.x, hitResult.y);
		}

		// hide end circles?
		if (Options.isExperimentalSliderStyle() && !Options.isExperimentalSliderCapsDrawn() &&
		    hitResult.result != HIT_SLIDER_REPEAT && hitResult.curve != null)
			return;

		// hit circles
		float progress = AnimationEquation.OUT_CUBIC.calc(
			(float) Utils.clamp(trackPosition - hitResult.time, 0, HITCIRCLE_FADE_TIME) / HITCIRCLE_FADE_TIME);
		float scale = (!hitResult.expand) ? 1f : 1f + (HITCIRCLE_ANIM_SCALE - 1f) * progress;
		float alpha = 1f - progress;
		Image scaledHitCircle = GameImage.HITCIRCLE.getImage().getScaledCopy(scale);
		Image scaledHitCircleOverlay = GameImage.HITCIRCLE_OVERLAY.getImage().getScaledCopy(scale);
		scaledHitCircle.setAlpha(alpha);
		scaledHitCircleOverlay.setAlpha(alpha);
		scaledHitCircle.drawCentered(hitResult.x, hitResult.y, hitResult.color);
		scaledHitCircleOverlay.drawCentered(hitResult.x, hitResult.y);

		// repeat arrow
		if (hitResult.result == HIT_SLIDER_REPEAT) {
			Image scaledRepeat = GameImage.REVERSEARROW.getImage().getScaledCopy(scale);
			scaledRepeat.setAlpha(alpha);
			float ang;
			if (hitResult.hitResultType == HitObjectType.SLIDER_FIRST) {
				ang = hitResult.curve.getStartAngle();
			} else {
				ang = hitResult.curve.getEndAngle();
			}
			scaledRepeat.rotate(ang);
			scaledRepeat.drawCentered(hitResult.x, hitResult.y, hitResult.color);
		}
	}

	/**
	 * Returns the current health percentage.
	 */
	public float getHealthPercent() { return health.getHealth(); }

	/**
	 * Sets the health modifiers.
	 * @param hpDrainRate the HP drain rate
	 * @param hpMultiplierNormal the normal HP multiplier
	 * @param hpMultiplierComboEnd the combo-end HP multiplier
	 */
	public void setHealthModifiers(float hpDrainRate, float hpMultiplierNormal, float hpMultiplierComboEnd) {
		health.setModifiers(hpDrainRate, hpMultiplierNormal, hpMultiplierComboEnd);
	}

	/**
	 * Returns false if health is zero.
	 * If "No Fail" or "Auto" mods are active, this will always return true.
	 */
	public boolean isAlive() {
		return (health.getHealth() > 0f || GameMod.NO_FAIL.isActive() || GameMod.AUTO.isActive() ||
		        GameMod.RELAX.isActive() || GameMod.AUTOPILOT.isActive());
	}

	/**
	 * Changes health by a raw value.
	 * @param value the health value
	 */
	public void changeHealth(float value) { health.changeHealth(value); }

	/**
	 * Returns the raw score.
	 */
	public long getScore() { return score; }

	/**
	 * Changes score by a raw value (not affected by other modifiers).
	 * @param value the score value
	 */
	public void changeScore(int value) { score += value; }

	/**
	 * Returns the raw score percentage.
	 * @param hit300 the number of 300s
	 * @param hit100 the number of 100s
	 * @param hit50 the number of 50s
	 * @param miss the number of misses
	 * @return the score percentage
	 */
	public static float getScorePercent(int hit300, int hit100, int hit50, int miss) {
		float percent = 0;
		int objectCount = hit300 + hit100 + hit50 + miss;
		if (objectCount > 0)
			percent = (hit300 * 300 + hit100 * 100 + hit50 * 50) / (objectCount * 300f) * 100f;
		return percent;
	}

	/**
	 * Returns the raw score percentage.
	 */
	public float getScorePercent() {
		return getScorePercent(
			hitResultCount[HIT_300], hitResultCount[HIT_100],
			hitResultCount[HIT_50], hitResultCount[HIT_MISS]
		);
	}

	/**
	 * Returns letter grade based on score data,
	 * or Grade.NULL if no objects have been processed.
	 * @param hit300 the number of 300s
	 * @param hit100 the number of 100s
	 * @param hit50 the number of 50s
	 * @param miss the number of misses
	 * @param silver whether or not a silver SS/S should be awarded (if applicable)
	 * @return the current Grade
	 */
	public static Grade getGrade(int hit300, int hit100, int hit50, int miss, boolean silver) {
		int objectCount = hit300 + hit100 + hit50 + miss;
		if (objectCount < 1)  // avoid division by zero
			return Grade.NULL;

		float percent = getScorePercent(hit300, hit100, hit50, miss);
		float hit300ratio = hit300 * 100f / objectCount;
		float hit50ratio = hit50 * 100f / objectCount;
		boolean noMiss = (miss == 0);
		if (percent >= 100f)
			return (silver) ? Grade.SSH : Grade.SS;
		else if (hit300ratio >= 90f && hit50ratio < 1.0f && noMiss)
			return (silver) ? Grade.SH : Grade.S;
		else if ((hit300ratio >= 80f && noMiss) || hit300ratio >= 90f)
			return Grade.A;
		else if ((hit300ratio >= 70f && noMiss) || hit300ratio >= 80f)
			return Grade.B;
		else if (hit300ratio >= 60f)
			return Grade.C;
		else
			return Grade.D;
	}

	/**
	 * Returns letter grade based on score data,
	 * or {@code Grade.NULL} if no objects have been processed.
	 */
	private Grade getGrade() {
		boolean silver = (scoreData == null) ?
				(GameMod.HIDDEN.isActive() || GameMod.FLASHLIGHT.isActive()) :
				(scoreData.mods & (GameMod.HIDDEN.getBit() | GameMod.FLASHLIGHT.getBit())) != 0;
		return getGrade(
			hitResultCount[HIT_300], hitResultCount[HIT_100],
			hitResultCount[HIT_50], hitResultCount[HIT_MISS],
			silver
		);
	}

	/**
	 * Updates displayed elements based on a delta value.
	 * @param delta the delta interval since the last call
	 */
	public void updateDisplays(int delta) {
		// score display
		if (scoreDisplay < score) {
			scoreDisplay += (score - scoreDisplay) * delta / 50 + 1;
			if (scoreDisplay > score)
				scoreDisplay = score;
		}

		// score percent display
		float scorePercent = getScorePercent();
		if (scorePercentDisplay != scorePercent) {
			if (scorePercentDisplay < scorePercent) {
				scorePercentDisplay += (scorePercent - scorePercentDisplay) * delta / 50f + 0.01f;
				if (scorePercentDisplay > scorePercent)
					scorePercentDisplay = scorePercent;
			} else {
				scorePercentDisplay -= (scorePercentDisplay - scorePercent) * delta / 50f + 0.01f;
				if (scorePercentDisplay < scorePercent)
					scorePercentDisplay = scorePercent;
			}
		}

		// health display
		health.update(delta);

		// combo burst
		if (comboBurstIndex > -1 && Options.isComboBurstEnabled()) {
			int leftX  = 0;
			int rightX = width - comboBurstImages[comboBurstIndex].getWidth();
			if (comboBurstX < leftX) {
				comboBurstX += (delta / 2f) * GameImage.getUIscale();
				if (comboBurstX > leftX)
					comboBurstX = leftX;
			} else if (comboBurstX > rightX) {
				comboBurstX -= (delta / 2f) * GameImage.getUIscale();
				if (comboBurstX < rightX)
					comboBurstX = rightX;
			} else if (comboBurstAlpha > 0f) {
				comboBurstAlpha -= (delta / 1200f);
				if (comboBurstAlpha < 0f)
					comboBurstAlpha = 0f;
			}
		}

		// combo pop
		comboPopTime += delta;
		if (comboPopTime > COMBO_POP_TIME)
			comboPopTime = COMBO_POP_TIME;

		// hit error bar
		if (Options.isHitErrorBarEnabled()) {
			int trackPosition = MusicController.getPosition(true);
			Iterator<HitErrorInfo> iter = hitErrorList.iterator();
			while (iter.hasNext()) {
				HitErrorInfo info = iter.next();
				if (Math.abs(info.timeDiff) >= hitResultOffset[GameData.HIT_50] ||
				    info.time + HIT_ERROR_FADE_TIME <= trackPosition)
					iter.remove();
			}
		}
	}

	/**
	 * Updates displayed ranking elements based on a delta value.
	 * @param delta the delta interval since the last call
	 * @param mouseX the mouse x coordinate
	 * @param mouseY the mouse y coordinate
	 */
	public void updateRankingDisplays(int delta, int mouseX, int mouseY) {
		// graph tooltip
		Image graphImg = GameImage.RANKING_GRAPH.getImage();
		float graphX = 416 * GameImage.getUIscale();
		float graphY = 688 * GameImage.getUIscale();
		if (isGameplay &&
		    mouseX >= graphX - graphImg.getWidth() / 2f && mouseX <= graphX + graphImg.getWidth() / 2f &&
		    mouseY >= graphY - graphImg.getHeight() / 2f && mouseY <= graphY + graphImg.getHeight() / 2f) {
			if (performanceString == null)
				performanceString = getPerformanceString(hitErrors);
			UI.updateTooltip(delta, performanceString, true);
		}
	}

	/**
	 * Returns the current combo streak.
	 */
	public int getComboStreak() { return combo; }

	/**
	 * Increases the combo streak by one.
	 */
	private void incrementComboStreak() {
		combo++;
		comboPopTime = 0;
		if (combo > comboMax)
			comboMax = combo;

		// combo bursts (at 30, 60, 100+50x)
		if (Options.isComboBurstEnabled() &&
			(combo == 30 || combo == 60 || (combo >= 100 && combo % 50 == 0))) {
			if (Options.getSkin().isComboBurstRandom())
				comboBurstIndex = (int) (Math.random() * comboBurstImages.length);
			else {
				if (combo == 30)
					comboBurstIndex = 0;
				else
					comboBurstIndex = (comboBurstIndex + 1) % comboBurstImages.length;
			}
			comboBurstAlpha = 0.8f;
			if ((comboBurstIndex % 2) == 0)
				comboBurstX = width;
			else
				comboBurstX = comboBurstImages[0].getWidth() * -1;
		}
	}

	/**
	 * Resets the combo streak to zero.
	 */
	private void resetComboStreak() {
		if (combo > 20 && !(GameMod.RELAX.isActive() || GameMod.AUTOPILOT.isActive())) {
			if (!Options.isGameplaySoundDisabled())
				SoundController.playSound(SoundEffect.COMBOBREAK);
		}
		combo = 0;
		if (GameMod.SUDDEN_DEATH.isActive())
			health.setHealth(0f);
	}

	/**
	 * Handles a slider repeat result (animation only: arrow).
	 * @param time the repeat time
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param color the arrow color
	 * @param curve the slider curve
	 * @param type the hit object type
	 */
	public void sendSliderRepeatResult(int time, float x, float y, Color color, Curve curve, HitObjectType type) {
		hitResultList.add(new HitObjectResult(time, HIT_SLIDER_REPEAT, x, y, color, type, curve, true, true));
	}

	/**
	 * Handles a slider start result (animation only: initial circle).
	 * @param time the hit time
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param color the slider color
	 * @param expand whether or not the hit result animation should expand
	 */
	public void sendSliderStartResult(int time, float x, float y, Color color, boolean expand) {
		hitResultList.add(new HitObjectResult(time, HIT_ANIMATION_RESULT, x, y, color, HitObjectType.CIRCLE, null, expand, true));
	}

	/**
	 * Handles a slider tick result.
	 * @param time the tick start time
	 * @param result the hit result (HIT_* constants)
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param hitObject the hit object
	 * @param repeat the current repeat number
	 */
	public void sendSliderTickResult(int time, int result, float x, float y, HitObject hitObject, int repeat) {
		int hitValue = 0;
		switch (result) {
		case HIT_SLIDER30:
			hitValue = 30;
			SoundController.playHitSound(
					hitObject.getEdgeHitSoundType(repeat),
					hitObject.getSampleSet(repeat),
					hitObject.getAdditionSampleSet(repeat));
			break;
		case HIT_SLIDER10:
			hitValue = 10;
			SoundController.playHitSound(HitSound.SLIDERTICK);
			break;
		case HIT_MISS:
			resetComboStreak();
			break;
		default:
			return;
		}

		if (hitValue > 0) {
			// calculate score and increment combo streak
			score += hitValue;
			incrementComboStreak();
			health.changeHealthForHit(result);

			if (!Options.isPerfectHitBurstEnabled())
				;  // hide perfect hit results
			else
				hitResultList.add(new HitObjectResult(time, result, x, y, null, HitObjectType.SLIDERTICK, null, false, false));
		}
		fullObjectCount++;
	}

	/**
	 * Handles a spinner spin result.
	 * @param result the hit result (HIT_* constants)
	 */
	public void sendSpinnerSpinResult(int result) {
		int hitValue = 0;
		switch (result) {
		case HIT_SPINNERSPIN:
			hitValue = 100;
			if (!Options.isGameplaySoundDisabled())
				SoundController.playSound(SoundEffect.SPINNERSPIN);
			break;
		case HIT_SPINNERBONUS:
			hitValue = 1100;
			if (!Options.isGameplaySoundDisabled())
				SoundController.playSound(SoundEffect.SPINNERBONUS);
			break;
		default:
			return;
		}

		score += hitValue;
		health.changeHealthForHit(result);
	}

	/**
	 * Returns the score for a hit based on the following score formula:
	 * <p>
	 * Score = Hit Value + Hit Value * (Combo * Difficulty * Mod) / 25
	 * <ul>
	 * <li><strong>Hit Value:</strong> hit result (50, 100, 300), slider ticks, spinner bonus
	 * <li><strong>Combo:</strong> combo before this hit - 1 (minimum 0)
	 * <li><strong>Difficulty:</strong> the difficulty setting (see {@link #calculateDifficultyMultiplier(float, float, float)})
	 * <li><strong>Mod:</strong> mod multipliers
	 * </ul>
	 * @param hitValue the hit value
	 * @param hitObject the hit object
	 * @return the score value
	 * @see <a href="https://osu.ppy.sh/wiki/Score">https://osu.ppy.sh/wiki/Score</a>
	 */
	private int getScoreForHit(int hitValue, HitObject hitObject) {
		int comboMultiplier = Math.max(combo - 1, 0);
		if (hitObject.isSlider())
			comboMultiplier++;
		return (hitValue + (int)(hitValue * (comboMultiplier * difficultyMultiplier * GameMod.getScoreMultiplier()) / 25));
	}

	/**
	 * Computes and stores the difficulty multiplier used in the score formula.
	 * @param drainRate the raw HP drain rate value
	 * @param circleSize the raw circle size value
	 * @param overallDifficulty the raw overall difficulty value
	 * @see <a href="https://osu.ppy.sh/wiki/Score#How_to_calculate_the_Difficulty_multiplier">https://osu.ppy.sh/wiki/Score#How_to_calculate_the_Difficulty_multiplier</a>
	 */
	public void calculateDifficultyMultiplier(float drainRate, float circleSize, float overallDifficulty) {
		// TODO: find the actual formula (osu!wiki is wrong)
		// seems to be based on hit object density? (total objects / time)
		// 924 3x1/4 beat notes 0.14stars
		// 924 3x1beat 0.28stars
		// 912 3x1beat with 1 extra note 10 sec away 0.29stars

		float sum = drainRate + circleSize + overallDifficulty;  // typically 2~27
		if (sum <= 5f)
			difficultyMultiplier = 2;
		else if (sum <= 12f)
			difficultyMultiplier = 3;
		else if (sum <= 17f)
			difficultyMultiplier = 4;
		else if (sum <= 24f)
			difficultyMultiplier = 5;
		else //if (sum <= 30f)
			difficultyMultiplier = 6;

		//float multiplier = ((circleSize + overallDifficulty + drainRate) / 6) + 1.5f;
		//difficultyMultiplier = (int) multiplier;
	}

	/**
	 * Handles a hit result and performs all associated calculations.
	 * @param time the object start time
	 * @param result the base hit result (HIT_* constants)
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param color the combo color
	 * @param end true if this is the last hit object in the combo
	 * @param hitObject the hit object
	 * @param hitResultType the type of hit object for the result
	 * @param repeat the current repeat number (for sliders, or 0 otherwise)
	 * @param noIncrementCombo if the combo should not be incremented by this result
	 * @return the actual hit result (HIT_* constants)
	 */
	private int handleHitResult(int time, int result, float x, float y, Color color, boolean end,
			HitObject hitObject, HitObjectType hitResultType, int repeat, boolean noIncrementCombo) {
		// update health, score, and combo streak based on hit result
		int hitValue = 0;
		switch (result) {
		case HIT_300:
			hitValue = 300;
			break;
		case HIT_100:
			hitValue = 100;
			comboEnd |= 1;
			break;
		case HIT_50:
			hitValue = 50;
			comboEnd |= 2;
			break;
		case HIT_MISS:
			hitValue = 0;
			comboEnd |= 2;
			resetComboStreak();
			break;
		default:
			return HIT_MISS;
		}
		if (hitValue > 0) {
			SoundController.playHitSound(
					hitObject.getEdgeHitSoundType(repeat),
					hitObject.getSampleSet(repeat),
					hitObject.getAdditionSampleSet(repeat));

			// calculate score and increment combo streak
			changeScore(getScoreForHit(hitValue, hitObject));
			if (!noIncrementCombo)
				incrementComboStreak();
		}
		health.changeHealthForHit(result);
		hitResultCount[result]++;
		fullObjectCount++;

		// last element in combo: check for Geki/Katu
		if (end) {
			if (comboEnd == 0) {
				result = HIT_300G;
				health.changeHealthForHit(HIT_300G);
				hitResultCount[result]++;
			} else if ((comboEnd & 2) == 0) {
				if (result == HIT_100) {
					result = HIT_100K;
					health.changeHealthForHit(HIT_100K);
					hitResultCount[result]++;
				} else if (result == HIT_300) {
					result = HIT_300K;
					health.changeHealthForHit(HIT_300K);
					hitResultCount[result]++;
				}
			} else if (hitValue > 0)
				health.changeHealthForHit(HIT_MU);
			comboEnd = 0;
		}

		return result;
	}

	/**
	 * Handles a hit result.
	 * @param time the object start time
	 * @param result the hit result (HIT_* constants)
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param color the combo color
	 * @param end true if this is the last hit object in the combo
	 * @param hitObject the hit object
	 * @param hitResultType the type of hit object for the result
	 * @param expand whether or not the hit result animation should expand (if applicable)
	 * @param repeat the current repeat number (for sliders, or 0 otherwise)
	 * @param curve the slider curve (or null if not applicable)
	 * @param sliderHeldToEnd whether or not the slider was held to the end (if applicable)
	 */
	public void sendHitResult(
		int time, int result, float x, float y, Color color,
		boolean end, HitObject hitObject, HitObjectType hitResultType,
		boolean expand, int repeat, Curve curve, boolean sliderHeldToEnd
	) {
		int hitResult = handleHitResult(time, result, x, y, color, end, hitObject, hitResultType, repeat, (curve != null && !sliderHeldToEnd));

		if (hitResult == HIT_MISS && (GameMod.RELAX.isActive() || GameMod.AUTOPILOT.isActive()))
			return;  // "relax" and "autopilot" mods: hide misses

		boolean hideResult = (hitResult == HIT_300 || hitResult == HIT_300G || hitResult == HIT_300K) && !Options.isPerfectHitBurstEnabled();
		hitResultList.add(new HitObjectResult(time, hitResult, x, y, color, hitResultType, curve, expand, hideResult));
	}

	/**
	 * Returns a ScoreData object encapsulating all game data.
	 * If score data already exists, the existing object will be returned
	 * (i.e. this will not overwrite existing data).
	 * @param beatmap the beatmap
	 * @return the ScoreData object
	 * @see #getCurrentScoreData(Beatmap, boolean)
	 */
	public ScoreData getScoreData(Beatmap beatmap) {
		if (scoreData == null)
			scoreData = getCurrentScoreData(beatmap, false);
		return scoreData;
	}

	/**
	 * Returns a ScoreData object encapsulating all current game data.
	 * @param beatmap the beatmap
	 * @param slidingScore if true, use the display score (might not be actual score)
	 * @return the ScoreData object
	 * @see #getScoreData(Beatmap)
	 */
	public ScoreData getCurrentScoreData(Beatmap beatmap, boolean slidingScore) {
		ScoreData sd = new ScoreData();
		sd.timestamp = System.currentTimeMillis() / 1000L;
		sd.MID = beatmap.beatmapID;
		sd.MSID = beatmap.beatmapSetID;
		sd.title = beatmap.title;
		sd.artist = beatmap.artist;
		sd.creator = beatmap.creator;
		sd.version = beatmap.version;
		sd.hit300 = hitResultCount[HIT_300];
		sd.hit100 = hitResultCount[HIT_100];
		sd.hit50 = hitResultCount[HIT_50];
		sd.geki = hitResultCount[HIT_300G];
		sd.katu = hitResultCount[HIT_300K] + hitResultCount[HIT_100K];
		sd.miss = hitResultCount[HIT_MISS];
		sd.score = slidingScore ? scoreDisplay : score;
		sd.combo = comboMax;
		sd.perfect = (comboMax == fullObjectCount);
		sd.mods = GameMod.getModState();
		sd.replayString = (replay == null) ? null : replay.getReplayFilename();
		sd.playerName = GameMod.AUTO.isActive() ?
			UserList.AUTO_USER_NAME : UserList.get().getCurrentUser().getName();
		return sd;
	}

	/**
	 * Returns a Replay object encapsulating all game data.
	 * If a replay already exists and frames is null, the existing object will be returned.
	 * @param frames the replay frames
	 * @param lifeFrames the life frames
	 * @param beatmap the associated beatmap
	 * @return the Replay object, or null if none exists and frames is null
	 */
	public Replay getReplay(ReplayFrame[] frames, LifeFrame[] lifeFrames, Beatmap beatmap) {
		if (replay != null && frames == null)
			return replay;

		if (frames == null)
			return null;

		replay = new Replay();
		replay.mode = Beatmap.MODE_OSU;
		replay.version = Updater.get().getBuildDate();
		replay.beatmapHash = (beatmap == null) ? "" : beatmap.md5Hash;
		replay.playerName = UserList.get().getCurrentUser().getName();
		replay.replayHash = Long.toString(System.currentTimeMillis());  // TODO
		replay.hit300 = (short) hitResultCount[HIT_300];
		replay.hit100 = (short) hitResultCount[HIT_100];
		replay.hit50 = (short) hitResultCount[HIT_50];
		replay.geki = (short) hitResultCount[HIT_300G];
		replay.katu = (short) (hitResultCount[HIT_300K] + hitResultCount[HIT_100K]);
		replay.miss = (short) hitResultCount[HIT_MISS];
		replay.score = (int) score;
		replay.combo = (short) comboMax;
		replay.perfect = (comboMax == fullObjectCount);
		replay.mods = GameMod.getModState();
		replay.lifeFrames = lifeFrames;
		replay.timestamp = new Date();
		replay.frames = frames;
		replay.seed = 0;  // TODO
		replay.loaded = true;

		return replay;
	}

	/**
	 * Sets the replay object.
	 * @param replay the replay
	 */
	public void setReplay(Replay replay) { this.replay = replay; }

	/**
	 * Returns whether or not this object is used for gameplay.
	 * @return true if gameplay, false if score viewing
	 */
	public boolean isGameplay() { return isGameplay; }

	/**
	 * Sets whether or not this object is used for gameplay.
	 * @param gameplay true if gameplay, false if score viewing
	 */
	public void setGameplay(boolean gameplay) { this.isGameplay = gameplay; }

	/**
	 * Adds the hit into the list of hit error information.
	 * @param time the correct hit time
	 * @param x the x coordinate of the hit
	 * @param y the y coordinate of the hit
	 * @param timeDiff the difference between the correct and actual hit times
	 */
	public void addHitError(int time, int x, int y, int timeDiff) {
		hitErrorList.addFirst(new HitErrorInfo(time, x, y, timeDiff));
		hitErrors.add(timeDiff);
	}

	/**
	 * Computes the error values and unstable rate for the map.
	 * @see <a href="https://osu.ppy.sh/wiki/Accuracy#Performance_Graph">https://osu.ppy.sh/wiki/Accuracy#Performance_Graph</a>
	 */
	private String getPerformanceString(List<Integer> errors) {
		int earlyCount = 0, lateCount = 0;
		int earlySum = 0, lateSum = 0;
		for (int diff : errors) {
			if (diff < 0) {
				earlyCount++;
				earlySum += diff;
			} else if (diff > 0) {
				lateCount++;
				lateSum += diff;
			}
		}
		float hitErrorEarly = (earlyCount > 0) ? (float) earlySum / earlyCount : 0f;
		float hitErrorLate = (lateCount > 0) ? (float) lateSum / lateCount : 0f;
		float unstableRate = (!errors.isEmpty()) ? (float) (Utils.standardDeviation(errors) * 10) : 0f;
		return String.format(
			"Accuracy:\nError: %.2fms - %.2fms avg\nUnstable Rate: %.2f",
			hitErrorEarly, hitErrorLate, unstableRate
		);
	}
}
