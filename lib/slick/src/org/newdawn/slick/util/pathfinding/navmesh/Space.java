package org.newdawn.slick.util.pathfinding.navmesh;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * A quad space within a navigation mesh
 * 
 * @author kevin
 */
public class Space {
	/** The x coordinate of the top corner of the space */
	private float x;
	/** The y coordinate of the top corner of the space */
	private float y;
	/** The width of the space */
	private float width;
	/** The height of the space */
	private float height;
	
	/** A map from spaces to the links that connect them to this space */
	private HashMap links = new HashMap();
	/** A list of the links from this space to others */
	private ArrayList linksList = new ArrayList();
	/** The cost to get to this node */
	private float cost;
	
	/**
	 * Create a new space 
	 * 
	 * @param x The x coordinate of the top corner of the space 
	 * @param y The y coordinate of the top corner of the space 
	 * @param width The width of the space
	 * @param height The height of the space
	 */
	public Space(float x, float y, float width, float height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}
	
	/**
	 * Get the width of the space
	 * 
	 * @return The width of the space
	 */
	public float getWidth() {
		return width;
	}

	/**
	 * Get the height of the space
	 * 
	 * @return The height of the space
	 */
	public float getHeight() {
		return height;
	}

	/**
	 * Get the x coordinate of the top corner of the space
	 * 
	 * @return The x coordinate of the top corner of the space
	 */
	public float getX() {
		return x;
	}

	/**
	 * Get the y coordinate of the top corner of the space
	 * 
	 * @return The y coordinate of the top corner of the space
	 */
	public float getY() {
		return y;
	}
	
	/**
	 * Link this space to another by creating a link and finding the point
	 * at which the spaces link up
	 * 
	 * @param other The other space to link to
	 */
	public void link(Space other) {
		// aligned vertical edges
		if (inTolerance(x,other.x+other.width) || inTolerance(x+width, other.x)) {
			float linkx = x;
			if (x+width == other.x) {
				linkx = x+width;
			}
			
			float top = Math.max(y, other.y);
			float bottom = Math.min(y+height, other.y+other.height);
			float linky = top + ((bottom-top)/2);
			
			Link link = new Link(linkx, linky, other);
			links.put(other,link);
			linksList.add(link);
		}
		// aligned horizontal edges
		if (inTolerance(y, other.y+other.height) || inTolerance(y+height, other.y)) {
			float linky = y;
			if (y+height == other.y) {
				linky = y+height;
			}
			
			float left = Math.max(x, other.x);
			float right = Math.min(x+width, other.x+other.width);
			float linkx = left + ((right-left)/2);
			
			Link link = new Link(linkx, linky, other);
			links.put(other, link);
			linksList.add(link);
		}		
	}
	
	/**
	 * Check whether two locations are within tolerance distance. This is
	 * used when finding aligned edges to remove float rounding errors
	 * 
	 * @param a The first value
	 * @param b The second value
	 * @return True if the edges are close enough (tm)
	 */
	private boolean inTolerance(float a, float b) {
		return a == b;
	}
	
	/**
	 * Check if this space has an edge that is joined with another
	 * 
	 * @param other The other space to check against
	 * @return True if the spaces have a shared edge
	 */
	public boolean hasJoinedEdge(Space other) {
		// aligned vertical edges
		if (inTolerance(x,other.x+other.width) || inTolerance(x+width,other.x)) {
			if ((y >= other.y) && (y <= other.y + other.height)) {
				return true;
			}
			if ((y+height >= other.y) && (y+height <= other.y + other.height)) {
				return true;
			}
			if ((other.y >= y) && (other.y <= y + height)) {
				return true;
			}
			if ((other.y+other.height >= y) && (other.y+other.height <= y + height)) {
				return true;
			}
		}
		// aligned horizontal edges
		if (inTolerance(y, other.y+other.height) || inTolerance(y+height, other.y)) {
			if ((x >= other.x) && (x <= other.x + other.width)) {
				return true;
			}
			if ((x+width >= other.x) && (x+width <= other.x + other.width)) {
				return true;
			}
			if ((other.x >= x) && (other.x <= x + width)) {
				return true;
			}
			if ((other.x+other.width >= x) && (other.x+other.width <= x + width)) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Merge this space with another 
	 * 
	 * @param other The other space to merge with
	 * @return The result space created by joining the two
	 */
	public Space merge(Space other) {
		float minx = Math.min(x, other.x);
		float miny = Math.min(y, other.y);
		
		float newwidth = width+other.width;
		float newheight = height+other.height;
		if (x == other.x) {
			newwidth = width;
		} else {
			newheight = height;
		}
		return new Space(minx, miny, newwidth, newheight);
	}
	
	/**
	 * Check if the given space can be merged with this one. It must have
	 * an adjacent edge and have the same height or width as this space.
	 * 
	 * @param other The other space to be considered
	 * @return True if the spaces can be joined together
	 */
	public boolean canMerge(Space other) {
		if (!hasJoinedEdge(other)) {
			return false;
		}
		
		if ((x == other.x) && (width == other.width)) {
			return true;
		}
		if ((y == other.y) && (height == other.height)) {
			return true;
		}
		
		return false;
	}
	
	/**
	 * Get the number of links
	 * 
	 * @return The number of links from the space to others
	 */
	public int getLinkCount() {
		return linksList.size();
	}
	
	/**
	 * Get the link from this space to another at a particular index
	 * 
	 * @param index The index of the link to retrieve
	 * @return The link from this space to another
	 */
	public Link getLink(int index) {
		return (Link) linksList.get(index);
	}
	
	/**
	 * Check if this space contains a given point
	 * 
	 * @param xp The x coordinate to check
	 * @param yp The y coordinate to check
	 * @return True if this space container the coordinate given
	 */
	public boolean contains(float xp, float yp) {
		return (xp >= x) && (xp < x+width) && (yp >= y) && (yp < y+height);
	}
	
	/**
	 * Fill the spaces based on the cost from a given starting point
	 * 
	 * @param target The target space we're heading for
	 * @param sx The x coordinate of the starting point
	 * @param sy The y coordinate of the starting point
	 * @param cost The cost up to this point
	 */
	public void fill(Space target, float sx, float sy, float cost) {
		if (cost >= this.cost) {
			return;
		}
		this.cost = cost;
		if (target == this) {
			return;
		}
		
		for (int i=0;i<getLinkCount();i++) {
			Link link = getLink(i);
			float extraCost = link.distance2(sx,sy);
			float nextCost = cost + extraCost;
			link.getTarget().fill(target, link.getX(), link.getY(), nextCost);
		}
	}
	
	/**
	 * Clear the costing values across the whole map
	 */
	public void clearCost() {
		cost = Float.MAX_VALUE;
	}

	/**
	 * Get the cost to get to this node at the moment
	 * 
	 * @return The cost to get to this node
	 */
	public float getCost() {
		return cost;
	}

	/**
	 * Pick the lowest cost route from this space to another on the path
	 *  
	 * @param target The target space we're looking for
	 * @param path The path to add the steps to
	 * @return True if the path was found
	 */
	public boolean pickLowestCost(Space target, NavPath path) {
		if (target == this) {
			return true;
		}
		if (links.size() == 0) {
			return false;
		}

		Link bestLink = null;	
		for (int i=0;i<getLinkCount();i++) {
			Link link = getLink(i);
			if ((bestLink == null) || (link.getTarget().getCost() < bestLink.getTarget().getCost())) {
				bestLink = link;
			}
		}
		
		path.push(bestLink);
		return bestLink.getTarget().pickLowestCost(target, path);
	}
	
	/**
	 * Get the string representation of this instance
	 * 
	 * @return The string representation of this instance
	 */
	public String toString() {
		return "[Space "+x+","+y+" "+width+","+height+"]";
	}
}
