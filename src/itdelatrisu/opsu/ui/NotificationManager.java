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

import itdelatrisu.opsu.ui.animations.AnimationEquation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.MouseListener;
import org.newdawn.slick.UnicodeFont;

/**
 * Notification manager.
 */
public class NotificationManager {
	/** Duration, in milliseconds, to display bubble notifications. */
	private static final int NOTIFICATION_TIME = 8000;

	/** Duration, in milliseconds, to animate bubble notifications in/out. */
	private static final int NOTIFICATION_ANIMATION_TIME = 450;

	/** Duration, in milliseconds, to display bar notifications. */
	private static final int BAR_NOTIFICATION_TIME = 1500;

	/** Listener for notification clicks. */
	public interface NotificationListener {
		/** Fired when this notification is clicked. */
		public void click();
	}

	/** Notification. */
	private class BubbleNotification {
		/** The listener. */
		private final NotificationListener listener;

		/** The notification string. */
		private final String message;

		/** The lines of text. */
		private final List<String> lines;

		/** The border color. */
		private final Color borderColor, borderFocusColor;

		/** The timer. */
		private int time = 0;

		/** The coordinates. */
		private int x, y;

		/** The dimensions. */
		private final int width, height;

		/** The font to use. */
		private final UnicodeFont font = Fonts.SMALLBOLD;

		/** Whether this notification has been clicked. */
		private boolean clicked = false;

		/**
		 * Creates a new bubble notification.
		 * @param s the notification string
		 * @param c the border color
		 * @param listener the listener
		 * @param width the element width
		 */
		public BubbleNotification(String s, Color c, NotificationListener listener, int width) {
			this.message = s;
			this.lines = Fonts.wrap(font, s, (int) (width * 0.96f), true);
			this.borderColor = new Color(c);
			this.borderFocusColor = (borderColor.equals(Color.white)) ?
				new Color(Colors.GREEN) : new Color(Color.white);
			this.listener = listener;
			this.width = width;
			this.height = (int) (font.getLineHeight() * (lines.size() + 0.5f));
		}

		/** Returns the x position. */
		public int getX() { return x; }

		/** Returns the y position. */
		public int getY() { return y; }

		/** Returns the width of this notification. */
		public int getWidth() { return width; }

		/** Returns the height of this notification. */
		public int getHeight() { return height; }

		/**
		 * Sets the position of this bubble.
		 * @param x the x coordinate
		 * @param y the y coordinate
		 */
		public void setPosition(int x, int y) {
			this.x = x;
			this.y = y;
		}

		/**
		 * Returns true if the coordinates are within the bubble bounds.
		 * @param cx the x coordinate
		 * @param cy the y coordinate
		 */
		public boolean contains(int cx, int cy) {
			return ((cx > x && cx < x + width) && (cy > y && cy < y + height));
		}

		/**
		 * Draws the bubble notification.
		 * @param g the graphics context
		 * @param focus true if focused
		 */
		public void draw(Graphics g, boolean focus) {
			// get animation values
			float alpha = 1f;
			int offset = 0;
			if (time < NOTIFICATION_ANIMATION_TIME) {
				float t = AnimationEquation.OUT_BACK.calc((float) time / NOTIFICATION_ANIMATION_TIME);
				alpha = t;
				offset = (int) ((1f - t) * (width / 2f));
			} else if (NOTIFICATION_TIME - time < NOTIFICATION_ANIMATION_TIME) {
				float t = (float) (NOTIFICATION_TIME - time) / NOTIFICATION_ANIMATION_TIME;
				alpha = t;
			}

			float oldBlackAlpha = Colors.BLACK_ALPHA.a;
			float oldWhiteAlpha = Colors.WHITE_FADE.a;
			Colors.BLACK_ALPHA.a = alpha;
			Colors.WHITE_FADE.a = alpha;
			Color border = focus ? borderFocusColor : borderColor;
			border.a = alpha;

			// draw rectangle
			int lineHeight = font.getLineHeight();
			int cornerRadius = 6;
			g.setColor(Colors.BLACK_ALPHA);
			g.fillRoundRect(x + offset, y, width, height, cornerRadius);
			g.setLineWidth(1f);
			g.setColor(border);
			g.drawRoundRect(x + offset, y, width, height, cornerRadius);

			// draw text
			Fonts.loadGlyphs(font, message);
			int cx = x + (int) (width * 0.02f) + offset, cy = y + lineHeight / 4;
			for (String s : lines) {
				font.drawString(cx, cy, s, Colors.WHITE_FADE);
				cy += lineHeight;
			}

			Colors.BLACK_ALPHA.a = oldBlackAlpha;
			Colors.WHITE_FADE.a = oldWhiteAlpha;
		}

		/**
		 * Updates the bubble notification by a delta interval.
		 * @param delta the delta interval since the last call.
		 * @return true if an update was applied, false if the timer is finished
		 */
		public boolean update(int delta) {
			if (isFinished())
				return false;

			time = Math.min(time + delta, NOTIFICATION_TIME);
			return true;
		}

		/** Returns whether this notification is finished being displayed. */
		public boolean isFinished() { return time >= NOTIFICATION_TIME; }

		/** Returns whether this notification has finished animating in. */
		public boolean isStartAnimationFinished() { return time >= NOTIFICATION_ANIMATION_TIME; }

		/**
		 * Click handler.
		 * @param doAction whether to fire the listener
		 */
		public synchronized void click(boolean doAction) {
			if (isFinished() || clicked)
				return;
			clicked = true;
			time = Math.max(time, NOTIFICATION_TIME - NOTIFICATION_ANIMATION_TIME);
			if (listener != null && doAction)
				listener.click();
		}
	}

	/** All bubble notifications. */
	private List<BubbleNotification> notifications;

	/** The current bar notification string. */
	private String barNotif;

	/** The current bar notification timer. */
	private int barNotifTimer = -1;

	// game-related variables
	private final GameContainer container;
	private final Input input;

	/**
	 * Constructor.
	 * @param container the game container
	 */
	public NotificationManager(GameContainer container) {
		this.container = container;
		this.input = container.getInput();
		this.notifications = Collections.synchronizedList(new ArrayList<BubbleNotification>());
		input.addMouseListener(new MouseListener() {
			@Override
			public void mousePressed(int button, int x, int y) {
				if (button == Input.MOUSE_MIDDLE_BUTTON)
					return;
				synchronized (notifications) {
					for (BubbleNotification n : notifications) {
						if (n.contains(x, y)) {
							n.click(button == Input.MOUSE_LEFT_BUTTON);
							break;
						}
					}
				}
			}
			@Override public void setInput(Input input) {}
			@Override public boolean isAcceptingInput() { return true; }
			@Override public void inputEnded() {}
			@Override public void inputStarted() {}
			@Override public void mouseWheelMoved(int change) {}
			@Override public void mouseClicked(int button, int x, int y, int clickCount) {}
			@Override public void mouseReleased(int button, int x, int y) {}
			@Override public void mouseMoved(int oldx, int oldy, int newx, int newy) {}
			@Override public void mouseDragged(int oldx, int oldy, int newx, int newy) {}
		});
	}

	/**
	 * Draws all notifications.
	 * @param g the graphics context
	 */
	public void draw(Graphics g) {
		drawNotifications(g);
		drawBarNotification(g);
	}

	/**
	 * Draws the notifications sent from {@link #sendNotification(String, Color)}.
	 * @param g the graphics context
	 */
	private void drawNotifications(Graphics g) {
		int mouseX = input.getMouseX(), mouseY = input.getMouseY();
		synchronized (notifications) {
			for (BubbleNotification n : notifications) {
				if (!n.isFinished())
					n.draw(g, n.contains(mouseX, mouseY));
			}
		}
	}

	/**
	 * Draws the notification sent from {@link #sendBarNotification(String)}.
	 * @param g the graphics context
	 */
	private void drawBarNotification(Graphics g) {
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
			midY - Fonts.LARGE.getLineHeight() / 2f,
			barNotif, Colors.WHITE_ALPHA
		);
		Colors.BLACK_ALPHA.a = oldAlphaB;
		Colors.WHITE_ALPHA.a = oldAlphaW;
	}

	/**
	 * Updates all notifications by a delta interval.
	 * @param delta the delta interval since the last call.
	 */
	public void update(int delta) {
		// update notifications
		boolean allFinished = true, startFinished = true;
		synchronized (notifications) {
			for (BubbleNotification n : notifications) {
				if (startFinished) {
					if (n.update(delta))
						allFinished = false;
				} else if (!n.isFinished())
					allFinished = false;
				startFinished = n.isStartAnimationFinished();
			}
			if (allFinished)
				notifications.clear();  // clear when all are finished showing
		}

		// update bar notification
		if (barNotifTimer > -1 && barNotifTimer < BAR_NOTIFICATION_TIME) {
			barNotifTimer += delta;
			if (barNotifTimer > BAR_NOTIFICATION_TIME)
				barNotifTimer = BAR_NOTIFICATION_TIME;
		}
	}

	/**
	 * Submits a bubble notification for drawing.
	 * @param s the notification string
	 */
	public void sendNotification(String s) { sendNotification(s, Color.white); }

	/**
	 * Submits a bubble notification for drawing.
	 * @param s the notification string
	 * @param c the border color
	 */
	public void sendNotification(String s, Color c) { sendNotification(s, c, null); }

	/**
	 * Submits a bubble notification for drawing.
	 * @param s the notification string
	 * @param c the border color
	 * @param listener the listener
	 */
	public synchronized void sendNotification(String s, Color c, NotificationListener listener) {
		BubbleNotification notif = new BubbleNotification(s, c, listener, container.getWidth() / 5);
		int x, y;
		int bottomY = (int) (container.getHeight() * 0.9645f);
		int paddingX = 6;
		int paddingY = (int) (container.getHeight() * 0.0144f);
		if (notifications.isEmpty()) {
			x = container.getWidth() - paddingX - notif.getWidth();
			y = bottomY - notif.getHeight();
		} else {
			BubbleNotification n = notifications.get(notifications.size() - 1);
			x = n.getX();
			y = n.getY() - paddingY - notif.getHeight();
			if (y <= paddingY) {
				x -= paddingX + notif.getWidth();
				y = bottomY - notif.getHeight();
			}
		}
		notif.setPosition(x, y);
		notifications.add(notif);
	}

	/**
	 * Submits a bar notification for drawing.
	 * @param s the notification string
	 */
	public void sendBarNotification(String s) {
		if (s != null) {
			barNotif = s;
			barNotifTimer = 0;
		}
	}

	/**
	 * Resets all notifications.
	 */
	public void reset() {
		// resets the bar notification
		barNotifTimer = -1;
		barNotif = null;
	}
}
