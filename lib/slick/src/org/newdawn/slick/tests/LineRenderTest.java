package org.newdawn.slick.tests;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Path;
import org.newdawn.slick.geom.Polygon;
import org.newdawn.slick.opengl.renderer.Renderer;

/**
 * A test for the line rendering capability
 * 
 * @author kevin
 */
public class LineRenderTest extends BasicGame {
	/** The polygon to be rendered */
	private Polygon polygon = new Polygon();
	/** The path to be rendered */
	private Path path = new Path(100,100);
	/** The line width to render to */
	private float width = 10;
	/** True if antialiasing */
	private boolean antialias = true;
	
	/**
	 * Create a new test
	 */
	public LineRenderTest() {
		super("LineRenderTest");
	}
	
	/**
	 * @see org.newdawn.slick.BasicGame#init(org.newdawn.slick.GameContainer)
	 */
	public void init(GameContainer container) throws SlickException {
		polygon.addPoint(100,100);
		polygon.addPoint(200,80);
		polygon.addPoint(320,150);
		polygon.addPoint(230,210);
		polygon.addPoint(170,260);
		
		path.curveTo(200,200,200,100,100,200);
		path.curveTo(400,100,400,200,200,100);
		path.curveTo(500,500,400,200,200,100);
	}

	/**
	 * @see org.newdawn.slick.BasicGame#update(org.newdawn.slick.GameContainer, int)
	 */
	public void update(GameContainer container, int delta) throws SlickException {
		if (container.getInput().isKeyPressed(Input.KEY_SPACE)) {
			antialias = !antialias;
		}
	}

	/**
	 * @see org.newdawn.slick.Game#render(org.newdawn.slick.GameContainer, org.newdawn.slick.Graphics)
	 */
	public void render(GameContainer container, Graphics g) throws SlickException {
		g.setAntiAlias(antialias);
		g.setLineWidth(50);
		g.setColor(Color.red);
		g.draw(path);
		
//		g.setColor(Color.red);
//		TextureImpl.bindNone();
//		g.setLineWidth(width);
//		g.setAntiAlias(true);
//		for (int i=0;i<10;i++) {
//			g.translate(35,35);
//			g.draw(polygon);
//		}
//		g.translate(-350,-350);
//		
//		g.setColor(Color.white);
//		g.setLineWidth(1);
//		g.setAntiAlias(false);
//		g.draw(polygon);
	}

	/**
	 * Entry point to our test
	 * 
	 * @param argv The arguments passed to the test
	 */
	public static void main(String[] argv) {
		try {
			Renderer.setLineStripRenderer(Renderer.QUAD_BASED_LINE_STRIP_RENDERER);
			Renderer.getLineStripRenderer().setLineCaps(true);
			
			AppGameContainer container = new AppGameContainer(new LineRenderTest());
			container.setDisplayMode(800,600,false);
			container.start();
		} catch (SlickException e) {
			e.printStackTrace();
		}
	}
}
