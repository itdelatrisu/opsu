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
import itdelatrisu.opsu.Opsu;
import itdelatrisu.opsu.OsuGroupList;
import itdelatrisu.opsu.OsuParser;
import itdelatrisu.opsu.OszUnpacker;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.audio.SoundController;

import java.io.File;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.BasicGameState;
import org.newdawn.slick.state.StateBasedGame;

/**
 * "Splash Screen" state.
 * <p>
 * Loads game resources and enters "Main Menu" state.
 */
public class Splash extends BasicGameState {
	/**
	 * Whether or not loading has completed.
	 */
	private boolean finished = false;

	// game-related variables
	private int state;
	private GameContainer container;
	private boolean init = false;

	public Splash(int state) {
		this.state = state;
	}

	@Override
	public void init(GameContainer container, StateBasedGame game)
			throws SlickException {
		this.container = container;

		// load Utils class first (needed in other 'init' methods)
		Utils.init(container, game);

		GameImage.MENU_LOGO.getImage().setAlpha(0f);
	}

	@Override
	public void render(GameContainer container, StateBasedGame game, Graphics g)
			throws SlickException {
		g.setBackground(Color.black);
		GameImage.MENU_LOGO.getImage().drawCentered(container.getWidth() / 2, container.getHeight() / 2);

		// display progress
		String unpackedFile = OszUnpacker.getCurrentFileName();
		String parsedFile = OsuParser.getCurrentFileName();
		String soundFile = SoundController.getCurrentFileName();
		if (unpackedFile != null) {
			drawLoadProgress(
					g, OszUnpacker.getUnpackerProgress(),
					"Unpacking new beatmaps...", unpackedFile
			);
		} else if (parsedFile != null) {
			drawLoadProgress(
					g, OsuParser.getParserProgress(),
					"Loading beatmaps...", parsedFile
			);
		} else if (soundFile != null) {
			drawLoadProgress(
					g, SoundController.getLoadingProgress(),
					"Loading sounds...", soundFile
			);
		}
	}

	@Override
	public void update(GameContainer container, StateBasedGame game, int delta)
			throws SlickException {
		if (!init) {
			init = true;

			// load other resources in a new thread
			final int width = container.getWidth();
			final int height = container.getHeight();
			new Thread() {
				@Override
				public void run() {
					File beatmapDir = Options.getBeatmapDir();

					// unpack all OSZ archives
					OszUnpacker.unpackAllFiles(Options.getOSZDir(), beatmapDir);

					// parse song directory
					OsuParser.parseAllFiles(beatmapDir, width, height);

					// load sounds
					SoundController.init();

					finished = true;
				}
			}.start();
		}

		// fade in logo
		Image logo = GameImage.MENU_LOGO.getImage();
		float alpha = logo.getAlpha();
		if (alpha < 1f)
			logo.setAlpha(alpha + (delta / 500f));

		// change states when loading complete
		if (finished && alpha >= 1f) {
			// initialize song list
			OsuGroupList.get().init();
			((SongMenu) game.getState(Opsu.STATE_SONGMENU)).setFocus(OsuGroupList.get().getRandomNode(), -1, true);

			// play the theme song
			if (Options.isThemSongEnabled())
				MusicController.playThemeSong();

			game.enterState(Opsu.STATE_MAINMENU);
		}
	}

	@Override
	public int getID() { return state; }

	@Override
	public void keyPressed(int key, char c) {
		if (key == Input.KEY_ESCAPE) {
			Options.saveOptions();
			Opsu.closeSocket();
			container.exit();
		}
	}

	/**
	 * Draws loading progress.
	 * @param g the graphics context
	 * @param progress the completion percentage
	 * @param text the progress text
	 * @param file the file being loaded
	 */
	private void drawLoadProgress(Graphics g, int progress, String text, String file) {
		g.setColor(Color.white);
		g.setFont(Utils.FONT_MEDIUM);
		int lineY = container.getHeight() - 25;
		int lineOffsetY = Utils.FONT_MEDIUM.getLineHeight();

		if (Options.isLoadVerbose()) {
			g.drawString(String.format("%s (%d%%)", text, progress), 25, lineY - (lineOffsetY * 2));
			g.drawString(file, 25, lineY - lineOffsetY);
		} else {
			g.drawString(text, 25, lineY - (lineOffsetY * 2));
			g.fillRect(25, lineY - (lineOffsetY / 2f), (container.getWidth() - 50) * progress / 100f, lineOffsetY / 4f);
		}
	}
}
