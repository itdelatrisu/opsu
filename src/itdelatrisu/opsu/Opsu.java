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

package itdelatrisu.opsu;

import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.db.DBController;
import itdelatrisu.opsu.downloads.DownloadList;
import itdelatrisu.opsu.states.ButtonMenu;
import itdelatrisu.opsu.states.DownloadsMenu;
import itdelatrisu.opsu.states.Game;
import itdelatrisu.opsu.states.GamePauseMenu;
import itdelatrisu.opsu.states.GameRanking;
import itdelatrisu.opsu.states.MainMenu;
import itdelatrisu.opsu.states.OptionsMenu;
import itdelatrisu.opsu.states.SongMenu;
import itdelatrisu.opsu.states.Splash;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.StateBasedGame;
import org.newdawn.slick.state.transition.FadeInTransition;
import org.newdawn.slick.state.transition.FadeOutTransition;
import org.newdawn.slick.util.ClasspathLocation;
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
	/** Game states. */
	public static final int
		STATE_SPLASH        = 0,
		STATE_MAINMENU      = 1,
		STATE_BUTTONMENU    = 2,
		STATE_SONGMENU      = 3,
		STATE_GAME          = 4,
		STATE_GAMEPAUSEMENU = 5,
		STATE_GAMERANKING   = 6,
		STATE_OPTIONSMENU   = 7,
		STATE_DOWNLOADSMENU = 8;

	/** Server socket for restricting the program to a single instance. */
	private static ServerSocket SERVER_SOCKET;

	/**
	 * Constructor.
	 * @param name the program name
	 */
	public Opsu(String name) {
		super(name);
	}

	@Override
	public void initStatesList(GameContainer container) throws SlickException {
		addState(new Splash(STATE_SPLASH));
		addState(new MainMenu(STATE_MAINMENU));
		addState(new ButtonMenu(STATE_BUTTONMENU));
		addState(new SongMenu(STATE_SONGMENU));
		addState(new Game(STATE_GAME));
		addState(new GamePauseMenu(STATE_GAMEPAUSEMENU));
		addState(new GameRanking(STATE_GAMERANKING));
		addState(new OptionsMenu(STATE_OPTIONSMENU));
		addState(new DownloadsMenu(STATE_DOWNLOADSMENU));
	}

	/**
	 * Launches opsu!.
	 */
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
				ErrorHandler.error("** Uncaught Exception! **", e, true);
			}
		});

		// parse configuration file
		Options.parseOptions();

		// only allow a single instance
		try {
			SERVER_SOCKET = new ServerSocket(Options.getPort());
		} catch (IOException e) {
			ErrorHandler.error(String.format("Another program is already running on port %d.", Options.getPort()), e, false);
			System.exit(1);
		}

		// set path for lwjgl natives - NOT NEEDED if using JarSplice
		File nativeDir = new File("./target/natives/");
		if (nativeDir.isDirectory())
			System.setProperty("org.lwjgl.librarypath", nativeDir.getAbsolutePath());

		// set the resource paths
		ResourceLoader.removeAllResourceLocations();
		ResourceLoader.addResourceLocation(new FileSystemLocation(Options.getSkinDir()));
		ResourceLoader.addResourceLocation(new ClasspathLocation());
		ResourceLoader.addResourceLocation(new FileSystemLocation(new File(".")));
		ResourceLoader.addResourceLocation(new FileSystemLocation(new File("./res/")));

		// initialize databases
		DBController.init();

		// start the game
		try {
			// loop until force exit
			while (true) {
				Opsu opsu = new Opsu("opsu!");
				Container app = new Container(opsu);

				// basic game settings
				Options.setDisplayMode(app);
				String[] icons = { "icon16.png", "icon32.png" };
				app.setIcons(icons);
				app.setForceExit(true);

				app.start();
			}
		} catch (SlickException e) {
			// JARs will not run properly inside directories containing '!'
			// http://bugs.java.com/view_bug.do?bug_id=4523159
			if (new File("").getAbsolutePath().indexOf('!') != -1)
				ErrorHandler.error("Cannot run JAR from path containing '!'.", null, false);
			else
				ErrorHandler.error("Error while creating game container.", e, true);
		}

		Opsu.exit();
	}

	@Override
	public boolean closeRequested() {
		int id = this.getCurrentStateID();

		// intercept close requests in game-related states and return to song menu
		if (id == STATE_GAME || id == STATE_GAMEPAUSEMENU || id == STATE_GAMERANKING) {
			// start playing track at preview position
			SongMenu songMenu = (SongMenu) this.getState(Opsu.STATE_SONGMENU);
			if (id == STATE_GAMERANKING) {
				GameData data = ((GameRanking) this.getState(Opsu.STATE_GAMERANKING)).getGameData();
				if (data != null && data.isGameplay()) {
					songMenu.resetGameDataOnLoad();
					songMenu.resetTrackOnLoad();
				}
			} else {
				songMenu.resetGameDataOnLoad();
				if (id == STATE_GAME) {
					MusicController.pause();
					MusicController.resume();
				} else
					songMenu.resetTrackOnLoad();
			}
			UI.resetCursor();
			this.enterState(Opsu.STATE_SONGMENU, new FadeOutTransition(Color.black), new FadeInTransition(Color.black));
			return false;
		}

		// show confirmation dialog if any downloads are active
		if (DownloadList.get().hasActiveDownloads() && DownloadList.showExitConfirmation())
			return false;

		return true;
	}

	/**
	 * Closes all resources and exits the application.
	 */
	public static void exit() {
		// close databases
		DBController.closeConnections();

		// cancel all downloads
		DownloadList.get().cancelAllDownloads();

		// close server socket
		if (SERVER_SOCKET != null) {
			try {
				SERVER_SOCKET.close();
			} catch (IOException e) {
				ErrorHandler.error("Failed to close server socket.", e, false);
			}
		}

		System.exit(0);
	}
}
