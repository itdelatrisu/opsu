package org.newdawn.slick;

import org.newdawn.slick.geom.Shape;
import org.newdawn.slick.geom.Vector2f;

/**
 * A filling method for a shape. This allows changing colours at shape verticies and 
 * modify they're positions as required
 *
 * @author kevin
 */
public interface ShapeFill {

	/**
	 * Get the colour that should be applied at the specified location
	 * 
	 * @param shape The shape being filled
	 * @param x The x coordinate of the point being coloured 
	 * @param y The y coordinate of the point being coloured
	 * @return The colour that should be applied based on the control points of this gradient
	 */
	public Color colorAt(Shape shape, float x, float y);

	/**
	 * Get the offset for a vertex at a given location based on it's shape
	 * 
	 * @param shape The shape being filled
	 * @param x The x coordinate of the point being drawn 
	 * @param y The y coordinate of the point being drawn
	 * @return The offset to apply to this vertex
	 */
	public Vector2f getOffsetAt(Shape shape, float x, float y);
}
