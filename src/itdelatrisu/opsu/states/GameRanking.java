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

import itdelatrisu.opsu.GameData;
import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.GameMod;
import itdelatrisu.opsu.Opsu;
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.audio.SoundEffect;
import itdelatrisu.opsu.beatmap.Beatmap;
import itdelatrisu.opsu.options.Options;
import itdelatrisu.opsu.replay.Replay;
import itdelatrisu.opsu.ui.MenuButton;
import itdelatrisu.opsu.ui.UI;
import itdelatrisu.opsu.ui.animations.AnimatedValue;
import itdelatrisu.opsu.ui.animations.AnimationEquation;

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
import org.newdawn.slick.state.transition.EasedFadeOutTransition;
import org.newdawn.slick.state.transition.FadeInTransition;
import org.newdawn.slick.util.Log;

/**
 * "Game Ranking" (score card) state.
 * <p>
 * Players are able to view their score statistics, retry the beatmap (if applicable),
 * or watch a replay of the game from this state.
 * </ul>
 */
public class GameRanking extends BasicGameState {
	/** Associated GameData object. */
	private GameData data;

	/** "Retry" and "Replay" buttons. */
	private MenuButton retryButton, replayButton;

	/** Button coordinates. */
	private float retryY, replayY;

	/** Animation progress. */
	private AnimatedValue animationProgress = new AnimatedValue(6000, 0f, 1f, AnimationEquation.LINEAR);

	/** The loaded replay, or null if it couldn't be loaded. */
	private Replay replay = null;

	// game-related variables
	private StateBasedGame game;
	private final int state;
	private Input input;

	public GameRanking(int state) {
		this.state = state;
	}

	@Override
	public void init(GameContainer container, StateBasedGame game)
			throws SlickException {
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
		int mouseX = input.getMouseX(), mouseY = input.getMouseY();

		Beatmap beatmap = MusicController.getBeatmap();

		// background
		float parallaxX = 0, parallaxY = 0;
		if (Options.isParallaxEnabled()) {
			int offset = (int) (height * (GameImage.PARALLAX_SCALE - 1f));
			parallaxX = -offset / 2f * (mouseX - width / 2) / (width / 2);
			parallaxY = -offset / 2f * (mouseY - height / 2) / (height / 2);
		}
		if (!beatmap.drawBackground(width, height, parallaxX, parallaxY, 0.5f, true)) {
			Image bg = GameImage.MENU_BG.getImage();
			if (Options.isParallaxEnabled()) {
				bg = bg.getScaledCopy(GameImage.PARALLAX_SCALE);
				bg.setAlpha(0.5f);
				bg.drawCentered(width / 2 + parallaxX, height / 2 + parallaxY);
			} else {
				bg.setAlpha(0.5f);
				bg.drawCentered(width / 2, height / 2);
				bg.setAlpha(1f);
			}
		}

		// ranking screen elements
		data.drawRankingElements(g, beatmap, animationProgress.getTime());

		// buttons
		replayButton.draw();
		if (data.isGameplay() && !GameMod.AUTO.isActive())
			retryButton.draw();
		UI.getBackButton().draw(g);

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
		animationProgress.update(delta);
		data.updateRankingDisplays(delta, mouseX, mouseY);
	}

	@Override
	public int getID() { return state; }

	@Override
	public void mouseWheelMoved(int newValue) {
		UI.globalMouseWheelMoved(newValue, true);
	}

	@Override
	public void keyPressed(int key, char c) {
		if (UI.globalKeyPressed(key))
			return;

		switch (key) {
		case Input.KEY_ESCAPE:
			returnToSongMenu();
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
		boolean replayButtonPressed = replayButton.contains(x, y);
		if (replayButtonPressed && !(data.isGameplay() && GameMod.AUTO.isActive())) {
			if (replay != null) {
				gameState.setReplay(replay);
				gameState.setPlayState((data.isGameplay()) ? Game.PlayState.REPLAY : Game.PlayState.FIRST_LOAD);
				returnToGame = true;
			} else
				UI.getNotificationManager().sendBarNotification("Replay file not found.");
		}

		// retry
		else if (data.isGameplay() &&
		         (!GameMod.AUTO.isActive() && retryButton.contains(x, y)) ||
		         (GameMod.AUTO.isActive() && replayButtonPressed)) {
			gameState.setReplay(null);
			gameState.setPlayState(Game.PlayState.RETRY);
			returnToGame = true;
		}

		if (returnToGame) {
			Beatmap beatmap = MusicController.getBeatmap();
			gameState.loadBeatmap(beatmap);
			SoundController.playSound(SoundEffect.MENUHIT);
			game.enterState(Opsu.STATE_GAME, new EasedFadeOutTransition(), new FadeInTransition());
			return;
		}

		// otherwise, finish the animation
		animationProgress.setTime(animationProgress.getDuration());
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
			animationProgress.setTime(animationProgress.getDuration());
		} else {
			SoundController.playSound(SoundEffect.APPLAUSE);
			retryButton.resetHover();
			if (GameMod.AUTO.isActive()) {
				replayButton.setY(retryY);
				animationProgress.setTime(animationProgress.getDuration());
			} else {
				replayButton.setY(replayY);
				animationProgress.setTime(0);
			}
		}
		replayButton.resetHover();
		loadReplay();
	}

	@Override
	public void leave(GameContainer container, StateBasedGame game)
			throws SlickException {
		this.data = null;
		if (MusicController.isTrackDimmed())
			MusicController.toggleTrackDimmed(1f);

		SoundController.stopSound(SoundEffect.APPLAUSE);
	}

	/**
	 * Returns to the song menu.
	 */
	private void returnToSongMenu() {
		SoundController.playSound(SoundEffect.MENUBACK);
		SongMenu songMenu = (SongMenu) game.getState(Opsu.STATE_SONGMENU);
		if (data.isGameplay())
			songMenu.resetTrackOnLoad();
		songMenu.resetGameDataOnLoad();
		if (UI.getCursor().isBeatmapSkinned())
			UI.getCursor().reset();
		game.enterState(Opsu.STATE_SONGMENU, new EasedFadeOutTransition(), new FadeInTransition());
	}

	/** Loads the replay data. */
	private void loadReplay() {
		this.replay = null;
		Replay r = data.getReplay(null, null, null);
		if (r != null) {
			try {
				r.load();
				this.replay = r;
			} catch (FileNotFoundException e) {
				// file not found
			} catch (IOException e) {
				Log.error("Failed to load replay data.", e);
				UI.getNotificationManager().sendNotification("Failed to load replay data.\nSee log for details.", Color.red);
			}
		}
		// else file not found
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
