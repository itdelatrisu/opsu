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

package itdelatrisu.opsu.states;

import itdelatrisu.opsu.GUIMenuButton;
import itdelatrisu.opsu.Opsu;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.states.Options.OpsuOptions;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.StateBasedGame;
import org.newdawn.slick.state.transition.EmptyTransition;
import org.newdawn.slick.state.transition.FadeInTransition;

/**
 * "Confirm Exit" state.
 * <ul>
 * <li>[Yes] - quit game
 * <li>[No]  - return to main menu
 * </ul>
 */
public class MainMenuExit extends Utils {
	/**
	 * "Yes" and "No" buttons.
	 */
	private GUIMenuButton yesButton, noButton;

	/**
	 * Initial x coordinate offsets left/right of center (for shifting animation).
	 */
	private float centerOffset;

	public MainMenuExit(int state, OpsuOptions options) {
		super(state, options);
	}

	@Override
	public void init(GameContainer container, StateBasedGame game)
			throws SlickException {
		super.init(container, game);

		int width = container.getWidth();
		int height = container.getHeight();

		centerOffset = width / 18f;

		// initialize buttons
		Image button = new Image("button-middle.png");
		Image buttonL = new Image("button-left.png");
		Image buttonR = new Image("button-right.png");
		button = button.getScaledCopy(width / 2, button.getHeight());
		yesButton = new GUIMenuButton(button, buttonL, buttonR,
				width / 2f + centerOffset, height * 0.2f
		);
		noButton = new GUIMenuButton(button, buttonL, buttonR,
				width / 2f - centerOffset, height * 0.2f + (button.getHeight() * 1.25f)
		);
	}

	@Override
	public void render(GameContainer container, StateBasedGame game, Graphics g)
			throws SlickException {
		g.setBackground(Color.black);
		g.setColor(Color.white);

		// draw text
		float c = container.getWidth() * 0.02f;
		Utils.FONT_LARGE.drawString(c, c, "Are you sure you want to exit opsu!?");

		// draw buttons
		yesButton.draw(Color.green);
		noButton.draw(Color.red);
		g.setFont(Utils.FONT_XLARGE);
		g.drawString("1. Yes",
				yesButton.getX() - (Utils.FONT_XLARGE.getWidth("1. Yes") / 2f),
				yesButton.getY() - (Utils.FONT_XLARGE.getLineHeight() / 2f)
		);
		g.drawString("2. No",
				noButton.getX() - (Utils.FONT_XLARGE.getWidth("2. No") / 2f),
				noButton.getY() - (Utils.FONT_XLARGE.getLineHeight() / 2f)
		);

		drawFPS();
		drawCursor();
	}

	@Override
	public void update(GameContainer container, StateBasedGame game, int delta)
			throws SlickException {
		Utils.updateCursor(delta);

		// move buttons to center
		float yesX = yesButton.getX(), noX = noButton.getX();
		float center = container.getWidth() / 2f;
		if (yesX < center)
			yesButton.setX(Math.min(yesX + (delta / 5f), center));
		if (noX > center)
			noButton.setX(Math.max(noX - (delta / 5f), center));
	}

	@Override
	public void mousePressed(int button, int x, int y) {
		// check mouse button 
		if (button != Input.MOUSE_LEFT_BUTTON)
			return;

		if (yesButton.contains(x, y)) {
			options.saveOptions();
			Opsu.closeSocket();
			container.exit();
		} else if (noButton.contains(x, y))
			game.enterState(Opsu.STATE_MAINMENU, new EmptyTransition(), new FadeInTransition(Color.black));
	}

	@Override
	public void keyPressed(int key, char c) {
		switch (key) {
		case Input.KEY_1:
			options.saveOptions();
			Opsu.closeSocket();
			container.exit();
			break;
		case Input.KEY_2:
		case Input.KEY_ESCAPE:
			game.enterState(Opsu.STATE_MAINMENU, new EmptyTransition(), new FadeInTransition(Color.black));
			break;
		case Input.KEY_F12:
			takeScreenShot();
			break;
		}
	}

	@Override
	public void enter(GameContainer container, StateBasedGame game)
			throws SlickException {
		float center = container.getWidth() / 2f;
		yesButton.setX(center - centerOffset);
		noButton.setX(center + centerOffset);
	}
}
