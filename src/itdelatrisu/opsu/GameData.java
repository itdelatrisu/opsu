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

import itdelatrisu.opsu.audio.HitSound;
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.audio.SoundEffect;
import itdelatrisu.opsu.beatmap.Beatmap;
import itdelatrisu.opsu.beatmap.HitObject;
import itdelatrisu.opsu.downloads.Updater;
import itdelatrisu.opsu.objects.curves.Curve;
import itdelatrisu.opsu.replay.Replay;
import itdelatrisu.opsu.replay.ReplayFrame;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingDeque;

import org.newdawn.slick.Animation;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;

/**
 * Holds game data and renders all related elements.
 */
public class GameData {
	/** Delta multiplier for steady HP drain. */
	public static final float HP_DRAIN_MULTIPLIER = 1 / 200f;

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
		private GameImage large, small;

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
			if (!small.hasSkinImage())  // save default image only
				this.menuImage = img;
			return img;
		}
	}

	/** Hit result types. */
	public static final int
		HIT_MISS     = 0,
		HIT_50       = 1,
		HIT_100      = 2,
		HIT_300      = 3,
		HIT_100K     = 4,   // 100-Katu
		HIT_300K     = 5,   // 300-Katu
		HIT_300G     = 6,   // Geki
		HIT_SLIDER10 = 7,
		HIT_SLIDER30 = 8,
		HIT_MAX      = 9;   // not a hit result

	/** Hit result-related images (indexed by HIT_* constants). */
	private Image[] hitResults;

	/** Counts of each hit result so far. */
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

	/** Time offsets for obtaining each hit result (indexed by HIT_* constants). */
	private int[] hitResultOffset;

	/** List of hit result objects associated with hit objects. */
	private LinkedBlockingDeque<HitObjectResult> hitResultList;

	/**
	 * Class to store hit error information.
	 * @author fluddokt
	 */
	private class HitErrorInfo {
		/** The correct hit time. */
		private int time;

		/** The coordinates of the hit. */
		@SuppressWarnings("unused")
		private int x, y;

		/** The difference between the correct and actual hit times. */
		private int timeDiff;

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

	/** Hit object types, used for drawing results. */
	public enum HitObjectType { CIRCLE, SLIDERTICK, SLIDER_FIRST, SLIDER_LAST, SPINNER }

	/** Hit result helper class. */
	private class HitObjectResult {
		/** Object start time. */
		public int time;

		/** Hit result. */
		public int result;

		/** Object coordinates. */
		public float x, y;

		/** Combo color. */
		public Color color;

		/** The type of the hit object. */
		public HitObjectType hitResultType;

		/** Alpha level (for fading out). */
		public float alpha = 1f;

		/** Slider curve. */
		public Curve curve;

		/** Whether or not to expand when animating. */
		public boolean expand;

		/**
		 * Constructor.
		 * @param time the result's starting track position
		 * @param result the hit result (HIT_* constants)
		 * @param x the center x coordinate
		 * @param y the center y coordinate
		 * @param color the color of the hit object
		 * @param curve the slider curve (or null if not applicable)
		 * @param expand whether or not the hit result animation should expand (if applicable)
		 */
		public HitObjectResult(int time, int result, float x, float y, Color color,
				HitObjectType hitResultType, Curve curve, boolean expand) {
			this.time = time;
			this.result = result;
			this.x = x;
			this.y = y;
			this.color = color;
			this.hitResultType = hitResultType;
			this.curve = curve;
			this.expand = expand;
		}
	}

	/** Current game score. */
	private long score;

	/** Displayed game score (for animation, slightly behind score). */
	private long scoreDisplay;

	/** Displayed game score percent (for animation, slightly behind score percent). */
	private float scorePercentDisplay;

	/** Current health bar percentage. */
	private float health;

	/** Displayed health (for animation, slightly behind health). */
	private float healthDisplay;

	/** Beatmap HPDrainRate value. (0:easy ~ 10:hard) */
	private float drainRate = 5f;

	/** Beatmap OverallDifficulty value. (0:easy ~ 10:hard) */
	private float difficulty = 5f;

	/** Beatmap ApproachRate value. (0:easy ~ 10:hard) */
	private float approachRate = 5f;
	
	/** Beatmap CircleSize value. (2:big ~ 7:small) */
	private float circleSize = 5f;
	
	private int difficultyMultiplier = -1;

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
	private boolean gameplay;

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
		this.gameplay = true;

		clear();
	}

	/**
	 * Constructor for score viewing.
	 * This will initialize all parameters and images needed for the
	 * {@link #drawRankingElements(Graphics, Beatmap)} method.
	 * @param s the ScoreData object
	 * @param width container width
	 * @param height container height
	 */
	public GameData(ScoreData s, int width, int height) {
		this.width = width;
		this.height = height;
		this.gameplay = false;

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
		health = 100f;
		healthDisplay = 100f;
		hitResultCount = new int[HIT_MAX];
		if (hitResultList != null) {
			for (HitObjectResult hitResult : hitResultList) {
				if (hitResult.curve != null)
					hitResult.curve.discardCache();
			}
		}
		hitResultList = new LinkedBlockingDeque<HitObjectResult>();
		hitErrorList = new LinkedBlockingDeque<HitErrorInfo>();
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
			if (GameImage.COMBO_BURST.hasSkinImages() ||
			    (!GameImage.COMBO_BURST.hasSkinImage() && GameImage.COMBO_BURST.getImages() != null))
				comboBurstImages = GameImage.COMBO_BURST.getImages();
			else
				comboBurstImages = new Image[]{ GameImage.COMBO_BURST.getImage() };

			// scorebar-colour animation
			Image[] scorebar = GameImage.SCOREBAR_COLOUR.getImages();
			scorebarColour = (scorebar != null) ? new Animation(scorebar, 60) : null;

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
	 * Sets the health drain rate.
	 * @param drainRate the new drain rate [0-10]
	 */
	public void setDrainRate(float drainRate) { this.drainRate = drainRate; }

	/**
	 * Returns the health drain rate.
	 */
	public float getDrainRate() { return drainRate; }

	/**
	 * Sets the overall difficulty level.
	 * @param difficulty the new difficulty [0-10]
	 */
	public void setDifficulty(float difficulty) { this.difficulty = difficulty; }

	/**
	 * Returns the overall difficulty level.
	 */
	public float getDifficulty() { return difficulty; }

	/**
	 * Sets or returns the approach rate.
	 */
	public void setApproachRate(float approachRate) { this.approachRate = approachRate; }
	public float getApproachRate() { return approachRate; }
	
	/**
	 * Sets or returns the approach rate.
	 */
	public void setCircleSize(float circleSize) { this.circleSize = circleSize; }
	public float getCircleSize() { return circleSize; }
	
	/**
	 * Sets the array of hit result offsets.
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
	 * @param fixedsize the width to use for all symbols
	 * @param rightAlign align right (true) or left (false)
	 */
	public void drawFixedSizeSymbolString(String str, float x, float y, float scale, float fixedsize, boolean rightAlign) {
		char[] c = str.toCharArray();
		float cx = x;
		if (rightAlign) {
			for (int i = c.length - 1; i >= 0; i--) {
				Image digit = getScoreSymbolImage(c[i]);
				if (scale != 1.0f)
					digit = digit.getScaledCopy(scale);
				cx -= fixedsize;
				digit.draw(cx + (fixedsize - digit.getWidth()) / 2, y);
			}
		} else {
			for (int i = 0; i < c.length; i++) {
				Image digit = getScoreSymbolImage(c[i]);
				if (scale != 1.0f)
					digit = digit.getScaledCopy(scale);
				digit.draw(cx + (fixedsize - digit.getWidth()) / 2, y);
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
	 */
	@SuppressWarnings("deprecation")
	public void drawGameElements(Graphics g, boolean breakPeriod, boolean firstObject) {
		boolean relaxAutoPilot = (GameMod.RELAX.isActive() || GameMod.AUTOPILOT.isActive());
		int margin = (int) (width * 0.008f);
		float uiScale = GameImage.getUIscale();

		// score
		if (!relaxAutoPilot)
			drawFixedSizeSymbolString((scoreDisplay < 100000000) ? String.format("%08d", scoreDisplay) : Long.toString(scoreDisplay),
					width - margin, 0, 1.0f, getScoreSymbolImage('0').getWidth() - 2, true);

		// score percentage
		int symbolHeight = getScoreSymbolImage('0').getHeight();
		if (!relaxAutoPilot)
			drawSymbolString(
					String.format((scorePercentDisplay < 10f) ? "0%.2f%%" : "%.2f%%", scorePercentDisplay),
					width - margin, symbolHeight, 0.60f, 1f, true);

		// map progress circle
		Beatmap beatmap = MusicController.getBeatmap();
		int firstObjectTime = beatmap.objects[0].getTime();
		int trackPosition = MusicController.getPosition();
		float circleDiameter = symbolHeight * 0.60f;
		int circleX = (int) (width - margin - (  // max width: "100.00%"
				getScoreSymbolImage('1').getWidth() +
				getScoreSymbolImage('0').getWidth() * 4 +
				getScoreSymbolImage('.').getWidth() +
				getScoreSymbolImage('%').getWidth()
		) * 0.60f - circleDiameter);
		if (!relaxAutoPilot) {
			g.setAntiAlias(true);
			g.setLineWidth(2f);
			g.setColor(Color.white);
			g.drawOval(circleX, symbolHeight, circleDiameter, circleDiameter);
			if (trackPosition > firstObjectTime) {
				// map progress (white)
				g.fillArc(circleX, symbolHeight, circleDiameter, circleDiameter,
						-90, -90 + (int) (360f * (trackPosition - firstObjectTime) / (beatmap.endTime - firstObjectTime))
				);
			} else {
				// lead-in time (yellow)
				g.setColor(Utils.COLOR_YELLOW_ALPHA);
				g.fillArc(circleX, symbolHeight, circleDiameter, circleDiameter,
						-90 + (int) (360f * trackPosition / firstObjectTime), -90
				);
			}
			g.setAntiAlias(false);
		}

		// mod icons
		if ((firstObject && trackPosition < firstObjectTime) || GameMod.AUTO.isActive()) {
			int modWidth = GameMod.AUTO.getImage().getWidth();
			float modX = (width * 0.98f) - modWidth;
			int modCount = 0;
			for (GameMod mod : GameMod.VALUES_REVERSED) {
				if (mod.isActive()) {
					mod.getImage().draw(
							modX - (modCount * (modWidth / 2f)),
							symbolHeight + circleDiameter + 10
					);
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
			float oldAlphaBlack = Utils.COLOR_BLACK_ALPHA.a;
			Utils.COLOR_BLACK_ALPHA.a = hitErrorAlpha;
			g.setColor(Utils.COLOR_BLACK_ALPHA);
			g.fillRect((hitErrorX - 3 - hitResultOffset[HIT_50]) * uiScale, tickY,
					(hitResultOffset[HIT_50] * 2) * uiScale, tickHeight);
			Utils.COLOR_BLACK_ALPHA.a = oldAlphaBlack;
			Utils.COLOR_LIGHT_ORANGE.a = hitErrorAlpha;
			g.setColor(Utils.COLOR_LIGHT_ORANGE);
			g.fillRect((hitErrorX - 3 - hitResultOffset[HIT_50]) * uiScale, barY,
					(hitResultOffset[HIT_50] * 2) * uiScale, barHeight);
			Utils.COLOR_LIGHT_ORANGE.a = 1f;
			Utils.COLOR_LIGHT_GREEN.a = hitErrorAlpha;
			g.setColor(Utils.COLOR_LIGHT_GREEN);
			g.fillRect((hitErrorX - 3 - hitResultOffset[HIT_100]) * uiScale, barY,
					(hitResultOffset[HIT_100] * 2) * uiScale, barHeight);
			Utils.COLOR_LIGHT_GREEN.a = 1f;
			Utils.COLOR_LIGHT_BLUE.a = hitErrorAlpha;
			g.setColor(Utils.COLOR_LIGHT_BLUE);
			g.fillRect((hitErrorX - 3 - hitResultOffset[HIT_300]) * uiScale, barY,
					(hitResultOffset[HIT_300] * 2) * uiScale, barHeight);
			Utils.COLOR_LIGHT_BLUE.a = 1f;
			white.a = hitErrorAlpha;
			g.setColor(white);
			g.fillRect((hitErrorX - 1.5f) * uiScale, tickY, 3 * uiScale, tickHeight);

			// draw ticks
			float tickWidth = 2 * uiScale;
			for (HitErrorInfo info : hitErrorList) {
				int time = info.time;
				float alpha = 1 - ((float) (trackPosition - time) / HIT_ERROR_FADE_TIME);
				white.a = alpha * hitErrorAlpha;
				g.setColor(white);
				g.fillRect((hitErrorX + info.timeDiff - 1) * uiScale, tickY, tickWidth, tickHeight);
			}
		}

		if (!breakPeriod && !relaxAutoPilot) {
			// scorebar
			float healthRatio = healthDisplay / 100f;
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

			scorebar.setAlpha(1f);
			scorebar.draw(0, 0);
			colourCropped.draw(colourX, colourY);

			Image ki = null;
			if (health >= 50f)
				ki = GameImage.SCOREBAR_KI.getImage();
			else if (health >= 25f)
				ki = GameImage.SCOREBAR_KI_DANGER.getImage();
			else
				ki = GameImage.SCOREBAR_KI_DANGER2.getImage();
			if (comboPopTime < COMBO_POP_TIME)
				ki = ki.getScaledCopy(1f + (0.45f * (1f - (float) comboPopTime / COMBO_POP_TIME)));
			ki.drawCentered(colourX + colourCropped.getWidth(), colourY);

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
					drawSymbolString(comboString, margin, height - margin - (symbolHeight * comboPopBack), comboPopBack, 0.5f, false);
				drawSymbolString(comboString, margin, height - margin - (symbolHeight * comboPopFront), comboPopFront, 1f, false);
			}
		} else if (!relaxAutoPilot) {
			// grade
			Grade grade = getGrade();
			if (grade != Grade.NULL) {
				Image gradeImage = grade.getSmallImage();
				float gradeScale = symbolHeight * 0.75f / gradeImage.getHeight();
				gradeImage.getScaledCopy(gradeScale).draw(
						circleX - gradeImage.getWidth(), symbolHeight
				);
			}
		}
	}

	/**
	 * Draws ranking elements: score, results, ranking, game mods.
	 * @param g the graphics context
	 * @param beatmap the beatmap
	 */
	public void drawRankingElements(Graphics g, Beatmap beatmap) {
		// TODO Version 2 skins
		float rankingHeight = 75;
		float scoreTextScale = 1.0f;
		float symbolTextScale = 1.15f;
		float rankResultScale = 0.5f;
		float uiScale = GameImage.getUIscale();

		// ranking panel
		GameImage.RANKING_PANEL.getImage().draw(0, (int) (rankingHeight * uiScale));

		// score
		drawFixedSizeSymbolString(
				(score < 100000000) ? String.format("%08d", score) : Long.toString(score),
				210 * uiScale, (rankingHeight + 50) * uiScale,
				scoreTextScale, getScoreSymbolImage('0').getWidth() * scoreTextScale - 2, false
		);

		// result counts
		float resultInitialX = 130;
		float resultInitialY = rankingHeight + 140;
		float resultHitInitialX = 65;
		float resultHitInitialY = rankingHeight + 182;
		float resultOffsetX = 320;
		float resultOffsetY = 96;

		int[] rankDrawOrder = { HIT_300, HIT_300G, HIT_100, HIT_100K, HIT_50, HIT_MISS };
		int[] rankResultOrder = {
				hitResultCount[HIT_300], hitResultCount[HIT_300G],
				hitResultCount[HIT_100], hitResultCount[HIT_100K] + hitResultCount[HIT_300K],
				hitResultCount[HIT_50], hitResultCount[HIT_MISS]
		};

		for (int i = 0; i < rankDrawOrder.length; i += 2) {
			hitResults[rankDrawOrder[i]].getScaledCopy(rankResultScale).drawCentered(
					resultHitInitialX * uiScale,
					(resultHitInitialY + (resultOffsetY * (i / 2))) * uiScale);
			hitResults[rankDrawOrder[i+1]].getScaledCopy(rankResultScale).drawCentered(
					(resultHitInitialX + resultOffsetX) * uiScale,
					(resultHitInitialY  + (resultOffsetY * (i / 2))) * uiScale);
			drawSymbolString(String.format("%dx", rankResultOrder[i]),
					resultInitialX * uiScale,
					(resultInitialY + (resultOffsetY * (i / 2))) * uiScale,
					symbolTextScale, 1f, false);
			drawSymbolString(String.format("%dx", rankResultOrder[i+1]),
					(resultInitialX + resultOffsetX) * uiScale,
					(resultInitialY + (resultOffsetY * (i / 2))) * uiScale,
					symbolTextScale, 1f, false);
		}

		// combo and accuracy
		float accuracyX = 295;
		float textY = rankingHeight + 425;
		float numbersY = textY + 30;
		drawSymbolString(String.format("%dx", comboMax),
				25 * uiScale, numbersY * uiScale, symbolTextScale, 1f, false);
		drawSymbolString(String.format("%02.2f%%", getScorePercent()),
				(accuracyX + 20) * uiScale, numbersY * uiScale, symbolTextScale, 1f, false);
		GameImage.RANKING_MAXCOMBO.getImage().draw(10 * uiScale, textY * uiScale);
		GameImage.RANKING_ACCURACY.getImage().draw(accuracyX * uiScale, textY * uiScale);

		// full combo
		if (comboMax == fullObjectCount) {
			GameImage.RANKING_PERFECT.getImage().draw(
					width * 0.08f,
					(height * 0.99f) - GameImage.RANKING_PERFECT.getImage().getHeight()
			);
		}

		// grade
		Grade grade = getGrade();
		if (grade != Grade.NULL)
			grade.getLargeImage().draw(width - grade.getLargeImage().getWidth(), rankingHeight);

		// header
		Image rankingTitle = GameImage.RANKING_TITLE.getImage();
		g.setColor(Utils.COLOR_BLACK_ALPHA);
		g.fillRect(0, 0, width, 100 * uiScale);
		rankingTitle.draw((width * 0.97f) - rankingTitle.getWidth(), 0);
		float marginX = width * 0.01f, marginY = height * 0.002f;
		Utils.FONT_LARGE.drawString(marginX, marginY,
				String.format("%s - %s [%s]", beatmap.getArtist(), beatmap.getTitle(), beatmap.version), Color.white);
		Utils.FONT_MEDIUM.drawString(marginX, marginY + Utils.FONT_LARGE.getLineHeight() - 6,
				String.format("Beatmap by %s", beatmap.creator), Color.white);
		Utils.FONT_MEDIUM.drawString(marginX, marginY + Utils.FONT_LARGE.getLineHeight() + Utils.FONT_MEDIUM.getLineHeight() - 10,
				String.format("Played on %s.", scoreData.getTimeString()), Color.white);

		// mod icons
		int modWidth = GameMod.AUTO.getImage().getWidth();
		float modX = (width * 0.98f) - modWidth;
		int modCount = 0;
		for (GameMod mod : GameMod.VALUES_REVERSED) {
			if ((mod.getBit() & scoreData.mods) > 0) {
				mod.getImage().draw(modX - (modCount * (modWidth / 2f)), height / 2f);
				modCount++;
			}
		}
	}

	/**
	 * Draws stored hit results and removes them from the list as necessary.
	 * @param trackPosition the current track position (in ms)
	 */
	public void drawHitResults(int trackPosition) {
		Iterator<HitObjectResult> iter = hitResultList.iterator();
		while (iter.hasNext()) {
			HitObjectResult hitResult = iter.next();
			if (hitResult.time + HITRESULT_TIME > trackPosition) {
				// spinner
				if (hitResult.hitResultType == HitObjectType.SPINNER && hitResult.result != HIT_MISS) {
					Image spinnerOsu = GameImage.SPINNER_OSU.getImage();
					spinnerOsu.setAlpha(hitResult.alpha);
					spinnerOsu.drawCentered(width / 2, height / 4);
					spinnerOsu.setAlpha(1f);
				}

				// hit lighting
				else if (Options.isHitLightingEnabled() && hitResult.result != HIT_MISS &&
					hitResult.result != HIT_SLIDER30 && hitResult.result != HIT_SLIDER10) {
					// TODO: add particle system
					Image lighting = GameImage.LIGHTING.getImage();
					lighting.setAlpha(hitResult.alpha);
					lighting.drawCentered(hitResult.x, hitResult.y, hitResult.color);
				}

				// hit animation
				if (hitResult.result != HIT_MISS && (
				    hitResult.hitResultType == HitObjectType.CIRCLE ||
				    hitResult.hitResultType == HitObjectType.SLIDER_FIRST ||
				    hitResult.hitResultType == HitObjectType.SLIDER_LAST)) {
					float scale = (!hitResult.expand) ? 1f : Utils.easeOut(
							Utils.clamp(trackPosition - hitResult.time, 0, HITCIRCLE_FADE_TIME),
							1f, HITCIRCLE_ANIM_SCALE - 1f, HITCIRCLE_FADE_TIME
					);
					float alpha = Utils.easeOut(
							Utils.clamp(trackPosition - hitResult.time, 0, HITCIRCLE_FADE_TIME),
							1f, -1f, HITCIRCLE_FADE_TIME
					);

					// slider curve
					if (hitResult.curve != null) {
						float oldWhiteAlpha = Utils.COLOR_WHITE_FADE.a;
						float oldColorAlpha = hitResult.color.a;
						Utils.COLOR_WHITE_FADE.a = alpha;
						hitResult.color.a = alpha;
						hitResult.curve.draw(hitResult.color);
						Utils.COLOR_WHITE_FADE.a = oldWhiteAlpha;
						hitResult.color.a = oldColorAlpha;
					}

					// hit circles
					Image scaledHitCircle = GameImage.HITCIRCLE.getImage().getScaledCopy(scale);
					Image scaledHitCircleOverlay = GameImage.HITCIRCLE_OVERLAY.getImage().getScaledCopy(scale);
					scaledHitCircle.setAlpha(alpha);
					scaledHitCircleOverlay.setAlpha(alpha);
					scaledHitCircle.drawCentered(hitResult.x, hitResult.y, hitResult.color);
					scaledHitCircleOverlay.drawCentered(hitResult.x, hitResult.y);
				}

				// hit result
				if (hitResult.hitResultType == HitObjectType.CIRCLE ||
				    hitResult.hitResultType == HitObjectType.SPINNER ||
				    hitResult.curve != null) {
					float scale = Utils.easeBounce(
							Utils.clamp(trackPosition - hitResult.time, 0, HITCIRCLE_TEXT_BOUNCE_TIME),
							1f, HITCIRCLE_TEXT_ANIM_SCALE - 1f, HITCIRCLE_TEXT_BOUNCE_TIME
					);
					float alpha = Utils.easeOut(
							Utils.clamp((trackPosition - hitResult.time) - HITCIRCLE_FADE_TIME, 0, HITCIRCLE_TEXT_FADE_TIME),
							1f, -1f, HITCIRCLE_TEXT_FADE_TIME
					);
					Image scaledHitResult = hitResults[hitResult.result].getScaledCopy(scale);
					scaledHitResult.setAlpha(alpha);
					scaledHitResult.drawCentered(hitResult.x, hitResult.y);
				}

				hitResult.alpha = 1 - ((float) (trackPosition - hitResult.time) / HITRESULT_FADE_TIME);
			} else {
				if (hitResult.curve != null)
					hitResult.curve.discardCache();
				iter.remove();
			}
		}
	}

	/**
	 * Changes health by a given percentage, modified by drainRate.
	 * @param percent the health percentage
	 */
	public void changeHealth(float percent) {
		// TODO: drainRate formula
		health += percent;
		if (health > 100f)
			health = 100f;
		else if (health < 0f)
			health = 0f;
	}

	/**
	 * Returns the current health percentage.
	 */
	public float getHealth() { return health; }

	/**
	 * Returns false if health is zero.
	 * If "No Fail" or "Auto" mods are active, this will always return true.
	 */
	public boolean isAlive() {
		return (health > 0f || GameMod.NO_FAIL.isActive() || GameMod.AUTO.isActive() ||
		        GameMod.RELAX.isActive() || GameMod.AUTOPILOT.isActive());
	}

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
	private float getScorePercent() {
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
		return getGrade(
			hitResultCount[HIT_300], hitResultCount[HIT_100],
			hitResultCount[HIT_50], hitResultCount[HIT_MISS],
			(GameMod.HIDDEN.isActive() || GameMod.FLASHLIGHT.isActive())
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
		if (healthDisplay != health) {
			float shift = delta / 15f;
			if (healthDisplay < health) {
				healthDisplay += shift;
				if (healthDisplay > health)
					healthDisplay = health;
			} else {
				healthDisplay -= shift;
				if (healthDisplay < health)
					healthDisplay = health;
			}
		}

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
			int trackPosition = MusicController.getPosition();
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
		if (combo >= 20 && !(GameMod.RELAX.isActive() || GameMod.AUTOPILOT.isActive()))
			SoundController.playSound(SoundEffect.COMBOBREAK);
		combo = 0;
		if (GameMod.SUDDEN_DEATH.isActive())
			health = 0f;
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
	public void sliderTickResult(int time, int result, float x, float y, HitObject hitObject, int repeat) {
		int hitValue = 0;
		switch (result) {
		case HIT_SLIDER30:
			hitValue = 30;
			incrementComboStreak();
			changeHealth(1f);
			SoundController.playHitSound(
					hitObject.getEdgeHitSoundType(repeat),
					hitObject.getSampleSet(repeat),
					hitObject.getAdditionSampleSet(repeat));
			break;
		case HIT_SLIDER10:
			hitValue = 10;
			incrementComboStreak();
			SoundController.playHitSound(HitSound.SLIDERTICK);
			break;
		case HIT_MISS:
			resetComboStreak();
			break;
		default:
			return;
		}
		fullObjectCount++;

		if (hitValue > 0) {
			score += hitValue;
			if (!Options.isPerfectHitBurstEnabled())
				;  // hide perfect hit results
			else
				hitResultList.add(new HitObjectResult(time, result, x, y, null, HitObjectType.SLIDERTICK, null, false));
		}
	}
	public void sliderFinalResult(int time, int hitSlider30, float x, float y,
			OsuHitObject hitObject, int currentRepeats) {
		score += 30;
	}

	/**
	 * Returns the score for a hit based on the following score formula:
	 * <p>
	 * Score = Hit Value + Hit Value * (Combo * Difficulty * Mod) / 25
	 * <ul>
	 * <li><strong>Hit Value:</strong> hit result (50, 100, 300), slider ticks, spinner bonus
	 * <li><strong>Combo:</strong> combo before this hit - 1 (minimum 0)
	 * <li><strong>Difficulty:</strong> the beatmap difficulty
	 * <li><strong>Mod:</strong> mod multipliers
	 * </ul>
	 * @param hitValue the hit value
	 * @return the score value
	 */
	private int getScoreForHit(int hitValue) {
		return hitValue + (int) (hitValue * (Math.max(combo - 1, 0) * difficulty * GameMod.getScoreMultiplier()) / 25);
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
	 * @param repeat the current repeat number (for sliders, or 0 otherwise)
	 * @param hitResultType the type of hit object for the result
	 * @return the actual hit result (HIT_* constants)
	 */
	private int handleHitResult(int time, int result, float x, float y, Color color,
			boolean end, HitObject hitObject, int repeat, HitObjectType hitResultType) {
		// update health, score, and combo streak based on hit result
		int hitValue = 0;
		switch (result) {
		case HIT_300:
			hitValue = 300;
			changeHealth(5f);
			break;
		case HIT_100:
			System.out.println("100! "+hitObject+" "+hitObject.getTime()+" "+hitObject.getTypeName());
			hitValue = 100;
			changeHealth(2f);
			comboEnd |= 1;
			break;
		case HIT_50:
			System.out.println("50! "+hitObject+" "+hitObject.getTime()+" "+hitObject.getTypeName());
			hitValue = 50;
			comboEnd |= 2;
			break;
		case HIT_MISS:
			System.out.println("miss! "+hitObject+" "+hitObject.getTime()+" "+hitObject.getTypeName());
			hitValue = 0;
			changeHealth(-10f);
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
			//TODO merge conflict
			
			/**
			 * https://osu.ppy.sh/wiki/Score
			 * [SCORE FORMULA]
			 * Score = Hit Value + Hit Value * (Combo * Difficulty * Mod) / 25
			 * - Hit Value: hit result (50, 100, 300), slider ticks, spinner bonus
			 * - Combo: combo before this hit - 1 (minimum 0)
			 * - Difficulty: the beatmap difficulty
			 * - Mod: mod multipliers
			 */
			int comboMulti = Math.max(combo - 1, 0);
			if(hitObject.isSlider()){
				comboMulti += 1;
			}
			score += (hitValue + (hitValue * (comboMulti * getDifficultyMultiplier() * GameMod.getScoreMultiplier()) / 25));
			
			// calculate score and increment combo streak
			changeScore(getScoreForHit(hitValue));
			incrementComboStreak();
			//merge conflict end
		}
		hitResultCount[result]++;
		fullObjectCount++;

		// last element in combo: check for Geki/Katu
		if (end) {
			if (comboEnd == 0) {
				result = HIT_300G;
				changeHealth(15f);
				hitResultCount[result]++;
			} else if ((comboEnd & 2) == 0) {
				if (result == HIT_100) {
					result = HIT_100K;
					changeHealth(10f);
					hitResultCount[result]++;
				} else if (result == HIT_300) {
					result = HIT_300K;
					changeHealth(10f);
					hitResultCount[result]++;
				}
			}
			comboEnd = 0;
		}

		return result;
	}

	/**
	 * Handles a slider hit result.
	 * @param time the object start time
	 * @param result the hit result (HIT_* constants)
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param color the combo color
	 * @param end true if this is the last hit object in the combo
	 * @param hitObject the hit object
	 * @param repeat the current repeat number (for sliders, or 0 otherwise)
	 * @param hitResultType the type of hit object for the result
	 * @param curve the slider curve (or null if not applicable)
	 * @param expand whether or not the hit result animation should expand (if applicable)
	 */
	public void hitResult(int time, int result, float x, float y, Color color,
						  boolean end, HitObject hitObject, int repeat,
						  HitObjectType hitResultType, Curve curve, boolean expand) {
		result = handleHitResult(time, result, x, y, color, end, hitObject, repeat, hitResultType);

		if ((result == HIT_300 || result == HIT_300G || result == HIT_300K) && !Options.isPerfectHitBurstEnabled())
			;  // hide perfect hit results
		else if (result == HIT_MISS && (GameMod.RELAX.isActive() || GameMod.AUTOPILOT.isActive()))
			;  // "relax" and "autopilot" mods: hide misses
		else {
			hitResultList.add(new HitObjectResult(time, result, x, y, color, hitResultType, curve, expand));

			// sliders: add the other curve endpoint for the hit animation
			if (curve != null) {
				boolean isFirst = (hitResultType == HitObjectType.SLIDER_FIRST);
				float[] p = curve.pointAt((isFirst) ? 1f : 0f);
				HitObjectType type = (isFirst) ? HitObjectType.SLIDER_LAST : HitObjectType.SLIDER_FIRST;
				hitResultList.add(new HitObjectResult(time, result, p[0], p[1], color, type, null, expand));
			}
		}
	}

	private int getDifficultyMultiplier() {
		return difficultyMultiplier;
	}

	public void calculateDifficultyMultiplier() {
		//https://osu.ppy.sh/wiki/Score#How_to_calculate_the_Difficulty_multiplier
		//TODO   THE LIES ( difficultyMultiplier )
		/*
			924 3x1/4 beat notes 0.14stars
			924 3x1beat 0.28stars
			912 3x1beat wth 1 extra note 10 sec away 0.29stars
			
			seems to be based on hitobject density?  (Total Objects/Time)
		 */
		
		difficultyMultiplier = (int)((circleSize + difficulty + drainRate) / 6) + 2;
	}
	/**
	 * Returns a ScoreData object encapsulating all game data.
	 * If score data already exists, the existing object will be returned
	 * (i.e. this will not overwrite existing data).
	 * @param beatmap the beatmap
	 * @return the ScoreData object
	 */
	public ScoreData getScoreData(Beatmap beatmap) {
		if (scoreData != null)
			return scoreData;

		scoreData = new ScoreData();
		scoreData.timestamp = System.currentTimeMillis() / 1000L;
		scoreData.MID = beatmap.beatmapID;
		scoreData.MSID = beatmap.beatmapSetID;
		scoreData.title = beatmap.title;
		scoreData.artist = beatmap.artist;
		scoreData.creator = beatmap.creator;
		scoreData.version = beatmap.version;
		scoreData.hit300 = hitResultCount[HIT_300];
		scoreData.hit100 = hitResultCount[HIT_100];
		scoreData.hit50 = hitResultCount[HIT_50];
		scoreData.geki = hitResultCount[HIT_300G];
		scoreData.katu = hitResultCount[HIT_300K] + hitResultCount[HIT_100K];
		scoreData.miss = hitResultCount[HIT_MISS];
		scoreData.score = score;
		scoreData.combo = comboMax;
		scoreData.perfect = (comboMax == fullObjectCount);
		scoreData.mods = GameMod.getModState();
		scoreData.replayString = (replay == null) ? null : replay.getReplayFilename();
		scoreData.playerName = "OpsuPlayer"; //TODO GameDataPlayerName?
		return scoreData;
	}

	/**
	 * Returns a Replay object encapsulating all game data.
	 * If a replay already exists and frames is null, the existing object will be returned.
	 * @param frames the replay frames
	 * @param beatmap the associated beatmap
	 * @return the Replay object, or null if none exists and frames is null
	 */
	public Replay getReplay(ReplayFrame[] frames, Beatmap beatmap) {
		if (replay != null && frames == null)
			return replay;

		if (frames == null)
			return null;

		replay = new Replay();
		replay.mode = Beatmap.MODE_OSU;
		replay.version = Updater.get().getBuildDate();
		replay.beatmapHash = (beatmap == null) ? "" : Utils.getMD5(beatmap.getFile());
		replay.playerName = "";  // TODO
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
		replay.lifeFrames = null;  // TODO
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
	public boolean isGameplay() { return gameplay; }

	/**
	 * Adds the hit into the list of hit error information.
	 * @param time the correct hit time
	 * @param x the x coordinate of the hit
	 * @param y the y coordinate of the hit
	 * @param timeDiff the difference between the correct and actual hit times
	 */
	public void addHitError(int time, int x, int y, int timeDiff) {
		hitErrorList.addFirst(new HitErrorInfo(time, x, y, timeDiff));
	}
}
