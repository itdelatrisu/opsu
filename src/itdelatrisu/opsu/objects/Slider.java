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

package itdelatrisu.opsu.objects;

import itdelatrisu.opsu.GameData;
import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.GameMod;
import itdelatrisu.opsu.OsuFile;
import itdelatrisu.opsu.OsuHitObject;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.objects.curves.CircumscribedCircle;
import itdelatrisu.opsu.objects.curves.Curve;
import itdelatrisu.opsu.objects.curves.LinearBezier;
import itdelatrisu.opsu.states.Game;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;

/**
 * Data type representing a slider object.
 */
public class Slider implements HitObject {
	/** Slider ball frames. */
	private static Image[] sliderBallImages;

	/** Slider movement speed multiplier. */
	private static float sliderMultiplier = 1.0f;

	/** Rate at which slider ticks are placed. */
	private static float sliderTickRate = 1.0f;

	/** The amount of time, in milliseconds, to fade in the slider. */
	private static final int FADE_IN_TIME = 375;

	/** The associated OsuHitObject. */
	private OsuHitObject hitObject;

	/** The scaled starting x, y coordinates. */
	protected float x, y;

	/** The associated Game object. */
	private Game game;

	/** The associated GameData object. */
	private GameData data;

	/** The color of this slider. */
	private Color color;

	/** The underlying Curve. */
	private Curve curve;

	/** The time duration of the slider, in milliseconds. */
	private float sliderTime = 0f;

	/** The time duration of the slider including repeats, in milliseconds. */
	private float sliderTimeTotal = 0f;

	/** Whether or not the result of the initial/final hit circles have been processed. */
	private boolean sliderClickedInitial = false, sliderClickedFinal = false;

	/** Whether or not to show the follow circle. */
	private boolean followCircleActive = false;

	/** Whether or not the slider result ends the combo streak. */
	private boolean comboEnd;

	/** The number of repeats that have passed so far. */
	private int currentRepeats = 0;

	/** The t values of the slider ticks. */
	private float[] ticksT;

	/** The tick index in the ticksT[] array. */
	private int tickIndex = 0;

	/** Number of ticks hit and tick intervals so far. */
	private int ticksHit = 0, tickIntervals = 1;

	/** Container dimensions. */
	private static int containerWidth, containerHeight;

	/**
	 * Initializes the Slider data type with images and dimensions.
	 * @param container the game container
	 * @param circleSize the map's circleSize value
	 * @param osu the associated OsuFile object
	 */
	public static void init(GameContainer container, float circleSize, OsuFile osu) {
		containerWidth = container.getWidth();
		containerHeight = container.getHeight();

		int diameter = (int) (104 - (circleSize * 8));
		diameter = (int) (diameter * OsuHitObject.getXMultiplier());  // convert from Osupixels (640x480)

		// slider ball
		if (GameImage.SLIDER_BALL.hasSkinImages() ||
		    (!GameImage.SLIDER_BALL.hasSkinImage() && GameImage.SLIDER_BALL.getImages() != null))
			sliderBallImages = GameImage.SLIDER_BALL.getImages();
		else
			sliderBallImages = new Image[]{ GameImage.SLIDER_BALL.getImage() };
		for (int i = 0; i < sliderBallImages.length; i++)
			sliderBallImages[i] = sliderBallImages[i].getScaledCopy(diameter * 118 / 128, diameter * 118 / 128);

		GameImage.SLIDER_FOLLOWCIRCLE.setImage(GameImage.SLIDER_FOLLOWCIRCLE.getImage().getScaledCopy(diameter * 259 / 128, diameter * 259 / 128));
		GameImage.REVERSEARROW.setImage(GameImage.REVERSEARROW.getImage().getScaledCopy(diameter, diameter));
		GameImage.SLIDER_TICK.setImage(GameImage.SLIDER_TICK.getImage().getScaledCopy(diameter / 4, diameter / 4));

		sliderMultiplier = osu.sliderMultiplier;
		sliderTickRate = osu.sliderTickRate;
	}

	/**
	 * Constructor.
	 * @param hitObject the associated OsuHitObject
	 * @param game the associated Game object
	 * @param data the associated GameData object
	 * @param color the color of this slider
	 * @param comboEnd true if this is the last hit object in the combo
	 */
	public Slider(OsuHitObject hitObject, Game game, GameData data, Color color, boolean comboEnd) {
		this.hitObject = hitObject;
		this.game = game;
		this.data = data;
		this.color = color;
		this.comboEnd = comboEnd;
		updatePosition();

		// slider time calculations
		this.sliderTime = game.getBeatLength() * (hitObject.getPixelLength() / sliderMultiplier) / 100f;
		this.sliderTimeTotal = sliderTime * hitObject.getRepeatCount();

		// ticks
		float tickLengthDiv = 100f * sliderMultiplier / sliderTickRate / game.getTimingPointMultiplier();
		int tickCount = (int) Math.ceil(hitObject.getPixelLength() / tickLengthDiv) - 1;
		if (tickCount > 0) {
			this.ticksT = new float[tickCount];
			float tickTOffset = 1f / (tickCount + 1);
			float t = tickTOffset;
			for (int i = 0; i < tickCount; i++, t += tickTOffset)
				ticksT[i] = t;
		}
	}

	@Override
	public void draw(Graphics g, int trackPosition) {
		int timeDiff = hitObject.getTime() - trackPosition;
		float scale = timeDiff / (float) game.getApproachTime();
		float fadeinScale = (timeDiff - game.getApproachTime() + FADE_IN_TIME) / (float) FADE_IN_TIME;
		float approachScale = 1 + scale * 3;
		float alpha = Utils.clamp(1 - fadeinScale, 0, 1);

		float oldAlpha = Utils.COLOR_WHITE_FADE.a;
		Utils.COLOR_WHITE_FADE.a = color.a = alpha;

		// curve
		curve.draw();

		// ticks
		if (ticksT != null) {
			Image tick = GameImage.SLIDER_TICK.getImage();
			for (int i = 0; i < ticksT.length; i++) {
				float[] c = curve.pointAt(ticksT[i]);
				tick.drawCentered(c[0], c[1], Utils.COLOR_WHITE_FADE);
			}
		}

		Image hitCircleOverlay = GameImage.HITCIRCLE_OVERLAY.getImage();
		Image hitCircle = GameImage.HITCIRCLE.getImage();

		// end circle
		float[] endPos = curve.pointAt(1);
		hitCircle.drawCentered(endPos[0], endPos[1], color);
		hitCircleOverlay.drawCentered(endPos[0], endPos[1], Utils.COLOR_WHITE_FADE);

		// start circle
		hitCircle.drawCentered(x, y, color);
		hitCircleOverlay.drawCentered(x, y, Utils.COLOR_WHITE_FADE);
		if (sliderClickedInitial)
			;  // don't draw current combo number if already clicked
		else
			data.drawSymbolNumber(hitObject.getComboNumber(), x, y,
					hitCircle.getWidth() * 0.40f / data.getDefaultSymbolImage(0).getHeight(), alpha);

		// repeats
		for (int tcurRepeat = currentRepeats; tcurRepeat <= currentRepeats + 1; tcurRepeat++) {
			if (hitObject.getRepeatCount() - 1 > tcurRepeat) {
				Image arrow = GameImage.REVERSEARROW.getImage();
				if (tcurRepeat != currentRepeats) {
					if (sliderTime == 0)
						continue;
					float t = Math.max(getT(trackPosition, true), 0);
					arrow.setAlpha((float) (t - Math.floor(t)));
				} else
					arrow.setAlpha(1f);
				if (tcurRepeat % 2 == 0) {
					// last circle
					arrow.setRotation(curve.getEndAngle());
					arrow.drawCentered(endPos[0], endPos[1]);
				} else {
					// first circle
					arrow.setRotation(curve.getStartAngle());
					arrow.drawCentered(x, y);
				}
			}
		}

		if (timeDiff >= 0) {
			// approach circle
			GameImage.APPROACHCIRCLE.getImage().getScaledCopy(approachScale).drawCentered(x, y, color);
		} else {
			// Since update() might not have run before drawing during a replay, the
			// slider time may not have been calculated, which causes NAN numbers and flicker.
			if (sliderTime == 0)
				return;

			float[] c = curve.pointAt(getT(trackPosition, false));
			float[] c2 = curve.pointAt(getT(trackPosition, false) + 0.01f);

			float t = getT(trackPosition, false);
//			float dis = hitObject.getPixelLength() * OsuHitObject.getXMultiplier() * (t - (int) t);
//			Image sliderBallFrame = sliderBallImages[(int) (dis / (diameter * Math.PI) * 30) % sliderBallImages.length];
			Image sliderBallFrame = sliderBallImages[(int) (t * sliderTime * 60 / 1000) % sliderBallImages.length];
			float angle = (float) (Math.atan2(c2[1] - c[1], c2[0] - c[0]) * 180 / Math.PI);
			sliderBallFrame.setRotation(angle);
			sliderBallFrame.drawCentered(c[0], c[1]);

			// follow circle
			if (followCircleActive) {
				GameImage.SLIDER_FOLLOWCIRCLE.getImage().drawCentered(c[0], c[1]);

				// "flashlight" mod: dim the screen
				if (GameMod.FLASHLIGHT.isActive()) {
					float oldAlphaBlack = Utils.COLOR_BLACK_ALPHA.a;
					Utils.COLOR_BLACK_ALPHA.a = 0.75f;
					g.setColor(Utils.COLOR_BLACK_ALPHA);
					g.fillRect(0, 0, containerWidth, containerHeight);
					Utils.COLOR_BLACK_ALPHA.a = oldAlphaBlack;
				}
			}
		}

		Utils.COLOR_WHITE_FADE.a = oldAlpha;
	}

	/**
	 * Calculates the slider hit result.
	 * @return the hit result (GameData.HIT_* constants)
	 */
	private int hitResult() {
		float tickRatio = (float) ticksHit / tickIntervals;

		int result;
		if (tickRatio >= 1.0f)
			result = GameData.HIT_300;
		else if (tickRatio >= 0.5f)
			result = GameData.HIT_100;
		else if (tickRatio > 0f)
			result = GameData.HIT_50;
		else
			result = GameData.HIT_MISS;

		float[] lastPos = curve.pointAt(1);
		data.hitResult(hitObject.getTime() + (int) sliderTimeTotal, result,
				x, y, color, comboEnd, hitObject, currentRepeats + 1,
				currentRepeats % 2 == 0 ? HitResultType.SLIDEREND_FIRSTOBJECT : HitResultType.SLIDEREND);
		data.hitResult(hitObject.getTime() + (int) sliderTimeTotal, result,
				lastPos[0], lastPos[1], color, comboEnd, hitObject, currentRepeats + 1,
				currentRepeats % 2 == 0 ? HitResultType.SLIDEREND : HitResultType.SLIDEREND_FIRSTOBJECT);

		return result;
	}

	@Override
	public boolean mousePressed(int x, int y, int trackPosition) {
		if (sliderClickedInitial)  // first circle already processed
			return false;

		double distance = Math.hypot(this.x - x, this.y - y);
		int circleRadius = GameImage.HITCIRCLE.getImage().getWidth() / 2;
		if (distance < circleRadius) {
			int timeDiff = Math.abs(trackPosition - hitObject.getTime());
			int[] hitResultOffset = game.getHitResultOffsets();

			int result = -1;
			if (timeDiff < hitResultOffset[GameData.HIT_50]) {
				result = GameData.HIT_SLIDER30;
				ticksHit++;
			} else if (timeDiff < hitResultOffset[GameData.HIT_MISS])
				result = GameData.HIT_MISS;
			//else not a hit

			if (result > -1) {
				data.addHitError(hitObject.getTime(), x,y,trackPosition - hitObject.getTime());
				sliderClickedInitial = true;
				data.sliderTickResult(hitObject.getTime(), result, this.x, this.y, hitObject, currentRepeats);
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean update(boolean overlap, int delta, int mouseX, int mouseY, boolean keyPressed, int trackPosition) {
		int repeatCount = hitObject.getRepeatCount();
		int[] hitResultOffset = game.getHitResultOffsets();
		boolean isAutoMod = GameMod.AUTO.isActive();

		if (!sliderClickedInitial) {
			int time = hitObject.getTime();

			// start circle time passed
			if (trackPosition > time + hitResultOffset[GameData.HIT_50]) {
				sliderClickedInitial = true;
				if (isAutoMod) {  // "auto" mod: catch any missed notes due to lag
					ticksHit++;
					data.sliderTickResult(time, GameData.HIT_SLIDER30, x, y, hitObject, currentRepeats);
				} else
					data.sliderTickResult(time, GameData.HIT_MISS, x, y, hitObject, currentRepeats);
			}

			// "auto" mod: send a perfect hit result
			else if (isAutoMod) {
				if (Math.abs(trackPosition - time) < hitResultOffset[GameData.HIT_300]) {
					ticksHit++;
					sliderClickedInitial = true;
					data.sliderTickResult(time, GameData.HIT_SLIDER30, x, y, hitObject, currentRepeats);
				}
			}

			// "relax" mod: click automatically
			else if (GameMod.RELAX.isActive() && trackPosition >= time)
				mousePressed(mouseX, mouseY, trackPosition);
		}

		// end of slider
		if (overlap || trackPosition > hitObject.getTime() + sliderTimeTotal) {
			tickIntervals++;

			// check if cursor pressed and within end circle
			if (keyPressed || GameMod.RELAX.isActive()) {
				float[] c = curve.pointAt(getT(trackPosition, false));
				double distance = Math.hypot(c[0] - mouseX, c[1] - mouseY);
				int followCircleRadius = GameImage.SLIDER_FOLLOWCIRCLE.getImage().getWidth() / 2;
				if (distance < followCircleRadius)
					sliderClickedFinal = true;
			}

			// final circle hit
			if (sliderClickedFinal)
				ticksHit++;

			// "auto" mod: always send a perfect hit result
			if (isAutoMod)
				ticksHit = tickIntervals;

			// calculate and send slider result
			hitResult();
			return true;
		}

		// repeats
		boolean isNewRepeat = false;
		if (repeatCount - 1 > currentRepeats) {
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
			tickIntervals < (ticksT.length * (currentRepeats + 1)) + repeatCount &&
			tickIntervals < (ticksT.length * repeatCount) + repeatCount) {
			float t = getT(trackPosition, true);
			if (t - Math.floor(t) >= ticksT[tickIndex]) {
				tickIntervals++;
				tickIndex = (tickIndex + 1) % ticksT.length;
				isNewTick = true;
			}
		}

		// holding slider...
		float[] c = curve.pointAt(getT(trackPosition, false));
		double distance = Math.hypot(c[0] - mouseX, c[1] - mouseY);
		int followCircleRadius = GameImage.SLIDER_FOLLOWCIRCLE.getImage().getWidth() / 2;
		if (((keyPressed || GameMod.RELAX.isActive()) && distance < followCircleRadius) || isAutoMod) {
			// mouse pressed and within follow circle
			followCircleActive = true;
			data.changeHealth(delta * GameData.HP_DRAIN_MULTIPLIER);

			// held during new repeat
			if (isNewRepeat) {
				ticksHit++;
				if (currentRepeats % 2 > 0) {  // last circle
					int lastIndex = hitObject.getSliderX().length;
					data.sliderTickResult(trackPosition, GameData.HIT_SLIDER30,
							curve.getX(lastIndex), curve.getY(lastIndex), hitObject, currentRepeats);
				} else  // first circle
					data.sliderTickResult(trackPosition, GameData.HIT_SLIDER30,
							c[0], c[1], hitObject, currentRepeats);
			}

			// held during new tick
			if (isNewTick) {
				ticksHit++;
				data.sliderTickResult(trackPosition, GameData.HIT_SLIDER10,
						c[0], c[1], hitObject, currentRepeats);
			}

			// held near end of slider
			if (!sliderClickedFinal && trackPosition > hitObject.getTime() + sliderTimeTotal - hitResultOffset[GameData.HIT_300])
				sliderClickedFinal = true;
		} else {
			followCircleActive = false;

			if (isNewRepeat)
				data.sliderTickResult(trackPosition, GameData.HIT_MISS, 0, 0, hitObject, currentRepeats);
			if (isNewTick)
				data.sliderTickResult(trackPosition, GameData.HIT_MISS, 0, 0, hitObject, currentRepeats);
		}

		return false;
	}

	@Override
	public void updatePosition() {
		this.x = hitObject.getScaledX();
		this.y = hitObject.getScaledY();

		if (hitObject.getSliderType() == OsuHitObject.SLIDER_PASSTHROUGH && hitObject.getSliderX().length == 2)
			this.curve = new CircumscribedCircle(hitObject, color);
		else
			this.curve = new LinearBezier(hitObject, color);
	}

	@Override
	public float[] getPointAt(int trackPosition) {
		if (trackPosition <= hitObject.getTime())
			return new float[] { x, y };
		else if (trackPosition >= hitObject.getTime() + sliderTimeTotal) {
			if (hitObject.getRepeatCount() % 2 == 0)
				return new float[] { x, y };
			else
				return curve.pointAt(1);
		} else
			return curve.pointAt(getT(trackPosition, false));
	}

	@Override
	public int getEndTime() { return hitObject.getTime() + (int) sliderTimeTotal; }

	/**
	 * Returns the t value based on the given track position.
	 * @param trackPosition the current track position
	 * @param raw if false, ensures that the value lies within [0, 1] by looping repeats
	 * @return the t value: raw [0, repeats] or looped [0, 1]
	 */
	private float getT(int trackPosition, boolean raw) {
		float t = (trackPosition - hitObject.getTime()) / sliderTime;
		if (raw)
			return t;
		else {
			float floor = (float) Math.floor(t);
			return (floor % 2 == 0) ? t - floor : floor + 1 - t;
		}
	}
}
