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

package itdelatrisu.opsu.ui;

import itdelatrisu.opsu.ErrorHandler;
import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.Opsu;
import itdelatrisu.opsu.Options;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.skins.Skin;

import java.nio.IntBuffer;
import java.util.Iterator;
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
	private int lastX = -1, lastY = -1;

	/** Cursor rotation angle. */
	private float cursorAngle = 0f;

	/** Stores all previous cursor locations to display a trail. */
	private LinkedList<Integer> cursorX, cursorY;

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
	public Cursor() {
		cursorX = new LinkedList<Integer>();
		cursorY = new LinkedList<Integer>();
	}

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
		// determine correct cursor image
		Image cursor = null, cursorMiddle = null, cursorTrail = null;
		boolean skinned = GameImage.CURSOR.hasSkinImage();
		boolean newStyle, hasMiddle;
		if (skinned) {
			newStyle = true;  // osu! currently treats all beatmap cursors as new-style cursors
			hasMiddle = GameImage.CURSOR_MIDDLE.hasSkinImage();
		} else
			newStyle = hasMiddle = Options.isNewCursorEnabled();
		if (skinned || newStyle) {
			cursor = GameImage.CURSOR.getImage();
			cursorTrail = GameImage.CURSOR_TRAIL.getImage();
		} else {
			cursor = GameImage.CURSOR_OLD.getImage();
			cursorTrail = GameImage.CURSOR_TRAIL_OLD.getImage();
		}
		if (hasMiddle)
			cursorMiddle = GameImage.CURSOR_MIDDLE.getImage();

		int removeCount = 0;
		float FPSmod = Math.max(container.getFPS(), 1) / 60f;
		Skin skin = Options.getSkin();

		// scale cursor
		float cursorScale = Options.getCursorScale();
		if (mousePressed && skin.isCursorExpanded())
			cursorScale *= 1.25f;  // increase the cursor size if pressed
		if (cursorScale != 1f) {
			cursor = cursor.getScaledCopy(cursorScale);
			cursorTrail = cursorTrail.getScaledCopy(cursorScale);
		}

		// TODO: use an image buffer
		if (newStyle) {
			// new style: add all points between cursor movements
			if (lastX < 0) {
				lastX = mouseX;
				lastY = mouseY;
				return;
			}
			addCursorPoints(lastX, lastY, mouseX, mouseY);
			lastX = mouseX;
			lastY = mouseY;

			removeCount = (int) (cursorX.size() / (6 * FPSmod)) + 1;
		} else {
			// old style: sample one point at a time
			cursorX.add(mouseX);
			cursorY.add(mouseY);

			int max = (int) (10 * FPSmod);
			if (cursorX.size() > max)
				removeCount = cursorX.size() - max;
		}

		// remove points from the lists
		for (int i = 0; i < removeCount && !cursorX.isEmpty(); i++) {
			cursorX.remove();
			cursorY.remove();
		}

		// draw a fading trail
		float alpha = 0f;
		float t = 2f / cursorX.size();
		if (skin.isCursorTrailRotated())
			cursorTrail.setRotation(cursorAngle);
		Iterator<Integer> iterX = cursorX.iterator();
		Iterator<Integer> iterY = cursorY.iterator();
		while (iterX.hasNext()) {
			int cx = iterX.next();
			int cy = iterY.next();
			alpha += t;
			cursorTrail.setAlpha(alpha);
//			if (cx != x || cy != y)
				cursorTrail.drawCentered(cx, cy);
		}
		cursorTrail.drawCentered(mouseX, mouseY);

		// draw the other components
		if (newStyle && skin.isCursorRotated())
			cursor.setRotation(cursorAngle);
		cursor.drawCentered(mouseX, mouseY);
		if (hasMiddle)
			cursorMiddle.drawCentered(mouseX, mouseY);
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
					cursorX.add(x1);
					cursorY.add(y1);
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
					cursorX.add(x1);
					cursorY.add(y1);
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
	 * Resets all cursor data and skins.
	 */
	public void reset() {
		// destroy skin images
		GameImage.CURSOR.destroySkinImage();
		GameImage.CURSOR_MIDDLE.destroySkinImage();
		GameImage.CURSOR_TRAIL.destroySkinImage();

		// reset locations
		resetLocations();

		// reset angles
		cursorAngle = 0f;
		GameImage.CURSOR.getImage().setRotation(0f);
		GameImage.CURSOR_TRAIL.getImage().setRotation(0f);
	}

	/**
	 * Resets all cursor location data.
	 */
	public void resetLocations() {
		lastX = lastY = -1;
		cursorX.clear();
		cursorY.clear();
	}

	/**
	 * Returns whether or not the cursor is skinned.
	 */
	public boolean isSkinned() {
		return (GameImage.CURSOR.hasSkinImage() ||
		        GameImage.CURSOR_MIDDLE.hasSkinImage() ||
		        GameImage.CURSOR_TRAIL.hasSkinImage());
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
