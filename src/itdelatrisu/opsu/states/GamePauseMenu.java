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

package itdelatrisu.opsu.states;

import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.Opsu;
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.audio.SoundEffect;
import itdelatrisu.opsu.options.Options;
import itdelatrisu.opsu.ui.MenuButton;
import itdelatrisu.opsu.ui.UI;
import itdelatrisu.opsu.ui.animations.AnimationEquation;

import org.lwjgl.input.Keyboard;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.BasicGameState;
import org.newdawn.slick.state.StateBasedGame;
import org.newdawn.slick.state.transition.EasedFadeOutTransition;
import org.newdawn.slick.state.transition.FadeInTransition;

/**
 * "Game Pause/Fail" state.
 * <p>
 * Players are able to continue the game (if applicable), retry the beatmap,
 * or return to the song menu from this state.
 */
public class GamePauseMenu extends BasicGameState {
	/** "Continue", "Retry", and "Back" buttons. */
	private MenuButton continueButton, retryButton, backButton;

	// game-related variables
	private GameContainer container;
	private StateBasedGame game;
	private Input input;
	private final int state;
	private Game gameState;

	public GamePauseMenu(int state) {
		this.state = state;
	}

	@Override
	public void init(GameContainer container, StateBasedGame game)
			throws SlickException {
		this.container = container;
		this.game = game;
		this.input = container.getInput();
		this.gameState = (Game) game.getState(Opsu.STATE_GAME);
	}

	@Override
	public void render(GameContainer container, StateBasedGame game, Graphics g)
			throws SlickException {
		// get background image
		GameImage bg = (gameState.getPlayState() == Game.PlayState.LOSE) ?
				GameImage.FAIL_BACKGROUND : GameImage.PAUSE_OVERLAY;

		// don't draw default background if button skinned and background unskinned
		boolean buttonsSkinned =
			GameImage.PAUSE_CONTINUE.hasBeatmapSkinImage() ||
			GameImage.PAUSE_RETRY.hasBeatmapSkinImage() ||
			GameImage.PAUSE_BACK.hasBeatmapSkinImage();
		if (!buttonsSkinned || bg.hasBeatmapSkinImage())
			bg.getImage().drawCentered(container.getWidth() / 2, container.getHeight() / 2);
		else
			g.setBackground(Color.black);

		// draw buttons
		if (gameState.getPlayState() != Game.PlayState.LOSE)
			continueButton.draw();
		retryButton.draw();
		backButton.draw();

		UI.draw(g);
	}

	@Override
	public void update(GameContainer container, StateBasedGame game, int delta)
			throws SlickException {
		UI.update(delta);
		int mouseX = input.getMouseX(), mouseY = input.getMouseY();
		continueButton.hoverUpdate(delta, mouseX, mouseY);
		retryButton.hoverUpdate(delta, mouseX, mouseY);
		backButton.hoverUpdate(delta, mouseX, mouseY);
	}

	@Override
	public int getID() { return state; }

	@Override
	public void keyPressed(int key, char c) {
		if (UI.globalKeyPressed(key))
			return;

		// game keys
		if (!Keyboard.isRepeatEvent()) {
			if (key == Options.getGameKeyLeft())
				mousePressed(Input.MOUSE_LEFT_BUTTON, input.getMouseX(), input.getMouseY());
			else if (key == Options.getGameKeyRight())
				mousePressed(Input.MOUSE_RIGHT_BUTTON, input.getMouseX(), input.getMouseY());
		}

		switch (key) {
		case Input.KEY_ESCAPE:
			// 'esc' will normally unpause, but will return to song menu if health is zero
			if (gameState.getPlayState() == Game.PlayState.LOSE) {
				SoundController.playSound(SoundEffect.MENUBACK);
				((SongMenu) game.getState(Opsu.STATE_SONGMENU)).resetGameDataOnLoad();
				MusicController.playAt(MusicController.getBeatmap().previewTime, true);
				if (UI.getCursor().isBeatmapSkinned())
					UI.getCursor().reset();
				game.enterState(Opsu.STATE_SONGMENU, new EasedFadeOutTransition(), new FadeInTransition());
			} else {
				if (!Options.isGameplaySoundDisabled())
					SoundController.playSound(SoundEffect.MENUBACK);
				gameState.setPlayState(Game.PlayState.NORMAL);
				game.enterState(Opsu.STATE_GAME);
			}
			break;
		case Input.KEY_R:
			// restart
			if (input.isKeyDown(Input.KEY_RCONTROL) || input.isKeyDown(Input.KEY_LCONTROL)) {
				gameState.setPlayState(Game.PlayState.RETRY);
				game.enterState(Opsu.STATE_GAME);
			}
			break;
		case Input.KEY_EQUALS:
		case Input.KEY_ADD:
		case Input.KEY_MINUS:
		case Input.KEY_SUBTRACT:
			UI.getNotificationManager().sendBarNotification("Offset can only be changed while game is not paused.");
			break;
		}
	}

	@Override
	public void mousePressed(int button, int x, int y) {
		if (button == Input.MOUSE_MIDDLE_BUTTON)
			return;

		boolean loseState = (gameState.getPlayState() == Game.PlayState.LOSE);
		if (continueButton.contains(x, y) && !loseState) {
			if (!Options.isGameplaySoundDisabled())
				SoundController.playSound(SoundEffect.MENUBACK);
			gameState.setPlayState(Game.PlayState.NORMAL);
			game.enterState(Opsu.STATE_GAME);
		} else if (retryButton.contains(x, y)) {
			SoundController.playSound(SoundEffect.MENUHIT);
			gameState.setPlayState(Game.PlayState.RETRY);
			game.enterState(Opsu.STATE_GAME);
		} else if (backButton.contains(x, y)) {
			SoundController.playSound(SoundEffect.MENUBACK);
			((SongMenu) game.getState(Opsu.STATE_SONGMENU)).resetGameDataOnLoad();
			if (loseState)
				MusicController.playAt(MusicController.getBeatmap().previewTime, true);
			else
				MusicController.resume();
			if (UI.getCursor().isBeatmapSkinned())
				UI.getCursor().reset();
			MusicController.setPitch(1.0f);
			game.enterState(Opsu.STATE_SONGMENU, new EasedFadeOutTransition(), new FadeInTransition());
		}
	}

	@Override
	public void mouseWheelMoved(int newValue) {
		if (Options.isMouseWheelDisabled())
			return;

		UI.globalMouseWheelMoved(newValue, false);
	}

	@Override
	public void enter(GameContainer container, StateBasedGame game)
			throws SlickException {
		UI.enter();
		MusicController.pause();
		continueButton.resetHover();
		retryButton.resetHover();
		backButton.resetHover();
	}

	@Override
	public void leave(GameContainer container, StateBasedGame game) throws SlickException {
		SoundController.stopSound(SoundEffect.FAIL);
	}

	/**
	 * Loads all game pause/fail menu images.
	 */
	public void loadImages() {
		int width = container.getWidth();
		int height = container.getHeight();

		// initialize buttons
		continueButton = new MenuButton(GameImage.PAUSE_CONTINUE.getImage(), width / 2f, height * 0.25f);
		retryButton = new MenuButton(GameImage.PAUSE_RETRY.getImage(), width / 2f, height * 0.5f);
		backButton = new MenuButton(GameImage.PAUSE_BACK.getImage(), width / 2f, height * 0.75f);
		final int buttonAnimationDuration = 300;
		continueButton.setHoverAnimationDuration(buttonAnimationDuration);
		retryButton.setHoverAnimationDuration(buttonAnimationDuration);
		backButton.setHoverAnimationDuration(buttonAnimationDuration);
		final AnimationEquation buttonAnimationEquation = AnimationEquation.IN_OUT_BACK;
		continueButton.setHoverAnimationEquation(buttonAnimationEquation);
		retryButton.setHoverAnimationEquation(buttonAnimationEquation);
		backButton.setHoverAnimationEquation(buttonAnimationEquation);
		continueButton.setHoverExpand();
		retryButton.setHoverExpand();
		backButton.setHoverExpand();
	}
}
