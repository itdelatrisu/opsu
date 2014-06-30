package org.newdawn.slick.tests;

import java.io.IOException;

import org.newdawn.slick.BasicGame;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.util.Bootstrap;
import org.newdawn.slick.util.ResourceLoader;
import org.newdawn.slick.util.pathfinding.Mover;
import org.newdawn.slick.util.pathfinding.PathFindingContext;
import org.newdawn.slick.util.pathfinding.TileBasedMap;
import org.newdawn.slick.util.pathfinding.navmesh.Link;
import org.newdawn.slick.util.pathfinding.navmesh.NavMesh;
import org.newdawn.slick.util.pathfinding.navmesh.NavMeshBuilder;
import org.newdawn.slick.util.pathfinding.navmesh.NavPath;
import org.newdawn.slick.util.pathfinding.navmesh.Space;

/**
 * A test to show nav-mesh generation on tile based maps.
 * 
 * @author kevin
 */
public class NavMeshTest extends BasicGame implements PathFindingContext {
	/** The mesh built for this map */
	private NavMesh navMesh;
	/** The builder used to create the nav-mesh from the tile based map */
	private NavMeshBuilder builder;
	/** True if we're showing the open spaces from the mesh */
	private boolean showSpaces = true;
	/** True if we're showing the linking points */
	private boolean showLinks = true;
	/** The path if there is one current found between the two points */
	private NavPath path;
	
	/** The x coordinate of the start of the search */
	private float sx;
	/** The y coordinate of the start of the search */
	private float sy;
	/** The x coordinate of the end of the search */
	private float ex;
	/** The y coordinate of the end of the search */
	private float ey;
	/** The tile based map we're searching across - loaded from a raw file */
	private DataMap dataMap;
	
	/**
	 * Create a new test
	 */
	public NavMeshTest() {
		super("Nav-mesh Test");
	}

	/**
	 * Initialise resources and the map data
	 * 
	 * @param container the container the game is running in 
	 */
	public void init(GameContainer container) throws SlickException {
		container.setShowFPS(false);

		try {
			dataMap = new DataMap("testdata/map.dat");
		} catch (IOException e) {
			throw new SlickException("Failed to load map data", e);
		}
		builder = new NavMeshBuilder();
		navMesh = builder.build(dataMap);
		
		System.out.println("Navmesh shapes: "+navMesh.getSpaceCount());
	}
	
	/**
	 * Update data map etc
	 */
	public void update(GameContainer container, int delta)
			throws SlickException {
		if (container.getInput().isKeyPressed(Input.KEY_1)) {
			showLinks = !showLinks;
		}
		if (container.getInput().isKeyPressed(Input.KEY_2)) {
			showSpaces = !showSpaces;
		}
	}

	/**
	 * Render the game - in this case render the map and diagnostic data
	 * 
	 * @param container The container we're running the game in
	 * @param g The graphics context on which to render
	 */
	public void render(GameContainer container, Graphics g)
			throws SlickException {
		g.translate(50,50);
		for (int x=0;x<50;x++) {
			for (int y=0;y<50;y++) {
				if (dataMap.blocked(this, x, y)) {
					g.setColor(Color.gray);
					g.fillRect((x*10)+1,(y*10)+1,8,8);
				}
			}
		}
		
		if (showSpaces) {
			for (int i=0;i<navMesh.getSpaceCount();i++) {
				Space space = navMesh.getSpace(i);
				if (builder.clear(dataMap, space)) {
					g.setColor(new Color(1,1,0,0.5f));
					g.fillRect(space.getX()*10, space.getY()*10, space.getWidth()*10, space.getHeight()*10);
				}
				g.setColor(Color.yellow);
				g.drawRect(space.getX()*10, space.getY()*10, space.getWidth()*10, space.getHeight()*10);

				if (showLinks) {
					int links = space.getLinkCount();
					for (int j=0;j<links;j++) {
						Link link = space.getLink(j);
						g.setColor(Color.red);
						g.fillRect((link.getX()*10)-2, (link.getY()*10)-2,5,5);
					}
				}
			}
		}
		
		if (path != null) {
			g.setColor(Color.white);
			for (int i=0;i<path.length()-1;i++) {
				g.drawLine(path.getX(i)*10, path.getY(i)*10, path.getX(i+1)*10, path.getY(i+1)*10);
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.newdawn.slick.util.pathfinding.PathFindingContext#getMover()
	 */
	public Mover getMover() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.newdawn.slick.util.pathfinding.PathFindingContext#getSearchDistance()
	 */
	public int getSearchDistance() {
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * @see org.newdawn.slick.util.pathfinding.PathFindingContext#getSourceX()
	 */
	public int getSourceX() {
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * @see org.newdawn.slick.util.pathfinding.PathFindingContext#getSourceY()
	 */
	public int getSourceY() {
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * @see org.newdawn.slick.BasicGame#mousePressed(int, int, int)
	 */
	public void mousePressed(int button, int x, int y) {
		float mx = (x - 50) / 10.0f;
		float my = (y - 50) / 10.0f;
		
		if (button == 0) {
			sx = mx;
			sy = my;
		} else {
			ex = mx;
			ey = my;
		}

		path = navMesh.findPath(sx,sy,ex,ey,true);
	}

	/**
	 * A simple raw map implementation for testing purposes
	 * 
	 * @author kevin
	 */
	private class DataMap implements TileBasedMap {
		/** The map data */
		private byte[] map = new byte[50*50];
	
		/**
		 * Create a new map loading it from a file
		 * 
		 * @param ref The location to load the map from
		 * @throws IOException Indicatese a failure to access map data
		 */
		public DataMap(String ref) throws IOException {
			ResourceLoader.getResourceAsStream(ref).read(map);
		}

		/*
		 * (non-Javadoc)
		 * @see org.newdawn.slick.util.pathfinding.TileBasedMap#blocked(org.newdawn.slick.util.pathfinding.PathFindingContext, int, int)
		 */
		public boolean blocked(PathFindingContext context, int tx, int ty) {
			if ((tx < 0) || (ty < 0) || (tx >= 50) || (ty >= 50)) {
				return false;
			}
			
			return map[tx+(ty*50)] != 0;
		}
		
		/*
		 * (non-Javadoc)
		 * @see org.newdawn.slick.util.pathfinding.TileBasedMap#getCost(org.newdawn.slick.util.pathfinding.PathFindingContext, int, int)
		 */
		public float getCost(PathFindingContext context, int tx, int ty) {
			return 1;
		}

		/*
		 * (non-Javadoc)
		 * @see org.newdawn.slick.util.pathfinding.TileBasedMap#getHeightInTiles()
		 */
		public int getHeightInTiles() {
			return 50;
		}

		/*
		 * (non-Javadoc)
		 * @see org.newdawn.slick.util.pathfinding.TileBasedMap#getWidthInTiles()
		 */
		public int getWidthInTiles() {
			return 50;
		}

		/*
		 * (non-Javadoc)
		 * @see org.newdawn.slick.util.pathfinding.TileBasedMap#pathFinderVisited(int, int)
		 */
		public void pathFinderVisited(int x, int y) {
		}
	}
	
	/**
	 * Entry point to out application
	 * 
	 * @param argv The arguments passed to the application
	 */
	public static void main(String[] argv) {
		Bootstrap.runAsApplication(new NavMeshTest(), 600, 600, false);
	}
}
