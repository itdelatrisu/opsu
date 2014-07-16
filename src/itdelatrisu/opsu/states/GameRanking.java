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
import itdelatrisu.opsu.GameMod;
import itdelatrisu.opsu.GameScore;
import itdelatrisu.opsu.MusicController;
import itdelatrisu.opsu.Opsu;
import itdelatrisu.opsu.OsuFile;
import itdelatrisu.opsu.SoundController;
import itdelatrisu.opsu.Utils;

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
	private GUIMenuButton retryButton, exitButton;

	// game-related variables
	private StateBasedGame game;
	private int state;

	public GameRanking(int state) {
		this.state = state;
	}

	@Override
	public void init(GameContainer container, StateBasedGame game)
			throws SlickException {
		this.game = game;

		score = Game.getGameScore();

		int width = container.getWidth();
		int height = container.getHeight();

		// buttons
		Image retry = new Image("ranking-retry.png");
		Image exit  = new Image("ranking-back.png");
		float scale = (height * 0.15f) / retry.getHeight();
		retry = retry.getScaledCopy(scale);
		exit  = exit.getScaledCopy(scale);
		retryButton = new GUIMenuButton(retry,
				width - (retry.getWidth() / 2f),
				(height * 0.97f) - (exit.getHeight() * 1.5f)
		);
		exitButton  = new GUIMenuButton(exit,
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
		g.setColor(Color.white);
		Utils.FONT_LARGE.drawString(10, 0,
				String.format("%s - %s [%s]", osu.artist, osu.title, osu.version));
		Utils.FONT_MEDIUM.drawString(10, Utils.FONT_LARGE.getLineHeight() - 6,
				String.format("Beatmap by %s", osu.creator));

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
	}

	@Override
	public int getID() { return state; }

	@Override
	public void keyPressed(int key, char c) {
		switch (key) {
		case Input.KEY_ESCAPE:
			MusicController.pause();
			MusicController.playAt(MusicController.getOsuFile().previewTime, true);
			SoundController.playSound(SoundController.SOUND_MENUBACK);
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
			Game.setRestart(Game.RESTART_MANUAL);
			SoundController.playSound(SoundController.SOUND_MENUHIT);
			game.enterState(Opsu.STATE_GAME, new FadeOutTransition(Color.black), new FadeInTransition(Color.black));
		} else if (exitButton.contains(x, y)) {
			SoundController.playSound(SoundController.SOUND_MENUBACK);
			game.enterState(Opsu.STATE_MAINMENU, new FadeOutTransition(Color.black), new FadeInTransition(Color.black));
		} else if (Utils.getBackButton().contains(x, y)) {
			MusicController.pause();
			MusicController.playAt(MusicController.getOsuFile().previewTime, true);
			SoundController.playSound(SoundController.SOUND_MENUBACK);
			game.enterState(Opsu.STATE_SONGMENU, new FadeOutTransition(Color.black), new FadeInTransition(Color.black));
		}
	}

	@Override
	public void enter(GameContainer container, StateBasedGame game)
			throws SlickException {
		Display.setTitle(game.getTitle());
		SoundController.playSound(SoundController.SOUND_APPLAUSE);
	}
}
