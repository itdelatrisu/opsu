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

package itdelatrisu.opsu.states;

import itdelatrisu.opsu.GameData;
import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.MenuButton;
import itdelatrisu.opsu.Opsu;
import itdelatrisu.opsu.Options;
import itdelatrisu.opsu.UI;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.audio.SoundEffect;
import itdelatrisu.opsu.beatmap.Beatmap;
import itdelatrisu.opsu.replay.Replay;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.lwjgl.opengl.Display;
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
import org.newdawn.slick.util.Log;

/**
 * "Game Ranking" (score card) state.
 * <ul>
 * <li>[Retry]    - restart game (return to game state)
 * <li>[Replay]   - watch replay (return to game state)
 * <li>[Back]     - return to song menu state
 * </ul>
 */
public class GameRanking extends BasicGameState {
	/** Associated GameData object. */
	private GameData data;

	/** "Retry" and "Replay" buttons. */
	private MenuButton retryButton, replayButton;

	/** Button coordinates. */
	private float retryY, replayY;

	// game-related variables
	private GameContainer container;
	private StateBasedGame game;
	private int state;
	private Input input;

	public GameRanking(int state) {
		this.state = state;
	}

	@Override
	public void init(GameContainer container, StateBasedGame game)
			throws SlickException {
		this.container = container;
		this.game = game;
		this.input = container.getInput();

		int width = container.getWidth();
		int height = container.getHeight();

		// buttons
		Image retry = GameImage.PAUSE_RETRY.getImage();
		Image replay = GameImage.PAUSE_REPLAY.getImage();
		replayY = (height * 0.985f) - replay.getHeight() / 2f;
		retryY = replayY - (replay.getHeight() / 2f) - (retry.getHeight() / 1.975f);
		retryButton = new MenuButton(retry, width - (retry.getWidth() / 2f), retryY);
		replayButton = new MenuButton(replay, width - (replay.getWidth() / 2f), replayY);
		retryButton.setHoverFade();
		replayButton.setHoverFade();
	}

	@Override
	public void render(GameContainer container, StateBasedGame game, Graphics g)
			throws SlickException {
		int width = container.getWidth();
		int height = container.getHeight();

		Beatmap beatmap = MusicController.getBeatmap();

		// background
		if (!beatmap.drawBG(width, height, 0.7f, true))
			GameImage.PLAYFIELD.getImage().draw(0,0);

		// ranking screen elements
		data.drawRankingElements(g, beatmap);

		// buttons
		replayButton.draw();
		if (data.isGameplay())
			retryButton.draw();
		UI.getBackButton().draw();

		UI.draw(g);
	}

	@Override
	public void update(GameContainer container, StateBasedGame game, int delta)
			throws SlickException {
		UI.update(delta);
		int mouseX = input.getMouseX(), mouseY = input.getMouseY();
		replayButton.hoverUpdate(delta, mouseX, mouseY);
		if (data.isGameplay())
			retryButton.hoverUpdate(delta, mouseX, mouseY);
		else
			MusicController.loopTrackIfEnded(true);
		UI.getBackButton().hoverUpdate(delta, mouseX, mouseY);
	}

	@Override
	public int getID() { return state; }

	@Override
	public void mouseWheelMoved(int newValue) {
		if (input.isKeyDown(Input.KEY_LALT) || input.isKeyDown(Input.KEY_RALT))
			UI.changeVolume((newValue < 0) ? -1 : 1);
	}

	@Override
	public void keyPressed(int key, char c) {
		switch (key) {
		case Input.KEY_ESCAPE:
			returnToSongMenu();
			break;
		case Input.KEY_F7:
			Options.setNextFPS(container);
			break;
		case Input.KEY_F10:
			Options.toggleMouseDisabled();
			break;
		case Input.KEY_F12:
			Utils.takeScreenShot();
			break;
		}
	}

	@Override
	public void mousePressed(int button, int x, int y) {
		// check mouse button
		if (button == Input.MOUSE_MIDDLE_BUTTON)
			return;

		// back to menu
		if (UI.getBackButton().contains(x, y)) {
			returnToSongMenu();
			return;
		}

		// replay
		Game gameState = (Game) game.getState(Opsu.STATE_GAME);
		boolean returnToGame = false;
		if (replayButton.contains(x, y)) {
			Replay r = data.getReplay(null, null);
			if (r != null) {
				try {
					r.load();
					gameState.setReplay(r);
					gameState.setRestart((data.isGameplay()) ? Game.Restart.REPLAY : Game.Restart.NEW);
					returnToGame = true;
				} catch (FileNotFoundException e) {
					UI.sendBarNotification("Replay file not found.");
				} catch (IOException e) {
					Log.error("Failed to load replay data.", e);
					UI.sendBarNotification("Failed to load replay data. See log for details.");
				}
			} else
				UI.sendBarNotification("Replay file not found.");
		}

		// retry
		else if (data.isGameplay() && retryButton.contains(x, y)) {
			gameState.setReplay(null);
			gameState.setRestart(Game.Restart.MANUAL);
			returnToGame = true;
		}

		if (returnToGame) {
			Beatmap beatmap = MusicController.getBeatmap();
			gameState.loadBeatmap(beatmap);
			SoundController.playSound(SoundEffect.MENUHIT);
			game.enterState(Opsu.STATE_GAME, new FadeOutTransition(Color.black), new FadeInTransition(Color.black));
			return;
		}
	}

	@Override
	public void enter(GameContainer container, StateBasedGame game)
			throws SlickException {
		UI.enter();
		Display.setTitle(game.getTitle());
		if (!data.isGameplay()) {
			if (!MusicController.isTrackDimmed())
				MusicController.toggleTrackDimmed(0.5f);
			replayButton.setY(retryY);
		} else {
			SoundController.playSound(SoundEffect.APPLAUSE);
			retryButton.resetHover();
			replayButton.setY(replayY);
		}
		replayButton.resetHover();
	}

	@Override
	public void leave(GameContainer container, StateBasedGame game)
			throws SlickException {
		this.data = null;
		if (MusicController.isTrackDimmed())
			MusicController.toggleTrackDimmed(1f);
	}

	/**
	 * Returns to the song menu.
	 */
	private void returnToSongMenu() {
		SoundController.playSound(SoundEffect.MENUBACK);
		if (data.isGameplay()) {
			SongMenu songMenu = (SongMenu) game.getState(Opsu.STATE_SONGMENU);
			songMenu.resetGameDataOnLoad();
			songMenu.resetTrackOnLoad();
		}
		UI.resetCursor();
		game.enterState(Opsu.STATE_SONGMENU, new FadeOutTransition(Color.black), new FadeInTransition(Color.black));
	}

	/**
	 * Sets the associated GameData object.
	 * @param data the GameData
	 */
	public void setGameData(GameData data) { this.data = data; }

	/**
	 * Returns the current GameData object (usually null unless state active).
	 */
	public GameData getGameData() { return data; }
}
