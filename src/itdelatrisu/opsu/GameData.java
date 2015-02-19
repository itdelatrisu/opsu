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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

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

	/** Time, in milliseconds, for a hit error tick to fade. */
	private static final int HIT_ERROR_FADE_TIME = 5000;

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
	private int comboBurstX;

	/** Time offsets for obtaining each hit result (indexed by HIT_* constants). */
	private int[] hitResultOffset;

	/** List of hit result objects associated with hit objects. */
	private LinkedList<OsuHitObjectResult> hitResultList;

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
	private LinkedList<HitErrorInfo> hitErrorList;

	/**
	 * Hit result helper class.
	 */
	private class OsuHitObjectResult {
		/** Object start time. */
		public int time;

		/** Hit result. */
		public int result;

		/** Object coordinates. */
		public float x, y;

		/** Combo color. */
		public Color color;

		/** Alpha level (for fading out). */
		public float alpha = 1f;

		/**
		 * Constructor.
		 * @param time the result's starting track position
		 * @param result the hit result (HIT_* constants)
		 * @param x the center x coordinate
		 * @param y the center y coordinate
		 * @param color the color of the hit object
		 */
		public OsuHitObjectResult(int time, int result, float x, float y, Color color) {
			this.time = time;
			this.result = result;
			this.x = x;
			this.y = y;
			this.color = color;
		}
	}

	/** Current game score. */
	private long score;

	/** Displayed game score (for animation, slightly behind score). */
	private long scoreDisplay;

	/** Current health bar percentage. */
	private float health;

	/** Displayed health (for animation, slightly behind health). */
	private float healthDisplay;

	/** Beatmap HPDrainRate value. (0:easy ~ 10:hard) */
	private float drainRate = 5f;

	/** Beatmap OverallDifficulty value. (0:easy ~ 10:hard) */
	private float difficulty = 5f;

	/** Default text symbol images. */
	private Image[] defaultSymbols;

	/** Score text symbol images. */
	private HashMap<Character, Image> scoreSymbols;

	/** Scorebar animation. */
	private Animation scorebarColour;

	/** The associated score data. */
	private ScoreData scoreData;

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
	 * {@link #drawRankingElements(Graphics, OsuFile)} method.
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

		loadImages();
	}

	/**
	 * Clears all data and re-initializes object.
	 */
	public void clear() {
		score = 0;
		scoreDisplay = 0;
		health = 100f;
		healthDisplay = 100f;
		hitResultCount = new int[HIT_MAX];
		hitResultList = new LinkedList<OsuHitObjectResult>();
		hitErrorList = new LinkedList<HitErrorInfo>();
		fullObjectCount = 0;
		combo = 0;
		comboMax = 0;
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
	 * Returns a default/score text symbol image for a character.
	 */
	public Image getDefaultSymbolImage(int i) { return defaultSymbols[i]; }
	public Image getScoreSymbolImage(char c) { return scoreSymbols.get(c); }

	/**
	 * Sets or returns the health drain rate.
	 */
	public void setDrainRate(float drainRate) { this.drainRate = drainRate; }
	public float getDrainRate() { return drainRate; }

	/**
	 * Sets or returns the difficulty.
	 */
	public void setDifficulty(float difficulty) { this.difficulty = difficulty; }
	public float getDifficulty() { return difficulty; }

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
	 */
	public void drawSymbolNumber(int n, float x, float y, float scale) {
		int length = (int) (Math.log10(n) + 1);
		float digitWidth = getDefaultSymbolImage(0).getWidth() * scale;
		float cx = x + ((length - 1) * (digitWidth / 2));

		for (int i = 0; i < length; i++) {
			getDefaultSymbolImage(n % 10).getScaledCopy(scale).drawCentered(cx, y);
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
	 * @param rightAlign align right (true) or left (false)
	 */
	private void drawSymbolString(String str, int x, int y, float scale, boolean rightAlign) {
		char[] c = str.toCharArray();
		int cx = x;
		if (rightAlign) {
			for (int i = c.length - 1; i >= 0; i--) {
				Image digit = getScoreSymbolImage(c[i]);
				if (scale != 1.0f)
					digit = digit.getScaledCopy(scale);
				cx -= digit.getWidth();
				digit.draw(cx, y);
			}
		} else {
			for (int i = 0; i < c.length; i++) {
				Image digit = getScoreSymbolImage(c[i]);
				if (scale != 1.0f)
					digit = digit.getScaledCopy(scale);
				digit.draw(cx, y);
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
	private void drawFixedSizeSymbolString(String str, int x, int y, float scale, float fixedsize, boolean rightAlign) {
		char[] c = str.toCharArray();
		int cx = x;
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
		int marginX = (int) (width * 0.008f);

		// score
		drawFixedSizeSymbolString((scoreDisplay < 100000000) ? String.format("%08d", scoreDisplay) : Long.toString(scoreDisplay),
				width - marginX, 0, 1.0f, getScoreSymbolImage('0').getWidth() - 2, true);

		// score percentage
		int symbolHeight = getScoreSymbolImage('0').getHeight();
		float scorePercent = getScorePercent();
		drawSymbolString(
				String.format((scorePercent < 10f) ? "0%.2f%%" : "%.2f%%", scorePercent),
				width - marginX, symbolHeight, 0.60f, true
		);

		// map progress circle
		g.setAntiAlias(true);
		g.setLineWidth(2f);
		g.setColor(Color.white);
		float circleDiameter = symbolHeight * 0.60f;
		int circleX = (int) (width - marginX - (  // max width: "100.00%"
				getScoreSymbolImage('1').getWidth() +
				getScoreSymbolImage('0').getWidth() * 4 +
				getScoreSymbolImage('.').getWidth() +
				getScoreSymbolImage('%').getWidth()
		) * 0.60f - circleDiameter);
		g.drawOval(circleX, symbolHeight, circleDiameter, circleDiameter);

		OsuFile osu = MusicController.getOsuFile();
		int firstObjectTime = osu.objects[0].getTime();
		int trackPosition = MusicController.getPosition();
		if (trackPosition > firstObjectTime) {
			// map progress (white)
			g.fillArc(circleX, symbolHeight, circleDiameter, circleDiameter,
					-90, -90 + (int) (360f * (trackPosition - firstObjectTime) / (osu.endTime - firstObjectTime))
			);
		} else {
			// lead-in time (yellow)
			g.setColor(Utils.COLOR_YELLOW_ALPHA);
			g.fillArc(circleX, symbolHeight, circleDiameter, circleDiameter,
					-90 + (int) (360f * trackPosition / firstObjectTime), -90
			);
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

		if (!breakPeriod) {
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
			float colourX = 4 * GameImage.getUIscale(), colourY = 15 * GameImage.getUIscale();
			Image colourCropped = colour.getSubImage(0, 0, (int) (645 * GameImage.getUIscale() * healthRatio), colour.getHeight());

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
			ki.drawCentered(colourX + colourCropped.getWidth(), colourY);

			// combo burst
			if (comboBurstIndex != -1 && comboBurstAlpha > 0f) {
				Image comboBurst = comboBurstImages[comboBurstIndex];
				comboBurst.setAlpha(comboBurstAlpha);
				comboBurstImages[comboBurstIndex].draw(comboBurstX, height - comboBurst.getHeight());
			}

			// combo count
			if (combo > 0)  // 0 isn't a combo
				drawSymbolString(String.format("%dx", combo), 10, height - 10 - symbolHeight, 1.0f, false);

			// hit error bar
			if (Options.isHitErrorBarEnabled()) {
				// draw bar
				int hitErrorY = 30;
				g.setColor(Color.black);
				g.fillRect(width / 2f - 3 - hitResultOffset[HIT_50],
						height - marginX - hitErrorY - 10,
						hitResultOffset[HIT_50] * 2, 20);
				g.setColor(Utils.COLOR_LIGHT_ORANGE);
				g.fillRect(width / 2f - 3 - hitResultOffset[HIT_50],
						height - marginX - hitErrorY - 3,
						hitResultOffset[HIT_50] * 2, 6);
				g.setColor(Utils.COLOR_LIGHT_GREEN);
				g.fillRect(width / 2f - 3 - hitResultOffset[HIT_100],
						height - marginX - hitErrorY - 3,
						hitResultOffset[HIT_100] * 2, 6);
				g.setColor(Utils.COLOR_LIGHT_BLUE);
				g.fillRect(width / 2f - 3 - hitResultOffset[HIT_300],
						height - marginX - hitErrorY - 3,
						hitResultOffset[HIT_300] * 2, 6);
				g.setColor(Color.white);
				g.drawRect(width / 2f - 3, height - marginX - hitErrorY - 10, 6, 20);

				// draw ticks
				Color white = new Color(Color.white);
				Iterator<HitErrorInfo> iter = hitErrorList.iterator();
				while (iter.hasNext()) {
					HitErrorInfo info = iter.next();
					int time = info.time;
					if (Math.abs(info.timeDiff) < hitResultOffset[GameData.HIT_50] &&
					    time + HIT_ERROR_FADE_TIME > trackPosition) {
						float alpha = 1 - ((float) (trackPosition - time) / HIT_ERROR_FADE_TIME);
						white.a = alpha;
						g.setColor(white);
						g.fillRect(width / 2 + info.timeDiff - 1, height - marginX - hitErrorY - 10, 2, 20);
					} else
						iter.remove();
				}
			}
		} else {
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
	 * @param osu the OsuFile
	 */
	public void drawRankingElements(Graphics g, OsuFile osu) {
		// TODO Version 2 skins
		float rankingHeight = 75;
		float scoreTextScale = 1.0f;
		float symbolTextScale = 1.15f;
		float rankResultScale = 0.5f;

		// ranking panel
		GameImage.RANKING_PANEL.getImage().draw(0, (int) (rankingHeight * GameImage.getUIscale()));

		// score
		drawFixedSizeSymbolString(
				(score < 100000000) ? String.format("%08d", score) : Long.toString(score),
				(int) (210 * GameImage.getUIscale()),
				(int) ((rankingHeight + 50) * GameImage.getUIscale()),
				scoreTextScale,
				getScoreSymbolImage('0').getWidth() * scoreTextScale - 2, false);

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
					(resultHitInitialX * GameImage.getUIscale()),
					((resultHitInitialY  + (resultOffsetY * (i / 2))) * GameImage.getUIscale()));
			hitResults[rankDrawOrder[i+1]].getScaledCopy(rankResultScale).drawCentered(
					((resultHitInitialX + resultOffsetX) * GameImage.getUIscale()),
					((resultHitInitialY  + (resultOffsetY * (i / 2))) * GameImage.getUIscale()));
			drawSymbolString(String.format("%dx", rankResultOrder[i]),
					(int) (resultInitialX * GameImage.getUIscale()),
					(int) ((resultInitialY + (resultOffsetY * (i / 2))) * GameImage.getUIscale()),
					symbolTextScale, false);
			drawSymbolString(String.format("%dx", rankResultOrder[i+1]),
					(int) ((resultInitialX + resultOffsetX) * GameImage.getUIscale()),
					(int) ((resultInitialY + (resultOffsetY * (i / 2))) * GameImage.getUIscale()),
					symbolTextScale, false);
		}

		// combo and accuracy
		float accuracyX = 295;
		float textY = rankingHeight + 425;
		float numbersY = textY + 30;
		drawSymbolString(String.format("%dx", comboMax),
				(int) (25 * GameImage.getUIscale()),
				(int) (numbersY * GameImage.getUIscale()), symbolTextScale, false);
		drawSymbolString(String.format("%02.2f%%", getScorePercent()),
				(int) ((accuracyX + 20) * GameImage.getUIscale()),
				(int) (numbersY * GameImage.getUIscale()), symbolTextScale, false);
		GameImage.RANKING_MAXCOMBO.getImage().draw(
				(int) (10 * GameImage.getUIscale()),
				(int) (textY * GameImage.getUIscale()));
		GameImage.RANKING_ACCURACY.getImage().draw(
				(int) (accuracyX * GameImage.getUIscale()),
				(int) (textY * GameImage.getUIscale()));

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
		g.fillRect(0, 0, width, 100 * GameImage.getUIscale());
		rankingTitle.draw((width * 0.97f) - rankingTitle.getWidth(), 0);
		float c = width * 0.01f;
		Utils.FONT_LARGE.drawString(c, c,
				String.format("%s - %s [%s]", osu.getArtist(), osu.getTitle(), osu.version), Color.white);
		Utils.FONT_MEDIUM.drawString(c, c + Utils.FONT_LARGE.getLineHeight() - 6,
				String.format("Beatmap by %s", osu.creator), Color.white);
		Utils.FONT_MEDIUM.drawString(
				c, c + Utils.FONT_LARGE.getLineHeight() + Utils.FONT_MEDIUM.getLineHeight() - 10,
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
	 * @param trackPosition the current track position
	 */
	public void drawHitResults(int trackPosition) {
		final int fadeDelay = 500;

		Iterator<OsuHitObjectResult> iter = hitResultList.iterator();
		while (iter.hasNext()) {
			OsuHitObjectResult hitResult = iter.next();
			if (hitResult.time + fadeDelay > trackPosition) {
				hitResults[hitResult.result].setAlpha(hitResult.alpha);
				hitResult.alpha = 1 - ((float) (trackPosition - hitResult.time) / fadeDelay);
				hitResults[hitResult.result].drawCentered(hitResult.x, hitResult.y);

				// hit lighting
				if (Options.isHitLightingEnabled() && hitResult.result != HIT_MISS &&
					hitResult.result != HIT_SLIDER30 && hitResult.result != HIT_SLIDER10) {
					float scale = 1f + ((trackPosition - hitResult.time) / (float) fadeDelay);
					Image scaledLighting  = GameImage.LIGHTING.getImage().getScaledCopy(scale);
					Image scaledLighting1 = GameImage.LIGHTING1.getImage().getScaledCopy(scale);
					scaledLighting.setAlpha(hitResult.alpha);
					scaledLighting1.setAlpha(hitResult.alpha);

					scaledLighting.draw(hitResult.x - (scaledLighting.getWidth() / 2f),
							hitResult.y - (scaledLighting.getHeight() / 2f), hitResult.color);
					scaledLighting1.draw(hitResult.x - (scaledLighting1.getWidth() / 2f),
							hitResult.y - (scaledLighting1.getHeight() / 2f), hitResult.color);
				}
			} else
				iter.remove();
		}
	}

	/**
	 * Changes health by a given percentage, modified by drainRate.
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
	 * Returns health percentage.
	 */
	public float getHealth() { return health; }

	/**
	 * Returns false if health is zero.
	 * If "No Fail" or "Auto" mods are active, this will always return true.
	 */
	public boolean isAlive() {
		return (health > 0f || GameMod.NO_FAIL.isActive() || GameMod.AUTO.isActive());
	}

	/**
	 * Changes score by a raw value (not affected by other modifiers).
	 */
	public void changeScore(int value) { score += value; }

	/**
	 * Returns the raw score percentage.
	 * @param hit300 the number of 300s
	 * @param hit100 the number of 100s
	 * @param hit50 the number of 50s
	 * @param miss the number of misses
	 * @return the percentage
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
	 * @return the current Grade
	 */
	public static Grade getGrade(int hit300, int hit100, int hit50, int miss) {
		int objectCount = hit300 + hit100 + hit50 + miss;
		if (objectCount < 1)  // avoid division by zero
			return Grade.NULL;

		// TODO: silvers
		float percent = getScorePercent(hit300, hit100, hit50, miss);
		float hit300ratio = hit300 * 100f / objectCount;
		float hit50ratio  = hit50 * 100f / objectCount;
		boolean noMiss    = (miss == 0);
		if (percent >= 100f)
			return Grade.SS;
		else if (hit300ratio >= 90f && hit50ratio < 1.0f && noMiss)
			return Grade.S;
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
	 * or Grade.NULL if no objects have been processed.
	 */
	private Grade getGrade() {
		return getGrade(
			hitResultCount[HIT_300], hitResultCount[HIT_100],
			hitResultCount[HIT_50], hitResultCount[HIT_MISS]
		);
	}

	/**
	 * Updates the score, health, and combo burst displays based on a delta value.
	 * @param delta the delta interval since the last call
	 */
	public void updateDisplays(int delta) {
		// score display
		if (scoreDisplay < score) {
			scoreDisplay += (score - scoreDisplay) * delta / 50 + 1;
			if (scoreDisplay > score)
				scoreDisplay = score;
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
				comboBurstX += (delta / 2f);
				if (comboBurstX > leftX)
					comboBurstX = leftX;
			} else if (comboBurstX > rightX) {
				comboBurstX -= (delta / 2f);
				if (comboBurstX < rightX)
					comboBurstX = rightX;
			} else if (comboBurstAlpha > 0f) {
				comboBurstAlpha -= (delta / 1200f);
				if (comboBurstAlpha < 0f)
					comboBurstAlpha = 0f;
			}
		}
	}

	/**
	 * Increases the combo streak by one.
	 */
	private void incrementComboStreak() {
		combo++;
		if (combo > comboMax)
			comboMax = combo;

		// combo bursts (at 30, 60, 100+50x)
		if (Options.isComboBurstEnabled() &&
			(combo == 30 || combo == 60 || (combo >= 100 && combo % 50 == 0))) {
			if (combo == 30)
				comboBurstIndex = 0;
			else
				comboBurstIndex = (comboBurstIndex + 1) % comboBurstImages.length;
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
		if (combo >= 20)
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
	 * @param hitSound the object's hit sound
	 */
	public void sliderTickResult(int time, int result, float x, float y, byte hitSound) {
		int hitValue = 0;
		switch (result) {
		case HIT_SLIDER30:
			hitValue = 30;
			incrementComboStreak();
			changeHealth(1f);
			SoundController.playHitSound(hitSound);
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
				hitResultList.add(new OsuHitObjectResult(time, result, x, y, null));
		}
	}

	/**
	 * Handles a hit result.
	 * @param time the object start time
	 * @param result the hit result (HIT_* constants)
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param color the combo color
	 * @param end true if this is the last hit object in the combo
	 * @param hitSound the object's hit sound
	 */
	public void hitResult(int time, int result, float x, float y, Color color,
			boolean end, byte hitSound) {
		int hitValue = 0;
		boolean perfectHit = false;
		switch (result) {
		case HIT_300:
			perfectHit = true;
			hitValue = 300;
			changeHealth(5f);
			break;
		case HIT_100:
			hitValue = 100;
			changeHealth(2f);
			comboEnd |= 1;
			break;
		case HIT_50:
			hitValue = 50;
			comboEnd |= 2;
			break;
		case HIT_MISS:
			hitValue = 0;
			changeHealth(-10f);
			comboEnd |= 2;
			resetComboStreak();
			break;
		default:
			return;
		}
		if (hitValue > 0) {
			SoundController.playHitSound(hitSound);

			/**
			 * [SCORE FORMULA]
			 * Score = Hit Value + Hit Value * (Combo * Difficulty * Mod) / 25
			 * - Hit Value: hit result (50, 100, 300), slider ticks, spinner bonus
			 * - Combo: combo before this hit - 1 (minimum 0)
			 * - Difficulty: the beatmap difficulty
			 * - Mod: mod multipliers
			 */
			score += (hitValue + (hitValue * (Math.max(combo - 1, 0) * difficulty * GameMod.getScoreMultiplier()) / 25));
			incrementComboStreak();
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

		if (perfectHit && !Options.isPerfectHitBurstEnabled())
			;  // hide perfect hit results
		else
			hitResultList.add(new OsuHitObjectResult(time, result, x, y, color));
	}

	/**
	 * Returns a ScoreData object encapsulating all game data.
	 * If score data already exists, the existing object will be returned
	 * (i.e. this will not overwrite existing data).
	 * @param osu the OsuFile
	 * @return the ScoreData object
	 */
	public ScoreData getScoreData(OsuFile osu) {
		if (scoreData != null)
			return scoreData;

		scoreData = new ScoreData();
		scoreData.timestamp = System.currentTimeMillis() / 1000L;
		scoreData.MID = osu.beatmapID;
		scoreData.MSID = osu.beatmapSetID;
		scoreData.title = osu.title;
		scoreData.artist = osu.artist;
		scoreData.creator = osu.creator;
		scoreData.version = osu.version;
		scoreData.hit300 = hitResultCount[HIT_300];
		scoreData.hit100 = hitResultCount[HIT_100];
		scoreData.hit50 = hitResultCount[HIT_50];
		scoreData.geki = hitResultCount[HIT_300G];
		scoreData.katu = hitResultCount[HIT_300K] + hitResultCount[HIT_100K];
		scoreData.miss = hitResultCount[HIT_MISS];
		scoreData.score = score;
		scoreData.combo = comboMax;
		scoreData.perfect = (comboMax == fullObjectCount);
		int mods = 0;
		for (GameMod mod : GameMod.values()) {
			if (mod.isActive())
				mods |= mod.getBit();
		}
		scoreData.mods = mods;
		return scoreData;
	}

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
		hitErrorList.add(new HitErrorInfo(time, x, y, timeDiff));
	}
}
