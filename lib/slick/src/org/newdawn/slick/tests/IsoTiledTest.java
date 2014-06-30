package org.newdawn.slick.tests;

import org.newdawn.slick.BasicGame;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.tiled.TiledMap;
import org.newdawn.slick.util.Bootstrap;

/**
 * Simple test for isometric map rendering
 * 
 * @author kevin
 */
public class IsoTiledTest extends BasicGame {
	/** The tilemap we're going to render */
	private TiledMap tilemap;
	
	/**
	 * Create a new test
	 */
	public IsoTiledTest() {
		super("Isometric Tiled Map Test");
	}

	/*
	 * (non-Javadoc)
	 * @see org.newdawn.slick.BasicGame#init(org.newdawn.slick.GameContainer)
	 */
	public void init(GameContainer container) throws SlickException {
		tilemap = new TiledMap("testdata/isoexample.tmx", "testdata/");
	}

	/*
	 * (non-Javadoc)
	 * @see org.newdawn.slick.BasicGame#update(org.newdawn.slick.GameContainer, int)
	 */
	public void update(GameContainer container, int delta)
			throws SlickException {
	}

	/*
	 * (non-Javadoc)
	 * @see org.newdawn.slick.Game#render(org.newdawn.slick.GameContainer, org.newdawn.slick.Graphics)
	 */
	public void render(GameContainer container, Graphics g)
			throws SlickException {
		tilemap.render(350,150);
	}

	/**
	 * Entry point to our test
	 * 
	 * @param argv The arguments passed in from the command line
	 */
	public static void main(String[] argv) {
		Bootstrap.runAsApplication(new IsoTiledTest(), 800,600,false);
	}
}
