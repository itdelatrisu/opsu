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
import itdelatrisu.opsu.states.Game;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;

/**
 * Data type representing a circle object.
 */
public class Circle implements HitObject {
	/** The associated OsuHitObject. */
	private OsuHitObject hitObject;

	/** The scaled starting x, y coordinates. */
	private float x, y;

	/** The associated Game object. */
	private Game game;

	/** The associated GameData object. */
	private GameData data;

	/** The color of this circle. */
	private Color color;

	/** Whether or not the circle result ends the combo streak. */
	private boolean comboEnd;

	/**
	 * Initializes the Circle data type with map modifiers, images, and dimensions.
	 * @param container the game container
	 * @param circleSize the map's circleSize value
	 */
	public static void init(GameContainer container, float circleSize) {
		int diameter = (int) (104 - (circleSize * 8));
		diameter = (int) (diameter * OsuHitObject.getXMultiplier());  // convert from Osupixels (640x480)
		GameImage.HITCIRCLE.setImage(GameImage.HITCIRCLE.getImage().getScaledCopy(diameter, diameter));
		GameImage.HITCIRCLE_OVERLAY.setImage(GameImage.HITCIRCLE_OVERLAY.getImage().getScaledCopy(diameter, diameter));
		GameImage.APPROACHCIRCLE.setImage(GameImage.APPROACHCIRCLE.getImage().getScaledCopy(diameter, diameter));
	}

	/**
	 * Constructor.
	 * @param hitObject the associated OsuHitObject
	 * @param game the associated Game object
	 * @param data the associated GameData object
	 * @param color the color of this circle
	 * @param comboEnd true if this is the last hit object in the combo
	 */
	public Circle(OsuHitObject hitObject, Game game, GameData data, Color color, boolean comboEnd) {
		this.hitObject = hitObject;
		this.x = hitObject.getScaledX();
		this.y = hitObject.getScaledY();
		this.game = game;
		this.data = data;
		this.color = color;
		this.comboEnd = comboEnd;
	}

	@Override
	public void draw(Graphics g, int trackPosition) {
		int timeDiff = hitObject.getTime() - trackPosition;

		if (timeDiff >= 0) {
			float oldAlpha = color.a;
			float scale = timeDiff / (float)game.getApproachTime();

			float approachScale = 1 + scale * 3;
			color.a = 1 - scale;
			GameImage.APPROACHCIRCLE.getImage().getScaledCopy(approachScale).drawCentered(x, y, color);

			float alpha = Utils.clamp((1 - scale) * 2, 0, 1);
			color.a = alpha;
			Utils.COLOR_WHITE_FADE.a = alpha;
			GameImage.HITCIRCLE.getImage().drawCentered(x, y, color);
			GameImage.HITCIRCLE_OVERLAY.getImage().drawCentered(x, y, Utils.COLOR_WHITE_FADE);

			color.a = oldAlpha;
			Utils.COLOR_WHITE_FADE.a = 1f;
			data.drawSymbolNumber(hitObject.getComboNumber(), x, y,
					GameImage.HITCIRCLE.getImage().getWidth() * 0.40f / data.getDefaultSymbolImage(0).getHeight());
		}
	}

	/**
	 * Calculates the circle hit result.
	 * @param time the hit object time (difference between track time)
	 * @return the hit result (GameData.HIT_* constants)
	 */
	private int hitResult(int time) {
		int timeDiff = Math.abs(time);

		int[] hitResultOffset = game.getHitResultOffsets();
		int result = -1;
		if (timeDiff < hitResultOffset[GameData.HIT_300])
			result = GameData.HIT_300;
		else if (timeDiff < hitResultOffset[GameData.HIT_100])
			result = GameData.HIT_100;
		else if (timeDiff < hitResultOffset[GameData.HIT_50])
			result = GameData.HIT_50;
		else if (timeDiff < hitResultOffset[GameData.HIT_MISS])
			result = GameData.HIT_MISS;
		//else not a hit

		return result;
	}

	@Override
	public boolean mousePressed(int x, int y) {
		double distance = Math.hypot(this.x - x, this.y - y);
		int circleRadius = GameImage.HITCIRCLE.getImage().getWidth() / 2;
		if (distance < circleRadius) {
			int trackPosition = MusicController.getPosition();
			int timeDiff = trackPosition - hitObject.getTime();
			int result = hitResult(timeDiff);

			if (result > -1) {
				data.addHitError(hitObject.getTime(), x, y, timeDiff);
				data.hitResult(hitObject.getTime(), result, this.x, this.y, color, comboEnd, hitObject, 0);
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean update(boolean overlap, int delta, int mouseX, int mouseY, boolean keyPressed) {
		int time = hitObject.getTime();

		int trackPosition = MusicController.getPosition();
		int[] hitResultOffset = game.getHitResultOffsets();
		boolean isAutoMod = GameMod.AUTO.isActive();

		if (overlap || trackPosition > time + hitResultOffset[GameData.HIT_50]) {
			if (isAutoMod)  // "auto" mod: catch any missed notes due to lag
				data.hitResult(time, GameData.HIT_300, x, y, color, comboEnd, hitObject, 0);

			else  // no more points can be scored, so send a miss
				data.hitResult(time, GameData.HIT_MISS, x, y, null, comboEnd, hitObject, 0);
			return true;
		}

		// "auto" mod: send a perfect hit result
		else if (isAutoMod) {
			if (Math.abs(trackPosition - time) < hitResultOffset[GameData.HIT_300]) {
				data.hitResult(time, GameData.HIT_300, x, y, color, comboEnd, hitObject, 0);
				return true;
			}
		}

		// "relax" mod: click automatically
		else if (GameMod.RELAX.isActive() && trackPosition >= time)
			return mousePressed(mouseX, mouseY);

		return false;
	}

	@Override
	public float[] getPointAt(int trackPosition) { return new float[] { x, y }; }

	@Override
	public int getEndTime() { return hitObject.getTime(); }
}