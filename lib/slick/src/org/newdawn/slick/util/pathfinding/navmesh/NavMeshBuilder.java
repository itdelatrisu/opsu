package org.newdawn.slick.util.pathfinding.navmesh;

import java.util.ArrayList;

import org.newdawn.slick.util.pathfinding.Mover;
import org.newdawn.slick.util.pathfinding.PathFindingContext;
import org.newdawn.slick.util.pathfinding.TileBasedMap;

/**
 * The builder responsible for converting a tile based map into
 * a navigation mesh
 * 
 * @author kevin
 */
public class NavMeshBuilder implements PathFindingContext {
	/** The current x position we're searching */
	private int sx;
	/** The current y position we've searching */
	private int sy;
	/** The smallest space allowed */
	private float smallestSpace = 0.2f;
	/** True if we're working tile based */
	private boolean tileBased;
	
	/**
	 * Build a navigation mesh based on a tile map
	 * 
	 * @param map The map to build the navigation mesh from
	 * 
	 * @return The newly created navigation mesh 
	 */
	public NavMesh build(TileBasedMap map) {
		return build(map, true);
	}
	
	/**
	 * Build a navigation mesh based on a tile map
	 * 
	 * @param map The map to build the navigation mesh from
	 * @param tileBased True if we'll use the tiles for the mesh initially 
	 * rather than quad spacing
	 * @return The newly created navigation mesh 
	 */
	public NavMesh build(TileBasedMap map, boolean tileBased) {
		this.tileBased = tileBased;
		
		ArrayList spaces = new ArrayList();
		
		if (tileBased) {
			for (int x=0;x<map.getWidthInTiles();x++) {
				for (int y=0;y<map.getHeightInTiles();y++) {
					if (!map.blocked(this, x, y)) {
						spaces.add(new Space(x,y,1,1));
					}
				}
			}
		} else {
			Space space = new Space(0,0,map.getWidthInTiles(),map.getHeightInTiles());
		
			subsection(map, space, spaces);
		}
		
		while (mergeSpaces(spaces)) {}
		linkSpaces(spaces);
		
		return new NavMesh(spaces);
	}
	
	/**
	 * Merge the spaces that have been created to optimize out anywhere
	 * we can.
	 * 
	 * @param spaces The list of spaces to be merged
	 * @return True if a merge occured and we'll have to start the merge
	 * process again
	 */
	private boolean mergeSpaces(ArrayList spaces) {
		for (int source=0;source<spaces.size();source++) {
			Space a = (Space) spaces.get(source);
			
			for (int target=source+1;target<spaces.size();target++) {
				Space b = (Space) spaces.get(target);
				
				if (a.canMerge(b)) {
					spaces.remove(a);
					spaces.remove(b);
					spaces.add(a.merge(b));
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Determine the links between spaces
	 * 
	 * @param spaces The spaces to link up
	 */
	private void linkSpaces(ArrayList spaces) {
		for (int source=0;source<spaces.size();source++) {
			Space a = (Space) spaces.get(source);
			
			for (int target=source+1;target<spaces.size();target++) {
				Space b = (Space) spaces.get(target);
				
				if (a.hasJoinedEdge(b)) {
					a.link(b);
					b.link(a);
				}
			}
		}
	}
	
	/**
	 * Check if a particular space is clear of blockages
	 * 
	 * @param map The map the spaces are being built from
	 * @param space The space to check
	 * @return True if there are no blockages in the space
	 */
	public boolean clear(TileBasedMap map, Space space) {
		if (tileBased) {
			return true;
		}
		
		float x = 0;
		boolean donex = false;
		
		while (x < space.getWidth()) {
			float y = 0;
			boolean doney = false;
			
			while (y < space.getHeight()) {
				sx = (int) (space.getX()+x);
				sy = (int) (space.getY()+y);
				
				if (map.blocked(this, sx, sy)) {
					return false;
				}
				
				y += 0.1f;
				if ((y > space.getHeight()) && (!doney)) {
					y = space.getHeight();
					doney = true;
				}
			}
			
			
			x += 0.1f;
			if ((x > space.getWidth()) && (!donex)) {
				x = space.getWidth();
				donex = true;
			}
		}
		
		return true;
	}
	
	/**
	 * Subsection a space into smaller spaces if required to find a non-blocked
	 * area.
	 * 
	 * @param map The map being processed
	 * @param space The space being sections
	 * @param spaces The list of spaces that have been created
	 */
	private void subsection(TileBasedMap map, Space space, ArrayList spaces) {
		if (!clear(map, space)) {
			float width2 = space.getWidth()/2;
			float height2 = space.getHeight()/2;
			
			if ((width2 < smallestSpace) && (height2 < smallestSpace)) {
				return;
			}
			
			subsection(map, new Space(space.getX(), space.getY(), width2, height2), spaces);
			subsection(map, new Space(space.getX(), space.getY()+height2, width2, height2), spaces);
			subsection(map, new Space(space.getX()+width2, space.getY(), width2, height2), spaces);
			subsection(map, new Space(space.getX()+width2, space.getY()+height2, width2, height2), spaces);
		} else {
			spaces.add(space);
		}
	}

	/**
	 * Path finding context implementation
	 * 
	 * @return The current mover
	 */
	public Mover getMover() {
		return null;
	}

	/**
	 * Path finding context implementation
	 * 
	 * @return The current search distance
	 */
	public int getSearchDistance() {
		return 0;
	}

	/**
	 * Path finding context implementation
	 * 
	 * @return The current x location
	 */
	public int getSourceX() {
		return sx;
	}

	/**
	 * Path finding context implementation
	 * 
	 * @return The current y location
	 */
	public int getSourceY() {
		return sy;
	}
}
