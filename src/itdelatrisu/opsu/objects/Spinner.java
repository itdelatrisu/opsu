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

import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.GameMod;
import itdelatrisu.opsu.GameScore;
import itdelatrisu.opsu.OsuHitObject;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.audio.SoundEffect;
import itdelatrisu.opsu.states.Game;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;

/**
 * Data type representing a spinner object.
 */
public class Spinner implements HitObject {
	/**
	 * Container dimensions.
	 */
	private static int width, height;

	/**
	 * The associated OsuHitObject.
	 */
	private OsuHitObject hitObject;

	/**
	 * The associated GameScore object.
	 */
	private GameScore score;

	/**
	 * The last rotation angle.
	 */
	private float lastAngle = -1f;

	/**
	 * The current number of rotations.
	 */
	private float rotations = 0f;

	/**
	 * The total number of rotations needed to clear the spinner.
	 */
	private float rotationsNeeded;

	/**
	 * Initializes the Spinner data type with images and dimensions.
	 * @param container the game container
	 * @throws SlickException
	 */
	public static void init(GameContainer container) throws SlickException {
		width  = container.getWidth();
		height = container.getHeight();

		Image spinnerCircle = GameImage.SPINNER_CIRCLE.getImage();
		GameImage.SPINNER_CIRCLE.setImage(spinnerCircle.getScaledCopy(height * 9 / 10, height * 9 / 10));
		GameImage.SPINNER_APPROACHCIRCLE.setImage(GameImage.SPINNER_APPROACHCIRCLE.getImage().getScaledCopy(
				spinnerCircle.getWidth(), spinnerCircle.getHeight()));
		GameImage.SPINNER_METRE.setImage(GameImage.SPINNER_METRE.getImage().getScaledCopy(width, height));
	}

	/**
	 * Constructor.
	 * @param hitObject the associated OsuHitObject
	 * @param game the associated Game object
	 * @param score the associated GameScore object
	 */
	public Spinner(OsuHitObject hitObject, Game game, GameScore score) {
		this.hitObject = hitObject;
		this.score = score;

		// calculate rotations needed
		float spinsPerMinute = 100 + (score.getDifficulty() * 15);
		rotationsNeeded = spinsPerMinute * (hitObject.getEndTime() - hitObject.getTime()) / 60000f;
	}

	@Override
	public void draw(int trackPosition, boolean currentObject, Graphics g) {
		// only draw spinners if current object
		if (!currentObject)
			return;

		int timeDiff = hitObject.getTime() - trackPosition;
		boolean spinnerComplete = (rotations >= rotationsNeeded);

		// TODO: draw "OSU!" image after spinner ends
		//spinnerOsuImage.drawCentered(width / 2, height / 4);

		// darken screen
		g.setColor(Utils.COLOR_BLACK_ALPHA);
		g.fillRect(0, 0, width, height);

		if (timeDiff > 0)
			return;

		// spinner meter (subimage)
		Image spinnerMetre = GameImage.SPINNER_METRE.getImage();
		int spinnerMetreY = (spinnerComplete) ? 0 : (int) (spinnerMetre.getHeight() * (1 - (rotations / rotationsNeeded)));
		Image spinnerMetreSub = spinnerMetre.getSubImage(
				0, spinnerMetreY,
				spinnerMetre.getWidth(), spinnerMetre.getHeight() - spinnerMetreY
		);
		spinnerMetreSub.draw(0, height - spinnerMetreSub.getHeight());

		// main spinner elements
		float approachScale = 1 - ((float) timeDiff / (hitObject.getTime() - hitObject.getEndTime()));
		GameImage.SPINNER_CIRCLE.getImage().drawCentered(width / 2, height / 2);
		GameImage.SPINNER_APPROACHCIRCLE.getImage().getScaledCopy(approachScale).drawCentered(width / 2, height / 2);
		GameImage.SPINNER_SPIN.getImage().drawCentered(width / 2, height * 3 / 4);

		if (spinnerComplete) {
			GameImage.SPINNER_CLEAR.getImage().drawCentered(width / 2, height / 4);
			int extraRotations = (int) (rotations - rotationsNeeded);
			if (extraRotations > 0)
				score.drawSymbolNumber(extraRotations * 1000, width / 2, height * 2 / 3, 1.0f);
		}
	}

	/**
	 * Calculates and sends the spinner hit result.
	 * @return the hit result (GameScore.HIT_* constants)
	 */
	private int hitResult() {
		// TODO: verify ratios

		int result;
		float ratio = rotations / rotationsNeeded;
		if (ratio >= 1.0f ||
			GameMod.AUTO.isActive() || GameMod.SPUN_OUT.isActive()) {
			result = GameScore.HIT_300;
			SoundController.playSound(SoundEffect.SPINNEROSU);
		} else if (ratio >= 0.8f)
			result = GameScore.HIT_100;
		else if (ratio >= 0.5f)
			result = GameScore.HIT_50;
		else
			result = GameScore.HIT_MISS;

		score.hitResult(hitObject.getEndTime(), result, width / 2, height / 2,
				Color.transparent, true, (byte) -1);
		return result;
	}

	@Override
	public boolean mousePressed(int x, int y) { return false; }  // not used

	@Override
	public boolean update(boolean overlap, int delta, int mouseX, int mouseY) {
		int trackPosition = MusicController.getPosition();
		if (overlap)
			return true;

		// end of spinner
		if (trackPosition > hitObject.getEndTime()) {
			hitResult();
			return true;
		}

		// spin automatically (TODO: correct rotation angles)
		if (GameMod.AUTO.isActive()) {
			// "auto" mod (fast)
			score.changeHealth(delta * GameScore.HP_DRAIN_MULTIPLIER);
			rotate(delta / 20f);
			return false;
		} else if (GameMod.SPUN_OUT.isActive()) {
			// "spun out" mod (slow)
			score.changeHealth(delta * GameScore.HP_DRAIN_MULTIPLIER);
			rotate(delta / 32f);
			return false;
		}

		// not spinning: nothing to do
		if (!Utils.isGameKeyPressed()) {
			lastAngle = -1f;
			return false;
		}

		// scale angle from [-pi, +pi] to [0, +pi]
		float angle = (float) Math.atan2(mouseY - (height / 2), mouseX - (width / 2));
		if (angle < 0f)
			angle += Math.PI;

		if (lastAngle >= 0f) {  // skip initial clicks
			float angleDiff = Math.abs(lastAngle - angle);
			if (angleDiff < Math.PI / 2) {  // skip huge angle changes...
				score.changeHealth(delta * GameScore.HP_DRAIN_MULTIPLIER);
				rotate(angleDiff);
			}
		}

		lastAngle = angle;
		return false;
	}

	/**
	 * Rotates the spinner by a number of degrees.
	 * @param degrees the angle to rotate (in radians)
	 */
	private void rotate(float degrees) {
		float newRotations = rotations + (degrees / (float) (2 * Math.PI));

		// added one whole rotation...
		if (Math.floor(newRotations) > rotations) {
			if (newRotations > rotationsNeeded) {  // extra rotations
				score.changeScore(1000);
				SoundController.playSound(SoundEffect.SPINNERBONUS);
			} else {
				score.changeScore(100);
				SoundController.playSound(SoundEffect.SPINNERSPIN);
			}
		}

		rotations = newRotations;
	}
}