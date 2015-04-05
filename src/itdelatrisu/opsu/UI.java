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

package itdelatrisu.opsu;

import itdelatrisu.opsu.audio.SoundController;

import java.nio.IntBuffer;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Cursor;
import org.newdawn.slick.Animation;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.StateBasedGame;

/**
 * Class primarily used for drawing UI components.
 */
public class UI {
	/** Back button. */
	private static MenuButton backButton;

	/** Empty cursor. */
	private static Cursor emptyCursor;

	/** Last cursor coordinates. */
	private static int lastX = -1, lastY = -1;

	/** Cursor rotation angle. */
	private static float cursorAngle = 0f;

	/** Stores all previous cursor locations to display a trail. */
	private static LinkedList<Integer>
		cursorX = new LinkedList<Integer>(),
		cursorY = new LinkedList<Integer>();

	/** Time to show volume image, in milliseconds. */
	private static final int VOLUME_DISPLAY_TIME = 1500;

	/** Volume display elapsed time. */
	private static int volumeDisplay = -1;

	/** The current bar notification string. */
	private static String barNotif;

	/** The current bar notification timer. */
	private static int barNotifTimer = -1;

	/** Duration, in milliseconds, to display bar notifications. */
	private static final int BAR_NOTIFICATION_TIME = 1250;

	/** The current tooltip. */
	private static String tooltip;

	/** Whether or not to check the current tooltip for line breaks. */
	private static boolean tooltipNewlines;

	/** The current tooltip timer. */
	private static int tooltipTimer = -1;

	/** Duration, in milliseconds, to fade tooltips. */
	private static final int TOOLTIP_FADE_TIME = 200;

	// game-related variables
	private static GameContainer container;
	private static StateBasedGame game;
	private static Input input;

	// This class should not be instantiated.
	private UI() {}

	/**
	 * Initializes UI data.
	 * @param container the game container
	 * @param game the game object
	 * @throws SlickException
	 */
	public static void init(GameContainer container, StateBasedGame game)
			throws SlickException {
		UI.container = container;
		UI.game = game;
		UI.input = container.getInput();

		// hide native cursor
		try {
			int min = Cursor.getMinCursorSize();
			IntBuffer tmp = BufferUtils.createIntBuffer(min * min);
			emptyCursor = new Cursor(min, min, min/2, min/2, 1, tmp, null);
			hideCursor();
		} catch (LWJGLException e) {
			ErrorHandler.error("Failed to create hidden cursor.", e, true);
		}

		// back button
		if (GameImage.MENU_BACK.getImages() != null) {
			Animation back = GameImage.MENU_BACK.getAnimation(120);
			backButton = new MenuButton(back, back.getWidth() / 2f, container.getHeight() - (back.getHeight() / 2f));
		} else {
			Image back = GameImage.MENU_BACK.getImage();
			backButton = new MenuButton(back, back.getWidth() / 2f, container.getHeight() - (back.getHeight() / 2f));
		}
		backButton.setHoverExpand(MenuButton.Expand.UP_RIGHT);
	}

	/**
	 * Updates all UI components by a delta interval.
	 * @param delta the delta interval since the last call.
	 */
	public static void update(int delta) {
		updateCursor(delta);
		updateVolumeDisplay(delta);
		updateBarNotification(delta);
		if (tooltipTimer > 0)
			tooltipTimer -= delta;
	}

	/**
	 * Draws the global UI components: cursor, FPS, volume bar, bar notifications.
	 * @param g the graphics context
	 */
	public static void draw(Graphics g) {
		drawBarNotification(g);
		drawVolume(g);
		drawFPS();
		drawCursor();
		drawTooltip(g);
	}

	/**
	 * Draws the global UI components: cursor, FPS, volume bar, bar notifications.
	 * @param g the graphics context
	 * @param mouseX the mouse x coordinate
	 * @param mouseY the mouse y coordinate
	 * @param mousePressed whether or not the mouse button is pressed
	 */
	public static void draw(Graphics g, int mouseX, int mouseY, boolean mousePressed) {
		drawBarNotification(g);
		drawVolume(g);
		drawFPS();
		drawCursor(mouseX, mouseY, mousePressed);
		drawTooltip(g);
	}

	/**
	 * Resets the necessary UI components upon entering a state.
	 */
	public static void enter() {
		backButton.resetHover();
		resetBarNotification();
		resetCursorLocations();
		resetTooltip();
	}

	/**
	 * Returns the 'menu-back' MenuButton.
	 */
	public static MenuButton getBackButton() { return backButton; }

	/**
	 * Draws a tab image and text centered at a location.
	 * @param x the center x coordinate
	 * @param y the center y coordinate
	 * @param text the text to draw inside the tab
	 * @param selected whether the tab is selected (white) or not (red)
	 * @param isHover whether to include a hover effect (unselected only)
	 */
	public static void drawTab(float x, float y, String text, boolean selected, boolean isHover) {
		Image tabImage = GameImage.MENU_TAB.getImage();
		float tabTextX = x - (Utils.FONT_MEDIUM.getWidth(text) / 2);
		float tabTextY = y - (tabImage.getHeight() / 2.5f);
		Color filter, textColor;
		if (selected) {
			filter = Color.white;
			textColor = Color.black;
		} else {
			filter = (isHover) ? Utils.COLOR_RED_HOVER : Color.red;
			textColor = Color.white;
		}
		tabImage.drawCentered(x, y, filter);
		Utils.FONT_MEDIUM.drawString(tabTextX, tabTextY, text, textColor);
	}

	/**
	 * Draws the cursor.
	 */
	public static void drawCursor() {
		int state = game.getCurrentStateID();
		boolean mousePressed =
			(((state == Opsu.STATE_GAME || state == Opsu.STATE_GAMEPAUSEMENU) && Utils.isGameKeyPressed()) ||
			((input.isMouseButtonDown(Input.MOUSE_LEFT_BUTTON) || input.isMouseButtonDown(Input.MOUSE_RIGHT_BUTTON)) &&
			!(state == Opsu.STATE_GAME && Options.isMouseDisabled())));
		drawCursor(input.getMouseX(), input.getMouseY(), mousePressed);
	}

	/**
	 * Draws the cursor.
	 * @param mouseX the mouse x coordinate
	 * @param mouseY the mouse y coordinate
	 * @param mousePressed whether or not the mouse button is pressed
	 */
	public static void drawCursor(int mouseX, int mouseY, boolean mousePressed) {
		// determine correct cursor image
		// TODO: most beatmaps don't skin CURSOR_MIDDLE, so how to determine style?
		Image cursor = null, cursorMiddle = null, cursorTrail = null;
		boolean skinned = GameImage.CURSOR.hasSkinImage();
		boolean newStyle = (skinned) ? true : Options.isNewCursorEnabled();
		if (skinned || newStyle) {
			cursor = GameImage.CURSOR.getImage();
			cursorTrail = GameImage.CURSOR_TRAIL.getImage();
		} else {
			cursor = GameImage.CURSOR_OLD.getImage();
			cursorTrail = GameImage.CURSOR_TRAIL_OLD.getImage();
		}
		if (newStyle)
			cursorMiddle = GameImage.CURSOR_MIDDLE.getImage();

		int removeCount = 0;
		int FPSmod = (Options.getTargetFPS() / 60);

		// TODO: use an image buffer
		/*
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

			removeCount = (cursorX.size() / (6 * FPSmod)) + 1;
		} else {
			// old style: sample one point at a time
			cursorX.add(mouseX);
			cursorY.add(mouseY);

			int max = 10 * FPSmod;
			if (cursorX.size() > max)
				removeCount = cursorX.size() - max;
		}*/

		// remove points from the lists
		for (int i = 0; i < removeCount && !cursorX.isEmpty(); i++) {
			cursorX.remove();
			cursorY.remove();
		}

		// draw a fading trail
		float alpha = 0f;
		float t = 2f / cursorX.size();
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

		// increase the cursor size if pressed
		final float scale = 1.25f;
		if (mousePressed) {
			cursor = cursor.getScaledCopy(scale);
			if (newStyle)
				cursorMiddle = cursorMiddle.getScaledCopy(scale);
		}

		// draw the other components
		if (newStyle)
			cursor.setRotation(cursorAngle);
		cursor.drawCentered(mouseX, mouseY);
		if (newStyle)
			cursorMiddle.drawCentered(mouseX, mouseY);
	}

	/**
	 * Adds all points between (x1, y1) and (x2, y2) to the cursor point lists.
	 * @author http://rosettacode.org/wiki/Bitmap/Bresenham's_line_algorithm#Java
	 */
	private static void addCursorPoints(int x1, int y1, int x2, int y2) {
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
	private static void updateCursor(int delta) {
		cursorAngle += delta / 40f;
		cursorAngle %= 360;
	}

	/**
	 * Resets all cursor data and skins.
	 */
	public static void resetCursor() {
		GameImage.CURSOR.destroySkinImage();
		GameImage.CURSOR_MIDDLE.destroySkinImage();
		GameImage.CURSOR_TRAIL.destroySkinImage();
		cursorAngle = 0f;
		GameImage.CURSOR.getImage().setRotation(0f);
	}

	/**
	 * Resets all cursor location data.
	 */
	private static void resetCursorLocations() {
		lastX = lastY = -1;
		cursorX.clear();
		cursorY.clear();
	}

	/**
	 * Hides the cursor, if possible.
	 */
	public static void hideCursor() {
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
	public static void showCursor() {
		container.setDefaultMouseCursor();
	}

	/**
	 * Draws the FPS at the bottom-right corner of the game container.
	 * If the option is not activated, this will do nothing.
	 */
	public static void drawFPS() {
		if (!Options.isFPSCounterEnabled())
			return;

		String fps = String.format("%dFPS", container.getFPS());
		Utils.FONT_BOLD.drawString(
				container.getWidth() * 0.997f - Utils.FONT_BOLD.getWidth(fps),
				container.getHeight() * 0.997f - Utils.FONT_BOLD.getHeight(fps),
				Integer.toString(container.getFPS()), Color.white
		);
		Utils.FONT_DEFAULT.drawString(
				container.getWidth() * 0.997f - Utils.FONT_BOLD.getWidth("FPS"),
				container.getHeight() * 0.997f - Utils.FONT_BOLD.getHeight("FPS"),
				"FPS", Color.white
		);
	}

	/**
	 * Draws the volume bar on the middle right-hand side of the game container.
	 * Only draws if the volume has recently been changed using with {@link #changeVolume(int)}.
	 * @param g the graphics context
	 */
	public static void drawVolume(Graphics g) {
		if (volumeDisplay == -1)
			return;

		int width = container.getWidth(), height = container.getHeight();
		Image img = GameImage.VOLUME.getImage();

		// move image in/out
		float xOffset = 0;
		float ratio = (float) volumeDisplay / VOLUME_DISPLAY_TIME;
		if (ratio <= 0.1f)
			xOffset = img.getWidth() * (1 - (ratio * 10f));
		else if (ratio >= 0.9f)
			xOffset = img.getWidth() * (1 - ((1 - ratio) * 10f));

		img.drawCentered(width - img.getWidth() / 2f + xOffset, height / 2f);
		float barHeight = img.getHeight() * 0.9f;
		float volume = Options.getMasterVolume();
		g.setColor(Color.white);
		g.fillRoundRect(
				width - (img.getWidth() * 0.368f) + xOffset,
				(height / 2f) - (img.getHeight() * 0.47f) + (barHeight * (1 - volume)),
				img.getWidth() * 0.15f, barHeight * volume, 3
		);
	}

	/**
	 * Updates volume display by a delta interval.
	 * @param delta the delta interval since the last call
	 */
	private static void updateVolumeDisplay(int delta) {
		if (volumeDisplay == -1)
			return;

		volumeDisplay += delta;
		if (volumeDisplay > VOLUME_DISPLAY_TIME)
			volumeDisplay = -1;
	}

	/**
	 * Changes the master volume by a unit (positive or negative).
	 * @param units the number of units
	 */
	public static void changeVolume(int units) {
		final float UNIT_OFFSET = 0.05f;
		Options.setMasterVolume(container, Utils.getBoundedValue(Options.getMasterVolume(), UNIT_OFFSET * units, 0f, 1f));
		if (volumeDisplay == -1)
			volumeDisplay = 0;
		else if (volumeDisplay >= VOLUME_DISPLAY_TIME / 10)
			volumeDisplay = VOLUME_DISPLAY_TIME / 10;
	}

	/**
	 * Draws loading progress (OSZ unpacking, OsuFile parsing, sound loading)
	 * at the bottom of the screen.
	 */
	public static void drawLoadingProgress(Graphics g) {
		String text, file;
		int progress;

		// determine current action
		if ((file = OszUnpacker.getCurrentFileName()) != null) {
			text = "Unpacking new beatmaps...";
			progress = OszUnpacker.getUnpackerProgress();
		} else if ((file = OsuParser.getCurrentFileName()) != null) {
			text = (OsuParser.getStatus() == OsuParser.Status.INSERTING) ?
					"Updating database..." : "Loading beatmaps...";
			progress = OsuParser.getParserProgress();
		} else if ((file = SoundController.getCurrentFileName()) != null) {
			text = "Loading sounds...";
			progress = SoundController.getLoadingProgress();
		} else
			return;

		// draw loading info
		float marginX = container.getWidth() * 0.02f, marginY = container.getHeight() * 0.02f;
		float lineY = container.getHeight() - marginY;
		int lineOffsetY = Utils.FONT_MEDIUM.getLineHeight();
		if (Options.isLoadVerbose()) {
			// verbose: display percentages and file names
			Utils.FONT_MEDIUM.drawString(
					marginX, lineY - (lineOffsetY * 2),
					String.format("%s (%d%%)", text, progress), Color.white);
			Utils.FONT_MEDIUM.drawString(marginX, lineY - lineOffsetY, file, Color.white);
		} else {
			// draw loading bar
			Utils.FONT_MEDIUM.drawString(marginX, lineY - (lineOffsetY * 2), text, Color.white);
			g.setColor(Color.white);
			g.fillRoundRect(marginX, lineY - (lineOffsetY / 2f),
					(container.getWidth() - (marginX * 2f)) * progress / 100f, lineOffsetY / 4f, 4
			);
		}
	}

	/**
	 * Draws a scroll bar.
	 * @param g the graphics context
	 * @param unitIndex the unit index
	 * @param totalUnits the total number of units
	 * @param maxShown the maximum number of units shown at one time
	 * @param unitBaseX the base x coordinate of the units
	 * @param unitBaseY the base y coordinate of the units
	 * @param unitWidth the width of a unit
	 * @param unitHeight the height of a unit
	 * @param unitOffsetY the y offset between units
	 * @param bgColor the scroll bar area background color (null if none)
	 * @param scrollbarColor the scroll bar color
	 * @param right whether or not to place the scroll bar on the right side of the unit
	 */
	public static void drawScrollbar(
			Graphics g, int unitIndex, int totalUnits, int maxShown,
			float unitBaseX, float unitBaseY, float unitWidth, float unitHeight, float unitOffsetY,
			Color bgColor, Color scrollbarColor, boolean right
	) {
		float scrollbarWidth = container.getWidth() * 0.00347f;
		float heightRatio = (float) (2.6701f * Math.exp(-0.81 * Math.log(totalUnits)));
		float scrollbarHeight = container.getHeight() * heightRatio;
		float scrollAreaHeight = unitHeight + unitOffsetY * (maxShown - 1);
		float offsetY = (scrollAreaHeight - scrollbarHeight) * ((float) unitIndex / (totalUnits - maxShown));
		float scrollbarX = unitBaseX + unitWidth - ((right) ? scrollbarWidth : 0);
		if (bgColor != null) {
			g.setColor(bgColor);
			g.fillRect(scrollbarX, unitBaseY, scrollbarWidth, scrollAreaHeight);
		}
		g.setColor(scrollbarColor);
		g.fillRect(scrollbarX, unitBaseY + offsetY, scrollbarWidth, scrollbarHeight);
	}

	/**
	 * Sets or updates a tooltip for drawing.
	 * Must be called with {@link #drawTooltip(Graphics)}.
	 * @param delta the delta interval since the last call
	 * @param s the tooltip text
	 * @param newlines whether to check for line breaks ('\n')
	 */
	public static void updateTooltip(int delta, String s, boolean newlines) {
		if (s != null) {
			tooltip = s;
			tooltipNewlines = newlines;
			if (tooltipTimer <= 0)
				tooltipTimer = delta;
			else
				tooltipTimer += delta * 2;
			if (tooltipTimer > TOOLTIP_FADE_TIME)
				tooltipTimer = TOOLTIP_FADE_TIME;
		}
	}

	/**
	 * Draws a tooltip, if any, near the current mouse coordinates,
	 * bounded by the container dimensions.
	 * @param g the graphics context
	 */
	public static void drawTooltip(Graphics g) {
		if (tooltipTimer <= 0 || tooltip == null)
			return;

		int containerWidth = container.getWidth(), containerHeight = container.getHeight();
		int margin = containerWidth / 100, textMarginX = 2;
		int offset = GameImage.CURSOR_MIDDLE.getImage().getWidth() / 2;
		int lineHeight = Utils.FONT_SMALL.getLineHeight();
		int textWidth = textMarginX * 2, textHeight = lineHeight;
		if (tooltipNewlines) {
			String[] lines = tooltip.split("\\n");
			int maxWidth = Utils.FONT_SMALL.getWidth(lines[0]);
			for (int i = 1; i < lines.length; i++) {
				int w = Utils.FONT_SMALL.getWidth(lines[i]);
				if (w > maxWidth)
					maxWidth = w;
			}
			textWidth += maxWidth;
			textHeight += lineHeight * (lines.length - 1);
		} else
			textWidth += Utils.FONT_SMALL.getWidth(tooltip);

		// get drawing coordinates
		int x = input.getMouseX() + offset, y = input.getMouseY() + offset;
		if (x + textWidth > containerWidth - margin)
			x = containerWidth - margin - textWidth;
		else if (x < margin)
			x = margin;
		if (y + textHeight > containerHeight - margin)
			y = containerHeight - margin - textHeight;
		else if (y < margin)
			y = margin;

		// draw tooltip text inside a filled rectangle
		float alpha = (float) tooltipTimer / TOOLTIP_FADE_TIME;
		float oldAlpha = Utils.COLOR_BLACK_ALPHA.a;
		Utils.COLOR_BLACK_ALPHA.a = alpha;
		g.setColor(Utils.COLOR_BLACK_ALPHA);
		Utils.COLOR_BLACK_ALPHA.a = oldAlpha;
		g.fillRect(x, y, textWidth, textHeight);
		oldAlpha = Utils.COLOR_DARK_GRAY.a;
		Utils.COLOR_DARK_GRAY.a = alpha;
		g.setColor(Utils.COLOR_DARK_GRAY);
		g.setLineWidth(1);
		g.drawRect(x, y, textWidth, textHeight);
		Utils.COLOR_DARK_GRAY.a = oldAlpha;
		oldAlpha = Utils.COLOR_WHITE_ALPHA.a;
		Utils.COLOR_WHITE_ALPHA.a = alpha;
		Utils.FONT_SMALL.drawString(x + textMarginX, y, tooltip, Utils.COLOR_WHITE_ALPHA);
		Utils.COLOR_WHITE_ALPHA.a = oldAlpha;
	}

	/**
	 * Resets the tooltip.
	 */
	public static void resetTooltip() {
		tooltipTimer = -1;
		tooltip = null;
	}

	/**
	 * Submits a bar notification for drawing.
	 * Must be called with {@link #drawBarNotification(Graphics)}.
	 * @param s the notification string
	 */
	public static void sendBarNotification(String s) {
		if (s != null) {
			barNotif = s;
			barNotifTimer = 0;
		}
	}

	/**
	 * Updates the bar notification by a delta interval.
	 * @param delta the delta interval since the last call
	 */
	private static void updateBarNotification(int delta) {
		if (barNotifTimer > -1 && barNotifTimer < BAR_NOTIFICATION_TIME) {
			barNotifTimer += delta;
			if (barNotifTimer > BAR_NOTIFICATION_TIME)
				barNotifTimer = BAR_NOTIFICATION_TIME;
		}
	}

	/**
	 * Resets the bar notification.
	 */
	public static void resetBarNotification() {
		barNotifTimer = -1;
		barNotif = null;
	}

	/**
	 * Draws the notification sent from {@link #sendBarNotification(String)}.
	 * @param g the graphics context
	 */
	public static void drawBarNotification(Graphics g) {
		if (barNotifTimer <= 0 || barNotifTimer >= BAR_NOTIFICATION_TIME)
			return;

		float alpha = 1f;
		if (barNotifTimer >= BAR_NOTIFICATION_TIME * 0.9f)
			alpha -= 1 - ((BAR_NOTIFICATION_TIME - barNotifTimer) / (BAR_NOTIFICATION_TIME * 0.1f));
		int midX = container.getWidth() / 2, midY = container.getHeight() / 2;
		float barHeight = Utils.FONT_LARGE.getLineHeight() * (1f + 0.6f * Math.min(barNotifTimer * 15f / BAR_NOTIFICATION_TIME, 1f));
		float oldAlphaB = Utils.COLOR_BLACK_ALPHA.a, oldAlphaW = Utils.COLOR_WHITE_ALPHA.a;
		Utils.COLOR_BLACK_ALPHA.a *= alpha;
		Utils.COLOR_WHITE_ALPHA.a = alpha;
		g.setColor(Utils.COLOR_BLACK_ALPHA);
		g.fillRect(0, midY - barHeight / 2f, container.getWidth(), barHeight);
		Utils.FONT_LARGE.drawString(
				midX - Utils.FONT_LARGE.getWidth(barNotif) / 2f,
				midY - Utils.FONT_LARGE.getLineHeight() / 2.2f,
				barNotif, Utils.COLOR_WHITE_ALPHA);
		Utils.COLOR_BLACK_ALPHA.a = oldAlphaB;
		Utils.COLOR_WHITE_ALPHA.a = oldAlphaW;
	}

	/**
	 * Shows a confirmation dialog (used before exiting the game).
	 * @param message the message to display
	 * @return true if user selects "yes", false otherwise
	 */
	public static boolean showExitConfirmation(String message) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			ErrorHandler.error("Could not set system look and feel for exit confirmation.", e, true);
		}
		int n = JOptionPane.showConfirmDialog(null, message, "Warning",
				JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		return (n != JOptionPane.YES_OPTION);
	}
}
