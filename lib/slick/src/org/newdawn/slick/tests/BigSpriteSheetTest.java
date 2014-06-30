package org.newdawn.slick.tests;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.BigImage;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.SpriteSheet;

/**
 * A test for big images used as sprites sheets
 *
 * @author kevin
 */
public class BigSpriteSheetTest extends BasicGame {
	/** The original 1024x768 image loaded */
	private Image original;
	/** A sprite sheet made from the big image */
	private SpriteSheet bigSheet;
	/** True if we should use the old method */
	private boolean oldMethod = true;
	
	/**
	 * Create a new image rendering test
	 */
	public BigSpriteSheetTest() {
		super("Big SpriteSheet Test");
	}
	
	/**
	 * @see org.newdawn.slick.BasicGame#init(org.newdawn.slick.GameContainer)
	 */
	public void init(GameContainer container) throws SlickException {
		original = new BigImage("testdata/bigimage.tga", Image.FILTER_NEAREST, 256);
		bigSheet = new SpriteSheet(original, 16, 16);
	}

	/**
	 * @see org.newdawn.slick.BasicGame#render(org.newdawn.slick.GameContainer, org.newdawn.slick.Graphics)
	 */
	public void render(GameContainer container, Graphics g) {
		if (oldMethod) {
			for (int x=0;x<43;x++) {
				for (int y=0;y<27;y++) {
					bigSheet.getSprite(x, y).draw(10+(x*18),50+(y*18));
				}
			}
		} else {
			bigSheet.startUse();
			for (int x=0;x<43;x++) {
				for (int y=0;y<27;y++) {
					bigSheet.renderInUse(10+(x*18),50+(y*18),x,y);
				}
			}
			bigSheet.endUse();
		}
		
		g.drawString("Press space to toggle rendering method",10,30);
		
		container.getDefaultFont().drawString(10, 100, "TEST");
	}

	/**
	 * Entry point to our test
	 * 
	 * @param argv The arguments to pass into the test
	 */
	public static void main(String[] argv) {
		try {
			AppGameContainer container = new AppGameContainer(new BigSpriteSheetTest());
			container.setDisplayMode(800,600,false);
			container.start();
		} catch (SlickException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @see org.newdawn.slick.BasicGame#update(org.newdawn.slick.GameContainer, int)
	 */
	public void update(GameContainer container, int delta) throws SlickException {
		if (container.getInput().isKeyPressed(Input.KEY_SPACE)) {
			oldMethod = !oldMethod;
		}
	}
}
