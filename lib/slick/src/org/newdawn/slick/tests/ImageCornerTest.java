package org.newdawn.slick.tests;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;

/**
 * A test for basic image rendering
 *
 * @author kevin
 */
public class ImageCornerTest extends BasicGame {
	/** The test image */
	private Image image;
	/** The sub images */
	private Image[] images;
	/** The width */
	private int width;
	/** The height */
	private int height;
	
	/**
	 * Create a new image rendering test
	 */
	public ImageCornerTest() {
		super("Image Corner Test");
	}
	
	/**
	 * @see org.newdawn.slick.BasicGame#init(org.newdawn.slick.GameContainer)
	 */
	public void init(GameContainer container) throws SlickException {
		image = new Image("testdata/logo.png");
		
		width = image.getWidth() / 3;
		height = image.getHeight() / 3;
		
		images = new Image[] {
				image.getSubImage(0, 0, width, height), image.getSubImage(width,0,width,height), image.getSubImage(width*2,0,width,height),
				image.getSubImage(0, height, width, height), image.getSubImage(width,height,width,height), image.getSubImage(width*2,height,width,height),
				image.getSubImage(0, height*2, width, height), image.getSubImage(width,height*2,width,height), image.getSubImage(width*2,height*2,width,height),
		};
		
		images[0].setColor(Image.BOTTOM_RIGHT, 0,1,1,1);
		images[1].setColor(Image.BOTTOM_LEFT, 0,1,1,1);
		images[1].setColor(Image.BOTTOM_RIGHT, 0,1,1,1);
		images[2].setColor(Image.BOTTOM_LEFT, 0,1,1,1);
		images[3].setColor(Image.TOP_RIGHT, 0,1,1,1);
		images[3].setColor(Image.BOTTOM_RIGHT, 0,1,1,1);
		
		images[4].setColor(Image.TOP_RIGHT, 0,1,1,1);
		images[4].setColor(Image.TOP_LEFT, 0,1,1,1);
		images[4].setColor(Image.BOTTOM_LEFT, 0,1,1,1);
		images[4].setColor(Image.BOTTOM_RIGHT, 0,1,1,1);
		images[5].setColor(Image.TOP_LEFT, 0,1,1,1);
		images[5].setColor(Image.BOTTOM_LEFT, 0,1,1,1);
		
		images[6].setColor(Image.TOP_RIGHT, 0,1,1,1);
		images[7].setColor(Image.TOP_RIGHT, 0,1,1,1);
		images[7].setColor(Image.TOP_LEFT, 0,1,1,1);
		images[8].setColor(Image.TOP_LEFT, 0,1,1,1);
	}

	/**
	 * @see org.newdawn.slick.BasicGame#render(org.newdawn.slick.GameContainer, org.newdawn.slick.Graphics)
	 */
	public void render(GameContainer container, Graphics g) {
		for (int x=0;x<3;x++) {
			for (int y=0;y<3;y++) {
				images[x+(y*3)].draw(100+(x*width),100+(y*height));
			}
		}
	}

	/**
	 * Entry point to our test
	 * 
	 * @param argv The arguments to pass into the test
	 */
	public static void main(String[] argv) {
		boolean sharedContextTest = false;
		
		try {
			AppGameContainer container = new AppGameContainer(new ImageCornerTest());
			container.setDisplayMode(800,600,false);
			container.start();
		} catch (SlickException e) {
			e.printStackTrace();
		}
	}

	public void update(GameContainer container, int delta) throws SlickException {
	}
}
