package org.newdawn.slick.tests;

import org.newdawn.slick.AngelCodeFont;
import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.Color;
import org.newdawn.slick.Font;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.opengl.pbuffer.GraphicsFactory;

/**
 * A test for rendering to an image
 *
 * @author kevin
 */
public class ImageGraphicsTest extends BasicGame {
	/** The image loaded and then rendered to */
	private Image preloaded;
	/** The image rendered to */
	private Image target;
	/** The image cut from the screen */
	private Image cut;
	/** The offscreen graphics */
	private Graphics gTarget;
	/** The offscreen graphics */
	private Graphics offscreenPreload;
	/** The image loaded */
	private Image testImage;
	/** The font loaded */
	private Font testFont;
	/** The angle of the rotation */
	private float ang;
	/** The name of the dynamic image technique in use */
	private String using = "none";
	
	/**
	 * Create a new image rendering test
	 */
	public ImageGraphicsTest() {
		super("Image Graphics Test");
	}
	
	/**
	 * @see org.newdawn.slick.BasicGame#init(org.newdawn.slick.GameContainer)
	 */
	public void init(GameContainer container) throws SlickException {
		testImage = new Image("testdata/logo.png");
		preloaded = new Image("testdata/logo.png");
		testFont = new AngelCodeFont("testdata/hiero.fnt","testdata/hiero.png");
		target = new Image(400,300);
		cut = new Image(100,100);
		gTarget = target.getGraphics();
		offscreenPreload = preloaded.getGraphics();
		
		offscreenPreload.drawString("Drawing over a loaded image", 5, 15);
		offscreenPreload.setLineWidth(5);
		offscreenPreload.setAntiAlias(true);
		offscreenPreload.setColor(Color.blue.brighter());
		offscreenPreload.drawOval(200, 30, 50, 50);
		offscreenPreload.setColor(Color.white);
		offscreenPreload.drawRect(190,20,70,70);
		offscreenPreload.flush();
		
		if (GraphicsFactory.usingFBO()) {
			using = "FBO (Frame Buffer Objects)";
		} else if (GraphicsFactory.usingPBuffer()) {
			using = "Pbuffer (Pixel Buffers)";
		}
		
		System.out.println(preloaded.getColor(50,50));
	}
	
	/**
	 * @see org.newdawn.slick.BasicGame#render(org.newdawn.slick.GameContainer, org.newdawn.slick.Graphics)
	 */
	public void render(GameContainer container, Graphics g) throws SlickException {

		// RENDERING TO AN IMAGE AND THEN DRAWING IT TO THE DISPLAY
		// Draw graphics and text onto our graphics context from the Image target
		gTarget.setBackground(new Color(0,0,0,0));
		gTarget.clear();
		gTarget.rotate(200,160,ang);
		gTarget.setFont(testFont);
		gTarget.fillRect(10, 10, 50, 50);
		gTarget.drawString("HELLO WORLD",10,10);

		gTarget.drawImage(testImage,100,150);
		gTarget.drawImage(testImage,100,50);
		gTarget.drawImage(testImage,50,75);
		
		// Note we started by clearing the offscreen graphics area and then end
		// by calling flush
		gTarget.flush(); 

		g.setColor(Color.red);
		g.fillRect(250, 50, 200, 200);
		// The image has been updated using its graphics context, so now draw the image
		// to the screen a few times
		target.draw(300,100);
		target.draw(300,410,200,150);
		target.draw(505,410,100,75);
		
		// Draw some text on the screen to indicate what we did and put some
		// nice boxes around the three areas
		g.setColor(Color.white);
		g.drawString("Testing On Offscreen Buffer", 300, 80);
		g.setColor(Color.green);
		g.drawRect(300, 100, target.getWidth(), target.getHeight());
		g.drawRect(300, 410, target.getWidth()/2, target.getHeight()/2);
		g.drawRect(505, 410, target.getWidth()/4, target.getHeight()/4);
		
		// SCREEN COPY EXAMPLE
		// Put some text and simple graphics on the screen to test copying
		// from the screen to a target image
		g.setColor(Color.white);
		g.drawString("Testing Font On Back Buffer", 10, 100);
		g.drawString("Using: "+using, 10, 580);
		g.setColor(Color.red);
		g.fillRect(10,120,200,5);
		
		// Copy the screen area into a destination image
		int xp = (int) (60 + (Math.sin(ang / 60) * 50));
		g.copyArea(cut,xp,50);
		
		// Draw the copied image to the screen and put some nice
		// boxes around the source and the destination
		cut.draw(30,250);
		g.setColor(Color.white);
		g.drawRect(30, 250, cut.getWidth(), cut.getHeight());
		g.setColor(Color.gray);
		g.drawRect(xp, 50, cut.getWidth(), cut.getHeight());
		
		// ALTERING A LOADED IMAGE EXAMPLE
		// Draw the image we loaded in the init method and then modified
		// by drawing some text and simple geometry on it
		preloaded.draw(2,400);
		g.setColor(Color.blue);
		g.drawRect(2,400,preloaded.getWidth(),preloaded.getHeight());
	}

	/**
	 * @see org.newdawn.slick.BasicGame#update(org.newdawn.slick.GameContainer, int)
	 */
	public void update(GameContainer container, int delta) {
		ang += delta * 0.1f;
	}

	/**
	 * Entry point to our test
	 * 
	 * @param argv The arguments to pass into the test
	 */
	public static void main(String[] argv) {
		try {
			GraphicsFactory.setUseFBO(false);
			
			AppGameContainer container = new AppGameContainer(new ImageGraphicsTest());
			container.setDisplayMode(800,600,false);
			container.start();
		} catch (SlickException e) {
			e.printStackTrace();
		}
	}
}
