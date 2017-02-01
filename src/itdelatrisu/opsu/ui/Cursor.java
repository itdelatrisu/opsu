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

package itdelatrisu.opsu.ui;

import itdelatrisu.opsu.ErrorHandler;
import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.Opsu;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.options.Options;
import itdelatrisu.opsu.skins.Skin;
import itdelatrisu.opsu.ui.animations.AnimationEquation;

import java.awt.Point;
import java.nio.IntBuffer;
import java.util.LinkedList;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.StateBasedGame;

/**
 * Updates and draws the cursor.
 */
public class Cursor {
	/** Empty cursor. */
	private static org.lwjgl.input.Cursor emptyCursor;

	/** Last cursor coordinates. */
	private Point lastPosition;

	/** Cursor rotation angle. */
	private float cursorAngle = 0f;

	/** The time in milliseconds when the cursor was last pressed, used for the scaling animation. */
	private long lastCursorPressTime = 0L;

	/** Whether or not the cursor was pressed in the last frame, used for the scaling animation. */
	private boolean lastCursorPressState = false;

	/** The amount the cursor scale increases, if enabled, when pressed. */
	private static final float CURSOR_SCALE_CHANGE = 0.25f;

	/** The time it takes for the cursor to scale, in milliseconds. */
	private static final float CURSOR_SCALE_TIME = 125;

	/** Stores all previous cursor locations to display a trail. */
	private LinkedList<Point> trail = new LinkedList<Point>();

	// game-related variables
	private static GameContainer container;
	private static StateBasedGame game;
	private static Input input;

	/**
	 * Initializes the class.
	 * @param container the game container
	 * @param game the game object
	 */
	public static void init(GameContainer container, StateBasedGame game) {
		Cursor.container = container;
		Cursor.game = game;
		Cursor.input = container.getInput();

		// create empty cursor to simulate hiding the cursor
		try {
			int min = org.lwjgl.input.Cursor.getMinCursorSize();
			IntBuffer tmp = BufferUtils.createIntBuffer(min * min);
			emptyCursor = new org.lwjgl.input.Cursor(min, min, min/2, min/2, 1, tmp, null);
		} catch (LWJGLException e) {
			ErrorHandler.error("Failed to create hidden cursor.", e, true);
		}
	}

	/**
	 * Constructor.
	 */
	public Cursor() {}

	/**
	 * Draws the cursor.
	 */
	public void draw() {
		int state = game.getCurrentStateID();
		boolean mousePressed =
			(((state == Opsu.STATE_GAME || state == Opsu.STATE_GAMEPAUSEMENU) && Utils.isGameKeyPressed()) ||
			((input.isMouseButtonDown(Input.MOUSE_LEFT_BUTTON) || input.isMouseButtonDown(Input.MOUSE_RIGHT_BUTTON)) &&
			!(state == Opsu.STATE_GAME && Options.isMouseDisabled())));
		draw(input.getMouseX(), input.getMouseY(), mousePressed);
	}

	/**
	 * Draws the cursor.
	 * @param mouseX the mouse x coordinate
	 * @param mouseY the mouse y coordinate
	 * @param mousePressed whether or not the mouse button is pressed
	 */
	public void draw(int mouseX, int mouseY, boolean mousePressed) {
		if (Options.isCursorDisabled())
			return;

		Skin skin = Options.getSkin();

		// determine correct cursor image
		Image cursor, cursorTrail, cursorMiddle = null;
		if (Options.isSkinCursorForced()) {
			cursor = GameImage.CURSOR.getDefaultImage();
			cursorTrail = GameImage.CURSOR_TRAIL.getDefaultImage();
		} else {
			cursor = GameImage.CURSOR.getImage();
			cursorTrail = GameImage.CURSOR_TRAIL.getImage();
		}

		if (GameImage.CURSOR.hasBeatmapSkinImage() && !Options.isSkinCursorForced()) {
			if (GameImage.CURSOR_MIDDLE.hasBeatmapSkinImage())
				cursorMiddle = GameImage.CURSOR_MIDDLE.getImage();
		} else if (GameImage.CURSOR.hasGameSkinImage()) {
			if (GameImage.CURSOR_MIDDLE.hasGameSkinImage())
				cursorMiddle = GameImage.CURSOR_MIDDLE.getDefaultImage();
		} else
			cursorMiddle = GameImage.CURSOR_MIDDLE.getDefaultImage();

		// scale cursor
		float cursorScaleAnimated = 1f;
		if (skin.isCursorExpanded()) {
			if (lastCursorPressState != mousePressed) {
				lastCursorPressState = mousePressed;
				lastCursorPressTime = System.currentTimeMillis();
			}

			float cursorScaleChange = CURSOR_SCALE_CHANGE * AnimationEquation.IN_OUT_CUBIC.calc(
					Utils.clamp(System.currentTimeMillis() - lastCursorPressTime, 0, CURSOR_SCALE_TIME) / CURSOR_SCALE_TIME);
			cursorScaleAnimated = 1f + ((mousePressed) ? cursorScaleChange : CURSOR_SCALE_CHANGE - cursorScaleChange);
		}
		float cursorScale = cursorScaleAnimated * Options.getCursorScale();
		if (cursorScale != 1f) {
			cursor = cursor.getScaledCopy(cursorScale);
			cursorTrail = cursorTrail.getScaledCopy(cursorScale);
		}

		// TODO: use an image buffer
		int removeCount = 0;
		float FPSmod = Math.max(container.getFPS(), 1) / 60f;
		if (cursorMiddle != null) {
			// new style: add all points between cursor movements
			if (lastPosition == null) {
				lastPosition = new Point(mouseX, mouseY);
				return;
			}
			addCursorPoints(lastPosition.x, lastPosition.y, mouseX, mouseY);
			lastPosition.move(mouseX, mouseY);

			removeCount = (int) (trail.size() / (6 * FPSmod)) + 1;
		} else {
			// old style: sample one point at a time
			trail.add(new Point(mouseX, mouseY));

			int max = (int) (10 * FPSmod);
			if (trail.size() > max)
				removeCount = trail.size() - max;
		}

		// remove points from the lists
		for (int i = 0; i < removeCount && !trail.isEmpty(); i++)
			trail.remove();

		// draw a fading trail
		float alpha = 0f;
		float t = 2f / trail.size();
		int cursorTrailWidth = cursorTrail.getWidth(), cursorTrailHeight = cursorTrail.getHeight();
		float cursorTrailRotation = (skin.isCursorTrailRotated()) ? cursorAngle : 0;
		float offsetX = -cursorTrailWidth / 2f, offsetY = -cursorTrailHeight / 2f;
		if (!skin.isCursorCentered())
			offsetX = offsetY = 0f;
		cursorTrail.startUse();
		for (Point p : trail) {
			alpha += t;
			cursorTrail.setImageColor(1f, 1f, 1f, alpha);
			cursorTrail.drawEmbedded(
				p.x + offsetX, p.y + offsetY,
				cursorTrailWidth, cursorTrailHeight, cursorTrailRotation
			);
		}
		cursorTrail.drawEmbedded(
			mouseX + offsetX, mouseY + offsetY,
			cursorTrailWidth, cursorTrailHeight, cursorTrailRotation
		);
		cursorTrail.endUse();

		// draw the other components
		if (skin.isCursorRotated())
			cursor.setRotation(cursorAngle);
		if (skin.isCursorCentered()) {
			cursor.drawCentered(mouseX, mouseY);
			if (cursorMiddle != null)
				cursorMiddle.drawCentered(mouseX, mouseY);
		} else {
			cursor.draw(mouseX, mouseY);
			if (cursorMiddle != null)
				cursorMiddle.drawCentered(mouseX + cursor.getWidth() / 2, mouseY + cursor.getHeight() / 2);
		}
	}

	/**
	 * Adds all points between (x1, y1) and (x2, y2) to the cursor point lists.
	 * @author http://rosettacode.org/wiki/Bitmap/Bresenham's_line_algorithm#Java
	 */
	private void addCursorPoints(int x1, int y1, int x2, int y2) {
		// delta of exact value and rounded value of the dependent variable
		int d = 0;
		int dy = Math.abs(y2 - y1);
		int dx = Math.abs(x2 - x1);

		int dy2 = (dy << 1);  // slope scaling factors to avoid floating
		int dx2 = (dx << 1);  // point
		int ix = x1 < x2 ? 1 : -1;  // increment direction
		int iy = y1 < y2 ? 1 : -1;

		int k = 5;  // sample size
		if (dy <= dx) {
			for (int i = 0; ; i++) {
				if (i == k) {
					trail.add(new Point(x1, y1));
					i = 0;
				}
				if (x1 == x2)
					break;
				x1 += ix;
				d += dy2;
				if (d > dx) {
					y1 += iy;
					d -= dx2;
				}
			}
		} else {
			for (int i = 0; ; i++) {
				if (i == k) {
					trail.add(new Point(x1, y1));
					i = 0;
				}
				if (y1 == y2)
					break;
				y1 += iy;
				d += dx2;
				if (d > dy) {
					x1 += ix;
					d -= dy2;
				}
			}
		}
	}

	/**
	 * Rotates the cursor by a degree determined by a delta interval.
	 * If the old style cursor is being used, this will do nothing.
	 * @param delta the delta interval since the last call
	 */
	public void update(int delta) {
		cursorAngle += delta / 40f;
		cursorAngle %= 360;
	}

	/**
	 * Resets all cursor data and beatmap skins.
	 */
	public void reset() {
		// destroy skin images
		GameImage.CURSOR.destroyBeatmapSkinImage();
		GameImage.CURSOR_MIDDLE.destroyBeatmapSkinImage();
		GameImage.CURSOR_TRAIL.destroyBeatmapSkinImage();

		// reset locations
		resetLocations();

		// reset angles
		cursorAngle = 0f;
	}

	/**
	 * Resets all cursor location data.
	 */
	public void resetLocations() {
		lastPosition = null;
		trail.clear();
	}

	/**
	 * Returns whether or not the cursor is skinned.
	 */
	public boolean isBeatmapSkinned() {
		return (GameImage.CURSOR.hasBeatmapSkinImage() ||
		        GameImage.CURSOR_MIDDLE.hasBeatmapSkinImage() ||
		        GameImage.CURSOR_TRAIL.hasBeatmapSkinImage());
	}

	/**
	 * Hides the cursor, if possible.
	 */
	public void hide() {
		if (emptyCursor != null) {
			try {
				container.setMouseCursor(emptyCursor, 0, 0);
			} catch (SlickException e) {
				ErrorHandler.error("Failed to hide the cursor.", e, true);
			}
		}
	}

	/**
	 * Unhides the cursor.
	 */
	public void show() {
		container.setDefaultMouseCursor();
	}
}
