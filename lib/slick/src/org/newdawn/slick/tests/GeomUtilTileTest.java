package org.newdawn.slick.tests;

import java.util.ArrayList;
import java.util.HashSet;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Circle;
import org.newdawn.slick.geom.GeomUtil;
import org.newdawn.slick.geom.GeomUtilListener;
import org.newdawn.slick.geom.Polygon;
import org.newdawn.slick.geom.Shape;
import org.newdawn.slick.geom.Vector2f;

/**
 * A test to try shape building from multiple tiles
 * 
 * @author Kevin Glass
 */
public class GeomUtilTileTest extends BasicGame implements GeomUtilListener {
	/** The shape we're cutting out of */
	private Shape source;
	/** The shape we're cutting */
	private Shape cut;
	/** The resulting shape */
	private Shape[] result;

	/** The util under test */
	private GeomUtil util = new GeomUtil();

	/** The original list of shapes */
	private ArrayList original = new ArrayList();
	/** The original list of shapes */
	private ArrayList combined = new ArrayList();

	/** The list of intersection points */
	private ArrayList intersections = new ArrayList();
	/** The list of used points */
	private ArrayList used = new ArrayList();

	/** The quad space of shapes that need to be checked against each other */
	private ArrayList[][] quadSpace;
	/** The shapes present in each quad space - used to optimize generation */
	private Shape[][] quadSpaceShapes;
	
	/**
	 * Create a simple test
	 */
	public GeomUtilTileTest() {
		super("GeomUtilTileTest");
	}

	/**
	 * So this is going to generate a quad space that holds that segments the
	 * shapes into quads across the map. This makes it tunable and limits the number
	 * of comparisons that need to be done for each shape
	 * 
	 * @param shapes The shapes to be segments
	 * @param minx The minimum x value of the map
	 * @param miny The mimimum y value of the map
	 * @param maxx The maximum x value of the map
	 * @param maxy The maximum y value of the map
	 * @param segments The number of segments to split the map into
	 */
	private void generateSpace(ArrayList shapes, float minx, float miny, float maxx, float maxy, int segments) {
		quadSpace = new ArrayList[segments][segments];
		quadSpaceShapes = new Shape[segments][segments];
		
		float dx = (maxx - minx) / segments;
		float dy = (maxy - miny) / segments;
		
		for (int x=0;x<segments;x++) {
			for (int y=0;y<segments;y++) {
				quadSpace[x][y] = new ArrayList();
				
				// quad for this segment
				Polygon segmentPolygon = new Polygon();
				segmentPolygon.addPoint(minx+(dx*x), miny+(dy*y));
				segmentPolygon.addPoint(minx+(dx*x)+dx, miny+(dy*y));
				segmentPolygon.addPoint(minx+(dx*x)+dx, miny+(dy*y)+dy);
				segmentPolygon.addPoint(minx+(dx*x), miny+(dy*y)+dy);
				
				for (int i=0;i<shapes.size();i++) {
					Shape shape = (Shape) shapes.get(i);
					
					if (collides(shape, segmentPolygon)) {
						quadSpace[x][y].add(shape);
					}
				}
				
				quadSpaceShapes[x][y] = segmentPolygon;
			}
		}
	}
	
	/**
	 * Remove the given shape from the quad space
	 * 
	 * @param shape The shape to remove
	 */
	private void removeFromQuadSpace(Shape shape) {
		int segments = quadSpace.length;
		
		for (int x=0;x<segments;x++) {
			for (int y=0;y<segments;y++) {
				quadSpace[x][y].remove(shape);
			}
		}
	}
	
	/**
	 * Add a particular shape to quad space
	 * 
	 * @param shape The shape to be added
	 */
	private void addToQuadSpace(Shape shape) {
		int segments = quadSpace.length;
		
		for (int x=0;x<segments;x++) {
			for (int y=0;y<segments;y++) {
				if (collides(shape, quadSpaceShapes[x][y])) {
					quadSpace[x][y].add(shape);
				}
			}
		}
	}
	
	/**
	 * Perform the cut
	 */
	public void init() {
		int size = 10;
		int[][] map = new int[][] { 
				{ 0, 0, 0, 0, 0, 0, 0, 3, 0, 0 },
				{ 0, 1, 1, 1, 0, 0, 1, 1, 1, 0 },
				{ 0, 1, 1, 0, 0, 0, 5, 1, 6, 0 },
				{ 0, 1, 2, 0, 0, 0, 4, 1, 1, 0 },
				{ 0, 1, 1, 0, 0, 0, 1, 1, 0, 0 },
				{ 0, 0, 0, 0, 3, 0, 1, 1, 0, 0 },
				{ 0, 0, 0, 1, 1, 0, 0, 0, 1, 0 },
				{ 0, 0, 0, 1, 1, 0, 0, 0, 0, 0 },
				{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
				{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, 
				};
		
//		size = 100;
//		map = new int[size][size];
//		for (int x=0;x<size;x++) {
//			for (int y=0;y<size;y++) {
//				if ((x+y) % 2 == 0) {
//					map[y][x] = 1;
//				}
//			}
//		}
		
		for (int x = 0; x < map[0].length; x++) {
			for (int y = 0; y < map.length; y++) {
				if (map[y][x] != 0) {
					switch (map[y][x]) {
					case 1:
						Polygon p2 = new Polygon();
						p2.addPoint(x * 32, y * 32);
						p2.addPoint((x * 32) + 32, y * 32);
						p2.addPoint((x * 32) + 32, (y * 32) + 32);
						p2.addPoint(x * 32, (y * 32) + 32);
						original.add(p2);
						break;
					case 2:
						Polygon poly = new Polygon();
						poly.addPoint(x * 32, y * 32);
						poly.addPoint((x * 32) + 32, y * 32);
						poly.addPoint(x * 32, (y * 32) + 32);
						original.add(poly);
						break;
					case 3:
						Circle ellipse = new Circle((x*32)+16,(y*32)+32,16,16);
						original.add(ellipse);
						break;
					case 4:
						Polygon p = new Polygon();
						p.addPoint((x * 32) + 32, (y * 32));
						p.addPoint((x * 32) + 32, (y * 32)+32);
						p.addPoint(x * 32, (y * 32) + 32);
						original.add(p);
						break;
					case 5:
						Polygon p3 = new Polygon();
						p3.addPoint((x * 32), (y * 32));
						p3.addPoint((x * 32) + 32, (y * 32));
						p3.addPoint((x * 32) + 32, (y * 32)+32);
						original.add(p3);
						break;
					case 6:
						Polygon p4 = new Polygon();
						p4.addPoint((x * 32), (y * 32));
						p4.addPoint((x * 32) + 32, (y * 32));
						p4.addPoint((x * 32), (y * 32)+32);
						original.add(p4);
						break;
					}
				}
			}
		}

		long before = System.currentTimeMillis();
		
		// the quad spaced method
		generateSpace(original, 0, 0, (size+1)*32,(size+1)*32,8);
		combined = combineQuadSpace();
		
		// the brute force method
		//combined = combine(original);
		
		long after = System.currentTimeMillis();
		System.out.println("Combine took: "+(after-before));
		System.out.println("Combine result: "+combined.size());
	}

	/**
	 * Combine the shapes in the quad space
	 *  
	 * @return The newly combined list of shapes
	 */
	private ArrayList combineQuadSpace() {
		boolean updated = true;
		while (updated) {
			updated = false;
			
			for (int x=0;x<quadSpace.length;x++) {
				for (int y=0;y<quadSpace.length;y++) {
					ArrayList shapes = quadSpace[x][y];
					int before = shapes.size();
					combine(shapes);
					int after = shapes.size();
					
					updated |= before != after;
				}
			}
		}
		
		// at this stage all the shapes that can be combined within their quads
		// will have gone on - we may need to combine stuff on the boundary tho
		HashSet result = new HashSet();
		
		for (int x=0;x<quadSpace.length;x++) {
			for (int y=0;y<quadSpace.length;y++) {
				result.addAll(quadSpace[x][y]);
			}
		}
		
		return new ArrayList(result);
	}
	
	/**
	 * Combine a set of shapes together
	 * 
	 * @param shapes
	 *            The shapes to be combined
	 * @return The list of combined shapes
	 */
	private ArrayList combine(ArrayList shapes) {
		ArrayList last = shapes;
		ArrayList current = shapes;
		boolean first = true;

		while ((current.size() != last.size()) || (first)) {
			first = false;
			last = current;
			current = combineImpl(current);
		}

		ArrayList pruned = new ArrayList();
		for (int i = 0; i < current.size(); i++) {
			pruned.add(((Shape) current.get(i)).prune());
		}
		return pruned;
	}

	/**
	 * Attempt to find a simple combination that can be performed
	 * 
	 * @param shapes
	 *            The shapes to be combined
	 * @return The new list of shapes - this will be the same length as the
	 *         input if there are no new combinations
	 */
	private ArrayList combineImpl(ArrayList shapes) {
		ArrayList result = new ArrayList(shapes);
		if (quadSpace != null) {
			result = shapes;
		}
		
		for (int i = 0; i < shapes.size(); i++) {
			Shape first = (Shape) shapes.get(i);
			for (int j = i + 1; j < shapes.size(); j++) {
				Shape second = (Shape) shapes.get(j);

				if (!first.intersects(second)) {
					continue;
				}
				
				Shape[] joined = util.union(first, second);
				if (joined.length == 1) {
					if (quadSpace != null) {
						removeFromQuadSpace(first);
						removeFromQuadSpace(second);
						addToQuadSpace(joined[0]);
					} else {
						result.remove(first);
						result.remove(second);
						result.add(joined[0]);
					}
					return result;
				}
			}
		}

		return result;
	}

	/**
	 * Check if two shapes collide
	 * 
	 * @param shape1 The first shape
	 * @param shape2 The second shape
	 * @return True if the shapes collide (i.e. intersection or overlap)
	 */
	public boolean collides(Shape shape1, Shape shape2) {
		if (shape1.intersects(shape2)) {
			return true;
		}
		for (int i=0;i<shape1.getPointCount();i++) {
			float[] pt = shape1.getPoint(i);
			if (shape2.contains(pt[0], pt[1])) {
				return true;
			}
		}
		for (int i=0;i<shape2.getPointCount();i++) {
			float[] pt = shape2.getPoint(i);
			if (shape1.contains(pt[0], pt[1])) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * @see BasicGame#init(GameContainer)
	 */
	public void init(GameContainer container) throws SlickException {
		util.setListener(this);
		init();
		//container.setVSync(true);
	}

	/**
	 * @see BasicGame#update(GameContainer, int)
	 */
	public void update(GameContainer container, int delta)
			throws SlickException {
	}

	/**
	 * @see org.newdawn.slick.Game#render(GameContainer, Graphics)
	 */
	public void render(GameContainer container, Graphics g)
			throws SlickException {
		g.setColor(Color.green);
		for (int i = 0; i < original.size(); i++) {
			Shape shape = (Shape) original.get(i);
			g.draw(shape);
		}

		g.setColor(Color.white);
		if (quadSpaceShapes != null) {
			g.draw(quadSpaceShapes[0][0]);
		}
		
		g.translate(0, 320);

		for (int i = 0; i < combined.size(); i++) {
			g.setColor(Color.white);
			Shape shape = (Shape) combined.get(i);
			g.draw(shape);
			for (int j = 0; j < shape.getPointCount(); j++) {
				g.setColor(Color.yellow);
				float[] pt = shape.getPoint(j);
				g.fillOval(pt[0] - 1, pt[1] - 1, 3, 3);
			}
		}

	}

	/**
	 * Entry point to our test
	 * 
	 * @param argv
	 *            The arguments passed to the test
	 */
	public static void main(String[] argv) {
		try {
			AppGameContainer container = new AppGameContainer(
					new GeomUtilTileTest());
			container.setDisplayMode(800, 600, false);
			container.start();
		} catch (SlickException e) {
			e.printStackTrace();
		}
	}

	public void pointExcluded(float x, float y) {
	}

	public void pointIntersected(float x, float y) {
		intersections.add(new Vector2f(x, y));
	}

	public void pointUsed(float x, float y) {
		used.add(new Vector2f(x, y));
	}
}
