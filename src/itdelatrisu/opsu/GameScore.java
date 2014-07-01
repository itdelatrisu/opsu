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

import itdelatrisu.opsu.states.Game;
import itdelatrisu.opsu.states.Options;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.util.Log;

/**
 * Holds score data and renders all score-related elements.
 */
public class GameScore {
	/**
	 * Letter grades.
	 */
	public static final int
		GRADE_SS  = 0,
		GRADE_SSH = 1,   // silver
		GRADE_S   = 2,
		GRADE_SH  = 3,   // silver
		GRADE_A   = 4,
		GRADE_B   = 5,
		GRADE_C   = 6,
		GRADE_D   = 7,
		GRADE_MAX = 8;   // not a grade

	/**
	 * Hit result types.
	 */
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

	/**
	 * Hit result-related images (indexed by HIT_* constants).
	 */
	private Image[] hitResults;

	/**
	 * Counts of each hit result so far.
	 */
	private int[] hitResultCount;

	/**
	 * Total number of hit objects so far, not including Katu/Geki (for calculating grade).
	 */
	private int objectCount;

	/**
	 * Total objects including slider hits/ticks (for determining Full Combo status).
	 */
	private int fullObjectCount;

	/**
	 * The current combo streak.
	 */
	private int combo;

	/**
	 * The max combo streak obtained.
	 */
	private int comboMax;

	/**
	 * Hit result types accumulated this streak (bitmask), for Katu/Geki status.
	 * <ul>
	 * <li>&1: 100
	 * <li>&2: 50/Miss
	 * </ul>
	 */
	private byte comboEnd;

	/**
	 * Combo burst images.
	 */
	private Image[] comboBurstImages;

	/**
	 * Index of the current combo burst image.
	 */
	private int comboBurstIndex;

	/**
	 * Alpha level of the current combo burst image (for fade out).
	 */
	private float comboBurstAlpha;

	/**
	 * Current x coordinate of the combo burst image (for sliding animation).
	 */
	private int comboBurstX;

	/**
	 * List of hit result objects associated with hit objects.
	 */
	private LinkedList<OsuHitObjectResult> hitResultList;

	/**
	 * Hit result helper class.
	 */
	private class OsuHitObjectResult {
		public int time;                // object start time
		public int result;              // hit result
		public float x, y;              // object coordinates
		public Color color;             // combo color
		public float alpha = 1f;        // alpha level (for fade out)

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

	/**
	 * Current game score.
	 */
	private long score;

	/**
	 * Current health bar percentage.
	 */
	private float health;

	/**
	 * Beatmap HPDrainRate value. (0:easy ~ 10:hard)
	 */
	private float drainRate = 5f;

	/**
	 * Beatmap OverallDifficulty value. (0:easy ~ 10:hard)
	 */
	private float difficulty = 5f;

	/**
	 * Scorebar-related images.
	 */
	private Image
		bgImage,             // background (always rendered)
		colourImage,         // health bar (cropped)
		kiImage,             // end image (50~100% health)
		kiDangerImage,       // end image (25~50% health)
		kiDanger2Image;      // end image (0~25% health)

	/**
	 * Ranking screen images.
	 */
	private Image 
		rankingPanel,         // panel to display text in
		perfectImage,         // display if full combo
		rankingImage,         // styled text "Ranking"
		comboImage,           // styled text "Combo"
		accuracyImage;        // styled text "Accuracy"

	/**
	 * Default text symbol images.
	 */
	private Image[] defaultSymbols;

	/**
	 * Score text symbol images.
	 */
	private HashMap<Character, Image> scoreSymbols;

	/**
	 * Letter grade images (large and small sizes).
	 */
	private Image[] gradesLarge, gradesSmall;

	/**
	 * Lighting effects, displayed behind hit object results (optional).
	 */
	private Image lighting, lighting1;

	/**
	 * Container dimensions.
	 */
	private int width, height;

	/**
	 * Constructor.
	 * @param width container width
	 * @param height container height
	 */
	public GameScore(int width, int height) {
		this.width = width;
		this.height = height;

		hitResults = new Image[HIT_MAX];
		defaultSymbols = new Image[10];
		scoreSymbols = new HashMap<Character, Image>(14);
		gradesLarge = new Image[GRADE_MAX];
		gradesSmall = new Image[GRADE_MAX];
		comboBurstImages = new Image[4];

		clear();

		try {
			initializeImages();
		} catch (Exception e) {
			Log.error("Failed to initialize images.", e);
		}
	}

	/**
	 * Clears all data and re-initializes object.
	 */
	public void clear() {
		score = 0;
		health = 100f;
		hitResultCount = new int[HIT_MAX];
		hitResultList = new LinkedList<OsuHitObjectResult>();
		objectCount = 0;
		fullObjectCount = 0;
		combo = 0;
		comboMax = 0;
		comboEnd = 0;
		comboBurstIndex = -1;
	}

	/**
	 * Initialize all images tied to this object.
	 * @throws SlickException
	 */
	private void initializeImages() throws SlickException {
		// scorebar
		setScorebarImage(
				new Image("scorebar-bg.png"),
				new Image("scorebar-colour.png"),
				new Image("scorebar-ki.png"),
				new Image("scorebar-kidanger.png"),
				new Image("scorebar-kidanger2.png")
		);

		// text symbol images
		for (int i = 0; i <= 9; i++) {
			defaultSymbols[i] = new Image(String.format("default-%d.png", i));
			scoreSymbols.put(Character.forDigit(i, 10), new Image(String.format("score-%d.png", i)));
		}
		scoreSymbols.put(',', new Image("score-comma.png"));
		scoreSymbols.put('.', new Image("score-dot.png"));
		scoreSymbols.put('%', new Image("score-percent.png"));
		scoreSymbols.put('x', new Image("score-x.png"));

		// hit result images
		hitResults[HIT_MISS]     = new Image("hit0.png");
		hitResults[HIT_50]       = new Image("hit50.png");
		hitResults[HIT_100]      = new Image("hit100.png");
		hitResults[HIT_300]      = new Image("hit300.png");
		hitResults[HIT_100K]     = new Image("hit100k.png");
		hitResults[HIT_300K]     = new Image("hit300k.png");
		hitResults[HIT_300G]     = new Image("hit300g.png");
		hitResults[HIT_SLIDER10] = new Image("sliderpoint10.png");
		hitResults[HIT_SLIDER30] = new Image("sliderpoint30.png");

		// combo burst images
		for (int i = 0; i <= 3; i++)
			comboBurstImages[i] = new Image(String.format("comboburst-%d.png", i));

		// lighting image
		try {
			lighting  = new Image("lighting.png");
			lighting1 = new Image("lighting1.png");
		} catch (Exception e) {
			// optional
		}

		// letter grade images
		String[] grades = { "X", "XH", "S", "SH", "A", "B", "C", "D" };
		for (int i = 0; i < grades.length; i++) {
			gradesLarge[i] = new Image(String.format("ranking-%s.png", grades[i]));
			gradesSmall[i] = new Image(String.format("ranking-%s-small.png", grades[i]));
		}

		// ranking screen elements
		setRankingImage(
			new Image("ranking-panel.png"),
			new Image("ranking-perfect.png"),
			new Image("ranking-title.png"),
			new Image("ranking-maxcombo.png"),
			new Image("ranking-accuracy.png")
		);
	}

	/**
	 * Sets a background, health bar, and end image.
	 * @param bgImage background image
	 * @param colourImage health bar image
	 * @param kiImage end image
	 */
	public void setScorebarImage(Image bg, Image colour,
			Image ki, Image kiDanger, Image kiDanger2) {
		int bgWidth = width / 2;
		this.bgImage        = bg.getScaledCopy(bgWidth, bg.getHeight());
		this.colourImage    = colour.getScaledCopy(bgWidth, colour.getHeight());
		this.kiImage        = ki;
		this.kiDangerImage  = kiDanger;
		this.kiDanger2Image = kiDanger2;
	}

	/**
	 * Sets a ranking panel, full combo, and ranking/combo/accuracy text image.
	 * @param rankingPanel ranking panel image
	 * @param perfectImage full combo image
	 * @param rankingImage styled text "Ranking"
	 * @param comboImage styled text "Combo"
	 * @param accuracyImage styled text "Accuracy"
	 */
	public void setRankingImage(Image rankingPanel, Image perfectImage,
			Image rankingImage, Image comboImage, Image accuracyImage) {
		this.rankingPanel = rankingPanel.getScaledCopy((height * 0.63f) / rankingPanel.getHeight());
		this.perfectImage = perfectImage.getScaledCopy((height * 0.16f) / perfectImage.getHeight());
		this.rankingImage = rankingImage.getScaledCopy((height * 0.15f) / rankingImage.getHeight());
		this.comboImage = comboImage.getScaledCopy((height * 0.05f) / comboImage.getHeight());
		this.accuracyImage = accuracyImage.getScaledCopy((height * 0.05f) / accuracyImage.getHeight());
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
	 * Draws game elements: scorebar, score, score percentage, combo count, and combo burst.
	 * @param g the graphics context
	 * @param mapLength the length of the beatmap (in ms)
	 * @param breakPeriod if true, will not draw scorebar and combo elements, and will draw grade
	 * @param firstObject true if the first hit object's start time has not yet passed
	 */
	public void drawGameElements(Graphics g, int mapLength, boolean breakPeriod, boolean firstObject) {
		// score
		drawSymbolString(String.format("%08d", score),
				width - 2, 0, 1.0f, true);

		// score percentage
		String scorePercentage = String.format("%02.2f%%", getScorePercent());
		drawSymbolString(scorePercentage, width - 2, getScoreSymbolImage('0').getHeight(), 0.75f, true);

		// map progress circle
		g.setAntiAlias(true);
		g.setLineWidth(2f);
		g.setColor(Color.white);
		int circleX = width - (getScoreSymbolImage('0').getWidth() * scorePercentage.length());
		int circleY = getScoreSymbolImage('0').getHeight();
		float circleDiameter = getScoreSymbolImage('0').getHeight() * 0.75f;
		g.drawOval(circleX, circleY, circleDiameter, circleDiameter);

		int firstObjectTime = Game.getOsuFile().objects[0].time;
		int trackPosition = MusicController.getPosition();
		if (trackPosition > firstObjectTime) {
			g.fillArc(circleX, circleY, circleDiameter, circleDiameter,
					-90, -90 + (int) (360f * (trackPosition - firstObjectTime) / mapLength)
			);
		}

		if (!breakPeriod) {
			// scorebar
			float healthRatio = health / 100f;
			if (firstObject) {  // gradually move ki before map begins
				if (firstObjectTime >= 1500 && trackPosition < firstObjectTime - 500)
					healthRatio = (float) trackPosition / (firstObjectTime - 500);
			}
			bgImage.draw(0, 0);
			Image colourCropped = colourImage.getSubImage(0, 0, (int) (colourImage.getWidth() * healthRatio), colourImage.getHeight());
			colourCropped.draw(0, bgImage.getHeight() / 4f);
			if (health >= 50f)
				kiImage.drawCentered(colourCropped.getWidth(), kiImage.getHeight() / 2f);
			else if (health >= 25f)
				kiDangerImage.drawCentered(colourCropped.getWidth(), kiDangerImage.getHeight() / 2f);
			else
				kiDanger2Image.drawCentered(colourCropped.getWidth(), kiDanger2Image.getHeight() / 2f);

			// combo burst
			if (comboBurstIndex != -1 && comboBurstAlpha > 0f) {
				Image comboBurst = comboBurstImages[comboBurstIndex];
				comboBurst.setAlpha(comboBurstAlpha);
				comboBurstImages[comboBurstIndex].draw(comboBurstX, height - comboBurst.getHeight());
			}

			// combo count
			if (combo > 0)  // 0 isn't a combo
				drawSymbolString(String.format("%dx", combo), 10, height - 10 - getScoreSymbolImage('0').getHeight(), 1.0f, false);
		} else {
			// grade
			Image grade = gradesSmall[getGrade()];
			float gradeScale = circleY * 0.75f / grade.getHeight();
			gradesSmall[getGrade()].getScaledCopy(gradeScale).draw(
					circleX - grade.getWidth(), circleY
			);
		}
	}

	/**
	 * Draws ranking elements: score, results, ranking.
	 * @param g the graphics context
	 * @param width the width of the container
	 * @param height the height of the container
	 */
	public void drawRankingElements(Graphics g, int width, int height) {
		// grade
		Image grade = gradesLarge[getGrade()];
		float gradeScale = (height * 0.5f) / grade.getHeight();
		grade = grade.getScaledCopy(gradeScale);
		grade.draw(width - grade.getWidth(), height * 0.09f);

		// header & "Ranking" text
		float rankingHeight = (rankingImage.getHeight() * 0.75f) + 3;
		g.setColor(Utils.COLOR_BLACK_ALPHA);
		g.fillRect(0, 0, width, rankingHeight);
		rankingImage.draw((width * 0.97f) - rankingImage.getWidth(), 0);

		// ranking panel
		int rankingPanelWidth  = rankingPanel.getWidth();
		int rankingPanelHeight = rankingPanel.getHeight();
		rankingPanel.draw(0, rankingHeight - (rankingHeight / 10f));

		float symbolTextScale = (height / 15f) / getScoreSymbolImage('0').getHeight();
		float rankResultScale = (height * 0.03f) / hitResults[HIT_300].getHeight();

		// score
		drawSymbolString((score / 100000000 == 0) ? String.format("%08d", score) : Long.toString(score),
				(int) (width * 0.18f), height / 6, symbolTextScale, false);

		// result counts
		float resultInitialX = rankingPanelWidth * 0.20f;
		float resultInitialY = rankingHeight + (rankingPanelHeight * 0.27f) + (rankingHeight / 10f);
		float resultHitInitialX = rankingPanelWidth * 0.05f;
		float resultHitInitialY = resultInitialY + (getScoreSymbolImage('0').getHeight() * symbolTextScale);
		float resultOffsetX = rankingPanelWidth / 2f;
		float resultOffsetY = rankingPanelHeight * 0.2f;

		int[] rankDrawOrder = { HIT_300, HIT_300G, HIT_100, HIT_100K, HIT_50, HIT_MISS };
		int[] rankResultOrder = {
				hitResultCount[HIT_300], hitResultCount[HIT_300G],
				hitResultCount[HIT_100], hitResultCount[HIT_100K] + hitResultCount[HIT_300K],
				hitResultCount[HIT_50], hitResultCount[HIT_MISS]
		};

		for (int i = 0; i < rankDrawOrder.length; i += 2) {
			hitResults[rankDrawOrder[i]].getScaledCopy(rankResultScale).draw(
					resultHitInitialX, resultHitInitialY - (hitResults[rankDrawOrder[i]].getHeight() * rankResultScale) + (resultOffsetY * (i / 2)));
			hitResults[rankDrawOrder[i+1]].getScaledCopy(rankResultScale).draw(
					resultHitInitialX + resultOffsetX, resultHitInitialY - (hitResults[rankDrawOrder[i]].getHeight() * rankResultScale) + (resultOffsetY * (i / 2)));
			drawSymbolString(String.format("%dx", rankResultOrder[i]),
					(int) resultInitialX, (int) (resultInitialY + (resultOffsetY * (i / 2))), symbolTextScale, false);
			drawSymbolString(String.format("%dx", rankResultOrder[i+1]),
					(int) (resultInitialX + resultOffsetX), (int) (resultInitialY + (resultOffsetY * (i / 2))), symbolTextScale, false);
		}

		// combo and accuracy
		float textY = rankingHeight + (rankingPanelHeight * 0.87f) - (rankingHeight / 10f);
		float numbersX = comboImage.getWidth() * .07f;
		float numbersY = textY + comboImage.getHeight() * 0.7f;
		comboImage.draw(width * 0.01f, textY);
		accuracyImage.draw(rankingPanelWidth / 2f, textY);
		drawSymbolString(String.format("%dx", comboMax),
				(int) (width * 0.01f + numbersX), (int) numbersY, symbolTextScale, false);
		drawSymbolString(String.format("%02.2f%%", getScorePercent()),
				(int) (rankingPanelWidth / 2f + numbersX), (int) numbersY, symbolTextScale, false);

		// full combo
		if (combo == fullObjectCount)
			perfectImage.draw(width * 0.08f, (height * 0.99f) - perfectImage.getHeight());
	}

	/**
	 * Draws stored hit results and removes them from the list as necessary.
	 * @param trackPosition the current track position
	 */
	public void drawHitResults(int trackPosition) {
		int fadeDelay = 500;

		Iterator<OsuHitObjectResult> iter = hitResultList.iterator();
		while (iter.hasNext()) {
			OsuHitObjectResult hitResult = iter.next();
			if (hitResult.time + fadeDelay > trackPosition) {
				hitResults[hitResult.result].setAlpha(hitResult.alpha);
				hitResult.alpha = 1 - ((float) (trackPosition - hitResult.time) / fadeDelay);
				hitResults[hitResult.result].drawCentered(hitResult.x, hitResult.y);

				// hit lighting
				if (Options.isHitLightingEnabled() && lighting != null &&
					hitResult.result != HIT_MISS && hitResult.result != HIT_SLIDER30 && hitResult.result != HIT_SLIDER10) {
					float scale = 1f + ((trackPosition - hitResult.time) / (float) fadeDelay);
					Image scaledLighting  = lighting.getScaledCopy(scale);
					scaledLighting.draw(hitResult.x - (scaledLighting.getWidth() / 2f),
										hitResult.y - (scaledLighting.getHeight() / 2f),
										hitResult.color);
					if (lighting1 != null) {
						Image scaledLighting1 = lighting1.getScaledCopy(scale);
						scaledLighting1.draw(hitResult.x - (scaledLighting1.getWidth() / 2f),
								hitResult.y - (scaledLighting1.getHeight() / 2f),
								hitResult.color);
					}
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
		if (health < 0f)
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
		return (health > 0f ||
				Options.isModActive(Options.MOD_NO_FAIL) ||
				Options.isModActive(Options.MOD_AUTO));
	}

	/**
	 * Changes score by a raw value (not affected by other modifiers).
	 */
	public void changeScore(int value) { score += value; }

	/**
	 * Returns score percentage (raw score only).
	 */
	private float getScorePercent() {
		float percent = 0;
		if (objectCount > 0)
			percent = ((hitResultCount[HIT_50] * 50) + (hitResultCount[HIT_100] * 100)
					+ (hitResultCount[HIT_300] * 300)) / (objectCount * 300f) * 100f;
		return percent;
	}

	/**
	 * Returns (current) letter grade.
	 */
	private int getGrade() {
		if (objectCount < 1)  // avoid division by zero
			return GRADE_D;

		// TODO: silvers
		float percent = getScorePercent();
		float hit300ratio = hitResultCount[HIT_300] * 100f / objectCount;
		float hit50ratio  = hitResultCount[HIT_50] * 100f / objectCount;
		boolean noMiss    = (hitResultCount[HIT_MISS] == 0);
		if (percent >= 100f)
			return GRADE_SS;
		else if (hit300ratio >= 90f && hit50ratio < 1.0f && noMiss)
			return GRADE_S;
		else if ((hit300ratio >= 80f && noMiss) || hit300ratio >= 90f)
			return GRADE_A;
		else if ((hit300ratio >= 70f && noMiss) || hit300ratio >= 80f)
			return GRADE_B;
		else if (hit300ratio >= 60f)
			return GRADE_C;
		else
			return GRADE_D;
	}

	/**
	 * Updates combo burst data based on a delta value.
	 */
	public void updateComboBurst(int delta) {
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
			SoundController.playSound(SoundController.SOUND_COMBOBREAK);
		combo = 0;
		if (Options.isModActive(Options.MOD_SUDDEN_DEATH))
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
			SoundController.playHitSound(SoundController.HIT_SLIDERTICK);
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
		switch (result) {
		case HIT_300:
			hitValue = 300;
			changeHealth(5f);
			objectCount++;
			break;
		case HIT_100:
			hitValue = 100;
			changeHealth(2f);
			comboEnd |= 1;
			objectCount++;
			break;
		case HIT_50:
			hitValue = 50;
			comboEnd |= 2;
			objectCount++;
			break;
		case HIT_MISS:
			hitValue = 0;
			changeHealth(-10f);
			comboEnd |= 2;
			resetComboStreak();
			objectCount++;
			break;
		default:
			return;
		}
		if (hitValue > 0) {
			SoundController.playHitSound(hitSound);

			// game mod score multipliers
			float modMultiplier = 1f;
			if (Options.isModActive(Options.MOD_NO_FAIL))
				modMultiplier *= 0.5f;
			if (Options.isModActive(Options.MOD_HARD_ROCK))
				modMultiplier *= 1.06f;
			if (Options.isModActive(Options.MOD_SPUN_OUT))
				modMultiplier *= 0.9f;
			// not implemented:
			// EASY (0.5x), HALF_TIME (0.3x),
			// DOUBLE_TIME (1.12x), HIDDEN (1.06x), FLASHLIGHT (1.12x)

			/**
			 * [SCORE FORMULA]
			 * Score = Hit Value + Hit Value * (Combo * Difficulty * Mod) / 25
			 * - Hit Value: hit result (50, 100, 300), slider ticks, spinner bonus
			 * - Combo: combo before this hit - 1 (minimum 0)
			 * - Difficulty: the beatmap difficulty
			 * - Mod: mod multipliers
			 */
			score += (hitValue + (hitValue * (Math.max(combo - 1, 0) * difficulty * modMultiplier) / 25));
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

		hitResultList.add(new OsuHitObjectResult(time, result, x, y, color));
	}
}