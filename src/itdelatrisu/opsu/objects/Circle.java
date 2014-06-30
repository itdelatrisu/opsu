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
import itdelatrisu.opsu.OsuHitObject;
import itdelatrisu.opsu.states.Game;
import itdelatrisu.opsu.states.Options;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;

/**
 * Data type representing a circle object.
 */
public class Circle {
	/**
	 * Images related to hit circles.
	 */
	private static Image
		hitCircle,           // hit circle
		hitCircleOverlay,    // hit circle overlay
		approachCircle;      // approach circle

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
		hitCircle = new Image("hitcircle.png").getScaledCopy(diameter, diameter);
		hitCircleOverlay = new Image("hitcircleoverlay.png").getScaledCopy(diameter, diameter);
		approachCircle = new Image("approachcircle.png").getScaledCopy(diameter, diameter);
	}

	/**
	 * Returns the hit circle image.
	 */
	public static Image getHitCircle() { return hitCircle; }

	/**
	 * Returns the hit circle overlay image.
	 */
	public static Image getHitCircleOverlay() { return hitCircleOverlay; }

	/**
	 * Returns the approach circle image.
	 */
	public static Image getApproachCircle() { return approachCircle; }

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
			drawCentered(approachCircle.getScaledCopy(approachScale), hitObject.x, hitObject.y, color);
			drawCentered(hitCircleOverlay, hitObject.x, hitObject.y, Color.white);
			drawCentered(hitCircle, hitObject.x, hitObject.y, color);
			score.drawSymbolNumber(hitObject.comboNumber, hitObject.x, hitObject.y,
					hitCircle.getWidth() * 0.40f / score.getDefaultSymbolImage(0).getHeight());
		}
	}

	/**
	 * Draws an image based on its center with a color filter.
	 */
	private void drawCentered(Image img, float x, float y, Color color) {
		img.draw(x - (img.getWidth() / 2f), y - (img.getHeight() / 2f), color);
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
		int circleRadius = hitCircle.getWidth() / 2;
		if (distance < circleRadius) {
			int result = hitResult(hitObject.time);
			if (result > -1) {
				score.hitResult(hitObject.time, result, hitObject.x, hitObject.y, color, comboEnd);
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
						hitObject.x, hitObject.y, color, comboEnd);
			
			else  // no more points can be scored, so send a miss
				score.hitResult(hitObject.time, GameScore.HIT_MISS,
						hitObject.x, hitObject.y, null, comboEnd);
			return true;
		}

		// "auto" mod: send a perfect hit result
		else if (isAutoMod) {
			if (Math.abs(trackPosition - hitObject.time) < hitResultOffset[GameScore.HIT_300]) {
				score.hitResult(hitObject.time, GameScore.HIT_300,
						hitObject.x, hitObject.y, color, comboEnd);
				return true;
			}
		}

		return false;
	}
}