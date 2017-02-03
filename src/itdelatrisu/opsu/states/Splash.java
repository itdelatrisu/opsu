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
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.beatmap.BeatmapParser;
import itdelatrisu.opsu.beatmap.BeatmapSetList;
import itdelatrisu.opsu.beatmap.BeatmapWatchService;
import itdelatrisu.opsu.beatmap.OszUnpacker;
import itdelatrisu.opsu.options.Options;
import itdelatrisu.opsu.replay.ReplayImporter;
import itdelatrisu.opsu.skins.SkinUnpacker;
import itdelatrisu.opsu.ui.UI;
import itdelatrisu.opsu.ui.animations.AnimatedValue;
import itdelatrisu.opsu.ui.animations.AnimationEquation;

import java.io.File;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
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
	/** Minimum time, in milliseconds, to display the splash screen (and fade in the logo). */
	private static final int MIN_SPLASH_TIME = 350;

	/** Minimum elapsed time, in milliseconds, before displaying progress information (if non-verbose). */
	private static final int PROGRESS_START_TIME = 1000;

	/** Whether or not loading has completed. */
	private boolean finished = false;

	/** Loading thread. */
	private Thread thread;

	/** Number of times the 'Esc' key has been pressed. */
	private int escapeCount = 0;

	/** Whether the skin being loaded is a new skin (for program restarts). */
	private boolean newSkin = false;

	/** Whether the watch service is newly enabled (for program restarts). */
	private boolean watchServiceChange = false;

	/** Logo alpha level. */
	private AnimatedValue logoAlpha = new AnimatedValue(MIN_SPLASH_TIME, 0f, 1f, AnimationEquation.LINEAR);

	/** Loading progress alpha level. */
	private AnimatedValue progressAlpha = new AnimatedValue(MIN_SPLASH_TIME, 0f, 1f, AnimationEquation.OUT_CUBIC);

	/** Elapsed time. */
	private int elapsedTime = 0;

	// game-related variables
	private final int state;
	private GameContainer container;
	private boolean init = false;

	public Splash(int state) {
		this.state = state;
	}

	@Override
	public void init(GameContainer container, StateBasedGame game)
			throws SlickException {
		this.container = container;

		// check if skin changed
		if (Options.getSkin() != null)
			this.newSkin = (Options.getSkin().getDirectory() != Options.getSkinDir());

		// check if watch service newly enabled
		this.watchServiceChange = Options.isWatchServiceEnabled() && BeatmapWatchService.get() == null;

		// load Utils class first (needed in other 'init' methods)
		Utils.init(container, game);

		// fade in logo
		GameImage.MENU_LOGO.getImage().setAlpha(0f);
	}

	@Override
	public void render(GameContainer container, StateBasedGame game, Graphics g)
			throws SlickException {
		g.setBackground(Color.black);
		GameImage.MENU_LOGO.getImage().drawCentered(container.getWidth() / 2, container.getHeight() / 2);
		UI.drawLoadingProgress(g, Options.isLoadVerbose() ? 1f : progressAlpha.getValue());
	}

	@Override
	public void update(GameContainer container, StateBasedGame game, int delta)
			throws SlickException {
		if (!init) {
			init = true;

			// resources already loaded (from application restart)
			if (BeatmapSetList.get() != null) {
				if (newSkin || watchServiceChange) {  // need to reload resources
					thread = new Thread() {
						@Override
						public void run() {
							// reload beatmaps if watch service newly enabled
							if (watchServiceChange)
								BeatmapParser.parseAllFiles(Options.getBeatmapDir());

							// reload sounds if skin changed
							// TODO: only reload each sound if actually needed?
							if (newSkin)
								SoundController.init();

							Utils.gc(true);

							finished = true;
							thread = null;
						}
					};
					thread.start();
				} else  // don't reload anything
					finished = true;
			}

			// load all resources in a new thread
			else {
				thread = new Thread() {
					@Override
					public void run() {
						File beatmapDir = Options.getBeatmapDir();
						File importDir = Options.getImportDir();

						// unpack all OSZ archives
						OszUnpacker.unpackAllFiles(importDir, beatmapDir);

						// parse song directory
						BeatmapParser.parseAllFiles(beatmapDir);

						// import skins
						SkinUnpacker.unpackAllFiles(importDir, Options.getSkinRootDir());

						// import replays
						ReplayImporter.importAllReplaysFromDir(importDir);

						// load sounds
						SoundController.init();

						Utils.gc(true);

						finished = true;
						thread = null;
					}
				};
				thread.start();
			}
		}

		// fade in logo
		if (logoAlpha.update(delta))
			GameImage.MENU_LOGO.getImage().setAlpha(logoAlpha.getValue());

		// fade in loading progress
		elapsedTime += delta;
		if (elapsedTime >= PROGRESS_START_TIME)
			progressAlpha.update(delta);

		// change states when loading complete
		if (finished && logoAlpha.isFinished()) {
			// initialize song list
			if (BeatmapSetList.get().size() > 0) {
				BeatmapSetList.get().init();
				if (Options.isThemeSongEnabled())
					MusicController.playThemeSong();
				else
					((SongMenu) game.getState(Opsu.STATE_SONGMENU)).setFocus(BeatmapSetList.get().getRandomNode(), -1, true, true);
			}

			// play the theme song
			else
				MusicController.playThemeSong();

			game.enterState(Opsu.STATE_MAINMENU);
		}
	}

	@Override
	public int getID() { return state; }

	@Override
	public void keyPressed(int key, char c) {
		if (key == Input.KEY_ESCAPE) {
			// close program
			if (++escapeCount >= 3)
				container.exit();

			// stop parsing beatmaps by sending interrupt to BeatmapParser
			else if (thread != null)
				thread.interrupt();
		}
	}
}
