package org.newdawn.slick.tests;

import java.util.ArrayList;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Circle;
import org.newdawn.slick.geom.GeomUtil;
import org.newdawn.slick.geom.GeomUtilListener;
import org.newdawn.slick.geom.Polygon;
import org.newdawn.slick.geom.Rectangle;
import org.newdawn.slick.geom.Shape;
import org.newdawn.slick.geom.Transform;
import org.newdawn.slick.geom.Vector2f;

/**
 * A test to try shape cutting
 * 
 * @author Kevin Glass
 */
public class GeomUtilTest extends BasicGame implements GeomUtilListener {
	/** The shape we're cutting out of */
	private Shape source;
	/** The shape we're cutting */
	private Shape cut;
	/** The resulting shape */
	private Shape[] result;
	
	/** The points used */
	private ArrayList points = new ArrayList();
	/** The points intersected */
	private ArrayList marks = new ArrayList();
	/** The points excluded */
	private ArrayList exclude = new ArrayList();
	
	/** True if we're moving the shape around */
	private boolean dynamic;
	/** The util under test */
	private GeomUtil util = new GeomUtil();
	/** The x position of the shape */
	private int xp;
	/** The y position of the shape */
	private int yp;
	
	/** The circle cutting tool */
	private Circle circle;
	/** The rectangle cutting tool */
	private Shape rect;
	/** The star cutting tool */
	private Polygon star;
	/** True if we're in union mode */
	private boolean union;
	
	/**
	 * Create a simple test
	 */
	public GeomUtilTest() {
		super("GeomUtilTest");
	}

	/**
	 * Perform the cut
	 */
	public void init() {
		Polygon source = new Polygon();
		source.addPoint(100,100);
		source.addPoint(150,80);
		source.addPoint(210,120);
		source.addPoint(340,150);
		source.addPoint(150,200);
		source.addPoint(120,250);
		this.source = source;
		
		circle = new Circle(0,0,50);
		rect = new Rectangle(-100,-40,200,80);
		star = new Polygon();
		
		float dis = 40;
		for (int i=0;i<360;i+=30) {
			dis = dis == 40 ? 60 : 40;
			double x = (Math.cos(Math.toRadians(i)) * dis);
			double y = (Math.sin(Math.toRadians(i)) * dis);
			star.addPoint((float) x, (float) y);
		}
		
		this.cut = circle;
		cut.setLocation(203,78);
		xp = (int) cut.getCenterX();
		yp = (int) cut.getCenterY();
		makeBoolean();
	}
	
	/**
	 * @see BasicGame#init(GameContainer)
	 */
	public void init(GameContainer container) throws SlickException {
		util.setListener(this);
		init();
		container.setVSync(true);
	}

	/**
	 * @see BasicGame#update(GameContainer, int)
	 */
	public void update(GameContainer container, int delta)
			throws SlickException {
		if (container.getInput().isKeyPressed(Input.KEY_SPACE)) {
			dynamic = !dynamic;
		}
		if (container.getInput().isKeyPressed(Input.KEY_ENTER)) {
			union = !union;
			makeBoolean();
		}
		if (container.getInput().isKeyPressed(Input.KEY_1)) {
			cut = circle;
			circle.setCenterX(xp);
			circle.setCenterY(yp);
			makeBoolean();
		}
		if (container.getInput().isKeyPressed(Input.KEY_2)) {
			cut = rect;
			rect.setCenterX(xp);
			rect.setCenterY(yp);
			makeBoolean();
		}
		if (container.getInput().isKeyPressed(Input.KEY_3)) {
			cut = star;
			star.setCenterX(xp);
			star.setCenterY(yp);
			makeBoolean();
		}
		
		if (dynamic) {
			xp = container.getInput().getMouseX();
			yp = container.getInput().getMouseY();
			makeBoolean();
		}
	}

	/**
	 * Make the boolean operation
	 */
	private void makeBoolean() {
		marks.clear();
		points.clear();
		exclude.clear();
		cut.setCenterX(xp);
		cut.setCenterY(yp);
		if (union) {
			result = util.union(source, cut);
		} else {
			result = util.subtract(source, cut);
		}
	}
	
	/**
	 * @see org.newdawn.slick.Game#render(GameContainer, Graphics)
	 */
	public void render(GameContainer container, Graphics g)
			throws SlickException {
		g.drawString("Space - toggle movement of cutting shape",530,10);
		g.drawString("1,2,3 - select cutting shape",530,30);
		g.drawString("Mouse wheel - rotate shape",530,50);
		g.drawString("Enter - toggle union/subtract",530,70);
		g.drawString("MODE: "+(union ? "Union" : "Cut"),530,200);
		
		g.setColor(Color.green);
		g.draw(source);
		g.setColor(Color.red);
		g.draw(cut);

		g.setColor(Color.white);
		for (int i=0;i<exclude.size();i++) {
			Vector2f pt = (Vector2f) exclude.get(i);
			g.drawOval(pt.x-3, pt.y-3, 7,7);
		}
		g.setColor(Color.yellow);
		for (int i=0;i<points.size();i++) {
			Vector2f pt = (Vector2f) points.get(i);
			g.fillOval(pt.x-1, pt.y-1, 3,3);
		}
		g.setColor(Color.white);
		for (int i=0;i<marks.size();i++) {
			Vector2f pt = (Vector2f) marks.get(i);
			g.fillOval(pt.x-1, pt.y-1, 3,3);
		}
		
		g.translate(0,300);
		g.setColor(Color.white);
		if (result != null) {
			for (int i=0;i<result.length;i++) {
				g.draw(result[i]);
			}
			
			g.drawString("Polys:"+result.length,10,100);
			g.drawString("X:"+xp,10,120);
			g.drawString("Y:"+yp,10,130);
		}
		
	}
	
	/**
	 * Entry point to our test
	 * 
	 * @param argv The arguments passed to the test
	 */
	public static void main(String[] argv) {
		try {
			AppGameContainer container = new AppGameContainer(new GeomUtilTest());
			container.setDisplayMode(800,600,false);
			container.start();
		} catch (SlickException e) {
			e.printStackTrace();
		}
	}

	public void pointExcluded(float x, float y) {
		exclude.add(new Vector2f(x,y));
	}

	public void pointIntersected(float x, float y) {
		marks.add(new Vector2f(x,y));
	}

	public void pointUsed(float x, float y) {
		points.add(new Vector2f(x,y));
	}

	public void mouseWheelMoved(int change) {
		if (dynamic) {
			if (change < 0) {
				cut = cut.transform(Transform.createRotateTransform((float) Math.toRadians(10), cut.getCenterX(), cut.getCenterY()));
			} else {
				cut = cut.transform(Transform.createRotateTransform((float) Math.toRadians(-10), cut.getCenterX(), cut.getCenterY()));
			}
		}
	}
}
