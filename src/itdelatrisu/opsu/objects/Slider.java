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

package itdelatrisu.opsu.objects;

import itdelatrisu.opsu.GameScore;
import itdelatrisu.opsu.MusicController;
import itdelatrisu.opsu.OsuFile;
import itdelatrisu.opsu.OsuHitObject;
import itdelatrisu.opsu.states.Game;
import itdelatrisu.opsu.states.Options;

import org.newdawn.slick.Animation;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;

/**
 * Data type representing a slider object.
 */
public class Slider {
	/**
	 * Images related to sliders.
	 */
	private static Image
		sliderFollowCircle,  // slider follow circle
		reverseArrow,        // reverse arrow (for repeats)
		sliderTick;          // slider tick

	/**
	 * Slider ball animation.
	 */
	private static Animation sliderBall;

	/**
	 * Slider movement speed multiplier.
	 */
	private static float sliderMultiplier = 1.0f;

	/**
	 * Rate at which slider ticks are placed.
	 */
	private static float sliderTickRate = 1.0f;

	/**
	 * The associated OsuHitObject.
	 */
	private OsuHitObject hitObject;

	/**
	 * The associated Game object.
	 */
	private Game game;

	/**
	 * The associated GameScore object.
	 */
	private GameScore score;

	/**
	 * The color of this slider.
	 */
	private Color color;

	/**
	 * The underlying Bezier object.
	 */
	private Bezier bezier;

	/**
	 * The time duration of the slider, in milliseconds.
	 */
	private float sliderTime = 0f;
	
	/**
	 * The time duration of the slider including repeats, in milliseconds.
	 */
	private float sliderTimeTotal = 0f;

	/**
	 * Whether or not the result of the initial hit circle has been processed.
	 */
	private boolean sliderClicked = false;

	/**
	 * Whether or not to show the follow circle.
	 */
	private boolean followCircleActive = false;
	
	/**
	 * Whether or not the slider result ends the combo streak.
	 */
	private boolean comboEnd;

	/**
	 * The number of repeats that have passed so far.
	 */
	private int currentRepeats = 0;

	/**
	 * The t values of the slider ticks.
	 */
	private float[] ticksT;
	
	/**
	 * The tick index in the ticksT[] array.
	 */
	private int tickIndex = 0;
	
	/**
	 * Number of ticks hit and tick intervals so far.
	 */
	private int ticksHit = 0, tickIntervals = 1;

	/**
	 * Representation of a Bezier curve, the main component of a slider.
	 *
	 * @author Alex Gheorghiu (http://html5tutorial.com/how-to-draw-n-grade-bezier-curve-with-canvas-api/)
	 * @author pictuga (https://github.com/pictuga/osu-web)
	 */
	private class Bezier {
		/**
		 * The order of the Bezier curve.
		 */
		private int order;

		/**
		 * The step size (used for drawing),
		 */
		private float step;

		/**
		 * The curve points for drawing with step size given by 'step'.
		 */
		private float[] curveX, curveY;

		/**
		 * Constructor.
		 */
		public Bezier(float length) {
			this.order = hitObject.sliderX.length + 1;
			this.step = 5 / length;

			// calculate curve points for drawing
			int N = (int) (1 / step);
			this.curveX = new float[N];
			this.curveY = new float[N];
			float t = 0f;
			for (int i = 0; i < N; i++, t += step) {
				float[] c = pointAt(t);
				curveX[i] = c[0];
				curveY[i] = c[1];
			}
		}

		/**
		 * Returns the x coordinate of the control point at index i.
		 */
		private float getX(int i) {
			return (i == 0) ? hitObject.x : hitObject.sliderX[i - 1];
		}
	
		/**
		 * Returns the y coordinate of the control point at index i.
		 */
		private float getY(int i) {
			return (i == 0) ? hitObject.y : hitObject.sliderY[i - 1];
		}
	
		/**
		 * Calculates the factorial of a number.
		 */
		private long factorial(int n) {
			return (n <= 1 || n > 20) ? 1 : n * factorial(n - 1);
		}
	
		/**
		 * Calculates the Bernstein polynomial.
		 * @param i the index
		 * @param n the degree of the polynomial (i.e. number of points)
		 * @param t the t value [0, 1]
		 */
		private double bernstein(int i, int n, float t) {
			return factorial(n) / (factorial(i) * factorial(n-i)) *
					Math.pow(t, i) * Math.pow(1-t, n-i);
		}
	
		/**
		 * Returns the point on the Bezier curve at a value t.
		 * For curves of order greater than 4, points will be generated along
		 * a path of overlapping cubic (at most) Beziers.
		 * @param t the t value [0, 1]
		 * @return the point [x, y]
		 */
		public float[] pointAt(float t) {
			float[] c = { 0f, 0f };
			int n = order - 1;
			if (n < 4) {  // normal curve
				for (int i = 0; i <= n; i++) {
		            c[0] += getX(i) * bernstein(i, n, t);
		            c[1] += getY(i) * bernstein(i, n, t);
				}
			} else {  // split curve into path
				// TODO: this is probably wrong...
				int segmentCount = (n / 3) + 1;
				int segment = (int) Math.floor(t * segmentCount);
				int startIndex = 3 * segment;
				int segmentOrder = Math.min(startIndex + 3, n) - startIndex;
				float segmentT = (t * segmentCount) - segment;
				for (int i = 0; i <= segmentOrder; i++) {
		            c[0] += getX(i + startIndex) * bernstein(i, segmentOrder, segmentT);
		            c[1] += getY(i + startIndex) * bernstein(i, segmentOrder, segmentT);
				}
			}
			return c;
		}

		/**
		 * Draws the full Bezier curve to the graphics context.
		 */
		public void draw() {
			Image hitCircle = Circle.getHitCircle();
			Image hitCircleOverlay = Circle.getHitCircleOverlay();

			// draw overlay and hit circle
			for (int i = curveX.length - 1; i >= 0; i--)
				drawCentered(hitCircleOverlay, curveX[i], curveY[i], Color.white);
			for (int i = curveX.length - 1; i >= 0; i--)
				drawCentered(hitCircle, curveX[i], curveY[i], color);
		}
	}

	/**
	 * Initializes the Slider data type with images and dimensions.
	 * @param container the game container
	 * @param circleSize the map's circleSize value
	 * @param osu the associated OsuFile object
	 * @throws SlickException
	 */
	public static void init(GameContainer container, byte circleSize, OsuFile osu) throws SlickException {
		int diameter = 96 - (circleSize * 8);
		diameter = diameter * container.getWidth() / 640;  // convert from Osupixels (640x480)

		sliderBall = new Animation();
		for (int i = 0; i <= 9; i++)
			sliderBall.addFrame(new Image(String.format("sliderb%d.png", i)).getScaledCopy(diameter * 118 / 128, diameter * 118 / 128), 60);
		sliderFollowCircle = new Image("sliderfollowcircle.png").getScaledCopy(diameter * 259 / 128, diameter * 259 / 128);
		reverseArrow = new Image("reversearrow.png").getScaledCopy(diameter, diameter);
		sliderTick = new Image("sliderscorepoint.png").getScaledCopy(diameter / 4, diameter / 4);

		sliderMultiplier = osu.sliderMultiplier;
		sliderTickRate = osu.sliderTickRate;
	}

	/**
	 * Constructor.
	 * @param hitObject the associated OsuHitObject
	 * @param game the associated Game object
	 * @param score the associated GameScore object
	 * @param color the color of this circle
	 * @param comboEnd true if this is the last hit object in the combo
	 */
	public Slider(OsuHitObject hitObject, Game game, GameScore score, Color color, boolean comboEnd) {
		this.hitObject = hitObject;
		this.game = game;
		this.score = score;
		this.color = color;
		this.comboEnd = comboEnd;

		this.bezier = new Bezier(hitObject.pixelLength);

		// calculate slider time and ticks upon first update call
	}

	/**
	 * Draws the slider to the graphics context.
	 * @param trackPosition the current track position
	 * @param currentObject true if this is the current hit object
	 */
	public void draw(int trackPosition, boolean currentObject) {
		int timeDiff = hitObject.time - trackPosition;

		Image hitCircleOverlay = Circle.getHitCircleOverlay();
		Image hitCircle = Circle.getHitCircle();

		// bezier
		bezier.draw();

		// ticks
		if (currentObject && ticksT != null) {
			for (int i = 0; i < ticksT.length; i++) {
				float[] c = bezier.pointAt(ticksT[i]);
				sliderTick.drawCentered(c[0], c[1]);
			}
		}

		// end circle
		int lastIndex = hitObject.sliderX.length - 1;
		drawCentered(hitCircleOverlay, hitObject.sliderX[lastIndex], hitObject.sliderY[lastIndex], Color.white);
		drawCentered(hitCircle, hitObject.sliderX[lastIndex], hitObject.sliderY[lastIndex], color);

		// start circle
		drawCentered(hitCircleOverlay, hitObject.x, hitObject.y, Color.white);
		drawCentered(hitCircle, hitObject.x, hitObject.y, color);
		if (sliderClicked)
			;  // don't draw current combo number if already clicked
		else
			score.drawSymbolNumber(hitObject.comboNumber, hitObject.x, hitObject.y,
					hitCircle.getWidth() * 0.40f / score.getDefaultSymbolImage(0).getHeight());

		// repeats
		if (hitObject.repeat - 1 > currentRepeats) {
			if (currentRepeats % 2 == 0)  // last circle
				reverseArrow.drawCentered(hitObject.sliderX[lastIndex], hitObject.sliderY[lastIndex]);
			else  // first circle
				reverseArrow.drawCentered(hitObject.x, hitObject.y);
		}

		if (timeDiff >= 0) {
			// approach circle
			float approachScale = 1 + (timeDiff * 2f / game.getApproachTime());
			drawCentered(Circle.getApproachCircle().getScaledCopy(approachScale), hitObject.x, hitObject.y, color);
		} else {
			float[] c = bezier.pointAt(getT(trackPosition, false));

			// slider ball
			drawCentered(sliderBall, c[0], c[1]);

			// follow circle
			if (followCircleActive)
				sliderFollowCircle.drawCentered(c[0], c[1]);
		}
	}

	/**
	 * Draws an image based on its center with a color filter.
	 */
	private void drawCentered(Image img, float x, float y, Color color) {
		img.draw(x - (img.getWidth() / 2f), y - (img.getHeight() / 2f), color);
	}

	/**
	 * Draws an animation based on its center with a color filter.
	 */
	private static void drawCentered(Animation anim, float x, float y) {
		anim.draw(x - (anim.getWidth() / 2f), y - (anim.getHeight() / 2f));
	}

	/**
	 * Calculates the slider hit result.
	 * @param time the hit object time (difference between track time)
	 * @param lastCircleHit true if the cursor was held within the last circle
	 * @return the hit result (GameScore.HIT_* constants)
	 */
	public int hitResult() {
		int lastIndex = hitObject.sliderX.length - 1;
		float tickRatio = (float) ticksHit / tickIntervals;

		int result;
		if (tickRatio >= 1.0f)
			result = GameScore.HIT_300;
		else if (tickRatio >= 0.5f)
			result = GameScore.HIT_100;
		else if (tickRatio > 0f)
			result = GameScore.HIT_50;
		else
			result = GameScore.HIT_MISS;

		if (currentRepeats % 2 == 0)  // last circle
			score.hitResult(hitObject.time + (int) sliderTimeTotal, result,
					hitObject.sliderX[lastIndex], hitObject.sliderY[lastIndex], color, comboEnd);
		else  // first circle
			score.hitResult(hitObject.time + (int) sliderTimeTotal, result,
					hitObject.x, hitObject.y, color, comboEnd);

		return result;
	}

	/**
	 * Processes a mouse click.
	 * @param x the x coordinate of the mouse
	 * @param y the y coordinate of the mouse
	 * @param comboEnd if this is the last object in the combo
	 * @return true if a hit result was processed
	 */
	public boolean mousePressed(int x, int y) {
		if (sliderClicked)  // first circle already processed
			return false;

		double distance = Math.hypot(hitObject.x - x, hitObject.y - y);
		int circleRadius = Circle.getHitCircle().getWidth() / 2;
		if (distance < circleRadius) {
			int trackPosition = MusicController.getPosition();
			int timeDiff = Math.abs(trackPosition - hitObject.time);
			int[] hitResultOffset = game.getHitResultOffsets();
			
			int result = -1;
			if (timeDiff < hitResultOffset[GameScore.HIT_50]) {
				result = GameScore.HIT_SLIDER30;
				ticksHit++;
			} else if (timeDiff < hitResultOffset[GameScore.HIT_MISS])
				result = GameScore.HIT_MISS;
			//else not a hit

			if (result > -1) {
				sliderClicked = true;
				score.sliderTickResult(hitObject.time, result, hitObject.x, hitObject.y);
				return true;
			}
		}
		return false;
	}

	/**
	 * Updates the slider object.
	 * @param overlap true if the next object's start time has already passed
	 * @param delta the delta interval since the last call
	 * @param mouseX the x coordinate of the mouse
	 * @param mouseY the y coordinate of the mouse
	 * @return true if slider ended
	 */
	public boolean update(boolean overlap, int delta, int mouseX, int mouseY) {
		// slider time and tick calculations
		if (sliderTimeTotal == 0f) {
			// slider time
			this.sliderTime = game.getBeatLength() * (hitObject.pixelLength / sliderMultiplier) / 100f;
			this.sliderTimeTotal = sliderTime * hitObject.repeat;

			// ticks
			float tickLengthDiv = 100f * sliderMultiplier / sliderTickRate / game.getTimingPointMultiplier();
			int tickCount = (int) Math.ceil(hitObject.pixelLength / tickLengthDiv) - 1;
			if (tickCount > 0) {
				this.ticksT = new float[tickCount];
				float tickTOffset = 1f / (tickCount + 1);
				float t = tickTOffset;
				for (int i = 0; i < tickCount; i++, t += tickTOffset)
					ticksT[i] = t;
			}
		}

		int trackPosition = MusicController.getPosition();
		int[] hitResultOffset = game.getHitResultOffsets();	
		int lastIndex = hitObject.sliderX.length - 1;
		boolean isAutoMod = Options.isModActive(Options.MOD_AUTO);

		if (!sliderClicked) {
			// start circle time passed
			if (trackPosition > hitObject.time + hitResultOffset[GameScore.HIT_50]) {
				sliderClicked = true;
				if (isAutoMod) {  // "auto" mod: catch any missed notes due to lag
					ticksHit++;
					score.sliderTickResult(hitObject.time, GameScore.HIT_SLIDER30, hitObject.x, hitObject.y);
				} else
					score.sliderTickResult(hitObject.time, GameScore.HIT_MISS, hitObject.x, hitObject.y);
			}

			// "auto" mod: send a perfect hit result
			else if (isAutoMod) {
				if (Math.abs(trackPosition - hitObject.time) < hitResultOffset[GameScore.HIT_300]) {
					ticksHit++;
					sliderClicked = true;
					score.sliderTickResult(hitObject.time, GameScore.HIT_SLIDER30, hitObject.x, hitObject.y);
				}
			}
		}


		// end of slider
		if (overlap || trackPosition > hitObject.time + sliderTimeTotal) {
			tickIntervals++;

			// "auto" mod: send a perfect hit result
			if (isAutoMod)
				ticksHit++;

			// check if cursor pressed and within end circle
			else if (game.isInputKeyPressed()) {
				double distance = Math.hypot(hitObject.sliderX[lastIndex] - mouseX, hitObject.sliderY[lastIndex] - mouseY);
				int followCircleRadius = sliderFollowCircle.getWidth() / 2;
				if (distance < followCircleRadius)
					ticksHit++;
			}
			
			// calculate and send slider result
			hitResult();
			return true;
		}

		// repeats
		boolean isNewRepeat = false;
		if (hitObject.repeat - 1 > currentRepeats) {
			float t = getT(trackPosition, true);
			if (Math.floor(t) > currentRepeats) {
				currentRepeats++;
				tickIntervals++;
				isNewRepeat = true;
			}
		}

		// ticks
		boolean isNewTick = false;
		if (ticksT != null &&
			tickIntervals < (ticksT.length * (currentRepeats + 1)) + hitObject.repeat &&
			tickIntervals < (ticksT.length * hitObject.repeat) + hitObject.repeat) {
			float t = getT(trackPosition, true);
			if (t - Math.floor(t) >= ticksT[tickIndex]) {
				tickIntervals++;
				tickIndex = (tickIndex + 1) % ticksT.length;
				isNewTick = true;
			}
		}

		// holding slider...
		float[] c = bezier.pointAt(getT(trackPosition, false));
		double distance = Math.hypot(c[0] - mouseX, c[1] - mouseY);
		int followCircleRadius = sliderFollowCircle.getWidth() / 2;
		if ((game.isInputKeyPressed() && distance < followCircleRadius) || isAutoMod) {
			// mouse pressed and within follow circle
			followCircleActive = true;
			score.changeHealth(delta / 200f);

			// held during new repeat
			if (isNewRepeat) {
				ticksHit++;
				if (currentRepeats % 2 > 0)  // last circle
					score.sliderTickResult(trackPosition, GameScore.HIT_SLIDER30,
							hitObject.sliderX[lastIndex], hitObject.sliderY[lastIndex]);
				else  // first circle
					score.sliderTickResult(trackPosition, GameScore.HIT_SLIDER30,
							c[0], c[1]);
			}

			// held during new tick
			if (isNewTick) {
				ticksHit++;
				score.sliderTickResult(trackPosition, GameScore.HIT_SLIDER10, c[0], c[1]);
			}
		} else {
			followCircleActive = false;

			if (isNewRepeat)
				score.sliderTickResult(trackPosition, GameScore.HIT_MISS, 0, 0);
			if (isNewTick)
				score.sliderTickResult(trackPosition, GameScore.HIT_MISS, 0, 0);
		}

		return false;
	}

	/**
	 * Returns the t value based on the given track position.
	 * @param trackPosition the current track position
	 * @param raw if false, ensures that the value lies within [0, 1] by looping repeats
	 * @return the t value: raw [0, repeats] or looped [0, 1]
	 */
	public float getT(int trackPosition, boolean raw) {
		float t = (trackPosition - hitObject.time) / sliderTime;
		if (raw)
			return t;
		else {
			float floor = (float) Math.floor(t);
			return (floor % 2 == 0) ? t - floor : floor + 1 - t;
		}
	}
}