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

import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.GameScore;
import itdelatrisu.opsu.MusicController;
import itdelatrisu.opsu.OsuHitObject;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.states.Game;
import itdelatrisu.opsu.states.Options;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.SlickException;

/**
 * Data type representing a circle object.
 */
public class Circle {
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
	 * The color of this circle.
	 */
	private Color color;

	/**
	 * Whether or not the circle result ends the combo streak.
	 */
	private boolean comboEnd;

	/**
	 * Initializes the Circle data type with map modifiers, images, and dimensions.
	 * @param container the game container
	 * @param circleSize the map's circleSize value
	 * @throws SlickException
	 */
	public static void init(GameContainer container, float circleSize) throws SlickException {
		int diameter = (int) (96 - (circleSize * 8));
		diameter = diameter * container.getWidth() / 640;  // convert from Osupixels (640x480)
		GameImage.HITCIRCLE.setImage(GameImage.HITCIRCLE.getImage().getScaledCopy(diameter, diameter));
		GameImage.HITCIRCLE_OVERLAY.setImage(GameImage.HITCIRCLE_OVERLAY.getImage().getScaledCopy(diameter, diameter));
		GameImage.APPROACHCIRCLE.setImage(GameImage.APPROACHCIRCLE.getImage().getScaledCopy(diameter, diameter));
	}

	/**
	 * Constructor.
	 * @param hitObject the associated OsuHitObject
	 * @param game the associated Game object
	 * @param score the associated GameScore object
	 * @param color the color of this circle
	 * @param comboEnd true if this is the last hit object in the combo
	 */
	public Circle(OsuHitObject hitObject, Game game, GameScore score, Color color, boolean comboEnd) {
		this.hitObject = hitObject;
		this.game = game;
		this.score = score;
		this.color = color;
		this.comboEnd = comboEnd;
	}

	/**
	 * Draws the circle to the graphics context.
	 * @param trackPosition the current track position
	 */
	public void draw(int trackPosition) { 
		int timeDiff = hitObject.time - trackPosition;

		if (timeDiff >= 0) {
			float approachScale = 1 + (timeDiff * 2f / game.getApproachTime());
			Utils.drawCentered(GameImage.APPROACHCIRCLE.getImage().getScaledCopy(approachScale),
					hitObject.x, hitObject.y, color);
			Utils.drawCentered(GameImage.HITCIRCLE_OVERLAY.getImage(), hitObject.x, hitObject.y, Color.white);
			Utils.drawCentered(GameImage.HITCIRCLE.getImage(), hitObject.x, hitObject.y, color);
			score.drawSymbolNumber(hitObject.comboNumber, hitObject.x, hitObject.y,
					GameImage.HITCIRCLE.getImage().getWidth() * 0.40f / score.getDefaultSymbolImage(0).getHeight());
		}
	}

	/**
	 * Calculates the circle hit result.
	 * @param time the hit object time (difference between track time)
	 * @return the hit result (GameScore.HIT_* constants)
	 */
	public int hitResult(int time) {
		int trackPosition = MusicController.getPosition();
		int timeDiff = Math.abs(trackPosition - time);

		int[] hitResultOffset = game.getHitResultOffsets();
		int result = -1;
		if (timeDiff < hitResultOffset[GameScore.HIT_300])
			result = GameScore.HIT_300;
		else if (timeDiff < hitResultOffset[GameScore.HIT_100])
			result = GameScore.HIT_100;
		else if (timeDiff < hitResultOffset[GameScore.HIT_50])
			result = GameScore.HIT_50;
		else if (timeDiff < hitResultOffset[GameScore.HIT_MISS])
			result = GameScore.HIT_MISS;
		//else not a hit

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
		double distance = Math.hypot(hitObject.x - x, hitObject.y - y);
		int circleRadius = GameImage.HITCIRCLE.getImage().getWidth() / 2;
		if (distance < circleRadius) {
			int result = hitResult(hitObject.time);
			if (result > -1) {
				score.hitResult(hitObject.time, result, hitObject.x, hitObject.y,
						color, comboEnd, hitObject.hitSound
				);
				return true;
			}
		}
		return false;
	}

	/**
	 * Updates the circle object.
	 * @param overlap true if the next object's start time has already passed
	 * @return true if a hit result (miss) was processed
	 */
	public boolean update(boolean overlap) {
		int trackPosition = MusicController.getPosition();
		int[] hitResultOffset = game.getHitResultOffsets();
		boolean isAutoMod = Options.isModActive(Options.MOD_AUTO);

		if (overlap || trackPosition > hitObject.time + hitResultOffset[GameScore.HIT_50]) {
			if (isAutoMod)  // "auto" mod: catch any missed notes due to lag
				score.hitResult(hitObject.time, GameScore.HIT_300,
						hitObject.x, hitObject.y, color, comboEnd, hitObject.hitSound);
			
			else  // no more points can be scored, so send a miss
				score.hitResult(hitObject.time, GameScore.HIT_MISS,
						hitObject.x, hitObject.y, null, comboEnd, hitObject.hitSound);
			return true;
		}

		// "auto" mod: send a perfect hit result
		else if (isAutoMod) {
			if (Math.abs(trackPosition - hitObject.time) < hitResultOffset[GameScore.HIT_300]) {
				score.hitResult(hitObject.time, GameScore.HIT_300,
						hitObject.x, hitObject.y, color, comboEnd, hitObject.hitSound);
				return true;
			}
		}

		return false;
	}
}