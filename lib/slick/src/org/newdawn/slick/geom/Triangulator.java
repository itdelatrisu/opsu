package org.newdawn.slick.geom;

import java.io.Serializable;

/**
 * A collection of triangles
 *
 * @author kevin
 */
public interface Triangulator extends Serializable {

	/**
	 * Get a count of the number of triangles produced
	 * 
	 * @return The number of triangles produced
	 */
	public int getTriangleCount();
	
	/**
	 * Get a point on a specified generated triangle
	 * 
	 * @param tri The index of the triangle to interegate
	 * @param i The index of the point within the triangle to retrieve
	 * (0 - 2)
	 * @return The x,y coordinate pair for the point
	 */
	public float[] getTrianglePoint(int tri, int i);
	
	/**
	 * Add a point that forms part of the outer polygon
	 * 
	 * @param x The x coordinate of the point
	 * @param y The y coordiante of the point
	 */
	public void addPolyPoint(float x, float y);
	
	/**
	 * Start a hole in the polygon
	 */
	public void startHole();
	
	/**
	 * Run the triangulation
	 * 
	 * @return True if successful
	 */
	public boolean triangulate();
}
