package org.newdawn.slick.tests;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;

/**
 * A test for basic image rendering
 *
 * @author kevin
 */
public class ImageTest extends BasicGame {
	/** The TGA image loaded */
	private Image tga;
	/** The TGA image loaded */
	private Image scaleMe;
	/** The TGA image loaded */
	private Image scaled;
	/** The GIF version of the image */
	private Image gif;
	/** The image we're currently displaying */
	private Image image;
	/** A sub part of the logo image */
	private Image subImage;
    /** Newer image rotation image. */
    private Image rotImage;
	/** The current rotation of our test image */
	private float rot;
	/** True if the test should just exit first time round, used for testing shared contexts */
	public static boolean exitMe = true;
	
	/**
	 * Create a new image rendering test
	 */
	public ImageTest() {
		super("Image Test");
	}
	
	/**
	 * @see org.newdawn.slick.BasicGame#init(org.newdawn.slick.GameContainer)
	 */
	public void init(GameContainer container) throws SlickException {
		image = tga = new Image("testdata/logo.png");
        rotImage = new Image("testdata/logo.png");
        rotImage = rotImage.getScaledCopy(rotImage.getWidth() / 2, rotImage.getHeight() / 2);
        //rotImage.setCenterOfRotation(0,0);
        
		scaleMe = new Image("testdata/logo.tga", true, Image.FILTER_NEAREST);
		gif = new Image("testdata/logo.gif");
		gif.destroy();
		gif = new Image("testdata/logo.gif");
		scaled = gif.getScaledCopy(120, 120);
		subImage = image.getSubImage(200,0,70,260);
		rot = 0;
		
		if (exitMe) {
			container.exit();
		}
		
		Image test = tga.getSubImage(50,50,50,50);
		System.out.println(test.getColor(50, 50));
	}

	/**
	 * @see org.newdawn.slick.BasicGame#render(org.newdawn.slick.GameContainer, org.newdawn.slick.Graphics)
	 */
	public void render(GameContainer container, Graphics g) {
		g.drawRect(0,0,image.getWidth(),image.getHeight());
		image.draw(0,0);
		image.draw(500,0,200,100);
		scaleMe.draw(500,100,200,100);
		scaled.draw(400,500);
		Image flipped = scaled.getFlippedCopy(true, false);
		flipped.draw(520,500);
		Image flipped2 = flipped.getFlippedCopy(false, true);
		flipped2.draw(520,380);
		Image flipped3 = flipped2.getFlippedCopy(true, false);
		flipped3.draw(400,380);
		
		for (int i=0;i<3;i++) {
			subImage.draw(200+(i*30),300);
		}
		
		g.translate(500, 200);
		g.rotate(50, 50, rot);
		g.scale(0.3f,0.3f);
		image.draw();
		g.resetTransform();
        
        rotImage.setRotation(rot);
        rotImage.draw(100, 200);
	}

	/**
	 * @see org.newdawn.slick.BasicGame#update(org.newdawn.slick.GameContainer, int)
	 */
	public void update(GameContainer container, int delta) {
		rot += delta * 0.1f;
		if (rot > 360) {
			rot -= 360;
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
			exitMe = false;
			if (sharedContextTest) {
				GameContainer.enableSharedContext();
				exitMe = true;
			}
			
			AppGameContainer container = new AppGameContainer(new ImageTest());
			container.setForceExit(!sharedContextTest);
			container.setDisplayMode(800,600,false);
			container.start();
			
			if (sharedContextTest) {
				System.out.println("Exit first instance");
				exitMe = false;
				container = new AppGameContainer(new ImageTest());
				container.setDisplayMode(800,600,false);
				container.start();
			}
		} catch (SlickException e) {
			e.printStackTrace();
		}
	}
	

	/**
	 * @see org.newdawn.slick.BasicGame#keyPressed(int, char)
	 */
	public void keyPressed(int key, char c) {
		if (key == Input.KEY_SPACE) {
			if (image == gif) {
				image = tga;
			} else {
				image = gif;
			}
		}
	}
}
