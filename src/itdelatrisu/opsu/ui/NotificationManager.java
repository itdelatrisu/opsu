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

import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;

/**
 * Notification manager.
 */
public class NotificationManager {
	/** Duration, in milliseconds, to display bar notifications. */
	private static final int BAR_NOTIFICATION_TIME = 1500;

	/** The current bar notification string. */
	private String barNotif;

	/** The current bar notification timer. */
	private int barNotifTimer = -1;

	/** The game container. */
	private final GameContainer container;

	/**
	 * Constructor.
	 * @param container the game container
	 */
	public NotificationManager(GameContainer container) {
		this.container = container;
	}

	/**
	 * Draws all notifications.
	 * @param g the graphics context
	 */
	public void draw(Graphics g) {
		drawBarNotification(g);
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
				midY - Fonts.LARGE.getLineHeight() / 2.2f,
				barNotif, Colors.WHITE_ALPHA);
		Colors.BLACK_ALPHA.a = oldAlphaB;
		Colors.WHITE_ALPHA.a = oldAlphaW;
	}
	
	/**
	 * Updates all notifications by a delta interval.
	 * @param delta the delta interval since the last call.
	 */
	public void update(int delta) {
		// update bar notification
		if (barNotifTimer > -1 && barNotifTimer < BAR_NOTIFICATION_TIME) {
			barNotifTimer += delta;
			if (barNotifTimer > BAR_NOTIFICATION_TIME)
				barNotifTimer = BAR_NOTIFICATION_TIME;
		}
	}

	/**
	 * Submits a bar notification for drawing.
	 * Must be called with {@link #drawBarNotification(Graphics)}.
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
