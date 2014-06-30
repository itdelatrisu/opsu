package org.newdawn.slick.tests;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;

/**
 * The double click testing
 * 
 * @author kevin
 */
public class DoubleClickTest extends BasicGame {

	/**
	 * Create the test game
	 */
	public DoubleClickTest() {
		super("Double Click Test");
	}

	/** The test message to display */
	private String message = "Click or Double Click";
	
	/**
	 * @see org.newdawn.slick.BasicGame#init(org.newdawn.slick.GameContainer)
	 */
	public void init(GameContainer container) throws SlickException {
	}

	/**
	 * @see org.newdawn.slick.BasicGame#update(org.newdawn.slick.GameContainer, int)
	 */
	public void update(GameContainer container, int delta) throws SlickException {
	}

	/**
	 * @see org.newdawn.slick.Game#render(org.newdawn.slick.GameContainer, org.newdawn.slick.Graphics)
	 */
	public void render(GameContainer container, Graphics g) throws SlickException {
		g.drawString(message, 100, 100);
	}
	
	/**
	 * Entry point to our test
	 * 
	 * @param argv The arguments to pass into the test, not used here
	 */
	public static void main(String[] argv) {
		try {
			AppGameContainer container = new AppGameContainer(new DoubleClickTest());
			container.setDisplayMode(800,600,false);
			container.start();
		} catch (SlickException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @see org.newdawn.slick.BasicGame#mouseClicked(int, int, int, int)
	 */
	public void mouseClicked(int button, int x, int y, int clickCount) {
		if (clickCount == 1) {
			message = "Single Click: "+button+" "+x+","+y;
		}
		if (clickCount == 2) {
			message = "Double Click: "+button+" "+x+","+y;
		}
	}
}
