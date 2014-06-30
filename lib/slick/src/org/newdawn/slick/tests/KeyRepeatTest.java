package org.newdawn.slick.tests;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;

/**
 * A test for basic image rendering
 *
 * @author kevin
 */
public class KeyRepeatTest extends BasicGame {
	/** The number of times the key pressed event has been fired */
	private int count;
	/** The input sub system */
	private Input input;
	
	/**
	 * Create a new image rendering test
	 */
	public KeyRepeatTest() {
		super("KeyRepeatTest");
	}
	
	/**
	 * @see org.newdawn.slick.BasicGame#init(org.newdawn.slick.GameContainer)
	 */
	public void init(GameContainer container) throws SlickException {
		input = container.getInput();
		input.enableKeyRepeat(300,100);
	}

	/**
	 * @see org.newdawn.slick.BasicGame#render(org.newdawn.slick.GameContainer, org.newdawn.slick.Graphics)
	 */
	public void render(GameContainer container, Graphics g) {
		g.drawString("Key Press Count: "+count, 100,100);
		g.drawString("Press Space to Toggle Key Repeat", 100,150);
		g.drawString("Key Repeat Enabled: "+input.isKeyRepeatEnabled(), 100,200);
	}

	/**
	 * @see org.newdawn.slick.BasicGame#update(org.newdawn.slick.GameContainer, int)
	 */
	public void update(GameContainer container, int delta) {
	}

	/**
	 * Entry point to our test
	 * 
	 * @param argv The arguments to pass into the test
	 */
	public static void main(String[] argv) {
		try {
			AppGameContainer container = new AppGameContainer(new KeyRepeatTest());
			container.setDisplayMode(800,600,false);
			container.start();
		} catch (SlickException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @see org.newdawn.slick.BasicGame#keyPressed(int, char)
	 */
	public void keyPressed(int key, char c) {
		count++;
		if (key == Input.KEY_SPACE) {
			if (input.isKeyRepeatEnabled()) {
				input.disableKeyRepeat();
			} else {
				input.enableKeyRepeat(300,100);
			}
		}
	}
}
