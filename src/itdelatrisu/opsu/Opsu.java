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

package itdelatrisu.opsu;

import itdelatrisu.opsu.states.Game;
import itdelatrisu.opsu.states.GamePauseMenu;
import itdelatrisu.opsu.states.GameRanking;
import itdelatrisu.opsu.states.MainMenu;
import itdelatrisu.opsu.states.MainMenuExit;
import itdelatrisu.opsu.states.Options;
import itdelatrisu.opsu.states.SongMenu;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.StateBasedGame;
import org.newdawn.slick.state.transition.FadeInTransition;
import org.newdawn.slick.state.transition.FadeOutTransition;
import org.newdawn.slick.util.DefaultLogSystem;
import org.newdawn.slick.util.FileSystemLocation;
import org.newdawn.slick.util.Log;
import org.newdawn.slick.util.ResourceLoader;

/**
 * Main class.
 * <p>
 * Creates game container, adds all other states, and initializes song data.
 */
public class Opsu extends StateBasedGame {
	/**
	 * Song group structure (each group contains of an ArrayList of OsuFiles).
	 */
	public static OsuGroupList groups = new OsuGroupList();

	/**
	 * Game states.
	 */
	public static final int
		STATE_OPTIONS       = 1,
		STATE_MAINMENU      = 2,
		STATE_MAINMENUEXIT  = 3,
		STATE_SONGMENU      = 4,
		STATE_GAME          = 5,
		STATE_GAMEPAUSEMENU = 6,
		STATE_GAMERANKING   = 7;

	/**
	 * Used to restrict the program to a single instance.
	 */
	private static ServerSocket SERVER_SOCKET;

	public Opsu(String name) {
		super(name);
	}

	@Override
	public void initStatesList(GameContainer container) throws SlickException {
		addState(new Options(STATE_OPTIONS));
		addState(new MainMenu(STATE_MAINMENU));
		addState(new MainMenuExit(STATE_MAINMENUEXIT));
		addState(new SongMenu(STATE_SONGMENU));
		addState(new Game(STATE_GAME));
		addState(new GamePauseMenu(STATE_GAMEPAUSEMENU));
		addState(new GameRanking(STATE_GAMERANKING));
	}

	public static void main(String[] args) {
		// log all errors to a file
		Log.setVerbose(false);
		try {
			DefaultLogSystem.out = new PrintStream(new FileOutputStream(Options.LOG_FILE, true));
		} catch (FileNotFoundException e) {
			Log.error(e);
		}
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				Log.error("** Uncaught Exception! **", e);
			}
		});

		// parse configuration file
		Options.parseOptions();

		// only allow a single instance
		try {
			SERVER_SOCKET = new ServerSocket(Options.getPort());
		} catch (IOException e) {
			Log.error(String.format("Another program is already running on port %d.", Options.getPort()), e);
			System.exit(1);
		}

		// set path for lwjgl natives - NOT NEEDED if using JarSplice
//		System.setProperty("org.lwjgl.librarypath", new File("native").getAbsolutePath());

		// set the resource path
		ResourceLoader.addResourceLocation(new FileSystemLocation(new File("./res/")));

		// start the game
		Opsu osuGame = new Opsu("opsu!");
		try {
			AppGameContainer app = new AppGameContainer(osuGame);

			// basic game settings
			int[] containerSize = Options.getContainerSize();
			app.setDisplayMode(containerSize[0], containerSize[1], false);
			String[] icons = { "icon16.png", "icon32.png" };
			app.setIcons(icons);

			// parse song directory
			OsuParser.parseAllFiles(Options.getBeatmapDir(),
					app.getWidth(), app.getHeight()
			);

			// clear the cache
			if (!Options.TMP_DIR.mkdir()) {
				for (File tmp : Options.TMP_DIR.listFiles())
					tmp.delete();
			}
			Options.TMP_DIR.deleteOnExit();

			// load sounds
			SoundController.init();

			app.start();
		} catch (SlickException e) {
			Log.error("Error while creating game container.", e);
		}
	}

	@Override
	public boolean closeRequested() {
		int id = this.getCurrentStateID();

		// intercept close requests in game-related states and return to song menu
		if (id == STATE_GAME || id == STATE_GAMEPAUSEMENU || id == STATE_GAMERANKING) {
			// start playing track at preview position
			MusicController.pause();
			MusicController.playAt(Game.getOsuFile().previewTime, true);
			this.enterState(Opsu.STATE_SONGMENU, new FadeOutTransition(Color.black), new FadeInTransition(Color.black));
			return false;
		}

		Options.saveOptions();
		((AppGameContainer) this.getContainer()).destroy();
		closeSocket();
		return true;
	}

	/**
	 * Closes the server socket.
	 */
	public static void closeSocket() {
		try {
			SERVER_SOCKET.close();
		} catch (IOException e) {
			Log.error("Failed to close server socket.", e);
		}
	}
}
