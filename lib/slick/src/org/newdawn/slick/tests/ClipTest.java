package org.newdawn.slick.tests;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;

/**
 * A test to demonstrate world clipping as opposed to screen clipping
 *
 * @author kevin
 */
public class ClipTest extends BasicGame {

	/** The current angle of rotation */
	private float ang = 0;
	/** True if we're showing world clipping */
	private boolean world;
	/** True if we're showing screen clipping */
	private boolean clip;
	
	/**
	 * Create a new tester for the clip plane based clipping
	 */
	public ClipTest() {
		super("Clip Test");
	}
	
	/**
	 * @see org.newdawn.slick.BasicGame#init(org.newdawn.slick.GameContainer)
	 */
	public void init(GameContainer container) throws SlickException {
	}

	/**
	 * @see org.newdawn.slick.BasicGame#update(org.newdawn.slick.GameContainer, int)
	 */
	public void update(GameContainer container, int delta)
			throws SlickException {
		ang += delta * 0.01f;
	}

	/**
	 * @see org.newdawn.slick.Game#render(org.newdawn.slick.GameContainer, org.newdawn.slick.Graphics)
	 */
	public void render(GameContainer container, Graphics g)
			throws SlickException {
		g.setColor(Color.white);
		g.drawString("1 - No Clipping", 100, 10);
		g.drawString("2 - Screen Clipping", 100, 30);
		g.drawString("3 - World Clipping", 100, 50);
		
		if (world) {
			g.drawString("WORLD CLIPPING ENABLED", 200, 80);
		} 
		if (clip) {
			g.drawString("SCREEN CLIPPING ENABLED", 200, 80);
		}
		
		g.rotate(400, 400, ang);
		if (world) {
			g.setWorldClip(350,302,100,196);
		}
		if (clip) {
			g.setClip(350,302,100,196);
		}
		g.setColor(Color.red);
		g.fillOval(300,300,200,200);
		g.setColor(Color.blue);
		g.fillRect(390,200,20,400);
		
		g.clearClip();
		g.clearWorldClip();
	}

	/**
	 * @see org.newdawn.slick.BasicGame#keyPressed(int, char)
	 */
	public void keyPressed(int key, char c) {
		if (key == Input.KEY_1) {
			world = false;
			clip = false;
		}
		if (key == Input.KEY_2) {
			world = false;
			clip = true;
		}
		if (key == Input.KEY_3) {
			world = true;
			clip = false;
		}
	}
	
	/**
	 * Entry point to our test
	 * 
	 * @param argv The arguments to pass into the test
	 */
	public static void main(String[] argv) {
		try {
			AppGameContainer container = new AppGameContainer(new ClipTest());
			container.setDisplayMode(800,600,false);
			container.start();
		} catch (SlickException e) {
			e.printStackTrace();
		}
	}
}
