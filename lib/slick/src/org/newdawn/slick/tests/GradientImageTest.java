package org.newdawn.slick.tests;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.fills.GradientFill;
import org.newdawn.slick.geom.Polygon;
import org.newdawn.slick.geom.Rectangle;
import org.newdawn.slick.geom.Shape;

/**
 * A test for applying gradients to images
 *
 * @author kevin
 */
public class GradientImageTest extends BasicGame {
	/** The first image loaded */
	private Image image1;
	/** The second image loaded */
	private Image image2;
	/** The gradient paint we'll apply */
	private GradientFill fill;
	/** The shape we'll blend across */
	private Shape shape;
	/** The shape we'll blend across */
	private Polygon poly;
	/** The container for the test */
	private GameContainer container;
	/** The angle of rotation */
	private float ang;
	/** True if we're rotating */
	private boolean rotating = false;
	
	/**
	 * Create a new image rendering test
	 */
	public GradientImageTest() {
		super("Gradient Image Test");
	}
	
	/**
	 * @see org.newdawn.slick.BasicGame#init(org.newdawn.slick.GameContainer)
	 */
	public void init(GameContainer container) throws SlickException {
		this.container = container;
		
		image1 = new Image("testdata/grass.png");
		image2 = new Image("testdata/rocks.png");
		
		fill = new GradientFill(-64,0,new Color(1,1,1,1f),64,0,new Color(0,0,0,0));
		shape = new Rectangle(336,236,128,128);
	    poly = new Polygon();
		poly.addPoint(320,220);
		poly.addPoint(350,200);
		poly.addPoint(450,200);
		poly.addPoint(480,220);
		poly.addPoint(420,400);
		poly.addPoint(400,390);
	}

	/**
	 * @see org.newdawn.slick.BasicGame#render(org.newdawn.slick.GameContainer, org.newdawn.slick.Graphics)
	 */
	public void render(GameContainer container, Graphics g) {
		g.drawString("R - Toggle Rotationg",10,50);
		g.drawImage(image1, 100, 236);
		g.drawImage(image2, 600, 236);
		
		g.translate(0, -150);
		g.rotate(400, 300, ang);
		g.texture(shape, image2);
		g.texture(shape, image1, fill);
		g.resetTransform();
		
		g.translate(0, 150);
		g.rotate(400, 300, ang);
		g.texture(poly, image2);
		g.texture(poly, image1, fill);
		g.resetTransform();
	}

	/**
	 * @see org.newdawn.slick.BasicGame#update(org.newdawn.slick.GameContainer, int)
	 */
	public void update(GameContainer container, int delta) {
		if (rotating) {
			ang += (delta * 0.1f);
		}
	}

	/**
	 * Entry point to our test
	 * 
	 * @param argv The arguments to pass into the test
	 */
	public static void main(String[] argv) {
		try {
			AppGameContainer container = new AppGameContainer(new GradientImageTest());
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
		if (key == Input.KEY_R) {
			rotating = !rotating;
		}
		if (key == Input.KEY_ESCAPE) {
			container.exit();
		}
	}
}
