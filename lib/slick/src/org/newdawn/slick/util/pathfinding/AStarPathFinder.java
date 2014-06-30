package org.newdawn.slick.util.pathfinding;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.newdawn.slick.util.pathfinding.heuristics.ClosestHeuristic;

/**
 * A path finder implementation that uses the AStar heuristic based algorithm
 * to determine a path. 
 * 
 * @author Kevin Glass
 */
public class AStarPathFinder implements PathFinder, PathFindingContext {
	/** The set of nodes that have been searched through */
	private ArrayList closed = new ArrayList();
	/** The set of nodes that we do not yet consider fully searched */
	private PriorityList open = new PriorityList();
	
	/** The map being searched */
	private TileBasedMap map;
	/** The maximum depth of search we're willing to accept before giving up */
	private int maxSearchDistance;
	
	/** The complete set of nodes across the map */
	private Node[][] nodes;
	/** True if we allow diaganol movement */
	private boolean allowDiagMovement;
	/** The heuristic we're applying to determine which nodes to search first */
	private AStarHeuristic heuristic;
	/** The node we're currently searching from */
	private Node current;
	
	/** The mover going through the path */
	private Mover mover;
	/** The x coordinate of the source tile we're moving from */
	private int sourceX;
	/** The y coordinate of the source tile we're moving from */
	private int sourceY;
	/** The distance searched so far */
	private int distance;
	
	/**
	 * Create a path finder with the default heuristic - closest to target.
	 * 
	 * @param map The map to be searched
	 * @param maxSearchDistance The maximum depth we'll search before giving up
	 * @param allowDiagMovement True if the search should try diaganol movement
	 */
	public AStarPathFinder(TileBasedMap map, int maxSearchDistance, boolean allowDiagMovement) {
		this(map, maxSearchDistance, allowDiagMovement, new ClosestHeuristic());
	}

	/**
	 * Create a path finder 
	 * 
	 * @param heuristic The heuristic used to determine the search order of the map
	 * @param map The map to be searched
	 * @param maxSearchDistance The maximum depth we'll search before giving up
	 * @param allowDiagMovement True if the search should try diaganol movement
	 */
	public AStarPathFinder(TileBasedMap map, int maxSearchDistance, 
						   boolean allowDiagMovement, AStarHeuristic heuristic) {
		this.heuristic = heuristic;
		this.map = map;
		this.maxSearchDistance = maxSearchDistance;
		this.allowDiagMovement = allowDiagMovement;
		
		nodes = new Node[map.getWidthInTiles()][map.getHeightInTiles()];
		for (int x=0;x<map.getWidthInTiles();x++) {
			for (int y=0;y<map.getHeightInTiles();y++) {
				nodes[x][y] = new Node(x,y);
			}
		}
	}
	
	/**
	 * @see PathFinder#findPath(Mover, int, int, int, int)
	 */
	public Path findPath(Mover mover, int sx, int sy, int tx, int ty) {
		current = null;
		
		// easy first check, if the destination is blocked, we can't get there
		this.mover = mover;
		this.sourceX = tx;
		this.sourceY = ty;
		this.distance = 0;
		
		if (map.blocked(this, tx, ty)) {
			return null;
		}

		for (int x=0;x<map.getWidthInTiles();x++) {
			for (int y=0;y<map.getHeightInTiles();y++) {
				nodes[x][y].reset();
			}
		}
		
		// initial state for A*. The closed group is empty. Only the starting
		// tile is in the open list and it's cost is zero, i.e. we're already there
		nodes[sx][sy].cost = 0;
		nodes[sx][sy].depth = 0;
		closed.clear();
		open.clear();
		addToOpen(nodes[sx][sy]);
		
		nodes[tx][ty].parent = null;
		
		// while we haven't found the goal and haven't exceeded our max search depth
		int maxDepth = 0;
		while ((maxDepth < maxSearchDistance) && (open.size() != 0)) {
			// pull out the first node in our open list, this is determined to 
			// be the most likely to be the next step based on our heuristic
			int lx = sx;
			int ly = sy;
			if (current != null) {
				lx = current.x;
				ly = current.y;
			}
			
			current = getFirstInOpen();
			distance = current.depth;
			
			if (current == nodes[tx][ty]) {
				if (isValidLocation(mover,lx,ly,tx,ty)) {
					break;
				}
			}
			
			removeFromOpen(current);
			addToClosed(current);
			
			// search through all the neighbours of the current node evaluating
			// them as next steps
			for (int x=-1;x<2;x++) {
				for (int y=-1;y<2;y++) {
					// not a neighbour, its the current tile
					if ((x == 0) && (y == 0)) {
						continue;
					}
					
					// if we're not allowing diaganol movement then only 
					// one of x or y can be set
					if (!allowDiagMovement) {
						if ((x != 0) && (y != 0)) {
							continue;
						}
					}
					
					// determine the location of the neighbour and evaluate it
					int xp = x + current.x;
					int yp = y + current.y;
					
					if (isValidLocation(mover,current.x,current.y,xp,yp)) {
						// the cost to get to this node is cost the current plus the movement
						// cost to reach this node. Note that the heursitic value is only used
						// in the sorted open list
						float nextStepCost = current.cost + getMovementCost(mover, current.x, current.y, xp, yp);
						Node neighbour = nodes[xp][yp];
						map.pathFinderVisited(xp, yp);
						
						// if the new cost we've determined for this node is lower than 
						// it has been previously makes sure the node hasn't been discarded. We've
						// determined that there might have been a better path to get to
						// this node so it needs to be re-evaluated
						if (nextStepCost < neighbour.cost) {
							if (inOpenList(neighbour)) {
								removeFromOpen(neighbour);
							}
							if (inClosedList(neighbour)) {
								removeFromClosed(neighbour);
							}
						}
						
						// if the node hasn't already been processed and discarded then
						// reset it's cost to our current cost and add it as a next possible
						// step (i.e. to the open list)
						if (!inOpenList(neighbour) && !(inClosedList(neighbour))) {
							neighbour.cost = nextStepCost;
							neighbour.heuristic = getHeuristicCost(mover, xp, yp, tx, ty);
							maxDepth = Math.max(maxDepth, neighbour.setParent(current));
							addToOpen(neighbour);
						} 
					}
				}
			}
		}

		// since we've got an empty open list or we've run out of search 
		// there was no path. Just return null
		if (nodes[tx][ty].parent == null) {
			return null;
		}
		
		// At this point we've definitely found a path so we can uses the parent
		// references of the nodes to find out way from the target location back
		// to the start recording the nodes on the way.
		Path path = new Path();
		Node target = nodes[tx][ty];
		while (target != nodes[sx][sy]) {
			path.prependStep(target.x, target.y);
			target = target.parent;
		}
		path.prependStep(sx,sy);
		
		// thats it, we have our path 
		return path;
	}

	/**
	 * Get the X coordinate of the node currently being evaluated
	 * 
	 * @return The X coordinate of the node currently being evaluated
	 */
	public int getCurrentX() {
		if (current == null) {
			return -1;
		}
		
		return current.x;
	}

	/**
	 * Get the Y coordinate of the node currently being evaluated
	 * 
	 * @return The Y coordinate of the node currently being evaluated
	 */
	public int getCurrentY() {
		if (current == null) {
			return -1;
		}
		
		return current.y;
	}
	
	/**
	 * Get the first element from the open list. This is the next
	 * one to be searched.
	 * 
	 * @return The first element in the open list
	 */
	protected Node getFirstInOpen() {
		return (Node) open.first();
	}
	
	/**
	 * Add a node to the open list
	 * 
	 * @param node The node to be added to the open list
	 */
	protected void addToOpen(Node node) {
		node.setOpen(true);
		open.add(node);
	}
	
	/**
	 * Check if a node is in the open list
	 * 
	 * @param node The node to check for
	 * @return True if the node given is in the open list
	 */
	protected boolean inOpenList(Node node) {
		return node.isOpen();
	}
	
	/**
	 * Remove a node from the open list
	 * 
	 * @param node The node to remove from the open list
	 */
	protected void removeFromOpen(Node node) {
		node.setOpen(false);
		open.remove(node);
	}
	
	/**
	 * Add a node to the closed list
	 * 
	 * @param node The node to add to the closed list
	 */
	protected void addToClosed(Node node) {
		node.setClosed(true);
		closed.add(node);
	}
	
	/**
	 * Check if the node supplied is in the closed list
	 * 
	 * @param node The node to search for
	 * @return True if the node specified is in the closed list
	 */
	protected boolean inClosedList(Node node) {
		return node.isClosed();
	}
	
	/**
	 * Remove a node from the closed list
	 * 
	 * @param node The node to remove from the closed list
	 */
	protected void removeFromClosed(Node node) {
		node.setClosed(false);
		closed.remove(node);
	}
	
	/**
	 * Check if a given location is valid for the supplied mover
	 * 
	 * @param mover The mover that would hold a given location
	 * @param sx The starting x coordinate
	 * @param sy The starting y coordinate
	 * @param x The x coordinate of the location to check
	 * @param y The y coordinate of the location to check
	 * @return True if the location is valid for the given mover
	 */
	protected boolean isValidLocation(Mover mover, int sx, int sy, int x, int y) {
		boolean invalid = (x < 0) || (y < 0) || (x >= map.getWidthInTiles()) || (y >= map.getHeightInTiles());
		
		if ((!invalid) && ((sx != x) || (sy != y))) {
			this.mover = mover;
			this.sourceX = sx;
			this.sourceY = sy;
			invalid = map.blocked(this, x, y);
		}
		
		return !invalid;
	}
	
	/**
	 * Get the cost to move through a given location
	 * 
	 * @param mover The entity that is being moved
	 * @param sx The x coordinate of the tile whose cost is being determined
	 * @param sy The y coordiante of the tile whose cost is being determined
	 * @param tx The x coordinate of the target location
	 * @param ty The y coordinate of the target location
	 * @return The cost of movement through the given tile
	 */
	public float getMovementCost(Mover mover, int sx, int sy, int tx, int ty) {
		this.mover = mover;
		this.sourceX = sx;
		this.sourceY = sy;
		
		return map.getCost(this, tx, ty);
	}

	/**
	 * Get the heuristic cost for the given location. This determines in which 
	 * order the locations are processed.
	 * 
	 * @param mover The entity that is being moved
	 * @param x The x coordinate of the tile whose cost is being determined
	 * @param y The y coordiante of the tile whose cost is being determined
	 * @param tx The x coordinate of the target location
	 * @param ty The y coordinate of the target location
	 * @return The heuristic cost assigned to the tile
	 */
	public float getHeuristicCost(Mover mover, int x, int y, int tx, int ty) {
		return heuristic.getCost(map, mover, x, y, tx, ty);
	}
	
	/**
	 * A list that sorts any element provided into the list
	 *
	 * @author kevin
	 */
	private class PriorityList {
		/** The list of elements */
		private List list = new LinkedList();
		
		/**
		 * Retrieve the first element from the list
		 *  
		 * @return The first element from the list
		 */
		public Object first() {
			return list.get(0);
		}
		
		/**
		 * Empty the list
		 */
		public void clear() {
			list.clear();
		}
		
		/**
		 * Add an element to the list - causes sorting
		 * 
		 * @param o The element to add
		 */
		public void add(Object o) {
			// float the new entry 
			for (int i=0;i<list.size();i++) {
				if (((Comparable) list.get(i)).compareTo(o) > 0) {
					list.add(i, o);
					break;
				}
			}
			if (!list.contains(o)) {
				list.add(o);
			}
			//Collections.sort(list);
		}
		
		/**
		 * Remove an element from the list
		 * 
		 * @param o The element to remove
		 */
		public void remove(Object o) {
			list.remove(o);
		}
	
		/**
		 * Get the number of elements in the list
		 * 
		 * @return The number of element in the list
 		 */
		public int size() {
			return list.size();
		}
		
		/**
		 * Check if an element is in the list
		 * 
		 * @param o The element to search for
		 * @return True if the element is in the list
		 */
		public boolean contains(Object o) {
			return list.contains(o);
		}
		
		public String toString() {
			String temp = "{";
			for (int i=0;i<size();i++) {
				temp += list.get(i).toString()+",";
			}
			temp += "}";
			
			return temp;
		}
	}
	
	/**
	 * A single node in the search graph
	 */
	private class Node implements Comparable {
		/** The x coordinate of the node */
		private int x;
		/** The y coordinate of the node */
		private int y;
		/** The path cost for this node */
		private float cost;
		/** The parent of this node, how we reached it in the search */
		private Node parent;
		/** The heuristic cost of this node */
		private float heuristic;
		/** The search depth of this node */
		private int depth;
		/** In the open list */
		private boolean open;
		/** In the closed list */
		private boolean closed;
		
		/**
		 * Create a new node
		 * 
		 * @param x The x coordinate of the node
		 * @param y The y coordinate of the node
		 */
		public Node(int x, int y) {
			this.x = x;
			this.y = y;
		}
		
		/**
		 * Set the parent of this node
		 * 
		 * @param parent The parent node which lead us to this node
		 * @return The depth we have no reached in searching
		 */
		public int setParent(Node parent) {
			depth = parent.depth + 1;
			this.parent = parent;
			
			return depth;
		}
		
		/**
		 * @see Comparable#compareTo(Object)
		 */
		public int compareTo(Object other) {
			Node o = (Node) other;
			
			float f = heuristic + cost;
			float of = o.heuristic + o.cost;
			
			if (f < of) {
				return -1;
			} else if (f > of) {
				return 1;
			} else {
				return 0;
			}
		}
		
		/**
		 * Indicate whether the node is in the open list
		 * 
		 * @param open True if the node is in the open list
		 */
		public void setOpen(boolean open) {
			this.open = open;
		}
		
		/**
		 * Check if the node is in the open list
		 * 
		 * @return True if the node is in the open list
		 */
		public boolean isOpen() {
			return open;
		}
		
		/**
		 * Indicate whether the node is in the closed list
		 * 
		 * @param closed True if the node is in the closed list
		 */
		public void setClosed(boolean closed) {
			this.closed = closed;
		}
		
		/**
		 * Check if the node is in the closed list
		 * 
		 * @return True if the node is in the closed list
		 */
		public boolean isClosed() {
			return closed;
		}

		/**
		 * Reset the state of this node
		 */
		public void reset() {
			closed = false;
			open = false;
			cost = 0;
			depth = 0;
		}
		
		/**
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return "[Node "+x+","+y+"]";
		}
	}

	/**
	 * @see org.newdawn.slick.util.pathfinding.PathFindingContext#getMover()
	 */
	public Mover getMover() {
		return mover;
	}

	/**
	 * @see org.newdawn.slick.util.pathfinding.PathFindingContext#getSearchDistance()
	 */
	public int getSearchDistance() {
		return distance;
	}

	/**
	 * @see org.newdawn.slick.util.pathfinding.PathFindingContext#getSourceX()
	 */
	public int getSourceX() {
		return sourceX;
	}

	/**
	 * @see org.newdawn.slick.util.pathfinding.PathFindingContext#getSourceY()
	 */
	public int getSourceY() {
		return sourceY;
	}
}
