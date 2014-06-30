package org.newdawn.slick.util;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.Game;

/**
 * Utility class to wrap up starting a game in a single line
 * 
 * @author kevin
 */
public class Bootstrap {

	/**
	 * Start the game as an application
	 * 
	 * @param game The game to be started
	 * @param width The width of the window
	 * @param height The height of the window
	 * @param fullscreen True if the window should be fullscreen
	 */
	public static void runAsApplication(Game game, int width, int height, boolean fullscreen) {
		try {
			AppGameContainer container = new AppGameContainer(game, width, height, fullscreen);
			container.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
