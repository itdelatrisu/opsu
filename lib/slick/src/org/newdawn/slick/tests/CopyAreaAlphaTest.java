package org.newdawn.slick.tests;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;

/**
 * A test to demonstrate world clipping as opposed to screen clipping
 *
 * @author kevin
 */
public class CopyAreaAlphaTest extends BasicGame {
	/** The texture to apply over the top */
	private Image textureMap;
	/** The copied image */
	private Image copy;
	
	/**
	 * Create a new tester for the clip plane based clipping
	 */
	public CopyAreaAlphaTest() {
		super("CopyArea Alpha Test");
	}
	
	/**
	 * @see org.newdawn.slick.BasicGame#init(org.newdawn.slick.GameContainer)
	 */
	public void init(GameContainer container) throws SlickException {
		textureMap = new Image("testdata/grass.png");
		container.getGraphics().setBackground(Color.black);
		copy = new Image(100,100);
	}

	/**
	 * @see org.newdawn.slick.BasicGame#update(org.newdawn.slick.GameContainer, int)
	 */
	public void update(GameContainer container, int delta)
			throws SlickException {
	}

	/**
	 * @see org.newdawn.slick.Game#render(org.newdawn.slick.GameContainer, org.newdawn.slick.Graphics)
	 */
	public void render(GameContainer container, Graphics g)
			throws SlickException {
		g.clearAlphaMap();
		g.setDrawMode(Graphics.MODE_NORMAL);
		g.setColor(Color.white);
		g.fillOval(100,100,150,150);
		textureMap.draw(10,50);
		
		g.copyArea(copy, 100,100);
		g.setColor(Color.red);
		g.fillRect(300,100,200,200);
		copy.draw(350,150);
	}

	/**
	 * @see org.newdawn.slick.BasicGame#keyPressed(int, char)
	 */
	public void keyPressed(int key, char c) {
	}
	
	/**
	 * Entry point to our test
	 * 
	 * @param argv The arguments to pass into the test
	 */
	public static void main(String[] argv) {
		try {
			AppGameContainer container = new AppGameContainer(new CopyAreaAlphaTest());
			container.setDisplayMode(800,600,false);
			container.start();
		} catch (SlickException e) {
			e.printStackTrace();
		}
	}
}
