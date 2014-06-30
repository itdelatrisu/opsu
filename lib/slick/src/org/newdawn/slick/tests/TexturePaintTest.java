package org.newdawn.slick.tests;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Polygon;
import org.newdawn.slick.geom.Rectangle;
import org.newdawn.slick.geom.ShapeRenderer;
import org.newdawn.slick.geom.TexCoordGenerator;
import org.newdawn.slick.geom.Vector2f;

/**
 * Test to emulate texture paint
 * 
 * @author kevin
 */
public class TexturePaintTest extends BasicGame {
	/** The poly being drawn */
	private Polygon poly = new Polygon();
	/** The image being textured */
	private Image image;
	
	/** The texture paint rectangle */
	private Rectangle texRect = new Rectangle(50,50,100,100);
	/** The texture paint */
	private TexCoordGenerator texPaint;
	
	/**
	 * Create the test
	 */
	public TexturePaintTest() {
		super("Texture Paint Test");
	}
	
	/**
	 * @see org.newdawn.slick.BasicGame#init(org.newdawn.slick.GameContainer)
	 */
	public void init(GameContainer container) throws SlickException {
		poly.addPoint(120, 120);
		poly.addPoint(420, 100);
		poly.addPoint(620, 420);
		poly.addPoint(300, 320);
	
		image = new Image("testdata/rocks.png");
		
		texPaint = new TexCoordGenerator() {
			public Vector2f getCoordFor(float x, float y) {
				float tx = (texRect.getX() - x) / texRect.getWidth();
				float ty = (texRect.getY() - y) / texRect.getHeight();
				
				return new Vector2f(tx,ty);
			}
		};
	}

	/**
	 * @see org.newdawn.slick.BasicGame#update(org.newdawn.slick.GameContainer, int)
	 */
	public void update(GameContainer container, int delta) throws SlickException {
	}

	/**
	 * @see org.newdawn.slick.Game#render(org.newdawn.slick.GameContainer, org.newdawn.slick.Graphics)
	 */
	public void render(GameContainer container, Graphics g) throws SlickException {
		g.setColor(Color.white);
		g.texture(poly, image);
		
		ShapeRenderer.texture(poly, image, texPaint);
	}

	/**
	 * Entry point to our test
	 * 
	 * @param argv The arguments to pass into the test
	 */
	public static void main(String[] argv) {
		try {
			AppGameContainer container = new AppGameContainer(new TexturePaintTest());
			container.setDisplayMode(800,600,false);
			container.start();
		} catch (SlickException e) {
			e.printStackTrace();
		}
	}
}
