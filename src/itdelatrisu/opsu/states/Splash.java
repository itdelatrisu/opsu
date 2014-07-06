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

import itdelatrisu.opsu.Opsu;
import itdelatrisu.opsu.OsuGroupList;
import itdelatrisu.opsu.OsuParser;
import itdelatrisu.opsu.SoundController;
import itdelatrisu.opsu.Utils;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.BasicGameState;
import org.newdawn.slick.state.StateBasedGame;

public class Splash extends BasicGameState {
	/**
	 * Logo image.
	 */
	private Image logo;

	/**
	 * Whether or not loading has completed.
	 */
	private boolean finished = false;

	// game-related variables
	private int state;

	public Splash(int state) {
		this.state = state;
	}

	@Override
	public void init(GameContainer container, StateBasedGame game)
			throws SlickException {
		final int width = container.getWidth();
		final int height = container.getHeight();

		logo = new Image("logo.png");
		logo = logo.getScaledCopy((height / 1.2f) / logo.getHeight());
		logo.setAlpha(0f);

		// load Utils class first (needed in other 'init' methods)
		Utils.init(container, game);

		// load other resources in a new thread
		final SongMenu menu = (SongMenu) game.getState(Opsu.STATE_SONGMENU);
		new Thread() {
			@Override
			public void run() {
				// parse song directory
				OsuParser.parseAllFiles(Options.getBeatmapDir(), width, height);

				// initialize song list
				Opsu.groups.init(OsuGroupList.SORT_TITLE);
				menu.setFocus(Opsu.groups.getRandomNode(), -1, true);

				// load sounds
				SoundController.init();

				finished = true;
			}
		}.start();
	}

	@Override
	public void render(GameContainer container, StateBasedGame game, Graphics g)
			throws SlickException {
		g.setBackground(Color.black);

		int width = container.getWidth();
		int height = container.getHeight();

		logo.drawCentered(width / 2, height / 2);

		// progress tracking
		String currentFile = OsuParser.getCurrentFileName();
		if (currentFile != null && Options.isLoadVerbose()) {
			g.setColor(Color.white);
			g.setFont(Utils.FONT_MEDIUM);
			int lineHeight = Utils.FONT_MEDIUM.getLineHeight();
			g.drawString(
					String.format("Loading... (%s%%)", OsuParser.getParserProgress()),
					25, height - 25 - (lineHeight * 2)
			);
			g.drawString(currentFile, 25, height - 25 - lineHeight);
		}
	}

	@Override
	public void update(GameContainer container, StateBasedGame game, int delta)
			throws SlickException {
		// fade in logo
		float alpha = logo.getAlpha();
		if (alpha < 1f)
			logo.setAlpha(alpha + (delta / 400f));

		// change states when loading complete
		if (finished && alpha >= 1f)
			game.enterState(Opsu.STATE_MAINMENU);
	}

	@Override
	public int getID() { return state; }
}
