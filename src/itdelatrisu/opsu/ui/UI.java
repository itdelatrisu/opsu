/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014, 2015, 2016 Jeffrey Han
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
import itdelatrisu.opsu.Options;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.beatmap.BeatmapParser;
import itdelatrisu.opsu.beatmap.OszUnpacker;
import itdelatrisu.opsu.replay.ReplayImporter;
import itdelatrisu.opsu.skins.SkinUnpacker;
import itdelatrisu.opsu.ui.animations.AnimatedValue;
import itdelatrisu.opsu.ui.animations.AnimationEquation;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import org.newdawn.slick.Animation;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.state.StateBasedGame;

/**
 * Draws common UI components.
 */
public class UI {
	/** Cursor. */
	private static Cursor cursor = new Cursor();

	/** Back button. */
	private static MenuButton backButton;

	/** Time to show volume image, in milliseconds. */
	private static final int VOLUME_DISPLAY_TIME = 1500;

	/** Volume display elapsed time. */
	private static int volumeDisplay = -1;

	/** The current bar notification string. */
	private static String barNotif;

	/** The current bar notification timer. */
	private static int barNotifTimer = -1;

	/** Duration, in milliseconds, to display bar notifications. */
	private static final int BAR_NOTIFICATION_TIME = 1500;

	/** The current tooltip. */
	private static String tooltip;

	/** Whether or not to check the current tooltip for line breaks. */
	private static boolean tooltipNewlines;

	/** The alpha level of the current tooltip (if any). */
	private static AnimatedValue tooltipAlpha = new AnimatedValue(200, 0f, 1f, AnimationEquation.LINEAR);

	// game-related variables
	private static GameContainer container;
	private static Input input;

	// This class should not be instantiated.
	private UI() {}

	/**
	 * Initializes UI data.
	 * @param container the game container
	 * @param game the game object
	 */
	public static void init(GameContainer container, StateBasedGame game) {
		UI.container = container;
		UI.input = container.getInput();

		// initialize cursor
		Cursor.init(container, game);
		cursor.hide();

		// back button
		if (GameImage.MENU_BACK.getImages() != null) {
			Animation back = GameImage.MENU_BACK.getAnimation(120);
			backButton = new MenuButton(back, back.getWidth() / 2f, container.getHeight() - (back.getHeight() / 2f));
		} else {
			Image back = GameImage.MENU_BACK.getImage();
			backButton = new MenuButton(back, back.getWidth() / 2f, container.getHeight() - (back.getHeight() / 2f));
		}
		backButton.setHoverAnimationDuration(350);
		backButton.setHoverAnimationEquation(AnimationEquation.IN_OUT_BACK);
		backButton.setHoverExpand(MenuButton.Expand.UP_RIGHT);
	}

	/**
	 * Updates all UI components by a delta interval.
	 * @param delta the delta interval since the last call.
	 */
	public static void update(int delta) {
		cursor.update(delta);
		updateVolumeDisplay(delta);
		updateBarNotification(delta);
		tooltipAlpha.update(-delta);
	}

	/**
	 * Draws the global UI components: cursor, FPS, volume bar, tooltips, bar notifications.
	 * @param g the graphics context
	 */
	public static void draw(Graphics g) {
		drawBarNotification(g);
		drawVolume(g);
		drawFPS();
		cursor.draw();
		drawTooltip(g);
	}

	/**
	 * Draws the global UI components: cursor, FPS, volume bar, tooltips, bar notifications.
	 * @param g the graphics context
	 * @param mouseX the mouse x coordinate
	 * @param mouseY the mouse y coordinate
	 * @param mousePressed whether or not the mouse button is pressed
	 */
	public static void draw(Graphics g, int mouseX, int mouseY, boolean mousePressed) {
		drawBarNotification(g);
		drawVolume(g);
		drawFPS();
		cursor.draw(mouseX, mouseY, mousePressed);
		drawTooltip(g);
	}

	/**
	 * Resets the necessary UI components upon entering a state.
	 */
	public static void enter() {
		backButton.resetHover();
		cursor.resetLocations();
		resetBarNotification();
		resetTooltip();
	}

	/**
	 * Returns the game cursor.
	 */
	public static Cursor getCursor() { return cursor; }

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
		float tabTextX = x - (Fonts.MEDIUM.getWidth(text) / 2);
		float tabTextY = y - (tabImage.getHeight() / 2);
		Color filter, textColor;
		if (selected) {
			filter = Color.white;
			textColor = Color.black;
		} else {
			filter = (isHover) ? Colors.RED_HOVER : Color.red;
			textColor = Color.white;
		}
		tabImage.drawCentered(x, y, filter);
		Fonts.MEDIUM.drawString(tabTextX, tabTextY, text, textColor);
	}

	/**
	 * Draws the FPS at the bottom-right corner of the game container.
	 * If the option is not activated, this will do nothing.
	 */
	public static void drawFPS() {
		if (!Options.isFPSCounterEnabled())
			return;

		String fps = String.format("%dFPS", container.getFPS());
		Fonts.BOLD.drawString(
				container.getWidth() * 0.997f - Fonts.BOLD.getWidth(fps),
				container.getHeight() * 0.997f - Fonts.BOLD.getHeight(fps),
				Integer.toString(container.getFPS()), Color.white
		);
		Fonts.DEFAULT.drawString(
				container.getWidth() * 0.997f - Fonts.BOLD.getWidth("FPS"),
				container.getHeight() * 0.997f - Fonts.BOLD.getHeight("FPS"),
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
		Options.setMasterVolume(container, Utils.clamp(Options.getMasterVolume() + (UNIT_OFFSET * units), 0f, 1f));
		if (volumeDisplay == -1)
			volumeDisplay = 0;
		else if (volumeDisplay >= VOLUME_DISPLAY_TIME / 10)
			volumeDisplay = VOLUME_DISPLAY_TIME / 10;
	}

	/**
	 * Draws loading progress (OSZ unpacking, beatmap parsing, replay importing, sound loading)
	 * at the bottom of the screen.
	 * @param g the graphics context
	 */
	public static void drawLoadingProgress(Graphics g) {
		String text, file;
		int progress;

		// determine current action
		if ((file = OszUnpacker.getCurrentFileName()) != null) {
			text = "Unpacking new beatmaps...";
			progress = OszUnpacker.getUnpackerProgress();
		} else if ((file = BeatmapParser.getCurrentFileName()) != null) {
			text = (BeatmapParser.getStatus() == BeatmapParser.Status.INSERTING) ?
					"Updating database..." : "Loading beatmaps...";
			progress = BeatmapParser.getParserProgress();
		} else if ((file = SkinUnpacker.getCurrentFileName()) != null) {
			text = "Unpacking skins...";
			progress = SkinUnpacker.getUnpackerProgress();
		} else if ((file = ReplayImporter.getCurrentFileName()) != null) {
			text = "Importing replays...";
			progress = ReplayImporter.getLoadingProgress();
		} else if ((file = SoundController.getCurrentFileName()) != null) {
			text = "Loading sounds...";
			progress = SoundController.getLoadingProgress();
		} else
			return;

		// draw loading info
		float marginX = container.getWidth() * 0.02f, marginY = container.getHeight() * 0.02f;
		float lineY = container.getHeight() - marginY;
		int lineOffsetY = Fonts.MEDIUM.getLineHeight();
		if (Options.isLoadVerbose()) {
			// verbose: display percentages and file names
			Fonts.MEDIUM.drawString(
					marginX, lineY - (lineOffsetY * 2),
					String.format("%s (%d%%)", text, progress), Color.white);
			Fonts.MEDIUM.drawString(marginX, lineY - lineOffsetY, file, Color.white);
		} else {
			// draw loading bar
			Fonts.MEDIUM.drawString(marginX, lineY - (lineOffsetY * 2), text, Color.white);
			g.setColor(Color.white);
			g.fillRoundRect(marginX, lineY - (lineOffsetY / 2f),
					(container.getWidth() - (marginX * 2f)) * progress / 100f, lineOffsetY / 4f, 4
			);
		}
	}

	/**
	 * Draws a scroll bar.
	 * @param g the graphics context
	 * @param position the position in the virtual area
	 * @param totalLength the total length of the virtual area
	 * @param lengthShown the length of the virtual area shown
	 * @param unitBaseX the base x coordinate
	 * @param unitBaseY the base y coordinate
	 * @param unitWidth the width of a unit
	 * @param scrollAreaHeight the height of the scroll area
	 * @param bgColor the scroll bar area background color (null if none)
	 * @param scrollbarColor the scroll bar color
	 * @param right whether or not to place the scroll bar on the right side of the unit
	 */
	public static void drawScrollbar(
			Graphics g, float position, float totalLength, float lengthShown,
			float unitBaseX, float unitBaseY, float unitWidth, float scrollAreaHeight,
			Color bgColor, Color scrollbarColor, boolean right
	) {
		float scrollbarWidth = container.getWidth() * 0.00347f;
		float scrollbarHeight = scrollAreaHeight * lengthShown / totalLength;
		float offsetY = (scrollAreaHeight - scrollbarHeight) * (position / (totalLength - lengthShown));
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
			tooltipAlpha.update(delta * 2);
		}
	}

	/**
	 * Draws a tooltip, if any, near the current mouse coordinates,
	 * bounded by the container dimensions.
	 * @param g the graphics context
	 */
	public static void drawTooltip(Graphics g) {
		if (tooltipAlpha.getTime() == 0 || tooltip == null)
			return;

		int containerWidth = container.getWidth(), containerHeight = container.getHeight();
		int margin = containerWidth / 100, textMarginX = 2;
		int offset = GameImage.CURSOR_MIDDLE.getImage().getWidth() / 2;
		int lineHeight = Fonts.SMALL.getLineHeight();
		int textWidth = textMarginX * 2, textHeight = lineHeight;
		if (tooltipNewlines) {
			String[] lines = tooltip.split("\\n");
			int maxWidth = Fonts.SMALL.getWidth(lines[0]);
			for (int i = 1; i < lines.length; i++) {
				int w = Fonts.SMALL.getWidth(lines[i]);
				if (w > maxWidth)
					maxWidth = w;
			}
			textWidth += maxWidth;
			textHeight += lineHeight * (lines.length - 1);
		} else
			textWidth += Fonts.SMALL.getWidth(tooltip);

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
		float alpha = tooltipAlpha.getValue();
		float oldAlpha = Colors.BLACK_ALPHA.a;
		Colors.BLACK_ALPHA.a = alpha;
		g.setColor(Colors.BLACK_ALPHA);
		Colors.BLACK_ALPHA.a = oldAlpha;
		g.fillRect(x, y, textWidth, textHeight);
		oldAlpha = Colors.DARK_GRAY.a;
		Colors.DARK_GRAY.a = alpha;
		g.setColor(Colors.DARK_GRAY);
		g.setLineWidth(1);
		g.drawRect(x, y, textWidth, textHeight);
		Colors.DARK_GRAY.a = oldAlpha;
		oldAlpha = Colors.WHITE_ALPHA.a;
		Colors.WHITE_ALPHA.a = alpha;
		Fonts.SMALL.drawString(x + textMarginX, y, tooltip, Colors.WHITE_ALPHA);
		Colors.WHITE_ALPHA.a = oldAlpha;
	}

	/**
	 * Resets the tooltip.
	 */
	public static void resetTooltip() {
		tooltipAlpha.setTime(0);
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
		float barHeight = Fonts.LARGE.getLineHeight() * (1f + 0.6f * Math.min(barNotifTimer * 15f / BAR_NOTIFICATION_TIME, 1f));
		float oldAlphaB = Colors.BLACK_ALPHA.a, oldAlphaW = Colors.WHITE_ALPHA.a;
		Colors.BLACK_ALPHA.a *= alpha;
		Colors.WHITE_ALPHA.a = alpha;
		g.setColor(Colors.BLACK_ALPHA);
		g.fillRect(0, midY - barHeight / 2f, container.getWidth(), barHeight);
		Fonts.LARGE.drawString(
				midX - Fonts.LARGE.getWidth(barNotif) / 2f,
				midY - Fonts.LARGE.getLineHeight() / 2.2f,
				barNotif, Colors.WHITE_ALPHA);
		Colors.BLACK_ALPHA.a = oldAlphaB;
		Colors.WHITE_ALPHA.a = oldAlphaW;
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
