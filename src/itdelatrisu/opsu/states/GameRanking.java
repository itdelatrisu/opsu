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

import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.GameMod;
import itdelatrisu.opsu.GameScore;
import itdelatrisu.opsu.MenuButton;
import itdelatrisu.opsu.Opsu;
import itdelatrisu.opsu.OsuFile;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.audio.SoundEffect;

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

/**
 * "Game Ranking" (score card) state.
 * <ul>
 * <li>[Retry]    - restart game (return to game state)
 * <li>[Exit]     - return to main menu state
 * <li>[Back]     - return to song menu state
 * </ul>
 */
public class GameRanking extends BasicGameState {
	/**
	 * Associated GameScore object.
	 */
	private GameScore score;

	/**
	 * "Retry" and "Exit" buttons.
	 */
	private MenuButton retryButton, exitButton;

	// game-related variables
	private StateBasedGame game;
	private int state;
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
		Image retry = GameImage.RANKING_RETRY.getImage();
		Image exit  = GameImage.RANKING_EXIT.getImage();
		retryButton = new MenuButton(retry,
				width - (retry.getWidth() / 2f),
				(height * 0.97f) - (exit.getHeight() * 1.5f)
		);
		exitButton  = new MenuButton(exit,
				width - (exit.getWidth() / 2f),
				(height * 0.97f) - (exit.getHeight() / 2f)
		);
	}

	@Override
	public void render(GameContainer container, StateBasedGame game, Graphics g)
			throws SlickException {
		int width = container.getWidth();
		int height = container.getHeight();

		OsuFile osu = MusicController.getOsuFile();

		// background
		if (!osu.drawBG(width, height, 0.7f))
			g.setBackground(Utils.COLOR_BLACK_ALPHA);

		// ranking screen elements
		score.drawRankingElements(g, width, height);

		// game mods
		for (GameMod mod : GameMod.valuesReversed()) {
			if (mod.isActive()) {
				Image modImage = mod.getImage();
				modImage.draw(
						(width * 0.75f) + ((mod.getID() - (GameMod.size() / 2)) * modImage.getWidth() / 3f),
						height / 2f
				);
			}
		}

		// header text
		Utils.FONT_LARGE.drawString(10, 0,
				String.format("%s - %s [%s]", osu.getArtist(), osu.getTitle(), osu.version), Color.white);
		Utils.FONT_MEDIUM.drawString(10, Utils.FONT_LARGE.getLineHeight() - 6,
				String.format("Beatmap by %s", osu.creator), Color.white);

		// buttons
		retryButton.draw();
		exitButton.draw();
		Utils.getBackButton().draw();

		Utils.drawFPS();
		Utils.drawCursor();
	}

	@Override
	public void update(GameContainer container, StateBasedGame game, int delta)
			throws SlickException {
		Utils.updateCursor(delta);
		int mouseX = input.getMouseX(), mouseY = input.getMouseY();
		Utils.getBackButton().hoverUpdate(delta, mouseX, mouseY);
	}

	@Override
	public int getID() { return state; }

	@Override
	public void keyPressed(int key, char c) {
		switch (key) {
		case Input.KEY_ESCAPE:
			MusicController.pause();
			MusicController.playAt(MusicController.getOsuFile().previewTime, true);
			SoundController.playSound(SoundEffect.MENUBACK);
			((SongMenu) game.getState(Opsu.STATE_SONGMENU)).resetGameDataOnLoad();
			game.enterState(Opsu.STATE_SONGMENU, new FadeOutTransition(Color.black), new FadeInTransition(Color.black));
			break;
		case Input.KEY_F12:
			Utils.takeScreenShot();
			break;
		}
	}

	@Override
	public void mousePressed(int button, int x, int y) {
		// check mouse button 
		if (button != Input.MOUSE_LEFT_BUTTON)
			return;

		if (retryButton.contains(x, y)) {
			OsuFile osu = MusicController.getOsuFile();
			Display.setTitle(String.format("%s - %s", game.getTitle(), osu.toString()));
			((Game) game.getState(Opsu.STATE_GAME)).setRestart(Game.Restart.MANUAL);
			SoundController.playSound(SoundEffect.MENUHIT);
			game.enterState(Opsu.STATE_GAME, new FadeOutTransition(Color.black), new FadeInTransition(Color.black));
		} else if (exitButton.contains(x, y)) {
			SoundController.playSound(SoundEffect.MENUBACK);
			((MainMenu) game.getState(Opsu.STATE_MAINMENU)).reset();
			((SongMenu) game.getState(Opsu.STATE_SONGMENU)).resetGameDataOnLoad();
			game.enterState(Opsu.STATE_MAINMENU, new FadeOutTransition(Color.black), new FadeInTransition(Color.black));
		} else if (Utils.getBackButton().contains(x, y)) {
			MusicController.pause();
			MusicController.playAt(MusicController.getOsuFile().previewTime, true);
			SoundController.playSound(SoundEffect.MENUBACK);
			((SongMenu) game.getState(Opsu.STATE_SONGMENU)).resetGameDataOnLoad();
			game.enterState(Opsu.STATE_SONGMENU, new FadeOutTransition(Color.black), new FadeInTransition(Color.black));
		}
	}

	@Override
	public void enter(GameContainer container, StateBasedGame game)
			throws SlickException {
		Display.setTitle(game.getTitle());
		Utils.getBackButton().setScale(1f);
		SoundController.playSound(SoundEffect.APPLAUSE);
	}

	/**
	 * Sets the associated GameScore object.
	 * @param score the GameScore
	 */
	public void setGameScore(GameScore score) {
		this.score = score;
	}
}
