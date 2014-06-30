package org.newdawn.slick.tests;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Curve;
import org.newdawn.slick.geom.Polygon;
import org.newdawn.slick.geom.Vector2f;

/**
 * A rudimentry test of loading SVG from inkscape
 *
 * @author kevin
 */
public class CurveTest extends BasicGame {
	/** The curve being rendered */
	private Curve curve;
	/** The start point of the curve */
	private Vector2f p1 = new Vector2f(100,300);
	/** The first control point */
	private Vector2f c1 = new Vector2f(100,100);
	/** The second control point */
	private Vector2f c2 = new Vector2f(300,100);
	/** The end point of the curve */
	private Vector2f p2 = new Vector2f(300,300);
	
	/** The polygon drawn next done */
	private Polygon poly;
	
	/**
	 * Create a new test for inkscape loading
	 */
	public CurveTest() {
		super("Curve Test");
	}

	/**
	 * @see org.newdawn.slick.BasicGame#init(org.newdawn.slick.GameContainer)
	 */
	public void init(GameContainer container) throws SlickException {
		container.getGraphics().setBackground(Color.white);
		
		curve = new Curve(p2,c2,c1,p1);
		poly = new Polygon();
		poly.addPoint(500,200);
		poly.addPoint(600,200);
		poly.addPoint(700,300);
		poly.addPoint(400,300);
	}

	/**
	 * @see org.newdawn.slick.BasicGame#update(org.newdawn.slick.GameContainer, int)
	 */
	public void update(GameContainer container, int delta) throws SlickException {
	}

	/**
	 * Draw a marker for a given point
	 * 
	 * @param g The graphics context on which to draw
	 * @param p The point to draw
	 */
	private void drawMarker(Graphics g, Vector2f p) {
		g.drawRect(p.x-5, p.y-5,10,10);
	}
	
	/**
	 * @see org.newdawn.slick.Game#render(org.newdawn.slick.GameContainer, org.newdawn.slick.Graphics)
	 */
	public void render(GameContainer container, Graphics g) throws SlickException {
		g.setColor(Color.gray);
		drawMarker(g, p1);
		drawMarker(g, p2);
		g.setColor(Color.red);
		drawMarker(g, c1);
		drawMarker(g, c2);
		
		g.setColor(Color.black);
		g.draw(curve);
		g.fill(curve);
		
		g.draw(poly);
		g.fill(poly);
	}
	
	/**
	 * Entry point to our simple test
	 * 
	 * @param argv The arguments passed in
	 */
	public static void main(String argv[]) {
		try {
			AppGameContainer container = new AppGameContainer(new CurveTest());
			container.setDisplayMode(800,600,false);
			container.start();
		} catch (SlickException e) {
			e.printStackTrace();
		}
	}
}
