package org.newdawn.slick.tests;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.tiled.TiledMap;

/**
 * A test of the tile map system based around the TilED (http://www.mapeditor.org) tool
 *
 * @author kevin
 */
public class TileMapTest extends BasicGame {
	/** The tile map we're going to load and render */
	private TiledMap map;
	
	/** the name of the map, read from map properties, specified by TilED */
	private String mapName;
	
	/** how hard are the monsters, read from layer properties, specified by TilED */
	private String monsterDifficulty;
	
	/** we try to read a property from the map which doesn't exist so we expect the default value */
	private String nonExistingMapProperty;
	
	/** we try to read a property from the layer which doesn't exist so we expect the default value */
	private String nonExistingLayerProperty;
	
	/** how long did we wait already until next update */
	private int updateCounter = 0;
	
	/** changing some tile of the map every UPDATE_TIME milliseconds */
	private static int UPDATE_TIME = 1000;
	
	/** we want to store the originalTileID before we set a new one */
	private int originalTileID = 0;
	
	/**
	 * Create our tile map test
	 */
	public TileMapTest() {
		super("Tile Map Test");
	}
	
	/**
	 * @see org.newdawn.slick.BasicGame#init(org.newdawn.slick.GameContainer)
	 */
	public void init(GameContainer container) throws SlickException {
		map = new TiledMap("testdata/testmap.tmx","testdata");
		// read some properties from map and layer
		mapName = map.getMapProperty("name", "Unknown map name");
		monsterDifficulty = map.getLayerProperty(0, "monsters", "easy peasy");
		nonExistingMapProperty = map.getMapProperty("zaphod", "Undefined map property");
		nonExistingLayerProperty = map.getLayerProperty(1, "beeblebrox", "Undefined layer property");
		
		// store the original tileid of layer 0 at 10, 10
		originalTileID = map.getTileId(10, 10, 0);
	}

	/**
	 * @see org.newdawn.slick.BasicGame#render(org.newdawn.slick.GameContainer, org.newdawn.slick.Graphics)
	 */
	public void render(GameContainer container, Graphics g) {
		map.render(10, 10, 4,4,15,15);
		
		g.scale(0.35f,0.35f);
		map.render(1400, 0);
		g.resetTransform();
		
		g.drawString("map name: " + mapName, 10, 500);
		g.drawString("monster difficulty: " + monsterDifficulty, 10, 550);
		
		g.drawString("non existing map property: " + nonExistingMapProperty, 10, 525);
		g.drawString("non existing layer property: " + nonExistingLayerProperty, 10, 575);
	}

	/**
	 * @see org.newdawn.slick.BasicGame#update(org.newdawn.slick.GameContainer, int)
	 */
	public void update(GameContainer container, int delta) {
		updateCounter += delta;
		if (updateCounter > UPDATE_TIME) {
			// swap the tile every second
			updateCounter -= UPDATE_TIME;
			int currentTileID = map.getTileId(10, 10, 0);
			if (currentTileID != originalTileID)
				map.setTileId(10, 10, 0, originalTileID);
			else
				map.setTileId(10, 10, 0, 1);
		}
	}

	/**
	 * @see org.newdawn.slick.BasicGame#keyPressed(int, char)
	 */
	public void keyPressed(int key, char c) {
		if (key == Input.KEY_ESCAPE) {
			System.exit(0);
		}
	}
	
	/**
	 * Entry point to our test
	 * 
	 * @param argv The arguments passed to the test
	 */
	public static void main(String[] argv) {
		try {
			AppGameContainer container = new AppGameContainer(new TileMapTest());
			container.setDisplayMode(800,600,false);
			container.start();
		} catch (SlickException e) {
			e.printStackTrace();
		}
	}
}
