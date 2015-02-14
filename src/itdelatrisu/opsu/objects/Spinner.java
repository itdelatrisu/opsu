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

/**
 * Data type representing a spinner object.
 */
public class Spinner implements HitObject {

	/** Container dimensions. */
	private static int width, height;

	/** The associated OsuHitObject. */
	private OsuHitObject hitObject;

	/** The associated GameData object. */
	private GameData data;

	/** The last rotation angle. */
	private float lastAngle = 0f;

	/** The current number of rotations. */
	private float rotations = 0f;

	/** The total number of rotations needed to clear the spinner. */
	private float rotationsNeeded;
        
        /** The sum of all the velocities in storedVelocities. */
        private float sumVelocity = 0f;
        
        /** Array of the last 50 rotation velocities. */
        private float[] storedVelocities = new float[50];
	
	/** True if the mouse cursor is pressed. */
	private boolean isSpinning;
        
        /** Current index of the stored velocities in rotations/second. */
        private int velocityIndex = 0;

	/**
	 * Initializes the Spinner data type with images and dimensions.
	 * @param container the game container
	 */
	public static void init(GameContainer container) {
		width  = container.getWidth();
		height = container.getHeight();
	}

	/**
	 * Constructor.
	 * @param hitObject the associated OsuHitObject
	 * @param game the associated Game object
	 * @param data the associated GameData object
	 */
	public Spinner(OsuHitObject hitObject, Game game, GameData data) {
		this.hitObject = hitObject;
		this.data = data;

		// calculate rotations needed
		float spinsPerMinute = 100 + (data.getDifficulty() * 15);
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
		//GameImage.SPINNER_OSU.getImage().drawCentered(width / 2, height / 4);

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
				data.drawSymbolNumber(extraRotations * 1000, width / 2, height * 2 / 3, 1.0f);
		}
                
                g.setColor(Color.white);
                g.drawString(String.format("RPM: %d", Math.abs(Math.round(sumVelocity/storedVelocities.length*60))), 100, 100);
	}

	/**
	 * Calculates and sends the spinner hit result.
	 * @return the hit result (GameData.HIT_* constants)
	 */
	private int hitResult() {
		// TODO: verify ratios

		int result;
		float ratio = rotations / rotationsNeeded;
		if (ratio >= 1.0f ||
			GameMod.AUTO.isActive() || GameMod.SPUN_OUT.isActive()) {
			result = GameData.HIT_300;
			SoundController.playSound(SoundEffect.SPINNEROSU);
		} else if (ratio >= 0.8f)
			result = GameData.HIT_100;
		else if (ratio >= 0.5f)
			result = GameData.HIT_50;
		else
			result = GameData.HIT_MISS;

		data.hitResult(hitObject.getEndTime(), result, width / 2, height / 2,
				Color.transparent, true, (byte) -1);
		return result;
	}

	@Override
	public boolean mousePressed(int x, int y) { return false; }  // not used

	@Override
	public boolean update(boolean overlap, int delta, int mouseX, int mouseY) {
		int trackPosition = MusicController.getPosition();

		// end of spinner
		if (overlap || trackPosition > hitObject.getEndTime()) {
			hitResult();
			return true;
		}

		// spin automatically (TODO: correct rotation angles)
		if (GameMod.AUTO.isActive()) {
			// "auto" mod (fast)
			data.changeHealth(delta * GameData.HP_DRAIN_MULTIPLIER);
			rotate(delta / 20f);
			return false;
		} else if (GameMod.SPUN_OUT.isActive()) {
			// "spun out" mod (slow)
			data.changeHealth(delta * GameData.HP_DRAIN_MULTIPLIER);
			rotate(delta / 32f);
			return false;
		}

		// game button is released
		if (isSpinning && !Utils.isGameKeyPressed()) {
			isSpinning = false;
		}

		float angle = (float) Math.atan2(mouseY - (height / 2), mouseX - (width / 2));
		
		// set initial angle to current mouse position to skip first click
		if (!isSpinning && Utils.isGameKeyPressed()) {
			lastAngle = angle;
			isSpinning = true;
			return false;
		}
		
		float angleDiff = angle - lastAngle;
		
		// make angleDiff the smallest angle change possible
		// (i.e. 1/4 rotation instead of 3/4 rotation)
		if (angleDiff < -Math.PI) {
			angleDiff = (float) (angleDiff + Math.PI*2);
		} else if (angleDiff > Math.PI) {
			angleDiff = (float) (angleDiff - Math.PI*2);
		}
		
		// spin caused by the cursor
		float cursorVelocity = 0;
		if (isSpinning)
			cursorVelocity = Math.min((float)(angleDiff / (Math.PI*2) / delta * 1000), 8f);
                
                sumVelocity -= storedVelocities[velocityIndex];
                sumVelocity += cursorVelocity;
                storedVelocities[velocityIndex++] = cursorVelocity;
                velocityIndex %= storedVelocities.length;

		data.changeHealth(delta * GameData.HP_DRAIN_MULTIPLIER);
		rotate(sumVelocity / storedVelocities.length * (float)Math.PI*2 * delta / 1000);
		
		lastAngle = angle;
		return false;
	}

	/**
	 * Rotates the spinner by a number of radians.
	 * @param angle the angle to rotate (in radians)
	 */
	private void rotate(float angle) {
		angle = Math.abs(angle);
		float newRotations = rotations + (angle / (float) (2 * Math.PI));

		// added one whole rotation...
		if (Math.floor(newRotations) > rotations) {
			if (newRotations > rotationsNeeded) {  // extra rotations
				data.changeScore(1000);
				SoundController.playSound(SoundEffect.SPINNERBONUS);
			} else {
				data.changeScore(100);
				SoundController.playSound(SoundEffect.SPINNERSPIN);
			}
		}

		rotations = newRotations;
	}
}
