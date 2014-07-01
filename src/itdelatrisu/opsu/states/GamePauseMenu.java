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
import itdelatrisu.opsu.MusicController;
import itdelatrisu.opsu.Opsu;
import itdelatrisu.opsu.SoundController;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.BasicGameState;
import org.newdawn.slick.state.StateBasedGame;
import org.newdawn.slick.state.transition.FadeInTransition;
import org.newdawn.slick.state.transition.FadeOutTransition;

/**
 * "Game Paused" state.
 * <ul>
 * <li>[Continue] - unpause game (return to game state)
 * <li>[Retry]    - restart game (return to game state)
 * <li>[Back]     - return to song menu state
 * </ul>
 */
public class GamePauseMenu extends BasicGameState {
	/**
	 * Music fade-out time, in milliseconds.
	 */
	private static final int FADEOUT_TIME = 1000;

	/**
	 * Track position when the pause menu was loaded (for FADEOUT_TIME).
	 */
	private long pauseStartTime;

	/**
	 * "Continue", "Retry", and "Back" buttons.
	 */
	private GUIMenuButton continueButton, retryButton, backButton;

	/**
	 * Background image for pause menu (optional).
	 */
	private Image backgroundImage;

	/**
	 * Background image for fail menu (optional).
	 */
	private Image failImage;

	// game-related variables
	private StateBasedGame game;
	private int state;

	public GamePauseMenu(int state) {
		this.state = state;
	}

	@Override
	public void init(GameContainer container, StateBasedGame game)
			throws SlickException {
		this.game = game;

		int width = container.getWidth();
		int height = container.getHeight();

		// initialize buttons
		continueButton = new GUIMenuButton(new Image("pause-continue.png"), width / 2f, height * 0.25f);
		retryButton = new GUIMenuButton(new Image("pause-retry.png"), width / 2f, height * 0.5f);
		backButton = new GUIMenuButton(new Image("pause-back.png"), width / 2f, height * 0.75f);

		// pause background image
		try {
			backgroundImage = new Image("pause-overlay.png").getScaledCopy(width, height);
			backgroundImage.setAlpha(0.7f);
		} catch (Exception e) {
			// optional
		}

		// fail image
		try {
			failImage = new Image("fail-background.png").getScaledCopy(width, height);
			failImage.setAlpha(0.7f);
		} catch (Exception e) {
			// optional
		}
	}

	@Override
	public void render(GameContainer container, StateBasedGame game, Graphics g)
			throws SlickException {
		// background
		if (backgroundImage != null && Game.getRestart() != Game.RESTART_LOSE)
			backgroundImage.draw();
		else if (failImage != null && Game.getRestart() == Game.RESTART_LOSE)
			failImage.draw();
		else
			g.setBackground(Color.black);

		Options.drawFPS();

		// draw buttons
		if (Game.getRestart() != Game.RESTART_LOSE)
			continueButton.draw();
		retryButton.draw();
		backButton.draw();
	}

	@Override
	public void update(GameContainer container, StateBasedGame game, int delta)
			throws SlickException {
		// empty
	}

	@Override
	public int getID() { return state; }

	@Override
	public void keyPressed(int key, char c) {
		switch (key) {
		case Input.KEY_ESCAPE:
			// 'esc' will normally unpause, but will return to song menu if health is zero
			if (Game.getRestart() == Game.RESTART_LOSE) {
				MusicController.stop();
				MusicController.playAt(Game.getOsuFile().previewTime, true);
				SoundController.playSound(SoundController.SOUND_MENUBACK);
				game.enterState(Opsu.STATE_SONGMENU, new FadeOutTransition(Color.black), new FadeInTransition(Color.black));
			} else
				unPause(Game.RESTART_FALSE);
			break;
		case Input.KEY_F12:
			Options.takeScreenShot();
			break;
		}
	}

	@Override
	public void mousePressed(int button, int x, int y) {
		// check mouse button 
		if (button != Input.MOUSE_LEFT_BUTTON)
			return;

		boolean loseState = (Game.getRestart() == Game.RESTART_LOSE);

		// if music faded out (i.e. health is zero), don't process any actions before FADEOUT_TIME
		if (loseState && System.currentTimeMillis() - pauseStartTime < FADEOUT_TIME)
			return;

		if (continueButton.contains(x, y) && !loseState)
			unPause(Game.RESTART_FALSE);
		else if (retryButton.contains(x, y)) {
			unPause(Game.RESTART_MANUAL);
		} else if (backButton.contains(x, y)) {
			MusicController.pause();  // lose state
			MusicController.playAt(Game.getOsuFile().previewTime, true);
			SoundController.playSound(SoundController.SOUND_MENUBACK);
			game.enterState(Opsu.STATE_SONGMENU, new FadeOutTransition(Color.black), new FadeInTransition(Color.black));
		}
	}

	@Override
	public void enter(GameContainer container, StateBasedGame game)
			throws SlickException {
		pauseStartTime = System.currentTimeMillis();
		if (Game.getRestart() == Game.RESTART_LOSE) {
			MusicController.fadeOut(FADEOUT_TIME);
			SoundController.playSound(SoundController.SOUND_FAIL);
		} else
			MusicController.pause();
	}

	/**
	 * Unpause and return to the Game state.
	 */
	private void unPause(byte restart) {
		if (restart == Game.RESTART_MANUAL)
			SoundController.playSound(SoundController.SOUND_MENUHIT);
		else
			SoundController.playSound(SoundController.SOUND_MENUBACK);
		Game.setRestart(restart);
		game.enterState(Opsu.STATE_GAME);
	}
}
