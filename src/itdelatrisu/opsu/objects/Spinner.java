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

package itdelatrisu.opsu.objects;

import itdelatrisu.opsu.GameData;
import itdelatrisu.opsu.GameData.HitObjectType;
import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.GameMod;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.audio.SoundEffect;
import itdelatrisu.opsu.beatmap.HitObject;
import itdelatrisu.opsu.objects.curves.Vec2f;
import itdelatrisu.opsu.options.Options;
import itdelatrisu.opsu.states.Game;
import itdelatrisu.opsu.ui.Colors;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;

/**
 * Data type representing a spinner object.
 */
public class Spinner implements GameObject {
	/** Container dimensions. */
	private static int width, height;

	/** The map's overall difficulty value. */
	private static float overallDifficulty = 5f;

	/** The number of rotation velocities to store. */
	private int maxStoredDeltaAngles;

	/** The amount of time, in milliseconds, before another velocity is stored. */
	private static final float DELTA_UPDATE_TIME = 1000 / 60f;

	/** Angle mod multipliers: "auto" (477rpm), "spun out" (287rpm) */
	private static final float
		AUTO_MULTIPLIER = 1 / 20f,         // angle = 477/60f * delta/1000f * TWO_PI;
		SPUN_OUT_MULTIPLIER = 1 / 33.25f;  // angle = 287/60f * delta/1000f * TWO_PI;

	/** Maximum angle difference. */
	private static final float MAX_ANG_DIFF = DELTA_UPDATE_TIME * AUTO_MULTIPLIER; // ~95.3

	/** PI constants. */
	private static final float
		TWO_PI  = (float) (Math.PI * 2),
		HALF_PI = (float) (Math.PI / 2);

	/** The associated HitObject. */
	private HitObject hitObject;

	/** The associated Game object. */
	private Game game;

	/** The associated GameData object. */
	private GameData data;

	/** The last rotation angle. */
	private float lastAngle = 0f;

	/** The current number of rotations. */
	private float rotations = 0f;

	/** The current rotation to draw. */
	private float drawRotation = 0f;

	/** The total number of rotations needed to clear the spinner. */
	private float rotationsNeeded;

	/** The remaining amount of time that was not used. */
	private float deltaOverflow;

	/** The sum of all the velocities in storedVelocities. */
	private float sumDeltaAngle = 0f;

	/** Array holding the most recent rotation velocities. */
	private float[] storedDeltaAngle;

	/** True if the mouse cursor is pressed. */
	private boolean isSpinning;

	/** Current index of the stored velocities in rotations/second. */
	private int deltaAngleIndex = 0;

	/** The remaining amount of the angle that was not used. */
	private float deltaAngleOverflow = 0;

	/** The RPM that is drawn to the screen. */
	private int drawnRPM = 0;

	/**
	 * Initializes the Spinner data type with images and dimensions.
	 * @param container the game container
	 * @param difficulty the map's overall difficulty value
	 */
	public static void init(GameContainer container, float difficulty) {
		width  = container.getWidth();
		height = container.getHeight();
		overallDifficulty = difficulty;
	}

	/**
	 * Constructor.
	 * @param hitObject the associated HitObject
	 * @param game the associated Game object
	 * @param data the associated GameData object
	 */
	public Spinner(HitObject hitObject, Game game, GameData data) {
		this.hitObject = hitObject;
		this.game = game;
		this.data = data;

/*
		1 beat = 731.707317073171ms
			RPM at frame X with spinner Y beats long
				10	20	30	40	50	60 <frame#
		1.00	306	418	457	470
		1.25	323	424	459	471	475
		1.5		305	417	456	470	475	477
		1.75	322	417	456	471	475
		2.00	304	410	454	469	474	476
		2.25	303	410	451	467	474	476
		2.50	303	417	456	470	475	476
		2.75	302	416	456	470	475	476
		3.00	301	416	456	470	475		<-- ~2sec
		4.00	274	414	453	470	475
		5.00	281	409	454	469	475
		6.00	232	392	451	467	472	476
		6.25	193	378	443	465
		6.50	133	344	431	461
		6.75	85	228	378	435	463	472	<-- ~5sec
		7.00	53	154	272	391	447
		8.00	53	154	272	391	447
		9.00	53	154	272	400	450
		10.00	53	154	272	400	450
		15.00	53	154	272	391	444	466
		20.00	61	154	272	400	447
		25.00	53	154	272	391	447	466
		^beats
*/
		// TODO not correct at all, but close enough?
		// <2sec ~ 12 ~ 200ms
		// >5sec ~ 48 ~ 800ms

		final int minVel = 12;
		final int maxVel = 48;
		final int minTime = 2000;
		final int maxTime = 5000;
		maxStoredDeltaAngles = Utils.clamp((hitObject.getEndTime() - hitObject.getTime() - minTime)
				* (maxVel - minVel) / (maxTime - minTime) + minVel, minVel, maxVel);
		storedDeltaAngle = new float[maxStoredDeltaAngles];

		// calculate rotations needed
		float spinsPerMinute = 100 + (overallDifficulty * 15);
		rotationsNeeded = spinsPerMinute * (hitObject.getEndTime() - hitObject.getTime()) / 60000f;
	}

	@Override
	public void draw(Graphics g, int trackPosition) {
		// only draw spinners shortly before start time
		int timeDiff = hitObject.getTime() - trackPosition;
		final int fadeInTime = game.getFadeInTime();
		if (timeDiff - fadeInTime > 0)
			return;

		boolean spinnerComplete = (rotations >= rotationsNeeded);
		float alpha = Utils.clamp(1 - (float) timeDiff / fadeInTime, 0f, 1f);

		// darken screen
		if (Options.getSkin().isSpinnerFadePlayfield()) {
			float oldAlpha = Colors.BLACK_ALPHA.a;
			if (timeDiff > 0)
				Colors.BLACK_ALPHA.a *= alpha;
			g.setColor(Colors.BLACK_ALPHA);
			g.fillRect(0, 0, width, height);
			Colors.BLACK_ALPHA.a = oldAlpha;
		}

		// rpm
		Image rpmImg = GameImage.SPINNER_RPM.getImage();
		rpmImg.setAlpha(alpha);
		rpmImg.drawCentered(width / 2f, height - rpmImg.getHeight() / 2f);
		if (timeDiff < 0)
			data.drawSymbolString(Integer.toString(drawnRPM), (width + rpmImg.getWidth() * 0.95f) / 2f,
					height - data.getScoreSymbolImage('0').getHeight() * 1.025f, 1f, 1f, true);

		// spinner meter (subimage)
		Image spinnerMetre = GameImage.SPINNER_METRE.getImage();
		int spinnerMetreY = (spinnerComplete) ? 0 : (int) (spinnerMetre.getHeight() * (1 - (rotations / rotationsNeeded)));
		Image spinnerMetreSub = spinnerMetre.getSubImage(
				0, spinnerMetreY,
				spinnerMetre.getWidth(), spinnerMetre.getHeight() - spinnerMetreY
		);
		spinnerMetreSub.setAlpha(alpha);
		spinnerMetreSub.draw(0, height - spinnerMetreSub.getHeight());

		// main spinner elements
		GameImage.SPINNER_CIRCLE.getImage().setAlpha(alpha);
		GameImage.SPINNER_CIRCLE.getImage().setRotation(drawRotation * 360f);
		GameImage.SPINNER_CIRCLE.getImage().drawCentered(width / 2, height / 2);
		if (!GameMod.HIDDEN.isActive()) {
			float approachScale = 1 - Utils.clamp(((float) timeDiff / (hitObject.getTime() - hitObject.getEndTime())), 0f, 1f);
			Image approachCircleScaled = GameImage.SPINNER_APPROACHCIRCLE.getImage().getScaledCopy(approachScale);
			approachCircleScaled.setAlpha(alpha);
			approachCircleScaled.drawCentered(width / 2, height / 2);
		}
		GameImage.SPINNER_SPIN.getImage().setAlpha(alpha);
		GameImage.SPINNER_SPIN.getImage().drawCentered(width / 2, height * 3 / 4);

		if (spinnerComplete) {
			GameImage.SPINNER_CLEAR.getImage().drawCentered(width / 2, height / 4);
			int extraRotations = (int) (rotations - rotationsNeeded);
			if (extraRotations > 0)
				data.drawSymbolNumber(extraRotations * 1000, width / 2, height * 2 / 3, 1f, 1f);
		}
	}

	/**
	 * Calculates and sends the spinner hit result.
	 * @return the hit result (GameData.HIT_* constants)
	 */
	private int hitResult() {
		// TODO: verify ratios
		int result;
		float ratio = rotations / rotationsNeeded;
		if (ratio >= 1.0f || GameMod.AUTO.isActive() || GameMod.AUTOPILOT.isActive() || GameMod.SPUN_OUT.isActive()) {
			result = GameData.HIT_300;
			if (!Options.isGameplaySoundDisabled())
				SoundController.playSound(SoundEffect.SPINNEROSU);
		} else if (ratio >= 0.9f)
			result = GameData.HIT_100;
		else if (ratio >= 0.75f)
			result = GameData.HIT_50;
		else
			result = GameData.HIT_MISS;

		data.sendHitResult(hitObject.getEndTime(), result, width / 2, height / 2,
				Color.transparent, true, hitObject, HitObjectType.SPINNER, true, 0, null, false);
		return result;
	}

	@Override
	public boolean mousePressed(int x, int y, int trackPosition) {
		lastAngle = (float) Math.atan2(x - (height / 2), y - (width / 2));
		return false;
	}

	@Override
	public boolean update(int delta, int mouseX, int mouseY, boolean keyPressed, int trackPosition) {
		// end of spinner
		if (trackPosition > hitObject.getEndTime()) {
			hitResult();
			return true;
		}

		// game button is released
		if (isSpinning && !(keyPressed || GameMod.RELAX.isActive()))
			isSpinning = false;

		// spin automatically
		// http://osu.ppy.sh/wiki/FAQ#Spinners

		deltaOverflow += delta;

		float angleDiff = 0;
		if (GameMod.AUTO.isActive()) {
			angleDiff = delta * AUTO_MULTIPLIER;
			isSpinning = true;
		} else if (GameMod.SPUN_OUT.isActive() || GameMod.AUTOPILOT.isActive()) {
			angleDiff = delta * SPUN_OUT_MULTIPLIER;
			isSpinning = true;
		} else {
			float angle = (float) Math.atan2(mouseY - (height / 2), mouseX - (width / 2));

			// set initial angle to current mouse position to skip first click
			if (!isSpinning && (keyPressed || GameMod.RELAX.isActive())) {
				lastAngle = angle;
				isSpinning = true;
				return false;
			}

			angleDiff = angle - lastAngle;
			if (Math.abs(angleDiff) > 0.01f)
				lastAngle = angle;
			else
				angleDiff = 0;
		}

		// make angleDiff the smallest angle change possible
		// (i.e. 1/4 rotation instead of 3/4 rotation)
		if (angleDiff < -Math.PI)
			angleDiff += TWO_PI;
		else if (angleDiff > Math.PI)
			angleDiff -= TWO_PI;

		// may be a problem at higher frame rate due to floating point round off
		if (isSpinning)
			deltaAngleOverflow += angleDiff;

		while (deltaOverflow >= DELTA_UPDATE_TIME) {
			// spin caused by the cursor
			float deltaAngle = 0;
			if (isSpinning) {
				deltaAngle = deltaAngleOverflow * DELTA_UPDATE_TIME / deltaOverflow;
				deltaAngleOverflow -= deltaAngle;
				deltaAngle = Utils.clamp(deltaAngle, -MAX_ANG_DIFF, MAX_ANG_DIFF);
			}
			sumDeltaAngle -= storedDeltaAngle[deltaAngleIndex];
			sumDeltaAngle += deltaAngle;
			storedDeltaAngle[deltaAngleIndex++] = deltaAngle;
			deltaAngleIndex %= storedDeltaAngle.length;
			deltaOverflow -= DELTA_UPDATE_TIME;

			float rotationAngle = sumDeltaAngle / maxStoredDeltaAngles;
			rotationAngle = Utils.clamp(rotationAngle, -MAX_ANG_DIFF, MAX_ANG_DIFF);
			float rotationPerSec = rotationAngle * (1000 / DELTA_UPDATE_TIME) / TWO_PI;

			drawnRPM = (int) (Math.abs(rotationPerSec * 60));

			rotate(rotationAngle);
		}

		//TODO may need to update 1 more time when the spinner ends?
		return false;
	}

	@Override
	public void updatePosition() {}

	@Override
	public Vec2f getPointAt(int trackPosition) {
		// get spinner time
		int timeDiff;
		float x = hitObject.getScaledX(), y = hitObject.getScaledY();
		if (trackPosition <= hitObject.getTime())
			timeDiff = 0;
		else if (trackPosition >= hitObject.getEndTime())
			timeDiff = hitObject.getEndTime() - hitObject.getTime();
		else
			timeDiff = trackPosition - hitObject.getTime();

		// calculate point
		float multiplier = (GameMod.AUTO.isActive()) ? AUTO_MULTIPLIER : SPUN_OUT_MULTIPLIER;
		float angle = (timeDiff * multiplier) - HALF_PI;
		final float r = height / 10f;
		return new Vec2f((float) (x + r * Math.cos(angle)), (float) (y + r * Math.sin(angle)));
	}

	@Override
	public int getEndTime() { return hitObject.getEndTime(); }

	/**
	 * Rotates the spinner by an angle.
	 * @param angle the angle to rotate (in radians)
	 */
	private void rotate(float angle) {
		drawRotation += angle / TWO_PI;
		angle = Math.abs(angle);
		float newRotations = rotations + (angle / TWO_PI);

		// added one whole rotation...
		if (Math.floor(newRotations) > rotations) {
			if (newRotations > rotationsNeeded)  // extra rotations
				data.sendSpinnerSpinResult(GameData.HIT_SPINNERBONUS);
			else
				data.sendSpinnerSpinResult(GameData.HIT_SPINNERSPIN);
		}

		rotations = newRotations;
	}

	@Override
	public void reset() {
		deltaAngleIndex = 0;
		sumDeltaAngle = 0;
		for (int i = 0; i < storedDeltaAngle.length; i++)
			storedDeltaAngle[i] = 0;
		drawRotation = 0;
		rotations = 0;
		deltaOverflow = 0;
		isSpinning = false;
	}
}
