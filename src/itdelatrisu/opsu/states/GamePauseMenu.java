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
import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.MusicController;
import itdelatrisu.opsu.Opsu;
import itdelatrisu.opsu.Resources;
import itdelatrisu.opsu.SoundController;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.states.Options.OpsuOptions;

import org.lwjgl.input.Keyboard;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.StateBasedGame;
import org.newdawn.slick.state.transition.FadeInTransition;
import org.newdawn.slick.state.transition.FadeOutTransition;

import static itdelatrisu.opsu.SoundController.BasicSounds.*;

/**
 * "Game Pause/Fail" state.
 * <ul>
 * <li>[Continue] - unpause game (return to game state)
 * <li>[Retry]    - restart game (return to game state)
 * <li>[Back]     - return to song menu state
 * </ul>
 */
public class GamePauseMenu extends Utils {
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

	private Game gameState;
	public GamePauseMenu(int state, OpsuOptions options, SoundController soundController, Resources resources) {
		super(state, options, soundController, resources);
	}

	@Override
	public void init(GameContainer container, StateBasedGame game)
			throws SlickException {
		super.init(container, game);

		gameState = (Game) game.getState(Opsu.STATE_GAME);
	}

	@Override
	public void render(GameContainer container, StateBasedGame game, Graphics g)
			throws SlickException {
		// background
		if (gameState.getRestart() != Game.RESTART_LOSE)
			GameImage.PAUSE_OVERLAY.getImage().draw();
		else
			GameImage.FAIL_BACKGROUND.getImage().draw();

		// draw buttons
		if (gameState.getRestart() != Game.RESTART_LOSE)
			continueButton.draw();
		retryButton.draw();
		backButton.draw();

		drawFPS();
		drawCursor();
	}

	@Override
	public void update(GameContainer container, StateBasedGame game, int delta)
			throws SlickException {
		Utils.updateCursor(delta);
	}

	@Override
	public void keyPressed(int key, char c) {
		// game keys
		if (!Keyboard.isRepeatEvent()) {
			if (key == options.getGameKeyLeft())
				mousePressed(Input.MOUSE_LEFT_BUTTON, input.getMouseX(), input.getMouseY());
			else if (key == options.getGameKeyRight())
				mousePressed(Input.MOUSE_RIGHT_BUTTON, input.getMouseX(), input.getMouseY());
		}

		switch (key) {
		case Input.KEY_ESCAPE:
			// 'esc' will normally unpause, but will return to song menu if health is zero
			if (gameState.getRestart() == Game.RESTART_LOSE) {
				MusicController.stop();
				MusicController.playAt(MusicController.getOsuFile().previewTime, true);
				soundController.playSound(SOUND_MENUBACK);
				game.enterState(Opsu.STATE_SONGMENU, new FadeOutTransition(Color.black), new FadeInTransition(Color.black));
			} else
				unPause(Game.RESTART_FALSE);
			break;
		case Input.KEY_F12:
			takeScreenShot();
			break;
		}
	}

	@Override
	public void mousePressed(int button, int x, int y) {
		if (button == Input.MOUSE_MIDDLE_BUTTON)
			return;

		boolean loseState = (gameState.getRestart() == Game.RESTART_LOSE);

		// if music faded out (i.e. health is zero), don't process any actions before FADEOUT_TIME
		if (loseState && System.currentTimeMillis() - pauseStartTime < FADEOUT_TIME)
			return;

		if (continueButton.contains(x, y) && !loseState)
			unPause(Game.RESTART_FALSE);
		else if (retryButton.contains(x, y)) {
			unPause(Game.RESTART_MANUAL);
		} else if (backButton.contains(x, y)) {
			MusicController.pause();  // lose state
			MusicController.playAt(MusicController.getOsuFile().previewTime, true);
			soundController.playSound(SOUND_MENUBACK);
			game.enterState(Opsu.STATE_SONGMENU, new FadeOutTransition(Color.black), new FadeInTransition(Color.black));
		}
	}

	@Override
	public void enter(GameContainer container, StateBasedGame game)
			throws SlickException {
		pauseStartTime = System.currentTimeMillis();
		if (gameState.getRestart() == Game.RESTART_LOSE) {
			MusicController.fadeOut(FADEOUT_TIME);
			soundController.playSound(SOUND_FAIL);
		} else
			MusicController.pause();
	}

	/**
	 * Unpause and return to the Game state.
	 */
	private void unPause(byte restart) {
		if (restart == Game.RESTART_MANUAL)
			soundController.playSound(SOUND_MENUHIT);
		else
			soundController.playSound(SOUND_MENUBACK);
		gameState.setRestart(restart);
		game.enterState(Opsu.STATE_GAME);
	}

	/**
	 * Loads all game pause/fail menu images.
	 */
	public void loadImages() {
		int width = container.getWidth();
		int height = container.getHeight();

		// initialize buttons
		continueButton = new GUIMenuButton(GameImage.PAUSE_CONTINUE.getImage(), width / 2f, height * 0.25f);
		retryButton = new GUIMenuButton(GameImage.PAUSE_RETRY.getImage(), width / 2f, height * 0.5f);
		backButton = new GUIMenuButton(GameImage.PAUSE_BACK.getImage(), width / 2f, height * 0.75f);

		// pause background image
		if (!GameImage.PAUSE_OVERLAY.isScaled()) {
			GameImage.PAUSE_OVERLAY.setImage(GameImage.PAUSE_OVERLAY.getImage().getScaledCopy(width, height));
			GameImage.PAUSE_OVERLAY.getImage().setAlpha(0.7f);
			GameImage.PAUSE_OVERLAY.setScaled();
		}

		// fail image
		if (!GameImage.FAIL_BACKGROUND.isScaled()) {
			GameImage.FAIL_BACKGROUND.setImage(GameImage.FAIL_BACKGROUND.getImage().getScaledCopy(width, height));
			GameImage.FAIL_BACKGROUND.getImage().setAlpha(0.7f);
			GameImage.FAIL_BACKGROUND.setScaled();
		}
	}
}
